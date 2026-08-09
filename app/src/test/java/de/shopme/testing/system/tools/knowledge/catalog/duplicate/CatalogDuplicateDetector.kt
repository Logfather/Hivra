package de.shopme.testing.system.tools.knowledge.catalog.duplicate

import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogNormalizationResult
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import kotlin.math.max

class CatalogDuplicateDetector {

    fun detect(
        entries: List<IndexedCatalogFoodItem>,
        normalizations: List<CatalogNormalizationResult>
    ): List<CatalogDuplicateGroup> {
        require(entries.map { it.sourceIndex }.distinct().size == entries.size) {
            "Catalog entries contain duplicate sourceIndex values."
        }

        require(normalizations.map { it.sourceIndex }.distinct().size == normalizations.size) {
            "Catalog normalizations contain duplicate sourceIndex values."
        }

        val entriesBySourceIndex = entries.associateBy { it.sourceIndex }
        val normalizationsBySourceIndex = normalizations.associateBy { it.sourceIndex }

        val missingNormalizationIndices = entriesBySourceIndex.keys
            .minus(normalizationsBySourceIndex.keys)
            .sorted()

        require(missingNormalizationIndices.isEmpty()) {
            "Missing catalog normalizations for source indices: " +
                    missingNormalizationIndices.joinToString(", ")
        }

        val unexpectedNormalizationIndices = normalizationsBySourceIndex.keys
            .minus(entriesBySourceIndex.keys)
            .sorted()

        require(unexpectedNormalizationIndices.isEmpty()) {
            "Catalog normalizations reference unknown source indices: " +
                    unexpectedNormalizationIndices.joinToString(", ")
        }

        if (entries.size < 2) {
            return emptyList()
        }

        val records = entries
            .map { entry ->
                val normalization = normalizationsBySourceIndex.getValue(entry.sourceIndex)

                DuplicateDetectionRecord(
                    sourceIndex = entry.sourceIndex,
                    itemName = entry.item.itemname,
                    plural = entry.item.plural,
                    originalNormalizedKey = entry.item.normalized,
                    normalizedKey = normalization.computedNormalizedKey,
                    canonicalName = normalization.computedCanonicalName,
                    exactNameKey = normalizeExactName(
                        entry.item.itemname
                    ),
                    semanticNameKey = normalizeSemanticName(
                        normalization.computedCanonicalName
                    ),
                    sortedTokenKey = sortedTokenKey(
                        normalization.computedCanonicalName
                    ),
                    singularPluralKeys = buildSingularPluralKeys(
                        itemName = normalization.computedCanonicalName,
                        plural = entry.item.plural
                    ),
                    normalizationChangeCount =
                        normalization.changes.size,
                    alreadyCanonical =
                        entry.item.itemname ==
                                normalization.computedCanonicalName &&
                                entry.item.normalized ==
                                normalization.computedNormalizedKey &&
                                normalization.changes.isEmpty()
                )
            }
            .sortedWith(
                compareBy<DuplicateDetectionRecord>(
                    { it.normalizedKey },
                    { it.semanticNameKey },
                    { it.sourceIndex }
                )
            )

        val unionFind = UnionFind(records.map { it.sourceIndex })

        val pairEvaluations = linkedMapOf<SourceIndexPair, PairEvaluation>()

        addExactNormalizedKeyMatches(
            records = records,
            unionFind = unionFind,
            pairEvaluations = pairEvaluations
        )

        addExactItemNameMatches(
            records = records,
            unionFind = unionFind,
            pairEvaluations = pairEvaluations
        )

        addNormalizedNameMatches(
            records = records,
            unionFind = unionFind,
            pairEvaluations = pairEvaluations
        )

        addWordOrderMatches(
            records = records,
            unionFind = unionFind,
            pairEvaluations = pairEvaluations
        )

        addSingularPluralMatches(
            records = records,
            unionFind = unionFind,
            pairEvaluations = pairEvaluations
        )

        addCandidateSimilarityMatches(
            records = records,
            unionFind = unionFind,
            pairEvaluations = pairEvaluations
        )

        val recordsBySourceIndex = records.associateBy { it.sourceIndex }

        return records
            .groupBy { unionFind.find(it.sourceIndex) }
            .values
            .asSequence()
            .filter { groupRecords -> groupRecords.size > 1 }
            .map { groupRecords ->
                createDuplicateGroup(
                    records = groupRecords,
                    recordsBySourceIndex = recordsBySourceIndex,
                    pairEvaluations = pairEvaluations
                )
            }
            .sortedWith(
                compareByDescending<CatalogDuplicateGroup> { it.confidence }
                    .thenBy {
                        it.canonicalCandidateName.lowercase(Locale.ROOT)
                    }
                    .thenBy { it.groupId }
            )
            .toList()
    }

