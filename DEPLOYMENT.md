# LMS backend — cloud deployment (Render + Neon)

This document describes how to run the LMS Spring Boot API in a **production** container (e.g. [Render](https://render.com) Free Web Service) with [Neon](https://neon.tech) PostgreSQL.

**Do not commit secrets.** Use the platform’s secret/environment UI only.

## Architecture summary

- **Runtime:** Java 17, Spring Boot 4, executable JAR in Docker
- **Database:** PostgreSQL (Neon); schema via **Flyway**; Hibernate `ddl-auto=validate`
- **Auth:** Stateless JWT (Bearer); CSRF disabled for API (documented in `SecurityConfig`)
- **No local file uploads** — course media is URL-based; ephemeral container disk is not used for user content

## Required environment variables

| Variable | Required | Description |
|----------|----------|-------------|
| `DB_URL` | Yes | JDBC URL to Neon Postgres, e.g. `jdbc:postgresql://ep-xxx.region.aws.neon.tech/neondb?sslmode=require` |
| `DB_USERNAME` | Yes | Neon database user |
| `DB_PASSWORD` | Yes | Neon database password |
| `JWT_SECRET` | Yes | HMAC secret, **at least 32 bytes** (app refuses to start otherwise) |
| `JWT_EXPIRATION_MS` | No | Token TTL in ms (default `3600000`) |
| `CORS_ALLOWED_ORIGINS` | Yes (prod) | Comma-separated browser origins, **no wildcards**, e.g. `https://your-ui.onrender.com` |
| `FRONTEND_URL` | No | Single UI origin; merged into CORS allowlist if not already listed |
| `PORT` | Set by Render | HTTP port the app must bind to (Spring: `server.port=${PORT}`) |
| `SPRING_PROFILES_ACTIVE` | Recommended | Set to `prod` in Docker image (already default in `Dockerfile`) |

Optional tuning:

| Variable | Description |
|----------|-------------|
| `JVM_HEAP_OPTS` | Set by the Docker image (container-aware heap); do not remove on Render |
| `JAVA_OPTS` | Optional extra JVM flags appended by the entrypoint (does not replace `JVM_HEAP_OPTS`) |

### Neon JDBC notes

- Use Neon’s **pooled** or direct connection string converted to JDBC form.
- Include **`sslmode=require`** (or `verify-full` if you configure trust) in `DB_URL`.
- Example shape only:

  ```text
  DB_URL=jdbc:postgresql://YOUR_HOST/neondb?sslmode=require
  DB_USERNAME=neondb_owner
  DB_PASSWORD=your_secret_here
  ```

## CORS

- Configured via `app.cors.allowed-origins` ← `CORS_ALLOWED_ORIGINS`.
- `FRONTEND_URL` is an optional convenience alias for one production UI origin.
- Credentials are allowed; **wildcard origins are not used** (see `SecurityConfig`).
- Set `CORS_ALLOWED_ORIGINS` to the exact scheme + host + port of your deployed `platform-ui` (and any extra dev origins if needed).

## Health checks

| Endpoint | Purpose | Auth |
|----------|---------|------|
| `GET /health` | **Liveness** — process up | Public |
| `GET /health/ready` | **Readiness** — Postgres reachable | Public |

On Render, use:

- **Health check path:** `/health/ready` (recommended) or `/health` for liveness only
- Responses are minimal (`{"status":"UP"}` / `503` with `DOWN`) — no secrets or config dump.

Actuator is **not** on the classpath; these endpoints are intentional lightweight replacements.

## Flyway

- Enabled by default (`spring.flyway.enabled=true`).
- Migrations run on application startup against `DB_URL`.
- Hibernate validates the schema after migrations (`spring.jpa.hibernate.ddl-auto=validate`).
- First deploy to an empty Neon database: Flyway applies `V1`…`V6` automatically.

## Build and run Docker locally

Prerequisites: Docker, env vars set (or passed with `-e`).

```bash
cd /path/to/lms

docker build -t lms-api:local .

docker run --rm -p 8080:8080 \
  -e DB_URL='jdbc:postgresql://host:5432/db?sslmode=require' \
  -e DB_USERNAME='user' \
  -e DB_PASSWORD='password' \
  -e JWT_SECRET='your_local_dev_secret_at_least_32_chars_long' \
  -e CORS_ALLOWED_ORIGINS='http://localhost:5173' \
  lms-api:local
```

Verify:

```bash
curl -s http://localhost:8080/health
curl -s http://localhost:8080/health/ready
```

Local Compose Postgres (development only) remains in `docker-compose.yml`; the **app container does not include Postgres**.

## Render Web Service setup

1. Create a **Neon** project and database; copy connection details into `DB_*` variables.
2. On Render, create a **Web Service**:
   - **Environment:** Docker
   - **Repository:** this backend repo (already has Git remote)
   - **Dockerfile path:** `Dockerfile`
   - **Instance:** Free (512 MB RAM)
3. Set environment variables (table above). Set `SPRING_PROFILES_ACTIVE=prod` if not using the provided image default.
4. **Health check path:** `/health/ready`
5. Deploy. On success, Flyway migrates Neon and the API listens on Render’s `PORT`.

Point `platform-ui` `VITE_API_BASE_URL` at the Render service URL (HTTPS).

## Free tier limitations

| Area | Limitation |
|------|------------|
| Render Free | Spins down on idle; cold starts; 512 MB RAM; no persistent disk |
| Neon Free | Storage/connection limits; use `sslmode=require` |
| JVM | `JVM_HEAP_OPTS` (~60% max RAM, metaspace cap); avoid large imports |

## Persistence and background jobs

| Feature | Risk on Render Free |
|---------|---------------------|
| **File uploads** | None implemented — content is URL-based (video/article URLs). |
| **Article extraction** | Runs **after commit** in-process (`TransactionSynchronization.afterCommit`). If the instance **restarts or scales down** while status is `PENDING`, extraction may **not complete** until an admin triggers **Refresh article** or the lesson URL is saved again. No job queue in this codebase. |
| **JWT / sessions** | Stateless; no server-side session store to lose. |

**Follow-up (out of scope for this task):** durable queue for article extraction, object storage for any future uploads, paid tier for always-on instances.

## Manual steps remaining (you)

1. Create Neon database and note `DB_URL` / user / password.
2. Generate a strong `JWT_SECRET` (≥ 32 bytes).
3. Deploy frontend; set `CORS_ALLOWED_ORIGINS` (and optionally `FRONTEND_URL`) to that HTTPS origin.
4. Create Render Web Service from this repo’s Dockerfile; paste env vars.
5. Configure Render health check to `/health/ready`.
6. Smoke-test: health endpoints, login, Flyway version in logs, API from UI.
7. Seed or create admin user via existing registration/admin flows as needed.

Do **not** commit `.env` or paste real credentials into Git or this file.
