package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import java.text.Normalizer
import java.util.Locale

/**
 * Deterministic OFF-only extraction. It consumes an already-created OFF
 * evidence projection and never receives a desired target canonical.
 */
object HimOffDeterministicPrimaryIdentityExtractorV1 {
    const val VERSION = "HIM_OFF_DETERMINISTIC_PRIMARY_IDENTITY_EXTRACTOR_V1"

    private val identityFields = listOf(
        IdentityField("identity.productName", "productName", ExtractionPathV1.EXPLICIT_PRODUCT_IDENTITY),
        IdentityField("identity.productNameGerman", "productNameGerman", ExtractionPathV1.STRUCTURED_IDENTITY),
        IdentityField("identity.productNameEnglish", "productNameEnglish", ExtractionPathV1.STRUCTURED_IDENTITY),
        IdentityField("identity.genericName", "genericName", ExtractionPathV1.GENERIC_NAME),
        IdentityField("identity.genericNameGerman", "genericNameGerman", ExtractionPathV1.GENERIC_NAME),
        IdentityField("identity.genericNameEnglish", "genericNameEnglish", ExtractionPathV1.GENERIC_NAME),
    )

    /* These are structural language/context boundaries, not identity labels. */
    private val contextBoundaries = setOf(
        "à", "and", "aroma", "aromatisé", "aromatisée", "avec", "aux", "au",
        "flavored", "flavoured", "mit", "of", "und", "with",
    )

    fun extract(
        record: HimEvidenceRetrievalIndexRecord,
        catalog: HimProductOnlyCanonicalMaster,
        authority: HimCanonicalFamilyAuthority,
    ): HimOffPrimaryIdentityExtractionV1 {
        require(record.recordKind == HimEvidenceRecordKind.OFF_PRODUCT)
        require(record.sourceRecordReference.source == de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource.OPEN_FOOD_FACTS)

        val projection = JsonParser.parseString(record.evidenceProjection.deterministicJson).asJsonObject
        val identity = projection.getAsJsonObject("identity")
        val brands = identity?.get("brands")?.let(::strings).orEmpty()
        val vocabulary = vocabulary(catalog, authority)
        val taxonomyTerms = projection.getAsJsonObject("taxonomy")?.let(::allStrings)
            ?.map(::normalize)
            ?.filter(String::isNotEmpty)
            ?.toSet()
            .orEmpty()

        var sawIdentityField = false
        identityFields.forEach { field ->
            val raw = identity?.get(field.jsonName)?.let(::stringValue)?.trim()
                ?.takeIf(String::isNotEmpty) ?: return@forEach
            sawIdentityField = true
            val cleaned = removeBrandPrefix(raw, brands)
            val normalizedCleaned = normalize(cleaned)
            vocabulary.firstOrNull { it.normalizedName == normalizedCleaned }?.let { term ->
                return resolved(record, term, emptyList(), field, ExtractionPathV1.EXPLICIT_PRODUCT_IDENTITY)
            }

            val alternatives = cleaned.split(Regex("\\s*(?:/|\\|)\\s*"))
                .map(String::trim)
                .filter(String::isNotEmpty)
            if (alternatives.size > 1) {
                val alternativeTerms = alternatives.mapNotNull { alternative ->
                    vocabulary.firstOrNull { it.normalizedName == normalize(alternative) }
                }
                if (alternativeTerms.size == alternatives.size && alternativeTerms.map { it.normalizedName }.distinct().size > 1) {
                    return unresolved(record, field, alternativeTerms.map { it.displayName })
                }
            }

            val leadingTokens = leadingProductExpression(cleaned)
            val candidates = headCandidates(leadingTokens, vocabulary, taxonomyTerms)
            choose(candidates)?.let { chosen ->
                val path = if (field.pathType == ExtractionPathV1.GENERIC_NAME) {
                    ExtractionPathV1.GENERIC_NAME
                } else {
                    ExtractionPathV1.CATALOG_COMPOSITION
                }
                return resolved(record, chosen.term, chosen.modifiers, field, path)
            }
            if (candidates.size > 1) {
                return unresolved(record, field, candidates.map { it.term.displayName })
            }
        }

        return if (sawIdentityField) {
            unresolved(record, null, emptyList())
        } else {
            missing(record)
        }
    }

    private fun resolved(
        record: HimEvidenceRetrievalIndexRecord,
        term: VocabularyTerm,
        modifiers: List<String>,
        field: IdentityField,
        path: ExtractionPathV1,
    ) = HimOffPrimaryIdentityExtractionV1(
        sourceRecordIdentity = record.sourceRecordReference.value,
        primaryIdentity = term.displayName,
        resolution = HimOffPrimaryIdentityResolutionV1.RESOLVED,
        modifiers = modifiers,
        candidateIdentities = listOf(term.displayName),
        identityFieldUsed = field.path,
        extractionPath = path.name,
    )

