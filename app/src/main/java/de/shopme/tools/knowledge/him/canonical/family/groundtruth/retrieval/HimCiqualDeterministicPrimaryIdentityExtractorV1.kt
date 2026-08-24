package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import java.text.Normalizer
import java.util.Locale

/**
 * Deterministic CIQUAL Food-only primary-identity extraction.
 *
 * CIQUAL names remain source evidence. This adapter only separates a
 * source-native primary expression from trailing form/state/context terms;
 * target-family alignment is delegated to HimEvidenceAlignmentContractV1.
 */
object HimCiqualDeterministicPrimaryIdentityExtractorV1 {
    const val VERSION = "HIM_CIQUAL_DETERMINISTIC_PRIMARY_IDENTITY_EXTRACTOR_V1"

    private val contextBoundaries = setOf(
        "à", "a", "and", "aroma", "aromatisé", "aromatisée", "avec", "aux", "au",
        "de", "des", "du", "flavored", "flavoured", "mit", "of", "und", "with",
    )
    private val removableContextWords = contextBoundaries + setOf("la", "le", "les", "the")

    fun extract(
        record: HimEvidenceRetrievalIndexRecord,
        catalog: HimProductOnlyCanonicalMaster,
        authority: HimCanonicalFamilyAuthority,
    ): HimCiqualPrimaryIdentityExtractionV1 {
        require(record.recordKind == HimEvidenceRecordKind.CIQUAL_FOOD)
        require(record.sourceRecordReference.source == de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource.CIQUAL)

        val projection = JsonParser.parseString(record.evidenceProjection.deterministicJson).asJsonObject
        val vocabulary = vocabulary(catalog, authority)
        val groups = listOf(
            "groupNameFr", "groupNameEn", "subgroupNameFr", "subgroupNameEn",
            "subSubgroupNameFr", "subSubgroupNameEn",
        ).flatMap { projection.get(it)?.strings().orEmpty() }.map(::normalize)

        val productSignals = listOf(
            ProductField("nameFr", projection.get("nameFr")?.stringValue()),
            ProductField("nameEn", projection.get("nameEn")?.stringValue()),
        ).mapNotNull { field ->
            field.value?.trim()?.takeIf(String::isNotEmpty)?.let {
                analyseProductField(field.name, it, vocabulary, groups)
            }
        }
        val resolvedProducts = productSignals.mapNotNull { it.resolved }
        val productCandidates = productSignals.flatMap { it.candidates }

        if (resolvedProducts.size > 1 && resolvedProducts.drop(1).any { !sameIdentity(resolvedProducts.first().term, it.term) }) {
            return unresolved(record, listOfNotNull("nameFr", "nameEn"), resolvedProducts.map { it.term.displayName } + productCandidates)
        }

        val chosen = resolvedProducts.firstOrNull()
        if (chosen != null) {
            val supporting = resolvedProducts.filter { sameIdentity(chosen.term, it.term) }
            return resolved(
                record,
                productSignals.filter { it.resolved != null }.map { it.fieldName },
                chosen.copy(modifiers = supporting.flatMap { it.modifiers }.distinctBy(::normalize)),
            )
        }

        val scientific = projection.getAsJsonObject("scientificName")?.get("lexicalValue")?.stringValue()
            ?.trim()?.takeIf(String::isNotEmpty)
        val scientificTerm = scientific?.let { raw ->
            vocabulary.firstOrNull { it.authorityBound && it.normalizedName == normalize(raw) }
        }
        if (scientificTerm != null) {
            return resolved(
                record,
                listOf("scientificName.lexicalValue"),
                ResolvedSignal(scientificTerm, emptyList(), ExtractionPathV1.BOUND_SCIENTIFIC_SUPPORT),
            )
        }

        return if (productSignals.isEmpty() && scientific.isNullOrBlank()) {
            missing(record)
        } else {
            unresolved(record, productSignals.map { it.fieldName } + listOfNotNull("scientificName.lexicalValue".takeIf { scientific != null }), productCandidates)
        }
    }

    private fun analyseProductField(
        fieldName: String,
        value: String,
        vocabulary: List<VocabularyTerm>,
        groups: List<String>,
    ): FieldAnalysis {
        val normalized = normalize(value)
        vocabulary.firstOrNull { it.normalizedName == normalized }?.let {
            return FieldAnalysis(fieldName, ResolvedSignal(it, emptyList(), ExtractionPathV1.EXPLICIT_PRODUCT_IDENTITY), emptyList())
        }

        val alternatives = value.split(Regex("\\s*(?:/|\\|)\\s*")).map(String::trim).filter(String::isNotEmpty)
        if (alternatives.size > 1) {
            val terms = alternatives.mapNotNull { alternative -> vocabulary.firstOrNull { it.normalizedName == normalize(alternative) } }
            if (terms.size == alternatives.size && terms.map { it.familyKey ?: it.normalizedName }.distinct().size > 1) {
                return FieldAnalysis(fieldName, null, terms.map { it.displayName })
            }
        }

        val structural = value.substringBeforeAny(',', ';')
        val structuralTokens = tokenize(structural)
        val boundary = structuralTokens.indexOfFirst { it.normalized in contextBoundaries }
        val headTokens = if (boundary >= 0) structuralTokens.take(boundary) else structuralTokens
        val suffixTokens = if (boundary >= 0) {
            structuralTokens.drop(boundary + 1) + tokenize(value.substringAfterFirst(',', ';'))
        } else {
            tokenize(value.substringAfterFirst(',', ';'))
        }
        val candidates = headCandidates(headTokens, vocabulary, groups)
        val chosen = choose(candidates)
        if (chosen != null) {
            val modifiers = (chosen.modifiers + suffixTokens.map(TextToken::display))
                .filterNot { it.lowercase(Locale.ROOT) in removableContextWords }
            return FieldAnalysis(fieldName, ResolvedSignal(chosen.term, modifiers, ExtractionPathV1.CATALOG_COMPOSITION), emptyList())
        }
        if (candidates.size > 1) return FieldAnalysis(fieldName, null, candidates.map { it.term.displayName })
        return FieldAnalysis(fieldName, null, emptyList())
    }

