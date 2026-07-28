package de.shopme.tools.knowledge.off.nutrition.reference

import de.shopme.tools.knowledge.ki_candidates.CanonicalKnowledgeCandidate
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidate
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidateType
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.OFFNutritionReferenceCandidateTrace
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.OFFNutritionReferenceCandidateTraceSink

class OFFNutritionReferenceCandidateGenerator(
    private val traceSink:
    OFFNutritionReferenceCandidateTraceSink =
        OFFNutritionReferenceCandidateTraceSink.NONE
) {

    fun generate(
        candidates: List<CanonicalKnowledgeCandidate>
    ): OFFNutritionReferenceCandidateGenerationResult {

        val generated =
            mutableListOf<CanonicalOFFNutritionReferenceCandidate>()

        var skippedWithoutNutritionCount =
            0

        var skippedInvalidIdentityCount =
            0

        var skippedInvalidNutritionPayloadCount =
            0

        candidates.forEach { candidate ->

            val rawSourceId =
                candidate.metadata.sourceId
                    ?.trim()

            val rawCanonicalId =
                candidate.canonicalId
                    .trim()

            val sourceId =
                rawSourceId
                    ?.takeIf(String::isNotBlank)

            val canonicalId =
                rawCanonicalId
                    .takeIf(String::isNotBlank)

            val productName =
                candidate.metadata
                    .attributes["productName"]
                    ?.normalizeOptionalString()
                    ?: canonicalId
                    ?: sourceId
                    ?: UNKNOWN_PRODUCT_NAME

            val normalizedProductIdentities =
                buildNormalizedProductIdentities(
                    candidate = candidate,
                    canonicalId = canonicalId,
                    productName = productName
                )

            if (sourceId == null || canonicalId == null) {
                skippedInvalidIdentityCount++

                traceSink.record(
                    OFFNutritionReferenceCandidateTrace(
                        sourceProductId =
                            sourceId
                                ?: canonicalId
                                ?: UNKNOWN_SOURCE_PRODUCT_ID,
                        productName =
                            productName,
                        normalizedProductIdentities =
                            normalizedProductIdentities,
                        identityAccepted =
                            false,
                        identityRejectionReasons =
                            buildIdentityRejectionReasons(
                                sourceId = sourceId,
                                canonicalId = canonicalId
                            ),
                        nutritionAccepted =
                            false,
                        nutritionRejectionReasons =
                            emptyList(),
                        referenceEligible =
                            false,
                        referenceEligibilityRejectionReasons =
                            emptyList(),
                        candidateCreated =
                            false,
                        createdCandidateId =
                            null
                    )
                )

                return@forEach
            }

            val nutritionDimensions =
                candidate.dimensions
                    .filter { dimension ->
                        dimension.dimension ==
                                KnowledgeDimensionCandidateType.NUTRITION
                    }

            if (nutritionDimensions.isEmpty()) {
                skippedWithoutNutritionCount++

                traceSink.record(
                    OFFNutritionReferenceCandidateTrace(
                        sourceProductId =
                            sourceId,
                        productName =
                            productName,
                        normalizedProductIdentities =
                            normalizedProductIdentities,
                        identityAccepted =
                            true,
                        identityRejectionReasons =
                            emptyList(),
                        nutritionAccepted =
                            false,
                        nutritionRejectionReasons =
                            listOf(
                                REJECTION_MISSING_NUTRITION_DIMENSION
                            ),
                        referenceEligible =
                            false,
                        referenceEligibilityRejectionReasons =
                            emptyList(),
                        candidateCreated =
                            false,
                        createdCandidateId =
                            null
                    )
                )

                return@forEach
            }

            if (nutritionDimensions.size > 1) {
                skippedInvalidNutritionPayloadCount++

                traceSink.record(
                    OFFNutritionReferenceCandidateTrace(
                        sourceProductId =
                            sourceId,
                        productName =
                            productName,
                        normalizedProductIdentities =
                            normalizedProductIdentities,
                        identityAccepted =
                            true,
                        identityRejectionReasons =
                            emptyList(),
                        nutritionAccepted =
                            false,
                        nutritionRejectionReasons =
                            listOf(
                                REJECTION_MULTIPLE_NUTRITION_DIMENSIONS
                            ),
                        referenceEligible =
                            false,
                        referenceEligibilityRejectionReasons =
                            emptyList(),
                        candidateCreated =
                            false,
                        createdCandidateId =
                            null
                    )
                )

                return@forEach
            }

            val nutritionDimension =
                nutritionDimensions.single()

            val nutritionParsingResult =
                parseNutritionPayload(
                    dimension =
                        nutritionDimension
                )

            val nutrition =
                nutritionParsingResult.nutrition

            if (nutrition == null) {
                skippedInvalidNutritionPayloadCount++

                traceSink.record(
                    OFFNutritionReferenceCandidateTrace(
                        sourceProductId =
                            sourceId,
                        productName =
                            productName,
                        normalizedProductIdentities =
                            normalizedProductIdentities,
                        identityAccepted =
                            true,
                        identityRejectionReasons =
                            emptyList(),
                        nutritionAccepted =
                            false,
                        nutritionRejectionReasons =
                            listOf(
                                requireNotNull(
                                    nutritionParsingResult.rejectionReason
                                )
                            ),
                        referenceEligible =
                            false,
                        referenceEligibilityRejectionReasons =
                            emptyList(),
                        candidateCreated =
                            false,
                        createdCandidateId =
                            null
                    )
                )

                return@forEach
            }

            val generatedCandidate =
                CanonicalOFFNutritionReferenceCandidate(
                    sourceId =
                        sourceId,
                    canonicalId =
                        canonicalId,
                    aliases =
                        candidate.aliases
                            .normalizeStrings(),
                    matchAliases =
                        candidate.matchAliases
                            .normalizeStrings(),
                    nutrition =
                        nutrition,
                    productName =
                        candidate.metadata
                            .attributes["productName"]
                            ?.normalizeOptionalString(),
                    brand =
                        candidate.metadata
                            .attributes["brand"]
                            ?.normalizeOptionalString(),
                    categories =
                        candidate.metadata
                            .attributes["categories"]
                            ?.normalizeOptionalString(),
                    singleIngredientNutritionAliases =
                        candidate.metadata
                            .attributes[
                            "singleIngredientNutritionAliases"
                        ]
                            .toStringSet(),
                    source =
                        requireNotNull(
                            candidate.metadata.source
                        ) {
                            "Missing source for ${candidate.canonicalId}"
                        },
                    sourceVersion =
                        candidate.metadata.version
                            ?: "1",
                    sourceConfidence =
                        candidate.metadata.confidence
                )

            generated +=
                generatedCandidate

            traceSink.record(
                OFFNutritionReferenceCandidateTrace(
                    sourceProductId =
                        sourceId,
                    productName =
                        productName,
                    normalizedProductIdentities =
                        normalizedProductIdentities,
                    identityAccepted =
                        true,
                    identityRejectionReasons =
                        emptyList(),
                    nutritionAccepted =
                        true,
                    nutritionRejectionReasons =
                        emptyList(),
                    referenceEligible =
                        true,
                    referenceEligibilityRejectionReasons =
                        emptyList(),
                    candidateCreated =
                        true,
                    createdCandidateId =
                        canonicalId
                )
            )
        }

        val sorted =
            generated.sortedWith(
                compareBy<CanonicalOFFNutritionReferenceCandidate>(
                    { it.sourceId },
                    { it.canonicalId }
                )
            )

        return OFFNutritionReferenceCandidateGenerationResult(
            inputCandidateCount =
                candidates.size,
            generatedCandidateCount =
                sorted.size,
            skippedWithoutNutritionCount =
                skippedWithoutNutritionCount,
            skippedInvalidIdentityCount =
                skippedInvalidIdentityCount,
            skippedInvalidNutritionPayloadCount =
                skippedInvalidNutritionPayloadCount,
            candidates =
                sorted
        )
    }

    private fun parseNutritionPayload(
        dimension: KnowledgeDimensionCandidate
    ): NutritionPayloadParsingResult {

        val rawPayload =
            dimension.payload as? Map<*, *>
                ?: return NutritionPayloadParsingResult.rejected(
                    reason =
                        REJECTION_PAYLOAD_NOT_MAP
                )

        if (rawPayload.isEmpty()) {
            return NutritionPayloadParsingResult.rejected(
                reason =
                    REJECTION_EMPTY_NUTRITION_PAYLOAD
            )
        }

        val values =
            mutableMapOf<String, Double>()

        rawPayload.forEach { (rawKey, rawValue) ->

            val key =
                rawKey as? String
                    ?: return NutritionPayloadParsingResult.rejected(
                        reason =
                            REJECTION_NON_STRING_NUTRITION_KEY
                    )

            val value =
                rawValue as? Number
                    ?: return NutritionPayloadParsingResult.rejected(
                        reason =
                            REJECTION_NON_NUMERIC_NUTRITION_VALUE
                    )

            values[key] =
                value.toDouble()
        }

        if (values.isEmpty()) {
            return NutritionPayloadParsingResult.rejected(
                reason =
                    REJECTION_EMPTY_NUTRITION_PAYLOAD
            )
        }

        if (
            values.keys.any { key ->
                key !in SUPPORTED_NUTRITION_KEYS
            }
        ) {
            return NutritionPayloadParsingResult.rejected(
                reason =
                    REJECTION_UNSUPPORTED_NUTRITION_KEY
            )
        }

        if (
            values.values.any { value ->
                !value.isFinite()
            }
        ) {
            return NutritionPayloadParsingResult.rejected(
                reason =
                    REJECTION_NON_FINITE_NUTRITION_VALUE
            )
        }

        return NutritionPayloadParsingResult.accepted(
            nutrition =
                values.toSortedMap()
        )
    }

    private fun buildNormalizedProductIdentities(
        candidate: CanonicalKnowledgeCandidate,
        canonicalId: String?,
        productName: String
    ): List<String> {

        val singleIngredientAliases =
            candidate.metadata
                .attributes["singleIngredientNutritionAliases"]
                .toStringSet()

        return buildList {
            canonicalId?.let(::add)

            add(productName)

            addAll(candidate.aliases)
            addAll(candidate.matchAliases)
            addAll(singleIngredientAliases)
        }
            .asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
            .toList()
            .ifEmpty {
                listOf(
                    UNKNOWN_PRODUCT_IDENTITY
                )
            }
    }

    private fun buildIdentityRejectionReasons(
        sourceId: String?,
        canonicalId: String?
    ): List<String> {

        return buildList {
            if (sourceId == null) {
                add(
                    REJECTION_MISSING_SOURCE_ID
                )
            }

            if (canonicalId == null) {
                add(
                    REJECTION_MISSING_CANONICAL_ID
                )
            }
        }
            .distinct()
            .sorted()
    }

    private fun Set<String>.normalizeStrings(): Set<String> {

        return asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSortedSet()
    }

    private fun String.normalizeOptionalString(): String? {

        return trim()
            .takeIf(String::isNotBlank)
    }

    private fun String?.toStringSet(): Set<String> {

        if (isNullOrBlank()) {
            return emptySet()
        }

        return split("|")
            .asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSortedSet()
    }

    private data class NutritionPayloadParsingResult(
        val nutrition: Map<String, Double>?,
        val rejectionReason: String?
    ) {

        init {
            require(
                (nutrition != null) !=
                        (rejectionReason != null)
            ) {
                "Exactly one of nutrition and rejectionReason must be present."
            }

            require(
                rejectionReason == null ||
                        rejectionReason.isNotBlank()
            )
        }

        companion object {

            fun accepted(
                nutrition: Map<String, Double>
            ): NutritionPayloadParsingResult {

                require(nutrition.isNotEmpty())

                return NutritionPayloadParsingResult(
                    nutrition =
                        nutrition,
                    rejectionReason =
                        null
                )
            }

            fun rejected(
                reason: String
            ): NutritionPayloadParsingResult {

                require(reason.isNotBlank())

                return NutritionPayloadParsingResult(
                    nutrition =
                        null,
                    rejectionReason =
                        reason
                )
            }
        }
    }

    private companion object {

        const val UNKNOWN_SOURCE_PRODUCT_ID =
            "<missing-source-product-id>"

        const val UNKNOWN_PRODUCT_NAME =
            "<missing-product-name>"

        const val UNKNOWN_PRODUCT_IDENTITY =
            "<missing-product-identity>"

        const val REJECTION_MISSING_SOURCE_ID =
            "MISSING_SOURCE_ID"

        const val REJECTION_MISSING_CANONICAL_ID =
            "MISSING_CANONICAL_ID"

        const val REJECTION_MISSING_NUTRITION_DIMENSION =
            "MISSING_NUTRITION_DIMENSION"

        const val REJECTION_MULTIPLE_NUTRITION_DIMENSIONS =
            "MULTIPLE_NUTRITION_DIMENSIONS"

        const val REJECTION_PAYLOAD_NOT_MAP =
            "NUTRITION_PAYLOAD_NOT_MAP"

        const val REJECTION_EMPTY_NUTRITION_PAYLOAD =
            "EMPTY_NUTRITION_PAYLOAD"

        const val REJECTION_NON_STRING_NUTRITION_KEY =
            "NON_STRING_NUTRITION_KEY"

        const val REJECTION_NON_NUMERIC_NUTRITION_VALUE =
            "NON_NUMERIC_NUTRITION_VALUE"

        const val REJECTION_UNSUPPORTED_NUTRITION_KEY =
            "UNSUPPORTED_NUTRITION_KEY"

        const val REJECTION_NON_FINITE_NUTRITION_VALUE =
            "NON_FINITE_NUTRITION_VALUE"

        val SUPPORTED_NUTRITION_KEYS =
            setOf(
                "energyKcalPer100g",
                "fatPer100g",
                "saturatedFatPer100g",
                "carbohydratesPer100g",
                "sugarsPer100g",
                "fiberPer100g",
                "proteinsPer100g",
                "saltPer100g"
            )
    }
}