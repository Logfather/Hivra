package de.shopme.testing.system.tools.knowledge.catalog.application

import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction

data class CatalogCanonicalizationApplicationEntry(
    val sourceIndex: Int,
    val originalItemName: String,
    val action: CatalogCanonicalizationAction,
    val automatic: Boolean,
    val status: CatalogCanonicalizationApplicationStatus,
    val resultingNormalizedKey: String?,
    val resultingCategory: String?,
    val mergeTargetSourceIndex: Int?,
    val reasons: List<String>
) {

    init {
        require(sourceIndex >= 0) {
            "sourceIndex must not be negative."
        }

        require(originalItemName.isNotBlank()) {
            "originalItemName must not be blank."
        }

        require(reasons.none(String::isBlank)) {
            "reasons must not contain blank values."
        }

        require(reasons.distinct().size == reasons.size) {
            "reasons must not contain duplicates."
        }

        require(reasons == reasons.sorted()) {
            "reasons must be deterministically sorted."
        }

        if (
            status ==
            CatalogCanonicalizationApplicationStatus.MERGED_INTO_TARGET
        ) {
            requireNotNull(mergeTargetSourceIndex) {
                "Merged application entry must contain mergeTargetSourceIndex."
            }

            require(mergeTargetSourceIndex != sourceIndex) {
                "Merged application entry must not target itself."
            }
        }
    }
}