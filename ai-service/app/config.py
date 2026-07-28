import os

from dotenv import load_dotenv


load_dotenv()


def _gemini_model_env(name: str, default: str = "gemini-3.5-flash-lite") -> str:
    model = os.getenv(name, default).strip().strip('"').strip("'")
    if model in {"gemini-1.5-flash", "gemini-2.5-flash", "gemini-2.5-flash-lite"}:
        return "gemini-3.5-flash-lite"
    return model


class Settings:
    ai_v2_mode: str = os.getenv("AI_V2_MODE", "mock").lower()
    ai_provider: str = os.getenv("AI_PROVIDER", "gemini_api").lower()
    gemini_api_key: str = os.getenv("GEMINI_API_KEY", "")
    gemini_model: str = _gemini_model_env("GEMINI_MODEL")
    gemini_timeout_seconds: float = float(os.getenv("GEMINI_TIMEOUT_SECONDS", "90"))
    vertex_project_id: str = os.getenv("VERTEX_PROJECT_ID", "")
    vertex_location: str = os.getenv("VERTEX_LOCATION", "us-central1")
    vertex_model: str = _gemini_model_env("VERTEX_MODEL")


settings = Settings()
