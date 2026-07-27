package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.analysis

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

class OFFNutritionReferenceCandidateGapAnalyzer {

    fun analyze(
        sourceDiagnosticFile: File
    ): OFFNutritionReferenceCandidateGapAnalysisReport {

        require(sourceDiagnosticFile.isFile) {
            "OFF nutrition source diagnostic file not found: " +
                    sourceDiagnosticFile.absolutePath
        }

        val root =
            sourceDiagnosticFile
                .reader()
                .use { reader ->
                    JsonParser
                        .parseReader(reader)
                        .asJsonObject
                }

        val sourceDiagnosticVersion =
            root.requiredInt("version")

        val sourceRequestCount =
            root.requiredInt("requestCount")

        val sourceMissingRequestCount =
            root.requiredInt("missingRequestCount")

        val sourceRawOFFScannedProductCount =
            root.requiredLong("rawOFFScannedProductCount")

        val findings =
            root.requiredArray("findings")
                .map { element ->
                    analyzeFinding(
                        finding =
                            element.asJsonObject
                    )
                }
                .sortedWith(
                    OFFNutritionReferenceCandidateGapAnalysisReport
                        .FINDING_COMPARATOR
                )

        require(findings.size == sourceMissingRequestCount) {
            "Source diagnostic finding count does not match " +
                    "missingRequestCount: " +
                    "findings=${findings.size}, " +
                    "missingRequestCount=$sourceMissingRequestCount."
        }

        val countsByFirstMissingStage =
            findings
                .groupingBy { finding ->
                    finding.firstMissingStage
                }
                .eachCount()
                .toSortedMap()

        val countsByCause =
            enumValues<OFFNutritionReferenceCandidateGapCause>()
                .associateWith { cause ->
                    findings.count { finding ->
                        finding.cause == cause
                    }
                }
                .filterValues { count ->
                    count > 0
                }

        val countsByPriority =
            enumValues<OFFNutritionReferenceCandidateGapPriority>()
                .associateWith { priority ->
                    findings.count { finding ->
                        finding.priority == priority
                    }
                }
                .filterValues { count ->
                    count > 0
                }

        return OFFNutritionReferenceCandidateGapAnalysisReport(
            version =
                REPORT_VERSION,
            sourceDiagnosticVersion =
                sourceDiagnosticVersion,
            sourceRequestCount =
                sourceRequestCount,
            sourceMissingRequestCount =
                sourceMissingRequestCount,
            sourceRawOFFScannedProductCount =
                sourceRawOFFScannedProductCount,
            analyzedFindingCount =
                findings.size,
            referenceCandidateGapCount =
                findings.count { finding ->
                    finding.firstMissingStage ==
                            STAGE_REFERENCE_CANDIDATE
                },
            countsByFirstMissingStage =
                countsByFirstMissingStage,
            countsByCause =
                countsByCause,
            countsByPriority =
                countsByPriority,
            findings =
                findings
        )
    }

