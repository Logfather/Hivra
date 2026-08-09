package de.shopme.testing.system.tools.knowledge.catalog.finalization.v2

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File

class CanonicalFoodCatalogV2FinalizationWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        projectRoot: File,
        result: CanonicalFoodCatalogV2FinalizationResult,
        catalogPayload: String
    ): CanonicalFoodCatalogV2ReleasePaths {

        require(result.valid) {
            "Refusing to publish invalid canonical food catalog v2."
        }

        val paths =
            KnowledgeBuildPaths.fromProjectRoot(
                projectRoot
            )

        val releaseDirectory =
            paths.catalogReleasesRoot

        require(
            releaseDirectory.exists() ||
                    releaseDirectory.mkdirs()
        ) {
            "Could not create catalog release directory: " +
                    releaseDirectory.absolutePath
        }

        require(releaseDirectory.isDirectory) {
            "Catalog release path is not a directory: " +
                    releaseDirectory.absolutePath
        }

        val productiveCatalogFile =
            paths.canonicalFoodCatalog

        val immutableSnapshotFile =
            releaseDirectory.resolve(
                result.finalizationId + ".json"
            )

        val finalizationReportFile =
            releaseDirectory.resolve(
                "canonical-food-catalog-finalization-v2.json"
            )

        /*
         * Immutable release snapshot.
         *
         * Falls derselbe Snapshot bereits existiert, müssen
         * die Bytes identisch sein.
         */
        if (
            immutableSnapshotFile.exists()
        ) {
            require(
                immutableSnapshotFile.readText() ==
                        catalogPayload
            ) {
                "Immutable v2 release snapshot already exists " +
                        "with different content: " +
                        immutableSnapshotFile.absolutePath
            }
        } else {
            immutableSnapshotFile.writeText(
                catalogPayload
            )
        }

        /*
         * Finalization report.
         */
        finalizationReportFile.writeText(
            gson.toJson(result) + "\n"
        )

        /*
         * Produktive Catalog-Authority.
         *
         * Ab diesem Punkt muss
         * data/catalog/canonical-food-catalog.json
         * byte-identisch mit dem immutable Release-Snapshot sein.
         */
        productiveCatalogFile
            .parentFile
            ?.let { parentDirectory ->
                require(
                    parentDirectory.exists() ||
                            parentDirectory.mkdirs()
                ) {
                    "Could not create catalog directory: " +
                            parentDirectory.absolutePath
                }
            }

        productiveCatalogFile.writeText(
            catalogPayload
        )

        require(
            productiveCatalogFile.readBytes()
                .contentEquals(
                    immutableSnapshotFile.readBytes()
                )
        ) {
            "Productive canonical food catalog differs from " +
                    "immutable release snapshot."
        }

        return CanonicalFoodCatalogV2ReleasePaths(
            finalCatalogPath =
                productiveCatalogFile
                    .relativeTo(projectRoot)
                    .path,

            immutableSnapshotPath =
                immutableSnapshotFile
                    .relativeTo(projectRoot)
                    .path,

            finalizationReportPath =
                finalizationReportFile
                    .relativeTo(projectRoot)
                    .path,

            productiveCatalogPath =
                productiveCatalogFile
                    .relativeTo(projectRoot)
                    .path
        )
    }
}