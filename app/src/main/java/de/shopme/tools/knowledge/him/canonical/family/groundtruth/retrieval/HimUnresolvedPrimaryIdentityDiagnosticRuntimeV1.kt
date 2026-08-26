package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.io.File

data class HimUnresolvedPrimaryIdentityDiagnosticMissionInputV1(
    val analysisReport: HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1,
    val analysisBinding: HimUnresolvedPrimaryIdentityDiagnosticFileBindingV1,
    val enrichmentMission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
    val enrichmentMissionBinding: HimUnresolvedPrimaryIdentityDiagnosticFileBindingV1,
    val enrichmentAggregate: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1,
    val enrichmentAggregateBinding: HimUnresolvedPrimaryIdentityDiagnosticFileBindingV1,
    val sourceBindings: List<HimEvidenceAlignmentCatalogAuditSourceBindingV1>,
    val diagnosticImplementationHead: String,
)

interface HimUnresolvedPrimaryIdentityDiagnosticExactFetchPortV1 {
    val source: HimGroundTruthSource

    fun fetchExact(
        source: HimGroundTruthSource,
        evidenceReference: HimEvidenceRecordReference,
    ): HimEvidenceRetrievalIndexRecord?
}

data class HimUnresolvedPrimaryIdentityDiagnosticRuntimeRequestV1(
    val enabled: Boolean,
    val mission: HimUnresolvedPrimaryIdentityDiagnosticMissionV1,
    val shardId: String,
    val catalog: HimProductOnlyCanonicalMaster,
    val authority: HimCanonicalFamilyAuthority,
    val fetchPorts: List<HimUnresolvedPrimaryIdentityDiagnosticExactFetchPortV1>,
    val outputFile: File? = null,
)

sealed interface HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult<out T> {
    data class Completed<T>(val value: T) : HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult<T>
    data class Skipped(val reason: String) : HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult<Nothing>
    data class Failed(
        val reason: HimUnresolvedPrimaryIdentityDiagnosticFailureReasonV1,
        val safeContext: String,
    ) : HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult<Nothing>
}

object HimUnresolvedPrimaryIdentityDiagnosticRuntimeV1 {
    private val supportedKinds = setOf(
        HimEvidenceRecordKind.OFF_PRODUCT,
        HimEvidenceRecordKind.AGRIBALYSE_RECORD,
        HimEvidenceRecordKind.CIQUAL_FOOD,
        HimEvidenceRecordKind.GI_MEASUREMENT,
    )

