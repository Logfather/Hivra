package de.shopme.tools.knowledge.him.canonical.family.groundtruth

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintGenerator
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndex
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus

data class HimGroundTruthEntityFingerprintIndexValidationResultV1(
    val entries: Int,
    val canonicalScopes: Int,
    val identityScopes: Int,
    val variantReferences: Int,
    val uniqueFingerprints: Int,
    val duplicateFingerprintGroups: Int,
    val invalidFingerprints: Int,
    val duplicateStructuralScopes: Int,
    val registryAuthorityMismatches: Int,
    val missingExpectedScopes: Int,
    val unexpectedScopes: Int,
    val fingerprintRecomputationMismatches: Int,
)

class HimGroundTruthEntityFingerprintIndexValidatorV1(
    private val generator: HimEntityFingerprintGenerator =
        HimEntityFingerprintGenerator(),
) {

    fun validate(
        registry: HimEntityIdRegistry,
        authority: HimCanonicalFamilyAuthority,
        index: HimEntityFingerprintIndex,
        registrySha256: String,
        authoritySha256: String,
    ): HimGroundTruthEntityFingerprintIndexValidationResultV1 {

        require(registrySha256.isNotBlank()) {
            "Entity-ID Registry SHA-256 must not be blank."
        }

        require(authoritySha256.isNotBlank()) {
            "Canonical Family Authority SHA-256 must not be blank."
        }

        require(
            index.schemaVersion ==
                    HimGroundTruthEntityFingerprintIndexContractV1.SCHEMA_VERSION
        ) {
            "Unsupported Ground-Truth fingerprint index schema: ${index.schemaVersion}"
        }

        require(
            index.sourceAuthorities.entityIdRegistrySha256 ==
                    registrySha256
        ) {
            "Ground-Truth fingerprint index Registry binding mismatch."
        }

        require(
            index.sourceAuthorities.canonicalFamilyAuthoritySha256 ==
                    authoritySha256
        ) {
            "Ground-Truth fingerprint index Authority binding mismatch."
        }

        require(
            index.entryCount ==
                    index.entries.size
        ) {
            "Ground-Truth fingerprint index entryCount mismatch."
        }

        val registryById =
            registry.entries.associateBy {
                it.entityId
            }

        require(
            registryById.size ==
                    registry.entries.size
        ) {
            "Duplicate Entity IDs in active Entity-ID Registry."
        }

        val authorityEntities =
            authorityEntities(authority)

        require(
            authorityEntities
                .map { it.first }
                .distinct()
                .size ==
                    authorityEntities.size
        ) {
            "Duplicate Entity IDs in Canonical Family Authority."
        }

        val registryAuthorityMismatches =
            authorityEntities.count { (entityId, entityType) ->
                registryById[entityId]?.entityType != entityType
            } +
                    registry.entries.count { registryEntry ->
                        authorityEntities.none { (entityId, entityType) ->
                            entityId == registryEntry.entityId &&
                                    entityType == registryEntry.entityType
                        }
                    }

        val expectedScopes =
            expectedScopes(authority)

        val actualScopes =
            index.entries.map {
                structuralScope(
                    canonicalId = it.canonicalId,
                    identityId = it.identityId,
                    variantIds = it.variantIds,
                )
            }

        val expectedScopeSet =
            expectedScopes.toSet()

        val actualScopeSet =
            actualScopes.toSet()

        val fingerprintCounts =
            index.entries
                .groupingBy { it.fingerprint }
                .eachCount()

        val result =
            HimGroundTruthEntityFingerprintIndexValidationResultV1(
                entries =
                    index.entries.size,

                canonicalScopes =
                    index.entries.count {
                        it.identityId == null
                    },

                identityScopes =
                    index.entries.count {
                        it.identityId != null
                    },

                variantReferences =
                    index.entries.sumOf {
                        it.variantIds.size
                    },

                uniqueFingerprints =
                    fingerprintCounts.size,

                duplicateFingerprintGroups =
                    fingerprintCounts.count {
                        it.value > 1
                    },

                invalidFingerprints =
                    index.entries.count {
                        !it.fingerprint.matches(
                            FINGERPRINT_PATTERN
                        )
                    },

                duplicateStructuralScopes =
                    actualScopes.size -
                            actualScopeSet.size,

                registryAuthorityMismatches =
                    registryAuthorityMismatches,

                missingExpectedScopes =
                    (expectedScopeSet -
                            actualScopeSet).size,

                unexpectedScopes =
                    (actualScopeSet -
                            expectedScopeSet).size,

                fingerprintRecomputationMismatches =
                    index.entries.count { entry ->
                        entry.fingerprint !=
                                generator.generate(
                                    canonicalId =
                                        entry.canonicalId,
                                    identityId =
                                        entry.identityId,
                                    variantIds =
                                        entry.variantIds,
                                )
                    },
            )

        require(
            result.entries ==
                    expectedScopes.size
        ) {
            "Ground-Truth fingerprint index entry count does not match expected active scopes."
        }

        require(
            result.uniqueFingerprints ==
                    result.entries
        ) {
            "Ground-Truth fingerprint index contains duplicate fingerprints."
        }

        require(
            result.duplicateFingerprintGroups == 0
        ) {
            "Ground-Truth fingerprint index contains duplicate fingerprint groups."
        }

        require(
            result.invalidFingerprints == 0
        ) {
            "Ground-Truth fingerprint index contains invalid SHA-256 fingerprints."
        }

        require(
            result.duplicateStructuralScopes == 0
        ) {
            "Ground-Truth fingerprint index contains duplicate structural scopes."
        }

        require(
            result.registryAuthorityMismatches == 0
        ) {
            "Ground-Truth Authority and active Entity-ID Registry are inconsistent."
        }

        require(
            result.missingExpectedScopes == 0
        ) {
            "Ground-Truth fingerprint index is missing expected structural scopes."
        }

        require(
            result.unexpectedScopes == 0
        ) {
            "Ground-Truth fingerprint index contains unexpected structural scopes."
        }

        require(
            result.fingerprintRecomputationMismatches == 0
        ) {
            "Ground-Truth fingerprint index contains fingerprint recomputation mismatches."
        }

        require(
            actualScopes ==
                    expectedScopes
        ) {
            "Ground-Truth fingerprint index ordering does not match deterministic Authority scope order."
        }

        return result
    }

    private fun authorityEntities(
        authority: HimCanonicalFamilyAuthority,
    ): List<Pair<HimEntityId, HimEntityType>> =
        buildList {
            authority.families.forEach { family ->
                add(
                    family.canonicalId to
                            HimEntityType.CANONICAL
                )

                family.identities.forEach { identity ->
                    add(
                        identity.identityId to
                                HimEntityType.IDENTITY
                    )

                    identity.variants.forEach { variant ->
                        add(
                            variant.variantId to
                                    HimEntityType.VARIANT
                        )
                    }

                    identity.aliases.forEach { alias ->
                        add(
                            alias.aliasId to
                                    HimEntityType.ALIAS
                        )
                    }
                }

                family.variants.forEach { variant ->
                    add(
                        variant.variantId to
                                HimEntityType.VARIANT
                    )
                }

                family.aliases.forEach { alias ->
                    add(
                        alias.aliasId to
                                HimEntityType.ALIAS
                    )
                }
            }
        }

    private fun expectedScopes(
        authority: HimCanonicalFamilyAuthority,
    ): List<String> =
        authority.families
            .sortedBy {
                it.canonicalId.value
            }
            .flatMap { family ->
                val canonicalScope =
                    structuralScope(
                        canonicalId =
                            family.canonicalId,
                        identityId =
                            null,
                        variantIds =
                            family.variants
                                .filter {
                                    it.lifecycleStatus ==
                                            HimLifecycleStatus.ACTIVE
                                }
                                .map {
                                    it.variantId
                                },
                    )

                val identityScopes =
                    family.identities
                        .filter {
                            it.lifecycleStatus ==
                                    HimLifecycleStatus.ACTIVE
                        }
                        .sortedBy {
                            it.identityId.value
                        }
                        .map { identity ->
                            structuralScope(
                                canonicalId =
                                    family.canonicalId,
                                identityId =
                                    identity.identityId,
                                variantIds =
                                    identity.variants
                                        .filter {
                                            it.lifecycleStatus ==
                                                    HimLifecycleStatus.ACTIVE
                                        }
                                        .map {
                                            it.variantId
                                        },
                            )
                        }

                listOf(canonicalScope) +
                        identityScopes
            }

    private fun structuralScope(
        canonicalId: HimEntityId,
        identityId: HimEntityId?,
        variantIds: List<HimEntityId>,
    ): String =
        buildString {
            append(canonicalId.value)
            append('|')
            append(
                identityId
                    ?.value
                    .orEmpty()
            )
            append('|')
            append(
                variantIds
                    .map {
                        it.value
                    }
                    .distinct()
                    .sorted()
                    .joinToString(",")
            )
        }

    private companion object {
        val FINGERPRINT_PATTERN =
            Regex("[0-9a-f]{64}")
    }
}