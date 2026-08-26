package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource

/**
 * Offline analysis of materialized diagnostic fields. The result describes
 * observable signals only; it does not assert why an extractor found no
 * candidate. No source, catalog, authority, retrieval, or tokenizer is used.
 */
object HimZeroCandidateCauseAnalysisRuntimeV1 {
    private val primaryRoles = setOf(
        HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.PRIMARY_IDENTITY_CANDIDATE,
        HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.GENERIC_IDENTITY_CANDIDATE,
    )
    private val contextRoles = setOf(
        HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.CATEGORY_CONTEXT,
        HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.BRAND_CONTEXT,
        HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.SCIENTIFIC_CONTEXT,
    )
    private val localizedPaths = mapOf(
        HimGroundTruthSource.OPEN_FOOD_FACTS to setOf("identity.productNameEnglish"),
        HimGroundTruthSource.AGRIBALYSE to setOf("productNameFr"),
        HimGroundTruthSource.CIQUAL to setOf("nameFr", "nameEn"),
        HimGroundTruthSource.GLYCEMIC_INDEX to emptySet(),
    )
    private val neutralPaths = mapOf(
        HimGroundTruthSource.OPEN_FOOD_FACTS to setOf(
            "identity.productName",
            "identity.productNameGerman",
            "identity.genericName",
            "identity.genericNameGerman",
        ),
        HimGroundTruthSource.AGRIBALYSE to setOf("lciName"),
        HimGroundTruthSource.CIQUAL to emptySet(),
        HimGroundTruthSource.GLYCEMIC_INDEX to setOf("foodItem"),
    )

    fun analyze(
        request: HimZeroCandidateCauseAnalysisRuntimeRequestV1,
    ): HimZeroCandidateCauseAnalysisRuntimeResult<HimZeroCandidateCauseAnalysisReportV1> {
        if (!request.enabled) return HimZeroCandidateCauseAnalysisRuntimeResult.Skipped("ANALYSIS_OPT_IN_REQUIRED")
        return try {
            val aggregate = normalizeAggregate(
                request.aggregate,
                request.mission,
            )
            validateInput(request, aggregate)
            val sourceRecords = aggregate.shardResults
                .flatMap { it.records }
                .filter { it.status == HimUnresolvedPrimaryIdentityDiagnosticStatusV1.IDENTITY_FIELDS_PRESENT_NO_CANDIDATE }
            require(sourceRecords.size == request.aggregate.counters.zeroCandidateRecords) {
                fail(HimZeroCandidateCauseAnalysisFailureReasonV1.ZERO_CANDIDATE_COUNT_MISMATCH, "zeroCandidateRecords")
            }
            require(sourceRecords.all { it.candidateIdentities.isEmpty() }) {
                fail(HimZeroCandidateCauseAnalysisFailureReasonV1.ZERO_CANDIDATE_COUNT_MISMATCH, "candidateIdentities")
            }
            require(sourceRecords.map { it.source to it.evidenceReference }.distinct().size == sourceRecords.size) {
                fail(HimZeroCandidateCauseAnalysisFailureReasonV1.DUPLICATE_EVIDENCE_REFERENCE, "sourceReference")
            }
            val records = sourceRecords.map(::analyzeRecord).sortedWith(analysisRecordComparator)
            val reportUnsigned = buildReport(request, records)
            val report = reportUnsigned.copy(
                logicalDigest = HimZeroCandidateCauseAnalysisPersistenceV1.logicalDigest(reportUnsigned),
            )
            report.validate()
            if (request.jsonOutputFile != null || request.textOutputFile != null) {
                require(request.jsonOutputFile != null && request.textOutputFile != null) {
                    fail(HimZeroCandidateCauseAnalysisFailureReasonV1.PERSISTENCE_FAILED, "outputPair")
                }
                try {
                    HimZeroCandidateCauseAnalysisPersistenceV1.writeReport(
                        request.jsonOutputFile!!,
                        request.textOutputFile!!,
                        report,
                    )
                } catch (_: Throwable) {
                    fail(HimZeroCandidateCauseAnalysisFailureReasonV1.PERSISTENCE_FAILED, "writeReport")
                }
            }
            HimZeroCandidateCauseAnalysisRuntimeResult.Completed(report)
        } catch (failure: AnalysisFailure) {
            HimZeroCandidateCauseAnalysisRuntimeResult.Failed(failure.reason, failure.context)
        } catch (_: Throwable) {
            HimZeroCandidateCauseAnalysisRuntimeResult.Failed(
                HimZeroCandidateCauseAnalysisFailureReasonV1.REPORT_VALIDATION_FAILED,
                "report",
            )
        }
    }

