package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.validation

import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionRetrievalRequest
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionRetrievedCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.OFFNutritionRetrievalTextNormalizer

class OFFNutritionRetrievalQualityValidator {

    fun validate(
        requests: List<CatalogOFFNutritionRetrievalRequest>
    ): OFFNutritionRetrievalQualityReport {

        require(
            requests ==
                    requests.sortedBy { request ->
                        request.catalogIndex
                    }
        ) {
            "Retrieval requests must be sorted by catalogIndex."
        }

        require(
            requests.map { request ->
                request.catalogIndex
            } ==
                    requests.indices.toList()
        ) {
            "Retrieval request indexes must be contiguous."
        }

        val findings =
            requests.map { request ->
                validateRequest(
                    request = request
                )
            }

        val countsByPrimaryType =
            OFFNutritionRetrievalQualityType.entries
                .associateWith { type ->
                    findings.count { finding ->
                        finding.primaryType == type
                    }
                }
                .filterValues { count ->
                    count > 0
                }

        val countsByQualityType =
            OFFNutritionRetrievalQualityType.entries
                .associateWith { type ->
                    findings.count { finding ->
                        type in finding.qualityTypes
                    }
                }
                .filterValues { count ->
                    count > 0
                }

        val safePrimaryTypes =
            setOf(
                OFFNutritionRetrievalQualityType.EXACT_ALIAS_MATCH,
                OFFNutritionRetrievalQualityType.STRONG_LEXICAL_MATCH
            )

        return OFFNutritionRetrievalQualityReport(
            version =
                OFFNutritionRetrievalQualityReport.CURRENT_VERSION,
            requestCount =
                findings.size,
            requestWithCandidatesCount =
                findings.count { finding ->
                    finding.candidateCount > 0
                },
            requestWithoutCandidatesCount =
                findings.count { finding ->
                    finding.candidateCount == 0
                },
            exactAliasMatchCount =
                findings.count { finding ->
                    finding.primaryType ==
                            OFFNutritionRetrievalQualityType.EXACT_ALIAS_MATCH
                },
            strongLexicalMatchCount =
                findings.count { finding ->
                    finding.primaryType ==
                            OFFNutritionRetrievalQualityType.STRONG_LEXICAL_MATCH
                },
            riskyFindingCount =
                findings.count { finding ->
                    finding.primaryType !in safePrimaryTypes
                },
            countsByPrimaryType =
                countsByPrimaryType,
            countsByQualityType =
                countsByQualityType,
            findings =
                findings
        )
    }

    private fun validateRequest(
        request: CatalogOFFNutritionRetrievalRequest
    ): OFFNutritionRetrievalQualityFinding {

        val topCandidate =
            request.candidates.firstOrNull()

        if (topCandidate == null) {
            return OFFNutritionRetrievalQualityFinding(
                catalogIndex =
                    request.catalogIndex,
                catalogKey =
                    request.catalogKey,
                normalizedEnglish =
                    request.normalizedEnglish,
                itemName =
                    request.itemName,
                category =
                    request.category,
                production =
                    request.production,
                candidateCount =
                    0,
                topCandidateServerKey =
                    null,
                topCandidateScore =
                    null,
                topCandidateExactMatch =
                    null,
                matchedCatalogTerm =
                    null,
                matchedCandidateAlias =
                    null,
                tokenIntersectionCount =
                    null,
                tokenUnionCount =
                    null,
                tokenJaccard =
                    null,
                containmentScore =
                    null,
                primaryType =
                    OFFNutritionRetrievalQualityType.NO_CANDIDATES,
                qualityTypes =
                    listOf(
                        OFFNutritionRetrievalQualityType.NO_CANDIDATES
                    ),
                reasons =
                    listOf(
                        "No OFF nutrition candidate was retrieved."
                    )
            )
        }

        val classification =
            classifyCandidate(
                request = request,
                candidate = topCandidate
            )

        return OFFNutritionRetrievalQualityFinding(
            catalogIndex =
                request.catalogIndex,
            catalogKey =
                request.catalogKey,
            normalizedEnglish =
                request.normalizedEnglish,
            itemName =
                request.itemName,
            category =
                request.category,
            production =
                request.production,
            candidateCount =
                request.candidates.size,
            topCandidateServerKey =
                topCandidate.serverKey,
            topCandidateScore =
                topCandidate.score,
            topCandidateExactMatch =
                topCandidate.exactMatch,
            matchedCatalogTerm =
                topCandidate.matchedCatalogTerm,
            matchedCandidateAlias =
                topCandidate.matchedCandidateAlias,
            tokenIntersectionCount =
                topCandidate.tokenIntersectionCount,
            tokenUnionCount =
                topCandidate.tokenUnionCount,
            tokenJaccard =
                topCandidate.tokenJaccard,
            containmentScore =
                topCandidate.containmentScore,
            primaryType =
                classification.primaryType,
            qualityTypes =
                classification.qualityTypes,
            reasons =
                classification.reasons
        )
    }

