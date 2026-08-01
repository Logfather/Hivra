package de.shopme.tools.knowledge.off.nutrition.reference.freeze

import java.io.File

data class FreezeOFFNutritionSourceRequest(
    val sourceAggregateFile: File,
    val resultingNutritionValidationReportFile: File,
    val nutritionConflictPolicyFile: File,
    val frozenAggregateFile: File,
    val snapshotFile: File
) {

    init {
        require(
            sourceAggregateFile !=
                    frozenAggregateFile
        ) {
            "Source and frozen OFF Nutrition aggregate files must differ."
        }

        require(
            frozenAggregateFile !=
                    snapshotFile
        ) {
            "Frozen aggregate and snapshot files must differ."
        }
    }
}