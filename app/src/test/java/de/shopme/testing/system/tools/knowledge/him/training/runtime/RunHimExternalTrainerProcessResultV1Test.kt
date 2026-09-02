package de.shopme.testing.system.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1
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
import de.shopme.tools.knowledge.him.training.runtime.HimExternalTrainerProcessBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimExternalTrainerProcessResultV1
import de.shopme.tools.knowledge.him.training.runtime.HimExternalTrainerRuntimeBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimExternalTrainerExecutionRequestV1
import de.shopme.tools.knowledge.him.training.runtime.HimAdamWParametersV1
import de.shopme.tools.knowledge.him.training.runtime.HimAdamWRuntimeBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimLocalModelArtifactResolverV1
import de.shopme.tools.knowledge.him.training.runtime.HimModelBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimOptimizerMappingV1
import de.shopme.tools.knowledge.him.training.runtime.HimPythonPyTorchRuntimeEnvironmentV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainerPortV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainerProtocolAdamWRuntimeBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainerProtocolV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingConfigurationV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingMissionV1
import java.math.BigDecimal
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private fun digest(seed: String): HimSha256 = HimSha256(
    MessageDigest.getInstance("SHA-256")
        .digest(seed.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) },
)

private data class ResultFixture(
    val processBinding: HimExternalTrainerProcessBindingV1,
    val requestSerialization: HimExternalTrainerProcessResultV1.RequestSerializationIdentity,
    val diagnosticsDigest: HimSha256,
    val finalModel: HimExternalTrainerProcessResultV1.OutputArtifact,
)

class RunHimExternalTrainerProcessResultV1Test {
    @Test
    fun validCompletedResultCanBeConstructed() {
        assertNotNull(completed())
    }

    @Test
    fun validFailedResultCanBeConstructed() {
        assertNotNull(failed())
    }

    @Test
    fun contractIdIsCorrect() = assertEquals(
        "HIM_EXTERNAL_TRAINER_PROCESS_RESULT_V1",
        HimExternalTrainerProcessResultV1.CONTRACT_ID,
    )

    @Test
    fun versionIsCorrect() = assertEquals("1", HimExternalTrainerProcessResultV1.VERSION)

    @Test
    fun stateIsCorrect() = assertEquals(
        "EXTERNAL_TRAINER_PROCESS_RESULT_REPORTED",
        HimExternalTrainerProcessResultV1.STATE,
    )

    @Test
    fun resultReferenceIsDeterministic() {
        val result = completed()
        assertEquals(
            "external-trainer-process-result:v1:${result.logicalDigest.value}",
            result.resultReference,
        )
    }

    @Test
    fun processBindingIsMandatory() = assertEquals(
        "YES",
        HimExternalTrainerProcessResultV1.PROCESS_RESULT_REQUIRES_PROCESS_BINDING,
    )

    @Test
    fun alternateAuthorizationPathsAreForbidden() = assertEquals(
        0,
        HimExternalTrainerProcessResultV1.ALTERNATE_PROCESS_RESULT_AUTHORIZATION_PATHS,
    )

    @Test
    fun requestSerializationIdentityIsIncluded() {
        val result = completed()
        assertEquals(fixture().requestSerialization, result.requestSerialization)
        assertEquals("YES", HimExternalTrainerProcessResultV1.PROCESS_REQUEST_IDENTITY_INCLUDED)
    }

    @Test
    fun runtimeBindingIdentityIsIncluded() {
        val result = completed()
        assertEquals(result.processBinding.runtimeBinding.logicalDigest, result.runtimeBindingDigest)
        assertEquals(result.processBinding.runtimeBinding.bindingReference, result.runtimeBindingReference)
    }

    @Test
    fun executionRequestIdentityIsIncluded() {
        val result = completed()
        assertEquals(result.processBinding.runtimeBinding.executionRequest.logicalDigest, result.executionRequestDigest)
        assertEquals(result.processBinding.runtimeBinding.executionRequest.requestReference, result.executionRequestReference)
    }

    @Test
    fun trainerIdentityIsIncluded() {
        val result = completed()
        assertEquals(result.processBinding.trainerModule, result.trainerModule)
        assertEquals(result.processBinding.trainerImplementationFingerprint, result.trainerImplementationFingerprint)
        assertEquals(result.processBinding.trainerImplementationReference, result.trainerImplementationReference)
    }

    @Test
    fun trainerIdentityFlagIsEnabled() = assertEquals(
        "YES",
        HimExternalTrainerProcessResultV1.TRAINER_IMPLEMENTATION_IDENTITY_INCLUDED,
    )

    @Test
    fun adapterFingerprintIsIncluded() {
        val result = completed()
        assertEquals(result.processBinding.processAdapterImplementationFingerprint, result.processAdapterImplementationFingerprint)
        assertEquals("YES", HimExternalTrainerProcessResultV1.PROCESS_ADAPTER_IMPLEMENTATION_IDENTITY_INCLUDED)
    }

