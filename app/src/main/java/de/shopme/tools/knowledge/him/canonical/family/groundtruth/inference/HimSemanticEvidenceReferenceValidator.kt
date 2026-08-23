package de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceSearchResult

object HimSemanticSourceArtifactIdentityV1 {
    private val hashes = mapOf(
        HimGroundTruthSource.OPEN_FOOD_FACTS to HimSha256("63b20183229a6f3f648c3d189c265d5d5a4b332d530d084af5640081d060a236"),
        HimGroundTruthSource.AGRIBALYSE to HimSha256("9068c89fa887ef087e87dcc51f758dd29e9623b93d9bd0a2277a4faae57c2297"),
        HimGroundTruthSource.CIQUAL to HimSha256("807d16c222f0e812831c10bdd89f1a5ebbc74c95e8a4944723db486add96dcff"),
        HimGroundTruthSource.GLYCEMIC_INDEX to HimSha256("6891c2ff2ab3734a1d2768339c1f660f7093a31b97a856c82bf9b8c3c88422b5"),
    )

    fun reference(record: HimEvidenceSearchResult) = HimEvidenceReference(
        record.source.name,
        requireNotNull(hashes[record.source]),
        record.sourceRecordReference.value,
    )
}

object HimSemanticEvidenceReferenceValidator {
    fun validate(request: HimSemanticInferenceRequest, success: HimSemanticInferenceSuccess) =
        validate(request.evidence.map(HimSemanticSourceArtifactIdentityV1::reference).toSet(), success)

    fun validate(allowedEvidence: Set<HimEvidenceReference>, success: HimSemanticInferenceSuccess) {
        success.candidates.forEach { candidate ->
            val references = candidate.evidenceAssessments.map { it.evidenceReference }
            require(references.all { it in allowedEvidence }) { "Candidate cites Evidence not supplied to semantic inference" }
            when (candidate.evidenceOrigin) {
                HimSemanticEvidenceOrigin.SOURCE_SUPPORTED -> require(references.isNotEmpty())
                HimSemanticEvidenceOrigin.MODEL_DERIVED -> require(references.isEmpty())
                HimSemanticEvidenceOrigin.MIXED -> require(references.isNotEmpty())
            }
        }
        success.authorityConflicts.forEach { conflict ->
            require(conflict.conflictingEvidence.all { it in allowedEvidence }) {
                "Authority diagnostic cites Evidence not supplied to semantic inference"
            }
        }
    }
}
