package de.shopme.testing.system.tools.knowledge.catalog

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.CatalogNutritionReferenceAliasMapper
import de.shopme.tools.knowledge.catalog.CatalogNutritionReferenceNormalizer
import de.shopme.tools.knowledge.catalog.report.CatalogNutritionNormalizationReportBuilder
import de.shopme.tools.knowledge.catalog.report.CatalogNutritionNormalizationReportPrinter
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CatalogNutritionReferenceNormalizerTest {

    private fun resolveCanonicalCatalogFile(): File {

        val catalogFile =
            KnowledgeBuildPaths
                .default()
                .canonicalFoodCatalog

        require(catalogFile.isFile) {
            "Canonical food catalog not found: " +
                    catalogFile.absolutePath
        }

        return catalogFile
    }

    @Test
    fun normalizeCatalog() {

        val inputFile =
            resolveCanonicalCatalogFile()

        val temporaryDirectory =
            Files.createTempDirectory(
                "catalog-nutrition-normalization-"
            )
                .toFile()

        val outputFile =
            File(
                temporaryDirectory,
                "normalized-canonical-food-catalog.json"
            )

        try {
            val normalizer =
                CatalogNutritionReferenceNormalizer(
                    inputFile =
                        inputFile,
                    outputFile =
                        outputFile,
                    aliasMapper =
                        CatalogNutritionReferenceAliasMapper()
                )

            normalizer.normalize()

            require(outputFile.isFile) {
                "Normalized catalog was not created: " +
                        outputFile.absolutePath
            }

            require(outputFile.length() > 0L) {
                "Normalized catalog is empty: " +
                        outputFile.absolutePath
            }

            val normalizedContent =
                outputFile.readText(
                    Charsets.UTF_8
                )

            val report =
                CatalogNutritionNormalizationReportBuilder()
                    .build(
                        normalizedContent
                    )

            CatalogNutritionNormalizationReportPrinter()
                .print(
                    report
                )

        } finally {
            if (temporaryDirectory.exists()) {
                temporaryDirectory
                    .walkBottomUp()
                    .forEach { file ->
                        require(
                            file.delete()
                        ) {
                            "Could not delete temporary test path: " +
                                    file.absolutePath
                        }
                    }
            }
        }
    }
}