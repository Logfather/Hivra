package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimP1EvaluationDatasetV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class RunHimP1EvaluationDatasetV2Test {
    private val root: File = locateRoot()
    private val authority = HimP1EvaluationDatasetV2

    @Test
    fun completeV2DatasetIsNonEmptyAndFamilyPartitioned() {
        val materialization = authority.materialize(root)
        authority.validateCorpus(materialization.corpus)
        authority.validatePartition(materialization.corpus, materialization.partition)
        authority.validateReadiness(materialization.corpus, materialization.partition, materialization.readiness)
        assertEquals(40, materialization.corpus.examples.size)
        assertEquals(15, materialization.corpus.familyGroups.size)
        assertEquals(11, materialization.partition.trainFamilyCount)
        assertEquals(3, materialization.partition.validationFamilyCount)
        assertEquals(1, materialization.partition.holdoutFamilyCount)
        assertEquals(32, materialization.partition.trainExampleCount)
        assertEquals(6, materialization.partition.validationExampleCount)
        assertEquals(2, materialization.partition.holdoutExampleCount)
        assertEquals(HimP1EvaluationDatasetV2.READY_STATE, materialization.readiness.state)
    }

    @Test
    fun allFamilyBucketsAreExposedAndHistoricalBucketsReproduce() {
        val partition = authority.materialize(root).partition
        assertEquals(15, partition.familyAssignments.size)
        val byFamily = partition.familyAssignments.associateBy { it.familyGroupReference }
        assertEquals(7889, byFamily.getValue("family:v1:canonical:uVfHe4").bucket)
        assertEquals(9411, byFamily.getValue("family:v1:canonical:ZuhV5V").bucket)
        assertEquals(8151, byFamily.getValue("family:v1:canonical:2Mu5SI").bucket)
        assertEquals(8355, byFamily.getValue("family:v1:canonical:4LRBAE").bucket)
        assertEquals(8071, byFamily.getValue("family:v1:canonical:8LxrZr").bucket)
        assertTrue(partition.familyAssignments.all { it.hash.length == 64 && it.firstEightHex.length == 8 })
    }

    @Test
    fun targetKindsAndRelationsRemainDiagnosticPerPartition() {
        val materialization = authority.materialize(root)
        val assignments = materialization.partition.exampleAssignments
        fun counts(partition: String, selector: (HimP1EvaluationDatasetV2.ExampleAssignmentV2) -> String) =
            assignments.filter { it.partition == partition }.groupingBy(selector).eachCount().toSortedMap()
        assertEquals(mapOf("IDENTITY" to 3, "VARIANT" to 29), counts("TRAIN") { it.targetKind })
        assertEquals(mapOf("IDENTITY" to 2, "VARIANT" to 4), counts("VALIDATION") { it.targetKind })
        assertEquals(mapOf("VARIANT" to 2), counts("HOLDOUT") { it.targetKind })
        assertEquals(mapOf("IDENTITY_OF" to 3, "PREPARATION_STATE_OF" to 4, "PROCESSING_FORM_OF" to 7, "PRODUCT_FORM_OF" to 4, "VARIANT_OF" to 14), counts("TRAIN") { it.relation })
        assertEquals(mapOf("IDENTITY_OF" to 2, "PRODUCT_FORM_OF" to 4), counts("VALIDATION") { it.relation })
        assertEquals(mapOf("VARIANT_OF" to 2), counts("HOLDOUT") { it.relation })
    }

    @Test
    fun corpusAndPartitionRoundTripByteAndDigestIdentically() {
        val first = authority.materialize(root)
        val corpusBytes = Files.readAllBytes(root.resolve(HimP1EvaluationDatasetV2.DURABLE_ROOT).resolve(HimP1EvaluationDatasetV2.CORPUS_FILE).toPath())
        val partitionBytes = Files.readAllBytes(root.resolve(HimP1EvaluationDatasetV2.DURABLE_ROOT).resolve(HimP1EvaluationDatasetV2.PARTITION_FILE).toPath())
        val readinessBytes = Files.readAllBytes(root.resolve(HimP1EvaluationDatasetV2.DURABLE_ROOT).resolve(HimP1EvaluationDatasetV2.READINESS_FILE).toPath())
        val second = authority.materialize(root)
        assertEquals(first.corpus, second.corpus)
        assertEquals(first.partition, second.partition)
        assertEquals(first.readiness, second.readiness)
        assertEquals(corpusBytes.toList(), authority.serialize(second.corpus).toList())
        assertEquals(partitionBytes.toList(), authority.serialize(second.partition).toList())
        assertEquals(readinessBytes.toList(), authority.serialize(second.readiness).toList())
        assertEquals(first.corpus.logicalDigest, second.corpus.logicalDigest)
        assertEquals(first.partition.logicalDigest, second.partition.logicalDigest)
        assertEquals(first.readiness.logicalDigest, second.readiness.logicalDigest)
    }

    @Test
    fun partitionThresholdMutationFails() {
        val m = authority.materialize(root)
        assertThrows(IllegalArgumentException::class.java) { authority.validatePartition(m.corpus, m.partition.copy(trainRange = "0-8999")) }
    }

    @Test
    fun wrongPartitionKeyFails() {
        val m = authority.materialize(root)
        assertThrows(IllegalArgumentException::class.java) { authority.validatePartition(m.corpus, m.partition.copy(partitionKey = "exampleReference")) }
    }

    @Test
    fun rngAssignmentFails() {
        val m = authority.materialize(root)
        assertThrows(IllegalArgumentException::class.java) { authority.validatePartition(m.corpus, m.partition.copy(rngUsed = true)) }
    }

    @Test
    fun historicalBucketMismatchFails() {
        val m = authority.materialize(root)
        val index = m.partition.familyAssignments.indexOfFirst { it.familyGroupReference == "family:v1:canonical:uVfHe4" }
        val assignments = m.partition.familyAssignments.toMutableList()
        assignments[index] = assignments[index].copy(bucket = 7888)
        assertThrows(IllegalArgumentException::class.java) { authority.validatePartition(m.corpus, m.partition.copy(familyAssignments = assignments)) }
    }

    @Test
    fun familySplitAcrossPartitionsFails() {
        val m = authority.materialize(root)
        val assignments = m.partition.familyAssignments.toMutableList()
        assignments[0] = assignments[0].copy(partition = if (assignments[0].partition == "TRAIN") "VALIDATION" else "TRAIN")
        assertThrows(IllegalArgumentException::class.java) { authority.validatePartition(m.corpus, m.partition.copy(familyAssignments = assignments)) }
    }

    @Test
    fun exampleSplitFromFamilyFails() {
        val m = authority.materialize(root)
        val assignments = m.partition.exampleAssignments.toMutableList()
        assignments[0] = assignments[0].copy(partition = if (assignments[0].partition == "TRAIN") "HOLDOUT" else "TRAIN")
        assertThrows(IllegalArgumentException::class.java) { authority.validatePartition(m.corpus, m.partition.copy(exampleAssignments = assignments)) }
    }

    @Test
    fun duplicateCrossPartitionExampleFails() {
        val m = authority.materialize(root)
        val duplicate = m.partition.exampleAssignments + m.partition.exampleAssignments.first().copy(partition = "HOLDOUT")
        assertThrows(IllegalArgumentException::class.java) { authority.validatePartition(m.corpus, m.partition.copy(exampleAssignments = duplicate)) }
    }

    @Test
    fun aiOnlyExampleCannotEnterCorpus() {
        val m = authority.materialize(root)
        val examples = m.corpus.examples.toMutableList()
        examples[0] = examples[0].copy(originAuthority = "AI_ONLY")
        assertThrows(IllegalArgumentException::class.java) { authority.validateCorpus(m.corpus.copy(examples = examples)) }
    }

    @Test
    fun unresolvedExampleCannotEnterCorpus() {
        val m = authority.materialize(root)
        val examples = m.corpus.examples.toMutableList()
        examples[0] = examples[0].copy(authorityStatus = "UNRESOLVED")
        assertThrows(IllegalArgumentException::class.java) { authority.validateCorpus(m.corpus.copy(examples = examples)) }
    }

    @Test
    fun escalationExampleCannotEnterCorpus() {
        val m = authority.materialize(root)
        val examples = m.corpus.examples.toMutableList()
        examples[0] = examples[0].copy(authorityStatus = "ESCALATION")
        assertThrows(IllegalArgumentException::class.java) { authority.validateCorpus(m.corpus.copy(examples = examples)) }
    }

    @Test
    fun componentAuxiliaryRelationCannotBecomeModelTarget() {
        val m = authority.materialize(root)
        val examples = m.corpus.examples.toMutableList()
        examples[0] = examples[0].copy(relation = "COMPONENT_OR_DERIVED_PRODUCT_OF")
        assertThrows(IllegalArgumentException::class.java) { authority.validateCorpus(m.corpus.copy(examples = examples)) }
    }

    @Test
    fun emptyValidationCannotBecomeReady() {
        val m = authority.materialize(root)
        val blocked = authority.buildReadiness(m.corpus, m.partition.copy(validationFamilyCount = 0, validationExampleCount = 0))
        assertEquals(HimP1EvaluationDatasetV2.BLOCKED_STATE, blocked.state)
    }

    @Test
    fun emptyHoldoutCannotBecomeReady() {
        val m = authority.materialize(root)
        val blocked = authority.buildReadiness(m.corpus, m.partition.copy(holdoutFamilyCount = 0, holdoutExampleCount = 0))
        assertEquals(HimP1EvaluationDatasetV2.BLOCKED_STATE, blocked.state)
    }

    @Test
    fun emptyTrainCannotBecomeReady() {
        val m = authority.materialize(root)
        val blocked = authority.buildReadiness(m.corpus, m.partition.copy(trainFamilyCount = 0, trainExampleCount = 0))
        assertEquals(HimP1EvaluationDatasetV2.BLOCKED_STATE, blocked.state)
    }

    @Test
    fun manualFamilyReassignmentFails() {
        val m = authority.materialize(root)
        val assignments = m.partition.familyAssignments.toMutableList()
        assignments[0] = assignments[0].copy(candidateId = "manual")
        assertThrows(IllegalArgumentException::class.java) { authority.validatePartition(m.corpus, m.partition.copy(familyAssignments = assignments)) }
    }

    @Test
    fun bucketDrivenFamilySelectionFails() {
        val m = authority.materialize(root)
        assertThrows(IllegalArgumentException::class.java) { authority.validatePartition(m.corpus, m.partition.copy(familyAssignments = m.partition.familyAssignments.drop(1))) }
    }

    @Test
    fun v1CorpusMutationFails() {
        val m = authority.materialize(root)
        val mutated = m.corpus.copy(originalV1CorpusDigest = "0".repeat(64))
        assertThrows(IllegalArgumentException::class.java) { authority.validateCorpus(mutated) }
    }

    @Test
    fun v1PartitionMutationFails() {
        val m = authority.materialize(root)
        val mutated = m.corpus.copy(originalV1PartitionDigest = "0".repeat(64))
        assertThrows(IllegalArgumentException::class.java) { authority.validateCorpus(mutated) }
    }

    @Test
    fun excludedAndAIAuthorityLineageNeverEntersCorpus() {
        val m = authority.materialize(root)
        val excluded = setOf("0efHM5", "0k9QJF", "2fTXul", "3zWNPA", "7B7beg", "7SgCPz", "7kMTME")
        assertTrue(m.corpus.examples.none { it.candidateId in excluded })
        assertTrue(m.corpus.examples.all { it.originAuthority != "AI_ONLY" })
        assertEquals(0, m.corpus.examples.count { it.relation == "COMPONENT_OR_DERIVED_PRODUCT_OF" })
        assertNotEquals(0, m.corpus.examples.size)
        assertFalse(m.readiness.state == HimP1EvaluationDatasetV2.BLOCKED_STATE)
    }

    private fun locateRoot(): File {
        var current = File(System.getProperty("user.dir")).canonicalFile
        while (current.parentFile != null) {
            if (current.resolve("data").isDirectory && current.resolve("him-server").isDirectory) return current
            current = current.parentFile
        }
        error("ShopMe repository root not found")
    }
}
