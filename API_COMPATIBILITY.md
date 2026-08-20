# API compatibility matrix

| Consumer | Client version | Platform API |
|---|---:|---:|
| Website / Portal | 1.1.0-dev.1 | v1 |
| Android Client | 0.1.0 | >=1.0 <2.0 |
| Android Admin | 0.1.0 | >=1.0 <2.0 |
| Android SDK | 0.2.0-dev.1 | >=1.0 <2.0 |

## Принятый серверный baseline Control Center 1.0.0

SDK синхронизирован с фактически зарегистрированными server endpoints:

- `GET /api/v1/health`
- `GET /api/v1/readiness`
- `GET /api/v1/version`
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/session`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/password`
- `GET /api/v1/system/status`
- `GET /api/v1/rbac/users`
- `POST /api/v1/rbac/users`
- `POST /api/v1/rbac/users/{username}/blocked`
- `GET /api/v1/operations`
- `GET /api/v1/audit`
- `GET /api/v1/diagnostics/summary`
- `GET /api/v1/diagnostics/export`

Удалены прежние speculative-контракты `/account/*`, `/admin/*` и `/api/v1/release`, которых нет в принятом HTTP router Control Center 1.0.0.

## Security transport baseline 0.2.0-dev.1

- HTTPS обязателен для non-loopback endpoint;
- insecure HTTP допускается только для явно разрешённого loopback development;
- redirects запрещены;
- `cc_session` хранится только в памяти transport-объекта и не возвращается вызывающему коду;
- CSRF передаётся только заголовком `X-CSRF-Token` для mutation requests;
- используется серверный `X-Control-Center-Operation-ID`;
- JSON request body ограничен серверным пределом 64 KiB;
- response body ограничен конфигурируемым fail-closed лимитом;
- бинарный GET поддержан для `diagnostics/export`;
- пути ограничены каноническим `/api/v1` namespace.

Breaking API changes требуют нового major contract или явного migration contract. Мобильные клиенты не являются источником canonical product state и не подменяют server-side RBAC.