    private fun analyzeFinding(
        finding: JsonObject
    ): OFFNutritionReferenceCandidateGapAnalysisFinding {

        val catalogIndex =
            finding.requiredInt("catalogIndex")

        val catalogKey =
            finding.requiredString("catalogKey")

        val normalizedEnglish =
            finding.requiredString("normalizedEnglish")

        val firstMissingStage =
            finding.requiredString("firstMissingStage")

        val rawOFFProductMatchCount =
            finding.requiredInt("rawOFFProductMatchCount")

        val rawOFFProductWithAnyNutritionCount =
            finding.requiredInt(
                "rawOFFProductWithAnyNutritionCount"
            )

        val rawOFFProductWithUsableNutritionCount =
            finding.requiredInt(
                "rawOFFProductWithUsableNutritionCount"
            )

        val referenceCandidateMatchCount =
            finding.requiredInt(
                "referenceCandidateMatchCount"
            )

        val referenceAggregateMatchCount =
            finding.requiredInt(
                "referenceAggregateMatchCount"
            )

        val matcherCandidateMatchCount =
            finding.requiredInt(
                "matcherCandidateMatchCount"
            )

        val matchedRawProductNames =
            finding.stringList("matchedRawProductNames")

        val diagnosticReasons =
            finding.stringList("reasons")

        val traceIdentityAccepted =
            finding.optionalBoolean(
                "traceIdentityAccepted"
            )

        val traceNutritionAccepted =
            finding.optionalBoolean(
                "traceNutritionAccepted"
            )

        val traceReferenceEligible =
            finding.optionalBoolean(
                "traceReferenceEligible"
            )

        val traceCandidateCreated =
            finding.optionalBoolean(
                "traceCandidateCreated"
            )

        val identityRejectionReasons =
            finding.stringList(
                "identityRejectionReasons"
            )

        val nutritionRejectionReasons =
            finding.stringList(
                "nutritionRejectionReasons"
            )

        val referenceEligibilityRejectionReasons =
            finding.stringList(
                "referenceEligibilityRejectionReasons"
            )

        val cause =
            classifyCause(
                firstMissingStage =
                    firstMissingStage,
                rawOFFProductMatchCount =
                    rawOFFProductMatchCount,
                rawOFFProductWithUsableNutritionCount =
                    rawOFFProductWithUsableNutritionCount,
                referenceCandidateMatchCount =
                    referenceCandidateMatchCount,
                referenceAggregateMatchCount =
                    referenceAggregateMatchCount,
                traceIdentityAccepted =
                    traceIdentityAccepted,
                traceNutritionAccepted =
                    traceNutritionAccepted,
                traceReferenceEligible =
                    traceReferenceEligible,
                traceCandidateCreated =
                    traceCandidateCreated,
                identityRejectionReasons =
                    identityRejectionReasons,
                nutritionRejectionReasons =
                    nutritionRejectionReasons,
                referenceEligibilityRejectionReasons =
                    referenceEligibilityRejectionReasons
            )

        val priority =
            classifyPriority(
                cause =
                    cause,
                rawOFFProductWithUsableNutritionCount =
                    rawOFFProductWithUsableNutritionCount
            )

        return OFFNutritionReferenceCandidateGapAnalysisFinding(
            catalogIndex =
                catalogIndex,
            catalogKey =
                catalogKey,
            normalizedEnglish =
                normalizedEnglish,
            firstMissingStage =
                firstMissingStage,
            rawOFFProductMatchCount =
                rawOFFProductMatchCount,
            rawOFFProductWithAnyNutritionCount =
                rawOFFProductWithAnyNutritionCount,
            rawOFFProductWithUsableNutritionCount =
                rawOFFProductWithUsableNutritionCount,
            referenceCandidateMatchCount =
                referenceCandidateMatchCount,
            referenceAggregateMatchCount =
                referenceAggregateMatchCount,
            matcherCandidateMatchCount =
                matcherCandidateMatchCount,
            matchedRawProductNames =
                matchedRawProductNames,
            diagnosticReasons =
                diagnosticReasons,
            cause =
                cause,
            priority =
                priority,
            recommendedAction =
                recommendedAction(cause)
        )
    }

