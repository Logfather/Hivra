package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit

import com.google.gson.JsonObject
import java.text.Normalizer

class CanonicalIdentityDuplicateAnalyzer {

    fun analyze(
        baseline: List<JsonObject>,
        regeneratedExpansion: List<JsonObject>
    ): CanonicalIdentityDuplicateAuditReport {

        val items =
            buildList {

                baseline.forEach { json ->
                    add(
                        toAuditItem(
                            json = json,
                            sourceLayer =
                                CanonicalIdentitySourceLayer.BASELINE
                        )
                    )
                }

                regeneratedExpansion.forEach { json ->
                    add(
                        toAuditItem(
                            json = json,
                            sourceLayer =
                                CanonicalIdentitySourceLayer
                                    .REGENERATED_EXPANSION
                        )
                    )
                }
            }

        val groups =
            buildList {

                addAll(
                    duplicateGroups(
                        items = items,
                        reason =
                            CanonicalIdentityDuplicateReason
                                .IDENTICAL_NORMALIZED_KEY,
                        severity =
                            CanonicalIdentityDuplicateSeverity.ERROR,
                        fingerprint = {
                            it.normalized
                                .trim()
                                .lowercase()
                        }
                    )
                )

                addAll(
                    duplicateGroups(
                        items = items,
                        reason =
                            CanonicalIdentityDuplicateReason
                                .IDENTICAL_NORMALIZED_NAME,
                        severity =
                            CanonicalIdentityDuplicateSeverity.ERROR,
                        fingerprint = {
                            normalizeName(
                                it.itemName
                            )
                        }
                    )
                )

                addAll(
                    punctuationVariantGroups(
                        items
                    )
                )

                addAll(
                    wordOrderVariantGroups(
                        items
                    )
                )
            }
                /*
                 * Ein identisches Item-Paar kann theoretisch über
                 * mehrere heuristische Fingerprints gefunden werden.
                 *
                 * Die Gründe bleiben getrennt, identische Resultate
                 * desselben Reason/Fingerprint aber nicht.
                 */
                .distinctBy { group ->
                    listOf(
                        group.reason.name,
                        group.fingerprint,
                        group.entries
                            .map { it.normalized }
                            .sorted()
                            .joinToString("|")
                    )
                        .joinToString("::")
                }
                .sortedWith(
                    compareBy<CanonicalIdentityDuplicateGroup>(
                        { it.severity.name },
                        { it.reason.name },
                        { it.fingerprint }
                    )
                )

        val errorGroups =
            groups.filter {
                it.severity ==
                        CanonicalIdentityDuplicateSeverity.ERROR
            }

        val reviewGroups =
            groups.filter {
                it.severity ==
                        CanonicalIdentityDuplicateSeverity.REVIEW
            }

        val affectedEntries =
            groups
                .flatMap {
                    it.entries
                }
                .map {
                    it.normalized
                }
                .toSet()

        val errorAffectedEntries =
            errorGroups
                .flatMap {
                    it.entries
                }
                .map {
                    it.normalized
                }
                .toSet()

        val reviewAffectedEntries =
            reviewGroups
                .flatMap {
                    it.entries
                }
                .map {
                    it.normalized
                }
                .toSet()

        val countsByReason =
            groups
                .groupingBy {
                    it.reason.name
                }
                .eachCount()
                .toSortedMap()

        return CanonicalIdentityDuplicateAuditReport(
            schemaVersion = 1,

            baselineEntryCount =
                baseline.size,

            regeneratedExpansionEntryCount =
                regeneratedExpansion.size,

            projectedCatalogEntryCount =
                items.size,

            duplicateGroupCount =
                groups.size,

            errorGroupCount =
                errorGroups.size,

            reviewGroupCount =
                reviewGroups.size,

            affectedEntryCount =
                affectedEntries.size,

            errorAffectedEntryCount =
                errorAffectedEntries.size,

            reviewAffectedEntryCount =
                reviewAffectedEntries.size,

            countsByReason =
                countsByReason,

            groups =
                groups
        )
    }

    private fun duplicateGroups(
        items: List<CanonicalIdentityAuditItem>,
        reason: CanonicalIdentityDuplicateReason,
        severity: CanonicalIdentityDuplicateSeverity,
        fingerprint: (CanonicalIdentityAuditItem) -> String
    ): List<CanonicalIdentityDuplicateGroup> {

        return items
            .groupBy(fingerprint)
            .filter { (key, values) ->
                key.isNotBlank() &&
                        values.size > 1
            }
            .map { (key, values) ->
                group(
                    reason = reason,
                    severity = severity,
                    fingerprint = key,
                    entries = values
                )
            }
    }

