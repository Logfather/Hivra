package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import java.text.Normalizer
import java.util.Locale

/**
 * Deterministic Agribalyse-only extraction. French and LCI names remain
 * source-language evidence; this class performs no translation.
 */
object HimAgribalyseDeterministicPrimaryIdentityExtractorV1 {
    const val VERSION = "HIM_AGRIBALYSE_DETERMINISTIC_PRIMARY_IDENTITY_EXTRACTOR_V1"

    private val qualifierBoundaries = setOf(
        "and", "aroma", "aromatisé", "aromatisée", "avec", "aux", "au",
        "flavored", "flavoured", "mit", "of", "und", "with",
    )

    fun extract(
        record: HimEvidenceRetrievalIndexRecord,
        catalog: HimProductOnlyCanonicalMaster,
        authority: HimCanonicalFamilyAuthority,
    ): HimAgribalysePrimaryIdentityExtractionV1 {
        require(record.recordKind == HimEvidenceRecordKind.AGRIBALYSE_RECORD)
        require(record.sourceRecordReference.source == de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource.AGRIBALYSE)

        val projection = JsonParser.parseString(record.evidenceProjection.deterministicJson).asJsonObject
        val vocabulary = vocabulary(catalog, authority)
        val groups = listOfNotNull(
            projection.get("foodGroup")?.stringValue(),
            projection.get("foodSubgroup")?.stringValue(),
        ).map(::normalize)

        val signals = listOf(
            ProductField("productNameFr", projection.get("productNameFr")?.stringValue()),
            ProductField("lciName", projection.get("lciName")?.stringValue()),
        ).mapNotNull { field ->
            field.value?.trim()?.takeIf(String::isNotEmpty)?.let { value ->
                analyseField(field.name, value, vocabulary, groups)
            }
        }

        val resolved = signals.mapNotNull { it.resolved }
        val candidates = signals.flatMap { it.candidates }.distinctBy { normalize(it) }.sortedBy(::normalize)
        if (resolved.isEmpty()) {
            return if (signals.isEmpty()) {
                missing(record)
            } else {
                unresolved(record, signals.firstOrNull()?.fieldName, candidates)
            }
        }

        val first = resolved.first()
        val conflicting = resolved.drop(1).filterNot { sameIdentity(first.term, it.term) }
        if (conflicting.isNotEmpty()) {
            return unresolved(
                record,
                signals.firstOrNull()?.fieldName,
                (resolved.map { it.term.displayName } + candidates).distinct().sortedBy(::normalize),
            )
        }

        val modifiers = resolved
            .flatMap { it.modifiers }
            .distinctBy(::normalize)
        return HimAgribalysePrimaryIdentityExtractionV1(
            sourceRecordIdentity = record.sourceRecordReference.value,
            primaryIdentity = first.term.displayName,
            resolution = HimAgribalysePrimaryIdentityResolutionV1.RESOLVED,
            modifiers = modifiers,
            candidateIdentities = listOf(first.term.displayName),
            primaryIdentityAuthorityBound = first.term.authorityBound,
            identityFieldsUsed = resolved.map { it.fieldName }.distinct(),
            extractionPath = resolved.first().path.name,
        )
    }

