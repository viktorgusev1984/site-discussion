# Open Ideas — discussions platform

A full-stack product-feedback community inspired by GitHub Discussions. Users can publish ideas in categories, search and sort them, vote once, and hold nested conversations.

## Architecture

```text
Browser → React 18 / TypeScript / Vite → REST / JWT → Spring Boot 3 / JPA
                                                        ↓
                                              PostgreSQL 17 / Flyway

Administrators → Qwen Code Web UI → HTTP + SSE → isolated `qwen serve` agent
```

- **`frontend/`** — responsive single-page app with React Router, Axios, Markdown preview, reusable discussion, voting, category, form, and threaded-comment components.
- **`backend/`** — stateless Spring Web API. Spring Security verifies signed JWTs; Bean Validation protects request boundaries; author, moderator, and administrator checks protect mutations.
- **PostgreSQL** — relational source of truth. The vote table has a database-level unique `(user_id, discussion_id)` constraint, so concurrent duplicate votes cannot persist.
- **`qwen-agent/`** — a separate Qwen Code daemon with its built-in Web UI and agent loop. It has its own persistent workspace and is not embedded into the Spring process.
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

The Qwen Code Web UI is available at <http://localhost:4170>. Administrators can
also open it from the account menu. Enter `QWEN_SERVER_TOKEN` when the Web UI
asks for the daemon token. The token grants access to an agent capable of
executing tools and must not be shared with ordinary application users.

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
| `NOTIFICATION_ENCRYPTION_KEY` | Key used to encrypt webhook URLs and signing secrets (prefer a base64-encoded random 32-byte value) | required in Compose |
| `NOTIFICATION_WORKER_INTERVAL` | Delay between delivery worker passes | `10s` |
| `NOTIFICATION_MAX_ATTEMPTS` | Maximum delivery attempts | `5` |
| `NOTIFICATION_INITIAL_BACKOFF` | Initial exponential retry delay | `30s` |
| `NOTIFICATION_CONNECT_TIMEOUT`, `NOTIFICATION_READ_TIMEOUT` | Outbound webhook timeouts | `3s`, `5s` |
| `NOTIFICATION_MAX_RESPONSE_BYTES` | Maximum webhook response body consumed | `65536` |
| `NOTIFICATION_MAX_REDIRECTS` | Maximum validated webhook redirects | `2` |
| `NOTIFICATION_TEST_INTERVAL` | Minimum interval between channel tests per channel | `1m` |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` | SMTP connection used by e-mail channels | `localhost`, `1025`, empty, empty |
| `SMTP_AUTH`, `SMTP_STARTTLS` | Enable SMTP authentication and STARTTLS | `false`, `false` |
| `QWEN_API_KEY` | Provider credential used only by the isolated Qwen Code container | required in Compose |
| `QWEN_BASE_URL`, `QWEN_MODEL` | OpenAI-compatible Qwen endpoint and model | DashScope / `qwen3-coder-plus` |
| `QWEN_SERVER_TOKEN` | Bearer token protecting the Qwen Code daemon and Web UI | required in Compose |
| `QWEN_AGENT_PORT` | Published Qwen Code Web UI port | `4170` |
| `VITE_QWEN_AGENT_URL` | URL opened by the administrator menu item; omit it to hide the item | `http://localhost:4170` |

Do not commit `.env`; only `.env.example` is versioned.

## Qwen Code agent

Qwen Code runs as a sidecar instead of a library inside Spring Boot. This keeps
the application's request lifecycle separate from the agent loop and reuses the
official Web UI supplied by `qwen serve`. HTTP/SSE sessions, tool approvals and
conversation rendering are consequently owned by Qwen Code rather than
reimplemented in this repository.

The container receives two named volumes: `qwen_agent_home` for daemon state and
`qwen_agent_workspace` for files created by the agent. It deliberately does not
mount the application source, Docker socket, database volume, or backend secrets.
To let an operator work on a repository, copy or clone it into the dedicated
agent workspace and review tool approval prompts in the Qwen UI. Do not mount a
production host filesystem into the agent container.

