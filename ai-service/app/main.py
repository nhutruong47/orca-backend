from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.gemini_ai import GeminiExtractError, GeminiPlanError, GeminiPlanInputError, GeminiReviseError
from app.gemini_ai import extract as gemini_extract
from app.gemini_ai import plan as gemini_plan
from app.gemini_ai import revise as gemini_revise
from app.mock_ai import extract as mock_extract
from app.mock_ai import plan as mock_plan
from app.mock_ai import revise as mock_revise
from app.models import ExtractRequest, ExtractResponse, PlanDraftResponse, PlanRequest, ReviseRequest

# RAG imports
from app.rag.models import RAGRequest as RAGRequestModel, StandardizedAIResponse
from app.rag.rag_service import get_rag_service


app = FastAPI(title="ORCA AI Service", version="0.2.0")

# CORS middleware for frontend access
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure appropriately for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


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


# =============================================================================
# RAG Endpoints (v0.2.0)
# =============================================================================

@app.post("/api/rag/query", response_model=StandardizedAIResponse)
def rag_query(request: RAGRequestModel) -> StandardizedAIResponse:
    """
    Query the RAG system with a natural language question.
    Returns a standardized response with citations.
    """
    try:
        rag_service = get_rag_service()
        return rag_service.query(request)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.get("/api/rag/stats")
def rag_stats() -> dict:
    """
    Get RAG system statistics.
    """
    try:
        rag_service = get_rag_service()
        return rag_service.get_stats()
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/api/rag/index/{source_type}")
def rag_index(source_type: str) -> dict:
    """
    Trigger reindexing for a source type.
    Admin only - would need authentication in production.
    """
    # This would trigger async indexing job
    return {
        "status": "indexing_started",
        "source": source_type,
        "message": f"Reindexing for source '{source_type}' has been queued."
    }


@app.get("/api/rag/sources")
def rag_sources() -> dict:
    """
    List all indexed knowledge sources.
    """
    try:
        rag_service = get_rag_service()
        stats = rag_service.get_stats()
        return {
            "sources": list(stats.get("sources", {}).keys()),
            "documents_by_source": stats.get("sources", {}),
            "total_documents": stats.get("documents_indexed", 0)
        }
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc
