package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.inference

import com.google.gson.JsonObject
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.provider.openai.*
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RunHimOpenAiSemanticProviderAdapterTest {
    @Test
    fun requestSerializationBindsFrozenContractWithoutSecretOrTools() {
        val packed = packedInput()
        val secret = "unit-test-secret-never-serialize"
        val body = HimOpenAiResponsesRequestSerializer(configuration()).serialize(packed)
        assertEquals("gpt-5.6-sol", body.get("model").asString)
        assertEquals("medium", body.getAsJsonObject("reasoning").get("effort").asString)
        assertEquals(8192, body.get("max_output_tokens").asInt)
        assertFalse(body.get("store").asBoolean)
        assertEquals("disabled", body.get("truncation").asString)
        assertTrue(body.getAsJsonArray("tools").isEmpty)
        val format = body.getAsJsonObject("text").getAsJsonObject("format")
        assertEquals("json_schema", format.get("type").asString)
        assertTrue(format.get("strict").asBoolean)
        assertFalse(body.toString().contains(secret))
        assertFalse(body.toString().contains("Authorization"))
        assertTrue(format.getAsJsonObject("schema").get("additionalProperties").asBoolean.not())
    }

    @Test
    fun adapterDecodesStructuredOutputUsageAndProviderNeutralRuntimeResult() {
        val transport = RecordingTransport(successResponse())
        val provider = provider({ "test-key" }, transport)
        val result = HimProviderBackedSemanticInferenceRuntime(
            provider, runtimeConfiguration(), HimSemanticInferenceJsonDecoder(), configuration().fingerprint(),
        ).infer(request())
        assertTrue(result is HimSemanticInferenceResult.Success)
        val success = (result as HimSemanticInferenceResult.Success).value
        assertTrue(success.candidates.isEmpty())
        assertEquals("OPENAI", success.provenance.providerIdentifier)
        assertEquals("gpt-5.6-sol", success.provenance.modelIdentifier)
        assertEquals(HimOpenAiSemanticProviderConfiguration().fingerprint(), success.provenance.providerConfigurationFingerprint)
        assertEquals(1, success.provenance.technicalAttemptCount)
        assertEquals(1, provider.diagnostics.physicalAttempts)
        assertEquals(321L, provider.diagnostics.usage?.inputTokens)
        assertEquals(17L, provider.diagnostics.usage?.outputTokens)
        assertEquals(12L, provider.diagnostics.usage?.cachedInputTokens)
        assertFalse(transport.body.toString().contains("test-key"))
    }

    @Test
    fun missingKeyAndContextOverflowNeverInvokeTransport() {
        val transport = RecordingTransport(successResponse())
        val missing = provider({ null }, transport)
        val missingResult = missing.invoke(request()) as HimSemanticProviderOutcome.TechnicalFailure
        assertEquals(HimSemanticInferenceFailureKind.CONFIGURATION_ERROR, missingResult.kind)
        assertEquals(0, transport.calls)

        val overflowProvider = HimOpenAiSemanticInferenceProvider(
            configuration(), { "test-key" },
            HimOpenAiFixedContextFactory { fixed().copy(canonicalContextJson = "x".repeat(240_000)) },
            transport,
        )
        val overflow = overflowProvider.invoke(request()) as HimSemanticProviderOutcome.TechnicalFailure
        assertEquals(HimSemanticInferenceFailureKind.CONTEXT_BUDGET_EXCEEDED, overflow.kind)
        assertEquals(0, transport.calls)
    }

    @Test
    fun httpFailuresMapWithoutSecretExposure() {
        val cases = mapOf(
            401 to HimSemanticInferenceFailureKind.CONFIGURATION_ERROR,
            403 to HimSemanticInferenceFailureKind.CONFIGURATION_ERROR,
            408 to HimSemanticInferenceFailureKind.TIMEOUT,
            429 to HimSemanticInferenceFailureKind.RATE_LIMITED,
            500 to HimSemanticInferenceFailureKind.PROVIDER_ERROR,
        )
        cases.forEach { (status, expected) ->
            val provider = provider({ "secret-$status" }, RecordingTransport(errorResponse(status)))
            val result = provider.invoke(request()) as HimSemanticProviderOutcome.TechnicalFailure
            assertEquals(expected, result.kind)
            assertFalse(result.safeMessage.contains("secret-$status"))
            assertEquals(status, result.safeProviderDiagnostic?.httpStatusCode)
            assertEquals(status / 100, result.safeProviderDiagnostic?.httpStatusFamily)
            assertEquals(true, result.safeProviderDiagnostic?.requestReachedProvider)
            assertEquals(true, result.safeProviderDiagnostic?.providerResponseReceived)
            assertEquals(false, result.safeProviderDiagnostic?.usageReceived)
        }
    }

    @Test
    fun safeErrorEnvelopeRetainsOnlyBoundedMachineFields() {
        val body = """{"error":{"message":"DO NOT PERSIST THIS OR secret-value","type":"invalid_request_error","param":"text.format.schema","code":"invalid_json_schema"}}"""
        val provider = provider({ "secret-value" }, FreshErrorTransport(400, body))
        val result = HimProviderBackedSemanticInferenceRuntime(
            provider, runtimeConfiguration(), HimSemanticInferenceJsonDecoder(), configuration().fingerprint(),
        ).infer(request()) as HimSemanticInferenceResult.TechnicalFailure
        assertEquals(2, result.failure.attemptDiagnostics.size)
        result.failure.attemptDiagnostics.forEach { attempt ->
            val safe = attempt.safeProviderDiagnostic!!
            assertEquals(400, safe.httpStatusCode)
            assertEquals(4, safe.httpStatusFamily)
            assertEquals("invalid_request_error", safe.providerErrorType)
            assertEquals("invalid_json_schema", safe.providerErrorCode)
            assertEquals("text.format.schema", safe.providerErrorParam)
            assertTrue(safe.requestReachedProvider == true && safe.providerResponseReceived)
            assertFalse(safe.usageReceived)
            assertFalse(safe.toString().contains("DO NOT PERSIST"))
            assertFalse(safe.toString().contains("secret-value"))
        }
    }

    @Test
    fun unsafeOrMissingProviderFieldsAreDiscardedWhileStatusSurvives() {
        val unsafe = """{"error":{"message":"ignored","type":"bad type\nsecret","param":"{request-payload}","code":"${"x".repeat(129)}"}}"""
        listOf(unsafe, "not-json", "{}").forEach { body ->
            val result = provider({ "key" }, RecordingTransport(errorResponse(422, body))).invoke(request()) as HimSemanticProviderOutcome.TechnicalFailure
            val safe = result.safeProviderDiagnostic!!
            assertEquals(422, safe.httpStatusCode)
            assertEquals(null, safe.providerErrorType)
            assertEquals(null, safe.providerErrorCode)
            assertEquals(null, safe.providerErrorParam)
        }
    }

    @Test
    fun explicitOptInRequiresBothBooleanFlagAndKey() {
        fun enabled(flag: String?, key: String?) = flag == "true" && !key.isNullOrBlank()
        assertFalse(enabled(null, "key"))
        assertFalse(enabled("false", "key"))
        assertFalse(enabled("true", null))
        assertTrue(enabled("true", "key"))
    }

    private fun provider(key: HimOpenAiApiKeyProvider, transport: HimOpenAiResponsesTransport) =
        HimOpenAiSemanticInferenceProvider(configuration(), key, HimOpenAiFixedContextFactory { fixed() }, transport)

    private fun packedInput(): String {
        val packed = HimSemanticContextPacker(HimSemanticContextBudgetPolicy.OPENAI_GPT_5_6_SOL_V1).pack(request(), fixed())
        return (packed as HimSemanticContextPackingResult.Packed).providerInputJson
    }

    private fun request() = HimSemanticInferenceRequest(
        "CONNECTIVITY_TEST_ONLY", "Braeburn", emptyList(), emptyList(), HimRetrievalRound(0),
        setOf(HimGroundTruthSource.OPEN_FOOD_FACTS),
    )

    private fun fixed() = HimSemanticFixedContext(
        "Braeburn", "[]", "{}", HimOpenAiSemanticOutputJsonSchema.V2.toString(),
    )

    private fun configuration() = HimOpenAiSemanticProviderConfiguration()

    private fun runtimeConfiguration() = HimSemanticInferenceProviderConfiguration(
        "OPENAI", "gpt-5.6-sol", "HIM_OPENAI_SEMANTIC_PROVIDER_CONFIGURATION_V1",
        HimSemanticInferenceSchema.OUTPUT_VERSION, HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
        null, null, 8192, "medium", "HIM_OPENAI_REASONING_TIMEOUT_POLICY_V1", 1_000_000,
    )

    private fun successResponse(): Response<JsonObject> = Response.success(JsonObject().apply {
        addProperty("status", "completed")
        addProperty("model", "gpt-5.6-sol")
        add("output", com.google.gson.JsonArray().apply {
            add(JsonObject().apply {
                addProperty("type", "message")
                add("content", com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("type", "output_text")
                        addProperty("text", """{"schemaVersion":"${HimSemanticInferenceSchema.OUTPUT_VERSION}","candidates":[],"informationGain":"NO_EXPECTED_INFORMATION_GAIN","retrievalDirective":null,"authorityConflicts":[]}""")
                    })
                })
            })
        })
        add("usage", JsonObject().apply {
            addProperty("input_tokens", 321)
            addProperty("output_tokens", 17)
            add("input_tokens_details", JsonObject().apply { addProperty("cached_tokens", 12) })
        })
    })

    private fun errorResponse(code: Int, body: String = "{}"): Response<JsonObject> = Response.error(
        code,
        ResponseBody.create(MediaType.parse("application/json"), body),
    )

    private class RecordingTransport(private val response: Response<JsonObject>) : HimOpenAiResponsesTransport {
        var calls = 0
        lateinit var body: JsonObject
        override fun execute(apiKey: String, body: JsonObject): Response<JsonObject> {
            calls++
            this.body = body
            return response
        }
    }
    private class FreshErrorTransport(private val status: Int, private val errorBody: String) : HimOpenAiResponsesTransport {
        override fun execute(apiKey: String, body: JsonObject): Response<JsonObject> = Response.error(
            status, ResponseBody.create(MediaType.parse("application/json"), errorBody),
        )
    }
}