    @Test
    fun deviceIdentityIsIncluded() {
        val result = completed()
        assertEquals(HimExternalTrainerProcessBindingV1.Device.CPU, result.device)
    }

    @Test
    fun runtimeEnvironmentIdentityIsIncluded() {
        val result = completed()
        val environment = result.processBinding.runtimeBinding.runtimeEnvironment
        assertEquals(environment.logicalDigest, result.runtimeEnvironmentDigest)
        assertEquals(environment.environmentReference, result.runtimeEnvironmentReference)
    }

    @Test
    fun processCompletedStatusIsRepresented() = assertEquals(
        HimExternalTrainerProcessResultV1.ProcessStatus.COMPLETED,
        completed().processStatus,
    )

    @Test
    fun processFailedStatusIsRepresented() = assertEquals(
        HimExternalTrainerProcessResultV1.ProcessStatus.FAILED,
        failed().processStatus,
    )

    @Test
    fun processExitSuccessDoesNotMeanTrainingAccepted() {
        val result = HimExternalTrainerProcessResultV1.create(
            fixture().processBinding,
            fixture().requestSerialization,
            HimExternalTrainerProcessResultV1.ProcessStatus.COMPLETED,
            0,
            HimExternalTrainerProcessResultV1.TrainingOutcome.TRAINING_FAILED,
            HimExternalTrainerProcessResultV1.FailureReason.TRAINING_FAILED,
            fixture().diagnosticsDigest,
            emptyList(),
        )
        assertEquals(0, result.processExitCode)
        assertEquals(HimExternalTrainerProcessResultV1.TrainingOutcome.TRAINING_FAILED, result.trainingOutcome)
        assertEquals(HimExternalTrainerProcessResultV1.FailureReason.TRAINING_FAILED, result.failureReason)
    }

    @Test
    fun trainingCompletedStatusIsRepresented() = assertEquals(
        HimExternalTrainerProcessResultV1.TrainingOutcome.TRAINING_COMPLETED,
        completed().trainingOutcome,
    )

    @Test
    fun trainingFailedStatusIsRepresented() = assertEquals(
        HimExternalTrainerProcessResultV1.TrainingOutcome.TRAINING_FAILED,
        failed(
            failureReason = HimExternalTrainerProcessResultV1.FailureReason.TRAINING_FAILED,
            trainingOutcome = HimExternalTrainerProcessResultV1.TrainingOutcome.TRAINING_FAILED,
        ).trainingOutcome,
    )

    @Test
    fun noTrainingResultIsRepresented() = assertEquals(
        HimExternalTrainerProcessResultV1.TrainingOutcome.NO_TRAINING_RESULT,
        completedNoTraining().trainingOutcome,
    )

    @Test
    fun failedResultRequiresFailureReason() {
        assertFailsWith<IllegalArgumentException> {
        HimExternalTrainerProcessResultV1.create(
            fixture().processBinding,
            fixture().requestSerialization,
            HimExternalTrainerProcessResultV1.ProcessStatus.FAILED,
            1,
            HimExternalTrainerProcessResultV1.TrainingOutcome.NO_TRAINING_RESULT,
            null,
            fixture().diagnosticsDigest,
            emptyList(),
        )
        }
    }

    @Test
    fun failureReasonIsExposed() = assertEquals(
        HimExternalTrainerProcessResultV1.FailureReason.PROCESS_FAILED,
        failed().failureReason,
    )

    @Test
    fun invalidFailureCombinationIsRejected() {
        assertFailsWith<IllegalArgumentException> {
        HimExternalTrainerProcessResultV1.create(
            fixture().processBinding,
            fixture().requestSerialization,
            HimExternalTrainerProcessResultV1.ProcessStatus.COMPLETED,
            0,
            HimExternalTrainerProcessResultV1.TrainingOutcome.TRAINING_COMPLETED,
            HimExternalTrainerProcessResultV1.FailureReason.TRAINING_FAILED,
            fixture().diagnosticsDigest,
            listOf(fixture().finalModel),
        )
        }
    }

    @Test
    fun diagnosticsDigestIsMandatory() {
        assertFailsWith<IllegalArgumentException> {
        HimExternalTrainerProcessResultV1.completed(
            fixture().processBinding,
            fixture().requestSerialization,
            HimSha256("0".repeat(64)),
            listOf(fixture().finalModel),
            HimExternalTrainerProcessResultV1.TrainingOutcome.TRAINING_COMPLETED,
        )
        }
    }

    @Test
    fun diagnosticsDigestIsBound() = assertEquals(
        "YES",
        HimExternalTrainerProcessResultV1.DIAGNOSTICS_DIGEST_BOUND,
    )

    @Test
    fun changingDiagnosticsDigestChangesResultDigest() {
        assertNotEquals(completed().logicalDigest, completed(diagnosticsDigest = digest("other-diagnostics")).logicalDigest)
    }

