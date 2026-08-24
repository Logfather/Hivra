package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceRelation

/**
 * Source-neutral orchestration for the four deterministic source adapters.
 * Extractors never receive the target family; only the alignment contract does.
 */
object HimDeterministicEvidenceAlignmentEvaluatorV1 {
    const val VERSION = "HIM_DETERMINISTIC_EVIDENCE_ALIGNMENT_EVALUATOR_V1"

    fun evaluate(
        record: HimEvidenceRetrievalIndexRecord,
        catalog: HimProductOnlyCanonicalMaster,
        authority: HimCanonicalFamilyAuthority,
        canonicalFamily: HimCanonicalFamily,
        claimedEvidenceRelation: HimSemanticEvidenceRelation? = null,
    ): HimDeterministicEvidenceAlignmentEvaluationV1 = align(
        extract(record, catalog, authority),
        canonicalFamily,
        claimedEvidenceRelation,
    )

    fun evaluateAll(
        records: List<HimEvidenceRetrievalIndexRecord>,
        catalog: HimProductOnlyCanonicalMaster,
        authority: HimCanonicalFamilyAuthority,
        canonicalFamily: HimCanonicalFamily,
        claimedEvidenceRelation: HimSemanticEvidenceRelation? = null,
    ): List<HimDeterministicEvidenceAlignmentEvaluationV1> = records
        .map { record -> evaluate(record, catalog, authority, canonicalFamily, claimedEvidenceRelation) }
        .sortedWith(
            compareBy<HimDeterministicEvidenceAlignmentEvaluationV1> { it.source.name }
                .thenBy { it.recordKind.name }
                .thenBy { it.sourceRecordIdentity },
        )

    fun extract(
        record: HimEvidenceRetrievalIndexRecord,
        catalog: HimProductOnlyCanonicalMaster,
        authority: HimCanonicalFamilyAuthority,
    ): HimDeterministicEvidenceExtractionV1 {
        val extraction = runCatching { extractSupported(record, catalog, authority) }
            .getOrElse { null }
        return extraction?.let {
            HimDeterministicEvidenceExtractionV1(
                source = record.sourceRecordReference.source,
                recordKind = record.recordKind,
                sourceRecordIdentity = record.sourceRecordReference.value,
                primaryIdentity = it.primaryIdentity,
                modifiers = it.modifiers,
                candidateIdentities = it.candidateIdentities,
                extractionPath = it.extractionPath,
                alignmentInput = it.alignmentInput,
                failureReason = null,
            )
        } ?: HimDeterministicEvidenceExtractionV1(
            source = record.sourceRecordReference.source,
            recordKind = record.recordKind,
            sourceRecordIdentity = record.sourceRecordReference.value,
            primaryIdentity = null,
            modifiers = emptyList(),
            candidateIdentities = emptyList(),
            extractionPath = if (isSupported(record.recordKind)) INVALID_OR_INCOMPLETE_PROJECTION else UNSUPPORTED_RECORD_KIND,
            alignmentInput = unresolvedInput(record.sourceRecordReference.value),
            failureReason = if (isSupported(record.recordKind)) INVALID_OR_INCOMPLETE_PROJECTION else UNSUPPORTED_RECORD_KIND,
        )
    }

    fun align(
        extraction: HimDeterministicEvidenceExtractionV1,
        canonicalFamily: HimCanonicalFamily,
        claimedEvidenceRelation: HimSemanticEvidenceRelation? = null,
    ): HimDeterministicEvidenceAlignmentEvaluationV1 {

        val alignment = HimEvidenceAlignmentContractV1.evaluate(
            extraction.alignmentInput.copy(claimedEvidenceRelation = claimedEvidenceRelation),
            canonicalFamily,
        )
        return HimDeterministicEvidenceAlignmentEvaluationV1(
            source = extraction.source,
            recordKind = extraction.recordKind,
            sourceRecordIdentity = extraction.sourceRecordIdentity,
            primaryIdentity = extraction.primaryIdentity,
            modifiers = extraction.modifiers,
            candidateIdentities = extraction.candidateIdentities,
            extractionPath = extraction.extractionPath,
            alignment = alignment,
            failureReason = extraction.failureReason,
        )
    }

