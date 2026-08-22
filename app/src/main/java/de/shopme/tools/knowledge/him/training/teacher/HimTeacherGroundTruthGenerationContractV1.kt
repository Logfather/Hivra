package de.shopme.tools.knowledge.him.training.teacher

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimRetrievalFoundationBinding
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceAssessment
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceOrigin
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceProvenance
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInformationGainJudgment
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceRequest
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceSchema
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticRetrievalDirective
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticRetrievalHistoryEntry
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticSourceQueries
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceProjection
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceSearchResult
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingContractV1
import de.shopme.tools.knowledge.him.training.scaling.HimTeacherGroundTruthWorkItemV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import java.security.MessageDigest

object HimTeacherGroundTruthGenerationContractV1 {
    const val VERSION = "HIM_TEACHER_GROUND_TRUTH_GENERATION_V1"
    const val REQUEST_SCHEMA_VERSION = "HIM_TEACHER_GROUND_TRUTH_REQUEST_V1"
    const val OUTPUT_SCHEMA_VERSION = "HIM_TEACHER_GROUND_TRUTH_OUTPUT_V1"
    const val RESULT_SCHEMA_VERSION = "HIM_TEACHER_GROUND_TRUTH_RESULT_V1"
    const val REQUEST_REFERENCE_CONTRACT = "HIM_TEACHER_GROUND_TRUTH_REQUEST_REFERENCE_V1"
    const val RESULT_REFERENCE_CONTRACT = "HIM_TEACHER_GROUND_TRUTH_RESULT_REFERENCE_V1"
    const val PAID_OPT_IN_ENVIRONMENT_FLAG = "HIM_OPENAI_TEACHER_GROUND_TRUTH_GENERATION"
}

enum class HimTeacherSemanticClassificationV1 { EXISTING_CANONICAL, IDENTITY, VARIANT, ALIAS, NEW_CANONICAL }

sealed interface HimTeacherSemanticRelationV1 {
    data class ExistingCanonical(val canonicalId: HimEntityId) : HimTeacherSemanticRelationV1
    data class Identity(val parentCanonicalId: HimEntityId) : HimTeacherSemanticRelationV1
    data class Variant(val scope: HimFamilyEntityReference) : HimTeacherSemanticRelationV1
    data class Alias(val equivalentEntity: HimFamilyEntityReference) : HimTeacherSemanticRelationV1
    data class NewCanonical(val proposedCanonicalName: String?) : HimTeacherSemanticRelationV1 {
        init { require(proposedCanonicalName == null || proposedCanonicalName.isNotBlank()) }
    }

    fun classification(): HimTeacherSemanticClassificationV1 = when (this) {
        is ExistingCanonical -> HimTeacherSemanticClassificationV1.EXISTING_CANONICAL
        is Identity -> HimTeacherSemanticClassificationV1.IDENTITY
        is Variant -> HimTeacherSemanticClassificationV1.VARIANT
        is Alias -> HimTeacherSemanticClassificationV1.ALIAS
        is NewCanonical -> HimTeacherSemanticClassificationV1.NEW_CANONICAL
    }
}

data class HimTeacherSemanticProposalV1(
    val proposalReference: String,
    val candidateTerm: String,
    val relation: HimTeacherSemanticRelationV1,
    val confidence: HimCandidateConfidence,
    val evidenceOrigin: HimSemanticEvidenceOrigin,
    val evidenceAssessments: List<HimSemanticEvidenceAssessment>,
    val shortRationale: String,
) {
    init {
        require(proposalReference.isNotBlank() && candidateTerm.isNotBlank() && shortRationale.isNotBlank())
        require(evidenceAssessments.map { it.evidenceReference }.distinct().size == evidenceAssessments.size)
        when (evidenceOrigin) {
            HimSemanticEvidenceOrigin.MODEL_DERIVED -> require(evidenceAssessments.isEmpty())
            HimSemanticEvidenceOrigin.SOURCE_SUPPORTED,
            HimSemanticEvidenceOrigin.MIXED -> require(evidenceAssessments.isNotEmpty())
        }
    }
}

