package de.shopme.tools.knowledge.build

import java.io.File

/**
 * Single path authority for the productive Knowledge Build.
 *
 * Persistent inputs live below data/.
 * Reproducible build outputs live below build/knowledge/.
 * Published Android assets are not Knowledge Build inputs.
 */
class KnowledgeBuildPaths private constructor(
    val projectRoot: File
) {

    // -------------------------------------------------------------------------
    // Persistent input state
    // -------------------------------------------------------------------------

    val dataRoot: File =
        projectRoot.resolve("data")

    val catalogRoot: File =
        dataRoot.resolve("catalog")

    val catalogReleasesRoot: File =
        catalogRoot.resolve("releases")

    val knowledgeRoot: File =
        dataRoot.resolve("knowledge")

    val canonicalCatalogMasterRoot: File =
        knowledgeRoot
            .resolve("catalog")
            .resolve("master")
            .resolve("product-only")

    /**
     * The one and only productive canonical Product-Only catalog authority.
     */
    val canonicalFoodCatalog: File =
        canonicalCatalogMasterRoot.resolve(
            "canonical-food-catalog.product-only.master.json"
        )

    val sourcesRoot: File =
        dataRoot.resolve("sources")

    val agribalyseSourceRoot: File =
        sourcesRoot.resolve("agribalyse")

    val ciqualSourceRoot: File =
        sourcesRoot.resolve("ciqual/Ciqual")

    val foodDataCentralSourceRoot: File =
        sourcesRoot.resolve("fdc")

    val openFoodFactsSourceRoot: File =
        sourcesRoot.resolve("openfoodfacts")

    val openFoodFactsProducts: File =
        openFoodFactsSourceRoot.resolve(
            "openfoodfacts-products.jsonl.gz"
        )

    val ciqualFoods: File =
        ciqualSourceRoot.resolve(
            "alim_2025_11_03.xml"
        )

    val ciqualFoodGroups: File =
        ciqualSourceRoot.resolve(
            "alim_grp_2025_11_03.xml"
        )

    val ciqualComposition: File =
        ciqualSourceRoot.resolve(
            "compo_2025_11_03.xml"
        )

    val ciqualConstituents: File =
        ciqualSourceRoot.resolve(
            "const_2025_11_03.xml"
        )

    val ciqualSources: File =
        ciqualSourceRoot.resolve(
            "sources_2025_11_03.xml"
        )

    val referencesRoot: File =
        dataRoot.resolve("references")

    val offReferencesRoot: File =
        referencesRoot.resolve("off")

    val offNutritionReferenceAggregates: File =
        offReferencesRoot.resolve(
            "off-nutrition-reference-aggregates.json"
        )

    val offNutritionAcceptedReferences: File =
        offReferencesRoot.resolve(
            "off-nutrition-references.accepted.jsonl.gz"
        )

    val offNutritionAcceptedManifest: File =
        offReferencesRoot.resolve(
            "off-nutrition-references.accepted.manifest.json"
        )

    val frozenRoot: File =
        dataRoot.resolve("frozen")

    val frozenOffNutritionRoot: File =
        frozenRoot.resolve("off/nutrition")

    val frozenOffNutritionReferenceAggregates: File =
        frozenOffNutritionRoot.resolve(
            "off-nutrition-reference-aggregates.json"
        )

    val frozenOffNutritionSourceSnapshot: File =
        frozenOffNutritionRoot.resolve(
            "off-nutrition-source-snapshot.json"
        )

    val policiesRoot: File =
        dataRoot.resolve("policies")

    val offNutritionReferenceQualityPolicy: File =
        policiesRoot.resolve(
            "off-nutrition-reference-quality-policy.json"
        )

    val resultingNutritionConflictPolicy: File =
        policiesRoot.resolve(
            "resulting-nutrition-conflict-policy.json"
        )

    val modelsRoot: File =
        dataRoot.resolve("models")

    val nutritionLocalMatcherModel: File =
        modelsRoot.resolve(
            "nutrition.local-matcher-model.json"
        )

    val trainingRoot: File =
        dataRoot.resolve("training")

    val nutritionMatcherTrainingDataset: File =
        trainingRoot.resolve(
            "nutrition.matcher-training-dataset.json"
        )

    // -------------------------------------------------------------------------
    // Reproducible build output
    // -------------------------------------------------------------------------

    val knowledgeBuildRoot: File =
        projectRoot.resolve("build/knowledge")

    val legacyRoot: File =
        knowledgeBuildRoot.resolve("legacy")

    val legacyServerRoot: File =
        legacyRoot.resolve("server")

    val diagnosticsRoot: File =
        knowledgeBuildRoot.resolve("diagnostics")

    val intermediateRoot: File =
        knowledgeBuildRoot.resolve("intermediate")

    val mappingsRoot: File =
        knowledgeBuildRoot.resolve("mappings")

    val reportsRoot: File =
        knowledgeBuildRoot.resolve("reports")

    val runtimeRoot: File =
        knowledgeBuildRoot.resolve("runtime")

    val serverRoot: File =
        knowledgeBuildRoot.resolve("server")

    // -------------------------------------------------------------------------
    // Publication targets
    // -------------------------------------------------------------------------

    val appAssetsRoot: File =
        projectRoot.resolve(
            "app/src/main/assets"
        )

    val publishedCatalog: File =
        appAssetsRoot.resolve(
            "catalog/catalog.json"
        )

    val publishedRuntimeRoot: File =
        appAssetsRoot.resolve(
            "knowledge/runtime"
        )

    val validatedMappingsDirectory: File =
        mappingsRoot.resolve("validated")

    val catalogServerMappings: File =
        mappingsRoot.resolve("catalog-server.mappings.json")

    val nutritionExactMappings: File =
        mappingsRoot.resolve("nutrition.mappings.json")

    val nutritionMatchRequests: File =
        intermediateRoot.resolve(
            "match-requests/nutrition.match-requests.json"
        )

    val nutritionMatchDecisions: File =
        intermediateRoot.resolve(
            "match-decisions/nutrition.match-decisions.json"
        )

    val nutritionMatchErrors: File =
        intermediateRoot.resolve(
            "match-decisions/nutrition.match-errors.json"
        )

    val nutritionValidationReport: File =
        reportsRoot.resolve(
            "nutrition.mapping-validation-report.json"
        )

    val nutritionRejectedCandidateQualityReport: File =
        reportsRoot.resolve(
            "nutrition.rejected-candidate-quality.json"
        )

    val nutritionLowConfidenceValidationReport: File =
        reportsRoot.resolve(
            "nutrition.low-confidence-validation.json"
        )

    val nutritionMatchDiagnostics: File =
        diagnosticsRoot.resolve(
            "nutrition.match-diagnostics.json"
        )

    val catalogServerMappingConflictReport: File =
        reportsRoot.resolve(
            "catalog-server-mapping-conflicts.json"
        )

    val catalogServerMappingMergeReport: File =
        reportsRoot.resolve(
            "catalog-server-mapping-merge-report.json"
        )

    val catalogServerMappingCoverageReport: File =
        reportsRoot.resolve(
            "catalog-server-mapping-artifact-coverage.json"
        )

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    fun serverArtifact(
        fileName: String
    ): File =
        serverRoot.resolve(fileName)

    fun runtimeArtifact(
        fileName: String
    ): File =
        runtimeRoot.resolve(fileName)

    fun reportArtifact(
        fileName: String
    ): File =
        reportsRoot.resolve(fileName)

    fun diagnosticArtifact(
        fileName: String
    ): File =
        diagnosticsRoot.resolve(fileName)

    fun mappingArtifact(
        fileName: String
    ): File =
        mappingsRoot.resolve(fileName)

    fun intermediateArtifact(
        fileName: String
    ): File =
        intermediateRoot.resolve(fileName)

    fun ensureBuildDirectories() {
        listOf(
            knowledgeBuildRoot,
            diagnosticsRoot,
            intermediateRoot,
            mappingsRoot,
            reportsRoot,
            runtimeRoot,
            serverRoot,
            legacyRoot,
            legacyServerRoot
        ).forEach { directory ->
            check(
                directory.exists() ||
                        directory.mkdirs()
            ) {
                "Could not create Knowledge Build directory: " +
                        directory.absolutePath
            }

            check(directory.isDirectory) {
                "Knowledge Build path is not a directory: " +
                        directory.absolutePath
            }
        }
    }

    fun validateCanonicalCatalogAuthority() {
        require(canonicalFoodCatalog.isFile) {
            "Canonical food catalog missing: " +
                    canonicalFoodCatalog.absolutePath
        }

        require(canonicalFoodCatalog.canRead()) {
            "Canonical food catalog is not readable: " +
                    canonicalFoodCatalog.absolutePath
        }

        require(canonicalFoodCatalog.length() > 0L) {
            "Canonical food catalog is empty: " +
                    canonicalFoodCatalog.absolutePath
        }
    }

    companion object {

        const val CANONICAL_CATALOG_SHA256 =
            "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f"

        const val CANONICAL_CATALOG_ENTRY_COUNT =
            1384


        fun default(): KnowledgeBuildPaths =
            fromProjectRoot(
                resolveProjectRoot()
            )

        fun fromProjectRoot(
            projectRoot: File
        ): KnowledgeBuildPaths {

            val canonicalRoot =
                projectRoot.canonicalFile

            require(
                canonicalRoot.resolve("app").isDirectory
            ) {
                "ShopMe app directory missing below project root: " +
                        canonicalRoot.absolutePath
            }

            require(
                canonicalRoot.resolve("gradlew").isFile
            ) {
                "ShopMe Gradle wrapper missing below project root: " +
                        canonicalRoot.absolutePath
            }

            require(
                canonicalRoot.resolve("data").isDirectory
            ) {
                "ShopMe data directory missing below project root: " +
                        canonicalRoot.absolutePath
            }

            return KnowledgeBuildPaths(
                projectRoot = canonicalRoot
            )
        }

        private fun resolveProjectRoot(): File {

            val userDir =
                requireNotNull(
                    System.getProperty("user.dir")
                ) {
                    "System property user.dir is not available."
                }

            val startDirectory =
                File(userDir).canonicalFile

            var current =
                startDirectory

            while (true) {

                val hasGradleWrapper =
                    current
                        .resolve("gradlew")
                        .isFile

                val hasApp =
                    current
                        .resolve("app")
                        .isDirectory

                val hasData =
                    current
                        .resolve("data")
                        .isDirectory

                if (
                    hasGradleWrapper &&
                    hasApp &&
                    hasData
                ) {
                    return current
                }

                current =
                    current.parentFile
                        ?: error(
                            "Could not resolve ShopMe project root " +
                                    "from ${startDirectory.absolutePath}."
                        )
            }
        }
    }
}