`qwen serve` is still described upstream as experimental and local/small-team
oriented. For an internet deployment, put it behind TLS and an additional access
control layer, keep `QWEN_SERVER_TOKEN` enabled, and restrict network access.
The admin-only frontend link is a discoverability control, not an authorization
boundary; the daemon token remains the actual credential.

The application backend also uses this daemon as an agent-loop adapter for
authenticated assistant functions. Authors can improve a draft before saving;
discussion participants can request an on-demand summary or place an AI reply
draft into the normal editable comment field. Nothing produced by the agent is
published automatically. Spring creates an isolated thread session, submits a
constrained prompt, and polls the daemon's turn result while Qwen Code owns the
model and tool loop. The corresponding API routes are:

- `POST /api/ai/drafts/improve`
- `POST /api/ai/discussions/{id}/summary`
- `POST /api/ai/discussions/{id}/reply-draft`

Authenticated users also have a persistent multi-turn conversation at
`/assistant`. The page embeds the official `@qwen-code/web-shell` React template,
which in turn uses `@qwen-code/sdk` for daemon sessions, resumable SSE and the
chat transcript. Each application user is assigned a Qwen thread session in
`ai_chat_sessions`. Browser requests go through `/api/agent`: the bridge checks
that the requested session belongs to the JWT owner and replaces the application
JWT with `QWEN_SERVER_TOKEN` only on the server. The daemon credential is never
included in the frontend bundle or returned to the browser.

## Render deployment