    private fun classifyCandidate(
        request: CatalogOFFNutritionRetrievalRequest,
        candidate: CatalogOFFNutritionRetrievedCandidate
    ): Classification {

        if (candidate.exactMatch) {
            return Classification(
                primaryType =
                    OFFNutritionRetrievalQualityType.EXACT_ALIAS_MATCH,
                qualityTypes =
                    sortedTypes(
                        OFFNutritionRetrievalQualityType.EXACT_ALIAS_MATCH
                    ),
                reasons =
                    sortedReasons(
                        "Top candidate contains an exact normalized alias match."
                    )
            )
        }

        val catalogTokens =
            normalizedTokens(
                candidate.matchedCatalogTerm
            )

        val candidateTokens =
            normalizedTokens(
                candidate.matchedCandidateAlias
            )

        val qualityTypes =
            mutableSetOf<OFFNutritionRetrievalQualityType>()

        val reasons =
            mutableSetOf<String>()

        if (candidate.score < VERY_LOW_SCORE_THRESHOLD) {
            qualityTypes +=
                OFFNutritionRetrievalQualityType.VERY_LOW_SCORE

            reasons +=
                "Top candidate score is below $VERY_LOW_SCORE_THRESHOLD."
        } else if (candidate.score < LOW_SCORE_THRESHOLD) {
            qualityTypes +=
                OFFNutritionRetrievalQualityType.LOW_SCORE

            reasons +=
                "Top candidate score is below $LOW_SCORE_THRESHOLD."
        }

        if (
            candidate.tokenIntersectionCount <= 1 &&
            candidate.tokenJaccard < MINIMUM_ACCEPTABLE_JACCARD
        ) {
            qualityTypes +=
                OFFNutritionRetrievalQualityType.WEAK_TOKEN_OVERLAP

            reasons +=
                "Top candidate has weak token overlap."
        }

        detectGenericModifierMatch(
            catalogTokens = catalogTokens,
            candidateTokens = candidateTokens,
            qualityTypes = qualityTypes,
            reasons = reasons
        )

        detectProcessingFormMismatch(
            catalogTokens = catalogTokens,
            candidateTokens = candidateTokens,
            qualityTypes = qualityTypes,
            reasons = reasons
        )

        detectPreparationFormMismatch(
            catalogTokens = catalogTokens,
            candidateTokens = candidateTokens,
            qualityTypes = qualityTypes,
            reasons = reasons
        )

        detectAnimalSpeciesMismatch(
            catalogTokens = catalogTokens,
            candidateTokens = candidateTokens,
            qualityTypes = qualityTypes,
            reasons = reasons
        )

        detectPlantProductMismatch(
            catalogTokens = catalogTokens,
            candidateTokens = candidateTokens,
            qualityTypes = qualityTypes,
            reasons = reasons
        )

        detectDietaryFormMismatch(
            catalogTokens = catalogTokens,
            candidateTokens = candidateTokens,
            qualityTypes = qualityTypes,
            reasons = reasons
        )

        if (qualityTypes.isEmpty()) {
            if (
                candidate.score >= STRONG_SCORE_THRESHOLD &&
                candidate.containmentScore >=
                STRONG_CONTAINMENT_THRESHOLD &&
                candidate.tokenJaccard >=
                STRONG_JACCARD_THRESHOLD
            ) {
                qualityTypes +=
                    OFFNutritionRetrievalQualityType.STRONG_LEXICAL_MATCH

                reasons +=
                    "Top candidate passes deterministic strong lexical thresholds."
            } else {
                qualityTypes +=
                    OFFNutritionRetrievalQualityType.UNKNOWN_RISK

                reasons +=
                    "Top candidate does not pass strong lexical thresholds."
            }
        }

        val sortedQualityTypes =
            qualityTypes
                .sortedBy { type ->
                    type.name
                }

        return Classification(
            primaryType =
                determinePrimaryType(
                    qualityTypes = qualityTypes
                ),
            qualityTypes =
                sortedQualityTypes,
            reasons =
                reasons.sorted()
        )
    }