    private fun analyseField(
        fieldName: String,
        value: String,
        vocabulary: List<VocabularyTerm>,
        groups: List<String>,
    ): FieldAnalysis {
        val normalized = normalize(value)
        vocabulary.firstOrNull { it.normalizedName == normalized }?.let { term ->
            return FieldAnalysis(
                fieldName = fieldName,
                resolved = ResolvedSignal(term, emptyList(), fieldName, ExtractionPathV1.EXPLICIT_PRODUCT_IDENTITY),
                candidates = emptyList(),
            )
        }

        val alternatives = value.split(Regex("\\s*(?:/|\\|)\\s*"))
            .map(String::trim)
            .filter(String::isNotEmpty)
        if (alternatives.size > 1) {
            val alternativeTerms = alternatives.mapNotNull { alternative ->
                vocabulary.firstOrNull { it.normalizedName == normalize(alternative) }
            }
            if (alternativeTerms.size == alternatives.size && alternativeTerms.map { it.normalizedName }.distinct().size > 1) {
                return FieldAnalysis(fieldName, null, alternativeTerms.map { it.displayName })
            }
        }

        val leading = value.substringBeforeAny(',', ';')
        val leadingTokens = tokenize(leading)
        val qualifiers = tokenize(value.substringAfterFirst(',', ';'))
            .filterNot { it.normalized in qualifierBoundaries }
        val candidates = headCandidates(leadingTokens, qualifiers, vocabulary, groups)
        if (candidates.isNotEmpty()) {
            val ordered = candidates.sortedWith(
                compareByDescending<HeadCandidate> { it.groupScore }
                    .thenByDescending { it.positionScore }
                    .thenBy { it.term.normalizedName }
                    .thenBy { it.term.displayName },
            )
            val best = ordered.first()
            val ambiguous = ordered.drop(1).any {
                it.groupScore == best.groupScore &&
                    it.positionScore == best.positionScore &&
                    !sameIdentity(best.term, it.term)
            }
            if (!ambiguous) {
                return FieldAnalysis(
                    fieldName,
                    ResolvedSignal(best.term, best.modifiers, fieldName, ExtractionPathV1.CATALOG_COMPOSITION),
                    emptyList(),
                )
            }
            return FieldAnalysis(fieldName, null, ordered.map { it.term.displayName })
        }

        val firstToken = leadingTokens.firstOrNull()
        if (firstToken != null && groupSupports(firstToken.normalized, groups)) {
            val lexical = VocabularyTerm(
                displayName = firstToken.display,
                normalizedName = firstToken.normalized,
                familyKey = null,
                authorityBound = false,
                entityType = null,
                tokens = listOf(firstToken.normalized),
            )
            return FieldAnalysis(
                fieldName,
                ResolvedSignal(
                    lexical,
                    (leadingTokens.drop(1) + qualifiers).map(TextToken::display),
                    fieldName,
                    ExtractionPathV1.GROUP_CONFIRMED_SOURCE_IDENTITY,
                ),
                emptyList(),
            )
        }
        return FieldAnalysis(fieldName, null, emptyList())
    }

    private fun headCandidates(
        tokens: List<TextToken>,
        qualifiers: List<TextToken>,
        vocabulary: List<VocabularyTerm>,
        groups: List<String>,
    ): List<HeadCandidate> {
        if (tokens.isEmpty()) return emptyList()
        val candidates = mutableListOf<HeadCandidate>()
        vocabulary.forEach { term ->
            val termTokens = term.tokens
            if (termTokens.size <= tokens.size && tokens.takeLast(termTokens.size).map(TextToken::normalized) == termTokens) {
                candidates += HeadCandidate(
                    term,
                    tokens.dropLast(termTokens.size).map(TextToken::display) + qualifiers.map(TextToken::display),
                    if (groupSupports(term.normalizedName, groups)) 1 else 0,
                    3,
                )
            }
        }

        if (tokens.size == 1) {
            segmentClosedCompound(tokens.single(), vocabulary).forEach { segmentation ->
                val head = segmentation.last()
                candidates += HeadCandidate(
                    head.term,
                    segmentation.dropLast(1).map { it.term.displayName } + qualifiers.map(TextToken::display),
                    if (groupSupports(head.term.normalizedName, groups)) 1 else 0,
                    3,
                )
            }
        }

        val matching = vocabulary.filter { term ->
            tokens.any { token -> term.tokens.size == 1 && token.normalized == term.tokens.single() }
        }
        if (matching.size == 1) {
            val term = matching.single()
            val remaining = tokens.filterNot { it.normalized == term.normalizedName }
            candidates += HeadCandidate(
                term,
                remaining.map(TextToken::display) + qualifiers.map(TextToken::display),
                if (groupSupports(term.normalizedName, groups)) 1 else 0,
                2,
            )
        }
        return candidates.distinctBy { it.term.normalizedName }
    }

