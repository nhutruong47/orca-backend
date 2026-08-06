from __future__ import annotations

import json
import re
import unicodedata
from datetime import datetime
from typing import Any, Type
from zoneinfo import ZoneInfo

import httpx
from pydantic import ValidationError

from app.config import settings
from app.models import ExtractRequest, ExtractResponse, PlanDraftResponse, PlanRequest, ReviseRequest, TaskDraft


class GeminiExtractError(RuntimeError):
    pass


class GeminiPlanError(RuntimeError):
    pass


class GeminiPlanInputError(GeminiPlanError):
    pass


class GeminiReviseError(RuntimeError):
    pass


def extract(request: ExtractRequest) -> ExtractResponse:
    prompt = _build_extract_prompt(request)
    data = _generate_json_object(prompt, max_output_tokens=4096, error_cls=GeminiExtractError)
    try:
        response = ExtractResponse.model_validate(data)
    except ValidationError as exc:
        raise GeminiExtractError(f"Gemini extract JSON failed schema validation: {exc}") from exc
    response = _apply_extract_context(response, request)
    # Always override clarifyingQuestion with our precise computed version based on actual missing fields
    if response.missingFields:
        response.clarifyingQuestion = _clarifying_question_for_missing(response.intent, response.missingFields, response.fields)
    elif response.intent == "UNKNOWN" and not response.clarifyingQuestion:
        response.clarifyingQuestion = _clarifying_question_for_missing(response.intent, response.missingFields, response.fields)
    return response


def plan(request: PlanRequest) -> PlanDraftResponse:
    _validate_plan_input(request)
    prompt = _build_plan_prompt(request)
    data = _generate_json_object(prompt, max_output_tokens=8192, error_cls=GeminiPlanError)
    try:
        draft = PlanDraftResponse.model_validate(data)
    except ValidationError as exc:
        raise GeminiPlanError(f"Gemini plan JSON failed schema validation: {exc}") from exc
    return _validate_plan_output(draft, request)


def revise(request: ReviseRequest) -> PlanDraftResponse:
    if _is_ambiguous_revise_instruction(request.instruction):
        return request.draft

    safe_draft = _revise_with_safe_rule(request)
    if safe_draft is not None:
        return safe_draft

    prompt = _build_revise_prompt(request)

    def _attempt(extra_suffix: str = "") -> PlanDraftResponse:
        data = _generate_json_object(prompt + extra_suffix, max_output_tokens=8192, error_cls=GeminiReviseError)
        try:
            draft = PlanDraftResponse.model_validate(data)
        except ValidationError as exc:
            raise GeminiReviseError(f"Gemini revise JSON failed schema validation: {exc}") from exc
        draft = _normalize_revise_output(draft, request)
        return _validate_revise_output(draft, request)

    try:
        return _attempt()
    except GeminiReviseError:
        # Retry once with a stricter instruction appended to the prompt
        retry_suffix = (
            "\n\n[RETRY INSTRUCTION] Your previous response was rejected by validation rules. "
            "Do NOT change any field that was not explicitly mentioned in the user's instruction. "
            "Only modify the specific parts the user asked about. Keep all other fields identical."
        )
        try:
            return _attempt(retry_suffix)
        except GeminiReviseError:
            # Fallback: return original draft untouched + friendly aiNote
            fallback = request.draft.model_copy(deep=True)
            fallback.aiNote = "Mình chưa hiểu rõ yêu cầu chỉnh sửa này. Bạn thử mô tả cụ thể hơn nhé? Ví dụ: \"đổi tên thành...\", \"thêm công việc...\", \"đổi hạn thành ngày...\"."
            return fallback




def _apply_extract_context(response: ExtractResponse, request: ExtractRequest) -> ExtractResponse:
    context = request.context
    if context is None or context.mode != "WAITING_CLARIFICATION":
        return response

    # If the user asked an UNKNOWN out-of-scope question, do not stick to previous context intent
    if response.intent == "UNKNOWN":
        return response

    fields = dict(context.previousFields or {})
    fields.update({key: value for key, value in (response.fields or {}).items() if value not in (None, "", [])})

    intent = response.intent
    if context.intent and context.intent != "UNKNOWN":
        intent = context.intent

    missing = _missing_extract_fields(intent, fields)
    clarifying_question = None if not missing else _clarifying_question_for_missing(intent, missing, fields)
    confidence = max(response.confidence, 0.7 if not missing else 0.55)

    return ExtractResponse(
        intent=intent,
        confidence=min(confidence, 1),
        fields=fields,
        missingFields=missing,
        clarifyingQuestion=clarifying_question,
    )


def _missing_extract_fields(intent: str, fields: dict[str, Any]) -> list[str]:
    if intent == "PRODUCTION_PLAN":
        required = ["productName", "quantity", "unit", "deadline"]
    elif intent == "OPERATION_TASK":
        required = ["title", "deadline"]
    else:
        return []

    missing: list[str] = []
    for key in required:
        value = fields.get(key)
        if value is None or value == "" or value == []:
            missing.append(key)
    return missing


