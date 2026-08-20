package pro.controlcenter.sdk

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

private const val DEFAULT_CONNECT_TIMEOUT_MS = 5_000
private const val DEFAULT_READ_TIMEOUT_MS = 5_000
private const val DEFAULT_MAX_RESPONSE_BYTES = 1_048_576
private const val MAX_JSON_REQUEST_BYTES = 65_536
private val OPERATION_ID_RE = Regex("^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$")
private val API_PATH_RE = Regex("^/api/v1(?:/[A-Za-z0-9._~-]+)*$")
private val SESSION_TOKEN_RE = Regex("^[A-Za-z0-9_-]{32,256}$")
private const val SESSION_COOKIE_NAME = "cc_session"
private const val OPERATION_ID_HEADER = "X-Control-Center-Operation-ID"
private const val CSRF_HEADER = "X-CSRF-Token"

data class ControlCenterHttpResponse(
    val statusCode: Int,
    val body: String,
    val operationId: String?
)

data class ControlCenterBinaryResponse(
    val statusCode: Int,
    val body: ByteArray,
    val operationId: String?,
    val contentType: String?
)

class ControlCenterEndpoint private constructor(
    val baseUrl: String
) {
    companion object {
        fun parse(raw: String, allowInsecureLoopback: Boolean = false): ControlCenterEndpoint {
            val uri = try {
                URI(raw.trim())
            } catch (exception: Exception) {
                throw IllegalArgumentException("invalid Control Center base URL", exception)
            }

            require(uri.userInfo == null) { "base URL must not contain user info" }
            require(uri.query == null) { "base URL must not contain a query" }
            require(uri.fragment == null) { "base URL must not contain a fragment" }
            require(!uri.host.isNullOrBlank()) { "base URL must contain a host" }

            val scheme = uri.scheme?.lowercase()
            val loopback = uri.host.equals("localhost", ignoreCase = true) ||
                uri.host == "127.0.0.1" || uri.host == "::1"
            val secure = scheme == "https"
            val permittedLoopback = scheme == "http" && loopback && allowInsecureLoopback
            require(secure || permittedLoopback) {
                "base URL must use HTTPS; insecure HTTP is allowed only for explicit loopback development"
            }

            val path = uri.path.orEmpty().trimEnd('/')
            require(path.isEmpty()) { "base URL must not include an application path" }

            val normalized = URI(
                scheme,
                null,
                uri.host,
                uri.port,
                null,
                null,
                null
            ).toString().trimEnd('/')
            return ControlCenterEndpoint(normalized)
        }
    }

    fun resolve(path: String): URL {
        require(API_PATH_RE.matches(path)) {
            "path must be a canonical ${ControlCenterApiContract.API_BASE} path"
        }
        val segments = path.split('/').drop(3)
        require(segments.none { it == "." || it == ".." }) {
            "API path must not contain traversal segments"
        }
        return URL(baseUrl + path)
    }
}

/**
 * Ограниченный transport для API v1 Control Center.
 *
 * cc_session хранится только в памяти экземпляра и никогда не возвращается вызывающему коду.
 * Для POST-мутаций вызывающий код передаёт CSRF-токен, полученный из auth/login или auth/session.
 * Redirects запрещены, чтобы cookie и CSRF не могли уйти на другой origin.
 */
