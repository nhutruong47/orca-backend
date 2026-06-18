# ORCA Platform

ORCA is a full-stack operations management platform for team-based production and collaboration workflows. The system provides workspaces for teams, task planning, inventory, orders, notifications, chat, AI-assisted planning, authentication, and payment upgrade flows.

This repository is organized as a monorepo with a Spring Boot backend and a React/Vite frontend.

## Table of Contents

- [Business Scope](#business-scope)
- [Technology Stack](#technology-stack)
- [Repository Structure](#repository-structure)
- [System Architecture](#system-architecture)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Development Commands](#development-commands)
- [Testing and Quality Gates](#testing-and-quality-gates)
- [Deployment](#deployment)
- [Security Guidelines](#security-guidelines)
- [Development Standards](#development-standards)
- [Maintenance Notes](#maintenance-notes)

## Business Scope

ORCA supports operational teams that need a shared workspace for planning, execution, communication, and order coordination.

Core capabilities:

- User registration, login, JWT authentication, and Google OAuth2 callback support.
- Team and group management with invitation flow.
- Task creation, assignment, checklist tracking, and progress monitoring.
- Goal planning and AI-assisted parsing/planning workflows.
- Inventory management and inter-group order coordination.
- Marketplace and order management screens.
- Chat, notification, and presence services.
- VNPAY sandbox/mock payment flow for plan upgrades.
- Admin workspace for operational management.

## Technology Stack

Backend:

- Java 21
- Spring Boot 3.4.x
- Spring Web
- Spring Security
- JWT with JJWT
- OAuth2 Client and Resource Server
- Spring Data JPA / Hibernate
- WebSocket / STOMP
- H2 for local development
- PostgreSQL for production
- Maven Wrapper

Frontend:

- React 19
- TypeScript
- Vite
- React Router
- Axios
- STOMP/SockJS
- Recharts
- Lucide React
- ESLint

Infrastructure and deployment:

- Docker for backend packaging
- Nginx-based frontend container support
- Vercel-compatible frontend routing config
- Environment-driven production configuration

## Repository Structure

```text
orca/
|-- backend/
|   |-- src/
|   |   |-- main/
|   |   |   |-- java/org/example/backend/
|   |   |   |   |-- config/       # Application, WebSocket, bootstrap config
|   |   |   |   |-- controller/   # REST API controllers
|   |   |   |   |-- dto/          # API request/response DTOs
|   |   |   |   |-- entity/       # JPA entities
|   |   |   |   |-- exception/    # Global exception handling
|   |   |   |   |-- repository/   # Spring Data repositories
|   |   |   |   |-- security/     # JWT, OAuth2, filters, security rules
|   |   |   |   `-- service/      # Business services
|   |   |   `-- resources/       # Spring application profiles
|   |   `-- test/                # Backend tests
|   |-- Dockerfile
|   |-- mvnw
|   |-- mvnw.cmd
|   `-- pom.xml
|-- frontend/
|   |-- public/                  # Static public assets
|   |-- src/
|   |   |-- assets/              # Frontend assets
|   |   |-- components/          # Shared UI components
|   |   |-- context/             # React providers
|   |   |-- pages/               # Route-level pages
|   |   |-- services/            # API and integration clients
|   |   |-- types/               # Shared TypeScript types
|   |   `-- utils/               # Utility functions
|   |-- Dockerfile
|   |-- nginx.conf
|   |-- package.json
|   |-- package-lock.json
|   `-- vite.config.ts
|-- data/                        # Local H2 database files
|-- .legacy/                     # Archived pre-restructure folders
|-- .gitignore
`-- README.md
```

## System Architecture

```text
Browser
  |
  | HTTP / WebSocket
  v
React Frontend
  |
  | REST API / JWT / STOMP
  v
Spring Boot Backend
  |
  | JPA
  v
Database

External integrations:
- Google OAuth2
- SMTP mail provider
- AI service
- VNPAY sandbox/payment gateway
```

Backend layers:

- `controller`: exposes HTTP endpoints and delegates use cases.
- `service`: contains business logic and orchestration.
- `repository`: handles persistence through Spring Data JPA.
- `entity`: defines persistence models.
- `dto`: defines API boundary objects.
- `security`: centralizes authentication, authorization, JWT, and OAuth2 behavior.

Frontend layers:

- `pages`: route-level experiences.
- `components`: reusable UI and layout primitives.
- `services`: API clients and integration helpers.
- `context`: app-level state providers such as authentication and theme.
- `types`: shared domain and API types.

## Getting Started

### Prerequisites

- Java 21
- Node.js 20 or newer
- npm
- Docker, optional
- PostgreSQL, required for production-like deployment

### Start Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Default backend URL:

```text
http://localhost:8080
```

Run backend with local H2 file database:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

H2 Console:

```text
http://localhost:8080/h2-console
```

### Start Frontend

```powershell
cd frontend
npm install
npm run dev
```

Default frontend URL:

```text
http://localhost:5173
```

## Environment Variables

Production secrets must be provided through environment variables or a secret manager. Do not commit real secrets to the repository.

Backend:

```env
DB_URL=jdbc:postgresql://host:5432/database
DB_USERNAME=database_user
DB_PASSWORD=database_password

JWT_SECRET=replace_with_a_long_random_secret

GOOGLE_CLIENT_ID=google_oauth_client_id
GOOGLE_CLIENT_SECRET=google_oauth_client_secret

MAIL_USERNAME=smtp_username
MAIL_PASSWORD=smtp_password_or_app_password

FRONTEND_URL=https://your-frontend-domain.com

AI_SERVICE_API_KEY=ai_service_api_key

VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_TMN_CODE=vnpay_tmn_code
VNPAY_HASH_SECRET=vnpay_hash_secret
VNPAY_RETURN_URL=https://your-backend-domain.com/api/payments/vnpay/return
```

Frontend:

```env
VITE_API_BASE_URL=http://localhost:8080
```

## Development Commands

Backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
.\mvnw.cmd clean package
```

Frontend:

```powershell
cd frontend
npm install
npm run dev
npm run lint
npm run build
npm run preview
```

## Testing and Quality Gates

Minimum checks before merging or deploying:

```powershell
cd backend
.\mvnw.cmd test
```

```powershell
cd frontend
npm run lint
npm run build
```

Recommended engineering gates:

- Backend unit/integration tests must pass.
- Frontend lint and production build must pass.
- No generated artifacts such as `target/`, `dist/`, `node_modules/`, `.class`, `.log`, or `.err` should be committed.
- No credentials or private keys should be committed.
- API changes should include DTO, service, and frontend service updates when applicable.

## Deployment

### Backend Docker Build

From the repository root:

```bash
docker build -t orca-backend backend
```

Run with an environment file:

```bash
docker run -p 8080:8080 --env-file .env orca-backend
```

The backend Dockerfile starts the application with the `prod` Spring profile.

### Frontend Build

```powershell
cd frontend
npm run build
```

The production output is generated in:

```text
frontend/dist/
```

### Production Checklist

- Configure PostgreSQL connection variables.
- Configure JWT, OAuth2, SMTP, AI service, and VNPAY secrets.
- Set `FRONTEND_URL` to the deployed frontend domain.
- Set `VITE_API_BASE_URL` to the deployed backend API URL.
- Verify OAuth callback URL in Google Cloud Console.
- Verify VNPAY return URL.
- Run backend tests and frontend build before release.
- Review startup logs after deployment.

## Security Guidelines

- Never commit real passwords, SMTP credentials, OAuth secrets, JWT secrets, payment secrets, or AI API keys.
- Rotate any secret that was committed or shared.
- Use a strong production `JWT_SECRET`.
- Keep production database credentials outside source code.
- Restrict OAuth redirect URLs to trusted domains only.
- Keep CORS and `FRONTEND_URL` explicit per environment.
- Avoid exposing raw SQL, stack traces, or internal exception details in API responses.

## Development Standards

Backend conventions:

- Keep controllers thin and delegate business logic to services.
- Use DTOs at API boundaries instead of exposing entities directly.
- Keep persistence logic inside repositories.
- Use service-level methods for transactional workflows.
- Prefer explicit validation and meaningful domain errors.
- Add tests for authentication, authorization, payment, order, and task workflows.

Frontend conventions:

- Keep route-level logic in `pages/`.
- Keep shared UI in `components/`.
- Keep API calls in `services/`.
- Keep reusable types in `types/`.
- Use environment variables for API URLs.
- Avoid hardcoding production URLs or credentials.

Branch and commit recommendations:

- Use feature branches for new functionality.
- Keep commits focused and reviewable.
- Include tests or build verification for behavior changes.
- Do not mix formatting-only changes with business logic changes unless intentional.

## Maintenance Notes

- `.legacy/` contains archived folders from the previous project layout. It is ignored by Git and should not be used for active development.
- `data/` contains local H2 database files. Production should use PostgreSQL.
- Runtime logs, build outputs, dependency folders, and generated files are ignored.
- If frontend dependencies were cleaned, run `npm install` again inside `frontend/`.
- If backend build output was cleaned, Maven will regenerate `backend/target/`.

## License

No license has been declared yet. Add a license before distributing or publishing this repository.
   
 