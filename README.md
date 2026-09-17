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

## Free test deployment on Render

[Render](https://render.com/docs) is a cloud application platform (PaaS): it builds
the project from a Git repository, runs web services, publishes static sites, and
can provision managed PostgreSQL databases. Unlike a VPS, it does not require
manually installing Java, Nginx, PostgreSQL, or configuring HTTPS on a server.

The repository includes a [Render Blueprint](https://render.com/docs/infrastructure-as-code)
(`render.yaml`) that provisions three resources: the React static site, the Spring
Boot Docker service, and PostgreSQL. This is the simplest way to put the complete
application online for a demo without changing its architecture. The available
Blueprint fields are documented in the official
[Blueprint specification](https://render.com/docs/blueprint-spec).

1. Push the repository to GitHub or GitLab.
2. In the Render dashboard choose **New → Blueprint**, connect the repository, and
   apply `render.yaml`.
3. If Render changes either service name because it is already taken, update both
   `VITE_API_URL` on the static site and `APP_CORS_ALLOWED_ORIGINS` on the API to
   the actual public URLs, then redeploy them.
4. Open the static-site URL and sign in with the ready-made test account
   (`demo` / `demo12345`), also shown on the login page. The API health check is
   available at `/api/categories`, and Swagger UI is at `/swagger-ui.html` on the
   API service.

Before deploying, review Render's current [free-instance limitations](https://render.com/docs/free).
Free instances can sleep after inactivity and the first request may therefore be
slow. Render's free PostgreSQL is intended only for short-lived testing and may
expire; export any data that matters. For a longer-lived free demo, create a free
PostgreSQL database at Neon, replace `DATABASE_URL` with its JDBC connection URL
(`jdbc:postgresql://...`), and set `DATABASE_USER` and `DATABASE_PASSWORD` from
the Neon credentials.

Useful official references:

- [Render documentation](https://render.com/docs)
- [Deploying a Spring Boot application](https://render.com/docs/deploy-spring-boot)
- [Static sites](https://render.com/docs/static-sites)
- [Render Postgres](https://render.com/docs/postgresql-creating-connecting)

The API also supports platform-assigned ports via `PORT`. For deployments where
the frontend and API use different origins, set `APP_CORS_ALLOWED_ORIGINS` to a
comma-separated list of exact frontend origins (without trailing slashes).

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