    private fun segmentClosedCompound(
        token: TextToken,
        vocabulary: List<VocabularyTerm>,
    ): List<List<Segment>> {
        val matches = vocabulary.filter { it.tokens.size == 1 && token.normalized.contains(it.tokens.single()) }
            .sortedWith(compareBy<VocabularyTerm> { it.normalizedName.length }.thenBy { it.normalizedName })

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

    private fun vocabulary(
        catalog: HimProductOnlyCanonicalMaster,
        authority: HimCanonicalFamilyAuthority,
    ): List<VocabularyTerm> {
        val terms = buildList {
            val familyByNormalized = authority.families.associateBy { normalize(it.normalizedName) }
            catalog.records.forEach { record ->
                val family = familyByNormalized[normalize(record.normalized)]
                add(VocabularyTerm(record.itemname, family?.canonicalId?.value, family != null, HimEntityType.CANONICAL))
            }
            authority.families.forEach { family ->
                val familyKey = family.canonicalId.value
                add(VocabularyTerm(family.canonicalName, familyKey, true, HimEntityType.CANONICAL))
                family.identities.forEach { identity ->
                    add(VocabularyTerm(identity.identityName, familyKey, true, HimEntityType.IDENTITY))
                    identity.variants.forEach { add(VocabularyTerm(it.variantName, familyKey, true, HimEntityType.VARIANT)) }
                    identity.aliases.forEach { add(VocabularyTerm(it.aliasName, familyKey, true, HimEntityType.ALIAS)) }
                }
                family.variants.forEach { add(VocabularyTerm(it.variantName, familyKey, true, HimEntityType.VARIANT)) }
                family.aliases.forEach { add(VocabularyTerm(it.aliasName, familyKey, true, HimEntityType.ALIAS)) }
            }
        }
        return terms
            .filter { it.normalizedName.isNotEmpty() }
            .groupBy { it.normalizedName }
            .values
            .map { same -> same.sortedWith(compareByDescending<VocabularyTerm> { it.authorityBound }.thenBy { it.displayName }).first() }
            .sortedWith(compareBy<VocabularyTerm> { it.normalizedName }.thenBy { it.displayName })
    }

    private fun sameIdentity(left: VocabularyTerm, right: VocabularyTerm): Boolean =
        left.familyKey != null && left.familyKey == right.familyKey ||
            left.normalizedName == right.normalizedName

    private fun groupSupports(term: String, groups: List<String>): Boolean {
        val pattern = Regex("(^|[^\\p{L}\\p{N}])${Regex.escape(term)}($|[^\\p{L}\\p{N}])")
        return groups.any(pattern::containsMatchIn)
    }

    private fun unresolved(
        record: HimEvidenceRetrievalIndexRecord,
        field: String?,
        candidates: List<String>,
    ) = HimAgribalysePrimaryIdentityExtractionV1(
        sourceRecordIdentity = record.sourceRecordReference.value,
        primaryIdentity = null,
        resolution = HimAgribalysePrimaryIdentityResolutionV1.UNRESOLVED,
        modifiers = emptyList(),
        candidateIdentities = candidates.distinct().sortedBy(::normalize),
        primaryIdentityAuthorityBound = false,
        identityFieldsUsed = listOfNotNull(field),
        extractionPath = ExtractionPathV1.UNRESOLVED.name,
    )

    private fun missing(record: HimEvidenceRetrievalIndexRecord) = HimAgribalysePrimaryIdentityExtractionV1(
        sourceRecordIdentity = record.sourceRecordReference.value,
        primaryIdentity = null,
        resolution = HimAgribalysePrimaryIdentityResolutionV1.MISSING,
        modifiers = emptyList(),
        candidateIdentities = emptyList(),
        primaryIdentityAuthorityBound = false,
        identityFieldsUsed = emptyList(),
        extractionPath = ExtractionPathV1.UNRESOLVED.name,
    )

    private fun JsonElement.stringValue(): String? = takeUnless { it.isJsonNull }
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
        ?.asString

    private fun tokenize(value: String): List<TextToken> = Regex("[\\p{L}\\p{N}]+")
        .findAll(value)
        .map { TextToken(it.value, normalize(it.value)) }
        .filter { it.normalized.isNotEmpty() }
        .toList()

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFC)
        .trim()
        .replace(Regex("\\s+"), " ")
        .lowercase(Locale.ROOT)

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
    private data class ResolvedSignal(val term: VocabularyTerm, val modifiers: List<String>, val fieldName: String, val path: ExtractionPathV1)
    private data class HeadCandidate(val term: VocabularyTerm, val modifiers: List<String>, val groupScore: Int, val positionScore: Int)
    private data class Segment(val term: VocabularyTerm)
    private data class TextToken(val display: String, val normalized: String)
    private data class VocabularyTerm(
        val displayName: String,
        val familyKey: String?,
        val authorityBound: Boolean,
        val entityType: HimEntityType?,
        val tokens: List<String> = normalize(displayName).split(Regex("[^\\p{L}\\p{N}]+"))
            .filter(String::isNotEmpty),
        val normalizedName: String = normalize(displayName),
    )

