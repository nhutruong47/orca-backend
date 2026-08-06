from __future__ import annotations

import re
from datetime import datetime, timedelta
from typing import Any

from app.models import (
    ExtractRequest,
    ExtractResponse,
    PlanDraftResponse,
    PlanRequest,
    ReviseRequest,
    TaskDraft,
    TeamMemberContext,
)


def extract(request: ExtractRequest) -> ExtractResponse:
    text = request.text.strip()
    normalized = text.lower()

    if any(keyword in normalized for keyword in ["quét", "dọn", "sắp xếp", "vệ sinh", "kiểm tra máy"]):
        return _extract_operation(text, normalized)

    has_production_keyword = any(keyword in normalized for keyword in ["sản xuất", "rang", "kg", "hộp", "robusta", "arabica"])
    has_make_with_quantity = "làm" in normalized and re.search(r"\d+", normalized)
    if has_production_keyword or has_make_with_quantity:
        return _extract_production(text, normalized)

    return ExtractResponse(
        intent="UNKNOWN",
        confidence=0.35,
        fields={},
        missingFields=["taskDescription"],
        clarifyingQuestion="Anh/chị muốn tạo công việc gì và hạn hoàn thành khi nào?",
    )


def plan(request: PlanRequest) -> PlanDraftResponse:
    if request.intent == "PRODUCTION_PLAN":
        return _plan_production(request.fields, request.members)

    if request.intent == "OPERATION_TASK":
        return _plan_operation(request.fields, request.members)

    raise ValueError("Cannot create a plan for UNKNOWN intent.")


def revise(request: ReviseRequest) -> PlanDraftResponse:
    instruction = request.instruction.lower()
    draft = request.draft.model_copy(deep=True)

    count_match = re.search(r"(\d+)\s*task", instruction)
    if "rút gọn" in instruction and count_match:
        target_count = max(1, int(count_match.group(1)))
        draft.tasks = draft.tasks[:target_count]
        return draft

    if "kiểm tra chất lượng" in instruction or "qc" in instruction:
        assignee = _find_member(request.members, ["qc", "kiểm", "quality"])
        draft.tasks.append(
            TaskDraft(
                title="Kiểm tra chất lượng cuối cùng",
                description="Xác nhận kết quả hoàn thành đạt yêu cầu trước khi đóng mục tiêu.",
                priority=3,
                workload=1.0,
                suggestedAssigneeId=assignee["id"] if assignee else None,
                suggestedAssigneeName=assignee["name"] if assignee else None,
                suggestedReason=assignee["reason"] if assignee else "Chưa có thành viên có nhãn QC rõ ràng.",
            )
        )
        return draft

    new_deadline = _infer_deadline(instruction)
    if new_deadline and ("deadline" in instruction or "hạn" in instruction or "đổi" in instruction):
        draft.deadline = new_deadline
        return draft

    priority_match = re.search(r"(cao nhất|ưu tiên cao|priority cao)", instruction)
    if priority_match:
        target = _find_priority_target_task(draft.tasks, instruction)
        if target:
            target.priority = 5
        return draft

    return draft


def _extract_production(text: str, normalized: str) -> ExtractResponse:
    quantity_match = re.search(r"(\d+(?:[.,]\d+)?)\s*(kg|hộp|túi|bao|cái)?", normalized)
    quantity = None
    unit = None
    if quantity_match:
        quantity = float(quantity_match.group(1).replace(",", "."))
        if quantity.is_integer():
            quantity = int(quantity)
        unit = quantity_match.group(2) or "đơn vị"

    product = None
    if "robusta" in normalized:
        product = "Robusta"
    elif "arabica" in normalized:
        product = "Arabica"
    elif "cà phê" in normalized:
        product = "cà phê"

    deadline = _infer_deadline(normalized)
    missing = []
    if product is None:
        missing.append("productName")
    if quantity is None:
        missing.append("quantity")
    if deadline is None:
        missing.append("deadline")

    fields: dict[str, Any] = {}
    if product:
        fields["productName"] = product
    if quantity is not None:
        fields["quantity"] = quantity
        fields["unit"] = unit
    if deadline:
        fields["deadline"] = deadline
    fields["priority"] = "HIGH" if "gấp" in normalized or "trước" in normalized else "MEDIUM"
    if product and quantity is not None:
        fields["title"] = f"Sản xuất {quantity}{unit if unit else ''} {product}"

    question = None
    if missing:
        question = _production_question(product, missing)

    return ExtractResponse(
        intent="PRODUCTION_PLAN",
        confidence=0.78 if missing else 0.9,
        fields=fields,
        missingFields=missing,
        clarifyingQuestion=question,
    )