    fun planMission(
        input: HimUnresolvedPrimaryIdentityDiagnosticMissionInputV1,
    ): HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult<HimUnresolvedPrimaryIdentityDiagnosticMissionV1> = try {
        input.analysisReport.validate()
        input.enrichmentMission.validate()
        input.enrichmentAggregate.validateAgainst(input.enrichmentMission)
        input.sourceBindings.forEach { it.validate() }
        require(input.sourceBindings.map { it.source } == HimUnresolvedPrimaryIdentityDiagnosticContractV1.SOURCE_ORDER)
        require(input.sourceBindings == input.enrichmentMission.provenance.auditBindings.sourceBindings)
        require(input.analysisBinding.relativePath.isNotBlank())
        require(input.enrichmentMissionBinding.logicalDigest == input.enrichmentMission.logicalDigest)
        require(input.enrichmentAggregateBinding.logicalDigest == input.enrichmentAggregate.logicalDigest)
        require(input.diagnosticImplementationHead.matches(HEAD))

        val selected = input.analysisReport.findingIndex.filter { finding ->
            finding.originalAuditClassification == HimEvidenceAlignmentAuditClassification.UNRESOLVED_PRIMARY_IDENTITY &&
                finding.originalEvidenceReference != null &&
                finding.reconstructionStatus == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.RECONSTRUCTED &&
                finding.recordKind in supportedKinds
        }.map { it.toOccurrence() }
        require(selected.map { it.findingOccurrenceId }.distinct().size == selected.size)

        val referencePlans = selected.groupBy { it.source to it.evidenceReference }
            .map { (key, occurrences) ->
                val ordered = occurrences.sortedWith(
                    compareBy<HimUnresolvedPrimaryIdentityDiagnosticOccurrenceV1> { it.auditShardId }
                        .thenBy { it.entityId }
                        .thenBy { it.findingOccurrenceId },
                )
                HimUnresolvedPrimaryIdentityDiagnosticReferencePlanV1(
                    source = key.first,
                    evidenceReference = key.second,
                    ownerShardId = HimUnresolvedPrimaryIdentityDiagnosticContractV1.expectedOwner(ordered),
                    occurrences = ordered,
                )
            }.sortedWith(compareBy({ HimUnresolvedPrimaryIdentityDiagnosticContractV1.SOURCE_ORDER.indexOf(it.source) }, { it.evidenceReference }))

        val sourceCounts = HimUnresolvedPrimaryIdentityDiagnosticContractV1.SOURCE_ORDER.map { source ->
            HimUnresolvedPrimaryIdentityDiagnosticSourceReferenceCountV1(
                source,
                referencePlans.count { it.source == source },
            )
        }
        val occurrenceCount = selected.size
        val canonicalCount = selected.map { it.entityId }.distinct().size
        val inputBindingUnsigned = HimUnresolvedPrimaryIdentityDiagnosticInputBindingV1(
            analysis = input.analysisBinding,
            enrichmentMission = input.enrichmentMissionBinding,
            enrichmentAggregate = input.enrichmentAggregateBinding,
            auditHead = input.analysisReport.inputBinding.auditHead,
            enrichmentImplementationHead = input.enrichmentMission.provenance.enrichmentImplementationHead,
            analysisImplementationHead = input.analysisReport.inputBinding.analysisImplementationHead,
            expectedOccurrenceCount = occurrenceCount,
            expectedReferenceCount = referencePlans.size,
            expectedCanonicalCount = canonicalCount,
            sourceReferenceCounts = sourceCounts,
            bindingDigest = "",
        )
        val inputBinding = inputBindingUnsigned.copy(
            bindingDigest = HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.bindingDigest(inputBindingUnsigned),
        )
        val shards = HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS.map { shardId ->
            HimUnresolvedPrimaryIdentityDiagnosticShardPlanV1(
                shardId = shardId,
                referencePlanKeys = referencePlans.filter { it.ownerShardId == shardId }.map { it.key }.sorted(),
            )
        }
        val unsigned = HimUnresolvedPrimaryIdentityDiagnosticMissionV1(
            contractId = HimUnresolvedPrimaryIdentityDiagnosticContractV1.VERSION,
            inputBinding = inputBinding,
            auditHead = inputBinding.auditHead,
            enrichmentImplementationHead = inputBinding.enrichmentImplementationHead,
            analysisImplementationHead = inputBinding.analysisImplementationHead,
            diagnosticImplementationHead = input.diagnosticImplementationHead,
            analysisBinding = input.analysisBinding,
            enrichmentMissionBinding = input.enrichmentMissionBinding,
            enrichmentAggregateBinding = input.enrichmentAggregateBinding,
            sourceBindings = input.sourceBindings,
            expectedOccurrenceCount = occurrenceCount,
            expectedReferenceCount = referencePlans.size,
            expectedCanonicalCount = canonicalCount,
            reusedReferenceCount = referencePlans.count { it.occurrences.size > 1 },
            reusedOccurrenceCount = referencePlans.filter { it.occurrences.size > 1 }.sumOf { it.occurrences.size },
            missingIdentityCount = selected.count { it.originalExtractorResolution == "MISSING" },
            zeroCandidateCount = selected.count { it.candidateIdentities.isEmpty() },
            singleCandidateCount = selected.count { it.candidateIdentities.size == 1 },
            multipleCandidateCount = selected.count { it.candidateIdentities.size >= 2 },
            referencePlans = referencePlans,
            shards = shards,
            logicalDigest = "",
        )
        val mission = unsigned.copy(logicalDigest = HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.logicalDigest(unsigned))
        mission.validate()
        HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Completed(mission)
    } catch (_: Throwable) {
        HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Failed(
            HimUnresolvedPrimaryIdentityDiagnosticFailureReasonV1.INPUT_BINDING_MISMATCH,
            safeContext(null, null, null, null, null),
        )
    }

