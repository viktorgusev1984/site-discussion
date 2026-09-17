# Open Ideas — discussions platform

A full-stack product-feedback community inspired by GitHub Discussions. Users can publish ideas in categories, search and sort them, vote once, and hold nested conversations.

## Architecture

```text
Browser → React 18 / TypeScript / Vite → REST / JWT → Spring Boot 3 / JPA
                                                        ↓
                                              PostgreSQL 17 / Flyway
```

- **`frontend/`** — responsive single-page app with React Router, Axios, Markdown preview, reusable discussion, voting, category, form, and threaded-comment components.
- **`backend/`** — stateless Spring Web API. Spring Security verifies signed JWTs; Bean Validation protects request boundaries; author/admin checks protect mutations.
- **PostgreSQL** — relational source of truth. The vote table has a database-level unique `(user_id, discussion_id)` constraint, so concurrent duplicate votes cannot persist.
- **Flyway** owns schema evolution. Hibernate runs in validation-only mode.

## Requirements

For the container workflow: Docker Engine with Compose v2. For development without Docker: Node.js 22+, npm 10+, JDK 21, Maven 3.9+, and PostgreSQL 15+.

## Quick start with Docker

```bash
cp .env.example .env
# Replace POSTGRES_PASSWORD and JWT_SECRET with strong random values.
docker compose up --build
```

Open the UI at <http://localhost:3000>, the API at <http://localhost:8080/api>, and Swagger UI at <http://localhost:8080/swagger-ui.html>. The frontend Nginx server proxies `/api` to the backend.

## Local development

1. Start PostgreSQL and create the database/user represented by the environment variables below.
2. Start the API: `cd backend && mvn spring-boot:run`.
3. In another terminal: `cd frontend && npm install && npm run dev`.
4. Open <http://localhost:5173>. Vite proxies API calls to port 8080.

## Environment variables

| Variable | Purpose | Local default |
| --- | --- | --- |
| `DATABASE_URL` | JDBC PostgreSQL URL (backend outside Compose) | `jdbc:postgresql://localhost:5432/discussions` |
| `DATABASE_USER` / `POSTGRES_USER` | Database user | `discussions` |
| `DATABASE_PASSWORD` / `POSTGRES_PASSWORD` | Database password | required in Compose |
| `POSTGRES_DB` | Database name | `discussions` |
| `JWT_SECRET` | HMAC signing secret, at least 32 random characters | required in Compose |
| `JWT_TTL` | ISO-8601 token lifetime | `PT24H` |
| `VITE_API_URL` | Frontend API base URL | `/api` |
| `BACKEND_PORT`, `FRONTEND_PORT` | Published Compose ports | `8080`, `3000` |

Do not commit `.env`; only `.env.example` is versioned.

## API overview

- `POST /api/auth/register`, `POST /api/auth/login`
- `GET /api/discussions?q=&category=&status=&sort=&page=&size=`
- `GET|POST /api/discussions`, `GET|PUT|DELETE /api/discussions/{id}`
- `POST /api/discussions/{id}/comments` (optional `parentId` creates a reply)
- `PUT|DELETE /api/discussions/{id}/vote`
- `GET /api/categories`, `GET /api/users/{username}`

Accepted discussion sorts are `activity,desc`, `createdAt,desc`, and `voteCount,desc`. Search is case-insensitive across title and body. Mutation endpoints require `Authorization: Bearer <token>`. Only authors and administrators can update or delete content.

## Migrations

Flyway applies scripts from `backend/src/main/resources/db/migration` automatically at backend startup. To run them without starting the server:

```bash
cd backend
DATABASE_URL=jdbc:postgresql://localhost:5432/discussions \
DATABASE_USER=discussions DATABASE_PASSWORD=... mvn flyway:migrate
```

Never edit an applied migration; add a new versioned `V2__description.sql` file instead.

## Testing and builds

```bash
cd backend && mvn test
cd frontend && npm test
cd frontend && npm run build
# Validate resolved Compose configuration after creating .env:
docker compose config
```

Backend coverage includes authentication, author/admin authorization, the concurrent-vote uniqueness invariant, and nested comments. Frontend component tests cover discussion search filtering, form validation/submission, and optimistic vote toggling.
