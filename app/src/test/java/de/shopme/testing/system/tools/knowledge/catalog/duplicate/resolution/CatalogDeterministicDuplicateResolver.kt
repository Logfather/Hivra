package de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution

import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlan
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlanEntry
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import java.util.Locale

class CatalogDeterministicDuplicateResolver {

    fun resolve(
        itemsBySourceIndex: Map<Int, CatalogFoodItem>,
        plan: CatalogCanonicalizationPlan
    ): CatalogDuplicateResolutionResult {
        validateInputs(
            itemsBySourceIndex = itemsBySourceIndex,
            plan = plan
        )

        val mutableItems = itemsBySourceIndex
            .toSortedMap()
            .toMutableMap()

        val planEntriesBySourceIndex =
            plan.entries.associateBy { it.sourceIndex }

        val referencedMergeTargets = plan.entries
            .mapNotNull { entry ->
                entry.mergeTargetSourceIndex
            }
            .toSet()

        val decisions =
            mutableListOf<CatalogDuplicateResolutionDecision>()

        /*
         * Phase 1:
         *
         * Gleicher kanonischer Key bedeutet gleiche kanonische Identität.
         */
        duplicateNormalizedKeyGroups(mutableItems)
            .forEach { (normalizedKey, sourceIndices) ->
                resolveGroup(
                    sourceIndices = sourceIndices,
                    normalizedKey = normalizedKey,
                    reason =
                        CatalogDuplicateResolutionReason
                            .IDENTICAL_NORMALIZED_KEY,
                    mutableItems = mutableItems,
                    planEntriesBySourceIndex =
                        planEntriesBySourceIndex,
                    referencedMergeTargets =
                        referencedMergeTargets,
                    decisions = decisions
                )
            }

        /*
         * Phase 2:
         *
         * Nach Phase 1 können theoretisch noch identische kanonische Namen
         * mit unterschiedlichen Legacy-Keys verbleiben. Sie werden nur dann
         * zusammengeführt, wenn zusätzlich die Kategorie identisch ist.
         */
        duplicateCanonicalNameAndCategoryGroups(mutableItems)
            .forEach {
                    (groupKey, sourceIndices) ->

                val targetKey = sourceIndices
                    .mapNotNull { sourceIndex ->
                        mutableItems[sourceIndex]
                            ?.normalized
                            ?.trim()
                            ?.takeIf(String::isNotBlank)
                    }
                    .sorted()
                    .firstOrNull()
                    ?: groupKey

                resolveGroup(
                    sourceIndices = sourceIndices,
                    normalizedKey = targetKey,
                    reason =
                        CatalogDuplicateResolutionReason
                            .IDENTICAL_CANONICAL_NAME_AND_CATEGORY,
                    mutableItems = mutableItems,
                    planEntriesBySourceIndex =
                        planEntriesBySourceIndex,
                    referencedMergeTargets =
                        referencedMergeTargets,
                    decisions = decisions
                )
            }

        /*
         * Phase 3:
         *
         * Singular und Plural besitzen bewusst unterschiedliche kanonische Keys.
         * Sie können deshalb weder über Phase 1 noch Phase 2 zusammengeführt
         * werden.
         *
         * Ein Merge wird ausschließlich dann durchgeführt, wenn:
         *
         * - der Canonicalization-Plan ausdrücklich MERGE vorgibt,
         * - das Ziel eindeutig angegeben ist,
         * - nur SINGULAR_PLURAL_VARIANT als fachlicher Duplicate-Grund vorliegt,
         * - die Kategorien identisch sind,
         * - der Quellname exakt dem Plural des Zielartikels entspricht.
         */
        resolveDeterministicSingularPluralVariants(
            mutableItems = mutableItems,
            plan = plan,
            decisions = decisions
        )

        /*
         * Phase 4:
         *
         * Ein TYPO_VARIANT wird nur dann zusammengeführt, wenn der Audit-Plan ein
         * eindeutiges Ziel enthält, keine weiteren fachlichen Duplicate-Gründe
         * vorliegen und die beiden Namen eine streng begrenzte orthografische
         * Abweichung besitzen.
         */
        resolveDeterministicTypoVariants(
            mutableItems = mutableItems,
            plan = plan,
            decisions = decisions
        )

        val sortedDecisions = decisions
            .distinctBy { it.sourceIndex }
            .sortedBy { it.sourceIndex }

        validateResolvedOutput(
            itemsBySourceIndex = mutableItems,
            decisions = sortedDecisions
        )

        return CatalogDuplicateResolutionResult(
            version =
                CatalogDuplicateResolutionResult.CURRENT_VERSION,
            inputEntryCount = itemsBySourceIndex.size,
            outputEntryCount = mutableItems.size,
            mergedEntryCount = sortedDecisions.size,
            itemsBySourceIndex = mutableItems.toSortedMap(),
            decisions = sortedDecisions,
            valid = true
        )
    }

