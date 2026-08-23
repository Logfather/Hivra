package de.shopme.tools.knowledge.him.canonical.family

data class HimCanonicalFamilyAuthority(
    val schemaVersion: String,
    val sourceCatalog: HimCanonicalFamilySourceCatalog,
    val families: List<HimCanonicalFamily>,
)

data class HimCanonicalFamilySourceCatalog(
    val path: String,
    val contentSha256: String,
    val recordCount: Int,
)