    private fun addExactNormalizedKeyMatches(
        records: List<DuplicateDetectionRecord>,
        unionFind: UnionFind,
        pairEvaluations: MutableMap<SourceIndexPair, PairEvaluation>
    ) {
        records
            .filter { it.normalizedKey.isNotBlank() }
            .groupBy { it.normalizedKey }
            .values
            .filter { it.size > 1 }
            .forEach { matchingRecords ->
                connectAll(
                    records = matchingRecords,
                    reason = CatalogDuplicateReason.IDENTICAL_NORMALIZED_KEY,
                    score = SCORE_IDENTICAL_NORMALIZED_KEY,
                    unionFind = unionFind,
                    pairEvaluations = pairEvaluations
                )
            }
    }

    private fun addExactItemNameMatches(
        records: List<DuplicateDetectionRecord>,
        unionFind: UnionFind,
        pairEvaluations: MutableMap<SourceIndexPair, PairEvaluation>
    ) {
        records
            .filter { it.exactNameKey.isNotBlank() }
            .groupBy { it.exactNameKey }
            .values
            .filter { it.size > 1 }
            .forEach { matchingRecords ->
                connectAll(
                    records = matchingRecords,
                    reason = CatalogDuplicateReason.IDENTICAL_ITEM_NAME,
                    score = SCORE_IDENTICAL_ITEM_NAME,
                    unionFind = unionFind,
                    pairEvaluations = pairEvaluations
                )
            }
    }

    private fun addNormalizedNameMatches(
        records: List<DuplicateDetectionRecord>,
        unionFind: UnionFind,
        pairEvaluations: MutableMap<SourceIndexPair, PairEvaluation>
    ) {
        records
            .filter { it.semanticNameKey.isNotBlank() }
            .groupBy { it.semanticNameKey }
            .values
            .filter { it.size > 1 }
            .forEach { matchingRecords ->
                matchingRecords
                    .sortedBy { it.sourceIndex }
                    .forEachPair { first, second ->
                        val reasons = linkedSetOf(
                            CatalogDuplicateReason.NORMALIZED_NAME_MATCH
                        )

                        if (hasPunctuationDifference(first.itemName, second.itemName)) {
                            reasons += CatalogDuplicateReason.PUNCTUATION_VARIANT
                        }

                        if (isColorOrderVariant(first, second)) {
                            reasons += CatalogDuplicateReason.COLOR_ORDER_VARIANT
                        }

                        connect(
                            first = first,
                            second = second,
                            reasons = reasons,
                            score = SCORE_NORMALIZED_NAME_MATCH,
                            unionFind = unionFind,
                            pairEvaluations = pairEvaluations
                        )
                    }
            }
    }

    private fun addWordOrderMatches(
        records: List<DuplicateDetectionRecord>,
        unionFind: UnionFind,
        pairEvaluations: MutableMap<SourceIndexPair, PairEvaluation>
    ) {
        records
            .filter {
                it.sortedTokenKey.isNotBlank() &&
                        tokenSet(it.canonicalName).size >= MINIMUM_WORD_ORDER_TOKEN_COUNT
            }
            .groupBy { it.sortedTokenKey }
            .values
            .filter { it.size > 1 }
            .forEach { matchingRecords ->
                matchingRecords
                    .sortedBy { it.sourceIndex }
                    .forEachPair { first, second ->
                        if (first.semanticNameKey == second.semanticNameKey) {
                            return@forEachPair
                        }

                        val reasons = linkedSetOf(
                            CatalogDuplicateReason.WORD_ORDER_VARIANT
                        )

                        if (isColorOrderVariant(first, second)) {
                            reasons += CatalogDuplicateReason.COLOR_ORDER_VARIANT
                        }

                        connect(
                            first = first,
                            second = second,
                            reasons = reasons,
                            score = SCORE_WORD_ORDER_VARIANT,
                            unionFind = unionFind,
                            pairEvaluations = pairEvaluations
                        )
                    }
            }
    }

    private fun addSingularPluralMatches(
        records: List<DuplicateDetectionRecord>,
        unionFind: UnionFind,
        pairEvaluations: MutableMap<SourceIndexPair, PairEvaluation>
    ) {
        val recordsBySingularPluralKey = linkedMapOf<String, MutableList<DuplicateDetectionRecord>>()

        records.forEach { record ->
            record.singularPluralKeys
                .filter { it.isNotBlank() }
                .sorted()
                .forEach { key ->
                    recordsBySingularPluralKey
                        .getOrPut(key) { mutableListOf() }
                        .add(record)
                }
        }

        recordsBySingularPluralKey
            .toSortedMap()
            .values
            .filter { it.map { record -> record.sourceIndex }.distinct().size > 1 }
            .forEach { matchingRecords ->
                matchingRecords
                    .distinctBy { it.sourceIndex }
                    .sortedBy { it.sourceIndex }
                    .forEachPair { first, second ->
                        if (!isSingularPluralPair(first, second)) {
                            return@forEachPair
                        }

                        connect(
                            first = first,
                            second = second,
                            reasons = setOf(
                                CatalogDuplicateReason.SINGULAR_PLURAL_VARIANT
                            ),
                            score = SCORE_SINGULAR_PLURAL_VARIANT,
                            unionFind = unionFind,
                            pairEvaluations = pairEvaluations
                        )
                    }
            }
    }

