# URL Shortener — Risk Register

Risks identified against the current roadmap, with likelihood, impact, and planned
mitigation. This is a living document — update it as phases are implemented and new
risks surface.

| # | Risk | Related Phase | Likelihood | Impact | Mitigation |
|---|---|---|---|---|---|
| 1 | DB-table-as-queue becomes a write bottleneck as click volume grows | Phase 2 | Medium | High | Documented revisit trigger in ADR 0001; monitor queue drain rate vs. write rate |
| 2 | Unbounded growth of the raw click-event table | Phase 2 | High | Medium | Define and implement a retention/archival policy before Phase 2 ships |
| 3 | "Paid" tier is trivially spoofable since there's no real payment gate | Phase 3 | High | Low (learning project; would be High for a real product) | Explicitly documented as out-of-scope trust boundary in ADR 0002; flag clearly if project scope ever expands to real users |
| 4 | Temporary links never expire due to missing/failed cleanup job | Phase 3 | Medium | Medium | Implement TTL check at both write-time (scheduled cleanup) and read-time (lazy expiry on access) as a fallback |
| 5 | Batch creation floods the database and degrades redirect latency for existing links | Phase 5 | Medium | High | Rate-limit/throttle batch jobs; consider async job processing instead of synchronous batch requests |
| 6 | Short-code collisions surface as 500 errors today and become likelier as link volume grows | Phase 1 | Low–Medium | Medium | Collision-checked generation with retry (URLS-8); revisit code length/entropy if volume grows significantly |
| 7 | Batch creation endpoint abused for spam/malicious link generation | Phase 5 | Medium | Medium | Tie batch creation limits to the tier model (Phase 3); add basic abuse detection before any public exposure |
| 8 | Incomplete auth surface (password change / refresh token handling; now largely implemented, hardening remains) creates account-security gaps in the interim | Phase 1 | Medium | Medium | Prioritize refresh token handling early within Phase 1, since it has the largest security implication of the group |
| 9 | Custom short codes let users probe for existing codes or squat reserved/route-like codes | Phase 4 | Medium | Medium | Per-user cap on custom codes (URLS-49), existing redirect rate limiting, and a reserved-code denylist |

## How to Use This Document
Before starting each phase, review the rows tagged to it and confirm the mitigation
is either already implemented or explicitly planned as part of that phase's scope.
Add new rows as new risks are identified during implementation — a risk register
that isn't updated after go-live isn't doing its job.