    private fun classifyCause(
        firstMissingStage: String,
        rawOFFProductMatchCount: Int,
        rawOFFProductWithUsableNutritionCount: Int,
        referenceCandidateMatchCount: Int,
        referenceAggregateMatchCount: Int,
        traceIdentityAccepted: Boolean?,
        traceNutritionAccepted: Boolean?,
        traceReferenceEligible: Boolean?,
        traceCandidateCreated: Boolean?,
        identityRejectionReasons: List<String>,
        nutritionRejectionReasons: List<String>,
        referenceEligibilityRejectionReasons: List<String>
    ): OFFNutritionReferenceCandidateGapCause {

        if (
            firstMissingStage == STAGE_RAW_OFF_PRODUCT ||
            rawOFFProductMatchCount == 0
        ) {
            return OFFNutritionReferenceCandidateGapCause
                .NO_RAW_OFF_PRODUCT
        }

        if (
            firstMissingStage ==
            STAGE_RAW_OFF_USABLE_NUTRITION ||
            rawOFFProductWithUsableNutritionCount == 0
        ) {
            return OFFNutritionReferenceCandidateGapCause
                .NO_USABLE_RAW_NUTRITION
        }

        if (
            firstMissingStage ==
            STAGE_REFERENCE_AGGREGATE &&
            referenceCandidateMatchCount > 0 &&
            referenceAggregateMatchCount == 0
        ) {
            return OFFNutritionReferenceCandidateGapCause
                .REFERENCE_AGGREGATION_GAP
        }

        if (
            firstMissingStage ==
            STAGE_MATCHER_CANDIDATE
        ) {
            return OFFNutritionReferenceCandidateGapCause
                .MATCHER_RETRIEVAL_GAP
        }

        if (
            traceIdentityAccepted == false ||
            identityRejectionReasons.isNotEmpty()
        ) {
            return OFFNutritionReferenceCandidateGapCause
                .CANDIDATE_IDENTITY_MISMATCH
        }

        if (
            traceNutritionAccepted == false ||
            nutritionRejectionReasons.isNotEmpty()
        ) {
            return OFFNutritionReferenceCandidateGapCause
                .CANDIDATE_NUTRITION_REJECTED
        }

        if (
            traceReferenceEligible == false ||
            referenceEligibilityRejectionReasons.isNotEmpty()
        ) {
            return OFFNutritionReferenceCandidateGapCause
                .CANDIDATE_REFERENCE_INELIGIBLE
        }

        if (
            firstMissingStage ==
            STAGE_REFERENCE_CANDIDATE ||
            traceCandidateCreated == false
        ) {
            return OFFNutritionReferenceCandidateGapCause
                .CANDIDATE_NOT_CREATED
        }

        return OFFNutritionReferenceCandidateGapCause
            .UNCLASSIFIED
    }

    private fun classifyPriority(
        cause: OFFNutritionReferenceCandidateGapCause,
        rawOFFProductWithUsableNutritionCount: Int
    ): OFFNutritionReferenceCandidateGapPriority {

        return when (cause) {

            OFFNutritionReferenceCandidateGapCause
                .CANDIDATE_IDENTITY_MISMATCH,

            OFFNutritionReferenceCandidateGapCause
                .CANDIDATE_NUTRITION_REJECTED,

            OFFNutritionReferenceCandidateGapCause
                .CANDIDATE_REFERENCE_INELIGIBLE,

            OFFNutritionReferenceCandidateGapCause
                .CANDIDATE_NOT_CREATED,

            OFFNutritionReferenceCandidateGapCause
                .REFERENCE_AGGREGATION_GAP -> {

                if (rawOFFProductWithUsableNutritionCount > 0) {
                    OFFNutritionReferenceCandidateGapPriority.HIGH
                } else {
                    OFFNutritionReferenceCandidateGapPriority.MEDIUM
                }
            }

            OFFNutritionReferenceCandidateGapCause
                .MATCHER_RETRIEVAL_GAP ->
                OFFNutritionReferenceCandidateGapPriority.MEDIUM

            OFFNutritionReferenceCandidateGapCause
                .NO_USABLE_RAW_NUTRITION,

            OFFNutritionReferenceCandidateGapCause
                .NO_RAW_OFF_PRODUCT ->
                OFFNutritionReferenceCandidateGapPriority.LOW

            OFFNutritionReferenceCandidateGapCause
                .UNCLASSIFIED ->
                OFFNutritionReferenceCandidateGapPriority.MEDIUM
        }
    }

