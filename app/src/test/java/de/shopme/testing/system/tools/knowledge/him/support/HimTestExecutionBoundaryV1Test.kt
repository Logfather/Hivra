package de.shopme.testing.system.tools.knowledge.him.support

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HimTestExecutionBoundaryV1Test {
    @Test fun sourceMissingSkips() = assertFalse(HimTestExecutionBoundaryV1.sourceIntegrationEnabled(null))
    @Test fun sourceFalseSkips() = assertFalse(HimTestExecutionBoundaryV1.sourceIntegrationEnabled("false"))
    @Test fun sourceUppercaseTrueSkips() = assertFalse(HimTestExecutionBoundaryV1.sourceIntegrationEnabled("TRUE"))
    @Test fun sourceExactTrueEnables() = assertTrue(HimTestExecutionBoundaryV1.sourceIntegrationEnabled("true"))

    @Test fun paidMissingSkips() = assertFalse(HimTestExecutionBoundaryV1.paidNetworkEnabled(null, null))
    @Test fun paidMissingConfirmationSkips() = assertFalse(HimTestExecutionBoundaryV1.paidNetworkEnabled("true", null))
    @Test fun paidWrongConfirmationSkips() = assertFalse(HimTestExecutionBoundaryV1.paidNetworkEnabled("true", "AUTHORIZED_PAID_INFERENCE_WRONG"))
    @Test fun paidPropertyVariantsSkip() {
        assertFalse(HimTestExecutionBoundaryV1.paidNetworkEnabled("TRUE", HimTestExecutionBoundaryV1.PAID_NETWORK_CONFIRMATION))
        assertFalse(HimTestExecutionBoundaryV1.paidNetworkEnabled(" true", HimTestExecutionBoundaryV1.PAID_NETWORK_CONFIRMATION))
    }
    @Test fun paidExactPairEnables() = assertTrue(HimTestExecutionBoundaryV1.paidNetworkEnabled("true", HimTestExecutionBoundaryV1.PAID_NETWORK_CONFIRMATION))
    @Test fun sourceDoesNotEnablePaid() = assertFalse(HimTestExecutionBoundaryV1.paidNetworkEnabled(null, HimTestExecutionBoundaryV1.PAID_NETWORK_CONFIRMATION))
    @Test fun apiKeyIsIrrelevantToPureDecision() {
        assertFalse(HimTestExecutionBoundaryV1.sourceIntegrationEnabled(null))
        assertFalse(HimTestExecutionBoundaryV1.paidNetworkEnabled(null, null))
    }
}
