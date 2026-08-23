package de.shopme.tools.knowledge.him.canonical.family.groundtruth

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId

enum class HimCandidateType {
    IDENTITY,
    VARIANT,
    ALIAS,
    CREATE_NEW_CANONICAL,
}

sealed interface HimCandidateRelation {
    val candidateType: HimCandidateType

    data class Identity(
        val parentCanonicalId: HimEntityId,
    ) : HimCandidateRelation {
        override val candidateType = HimCandidateType.IDENTITY
    }

    data class Variant(
        val scope: HimFamilyEntityReference,
    ) : HimCandidateRelation {
        override val candidateType = HimCandidateType.VARIANT
    }

    data class Alias(
        val equivalentEntity: HimFamilyEntityReference,
    ) : HimCandidateRelation {
        override val candidateType = HimCandidateType.ALIAS
    }

    object CreateNewCanonical : HimCandidateRelation {
        override val candidateType = HimCandidateType.CREATE_NEW_CANONICAL
    }
}

data class HimGroundTruthCandidate(
    val candidateReference: HimCandidateReference,
    val candidateTerm: String,
    val relation: HimCandidateRelation,
    val evidenceReferences: List<HimEvidenceReference>,
    val candidateConfidence: HimCandidateConfidence,
) {
    init {
        require(candidateTerm.isNotBlank())
    }

    val candidateType: HimCandidateType
        get() = relation.candidateType
}