    private fun recommendedAction(
        cause: OFFNutritionReferenceCandidateGapCause
    ): String {

        return when (cause) {

            OFFNutritionReferenceCandidateGapCause
                .NO_RAW_OFF_PRODUCT ->
                "Keep unresolved for OFF and cover through another " +
                        "nutrition source."

            OFFNutritionReferenceCandidateGapCause
                .NO_USABLE_RAW_NUTRITION ->
                "Keep unresolved for OFF until usable core nutrition " +
                        "is available or use another nutrition source."

            OFFNutritionReferenceCandidateGapCause
                .CANDIDATE_IDENTITY_MISMATCH ->
                "Inspect identity normalization and aliases for the " +
                        "matching OFF products."

            OFFNutritionReferenceCandidateGapCause
                .CANDIDATE_NUTRITION_REJECTED ->
                "Inspect nutrition payload parsing and core-nutrient " +
                        "validation."

            OFFNutritionReferenceCandidateGapCause
                .CANDIDATE_REFERENCE_INELIGIBLE ->
                "Inspect reference eligibility rules and rejection reasons."

            OFFNutritionReferenceCandidateGapCause
                .CANDIDATE_NOT_CREATED ->
                "Compare usable raw OFF products with generator traces and " +
                        "identify the first rejected generator condition."

            OFFNutritionReferenceCandidateGapCause
                .REFERENCE_AGGREGATION_GAP ->
                "Inspect candidate deduplication and reference aggregation."

            OFFNutritionReferenceCandidateGapCause
                .MATCHER_RETRIEVAL_GAP ->
                "Inspect matcher candidate export and retrieval aliases."

            OFFNutritionReferenceCandidateGapCause
                .UNCLASSIFIED ->
                "Inspect the complete diagnostic trace manually."
        }
    }

    private fun JsonObject.requiredString(
        name: String
    ): String {

        val value =
            get(name)
                ?.takeUnless { element ->
                    element.isJsonNull
                }
                ?.asString
                ?.trim()

        require(!value.isNullOrBlank()) {
            "Required string '$name' is missing or blank."
        }

        return value
    }

    private fun JsonObject.requiredInt(
        name: String
    ): Int {

        val element =
            get(name)

        require(element != null && !element.isJsonNull) {
            "Required integer '$name' is missing."
        }

        return element.asInt
    }

    private fun JsonObject.requiredLong(
        name: String
    ): Long {

        val element =
            get(name)

        require(element != null && !element.isJsonNull) {
            "Required long '$name' is missing."
        }

        return element.asLong
    }

    private fun JsonObject.requiredArray(
        name: String
    ): JsonArray {

        val element =
            get(name)

        require(
            element != null &&
                    !element.isJsonNull &&
                    element.isJsonArray
        ) {
            "Required array '$name' is missing."
        }

        return element.asJsonArray
    }

    private fun JsonObject.optionalBoolean(
        name: String
    ): Boolean? {

        val element =
            get(name)
                ?: return null

        if (element.isJsonNull) {
            return null
        }

        return element.asBoolean
    }

    private fun JsonObject.stringList(
        name: String
    ): List<String> {

        val element =
            get(name)
                ?: return emptyList()

        if (
            element.isJsonNull ||
            !element.isJsonArray
        ) {
            return emptyList()
        }

        return element
            .asJsonArray
            .mapNotNull { value ->
                value
                    .takeUnless { item ->
                        item.isJsonNull
                    }
                    ?.asString
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
            }
            .distinct()
            .sorted()
    }

    private companion object {

        const val REPORT_VERSION =
            1

        const val STAGE_RAW_OFF_PRODUCT =
            "RAW_OFF_PRODUCT"

        const val STAGE_RAW_OFF_USABLE_NUTRITION =
            "RAW_OFF_USABLE_NUTRITION"

        const val STAGE_REFERENCE_CANDIDATE =
            "REFERENCE_CANDIDATE"

        const val STAGE_REFERENCE_AGGREGATE =
            "REFERENCE_AGGREGATE"

        const val STAGE_MATCHER_CANDIDATE =
            "MATCHER_CANDIDATE"
    }
}