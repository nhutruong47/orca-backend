from fastapi import FastAPI, HTTPException

from app.config import settings
from app.gemini_ai import GeminiExtractError, GeminiPlanError, GeminiPlanInputError, GeminiReviseError
from app.gemini_ai import extract as gemini_extract
from app.gemini_ai import plan as gemini_plan
from app.gemini_ai import revise as gemini_revise
from app.mock_ai import extract as mock_extract
from app.mock_ai import plan as mock_plan
from app.mock_ai import revise as mock_revise
from app.models import ExtractRequest, ExtractResponse, PlanDraftResponse, PlanRequest, ReviseRequest


app = FastAPI(title="ORCA AI Service", version="0.1.0")


@app.get("/health")
def health() -> dict[str, str]:
    model = settings.vertex_model if settings.ai_provider in {"vertex", "vertex_ai"} else settings.gemini_model
    return {"status": "ok", "mode": settings.ai_v2_mode, "provider": settings.ai_provider, "model": model}


@app.post("/extract", response_model=ExtractResponse)
def extract(request: ExtractRequest) -> ExtractResponse:
    if settings.ai_v2_mode == "mock":
        return mock_extract(request)
    if settings.ai_v2_mode == "gemini":
        try:
            return gemini_extract(request)
        except GeminiExtractError as exc:
            raise HTTPException(status_code=502, detail=str(exc)) from exc
    raise HTTPException(status_code=400, detail=f"Unsupported AI_V2_MODE: {settings.ai_v2_mode}")


@app.post("/plan", response_model=PlanDraftResponse)
def plan(request: PlanRequest) -> PlanDraftResponse:
    if request.intent == "UNKNOWN":
        raise HTTPException(status_code=400, detail="Cannot create a plan for UNKNOWN intent.")

    if settings.ai_v2_mode == "mock":
        try:
            return mock_plan(request)
        except ValueError as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc
    if settings.ai_v2_mode == "gemini":
        try:
            return gemini_plan(request)
        except GeminiPlanInputError as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc
        except GeminiPlanError as exc:
            raise HTTPException(status_code=502, detail=str(exc)) from exc
    raise HTTPException(status_code=400, detail=f"Unsupported AI_V2_MODE: {settings.ai_v2_mode}")


@app.post("/revise", response_model=PlanDraftResponse)
def revise(request: ReviseRequest) -> PlanDraftResponse:
    if settings.ai_v2_mode == "mock":
        return mock_revise(request)
    if settings.ai_v2_mode == "gemini":
        try:
            return gemini_revise(request)
        except GeminiReviseError as exc:
            raise HTTPException(status_code=502, detail=str(exc)) from exc
    raise HTTPException(status_code=400, detail=f"Unsupported AI_V2_MODE: {settings.ai_v2_mode}")

@app.get("/models")
def list_models():
    import httpx
    url = "https://generativelanguage.googleapis.com/v1beta/models"
    try:
        resp = httpx.get(url, params={"key": settings.gemini_api_key}, timeout=10)
        resp.raise_for_status()
        return resp.json()
    except Exception as e:
        return {"error": str(e)}
