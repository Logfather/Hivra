package de.shopme.tools.knowledge.mapping.catalog.nutrition.group

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.mapping.catalog.retrieval.CanonicalCatalogRetrievalIdentityReader
import java.io.File
import java.text.Normalizer
import java.util.Locale

enum class CanonicalNutritionSourceVariantMembershipDecision {

    ACCEPT,

    REJECT,

    REVIEW
}

enum class CanonicalNutritionSourceVariantMembershipReason {

    EXACT_MAPPING,

    DIRECT_IDENTITY_EXACT,

    DIRECT_IDENTITY_COMPATIBLE,

    MORE_SPECIFIC_CANONICAL_IDENTITY_EXISTS,

    DERIVED_PRODUCT_IDENTITY,

    AMBIGUOUS_PRODUCT_IDENTITY,

    CONFLICTING_PRODUCT_IDENTITY,

    HIGH_CONFIDENCE_COMPACT_IDENTITY,

    HIGH_CONFIDENCE_TYPO_IDENTITY,

    HIGH_CONFIDENCE_SOURCE_ALIAS,

    MATCH_ALIAS_ONLY,

    INSUFFICIENT_IDENTITY_EVIDENCE
}

data class CanonicalNutritionSourceVariantMembership(
    val catalogKey: String,
    val canonicalName: String,
    val serverKey: String,
    val exact: Boolean,
    val retrievalScore: Double,
    val decision:
    CanonicalNutritionSourceVariantMembershipDecision,
    val reason:
    CanonicalNutritionSourceVariantMembershipReason,
    val confidence: Double,
    val matchedSourceTerm: String,
    val sharedTokens: List<String>,
    val aliases: List<String>,
    val matchAliases: List<String>,
    val competingCatalogKey: String? = null
)

data class CanonicalNutritionSourceVariantMembershipGroup(
    val catalogKey: String,
    val canonicalName: String,
    val acceptedSourceVariants:
    List<CanonicalNutritionSourceVariantMembership>,
    val rejectedSourceVariants:
    List<CanonicalNutritionSourceVariantMembership>,
    val reviewSourceVariants:
    List<CanonicalNutritionSourceVariantMembership>
)

data class CanonicalNutritionSourceVariantMembershipValidation(
    val version: Int,
    val catalogEntryCount: Int,
    val groups:
    List<CanonicalNutritionSourceVariantMembershipGroup>
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}

data class CanonicalNutritionSourceVariantMembershipValidationReport(
    val version: Int,
    val catalogEntryCount: Int,
    val inputVariantCount: Int,
    val acceptedVariantCount: Int,
    val rejectedVariantCount: Int,
    val reviewVariantCount: Int,
    val groupsWithAcceptedVariants: Int,
    val groupsWithoutAcceptedVariants: Int,
    val reasonCounts: Map<String, Int>,
    val validationFile: String
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}

class ValidateCanonicalNutritionSourceVariantMembership {