[Render](https://render.com/docs) is a cloud application platform (PaaS): it builds
the project from a Git repository, runs web services, publishes static sites, and
can provision managed PostgreSQL databases. Unlike a VPS, it does not require
manually installing Java, Nginx, PostgreSQL, or configuring HTTPS on a server.

The repository includes a [Render Blueprint](https://render.com/docs/infrastructure-as-code)
(`render.yaml`) that provisions four resources: the React static site, the Spring
Boot Docker service, the isolated Qwen Code service, and PostgreSQL. The API and
Qwen service receive the same generated daemon token through a shared Render
environment-variable group, and the API contacts Qwen over Render's private
network. All services use free plans. To fit Qwen into the 512 MiB POC instance,
the image caps the HTTP daemon heap at 96 MiB and patches the pinned Qwen 0.24.1
ACP launcher to honor `QWEN_ACP_HEAP_MB`, set to 160 MiB by default. The built-in
Web UI is disabled and the live journal is capped at 1 MiB to remove additional
memory pressure. The application serves its own copy of the Web Shell, so
`--no-web` does not remove the `/assistant` interface.

Qwen still prints a warning that its derived *daemon memory budget* is below
1 GiB. In 0.24.1 that budget controls adaptive live-journal growth and does not
size the ACP child; the explicit journal cap disables that growth. A `SIGKILL`
immediately after preheat is the container OOM-killing the ACP child, which is
why the child needs the separate heap cap. This is a POC compromise: large
conversations or tool results can still exhaust 512 MiB. Increase the Render
plan and `QWEN_ACP_HEAP_MB` before production use. The available Blueprint
fields are documented in the official
[Blueprint specification](https://render.com/docs/blueprint-spec).

1. Push the repository to GitHub or GitLab.
2. In the Render dashboard choose **New → Blueprint**, connect the repository, and
   apply `render.yaml`.
3. If Render changes either service name because it is already taken, update both
   `VITE_API_URL` on the static site and `APP_CORS_ALLOWED_ORIGINS` on the API to
   the actual public URLs. Also update `QWEN_AGENT_INTERNAL_URL` on the API when
   the `open-ideas-qwen` service name changes, then redeploy the services.
4. Set `OPENAI_API_KEY` on the `open-ideas-qwen` service to the provider key used
   by Qwen Code. The Blueprint configures the DashScope-compatible endpoint and
   `qwen3-coder-plus` by default; override `OPENAI_BASE_URL` or `OPENAI_MODEL` on
   that service when using a different compatible provider.
5. The Blueprint uses Gmail SMTP by default. Enable two-step verification for the
   Google account used by the application, create an
   [app password](https://support.google.com/accounts/answer/185833), and save the
   complete Gmail address in `SMTP_USERNAME` and the 16-character app password in
   `SMTP_PASSWORD` for the API service. The configured endpoint is `smtp.gmail.com`
   with authentication and STARTTLS on port 587.

   Gmail is only the ready-to-use default. To use another provider, override
   `SMTP_HOST` (and `SMTP_PORT` when necessary) in the Render dashboard:

   | Provider | `SMTP_HOST` | Port | Credentials |
   | --- | --- | --- | --- |
   | [Yandex Mail](https://yandex.com/support/yandex-360/customers/mail/ru/mail-clients/others.html) | `smtp.yandex.ru` | `587` | Full mailbox address and an app password |
   | [SMTP2GO](https://support.smtp2go.com/hc/en-gb/articles/223087627) | `mail.smtp2go.com` | `587` | SMTP username and password from SMTP2GO |
   | [Mailjet](https://documentation.mailjet.com/hc/en-us/articles/360043229473) | `in-v3.mailjet.com` | `587` | Mailjet API key and secret key |

   A public relay without credentials is intentionally not used: it would allow
   third parties to send spam. SMTP credentials belong to the server and are never
   requested from users adding an email channel. The visible sender name remains
   `open-ideas`, and the authenticated SMTP login is used as its address.
6. Open the static-site URL and sign in with the ready-made test account
   (`demo` / `demo12345`). Moderation can be tested with the administrator account
   (`admin` / `admin12345`); both accounts are shown on the login page. The API health check is
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
- `PUT /api/discussions/{id}/close|cancel`, `POST /api/discussions/{id}/actions/jira`
- `PUT /api/users/{username}/role?role=MODERATOR` (administrator only)
- `GET /api/categories`, `GET /api/users/{username}`

### Personal notification settings

All endpoints below require `Authorization: Bearer <token>` and always operate on the token owner;
channel and rule IDs owned by another user are returned as not found. Public profile responses from
`GET /api/users/{username}` contain no notification settings.

- `GET|POST /api/me/notifications/channels` — list or create channels.
- `PUT /api/me/notifications/channels/{id}` — replace editable channel settings. An empty `url` or
  `secret` retains the stored credential.
- `PATCH /api/me/notifications/channels/{id}/disable` — stop delivery through a channel.
- `DELETE /api/me/notifications/channels/{id}` — delete a channel. Its rules and queued delivery
  records are deleted by database foreign-key cascades.
- `POST /api/me/notifications/channels/{id}/test` — send a test through the production sender.
  Tests use the normal SSRF checks and timeouts and are limited by `NOTIFICATION_TEST_INTERVAL`.
- `GET|POST /api/me/notifications/rules`, `PUT|DELETE /api/me/notifications/rules/{id}` — manage
  subscription rules. `DISCUSSION` scope requires `discussionId`; `ALL_DISCUSSIONS` forbids it.

Channel types are `EMAIL`, `MATTERMOST`, and `WEBHOOK`; rule triggers are `NEW_DISCUSSION`,
`NEW_COMMENT`, `NEW_REPLY`, `FIRST_VOTE`, `NEW_REACTION`, `STATUS_CHANGED`,
`JIRA_ACTION_CREATED`, and `MENTION`. Mattermost and webhook channels require an HTTPS `url`, and a
generic webhook also requires `secret` for HMAC-SHA256 signing. URLs and secrets are encrypted at
rest. Read and write responses expose only a connection description such as
`hooks.example.com/…a1b2`; the original URL and secret are never returned.

Accepted discussion sorts are `activity,desc`, `createdAt,desc`, and `voteCount,desc`. Search is case-insensitive across title and body. Mutation endpoints require `Authorization: Bearer <token>`. Authors can cancel their discussions; moderators can close or cancel them and attach Jira actions. Administrators have every moderator capability, can edit or delete any discussion, and exclusively grant or revoke the moderator role.

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
