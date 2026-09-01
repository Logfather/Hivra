package de.shopme.testing.system.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExamplePolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
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
import de.shopme.tools.knowledge.him.training.encoding.HimTrainingTargetEncodingV1
import de.shopme.tools.knowledge.him.training.objective.HimTrainingObjectiveV1
import de.shopme.tools.knowledge.him.training.runtime.HimModelBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainerPortV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainerProtocolV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingConfigurationV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingMissionV1
import java.lang.reflect.Modifier
import java.math.BigDecimal
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private fun digest(seed: String): HimSha256 = HimSha256(
    MessageDigest.getInstance("SHA-256")
        .digest(seed.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) },
)

private val FIXTURE_ARTIFACT_DIGEST = HimSha256("a".repeat(64))

class RunHimTrainerProtocolV1Test {
    @Test
    fun contractIdentityAndStateAreFrozen() {
        val protocol = HimTrainerProtocolV1.create()

        assertEquals("HIM_TRAINER_PROTOCOL_V1", protocol.contractId)
        assertEquals("1", protocol.version)
        assertEquals("TRAINER_PROTOCOL_DEFINED", protocol.state)
        assertTrue(protocol.protocolReference.matches(Regex("trainer-protocol:v1:[0-9a-f]{64}")))
    }

    @Test
    fun protocolDigestAndReferenceAreBound() {
        val protocol = HimTrainerProtocolV1.create()

        assertEquals(
            "trainer-protocol:v1:${protocol.logicalDigest.value}",
            protocol.protocolReference,
        )
        assertEquals(HimTrainingObjectiveV1.create().logicalDigest, protocol.objectiveDigest)
        assertEquals(HimTrainingTargetEncodingV1.create().logicalDigest, protocol.targetEncodingDigest)
        assertEquals(
            "training-objective:v1:${protocol.objectiveDigest.value}",
            protocol.objectiveReference,
        )
        assertEquals(
            "training-target-encoding:v1:${protocol.targetEncodingDigest.value}",
            protocol.targetEncodingReference,
        )
    }

