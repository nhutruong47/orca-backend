# ORCA AI Service

Python FastAPI service for ORCA AI v2 workflow.

This service only creates AI drafts. It must not save Goal/Task data to the database.

## Local Setup

From `F:\code\InSchool\Semester8\EXE201\Ver2\ai-service`:

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install --upgrade pip
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

## Run

```powershell
.\run-local.ps1
```

Service URL:

```text
http://localhost:8000
```

Swagger:

```text
http://localhost:8000/docs
```

## Endpoints

```text
GET  /health
POST /extract
POST /plan
POST /revise
```

Phase 1 uses mock mode. Gemini integration comes later.

## AI Modes

Default mode is `mock` when no `.env` file is present.

For Gemini mode, create `.env` in this folder:

```text
AI_V2_MODE=gemini
AI_PROVIDER=gemini_api
GEMINI_API_KEY=your_new_key
GEMINI_MODEL=gemini-1.5-flash
GEMINI_TIMEOUT_SECONDS=30
```

To use Vertex AI instead of Gemini API keys:

```text
AI_V2_MODE=gemini
AI_PROVIDER=vertex
VERTEX_PROJECT_ID=your-google-cloud-project-id
VERTEX_LOCATION=global
VERTEX_MODEL=gemini-3.5-flash
GEMINI_TIMEOUT_SECONDS=90
GOOGLE_APPLICATION_CREDENTIALS=F:\path\to\service-account.json
```

Use the project id shown in Google Cloud Console for the model page, not necessarily the old Gemini API key project.

Vertex AI uses Google Cloud OAuth credentials. You can either set `GOOGLE_APPLICATION_CREDENTIALS` to a service account JSON or run:

```powershell
gcloud auth application-default login
```

In `gemini` mode:

```text
/extract uses the configured provider through httpx
/plan uses the configured provider through httpx
/revise uses the configured provider through httpx for open-ended revise instructions
```

`/revise` also has safe local rules for simple edits like reducing task count, changing deadline, increasing priority, and ignoring unclear subjective instructions. Gemini is used for more open-ended revise instructions.

Mock mode is still available for local integration checks when Gemini key, quota, model, or network is unstable.

## Phase 3 Plan Acceptance Test

Before implementing Gemini for `/plan`, use this script as the acceptance target:

```powershell
F:\code\InSchool\Semester8\EXE201\Ver2\ai-service\.venv\Scripts\python.exe F:\code\InSchool\Semester8\EXE201\Ver2\test_ai_v2_plan_api.py
```

The script calls Spring Boot, not Python directly, so it also verifies that Spring can load team members and job labels before forwarding the request to the AI service.

Expected before Phase 3 implementation:

```text
Some tests can fail because /plan is still mock.
```

Expected after Phase 3 implementation:

```text
All Gemini plan acceptance tests pass.
```

## Phase 4 Revise Acceptance Test

Use this script to verify `/revise` through Spring Boot:

```powershell
F:\code\InSchool\Semester8\EXE201\Ver2\ai-service\.venv\Scripts\python.exe F:\code\InSchool\Semester8\EXE201\Ver2\test_ai_v2_revise_api.py
```

The script does not call `/extract` or `/plan`; it sends a fixed draft to `/revise` so it can test revision behavior with fewer Gemini calls.
