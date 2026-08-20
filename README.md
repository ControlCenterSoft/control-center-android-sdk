# Control Center Android SDK

Общая базовая линия Android API-контрактов для Control Center Client и Control Center Admin.

Текущая development-версия: **0.2.0-dev.1**.

Язык проектной документации: **русский**. Технические идентификаторы, команды и имена API сохраняются в исходном виде.

## Текущий scope

- фактический HTTP contract Control Center 1.0.0 для Platform/Auth/System/RBAC/Operations/Audit/Diagnostics;
- Kotlin-модели принятого API v1;
- ограниченный `HttpURLConnection` transport для GET и JSON POST;
- in-memory обработка `cc_session` без раскрытия session token вызывающему коду;
- `X-CSRF-Token` для mutation requests;
- `X-Control-Center-Operation-ID` из серверного ответа;
- бинарный GET для diagnostics export;
- обязательный HTTPS для endpoint вне loopback;
- отключённые redirects;
- лимиты request/response body;
- принудительное использование канонических путей `/api/v1`.

## Граница безопасности

Авторизация всегда остаётся server-side. SDK не превращает локальную видимость UI в permission, не содержит production credentials, не выполняет команды от root и не хранит `cc_session` в persistent storage.

Transport хранит session cookie только в памяти экземпляра. CSRF-токен остаётся отдельным значением session state и передаётся только в mutation request. При logout/password-change приложение должно очистить локальное session state в соответствии с ответом сервера.

Матрица совместимости находится в `API_COMPATIBILITY.md`, а синхронизация функций — в `FEATURE_PARITY.md`.