def _clarifying_question_for_missing(intent: str, missing: list[str], fields: dict | None = None) -> str:
    fields = fields or {}
    product = fields.get("productName", "")
    qty = fields.get("quantity", "")
    unit = fields.get("unit", "")

    if intent == "UNKNOWN":
        return "Dạ em là trợ lý ORCA chuyên hỗ trợ quản lý sản xuất và công việc cho xưởng. Anh/chị cần em giúp lên kế hoạch sản xuất hay tạo công việc vận hành nào không ạ?"

    # Single missing field — ask naturally and specifically
    if missing == ["deadline"]:
        if product and qty and unit:
            return f"Anh/chị muốn hoàn thành mẻ {product} {qty}{unit} này trước thời gian nào ạ?"
        if product:
            return f"Anh/chị cho mình biết hạn hoàn thành cho yêu cầu {product} này nhé?"
        return "Anh/chị muốn hoàn thành công việc này trước ngày/giờ nào ạ?"

    if missing == ["productName"]:
        return "Anh/chị đang muốn sản xuất sản phẩm gì vậy ạ? (ví dụ: Arabica, Robusta, Blend...)"

    if missing == ["quantity"]:
        if product:
            return f"Anh/chị muốn sản xuất bao nhiêu {product} ạ?"
        return "Anh/chị muốn sản xuất bao nhiêu ạ?"

    if missing == ["unit"]:
        return "Đơn vị tính là gì ạ? (ví dụ: kg, tấn, túi...)"

    if missing == ["title"]:
        return "Anh/chị muốn tạo công việc gì ạ?"

    # Multiple missing fields — list them out clearly
    label_map = {
        "productName": "tên sản phẩm (ví dụ: Arabica, Robusta)",
        "quantity":    "số lượng",
        "unit":        "đơn vị (kg, tấn...)",
        "deadline":    "hạn hoàn thành",
        "title":       "nội dung công việc",
        "taskDescription": "mô tả công việc",
    }
    readable = ", ".join(label_map.get(f, f) for f in missing)
    if intent == "PRODUCTION_PLAN":
        return f"Anh/chị bổ sung giúp mình {readable} cho yêu cầu sản xuất này nhé?"
    if intent == "OPERATION_TASK":
        return f"Anh/chị bổ sung giúp mình {readable} cho công việc này nhé?"
    return "Dạ em là trợ lý ORCA chuyên hỗ trợ quản lý xưởng. Anh/chị cần em hỗ trợ lên kế hoạch sản xuất hay giao việc gì hôm nay không ạ?"


def _build_extract_context_section(request: ExtractRequest) -> str:
    if request.context is None:
        return "No active conversation context. Treat the user request as a new request."

    context_json = json.dumps(request.context.model_dump(), ensure_ascii=False, indent=2)
    return f"""
Active conversation context:
{context_json}

Context rules:
- mode WAITING_CLARIFICATION means the user is answering a previous clarifying question.
- Preserve originalText, previousFields, and intent unless the latest message clearly corrects one of them.
- Extract only the missing or corrected fields from the latest user message, then return the full merged fields object.
- Do not restart classification from scratch when context.intent is PRODUCTION_PLAN or OPERATION_TASK.
""".strip()
def _build_extract_prompt(request: ExtractRequest) -> str:
    now = _local_now()
    context_section = _build_extract_context_section(request)
    return f"""
You are ORCA AI v2 extract module for a Vietnamese workshop/task management app.

Your only job is to classify the user request and extract structured fields.
Do not create tasks. Do not save data. Do not explain.

Current local datetime: {now.isoformat(timespec="minutes")}
Timezone: Asia/Ho_Chi_Minh

Supported intents:
- PRODUCTION_PLAN: production/manufacturing requests such as roast, produce, make a quantity of coffee/product.
- OPERATION_TASK: internal operational tasks such as cleaning, arranging, checking an area or equipment.
- UNKNOWN: unclear requests or features outside MVP such as inventory summary, marketplace, delivery workflow, maintenance workflow, progress summary.

Required fields:
- PRODUCTION_PLAN: productName, quantity, unit, deadline.
- OPERATION_TASK: title, deadline if the user gives or implies a time/date; if no deadline is present, include missingFields ["deadline"].

Deadline rules:
- Return deadline as ISO local datetime string without timezone offset, e.g. "2026-06-07T17:00:00".
- Resolve Vietnamese relative and absolute dates using Current local datetime (year {now.year}).
- "5 tháng 8" or "hạn 5 tháng 8" or "ngày 5 tháng 8" or "5/8" means day 5 of month 8 in the current year: "{now.year}-08-05T17:00:00".
- "hom nay"/"hôm nay" means today.
- "ngay mai"/"ngày mai"/"mai" means tomorrow.
- "sang"/"sáng" default time 09:00.
- "chieu"/"chiều" default time 14:00.
- If date is present but time is absent, default time 17:00.
- If exact time is present, use that exact time.
- When the user text contains a date phrase (e.g. "hạn 5 tháng 8", "ngày 5/8", "hôm nay"), you MUST extract deadline into fields and remove "deadline" from missingFields.

Output only one JSON object matching this schema:
{{
  "intent": "PRODUCTION_PLAN" | "OPERATION_TASK" | "UNKNOWN",
  "confidence": number between 0 and 1,
  "fields": object,
  "missingFields": array of strings,
  "clarifyingQuestion": string or null
}}

Field conventions:
- fields.priority must be "LOW", "MEDIUM", or "HIGH" when known.
- PRODUCTION_PLAN fields may include: title, productName, quantity, unit, deadline, priority.
- For detailed production orders, preserve all provided specs in fields using clear keys such as orderCode, startDate, deliveryDate, composition, materialSources, roastLevel, targetAgtron, grindType, targetMicron, flavorNotes, packagingSpec, expectedPackageCount, packageMaterial, labelSpec, cartonSpec, qualityRequirements. Do not drop details.
- OPERATION_TASK fields may include: title, area, deadline, priority.
- UNKNOWN should not invent production fields.
- If missingFields is not empty, clarifyingQuestion must ask for those missing details in Vietnamese.
- If intent is UNKNOWN (out-of-scope, casual chat, weather, general knowledge, or unsupported feature requests):
  - missingFields MUST be empty [].
  - clarifyingQuestion MUST be a witty, friendly, humorous response in Vietnamese. Use the specific topic of the user's input (e.g. weather, geography, love, revenue reports) to make a lighthearted joke about your limitations as a workshop assistant, then warmly guide them back to creating a production plan or an operational task.

Examples:
User: "Rang 120kg Arabica trước 17:00 hôm nay"
JSON:
{{
  "intent": "PRODUCTION_PLAN",
  "confidence": 0.9,
  "fields": {{
    "title": "Rang 120kg Arabica",
    "productName": "Arabica",
    "quantity": 120,
    "unit": "kg",
    "deadline": "{now.year}-08-03T17:00:00"
  }},
  "missingFields": [],
  "clarifyingQuestion": null
}}

User: "Thời tiết hôm nay thế nào?"
JSON:
{{
  "intent": "UNKNOWN",
  "confidence": 0.95,
  "fields": {{}},
  "missingFields": [],
  "clarifyingQuestion": "Mình không biết dự báo thời tiết hôm nay nắng hay mưa đâu ☀️🌧️, nhưng mình biết chắc xưởng bạn đang chờ lên mẻ sản xuất đấy! Bạn muốn tạo mẻ rang nào hôm nay không?"
}}

User: "Quê của Bác Hồ ở đâu?"
JSON:
{{
  "intent": "UNKNOWN",
  "confidence": 0.95,
  "fields": {{}},
  "missingFields": [],
  "clarifyingQuestion": "Biết quê Bác ở Nam Đàn, Nghệ An thì vượt quá tầm phủ sóng của một trợ lý xưởng như mình rồi 😅! Mình chỉ giỏi chia việc và lên mẻ sản xuất thôi. Bạn có việc gì cần mình giao hôm nay không?"
}}

User: "Làm cái kia cho khách"
JSON:
{{
  "intent": "UNKNOWN",
  "confidence": 0.35,
  "fields": {{}},
  "missingFields": ["taskDescription"],
  "clarifyingQuestion": "Anh/chị muốn tạo công việc gì và hạn hoàn thành khi nào?"
}}

Conversation context:
{context_section}

Now extract this user request:
{request.text}
""".strip()


