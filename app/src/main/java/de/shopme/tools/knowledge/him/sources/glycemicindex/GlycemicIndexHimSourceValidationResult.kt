package de.shopme.tools.knowledge.him.sources.glycemicindex

data class GlycemicIndexHimSourceValidationResult(
    val measurementCount: Int,
    val meanSummaryCount: Int,
    val categoryNoteCount: Int,
    val footnoteCount: Int,
    val majorCategoryCount: Int,
    val subcategoryCount: Int,
    val unresolvedCount: Int,
    val giMinimum: Double,
    val giMaximum: Double,
    val giAbove100Count: Int,
    val sizeBytes: Long,
    val compressedSha256: String,
    val contentSha256: String,
)