    fun validate(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default()
    ): CanonicalNutritionSourceVariantMembershipValidationReport {

        val discoveryFile =
            paths.projectRoot.resolve(
                "build/knowledge/source-variant-groups/" +
                        "nutrition.source-variant-discovery.json"
            )

        require(
            discoveryFile.isFile
        ) {
            "Nutrition source-variant discovery does not exist: " +
                    discoveryFile.absolutePath
        }

        val canonicalIdentities =
            CanonicalCatalogRetrievalIdentityReader()
                .read(
                    file =
                        paths.canonicalFoodCatalog
                )

        val canonicalTermsByKey =
            canonicalIdentities
                .associate { identity ->

                    identity.catalogKey to
                            identity.retrievalTerms
                                .asSequence()
                                .map(
                                    ::normalize
                                )
                                .filter(
                                    String::isNotBlank
                                )
                                .distinct()
                                .toList()
                }

        val canonicalTermIndex =
            buildCanonicalTermIndex(
                canonicalTermsByKey =
                    canonicalTermsByKey
            )

        val discovery =
            readDiscovery(
                file =
                    discoveryFile
            )

        require(
            discovery.size ==
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_ENTRY_COUNT
        ) {
            "Unexpected discovery group count: " +
                    discovery.size
        }

        val groups =
            discovery
                .map { group ->

                    val decisions =
                        group.sourceVariants
                            .map { variant ->

                                validateVariant(
                                    group =
                                        group,
                                    variant =
                                        variant,
                                    canonicalTermsByKey =
                                        canonicalTermsByKey,
                                    canonicalTermIndex =
                                        canonicalTermIndex
                                )
                            }
                            .sortedWith(
                                MEMBERSHIP_ORDER
                            )

                    CanonicalNutritionSourceVariantMembershipGroup(
                        catalogKey =
                            group.catalogKey,

                        canonicalName =
                            group.canonicalName,

                        acceptedSourceVariants =
                            decisions.filter {
                                it.decision ==
                                        CanonicalNutritionSourceVariantMembershipDecision
                                            .ACCEPT
                            },

                        rejectedSourceVariants =
                            decisions.filter {
                                it.decision ==
                                        CanonicalNutritionSourceVariantMembershipDecision
                                            .REJECT
                            },

                        reviewSourceVariants =
                            decisions.filter {
                                it.decision ==
                                        CanonicalNutritionSourceVariantMembershipDecision
                                            .REVIEW
                            }
                    )
                }
                .sortedBy {
                    it.catalogKey
                }

        val result =
            CanonicalNutritionSourceVariantMembershipValidation(
                version =
                    CanonicalNutritionSourceVariantMembershipValidation
                        .CURRENT_VERSION,

                catalogEntryCount =
                    groups.size,

                groups =
                    groups
            )

        val outputDirectory =
            paths.projectRoot.resolve(
                "build/knowledge/source-variant-groups"
            )

        require(
            outputDirectory.exists() ||
                    outputDirectory.mkdirs()
        )

        val validationFile =
            outputDirectory.resolve(
                "nutrition.source-variant-membership.json"
            )

        writeJson(
            value =
                result,
            file =
                validationFile
        )

        val allDecisions =
            groups.flatMap { group ->
                group.acceptedSourceVariants +
                        group.rejectedSourceVariants +
                        group.reviewSourceVariants
            }

        val report =
            CanonicalNutritionSourceVariantMembershipValidationReport(
                version =
                    CanonicalNutritionSourceVariantMembershipValidationReport
                        .CURRENT_VERSION,

                catalogEntryCount =
                    groups.size,

                inputVariantCount =
                    allDecisions.size,

                acceptedVariantCount =
                    allDecisions.count {
                        it.decision ==
                                CanonicalNutritionSourceVariantMembershipDecision
                                    .ACCEPT
                    },

                rejectedVariantCount =
                    allDecisions.count {
                        it.decision ==
                                CanonicalNutritionSourceVariantMembershipDecision
                                    .REJECT
                    },

                reviewVariantCount =
                    allDecisions.count {
                        it.decision ==
                                CanonicalNutritionSourceVariantMembershipDecision
                                    .REVIEW
                    },

                groupsWithAcceptedVariants =
                    groups.count {
                        it.acceptedSourceVariants
                            .isNotEmpty()
                    },

                groupsWithoutAcceptedVariants =
                    groups.count {
                        it.acceptedSourceVariants
                            .isEmpty()
                    },

                reasonCounts =
                    allDecisions
                        .groupingBy {
                            it.reason.name
                        }
                        .eachCount()
                        .toSortedMap(),

                validationFile =
                    validationFile.path
            )

        val reportFile =
            paths.reportsRoot.resolve(
                "nutrition-source-variant-membership-validation.json"
            )

        writeJson(
            value =
                report,
            file =
                reportFile
        )

        printReport(
            report =
                report,
            reportFile =
                reportFile
        )

        return report
    }

