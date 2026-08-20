# Feature parity matrix

| Capability | Control Center 1.0.0 | Android Client | Android Admin | SDK 0.2.0-dev.1 |
|---|---|---|---|---|
| Health / readiness / version | production-ready | planned integration | planned integration | implemented |
| Authentication / session | production-ready | next | next | transport implemented |
| Password change | production-ready | next | next | transport implemented |
| System status | production-ready | next | next | contract implemented |
| RBAC users read | production-ready | — | next | contract implemented |
| RBAC user create/block | production-ready | — | next | mutation transport implemented |
| Operations | production-ready | — | next | contract implemented |
| Audit | production-ready | — | next | contract implemented |
| Diagnostics summary/export | production-ready | planned | next | contract + binary GET implemented |
| Market / extended modules | later release | blocked by server | blocked by server | not exposed |

`next` означает следующий мобильный этап, но не production availability. Клиентская возможность считается готовой только после интеграции UI/state-management, собственных build/lint/unit/API-contract/security проверок и acceptance.
