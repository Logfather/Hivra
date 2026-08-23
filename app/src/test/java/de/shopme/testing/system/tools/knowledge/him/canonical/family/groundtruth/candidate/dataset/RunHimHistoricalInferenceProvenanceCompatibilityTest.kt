package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateConfidenceDiagnostics
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDataset
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetContractV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetPersistenceV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateFinalInferenceOutcome
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateGenerationInputRun
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateGenerationRun
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateGenerationRunState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInferenceProvenance
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInputCompletionState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInputProvenance
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimRetrievalFoundationBinding
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceSchema
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInformationGainJudgment
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class RunHimHistoricalInferenceProvenanceCompatibilityTest {

    @Test
    fun `historical v2_1 and current v2_2 inference provenance coexist and digest deterministically`() {
        val historicalInference = historicalV21Inference()
        val currentInference = currentV22Inference()

        assertEquals(
            HISTORICAL_POLICY,
            historicalInference.instructionPolicy,
        )
        assertEquals(
            HISTORICAL_PROVIDER_FINGERPRINT,
            historicalInference.providerConfigurationFingerprint.value,
        )

        assertEquals(
            HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
            currentInference.instructionPolicy,
        )
        assertEquals(
            HimCandidateDatasetContractV2.PROVIDER_CONFIGURATION_FINGERPRINT,
            currentInference.providerConfigurationFingerprint,
        )

        assertNotEquals(
            historicalInference.instructionPolicy,
            currentInference.instructionPolicy,
        )
        assertNotEquals(
            historicalInference.providerConfigurationFingerprint,
            currentInference.providerConfigurationFingerprint,
        )

        val historicalRun = emptyCandidateRun(
            mission = "HISTORICAL-PROVENANCE-V2_1",
            rawInput = "Historical input",
            normalizedInput = "historical input",
            inputSetIdentity = HimSha256("1".repeat(64)),
            inference = historicalInference,
        )

        val currentRun = emptyCandidateRun(
            mission = "CURRENT-PROVENANCE-V2_2",
            rawInput = "Current input",
            normalizedInput = "current input",
            inputSetIdentity = HimSha256("2".repeat(64)),
            inference = currentInference,
        )

        val afterHistorical = HimCandidateDatasetPersistenceV2.addRun(
            HimCandidateDataset(),
            historicalRun,
        )

        val combined = HimCandidateDatasetPersistenceV2.addRun(
            afterHistorical,
            currentRun,
        )

        assertEquals(2, combined.runs.size)
        assertTrue(combined.candidates.isEmpty())

        val historicalPersistedInference = requireNotNull(
            combined.runs
                .single { it.generationMissionReference == "HISTORICAL-PROVENANCE-V2_1" }
                .inputRuns
                .single()
                .inference
        )

        val currentPersistedInference = requireNotNull(
            combined.runs
                .single { it.generationMissionReference == "CURRENT-PROVENANCE-V2_2" }
                .inputRuns
                .single()
                .inference
        )

        assertEquals(
            HISTORICAL_POLICY,
            historicalPersistedInference.instructionPolicy,
        )
        assertEquals(
            HISTORICAL_PROVIDER_FINGERPRINT,
            historicalPersistedInference.providerConfigurationFingerprint.value,
        )

        assertEquals(
            HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
            currentPersistedInference.instructionPolicy,
        )
        assertEquals(
            HimCandidateDatasetContractV2.PROVIDER_CONFIGURATION_FINGERPRINT,
            currentPersistedInference.providerConfigurationFingerprint,
        )

        val firstDigest = HimCandidateIdentityV1.datasetDigest(combined)
        val secondDigest = HimCandidateIdentityV1.datasetDigest(combined)

        assertEquals(
            firstDigest,
            secondDigest,
        )

        /*
         * Usage must not influence the logical dataset digest.
         */
        val combinedWithDifferentUsage = combined.copy(
            runs = combined.runs.map { run ->
                run.copy(
                    inputRuns = run.inputRuns.map { inputRun ->
                        inputRun.copy(
                            inference = inputRun.inference?.copy(
                                usage = HimSemanticUsage(
                                    inputTokens = 999_999,
                                    outputTokens = 88_888,
                                    cachedInputTokens = 77_777,
                                )
                            )
                        )
                    }
                )
            }
        )

        assertEquals(
            firstDigest,
            HimCandidateIdentityV1.datasetDigest(combinedWithDifferentUsage),
        )

        val temporaryDirectory = Files.createTempDirectory(
            "him-historical-provenance-compatibility-"
        ).toFile()

        try {
            val datasetFile = temporaryDirectory.resolve("candidate-dataset.v2.json")

            HimCandidateDatasetPersistenceV2.writeMaster(
                datasetFile,
                combined,
            )

            val reloaded = HimCandidateDatasetPersistenceV2.readDataset(
                datasetFile,
            )

            assertEquals(
                combined,
                reloaded,
            )

            assertEquals(
                firstDigest,
                HimCandidateIdentityV1.datasetDigest(reloaded),
            )

            val reloadedHistorical = requireNotNull(
                reloaded.runs
                    .single {
                        it.generationMissionReference ==
                                "HISTORICAL-PROVENANCE-V2_1"
                    }
                    .inputRuns
                    .single()
                    .inference
            )

            val reloadedCurrent = requireNotNull(
                reloaded.runs
                    .single {
                        it.generationMissionReference ==
                                "CURRENT-PROVENANCE-V2_2"
                    }
                    .inputRuns
                    .single()
                    .inference
            )

            assertEquals(
                HISTORICAL_POLICY,
                reloadedHistorical.instructionPolicy,
            )
            assertEquals(
                HISTORICAL_PROVIDER_FINGERPRINT,
                reloadedHistorical.providerConfigurationFingerprint.value,
            )

            assertEquals(
                HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
                reloadedCurrent.instructionPolicy,
            )
            assertEquals(
                HimCandidateDatasetContractV2.PROVIDER_CONFIGURATION_FINGERPRINT,
                reloadedCurrent.providerConfigurationFingerprint,
            )
        } finally {
            temporaryDirectory.deleteRecursively()
        }
    }

    private fun historicalV21Inference() =
        HimCandidateInferenceProvenance(
            provider = "OPENAI",
            model = "gpt-5.6-sol",
            providerConfigurationFingerprint =
                HimSha256(HISTORICAL_PROVIDER_FINGERPRINT),
            inferenceSchema =
                "HIM_SEMANTIC_INFERENCE_OUTPUT_V2_1",
            instructionPolicy =
                HISTORICAL_POLICY,
            contextBudgetPolicy =
                HimCandidateDatasetContractV2.CONTEXT_BUDGET_POLICY,
            retrievalFoundationRelease =
                HimRetrievalFoundationBinding.RELEASE_VERSION,
            retrievalFoundationReleaseSha256 =
                HimRetrievalFoundationBinding.RELEASE_SHA256,
            retrievalFoundationDigest =
                HimRetrievalFoundationBinding.FOUNDATION_DIGEST,
            technicalAttemptCount = 1,
            usage = HimSemanticUsage(
                inputTokens = 100,
                outputTokens = 20,
                cachedInputTokens = 10,
            ),
        )

    private fun currentV22Inference() =
        HimCandidateInferenceProvenance(
            provider =
                HimCandidateDatasetContractV2.PROVIDER,
            model =
                HimCandidateDatasetContractV2.MODEL,
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
            usage = HimSemanticUsage(
                inputTokens = 200,
                outputTokens = 40,
                cachedInputTokens = 20,
            ),
        )

    private fun emptyCandidateRun(
        mission: String,
        rawInput: String,
        normalizedInput: String,
        inputSetIdentity: HimSha256,
        inference: HimCandidateInferenceProvenance,
    ): HimCandidateGenerationRun {
        val runReference = HimCandidateIdentityV1.run(
            generationMissionReference = mission,
            inputSetIdentity = inputSetIdentity,
            inference = inference,
        )

        val input = HimCandidateInputProvenance(
            rawInput = rawInput,
            normalizedLookup = normalizedInput,
        )

        val inputRunReference = HimCandidateIdentityV1.inputRun(
            run = runReference,
            input = input,
        )

        val inputRun = HimCandidateGenerationInputRun(
            inputRunReference = inputRunReference,
            input = input,
            canonicalContext = emptyList(),
            retrievalHistory = emptyList(),
            finalInformationGainJudgment =
                HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN,
            terminalState = null,
            completionState =
                HimCandidateInputCompletionState.COMPLETED,
            finalOutcome =
                HimCandidateFinalInferenceOutcome.SUCCESS_WITH_NO_PERSISTED_CANDIDATE,
            confidenceDiagnostics =
                HimCandidateConfidenceDiagnostics(
                    high = 0,
                    medium = 0,
                    low = 0,
                    noConfidence = 0,
                ),
            knownRelations = emptyList(),
            authorityConflicts = emptyList(),
            persistedCandidateReferences = emptyList(),
            inference = inference,
            technicalFailure = null,
        )

        return HimCandidateGenerationRun(
            runReference = runReference,
            generationMissionReference = mission,
            runInputSetIdentity = inputSetIdentity,
            state = HimCandidateGenerationRunState.COMPLETE,
            inputRuns = listOf(inputRun),
            occurrences = emptyList(),
        )
    }

    companion object {
        private const val HISTORICAL_POLICY =
            "HIM_SEMANTIC_INFERENCE_INSTRUCTION_POLICY_V2_1"

        private const val HISTORICAL_PROVIDER_FINGERPRINT =
            "5317efd22a62e1cf485a7e5c3c6627c9d53e419ff4a63bbe91bc38fa4a592003"
    }
}