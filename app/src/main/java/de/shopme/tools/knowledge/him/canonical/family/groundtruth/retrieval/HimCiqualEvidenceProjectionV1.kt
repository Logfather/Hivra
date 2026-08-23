package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.text.Normalizer
import java.util.Locale

enum class HimCiqualLogicalRecordKind(val evidenceKind: HimEvidenceRecordKind) {
    FOOD(HimEvidenceRecordKind.CIQUAL_FOOD),
    TAXONOMY(HimEvidenceRecordKind.CIQUAL_TAXONOMY),
    CONSTITUENT(HimEvidenceRecordKind.CIQUAL_CONSTITUENT),
    SOURCE(HimEvidenceRecordKind.CIQUAL_SOURCE),
}

object HimCiqualEvidenceProjectionV1 {
    fun fromSourceObject(source: JsonObject, kind: HimCiqualLogicalRecordKind, internalKey: Long): HimEvidenceRetrievalIndexRecord {
        require(internalKey > 0)
        val projection = JsonObject().apply {
            addProperty("recordKind", kind.name)
            source.entrySet().forEach { (name, value) -> if (name != "recordKind") add(name, value.deepCopy()) }
        }
        return when (kind) {
            HimCiqualLogicalRecordKind.FOOD -> food(projection, internalKey)
            HimCiqualLogicalRecordKind.TAXONOMY -> taxonomy(projection, internalKey)
            HimCiqualLogicalRecordKind.CONSTITUENT -> constituent(projection, internalKey)
            HimCiqualLogicalRecordKind.SOURCE -> source(projection, internalKey)
        }
    }

    fun fromProjectionJson(json: String, internalKey: Long): HimEvidenceRetrievalIndexRecord {
        val projection = JsonParser.parseString(json).asJsonObject
        return fromSourceObject(projection, HimCiqualLogicalRecordKind.valueOf(projection.get("recordKind").asString), internalKey)
    }

    private fun food(value: JsonObject, key: Long): HimEvidenceRetrievalIndexRecord {
        val alim = required(value, "alimCode")
        val group = required(value, "groupCode")
        val subgroup = required(value, "subgroupCode")
        val subSubgroup = required(value, "subSubgroupCode")
        return record(
            key, HimEvidenceRecordReference.ciqualFood(alim), HimEvidenceRecordKind.CIQUAL_FOOD,
            "{\"alimCode\":${JsonPrimitive(alim)},\"groupCode\":${JsonPrimitive(group)},\"subgroupCode\":${JsonPrimitive(subgroup)},\"subSubgroupCode\":${JsonPrimitive(subSubgroup)}}",
            value,
            HimEvidenceSearchText(
                lookup(required(value, "nameFr")),
                lookup(required(value, "nameEn"), lexical(value, "scientificName")),
                "", "", "",
            ),
        )
    }

    private fun taxonomy(value: JsonObject, key: Long): HimEvidenceRetrievalIndexRecord {
        val group = required(value, "groupCode")
        val subgroup = required(value, "subgroupCode")
        val subSubgroup = required(value, "subSubgroupCode")
        return record(
            key, HimEvidenceRecordReference.ciqualTaxonomy(group, subgroup, subSubgroup), HimEvidenceRecordKind.CIQUAL_TAXONOMY,
            "{\"groupCode\":${JsonPrimitive(group)},\"subgroupCode\":${JsonPrimitive(subgroup)},\"subSubgroupCode\":${JsonPrimitive(subSubgroup)}}",
            value,
            HimEvidenceSearchText(
                lookup(required(value, "subSubgroupNameFr"), required(value, "subgroupNameFr"), required(value, "groupNameFr")),
                lookup(required(value, "subSubgroupNameEn"), required(value, "subgroupNameEn"), required(value, "groupNameEn")),
                "", "", "",
            ),
        )
    }

    private fun constituent(value: JsonObject, key: Long): HimEvidenceRetrievalIndexRecord {
        val constCode = required(value, "constCode")
        val infoods = lexical(value, "infoodsCode")
        return record(
            key, HimEvidenceRecordReference.ciqualConstituent(constCode), HimEvidenceRecordKind.CIQUAL_CONSTITUENT,
            "{\"constCode\":${JsonPrimitive(constCode)},\"infoodsCode\":${JsonPrimitive(infoods)}}",
            value,
            HimEvidenceSearchText(lookup(required(value, "nameFr")), lookup(required(value, "nameEn"), infoods), "", "", ""),
        )
    }

    private fun source(value: JsonObject, key: Long): HimEvidenceRetrievalIndexRecord {
        val sourceCode = required(value, "sourceCode")
        return record(
            key, HimEvidenceRecordReference.ciqualSource(sourceCode), HimEvidenceRecordKind.CIQUAL_SOURCE,
            "{\"sourceCode\":${JsonPrimitive(sourceCode)}}", value,
            HimEvidenceSearchText("", "", "", "", lookup(lexical(value, "citation"))),
        )
    }

    private fun record(
        key: Long,
        reference: HimEvidenceRecordReference,
        kind: HimEvidenceRecordKind,
        identifiers: String,
        projection: JsonObject,
        search: HimEvidenceSearchText,
    ) = HimEvidenceRetrievalIndexRecord(
        key, reference, kind, identifiers, HimEvidenceProjection(projection.toString()), search,
    )

    private fun required(value: JsonObject, name: String): String = requireNotNull(value.get(name)).asString.also { require(it.isNotBlank()) }

    private fun lexical(value: JsonObject, name: String): String =
        requireNotNull(value.getAsJsonObject(name)?.get("lexicalValue")).asString

    private fun lookup(vararg values: String): String = values.asSequence()
        .map { Normalizer.normalize(it, Normalizer.Form.NFC).trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT) }
        .filter(String::isNotEmpty).distinct().joinToString(" ")
}
