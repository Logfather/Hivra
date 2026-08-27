package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewAlternativeCanonicalProposalV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionBatchV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceKindV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidencePositionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewPersistenceCountersV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewPersistenceReportV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewPersistenceRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewPersistenceResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewPersistenceStatusV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReviewUnitV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisFileBindingV1
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewPersistenceV1Test {
    @Test fun persistenceContractIdAndVersionAreFrozen() {
        assertEquals("HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_PERSISTENCE_V1", HimZeroCandidateRecoveryHumanReviewPersistenceV1.CONTRACT_ID)
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewPersistenceV1.VERSION)
    }

    @Test fun durableAndDerivedRootsMatchContract() {
        assertEquals("data/knowledge/him/canonical-family/human-review/zero-candidate-recovery/v1/decision-batches", HimZeroCandidateRecoveryHumanReviewContractV1.DURABLE_DECISION_BATCH_ROOT)
        assertEquals("build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/v1", HimZeroCandidateRecoveryHumanReviewContractV1.DERIVED_REPORT_ROOT)
    }

    @Test fun safeBatchIdIsAccepted() { assertCompleted(execute(batchId = "review-batch-01")) }

    @Test fun invalidBatchIdsAreRejected() {
        listOf("", "Review", "has space", "a/b", "a\\b", "..", "a..b").forEach { id ->
            assertFailure(execute(batchId = id), HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INVALID_BATCH_ID)
        }
    }

    @Test fun targetResolutionCannotLeaveRoots() {
        assertFailure(execute(batchId = "../escape"), HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INVALID_BATCH_ID)
        assertTrue(HimZeroCandidateRecoveryHumanReviewPersistenceV1.CONTRACT_ID.isNotBlank())
    }

    @Test fun validBatchSerializesDeterministically() {
        val batch = batch()
        assertTrue(HimZeroCandidateRecoveryHumanReviewPersistenceV1.serializeBatch(batch).contentEquals(HimZeroCandidateRecoveryHumanReviewPersistenceV1.serializeBatch(batch)))
    }

    @Test fun canonicalJsonEndsWithExactlyOneLf() {
        val text = String(HimZeroCandidateRecoveryHumanReviewPersistenceV1.serializeBatch(batch()), StandardCharsets.UTF_8)
        assertTrue(text.endsWith("\n"))
        assertFalse(text.dropLast(1).endsWith("\n"))
    }

    @Test fun roundTripHasCompleteTypedEquality() {
        val original = batch()
        assertEquals(original, HimZeroCandidateRecoveryHumanReviewPersistenceV1.deserializeBatch(HimZeroCandidateRecoveryHumanReviewPersistenceV1.serializeBatch(original)))
    }

    @Test fun differentRecordInputOrderProducesIdenticalBytes() {
        val first = batch(listOf(record('a', "reviewer-a"), record('b', "reviewer-b")))
        val second = batch(first.decisionRecords.reversed())
        assertTrue(HimZeroCandidateRecoveryHumanReviewPersistenceV1.serializeBatch(first).contentEquals(HimZeroCandidateRecoveryHumanReviewPersistenceV1.serializeBatch(second)))
    }

    @Test fun reasonCodesAreDeterministicallyOrdered() {
        val record = record('a').copy(reasonCodes = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED))
        assertEquals(listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED), record.reasonCodes)
    }

    @Test fun evidenceReferencesAreDeterministicallyOrdered() {
        val record = record('a')
        assertEquals(record.evidenceReferences.sortedBy { it.evidenceReferenceId }, record.evidenceReferences)
    }

    @Test fun fieldReferencesAreDeterministicallyOrdered() {
        assertEquals(listOf("identity.name", "identity.variant"), evidence('a', HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY).copy(fieldReferences = listOf("identity.variant", "identity.name")).let { it.copy(fieldReferences = it.fieldReferences.sorted()) }.fieldReferences)
    }

    @Test fun decisionRecordsUseFrozenSortOrder() {
        val records = listOf(record('b', "reviewer-b"), record('a', "reviewer-a"))
        val result = assertCompleted(execute(batch = batch(records)))
        assertEquals(result.batch.decisionRecords, result.batch.decisionRecords.sortedWith(compareBy({ it.reviewUnit.reviewUnitId }, { it.reviewerRef }, { it.reviewRound }, { it.revision })))
    }

    @Test fun emptyBatchIsRejected() { assertFailure(execute(batch = batch(emptyList())), HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.EMPTY_DECISION_BATCH) }

    @Test fun identicalDecisionIdentityDuplicatesAreRejected() {
        assertFailure(execute(batch = batch(listOf(record('a'), record('a')))), HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.DUPLICATE_DECISION_IDENTITY)
    }

    @Test fun contradictoryDecisionIdentityDuplicatesAreRejected() {
        assertFailure(execute(batch = batch(listOf(record('a'), record('a').copy(decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, reasonCodes = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED), evidenceReferences = rejectEvidence('a'))))), HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.DUPLICATE_DECISION_IDENTITY)
    }

    @Test fun multipleReviewersForOneUnitRemainAllowed() {
        val result = assertCompleted(execute(batch = batch(listOf(record('a', "reviewer-a"), record('b', "reviewer-b")))))
        assertEquals(2, result.batch.counters.uniqueReviewers)
        assertEquals(1, result.batch.counters.uniqueReviewUnits)
    }

    @Test fun multipleRevisionsRemainSeparate() {
        val records = listOf(record('a').copy(revision = 1), record('b').copy(revision = 2))
        assertEquals(2, assertCompleted(execute(batch = batch(records))).batch.counters.decisionRecords)
    }

    @Test fun invalidDecisionRecordIsRejectedBeforeWrite() {
        val invalid = record('a').copy(reviewerRef = "bad reviewer")
        assertFailure(execute(batch = batch(listOf(invalid))), HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INVALID_DECISION_RECORD)
    }

    @Test fun alternativeCanonicalProposalRemainsTyped() {
        val proposal = HimZeroCandidateRecoveryHumanReviewAlternativeCanonicalProposalV1("D4e5F6")
        val proposalEvidence = rejectEvidence('a') + supportEvidence('e')
        val result = assertCompleted(execute(batch = batch(listOf(record('a').copy(decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, reasonCodes = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED), evidenceReferences = proposalEvidence, alternativeCanonicalProposal = proposal)))))
        assertEquals(proposal, result.batch.decisionRecords.single().alternativeCanonicalProposal)
    }

    @Test fun countersAreCompleteAndDerived() {
        val proposal = HimZeroCandidateRecoveryHumanReviewAlternativeCanonicalProposalV1("D4e5F6")
        val records = listOf(
            record('a'),
            record('b', "reviewer-b").copy(
                decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
                reasonCodes = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED),
                evidenceReferences = rejectEvidence('b') + supportEvidence('e'),
                alternativeCanonicalProposal = proposal,
            ),
        )
        val result = assertCompleted(execute(batch = batch(records)))
        assertEquals(2, result.batch.counters.decisionRecords)
        assertEquals(1, result.batch.counters.alternativeCanonicalProposals)
        assertEquals(1, result.batch.counters.decisionBreakdown.single { it.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION }.records)
        assertEquals(1, result.batch.counters.downstreamRouteBreakdown.single { it.route == HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1.POSITIVE_GOLD_CANDIDATE_AFTER_INDEPENDENT_VALIDATION }.records)
    }

    @Test fun persistedCounterMismatchIsDetectedOnReload() {
        val altered = batch().copy(counters = HimZeroCandidateRecoveryHumanReviewPersistenceCountersV1(0, 0, 0, emptyList(), emptyList(), 0, 0, 0))
        assertFailure(execute(batch = altered), HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INVALID_COUNTERS)
    }

    @Test fun bindingDigestIsDeterministic() { assertEquals(HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(binding()), HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(binding())) }

    @Test fun bindingDigestManipulationIsDetected() { assertFailure(execute(batch = batch().copy(bindingDigest = "f".repeat(64))), HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INPUT_BINDING_DIGEST_MISMATCH) }

    @Test fun batchLogicalDigestIsDeterministic() {
        val first = batch().batchLogicalDigest
        val second = batch().batchLogicalDigest
        assertEquals(first, second)
    }

    @Test fun semanticChangeChangesBatchLogicalDigest() {
        val changed = batch().copy(batchId = "other-batch", batchLogicalDigest = "")
        assertNotEquals(batch().batchLogicalDigest, HimZeroCandidateRecoveryHumanReviewPersistenceV1.batchLogicalDigest(changed))
    }

    @Test fun batchLogicalDigestManipulationIsDetected() { assertFailure(execute(batch = batch().copy(batchLogicalDigest = "f".repeat(64))), HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.BATCH_LOGICAL_DIGEST_MISMATCH) }

    @Test fun firstPersistenceCreatesExactlyThreeArtifacts() = withTempRoot { root ->
        val result = assertCompleted(execute(root, batch = batch()))
        assertEquals(HimZeroCandidateRecoveryHumanReviewPersistenceStatusV1.CREATED, result.persistenceStatus)
        assertEquals(3, root.walkTopDown().count { it.isFile })
    }

    @Test fun durableBatchIsOnlyUnderTemporaryDataRoot() = withTempRoot { root ->
        assertCompleted(execute(root, batch = batch()))
        assertTrue(root.resolve("data").resolve("batch-01/review-decisions.v1.json").isFile)
    }

    @Test fun reportsAreOnlyUnderTemporaryBuildReportRoot() = withTempRoot { root ->
        assertCompleted(execute(root, batch = batch()))
        assertTrue(root.resolve("build/batches/batch-01/persistence-report.v1.json").isFile)
        assertTrue(root.resolve("build/batches/batch-01/persistence-report.v1.txt").isFile)
    }

    @Test fun firstPersistenceReturnsCreated() = withTempRoot { root -> assertEquals(HimZeroCandidateRecoveryHumanReviewPersistenceStatusV1.CREATED, assertCompleted(execute(root, batch = batch())).persistenceStatus) }

    @Test fun reloadValidatesFullyPersistedBatch() = withTempRoot { root ->
        val first = assertCompleted(execute(root, batch = batch()))
        assertEquals(first.batch, HimZeroCandidateRecoveryHumanReviewPersistenceV1.readBatch(root.resolve("data/batch-01/review-decisions.v1.json")))
    }

    @Test fun secondIdenticalExecutionReturnsAlreadyPresent() = withTempRoot { root ->
        assertCompleted(execute(root, batch = batch()))
        assertEquals(HimZeroCandidateRecoveryHumanReviewPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL, assertCompleted(execute(root, batch = batch())).persistenceStatus)
    }

    @Test fun secondExecutionLeavesAllBytesIdentical() = withTempRoot { root ->
        assertCompleted(execute(root, batch = batch()))
        val before = root.walkTopDown().filter { it.isFile }.associate { it.relativeTo(root).path to it.readBytes().toList() }
        assertCompleted(execute(root, batch = batch()))
        val after = root.walkTopDown().filter { it.isFile }.associate { it.relativeTo(root).path to it.readBytes().toList() }
        assertEquals(before, after)
    }

    @Test fun durableBatchRemainsByteIdentical() = withTempRoot { root ->
        assertCompleted(execute(root, batch = batch()))
        val file = root.resolve("data/batch-01/review-decisions.v1.json")
        val before = file.readBytes()
        assertCompleted(execute(root, batch = batch()))
        assertTrue(before.contentEquals(file.readBytes()))
    }

    @Test fun jsonReportRemainsByteIdentical() = withTempRoot { root ->
        assertCompleted(execute(root, batch = batch()))
        val file = root.resolve("build/batches/batch-01/persistence-report.v1.json")
        val before = file.readBytes()
        assertCompleted(execute(root, batch = batch()))
        assertTrue(before.contentEquals(file.readBytes()))
    }

    @Test fun textReportRemainsByteIdentical() = withTempRoot { root ->
        assertCompleted(execute(root, batch = batch()))
        val file = root.resolve("build/batches/batch-01/persistence-report.v1.txt")
        val before = file.readBytes()
        assertCompleted(execute(root, batch = batch()))
        assertTrue(before.contentEquals(file.readBytes()))
    }

    @Test fun existingBatchWithMissingReportsRegeneratesOnlyReports() = withTempRoot { root ->
        assertCompleted(execute(root, batch = batch()))
        val batchFile = root.resolve("data/batch-01/review-decisions.v1.json")
        val before = batchFile.readBytes()
        root.resolve("build/batches/batch-01").deleteRecursively()
        val result = assertCompleted(execute(root, batch = batch()))
        assertEquals(HimZeroCandidateRecoveryHumanReviewPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL, result.persistenceStatus)
        assertTrue(before.contentEquals(batchFile.readBytes()))
    }

    @Test fun onlyOneReportIsRejectedAsPartialState() = withTempRoot { root ->
        assertCompleted(execute(root, batch = batch()))
        root.resolve("build/batches/batch-01/persistence-report.v1.txt").delete()
        assertFailure(execute(root, batch = batch()), HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.PARTIAL_ARTIFACT_STATE)
    }

    @Test fun reportsWithoutDurableBatchAreRejected() = withTempRoot { root ->
        assertCompleted(execute(root, batch = batch()))
        root.resolve("data/batch-01").deleteRecursively()
        assertFailure(execute(root, batch = batch()), HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.PARTIAL_ARTIFACT_STATE)
    }

    @Test fun divergentExistingBatchIsNotOverwritten() = withTempRoot { root ->
        assertCompleted(execute(root, batch = batch()))
        val file = root.resolve("data/batch-01/review-decisions.v1.json")
        val before = file.readBytes()
        file.writeText("different")
        assertFailure(execute(root, batch = batch()), HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.EXISTING_BATCH_CONFLICT)
        assertTrue("different".toByteArray().contentEquals(file.readBytes()))
        assertFalse(before.contentEquals(file.readBytes()))
    }

    @Test fun divergentExistingReportIsNotOverwritten() = withTempRoot { root ->
        assertCompleted(execute(root, batch = batch()))
        val file = root.resolve("build/batches/batch-01/persistence-report.v1.json")
        file.writeText("different")
        assertFailure(execute(root, batch = batch()), HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.DESERIALIZATION_FAILED)
        assertEquals("different", file.readText())
    }

    @Test fun batchIdCollisionIsTyped() = withTempRoot { root ->
        assertCompleted(execute(root, batch = batch()))
        val conflicting = batch(listOf(record('b', "reviewer-b")))
        assertFailure(execute(root, batch = conflicting), HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.EXISTING_BATCH_CONFLICT)
    }

    @Test fun controlledWriteFailureLeavesNoPublishedTargets() = withTempRoot { root ->
        val blocked = root.resolve("blocked")
        blocked.writeText("file")
        val result = execute(blocked, batch = batch())
        assertTrue(result is HimZeroCandidateRecoveryHumanReviewPersistenceResultV1.Failed)
        assertEquals(0, root.listFiles()?.count { it.name.contains("review-decisions") } ?: 0)
    }

    @Test fun temporaryFilesAreRemovedAfterControlledFailure() = withTempRoot { root ->
        val blocked = root.resolve("blocked")
        blocked.writeText("file")
        execute(blocked, batch = batch())
        assertTrue(root.walkTopDown().none { it.name.endsWith(".tmp") })
    }

    @Test fun persistenceReportContainsOnlyPersistenceMetadata() = withTempRoot { root ->
        assertCompleted(execute(root, batch = batch()))
        val text = root.resolve("build/batches/batch-01/persistence-report.v1.json").readText()
        assertFalse(text.contains("reviewerRef"))
        assertFalse(text.contains("evidenceReference"))
        assertContains(text, "persistenceStatus")
    }

    @Test fun persistenceReportHasNoApprovalGoldOrAuthoritySemantics() = withTempRoot { root ->
        assertCompleted(execute(root, batch = batch()))
        val text = root.resolve("build/batches/batch-01/persistence-report.v1.txt").readText().lowercase()
        assertFalse(text.contains("approvalstate") || text.contains("goldstate") || text.contains("authoritystate") || text.contains("mutationstate"))
    }

    @Test fun persistenceHasNoClockOrTimestampField() {
        assertFalse(HimZeroCandidateRecoveryHumanReviewPersistenceReportV1::class.java.declaredFields.any { it.name.contains("time", true) })
    }

    @Test fun persistenceHasNoOperationalDependency() {
        val names = HimZeroCandidateRecoveryHumanReviewPersistenceV1::class.java.declaredMethods.map { it.name }
        assertTrue(names.none { it.contains("network", true) || it.contains("openai", true) || it.contains("search", true) || it.contains("fetch", true) || it.contains("sqlite", true) })
    }

    @Test fun safeDiagnosticsContainNoPathsReviewersEvidenceOrExceptions() {
        val failure = assertFailure(execute(batch = batch().copy(bindingDigest = "f".repeat(64))), HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INPUT_BINDING_DIGEST_MISMATCH)
        assertFalse(failure.safeContext.contains('/') || failure.safeContext.contains('\\') || failure.safeContext.contains("Exception", true))
    }

    @Test fun publicRequestAndResultFieldsAreFrozen() {
        assertEquals(setOf("durableDecisionBatchRoot", "derivedReportRoot", "batch"), HimZeroCandidateRecoveryHumanReviewPersistenceRequestV1::class.java.declaredFields.filterNot { it.name.startsWith("$") }.map { it.name }.toSet())
        assertEquals(setOf("persistenceStatus", "batch", "report"), HimZeroCandidateRecoveryHumanReviewPersistenceResultV1.Completed::class.java.declaredFields.filterNot { it.name.startsWith("$") }.map { it.name }.toSet())
    }

    @Test fun realCorpusIsNeverOpenedByHermeticPersistence() {
        assertFalse(File(HimZeroCandidateRecoveryHumanReviewContractV1.DURABLE_DECISION_BATCH_ROOT).resolve("batch-01").exists())
        assertFalse(File(HimZeroCandidateRecoveryHumanReviewContractV1.DERIVED_REPORT_ROOT).resolve("batches/batch-01").exists())
    }

    @Test fun hermeticPersistenceUsesOnlyTemporaryRoots() = withTempRoot { root ->
        assertCompleted(execute(root, batch = batch()))
        assertTrue(root.resolve("data").exists())
        assertTrue(root.resolve("build").exists())
    }

    @Test fun allPersistenceStatusesAreClosed() { assertEquals(listOf("CREATED", "ALREADY_PRESENT_IDENTICAL"), HimZeroCandidateRecoveryHumanReviewPersistenceStatusV1.entries.map { it.name }) }

    @Test fun allRequiredFailureReasonsAreTyped() {
        assertTrue(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.entries.map { it.name }.containsAll(listOf("INVALID_BATCH_ID", "PARTIAL_ARTIFACT_STATE", "WRITE_FAILED", "RELOAD_MISMATCH")))
    }

    @Test fun noDecisionIsPublishedByPersistence() {
        assertTrue(HimZeroCandidateRecoveryHumanReviewPersistenceV1.CONTRACT_ID.contains("PERSISTENCE"))
        assertFalse(HimZeroCandidateRecoveryHumanReviewPersistenceV1.CONTRACT_ID.contains("APPROVAL"))
    }

    @Test fun noRealDecisionBatchIsCreated() { assertFalse(File(HimZeroCandidateRecoveryHumanReviewContractV1.DURABLE_DECISION_BATCH_ROOT).listFiles()?.any { it.name == "batch-01" } == true) }

    private fun assertCompleted(result: HimZeroCandidateRecoveryHumanReviewPersistenceResultV1): HimZeroCandidateRecoveryHumanReviewPersistenceResultV1.Completed {
        return when (result) {
            is HimZeroCandidateRecoveryHumanReviewPersistenceResultV1.Completed -> result
            is HimZeroCandidateRecoveryHumanReviewPersistenceResultV1.Failed ->
                error("${result.reason} ${result.safeContext}")
        }
    }

    private fun assertFailure(
        result: HimZeroCandidateRecoveryHumanReviewPersistenceResultV1,
        reason: HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1,
    ): HimZeroCandidateRecoveryHumanReviewPersistenceResultV1.Failed {
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewPersistenceResultV1.Failed>(result)
        assertEquals(reason, failure.reason)
        return failure
    }

    private fun execute(
        root: File = Files.createTempDirectory("him-human-review-persistence-").toFile().also { it.deleteOnExit() },
        batchId: String = "batch-01",
        batch: HimZeroCandidateRecoveryHumanReviewDecisionBatchV1 = batch(batchId = batchId),
    ): HimZeroCandidateRecoveryHumanReviewPersistenceResultV1 {
        val durable = root.resolve("data")
        val derived = root.resolve("build")
        return HimZeroCandidateRecoveryHumanReviewPersistenceV1.execute(
            HimZeroCandidateRecoveryHumanReviewPersistenceRequestV1(durable, derived, batch),
        )
    }

    private fun withTempRoot(block: (File) -> Unit) {
        val root = Files.createTempDirectory("him-human-review-persistence-").toFile()
        try { block(root) } finally { root.deleteRecursively() }
    }

    private fun batch(
        records: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1> = listOf(record('a')),
        batchId: String = "batch-01",
    ): HimZeroCandidateRecoveryHumanReviewDecisionBatchV1 {
        val unsigned = HimZeroCandidateRecoveryHumanReviewDecisionBatchV1(
            HimZeroCandidateRecoveryHumanReviewPersistenceV1.CONTRACT_ID,
            HimZeroCandidateRecoveryHumanReviewPersistenceV1.VERSION,
            batchId,
            binding(),
            records,
            HimZeroCandidateRecoveryHumanReviewPersistenceV1.deriveCounters(records),
            HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(binding()),
            "",
        )
        val canonicalForDigest = unsigned.copy(
            decisionRecords = records.sortedWith(compareBy({ it.reviewUnit.reviewUnitId }, { it.reviewerRef }, { it.reviewRound }, { it.revision })),
        )
        return unsigned.copy(batchLogicalDigest = HimZeroCandidateRecoveryHumanReviewPersistenceV1.batchLogicalDigest(canonicalForDigest))
    }

    private fun binding(): HimZeroCandidateRecoveryHumanReviewInputBindingV1 {
        val cause = HimZeroCandidateCauseAnalysisFileBindingV1("cause/analysis.json", 1, "a".repeat(64), "b".repeat(64))
        val existingBase = HimZeroCandidateRecoveryReviewCorpusInputBindingV1(
            cause,
            corpusBinding("catalog.json", 'c'),
            corpusBinding("authority.json", 'd'),
            "b".repeat(64),
            "a".repeat(40),
            "",
        )
        val existing = existingBase.copy(
            bindingDigest = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.bindingDigest(existingBase),
        )
        val base = HimZeroCandidateRecoveryHumanReviewInputBindingV1(
            humanBinding("review-corpus.json", 'e'),
            "e".repeat(64),
            "a".repeat(64),
            "b".repeat(64),
            existing,
            humanBinding("registry.json", 'c'),
            HimZeroCandidateRecoveryHumanReviewContractV1.VERSION,
            HimZeroCandidateRecoveryHumanReviewContractV1.VERSION,
            "a".repeat(40),
            "",
        )
        return base.copy(bindingDigest = HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(base))
    }

    private fun corpusBinding(path: String, seed: Char) = HimZeroCandidateRecoveryReviewCorpusFileBindingV1(path, 1, seed.toString().repeat(64), seed.toString().repeat(64))

    private fun humanBinding(path: String, seed: Char) = HimZeroCandidateRecoveryHumanReviewFileBindingV1(path, 1, seed.toString().repeat(64), seed.toString().repeat(64))

    private fun record(seed: Char, reviewer: String = "reviewer-1") = HimZeroCandidateRecoveryHumanReviewDecisionRecordV1(
        HimZeroCandidateRecoveryHumanReviewContractV1.VERSION,
        HimZeroCandidateRecoveryHumanReviewReviewUnitV1("a".repeat(64), "A1b2C3"),
        reviewer,
        1,
        if (seed == 'a') 1 else 2,
        HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
        listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED),
        confirmEvidence(seed),
    )

    private fun confirmEvidence(seed: Char) = listOf(
        evidence(seed, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION),
        evidence(if (seed == 'a') 'b' else 'c', HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION),
    )

    private fun rejectEvidence(seed: Char) = listOf(
        evidence(seed, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION),
        evidence(if (seed == 'a') 'b' else 'c', HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION),
    )

    private fun supportEvidence(seed: Char) = listOf(
        evidence(seed, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION),
        evidence(if (seed == 'e') 'f' else 'g', HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION),
    )

    private fun evidence(
        seed: Char,
        kind: HimZeroCandidateRecoveryHumanReviewEvidenceKindV1,
        position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1,
    ) = HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1(
        seed.toString().repeat(64),
        kind,
        HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
        position,
        "data/source/artifact.json",
        "record:$seed",
        "d".repeat(64),
        null,
        listOf("identity.name"),
    )
}
