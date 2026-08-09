package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution

import com.google.gson.JsonObject
import java.text.Normalizer

data class PostFamilyRepairIdentityResolutionResult(
    val inputEntryCount: Int,
    val collisionGroupCount: Int,
    val removedEntryCount: Int,
    val outputEntryCount: Int,
    val removedNormalizedKeys: List<String>,
    val entries: List<JsonObject>
)

class PostFamilyRepairIdentityResolver {

    fun resolve(
        catalog: List<JsonObject>
    ): PostFamilyRepairIdentityResolutionResult {

        val collisionGroups =
            catalog
                .groupBy { entry ->
                    canonicalNameKey(
                        requireString(
                            entry,
                            "itemname"
                        )
                    )
                }
                .filter { (_, entries) ->
                    entries.size > 1
                }

        val removedNormalizedKeys =
            collisionGroups
                .values
                .flatMap { entries ->

                    val winner =
                        selectWinner(
                            entries
                        )

                    entries
                        .filterNot {
                            it === winner
                        }
                        .map {
                            requireString(
                                it,
                                "normalized"
                            )
                        }
                }
                .toSet()

        val output =
            catalog
                .filter { entry ->
                    requireString(
                        entry,
                        "normalized"
                    ) !in removedNormalizedKeys
                }
                .sortedWith(
                    compareBy<JsonObject>(
                        {
                            requireString(
                                it,
                                "category"
                            )
                        },
                        {
                            requireString(
                                it,
                                "normalized"
                            )
                        }
                    )
                )

        require(
            output.size ==
                    catalog.size -
                    removedNormalizedKeys.size
        ) {
            "Post-family identity resolution arithmetic mismatch."
        }

        return PostFamilyRepairIdentityResolutionResult(
            inputEntryCount =
                catalog.size,

            collisionGroupCount =
                collisionGroups.size,

            removedEntryCount =
                removedNormalizedKeys.size,

            outputEntryCount =
                output.size,

            removedNormalizedKeys =
                removedNormalizedKeys.sorted(),

            entries =
                output
        )
    }

    private fun selectWinner(
        entries: List<JsonObject>
    ): JsonObject {

        /*
         * Kanonische Regel:
         *
         * Wenn der technische normalized key direkt der
         * kanonischen deutschen Produktidentität entspricht,
         * gewinnt dieser bestehende Eintrag.
         *
         * Apfelsaft / apfelsaft
         *   >
         * Apfelsaft / fruit-juice-apple
         */

        val directMatches =
            entries.filter { entry ->

                val itemName =
                    requireString(
                        entry,
                        "itemname"
                    )

                val normalized =
                    requireString(
                        entry,
                        "normalized"
                    )

                normalized ==
                        canonicalNormalizedKey(
                            itemName
                        )
            }

        if (directMatches.size == 1) {
            return directMatches.single()
        }

        /*
         * Ohne eindeutigen direkten Match entscheiden wir
         * absichtlich nicht stillschweigend.
         */
        error(
            buildString {
                append(
                    "Cannot deterministically resolve post-family " +
                            "identity collision: "
                )

                append(
                    entries.joinToString {
                        "${requireString(it, "itemname")} / " +
                                requireString(it, "normalized")
                    }
                )
            }
        )
    }

    private fun canonicalNormalizedKey(
        value: String
    ): String {

        val decomposed =
            Normalizer.normalize(
                value
                    .trim()
                    .lowercase(),
                Normalizer.Form.NFD
            )

        return decomposed
            .replace(
                COMBINING_MARKS_REGEX,
                ""
            )
            .replace(
                "ß",
                "ss"
            )
            .replace(
                NON_ALPHANUMERIC_REGEX,
                "-"
            )
            .replace(
                MULTIPLE_HYPHENS_REGEX,
                "-"
            )
            .trim('-')
    }

    private fun canonicalNameKey(
        value: String
    ): String =
        canonicalNormalizedKey(value)
            .replace(
                "-",
                ""
            )

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
            "Missing or blank '$key' in catalog entry: $json"
        }

    private companion object {

        val COMBINING_MARKS_REGEX =
            Regex("\\p{M}+")

        val NON_ALPHANUMERIC_REGEX =
            Regex("[^a-z0-9]+")

        val MULTIPLE_HYPHENS_REGEX =
            Regex("-+")
    }
}