    private fun headCandidates(
        tokens: List<TextToken>,
        vocabulary: List<VocabularyTerm>,
        groups: List<String>,
    ): List<HeadCandidate> {
        if (tokens.isEmpty()) return emptyList()
        return vocabulary.mapNotNull { term ->
            val termTokens = term.tokens
            val position = (0..tokens.size - termTokens.size).firstOrNull { start ->
                tokens.subList(start, start + termTokens.size).map(TextToken::normalized) == termTokens
            } ?: return@mapNotNull null
            HeadCandidate(
                term = term,
                modifiers = tokens.filterIndexed { index, _ -> index !in position until position + termTokens.size }
                    .map(TextToken::display),
                groupScore = if (groupSupports(term.normalizedName, groups)) 1 else 0,
                position = position,
                length = termTokens.size,
            )
        }
    }

    private fun choose(candidates: List<HeadCandidate>): HeadCandidate? {
        val ordered = candidates.sortedWith(
            compareByDescending<HeadCandidate> { it.groupScore }
                .thenBy { it.position }
                .thenByDescending { it.length }
                .thenBy { it.term.normalizedName }
                .thenBy { it.term.displayName },
        )
        val first = ordered.firstOrNull() ?: return null
        val tied = ordered.drop(1).any {
            it.groupScore == first.groupScore && it.position == first.position && it.length == first.length && !sameIdentity(first.term, it.term)
        }
        return first.takeUnless { tied }
    }

    private fun vocabulary(
        catalog: HimProductOnlyCanonicalMaster,
        authority: HimCanonicalFamilyAuthority,
    ): List<VocabularyTerm> {
        val byCanonical = authority.families.associateBy { normalize(it.normalizedName) }
        val terms = buildList {
            catalog.records.forEach { record ->
                val family = byCanonical[normalize(record.normalized)]
                add(VocabularyTerm(record.itemname, family?.canonicalId?.value, family != null, HimEntityType.CANONICAL))
            }
            authority.families.forEach { family ->
                add(VocabularyTerm(family.canonicalName, family.canonicalId.value, true, HimEntityType.CANONICAL))
                family.identities.forEach { identity ->
                    add(VocabularyTerm(identity.identityName, family.canonicalId.value, true, HimEntityType.IDENTITY))
                    identity.variants.forEach { add(VocabularyTerm(it.variantName, family.canonicalId.value, true, HimEntityType.VARIANT)) }
                    identity.aliases.forEach { add(VocabularyTerm(it.aliasName, family.canonicalId.value, true, HimEntityType.ALIAS)) }
                }
                family.variants.forEach { add(VocabularyTerm(it.variantName, family.canonicalId.value, true, HimEntityType.VARIANT)) }
                family.aliases.forEach { add(VocabularyTerm(it.aliasName, family.canonicalId.value, true, HimEntityType.ALIAS)) }
            }
        }
        return terms.filter { it.normalizedName.isNotEmpty() }
            .groupBy { it.normalizedName }
            .values
            .map { same -> same.sortedWith(compareByDescending<VocabularyTerm> { it.authorityBound }.thenBy { it.displayName }).first() }
            .sortedWith(compareBy<VocabularyTerm> { it.normalizedName }.thenBy { it.displayName })
    }

    private fun sameIdentity(left: VocabularyTerm, right: VocabularyTerm): Boolean =
        left.familyKey != null && left.familyKey == right.familyKey || left.normalizedName == right.normalizedName

    private fun groupSupports(term: String, groups: List<String>): Boolean {
        val pattern = Regex("(^|[^\\p{L}\\p{N}])${Regex.escape(term)}($|[^\\p{L}\\p{N}])")
        return groups.any(pattern::containsMatchIn)
    }

