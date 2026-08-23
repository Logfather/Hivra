package de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.provider.openai

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.*
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

private const val OPENAI_API_KEY_ENVIRONMENT_NAME = "OPENAI_API_KEY"

fun interface HimOpenAiApiKeyProvider { fun apiKey(): String? }
fun interface HimOpenAiFixedContextFactory { fun create(request: HimSemanticInferenceRequest): HimSemanticFixedContext }

class HimOpenAiEnvironmentApiKeyProvider(
    private val environment: (String) -> String? = System::getenv,
) : HimOpenAiApiKeyProvider {
    override fun apiKey(): String? = environment(OPENAI_API_KEY_ENVIRONMENT_NAME)?.takeIf(String::isNotBlank)
}

data class HimOpenAiUsageDiagnostics(val inputTokens: Long?, val outputTokens: Long?, val cachedInputTokens: Long?)
data class HimOpenAiInvocationDiagnostics(
    val physicalAttempts: Int,
    val providerModel: String?,
    val usage: HimOpenAiUsageDiagnostics?,
    val providerSuccess: Boolean,
    val attempts: List<HimSemanticProviderAttemptDiagnostic> = emptyList(),
)

fun interface HimOpenAiResponsesTransport {
    @Throws(IOException::class)
    fun execute(apiKey: String, body: JsonObject): Response<JsonObject>
}

class HimRetrofitOpenAiResponsesTransport private constructor(
    private val api: OpenAiResponsesApi,
) : HimOpenAiResponsesTransport {
    override fun execute(apiKey: String, body: JsonObject): Response<JsonObject> =
        api.create("Bearer $apiKey", body).execute()

    companion object {
        fun create(timeoutMilliseconds: Long): HimRetrofitOpenAiResponsesTransport {
            val client = OkHttpClient.Builder()
                .connectTimeout(timeoutMilliseconds, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMilliseconds, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMilliseconds, TimeUnit.MILLISECONDS)
                .callTimeout(timeoutMilliseconds, TimeUnit.MILLISECONDS)
                .build()
            val api = Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenAiResponsesApi::class.java)
            return HimRetrofitOpenAiResponsesTransport(api)
        }
    }
}

private interface OpenAiResponsesApi {
    @POST("v1/responses")
    fun create(@Header("Authorization") authorization: String, @Body body: JsonObject): Call<JsonObject>
}