    private fun detectGenericModifierMatch(
        catalogTokens: Set<String>,
        candidateTokens: Set<String>,
        qualityTypes: MutableSet<OFFNutritionRetrievalQualityType>,
        reasons: MutableSet<String>
    ) {

        val sharedTokens =
            catalogTokens intersect candidateTokens

        if (
            sharedTokens.isNotEmpty() &&
            sharedTokens.all { token ->
                token in GENERIC_MODIFIER_TOKENS
            }
        ) {
            qualityTypes +=
                OFFNutritionRetrievalQualityType.GENERIC_MODIFIER_MATCH

            reasons +=
                "Catalog and candidate overlap only through generic modifier tokens: " +
                        sharedTokens.sorted().joinToString(", ")
        }
    }

    private fun detectProcessingFormMismatch(
        catalogTokens: Set<String>,
        candidateTokens: Set<String>,
        qualityTypes: MutableSet<OFFNutritionRetrievalQualityType>,
        reasons: MutableSet<String>
    ) {

        val candidateProcessingTokens =
            candidateTokens intersect PROCESSING_FORM_TOKENS

        val catalogProcessingTokens =
            catalogTokens intersect PROCESSING_FORM_TOKENS

        if (
            candidateProcessingTokens.isNotEmpty() &&
            catalogProcessingTokens.isEmpty()
        ) {
            qualityTypes +=
                OFFNutritionRetrievalQualityType.PROCESSING_FORM_MISMATCH

            reasons +=
                "Candidate introduces processing-form tokens absent from catalog: " +
                        candidateProcessingTokens.sorted().joinToString(", ")
        }
    }

    private fun detectPreparationFormMismatch(
        catalogTokens: Set<String>,
        candidateTokens: Set<String>,
        qualityTypes: MutableSet<OFFNutritionRetrievalQualityType>,
        reasons: MutableSet<String>
    ) {

        val candidatePreparationTokens =
            candidateTokens intersect PREPARATION_FORM_TOKENS

        val catalogPreparationTokens =
            catalogTokens intersect PREPARATION_FORM_TOKENS

        if (
            candidatePreparationTokens.isNotEmpty() &&
            catalogPreparationTokens.isEmpty()
        ) {
            qualityTypes +=
                OFFNutritionRetrievalQualityType.PREPARATION_FORM_MISMATCH

            reasons +=
                "Candidate introduces preparation-form tokens absent from catalog: " +
                        candidatePreparationTokens.sorted().joinToString(", ")
        }
    }

    private fun detectAnimalSpeciesMismatch(
        catalogTokens: Set<String>,
        candidateTokens: Set<String>,
        qualityTypes: MutableSet<OFFNutritionRetrievalQualityType>,
        reasons: MutableSet<String>
    ) {

        val catalogSpecies =
            catalogTokens intersect ANIMAL_SPECIES_TOKENS

        val candidateSpecies =
            candidateTokens intersect ANIMAL_SPECIES_TOKENS

        if (
            catalogSpecies.isNotEmpty() &&
            candidateSpecies.isNotEmpty() &&
            catalogSpecies.intersect(candidateSpecies).isEmpty()
        ) {
            qualityTypes +=
                OFFNutritionRetrievalQualityType.ANIMAL_SPECIES_MISMATCH

            reasons +=
                "Catalog and candidate contain different animal-species tokens: " +
                        "catalog=${catalogSpecies.sorted()}, " +
                        "candidate=${candidateSpecies.sorted()}."
        }
    }

    private fun detectPlantProductMismatch(
        catalogTokens: Set<String>,
        candidateTokens: Set<String>,
        qualityTypes: MutableSet<OFFNutritionRetrievalQualityType>,
        reasons: MutableSet<String>
    ) {

        val catalogProductForms =
            catalogTokens intersect PLANT_PRODUCT_FORM_TOKENS

        val candidateProductForms =
            candidateTokens intersect PLANT_PRODUCT_FORM_TOKENS

        if (
            catalogProductForms.isEmpty() &&
            candidateProductForms.isNotEmpty()
        ) {
            qualityTypes +=
                OFFNutritionRetrievalQualityType.PLANT_PRODUCT_MISMATCH

            reasons +=
                "Candidate introduces a derived plant-product form: " +
                        candidateProductForms.sorted().joinToString(", ")
        }
    }

    private fun detectDietaryFormMismatch(
        catalogTokens: Set<String>,
        candidateTokens: Set<String>,
        qualityTypes: MutableSet<OFFNutritionRetrievalQualityType>,
        reasons: MutableSet<String>
    ) {

        val catalogDietTokens =
            catalogTokens intersect DIETARY_FORM_TOKENS

        val candidateDietTokens =
            candidateTokens intersect DIETARY_FORM_TOKENS

        if (
            catalogDietTokens.isNotEmpty() &&
            candidateDietTokens.isEmpty()
        ) {
            qualityTypes +=
                OFFNutritionRetrievalQualityType.DIETARY_FORM_MISMATCH

            reasons +=
                "Catalog dietary-form tokens are absent from candidate: " +
                        catalogDietTokens.sorted().joinToString(", ")
        }
    }