    fun executeShard(
        request: HimUnresolvedPrimaryIdentityDiagnosticRuntimeRequestV1,
    ): HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult<HimUnresolvedPrimaryIdentityDiagnosticShardResultV1> {
        if (!request.enabled) return HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Skipped("DIAGNOSTIC_OPT_IN_REQUIRED")
        return try {
            request.mission.validate()
            require(request.shardId in HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS)
            require(request.fetchPorts.map { it.source } == HimUnresolvedPrimaryIdentityDiagnosticContractV1.SOURCE_ORDER)
            val plan = request.mission.shards.single { it.shardId == request.shardId }
            val byKey = request.mission.referencePlans.associateBy { it.key }
            val ports = request.fetchPorts.associateBy { it.source }
            val records = plan.referencePlanKeys.map { key ->
                val referencePlan = byKey[key] ?: fail(
                    HimUnresolvedPrimaryIdentityDiagnosticFailureReasonV1.REFERENCE_PLAN_MISMATCH,
                    request.shardId, null, null, null, null,
                )
                val reference = try {
                    HimEvidenceRecordReference.parse(referencePlan.source, referencePlan.evidenceReference)
                } catch (_: Throwable) {
                    fail(HimUnresolvedPrimaryIdentityDiagnosticFailureReasonV1.REFERENCE_PLAN_MISMATCH, request.shardId, referencePlan.source, referencePlan.evidenceReference, null, null)
                }
                val record = ports[referencePlan.source]?.fetchExact(referencePlan.source, reference)
                    ?: fail(HimUnresolvedPrimaryIdentityDiagnosticFailureReasonV1.EXACT_FETCH_FAILED, request.shardId, referencePlan.source, referencePlan.evidenceReference, null, null)
                if (record.sourceRecordReference != reference) {
                    fail(HimUnresolvedPrimaryIdentityDiagnosticFailureReasonV1.RECORD_KIND_MISMATCH, request.shardId, referencePlan.source, referencePlan.evidenceReference, null, null)
                }
                if (record.recordKind !in supportedKinds || record.recordKind.source != referencePlan.source ||
                    referencePlan.occurrences.any { it.originalRecordKind != record.recordKind }) {
                    fail(HimUnresolvedPrimaryIdentityDiagnosticFailureReasonV1.RECORD_KIND_MISMATCH, request.shardId, referencePlan.source, referencePlan.evidenceReference, null, null)
                }
                val extraction = HimDeterministicEvidenceAlignmentEvaluatorV1.extract(record, request.catalog, request.authority)
                if (extraction.failureReason != null) {
                    fail(HimUnresolvedPrimaryIdentityDiagnosticFailureReasonV1.PROJECTION_INVALID, request.shardId, referencePlan.source, referencePlan.evidenceReference, null, null)
                }
                val fields = fields(record)
                val candidates = referencePlan.occurrences.flatMap { it.candidateIdentities }.distinct().sorted()
                val status = status(fields, candidates)
                HimUnresolvedPrimaryIdentityDiagnosticRecordV1(
                    source = referencePlan.source,
                    evidenceReference = referencePlan.evidenceReference,
                    recordKind = record.recordKind,
                    ownerShardId = request.shardId,
                    occurrences = referencePlan.occurrences,
                    canonicalEntityIds = referencePlan.occurrences.map { it.entityId }.distinct().sorted(),
                    fields = fields,
                    candidateIdentities = candidates,
                    originalExtractorResolutions = referencePlan.occurrences.mapNotNull { it.originalExtractorResolution }.distinct().sorted(),
                    originalIdentityFieldsUsed = referencePlan.occurrences.flatMap { it.originalIdentityFieldUsed }.distinct().sorted(),
                    originalExtractionPaths = referencePlan.occurrences.mapNotNull { it.originalExtractionPath }.distinct().sorted(),
                    originalReasonCodes = referencePlan.occurrences.mapNotNull { it.originalReasonCode }.distinct().sorted(),
                    status = status,
                )
            }
            val unsigned = HimUnresolvedPrimaryIdentityDiagnosticShardResultV1(
                missionDigest = request.mission.logicalDigest,
                shardId = request.shardId,
                state = HimUnresolvedPrimaryIdentityDiagnosticResultStateV1.COMPLETE,
                records = records,
                counters = deriveCounters(records),
                logicalDigest = "",
            )
            val result = unsigned.copy(logicalDigest = HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.logicalDigest(unsigned))
            result.validateAgainst(request.mission)
            request.outputFile?.let { output ->
                HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.writeShard(output, request.mission, result)
                HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Completed(
                    HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.readShard(output, request.mission),
                )
            } ?: HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Completed(result)
        } catch (failure: DiagnosticFailure) {
            HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Failed(failure.reason, failure.context)
        } catch (_: Throwable) {
            HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Failed(
                HimUnresolvedPrimaryIdentityDiagnosticFailureReasonV1.COUNTER_INVARIANT_FAILED,
                safeContext(request.shardId, null, null, null, null),
            )
        }
    }