    private enum class ExtractionPathV1 {
        EXPLICIT_PRODUCT_IDENTITY,
        CATALOG_COMPOSITION,
        GROUP_CONFIRMED_SOURCE_IDENTITY,
        UNRESOLVED,
    }

    private const val MAX_COMPOUND_SEGMENTATIONS = 32
}

enum class HimAgribalysePrimaryIdentityResolutionV1 {
    RESOLVED,
    MISSING,
    UNRESOLVED,
}

data class HimAgribalysePrimaryIdentityExtractionV1(
    val sourceRecordIdentity: String,
    val primaryIdentity: String?,
    val resolution: HimAgribalysePrimaryIdentityResolutionV1,
    val modifiers: List<String>,
    val candidateIdentities: List<String>,
    val primaryIdentityAuthorityBound: Boolean,
    val identityFieldsUsed: List<String>,
    val extractionPath: String,
) {
    init {
        require(sourceRecordIdentity.isNotBlank())
        when (resolution) {
            HimAgribalysePrimaryIdentityResolutionV1.RESOLVED -> require(!primaryIdentity.isNullOrBlank())
            HimAgribalysePrimaryIdentityResolutionV1.MISSING,
            HimAgribalysePrimaryIdentityResolutionV1.UNRESOLVED,
            -> require(primaryIdentity == null)
        }
        require(candidateIdentities.distinct().size == candidateIdentities.size)
        require(modifiers.all(String::isNotBlank))
        require(identityFieldsUsed.distinct().size == identityFieldsUsed.size)
        require(extractionPath.isNotBlank())
    }

    fun toAlignmentInput(): HimEvidenceAlignmentInputV1 = HimEvidenceAlignmentInputV1(
        sourceRecordIdentity = sourceRecordIdentity,
        primaryIdentity = primaryIdentity,
        primaryIdentityState = when (resolution) {
            HimAgribalysePrimaryIdentityResolutionV1.RESOLVED -> HimPrimaryIdentityStateV1.RESOLVED
            HimAgribalysePrimaryIdentityResolutionV1.MISSING -> HimPrimaryIdentityStateV1.MISSING
            HimAgribalysePrimaryIdentityResolutionV1.UNRESOLVED -> HimPrimaryIdentityStateV1.UNRESOLVED
        },
        modifiers = modifiers,
    )
}
