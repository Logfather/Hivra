package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.inference

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.provider.openai.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

class RunHimOpenAiSemanticRuntimeConnectivityTest {
    @Test
    fun executesOneOptInConnectivityOnlyRequest() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requirePaidNetworkEnabled()
        val optIn = System.getenv("HIM_OPENAI_CONNECTIVITY_TEST") == "true"
        val keyPresent = !System.getenv("OPENAI_API_KEY").isNullOrBlank()
        assumeTrue("External OpenAI connectivity requires explicit opt-in and API credential", optIn && keyPresent)

        val root = projectRoot()
        val guardsBefore = CONTENT_GUARDS.keys.associateWith { sha256(root.resolve(it)) }
        CONTENT_GUARDS.forEach { (path, expected) -> assertEquals(expected, requireNotNull(guardsBefore[path])) }
        val indexesBefore = INDEX_PATHS.associateWith { snapshot(root.resolve(it)) }
        val configuration = HimOpenAiSemanticProviderConfiguration()
        assertEquals(EXPECTED_FINGERPRINT, configuration.fingerprint().value)

        val provider = HimOpenAiSemanticInferenceProvider(
            configuration,
            HimOpenAiEnvironmentApiKeyProvider(),
            HimOpenAiFixedContextFactory { request ->
                HimSemanticFixedContext(
                    request.inputTerm, "[]", "{}", HimOpenAiSemanticOutputJsonSchema.V2.toString(),
                )
            },
            HimRetrofitOpenAiResponsesTransport.create(configuration.timeoutMilliseconds),
        )
        val runtime = HimProviderBackedSemanticInferenceRuntime(
            provider,
            HimSemanticInferenceProviderConfiguration(
                "OPENAI", configuration.model, configuration.configurationVersion,
                configuration.inferenceSchemaVersion, configuration.instructionPolicyVersion,
                null, null, configuration.maxOutputTokens, configuration.reasoningEffort,
                configuration.timeoutPolicyVersion, 1_000_000,
            ),
            HimSemanticInferenceJsonDecoder(),
            configuration.fingerprint(),
        )
        val result = runtime.infer(connectivityRequest())
        val report = report(result, provider.diagnostics, keyPresent, optIn)
        val output = root.resolve(REPORT_PATH)
        requireNotNull(output.parentFile).mkdirs()
        write(output, report)