    @Test
    fun finalModelArtifactIdentityIsSupported() {
        assertEquals(
            HimExternalTrainerProcessResultV1.OutputArtifactRole.FINAL_MODEL,
            completed().outputArtifacts.single().role,
        )
        assertEquals(fixture().finalModel.sha256, completed().outputArtifacts.single().sha256)
    }

    @Test
    fun pathOnlyModelResultIsRejected() {
        assertFailsWith<IllegalArgumentException> {
        HimExternalTrainerProcessResultV1.OutputArtifact(
            HimExternalTrainerProcessResultV1.OutputArtifactRole.FINAL_MODEL,
            "",
            digest("model"),
            10,
            "/models/final.bin",
        )
        }
    }

    @Test
    fun checkpointArtifactIdentityIsSupported() {
        val checkpoint = artifact(HimExternalTrainerProcessResultV1.OutputArtifactRole.CHECKPOINT, "checkpoint")
        val result = completed(outputArtifacts = listOf(fixture().finalModel, checkpoint))
        assertEquals(2, result.outputArtifacts.size)
        assertEquals(HimExternalTrainerProcessResultV1.OutputArtifactRole.CHECKPOINT, result.outputArtifacts[1].role)
    }

    @Test
    fun trainingMetadataArtifactIdentityIsSupported() {
        val metadata = artifact(HimExternalTrainerProcessResultV1.OutputArtifactRole.TRAINING_METADATA, "metadata")
        val result = completed(outputArtifacts = listOf(fixture().finalModel, metadata))
        assertEquals(HimExternalTrainerProcessResultV1.OutputArtifactRole.TRAINING_METADATA, result.outputArtifacts[1].role)
    }

    @Test
    fun outputArtifactsAreOrderedByRole() {
        val checkpoint = artifact(HimExternalTrainerProcessResultV1.OutputArtifactRole.CHECKPOINT, "checkpoint")
        assertFailsWith<IllegalArgumentException> {
            completed(outputArtifacts = listOf(checkpoint, fixture().finalModel))
        }
    }

    @Test
    fun outputArtifactRolesAreUnique() {
        val duplicate = artifact(HimExternalTrainerProcessResultV1.OutputArtifactRole.FINAL_MODEL, "other-model")
        assertFailsWith<IllegalArgumentException> {
            completed(outputArtifacts = listOf(fixture().finalModel, duplicate))
        }
    }

    @Test
    fun changingOutputArtifactDigestChangesResultDigest() {
        val other = artifact(HimExternalTrainerProcessResultV1.OutputArtifactRole.FINAL_MODEL, "other-model")
        assertNotEquals(
            completed().logicalDigest,
            completed(outputArtifacts = listOf(other)).logicalDigest,
        )
    }

    @Test
    fun absoluteOutputPathIsExcludedFromLogicalIdentity() {
        val first = artifact(HimExternalTrainerProcessResultV1.OutputArtifactRole.FINAL_MODEL, "model", "/one/final.bin")
        val second = artifact(HimExternalTrainerProcessResultV1.OutputArtifactRole.FINAL_MODEL, "model", "/two/final.bin")
        assertEquals(
            completed(outputArtifacts = listOf(first)).logicalDigest,
            completed(outputArtifacts = listOf(second)).logicalDigest,
        )
        assertEquals("NO", HimExternalTrainerProcessResultV1.ABSOLUTE_OUTPUT_PATH_IN_RESULT_LOGICAL_DIGEST)
    }

    @Test
    fun rawStderrIsNotPartOfResultIdentity() = assertEquals(
        "NO",
        HimExternalTrainerProcessResultV1.RAW_STDERR_IN_RESULT_IDENTITY,
    )

    @Test
    fun noStackTraceSemanticIdentityExists() = assertFalse(
        HimExternalTrainerProcessResultV1.Result::class.java.declaredMethods.any {
            it.name.contains("stack", ignoreCase = true)
        },
    )

    @Test
    fun processExitCodeIsRecordedSeparately() {
        assertEquals(0, completed().processExitCode)
        assertEquals(1, failed().processExitCode)
    }

    @Test
    fun processExitCodeZeroIsNotTrainingAcceptance() = assertNotEquals(
        HimExternalTrainerProcessResultV1.TrainingOutcome.TRAINING_COMPLETED,
        HimExternalTrainerProcessResultV1.completed(
            fixture().processBinding,
            fixture().requestSerialization,
            fixture().diagnosticsDigest,
            trainingOutcome = HimExternalTrainerProcessResultV1.TrainingOutcome.NO_TRAINING_RESULT,
        ).trainingOutcome,
    )

    @Test
    fun changingProcessBindingChangesDigest() = assertNotEquals(
        completed().logicalDigest,
        completed(fixture = fixture(device = HimExternalTrainerProcessBindingV1.Device.MPS)).logicalDigest,
    )

    @Test
    fun changingRequestSerializationChangesDigest() = assertNotEquals(
        completed().logicalDigest,
        completed(requestSerialization = requestSerialization("other-request")).logicalDigest,
    )