class HimOpenAiSemanticInferenceProvider(
    private val configuration: HimOpenAiSemanticProviderConfiguration,
    private val apiKeyProvider: HimOpenAiApiKeyProvider,
    private val fixedContextFactory: HimOpenAiFixedContextFactory,
    private val transport: HimOpenAiResponsesTransport,
    private val contextPacker: HimSemanticContextPacker = HimSemanticContextPacker(
        HimSemanticContextBudgetPolicy.OPENAI_GPT_5_6_SOL_V1,
    ),
) : HimSemanticInferenceProvider {
    var diagnostics = HimOpenAiInvocationDiagnostics(0, null, null, false)
        private set

    override fun invoke(request: HimSemanticInferenceRequest): HimSemanticProviderOutcome {
        val key = apiKeyProvider.apiKey()
            ?: return failure(HimSemanticInferenceFailureKind.CONFIGURATION_ERROR, "OpenAI API credential is not configured", localDiagnostic())
        val packed = contextPacker.pack(request, fixedContextFactory.create(request))
        if (packed is HimSemanticContextPackingResult.ContextBudgetExceeded) {
            return failure(HimSemanticInferenceFailureKind.CONTEXT_BUDGET_EXCEEDED, "Semantic provider context exceeds the frozen input budget", localDiagnostic())
        }
        packed as HimSemanticContextPackingResult.Packed
        val body = HimOpenAiResponsesRequestSerializer(configuration).serialize(packed.providerInputJson)
        diagnostics = diagnostics.copy(physicalAttempts = diagnostics.physicalAttempts + 1)
        val response = try {
            transport.execute(key, body)
        } catch (_: SocketTimeoutException) {
            return failure(HimSemanticInferenceFailureKind.TIMEOUT, "OpenAI request timed out", noResponseDiagnostic())
        } catch (_: IOException) {
            return failure(HimSemanticInferenceFailureKind.TRANSPORT_ERROR, "OpenAI transport failed", noResponseDiagnostic())
        }
        if (!response.isSuccessful) return httpFailure(response)
        return decodeResponse(response.body())
    }

    private fun decodeResponse(response: JsonObject?): HimSemanticProviderOutcome {
        response ?: return failure(HimSemanticInferenceFailureKind.PROVIDER_ERROR, "OpenAI response body is absent")
        val model = response.stringOrNull("model")
        val usage = response.objectOrNull("usage")?.let {
            HimOpenAiUsageDiagnostics(
                it.longOrNull("input_tokens"),
                it.longOrNull("output_tokens"),
                it.objectOrNull("input_tokens_details")?.longOrNull("cached_tokens"),
            )
        }
        diagnostics = diagnostics.copy(providerModel = model, usage = usage)
        if (model != configuration.model) {
            return failure(HimSemanticInferenceFailureKind.PROVIDER_ERROR, "OpenAI returned an unexpected model identity")
        }
        if (response.stringOrNull("status") != "completed") {
            return failure(HimSemanticInferenceFailureKind.PROVIDER_ERROR, "OpenAI response did not complete")
        }
        val outputText = response.getAsJsonArray("output")?.asSequence().orEmpty()
            .filter { it.isJsonObject && it.asJsonObject.stringOrNull("type") == "message" }
            .flatMap { it.asJsonObject.getAsJsonArray("content")?.asSequence().orEmpty() }
            .firstOrNull { it.isJsonObject && it.asJsonObject.stringOrNull("type") == "output_text" }
            ?.asJsonObject?.stringOrNull("text")
            ?: return failure(HimSemanticInferenceFailureKind.SCHEMA_INVALID, "OpenAI response lacks structured output text")
        diagnostics = diagnostics.copy(providerSuccess = true)
        return HimSemanticProviderOutcome.StructuredResponse(outputText)
    }

    private fun httpFailure(response: Response<JsonObject>): HimSemanticProviderOutcome {
        val status = response.code()
        val safe = HimOpenAiSafeErrorDiagnosticDecoder.decode(status, runCatching { response.errorBody()?.string() }.getOrNull())
        return when (status) {
            401, 403 -> failure(HimSemanticInferenceFailureKind.CONFIGURATION_ERROR, "OpenAI authentication or permission failed", safe)
            408 -> failure(HimSemanticInferenceFailureKind.TIMEOUT, "OpenAI request timed out", safe)
            429 -> failure(HimSemanticInferenceFailureKind.RATE_LIMITED, "OpenAI rate limit or quota prevented execution", safe)
            in 500..599 -> failure(HimSemanticInferenceFailureKind.PROVIDER_ERROR, "OpenAI service failed", safe)
            else -> failure(HimSemanticInferenceFailureKind.PROVIDER_ERROR, "OpenAI rejected the request", safe)
        }
    }

    private fun failure(
        kind: HimSemanticInferenceFailureKind,
        message: String,
        safeProviderDiagnostic: HimSemanticProviderErrorDiagnostic? = null,
    ): HimSemanticProviderOutcome {
        val attempt = if (safeProviderDiagnostic != null && diagnostics.physicalAttempts > diagnostics.attempts.size) {
            HimSemanticProviderAttemptDiagnostic(diagnostics.attempts.size + 1, kind, safeProviderDiagnostic)
        } else null
        diagnostics = diagnostics.copy(providerSuccess = false, attempts = diagnostics.attempts + listOfNotNull(attempt))
        return HimSemanticProviderOutcome.TechnicalFailure(kind, message, safeProviderDiagnostic)
    }

    private fun localDiagnostic() = HimSemanticProviderErrorDiagnostic("OPENAI", null, null, null, null, null, false, false, false)
    private fun noResponseDiagnostic() = HimSemanticProviderErrorDiagnostic("OPENAI", null, null, null, null, null, null, false, false)

    private fun JsonObject.stringOrNull(name: String) =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
    private fun JsonObject.objectOrNull(name: String) = get(name)?.takeIf { it.isJsonObject }?.asJsonObject
    private fun JsonObject.longOrNull(name: String) =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asLong
}