    private fun unresolved(
        record: HimEvidenceRetrievalIndexRecord,
        field: IdentityField?,
        candidates: List<String>,
    ) = HimOffPrimaryIdentityExtractionV1(
        sourceRecordIdentity = record.sourceRecordReference.value,
        primaryIdentity = null,
        resolution = HimOffPrimaryIdentityResolutionV1.UNRESOLVED,
        modifiers = emptyList(),
        candidateIdentities = candidates.distinct().sortedBy(::normalize),
        identityFieldUsed = field?.path,
        extractionPath = ExtractionPathV1.UNRESOLVED.name,
    )

    private fun missing(record: HimEvidenceRetrievalIndexRecord) = HimOffPrimaryIdentityExtractionV1(
        sourceRecordIdentity = record.sourceRecordReference.value,
        primaryIdentity = null,
        resolution = HimOffPrimaryIdentityResolutionV1.MISSING,
        modifiers = emptyList(),
        candidateIdentities = emptyList(),
        identityFieldUsed = null,
        extractionPath = ExtractionPathV1.UNRESOLVED.name,
    )

    private fun choose(candidates: List<HeadCandidate>): HeadCandidate? {
        if (candidates.isEmpty()) return null
        val ordered = candidates.sortedWith(
            compareByDescending<HeadCandidate> { it.taxonomyScore }
                .thenBy { it.term.normalizedName }
                .thenBy { it.term.displayName },
        )
        val best = ordered.first()
        return if (ordered.drop(1).any { it.taxonomyScore == best.taxonomyScore }) null else best
    }

    private fun headCandidates(
        tokens: List<TextToken>,
        vocabulary: List<VocabularyTerm>,
        taxonomyTerms: Set<String>,
    ): List<HeadCandidate> {
        if (tokens.isEmpty()) return emptyList()
        val candidates = mutableListOf<HeadCandidate>()

        vocabulary.forEach { term ->
            val termTokens = term.tokens
            if (termTokens.size <= tokens.size && tokens.takeLast(termTokens.size).map(TextToken::normalized) == termTokens) {
                candidates += HeadCandidate(
                    term = term,
                    modifiers = tokens.dropLast(termTokens.size).map(TextToken::display),
                    taxonomyScore = if (term.normalizedName in taxonomyTerms) 1 else 0,
                )
            }
        }

        if (tokens.size == 1) {
            segmentClosedCompound(tokens.single(), vocabulary).forEach { segmentation ->
                val head = segmentation.last()
                candidates += HeadCandidate(
                    term = head.term,
                    modifiers = segmentation.dropLast(1).map { it.term.displayName },
                    taxonomyScore = if (head.term.normalizedName in taxonomyTerms) 1 else 0,
                )
            }
        }
        return candidates.distinctBy { it.term.normalizedName }
    }

    private fun segmentClosedCompound(
        token: TextToken,
        vocabulary: List<VocabularyTerm>,
    ): List<List<Segment>> {
        val matches = vocabulary.filter { term ->
            term.tokens.size == 1 && token.normalized.contains(term.tokens.single())
        }.sortedWith(compareBy<VocabularyTerm> { it.normalizedName.length }.thenBy { it.normalizedName })

        fun visit(remaining: String, segments: List<Segment>): List<List<Segment>> {
            if (remaining.isEmpty()) return listOf(segments)
            return matches.asSequence()
                .filter { remaining.startsWith(it.tokens.single()) }
                .flatMap { term -> visit(remaining.removePrefix(term.tokens.single()), segments + Segment(term)) }
                .take(MAX_COMPOUND_SEGMENTATIONS)
                .toList()
        }
        return visit(token.normalized, emptyList())
    }

    private fun leadingProductExpression(value: String): List<TextToken> {
        val commaSeparated = value.substringBeforeAny(',', ';')
        val tokens = tokenize(commaSeparated)
        val boundary = tokens.indexOfFirst { it.normalized in contextBoundaries }
        return if (boundary < 0) tokens else tokens.take(boundary)
    }

    private fun removeBrandPrefix(value: String, brands: List<String>): String {
        val tokens = tokenize(value)
        val brandTokens = brands.flatMap(::tokenize)
        if (brandTokens.isNotEmpty() && tokens.take(brandTokens.size).map(TextToken::normalized) == brandTokens.map(TextToken::normalized)) {
            return tokens.drop(brandTokens.size).joinToString(" ") { it.display }
        }
        return value
    }

