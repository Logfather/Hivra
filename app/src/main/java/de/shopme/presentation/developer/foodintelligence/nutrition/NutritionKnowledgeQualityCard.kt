package de.shopme.presentation.developer.foodintelligence.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.shopme.ui.theme.BrandGreen

@Composable
fun NutritionKnowledgeQualityCard(
    model: NutritionKnowledgeQualityUiModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier =
            modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    2.dp
            )
    ) {
        Column(
            modifier =
                Modifier.padding(
                    20.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {
            NutritionKnowledgeQualityHeader(
                status =
                    model.overallStatus
            )

            HorizontalDivider()

            NutritionValidationSection(
                model =
                    model.validation
            )

            HorizontalDivider()

            NutritionCoverageSection(
                model =
                    model.coverage
            )

            HorizontalDivider()

            NutritionConflictSection(
                model =
                    model.conflicts
            )

            HorizontalDivider()

            NutritionConflictPolicySection(
                model =
                    model.conflictPolicy
            )

            HorizontalDivider()

            NutritionOffFreezeSection(
                model =
                    model.offFreeze
            )
        }
    }
}

@Composable
private fun NutritionKnowledgeQualityHeader(
    status: NutritionKnowledgeQualityStatusUiModel
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(
                8.dp
            )
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text =
                    "Nutrition-Datenqualität",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight =
                    FontWeight.SemiBold,
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            Spacer(
                modifier =
                    Modifier.padding(
                        horizontal = 6.dp
                    )
            )

            NutritionQualityStatusBadge(
                status =
                    status.status,
                label =
                    status.label
            )
        }

        Text(
            text =
                status.description,
            style =
                MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun NutritionValidationSection(
    model: NutritionKnowledgeValidationUiModel
) {
    NutritionQualitySection(
        title =
            "Runtime-Validierung",
        status =
            model.status,
        statusLabel =
            model.statusLabel
    ) {
        NutritionQualityMetricRow(
            label =
                "Einträge",
            value =
                model.entryCount
        )

        NutritionQualityMetricRow(
            label =
                "Gültige Einträge",
            value =
                model.validEntryCount
        )

        Text(
            text =
                model.summary,
            style =
                MaterialTheme.typography.bodyMedium,
            color =
                statusColor(
                    model.status
                )
        )
    }
}

@Composable
private fun NutritionCoverageSection(
    model: NutritionKnowledgeCoverageUiModel
) {
    NutritionQualitySection(
        title =
            "Abdeckung",
        status =
            model.status,
        statusLabel =
            model.statusLabel
    ) {
        NutritionQualityMetricRow(
            label =
                "Exact Coverage",
            value =
                model.exactCoveragePercentage,
            emphasized =
                true
        )

        NutritionQualityMetricRow(
            label =
                "Effective Coverage",
            value =
                model.effectiveCoveragePercentage,
            emphasized =
                true
        )

        Spacer(
            modifier =
                Modifier.height(
                    2.dp
                )
        )

        NutritionQualityMetricRow(
            label =
                "OFF-Aggregate",
            value =
                model.aggregateEntryCount
        )

        NutritionQualityMetricRow(
            label =
                "Runtime-Einträge",
            value =
                model.runtimeEntryCount
        )

        NutritionQualityMetricRow(
            label =
                "Exakte Treffer",
            value =
                model.exactMatchCount
        )

        NutritionQualityMetricRow(
            label =
                "Normalisierungsäquivalente Treffer",
            value =
                model.normalizationEquivalentMatchCount
        )

        NutritionQualityMetricRow(
            label =
                "Echte Missing",
            value =
                model.trueMissingEntryCount,
            valueColor =
                if (
                    model.trueMissingEntryCount ==
                    "0"
                ) {
                    statusColor(
                        NutritionKnowledgeQualityUiStatus.APPROVED
                    )
                } else {
                    MaterialTheme.colorScheme
                        .onSurface
                }
        )

        NutritionQualityMetricRow(
            label =
                "Echte Additional",
            value =
                model.trueAdditionalEntryCount
        )

        NutritionQualityMetricRow(
            label =
                "Normalization Collision Groups",
            value =
                model.normalizationCollisionGroupCount
        )
    }
}

@Composable
private fun NutritionConflictSection(
    model: NutritionKnowledgeConflictsUiModel
) {
    NutritionQualitySection(
        title =
            "Konflikte",
        status =
            model.status,
        statusLabel =
            model.statusLabel
    ) {
        NutritionQualityMetricRow(
            label =
                "Entry Conflict Rate",
            value =
                model.entryConflictPercentage,
            emphasized =
                true
        )

        NutritionQualityMetricRow(
            label =
                "Nutrient Conflict Rate",
            value =
                model.nutrientConflictPercentage,
            emphasized =
                true
        )

        NutritionQualityMetricRow(
            label =
                "Konflikt-Einträge",
            value =
                model.conflictEntryCount
        )

        NutritionQualityMetricRow(
            label =
                "Nährstoffkonflikte",
            value =
                model.nutrientConflictCount
        )

        NutritionQualityMetricRow(
            label =
                "Vergleichbare Einträge",
            value =
                model.comparableEntryCount
        )

        NutritionQualityMetricRow(
            label =
                "Konfliktfreie Einträge",
            value =
                model.conflictFreeEntryCount
        )
    }
}

@Composable
private fun NutritionConflictPolicySection(
    model: NutritionKnowledgeConflictPolicyUiModel
) {
    NutritionQualitySection(
        title =
            "Konflikt-Policy",
        status =
            model.status,
        statusLabel =
            model.statusLabel
    ) {
        NutritionQualityMetricRow(
            label =
                "Max. Entry Conflict Rate",
            value =
                model.maximumEntryConflictPercentage
        )

        NutritionQualityMetricRow(
            label =
                "Max. Nutrient Conflict Rate",
            value =
                model.maximumNutrientConflictPercentage
        )

        NutritionQualityMetricRow(
            label =
                "Extreme Konflikte",
            value =
                "${model.extremeConflictCount} / " +
                        model.maximumExtremeConflictCount
        )

        NutritionPolicyRuleRow(
            allowed =
                model.automaticCorrectionAllowed,
            label =
                model.automaticCorrectionLabel
        )

        NutritionPolicyRuleRow(
            allowed =
                model.automaticEntryRejectionAllowed,
            label =
                model.automaticEntryRejectionLabel
        )
    }
}

@Composable
private fun NutritionOffFreezeSection(
    model: NutritionKnowledgeOffFreezeUiModel
) {
    NutritionQualitySection(
        title =
            "OFF-Quelle",
        status =
            model.status,
        statusLabel =
            model.statusLabel
    ) {
        NutritionQualityMetricRow(
            label =
                "Quelle",
            value =
                model.source
        )

        NutritionQualityMetricRow(
            label =
                "Quellversion",
            value =
                model.sourceVersion
        )

        NutritionQualityMetricRow(
            label =
                "Aggregate",
            value =
                model.aggregateEntryCount
        )

        NutritionQualityMetricRow(
            label =
                "Snapshot-Größe",
            value =
                model.frozenFileSize
        )

        NutritionPolicyRuleRow(
            allowed =
                model.checksumVerified,
            label =
                model.checksumLabel,
            positiveWhenTrue =
                true
        )

        NutritionPolicyRuleRow(
            allowed =
                model.validationApproved,
            label =
                if (
                    model.validationApproved
                ) {
                    "Runtime-Validierung bestätigt"
                } else {
                    "Runtime-Validierung nicht bestätigt"
                },
            positiveWhenTrue =
                true
        )

        NutritionPolicyRuleRow(
            allowed =
                model.conflictPolicyApproved,
            label =
                if (
                    model.conflictPolicyApproved
                ) {
                    "Konflikt-Policy bestätigt"
                } else {
                    "Konflikt-Policy nicht bestätigt"
                },
            positiveWhenTrue =
                true
        )
    }
}

@Composable
private fun NutritionQualitySection(
    title: String,
    status: NutritionKnowledgeQualityUiStatus,
    statusLabel: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(
                8.dp
            )
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text =
                    title,
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight =
                    FontWeight.SemiBold,
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            Text(
                text =
                    statusLabel,
                style =
                    MaterialTheme.typography.labelMedium,
                color =
                    statusColor(
                        status
                    ),
                textAlign =
                    TextAlign.End
            )
        }

        content()
    }
}