object HimOpenAiSafeErrorDiagnosticDecoder {
    private const val IDENTIFIER_MAX = 128
    private const val PARAM_MAX = 256
    private val identifier = Regex("[A-Za-z0-9_-]+")
    private val parameter = Regex("[A-Za-z0-9_.\\[\\]-]+")

    fun decode(statusCode: Int, transientRawBody: String?): HimSemanticProviderErrorDiagnostic {
        val error = runCatching {
            transientRawBody?.let(JsonParser::parseString)?.takeIf { it.isJsonObject }?.asJsonObject
                ?.get("error")?.takeIf { it.isJsonObject }?.asJsonObject
        }.getOrNull()
        return HimSemanticProviderErrorDiagnostic(
            provider = "OPENAI",
            httpStatusCode = statusCode,
            httpStatusFamily = statusCode / 100,
            providerErrorType = safe(error?.stringOrNull("type"), IDENTIFIER_MAX, identifier),
            providerErrorCode = safe(error?.stringOrNull("code"), IDENTIFIER_MAX, identifier),
            providerErrorParam = safe(error?.stringOrNull("param"), PARAM_MAX, parameter),
            requestReachedProvider = true,
            providerResponseReceived = true,
            usageReceived = false,
        )
    }

    private fun safe(value: String?, maximum: Int, pattern: Regex) = value?.takeIf { it.length <= maximum && pattern.matches(it) }
    private fun JsonObject.stringOrNull(name: String) =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
}

class HimOpenAiResponsesRequestSerializer(
    private val configuration: HimOpenAiSemanticProviderConfiguration,
) {
    fun serialize(packedProviderInputJson: String): JsonObject {
        val context = JsonParser.parseString(packedProviderInputJson).asJsonObject
        val instructions = context.remove("instructions").asString
        return JsonObject().apply {
            addProperty("model", configuration.model)
            addProperty("instructions", instructions)
            add("input", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    add("content", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("type", "input_text")
                            addProperty("text", context.toString())
                        })
                    })
                })
            })
            add("reasoning", JsonObject().apply { addProperty("effort", configuration.reasoningEffort) })
            addProperty("max_output_tokens", configuration.maxOutputTokens)
            addProperty("store", configuration.storeProviderResponse)
            addProperty("truncation", "disabled")
            add("tools", JsonArray())
            add("text", JsonObject().apply {
                add("format", JsonObject().apply {
                    addProperty("type", "json_schema")
                    addProperty("name", "him_semantic_inference_output_v2_1")
                    addProperty("strict", true)
                    add("schema", HimOpenAiSemanticOutputJsonSchema.V2.deepCopy())
                })
            })
        }
    }
}

