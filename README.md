# Control Center Android SDK

Shared Android API contract baseline for Control Center Client and Admin.

Version: **0.1.0**.

## Current scope

- Platform API v1 path contract
- health/readiness/version/release models
- shared account/admin models and role/permission enums
- bounded GET-only `HttpURLConnection` transport
- HTTPS required for non-loopback endpoints
- redirects disabled
- bounded response body
- correlation ID validation/propagation
- canonical `/api/v1` path enforcement

## Security boundary

Server-side authorization, privileged operations and secrets are explicitly out of scope for the SDK. The SDK never turns UI visibility into authorization and does not embed production credentials.

Authentication will be added only when the Control Center server session/RBAC/audit contract is implemented and tested.

See `API_COMPATIBILITY.md` for the consumer compatibility matrix.
