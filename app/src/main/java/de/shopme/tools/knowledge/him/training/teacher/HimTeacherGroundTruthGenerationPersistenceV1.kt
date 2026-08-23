package de.shopme.tools.knowledge.him.training.teacher

import com.google.gson.GsonBuilder
import com.google.gson.Gson
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceProjection
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceSearchResult
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private val teacherPersistenceGson = GsonBuilder().serializeNulls().disableHtmlEscaping().create()
private val teacherFamilyGson = Gson()

/** Temporary/result-file persistence only. No production path is selected here. */
object HimTeacherGroundTruthGenerationPersistenceV1 {
    fun serializeRequest(request: HimTeacherGroundTruthGenerationRequestV1): ByteArray =
        (teacherPersistenceGson.toJson(request.toDto()) + "\n").toByteArray(Charsets.UTF_8)

    fun readRequest(file: File): HimTeacherGroundTruthGenerationRequestV1 =
        teacherPersistenceGson.fromJson(file.readText(), RequestDto::class.java).toModel()

    fun writeNewRequest(file: File, request: HimTeacherGroundTruthGenerationRequestV1) {
        require(!file.exists()) { "Immutable Teacher request already exists: ${file.path}" }
        atomicWrite(file, serializeRequest(request))
    }

    fun serializeResult(result: HimTeacherGroundTruthGenerationResultV1): ByteArray =
        (teacherPersistenceGson.toJson(result.toDto()) + "\n").toByteArray(Charsets.UTF_8)

    fun readResult(file: File): HimTeacherGroundTruthGenerationResultV1 =
        teacherPersistenceGson.fromJson(file.readText(), ResultDto::class.java).toModel()

    fun writeNewResult(file: File, result: HimTeacherGroundTruthGenerationResultV1) {
        require(!file.exists()) { "Immutable Teacher result already exists: ${file.path}" }
        atomicWrite(file, serializeResult(result))
    }

    fun addIdempotent(existing: List<HimTeacherGroundTruthGenerationResultV1>, result: HimTeacherGroundTruthGenerationResultV1): List<HimTeacherGroundTruthGenerationResultV1> {
        val collision = existing.firstOrNull { it.resultReference == result.resultReference }
        require(collision == null || serializeResult(collision).contentEquals(serializeResult(result))) {
            "Teacher semantic result identity collision"
        }
        return (existing + listOfNotNull(if (collision == null) result else null)).sortedBy { it.resultReference }
    }

