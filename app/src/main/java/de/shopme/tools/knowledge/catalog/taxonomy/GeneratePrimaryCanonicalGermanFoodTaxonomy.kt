package de.shopme.tools.knowledge.catalog.taxonomy

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File

data class PrimaryCanonicalGermanFoodTaxonomyGenerationReport(
    val version: Int,
    val departmentCount: Int,
    val groupCount: Int,
    val outputFile: String
)

class GeneratePrimaryCanonicalGermanFoodTaxonomy {

    fun generate(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default()
    ): PrimaryCanonicalGermanFoodTaxonomyGenerationReport {

        val taxonomy =
            PrimaryCanonicalGermanFoodTaxonomyRegistry
                .taxonomy

        PrimaryCanonicalGermanFoodTaxonomyValidator()
            .validate(
                taxonomy
            )

        val outputDirectory =
            paths.projectRoot.resolve(
                "build/knowledge/catalog/taxonomy"
            )

        require(
            outputDirectory.exists() ||
                    outputDirectory.mkdirs()
        ) {
            "Unable to create taxonomy output directory: " +
                    outputDirectory.absolutePath
        }

        val outputFile =
            outputDirectory.resolve(
                "primary-canonical-german-food-taxonomy.json"
            )

        writeJson(
            value =
                taxonomy,
            file =
                outputFile
        )

        val report =
            PrimaryCanonicalGermanFoodTaxonomyGenerationReport(
                version =
                    1,

                departmentCount =
                    taxonomy.departments.size,

                groupCount =
                    taxonomy.departments
                        .sumOf {
                            it.groups.size
                        },

                outputFile =
                    outputFile.path
            )

        writeJson(
            value =
                report,

            file =
                paths.reportsRoot.resolve(
                    "primary-canonical-german-food-taxonomy-generation.json"
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