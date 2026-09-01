package de.shopme.testing.system.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.training.runtime.HimModelBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingMissionV1
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimModelBindingV1Test {
    @Test
    fun validModelBindingIsValidated() {
        val binding = binding()

        assertEquals(HimModelBindingV1.CONTRACT_ID, bindingContractId())
        assertEquals(HimModelBindingV1.VERSION, bindingVersion())
        assertEquals(HimModelBindingV1.STATE, bindingState())
        assertEquals("model-family:fixture-encoder:v1", binding.modelFamilyId)
        assertEquals("base-model:fixture:v1", binding.baseModelId)
        assertEquals(HimSha256("1".repeat(64)), binding.baseModelArtifactDigest)
        assertEquals("tokenizer:fixture:v1", binding.tokenizerId)
        assertEquals(HimSha256("2".repeat(64)), binding.tokenizerArtifactDigest)
        assertEquals(HimSha256("3".repeat(64)), binding.modelConfigurationArtifactDigest)
    }

    @Test
    fun sameInputProducesIdenticalIdentity() {
        val first = binding()
        val second = binding()

        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.modelBindingReference, second.modelBindingReference)
        assertEquals(first, second)
    }

    @Test
    fun changingModelFamilyChangesLogicalDigest() {
        assertNotEquals(binding().logicalDigest, binding(modelFamilyId = "model-family:other:v1").logicalDigest)
    }

    @Test
    fun changingBaseModelIdChangesLogicalDigest() {
        assertNotEquals(binding().logicalDigest, binding(baseModelId = "base-model:other:v1").logicalDigest)
    }

    @Test
    fun changingBaseModelArtifactDigestChangesLogicalDigest() {
        assertNotEquals(
            binding().logicalDigest,
            binding(baseModelArtifactDigest = HimSha256("4".repeat(64))).logicalDigest,
        )
    }

    @Test
    fun changingTokenizerIdChangesLogicalDigest() {
        assertNotEquals(binding().logicalDigest, binding(tokenizerId = "tokenizer:other:v1").logicalDigest)
    }

    @Test
    fun changingTokenizerArtifactDigestChangesLogicalDigest() {
        assertNotEquals(
            binding().logicalDigest,
            binding(tokenizerArtifactDigest = HimSha256("5".repeat(64))).logicalDigest,
        )
    }

    @Test
    fun changingModelConfigurationArtifactDigestChangesLogicalDigest() {
        assertNotEquals(
            binding().logicalDigest,
            binding(modelConfigurationArtifactDigest = HimSha256("6".repeat(64))).logicalDigest,
        )
    }

    @Test
    fun blankModelFamilyIdFailsClosed() {
        assertFailsWith<IllegalArgumentException> { binding(modelFamilyId = "") }
    }

    @Test
    fun whitespaceOnlyModelFamilyIdFailsClosed() {
        assertFailsWith<IllegalArgumentException> { binding(modelFamilyId = " \t\n") }
    }

    @Test
    fun blankBaseModelIdFailsClosed() {
        assertFailsWith<IllegalArgumentException> { binding(baseModelId = "") }
    }

    @Test
    fun whitespaceOnlyBaseModelIdFailsClosed() {
        assertFailsWith<IllegalArgumentException> { binding(baseModelId = " \t") }
    }

    @Test
    fun blankTokenizerIdFailsClosed() {
        assertFailsWith<IllegalArgumentException> { binding(tokenizerId = "") }
    }

    @Test
    fun whitespaceOnlyTokenizerIdFailsClosed() {
        assertFailsWith<IllegalArgumentException> { binding(tokenizerId = "\n\t") }
    }

    @Test
    fun invalidBaseModelArtifactDigestFailsThroughHimSha256Boundary() {
        assertFailsWith<IllegalArgumentException> {
            HimModelBindingV1.create(
                modelFamilyId = "model-family:fixture-encoder:v1",
                baseModelId = "base-model:fixture:v1",
                baseModelArtifactDigest = HimSha256("invalid"),
                tokenizerId = "tokenizer:fixture:v1",
                tokenizerArtifactDigest = HimSha256("2".repeat(64)),
                modelConfigurationArtifactDigest = HimSha256("3".repeat(64)),
            )
        }
    }

    @Test
    fun invalidTokenizerArtifactDigestFailsThroughHimSha256Boundary() {
        assertFailsWith<IllegalArgumentException> {
            HimModelBindingV1.create(
                modelFamilyId = "model-family:fixture-encoder:v1",
                baseModelId = "base-model:fixture:v1",
                baseModelArtifactDigest = HimSha256("1".repeat(64)),
                tokenizerId = "tokenizer:fixture:v1",
                tokenizerArtifactDigest = HimSha256("invalid"),
                modelConfigurationArtifactDigest = HimSha256("3".repeat(64)),
            )
        }
    }

    @Test
    fun invalidModelConfigurationDigestFailsThroughHimSha256Boundary() {
        assertFailsWith<IllegalArgumentException> {
            HimModelBindingV1.create(
                modelFamilyId = "model-family:fixture-encoder:v1",
                baseModelId = "base-model:fixture:v1",
                baseModelArtifactDigest = HimSha256("1".repeat(64)),
                tokenizerId = "tokenizer:fixture:v1",
                tokenizerArtifactDigest = HimSha256("2".repeat(64)),
                modelConfigurationArtifactDigest = HimSha256("invalid"),
            )
        }
    }

    @Test
    fun modelBindingReferenceIsTiedToLogicalDigest() {
        val binding = binding()

        assertEquals("model-binding:v1:${binding.logicalDigest.value}", binding.modelBindingReference)
        assertEquals(64, binding.logicalDigest.value.length)
    }

    @Test
    fun publicApiContainsNoDatasetOrReadyBindingFields() {
        val forbidden = setOf("dataset", "ready", "corpus", "partition", "leakage", "snapshot")
        val fieldNames = HimModelBindingV1::class.java.declaredFields.map { it.name.lowercase() }

        assertTrue(fieldNames.none { it in forbidden })
    }

    @Test
    fun publicApiContainsNoTrainingConfigurationFields() {
        val forbidden = setOf(
            "trainingconfiguration",
            "epochs",
            "learningrate",
            "optimizer",
            "microbatchsize",
            "gradientaccumulationsteps",
            "seed",
        )
        val fieldNames = HimModelBindingV1::class.java.declaredFields.map { it.name.lowercase() }

        assertTrue(fieldNames.none { it in forbidden })
    }

    @Test
    fun publicApiContainsNoFrameworkDeviceOrRuntimeFields() {
        val forbidden = setOf(
            "framework",
            "device",
            "checkpoint",
            "trainer",
            "evaluation",
            "publication",
            "pytorch",
            "tensorflow",
            "onnx",
        )
        val fieldNames = HimModelBindingV1::class.java.declaredFields.map { it.name.lowercase() }

        assertTrue(fieldNames.none { it in forbidden })
    }

    @Test
    fun noPersistenceTrainingNetworkOrExecutionSurfaceExists() {
        val forbiddenMethods = setOf("persist", "write", "train", "execute", "run", "download", "infer")
        val methodNames = HimModelBindingV1::class.java.methods.map { it.name.lowercase() }
        val sourceConstructors = HimModelBindingV1::class.java.declaredConstructors
            .filterNot { it.isSynthetic }

        assertTrue(methodNames.none { it in forbiddenMethods })
        assertEquals(1, sourceConstructors.size)
        assertTrue(Modifier.isPrivate(sourceConstructors.single().modifiers))
    }

    @Test
    fun modelDigestIsCompatibleWithTrainingMissionModelBindingReference() {
        val binding = binding()
        val missionReference = HimTrainingMissionV1.ModelBindingReference(binding.logicalDigest)

        assertEquals(binding.logicalDigest, missionReference.digest)
        assertNotEquals(binding.baseModelArtifactDigest, missionReference.digest)
    }

    private fun binding(
        modelFamilyId: String = "model-family:fixture-encoder:v1",
        baseModelId: String = "base-model:fixture:v1",
        baseModelArtifactDigest: HimSha256 = HimSha256("1".repeat(64)),
        tokenizerId: String = "tokenizer:fixture:v1",
        tokenizerArtifactDigest: HimSha256 = HimSha256("2".repeat(64)),
        modelConfigurationArtifactDigest: HimSha256 = HimSha256("3".repeat(64)),
    ): HimModelBindingV1 =
        HimModelBindingV1.create(
            modelFamilyId = modelFamilyId,
            baseModelId = baseModelId,
            baseModelArtifactDigest = baseModelArtifactDigest,
            tokenizerId = tokenizerId,
            tokenizerArtifactDigest = tokenizerArtifactDigest,
            modelConfigurationArtifactDigest = modelConfigurationArtifactDigest,
        )

    private fun bindingContractId(): String = HimModelBindingV1.CONTRACT_ID

    private fun bindingVersion(): String = HimModelBindingV1.VERSION

    private fun bindingState(): String = HimModelBindingV1.STATE
}