    private fun addCandidateSimilarityMatches(
        records: List<DuplicateDetectionRecord>,
        unionFind: UnionFind,
        pairEvaluations: MutableMap<SourceIndexPair, PairEvaluation>
    ) {
        val recordsByBlockingKey = records
            .filter { it.semanticNameKey.isNotBlank() }
            .groupBy { blockingKey(it.semanticNameKey) }
            .toSortedMap()

        recordsByBlockingKey.values.forEach { block ->
            val sortedBlock = block.sortedBy { it.sourceIndex }

            sortedBlock.forEachPair { first, second ->
                val pair = SourceIndexPair.of(
                    first.sourceIndex,
                    second.sourceIndex
                )

                if (pairEvaluations.containsKey(pair)) {
                    return@forEachPair
                }

                val evaluation = evaluateSimilarity(first, second)

                if (evaluation.score < MINIMUM_SIMILARITY_SCORE) {
                    return@forEachPair
                }

                connect(
                    first = first,
                    second = second,
                    reasons = evaluation.reasons,
                    score = evaluation.score,
                    unionFind = unionFind,
                    pairEvaluations = pairEvaluations
                )
            }
        }
    }

    private fun evaluateSimilarity(
        first: DuplicateDetectionRecord,
        second: DuplicateDetectionRecord
    ): SimilarityEvaluation {
        val firstTokens = tokenSet(first.canonicalName)
        val secondTokens = tokenSet(second.canonicalName)

        if (firstTokens.isEmpty() || secondTokens.isEmpty()) {
            return SimilarityEvaluation(
                score = 0.0,
                reasons = emptySet()
            )
        }

        if (containsProtectedSemanticDifference(firstTokens, secondTokens)) {
            return SimilarityEvaluation(
                score = 0.0,
                reasons = emptySet()
            )
        }

        if (
            containsConflictingPreparationState(
                firstTokens = firstTokens,
                secondTokens = secondTokens
            )
        ) {
            return SimilarityEvaluation(
                score = 0.0,
                reasons = emptySet()
            )
        }

        val jaccardSimilarity = jaccard(firstTokens, secondTokens)
        val editSimilarity = normalizedEditSimilarity(
            first.semanticNameKey,
            second.semanticNameKey
        )

        val containmentSimilarity = tokenContainment(
            firstTokens,
            secondTokens
        )

        val weightedScore = (
                jaccardSimilarity * JACCARD_WEIGHT +
                        editSimilarity * EDIT_DISTANCE_WEIGHT +
                        containmentSimilarity * TOKEN_CONTAINMENT_WEIGHT
                ).coerceIn(0.0, 1.0)

        val reasons = linkedSetOf<CatalogDuplicateReason>()

        if (
            weightedScore >= POSSIBLE_SEMANTIC_DUPLICATE_THRESHOLD &&
            jaccardSimilarity >= MINIMUM_SEMANTIC_JACCARD
        ) {
            reasons += CatalogDuplicateReason.POSSIBLE_SEMANTIC_DUPLICATE
        }

        if (
            isLikelyTypoVariant(first.semanticNameKey, second.semanticNameKey) &&
            jaccardSimilarity >= MINIMUM_TYPO_JACCARD
        ) {
            reasons += CatalogDuplicateReason.TYPO_VARIANT
        }

        if (isSalesFormVariant(firstTokens, secondTokens)) {
            reasons += CatalogDuplicateReason.SALES_FORM_VARIANT
        }

        if (isBrandVariant(firstTokens, secondTokens)) {
            reasons += CatalogDuplicateReason.BRAND_VARIANT
        }

        if (reasons.isEmpty()) {
            return SimilarityEvaluation(
                score = 0.0,
                reasons = emptySet()
            )
        }

        val adjustedScore = when {
            CatalogDuplicateReason.BRAND_VARIANT in reasons ->
                max(weightedScore, SCORE_BRAND_VARIANT)

            CatalogDuplicateReason.SALES_FORM_VARIANT in reasons ->
                max(weightedScore, SCORE_SALES_FORM_VARIANT)

            CatalogDuplicateReason.TYPO_VARIANT in reasons ->
                max(weightedScore, SCORE_TYPO_VARIANT)

            else -> weightedScore
        }

        return SimilarityEvaluation(
            score = adjustedScore.coerceIn(0.0, 1.0),
            reasons = reasons
        )
    }