    private fun resolved(record: HimEvidenceRetrievalIndexRecord, fields: List<String>, signal: ResolvedSignal) = HimCiqualPrimaryIdentityExtractionV1(
        sourceRecordIdentity = record.sourceRecordReference.value,
        primaryIdentity = signal.term.displayName,
        resolution = HimCiqualPrimaryIdentityResolutionV1.RESOLVED,
        modifiers = signal.modifiers.distinctBy(::normalize),
        candidateIdentities = listOf(signal.term.displayName),
        primaryIdentityAuthorityBound = signal.term.authorityBound,
        identityFieldsUsed = fields.distinct(),
        extractionPath = signal.path.name,
    )

    private fun unresolved(record: HimEvidenceRetrievalIndexRecord, fields: List<String>, candidates: List<String>) = HimCiqualPrimaryIdentityExtractionV1(
        sourceRecordIdentity = record.sourceRecordReference.value,
        primaryIdentity = null,
        resolution = HimCiqualPrimaryIdentityResolutionV1.UNRESOLVED,
        modifiers = emptyList(),
        candidateIdentities = candidates.distinct().sortedBy(::normalize),
        primaryIdentityAuthorityBound = false,
        identityFieldsUsed = fields.distinct(),
        extractionPath = ExtractionPathV1.UNRESOLVED.name,
    )

    private fun missing(record: HimEvidenceRetrievalIndexRecord) = unresolved(record, emptyList(), emptyList()).copy(resolution = HimCiqualPrimaryIdentityResolutionV1.MISSING)

    private fun JsonElement.stringValue(): String? = takeUnless { it.isJsonNull }
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
        ?.asString

    private fun JsonElement.strings(): List<String> = if (isJsonArray) asJsonArray.mapNotNull { it.stringValue() } else listOfNotNull(stringValue())

    private fun tokenize(value: String): List<TextToken> = Regex("[\\p{L}\\p{N}]+").findAll(value)
        .map { TextToken(it.value, normalize(it.value)) }.filter { it.normalized.isNotEmpty() }.toList()

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFC)
        .trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

    private fun String.substringAfterFirst(vararg delimiters: Char): String {
        val index = delimiters.map { indexOf(it) }.filter { it >= 0 }.minOrNull() ?: length
        return if (index == length) "" else substring(index + 1)
    }

    private fun String.substringBeforeAny(vararg delimiters: Char): String {
        val index = delimiters.map { indexOf(it) }.filter { it >= 0 }.minOrNull() ?: length
        return substring(0, index)
    }

    private data class ProductField(val name: String, val value: String?)
    private data class FieldAnalysis(val fieldName: String, val resolved: ResolvedSignal?, val candidates: List<String>)
    private data class ResolvedSignal(val term: VocabularyTerm, val modifiers: List<String>, val path: ExtractionPathV1)
    private data class HeadCandidate(val term: VocabularyTerm, val modifiers: List<String>, val groupScore: Int, val position: Int, val length: Int)
    private data class TextToken(val display: String, val normalized: String)
    private data class VocabularyTerm(val displayName: String, val familyKey: String?, val authorityBound: Boolean, val entityType: HimEntityType?, val normalizedName: String = normalize(displayName), val tokens: List<String> = normalizedName.split(Regex("[^\\p{L}\\p{N}]+")).filter(String::isNotEmpty))
    private enum class ExtractionPathV1 { EXPLICIT_PRODUCT_IDENTITY, CATALOG_COMPOSITION, BOUND_SCIENTIFIC_SUPPORT, UNRESOLVED }
}

enum class HimCiqualPrimaryIdentityResolutionV1 { RESOLVED, MISSING, UNRESOLVED }

data class HimCiqualPrimaryIdentityExtractionV1(
    val sourceRecordIdentity: String,
    val primaryIdentity: String?,
    val resolution: HimCiqualPrimaryIdentityResolutionV1,
    val modifiers: List<String>,
    val candidateIdentities: List<String>,
    val primaryIdentityAuthorityBound: Boolean,
    val identityFieldsUsed: List<String>,
    val extractionPath: String,
) {
    init {
        require(sourceRecordIdentity.isNotBlank())
        when (resolution) {
            HimCiqualPrimaryIdentityResolutionV1.RESOLVED -> require(!primaryIdentity.isNullOrBlank())
            HimCiqualPrimaryIdentityResolutionV1.MISSING,
            HimCiqualPrimaryIdentityResolutionV1.UNRESOLVED -> require(primaryIdentity == null)
        }
        require(candidateIdentities.distinct().size == candidateIdentities.size)
        require(modifiers.all(String::isNotBlank))
        require(identityFieldsUsed.distinct().size == identityFieldsUsed.size)
        require(extractionPath.isNotBlank())
    }

    fun toAlignmentInput() = HimEvidenceAlignmentInputV1(
        sourceRecordIdentity = sourceRecordIdentity,
        primaryIdentity = primaryIdentity,
        primaryIdentityState = when (resolution) {
            HimCiqualPrimaryIdentityResolutionV1.RESOLVED -> HimPrimaryIdentityStateV1.RESOLVED
            HimCiqualPrimaryIdentityResolutionV1.MISSING -> HimPrimaryIdentityStateV1.MISSING
            HimCiqualPrimaryIdentityResolutionV1.UNRESOLVED -> HimPrimaryIdentityStateV1.UNRESOLVED
        },
        modifiers = modifiers,
    )
}