    private fun resolveDeterministicSingularPluralVariants(
        mutableItems: MutableMap<Int, CatalogFoodItem>,
        plan: CatalogCanonicalizationPlan,
        decisions: MutableList<CatalogDuplicateResolutionDecision>
    ) {
        plan.entries
            .asSequence()

            /*
             * Keine Vorfilterung ausschließlich auf MERGE.
             *
             * Die vollständige Kandidatenprüfung akzeptiert sowohl MERGE als
             * auch REVIEW, sofern ein eindeutiges Ziel und ausschließlich eine
             * sichere Singular-/Plural-Evidenz vorhanden sind.
             */
            .filter(::isPureSingularPluralMergeCandidate)

            .sortedBy { it.sourceIndex }
            .forEach { planEntry ->
                val sourceIndex =
                    planEntry.sourceIndex

                val targetSourceIndex =
                    requireNotNull(
                        planEntry.mergeTargetSourceIndex
                    ) {
                        "Deterministic singular-plural candidate " +
                                "$sourceIndex has no merge target."
                    }

                /*
                 * Quelle oder Ziel können bereits durch eine frühere
                 * deterministische Auflösungsphase entfernt worden sein.
                 */
                val sourceItem =
                    mutableItems[sourceIndex]
                        ?: return@forEach

                val targetItem =
                    mutableItems[targetSourceIndex]
                        ?: return@forEach

                if (
                    !hasIdenticalCanonicalCategory(
                        sourceItem = sourceItem,
                        targetItem = targetItem
                    )
                ) {
                    return@forEach
                }

                if (
                    !isExactSingularPluralRelationship(
                        sourceItem = sourceItem,
                        targetItem = targetItem
                    )
                ) {
                    return@forEach
                }

                val mergedTargetItem =
                    mergeItems(
                        targetItem = targetItem,
                        duplicateItems =
                            listOf(sourceItem)
                    )

                mutableItems[targetSourceIndex] =
                    mergedTargetItem

                mutableItems.remove(sourceIndex)

                decisions +=
                    CatalogDuplicateResolutionDecision(
                        sourceIndex = sourceIndex,
                        targetSourceIndex =
                            targetSourceIndex,
                        normalizedKey =
                            requireNotNull(
                                mergedTargetItem.normalized
                                    ?.trim()
                                    ?.takeIf(String::isNotBlank)
                            ) {
                                "Singular-plural merge target " +
                                        "$targetSourceIndex has no " +
                                        "normalized key."
                            },
                        reason =
                            CatalogDuplicateResolutionReason
                                .DETERMINISTIC_SINGULAR_PLURAL_VARIANT
                    )
            }
    }

    private fun isPureSingularPluralMergeCandidate(
        planEntry: CatalogCanonicalizationPlanEntry
    ): Boolean {
        /*
         * Der historische Plan enthält sichere Singular-/Plural-Fälle sowohl
         * als MERGE als auch als REVIEW. Entscheidend ist daher nicht allein
         * die Aktion, sondern die vollständige Evidenz:
         *
         * - eindeutiges Ziel,
         * - expliziter SINGULAR_PLURAL_VARIANT-Grund,
         * - keine zusätzliche semantische Konfliktevidenz.
         */
        if (
            planEntry.action !=
            CatalogCanonicalizationAction.MERGE &&
            planEntry.action !=
            CatalogCanonicalizationAction.REVIEW
        ) {
            return false
        }

        val targetSourceIndex =
            planEntry.mergeTargetSourceIndex
                ?: return false

        if (targetSourceIndex == planEntry.sourceIndex) {
            return false
        }

        val normalizedReasons = planEntry.reasons
            .map(::normalizeReason)
            .filter(String::isNotBlank)

        if (normalizedReasons.isEmpty()) {
            return false
        }

        val hasSingularPluralReason =
            normalizedReasons.any {
                SINGULAR_PLURAL_REASON_MARKER in it
            }

        if (!hasSingularPluralReason) {
            return false
        }

        val hasConflictingReason =
            normalizedReasons.any { reason ->
                CONFLICTING_DUPLICATE_REASON_MARKERS.any {
                        marker ->
                    marker in reason
                }
            }

        if (hasConflictingReason) {
            return false
        }

        /*
         * Zulässig sind nur Diagnose, Zielreferenz, Gruppenreferenz und eine
         * REVIEW- oder MERGE_AFTER_REVIEW-Empfehlung.
         */
        return normalizedReasons.all { reason ->
            SINGULAR_PLURAL_REASON_MARKER in reason ||
                    ALLOWED_SINGULAR_PLURAL_REASON_MARKERS.any {
                            allowedMarker ->
                        allowedMarker in reason
                    }
        }
    }

    private fun hasIdenticalCanonicalCategory(
        sourceItem: CatalogFoodItem,
        targetItem: CatalogFoodItem
    ): Boolean {
        val sourceCategory = sourceItem.category
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?.lowercase(Locale.ROOT)
            ?: return false

        val targetCategory = targetItem.category
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?.lowercase(Locale.ROOT)
            ?: return false

        return sourceCategory == targetCategory
    }

    private fun isExactSingularPluralRelationship(
        sourceItem: CatalogFoodItem,
        targetItem: CatalogFoodItem
    ): Boolean {
        val sourceName =
            normalizeLinguisticText(sourceItem.itemname)

        val targetName =
            normalizeLinguisticText(targetItem.itemname)

        if (
            sourceName.isBlank() ||
            targetName.isBlank() ||
            sourceName == targetName
        ) {
            return false
        }

        /*
         * Höchste Evidenz: Das persistierte Pluralfeld einer Seite entspricht
         * exakt dem Namen der anderen Seite.
         */
        val sourcePlural = sourceItem.plural
            ?.let(::normalizeLinguisticText)
            ?.takeIf(String::isNotBlank)

        val targetPlural = targetItem.plural
            ?.let(::normalizeLinguisticText)
            ?.takeIf(String::isNotBlank)

        if (
            targetPlural != null &&
            sourceName == targetPlural
        ) {
            return true
        }

        if (
            sourcePlural != null &&
            targetName == sourcePlural
        ) {
            return true
        }

        /*
         * Zweite Evidenzstufe: deterministisch erzeugte Kandidaten aus dem
         * Namen. Bei Mehrwortbegriffen wird ausschließlich das letzte
         * lexikalische Token verändert.
         */
        if (
            sourceName in
            generateDeterministicPluralCandidates(targetName)
        ) {
            return true
        }

        if (
            targetName in
            generateDeterministicPluralCandidates(sourceName)
        ) {
            return true
        }

        return false
    }

