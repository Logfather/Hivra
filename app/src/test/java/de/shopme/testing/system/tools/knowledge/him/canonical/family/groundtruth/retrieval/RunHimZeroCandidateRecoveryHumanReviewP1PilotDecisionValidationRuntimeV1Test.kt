package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceStatusV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeV1Test {
    @Test
    fun runtimeIdentityAndStateAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_RUNTIME_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeV1.VERSION)
        assertEquals("INDEPENDENT_DECISION_VALIDATION_CONTEXT_ONLY", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeV1.STATE)
    }

    @Test
    fun requestFieldModelIsExactlyFrozen() {
        val fields = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeRequestV1::class.java.declaredFields
            .filterNot { it.isSynthetic || it.name.startsWith("$") }
            .map { it.name }
            .toSet()
        assertEquals(
            setOf(
                "enabled", "validationBatchId", "inputBinding", "originalDecisionBatch",
                "validatorReviewerRef", "validationRound", "validationRevision", "submittedValidationRecords",
                "durableRoot", "reportRoot",
            ),
            fields,
        )
    }

    @Test
    fun resultIsClosedToDisabledCompletedAndFailed() {
        assertEquals(
            setOf("Disabled", "Completed", "Failed"),
            setOf(
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1.Disabled::class.simpleName,
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1.Completed::class.simpleName,
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1.Failed::class.simpleName,
            ),
        )
    }

    @Test
    fun disabledReturnsBeforeValidationAndFileIo() {
        val result = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeV1.execute(
            request(enabled = false, durable = Path.of("/path/not-used"), reports = Path.of("/path/not-used")),
        )
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1.Disabled>(result)
    }

    @Test
    fun validInputMaterializesAndPersistsOneCompleteBatch() = withRoots { durable, reports ->
        val result = completed(execute(request(durable = durable, reports = reports)))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeV1.CONTRACT_ID, result.runtimeContractId)
        assertEquals("1", result.runtimeVersion)
        assertEquals("validation-batch-1", result.validationBatchId)
        assertEquals("INDEPENDENT_DECISION_VALIDATION_CONTEXT_ONLY", result.state)
        assertEquals(4, result.validationBatch.records.size)
        assertEquals(result.validationBatch.counters, result.counters)
        assertEquals(result.validationBatch.inputBinding.bindingDigest, result.inputBindingDigest)
        assertTrue(result.durableRelativePath.startsWith("validation-batch-1/"))
        assertFalse(result.durableRelativePath.startsWith('/'))
        assertTrue(result.durableJsonSha256.matches(Regex("[0-9a-f]{64}")))
        assertTrue(result.reportJsonSha256.matches(Regex("[0-9a-f]{64}")))
        assertTrue(result.reportTextSha256.matches(Regex("[0-9a-f]{64}")))
        assertEquals(
            result.validationBatch,
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.readValidationBatch(
                durable.resolve(result.durableRelativePath).toFile(),
            ),
        )
    }

    @Test
    fun validationRecordsRemainInFrozenOrderAndRoutesAreDerived() = withRoots { durable, reports ->
        val result = completed(execute(request(durable = durable, reports = reports)))
        assertEquals(
            inputBinding().originalSelections.map { it.reviewUnitId },
            result.validationBatch.records.map { it.originalDecision.reviewUnitId },
        )
        assertEquals(
            listOf(
                "POTENTIAL_POSITIVE_GOLD_CANDIDATE",
                "POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE",
                "POTENTIAL_POSITIVE_GOLD_CANDIDATE",
                "POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE",
            ),
            result.validationBatch.records.map { it.downstreamRoute.name },
        )
        assertEquals(2, result.counters.potentialPositiveGoldCandidates)
        assertEquals(2, result.counters.potentialNegativeSupervisionCandidates)
    }

    @Test
    fun originalDecisionBatchMustBeTheCommittedValidatedSubmission() = withRoots { durable, reports ->
        val original = originalDecisionBatch()
        val result = execute(
            request(
                durable = durable,
                reports = reports,
                originalBatch = original.copy(selections = original.selections.reversed()),
            ),
        )
        assertFailure(result, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1.INVALID_ORIGINAL_DECISION_BATCH)
    }

    @Test
    fun inputBindingMustBeFrozen() = withRoots { durable, reports ->
        val binding = inputBinding().copy(originalBatchLogicalDigest = "0".repeat(64))
        assertFailure(
            execute(request(durable = durable, reports = reports, binding = binding)),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1.INVALID_INPUT_BINDING,
        )
    }

    @Test
    fun validatorMustBeIndependentAndValid() = withRoots { durable, reports ->
        assertFailure(
            execute(request(durable = durable, reports = reports, validator = inputBinding().originalReviewerRef)),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1.VALIDATOR_NOT_INDEPENDENT,
        )
        assertFailure(
            execute(request(durable = durable, reports = reports, validator = "")),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1.INVALID_VALIDATOR_REVIEWER,
        )
    }

    @Test
    fun validationRoundAndRevisionAreExactlyOne() = withRoots { durable, reports ->
        assertFailure(
            execute(request(durable = durable, reports = reports, round = 2)),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1.INVALID_VALIDATION_ROUND,
        )
        assertFailure(
            execute(request(durable = durable, reports = reports, revision = 2)),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1.INVALID_VALIDATION_REVISION,
        )
    }

    @Test
    fun exactlyFourRecordsAndFrozenOrderAreRequired() = withRoots { durable, reports ->
        val records = validationRecords()
        assertFailure(
            execute(request(durable = durable, reports = reports, records = records.dropLast(1))),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1.INVALID_RECORD_COUNT,
        )
        assertFailure(
            execute(request(durable = durable, reports = reports, records = records.reversed())),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1.INVALID_RECORD_ORDER,
        )
    }

    @Test
    fun recordsRemainBoundToOriginalDecisionIdentity() = withRoots { durable, reports ->
        val bad = validationRecords().toMutableList()
        bad[0] = bad[0].copy(
            originalDecision = bad[0].originalDecision.copy(canonicalEntityId = "foreign"),
        )
        assertFailure(
            execute(request(durable = durable, reports = reports, records = bad)),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1.ORIGINAL_DECISION_MISMATCH,
        )
    }

    @Test
    fun recordIdsRemainContractDerived() = withRoots { durable, reports ->
        val bad = validationRecords().toMutableList()
        bad[0] = bad[0].copy(validationRecordId = "0".repeat(64))
        assertFailure(
            execute(request(durable = durable, reports = reports, records = bad)),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1.VALIDATION_RECORD_BINDING_MISMATCH,
        )
    }

    @Test
    fun contractRejectsChangedAssessmentEvidenceAndRationale() = withRoots { durable, reports ->
        val bad = validationRecords().toMutableList()
        bad[0] = bad[0].copy(
            assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ABSTAIN_VALIDATION_INSUFFICIENT_EVIDENCE,
        )
        assertFailure(
            execute(request(durable = durable, reports = reports, records = bad)),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1.INVALID_VALIDATION_RECORD,
        )
    }

    @Test
    fun countersAreDerivedExclusivelyFromMaterializedRecords() = withRoots { durable, reports ->
        val result = completed(execute(request(durable = durable, reports = reports)))
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.deriveCounters(result.validationBatch.records),
            result.validationBatch.counters,
        )
        assertEquals(12, result.counters.referencedEvidenceCount)
        assertEquals(12, result.counters.distinctReferencedEvidenceCount)
        assertEquals(4, result.counters.distinctReviewUnits)
        assertEquals(4, result.counters.distinctOriginalDecisionIdentities)
    }

    @Test
    fun firstAndSecondExecutionAreReloadedAndIdempotent() = withRoots { durable, reports ->
        val request = request(durable = durable, reports = reports)
        val first = completed(execute(request))
        val before = snapshot(durable, reports)
        val second = completed(execute(request))
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceStatusV1.CREATED,
            first.persistenceStatus,
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL,
            second.persistenceStatus,
        )
        assertEquals(before, snapshot(durable, reports))
        assertEquals(first.durableJsonSha256, second.durableJsonSha256)
        assertEquals(first.validationBatch, second.validationBatch)
    }

    @Test
    fun persistencePartialStateIsMappedWithoutRepair() = withRoots { durable, reports ->
        val partial = durable.resolve("validation-batch-1/decision-validations.v1.json")
        Files.createDirectories(partial.parent)
        Files.write(partial, byteArrayOf(1))
        assertFailure(
            execute(request(durable = durable, reports = reports)),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1.PERSISTENCE_FAILED,
        )
        assertTrue(Files.exists(partial))
    }

    @Test
    fun malformedBatchIdFailsBeforePersistence() = withRoots { durable, reports ->
        assertFailure(
            execute(request(durable = durable, reports = reports, batchId = "../unsafe")),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1.INVALID_VALIDATION_BATCH_ID,
        )
    }

    @Test
    fun failedResultsExposeOnlyTypedSafeContext() = withRoots { durable, reports ->
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1.Failed>(
            execute(request(durable = durable, reports = reports, validator = "")),
        )
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1.INVALID_VALIDATOR_REVIEWER, failure.reason)
        assertEquals("validator", failure.safeContext)
        assertFalse(failure.safeContext.contains('/'))
        assertFalse(failure.safeContext.contains("exception", ignoreCase = true))
    }

    @Test
    fun runtimeModelsContainNoOperationalOrPromotionFields() {
        val forbidden = setOf("clock", "timestamp", "random", "uuid", "network", "search", "fetch", "scan", "gold", "training", "publication", "authorityMutation")
        val classes = listOf(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeRequestV1::class.java,
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1.Completed::class.java,
        )
        val fields = classes.flatMap { type -> type.declaredFields.map { it.name.lowercase() } }
        assertTrue(forbidden.none { forbiddenName -> fields.any { it.contains(forbiddenName) } })
    }

    @Test
    fun runtimeUsesOnlyTheCommittedValidationAndPersistenceContracts() {
        val names = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeV1::class.java.declaredMethods
            .map { it.name.lowercase() }
            .joinToString(" ")
        assertFalse(names.contains("search"))
        assertFalse(names.contains("fetch"))
        assertFalse(names.contains("sqlite"))
    }

    private fun request(
        enabled: Boolean = true,
        batchId: String = "validation-batch-1",
        binding: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationInputBindingV1 = inputBinding(),
        originalBatch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1 = originalDecisionBatch(),
        validator: String = "reviewer:independent:v1",
        round: Int = 1,
        revision: Int = 1,
        records: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1> = validationRecords(),
        durable: Path = Path.of("/private/tmp/him-validation-runtime-durable-not-used"),
        reports: Path = Path.of("/private/tmp/him-validation-runtime-reports-not-used"),
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeRequestV1(
        enabled,
        batchId,
        binding,
        originalBatch,
        validator,
        round,
        revision,
        records,
        durable.toFile(),
        reports.toFile(),
    )

    private fun execute(request: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeRequestV1) =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeV1.execute(request)

    private fun completed(result: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1) =
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1.Completed>(result)

    private fun assertFailure(
        result: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1,
        reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1,
    ) {
        assertEquals(reason, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1.Failed>(result).reason)
    }

    private fun inputBinding() = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING

    private fun originalDecisionBatch() = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.create()

    private fun validationRecords(): List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1> =
        inputBinding().originalSelections.mapIndexed { index, selection ->
            val confirm = selection.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.createValidationRecord(
                inputBinding = inputBinding(),
                originalDecision = selection,
                validatorReviewerRef = "reviewer:independent:v1",
                validationRound = 1,
                validationRevision = 1,
                assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
                reasonCodes = if (confirm) {
                    listOf(
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_SUPPORTS_ORIGINAL_DECISION,
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.ORIGINAL_REASON_CODES_SUPPORTED,
                    )
                } else {
                    listOf(
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_CONTRADICTS_ORIGINAL_DECISION,
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.ORIGINAL_REASON_CODES_SUPPORTED,
                    )
                },
                evidenceReferenceIds = selection.evidenceReferenceIds,
                rationale = "Independent validation record ${index + 1}",
            )
        }

    private fun withRoots(block: (durable: Path, reports: Path) -> Unit) {
        val root = Files.createTempDirectory("him-validation-runtime-test")
        try {
            block(root.resolve("durable"), root.resolve("reports"))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun snapshot(vararg roots: Path): Map<String, List<Byte>> = roots
        .flatMap { root ->
            if (!Files.exists(root)) emptyList() else Files.walk(root).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .map { file -> file.toString() to Files.readAllBytes(file).toList() }
                    .toList()
            }
        }
        .toMap()
}