/**
 * Information gain describes whether further evidence retrieval is expected to
 * add useful information. It is independent of the already available semantic
 * proposals: NO_EXPECTED_INFORMATION_GAIN does not require an empty proposal
 * list, and valid existing proposals must be preserved.
 */
data class HimTeacherGroundTruthOutputV1(
    val schemaVersion: String,
    val proposals: List<HimTeacherSemanticProposalV1>,
    val informationGain: HimSemanticInformationGainJudgment,
) {
    init {
        require(schemaVersion == HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION)
        require(proposals.map { it.proposalReference }.distinct().size == proposals.size)
    }
}

data class HimTeacherGroundTruthPolicyBindingsV1(
    val scalingPolicyVersion: String = HimCanonicalGroundTruthScalingContractV1.POLICY_VERSION,
    val retrievalFoundation: HimRetrievalFoundationBinding = HimRetrievalFoundationBinding.V1,
    val instructionPolicyVersion: String = HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
    val outputSchemaVersion: String = HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION,
    val providerConfigurationFingerprint: HimSha256,
    val contextBudgetPolicyVersion: String,
) {
    init {
        require(scalingPolicyVersion == HimCanonicalGroundTruthScalingContractV1.POLICY_VERSION)
        require(instructionPolicyVersion == HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION)
        require(outputSchemaVersion == HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION)
        require(contextBudgetPolicyVersion.isNotBlank())
    }
}

data class HimTeacherGroundTruthGenerationRequestV1(
    val requestReference: String,
    val workItemReference: String,
    val canonicalId: HimEntityId,
    val partition: HimTrainingPartitionV1,
    val requestedCoverage: List<HimTrainingClassificationV1>,
    val observedTerm: String,
    val canonicalContext: List<HimCandidateCanonicalContext>,
    val inferenceRequest: HimSemanticInferenceRequest,
    val policyBindings: HimTeacherGroundTruthPolicyBindingsV1,
) {
    init {
        require(requestReference.matches(Regex("teacher-request:v1:[0-9a-f]{64}")))
        require(workItemReference.matches(Regex("teacher-work:v1:[0-9a-f]{64}")))
        require(requestedCoverage.isNotEmpty() && requestedCoverage == requestedCoverage.distinct().sortedBy { it.name })
        require(requestedCoverage.all { it in setOf(HimTrainingClassificationV1.IDENTITY, HimTrainingClassificationV1.VARIANT, HimTrainingClassificationV1.ALIAS) })
        require(observedTerm.isNotBlank())
        require(canonicalContext.size == inferenceRequest.canonicalContext.size)
        require(canonicalContext.zip(inferenceRequest.canonicalContext).all { (context, result) ->
            context.rank == result.rank && context.canonicalId == result.canonicalId && context.canonicalName == result.canonicalName
        })
        require(observedTerm == inferenceRequest.inputTerm)
        require(canonicalContext.any { it.canonicalId == canonicalId })
        require(inferenceRequest.f3ContractVersion.isNotBlank())
        require(requestReference == HimTeacherGroundTruthRequestIdentityV1.reference(this) || requestReference == ZERO_REFERENCE)
    }

    companion object {
        private const val ZERO_REFERENCE = "teacher-request:v1:0000000000000000000000000000000000000000000000000000000000000000"
        fun create(
            workItem: HimTeacherGroundTruthWorkItemV1,
            observedTerm: String,
            canonicalContext: List<HimCandidateCanonicalContext>,
            inferenceRequest: HimSemanticInferenceRequest,
            policyBindings: HimTeacherGroundTruthPolicyBindingsV1,
        ): HimTeacherGroundTruthGenerationRequestV1 {
            val withoutReference = HimTeacherGroundTruthGenerationRequestV1(
                requestReference = "teacher-request:v1:${"0".repeat(64)}",
                workItemReference = workItem.reference,
                canonicalId = workItem.canonicalId,
                partition = workItem.partition,
                requestedCoverage = workItem.missingCoverage,
                observedTerm = observedTerm,
                canonicalContext = canonicalContext,
                inferenceRequest = inferenceRequest,
                policyBindings = policyBindings,
            )
            return withoutReference.copy(requestReference = HimTeacherGroundTruthRequestIdentityV1.reference(withoutReference))
        }
    }
}