    internal fun deriveCounters(records: List<HimUnresolvedPrimaryIdentityDiagnosticRecordV1>) =
        HimUnresolvedPrimaryIdentityDiagnosticCountersV1(
            unresolvedOccurrences = records.sumOf { it.occurrences.size },
            canonicalTargets = records.flatMap { it.canonicalEntityIds }.distinct().size,
            uniqueEvidenceReferences = records.size,
            exactFetches = records.size,
            recordsLoaded = records.size,
            identityFieldsPresent = records.count { it.fields.any { field -> !field.lexicalFeatures.empty } },
            recordsWithNoIdentityFields = records.count { it.fields.isEmpty() },
            recordsWithOnlyEmptyIdentityFields = records.count { it.fields.isNotEmpty() && it.fields.all { field -> field.lexicalFeatures.empty } },
            zeroCandidateRecords = records.count { it.candidateIdentities.isEmpty() },
            singleCandidateRecords = records.count { it.candidateIdentities.size == 1 },
            multipleCandidateRecords = records.count { it.candidateIdentities.size >= 2 },
            perSourceReferences = HimUnresolvedPrimaryIdentityDiagnosticContractV1.SOURCE_ORDER.map { source ->
                HimUnresolvedPrimaryIdentityDiagnosticSourceCounterV1(source, records.count { it.source == source })
            },
            technicalErrors = 0,
        )

    private fun HimEvidenceAlignmentCatalogAuditFindingsAnalysisFindingIndexEntryV1.toOccurrence() =
        HimUnresolvedPrimaryIdentityDiagnosticOccurrenceV1(
            findingOccurrenceId = findingOccurrenceId,
            auditShardId = auditShardId,
            entityId = entityId,
            canonicalName = canonicalName,
            normalizedName = normalizedName,
            source = source,
            evidenceReference = requireNotNull(originalEvidenceReference),
            originalAuditClassification = originalAuditClassification,
            originalRecordKind = requireNotNull(recordKind),
            candidateIdentities = candidateIdentities,
            originalExtractorResolution = extractorResolution,
            originalIdentityFieldUsed = identityFieldsUsed,
            originalExtractionPath = extractionPath,
            originalReasonCode = reconstructedReasonCode,
        )

    private fun fields(record: HimEvidenceRetrievalIndexRecord): List<HimUnresolvedPrimaryIdentityDiagnosticFieldV1> {
        val projection = JsonParser.parseString(record.evidenceProjection.deterministicJson).asJsonObject
        val values = when (record.recordKind) {
            HimEvidenceRecordKind.OFF_PRODUCT -> offFields(projection)
            HimEvidenceRecordKind.AGRIBALYSE_RECORD -> agribalyseFields(projection)
            HimEvidenceRecordKind.CIQUAL_FOOD -> ciqualFields(projection)
            HimEvidenceRecordKind.GI_MEASUREMENT -> giFields(projection)
            else -> emptyList()
        }
        return values.map { (path, value, language, role) ->
            val trimmed = value.trim()
            HimUnresolvedPrimaryIdentityDiagnosticFieldV1(
                fieldPath = path,
                originalLexicalValue = value,
                trimmedValue = trimmed,
                language = language,
                role = role,
                source = record.sourceRecordReference.source,
                recordKind = record.recordKind,
                lexicalFeatures = lexicalFeatures(value, role == HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.BRAND_CONTEXT),
            )
        }.sortedWith(compareBy({ it.fieldPath }, { it.originalLexicalValue }))
    }