    private fun validateInput(
        request: HimZeroCandidateCauseAnalysisRuntimeRequestV1,
        aggregate: HimUnresolvedPrimaryIdentityDiagnosticAggregateV1,
    ) {
        try {
            request.inputBinding.validate()
        } catch (_: Throwable) {
            fail(HimZeroCandidateCauseAnalysisFailureReasonV1.INPUT_BINDING_MISMATCH, "inputBinding")
        }
        try {
            request.mission.validate()
        } catch (_: Throwable) {
            fail(HimZeroCandidateCauseAnalysisFailureReasonV1.MISSION_VALIDATION_FAILED, "mission")
        }
        try {
            aggregate.validateAgainst(request.mission)
        } catch (_: Throwable) {
            fail(HimZeroCandidateCauseAnalysisFailureReasonV1.AGGREGATE_VALIDATION_FAILED, "aggregate")
        }
        require(aggregate.missionDigest == request.mission.logicalDigest) {
            fail(HimZeroCandidateCauseAnalysisFailureReasonV1.AGGREGATE_BINDING_MISMATCH, "missionDigest")
        }
        require(request.inputBinding.diagnosticMission.logicalDigest == request.mission.logicalDigest) {
            fail(HimZeroCandidateCauseAnalysisFailureReasonV1.MISSION_BINDING_MISMATCH, "missionDigest")
        }
        require(request.inputBinding.diagnosticAggregate.logicalDigest == aggregate.logicalDigest) {
            fail(HimZeroCandidateCauseAnalysisFailureReasonV1.AGGREGATE_BINDING_MISMATCH, "aggregateDigest")
        }
    }

    private fun normalizeAggregate(
        aggregate: HimUnresolvedPrimaryIdentityDiagnosticAggregateV1,
        mission: HimUnresolvedPrimaryIdentityDiagnosticMissionV1,
    ): HimUnresolvedPrimaryIdentityDiagnosticAggregateV1 {
        val orderedShards = aggregate.shardResults.sortedBy {
            HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS.indexOf(it.shardId)
        }
        require(
            orderedShards.map { it.shardId } ==
                    HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS,
        ) {
            fail(
                HimZeroCandidateCauseAnalysisFailureReasonV1.AGGREGATE_VALIDATION_FAILED,
                "shards",
            )
        }

        val normalizedShards = orderedShards.map { shard ->
            val plan = mission.shards.singleOrNull { it.shardId == shard.shardId }
                ?: fail(
                    HimZeroCandidateCauseAnalysisFailureReasonV1.AGGREGATE_VALIDATION_FAILED,
                    "shardPlan",
                )

            val recordsByKey = shard.records.associateBy { record ->
                HimUnresolvedPrimaryIdentityDiagnosticContractV1.referenceKey(
                    record.source,
                    record.evidenceReference,
                )
            }
            require(recordsByKey.size == shard.records.size) {
                fail(
                    HimZeroCandidateCauseAnalysisFailureReasonV1.AGGREGATE_VALIDATION_FAILED,
                    "duplicateShardReference",
                )
            }

            val records = plan.referencePlanKeys.map { key ->
                recordsByKey[key]
                    ?: fail(
                        HimZeroCandidateCauseAnalysisFailureReasonV1.AGGREGATE_VALIDATION_FAILED,
                        "missingShardReference",
                    )
            }
            require(records.size == shard.records.size) {
                fail(
                    HimZeroCandidateCauseAnalysisFailureReasonV1.AGGREGATE_VALIDATION_FAILED,
                    "foreignShardReference",
                )
            }

            val unsigned = shard.copy(
                records = records,
                logicalDigest = "",
            )
            unsigned.copy(
                logicalDigest =
                    HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.logicalDigest(unsigned),
            )
        }

        val unsigned = aggregate.copy(
            shardResults = normalizedShards,
            logicalDigest = "",
        )
        return unsigned.copy(
            logicalDigest =
                HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.logicalDigest(unsigned),
        )
    }

