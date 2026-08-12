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

data class HighConfidenceNutritionReviewResolutionReport(
    val version: Int,
    val catalogEntryCount: Int,
    val inputAcceptedCount: Int,
    val inputRejectedCount: Int,
    val inputReviewCount: Int,
    val resolvedReviewCount: Int,
    val remainingReviewCount: Int,
    val finalAcceptedCount: Int,
    val finalRejectedCount: Int,
    val groupsWithAcceptedVariants: Int,
    val groupsWithoutAcceptedVariants: Int,
    val resolutionReasonCounts: Map<String, Int>,
    val resolvedMembershipFile: String
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}

class ResolveHighConfidenceNutritionSourceVariantReviews {

    fun resolve(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default()
    ): HighConfidenceNutritionReviewResolutionReport {

        val membershipFile =
            paths.projectRoot.resolve(
                "build/knowledge/source-variant-groups/" +
                        "nutrition.source-variant-membership.json"
            )

        require(
            membershipFile.isFile
        ) {
            "Nutrition source-variant membership file does not exist: " +
                    membershipFile.absolutePath
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

        val groups =
            readMembershipGroups(
                file =
                    membershipFile
            )

        require(
            groups.size ==
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_ENTRY_COUNT
        ) {
            "Unexpected Nutrition membership group count: " +
                    groups.size
        }

        var resolvedReviewCount =
            0

        val resolutionReasonCounts =
            mutableMapOf<String, Int>()

        val resolvedGroups =
            groups.map { group ->

                val newlyAccepted =
                    mutableListOf<
                            CanonicalNutritionSourceVariantMembership
                            >()

                val remainingReviews =
                    mutableListOf<
                            CanonicalNutritionSourceVariantMembership
                            >()

                group.reviewSourceVariants
                    .forEach { review ->

                        val resolved =
                            resolveReview(
                                review =
                                    review,
                                canonicalTerms =
                                    canonicalTermsByKey[
                                        group.catalogKey
                                    ].orEmpty()
                            )

                        if (
                            resolved != null
                        ) {

                            newlyAccepted +=
                                resolved

                            resolvedReviewCount++

                            val reason =
                                resolved.reason.name

                            resolutionReasonCounts[
                                reason
                            ] =
                                resolutionReasonCounts
                                    .getOrDefault(
                                        reason,
                                        0
                                    ) + 1

                        } else {

                            remainingReviews +=
                                review
                        }
                    }

                CanonicalNutritionSourceVariantMembershipGroup(
                    catalogKey =
                        group.catalogKey,

                    canonicalName =
                        group.canonicalName,

                    acceptedSourceVariants =
                        (
                                group.acceptedSourceVariants +
                                        newlyAccepted
                                )
                            .distinctBy {
                                it.serverKey
                            }
                            .sortedWith(
                                ACCEPTED_ORDER
                            ),

                    rejectedSourceVariants =
                        group.rejectedSourceVariants
                            .distinctBy {
                                it.serverKey
                            }
                            .sortedBy {
                                it.serverKey
                            },

                    reviewSourceVariants =
                        remainingReviews
                            .distinctBy {
                                it.serverKey
                            }
                            .sortedWith(
                                REVIEW_ORDER
                            )
                )
            }
                .sortedBy {
                    it.catalogKey
                }

        val resolvedValidation =
            CanonicalNutritionSourceVariantMembershipValidation(
                version =
                    CanonicalNutritionSourceVariantMembershipValidation
                        .CURRENT_VERSION,

                catalogEntryCount =
                    resolvedGroups.size,

                groups =
                    resolvedGroups
            )

        val outputDirectory =
            paths.projectRoot.resolve(
                "build/knowledge/source-variant-groups"
            )

        require(
            outputDirectory.exists() ||
                    outputDirectory.mkdirs()
        ) {
            "Could not create source-variant group directory: " +
                    outputDirectory.absolutePath
        }

        val resolvedMembershipFile =
            outputDirectory.resolve(
                "nutrition.source-variant-membership.resolved.json"
            )

        writeJson(
            value =
                resolvedValidation,
            file =
                resolvedMembershipFile
        )

        val inputAcceptedCount =
            groups.sumOf {
                it.acceptedSourceVariants.size
            }

        val inputRejectedCount =
            groups.sumOf {
                it.rejectedSourceVariants.size
            }

        val inputReviewCount =
            groups.sumOf {
                it.reviewSourceVariants.size
            }

        val finalAcceptedCount =
            resolvedGroups.sumOf {
                it.acceptedSourceVariants.size
            }

        val finalRejectedCount =
            resolvedGroups.sumOf {
                it.rejectedSourceVariants.size
            }

        val remainingReviewCount =
            resolvedGroups.sumOf {
                it.reviewSourceVariants.size
            }

        require(
            inputAcceptedCount +
                    inputRejectedCount +
                    inputReviewCount ==
                    finalAcceptedCount +
                    finalRejectedCount +
                    remainingReviewCount
        ) {
            "Nutrition review resolution changed total variant arithmetic."
        }

        require(
            finalAcceptedCount ==
                    inputAcceptedCount +
                    resolvedReviewCount
        ) {
            "Resolved reviews were not transferred exactly into ACCEPT."
        }

        val report =
            HighConfidenceNutritionReviewResolutionReport(
                version =
                    HighConfidenceNutritionReviewResolutionReport
                        .CURRENT_VERSION,

                catalogEntryCount =
                    resolvedGroups.size,

                inputAcceptedCount =
                    inputAcceptedCount,

                inputRejectedCount =
                    inputRejectedCount,

                inputReviewCount =
                    inputReviewCount,

                resolvedReviewCount =
                    resolvedReviewCount,

                remainingReviewCount =
                    remainingReviewCount,

                finalAcceptedCount =
                    finalAcceptedCount,

                finalRejectedCount =
                    finalRejectedCount,

                groupsWithAcceptedVariants =
                    resolvedGroups.count {
                        it.acceptedSourceVariants
                            .isNotEmpty()
                    },

                groupsWithoutAcceptedVariants =
                    resolvedGroups.count {
                        it.acceptedSourceVariants
                            .isEmpty()
                    },

                resolutionReasonCounts =
                    resolutionReasonCounts
                        .toSortedMap(),

                resolvedMembershipFile =
                    resolvedMembershipFile.path
            )

        val reportFile =
            paths.reportsRoot.resolve(
                "nutrition-source-variant-review-resolution.json"
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

    private fun resolveReview(
        review:
        CanonicalNutritionSourceVariantMembership,
        canonicalTerms: List<String>
    ): CanonicalNutritionSourceVariantMembership? {

        /*
         * Nur die beiden großen unsicheren Evidence-Klassen
         * werden hier deterministisch betrachtet.
         *
         * AMBIGUOUS_PRODUCT_IDENTITY bleibt REVIEW.
         */
        if (
            review.reason !=
            CanonicalNutritionSourceVariantMembershipReason
                .MATCH_ALIAS_ONLY &&
            review.reason !=
            CanonicalNutritionSourceVariantMembershipReason
                .INSUFFICIENT_IDENTITY_EVIDENCE
        ) {
            return null
        }

        val directTerms =
            buildList {

                add(
                    DirectSourceTerm(
                        value =
                            review.serverKey,
                        fromAlias =
                            false
                    )
                )

                review.aliases.forEach { alias ->

                    add(
                        DirectSourceTerm(
                            value =
                                alias,
                            fromAlias =
                                true
                        )
                    )
                }
            }
                .map { term ->

                    term.copy(
                        value =
                            normalize(
                                term.value
                            )
                    )
                }
                .filter {
                    it.value.isNotBlank()
                }
                .distinctBy {
                    it.value
                }

        if (
            directTerms.isEmpty() ||
            canonicalTerms.isEmpty()
        ) {
            return null
        }

        /*
         * High-confidence promotion is forbidden if the
         * direct source identity itself describes a
         * conflicting/derived/ambiguous product.
         */
        if (
            directTerms.any {
                containsBlockingIdentityToken(
                    it.value
                )
            }
        ) {
            return null
        }

        /*
         * Case 1:
         *
         * puremaple syrup
         *      ↓
         * maple syrup
         *
         * Whitespace/tokenization defect only.
         */
        findCompactIdentityMatch(
            canonicalTerms =
                canonicalTerms,
            sourceTerms =
                directTerms
        )
            ?.let { evidence ->

                return promote(
                    review =
                        review,
                    reason =
                        if (
                            evidence.fromAlias
                        ) {
                            CanonicalNutritionSourceVariantMembershipReason
                                .HIGH_CONFIDENCE_SOURCE_ALIAS
                        } else {
                            CanonicalNutritionSourceVariantMembershipReason
                                .HIGH_CONFIDENCE_COMPACT_IDENTITY
                        },
                    confidence =
                        if (
                            evidence.fromAlias
                        ) {
                            0.97
                        } else {
                            0.98
                        }
                )
            }

        /*
         * Case 2:
         *
         * marple syrup
         *      ↓
         * maple syrup
         *
         * Exactly one small typo is tolerated.
         */
        findSingleTypoIdentityMatch(
            canonicalTerms =
                canonicalTerms,
            sourceTerms =
                directTerms
        )
            ?.let { evidence ->

                return promote(
                    review =
                        review,
                    reason =
                        if (
                            evidence.fromAlias
                        ) {
                            CanonicalNutritionSourceVariantMembershipReason
                                .HIGH_CONFIDENCE_SOURCE_ALIAS
                        } else {
                            CanonicalNutritionSourceVariantMembershipReason
                                .HIGH_CONFIDENCE_TYPO_IDENTITY
                        },
                    confidence =
                        if (
                            evidence.fromAlias
                        ) {
                            0.96
                        } else {
                            0.97
                        }
                )
            }

        return null
    }

    private fun findCompactIdentityMatch(
        canonicalTerms: List<String>,
        sourceTerms: List<DirectSourceTerm>
    ): DirectSourceTerm? {

        val canonicalCollapsed =
            canonicalTerms
                .asSequence()
                .map(
                    ::collapseIdentity
                )
                .filter {
                    it.length >=
                            MINIMUM_COLLAPSED_IDENTITY_LENGTH
                }
                .distinct()
                .toList()

        if (
            canonicalCollapsed.isEmpty()
        ) {
            return null
        }

        return sourceTerms
            .firstOrNull { sourceTerm ->

                val collapsedSource =
                    collapseIdentity(
                        sourceTerm.value
                    )

                canonicalCollapsed.any { canonical ->

                    /*
                     * Do not use this for ordinary token containment;
                     * that was already handled by the primary
                     * membership validator.
                     *
                     * This specifically recovers token-boundary
                     * defects such as "puremaple".
                     */
                    collapsedSource.contains(
                        canonical
                    ) &&
                            !containsCanonicalAsTokenSequence(
                                canonicalTerms =
                                    canonicalTerms,
                                sourceTerm =
                                    sourceTerm.value
                            )
                }
            }
    }

    private fun findSingleTypoIdentityMatch(
        canonicalTerms: List<String>,
        sourceTerms: List<DirectSourceTerm>
    ): DirectSourceTerm? {

        canonicalTerms.forEach { canonicalTerm ->

            val canonicalTokens =
                tokenize(
                    canonicalTerm
                )

            if (
                canonicalTokens.isEmpty()
            ) {
                return@forEach
            }

            sourceTerms.forEach { sourceTerm ->

                val sourceTokens =
                    tokenize(
                        sourceTerm.value
                    )

                if (
                    sourceTokens.isEmpty()
                ) {
                    return@forEach
                }

                var fuzzyReplacementCount =
                    0

                val allCanonicalTokensCovered =
                    canonicalTokens.all { canonicalToken ->

                        if (
                            canonicalToken in
                            sourceTokens
                        ) {
                            true

                        } else {

                            val fuzzyMatches =
                                sourceTokens.filter { sourceToken ->

                                    isSafeSingleTypo(
                                        canonicalToken =
                                            canonicalToken,
                                        sourceToken =
                                            sourceToken
                                    )
                                }

                            if (
                                fuzzyMatches.size ==
                                1
                            ) {

                                fuzzyReplacementCount++

                                true

                            } else {

                                false
                            }
                        }
                    }

                if (
                    allCanonicalTokensCovered &&
                    fuzzyReplacementCount ==
                    1
                ) {
                    return sourceTerm
                }
            }
        }

        return null
    }

    private fun isSafeSingleTypo(
        canonicalToken: String,
        sourceToken: String
    ): Boolean {

        if (
            canonicalToken.length <
            MINIMUM_FUZZY_TOKEN_LENGTH ||
            sourceToken.length <
            MINIMUM_FUZZY_TOKEN_LENGTH
        ) {
            return false
        }

        if (
            kotlin.math.abs(
                canonicalToken.length -
                        sourceToken.length
            ) > 1
        ) {
            return false
        }

        return levenshteinDistance(
            left =
                canonicalToken,
            right =
                sourceToken
        ) <=
                MAXIMUM_FUZZY_DISTANCE
    }

    private fun containsCanonicalAsTokenSequence(
        canonicalTerms: List<String>,
        sourceTerm: String
    ): Boolean {

        val sourceTokens =
            tokenize(
                sourceTerm
            )

        return canonicalTerms.any { canonicalTerm ->

            val canonicalTokens =
                tokenize(
                    canonicalTerm
                )

            canonicalTokens.isNotEmpty() &&
                    sourceTokens.containsAll(
                        canonicalTokens
                    )
        }
    }

    private fun containsBlockingIdentityToken(
        value: String
    ): Boolean {

        val tokens =
            tokenize(
                value
            )

        return tokens.any {
            it in
                    BLOCKING_IDENTITY_TOKENS
        }
    }

    private fun promote(
        review:
        CanonicalNutritionSourceVariantMembership,
        reason:
        CanonicalNutritionSourceVariantMembershipReason,
        confidence: Double
    ): CanonicalNutritionSourceVariantMembership {

        return review.copy(
            decision =
                CanonicalNutritionSourceVariantMembershipDecision
                    .ACCEPT,

            reason =
                reason,

            confidence =
                confidence
                    .coerceIn(
                        0.0,
                        1.0
                    ),

            competingCatalogKey =
                null
        )
    }

    private fun readMembershipGroups(
        file: File
    ): List<CanonicalNutritionSourceVariantMembershipGroup> {

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )
                .asJsonObject

        return root
            .requiredArray(
                key =
                    "groups"
            )
            .map { groupElement ->

                val group =
                    groupElement.asJsonObject

                CanonicalNutritionSourceVariantMembershipGroup(
                    catalogKey =
                        group.requiredString(
                            key =
                                "catalogKey"
                        ),

                    canonicalName =
                        group.requiredString(
                            key =
                                "canonicalName"
                        ),

                    acceptedSourceVariants =
                        group.readMembershipArray(
                            key =
                                "acceptedSourceVariants"
                        ),

                    rejectedSourceVariants =
                        group.readMembershipArray(
                            key =
                                "rejectedSourceVariants"
                        ),

                    reviewSourceVariants =
                        group.readMembershipArray(
                            key =
                                "reviewSourceVariants"
                        )
                )
            }
    }

    private fun JsonObject.readMembershipArray(
        key: String
    ): List<CanonicalNutritionSourceVariantMembership> =
        requiredArray(
            key =
                key
        )
            .map { element ->

                val json =
                    element.asJsonObject

                CanonicalNutritionSourceVariantMembership(
                    catalogKey =
                        json.requiredString(
                            "catalogKey"
                        ),

                    canonicalName =
                        json.requiredString(
                            "canonicalName"
                        ),

                    serverKey =
                        json.requiredString(
                            "serverKey"
                        ),

                    exact =
                        json.requiredBoolean(
                            "exact"
                        ),

                    retrievalScore =
                        json.requiredDouble(
                            "retrievalScore"
                        ),

                    decision =
                        CanonicalNutritionSourceVariantMembershipDecision
                            .valueOf(
                                json.requiredString(
                                    "decision"
                                )
                            ),

                    reason =
                        CanonicalNutritionSourceVariantMembershipReason
                            .valueOf(
                                json.requiredString(
                                    "reason"
                                )
                            ),

                    confidence =
                        json.requiredDouble(
                            "confidence"
                        ),

                    matchedSourceTerm =
                        json.requiredString(
                            "matchedSourceTerm"
                        ),

                    sharedTokens =
                        json.stringArray(
                            "sharedTokens"
                        ),

                    aliases =
                        json.stringArray(
                            "aliases"
                        ),

                    matchAliases =
                        json.stringArray(
                            "matchAliases"
                        ),

                    competingCatalogKey =
                        json.optionalString(
                            "competingCatalogKey"
                        )
                )
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

    private fun collapseIdentity(
        value: String
    ): String =
        normalize(
            value
        )
            .replace(
                " ",
                ""
            )

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

    private fun levenshteinDistance(
        left: String,
        right: String
    ): Int {

        if (
            left ==
            right
        ) {
            return 0
        }

        if (
            left.isEmpty()
        ) {
            return right.length
        }

        if (
            right.isEmpty()
        ) {
            return left.length
        }

        var previous =
            IntArray(
                right.length + 1
            ) {
                it
            }

        left.forEachIndexed { leftIndex, leftCharacter ->

            val current =
                IntArray(
                    right.length + 1
                )

            current[0] =
                leftIndex + 1

            right.forEachIndexed { rightIndex, rightCharacter ->

                current[
                    rightIndex + 1
                ] =
                    minOf(
                        current[
                            rightIndex
                        ] + 1,

                        previous[
                            rightIndex + 1
                        ] + 1,

                        previous[
                            rightIndex
                        ] +
                                if (
                                    leftCharacter ==
                                    rightCharacter
                                ) {
                                    0
                                } else {
                                    1
                                }
                    )
            }

            previous =
                current
        }

        return previous[
            right.length
        ]
    }

    private fun writeJson(
        value: Any,
        file: File
    ) {

        val parent =
            requireNotNull(
                file.parentFile
            ) {
                "Output file has no parent: " +
                        file.absolutePath
            }

        require(
            parent.exists() ||
                    parent.mkdirs()
        ) {
            "Could not create output directory: " +
                    parent.absolutePath
        }

        file.writeText(
            gson.toJson(
                value
            ) + "\n"
        )
    }

    private fun printReport(
        report:
        HighConfidenceNutritionReviewResolutionReport,
        reportFile: File
    ) {

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("HIGH-CONFIDENCE NUTRITION REVIEW RESOLUTION")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Catalog entries        : " +
                    report.catalogEntryCount
        )
        println(
            "Input ACCEPT           : " +
                    report.inputAcceptedCount
        )
        println(
            "Input REJECT           : " +
                    report.inputRejectedCount
        )
        println(
            "Input REVIEW           : " +
                    report.inputReviewCount
        )
        println(
            "Resolved REVIEW        : " +
                    report.resolvedReviewCount
        )
        println(
            "Remaining REVIEW       : " +
                    report.remainingReviewCount
        )
        println(
            "Final ACCEPT           : " +
                    report.finalAcceptedCount
        )
        println(
            "Groups with ACCEPT     : " +
                    report.groupsWithAcceptedVariants
        )
        println(
            "Groups without ACCEPT  : " +
                    report.groupsWithoutAcceptedVariants
        )
        println()

        report.resolutionReasonCounts
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
            "Resolved membership    : " +
                    report.resolvedMembershipFile
        )
        println(
            "Report                 : " +
                    reportFile.path
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
    }

    private fun JsonObject.requiredString(
        key: String
    ): String =
        optionalString(
            key
        )
            ?: error(
                "Missing or blank '$key'."
            )

    private fun JsonObject.optionalString(
        key: String
    ): String? =
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
                "Missing array '$key'."
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

    private data class DirectSourceTerm(
        val value: String,
        val fromAlias: Boolean
    )

    companion object {

        private const val MINIMUM_COLLAPSED_IDENTITY_LENGTH =
            6

        private const val MINIMUM_FUZZY_TOKEN_LENGTH =
            5

        private const val MAXIMUM_FUZZY_DISTANCE =
            1

        private const val MINIMUM_SINGULARIZATION_LENGTH =
            4

        /*
         * REVIEWs containing these tokens are deliberately
         * not auto-promoted.
         *
         * Ambiguous processing terms are included as well:
         * they need the later semantic resolver rather than
         * a fuzzy typo rule.
         */
        private val BLOCKING_IDENTITY_TOKENS =
            setOf(
                "cider",

                "nectar",
                "nektar",

                "drink",
                "getrank",
                "getraenk",
                "beverage",

                "smoothie",

                "concentrate",
                "konzentrat",

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
                "biscuit",

                "cake",
                "kuchen",

                "mix",
                "mixe",
                "mixture",

                "sauce",
                "dressing"
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

        private val ACCEPTED_ORDER =
            compareByDescending<
                    CanonicalNutritionSourceVariantMembership
                    > {
                it.confidence
            }
                .thenByDescending {
                    it.retrievalScore
                }
                .thenBy {
                    it.serverKey
                }

        private val REVIEW_ORDER =
            compareByDescending<
                    CanonicalNutritionSourceVariantMembership
                    > {
                it.retrievalScore
            }
                .thenByDescending {
                    it.confidence
                }
                .thenBy {
                    it.serverKey
                }
    }
}