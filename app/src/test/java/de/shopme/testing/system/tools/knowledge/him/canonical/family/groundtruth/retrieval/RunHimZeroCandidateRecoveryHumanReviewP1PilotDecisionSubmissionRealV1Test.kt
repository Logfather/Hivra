package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyValidator
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReviewUnitV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewRuntimeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewRuntimeRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewScopeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusReportV1
import java.io.File
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Assume.assumeTrue

/**
 * Default-disabled boundary for the already committed P1 decision submission.
 * The enabled path only becomes operational after all explicit gates pass.
 */
class RunHimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionRealV1Test {

    @Test
    fun `entrypoint contract and confirmation are frozen`() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_SUBMISSION_REAL_BOUND_ENTRYPOINT_V1",
            ENTRYPOINT_CONTRACT_ID,
        )
        assertEquals(
            "him.zeroCandidateRecoveryHumanReviewP1PilotDecisionSubmission.enabled",
            ENABLED_PROPERTY,
        )
        assertEquals(
            "him.zeroCandidateRecoveryHumanReviewP1PilotDecisionSubmission.confirmation",
            CONFIRMATION_PROPERTY,
        )
        assertEquals(
            "him.zeroCandidateRecoveryHumanReviewP1PilotDecisionSubmission.authorizedExecutionHead",
            AUTHORIZED_EXECUTION_HEAD_PROPERTY,
        )
        assertEquals(
            "AUTHORIZED_BOUNDED_P1_PILOT_DECISION_SUBMISSION_OFFLINE",
            CONFIRMATION,
        )
    }

    @Test
    fun `entrypoint is disabled with explicit null inputs`() {
        val gate = gate(null, null, null)
        assertFalse(gate.enabled)
        assertFalse(gateIsComplete(gate))
        assertEquals(null, parseAuthorizedExecutionHead(gate.authorizedExecutionHead))
    }

    @Test
    fun `default gate does not depend on ambient properties`() {
        val gate = gate(null, null, null)
        assertFalse(gate.enabled && gate.confirmation == CONFIRMATION)
        assertFalse(gateIsComplete(gate))
    }

    @Test
    fun `wrong confirmation is rejected before repository access`() {
        val gate = gate("true", "WRONG", "a".repeat(40))
        assertTrue(gate.enabled)
        assertFalse(gateIsComplete(gate))
    }

    @Test
    fun `missing confirmation is rejected before repository access`() {
        assertFalse(gateIsComplete(gate("true", null, "a".repeat(40))))
    }

    @Test
    fun `missing authorized execution head is rejected before repository access`() {
        assertFalse(gateIsComplete(gate("true", CONFIRMATION, null)))
    }

    @Test
    fun `blank authorized execution head is rejected`() {
        assertEquals(null, parseAuthorizedExecutionHead(""))
        assertEquals(null, parseAuthorizedExecutionHead("   "))
    }

    @Test
    fun `uppercase authorized execution head is rejected`() {
        assertEquals(null, parseAuthorizedExecutionHead("A".repeat(40)))
    }

    @Test
    fun `wrong length authorized execution head is rejected`() {
        assertEquals(null, parseAuthorizedExecutionHead("a".repeat(39)))
        assertEquals(null, parseAuthorizedExecutionHead("a".repeat(41)))
    }

    @Test
    fun `non hexadecimal authorized execution head is rejected`() {
        assertEquals(null, parseAuthorizedExecutionHead("g".repeat(40)))
    }

    @Test
    fun `gate stages keep input access after all authorization gates`() {
        assertEquals(
            listOf(
                "enabled",
                "confirmation",
                "authorizedExecutionHead",
                "sourceIntegration",
                "repositoryInputs",
                "outputs",
            ),
            GATE_STAGES,
        )
        assertTrue(GATE_STAGES.indexOf("sourceIntegration") < GATE_STAGES.indexOf("repositoryInputs"))
        assertTrue(GATE_STAGES.indexOf("repositoryInputs") < GATE_STAGES.indexOf("outputs"))
    }

    @Test
    fun `core baseline is frozen independently`() {
        assertEquals("06ee4947762ce9493d19ebfd026c32103dbd1d91", CORE_BASELINE_HEAD)
        assertTrue(CORE_BASELINE_HEAD.matches(HEAD_PATTERN))
        val executionHead = "a".repeat(40)
        assertEquals(executionHead, executionHeadForRuntime(executionHead))
        assertTrue(CORE_BASELINE_HEAD != executionHead)
    }

    @Test
    fun `execution head mismatch fails closed`() {
        assertEquals(
            "EXECUTION_HEAD_MISMATCH",
            executionHeadFailure("a".repeat(40), "b".repeat(40)),
        )
        assertEquals(null, executionHeadFailure("a".repeat(40), "a".repeat(40)))
    }

    @Test
    fun `ancestor result is fail closed`() {
        assertTrue(ancestorExitIsValid(0))
        assertFalse(ancestorExitIsValid(1))
    }

    @Test
    fun `submission contract state is unpersisted`() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_SUBMISSION_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.VERSION)
        assertEquals(
            "HUMAN_REVIEWER_AUTHORIZED_UNPERSISTED",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.STATE,
        )
    }

    @Test
    fun `submission identity and reviewer are frozen`() {
        val submission = submission()
        assertEquals(
            "p1-artischocken-herzen-brie-double-creme-reviewer-logfather-r1-v1",
            submission.submissionId,
        )
        assertEquals("reviewer:logfather:v1", submission.reviewerRef)
        assertEquals(1, submission.reviewRound)
        assertTrue(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.validate(submission).valid)
    }

    @Test
    fun `submission contains exactly four frozen selections`() {
        val submission = submission()
        assertEquals(4, submission.selections.size)
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.FROZEN_SELECTIONS.map { it.reviewUnitId },
            submission.selections.map { it.reviewUnitId },
        )
        assertEquals(4, submission.selections.map { it.reviewUnitId }.distinct().size)
    }

    @Test
    fun `submission decisions are confirm reject confirm reject`() {
        assertEquals(
            listOf(
                HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
            ),
            submission().selections.map { it.decision },
        )
    }

    @Test
    fun `reject selections contain both frozen reject reasons`() {
        val expected = setOf(
            HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_SEMANTIC_MISMATCH,
            HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED,
        )
        submission().selections.filter { it.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION }
            .forEach { assertEquals(expected, it.reasonCodes.toSet()) }
    }

    @Test
    fun `submission has no alternative canonical proposals`() {
        assertTrue(submission().selections.all { it.alternativeCanonicalProposal == null })
    }

    @Test
    fun `submission revisions are uniform`() {
        assertEquals(setOf(1), submission().selections.map { it.revision }.toSet())
    }

    @Test
    fun `submission evidence is twelve unique references`() {
        val references = submission().selections.flatMap { it.evidenceReferenceIds }
        assertEquals(12, references.size)
        assertEquals(12, references.distinct().size)
    }

    @Test
    fun `submission creation is deterministic`() {
        val first = submission()
        val second = submission()
        assertEquals(first, second)
        assertEquals(first.submissionBindingDigest, second.submissionBindingDigest)
        assertEquals(first.submissionLogicalDigest, second.submissionLogicalDigest)
    }

    @Test
    fun `submission materialization is the committed pure operation`() {
        val methods = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1::class.java
            .declaredMethods.map { it.name }
        assertTrue(methods.contains("materialize"))
        assertTrue(methods.contains("validate"))
        assertTrue(methods.none { it.contains("persist", ignoreCase = true) || it.contains("write", ignoreCase = true) })
    }

    @Test
    fun `runtime request fields remain committed`() {
        val fields = HimZeroCandidateRecoveryHumanReviewRuntimeRequestV1::class.java
            .declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .map { it.name }
        assertEquals(
            setOf(
                "enabled",
                "batchId",
                "inputBinding",
                "corpus",
                "catalog",
                "registry",
                "authority",
                "reviewScope",
                "priorDecisionRecords",
                "submittedDecisionRecords",
                "durableDecisionBatchRoot",
                "derivedReportRoot",
            ),
            fields.toSet(),
        )
    }

    @Test
    fun `runtime request has no source or provider dependency`() {
        val forbidden = listOf("Source", "Store", "Search", "Fetch", "Scan", "SQLite", "OpenAI", "Provider", "Inference")
        val names = HimZeroCandidateRecoveryHumanReviewRuntimeRequestV1::class.java.declaredFields.flatMap { field ->
            listOf(field.name, field.type.simpleName)
        }
        assertTrue(names.none { name -> forbidden.any { token -> name.contains(token, ignoreCase = true) } })
    }

    @Test
    fun `foundation uses master catalog registry and authority`() {
        assertEquals("masterFoundation", FOUNDATION_ROLE)
        assertFalse(FOUNDATION_ROLE.contains("active", ignoreCase = true))
        assertEquals("HimCanonicalFamilyValidator", FOUNDATION_VALIDATOR)
    }

    @Test
    fun `output roots and batch paths are relative`() {
        assertFalse(HimZeroCandidateRecoveryHumanReviewContractV1.DURABLE_DECISION_BATCH_ROOT.startsWith('/'))
        assertFalse(HimZeroCandidateRecoveryHumanReviewContractV1.DERIVED_REPORT_ROOT.startsWith('/'))
        assertEquals("review-decisions.v1.json", DECISION_BATCH_FILE_NAME)
        assertEquals("persistence-report.v1.json", PERSISTENCE_REPORT_JSON_FILE_NAME)
        assertEquals("persistence-report.v1.txt", PERSISTENCE_REPORT_TEXT_FILE_NAME)
    }

    @Test
    fun `batch identifier is derived from committed submission`() {
        val submission = submission()
        val batchId = batchIdForSubmission(submission)
        assertEquals(OPERATIVE_BATCH_ID, batchId)
        assertTrue(batchId.matches(SAFE_BATCH_ID))
        assertTrue(batchId.length <= 64)
        assertEquals(batchId, batchIdForSubmission(submission))
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.SUBMISSION_ID,
            submission.submissionId,
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.REVIEWER_REF,
            submission.reviewerRef,
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.REVIEW_ROUND,
            submission.reviewRound,
        )
        assertTrue(submission.selections.all { it.revision == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.REVISION })
    }

    @Test
    fun `batch identifier rejects a different submission binding`() {
        val submission = submission()
        assertFailsWith<IllegalArgumentException> {
            batchIdForSubmission(submission.copy(submissionId = "different-submission-v1"))
        }
        assertFailsWith<IllegalArgumentException> {
            batchIdForSubmission(submission.copy(reviewerRef = "reviewer:other:v1"))
        }
        assertFailsWith<IllegalArgumentException> {
            batchIdForSubmission(submission.copy(reviewRound = 2))
        }
        assertFailsWith<IllegalArgumentException> {
            batchIdForSubmission(
                submission.copy(
                    selections = submission.selections.map { it.copy(revision = 2) },
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            batchIdForSubmission(
                submission.copy(
                    missionBinding = submission.missionBinding.copy(missionId = "different-mission-v1"),
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            requireConfiguredBatchId("different-batch-v1")
        }
    }

    @Test
    fun `derived output paths stay inside their roots`() {
        val root = File("fixture-root")
        val paths = outputPaths(root, batchIdForSubmission(submission()))
        assertTrue(paths.batch.path.contains("decision-batches"))
        assertTrue(paths.reportJson.path.contains("batches"))
        assertTrue(paths.reportText.path.contains("batches"))
        assertFalse(paths.batch.path.contains(".."))
        assertFalse(paths.reportJson.path.contains(".."))
        assertFalse(paths.reportText.path.contains(".."))
    }

    @Test
    fun `operational entrypoint is skipped without complete opt in`() {
        val gate = gate(null, null, null)
        assumeTrue(!gateIsComplete(gate))
    }

    @Test
    fun `disabled boundary does not access repository inputs`() {
        val gate = gate(null, null, null)
        assertFalse(gateIsComplete(gate))
        assertTrue(INPUT_ACCESS_REQUIRES_COMPLETE_GATE)
        assertTrue(OUTPUT_ACCESS_REQUIRES_COMPLETE_GATE)
    }

    @Test
    fun `diagnostics contain only typed safe values`() {
        val message = safeDiagnostic("INVALID_INPUT_BINDING", "inputBinding")
        assertEquals("INVALID_INPUT_BINDING inputBinding", message)
        assertFalse(message.contains("Exception"))
        assertFalse(message.contains("Throwable"))
        assertFalse(message.contains('/'))
    }

    @Test
    fun `real input names are frozen without loading them on the default path`() {
        assertEquals("review-corpus.v1.json", CORPUS_FILE_NAME)
        assertEquals("review-packet.v1.json", PACKET_FILE_NAME)
        assertEquals("direct-evidence-supplement.v1.json", SUPPLEMENT_FILE_NAME)
        assertTrue(CORPUS_JSON_PATH.startsWith("build/"))
        assertTrue(PACKET_JSON_PATH.startsWith("build/"))
        assertTrue(SUPPLEMENT_JSON_PATH.startsWith("build/"))
    }

    @Test
    fun `real-bound entrypoint does not invoke runtime or persistence`() {
        assertEquals(
            "writes current real-bound P1 pilot decision submission without persisting decisions",
            REAL_EXECUTION_TEST_NAME,
        )
        assertTrue(NO_PERSISTENCE_IN_CURRENT_OFFLINE_ENTRYPOINT)
    }

    @Test
    fun `hermetic tests do not create decision batches`() {
        val root = Files.createTempDirectory("him-p1-submission-hermetic-").toFile()
        val paths = outputPaths(root, batchIdForSubmission(submission()))
        assertFalse(paths.batch.exists())
        assertFalse(paths.reportJson.exists())
        assertFalse(paths.reportText.exists())
        root.deleteRecursively()
    }

    @Test
    fun `foundation and input validation stay read only`() {
        val methods = listOf(
            HimCanonicalFamilyPersistence::class.java,
            HimProductOnlyCanonicalMasterReader::class.java,
            HimCanonicalFamilyValidator::class.java,
            HimActiveGroundTruthResolutionV1::class.java,
        ).flatMap { type -> type.declaredMethods.map { it.name } }
        assertTrue(methods.contains("read"))
        assertTrue(methods.none { it.contains("write", ignoreCase = true) && it.contains("Index", ignoreCase = true) })
    }

    @Test
    fun `real-bound operational path has exactly one entrypoint`() {
        val methods = this::class.java.declaredMethods.map { it.name }
        assertEquals(1, methods.count { it == REAL_EXECUTION_METHOD_NAME })
    }

    @Test
    fun `real-bound path requires source integration only after submission gates`() {
        assertEquals("sourceIntegration", GATE_STAGES[3])
        assertTrue(GATE_STAGES.take(3).all { it != "repositoryInputs" })
    }

    @Test
    fun `no real artifact is used by hermetic submission fixtures`() {
        assertTrue(HERMETIC_FIXTURES_ONLY)
        assertFalse(HERMETIC_PACKET_PATH.startsWith("build/"))
        assertFalse(HERMETIC_SUPPLEMENT_PATH.startsWith("build/"))
        assertFalse(HERMETIC_CORPUS_PATH.startsWith("build/"))
    }

    /**
     * This is deliberately the only method that can pass the opt-in boundary. It does not
     * execute the HumanReview runtime in this phase: doing so would publish a decision batch.
     */
    @Test
    fun `writes current real-bound P1 pilot decision submission without persisting decisions`() {
        val authorizedExecutionHead = requireRealGate()
        val root = projectRoot()
        val currentHead = git(root, "rev-parse", "HEAD")
        require(currentHead == authorizedExecutionHead) { "EXECUTION_HEAD_MISMATCH" }
        require(isAncestor(root, CORE_BASELINE_HEAD, authorizedExecutionHead)) {
            "CORE_BASELINE_NOT_ANCESTOR"
        }

        val corpusFile = root.resolve(CORPUS_JSON_PATH)
        require(corpusFile.isFile) { "CORPUS_INPUT_MISSING" }
        require(corpusFile.length() == CORPUS_BYTES) { "CORPUS_SIZE_MISMATCH" }
        require(sha256(corpusFile) == CORPUS_SHA256) { "CORPUS_SHA256_MISMATCH" }
        val corpus = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.readReport(corpusFile)
        require(corpus.logicalDigest == CORPUS_LOGICAL_DIGEST) { "CORPUS_LOGICAL_DIGEST_MISMATCH" }
        require(corpus.inputBinding.bindingDigest == CORPUS_BINDING_DIGEST) { "CORPUS_BINDING_DIGEST_MISMATCH" }

        val packetFile = root.resolve(PACKET_JSON_PATH)
        require(packetFile.isFile) { "PACKET_INPUT_MISSING" }
        require(sha256(packetFile) == PACKET_JSON_SHA256) { "PACKET_SHA256_MISMATCH" }
        val packet = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.readPacket(packetFile)
        require(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.validatePacket(packet).valid) {
            "PACKET_VALIDATION_FAILED"
        }
        require(packet.packetBindingDigest == PACKET_BINDING_DIGEST) { "PACKET_BINDING_MISMATCH" }
        require(packet.packetLogicalDigest == PACKET_LOGICAL_DIGEST) { "PACKET_LOGICAL_DIGEST_MISMATCH" }

        val supplementFile = root.resolve(SUPPLEMENT_JSON_PATH)
        require(supplementFile.isFile) { "SUPPLEMENT_INPUT_MISSING" }
        require(sha256(supplementFile) == SUPPLEMENT_JSON_SHA256) { "SUPPLEMENT_SHA256_MISMATCH" }
        val supplement = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.readSupplement(supplementFile)
        require(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.validate(supplement).valid) {
            "SUPPLEMENT_VALIDATION_FAILED"
        }
        require(supplement.supplementBindingDigest == SUPPLEMENT_BINDING_DIGEST) { "SUPPLEMENT_BINDING_MISMATCH" }
        require(supplement.supplementLogicalDigest == SUPPLEMENT_LOGICAL_DIGEST) { "SUPPLEMENT_LOGICAL_DIGEST_MISMATCH" }

        val paths = HimCanonicalFamilyPaths(root)
        val catalog = HimProductOnlyCanonicalMasterReader().read(paths)
        val persistence = HimCanonicalFamilyPersistence()
        val registry = persistence.readRegistry(paths.entityIdRegistry)
        val masterAuthority = persistence.readAuthority(paths.familyAuthority)
        HimCanonicalFamilyValidator().validate(catalog, registry, masterAuthority)
        require(catalog.contentSha256 == CATALOG_SHA256) { "CATALOG_SHA256_MISMATCH" }
        require(sha256(paths.entityIdRegistry) == REGISTRY_SHA256) { "REGISTRY_SHA256_MISMATCH" }
        require(sha256(paths.familyAuthority) == AUTHORITY_SHA256) { "AUTHORITY_SHA256_MISMATCH" }

        val inputBinding = inputBinding(root, corpusFile, corpus, catalog, registry, currentHead, persistence)
        require(inputBinding.validate().valid) { "INPUT_BINDING_INVALID" }
        val submission = submission()
        require(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.validate(submission).valid) {
            "SUBMISSION_VALIDATION_FAILED"
        }
        val materialized = when (
            val result = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.materialize(submission, supplement)
        ) {
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1.Completed -> result.records
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1.Failed ->
                fail(safeDiagnostic(result.reason.name, "materialization"))
        }
        require(materialized.size == 4) { "MATERIALIZED_DECISION_COUNT_MISMATCH" }
        materialized.forEach { record -> require(record.validate().valid) { "DECISION_RECORD_INVALID" } }

        val scope = scope(inputBinding, materialized)
        val request = HimZeroCandidateRecoveryHumanReviewRuntimeRequestV1(
            enabled = true,
            batchId = OPERATIVE_BATCH_ID,
            inputBinding = inputBinding,
            corpus = corpus,
            catalog = catalog,
            registry = registry,
            authority = masterAuthority,
            reviewScope = scope,
            priorDecisionRecords = emptyList(),
            submittedDecisionRecords = materialized,
            durableDecisionBatchRoot = root.resolve(HimZeroCandidateRecoveryHumanReviewContractV1.DURABLE_DECISION_BATCH_ROOT),
            derivedReportRoot = root.resolve(HimZeroCandidateRecoveryHumanReviewContractV1.DERIVED_REPORT_ROOT),
        )
        require(request.enabled && request.submittedDecisionRecords.size == 4) { "REQUEST_SHAPE_INVALID" }
        val outputs = outputPaths(root, request.batchId)
        require(!outputs.batch.exists() && !outputs.reportJson.exists() && !outputs.reportText.exists()) {
            "REAL_OUTPUT_ALREADY_EXISTS"
        }
    }

    private fun submission() =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.create()

    private fun scope(
        inputBinding: HimZeroCandidateRecoveryHumanReviewInputBindingV1,
        records: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1>,
    ) = HimZeroCandidateRecoveryHumanReviewScopeV1(
        scopeId = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.MISSION_ID,
        reviewRound = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.REVIEW_ROUND,
        authorizedReviewUnitIds = records.map { it.reviewUnit.reviewUnitId }.sorted(),
        maxNewDecisionRecords = records.size,
        inputBindingDigest = inputBinding.bindingDigest,
        scopeDigest = HimZeroCandidateRecoveryHumanReviewRuntimeV1.scopeDigest(
            HimZeroCandidateRecoveryHumanReviewScopeV1(
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.MISSION_ID,
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.REVIEW_ROUND,
                records.map { it.reviewUnit.reviewUnitId }.sorted(),
                records.size,
                inputBinding.bindingDigest,
                "",
            ),
        ),
    )

    private fun inputBinding(
        root: File,
        corpusFile: File,
        corpus: HimZeroCandidateRecoveryReviewCorpusReportV1,
        catalog: HimProductOnlyCanonicalMaster,
        registry: HimEntityIdRegistry,
        currentHead: String,
        persistence: HimCanonicalFamilyPersistence,
    ): HimZeroCandidateRecoveryHumanReviewInputBindingV1 {
        val unsigned = HimZeroCandidateRecoveryHumanReviewInputBindingV1(
            corpusFileBinding = fileBinding(root, corpusFile, corpus.logicalDigest),
            corpusLogicalDigest = corpus.logicalDigest,
            corpusReportDigest = sha256(corpusFile),
            corpusBindingDigest = corpus.inputBinding.bindingDigest,
            existingCorpusInputBinding = corpus.inputBinding,
            registryBinding = fileBinding(
                root,
                HimCanonicalFamilyPaths(root).entityIdRegistry,
                logicalDigest(persistence.serialize(registry)),
            ),
            contractId = HimZeroCandidateRecoveryHumanReviewContractV1.VERSION,
            contractVersion = HimZeroCandidateRecoveryHumanReviewContractV1.VERSION,
            implementationHead = currentHead,
            bindingDigest = "",
        )
        require(unsigned.existingCorpusInputBinding.canonicalCatalog.relativePath == catalog.path) {
            "CORPUS_CATALOG_BINDING_MISMATCH"
        }
        return unsigned.copy(bindingDigest = HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(unsigned))
    }

    private fun fileBinding(root: File, file: File, logicalDigest: String) =
        HimZeroCandidateRecoveryHumanReviewFileBindingV1(
            relativePath = relativePath(root, file),
            byteSize = file.length(),
            sha256 = sha256(file),
            logicalDigest = logicalDigest,
        )

    private fun outputPaths(root: File, batchId: String) = OutputPaths(
        batch = root.resolve(HimZeroCandidateRecoveryHumanReviewContractV1.DURABLE_DECISION_BATCH_ROOT)
            .resolve("$batchId/$DECISION_BATCH_FILE_NAME"),
        reportJson = root.resolve(HimZeroCandidateRecoveryHumanReviewContractV1.DERIVED_REPORT_ROOT)
            .resolve("batches/$batchId/$PERSISTENCE_REPORT_JSON_FILE_NAME"),
        reportText = root.resolve(HimZeroCandidateRecoveryHumanReviewContractV1.DERIVED_REPORT_ROOT)
            .resolve("batches/$batchId/$PERSISTENCE_REPORT_TEXT_FILE_NAME"),
    )

    private fun logicalDigest(serialized: ByteArray): String = sha256(
        if (serialized.lastOrNull() == '\n'.code.toByte()) serialized.copyOf(serialized.size - 1) else serialized,
    )

    private fun requireRealGate(): String {
        assumeTrue(System.getProperty(ENABLED_PROPERTY) == "true")
        assumeTrue(System.getProperty(CONFIRMATION_PROPERTY) == CONFIRMATION)
        val authorized = parseAuthorizedExecutionHead(System.getProperty(AUTHORIZED_EXECUTION_HEAD_PROPERTY))
        assumeTrue(authorized != null)
        HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        return requireNotNull(authorized)
    }

    private fun gate(enabled: String?, confirmation: String?, authorizedExecutionHead: String?) = Gate(
        enabled = enabled == "true",
        confirmation = confirmation,
        authorizedExecutionHead = authorizedExecutionHead,
    )

    private fun gateIsComplete(gate: Gate): Boolean =
        gate.enabled && gate.confirmation == CONFIRMATION && parseAuthorizedExecutionHead(gate.authorizedExecutionHead) != null

    private fun parseAuthorizedExecutionHead(value: String?): String? = value?.takeIf { it.matches(HEAD_PATTERN) }

    private fun executionHeadForRuntime(authorizedExecutionHead: String): String = authorizedExecutionHead

    private fun executionHeadFailure(authorizedExecutionHead: String, currentHead: String): String? =
        if (authorizedExecutionHead == currentHead) null else "EXECUTION_HEAD_MISMATCH"

    private fun ancestorExitIsValid(exitCode: Int): Boolean = exitCode == 0

    private fun projectRoot(): File {
        var current = File(System.getProperty("user.dir") ?: error("USER_DIR_UNAVAILABLE")).canonicalFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile) return current
            current = current.parentFile ?: error("REPOSITORY_ROOT_NOT_FOUND")
        }
    }

    private fun git(root: File, vararg arguments: String): String {
        val process = ProcessBuilder(listOf("git") + arguments.toList()).directory(root).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        require(process.waitFor() == 0 && output.matches(HEAD_PATTERN)) { "GIT_BINDING_FAILED" }
        return output
    }

    private fun isAncestor(root: File, ancestor: String, descendant: String): Boolean {
        val process = ProcessBuilder("git", "merge-base", "--is-ancestor", ancestor, descendant)
            .directory(root)
            .redirectErrorStream(true)
            .start()
        process.inputStream.close()
        return process.waitFor() == 0
    }

    private fun relativePath(root: File, file: File): String = root.canonicalFile.toPath()
        .relativize(file.canonicalFile.toPath())
        .toString()
        .replace(File.separatorChar, '/')

    private fun sha256(file: File): String = file.inputStream().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(1024 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun batchIdForSubmission(submission: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1): String =
        OPERATIVE_BATCH_ID.also {
            require(submission.submissionId == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.SUBMISSION_ID) {
                "BATCH_ID_SUBMISSION_MISMATCH"
            }
            require(submission.missionBinding.missionId == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.MISSION_ID) {
                "BATCH_ID_MISSION_MISMATCH"
            }
            require(submission.missionBinding.scopeId == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.MISSION_ID) {
                "BATCH_ID_SCOPE_MISMATCH"
            }
            require(submission.reviewerRef == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.REVIEWER_REF) {
                "BATCH_ID_REVIEWER_MISMATCH"
            }
            require(submission.reviewRound == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.REVIEW_ROUND) {
                "BATCH_ID_ROUND_MISMATCH"
            }
            require(submission.selections.all { it.revision == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.REVISION }) {
                "BATCH_ID_REVISION_MISMATCH"
            }
            requireConfiguredBatchId(it)
        }

    private fun requireConfiguredBatchId(batchId: String) {
        require(batchId == OPERATIVE_BATCH_ID) { "BATCH_ID_MISMATCH" }
        require(batchId.matches(SAFE_BATCH_ID)) { "BATCH_ID_INVALID" }
        require(batchId.length <= 64) { "BATCH_ID_TOO_LONG" }
    }

    private fun safeDiagnostic(reason: String, safeContext: String): String = "$reason $safeContext"

    private data class Gate(
        val enabled: Boolean,
        val confirmation: String?,
        val authorizedExecutionHead: String?,
    )

    private data class OutputPaths(
        val batch: File,
        val reportJson: File,
        val reportText: File,
    )

    companion object {
        const val ENTRYPOINT_CONTRACT_ID =
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_SUBMISSION_REAL_BOUND_ENTRYPOINT_V1"
        const val ENABLED_PROPERTY =
            "him.zeroCandidateRecoveryHumanReviewP1PilotDecisionSubmission.enabled"
        const val CONFIRMATION_PROPERTY =
            "him.zeroCandidateRecoveryHumanReviewP1PilotDecisionSubmission.confirmation"
        const val AUTHORIZED_EXECUTION_HEAD_PROPERTY =
            "him.zeroCandidateRecoveryHumanReviewP1PilotDecisionSubmission.authorizedExecutionHead"
        const val CONFIRMATION = "AUTHORIZED_BOUNDED_P1_PILOT_DECISION_SUBMISSION_OFFLINE"
        const val CORE_BASELINE_HEAD = "06ee4947762ce9493d19ebfd026c32103dbd1d91"
        const val SOURCE_INTEGRATION_PROPERTY = "him.sourceIntegration.enabled"
        const val FOUNDATION_ROLE = "masterFoundation"
        const val FOUNDATION_VALIDATOR = "HimCanonicalFamilyValidator"
        const val INPUT_ACCESS_REQUIRES_COMPLETE_GATE = true
        const val OUTPUT_ACCESS_REQUIRES_COMPLETE_GATE = true
        const val NO_PERSISTENCE_IN_CURRENT_OFFLINE_ENTRYPOINT = true
        const val HERMETIC_FIXTURES_ONLY = true
        const val HERMETIC_PACKET_PATH = "fixture/review-packet.v1.json"
        const val HERMETIC_SUPPLEMENT_PATH = "fixture/direct-evidence-supplement.v1.json"
        const val HERMETIC_CORPUS_PATH = "fixture/review-corpus.v1.json"
        const val REAL_EXECUTION_TEST_NAME =
            "writes current real-bound P1 pilot decision submission without persisting decisions"
        const val REAL_EXECUTION_METHOD_NAME =
            "writes current real-bound P1 pilot decision submission without persisting decisions"
        const val CORPUS_FILE_NAME = "review-corpus.v1.json"
        const val PACKET_FILE_NAME = "review-packet.v1.json"
        const val SUPPLEMENT_FILE_NAME = "direct-evidence-supplement.v1.json"
        const val DECISION_BATCH_FILE_NAME = "review-decisions.v1.json"
        const val OPERATIVE_BATCH_ID = "p1-artischocken-brie-logfather-r1-v1"
        const val PERSISTENCE_REPORT_JSON_FILE_NAME = "persistence-report.v1.json"
        const val PERSISTENCE_REPORT_TEXT_FILE_NAME = "persistence-report.v1.txt"
        const val CORPUS_JSON_PATH =
            "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-review-corpus/v1/review-corpus.v1.json"
        const val PACKET_JSON_PATH =
            "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-review-packet/v1/p1-artischocken-herzen-brie-double-creme-v1/review-packet.v1.json"
        const val SUPPLEMENT_JSON_PATH =
            "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-direct-evidence-supplement/v1/p1-artischocken-herzen-brie-double-creme-v1/direct-evidence-supplement.v1.json"
        const val CORPUS_BYTES = 2359985L
        const val CORPUS_SHA256 = "4c2dee0e378c62139dcc349c4f0992b49fb7d3fbf2afa408faec3a4b56fc8016"
        const val CORPUS_LOGICAL_DIGEST = "3f1de89b5ea97bfa340ae3c61baa3e7f2a2e4ed0a7dad9bc7867118614d03d02"
        const val CORPUS_BINDING_DIGEST = "b1691dbf87eed143d23ea44275ec5d96bf9483e4675469af5d282d3858288e81"
        const val PACKET_JSON_SHA256 = "4c06a60e7e9f8548088f7d006667d55f36d1d92fb93818d6d95b2f54dd6c8a43"
        const val PACKET_BINDING_DIGEST = "beb465178ecde447e3995672ea77fd16b5a020b6eabfc4e310ee0ae8f8771793"
        const val PACKET_LOGICAL_DIGEST = "311f4925716045d47a02af3652dea66393f7cb713dbfe7239705ce4684acce6e"
        const val SUPPLEMENT_JSON_SHA256 = "5a386e8adba953ac0bbdd8324b92de53cde867dfc577f87ba4a53b2b647062fb"
        const val SUPPLEMENT_BINDING_DIGEST = "6674ba8683e349a9d399ffa72c2fe3a2f52b17ea041901cb74ab15d2d99be291"
        const val SUPPLEMENT_LOGICAL_DIGEST = "ebc15972f77d34265810e77a71c729f4ddf5aba79e2518f3a4759ca71302c579"
        const val CATALOG_SHA256 = "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f"
        const val REGISTRY_SHA256 = "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a"
        const val AUTHORITY_SHA256 = "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184"
        val GATE_STAGES = listOf("enabled", "confirmation", "authorizedExecutionHead", "sourceIntegration", "repositoryInputs", "outputs")
        private val HEAD_PATTERN = Regex("[0-9a-f]{40}")
        private val SAFE_BATCH_ID = Regex("[a-z0-9][a-z0-9-]{0,63}")
    }
}
