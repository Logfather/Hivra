package de.shopme.presentation.developer.foodintelligence.nutrition

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy.ResultingNutritionConflictPolicyStatus
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.report.NutritionKnowledgeQualityReport
import de.shopme.tools.knowledge.off.nutrition.reference.freeze.OFFNutritionSourceSnapshotStatus
import java.text.NumberFormat
import java.util.Locale

class NutritionKnowledgeQualityUiMapper {

    fun map(
        report: NutritionKnowledgeQualityReport
    ): NutritionKnowledgeQualityUiModel {

        return NutritionKnowledgeQualityUiModel(
            overallStatus =
                mapOverallStatus(
                    report
                ),
            validation =
                mapValidation(
                    report
                ),
            coverage =
                mapCoverage(
                    report
                ),
            conflicts =
                mapConflicts(
                    report
                ),
            conflictPolicy =
                mapConflictPolicy(
                    report
                ),
            offFreeze =
                mapOffFreeze(
                    report
                )
        )
    }

    private fun mapOverallStatus(
        report: NutritionKnowledgeQualityReport
    ): NutritionKnowledgeQualityStatusUiModel {

        val status =
            when {
                report.approved ->
                    NutritionKnowledgeQualityUiStatus.APPROVED

                report.validation.errorCount > 0L ||
                        report.validation.rejectedEntryCount > 0L ||
                        !report.conflictPolicy.decision.approved ->
                    NutritionKnowledgeQualityUiStatus.REJECTED

                else ->
                    NutritionKnowledgeQualityUiStatus.WARNING
            }

        return when (status) {
            NutritionKnowledgeQualityUiStatus.APPROVED ->
                NutritionKnowledgeQualityStatusUiModel(
                    status =
                        status,
                    label =
                        "APPROVED",
                    description =
                        "Nutrition-Datensatz ist validiert, " +
                                "policy-konform und reproduzierbar eingefroren."
                )

            NutritionKnowledgeQualityUiStatus.WARNING ->
                NutritionKnowledgeQualityStatusUiModel(
                    status =
                        status,
                    label =
                        "WARNING",
                    description =
                        "Nutrition-Datensatz ist verfügbar, enthält jedoch " +
                                "Hinweise oder unvollständige Qualitätsnachweise."
                )

            NutritionKnowledgeQualityUiStatus.REJECTED ->
                NutritionKnowledgeQualityStatusUiModel(
                    status =
                        status,
                    label =
                        "REJECTED",
                    description =
                        "Nutrition-Datensatz erfüllt mindestens eine " +
                                "verbindliche Qualitätsanforderung nicht."
                )
        }
    }

    private fun mapValidation(
        report: NutritionKnowledgeQualityReport
    ): NutritionKnowledgeValidationUiModel {

        val validation =
            report.validation

        val status =
            when {
                !validation.valid ||
                        validation.errorCount > 0L ||
                        validation.rejectedEntryCount > 0L ->
                    NutritionKnowledgeQualityUiStatus.REJECTED

                validation.warningCount > 0L ->
                    NutritionKnowledgeQualityUiStatus.WARNING

                else ->
                    NutritionKnowledgeQualityUiStatus.APPROVED
            }

        val statusLabel =
            when (status) {
                NutritionKnowledgeQualityUiStatus.APPROVED ->
                    "Gültig"

                NutritionKnowledgeQualityUiStatus.WARNING ->
                    "Gültig mit Warnungen"

                NutritionKnowledgeQualityUiStatus.REJECTED ->
                    "Ungültig"
            }

        return NutritionKnowledgeValidationUiModel(
            status =
                status,
            statusLabel =
                statusLabel,
            entryCount =
                formatCount(
                    validation.entryCount
                ),
            validEntryCount =
                formatCount(
                    validation.validEntryCount
                ),
            warningCount =
                formatCount(
                    validation.warningCount
                ),
            errorCount =
                formatCount(
                    validation.errorCount
                ),
            rejectedEntryCount =
                formatCount(
                    validation.rejectedEntryCount
                ),
            summary =
                "${formatCount(validation.errorCount)} Fehler · " +
                        "${formatCount(validation.warningCount)} Warnungen · " +
                        "${formatCount(validation.rejectedEntryCount)} " +
                        "abgelehnte Einträge"
        )
    }

