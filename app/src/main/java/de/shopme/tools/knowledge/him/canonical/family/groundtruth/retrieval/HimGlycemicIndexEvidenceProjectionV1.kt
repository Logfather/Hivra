package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.text.Normalizer
import java.util.Locale

enum class HimGlycemicIndexLogicalRecordKind(
    val evidenceKind: HimEvidenceRecordKind,
    val referenceKind: String,
) {
    MEASUREMENT(HimEvidenceRecordKind.GI_MEASUREMENT, "measurement"),
    MEAN_SUMMARY(HimEvidenceRecordKind.GI_MEAN_SUMMARY, "mean-summary"),
    CATEGORY_NOTE(HimEvidenceRecordKind.GI_CATEGORY_NOTE, "category-note"),
    FOOTNOTE(HimEvidenceRecordKind.GI_FOOTNOTE, "footnote"),
}

object HimGlycemicIndexEvidenceProjectionV1 {
    fun fromSourceObject(source: JsonObject, kind: HimGlycemicIndexLogicalRecordKind, ordinal: Long, internalKey: Long): HimEvidenceRetrievalIndexRecord {
        require(ordinal > 0 && internalKey > 0)
        val projection = JsonObject().apply {
            addProperty("recordKind", kind.name)
            addProperty("arrayOrdinal", ordinal)
            source.entrySet().forEach { (name, value) -> if (name !in setOf("recordKind", "arrayOrdinal")) add(name, value.deepCopy()) }
        }
        val reference = HimEvidenceRecordReference.gi(kind.referenceKind, ordinal)
        return when (kind) {
            HimGlycemicIndexLogicalRecordKind.MEASUREMENT -> measurement(projection, reference, internalKey, ordinal)
            HimGlycemicIndexLogicalRecordKind.MEAN_SUMMARY -> summary(projection, reference, internalKey, ordinal)
            HimGlycemicIndexLogicalRecordKind.CATEGORY_NOTE -> categoryNote(projection, reference, internalKey, ordinal)
            HimGlycemicIndexLogicalRecordKind.FOOTNOTE -> footnote(projection, reference, internalKey, ordinal)
        }
    }

    fun fromProjectionJson(json: String, internalKey: Long): HimEvidenceRetrievalIndexRecord {
        val projection = JsonParser.parseString(json).asJsonObject
        return fromSourceObject(
            projection,
            HimGlycemicIndexLogicalRecordKind.valueOf(projection.get("recordKind").asString),
            projection.get("arrayOrdinal").asLong,
            internalKey,
        )
    }

    private fun measurement(value: JsonObject, reference: HimEvidenceRecordReference, key: Long, ordinal: Long): HimEvidenceRetrievalIndexRecord {
        val sourceContext = value.getAsJsonObject("sourceContext")
        val foodNumber = value.get("foodNumber").asLong
        val page = value.get("pageNumber").asLong
        val referenceCode = lexical(value, "referenceCode")
        return record(
            key, reference, HimEvidenceRecordKind.GI_MEASUREMENT,
            "{\"arrayOrdinal\":$ordinal,\"foodNumber\":$foodNumber,\"pageNumber\":$page,\"referenceCode\":${JsonPrimitive(referenceCode)}}",
            value,
            HimEvidenceSearchText(
                lookup(lexical(value, "foodItem")), "",
                lookup(*strings(sourceContext, "majorCategory", "subcategory", "deeperHeading")), "",
                lookup(
                    lexical(value, "country"), lexical(value, "referenceFoodTime"), lexical(value, "timepoints"),
                    lexical(value, "sampleCollection"), lexical(value, "analysisMethod"),
                ),
            ),
        )
    }

    private fun summary(value: JsonObject, reference: HimEvidenceRecordReference, key: Long, ordinal: Long): HimEvidenceRetrievalIndexRecord {
        val sourceContext = value.getAsJsonObject("sourceContext")
        return record(
            key, reference, HimEvidenceRecordKind.GI_MEAN_SUMMARY,
            "{\"arrayOrdinal\":$ordinal,\"pageNumber\":${value.get("pageNumber").asLong}}", value,
            HimEvidenceSearchText(lookup(lexical(value, "lexicalText")), "", lookup(*strings(sourceContext, "majorCategory", "subcategory", "deeperHeading")), "", ""),
        )
    }

    private fun categoryNote(value: JsonObject, reference: HimEvidenceRecordReference, key: Long, ordinal: Long): HimEvidenceRetrievalIndexRecord {
        val sourceContext = value.getAsJsonObject("sourceContext")
        return record(
            key, reference, HimEvidenceRecordKind.GI_CATEGORY_NOTE,
            "{\"arrayOrdinal\":$ordinal,\"pageNumber\":${value.get("pageNumber").asLong}}", value,
            HimEvidenceSearchText(
                lookup(*strings(sourceContext, "majorCategory")), "",
                lookup(*strings(sourceContext, "subcategory", "deeperHeading")), "", lookup(lexical(value, "lexicalText")),
            ),
        )
    }

    private fun footnote(value: JsonObject, reference: HimEvidenceRecordReference, key: Long, ordinal: Long): HimEvidenceRetrievalIndexRecord {
        val identifier = lexical(value, "identifier")
        return record(
            key, reference, HimEvidenceRecordKind.GI_FOOTNOTE,
            "{\"arrayOrdinal\":$ordinal,\"pageNumber\":${value.get("pageNumber").asLong},\"identifier\":${JsonPrimitive(identifier)}}", value,
            HimEvidenceSearchText("", "", "", "", lookup(lexical(value, "lexicalText"))),
        )
    }

    private fun record(
        key: Long, reference: HimEvidenceRecordReference, kind: HimEvidenceRecordKind,
        identifiers: String, projection: JsonObject, search: HimEvidenceSearchText,
    ) = HimEvidenceRetrievalIndexRecord(key, reference, kind, identifiers, HimEvidenceProjection(projection.toString()), search)

    private fun lexical(value: JsonObject, name: String): String =
        value.getAsJsonObject(name)?.get("lexicalValue")?.takeUnless { it.isJsonNull }?.asString.orEmpty()
    private fun strings(value: JsonObject?, vararg names: String): Array<String> = names.mapNotNull { name -> value?.get(name)?.takeUnless { it.isJsonNull }?.asString }.toTypedArray()
    private fun lookup(vararg values: String): String = values.asSequence()
        .map { Normalizer.normalize(it, Normalizer.Form.NFC).trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT) }
        .filter(String::isNotEmpty).distinct().joinToString(" ")
}