object HimTeacherGroundTruthRequestIdentityV1 {
    fun reference(request: HimTeacherGroundTruthGenerationRequestV1): String =
        "teacher-request:v1:${sha256(canonical(request))}"

    fun canonical(request: HimTeacherGroundTruthGenerationRequestV1): String = buildString {
        appendLine("contract=${HimTeacherGroundTruthGenerationContractV1.REQUEST_REFERENCE_CONTRACT}")
        appendLine("schema=${HimTeacherGroundTruthGenerationContractV1.REQUEST_SCHEMA_VERSION}")
        appendLine("work-item=${request.workItemReference}")
        appendLine("canonical=${request.canonicalId.value}")
        appendLine("partition=${request.partition.name}")
        appendLine("coverage=${request.requestedCoverage.joinToString(",") { it.name }}")
        appendLine("observed=${request.observedTerm}")
        request.canonicalContext.forEach { context ->
            appendLine("context=${context.rank}|${context.canonicalId.value}|${context.canonicalName}|${context.fullRecordCanonicalJson.orEmpty()}")
        }
        appendLine("inference=${inferenceCanonical(request.inferenceRequest)}")
        appendLine("scaling=${request.policyBindings.scalingPolicyVersion}")
        appendLine("retrieval-release=${request.policyBindings.retrievalFoundation.releaseVersion}")
        appendLine("retrieval-release-sha256=${request.policyBindings.retrievalFoundation.releaseRecordSha256.value}")
        appendLine("retrieval-foundation=${request.policyBindings.retrievalFoundation.foundationDigest.value}")
        appendLine("instruction=${request.policyBindings.instructionPolicyVersion}")
        appendLine("output-schema=${request.policyBindings.outputSchemaVersion}")
        appendLine("provider=${request.policyBindings.providerConfigurationFingerprint.value}")
        appendLine("context-budget=${request.policyBindings.contextBudgetPolicyVersion}")
    }

    internal fun inferenceCanonical(request: HimSemanticInferenceRequest): String = buildString {
        append("${request.invocationReference}|${request.inputTerm}|${request.retrievalRound.value}|${request.f3ContractVersion}|")
        append(request.availableSources.sortedBy { it.ordinal }.joinToString(",") { it.name })
        request.canonicalContext.forEach {
            val full = when (it) {
                is de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult.Full -> com.google.gson.Gson().toJson(it.family)
                is de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult.Compact -> ""
            }
            append("|c:${it.rank}:${it.canonicalId.value}:${it.canonicalName}:$full")
        }
        request.evidence.sortedWith(compareBy({ it.source.ordinal }, { it.retrievalRank }, { it.sourceRecordReference.value })).forEach { evidence ->
            append("|e:${evidence.source.name}:${evidence.retrievalRank}:${evidence.sourceRecordReference.value}:${evidence.recordKind.name}:${evidence.evidenceProjection.deterministicJson}")
        }
        request.retrievalHistory.forEach { history ->
            append("|h:${history.round.value}:${history.retrievedEvidenceReferences.joinToString(",") { evidenceKey(it) }}:${directiveCanonical(history.directive)}")
        }
    }

    internal fun evidenceKey(reference: HimEvidenceReference) = "${reference.source}|${reference.sourceArtifactSha256.value}|${reference.sourceRecordIdentity}"

