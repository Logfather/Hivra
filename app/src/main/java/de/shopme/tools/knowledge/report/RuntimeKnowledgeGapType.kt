package de.shopme.tools.knowledge.report

/**
 * Beschreibt die Ursache dafür, dass für einen Catalog Key kein
 * Runtime-Knowledge-Eintrag erzeugt werden konnte.
 */
enum class RuntimeKnowledgeGapType {

    /**
     * Das Candidate Retrieval hat keine möglichen Server Keys geliefert.
     */
    NO_CANDIDATES,

    /**
     * Die AI hat trotz vorhandener Kandidaten bewusst keinen Match gewählt.
     */
    NO_MATCH,

    /**
     * Die AI hat einen Match vorgeschlagen, dessen Confidence unterhalb
     * der erforderlichen Schwelle lag.
     */
    LOW_CONFIDENCE,

    /**
     * Die AI-Entscheidung war grundsätzlich verwendbar, wurde aber durch
     * die deterministische Validierung verworfen.
     */
    VALIDATION_REJECTED,

    /**
     * Ein Match wurde akzeptiert, der referenzierte Server Key existiert
     * jedoch nicht im Knowledge-Artefakt.
     */
    SERVER_ENTRY_MISSING,

    /**
     * Mehrere gleichwertige Kandidaten konnten nicht eindeutig aufgelöst
     * werden.
     */
    AMBIGUOUS_MATCH,

    /**
     * Die vorhandenen Diagnoseinformationen reichen für keine präzisere
     * Klassifikation aus.
     */
    UNKNOWN
}

/**
 * Klassifikation eines einzelnen fehlenden Catalog Keys.
 */
data class RuntimeKnowledgeGap(
    val catalogKey: String,
    val type: RuntimeKnowledgeGapType,
    val candidateCount: Int,
    val selectedServerKey: String? = null,
    val confidence: Double? = null,
    val details: String? = null
)

/**
 * Vollständiger, deterministisch sortierter Gap-Report.
 */
data class RuntimeKnowledgeGapReport(
    val gaps: List<RuntimeKnowledgeGap>
) {

    val totalCount: Int
        get() =
            gaps.size

    val counts: Map<RuntimeKnowledgeGapType, Int>
        get() =
            RuntimeKnowledgeGapType.entries
                .associateWith { type ->
                    gaps.count {
                        it.type == type
                    }
                }

    fun count(
        type: RuntimeKnowledgeGapType
    ): Int =
        counts.getValue(type)
}

/**
 * Klassifiziert fehlende Runtime-Knowledge-Einträge anhand bereits
 * persistierter Pipeline-Ergebnisse.
 *
 * Es werden keine Kandidaten neu gesucht, keine AI-Anfragen ausgeführt
 * und keine Mappings verändert.
 */
class RuntimeKnowledgeGapClassifier {

    fun classify(
        inputs: Collection<RuntimeKnowledgeGapInput>
    ): RuntimeKnowledgeGapReport {

        requireUniqueCatalogKeys(
            inputs = inputs
        )

        val gaps =
            inputs
                .map(::classify)
                .sortedBy {
                    it.catalogKey
                }

        return RuntimeKnowledgeGapReport(
            gaps = gaps
        )
    }

    fun classify(
        input: RuntimeKnowledgeGapInput
    ): RuntimeKnowledgeGap {

        val catalogKey =
            input.catalogKey.trim()

        require(catalogKey.isNotEmpty()) {
            "catalogKey must not be blank."
        }

        val candidates =
            input.candidates
                .asSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
                .sorted()
                .toList()

        val decision =
            input.decision

        val type =
            classifyType(
                candidates = candidates,
                decision = decision,
                validation = input.validation,
                serverEntryExists =
                    input.serverEntryExists
            )

        return RuntimeKnowledgeGap(
            catalogKey =
                catalogKey,
            type =
                type,
            candidateCount =
                candidates.size,
            selectedServerKey =
                decision
                    ?.selectedServerKey
                    ?.trim()
                    ?.takeIf(String::isNotEmpty),
            confidence =
                decision?.confidence,
            details =
                createDetails(
                    type = type,
                    decision = decision,
                    validation =
                        input.validation
                )
        )
    }