    private fun createDuplicateGroup(
        records: List<DuplicateDetectionRecord>,
        recordsBySourceIndex: Map<Int, DuplicateDetectionRecord>,
        pairEvaluations: Map<SourceIndexPair, PairEvaluation>
    ): CatalogDuplicateGroup {
        val sortedRecords = records.sortedWith(
            compareBy<DuplicateDetectionRecord>(
                /*
                 * Ein bereits kanonischer Datensatz ist immer das bevorzugte
                 * Gruppenziel.
                 */
                { if (it.alreadyCanonical) 0 else 1 },

                /*
                 * Danach gewinnt der Eintrag mit den wenigsten notwendigen
                 * Normalisierungsänderungen.
                 */
                { it.normalizationChangeCount },

                /*
                 * Erst anschließend greifen fachliche Nachteile wie Marke,
                 * Verkaufsform, Packungsgröße oder Produktnummer.
                 */
                { canonicalCandidateRank(it) },

                { it.canonicalName.length },
                { it.canonicalName.lowercase(Locale.ROOT) },
                { it.sourceIndex }
            )
        )

        val canonicalCandidate = sortedRecords.first()

        val memberCandidates = sortedRecords
            .map { record ->
                val evaluation = if (record.sourceIndex == canonicalCandidate.sourceIndex) {
                    aggregateEvaluationForRecord(
                        sourceIndex = record.sourceIndex,
                        groupSourceIndices = sortedRecords.map { it.sourceIndex }.toSet(),
                        pairEvaluations = pairEvaluations
                    )
                } else {
                    pairEvaluations[
                        SourceIndexPair.of(
                            canonicalCandidate.sourceIndex,
                            record.sourceIndex
                        )
                    ] ?: aggregateEvaluationForRecord(
                        sourceIndex = record.sourceIndex,
                        groupSourceIndices = sortedRecords.map { it.sourceIndex }.toSet(),
                        pairEvaluations = pairEvaluations
                    )
                }

                CatalogDuplicateCandidate(
                    sourceIndex = record.sourceIndex,
                    itemName = record.itemName,
                    normalizedKey = record.normalizedKey,
                    matchScore = if (
                        record.sourceIndex == canonicalCandidate.sourceIndex
                    ) {
                        1.0
                    } else {
                        evaluation.score
                    },
                    reasons = evaluation.reasons
                )
            }
            .sortedWith(
                compareByDescending<CatalogDuplicateCandidate> { it.matchScore }
                    .thenBy { it.itemName.lowercase(Locale.ROOT) }
                    .thenBy { it.sourceIndex }
            )

        val pairScores = sortedRecords
            .flatMapIndexed { firstIndex, first ->
                sortedRecords
                    .drop(firstIndex + 1)
                    .mapNotNull { second ->
                        pairEvaluations[
                            SourceIndexPair.of(
                                first.sourceIndex,
                                second.sourceIndex
                            )
                        ]?.score
                    }
            }

        val confidence = when {
            pairScores.isEmpty() -> 0.0
            else -> pairScores.average().coerceIn(0.0, 1.0)
        }

        val allReasons = memberCandidates
            .flatMap { it.reasons }
            .toSet()

        val recommendation = determineRecommendation(
            confidence = confidence,
            reasons = allReasons
        )

        val stableMemberKeys = sortedRecords
            .map { record ->
                recordsBySourceIndex.getValue(record.sourceIndex)
            }
            .map { record ->
                buildString {
                    append(record.normalizedKey)
                    append(':')
                    append(record.semanticNameKey)
                    append(':')
                    append(record.sourceIndex)
                }
            }
            .sorted()

        return CatalogDuplicateGroup(
            groupId = createStableGroupId(stableMemberKeys),
            canonicalCandidateSourceIndex = canonicalCandidate.sourceIndex,
            canonicalCandidateName = canonicalCandidate.canonicalName,
            members = memberCandidates,
            confidence = confidence,
            recommendation = recommendation
        )
    }

    private fun determineRecommendation(
        confidence: Double,
        reasons: Set<CatalogDuplicateReason>
    ): CatalogDuplicateRecommendation {
        val containsOnlySafeReasons = reasons.isNotEmpty() &&
                reasons.all { reason -> reason in SAFE_AUTOMATIC_REASONS }

        val containsReviewReason = reasons.any { reason ->
            reason in REVIEW_REQUIRED_REASONS
        }

        return when {
            confidence >= AUTOMATIC_MERGE_CONFIDENCE &&
                    containsOnlySafeReasons ->
                CatalogDuplicateRecommendation.MERGE_AUTOMATICALLY

            confidence >= REVIEW_MERGE_CONFIDENCE &&
                    !containsReviewReason ->
                CatalogDuplicateRecommendation.MERGE_AFTER_REVIEW

            confidence >= MINIMUM_SIMILARITY_SCORE ->
                CatalogDuplicateRecommendation.REVIEW

            else ->
                CatalogDuplicateRecommendation.KEEP_SEPARATE
        }
    }

    private fun aggregateEvaluationForRecord(
        sourceIndex: Int,
        groupSourceIndices: Set<Int>,
        pairEvaluations: Map<SourceIndexPair, PairEvaluation>
    ): PairEvaluation {
        val evaluations = groupSourceIndices
            .asSequence()
            .filter { it != sourceIndex }
            .mapNotNull { otherSourceIndex ->
                pairEvaluations[
                    SourceIndexPair.of(sourceIndex, otherSourceIndex)
                ]
            }
            .toList()

        if (evaluations.isEmpty()) {
            return PairEvaluation(
                score = 0.0,
                reasons = emptySet()
            )
        }

        return PairEvaluation(
            score = evaluations.maxOf { it.score },
            reasons = evaluations
                .flatMap { it.reasons }
                .toSortedSet(compareBy { it.name })
        )
    }

