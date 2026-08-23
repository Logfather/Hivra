package de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceSearchResult

object HimSemanticInferenceSchema {
    const val OUTPUT_VERSION = "HIM_SEMANTIC_INFERENCE_OUTPUT_V2_1"
    const val INSTRUCTION_POLICY_VERSION = "HIM_SEMANTIC_INFERENCE_INSTRUCTION_POLICY_V2_2"
    const val F3_CONTRACT_VERSION = "HIM_GROUND_TRUTH_CONTRACT_V1"
}

data class HimRetrievalFoundationBinding(
    val releaseVersion: String,
    val releaseRecordSha256: HimSha256,
    val foundationDigest: HimSha256,
) {
    init {
        require(releaseVersion == RELEASE_VERSION)
        require(releaseRecordSha256 == RELEASE_SHA256)
        require(foundationDigest == FOUNDATION_DIGEST)
    }

    companion object {
        const val RELEASE_VERSION = "F3D_2_RETRIEVAL_FOUNDATION_V1"
        val RELEASE_SHA256 = HimSha256("b4814fbd8141c43b04b97bd0f87c5e01d5128b7143ff74b80ed873a8f0068023")
        val FOUNDATION_DIGEST = HimSha256("9780a1dc28ece40e1251981f007a509418de5db4c1e8921ba3402ac2dcf3fd86")
        val V1 = HimRetrievalFoundationBinding(RELEASE_VERSION, RELEASE_SHA256, FOUNDATION_DIGEST)
    }
}

data class HimSemanticSourceQueries(
    val source: HimGroundTruthSource,
    val queries: List<String>,
) {
    init {
        require(queries.isNotEmpty() && queries.size <= MAX_QUERIES_PER_SOURCE_PER_ROUND)
        require(queries.all(String::isNotBlank))
        require(queries.distinct().size == queries.size)
    }

    companion object {
        const val MAX_QUERIES_PER_SOURCE_PER_ROUND = 3
    }
}

data class HimSemanticRetrievalDirective(
    val sourceQueries: List<HimSemanticSourceQueries>,
    val informationGainJudgment: HimSemanticInformationGainJudgment,
) {
    init {
        require(informationGainJudgment == HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP)
        require(sourceQueries.isNotEmpty())
        require(sourceQueries.map { it.source }.distinct().size == sourceQueries.size)
    }
}

data class HimSemanticRetrievalHistoryEntry(
    val round: HimRetrievalRound,
    val directive: HimSemanticRetrievalDirective,
    val retrievedEvidenceReferences: List<HimEvidenceReference>,
) {
    init { require(round.value in 1..HimRetrievalRound.MAX_ROUNDS) }
}

data class HimSemanticInferenceRequest(
    val invocationReference: String,
    val inputTerm: String,
    val canonicalContext: List<HimCanonicalRetrievalResult>,
    val evidence: List<HimEvidenceSearchResult>,
    val retrievalRound: HimRetrievalRound,
    val availableSources: Set<HimGroundTruthSource> = HimGroundTruthSource.entries.toSet(),
    val retrievalHistory: List<HimSemanticRetrievalHistoryEntry> = emptyList(),
    val f3ContractVersion: String = HimSemanticInferenceSchema.F3_CONTRACT_VERSION,
    val retrievalFoundation: HimRetrievalFoundationBinding = HimRetrievalFoundationBinding.V1,
) {
    init {
        require(invocationReference.isNotBlank() && inputTerm.isNotBlank())
        require(canonicalContext.size <= 10)
        require(canonicalContext.count { it is HimCanonicalRetrievalResult.Full } <= 3)
        require(canonicalContext.map { it.rank } == (1..canonicalContext.size).toList())
        require(evidence.groupBy { it.source }.values.all { it.size <= 10 })
        require(availableSources.isNotEmpty())
        require(evidence.all { it.source in availableSources })
        require(retrievalHistory.size == retrievalRound.value)
        require(retrievalHistory.map { it.round.value } == (1..retrievalHistory.size).toList())
        require(retrievalHistory.flatMap { it.directive.sourceQueries }.all { it.source in availableSources })
        if (retrievalRound.value == 0) require(evidence.isEmpty())
        val historicalEvidence = retrievalHistory.flatMap { it.retrievedEvidenceReferences }.toSet()
        require(evidence.map(HimSemanticSourceArtifactIdentityV1::reference).all { it in historicalEvidence })
        require(f3ContractVersion.isNotBlank())
    }
}

enum class HimSemanticEvidenceOrigin { SOURCE_SUPPORTED, MODEL_DERIVED, MIXED }

enum class HimSemanticEvidenceRelation { DIRECT, RELATED, PARENT, INGREDIENT, VARIANT }

data class HimSemanticEvidenceAssessment(
    val evidenceReference: HimEvidenceReference,
    val relation: HimSemanticEvidenceRelation,
)

enum class HimSemanticInformationGainJudgment { MORE_EVIDENCE_MAY_HELP, NO_EXPECTED_INFORMATION_GAIN }

data class HimSemanticCandidateProposal(
    val proposalReference: String,
    val candidateTerm: String,
    val relation: HimCandidateRelation,
    val confidence: HimCandidateConfidence,
    val evidenceOrigin: HimSemanticEvidenceOrigin,
    val evidenceAssessments: List<HimSemanticEvidenceAssessment>,
    val shortRationale: String,
) {
    init {
        require(proposalReference.isNotBlank() && candidateTerm.isNotBlank() && shortRationale.isNotBlank())
        require(evidenceOrigin != HimSemanticEvidenceOrigin.SOURCE_SUPPORTED || evidenceAssessments.isNotEmpty())
    }
}

data class HimSemanticAuthorityConflictDiagnostic(
    val authorityEntityReference: String,
    val conflictingEvidence: List<HimEvidenceReference>,
    val shortRationale: String,
) {
    init {
        require(authorityEntityReference.isNotBlank() && conflictingEvidence.isNotEmpty() && shortRationale.isNotBlank())
    }
}

data class HimSemanticInferenceSuccess(
    val candidates: List<HimSemanticCandidateProposal>,
    val informationGain: HimSemanticInformationGainJudgment,
    val retrievalDirective: HimSemanticRetrievalDirective?,
    val authorityConflicts: List<HimSemanticAuthorityConflictDiagnostic>,
    val provenance: HimSemanticInferenceProvenance,
) {
    init {
        when (informationGain) {
            HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP -> {
                requireNotNull(retrievalDirective)
                require(retrievalDirective.informationGainJudgment == informationGain)
            }
            HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN -> require(retrievalDirective == null)
        }
    }
}

sealed interface HimSemanticInferenceResult {
    data class Success(val value: HimSemanticInferenceSuccess) : HimSemanticInferenceResult
    data class TechnicalFailure(val failure: HimSemanticInferenceFailure) : HimSemanticInferenceResult
}
