package de.shopme.tools.knowledge.him.training.teacher

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceProvenance
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceRuntime
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticCandidateProposal
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceOrigin
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceSuccess
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceSchema
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticUsage
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlanV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlanValidatorV1

sealed interface HimTeacherGroundTruthProviderOutcomeV1 {
    data class StructuredResponse(
        val json: String,
        val inferenceProvenance: HimSemanticInferenceProvenance,
        val usage: HimSemanticUsage? = null,
    ) : HimTeacherGroundTruthProviderOutcomeV1

    data class TechnicalFailure(val safeMessage: String) : HimTeacherGroundTruthProviderOutcomeV1 {
        init { require(safeMessage.isNotBlank()) }
    }
}

fun interface HimTeacherGroundTruthProviderV1 {
    fun invoke(request: HimTeacherGroundTruthGenerationRequestV1): HimTeacherGroundTruthProviderOutcomeV1
}

/**
 * Adapter over the existing F3.7 semantic runtime. It deliberately does not
 * create an OpenAI client and therefore remains usable with an offline fake
 * provider in tests.
 */
class HimSemanticInferenceTeacherProviderAdapterV1(
    private val runtime: HimSemanticInferenceRuntime,
) : HimTeacherGroundTruthProviderV1 {
    override fun invoke(request: HimTeacherGroundTruthGenerationRequestV1): HimTeacherGroundTruthProviderOutcomeV1 =
        when (val result = runtime.infer(request.inferenceRequest)) {
            is HimSemanticInferenceResult.TechnicalFailure -> HimTeacherGroundTruthProviderOutcomeV1.TechnicalFailure(result.failure.safeMessage)
            is HimSemanticInferenceResult.Success -> {
                val output = result.value.toTeacherOutput()
                HimTeacherGroundTruthProviderOutcomeV1.StructuredResponse(
                    String(HimTeacherGroundTruthOutputCodecV1.serialize(output), Charsets.UTF_8),
                    result.value.provenance,
                )
            }
        }

    private fun HimSemanticInferenceSuccess.toTeacherOutput(): HimTeacherGroundTruthOutputV1 {
        val proposals = candidates.map { candidate ->
            HimTeacherSemanticProposalV1(
                proposalReference = candidate.proposalReference,
                candidateTerm = candidate.candidateTerm,
                relation = candidate.relation.toTeacherRelation(candidate.candidateTerm),
                confidence = candidate.confidence,
                evidenceOrigin = candidate.evidenceOrigin,
                evidenceAssessments = candidate.evidenceAssessments,
                shortRationale = candidate.shortRationale,
            )
        }
        return HimTeacherGroundTruthOutputV1(
            HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION,
            proposals,
            informationGain,
        )
    }

    private fun de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation.toTeacherRelation(term: String) = when (this) {
        is de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation.Identity -> HimTeacherSemanticRelationV1.Identity(parentCanonicalId)
        is de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation.Variant -> HimTeacherSemanticRelationV1.Variant(scope)
        is de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation.Alias -> HimTeacherSemanticRelationV1.Alias(equivalentEntity)
        de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation.CreateNewCanonical -> HimTeacherSemanticRelationV1.NewCanonical(term)
    }
}

enum class HimTeacherGroundTruthExecutionModeV1 { OFFLINE_FIXTURE, PAID_OPT_IN }

/** The explicit paid boundary required by F3.8f. */
class HimOptInTeacherGroundTruthProviderV1(
    private val delegate: HimTeacherGroundTruthProviderV1,
    private val paidOptIn: () -> Boolean = { System.getenv(HimTeacherGroundTruthGenerationContractV1.PAID_OPT_IN_ENVIRONMENT_FLAG) == "true" },
    private val apiKeyPresent: () -> Boolean = { !System.getenv("OPENAI_API_KEY").isNullOrBlank() },
) : HimTeacherGroundTruthProviderV1 {
    override fun invoke(request: HimTeacherGroundTruthGenerationRequestV1): HimTeacherGroundTruthProviderOutcomeV1 {
        check(paidOptIn()) { "Paid Teacher execution requires ${HimTeacherGroundTruthGenerationContractV1.PAID_OPT_IN_ENVIRONMENT_FLAG}=true" }
        check(apiKeyPresent()) { "Paid Teacher execution requires OPENAI_API_KEY" }
        return delegate.invoke(request)
    }
}

object HimTeacherGroundTruthRequestValidatorV1 {
    fun validateAgainstPlan(
        plan: HimCanonicalGroundTruthScalingPlanV1,
        request: HimTeacherGroundTruthGenerationRequestV1,
    ) {
        require(HimCanonicalGroundTruthScalingPlanValidatorV1.validate(plan).valid) { "F3.8e scaling plan is invalid" }
        val workItem = plan.workItems.singleOrNull { it.reference == request.workItemReference }
            ?: error("Teacher request is not bound to an F3.8e work item")
        require(request.canonicalId == workItem.canonicalId)
        require(request.partition == workItem.partition)
        require(request.requestedCoverage == workItem.missingCoverage)
        require(request.requestReference == HimTeacherGroundTruthRequestIdentityV1.reference(request))
        require(request.policyBindings.scalingPolicyVersion == plan.policyVersion)
        require(request.canonicalContext.any { it.canonicalId == workItem.canonicalId })
    }
}

