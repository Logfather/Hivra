package de.shopme.testing.system.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExamplePolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupReferenceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionAssignmentV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionContractV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionManifestV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionRecordV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import de.shopme.tools.knowledge.him.training.runtime.HimAdamWParametersV1
import de.shopme.tools.knowledge.him.training.runtime.HimAdamWRuntimeBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimModelBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimOptimizerMappingV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainerPortV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainerProtocolAdamWRuntimeBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainerProtocolV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingConfigurationV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingMissionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1
import java.lang.reflect.Modifier
import java.math.BigDecimal
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

private fun digest(seed: String): HimSha256 = HimSha256(
    MessageDigest.getInstance("SHA-256")
        .digest(seed.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) },
)

class RunHimTrainerProtocolAdamWRuntimeBindingV1Test {
    @Test
    fun validProtocolRequestAndAdamWBindingCreateBinding() {
        val result = binding()

        assertEquals("HIM_TRAINER_PROTOCOL_ADAMW_RUNTIME_BINDING_V1", result.contractId)
        assertEquals("1", result.version)
        assertEquals("TRAINER_PROTOCOL_ADAMW_RUNTIME_BOUND", result.state)
    }

    @Test
    fun protocolRequestIsMandatory() {
        assertEquals(
            HimTrainerProtocolV1.Request::class.java,
            publicCreate().parameterTypes[0],
        )
    }

    @Test
    fun adamWRuntimeBindingIsMandatory() {
        assertEquals(
            HimAdamWRuntimeBindingV1::class.java,
            publicCreate().parameterTypes[1],
        )
    }

    @Test
    fun noMissionOnlyBindingApiExists() {
        assertEquals(2, publicCreate().parameterTypes.size)
        assertFalse(publicCreate().parameterTypes.contains(HimTrainingMissionV1.Mission::class.java))
    }

    @Test
    fun noConfigurationOnlyBindingApiExists() {
        assertFalse(publicCreate().parameterTypes.contains(HimTrainingConfigurationV1::class.java))
    }

    @Test
    fun noParametersOnlyBindingApiExists() {
        assertFalse(publicCreate().parameterTypes.contains(HimAdamWParametersV1::class.java))
    }

    @Test
    fun onlyAuthoritativeInputsAreAccepted() {
        assertEquals(
            listOf(HimTrainerProtocolV1.Request::class.java, HimAdamWRuntimeBindingV1::class.java),
            publicCreate().parameterTypes.toList(),
        )
    }

    @Test
    fun missionIdentityMatchesExactly() {
        val fixture = Fixture.valid()
        val result = binding(fixture)

        assertEquals(fixture.mission.logicalDigest, result.trainingMissionDigest)
        assertEquals(fixture.mission.missionReference, result.trainingMissionReference)
    }

    @Test
    fun missionMismatchFailsClosed() {
        assertFailsWith<IllegalArgumentException> {
            HimTrainerProtocolAdamWRuntimeBindingV1.create(
                Fixture.changedInput().protocolRequest,
                Fixture.valid().runtimeBinding,
            )
        }
    }

    @Test
    fun configurationIdentityMatchesExactly() {
        val fixture = Fixture.valid()
        val result = binding(fixture)

        assertEquals(fixture.configuration.logicalDigest, result.trainingConfigurationDigest)
        assertEquals(fixture.configuration.configurationReference, result.trainingConfigurationReference)
    }

    @Test
    fun configurationMismatchFailsClosed() {
        assertFailsWith<IllegalArgumentException> {
            HimTrainerProtocolAdamWRuntimeBindingV1.create(
                Fixture.changedConfiguration().protocolRequest,
                Fixture.valid().runtimeBinding,
            )
        }
    }

    @Test
    fun optimizerIdMatchesExactly() {
        assertEquals(HimAdamWRuntimeBindingV1.BOUND_OPTIMIZER_ID, binding().optimizerId)
        assertEquals(binding().optimizerId, Fixture.valid().protocolRequest.configuration.optimizerId)
    }

    @Test
    fun nonAdamWProtocolRequestCannotBindToAdamWRuntimeBinding() {
        assertFailsWith<IllegalStateException> {
            Fixture.nonAdamW()
        }
    }

