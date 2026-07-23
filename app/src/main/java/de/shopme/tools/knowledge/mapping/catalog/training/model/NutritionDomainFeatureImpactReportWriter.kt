package de.shopme.tools.knowledge.mapping.catalog.training.model

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class NutritionDomainFeatureImpactReportWriter(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create(),
) {

    fun write(
        report: NutritionDomainFeatureImpactReport,
        outputFile: File,
    ) {
        validate(
            report =
                report,
        )

        val parentDirectory =
            requireNotNull(
                outputFile.parentFile,
            ) {
                "Nutrition Domain-Mismatch impact report output file must " +
                        "have a parent directory."
            }

        require(
            parentDirectory.exists() ||
                    parentDirectory.mkdirs(),
        ) {
            "Could not create Nutrition Domain-Mismatch impact report " +
                    "directory: ${parentDirectory.absolutePath}"
        }

        val json =
            gson.toJson(
                report,
            ) + "\n"

        val temporaryFile =
            File(
                parentDirectory,
                outputFile.name + ".tmp",
            )

        temporaryFile.writeText(
            text =
                json,
            charset =
                Charsets.UTF_8,
        )

        require(
            temporaryFile.renameTo(
                outputFile,
            ),
        ) {
            temporaryFile.delete()

            "Could not atomically replace Nutrition Domain-Mismatch impact " +
                    "report: ${outputFile.absolutePath}"
        }
    }

    private fun validate(
        report: NutritionDomainFeatureImpactReport,
    ) {
        require(
            report.version ==
                    NutritionDomainFeatureImpactAnalyzer.REPORT_VERSION,
        ) {
            "Unsupported Nutrition Domain-Mismatch impact report version: " +
                    report.version
        }

        require(
            report.sourceComparisonReportVersion > 0,
        ) {
            "Source comparison report version must be positive."
        }

        require(
            report.domainFeatureCount ==
                    report.impacts.size,
        ) {
            "Nutrition Domain-Mismatch impact report feature count differs " +
                    "from impact count."
        }

        require(
            report.harmfulCount +
                    report.neutralCount +
                    report.beneficialCount ==
                    report.domainFeatureCount,
        ) {
            "Nutrition Domain-Mismatch impact report classification counts " +
                    "do not cover all features."
        }

        require(
            report.harmfulCount ==
                    report.harmfulFeatureNames.size,
        ) {
            "Harmful feature count differs from harmful feature names."
        }

        require(
            report.neutralCount ==
                    report.neutralFeatureNames.size,
        ) {
            "Neutral feature count differs from neutral feature names."
        }

        require(
            report.beneficialCount ==
                    report.beneficialFeatureNames.size,
        ) {
            "Beneficial feature count differs from beneficial feature names."
        }

        require(
            report.impacts.map {
                it.featureName
            }.distinct().size ==
                    report.impacts.size,
        ) {
            "Nutrition Domain-Mismatch impact report contains duplicate " +
                    "features."
        }

        require(
            report.impacts.all { impact ->
                impact.featureName.isNotBlank() &&
                        impact.deltaPrecision.isFinite() &&
                        impact.deltaRecall.isFinite() &&
                        impact.deltaF1.isFinite() &&
                        impact.deltaBalancedAccuracy.isFinite()
            },
        ) {
            "Nutrition Domain-Mismatch impact report contains invalid values."
        }
    }
}