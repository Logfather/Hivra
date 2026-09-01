package de.shopme.testing.system.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.training.runtime.HimTrainingConfigurationV1
import java.lang.reflect.Modifier
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimTrainingConfigurationV1Test {
    @Test
    fun validConfigurationIsValidated() {
        val configuration = configuration()

        assertEquals(HimTrainingConfigurationV1.CONTRACT_ID, configurationContractId())
        assertEquals(HimTrainingConfigurationV1.VERSION, configurationVersion())
        assertEquals(HimTrainingConfigurationV1.STATE, configurationState())
        assertEquals(7L, configuration.seed)
        assertEquals(3, configuration.epochs)
        assertEquals(8, configuration.microBatchSize)
        assertEquals(4, configuration.gradientAccumulationSteps)
        assertEquals("0.0001", configuration.learningRate.toPlainString())
        assertEquals("optimizer:adamw:v1", configuration.optimizerId)
    }

    @Test
    fun configurationReferenceBindsLogicalDigest() {
        val configuration = configuration()

        assertEquals(
            "training-configuration:v1:${configuration.logicalDigest.value}",
            configuration.configurationReference,
        )
    }

    @Test
    fun repeatedCreationIsDeterministic() {
        val first = configuration()
        val second = configuration()

        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.configurationReference, second.configurationReference)
        assertEquals(first, second)
    }

    @Test
    fun equivalentDecimalRepresentationsHaveIdenticalIdentity() {
        val first = configuration(learningRate = BigDecimal("0.0001"))
        val second = configuration(learningRate = BigDecimal("1E-4"))

        assertEquals("0.0001", first.learningRate.toPlainString())
        assertEquals(first.learningRate, second.learningRate)
        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.configurationReference, second.configurationReference)
    }

    @Test
    fun changingSeedChangesIdentity() {
        assertNotEquals(configuration().logicalDigest, configuration(seed = 8L).logicalDigest)
    }

    @Test
    fun changingEpochsChangesIdentity() {
        assertNotEquals(configuration().logicalDigest, configuration(epochs = 4).logicalDigest)
    }

    @Test
    fun changingMicroBatchSizeChangesIdentity() {
        assertNotEquals(configuration().logicalDigest, configuration(microBatchSize = 16).logicalDigest)
    }

    @Test
    fun changingGradientAccumulationChangesIdentity() {
        assertNotEquals(
            configuration().logicalDigest,
            configuration(gradientAccumulationSteps = 8).logicalDigest,
        )
    }

    @Test
    fun changingLearningRateChangesIdentity() {
        assertNotEquals(
            configuration().logicalDigest,
            configuration(learningRate = BigDecimal("0.0002")).logicalDigest,
        )
    }

    @Test
    fun changingOptimizerChangesIdentity() {
        assertNotEquals(
            configuration().logicalDigest,
            configuration(optimizerId = "optimizer:sgd:v1").logicalDigest,
        )
    }

    @Test
    fun zeroSeedIsAllowed() {
        assertEquals(0L, configuration(seed = 0L).seed)
    }

    @Test
    fun negativeSeedIsAllowed() {
        assertEquals(-7L, configuration(seed = -7L).seed)
    }

    @Test
    fun zeroEpochsFailClosed() {
        assertFailsWith<IllegalArgumentException> { configuration(epochs = 0) }
    }

    @Test
    fun negativeEpochsFailClosed() {
        assertFailsWith<IllegalArgumentException> { configuration(epochs = -1) }
    }

    @Test
    fun zeroMicroBatchSizeFailsClosed() {
        assertFailsWith<IllegalArgumentException> { configuration(microBatchSize = 0) }
    }

    @Test
    fun negativeGradientAccumulationFailsClosed() {
        assertFailsWith<IllegalArgumentException> {
            configuration(gradientAccumulationSteps = -1)
        }
    }

    @Test
    fun zeroLearningRateFailsClosed() {
        assertFailsWith<IllegalArgumentException> {
            configuration(learningRate = BigDecimal.ZERO)
        }
    }

    @Test
    fun negativeLearningRateFailsClosed() {
        assertFailsWith<IllegalArgumentException> {
            configuration(learningRate = BigDecimal("-0.1"))
        }
    }

    @Test
    fun blankOrWhitespaceOptimizerFailsClosed() {
        assertFailsWith<IllegalArgumentException> { configuration(optimizerId = "") }
        assertFailsWith<IllegalArgumentException> { configuration(optimizerId = " ") }
    }

    @Test
    fun effectiveBatchSizeIsDerivedDeterministically() {
        assertEquals(32L, configuration().effectiveBatchSize)
    }

    @Test
    fun configurationHasNoDatasetModelOrExecutionSurface() {
        val forbiddenNames = setOf(
            "dataset",
            "corpus",
            "model",
            "tokenizer",
            "trainer",
            "checkpoint",
            "evaluation",
            "publication",
            "promotion",
        )
        val declaredFieldNames = HimTrainingConfigurationV1::class.java.declaredFields.map { it.name }
        val publicMethodNames = HimTrainingConfigurationV1::class.java.methods.map { it.name }
        val sourceConstructors = HimTrainingConfigurationV1::class.java.declaredConstructors
            .filterNot { it.isSynthetic }

        assertTrue(declaredFieldNames.none { it.lowercase() in forbiddenNames })
        assertTrue(publicMethodNames.none { it in setOf("execute", "train", "persist", "write", "infer") })
        assertEquals(1, sourceConstructors.size)
        assertTrue(Modifier.isPrivate(sourceConstructors.single().modifiers))
    }

    @Test
    fun configurationDoesNotExposeEnvironmentOrRandomInputs() {
        val names = HimTrainingConfigurationV1::class.java.methods.map { it.name }.toSet()

        assertFalse(names.any { it.contains("environment", ignoreCase = true) })
        assertFalse(names.any { it.contains("random", ignoreCase = true) })
        assertFalse(names.any { it.contains("file", ignoreCase = true) })
    }

    private fun configuration(
        seed: Long = 7L,
        epochs: Int = 3,
        microBatchSize: Int = 8,
        gradientAccumulationSteps: Int = 4,
        learningRate: BigDecimal = BigDecimal("0.0001"),
        optimizerId: String = "optimizer:adamw:v1",
    ): HimTrainingConfigurationV1 =
        HimTrainingConfigurationV1.create(
            seed = seed,
            epochs = epochs,
            microBatchSize = microBatchSize,
            gradientAccumulationSteps = gradientAccumulationSteps,
            learningRate = learningRate,
            optimizerId = optimizerId,
        )

    private fun configurationContractId(): String = HimTrainingConfigurationV1.CONTRACT_ID

    private fun configurationVersion(): String = HimTrainingConfigurationV1.VERSION

    private fun configurationState(): String = HimTrainingConfigurationV1.STATE
}
