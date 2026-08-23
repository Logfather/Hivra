package de.shopme.tools.knowledge.him.canonical.family.groundtruth

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType

enum class HimGroundTruthMutationType {
    ADD_IDENTITY,
    ADD_VARIANT,
    ADD_ALIAS,
}

data class HimCanonicalFamilyMutationLedgerEntry(
    val mutationReference: HimMutationReference,
    val validationReference: HimValidationReference,
    val mutationType: HimGroundTruthMutationType,
    val entityType: HimEntityType,
    val newEntityId: HimEntityId,
    val parent: HimFamilyEntityReference,
    val canonicalFamilyAuthoritySha256Before: HimSha256,
    val canonicalFamilyAuthoritySha256After: HimSha256,
    val entityIdRegistrySha256Before: HimSha256,
    val entityIdRegistrySha256After: HimSha256,
    val contractVersion: String,
) {
    init {
        require(entityType != HimEntityType.CANONICAL)
        require(
            (mutationType == HimGroundTruthMutationType.ADD_IDENTITY &&
                    entityType == HimEntityType.IDENTITY) ||
                    (mutationType == HimGroundTruthMutationType.ADD_VARIANT &&
                            entityType == HimEntityType.VARIANT) ||
                    (mutationType == HimGroundTruthMutationType.ADD_ALIAS &&
                            entityType == HimEntityType.ALIAS)
        )
        require(contractVersion.isNotBlank())
    }
}

data class HimCanonicalFamilyMutationLedger(
    val schemaVersion: String,
    val entries: List<HimCanonicalFamilyMutationLedgerEntry>,
) {
    init {
        require(schemaVersion.isNotBlank())
    }
}

data class HimRetiredEntityIdRegistryEntry(
    val entityId: HimEntityId,
    val entityType: HimEntityType,
    val retirementMutationReference: HimMutationReference,
)

data class HimRetiredEntityIdRegistry(
    val schemaVersion: String,
    val entries: List<HimRetiredEntityIdRegistryEntry>,
) {
    init {
        require(schemaVersion.isNotBlank())
        require(entries.map { it.entityId }.distinct().size == entries.size)
    }
}

data class HimGroundTruthReleaseArtifactReference(
    val path: String,
    val sha256: HimSha256,
    val recordCount: Int,
) {
    init {
        require(path.isNotBlank())
        require(recordCount >= 0)
    }
}

data class HimGroundTruthReleaseSources(
    val canonicalFamilyAuthority: HimGroundTruthReleaseArtifactReference,
    val activeEntityIdRegistry: HimGroundTruthReleaseArtifactReference,
    val retiredEntityIdRegistry: HimGroundTruthReleaseArtifactReference,
    val mutationLedger: HimGroundTruthReleaseArtifactReference,
    val entityFingerprintIndex: HimGroundTruthReleaseArtifactReference,
)

data class HimGroundTruthReleaseCounts(
    val canonicals: Int,
    val identities: Int,
    val variants: Int,
    val aliases: Int,
    val activeEntityIds: Int,
    val retiredEntityIds: Int,
    val mutations: Int,
) {
    init {
        require(
            listOf(
                canonicals,
                identities,
                variants,
                aliases,
                activeEntityIds,
                retiredEntityIds,
                mutations,
            ).all { it >= 0 }
        )
    }
}

enum class HimGroundTruthReleaseState {
    RELEASED,
}

data class HimGroundTruthRelease(
    val releaseVersion: String,
    val sources: HimGroundTruthReleaseSources,
    val counts: HimGroundTruthReleaseCounts,
    val state: HimGroundTruthReleaseState,
) {
    init {
        require(releaseVersion.isNotBlank())
    }
}
