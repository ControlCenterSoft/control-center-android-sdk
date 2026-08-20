package pro.controlcenter.sdk

object ControlCenterApiContract {
    const val API_MAJOR = 1
    const val API_BASE = "/api/v1"

    object Platform {
        const val HEALTH = "$API_BASE/health"
        const val READINESS = "$API_BASE/readiness"
        const val VERSION = "$API_BASE/version"
        const val RELEASE = "$API_BASE/release"
    }

    object Account {
        const val PROFILE = "$API_BASE/account"
        const val SERVERS = "$API_BASE/account/servers"
        const val SESSIONS = "$API_BASE/account/sessions"
        const val NOTIFICATIONS = "$API_BASE/account/notifications"
    }

    object Admin {
        const val OVERVIEW = "$API_BASE/admin/overview"
        const val USERS = "$API_BASE/admin/users"
        const val SERVERS = "$API_BASE/admin/servers"
        const val RELEASES = "$API_BASE/admin/releases"
        const val OPERATIONS = "$API_BASE/admin/operations"
        const val AUDIT = "$API_BASE/admin/audit"
    }
}

enum class ClientKind {
    WEB,
    ANDROID_CLIENT,
    ANDROID_ADMIN
}

enum class Role {
    VIEWER,
    ADMIN
}

enum class Permission {
    ACCOUNT_READ,
    ACCOUNT_SESSION_REVOKE,
    SERVER_READ,
    DIAGNOSTICS_READ,
    ADMIN_USERS_READ,
    ADMIN_SERVERS_READ,
    ADMIN_RELEASES_READ,
    ADMIN_AUDIT_READ
}

data class ApiError(
    val code: String,
    val message: String,
    val correlationId: String? = null
)

data class PlatformHealth(
    val status: String,
    val service: String,
    val apiVersion: Int
)

data class PlatformReadiness(
    val status: String,
    val service: String,
    val deploymentManifest: String,
    val release: String? = null
)

data class PlatformVersion(
    val service: String,
    val apiVersion: Int,
    val product: String,
    val version: String,
    val channel: String,
    val status: String,
    val acceptance: String,
    val sourceSha: String? = null
)

data class SessionInfo(
    val sessionId: String,
    val userId: String,
    val role: Role,
    val client: ClientKind,
    val active: Boolean
)

data class ServerSummary(
    val id: String,
    val name: String,
    val version: String,
    val online: Boolean,
    val health: String,
    val readiness: String
)

data class ReleaseSummary(
    val version: String,
    val channel: String,
    val status: String,
    val acceptance: String
)

data class AccountSummary(
    val userId: String,
    val displayName: String,
    val role: Role,
    val serverCount: Int
)

data class AdminOverview(
    val clients: Int,
    val serversOnline: Int,
    val serversOffline: Int,
    val activeOperations: Int,
    val securityEvents: Int
)

interface SessionController {
    suspend fun logout()
    suspend fun revokeSession(sessionId: String)
}