    private fun generateDeterministicPluralCandidates(
        singularName: String
    ): Set<String> {
        val normalized =
            normalizeLinguisticText(singularName)

        if (normalized.isBlank()) {
            return emptySet()
        }

        val tokens = normalized.split(' ')

        val prefix = tokens
            .dropLast(1)
            .joinToString(" ")

        val lexicalToken = tokens.last()

        val pluralTokens =
            generatePluralTokenCandidates(lexicalToken)

        return pluralTokens
            .mapTo(sortedSetOf()) { pluralToken ->
                if (prefix.isBlank()) {
                    pluralToken
                } else {
                    "$prefix $pluralToken"
                }
            }
    }

    private fun generatePluralTokenCandidates(
        singularToken: String
    ): Set<String> {
        val normalizedToken =
            normalizeLinguisticToken(singularToken)

        if (normalizedToken.isBlank()) {
            return emptySet()
        }

        val candidates = linkedSetOf<String>()

        /*
         * Persistierte unveränderliche Lebensmittelbegriffe.
         */
        if (normalizedToken in INVARIANT_PLURAL_TOKENS) {
            candidates += normalizedToken
        }

        /*
         * Regelmäßige deutsche Pluralbildungen.
         */
        candidates += normalizedToken + "e"
        candidates += normalizedToken + "en"
        candidates += normalizedToken + "n"
        candidates += normalizedToken + "er"
        candidates += normalizedToken + "s"

        /*
         * Endet der Singular bereits auf -e, ist -n die häufigste Bildung:
         * Erdbeere → Erdbeeren
         * Kirsche  → Kirschen
         */
        if (normalizedToken.endsWith("e")) {
            candidates += normalizedToken + "n"
        }

        /*
         * Endet der Singular auf -el, -er oder -en, bleibt der Wortkörper
         * häufig stabil oder erhält einen Umlaut:
         * Apfel → Äpfel
         * Zwiebel → Zwiebeln
         */
        if (
            normalizedToken.endsWith("el") ||
            normalizedToken.endsWith("er") ||
            normalizedToken.endsWith("en")
        ) {
            candidates += normalizedToken
            candidates += addDeterministicUmlaut(normalizedToken)
            candidates += addDeterministicUmlaut(normalizedToken) + "n"
        }

        /*
         * Häufige feminine Endungen.
         */
        if (normalizedToken.endsWith("in")) {
            candidates += normalizedToken + "nen"
        }

        /*
         * Häufige -um/-ium-Neutra.
         */
        if (normalizedToken.endsWith("um")) {
            candidates +=
                normalizedToken.removeSuffix("um") + "en"
        }

        if (normalizedToken.endsWith("ium")) {
            candidates +=
                normalizedToken.removeSuffix("ium") + "ien"
        }

        /*
         * Explizite fachliche Ausnahmen. Sie verhindern Kunstformen und
         * decken nur geprüfte Lebensmittelbegriffe ab.
         */
        IRREGULAR_PLURAL_TOKENS[
            normalizedToken
        ]?.let(candidates::add)

        return candidates
            .map(::normalizeLinguisticToken)
            .filter(String::isNotBlank)
            .toSortedSet()
    }

    private fun addDeterministicUmlaut(
        value: String
    ): String {
        val replacement = UMLAUT_REPLACEMENTS
            .firstOrNull { (plain, _) ->
                plain in value
            }
            ?: return value

        return value.replaceFirst(
            oldValue = replacement.first,
            newValue = replacement.second
        )
    }

    private fun normalizeLinguisticToken(
        value: String
    ): String =
        normalizeLinguisticText(value)
            .replace(" ", "")

    private fun normalizeLinguisticText(
        value: String
    ): String =
        value
            .trim()
            .lowercase(Locale.GERMAN)
            .replace(NON_LEXICAL_CHARACTER_REGEX, " ")
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .trim()

    private fun normalizeReason(
        value: String
    ): String =
        value
            .trim()
            .uppercase(Locale.ROOT)
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")