    private fun connectAll(
        records: List<DuplicateDetectionRecord>,
        reason: CatalogDuplicateReason,
        score: Double,
        unionFind: UnionFind,
        pairEvaluations: MutableMap<SourceIndexPair, PairEvaluation>
    ) {
        records
            .sortedBy { it.sourceIndex }
            .forEachPair { first, second ->
                connect(
                    first = first,
                    second = second,
                    reasons = setOf(reason),
                    score = score,
                    unionFind = unionFind,
                    pairEvaluations = pairEvaluations
                )
            }
    }

    private fun connect(
        first: DuplicateDetectionRecord,
        second: DuplicateDetectionRecord,
        reasons: Set<CatalogDuplicateReason>,
        score: Double,
        unionFind: UnionFind,
        pairEvaluations: MutableMap<SourceIndexPair, PairEvaluation>
    ) {
        if (first.sourceIndex == second.sourceIndex) {
            return
        }

        unionFind.union(first.sourceIndex, second.sourceIndex)

        val pair = SourceIndexPair.of(
            first.sourceIndex,
            second.sourceIndex
        )

        val existing = pairEvaluations[pair]

        pairEvaluations[pair] = PairEvaluation(
            score = max(existing?.score ?: 0.0, score.coerceIn(0.0, 1.0)),
            reasons = buildSet {
                existing?.reasons?.let(::addAll)
                addAll(reasons)
            }.toSortedSet(compareBy { it.name })
        )
    }

    private fun canonicalCandidateRank(
        record: DuplicateDetectionRecord
    ): Int {
        val tokens = tokenSet(record.canonicalName)

        var rank = 0

        if (tokens.any { it in BRAND_TOKENS }) {
            rank += 100
        }

        if (tokens.any { it in SALES_FORM_TOKENS }) {
            rank += 50
        }

        if (tokens.any { it in PREPARATION_TOKENS }) {
            rank += 20
        }

        if (containsPackageSize(record.canonicalName)) {
            rank += 100
        }

        if (containsProductNumber(record.canonicalName)) {
            rank += 100
        }

        return rank
    }

    private fun normalizeExactName(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC)
            .trim()
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .lowercase(Locale.ROOT)

    private fun normalizeSemanticName(
        value: String
    ): String =
        Normalizer.normalize(
            transliterateGermanCharacters(value),
            Normalizer.Form.NFKD
        )
            .lowercase(Locale.ROOT)
            .replace(COMBINING_MARKS_REGEX, "")
            .replace(PERCENT_REGEX, " prozent ")
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
            .replace("ẞ", "SS")
            .replace("ß", "ss")

    private fun sortedTokenKey(value: String): String =
        tokenSet(value)
            .sorted()
            .joinToString(" ")

    private fun tokenSet(value: String): Set<String> =
        normalizeSemanticName(value)
            .split(' ')
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSortedSet()

    private fun buildSingularPluralKeys(
        itemName: String,
        plural: String?
    ): Set<String> {
        val values = linkedSetOf<String>()

        val normalizedItemName = normalizeSemanticName(itemName)

        if (normalizedItemName.isNotBlank()) {
            values += normalizedItemName
            values += heuristicSingularForm(normalizedItemName)
        }

        val normalizedPlural = plural
            ?.takeIf { it.isNotBlank() }
            ?.let(::normalizeSemanticName)

        if (!normalizedPlural.isNullOrBlank()) {
            values += normalizedPlural
            values += heuristicSingularForm(normalizedPlural)
        }

        return values
            .filter { it.isNotBlank() }
            .toSortedSet()
    }

    private fun heuristicSingularForm(value: String): String {
        val tokens = value.split(' ').toMutableList()

        if (tokens.isEmpty()) {
            return value
        }

        val finalToken = tokens.last()

        val singularFinalToken = when {
            finalToken.length > 5 && finalToken.endsWith("nen") ->
                finalToken.dropLast(2)

            finalToken.length > 4 && finalToken.endsWith("en") ->
                finalToken.dropLast(2)

            finalToken.length > 4 && finalToken.endsWith("er") ->
                finalToken

            finalToken.length > 4 && finalToken.endsWith("e") ->
                finalToken.dropLast(1)

            finalToken.length > 4 && finalToken.endsWith("n") ->
                finalToken.dropLast(1)

            finalToken.length > 4 && finalToken.endsWith("s") ->
                finalToken.dropLast(1)

            else -> finalToken
        }

        tokens[tokens.lastIndex] = singularFinalToken

        return tokens.joinToString(" ")
    }

    private fun isSingularPluralPair(
        first: DuplicateDetectionRecord,
        second: DuplicateDetectionRecord
    ): Boolean {
        val sharedKeys = first.singularPluralKeys
            .intersect(second.singularPluralKeys)

        if (sharedKeys.isEmpty()) {
            return false
        }

        return first.semanticNameKey != second.semanticNameKey ||
                first.plural
                    ?.let(::normalizeSemanticName)
                    ?.let { it == second.semanticNameKey } == true ||
                second.plural
                    ?.let(::normalizeSemanticName)
                    ?.let { it == first.semanticNameKey } == true
    }

