package pro.controlcenter.sdk

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TransportTest {
    @Test
    fun platformPathsMatchServerContract() {
        assertEquals("/api/v1/health", ControlCenterApiContract.Platform.HEALTH)
        assertEquals("/api/v1/readiness", ControlCenterApiContract.Platform.READINESS)
        assertEquals("/api/v1/version", ControlCenterApiContract.Platform.VERSION)
        assertEquals("/api/v1/release", ControlCenterApiContract.Platform.RELEASE)
    }

    @Test
    fun httpsEndpointIsAccepted() {
        val endpoint = ControlCenterEndpoint.parse("https://control-center.example")
        assertEquals(
            "https://control-center.example/api/v1/health",
            endpoint.resolve(ControlCenterApiContract.Platform.HEALTH).toString()
        )
    }

    @Test
    fun publicHttpEndpointIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ControlCenterEndpoint.parse("http://control-center.example")
        }
    }

    @Test
    fun loopbackHttpRequiresExplicitOptIn() {
        assertThrows(IllegalArgumentException::class.java) {
            ControlCenterEndpoint.parse("http://127.0.0.1:8876")
        }
        val endpoint = ControlCenterEndpoint.parse(
            "http://127.0.0.1:8876",
            allowInsecureLoopback = true
        )
        assertEquals(
            "http://127.0.0.1:8876/api/v1/version",
            endpoint.resolve(ControlCenterApiContract.Platform.VERSION).toString()
        )
    }

    @Test
    fun baseUrlCannotCarryCredentialsOrApplicationPath() {
        assertThrows(IllegalArgumentException::class.java) {
            ControlCenterEndpoint.parse("https://user:pass@control-center.example")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ControlCenterEndpoint.parse("https://control-center.example/private")
        }
    }

    @Test
    fun resolvedPathCannotEscapeApiV1() {
        val endpoint = ControlCenterEndpoint.parse("https://control-center.example")
        assertThrows(IllegalArgumentException::class.java) {
            endpoint.resolve("/admin")
        }
        assertThrows(IllegalArgumentException::class.java) {
            endpoint.resolve("/api/v1/health?token=secret")
        }
        assertThrows(IllegalArgumentException::class.java) {
            endpoint.resolve("/api/v1/../admin")
        }
        assertThrows(IllegalArgumentException::class.java) {
            endpoint.resolve("/api/v1/%2e%2e/admin")
        }
    }

    @Test
    fun boundedReaderAcceptsBodyAtLimit() {
        val body = "abcd"
        val result = readBoundedUtf8(ByteArrayInputStream(body.toByteArray()), 4)
        assertEquals(body, result)
    }

    @Test
    fun boundedReaderRejectsOversizedBody() {
        assertThrows(IllegalStateException::class.java) {
            readBoundedUtf8(ByteArrayInputStream("abcde".toByteArray()), 4)
        }
    }
}
