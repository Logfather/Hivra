package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.io.path.deleteRecursively
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1 as BindingContract
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingPersistenceV1 as Persistence

@OptIn(kotlin.io.path.ExperimentalPathApi::class)
class RunHimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingPersistenceV1Test {
    @Test
    fun contractIdentityAndPersistenceStateAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_TRAINING_EXAMPLE_REFERENCE_BINDING_PERSISTENCE_V1",
            Persistence.CONTRACT_ID,
        )
        assertEquals("1", Persistence.VERSION)
        assertEquals("POSITIVE_TRAINING_EXAMPLE_REFERENCE_BINDING_PERSISTED", Persistence.STATE)
    }

    @Test
    fun validBoundDecisionPersists() = withRoot { root ->
        val result = completed(execute(root))

        assertEquals(Persistence.PersistenceStatus.CREATED, result.status)
        assertEquals(BindingContract.BindingState.BOUND, result.record.state)
        assertTrue(Persistence.pathFor(Fixture.bound().bindingKey, root).isFile)
    }

    @Test
    fun exactReadByBindingKeySucceeds() = withRoot { root ->
        val decision = Fixture.bound()
        val persisted = completed(execute(root, decision)).record

        assertEquals(persisted, Persistence.readByNegativeSupervisionRecordId(decision.bindingKey, root))
    }

    @Test
    fun persistedRecordPreservesRequiredFields() = withRoot { root ->
        val decision = Fixture.bound()
        val record = completed(execute(root, decision)).record

        assertEquals(Persistence.CONTRACT_ID, record.contractId)
        assertEquals(Persistence.VERSION, record.version)
        assertEquals(BindingContract.BindingState.BOUND, record.state)
        assertEquals(decision.bindingKey, record.bindingKey)
        assertEquals(decision.negativeSupervisionRecordId, record.negativeSupervisionRecordId)
        assertEquals(decision.materializationDecisionId, record.materializationDecisionId)
        assertEquals(decision.trainingExampleReference, record.trainingExampleReference)
        assertEquals(decision.bindingDecisionId, record.bindingDecisionId)
        assertTrue(record.reasons.isEmpty())
        assertEquals(record.logicalDigest, Persistence.recordLogicalDigest(record.copy(logicalDigest = "")))
    }

    @Test
    fun repeatedIdenticalPersistReturnsAlreadyPresentIdentical() = withRoot { root ->
        val decision = Fixture.bound()

        assertEquals(Persistence.PersistenceStatus.CREATED, completed(execute(root, decision)).status)
        assertEquals(Persistence.PersistenceStatus.ALREADY_PRESENT_IDENTICAL, completed(execute(root, decision)).status)
    }

    @Test
    fun repeatedIdenticalPersistLeavesBytesUnchanged() = withRoot { root ->
        val decision = Fixture.bound()
        val first = completed(execute(root, decision))
        val before = Persistence.pathFor(decision.bindingKey, root).readBytes()

        completed(execute(root, decision))

        assertContentEquals(before, Persistence.pathFor(decision.bindingKey, root).readBytes())
        assertEquals(first.sha256, completed(execute(root, decision)).sha256)
    }

    @Test
    fun sameKeyWithDifferentReferenceConflicts() = withRoot { root ->
        val first = Fixture.bound()
        val differentReference = first.copy(trainingExampleReference = Fixture.reference("other-reference"))

        completed(execute(root, first))

        assertReason(execute(root, differentReference), Persistence.FailureReason.EXISTING_ARTIFACT_CONFLICT)
    }

    @Test
    fun sameKeyAndReferenceWithDifferentMaterializationIdConflicts() = withRoot { root ->
        val first = Fixture.bound()
        val differentMaterialization = first.copy(materializationDecisionId = Fixture.sha("other-materialization"))

        completed(execute(root, first))

        assertReason(execute(root, differentMaterialization), Persistence.FailureReason.EXISTING_ARTIFACT_CONFLICT)
    }

    @Test
    fun inconsistentBindingDecisionIdConflicts() = withRoot { root ->
        val first = Fixture.bound()
        val inconsistent = first.copy(bindingDecisionId = Fixture.sha("other-binding-decision"))

        completed(execute(root, first))

        assertReason(execute(root, inconsistent), Persistence.FailureReason.EXISTING_ARTIFACT_CONFLICT)
    }

    @Test
    fun malformedExistingArtifactFailsClosed() = withRoot { root ->
        val decision = Fixture.bound()
        writeTarget(root, decision, "not-json\n")

        assertReason(execute(root, decision), Persistence.FailureReason.MALFORMED_ARTIFACT)
    }

    @Test
    fun truncatedExistingArtifactFailsClosed() = withRoot { root ->
        val decision = Fixture.bound()
        val bytes = Persistence.serializeRecord(Persistence.recordFrom(decision))
        writeTarget(root, decision, bytes.copyOf(bytes.size - 1))

        assertReason(execute(root, decision), Persistence.FailureReason.MALFORMED_ARTIFACT)
    }

    @Test
    fun wrongContractIsRejectedOnReload() = withRoot { root ->
        val decision = Fixture.bound()
        val payload = validJson(decision).replace(Persistence.CONTRACT_ID, "WRONG_CONTRACT")
        writeTarget(root, decision, payload)

        assertReason(execute(root, decision), Persistence.FailureReason.INVALID_CONTRACT)
    }

    @Test
    fun wrongVersionIsRejectedOnReload() = withRoot { root ->
        val decision = Fixture.bound()
        val payload = validJson(decision).replace("\"version\":\"1\"", "\"version\":\"2\"")
        writeTarget(root, decision, payload)

        assertReason(execute(root, decision), Persistence.FailureReason.INVALID_VERSION)
    }

    @Test
    fun wrongStateIsRejectedOnReload() = withRoot { root ->
        val decision = Fixture.bound()
        val payload = validJson(decision).replace("\"state\":\"BOUND\"", "\"state\":\"NOT_YET_BINDABLE\"")
        writeTarget(root, decision, payload)

        assertReason(execute(root, decision), Persistence.FailureReason.INVALID_STATE)
    }

    @Test
    fun nonEmptyReasonsAreRejectedOnReload() = withRoot { root ->
        val decision = Fixture.bound()
        val payload = validJson(decision).replace("\"reasons\":[]", "\"reasons\":[\"INVALID_BINDING_CONTEXT\"]")
        writeTarget(root, decision, payload)

        assertReason(execute(root, decision), Persistence.FailureReason.NON_EMPTY_REASONS)
    }

    @Test
    fun invalidBindingKeyIsRejectedBeforeWrite() = withRoot { root ->
        val broken = Fixture.bound().copy(bindingKey = "bad", negativeSupervisionRecordId = "bad")

        assertReason(execute(root, broken), Persistence.FailureReason.INVALID_BINDING_KEY)
        assertTrue(root.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun negativeSupervisionRecordIdMismatchIsRejected() = withRoot { root ->
        val broken = Fixture.bound().copy(negativeSupervisionRecordId = Fixture.sha("other-record"))

        assertReason(execute(root, broken), Persistence.FailureReason.BINDING_IDENTITY_MISMATCH)
    }

    @Test
    fun missingMaterializationIdIsRejected() = withRoot { root ->
        val broken = Fixture.bound().copy(materializationDecisionId = null)

        assertReason(execute(root, broken), Persistence.FailureReason.MISSING_MATERIALIZATION_DECISION_ID)
    }

    @Test
    fun malformedMaterializationIdIsRejected() = withRoot { root ->
        val broken = Fixture.bound().copy(materializationDecisionId = "bad")

        assertReason(execute(root, broken), Persistence.FailureReason.INVALID_MATERIALIZATION_DECISION_ID)
    }

    @Test
    fun missingTrainingExampleReferenceIsRejected() = withRoot { root ->
        val broken = Fixture.bound().copy(trainingExampleReference = null)

        assertReason(execute(root, broken), Persistence.FailureReason.MISSING_TRAINING_EXAMPLE_REFERENCE)
    }

    @Test
    fun malformedTrainingExampleReferenceIsRejectedOnReload() = withRoot { root ->
        val decision = Fixture.bound()
        val payload = validJson(decision).replace(decision.trainingExampleReference!!.value, "example:v1:bad")
        writeTarget(root, decision, payload)

        assertReason(execute(root, decision), Persistence.FailureReason.INVALID_TRAINING_EXAMPLE_REFERENCE)
    }

    @Test
    fun logicalDigestMismatchIsRejectedOnReload() = withRoot { root ->
        val decision = Fixture.bound()
        val record = Persistence.recordFrom(decision)
        val payload = validJson(decision).replace(record.logicalDigest, "0".repeat(64))
        writeTarget(root, decision, payload)

        assertReason(execute(root, decision), Persistence.FailureReason.LOGICAL_DIGEST_MISMATCH)
    }

    @Test
    fun duplicateJsonFieldIsRejected() = withRoot { root ->
        val decision = Fixture.bound()
        val payload = validJson(decision).replace(
            "\"version\"",
            "\"contractId\":\"${Persistence.CONTRACT_ID}\",\"version\"",
        )
        writeTarget(root, decision, payload)

        assertReason(execute(root, decision), Persistence.FailureReason.MALFORMED_ARTIFACT)
    }

    @Test
    fun unknownJsonFieldIsRejected() = withRoot { root ->
        val decision = Fixture.bound()
        val payload = validJson(decision).replaceFirst("{", "{\"unknown\":true,")
        writeTarget(root, decision, payload)

        assertReason(execute(root, decision), Persistence.FailureReason.MALFORMED_ARTIFACT)
    }

    @Test
    fun serializedWireHasExactlyOneFinalLf() {
        val bytes = Persistence.serializeRecord(Persistence.recordFrom(Fixture.bound()))

        assertTrue(bytes.isNotEmpty())
        assertEquals('\n'.code.toByte(), bytes.last())
        assertTrue(bytes.size == 1 || bytes[bytes.size - 2] != '\n'.code.toByte())
    }

    @Test
    fun independentRunsProduceIdenticalRecordAndBytes() = withRoots { firstRoot, secondRoot ->
        val decision = Fixture.bound()
        val first = completed(execute(firstRoot, decision))
        val second = completed(execute(secondRoot, decision))

        assertEquals(first.record, second.record)
        assertEquals(first.sha256, second.sha256)
        assertContentEquals(
            Persistence.pathFor(decision.bindingKey, firstRoot).readBytes(),
            Persistence.pathFor(decision.bindingKey, secondRoot).readBytes(),
        )
    }

    @Test
    fun differentBindingKeysMayShareOneTrainingExampleReference() = withRoot { root ->
        val reference = Fixture.reference("shared-reference")
        val first = Fixture.bound(1).copy(trainingExampleReference = reference)
        val second = Fixture.bound(2).copy(trainingExampleReference = reference)

        assertEquals(Persistence.PersistenceStatus.CREATED, completed(execute(root, first)).status)
        assertEquals(Persistence.PersistenceStatus.CREATED, completed(execute(root, second)).status)
        assertEquals(
            completed(execute(root, first)).record.trainingExampleReference,
            completed(execute(root, second)).record.trainingExampleReference,
        )
        assertTrue(Persistence.pathFor(first.bindingKey, root) != Persistence.pathFor(second.bindingKey, root))
    }

    @Test
    fun readerUsesDirectBindingKeyPath() = withRoot { root ->
        val first = Fixture.bound(1)
        val second = Fixture.bound(2)
        completed(execute(root, first))
        completed(execute(root, second))

        assertEquals(first.bindingKey + Persistence.DURABLE_FILE_SUFFIX, Persistence.pathFor(first.bindingKey, root).name)
        assertEquals(first.bindingKey, Persistence.readByNegativeSupervisionRecordId(first.bindingKey, root).bindingKey)
    }

    @Test
    fun nonBoundDecisionCannotBePersisted() = withRoot { root ->
        val nonBound = Fixture.bound().copy(
            state = BindingContract.BindingState.NOT_YET_BINDABLE,
            reasons = listOf(BindingContract.FailureReason.MATERIALIZATION_NOT_PROJECTED),
            materializationDecisionId = null,
            trainingExampleReference = null,
        )

        assertReason(execute(root, nonBound), Persistence.FailureReason.NON_BOUND_DECISION)
        assertTrue(root.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun rootSymlinkFailsClosed() {
        val outer = Files.createTempDirectory("him-binding-root-")
        val real = Files.createDirectory(outer.resolve("real"))
        val link = outer.resolve("link")
        try {
            Files.createSymbolicLink(link, real)
        } catch (_: UnsupportedOperationException) {
            outer.deleteRecursively()
            return
        } catch (_: java.io.IOException) {
            outer.deleteRecursively()
            return
        }

        try {
            assertReason(execute(link.toFile(), Fixture.bound()), Persistence.FailureReason.UNSAFE_DURABLE_ROOT)
        } finally {
            outer.deleteRecursively()
        }
    }

    @Test
    fun targetSymlinkFailsClosed() = withRoot { root ->
        val decision = Fixture.bound()
        val target = Persistence.pathFor(decision.bindingKey, root).toPath()
        val outside = Files.createTempFile("him-binding-outside-", ".json")
        try {
            Files.createSymbolicLink(target, outside)
        } catch (_: UnsupportedOperationException) {
            Files.deleteIfExists(outside)
            return@withRoot
        } catch (_: java.io.IOException) {
            Files.deleteIfExists(outside)
            return@withRoot
        }

        try {
            assertReason(execute(root, decision), Persistence.FailureReason.UNSAFE_TARGET_PATH)
        } finally {
            Files.deleteIfExists(target)
            Files.deleteIfExists(outside)
        }
    }

    @Test
    fun preexistingTemporaryFileIsRejectedAndNotOverwritten() = withRoot { root ->
        val decision = Fixture.bound()
        val target = Persistence.pathFor(decision.bindingKey, root)
        target.parentFile.mkdirs()
        val temporary = target.parentFile.resolve(".${target.name}.tmp")
        val before = byteArrayOf(7, 8, 9)
        temporary.writeBytes(before)

        assertReason(execute(root, decision), Persistence.FailureReason.PARTIAL_ARTIFACT)
        assertContentEquals(before, temporary.readBytes())
    }

    @Test
    fun successfulPublicationLeavesNoTemporaryFile() = withRoot { root ->
        val decision = Fixture.bound()
        completed(execute(root, decision))

        val target = Persistence.pathFor(decision.bindingKey, root)
        assertFalse(target.parentFile.resolve(".${target.name}.tmp").exists())
    }

    @Test
    fun readOfMissingBindingFailsClosed() = withRoot { root ->
        assertFailsWith<IllegalArgumentException> {
            Persistence.readByNegativeSupervisionRecordId(Fixture.bound().bindingKey, root)
        }
    }

    @Test
    fun failureSurfaceUsesOnlySafeContext() = withRoot { root ->
        val failure = assertIs<Persistence.Result.Failed>(execute(root, Fixture.bound().copy(bindingKey = "bad")))

        assertEquals(Persistence.FailureReason.INVALID_BINDING_KEY, failure.reason)
        assertFalse(failure.safeContext.contains('/'))
        assertFalse(failure.safeContext.contains("exception", ignoreCase = true))
    }

    private fun execute(
        root: java.io.File,
        decision: BindingContract.BindingDecision = Fixture.bound(),
    ): Persistence.Result = Persistence.execute(Persistence.Request(decision, root))

    private fun completed(result: Persistence.Result): Persistence.Result.Completed =
        assertIs<Persistence.Result.Completed>(result)

    private fun assertReason(result: Persistence.Result, reason: Persistence.FailureReason) {
        assertEquals(reason, assertIs<Persistence.Result.Failed>(result).reason)
    }

    private fun validJson(decision: BindingContract.BindingDecision): String =
        String(Persistence.serializeRecord(Persistence.recordFrom(decision)), StandardCharsets.UTF_8)

    private fun writeTarget(
        root: java.io.File,
        decision: BindingContract.BindingDecision,
        payload: String,
    ) {
        val target = Persistence.pathFor(decision.bindingKey, root)
        target.parentFile.mkdirs()
        target.writeText(payload, StandardCharsets.UTF_8)
    }

    private fun writeTarget(
        root: java.io.File,
        decision: BindingContract.BindingDecision,
        payload: ByteArray,
    ) {
        val target = Persistence.pathFor(decision.bindingKey, root)
        target.parentFile.mkdirs()
        target.writeBytes(payload)
    }

    private fun withRoot(block: (java.io.File) -> Unit) {
        val root = Files.createTempDirectory("him-binding-persistence-")
        try {
            block(root.toFile())
        } finally {
            root.deleteRecursively()
        }
    }

    private fun withRoots(block: (java.io.File, java.io.File) -> Unit) {
        val first = Files.createTempDirectory("him-binding-persistence-first-")
        val second = Files.createTempDirectory("him-binding-persistence-second-")
        try {
            block(first.toFile(), second.toFile())
        } finally {
            first.deleteRecursively()
            second.deleteRecursively()
        }
    }

    private object Fixture {
        fun bound(index: Int = 1): BindingContract.BindingDecision {
            val bindingKey = sha("binding-key-$index")
            return BindingContract.BindingDecision(
                bindingDecisionId = sha("binding-decision-$index"),
                bindingKey = bindingKey,
                negativeSupervisionRecordId = bindingKey,
                materializationDecisionId = sha("materialization-$index"),
                trainingExampleReference = reference("example-$index"),
                state = BindingContract.BindingState.BOUND,
                reasons = emptyList(),
            )
        }

        fun reference(seed: String): HimTrainingExampleReference =
            HimTrainingExampleReference("example:v1:${sha(seed)}")

        fun sha(value: String): String =
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(StandardCharsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
