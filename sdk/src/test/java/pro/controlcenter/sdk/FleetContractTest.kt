package pro.controlcenter.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FleetContractTest {
    @Test
    fun fleetNodesPathMatchesCoreV1() {
        assertEquals("/api/v1/fleet/nodes", ControlCenterApiContract.Fleet.NODES)
    }

    @Test
    fun enrollmentPathIsDerivedFromValidatedNodeId() {
        assertEquals(
            "/api/v1/fleet/nodes/node-01/enrollment",
            ControlCenterApiContract.Fleet.enrollment(" node-01 ")
        )
    }

    @Test
    fun enrollmentRejectsPathInjection() {
        assertThrows(IllegalArgumentException::class.java) {
            ControlCenterApiContract.Fleet.enrollment("node/../../admin")
        }
    }

    @Test
    fun fleetSummaryCarriesCoreEnrollmentCounters() {
        val summary = FleetSummary(total = 4, pendingEnrollment = 1, enrolled = 3)
        assertEquals(4, summary.total)
        assertEquals(1, summary.pendingEnrollment)
        assertEquals(3, summary.enrolled)
    }
}
