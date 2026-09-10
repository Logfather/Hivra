package de.shopme.testing.system.tools.knowledge.him.training.request

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimP1Point9ProductiveAuthorityV1
import de.shopme.tools.knowledge.him.training.authority.HimP1ProductiveTrainingAuthorityAssemblerV1
import de.shopme.tools.knowledge.him.training.authority.HimP1ProductiveTrainingAuthoritiesV1
import de.shopme.tools.knowledge.him.training.authority.HimP1TrainerProtocolAuthorityBindingV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupResolutionV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupResolverV1
import de.shopme.tools.knowledge.him.training.runtime.HimAdamWRuntimeBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimAdamWParametersV1
import de.shopme.tools.knowledge.him.training.runtime.HimExternalTrainerExecutionRequestV1
import de.shopme.tools.knowledge.him.training.runtime.HimExternalTrainerProcessBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimExternalTrainerProcessRequestSerializationV1
import de.shopme.tools.knowledge.him.training.runtime.HimExternalTrainerRuntimeBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimModelArtifactResolutionV1
import de.shopme.tools.knowledge.him.training.runtime.HimOptimizerMappingV1
import de.shopme.tools.knowledge.him.training.runtime.HimPythonPyTorchRuntimeEnvironmentV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainerProtocolAdamWRuntimeBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainerProtocolV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingMissionV1
import de.shopme.tools.knowledge.him.training.request.HimP1ProductiveTrainingRequestAssemblerV1
import de.shopme.tools.knowledge.him.training.request.HimP1ProductiveTrainingRequestV1
import de.shopme.tools.knowledge.him.training.request.HimTrainingRequestDurableManifestPersistenceV1
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private fun digest(value: String): HimSha256 = HimSha256(
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) },
)

class RunHimP1ProductiveTrainingRequestV1Test {
    @Test
    fun assemblesFromTheProductiveAuthoritiesAndServerPoint9() {
        val request = productiveRequest()
        assertEquals(HimP1ProductiveTrainingRequestV1.CONTRACT_ID, request.contractId)
        assertEquals(HimP1ProductiveTrainingRequestV1.VERSION, request.version)
        assertEquals(HimP1ProductiveTrainingRequestV1.STATE, request.state)
        assertEquals("Ready", request.productiveAuthorities.readinessBinding.readyResultReference)
        assertEquals(6, request.protocolRequest.records.size)
    }

    @Test
    fun bindsAllSemanticAuthorityInputs() {
        val request = productiveRequest()
        val authorities = request.productiveAuthorities
        val protocol = request.protocolRequest
        assertEquals(authorities.modelExecutionBinding, protocol.modelExecutionBinding)
        assertEquals(authorities.forwardRngAuthority, protocol.forwardRngAuthority)
        assertEquals(authorities.lossAuthority, protocol.lossAuthority)
        assertEquals(authorities.trainabilityPolicy, protocol.trainabilityPolicy)
        assertEquals(authorities.optimizerExecutionPolicy, protocol.optimizerExecutionPolicy)
        assertEquals(authorities.corpusBinding.corpusLogicalDigest, request.trainingMission.readinessBinding.corpusLogicalDigest)
        assertEquals(authorities.partitionBinding.logicalDigest, request.trainingMission.readinessBinding.partitionManifestLogicalDigest)
        assertEquals(protocol.objectiveDigest, protocol.objectiveDigest)
        assertEquals(protocol.targetEncodingDigest, protocol.targetEncodingDigest)
        assertEquals(protocol.configuration.logicalDigest, request.trainingMission.trainingConfiguration.digest)
        assertEquals(protocol.modelBinding.logicalDigest, request.trainingMission.modelBinding.digest)
        assertEquals(protocol.implementationFingerprint, request.trainingMission.implementationBinding.fingerprint)
    }

    @Test
    fun preservesExactPoint9OrderAndIdentities() {
        val request = productiveRequest()
        assertEquals(point9().records.map { it.exampleReference }, request.point9RecordReferences)
        assertEquals((0 until 6).toList(), request.protocolRequest.records.map { it.assignmentIndex })
        assertEquals(request.protocolRequest.records.map { it.recordReference }, request.point9RecordReferences)
        assertEquals(6, request.point9RecordReferences.distinct().size)
    }