    private fun resolveDeterministicTypoVariants(
        mutableItems: MutableMap<Int, CatalogFoodItem>,
        plan: CatalogCanonicalizationPlan,
        decisions: MutableList<CatalogDuplicateResolutionDecision>
    ) {
        plan.entries
            .asSequence()
            .filter(::isPureTypoMergeCandidate)
            .sortedBy { it.sourceIndex }
            .forEach { planEntry ->
                val sourceIndex =
                    planEntry.sourceIndex

                val targetSourceIndex =
                    requireNotNull(
                        planEntry.mergeTargetSourceIndex
                    ) {
                        "Deterministic typo candidate $sourceIndex " +
                                "has no merge target."
                    }

                /*
                 * Frühere Resolver-Phasen können Quelle oder Ziel bereits
                 * entfernt haben. In diesem Fall ist keine weitere Aktion nötig.
                 */
                val sourceItem =
                    mutableItems[sourceIndex]
                        ?: return@forEach

                val targetItem =
                    mutableItems[targetSourceIndex]
                        ?: return@forEach

                if (
                    !hasIdenticalCanonicalCategory(
                        sourceItem = sourceItem,
                        targetItem = targetItem
                    )
                ) {
                    return@forEach
                }

                if (
                    !isDeterministicTypoRelationship(
                        sourceItem = sourceItem,
                        targetItem = targetItem
                    )
                ) {
                    return@forEach
                }

                val mergedTargetItem =
                    mergeItems(
                        targetItem = targetItem,
                        duplicateItems = listOf(sourceItem)
                    )

                mutableItems[targetSourceIndex] =
                    mergedTargetItem

                mutableItems.remove(sourceIndex)

                decisions +=
                    CatalogDuplicateResolutionDecision(
                        sourceIndex = sourceIndex,
                        targetSourceIndex =
                            targetSourceIndex,
                        normalizedKey =
                            requireNotNull(
                                mergedTargetItem.normalized
                                    ?.trim()
                                    ?.takeIf(String::isNotBlank)
                            ) {
                                "Typo merge target $targetSourceIndex " +
                                        "has no normalized key."
                            },
                        reason =
                            CatalogDuplicateResolutionReason
                                .DETERMINISTIC_TYPO_VARIANT
                    )
            }
    }

    private fun isPureTypoMergeCandidate(
        planEntry: CatalogCanonicalizationPlanEntry
    ): Boolean {
        if (
            planEntry.action !=
            CatalogCanonicalizationAction.MERGE &&
            planEntry.action !=
            CatalogCanonicalizationAction.REVIEW
        ) {
            return false
        }

        val targetSourceIndex =
            planEntry.mergeTargetSourceIndex
                ?: return false

        if (targetSourceIndex == planEntry.sourceIndex) {
            return false
        }

        val normalizedReasons = planEntry.reasons
            .map(::normalizeReason)
            .filter(String::isNotBlank)

        if (normalizedReasons.isEmpty()) {
            return false
        }

        val hasTypoReason =
            normalizedReasons.any {
                TYPO_REASON_MARKER in it
            }

        if (!hasTypoReason) {
            return false
        }

        val hasConflictingReason =
            normalizedReasons.any { reason ->
                CONFLICTING_TYPO_REASON_MARKERS.any {
                        conflictingMarker ->
                    conflictingMarker in reason
                }
            }

        if (hasConflictingReason) {
            return false
        }

        /*
         * Erlaubt sind nur:
         *
         * - TYPO_VARIANT-Diagnose,
         * - Zielreferenz,
         * - Review- beziehungsweise Merge-Empfehlung,
         * - Duplicate-Gruppenreferenz.
         *
         * Jeder unbekannte Zusatzgrund hält den Eintrag konservativ im Review.
         */
        return normalizedReasons.all { reason ->
            TYPO_REASON_MARKER in reason ||
                    ALLOWED_TYPO_REASON_MARKERS.any {
                            allowedMarker ->
                        allowedMarker in reason
                    }
        }
    }

    private fun isDeterministicTypoRelationship(
        sourceItem: CatalogFoodItem,
        targetItem: CatalogFoodItem
    ): Boolean {
        val sourceName =
            normalizeTypoComparisonText(
                sourceItem.itemname
            )

        val targetName =
            normalizeTypoComparisonText(
                targetItem.itemname
            )

        if (
            sourceName.isBlank() ||
            targetName.isBlank() ||
            sourceName == targetName
        ) {
            return false
        }

        val sourceNumbers =
            extractNumericTokens(sourceName)

        val targetNumbers =
            extractNumericTokens(targetName)

        /*
         * Produktnummern, Fettstufen, Typnummern oder Prozentwerte dürfen durch
         * einen Typo-Merge niemals verändert werden.
         */
        if (sourceNumbers != targetNumbers) {
            return false
        }

        val sourceTokens =
            tokenizeTypoComparisonText(sourceName)

        val targetTokens =
            tokenizeTypoComparisonText(targetName)

        if (
            sourceTokens.isEmpty() ||
            targetTokens.isEmpty() ||
            sourceTokens.size != targetTokens.size
        ) {
            return false
        }

        val differingTokenPairs =
            sourceTokens
                .zip(targetTokens)
                .filter { (sourceToken, targetToken) ->
                    sourceToken != targetToken
                }

        /*
         * Mehrere abweichende Wörter sind kein enger Typo mehr, sondern können
         * eine semantische, Wortreihenfolge- oder Produktvariantenänderung sein.
         */
        if (differingTokenPairs.size != 1) {
            return false
        }

        val (sourceToken, targetToken) =
            differingTokenPairs.single()

        return isDeterministicTypoTokenPair(
            sourceToken = sourceToken,
            targetToken = targetToken
        )
    }

