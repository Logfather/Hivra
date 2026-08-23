package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateConfidenceDiagnostics
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDataset
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetContractV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetPersistenceV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateFinalInferenceOutcome
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateGenerationInputRun
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateGenerationRun
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateGenerationRunState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateHypothesis
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInferenceProvenance
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInputCompletionState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInputProvenance
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateOccurrence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimRetrievalFoundationBinding
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceOrigin
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceSchema
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInformationGainJudgment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunHimCandidateReferenceCrossRunCollisionDiagnosticTest {

    @Test
    fun `same semantic candidate reference merges across runs when only rationale differs`() {
        val relation = HimCandidateRelation.CreateNewCanonical
        val normalizedCandidateTerm = "diagnostic candidate"

        val candidateReference = HimCandidateIdentityV1.candidate(
            normalizedCandidateTerm = normalizedCandidateTerm,
            relation = relation,
        )

        val firstHypothesis = HimCandidateHypothesis(
            candidateReference = candidateReference,
            candidateTerm = "Diagnostic Candidate",
            normalizedCandidateTerm = normalizedCandidateTerm,
            relation = relation,
            confidence = HimCandidateConfidence.HIGH,
            evidenceOrigin = HimSemanticEvidenceOrigin.MODEL_DERIVED,
            shortRationale = "First run rationale.",
        )

        val secondHypothesis = firstHypothesis.copy(
            shortRationale = "Second run rationale.",
        )

        assertEquals(
            firstHypothesis.candidateReference,
            secondHypothesis.candidateReference,
        )
        assertEquals(
            firstHypothesis.normalizedCandidateTerm,
            secondHypothesis.normalizedCandidateTerm,
        )
        assertEquals(
            firstHypothesis.relation,
            secondHypothesis.relation,
        )

        assertNotEquals(
            firstHypothesis,
            secondHypothesis,
        )

        val firstRun = run(
            mission = "DIAGNOSTIC-CANDIDATE-COLLISION-RUN-A",
            hypothesis = firstHypothesis,
        )

        val secondRun = run(
            mission = "DIAGNOSTIC-CANDIDATE-COLLISION-RUN-B",
            hypothesis = secondHypothesis,
        )

        val afterFirstRun = HimCandidateDatasetPersistenceV2.addRun(
            HimCandidateDataset(),
            firstRun,
        )

        val afterSecondRun = HimCandidateDatasetPersistenceV2.addRun(
            afterFirstRun,
            secondRun,
        )

        assertEquals(2, afterSecondRun.runs.size)
        assertEquals(1, afterSecondRun.candidates.size)

        val master = afterSecondRun.candidates.single()

        assertEquals(
            candidateReference,
            master.candidate.candidateReference,
        )

        assertEquals(
            normalizedCandidateTerm,
            master.candidate.normalizedCandidateTerm,
        )

        assertEquals(
            relation,
            master.candidate.relation,
        )

        assertEquals(2, master.occurrences.size)

        assertEquals(
            setOf(
                "First run rationale.",
                "Second run rationale.",
            ),
            master.occurrences
                .map { it.candidate.shortRationale }
                .toSet(),
        )
    }

    @Test
    fun `same candidate reference still rejects different semantic identity`() {
        val candidateReference = HimCandidateIdentityV1.candidate(
            normalizedCandidateTerm = "diagnostic candidate",
            relation = HimCandidateRelation.CreateNewCanonical,
        )

        val firstHypothesis = HimCandidateHypothesis(
            candidateReference = candidateReference,
            candidateTerm = "Diagnostic Candidate",
            normalizedCandidateTerm = "diagnostic candidate",
            relation = HimCandidateRelation.CreateNewCanonical,
            confidence = HimCandidateConfidence.HIGH,
            evidenceOrigin = HimSemanticEvidenceOrigin.MODEL_DERIVED,
            shortRationale = "First semantic identity.",
        )

        val conflictingHypothesis = firstHypothesis.copy(
            normalizedCandidateTerm = "different diagnostic candidate",
        )

        val firstRun = run(
            mission = "DIAGNOSTIC-CANDIDATE-COLLISION-RUN-A",
            hypothesis = firstHypothesis,
        )

        val conflictingRun = run(
            mission = "DIAGNOSTIC-CANDIDATE-COLLISION-RUN-B",
            hypothesis = conflictingHypothesis,
        )

        val dataset = HimCandidateDatasetPersistenceV2.addRun(
            HimCandidateDataset(),
            firstRun,
        )

        val failure = runCatching {
            HimCandidateDatasetPersistenceV2.addRun(
                dataset,
                conflictingRun,
            )
        }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException,
        )

        assertEquals(
            "CandidateReference collision between different semantic identities.",
            failure?.message,
        )
    }

    private fun run(
        mission: String,
        hypothesis: HimCandidateHypothesis,
    ): HimCandidateGenerationRun {
        val inference = inference()

        val inputSetIdentity = HimSha256(
            when (mission) {
                "DIAGNOSTIC-CANDIDATE-COLLISION-RUN-A" ->
                    "a".repeat(64)

                "DIAGNOSTIC-CANDIDATE-COLLISION-RUN-B" ->
                    "b".repeat(64)

                else ->
                    error("Unexpected diagnostic mission: $mission")
            }
        )

        val runReference = HimCandidateIdentityV1.run(
            generationMissionReference = mission,
            inputSetIdentity = inputSetIdentity,
            inference = inference,
        )

        val input = HimCandidateInputProvenance(
            rawInput = "Diagnostic Candidate",
            normalizedLookup = "diagnostic candidate",
        )

        val inputRunReference = HimCandidateIdentityV1.inputRun(
            run = runReference,
            input = input,
        )

        val occurrence = HimCandidateOccurrence(
            occurrenceReference = HimCandidateIdentityV1.occurrence(
                candidate = hypothesis.candidateReference,
                run = runReference,
                inputRun = inputRunReference,
                evidence = emptyList(),
            ),
            inputRunReference = inputRunReference,
            candidate = hypothesis,
            evidence = emptyList(),
        )

        val inputRun = HimCandidateGenerationInputRun(
            inputRunReference = inputRunReference,
            input = input,
            canonicalContext = emptyList(),
            retrievalHistory = emptyList(),
            finalInformationGainJudgment =
                HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN,
            terminalState = null,
            completionState = HimCandidateInputCompletionState.COMPLETED,
            finalOutcome =
                HimCandidateFinalInferenceOutcome.SUCCESS_WITH_PERSISTED_CANDIDATE,
            confidenceDiagnostics = HimCandidateConfidenceDiagnostics(
                high = 1,
                medium = 0,
                low = 0,
                noConfidence = 0,
            ),
            knownRelations = emptyList(),
            authorityConflicts = emptyList(),
            persistedCandidateReferences = listOf(hypothesis.candidateReference),
            inference = inference,
            technicalFailure = null,
        )

        return HimCandidateGenerationRun(
            runReference = runReference,
            generationMissionReference = mission,
            runInputSetIdentity = inputSetIdentity,
            state = HimCandidateGenerationRunState.COMPLETE,
            inputRuns = listOf(inputRun),
            occurrences = listOf(occurrence),
        )
    }

    private fun inference() = HimCandidateInferenceProvenance(
        provider = HimCandidateDatasetContractV2.PROVIDER,
        model = HimCandidateDatasetContractV2.MODEL,
        providerConfigurationFingerprint =
            HimCandidateDatasetContractV2.PROVIDER_CONFIGURATION_FINGERPRINT,
        inferenceSchema =
            HimSemanticInferenceSchema.OUTPUT_VERSION,
        instructionPolicy =
            HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
        contextBudgetPolicy =
            HimCandidateDatasetContractV2.CONTEXT_BUDGET_POLICY,
        retrievalFoundationRelease =
            HimRetrievalFoundationBinding.RELEASE_VERSION,
        retrievalFoundationReleaseSha256 =
            HimRetrievalFoundationBinding.RELEASE_SHA256,
        retrievalFoundationDigest =
            HimRetrievalFoundationBinding.FOUNDATION_DIGEST,
        technicalAttemptCount = 1,
        usage = null,
    )
}