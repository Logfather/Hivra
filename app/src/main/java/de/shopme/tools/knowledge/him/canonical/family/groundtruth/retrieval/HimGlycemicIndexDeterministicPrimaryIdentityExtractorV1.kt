package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import java.text.Normalizer
import java.util.Locale

/**
 * Deterministic GI-measurement-only primary-identity extraction.
 *
 * The food item remains source-faithful evidence. Category context is used
 * only as a controlled fallback; target-family alignment remains the sole
 * owner of DIRECT-relation decisions.
 */
object HimGlycemicIndexDeterministicPrimaryIdentityExtractorV1 {
    const val VERSION = "HIM_GLYCEMIC_INDEX_DETERMINISTIC_PRIMARY_IDENTITY_EXTRACTOR_V1"

    private val qualifierBoundaries = setOf(
        "à", "a", "and", "avec", "aux", "au", "de", "des", "du", "flavored", "flavoured",
        "mit", "of", "und", "with",
    )
    private val contextWords = qualifierBoundaries + setOf("la", "le", "les", "the")

    fun extract(
        record: HimEvidenceRetrievalIndexRecord,
        catalog: HimProductOnlyCanonicalMaster,
        authority: HimCanonicalFamilyAuthority,
    ): HimGlycemicIndexPrimaryIdentityExtractionV1 {
        require(record.recordKind == HimEvidenceRecordKind.GI_MEASUREMENT)
        require(record.sourceRecordReference.source == de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource.GLYCEMIC_INDEX)

        val projection = JsonParser.parseString(record.evidenceProjection.deterministicJson).asJsonObject
        val vocabulary = vocabulary(catalog, authority)
        val foodItem = projection.getAsJsonObject("foodItem")?.get("lexicalValue")?.stringValue()?.trim().orEmpty()
        val sourceContext = projection.getAsJsonObject("sourceContext")
        val categorySignals = listOf(
            ContextField("sourceContext.deeperHeading", sourceContext?.get("deeperHeading")?.stringValue()),
            ContextField("sourceContext.subcategory", sourceContext?.get("subcategory")?.stringValue()),
            ContextField("sourceContext.majorCategory", sourceContext?.get("majorCategory")?.stringValue()),
        ).mapNotNull { field ->
            field.value?.trim()?.takeIf(String::isNotEmpty)?.let { raw ->
                vocabulary.firstOrNull { it.normalizedName == normalize(raw) }?.let { term ->
                    ContextSignal(field.name, raw, term)
                }
            }
        }
        val category = categorySignals.firstOrNull()
        val categoryConflict = category != null && categorySignals.drop(1).any { !sameIdentity(category.term, it.term) }
        val product = analyseFoodItem(foodItem, vocabulary, category?.term)

        if (categoryConflict) {
            return unresolved(record, listOfNotNull("foodItem.lexicalValue", category?.fieldName), product.candidates)
        }

        val resolved = product.resolved
        if (resolved != null) {
            if (category != null && !sameIdentity(resolved.term, category.term)) {
                return unresolved(
                    record,
                    listOf("foodItem.lexicalValue", category.fieldName),
                    product.candidates + listOf(resolved.term.displayName, category.raw),
                )
            }
            return resolved(
                record,
                listOf("foodItem.lexicalValue"),
                resolved.copy(modifiers = product.modifiers),
            )
        }

        if (foodItem.isNotBlank() && category != null) {
            return categoryBacked(
                record,
                category,
                product.modifiers,
                listOf("foodItem.lexicalValue", category.fieldName),
            )
        }

        return if (foodItem.isBlank()) {
            unresolved(record, listOfNotNull(category?.fieldName), emptyList())
        } else {
            unresolved(record, listOf("foodItem.lexicalValue"), product.candidates)
        }
    }

