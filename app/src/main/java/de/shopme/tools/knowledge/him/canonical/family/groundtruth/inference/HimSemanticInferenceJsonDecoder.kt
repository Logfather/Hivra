package de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource

class HimSemanticInferenceJsonDecoder : HimSemanticInferenceOutputDecoder {
    override fun decode(json: String, provenance: HimSemanticInferenceProvenance): HimSemanticInferenceSuccess = schema {
        val root = JsonParser.parseString(json).takeIf { it.isJsonObject }?.asJsonObject ?: invalid("Root must be an object")
        root.requireFields(setOf("schemaVersion", "candidates", "informationGain", "retrievalDirective", "authorityConflicts"), nullable = setOf("retrievalDirective"))
        require(root.string("schemaVersion") == HimSemanticInferenceSchema.OUTPUT_VERSION)
        val candidates = root.array("candidates").mapObjects(::candidate)
        val informationGain = enumValue<HimSemanticInformationGainJudgment>(root.string("informationGain"))
        val directive = root.get("retrievalDirective").takeUnless { it.isJsonNull }?.let {
            if (!it.isJsonObject) invalid("retrievalDirective must be an object or null")
            retrievalDirective(it.asJsonObject)
        }
        val conflicts = root.array("authorityConflicts").mapObjects(::conflict)
        HimSemanticInferenceSuccess(candidates, informationGain, directive, conflicts, provenance)
    }

    private fun retrievalDirective(value: JsonObject): HimSemanticRetrievalDirective {
        value.requireFields(setOf("sourceQueries", "informationGainJudgment"))
        val sourceQueries = value.array("sourceQueries").mapObjects { entry ->
            entry.requireFields(setOf("source", "queries"))
            HimSemanticSourceQueries(
                source = enumValue(entry.string("source")),
                queries = entry.array("queries").map { query ->
                    query.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
                        ?: invalid("query must be a string")
                },
            )
        }
        return HimSemanticRetrievalDirective(
            sourceQueries,
            enumValue(value.string("informationGainJudgment")),
        )
    }

    private fun candidate(value: JsonObject): HimSemanticCandidateProposal {
        value.requireFields(setOf("proposalReference", "candidateTerm", "candidateType", "relation", "confidence", "evidenceOrigin", "evidenceAssessments", "shortRationale"))
        val type = enumValue<HimCandidateType>(value.string("candidateType"))
        return HimSemanticCandidateProposal(
            proposalReference = value.string("proposalReference"),
            candidateTerm = value.string("candidateTerm"),
            relation = relation(type, value.obj("relation")),
            confidence = enumValue(value.string("confidence")),
            evidenceOrigin = enumValue(value.string("evidenceOrigin")),
            evidenceAssessments = value.array("evidenceAssessments").mapObjects(::assessment),
            shortRationale = value.string("shortRationale"),
        )
    }

    private fun relation(type: HimCandidateType, value: JsonObject): HimCandidateRelation = when (type) {
        HimCandidateType.IDENTITY -> {
            value.requireFields(setOf("parentCanonicalId"))
            HimCandidateRelation.Identity(HimEntityId(value.string("parentCanonicalId")))
        }
        HimCandidateType.VARIANT -> {
            value.requireFields(setOf("scope", "canonicalId", "identityId"), nullable = setOf("identityId"))
            HimCandidateRelation.Variant(entityReference(value))
        }
        HimCandidateType.ALIAS -> {
            value.requireFields(setOf("scope", "canonicalId", "identityId"), nullable = setOf("identityId"))
            HimCandidateRelation.Alias(entityReference(value))
        }
        HimCandidateType.CREATE_NEW_CANONICAL -> {
            value.requireFields(emptySet())
            HimCandidateRelation.CreateNewCanonical
        }
    }

    private fun entityReference(value: JsonObject): HimFamilyEntityReference = when (value.string("scope")) {
        "CANONICAL" -> {
            require(value.get("identityId").isJsonNull)
            HimFamilyEntityReference.Canonical(HimEntityId(value.string("canonicalId")))
        }
        "IDENTITY" -> HimFamilyEntityReference.Identity(HimEntityId(value.string("canonicalId")), HimEntityId(value.string("identityId")))
        else -> invalid("Invalid entity scope")
    }

    private fun assessment(value: JsonObject): HimSemanticEvidenceAssessment {
        value.requireFields(setOf("source", "sourceArtifactSha256", "sourceRecordIdentity", "relation"))
        return HimSemanticEvidenceAssessment(evidenceReference(value), enumValue(value.string("relation")))
    }

    private fun conflict(value: JsonObject): HimSemanticAuthorityConflictDiagnostic {
        value.requireFields(setOf("authorityEntityReference", "conflictingEvidence", "shortRationale"))
        val evidence = value.array("conflictingEvidence").mapObjects {
            it.requireFields(setOf("source", "sourceArtifactSha256", "sourceRecordIdentity"))
            evidenceReference(it)
        }
        return HimSemanticAuthorityConflictDiagnostic(value.string("authorityEntityReference"), evidence, value.string("shortRationale"))
    }

    private fun evidenceReference(value: JsonObject) = HimEvidenceReference(
        enumValue<HimGroundTruthSource>(value.string("source")).name,
        HimSha256(value.string("sourceArtifactSha256")),
        value.string("sourceRecordIdentity"),
    )

    private fun JsonObject.requireFields(required: Set<String>, nullable: Set<String> = emptySet()) {
        require(entrySet().map { it.key }.toSet() == required)
        required.minus(nullable).forEach { require(has(it) && !get(it).isJsonNull) }
    }
    private fun JsonObject.string(name: String): String = get(name).takeIf { it != null && it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString ?: invalid("$name must be a string")
    private fun JsonObject.obj(name: String): JsonObject = get(name).takeIf { it != null && it.isJsonObject }?.asJsonObject ?: invalid("$name must be an object")
    private fun JsonObject.array(name: String): JsonArray = get(name).takeIf { it != null && it.isJsonArray }?.asJsonArray ?: invalid("$name must be an array")
    private fun <T> JsonArray.mapObjects(transform: (JsonObject) -> T): List<T> = map { it.takeIf { element -> element.isJsonObject }?.asJsonObject?.let(transform) ?: invalid("Array element must be an object") }

    private inline fun <reified T : Enum<T>> enumValue(value: String): T = enumValues<T>().firstOrNull { it.name == value } ?: invalid("Invalid ${T::class.simpleName}")
    private fun invalid(message: String): Nothing = throw HimSemanticInferenceSchemaException(message)
    private fun <T> schema(block: () -> T): T = try { block() } catch (failure: HimSemanticInferenceSchemaException) { throw failure } catch (failure: Throwable) { throw HimSemanticInferenceSchemaException(failure.message ?: "Invalid structured response") }
}
