package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.text.Normalizer
import java.util.Locale

object HimAgribalyseEvidenceProjectionV1 {
    fun fromOptimizedSourceLine(line: String, rowOrdinal: Long): HimEvidenceRetrievalIndexRecord {
        require(rowOrdinal > 0)
        return fromSourceObject(JsonParser.parseString(line).asJsonObject, rowOrdinal)
    }

    fun fromProjectionJson(projectionJson: String): HimEvidenceRetrievalIndexRecord {
        val projection = JsonParser.parseString(projectionJson).asJsonObject
        return fromSourceObject(projection, projection.get("rowOrdinal").asLong)
    }

    private fun fromSourceObject(source: JsonObject, rowOrdinal: Long): HimEvidenceRetrievalIndexRecord {
        val agbCode = requiredString(source, "agbCode", rowOrdinal)
        val projection = JsonObject().apply {
            addProperty("rowOrdinal", rowOrdinal)
            source.entrySet().forEach { (name, value) ->
                if (name != "rowOrdinal") add(name, value.deepCopy())
            }
        }
        val ciqualCode = requiredString(projection, "ciqualCode", rowOrdinal)
        val seasonCode = requiredString(projection, "seasonCode", rowOrdinal)
        val airTransportCode = requiredString(projection, "airTransportCode", rowOrdinal)
        return HimEvidenceRetrievalIndexRecord(
            internalRecordKey = rowOrdinal,
            sourceRecordReference = HimEvidenceRecordReference.agribalyse(rowOrdinal, agbCode),
            recordKind = HimEvidenceRecordKind.AGRIBALYSE_RECORD,
            sourceNativeIdentifiersJson =
                "{\"rowOrdinal\":$rowOrdinal,\"agbCode\":${JsonPrimitive(agbCode)}," +
                    "\"ciqualCode\":${JsonPrimitive(ciqualCode)},\"seasonCode\":${JsonPrimitive(seasonCode)}," +
                    "\"airTransportCode\":${JsonPrimitive(airTransportCode)}}",
            evidenceProjection = HimEvidenceProjection(projection.toString()),
            searchText = HimEvidenceSearchText(
                primaryName = lookup(requiredString(projection, "productNameFr", rowOrdinal)),
                secondaryNames = lookup(requiredString(projection, "lciName", rowOrdinal)),
                taxonomyText = lookup(
                    requiredString(projection, "foodGroup", rowOrdinal),
                    requiredString(projection, "foodSubgroup", rowOrdinal),
                ),
                ingredientText = "",
                contextText = lookup(
                    requiredString(projection, "preparation", rowOrdinal),
                    requiredString(projection, "delivery", rowOrdinal),
                    requiredString(projection, "packagingApproach", rowOrdinal),
                ),
            ),
        )
    }

    private fun requiredString(record: JsonObject, name: String, rowOrdinal: Long): String {
        val value = requireNotNull(record.get(name)) { "AGRIBALYSE row $rowOrdinal lacks $name" }.asString
        require(value.isNotBlank()) { "AGRIBALYSE row $rowOrdinal has blank $name" }
        return value
    }

    private fun lookup(vararg values: String): String = values.asSequence()
        .map { Normalizer.normalize(it, Normalizer.Form.NFC).trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT) }
        .filter(String::isNotEmpty)
        .distinct()
        .joinToString(" ")
}
