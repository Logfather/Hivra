package de.shopme.tools.knowledge.him.canonical.family.groundtruth

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId

data class HimCandidateReference(val value: String) {
    init {
        require(value.isNotBlank())
    }
}

data class HimValidationReference(val value: String) {
    init {
        require(value.isNotBlank())
    }
}

data class HimMutationReference(val value: String) {
    init {
        require(value.isNotBlank())
    }
}

data class HimSha256(val value: String) {
    init {
        require(value.matches(Regex("[0-9a-f]{64}")))
    }
}

sealed interface HimFamilyEntityReference {
    val canonicalId: HimEntityId

    data class Canonical(
        override val canonicalId: HimEntityId,
    ) : HimFamilyEntityReference

    data class Identity(
        override val canonicalId: HimEntityId,
        val identityId: HimEntityId,
    ) : HimFamilyEntityReference
}

data class HimEvidenceReference(
    val source: String,
    val sourceArtifactSha256: HimSha256,
    val sourceRecordIdentity: String,
) {
    init {
        require(source.isNotBlank())
        require(sourceRecordIdentity.isNotBlank())
    }
}

enum class HimCandidateConfidence {
    HIGH,
    MEDIUM,
    LOW,
    NO_CONFIDENCE,
}

enum class HimApprovalConfidence {
    HIGH,
    MEDIUM,
    LOW,
    NO_CONFIDENCE,
}

enum class HimRetrievalTerminalState {
    SUFFICIENT_EVIDENCE,
    SEARCH_EXHAUSTED,
}

enum class HimEvidenceSufficiency {
    SUFFICIENT,
    INSUFFICIENT,
    UNKNOWN,
}
