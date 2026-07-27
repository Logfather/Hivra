package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.classification

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.text.Normalizer

class OFFNutritionReferenceCandidateCreationRejectionClassifier {

    fun classify(
        gapAnalysisFile: File,
        traceFile: File
    ): OFFNutritionReferenceCandidateCreationRejectionReport {

        require(gapAnalysisFile.isFile) {
            "OFF nutrition gap analysis file not found: " +
                    gapAnalysisFile.absolutePath
        }

        require(traceFile.isFile) {
            "OFF nutrition candidate trace file not found: " +
                    traceFile.absolutePath
        }

        val gapRoot =
            gapAnalysisFile
                .reader()
                .use { reader ->
                    JsonParser
                        .parseReader(reader)
                        .asJsonObject
                }

        val traceRoot =
            traceFile
                .reader()
                .use { reader ->
                    JsonParser.parseReader(reader)
                }

        val sourceGapAnalysisVersion =
            gapRoot.requiredInt("version")

        val sourceTraceVersion =
            when {
                traceRoot.isJsonObject ->
                    traceRoot
                        .asJsonObject
                        .optionalInt("version")

                else ->
                    null
            }

        val traces =
            traceRoot
                .resolveTraceArray()
                .map { element ->
                    parseTrace(element.asJsonObject)
                }

        val tracesByIdentity =
            buildTraceIdentityIndex(traces)

        val sourceFindings =
            gapRoot
                .requiredArray("findings")
                .map(JsonElement::getAsJsonObject)
                .filter { finding ->
                    finding.requiredString("cause") ==
                            SOURCE_CAUSE_CANDIDATE_NOT_CREATED
                }

        val findings =
            sourceFindings
                .map { sourceFinding ->
                    classifyFinding(
                        sourceFinding =
                            sourceFinding,
                        tracesByIdentity =
                            tracesByIdentity
                    )
                }
                .sortedWith(
                    OFFNutritionReferenceCandidateCreationRejectionReport
                        .FINDING_COMPARATOR
                )

        val countsByFirstRejectionStage =
            enumValues<
                    OFFNutritionReferenceCandidateCreationRejectionStage
                    >()
                .associateWith { stage ->
                    findings.count { finding ->
                        finding.firstRejectionStage == stage
                    }
                }
                .filterValues { count ->
                    count > 0
                }

        val countsByRejectionStage =
            enumValues<
                    OFFNutritionReferenceCandidateCreationRejectionStage
                    >()
                .associateWith { stage ->
                    findings.sumOf { finding ->
                        finding.countsByRejectionStage[stage] ?: 0
                    }
                }
                .filterValues { count ->
                    count > 0
                }

        val countsByRejectionReason =
            findings
                .flatMap { finding ->
                    finding.countsByRejectionReason
                        .entries
                        .map { entry ->
                            entry.key to entry.value
                        }
                }
                .groupBy(
                    keySelector =
                        Pair<String, Int>::first,
                    valueTransform =
                        Pair<String, Int>::second
                )
                .mapValues { (_, counts) ->
                    counts.sum()
                }
                .toSortedMap()

        return OFFNutritionReferenceCandidateCreationRejectionReport(
            version =
                REPORT_VERSION,
            sourceGapAnalysisVersion =
                sourceGapAnalysisVersion,
            sourceTraceVersion =
                sourceTraceVersion,
            candidateNotCreatedFindingCount =
                findings.size,
            classifiedFindingCount =
                findings.count { finding ->
                    finding.firstRejectionStage !=
                            OFFNutritionReferenceCandidateCreationRejectionStage
                                .TRACE_NOT_FOUND
                },
            unmatchedFindingCount =
                findings.count { finding ->
                    finding.firstRejectionStage ==
                            OFFNutritionReferenceCandidateCreationRejectionStage
                                .TRACE_NOT_FOUND
                },
            matchedTraceCount =
                findings.sumOf { finding ->
                    finding.matchedTraceCount
                },
            rejectedTraceCount =
                findings.sumOf { finding ->
                    finding.rejectedTraceCount
                },
            createdTraceCount =
                findings.sumOf { finding ->
                    finding.createdTraceCount
                },
            countsByFirstRejectionStage =
                countsByFirstRejectionStage,
            countsByRejectionStage =
                countsByRejectionStage,
            countsByRejectionReason =
                countsByRejectionReason,
            findings =
                findings
        )
    }