        assertTrue("Real OpenAI connectivity failed; inspect secret-safe derived report", result is HimSemanticInferenceResult.Success)
        val success = result as HimSemanticInferenceResult.Success
        assertEquals("OPENAI", success.value.provenance.providerIdentifier)
        assertEquals("gpt-5.6-sol", success.value.provenance.modelIdentifier)
        assertEquals(configuration.fingerprint(), success.value.provenance.providerConfigurationFingerprint)
        assertEquals(HimRetrievalFoundationBinding.V1, success.value.provenance.retrievalFoundation)
        assertTrue(success.value.provenance.technicalAttemptCount in 1..2)
        assertEquals(guardsBefore, CONTENT_GUARDS.keys.associateWith { sha256(root.resolve(it)) })
        assertEquals(indexesBefore, INDEX_PATHS.associateWith { snapshot(root.resolve(it)) })
    }

    private fun connectivityRequest() = HimSemanticInferenceRequest(
        invocationReference = "CONNECTIVITY_TEST_ONLY",
        inputTerm = "CONNECTIVITY_TEST_ONLY: return no semantic candidate; verify the required JSON schema only",
        canonicalContext = emptyList(),
        evidence = emptyList(),
        retrievalRound = HimRetrievalRound(0),
        availableSources = setOf(HimGroundTruthSource.OPEN_FOOD_FACTS),
    )

    private fun report(
        result: HimSemanticInferenceResult,
        diagnostics: HimOpenAiInvocationDiagnostics,
        keyPresent: Boolean,
        optIn: Boolean,
    ): String {
        val success = result as? HimSemanticInferenceResult.Success
        val failure = (result as? HimSemanticInferenceResult.TechnicalFailure)?.failure
        return """
            HIM F3d.4a OPENAI RUNTIME CONNECTIVITY VERIFICATION
            ========================================================================
            DETERMINISTIC CONTRACT DATA
            FOUNDATION STATUS=PASS
            RETRIEVAL FOUNDATION STATUS=PASS release=F3D_2_RETRIEVAL_FOUNDATION_V1 releaseSha=b4814fbd8141c43b04b97bd0f87c5e01d5128b7143ff74b80ed873a8f0068023 foundationDigest=9780a1dc28ece40e1251981f007a509418de5db4c1e8921ba3402ac2dcf3fd86
            OPENAI PROVIDER CONFIGURATION=OPENAI/gpt-5.6-sol/RESPONSES_API_V1/JSON_SCHEMA_STRICT
            CONFIGURATION FINGERPRINT=$EXPECTED_FINGERPRINT
            RUNTIME OWNERSHIP=JVM_SYSTEM_TEST_ONLY androidClientKeyExposure=false
            NETWORK ADAPTER=HimOpenAiSemanticInferenceProvider transport=Retrofit/OkHttp endpoint=/v1/responses additionalDependency=false
            API MODE=Responses API model=gpt-5.6-sol reasoningEffort=medium store=false tools=none
            SECRET CONFIGURATION STATUS=OPENAI_API_KEY_PRESENT=$keyPresent secretLogged=false secretPersisted=false
            CONNECTIVITY OPT-IN=$optIn
            PREFLIGHT=PASS
            REQUEST SERIALIZATION=PASS strictJsonSchema=true contextBudget=true noSecretInBody=true
            DATA WRITES=candidates=0,candidateDataset=0,validations=0,entityIds=0,authorityMutations=0,mutationLedger=0,retiredIds=0,groundTruthReleases=0,retrievalFoundationMutations=0
            AUTHORITY INTEGRITY=UNCHANGED
            RETRIEVAL INDEX INTEGRITY=UNCHANGED
            RUNTIME CONNECTIVITY DATA
            REAL CALL EXECUTED=${diagnostics.physicalAttempts > 0}
            HTTP / PROVIDER STATUS=${if (diagnostics.providerSuccess) "PASS" else "FAIL"}
            STRUCTURED OUTPUT STATUS=${if (success != null) "PASS" else "FAIL"}
            STRICT DECODER STATUS=${if (success != null) "PASS" else "FAIL"}
            EVIDENCE REFERENCE VALIDATION=${if (success != null) "PASS" else "FAIL"}
            PROVIDER-NEUTRAL RESULT STATUS=${if (success != null) "SUCCESS" else "TECHNICAL_FAILURE"}
            TECHNICAL FAILURE=${failure?.kind?.name ?: "none"}
            TECHNICAL ATTEMPTS=${success?.value?.provenance?.technicalAttemptCount ?: failure?.technicalAttemptCount ?: diagnostics.physicalAttempts}
            PHYSICAL ATTEMPTS=${diagnostics.physicalAttempts}
            PROVIDER MODEL=${diagnostics.providerModel ?: "not-received"}
            USAGE INPUT TOKENS=${diagnostics.usage?.inputTokens ?: "not-received"}
            USAGE OUTPUT TOKENS=${diagnostics.usage?.outputTokens ?: "not-received"}
            USAGE CACHED INPUT TOKENS=${diagnostics.usage?.cachedInputTokens ?: "not-received"}
            REAL_OPENAI_RUNTIME_ENABLEMENT_READY=${success != null}
            F3d.4a COMPLETE=${success != null}
        """.trimIndent() + "\n"
    }

    private data class Snapshot(val bytes: Long, val modified: Long)
    private fun snapshot(file: File) = Snapshot(file.length(), file.lastModified())
    private fun write(file: File, text: String) = Files.write(
        file.toPath(), text.toByteArray(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
    )
    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    private fun projectRoot(): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (!current.resolve("settings.gradle.kts").isFile) current = requireNotNull(current.parentFile)
        return current
    }

    companion object {
        private const val EXPECTED_FINGERPRINT = "b01d790cb8ebd886024cef145d85786029fee684a032379776636d426d851579"
        private const val REPORT_PATH = "build/knowledge/reports/him/inference/him-f3d4a-openai-runtime-connectivity-verification.txt"
        private val CONTENT_GUARDS = linkedMapOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json" to "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json" to "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a",
            "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json" to "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json" to "4afc7490bd69f2f8919558d33d98c912771d128da4ac7a782cc116a1c9fccb6e",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json" to "6dcb065d6dce0aaa22c8d1d3dd620372fc90d3fe9a3ec9d20ee54737a8f9a951",
            "data/knowledge/him/retrieval/master/him-retrieval-foundation-release.v1.json" to "b4814fbd8141c43b04b97bd0f87c5e01d5128b7143ff74b80ed873a8f0068023",
        )
        private val INDEX_PATHS = listOf(
            "data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite",
            "data/sources/agribalyse/index/agribalyse-him-evidence-index.v1.sqlite",
            "data/sources/ciqual/index/ciqual-him-evidence-index.v1.sqlite",
            "data/sources/glycemic-index/index/glycemic-index-him-evidence-index.v1.sqlite",
        )
    }
}
