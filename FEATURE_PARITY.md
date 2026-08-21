# Feature parity matrix

| Capability | Control Center | Android Client | Android Admin | Android SDK |
|---|---|---|---|---|
| Health / readiness / version | production-ready | planned integration | implemented | implemented |
| Authentication / session | production-ready | next | implemented | transport implemented |
| Password change | production-ready | next | implemented | transport implemented |
| System status | production-ready | next | implemented | contract implemented |
| RBAC users read | production-ready | — | implemented | contract implemented |
| RBAC user create/block | production-ready | — | implemented | mutation transport implemented |
| Operations | production-ready | — | implemented | contract implemented |
| Audit | production-ready | — | implemented | contract implemented |
| Diagnostics summary/export | production-ready | planned | implemented | contract + binary GET implemented |
| Fleet node inventory | 1.1.x development | — | implementation branch | contract implemented |
| Fleet node registration | 1.1.x development | — | intentionally deferred until server slice acceptance | contract path implemented |
| Market / extended modules | later release | blocked by server | blocked by server | not exposed |

`1.1.x development` не является production availability claim. Мобильная capability считается готовой только после принятия соответствующего server-side slice, интеграции UI/state-management, build/lint/unit/API-contract/security checks и acceptance. Для Fleet мобильный Admin сначала получает безопасный inventory/monitoring сценарий; конфигурация enrollment остаётся в основном Web UI до отдельного mobile acceptance.