    private fun classifyFinding(
        sourceFinding: JsonObject,
        tracesByIdentity: Map<String, List<TraceRecord>>
    ): OFFNutritionReferenceCandidateCreationRejectionFinding {

        val catalogIndex =
            sourceFinding.requiredInt("catalogIndex")

        val catalogKey =
            sourceFinding.requiredString("catalogKey")

        val normalizedEnglish =
            sourceFinding.requiredString("normalizedEnglish")

        val lookupIdentities =
            buildSet {
                add(normalizeIdentity(catalogKey))
                add(normalizeIdentity(normalizedEnglish))

                sourceFinding
                    .stringList("matchedRawProductNames")
                    .forEach { productName ->
                        add(normalizeIdentity(productName))
                    }
            }
                .filter(String::isNotBlank)
                .toSortedSet()

        val matchedTraces =
            lookupIdentities
                .flatMap { identity ->
                    tracesByIdentity[identity].orEmpty()
                }
                .distinctBy { trace ->
                    trace.identity
                }
                .sortedWith(
                    compareBy(
                        TraceRecord::sourceProductId,
                        TraceRecord::productName
                    )
                )

        val rejectedTraces =
            matchedTraces.filterNot(TraceRecord::candidateCreated)

        val createdTraces =
            matchedTraces.filter(TraceRecord::candidateCreated)

        val countsByRejectionStage =
            rejectedTraces
                .groupingBy(::classifyTraceStage)
                .eachCount()
                .toSortedMap(
                    compareBy(
                        OFFNutritionReferenceCandidateCreationRejectionStage::name
                    )
                )

        val countsByRejectionReason =
            rejectedTraces
                .flatMap(::rejectionReasons)
                .groupingBy { reason ->
                    reason
                }
                .eachCount()
                .toSortedMap()

        val firstRejectionStage =
            selectFirstRejectionStage(
                matchedTraces =
                    matchedTraces,
                countsByRejectionStage =
                    countsByRejectionStage
            )

        return OFFNutritionReferenceCandidateCreationRejectionFinding(
            catalogIndex =
                catalogIndex,
            catalogKey =
                catalogKey,
            normalizedEnglish =
                normalizedEnglish,
            rawOFFProductMatchCount =
                sourceFinding.requiredInt(
                    "rawOFFProductMatchCount"
                ),
            rawOFFProductWithUsableNutritionCount =
                sourceFinding.requiredInt(
                    "rawOFFProductWithUsableNutritionCount"
                ),
            matchedTraceCount =
                matchedTraces.size,
            rejectedTraceCount =
                rejectedTraces.size,
            createdTraceCount =
                createdTraces.size,
            firstRejectionStage =
                firstRejectionStage,
            countsByRejectionStage =
                countsByRejectionStage,
            countsByRejectionReason =
                countsByRejectionReason,
            matchedSourceProductIds =
                matchedTraces
                    .map(TraceRecord::sourceProductId)
                    .distinct()
                    .sorted(),
            matchedProductNames =
                matchedTraces
                    .map(TraceRecord::productName)
                    .distinct()
                    .sorted()
        )
    }

    private fun buildTraceIdentityIndex(
        traces: List<TraceRecord>
    ): Map<String, List<TraceRecord>> {

        val mutableIndex =
            mutableMapOf<String, MutableList<TraceRecord>>()

        traces.forEach { trace ->

            val identities =
                buildSet<String> {
                    add(
                        normalizeIdentity(
                            trace.productName
                        )
                    )

                    trace.normalizedProductIdentities
                        .forEach { identity ->
                            add(
                                normalizeIdentity(identity)
                            )
                        }
                }
                    .filter(String::isNotBlank)
                    .toSortedSet()

            identities.forEach { identity ->

                mutableIndex
                    .getOrPut(identity) {
                        mutableListOf()
                    }
                    .add(trace)
            }
        }

        return mutableIndex
            .mapValues { (_, indexedTraces) ->
                indexedTraces
                    .distinctBy { trace ->
                        trace.identity
                    }
                    .sortedWith(
                        compareBy(
                            TraceRecord::sourceProductId,
                            TraceRecord::productName
                        )
                    )
            }
            .toSortedMap()
    }

