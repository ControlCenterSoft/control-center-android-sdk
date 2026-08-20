package pro.controlcenter.sdk

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFailsWith
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
        assertFailsWith<IllegalArgumentException> {
            ControlCenterEndpoint.parse("http://control-center.example")
        }
    }

    @Test
    fun loopbackHttpRequiresExplicitOptIn() {
        assertFailsWith<IllegalArgumentException> {
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
        assertFailsWith<IllegalArgumentException> {
            ControlCenterEndpoint.parse("https://user:pass@control-center.example")
        }
        assertFailsWith<IllegalArgumentException> {
            ControlCenterEndpoint.parse("https://control-center.example/private")
        }
    }

    @Test
    fun resolvedPathCannotEscapeApiV1() {
        val endpoint = ControlCenterEndpoint.parse("https://control-center.example")
        assertFailsWith<IllegalArgumentException> {
            endpoint.resolve("/admin")
        }
        assertFailsWith<IllegalArgumentException> {
            endpoint.resolve("/api/v1/health?token=secret")
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
        assertFailsWith<IllegalStateException> {
            readBoundedUtf8(ByteArrayInputStream("abcde".toByteArray()), 4)
        }
    }
}
