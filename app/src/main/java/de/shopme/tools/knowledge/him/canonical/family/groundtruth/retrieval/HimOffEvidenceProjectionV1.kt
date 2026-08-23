package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.text.Normalizer
import java.util.Locale

object HimOffEvidenceProjectionV1 {
    private val projectionGroups = listOf(
        "source", "identity", "taxonomy", "ingredients", "nutrition",
        "classification", "allergens", "geography", "environmentalEvidence",
        "packagingEvidence", "quality",
    )

    fun fromOptimizedSourceLine(line: String, rowOrdinal: Long): HimEvidenceRetrievalIndexRecord {
        require(rowOrdinal > 0)
        val sourceRecord = JsonParser.parseString(line).asJsonObject
        return fromSourceObject(sourceRecord, rowOrdinal)
    }

    fun fromProjectionJson(projectionJson: String): HimEvidenceRetrievalIndexRecord {
        val projection = JsonParser.parseString(projectionJson).asJsonObject
        val rowOrdinal = projection.get("rowOrdinal").asLong
        return fromSourceObject(projection, rowOrdinal)
    }

    private fun fromSourceObject(sourceRecord: JsonObject, rowOrdinal: Long): HimEvidenceRetrievalIndexRecord {
        val code = requireNotNull(sourceRecord.getAsJsonObject("source")?.get("code")) {
            "OFF record row $rowOrdinal has no source.code"
        }.asString
        require(code.isNotBlank()) { "OFF record row $rowOrdinal has blank source.code" }

        val projection = JsonObject().apply {
            addProperty("rowOrdinal", rowOrdinal)
            projectionGroups.forEach { group ->
                sourceRecord.get(group)?.let { add(group, it.deepCopy()) }
            }
        }
        listOf("source", "identity", "quality").forEach { group ->
            require(projection.has(group)) { "OFF record row $rowOrdinal lacks required group $group" }
        }

        return HimEvidenceRetrievalIndexRecord(
            internalRecordKey = rowOrdinal,
            sourceRecordReference = HimEvidenceRecordReference.offProduct(rowOrdinal, code),
            recordKind = HimEvidenceRecordKind.OFF_PRODUCT,
            sourceNativeIdentifiersJson =
                "{\"code\":${JsonPrimitive(code)},\"rowOrdinal\":$rowOrdinal}",
            evidenceProjection = HimEvidenceProjection(projection.toString()),
            searchText = searchText(projection),
        )
    }

    private fun searchText(record: JsonObject): HimEvidenceSearchText {
        val identity = record.getAsJsonObject("identity")
        val taxonomy = record.getAsJsonObject("taxonomy")
        val ingredients = record.getAsJsonObject("ingredients")
        val nutrition = record.getAsJsonObject("nutrition")

        val primary = strings(identity, "productName")
        val secondary = buildList {
            addAll(strings(identity, "productNameGerman", "productNameEnglish"))
            addAll(strings(identity, "genericName", "genericNameGerman", "genericNameEnglish"))
            addAll(strings(identity, "brands", "quantity", "servingSize", "productType"))
        }
        val taxonomyText = strings(
            taxonomy, "categories", "categoryHierarchy", "foodGroups", "pnnsGroups", "mainCategory"
        )
        val ingredientText = buildList {
            addAll(strings(ingredients, "text", "tags", "hierarchy"))
            ingredients?.get("items")?.let { addAll(allStrings(it)) }
            addAll(strings(
                nutrition, "vitamins", "minerals", "aminoAcids", "nucleotides",
                "otherNutritionalSubstances"
            ))
        }
        val context = buildList {
            record.get("allergens")?.let { addAll(allStrings(it)) }
            record.get("geography")?.let { addAll(allStrings(it)) }
            record.get("packagingEvidence")?.let { addAll(allStrings(it)) }
        }
        return HimEvidenceSearchText(
            primaryName = lookup(primary),
            secondaryNames = lookup(secondary),
            taxonomyText = lookup(taxonomyText),
            ingredientText = lookup(ingredientText),
            contextText = lookup(context),
        )
    }

    private fun strings(objectValue: JsonObject?, vararg names: String): List<String> =
        buildList {
            names.forEach { name -> objectValue?.get(name)?.let { addAll(allStrings(it)) } }
        }

    private fun allStrings(element: JsonElement): List<String> =
        buildList {
            when {
                element.isJsonPrimitive && element.asJsonPrimitive.isString -> add(element.asString)
                element.isJsonArray -> element.asJsonArray.forEach { addAll(allStrings(it)) }
                element.isJsonObject -> element.asJsonObject.entrySet().forEach { (_, value) ->
                    addAll(allStrings(value))
                }
            }
        }

    private fun lookup(values: List<String>): String {
        val unique = linkedSetOf<String>()
        values.forEach { value ->
            val normalized = Normalizer.normalize(value, Normalizer.Form.NFC)
                .trim()
                .replace(Regex("\\s+"), " ")
                .lowercase(Locale.ROOT)
            if (normalized.isNotEmpty()) unique += normalized
        }
        return unique.joinToString(" ")
    }
}