    private fun isDeterministicTypoTokenPair(
        sourceToken: String,
        targetToken: String
    ): Boolean {
        if (
            sourceToken.isBlank() ||
            targetToken.isBlank() ||
            sourceToken == targetToken
        ) {
            return false
        }

        /*
         * Explizite fachliche Äquivalenz für deutsche Mehltypen:
         *
         * Typ 405  ↔ Type 405
         * Typ 550  ↔ Type 550
         * Typ 1050 ↔ Type 1050
         *
         * Diese Prüfung muss vor der allgemeinen Mindestlängenregel erfolgen,
         * weil "typ" nur drei Zeichen besitzt.
         */
        if (
            isTypTypeEquivalent(
                sourceToken = sourceToken,
                targetToken = targetToken
            )
        ) {
            return true
        }

        /*
         * Explizite orthografische Äquivalenz:
         *
         * Sauce ↔ Soße ↔ Sosse
         * Tomatensauce ↔ Tomatensoße
         *
         * Die Prüfung vereinheitlicht ausschließlich den Bestandteil
         * "Sauce/Soße/Sosse". Alle übrigen Wortbestandteile müssen danach
         * identisch sein.
         *
         * Daher gilt beispielsweise:
         *
         * Tomatensauce ↔ Tomatensoße  = akzeptiert
         * Sahnesauce   ↔ Senfsauce    = abgelehnt
         */
        if (
            isSauceSosseEquivalent(
                sourceToken = sourceToken,
                targetToken = targetToken
            )
        ) {
            return true
        }

        /*
         * Sehr kurze Wörter sind bei einer einzelnen Zeichenänderung häufig
         * bereits andere Lebensmittel oder Eigenschaften.
         */
        if (
            sourceToken.length <
            MINIMUM_TYPO_TOKEN_LENGTH ||
            targetToken.length <
            MINIMUM_TYPO_TOKEN_LENGTH
        ) {
            return false
        }

        val normalizedSource =
            normalizeKnownGermanOrthography(
                sourceToken
            )

        val normalizedTarget =
            normalizeKnownGermanOrthography(
                targetToken
            )

        /*
         * Bekannte äquivalente deutsche Schreibweisen:
         *
         * Weißkohl / Weisskohl
         * Müsliriegel / Muesliriegel
         */
        if (normalizedSource == normalizedTarget) {
            return true
        }

        val distance =
            damerauLevenshteinDistance(
                left = normalizedSource,
                right = normalizedTarget,
                maximumDistance =
                    MAXIMUM_DETERMINISTIC_TYPO_DISTANCE
            )

        return distance in
                1..MAXIMUM_DETERMINISTIC_TYPO_DISTANCE &&
                lengthDifference(
                    normalizedSource,
                    normalizedTarget
                ) <= MAXIMUM_TYPO_LENGTH_DIFFERENCE
    }

    private fun isSauceSosseEquivalent(
        sourceToken: String,
        targetToken: String
    ): Boolean {
        val normalizedSource =
            normalizeSauceSosseToken(
                sourceToken
            )

        val normalizedTarget =
            normalizeSauceSosseToken(
                targetToken
            )

        if (
            normalizedSource.isBlank() ||
            normalizedTarget.isBlank() ||
            normalizedSource != normalizedTarget
        ) {
            return false
        }

        /*
         * Eine identische Normalform reicht allein nicht aus. Mindestens eine
         * Seite muss tatsächlich eine Sauce-/Soße-Schreibweise enthalten.
         * Dadurch bleibt diese Regel fachlich eng begrenzt.
         */
        return containsSauceSosseVariant(sourceToken) &&
                containsSauceSosseVariant(targetToken) &&
                !sourceToken.equals(
                    targetToken,
                    ignoreCase = true
                )
    }

    private fun normalizeSauceSosseToken(
        value: String
    ): String =
        normalizeKnownGermanOrthography(
            value
        )
            /*
             * Nach normalizeKnownGermanOrthography gilt:
             *
             * Soße  → sosse
             * Sosse → sosse
             * Sauce → sauce
             *
             * Danach wird nur "sauce" auf die gemeinsame kanonische
             * Vergleichsform "sosse" abgebildet.
             */
            .replace(
                oldValue = "sauce",
                newValue = "sosse"
            )

    private fun containsSauceSosseVariant(
        value: String
    ): Boolean {
        val normalized =
            normalizeKnownGermanOrthography(
                value
            )

        return "sauce" in normalized ||
                "sosse" in normalized
    }

    private fun isTypTypeEquivalent(
        sourceToken: String,
        targetToken: String
    ): Boolean {
        val normalizedSource =
            sourceToken
                .trim()
                .lowercase(Locale.GERMAN)

        val normalizedTarget =
            targetToken
                .trim()
                .lowercase(Locale.GERMAN)

        return (
                normalizedSource == "typ" &&
                        normalizedTarget == "type"
                ) ||
                (
                        normalizedSource == "type" &&
                                normalizedTarget == "typ"
                        )
    }

    private fun normalizeTypoComparisonText(
        value: String
    ): String =
        value
            .trim()
            .lowercase(Locale.GERMAN)
            .replace(OPTIONAL_CHARACTER_BRACKET_REGEX, "$1")
            .replace(NON_LEXICAL_CHARACTER_REGEX, " ")
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .trim()

    private fun tokenizeTypoComparisonText(
        value: String
    ): List<String> =
        value
            .split(' ')
            .map(String::trim)
            .filter(String::isNotBlank)

    private fun extractNumericTokens(
        value: String
    ): List<String> =
        NUMBER_REGEX
            .findAll(value)
            .map { it.value }
            .toList()

    private fun normalizeKnownGermanOrthography(
        value: String
    ): String =
        value
            .lowercase(Locale.GERMAN)
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("ß", "ss")

    private fun lengthDifference(
        left: String,
        right: String
    ): Int =
        kotlin.math.abs(
            left.length - right.length
        )

