package de.shopme.testing.system.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.training.runtime.HimOptimizerMappingV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingConfigurationV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainerProtocolV1
import java.lang.reflect.Modifier
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimOptimizerMappingV1Test {
    @Test
    fun contractIdentityIsValid() {
        val mapping = mapping()

        assertEquals("HIM_OPTIMIZER_MAPPING_V1", mapping.contractId)
        assertEquals("1", mapping.version)
        assertEquals("OPTIMIZER_MAPPING_DEFINED", mapping.state)
    }

    @Test
    fun mappingReferenceBindsDigest() {
        val mapping = mapping()

        assertEquals("optimizer-mapping:v1:${mapping.logicalDigest.value}", mapping.mappingReference)
    }

    @Test
    fun repeatedCreationIsDeterministic() {
        val first = mapping()
        val second = mapping()

        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.mappingReference, second.mappingReference)
        assertEquals(first, second)
    }

    @Test
    fun exactSupportedOptimizerSetIsFrozen() {
        assertEquals(listOf("optimizer:adamw:v1"), mapping().supportedOptimizerIds)
    }

    @Test
    fun supportedAlgorithmSetIsFrozen() {
        assertEquals(listOf(HimOptimizerMappingV1.AlgorithmKind.ADAMW), mapping().supportedAlgorithmKinds)
    }

    @Test
    fun adamwResolves() {
        assertEquals("optimizer:adamw:v1", mapping().resolve("optimizer:adamw:v1").optimizerId)
    }

    @Test
    fun configurationOptimizerIdResolves() {
        val configuration = configuration()

        assertEquals(configuration.optimizerId, mapping().resolve(configuration).optimizerId)
    }

    @Test
    fun resolvedAlgorithmKindIsExact() {
        assertEquals(
            HimOptimizerMappingV1.AlgorithmKind.ADAMW,
            mapping().resolve("optimizer:adamw:v1").algorithmKind,
        )
    }

    @Test
    fun resolvedMappingVersionIsExact() {
        assertEquals("1", mapping().resolve("optimizer:adamw:v1").mappingVersion)
    }

    @Test
    fun resolvedMappingDigestBindsMapping() {
        val mapping = mapping()

        assertEquals(mapping.logicalDigest, mapping.resolve("optimizer:adamw:v1").mappingDigest)
    }

    @Test
    fun resolvedReferenceBindsResolvedDigest() {
        val resolved = mapping().resolve("optimizer:adamw:v1")

        assertEquals("optimizer-spec:v1:${resolved.logicalDigest.value}", resolved.optimizerReference)
    }

    @Test
    fun optimizerProfileIsExplicitlyIncomplete() {
        assertEquals(
            HimOptimizerMappingV1.ProfileCompleteness.INCOMPLETE,
            mapping().profileCompleteness,
        )
    }

    @Test
    fun resolvedProfileIsExplicitlyIncomplete() {
        assertEquals(
            HimOptimizerMappingV1.ProfileCompleteness.INCOMPLETE,
            mapping().resolve("optimizer:adamw:v1").profileCompleteness,
        )
    }

    @Test
    fun runtimeIsNotReadyWithoutMandatoryParameters() {
        assertFalse(mapping().resolve("optimizer:adamw:v1").runtimeReady)
    }

    @Test
    fun unresolvedMandatoryParametersAreCompleteAndOrdered() {
        assertEquals(
            listOf("beta1", "beta2", "epsilon", "weight-decay", "decoupled-weight-decay", "bias-correction"),
            mapping().unresolvedMandatoryParameters,
        )
    }

    @Test
    fun resolvedUnresolvedParametersMatchMapping() {
        val mapping = mapping()

        assertEquals(
            mapping.unresolvedMandatoryParameters,
            mapping.resolve("optimizer:adamw:v1").unresolvedMandatoryParameters,
        )
    }

    @Test
    fun unknownOptimizerFailsClosed() {
        assertFailsWith<IllegalStateException> { mapping().resolve("optimizer:sgd:v1") }
    }

    @Test
    fun noFallbackOptimizerExists() {
        assertFailsWith<IllegalStateException> { mapping().resolve("optimizer:unknown:v1") }
    }

    @Test
    fun blankOptimizerFailsClosed() {
        assertFailsWith<IllegalArgumentException> { mapping().resolve("") }
    }

    @Test
    fun whitespaceOnlyOptimizerFailsClosed() {
        assertFailsWith<IllegalArgumentException> { mapping().resolve(" ") }
    }

    @Test
    fun caseMutatedOptimizerDoesNotResolve() {
        assertFailsWith<IllegalArgumentException> { mapping().resolve("Optimizer:AdamW:V1") }
    }

    @Test
    fun trailingSpaceDoesNotResolve() {
        assertFailsWith<IllegalArgumentException> { mapping().resolve("optimizer:adamw:v1 ") }
    }

    @Test
    fun supportedOptimizerOrderingIsDeterministic() {
        assertEquals(mapping().supportedOptimizerIds, mapping().supportedOptimizerIds.toList())
        assertEquals(listOf("optimizer:adamw:v1"), mapping().supportedOptimizerIds)
    }

    @Test
    fun algorithmEnumIsSemanticNotOrdinal() {
        val resolved = mapping().resolve("optimizer:adamw:v1")

        assertEquals("ADAMW", resolved.algorithmKind.name)
        assertTrue(resolved.algorithmKind == HimOptimizerMappingV1.AlgorithmKind.ADAMW)
    }

    @Test
    fun frameworkSpecificNamesAreNotMappingAuthority() {
        val mappingFields = HimOptimizerMappingV1::class.java.declaredFields.map { it.name }

        assertTrue(mappingFields.none { it.contains("torch", ignoreCase = true) })
        assertTrue(mappingFields.none { it.contains("python", ignoreCase = true) })
        assertTrue(mappingFields.none { it.contains("tensorflow", ignoreCase = true) })
    }

    @Test
    fun mappingHasNoSchedulerSemantics() {
        val fields = HimOptimizerMappingV1::class.java.declaredFields.map { it.name }

        assertTrue(fields.none { it.contains("scheduler", ignoreCase = true) })
    }

    @Test
    fun mappingHasNoParameterGroupSemantics() {
        val fields = HimOptimizerMappingV1::class.java.declaredFields.map { it.name }

        assertTrue(fields.none { it.contains("parameterGroup", ignoreCase = true) })
    }

    @Test
    fun resolvedSpecHasNoTrainingMissionOrDatasetBinding() {
        val fields = HimOptimizerMappingV1.Resolved::class.java.declaredFields.map { it.name }

        assertTrue(fields.none { it.contains("mission", ignoreCase = true) })
        assertTrue(fields.none { it.contains("dataset", ignoreCase = true) })
    }

    @Test
    fun resolvedSpecHasNoModelBindingDependency() {
        val fields = HimOptimizerMappingV1.Resolved::class.java.declaredFields.map { it.name }

        assertTrue(fields.none { it.contains("model", ignoreCase = true) })
    }

    @Test
    fun trainerProtocolCarriesSemanticOptimizerId() {
        val optimizerField = HimTrainerProtocolV1.Configuration::class.java
            .declaredFields
            .single { it.name == "optimizerId" }

        assertEquals(String::class.java, optimizerField.type)
    }

    @Test
    fun configurationDifferencesOutsideOptimizerDoNotChangeResolvedMapping() {
        val first = mapping().resolve(configuration(seed = 1L))
        val second = mapping().resolve(configuration(seed = 2L))

        assertEquals(first, second)
    }

    @Test
    fun resolvedIdentityIsDeterministic() {
        val first = mapping().resolve("optimizer:adamw:v1")
        val second = mapping().resolve("optimizer:adamw:v1")

        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.optimizerReference, second.optimizerReference)
    }

    @Test
    fun resolvedIdentityDiffersFromMappingIdentity() {
        val mapping = mapping()

        assertNotEquals(mapping.logicalDigest, mapping.resolve("optimizer:adamw:v1").logicalDigest)
    }

    @Test
    fun publicMappingApiDoesNotExposeExecutionOperation() {
        val methodNames = HimOptimizerMappingV1::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) }
            .map { it.name }
            .toSet()

        assertTrue(methodNames.contains("resolve"))
        assertTrue(methodNames.none { it.contains("execute", ignoreCase = true) })
        assertTrue(methodNames.none { it.contains("step", ignoreCase = true) })
    }

    @Test
    fun mappingHasNoPythonPyTorchOrProcessApi() {
        val names = (HimOptimizerMappingV1::class.java.declaredFields.asSequence() +
            HimOptimizerMappingV1::class.java.declaredMethods.asSequence())
            .map { it.name }
            .toList()

        assertTrue(names.none { it.contains("python", ignoreCase = true) })
        assertTrue(names.none { it.contains("pytorch", ignoreCase = true) })
        assertTrue(names.none { it.contains("process", ignoreCase = true) })
    }

    @Test
    fun mappingHasNoPersistenceNetworkOrTrainingApi() {
        val methodNames = HimOptimizerMappingV1::class.java.declaredMethods.map { it.name }

        assertTrue(methodNames.none { it.contains("persist", ignoreCase = true) })
        assertTrue(methodNames.none { it.contains("network", ignoreCase = true) })
        assertTrue(methodNames.none { it.contains("train", ignoreCase = true) })
    }

    @Test
    fun mappingDoesNotBindNumericalDefaults() {
        val unresolved = mapping().unresolvedMandatoryParameters

        assertTrue(unresolved.containsAll(listOf("beta1", "beta2", "epsilon")))
        assertTrue(unresolved.containsAll(listOf("weight-decay", "decoupled-weight-decay", "bias-correction")))
    }

    private fun mapping(): HimOptimizerMappingV1 = HimOptimizerMappingV1.create()

    private fun configuration(seed: Long = 7L): HimTrainingConfigurationV1 =
        HimTrainingConfigurationV1.create(
            seed = seed,
            epochs = 3,
            microBatchSize = 8,
            gradientAccumulationSteps = 4,
            learningRate = BigDecimal("0.0001"),
            optimizerId = "optimizer:adamw:v1",
        )
}
