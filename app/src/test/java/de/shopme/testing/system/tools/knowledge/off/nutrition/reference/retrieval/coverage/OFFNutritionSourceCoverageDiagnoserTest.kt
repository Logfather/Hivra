package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.retrieval.coverage

import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionRetrievalRequest
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage.OFFNutritionCoverageAliasEntry
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage.OFFNutritionRawSourceCoverage
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage.OFFNutritionRawSourceCoverageEntry
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage.OFFNutritionSourceCoverageDiagnoser
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage.OFFNutritionSourceCoverageStage
import kotlin.test.Test
import kotlin.test.assertEquals

class OFFNutritionSourceCoverageDiagnoserTest {

    @Test
    fun diagnose_identifiesMatcherCandidateRetrievalIndexGap() {

        val request =
            request(
                catalogIndex =
                    1,
                catalogKey =
                    "kiwis",
                normalizedEnglish =
                    "kiwis",
                terms =
                    listOf(
                        "kiwi",
                        "kiwis"
                    )
            )

        val report =
            OFFNutritionSourceCoverageDiagnoser()
                .diagnose(
                    allRequests =
                        listOf(request),
                    rawSourceCoverage =
                        OFFNutritionRawSourceCoverage(
                            scannedProductCount =
                                100,
                            entriesByCatalogIndex =
                                mapOf(
                                    1 to
                                            OFFNutritionRawSourceCoverageEntry(
                                                productMatchCount =
                                                    4,
                                                productWithAnyNutritionCount =
                                                    3,
                                                productWithUsableNutritionCount =
                                                    2,
                                                matchedProductNames =
                                                    listOf(
                                                        "Kiwi"
                                                    )
                                            )
                                )
                        ),
                    referenceCandidates =
                        listOf(
                            entry(
                                identity = "kiwi-reference",
                                aliases =
                                    setOf("kiwi")
                            )
                        ),
                    referenceAggregates =
                        listOf(
                            entry(
                                identity = "kiwi-aggregate",
                                aliases =
                                    setOf("kiwi")
                            )
                        ),
                    matcherCandidates =
                        listOf(
                            entry(
                                identity = "kiwi",
                                aliases =
                                    setOf("kiwi")
                            )
                        )
                )

        val finding =
            report.findings.single()

        assertEquals(
            OFFNutritionSourceCoverageStage.RETRIEVAL_INDEX,
            finding.firstMissingStage
        )

        assertEquals(
            1,
            report.countsByFirstMissingStage[
                OFFNutritionSourceCoverageStage.RETRIEVAL_INDEX
            ]
        )
    }

    @Test
    fun diagnose_identifiesRawSourceGap() {

        val request =
            request(
                catalogIndex =
                    2,
                catalogKey =
                    "unknown food",
                normalizedEnglish =
                    "unknown food",
                terms =
                    listOf(
                        "unknown food"
                    )
            )

        val report =
            OFFNutritionSourceCoverageDiagnoser()
                .diagnose(
                    allRequests =
                        listOf(request),
                    rawSourceCoverage =
                        OFFNutritionRawSourceCoverage(
                            scannedProductCount =
                                100,
                            entriesByCatalogIndex =
                                mapOf(
                                    2 to
                                            OFFNutritionRawSourceCoverageEntry(
                                                productMatchCount =
                                                    0,
                                                productWithAnyNutritionCount =
                                                    0,
                                                productWithUsableNutritionCount =
                                                    0,
                                                matchedProductNames =
                                                    emptyList()
                                            )
                                )
                        ),
                    referenceCandidates =
                        emptyList(),
                    referenceAggregates =
                        emptyList(),
                    matcherCandidates =
                        emptyList()
                )

        assertEquals(
            OFFNutritionSourceCoverageStage.RAW_OFF_PRODUCT,
            report.findings.single().firstMissingStage
        )
    }

    private fun entry(
        identity: String,
        aliases: Set<String>
    ): OFFNutritionCoverageAliasEntry {

        return OFFNutritionCoverageAliasEntry(
            identity =
                identity,
            aliases =
                aliases.toSortedSet()
        )
    }

    private fun request(
        catalogIndex: Int,
        catalogKey: String,
        normalizedEnglish: String,
        terms: List<String>
    ): CatalogOFFNutritionRetrievalRequest {

        return CatalogOFFNutritionRetrievalRequest(
            catalogIndex =
                catalogIndex,
            catalogKey =
                catalogKey,
            normalizedEnglish =
                normalizedEnglish,
            itemName =
                catalogKey,
            category =
                null,
            production =
                null,
            catalogTerms =
                terms.sorted(),
            candidates =
                emptyList()
        )
    }
}