    private fun damerauLevenshteinDistance(
        left: String,
        right: String,
        maximumDistance: Int
    ): Int {
        require(maximumDistance >= 0)

        if (left == right) {
            return 0
        }

        if (
            kotlin.math.abs(
                left.length - right.length
            ) > maximumDistance
        ) {
            return maximumDistance + 1
        }

        if (left.isEmpty()) {
            return right.length
        }

        if (right.isEmpty()) {
            return left.length
        }

        var previousPreviousRow =
            IntArray(right.length + 1)

        var previousRow =
            IntArray(right.length + 1) { index ->
                index
            }

        for (leftIndex in 1..left.length) {
            val currentRow =
                IntArray(right.length + 1)

            currentRow[0] = leftIndex

            var minimumCurrentRowValue =
                currentRow[0]

            for (rightIndex in 1..right.length) {
                val substitutionCost =
                    if (
                        left[leftIndex - 1] ==
                        right[rightIndex - 1]
                    ) {
                        0
                    } else {
                        1
                    }

                var distance = minOf(
                    previousRow[rightIndex] + 1,
                    currentRow[rightIndex - 1] + 1,
                    previousRow[rightIndex - 1] +
                            substitutionCost
                )

                if (
                    leftIndex > 1 &&
                    rightIndex > 1 &&
                    left[leftIndex - 1] ==
                    right[rightIndex - 2] &&
                    left[leftIndex - 2] ==
                    right[rightIndex - 1]
                ) {
                    distance = minOf(
                        distance,
                        previousPreviousRow[
                            rightIndex - 2
                        ] + 1
                    )
                }

                currentRow[rightIndex] =
                    distance

                minimumCurrentRowValue =
                    minOf(
                        minimumCurrentRowValue,
                        distance
                    )
            }

            if (
                minimumCurrentRowValue >
                maximumDistance
            ) {
                return maximumDistance + 1
            }

            previousPreviousRow =
                previousRow

            previousRow =
                currentRow
        }

        return previousRow[right.length]
    }

    private fun resolveGroup(
        sourceIndices: List<Int>,
        normalizedKey: String,
        reason: CatalogDuplicateResolutionReason,
        mutableItems: MutableMap<Int, CatalogFoodItem>,
        planEntriesBySourceIndex:
        Map<Int, CatalogCanonicalizationPlanEntry>,
        referencedMergeTargets: Set<Int>,
        decisions:
        MutableList<CatalogDuplicateResolutionDecision>
    ) {
        val existingSourceIndices = sourceIndices
            .filter { it in mutableItems }
            .distinct()
            .sorted()

        if (existingSourceIndices.size < 2) {
            return
        }

        val targetSourceIndex = selectTargetSourceIndex(
            sourceIndices = existingSourceIndices,
            mutableItems = mutableItems,
            planEntriesBySourceIndex =
                planEntriesBySourceIndex,
            referencedMergeTargets =
                referencedMergeTargets
        )

        val targetItem = mutableItems.getValue(
            targetSourceIndex
        )

        val duplicateItems = existingSourceIndices
            .filterNot { it == targetSourceIndex }
            .map { sourceIndex ->
                sourceIndex to mutableItems.getValue(sourceIndex)
            }

        val mergedTargetItem = mergeItems(
            targetItem = targetItem,
            duplicateItems = duplicateItems.map { it.second }
        )

        mutableItems[targetSourceIndex] =
            mergedTargetItem

        duplicateItems.forEach {
                (sourceIndex, _) ->

            mutableItems.remove(sourceIndex)

            decisions += CatalogDuplicateResolutionDecision(
                sourceIndex = sourceIndex,
                targetSourceIndex = targetSourceIndex,
                normalizedKey =
                    mergedTargetItem.normalized
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?: normalizedKey,
                reason = reason
            )
        }
    }

    private fun selectTargetSourceIndex(
        sourceIndices: List<Int>,
        mutableItems: Map<Int, CatalogFoodItem>,
        planEntriesBySourceIndex:
        Map<Int, CatalogCanonicalizationPlanEntry>,
        referencedMergeTargets: Set<Int>
    ): Int =
        sourceIndices.sortedWith(
            compareBy<Int>(
                /*
                 * Ein bereits vom Canonicalization-Plan referenziertes
                 * Merge-Ziel behält Vorrang.
                 */
                {
                    if (it in referencedMergeTargets) {
                        0
                    } else {
                        1
                    }
                },

                /*
                 * Danach gewinnt die fachlich stabilste Plan-Aktion.
                 */
                {
                    planEntriesBySourceIndex[it]
                        ?.action
                        ?.let(::actionRank)
                        ?: Int.MAX_VALUE
                },

                /*
                 * Automatische Entscheidungen sind stärker validiert als
                 * offene Review-Entscheidungen.
                 */
                {
                    val planEntry =
                        planEntriesBySourceIndex[it]

                    when {
                        planEntry == null -> 2
                        planEntry.automatic -> 0
                        else -> 1
                    }
                },

                /*
                 * Ein Eintrag mit vollständiger Kategorie hat Vorrang.
                 */
                {
                    if (
                        mutableItems[it]
                            ?.category
                            .isNullOrBlank()
                    ) {
                        1
                    } else {
                        0
                    }
                },

                /*
                 * Ein vorhandener englischer Referenzname ist nützlich für
                 * spätere Knowledge-Matches.
                 */
                {
                    if (
                        mutableItems[it]
                            ?.normalizedEnglish
                            .isNullOrBlank()
                    ) {
                        1
                    } else {
                        0
                    }
                },

                /*
                 * Finaler deterministischer Tie-Breaker.
                 */
                { it }
            )
        ).first()

