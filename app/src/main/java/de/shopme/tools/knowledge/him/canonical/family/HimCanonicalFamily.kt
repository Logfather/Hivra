package de.shopme.tools.knowledge.him.canonical.family

data class HimCanonicalFamily(
    val canonicalId: HimEntityId,
    val canonicalName: String,
    val normalizedName: String,
    val taxonomyPaths: List<List<String>>,
    val lifecycleStatus: HimLifecycleStatus,
    val identities: List<HimCanonicalIdentity>,
    val variants: List<HimCanonicalVariant>,
    val aliases: List<HimCanonicalAlias>,
)

data class HimCanonicalIdentity(
    val identityId: HimEntityId,
    val identityName: String,
    val normalizedName: String,
    val lifecycleStatus: HimLifecycleStatus,
    val variants: List<HimCanonicalVariant>,
    val aliases: List<HimCanonicalAlias>,
)

data class HimCanonicalVariant(
    val variantId: HimEntityId,
    val variantName: String,
    val normalizedName: String,
    val lifecycleStatus: HimLifecycleStatus,
)

data class HimCanonicalAlias(
    val aliasId: HimEntityId,
    val aliasName: String,
    val normalizedName: String,
    val lifecycleStatus: HimLifecycleStatus,
)