    @Test
    fun changingRuntimeBindingChangesDigest() = assertNotEquals(
        completed().logicalDigest,
        completed(fixture = fixture(observedTerm = "other input")).logicalDigest,
    )

    @Test
    fun changingExecutionRequestChangesDigest() = assertNotEquals(
        completed().logicalDigest,
        completed(fixture = fixture(observedTerm = "execution input")).logicalDigest,
    )

    @Test
    fun changingTrainerFingerprintChangesDigest() = assertNotEquals(
        completed().logicalDigest,
        completed(fixture = fixture(trainerFingerprint = digest("other-trainer"))).logicalDigest,
    )

    @Test
    fun changingAdapterFingerprintChangesDigest() = assertNotEquals(
        completed().logicalDigest,
        completed(fixture = fixture(adapterFingerprint = digest("other-adapter"))).logicalDigest,
    )

    @Test
    fun changingDeviceChangesDigest() = assertNotEquals(
        completed().logicalDigest,
        completed(fixture = fixture(device = HimExternalTrainerProcessBindingV1.Device.MPS)).logicalDigest,
    )

    @Test
    fun changingProcessStatusChangesDigest() = assertNotEquals(
        completed().logicalDigest,
        failed().logicalDigest,
    )

    @Test
    fun changingTrainingOutcomeChangesDigest() = assertNotEquals(
        completed().logicalDigest,
        completedNoTraining().logicalDigest,
    )

    @Test
    fun changingFailureReasonChangesDigest() {
        val processFailed = failed(HimExternalTrainerProcessResultV1.FailureReason.PROCESS_FAILED)
        val invalid = failed(HimExternalTrainerProcessResultV1.FailureReason.INVALID_RESULT)
        assertNotEquals(processFailed.logicalDigest, invalid.logicalDigest)
    }

