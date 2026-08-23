package de.shopme.testing.system.tools.knowledge.him.training.teacher

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingWorkReasonV1
import de.shopme.tools.knowledge.him.training.scaling.HimTeacherGroundTruthWorkItemV1
import de.shopme.tools.knowledge.him.training.teacher.HimSmallMultiItemTeacherPilotSelectionV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RunHimSmallMultiItemTeacherPilotSelectionV1Test {
    @Test
    fun selectsExactlyThreeDeterministicExistingItemsOffline() {
        val excluded = workItem(0, HimTrainingPartitionV1.TRAIN, "Exc001")
        val trainFirst = workItem(1, HimTrainingPartitionV1.TRAIN, "Tra001")
        val trainFill = workItem(2, HimTrainingPartitionV1.TRAIN, "Tra002")
        val trainLast = workItem(3, HimTrainingPartitionV1.TRAIN, "Tra003")
        val validationFirst = workItem(4, HimTrainingPartitionV1.VALIDATION, "Val001")
        val input = listOf(trainLast, excluded, validationFirst, trainFill, trainFirst)

        val selected = HimSmallMultiItemTeacherPilotSelectionV1.select(
            input,
            setOf(excluded.reference),
        )
        val replay = HimSmallMultiItemTeacherPilotSelectionV1.select(
            input.reversed(),
            setOf(excluded.reference),
        )

        assertEquals(3, selected.size)
        assertEquals(listOf(trainFirst, validationFirst, trainFill), selected)
        assertEquals(selected, replay)
        assertEquals(listOf(trainFirst.reference, validationFirst.reference, trainFill.reference), selected.map { it.reference })
        assertFalse(selected.any { it.reference == excluded.reference })
    }

    private fun workItem(
        referenceValue: Int,
        partition: HimTrainingPartitionV1,
        canonicalId: String,
    ) = HimTeacherGroundTruthWorkItemV1(
        reference = "teacher-work:v1:${referenceValue.toString(16).padStart(64, '0')}",
        canonicalId = HimEntityId(canonicalId),
        partition = partition,
        missingCoverage = listOf(HimTrainingClassificationV1.VARIANT),
        reason = HimCanonicalGroundTruthScalingWorkReasonV1.MISSING_SEMANTIC_COVERAGE,
        groundTruthReleaseReference = RELEASE,
    )

    private companion object {
        val RELEASE = HimGroundTruthReleaseIdentityV1("release:v1:${"d".repeat(64)}")
    }
}
