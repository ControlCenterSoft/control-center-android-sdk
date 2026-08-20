# Control Center Android SDK

Общая базовая линия Android API-контрактов для Control Center Client и Control Center Admin.

Версия: **0.1.0**.

Язык проектной документации: **русский**. Технические идентификаторы, команды и имена API сохраняются в исходном виде.

## Текущий scope

- контракт путей Platform API v1;
- модели health/readiness/version/release;
- общие модели account/admin и enum ролей/permissions;
- ограниченный GET-only transport на `HttpURLConnection`;
- обязательный HTTPS для endpoint вне loopback;
- отключённые redirects;
- ограниченный размер response body;
- проверка и передача correlation ID;
- принудительное использование канонических путей `/api/v1`.

## Граница безопасности

Server-side authorization, привилегированные операции и секреты намеренно не входят в scope SDK. SDK никогда не превращает видимость элементов UI в авторизацию и не содержит production credentials.

Аутентификация будет добавлена только после реализации и проверки server-side session/RBAC/audit-контракта Control Center.

Матрица совместимости потребителей находится в `API_COMPATIBILITY.md`.
