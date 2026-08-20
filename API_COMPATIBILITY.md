# API compatibility matrix

| Consumer | Client version | Platform API |
|---|---:|---:|
| Website / Portal | 1.1.0-dev.1 | v1 |
| Android Client | 0.1.0 | >=1.0 <2.0 |
| Android Admin | 0.1.0 | >=1.0 <2.0 |

## Implemented Platform v1 baseline

The Android SDK contract now matches the server endpoints:

- `/api/v1/health`
- `/api/v1/readiness`
- `/api/v1/version`
- `/api/v1/release`

The 0.1.0 transport is intentionally GET-only and bounded. It requires HTTPS for non-loopback endpoints, does not follow redirects, validates correlation IDs and rejects non-canonical paths outside `/api/v1`.

Authentication and privileged write operations are not part of this baseline. They will be introduced only together with the server-side session/RBAC/audit contract.

Breaking API changes require a new major contract or an explicit migration contract. Mobile clients do not own canonical product state.