def _build_plan_prompt(request: PlanRequest) -> str:
    fields_json = json.dumps(request.fields, ensure_ascii=False, indent=2)
    members_json = json.dumps([member.model_dump() for member in request.members], ensure_ascii=False, indent=2)
    if request.intent == "PRODUCTION_PLAN" and _is_detailed_production_order(request.fields or {}):
        task_count_rule = "6 to 12 tasks"
    else:
        task_count_rule = "3 to 6 tasks" if request.intent == "PRODUCTION_PLAN" else "2 to 4 tasks"

    return f"""
You are ORCA AI v2 plan module for a Vietnamese workshop/task management app.

Your only job is to convert extracted structured fields into a draft Goal and draft Tasks.
Do not classify intent. Do not ask questions. Do not save data. Do not explain.

Input intent:
{request.intent}

Extracted fields:
{fields_json}

Team members available for suggested assignment:
{members_json}

Output only one JSON object matching this exact schema:
{{
  "goalTitle": "string",
  "outputTarget": "string",
  "deadline": "ISO datetime string from extracted fields or null",
  "priority": integer from 1 to 5,
  "tasks": [
    {{
      "title": "string",
      "description": "string",
      "priority": integer from 1 to 5,
      "workload": number greater than 0,
      "suggestedAssigneeId": "team member userId or null",
      "suggestedAssigneeName": "matching team member fullName/username or null",
      "suggestedReason": "Vietnamese reason or null"
    }}
  ]
}}

Hard rules:
- Return {task_count_rule}.
- Keep each task title concise, under 90 characters.
- Keep each task description concise, one sentence, under 240 characters.
- For detailed orders, distribute requirements across tasks instead of repeating all specs in every task.
- deadline must be copied from extracted fields.deadline exactly when present.
- priority must be an integer: 1 lowest, 3 medium, 5 highest.
- workload is estimated effort hours, must be greater than 0.
- suggestedAssigneeId is optional and must be one of the provided team member userId values.
- If no suitable member exists, set suggestedAssigneeId and suggestedAssigneeName to null.
- A member with empty jobLabels is not suitable for any specialized task.
- Only suggest a member when their jobLabels semantically match the main action of the task. If a member has a jobLabel directly matching a task action (such as 'phối trộn' or 'trộn' for blending tasks, 'medium roast', 'rang', or 'rang xay' for roasting tasks, 'xay' for grinding, 'đóng gói' for packaging), you MUST suggest that member for that task.
- For primary execution tasks (e.g., 'Rang 129kg', 'Phối trộn', 'Xay hạt', 'Đóng gói'), match members whose jobLabels contain relevant execution keywords (e.g., 'rang', 'rang xay', 'phối trộn', 'trộn', 'medium roast', 'sản xuất', 'xay', 'đóng gói') regardless of accents/diacritics or casing. ALWAYS prioritize assigning the specialist member with matching jobLabels (e.g. 'rang', 'rang xay') to the PRIMARY roasting execution task FIRST. Do NOT assign the roaster exclusively to a preliminary preparation task while leaving the main roasting task unassigned (Task 'Rang 129kg').
- A qualified team member CAN be suggested for multiple tasks in the plan if they are the only qualified person for those tasks.
- NEVER evaluate an execution task (roasting, grinding, blending, packaging) as requiring a QC/quality control label, even if its description mentions quality standards. QC label matching is reserved strictly and exclusively for dedicated quality inspection/testing/cupping tasks.
- Never invent a person, username, full name, or userId.
- suggestedAssigneeName must match the selected member's fullName when available, otherwise username.
- Keep the draft in Vietnamese.
- If extracted fields contain detailed order, product, packaging, grinding, roasting, flavor, or quality specs, every provided requirement must appear in either goalTitle, outputTarget, or at least one task title/description. Do not omit provided specs.
- The draft is not saved data, so do not include id, status, createdAt, totalTasks, or database fields.

Intent-specific rules:
- PRODUCTION_PLAN: create production workflow tasks, such as preparation, execution, checking/QC, and completion.
- PRODUCTION_PLAN with detailed order specs: create enough tasks to cover material preparation/blending, roasting target, grinding target, packaging material/labeling, carton packing, QC requirements, and delivery deadline.
- PRODUCTION_PLAN: always include at least one QC/kiểm tra chất lượng task.
- PRODUCTION_PLAN: goalTitle and outputTarget must include productName, quantity, and unit when present.
- OPERATION_TASK: create only internal operation tasks based on title/area.
- OPERATION_TASK: do not create production/roasting/quantity tasks unless the extracted title explicitly says so.

Now produce the draft JSON.
""".strip()