object HimOpenAiSemanticOutputJsonSchema {
    val V2: JsonObject = objectSchema(linkedMapOf(
        "schemaVersion" to stringSchema(constant = HimSemanticInferenceSchema.OUTPUT_VERSION),
        "candidates" to arraySchema(JsonObject().apply {
            add("anyOf", JsonArray().apply {
                add(candidateSchema("CREATE_NEW_CANONICAL", objectSchema(linkedMapOf())))
                add(candidateSchema("IDENTITY", objectSchema(linkedMapOf("parentCanonicalId" to stringSchema()))))
                val scoped = objectSchema(linkedMapOf(
                    "scope" to stringSchema(enums = listOf("CANONICAL", "IDENTITY")),
                    "canonicalId" to stringSchema(),
                    "identityId" to nullableStringSchema(),
                ))
                add(candidateSchema("VARIANT", scoped))
                add(candidateSchema("ALIAS", scoped))
            })
        }),
        "informationGain" to stringSchema(enums = listOf("MORE_EVIDENCE_MAY_HELP", "NO_EXPECTED_INFORMATION_GAIN")),
        "retrievalDirective" to nullableObjectSchema(objectSchema(linkedMapOf(
            "sourceQueries" to arraySchema(objectSchema(linkedMapOf(
                "source" to stringSchema(enums = listOf("OPEN_FOOD_FACTS", "AGRIBALYSE", "CIQUAL", "GLYCEMIC_INDEX")),
                "queries" to boundedStringArraySchema(1, 3),
            ))),
            "informationGainJudgment" to stringSchema(constant = "MORE_EVIDENCE_MAY_HELP"),
        ))),
        "authorityConflicts" to arraySchema(authorityConflictSchema()),
    ))

    private fun candidateSchema(type: String, relation: JsonObject) = objectSchema(linkedMapOf(
        "proposalReference" to stringSchema(),
        "candidateTerm" to stringSchema(),
        "candidateType" to stringSchema(constant = type),
        "relation" to relation,
        "confidence" to stringSchema(enums = listOf("HIGH", "MEDIUM", "LOW", "NO_CONFIDENCE")),
        "evidenceOrigin" to stringSchema(enums = listOf("SOURCE_SUPPORTED", "MODEL_DERIVED", "MIXED")),
        "evidenceAssessments" to arraySchema(evidenceAssessmentSchema()),
        "shortRationale" to stringSchema(),
    ))

    private fun evidenceProperties() = linkedMapOf(
        "source" to stringSchema(enums = HimGroundTruthSource.entries.map { it.name }),
        "sourceArtifactSha256" to stringSchema(pattern = "^[0-9a-f]{64}${'$'}"),
        "sourceRecordIdentity" to stringSchema(),
    )
    private fun evidenceAssessmentSchema() = objectSchema(evidenceProperties().apply {
        put("relation", stringSchema(enums = listOf("DIRECT", "RELATED", "PARENT", "INGREDIENT", "VARIANT")))
    })
    private fun authorityConflictSchema() = objectSchema(linkedMapOf(
        "authorityEntityReference" to stringSchema(),
        "conflictingEvidence" to arraySchema(objectSchema(evidenceProperties())),
        "shortRationale" to stringSchema(),
    ))
    private fun objectSchema(properties: LinkedHashMap<String, JsonObject>) = JsonObject().apply {
        addProperty("type", "object")
        addProperty("additionalProperties", false)
        add("properties", JsonObject().apply { properties.forEach(::add) })
        add("required", JsonArray().apply { properties.keys.forEach(::add) })
    }
    private fun arraySchema(items: JsonObject) = JsonObject().apply {
        addProperty("type", "array")
        add("items", items)
    }
    private fun stringSchema(constant: String? = null, enums: List<String>? = null, pattern: String? = null) =
        JsonObject().apply {
            addProperty("type", "string")
            constant?.let { add("enum", JsonArray().apply { add(it) }) }
            enums?.let { add("enum", JsonArray().apply { it.forEach(::add) }) }
            pattern?.let { addProperty("pattern", it) }
        }
    private fun nullableStringSchema() = JsonObject().apply {
        add("type", JsonArray().apply { add("string"); add("null") })
    }
    private fun nullableObjectSchema(value: JsonObject) = JsonObject().apply {
        add("anyOf", JsonArray().apply {
            add(value)
            add(JsonObject().apply { addProperty("type", "null") })
        })
    }
    private fun boundedStringArraySchema(min: Int, max: Int) = JsonObject().apply {
        addProperty("type", "array")
        add("items", stringSchema())
        addProperty("minItems", min)
        addProperty("maxItems", max)
    }
}
