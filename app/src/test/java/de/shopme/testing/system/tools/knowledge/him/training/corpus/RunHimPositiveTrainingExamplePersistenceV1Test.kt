package de.shopme.testing.system.tools.knowledge.him.training.corpus

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.training.corpus.HimPositiveTrainingExamplePersistenceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleIdentityV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimPositiveTrainingExamplePersistenceV1.FailureReason
import de.shopme.tools.knowledge.him.training.corpus.HimPositiveTrainingExamplePersistenceV1.PersistenceFailure
import de.shopme.tools.knowledge.him.training.corpus.HimPositiveTrainingExamplePersistenceV1.Result
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RunHimPositiveTrainingExamplePersistenceV1Test {

    @Test
    fun `valid example persists`() = withRoot { root ->
        val result = execute(root)
        assertTrue(result is Result.Created)
        assertTrue(HimPositiveTrainingExamplePersistenceV1.pathFor(EXAMPLE.exampleReference, root).isFile)
    }

    @Test
    fun `returned reference equals recomputed reference`() = withRoot { root ->
        val result = created(root)
        assertEquals(EXAMPLE.exampleReference, result.reference)
        assertEquals(HimTrainingExampleIdentityV1.example(EXAMPLE.taskType, EXAMPLE.input, EXAMPLE.target, EXAMPLE.provenance), result.reference)
    }

    @Test
    fun `exact file identity is deterministic`() = withRoot { root ->
        val first = created(root)
        val second = execute(root)
        assertEquals(first.relativePath, (second as Result.AlreadyPresentIdentical).relativePath)
        assertEquals(first.sha256, second.sha256)
        assertEquals(first.byteSize, second.byteSize)
    }

    @Test
    fun `read by exact reference succeeds`() = withRoot { root ->
        created(root)
        assertEquals(EXAMPLE, HimPositiveTrainingExamplePersistenceV1.read(EXAMPLE.exampleReference, root))
    }

    @Test
    fun `round trip preserves semantic equality`() = withRoot { root ->
        val created = created(root)
        assertEquals(created.record.example, HimPositiveTrainingExamplePersistenceV1.read(created.reference, root))
    }

    @Test
    fun `re encoded bytes are identical`() = withRoot { root ->
        val result = created(root)
        val path = HimPositiveTrainingExamplePersistenceV1.pathFor(result.reference, root)
        assertTrue(path.readBytes().contentEquals(HimPositiveTrainingExamplePersistenceV1.serializeRecord(result.record)))
    }

    @Test
    fun `identical second write is already present`() = withRoot { root ->
        created(root)
        assertTrue(execute(root) is Result.AlreadyPresentIdentical)
    }

    @Test
    fun `existing valid different wire bytes fail closed without overwrite`() = withRoot { root ->
        val created = created(root)
        val path = HimPositiveTrainingExamplePersistenceV1.pathFor(created.reference, root)
        val json = JsonParser.parseString(path.readText()).toString()
        path.writeText(GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(JsonParser.parseString(json)) + "\n")
        val before = path.readBytes()
        val result = execute(root)
        assertEquals(FailureReason.EXISTING_ARTIFACT_CONFLICT, (result as Result.Failed).reason)
        assertTrue(path.readBytes().contentEquals(before))
    }

    @Test
    fun `malformed existing artifact fails closed`() = withRoot { root ->
        val path = HimPositiveTrainingExamplePersistenceV1.pathFor(EXAMPLE.exampleReference, root)
        root.mkdirs()
        path.writeText("not-json\n")
        assertEquals(FailureReason.MALFORMED_ARTIFACT, (execute(root) as Result.Failed).reason)
    }

    @Test
    fun `truncated existing artifact fails closed`() = withRoot { root ->
        val created = created(root)
        val path = HimPositiveTrainingExamplePersistenceV1.pathFor(created.reference, root)
        path.writeBytes(path.readBytes().copyOf(path.length().toInt() / 2))
        assertEquals(FailureReason.MALFORMED_ARTIFACT, (execute(root) as Result.Failed).reason)
    }

    @Test
    fun `invalid schema id is rejected`() = withRoot { root ->
        val created = created(root)
        val path = HimPositiveTrainingExamplePersistenceV1.pathFor(created.reference, root)
        val json = JsonParser.parseString(path.readText()).asJsonObject
        json.addProperty("contractId", "wrong")
        path.writeText(json.toString() + "\n")
        assertEquals(FailureReason.MALFORMED_ARTIFACT, (execute(root) as Result.Failed).reason)
    }

    @Test
    fun `invalid schema version is rejected`() = withRoot { root ->
        val created = created(root)
        val path = HimPositiveTrainingExamplePersistenceV1.pathFor(created.reference, root)
        val json = JsonParser.parseString(path.readText()).asJsonObject
        json.addProperty("version", "2")
        path.writeText(json.toString() + "\n")
        assertEquals(FailureReason.MALFORMED_ARTIFACT, (execute(root) as Result.Failed).reason)
    }

    @Test
    fun `invalid example reference is rejected`() = withRoot { root ->
        val created = created(root)
        val path = HimPositiveTrainingExamplePersistenceV1.pathFor(created.reference, root)
        val json = JsonParser.parseString(path.readText()).asJsonObject
        json.addProperty("exampleReference", "invalid")
        path.writeText(json.toString() + "\n")
        assertEquals(FailureReason.MALFORMED_ARTIFACT, (execute(root) as Result.Failed).reason)
    }

    @Test
    fun `reference content mismatch is rejected`() = withRoot { root ->
        val created = created(root)
        val path = HimPositiveTrainingExamplePersistenceV1.pathFor(created.reference, root)
        val json = JsonParser.parseString(path.readText()).asJsonObject
        json.addProperty("exampleReference", "example:v1:${"0".repeat(64)}")
        path.writeText(json.toString() + "\n")
        assertEquals(FailureReason.REFERENCE_CONTENT_MISMATCH, (execute(root) as Result.Failed).reason)
    }

    @Test
    fun `invalid training example is rejected`() = withRoot { root ->
        val created = created(root)
        val path = HimPositiveTrainingExamplePersistenceV1.pathFor(created.reference, root)
        val json = JsonParser.parseString(path.readText()).asJsonObject
        json.getAsJsonObject("example").getAsJsonObject("input").addProperty("observedTerm", "")
        path.writeText(json.toString() + "\n")
        assertEquals(FailureReason.MALFORMED_ARTIFACT, (execute(root) as Result.Failed).reason)
    }

    @Test
    fun `all target variants round trip`() = withRoot { root ->
        val variants = listOf(
            HimTrainingTargetV1.ExistingCanonical(ID),
            HimTrainingTargetV1.Identity(ID),
            HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(ID)),
            HimTrainingTargetV1.Alias(HimFamilyEntityReference.Identity(ID, HimEntityId("def456"))),
            HimTrainingTargetV1.NewCanonical("New food"),
        )
        variants.forEachIndexed { index, target ->
            val example = example(target, "observed-$index")
            val result = execute(root, example)
            val record = (result as Result.Created).record
            assertEquals(target, HimPositiveTrainingExamplePersistenceV1.read(record.exampleReference, root).target)
        }
    }

    @Test
    fun `evidence ordering is canonical and deterministic`() = withRoot { root ->
        val reversed = example(EXAMPLE.target, EXAMPLE.input.observedTerm, evidence = EVIDENCE.reversed())
        val first = HimPositiveTrainingExamplePersistenceV1.serializeRecord(HimPositiveTrainingExamplePersistenceV1.buildRecord(EXAMPLE))
        val second = HimPositiveTrainingExamplePersistenceV1.serializeRecord(HimPositiveTrainingExamplePersistenceV1.buildRecord(reversed))
        assertTrue(first.contentEquals(second))
    }

    @Test
    fun `provenance ordering is canonical and deterministic`() {
        val reversed = EXAMPLE.copy(
            provenance = EXAMPLE.provenance.copy(
                sourceEvidenceReferences = EXAMPLE.provenance.sourceEvidenceReferences.reversed(),
                sourceArtifactDigests = EXAMPLE.provenance.sourceArtifactDigests.reversed(),
            ),
        )
        val first = HimPositiveTrainingExamplePersistenceV1.buildRecord(EXAMPLE)
        val second = HimPositiveTrainingExamplePersistenceV1.buildRecord(reversed)
        assertTrue(HimPositiveTrainingExamplePersistenceV1.serializeRecord(first).contentEquals(HimPositiveTrainingExamplePersistenceV1.serializeRecord(second)))
    }

    @Test
    fun `duplicate evidence is rejected by the model`() {
        try {
            HimTrainingInputV1("same", "same", evidence = listOf(EVIDENCE.first(), EVIDENCE.first()))
            fail("duplicate evidence must be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected model invariant.
        }
    }

    @Test
    fun `changed observed term changes example reference`() {
        assertNotEquals(EXAMPLE.exampleReference, example(EXAMPLE.target, "changed").exampleReference)
    }

    @Test
    fun `changed target changes example reference`() {
        assertNotEquals(EXAMPLE.exampleReference, example(HimTrainingTargetV1.Identity(ID), EXAMPLE.input.observedTerm).exampleReference)
    }

    @Test
    fun `changed provenance changes example reference`() {
        val changed = HimTrainingExampleV1.create(
            taskType = EXAMPLE.taskType,
            input = EXAMPLE.input,
            target = EXAMPLE.target,
            provenance = EXAMPLE.provenance.copy(retrievalFoundationRelease = "foundation-v2"),
        )
        assertNotEquals(EXAMPLE.exampleReference, changed.exampleReference)
    }

    @Test
    fun `changed evidence changes example reference`() {
        val changedEvidence = HimTrainingEvidenceInputV1(
            reference = HimEvidenceReference("OPEN_FOOD_FACTS", HimSha256("3".repeat(64)), "off:changed"),
            recordKind = "FOOD",
            retrievalRank = 1,
        )
        val changed = example(EXAMPLE.target, EXAMPLE.input.observedTerm, listOf(changedEvidence))
        assertNotEquals(EXAMPLE.exampleReference, changed.exampleReference)
    }

    @Test
    fun `disabled request writes nothing`() = withRoot { root ->
        assertTrue(execute(root, enabled = false) === Result.Disabled)
        assertTrue(root.listFiles().orEmpty().isEmpty())
        assertFalse(HimPositiveTrainingExamplePersistenceV1.pathFor(EXAMPLE.exampleReference, root).exists())
    }

    @Test
    fun `exact reference lookup does not use fuzzy lookup`() = withRoot { root ->
        created(root)
        val other = example(EXAMPLE.target, "other")
        expectReadFailure(other.exampleReference, root, FailureReason.ARTIFACT_MISSING)
    }

    @Test
    fun `no partition output is produced`() = withRoot { root ->
        created(root)
        assertTrue(root.walkTopDown().none { it.name.contains("partition", ignoreCase = true) })
    }

    @Test
    fun `no corpus output is produced`() = withRoot { root ->
        created(root)
        assertTrue(root.walkTopDown().none { it.name.contains("corpus", ignoreCase = true) })
    }

    @Test
    fun `no negative example output is produced`() = withRoot { root ->
        created(root)
        assertTrue(root.walkTopDown().none { it.name.contains("negative", ignoreCase = true) })
    }

    @Test
    fun `public API has no source or retrieval dependency`() {
        val names = HimPositiveTrainingExamplePersistenceV1::class.java.methods.map { it.name }
        assertTrue(names.none { it.contains("search", true) || it.contains("fetch", true) || it.contains("scan", true) || it.contains("sqlite", true) || it.contains("fts", true) })
    }

    @Test
    fun `filesystem is the only external boundary`() {
        val parameterTypes = HimPositiveTrainingExamplePersistenceV1::class.java.methods
            .flatMap { it.parameterTypes.toList() }
            .map { it.name }
        assertTrue(parameterTypes.none { it.contains("Source") || it.contains("SQLite") || it.contains("Connection") })
    }

    @Test
    fun `invalid reference cannot resolve a path`() {
        try {
            HimPositiveTrainingExamplePersistenceV1.pathFor(HimTrainingExampleReference("invalid"))
            fail("invalid reference must be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected reference invariant.
        }
    }

    @Test
    fun `record logical digest is stable`() {
        val first = HimPositiveTrainingExamplePersistenceV1.buildRecord(EXAMPLE)
        val second = HimPositiveTrainingExamplePersistenceV1.buildRecord(EXAMPLE)
        assertEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun `stored record reference selects digest-only filename`() = withRoot { root ->
        val path = HimPositiveTrainingExamplePersistenceV1.pathFor(EXAMPLE.exampleReference, root)
        assertTrue(path.name.matches(Regex("[0-9a-f]{64}\\.training-example\\.v1\\.json")))
        assertFalse(path.name.contains(':'))
    }

    @Test
    fun `read validates the stored record`() = withRoot { root ->
        val created = created(root)
        val path = HimPositiveTrainingExamplePersistenceV1.pathFor(created.reference, root)
        val json = JsonParser.parseString(path.readText()).asJsonObject
        json.addProperty("logicalDigest", "0".repeat(64))
        path.writeText(json.toString() + "\n")
        expectReadFailure(created.reference, root, FailureReason.MALFORMED_ARTIFACT)
    }

    @Test
    fun `write does not overwrite a conflicting artifact`() = withRoot { root ->
        val created = created(root)
        val path = HimPositiveTrainingExamplePersistenceV1.pathFor(created.reference, root)
        val original = path.readBytes()
        path.writeBytes(original + " ".toByteArray(StandardCharsets.UTF_8))
        val result = execute(root)
        assertTrue(result is Result.Failed)
        assertTrue(path.readBytes().contentEquals(original + " ".toByteArray(StandardCharsets.UTF_8)))
    }

    @Test
    fun `temporary partial artifact is rejected`() = withRoot { root ->
        val path = HimPositiveTrainingExamplePersistenceV1.pathFor(EXAMPLE.exampleReference, root)
        root.mkdirs()
        File(root, ".${path.name}.tmp").writeText("partial")
        assertEquals(FailureReason.PARTIAL_ARTIFACT, (execute(root) as Result.Failed).reason)
    }

    private fun execute(
        root: File,
        example: HimTrainingExampleV1 = EXAMPLE,
        enabled: Boolean = true,
    ): Result = HimPositiveTrainingExamplePersistenceV1.execute(
        HimPositiveTrainingExamplePersistenceV1.Request(example = example, durableRoot = root, enabled = enabled),
    )

    private fun created(root: File, example: HimTrainingExampleV1 = EXAMPLE): Result.Created {
        val result = execute(root, example)
        assertTrue(result.toString(), result is Result.Created)
        return result as Result.Created
    }

    private fun expectReadFailure(reference: HimTrainingExampleReference, root: File, reason: FailureReason) {
        try {
            HimPositiveTrainingExamplePersistenceV1.read(reference, root)
            fail("read must fail")
        } catch (failure: PersistenceFailure) {
            assertEquals(reason, failure.reason)
        }
    }

    private fun withRoot(block: (File) -> Unit) {
        val root = Files.createTempDirectory("him-positive-example-").toFile()
        try {
            block(root)
        } finally {
            root.walkBottomUp().forEach { it.delete() }
        }
    }

    companion object {
        private val ID = HimEntityId("Abc123")
        private val EVIDENCE = listOf(
            HimTrainingEvidenceInputV1(
                reference = HimEvidenceReference("OPEN_FOOD_FACTS", HimSha256("1".repeat(64)), "off:product:1"),
                recordKind = "FOOD",
                retrievalRank = 1,
            ),
            HimTrainingEvidenceInputV1(
                reference = HimEvidenceReference("OPEN_FOOD_FACTS", HimSha256("2".repeat(64)), "off:product:2"),
                recordKind = "FOOD",
                retrievalRank = 1,
            ),
        )
        private val EXAMPLE = example(
            target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(ID)),
            observedTerm = "Hering eingelegt",
        )

        private fun example(
            target: HimTrainingTargetV1,
            observedTerm: String,
            evidence: List<HimTrainingEvidenceInputV1> = EVIDENCE,
        ): HimTrainingExampleV1 {
            val provenance = HimTrainingProvenanceV1(
                sourceEvidenceReferences = evidence.map { it.reference },
                sourceArtifactDigests = evidence.map { it.reference.sourceArtifactSha256 }.distinct(),
            )
            return HimTrainingExampleV1.create(
                taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
                input = HimTrainingInputV1(
                    observedTerm = observedTerm,
                    normalizedObservedTerm = observedTerm.lowercase(),
                    canonicalContext = listOf(HimCandidateCanonicalContext(1, ID, "Hering", null)),
                    evidence = evidence,
                ),
                target = target,
                provenance = provenance,
            )
        }
    }
}