    private fun vocabulary(
        catalog: HimProductOnlyCanonicalMaster,
        authority: HimCanonicalFamilyAuthority,
    ): List<VocabularyTerm> {
        val terms = buildList {
            catalog.records.forEach { add(VocabularyTerm(it.itemname)) }
            authority.families.forEach { family ->
                add(VocabularyTerm(family.canonicalName))
                family.identities.forEach { identity ->
                    add(VocabularyTerm(identity.identityName))
                    identity.variants.forEach { add(VocabularyTerm(it.variantName)) }
                    identity.aliases.forEach { add(VocabularyTerm(it.aliasName)) }
                }
                family.variants.forEach { add(VocabularyTerm(it.variantName)) }
                family.aliases.forEach { add(VocabularyTerm(it.aliasName)) }
            }
        }
        return terms
            .filter { it.normalizedName.isNotEmpty() }
            .groupBy { it.normalizedName }
            .values
            .map { same -> same.sortedBy { it.displayName }.first() }
            .sortedWith(compareBy<VocabularyTerm> { it.normalizedName }.thenBy { it.displayName })
    }

    private fun strings(element: com.google.gson.JsonElement): List<String> =
        if (element.isJsonArray) element.asJsonArray.mapNotNull(::stringValue) else listOfNotNull(stringValue(element))

    private fun allStrings(element: JsonObject): List<String> =
        element.entrySet().flatMap { (_, value) -> stringsRecursively(value) }

    private fun stringsRecursively(element: com.google.gson.JsonElement): List<String> = when {
        element.isJsonPrimitive && element.asJsonPrimitive.isString -> listOf(element.asString)
        element.isJsonArray -> element.asJsonArray.flatMap(::stringsRecursively)
        element.isJsonObject -> element.asJsonObject.entrySet().flatMap { (_, value) -> stringsRecursively(value) }
        else -> emptyList()
    }

    private fun stringValue(element: com.google.gson.JsonElement): String? =
        element.takeUnless { it.isJsonNull }?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    private fun tokenize(value: String): List<TextToken> =
        Regex("[\\p{L}\\p{N}]+")
            .findAll(value)
            .map { TextToken(it.value, normalize(it.value)) }
            .filter { it.normalized.isNotEmpty() }
            .toList()

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFC)
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)

    private fun String.substringBeforeAny(vararg delimiters: Char): String {
        val index = delimiters.map { indexOf(it) }.filter { it >= 0 }.minOrNull() ?: length
        return substring(0, index)
    }

    private data class IdentityField(
        val path: String,
        val jsonName: String,
        val pathType: ExtractionPathV1,
    )

    private data class VocabularyTerm(
        val displayName: String,
    ) {
        val normalizedName: String = normalize(displayName)
        val tokens: List<String> = normalizedName.split(Regex("[^\\p{L}\\p{N}]+"))
            .filter(String::isNotEmpty)
    }

    private data class TextToken(val display: String, val normalized: String)
    private data class Segment(val term: VocabularyTerm)
    private data class HeadCandidate(val term: VocabularyTerm, val modifiers: List<String>, val taxonomyScore: Int)

    private enum class ExtractionPathV1 {
        EXPLICIT_PRODUCT_IDENTITY,
        STRUCTURED_IDENTITY,
        GENERIC_NAME,
        CATALOG_COMPOSITION,
        UNRESOLVED,
    }

    private const val MAX_COMPOUND_SEGMENTATIONS = 32
}

enum class HimOffPrimaryIdentityResolutionV1 {
    RESOLVED,
    MISSING,
    UNRESOLVED,
}

data class HimOffPrimaryIdentityExtractionV1(
    val sourceRecordIdentity: String,
    val primaryIdentity: String?,
    val resolution: HimOffPrimaryIdentityResolutionV1,
    val modifiers: List<String>,
    val candidateIdentities: List<String>,
    val identityFieldUsed: String?,
    val extractionPath: String,
) {
    init {
        require(sourceRecordIdentity.isNotBlank())
        when (resolution) {
            HimOffPrimaryIdentityResolutionV1.RESOLVED -> require(!primaryIdentity.isNullOrBlank())
            HimOffPrimaryIdentityResolutionV1.MISSING,
            HimOffPrimaryIdentityResolutionV1.UNRESOLVED,
            -> require(primaryIdentity == null)
        }
        require(candidateIdentities.distinct().size == candidateIdentities.size)
        require(modifiers.all(String::isNotBlank))
        require(extractionPath.isNotBlank())
    }

    fun toAlignmentInput(): HimEvidenceAlignmentInputV1 = HimEvidenceAlignmentInputV1(
        sourceRecordIdentity = sourceRecordIdentity,
        primaryIdentity = primaryIdentity,
        primaryIdentityState = when (resolution) {
            HimOffPrimaryIdentityResolutionV1.RESOLVED -> HimPrimaryIdentityStateV1.RESOLVED
            HimOffPrimaryIdentityResolutionV1.MISSING -> HimPrimaryIdentityStateV1.MISSING
            HimOffPrimaryIdentityResolutionV1.UNRESOLVED -> HimPrimaryIdentityStateV1.UNRESOLVED
        },
        modifiers = modifiers,
    )
}
