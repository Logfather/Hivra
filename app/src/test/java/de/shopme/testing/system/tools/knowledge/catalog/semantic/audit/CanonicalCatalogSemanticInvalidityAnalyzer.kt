package de.shopme.testing.system.tools.knowledge.catalog.semantic.audit

import com.google.gson.JsonArray
import com.google.gson.JsonObject

class CanonicalCatalogSemanticInvalidityAnalyzer {

    fun analyze(
        catalog: JsonArray,
        inputFile: String
    ): CanonicalCatalogSemanticInvalidityReport {

        val issues =
            catalog
                .mapNotNull { element ->
                    element
                        .takeIf { it.isJsonObject }
                        ?.asJsonObject
                }
                .flatMap(::analyzeEntry)
                .distinctBy { issue ->
                    listOf(
                        issue.normalized,
                        issue.reason.name,
                        issue.explanation
                    )
                }
                .sortedWith(
                    compareBy<CanonicalCatalogSemanticInvalidity>(
                        { it.category },
                        { it.normalized },
                        { it.reason.name },
                        { it.severity.name }
                    )
                )

        val semanticIssues =
            issues.filter {
                it.domain ==
                        CanonicalCatalogSemanticInvalidityDomain.PRODUCT_IDENTITY
            }

        val metadataIssues =
            issues.filter {
                it.domain ==
                        CanonicalCatalogSemanticInvalidityDomain.METADATA
            }

        val semanticInvalidEntries =
            semanticIssues
                .filter {
                    it.severity ==
                            CanonicalCatalogSemanticInvaliditySeverity.ERROR
                }
                .map { it.normalized }
                .toSet()

        val semanticReviewEntries =
            semanticIssues
                .filter {
                    it.severity ==
                            CanonicalCatalogSemanticInvaliditySeverity.REVIEW
                }
                .map { it.normalized }
                .toSet()

        val metadataInvalidEntries =
            metadataIssues
                .filter {
                    it.severity ==
                            CanonicalCatalogSemanticInvaliditySeverity.ERROR
                }
                .map { it.normalized }
                .toSet()

        val metadataReviewEntries =
            metadataIssues
                .filter {
                    it.severity ==
                            CanonicalCatalogSemanticInvaliditySeverity.REVIEW
                }
                .map { it.normalized }
                .toSet()

        val countsByReason =
            issues
                .groupingBy { it.reason.name }
                .eachCount()
                .toSortedMap()

        val countsByCategory =
            issues
                .groupingBy { it.category }
                .eachCount()
                .toSortedMap()

        val semanticCountsByCategory =
            semanticIssues
                .groupingBy { it.category }
                .eachCount()
                .toSortedMap()

        return CanonicalCatalogSemanticInvalidityReport(
            schemaVersion = 2,
            inputFile = inputFile,
            catalogEntryCount = catalog.size(),

            semanticInvalidEntryCount = semanticInvalidEntries.size,
            semanticReviewEntryCount = semanticReviewEntries.size,
            metadataInvalidEntryCount = metadataInvalidEntries.size,
            metadataReviewEntryCount = metadataReviewEntries.size,

            semanticIssueCount = semanticIssues.size,
            metadataIssueCount = metadataIssues.size,
            issueCount = issues.size,

            countsByReason = countsByReason,
            countsByCategory = countsByCategory,
            semanticCountsByCategory = semanticCountsByCategory,

            issues = issues
        )
    }

    private fun analyzeEntry(
        json: JsonObject
    ): List<CanonicalCatalogSemanticInvalidity> {

        val itemName =
            json.string("itemname")
                ?: return emptyList()

        val normalized =
            json.string("normalized")
                ?: return emptyList()

        val category =
            json.string("category")
                ?: return emptyList()

        val parts =
            itemName
                .split(" – ")
                .map(String::trim)
                .filter(String::isNotBlank)

        val family =
            parts.firstOrNull()
                ?: itemName

        val variants =
            parts.drop(1)

        if (variants.isEmpty()) {
            return emptyList()
        }

        val normalizedVariants =
            variants.map(::normalize)

        return buildList {

            addGenericSemanticPlaceholderIssues(
                itemName = itemName,
                normalized = normalized,
                category = category,
                family = family,
                variants = variants,
                normalizedVariants = normalizedVariants
            )

            addKnowledgeAttributeIssues(
                itemName = itemName,
                normalized = normalized,
                category = category,
                family = family,
                variants = variants,
                normalizedVariants = normalizedVariants
            )

            addCrossDomainIssues(
                itemName = itemName,
                normalized = normalized,
                category = category,
                family = family,
                variants = variants,
                normalizedVariants = normalizedVariants
            )

            addNonIdentityProcessingIssues(
                itemName = itemName,
                normalized = normalized,
                category = category,
                family = family,
                variants = variants,
                normalizedVariants = normalizedVariants
            )

            addSyntheticCombinationIssues(
                itemName = itemName,
                normalized = normalized,
                category = category,
                family = family,
                variants = variants,
                normalizedVariants = normalizedVariants
            )

            addColloquialLeakIssues(
                json = json,
                itemName = itemName,
                normalized = normalized,
                category = category,
                family = family,
                variants = variants
            )
        }
    }

