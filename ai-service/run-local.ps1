$ErrorActionPreference = "Stop"

$ServiceRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$Python = Join-Path $ServiceRoot ".venv\Scripts\python.exe"

if (-not (Test-Path $Python)) {
    throw "Python venv not found. Run: python -m venv .venv"
}

Set-Location $ServiceRoot
& $Python -m uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
