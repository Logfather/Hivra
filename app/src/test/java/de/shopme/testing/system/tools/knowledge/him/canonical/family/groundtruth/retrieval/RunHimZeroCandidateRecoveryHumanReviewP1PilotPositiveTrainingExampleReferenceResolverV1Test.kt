package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1 as BindingContract
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingPersistenceV1 as BindingPersistence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceResolverV1 as Resolver
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.training.corpus.HimPositiveTrainingExamplePersistenceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceResolverV1Test {
    @Test
    fun validPersistedBindingAndPositiveExampleResolve() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persist(fixture, bindingRoot, exampleRoot)

        val resolved = completed(resolve(fixture, bindingRoot, exampleRoot))
        assertEquals(fixture.bindingRecord, resolved.bindingRecord)
        assertEquals(fixture.example, resolved.trainingExample)
    }

    @Test
    fun successReturnsExactBindingRecord() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persist(fixture, bindingRoot, exampleRoot)

        assertEquals(fixture.bindingRecord, completed(resolve(fixture, bindingRoot, exampleRoot)).bindingRecord)
    }

    @Test
    fun successReturnsExactTrainingExample() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persist(fixture, bindingRoot, exampleRoot)

        assertEquals(fixture.example, completed(resolve(fixture, bindingRoot, exampleRoot)).trainingExample)
    }

    @Test
    fun trainingExampleReferenceIsTakenFromBindingRecord() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persist(fixture, bindingRoot, exampleRoot)

        val resolved = completed(resolve(fixture, bindingRoot, exampleRoot))
        assertEquals(fixture.bindingRecord.trainingExampleReference, resolved.trainingExample.exampleReference)
    }

    @Test
    fun repeatedResolutionIsDeterministic() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persist(fixture, bindingRoot, exampleRoot)

        val first = completed(resolve(fixture, bindingRoot, exampleRoot))
        val second = completed(resolve(fixture, bindingRoot, exampleRoot))
        assertEquals(first, second)
    }

    @Test
    fun missingBindingProducesTypedFailure() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()

        assertFailure(
            resolve(fixture, bindingRoot, exampleRoot),
            Resolver.FailureReason.BINDING_NOT_FOUND,
        )
    }

    @Test
    fun malformedBindingProducesTypedFailure() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        writeBinding(bindingRoot, fixture, "not-json\n".toByteArray(StandardCharsets.UTF_8))

        assertFailure(
            resolve(fixture, bindingRoot, exampleRoot),
            Resolver.FailureReason.INVALID_PERSISTED_BINDING,
        )
    }

    @Test
    fun truncatedBindingProducesTypedFailure() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        val bytes = BindingPersistence.serializeRecord(fixture.bindingRecord)
        writeBinding(bindingRoot, fixture, bytes.copyOf(bytes.size - 2))

        assertFailure(
            resolve(fixture, bindingRoot, exampleRoot),
            Resolver.FailureReason.INVALID_PERSISTED_BINDING,
        )
    }

    @Test
    fun bindingDigestCorruptionProducesTypedFailure() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        val valid = String(BindingPersistence.serializeRecord(fixture.bindingRecord), StandardCharsets.UTF_8)
        val broken = valid.replace(
            "\"logicalDigest\":\"${fixture.bindingRecord.logicalDigest}\"",
            "\"logicalDigest\":\"${"0".repeat(64)}\"",
        )
        writeBinding(bindingRoot, fixture, broken.toByteArray(StandardCharsets.UTF_8))

        assertFailure(
            resolve(fixture, bindingRoot, exampleRoot),
            Resolver.FailureReason.INVALID_PERSISTED_BINDING,
        )
    }

    @Test
    fun otherBindingIntegrityFailureMapsSafely() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        val valid = String(BindingPersistence.serializeRecord(fixture.bindingRecord), StandardCharsets.UTF_8)
        val broken = valid.replace(
            "\"contractId\":\"${BindingPersistence.CONTRACT_ID}\"",
            "\"contractId\":\"wrong\"",
        )
        writeBinding(bindingRoot, fixture, broken.toByteArray(StandardCharsets.UTF_8))

        assertFailure(
            resolve(fixture, bindingRoot, exampleRoot),
            Resolver.FailureReason.INVALID_PERSISTED_BINDING,
        )
    }

    @Test
    fun missingPositiveExampleProducesTypedFailure() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persistBinding(fixture, bindingRoot)

        assertFailure(
            resolve(fixture, bindingRoot, exampleRoot),
            Resolver.FailureReason.MISSING_POSITIVE_TRAINING_EXAMPLE,
        )
    }

    @Test
    fun malformedPositiveExampleProducesTypedFailure() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persistBinding(fixture, bindingRoot)
        writePositive(exampleRoot, fixture, "not-json\n".toByteArray(StandardCharsets.UTF_8))

        assertFailure(
            resolve(fixture, bindingRoot, exampleRoot),
            Resolver.FailureReason.INVALID_POSITIVE_TRAINING_EXAMPLE,
        )
    }

    @Test
    fun truncatedPositiveExampleProducesTypedFailure() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persistBinding(fixture, bindingRoot)
        val bytes = HimPositiveTrainingExamplePersistenceV1.serializeRecord(
            HimPositiveTrainingExamplePersistenceV1.buildRecord(fixture.example),
        )
        writePositive(exampleRoot, fixture, bytes.copyOf(bytes.size - 2))

        assertFailure(
            resolve(fixture, bindingRoot, exampleRoot),
            Resolver.FailureReason.INVALID_POSITIVE_TRAINING_EXAMPLE,
        )
    }

    @Test
    fun positiveExampleDigestCorruptionProducesTypedFailure() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persistBinding(fixture, bindingRoot)
        val valid = String(
            HimPositiveTrainingExamplePersistenceV1.serializeRecord(
                HimPositiveTrainingExamplePersistenceV1.buildRecord(fixture.example),
            ),
            StandardCharsets.UTF_8,
        )
        val broken = valid.replace(
            "\"logicalDigest\":\"${fixture.exampleRecordDigest()}\"",
            "\"logicalDigest\":\"${"0".repeat(64)}\"",
        )
        writePositive(exampleRoot, fixture, broken.toByteArray(StandardCharsets.UTF_8))

        assertFailure(
            resolve(fixture, bindingRoot, exampleRoot),
            Resolver.FailureReason.INVALID_POSITIVE_TRAINING_EXAMPLE,
        )
    }

    @Test
    fun referenceContentMismatchProducesTypedFailure() = withRoots { bindingRoot, exampleRoot ->
        val first = fixture(1)
        val foreign = fixture(2)
        persistBinding(first, bindingRoot)
        val foreignRecord = HimPositiveTrainingExamplePersistenceV1.buildRecord(foreign.example)
        val target = HimPositiveTrainingExamplePersistenceV1.pathFor(first.example.exampleReference, exampleRoot)
        target.parentFile.mkdirs()
        target.writeBytes(HimPositiveTrainingExamplePersistenceV1.serializeRecord(foreignRecord))

        assertFailure(
            resolve(first, bindingRoot, exampleRoot),
            Resolver.FailureReason.REFERENCE_CONTENT_MISMATCH,
        )
    }

    @Test
    fun oneInvocationResolvesExactlyOneBindingKey() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persist(fixture, bindingRoot, exampleRoot)

        val result = completed(resolve(fixture, bindingRoot, exampleRoot))
        assertEquals(fixture.bindingRecord.bindingKey, result.bindingRecord.bindingKey)
    }

    @Test
    fun differentBindingKeysMayShareOneReference() = withRoots { bindingRoot, exampleRoot ->
        val example = example(1)
        val first = fixture(1, example)
        val second = fixture(2, example)
        persist(first, bindingRoot, exampleRoot)
        persistBinding(second, bindingRoot)

        val firstResolved = completed(resolve(first, bindingRoot, exampleRoot))
        val secondResolved = completed(resolve(second, bindingRoot, exampleRoot))
        assertEquals(firstResolved.trainingExample, secondResolved.trainingExample)
        assertEquals(first.bindingRecord.trainingExampleReference, second.bindingRecord.trainingExampleReference)
    }

    @Test
    fun firstManyToOneBindingResolves() = withRoots { bindingRoot, exampleRoot ->
        val example = example(1)
        val first = fixture(1, example)
        persist(first, bindingRoot, exampleRoot)

        assertEquals(example, completed(resolve(first, bindingRoot, exampleRoot)).trainingExample)
    }

    @Test
    fun secondManyToOneBindingResolves() = withRoots { bindingRoot, exampleRoot ->
        val example = example(1)
        val first = fixture(1, example)
        val second = fixture(2, example)
        persist(first, bindingRoot, exampleRoot)
        persistBinding(second, bindingRoot)

        assertEquals(example, completed(resolve(second, bindingRoot, exampleRoot)).trainingExample)
    }

    @Test
    fun manyToOneBindingsRetainDifferentBindingRecords() = withRoots { bindingRoot, exampleRoot ->
        val example = example(1)
        val first = fixture(1, example)
        val second = fixture(2, example)
        persist(first, bindingRoot, exampleRoot)
        persistBinding(second, bindingRoot)

        val firstRecord = completed(resolve(first, bindingRoot, exampleRoot)).bindingRecord
        val secondRecord = completed(resolve(second, bindingRoot, exampleRoot)).bindingRecord
        assertNotEquals(firstRecord, secondRecord)
        assertNotEquals(firstRecord.bindingKey, secondRecord.bindingKey)
    }

    @Test
    fun manyToOneResolutionDoesNotRejectDuplicateReference() = withRoots { bindingRoot, exampleRoot ->
        val example = example(1)
        val first = fixture(1, example)
        val second = fixture(2, example)
        persist(first, bindingRoot, exampleRoot)
        persistBinding(second, bindingRoot)

        assertTrue(resolve(first, bindingRoot, exampleRoot) is Resolver.Result.Completed)
        assertTrue(resolve(second, bindingRoot, exampleRoot) is Resolver.Result.Completed)
    }

    @Test
    fun resolverDoesNotInvokeBatchBindingResolution() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persist(fixture, bindingRoot, exampleRoot)

        val result = resolve(fixture, bindingRoot, exampleRoot)
        assertIs<Resolver.Result.Completed>(result)
        assertEquals(Resolver.FailureReason.entries.toSet(), setOf(
            Resolver.FailureReason.BINDING_NOT_FOUND,
            Resolver.FailureReason.INVALID_PERSISTED_BINDING,
            Resolver.FailureReason.MISSING_POSITIVE_TRAINING_EXAMPLE,
            Resolver.FailureReason.INVALID_POSITIVE_TRAINING_EXAMPLE,
            Resolver.FailureReason.REFERENCE_CONTENT_MISMATCH,
        ))
    }

    @Test
    fun resolverDoesNotReopenMaterializationLineage() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persist(fixture, bindingRoot, exampleRoot)

        assertEquals(fixture.example, completed(resolve(fixture, bindingRoot, exampleRoot)).trainingExample)
    }

    @Test
    fun resolverUsesExactReferenceInsteadOfDirectoryScan() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture(1)
        val decoy = fixture(2)
        persist(fixture, bindingRoot, exampleRoot)
        val decoyRecord = HimPositiveTrainingExamplePersistenceV1.buildRecord(decoy.example)
        val decoyPath = HimPositiveTrainingExamplePersistenceV1.pathFor(decoy.example.exampleReference, exampleRoot)
        decoyPath.parentFile.mkdirs()
        decoyPath.writeBytes(HimPositiveTrainingExamplePersistenceV1.serializeRecord(decoyRecord))

        assertEquals(fixture.example, completed(resolve(fixture, bindingRoot, exampleRoot)).trainingExample)
    }

    @Test
    fun resolverDoesNotWriteDuringResolution() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persist(fixture, bindingRoot, exampleRoot)
        val beforeBinding = snapshot(bindingRoot)
        val beforeExamples = snapshot(exampleRoot)

        completed(resolve(fixture, bindingRoot, exampleRoot))

        assertSnapshotsEqual(beforeBinding, snapshot(bindingRoot))
        assertSnapshotsEqual(beforeExamples, snapshot(exampleRoot))
    }

    @Test
    fun resolverUsesOnlyProvidedTemporaryRoots() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persist(fixture, bindingRoot, exampleRoot)

        assertTrue(bindingRoot.isDirectory)
        assertTrue(exampleRoot.isDirectory)
        assertEquals(1, bindingRoot.walkTopDown().count { it.isFile })
        assertEquals(1, exampleRoot.walkTopDown().count { it.isFile })
    }

    @Test
    fun missingBindingIsTheOnlyMissingBindingFailure() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()

        val failure = assertIs<Resolver.Result.Failed>(resolve(fixture, bindingRoot, exampleRoot))
        assertEquals(Resolver.FailureReason.BINDING_NOT_FOUND, failure.reason)
    }

    @Test
    fun allResolverFailureReasonsAreTyped() {
        assertEquals(5, Resolver.FailureReason.entries.size)
        assertTrue(Resolver.FailureReason.entries.all { it.name.isNotBlank() })
    }

    @Test
    fun completedResultContainsNoGeneratedIdentityOrDigest() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persist(fixture, bindingRoot, exampleRoot)

        val resolved = completed(resolve(fixture, bindingRoot, exampleRoot))
        assertEquals(fixture.bindingRecord, resolved.bindingRecord)
        assertEquals(fixture.example, resolved.trainingExample)
    }

    @Test
    fun resolverFailureDoesNotExposeFilesystemDetails() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        val failure = assertIs<Resolver.Result.Failed>(resolve(fixture, bindingRoot, exampleRoot))

        assertEquals(Resolver.FailureReason.BINDING_NOT_FOUND, failure.reason)
    }

    @Test
    fun resolverIsOfflineAndDeterministicForIndependentRoots() = withRootsPair { first, second ->
        val fixture = fixture()
        persist(fixture, first.bindingRoot, first.exampleRoot)
        persist(fixture, second.bindingRoot, second.exampleRoot)

        assertEquals(
            completed(resolve(fixture, first.bindingRoot, first.exampleRoot)),
            completed(resolve(fixture, second.bindingRoot, second.exampleRoot)),
        )
    }

    @Test
    fun resolverPreservesBindingReferenceWithoutCoercion() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persist(fixture, bindingRoot, exampleRoot)

        val resolved = completed(resolve(fixture, bindingRoot, exampleRoot))
        assertEquals(fixture.bindingRecord.trainingExampleReference, resolved.bindingRecord.trainingExampleReference)
    }

    @Test
    fun positiveStoreIsReadExactlyOnceByTheResolverPath() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persist(fixture, bindingRoot, exampleRoot)
        val before = snapshot(exampleRoot)

        assertIs<Resolver.Result.Completed>(resolve(fixture, bindingRoot, exampleRoot))
        assertSnapshotsEqual(before, snapshot(exampleRoot))
    }

    @Test
    fun resolverDoesNotCreateASecondBindingForOneKey() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persist(fixture, bindingRoot, exampleRoot)

        completed(resolve(fixture, bindingRoot, exampleRoot))
        assertEquals(1, bindingRoot.walkTopDown().count { it.isFile })
    }

    @Test
    fun invalidPositiveExampleReferenceIsNotAcceptedAsResolved() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persistBinding(fixture, bindingRoot)
        val path = HimPositiveTrainingExamplePersistenceV1.pathFor(fixture.example.exampleReference, exampleRoot)
        path.parentFile.mkdirs()
        path.writeText("{\"logicalDigest\":\"bad\"}\n", StandardCharsets.UTF_8)

        assertFailure(
            resolve(fixture, bindingRoot, exampleRoot),
            Resolver.FailureReason.INVALID_POSITIVE_TRAINING_EXAMPLE,
        )
    }

    @Test
    fun resolverResultIsImmutableData() = withRoots { bindingRoot, exampleRoot ->
        val fixture = fixture()
        persist(fixture, bindingRoot, exampleRoot)

        val result = completed(resolve(fixture, bindingRoot, exampleRoot))
        assertEquals(fixture.bindingRecord.bindingKey, result.bindingRecord.bindingKey)
        assertEquals(fixture.example.exampleReference, result.trainingExample.exampleReference)
    }

    private fun resolve(fixture: Fixture, bindingRoot: File, exampleRoot: File): Resolver.Result =
        Resolver.resolveByNegativeSupervisionRecordId(
            recordId = fixture.bindingRecord.negativeSupervisionRecordId,
            bindingRoot = bindingRoot,
            positiveExampleRoot = exampleRoot,
        )

    private fun completed(result: Resolver.Result): Resolver.Resolved =
        assertIs<Resolver.Result.Completed>(result).value

    private fun assertFailure(result: Resolver.Result, reason: Resolver.FailureReason) {
        assertEquals(reason, assertIs<Resolver.Result.Failed>(result).reason)
    }

    private fun persist(fixture: Fixture, bindingRoot: File, exampleRoot: File) {
        persistExample(fixture.example, exampleRoot)
        persistBinding(fixture, bindingRoot)
    }

    private fun persistBinding(fixture: Fixture, bindingRoot: File) {
        val result = BindingPersistence.execute(
            BindingPersistence.Request(
                bindingDecision = fixture.decision,
                durableRoot = bindingRoot,
            ),
        )
        assertTrue(result is BindingPersistence.Result.Completed)
    }

    private fun persistExample(example: HimTrainingExampleV1, exampleRoot: File) {
        val result = HimPositiveTrainingExamplePersistenceV1.execute(
            HimPositiveTrainingExamplePersistenceV1.Request(
                example = example,
                durableRoot = exampleRoot,
            ),
        )
        assertTrue(result is HimPositiveTrainingExamplePersistenceV1.Result.Created)
    }

    private fun writeBinding(root: File, fixture: Fixture, bytes: ByteArray) {
        val target = BindingPersistence.pathFor(fixture.bindingRecord.bindingKey, root)
        target.parentFile.mkdirs()
        target.writeBytes(bytes)
    }

    private fun writePositive(root: File, fixture: Fixture, bytes: ByteArray) {
        val target = HimPositiveTrainingExamplePersistenceV1.pathFor(fixture.example.exampleReference, root)
        target.parentFile.mkdirs()
        target.writeBytes(bytes)
    }

    private fun snapshot(root: File): Map<String, ByteArray> = root.walkTopDown()
        .filter { it.isFile }
        .associate { it.relativeTo(root).path to it.readBytes() }

    private fun assertSnapshotsEqual(
        expected: Map<String, ByteArray>,
        actual: Map<String, ByteArray>,
    ) {
        assertEquals(expected.keys, actual.keys)
        expected.forEach { (path, expectedBytes) ->
            val actualBytes = assertNotNull(actual[path], "Missing snapshot entry: $path")
            assertTrue(expectedBytes.contentEquals(actualBytes), "Snapshot bytes differ for: $path")
        }
    }

    private fun withRoots(block: (File, File) -> Unit) {
        val bindingRoot = Files.createTempDirectory("him-reference-resolver-binding-").toFile()
        val exampleRoot = Files.createTempDirectory("him-reference-resolver-example-").toFile()
        try {
            block(bindingRoot, exampleRoot)
        } finally {
            bindingRoot.walkBottomUp().forEach { it.delete() }
            exampleRoot.walkBottomUp().forEach { it.delete() }
        }
    }

    private fun withRootsPair(block: (Roots, Roots) -> Unit) {
        val first = Roots(
            Files.createTempDirectory("him-reference-resolver-first-binding-").toFile(),
            Files.createTempDirectory("him-reference-resolver-first-example-").toFile(),
        )
        val second = Roots(
            Files.createTempDirectory("him-reference-resolver-second-binding-").toFile(),
            Files.createTempDirectory("him-reference-resolver-second-example-").toFile(),
        )
        try {
            block(first, second)
        } finally {
            listOf(first.bindingRoot, first.exampleRoot, second.bindingRoot, second.exampleRoot)
                .forEach { root -> root.walkBottomUp().forEach { it.delete() } }
        }
    }

    private data class Roots(val bindingRoot: File, val exampleRoot: File)

    private data class Fixture(
        val example: HimTrainingExampleV1,
        val decision: BindingContract.BindingDecision,
    ) {
        val bindingRecord: BindingPersistence.BindingRecord
            get() = BindingPersistence.recordFrom(decision)

        fun exampleRecordDigest(): String =
            HimPositiveTrainingExamplePersistenceV1.buildRecord(example).logicalDigest
    }

    private fun fixture(index: Int = 1, example: HimTrainingExampleV1 = example(index)): Fixture = Fixture(
        example = example,
        decision = boundDecision(index, example.exampleReference),
    )

    private fun boundDecision(index: Int, reference: HimTrainingExampleReference) =
        BindingContract.BindingDecision(
            bindingDecisionId = sha("binding-decision-$index"),
            bindingKey = sha("binding-key-$index"),
            negativeSupervisionRecordId = sha("binding-key-$index"),
            materializationDecisionId = sha("materialization-$index"),
            trainingExampleReference = reference,
            state = BindingContract.BindingState.BOUND,
            reasons = emptyList(),
        )

    private fun example(index: Int): HimTrainingExampleV1 {
        val canonical = HimEntityId("Abc123")
        val evidence = HimTrainingEvidenceInputV1(
            reference = HimEvidenceReference(
                source = HimGroundTruthSource.OPEN_FOOD_FACTS.name,
                sourceArtifactSha256 = HimSha256("$index".repeat(64).take(64)),
                sourceRecordIdentity = "off:resolver:$index",
            ),
            recordKind = "FOOD",
            retrievalRank = 1,
        )
        val input = HimTrainingInputV1(
            observedTerm = "Hering eingelegt $index",
            normalizedObservedTerm = "hering eingelegt $index",
            canonicalContext = listOf(HimCandidateCanonicalContext(1, canonical, "Hering", null)),
            evidence = listOf(evidence),
        )
        return HimTrainingExampleV1.create(
            taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
            input = input,
            target = HimTrainingTargetV1.ExistingCanonical(canonical),
            provenance = HimTrainingProvenanceV1(
                sourceEvidenceReferences = listOf(evidence.reference),
                sourceArtifactDigests = listOf(evidence.reference.sourceArtifactSha256),
            ),
        )
    }

    private fun sha(value: String): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