    private fun actionRank(
        action: CatalogCanonicalizationAction
    ): Int =
        when (action) {
            CatalogCanonicalizationAction.KEEP -> 0
            CatalogCanonicalizationAction.NORMALIZE -> 1
            CatalogCanonicalizationAction.RENAME -> 2
            CatalogCanonicalizationAction.MOVE_CATEGORY -> 3
            CatalogCanonicalizationAction.REVIEW -> 4
            CatalogCanonicalizationAction.SPLIT -> 5
            CatalogCanonicalizationAction.MERGE -> 6
            CatalogCanonicalizationAction.REMOVE_NON_FOOD -> 7
        }

    private fun mergeItems(
        targetItem: CatalogFoodItem,
        duplicateItems: List<CatalogFoodItem>
    ): CatalogFoodItem {
        val allItems = listOf(targetItem) +
                duplicateItems

        val targetNameComparison =
            normalizeComparisonText(targetItem.itemname)

        val additionalNameAliases = duplicateItems
            .map { it.itemname.trim() }
            .filter(String::isNotBlank)
            .filter {
                normalizeComparisonText(it) !=
                        targetNameComparison
            }

        return targetItem.copy(
            production =
                targetItem.production
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?: allItems
                        .asSequence()
                        .mapNotNull { it.production }
                        .map(String::trim)
                        .firstOrNull(String::isNotBlank),

            category =
                targetItem.category
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?: allItems
                        .asSequence()
                        .mapNotNull { it.category }
                        .map(String::trim)
                        .firstOrNull(String::isNotBlank),

            plural =
                choosePlural(
                    targetItem = targetItem,
                    allItems = allItems
                ),

            colloquial =
                normalizeDisplayValues(
                    allItems.flatMap { it.colloquial } +
                            additionalNameAliases
                ),

            phoneticTokens =
                normalizeTechnicalValues(
                    allItems.flatMap {
                        it.phoneticTokens
                    }
                ),

            autocompleteTokens =
                normalizeTechnicalValues(
                    allItems.flatMap {
                        it.autocompleteTokens
                    } +
                            allItems.map { item ->
                                normalizeLinguisticText(
                                    item.itemname
                                )
                            } +
                            additionalNameAliases.map(
                                ::normalizeLinguisticText
                            )
                ),

            normalizedEnglish =
                targetItem.normalizedEnglish
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?: allItems
                        .asSequence()
                        .mapNotNull {
                            it.normalizedEnglish
                        }
                        .map(String::trim)
                        .firstOrNull(String::isNotBlank)
        )
    }

    private fun choosePlural(
        targetItem: CatalogFoodItem,
        allItems: List<CatalogFoodItem>
    ): String? {
        val targetPlural = targetItem.plural
            ?.trim()
            ?.takeIf(String::isNotBlank)

        if (targetPlural != null) {
            return targetPlural
        }

        return allItems
            .asSequence()
            .mapNotNull { it.plural }
            .map(String::trim)
            .filter(String::isNotBlank)
            .sortedWith(
                compareBy<String>(
                    { it.length },
                    {
                        it.lowercase(
                            Locale.GERMAN
                        )
                    },
                    { it }
                )
            )
            .firstOrNull()
    }

    private fun duplicateNormalizedKeyGroups(
        itemsBySourceIndex: Map<Int, CatalogFoodItem>
    ): Map<String, List<Int>> =
        itemsBySourceIndex
            .mapNotNull {
                    (sourceIndex, item) ->

                item.normalized
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?.let { normalizedKey ->
                        normalizedKey to sourceIndex
                    }
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )
            .filterValues { it.size > 1 }
            .mapValues { (_, sourceIndices) ->
                sourceIndices.sorted()
            }
            .toSortedMap()

    private fun duplicateCanonicalNameAndCategoryGroups(
        itemsBySourceIndex: Map<Int, CatalogFoodItem>
    ): Map<String, List<Int>> =
        itemsBySourceIndex
            .mapNotNull {
                    (sourceIndex, item) ->

                val name = normalizeComparisonText(
                    item.itemname
                )

                val category = item.category
                    ?.trim()
                    ?.lowercase(Locale.ROOT)
                    ?.takeIf(String::isNotBlank)

                if (
                    name.isBlank() ||
                    category == null
                ) {
                    null
                } else {
                    "$category\u0000$name" to sourceIndex
                }
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )
            .filterValues { it.size > 1 }
            .mapValues { (_, sourceIndices) ->
                sourceIndices.sorted()
            }
            .toSortedMap()

    private fun normalizeDisplayValues(
        values: List<String>
    ): List<String> =
        values
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy(::normalizeComparisonText)
            .sortedWith(
                compareBy<String>(
                    {
                        it.lowercase(
                            Locale.GERMAN
                        )
                    },
                    { it }
                )
            )

    private fun normalizeTechnicalValues(
        values: List<String>
    ): List<String> =
        values
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()

    private fun normalizeComparisonText(
        value: String
    ): String =
        value
            .trim()
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .lowercase(Locale.GERMAN)

