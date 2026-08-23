package de.shopme.tools.knowledge.him.sources.glycemicindex

/**
 * Source-faithful projection of Supplemental Table 1.
 *
 * Preserves lexical source values and keeps source missingness distinguishable
 * from unresolved extraction. It performs no canonical catalog mapping,
 * identity normalization, aggregation, Knowledge merge, or LOW/MEDIUM/HIGH
 * classification.
 */
data class GlycemicIndexHimSourceArtifact(
    val measurements: List<GlycemicIndexMeasurement>,
    val meanSummaries: List<GlycemicIndexMeanSummary>,
    val categoryNotes: List<GlycemicIndexCategoryNote>,
    val footnotes: List<GlycemicIndexFootnote>,
)

data class GlycemicIndexMeasurement(
    val foodNumber: Int,
    val pageNumber: Int,
    val sourceContext: GlycemicIndexSourceContext,
    val foodItem: GlycemicSourceField,
    val country: GlycemicSourceField,
    val year: GlycemicSourceField,
    val gi: GlycemicSourceField,
    val sem: GlycemicSourceField,
    val gl: GlycemicSourceField,
    val subjects: GlycemicSourceField,
    val availableCarbohydrate: GlycemicSourceField,
    val testPortion: GlycemicSourceField,
    val referenceFoodTime: GlycemicSourceField,
    val timepoints: GlycemicSourceField,
    val sampleCollection: GlycemicSourceField,
    val analysisMethod: GlycemicSourceField,
    val referenceCode: GlycemicSourceField,
)

data class GlycemicIndexSourceContext(
    val majorCategory: String,
    val subcategory: String?,
    val deeperHeading: String?,
)

data class GlycemicSourceField(
    val lexicalValue: String?,
    val status: GlycemicSourceFieldStatus,
)

enum class GlycemicSourceFieldStatus {
    PRESENT,
    SOURCE_MISSING,
    UNRESOLVED,
}

data class GlycemicIndexMeanSummary(
    val pageNumber: Int,
    val sourceContext: GlycemicIndexSourceContext,
    val lexicalText: GlycemicSourceField,
)

data class GlycemicIndexCategoryNote(
    val pageNumber: Int,
    val sourceContext: GlycemicIndexSourceContext,
    val lexicalText: GlycemicSourceField,
)

data class GlycemicIndexFootnote(
    val pageNumber: Int,
    val identifier: GlycemicSourceField,
    val lexicalText: GlycemicSourceField,
)
