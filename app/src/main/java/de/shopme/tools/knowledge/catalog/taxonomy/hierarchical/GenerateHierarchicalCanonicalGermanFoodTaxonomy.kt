package de.shopme.tools.knowledge.catalog.taxonomy.hierarchical

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File

data class HierarchicalCanonicalGermanFoodTaxonomyGenerationReport(
    val version: Int,
    val departmentCount: Int,
    val nodeCount: Int,
    val leafCount: Int,
    val maximumDepth: Int,
    val outputFile: String,
    val validationFile: String
)

class GenerateHierarchicalCanonicalGermanFoodTaxonomy {

    fun generate(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default()
    ): HierarchicalCanonicalGermanFoodTaxonomyGenerationReport {

        val taxonomy =
            HierarchicalCanonicalGermanFoodTaxonomyRegistry
                .taxonomy

        val validation =
            HierarchicalCanonicalGermanFoodTaxonomyValidator()
                .validate(
                    taxonomy
                )

        require(
            validation.valid
        ) {
            "Hierarchical canonical German food taxonomy is invalid: " +
                    validation
        }

        /*
         * Zusätzlich gesamten Baum indexieren.
         * Dadurch werden ungültige/mehrdeutige Pfade
         * bereits beim Build sichtbar.
         */
        HierarchicalFoodTaxonomyIndex(
            taxonomy
        )

        val outputDirectory =
            paths.projectRoot.resolve(
                "build/knowledge/catalog/taxonomy-hierarchical"
            )

        require(
            outputDirectory.exists() ||
                    outputDirectory.mkdirs()
        )

        val outputFile =
            outputDirectory.resolve(
                "primary-canonical-german-food-taxonomy.hierarchical.json"
            )

        val validationFile =
            paths.reportsRoot.resolve(
                "hierarchical-canonical-german-food-taxonomy-validation.json"
            )

        writeJson(
            value =
                taxonomy,
            file =
                outputFile
        )

        writeJson(
            value =
                validation,
            file =
                validationFile
        )

        val report =
            HierarchicalCanonicalGermanFoodTaxonomyGenerationReport(
                version =
                    1,

                departmentCount =
                    validation.departmentCount,

                nodeCount =
                    validation.nodeCount,

                leafCount =
                    validation.leafCount,

                maximumDepth =
                    validation.maximumDepth,

                outputFile =
                    outputFile.path,

                validationFile =
                    validationFile.path
            )

        writeJson(
            value =
                report,

            file =
                paths.reportsRoot.resolve(
                    "hierarchical-canonical-german-food-taxonomy-generation.json"
                )
        )

        return report
    }

    private fun writeJson(
        value: Any,
        file: File
    ) {

        val parent =
            requireNotNull(
                file.parentFile
            )

        require(
            parent.exists() ||
                    parent.mkdirs()
        )

        file.writeText(
            gson.toJson(
                value
            ) + "\n"
        )
    }

    companion object {

        private val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
    }
}