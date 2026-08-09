package de.shopme.testing.system.tools.knowledge.catalog.review.reclassification

import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics.CatalogRemainingTypoDiagnosticsResult
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics.CatalogTypoCandidateDiagnosticEntry
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics.CatalogTypoCandidateDiagnosticReason
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics.CatalogTypoCandidateRecommendation
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics.CatalogTypoCandidateSubtype
import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogMisclassifiedTypoReclassifierTest {

    private val reclassifier =
        CatalogMisclassifiedTypoReclassifier()

    @Test
    fun reclassifyFrozenFormVariant() {
        val result =
            reclassifier.reclassify(
                diagnosticsResult(
                    diagnosticEntry(
                        sourceIndex = 10,
                        sourceName = "TK-Blattspinat",
                        sourceCategory =
                            "Tiefkühlprodukte",
                        targetSourceIndex = 20,
                        targetName = "Blattspinat",
                        targetCategory = "Gemüse"
                    )
                )
            )

        val entry = result.entries.single()

        assertEquals(
            CatalogMisclassifiedTypoClass
                .FROZEN_FORM_VARIANT,
            entry.reclassifiedAs
        )

        assertEquals(
            CatalogMisclassifiedTypoRecommendation
                .PRESERVE_AS_SEPARATE_FOODS,
            entry.recommendation
        )
    }

    @Test
    fun reclassifyBioAttributeVariant() {
        val result =
            reclassifier.reclassify(
                diagnosticsResult(
                    diagnosticEntry(
                        sourceIndex = 10,
                        sourceName =
                            "Vegetarische Pizza Bio",
                        sourceCategory =
                            "Vegetarisch",
                        targetSourceIndex = 20,
                        targetName =
                            "Vegetarische Pizza",
                        targetCategory =
                            "Vegetarisch"
                    )
                )
            )

        assertEquals(
            CatalogMisclassifiedTypoClass
                .BIO_ATTRIBUTE_VARIANT,
            result.entries.single()
                .reclassifiedAs
        )
    }

    @Test
    fun reclassifyCannedFormVariant() {
        val result =
            reclassifier.reclassify(
                diagnosticsResult(
                    diagnosticEntry(
                        sourceIndex = 10,
                        sourceName =
                            "Ananas in Dose",
                        sourceCategory =
                            "Konserven",
                        targetSourceIndex = 20,
                        targetName = "Ananas",
                        targetCategory = "Obst"
                    )
                )
            )

        assertEquals(
            CatalogMisclassifiedTypoClass
                .CANNED_FORM_VARIANT,
            result.entries.single()
                .reclassifiedAs
        )
    }

    @Test
    fun preserveOnlyMisclassifiedCandidates() {
        val diagnostics =
            diagnosticsResult(
                diagnosticEntry(
                    sourceIndex = 10,
                    sourceName =
                        "TK-Blattspinat",
                    sourceCategory =
                        "Tiefkühlprodukte",
                    targetSourceIndex = 20,
                    targetName = "Blattspinat",
                    targetCategory = "Gemüse"
                ),
                diagnosticEntry(
                    sourceIndex = 30,
                    sourceName =
                        "Chili sin Carne",
                    sourceCategory =
                        "Vegetarisch",
                    targetSourceIndex = 40,
                    targetName =
                        "Chili con Carne",
                    targetCategory =
                        "Fertiggerichte",
                    subtype =
                        CatalogTypoCandidateSubtype
                            .MULTI_EDIT_TYPO
                )
            )

        val result =
            reclassifier.reclassify(diagnostics)

        assertEquals(1, result.inputCandidateCount)
        assertEquals(1, result.entries.size)
        assertEquals(10, result.entries.single().sourceIndex)
    }

    @Test
    fun reclassifyDeterministically() {
        val diagnostics =
            diagnosticsResult(
                diagnosticEntry(
                    sourceIndex = 20,
                    sourceName = "Ananas in Dose",
                    sourceCategory = "Konserven",
                    targetSourceIndex = 30,
                    targetName = "Ananas",
                    targetCategory = "Obst"
                ),
                diagnosticEntry(
                    sourceIndex = 10,
                    sourceName = "TK-Blattspinat",
                    sourceCategory =
                        "Tiefkühlprodukte",
                    targetSourceIndex = 40,
                    targetName = "Blattspinat",
                    targetCategory = "Gemüse"
                )
            )

        val first =
            reclassifier.reclassify(diagnostics)

        val second =
            reclassifier.reclassify(diagnostics)

        assertEquals(first, second)

        assertEquals(
            listOf(10, 20),
            first.entries.map { it.sourceIndex }
        )
    }

    private fun diagnosticEntry(
        sourceIndex: Int,
        sourceName: String,
        sourceCategory: String,
        targetSourceIndex: Int,
        targetName: String,
        targetCategory: String,
        subtype:
        CatalogTypoCandidateSubtype =
            CatalogTypoCandidateSubtype
                .PROBABLY_MISCLASSIFIED
    ): CatalogTypoCandidateDiagnosticEntry =
        CatalogTypoCandidateDiagnosticEntry(
            sourceIndex = sourceIndex,
            sourceName = sourceName,
            sourceCategory = sourceCategory,
            sourceNormalizedKey =
                sourceName.lowercase(),

            targetSourceIndex =
                targetSourceIndex,
            targetName = targetName,
            targetCategory = targetCategory,
            targetNormalizedKey =
                targetName.lowercase(),

            originalAction =
                CatalogCanonicalizationAction.REVIEW,

            originalReasons =
                listOf(
                    "Duplicate reasons: TYPO_VARIANT."
                ),

            normalizedSourceText =
                sourceName.lowercase(),
            normalizedTargetText =
                targetName.lowercase(),

            sourceTokens =
                sourceName.lowercase()
                    .split(' '),
            targetTokens =
                targetName.lowercase()
                    .split(' '),

            sourceNumericTokens =
                emptyList(),
            targetNumericTokens =
                emptyList(),

            tokenDifferences =
                emptyList(),

            subtype = subtype,

            recommendation =
                if (
                    subtype ==
                    CatalogTypoCandidateSubtype
                        .PROBABLY_MISCLASSIFIED
                ) {
                    CatalogTypoCandidateRecommendation
                        .RECLASSIFY_DUPLICATE_REASON
                } else {
                    CatalogTypoCandidateRecommendation
                        .MANUAL_REVIEW_REQUIRED
                },

            existingResolverWouldAccept =
                false,

            diagnosticReasons =
                listOf(
                    CatalogTypoCandidateDiagnosticReason
                        .TOKEN_COUNT_MISMATCH
                )
        )

    private fun diagnosticsResult(
        vararg entries:
        CatalogTypoCandidateDiagnosticEntry
    ): CatalogRemainingTypoDiagnosticsResult {
        val sorted =
            entries.sortedBy { it.sourceIndex }

        val countsBySubtype =
            sorted
                .groupingBy { it.subtype }
                .eachCount()
                .toList()
                .sortedBy { it.first.name }
                .associate { it }

        val countsByRecommendation =
            sorted
                .groupingBy { it.recommendation }
                .eachCount()
                .toList()
                .sortedBy { it.first.name }
                .associate { it }

        val countsByReason =
            sorted
                .flatMap {
                    it.diagnosticReasons
                }
                .groupingBy { it }
                .eachCount()
                .toList()
                .sortedBy { it.first.name }
                .associate { it }

        val countsByCategory =
            sorted
                .groupingBy {
                    it.sourceCategory ?: "<uncategorized>"
                }
                .eachCount()
                .toSortedMap()

        return CatalogRemainingTypoDiagnosticsResult(
            version =
                CatalogRemainingTypoDiagnosticsResult
                    .CURRENT_VERSION,
            remainingTypoCandidateCount =
                sorted.size,
            diagnosedEntryCount =
                sorted.size,
            existingResolverShouldHaveAcceptedCount =
                0,
            countsBySubtype =
                countsBySubtype,
            countsByRecommendation =
                countsByRecommendation,
            countsByDiagnosticReason =
                countsByReason,
            countsByCategory =
                countsByCategory,
            entries =
                sorted,
            valid = true
        )
    }
}