    @Test
    fun learningRateMatchesExactly() {
        val result = binding()

        assertEquals("0.01", result.learningRate.toPlainString())
        assertEquals(result.learningRate.toPlainString(), Fixture.valid().protocolRequest.configuration.learningRate)
    }

    @Test
    fun learningRateMismatchFailsClosed() {
        assertFailsWith<IllegalArgumentException> {
            HimTrainerProtocolAdamWRuntimeBindingV1.create(
                Fixture.changedLearningRate().protocolRequest,
                Fixture.valid().runtimeBinding,
            )
        }
    }

    @Test
    fun completeAdamWRuntimeBindingIsRequired() {
        val result = binding()

        assertEquals(HimAdamWRuntimeBindingV1.ProfileCompleteness.COMPLETE, result.adamwRuntimeBinding.profileCompleteness)
        assertTrue(result.adamwRuntimeBinding.runtimeReady)
    }

    @Test
    fun incompleteOptimizerMappingCannotSubstituteForRuntimeBinding() {
        assertEquals(HimOptimizerMappingV1.ProfileCompleteness.INCOMPLETE, Fixture.valid().mapping.profileCompleteness)
        assertFalse(publicCreate().parameterTypes.contains(HimOptimizerMappingV1::class.java))
    }

    @Test
    fun AdamWParametersAloneCannotSubstituteForRuntimeBinding() {
        assertFalse(publicCreate().parameterTypes.contains(HimAdamWParametersV1::class.java))
    }

    @Test
    fun completeOptimizerSemanticsArePreservedExactly() {
        val result = binding()

        assertEquals("optimizer:adamw:v1", result.optimizerId)
        assertEquals(HimOptimizerMappingV1.AlgorithmKind.ADAMW, result.algorithm)
        assertEquals(BigDecimal("0.01"), result.learningRate)
        assertEquals(BigDecimal("0.9"), result.beta1)
        assertEquals(BigDecimal("0.999"), result.beta2)
        assertEquals(BigDecimal("0.00000001"), result.epsilon)
        assertEquals(BigDecimal("0.01"), result.weightDecay)
    }

    @Test
    fun decoupledWeightDecayRemainsTrue() {
        assertTrue(binding().decoupledWeightDecay)
    }

    @Test
    fun biasCorrectionRemainsTrue() {
        assertTrue(binding().biasCorrection)
    }

    @Test
    fun beta1IsPreserved() {
        assertEquals(Fixture.valid().runtimeBinding.beta1, binding().beta1)
    }

    @Test
    fun beta2IsPreserved() {
        assertEquals(Fixture.valid().runtimeBinding.beta2, binding().beta2)
    }

    @Test
    fun epsilonIsPreserved() {
        assertEquals(Fixture.valid().runtimeBinding.epsilon, binding().epsilon)
    }

    @Test
    fun weightDecayIsPreserved() {
        assertEquals(Fixture.valid().runtimeBinding.weightDecay, binding().weightDecay)
    }

    @Test
    fun implementationFingerprintContinuityIsTransitive() {
        val fixture = Fixture.valid()
        val result = binding(fixture)

        assertEquals(fixture.mission.implementationBinding.fingerprint, result.implementationFingerprint)
        assertEquals(fixture.protocolRequest.implementationFingerprint, result.implementationFingerprint)
    }

    @Test
    fun protocolRequestIdentityIsPreserved() {
        val fixture = Fixture.valid()
        val result = binding(fixture)

        assertSame(fixture.protocolRequest, result.protocolRequest)
        assertEquals(fixture.protocolRequest.logicalDigest, result.protocolRequestDigest)
        assertEquals(fixture.protocolRequest.requestReference, result.protocolRequestReference)
    }

    @Test
    fun protocolTrainRecordsAreNotRebuiltOrReordered() {
        val fixture = Fixture.valid()
        val result = binding(fixture)

        assertSame(fixture.protocolRequest.records, result.protocolRequest.records)
        assertEquals(fixture.protocolRequest.records.map { it.assignmentIndex }, result.protocolRequest.records.map { it.assignmentIndex })
    }

    @Test
    fun manyToOneSemanticsRemainUntouched() {
        val records = binding().protocolRequest.records

        assertEquals(3, records.size)
        assertEquals(1, records.map { it.groupReference }.distinct().size)
        assertEquals(3, records.map { it.recordReference }.distinct().size)
        assertEquals(1, records.map { it.positiveExampleReference }.distinct().size)
    }

