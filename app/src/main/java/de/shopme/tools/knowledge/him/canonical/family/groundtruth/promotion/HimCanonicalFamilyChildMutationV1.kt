package de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion

import de.shopme.tools.knowledge.him.canonical.family.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.*
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

object HimCanonicalFamilyChildMutationContractV1 {
    const val VERSION = "HIM_CANONICAL_FAMILY_CHILD_MUTATION_V1"
    const val MUTATION_REFERENCE_CONTRACT = "HIM_CANONICAL_FAMILY_CHILD_MUTATION_REFERENCE_V1"
}

data class HimCanonicalFamilyChildMutationResultV1(
    val authorityAfter: HimCanonicalFamilyAuthority,
    val registryAfter: HimEntityIdRegistry,
    val mutationLedgerEntry: HimCanonicalFamilyMutationLedgerEntry,
)

class HimCanonicalFamilyChildMutationV1(
    private val persistence: HimCanonicalFamilyPersistence = HimCanonicalFamilyPersistence(),
) {
    fun apply(
        authorityBefore: HimCanonicalFamilyAuthority,
        registryBefore: HimEntityIdRegistry,
        promotion: HimCandidatePromotionProposalV1,
        validationReference: HimValidationReference,
        newEntityId: HimEntityId,
        authoritySha256Before: HimSha256,
        registrySha256Before: HimSha256,
    ): HimCanonicalFamilyChildMutationResultV1 {
        require(sha256(persistence.serialize(authorityBefore)) == authoritySha256Before) { "authoritySha256Before does not match the in-memory Authority." }
        require(sha256(persistence.serialize(registryBefore)) == registrySha256Before) { "registrySha256Before does not match the in-memory Registry." }
        require(promotion.authorityDigestBefore == authoritySha256Before) { "Promotion authority-before digest mismatch." }
        require(promotion.entityIdRegistryDigestBefore == registrySha256Before) { "Promotion registry-before digest mismatch." }
        require(promotion.validationReference.value == validationReference.value) { "Promotion validation reference mismatch." }
        require(registryBefore.entries.none { it.entityId == newEntityId }) { "newEntityId is already assigned in the active registry." }
        require(newEntityId !in authorityEntityIds(authorityBefore)) { "newEntityId already exists in Canonical Family Authority." }

        val mutation = when (val target = promotion.target) {
            is HimCandidatePromotionTargetV1.AddIdentity -> addIdentity(authorityBefore, target, newEntityId)
            is HimCandidatePromotionTargetV1.AddVariant -> addVariant(authorityBefore, target, newEntityId)
            is HimCandidatePromotionTargetV1.AddAlias -> addAlias(authorityBefore, target, newEntityId)
            is HimCandidatePromotionTargetV1.CreateCanonical -> error("CREATE_CANONICAL is outside F3.7d.1 child mutation scope.")
        }
        require(mutation.authority.families.size == authorityBefore.families.size)
        require(mutation.authority.sourceCatalog == authorityBefore.sourceCatalog)
        require(mutation.authority.schemaVersion == authorityBefore.schemaVersion)

        val registryAfter = registryBefore.copy(
            entries = registryBefore.entries + HimEntityIdRegistryEntry(
                entityId = newEntityId,
                entityType = mutation.entityType,
                sourceReferenceType = HimEntitySourceReferenceType.GROUND_TRUTH_PROMOTION,
                sourceReference = promotion.promotionReference.value,
            ),
        )
        require(registryAfter.entries.dropLast(1) == registryBefore.entries)

        val authorityAfterSha = sha256(persistence.serialize(mutation.authority))
        val registryAfterSha = sha256(persistence.serialize(registryAfter))
        val mutationReference = mutationReference(
            promotion, validationReference, mutation.mutationType, newEntityId, mutation.parent,
            authoritySha256Before, authorityAfterSha, registrySha256Before, registryAfterSha,
        )
        val ledger = HimCanonicalFamilyMutationLedgerEntry(
            mutationReference = mutationReference,
            validationReference = validationReference,
            mutationType = mutation.mutationType,
            entityType = mutation.entityType,
            newEntityId = newEntityId,
            parent = mutation.parent,
            canonicalFamilyAuthoritySha256Before = authoritySha256Before,
            canonicalFamilyAuthoritySha256After = authorityAfterSha,
            entityIdRegistrySha256Before = registrySha256Before,
            entityIdRegistrySha256After = registryAfterSha,
            contractVersion = HimCanonicalFamilyChildMutationContractV1.VERSION,
        )
        return HimCanonicalFamilyChildMutationResultV1(mutation.authority, registryAfter, ledger)
    }

    private fun addIdentity(
        authority: HimCanonicalFamilyAuthority,
        target: HimCandidatePromotionTargetV1.AddIdentity,
        id: HimEntityId,
    ): Mutation {
        val name = requireName(target.identityName)
        val normalized = normalize(name)
        val familyIndex = authority.families.indexOfFirst { it.canonicalId == target.parentCanonicalId }
        require(familyIndex >= 0) { "ADD_IDENTITY parent canonical does not exist." }
        val family = authority.families[familyIndex]
        require(family.identities.none { it.normalizedName == normalized }) { "Equivalent identity already exists in target scope." }
        val child = HimCanonicalIdentity(id, name, normalized, HimLifecycleStatus.ACTIVE, emptyList(), emptyList())
        return Mutation(
            replaceFamily(authority, familyIndex, family.copy(identities = family.identities + child)),
            HimGroundTruthMutationType.ADD_IDENTITY,
            HimEntityType.IDENTITY,
            HimFamilyEntityReference.Canonical(family.canonicalId),
        )
    }

    private fun addVariant(
        authority: HimCanonicalFamilyAuthority,
        target: HimCandidatePromotionTargetV1.AddVariant,
        id: HimEntityId,
    ): Mutation {
        val name = requireName(target.variantName)
        val normalized = normalize(name)
        val location = locate(authority, target.scope)
        val child = HimCanonicalVariant(id, name, normalized, HimLifecycleStatus.ACTIVE)
        val family = location.family
        val updated = when (target.scope) {
            is HimFamilyEntityReference.Canonical -> {
                require(family.variants.none { it.normalizedName == normalized }) { "Equivalent variant already exists in target scope." }
                family.copy(variants = family.variants + child)
            }
            is HimFamilyEntityReference.Identity -> {
                val identity = family.identities[location.identityIndex]
                require(identity.variants.none { it.normalizedName == normalized }) { "Equivalent variant already exists in target scope." }
                family.copy(identities = replace(family.identities, location.identityIndex, identity.copy(variants = identity.variants + child)))
            }
        }
        return Mutation(replaceFamily(authority, location.familyIndex, updated), HimGroundTruthMutationType.ADD_VARIANT, HimEntityType.VARIANT, target.scope)
    }

    private fun addAlias(
        authority: HimCanonicalFamilyAuthority,
        target: HimCandidatePromotionTargetV1.AddAlias,
        id: HimEntityId,
    ): Mutation {
        val name = requireName(target.aliasName)
        val normalized = normalize(name)
        val location = locate(authority, target.equivalentEntity)
        val child = HimCanonicalAlias(id, name, normalized, HimLifecycleStatus.ACTIVE)
        val family = location.family
        val updated = when (target.equivalentEntity) {
            is HimFamilyEntityReference.Canonical -> {
                require(family.aliases.none { it.normalizedName == normalized }) { "Equivalent alias already exists in target scope." }
                family.copy(aliases = family.aliases + child)
            }
            is HimFamilyEntityReference.Identity -> {
                val identity = family.identities[location.identityIndex]
                require(identity.aliases.none { it.normalizedName == normalized }) { "Equivalent alias already exists in target scope." }
                family.copy(identities = replace(family.identities, location.identityIndex, identity.copy(aliases = identity.aliases + child)))
            }
        }
        return Mutation(replaceFamily(authority, location.familyIndex, updated), HimGroundTruthMutationType.ADD_ALIAS, HimEntityType.ALIAS, target.equivalentEntity)
    }

    private fun locate(authority: HimCanonicalFamilyAuthority, reference: HimFamilyEntityReference): Location {
        val familyIndex = authority.families.indexOfFirst { it.canonicalId == reference.canonicalId }
        require(familyIndex >= 0) { "Mutation canonical scope does not exist." }
        val family = authority.families[familyIndex]
        val identityIndex = when (reference) {
            is HimFamilyEntityReference.Canonical -> -1
            is HimFamilyEntityReference.Identity -> family.identities.indexOfFirst { it.identityId == reference.identityId }.also {
                require(it >= 0) { "Mutation identity scope does not exist." }
            }
        }
        return Location(familyIndex, identityIndex, family)
    }

    private fun mutationReference(
        promotion: HimCandidatePromotionProposalV1,
        validation: HimValidationReference,
        type: HimGroundTruthMutationType,
        entityId: HimEntityId,
        parent: HimFamilyEntityReference,
        authorityBefore: HimSha256,
        authorityAfter: HimSha256,
        registryBefore: HimSha256,
        registryAfter: HimSha256,
    ): HimMutationReference {
        val canonical = buildString {
            appendLine("contract=${HimCanonicalFamilyChildMutationContractV1.MUTATION_REFERENCE_CONTRACT}")
            appendLine("promotion=${promotion.promotionReference.value}")
            appendLine("validation=${validation.value}")
            appendLine("mutation-type=${type.name}")
            appendLine("new-entity-id=${entityId.value}")
            appendLine("parent-canonical-id=${parent.canonicalId.value}")
            appendLine("parent-identity-id=${(parent as? HimFamilyEntityReference.Identity)?.identityId?.value.orEmpty()}")
            appendLine("authority-before=${authorityBefore.value}")
            appendLine("authority-after=${authorityAfter.value}")
            appendLine("registry-before=${registryBefore.value}")
            appendLine("registry-after=${registryAfter.value}")
        }
        return HimMutationReference("mutation:v1:${sha256(canonical.toByteArray()).value}")
    }

    private fun authorityEntityIds(authority: HimCanonicalFamilyAuthority): Set<HimEntityId> = buildSet {
        authority.families.forEach { family ->
            add(family.canonicalId)
            family.identities.forEach { identity ->
                add(identity.identityId)
                addAll(identity.variants.map { it.variantId })
                addAll(identity.aliases.map { it.aliasId })
            }
            addAll(family.variants.map { it.variantId })
            addAll(family.aliases.map { it.aliasId })
        }
    }

    private fun replaceFamily(authority: HimCanonicalFamilyAuthority, index: Int, family: HimCanonicalFamily) =
        authority.copy(families = replace(authority.families, index, family))
    private fun <T> replace(values: List<T>, index: Int, value: T) = values.mapIndexed { current, existing -> if (current == index) value else existing }
    private fun requireName(value: String) = value.trim().also { require(it.isNotEmpty()) { "Child name must not be empty." } }
    private fun normalize(value: String) = Normalizer.normalize(value, Normalizer.Form.NFC).trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
    private fun sha256(bytes: ByteArray) = HimSha256(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) })

    private data class Mutation(val authority: HimCanonicalFamilyAuthority, val mutationType: HimGroundTruthMutationType, val entityType: HimEntityType, val parent: HimFamilyEntityReference)
    private data class Location(val familyIndex: Int, val identityIndex: Int, val family: HimCanonicalFamily)
}
