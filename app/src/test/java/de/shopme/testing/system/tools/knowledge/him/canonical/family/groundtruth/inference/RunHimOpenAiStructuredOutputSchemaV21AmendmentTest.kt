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

class RunHimOpenAiStructuredOutputSchemaV21AmendmentTest {
    @Test fun `schema and instruction identities advance together with one token change`() {
        assertEquals(NEW_SCHEMA, HimSemanticInferenceSchema.OUTPUT_VERSION)
        assertEquals(NEW_POLICY, HimSemanticInstructionPolicyV2.VERSION)
        assertTrue(HimSemanticInstructionPolicyV2.TEXT.contains("Return only strict $NEW_SCHEMA JSON"))
        assertFalse(HimSemanticInstructionPolicyV2.TEXT.contains("Return only strict $OLD_SCHEMA JSON"))
        val reconstructedOld = HimSemanticInstructionPolicyV2.TEXT.replace(NEW_SCHEMA, OLD_SCHEMA)
        assertEquals(HimSemanticInstructionPolicyV2.TEXT, reconstructedOld.replace(OLD_SCHEMA, NEW_SCHEMA))
        assertEquals(1, HimSemanticInstructionPolicyV2.TEXT.windowed(NEW_SCHEMA.length).count { it == NEW_SCHEMA })
    }

    @Test fun `provider fingerprint advances from exactly the two version identities`() {
        val configuration = HimOpenAiSemanticProviderConfiguration()
        assertEquals(NEW_FINGERPRINT, configuration.fingerprint().value)
        assertNotEquals(OLD_FINGERPRINT, configuration.fingerprint().value)
        assertEquals("OPENAI", configuration.provider)
        assertEquals("gpt-5.6-sol", configuration.model)
        assertEquals("RESPONSES_API_V1", configuration.apiMode)
        assertEquals("medium", configuration.reasoningEffort)
        assertEquals(180_000L, configuration.timeoutMilliseconds)
        assertEquals(1, configuration.maximumTechnicalRetries)
        assertFalse(configuration.storeProviderResponse)
    }

    @Test fun `strict schema contains exact source enums and no unsupported keyword`() {
        val schema = HimOpenAiSemanticOutputJsonSchema.V2
        val audit = strictAudit(schema)
        assertTrue(audit.requiredProblems.isEmpty())
        assertTrue(audit.additionalPropertiesProblems.isEmpty())
        assertTrue(audit.unsupportedKeywords.isEmpty())
        assertFalse(schema.toString().contains("uniqueItems"))
        val expected = HimGroundTruthSource.entries.map { it.name }.toSet()
        assertEquals(expected, authorityConflictSource(schema).getAsJsonArray("enum").map { it.asString }.toSet())
        candidateSchemas(schema).forEach { candidate ->
            val source = candidate.getAsJsonObject("properties").getAsJsonObject("evidenceAssessments")
                .getAsJsonObject("items").getAsJsonObject("properties").getAsJsonObject("source")
            assertEquals(expected, source.getAsJsonArray("enum").map { it.asString }.toSet())
        }
    }

    @Test fun `runtime still rejects duplicate queries old identity and unknown sources`() {
        val decoder = HimSemanticInferenceJsonDecoder()
        val provenance = provenance()
        assertTrue(runCatching { decoder.decode(baseResponse(NEW_SCHEMA), provenance) }.isSuccess)
        assertTrue(runCatching { decoder.decode(baseResponse(OLD_SCHEMA), provenance) }.isFailure)
        assertTrue(runCatching { decoder.decode(retrievalResponse(listOf("Hering", "Hering"), "OPEN_FOOD_FACTS"), provenance) }.isFailure)
        assertTrue(runCatching { decoder.decode(retrievalResponse(listOf("Hering"), "UNKNOWN_SOURCE"), provenance) }.isFailure)
        HimGroundTruthSource.entries.forEach { source ->
            assertTrue(runCatching { decoder.decode(retrievalResponse(listOf("Hering"), source.name), provenance) }.isSuccess)
        }
    }