    private fun MutableList<CanonicalCatalogSemanticInvalidity>
            .addGenericSemanticPlaceholderIssues(
        itemName: String,
        normalized: String,
        category: String,
        family: String,
        variants: List<String>,
        normalizedVariants: List<String>
    ) {

        val matches =
            normalizedVariants
                .filter {
                    it in GENERIC_SEMANTIC_PLACEHOLDERS
                }

        if (matches.isEmpty()) {
            return
        }

        add(
            error(
                itemName = itemName,
                normalized = normalized,
                category = category,
                family = family,
                variants = variants,
                reason =
                    CanonicalCatalogSemanticInvalidityReason
                        .GENERIC_SEMANTIC_PLACEHOLDER,
                explanation =
                    "Generic semantic placeholder materialized as " +
                            "catalog identity: ${matches.joinToString()}"
            )
        )
    }

    private fun MutableList<CanonicalCatalogSemanticInvalidity>
            .addKnowledgeAttributeIssues(
        itemName: String,
        normalized: String,
        category: String,
        family: String,
        variants: List<String>,
        normalizedVariants: List<String>
    ) {

        val matches =
            normalizedVariants
                .filter {
                    it in KNOWLEDGE_ATTRIBUTES
                }

        if (matches.isEmpty()) {
            return
        }

        add(
            error(
                itemName = itemName,
                normalized = normalized,
                category = category,
                family = family,
                variants = variants,
                reason =
                    CanonicalCatalogSemanticInvalidityReason
                        .KNOWLEDGE_ATTRIBUTE_AS_PRODUCT,
                explanation =
                    "Knowledge attribute materialized as independent " +
                            "catalog identity: ${matches.joinToString()}"
            )
        )
    }

    private fun MutableList<CanonicalCatalogSemanticInvalidity>
            .addCrossDomainIssues(
        itemName: String,
        normalized: String,
        category: String,
        family: String,
        variants: List<String>,
        normalizedVariants: List<String>
    ) {

        val prohibited =
            CROSS_DOMAIN_PROHIBITIONS[category]
                ?: emptySet()

        val matches =
            normalizedVariants.filter {
                it in prohibited
            }

        if (matches.isEmpty()) {
            return
        }

        add(
            error(
                itemName = itemName,
                normalized = normalized,
                category = category,
                family = family,
                variants = variants,
                reason =
                    CanonicalCatalogSemanticInvalidityReason
                        .CROSS_DOMAIN_VARIANT,
                explanation =
                    "Variant is incompatible with category '$category': " +
                            matches.joinToString()
            )
        )
    }

    private fun MutableList<CanonicalCatalogSemanticInvalidity>
            .addNonIdentityProcessingIssues(
        itemName: String,
        normalized: String,
        category: String,
        family: String,
        variants: List<String>,
        normalizedVariants: List<String>
    ) {

        val familyNormalized =
            normalize(family)

        val matches =
            normalizedVariants.filter { variant ->

                variant in NON_IDENTITY_PROCESSING &&
                        isNonIdentityProcessing(
                            category = category,
                            family = familyNormalized,
                            variant = variant
                        )
            }

        if (matches.isEmpty()) {
            return
        }

        add(
            error(
                itemName = itemName,
                normalized = normalized,
                category = category,
                family = family,
                variants = variants,
                reason =
                    CanonicalCatalogSemanticInvalidityReason
                        .NON_IDENTITY_PROCESSING_VARIANT,
                explanation =
                    "Processing state does not create a useful canonical " +
                            "identity for this family: ${matches.joinToString()}"
            )
        )
    }

    private fun MutableList<CanonicalCatalogSemanticInvalidity>
            .addSyntheticCombinationIssues(
        itemName: String,
        normalized: String,
        category: String,
        family: String,
        variants: List<String>,
        normalizedVariants: List<String>
    ) {

        if (variants.size < 2) {
            return
        }

        val suspiciousCount =
            normalizedVariants.count { variant ->
                variant in GENERIC_SEMANTIC_PLACEHOLDERS ||
                        variant in KNOWLEDGE_ATTRIBUTES ||
                        variant in NON_IDENTITY_PROCESSING
            }

        if (suspiciousCount < 2) {
            return
        }

        add(
            error(
                itemName = itemName,
                normalized = normalized,
                category = category,
                family = family,
                variants = variants,
                reason =
                    CanonicalCatalogSemanticInvalidityReason
                        .SYNTHETIC_VARIANT_COMBINATION,
                explanation =
                    "Multiple non-identity semantic values were " +
                            "combinatorially materialized."
            )
        )
    }