    @Test
    fun identicalProtocolInputsProduceIdenticalIdentity() {
        val first = HimTrainerProtocolV1.create()
        val second = HimTrainerProtocolV1.create()

        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.protocolReference, second.protocolReference)
        assertEquals(first.objectiveDigest, second.objectiveDigest)
        assertEquals(first.targetEncodingDigest, second.targetEncodingDigest)
    }

    @Test
    fun protocolConstructionRejectsMismatchedObjectiveAndEncoding() {
        val objective = HimTrainingObjectiveV1.create()
        val encoding = HimTrainingTargetEncodingV1.create()

        assertEquals(objective.logicalDigest, encoding.objectiveDigest)
        assertEquals(objective.objectiveReference, encoding.objectiveReference)
        assertEquals(objective.logicalDigest, HimTrainerProtocolV1.create(objective, encoding).objectiveDigest)
    }

    @Test
    fun onlyTrainerPortRequestCanCreateProtocolRequest() {
        val createRequest = HimTrainerProtocolV1::class.java.declaredMethods
            .single { it.name == "createRequest" }

        assertTrue(Modifier.isPublic(createRequest.modifiers))
        assertEquals(listOf(HimTrainerPortV1.Request::class.java), createRequest.parameterTypes.toList())
    }

    @Test
    fun protocolHasNoPublicMissionReadyCorpusOrAssignmentInputFactory() {
        val publicMethods = HimTrainerProtocolV1::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) }

        assertFalse(publicMethods.any { method ->
            method.parameterTypes.any { type ->
                type.simpleName.contains("Mission") ||
                    type.simpleName.contains("Readiness") ||
                    type.simpleName.contains("Corpus") ||
                    type.simpleName.contains("Assignment")
            }
        })
    }

    @Test
    fun protocolRequestRetainsTrainerPortBinding() {
        val portRequest = Fixture.allTrain().portRequest()
        val request = HimTrainerProtocolV1.create().createRequest(portRequest)

        assertEquals(portRequest.logicalDigest, request.trainerPortRequestDigest)
        assertEquals(portRequest.requestReference, request.trainerPortRequestReference)
        assertEquals(portRequest.mission.logicalDigest, request.trainingMissionDigest)
        assertEquals(portRequest.mission.missionReference, request.trainingMissionReference)
    }

    @Test
    fun configurationFieldsAreProjectedExactly() {
        val fixture = Fixture.allTrain()
        val configuration = HimTrainerProtocolV1.create().createRequest(fixture.portRequest()).configuration

        assertEquals(7, configuration.seed)
        assertEquals(3, configuration.epochs)
        assertEquals(2, configuration.microBatchSize)
        assertEquals(1, configuration.gradientAccumulationSteps)
        assertEquals("0.01", configuration.learningRate)
        assertEquals("fixture:optimizer:configuration", configuration.optimizerId)
        assertEquals(fixture.configuration.logicalDigest, configuration.logicalDigest)
        assertEquals(fixture.configuration.configurationReference, configuration.configurationReference)
    }

    @Test
    fun learningRateUsesCanonicalDecimalRepresentation() {
        val configuration = HimTrainerProtocolV1.create()
            .createRequest(Fixture.allTrain().portRequest())
            .configuration

        assertEquals("0.01", configuration.learningRate)
        assertFalse(configuration.learningRate.contains('E', ignoreCase = true))
        assertEquals(configuration.learningRate, BigDecimal(configuration.learningRate).stripTrailingZeros().toPlainString())
    }

    @Test
    fun modelBindingFieldsAndDigestsAreProjectedExactly() {
        val fixture = Fixture.allTrain()
        val modelBinding = HimTrainerProtocolV1.create().createRequest(fixture.portRequest()).modelBinding

        assertEquals(fixture.modelBinding.modelFamilyId, modelBinding.modelFamilyId)
        assertEquals(fixture.modelBinding.baseModelId, modelBinding.baseModelId)
        assertEquals(fixture.modelBinding.baseModelArtifactDigest, modelBinding.baseModelArtifactDigest)
        assertEquals(fixture.modelBinding.tokenizerId, modelBinding.tokenizerId)
        assertEquals(fixture.modelBinding.tokenizerArtifactDigest, modelBinding.tokenizerArtifactDigest)
        assertEquals(fixture.modelBinding.modelConfigurationArtifactDigest, modelBinding.modelConfigurationArtifactDigest)
        assertEquals(fixture.modelBinding.logicalDigest, modelBinding.logicalDigest)
        assertEquals(fixture.modelBinding.modelBindingReference, modelBinding.modelBindingReference)
    }

    @Test
    fun objectiveAndTargetEncodingBindingsAreProjectedExactly() {
        val request = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())

        assertEquals(HimTrainingObjectiveV1.create().logicalDigest, request.objectiveDigest)
        assertEquals("training-objective:v1:${request.objectiveDigest.value}", request.objectiveReference)
        assertEquals(HimTrainingTargetEncodingV1.create().logicalDigest, request.targetEncodingDigest)
        assertEquals(
            "training-target-encoding:v1:${request.targetEncodingDigest.value}",
            request.targetEncodingReference,
        )
    }

    @Test
    fun implementationFingerprintIsRetained() {
        val fixture = Fixture.allTrain()
        val request = HimTrainerProtocolV1.create().createRequest(fixture.portRequest())

        assertEquals(fixture.mission.implementationBinding.fingerprint, request.implementationFingerprint)
    }

    @Test
    fun allRecordsAreTrainOnly() {
        val request = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())

        assertEquals(3, request.records.size)
        assertTrue(request.records.all { it.partition == "TRAIN_ONLY" })
        assertEquals(0, request.records.count { it.partition == "VALIDATION" })
        assertEquals(0, request.records.count { it.partition == "HOLDOUT" })
    }

    @Test
    fun validationAndHoldoutAssignmentsDoNotEnterProtocolRequest() {
        val request = HimTrainerProtocolV1.create().createRequest(Fixture.mixedPartitions().portRequest())

        assertEquals(1, request.records.size)
        assertTrue(request.records.single().partition == "TRAIN_ONLY")
    }

    @Test
    fun emptyTrainInputCannotEnterProtocol() {
        val fixture = Fixture.allTrain()
        val emptyManifest = HimTrainingPartitionManifestV1.create(
            fixture.manifest.assignments.map { it.copy(partition = HimTrainingPartitionV1.HOLDOUT) },
        )

        assertFailsWith<IllegalArgumentException> {
            fixture.portRequest(emptyManifest)
        }
    }

    @Test
    fun trainOrderingIsPreserved() {
        val fixture = Fixture.allTrain()
        val request = HimTrainerProtocolV1.create().createRequest(fixture.portRequest())

        assertEquals(
            fixture.manifest.assignments.map { it.record.recordReference },
            request.records.map { it.recordReference },
        )
        assertEquals((0..2).toList(), request.records.map { it.assignmentIndex })
    }

    @Test
    fun positiveAndNegativePolarityArePreserved() {
        val request = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())

        assertEquals(1, request.records.count { it.polarity == HimTrainerProtocolV1.PolarityV1.POSITIVE })
        assertEquals(2, request.records.count { it.polarity == HimTrainerProtocolV1.PolarityV1.NEGATIVE })
    }

    @Test
    fun positiveTargetUsesExistingObjectiveAndEncoding() {
        val request = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())
        val positive = request.records.single { it.polarity == HimTrainerProtocolV1.PolarityV1.POSITIVE }
        val target = assertIs<HimTrainingTargetEncodingV1.Target.Positive>(positive.encodedTarget)

        assertEquals(HimTrainingClassificationV1.VARIANT, target.semanticTarget.kind)
        assertEquals(3, target.targetKindCode)
        assertEquals(request.objectiveDigest, target.objectiveDigest)
        assertTrue(target.targetReference.startsWith("training-encoded-positive-target:v1:"))
    }

    @Test
    fun negativeTargetsPreserveBoundaryAndPositiveTarget() {
        val fixture = Fixture.allTrain()
        val request = HimTrainerProtocolV1.create().createRequest(fixture.portRequest())
        val expectedNegatives = fixture.manifest.assignments
            .map { it.record }
            .filterIsInstance<HimTrainingPartitionRecordV1.Negative>()

        request.records.filter { it.polarity == HimTrainerProtocolV1.PolarityV1.NEGATIVE }
            .zip(expectedNegatives)
            .forEach { (record, expected) ->
                val target = assertIs<HimTrainingTargetEncodingV1.Target.Negative>(record.encodedTarget)
                assertEquals(expected.negativeExample.boundaryType, target.boundaryType)
                assertEquals(target.positiveTarget.objectiveDigest, target.objectiveDigest)
                assertTrue(target.targetReference.startsWith("training-encoded-negative-target:v1:"))
            }
    }

    @Test
    fun negativeBoundaryCodeUsesFrozenTargetEncoding() {
        val request = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())
        val negatives = request.records
            .mapNotNull { it.encodedTarget as? HimTrainingTargetEncodingV1.Target.Negative }

        assertEquals(2, negatives.size)
        assertTrue(negatives.all { it.boundaryTypeCode in 1..6 })
        assertEquals(
            negatives.map { it.boundaryTypeCode },
            negatives.map { HimTrainingTargetEncodingV1.NEGATIVE_BOUNDARY_CODES.getValue(it.boundaryType) },
        )
    }

    @Test
    fun manyToOneRecordsAreNotCollapsed() {
        val request = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())

        assertEquals(3, request.records.size)
        assertEquals(3, request.records.map { it.recordReference }.distinct().size)
        assertEquals(2, request.records.count { it.polarity == HimTrainerProtocolV1.PolarityV1.NEGATIVE })
        assertEquals(1, request.records.map { it.groupReference }.distinct().size)
    }

    @Test
    fun groupReferenceIsPreservedForEveryRecord() {
        val request = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())

        assertEquals(listOf("family:v1:canonical:Abc123"), request.records.map { it.groupReference }.distinct())
        assertTrue(request.records.all { it.groupReference.isNotBlank() })
    }

    @Test
    fun inputTermAndCanonicalContextAreProjected() {
        val request = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())
        val input = request.records.first().input

        assertEquals("Fixture variant", input.observedTerm)
        assertEquals("fixture variant", input.normalizedObservedTerm)
        assertEquals(listOf(1, 2), input.canonicalContext.map { it.rank })
        assertEquals(listOf("Abc123", "Def456"), input.canonicalContext.map { it.canonicalId })
        assertEquals(listOf("Fixture variant", "Other canonical"), input.canonicalContext.map { it.canonicalName })
    }

    @Test
    fun evidenceOwnershipAndOrderAreProjected() {
        val request = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())
        val evidence = request.records.first().input.evidence

        assertEquals(2, evidence.size)
        assertEquals(listOf("OPEN_FOOD_FACTS", "CIQUAL"), evidence.map { it.source })
        assertEquals(listOf("off:product:fixture", "ciqual:food:fixture"), evidence.map { it.sourceRecordIdentity })
        assertTrue(evidence.all { it.sourceArtifactSha256 == FIXTURE_ARTIFACT_DIGEST })
        assertTrue(evidence.all { it.retrievalRank == 1 })
    }

    @Test
    fun inputEvidenceDoesNotCarryHimAssignedRelations() {
        val evidenceFields = HimTrainerProtocolV1.Evidence::class.java.declaredFields.map { it.name }.toSet()

        assertTrue(
            evidenceFields.containsAll(
                setOf("source", "sourceArtifactSha256", "sourceRecordIdentity", "recordKind", "retrievalRank"),
            ),
        )
        assertFalse(evidenceFields.any { it.contains("relation", ignoreCase = true) })
    }

    @Test
    fun requestDigestChangesWhenInputChanges() {
        val first = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())
        val second = HimTrainerProtocolV1.create().createRequest(Fixture.changedInput().portRequest())

        assertNotEquals(first.logicalDigest, second.logicalDigest)
        assertNotEquals(first.requestReference, second.requestReference)
    }

    @Test
    fun requestDigestChangesWhenConfigurationChanges() {
        val first = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())
        val second = HimTrainerProtocolV1.create().createRequest(Fixture.changedConfiguration().portRequest())

        assertNotEquals(first.configuration.logicalDigest, second.configuration.logicalDigest)
        assertNotEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun requestDigestChangesWhenModelBindingChanges() {
        val first = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())
        val second = HimTrainerProtocolV1.create().createRequest(Fixture.changedModel().portRequest())

        assertNotEquals(first.modelBinding.logicalDigest, second.modelBinding.logicalDigest)
        assertNotEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun independentlyConstructedEquivalentFixturesProduceIdenticalRequest() {
        val first = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())
        val second = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())

        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.requestReference, second.requestReference)
        assertEquals(first.records, second.records)
    }

    @Test
    fun resultCompletedIsTypedAndBoundToRequest() {
        val protocol = HimTrainerProtocolV1.create()
        val request = protocol.createRequest(Fixture.allTrain().portRequest())
        val result = protocol.completed(request)

        assertIs<HimTrainerProtocolV1.Result.Completed>(result)
        assertEquals(request.logicalDigest, result.requestDigest)
        assertTrue(result.resultReference.matches(Regex("trainer-protocol-result:v1:[0-9a-f]{64}")))
        assertEquals("trainer-protocol-result:v1:${result.resultLogicalDigest.value}", result.resultReference)
    }

    @Test
    fun completedResultIsDeterministic() {
        val protocol = HimTrainerProtocolV1.create()
        val first = protocol.completed(protocol.createRequest(Fixture.allTrain().portRequest()))
        val second = protocol.completed(protocol.createRequest(Fixture.allTrain().portRequest()))

        assertEquals(first, second)
        assertEquals(first.resultLogicalDigest, second.resultLogicalDigest)
    }

    @Test
    fun completedAcceptsRequestFromEquivalentProtocol() {
        val request = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())
        val otherProtocol = HimTrainerProtocolV1.create()

        assertEquals(request.protocolReference, otherProtocol.protocolReference)
        assertEquals(request.logicalDigest, otherProtocol.completed(request).requestDigest)
    }

    @Test
    fun failedResultIsTypedAndSafe() {
        val protocol = HimTrainerProtocolV1.create()
        val request = protocol.createRequest(Fixture.allTrain().portRequest())
        val result = protocol.failed(request, "fixture-safe-context")

        assertIs<HimTrainerProtocolV1.Result.Failed>(result)
        assertEquals(HimTrainerProtocolV1.FailureReasonV1.PROTOCOL_RUNTIME_FAILED, result.reason)
        assertEquals(request.logicalDigest, result.requestDigest)
        assertEquals("fixture-safe-context", result.safeContext)
        assertFalse(result.safeContext.any { it.isISOControl() })
    }

    @Test
    fun failedRejectsBlankOrControlContext() {
        val protocol = HimTrainerProtocolV1.create()
        val request = protocol.createRequest(Fixture.allTrain().portRequest())

        assertFailsWith<IllegalArgumentException> { protocol.failed(request, "") }
        assertFailsWith<IllegalArgumentException> { protocol.failed(request, "bad\ncontext") }
    }

    @Test
    fun requestReferenceIsBoundToRequestDigest() {
        val request = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())

        assertEquals("trainer-request:v1:${request.logicalDigest.value}", request.requestReference)
    }

    @Test
    fun recordReferencesRemainDistinctAndBoundToExamples() {
        val fixture = Fixture.allTrain()
        val request = HimTrainerProtocolV1.create().createRequest(fixture.portRequest())

        assertEquals(1, request.records.count { it.recordReference == it.positiveExampleReference })
        assertEquals(1, request.records.map { it.positiveExampleReference }.distinct().size)
        assertEquals(2, request.records.drop(1).map { it.recordReference }.distinct().size)
        assertEquals(request.records.size, request.records.map { it.logicalDigest }.distinct().size)
    }

    @Test
    fun recordDigestIncludesEncodedTargetIdentity() {
        val request = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())

        request.records.forEach { record ->
            assertTrue(record.logicalDigest.value != record.encodedTarget.logicalDigest.value)
            assertTrue(record.logicalDigest.value.matches(Regex("[0-9a-f]{64}")))
        }
    }

    @Test
    fun noPersistenceOrExecutionApiIsExposed() {
        val methods = buildSet {
            addAll(HimTrainerProtocolV1::class.java.methods.map { it.name })
            addAll(HimTrainerProtocolV1.Request::class.java.methods.map { it.name })
            addAll(HimTrainerProtocolV1.Result.Completed::class.java.methods.map { it.name })
            addAll(HimTrainerProtocolV1.Result.Failed::class.java.methods.map { it.name })
        }

        assertFalse(methods.any { it.contains("persist", ignoreCase = true) })
        assertFalse(methods.any { it.contains("execute", ignoreCase = true) })
        assertFalse(methods.any { it.contains("fit", ignoreCase = true) })
        assertFalse(methods.any { it.contains("optimiz", ignoreCase = true) })
        assertFalse(methods.any { it.contains("token", ignoreCase = true) })
        assertFalse(methods.any { it.contains("tensor", ignoreCase = true) })
    }

    @Test
    fun noArtifactCheckpointEvaluationOrFrameworkBindingIsExposed() {
        val fields = HimTrainerProtocolV1.Result.Completed::class.java.declaredFields.map { it.name }
        val publicNames = HimTrainerProtocolV1::class.java.methods.map { it.name }.joinToString(" ")

        assertFalse(fields.any { it.contains("artifact", ignoreCase = true) })
        assertFalse(fields.any { it.contains("checkpoint", ignoreCase = true) })
        assertFalse(publicNames.contains("evaluate", ignoreCase = true))
        assertFalse(publicNames.contains("pytorch", ignoreCase = true))
        assertFalse(publicNames.contains("tensorflow", ignoreCase = true))
        assertFalse(publicNames.contains("device", ignoreCase = true))
    }

    @Test
    fun requiredBoundaryFlagsRemainDisabled() {
        assertEquals("YES", HimTrainerProtocolV1.TRAINER_PROTOCOL_REQUIRES_TRAINER_PORT_REQUEST)
        assertEquals("TRAIN_ONLY", HimTrainerProtocolV1.TRAINER_PROTOCOL_PARTITION)
        assertEquals("YES", HimTrainerProtocolV1.POLARITY_PRESERVED)
        assertEquals("YES", HimTrainerProtocolV1.NEGATIVE_BOUNDARY_PRESERVED)
        assertEquals(0, HimTrainerProtocolV1.MANY_TO_ONE_COLLAPSE)
        assertEquals(0, HimTrainerProtocolV1.PROCESS_EXECUTION)
        assertEquals(0, HimTrainerProtocolV1.MODEL_ARTIFACT_RESOLUTION)
        assertEquals(0, HimTrainerProtocolV1.OPTIMIZER_MAPPING)
        assertEquals(0, HimTrainerProtocolV1.TOKENIZATION_EXECUTION)
        assertEquals(0, HimTrainerProtocolV1.TENSORIZATION)
        assertEquals(0, HimTrainerProtocolV1.PERSISTENCE_WRITES)
        assertEquals(0, HimTrainerProtocolV1.NUMERICAL_TRAINING_EXECUTION)
    }

    @Test
    fun objectiveAndTargetEncodingAreNotReimplemented() {
        assertEquals(0, HimTrainerProtocolV1.OBJECTIVE_REIMPLEMENTATION)
        assertEquals(0, HimTrainerProtocolV1.TARGET_ENCODING_REIMPLEMENTATION)
        assertEquals("DOMAIN_ONLY", HimTrainerProtocolV1.PROTOCOL_SERIALIZATION)
    }

    @Test
    fun requestContainsNoEvaluationOrHoldoutData() {
        val fieldNames = HimTrainerProtocolV1.Request::class.java.declaredFields.map { it.name }.toSet()
        val request = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())

        assertFalse(fieldNames.any { it.contains("validation", ignoreCase = true) })
        assertFalse(fieldNames.any { it.contains("holdout", ignoreCase = true) })
        assertFalse(request.records.any { it.partition != "TRAIN_ONLY" })
    }

    @Test
    fun requestDoesNotCollapseSharedInputs() {
        val request = HimTrainerProtocolV1.create().createRequest(Fixture.allTrain().portRequest())

        assertEquals(3, request.records.map { it.input }.size)
        assertEquals(1, request.records.map { it.input }.distinct().size)
        assertEquals(3, request.records.map { it.recordReference }.distinct().size)
    }

    private data class Fixture(
        val configuration: HimTrainingConfigurationV1,
        val modelBinding: HimModelBindingV1,
        val mission: HimTrainingMissionV1.Mission,
        val manifest: HimTrainingPartitionManifestV1,
    ) {
        fun portRequest(
            partitionManifest: HimTrainingPartitionManifestV1 = manifest,
        ) = HimTrainerPortV1.Request.create(mission, configuration, modelBinding, partitionManifest)

        companion object {
            fun allTrain() = create(
                partitions = listOf(
                    HimTrainingPartitionV1.TRAIN,
                    HimTrainingPartitionV1.TRAIN,
                    HimTrainingPartitionV1.TRAIN,
                ),
            )

            fun mixedPartitions() = create(
                partitions = listOf(
                    HimTrainingPartitionV1.TRAIN,
                    HimTrainingPartitionV1.VALIDATION,
                    HimTrainingPartitionV1.HOLDOUT,
                ),
            )

            fun changedInput() = create(
                partitions = listOf(
                    HimTrainingPartitionV1.TRAIN,
                    HimTrainingPartitionV1.TRAIN,
                    HimTrainingPartitionV1.TRAIN,
                ),
                observedTerm = "Changed variant",
            )

            fun changedConfiguration() = create(
                partitions = listOf(
                    HimTrainingPartitionV1.TRAIN,
                    HimTrainingPartitionV1.TRAIN,
                    HimTrainingPartitionV1.TRAIN,
                ),
                configurationSeed = "changedconfiguration",
            )

            fun changedModel() = create(
                partitions = listOf(
                    HimTrainingPartitionV1.TRAIN,
                    HimTrainingPartitionV1.TRAIN,
                    HimTrainingPartitionV1.TRAIN,
                ),
                modelSeed = "changed-model",
            )

            private fun create(
                partitions: List<HimTrainingPartitionV1>,
                observedTerm: String = "Fixture variant",
                configurationSeed: String = "configuration",
                modelSeed: String = "model",
            ): Fixture {
                val positive = positiveExample(observedTerm)
                val negatives = HimNegativeTrainingExamplePolicyV1.derive(positive).take(2)
                require(negatives.size == 2)
                val group = HimTrainingFamilyGroupReferenceV1.canonical(CANONICAL_ID)
                val records = listOf(HimTrainingPartitionRecordV1.Positive(positive)) +
                    negatives.map { HimTrainingPartitionRecordV1.Negative(it) }
                val manifest = HimTrainingPartitionManifestV1.create(
                    records.zip(partitions).map { (record, partition) ->
                        HimTrainingPartitionAssignmentV1(record, group, partition)
                    },
                )
                val configuration = configuration(configurationSeed)
                val modelBinding = modelBinding(modelSeed)
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
                return Fixture(configuration, modelBinding, mission, manifest)
            }

            private fun positiveExample(observedTerm: String): HimTrainingExampleV1 {
                val input = HimTrainingInputV1(
                    observedTerm = observedTerm,
                    normalizedObservedTerm = observedTerm.lowercase(),
                    canonicalContext = listOf(
                        de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext(
                            1,
                            CANONICAL_ID,
                            "Fixture variant",
                            null,
                        ),
                        de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext(
                            2,
                            HimEntityId("Def456"),
                            "Other canonical",
                            null,
                        ),
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
                de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Result.Ready(
                    snapshotBinding =
                        de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1(
                            snapshotId = "training-corpus:v1:${digest("snapshot").value}",
                            corpusLogicalDigest = digest("snapshot"),
                        ),
                    partitionManifestLogicalDigest = manifest.logicalDigest,
                    partitionCounters =
                        de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Counters.from(
                            manifest.assignments,
                        ),
                    partitionPolicyVersion = HimTrainingPartitionContractV1.POLICY_VERSION,
                    leakageValidationResult =
                        de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionValidationResultV1(
                            valid = true,
                            diagnostics = emptyList(),
                        ),
                )

            private fun configuration(seed: String) = HimTrainingConfigurationV1.create(
                seed = 7,
                epochs = 3,
                microBatchSize = 2,
                gradientAccumulationSteps = 1,
                learningRate = BigDecimal("0.01"),
                optimizerId = "fixture:optimizer:$seed",
            )

            private fun modelBinding(seed: String) = HimModelBindingV1.create(
                modelFamilyId = "fixture:model-family:$seed",
                baseModelId = "fixture:base-model:$seed",
                baseModelArtifactDigest = digest("$seed-base"),
                tokenizerId = "fixture:tokenizer:$seed",
                tokenizerArtifactDigest = digest("$seed-tokenizer"),
                modelConfigurationArtifactDigest = digest("$seed-config"),
            )

            private val CANONICAL_ID = HimEntityId("Abc123")
        }
    }
}
