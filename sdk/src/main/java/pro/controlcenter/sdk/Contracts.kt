package pro.controlcenter.sdk

object ControlCenterApiContract {
    const val API_MAJOR = 1
    const val API_BASE = "/api/v1"

    object Platform {
        const val HEALTH = "$API_BASE/health"
        const val READINESS = "$API_BASE/readiness"
        const val VERSION = "$API_BASE/version"
    }

    object Auth {
        const val LOGIN = "$API_BASE/auth/login"
        const val SESSION = "$API_BASE/auth/session"
        const val LOGOUT = "$API_BASE/auth/logout"
        const val PASSWORD = "$API_BASE/auth/password"
    }

    object System {
        const val STATUS = "$API_BASE/system/status"
    }

    object Rbac {
        const val USERS = "$API_BASE/rbac/users"
        private val USERNAME_RE = Regex("^[a-z][a-z0-9._-]{2,63}$")

        fun blocked(username: String): String {
            val normalized = username.trim().lowercase()
            require(USERNAME_RE.matches(normalized)) {
                "username must match ^[a-z][a-z0-9._-]{2,63}$"
            }
            return "$USERS/$normalized/blocked"
        }
    }

    object Operations {
        const val LIST = "$API_BASE/operations"
    }

    object Audit {
        const val LIST = "$API_BASE/audit"
    }

    object Diagnostics {
        const val SUMMARY = "$API_BASE/diagnostics/summary"
        const val EXPORT = "$API_BASE/diagnostics/export"
    }

    object Fleet {
        const val NODES = "$API_BASE/fleet/nodes"
    }
}

enum class Role(val wireValue: String) {
    VIEWER("viewer"),
    ADMIN("admin");

    companion object {
        fun fromWire(value: String): Role = entries.firstOrNull { it.wireValue == value }
            ?: throw IllegalArgumentException("unsupported role: $value")
    }
}

enum class Permission(val wireValue: String) {
    SYSTEM_READ("system.read"),
    RBAC_USERS_READ("rbac.users.read"),
    RBAC_USERS_WRITE("rbac.users.write"),
    OPERATIONS_READ("operations.read"),
    AUDIT_READ("audit.read"),
    DIAGNOSTICS_EXPORT("diagnostics.export"),
    FLEET_NODES_READ("fleet.nodes.read"),
    FLEET_NODES_WRITE("fleet.nodes.write")
}

data class ApiError(
    val code: String,
    val message: String,
    val operationId: String? = null
)

data class PublicUser(
    val username: String,
    val role: Role,
    val blocked: Boolean,
    val mustChangePassword: Boolean,
    val createdAt: String,
    val passwordChangedAt: String
)

data class AuthSession(
    val user: PublicUser,
    val csrfToken: String
)

data class PlatformHealth(
    val status: String,
    val service: String,
    val time: String
)

data class ReadinessCheck(
    val name: String,
    val ok: Boolean
)

data class PlatformReadiness(
    val status: String,
    val ready: Boolean,
    val checks: List<ReadinessCheck>
)

data class PlatformVersion(
    val product: String,
    val version: String,
    val commit: String,
    val builtAt: String,
    val stateSchema: Int,
    val operationsSchema: Int
)

data class PasswordChangeResult(
    val status: String,
    val reauthenticationRequired: Boolean
)

data class FleetNode(
    val id: String,
    val name: String,
    val address: String,
    val group: String,
    val environment: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

data class FleetSummary(
    val total: Int,
    val pendingEnrollment: Int
)

/**
 * Состояние мобильной аутентификации без раскрытия значения cc_session.
 * Сам session cookie хранится только внутри transport-слоя и не должен персистироваться клиентом.
 */
data class MobileSessionState(
    val authenticated: Boolean,
    val csrfToken: String? = null
)