    @Test
    fun objectiveIsNotReimplemented() {
        val fixture = Fixture.valid()
        val result = binding(fixture)

        assertEquals(fixture.protocolRequest.objectiveDigest, result.protocolRequest.objectiveDigest)
        assertEquals(fixture.protocolRequest.objectiveReference, result.protocolRequest.objectiveReference)
    }

    @Test
    fun targetEncodingIsNotReimplemented() {
        val fixture = Fixture.valid()
        val result = binding(fixture)

        assertEquals(fixture.protocolRequest.targetEncodingDigest, result.protocolRequest.targetEncodingDigest)
        assertEquals(fixture.protocolRequest.targetEncodingReference, result.protocolRequest.targetEncodingReference)
    }

    @Test
    fun identicalInputsCreateIdenticalBindingDigestAndReference() {
        assertEquals(binding(), binding())
        assertEquals(binding().logicalDigest, binding().logicalDigest)
        assertEquals(binding().bindingReference, binding().bindingReference)
    }

    @Test
    fun changingProtocolRequestIdentityChangesBindingDigest() {
        val first = Fixture.valid()
        val second = Fixture.changedInput()

        assertNotEquals(first.protocolRequest.logicalDigest, second.protocolRequest.logicalDigest)
        assertNotEquals(binding(first).logicalDigest, binding(second).logicalDigest)
    }

