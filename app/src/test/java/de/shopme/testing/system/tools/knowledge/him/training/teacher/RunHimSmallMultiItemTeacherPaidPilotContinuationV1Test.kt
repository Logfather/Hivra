package de.shopme.testing.system.tools.knowledge.him.training.teacher

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceOrigin
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceProvenance
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceRuntime
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceSchema
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceSuccess
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInformationGainJudgment
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthGenerationPersistenceV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthResultIdentityV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthOutputValidatorV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthProviderOutcomeV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthProviderV1
import de.shopme.tools.knowledge.him.training.teacher.HimSemanticInferenceTeacherProviderAdapterV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class RunHimSmallMultiItemTeacherPaidPilotContinuationV1Test {
    @Test
    fun `accepts exact recovery state and executes only item two and three offline`() {
        val root = projectRoot()
        val runner = RunHimSmallMultiItemTeacherGroundTruthPaidPilotV1Test()
        val missionFile = root.resolve(MISSION_PATH)
        val priorReportFile = root.resolve(PRIOR_REPORT_PATH)
        val missionBefore = missionFile.readBytes()
        val priorReportBefore = priorReportFile.readBytes()
        val protectedBefore = runner.protectedArtifactSnapshotForTest(root)
        val continuationMarker = Files.createTempFile("him-f3-8g4-continuation-marker", ".txt").toFile().also { it.delete() }
        val state = runner.resolveContinuationState(root, continuationMarker)

        assertExistingPaidEvidenceIsReadableAndValid(root, state)
        assertEquals(listOf("Makrelen", "Rindergulasch"), state.continuationItems.map { it.rawInput })
        assertEquals("Salbei", state.item1.rawInput)
        assertEquals(state.item1Result, HimTeacherGroundTruthGenerationPersistenceV1.readResult(root.resolve(state.item1.resultPath)))

        val calls = mutableListOf<String>()
        val runtime = HimSemanticInferenceRuntime { request ->
            calls += request.inputTerm
            HimSemanticInferenceResult.Success(
                HimSemanticInferenceSuccess(
                    candidates = listOf(
                        de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticCandidateProposal(
                            proposalReference = "offline-${request.inputTerm}",
                            candidateTerm = request.inputTerm,
                            relation = HimCandidateRelation.CreateNewCanonical,
                            confidence = HimCandidateConfidence.HIGH,
                            evidenceOrigin = HimSemanticEvidenceOrigin.MODEL_DERIVED,
                            evidenceAssessments = emptyList(),
                            shortRationale = "offline continuation fixture",
                        ),
                    ),
                    informationGain = HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN,
                    retrievalDirective = null,
                    authorityConflicts = emptyList(),
                    provenance = provenance(request, state),
                ),
            )
        }
        val provider = HimSemanticInferenceTeacherProviderAdapterV1(runtime)
        val outputDirectory = Files.createTempDirectory("him-f3-8g4-continuation-results").toFile()
        val completed = runner.executeContinuationItemsForTest(state, outputDirectory, provider) { calls.size }

        assertEquals(listOf("Makrelen", "Rindergulasch"), calls)
        assertEquals(2, completed.size)
        assertEquals(
            listOf(
                "him-f3-8g4-item-01a250090599.result.v1.json",
                "him-f3-8g4-item-03c60a01f03b.result.v1.json",
            ),
            completed.map { it.item.resultPath.substringAfterLast('/') },
        )
        completed.forEach { item ->
            val resultFile = outputDirectory.resolve(item.item.resultPath)
            val rootLevelResult = outputDirectory.resolve(item.item.resultPath.substringAfterLast('/'))
            assertTrue(resultFile.isFile)
            assertFalse(rootLevelResult.isFile)
            val reloaded = HimTeacherGroundTruthGenerationPersistenceV1.readResult(resultFile)
            assertEquals(item.result, reloaded)
            assertEquals(HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN, reloaded.output.informationGain)
            assertEquals(1, reloaded.output.proposals.size)
            HimTeacherGroundTruthOutputValidatorV1.validate(item.item.request, reloaded.output)
        }

        val continuationReport = continuationMarker.apply {
            writeText(runner.continuationSuccessReportForTest(state, completed, calls.size, protectedBefore == runner.protectedArtifactSnapshotForTest(root)))
        }
        val continuationReportBytes = continuationReport.readBytes()
        val continuationText = continuationReport.readText()
        assertTrue(continuationText.contains("PREVIOUS_OPENAI_CALLS=3"))
        assertTrue(continuationText.contains("CONTINUATION_OPENAI_CALLS=2"))
        assertTrue(continuationText.contains("CUMULATIVE_OPENAI_CALLS=5"))
        assertTrue(continuationText.contains("ITEM_1_REUSED=true"))
        assertTrue(continuationText.contains("ITEM_3_EXECUTED=true"))
        assertTrue(missionBefore.contentEquals(missionFile.readBytes()))
        assertTrue(priorReportBefore.contentEquals(priorReportFile.readBytes()))
        assertEquals(protectedBefore, runner.protectedArtifactSnapshotForTest(root))

        var secondAttemptProviderCalls = 0
        assertFails {
            runner.resolveContinuationState(root, continuationMarker)
            secondAttemptProviderCalls++
        }
        assertEquals(0, secondAttemptProviderCalls)
        assertTrue(continuationReportBytes.contentEquals(continuationMarker.readBytes()))
        assertEquals(listOf("Makrelen", "Rindergulasch"), calls)
    }

    @Test
    fun `stops before item three when item two fails offline`() {
        val root = projectRoot()
        val runner = RunHimSmallMultiItemTeacherGroundTruthPaidPilotV1Test()
        val missionFile = root.resolve(MISSION_PATH)
        val priorReportFile = root.resolve(PRIOR_REPORT_PATH)
        val missionBefore = missionFile.readBytes()
        val priorReportBefore = priorReportFile.readBytes()
        val protectedBefore = runner.protectedArtifactSnapshotForTest(root)
        val continuationMarker = Files.createTempFile("him-f3-8g4-continuation-failure-marker", ".txt").toFile().also { it.delete() }
        val state = runner.resolveContinuationState(root, continuationMarker)
        assertExistingPaidEvidenceIsReadableAndValid(root, state)
        val calls = mutableListOf<String>()
        val provider = HimTeacherGroundTruthProviderV1 { request ->
            calls += request.observedTerm
            HimTeacherGroundTruthProviderOutcomeV1.TechnicalFailure("offline Item-2 failure")
        }
        val outputDirectory = Files.createTempDirectory("him-f3-8g4-continuation-failure-results").toFile()

        assertFails { runner.executeContinuationItemsForTest(state, outputDirectory, provider) { calls.size } }
        assertEquals(listOf("Makrelen"), calls)
        assertTrue(outputDirectory.listFiles().orEmpty().isEmpty())
        assertFalse(outputDirectory.resolve(state.continuationItems[0].resultPath).exists())
        assertFalse(outputDirectory.resolve(state.continuationItems[1].resultPath).exists())
        assertFalse(outputDirectory.resolve(state.continuationItems[0].resultPath.substringAfterLast('/')).exists())
        assertFalse(outputDirectory.resolve(state.continuationItems[1].resultPath.substringAfterLast('/')).exists())
        assertTrue(missionBefore.contentEquals(missionFile.readBytes()))
        assertTrue(priorReportBefore.contentEquals(priorReportFile.readBytes()))
        assertEquals(protectedBefore, runner.protectedArtifactSnapshotForTest(root))
    }

    private fun assertExistingPaidEvidenceIsReadableAndValid(
        root: File,
        state: HimTeacherPaidPilotContinuationStateV1,
    ) {
        state.continuationItems.forEach { item ->
            val misplacedFile = root.resolve(item.resultPath.substringAfterLast('/'))
            val destinationFile = root.resolve(item.resultPath)
            assertTrue(misplacedFile.isFile)
            assertFalse(destinationFile.exists())
            val reloaded = HimTeacherGroundTruthGenerationPersistenceV1.readResult(misplacedFile)
            assertEquals(reloaded.resultReference, HimTeacherGroundTruthResultIdentityV1.reference(reloaded))
            assertEquals(reloaded.logicalDigest, HimTeacherGroundTruthResultIdentityV1.digest(reloaded))
            HimTeacherGroundTruthOutputValidatorV1.validate(item.request, reloaded.output)
        }
    }

    private fun provenance(
        request: de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceRequest,
        state: HimTeacherPaidPilotContinuationStateV1,
    ) = HimSemanticInferenceProvenance(
        "OFFLINE_CONTINUATION_FAKE",
        "DETERMINISTIC",
        state.continuationItems.first().request.policyBindings.providerConfigurationFingerprint,
        HimSemanticInferenceSchema.OUTPUT_VERSION,
        state.continuationItems.first().request.policyBindings.instructionPolicyVersion,
        request.retrievalFoundation,
        1,
    )

    private fun assertFails(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected continuation failure")
        } catch (_: AssertionError) {
            throw AssertionError("Expected continuation failure")
        } catch (_: Throwable) {
            // expected
        }
    }

    private fun projectRoot(): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (!current.resolve("settings.gradle.kts").isFile && !current.resolve("settings.gradle").isFile) current = requireNotNull(current.parentFile)
        return current
    }

    companion object {
        private const val MISSION_PATH = "build/knowledge/reports/him/training/him-f3-8g4-small-multi-item-teacher-paid-pilot.selection.v1.json"
        private const val PRIOR_REPORT_PATH = "build/knowledge/reports/him/training/him-f3-8g4-small-multi-item-teacher-paid-pilot.txt"
    }
}
