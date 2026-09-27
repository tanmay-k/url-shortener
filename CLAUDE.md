# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

A Spring Boot (4.1.0, Java 21) URL shortener. REST API for creating short codes and a public redirect endpoint, backed by MySQL/JPA, with stateless JWT authentication. Base package: `com.practice.url_shortner`. This is a work-in-progress personal project — expect TODOs, dead/commented-out code, and incomplete features.

## Build, run, test

```bash
./mvnw clean package -DskipTests   # build (Windows: mvnw.cmd)
./mvnw spring-boot:run             # run locally (active profile: local)
./mvnw test                        # run all tests
./mvnw test -Dtest=UrlShortnerApplicationTests#contextLoads   # run a single test
```

Docker build runs with `--spring.profiles.active=dev` (see `Dockerfile`).

Requires a running MySQL instance matching the active profile's `spring.datasource` config (`local` → `localhost:3306/url_shortner`, `dev` → `host.docker.internal:3306/url_shortner`, both `root/root` by default). `spring.jpa.hibernate.ddl-auto` is `none`, so the `url_mapping` and user tables must exist already — there is no migration tool in this repo.

### Secrets

DB credentials and the JWT secret are read from environment variables (`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_ISSUER`, `JWT_AUDIENCE`, `JWT_VALIDITY`) via `${VAR:default}` placeholders in `application.yaml`/`application-local.yml`/`application-dev.yml` — see `.env.example` for the full list. `DB_*` and `JWT_ISSUER`/`JWT_AUDIENCE`/`JWT_VALIDITY` default to the local dev values if unset; `JWT_SECRET` has no default and must be set, or the app fails fast at startup with `PlaceholderResolutionException: Could not resolve placeholder 'JWT_SECRET'`. No secrets-manager dependency is wired in yet — this is a placeholder for a future Vault/AWS Secrets Manager/Azure Key Vault integration, which would only need to supply the same environment variables (or an earlier Spring `PropertySource`).

Ways to supply these locally:
- **Shell**: copy `.env.example` to `.env`, fill in real values, and export them into your shell before running (`.env` is gitignored, `.env.example` is not).
- **STS/Eclipse run configuration**: open the launch config (`Run > Run Configurations...`), select the `UrlShortnerApplication` entry, and set the six variables on the **Environment** tab (not **Arguments** — program arguments aren't read as env vars or Spring properties here). This project's `UrlShortnerApplication.launch` file already has this wired up.
  - ⚠️ `UrlShortnerApplication.launch` **is tracked in git**. Only put non-production, throwaway values in it (the current `root`/`secret`/etc. dummy values are fine) — never a real prod DB password or JWT signing secret, since that would recreate the exact hardcoded-secret problem this setup was meant to fix. For anything sensitive, prefer `.env` (gitignored) or an untracked/local-only launch config instead.

## Architecture

- **Layering**: `controller` → `service.*` (interface + `*Impl`/`*Service`) → `repository` (Spring Data JPA) → `entity`. Request/response DTOs are Java records in `model`. MapStruct (`mapper` package, interfaces prefixed `I...Mapper`) converts between entities and records.
- **API versioning**: Controllers under `/api/1` opt into path-based versioning via `@RequestMapping(version = "1")`, resolved by the `ApiVersionResolver` bean in `configuration/AppConfiguration.java` (only paths starting with `/api/` get a version segment).
- **Redirect endpoint is special**: `RedirectionController` serves `GET /{shortCode}` at the root (not under `/api/1`) and is deliberately excluded from JWT filtering — see below.
- **Auth flow**: `POST /api/1/login` authenticates via Spring Security's `AuthenticationManager` (delegating to `UserDetailsManagerImpl`/`IUserRepository`) and returns a JWT signed with HMAC256 (`utility/JwtUtils`, secret/issuer/audience/validity from `app.security.*` properties). `JwtAuthenticationFilter` (a `OncePerRequestFilter`) reads the `Authorization: Bearer <token>` header, verifies it, and sets a `UsernamePasswordAuthenticationToken` (username only, no authorities) on the `SecurityContext`.
  - `JwtAuthenticationFilter.shouldNotFilter` skips the filter entirely for any GET request whose servlet path doesn't start with `/api/` — this is what lets short-code redirects work without a token. Keep this in mind when adding new non-`/api` GET routes.
  - `SecurityConfig` permits `/api/*/login`, `/api/*/logout`, `POST /api/*/user`, GET on `/{shortCode:[a-zA-Z0-9]{6,10}}`, and Swagger UI/api-docs without auth; everything else requires authentication. Sessions are stateless (`SessionCreationPolicy.STATELESS`), CSRF is disabled.
- **URL mapping creation** (`UrlMappingService.createNewMapping`) reads the authenticated username out of `SecurityContextHolder` to resolve the owning `UserEntity`, generates an 8-char random alphanumeric short code (`RandomStringUtils.secureStrong()`), and persists a `UrlMappingEntity`. There is currently no collision check against existing short codes despite the `isShortCodeUnique` stub.
- **Validation**: `@SafeUrl` (`validator/SafeUrl` + `SafeUrlValidator`) restricts long URLs to `http`/`https` schemes and rejects site-local (private) IP addresses, intended as basic SSRF protection for user-submitted URLs.
- **Error handling**: `GlobalExceptionHandler` (`@RestControllerAdvice`) maps `CustomException` (carries an `ErrorCode`), `BadCredentialsException`, and generic `Exception` to a uniform `ErrorResponse` body. New failure cases should generally add an `ErrorCode` enum entry and throw `CustomException` rather than a new bespoke exception type.
- **Config profiles**: `application.yaml` is the base config (default active profile: `local`); `application-local.yml` and `application-dev.yml` hold environment-specific datasource settings. JWT secret/issuer/audience/validity live under `app.security.*` in `application.yaml` — treat the checked-in secret as a placeholder, not a real secret.
- **API docs**: springdoc-openapi is wired up with a `v1` group matching `/api/1/**`; Swagger UI is available unauthenticated at `/swagger-ui/**`.
