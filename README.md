# URL Shortener

A self-hosted URL shortening service built with **Spring Boot 4.1 and Java 21**, backed by MySQL and secured with stateless JWT authentication.

> **Status: work in progress.** This is a personal learning project. Expect TODOs and incomplete features. See the [roadmap](#roadmap) for what is built and what is planned.

## Purpose

Long URLs are hard to share, track and manage. This project turns them into short links. It has two goals:

1. **Backend engineering practice.** It covers layered Spring Boot design, JWT auth with refresh tokens and revocation, Flyway migrations, rate limiting and SSRF-aware input validation.
2. **Structured product thinking.** Scope, phased delivery, trade-offs and risks are written down before the code is, in a [PRD](docs/PRD.md), a [roadmap](docs/ROADMAP.md), [architecture decision records](#strategies-and-key-decisions) and a [risk register](docs/RISKS.md).

### Non-goals

- Payment processing or billing. A "paid plan" is a usage-tier flag only.
- Multi-region or high-availability deployment.
- Kafka or other streaming infrastructure for analytics.

## Features (implemented today)

- Create short links for `http`/`https` URLs. Private (site-local) IPs are rejected as basic SSRF protection.
- Public redirect: `GET /{shortCode}` returns a redirect without needing a token.
- User registration, login, logout and password change.
- Short-lived JWT access tokens (15 minutes by default) with rotating refresh tokens.
- Token revocation: logout adds the token's `jti` to a denylist.
- A per-user session cap (default 2). Reusing a spent refresh token revokes the whole session.
- Account deactivation (soft delete) and recovery-code-based password reset. There is no email flow yet.
- Configurable password policy and per-endpoint rate limits.
- Versioned REST API (`/api/1/...`) with OpenAPI docs and Swagger UI.
- Unit and integration test suite with JaCoCo coverage, and a GitHub Actions workflow that reports test results.

## API overview

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/1/user` | Public | Register. Returns a one-time recovery code. |
| `POST` | `/api/1/login` | Public | Returns an access token and a refresh token. |
| `POST` | `/api/1/token/refresh` | Public | Rotates the refresh token and issues a new pair. |
| `POST` | `/api/1/password-reset` | Public | Resets the password with a recovery code. |
| `POST` | `/api/1/logout` | Bearer | Revokes the current session. |
| `PUT` | `/api/1/user/password` | Bearer | Changes the password. |
| `POST` | `/api/1/user/recovery-code` | Bearer | Regenerates the recovery code. |
| `DELETE` | `/api/1/user` | Bearer | Deactivates the account. |
| `POST` | `/api/1/short-url` | Bearer | Creates a short link. |
| `GET` | `/{shortCode}` | Public | Redirects to the original URL. |

Interactive docs are at <http://localhost:8080/swagger-ui/index.html> once the app is running.

## Roadmap

The phases are ordered by dependency and risk, not by feature appeal. Full detail is in [ROADMAP.md](docs/ROADMAP.md).

| Phase | Theme | Status |
|---|---|---|
| 0 | **Foundation.** Core shortening, public redirect, JWT login, Flyway, rate limiting, tests and CI. | ✅ Complete |
| 1 | **Account/auth API completion.** Password change, account deactivation, recovery-code reset, refresh tokens and logout. Remaining hardening is tracked in Jira. | 🚧 In progress (core done, hardening open) |
| 2 | **Click analytics / audit log.** Record every redirect in a DB-backed queue table and drain it into aggregates with a scheduled worker. | ⏳ Planned |
| 3 | **Temporary vs. permanent links.** Link types with expiry, mapped to a free/paid tier flag (no payment gateway). | ⏳ Planned |
| 4 | **Link customisation.** Custom short codes (capped per user) and a separate QR code API. | ⏳ Planned |
| 5 | **Batch creation (stretch goal).** CSV import of a few thousand links per async job, respecting tier limits and protecting redirect latency. | ⏳ Planned, optional |

Phase 1 comes first because every later feature builds on authenticated users. Phases 2 → 3 → 4 → 5 are then sequenced by dependency and user value: analytics data informs the tier limits, the tier limits must exist before custom codes and batch creation can enforce their caps, and batch creation is last because it is the most niche feature.

### Target success metrics

| Metric | Target |
|---|---|
| Redirect latency (p95) | < 100 ms |
| Short-code collision rate | ~0% (collision-checked on generation) |
| Click events captured vs. served | 100% under normal load |
| Batch creation (stretch) | Batches in the thousands per job without timing out |

### Open questions

Decisions still to be made, each tied to the phase it blocks.

| Phase | Question | Options and trade-offs |
|---|---|---|
| 2 | How long do we keep raw click events after they are aggregated? | Delete after N days (cheapest), archive to a cold table (keeps history, grows storage), or keep forever (simple, but the queue table grows without bound). Needed before Phase 2 ships. |
| 2 | What should the redirect do if recording a click fails? | Fail open (redirect anyway, accept some lost clicks) or fail closed (guarantee capture, but analytics can break redirects). The 100% capture target conflicts with the p95 latency target. |
| 3 | How is a user marked "paid" without a payment gateway? | Admin-set flag (simple, needs an admin path), invite or coupon code (self-service, can be shared), or a config allowlist (no schema change, not scalable). |
| 3 | What happens to a temporary link at expiry? | Return 404 or 410, soft-delete and keep for analytics, or hard-delete. This also decides whether the short code can ever be reused. |
| 4 | Which short codes are reserved? | A denylist of route-like codes (`api`, `login`, `swagger-ui`) and a minimum length, so custom codes can't collide with application routes. |
| 5 | Is batch creation synchronous or an async job? | Synchronous is simple but times out on large batches. An async job with a status endpoint scales better but adds job state and polling. A design note is needed if the phase starts. |

## Backlog and future work

Work is tracked in Jira (project `URLS`). Besides the three feature phases above, the backlog has these themes. Ticket IDs are in brackets.

**Product features (ideas, need grooming)**
- Tiered "paid plan" model with no payment integration [URLS-47] and permanent vs. temporary links [URLS-48]. These are the Phase 3 work in ADR 0002.
- Custom short codes, capped per user (for example 5) so the feature can't be used to probe for existing codes [URLS-49].
- QR code API for a short link. It is a separate endpoint so link creation stays fast, and the generator sits behind an interface so a library can be swapped for an external service [URLS-51].
- Location data on analytics events, resolved from the client IP through the free GeoJS API [URLS-50].
- Email flow: async welcome mail, verification and emailed password reset. It would replace the recovery code [URLS-44].
- Per-user audit log (registration, login success/failure, logout, refresh-token reuse, password change, deactivation) [URLS-53].
- Caching for redirect lookups [URLS-24].

**Analytics (Phase 2 design is ready)**
- A `access_events` queue table with PENDING/PROCESSED/FAILED status, non-blocking capture in the redirect controller, a scheduled batch processor and a separate `click_analytics` summary table for fast reads [URLS-42].

**Scale and operations**
- Base62 short-code generation with collision handling and a retry on conflict. Today the code is random and unchecked, so a collision would surface as a 500 [URLS-8].
- Load-test profile and suite. Target: ~100 ms p95 at 1K concurrent users, with read-heavy, write-heavy and mixed scenarios [URLS-41].
- Configurable short-URL base. It is currently hard-coded to `http://localhost:8080`, so this is needed before any deployment [URLS-59].
- Better logging strategy and `logback` config [URLS-43].
- External secrets manager (Vault, AWS Secrets Manager or Azure Key Vault) [URLS-26].
- Low-priority research: run on AWS (ECS/Fargate and RDS MySQL), developed locally against the Floci emulator with no real cloud spend [URLS-58].
- CI check that the build produces the jar [URLS-57].
- Containerisation improvements (low priority): shrink the ~400–500 MB image, for example with a layered jar, a jlink-built minimal runtime, and a non-root user. The current image already works [URLS-61].

## Security posture and known gaps

Security was reviewed ticket by ticket. Closed items are good material for a demo or write-up.

**Fixed**
- JWT expiry calculation bug, where tokens effectively never expired [URLS-28].
- Missing or malformed `Authorization` header silently passed through [URLS-29], and JWT verification errors bypassing the app's error format [URLS-5].
- Hard-coded JWT secret and DB credentials moved to environment variables [URLS-33, URLS-34].
- Race condition in registration that allowed duplicate usernames [URLS-27].
- IP-based rate limiting on login and registration [URLS-35]. The redirect and URL-creation endpoints are limited too.
- No logout or token revocation [URLS-9], no refresh tokens [URLS-15], and no password change, reset or account deactivation [URLS-14]. All three are in review or merged.

**Open (tracked, not yet fixed)**
- `X-Forwarded-For` is trusted blindly, so rate limits can be bypassed. Bucket maps are unbounded [URLS-52].
- No per-username lockout, non-uniform login timing and errors, and no HTTPS/HSTS enforcement [URLS-54].
- Username enumeration on registration, because the response differs for a taken name [URLS-13, URLS-55].
- Short-code enumeration on the public redirect endpoint [URLS-19].
- Swagger UI and API docs are open in every profile [URLS-11].
- No CORS configuration [URLS-18], and no roles or authorities in the JWT [URLS-21].
- No validation on login and registration bodies [URLS-17].
- `SafeUrlValidator` relies on a DNS lookup that is open to DNS rebinding and checks only the first A record. A syntactic check is proposed [URLS-46].
- Account-management follow-ups from URLS-14: account-lock and expiry flags are not checked on reset, a distinct wrong-password error, and a login/password-change race [URLS-60].
- Smaller robustness items: `DataIntegrityViolationException` surfaces as a 500 [URLS-56], a fragile principal cast in URL creation [URLS-16], missing static resources return 500 instead of 404 [URLS-45], and dead code in the JWT filter [URLS-20].

## Strategies and key decisions

Significant choices are recorded as ADRs.

| ADR | Decision | Status |
|---|---|---|
| [0001 — Click analytics storage](docs/adr/0001-click-analytics-storage-strategy.md) | Use a **database table as a queue**, drained by a scheduled worker, instead of Kafka or a managed queue. It fits current scale and avoids new infrastructure. It will be revisited if the worker can't drain faster than the table fills, or if several consumers need the same stream. | Accepted |
| [0002 — Tiered link access](docs/adr/0002-tiered-link-access-model.md) | Model free/paid as a **tier flag** on the user or link, with no payment integration. This exercises the access-control and limit-enforcement logic without the billing scope. | Proposed |

Other engineering strategies already in the code:

- **Stateless auth.** HMAC-signed JWTs carry a random `jti`. A revocation denylist in MySQL is purged on a schedule once entries expire. Refresh tokens are opaque and stored only as SHA-256 hashes, with an absolute per-session expiry.
- **Schema as code.** Flyway owns the schema (`ddl-auto: none`). Every migration has a hand-written rollback script in `db/rollback`, because Flyway Community has no undo.
- **Uniform errors.** All failures flow through `GlobalExceptionHandler` as a consistent `ErrorResponse` built from an `ErrorCode`.
- **Secrets from the environment.** Nothing sensitive is hard-coded. The layout is ready for a secrets manager to supply the same variables later.

### Risk management

[RISKS.md](docs/RISKS.md) is a living register tied to roadmap phases. Highlights:

- The DB-queue becoming a write bottleneck, and unbounded growth of raw click rows. Mitigations are drain-rate monitoring and a retention policy before Phase 2 ships.
- Temporary links that never expire. The mitigation is expiry checks both in a scheduled cleanup and lazily at read time.
- Batch creation flooding the database or being abused for spam. Mitigations are throttling, async jobs and tier-based limits.
- The "paid" tier being trivially spoofable. This is accepted for a learning project and documented as an explicit trust boundary.

## Architecture

```
controller  →  service (interface + impl)  →  repository (Spring Data JPA)  →  entity
                         ↑
      model (request/response records) + mapper (MapStruct)
```

- Base package: `com.practice.url_shortner`
- Stack: Spring Boot, Spring Security, Spring Data JPA, MySQL, Flyway, MapStruct, springdoc-openapi, java-jwt
- The redirect endpoint (`GET /{shortCode}`) sits at the root, outside `/api/1`, and is excluded from JWT filtering.

More detail on the auth flow, token lifecycle and conventions is in [CLAUDE.md](CLAUDE.md).

## Running locally

### Prerequisites

- JDK 21
- MySQL 8.x running on `localhost:3306`
- Git. Maven is not needed because the repo ships the Maven wrapper.

### 1. Clone

```bash
git clone https://github.com/tanmay-k/url-shortener.git
cd url-shortener
```

### 2. Create the database

```sql
CREATE DATABASE url_shortner;
```

The default `local` profile connects to `localhost:3306/url_shortner`. Flyway creates all tables on first start, so you don't need to run any SQL scripts.

### 3. Configure environment variables

Copy `.env.example` and fill in the values.

| Variable | Required | Default | Notes |
|---|---|---|---|
| `JWT_SECRET` | **Yes** | none | The app fails fast at startup without it. Use a long random string. |
| `DB_USERNAME` | No | `root` | |
| `DB_PASSWORD` | No | `root` | |
| `JWT_ISSUER` | No | `tanmay@localhost` | |
| `JWT_AUDIENCE` | No | `url_shortner` | |
| `JWT_VALIDITY` | No | `900` | Access-token lifetime in seconds. |
| `REFRESH_TOKEN_VALIDITY` | No | `P7D` | ISO-8601 duration. Absolute per session. |
| `MAX_SESSIONS_PER_USER` | No | `2` | Must be at least 1. |

Export the variables into your shell before running. `.env` is gitignored, so never commit real secrets.

```bash
cp .env.example .env
```

In STS/Eclipse, set them on the **Environment** tab of the run configuration instead. Put only throwaway values in the tracked `UrlShortnerApplication.launch` file.

### 4. Run

```bash
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd spring-boot:run`. The app starts on <http://localhost:8080> with the `local` profile.

### 5. Try it

```bash
# Register (save the recovery code from the response)
curl -X POST http://localhost:8080/api/1/user \
  -H "Content-Type: application/json" \
  -d '{"userName":"alice","password":"a-strong-password"}'

# Log in
curl -X POST http://localhost:8080/api/1/login \
  -H "Content-Type: application/json" \
  -d '{"userName":"alice","password":"a-strong-password"}'

# Create a short link with the returned access token
curl -X POST http://localhost:8080/api/1/short-url \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"longUrl":"https://example.com/a/very/long/path"}'

# Follow the short link
curl -i http://localhost:8080/<shortCode>
```

The request field names above are illustrative. Check Swagger UI for the exact schemas.

### Tests

```bash
./mvnw test
```

Tests run against in-memory H2 with Flyway disabled, so no MySQL is needed.

### Docker

A multi-stage `Dockerfile` is included. The application runs correctly from the image I built. The image is currently large (roughly 400–500 MB) because it hasn't been optimised yet. Slimming it down is a low-priority future item, not a blocker. The commands below are the manual way to build and run it, and I don't test them in CI.

```bash
docker build -t url-shortener .
docker run -p 8080:8080 \
  -e JWT_SECRET=<your-secret> \
  -e DB_PASSWORD=<db-password> \
  url-shortener
```

The image runs with the `dev` profile, which expects MySQL at `host.docker.internal:3306/url_shortner`.

### Database migrations

- Migrations live in `src/main/resources/db/migration` as `V{n}__description.sql`. Flyway applies them on startup.
- Each migration needs a matching `V{n}__description_rollback.sql` in `src/main/resources/db/rollback`. Flyway never runs these. See `db/rollback/README.md` to apply one by hand.
- If you point the app at a database that already has the schema but no `flyway_schema_history`, `baseline-on-migrate` baselines it at V1 instead of failing.

## Documentation index

| Document | Contents |
|---|---|
| [PRD.md](docs/PRD.md) | Problem statement, goals, non-goals, personas, success metrics |
| [ROADMAP.md](docs/ROADMAP.md) | Phased plan, dependencies and open decisions |
| [RISKS.md](docs/RISKS.md) | Risk register mapped to roadmap phases |
| [ADR 0001](docs/adr/0001-click-analytics-storage-strategy.md) | Click analytics storage strategy |
| [ADR 0002](docs/adr/0002-tiered-link-access-model.md) | Tiered link access model |
| [CLAUDE.md](CLAUDE.md) | Detailed architecture and conventions |

## Copyright and usage

© 2026 Tanmay Kulkarni. All rights reserved.

This project is published for demonstration and portfolio purposes only. The source is public so it can be read and evaluated, but no license is granted to copy, modify, redistribute or use it commercially without my written permission.

### Diligence statement

The foundation phase of this project was written largely by hand. In later phases I used Claude (Anthropic) as an AI coding assistant for code, tests and documentation. I directed the design and scope, and I reviewed, edited and curated all AI-generated and co-created content. The result reflects my understanding, expertise and intended design. AI tools were instrumental in the process, but I take full responsibility for the code, its accuracy and its presentation. I make this disclosure in the interest of transparency and to acknowledge the evolving role of AI in software development.

## Contributing

This is a personal learning project, so I'm not looking for code contributions or pull requests. Issues, questions and suggestions are welcome: please open a GitHub issue.
