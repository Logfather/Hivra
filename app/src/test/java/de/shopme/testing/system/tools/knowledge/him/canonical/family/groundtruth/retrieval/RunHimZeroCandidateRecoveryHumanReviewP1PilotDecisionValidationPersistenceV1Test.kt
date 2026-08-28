package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationEvidenceBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceReportV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceStatusV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1Test {
    @Test
    fun persistenceIdentityStateAndPathsAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_PERSISTENCE_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.VERSION)
        assertEquals(
            "data/knowledge/him/canonical-family/human-review/zero-candidate-recovery/v1/decision-validation-batches",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.DURABLE_ROOT,
        )
        assertEquals(
            "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/v1/decision-validation-batches",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.REPORT_ROOT,
        )
        assertEquals("decision-validations.v1.json", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.DURABLE_FILE_NAME)
        assertEquals("persistence-report.v1.json", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.REPORT_JSON_FILE_NAME)
        assertEquals("persistence-report.v1.txt", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.REPORT_TEXT_FILE_NAME)
        assertEquals("INDEPENDENT_DECISION_VALIDATION_CONTEXT_ONLY", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.VALIDATION_STATE)
    }

    @Test
    fun requestHasExactlyTheFiveAllowedFields() {
        assertEquals(
            setOf("enabled", "durableRoot", "reportRoot", "validationBatchId", "validationBatch"),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceRequestV1::class.java.declaredFields
                .filterNot { it.isSynthetic || it.name.startsWith("$") }
                .map { it.name }
                .toSet(),
        )
    }

    @Test
    fun resultHasOnlyTypedClosedVariants() {
        assertEquals(
            setOf("Disabled", "Completed", "Failed"),
            setOf(
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Disabled::class.simpleName,
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Completed::class.simpleName,
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Failed::class.simpleName,
            ),
        )
        assertEquals(listOf("CREATED", "ALREADY_PRESENT_IDENTICAL"), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceStatusV1.entries.map { it.name })
    }

    @Test
    fun batchIdsAreStrictlyBounded() {
        val invalid = listOf("", " ", "A1", "a_b", "a/b", "a\\b", "a.b", "../a", "a/../b", "-a", "a".repeat(65), "http://a")
        withRoots { durable, reports ->
            invalid.forEach { id ->
                val result = execute(id, validBatch(), durable, reports)
                assertFailure(result, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceFailureReasonV1.INVALID_VALIDATION_BATCH_ID)
            }
            assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Completed>(
                execute("valid-batch-1", validBatch(), durable, reports),
            )
        }
    }

    @Test
    fun disabledReturnsBeforeValidationOrFileAccess() {
        val result = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.execute(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceRequestV1(
                enabled = false,
                durableRoot = File("/path/that/is/not/used"),
                reportRoot = File("/path/that/is/not/used"),
                validationBatchId = "INVALID ID",
                validationBatch = validBatch().copy(state = "invalid"),
            ),
        )
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Disabled>(result)
    }

    @Test
    fun jsonHasStableFieldOrderUtf8AndExactlyOneFinalLf() {
        val bytes = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.serializeValidationBatch(validBatch())
        assertEquals(0, bytes.count { it == 0xEF.toByte() })
        assertEquals('\n'.code.toByte(), bytes.last())
        assertFalse(bytes.dropLast(1).contains('\n'.code.toByte()))
        val text = bytes.toString(Charsets.UTF_8)
        assertTrue(text.startsWith("{\"contractId\""))
        assertTrue(text.endsWith("}\n"))
        assertFalse(text.contains("\\u"))
    }

    @Test
    fun unicodeRationaleRoundTripsWithoutChangingItsBytes() {
        val original = validRecords()[0]
        val unicode = original.copy(rationale = "Unabhängige Prüfung: crème brûlée — 日本語")
        val batch = batchReplacing(0, unicode)
        val bytes = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.serializeValidationBatch(batch)
        assertEquals(batch, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.deserializeValidationBatch(bytes))
        assertTrue(bytes.toString(Charsets.UTF_8).contains("日本語"))
    }

    @Test
    fun completeBatchRoundTripsThroughTypedReader() {
        val batch = validBatch()
        val bytes = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.serializeBatch(batch)
        val reloaded = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.deserializeBatch(bytes)
        assertEquals(batch, reloaded)
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1.Valid>(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.validate(reloaded),
        )
    }

    @Test
    fun unknownTopLevelAndNestedFieldsAndDuplicateKeysFailClosed() {
        val json = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.serializeBatch(validBatch()).toString(Charsets.UTF_8)
        assertDeserializeFailure(json.replace("{\"contractId\"", "{\"unknown\":1,\"contractId\""))
        assertDeserializeFailure(json.replace("\"batchId\"", "\"unknown\":1,\"batchId\""))
        assertDeserializeFailure(json.replace("\"state\":\"INDEPENDENT_DECISION_VALIDATION_CONTEXT_ONLY\"", "\"state\":\"INDEPENDENT_DECISION_VALIDATION_CONTEXT_ONLY\",\"state\":\"INDEPENDENT_DECISION_VALIDATION_CONTEXT_ONLY\""))
        assertDeserializeFailure(json.replaceFirst("VALIDATE_ORIGINAL_DECISION", "NOT_A_VALID_ASSESSMENT"))
    }

    @Test
    fun wrongContractStateAndDigestsFailClosed() {
        assertSerializationFailure(validBatch().copy(contractId = "wrong"))
        assertSerializationFailure(validBatch().copy(version = "2"))
        assertSerializationFailure(validBatch().copy(state = "PROMOTED"))
        assertSerializationFailure(validBatch().copy(bindingDigest = "0".repeat(64)))
        assertSerializationFailure(validBatch().copy(logicalDigest = "0".repeat(64)))
        assertSerializationFailure(validBatch().copy(counters = validBatch().counters.copy(totalValidationRecords = 0)))
    }

    @Test
    fun resultCarriesSafeRelativePathsAndDerivedMetadata() = withRoots { durable, reports ->
        val result = execute("validation-batch-1", validBatch(), durable, reports)
        val completed = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Completed>(result)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceStatusV1.CREATED, completed.status)
        assertEquals("validation-batch-1/decision-validations.v1.json", completed.durableRelativePath)
        assertFalse(completed.durableRelativePath.startsWith('/'))
        assertTrue(completed.durableJsonSha256.matches(Regex("[0-9a-f]{64}")))
        assertTrue(completed.reportJsonSha256.matches(Regex("[0-9a-f]{64}")))
        assertTrue(completed.reportTextSha256.matches(Regex("[0-9a-f]{64}")))
        assertEquals(validBatch().bindingDigest, completed.validationBatchBindingDigest)
        assertEquals(validBatch().logicalDigest, completed.validationBatchLogicalDigest)
    }

    @Test
    fun firstPersistenceCreatesExactlyThreeFilesAndReloadsReports() = withRoots { durable, reports ->
        val batch = validBatch()
        val result = execute("validation-batch-1", batch, durable, reports)
        val completed = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Completed>(result)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceStatusV1.CREATED, completed.status)
        val durableFile = durable.toPath().resolve("validation-batch-1/decision-validations.v1.json")
        val reportJson = reports.toPath().resolve("validation-batch-1/persistence-report.v1.json")
        val reportText = reports.toPath().resolve("validation-batch-1/persistence-report.v1.txt")
        assertEquals(setOf(durableFile, reportJson, reportText), allFiles(durable.toPath(), reports.toPath()))
        assertEquals(batch, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.readValidationBatch(durableFile.toFile()))
        val report = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.deserializeReport(Files.readAllBytes(reportJson))
        assertEquals(batch.logicalDigest, report.validationBatchLogicalDigest)
        assertTrue(reportTextBytes(report).contentEquals(Files.readAllBytes(reportText)))
        assertTrue(Files.readAllBytes(reportJson).toString(Charsets.UTF_8).contains("durableJsonSha256"))
    }

    @Test
    fun reportJsonAndTextAreDeterministicAndDoNotDuplicateEvidenceOrRationale() = withRoots { durable, reports ->
        execute("validation-batch-1", validBatch(), durable, reports)
        val json = Files.readAllBytes(reports.toPath().resolve("validation-batch-1/persistence-report.v1.json")).toString(Charsets.UTF_8)
        val text = Files.readAllBytes(reports.toPath().resolve("validation-batch-1/persistence-report.v1.txt")).toString(Charsets.UTF_8)
        assertFalse(json.contains("Independent evidence review"))
        assertFalse(json.contains("evidenceReferenceId"))
        assertTrue(text.contains("VALIDATION CONTEXT ONLY — NO GOLD, NEGATIVE SUPERVISION, TRAINING, PUBLICATION, OR AUTHORITY EFFECT"))
        assertTrue(text.endsWith("\n"))
        assertFalse(text.endsWith("\n\n"))
    }

    @Test
    fun secondIdenticalExecutionIsIdempotentAndByteIdentical() = withRoots { durable, reports ->
        val batch = validBatch()
        val request = request("validation-batch-1", batch, durable, reports)
        val first = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Completed>(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.execute(request),
        )
        val before = snapshot(durable.toPath(), reports.toPath())
        val second = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Completed>(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.execute(request),
        )
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceStatusV1.CREATED, first.status)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL, second.status)
        assertEquals(before, snapshot(durable.toPath(), reports.toPath()))
        assertEquals(first.durableJsonSha256, second.durableJsonSha256)
    }

    @Test
    fun everyPartialArtifactCombinationFailsClosed() = withRoots { durable, reports ->
        val targets = listOf(
            durable.toPath().resolve("validation-batch-1/decision-validations.v1.json"),
            reports.toPath().resolve("validation-batch-1/persistence-report.v1.json"),
            reports.toPath().resolve("validation-batch-1/persistence-report.v1.txt"),
        )
        for (mask in 1 until 7) {
            targets.forEachIndexed { index, path -> if (mask and (1 shl index) != 0) write(path, byteArrayOf(1)) }
            val result = execute("validation-batch-1", validBatch(), durable, reports)
            assertFailure(result, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceFailureReasonV1.PARTIAL_ARTIFACT_STATE)
            targets.forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun existingDurableOrReportConflictIsNeverOverwritten() = withRoots { durable, reports ->
        val request = request("validation-batch-1", validBatch(), durable, reports)
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Completed>(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.execute(request),
        )
        val durablePath = durable.toPath().resolve("validation-batch-1/decision-validations.v1.json")
        val reportPath = reports.toPath().resolve("validation-batch-1/persistence-report.v1.json")
        val textPath = reports.toPath().resolve("validation-batch-1/persistence-report.v1.txt")
        val originalDurable = Files.readAllBytes(durablePath)
        write(durablePath, originalDurable + 1)
        assertFailure(executeRequest(request), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceFailureReasonV1.EXISTING_DURABLE_ARTIFACT_CONFLICT)
        write(durablePath, originalDurable)
        val originalReport = Files.readAllBytes(reportPath)
        write(reportPath, originalReport + 1)
        assertFailure(executeRequest(request), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceFailureReasonV1.EXISTING_REPORT_ARTIFACT_CONFLICT)
        write(reportPath, originalReport)
        val originalText = Files.readAllBytes(textPath)
        write(textPath, originalText + 1)
        assertFailure(executeRequest(request), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceFailureReasonV1.EXISTING_REPORT_ARTIFACT_CONFLICT)
        assertTrue(Files.readAllBytes(durablePath).contentEquals(originalDurable))
    }

    @Test
    fun unsafeRootsAndSymlinkEscapesFailClosed() = withRoots { durable, reports ->
        val rootFile = Files.createTempFile("him-validation-root-file", ".tmp").toFile()
        try {
            assertFailure(
                execute("validation-batch-1", validBatch(), rootFile, reports),
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceFailureReasonV1.WRITE_FAILED,
            )
        } finally {
            rootFile.delete()
        }
        val symlinkParent = Files.createTempDirectory("him-validation-symlink")
        val outside = Files.createTempDirectory("him-validation-outside")
        try {
            val link = symlinkParent.resolve("link")
            try {
                Files.createSymbolicLink(link, outside)
            } catch (_: UnsupportedOperationException) {
                return@withRoots
            } catch (_: java.nio.file.FileSystemException) {
                return@withRoots
            }
            assertFailure(
                execute("validation-batch-1", validBatch(), link.toFile(), reports),
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceFailureReasonV1.UNSAFE_DURABLE_ROOT,
            )
        } finally {
            symlinkParent.toFile().deleteRecursively()
            outside.toFile().deleteRecursively()
        }
    }

    @Test
    fun typedFailuresContainOnlySafeContext() = withRoots { durable, reports ->
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Failed>(
            execute("validation-batch-1", validBatch().copy(state = "wrong"), durable, reports),
        )
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceFailureReasonV1.INVALID_VALIDATION_BATCH, failure.reason)
        assertTrue(failure.safeContext in setOf("validationBatch", "batchId", "persistence"))
        assertFalse(failure.safeContext.contains('/'))
        assertFalse(failure.safeContext.contains("exception", ignoreCase = true))
    }

    @Test
    fun forbiddenSemanticsAndRuntimeDependenciesAreAbsentFromPersistenceModels() {
        val forbidden = setOf(
            "goldLabel", "goldApproved", "negativeSupervisionRecord", "trainingApproved", "trainingSplit", "modelTarget",
            "authorityMutation", "catalogMutation", "registryMutation", "publicationApproved", "automaticPromotion",
            "automaticAdjudication", "validatorAssignment", "clock", "randomness", "uuid",
        )
        val classes = listOf(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceRequestV1::class.java,
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceReportV1::class.java,
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Completed::class.java,
        )
        val names = classes.flatMap { it.declaredFields.filterNot { field -> field.isSynthetic }.map { field -> field.name } }.toSet()
        assertTrue(forbidden.intersect(names).isEmpty())
        assertFalse(names.any { it.contains("network", ignoreCase = true) || it.contains("sqlite", ignoreCase = true) })
    }

    @Test
    fun failureTaxonomyContainsAllRequiredSafeReasons() {
        val required = setOf(
            "INVALID_VALIDATION_BATCH_ID", "UNSAFE_DURABLE_ROOT", "UNSAFE_REPORT_ROOT", "UNSAFE_TARGET_PATH",
            "INVALID_VALIDATION_BATCH", "INVALID_INPUT_BINDING", "INPUT_BINDING_DIGEST_MISMATCH", "INVALID_VALIDATOR",
            "VALIDATOR_NOT_INDEPENDENT", "INVALID_VALIDATION_RECORD", "DUPLICATE_VALIDATION_IDENTITY", "INVALID_COUNTERS",
            "VALIDATION_RECORD_DIGEST_MISMATCH", "VALIDATION_BINDING_DIGEST_MISMATCH", "VALIDATION_LOGICAL_DIGEST_MISMATCH",
            "PARTIAL_ARTIFACT_STATE", "EXISTING_DURABLE_ARTIFACT_CONFLICT", "EXISTING_REPORT_ARTIFACT_CONFLICT", "SERIALIZATION_FAILED",
            "DESERIALIZATION_FAILED", "READ_FAILED", "WRITE_FAILED", "ATOMIC_PUBLICATION_FAILED", "ROLLBACK_FAILED",
            "RELOAD_MISMATCH", "BYTE_IDENTITY_MISMATCH", "FORBIDDEN_GOLD_SEMANTICS", "FORBIDDEN_NEGATIVE_SUPERVISION_SEMANTICS",
            "FORBIDDEN_TRAINING_SEMANTICS", "FORBIDDEN_AUTHORITY_MUTATION_SEMANTICS",
        )
        assertTrue(required.all { name -> HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceFailureReasonV1.entries.any { it.name == name } })
    }

    private fun validBatch() = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.createBatch(
        inputBinding(), VALIDATOR, 1, 1, validRecords(),
    )

    private fun inputBinding() = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING

    private fun validRecords() = inputBinding().originalSelections.mapIndexed { index, selection -> record(index, selection) }

    private fun record(
        index: Int,
        selection: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1 = inputBinding().originalSelections[index],
        assessment: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1 = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
        reasonCodes: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1> = if (selection.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION) {
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
        evidenceReferenceIds: List<String> = selection.evidenceReferenceIds.sorted(),
        rationale: String = "Independent evidence review for unit $index",
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.createValidationRecord(
        inputBinding(), selection, VALIDATOR, 1, 1, assessment, reasonCodes, evidenceReferenceIds, rationale,
    )

    private fun batchReplacing(index: Int, replacement: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1) =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.createBatch(
            inputBinding(), VALIDATOR, 1, 1, validRecords().toMutableList().also { it[index] = replacement },
        )

    private fun request(
        id: String,
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
        durable: File,
        reports: File,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceRequestV1(true, durable, reports, id, batch)

    private fun execute(
        id: String,
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
        durable: File = File("/private/tmp/him-validation-not-used-durable"),
        reports: File = File("/private/tmp/him-validation-not-used-reports"),
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.execute(request(id, batch, durable, reports))

    private fun executeRequest(request: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceRequestV1) =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.execute(request)

    private fun assertFailure(
        result: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1,
        reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceFailureReasonV1,
    ) {
        assertEquals(reason, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Failed>(result).reason)
    }

    private fun assertSerializationFailure(batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1) {
        assertFailsWith<RuntimeException> {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.serializeBatch(batch)
        }
    }

    private fun assertDeserializeFailure(json: String) {
        assertFailsWith<RuntimeException> {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.deserializeBatch(json.toByteArray(Charsets.UTF_8))
        }
    }

    private fun withRoots(block: (durable: File, reports: File) -> Unit) {
        val root = Files.createTempDirectory("him-validation-persistence-test")
        try {
            block(root.resolve("durable").toFile(), root.resolve("reports").toFile())
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun write(path: Path, bytes: ByteArray) {
        Files.createDirectories(path.parent)
        Files.write(path, bytes)
    }

    private fun snapshot(vararg roots: Path): Map<String, List<Byte>> = roots
        .flatMap { root ->
            if (!Files.exists(root)) emptyList() else Files.walk(root).use { stream ->
                stream.filter { Files.isRegularFile(it) }.map { file -> file.toString() to Files.readAllBytes(file).toList() }.toList()
            }
        }
        .toMap()

    private fun allFiles(vararg roots: Path): Set<Path> = roots
        .flatMap { root ->
            if (!Files.exists(root)) emptyList() else Files.walk(root).use { stream -> stream.filter { Files.isRegularFile(it) }.toList() }
        }
        .toSet()

    private fun reportTextBytes(report: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceReportV1) =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.renderReportText(report).toByteArray(Charsets.UTF_8)

    private companion object {
        const val VALIDATOR = "reviewer:independent-v1"
    }
}
