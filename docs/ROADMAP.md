# URL Shortener — Roadmap

This roadmap sequences the planned work by dependency and risk, not just by feature
appeal. Each phase lists its goal, scope, dependencies on prior phases, and open
decisions still to be made.

## Status Overview

| Phase | Name | Status |
|---|---|---|
| 0 | Foundation — Core Shortening, Redirect & Authentication | ✅ Complete |
| 1 | Account/Auth API Completion | 🚧 In progress (core features done, hardening open) |
| 2 | Click Analytics / Audit Log | ⏳ Planned |
| 3 | Temporary vs. Permanent Links (Tiered Access) | ⏳ Planned |
| 4 | Link Customisation — Custom Short Codes & QR Codes | ⏳ Planned |
| 5 | Batch Link Creation (CSV Import) — *stretch goal* | ⏳ Planned, optional |

---

## Phase 0 — Foundation: Core Shortening, Redirect & Authentication
**Status:** ✅ Complete

**Goal:** A working, secured baseline that later phases build on.

**Delivered:**
- Short-link creation for authenticated users and a public redirect endpoint.
- User registration and JWT-based login.
- MySQL persistence with Flyway-managed schema migrations.
- Secrets (DB credentials, JWT signing key) read from environment variables.
- IP-based rate limiting on login, registration, link creation and redirect.
- URL validation restricting links to `http`/`https` and rejecting private IPs.
- Unit and integration test suite with JaCoCo coverage, and a GitHub Actions
  workflow reporting test results.
- OpenAPI / Swagger UI with Bearer-token support.

**Not included (deferred to later phases):** analytics, link tiers, link customisation, batch creation.

---

## Phase 1 — Account/Auth API Completion
**Status:** 🚧 In progress — core features implemented, hardening follow-ups open

**Goal:** Close out the remaining account management surface before building
link-lifecycle features on top of it.

**Scope:**
- Password change endpoint. *(implemented)*
- Account deactivation (soft delete) and recovery-code password reset. *(implemented)*
- Refresh token handling with rotation and a per-user session cap. *(implemented)*
- Logout with JWT revocation. *(implemented)*
- Remaining hardening follow-ups (account-status checks, login lockout, rate-limit
  proxy handling) are tracked in Jira (URLS-52, URLS-54, URLS-60).
- Related reliability fixes that every later phase depends on: collision-safe
  short-code generation with retry (URLS-8) and a configurable short-URL base
  (URLS-59).

**Dependencies:** None functionally. Sequenced first because an incomplete auth
surface is a security gap (see risk #8 in [RISKS.md](./RISKS.md)) and every later
phase depends on authenticated, per-user behaviour.

---

## Phase 2 — Click Analytics / Audit Log
**Status:** ⏳ Planned

**Goal:** Capture a click event for every redirect, without adding infrastructure the
project doesn't yet need.

**Scope:**
- Log each click (link ID, timestamp, and minimal metadata) to a database table used
  as a lightweight queue.
- A background worker/consumer drains the table into aggregated analytics on a
  schedule.

**Explicitly deferred:** Kafka or other streaming platforms — see
[ADR 0001](./adr/0001-click-analytics-storage-strategy.md) for the reasoning.

**Dependencies:** None technically; sequenced after Phase 1 so the auth surface is
complete before new link features are layered on.

**Open decisions:**
- Retention/archival policy for raw click rows once aggregated (needed before this
  table grows unbounded).
- Whether a failed click write should fail open (redirect anyway) or fail closed;
  this trades the 100% capture target against redirect latency.

See the open-questions table in [PRD.md](./PRD.md#8-open-questions).

---

## Phase 3 — Temporary vs. Permanent Links (Tiered Access)
**Status:** ⏳ Planned

**Goal:** Differentiate link lifecycle and map it to a free/paid usage concept —
without building payment infrastructure.

**Scope:**
- Add a link "type" (temporary / permanent) with an expiry mechanism for temporary
  links.
- Add a usage tier concept (e.g. free = temporary + limited count, paid = permanent +
  higher limits), represented as a flag/plan field rather than a real billing system.

**Explicitly deferred:** Payment gateway integration.

**Dependencies:** Benefits from Phase 2 being in place, so tier limits can be
validated against real usage data rather than guesses.

**Open decisions:**
- How a user is marked "paid" without a payment flow (admin flag, invite/coupon code
  or config allowlist).
- What happens to a temporary link at expiry (404 vs. 410, soft vs. hard delete, and
  whether the code can be reused).

See the open-questions table in [PRD.md](./PRD.md#8-open-questions).

---

## Phase 4 — Link Customisation: Custom Short Codes & QR Codes
**Status:** ⏳ Planned

**Goal:** Let users personalise links in the ways short-link users most commonly
expect, without slowing down plain link creation.

**Scope:**
- User-chosen custom short codes (URLS-49), capped per user (e.g. 5 at a time) so
  the feature can't be used to probe for existing codes.
- A separate QR code API for a short link (URLS-51), kept out of the creation call
  so creation stays fast. The generator sits behind an interface so a library can
  later be swapped for an external service.

**Dependencies:** Custom-code caps reuse the tier limits from Phase 3.

**Open decision:** Reserved or disallowed codes (e.g. `api`, `login`, `swagger-ui`)
that must not collide with application routes.

---

## Phase 5 — Batch Link Creation (CSV Import) — Stretch Goal
**Status:** ⏳ Planned, optional

**Goal:** Let power users create many links in one operation (a few thousand rows
per job) for import and programmatic use. This is a niche feature in real products
and is deliberately last; it is mainly valuable as an exercise in async job design
and backpressure.

**Scope:**
- CSV upload (or batch endpoint) processed as an async job with a status endpoint.
- Batched inserts and collision-safe code generation under load.
- Backpressure/rate-limiting so batch jobs don't degrade the core redirect service.

**Dependencies:** Follows Phase 3, since batch creation must respect the same tier
limits (a free-tier user shouldn't create past their allowance).

**Open decision:** Synchronous request vs. async job with a status/polling endpoint —
this is a scale and UX trade-off worth its own short design note if the phase is
started.

---

## Sequencing Rationale
Phase 1 comes first because every later feature builds on authenticated users, and
an incomplete auth surface is the largest security gap. Phases 2 → 3 → 4 → 5 are
then ordered by dependency and user value: analytics data informs tier limits, tier
limits need to exist before custom codes and batch creation can enforce their caps,
and batch creation comes last because it is the most niche feature.
