package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantTaxonomyRegistry

class SemanticIdentitySeparationAnalyzer {

    fun analyze(
        catalog: JsonArray,
        inputFile: String
    ): SemanticIdentitySeparationAuditReport {

        val analyzedEntries =
            catalog
                .mapNotNull { element ->
                    element
                        .takeIf { it.isJsonObject }
                        ?.asJsonObject
                }
                .mapNotNull(::analyzeEntry)

        val entries =
            analyzedEntries
                .map { it.first }
                .sortedWith(
                    compareBy<SemanticIdentitySeparationEntry>(
                        { it.category },
                        { it.projectedBaseIdentity },
                        { it.normalizedItem }
                    )
                )

        val occurrences =
            analyzedEntries
                .flatMap { it.second }
                .sortedWith(
                    compareBy<SemanticIdentitySeparationOccurrence>(
                        { it.category },
                        { it.normalizedItem },
                        { it.variantCanonicalKey }
                    )
                )

        val knowledgeOnlyOccurrences =
            occurrences.filter {
                it.identityDecision ==
                        SemanticVariantIdentityDecision.KNOWLEDGE_ONLY
            }

        val identityOccurrences =
            occurrences.filter {
                it.identityDecision ==
                        SemanticVariantIdentityDecision.IDENTITY_ALLOWED
            }

        val reviewOccurrences =
            occurrences.filter {
                it.identityDecision ==
                        SemanticVariantIdentityDecision.REVIEW
            }

        val entriesWithKnowledgeOnlyVariants =
            entries.count {
                it.hasKnowledgeOnlyVariant
            }

        val pureKnowledgeAttributeEntries =
            entries.count {
                it.hasKnowledgeOnlyVariant &&
                        !it.hasIdentityVariant &&
                        !it.hasReviewVariant
            }

        val mixedEntries =
            entries.count {
                it.hasKnowledgeOnlyVariant &&
                        (
                                it.hasIdentityVariant ||
                                        it.hasReviewVariant
                                )
            }

        val countsByType =
            knowledgeOnlyOccurrences
                .groupingBy {
                    it.variantType.name
                }
                .eachCount()
                .toSortedMap()

        val countsByKnowledgeDimension =
            knowledgeOnlyOccurrences
                .groupingBy {
                    it.targetKnowledgeDimension.name
                }
                .eachCount()
                .toSortedMap()

        val projectedBaseIdentityCollisions =
            entries
                .groupBy {
                    it.projectedBaseIdentity
                }
                .filterValues {
                    it.size > 1
                }
                .mapValues {
                    it.value.size
                }
                .toList()
                .sortedWith(
                    compareByDescending<Pair<String, Int>> {
                        it.second
                    }
                        .thenBy {
                            it.first
                        }
                )
                .toMap()

        return SemanticIdentitySeparationAuditReport(
            schemaVersion = 1,
            inputFile = inputFile,

            catalogEntryCount = catalog.size(),

            variantBearingEntryCount =
                entries.size,

            knowledgeOnlyOccurrenceCount =
                knowledgeOnlyOccurrences.size,

            identityAllowedOccurrenceCount =
                identityOccurrences.size,

            reviewOccurrenceCount =
                reviewOccurrences.size,

            entriesWithKnowledgeOnlyVariants =
                entriesWithKnowledgeOnlyVariants,

            pureKnowledgeAttributeEntryCount =
                pureKnowledgeAttributeEntries,

            mixedIdentityAndKnowledgeEntryCount =
                mixedEntries,

            countsByType =
                countsByType,

            countsByKnowledgeDimension =
                countsByKnowledgeDimension,

            projectedBaseIdentityCollisions =
                projectedBaseIdentityCollisions,

            entries =
                entries,

            occurrences =
                occurrences
        )
    }

