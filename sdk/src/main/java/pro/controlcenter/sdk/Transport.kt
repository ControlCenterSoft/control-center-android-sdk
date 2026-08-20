package pro.controlcenter.sdk

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

private const val DEFAULT_CONNECT_TIMEOUT_MS = 5_000
private const val DEFAULT_READ_TIMEOUT_MS = 5_000
private const val DEFAULT_MAX_RESPONSE_BYTES = 1_048_576
private val CORRELATION_ID_RE = Regex("^[A-Za-z0-9][A-Za-z0-9._:-]{0,63}$")

data class ControlCenterHttpResponse(
    val statusCode: Int,
    val body: String,
    val correlationId: String?
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
        require(path == ControlCenterApiContract.API_BASE || path.startsWith("${ControlCenterApiContract.API_BASE}/")) {
            "path must stay inside ${ControlCenterApiContract.API_BASE}"
        }
        require(!path.contains('?') && !path.contains('#')) {
            "API path must not contain query or fragment data"
        }
        return URL(baseUrl + path)
    }
}

class BlockingControlCenterTransport(
    private val endpoint: ControlCenterEndpoint,
    private val connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = DEFAULT_READ_TIMEOUT_MS,
    private val maxResponseBytes: Int = DEFAULT_MAX_RESPONSE_BYTES
) {
    init {
        require(connectTimeoutMs in 1..60_000) { "connect timeout is out of range" }
        require(readTimeoutMs in 1..60_000) { "read timeout is out of range" }
        require(maxResponseBytes in 1..8_388_608) { "response limit is out of range" }
    }

    /**
     * Performs one bounded GET request. Call this from an IO/background dispatcher.
     * Authentication is intentionally not part of the 0.1.0 transport baseline.
     */
    fun get(path: String, correlationId: String? = null): ControlCenterHttpResponse {
        require(correlationId == null || CORRELATION_ID_RE.matches(correlationId)) {
            "invalid correlation ID"
        }

        val connection = endpoint.resolve(path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.setRequestProperty("Accept", "application/json")
            if (correlationId != null) {
                connection.setRequestProperty("X-Correlation-ID", correlationId)
            }

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..399) connection.inputStream else connection.errorStream
            val body = stream?.use { readBoundedUtf8(it, maxResponseBytes) }.orEmpty()
            return ControlCenterHttpResponse(
                statusCode = statusCode,
                body = body,
                correlationId = connection.getHeaderField("X-Correlation-ID")
            )
        } finally {
            connection.disconnect()
        }
    }
}

internal fun readBoundedUtf8(input: InputStream, maxBytes: Int): String {
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
    return output.toString(Charsets.UTF_8.name())
}
