package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleReferenceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1Test {
    @Test
    fun validAssembledCorpusPersists() = withRoot { root ->
        val result = persist(root, Fixture.corpus())
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.PersistenceStatusV1.CREATED, result.status)
        assertEquals(Fixture.corpus().logicalDigest, result.record.corpusLogicalDigest)
    }

    @Test
    fun createdArtifactExistsUnderTheSuppliedTemporaryRoot() = withRoot { root ->
        val result = persist(root, Fixture.corpus())
        assertTrue(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.pathFor(result.record.corpusLogicalDigest.value, root).isFile)
    }

    @Test
    fun exactReaderReloadSucceeds() = withRoot { root ->
        val created = persist(root, Fixture.corpus())
        assertEquals(created.record, HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.readByCorpusLogicalDigest(created.record.corpusLogicalDigest.value, root))
    }

    @Test
    fun allMembersSurviveExactReload() = withRoot { root ->
        val corpus = Fixture.corpusWithNegative()
        val reloaded = persist(root, corpus).record
        assertEquals(corpus.members, reloaded.members)
    }

    @Test
    fun polaritySurvivesExactReload() = withRoot { root ->
        val members = persist(root, Fixture.corpusWithNegative()).record.members
        assertEquals(
            listOf(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.POSITIVE,
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.NEGATIVE,
            ),
            members.map { it.polarity },
        )
    }

    @Test
    fun modelInputSurvivesExactReload() = withRoot { root ->
        val corpus = Fixture.corpusWithNegative()
        assertEquals(corpus.members.map { it.modelInput }, persist(root, corpus).record.members.map { it.modelInput })
    }

    @Test
    fun supervisionSurvivesExactReload() = withRoot { root ->
        val corpus = Fixture.corpusWithNegative()
        assertEquals(corpus.members.map { it.supervision }, persist(root, corpus).record.members.map { it.supervision })
    }

    @Test
    fun auditBindingSurvivesExactReload() = withRoot { root ->
        val corpus = Fixture.corpusWithNegative()
        assertEquals(corpus.members.map { it.auditBinding }, persist(root, corpus).record.members.map { it.auditBinding })
    }

    @Test
    fun countersSurviveExactReload() = withRoot { root ->
        val corpus = Fixture.corpusWithNegative()
        assertEquals(corpus.counters, persist(root, corpus).record.counters)
    }

    @Test
    fun corpusLogicalDigestSurvivesExactReload() = withRoot { root ->
        val corpus = Fixture.corpusWithNegative()
        assertEquals(corpus.logicalDigest, persist(root, corpus).record.corpusLogicalDigest)
    }

    @Test
    fun canonicalBytesAreDeterministic() = withRoot { root ->
        val corpus = Fixture.corpusWithNegative()
        val first = persist(root, corpus)
        val bytes = Files.readAllBytes(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.pathFor(first.record.corpusLogicalDigest.value, root).toPath())
        assertEquals(bytes.toList(), HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.serializeRecord(first.record).toList())
        assertEquals(first.sha256, sha256(bytes.toString(Charsets.UTF_8)))
    }

    @Test
    fun emptyAssembledCorpusPersistsWithoutTrainingReadyState() = withRoot { root ->
        val corpus = completedAssembly(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request())
        val result = persist(root, corpus)
        assertEquals(0, result.record.counters.total)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.STATE, result.record.state)
        assertNotEquals("TRAINING_READY", result.record.state)
    }

    @Test
    fun emptySnapshotReloadPreservesZeroCounters() = withRoot { root ->
        val corpus = completedAssembly(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request())
        val reloaded = persist(root, corpus).record
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CountersV1(0, 0, 0), reloaded.counters)
    }

    @Test
    fun secondWriteReturnsAlreadyPresentIdentical() = withRoot { root ->
        val corpus = Fixture.corpus()
        persist(root, corpus)
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.PersistenceStatusV1.ALREADY_PRESENT_IDENTICAL,
            persist(root, corpus).status,
        )
    }

    @Test
    fun identicalWriteDoesNotChangeBytes() = withRoot { root ->
        val corpus = Fixture.corpus()
        val first = persist(root, corpus)
        val path = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.pathFor(first.record.corpusLogicalDigest.value, root)
        val before = path.readBytes()
        persist(root, corpus)
        assertEquals(before.toList(), path.readBytes().toList())
    }

    @Test
    fun repeatedReaderIsDeterministic() = withRoot { root ->
        val corpus = Fixture.corpusWithNegative()
        val created = persist(root, corpus)
        assertEquals(created.record, HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.readByCorpusLogicalDigest(created.record.corpusLogicalDigest.value, root))
        assertEquals(created.record, HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.readByCorpusLogicalDigest(created.record.corpusLogicalDigest.value, root))
    }

    @Test
    fun changedSemanticContentGetsASeparateImmutableSnapshot() = withRoot { root ->
        val first = Fixture.corpus()
        val changedMember = Fixture.corpus(target = HimTrainingTargetV1.NewCanonical("different answer"))
        val changedSupervision = Fixture.corpusWithNegative(HimNegativeBoundaryTypeV1.WRONG_SCOPE)
        val changedAuditBinding = Fixture.corpusWithNegative(auditDigest = HimSha256("e".repeat(64)))
        persist(root, first)
        assertNotEquals(first.logicalDigest, changedMember.logicalDigest)
        assertNotEquals(first.logicalDigest, changedSupervision.logicalDigest)
        assertNotEquals(first.logicalDigest, changedAuditBinding.logicalDigest)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.PersistenceStatusV1.CREATED, persist(root, changedMember).status)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.PersistenceStatusV1.CREATED, persist(root, changedSupervision).status)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.PersistenceStatusV1.CREATED, persist(root, changedAuditBinding).status)
    }

    @Test
    fun changedCountersCannotBePersisted() = withRoot { root ->
        val corpus = Fixture.corpus()
        val invalid = corpus.copy(counters = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CountersV1(0, 0, 0))
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.Result.Failed>(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.execute(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.Request(invalid, root),
            ),
        )
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.FailureReasonV1.INVALID_ASSEMBLED_CORPUS, failure.reason)
    }

    @Test
    fun malformedExistingTargetIsNeverOverwritten() = withRoot { root ->
        val corpus = Fixture.corpus()
        val path = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.pathFor(corpus.logicalDigest.value, root)
        path.parentFile.mkdirs()
        path.writeText("{\"broken\":true}\n")
        val before = path.readBytes()
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.Result.Failed>(persistResult(root, corpus))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.FailureReasonV1.MALFORMED_EXISTING_ARTIFACT, failure.reason)
        assertEquals(before.toList(), path.readBytes().toList())
    }

    @Test
    fun missingArtifactFailsClosedOnDirectRead() = withRoot { root ->
        val failure = assertFailsWith<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.PersistenceFailure> {
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.readByCorpusLogicalDigest("a".repeat(64), root)
        }
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.FailureReasonV1.ARTIFACT_MISSING, failure.reason)
    }

    @Test
    fun malformedJsonFailsClosed() = withRoot { root ->
        val corpus = Fixture.corpus()
        val path = targetPath(root, corpus)
        path.parentFile.mkdirs()
        path.writeText("not-json\n")
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.FailureReasonV1.MALFORMED_EXISTING_ARTIFACT, assertFailsWith<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.PersistenceFailure> {
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.readByCorpusLogicalDigest(corpus.logicalDigest.value, root)
        }.reason)
    }

    @Test
    fun truncatedJsonFailsClosed() = withRoot { root ->
        val corpus = Fixture.corpus()
        val path = targetPath(root, corpus)
        path.parentFile.mkdirs()
        path.writeBytes(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.serializeRecord(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.buildRecord(corpus)).dropLast(2).toByteArray())
        assertFailsWith<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.PersistenceFailure> {
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.readByCorpusLogicalDigest(corpus.logicalDigest.value, root)
        }
    }

    @Test
    fun unknownFieldFailsClosed() = withRoot { root ->
        val corpus = Fixture.corpus()
        val record = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.buildRecord(corpus)
        val text = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.serializeRecord(record).toString(Charsets.UTF_8)
        targetPath(root, corpus).apply { parentFile.mkdirs(); writeText(text.removeSuffix("\n").removeSuffix("}") + ",\"unknown\":true}\n") }
        assertMalformedRead(root, corpus)
    }

    @Test
    fun duplicateFieldFailsClosed() = withRoot { root ->
        val corpus = Fixture.corpus()
        val record = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.buildRecord(corpus)
        val text = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.serializeRecord(record).toString(Charsets.UTF_8)
        val duplicate = text.replace("\"state\":\"${record.state}\"", "\"state\":\"${record.state}\",\"state\":\"${record.state}\"")
        targetPath(root, corpus).apply { parentFile.mkdirs(); writeText(duplicate) }
        assertMalformedRead(root, corpus)
    }

    @Test
    fun missingFieldFailsClosed() = withRoot { root ->
        val corpus = Fixture.corpus()
        val record = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.buildRecord(corpus)
        val text = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.serializeRecord(record).toString(Charsets.UTF_8)
        val missing = text.replace("\"state\":\"${record.state}\",", "")
        targetPath(root, corpus).apply { parentFile.mkdirs(); writeText(missing) }
        assertMalformedRead(root, corpus)
    }

    @Test
    fun wrongContractVersionAndStateFailClosed() = withRoot { root ->
        val corpus = Fixture.corpus()
        val record = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.buildRecord(corpus)
        val text = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.serializeRecord(record).toString(Charsets.UTF_8)
        targetPath(root, corpus).apply { parentFile.mkdirs(); writeText(text.replace(record.contractId, "wrong").replace("\"version\":\"1\"", "\"version\":\"2\"").replace(record.state, "WRONG_STATE")) }
        assertMalformedRead(root, corpus)
    }

    @Test
    fun malformedSnapshotIdentityFailsClosed() = withRoot { root ->
        val corpus = Fixture.corpus()
        val record = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.buildRecord(corpus)
        val text = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.serializeRecord(record).toString(Charsets.UTF_8)
        targetPath(root, corpus).apply { parentFile.mkdirs(); writeText(text.replace(record.snapshotId, "training-corpus:v1:bad")) }
        assertMalformedRead(root, corpus)
    }

    @Test
    fun wrongCorpusDigestFailsClosed() = withRoot { root ->
        val corpus = Fixture.corpus()
        val record = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.buildRecord(corpus)
        val text = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.serializeRecord(record).toString(Charsets.UTF_8)
        targetPath(root, corpus).apply { parentFile.mkdirs(); writeText(text.replace(corpus.logicalDigest.value, "b".repeat(64))) }
        assertMalformedRead(root, corpus)
    }

    @Test
    fun invalidPolarityFailsClosed() = withRoot { root ->
        val corpus = Fixture.corpus()
        val record = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.buildRecord(corpus)
        val text = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.serializeRecord(record).toString(Charsets.UTF_8)
        targetPath(root, corpus).apply { parentFile.mkdirs(); writeText(text.replace("\"polarity\":\"POSITIVE\"", "\"polarity\":\"UNKNOWN\"")) }
        assertMalformedRead(root, corpus)
    }

    @Test
    fun malformedMembershipIdentityFailsClosed() = withRoot { root ->
        val corpus = Fixture.corpus()
        val record = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.buildRecord(corpus)
        val text = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.serializeRecord(record).toString(Charsets.UTF_8)
        targetPath(root, corpus).apply { parentFile.mkdirs(); writeText(text.replace(record.members.single().membershipReference, "bad")) }
        assertMalformedRead(root, corpus)
    }

    @Test
    fun extraTrailingLfFailsClosed() = withRoot { root ->
        val corpus = Fixture.corpus()
        val record = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.buildRecord(corpus)
        targetPath(root, corpus).apply { parentFile.mkdirs(); writeBytes(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.serializeRecord(record) + '\n'.code.toByte()) }
        assertMalformedRead(root, corpus)
    }

    @Test
    fun missingTrailingLfFailsClosed() = withRoot { root ->
        val corpus = Fixture.corpus()
        val record = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.buildRecord(corpus)
        targetPath(root, corpus).apply { parentFile.mkdirs(); writeBytes(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.serializeRecord(record).dropLast(1).toByteArray()) }
        assertMalformedRead(root, corpus)
    }

    @Test
    fun crlfWireFailsClosed() = withRoot { root ->
        val corpus = Fixture.corpus()
        val record = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.buildRecord(corpus)
        val text = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.serializeRecord(record).toString(Charsets.UTF_8)
        targetPath(root, corpus).apply { parentFile.mkdirs(); writeBytes(text.replace("}\n", "}\r\n").toByteArray()) }
        assertMalformedRead(root, corpus)
    }

    @Test
    fun invalidCorpusIdentityFailsClosed() = withRoot { root ->
        val failure = assertFailsWith<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.PersistenceFailure> {
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.pathFor("not-a-digest", root)
        }
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.FailureReasonV1.INVALID_CORPUS_IDENTITY, failure.reason)
    }

    @Test
    fun traversalIdentityIsRejectedByDirectPathApi() {
        assertFailsWith<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.PersistenceFailure> {
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.pathFor("../" + "a".repeat(64))
        }
    }

    @Test
    fun rootSymlinkIsRejected() = withRoot { root ->
        val real = Files.createTempDirectory(root.toPath(), "real-").toFile()
        val link = root.resolve("root-link")
        Files.createSymbolicLink(link.toPath(), real.toPath())
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.Result.Failed>(persistResult(link, Fixture.corpus()))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.FailureReasonV1.UNSAFE_PATH, failure.reason)
    }

    @Test
    fun targetSymlinkIsRejected() = withRoot { root ->
        val corpus = Fixture.corpus()
        val target = targetPath(root, corpus)
        target.parentFile.mkdirs()
        val outside = root.resolve("outside.json").apply { writeText("outside") }
        Files.createSymbolicLink(target.toPath(), outside.toPath())
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.Result.Failed>(persistResult(root, corpus))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.FailureReasonV1.UNSAFE_PATH, failure.reason)
    }

    @Test
    fun deterministicTemporaryCollisionFailsClosed() = withRoot { root ->
        val corpus = Fixture.corpus()
        val path = targetPath(root, corpus)
        path.parentFile.mkdirs()
        path.resolveSibling(".${path.name}.tmp").writeText("occupied")
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.Result.Failed>(persistResult(root, corpus))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.FailureReasonV1.TEMPORARY_FILE_CONFLICT, failure.reason)
    }

    @Test
    fun persistenceHasNoPartitionLeakageTrainingOrStoreScanOperations() {
        val names = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1::class.java.declaredMethods.map { it.name }
        assertTrue(names.none { it.contains("partition", true) || it.contains("leakage", true) || it.contains("train", true) })
        assertTrue(names.none { it.contains("list", true) || it.contains("scan", true) || it.contains("walk", true) })
        assertTrue(names.none { it.contains("positive", true) && it.contains("read", true) })
        assertTrue(names.none { it.contains("negative", true) && it.contains("read", true) })
    }

    private fun persist(root: File, corpus: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CorpusV1) =
        assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.Result.Completed>(persistResult(root, corpus))

    private fun persistResult(root: File, corpus: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CorpusV1) =
        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.execute(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.Request(corpus, root),
        )

    private fun targetPath(root: File, corpus: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CorpusV1) =
        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.pathFor(corpus.logicalDigest.value, root)

    private fun assertMalformedRead(root: File, corpus: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CorpusV1) {
        assertFailsWith<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.PersistenceFailure> {
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.readByCorpusLogicalDigest(corpus.logicalDigest.value, root)
        }
    }

    private fun withRoot(block: (File) -> Unit) {
        val root = Files.createTempDirectory("him-corpus-persistence-").toFile().canonicalFile
        try {
            block(root)
        } finally {
            root.deleteRecursively()
        }
    }

    private object Fixture {
        private val canonicalId = HimEntityId("Abc123")
        private val evidenceReference = HimEvidenceReference(
            source = HimGroundTruthSource.OPEN_FOOD_FACTS.name,
            sourceArtifactSha256 = HimSha256("a".repeat(64)),
            sourceRecordIdentity = "off:product:fixture:123",
        )
        private val input = HimTrainingInputV1(
            observedTerm = "fixture food",
            normalizedObservedTerm = "fixture food",
            canonicalContext = listOf(HimCandidateCanonicalContext(1, canonicalId, "Fixture Food", null)),
            evidence = listOf(HimTrainingEvidenceInputV1(evidenceReference, "FOOD", 1)),
        )

        fun example(target: HimTrainingTargetV1 = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(canonicalId))) =
            HimTrainingExampleV1.create(
                taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
                input = input,
                target = target,
                provenance = HimTrainingProvenanceV1(sourceEvidenceReferences = listOf(evidenceReference)),
            )

        fun corpus(target: HimTrainingTargetV1 = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(canonicalId))) =
            completedAssembly(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                    positiveMembers = listOf(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PositiveMember(example(target))),
                ),
            )

        fun corpusWithNegative(
            boundaryType: HimNegativeBoundaryTypeV1 = HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
            auditDigest: HimSha256 = HimSha256("d".repeat(64)),
        ): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CorpusV1 {
            val positive = corpus().members.single()
            val positiveReference = (positive.auditBinding as HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Positive).exampleReference
            val materializationId = "negative-materialization:v1:${"e".repeat(64)}"
            val negative = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.MemberV1(
                polarity = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.NEGATIVE,
                membershipReference = membership("HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_TRAINING_CORPUS_NEGATIVE_MEMBER_V1", materializationId, "negative"),
                modelInput = positive.modelInput,
                supervision = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1.Negative(
                    rejectedTarget = HimTrainingTargetV1.NewCanonical("rejected fixture"),
                    boundaryType = boundaryType,
                ),
                auditBinding = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Negative(
                    p1MaterializationId = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.MaterializationReferenceV1(materializationId),
                    negativeExampleReference = HimNegativeTrainingExampleReferenceV1("negative-example:v1:${"f".repeat(64)}"),
                    positiveExampleReference = positiveReference,
                    negativeRecordLogicalDigest = auditDigest,
                ),
            )
            val members = listOf(positive, negative).sortedWith(compareBy({ it.polarity.ordinal }, { it.membershipReference }))
            val counters = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CountersV1.from(members)
            val unsigned = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CorpusV1(members, counters, HimSha256("0".repeat(64)))
            return unsigned.copy(logicalDigest = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.corpusLogicalDigest(unsigned))
        }

        private fun membership(domain: String, value: String, polarity: String): String {
            val canonical = "domain=${domain.length}:$domain\nvalue=${value.length}:$value\n"
            return "corpus-member:v1:$polarity:${sha256(canonical)}"
        }
    }

}

private fun completedAssembly(
    request: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request,
) = assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Result.Completed>(
    HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.assemble(request),
).corpus

private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }
