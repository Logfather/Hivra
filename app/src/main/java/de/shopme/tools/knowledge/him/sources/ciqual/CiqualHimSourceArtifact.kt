package de.shopme.tools.knowledge.him.sources.ciqual

/**
 * Source-faithful CIQUAL projection whose relations remain connected through
 * CIQUAL source codes.
 *
 * This artifact contains no Canonical Catalog assignment, matching, candidate
 * generation, knowledge merge, or semantic or numeric normalization.
 */
data class CiqualHimSourceArtifact(
    val foods: List<CiqualFood>,
    val taxonomy: List<CiqualFoodTaxonomy>,
    val constituents: List<CiqualConstituent>,
    val sources: List<CiqualSource>
)

data class CiqualFood(
    val alimCode: String,
    val nameFr: String,
    val nameEn: String,
    val scientificName: CiqualSourceLeaf,
    val groupCode: String,
    val subgroupCode: String,
    val subSubgroupCode: String,
    val jonesFactorLexical: String,
    val compositions: List<CiqualComposition>
)

data class CiqualComposition(
    val constCode: String,
    val teneurLexical: String,
    val minimum: CiqualSourceLeaf,
    val maximum: CiqualSourceLeaf,
    val confidenceCode: CiqualSourceLeaf,
    val sourceCode: CiqualSourceLeaf
)

data class CiqualFoodTaxonomy(
    val groupCode: String,
    val groupNameFr: String,
    val groupNameEn: String,
    val subgroupCode: String,
    val subgroupNameFr: String,
    val subgroupNameEn: String,
    val subSubgroupCode: String,
    val subSubgroupNameFr: String,
    val subSubgroupNameEn: String
)

data class CiqualConstituent(
    val constCode: String,
    val nameFr: String,
    val nameEn: String,
    val infoodsCode: CiqualSourceLeaf
)

data class CiqualSource(
    val sourceCode: String,
    val citation: CiqualSourceLeaf
)

data class CiqualSourceLeaf(
    val lexicalValue: String,
    val missingAttributeValue: String?
)