    internal fun directiveCanonical(directive: HimSemanticRetrievalDirective) = directive.sourceQueries
        .sortedBy { it.source.ordinal }
        .joinToString(";") { "${it.source.name}:${it.queries.joinToString(",")}" } + ":${directive.informationGainJudgment.name}"

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

data class HimTeacherRetrievalProvenanceV1(
    val sourcesQueried: List<HimGroundTruthSource>,
    val evidenceReferences: List<HimEvidenceReference>,
    val retrievalRoundCount: Int,
) {
    init {
        require(sourcesQueried == sourcesQueried.distinct().sortedBy { it.ordinal })
        require(evidenceReferences == evidenceReferences.distinct())
        require(retrievalRoundCount in 0..HimRetrievalRound.MAX_ROUNDS)
    }
}

data class HimTeacherGroundTruthGenerationResultV1(
    val resultReference: String,
    val requestReference: String,
    val workItemReference: String,
    val canonicalId: HimEntityId,
    val partition: HimTrainingPartitionV1,
    val output: HimTeacherGroundTruthOutputV1,
    val retrievalProvenance: HimTeacherRetrievalProvenanceV1,
    val inferenceProvenance: HimSemanticInferenceProvenance,
    val usage: de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticUsage? = null,
    val logicalDigest: HimSha256,
) {
    init {
        require(resultReference.matches(Regex("teacher-result:v1:[0-9a-f]{64}")))
        require(requestReference.matches(Regex("teacher-request:v1:[0-9a-f]{64}")))
        require(logicalDigest == HimTeacherGroundTruthResultIdentityV1.digest(this) || logicalDigest.value == ZERO_DIGEST)
    }

    companion object {
        private const val ZERO_DIGEST = "0000000000000000000000000000000000000000000000000000000000000000"
    }
}

object HimTeacherGroundTruthResultIdentityV1 {
    fun reference(result: HimTeacherGroundTruthGenerationResultV1): String =
        "teacher-result:v1:${sha256(canonical(result))}"

    fun digest(result: HimTeacherGroundTruthGenerationResultV1): HimSha256 = HimSha256(sha256(canonical(result)))

    fun canonical(result: HimTeacherGroundTruthGenerationResultV1): String = buildString {
        appendLine("contract=${HimTeacherGroundTruthGenerationContractV1.RESULT_REFERENCE_CONTRACT}")
        appendLine("schema=${HimTeacherGroundTruthGenerationContractV1.RESULT_SCHEMA_VERSION}")
        appendLine("request=${result.requestReference}")
        appendLine("work-item=${result.workItemReference}")
        appendLine("canonical=${result.canonicalId.value}")
        appendLine("partition=${result.partition.name}")
        appendLine("information-gain=${result.output.informationGain.name}")
        result.output.proposals.sortedBy { it.proposalReference }.forEach { proposal ->
            appendLine("proposal=${proposal.proposalReference}|${proposal.candidateTerm}|${proposal.relation.canonicalKey()}|${proposal.confidence.name}|${proposal.evidenceOrigin.name}|${proposal.evidenceAssessments.joinToString(",") { HimTeacherGroundTruthRequestIdentityV1.evidenceKey(it.evidenceReference) }}|${proposal.shortRationale}")
        }
        appendLine("retrieval-sources=${result.retrievalProvenance.sourcesQueried.joinToString(",") { it.name }}")
        appendLine("retrieval-evidence=${result.retrievalProvenance.evidenceReferences.joinToString(",") { HimTeacherGroundTruthRequestIdentityV1.evidenceKey(it) }}")
        appendLine("retrieval-rounds=${result.retrievalProvenance.retrievalRoundCount}")
        appendLine("provider=${result.inferenceProvenance.providerIdentifier}|${result.inferenceProvenance.modelIdentifier}|${result.inferenceProvenance.providerConfigurationFingerprint.value}")
        appendLine("inference-schema=${result.inferenceProvenance.inferenceSchemaVersion}|${result.inferenceProvenance.instructionPolicyVersion}")
        appendLine("retrieval-foundation=${result.inferenceProvenance.retrievalFoundation.foundationDigest.value}")
    }