    private fun validateVariant(
        group: DiscoveryGroup,
        variant: DiscoveryVariant,
        canonicalTermsByKey:
        Map<String, List<String>>,
        canonicalTermIndex:
        List<CanonicalIndexedTerm>
    ): CanonicalNutritionSourceVariantMembership {

        if (
            variant.exact
        ) {
            return membership(
                group =
                    group,
                variant =
                    variant,
                decision =
                    CanonicalNutritionSourceVariantMembershipDecision
                        .ACCEPT,
                reason =
                    CanonicalNutritionSourceVariantMembershipReason
                        .EXACT_MAPPING,
                confidence =
                    1.0
            )
        }

        val canonicalTerms =
            canonicalTermsByKey[
                group.catalogKey
            ].orEmpty()

        val directSourceTerms =
            buildList {

                add(
                    variant.serverKey
                )

                addAll(
                    variant.aliases
                )
            }
                .asSequence()
                .map(
                    ::normalize
                )
                .filter(
                    String::isNotBlank
                )
                .distinct()
                .toList()

        /*
         * Ein MatchAlias darf Discovery ermöglichen,
         * ist aber keine ausreichende direkte
         * Product-Identity-Evidenz.
         */
        if (
            directSourceTerms.isEmpty()
        ) {
            return membership(
                group =
                    group,
                variant =
                    variant,
                decision =
                    CanonicalNutritionSourceVariantMembershipDecision
                        .REVIEW,
                reason =
                    CanonicalNutritionSourceVariantMembershipReason
                        .MATCH_ALIAS_ONLY,
                confidence =
                    reviewConfidence(
                        variant.retrievalScore
                    )
            )
        }

        val exactDirectMatch =
            directSourceTerms.any { sourceTerm ->

                canonicalTerms.any { canonicalTerm ->

                    sourceTerm ==
                            canonicalTerm
                }
            }

        if (
            exactDirectMatch
        ) {
            return membership(
                group =
                    group,
                variant =
                    variant,
                decision =
                    CanonicalNutritionSourceVariantMembershipDecision
                        .ACCEPT,
                reason =
                    CanonicalNutritionSourceVariantMembershipReason
                        .DIRECT_IDENTITY_EXACT,
                confidence =
                    1.0
            )
        }

        val bestCompatibleTerm =
            findBestCompatibleTerm(
                canonicalTerms =
                    canonicalTerms,
                directSourceTerms =
                    directSourceTerms
            )

        if (
            bestCompatibleTerm ==
            null
        ) {

            val matchedOnlyThroughAlias =
                variant.matchAliases
                    .asSequence()
                    .map(
                        ::normalize
                    )
                    .any { matchAlias ->

                        canonicalTerms.any { canonicalTerm ->

                            compatibleIdentity(
                                canonicalTerm =
                                    canonicalTerm,
                                sourceTerm =
                                    matchAlias
                            )
                        }
                    }

            return membership(
                group =
                    group,
                variant =
                    variant,
                decision =
                    CanonicalNutritionSourceVariantMembershipDecision
                        .REVIEW,
                reason =
                    if (
                        matchedOnlyThroughAlias
                    ) {
                        CanonicalNutritionSourceVariantMembershipReason
                            .MATCH_ALIAS_ONLY
                    } else {
                        CanonicalNutritionSourceVariantMembershipReason
                            .INSUFFICIENT_IDENTITY_EVIDENCE
                    },
                confidence =
                    reviewConfidence(
                        variant.retrievalScore
                    )
            )
        }

        val canonicalTokens =
            tokenize(
                bestCompatibleTerm.canonicalTerm
            )

        val sourceTokens =
            tokenize(
                bestCompatibleTerm.sourceTerm
            )

        val additionalTokens =
            sourceTokens -
                    canonicalTokens

        val conflictingIdentityTokens =
            additionalTokens
                .intersect(
                    CONFLICTING_IDENTITY_TOKENS
                )

        if (
            conflictingIdentityTokens.isNotEmpty()
        ) {

            return membership(
                group =
                    group,
                variant =
                    variant,
                decision =
                    CanonicalNutritionSourceVariantMembershipDecision
                        .REJECT,
                reason =
                    CanonicalNutritionSourceVariantMembershipReason
                        .CONFLICTING_PRODUCT_IDENTITY,
                confidence =
                    0.99
            )
        }

        val ambiguousIdentityTokens =
            additionalTokens
                .intersect(
                    AMBIGUOUS_IDENTITY_TOKENS
                )

        if (
            ambiguousIdentityTokens.isNotEmpty()
        ) {

            return membership(
                group =
                    group,
                variant =
                    variant,
                decision =
                    CanonicalNutritionSourceVariantMembershipDecision
                        .REVIEW,
                reason =
                    CanonicalNutritionSourceVariantMembershipReason
                        .AMBIGUOUS_PRODUCT_IDENTITY,
                confidence =
                    reviewConfidence(
                        variant.retrievalScore
                    )
            )
        }

        /*
         * Aus Agar-Agar darf z.B. nicht automatisch
         * Agar Candy / Agar Dessert Mix werden.
         */
        val derivedProductTokens =
            additionalTokens
                .intersect(
                    DERIVED_PRODUCT_TOKENS
                )

        if (
            derivedProductTokens.isNotEmpty()
        ) {
            return membership(
                group =
                    group,
                variant =
                    variant,
                decision =
                    CanonicalNutritionSourceVariantMembershipDecision
                        .REJECT,
                reason =
                    CanonicalNutritionSourceVariantMembershipReason
                        .DERIVED_PRODUCT_IDENTITY,
                confidence =
                    0.99
            )
        }

        /*
         * Prüfen, ob die Source-Identity eigentlich
         * zu einem spezifischeren kanonischen Begriff
         * gehört.
         *
         * Beispiel:
         *
         * generic apple juice
         * vs.
         * cloudy apple juice
         *
         * Wenn "cloudy apple juice" selbst kanonisch
         * existiert, darf es nicht automatisch in die
         * allgemeinere Apfelsaft-Gruppe fallen.
         */
        val competingIdentity =
            findMoreSpecificCanonicalIdentity(
                currentCatalogKey =
                    group.catalogKey,
                currentCanonicalTerms =
                    canonicalTerms,
                sourceTerms =
                    directSourceTerms,
                currentCanonicalTokenCount =
                    canonicalTokens.size,
                canonicalTermIndex =
                    canonicalTermIndex
            )

        if (
            competingIdentity !=
            null
        ) {
            return membership(
                group =
                    group,
                variant =
                    variant,
                decision =
                    CanonicalNutritionSourceVariantMembershipDecision
                        .REJECT,
                reason =
                    CanonicalNutritionSourceVariantMembershipReason
                        .MORE_SPECIFIC_CANONICAL_IDENTITY_EXISTS,
                confidence =
                    0.99,
                competingCatalogKey =
                    competingIdentity
            )
        }

        return membership(
            group =
                group,
            variant =
                variant,
            decision =
                CanonicalNutritionSourceVariantMembershipDecision
                    .ACCEPT,
            reason =
                CanonicalNutritionSourceVariantMembershipReason
                    .DIRECT_IDENTITY_COMPATIBLE,
            confidence =
                compatibleConfidence(
                    retrievalScore =
                        variant.retrievalScore,
                    canonicalTokenCount =
                        canonicalTokens.size,
                    sourceTokenCount =
                        sourceTokens.size
                )
        )
    }

