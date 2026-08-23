package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource

data class HimEvidenceRecordReference private constructor(
    val source: HimGroundTruthSource,
    val value: String,
) {
    init {
        require(value.isNotBlank())
    }

    companion object {
        fun offProduct(rowOrdinal: Long, sourceCode: String): HimEvidenceRecordReference {
            require(rowOrdinal > 0)
            requireComponent(sourceCode)
            return HimEvidenceRecordReference(
                HimGroundTruthSource.OPEN_FOOD_FACTS,
                "off:product:row:$rowOrdinal:code:$sourceCode",
            )
        }

        fun agribalyse(rowOrdinal: Long, agbCode: String): HimEvidenceRecordReference {
            require(rowOrdinal > 0)
            requireComponent(agbCode)
            return HimEvidenceRecordReference(
                HimGroundTruthSource.AGRIBALYSE,
                "agribalyse:row:$rowOrdinal:agb:$agbCode",
            )
        }

        fun ciqualFood(alimCode: String) = ciqual("food", alimCode)

        fun ciqualTaxonomy(group: String, subgroup: String, subSubgroup: String): HimEvidenceRecordReference {
            listOf(group, subgroup, subSubgroup).forEach(::requireComponent)
            return HimEvidenceRecordReference(
                HimGroundTruthSource.CIQUAL,
                "ciqual:taxonomy:$group/$subgroup/$subSubgroup",
            )
        }

        fun ciqualConstituent(constCode: String) = ciqual("constituent", constCode)

        fun ciqualSource(sourceCode: String) = ciqual("source", sourceCode)

        fun gi(recordKind: String, arrayOrdinal: Long): HimEvidenceRecordReference {
            require(recordKind in setOf("measurement", "mean-summary", "category-note", "footnote"))
            require(arrayOrdinal > 0)
            return HimEvidenceRecordReference(
                HimGroundTruthSource.GLYCEMIC_INDEX,
                "gi:$recordKind:$arrayOrdinal",
            )
        }

        fun parse(source: HimGroundTruthSource, value: String): HimEvidenceRecordReference {
            val valid = when (source) {
                HimGroundTruthSource.OPEN_FOOD_FACTS -> OFF_PATTERN.matches(value)
                HimGroundTruthSource.AGRIBALYSE -> AGRIBALYSE_PATTERN.matches(value)
                HimGroundTruthSource.CIQUAL -> CIQUAL_PATTERN.matches(value)
                HimGroundTruthSource.GLYCEMIC_INDEX -> GI_PATTERN.matches(value)
            }
            require(valid) { "Invalid ${source.name} Evidence record reference: $value" }
            return HimEvidenceRecordReference(source, value)
        }

        private fun ciqual(kind: String, identifier: String): HimEvidenceRecordReference {
            requireComponent(identifier)
            return HimEvidenceRecordReference(HimGroundTruthSource.CIQUAL, "ciqual:$kind:$identifier")
        }

        private fun requireComponent(value: String) {
            require(value.isNotBlank())
            require(':' !in value && '/' !in value && '\n' !in value)
        }

        private val OFF_PATTERN = Regex("off:product:row:[1-9][0-9]*:code:[^:/\\n]+")
        private val AGRIBALYSE_PATTERN = Regex("agribalyse:row:[1-9][0-9]*:agb:[^:/\\n]+")
        private val CIQUAL_PATTERN = Regex("ciqual:(food|constituent|source):[^:/\\n]+|ciqual:taxonomy:[^:/\\n]+/[^:/\\n]+/[^:/\\n]+")
        private val GI_PATTERN = Regex("gi:(measurement|mean-summary|category-note|footnote):[1-9][0-9]*")
    }
}
