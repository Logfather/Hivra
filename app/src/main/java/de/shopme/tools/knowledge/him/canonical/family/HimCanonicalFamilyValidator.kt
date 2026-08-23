package de.shopme.tools.knowledge.him.canonical.family

data class HimCanonicalFamilyValidationResult(
    val registryEntries: Int,
    val canonicalEntries: Int,
    val identityEntries: Int,
    val variantEntries: Int,
    val aliasEntries: Int,
    val uniqueIds: Int,
    val invalidIds: Int,
    val collisions: Int,
    val families: Int,
    val activeFamilies: Int,
    val deprecatedFamilies: Int,
    val identities: Int,
    val variants: Int,
    val aliases: Int,
    val canonicalNameMismatches: Int,
    val normalizedNameMismatches: Int,
    val taxonomyPathsMismatches: Int,
    val registryCanonicalIdsWithoutFamily: Int,
    val familyIdsWithoutRegistryEntry: Int,
    val duplicateFamilyIds: Int,
)

class HimCanonicalFamilyValidator {

    fun validate(
        source: HimProductOnlyCanonicalMaster,
        registry: HimEntityIdRegistry,
        authority: HimCanonicalFamilyAuthority,
    ): HimCanonicalFamilyValidationResult {
        val entries = registry.entries
        val ids = entries.map { it.entityId }
        val uniqueIds = ids.toSet()
        val invalidIds = ids.count { !it.value.matches(ID_PATTERN) }
        val collisions = ids.size - uniqueIds.size

        val canonicalEntries = entries.filter { it.entityType == HimEntityType.CANONICAL }
        val canonicalByReference =
            canonicalEntries.associateBy { entry ->
                require(entry.sourceReferenceType ==
                        HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED)
                entry.sourceReference
            }

        require(entries.size == HimProductOnlyCanonicalMasterReader.EXPECTED_RECORD_COUNT)
        require(invalidIds == 0)
        require(collisions == 0)
        require(canonicalEntries.size == entries.size)
        require(canonicalByReference.size == canonicalEntries.size)
        require(canonicalByReference.keys == source.records.map { it.normalized }.toSet())

        require(authority.schemaVersion == AUTHORITY_SCHEMA_VERSION)
        require(authority.sourceCatalog.path == source.path)
        require(authority.sourceCatalog.contentSha256 == source.contentSha256)
        require(authority.sourceCatalog.recordCount == source.records.size)
        require(authority.families.size == source.records.size)

        val familyIds = authority.families.map { it.canonicalId }
        val familyIdSet = familyIds.toSet()
        val canonicalIdSet = canonicalEntries.map { it.entityId }.toSet()
        val duplicateFamilyIds = familyIds.size - familyIdSet.size

        val canonicalNameMismatches =
            authority.families.zip(source.records).count { (family, record) ->
                family.canonicalName != record.itemname
            }
        val normalizedNameMismatches =
            authority.families.zip(source.records).count { (family, record) ->
                family.normalizedName != record.normalized
            }
        val taxonomyPathsMismatches =
            authority.families.zip(source.records).count { (family, record) ->
                family.taxonomyPaths != record.taxonomyPaths
            }

        val result =
            HimCanonicalFamilyValidationResult(
                registryEntries = entries.size,
                canonicalEntries = canonicalEntries.size,
                identityEntries = entries.count { it.entityType == HimEntityType.IDENTITY },
                variantEntries = entries.count { it.entityType == HimEntityType.VARIANT },
                aliasEntries = entries.count { it.entityType == HimEntityType.ALIAS },
                uniqueIds = uniqueIds.size,
                invalidIds = invalidIds,
                collisions = collisions,
                families = authority.families.size,
                activeFamilies = authority.families.count {
                    it.lifecycleStatus == HimLifecycleStatus.ACTIVE
                },
                deprecatedFamilies = authority.families.count {
                    it.lifecycleStatus == HimLifecycleStatus.DEPRECATED
                },
                identities = authority.families.sumOf { it.identities.size },
                variants = authority.families.sumOf { family ->
                    family.variants.size + family.identities.sumOf { it.variants.size }
                },
                aliases = authority.families.sumOf { family ->
                    family.aliases.size + family.identities.sumOf { it.aliases.size }
                },
                canonicalNameMismatches = canonicalNameMismatches,
                normalizedNameMismatches = normalizedNameMismatches,
                taxonomyPathsMismatches = taxonomyPathsMismatches,
                registryCanonicalIdsWithoutFamily = (canonicalIdSet - familyIdSet).size,
                familyIdsWithoutRegistryEntry = (familyIdSet - canonicalIdSet).size,
                duplicateFamilyIds = duplicateFamilyIds,
            )

        require(result.identityEntries == 0)
        require(result.variantEntries == 0)
        require(result.aliasEntries == 0)
        require(result.activeFamilies == source.records.size)
        require(result.deprecatedFamilies == 0)
        require(result.identities == 0)
        require(result.variants == 0)
        require(result.aliases == 0)
        require(result.canonicalNameMismatches == 0)
        require(result.normalizedNameMismatches == 0)
        require(result.taxonomyPathsMismatches == 0)
        require(result.registryCanonicalIdsWithoutFamily == 0)
        require(result.familyIdsWithoutRegistryEntry == 0)
        require(result.duplicateFamilyIds == 0)

        authority.families.zip(source.records).forEach { (family, record) ->
            require(family.canonicalId == canonicalByReference.getValue(record.normalized).entityId)
        }

        return result
    }

    companion object {
        const val AUTHORITY_SCHEMA_VERSION = "1"
        private val ID_PATTERN = Regex("[0-9A-Za-z]{6}")
    }
}