    private fun analyzeEntry(
        json: JsonObject
    ): Pair<
            SemanticIdentitySeparationEntry,
            List<SemanticIdentitySeparationOccurrence>
            >? {

        val itemName =
            json.string("itemname")
                ?: return null

        val normalizedItem =
            json.string("normalized")
                ?: return null

        val category =
            json.string("category")
                ?: return null

        val parts =
            itemName
                .split(VARIANT_SEPARATOR)
                .map(String::trim)
                .filter(String::isNotBlank)

        if (parts.size < 2) {
            return null
        }

        val family =
            parts.first()

        val definitions =
            parts
                .drop(1)
                .map { rawVariant ->

                    val definition =
                        requireNotNull(
                            SemanticVariantTaxonomyRegistry
                                .definitionFor(rawVariant)
                        ) {
                            "Missing semantic variant taxonomy for " +
                                    "'$rawVariant' in '$itemName'."
                        }

                    rawVariant to definition
                }

        val occurrences =
            definitions.map { (rawVariant, definition) ->

                val policy =
                    SemanticVariantIdentityPolicy
                        .evaluate(
                            definition.type
                        )

                SemanticIdentitySeparationOccurrence(
                    itemName = itemName,
                    normalizedItem = normalizedItem,
                    category = category,
                    family = family,

                    variantRawValue = rawVariant,
                    variantCanonicalKey =
                        definition.canonicalKey,
                    variantType =
                        definition.type,

                    identityDecision =
                        policy.decision,
                    identityReason =
                        policy.reason,

                    targetKnowledgeDimension =
                        SemanticKnowledgeProjection
                            .dimensionFor(
                                definition.type
                            )
                )
            }

        val knowledgeOnly =
            occurrences
                .filter {
                    it.identityDecision ==
                            SemanticVariantIdentityDecision.KNOWLEDGE_ONLY
                }
                .map {
                    it.variantCanonicalKey
                }

        val identityAllowed =
            occurrences
                .filter {
                    it.identityDecision ==
                            SemanticVariantIdentityDecision.IDENTITY_ALLOWED
                }
                .map {
                    it.variantCanonicalKey
                }

        val review =
            occurrences
                .filter {
                    it.identityDecision ==
                            SemanticVariantIdentityDecision.REVIEW
                }
                .map {
                    it.variantCanonicalKey
                }

        val projectedBaseIdentity =
            buildProjectedBaseIdentity(
                family = family,
                occurrences = occurrences
            )

        val entry =
            SemanticIdentitySeparationEntry(
                itemName = itemName,
                normalizedItem = normalizedItem,
                category = category,
                family = family,

                knowledgeOnlyVariantKeys =
                    knowledgeOnly.sorted(),

                identityVariantKeys =
                    identityAllowed.sorted(),

                reviewVariantKeys =
                    review.sorted(),

                hasKnowledgeOnlyVariant =
                    knowledgeOnly.isNotEmpty(),

                hasIdentityVariant =
                    identityAllowed.isNotEmpty(),

                hasReviewVariant =
                    review.isNotEmpty(),

                projectedBaseIdentity =
                    projectedBaseIdentity
            )

        return entry to occurrences
    }

    private fun buildProjectedBaseIdentity(
        family: String,
        occurrences:
        List<SemanticIdentitySeparationOccurrence>
    ): String {

        val retainedVariantKeys =
            occurrences
                .filter {
                    it.identityDecision !=
                            SemanticVariantIdentityDecision.KNOWLEDGE_ONLY
                }
                .map {
                    it.variantCanonicalKey
                }
                .sorted()

        return buildList {
            add(
                SemanticVariantTaxonomyRegistry
                    .normalizeLookupKey(family)
            )
            addAll(
                retainedVariantKeys
            )
        }
            .joinToString("::")
    }

    private fun JsonObject.string(
        key: String
    ): String? {

        val value =
            get(key)
                ?: return null

        if (
            value.isJsonNull ||
            !value.isJsonPrimitive
        ) {
            return null
        }

        return value
            .asString
            .trim()
            .takeIf(String::isNotBlank)
    }

    private companion object {

        const val VARIANT_SEPARATOR =
            " – "
    }
}