    @Test
    fun identicalInputsProduceIdenticalDigest() {
        val fixture = fixture()
        val first = completed(fixture = fixture)
        val second = completed(fixture = fixture)
        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.resultReference, second.resultReference)
    }

    @Test
    fun resultIsTimestampFree() = assertFalse(
        HimExternalTrainerProcessResultV1.Result::class.java.declaredMethods.any {
            it.name.contains("timestamp", ignoreCase = true)
        },
    )

    @Test
    fun resultIsUuidFree() = assertFalse(
        HimExternalTrainerProcessResultV1.Result::class.java.declaredMethods.any {
            it.name.contains("uuid", ignoreCase = true)
        },
    )

    @Test
    fun resultIsPidFree() = assertFalse(
        HimExternalTrainerProcessResultV1.Result::class.java.declaredMethods.any {
            it.name.contains("pid", ignoreCase = true)
        },
    )

    @Test
    fun resultIsHostnameFree() = assertFalse(
        HimExternalTrainerProcessResultV1.Result::class.java.declaredMethods.any {
            it.name.contains("host", ignoreCase = true)
        },
    )

    @Test
    fun evaluationAcceptanceIsExcluded() {
        assertEquals("NO", HimExternalTrainerProcessResultV1.HOLDOUT_ACCEPTANCE_IN_PROCESS_RESULT)
        assertFalse(
            HimExternalTrainerProcessResultV1.Result::class.java.declaredMethods.any {
                it.name.contains("evaluation", ignoreCase = true)
            },
        )
    }

    @Test
    fun promotionIsExcluded() = assertEquals(
        "NO",
        HimExternalTrainerProcessResultV1.TRAINING_PROCESS_RESULT_PERFORMS_MODEL_PROMOTION,
    )

    @Test
    fun finalModelAcceptanceIsExcluded() = assertEquals(
        "NO",
        HimExternalTrainerProcessResultV1.PROCESS_RESULT_IS_FINAL_MODEL_ACCEPTANCE,
    )

    @Test
    fun trainingConfigurationIsNotReimplemented() = assertEquals(
        0,
        HimExternalTrainerProcessResultV1.TRAINING_CONFIGURATION_REIMPLEMENTATION,
    )

    @Test
    fun modelBindingIsPreservedTransitively() {
        val result = completed()
        assertEquals(result.processBinding.runtimeBinding.executionRequest.modelBindingDigest, result.modelBindingDigest)
        assertEquals(result.processBinding.runtimeBinding.executionRequest.modelBindingReference, result.modelBindingReference)
    }

    @Test
    fun pythonVersionIsNotDuplicated() = assertFalse(
        Result::class.java.declaredMethods.any { it.name == "getPythonVersion" },
    )

    @Test
    fun pytorchVersionIsNotDuplicated() = assertFalse(
        Result::class.java.declaredMethods.any { it.name == "getPytorchVersion" },
    )

    @Test
    fun jvmSpecificSemanticsAreAbsent() = assertEquals(
        0,
        HimExternalTrainerProcessResultV1.JVM_SPECIFIC_RESULT_SEMANTICS,
    )

    @Test
    fun stdoutSerializerCanAccessAllResultFields() {
        val names = HimExternalTrainerProcessResultV1.Result::class.java.methods.map { it.name }.toSet()
        assertTrue(names.containsAll(setOf("getLogicalDigest", "getResultReference", "getProcessStatus")))
    }

    @Test
    fun filesystemIoIsForbidden() = assertEquals(0, HimExternalTrainerProcessResultV1.PROCESS_RESULT_FILESYSTEM_IO)

    @Test
    fun processExecutionIsForbidden() = assertEquals(0, HimExternalTrainerProcessResultV1.PROCESS_EXECUTION)

    @Test
    fun pythonExecutionIsForbidden() = assertEquals(0, HimExternalTrainerProcessResultV1.PYTHON_EXECUTION)

    @Test
    fun uvExecutionIsForbidden() = assertEquals(0, HimExternalTrainerProcessResultV1.UV_EXECUTION)

    @Test
    fun pytorchExecutionIsForbidden() = assertEquals(0, HimExternalTrainerProcessResultV1.PYTORCH_EXECUTION)

    @Test
    fun networkAccessIsForbidden() = assertEquals(0, HimExternalTrainerProcessResultV1.NETWORK_ACCESS)

    @Test
    fun numericalTrainingIsForbidden() = assertEquals(0, HimExternalTrainerProcessResultV1.NUMERICAL_TRAINING_EXECUTION)

    @Test
    fun persistenceIsForbidden() = assertEquals(0, HimExternalTrainerProcessResultV1.PROCESS_RESULT_PERSISTENCE)

    @Test
    fun transportHashingIsDeferred() = assertEquals(
        "DEFERRED_TO_RESULT_SERIALIZATION_V1",
        HimExternalTrainerProcessResultV1.CONTENT_TRANSPORT_HASHING,
    )

    @Test
    fun dedicatedTrainingResultRemainsSeparate() = assertEquals(
        "YES",
        HimExternalTrainerProcessResultV1.DEDICATED_TRAINING_RESULT_CONTRACT_REQUIRED,
    )

    @Test
    fun failureReasonsHaveNoDeadEntries() = assertEquals(
        HimExternalTrainerProcessResultV1.FailureReason.entries.size,
        HimExternalTrainerProcessResultV1.REASON_COUNT,
    )

    @Test
    fun deadReasonsAreZero() = assertEquals(0, HimExternalTrainerProcessResultV1.DEAD_REASONS)

    @Test
    fun eachFailureReasonIsReachable() {
        HimExternalTrainerProcessResultV1.FailureReason.entries.forEach { reason ->
            val result = failed(reason)
            assertEquals(reason, result.failureReason)
        }
    }

    @Test
    fun processBindingLineageIsPreserved() {
        val result = completed()
        assertEquals(result.processBinding.logicalDigest, result.processBinding.logicalDigest)
        assertEquals(result.processBinding.bindingReference, result.processBinding.bindingReference)
    }

    @Test
    fun resultStatusAndOutcomeRemainSeparate() {
        val result = completedNoTraining()
        assertEquals(HimExternalTrainerProcessResultV1.ProcessStatus.COMPLETED, result.processStatus)
        assertEquals(HimExternalTrainerProcessResultV1.TrainingOutcome.NO_TRAINING_RESULT, result.trainingOutcome)
    }

    @Test
    fun failedTrainingCannotCarryOutputArtifacts() {
        assertFailsWith<IllegalArgumentException> {
            failed(
                trainingOutcome = HimExternalTrainerProcessResultV1.TrainingOutcome.TRAINING_FAILED,
                outputArtifacts = listOf(fixture().finalModel),
            )
        }
    }

    @Test
    fun completedTrainingRequiresFinalModel() {
        assertFailsWith<IllegalArgumentException> {
        completed(
            outputArtifacts = emptyList(),
            trainingOutcome = HimExternalTrainerProcessResultV1.TrainingOutcome.TRAINING_COMPLETED,
        )
        }
    }

    @Test
    fun noTrainingResultCannotCarryOutputArtifacts() {
        assertFailsWith<IllegalArgumentException> {
        completed(
            outputArtifacts = listOf(fixture().finalModel),
            trainingOutcome = HimExternalTrainerProcessResultV1.TrainingOutcome.NO_TRAINING_RESULT,
        )
        }
    }

    @Test
    fun failedProcessCannotClaimTrainingCompleted() {
        assertFailsWith<IllegalArgumentException> {
        HimExternalTrainerProcessResultV1.create(
            fixture().processBinding,
            fixture().requestSerialization,
            HimExternalTrainerProcessResultV1.ProcessStatus.FAILED,
            1,
            HimExternalTrainerProcessResultV1.TrainingOutcome.TRAINING_COMPLETED,
            null,
            fixture().diagnosticsDigest,
            listOf(fixture().finalModel),
        )
        }
    }

    @Test
    fun nonzeroExitCodeCannotBeCompleted() {
        assertFailsWith<IllegalArgumentException> {
        HimExternalTrainerProcessResultV1.completed(
            fixture().processBinding,
            fixture().requestSerialization,
            fixture().diagnosticsDigest,
            trainingOutcome = HimExternalTrainerProcessResultV1.TrainingOutcome.NO_TRAINING_RESULT,
            processExitCode = 2,
        )
        }
    }

    @Test
    fun zeroExitCodeCannotBeFailed() {
        assertFailsWith<IllegalArgumentException> {
        HimExternalTrainerProcessResultV1.failed(
            fixture().processBinding,
            fixture().requestSerialization,
            fixture().diagnosticsDigest,
            HimExternalTrainerProcessResultV1.FailureReason.PROCESS_FAILED,
            processExitCode = 0,
        )
        }
    }

    @Test
    fun outputArtifactSizeCanBeAbsent() {
        val artifact = artifact(HimExternalTrainerProcessResultV1.OutputArtifactRole.FINAL_MODEL, "sized", null, null)
        assertEquals(null, artifact.byteSize)
    }

    @Test
    fun outputArtifactSizeCannotBeNegative() {
        assertFailsWith<IllegalArgumentException> {
        HimExternalTrainerProcessResultV1.OutputArtifact(
            HimExternalTrainerProcessResultV1.OutputArtifactRole.FINAL_MODEL,
            "external-trainer-output:v1:FINAL_MODEL:${digest("negative").value}",
            digest("negative"),
            -1,
            null,
        )
        }
    }

    @Test
    fun operationalPathIsOptional() = assertEquals(null, fixture().finalModel.operationalPath)

    @Test
    fun resultReferenceHasFixedDigestShape() = assertTrue(
        completed().resultReference.matches(Regex("external-trainer-process-result:v1:[0-9a-f]{64}")),
    )

    private fun completed(
        fixture: ResultFixture = fixture(),
        requestSerialization: HimExternalTrainerProcessResultV1.RequestSerializationIdentity = fixture.requestSerialization,
        diagnosticsDigest: HimSha256 = fixture.diagnosticsDigest,
        outputArtifacts: List<HimExternalTrainerProcessResultV1.OutputArtifact> = listOf(fixture.finalModel),
        trainingOutcome: HimExternalTrainerProcessResultV1.TrainingOutcome = HimExternalTrainerProcessResultV1.TrainingOutcome.TRAINING_COMPLETED,
    ): HimExternalTrainerProcessResultV1.Result.Completed = HimExternalTrainerProcessResultV1.completed(
        processBinding = fixture.processBinding,
        requestSerialization = requestSerialization,
        diagnosticsDigest = diagnosticsDigest,
        outputArtifacts = outputArtifacts,
        trainingOutcome = trainingOutcome,
    )

    private fun completedNoTraining() = completed(
        outputArtifacts = emptyList(),
        trainingOutcome = HimExternalTrainerProcessResultV1.TrainingOutcome.NO_TRAINING_RESULT,
    )

    private fun failed(
        failureReason: HimExternalTrainerProcessResultV1.FailureReason = HimExternalTrainerProcessResultV1.FailureReason.PROCESS_FAILED,
        fixture: ResultFixture = fixture(),
        requestSerialization: HimExternalTrainerProcessResultV1.RequestSerializationIdentity = fixture.requestSerialization,
        diagnosticsDigest: HimSha256 = fixture.diagnosticsDigest,
        trainingOutcome: HimExternalTrainerProcessResultV1.TrainingOutcome =
            if (failureReason == HimExternalTrainerProcessResultV1.FailureReason.TRAINING_FAILED) {
                HimExternalTrainerProcessResultV1.TrainingOutcome.TRAINING_FAILED
            } else {
                HimExternalTrainerProcessResultV1.TrainingOutcome.NO_TRAINING_RESULT
            },
        outputArtifacts: List<HimExternalTrainerProcessResultV1.OutputArtifact> = emptyList(),
        processExitCode: Int = 1,
    ): HimExternalTrainerProcessResultV1.Result.Failed = HimExternalTrainerProcessResultV1.failed(
        processBinding = fixture.processBinding,
        requestSerialization = requestSerialization,
        diagnosticsDigest = diagnosticsDigest,
        failureReason = failureReason,
        trainingOutcome = trainingOutcome,
        outputArtifacts = outputArtifacts,
        processExitCode = processExitCode,
    )

    private fun requestSerialization(seed: String) =
        HimExternalTrainerProcessResultV1.RequestSerializationIdentity(
            reference = "external-trainer-process-request-serialization:v1:${digest(seed).value}",
            logicalDigest = digest(seed),
        )

    private fun artifact(
        role: HimExternalTrainerProcessResultV1.OutputArtifactRole,
        seed: String,
        operationalPath: String? = null,
        byteSize: Long? = 10,
    ) = HimExternalTrainerProcessResultV1.OutputArtifact(
        role = role,
        reference = "external-trainer-output:v1:${role.name}:${digest(seed).value}",
        sha256 = digest(seed),
        byteSize = byteSize,
        operationalPath = operationalPath,
    )

    private fun fixture(
        observedTerm: String = "Result fixture",
        device: HimExternalTrainerProcessBindingV1.Device = HimExternalTrainerProcessBindingV1.Device.CPU,
        trainerFingerprint: HimSha256 = digest("trainer"),
        adapterFingerprint: HimSha256 = digest("adapter"),
    ): ResultFixture {
        val modelBinding = HimModelBindingV1.create(
            modelFamilyId = "fixture:model-family",
            baseModelId = "fixture:base-model",
            baseModelArtifactDigest = digest("base-model"),
            tokenizerId = "fixture:tokenizer",
            tokenizerArtifactDigest = digest("tokenizer"),
            modelConfigurationArtifactDigest = digest("configuration"),
        )
        val base = HimLocalModelArtifactResolverV1.VerifiedArtifact(
            HimLocalModelArtifactResolverV1.ArtifactRole.BASE_MODEL,
            modelBinding.baseModelArtifactDigest,
            modelBinding.baseModelArtifactDigest,
            Path.of("models/base.bin"),
            Path.of("base.bin"),
            1,
        )
        val tokenizer = HimLocalModelArtifactResolverV1.VerifiedArtifact(
            HimLocalModelArtifactResolverV1.ArtifactRole.TOKENIZER,
            modelBinding.tokenizerArtifactDigest,
            modelBinding.tokenizerArtifactDigest,
            Path.of("models/tokenizer.bin"),
            Path.of("tokenizer.bin"),
            1,
        )
        val configurationArtifact = HimLocalModelArtifactResolverV1.VerifiedArtifact(
            HimLocalModelArtifactResolverV1.ArtifactRole.MODEL_CONFIGURATION,
            modelBinding.modelConfigurationArtifactDigest,
            modelBinding.modelConfigurationArtifactDigest,
            Path.of("models/configuration.json"),
            Path.of("configuration.json"),
            1,
        )
        val resolutionDigest = resolutionDigest(modelBinding, base, tokenizer, configurationArtifact)
        val resolution = HimLocalModelArtifactResolverV1.Resolution(
            modelBinding,
            base,
            tokenizer,
            configurationArtifact,
            resolutionDigest,
            "local-model-artifact-resolution:v1:${resolutionDigest.value}",
        )
        val configuration = HimTrainingConfigurationV1.create(
            seed = 7L,
            epochs = 2,
            microBatchSize = 1,
            gradientAccumulationSteps = 1,
            learningRate = BigDecimal("0.01"),
            optimizerId = "optimizer:adamw:v1",
        )
        val positive = positiveExample(observedTerm)
        val negatives = HimNegativeTrainingExamplePolicyV1.derive(positive).take(2)
        val records = listOf(HimTrainingPartitionRecordV1.Positive(positive)) +
            negatives.map(HimTrainingPartitionRecordV1::Negative)
        val group = HimTrainingFamilyGroupReferenceV1.canonical(HimEntityId("Abc123"))
        val manifest = HimTrainingPartitionManifestV1.create(
            records.map { HimTrainingPartitionAssignmentV1(it, group, HimTrainingPartitionV1.TRAIN) },
        )
        val mission = HimTrainingMissionV1.create(
            HimTrainingMissionV1.Request(
                readiness = readiness(manifest),
                trainingConfiguration = HimTrainingMissionV1.TrainingConfigurationReference(configuration.logicalDigest),
                modelBinding = HimTrainingMissionV1.ModelBindingReference(modelBinding.logicalDigest),
                implementationBinding = HimTrainingMissionV1.ImplementationBindingReference(digest("implementation")),
            ),
        )
        val portRequest = HimTrainerPortV1.Request.create(mission, configuration, modelBinding, manifest)
        val protocolRequest = HimTrainerProtocolV1.create().createRequest(portRequest)
        val adamwBinding = HimAdamWRuntimeBindingV1.create(
            mission,
            configuration,
            HimOptimizerMappingV1.create(),
            HimAdamWParametersV1.create(
                beta1 = BigDecimal("0.9"),
                beta2 = BigDecimal("0.999"),
                epsilon = BigDecimal("0.00000001"),
                weightDecay = BigDecimal("0.01"),
            ),
        )
        val protocolAdamWBinding = HimTrainerProtocolAdamWRuntimeBindingV1.create(protocolRequest, adamwBinding)
        val executionRequest = HimExternalTrainerExecutionRequestV1.create(protocolAdamWBinding, resolution)
        val runtimeBinding = HimExternalTrainerRuntimeBindingV1.create(executionRequest, environment())
        val trainerReference = "trainer-implementation:v1:${trainerFingerprint.value}"
        val processBinding = HimExternalTrainerProcessBindingV1.create(
            runtimeBinding = runtimeBinding,
            trainerModule = "him_trainer",
            trainerImplementationFingerprint = trainerFingerprint,
            device = device,
            launcher = HimExternalTrainerProcessBindingV1.Launcher.UV,
            workingDirectory = "training/him",
            requestTransport = HimExternalTrainerProcessBindingV1.RequestTransport.IMMUTABLE_TEMP_FILE,
            processExecutionPolicyFingerprint = digest("process-policy"),
            processAdapterImplementationFingerprint = adapterFingerprint,
        )
        assertEquals(trainerReference, processBinding.trainerImplementationReference)
        return ResultFixture(
            processBinding = processBinding,
            requestSerialization = requestSerialization("request"),
            diagnosticsDigest = digest("diagnostics"),
            finalModel = artifact(HimExternalTrainerProcessResultV1.OutputArtifactRole.FINAL_MODEL, "model"),
        )
    }

    private fun positiveExample(observedTerm: String) = HimTrainingExampleV1.create(
        taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
        input = HimTrainingInputV1(
            observedTerm = observedTerm,
            normalizedObservedTerm = observedTerm.lowercase(),
            canonicalContext = listOf(
                HimCandidateCanonicalContext(1, HimEntityId("Abc123"), "Fixture", null),
            ),
            evidence = listOf(
                de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1(
                    HimEvidenceReference("OPEN_FOOD_FACTS", digest("evidence"), "off:fixture"),
                    "fixture evidence",
                    1,
                ),
            ),
        ),
        target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(HimEntityId("Abc123"))),
        provenance = HimTrainingProvenanceV1(
            sourceEvidenceReferences = listOf(HimEvidenceReference("OPEN_FOOD_FACTS", digest("evidence"), "off:fixture")),
            sourceArtifactDigests = listOf(digest("evidence")),
        ),
    )

    private fun readiness(manifest: HimTrainingPartitionManifestV1) = HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Result.Ready(
        snapshotBinding = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1(
            snapshotId = "training-corpus:v1:${digest("snapshot").value}",
            corpusLogicalDigest = digest("snapshot"),
        ),
        partitionManifestLogicalDigest = manifest.logicalDigest,
        partitionCounters = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Counters.from(manifest.assignments),
        partitionPolicyVersion = HimTrainingPartitionContractV1.POLICY_VERSION,
        leakageValidationResult = de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionValidationResultV1(true, emptyList()),
    )

    private fun environment() = HimPythonPyTorchRuntimeEnvironmentV1.create(
        runtimeChannel = HimPythonPyTorchRuntimeEnvironmentV1.RuntimeChannel.STABLE,
        pythonVersion = "3.13.14",
        pythonImplementation = "CPython",
        uvVersion = "0.12.2",
        targetOsFamily = "macOS",
        targetArchitecture = "arm64",
        pythonAbi = "cpython-313-darwin",
        pytorchVersion = "2.13.0",
        pytorchPackageSource = "https://download.pytorch.org/whl/cpu",
        pytorchWheelIdentity = "torch-2.13.0-fixture.whl",
        pytorchPackageSha256 = digest("package"),
        pytorchGitVersion = "0123456789abcdef0123456789abcdef01234567",
        pyprojectSha256 = digest("pyproject"),
        uvLockSha256 = digest("uv-lock"),
        pythonVersionFileSha256 = digest("python-version-file"),
        environmentImplementationFingerprint = digest("environment-implementation"),
    )

    private fun resolutionDigest(
        modelBinding: HimModelBindingV1,
        base: HimLocalModelArtifactResolverV1.VerifiedArtifact,
        tokenizer: HimLocalModelArtifactResolverV1.VerifiedArtifact,
        configuration: HimLocalModelArtifactResolverV1.VerifiedArtifact,
    ): HimSha256 {
        val canonical = buildString {
            field("contract", HimLocalModelArtifactResolverV1.CONTRACT_ID)
            field("version", HimLocalModelArtifactResolverV1.VERSION)
            field("state", HimLocalModelArtifactResolverV1.STATE)
            field("model-binding-digest", modelBinding.logicalDigest.value)
            field("model-binding-reference", modelBinding.modelBindingReference)
            artifactFields("base-model", base)
            artifactFields("tokenizer", tokenizer)
            artifactFields("model-configuration", configuration)
        }
        return digest(canonical)
    }

    private fun StringBuilder.artifactFields(prefix: String, artifact: HimLocalModelArtifactResolverV1.VerifiedArtifact) {
        field("$prefix-role", artifact.role.name)
        field("$prefix-expected-digest", artifact.expectedDigest.value)
        field("$prefix-actual-digest", artifact.actualDigest.value)
        field("$prefix-relative-path", artifact.relativePath.toString())
    }

    private fun StringBuilder.field(key: String, value: String) {
        append(key).append('=').append(value.length).append(':').append(value).append('\n')
    }
}