    private fun parseTrace(
        json: JsonObject
    ): TraceRecord {

        return TraceRecord(
            sourceProductId =
                json.requiredString("sourceProductId"),
            productName =
                json.requiredString("productName"),
            normalizedProductIdentities =
                json.stringList(
                    "normalizedProductIdentities"
                ),
            identityAccepted =
                json.requiredBoolean("identityAccepted"),
            identityRejectionReasons =
                json.stringList(
                    "identityRejectionReasons"
                ),
            nutritionAccepted =
                json.requiredBoolean("nutritionAccepted"),
            nutritionRejectionReasons =
                json.stringList(
                    "nutritionRejectionReasons"
                ),
            referenceEligible =
                json.requiredBoolean("referenceEligible"),
            referenceEligibilityRejectionReasons =
                json.stringList(
                    "referenceEligibilityRejectionReasons"
                ),
            candidateCreated =
                json.requiredBoolean("candidateCreated")
        )
    }

    private fun classifyTraceStage(
        trace: TraceRecord
    ): OFFNutritionReferenceCandidateCreationRejectionStage {

        return when {
            !trace.identityAccepted ->
                OFFNutritionReferenceCandidateCreationRejectionStage.IDENTITY

            !trace.nutritionAccepted ->
                OFFNutritionReferenceCandidateCreationRejectionStage.NUTRITION

            !trace.referenceEligible ->
                OFFNutritionReferenceCandidateCreationRejectionStage
                    .REFERENCE_ELIGIBILITY

            !trace.candidateCreated ->
                OFFNutritionReferenceCandidateCreationRejectionStage
                    .CANDIDATE_CREATION

            else ->
                OFFNutritionReferenceCandidateCreationRejectionStage.UNKNOWN
        }
    }

    private fun rejectionReasons(
        trace: TraceRecord
    ): List<String> {

        val reasons =
            when (classifyTraceStage(trace)) {
                OFFNutritionReferenceCandidateCreationRejectionStage.IDENTITY ->
                    trace.identityRejectionReasons

                OFFNutritionReferenceCandidateCreationRejectionStage.NUTRITION ->
                    trace.nutritionRejectionReasons

                OFFNutritionReferenceCandidateCreationRejectionStage
                    .REFERENCE_ELIGIBILITY ->
                    trace.referenceEligibilityRejectionReasons

                OFFNutritionReferenceCandidateCreationRejectionStage
                    .CANDIDATE_CREATION ->
                    emptyList()

                OFFNutritionReferenceCandidateCreationRejectionStage
                    .TRACE_NOT_FOUND ->
                    emptyList()

                OFFNutritionReferenceCandidateCreationRejectionStage.UNKNOWN ->
                    emptyList()
            }

        return reasons
            .ifEmpty {
                listOf(
                    when (classifyTraceStage(trace)) {
                        OFFNutritionReferenceCandidateCreationRejectionStage
                            .IDENTITY ->
                            REASON_IDENTITY_REJECTED_WITHOUT_REASON

                        OFFNutritionReferenceCandidateCreationRejectionStage
                            .NUTRITION ->
                            REASON_NUTRITION_REJECTED_WITHOUT_REASON

                        OFFNutritionReferenceCandidateCreationRejectionStage
                            .REFERENCE_ELIGIBILITY ->
                            REASON_REFERENCE_REJECTED_WITHOUT_REASON

                        OFFNutritionReferenceCandidateCreationRejectionStage
                            .CANDIDATE_CREATION ->
                            REASON_CANDIDATE_NOT_CREATED_WITHOUT_REASON

                        else ->
                            REASON_UNKNOWN
                    }
                )
            }
            .distinct()
            .sorted()
    }

    private fun selectFirstRejectionStage(
        matchedTraces: List<TraceRecord>,
        countsByRejectionStage:
        Map<OFFNutritionReferenceCandidateCreationRejectionStage, Int>
    ): OFFNutritionReferenceCandidateCreationRejectionStage {

        if (matchedTraces.isEmpty()) {
            return OFFNutritionReferenceCandidateCreationRejectionStage
                .TRACE_NOT_FOUND
        }

        return FIRST_REJECTION_STAGE_ORDER
            .firstOrNull { stage ->
                (countsByRejectionStage[stage] ?: 0) > 0
            }
            ?: OFFNutritionReferenceCandidateCreationRejectionStage.UNKNOWN
    }

