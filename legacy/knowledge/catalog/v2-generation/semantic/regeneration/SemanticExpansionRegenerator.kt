package de.shopme.testing.system.tools.knowledge.catalog.semantic.regeneration

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamily
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyPolicy
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.SemanticVariantIdentityDecision
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.SemanticVariantIdentityPolicy
import de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility.MarketPlausibilityDecision
import de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility.MarketPlausibilityValidator
import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantDefinition
import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantTaxonomyRegistry

class SemanticExpansionRegenerator {

    fun regenerate(
        catalog: JsonArray
    ): SemanticExpansionRegenerationResult {

        val entries =
            catalog
                .mapNotNull { element ->
                    element
                        .takeIf { it.isJsonObject }
                        ?.asJsonObject
                }

        val baseline =
            entries
                .filterNot(::isVariantBearing)

        val expansion =
            entries
                .filter(::isVariantBearing)

        /*
         * Jede Expansion basiert auf einer existierenden
         * kanonischen Family aus dem Basiskatalog.
         */
        val familiesByDisplayName =
            CanonicalProductFamilyPolicy
                .FAMILIES
                .associateBy { family ->
                    family.displayName
                }

        require(
            familiesByDisplayName.size ==
                    CanonicalProductFamilyPolicy.FAMILIES.size
        ) {
            "Canonical product-family display names must be unique."
        }

        val validator =
            MarketPlausibilityValidator()

        val acceptedCandidates =
            mutableListOf<ProjectedCandidate>()

        val rejectedEntries =
            mutableListOf<SemanticExpansionRejectedEntry>()

        val reviewEntries =
            mutableListOf<SemanticExpansionReviewEntry>()

        val projections =
            mutableListOf<SemanticExpansionProjection>()

        var acceptedInputCount =
            0

        var rejectedInputCount =
            0

        var reviewInputCount =
            0

        expansion.forEach { source ->

            val plausibility =
                requireNotNull(
                    validator.validate(source)
                ) {
                    "Market plausibility validator returned null for " +
                            requireString(source, "itemname")
                }

            when (plausibility.decision) {

                MarketPlausibilityDecision.ACCEPT -> {

                    acceptedInputCount++

                    val projection =
                        projectAcceptedEntry(
                            source = source,
                            familiesByDisplayName =
                                familiesByDisplayName
                        )

                    acceptedCandidates +=
                        projection

                    if (
                        projection
                            .removedKnowledgeVariantKeys
                            .isNotEmpty()
                    ) {
                        projections +=
                            SemanticExpansionProjection(
                                sourceItemName =
                                    requireString(
                                        source,
                                        "itemname"
                                    ),
                                sourceNormalized =
                                    requireString(
                                        source,
                                        "normalized"
                                    ),
                                projectedItemName =
                                    requireString(
                                        projection.entry,
                                        "itemname"
                                    ),
                                projectedNormalized =
                                    requireString(
                                        projection.entry,
                                        "normalized"
                                    ),
                                removedKnowledgeVariantKeys =
                                    projection
                                        .removedKnowledgeVariantKeys
                            )
                    }
                }

                MarketPlausibilityDecision.REJECT -> {

                    rejectedInputCount++

                    rejectedEntries +=
                        SemanticExpansionRejectedEntry(
                            itemName =
                                requireString(
                                    source,
                                    "itemname"
                                ),
                            normalized =
                                requireString(
                                    source,
                                    "normalized"
                                ),
                            reason =
                                plausibility.reason.name
                        )
                }

                MarketPlausibilityDecision.REVIEW -> {

                    reviewInputCount++

                    reviewEntries +=
                        SemanticExpansionReviewEntry(
                            itemName =
                                requireString(
                                    source,
                                    "itemname"
                                ),
                            normalized =
                                requireString(
                                    source,
                                    "normalized"
                                ),
                            reason =
                                plausibility.reason.name
                        )
                }
            }
        }

        /*
         * Mehrere alte Expansion-Einträge können nach Entfernung
         * von Knowledge-Attributen auf dieselbe Produktidentität
         * projizieren.
         *
         * Beispiel:
         *
         * Müsli – Mandel – Glutenfrei
         * Müsli – Mandel – Eifrei
         *
         *              ↓
         *
         * Müsli – Mandel
         */
        val groupedByNormalized =
            acceptedCandidates
                .groupBy { candidate ->
                    requireString(
                        candidate.entry,
                        "normalized"
                    )
                }
                .toSortedMap()

        val regeneratedExpansion =
            groupedByNormalized
                .map { (_, candidates) ->

                    /*
                     * Deterministische Winner-Auswahl.
                     *
                     * Bevorzugt wird der Candidate, der keine
                     * Knowledge-Projektion benötigt hat.
                     * Danach sourceNormalized lexikographisch.
                     */
                    candidates
                        .sortedWith(
                            compareBy<ProjectedCandidate>(
                                {
                                    it.removedKnowledgeVariantKeys
                                        .isNotEmpty()
                                },
                                {
                                    it.sourceNormalized
                                }
                            )
                        )
                        .first()
                        .entry
                }
                .sortedWith(
                    compareBy<JsonObject>(
                        {
                            requireString(
                                it,
                                "category"
                            )
                        },
                        {
                            requireString(
                                it,
                                "normalized"
                            )
                        }
                    )
                )

        val collapsedDuplicateCount =
            acceptedCandidates.size -
                    regeneratedExpansion.size

        /*
         * Kein regenerierter Expansion-Key darf mit einem
         * Baseline-Key kollidieren.
         *
         * Wenn Knowledge-Projektion auf reine Family kollabiert,
         * gehört der Entry nicht mehr in die Expansion.
         */
        val baselineNormalizedKeys =
            baseline
                .map {
                    requireString(
                        it,
                        "normalized"
                    )
                }
                .toSet()

        val finalExpansion =
            regeneratedExpansion
                .filter { entry ->
                    requireString(
                        entry,
                        "normalized"
                    ) !in baselineNormalizedKeys
                }

        val collapsedIntoBaselineCount =
            regeneratedExpansion.size -
                    finalExpansion.size

        return SemanticExpansionRegenerationResult(
            schemaVersion = 1,

            inputCatalogEntryCount =
                catalog.size(),

            baselineEntryCount =
                baseline.size,

            expansionInputEntryCount =
                expansion.size,

            acceptedInputEntryCount =
                acceptedInputCount,

            rejectedInputEntryCount =
                rejectedInputCount,

            reviewInputEntryCount =
                reviewInputCount,

            acceptedProjectedEntryCount =
                acceptedCandidates.size,

            collapsedDuplicateCount =
                collapsedDuplicateCount +
                        collapsedIntoBaselineCount,

            knowledgeAttributeProjectionCount =
                projections.size,

            regeneratedExpansionEntryCount =
                finalExpansion.size,

            projectedCatalogEntryCount =
                baseline.size +
                        finalExpansion.size,

            regeneratedExpansion =
                finalExpansion,

            rejectedEntries =
                rejectedEntries
                    .sortedBy {
                        it.normalized
                    },

            reviewEntries =
                reviewEntries
                    .sortedBy {
                        it.normalized
                    },

            projections =
                projections
                    .sortedWith(
                        compareBy(
                            { it.projectedNormalized },
                            { it.sourceNormalized }
                        )
                    )
        )
    }