def _extract_operation(text: str, normalized: str) -> ExtractResponse:
    deadline = _infer_deadline(normalized)
    area = "xưởng" if "xưởng" in normalized else None
    if "khu đóng gói" in normalized:
        area = "khu đóng gói"
    elif "máy rang" in normalized:
        area = "máy rang"

    missing = [] if deadline else ["deadline"]
    title = text.rstrip(".")
    fields: dict[str, Any] = {
        "title": title,
        "priority": "MEDIUM",
    }
    if area:
        fields["area"] = area
    if deadline:
        fields["deadline"] = deadline

    return ExtractResponse(
        intent="OPERATION_TASK",
        confidence=0.92 if not missing else 0.82,
        fields=fields,
        missingFields=missing,
        clarifyingQuestion=None
        if not missing
        else "Anh/chị muốn hoàn thành công việc này vào thời điểm nào?",
    )


def _plan_production(fields: dict[str, Any], members: list[TeamMemberContext]) -> PlanDraftResponse:
    title = fields.get("title") or "Kế hoạch sản xuất"
    deadline = fields.get("deadline")
    product = fields.get("productName", "sản phẩm")
    quantity = fields.get("quantity")
    unit = fields.get("unit", "")
    output = f"Hoàn thành {quantity}{unit} {product}" if quantity else f"Hoàn thành {product}"

    production_member = _find_member(members, ["rang", "sản xuất", "senior"])
    qc_member = _find_member(members, ["qc", "kiểm", "quality"])

    return PlanDraftResponse(
        goalTitle=title,
        outputTarget=output,
        deadline=deadline,
        priority=_priority_number(fields.get("priority")),
        tasks=[
            TaskDraft(
                title="Chuẩn bị kế hoạch sản xuất",
                description="Xác nhận yêu cầu, thời hạn và phân chia đầu việc trước khi thực hiện.",
                priority=3,
                workload=1.5,
                suggestedReason="Task điều phối ban đầu, có thể giao cho trưởng nhóm.",
            ),
            TaskDraft(
                title=f"Thực hiện sản xuất {product}",
                description=f"Thực hiện sản xuất theo yêu cầu: {output}.",
                priority=4,
                workload=6.0,
                suggestedAssigneeId=production_member["id"] if production_member else None,
                suggestedAssigneeName=production_member["name"] if production_member else None,
                suggestedReason=production_member["reason"] if production_member else "Chưa có thành viên có nhãn sản xuất/rang rõ ràng.",
            ),
            TaskDraft(
                title="Kiểm tra kết quả hoàn thành",
                description="Kiểm tra số lượng và chất lượng trước khi đóng mục tiêu.",
                priority=3,
                workload=1.5,
                suggestedAssigneeId=qc_member["id"] if qc_member else None,
                suggestedAssigneeName=qc_member["name"] if qc_member else None,
                suggestedReason=qc_member["reason"] if qc_member else "Chưa có thành viên có nhãn QC rõ ràng.",
            ),
        ],
    )


def _plan_operation(fields: dict[str, Any], members: list[TeamMemberContext]) -> PlanDraftResponse:
    title = fields.get("title") or "Công việc vận hành"
    deadline = fields.get("deadline")
    area = fields.get("area", "khu vực liên quan")
    assignee = _find_member(members, ["vệ sinh", "dọn", "operation", "vận hành"])

    return PlanDraftResponse(
        goalTitle=title,
        outputTarget=f"Hoàn thành công việc vận hành tại {area}",
        deadline=deadline,
        priority=_priority_number(fields.get("priority")),
        tasks=[
            TaskDraft(
                title=f"Thực hiện công việc tại {area}",
                description="Thực hiện công việc theo yêu cầu đã được mô tả.",
                priority=2,
                workload=1.0,
                suggestedAssigneeId=assignee["id"] if assignee else None,
                suggestedAssigneeName=assignee["name"] if assignee else None,
                suggestedReason=assignee["reason"] if assignee else "Chưa có thành viên có nhãn vận hành phù hợp.",
            ),
            TaskDraft(
                title="Kiểm tra lại sau khi hoàn thành",
                description="Xác nhận khu vực đã đạt yêu cầu trước khi đóng task.",
                priority=2,
                workload=0.5,
            ),
        ],
    )


