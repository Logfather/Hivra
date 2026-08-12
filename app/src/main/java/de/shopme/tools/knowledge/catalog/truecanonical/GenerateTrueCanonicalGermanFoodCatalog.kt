package de.shopme.tools.knowledge.catalog.truecanonical

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.canonical.rebuild.model.CanonicalFoodIdentity
import java.io.File

data class TrueCanonicalGermanFoodCatalogGenerationReport(
    val version: Int,
    val identityCount: Int,
    val categoryCount: Int,
    val variantCount: Int,
    val sourceVariantCount: Int,
    val outputFile: String
)

class GenerateTrueCanonicalGermanFoodCatalog {

    fun generate(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default()
    ): TrueCanonicalGermanFoodCatalogGenerationReport {

        val source =
            TrueCanonicalGermanFoodIdentityRegistry
                .identities

        require(
            source.isNotEmpty()
        ) {
            "True canonical identity registry is empty."
        }

        val output =
            source
                .map { identity ->

                    CanonicalFoodIdentity(
                        itemname =
                            identity.itemname,

                        normalized =
                            identity.normalized,

                        category =
                            identity.category,

                        variants =
                            emptyList(),

                        sourceVariants =
                            emptyList()
                    )
                }
                .sortedBy {
                    it.normalized
                }

        TrueCanonicalGermanFoodCatalogValidator()
            .validate(
                output
            )

        val outputDirectory =
            paths.projectRoot.resolve(
                "build/knowledge/catalog/true-canonical"
            )

        require(
            outputDirectory.exists() ||
                    outputDirectory.mkdirs()
        ) {
            "Unable to create output directory: " +
                    outputDirectory.absolutePath
        }

        val outputFile =
            outputDirectory.resolve(
                "canonical-food-catalog.true-canonical.json"
            )

        writeJson(
            value =
                output,

            file =
                outputFile
        )

        val report =
            TrueCanonicalGermanFoodCatalogGenerationReport(
                version =
                    1,

                identityCount =
                    output.size,

                categoryCount =
                    output
                        .map {
                            it.category
                        }
                        .distinct()
                        .size,

                variantCount =
                    output.sumOf {
                        it.variants.size
                    },

                sourceVariantCount =
                    output.sumOf {
                        it.sourceVariants.size
                    },

                outputFile =
                    outputFile.path
            )

        writeJson(
            value =
                report,

            file =
                paths.reportsRoot.resolve(
                    "true-canonical-german-food-catalog-generation.json"
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