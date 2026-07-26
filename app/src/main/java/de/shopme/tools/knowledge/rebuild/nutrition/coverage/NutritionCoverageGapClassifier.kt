package de.shopme.tools.knowledge.rebuild.nutrition.coverage

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecisionSource
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecisionType
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeSnapshotReader
import java.io.File

class NutritionCoverageGapClassifier(
    private val catalogFile: File,
    private val exactMappingFile: File,
    private val catalogServerMappingFile: File,
    private val exactMatchReportFile: File,
    private val requestFile: File,
    private val decisionFile: File,
    private val snapshotReader: NutritionKnowledgeSnapshotReader
) {

    fun classify():
            NutritionCoverageGapReport {

        val snapshot =
            snapshotReader.read()

        val catalogKeys =
            readCatalogKeys(
                file =
                    catalogFile
            )

        val exactCatalogKeys =
            readMappingCatalogKeys(
                file =
                    exactMappingFile,
                sourceName =
                    "exact nutrition mapping file",
                requireFile =
                    true
            )
                .intersect(
                    catalogKeys
                )

        val mappedCatalogKeys =
            readMappingCatalogKeys(
                file =
                    catalogServerMappingFile,
                sourceName =
                    "catalog-server mapping file",
                requireFile =
                    false
            )
                .intersect(
                    catalogKeys
                )
                .minus(
                    exactCatalogKeys
                )

        val coveredCatalogKeys =
            (
                    exactCatalogKeys +
                            mappedCatalogKeys
                    )
                .toSortedSet()

        val missingCatalogKeys =
            catalogKeys
                .minus(
                    coveredCatalogKeys
                )
                .toSortedSet()

        val reportedExactMatchCatalogKeys =
            readExactMatchReportCatalogKeys(
                file =
                    exactMatchReportFile
            )
                .intersect(
                    catalogKeys
                )

        require(
            catalogKeys.size ==
                    snapshot.catalogItemCount
        ) {
            "Catalog key count differs from rebuild snapshot: " +
                    "classifier=${catalogKeys.size}, " +
                    "snapshot=${snapshot.catalogItemCount}."
        }

        require(
            coveredCatalogKeys.size ==
                    snapshot.coveredCatalogItemCount
        ) {
            "Covered catalog key count differs from rebuild snapshot: " +
                    "classifier=${coveredCatalogKeys.size}, " +
                    "snapshot=${snapshot.coveredCatalogItemCount}."
        }

        require(
            missingCatalogKeys.size ==
                    snapshot.missingCatalogItemCount
        ) {
            "Missing catalog key count differs from rebuild snapshot: " +
                    "classifier=${missingCatalogKeys.size}, " +
                    "snapshot=${snapshot.missingCatalogItemCount}."
        }

        val requestsByCatalogKey =
            readRequests(
                file =
                    requestFile
            )
                .associateBy {
                    it.catalogKey
                }

        val decisionsByCatalogKey =
            readDecisions(
                file =
                    decisionFile
            )
                .associateBy {
                    it.catalogKey
                }

        val gaps =
            missingCatalogKeys
                .map { catalogKey ->

                    classifyGap(
                        catalogKey =
                            catalogKey,
                        exactMatchReported =
                            catalogKey in
                                    reportedExactMatchCatalogKeys,
                        request =
                            requestsByCatalogKey[
                                catalogKey
                            ],
                        decision =
                            decisionsByCatalogKey[
                                catalogKey
                            ]
                    )
                }
                .sortedBy {
                    it.catalogKey
                }

        val unclassifiedGapCount =
            gaps.count {
                it.type ==
                        NutritionCoverageGapType.UNKNOWN
            }

        val report =
            NutritionCoverageGapReport(
                version =
                    NutritionCoverageGapReport.CURRENT_VERSION,
                catalogItemCount =
                    snapshot.catalogItemCount,
                coveredCatalogItemCount =
                    snapshot.coveredCatalogItemCount,
                missingCatalogItemCount =
                    snapshot.missingCatalogItemCount,
                classifiedGapCount =
                    gaps.size -
                            unclassifiedGapCount,
                unclassifiedGapCount =
                    unclassifiedGapCount,
                countsByType =
                    gaps
                        .groupingBy {
                            it.type.name
                        }
                        .eachCount()
                        .toSortedMap(),
                gaps =
                    gaps
            )

        require(
            report.gaps.size ==
                    snapshot.missingCatalogItemCount
        ) {
            "Coverage gap report does not cover every missing " +
                    "catalog key."
        }

        return report
    }

    private fun classifyGap(
        catalogKey: String,
        exactMatchReported: Boolean,
        request: PersistedRequest?,
        decision: PersistedDecision?
    ): NutritionCoverageGap {

        val candidates =
            request
                ?.candidates
                .orEmpty()

        val topCandidate =
            candidates.firstOrNull()

        val secondCandidate =
            candidates.getOrNull(
                index =
                    1
            )

        val topScoreDelta =
            if (
                topCandidate != null &&
                secondCandidate != null
            ) {
                (
                        topCandidate.diagnosticScore -
                                secondCandidate.diagnosticScore
                        )
                    .coerceAtLeast(
                        minimumValue =
                            0.0
                    )
            } else {
                null
            }

        val classification =
            determineClassification(
                catalogKey =
                    catalogKey,
                exactMatchReported =
                    exactMatchReported,
                request =
                    request,
                decision =
                    decision,
                topCandidate =
                    topCandidate,
                topScoreDelta =
                    topScoreDelta
            )

        return NutritionCoverageGap(
            catalogKey =
                catalogKey,
            type =
                classification.type,
            noMatchCause =
                classification.noMatchCause,
            requestExists =
                request != null,
            decisionExists =
                decision != null,
            decisionType =
                decision
                    ?.type
                    ?.name,
            decisionSource =
                decision
                    ?.decisionSource
                    ?.name,
            selectedServerKey =
                decision
                    ?.selectedServerKey,
            decisionConfidence =
                decision
                    ?.confidence,
            candidateCount =
                candidates.size,
            topCandidateKey =
                topCandidate
                    ?.serverKey,
            topCandidateScore =
                topCandidate
                    ?.diagnosticScore,
            secondCandidateScore =
                secondCandidate
                    ?.diagnosticScore,
            topScoreDelta =
                topScoreDelta,
            topCandidateSharedTokens =
                topCandidate
                    ?.sharedTokens
                    .orEmpty()
                    .distinct()
                    .sorted(),
            mappingExists =
                false,
            details =
                classification.details
        )
    }

    private fun determineClassification(
        catalogKey: String,
        exactMatchReported: Boolean,
        request: PersistedRequest?,
        decision: PersistedDecision?,
        topCandidate: PersistedCandidate?,
        topScoreDelta: Double?
    ): Classification {

        if (exactMatchReported) {
            return Classification(
                type =
                    NutritionCoverageGapType
                        .EXACT_MATCH_NOT_IN_RUNTIME,
                details =
                    "The catalog key is reported as an exact nutrition " +
                            "server match, but it is not covered by the " +
                            "persisted runtime mapping artifacts."
            )
        }

        if (
            decision?.type ==
            CatalogKnowledgeMatchDecisionType.MATCH
        ) {
            return Classification(
                type =
                    NutritionCoverageGapType.MATCH_NOT_PERSISTED,
                details =
                    "A MATCH decision exists, but the catalog key is " +
                            "not covered by the persisted mapping " +
                            "artifact."
            )
        }

        if (request == null) {
            return Classification(
                type =
                    NutritionCoverageGapType.NO_REQUEST,
                details =
                    "No persisted nutrition match request exists for " +
                            "the missing catalog key."
            )
        }

        if (decision == null) {
            return Classification(
                type =
                    NutritionCoverageGapType.NO_DECISION,
                details =
                    "A nutrition match request exists, but no persisted " +
                            "decision exists."
            )
        }

        if (request.candidates.isEmpty()) {
            return Classification(
                type =
                    NutritionCoverageGapType.NO_CANDIDATES,
                details =
                    "The persisted nutrition match request contains no " +
                            "candidates."
            )
        }

        require(
            decision.type ==
                    CatalogKnowledgeMatchDecisionType.NO_MATCH
        ) {
            "Unsupported decision type for missing catalog key " +
                    "'$catalogKey': ${decision.type}."
        }

        requireNotNull(topCandidate) {
            "A request with candidates must have a top candidate."
        }

        if (topCandidate.sharedTokens.isEmpty()) {
            return Classification(
                type =
                    NutritionCoverageGapType.NO_SHARED_TOKENS,
                details =
                    "The top candidate shares no diagnostic tokens " +
                            "with the catalog key."
            )
        }

        if (
            topCandidate.diagnosticScore <
            VERY_LOW_SCORE_THRESHOLD
        ) {
            return Classification(
                type =
                    NutritionCoverageGapType.VERY_LOW_SCORE,
                details =
                    "The top candidate diagnostic score " +
                            "${formatScore(topCandidate.diagnosticScore)} " +
                            "is below the threshold " +
                            "${formatScore(VERY_LOW_SCORE_THRESHOLD)}."
            )
        }

        val catalogTokens =
            tokenize(
                value =
                    catalogKey
            )

        val candidateTokens =
            tokenize(
                value =
                    topCandidate.serverKey
            )

        val mismatchingModifiers =
            findMismatchingModifiers(
                catalogTokens =
                    catalogTokens,
                candidateTokens =
                    candidateTokens
            )

        if (mismatchingModifiers.isNotEmpty()) {
            return Classification(
                type =
                    NutritionCoverageGapType.MODIFIER_MISMATCH,
                details =
                    "Catalog key and top candidate contain conflicting " +
                            "modifiers: " +
                            mismatchingModifiers.joinToString()
            )
        }

        val sharedTokenCount =
            topCandidate
                .sharedTokens
                .distinct()
                .size

        if (
            catalogTokens.size >=
            MIN_MULTI_TOKEN_KEY_SIZE &&
            sharedTokenCount <=
            WEAK_SHARED_TOKEN_MAX_COUNT
        ) {
            return Classification(
                type =
                    NutritionCoverageGapType.WEAK_TOKEN_OVERLAP,
                details =
                    "The top candidate shares only $sharedTokenCount " +
                            "token(s) with a ${catalogTokens.size}-token " +
                            "catalog key."
            )
        }

        if (
            catalogTokens.size -
            candidateTokens.size >=
            TOKEN_SPECIFICITY_DIFFERENCE
        ) {
            return Classification(
                type =
                    NutritionCoverageGapType.TOO_GENERIC,
                details =
                    "The top candidate is substantially more generic " +
                            "than the catalog key: catalogTokens=" +
                            "${catalogTokens.size}, candidateTokens=" +
                            "${candidateTokens.size}."
            )
        }

        if (
            candidateTokens.size -
            catalogTokens.size >=
            TOKEN_SPECIFICITY_DIFFERENCE
        ) {
            return Classification(
                type =
                    NutritionCoverageGapType.TOO_SPECIFIC,
                details =
                    "The top candidate is substantially more specific " +
                            "than the catalog key: catalogTokens=" +
                            "${catalogTokens.size}, candidateTokens=" +
                            "${candidateTokens.size}."
            )
        }

        if (
            topScoreDelta != null &&
            topScoreDelta <=
            SCORE_CLUSTER_MAX_DELTA
        ) {
            return Classification(
                type =
                    NutritionCoverageGapType.SCORE_CLUSTER,
                details =
                    "The score difference between the first two " +
                            "candidates is only " +
                            "${formatScore(topScoreDelta)}."
            )
        }

        return classifyNoMatchCause(
            catalogKey =
                catalogKey,
            request =
                request,
            topCandidate =
                topCandidate
        )
    }

    private fun classifyNoMatchCause(
        catalogKey: String,
        request: PersistedRequest,
        topCandidate: PersistedCandidate
    ): Classification {

        val catalogTokens =
            tokenize(
                value =
                    catalogKey
            )

        val topCandidateTokens =
            tokenize(
                value =
                    topCandidate.serverKey
            )

        val catalogProductClasses =
            productClasses(
                tokens =
                    catalogTokens
            )

        val topCandidateProductClasses =
            productClasses(
                tokens =
                    topCandidateTokens
            )

        if (
            catalogProductClasses.isNotEmpty() &&
            topCandidateProductClasses.isNotEmpty() &&
            catalogProductClasses.intersect(
                topCandidateProductClasses
            ).isEmpty()
        ) {
            return Classification(
                type =
                    NutritionCoverageGapType.NO_MATCH,
                noMatchCause =
                    NutritionNoMatchCause.PRODUCT_CLASS_MISMATCH,
                details =
                    "Catalog key and top candidate belong to different " +
                            "product classes: catalogClasses=" +
                            catalogProductClasses.joinToString() +
                            ", candidateClasses=" +
                            topCandidateProductClasses.joinToString() +
                            "."
            )
        }

        if (
            isPreparedMeal(
                tokens =
                    catalogTokens
            ) &&
            isPreparedMeal(
                tokens =
                    topCandidateTokens
            ) &&
            preparedMealIdentities(
                tokens =
                    catalogTokens
            ) !=
            preparedMealIdentities(
                tokens =
                    topCandidateTokens
            )
        ) {
            return Classification(
                type =
                    NutritionCoverageGapType.NO_MATCH,
                noMatchCause =
                    NutritionNoMatchCause.PREPARED_MEAL_VARIANT,
                details =
                    "Catalog key and top candidate describe different " +
                            "prepared-meal variants."
            )
        }

        val catalogCoreTokens =
            coreTokens(
                tokens =
                    catalogTokens
            )

        val candidateCoreTokens =
            coreTokens(
                tokens =
                    topCandidateTokens
            )

        val sharedCoreTokens =
            catalogCoreTokens.intersect(
                candidateCoreTokens
            )

        if (
            catalogCoreTokens.isNotEmpty() &&
            sharedCoreTokens.isNotEmpty() &&
            sharedCoreTokens.size <
            catalogCoreTokens.size
        ) {
            return Classification(
                type =
                    NutritionCoverageGapType.NO_MATCH,
                noMatchCause =
                    NutritionNoMatchCause.PARTIAL_CORE_TOKEN_MATCH,
                details =
                    "The top candidate covers only part of the catalog " +
                            "core meaning: sharedCoreTokens=" +
                            sharedCoreTokens.sorted().joinToString() +
                            ", catalogCoreTokens=" +
                            catalogCoreTokens.sorted().joinToString() +
                            "."
            )
        }

        val candidateProductClassSets =
            request.candidates
                .map { candidate ->

                    productClasses(
                        tokens =
                            tokenize(
                                value =
                                    candidate.serverKey
                            )
                    )
                }
                .filter {
                    it.isNotEmpty()
                }
                .distinct()

        if (
            candidateProductClassSets.size >=
            MIN_MIXED_PRODUCT_CLASS_COUNT
        ) {
            return Classification(
                type =
                    NutritionCoverageGapType.NO_MATCH,
                noMatchCause =
                    NutritionNoMatchCause
                        .MIXED_PRODUCT_CLASS_CANDIDATES,
                details =
                    "The candidate set contains multiple incompatible " +
                            "product-class groups: " +
                            candidateProductClassSets
                                .map {
                                    it.sorted().joinToString(
                                        separator =
                                            "+"
                                    )
                                }
                                .sorted()
                                .joinToString() +
                            "."
            )
        }

        if (
            topCandidate.diagnosticScore >=
            STRONG_TOP_CANDIDATE_THRESHOLD
        ) {
            return Classification(
                type =
                    NutritionCoverageGapType.NO_MATCH,
                noMatchCause =
                    NutritionNoMatchCause
                        .STRONG_TOP_CANDIDATE_REJECTED,
                details =
                    "A strong top candidate with diagnostic score " +
                            formatScore(
                                topCandidate.diagnosticScore
                            ) +
                            " was rejected as NO_MATCH."
            )
        }

        if (
            topCandidate.diagnosticScore >=
            MODERATE_TOP_CANDIDATE_THRESHOLD
        ) {
            return Classification(
                type =
                    NutritionCoverageGapType.NO_MATCH,
                noMatchCause =
                    NutritionNoMatchCause
                        .MODERATE_TOP_CANDIDATE_REJECTED,
                details =
                    "The top candidate has a moderate diagnostic score " +
                            formatScore(
                                topCandidate.diagnosticScore
                            ) +
                            " but was rejected as NO_MATCH."
            )
        }

        return Classification(
            type =
                NutritionCoverageGapType.NO_MATCH,
            noMatchCause =
                NutritionNoMatchCause.WEAK_CANDIDATE_SET,
            details =
                "The best available candidate has only a weak " +
                        "diagnostic score of " +
                        formatScore(
                            topCandidate.diagnosticScore
                        ) +
                        "."
        )
    }

    private fun productClasses(
        tokens: Set<String>
    ): Set<String> {

        return PRODUCT_CLASS_TOKENS
            .mapNotNull { (productClass, classTokens) ->

                productClass.takeIf {
                    tokens.intersect(
                        classTokens
                    ).isNotEmpty()
                }
            }
            .toSortedSet()
    }

    private fun coreTokens(
        tokens: Set<String>
    ): Set<String> {

        return tokens
            .asSequence()
            .filterNot {
                it in NON_CORE_TOKENS
            }
            .filterNot { token ->

                PRODUCT_CLASS_TOKENS
                    .values
                    .any { classTokens ->

                        token in classTokens
                    }
            }
            .toSortedSet()
    }

    private fun isPreparedMeal(
        tokens: Set<String>
    ): Boolean {

        return tokens.intersect(
            PREPARED_MEAL_TOKENS
        ).isNotEmpty()
    }

    private fun preparedMealIdentities(
        tokens: Set<String>
    ): Set<String> {

        return tokens.intersect(
            PREPARED_MEAL_IDENTITY_TOKENS
        )
    }

    private fun readCatalogKeys(
        file: File
    ): Set<String> {

        require(file.isFile) {
            "Catalog file does not exist: " +
                    file.absolutePath
        }

        val root =
            JsonParser.parseString(
                file.readText()
            )

        val items =
            when {

                root.isJsonArray ->
                    root.asJsonArray

                root.isJsonObject -> {

                    val rootObject =
                        root.asJsonObject

                    rootObject.arrayOrNull(
                        key =
                            "items"
                    )
                        ?: rootObject.arrayOrNull(
                            key =
                                "foods"
                        )
                        ?: rootObject.arrayOrNull(
                            key =
                                "products"
                        )
                        ?: error(
                            "Catalog contains no 'items', 'foods' or " +
                                    "'products' array: " +
                                    file.absolutePath
                        )
                }

                else ->
                    error(
                        "Unsupported catalog JSON root: " +
                                file.absolutePath
                    )
            }

        val catalogKeys =
            items
                .mapNotNull { element ->

                    if (!element.isJsonObject) {
                        return@mapNotNull null
                    }

                    val item =
                        element.asJsonObject

                    item.optionalString(
                        key =
                            "normalizedEnglish"
                    )
                        ?: item.optionalString(
                            key =
                                "normalized"
                        )
                        ?: item.optionalString(
                            key =
                                "canonicalKey"
                        )
                        ?: item.optionalString(
                            key =
                                "canonicalId"
                        )
                        ?: item.optionalString(
                            key =
                                "id"
                        )
                        ?: item.optionalString(
                            key =
                                "name"
                        )
                }
                .map(
                    ::normalizeKey
                )
                .filter {
                    it.isNotBlank()
                }
                .toSortedSet()

        require(catalogKeys.isNotEmpty()) {
            "Catalog contains no readable normalized keys: " +
                    file.absolutePath
        }

        return catalogKeys
    }

    private fun readMappingCatalogKeys(
        file: File,
        sourceName: String,
        requireFile: Boolean
    ): Set<String> {

        if (!file.isFile) {

            require(!requireFile) {
                "$sourceName does not exist: " +
                        file.absolutePath
            }

            return emptySet()
        }

        val root =
            JsonParser.parseString(
                file.readText()
            )

        require(root.isJsonObject) {
            "$sourceName must contain a JSON object: " +
                    file.absolutePath
        }

        val mappings =
            root.asJsonObject
                .arrayOrNull(
                    key =
                        "mappings"
                )
                ?: error(
                    "$sourceName contains no 'mappings' array: " +
                            file.absolutePath
                )

        return mappings
            .mapNotNull { element ->

                require(element.isJsonObject) {
                    "$sourceName mapping entry must be a JSON object."
                }

                val mapping =
                    element.asJsonObject

                val artifact =
                    mapping.optionalString(
                        key =
                            "serverArtifact"
                    )
                        ?: mapping.optionalString(
                            key =
                                "sourceArtifact"
                        )
                        ?: NUTRITION_ARTIFACT

                if (
                    artifact !=
                    NUTRITION_ARTIFACT
                ) {
                    return@mapNotNull null
                }

                normalizeKey(
                    value =
                        mapping.requiredString(
                            key =
                                "catalogKey"
                        )
                )
            }
            .filter {
                it.isNotBlank()
            }
            .toSortedSet()
    }

    private fun readExactMatchReportCatalogKeys(
        file: File
    ): Set<String> {

        require(file.isFile) {
            "Nutrition exact-match report does not exist: " +
                    file.absolutePath
        }

        val root =
            JsonParser.parseString(
                file.readText()
            )

        require(root.isJsonObject) {
            "Nutrition exact-match report must contain a JSON object: " +
                    file.absolutePath
        }

        val exactMatches =
            root.asJsonObject
                .arrayOrNull(
                    key =
                        "exactMatches"
                )
                ?: error(
                    "Nutrition exact-match report contains no " +
                            "'exactMatches' array: " +
                            file.absolutePath
                )

        val catalogKeys =
            exactMatches
                .map { element ->

                    require(
                        element.isJsonPrimitive &&
                                element
                                    .asJsonPrimitive
                                    .isString
                    ) {
                        "Nutrition exactMatches must contain strings."
                    }

                    normalizeKey(
                        value =
                            element.asString
                    )
                }
                .filter {
                    it.isNotBlank()
                }

        requireNoDuplicateKeys(
            values =
                catalogKeys,
            sourceName =
                "nutrition exact matches"
        )

        return catalogKeys
            .toSortedSet()
    }

    private fun readRequests(
        file: File
    ): List<PersistedRequest> {

        require(file.isFile) {
            "Nutrition match request file does not exist: " +
                    file.absolutePath
        }

        val root =
            JsonParser.parseString(
                file.readText()
            )

        require(root.isJsonObject) {
            "Nutrition match request file must contain a JSON object."
        }

        val requests =
            root.asJsonObject
                .arrayOrNull(
                    key =
                        "requests"
                )
                ?: error(
                    "Nutrition match request file contains no " +
                            "'requests' array."
                )

        val parsedRequests =
            requests.mapNotNull { element ->

                require(element.isJsonObject) {
                    "Nutrition match request must be a JSON object."
                }

                val objectValue =
                    element.asJsonObject

                val serverArtifact =
                    objectValue.requiredString(
                        key =
                            "serverArtifact"
                    )

                if (
                    serverArtifact !=
                    NUTRITION_ARTIFACT
                ) {
                    return@mapNotNull null
                }

                val candidates =
                    objectValue
                        .arrayOrNull(
                            key =
                                "candidates"
                        )
                        .orEmpty()
                        .map { candidateElement ->

                            require(candidateElement.isJsonObject) {
                                "Nutrition candidate must be a JSON " +
                                        "object."
                            }

                            val candidate =
                                candidateElement.asJsonObject

                            PersistedCandidate(
                                serverKey =
                                    normalizeKey(
                                        candidate.requiredString(
                                            key =
                                                "serverKey"
                                        )
                                    ),
                                diagnosticScore =
                                    candidate.requiredDouble(
                                        key =
                                            "diagnosticScore"
                                    ),
                                sharedTokens =
                                    candidate
                                        .arrayOrNull(
                                            key =
                                                "sharedTokens"
                                        )
                                        .orEmpty()
                                        .map { tokenElement ->

                                            require(
                                                tokenElement
                                                    .isJsonPrimitive &&
                                                        tokenElement
                                                            .asJsonPrimitive
                                                            .isString
                                            ) {
                                                "sharedTokens must " +
                                                        "contain strings."
                                            }

                                            normalizeKey(
                                                value =
                                                    tokenElement.asString
                                            )
                                        }
                                        .filter {
                                            it.isNotBlank()
                                        }
                                        .distinct()
                                        .sorted()
                            )
                        }

                PersistedRequest(
                    catalogKey =
                        normalizeKey(
                            value =
                                objectValue.requiredString(
                                    key =
                                        "catalogKey"
                                )
                        ),
                    candidates =
                        candidates
                )
            }

        requireNoDuplicateKeys(
            values =
                parsedRequests.map {
                    it.catalogKey
                },
            sourceName =
                "nutrition match requests"
        )

        return parsedRequests
            .sortedBy {
                it.catalogKey
            }
    }

    private fun readDecisions(
        file: File
    ): List<PersistedDecision> {

        require(file.isFile) {
            "Nutrition decision file does not exist: " +
                    file.absolutePath
        }

        val root =
            JsonParser.parseString(
                file.readText()
            )

        require(root.isJsonObject) {
            "Nutrition decision file must contain a JSON object: " +
                    file.absolutePath
        }

        val rootObject =
            root.asJsonObject

        val entries =
            when {

                rootObject.hasJsonArray(
                    key =
                        "decisions"
                ) ->
                    rootObject.getAsJsonArray(
                        "decisions"
                    )

                rootObject.hasJsonArray(
                    key =
                        "diagnostics"
                ) ->
                    rootObject.getAsJsonArray(
                        "diagnostics"
                    )

                else ->
                    error(
                        "Nutrition decision file contains neither a " +
                                "'decisions' nor a 'diagnostics' array: " +
                                file.absolutePath
                    )
            }

        val decisions =
            entries
                .mapNotNull { element ->

                    require(element.isJsonObject) {
                        "Nutrition decision entries must be JSON objects."
                    }

                    val entry =
                        element.asJsonObject

                    val serverArtifact =
                        entry.requiredString(
                            key =
                                "serverArtifact"
                        )

                    if (
                        serverArtifact !=
                        NUTRITION_ARTIFACT
                    ) {
                        return@mapNotNull null
                    }

                    val catalogKey =
                        normalizeKey(
                            value =
                                entry.requiredString(
                                    key =
                                        "catalogKey"
                                )
                        )

                    val decisionTypeName =
                        entry.optionalString(
                            key =
                                "type"
                        )
                            ?: entry.optionalString(
                                key =
                                    "decisionType"
                            )
                            ?: error(
                                "Nutrition decision entry for " +
                                        "'$catalogKey' contains neither " +
                                        "'type' nor 'decisionType'."
                            )

                    val decisionType =
                        CatalogKnowledgeMatchDecisionType.valueOf(
                            decisionTypeName
                        )

                    val selectedServerKey =
                        entry.optionalString(
                            key =
                                "selectedServerKey"
                        )
                            ?.let { value ->

                                normalizeKey(
                                    value =
                                        value
                                )
                            }

                    val confidence =
                        entry.requiredDouble(
                            key =
                                "confidence"
                        )

                    val decisionSource =
                        entry.optionalString(
                            key =
                                "decisionSource"
                        )
                            ?.let { value ->

                                CatalogKnowledgeMatchDecisionSource
                                    .valueOf(
                                        value
                                    )
                            }

                    PersistedDecision(
                        catalogKey =
                            catalogKey,
                        type =
                            decisionType,
                        selectedServerKey =
                            selectedServerKey,
                        confidence =
                            confidence,
                        decisionSource =
                            decisionSource
                    )
                }

        requireNoDuplicateKeys(
            values =
                decisions.map {
                    it.catalogKey
                },
            sourceName =
                "nutrition decisions"
        )

        return decisions
            .sortedBy {
                it.catalogKey
            }
    }

    private fun findMismatchingModifiers(
        catalogTokens: Set<String>,
        candidateTokens: Set<String>
    ): List<String> {

        return MODIFIER_GROUPS
            .mapNotNull { group ->

                val catalogModifiers =
                    catalogTokens
                        .intersect(
                            group
                        )

                val candidateModifiers =
                    candidateTokens
                        .intersect(
                            group
                        )

                if (
                    catalogModifiers.isNotEmpty() &&
                    candidateModifiers.isNotEmpty() &&
                    catalogModifiers !=
                    candidateModifiers
                ) {
                    (
                            catalogModifiers +
                                    candidateModifiers
                            )
                        .sorted()
                        .joinToString(
                            separator =
                                " vs "
                        )
                } else {
                    null
                }
            }
            .sorted()
    }

    private fun tokenize(
        value: String
    ): Set<String> {

        return normalizeKey(
            value =
                value
        )
            .split(
                " "
            )
            .filter {
                it.isNotBlank()
            }
            .toSortedSet()
    }

    private fun normalizeKey(
        value: String
    ): String {

        return value
            .trim()
            .lowercase()
            .replace(
                "-",
                " "
            )
            .replace(
                "_",
                " "
            )
            .replace(
                WHITESPACE_REGEX,
                " "
            )
            .trim()
    }

    private fun formatScore(
        value: Double
    ): String {

        return String.format(
            java.util.Locale.ROOT,
            "%.4f",
            value
        )
    }

    private fun requireNoDuplicateKeys(
        values: List<String>,
        sourceName: String
    ) {
        val duplicates =
            values
                .groupingBy {
                    it
                }
                .eachCount()
                .filterValues {
                    it > 1
                }
                .keys

        require(duplicates.isEmpty()) {
            "$sourceName contain duplicate catalog keys: " +
                    duplicates
                        .sorted()
                        .take(
                            MAX_DIAGNOSTIC_KEYS
                        )
                        .joinToString()
        }
    }

    private fun JsonObject.hasJsonArray(
        key: String
    ): Boolean {

        return get(key)
            ?.isJsonArray ==
                true
    }

    private fun JsonObject.arrayOrNull(
        key: String
    ): JsonArray? {

        return get(key)
            ?.takeIf {
                it.isJsonArray
            }
            ?.asJsonArray
    }

    private fun JsonObject.requiredString(
        key: String
    ): String {

        return optionalString(
            key =
                key
        )
            ?: error(
                "Missing or blank string '$key'."
            )
    }

    private fun JsonObject.optionalString(
        key: String
    ): String? {

        return get(key)
            ?.takeIf {
                !it.isJsonNull &&
                        it.isJsonPrimitive &&
                        it.asJsonPrimitive.isString
            }
            ?.asString
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }
    }

    private fun JsonObject.requiredDouble(
        key: String
    ): Double {

        return get(key)
            ?.takeIf {
                it.isJsonPrimitive &&
                        it.asJsonPrimitive.isNumber
            }
            ?.asDouble
            ?: error(
                "Missing numeric value '$key'."
            )
    }

    private fun JsonArray?.orEmpty():
            List<com.google.gson.JsonElement> {

        return this
            ?.toList()
            .orEmpty()
    }

    private data class PersistedRequest(
        val catalogKey: String,
        val candidates: List<PersistedCandidate>
    )

    private data class PersistedCandidate(
        val serverKey: String,
        val diagnosticScore: Double,
        val sharedTokens: List<String>
    )

    private data class PersistedDecision(
        val catalogKey: String,
        val type: CatalogKnowledgeMatchDecisionType,
        val selectedServerKey: String?,
        val confidence: Double,
        val decisionSource: CatalogKnowledgeMatchDecisionSource?
    )

    private data class Classification(
        val type: NutritionCoverageGapType,
        val noMatchCause: NutritionNoMatchCause? = null,
        val details: String
    ) {

        init {
            require(
                type ==
                        NutritionCoverageGapType.NO_MATCH ||
                        noMatchCause == null
            ) {
                "Only NO_MATCH classifications may have a noMatchCause."
            }

            require(
                type !=
                        NutritionCoverageGapType.NO_MATCH ||
                        noMatchCause != null
            ) {
                "NO_MATCH classification requires a noMatchCause."
            }

            require(details.isNotBlank()) {
                "Classification details must not be blank."
            }
        }
    }

    private companion object {

        val PRODUCT_CLASS_TOKENS =
            linkedMapOf(
                "drink" to
                        setOf(
                            "drink",
                            "juice",
                            "smoothie",
                            "water",
                            "milk",
                            "coffee",
                            "tea"
                        ),
                "bread" to
                        setOf(
                            "bread",
                            "roll",
                            "baguette",
                            "toast",
                            "pita"
                        ),
                "meat" to
                        setOf(
                            "meat",
                            "beef",
                            "pork",
                            "chicken",
                            "turkey",
                            "lamb",
                            "ham",
                            "sausage"
                        ),
                "fish" to
                        setOf(
                            "fish",
                            "salmon",
                            "trout",
                            "cod",
                            "tuna",
                            "pollock",
                            "herring"
                        ),
                "cheese" to
                        setOf(
                            "cheese",
                            "quark"
                        ),
                "yogurt" to
                        setOf(
                            "yogurt",
                            "yoghurt",
                            "kefir"
                        ),
                "rice" to
                        setOf(
                            "rice",
                            "risotto"
                        ),
                "pasta" to
                        setOf(
                            "pasta",
                            "spaghetti",
                            "macaroni",
                            "noodle",
                            "lasagna"
                        ),
                "spread" to
                        setOf(
                            "spread",
                            "paste",
                            "pate"
                        ),
                "sauce" to
                        setOf(
                            "sauce",
                            "dressing",
                            "ketchup",
                            "mustard"
                        ),
                "sweet" to
                        setOf(
                            "candy",
                            "chocolate",
                            "dessert",
                            "cake",
                            "cookie",
                            "marshmallow"
                        ),
                "prepared_meal" to
                        setOf(
                            "meal",
                            "dish",
                            "curry",
                            "stew",
                            "casserole",
                            "gratin",
                            "lasagna",
                            "pizza",
                            "paella"
                        )
            )

        val PREPARED_MEAL_TOKENS =
            setOf(
                "meal",
                "dish",
                "ready",
                "prepared",
                "curry",
                "stew",
                "casserole",
                "gratin",
                "lasagna",
                "pizza",
                "paella",
                "risotto",
                "goulash"
            )

        val PREPARED_MEAL_IDENTITY_TOKENS =
            setOf(
                "curry",
                "stew",
                "casserole",
                "gratin",
                "lasagna",
                "pizza",
                "paella",
                "risotto",
                "goulash"
            )

        val NON_CORE_TOKENS =
            setOf(
                "fresh",
                "frozen",
                "dried",
                "canned",
                "raw",
                "cooked",
                "ready",
                "prepared",
                "organic",
                "plain",
                "classic",
                "original",
                "style",
                "with",
                "and",
                "of",
                "the"
            )

        const val STRONG_TOP_CANDIDATE_THRESHOLD =
            0.80

        const val MODERATE_TOP_CANDIDATE_THRESHOLD =
            0.65

        const val MIN_MIXED_PRODUCT_CLASS_COUNT =
            2

        const val NUTRITION_ARTIFACT =
            "nutrition.json"

        const val VERY_LOW_SCORE_THRESHOLD =
            0.45

        const val SCORE_CLUSTER_MAX_DELTA =
            0.03

        const val WEAK_SHARED_TOKEN_MAX_COUNT =
            1

        const val MIN_MULTI_TOKEN_KEY_SIZE =
            3

        const val TOKEN_SPECIFICITY_DIFFERENCE =
            2

        const val MAX_DIAGNOSTIC_KEYS =
            10

        val WHITESPACE_REGEX =
            Regex("\\s+")

        val MODIFIER_GROUPS =
            listOf(
                setOf(
                    "fresh",
                    "frozen",
                    "dried",
                    "canned"
                ),
                setOf(
                    "raw",
                    "cooked",
                    "fried",
                    "baked",
                    "roasted",
                    "grilled",
                    "steamed"
                ),
                setOf(
                    "whole",
                    "sliced",
                    "chopped",
                    "ground",
                    "minced",
                    "mashed"
                ),
                setOf(
                    "sweetened",
                    "unsweetened"
                ),
                setOf(
                    "salted",
                    "unsalted"
                ),
                setOf(
                    "lowfat",
                    "nonfat",
                    "fullfat",
                    "skimmed"
                ),
                setOf(
                    "vegan",
                    "vegetarian"
                )
            )
    }
}