    private fun extractSupported(
        record: HimEvidenceRetrievalIndexRecord,
        catalog: HimProductOnlyCanonicalMaster,
        authority: HimCanonicalFamilyAuthority,
    ): Extracted? = when (record.recordKind) {
        HimEvidenceRecordKind.OFF_PRODUCT -> HimOffDeterministicPrimaryIdentityExtractorV1
            .extract(record, catalog, authority).let { extraction ->
                Extracted(
                    extraction.primaryIdentity,
                    extraction.modifiers,
                    extraction.candidateIdentities,
                    extraction.extractionPath,
                    extraction.toAlignmentInput(),
                )
            }
        HimEvidenceRecordKind.AGRIBALYSE_RECORD -> HimAgribalyseDeterministicPrimaryIdentityExtractorV1
            .extract(record, catalog, authority).let { extraction ->
                Extracted(
                    extraction.primaryIdentity,
                    extraction.modifiers,
                    extraction.candidateIdentities,
                    extraction.extractionPath,
                    extraction.toAlignmentInput(),
                )
            }
        HimEvidenceRecordKind.CIQUAL_FOOD -> HimCiqualDeterministicPrimaryIdentityExtractorV1
            .extract(record, catalog, authority).let { extraction ->
                Extracted(
                    extraction.primaryIdentity,
                    extraction.modifiers,
                    extraction.candidateIdentities,
                    extraction.extractionPath,
                    extraction.toAlignmentInput(),
                )
            }
        HimEvidenceRecordKind.GI_MEASUREMENT -> HimGlycemicIndexDeterministicPrimaryIdentityExtractorV1
            .extract(record, catalog, authority).let { extraction ->
                Extracted(
                    extraction.primaryIdentity,
                    extraction.modifiers,
                    extraction.candidateIdentities,
                    extraction.extractionPath,
                    extraction.toAlignmentInput(),
                )
            }
        else -> null
    }

    private fun unresolvedInput(sourceRecordIdentity: String) = HimEvidenceAlignmentInputV1(
        sourceRecordIdentity = sourceRecordIdentity,
        primaryIdentity = null,
        primaryIdentityState = HimPrimaryIdentityStateV1.UNRESOLVED,
    )

    private fun isSupported(recordKind: HimEvidenceRecordKind): Boolean = recordKind in setOf(
        HimEvidenceRecordKind.OFF_PRODUCT,
        HimEvidenceRecordKind.AGRIBALYSE_RECORD,
        HimEvidenceRecordKind.CIQUAL_FOOD,
        HimEvidenceRecordKind.GI_MEASUREMENT,
    )

    private data class Extracted(
        val primaryIdentity: String?,
        val modifiers: List<String>,
        val candidateIdentities: List<String>,
        val extractionPath: String,
        val alignmentInput: HimEvidenceAlignmentInputV1,
    )

    private const val UNSUPPORTED_RECORD_KIND = "UNSUPPORTED_RECORD_KIND"
    private const val INVALID_OR_INCOMPLETE_PROJECTION = "INVALID_OR_INCOMPLETE_PROJECTION"
}

data class HimDeterministicEvidenceExtractionV1(
    val source: HimGroundTruthSource,
    val recordKind: HimEvidenceRecordKind,
    val sourceRecordIdentity: String,
    val primaryIdentity: String?,
    val modifiers: List<String>,
    val candidateIdentities: List<String>,
    val extractionPath: String,
    val alignmentInput: HimEvidenceAlignmentInputV1,
    val failureReason: String?,
) {
    init {
        require(recordKind.source == source)
        require(sourceRecordIdentity == alignmentInput.sourceRecordIdentity)
        require(primaryIdentity == alignmentInput.primaryIdentity)
        require(modifiers == alignmentInput.modifiers)
        require(sourceRecordIdentity.isNotBlank())
        require(extractionPath.isNotBlank())
    }
}

data class HimDeterministicEvidenceAlignmentEvaluationV1(
    val source: HimGroundTruthSource,
    val recordKind: HimEvidenceRecordKind,
    val sourceRecordIdentity: String,
    val primaryIdentity: String?,
    val modifiers: List<String>,
    val candidateIdentities: List<String>,
    val extractionPath: String,
    val alignment: HimEvidenceAlignmentResultV1,
    val failureReason: String?,
) {
    init {
        require(recordKind.source == source)
        require(sourceRecordIdentity == alignment.sourceRecordIdentity)
        require(primaryIdentity == alignment.primaryIdentity)
        require(modifiers == alignment.modifiers)
        require(sourceRecordIdentity.isNotBlank())
        require(extractionPath.isNotBlank())
    }

    val directEvidenceSupported: Boolean
        get() = alignment.directEvidenceSupported

    val effectiveEvidenceRelation: HimSemanticEvidenceRelation?
        get() = alignment.effectiveEvidenceRelation

    val uncoveredModifiers: List<String>
        get() = alignment.uncoveredModifiers

    val rationale: String
        get() = when {
            failureReason != null -> failureReason
            alignment.directEvidenceSupported -> "PRIMARY_IDENTITY_ALIGNED"
            alignment.classification == HimEvidenceAlignmentClassificationV1.CANONICAL_ONLY_AS_MODIFIER -> "CANONICAL_ONLY_AS_MODIFIER"
            alignment.classification == HimEvidenceAlignmentClassificationV1.UNRESOLVED_PRIMARY_IDENTITY -> "PRIMARY_IDENTITY_UNRESOLVED"
            else -> "PRIMARY_IDENTITY_NOT_ALIGNED"
        }
}
