package de.shopme.testing.system.tools.knowledge.catalog.finalization.v2

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.Normalizer

class CanonicalFoodCatalogV2Finalizer {

    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun finalize(
        catalog: List<JsonObject>,
        linguisticRepairReport: JsonObject
    ): Pair<
            CanonicalFoodCatalogV2FinalizationResult,
            String
            > {

        require(catalog.isNotEmpty()) {
            "Cannot finalize empty canonical food catalog."
        }

        val normalizedKeys =
            catalog.map {
                requireString(
                    it,
                    "normalized"
                )
            }

        val canonicalNames =
            catalog.map {
                requireString(
                    it,
                    "itemname"
                )
            }

        val categories =
            catalog.map {
                requireString(
                    it,
                    "category"
                )
            }

        val uniqueNormalizedKeyCount =
            normalizedKeys
                .distinct()
                .size

        val uniqueCanonicalNameCount =
            canonicalNames
                .map(::canonicalNameKey)
                .distinct()
                .size

        val deterministicOrder =
            isDeterministicallyOrdered(
                catalog
            )

        val linguisticRepairValid =
            requireBoolean(
                linguisticRepairReport,
                "valid"
            )

        val familyPluralFallbackCount =
            linguisticRepairReport
                .getAsJsonObject("stats")
                ?.get("familyPluralFallbackCount")
                ?.asInt
                ?: error(
                    "Missing linguistic repair " +
                            "stats.familyPluralFallbackCount."
                )

        val payload =
            serializeCatalog(
                catalog
            )

        val jsonRoundtripValid =
            validateRoundtrip(
                payload = payload,
                expectedCatalog = catalog
            )

        val sha256 =
            sha256(
                payload
            )

        val finalizationId =
            "canonical-food-catalog-final-v2-" +
                    sha256.take(
                        RELEASE_HASH_PREFIX_LENGTH
                    )

        val valid =
            catalog.size ==
                    EXPECTED_FINAL_ENTRY_COUNT &&
                    uniqueNormalizedKeyCount ==
                    catalog.size &&
                    uniqueCanonicalNameCount ==
                    catalog.size &&
                    deterministicOrder &&
                    jsonRoundtripValid &&
                    linguisticRepairValid &&
                    familyPluralFallbackCount == 0

        return CanonicalFoodCatalogV2FinalizationResult(
            schemaVersion = 1,
            releaseVersion = "v2",
            finalizationId = finalizationId,

            inputEntryCount =
                catalog.size,

            finalEntryCount =
                catalog.size,

            categoryCount =
                categories
                    .distinct()
                    .size,

            uniqueCanonicalNameCount =
                uniqueCanonicalNameCount,

            uniqueNormalizedKeyCount =
                uniqueNormalizedKeyCount,

            deterministicOrder =
                deterministicOrder,

            jsonRoundtripValid =
                jsonRoundtripValid,

            familyPluralFallbackCount =
                familyPluralFallbackCount,

            linguisticRepairValid =
                linguisticRepairValid,

            sha256 =
                sha256,

            valid =
                valid
        ) to payload
    }

    private fun serializeCatalog(
        catalog: List<JsonObject>
    ): String {

        val array =
            JsonArray().apply {
                catalog.forEach(::add)
            }

        return gson.toJson(array) + "\n"
    }

    private fun validateRoundtrip(
        payload: String,
        expectedCatalog: List<JsonObject>
    ): Boolean {

        val roundtrip =
            JsonParser
                .parseString(payload)
                .asJsonArray
                .mapNotNull { element ->
                    element
                        .takeIf {
                            it.isJsonObject
                        }
                        ?.asJsonObject
                }

        return roundtrip ==
                expectedCatalog
    }

    private fun isDeterministicallyOrdered(
        catalog: List<JsonObject>
    ): Boolean {

        val expected =
            catalog.sortedWith(
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

        return expected ==
                catalog
    }

    private fun canonicalNameKey(
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
                " "
            )
            .replace(
                WHITESPACE_REGEX,
                " "
            )
            .trim()
    }

    private fun sha256(
        payload: String
    ): String {

        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    payload.toByteArray(
                        StandardCharsets.UTF_8
                    )
                )

        return digest.joinToString(
            separator = ""
        ) { byte ->
            "%02x".format(
                byte.toInt() and 0xff
            )
        }
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
            "Missing or blank '$key': $json"
        }

    private fun requireBoolean(
        json: JsonObject,
        key: String
    ): Boolean =
        requireNotNull(
            json
                .get(key)
                ?.takeIf {
                    it.isJsonPrimitive
                }
        ) {
            "Missing boolean '$key'."
        }
            .asBoolean

    private companion object {

        const val EXPECTED_FINAL_ENTRY_COUNT =
            4596

        const val RELEASE_HASH_PREFIX_LENGTH =
            16

        val COMBINING_MARKS_REGEX =
            Regex("\\p{M}+")

        val NON_ALPHANUMERIC_REGEX =
            Regex("[^a-z0-9]+")

        val WHITESPACE_REGEX =
            Regex("\\s+")
    }
}