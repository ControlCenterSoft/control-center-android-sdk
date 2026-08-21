package pro.controlcenter.sdk

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TransportTest {
    @Test
    fun platformPathsMatchServerContract() {
        assertEquals("/api/v1/health", ControlCenterApiContract.Platform.HEALTH)
        assertEquals("/api/v1/readiness", ControlCenterApiContract.Platform.READINESS)
        assertEquals("/api/v1/version", ControlCenterApiContract.Platform.VERSION)
        assertEquals("/api/v1/auth/login", ControlCenterApiContract.Auth.LOGIN)
        assertEquals("/api/v1/auth/session", ControlCenterApiContract.Auth.SESSION)
        assertEquals("/api/v1/auth/logout", ControlCenterApiContract.Auth.LOGOUT)
        assertEquals("/api/v1/auth/password", ControlCenterApiContract.Auth.PASSWORD)
        assertEquals("/api/v1/system/status", ControlCenterApiContract.System.STATUS)
        assertEquals("/api/v1/fleet/nodes", ControlCenterApiContract.Fleet.NODES)
        assertEquals("/api/v1/rbac/users", ControlCenterApiContract.Rbac.USERS)
        assertEquals("/api/v1/operations", ControlCenterApiContract.Operations.LIST)
        assertEquals("/api/v1/audit", ControlCenterApiContract.Audit.LIST)
        assertEquals("/api/v1/diagnostics/summary", ControlCenterApiContract.Diagnostics.SUMMARY)
        assertEquals("/api/v1/diagnostics/export", ControlCenterApiContract.Diagnostics.EXPORT)
    }

    @Test
    fun fleetModelsPreserveServerInventorySemantics() {
        val node = FleetNode(
            id = "srv-01",
            name = "srv-01",
            address = "10.10.0.11",
            group = "office",
            environment = "production",
            status = "pending_enrollment",
            createdAt = "2026-08-21T08:00:00Z",
            updatedAt = "2026-08-21T08:00:00Z"
        )
        val inventory = FleetInventory(
            nodes = listOf(node),
            summary = FleetSummary(total = 1, pendingEnrollment = 1)
        )
        assertEquals("pending_enrollment", inventory.nodes.single().status)
        assertEquals(1, inventory.summary.total)
        assertEquals(1, inventory.summary.pendingEnrollment)
    }

    @Test
    fun rbacBlockedPathUsesServerUsernameContract() {
        assertEquals(
            "/api/v1/rbac/users/test.user/blocked",
            ControlCenterApiContract.Rbac.blocked(" TEST.User ")
        )
        assertThrows(IllegalArgumentException::class.java) {
            ControlCenterApiContract.Rbac.blocked("x")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ControlCenterApiContract.Rbac.blocked("admin/../root")
        }
    }

    @Test
    fun roleWireValuesMatchServerContract() {
        assertEquals("viewer", Role.VIEWER.wireValue)
        assertEquals("admin", Role.ADMIN.wireValue)
        assertEquals(Role.ADMIN, Role.fromWire("admin"))
        assertThrows(IllegalArgumentException::class.java) {
            Role.fromWire("owner")
        }
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
    fun sessionCookieIsCapturedWithoutExposingAttributes() {
        val update = extractSessionCookie(
            mapOf(
                "Set-Cookie" to listOf(
                    "cc_session=abcdefghijklmnopqrstuvwxyzABCDEFGH_123456789; Path=/; HttpOnly; Secure; SameSite=Strict"
                )
            )
        )
        assertTrue(update.present)
        assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGH_123456789", update.value)
    }

    @Test
    fun clearedOrMalformedSessionCookieFailsClosed() {
        val cleared = extractSessionCookie(mapOf("set-cookie" to listOf("cc_session=; Max-Age=0; Path=/")))
        assertTrue(cleared.present)
        assertNull(cleared.value)

        val malformed = extractSessionCookie(mapOf("Set-Cookie" to listOf("cc_session=bad token; Path=/")))
        assertTrue(malformed.present)
        assertNull(malformed.value)

        val absent = extractSessionCookie(mapOf("Content-Type" to listOf("application/json")))
        assertFalse(absent.present)
        assertNull(absent.value)
    }

    @Test
    fun operationIdUsesControlCenterHeaderContract() {
        assertEquals("0123456789abcdef0123456789abcdef", validatedOperationId("0123456789abcdef0123456789abcdef"))
        assertNull(validatedOperationId("bad id with spaces"))
        assertNull(validatedOperationId(null))
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