def _build_revise_prompt(request: ReviseRequest) -> str:
    draft_json = json.dumps(request.draft.model_dump(), ensure_ascii=False, indent=2)
    members_json = json.dumps([member.model_dump() for member in request.members], ensure_ascii=False, indent=2)
    now = _local_now()

    return f"""
You are ORCA AI v2 revise module for a Vietnamese workshop/task management app.

Your only job is to revise an existing draft Goal/Tasks according to the user's revision instruction.
Do not classify intent. Do not create a new plan from scratch. Do not save data. Do not explain.

Current local datetime: {now.isoformat(timespec="minutes")}
Timezone: Asia/Ho_Chi_Minh

User revision instruction:
{request.instruction}

Current draft:
{draft_json}

Team members available for suggested assignment:
{members_json}

Output only one JSON object matching this exact schema:
{{
  "goalTitle": "string",
  "outputTarget": "string",
  "deadline": "ISO datetime string or null",
  "priority": integer from 1 to 5,
  "tasks": [
    {{
      "title": "string",
      "description": "string",
      "priority": integer from 1 to 5,
      "workload": number greater than 0,
      "suggestedAssigneeId": "team member userId or null",
      "suggestedAssigneeName": "matching team member fullName/username or null",
      "suggestedReason": "Vietnamese reason or null"
    }}
  ]
}}

Hard rules:
- Preserve the existing draft context. This is a revision, not a fresh plan.
- Only change the fields directly requested by the instruction.
- If the instruction asks to reduce to N tasks, return exactly N tasks and keep the most important existing tasks.
- If the instruction asks to add a task, keep all existing tasks unchanged and append exactly one new relevant task.
- If the instruction asks to split a task, keep unrelated tasks unchanged and replace only the target task with exactly two smaller tasks.
- If the instruction asks to change deadline, change only deadline and keep tasks unchanged unless explicitly requested.
- If the instruction asks to increase priority for a specific task type, change only matching task priority.
- If the instruction is unclear or too subjective, return the current draft unchanged.
- Never save data and never include id, status, createdAt, totalTasks, or database fields.
- suggestedAssigneeId is optional and must be one of the provided team member userId values.
- If no suitable member exists, set suggestedAssigneeId and suggestedAssigneeName to null.
- A member with empty jobLabels is not suitable for any specialized task.
- Never invent a person, username, full name, or userId.
- Keep the draft in Vietnamese.
- If extracted fields contain detailed order, product, packaging, grinding, roasting, flavor, or quality specs, every provided requirement must appear in either goalTitle, outputTarget, or at least one task title/description. Do not omit provided specs.

Deadline rules:
- Return deadline as ISO local datetime string without timezone offset, e.g. "2026-06-09T18:00:00".
- Resolve Vietnamese relative dates using Current local datetime.
- "hôm nay" means today.
- If exact time is present, use that exact time.

Now return the revised draft JSON.
""".strip()



def _is_detailed_production_order(fields: dict[str, Any]) -> bool:
    detail_keys = {
        "orderCode", "startDate", "deliveryDate", "composition", "materialSources",
        "roastLevel", "targetAgtron", "grindType", "targetMicron", "flavorNotes",
        "packagingSpec", "expectedPackageCount", "packageMaterial", "labelSpec",
        "cartonSpec", "qualityRequirements",
    }
    if len(detail_keys.intersection(fields.keys())) >= 3:
        return True
    return len(fields.keys()) >= 10
def _validate_plan_input(request: PlanRequest) -> None:
    if request.intent == "UNKNOWN":
        raise GeminiPlanInputError("Cannot create a plan for UNKNOWN intent.")

    fields = request.fields or {}
    if request.intent == "PRODUCTION_PLAN":
        if _is_blank(fields.get("productName")) or _is_generic_product_name(fields.get("productName")):
            fields["productName"] = "Cà phê"
        required = ["productName", "quantity", "unit", "deadline"]
    elif request.intent == "OPERATION_TASK":
        required = ["title", "deadline"]
    else:
        raise GeminiPlanInputError(f"Unsupported intent for plan: {request.intent}")

    missing = [field for field in required if _is_blank(fields.get(field))]
    if missing:
        raise GeminiPlanInputError(f"Cannot create plan because required fields are missing: {', '.join(missing)}.")


def _validate_plan_output(draft: PlanDraftResponse, request: PlanRequest) -> PlanDraftResponse:
    fields = request.fields or {}
    if fields.get("deadline") and draft.deadline != fields["deadline"]:
        raise GeminiPlanError("Gemini plan changed the extracted deadline.")

    allowed_members = {member.userId: member for member in request.members}
    draft.tasks = [_sanitize_assignee(task, allowed_members) for task in draft.tasks]

    if request.intent == "PRODUCTION_PLAN":
        draft.tasks = [_fill_missing_production_assignee(task, allowed_members) for task in draft.tasks]
        draft.tasks = [_sanitize_production_assignee(task, allowed_members) for task in draft.tasks]
        if _is_detailed_production_order(fields):
            _require_task_count(draft.tasks, minimum=6, maximum=12)
        else:
            _require_task_count(draft.tasks, minimum=3, maximum=6)
    elif request.intent == "OPERATION_TASK":
        _require_task_count(draft.tasks, minimum=2, maximum=4)
        draft.tasks = [_sanitize_operation_assignee(task, allowed_members) for task in draft.tasks]
        text = _operation_scope_text(draft)
        forbidden_terms = ["sản xuất", "rang 120", "kg arabica", "kg robusta"]
        found_terms = [term for term in forbidden_terms if term in text]
        if found_terms:
            raise GeminiPlanError(f"Gemini plan added production terms to an operation task: {found_terms}.")

    return draft


def _validate_revise_output(draft: PlanDraftResponse, request: ReviseRequest) -> PlanDraftResponse:
    original = request.draft
    instruction = _normalize_revise_instruction(request.instruction)
    allowed_members = {member.userId: member for member in request.members}
    draft.tasks = [_sanitize_assignee(task, allowed_members) for task in draft.tasks]

    if not _mentions_any(instruction, [
        "tieu de", "title", "goal", "muc tieu", "output", "ket qua",
        "sua", "doi", "thanh", "ten", "doi ten",
        "arabica", "robusta", "blend", "cafe", "ca phe", "me", "san pham",
    ]):
        _require_equal(draft.goalTitle, original.goalTitle, "Gemini revise changed goalTitle without instruction.")
        _require_equal(draft.outputTarget, original.outputTarget, "Gemini revise changed outputTarget without instruction.")

    if not _mentions_any(instruction, ["priority", "uu tien"]):
        _require_equal(draft.priority, original.priority, "Gemini revise changed goal priority without instruction.")

    requested_task_count = _requested_task_count(instruction)
    if requested_task_count is not None:
        if len(draft.tasks) != requested_task_count:
            raise GeminiReviseError(
                f"Gemini revise returned {len(draft.tasks)} tasks, expected {requested_task_count} tasks."
            )
    elif not _mentions_any(instruction, [
        "them", "xoa", "rut gon", "tach", "task", "cong viec",
        "sua", "doi", "thanh", "arabica", "robusta", "blend", "cafe", "ca phe", "me", "san pham", "loai",
    ]):
        _require_task_signatures_equal(draft, original)

    requested_deadline = _requested_deadline(instruction)
    if requested_deadline is not None:
        _require_equal(draft.deadline, requested_deadline, "Gemini revise did not apply requested deadline.")
    elif not _mentions_any(instruction, ["deadline", "han", "thoi han", "doi deadline", "doi han"]):
        _require_equal(draft.deadline, original.deadline, "Gemini revise changed deadline without instruction.")

    if _mentions_any(instruction, ["uu tien", "priority"]) and _mentions_any(instruction, ["cao nhat", "cao"]):
        _validate_priority_revise(draft, original, instruction)

    return draft