    @Test
    fun preservesPoint13ClosedIdentities() {
        val authorities = productiveRequest().productiveAuthorities
        assertEquals("96a366848f33e127420fea381f7cd7d28cd4c220da2cb0fcf95d19ce947695c7", authorities.modelExecutionBinding.headContractLogicalDigest.value)
        assertEquals("3c3771b5a0864d578f7263d2a602eac3cfc5e0f45f3c4a68b5480603869a6809", authorities.modelExecutionBinding.fullInitialStateLogicalDigest.value)
        assertEquals("75fcb7c12864c36a002dbbf3080682e5f92ee4a278e7500c2e3466ff06833aca", authorities.modelExecutionBinding.logicalDigest.value)
        assertEquals("57185b372ad835f38e2a4a714481b51844ac6cbb9821d8ce56fa97fa925fdec3", authorities.forwardRngAuthority.logicalDigest.value)
        assertEquals("93cad16ebce71ea6ddc91b4799b2326f6069b95c494b3d0654f79c71b817e4ea", authorities.lossAuthority.logicalDigest.value)
        assertEquals("c94420c89a46fd703706dfeb7bd112b6bd0e44e4d8a29cdeba29f4028b56e8e1", authorities.trainabilityPolicy.logicalDigest.value)
        assertEquals("9f4858b1b2a839d39ac54ce985b597bdf39797d3e18c1f738680133e208bd6bf", authorities.optimizerExecutionPolicy.logicalDigest.value)
    }

    @Test
    fun preservesCorpusPartitionObjectiveTargetAndConfiguration() {
        val request = productiveRequest()
        assertEquals(HimP1ProductiveTrainingAuthoritiesV1.REAL_CORPUS_LOGICAL_DIGEST, request.productiveAuthorities.corpusBinding.corpusLogicalDigest.value)
        assertEquals(HimP1ProductiveTrainingAuthoritiesV1.PARTITION_LOGICAL_DIGEST, request.productiveAuthorities.partitionBinding.partitionIdentity.substringAfterLast(':'))
        assertEquals("Ready", request.productiveAuthorities.readinessBinding.readyResultReference)
        assertEquals("TARGET_KIND", request.protocolRequest.lossAuthority?.primaryObjective)
        assertEquals("CANDIDATE_COMPATIBILITY", request.protocolRequest.lossAuthority?.secondaryObjective)
        assertEquals("optimizer:adamw:v1", request.protocolRequest.configuration.optimizerId)
        assertEquals("0.0001", request.protocolRequest.configuration.learningRate)
    }