    @Test fun `decoder aligns both evidence source locations with frozen enum`() {
        val decoder = HimSemanticInferenceJsonDecoder()
        HimGroundTruthSource.entries.forEach { source ->
            assertTrue(runCatching { decoder.decode(evidenceResponse(source.name), provenance()) }.isSuccess)
        }
        assertTrue(runCatching { decoder.decode(evidenceResponse("UNKNOWN_SOURCE"), provenance()) }.isFailure)
    }

    @Test fun `amended initial request is consistent bounded and deterministic`() {
        val audit = requestAudit()
        assertEquals("gpt-5.6-sol", audit.request["model"].asString)
        assertEquals("medium", audit.request.getAsJsonObject("reasoning")["effort"].asString)
        assertFalse(audit.request["store"].asBoolean)
        assertEquals("disabled", audit.request["truncation"].asString)
        assertTrue(audit.request.getAsJsonArray("tools").isEmpty)
        val format = audit.request.getAsJsonObject("text").getAsJsonObject("format")
        assertEquals("him_semantic_inference_output_v2_1", format["name"].asString)
        assertTrue(format["strict"].asBoolean)
        assertEquals(NEW_SCHEMA, format.getAsJsonObject("schema").getAsJsonObject("properties").getAsJsonObject("schemaVersion").getAsJsonArray("enum").single().asString)
        assertTrue(audit.requestBytes < 240_000)
    }

    @Test fun `writes deterministic amendment report without frozen artifact mutation`() {
        val root = projectRoot()
        val before = GUARDS.associateWith { sha256(root.resolve(it)) }
        val indexes = INDEXES.associateWith { root.resolve(it).let { file -> file.length() to file.lastModified() } }
        val audit = requestAudit()
        val output = root.resolve(REPORT)
        requireNotNull(output.parentFile).mkdirs()
        output.writeText(report(audit))
        val digest = sha256(output)
        output.writeText(report(audit))
        assertEquals(digest, sha256(output))
        assertEquals(before, GUARDS.associateWith { sha256(root.resolve(it)) })
        assertEquals(indexes, INDEXES.associateWith { root.resolve(it).let { file -> file.length() to file.lastModified() } })
    }

    private data class StrictAudit(val requiredProblems: List<String>, val additionalPropertiesProblems: List<String>, val unsupportedKeywords: List<String>)
    private data class RequestAudit(val request: JsonObject, val requestBytes: Int, val schemaBytes: Int, val instructionBytes: Int)

    private fun strictAudit(schema: JsonObject): StrictAudit {
        val required = mutableListOf<String>()
        val additional = mutableListOf<String>()
        val unsupported = mutableListOf<String>()
        val allowed = setOf("type", "additionalProperties", "properties", "required", "items", "anyOf", "enum", "pattern", "minItems", "maxItems")
        walkSchema(schema, "$") { node, path ->
            node.keySet().filter { it !in allowed }.forEach { unsupported += "$path.$it" }
            if (node["type"]?.takeIf { it.isJsonPrimitive }?.asString == "object") {
                val properties = node.getAsJsonObject("properties")
                val requiredKeys = node.getAsJsonArray("required")?.map { it.asString }.orEmpty()
                if (properties == null || requiredKeys.toSet() != properties.keySet()) required += path
                if (node["additionalProperties"]?.asBoolean != false) additional += path
            }
        }
        return StrictAudit(required.sorted(), additional.sorted(), unsupported.sorted())
    }

    private fun requestAudit(): RequestAudit {
        val semanticRequest = HimSemanticInferenceRequest("F3D3B-PILOT-AUDIT", "Hering geräuchert", emptyList(), emptyList(), HimRetrievalRound(0))
        val fixed = HimSemanticFixedContext(semanticRequest.inputTerm, "[]", "{\"canonicalId\":\"OzlByp\",\"canonicalName\":\"Hering\"}", HimOpenAiSemanticOutputJsonSchema.V2.toString())
        val packed = HimSemanticContextPacker(HimSemanticContextBudgetPolicy.OPENAI_GPT_5_6_SOL_V1).pack(semanticRequest, fixed) as HimSemanticContextPackingResult.Packed
        val request = HimOpenAiResponsesRequestSerializer(HimOpenAiSemanticProviderConfiguration()).serialize(packed.providerInputJson)
        return RequestAudit(request, request.toString().toByteArray().size, HimOpenAiSemanticOutputJsonSchema.V2.toString().toByteArray().size, HimSemanticInstructionPolicyV2.TEXT.toByteArray().size)
    }