    private fun offFields(root: JsonObject): List<RawField> {
        val identity = root.getAsJsonObject("identity")
        val result = mutableListOf<RawField>()
        result += scalar(identity, "productName", "identity.productName", HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.UNSPECIFIED, HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.PRIMARY_IDENTITY_CANDIDATE)
        result += scalar(identity, "productNameGerman", "identity.productNameGerman", HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.DE, HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.PRIMARY_IDENTITY_CANDIDATE)
        result += scalar(identity, "productNameEnglish", "identity.productNameEnglish", HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.EN, HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.PRIMARY_IDENTITY_CANDIDATE)
        result += scalar(identity, "genericName", "identity.genericName", HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.UNSPECIFIED, HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.GENERIC_IDENTITY_CANDIDATE)
        result += scalar(identity, "genericNameGerman", "identity.genericNameGerman", HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.DE, HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.GENERIC_IDENTITY_CANDIDATE)
        result += scalar(identity, "genericNameEnglish", "identity.genericNameEnglish", HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.EN, HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.GENERIC_IDENTITY_CANDIDATE)
        result += strings(identity, "brands", "identity.brands", HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.UNSPECIFIED, HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.BRAND_CONTEXT)
        result += scalar(identity, "productType", "identity.productType", HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.UNSPECIFIED, HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.OTHER_IDENTITY_CONTEXT)
        val taxonomy = root.getAsJsonObject("taxonomy")
        listOf("categories", "categoryHierarchy", "foodGroups", "pnnsGroups", "mainCategory").forEach { name ->
            result += strings(taxonomy, name, "taxonomy.$name", HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.UNSPECIFIED, HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.CATEGORY_CONTEXT)
        }
        return result
    }

    private fun agribalyseFields(root: JsonObject) = listOf(
        raw(root, "productNameFr", HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.FR, HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.PRIMARY_IDENTITY_CANDIDATE),
        raw(root, "lciName", HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.EN, HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.PRIMARY_IDENTITY_CANDIDATE),
        raw(root, "foodGroup", HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.UNSPECIFIED, HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.CATEGORY_CONTEXT),
        raw(root, "foodSubgroup", HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.UNSPECIFIED, HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.CATEGORY_CONTEXT),
    ).flatten()

    private fun ciqualFields(root: JsonObject) = buildList {
        addAll(
            raw(
                root,
                "nameFr",
                HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.FR,
                HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.PRIMARY_IDENTITY_CANDIDATE,
            ),
        )
        addAll(
            raw(
                root,
                "nameEn",
                HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.EN,
                HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.PRIMARY_IDENTITY_CANDIDATE,
            ),
        )
        addAll(
            rawLexical(
                root,
                "scientificName",
                HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.SCIENTIFIC,
                HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.SCIENTIFIC_CONTEXT,
            ),
        )
        listOf(
            "groupNameFr",
            "subgroupNameFr",
            "subSubgroupNameFr",
        ).forEach { name ->
            addAll(
                raw(
                    root,
                    name,
                    HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.FR,
                    HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.CATEGORY_CONTEXT,
                ),
            )
        }
        listOf(
            "groupNameEn",
            "subgroupNameEn",
            "subSubgroupNameEn",
        ).forEach { name ->
            addAll(
                raw(
                    root,
                    name,
                    HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.EN,
                    HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.CATEGORY_CONTEXT,
                ),
            )
        }
    }

    private fun giFields(root: JsonObject) = buildList {
        addAll(rawLexical(root, "foodItem", HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.UNSPECIFIED, HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.PRIMARY_IDENTITY_CANDIDATE))
        val context = root.getAsJsonObject("sourceContext")
        listOf("majorCategory", "subcategory", "deeperHeading").forEach { name ->
            addAll(raw(context, name, HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.UNSPECIFIED, HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.CATEGORY_CONTEXT))
        }
    }

    private fun raw(root: JsonObject?, name: String, language: HimUnresolvedPrimaryIdentityDiagnosticLanguageV1, role: HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1): List<RawField> =
        root?.get(name)?.let { element ->
            if (element.isJsonPrimitive && element.asJsonPrimitive.isString) listOf(RawField(name, element.asString, language, role)) else emptyList()
        }.orEmpty()

