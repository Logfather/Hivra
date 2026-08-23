package de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistryEntry
import de.shopme.tools.knowledge.him.canonical.family.HimEntitySourceReferenceType
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import java.math.BigInteger
import java.security.MessageDigest

object HimDeterministicGroundTruthEntityIdAllocatorContractV1 {
    const val VERSION = "HIM_DETERMINISTIC_GROUND_TRUTH_ENTITY_ID_ALLOCATOR_V1"
    const val MAX_PROBE_ATTEMPTS = 1024
}

/**
 * Deterministically allocates child Entity IDs without changing existing IDs.
 *
 * The promotion reference is already the stable identity of the validated
 * candidate, target, scope, term, and preconditions. Entity type is added as
 * the allocation domain, then deterministic probing resolves namespace
 * collisions.
 */
class HimDeterministicGroundTruthEntityIdAllocatorV1 {

    fun allocate(
        promotionReference: HimCandidatePromotionReference,
        entityType: HimEntityType,
        registry: HimEntityIdRegistry,
        authority: HimCanonicalFamilyAuthority,
    ): HimEntityId {
        require(entityType in SUPPORTED_TYPES) {
            "Entity-ID allocation is only supported for child entity types."
        }

        val registryById = validateRegistry(registry)
        val authorityById = validateAuthority(authority)
        validateOverlappingTypes(registryById, authorityById)

        for (probe in 0 until HimDeterministicGroundTruthEntityIdAllocatorContractV1.MAX_PROBE_ATTEMPTS) {
            val candidate = candidateId(promotionReference, entityType, probe)
            val registryEntry = registryById[candidate]
            val authorityType = authorityById[candidate]

            if (registryEntry != null && authorityType != null) {
                require(registryEntry.entityType == authorityType) {
                    "Entity-ID Registry and Authority disagree for ${candidate.value}."
                }
            }

            if (
                registryEntry != null &&
                registryEntry.entityType == entityType &&
                registryEntry.sourceReferenceType ==
                HimEntitySourceReferenceType.GROUND_TRUTH_PROMOTION &&
                registryEntry.sourceReference == promotionReference.value &&
                authorityType == entityType
            ) {
                return candidate
            }

            if (registryEntry == null && authorityType == null) {
                return candidate
            }
        }

        error(
            "Deterministic Ground-Truth Entity-ID allocation exhausted " +
                    "${HimDeterministicGroundTruthEntityIdAllocatorContractV1.MAX_PROBE_ATTEMPTS} probes.",
        )
    }

    private fun candidateId(
        promotionReference: HimCandidatePromotionReference,
        entityType: HimEntityType,
        probe: Int,
    ): HimEntityId {
        val identity = buildString {
            appendLine("contract=${HimDeterministicGroundTruthEntityIdAllocatorContractV1.VERSION}")
            appendLine("promotion=${promotionReference.value}")
            appendLine("entity-type=${entityType.name}")
            appendLine("probe=$probe")
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8))
        val unsigned = BigInteger(1, digest)
        val bucket = unsigned.mod(BigInteger.valueOf(TYPE_BUCKET_COUNT)).toLong()
        val value = bucket * TYPE_COUNT + typeOffset(entityType)
        return HimEntityId(toBase62(value))
    }

    private fun validateRegistry(
        registry: HimEntityIdRegistry,
    ): Map<HimEntityId, HimEntityIdRegistryEntry> {
        val byId = registry.entries.associateBy { it.entityId }
        require(byId.size == registry.entries.size) {
            "Duplicate Entity IDs in active Entity-ID Registry."
        }
        return byId
    }

    private fun validateAuthority(
        authority: HimCanonicalFamilyAuthority,
    ): Map<HimEntityId, HimEntityType> {
        val entities = buildList {
            authority.families.forEach { family ->
                add(family.canonicalId to HimEntityType.CANONICAL)
                family.identities.forEach { identity ->
                    add(identity.identityId to HimEntityType.IDENTITY)
                    identity.variants.forEach { add(it.variantId to HimEntityType.VARIANT) }
                    identity.aliases.forEach { add(it.aliasId to HimEntityType.ALIAS) }
                }
                family.variants.forEach { add(it.variantId to HimEntityType.VARIANT) }
                family.aliases.forEach { add(it.aliasId to HimEntityType.ALIAS) }
            }
        }
        val byId = entities.toMap()
        require(byId.size == entities.size) {
            "Duplicate Entity IDs in Canonical Family Authority."
        }
        return byId
    }

    private fun validateOverlappingTypes(
        registry: Map<HimEntityId, HimEntityIdRegistryEntry>,
        authority: Map<HimEntityId, HimEntityType>,
    ) {
        registry.keys.intersect(authority.keys).forEach { entityId ->
            require(registry.getValue(entityId).entityType == authority.getValue(entityId)) {
                "Entity-ID Registry and Authority disagree for ${entityId.value}."
            }
        }
    }

    private fun typeOffset(entityType: HimEntityType): Long = when (entityType) {
        HimEntityType.IDENTITY -> 0L
        HimEntityType.VARIANT -> 1L
        HimEntityType.ALIAS -> 2L
        HimEntityType.CANONICAL -> error("Canonical Entity-ID allocation is outside child promotion scope.")
    }

    private fun toBase62(value: Long): String {
        var remaining = value
        val result = CharArray(ID_LENGTH)
        for (index in ID_LENGTH - 1 downTo 0) {
            result[index] = ALPHABET[(remaining % ALPHABET.length).toInt()]
            remaining /= ALPHABET.length
        }
        require(remaining == 0L)
        return String(result)
    }

    private companion object {
        val SUPPORTED_TYPES = setOf(
            HimEntityType.IDENTITY,
            HimEntityType.VARIANT,
            HimEntityType.ALIAS,
        )
        const val ID_LENGTH = 6
        const val TYPE_COUNT = 3L
        const val TYPE_BUCKET_COUNT = 18_933_411_861L
        const val ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    }
}