    @Test
    fun requestIdentityAndReferenceAreDeterministic() {
        val first = productiveRequest()
        val second = productiveRequest()
        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.requestReference, second.requestReference)
        assertEquals(first.canonicalSemanticProjection(), second.canonicalSemanticProjection())
        assertTrue(first.requestReference.endsWith(first.logicalDigest.value))
        assertNotEquals("8c29edf5dbd9ca4c6cba8754beb1434a9f889fca42ed24dfce6c31d6e07da09c", first.logicalDigest.value)
    }

    @Test
    fun canonicalProjectionAndStoredIdentitySelfValidate() {
        val request = productiveRequest()
        request.requireIdentity()
        assertThrows(IllegalArgumentException::class.java) {
            request.requireIdentity(expectedDigest = digest("wrong-request-identity"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            request.requireIdentity(expectedReference = "productive-training-request:v1:${digest("wrong-request-reference").value}")
        }
        assertThrows(IllegalArgumentException::class.java) {
            request.requireIdentity(semanticProjection = request.canonicalSemanticProjection() + "tampered")
        }
    }

    @Test
    fun meaningfulSemanticProjectionTamperingFailsClosed() {
        val request = productiveRequest()
        listOf(
            "model-revision",
            "full-initial-state",
            "model-execution",
            "forward-rng",
            "loss",
            "trainability",
            "optimizer",
            "corpus",
            "partition",
            "readiness",
            "record-0-reference",
            "record-1-reference",
            "record-2-reference",
            "record-3-reference",
            "record-4-reference",
            "record-5-reference",
        ).forEach { key ->
            val tampered = tamperField(request.canonicalSemanticProjection(), key)
            assertThrows("Expected $key tamper to fail", IllegalArgumentException::class.java) {
                request.requireIdentity(semanticProjection = tampered)
            }
        }
    }

    @Test
    fun transportStateIsNotInSemanticIdentity() {
        val request = productiveRequest()
        assertTrue(!request.canonicalSemanticProjection().contains("/workspace"))
        assertTrue(!request.canonicalSemanticProjection().contains("runpod"))
        assertTrue(!request.canonicalSemanticProjection().contains("hostname"))
        assertTrue(!request.canonicalSemanticProjection().contains("timestamp"))
        assertEquals(request.logicalDigest, digest(request.canonicalSemanticProjection()))
    }

    @Test
    fun readyIsRequiredAndReadinessCannotBeBypassed() {
        assertThrows(IllegalArgumentException::class.java) {
            HimTrainingMissionV1.ReadinessBinding(
                snapshotId = "training-corpus:v1:${HimP1ProductiveTrainingAuthoritiesV1.REAL_CORPUS_LOGICAL_DIGEST}",
                corpusLogicalDigest = HimSha256(HimP1ProductiveTrainingAuthoritiesV1.REAL_CORPUS_LOGICAL_DIGEST),
                partitionManifestLogicalDigest = HimSha256(HimP1ProductiveTrainingAuthoritiesV1.PARTITION_LOGICAL_DIGEST),
                partitionTotal = 6,
                partitionTrain = 4,
                partitionValidation = 0,
                partitionHoldout = 2,
                partitionPolicyVersion = HimTrainingMissionV1.ReadinessBinding.REQUIRED_PARTITION_POLICY_VERSION,
                leakageValidationValid = false,
                leakageValidationDigest = digest("invalid-leakage"),
            )
        }
        val request = productiveRequest()
        assertTrue(request.productiveAuthorities.readinessBinding.leakageValidationValid)
    }

    @Test
    fun productionPathUsesNoFixtureOrAppAndMapsLosslesslyToProtocol() {
        val request = productiveRequest()
        assertEquals(request.protocolRequest, request.protocolRequest)
        assertEquals(6, request.protocolRequest.records.size)
        assertEquals(request.point9RecordReferences, request.protocolRequest.records.map { it.recordReference })
        assertEquals(HimP1ProductiveTrainingRequestV1.DURABLE_PERSISTENCE, 0)
        assertEquals(HimP1ProductiveTrainingRequestV1.PROCESS_EXECUTION, 0)
        assertEquals(HimP1ProductiveTrainingRequestV1.MODEL_LOAD_EXECUTION, 0)
        assertEquals(HimP1ProductiveTrainingRequestV1.NUMERICAL_TRAINING_EXECUTION, 0)
    }

    @Test
    fun protocolSerializationIsDeterministicAndStrictPythonParseSucceeds() {
        val first = serialized()
        val second = serialized()
        assertTrue(first.utf8Bytes().contentEquals(second.utf8Bytes()))
        assertEquals(first.contentSha256, second.contentSha256)
        assertEquals(first.serializationReference, second.serializationReference)
        val training = JsonParser.parseString(first.canonicalPayload).asJsonObject.getAsJsonObject("trainingRequest")
        assertEquals(6, training.getAsJsonArray("records").size())
        listOf("modelExecutionBinding", "forwardRngAuthority", "lossAuthority", "trainabilityPolicy", "optimizerExecutionPolicy")
            .forEach { assertTrue(training.has(it)) }
        val script = """
            import sys
            from him_trainer.protocol_v1 import decode_external_trainer_request_v1
            value = decode_external_trainer_request_v1(sys.stdin.buffer.read())
            training = value.training_request
            print(training.model_execution_binding.logical_digest)
            print(training.forward_rng_authority.logical_digest)
            print(training.loss_authority.logical_digest)
            print(training.trainability_policy.logical_digest)
            print(training.optimizer_execution_policy.logical_digest)
            print(training.configuration.logical_digest)
            print(training.model_binding.logical_digest)
            print(training.objective_logical_digest)
            print(training.target_encoding_logical_digest)
            print("|".join(record.record_reference for record in training.records))
        """.trimIndent()
        val process = ProcessBuilder("python3", "-c", script)
            .directory(repositoryRoot())
            .redirectErrorStream(false)
            .apply { environment()["PYTHONPATH"] = repositoryRoot().resolve("training/him/src").path }
            .start()
        process.outputStream.use { it.write(first.utf8Bytes()) }
        val exitCode = process.waitFor()
        val stdout = process.inputStream.bufferedReader().readText().trim().lines()
        val stderr = process.errorStream.bufferedReader().readText()
        assertEquals("Python decoder stderr: $stderr", 0, exitCode)
        val request = productiveRequest().protocolRequest
        assertEquals(
            listOf(
                request.modelExecutionBinding!!.logicalDigest.value,
                request.forwardRngAuthority!!.logicalDigest.value,
                request.lossAuthority!!.logicalDigest.value,
                request.trainabilityPolicy!!.logicalDigest.value,
                request.optimizerExecutionPolicy!!.logicalDigest.value,
                request.configuration.logicalDigest.value,
                request.modelBinding.logicalDigest.value,
                request.objectiveDigest.value,
                request.targetEncodingDigest.value,
                request.records.joinToString("|") { it.recordReference },
            ),
            stdout,
        )
    }

    @Test
    fun protocolAuthorityAndRecordTamperingAreRejected() {
        val current = currentInputs()
        val request = productiveRequest()
        val mismatchedInput = request.protocolRequest
        val wrongProtocolInput = current.protocolInput.copy(implementationFingerprint = digest("other-implementation"))
        assertThrows(IllegalArgumentException::class.java) {
            HimP1ProductiveTrainingRequestAssemblerV1.create(
                HimP1ProductiveTrainingRequestAssemblerV1.Inputs(
                    currentAuthorities(),
                    point9(),
                    current.protocolBinding(wrongProtocolInput, currentAuthorities(), point9()),
                    request.trainingMission,
                ),
            )
        }
        assertNotEquals(mismatchedInput.implementationFingerprint, wrongProtocolInput.implementationFingerprint)
    }

    @Test
    fun modelRevisionAndEveryClosedAuthorityIdentityAreIdentityRelevant() {
        val request = productiveRequest()
        listOf(
            "model-revision", "full-initial-state", "model-execution", "forward-rng", "loss", "trainability", "optimizer",
            "corpus", "partition", "readiness", "configuration", "objective", "target-encoding", "implementation",
        ).forEach { key ->
            val changed = tamperField(request.canonicalSemanticProjection(), key)
            assertNotEquals(request.logicalDigest, digest(changed))
        }
    }

    @Test
    fun durableManifestPersistsCanonicalBytesAndReloads() {
        val root = Files.createTempDirectory("him-request-manifest-").toFile()
        try {
            val request = productiveRequest()
            val binding = processBinding()
            val serialized = HimExternalTrainerProcessRequestSerializationV1.serialize(binding)
            val result = HimTrainingRequestDurableManifestPersistenceV1.execute(
                HimTrainingRequestDurableManifestPersistenceV1.RequestV1(request, binding, root),
            )
            val persisted = assertIsCompleted(result)
            assertEquals(HimTrainingRequestDurableManifestPersistenceV1.PersistenceStatusV1.CREATED, persisted.status)
            assertTrue(persisted.path.isFile)
            assertTrue(persisted.protocolBytes.contentEquals(serialized.utf8Bytes()))
            assertTrue(persisted.bytes.size > persisted.protocolBytes.size)
            assertEquals(request.logicalDigest, persisted.productiveRequestLogicalDigest)
            assertEquals(request.requestReference, persisted.productiveRequestReference)
            assertEquals(28433, persisted.protocolBytes.size)
            assertEquals("8cbc4e473c096587e8579f130b37ff497a9e44ab79e18b6581ecd0cb05d80aaf", persisted.protocolSha256.value)
            assertEquals(digestBytes(persisted.bytes), persisted.manifestSha256)

            val reloaded = HimTrainingRequestDurableManifestPersistenceV1.load(
                persisted.path,
                request,
                binding,
            )
            assertEquals(request.logicalDigest, reloaded.request.logicalDigest)
            assertEquals(request.requestReference, reloaded.request.requestReference)
            assertEquals(persisted.protocolSha256, reloaded.protocolSha256)
            assertEquals(persisted.bytes.size, reloaded.bytes.size)
            assertTrue(reloaded.protocolBytes.contentEquals(serialized.utf8Bytes()))
            assertEquals(request.logicalDigest, reloaded.productiveRequestLogicalDigest)
            assertEquals(request.requestReference, reloaded.productiveRequestReference)
            assertTrue(persisted.bytes.contentEquals(reloaded.bytes))
            assertPythonManifestDecode(reloaded.bytes)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun durableManifestPublicationIsIdempotentAndPathIsIdentityBound() {
        val root = Files.createTempDirectory("him-request-manifest-idempotence-").toFile()
        val otherRoot = Files.createTempDirectory("him-request-manifest-path-").toFile()
        try {
            val request = productiveRequest()
            val binding = processBinding()
            val results = (0 until 3).map {
                HimTrainingRequestDurableManifestPersistenceV1.execute(
                    HimTrainingRequestDurableManifestPersistenceV1.RequestV1(request, binding, root),
                )
            }
            val persisted = results.map(::assertIsCompleted)
            assertEquals(
                listOf(
                    HimTrainingRequestDurableManifestPersistenceV1.PersistenceStatusV1.CREATED,
                    HimTrainingRequestDurableManifestPersistenceV1.PersistenceStatusV1.ALREADY_PRESENT_IDENTICAL,
                    HimTrainingRequestDurableManifestPersistenceV1.PersistenceStatusV1.ALREADY_PRESENT_IDENTICAL,
                ),
                persisted.map { it.status },
            )
            assertEquals(1, root.listFiles()!!.count { it.isFile })
            assertTrue(persisted[0].bytes.contentEquals(persisted[1].bytes))
            assertTrue(persisted[1].bytes.contentEquals(persisted[2].bytes))
            assertEquals(
                HimTrainingRequestDurableManifestPersistenceV1.pathFor(request, root).name,
                HimTrainingRequestDurableManifestPersistenceV1.pathFor(request, otherRoot).name,
            )
            assertTrue(
                HimTrainingRequestDurableManifestPersistenceV1.pathFor(request, root).parentFile !=
                    HimTrainingRequestDurableManifestPersistenceV1.pathFor(request, otherRoot).parentFile,
            )
            val wrongPath = otherRoot.resolve("wrong.request-manifest.v1.json")
            Files.write(wrongPath.toPath(), persisted[0].bytes)
            assertThrows(IllegalArgumentException::class.java) {
                HimTrainingRequestDurableManifestPersistenceV1.load(wrongPath, request, binding)
            }
        } finally {
            root.deleteRecursively()
            otherRoot.deleteRecursively()
        }
    }

    @Test
    fun durableManifestCollisionsAndReloadTamperingFailClosed() {
        val root = Files.createTempDirectory("him-request-manifest-collision-").toFile()
        try {
            val request = productiveRequest()
            val binding = processBinding()
            val first = assertIsCompleted(
                HimTrainingRequestDurableManifestPersistenceV1.execute(
                    HimTrainingRequestDurableManifestPersistenceV1.RequestV1(request, binding, root),
                ),
            )
            val original = first.bytes

            Files.write(first.path.toPath(), original.copyOf(original.size - 1))
            assertFailure(
                HimTrainingRequestDurableManifestPersistenceV1.execute(
                    HimTrainingRequestDurableManifestPersistenceV1.RequestV1(request, binding, root),
                ),
            )
            assertThrows(IllegalArgumentException::class.java) {
                HimTrainingRequestDurableManifestPersistenceV1.load(first.path, request, binding)
            }

            Files.write(first.path.toPath(), "{}\n".toByteArray())
            assertFailure(
                HimTrainingRequestDurableManifestPersistenceV1.execute(
                    HimTrainingRequestDurableManifestPersistenceV1.RequestV1(request, binding, root),
                ),
            )
            assertThrows(IllegalArgumentException::class.java) {
                HimTrainingRequestDurableManifestPersistenceV1.load(first.path, request, binding)
            }

            Files.delete(first.path.toPath())
            assertThrows(IllegalArgumentException::class.java) {
                HimTrainingRequestDurableManifestPersistenceV1.load(first.path, request, binding)
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun durableManifestCleansFailedTemporaryWrites() {
        val root = Files.createTempDirectory("him-request-manifest-invalid-").toFile()
        val rootFile = Files.createTempFile("him-request-manifest-root-", ".file").toFile()
        try {
            val request = productiveRequest()
            val binding = processBinding()
            val failed = HimTrainingRequestDurableManifestPersistenceV1.execute(
                HimTrainingRequestDurableManifestPersistenceV1.RequestV1(request, binding, rootFile),
            )
            assertFailure(failed)
            assertTrue(rootFile.parentFile.listFiles()!!.none { it.name.startsWith(".${rootFile.name}.") })
        } finally {
            root.deleteRecursively()
            rootFile.delete()
        }
    }

    @Test
    fun realDurableManifestMaterializesAndReloadsThroughStrictPythonContract() {
        val repositoryRoot = repositoryRoot()
        val request = productiveRequest()
        val binding = processBinding()
        val target = HimTrainingRequestDurableManifestPersistenceV1.pathFor(
            request,
            repositoryRoot.resolve(HimTrainingRequestDurableManifestPersistenceV1.DURABLE_ROOT),
        )
        removeLegacyManifestIfPresent(target)
        val result = assertIsCompleted(
            HimTrainingRequestDurableManifestPersistenceV1.execute(
                HimTrainingRequestDurableManifestPersistenceV1.RequestV1(
                    request,
                    binding,
                    repositoryRoot.resolve(HimTrainingRequestDurableManifestPersistenceV1.DURABLE_ROOT),
                ),
            ),
        )
        assertTrue(
            result.status == HimTrainingRequestDurableManifestPersistenceV1.PersistenceStatusV1.CREATED ||
                result.status == HimTrainingRequestDurableManifestPersistenceV1.PersistenceStatusV1.ALREADY_PRESENT_IDENTICAL,
        )
        assertEquals(request.logicalDigest.value, "517bab156ba3fe2346dd54ad2cde89ad1d7d106c43c7851a2ed899bf4a277653")
        assertEquals("517bab156ba3fe2346dd54ad2cde89ad1d7d106c43c7851a2ed899bf4a277653.request-manifest.v1.json", result.path.name)
        assertEquals(28433, result.protocolBytes.size)
        assertEquals("8cbc4e473c096587e8579f130b37ff497a9e44ab79e18b6581ecd0cb05d80aaf", result.protocolSha256.value)
        assertEquals(request.logicalDigest, result.productiveRequestLogicalDigest)
        assertEquals(request.requestReference, result.productiveRequestReference)
        val reloaded = HimTrainingRequestDurableManifestPersistenceV1.load(result.path, request, binding)
        assertEquals(request.logicalDigest, reloaded.request.logicalDigest)
        assertEquals(result.protocolSha256, reloaded.protocolSha256)
        assertEquals(request.logicalDigest, reloaded.productiveRequestLogicalDigest)
        assertEquals(request.requestReference, reloaded.productiveRequestReference)
        assertTrue(result.bytes.contentEquals(reloaded.bytes))
        assertPythonManifestDecode(reloaded.bytes)
    }

    @Test
    fun durableManifestIdentityAndPayloadTamperingFailsClosed() {
        val root = Files.createTempDirectory("him-request-manifest-identity-").toFile()
        try {
            val request = productiveRequest()
            val binding = processBinding()
            val persisted = assertIsCompleted(
                HimTrainingRequestDurableManifestPersistenceV1.execute(
                    HimTrainingRequestDurableManifestPersistenceV1.RequestV1(request, binding, root),
                ),
            )
            val original = persisted.bytes
            val tampered = listOf<(com.google.gson.JsonObject) -> Unit>(
                { it.remove("productiveRequestLogicalDigest") },
                { it.addProperty("productiveRequestLogicalDigest", "0".repeat(64)) },
                { it.addProperty("productiveRequestReference", "productive-training-request:v1:${"0".repeat(64)}") },
                {
                    it.addProperty("productiveRequestLogicalDigest", "0".repeat(64))
                    it.addProperty("productiveRequestReference", "productive-training-request:v1:${"0".repeat(64)}")
                },
                { it.addProperty("payload", it.get("payload").asString.dropLast(1)) },
            )
            tampered.forEach { change ->
                val rootJson = JsonParser.parseString(original.toString(Charsets.UTF_8)).asJsonObject
                change(rootJson)
                Files.write(persisted.path.toPath(), rootJson.toString().toByteArray(Charsets.UTF_8))
                assertThrows(IllegalArgumentException::class.java) {
                    HimTrainingRequestDurableManifestPersistenceV1.load(persisted.path, request, binding)
                }
            }
        } finally {
            root.deleteRecursively()
        }
    }

    private fun assertIsCompleted(
        result: HimTrainingRequestDurableManifestPersistenceV1.ResultV1,
    ): HimTrainingRequestDurableManifestPersistenceV1.PersistedV1 {
        assertTrue("Expected completed manifest result, got $result", result is HimTrainingRequestDurableManifestPersistenceV1.ResultV1.Completed)
        return (result as HimTrainingRequestDurableManifestPersistenceV1.ResultV1.Completed).value
    }

    private fun assertFailure(result: HimTrainingRequestDurableManifestPersistenceV1.ResultV1) {
        assertTrue("Expected failed manifest result, got $result", result is HimTrainingRequestDurableManifestPersistenceV1.ResultV1.Failed)
    }

    private fun assertPythonManifestDecode(bytes: ByteArray) {
        val script = """
            import sys
            from him_trainer.protocol_v1 import decode_durable_training_request_manifest_v1
            manifest = decode_durable_training_request_manifest_v1(sys.stdin.buffer.read())
            assert manifest.productive_request_logical_digest == "517bab156ba3fe2346dd54ad2cde89ad1d7d106c43c7851a2ed899bf4a277653"
            assert manifest.productive_request_reference == "productive-training-request:v1:517bab156ba3fe2346dd54ad2cde89ad1d7d106c43c7851a2ed899bf4a277653"
            request = manifest.payload
            assert len(request.training_request.records) == 6
            assert request.training_request.partition == "TRAIN_ONLY"
            assert request.training_request.model_execution_binding.logical_digest
            assert request.training_request.forward_rng_authority.logical_digest
            assert request.training_request.loss_authority.logical_digest
            assert request.training_request.trainability_policy.logical_digest
            assert request.training_request.optimizer_execution_policy.logical_digest
        """.trimIndent()
        val process = ProcessBuilder("python3", "-c", script)
            .directory(repositoryRoot())
            .redirectErrorStream(false)
            .apply { environment()["PYTHONPATH"] = repositoryRoot().resolve("training/him/src").path }
            .start()
        process.outputStream.use { it.write(bytes) }
        val exitCode = process.waitFor()
        val stderr = process.errorStream.bufferedReader().readText()
        assertEquals("Python decoder stderr: $stderr", 0, exitCode)
    }

    private fun digestBytes(value: ByteArray): HimSha256 = HimSha256(
        MessageDigest.getInstance("SHA-256")
            .digest(value)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) },
    )

    private fun productiveRequest(): HimP1ProductiveTrainingRequestV1 {
        val current = currentInputs()
        return HimP1ProductiveTrainingRequestAssemblerV1.create(
            HimP1ProductiveTrainingRequestAssemblerV1.Inputs(
                productiveAuthorities = currentAuthorities(),
                point9Authority = point9(),
                protocolBinding = current.protocolBinding(current.protocolInput, currentAuthorities(), point9()),
                trainingMission = current.mission,
            ),
        )
    }

    private fun serialized() = HimExternalTrainerProcessRequestSerializationV1.serialize(processBinding())

    private fun processBinding(): HimExternalTrainerProcessBindingV1 {
        val current = currentInputs()
        val request = productiveRequest().protocolRequest
        val runtime = HimAdamWRuntimeBindingV1.create(
            current.mission,
            current.configuration,
            HimOptimizerMappingV1.create(),
            current.adamWParameters,
        )
        val execution = HimExternalTrainerExecutionRequestV1.create(
            HimTrainerProtocolAdamWRuntimeBindingV1.create(request, runtime),
            artifacts(current),
        )
        val runtimeBinding = HimExternalTrainerRuntimeBindingV1.create(execution, environment())
        return HimExternalTrainerProcessBindingV1.create(
            runtimeBinding = runtimeBinding,
            trainerModule = "him_trainer",
            trainerImplementationFingerprint = digest("trainer-implementation"),
            device = HimP1ProductiveTrainingAuthorityAssemblerV1.currentP1ExecutionDevice(),
            launcher = HimExternalTrainerProcessBindingV1.Launcher.UV,
            workingDirectory = "training/him",
            requestTransport = HimExternalTrainerProcessBindingV1.RequestTransport.IMMUTABLE_TEMP_FILE,
            processExecutionPolicyFingerprint = digest("process-policy"),
            processAdapterImplementationFingerprint = digest("adapter-implementation"),
        )
    }

    private fun currentInputs(): Current {
        val source = HimP1ProductiveTrainingAuthorityAssemblerV1.currentP1Inputs()
        return Current(
            configuration = source.configuration,
            modelBinding = source.modelBinding,
            adamWParameters = source.adamWParameters,
            mission = mission(source.configuration, source.modelBinding),
            protocolInput = protocolInput(source.configuration, source.modelBinding),
        )
    }

    private data class Current(
        val configuration: de.shopme.tools.knowledge.him.training.runtime.HimTrainingConfigurationV1,
        val modelBinding: de.shopme.tools.knowledge.him.training.runtime.HimModelBindingV1,
        val adamWParameters: HimAdamWParametersV1,
        val mission: HimTrainingMissionV1.Mission,
        val protocolInput: HimTrainerProtocolV1.InputEnvelope,
    ) {
        fun protocolBinding(
            input: HimTrainerProtocolV1.InputEnvelope,
            authorities: HimP1ProductiveTrainingAuthoritiesV1.Bundle,
            point9: HimP1Point9ProductiveAuthorityV1.AuthorityV1,
        ) =
            HimP1TrainerProtocolAuthorityBindingV1.Inputs(
                protocol = HimTrainerProtocolV1.create(),
                protocolInput = input,
                adamwRuntimeBinding = HimAdamWRuntimeBindingV1.create(
                    mission,
                    configuration,
                    HimOptimizerMappingV1.create(),
                    adamWParameters,
                ),
                point9Authority = point9,
                productiveAuthorities = authorities,
            )
    }

    private fun currentAuthorities() = HimP1ProductiveTrainingAuthorityAssemblerV1.currentP1()

    private fun protocolInput(
        configuration: de.shopme.tools.knowledge.him.training.runtime.HimTrainingConfigurationV1,
        modelBinding: de.shopme.tools.knowledge.him.training.runtime.HimModelBindingV1,
    ): HimTrainerProtocolV1.InputEnvelope {
        val trainerPortDigest = digest("trainer-port-request")
        return HimTrainerProtocolV1.InputEnvelope(
            trainerPortRequestDigest = trainerPortDigest,
            trainerPortRequestReference = "trainer-request:v1:${trainerPortDigest.value}",
            trainingMissionDigest = mission(configuration, modelBinding).logicalDigest,
            trainingMissionReference = mission(configuration, modelBinding).missionReference,
            configuration = HimTrainerProtocolV1.Configuration(
                configuration.seed,
                configuration.epochs,
                configuration.microBatchSize,
                configuration.gradientAccumulationSteps,
                configuration.learningRate.toPlainString(),
                configuration.optimizerId,
                configuration.logicalDigest,
                configuration.configurationReference,
            ),
            modelBinding = HimTrainerProtocolV1.ModelBinding(
                modelBinding.modelFamilyId,
                modelBinding.baseModelId,
                modelBinding.baseModelArtifactDigest,
                modelBinding.tokenizerId,
                modelBinding.tokenizerArtifactDigest,
                modelBinding.modelConfigurationArtifactDigest,
                modelBinding.logicalDigest,
                modelBinding.modelBindingReference,
            ),
            implementationFingerprint = digest("implementation"),
            records = point9().records.map { instance ->
                val example = if (instance.exampleReference.startsWith("negative-example:")) {
                    point9().point9.negativeExamples.single { it.reference.value == instance.exampleReference }.positiveExample
                } else {
                    point9().point9.positiveExamples.single { it.exampleReference.value == instance.exampleReference }
                }
                val group = HimTrainingFamilyGroupResolverV1.resolve(example)
                require(group is HimTrainingFamilyGroupResolutionV1.Resolved)
                HimTrainerProtocolV1.InputRecord(instance.order, group.groupReference.value, "TRAIN_ONLY", instance.payload)
            },
            productiveAuthorities = currentAuthorities(),
        )
    }

    private fun mission(
        configuration: de.shopme.tools.knowledge.him.training.runtime.HimTrainingConfigurationV1,
        modelBinding: de.shopme.tools.knowledge.him.training.runtime.HimModelBindingV1,
    ): HimTrainingMissionV1.Mission {
        val authorities = currentAuthorities()
        return HimTrainingMissionV1.create(
            HimTrainingMissionV1.Request(
                readiness = HimTrainingMissionV1.ReadinessBinding(
                    snapshotId = "training-corpus:v1:${authorities.corpusBinding.corpusLogicalDigest.value}",
                    corpusLogicalDigest = authorities.corpusBinding.corpusLogicalDigest,
                    partitionManifestLogicalDigest = authorities.partitionBinding.logicalDigest,
                    partitionTotal = authorities.partitionBinding.totalCount,
                    partitionTrain = authorities.partitionBinding.trainCount,
                    partitionValidation = authorities.partitionBinding.validationCount,
                    partitionHoldout = authorities.partitionBinding.holdoutCount,
                    partitionPolicyVersion = authorities.readinessBinding.partitionPolicyVersion,
                    leakageValidationValid = authorities.readinessBinding.leakageValidationValid,
                    leakageValidationDigest = authorities.readinessBinding.leakageValidationDigest,
                ),
                trainingConfiguration = HimTrainingMissionV1.TrainingConfigurationReference(configuration.logicalDigest),
                modelBinding = HimTrainingMissionV1.ModelBindingReference(modelBinding.logicalDigest),
                implementationBinding = HimTrainingMissionV1.ImplementationBindingReference(digest("implementation")),
            ),
        )
    }

    private fun point9(): HimP1Point9ProductiveAuthorityV1.AuthorityV1 =
        HimP1Point9ProductiveAuthorityV1.requireReal(repositoryRoot())

    private fun artifacts(current: Current) = HimModelArtifactResolutionV1(
        modelBinding = current.modelBinding,
        baseModel = HimModelArtifactResolutionV1.VerifiedArtifact(
            HimModelArtifactResolutionV1.ArtifactRole.BASE_MODEL,
            current.modelBinding.baseModelArtifactDigest,
            current.modelBinding.baseModelArtifactDigest,
            "/srv/him/model/base.safetensors",
            "base.safetensors",
            1_115_567_652L,
        ),
        tokenizer = HimModelArtifactResolutionV1.VerifiedArtifact(
            HimModelArtifactResolutionV1.ArtifactRole.TOKENIZER,
            current.modelBinding.tokenizerArtifactDigest,
            current.modelBinding.tokenizerArtifactDigest,
            "/srv/him/model/tokenizer.json",
            "tokenizer.json",
            9_096_718L,
        ),
        modelConfiguration = HimModelArtifactResolutionV1.VerifiedArtifact(
            HimModelArtifactResolutionV1.ArtifactRole.MODEL_CONFIGURATION,
            current.modelBinding.modelConfigurationArtifactDigest,
            current.modelBinding.modelConfigurationArtifactDigest,
            "/srv/him/model/config.json",
            "config.json",
            615L,
        ),
        logicalDigest = digest("artifact-resolution"),
        resolutionReference = "local-model-artifact-resolution:v1:${digest("artifact-resolution").value}",
    )

    private fun environment() = HimPythonPyTorchRuntimeEnvironmentV1.create(
        runtimeChannel = HimPythonPyTorchRuntimeEnvironmentV1.RuntimeChannel.STABLE,
        pythonVersion = "3.13.14",
        pythonImplementation = "CPython",
        uvVersion = "0.12.2",
        targetOsFamily = "Linux",
        targetArchitecture = "x86_64",
        pythonAbi = "cpython-313-linux",
        pytorchVersion = "2.14.0",
        pytorchPackageSource = "https://download.pytorch.org/whl/cu130",
        pytorchWheelIdentity = "torch-2.14.0-cp313-cp313-linux_x86_64.whl",
        pytorchPackageSha256 = digest("pytorch-package"),
        pytorchGitVersion = "cf30153c4c131c8164ee7798e5022d810682e2cb",
        pyprojectSha256 = digest("pyproject"),
        uvLockSha256 = digest("uv-lock"),
        pythonVersionFileSha256 = digest("python-version-file"),
        environmentImplementationFingerprint = digest("environment-implementation"),
    )

    private fun tamperField(projection: String, key: String): String {
        val start = projection.indexOf("$key=")
        require(start >= 0) { "Missing projection field $key" }
        val lineStart = projection.lastIndexOf('\n', start).let { if (it < 0) 0 else it + 1 }
        val lineEnd = projection.indexOf('\n', start).let { if (it < 0) projection.length else it }
        val newValue = "tampered-$key"
        return projection.substring(0, lineStart) +
            "$key=${newValue.length}:$newValue" +
            projection.substring(lineEnd)
    }

    private fun repositoryRoot(): File {
        var current = File(System.getProperty("user.dir")).canonicalFile
        while (true) {
            if (File(current, "settings.gradle.kts").isFile && File(current, "gradlew").isFile) return current
            current = current.parentFile ?: error("Repository root not found")
        }
    }

    private fun removeLegacyManifestIfPresent(path: File) {
        if (!path.isFile) return
        val isLegacy = runCatching {
            val root = JsonParser.parseString(path.readText()).asJsonObject
            val payload = JsonParser.parseString(root.get("payload").asString).asJsonObject
            root.get("contractId")?.asString == HimTrainingRequestDurableManifestPersistenceV1.CONTRACT_ID &&
                payload.get("device")?.asString != "CUDA"
        }.getOrDefault(false)
        if (isLegacy) Files.delete(path.toPath())
    }
}