    private fun rawLexical(root: JsonObject?, name: String, language: HimUnresolvedPrimaryIdentityDiagnosticLanguageV1, role: HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1): List<RawField> =
        root?.getAsJsonObject(name)?.get("lexicalValue")?.takeUnless { it.isJsonNull }?.let { listOf(RawField(name, it.asString, language, role)) }.orEmpty()

    private fun scalar(root: JsonObject?, name: String, path: String, language: HimUnresolvedPrimaryIdentityDiagnosticLanguageV1, role: HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1) =
        raw(root, name, language, role).map { it.copy(path = path) }

    private fun strings(root: JsonObject?, name: String, path: String, language: HimUnresolvedPrimaryIdentityDiagnosticLanguageV1, role: HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1): List<RawField> =
        root?.get(name)?.let { element -> stringValues(element).map { RawField(path, it, language, role) } }.orEmpty()

    private fun stringValues(element: JsonElement): List<String> = when {
        element.isJsonPrimitive && element.asJsonPrimitive.isString -> listOf(element.asString)
        element.isJsonArray -> element.asJsonArray.flatMap(::stringValues)
        else -> emptyList()
    }

    private fun lexicalFeatures(value: String, brand: Boolean) = HimUnresolvedPrimaryIdentityDiagnosticLexicalFeaturesV1(
        empty = value.trim().isEmpty(),
        characterLength = value.length,
        tokenCount = value.trim().takeIf(String::isNotEmpty)?.split(Regex("\\s+")).orEmpty().size,
        distinctTokenCount = value.trim().takeIf(String::isNotEmpty)?.split(Regex("\\s+")).orEmpty().distinct().size,
        containsDigits = value.any(Char::isDigit),
        containsParentheses = '(' in value || ')' in value,
        containsHyphen = '-' in value,
        containsComma = ',' in value,
        containsSlash = '/' in value,
        containsBrandContext = brand,
    )

    private fun status(fields: List<HimUnresolvedPrimaryIdentityDiagnosticFieldV1>, candidates: List<String>) = when {
        fields.isEmpty() -> HimUnresolvedPrimaryIdentityDiagnosticStatusV1.IDENTITY_FIELDS_MISSING
        fields.all { it.lexicalFeatures.empty } -> HimUnresolvedPrimaryIdentityDiagnosticStatusV1.IDENTITY_FIELDS_EMPTY
        candidates.isEmpty() -> HimUnresolvedPrimaryIdentityDiagnosticStatusV1.IDENTITY_FIELDS_PRESENT_NO_CANDIDATE
        candidates.size == 1 -> HimUnresolvedPrimaryIdentityDiagnosticStatusV1.SINGLE_CANDIDATE_UNRESOLVED
        else -> HimUnresolvedPrimaryIdentityDiagnosticStatusV1.MULTIPLE_CANDIDATES_UNRESOLVED
    }

    private data class RawField(
        val path: String,
        val value: String,
        val language: HimUnresolvedPrimaryIdentityDiagnosticLanguageV1,
        val role: HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1,
    )

    private class DiagnosticFailure(
        val reason: HimUnresolvedPrimaryIdentityDiagnosticFailureReasonV1,
        val context: String,
    ) : RuntimeException()

    private fun fail(
        reason: HimUnresolvedPrimaryIdentityDiagnosticFailureReasonV1,
        shardId: String?,
        source: HimGroundTruthSource?,
        reference: String?,
        occurrenceId: String?,
        entityId: String?,
    ): Nothing = throw DiagnosticFailure(reason, safeContext(shardId, source, reference, occurrenceId, entityId))

    private fun safeContext(
        shardId: String?,
        source: HimGroundTruthSource?,
        reference: String?,
        occurrenceId: String?,
        entityId: String?,
    ): String = buildList {
        shardId?.let { add("shard=$it") }
        source?.let { add("source=${it.name}") }
        reference?.let { add("evidenceReference=$it") }
        occurrenceId?.let { add("findingOccurrenceId=$it") }
        entityId?.let { add("entityId=$it") }
    }.joinToString(" ").ifEmpty { "diagnostic" }
}

private val HEAD = Regex("[0-9a-f]{40}")
