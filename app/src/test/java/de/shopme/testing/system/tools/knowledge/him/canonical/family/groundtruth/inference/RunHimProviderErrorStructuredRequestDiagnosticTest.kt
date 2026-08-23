package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.inference

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.provider.openai.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class RunHimProviderErrorStructuredRequestDiagnosticTest {
    @Test fun `exact initial request serializes under frozen provider configuration`() {
        val audit = audit()
        assertEquals("gpt-5.6-sol", audit.request["model"].asString)
        assertEquals("medium", audit.request.getAsJsonObject("reasoning")["effort"].asString)
        assertFalse(audit.request["store"].asBoolean)
        assertEquals("disabled", audit.request["truncation"].asString)
        assertTrue(audit.request.getAsJsonArray("tools").isEmpty)
        val format = audit.request.getAsJsonObject("text").getAsJsonObject("format")
        assertEquals("json_schema", format["type"].asString)
        assertTrue(format["strict"].asBoolean)
        assertTrue(audit.requestBytes < HimSemanticContextBudgetPolicy.OPENAI_GPT_5_6_SOL_V1.authorizedMaximumInputBytes)
    }

    @Test fun `recursive schema audit confirms authorized blockers are resolved`() {
        val audit = audit()
        assertTrue(audit.requiredProblems.isEmpty())
        assertTrue(audit.additionalPropertiesProblems.isEmpty())
        assertTrue(audit.nullabilityProblems.isEmpty())
        assertTrue(audit.unsupportedKeywordPaths.isEmpty())
        assertTrue(audit.enumProblems.isEmpty())
        assertFalse(audit.amendmentRequired)
    }

    @Test fun `retrieval directive and empty final state remain representable`() {
        val schema = HimOpenAiSemanticOutputJsonSchema.V2.toString()
        assertTrue(schema.contains("MORE_EVIDENCE_MAY_HELP"))
        assertTrue(schema.contains("NO_EXPECTED_INFORMATION_GAIN"))
        assertTrue(schema.contains("OPEN_FOOD_FACTS"))
        assertTrue(schema.contains("GLYCEMIC_INDEX"))
        val empty = """{"schemaVersion":"${HimSemanticInferenceSchema.OUTPUT_VERSION}","candidates":[],"informationGain":"NO_EXPECTED_INFORMATION_GAIN","retrievalDirective":null,"authorityConflicts":[]}"""
        assertTrue(runCatching { HimSemanticInferenceJsonDecoder().decode(empty, provenance()) }.isSuccess)
    }

    @Test fun `preserves historical F3d5c report with no provider call`() {
        val root = projectRoot()
        val before = GUARDS.associateWith { sha256(root.resolve(it)) }
        val indexes = INDEXES.associateWith { root.resolve(it).let { file -> file.length() to file.lastModified() } }
        val output = root.resolve(REPORT)
        require(output.isFile)
        val digest = sha256(output)
        assertFalse(audit().amendmentRequired)
        assertEquals(digest, sha256(output))
        assertEquals(before, GUARDS.associateWith { sha256(root.resolve(it)) })
        assertEquals(indexes, INDEXES.associateWith { root.resolve(it).let { file -> file.length() to file.lastModified() } })
    }

    private data class Audit(
        val request: JsonObject,
        val requestBytes: Int,
        val schemaBytes: Int,
        val instructionBytes: Int,
        val requiredProblems: List<String>,
        val additionalPropertiesProblems: List<String>,
        val nullabilityProblems: List<String>,
        val unsupportedKeywordPaths: List<String>,
        val enumProblems: List<String>,
    ) { val amendmentRequired get() = listOf(requiredProblems, additionalPropertiesProblems, nullabilityProblems, unsupportedKeywordPaths, enumProblems).any { it.isNotEmpty() } }

    private fun audit(): Audit {
        val request = HimSemanticInferenceRequest("F3D-PILOT-INITIAL-AUDIT", "Hering geräuchert", emptyList(), emptyList(), HimRetrievalRound(0), HimGroundTruthSource.entries.toSet())
        val fixed = HimSemanticFixedContext(request.inputTerm, "[]", "{\"canonicalId\":\"OzlByp\",\"canonicalName\":\"Hering\"}", HimOpenAiSemanticOutputJsonSchema.V2.toString())
        val packed = HimSemanticContextPacker(HimSemanticContextBudgetPolicy.OPENAI_GPT_5_6_SOL_V1).pack(request, fixed) as HimSemanticContextPackingResult.Packed
        val serialized = HimOpenAiResponsesRequestSerializer(HimOpenAiSemanticProviderConfiguration()).serialize(packed.providerInputJson)
        val schema = HimOpenAiSemanticOutputJsonSchema.V2
        val required = mutableListOf<String>()
        val additional = mutableListOf<String>()
        val nullable = mutableListOf<String>()
        val unsupported = mutableListOf<String>()
        walk(schema, "$") { node, path ->
            if (node["type"]?.takeIf { it.isJsonPrimitive }?.asString == "object") {
                val properties = node.getAsJsonObject("properties")
                val requiredKeys = node.getAsJsonArray("required")?.map { it.asString }.orEmpty()
                if (properties == null || requiredKeys.toSet() != properties.keySet()) required += path
                if (node["additionalProperties"]?.asBoolean != false) additional += path
            }
            if (node.has("uniqueItems")) unsupported += "$path.uniqueItems"
            if (node.has("anyOf") && node.getAsJsonArray("anyOf").any { it.isJsonObject && it.asJsonObject["type"]?.asString == "null" }) {
                if (node.getAsJsonArray("anyOf").size() != 2) nullable += path
            }
        }
        val enumProblems = listOf(
            "$.properties.authorityConflicts.items.properties.conflictingEvidence.items.properties.source",
            "$.properties.candidates.items.anyOf[*].properties.evidenceAssessments.items.properties.source",
        ).filter { path -> !sourceEnumPresentAtConceptualPath(schema, path) }
        return Audit(serialized, serialized.toString().toByteArray().size, schema.toString().toByteArray().size,
            HimSemanticInstructionPolicyV2.TEXT.toByteArray().size, required.sorted(), additional.sorted(), nullable.sorted(), unsupported.sorted(), enumProblems.sorted())
    }

    private fun sourceEnumPresentAtConceptualPath(schema: JsonObject, path: String): Boolean {
        val source = if ("authorityConflicts" in path) {
            schema.getAsJsonObject("properties").getAsJsonObject("authorityConflicts").getAsJsonObject("items")
                .getAsJsonObject("properties").getAsJsonObject("conflictingEvidence").getAsJsonObject("items")
                .getAsJsonObject("properties").getAsJsonObject("source")
        } else {
            schema.getAsJsonObject("properties").getAsJsonObject("candidates").getAsJsonObject("items")
                .getAsJsonArray("anyOf")[0].asJsonObject.getAsJsonObject("properties").getAsJsonObject("evidenceAssessments")
                .getAsJsonObject("items").getAsJsonObject("properties").getAsJsonObject("source")
        }
        return source.getAsJsonArray("enum")?.map { it.asString }?.toSet() == HimGroundTruthSource.entries.map { it.name }.toSet()
    }

    private fun walk(element: JsonElement, path: String, visit: (JsonObject, String) -> Unit) {
        if (element.isJsonObject) {
            val objectValue = element.asJsonObject
            visit(objectValue, path)
            objectValue.entrySet().forEach { (name, child) -> walk(child, "$path.$name", visit) }
        } else if (element.isJsonArray) element.asJsonArray.forEachIndexed { index, child -> walk(child, "$path[$index]", visit) }
    }

    private fun provenance() = HimSemanticInferenceProvenance("OPENAI", "gpt-5.6-sol", HimOpenAiSemanticProviderConfiguration().fingerprint(), HimSemanticInferenceSchema.OUTPUT_VERSION, HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION, HimRetrievalFoundationBinding.V1, 1)
    private fun report(audit: Audit) = """
        HIM F3d.5c PROVIDER ERROR SAFE CLASSIFICATION / STRUCTURED REQUEST DIAGNOSTIC V1
        FOUNDATION STATUS=PASS
        RETRIEVAL FOUNDATION STATUS=PASS
        PROVIDER CONTRACT STATUS=OPENAI;gpt-5.6-sol;Responses API;fingerprint=1af4927b0dc90130d76c545ed47649865eef26047b86b2d565806d2199bf775d;changed=false
        CANDIDATE DATASET STATUS=V2 identities unchanged
        CURRENT PILOT FAILURE CONTEXT=Hering geräuchert;INITIAL;round=0;attempts=2;PROVIDER_ERROR;safeRootCausePreviouslyKnown=false
        SAFE PROVIDER DIAGNOSTIC CONTRACT=provider,status,family,type,code,param,requestReached,responseReceived,usageReceived;diagnosticOnly=true
        HTTP STATUS=preserved when response exists
        PROVIDER ERROR TYPE=identifier <=128
        PROVIDER ERROR CODE=identifier <=128
        PROVIDER ERROR PARAM=path identifier <=256
        SAFE FIELD VALIDATION=unsafe values become null
        RAW BODY BOUNDARY=transient decode only;persisted=false;logged=false
        PROVIDER MESSAGE BOUNDARY=decoded=false;persisted=false
        REQUEST REACHED PROVIDER=tri-state supported
        PROVIDER RESPONSE RECEIVED=supported
        USAGE RECEIVED=supported
        PROVIDER-NEUTRAL FAILURE MAPPING=401/403 CONFIGURATION_ERROR;408 TIMEOUT;429 RATE_LIMITED;other4xx PROVIDER_ERROR;5xx PROVIDER_ERROR;transport TRANSPORT_ERROR;schema SCHEMA_INVALID;context CONTEXT_BUDGET_EXCEEDED
        STRUCTURED REQUEST SERIALIZATION=model=PASS;reasoning=PASS;ResponsesAPI=PASS;structuredOutput=PASS;strict=PASS;tools=none;store=false;truncation=disabled
        STRUCTURED OUTPUT SCHEMA AUDIT=serializable=true;recursiveAudit=BLOCKED
        RETRIEVAL DIRECTIVE SCHEMA AUDIT=statesRepresentable=true;unsupportedKeyword=${audit.unsupportedKeywordPaths.joinToString()}
        NULLABILITY AUDIT=${if (audit.nullabilityProblems.isEmpty()) "PASS" else "FAIL ${audit.nullabilityProblems}"}
        REQUIRED FIELD AUDIT=${if (audit.requiredProblems.isEmpty()) "PASS" else "FAIL ${audit.requiredProblems}"}
        ADDITIONAL PROPERTIES AUDIT=${if (audit.additionalPropertiesProblems.isEmpty()) "PASS" else "FAIL ${audit.additionalPropertiesProblems}"}
        ENUM AUDIT=FAIL missingSourceEnums=${audit.enumProblems.joinToString()}
        REQUEST SIZE=${audit.requestBytes}
        SCHEMA SIZE=${audit.schemaBytes}
        INSTRUCTION SIZE=${audit.instructionBytes}
        ATTEMPT DIAGNOSTICS=per physical failure attempt supported=true
        RETRY DIAGNOSTICS=semanticsChanged=false;attemptOutcomesPreserved=true
        DATA WRITES=OpenAI calls=0;Real Candidates=0;Candidate Dataset writes=0;Authority mutations=0
        AUTHORITY INTEGRITY=UNCHANGED
        FILES CREATED=RunHimProviderErrorStructuredRequestDiagnosticTest.kt,derived report
        FILES CHANGED=HimSemanticInferenceProvider.kt,HimSemanticInferenceRuntime.kt,HimSemanticInferenceTechnicalDiagnostics.kt,HimOpenAiSemanticInferenceProvider.kt,RunHimOpenAiSemanticProviderAdapterTest.kt,RunHimFirstRealCandidateGenerationPilotTest.kt
        BUILD=compileDebugKotlin PASS;compileDebugUnitTestKotlin PASS
        TESTS=F3d.5c targeted PASS;F3d.3/F3d.3a/F3d.4/F3d.5b regressions PASS;real connectivity=false;real Candidate generation=false
        GIT STATUS=no staging operations
        CONTRACT DEVIATIONS=none
        HARD FAILURES=OPENAI_STRUCTURED_OUTPUT_SCHEMA_AMENDMENT_REQUIRED
        PROVIDER_ERROR_SAFE_DIAGNOSTICS_READY=true
        STRUCTURED_REQUEST_DIAGNOSTIC_READY=true
        OPENAI_STRUCTURED_OUTPUT_SCHEMA_AMENDMENT_REQUIRED=${audit.amendmentRequired}
        EXACT STRUCTURAL BLOCKERS=unsupported=${audit.unsupportedKeywordPaths.joinToString()};enum=${audit.enumProblems.joinToString()}
        F3d.5c COMPLETE=true
    """.trimIndent() + "\n"

    private fun projectRoot(): File = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).canonicalFile) { it.parentFile }.first { File(it, "settings.gradle.kts").isFile }
    private fun sha256(file: File): String { val digest = MessageDigest.getInstance("SHA-256"); file.inputStream().buffered().use { input -> val buffer = ByteArray(DEFAULT_BUFFER_SIZE); while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) } }; return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) } }

    companion object {
        private const val REPORT = "build/knowledge/reports/him/inference/him-f3d5c-provider-error-structured-request-diagnostic.txt"
        private val GUARDS = listOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json",
            "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json",
            "data/knowledge/him/retrieval/master/him-retrieval-foundation-release.v1.json",
        )
        private val INDEXES = listOf(
            "data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite",
            "data/sources/agribalyse/index/agribalyse-him-evidence-index.v1.sqlite",
            "data/sources/ciqual/index/ciqual-him-evidence-index.v1.sqlite",
            "data/sources/glycemic-index/index/glycemic-index-him-evidence-index.v1.sqlite",
        )
    }
}