    private fun analyseFoodItem(
        value: String,
        vocabulary: List<VocabularyTerm>,
        category: VocabularyTerm?,
    ): FoodAnalysis {
        if (value.isBlank()) return FoodAnalysis(null, emptyList(), emptyList())
        val withoutParentheses = value.replace(Regex("\\([^)]*\\)"), " ").trim()
        vocabulary.firstOrNull { it.normalizedName == normalize(value) || it.normalizedName == normalize(withoutParentheses) }?.let { term ->
            return FoodAnalysis(
                ResolvedSignal(term, ExtractionPathV1.EXPLICIT_PRODUCT_IDENTITY),
                emptyList(),
                listOf(term.displayName),
            )
        }

        val tokens = tokenize(withoutParentheses)
        val boundary = tokens.indexOfFirst { it.normalized in qualifierBoundaries }
        val headTokens = if (boundary >= 0) tokens.take(boundary) else tokens
        val suffixTokens = buildList {
            if (boundary >= 0) addAll(tokens.drop(boundary + 1))
            addAll(tokenize(value.substringAfterFirst(',', ';')))
        }
        val candidates = occurrences(headTokens, vocabulary)
        val chosen = choose(candidates, category)
        if (chosen != null) {
            val modifiers = (headTokens.filterIndexed { index, _ -> index !in chosen.start until chosen.start + chosen.length } + suffixTokens)
                .map(TextToken::display)
                .filterNot { it.lowercase(Locale.ROOT) in contextWords }
            return FoodAnalysis(
                ResolvedSignal(chosen.term, ExtractionPathV1.CATALOG_COMPOSITION),
                modifiers,
                candidates.map { it.term.displayName },
            )
        }

        val modifiers = candidates
            .filter { category == null || !sameIdentity(category, it.term) }
            .sortedWith(compareBy<FoodCandidate> { it.start }.thenByDescending { it.length }.thenBy { it.term.normalizedName })
            .map { it.term.displayName }
            .distinctBy(::normalize)
        return FoodAnalysis(null, modifiers, candidates.map { it.term.displayName })
    }

    private fun occurrences(tokens: List<TextToken>, vocabulary: List<VocabularyTerm>): List<FoodCandidate> =
        vocabulary.flatMap { term ->
            val termTokens = term.tokens
            if (termTokens.size > tokens.size) return@flatMap emptyList()
            (0..tokens.size - termTokens.size).mapNotNull { start ->
                if (tokens.subList(start, start + termTokens.size).map(TextToken::normalized) == termTokens) {
                    FoodCandidate(term, start, termTokens.size)
                } else null
            }
        }.distinctBy { it.term.normalizedName }

    private fun choose(candidates: List<FoodCandidate>, category: VocabularyTerm?): FoodCandidate? {
        if (candidates.isEmpty()) return null
        val categoryMatches = category?.let { target -> candidates.filter { sameIdentity(target, it.term) } }.orEmpty()
        if (categoryMatches.size == 1) return categoryMatches.single()
        if (categoryMatches.size > 1) return chooseLast(categoryMatches)
        if (candidates.size == 1) return candidates.single().takeIf { it.start == 0 }
        return chooseLast(candidates)
    }

    private fun chooseLast(candidates: List<FoodCandidate>): FoodCandidate? {
        val last = candidates.maxWithOrNull(compareBy<FoodCandidate> { it.start }.thenBy { it.length }.thenBy { it.term.normalizedName }) ?: return null
        val ties = candidates.filter { it.start == last.start && it.length == last.length && !sameIdentity(it.term, last.term) }
        return last.takeUnless { ties.isNotEmpty() }
    }

    private fun categoryBacked(
        record: HimEvidenceRetrievalIndexRecord,
        category: ContextSignal,
        modifiers: List<String>,
        fields: List<String>,
    ) = HimGlycemicIndexPrimaryIdentityExtractionV1(
        sourceRecordIdentity = record.sourceRecordReference.value,
        primaryIdentity = category.raw,
        resolution = HimGlycemicIndexPrimaryIdentityResolutionV1.RESOLVED,
        modifiers = modifiers.distinctBy(::normalize),
        candidateIdentities = listOf(category.raw),
        primaryIdentityAuthorityBound = category.term.authorityBound,
        identityFieldsUsed = fields.distinct(),
        extractionPath = ExtractionPathV1.CATEGORY_BACKED.name,
    )

    private fun resolved(
        record: HimEvidenceRetrievalIndexRecord,
        fields: List<String>,
        signal: ResolvedSignal,
    ) = HimGlycemicIndexPrimaryIdentityExtractionV1(
        sourceRecordIdentity = record.sourceRecordReference.value,
        primaryIdentity = signal.term.displayName,
        resolution = HimGlycemicIndexPrimaryIdentityResolutionV1.RESOLVED,
        modifiers = signal.modifiers.distinctBy(::normalize),
        candidateIdentities = listOf(signal.term.displayName),
        primaryIdentityAuthorityBound = signal.term.authorityBound,
        identityFieldsUsed = fields.distinct(),
        extractionPath = signal.path.name,
    )