    private fun findBestCompatibleTerm(
        canonicalTerms: List<String>,
        directSourceTerms: List<String>
    ): CompatibleTerm? {

        return canonicalTerms
            .asSequence()
            .flatMap { canonicalTerm ->

                directSourceTerms
                    .asSequence()
                    .mapNotNull { sourceTerm ->

                        if (
                            !compatibleIdentity(
                                canonicalTerm =
                                    canonicalTerm,
                                sourceTerm =
                                    sourceTerm
                            )
                        ) {
                            null
                        } else {

                            CompatibleTerm(
                                canonicalTerm =
                                    canonicalTerm,
                                sourceTerm =
                                    sourceTerm,
                                canonicalTokenCount =
                                    tokenize(
                                        canonicalTerm
                                    ).size
                            )
                        }
                    }
            }
            .sortedWith(
                compareByDescending<CompatibleTerm> {
                    it.canonicalTokenCount
                }
                    .thenBy {
                        it.sourceTerm.length
                    }
            )
            .firstOrNull()
    }

    private fun compatibleIdentity(
        canonicalTerm: String,
        sourceTerm: String
    ): Boolean {

        val canonicalTokens =
            tokenize(
                canonicalTerm
            )

        val sourceTokens =
            tokenize(
                sourceTerm
            )

        if (
            canonicalTokens.isEmpty() ||
            sourceTokens.isEmpty()
        ) {
            return false
        }

        return sourceTokens.containsAll(
            canonicalTokens
        )
    }