    private fun isColorOrderVariant(
        first: DuplicateDetectionRecord,
        second: DuplicateDetectionRecord
    ): Boolean {
        val firstTokens = tokenSet(first.canonicalName)
        val secondTokens = tokenSet(second.canonicalName)

        if (firstTokens != secondTokens) {
            return false
        }

        val containsColor = firstTokens.any { it in COLOR_TOKENS }

        return containsColor &&
                first.semanticNameKey != second.semanticNameKey
    }

    private fun hasPunctuationDifference(
        first: String,
        second: String
    ): Boolean {
        if (first == second) {
            return false
        }

        return normalizeSemanticName(first) ==
                normalizeSemanticName(second)
    }

    private fun containsConflictingPreparationState(
        firstTokens: Set<String>,
        secondTokens: Set<String>
    ): Boolean {
        val firstPreparationTokens =
            firstTokens.intersect(PREPARATION_TOKENS)

        val secondPreparationTokens =
            secondTokens.intersect(PREPARATION_TOKENS)

        if (
            firstPreparationTokens.isEmpty() &&
            secondPreparationTokens.isEmpty()
        ) {
            return false
        }

        /*
         * Ein Zubereitungsmarker auf nur einer Seite beschreibt bereits einen
         * anderen kanonischen Lebensmittelzustand, beispielsweise:
         *
         * Kartoffel        vs. Kartoffel gekocht
         * Hähnchenbrust    vs. Hähnchenbrust gegrillt
         * Tomate           vs. Tomate getrocknet
         */
        if (
            firstPreparationTokens.isEmpty() !=
            secondPreparationTokens.isEmpty()
        ) {
            return true
        }

        /*
         * Unterschiedliche explizite Zustände bleiben ebenfalls getrennt:
         *
         * roh vs. gekocht
         * gebraten vs. gegrillt
         * frisch vs. tiefgekühlt
         */
        return firstPreparationTokens != secondPreparationTokens
    }

    private fun isPreparationVariant(
        firstTokens: Set<String>,
        secondTokens: Set<String>
    ): Boolean {
        val difference = firstTokens.symmetricDifference(secondTokens)

        return difference.isNotEmpty() &&
                difference.all { it in PREPARATION_TOKENS } &&
                firstTokens.intersect(secondTokens).isNotEmpty()
    }

    private fun isSalesFormVariant(
        firstTokens: Set<String>,
        secondTokens: Set<String>
    ): Boolean {
        val difference = firstTokens.symmetricDifference(secondTokens)

        return difference.isNotEmpty() &&
                difference.all { it in SALES_FORM_TOKENS } &&
                firstTokens.intersect(secondTokens).isNotEmpty()
    }

    private fun isBrandVariant(
        firstTokens: Set<String>,
        secondTokens: Set<String>
    ): Boolean {
        val difference = firstTokens.symmetricDifference(secondTokens)

        return difference.isNotEmpty() &&
                difference.any { it in BRAND_TOKENS } &&
                difference.all {
                    it in BRAND_TOKENS ||
                            it in BIO_MARKER_TOKENS
                } &&
                firstTokens.intersect(secondTokens).isNotEmpty()
    }

    private fun containsProtectedSemanticDifference(
        firstTokens: Set<String>,
        secondTokens: Set<String>
    ): Boolean {
        val difference = firstTokens.symmetricDifference(secondTokens)

        return difference.any { token ->
            token in PROTECTED_DIFFERENTIATING_TOKENS
        }
    }

    private fun blockingKey(value: String): String {
        val tokens = tokenSet(value)

        if (tokens.isEmpty()) {
            return ""
        }

        val meaningfulTokens = tokens
            .filterNot {
                it in PREPARATION_TOKENS ||
                        it in SALES_FORM_TOKENS ||
                        it in BRAND_TOKENS ||
                        it in BIO_MARKER_TOKENS
            }

        val firstToken = meaningfulTokens
            .minOrNull()
            ?: tokens.minOrNull()
            ?: ""

        return firstToken.take(BLOCKING_KEY_LENGTH)
    }

    private fun isLikelyTypoVariant(
        first: String,
        second: String
    ): Boolean {
        if (first == second) {
            return false
        }

        val maximumLength = max(first.length, second.length)

        if (maximumLength == 0) {
            return false
        }

        val distance = levenshteinDistance(first, second)

        return when {
            maximumLength <= 5 -> distance <= 1
            maximumLength <= 12 -> distance <= 2
            else -> distance <= 3
        }
    }

    private fun jaccard(
        first: Set<String>,
        second: Set<String>
    ): Double {
        val unionSize = first.union(second).size

        if (unionSize == 0) {
            return 0.0
        }

        return first.intersect(second).size.toDouble() / unionSize
    }