    private fun projectAcceptedEntry(
        source: JsonObject,
        familiesByDisplayName:
        Map<String, CanonicalProductFamily>
    ): ProjectedCandidate {

        val sourceItemName =
            requireString(
                source,
                "itemname"
            )

        val sourceNormalized =
            requireString(
                source,
                "normalized"
            )

        val parts =
            sourceItemName
                .split(VARIANT_SEPARATOR)
                .map(String::trim)
                .filter(String::isNotBlank)

        require(parts.size >= 2) {
            "Expected variant-bearing expansion item: $sourceItemName"
        }

        val family =
            parts.first()

        val familyDefinition =
            requireNotNull(
                familiesByDisplayName[family]
            ) {
                "Expansion family '$family' is not defined in " +
                        "CanonicalProductFamilyPolicy."
            }

        require(
            familyDefinition.category ==
                    requireString(source, "category")
        ) {
            "Expansion family '$family' belongs to category " +
                    "'${familyDefinition.category}', but source entry uses " +
                    "'${requireString(source, "category")}'."
        }

        val variants =
            parts
                .drop(1)
                .map { rawVariant ->

                    rawVariant to
                            requireNotNull(
                                SemanticVariantTaxonomyRegistry
                                    .definitionFor(rawVariant)
                            ) {
                                "Missing semantic variant taxonomy for " +
                                        "'$rawVariant' in '$sourceItemName'."
                            }
                }

        val retained =
            variants.filter { (_, definition) ->

                SemanticVariantIdentityPolicy
                    .evaluate(
                        definition.type
                    )
                    .decision !=
                        SemanticVariantIdentityDecision.KNOWLEDGE_ONLY
            }

        val removed =
            variants.filter { (_, definition) ->

                SemanticVariantIdentityPolicy
                    .evaluate(
                        definition.type
                    )
                    .decision ==
                        SemanticVariantIdentityDecision.KNOWLEDGE_ONLY
            }

        /*
         * ACCEPT darf nach Identity Separation niemals
         * ausschließlich aus Knowledge-Attributen bestehen.
         */
        require(retained.isNotEmpty()) {
            "Accepted expansion unexpectedly collapsed to pure " +
                    "knowledge identity: $sourceItemName"
        }

        val projectedItemName =
            buildProjectedItemName(
                family = family,
                retainedVariants =
                    retained.map { it.first }
            )

        val projectedNormalized =
            buildProjectedNormalized(
                familyDefinition = familyDefinition,
                retainedDefinitions =
                    retained.map { it.second }
            )

        val projected =
            source.deepCopy()

        projected.addProperty(
            "itemname",
            projectedItemName
        )

        projected.addProperty(
            "normalized",
            projectedNormalized
        )

        /*
         * Linguistische Metadaten werden bewusst nicht versucht
         * "intelligent" zu reparieren.
         *
         * Der bekannte v1-Fehler:
         * Variant Values wurden pauschal als colloquial übernommen.
         *
         * Deshalb werden expansion-generierte colloquial-Tokens
         * für das Zwischenartefakt entfernt.
         *
         * Die vollständige linguistische Rekonstruktion erfolgt
         * im späteren Commit "Repair linguistic metadata".
         */
        projected.add(
            "colloquial",
            JsonArray()
        )

        projected.add(
            "phoneticTokens",
            JsonArray()
        )

        projected.add(
            "autocompleteTokens",
            JsonArray().apply {
                add(projectedNormalized)
            }
        )

        /*
         * Auch plural darf keine entfernten Knowledge-Attribute
         * mehr enthalten. Bis zur linguistischen Reparatur
         * verwenden wir deterministisch den projected itemName.
         */
        projected.addProperty(
            "plural",
            projectedItemName
        )

        return ProjectedCandidate(
            sourceNormalized =
                sourceNormalized,

            entry =
                projected,

            removedKnowledgeVariantKeys =
                removed
                    .map {
                        it.second.canonicalKey
                    }
                    .sorted()
        )
    }

    private fun buildProjectedItemName(
        family: String,
        retainedVariants: List<String>
    ): String =
        buildList {
            add(family)
            addAll(retainedVariants)
        }
            .joinToString(
                separator =
                    VARIANT_SEPARATOR
            )

    private fun buildProjectedNormalized(
        familyDefinition: CanonicalProductFamily,
        retainedDefinitions:
        List<SemanticVariantDefinition>
    ): String {

        return buildList {

            add(
                familyDefinition.key
            )

            addAll(
                retainedDefinitions
                    .map {
                        it.canonicalKey
                    }
            )
        }
            .joinToString("-")
    }

    private fun isVariantBearing(
        entry: JsonObject
    ): Boolean =
        requireString(
            entry,
            "itemname"
        )
            .contains(
                VARIANT_SEPARATOR
            )

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

    private data class ProjectedCandidate(
        val sourceNormalized: String,
        val entry: JsonObject,
        val removedKnowledgeVariantKeys: List<String>
    )

    private companion object {

        const val VARIANT_SEPARATOR =
            " – "
    }
}