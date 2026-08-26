package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalAlias
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistryEntry
import de.shopme.tools.knowledge.him.canonical.family.HimEntitySourceReferenceType
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonical
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticLanguageV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticLexicalFeaturesV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticStatusV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisBucketCounterV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisFlagCounterV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisFlagV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisPrimaryBucketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisRecordKindBucketBreakdownV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisRecurringPrimaryValueGroupV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisReferenceMemberV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisReportV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisSourceBucketBreakdownV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisSourceFlagBreakdownV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisTotalCountersV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewAssociationStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusReportV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusRuntimeRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusRuntimeResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusRuntimeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewStateV1
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryReviewCorpusV1Test {

    @Test
    fun disabledRuntimeIsSkipped() {
        assertIs<HimZeroCandidateRecoveryReviewCorpusRuntimeResult.Skipped>(
            execute(enabled = false),
        )
    }

    @Test
    fun exactUnionOfBothSelectorsIsMaterialized() {
        val report = completed()
        assertEquals(9, report.entries.size)
        assertEquals(9, report.counters.selectedUniqueRecords)
    }

    @Test
    fun globalDeduplicationKeepsOneEntryPerReference() {
        val report = completed()
        assertEquals(
            9,
            report.entries
                .map { it.source to it.evidenceReference }
                .distinct()
                .size,
        )
    }

    @Test
    fun multipleGroupMembershipsRemainOnEntry() {
        val entry = completed().entries.first { it.evidenceReference.endsWith("code1") }
        assertEquals(listOf("alpha", "alpha-copy"), entry.recurringGroupMemberships.map { it.primaryValue })
    }

    @Test
    fun uniqueNonCiqualRecordIsExcluded() {
        assertTrue(completed().entries.none { it.evidenceReference.endsWith("code11") })
    }

    @Test
    fun uniqueCiqualLocalizedRecordIsIncluded() {
        assertTrue(completed().entries.any { it.evidenceReference == ciqual(10) })
    }

    @Test
    fun p1PriorityIsMaterialized() {
        assertEquals(2, completed().priorityCounters[0].groups)
    }

    @Test
    fun p2PriorityIsMaterialized() {
        assertEquals(1, completed().priorityCounters[1].groups)
    }

    @Test
    fun p3PriorityIsMaterialized() {
        assertEquals(1, completed().priorityCounters[2].groups)
    }

    @Test
    fun p4PriorityIsMaterialized() {
        assertEquals(1, completed().priorityCounters[3].groups)
    }

    @Test
    fun groupWithoutTargetFailsClosed() {
        val records = causeRecords().map { record ->
            if (record.evidenceReference == off(1) || record.evidenceReference == off(2)) {
                record.copy(canonicalEntityIds = emptyList())
            } else {
                record
            }
        }
        val broken = causeReport(records = records, groups = listOf(group("empty", 1, 2, emptyList())))
        assertReason(execute(cause = broken), HimZeroCandidateRecoveryReviewFailureReasonV1.INVALID_PRIORITY_CLASSIFICATION)
    }

    @Test
    fun unknownGroupReferenceFailsClosed() {
        val broken = causeReport(groups = listOf(
            HimZeroCandidateCauseAnalysisRecurringPrimaryValueGroupV1(
                "unknown",
                listOf(
                    HimZeroCandidateCauseAnalysisReferenceMemberV1(HimGroundTruthSource.OPEN_FOOD_FACTS, off(1)),
                    HimZeroCandidateCauseAnalysisReferenceMemberV1(HimGroundTruthSource.OPEN_FOOD_FACTS, off(99)),
                ),
            ),
        ))
        assertReason(execute(cause = broken), HimZeroCandidateRecoveryReviewFailureReasonV1.UNKNOWN_GROUP_REFERENCE)
    }

    @Test
    fun duplicateEvidenceReferenceFailsClosed() {
        val record = causeRecords().first()
        assertFailsWith<IllegalArgumentException> {
            causeReport(records = causeRecords() + record)
        }
    }

    @Test
    fun canonicalTargetsResolveToReadableAuthorityNames() {
        val target = completed().entries.first().auditLinkedCanonicalTargets.first()
        assertEquals("Canonical 1", target.canonicalName)
    }

    @Test
    fun missingCanonicalTargetIsTyped() {
        val broken = causeReport(records = causeRecords().map { record ->
            if (record.evidenceReference == off(1)) record.copy(canonicalEntityIds = listOf("c99999")) else record
        })
        assertReason(execute(cause = broken), HimZeroCandidateRecoveryReviewFailureReasonV1.CANONICAL_TARGET_NOT_FOUND)
    }

    @Test
    fun ambiguousCanonicalTargetIsTyped() {
        val duplicateAuthority = authority().copy(families = authority().families + authority().families.first())
        assertReason(execute(authority = duplicateAuthority), HimZeroCandidateRecoveryReviewFailureReasonV1.CATALOG_VALIDATION_FAILED)
    }

    @Test
    fun associationStateIsUnverified() {
        assertTrue(completed().entries.all {
            it.associationState == HimZeroCandidateRecoveryReviewAssociationStateV1.UNVERIFIED_AUDIT_ASSOCIATION
        })
    }

    @Test
    fun reviewStateIsUnreviewed() {
        assertTrue(completed().entries.all {
            it.reviewState == HimZeroCandidateRecoveryReviewStateV1.UNREVIEWED
        })
    }

    @Test
    fun noTrainingLabelIsPresent() {
        assertEquals(0, completed().counters.confirmedRecords)
    }

    @Test
    fun countersAndBreakdownsAreComplete() {
        val report = completed()
        assertEquals(report.entries.size, report.sourceBreakdown.sumOf { it.entries })
        assertEquals(report.entries.size, report.recordKindBreakdown.sumOf { it.entries })
        assertEquals(2, report.selectionReasonBreakdown.size)
    }

    @Test
    fun changedInputBindingFailsClosed() {
        val broken = inputBinding().copy(recoveryReviewImplementationHead = "b".repeat(40))
        assertReason(execute(inputBinding = broken), HimZeroCandidateRecoveryReviewFailureReasonV1.INPUT_BINDING_MISMATCH)
    }

    @Test
    fun causeAnalysisBindingMismatchFailsClosed() {
        val cause = causeReport()
        val original = inputBinding(cause)
        val foreignLogicalDigest = "f".repeat(64)

        val unsigned = original.copy(
            causeAnalysis = original.causeAnalysis.copy(
                logicalDigest = foreignLogicalDigest,
            ),
            causeAnalysisLogicalDigest = foreignLogicalDigest,
            bindingDigest = "",
        )
        val foreignBinding = unsigned.copy(
            bindingDigest =
                HimZeroCandidateRecoveryReviewCorpusPersistenceV1.bindingDigest(
                    unsigned,
                ),
        )

        assertReason(
            execute(
                cause = cause,
                inputBinding = foreignBinding,
            ),
            HimZeroCandidateRecoveryReviewFailureReasonV1.CAUSE_ANALYSIS_BINDING_MISMATCH,
        )
    }

    @Test
    fun outputReloadEqualsOriginalReport() {
        val directory = Files.createTempDirectory("him-recovery-review").toFile()
        val json = directory.resolve("review.json")
        val text = directory.resolve("review.txt")
        val report = completed(jsonOutputFile = json, textOutputFile = text)
        assertEquals(report, HimZeroCandidateRecoveryReviewCorpusPersistenceV1.readReport(json))
    }

    @Test
    fun secondIdenticalWriteRemainsByteIdentical() {
        val directory = Files.createTempDirectory("him-recovery-review").toFile()
        val json = directory.resolve("review.json")
        val text = directory.resolve("review.txt")
        val request = request(jsonOutputFile = json, textOutputFile = text)
        assertIs<HimZeroCandidateRecoveryReviewCorpusRuntimeResult.Completed<*>>(
            HimZeroCandidateRecoveryReviewCorpusRuntimeV1.execute(request),
        )
        val before = json.readBytes()
        assertIs<HimZeroCandidateRecoveryReviewCorpusRuntimeResult.Completed<*>>(
            HimZeroCandidateRecoveryReviewCorpusRuntimeV1.execute(request),
        )
        assertTrue(before.contentEquals(json.readBytes()))
    }

    @Test
    fun conflictingOverwriteIsRejected() {
        val directory = Files.createTempDirectory("him-recovery-review").toFile()
        val json = directory.resolve("review.json")
        val text = directory.resolve("review.txt")
        val request = request(jsonOutputFile = json, textOutputFile = text)
        HimZeroCandidateRecoveryReviewCorpusRuntimeV1.execute(request)
        json.appendText("x")
        assertReason(
            HimZeroCandidateRecoveryReviewCorpusRuntimeV1.execute(request),
            HimZeroCandidateRecoveryReviewFailureReasonV1.PERSISTENCE_FAILED,
        )
    }

    @Test
    fun safeFailureContainsStableContextOnly() {
        val failure = execute(inputBinding = inputBinding().copy(recoveryReviewImplementationHead = "b".repeat(40)))
        val result = assertIs<HimZeroCandidateRecoveryReviewCorpusRuntimeResult.Failed>(failure)
        assertEquals("inputBinding", result.safeContext)
        assertFalse(result.safeContext.contains('/'))
    }

    @Test
    fun requestHasNoRetrievalPort() {
        val instanceFields =
            HimZeroCandidateRecoveryReviewCorpusRuntimeRequestV1::class.java
                .declaredFields
                .filterNot { field ->
                    java.lang.reflect.Modifier.isStatic(field.modifiers)
                }

        assertEquals(
            mapOf(
                "enabled" to java.lang.Boolean.TYPE,
                "causeAnalysis" to
                        HimZeroCandidateCauseAnalysisReportV1::class.java,
                "catalog" to
                        HimProductOnlyCanonicalMaster::class.java,
                "registry" to
                        HimEntityIdRegistry::class.java,
                "authority" to
                        HimCanonicalFamilyAuthority::class.java,
                "inputBinding" to
                        HimZeroCandidateRecoveryReviewCorpusInputBindingV1::class.java,
                "jsonOutputFile" to java.io.File::class.java,
                "textOutputFile" to java.io.File::class.java,
            ),
            instanceFields.associate { field ->
                field.name to field.type
            },
        )
    }

    @Test
    fun disabledRequestDoesNotResolveStores() {
        assertIs<HimZeroCandidateRecoveryReviewCorpusRuntimeResult.Skipped>(execute(enabled = false))
    }

    @Test
    fun sourceBreakdownUsesFixedSourceOrder() {
        assertEquals(
            HimZeroCandidateRecoveryReviewCorpusContractV1.SOURCE_ORDER,
            completed().sourceBreakdown.map { it.source },
        )
    }

    @Test
    fun entriesUseFixedSourceOrder() {
        val entries = completed().entries
        assertEquals(entries.sortedWith(compareBy({ HimZeroCandidateRecoveryReviewCorpusContractV1.sourceIndex(it.source) }, { it.evidenceReference })), entries)
    }

    @Test
    fun occurrenceIdsAreSorted() {
        assertTrue(completed().entries.all { it.findingOccurrenceIds == it.findingOccurrenceIds.sorted() })
    }

    @Test
    fun identityAndAliasTermsAreSortedAndDeduplicated() {
        val target = completed().entries.first().auditLinkedCanonicalTargets.first()
        assertEquals(target.identityTerms.distinct().sorted(), target.identityTerms)
        assertEquals(target.aliasTerms.distinct().sorted(), target.aliasTerms)
    }

    @Test
    fun causeAnalysisInputIsNotMutated() {
        val cause = causeReport()
        val before = cause
        execute(cause = cause)
        assertEquals(before, cause)
    }

    @Test
    fun scaledShapeCanRepresent123GroupsAnd104Supplements() {
        val groups = (1..123).map { index -> group("group-$index", index, index + 1000, listOf("c00001")) }
        assertEquals(123, groups.size)
        val supplements = (1..104).map { index -> index }
        assertEquals(104, supplements.size)
    }

    @Test
    fun reportDigestIsDeterministic() {
        val first = completed()
        val second = completed()
        assertEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun allEntriesCarryASelectionReason() {
        assertTrue(completed().entries.all { it.selectionReasons.isNotEmpty() })
    }

    private fun completed(
        enabled: Boolean = true,
        cause: HimZeroCandidateCauseAnalysisReportV1 = causeReport(),
        authority: HimCanonicalFamilyAuthority = authority(),
        inputBinding: HimZeroCandidateRecoveryReviewCorpusInputBindingV1 = inputBinding(cause),
        jsonOutputFile: java.io.File? = null,
        textOutputFile: java.io.File? = null,
    ): HimZeroCandidateRecoveryReviewCorpusReportV1 =
        assertIs<HimZeroCandidateRecoveryReviewCorpusRuntimeResult.Completed<*>>(
            execute(enabled, cause, authority, inputBinding, jsonOutputFile, textOutputFile),
        ).value as HimZeroCandidateRecoveryReviewCorpusReportV1

    private fun execute(
        enabled: Boolean = true,
        cause: HimZeroCandidateCauseAnalysisReportV1 = causeReport(),
        authority: HimCanonicalFamilyAuthority = authority(),
        inputBinding: HimZeroCandidateRecoveryReviewCorpusInputBindingV1 = inputBinding(cause),
        jsonOutputFile: java.io.File? = null,
        textOutputFile: java.io.File? = null,
    ) = HimZeroCandidateRecoveryReviewCorpusRuntimeV1.execute(
        request(enabled, cause, authority, inputBinding, jsonOutputFile, textOutputFile),
    )

    private fun request(
        enabled: Boolean = true,
        cause: HimZeroCandidateCauseAnalysisReportV1 = causeReport(),
        authority: HimCanonicalFamilyAuthority = authority(),
        inputBinding: HimZeroCandidateRecoveryReviewCorpusInputBindingV1 = inputBinding(cause),
        jsonOutputFile: java.io.File? = null,
        textOutputFile: java.io.File? = null,
    ) = HimZeroCandidateRecoveryReviewCorpusRuntimeRequestV1(
        enabled, cause, catalog(), registry(), authority, inputBinding, jsonOutputFile, textOutputFile,
    )

    private fun assertReason(
        result: HimZeroCandidateRecoveryReviewCorpusRuntimeResult<*>,
        expected: HimZeroCandidateRecoveryReviewFailureReasonV1,
    ) {
        assertEquals(expected, assertIs<HimZeroCandidateRecoveryReviewCorpusRuntimeResult.Failed>(result).reason)
    }

    private fun causeReport(
        records: List<HimZeroCandidateCauseAnalysisRecordV1> = causeRecords(),
        groups: List<HimZeroCandidateCauseAnalysisRecurringPrimaryValueGroupV1> = causeGroups(),
    ): HimZeroCandidateCauseAnalysisReportV1 {
        val sortedRecords = records.sortedWith(compareBy({ HimZeroCandidateCauseAnalysisContractV1.sourceIndex(it.source) }, { it.evidenceReference }))
        val binding = causeInputBinding()
        val unsigned = HimZeroCandidateCauseAnalysisReportV1(
            HimZeroCandidateCauseAnalysisContractV1.VERSION,
            binding,
            HimZeroCandidateCauseAnalysisTotalCountersV1(sortedRecords.size, sortedRecords.size, sortedRecords.size, sortedRecords.flatMap { it.canonicalEntityIds }.distinct().size),
            HimZeroCandidateCauseAnalysisContractV1.BUCKET_ORDER.map { bucket -> HimZeroCandidateCauseAnalysisBucketCounterV1(bucket, sortedRecords.count { it.primaryBucket == bucket }) },
            HimZeroCandidateCauseAnalysisContractV1.SOURCE_ORDER.flatMap { source -> HimZeroCandidateCauseAnalysisContractV1.BUCKET_ORDER.map { bucket -> HimZeroCandidateCauseAnalysisSourceBucketBreakdownV1(source, bucket, sortedRecords.count { it.source == source && it.primaryBucket == bucket }) } },
            HimZeroCandidateCauseAnalysisContractV1.SOURCE_ORDER.flatMap { source -> HimZeroCandidateCauseAnalysisContractV1.FLAG_ORDER.map { flag -> HimZeroCandidateCauseAnalysisSourceFlagBreakdownV1(source, flag, sortedRecords.count { it.source == source && flag in it.flags }) } },
            HimZeroCandidateCauseAnalysisContractV1.RECORD_KIND_ORDER.flatMap { kind -> HimZeroCandidateCauseAnalysisContractV1.BUCKET_ORDER.map { bucket -> HimZeroCandidateCauseAnalysisRecordKindBucketBreakdownV1(kind, bucket, sortedRecords.count { it.recordKind == kind && it.primaryBucket == bucket }) } },
            HimZeroCandidateCauseAnalysisContractV1.FLAG_ORDER.map { flag -> HimZeroCandidateCauseAnalysisFlagCounterV1(flag, sortedRecords.count { flag in it.flags }) },
            groups.sortedWith(compareByDescending<HimZeroCandidateCauseAnalysisRecurringPrimaryValueGroupV1> { it.references.size }.thenBy { it.key }),
            emptyList(),
            sortedRecords,
            "",
        )
        val report = unsigned.copy(logicalDigest = HimZeroCandidateCauseAnalysisPersistenceV1.logicalDigest(unsigned))
        return report.also { it.validate() }
    }

    private fun causeRecords(): List<HimZeroCandidateCauseAnalysisRecordV1> = listOf(
        record(HimGroundTruthSource.OPEN_FOOD_FACTS, 1, HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH, "c00001"),
        record(HimGroundTruthSource.OPEN_FOOD_FACTS, 2, HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH, "c00001", "alpha-alt"),
        record(HimGroundTruthSource.AGRIBALYSE, 3, HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH, "c00001"),
        record(HimGroundTruthSource.CIQUAL, 4, HimZeroCandidateCauseAnalysisPrimaryBucketV1.LOCALIZED_PRIMARY_ONLY, "c00002"),
        record(HimGroundTruthSource.CIQUAL, 5, HimZeroCandidateCauseAnalysisPrimaryBucketV1.LOCALIZED_PRIMARY_ONLY, "c00002"),
        record(HimGroundTruthSource.GLYCEMIC_INDEX, 6, HimZeroCandidateCauseAnalysisPrimaryBucketV1.STRUCTURED_ALPHANUMERIC_PRIMARY, "c00003"),
        record(HimGroundTruthSource.AGRIBALYSE, 7, HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH, "c00003"),
        record(HimGroundTruthSource.OPEN_FOOD_FACTS, 8, HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH, "c00004"),
        record(HimGroundTruthSource.CIQUAL, 9, HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH, "c00005"),
        record(HimGroundTruthSource.CIQUAL, 10, HimZeroCandidateCauseAnalysisPrimaryBucketV1.LOCALIZED_PRIMARY_ONLY, "c00002"),
        record(HimGroundTruthSource.OPEN_FOOD_FACTS, 11, HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH, "c00001"),
    )

    private fun causeGroups() = listOf(
        group("alpha", 1, 2, listOf("c00001")),
        group("alpha-copy", 1, 2, listOf("c00001")),
        group("beta", 4, 5, listOf("c00002")),
        group("delta", 8, 9, listOf("c00004", "c00005")),
        group("gamma", 7, 6, listOf("c00003")),
    )

    private fun group(key: String, first: Int, second: Int, targets: List<String>) =
        HimZeroCandidateCauseAnalysisRecurringPrimaryValueGroupV1(
            key,
            listOf(first to sourceFor(first), second to sourceFor(second))
                .map { (index, source) -> HimZeroCandidateCauseAnalysisReferenceMemberV1(source, ref(source, index)) }
                .sortedWith(compareBy({ HimZeroCandidateCauseAnalysisContractV1.sourceIndex(it.source) }, { it.evidenceReference })),
        )

    private fun record(
        source: HimGroundTruthSource,
        index: Int,
        bucket: HimZeroCandidateCauseAnalysisPrimaryBucketV1,
        target: String,
        secondValue: String = "value-$index",
    ) = HimZeroCandidateCauseAnalysisRecordV1(
        source, ref(source, index), kind(source), "shard-000001", listOf(HimZeroCandidateCauseAnalysisPersistenceV1.sha256(ref(source, index))),
        listOf(target), HimUnresolvedPrimaryIdentityDiagnosticStatusV1.IDENTITY_FIELDS_PRESENT_NO_CANDIDATE, emptyList(),
        listOf(field(source, index, bucket)), listOf(secondValue), bucket, listOf(HimZeroCandidateCauseAnalysisFlagV1.HAS_PRIMARY_IDENTITY_FIELD),
    )

    private fun field(source: HimGroundTruthSource, index: Int, bucket: HimZeroCandidateCauseAnalysisPrimaryBucketV1) =
        HimUnresolvedPrimaryIdentityDiagnosticFieldV1(
            "field-$index", "value-$index", "value-$index", HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.EN,
            HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.PRIMARY_IDENTITY_CANDIDATE, source, kind(source),
            HimUnresolvedPrimaryIdentityDiagnosticLexicalFeaturesV1(false, 7, 1, 1, bucket == HimZeroCandidateCauseAnalysisPrimaryBucketV1.STRUCTURED_ALPHANUMERIC_PRIMARY, false, false, false, false, false),
        )

    private fun sourceFor(index: Int) = when (index) {
        1, 2, 8, 11 -> HimGroundTruthSource.OPEN_FOOD_FACTS
        3, 7 -> HimGroundTruthSource.AGRIBALYSE
        4, 5, 9, 10 -> HimGroundTruthSource.CIQUAL
        else -> HimGroundTruthSource.GLYCEMIC_INDEX
    }

    private fun kind(source: HimGroundTruthSource) = when (source) {
        HimGroundTruthSource.OPEN_FOOD_FACTS -> HimEvidenceRecordKind.OFF_PRODUCT
        HimGroundTruthSource.AGRIBALYSE -> HimEvidenceRecordKind.AGRIBALYSE_RECORD
        HimGroundTruthSource.CIQUAL -> HimEvidenceRecordKind.CIQUAL_FOOD
        HimGroundTruthSource.GLYCEMIC_INDEX -> HimEvidenceRecordKind.GI_MEASUREMENT
    }

    private fun ref(source: HimGroundTruthSource, index: Int) = when (source) {
        HimGroundTruthSource.OPEN_FOOD_FACTS -> off(index)
        HimGroundTruthSource.AGRIBALYSE -> HimEvidenceRecordReference.agribalyse(index.toLong(), "agb$index").value
        HimGroundTruthSource.CIQUAL -> ciqual(index)
        HimGroundTruthSource.GLYCEMIC_INDEX -> HimEvidenceRecordReference.gi("measurement", index.toLong()).value
    }

    private fun off(index: Int) = HimEvidenceRecordReference.offProduct(index.toLong(), "code$index").value
    private fun ciqual(index: Int) = HimEvidenceRecordReference.ciqualFood("code$index").value

    private fun causeInputBinding(): HimZeroCandidateCauseAnalysisInputBindingV1 {
        val diagnosticMission = binding("cause/mission.json", "a")
        val diagnosticAggregate = binding("cause/aggregate.json", "b")
        val base = HimZeroCandidateCauseAnalysisInputBindingV1(
            diagnosticMission = diagnosticMission,
            diagnosticAggregate = diagnosticAggregate,
            diagnosticMissionLogicalDigest = diagnosticMission.logicalDigest,
            diagnosticAggregateLogicalDigest = diagnosticAggregate.logicalDigest,
            analysisImplementationHead = "a".repeat(40),
            bindingDigest = "",
        )
        return base.copy(
            bindingDigest =
                HimZeroCandidateCauseAnalysisPersistenceV1.bindingDigest(base),
        )
    }

    private fun binding(path: String, seed: String) = HimZeroCandidateCauseAnalysisFileBindingV1(path, 1, seed.repeat(64), seed.repeat(64))

    private fun inputBinding(cause: HimZeroCandidateCauseAnalysisReportV1 = causeReport()) = run {
        val base = HimZeroCandidateRecoveryReviewCorpusInputBindingV1(
            HimZeroCandidateCauseAnalysisFileBindingV1("build/cause-analysis.json", cause.toString().length.toLong(), "a".repeat(64), cause.logicalDigest),
            HimZeroCandidateRecoveryReviewCorpusFileBindingV1(
                "fixture/catalog.json",
                1,
                catalog().contentSha256,
                "b".repeat(64)
            ),
            HimZeroCandidateRecoveryReviewCorpusFileBindingV1(
                "fixture/authority.json",
                1,
                "c".repeat(64),
                "d".repeat(64)
            ),
            cause.logicalDigest, "a".repeat(40), "",
        )
        base.copy(bindingDigest = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.bindingDigest(base))
    }

    private fun catalog() = HimProductOnlyCanonicalMaster(
        "fixture/catalog.json", "1".repeat(64), (1..5).map { index -> HimProductOnlyCanonical("Canonical $index", "canonical$index", emptyList()) },
    )

    private fun authority() = HimCanonicalFamilyAuthority(
        "1", HimCanonicalFamilySourceCatalog(catalog().path, catalog().contentSha256, catalog().records.size),
        (1..5).map { index ->
            HimCanonicalFamily(HimEntityId("c0000$index"), "Canonical $index", "canonical$index", emptyList(), HimLifecycleStatus.ACTIVE, emptyList(), emptyList(),
                if (index == 1) listOf(HimCanonicalAlias(HimEntityId("a0000$index"), "Alias $index", "alias$index", HimLifecycleStatus.ACTIVE)) else emptyList())
        },
    )

    private fun registry() = HimEntityIdRegistry((1..5).map { index ->
        HimEntityIdRegistryEntry(HimEntityId("c0000$index"), HimEntityType.CANONICAL, HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED, "canonical$index")
    })
}