    private fun mapCoverage(
        report: NutritionKnowledgeQualityReport
    ): NutritionKnowledgeCoverageUiModel {

        val coverage =
            report.coverage

        val classification =
            report.coverageGapClassification

        val status =
            when {
                classification.effectiveCoverageRate >=
                        EFFECTIVE_COVERAGE_APPROVED_THRESHOLD ->
                    NutritionKnowledgeQualityUiStatus.APPROVED

                classification.effectiveCoverageRate >=
                        EFFECTIVE_COVERAGE_WARNING_THRESHOLD ->
                    NutritionKnowledgeQualityUiStatus.WARNING

                else ->
                    NutritionKnowledgeQualityUiStatus.REJECTED
            }

        val statusLabel =
            when (status) {
                NutritionKnowledgeQualityUiStatus.APPROVED ->
                    "Sehr hohe effektive Abdeckung"

                NutritionKnowledgeQualityUiStatus.WARNING ->
                    "Eingeschränkte effektive Abdeckung"

                NutritionKnowledgeQualityUiStatus.REJECTED ->
                    "Unzureichende effektive Abdeckung"
            }

        return NutritionKnowledgeCoverageUiModel(
            exactCoveragePercentage =
                formatPercentage(
                    classification.exactCoveragePercentage,
                    fractionDigits =
                        2
                ),
            effectiveCoveragePercentage =
                formatPercentage(
                    classification.effectiveCoveragePercentage,
                    fractionDigits =
                        2
                ),
            aggregateEntryCount =
                formatCount(
                    coverage.aggregateEntryCount
                ),
            runtimeEntryCount =
                formatCount(
                    coverage.runtimeEntryCount
                ),
            exactMatchCount =
                formatCount(
                    classification.exactMatchCount
                ),
            normalizationEquivalentMatchCount =
                formatCount(
                    classification.normalizationEquivalentMatchCount
                ),
            trueMissingEntryCount =
                formatCount(
                    classification.trueMissingRuntimeEntryCount
                ),
            trueAdditionalEntryCount =
                formatCount(
                    classification.trueAdditionalRuntimeEntryCount
                ),
            normalizationCollisionGroupCount =
                formatCount(
                    classification.normalizationCollisionGroupCount
                ),
            complete =
                classification.complete,
            status =
                status,
            statusLabel =
                statusLabel
        )
    }

    private fun mapConflicts(
        report: NutritionKnowledgeQualityReport
    ): NutritionKnowledgeConflictsUiModel {

        val conflicts =
            report.conflicts

        val policy =
            report.conflictPolicy.policy

        val status =
            when {
                conflicts.entryConflictRate >
                        policy.maximumEntryConflictRate ||
                        conflicts.nutrientConflictRate >
                        policy.maximumNutrientConflictRate ->
                    NutritionKnowledgeQualityUiStatus.REJECTED

                conflicts.conflictEntryCount > 0L ->
                    NutritionKnowledgeQualityUiStatus.WARNING

                else ->
                    NutritionKnowledgeQualityUiStatus.APPROVED
            }

        val statusLabel =
            when (status) {
                NutritionKnowledgeQualityUiStatus.APPROVED ->
                    "Keine Konflikte"

                NutritionKnowledgeQualityUiStatus.WARNING ->
                    "Konflikte innerhalb der Policy"

                NutritionKnowledgeQualityUiStatus.REJECTED ->
                    "Policy-Grenzwert überschritten"
            }

        return NutritionKnowledgeConflictsUiModel(
            conflictEntryCount =
                formatCount(
                    conflicts.conflictEntryCount
                ),
            nutrientConflictCount =
                formatCount(
                    conflicts.nutrientConflictCount
                ),
            entryConflictPercentage =
                formatPercentage(
                    conflicts.entryConflictPercentage,
                    fractionDigits =
                        6
                ),
            nutrientConflictPercentage =
                formatPercentage(
                    conflicts.nutrientConflictPercentage,
                    fractionDigits =
                        6
                ),
            comparableEntryCount =
                formatCount(
                    conflicts.comparableEntryCount
                ),
            conflictFreeEntryCount =
                formatCount(
                    conflicts.conflictFreeEntryCount
                ),
            status =
                status,
            statusLabel =
                statusLabel
        )
    }