    @Test
    fun changingAdamWRuntimeBindingIdentityChangesBindingDigest() {
        val fixture = Fixture.valid()
        val first = HimTrainerProtocolAdamWRuntimeBindingV1.create(fixture.protocolRequest, fixture.runtimeBinding)
        val second = HimTrainerProtocolAdamWRuntimeBindingV1.create(
            fixture.protocolRequest,
            fixture.alternateRuntimeBinding,
        )

        assertNotEquals(fixture.runtimeBinding.logicalDigest, fixture.alternateRuntimeBinding.logicalDigest)
        assertNotEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun bindingReferenceIsBoundToLogicalDigest() {
        val result = binding()

        assertEquals(
            "trainer-protocol-adamw-runtime-binding:v1:${result.logicalDigest.value}",
            result.bindingReference,
        )
    }

    @Test
    fun bindingReferenceHasCanonicalShape() {
        assertTrue(binding().bindingReference.matches(Regex("trainer-protocol-adamw-runtime-binding:v1:[0-9a-f]{64}")))
    }

    @Test
    fun noFrameworkSpecificBindingExists() {
        val names = allDeclaredNames()

        assertTrue(names.none { it.contains("torch", ignoreCase = true) })
        assertTrue(names.none { it.contains("python", ignoreCase = true) })
        assertTrue(names.none { it.contains("tensorflow", ignoreCase = true) })
        assertTrue(names.none { it.contains("huggingface", ignoreCase = true) })
    }

    @Test
    fun noProcessExecutionApiExists() {
        val names = allDeclaredNames()

        assertTrue(names.none { it.contains("process", ignoreCase = true) })
        assertTrue(names.none { it.contains("subprocess", ignoreCase = true) })
        assertTrue(names.none { it.contains("stdout", ignoreCase = true) })
        assertTrue(names.none { it.contains("stderr", ignoreCase = true) })
    }

    @Test
    fun noModelArtifactResolutionApiExists() {
        val names = allDeclaredNames()

        assertTrue(names.none { it.contains("resolver", ignoreCase = true) })
        assertTrue(names.none { it.contains("modelpath", ignoreCase = true) })
        assertTrue(names.none { it.contains("tokenizerpath", ignoreCase = true) })
    }

    @Test
    fun noOptimizerExecutionApiExists() {
        val names = allDeclaredNames()

        assertTrue(names.none { it.contains("execute", ignoreCase = true) })
        assertTrue(names.none { it == "step" || it == "update" || it == "train" })
    }

    @Test
    fun noPersistenceNetworkOrTrainingExecutionApiExists() {
        val names = allDeclaredNames()

        assertTrue(names.none { it.contains("persist", ignoreCase = true) })
        assertTrue(names.none { it.contains("network", ignoreCase = true) })
        assertTrue(names.none { it == "train" || it == "executeTraining" })
    }

    @Test
    fun noFrozenUpstreamContractIsReimplemented() {
        assertEquals(
            listOf(HimTrainerProtocolV1.Request::class.java, HimAdamWRuntimeBindingV1::class.java),
            publicCreate().parameterTypes.toList(),
        )
    }

    @Test
    fun trainerProtocolPartitionRemainsTrainOnly() {
        assertTrue(binding().protocolRequest.records.all { it.partition == "TRAIN_ONLY" })
    }

    @Test
    fun trainerRecordsAreNotReprojected() {
        val fixture = Fixture.valid()

        assertSame(fixture.protocolRequest.records, binding(fixture).protocolRequest.records)
    }

    @Test
    fun trainerRecordOrderDoesNotChange() {
        val records = binding().protocolRequest.records

        assertEquals(records.indices.toList(), records.map { it.assignmentIndex })
    }

    @Test
    fun protocolBindingRequiresTrainerProtocolRequest() {
        assertEquals("YES", HimTrainerProtocolAdamWRuntimeBindingV1.ADAMW_PROTOCOL_BINDING_REQUIRES_TRAINER_PROTOCOL_REQUEST)
    }

    @Test
    fun frozenUpstreamContractsRemainUnmodifiedByTheBindingApi() {
        assertFalse(allDeclaredNames().any { it.contains("mutation", ignoreCase = true) })
    }

    private fun binding(fixture: Fixture = Fixture.valid()): HimTrainerProtocolAdamWRuntimeBindingV1 =
        HimTrainerProtocolAdamWRuntimeBindingV1.create(fixture.protocolRequest, fixture.runtimeBinding)

    private fun publicCreate() = HimTrainerProtocolAdamWRuntimeBindingV1::class.java.declaredMethods.single {
        it.name == "create" && Modifier.isPublic(it.modifiers)
    }

    private fun allDeclaredNames(): Set<String> = buildSet {
        addAll(HimTrainerProtocolAdamWRuntimeBindingV1::class.java.declaredMethods.map { it.name })
        addAll(HimTrainerProtocolAdamWRuntimeBindingV1::class.java.declaredFields.map { it.name })
    }

    private data class Fixture(
        val configuration: HimTrainingConfigurationV1,
        val mapping: HimOptimizerMappingV1,
        val mission: HimTrainingMissionV1.Mission,
        val protocolRequest: HimTrainerProtocolV1.Request,
        val runtimeBinding: HimAdamWRuntimeBindingV1,
        val alternateRuntimeBinding: HimAdamWRuntimeBindingV1,
    ) {
        companion object {
            fun valid() = create()

            fun changedInput() = create(observedTerm = "Changed variant")

            fun changedConfiguration() = create(configurationSeed = 8L)

            fun changedLearningRate() = create(learningRate = "0.02")

            fun nonAdamW() = create(optimizerId = "optimizer:sgd:v1")

            private fun create(
                observedTerm: String = "Fixture variant",
                configurationSeed: Long = 7L,
                learningRate: String = "0.01",
                optimizerId: String = "optimizer:adamw:v1",
            ): Fixture {
                val positive = positiveExample(observedTerm)
                val negatives = HimNegativeTrainingExamplePolicyV1.derive(positive).take(2)
                require(negatives.size == 2)
                val group = HimTrainingFamilyGroupReferenceV1.canonical(CANONICAL_ID)
                val records = listOf(HimTrainingPartitionRecordV1.Positive(positive)) +
                    negatives.map { HimTrainingPartitionRecordV1.Negative(it) }
                val manifest = HimTrainingPartitionManifestV1.create(
                    records.map { record ->
                        HimTrainingPartitionAssignmentV1(record, group, HimTrainingPartitionV1.TRAIN)
                    },
                )
                val configuration = HimTrainingConfigurationV1.create(
                    seed = configurationSeed,
                    epochs = 3,
                    microBatchSize = 2,
                    gradientAccumulationSteps = 1,
                    learningRate = BigDecimal(learningRate),
                    optimizerId = optimizerId,
                )
                val modelBinding = HimModelBindingV1.create(
                    modelFamilyId = "fixture:model-family",
                    baseModelId = "fixture:base-model",
                    baseModelArtifactDigest = digest("base-model"),
                    tokenizerId = "fixture:tokenizer",
                    tokenizerArtifactDigest = digest("tokenizer"),
                    modelConfigurationArtifactDigest = digest("model-config"),
                )
                val mission = HimTrainingMissionV1.create(
                    HimTrainingMissionV1.Request(
                        readiness = readiness(manifest),
                        trainingConfiguration = HimTrainingMissionV1.TrainingConfigurationReference(
                            configuration.logicalDigest,
                        ),
                        modelBinding = HimTrainingMissionV1.ModelBindingReference(modelBinding.logicalDigest),
                        implementationBinding = HimTrainingMissionV1.ImplementationBindingReference(
                            digest("implementation"),
                        ),
                    ),
                )
                val portRequest = HimTrainerPortV1.Request.create(
                    mission,
                    configuration,
                    modelBinding,
                    manifest,
                )
                val protocolRequest = HimTrainerProtocolV1.create().createRequest(portRequest)
                val mapping = HimOptimizerMappingV1.create()
                val runtimeBinding = HimAdamWRuntimeBindingV1.create(
                    mission,
                    configuration,
                    mapping,
                    parameters(),
                )
                val alternateRuntimeBinding = HimAdamWRuntimeBindingV1.create(
                    mission,
                    configuration,
                    mapping,
                    parameters(beta1 = "0.8"),
                )
                return Fixture(
                    configuration = configuration,
                    mapping = mapping,
                    mission = mission,
                    protocolRequest = protocolRequest,
                    runtimeBinding = runtimeBinding,
                    alternateRuntimeBinding = alternateRuntimeBinding,
                )
            }

            private fun positiveExample(observedTerm: String): HimTrainingExampleV1 {
                val input = HimTrainingInputV1(
                    observedTerm = observedTerm,
                    normalizedObservedTerm = observedTerm.lowercase(),
                    canonicalContext = listOf(
                        HimCandidateCanonicalContext(1, CANONICAL_ID, "Fixture variant", null),
                        HimCandidateCanonicalContext(2, HimEntityId("Def456"), "Other canonical", null),
                    ),
                    evidence = listOf(
                        de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1(
                            HimEvidenceReference("OPEN_FOOD_FACTS", FIXTURE_ARTIFACT_DIGEST, "off:product:fixture"),
                            "fixture-evidence",
                            1,
                        ),
                        de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1(
                            HimEvidenceReference("CIQUAL", FIXTURE_ARTIFACT_DIGEST, "ciqual:food:fixture"),
                            "fixture-evidence",
                            1,
                        ),
                    ),
                )
                return HimTrainingExampleV1.create(
                    taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
                    input = input,
                    target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(CANONICAL_ID)),
                    provenance = HimTrainingProvenanceV1(
                        sourceEvidenceReferences = input.evidence.map { it.reference },
                        sourceArtifactDigests = listOf(FIXTURE_ARTIFACT_DIGEST),
                    ),
                )
            }

            private fun readiness(manifest: HimTrainingPartitionManifestV1) =
                HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Result.Ready(
                    snapshotBinding = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1(
                        snapshotId = "training-corpus:v1:${digest("snapshot").value}",
                        corpusLogicalDigest = digest("snapshot"),
                    ),
                    partitionManifestLogicalDigest = manifest.logicalDigest,
                    partitionCounters = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Counters.from(
                        manifest.assignments,
                    ),
                    partitionPolicyVersion = HimTrainingPartitionContractV1.POLICY_VERSION,
                    leakageValidationResult = de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionValidationResultV1(
                        valid = true,
                        diagnostics = emptyList(),
                    ),
                )

            private fun parameters(
                beta1: String = "0.9",
                beta2: String = "0.999",
                epsilon: String = "0.00000001",
                weightDecay: String = "0.01",
            ) = HimAdamWParametersV1.create(
                beta1 = BigDecimal(beta1),
                beta2 = BigDecimal(beta2),
                epsilon = BigDecimal(epsilon),
                weightDecay = BigDecimal(weightDecay),
            )

            private val CANONICAL_ID = HimEntityId("Abc123")
            private val FIXTURE_ARTIFACT_DIGEST = digest("fixture-artifact")
        }
    }
}
