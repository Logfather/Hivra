package de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics

import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction

data class CatalogTypoCandidateDiagnosticEntry(
    val sourceIndex: Int,
    val sourceName: String,
    val sourceCategory: String?,
    val sourceNormalizedKey: String?,

    val targetSourceIndex: Int?,
    val targetName: String?,
    val targetCategory: String?,
    val targetNormalizedKey: String?,

    val originalAction:
    CatalogCanonicalizationAction,

    val originalReasons: List<String>,

    val normalizedSourceText: String,
    val normalizedTargetText: String?,

    val sourceTokens: List<String>,
    val targetTokens: List<String>,

    val sourceNumericTokens: List<String>,
    val targetNumericTokens: List<String>,

    val tokenDifferences:
    List<CatalogTypoTokenDifference>,

    val subtype:
    CatalogTypoCandidateSubtype,

    val recommendation:
    CatalogTypoCandidateRecommendation,

    val existingResolverWouldAccept: Boolean,

    val diagnosticReasons:
    List<CatalogTypoCandidateDiagnosticReason>
) {

    init {
        require(sourceIndex >= 0)
        require(sourceName.isNotBlank())

        targetSourceIndex?.let {
            require(it >= 0)
            require(it != sourceIndex)
        }

        require(
            originalReasons ==
                    originalReasons
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        ) {
            "originalReasons must be normalized and sorted."
        }

        require(
            sourceTokens.none(String::isBlank)
        )

        require(
            targetTokens.none(String::isBlank)
        )

        require(
            sourceNumericTokens.none(String::isBlank)
        )

        require(
            targetNumericTokens.none(String::isBlank)
        )

        require(
            tokenDifferences ==
                    tokenDifferences.sortedBy {
                        it.tokenIndex
                    }
        ) {
            "tokenDifferences must be sorted by tokenIndex."
        }

        require(diagnosticReasons.isNotEmpty())

        require(
            diagnosticReasons ==
                    diagnosticReasons
                        .distinct()
                        .sortedBy { it.name }
        ) {
            "diagnosticReasons must be unique and sorted."
        }
    }
}