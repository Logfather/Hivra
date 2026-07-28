package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.trace

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

class OFFNutritionMissingGeneratorTraceDiagnoser {

    fun diagnose(
        rejectionReportFile: File,
        sourceCandidateFile: File,
        qualityFilteredCandidateFile: File,
        deduplicatedCandidateFile: File,
        traceFile: File
    ): OFFNutritionMissingGeneratorTraceReport {

        require(rejectionReportFile.isFile) {
            "Rejection report not found: " +
                    rejectionReportFile.absolutePath
        }

        val rejectionRoot =
            rejectionReportFile
                .reader()
                .use { reader ->
                    JsonParser
                        .parseReader(reader)
                        .asJsonObject
                }

        val sourceRejectionReportVersion =
            rejectionRoot.requiredInt("version")

        val sourceIndex =
            OFFNutritionArtifactIdentityIndex.read(
                sourceCandidateFile
            )

        val qualityFilteredIndex =
            OFFNutritionArtifactIdentityIndex.read(
                qualityFilteredCandidateFile
            )

        val deduplicatedIndex =
            OFFNutritionArtifactIdentityIndex.read(
                deduplicatedCandidateFile
            )

        val traceIndex =
            OFFNutritionArtifactIdentityIndex.read(
                traceFile
            )

        val findings =
            rejectionRoot
                .requiredArray("findings")
                .map(JsonElement::getAsJsonObject)
                .filter { finding ->
                    finding.requiredString(
                        "firstRejectionStage"
                    ) == SOURCE_STAGE_TRACE_NOT_FOUND
                }
                .map { sourceFinding ->
                    diagnoseFinding(
                        sourceFinding =
                            sourceFinding,
                        sourceIndex =
                            sourceIndex,
                        qualityFilteredIndex =
                            qualityFilteredIndex,
                        deduplicatedIndex =
                            deduplicatedIndex,
                        traceIndex =
                            traceIndex
                    )
                }
                .sortedWith(
                    OFFNutritionMissingGeneratorTraceReport
                        .FINDING_COMPARATOR
                )

        return OFFNutritionMissingGeneratorTraceReport(
            version =
                REPORT_VERSION,
            sourceRejectionReportVersion =
                sourceRejectionReportVersion,
            missingTraceFindingCount =
                findings.size,
            diagnosedFindingCount =
                findings.size,
            countsByFirstMissingStage =
                findings
                    .groupingBy(
                        OFFNutritionMissingGeneratorTraceFinding::
                        firstMissingStage
                    )
                    .eachCount()
                    .toSortedMap(
                        compareBy(
                            OFFNutritionMissingGeneratorTraceStage::name
                        )
                    ),
            countsByCause =
                findings
                    .groupingBy(
                        OFFNutritionMissingGeneratorTraceFinding::cause
                    )
                    .eachCount()
                    .toSortedMap(
                        compareBy(
                            OFFNutritionMissingGeneratorTraceCause::name
                        )
                    ),
            sourceCandidateMatchedFindingCount =
                findings.count { finding ->
                    finding.sourceCandidateMatch.present
                },
            qualityFilteredCandidateMatchedFindingCount =
                findings.count { finding ->
                    finding.qualityFilteredCandidateMatch.present
                },
            deduplicatedCandidateMatchedFindingCount =
                findings.count { finding ->
                    finding.deduplicatedCandidateMatch.present
                },
            generatorTraceMatchedFindingCount =
                findings.count { finding ->
                    finding.generatorTraceMatch.present
                },
            findings =
                findings
        )
    }

    private fun diagnoseFinding(
        sourceFinding: JsonObject,
        sourceIndex: OFFNutritionArtifactIdentityIndex,
        qualityFilteredIndex: OFFNutritionArtifactIdentityIndex,
        deduplicatedIndex: OFFNutritionArtifactIdentityIndex,
        traceIndex: OFFNutritionArtifactIdentityIndex
    ): OFFNutritionMissingGeneratorTraceFinding {

        val catalogKey =
            sourceFinding.requiredString("catalogKey")

        val normalizedEnglish =
            sourceFinding.requiredString("normalizedEnglish")

        val matchedRawProductNames =
            sourceFinding
                .stringList("matchedRawProductNames")

        val matchedRawProductIds =
            sourceFinding.stringList(
                "matchedRawProductIds"
            )

        val matchedRawProductWithUsableNutritionIds =
            sourceFinding.stringList(
                "matchedRawProductWithUsableNutritionIds"
            )


        val lookupIdentities =
            buildSet {

                addAll(
                    matchedRawProductWithUsableNutritionIds
                )

                addAll(
                    matchedRawProductIds
                )

                add(catalogKey)

                add(normalizedEnglish)

                addAll(
                    matchedRawProductNames
                )
            }

        val sourceCandidateMatch =
            sourceIndex.match(lookupIdentities)

        val qualityFilteredCandidateMatch =
            qualityFilteredIndex.match(lookupIdentities)

        val deduplicatedCandidateMatch =
            deduplicatedIndex.match(lookupIdentities)

        val generatorTraceMatch =
            traceIndex.match(lookupIdentities)

        val diagnosis =
            classify(
                sourceCandidateMatch =
                    sourceCandidateMatch,
                qualityFilteredCandidateMatch =
                    qualityFilteredCandidateMatch,
                deduplicatedCandidateMatch =
                    deduplicatedCandidateMatch,
                generatorTraceMatch =
                    generatorTraceMatch
            )

        return OFFNutritionMissingGeneratorTraceFinding(
            catalogIndex =
                sourceFinding.requiredInt("catalogIndex"),
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
            matchedRawProductIds =
                matchedRawProductIds,
            matchedRawProductWithUsableNutritionIds =
                matchedRawProductWithUsableNutritionIds,
            matchedRawProductNames =
                matchedRawProductNames,
            sourceCandidateMatch =
                sourceCandidateMatch,
            qualityFilteredCandidateMatch =
                qualityFilteredCandidateMatch,
            deduplicatedCandidateMatch =
                deduplicatedCandidateMatch,
            generatorTraceMatch =
                generatorTraceMatch,
            firstMissingStage =
                diagnosis.firstMissingStage,
            cause =
                diagnosis.cause,
            diagnosticReasons =
                diagnosis.diagnosticReasons,
            recommendedAction =
                diagnosis.recommendedAction
        )
    }