    private fun validateInputs(
        itemsBySourceIndex: Map<Int, CatalogFoodItem>,
        plan: CatalogCanonicalizationPlan
    ) {
        require(
            itemsBySourceIndex.keys.none { it < 0 }
        ) {
            "Catalog output contains negative source indices."
        }

        require(
            plan.entries.map { it.sourceIndex }
                .distinct()
                .size ==
                    plan.entries.size
        ) {
            "Canonicalization plan contains duplicate sourceIndex values."
        }

        val planSourceIndices =
            plan.entries.mapTo(sortedSetOf()) {
                it.sourceIndex
            }

        require(
            itemsBySourceIndex.keys.all {
                it in planSourceIndices
            }
        ) {
            "Catalog output contains source indices not covered by plan."
        }
    }

    private fun validateResolvedOutput(
        itemsBySourceIndex: Map<Int, CatalogFoodItem>,
        decisions: List<CatalogDuplicateResolutionDecision>
    ) {
        val remainingDuplicateKeys =
            duplicateNormalizedKeyGroups(
                itemsBySourceIndex
            )

        require(remainingDuplicateKeys.isEmpty()) {
            "Deterministic duplicate resolution left duplicate " +
                    "normalized keys: ${remainingDuplicateKeys.keys}"
        }

        val remainingDuplicateNames =
            duplicateCanonicalNameAndCategoryGroups(
                itemsBySourceIndex
            )

        require(remainingDuplicateNames.isEmpty()) {
            "Deterministic duplicate resolution left duplicate canonical " +
                    "names within the same category: " +
                    remainingDuplicateNames.keys
        }

        require(
            decisions.map { it.sourceIndex }
                .distinct()
                .size ==
                    decisions.size
        ) {
            "Duplicate source was resolved more than once."
        }
    }

    private companion object {

        const val TYPO_REASON_MARKER =
            "TYPO_VARIANT"

        const val MINIMUM_TYPO_TOKEN_LENGTH =
            4

        const val MAXIMUM_DETERMINISTIC_TYPO_DISTANCE =
            1

        const val MAXIMUM_TYPO_LENGTH_DIFFERENCE =
            1

        val NUMBER_REGEX =
            Regex("\\d+(?:[.,]\\d+)?")

        val INVARIANT_PLURAL_TOKENS = setOf(
            "brokkoli",
            "couscous",
            "fisch",
            "fleisch",
            "gemuese",
            "obst",
            "reis",
            "salami",
            "tofu"
        )

        val IRREGULAR_PLURAL_TOKENS = mapOf(
            "apfel" to "aepfel",
            "brot" to "brote",
            "ei" to "eier",
            "kartoffel" to "kartoffeln",
            "keks" to "kekse",
            "kohl" to "kohle",
            "kürbis" to "kürbisse",
            "kuerbis" to "kuerbisse",
            "nuss" to "nuesse",
            "tomate" to "tomaten",
            "zwiebel" to "zwiebeln"
        )

        val UMLAUT_REPLACEMENTS = listOf(
            "au" to "aeu",
            "a" to "ae",
            "o" to "oe",
            "u" to "ue"
        )

        val NON_LEXICAL_CHARACTER_REGEX =
            Regex("[^\\p{L}\\p{M}\\p{N}]+")

        val OPTIONAL_CHARACTER_BRACKET_REGEX =
            Regex("\\(([\\p{L}\\p{M}])\\)")

        val ALLOWED_TYPO_REASON_MARKERS = setOf(
            "CANONICAL DUPLICATE TARGET",
            "DUPLICATE RECOMMENDATION: MERGE_AFTER_REVIEW",
            "DUPLICATE RECOMMENDATION: REVIEW",
            "ENTRY BELONGS TO DUPLICATE GROUP"
        )

        val CONFLICTING_TYPO_REASON_MARKERS = setOf(
            "IDENTICAL_ITEM_NAME",
            "IDENTICAL_NORMALIZED_KEY",
            "NORMALIZED_NAME_MATCH",
            "SINGULAR_PLURAL_VARIANT",
            "WORD_ORDER_VARIANT",
            "PUNCTUATION_VARIANT",
            "COLOR_ORDER_VARIANT",
            "PREPARATION_VARIANT",
            "SALES_FORM_VARIANT",
            "BRAND_VARIANT",
            "POSSIBLE_SEMANTIC_DUPLICATE"
        )



        const val SINGULAR_PLURAL_REASON_MARKER =
            "SINGULAR_PLURAL_VARIANT"

        val ALLOWED_SINGULAR_PLURAL_REASON_MARKERS = setOf(
            "CANONICAL DUPLICATE TARGET",
            "DUPLICATE RECOMMENDATION: MERGE_AFTER_REVIEW",
            "DUPLICATE RECOMMENDATION: REVIEW",
            "ENTRY BELONGS TO DUPLICATE GROUP"
        )

        val CONFLICTING_DUPLICATE_REASON_MARKERS = setOf(
            "IDENTICAL_ITEM_NAME",
            "IDENTICAL_NORMALIZED_KEY",
            "NORMALIZED_NAME_MATCH",
            "WORD_ORDER_VARIANT",
            "PUNCTUATION_VARIANT",
            "COLOR_ORDER_VARIANT",
            "PREPARATION_VARIANT",
            "SALES_FORM_VARIANT",
            "BRAND_VARIANT",
            "TYPO_VARIANT",
            "POSSIBLE_SEMANTIC_DUPLICATE"
        )
        val MULTIPLE_WHITESPACE_REGEX =
            Regex("\\s+")
    }
}