def _normalize_revise_output(draft: PlanDraftResponse, request: ReviseRequest) -> PlanDraftResponse:
    instruction = _normalize_revise_instruction(request.instruction)
    original = request.draft

    requested_task_count = _requested_task_count(instruction)
    if requested_task_count is not None and requested_task_count <= len(original.tasks):
        draft.goalTitle = original.goalTitle
        draft.outputTarget = original.outputTarget
        draft.deadline = original.deadline
        draft.priority = original.priority
        draft.tasks = [task.model_copy(deep=True) for task in original.tasks[:requested_task_count]]
        return draft

    requested_deadline = _requested_deadline(instruction)
    if requested_deadline is not None:
        draft.goalTitle = original.goalTitle
        draft.outputTarget = original.outputTarget
        draft.priority = original.priority
        draft.deadline = requested_deadline
        draft.tasks = [task.model_copy(deep=True) for task in original.tasks]
        return draft

    if _mentions_any(instruction, ["uu tien", "priority"]) and _mentions_any(instruction, ["cao nhat", "cao"]):
        target_indexes = _target_task_indexes(original, instruction)
        if target_indexes:
            draft.goalTitle = original.goalTitle
            draft.outputTarget = original.outputTarget
            draft.deadline = original.deadline
            draft.priority = original.priority
            draft.tasks = [task.model_copy(deep=True) for task in original.tasks]
            for index in target_indexes:
                if index < len(draft.tasks):
                    draft.tasks[index].priority = 5
            return draft

    return draft


def _revise_with_safe_rule(request: ReviseRequest) -> PlanDraftResponse | None:
    instruction = _normalize_revise_instruction(request.instruction)
    original = request.draft

    requested_task_count = _requested_task_count(instruction)
    if requested_task_count is not None and requested_task_count <= len(original.tasks):
        draft = original.model_copy(deep=True)
        draft.tasks = [task.model_copy(deep=True) for task in original.tasks[:requested_task_count]]
        return draft

    requested_deadline = _requested_deadline(instruction)
    if requested_deadline is not None:
        draft = original.model_copy(deep=True)
        draft.deadline = requested_deadline
        return draft

    if _mentions_any(instruction, ["uu tien", "priority"]) and _mentions_any(instruction, ["cao nhat", "cao"]):
        target_indexes = _target_task_indexes(original, instruction)
        if target_indexes:
            draft = original.model_copy(deep=True)
            for index in target_indexes:
                if index < len(draft.tasks):
                    draft.tasks[index].priority = 5
            return draft

    if _mentions_any(instruction, ["them"]) and _has_qc_intent(instruction):
        draft = original.model_copy(deep=True)
        assignee = _find_member_by_labels(
            {member.userId: member for member in request.members},
            ["qc", "kiem", "kiem tra", "chat luong", "quality"],
        )
        task = TaskDraft(
            title="Kiểm tra chất lượng cuối cùng",
            description="Kiểm tra chất lượng tổng thể và xác nhận thành phẩm đạt yêu cầu trước khi hoàn tất.",
            priority=3,
            workload=1.0,
        )
        if assignee is not None:
            task.suggestedAssigneeId = assignee.userId
            task.suggestedAssigneeName = assignee.fullName or assignee.username
            task.suggestedReason = "Phù hợp vì có nhãn QC/kiểm tra chất lượng."
        draft.tasks.append(task)
        return draft

    if _mentions_any(instruction, ["tach"]) and _mentions_any(instruction, ["dong goi"]):
        packaging_index = _find_packaging_task_index(original.tasks)
        if packaging_index is not None:
            draft = original.model_copy(deep=True)
            original_task = original.tasks[packaging_index]
            first_workload = max(round(original_task.workload / 2, 2), 0.25)
            second_workload = max(round(original_task.workload - first_workload, 2), 0.25)
            split_tasks = [
                TaskDraft(
                    title="Chuẩn bị bao bì và nhãn thành phẩm",
                    description="Chuẩn bị bao bì, nhãn và khu vực đóng gói trước khi xử lý thành phẩm.",
                    priority=original_task.priority,
                    workload=first_workload,
                    suggestedAssigneeId=original_task.suggestedAssigneeId,
                    suggestedAssigneeName=original_task.suggestedAssigneeName,
                    suggestedReason=original_task.suggestedReason,
                ),
                TaskDraft(
                    title="Đóng gói và kiểm tra bao gói thành phẩm",
                    description="Đóng gói thành phẩm, kiểm tra bao gói và sẵn sàng bàn giao.",
                    priority=original_task.priority,
                    workload=second_workload,
                    suggestedAssigneeId=original_task.suggestedAssigneeId,
                    suggestedAssigneeName=original_task.suggestedAssigneeName,
                    suggestedReason=original_task.suggestedReason,
                ),
            ]
            draft.tasks = draft.tasks[:packaging_index] + split_tasks + draft.tasks[packaging_index + 1 :]
            return draft

    return None


def _sanitize_assignee(task: TaskDraft, allowed_members: dict[str, Any]) -> TaskDraft:
    if not task.suggestedAssigneeId:
        task.suggestedAssigneeName = None
        return task

    member = allowed_members.get(task.suggestedAssigneeId)
    if member is None:
        task.suggestedAssigneeId = None
        task.suggestedAssigneeName = None
        task.suggestedReason = "AI gợi ý thành viên không thuộc team nên hệ thống đã bỏ gợi ý này."
        return task

    task.suggestedAssigneeName = member.fullName or member.username
    return task