object HimTeacherGroundTruthOutputValidatorV1 {
    fun validate(
        request: HimTeacherGroundTruthGenerationRequestV1,
        output: HimTeacherGroundTruthOutputV1,
    ) {
        require(output.schemaVersion == request.policyBindings.outputSchemaVersion)
        val allowedCanonicalIds = request.canonicalContext.map { it.canonicalId }.toSet()
        val seen = mutableSetOf<String>()
        output.proposals.forEach { proposal ->
            require(proposal.candidateTerm.isNotBlank())
            require(seen.add(semanticKey(proposal))) { "Duplicate semantic Teacher hypothesis" }
            require(proposal.evidenceAssessments.map { it.evidenceReference }.all { evidence ->
                request.inferenceRequest.evidence.any { evidence == de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticSourceArtifactIdentityV1.reference(it) }
            }) { "Teacher output cites evidence not present in the request" }
            when (val relation = proposal.relation) {
                is HimTeacherSemanticRelationV1.ExistingCanonical -> require(relation.canonicalId in allowedCanonicalIds) { "EXISTING_CANONICAL is outside request context" }
                is HimTeacherSemanticRelationV1.Identity -> require(relation.parentCanonicalId in allowedCanonicalIds) { "IDENTITY parent is outside request context" }
                is HimTeacherSemanticRelationV1.Variant -> validateScope(relation.scope, request, allowedCanonicalIds)
                is HimTeacherSemanticRelationV1.Alias -> validateScope(relation.equivalentEntity, request, allowedCanonicalIds)
                is HimTeacherSemanticRelationV1.NewCanonical -> require(relation.proposedCanonicalName == null || relation.proposedCanonicalName == proposal.candidateTerm)
            }
        }
    }

    private fun validateScope(scope: HimFamilyEntityReference, request: HimTeacherGroundTruthGenerationRequestV1, allowedCanonicalIds: Set<HimEntityId>) {
        require(scope.canonicalId == request.canonicalId) { "Teacher child scope is outside the work-item family" }
        require(scope.canonicalId in allowedCanonicalIds)
    }

    private fun semanticKey(proposal: HimTeacherSemanticProposalV1): String = buildString {
        append(proposal.candidateTerm.trim().lowercase()).append('|').append(proposal.relation)
    }
}

class HimTeacherGroundTruthGenerationPipelineV1 {
    fun generate(
        plan: HimCanonicalGroundTruthScalingPlanV1,
        request: HimTeacherGroundTruthGenerationRequestV1,
        provider: HimTeacherGroundTruthProviderV1,
        mode: HimTeacherGroundTruthExecutionModeV1 = HimTeacherGroundTruthExecutionModeV1.OFFLINE_FIXTURE,
    ): HimTeacherGroundTruthGenerationResultV1 {
        HimTeacherGroundTruthRequestValidatorV1.validateAgainstPlan(plan, request)
        if (mode == HimTeacherGroundTruthExecutionModeV1.PAID_OPT_IN && provider !is HimOptInTeacherGroundTruthProviderV1) {
            error("Paid Teacher mode requires HimOptInTeacherGroundTruthProviderV1")
        }
        val outcome = provider.invoke(request)
        val structured = outcome as? HimTeacherGroundTruthProviderOutcomeV1.StructuredResponse
            ?: error((outcome as HimTeacherGroundTruthProviderOutcomeV1.TechnicalFailure).safeMessage)
        require(structured.inferenceProvenance.providerConfigurationFingerprint == request.policyBindings.providerConfigurationFingerprint) {
            "Teacher provider configuration fingerprint does not match the request"
        }
        require(structured.inferenceProvenance.instructionPolicyVersion == request.policyBindings.instructionPolicyVersion) {
            "Teacher instruction policy does not match the request"
        }
        require(structured.inferenceProvenance.retrievalFoundation == request.policyBindings.retrievalFoundation) {
            "Teacher retrieval foundation does not match the request"
        }
        val output = HimTeacherGroundTruthOutputDecoderV1.decode(structured.json)
        HimTeacherGroundTruthOutputValidatorV1.validate(request, output)
        val retrievalProvenance = HimTeacherRetrievalProvenanceV1(
            sourcesQueried = request.inferenceRequest.availableSources.toList().sortedBy { it.ordinal },
            evidenceReferences = request.inferenceRequest.evidence.map { evidence ->
                de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticSourceArtifactIdentityV1.reference(evidence)
            },
            retrievalRoundCount = request.inferenceRequest.retrievalRound.value,
        )
        val draft = HimTeacherGroundTruthGenerationResultV1(
            resultReference = "teacher-result:v1:${"0".repeat(64)}",
            requestReference = request.requestReference,
            workItemReference = request.workItemReference,
            canonicalId = request.canonicalId,
            partition = request.partition,
            output = output,
            retrievalProvenance = retrievalProvenance,
            inferenceProvenance = structured.inferenceProvenance,
            usage = structured.usage,
            logicalDigest = HimSha256("0".repeat(64)),
        )
        return draft.copy(
            resultReference = HimTeacherGroundTruthResultIdentityV1.reference(draft),
            logicalDigest = HimTeacherGroundTruthResultIdentityV1.digest(draft),
        )
    }
}