    private fun normalizeIdentity(
        value: String
    ): String {

        val decomposed =
            Normalizer.normalize(
                value,
                Normalizer.Form.NFD
            )

        return decomposed
            .replace(COMBINING_MARK_REGEX, "")
            .lowercase()
            .replace("&quot;", " ")
            .replace(NON_ALPHANUMERIC_REGEX, " ")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    private fun JsonElement.resolveTraceArray(): JsonArray {

        if (isJsonArray) {
            return asJsonArray
        }

        require(isJsonObject) {
            "Candidate trace file must contain an object or array."
        }

        val root =
            asJsonObject

        val traceElement =
            root.get("traces")
                ?: root.get("entries")
                ?: root.get("findings")

        require(
            traceElement != null &&
                    traceElement.isJsonArray
        ) {
            "Candidate trace object contains no traces array."
        }

        return traceElement.asJsonArray
    }

    private fun JsonObject.requiredString(
        name: String
    ): String {

        val value =
            get(name)
                ?.takeUnless(JsonElement::isJsonNull)
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

        val value =
            get(name)

        require(
            value != null &&
                    !value.isJsonNull
        ) {
            "Required integer '$name' is missing."
        }

        return value.asInt
    }

    private fun JsonObject.optionalInt(
        name: String
    ): Int? {

        val value =
            get(name)
                ?: return null

        if (value.isJsonNull) {
            return null
        }

        return value.asInt
    }

    private fun JsonObject.requiredBoolean(
        name: String
    ): Boolean {

        val value =
            get(name)

        require(
            value != null &&
                    !value.isJsonNull
        ) {
            "Required boolean '$name' is missing."
        }

        return value.asBoolean
    }

    private fun JsonObject.requiredArray(
        name: String
    ): JsonArray {

        val value =
            get(name)

        require(
            value != null &&
                    value.isJsonArray
        ) {
            "Required array '$name' is missing."
        }

        return value.asJsonArray
    }

    private fun JsonObject.stringList(
        name: String
    ): List<String> {

        val value =
            get(name)
                ?: return emptyList()

        if (
            value.isJsonNull ||
            !value.isJsonArray
        ) {
            return emptyList()
        }

        return value
            .asJsonArray
            .mapNotNull { element ->
                element
                    .takeUnless(JsonElement::isJsonNull)
                    ?.asString
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
            }
            .distinct()
            .sorted()
    }

    private data class TraceRecord(
        val sourceProductId: String,
        val productName: String,
        val normalizedProductIdentities: List<String>,
        val identityAccepted: Boolean,
        val identityRejectionReasons: List<String>,
        val nutritionAccepted: Boolean,
        val nutritionRejectionReasons: List<String>,
        val referenceEligible: Boolean,
        val referenceEligibilityRejectionReasons: List<String>,
        val candidateCreated: Boolean
    ) {

        val identity: String
            get() =
                listOf(
                    sourceProductId,
                    productName
                )
                    .joinToString("|")
    }

    private companion object {

        const val REPORT_VERSION =
            1

        const val SOURCE_CAUSE_CANDIDATE_NOT_CREATED =
            "CANDIDATE_NOT_CREATED"

        const val REASON_IDENTITY_REJECTED_WITHOUT_REASON =
            "IDENTITY_REJECTED_WITHOUT_REASON"

        const val REASON_NUTRITION_REJECTED_WITHOUT_REASON =
            "NUTRITION_REJECTED_WITHOUT_REASON"

        const val REASON_REFERENCE_REJECTED_WITHOUT_REASON =
            "REFERENCE_ELIGIBILITY_REJECTED_WITHOUT_REASON"

        const val REASON_CANDIDATE_NOT_CREATED_WITHOUT_REASON =
            "CANDIDATE_NOT_CREATED_WITHOUT_REASON"

        const val REASON_UNKNOWN =
            "UNKNOWN_REJECTION_REASON"

        val FIRST_REJECTION_STAGE_ORDER =
            listOf(
                OFFNutritionReferenceCandidateCreationRejectionStage.IDENTITY,
                OFFNutritionReferenceCandidateCreationRejectionStage.NUTRITION,
                OFFNutritionReferenceCandidateCreationRejectionStage
                    .REFERENCE_ELIGIBILITY,
                OFFNutritionReferenceCandidateCreationRejectionStage
                    .CANDIDATE_CREATION,
                OFFNutritionReferenceCandidateCreationRejectionStage.UNKNOWN
            )

        val COMBINING_MARK_REGEX =
            Regex("\\p{M}+")

        val NON_ALPHANUMERIC_REGEX =
            Regex("[^\\p{L}\\p{N}]+")

        val WHITESPACE_REGEX =
            Regex("\\s+")
    }
}