    private fun analyzeRecord(
        source: HimUnresolvedPrimaryIdentityDiagnosticRecordV1,
    ): HimZeroCandidateCauseAnalysisRecordV1 {
        source.fields.forEach { field ->
            require(field.source == source.source && field.recordKind == source.recordKind) {
                fail(HimZeroCandidateCauseAnalysisFailureReasonV1.UNSUPPORTED_FIELD_SHAPE, "fieldBinding")
            }
            require(field.trimmedValue == field.originalLexicalValue.trim()) {
                fail(HimZeroCandidateCauseAnalysisFailureReasonV1.UNSUPPORTED_FIELD_SHAPE, "trimmedValue")
            }
            when (field.role) {
                HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.PRIMARY_IDENTITY_CANDIDATE,
                HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.GENERIC_IDENTITY_CANDIDATE,
                HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.CATEGORY_CONTEXT,
                HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.BRAND_CONTEXT,
                HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.SCIENTIFIC_CONTEXT,
                HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.OTHER_IDENTITY_CONTEXT -> Unit
            }
        }
        val usable = source.fields.filter { !it.lexicalFeatures.empty && it.trimmedValue.isNotEmpty() }
        val primary = usable.filter { it.role in primaryRoles }
        val context = usable.filter { it.role in contextRoles }
        val bucket = when {
            usable.isEmpty() -> HimZeroCandidateCauseAnalysisPrimaryBucketV1.NO_USABLE_DIAGNOSTIC_FIELDS
            primary.isEmpty() && context.isNotEmpty() -> HimZeroCandidateCauseAnalysisPrimaryBucketV1.CONTEXT_ONLY
            primary.isNotEmpty() && primary.all { it.lexicalFeatures.containsBrandContext } ->
                HimZeroCandidateCauseAnalysisPrimaryBucketV1.BRAND_CONTEXT_DOMINATED
            primary.isNotEmpty() && primary.all { it.fieldPath in localizedPaths.getValue(source.source) } &&
                primary.none { it.fieldPath in neutralPaths.getValue(source.source) } ->
                HimZeroCandidateCauseAnalysisPrimaryBucketV1.LOCALIZED_PRIMARY_ONLY
            primary.any { it.lexicalFeatures.containsDigits } ->
                HimZeroCandidateCauseAnalysisPrimaryBucketV1.STRUCTURED_ALPHANUMERIC_PRIMARY
            primary.isNotEmpty() -> HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH
            context.isEmpty() -> HimZeroCandidateCauseAnalysisPrimaryBucketV1.UNCLASSIFIED
            else -> HimZeroCandidateCauseAnalysisPrimaryBucketV1.CONTEXT_ONLY
        }
        val flags = buildList {
            if (primary.isNotEmpty()) add(HimZeroCandidateCauseAnalysisFlagV1.HAS_PRIMARY_IDENTITY_FIELD)
            if (primary.size > 1) add(HimZeroCandidateCauseAnalysisFlagV1.HAS_MULTIPLE_PRIMARY_IDENTITY_FIELDS)
            if (usable.any { it.role == HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.BRAND_CONTEXT }) add(HimZeroCandidateCauseAnalysisFlagV1.HAS_BRAND_CONTEXT)
            if (usable.any { it.role == HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.CATEGORY_CONTEXT }) add(HimZeroCandidateCauseAnalysisFlagV1.HAS_CATEGORY_CONTEXT)
            if (primary.isEmpty() && context.isNotEmpty()) add(HimZeroCandidateCauseAnalysisFlagV1.HAS_ONLY_CONTEXT_FIELDS)
            if (primary.any { it.fieldPath in localizedPaths.getValue(source.source) }) add(HimZeroCandidateCauseAnalysisFlagV1.HAS_EXPLICIT_LOCALIZED_FIELD)
            if (primary.any { it.fieldPath in neutralPaths.getValue(source.source) }) add(HimZeroCandidateCauseAnalysisFlagV1.HAS_SOURCE_NEUTRAL_PRIMARY_FIELD)
            if (primary.any { it.lexicalFeatures.containsDigits }) add(HimZeroCandidateCauseAnalysisFlagV1.HAS_DIGITS_OR_CODE_SIGNAL)
            if (primary.any { it.originalLexicalValue.any { char -> char.code > 127 } }) add(HimZeroCandidateCauseAnalysisFlagV1.HAS_NON_ASCII_TEXT)
            if (primary.any { it.lexicalFeatures.tokenCount > 1 || it.lexicalFeatures.distinctTokenCount > 1 }) add(HimZeroCandidateCauseAnalysisFlagV1.HAS_MULTIPLE_NORMALIZED_TOKENS)
            if (primary.any { it.lexicalFeatures.containsHyphen || it.lexicalFeatures.containsComma || it.lexicalFeatures.containsSlash }) add(HimZeroCandidateCauseAnalysisFlagV1.HAS_TOKEN_SEPARATOR_SIGNAL)
            if (primary.any { it.lexicalFeatures.tokenCount > 1 || it.lexicalFeatures.containsHyphen || it.lexicalFeatures.containsComma || it.lexicalFeatures.containsSlash }) add(HimZeroCandidateCauseAnalysisFlagV1.POSSIBLE_COMPOUND_OR_TOKEN_BOUNDARY_REVIEW)
            if (source.occurrences.size > 1) add(HimZeroCandidateCauseAnalysisFlagV1.HAS_MULTIPLE_OCCURRENCES)
            if (source.canonicalEntityIds.size > 1) add(HimZeroCandidateCauseAnalysisFlagV1.HAS_MULTIPLE_CANONICAL_TARGETS)
        }
        return HimZeroCandidateCauseAnalysisRecordV1(
            source = source.source,
            evidenceReference = source.evidenceReference,
            recordKind = source.recordKind,
            ownerShardId = source.occurrences.minOf { it.auditShardId },
            findingOccurrenceIds = source.occurrences.map { it.findingOccurrenceId }.sorted(),
            canonicalEntityIds = source.canonicalEntityIds,
            originalStatus = source.status,
            candidateIdentities = source.candidateIdentities,
            fields = source.fields,
            primaryValues = primary.map { it.originalLexicalValue }.distinct().sorted(),
            primaryBucket = bucket,
            flags = flags,
        )
    }

