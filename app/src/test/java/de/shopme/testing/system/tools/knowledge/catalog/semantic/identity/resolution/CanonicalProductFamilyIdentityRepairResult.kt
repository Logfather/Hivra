package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution

import com.google.gson.JsonObject

data class CanonicalProductFamilyIdentityRepairResult(
    val inputEntryCount: Int,
    val outputEntryCount: Int,
    val changedEntryCount: Int,
    val entries: List<JsonObject>
)

class CanonicalProductFamilyIdentityRepairer {

    fun repair(
        catalog: List<JsonObject>
    ): CanonicalProductFamilyIdentityRepairResult {

        var changedEntryCount =
            0

        val repaired =
            catalog
                .map { source ->

                    val itemName =
                        requireString(
                            source,
                            "itemname"
                        )

                    val parts =
                        itemName
                            .split(VARIANT_SEPARATOR)
                            .map(String::trim)
                            .filter(String::isNotBlank)

                    val sourceFamily =
                        parts.first()

                    val canonicalFamily =
                        CanonicalProductFamilyIdentityPolicy
                            .canonicalFamilyName(
                                sourceFamily
                            )

                    val variants =
                        parts.drop(1)

                    val compoundIdentity =
                        if (variants.size == 1) {
                            CanonicalGermanCompoundIdentityPolicy
                                .canonicalItemName(
                                    family = canonicalFamily,
                                    variant = variants.single()
                                )
                        } else {
                            null
                        }

                    val repairedName =
                        compoundIdentity
                            ?: buildList {
                                add(canonicalFamily)
                                addAll(variants)
                            }
                                .joinToString(
                                    VARIANT_SEPARATOR
                                )

                    /*
                     * Keine Änderung an der kanonischen Identität:
                     * unveränderte Kopie zurückgeben und NICHT zählen.
                     */
                    if (
                        repairedName ==
                        itemName
                    ) {
                        return@map source.deepCopy()
                    }

                    /*
                     * Exakt eine Identity-Transformation pro Entry.
                     */
                    changedEntryCount++

                    source
                        .deepCopy()
                        .apply {
                            addProperty(
                                "itemname",
                                repairedName
                            )
                        }
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

        return CanonicalProductFamilyIdentityRepairResult(
            inputEntryCount =
                catalog.size,
            outputEntryCount =
                repaired.size,
            changedEntryCount =
                changedEntryCount,
            entries =
                repaired
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
            "Missing or blank '$key' in catalog entry: $json"
        }

    private companion object {

        const val VARIANT_SEPARATOR =
            " – "
    }
}