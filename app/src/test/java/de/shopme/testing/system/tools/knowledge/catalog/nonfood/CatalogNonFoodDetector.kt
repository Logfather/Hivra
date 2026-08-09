package de.shopme.testing.system.tools.knowledge.catalog.nonfood

import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import java.text.Normalizer
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class CatalogNonFoodDetector {

    fun detect(
        entries: List<IndexedCatalogFoodItem>
    ): List<CatalogNonFoodCandidate> {
        require(entries.map { it.sourceIndex }.distinct().size == entries.size) {
            "Catalog entries contain duplicate sourceIndex values."
        }

        return entries
            .asSequence()
            .sortedBy { it.sourceIndex }
            .mapNotNull(::detectCandidate)
            .sortedWith(
                compareByDescending<CatalogNonFoodCandidate> {
                    recommendationRank(it.recommendation)
                }
                    .thenByDescending { it.confidence }
                    .thenBy { it.itemName.lowercase(Locale.ROOT) }
                    .thenBy { it.sourceIndex }
            )
            .toList()
    }

    private fun detectCandidate(
        entry: IndexedCatalogFoodItem
    ): CatalogNonFoodCandidate? {
        val searchableValues = buildSearchableValues(entry)
        val combinedSearchText = searchableValues
            .joinToString(" ")
            .let(::normalizeSearchValue)

        val tokens = tokenize(combinedSearchText)

        if (combinedSearchText.isBlank() || tokens.isEmpty()) {
            return null
        }

        val evidence = mutableListOf<NonFoodEvidence>()

        STRONG_NON_FOOD_PHRASES
            .toSortedMap()
            .forEach { (phrase, reason) ->
                if (containsPhrase(combinedSearchText, phrase)) {
                    evidence += NonFoodEvidence(
                        reason = reason,
                        matchedTerm = phrase,
                        weight = STRONG_PHRASE_WEIGHT,
                        strength = EvidenceStrength.STRONG
                    )
                }
            }

        MEDIUM_NON_FOOD_PHRASES
            .toSortedMap()
            .forEach { (phrase, reason) ->
                if (containsPhrase(combinedSearchText, phrase)) {
                    evidence += NonFoodEvidence(
                        reason = reason,
                        matchedTerm = phrase,
                        weight = MEDIUM_PHRASE_WEIGHT,
                        strength = EvidenceStrength.MEDIUM
                    )
                }
            }

        WEAK_NON_FOOD_PHRASES
            .toSortedMap()
            .forEach { (phrase, reason) ->
                if (containsPhrase(combinedSearchText, phrase)) {
                    evidence += NonFoodEvidence(
                        reason = reason,
                        matchedTerm = phrase,
                        weight = WEAK_PHRASE_WEIGHT,
                        strength = EvidenceStrength.WEAK
                    )
                }
            }

        tokens
            .sorted()
            .forEach { token ->
                STRONG_NON_FOOD_TOKENS[token]?.let { reason ->
                    evidence += NonFoodEvidence(
                        reason = reason,
                        matchedTerm = token,
                        weight = STRONG_TOKEN_WEIGHT,
                        strength = EvidenceStrength.STRONG
                    )
                }

                MEDIUM_NON_FOOD_TOKENS[token]?.let { reason ->
                    evidence += NonFoodEvidence(
                        reason = reason,
                        matchedTerm = token,
                        weight = MEDIUM_TOKEN_WEIGHT,
                        strength = EvidenceStrength.MEDIUM
                    )
                }

                WEAK_NON_FOOD_TOKENS[token]?.let { reason ->
                    evidence += NonFoodEvidence(
                        reason = reason,
                        matchedTerm = token,
                        weight = WEAK_TOKEN_WEIGHT,
                        strength = EvidenceStrength.WEAK
                    )
                }
            }

        entry.item.category
            ?.takeIf(String::isNotBlank)
            ?.let(::normalizeSearchValue)
            ?.let { normalizedCategory ->
                CATEGORY_NON_FOOD_SIGNALS[normalizedCategory]
                    ?.let { signal ->
                        evidence += NonFoodEvidence(
                            reason = signal.reason,
                            matchedTerm = normalizedCategory,
                            weight = signal.weight,
                            strength = signal.strength
                        )
                    }
            }

        if (evidence.isEmpty()) {
            return null
        }

        val foodEvidence = detectFoodEvidence(
            combinedSearchText = combinedSearchText,
            tokens = tokens
        )

        val protectedFoodException = detectProtectedFoodException(
            combinedSearchText = combinedSearchText,
            tokens = tokens
        )

        val groupedEvidence = evidence
            .groupBy {
                EvidenceIdentity(
                    reason = it.reason,
                    matchedTerm = it.matchedTerm
                )
            }
            .map { (_, values) ->
                values.maxBy { it.weight }
            }
            .sortedWith(
                compareByDescending<NonFoodEvidence> { it.weight }
                    .thenBy { it.reason.name }
                    .thenBy { it.matchedTerm }
            )

        val reasons = groupedEvidence
            .mapTo(sortedSetOf(compareBy { it.name })) { it.reason }

        val matchedTerms = groupedEvidence
            .mapTo(sortedSetOf()) { it.matchedTerm }

        val confidence = calculateConfidence(
            evidence = groupedEvidence,
            foodEvidence = foodEvidence,
            protectedFoodException = protectedFoodException
        )

        val recommendation = determineRecommendation(
            evidence = groupedEvidence,
            confidence = confidence,
            foodEvidence = foodEvidence,
            protectedFoodException = protectedFoodException
        )

        if (
            recommendation == NonFoodRecommendation.KEEP &&
            confidence < MINIMUM_RETURN_CONFIDENCE
        ) {
            return null
        }

        return CatalogNonFoodCandidate(
            sourceIndex = entry.sourceIndex,
            itemName = entry.item.itemname,
            category = entry.item.category,
            reasons = reasons,
            matchedTerms = matchedTerms,
            confidence = confidence,
            recommendation = recommendation
        )
    }

    private fun buildSearchableValues(
        entry: IndexedCatalogFoodItem
    ): List<String> =
        buildList {
            add(entry.item.itemname)

            entry.item.category
                ?.takeIf(String::isNotBlank)
                ?.let(::add)

            entry.item.production
                ?.takeIf(String::isNotBlank)
                ?.let(::add)

            entry.item.plural
                ?.takeIf(String::isNotBlank)
                ?.let(::add)

            entry.item.normalized
                ?.takeIf(String::isNotBlank)
                ?.let(::add)

            entry.item.normalizedEnglish
                ?.takeIf(String::isNotBlank)
                ?.let(::add)

            addAll(
                entry.item.colloquial
                    .filter(String::isNotBlank)
            )

            addAll(
                entry.item.autocompleteTokens
                    .filter(String::isNotBlank)
            )
        }
            .distinct()
            .sorted()

    private fun detectFoodEvidence(
        combinedSearchText: String,
        tokens: Set<String>
    ): FoodEvidence {
        val matchedStrongTerms = STRONG_FOOD_PHRASES
            .filter { phrase ->
                containsPhrase(combinedSearchText, phrase)
            }
            .toSortedSet()

        val matchedMediumTerms = MEDIUM_FOOD_TOKENS
            .filter { it in tokens }
            .toSortedSet()

        val matchedWeakTerms = WEAK_FOOD_TOKENS
            .filter { it in tokens }
            .toSortedSet()

        val score =
            matchedStrongTerms.size * STRONG_FOOD_EVIDENCE_WEIGHT +
                    matchedMediumTerms.size * MEDIUM_FOOD_EVIDENCE_WEIGHT +
                    matchedWeakTerms.size * WEAK_FOOD_EVIDENCE_WEIGHT

        return FoodEvidence(
            score = score,
            strongTerms = matchedStrongTerms,
            mediumTerms = matchedMediumTerms,
            weakTerms = matchedWeakTerms
        )
    }

    private fun detectProtectedFoodException(
        combinedSearchText: String,
        tokens: Set<String>
    ): ProtectedFoodException? {
        val matchingPhrase = PROTECTED_FOOD_PHRASES
            .firstOrNull { phrase ->
                containsPhrase(combinedSearchText, phrase)
            }

        if (matchingPhrase != null) {
            return ProtectedFoodException(
                matchedTerm = matchingPhrase,
                strength = EvidenceStrength.STRONG
            )
        }

        val matchingToken = PROTECTED_FOOD_TOKENS
            .firstOrNull { token -> token in tokens }

        if (matchingToken != null) {
            return ProtectedFoodException(
                matchedTerm = matchingToken,
                strength = EvidenceStrength.MEDIUM
            )
        }

        return null
    }

    private fun calculateConfidence(
        evidence: List<NonFoodEvidence>,
        foodEvidence: FoodEvidence,
        protectedFoodException: ProtectedFoodException?
    ): Double {
        if (evidence.isEmpty()) {
            return 0.0
        }

        val uniqueReasonCount = evidence
            .map { it.reason }
            .distinct()
            .size

        val strongEvidenceCount = evidence.count {
            it.strength == EvidenceStrength.STRONG
        }

        val mediumEvidenceCount = evidence.count {
            it.strength == EvidenceStrength.MEDIUM
        }

        val weakEvidenceCount = evidence.count {
            it.strength == EvidenceStrength.WEAK
        }

        val rawEvidenceScore = evidence
            .sumOf { it.weight }
            .coerceAtMost(MAXIMUM_RAW_EVIDENCE_SCORE)

        var confidence =
            BASE_CONFIDENCE +
                    rawEvidenceScore * RAW_EVIDENCE_CONFIDENCE_FACTOR +
                    strongEvidenceCount * STRONG_EVIDENCE_BONUS +
                    mediumEvidenceCount * MEDIUM_EVIDENCE_BONUS +
                    weakEvidenceCount * WEAK_EVIDENCE_BONUS +
                    max(0, uniqueReasonCount - 1) * MULTI_REASON_BONUS

        if (foodEvidence.score > 0) {
            confidence -= min(
                MAXIMUM_FOOD_EVIDENCE_PENALTY,
                foodEvidence.score * FOOD_EVIDENCE_PENALTY_FACTOR
            )
        }

        if (protectedFoodException != null) {
            confidence -= when (protectedFoodException.strength) {
                EvidenceStrength.STRONG ->
                    STRONG_PROTECTED_FOOD_PENALTY

                EvidenceStrength.MEDIUM ->
                    MEDIUM_PROTECTED_FOOD_PENALTY

                EvidenceStrength.WEAK ->
                    WEAK_PROTECTED_FOOD_PENALTY
            }
        }

        if (
            strongEvidenceCount == 0 &&
            mediumEvidenceCount == 0
        ) {
            confidence = min(confidence, WEAK_ONLY_MAXIMUM_CONFIDENCE)
        }

        if (
            evidence.all {
                it.reason == CatalogNonFoodReason.UNKNOWN_NON_FOOD
            }
        ) {
            confidence = min(
                confidence,
                UNKNOWN_ONLY_MAXIMUM_CONFIDENCE
            )
        }

        return confidence.coerceIn(0.0, 1.0)
    }

    private fun determineRecommendation(
        evidence: List<NonFoodEvidence>,
        confidence: Double,
        foodEvidence: FoodEvidence,
        protectedFoodException: ProtectedFoodException?
    ): NonFoodRecommendation {
        val strongEvidenceCount = evidence.count {
            it.strength == EvidenceStrength.STRONG
        }

        val mediumEvidenceCount = evidence.count {
            it.strength == EvidenceStrength.MEDIUM
        }

        val onlySafeAutomaticReasons = evidence
            .map { it.reason }
            .all { it in AUTOMATIC_REMOVAL_REASONS }

        val containsAmbiguousReason = evidence
            .any { it.reason in AMBIGUOUS_REASONS }

        val hasConflictingFoodEvidence =
            foodEvidence.score >= CONFLICTING_FOOD_EVIDENCE_SCORE

        return when {
            protectedFoodException != null ->
                NonFoodRecommendation.REVIEW

            confidence >= REMOVE_AUTOMATICALLY_CONFIDENCE &&
                    strongEvidenceCount >= MINIMUM_AUTOMATIC_STRONG_EVIDENCE_COUNT &&
                    onlySafeAutomaticReasons &&
                    !containsAmbiguousReason &&
                    !hasConflictingFoodEvidence ->
                NonFoodRecommendation.REMOVE_AUTOMATICALLY

            confidence >= REMOVE_AFTER_REVIEW_CONFIDENCE &&
                    strongEvidenceCount + mediumEvidenceCount > 0 ->
                NonFoodRecommendation.REMOVE_AFTER_REVIEW

            confidence >= REVIEW_CONFIDENCE ->
                NonFoodRecommendation.REVIEW

            else ->
                NonFoodRecommendation.KEEP
        }
    }

    private fun containsPhrase(
        normalizedText: String,
        normalizedPhrase: String
    ): Boolean =
        Regex(
            pattern =
                "(?<![\\p{L}\\p{N}])" +
                        Regex.escape(normalizedPhrase) +
                        "(?![\\p{L}\\p{N}])"
        ).containsMatchIn(normalizedText)

    private fun normalizeSearchValue(
        value: String
    ): String =
        transliterateGermanCharacters(
            Normalizer.normalize(
                value,
                Normalizer.Form.NFKD
            )
        )
            .lowercase(Locale.ROOT)
            .replace(AMPERSAND_REGEX, " und ")
            .replace(NON_ALPHANUMERIC_REGEX, " ")
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .trim()

    private fun transliterateGermanCharacters(
        value: String
    ): String =
        value
            .replace("Ä", "Ae")
            .replace("Ö", "Oe")
            .replace("Ü", "Ue")
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("ß", "ss")
            .replace(COMBINING_MARKS_REGEX, "")

    private fun tokenize(
        value: String
    ): Set<String> =
        normalizeSearchValue(value)
            .split(' ')
            .asSequence()
            .map(String::trim)
            .filter { it.length >= MINIMUM_TOKEN_LENGTH }
            .filterNot { it in STOP_TOKENS }
            .toSortedSet()

    private fun recommendationRank(
        recommendation: NonFoodRecommendation
    ): Int =
        when (recommendation) {
            NonFoodRecommendation.REMOVE_AUTOMATICALLY -> 4
            NonFoodRecommendation.REMOVE_AFTER_REVIEW -> 3
            NonFoodRecommendation.REVIEW -> 2
            NonFoodRecommendation.KEEP -> 1
        }

    private data class NonFoodEvidence(
        val reason: CatalogNonFoodReason,
        val matchedTerm: String,
        val weight: Double,
        val strength: EvidenceStrength
    )

    private data class EvidenceIdentity(
        val reason: CatalogNonFoodReason,
        val matchedTerm: String
    )

    private data class FoodEvidence(
        val score: Int,
        val strongTerms: Set<String>,
        val mediumTerms: Set<String>,
        val weakTerms: Set<String>
    )

    private data class ProtectedFoodException(
        val matchedTerm: String,
        val strength: EvidenceStrength
    )

    private data class CategorySignal(
        val reason: CatalogNonFoodReason,
        val weight: Double,
        val strength: EvidenceStrength
    )

    private enum class EvidenceStrength {
        STRONG,
        MEDIUM,
        WEAK
    }

    private companion object {

        const val MINIMUM_TOKEN_LENGTH = 2

        const val STRONG_PHRASE_WEIGHT = 0.55
        const val MEDIUM_PHRASE_WEIGHT = 0.35
        const val WEAK_PHRASE_WEIGHT = 0.18

        const val STRONG_TOKEN_WEIGHT = 0.42
        const val MEDIUM_TOKEN_WEIGHT = 0.27
        const val WEAK_TOKEN_WEIGHT = 0.12

        const val BASE_CONFIDENCE = 0.10
        const val RAW_EVIDENCE_CONFIDENCE_FACTOR = 0.62
        const val STRONG_EVIDENCE_BONUS = 0.08
        const val MEDIUM_EVIDENCE_BONUS = 0.04
        const val WEAK_EVIDENCE_BONUS = 0.01
        const val MULTI_REASON_BONUS = 0.05

        const val MAXIMUM_RAW_EVIDENCE_SCORE = 1.20

        const val STRONG_FOOD_EVIDENCE_WEIGHT = 5
        const val MEDIUM_FOOD_EVIDENCE_WEIGHT = 2
        const val WEAK_FOOD_EVIDENCE_WEIGHT = 1

        const val FOOD_EVIDENCE_PENALTY_FACTOR = 0.055
        const val MAXIMUM_FOOD_EVIDENCE_PENALTY = 0.38

        const val STRONG_PROTECTED_FOOD_PENALTY = 0.58
        const val MEDIUM_PROTECTED_FOOD_PENALTY = 0.38
        const val WEAK_PROTECTED_FOOD_PENALTY = 0.20

        const val WEAK_ONLY_MAXIMUM_CONFIDENCE = 0.49
        const val UNKNOWN_ONLY_MAXIMUM_CONFIDENCE = 0.59

        const val REMOVE_AUTOMATICALLY_CONFIDENCE = 0.95
        const val REMOVE_AFTER_REVIEW_CONFIDENCE = 0.80
        const val REVIEW_CONFIDENCE = 0.50
        const val MINIMUM_RETURN_CONFIDENCE = 0.30

        const val MINIMUM_AUTOMATIC_STRONG_EVIDENCE_COUNT = 1
        const val CONFLICTING_FOOD_EVIDENCE_SCORE = 5

        val AMPERSAND_REGEX = Regex("&")
        val NON_ALPHANUMERIC_REGEX = Regex("[^a-z0-9]+")
        val MULTIPLE_WHITESPACE_REGEX = Regex("\\s+")
        val COMBINING_MARKS_REGEX = Regex("\\p{M}+")

        val STOP_TOKENS = setOf(
            "artikel",
            "aus",
            "bei",
            "das",
            "der",
            "die",
            "ein",
            "eine",
            "fuer",
            "für",
            "im",
            "in",
            "mit",
            "oder",
            "produkt",
            "und",
            "von",
            "zum",
            "zur"
        )

        val AUTOMATIC_REMOVAL_REASONS = setOf(
            CatalogNonFoodReason.HOUSEHOLD,
            CatalogNonFoodReason.CLEANING,
            CatalogNonFoodReason.PERSONAL_CARE,
            CatalogNonFoodReason.KITCHEN_SUPPLY,
            CatalogNonFoodReason.PACKAGING,
            CatalogNonFoodReason.PET_FOOD,
            CatalogNonFoodReason.TOBACCO,
            CatalogNonFoodReason.MEDICINE,
            CatalogNonFoodReason.DECORATION,
            CatalogNonFoodReason.TEXTILE,
            CatalogNonFoodReason.ELECTRONICS
        )

        val AMBIGUOUS_REASONS = setOf(
            CatalogNonFoodReason.SUPPLEMENT,
            CatalogNonFoodReason.UNKNOWN_NON_FOOD
        )

        val CATEGORY_NON_FOOD_SIGNALS = mapOf(
            "babybedarf" to CategorySignal(
                CatalogNonFoodReason.PERSONAL_CARE,
                STRONG_PHRASE_WEIGHT,
                EvidenceStrength.STRONG
            ),
            "drogerie" to CategorySignal(
                CatalogNonFoodReason.PERSONAL_CARE,
                STRONG_PHRASE_WEIGHT,
                EvidenceStrength.STRONG
            ),
            "elektronik" to CategorySignal(
                CatalogNonFoodReason.ELECTRONICS,
                STRONG_PHRASE_WEIGHT,
                EvidenceStrength.STRONG
            ),
            "haushalt" to CategorySignal(
                CatalogNonFoodReason.HOUSEHOLD,
                STRONG_PHRASE_WEIGHT,
                EvidenceStrength.STRONG
            ),
            "haustierbedarf" to CategorySignal(
                CatalogNonFoodReason.PET_FOOD,
                STRONG_PHRASE_WEIGHT,
                EvidenceStrength.STRONG
            ),
            "heimtier" to CategorySignal(
                CatalogNonFoodReason.PET_FOOD,
                STRONG_PHRASE_WEIGHT,
                EvidenceStrength.STRONG
            ),
            "hygiene" to CategorySignal(
                CatalogNonFoodReason.PERSONAL_CARE,
                STRONG_PHRASE_WEIGHT,
                EvidenceStrength.STRONG
            ),
            "kuechenbedarf" to CategorySignal(
                CatalogNonFoodReason.KITCHEN_SUPPLY,
                STRONG_PHRASE_WEIGHT,
                EvidenceStrength.STRONG
            ),
            "medizin" to CategorySignal(
                CatalogNonFoodReason.MEDICINE,
                STRONG_PHRASE_WEIGHT,
                EvidenceStrength.STRONG
            ),
            "reinigung" to CategorySignal(
                CatalogNonFoodReason.CLEANING,
                STRONG_PHRASE_WEIGHT,
                EvidenceStrength.STRONG
            ),
            "tabak" to CategorySignal(
                CatalogNonFoodReason.TOBACCO,
                STRONG_PHRASE_WEIGHT,
                EvidenceStrength.STRONG
            ),
            "textilien" to CategorySignal(
                CatalogNonFoodReason.TEXTILE,
                STRONG_PHRASE_WEIGHT,
                EvidenceStrength.STRONG
            ),
            "verpackung" to CategorySignal(
                CatalogNonFoodReason.PACKAGING,
                STRONG_PHRASE_WEIGHT,
                EvidenceStrength.STRONG
            )
        )

        val STRONG_NON_FOOD_PHRASES = mapOf(
            "abflussreiniger" to CatalogNonFoodReason.CLEANING,
            "aluminiumfolie" to CatalogNonFoodReason.PACKAGING,
            "alufolie" to CatalogNonFoodReason.PACKAGING,
            "antibakterielles spray" to CatalogNonFoodReason.CLEANING,
            "babypuder" to CatalogNonFoodReason.PERSONAL_CARE,
            "backpapier" to CatalogNonFoodReason.PACKAGING,
            "batterie" to CatalogNonFoodReason.ELECTRONICS,
            "batterien" to CatalogNonFoodReason.ELECTRONICS,
            "bleichmittel" to CatalogNonFoodReason.CLEANING,
            "body lotion" to CatalogNonFoodReason.PERSONAL_CARE,
            "bodylotion" to CatalogNonFoodReason.PERSONAL_CARE,
            "damenbinde" to CatalogNonFoodReason.PERSONAL_CARE,
            "damenbinden" to CatalogNonFoodReason.PERSONAL_CARE,
            "deo spray" to CatalogNonFoodReason.PERSONAL_CARE,
            "deodorant" to CatalogNonFoodReason.PERSONAL_CARE,
            "desinfektionsmittel" to CatalogNonFoodReason.CLEANING,
            "duschgel" to CatalogNonFoodReason.PERSONAL_CARE,
            "einweghandschuhe" to CatalogNonFoodReason.HOUSEHOLD,
            "feuchttuecher" to CatalogNonFoodReason.PERSONAL_CARE,
            "feuchttücher" to CatalogNonFoodReason.PERSONAL_CARE,
            "fensterreiniger" to CatalogNonFoodReason.CLEANING,
            "frischhaltefolie" to CatalogNonFoodReason.PACKAGING,
            "fussbodenreiniger" to CatalogNonFoodReason.CLEANING,
            "fußbodenreiniger" to CatalogNonFoodReason.CLEANING,
            "geschirrspuelmittel" to CatalogNonFoodReason.CLEANING,
            "geschirrspülmittel" to CatalogNonFoodReason.CLEANING,
            "gesichtscreme" to CatalogNonFoodReason.PERSONAL_CARE,
            "gesichtsreiniger" to CatalogNonFoodReason.PERSONAL_CARE,
            "glasreiniger" to CatalogNonFoodReason.CLEANING,
            "haarspray" to CatalogNonFoodReason.PERSONAL_CARE,
            "handcreme" to CatalogNonFoodReason.PERSONAL_CARE,
            "handseife" to CatalogNonFoodReason.PERSONAL_CARE,
            "hundefutter" to CatalogNonFoodReason.PET_FOOD,
            "hygienereiniger" to CatalogNonFoodReason.CLEANING,
            "kaffeefilter" to CatalogNonFoodReason.KITCHEN_SUPPLY,
            "katzenfutter" to CatalogNonFoodReason.PET_FOOD,
            "katzenstreu" to CatalogNonFoodReason.PET_FOOD,
            "klarsichtfolie" to CatalogNonFoodReason.PACKAGING,
            "klopapier" to CatalogNonFoodReason.HOUSEHOLD,
            "kondom" to CatalogNonFoodReason.PERSONAL_CARE,
            "kondome" to CatalogNonFoodReason.PERSONAL_CARE,
            "kosmetiktuecher" to CatalogNonFoodReason.PERSONAL_CARE,
            "kosmetiktücher" to CatalogNonFoodReason.PERSONAL_CARE,
            "kuechenrolle" to CatalogNonFoodReason.HOUSEHOLD,
            "küchenrolle" to CatalogNonFoodReason.HOUSEHOLD,
            "lippenpflege" to CatalogNonFoodReason.PERSONAL_CARE,
            "maschinenreiniger" to CatalogNonFoodReason.CLEANING,
            "medikament" to CatalogNonFoodReason.MEDICINE,
            "medikamente" to CatalogNonFoodReason.MEDICINE,
            "muellbeutel" to CatalogNonFoodReason.HOUSEHOLD,
            "müllbeutel" to CatalogNonFoodReason.HOUSEHOLD,
            "nagellack" to CatalogNonFoodReason.PERSONAL_CARE,
            "nasenspray" to CatalogNonFoodReason.MEDICINE,
            "ofenspray" to CatalogNonFoodReason.CLEANING,
            "pflaster" to CatalogNonFoodReason.MEDICINE,
            "rasierer" to CatalogNonFoodReason.PERSONAL_CARE,
            "rasiergel" to CatalogNonFoodReason.PERSONAL_CARE,
            "rasierklingen" to CatalogNonFoodReason.PERSONAL_CARE,
            "reinigungsspray" to CatalogNonFoodReason.CLEANING,
            "scheuermilch" to CatalogNonFoodReason.CLEANING,
            "schuhcreme" to CatalogNonFoodReason.HOUSEHOLD,
            "serviette" to CatalogNonFoodReason.HOUSEHOLD,
            "servietten" to CatalogNonFoodReason.HOUSEHOLD,
            "shampoo" to CatalogNonFoodReason.PERSONAL_CARE,
            "spuelmaschinentabs" to CatalogNonFoodReason.CLEANING,
            "spülmaschinentabs" to CatalogNonFoodReason.CLEANING,
            "spuelmittel" to CatalogNonFoodReason.CLEANING,
            "spülmittel" to CatalogNonFoodReason.CLEANING,
            "tampon" to CatalogNonFoodReason.PERSONAL_CARE,
            "tampons" to CatalogNonFoodReason.PERSONAL_CARE,
            "taschentuecher" to CatalogNonFoodReason.PERSONAL_CARE,
            "taschentücher" to CatalogNonFoodReason.PERSONAL_CARE,
            "teelicht" to CatalogNonFoodReason.DECORATION,
            "teelichter" to CatalogNonFoodReason.DECORATION,
            "toilettenpapier" to CatalogNonFoodReason.HOUSEHOLD,
            "toilettenreiniger" to CatalogNonFoodReason.CLEANING,
            "vollwaschmittel" to CatalogNonFoodReason.CLEANING,
            "waschmittel" to CatalogNonFoodReason.CLEANING,
            "weichspueler" to CatalogNonFoodReason.CLEANING,
            "weichspüler" to CatalogNonFoodReason.CLEANING,
            "wundsalbe" to CatalogNonFoodReason.MEDICINE,
            "zahnbuerste" to CatalogNonFoodReason.PERSONAL_CARE,
            "zahnbürste" to CatalogNonFoodReason.PERSONAL_CARE,
            "zahncreme" to CatalogNonFoodReason.PERSONAL_CARE,
            "zahnseide" to CatalogNonFoodReason.PERSONAL_CARE,
            "zahnpasta" to CatalogNonFoodReason.PERSONAL_CARE,
            "zigarette" to CatalogNonFoodReason.TOBACCO,
            "zigaretten" to CatalogNonFoodReason.TOBACCO,
            "zip beutel" to CatalogNonFoodReason.PACKAGING
        )
            .mapKeys { (key, _) -> normalizeStaticValue(key) }

        val MEDIUM_NON_FOOD_PHRASES = mapOf(
            "badreiniger" to CatalogNonFoodReason.CLEANING,
            "baumwolltuch" to CatalogNonFoodReason.HOUSEHOLD,
            "blumenerde" to CatalogNonFoodReason.DECORATION,
            "duftkerze" to CatalogNonFoodReason.DECORATION,
            "einwegbesteck" to CatalogNonFoodReason.KITCHEN_SUPPLY,
            "einwegbecher" to CatalogNonFoodReason.KITCHEN_SUPPLY,
            "einweggeschirr" to CatalogNonFoodReason.KITCHEN_SUPPLY,
            "fischfutter" to CatalogNonFoodReason.PET_FOOD,
            "fusscreme" to CatalogNonFoodReason.PERSONAL_CARE,
            "fußcreme" to CatalogNonFoodReason.PERSONAL_CARE,
            "geschenkpapier" to CatalogNonFoodReason.PACKAGING,
            "grillanzuender" to CatalogNonFoodReason.HOUSEHOLD,
            "grillanzünder" to CatalogNonFoodReason.HOUSEHOLD,
            "handtuch" to CatalogNonFoodReason.TEXTILE,
            "haushaltshandschuhe" to CatalogNonFoodReason.HOUSEHOLD,
            "holzbesteck" to CatalogNonFoodReason.KITCHEN_SUPPLY,
            "hundesnack" to CatalogNonFoodReason.PET_FOOD,
            "katzen snack" to CatalogNonFoodReason.PET_FOOD,
            "kerze" to CatalogNonFoodReason.DECORATION,
            "kerzen" to CatalogNonFoodReason.DECORATION,
            "kuechenschwamm" to CatalogNonFoodReason.CLEANING,
            "küchenschwamm" to CatalogNonFoodReason.CLEANING,
            "luftreiniger" to CatalogNonFoodReason.CLEANING,
            "moebelpolitur" to CatalogNonFoodReason.CLEANING,
            "möbelpolitur" to CatalogNonFoodReason.CLEANING,
            "mottenschutz" to CatalogNonFoodReason.HOUSEHOLD,
            "mueckenspray" to CatalogNonFoodReason.HOUSEHOLD,
            "mückenspray" to CatalogNonFoodReason.HOUSEHOLD,
            "papierbecher" to CatalogNonFoodReason.KITCHEN_SUPPLY,
            "pappteller" to CatalogNonFoodReason.KITCHEN_SUPPLY,
            "spuelschwamm" to CatalogNonFoodReason.CLEANING,
            "spülschwamm" to CatalogNonFoodReason.CLEANING,
            "staubtuch" to CatalogNonFoodReason.CLEANING,
            "tierstreu" to CatalogNonFoodReason.PET_FOOD,
            "vogelfutter" to CatalogNonFoodReason.PET_FOOD,
            "wattepad" to CatalogNonFoodReason.PERSONAL_CARE,
            "wattepads" to CatalogNonFoodReason.PERSONAL_CARE
        )
            .mapKeys { (key, _) -> normalizeStaticValue(key) }

        val WEAK_NON_FOOD_PHRASES = mapOf(
            "aroma diffuser" to CatalogNonFoodReason.DECORATION,
            "beauty" to CatalogNonFoodReason.PERSONAL_CARE,
            "pflegeprodukt" to CatalogNonFoodReason.PERSONAL_CARE,
            "reinigungsartikel" to CatalogNonFoodReason.CLEANING,
            "tierbedarf" to CatalogNonFoodReason.PET_FOOD,
            "wellness" to CatalogNonFoodReason.PERSONAL_CARE
        )
            .mapKeys { (key, _) -> normalizeStaticValue(key) }

        val STRONG_NON_FOOD_TOKENS = mapOf(
            "bleichmittel" to CatalogNonFoodReason.CLEANING,
            "deodorant" to CatalogNonFoodReason.PERSONAL_CARE,
            "duschgel" to CatalogNonFoodReason.PERSONAL_CARE,
            "haarspray" to CatalogNonFoodReason.PERSONAL_CARE,
            "katzenstreu" to CatalogNonFoodReason.PET_FOOD,
            "klopapier" to CatalogNonFoodReason.HOUSEHOLD,
            "kondome" to CatalogNonFoodReason.PERSONAL_CARE,
            "nagellack" to CatalogNonFoodReason.PERSONAL_CARE,
            "rasierer" to CatalogNonFoodReason.PERSONAL_CARE,
            "shampoo" to CatalogNonFoodReason.PERSONAL_CARE,
            "tampons" to CatalogNonFoodReason.PERSONAL_CARE,
            "waschmittel" to CatalogNonFoodReason.CLEANING,
            "zahnpasta" to CatalogNonFoodReason.PERSONAL_CARE,
            "zigaretten" to CatalogNonFoodReason.TOBACCO
        )

        val MEDIUM_NON_FOOD_TOKENS = mapOf(
            "becher" to CatalogNonFoodReason.KITCHEN_SUPPLY,
            "besteck" to CatalogNonFoodReason.KITCHEN_SUPPLY,
            "binden" to CatalogNonFoodReason.PERSONAL_CARE,
            "folie" to CatalogNonFoodReason.PACKAGING,
            "handschuhe" to CatalogNonFoodReason.HOUSEHOLD,
            "kerze" to CatalogNonFoodReason.DECORATION,
            "reiniger" to CatalogNonFoodReason.CLEANING,
            "schwamm" to CatalogNonFoodReason.CLEANING,
            "seife" to CatalogNonFoodReason.PERSONAL_CARE,
            "teller" to CatalogNonFoodReason.KITCHEN_SUPPLY,
            "toilettenpapier" to CatalogNonFoodReason.HOUSEHOLD,
            "tuch" to CatalogNonFoodReason.HOUSEHOLD,
            "tuecher" to CatalogNonFoodReason.HOUSEHOLD,
            "tücher" to CatalogNonFoodReason.HOUSEHOLD
        )

        val WEAK_NON_FOOD_TOKENS = mapOf(
            "accessoire" to CatalogNonFoodReason.UNKNOWN_NON_FOOD,
            "hygiene" to CatalogNonFoodReason.PERSONAL_CARE,
            "pflege" to CatalogNonFoodReason.PERSONAL_CARE,
            "reinigung" to CatalogNonFoodReason.CLEANING,
            "zubehoer" to CatalogNonFoodReason.UNKNOWN_NON_FOOD,
            "zubehör" to CatalogNonFoodReason.UNKNOWN_NON_FOOD
        )

        val STRONG_FOOD_PHRASES = setOf(
            "algen",
            "algen salat",
            "backhefe",
            "backpapier ersatz",
            "esspapier",
            "fisch schwamm",
            "gummibaerchen",
            "gummibärchen",
            "kaese creme",
            "käse creme",
            "kuchen dekoration",
            "speisegelatine",
            "speiseoel",
            "speiseöl",
            "zucker dekor"
        )
            .mapTo(sortedSetOf()) { normalizeStaticValue(it) }

        val MEDIUM_FOOD_TOKENS = setOf(
            "apfel",
            "banane",
            "beere",
            "brot",
            "butter",
            "ei",
            "eier",
            "essig",
            "fisch",
            "fleisch",
            "frucht",
            "gemuese",
            "gemüse",
            "getraenk",
            "getränk",
            "gewuerz",
            "gewürz",
            "joghurt",
            "kaese",
            "käse",
            "kartoffel",
            "kraeuter",
            "kräuter",
            "milch",
            "nudel",
            "obst",
            "oel",
            "öl",
            "pilz",
            "reis",
            "salat",
            "sauce",
            "suppe",
            "tee",
            "wasser",
            "wurst"
        )
            .mapTo(sortedSetOf()) { normalizeStaticValue(it) }

        val WEAK_FOOD_TOKENS = setOf(
            "bio",
            "gekocht",
            "getrocknet",
            "frisch",
            "gefroren",
            "geraeuchert",
            "geräuchert",
            "roh",
            "vegan",
            "vegetarisch"
        )
            .mapTo(sortedSetOf()) { normalizeStaticValue(it) }

        val PROTECTED_FOOD_PHRASES = setOf(
            "backpapier duenn",
            "backpapier dünn",
            "esspapier",
            "kandierter ingwer",
            "katzengras",
            "kuechenkräuter",
            "küchenkraeuter",
            "küchenkräuter",
            "papier duenn",
            "papier dünn",
            "schwammkuchen",
            "seifen kraut",
            "seifenkraut",
            "zuckerwatte"
        )
            .mapTo(sortedSetOf()) { normalizeStaticValue(it) }

        val PROTECTED_FOOD_TOKENS = setOf(
            "algen",
            "esspapier",
            "katzengras",
            "schwammkuchen",
            "seifenkraut",
            "zuckerwatte"
        )
            .mapTo(sortedSetOf()) { normalizeStaticValue(it) }

        fun normalizeStaticValue(
            value: String
        ): String =
            Normalizer.normalize(
                value,
                Normalizer.Form.NFKD
            )
                .replace("Ä", "Ae")
                .replace("Ö", "Oe")
                .replace("Ü", "Ue")
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replace("ß", "ss")
                .replace(COMBINING_MARKS_REGEX, "")
                .lowercase(Locale.ROOT)
                .replace(AMPERSAND_REGEX, " und ")
                .replace(NON_ALPHANUMERIC_REGEX, " ")
                .replace(MULTIPLE_WHITESPACE_REGEX, " ")
                .trim()
    }
}