    private fun productFamilySignature(
        value: String
    ): String? {

        val tokens =
            tokenize(
                value
            )

        if (
            tokens.isEmpty()
        ) {
            return null
        }

        /*
         * Produktbestimmende Tokens haben Vorrang.
         *
         * Dadurch:
         *
         * apple juice
         * granny smith apple juice
         *
         * → family = juice
         *
         * apple
         * granny smith apple
         *
         * → family = apple
         */
        PRODUCT_FAMILY_TOKENS
            .firstOrNull {
                it in tokens
            }
            ?.let {
                return it
            }

        /*
         * Deutsche Komposita des kanonischen Catalogs
         * müssen ebenfalls auf ihre Produktfamilie
         * zurückgeführt werden.
         */
        val normalized =
            normalize(
                value
            )

        COMPOUND_PRODUCT_FAMILIES
            .firstOrNull { (suffix, _) ->

                normalized.endsWith(
                    suffix
                )
            }
            ?.let { (_, family) ->

                return family
            }

        /*
         * Fallback:
         * erstes substantielles Identity-Token.
         */
        return tokens
            .firstOrNull {
                it !in
                        PRODUCT_QUALIFIER_TOKENS
            }
    }

    private fun findMoreSpecificCanonicalIdentity(
        currentCatalogKey: String,
        currentCanonicalTerms: List<String>,
        sourceTerms: List<String>,
        currentCanonicalTokenCount: Int,
        canonicalTermIndex:
        List<CanonicalIndexedTerm>
    ): String? {

        val currentFamilySignatures =
            currentCanonicalTerms
                .mapNotNull(
                    ::productFamilySignature
                )
                .toSet()

        if (
            currentFamilySignatures.isEmpty()
        ) {
            return null
        }

        return canonicalTermIndex
            .asSequence()

            .filter {
                it.catalogKey !=
                        currentCatalogKey
            }

            .filter {
                it.tokens.size >
                        currentCanonicalTokenCount
            }

            /*
             * Entscheidend:
             *
             * "Granny Smith Apple" darf nicht gegen
             * "Apple Juice" konkurrieren.
             *
             * Eine konkurrierende Identity muss zur
             * gleichen Produktfamilie gehören.
             */
            .filter { indexedTerm ->

                val competingFamily =
                    productFamilySignature(
                        indexedTerm.term
                    )
                        ?: return@filter false

                competingFamily in
                        currentFamilySignatures
            }

            .filter { indexedTerm ->

                sourceTerms.any { sourceTerm ->

                    tokenize(
                        sourceTerm
                    )
                        .containsAll(
                            indexedTerm.tokens
                        )
                }
            }

            .sortedWith(
                compareByDescending<CanonicalIndexedTerm> {
                    it.tokens.size
                }
                    .thenBy {
                        it.catalogKey
                    }
            )

            .firstOrNull()
            ?.catalogKey
    }

