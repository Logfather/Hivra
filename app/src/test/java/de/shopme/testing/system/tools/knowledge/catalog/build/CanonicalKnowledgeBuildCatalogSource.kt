package de.shopme.testing.system.tools.knowledge.catalog.build

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File
import java.security.MessageDigest

data class CanonicalKnowledgeBuildCatalog(
    val sourceFile: File,
    val entries: List<JsonObject>,
    val sha256: String
)

class CanonicalKnowledgeBuildCatalogSource(
    private val projectRoot: File
) {

    fun load(): CanonicalKnowledgeBuildCatalog {

        val paths =
            KnowledgeBuildPaths.fromProjectRoot(
                projectRoot
            )

        val sourceFile =
            paths.canonicalFoodCatalog

        require(sourceFile.isFile) {
            "Productive canonical catalog not found: " +
                    sourceFile.absolutePath
        }

        val payload =
            sourceFile.readBytes()

        val entries =
            JsonParser
                .parseString(
                    payload.toString(
                        Charsets.UTF_8
                    )
                )
                .asJsonArray
                .mapNotNull { element ->
                    element
                        .takeIf {
                            it.isJsonObject
                        }
                        ?.asJsonObject
                }

        require(
            entries.size ==
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_ENTRY_COUNT
        ) {
            "Productive canonical catalog arithmetic changed. " +
                    "Expected " +
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_ENTRY_COUNT +
                    " entries, found ${entries.size}."
        }

        val normalizedKeys =
            entries.map { entry ->
                requireString(
                    json = entry,
                    key = "normalized"
                )
            }

        require(
            normalizedKeys.distinct().size ==
                    entries.size
        ) {
            "Productive canonical catalog contains duplicate " +
                    "normalized keys."
        }

        val canonicalNames =
            entries.map { entry ->
                requireString(
                    json = entry,
                    key = "itemname"
                )
            }

        require(
            canonicalNames.distinct().size ==
                    entries.size
        ) {
            "Productive canonical catalog contains duplicate " +
                    "canonical item names."
        }

        val sha256 =
            MessageDigest
                .getInstance("SHA-256")
                .digest(payload)
                .joinToString("") { byte ->
                    "%02x".format(
                        byte.toInt() and 0xff
                    )
                }

        require(
            sha256 ==
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_SHA256
        ) {
            "Unexpected productive canonical catalog SHA-256. " +
                    "Expected " +
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_SHA256 +
                    ", found $sha256."
        }

        return CanonicalKnowledgeBuildCatalog(
            sourceFile =
                sourceFile,
            entries =
                entries,
            sha256 =
                sha256
        )
    }

    private fun requireString(
        json: JsonObject,
        key: String
    ): String =
        requireNotNull(
            json
                .get(key)
                ?.takeIf {
                    it.isJsonPrimitive
                }
                ?.asString
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
        ) {
            "Missing or blank '$key' in productive catalog."
        }

    companion object {

        const val EXPECTED_ENTRY_COUNT =
            KnowledgeBuildPaths.CANONICAL_CATALOG_ENTRY_COUNT

        const val EXPECTED_SHA256 =
            KnowledgeBuildPaths.CANONICAL_CATALOG_SHA256
    }
}