    private fun tokenContainment(
        first: Set<String>,
        second: Set<String>
    ): Double {
        val minimumSize = minOf(first.size, second.size)

        if (minimumSize == 0) {
            return 0.0
        }

        return first.intersect(second).size.toDouble() / minimumSize
    }

    private fun normalizedEditSimilarity(
        first: String,
        second: String
    ): Double {
        val maximumLength = max(first.length, second.length)

        if (maximumLength == 0) {
            return 1.0
        }

        val distance = levenshteinDistance(first, second)

        return (
                1.0 -
                        distance.toDouble() / maximumLength.toDouble()
                ).coerceIn(0.0, 1.0)
    }

    private fun levenshteinDistance(
        first: String,
        second: String
    ): Int {
        if (first == second) {
            return 0
        }

        if (first.isEmpty()) {
            return second.length
        }

        if (second.isEmpty()) {
            return first.length
        }

        var previous = IntArray(second.length + 1) { it }
        var current = IntArray(second.length + 1)

        for (firstIndex in first.indices) {
            current[0] = firstIndex + 1

            for (secondIndex in second.indices) {
                val substitutionCost =
                    if (first[firstIndex] == second[secondIndex]) {
                        0
                    } else {
                        1
                    }

                current[secondIndex + 1] = minOf(
                    current[secondIndex] + 1,
                    previous[secondIndex + 1] + 1,
                    previous[secondIndex] + substitutionCost
                )
            }

            val temporary = previous
            previous = current
            current = temporary
        }

        return previous[second.length]
    }

    private fun containsPackageSize(value: String): Boolean =
        PACKAGE_SIZE_REGEX.containsMatchIn(
            value.lowercase(Locale.ROOT)
        )

    private fun containsProductNumber(value: String): Boolean =
        PRODUCT_NUMBER_REGEX.containsMatchIn(
            value.lowercase(Locale.ROOT)
        )

