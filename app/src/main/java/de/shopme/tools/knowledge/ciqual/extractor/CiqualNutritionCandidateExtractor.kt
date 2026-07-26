package de.shopme.tools.knowledge.ciqual.extractor

import de.shopme.tools.knowledge.ciqual.CiqualNutritionSource
import de.shopme.tools.knowledge.ciqual.model.CiqualNutritionRecord
import de.shopme.tools.knowledge.ciqual.model.CiqualSourceFiles
import de.shopme.tools.knowledge.ki_candidates.CandidateMetadata
import de.shopme.tools.knowledge.ki_candidates.CanonicalKnowledgeCandidate
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidate
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidateType
import java.text.Normalizer

class CiqualNutritionCandidateExtractor(
    private val source: CiqualNutritionSource =
        CiqualNutritionSource()
) {

    private companion object {

        const val SOURCE_NAME =
            "ciqual"

        const val SOURCE_VERSION =
            "2025-11-03"

        val NON_ALPHANUMERIC_REGEX =
            Regex("[^a-z0-9]+")

        val WHITESPACE_REGEX =
            Regex("\\s+")
    }

    fun extract(
        files: CiqualSourceFiles
    ): List<CanonicalKnowledgeCandidate> {
        val candidates =
            mutableListOf<CanonicalKnowledgeCandidate>()

        forEachCandidate(files) { candidate ->
            candidates += candidate
        }

        return candidates
    }

    fun forEachCandidate(
        files: CiqualSourceFiles,
        consumer: (CanonicalKnowledgeCandidate) -> Unit
    ) {
        source.forEachRecord(files) { record ->
            toCandidate(record)
                ?.let(consumer)
        }
    }

    internal fun toCandidate(
        record: CiqualNutritionRecord
    ): CanonicalKnowledgeCandidate? {
        val englishName =
            record.englishName
                ?.cleanDisplayName()
                ?.takeIf { it.isNotBlank() }

        val frenchName =
            record.frenchName
                .cleanDisplayName()
                .takeIf { it.isNotBlank() }
                ?: return null

        val canonicalId =
            normalizeIdentity(
                englishName ?: frenchName
            )
                .takeIf { it.isNotBlank() }
                ?: return null

        val aliases =
            listOfNotNull(
                englishName,
                frenchName
            )
                .map(::normalizeAlias)
                .filter { it.isNotBlank() }
                .toSortedSet()

        if (aliases.isEmpty()) {
            return null
        }

        val nutritionPayload =
            createNutritionPayload(record)
                .takeIf { it.isNotEmpty() }
                ?: return null

        return CanonicalKnowledgeCandidate(
            canonicalId = canonicalId,
            aliases = aliases,
            matchAliases = aliases,
            dimensions =
                listOf(
                    KnowledgeDimensionCandidate(
                        dimension =
                            KnowledgeDimensionCandidateType.NUTRITION,
                        payload = nutritionPayload
                    )
                ),
            metadata =
                CandidateMetadata(
                    source = SOURCE_NAME,
                    sourceId = record.foodCode,
                    confidence = 1.0,
                    version = SOURCE_VERSION,
                    attributes =
                        buildMap {
                            put(
                                "frenchName",
                                frenchName
                            )

                            englishName?.let {
                                put(
                                    "englishName",
                                    it
                                )
                            }

                            record.groupCode
                                ?.trim()
                                ?.takeIf { it.isNotBlank() }
                                ?.let {
                                    put(
                                        "groupCode",
                                        it
                                    )
                                }
                        }
                )
        )
    }

    private fun createNutritionPayload(
        record: CiqualNutritionRecord
    ): Map<String, Double> {
        val nutrition =
            record.nutrition

        return buildMap {
            nutrition.energyKcalPer100g
                ?.takeIfValid(
                    minimum = 0.0,
                    maximum = 950.0
                )
                ?.let {
                    put(
                        "energyKcalPer100g",
                        it
                    )
                }

            nutrition.fatPer100g
                ?.takeIfValid()
                ?.let {
                    put(
                        "fatPer100g",
                        it
                    )
                }

            nutrition.saturatedFatPer100g
                ?.takeIfValid()
                ?.let {
                    put(
                        "saturatedFatPer100g",
                        it
                    )
                }

            nutrition.carbohydratesPer100g
                ?.takeIfValid()
                ?.let {
                    put(
                        "carbohydratesPer100g",
                        it
                    )
                }

            nutrition.sugarsPer100g
                ?.takeIfValid()
                ?.let {
                    put(
                        "sugarsPer100g",
                        it
                    )
                }

            nutrition.fiberPer100g
                ?.takeIfValid()
                ?.let {
                    put(
                        "fiberPer100g",
                        it
                    )
                }

            nutrition.proteinsPer100g
                ?.takeIfValid()
                ?.let {
                    put(
                        "proteinsPer100g",
                        it
                    )
                }

            nutrition.saltPer100g
                ?.takeIfValid()
                ?.let {
                    put(
                        "saltPer100g",
                        it
                    )
                }
        }
    }

    private fun Double.takeIfValid(
        minimum: Double = 0.0,
        maximum: Double = 100.0
    ): Double? =
        takeIf {
            isFinite() &&
                    this >= minimum &&
                    this <= maximum
        }

    private fun String.cleanDisplayName(): String =
        replace(
            WHITESPACE_REGEX,
            " "
        )
            .trim()

    private fun normalizeIdentity(
        value: String
    ): String =
        normalizeAlias(value)

    private fun normalizeAlias(
        value: String
    ): String =
        Normalizer
            .normalize(
                value,
                Normalizer.Form.NFD
            )
            .replace(
                Regex("\\p{M}+"),
                ""
            )
            .lowercase()
            .replace(
                NON_ALPHANUMERIC_REGEX,
                " "
            )
            .replace(
                WHITESPACE_REGEX,
                " "
            )
            .trim()
}