    private fun determinePrimaryType(
        qualityTypes: Set<OFFNutritionRetrievalQualityType>
    ): OFFNutritionRetrievalQualityType {

        return PRIMARY_TYPE_PRIORITY
            .firstOrNull { type ->
                type in qualityTypes
            }
            ?: error(
                "No primary retrieval quality type could be determined."
            )
    }

    private fun normalizedTokens(
        value: String
    ): Set<String> {

        return OFFNutritionRetrievalTextNormalizer
            .normalize(value)
            .split(' ')
            .asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSortedSet()
    }

    private fun sortedTypes(
        vararg types: OFFNutritionRetrievalQualityType
    ): List<OFFNutritionRetrievalQualityType> {

        return types
            .distinct()
            .sortedBy { type ->
                type.name
            }
    }

    private fun sortedReasons(
        vararg reasons: String
    ): List<String> {

        return reasons
            .distinct()
            .sorted()
    }

    private data class Classification(
        val primaryType: OFFNutritionRetrievalQualityType,
        val qualityTypes: List<OFFNutritionRetrievalQualityType>,
        val reasons: List<String>
    )

    companion object {

        private const val VERY_LOW_SCORE_THRESHOLD =
            0.40

        private const val LOW_SCORE_THRESHOLD =
            0.60

        private const val MINIMUM_ACCEPTABLE_JACCARD =
            0.34

        private const val STRONG_SCORE_THRESHOLD =
            0.80

        private const val STRONG_CONTAINMENT_THRESHOLD =
            0.80

        private const val STRONG_JACCARD_THRESHOLD =
            0.40

        private val PRIMARY_TYPE_PRIORITY =
            listOf(
                OFFNutritionRetrievalQualityType.NO_CANDIDATES,
                OFFNutritionRetrievalQualityType.ANIMAL_SPECIES_MISMATCH,
                OFFNutritionRetrievalQualityType.DIETARY_FORM_MISMATCH,
                OFFNutritionRetrievalQualityType.PROCESSING_FORM_MISMATCH,
                OFFNutritionRetrievalQualityType.PREPARATION_FORM_MISMATCH,
                OFFNutritionRetrievalQualityType.PLANT_PRODUCT_MISMATCH,
                OFFNutritionRetrievalQualityType.GENERIC_MODIFIER_MATCH,
                OFFNutritionRetrievalQualityType.VERY_LOW_SCORE,
                OFFNutritionRetrievalQualityType.LOW_SCORE,
                OFFNutritionRetrievalQualityType.WEAK_TOKEN_OVERLAP,
                OFFNutritionRetrievalQualityType.EXACT_ALIAS_MATCH,
                OFFNutritionRetrievalQualityType.STRONG_LEXICAL_MATCH,
                OFFNutritionRetrievalQualityType.UNKNOWN_RISK
            )

        private val GENERIC_MODIFIER_TOKENS =
            setOf(
                "bio",
                "canned",
                "cold",
                "dried",
                "fresh",
                "frozen",
                "ground",
                "organic",
                "plain",
                "prepared",
                "raw",
                "ready",
                "smoked",
                "sweet",
                "wild"
            )

        private val PROCESSING_FORM_TOKENS =
            setOf(
                "butter",
                "cake",
                "candy",
                "cream",
                "drink",
                "flakes",
                "flour",
                "jam",
                "juice",
                "milk",
                "oil",
                "paste",
                "powder",
                "pulp",
                "sauce",
                "shake",
                "spread",
                "syrup",
                "tea",
                "yogurt"
            )

        private val PREPARATION_FORM_TOKENS =
            setOf(
                "baked",
                "boiled",
                "breaded",
                "cooked",
                "fried",
                "grilled",
                "pickled",
                "roasted",
                "steamed",
                "stuffed"
            )

        private val ANIMAL_SPECIES_TOKENS =
            setOf(
                "beef",
                "boar",
                "chicken",
                "cod",
                "duck",
                "eel",
                "goat",
                "goose",
                "haddock",
                "herring",
                "lamb",
                "pork",
                "salmon",
                "trout",
                "turkey",
                "veal",
                "venison"
            )

        private val PLANT_PRODUCT_FORM_TOKENS =
            setOf(
                "cake",
                "candy",
                "cream",
                "drink",
                "jam",
                "juice",
                "milk",
                "oil",
                "pie",
                "pulp",
                "sauce",
                "spread",
                "syrup",
                "tea",
                "yogurt"
            )

        private val DIETARY_FORM_TOKENS =
            setOf(
                "plant",
                "plantbased",
                "substitute",
                "vegan",
                "vegetarian"
            )
    }
}