    private fun atomicWrite(file: File, bytes: ByteArray) {
        val parent = requireNotNull(file.parentFile)
        require(parent.exists() || parent.mkdirs())
        val temporary = Files.createTempFile(parent.toPath(), ".${file.name}.", ".tmp")
        try {
            Files.write(temporary, bytes)
            Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}

data class HimTeacherGroundTruthOfflinePreflightResultV1(
    val ready: Boolean,
    val diagnostics: List<String>,
    val requestBytesSha256: HimSha256?,
    val resultReference: String?,
)

object HimTeacherGroundTruthOfflinePreflightV1 {
    fun run(
        plan: de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlanV1,
        request: HimTeacherGroundTruthGenerationRequestV1,
        provider: HimTeacherGroundTruthProviderV1,
        temporaryDirectory: File,
    ): HimTeacherGroundTruthOfflinePreflightResultV1 {
        val diagnostics = mutableListOf<String>()
        return runCatching {
            val requestBytes = HimTeacherGroundTruthGenerationPersistenceV1.serializeRequest(request)
            val requestFile = temporaryDirectory.resolve("teacher-request.json")
            HimTeacherGroundTruthGenerationPersistenceV1.writeNewRequest(requestFile, request)
            val reloadedRequest = HimTeacherGroundTruthGenerationPersistenceV1.readRequest(requestFile)
            require(requestBytes.contentEquals(HimTeacherGroundTruthGenerationPersistenceV1.serializeRequest(reloadedRequest)))
            require(reloadedRequest.requestReference == request.requestReference)
            diagnostics += "request-serialization-reload"

            val pipeline = HimTeacherGroundTruthGenerationPipelineV1()
            val result = pipeline.generate(plan, reloadedRequest, provider)
            val resultFile = temporaryDirectory.resolve("teacher-result.json")
            HimTeacherGroundTruthGenerationPersistenceV1.writeNewResult(resultFile, result)
            val reloadedResult = HimTeacherGroundTruthGenerationPersistenceV1.readResult(resultFile)
            require(HimTeacherGroundTruthGenerationPersistenceV1.serializeResult(result).contentEquals(HimTeacherGroundTruthGenerationPersistenceV1.serializeResult(reloadedResult)))
            require(reloadedResult.resultReference == result.resultReference)
            diagnostics += "result-persistence-reload-digest"

            val rerun = pipeline.generate(plan, request, provider)
            require(rerun.resultReference == result.resultReference)
            require(HimTeacherGroundTruthGenerationPersistenceV1.serializeResult(rerun).contentEquals(HimTeacherGroundTruthGenerationPersistenceV1.serializeResult(result)))
            require(HimTeacherGroundTruthGenerationPersistenceV1.addIdempotent(listOf(result), rerun).size == 1)
            diagnostics += "idempotency"
            HimTeacherGroundTruthOfflinePreflightResultV1(true, diagnostics, HimSha256(sha256(requestBytes)), result.resultReference)
        }.getOrElse { failure ->
            diagnostics += failure.message ?: failure::class.simpleName.orEmpty()
            HimTeacherGroundTruthOfflinePreflightResultV1(false, diagnostics, null, null)
        }
    }

    private fun sha256(bytes: ByteArray): String = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

private data class RequestDto(
    val requestReference: String,
    val workItemReference: String,
    val canonicalId: String,
    val partition: String,
    val requestedCoverage: List<String>,
    val observedTerm: String,
    val canonicalContext: List<ContextDto>,
    val inferenceRequest: InferenceRequestDto,
    val policyBindings: PolicyDto,
) {
    fun toModel() = HimTeacherGroundTruthGenerationRequestV1(
        requestReference, workItemReference, HimEntityId(canonicalId), HimTrainingPartitionV1.valueOf(partition), requestedCoverage.map(HimTrainingClassificationV1::valueOf), observedTerm,
        canonicalContext.map(ContextDto::toModel), inferenceRequest.toModel(), policyBindings.toModel(),
    )
}

private data class ContextDto(val rank: Int, val canonicalId: String, val canonicalName: String, val fullRecordCanonicalJson: String?) {
    fun toModel() = HimCandidateCanonicalContext(rank, HimEntityId(canonicalId), canonicalName, fullRecordCanonicalJson)
}

private data class EvidenceDto(val source: String, val sourceRecordReference: String, val recordKind: String, val retrievalRank: Int, val evidenceProjection: String) {
    fun toModel() = HimEvidenceSearchResult(HimGroundTruthSource.valueOf(source), HimEvidenceRecordReference.parse(HimGroundTruthSource.valueOf(source), sourceRecordReference), HimEvidenceRecordKind.valueOf(recordKind), retrievalRank, HimEvidenceProjection(evidenceProjection))
}

private data class QueryDto(val source: String, val queries: List<String>)
private data class DirectiveDto(val sourceQueries: List<QueryDto>, val informationGainJudgment: String) {
    fun toModel() = HimSemanticRetrievalDirective(sourceQueries.map { HimSemanticSourceQueries(HimGroundTruthSource.valueOf(it.source), it.queries) }, HimSemanticInformationGainJudgment.valueOf(informationGainJudgment))
}
private data class HistoryDto(val round: Int, val directive: DirectiveDto, val retrievedEvidenceReferences: List<EvidenceReferenceDto>) {
    fun toModel() = HimSemanticRetrievalHistoryEntry(HimRetrievalRound(round), directive.toModel(), retrievedEvidenceReferences.map(EvidenceReferenceDto::toModel))
}
private data class EvidenceReferenceDto(val source: String, val sourceArtifactSha256: String, val sourceRecordIdentity: String) {
    fun toModel() = HimEvidenceReference(source, HimSha256(sourceArtifactSha256), sourceRecordIdentity)
}
private data class FoundationDto(val releaseVersion: String, val releaseRecordSha256: String, val foundationDigest: String) {
    fun toModel() = HimRetrievalFoundationBinding(releaseVersion, HimSha256(releaseRecordSha256), HimSha256(foundationDigest))
}
private data class InferenceRequestDto(
    val invocationReference: String, val inputTerm: String, val canonicalContext: List<ContextDto>, val evidence: List<EvidenceDto>, val retrievalRound: Int,
    val availableSources: List<String>, val retrievalHistory: List<HistoryDto>, val f3ContractVersion: String, val retrievalFoundation: FoundationDto,
) {
    fun toModel() = HimSemanticInferenceRequest(invocationReference, inputTerm, canonicalContext.map { context ->
        context.fullRecordCanonicalJson?.let { json ->
            de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult.Full(context.rank, teacherFamilyGson.fromJson(json, HimCanonicalFamily::class.java))
        } ?: de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult.Compact(context.rank, context.canonicalId.let(::HimEntityId), context.canonicalName)
    }, evidence.map(EvidenceDto::toModel), HimRetrievalRound(retrievalRound), availableSources.map(HimGroundTruthSource::valueOf).toSet(), retrievalHistory.map(HistoryDto::toModel), f3ContractVersion, retrievalFoundation.toModel())
}
private data class PolicyDto(val scalingPolicyVersion: String, val retrievalFoundation: FoundationDto, val instructionPolicyVersion: String, val outputSchemaVersion: String, val providerConfigurationFingerprint: String, val contextBudgetPolicyVersion: String) {
    fun toModel() = HimTeacherGroundTruthPolicyBindingsV1(scalingPolicyVersion, retrievalFoundation.toModel(), instructionPolicyVersion, outputSchemaVersion, HimSha256(providerConfigurationFingerprint), contextBudgetPolicyVersion)
}

private fun HimTeacherGroundTruthGenerationRequestV1.toDto() = RequestDto(
    requestReference, workItemReference, canonicalId.value, partition.name, requestedCoverage.map { it.name }, observedTerm,
    canonicalContext.map { ContextDto(it.rank, it.canonicalId.value, it.canonicalName, it.fullRecordCanonicalJson) }, inferenceRequest.toDto(),
    PolicyDto(policyBindings.scalingPolicyVersion, FoundationDto(policyBindings.retrievalFoundation.releaseVersion, policyBindings.retrievalFoundation.releaseRecordSha256.value, policyBindings.retrievalFoundation.foundationDigest.value), policyBindings.instructionPolicyVersion, policyBindings.outputSchemaVersion, policyBindings.providerConfigurationFingerprint.value, policyBindings.contextBudgetPolicyVersion),
)
private fun HimSemanticInferenceRequest.toDto() = InferenceRequestDto(
    invocationReference, inputTerm, canonicalContext.map { result ->
        when (result) {
            is de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult.Full -> ContextDto(result.rank, result.canonicalId.value, result.canonicalName, teacherFamilyGson.toJson(result.family))
            is de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult.Compact -> ContextDto(result.rank, result.canonicalId.value, result.canonicalName, null)
        }
    }, evidence.map { EvidenceDto(it.source.name, it.sourceRecordReference.value, it.recordKind.name, it.retrievalRank, it.evidenceProjection.deterministicJson) }, retrievalRound.value,
    availableSources.toList().sortedBy { it.ordinal }.map { it.name }, retrievalHistory.map { HistoryDto(it.round.value, DirectiveDto(it.directive.sourceQueries.map { q -> QueryDto(q.source.name, q.queries) }, it.directive.informationGainJudgment.name), it.retrievedEvidenceReferences.map { r -> EvidenceReferenceDto(r.source, r.sourceArtifactSha256.value, r.sourceRecordIdentity) }) }, f3ContractVersion, FoundationDto(retrievalFoundation.releaseVersion, retrievalFoundation.releaseRecordSha256.value, retrievalFoundation.foundationDigest.value),
)

private data class RelationDto(val type: String, val canonicalId: String?, val identityId: String?, val proposedCanonicalName: String?, val scope: String?)
private data class AssessmentDto(val source: String, val sourceArtifactSha256: String, val sourceRecordIdentity: String, val relation: String)
private data class ProposalDto(val proposalReference: String, val candidateTerm: String, val relation: RelationDto, val confidence: String, val evidenceOrigin: String, val evidenceAssessments: List<AssessmentDto>, val shortRationale: String)
private data class OutputDto(val schemaVersion: String, val proposals: List<ProposalDto>, val informationGain: String)
private data class RetrievalDto(val sourcesQueried: List<String>, val evidenceReferences: List<EvidenceReferenceDto>, val retrievalRoundCount: Int)
private data class ProvenanceDto(val providerIdentifier: String, val modelIdentifier: String, val providerConfigurationFingerprint: String, val inferenceSchemaVersion: String, val instructionPolicyVersion: String, val retrievalFoundation: FoundationDto, val technicalAttemptCount: Int)
private data class UsageDto(val inputTokens: Long?, val outputTokens: Long?, val cachedInputTokens: Long?)
private data class ResultDto(val resultReference: String, val requestReference: String, val workItemReference: String, val canonicalId: String, val partition: String, val output: OutputDto, val retrievalProvenance: RetrievalDto, val inferenceProvenance: ProvenanceDto, val usage: UsageDto?, val logicalDigest: String) {
    fun toModel(): HimTeacherGroundTruthGenerationResultV1 {
        val outputModel = HimTeacherGroundTruthOutputV1(output.schemaVersion, output.proposals.map { proposal ->
            val relation = when (proposal.relation.type) {
                "EXISTING_CANONICAL" -> HimTeacherSemanticRelationV1.ExistingCanonical(HimEntityId(requireNotNull(proposal.relation.canonicalId)))
                "IDENTITY" -> HimTeacherSemanticRelationV1.Identity(HimEntityId(requireNotNull(proposal.relation.canonicalId)))
                "VARIANT" -> HimTeacherSemanticRelationV1.Variant(proposal.relation.scope())
                "ALIAS" -> HimTeacherSemanticRelationV1.Alias(proposal.relation.scope())
                "NEW_CANONICAL" -> HimTeacherSemanticRelationV1.NewCanonical(proposal.relation.proposedCanonicalName)
                else -> error("Unknown persisted Teacher relation")
            }
            HimTeacherSemanticProposalV1(proposal.proposalReference, proposal.candidateTerm, relation, HimCandidateConfidence.valueOf(proposal.confidence), HimSemanticEvidenceOrigin.valueOf(proposal.evidenceOrigin), proposal.evidenceAssessments.map { HimSemanticEvidenceAssessment(HimEvidenceReference(it.source, HimSha256(it.sourceArtifactSha256), it.sourceRecordIdentity), HimSemanticEvidenceRelation.valueOf(it.relation)) }, proposal.shortRationale)
        }, HimSemanticInformationGainJudgment.valueOf(output.informationGain))
        val provenance = HimSemanticInferenceProvenance(inferenceProvenance.providerIdentifier, inferenceProvenance.modelIdentifier, HimSha256(inferenceProvenance.providerConfigurationFingerprint), inferenceProvenance.inferenceSchemaVersion, inferenceProvenance.instructionPolicyVersion, inferenceProvenance.retrievalFoundation.toModel(), inferenceProvenance.technicalAttemptCount)
        return HimTeacherGroundTruthGenerationResultV1(resultReference, requestReference, workItemReference, HimEntityId(canonicalId), HimTrainingPartitionV1.valueOf(partition), outputModel, HimTeacherRetrievalProvenanceV1(retrievalProvenance.sourcesQueried.map(HimGroundTruthSource::valueOf), retrievalProvenance.evidenceReferences.map(EvidenceReferenceDto::toModel), retrievalProvenance.retrievalRoundCount), provenance, usage?.let { HimSemanticUsage(it.inputTokens, it.outputTokens, it.cachedInputTokens) }, HimSha256(logicalDigest))
    }
}

private fun RelationDto.scope(): HimFamilyEntityReference = when (scope) {
    "CANONICAL" -> HimFamilyEntityReference.Canonical(HimEntityId(requireNotNull(canonicalId)))
    "IDENTITY" -> HimFamilyEntityReference.Identity(HimEntityId(requireNotNull(canonicalId)), HimEntityId(requireNotNull(identityId)))
    else -> error("Unknown persisted Teacher scope")
}

private fun HimTeacherGroundTruthGenerationResultV1.toDto() = ResultDto(
    resultReference, requestReference, workItemReference, canonicalId.value, partition.name,
    OutputDto(output.schemaVersion, output.proposals.map { proposal ->
        val relation = when (val value = proposal.relation) {
            is HimTeacherSemanticRelationV1.ExistingCanonical -> RelationDto("EXISTING_CANONICAL", value.canonicalId.value, null, null, null)
            is HimTeacherSemanticRelationV1.Identity -> RelationDto("IDENTITY", value.parentCanonicalId.value, null, null, null)
            is HimTeacherSemanticRelationV1.Variant -> value.scope.toDto("VARIANT")
            is HimTeacherSemanticRelationV1.Alias -> value.equivalentEntity.toDto("ALIAS")
            is HimTeacherSemanticRelationV1.NewCanonical -> RelationDto("NEW_CANONICAL", null, null, value.proposedCanonicalName, null)
        }
        ProposalDto(proposal.proposalReference, proposal.candidateTerm, relation, proposal.confidence.name, proposal.evidenceOrigin.name, proposal.evidenceAssessments.map { AssessmentDto(it.evidenceReference.source, it.evidenceReference.sourceArtifactSha256.value, it.evidenceReference.sourceRecordIdentity, it.relation.name) }, proposal.shortRationale)
    }, output.informationGain.name),
    RetrievalDto(retrievalProvenance.sourcesQueried.map { it.name }, retrievalProvenance.evidenceReferences.map { EvidenceReferenceDto(it.source, it.sourceArtifactSha256.value, it.sourceRecordIdentity) }, retrievalProvenance.retrievalRoundCount),
    ProvenanceDto(inferenceProvenance.providerIdentifier, inferenceProvenance.modelIdentifier, inferenceProvenance.providerConfigurationFingerprint.value, inferenceProvenance.inferenceSchemaVersion, inferenceProvenance.instructionPolicyVersion, FoundationDto(inferenceProvenance.retrievalFoundation.releaseVersion, inferenceProvenance.retrievalFoundation.releaseRecordSha256.value, inferenceProvenance.retrievalFoundation.foundationDigest.value), inferenceProvenance.technicalAttemptCount),
    usage?.let { UsageDto(it.inputTokens, it.outputTokens, it.cachedInputTokens) }, logicalDigest.value,
)

private fun HimFamilyEntityReference.toDto(type: String) = when (this) {
    is HimFamilyEntityReference.Canonical -> RelationDto(type, canonicalId.value, null, null, "CANONICAL")
    is HimFamilyEntityReference.Identity -> RelationDto(type, canonicalId.value, identityId.value, null, "IDENTITY")
}