    private fun punctuationVariantGroups(
        items: List<CanonicalIdentityAuditItem>
    ): List<CanonicalIdentityDuplicateGroup> {

        return items
            .groupBy {
                punctuationFingerprint(
                    it.itemName
                )
            }
            .filter { (fingerprint, values) ->

                if (
                    fingerprint.isBlank() ||
                    values.size < 2
                ) {
                    return@filter false
                }

                /*
                 * Wenn die normalisierten Namen ohnehin identisch
                 * sind, wird der Konflikt bereits als ERROR über
                 * IDENTICAL_NORMALIZED_NAME gemeldet.
                 */
                values
                    .map {
                        normalizeName(
                            it.itemName
                        )
                    }
                    .distinct()
                    .size > 1
            }
            .map { (fingerprint, values) ->
                group(
                    reason =
                        CanonicalIdentityDuplicateReason
                            .PUNCTUATION_VARIANT,
                    severity =
                        CanonicalIdentityDuplicateSeverity.REVIEW,
                    fingerprint =
                        fingerprint,
                    entries =
                        values
                )
            }
    }

    private fun wordOrderVariantGroups(
        items: List<CanonicalIdentityAuditItem>
    ): List<CanonicalIdentityDuplicateGroup> {

        return items
            /*
             * Word-order matching wird innerhalb derselben Category
             * durchgeführt. Dadurch vermeiden wir unnötige
             * Cross-Domain-False-Positives.
             */
            .groupBy { item ->
                listOf(
                    item.category,
                    tokenSetFingerprint(
                        item.itemName
                    )
                )
                    .joinToString("::")
            }
            .filter { (_, values) ->

                if (values.size < 2) {
                    return@filter false
                }

                val normalizedNames =
                    values
                        .map {
                            normalizeName(
                                it.itemName
                            )
                        }
                        .distinct()

                /*
                 * Gleiche Reihenfolge = kein Word-Order-Problem.
                 */
                normalizedNames.size > 1
            }
            .map { (fingerprint, values) ->
                group(
                    reason =
                        CanonicalIdentityDuplicateReason
                            .WORD_ORDER_VARIANT,
                    severity =
                        CanonicalIdentityDuplicateSeverity.REVIEW,
                    fingerprint =
                        fingerprint,
                    entries =
                        values
                )
            }
    }

    private fun group(
        reason: CanonicalIdentityDuplicateReason,
        severity: CanonicalIdentityDuplicateSeverity,
        fingerprint: String,
        entries: List<CanonicalIdentityAuditItem>
    ): CanonicalIdentityDuplicateGroup {

        val sortedEntries =
            entries
                .sortedWith(
                    compareBy<CanonicalIdentityAuditItem>(
                        { it.category },
                        { it.normalized },
                        { it.itemName }
                    )
                )

        return CanonicalIdentityDuplicateGroup(
            severity = severity,
            reason = reason,
            fingerprint = fingerprint,
            entryCount = sortedEntries.size,
            categories =
                sortedEntries
                    .map { it.category }
                    .distinct()
                    .sorted(),
            entries =
                sortedEntries
        )
    }

    private fun toAuditItem(
        json: JsonObject,
        sourceLayer: CanonicalIdentitySourceLayer
    ): CanonicalIdentityAuditItem =
        CanonicalIdentityAuditItem(
            itemName =
                requireString(
                    json,
                    "itemname"
                ),
            normalized =
                requireString(
                    json,
                    "normalized"
                ),
            category =
                requireString(
                    json,
                    "category"
                ),
            sourceLayer =
                sourceLayer
        )

    private fun normalizeName(
        value: String
    ): String {

        val decomposed =
            Normalizer.normalize(
                value
                    .trim()
                    .lowercase(),
                Normalizer.Form.NFD
            )

        return decomposed
            .replace(
                COMBINING_MARKS_REGEX,
                ""
            )
            .replace(
                "ß",
                "ss"
            )
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

    private fun punctuationFingerprint(
        value: String
    ): String =
        normalizeName(value)
            .replace(
                " ",
                ""
            )

    private fun tokenSetFingerprint(
        value: String
    ): String =
        normalizeName(value)
            .split(" ")
            .filter(String::isNotBlank)
            .sorted()
            .joinToString("|")

    private fun requireString(
        json: JsonObject,
        key: String
    ): String {

        val value =
            json.get(key)
                ?: error(
                    "Missing '$key' in catalog entry: $json"
                )

        require(
            value.isJsonPrimitive
        ) {
            "Expected primitive '$key' in catalog entry: $json"
        }

        return value
            .asString
            .trim()
            .also {
                require(it.isNotBlank()) {
                    "Blank '$key' in catalog entry: $json"
                }
            }
    }

    private companion object {

        val COMBINING_MARKS_REGEX =
            Regex("\\p{M}+")

        val NON_ALPHANUMERIC_REGEX =
            Regex("[^a-z0-9]+")

        val WHITESPACE_REGEX =
            Regex("\\s+")
    }
}