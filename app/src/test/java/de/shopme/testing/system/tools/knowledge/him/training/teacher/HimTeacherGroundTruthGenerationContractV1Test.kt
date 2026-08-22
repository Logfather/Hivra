package de.shopme.testing.system.tools.knowledge.him.training.teacher

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceOrigin
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInformationGainJudgment
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthGenerationContractV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthOutputV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherSemanticProposalV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherSemanticRelationV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HimTeacherGroundTruthGenerationContractV1Test {
    @Test
    fun `accepts empty proposals when no additional information gain is expected`() {
        val output = HimTeacherGroundTruthOutputV1(
            schemaVersion = HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION,
            proposals = emptyList(),
            informationGain = HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN,
        )

        assertTrue(output.proposals.isEmpty())
    }

    @Test
    fun `accepts existing proposals when no additional information gain is expected`() {
        val proposal = HimTeacherSemanticProposalV1(
            proposalReference = "proposal-1",
            candidateTerm = "Makrelen",
            relation = HimTeacherSemanticRelationV1.NewCanonical("Makrelen"),
            confidence = HimCandidateConfidence.HIGH,
            evidenceOrigin = HimSemanticEvidenceOrigin.MODEL_DERIVED,
            evidenceAssessments = emptyList(),
            shortRationale = "offline contract fixture",
        )

        val output = HimTeacherGroundTruthOutputV1(
            schemaVersion = HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION,
            proposals = listOf(proposal),
            informationGain = HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN,
        )

        assertEquals(listOf(proposal), output.proposals)
    }
}