    private fun unresolved(
        record: HimEvidenceRetrievalIndexRecord,
        fields: List<String>,
        candidates: List<String>,
    ) = HimGlycemicIndexPrimaryIdentityExtractionV1(
        sourceRecordIdentity = record.sourceRecordReference.value,
        primaryIdentity = null,
        resolution = HimGlycemicIndexPrimaryIdentityResolutionV1.UNRESOLVED,
        modifiers = emptyList(),
        candidateIdentities = candidates.distinct().sortedBy(::normalize),
        primaryIdentityAuthorityBound = false,
        identityFieldsUsed = fields.distinct(),
        extractionPath = ExtractionPathV1.UNRESOLVED.name,
    )

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

    private fun JsonElement.stringValue(): String? = takeUnless { it.isJsonNull }
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
        ?.asString

    private fun tokenize(value: String): List<TextToken> = Regex("[\\p{L}\\p{N}]+").findAll(value)
        .map { TextToken(it.value, normalize(it.value)) }.filter { it.normalized.isNotEmpty() }.toList()

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFC)
        .trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

    private fun String.substringAfterFirst(vararg delimiters: Char): String {
        val index = delimiters.map { indexOf(it) }.filter { it >= 0 }.minOrNull() ?: length
        return if (index == length) "" else substring(index + 1)
    }

    private data class ContextField(val name: String, val value: String?)
    private data class ContextSignal(val fieldName: String, val raw: String, val term: VocabularyTerm)
    private data class FoodAnalysis(val resolved: ResolvedSignal?, val modifiers: List<String>, val candidates: List<String>)
    private data class ResolvedSignal(
        val term: VocabularyTerm,
        val path: ExtractionPathV1,
        val modifiers: List<String> = emptyList(),
    )
    private data class FoodCandidate(val term: VocabularyTerm, val start: Int, val length: Int)
    private data class TextToken(val display: String, val normalized: String)
    private data class VocabularyTerm(
        val displayName: String,
        val familyKey: String?,
        val authorityBound: Boolean,
        val entityType: HimEntityType?,
        val normalizedName: String = normalize(displayName),
        val tokens: List<String> = normalizedName.split(Regex("[^\\p{L}\\p{N}]+"))
            .filter(String::isNotEmpty),
    )
    private enum class ExtractionPathV1 { EXPLICIT_PRODUCT_IDENTITY, CATALOG_COMPOSITION, CATEGORY_BACKED, UNRESOLVED }
}

enum class HimGlycemicIndexPrimaryIdentityResolutionV1 { RESOLVED, MISSING, UNRESOLVED }

data class HimGlycemicIndexPrimaryIdentityExtractionV1(
    val sourceRecordIdentity: String,
    val primaryIdentity: String?,
    val resolution: HimGlycemicIndexPrimaryIdentityResolutionV1,
    val modifiers: List<String>,
    val candidateIdentities: List<String>,
    val primaryIdentityAuthorityBound: Boolean,
    val identityFieldsUsed: List<String>,
    val extractionPath: String,
) {
    init {
        require(sourceRecordIdentity.isNotBlank())
        when (resolution) {
            HimGlycemicIndexPrimaryIdentityResolutionV1.RESOLVED -> require(!primaryIdentity.isNullOrBlank())
            HimGlycemicIndexPrimaryIdentityResolutionV1.MISSING,
            HimGlycemicIndexPrimaryIdentityResolutionV1.UNRESOLVED -> require(primaryIdentity == null)
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
            HimGlycemicIndexPrimaryIdentityResolutionV1.RESOLVED -> HimPrimaryIdentityStateV1.RESOLVED
            HimGlycemicIndexPrimaryIdentityResolutionV1.MISSING -> HimPrimaryIdentityStateV1.MISSING
            HimGlycemicIndexPrimaryIdentityResolutionV1.UNRESOLVED -> HimPrimaryIdentityStateV1.UNRESOLVED
        },
        modifiers = modifiers,
    )
}