def _sanitize_operation_assignee(task: TaskDraft, allowed_members: dict[str, Any]) -> TaskDraft:
    if not task.suggestedAssigneeId:
        return task

    member = allowed_members.get(task.suggestedAssigneeId)
    if member is None:
        return task

    labels = _normalize_match_text(" ".join(member.jobLabels))
    operation_keywords = ["ve sinh", "don", "operation", "van hanh", "dong goi", "sap xep"]
    if any(keyword in labels for keyword in operation_keywords):
        return task

    task.suggestedAssigneeId = None
    task.suggestedAssigneeName = None
    task.suggestedReason = "Chưa có thành viên có nhãn vận hành phù hợp."
    return task


def _sanitize_production_assignee(task: TaskDraft, allowed_members: dict[str, Any]) -> TaskDraft:
    if not task.suggestedAssigneeId:
        return task

    member = allowed_members.get(task.suggestedAssigneeId)
    if member is None:
        return task

    labels = _normalize_match_text(" ".join(member.jobLabels))
    if not labels.strip():
        return _clear_assignee(task, "Chưa có thành viên có nhãn công việc phù hợp.")

    text = _normalize_match_text(task_scope_text_for_matching(task))
    packaging_keywords = ["dong goi", "dan nhan", "bao bi", "pack"]
    production_keywords = ["rang", "san xuat", "van hanh may", "thuc hien san xuat"]

    if _has_qc_intent(text):
        if not _has_matching_label(labels, ["qc", "kiem", "kiem tra", "chat luong", "quality"]):
            return _clear_assignee(task, "Chưa có thành viên có nhãn QC/kiểm tra chất lượng phù hợp.")
        return task

    if any(keyword in text for keyword in packaging_keywords):
        if not any(keyword in labels for keyword in packaging_keywords):
            return _clear_assignee(task, "Chưa có thành viên có nhãn đóng gói phù hợp.")
        return task

    if any(keyword in text for keyword in production_keywords):
        if not any(keyword in labels for keyword in production_keywords):
            return _clear_assignee(task, "Chưa có thành viên có nhãn rang/sản xuất phù hợp.")
        return task

    return task


def _fill_missing_production_assignee(task: TaskDraft, allowed_members: dict[str, Any]) -> TaskDraft:
    if task.suggestedAssigneeId:
        return task

    text = _normalize_match_text(task_scope_text_for_matching(task))
    if _has_qc_intent(text):
        member = _find_member_by_labels(allowed_members, ["qc", "kiem", "kiem tra", "chat luong", "quality"])
        return _assign_if_found(task, member, "Phù hợp vì có nhãn QC/kiểm tra chất lượng.")

    if any(keyword in text for keyword in ["dong goi", "dan nhan", "bao bi", "pack"]):
        member = _find_member_by_labels(allowed_members, ["dong goi", "bao bi", "pack"])
        return _assign_if_found(task, member, "Phù hợp vì có nhãn đóng gói.")

    if any(keyword in text for keyword in ["rang", "san xuat", "van hanh may", "thuc hien san xuat"]):
        member = _find_member_by_labels(allowed_members, ["rang", "san xuat", "senior"])
        return _assign_if_found(task, member, "Phù hợp vì có nhãn rang/sản xuất.")

    return task


def _find_member_by_labels(allowed_members: dict[str, Any], keywords: list[str]) -> Any | None:
    for member in allowed_members.values():
        labels = _normalize_match_text(" ".join(member.jobLabels))
        if labels and _has_matching_label(labels, keywords):
            return member
    return None


def _assign_if_found(task: TaskDraft, member: Any | None, reason: str) -> TaskDraft:
    if member is None:
        return task
    task.suggestedAssigneeId = member.userId
    task.suggestedAssigneeName = member.fullName or member.username
    task.suggestedReason = reason
    return task


def _has_matching_label(labels: str, keywords: list[str]) -> bool:
    return any(keyword in labels for keyword in keywords)


def _has_qc_intent(text: str) -> bool:
    normalized = _normalize_match_text(text)
    return any(keyword in normalized for keyword in ["qc", "chat luong", "quality", "kiem dinh", "cupping", "thu nem"])


def _clear_assignee(task: TaskDraft, reason: str) -> TaskDraft:
    task.suggestedAssigneeId = None
    task.suggestedAssigneeName = None
    task.suggestedReason = reason
    return task


def _require_task_count(tasks: list[TaskDraft], minimum: int, maximum: int) -> None:
    if not minimum <= len(tasks) <= maximum:
        raise GeminiPlanError(f"Gemini plan returned {len(tasks)} tasks, expected {minimum}..{maximum}.")


def _requested_task_count(instruction: str) -> int | None:
    if _mentions_any(instruction, ["tach", "them"]):
        return None
    if not _mentions_any(instruction, ["rut gon", "giam", "con", "chi"]):
        return None

    match = re.search(r"(\d+)\s*task", instruction)
    if not match:
        return None
    return max(1, int(match.group(1)))


def _requested_deadline(instruction: str) -> str | None:
    if not _mentions_any(instruction, ["deadline", "han", "thoi han", "doi"]):
        return None
    time_match = re.search(r"(\d{1,2})(?::|h)(\d{2})?", instruction)
    if not time_match:
        return None
    hour = int(time_match.group(1))
    minute = int(time_match.group(2) or 0)
    if hour > 23 or minute > 59:
        return None
    if "hom nay" in instruction:
        return _local_now().replace(hour=hour, minute=minute, second=0, microsecond=0).isoformat()
    return None


def _validate_priority_revise(draft: PlanDraftResponse, original: PlanDraftResponse, instruction: str) -> None:
    target_indexes = _target_task_indexes(original, instruction)
    if not target_indexes:
        return

    if not any(draft.tasks[index].priority == 5 for index in target_indexes if index < len(draft.tasks)):
        raise GeminiReviseError("Gemini revise did not increase the requested task priority to 5.")

    for index, original_task in enumerate(original.tasks):
        if index >= len(draft.tasks) or index in target_indexes:
            continue
        revised_task = draft.tasks[index]
        if revised_task.priority != original_task.priority:
            raise GeminiReviseError(f"Gemini revise changed unrelated task priority at index {index}.")


