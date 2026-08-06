import os

from dotenv import load_dotenv


load_dotenv()


class Settings:
    ai_v2_mode: str = os.getenv("AI_V2_MODE", "mock").lower()
    ai_provider: str = os.getenv("AI_PROVIDER", "gemini_api").lower()
    gemini_api_key: str = os.getenv("GEMINI_API_KEY", "")
    gemini_model: str = os.getenv("GEMINI_MODEL", "gemini-1.5-flash")
    gemini_timeout_seconds: float = float(os.getenv("GEMINI_TIMEOUT_SECONDS", "90"))
    vertex_project_id: str = os.getenv("VERTEX_PROJECT_ID", "")
    vertex_location: str = os.getenv("VERTEX_LOCATION", "us-central1")
    vertex_model: str = os.getenv("VERTEX_MODEL", "gemini-2.0-flash")


settings = Settings()