class BlockingControlCenterTransport(
    private val endpoint: ControlCenterEndpoint,
    private val connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = DEFAULT_READ_TIMEOUT_MS,
    private val maxResponseBytes: Int = DEFAULT_MAX_RESPONSE_BYTES
) {
    @Volatile
    private var sessionCookieValue: String? = null

    init {
        require(connectTimeoutMs in 1..60_000) { "connect timeout is out of range" }
        require(readTimeoutMs in 1..60_000) { "read timeout is out of range" }
        require(maxResponseBytes in 1..8_388_608) { "response limit is out of range" }
    }

    fun hasSession(): Boolean = sessionCookieValue != null

    fun clearSession() {
        sessionCookieValue = null
    }

    /** Выполняет GET. Вызывать из IO/background dispatcher. */
    fun get(path: String): ControlCenterHttpResponse = requestJson("GET", path, null, null)

    /**
     * Выполняет JSON POST. Для login csrfToken не нужен; для logout/password/RBAC mutations обязателен.
     */
    fun postJson(path: String, jsonBody: String, csrfToken: String? = null): ControlCenterHttpResponse {
        val bytes = jsonBody.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_JSON_REQUEST_BYTES) { "JSON request body exceeds server limit" }
        return requestJson("POST", path, bytes, csrfToken)
    }

    /** Получает бинарный ответ, например diagnostics/export, с теми же transport-ограничениями. */
    fun getBytes(path: String): ControlCenterBinaryResponse {
        val connection = open("GET", path, null, null)
        try {
            val statusCode = connection.responseCode
            captureSessionCookie(connection)
            val stream = responseStream(connection, statusCode)
            val body = stream?.use { readBoundedBytes(it, maxResponseBytes) } ?: ByteArray(0)
            return ControlCenterBinaryResponse(
                statusCode = statusCode,
                body = body,
                operationId = validatedOperationId(connection.getHeaderField(OPERATION_ID_HEADER)),
                contentType = connection.contentType
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun requestJson(
        method: String,
        path: String,
        body: ByteArray?,
        csrfToken: String?
    ): ControlCenterHttpResponse {
        val connection = open(method, path, body, csrfToken)
        try {
            val statusCode = connection.responseCode
            captureSessionCookie(connection)
            val stream = responseStream(connection, statusCode)
            val responseBody = stream?.use { readBoundedUtf8(it, maxResponseBytes) }.orEmpty()
            return ControlCenterHttpResponse(
                statusCode = statusCode,
                body = responseBody,
                operationId = validatedOperationId(connection.getHeaderField(OPERATION_ID_HEADER))
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun open(
        method: String,
        path: String,
        body: ByteArray?,
        csrfToken: String?
    ): HttpURLConnection {
        require(method == "GET" || method == "POST") { "unsupported HTTP method" }
        require(csrfToken == null || csrfToken.isNotBlank()) { "CSRF token must not be blank" }

        val connection = endpoint.resolve(path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        connection.connectTimeout = connectTimeoutMs
        connection.readTimeout = readTimeoutMs
        connection.setRequestProperty("Accept", "application/json")

        sessionCookieValue?.let {
            connection.setRequestProperty("Cookie", "$SESSION_COOKIE_NAME=$it")
        }
        csrfToken?.let {
            connection.setRequestProperty(CSRF_HEADER, it)
        }

        if (body != null) {
            connection.doOutput = true
            connection.setFixedLengthStreamingMode(body.size)
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(body) }
        }
        return connection
    }

    private fun captureSessionCookie(connection: HttpURLConnection) {
        val update = extractSessionCookie(connection.headerFields)
        if (update.present) {
            sessionCookieValue = update.value
        }
    }
}

internal data class SessionCookieUpdate(
    val present: Boolean,
    val value: String?
)

internal fun extractSessionCookie(headers: Map<String?, List<String>>): SessionCookieUpdate {
    val values = headers.entries
        .filter { (name, _) -> name?.equals("Set-Cookie", ignoreCase = true) == true }
        .flatMap { it.value }

    for (header in values) {
        val pair = header.substringBefore(';')
        val separator = pair.indexOf('=')
        if (separator <= 0) continue
        val name = pair.substring(0, separator).trim()
        if (name != SESSION_COOKIE_NAME) continue
        val value = pair.substring(separator + 1).trim()
        if (value.isEmpty()) {
            return SessionCookieUpdate(present = true, value = null)
        }
        if (!SESSION_TOKEN_RE.matches(value)) {
            return SessionCookieUpdate(present = true, value = null)
        }
        return SessionCookieUpdate(present = true, value = value)
    }
    return SessionCookieUpdate(present = false, value = null)
}

internal fun validatedOperationId(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty() || !OPERATION_ID_RE.matches(value)) return null
    return value
}

private fun responseStream(connection: HttpURLConnection, statusCode: Int): InputStream? =
    if (statusCode in 200..399) connection.inputStream else connection.errorStream

internal fun readBoundedUtf8(input: InputStream, maxBytes: Int): String =
    readBoundedBytes(input, maxBytes).toString(Charsets.UTF_8)

internal fun readBoundedBytes(input: InputStream, maxBytes: Int): ByteArray {
    require(maxBytes > 0) { "maxBytes must be positive" }
    val buffer = ByteArray(8_192)
    val output = ByteArrayOutputStream(minOf(maxBytes, 32_768))
    var total = 0
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) {
            throw IllegalStateException("response body exceeds configured limit")
        }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}
