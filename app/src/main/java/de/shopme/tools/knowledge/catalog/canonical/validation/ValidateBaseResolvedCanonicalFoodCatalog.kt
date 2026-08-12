package de.shopme.tools.knowledge.catalog.canonical.validation

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.canonical.rebuild.model.CanonicalFoodIdentity
import de.shopme.tools.knowledge.catalog.canonical.validation.model.CanonicalFoodSemanticEntryValidation
import de.shopme.tools.knowledge.catalog.canonical.validation.model.CanonicalFoodSemanticIssue
import de.shopme.tools.knowledge.catalog.canonical.validation.model.CanonicalFoodSemanticIssueType
import de.shopme.tools.knowledge.catalog.canonical.validation.model.CanonicalFoodSemanticSeverity
import de.shopme.tools.knowledge.catalog.canonical.validation.model.CanonicalFoodSemanticValidationResult
import java.io.File

class ValidateBaseResolvedCanonicalFoodCatalog(
    private val categoryRegistry:
    CanonicalFoodCategorySemanticRegistry =
        CanonicalFoodCategorySemanticRegistry(),

    private val identityNormalizer:
    CanonicalFoodSemanticIdentityNormalizer =
        CanonicalFoodSemanticIdentityNormalizer()
) {

    fun validate(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default(),

        inputFile: File =
            paths.projectRoot.resolve(
                "build/knowledge/catalog/base-resolved/" +
                        "canonical-food-catalog.base-resolved.json"
            ),

        reportFile: File =
            paths.reportsRoot.resolve(
                "base-resolved-canonical-food-semantic-validation.json"
            )
    ): CanonicalFoodSemanticValidationResult {

        require(
            inputFile.isFile
        ) {
            "Canonical food catalog not found: " +
                    inputFile.absolutePath
        }

        val entries =
            readCatalog(
                inputFile
            )

        val duplicateKeys =
            entries
                .groupBy { entry ->

                    identityNormalizer
                        .normalize(
                            entry.itemname
                        )
                }
                .filterValues { group ->

                    group.size >
                            1
                }
                .keys

        val validations =
            entries.map { entry ->

                validateEntry(
                    entry =
                        entry,

                    duplicateSemanticKeys =
                        duplicateKeys
                )
            }

        val issues =
            validations.flatMap {
                it.issues
            }

        val result =
            CanonicalFoodSemanticValidationResult(
                version =
                    1,

                catalogEntryCount =
                    entries.size,

                validEntryCount =
                    validations.count {
                        it.issues.isEmpty()
                    },

                invalidEntryCount =
                    validations.count { validation ->

                        validation.issues.any {
                            it.severity ==
                                    CanonicalFoodSemanticSeverity.ERROR
                        }
                    },

                reviewEntryCount =
                    validations.count { validation ->

                        validation.issues.none {
                            it.severity ==
                                    CanonicalFoodSemanticSeverity.ERROR
                        } &&
                                validation.issues.isNotEmpty()
                    },

                errorIssueCount =
                    issues.count {
                        it.severity ==
                                CanonicalFoodSemanticSeverity.ERROR
                    },

                reviewIssueCount =
                    issues.count {
                        it.severity ==
                                CanonicalFoodSemanticSeverity.REVIEW
                    },

                issueCounts =
                    issues
                        .groupingBy {
                            it.type.name
                        }
                        .eachCount()
                        .toSortedMap(),

                entries =
                    validations
            )

        writeJson(
            value =
                result,

            file =
                reportFile
        )

        printReport(
            result =
                result,

            reportFile =
                reportFile
        )

        return result
    }

    private fun validateEntry(
        entry: CanonicalFoodIdentity,
        duplicateSemanticKeys: Set<String>
    ): CanonicalFoodSemanticEntryValidation {

        val issues =
            mutableListOf<CanonicalFoodSemanticIssue>()

        NON_IDENTITY_META_TOKENS
            .firstOrNull { token ->

                Regex(
                    """(^|\s)$token(\s|$)""",
                    RegexOption.IGNORE_CASE
                )
                    .containsMatchIn(
                        entry.itemname
                    )
            }
            ?.let { token ->

                issues +=
                    error(
                        entry =
                            entry,

                        type =
                            CanonicalFoodSemanticIssueType
                                .KNOWLEDGE_ATTRIBUTE_IN_IDENTITY,

                        message =
                            "Knowledge/meta attribute '$token' remains " +
                                    "in canonical identity."
                    )
            }

        TOO_SPECIFIC_PATTERNS
            .firstOrNull { pattern ->

                pattern
                    .containsMatchIn(
                        entry.itemname
                    )
            }
            ?.let { pattern ->

                issues +=
                    review(
                        entry =
                            entry,

                        type =
                            CanonicalFoodSemanticIssueType
                                .TOO_SPECIFIC_PRODUCT_IDENTITY,

                        message =
                            "Potentially non-identity product-state pattern " +
                                    "remains: '${pattern.pattern}'."
                    )
            }

        if (
            identityNormalizer
                .normalize(
                    entry.itemname
                ) in
            duplicateSemanticKeys
        ) {

            issues +=
                error(
                    entry =
                        entry,

                    type =
                        CanonicalFoodSemanticIssueType
                            .DUPLICATE_BASE_IDENTITY,

                    message =
                        "Another canonical entry resolves to the same " +
                                "semantic base identity."
                )
        }

        val expectedCategories =
            categoryRegistry
                .expectedCategories(
                    entry.itemname
                )

        if (
            expectedCategories.isNotEmpty() &&
            entry.category !in
            expectedCategories
        ) {

            issues +=
                error(
                    entry =
                        entry,

                    type =
                        CanonicalFoodSemanticIssueType
                            .INVALID_CATEGORY,

                    message =
                        "Category '${entry.category}' conflicts with " +
                                "expected categories " +
                                expectedCategories.sorted()
                )
        }

        entry.variants
            .filter { variant ->

                variant !in
                        ALLOWED_VARIANTS
            }
            .forEach { variant ->

                issues +=
                    review(
                        entry =
                            entry,

                        type =
                            CanonicalFoodSemanticIssueType
                                .INVALID_VARIANT,

                        message =
                            "Unknown/unvalidated canonical variant: " +
                                    "'$variant'."
                    )
            }

        if (
            entry.sourceVariants
                .any { sourceVariant ->

                    sourceVariant.equals(
                        entry.itemname,
                        ignoreCase =
                            true
                    )
                }
        ) {

            issues +=
                error(
                    entry =
                        entry,

                    type =
                        CanonicalFoodSemanticIssueType
                            .SOURCE_VARIANT_EQUALS_CANONICAL_NAME,

                    message =
                        "sourceVariants contains canonical itemname."
                )
        }

        val duplicateSourceVariants =
            entry.sourceVariants
                .groupBy { sourceVariant ->

                    identityNormalizer
                        .normalize(
                            sourceVariant
                        )
                }
                .filterValues { group ->

                    group.size >
                            1
                }

        if (
            duplicateSourceVariants.isNotEmpty()
        ) {

            issues +=
                review(
                    entry =
                        entry,

                    type =
                        CanonicalFoodSemanticIssueType
                            .DUPLICATE_SOURCE_VARIANT,

                    message =
                        "Semantically duplicate sourceVariants exist."
                )
        }

        explicitlyImplausible(
            entry
        )
            ?.let { message ->

                issues +=
                    error(
                        entry =
                            entry,

                        type =
                            CanonicalFoodSemanticIssueType
                                .SEMANTICALLY_IMPLAUSIBLE_IDENTITY,

                        message =
                            message
                    )
            }

        return CanonicalFoodSemanticEntryValidation(
            catalogKey =
                entry.normalized,

            itemname =
                entry.itemname,

            category =
                entry.category,

            valid =
                issues.none {
                    it.severity ==
                            CanonicalFoodSemanticSeverity.ERROR
                },

            issues =
                issues
        )
    }

    private fun explicitlyImplausible(
        entry: CanonicalFoodIdentity
    ): String? {

        val normalized =
            semanticIdentityKey(
                entry.itemname
            )

        if (
            "cocktailtomate" in
            normalized &&
            "tiefgekühlt" in
            entry.variants
        ) {

            return "Frozen cocktail tomatoes are not a plausible " +
                    "regular supermarket product."
        }

        return null
    }

    private fun semanticIdentityKey(
        value: String
    ): String =
        value
            .lowercase()
            .replace(
                "ä",
                "ae"
            )
            .replace(
                "ö",
                "oe"
            )
            .replace(
                "ü",
                "ue"
            )
            .replace(
                "ß",
                "ss"
            )
            .replace(
                Regex(
                    """[^a-z0-9]+"""
                ),
                ""
            )

    private fun error(
        entry: CanonicalFoodIdentity,
        type: CanonicalFoodSemanticIssueType,
        message: String
    ): CanonicalFoodSemanticIssue =
        CanonicalFoodSemanticIssue(
            catalogKey =
                entry.normalized,

            itemname =
                entry.itemname,

            severity =
                CanonicalFoodSemanticSeverity.ERROR,

            type =
                type,

            message =
                message
        )

    private fun review(
        entry: CanonicalFoodIdentity,
        type: CanonicalFoodSemanticIssueType,
        message: String
    ): CanonicalFoodSemanticIssue =
        CanonicalFoodSemanticIssue(
            catalogKey =
                entry.normalized,

            itemname =
                entry.itemname,

            severity =
                CanonicalFoodSemanticSeverity.REVIEW,

            type =
                type,

            message =
                message
        )

    private fun readCatalog(
        file: File
    ): List<CanonicalFoodIdentity> {

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )

        require(
            root.isJsonArray
        ) {
            "Canonical food catalog root must be an array: " +
                    file.absolutePath
        }

        return root
            .asJsonArray
            .map { element ->

                require(
                    element.isJsonObject
                ) {
                    "Canonical food catalog entry must be an object."
                }

                val json =
                    element.asJsonObject

                CanonicalFoodIdentity(
                    itemname =
                        json.requiredString(
                            "itemname"
                        ),

                    normalized =
                        json.requiredString(
                            "normalized"
                        ),

                    category =
                        json.requiredString(
                            "category"
                        ),

                    variants =
                        json.stringList(
                            "variants"
                        ),

                    sourceVariants =
                        json.stringList(
                            "sourceVariants"
                        )
                )
            }
    }

    private fun JsonObject.requiredString(
        key: String
    ): String =
        get(key)
            ?.takeUnless {
                it.isJsonNull
            }
            ?.takeIf {
                it.isJsonPrimitive
            }
            ?.asString
            ?.trim()
            ?.takeIf(
                String::isNotBlank
            )
            ?: error(
                "Missing or empty '$key'."
            )

    private fun JsonObject.stringList(
        key: String
    ): List<String> =
        get(key)
            ?.takeIf {
                it.isJsonArray
            }
            ?.asJsonArray
            ?.mapNotNull { element ->

                element
                    .takeIf {
                        it.isJsonPrimitive
                    }
                    ?.asString
                    ?.trim()
                    ?.takeIf(
                        String::isNotBlank
                    )
            }
            .orEmpty()

    private fun writeJson(
        value: Any,
        file: File
    ) {

        val parent =
            requireNotNull(
                file.parentFile
            ) {
                "Output file has no parent directory: " +
                        file.path
            }

        require(
            parent.exists() ||
                    parent.mkdirs()
        ) {
            "Unable to create output directory: " +
                    parent.path
        }

        file.writeText(
            gson.toJson(
                value
            ) + "\n"
        )
    }

    private fun printReport(
        result: CanonicalFoodSemanticValidationResult,
        reportFile: File
    ) {

        println()
        println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        )
        println(
            "CANONICAL FOOD CATALOG SEMANTIC VALIDATION"
        )
        println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        )
        println(
            "Catalog entries      : " +
                    result.catalogEntryCount
        )
        println(
            "Valid entries        : " +
                    result.validEntryCount
        )
        println(
            "Invalid entries      : " +
                    result.invalidEntryCount
        )
        println(
            "Review entries       : " +
                    result.reviewEntryCount
        )
        println(
            "ERROR issues         : " +
                    result.errorIssueCount
        )
        println(
            "REVIEW issues        : " +
                    result.reviewIssueCount
        )
        println()

        result.issueCounts
            .forEach { (type, count) ->

                println(
                    type.padEnd(
                        38
                    ) +
                            count
                                .toString()
                                .padStart(
                                    6
                                )
                )
            }

        println()
        println(
            "Input catalog        : " +
                    reportInputDescription(
                        result =
                            result
                    )
        )
        println(
            "Report               : " +
                    reportFile.path
        )
        println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        )
        println()
    }

    private fun reportInputDescription(
        result: CanonicalFoodSemanticValidationResult
    ): String =
        "${result.catalogEntryCount} entries"

    companion object {

        private val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        private val NON_IDENTITY_META_TOKENS =
            setOf(
                "bio",
                "standard",
                "organic",
                "premium"
            )

        private val TOO_SPECIFIC_PATTERNS =
            listOf(
                Regex(
                    """(?i)(^|\s)(tk|tiefkühl|tiefgekühlt|gefroren)(\s|$)"""
                ),

                Regex(
                    """(?i)(^|\s)(geschnitten|gewürfelt|geraspelt)(\s|$)"""
                ),

                /*
                 * "frisch" ausschließlich als eigenes Wort.
                 *
                 * Frischkäse, Frischmilch, Frischhefe usw.
                 * bleiben dadurch unangetastet.
                 */
                Regex(
                    """(?i)(^|\s)frisch(\s|$)"""
                )
            )

        private val ALLOWED_VARIANTS =
            setOf(
                "frisch",
                "tiefgekühlt",
                "getrocknet",
                "geräuchert",
                "konserviert",
                "geschnitten",
                "gewürfelt",
                "geraspelt",
                "gemahlen",
                "naturtrüb",
                "klar",
                "aus Konzentrat"
            )
    }
}