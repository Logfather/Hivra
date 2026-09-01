package de.shopme.testing.system.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.training.runtime.HimAdamWParametersV1
import de.shopme.tools.knowledge.him.training.runtime.HimOptimizerMappingV1
import java.lang.reflect.Modifier
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimAdamWParametersV1Test {
    @Test
    fun validAdamWParametersAreCreated() {
        val parameters = parameters()

        assertEquals(BigDecimal("0.9"), parameters.beta1)
        assertEquals(BigDecimal("0.999"), parameters.beta2)
        assertEquals(BigDecimal("0.00000001"), parameters.epsilon)
        assertEquals(BigDecimal("0.01"), parameters.weightDecay)
    }

    @Test
    fun contractIdentityIsValid() {
        assertEquals("HIM_ADAMW_PARAMETERS_V1", parametersContractId())
        assertEquals("1", parametersVersion())
        assertEquals("ADAMW_PARAMETERS_VALIDATED", parametersState())
    }

    @Test
    fun referenceBindsLogicalDigest() {
        val parameters = parameters()

        assertEquals("adamw-parameters:v1:${parameters.logicalDigest.value}", parameters.parametersReference)
    }

    @Test
    fun repeatedCreationHasIdenticalIdentity() {
        val first = parameters()
        val second = parameters()

        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.parametersReference, second.parametersReference)
        assertEquals(first, second)
    }

    @Test
    fun changingBeta1ChangesDigest() {
        assertNotEquals(parameters().logicalDigest, parameters(beta1 = "0.8").logicalDigest)
    }

    @Test
    fun changingBeta2ChangesDigest() {
        assertNotEquals(parameters().logicalDigest, parameters(beta2 = "0.998").logicalDigest)
    }

    @Test
    fun changingEpsilonChangesDigest() {
        assertNotEquals(parameters().logicalDigest, parameters(epsilon = "0.000000001").logicalDigest)
    }

    @Test
    fun changingWeightDecayChangesDigest() {
        assertNotEquals(parameters().logicalDigest, parameters(weightDecay = "0.02").logicalDigest)
    }

    @Test
    fun beta1CanonicalEquivalentValuesHaveIdenticalIdentity() {
        assertEquals(parameters(beta1 = "0.9"), parameters(beta1 = "0.9000"))
    }

    @Test
    fun beta2CanonicalEquivalentValuesHaveIdenticalIdentity() {
        assertEquals(parameters(beta2 = "0.999"), parameters(beta2 = "0.9990"))
    }

    @Test
    fun epsilonCanonicalEquivalentValuesHaveIdenticalIdentity() {
        assertEquals(parameters(epsilon = "1E-8"), parameters(epsilon = "0.00000001"))
    }

    @Test
    fun weightDecayCanonicalEquivalentValuesHaveIdenticalIdentity() {
        assertEquals(parameters(weightDecay = "0.0100"), parameters(weightDecay = "0.01"))
    }

    @Test
    fun beta1ZeroIsWithinAdamBounds() {
        assertEquals(BigDecimal.ZERO, parameters(beta1 = "0").beta1)
    }

    @Test
    fun beta1BelowZeroFailsClosed() {
        assertFailsWith<IllegalArgumentException> { parameters(beta1 = "-0.1") }
    }

    @Test
    fun beta1AtOneFailsClosed() {
        assertFailsWith<IllegalArgumentException> { parameters(beta1 = "1") }
    }

    @Test
    fun beta1AboveOneFailsClosed() {
        assertFailsWith<IllegalArgumentException> { parameters(beta1 = "1.0001") }
    }

    @Test
    fun beta2BelowZeroFailsClosed() {
        assertFailsWith<IllegalArgumentException> { parameters(beta2 = "-0.1") }
    }

    @Test
    fun beta2AtOneFailsClosed() {
        assertFailsWith<IllegalArgumentException> { parameters(beta2 = "1") }
    }

    @Test
    fun beta2AboveOneFailsClosed() {
        assertFailsWith<IllegalArgumentException> { parameters(beta2 = "1.0001") }
    }

    @Test
    fun epsilonZeroFailsClosed() {
        assertFailsWith<IllegalArgumentException> { parameters(epsilon = "0") }
    }

    @Test
    fun epsilonBelowZeroFailsClosed() {
        assertFailsWith<IllegalArgumentException> { parameters(epsilon = "-1E-8") }
    }

    @Test
    fun weightDecayBelowZeroFailsClosed() {
        assertFailsWith<IllegalArgumentException> { parameters(weightDecay = "-0.01") }
    }

    @Test
    fun weightDecayZeroIsValid() {
        assertEquals(BigDecimal.ZERO, parameters(weightDecay = "0").weightDecay)
    }

    @Test
    fun learningRateIsNotAParameterField() {
        val fields = HimAdamWParametersV1::class.java.declaredFields.map { it.name }

        assertTrue(fields.none { it.equals("learningRate", ignoreCase = true) })
    }

    @Test
    fun algorithmInvariantsAreNotCallerConfigurable() {
        val constructorTypes = HimAdamWParametersV1::class.java.declaredConstructors
            .flatMap { it.parameterTypes.toList() }

        assertFalse(constructorTypes.contains(Boolean::class.javaPrimitiveType))
        assertEquals(true, HimAdamWParametersV1.DECOUPLED_WEIGHT_DECAY)
        assertEquals(true, HimAdamWParametersV1.BIAS_CORRECTION)
    }

    @Test
    fun profileInvariantMetadataIsExactlyTrueAndTrue() {
        assertTrue(HimAdamWParametersV1.DECOUPLED_WEIGHT_DECAY)
        assertTrue(HimAdamWParametersV1.BIAS_CORRECTION)
    }

    @Test
    fun noFrameworkSpecificFieldExists() {
        val names = HimAdamWParametersV1::class.java.declaredFields.map { it.name }

        assertTrue(names.none { it.contains("torch", ignoreCase = true) })
        assertTrue(names.none { it.contains("python", ignoreCase = true) })
        assertTrue(names.none { it.contains("tensorflow", ignoreCase = true) })
    }

    @Test
    fun noOptimizerExecutionApiExists() {
        val methodNames = HimAdamWParametersV1::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) }
            .map { it.name }

        assertTrue(methodNames.none { it.contains("execute", ignoreCase = true) })
        assertTrue(methodNames.none { it.contains("step", ignoreCase = true) })
        assertTrue(methodNames.none { it.contains("update", ignoreCase = true) })
    }

    @Test
    fun noSchedulerFieldExists() {
        assertTrue(HimAdamWParametersV1::class.java.declaredFields.none {
            it.name.contains("scheduler", ignoreCase = true)
        })
    }

    @Test
    fun noParameterGroupFieldExists() {
        assertTrue(HimAdamWParametersV1::class.java.declaredFields.none {
            it.name.contains("parameterGroup", ignoreCase = true)
        })
    }

    @Test
    fun mappingUnresolvedParametersMatchExactly() {
        assertEquals(
            listOf(
                "beta1",
                "beta2",
                "epsilon",
                "weight-decay",
                "decoupled-weight-decay",
                "bias-correction",
            ),
            HimOptimizerMappingV1.UNRESOLVED_MANDATORY_PARAMETERS,
        )
    }

    @Test
    fun mappingDoesNotResolveAlgorithmInvariantsThroughParameters() {
        val unresolved = HimOptimizerMappingV1.UNRESOLVED_MANDATORY_PARAMETERS

        assertTrue(unresolved.contains("decoupled-weight-decay"))
        assertTrue(unresolved.contains("bias-correction"))
    }

    @Test
    fun noTrainingConfigurationOrTrainerProtocolDependencyExists() {
        val names = HimAdamWParametersV1::class.java.declaredFields.map { it.name }

        assertTrue(names.none { it.contains("configuration", ignoreCase = true) })
        assertTrue(names.none { it.contains("protocol", ignoreCase = true) })
    }

    @Test
    fun noPersistenceNetworkOrTrainingApiExists() {
        val methodNames = HimAdamWParametersV1::class.java.declaredMethods.map { it.name }

        assertTrue(methodNames.none { it.contains("persist", ignoreCase = true) })
        assertTrue(methodNames.none { it.contains("network", ignoreCase = true) })
        assertTrue(methodNames.none { it.contains("train", ignoreCase = true) })
    }

    @Test
    fun digestIsAValidatedSha256Value() {
        assertEquals(64, parameters().logicalDigest.value.length)
        assertTrue(parameters().logicalDigest.value.matches(Regex("[0-9a-f]{64}")))
    }

    private fun parameters(
        beta1: String = "0.9",
        beta2: String = "0.999",
        epsilon: String = "0.00000001",
        weightDecay: String = "0.01",
    ): HimAdamWParametersV1 = HimAdamWParametersV1.create(
        beta1 = BigDecimal(beta1),
        beta2 = BigDecimal(beta2),
        epsilon = BigDecimal(epsilon),
        weightDecay = BigDecimal(weightDecay),
    )

    private fun parametersContractId(): String = HimAdamWParametersV1.CONTRACT_ID

    private fun parametersVersion(): String = HimAdamWParametersV1.VERSION

    private fun parametersState(): String = HimAdamWParametersV1.STATE
}