    private fun classify(
        sourceCandidateMatch: OFFNutritionArtifactIdentityMatch,
        qualityFilteredCandidateMatch:
        OFFNutritionArtifactIdentityMatch,
        deduplicatedCandidateMatch:
        OFFNutritionArtifactIdentityMatch,
        generatorTraceMatch:
        OFFNutritionArtifactIdentityMatch
    ): Diagnosis {

        if (!sourceCandidateMatch.present) {
            return Diagnosis(
                firstMissingStage =
                    OFFNutritionMissingGeneratorTraceStage
                        .SOURCE_REFERENCE_CANDIDATE,
                cause =
                    OFFNutritionMissingGeneratorTraceCause
                        .SOURCE_CANDIDATE_NOT_FOUND,
                diagnosticReasons =
                    listOf(
                        "Usable raw OFF products were reported, but none of " +
                                "their identities occurs in the unfiltered " +
                                "nutrition reference candidate artifact."
                    ),
                recommendedAction =
                    "Inspect the raw-OFF-to-reference-candidate input " +
                            "selection and identity projection."
            )
        }

        if (!qualityFilteredCandidateMatch.present) {
            return Diagnosis(
                firstMissingStage =
                    OFFNutritionMissingGeneratorTraceStage
                        .QUALITY_FILTERED_REFERENCE_CANDIDATE,
                cause =
                    OFFNutritionMissingGeneratorTraceCause
                        .REMOVED_BY_QUALITY_FILTER,
                diagnosticReasons =
                    listOf(
                        "A source reference candidate exists, but no matching " +
                                "candidate survives the quality filter."
                    ),
                recommendedAction =
                    "Inspect the quality-filter rejection reasons for the " +
                            "matched source candidates."
            )
        }

        if (!deduplicatedCandidateMatch.present) {
            return Diagnosis(
                firstMissingStage =
                    OFFNutritionMissingGeneratorTraceStage
                        .DEDUPLICATED_REFERENCE_CANDIDATE,
                cause =
                    OFFNutritionMissingGeneratorTraceCause
                        .REMOVED_BY_DEDUPLICATION,
                diagnosticReasons =
                    listOf(
                        "A quality-filtered reference candidate exists, but " +
                                "no matching candidate survives deduplication."
                    ),
                recommendedAction =
                    "Inspect the deduplication winner and canonical identity " +
                            "chosen for the removed candidates."
            )
        }

        if (!generatorTraceMatch.present) {
            return Diagnosis(
                firstMissingStage =
                    OFFNutritionMissingGeneratorTraceStage
                        .GENERATOR_TRACE,
                cause =
                    OFFNutritionMissingGeneratorTraceCause
                        .TRACE_NOT_EMITTED,
                diagnosticReasons =
                    listOf(
                        "A deduplicated reference candidate exists, but no " +
                                "matching generator trace was persisted."
                    ),
                recommendedAction =
                    "Move trace emission to the beginning of candidate " +
                            "processing or inspect generator input iteration."
            )
        }

        return Diagnosis(
            firstMissingStage =
                OFFNutritionMissingGeneratorTraceStage.UNKNOWN,
            cause =
                OFFNutritionMissingGeneratorTraceCause
                    .ARTIFACT_IDENTITY_MISMATCH,
            diagnosticReasons =
                listOf(
                    "The trace artifact contains a matching identity, but the " +
                            "previous rejection classifier did not associate it " +
                            "with this catalog finding."
                ),
            recommendedAction =
                "Align both diagnostics on the shared artifact identity " +
                        "normalizer and source-product identity."
        )
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

    private fun JsonObject.requiredArray(
        name: String
    ) =
        get(name)
            ?.takeIf(JsonElement::isJsonArray)
            ?.asJsonArray
            ?: error(
                "Required array '$name' is missing."
            )

    private fun JsonObject.stringList(
        name: String
    ): List<String> {

        val value =
            get(name)
                ?: return emptyList()

        if (!value.isJsonArray) {
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

    private data class Diagnosis(
        val firstMissingStage:
        OFFNutritionMissingGeneratorTraceStage,
        val cause:
        OFFNutritionMissingGeneratorTraceCause,
        val diagnosticReasons: List<String>,
        val recommendedAction: String
    )

    private companion object {

        const val REPORT_VERSION =
            1

        const val SOURCE_STAGE_TRACE_NOT_FOUND =
            "TRACE_NOT_FOUND"
    }
}