def _target_task_indexes(draft: PlanDraftResponse, instruction: str) -> list[int]:
    if "rang" in instruction:
        keywords = ["rang"]
    elif "san xuat" in instruction:
        keywords = ["san xuat", "thuc hien"]
    elif "dong goi" in instruction:
        keywords = ["dong goi"]
    elif _has_qc_intent(instruction):
        keywords = ["qc", "kiem tra", "chat luong"]
    else:
        keywords = []

    if not keywords:
        return []

    title_indexes = []
    for index, task in enumerate(draft.tasks):
        text = _normalize_match_text(task.title)
        if "rang" in keywords and _mentions_any(text, ["kiem tra", "chat luong", "qc", "dong goi"]):
            continue
        if any(keyword in text for keyword in keywords):
            title_indexes.append(index)
    if title_indexes:
        return title_indexes

    indexes = []
    for index, task in enumerate(draft.tasks):
        text = _normalize_match_text(task_scope_text_for_matching(task))
        if any(keyword in text for keyword in keywords):
            indexes.append(index)
    return indexes


def _find_packaging_task_index(tasks: list[TaskDraft]) -> int | None:
    for index, task in enumerate(tasks):
        text = _normalize_match_text(task_scope_text_for_matching(task))
        if any(keyword in text for keyword in ["dong goi", "bao bi", "dan nhan", "thanh pham"]):
            return index
    return None


def _require_task_signatures_equal(draft: PlanDraftResponse, original: PlanDraftResponse) -> None:
    _require_equal(len(draft.tasks), len(original.tasks), "Gemini revise changed task count without instruction.")
    for index, original_task in enumerate(original.tasks):
        revised_task = draft.tasks[index]
        _require_equal(revised_task.title, original_task.title, f"Gemini revise changed task title at index {index}.")
        _require_equal(
            revised_task.description,
            original_task.description,
            f"Gemini revise changed task description at index {index}.",
        )
        _require_equal(
            revised_task.priority,
            original_task.priority,
            f"Gemini revise changed task priority at index {index}.",
        )
        _require_equal(
            revised_task.workload,
            original_task.workload,
            f"Gemini revise changed task workload at index {index}.",
        )


def _require_equal(actual: Any, expected: Any, message: str) -> None:
    if actual != expected:
        raise GeminiReviseError(message)


def _mentions_any(text: str, keywords: list[str]) -> bool:
    return any(keyword in text for keyword in keywords)


def _is_ambiguous_revise_instruction(instruction: str) -> bool:
    normalized = _normalize_revise_instruction(instruction).strip()
    ambiguous = [
        "lam lai cho hay hon",
        "lam hay hon",
        "sua cho hay hon",
        "toi uu hon",
        "on hon",
    ]
    return normalized in ambiguous


def _draft_text(draft: PlanDraftResponse) -> str:
    parts = [draft.goalTitle, draft.outputTarget, draft.deadline or ""]
    for task in draft.tasks:
        parts.extend([task.title, task.description, task.suggestedReason or ""])
    return " ".join(parts).lower()


def task_text_for_matching(task: TaskDraft) -> str:
    return " ".join([task.title, task.description, task.suggestedReason or ""]).lower()


def task_scope_text_for_matching(task: TaskDraft) -> str:
    return " ".join([task.title, task.description]).lower()


def _normalize_match_text(value: str) -> str:
    text = value.lower().replace("đ", "d")
    text = unicodedata.normalize("NFKD", text)
    return "".join(char for char in text if not unicodedata.combining(char))


def _normalize_revise_instruction(value: str) -> str:
    text = _normalize_match_text(value)
    replacements = {
        r"\b(taks|tas|taskk|tsk)\b": "task",
        r"\b(dedline|deadlin|dealin|dealine|dedlin)\b": "deadline",
        r"\buu\s*tin\b": "uu tien",
        r"\buu\s*tienn\b": "uu tien",
        r"\bu\s*tien\b": "uu tien",
        r"\brut\s*gon\b": "rut gon",
        r"\bcao\s*nhat\b": "cao nhat",
    }
    for pattern, replacement in replacements.items():
        text = re.sub(pattern, replacement, text)
    return re.sub(r"\s+", " ", text).strip()


def _operation_scope_text(draft: PlanDraftResponse) -> str:
    parts = [draft.goalTitle, draft.outputTarget]
    for task in draft.tasks:
        parts.extend([task.title, task.description])
    return " ".join(parts).lower()


def _is_blank(value: Any) -> bool:
    if value is None:
        return True
    if isinstance(value, str) and not value.strip():
        return True
    return False