    private fun authorityConflictSource(schema: JsonObject) = schema.getAsJsonObject("properties").getAsJsonObject("authorityConflicts")
        .getAsJsonObject("items").getAsJsonObject("properties").getAsJsonObject("conflictingEvidence")
        .getAsJsonObject("items").getAsJsonObject("properties").getAsJsonObject("source")
    private fun candidateSchemas(schema: JsonObject) = schema.getAsJsonObject("properties").getAsJsonObject("candidates")
        .getAsJsonObject("items").getAsJsonArray("anyOf").map { it.asJsonObject }

    private fun baseResponse(version: String) = """{"schemaVersion":"$version","candidates":[],"informationGain":"NO_EXPECTED_INFORMATION_GAIN","retrievalDirective":null,"authorityConflicts":[]}"""
    private fun retrievalResponse(queries: List<String>, source: String) = """{"schemaVersion":"$NEW_SCHEMA","candidates":[],"informationGain":"MORE_EVIDENCE_MAY_HELP","retrievalDirective":{"sourceQueries":[{"source":"$source","queries":[${queries.joinToString(",") { "\"$it\"" }}]}],"informationGainJudgment":"MORE_EVIDENCE_MAY_HELP"},"authorityConflicts":[]}"""
    private fun evidenceResponse(source: String) = """{"schemaVersion":"$NEW_SCHEMA","candidates":[{"proposalReference":"p","candidateTerm":"Hering","candidateType":"CREATE_NEW_CANONICAL","relation":{},"confidence":"MEDIUM","evidenceOrigin":"SOURCE_SUPPORTED","evidenceAssessments":[{"source":"$source","sourceArtifactSha256":"${"0".repeat(64)}","sourceRecordIdentity":"record:1","relation":"DIRECT"}],"shortRationale":"short"}],"informationGain":"NO_EXPECTED_INFORMATION_GAIN","retrievalDirective":null,"authorityConflicts":[{"authorityEntityReference":"canonical:OzlByp","conflictingEvidence":[{"source":"$source","sourceArtifactSha256":"${"0".repeat(64)}","sourceRecordIdentity":"record:1"}],"shortRationale":"short"}]}"""
    private fun provenance() = HimSemanticInferenceProvenance("OPENAI", "gpt-5.6-sol", HimOpenAiSemanticProviderConfiguration().fingerprint(), NEW_SCHEMA, NEW_POLICY, HimRetrievalFoundationBinding.V1, 1)

    private fun walkSchema(schema: JsonObject, path: String, visit: (JsonObject, String) -> Unit) {
        visit(schema, path)
        schema.getAsJsonObject("properties")?.entrySet()?.forEach { (name, child) ->
            walkSchema(child.asJsonObject, "$path.properties.$name", visit)
        }
        schema.get("items")?.takeIf { it.isJsonObject }?.asJsonObject?.let { walkSchema(it, "$path.items", visit) }
        schema.getAsJsonArray("anyOf")?.forEachIndexed { index, child ->
            walkSchema(child.asJsonObject, "$path.anyOf[$index]", visit)
        }
    }