    private fun classifyType(
        candidates: List<String>,
        decision: RuntimeKnowledgeGapDecision?,
        validation: RuntimeKnowledgeGapValidation?,
        serverEntryExists: Boolean?
    ): RuntimeKnowledgeGapType {

        /*
         * Reihenfolge ist fachlich relevant.
         *
         * Ein akzeptiertes Mapping auf einen nicht vorhandenen Server Key
         * muss beispielsweise als SERVER_ENTRY_MISSING erscheinen und nicht
         * als UNKNOWN.
         */

        if (candidates.isEmpty()) {
            return RuntimeKnowledgeGapType.NO_CANDIDATES
        }

        if (
            decision?.outcome ==
            RuntimeKnowledgeGapDecisionOutcome.AMBIGUOUS
        ) {
            return RuntimeKnowledgeGapType.AMBIGUOUS_MATCH
        }

        if (
            decision?.outcome ==
            RuntimeKnowledgeGapDecisionOutcome.NO_MATCH
        ) {
            return RuntimeKnowledgeGapType.NO_MATCH
        }

        if (
            decision?.outcome ==
            RuntimeKnowledgeGapDecisionOutcome.MATCH &&
            decision.isBelowMinimumConfidence()
        ) {
            return RuntimeKnowledgeGapType.LOW_CONFIDENCE
        }

        if (
            validation != null &&
            !validation.accepted
        ) {
            return RuntimeKnowledgeGapType.VALIDATION_REJECTED
        }

        if (
            decision?.outcome ==
            RuntimeKnowledgeGapDecisionOutcome.MATCH &&
            validation?.accepted == true &&
            serverEntryExists == false
        ) {
            return RuntimeKnowledgeGapType.SERVER_ENTRY_MISSING
        }

        return RuntimeKnowledgeGapType.UNKNOWN
    }

    private fun RuntimeKnowledgeGapDecision
            .isBelowMinimumConfidence(): Boolean {

        val actualConfidence =
            confidence
                ?: return false

        val requiredConfidence =
            minimumConfidence
                ?: return false

        return actualConfidence < requiredConfidence
    }

    private fun createDetails(
        type: RuntimeKnowledgeGapType,
        decision: RuntimeKnowledgeGapDecision?,
        validation: RuntimeKnowledgeGapValidation?
    ): String? =
        when (type) {

            RuntimeKnowledgeGapType.NO_CANDIDATES ->
                "Candidate retrieval returned no candidates."

            RuntimeKnowledgeGapType.NO_MATCH ->
                "AI decision returned NO_MATCH."

            RuntimeKnowledgeGapType.LOW_CONFIDENCE ->
                buildString {
                    append("AI confidence ")

                    append(
                        decision?.confidence
                            ?: "unknown"
                    )

                    append(
                        " is below required confidence "
                    )

                    append(
                        decision?.minimumConfidence
                            ?: "unknown"
                    )

                    append(".")
                }

            RuntimeKnowledgeGapType.VALIDATION_REJECTED ->
                validation
                    ?.reason
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?: "Deterministic validation rejected the match."

            RuntimeKnowledgeGapType.SERVER_ENTRY_MISSING ->
                "Selected server entry does not exist."

            RuntimeKnowledgeGapType.AMBIGUOUS_MATCH ->
                "AI decision could not resolve candidates unambiguously."

            RuntimeKnowledgeGapType.UNKNOWN ->
                "Available diagnostic data does not explain the gap."
        }

    private fun requireUniqueCatalogKeys(
        inputs: Collection<RuntimeKnowledgeGapInput>
    ) {

        val duplicateKeys =
            inputs
                .groupingBy {
                    it.catalogKey.trim()
                }
                .eachCount()
                .filterValues {
                    it > 1
                }
                .keys
                .filter {
                    it.isNotEmpty()
                }
                .sorted()

        require(
            duplicateKeys.isEmpty()
        ) {
            "Duplicate catalog keys in gap input: " +
                    duplicateKeys.joinToString()
        }
    }
}