    private fun buildCanonicalTermIndex(
        canonicalTermsByKey:
        Map<String, List<String>>
    ): List<CanonicalIndexedTerm> {

        return canonicalTermsByKey
            .flatMap { (catalogKey, terms) ->

                terms.mapNotNull { term ->

                    val tokens =
                        tokenize(
                            term
                        )

                    if (
                        tokens.isEmpty()
                    ) {
                        null
                    } else {

                        CanonicalIndexedTerm(
                            catalogKey =
                                catalogKey,
                            term =
                                term,
                            tokens =
                                tokens
                        )
                    }
                }
            }
            .distinctBy {
                it.catalogKey to
                        it.term
            }
            .sortedWith(
                compareBy<CanonicalIndexedTerm> {
                    it.catalogKey
                }
                    .thenBy {
                        it.term
                    }
            )
    }

    private fun membership(
        group: DiscoveryGroup,
        variant: DiscoveryVariant,
        decision:
        CanonicalNutritionSourceVariantMembershipDecision,
        reason:
        CanonicalNutritionSourceVariantMembershipReason,
        confidence: Double,
        competingCatalogKey: String? = null
    ): CanonicalNutritionSourceVariantMembership {

        return CanonicalNutritionSourceVariantMembership(
            catalogKey =
                group.catalogKey,

            canonicalName =
                group.canonicalName,

            serverKey =
                variant.serverKey,

            exact =
                variant.exact,

            retrievalScore =
                variant.retrievalScore,

            decision =
                decision,

            reason =
                reason,

            confidence =
                confidence.coerceIn(
                    0.0,
                    1.0
                ),

            matchedSourceTerm =
                variant.matchedSourceTerm,

            sharedTokens =
                variant.sharedTokens,

            aliases =
                variant.aliases,

            matchAliases =
                variant.matchAliases,

            competingCatalogKey =
                competingCatalogKey
        )
    }

    private fun compatibleConfidence(
        retrievalScore: Double,
        canonicalTokenCount: Int,
        sourceTokenCount: Int
    ): Double {

        val specificity =
            if (
                sourceTokenCount == 0
            ) {
                0.0
            } else {
                canonicalTokenCount.toDouble() /
                        sourceTokenCount.toDouble()
            }

        return (
                retrievalScore *
                        0.75 +
                        specificity *
                        0.25
                )
            .coerceIn(
                0.0,
                0.99
            )
    }

    private fun reviewConfidence(
        retrievalScore: Double
    ): Double =
        (
                retrievalScore *
                        0.75
                )
            .coerceIn(
                0.0,
                0.79
            )

    private fun readDiscovery(
        file: File
    ): List<DiscoveryGroup> {

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )
                .asJsonObject