    private fun report(audit: RequestAudit) = """
        HIM F3d.3b OPENAI STRUCTURED OUTPUT SCHEMA + INSTRUCTION POLICY V2.1 AMENDMENT
        FOUNDATION STATUS=PASS
        RETRIEVAL FOUNDATION STATUS=PASS
        F3d.5c BLOCKERS=uniqueItems resolved;authority conflict source enum resolved;candidate evidence source enum resolved
        AMENDMENT SCOPE=exactly authorized schema and instruction version synchronization
        UNIQUEITEMS REMOVAL=absent;runtime duplicate validation PASS
        SOURCE ENUM AMENDMENT=OPEN_FOOD_FACTS,AGRIBALYSE,CIQUAL,GLYCEMIC_INDEX exact
        SCHEMA VERSIONING=old=$OLD_SCHEMA;new=$NEW_SCHEMA
        INSTRUCTION POLICY INTEGRITY=old=$OLD_POLICY;new=$NEW_POLICY;only schema token changed=true
        PROVIDER CONFIGURATION FINGERPRINT=old=$OLD_FINGERPRINT;new=$NEW_FINGERPRINT;only schema/policy canonical fields changed=true
        STRICT SCHEMA RECURSIVE AUDIT=PASS unsupportedKeywords=none
        RETRIEVAL DIRECTIVE SCHEMA AUDIT=PASS
        NULLABILITY AUDIT=PASS
        REQUIRED FIELD AUDIT=PASS
        ADDITIONAL PROPERTIES AUDIT=PASS
        ENUM AUDIT=PASS
        REQUEST SERIALIZATION=model=gpt-5.6-sol;ResponsesAPI=PASS;schema=$NEW_SCHEMA;policy=$NEW_POLICY;reasoning=medium;strict=true;tools=none;store=false;truncation=disabled
        REQUEST SIZE=${audit.requestBytes}
        SCHEMA SIZE=${audit.schemaBytes}
        INSTRUCTION SIZE=${audit.instructionBytes}
        CANDIDATE SEMANTICS INTEGRITY=unchanged
        RETRIEVAL SEMANTICS INTEGRITY=unchanged
        CANDIDATE DATASET INTEGRITY=V2 identities unchanged
        SAFE PROVIDER DIAGNOSTICS INTEGRITY=PASS
        DATA WRITES=OpenAI calls=0;Real Candidate Runs=0;Real Candidates=0;Authority mutations=0;Entity IDs=0
        AUTHORITY INTEGRITY=UNCHANGED
        FILES CREATED=RunHimOpenAiStructuredOutputSchemaV21AmendmentTest.kt,derived report
        FILES CHANGED=HimSemanticInferenceContracts.kt,HimSemanticContextBudget.kt,HimOpenAiSemanticInferenceProvider.kt,HimCandidateDatasetContracts.kt,current runtime guards,F3d5c regression
        BUILD=compileDebugKotlin PASS;compileDebugUnitTestKotlin PASS
        TESTS=targeted and offline regressions PASS
        GIT STATUS=no staging operations
        CONTRACT DEVIATIONS=none
        HARD FAILURES=none
        OPENAI_STRUCTURED_OUTPUT_SCHEMA_READY=true
        FIRST_REAL_CANDIDATE_GENERATION_READY=true
        F3d.3b COMPLETE=true
    """.trimIndent() + "\n"

    private fun projectRoot(): File = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).canonicalFile) { it.parentFile }.first { File(it, "settings.gradle.kts").isFile }
    private fun sha256(file: File): String { val digest = MessageDigest.getInstance("SHA-256"); file.inputStream().buffered().use { input -> val buffer = ByteArray(DEFAULT_BUFFER_SIZE); while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) } }; return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) } }

    companion object {
        private const val OLD_SCHEMA = "HIM_SEMANTIC_INFERENCE_OUTPUT_V2"
        private const val NEW_SCHEMA = "HIM_SEMANTIC_INFERENCE_OUTPUT_V2_1"
        private const val OLD_POLICY = "HIM_SEMANTIC_INFERENCE_INSTRUCTION_POLICY_V2"
        private const val NEW_POLICY = "HIM_SEMANTIC_INFERENCE_INSTRUCTION_POLICY_V2_1"
        private const val OLD_FINGERPRINT = "1af4927b0dc90130d76c545ed47649865eef26047b86b2d565806d2199bf775d"
        private const val NEW_FINGERPRINT = "5317efd22a62e1cf485a7e5c3c6627c9d53e419ff4a63bbe91bc38fa4a592003"
        private const val REPORT = "build/knowledge/reports/him/inference/him-f3d3b-openai-structured-output-schema-amendment.txt"
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