    private fun createStableGroupId(memberKeys: List<String>): String {
        val input = memberKeys.joinToString("|")
        val digest = MessageDigest
            .getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))

        return digest
            .joinToString(separator = "") { byte ->
                "%02x".format(Locale.ROOT, byte.toInt() and 0xff)
            }
            .take(GROUP_ID_LENGTH)
    }

    private fun Set<String>.symmetricDifference(
        other: Set<String>
    ): Set<String> =
        (this - other) + (other - this)

    private inline fun <T> List<T>.forEachPair(
        action: (T, T) -> Unit
    ) {
        for (firstIndex in indices) {
            for (secondIndex in firstIndex + 1 until size) {
                action(this[firstIndex], this[secondIndex])
            }
        }
    }

    private data class DuplicateDetectionRecord(
        val sourceIndex: Int,
        val itemName: String,
        val plural: String?,
        val originalNormalizedKey: String?,
        val normalizedKey: String,
        val canonicalName: String,
        val exactNameKey: String,
        val semanticNameKey: String,
        val sortedTokenKey: String,
        val singularPluralKeys: Set<String>,
        val normalizationChangeCount: Int,
        val alreadyCanonical: Boolean
    )

    private data class PairEvaluation(
        val score: Double,
        val reasons: Set<CatalogDuplicateReason>
    )

    private data class SimilarityEvaluation(
        val score: Double,
        val reasons: Set<CatalogDuplicateReason>
    )

    private data class SourceIndexPair(
        val first: Int,
        val second: Int
    ) {
        companion object {

            fun of(
                first: Int,
                second: Int
            ): SourceIndexPair =
                if (first <= second) {
                    SourceIndexPair(first, second)
                } else {
                    SourceIndexPair(second, first)
                }
        }
    }

    private class UnionFind(
        sourceIndices: List<Int>
    ) {
        private val parent = sourceIndices.associateWith { it }.toMutableMap()
        private val rank = sourceIndices.associateWith { 0 }.toMutableMap()

        fun find(sourceIndex: Int): Int {
            val currentParent = requireNotNull(parent[sourceIndex]) {
                "Unknown source index in duplicate union: $sourceIndex"
            }

            if (currentParent != sourceIndex) {
                parent[sourceIndex] = find(currentParent)
            }

            return parent.getValue(sourceIndex)
        }

        fun union(
            first: Int,
            second: Int
        ) {
            val firstRoot = find(first)
            val secondRoot = find(second)

            if (firstRoot == secondRoot) {
                return
            }

            val firstRank = rank.getValue(firstRoot)
            val secondRank = rank.getValue(secondRoot)

            when {
                firstRank < secondRank -> {
                    parent[firstRoot] = secondRoot
                }

                firstRank > secondRank -> {
                    parent[secondRoot] = firstRoot
                }

                else -> {
                    val stableRoot = minOf(firstRoot, secondRoot)
                    val otherRoot = maxOf(firstRoot, secondRoot)

                    parent[otherRoot] = stableRoot
                    rank[stableRoot] = firstRank + 1
                }
            }
        }
    }

    private companion object {

        const val SCORE_IDENTICAL_NORMALIZED_KEY = 1.0
        const val SCORE_IDENTICAL_ITEM_NAME = 1.0
        const val SCORE_NORMALIZED_NAME_MATCH = 0.99
        const val SCORE_WORD_ORDER_VARIANT = 0.96
        const val SCORE_SINGULAR_PLURAL_VARIANT = 0.94
        const val SCORE_TYPO_VARIANT = 0.88
        const val SCORE_BRAND_VARIANT = 0.84
        const val SCORE_SALES_FORM_VARIANT = 0.82

        const val AUTOMATIC_MERGE_CONFIDENCE = 0.95
        const val REVIEW_MERGE_CONFIDENCE = 0.88
        const val MINIMUM_SIMILARITY_SCORE = 0.78
        const val POSSIBLE_SEMANTIC_DUPLICATE_THRESHOLD = 0.78

        const val MINIMUM_SEMANTIC_JACCARD = 0.50
        const val MINIMUM_TYPO_JACCARD = 0.50
        const val MINIMUM_WORD_ORDER_TOKEN_COUNT = 2

        const val JACCARD_WEIGHT = 0.50
        const val EDIT_DISTANCE_WEIGHT = 0.30
        const val TOKEN_CONTAINMENT_WEIGHT = 0.20

        const val BLOCKING_KEY_LENGTH = 5
        const val GROUP_ID_LENGTH = 16

        val MULTIPLE_WHITESPACE_REGEX = Regex("\\s+")
        val COMBINING_MARKS_REGEX = Regex("\\p{M}+")
        val NON_ALPHANUMERIC_REGEX = Regex("[^a-z0-9]+")
        val PERCENT_REGEX = Regex("%")
        val AMPERSAND_REGEX = Regex("&")

        val PACKAGE_SIZE_REGEX = Regex(
            """(?:^|\s)\d+(?:[.,]\d+)?\s*(?:mg|g|kg|ml|cl|dl|l|stk|stueck|stück)(?:\s|$)"""
        )

        val PRODUCT_NUMBER_REGEX = Regex(
            """(?:^|\s)(?:nr\.?|nummer|no\.?)\s*\d+(?:\s|$)"""
        )

        val SAFE_AUTOMATIC_REASONS = setOf(
            CatalogDuplicateReason.IDENTICAL_ITEM_NAME,
            CatalogDuplicateReason.IDENTICAL_NORMALIZED_KEY,
            CatalogDuplicateReason.NORMALIZED_NAME_MATCH,
            CatalogDuplicateReason.PUNCTUATION_VARIANT,
            CatalogDuplicateReason.WORD_ORDER_VARIANT,
            CatalogDuplicateReason.COLOR_ORDER_VARIANT
        )

        val REVIEW_REQUIRED_REASONS = setOf(
            CatalogDuplicateReason.PREPARATION_VARIANT,
            CatalogDuplicateReason.SALES_FORM_VARIANT,
            CatalogDuplicateReason.BRAND_VARIANT,
            CatalogDuplicateReason.POSSIBLE_SEMANTIC_DUPLICATE
        )

        val COLOR_TOKENS = setOf(
            "beige",
            "blau",
            "braun",
            "gelb",
            "gruen",
            "grüne",
            "gruenen",
            "lila",
            "orange",
            "rosa",
            "rot",
            "rote",
            "roten",
            "schwarz",
            "violett",
            "weiss",
            "weiße",
            "weissen"
        )

        val PREPARATION_TOKENS = setOf(
            "gekocht",
            "gegart",
            "gebacken",
            "gebraten",
            "gegrillt",
            "getrocknet",
            "geraeuchert",
            "geräuchert",
            "geschält",
            "geschaelt",
            "geschnitten",
            "gehackt",
            "gerieben",
            "gemahlen",
            "pueriert",
            "püriert",
            "mariniert",
            "blanchiert",
            "tiefgekuehlt",
            "tiefgekühlt",
            "gefroren",
            "aufgetaut",
            "roh"
        )

        val SALES_FORM_TOKENS = setOf(
            "lose",
            "stueck",
            "stück",
            "packung",
            "beutel",
            "dose",
            "glas",
            "flasche",
            "schale",
            "netz",
            "bund",
            "portion",
            "multipack",
            "vorratspackung"
        )

        val BRAND_TOKENS = setOf(
            "aldi",
            "barilla",
            "edeka",
            "gut",
            "guenstig",
            "günstig",
            "ja",
            "kaufland",
            "lidl",
            "milbona",
            "netto",
            "penny",
            "rewe"
        )

        val BIO_MARKER_TOKENS = setOf(
            "bio",
            "biologisch",
            "oekologisch",
            "ökologisch",
            "demeter"
        )

        val PROTECTED_DIFFERENTIATING_TOKENS = setOf(
            "alkoholfrei",
            "gezuckert",
            "ungesuesst",
            "ungesüßt",
            "laktosefrei",
            "glutenfrei",
            "vegan",
            "vegetarisch",
            "vollfett",
            "fettarm",
            "mager",
            "scharf",
            "mild",
            "suess",
            "süß",
            "sauer",
            "salzig",
            "hell",
            "dunkel"
        )
    }
}