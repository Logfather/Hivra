package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.report

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.ResultingNutritionKnowledgeValidationReport
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictReport
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy.ResultingNutritionConflictPolicy
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy.ResultingNutritionConflictPolicyReport
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage.ResultingNutritionCoverageGapClassificationReport
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage.ResultingNutritionCoverageReport
import de.shopme.tools.knowledge.off.nutrition.reference.freeze.OFFNutritionSourceSnapshot
import de.shopme.tools.knowledge.off.nutrition.reference.freeze.OFFNutritionSourceSnapshotReader
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.math.abs

class NutritionKnowledgeQualityReportReader(
    private val gson: Gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .create(),
    private val snapshotReader:
    OFFNutritionSourceSnapshotReader =
        OFFNutritionSourceSnapshotReader()
) {

    fun read(
        files: NutritionKnowledgeQualityReportFiles
    ): NutritionKnowledgeQualityReport {

        val validation =
            readJson(
                file =
                    files.validationReportFile,
                type =
                    ResultingNutritionKnowledgeValidationReport::class.java,
                description =
                    "resulting Nutrition validation report"
            )

        val coverage =
            readJson(
                file =
                    files.coverageReportFile,
                type =
                    ResultingNutritionCoverageReport::class.java,
                description =
                    "resulting Nutrition coverage report"
            )

        val coverageGapClassification =
            readJson(
                file =
                    files.coverageGapClassificationReportFile,
                type =
                    ResultingNutritionCoverageGapClassificationReport::class.java,
                description =
                    "resulting Nutrition coverage gap classification report"
            )

        val conflicts =
            readJson(
                file =
                    files.conflictReportFile,
                type =
                    ResultingNutritionConflictReport::class.java,
                description =
                    "resulting Nutrition conflict report"
            )

        val conflictPolicy =
            readJson(
                file =
                    files.conflictPolicyReportFile,
                type =
                    ResultingNutritionConflictPolicyReport::class.java,
                description =
                    "resulting Nutrition conflict policy report"
            )

        val snapshot =
            snapshotReader.read(
                files.offNutritionSourceSnapshotFile
            )

        validateVersions(
            validation =
                validation,
            coverage =
                coverage,
            coverageGapClassification =
                coverageGapClassification,
            conflicts =
                conflicts,
            conflictPolicy =
                conflictPolicy,
            snapshot =
                snapshot
        )

        validateIndividualReports(
            validation =
                validation,
            coverage =
                coverage,
            coverageGapClassification =
                coverageGapClassification,
            conflicts =
                conflicts,
            conflictPolicy =
                conflictPolicy,
            snapshot =
                snapshot
        )

        validateCrossReportConsistency(
            validation =
                validation,
            coverage =
                coverage,
            coverageGapClassification =
                coverageGapClassification,
            conflicts =
                conflicts,
            conflictPolicy =
                conflictPolicy,
            snapshot =
                snapshot
        )

        return NutritionKnowledgeQualityReport(
            validation =
                validation,
            coverage =
                coverage,
            coverageGapClassification =
                coverageGapClassification,
            conflicts =
                conflicts,
            conflictPolicy =
                conflictPolicy,
            offNutritionSourceSnapshot =
                snapshot
        )
    }

    private fun validateVersions(
        validation:
        ResultingNutritionKnowledgeValidationReport,
        coverage:
        ResultingNutritionCoverageReport,
        coverageGapClassification:
        ResultingNutritionCoverageGapClassificationReport,
        conflicts:
        ResultingNutritionConflictReport,
        conflictPolicy:
        ResultingNutritionConflictPolicyReport,
        snapshot:
        OFFNutritionSourceSnapshot
    ) {
        requireVersion(
            actual =
                validation.version,
            expected =
                ResultingNutritionKnowledgeValidationReport
                    .CURRENT_VERSION,
            artifact =
                "Nutrition validation report"
        )

        requireVersion(
            actual =
                coverage.version,
            expected =
                ResultingNutritionCoverageReport
                    .CURRENT_VERSION,
            artifact =
                "Nutrition coverage report"
        )

        requireVersion(
            actual =
                coverageGapClassification.version,
            expected =
                ResultingNutritionCoverageGapClassificationReport
                    .CURRENT_VERSION,
            artifact =
                "Nutrition coverage gap classification report"
        )

        requireVersion(
            actual =
                conflicts.version,
            expected =
                ResultingNutritionConflictReport
                    .CURRENT_VERSION,
            artifact =
                "Nutrition conflict report"
        )

        requireVersion(
            actual =
                conflictPolicy.version,
            expected =
                ResultingNutritionConflictPolicyReport
                    .CURRENT_VERSION,
            artifact =
                "Nutrition conflict policy report"
        )

        requireVersion(
            actual =
                conflictPolicy.policy.version,
            expected =
                ResultingNutritionConflictPolicy
                    .CURRENT_VERSION,
            artifact =
                "Nutrition conflict policy"
        )

        requireVersion(
            actual =
                snapshot.version,
            expected =
                OFFNutritionSourceSnapshot
                    .CURRENT_VERSION,
            artifact =
                "OFF Nutrition source snapshot"
        )
    }

    private fun validateIndividualReports(
        validation:
        ResultingNutritionKnowledgeValidationReport,
        coverage:
        ResultingNutritionCoverageReport,
        coverageGapClassification:
        ResultingNutritionCoverageGapClassificationReport,
        conflicts:
        ResultingNutritionConflictReport,
        conflictPolicy:
        ResultingNutritionConflictPolicyReport,
        snapshot:
        OFFNutritionSourceSnapshot
    ) {
        require(validation.entryCount >= 0L)
        require(validation.validEntryCount >= 0L)
        require(validation.warningEntryCount >= 0L)
        require(validation.rejectedEntryCount >= 0L)
        require(validation.warningCount >= 0L)
        require(validation.errorCount >= 0L)
        require(validation.issueCount >= 0L)

        require(
            validation.entryCount ==
                    validation.validEntryCount +
                    validation.warningEntryCount +
                    validation.rejectedEntryCount
        ) {
            "Nutrition validation entry counts are inconsistent."
        }

        require(
            validation.issueCount ==
                    validation.warningCount +
                    validation.errorCount
        ) {
            "Nutrition validation issue counts are inconsistent."
        }

        require(
            validation.valid ==
                    (
                            validation.errorCount == 0L &&
                                    validation.rejectedEntryCount == 0L
                            )
        ) {
            "Nutrition validation status is inconsistent with errors " +
                    "or rejected entries."
        }

        requireRate(
            value =
                coverage.aggregateCoverageRate,
            name =
                "Aggregate coverage rate"
        )

        requireEquivalentPercentage(
            rate =
                coverage.aggregateCoverageRate,
            percentage =
                coverage.aggregateCoveragePercentage,
            name =
                "Aggregate coverage"
        )

        require(
            coverage.coveredAggregateEntryCount +
                    coverage.missingRuntimeEntryCount ==
                    coverage.aggregateEntryCount
        ) {
            "Nutrition coverage aggregate counts are inconsistent."
        }

        require(
            coverage.complete ==
                    (
                            coverage.missingRuntimeEntryCount == 0L &&
                                    coverage.additionalRuntimeEntryCount == 0L
                            )
        ) {
            "Nutrition coverage completeness is inconsistent."
        }

        requireRate(
            value =
                coverageGapClassification.exactCoverageRate,
            name =
                "Exact coverage rate"
        )

        requireRate(
            value =
                coverageGapClassification.effectiveCoverageRate,
            name =
                "Effective coverage rate"
        )

        requireEquivalentPercentage(
            rate =
                coverageGapClassification.exactCoverageRate,
            percentage =
                coverageGapClassification.exactCoveragePercentage,
            name =
                "Exact coverage"
        )

        requireEquivalentPercentage(
            rate =
                coverageGapClassification.effectiveCoverageRate,
            percentage =
                coverageGapClassification.effectiveCoveragePercentage,
            name =
                "Effective coverage"
        )

        require(
            coverageGapClassification
                .effectiveCoveredAggregateEntryCount +
                    coverageGapClassification
                        .trueMissingRuntimeEntryCount ==
                    coverageGapClassification
                        .aggregateEntryCount
        ) {
            "Effective Nutrition coverage counts are inconsistent."
        }

        require(
            coverageGapClassification
                .effectiveCoveredAggregateEntryCount ==
                    coverageGapClassification.exactMatchCount +
                    coverageGapClassification
                        .normalizationEquivalentMatchCount
        ) {
            "Exact and normalization-equivalent Nutrition coverage " +
                    "counts are inconsistent."
        }

        require(
            coverageGapClassification.effectiveCoverageRate +
                    RATE_TOLERANCE >=
                    coverageGapClassification.exactCoverageRate
        ) {
            "Effective Nutrition coverage must not be lower than exact " +
                    "coverage."
        }

        requireRate(
            value =
                conflicts.entryConflictRate,
            name =
                "Entry conflict rate"
        )

        requireRate(
            value =
                conflicts.nutrientConflictRate,
            name =
                "Nutrient conflict rate"
        )

        requireEquivalentPercentage(
            rate =
                conflicts.entryConflictRate,
            percentage =
                conflicts.entryConflictPercentage,
            name =
                "Entry conflict"
        )

        requireEquivalentPercentage(
            rate =
                conflicts.nutrientConflictRate,
            percentage =
                conflicts.nutrientConflictPercentage,
            name =
                "Nutrient conflict"
        )

        require(
            conflicts.conflictEntryCount +
                    conflicts.conflictFreeEntryCount ==
                    conflicts.comparableEntryCount
        ) {
            "Nutrition conflict entry counts are inconsistent."
        }

        require(
            conflicts.comparableEntryCount +
                    conflicts.nonComparableEntryCount ==
                    conflicts.matchedEntryCount
        ) {
            "Nutrition conflict comparability counts are inconsistent."
        }

        require(
            conflicts.matchedEntryCount ==
                    conflicts.exactMatchedEntryCount +
                    conflicts
                        .normalizationEquivalentMatchedEntryCount
        ) {
            "Nutrition conflict match counts are inconsistent."
        }

        require(
            conflicts.nutrientConflictCount <=
                    conflicts.nutrientComparisonCount
        ) {
            "Nutrition nutrient conflict count exceeds comparison count."
        }

        require(
            conflictPolicy.decision.policyVersion ==
                    conflictPolicy.policy.version
        ) {
            "Nutrition conflict policy decision version differs from " +
                    "the policy version."
        }

        require(
            conflictPolicy.decision.policyStatus ==
                    conflictPolicy.policy.status
        ) {
            "Nutrition conflict policy decision status differs from " +
                    "the policy status."
        }

        require(
            conflictPolicy.decision.approved ==
                    conflictPolicy.decision.violations.isEmpty()
        ) {
            "Nutrition conflict policy approval differs from its " +
                    "violations."
        }

        require(
            conflictPolicy.policy.maximumEntryConflictRate ==
                    conflictPolicy.decision.maximumEntryConflictRate
        ) {
            "Maximum entry conflict rate differs between policy and " +
                    "decision."
        }

        require(
            conflictPolicy.policy.maximumNutrientConflictRate ==
                    conflictPolicy.decision.maximumNutrientConflictRate
        ) {
            "Maximum nutrient conflict rate differs between policy and " +
                    "decision."
        }

        require(
            conflictPolicy.policy.maximumExtremeConflictCount ==
                    conflictPolicy.decision.maximumExtremeConflictCount
        ) {
            "Maximum extreme conflict count differs between policy and " +
                    "decision."
        }

        require(
            snapshot.sourceFileSizeBytes ==
                    snapshot.frozenFileSizeBytes
        ) {
            "OFF Nutrition source and frozen snapshot sizes differ."
        }

        require(
            snapshot.sourceSha256 ==
                    snapshot.frozenSha256
        ) {
            "OFF Nutrition source and frozen snapshot checksums differ."
        }
    }

    private fun validateCrossReportConsistency(
        validation:
        ResultingNutritionKnowledgeValidationReport,
        coverage:
        ResultingNutritionCoverageReport,
        coverageGapClassification:
        ResultingNutritionCoverageGapClassificationReport,
        conflicts:
        ResultingNutritionConflictReport,
        conflictPolicy:
        ResultingNutritionConflictPolicyReport,
        snapshot:
        OFFNutritionSourceSnapshot
    ) {
        require(
            validation.entryCount ==
                    coverage.runtimeEntryCount
        ) {
            "Nutrition validation and coverage reports describe " +
                    "different runtime entry counts: " +
                    "validation=${validation.entryCount}, " +
                    "coverage=${coverage.runtimeEntryCount}."
        }

        require(
            coverage.aggregateEntryCount ==
                    coverageGapClassification.aggregateEntryCount
        ) {
            "Nutrition coverage reports describe different aggregate " +
                    "entry counts."
        }

        require(
            coverage.runtimeEntryCount ==
                    coverageGapClassification.runtimeEntryCount
        ) {
            "Nutrition coverage reports describe different runtime " +
                    "entry counts."
        }

        require(
            coverage.coveredAggregateEntryCount ==
                    coverageGapClassification.exactMatchCount
        ) {
            "Exact Nutrition coverage differs between coverage and gap " +
                    "classification reports."
        }

        requireApproximatelyEqual(
            left =
                coverage.aggregateCoverageRate,
            right =
                coverageGapClassification.exactCoverageRate,
            message =
                "Exact Nutrition coverage rates differ between reports."
        )

        require(
            coverage.aggregateEntryCount ==
                    conflicts.aggregateEntryCount
        ) {
            "Nutrition coverage and conflict reports describe different " +
                    "aggregate entry counts."
        }

        require(
            coverage.runtimeEntryCount ==
                    conflicts.runtimeEntryCount
        ) {
            "Nutrition coverage and conflict reports describe different " +
                    "runtime entry counts."
        }

        require(
            coverageGapClassification.exactMatchCount ==
                    conflicts.exactMatchedEntryCount
        ) {
            "Exact matched Nutrition entry counts differ between gap " +
                    "classification and conflict reports."
        }

        require(
            coverageGapClassification
                .normalizationEquivalentMatchCount ==
                    conflicts
                        .normalizationEquivalentMatchedEntryCount
        ) {
            "Normalization-equivalent Nutrition match counts differ " +
                    "between reports."
        }

        require(
            coverageGapClassification
                .effectiveCoveredAggregateEntryCount ==
                    conflicts.matchedEntryCount
        ) {
            "Effective Nutrition coverage differs from the conflict " +
                    "report matched entry count."
        }

        requireApproximatelyEqual(
            left =
                conflicts.entryConflictRate,
            right =
                conflictPolicy.decision.entryConflictRate,
            message =
                "Measured entry conflict rate differs from the policy " +
                        "decision."
        )

        requireApproximatelyEqual(
            left =
                conflicts.nutrientConflictRate,
            right =
                conflictPolicy.decision.nutrientConflictRate,
            message =
                "Measured nutrient conflict rate differs from the policy " +
                        "decision."
        )

        require(
            conflicts.conflictEntryCount ==
                    conflictPolicy.decision.conflictEntryCount
        ) {
            "Conflict entry count differs between conflict report and " +
                    "policy decision."
        }

        require(
            conflictPolicy.decision.evaluatedConflictEntryCount ==
                    conflictPolicy.decision.conflictEntryCount
        ) {
            "Nutrition conflict policy decision does not cover every " +
                    "conflict entry."
        }

        require(
            conflictPolicy.decision.omittedConflictEvidenceCount == 0L
        ) {
            "Nutrition conflict policy decision contains omitted " +
                    "conflict evidence."
        }

        require(
            conflictPolicy.decision.omittedEvaluationExampleCount == 0L
        ) {
            "Nutrition conflict policy decision contains omitted " +
                    "evaluation examples."
        }

        require(
            snapshot.aggregateEntryCount ==
                    coverage.aggregateEntryCount
        ) {
            "Frozen OFF Nutrition snapshot and coverage report describe " +
                    "different aggregate entry counts."
        }

        require(
            snapshot.sourceFileSizeBytes ==
                    coverage.aggregateFileSizeBytes
        ) {
            "Frozen OFF Nutrition snapshot and coverage report describe " +
                    "different aggregate file sizes."
        }

        require(
            snapshot.nutritionValidationApproved ==
                    validation.valid
        ) {
            "Frozen OFF Nutrition snapshot validation approval differs " +
                    "from the validation report."
        }

        require(
            snapshot.nutritionConflictPolicyVersion ==
                    conflictPolicy.policy.version
        ) {
            "Frozen OFF Nutrition snapshot conflict policy version " +
                    "differs from the current policy."
        }

        require(
            snapshot.nutritionConflictPolicyApproved ==
                    conflictPolicy.decision.approved
        ) {
            "Frozen OFF Nutrition snapshot policy approval differs from " +
                    "the policy decision."
        }
    }

    private fun <T : Any> readJson(
        file: File,
        type: Class<T>,
        description: String
    ): T {

        require(file.isFile) {
            "$description does not exist: ${file.absolutePath}"
        }

        require(file.length() > 0L) {
            "$description is empty: ${file.absolutePath}"
        }

        return file
            .reader(
                StandardCharsets.UTF_8
            )
            .use { reader ->
                gson.fromJson(
                    reader,
                    type
                )
            }
            ?: error(
                "Could not deserialize $description: " +
                        file.absolutePath
            )
    }

    private fun requireVersion(
        actual: Int,
        expected: Int,
        artifact: String
    ) {
        require(actual == expected) {
            "$artifact version is unsupported: " +
                    "actual=$actual, expected=$expected."
        }
    }

    private fun requireRate(
        value: Double,
        name: String
    ) {
        require(
            value.isFinite() &&
                    value in 0.0..1.0
        ) {
            "$name must be finite and between 0 and 1: $value."
        }
    }

    private fun requireEquivalentPercentage(
        rate: Double,
        percentage: Double,
        name: String
    ) {
        require(
            percentage.isFinite()
        ) {
            "$name percentage must be finite."
        }

        requireApproximatelyEqual(
            left =
                rate * 100.0,
            right =
                percentage,
            message =
                "$name rate and percentage are inconsistent."
        )
    }

    private fun requireApproximatelyEqual(
        left: Double,
        right: Double,
        message: String
    ) {
        require(
            left.isFinite() &&
                    right.isFinite() &&
                    abs(left - right) <=
                    RATE_TOLERANCE
        ) {
            "$message left=$left, right=$right."
        }
    }

    companion object {

        private const val RATE_TOLERANCE =
            1e-12
    }
}