def _infer_deadline(normalized: str) -> str | None:
    now = datetime.now().replace(second=0, microsecond=0)

    explicit_time = _extract_time(normalized)
    if "hôm nay" in normalized:
        hour, minute = explicit_time or (17, 0)
        return now.replace(hour=hour, minute=minute).isoformat()
    if "chiều nay" in normalized:
        hour, minute = explicit_time or (14, 0)
        return now.replace(hour=hour, minute=minute).isoformat()
    if "sáng mai" in normalized:
        hour, minute = explicit_time or (9, 0)
        return (now + timedelta(days=1)).replace(hour=hour, minute=minute).isoformat()
    if "chiều mai" in normalized:
        hour, minute = explicit_time or (14, 0)
        return (now + timedelta(days=1)).replace(hour=hour, minute=minute).isoformat()
    if "ngày mai" in normalized or "mai" in normalized:
        hour, minute = explicit_time or (17, 0)
        return (now + timedelta(days=1)).replace(hour=hour, minute=minute).isoformat()
    duration_match = re.search(r"trong\s+(\d+)\s*ngày", normalized)
    if duration_match:
        days = int(duration_match.group(1))
        hour, minute = explicit_time or (17, 0)
        return (now + timedelta(days=days)).replace(hour=hour, minute=minute).isoformat()
    if "thứ hai tuần sau" in normalized or "thứ 2 tuần sau" in normalized:
        days_ahead = (7 - now.weekday()) % 7
        if days_ahead == 0:
            days_ahead = 7
        hour, minute = explicit_time or (17, 0)
        return (now + timedelta(days=days_ahead)).replace(hour=hour, minute=minute).isoformat()
    if "thứ sáu" in normalized or "thứ 6" in normalized:
        days_ahead = (4 - now.weekday()) % 7
        if days_ahead == 0:
            days_ahead = 7
        hour, minute = explicit_time or (17, 0)
        return (now + timedelta(days=days_ahead)).replace(hour=hour, minute=minute).isoformat()
    if "trước" in normalized:
        if explicit_time:
            hour, minute = explicit_time
            return now.replace(hour=hour, minute=minute).isoformat()
        return (now + timedelta(days=3)).replace(hour=17, minute=0).isoformat()
    return None


def _extract_time(normalized: str) -> tuple[int, int] | None:
    time_match = re.search(r"(\d{1,2})(?::|h)(\d{2})?", normalized)
    if not time_match:
        return None
    hour = int(time_match.group(1))
    minute = int(time_match.group(2) or 0)
    if hour > 23 or minute > 59:
        return None
    return hour, minute


def _production_question(product: str | None, missing: list[str]) -> str:
    parts = []
    if "quantity" in missing:
        parts.append(f"số lượng {product}" if product else "số lượng cần sản xuất")
    if "deadline" in missing:
        parts.append("hạn hoàn thành")
    if "productName" in missing:
        parts.append("sản phẩm cần sản xuất")
    joined = " và ".join(parts)
    return f"Anh/chị cho biết thêm {joined} được không?"


def _priority_number(priority: Any) -> int:
    if priority == "HIGH":
        return 4
    if priority == "LOW":
        return 2
    return 3


def _find_member(members: list[TeamMemberContext], keywords: list[str]) -> dict[str, str] | None:
    for member in members:
        labels = " ".join(member.jobLabels).lower()
        if any(keyword in labels for keyword in keywords):
            name = member.fullName or member.username
            return {
                "id": member.userId,
                "name": name,
                "reason": f"Phù hợp vì có nhãn: {', '.join(member.jobLabels)}",
            }
    return None


def _find_priority_target_task(tasks: list[TaskDraft], instruction: str) -> TaskDraft | None:
    if "rang" in instruction:
        keywords = ["rang"]
    elif "sản xuất" in instruction:
        keywords = ["sản xuất", "thực hiện"]
    else:
        keywords = ["rang", "sản xuất", "thực hiện"]

    for task in tasks:
        title = task.title.lower()
        if any(keyword in title for keyword in keywords):
            return task

    for task in tasks:
        text = f"{task.title} {task.description}".lower()
        if any(keyword in text for keyword in keywords):
            return task

    return None