def _is_generic_product_name(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    normalized = value.strip().lower()
    return normalized in {"sản phẩm", "hàng", "đơn hàng", "mặt hàng"}


def _local_now() -> datetime:
    try:
        return datetime.now(ZoneInfo("Asia/Ho_Chi_Minh")).replace(second=0, microsecond=0)
    except Exception:
        return datetime.now().replace(second=0, microsecond=0)


def _generate_json_object(prompt: str, max_output_tokens: int, error_cls: Type[RuntimeError]) -> dict:
    token_attempts = [max_output_tokens]
    retry_tokens = min(max(max_output_tokens * 2, 4096), 16384)
    if retry_tokens not in token_attempts:
        token_attempts.append(retry_tokens)

    last_error: RuntimeError | None = None
    for attempt_index, token_limit in enumerate(token_attempts):
        attempt_prompt = prompt
        if attempt_index > 0:
            attempt_prompt = f"""
{prompt}

IMPORTANT RETRY INSTRUCTION:
Your previous response was not valid complete JSON, likely because it was truncated.
Return exactly one complete JSON object only. Keep descriptions concise enough to fit.
Do not include markdown, comments, or trailing text.
""".strip()

        try:
            return _generate_json_object_once(attempt_prompt, token_limit, error_cls)
        except error_cls as exc:
            last_error = exc
            if not _should_retry_json_generation_error(str(exc)) or attempt_index == len(token_attempts) - 1:
                raise

    raise last_error or error_cls("Gemini JSON generation failed.")


def _generate_json_object_once(prompt: str, max_output_tokens: int, error_cls: Type[RuntimeError]) -> dict:
    provider = settings.ai_provider.replace("-", "_")
    if provider in {"gemini", "gemini_api", "google_ai"}:
        return _generate_json_object_with_gemini_api(prompt, max_output_tokens, error_cls)
    if provider in {"vertex", "vertex_ai"}:
        return _generate_json_object_with_vertex_ai(prompt, max_output_tokens, error_cls)
    raise error_cls(f"Unsupported AI_PROVIDER: {settings.ai_provider}")


def _should_retry_json_generation_error(message: str) -> bool:
    retry_markers = [
        "output is not json",
        "output contains invalid json",
        "unterminated string",
        "expecting property name",
        "expecting value",
        "expecting ',' delimiter",
    ]
    normalized = message.lower()
    return any(marker in normalized for marker in retry_markers)


def _generate_json_object_with_gemini_api(prompt: str, max_output_tokens: int, error_cls: Type[RuntimeError]) -> dict:
    if not settings.gemini_api_key:
        raise error_cls("GEMINI_API_KEY is required when AI_PROVIDER=gemini_api.")

    payload = {
        "contents": [
            {
                "role": "user",
                "parts": [{"text": prompt}],
            }
        ],
        "generationConfig": {
            "temperature": 0.1,
            "topP": 0.8,
            "maxOutputTokens": max_output_tokens,
            "responseMimeType": "application/json",
        },
    }

    url = f"https://generativelanguage.googleapis.com/v1beta/models/{settings.gemini_model}:generateContent"
    try:
        response = httpx.post(
            url,
            params={"key": settings.gemini_api_key},
            json=payload,
            timeout=settings.gemini_timeout_seconds,
        )
        response.raise_for_status()
    except httpx.HTTPStatusError as exc:
        body = exc.response.text[:1000]
        raise error_cls(f"Gemini API returned HTTP {exc.response.status_code}: {body}") from exc
    except httpx.RequestError as exc:
        raise error_cls(f"Cannot reach Gemini API: {exc}") from exc

    raw_text = _read_gemini_text(response.json(), error_cls)
    return _parse_json_object(raw_text, error_cls)


def _generate_json_object_with_vertex_ai(prompt: str, max_output_tokens: int, error_cls: Type[RuntimeError]) -> dict:
    if not settings.vertex_project_id:
        raise error_cls("VERTEX_PROJECT_ID is required when AI_PROVIDER=vertex.")

    payload = {
        "contents": [
            {
                "role": "user",
                "parts": [{"text": prompt}],
            }
        ],
        "generationConfig": {
            "temperature": 0.1,
            "topP": 0.8,
            "maxOutputTokens": max_output_tokens,
            "responseMimeType": "application/json",
        },
    }

    model_name = _vertex_model_name()
    service_endpoint = _vertex_service_endpoint()
    url = f"https://{service_endpoint}/v1/{model_name}:generateContent"
    try:
        response = httpx.post(
            url,
            headers={"Authorization": f"Bearer {_vertex_access_token(error_cls)}"},
            json=payload,
            timeout=settings.gemini_timeout_seconds,
        )
        response.raise_for_status()
    except httpx.HTTPStatusError as exc:
        body = exc.response.text[:1000]
        raise error_cls(f"Vertex AI API returned HTTP {exc.response.status_code}: {body}") from exc
    except httpx.RequestError as exc:
        raise error_cls(f"Cannot reach Vertex AI API: {exc}") from exc

    raw_text = _read_gemini_text(response.json(), error_cls)
    return _parse_json_object(raw_text, error_cls)


def _vertex_model_name() -> str:
    model = _clean_env_value(settings.vertex_model)
    if model.startswith("projects/"):
        return model
    return (
        f"projects/{_clean_env_value(settings.vertex_project_id)}/locations/{_clean_env_value(settings.vertex_location)}"
        f"/publishers/google/models/{model}"
    )


def _vertex_service_endpoint() -> str:
    location = _clean_env_value(settings.vertex_location)
    if location == "global":
        return "aiplatform.googleapis.com"
    return f"{location}-aiplatform.googleapis.com"


def _clean_env_value(value: str) -> str:
    return value.strip().strip('"').strip("'")


def _vertex_access_token(error_cls: Type[RuntimeError]) -> str:
    try:
        import google.auth
        from google.auth.transport.requests import Request
    except ImportError as exc:
        raise error_cls("google-auth is required when AI_PROVIDER=vertex. Run pip install -r requirements.txt.") from exc

    try:
        credentials, _ = google.auth.default(scopes=["https://www.googleapis.com/auth/cloud-platform"])
        credentials.refresh(Request())
    except Exception as exc:
        raise error_cls(
            "Cannot get Vertex AI credentials. Set GOOGLE_APPLICATION_CREDENTIALS to a service account JSON "
            "or run gcloud auth application-default login."
        ) from exc

    return credentials.token


def _read_gemini_text(body: dict, error_cls: Type[RuntimeError]) -> str:
    try:
        candidates = body["candidates"]
        parts = candidates[0]["content"]["parts"]
        text = "".join(part.get("text", "") for part in parts)
    except (KeyError, IndexError, TypeError) as exc:
        raise error_cls(f"Gemini API response did not contain text: {body}") from exc

    if not text.strip():
        raise error_cls("Gemini API returned empty text.")
    return text


def _parse_json_object(raw_text: str, error_cls: Type[RuntimeError]) -> dict:
    text = raw_text.strip()
    if text.startswith("```"):
        text = re.sub(r"^```(?:json)?\s*", "", text)
        text = re.sub(r"\s*```$", "", text)

    try:
        parsed = json.loads(text)
    except json.JSONDecodeError:
        parsed = None
        match = re.search(r"\{.*", text, flags=re.DOTALL)
        if match:
            candidate = match.group(0).rstrip()
            candidate = re.sub(r"\s*```$", "", candidate)
            for suffix in ["", "}", "}\n", "\n}", "}}", "}\n}", "}]}", '"}}', '"}\n}', '"\n}', '}\n}\n}']:
                try:
                    parsed = json.loads(candidate + suffix)
                    break
                except json.JSONDecodeError:
                    pass
        if parsed is None:
            raise error_cls(f"Gemini output contains invalid JSON: {raw_text[:1000]}")

    if not isinstance(parsed, dict):
        raise error_cls("Gemini output must be a JSON object.")
    return parsed