    private fun buildReport(
        request: HimZeroCandidateCauseAnalysisRuntimeRequestV1,
        records: List<HimZeroCandidateCauseAnalysisRecordV1>,
    ): HimZeroCandidateCauseAnalysisReportV1 {
        val bucketCounters = HimZeroCandidateCauseAnalysisContractV1.BUCKET_ORDER.map { bucket ->
            HimZeroCandidateCauseAnalysisBucketCounterV1(bucket, records.count { it.primaryBucket == bucket })
        }
        val sourceBucket = HimZeroCandidateCauseAnalysisContractV1.SOURCE_ORDER.flatMap { source ->
            HimZeroCandidateCauseAnalysisContractV1.BUCKET_ORDER.map { bucket ->
                HimZeroCandidateCauseAnalysisSourceBucketBreakdownV1(source, bucket, records.count { it.source == source && it.primaryBucket == bucket })
            }
        }
        val sourceFlags = HimZeroCandidateCauseAnalysisContractV1.SOURCE_ORDER.flatMap { source ->
            HimZeroCandidateCauseAnalysisContractV1.FLAG_ORDER.map { flag ->
                HimZeroCandidateCauseAnalysisSourceFlagBreakdownV1(source, flag, records.count { it.source == source && flag in it.flags })
            }
        }
        val kindBuckets = HimZeroCandidateCauseAnalysisContractV1.RECORD_KIND_ORDER.flatMap { kind ->
            HimZeroCandidateCauseAnalysisContractV1.BUCKET_ORDER.map { bucket ->
                HimZeroCandidateCauseAnalysisRecordKindBucketBreakdownV1(kind, bucket, records.count { it.recordKind == kind && it.primaryBucket == bucket })
            }
        }
        val flagCounters = HimZeroCandidateCauseAnalysisContractV1.FLAG_ORDER.map { flag ->
            HimZeroCandidateCauseAnalysisFlagCounterV1(flag, records.count { flag in it.flags })
        }
        val groups = records.flatMap { record ->
            record.primaryValues.map { value -> value to (record.source to record.evidenceReference) }
        }.groupBy({ it.first }, { it.second })
            .mapNotNull { (key, refs) ->
                val members = refs.distinct().sortedWith(compareBy({ HimZeroCandidateCauseAnalysisContractV1.sourceIndex(it.first) }, { it.second }))
                if (members.size < 2) null else HimZeroCandidateCauseAnalysisRecurringPrimaryValueGroupV1(
                    key,
                    members.map { HimZeroCandidateCauseAnalysisReferenceMemberV1(it.first, it.second) },
                )
            }.sortedWith(compareByDescending<HimZeroCandidateCauseAnalysisRecurringPrimaryValueGroupV1> { it.references.size }.thenBy { it.key })
        return HimZeroCandidateCauseAnalysisReportV1(
            contractId = HimZeroCandidateCauseAnalysisContractV1.VERSION,
            inputBinding = request.inputBinding,
            counters = HimZeroCandidateCauseAnalysisTotalCountersV1(
                records = records.size,
                zeroCandidateRecords = records.size,
                uniqueEvidenceReferences = records.size,
                canonicalTargets = records.flatMap { it.canonicalEntityIds }.distinct().size,
            ),
            bucketCounters = bucketCounters,
            sourceBucketBreakdown = sourceBucket,
            sourceFlagBreakdown = sourceFlags,
            recordKindBucketBreakdown = kindBuckets,
            flagCounters = flagCounters,
            recurringPrimaryValueGroups = groups,
            recurringTokenGroups = emptyList(),
            records = records,
            logicalDigest = "",
        )
    }

    private val diagnosticRecordComparator =
        compareBy<HimUnresolvedPrimaryIdentityDiagnosticRecordV1>(
            { HimZeroCandidateCauseAnalysisContractV1.sourceIndex(it.source) },
            { it.evidenceReference },
        )

    private val analysisRecordComparator =
        compareBy<HimZeroCandidateCauseAnalysisRecordV1>(
            { HimZeroCandidateCauseAnalysisContractV1.sourceIndex(it.source) },
            { it.evidenceReference },
        )

    private class AnalysisFailure(
        val reason: HimZeroCandidateCauseAnalysisFailureReasonV1,
        val context: String,
    ) : IllegalArgumentException()

    private fun fail(reason: HimZeroCandidateCauseAnalysisFailureReasonV1, context: String): Nothing =
        throw AnalysisFailure(reason, context)
}