@Composable
private fun NutritionQualityMetricRow(
    label: String,
    value: String,
    emphasized: Boolean = false,
    valueColor: Color =
        MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.Top
    ) {
        Text(
            text =
                label,
            style =
                if (emphasized) {
                    MaterialTheme.typography.bodyLarge
                } else {
                    MaterialTheme.typography.bodyMedium
                },
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant,
            modifier =
                Modifier
                    .weight(
                        1f
                    )
                    .padding(
                        end = 16.dp
                    )
        )

        Text(
            text =
                value,
            style =
                if (emphasized) {
                    MaterialTheme.typography.bodyLarge
                } else {
                    MaterialTheme.typography.bodyMedium
                },
            fontWeight =
                if (emphasized) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                },
            color =
                valueColor,
            textAlign =
                TextAlign.End
        )
    }
}

@Composable
private fun NutritionPolicyRuleRow(
    allowed: Boolean,
    label: String,
    positiveWhenTrue: Boolean = false
) {
    val successful =
        if (positiveWhenTrue) {
            allowed
        } else {
            !allowed
        }

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(
                8.dp
            ),
        verticalAlignment =
            Alignment.Top
    ) {
        Text(
            text =
                if (successful) {
                    "✓"
                } else {
                    "!"
                },
            style =
                MaterialTheme.typography.bodyMedium,
            fontWeight =
                FontWeight.Bold,
            color =
                if (successful) {
                    statusColor(
                        NutritionKnowledgeQualityUiStatus.APPROVED
                    )
                } else {
                    statusColor(
                        NutritionKnowledgeQualityUiStatus.REJECTED
                    )
                }
        )

        Text(
            text =
                label,
            style =
                MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant,
            modifier =
                Modifier.weight(
                    1f
                )
        )
    }
}

@Composable
private fun NutritionQualityStatusBadge(
    status: NutritionKnowledgeQualityUiStatus,
    label: String
) {
    val color =
        statusColor(
            status
        )

    Surface(
        color =
            color.copy(
                alpha = 0.14f
            ),
        contentColor =
            color,
        shape =
            RoundedCornerShape(
                999.dp
            )
    ) {
        Text(
            text =
                label,
            modifier =
                Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 6.dp
                ),
            style =
                MaterialTheme.typography.labelMedium,
            fontWeight =
                FontWeight.Bold
        )
    }
}

@Composable
private fun statusColor(
    status: NutritionKnowledgeQualityUiStatus
): Color =
    when (status) {
        NutritionKnowledgeQualityUiStatus.APPROVED ->
            BrandGreen

        NutritionKnowledgeQualityUiStatus.WARNING ->
            Color(
                0xFFF57C00
            )

        NutritionKnowledgeQualityUiStatus.REJECTED ->
            MaterialTheme.colorScheme.error
    }