        return root
            .requiredArray(
                "groups"
            )
            .map { groupElement ->

                val group =
                    groupElement
                        .asJsonObject

                DiscoveryGroup(
                    catalogKey =
                        group.requiredString(
                            "catalogKey"
                        ),

                    canonicalName =
                        group.requiredString(
                            "canonicalName"
                        ),

                    sourceVariants =
                        group
                            .requiredArray(
                                "sourceVariants"
                            )
                            .map { variantElement ->

                                val variant =
                                    variantElement
                                        .asJsonObject

                                DiscoveryVariant(
                                    serverKey =
                                        variant.requiredString(
                                            "serverKey"
                                        ),

                                    exact =
                                        variant.requiredBoolean(
                                            "exact"
                                        ),

                                    retrievalScore =
                                        variant.requiredDouble(
                                            "retrievalScore"
                                        ),

                                    sharedTokens =
                                        variant.stringArray(
                                            "sharedTokens"
                                        ),

                                    matchedSourceTerm =
                                        variant.requiredString(
                                            "matchedSourceTerm"
                                        ),

                                    aliases =
                                        variant.stringArray(
                                            "aliases"
                                        ),

                                    matchAliases =
                                        variant.stringArray(
                                            "matchAliases"
                                        )
                                )
                            }
                )
            }
    }

    private fun normalize(
        value: String
    ): String {

        val decomposed =
            Normalizer.normalize(
                value,
                Normalizer.Form.NFKD
            )

        return decomposed
            .replace(
                DIACRITIC_REGEX,
                ""
            )
            .lowercase(
                Locale.ROOT
            )
            .replace(
                "-",
                " "
            )
            .replace(
                "_",
                " "
            )
            .replace(
                NON_KEY_CHARACTER_REGEX,
                " "
            )
            .replace(
                WHITESPACE_REGEX,
                " "
            )
            .trim()
    }

    private fun tokenize(
        value: String
    ): Set<String> =
        normalize(
            value
        )
            .split(
                " "
            )
            .asSequence()
            .filter(
                String::isNotBlank
            )
            .map(
                ::canonicalToken
            )
            .filter(
                String::isNotBlank
            )
            .toSortedSet()

    private fun canonicalToken(
        value: String
    ): String =
        when (
            value
        ) {

            "yoghurt" ->
                "yogurt"

            "sausages" ->
                "sausage"

            "vegetables" ->
                "vegetable"

            "fruits" ->
                "fruit"

            else ->
                if (
                    value.endsWith(
                        "s"
                    ) &&
                    value.length >
                    MINIMUM_SINGULARIZATION_LENGTH
                ) {
                    value.dropLast(
                        1
                    )
                } else {
                    value
                }
        }

    private fun writeJson(
        value: Any,
        file: File
    ) {

        val parent =
            requireNotNull(
                file.parentFile
            )

        require(
            parent.exists() ||
                    parent.mkdirs()
        )

        file.writeText(
            gson.toJson(
                value
            ) + "\n"
        )
    }

    private fun printReport(
        report:
        CanonicalNutritionSourceVariantMembershipValidationReport,
        reportFile: File
    ) {

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("NUTRITION SOURCE-VARIANT MEMBERSHIP")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Catalog entries         : " +
                    report.catalogEntryCount
        )
        println(
            "Input variants          : " +
                    report.inputVariantCount
        )
        println(
            "Accepted                : " +
                    report.acceptedVariantCount
        )
        println(
            "Rejected                : " +
                    report.rejectedVariantCount
        )
        println(
            "Review                  : " +
                    report.reviewVariantCount
        )
        println(
            "Groups with accepted    : " +
                    report.groupsWithAcceptedVariants
        )
        println(
            "Groups without accepted : " +
                    report.groupsWithoutAcceptedVariants
        )
        println()

        report.reasonCounts
            .forEach { (reason, count) ->

                println(
                    reason.padEnd(
                        44
                    ) +
                            count
                                .toString()
                                .padStart(
                                    8
                                )
                )
            }

        println()
        println(
            "Validation              : " +
                    report.validationFile
        )
        println(
            "Report                  : " +
                    reportFile.path
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
    }

    private fun JsonObject.requiredString(
        key: String
    ): String =
        get(key)
            ?.takeIf {
                !it.isJsonNull &&
                        it.isJsonPrimitive
            }
            ?.asString
            ?.trim()
            ?.takeIf(
                String::isNotBlank
            )
            ?: error(
                "Missing or blank '$key'."
            )

    private fun JsonObject.requiredDouble(
        key: String
    ): Double =
        get(key)
            ?.takeIf {
                !it.isJsonNull &&
                        it.isJsonPrimitive &&
                        it
                            .asJsonPrimitive
                            .isNumber
            }
            ?.asDouble
            ?: error(
                "Missing numeric '$key'."
            )

    private fun JsonObject.requiredBoolean(
        key: String
    ): Boolean =
        get(key)
            ?.takeIf {
                !it.isJsonNull &&
                        it.isJsonPrimitive &&
                        it
                            .asJsonPrimitive
                            .isBoolean
            }
            ?.asBoolean
            ?: error(
                "Missing boolean '$key'."
            )

    private fun JsonObject.requiredArray(
        key: String
    ): JsonArray =
        get(key)
            ?.takeIf {
                it.isJsonArray
            }
            ?.asJsonArray
            ?: error(
                "Missing JSON array '$key'."
            )

    private fun JsonObject.stringArray(
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
            ?.distinct()
            ?.sorted()
            .orEmpty()

    private data class DiscoveryGroup(
        val catalogKey: String,
        val canonicalName: String,
        val sourceVariants: List<DiscoveryVariant>
    )

    private data class DiscoveryVariant(
        val serverKey: String,
        val exact: Boolean,
        val retrievalScore: Double,
        val sharedTokens: List<String>,
        val matchedSourceTerm: String,
        val aliases: List<String>,
        val matchAliases: List<String>
    )

    private data class CompatibleTerm(
        val canonicalTerm: String,
        val sourceTerm: String,
        val canonicalTokenCount: Int
    )

    private data class CanonicalIndexedTerm(
        val catalogKey: String,
        val term: String,
        val tokens: Set<String>
    )

    companion object {

        private val CONFLICTING_IDENTITY_TOKENS =
            setOf(
                "nectar",
                "nektar",
                "drink",
                "getrank",
                "getraenk",
                "beverage",
                "smoothie",
                "soda",
                "lemonade",
                "limonade"
            )

        private val AMBIGUOUS_IDENTITY_TOKENS =
            setOf(
                "cider",
                "concentrate",
                "konzentrat"
            )

        private val PRODUCT_FAMILY_TOKENS =
            listOf(
                "juice",
                "saft",

                "syrup",
                "sirup",

                "milk",
                "milch",

                "cheese",
                "kase",
                "kaese",

                "yogurt",
                "joghurt",

                "bread",
                "brot",

                "flour",
                "mehl",

                "oil",
                "ol",
                "oel",

                "vinegar",
                "essig",

                "rice",
                "reis",

                "pasta",
                "nudel",

                "sausage",
                "wurst",

                "ham",
                "schinken",

                "butter",

                "cream",
                "sahne"
            )

        private val COMPOUND_PRODUCT_FAMILIES =
            listOf(
                "saft" to
                        "juice",

                "sirup" to
                        "syrup",

                "milch" to
                        "milk",

                "kaese" to
                        "cheese",

                "kase" to
                        "cheese",

                "joghurt" to
                        "yogurt",

                "brot" to
                        "bread",

                "mehl" to
                        "flour",

                "oel" to
                        "oil",

                "ol" to
                        "oil",

                "essig" to
                        "vinegar",

                "reis" to
                        "rice",

                "wurst" to
                        "sausage",

                "schinken" to
                        "ham"
            )

        private val PRODUCT_QUALIFIER_TOKENS =
            setOf(
                "100",
                "pure",
                "organic",
                "bio",
                "fresh",
                "raw",
                "natural",
                "classic",
                "original",
                "premium",
                "grade",
                "amber",
                "dark",
                "light",
                "red",
                "green",
                "yellow",
                "white",
                "black"
            )

        private const val MINIMUM_SINGULARIZATION_LENGTH =
            4

        /*
         * Diese Tokens beschreiben typischerweise ein
         * neues Produkt, das die Canonical Identity nur
         * als Zutat / Geschmack / Basis verwendet.
         *
         * Regel:
         * nur dann REJECT, wenn das Token zusätzlich
         * zur Canonical Identity im Source-Term auftaucht.
         */
        private val DERIVED_PRODUCT_TOKENS =
            setOf(
                "candy",
                "bonbon",
                "dessert",
                "jelly",
                "gelee",
                "topping",
                "granola",
                "muesli",
                "musli",
                "icecream",
                "eis",
                "snack",
                "cracker",
                "cookie",
                "cookies",
                "biscuit",
                "cake",
                "kuchen",
                "riegel",
                "mix",
                "mixe",
                "mixture",
                "sauce",
                "dressing",
                "nectar",
                "nektar"
            )

        private val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        private val DIACRITIC_REGEX =
            Regex("\\p{M}+")

        private val NON_KEY_CHARACTER_REGEX =
            Regex("[^\\p{L}\\p{N} ]+")

        private val WHITESPACE_REGEX =
            Regex("\\s+")

        private val MEMBERSHIP_ORDER =
            compareBy<
                    CanonicalNutritionSourceVariantMembership
                    > {
                it.catalogKey
            }
                .thenBy {
                    it.decision.ordinal
                }
                .thenByDescending {
                    it.confidence
                }
                .thenBy {
                    it.serverKey
                }
    }
}