    private fun HimTeacherSemanticRelationV1.canonicalKey(): String = when (this) {
        is HimTeacherSemanticRelationV1.ExistingCanonical -> "existing:${canonicalId.value}"
        is HimTeacherSemanticRelationV1.Identity -> "identity:${parentCanonicalId.value}"
        is HimTeacherSemanticRelationV1.Variant -> "variant:${scope.key()}"
        is HimTeacherSemanticRelationV1.Alias -> "alias:${equivalentEntity.key()}"
        is HimTeacherSemanticRelationV1.NewCanonical -> "new:${proposedCanonicalName.orEmpty()}"
    }

    private fun HimFamilyEntityReference.key() = when (this) {
        is HimFamilyEntityReference.Canonical -> "CANONICAL:${canonicalId.value}"
        is HimFamilyEntityReference.Identity -> "IDENTITY:${canonicalId.value}:${identityId.value}"
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

object HimTeacherGroundTruthOutputDecoderV1 {
    fun decode(json: String): HimTeacherGroundTruthOutputV1 {
        val root = JsonParser.parseString(json).takeIf { it.isJsonObject }?.asJsonObject
            ?: error("Teacher output root must be an object")
        requireExactFields(root, setOf("schemaVersion", "proposals", "informationGain"))
        require(root.string("schemaVersion") == HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION)
        val proposals = root.array("proposals").map { element ->
            val value = element.takeIf { it.isJsonObject }?.asJsonObject ?: error("Teacher proposal must be an object")
            requireExactFields(value, setOf("proposalReference", "candidateTerm", "classification", "relation", "confidence", "evidenceOrigin", "evidenceAssessments", "shortRationale"))
            val classification = enumValue<HimTeacherSemanticClassificationV1>(value.string("classification"))
            val relationObject = value.obj("relation")
            val relation = when (classification) {
                HimTeacherSemanticClassificationV1.EXISTING_CANONICAL -> { requireExactFields(relationObject, setOf("canonicalId")); HimTeacherSemanticRelationV1.ExistingCanonical(HimEntityId(relationObject.string("canonicalId"))) }
                HimTeacherSemanticClassificationV1.IDENTITY -> { requireExactFields(relationObject, setOf("parentCanonicalId")); HimTeacherSemanticRelationV1.Identity(HimEntityId(relationObject.string("parentCanonicalId"))) }
                HimTeacherSemanticClassificationV1.VARIANT -> HimTeacherSemanticRelationV1.Variant(scope(relationObject))
                HimTeacherSemanticClassificationV1.ALIAS -> HimTeacherSemanticRelationV1.Alias(scope(relationObject))
                HimTeacherSemanticClassificationV1.NEW_CANONICAL -> { requireExactFields(relationObject, emptySet()); HimTeacherSemanticRelationV1.NewCanonical(null) }
            }
            val assessments = value.array("evidenceAssessments").map { entry ->
                val assessment = entry.takeIf { it.isJsonObject }?.asJsonObject ?: error("Evidence assessment must be an object")
                requireExactFields(assessment, setOf("source", "sourceArtifactSha256", "sourceRecordIdentity", "relation"))
                HimSemanticEvidenceAssessment(
                    HimEvidenceReference(assessment.string("source"), HimSha256(assessment.string("sourceArtifactSha256")), assessment.string("sourceRecordIdentity")),
                    enumValue(assessment.string("relation")),
                )
            }
            HimTeacherSemanticProposalV1(
                value.string("proposalReference"), value.string("candidateTerm"), relation,
                enumValue(value.string("confidence")), enumValue(value.string("evidenceOrigin")), assessments,
                value.string("shortRationale"),
            )
        }
        return HimTeacherGroundTruthOutputV1(
            HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION,
            proposals,
            enumValue(root.string("informationGain")),
        )
    }

    private fun scope(value: JsonObject): HimFamilyEntityReference {
        requireExactFields(value, setOf("scope", "canonicalId", "identityId"))
        return when (value.string("scope")) {
            "CANONICAL" -> { require(value.get("identityId").isJsonNull); HimFamilyEntityReference.Canonical(HimEntityId(value.string("canonicalId"))) }
            "IDENTITY" -> { require(!value.get("identityId").isJsonNull); HimFamilyEntityReference.Identity(HimEntityId(value.string("canonicalId")), HimEntityId(value.string("identityId"))) }
            else -> error("Unsupported Teacher scope")
        }
    }

    private fun requireExactFields(value: JsonObject, expected: Set<String>) {
        require(value.entrySet().map { it.key }.toSet() == expected) { "Teacher structured output fields do not match the contract" }
    }
    private fun JsonObject.string(name: String) = get(name).takeIf { it != null && it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
        ?: error("$name must be a string")
    private fun JsonObject.obj(name: String) = get(name).takeIf { it != null && it.isJsonObject }?.asJsonObject ?: error("$name must be an object")
    private fun JsonObject.array(name: String): JsonArray = get(name).takeIf { it != null && it.isJsonArray }?.asJsonArray ?: error("$name must be an array")
    private inline fun <reified T : Enum<T>> enumValue(value: String): T = enumValues<T>().firstOrNull { it.name == value } ?: error("Invalid ${T::class.simpleName}")
}

object HimTeacherGroundTruthOutputCodecV1 {
    fun serialize(output: HimTeacherGroundTruthOutputV1): ByteArray = JsonObject().apply {
        addProperty("schemaVersion", output.schemaVersion)
        add("proposals", JsonArray().apply { output.proposals.forEach { add(proposal(it)) } })
        addProperty("informationGain", output.informationGain.name)
    }.toString().let { (it + "\n").toByteArray(Charsets.UTF_8) }

    private fun proposal(value: HimTeacherSemanticProposalV1) = JsonObject().apply {
        addProperty("proposalReference", value.proposalReference)
        addProperty("candidateTerm", value.candidateTerm)
        addProperty("classification", value.relation.classification().name)
        add("relation", relation(value.relation))
        addProperty("confidence", value.confidence.name)
        addProperty("evidenceOrigin", value.evidenceOrigin.name)
        add("evidenceAssessments", JsonArray().apply { value.evidenceAssessments.forEach { assessment -> add(JsonObject().apply {
            addProperty("source", assessment.evidenceReference.source)
            addProperty("sourceArtifactSha256", assessment.evidenceReference.sourceArtifactSha256.value)
            addProperty("sourceRecordIdentity", assessment.evidenceReference.sourceRecordIdentity)
            addProperty("relation", assessment.relation.name)
        }) } })
        addProperty("shortRationale", value.shortRationale)
    }

    private fun relation(value: HimTeacherSemanticRelationV1) = JsonObject().apply {
        when (value) {
            is HimTeacherSemanticRelationV1.ExistingCanonical -> addProperty("canonicalId", value.canonicalId.value)
            is HimTeacherSemanticRelationV1.Identity -> addProperty("parentCanonicalId", value.parentCanonicalId.value)
            is HimTeacherSemanticRelationV1.Variant -> scope(value.scope)
            is HimTeacherSemanticRelationV1.Alias -> scope(value.equivalentEntity)
            is HimTeacherSemanticRelationV1.NewCanonical -> Unit
        }
    }

    private fun JsonObject.scope(value: HimFamilyEntityReference) {
        when (value) {
            is HimFamilyEntityReference.Canonical -> { addProperty("scope", "CANONICAL"); addProperty("canonicalId", value.canonicalId.value); add("identityId", com.google.gson.JsonNull.INSTANCE) }
            is HimFamilyEntityReference.Identity -> { addProperty("scope", "IDENTITY"); addProperty("canonicalId", value.canonicalId.value); addProperty("identityId", value.identityId.value) }
        }
    }
}
