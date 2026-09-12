package de.shopme.testing.system.tools.knowledge.him.training.p2

import de.shopme.tools.knowledge.him.training.p2.HimP2ProductiveTrainingConfigurationV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunHimP2ProductiveTrainingConfigurationV1Test {
    @Test
    fun currentAuthorityBindsFrozenP2PartitionAndTrajectory() {
        val fields = HimP2ProductiveTrainingConfigurationV1.current().fields
        assertEquals("26", fields["trainExampleCount"])
        assertEquals("6", fields["validationExampleCount"])
        assertEquals("0", fields["holdoutExampleCount"])
        assertEquals("9", fields["secondaryOnlyTrainCount"])
        assertEquals("17", fields["trainCompatibleCount"])
        assertEquals("17", fields["trainPrimaryActiveCount"])
        assertEquals("3", fields["epochs"])
        assertEquals("8", fields["microBatchSize"])
        assertEquals("1", fields["gradientAccumulationSteps"])
        assertEquals("12", fields["expectedOptimizerStepCount"])
    }

    @Test
    fun numericalAuthorityIsExplicitAndComplete() {
        val fields = HimP2ProductiveTrainingConfigurationV1.current().fields
        assertEquals("0.0001", fields["learningRate"])
        assertEquals("0.01", fields["weightDecay"])
        assertEquals("7", fields["seed"])
        assertEquals("FP32", fields["precision"])
        assertTrue(fields["trainingConfigurationRationale"]!!.contains("Explicit P2 pilot decision"))
    }

    @Test
    fun emptyHoldoutAndValidationRejectLimitationAreBound() {
        val fields = HimP2ProductiveTrainingConfigurationV1.current().fields
        assertEquals("EMPTY_BY_PARTITION", fields["holdoutEvidenceState"])
        assertEquals("0", fields["validationRejectCount"])
        assertEquals("NO", fields["validationRejectGeneralizationMeasurable"])
        assertEquals("0", fields["expectedHoldoutForwardCount"])
    }

    @Test
    fun serializationRoundTripPreservesIdentity() {
        val authority = HimP2ProductiveTrainingConfigurationV1.current()
        val decoded = HimP2ProductiveTrainingConfigurationV1.fromJson(authority.serialize())
        assertEquals(authority, decoded)
    }

    @Test
    fun identityIsDeterministic() {
        assertEquals(
            HimP2ProductiveTrainingConfigurationV1.current(),
            HimP2ProductiveTrainingConfigurationV1.current(),
        )
    }
}