    private fun mapConflictPolicy(
        report: NutritionKnowledgeQualityReport
    ): NutritionKnowledgeConflictPolicyUiModel {

        val policy =
            report.conflictPolicy.policy

        val decision =
            report.conflictPolicy.decision

        val approved =
            decision.approved &&
                    decision.policyStatus ==
                    ResultingNutritionConflictPolicyStatus.APPROVED

        val status =
            if (approved) {
                NutritionKnowledgeQualityUiStatus.APPROVED
            } else {
                NutritionKnowledgeQualityUiStatus.REJECTED
            }

        return NutritionKnowledgeConflictPolicyUiModel(
            status =
                status,
            statusLabel =
                if (approved) {
                    "APPROVED"
                } else {
                    "REJECTED"
                },
            maximumEntryConflictPercentage =
                formatPercentage(
                    policy.maximumEntryConflictRate * 100.0,
                    fractionDigits =
                        3
                ),
            maximumNutrientConflictPercentage =
                formatPercentage(
                    policy.maximumNutrientConflictRate * 100.0,
                    fractionDigits =
                        3
                ),
            extremeConflictCount =
                formatCount(
                    decision.extremeConflictCount
                ),
            maximumExtremeConflictCount =
                formatCount(
                    decision.maximumExtremeConflictCount
                ),
            automaticCorrectionAllowed =
                policy.automaticCorrectionAllowed,
            automaticEntryRejectionAllowed =
                policy.automaticEntryRejectionAllowed,
            automaticCorrectionLabel =
                if (policy.automaticCorrectionAllowed) {
                    "Automatische Änderungen erlaubt"
                } else {
                    "Keine automatischen Änderungen"
                },
            automaticEntryRejectionLabel =
                if (policy.automaticEntryRejectionAllowed) {
                    "Automatische Löschungen erlaubt"
                } else {
                    "Keine automatischen Löschungen"
                }
        )
    }

    private fun mapOffFreeze(
        report: NutritionKnowledgeQualityReport
    ): NutritionKnowledgeOffFreezeUiModel {

        val snapshot =
            report.offNutritionSourceSnapshot

        val checksumVerified =
            snapshot.sourceSha256 ==
                    snapshot.frozenSha256

        val approved =
            snapshot.status ==
                    OFFNutritionSourceSnapshotStatus.APPROVED &&
                    checksumVerified &&
                    snapshot.nutritionValidationApproved &&
                    snapshot.nutritionConflictPolicyApproved

        val status =
            if (approved) {
                NutritionKnowledgeQualityUiStatus.APPROVED
            } else {
                NutritionKnowledgeQualityUiStatus.REJECTED
            }

        return NutritionKnowledgeOffFreezeUiModel(
            status =
                status,
            statusLabel =
                if (approved) {
                    "Eingefroren und reproduzierbar"
                } else {
                    "Freeze-Nachweis ungültig"
                },
            source =
                snapshot.source,
            sourceVersion =
                snapshot.sourceVersion,
            aggregateEntryCount =
                formatCount(
                    snapshot.aggregateEntryCount
                ),
            frozenFileSize =
                formatFileSizeMegabytes(
                    snapshot.frozenFileSizeBytes
                ),
            sha256 =
                snapshot.frozenSha256,
            checksumVerified =
                checksumVerified,
            checksumLabel =
                if (checksumVerified) {
                    "SHA-256 verifiziert"
                } else {
                    "SHA-256 stimmt nicht überein"
                },
            validationApproved =
                snapshot.nutritionValidationApproved,
            conflictPolicyApproved =
                snapshot.nutritionConflictPolicyApproved
        )
    }

    private fun formatCount(
        value: Long
    ): String {

        require(value >= 0L) {
            "UI count must not be negative: $value."
        }

        return integerFormat.format(
            value
        )
    }

    private fun formatPercentage(
        value: Double,
        fractionDigits: Int
    ): String {

        require(value.isFinite()) {
            "UI percentage must be finite: $value."
        }

        require(fractionDigits >= 0) {
            "Fraction digits must not be negative."
        }

        val format =
            NumberFormat
                .getNumberInstance(
                    Locale.GERMANY
                )
                .apply {
                    minimumFractionDigits =
                        fractionDigits
                    maximumFractionDigits =
                        fractionDigits
                    isGroupingUsed =
                        true
                }

        return format.format(
            value
        ) + " %"
    }

    private fun formatFileSizeMegabytes(
        bytes: Long
    ): String {

        require(bytes >= 0L) {
            "File size must not be negative: $bytes."
        }

        val megabytes =
            bytes.toDouble() /
                    BYTES_PER_MEGABYTE

        val format =
            NumberFormat
                .getNumberInstance(
                    Locale.GERMANY
                )
                .apply {
                    minimumFractionDigits =
                        1
                    maximumFractionDigits =
                        1
                    isGroupingUsed =
                        true
                }

        return format.format(
            megabytes
        ) + " MB"
    }

    companion object {

        private const val EFFECTIVE_COVERAGE_APPROVED_THRESHOLD =
            0.99

        private const val EFFECTIVE_COVERAGE_WARNING_THRESHOLD =
            0.95

        private const val BYTES_PER_MEGABYTE =
            1_000_000.0

        private val integerFormat =
            NumberFormat
                .getIntegerInstance(
                    Locale.GERMANY
                )
                .apply {
                    isGroupingUsed =
                        true
                }
    }
}