    private fun MutableList<CanonicalCatalogSemanticInvalidity>
            .addColloquialLeakIssues(
        json: JsonObject,
        itemName: String,
        normalized: String,
        category: String,
        family: String,
        variants: List<String>
    ) {

        val colloquial =
            json
                .getAsJsonArray("colloquial")
                ?.mapNotNull { value ->
                    value
                        .takeIf { it.isJsonPrimitive }
                        ?.asString
                }
                ?.map(::normalize)
                ?.toSet()
                ?: emptySet()

        val normalizedVariants =
            variants
                .map(::normalize)
                .toSet()

        if (
            colloquial.isEmpty() ||
            normalizedVariants.isEmpty()
        ) {
            return
        }

        if (
            colloquial.intersect(normalizedVariants).isEmpty()
        ) {
            return
        }

        add(
            review(
                itemName = itemName,
                normalized = normalized,
                category = category,
                family = family,
                variants = variants,
                reason =
                    CanonicalCatalogSemanticInvalidityReason
                        .VARIANT_LEAKED_INTO_COLLOQUIAL,
                explanation =
                    "Variant values are also stored as colloquial " +
                            "product names."
            )
        )
    }

    private fun isNonIdentityProcessing(
        category: String,
        family: String,
        variant: String
    ): Boolean {

        return when (variant) {

            "gebacken" ->
                category == "bakery"

            "gekocht" ->
                category in setOf(
                    "dairy",
                    "cheese"
                )

            "geschnitten" ->
                family in setOf(
                    "butter",
                    "frischkäse",
                    "frischkaese"
                )

            else ->
                false
        }
    }

    private fun error(
        itemName: String,
        normalized: String,
        category: String,
        family: String,
        variants: List<String>,
        reason: CanonicalCatalogSemanticInvalidityReason,
        explanation: String
    ) =
        CanonicalCatalogSemanticInvalidity(
            itemName = itemName,
            normalized = normalized,
            category = category,
            domain =
                CanonicalCatalogSemanticInvalidityDomain.PRODUCT_IDENTITY,
            severity =
                CanonicalCatalogSemanticInvaliditySeverity.ERROR,
            reason = reason,
            family = family,
            variants = variants,
            explanation = explanation
        )

    private fun review(
        itemName: String,
        normalized: String,
        category: String,
        family: String,
        variants: List<String>,
        reason: CanonicalCatalogSemanticInvalidityReason,
        explanation: String
    ) =
        CanonicalCatalogSemanticInvalidity(
            itemName = itemName,
            normalized = normalized,
            category = category,
            domain =
                CanonicalCatalogSemanticInvalidityDomain.METADATA,
            severity =
                CanonicalCatalogSemanticInvaliditySeverity.REVIEW,
            reason = reason,
            family = family,
            variants = variants,
            explanation = explanation
        )

    private fun normalize(
        value: String
    ): String =
        value
            .trim()
            .lowercase()
            .replace('ä', 'a')
            .replace('ö', 'o')
            .replace('ü', 'u')
            .replace('ß', 's')
            .replace(WHITESPACE_REGEX, " ")
            .trim()

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

        val WHITESPACE_REGEX =
            Regex("\\s+")

        val GENERIC_SEMANTIC_PLACEHOLDERS =
            setOf(
                "fischbasiert",
                "kasebasiert",
                "fruchtbasiert",
                "kultiviertes lebensmittel",
                "uberzogenes lebensmittel"
            )

        val KNOWLEDGE_ATTRIBUTES =
            setOf(
                "glutenfrei",
                "laktosefrei",
                "selleriefrei",
                "proteinreich",
                "eiweisreich",
                "fettarm",
                "fettreich",
                "zuckerfrei",
                "zuckerarm",
                "salzarm"
            )

        val NON_IDENTITY_PROCESSING =
            setOf(
                "gebacken",
                "gekocht",
                "geschnitten"
            )

        val CROSS_DOMAIN_PROHIBITIONS =
            mapOf(
                "bakery" to
                        setOf(
                            "fischbasiert"
                        ),
                "baking-ingredients" to
                        setOf(
                            "rind",
                            "fischbasiert",
                            "kasebasiert"
                        )
            )
    }
}