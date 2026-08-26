package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.io.File

object HimZeroCandidateRecoveryReviewCorpusRuntimeV1 {
    fun execute(
        request: HimZeroCandidateRecoveryReviewCorpusRuntimeRequestV1,
    ): HimZeroCandidateRecoveryReviewCorpusRuntimeResult<HimZeroCandidateRecoveryReviewCorpusReportV1> {
        if (!request.enabled) return HimZeroCandidateRecoveryReviewCorpusRuntimeResult.Skipped("RECOVERY_REVIEW_OPT_IN_REQUIRED")
        return try {
            validateInput(request)
            val report = buildReport(request)
            report.validate()
            if (request.jsonOutputFile != null || request.textOutputFile != null) {
                require(request.jsonOutputFile != null && request.textOutputFile != null) {
                    fail(HimZeroCandidateRecoveryReviewFailureReasonV1.PERSISTENCE_FAILED, "outputPair")
                }
                try {
                    HimZeroCandidateRecoveryReviewCorpusPersistenceV1.writeReport(
                        request.jsonOutputFile!!,
                        request.textOutputFile!!,
                        report,
                    )
                } catch (_: Throwable) {
                    fail(HimZeroCandidateRecoveryReviewFailureReasonV1.PERSISTENCE_FAILED, "writeReport")
                }
            }
            HimZeroCandidateRecoveryReviewCorpusRuntimeResult.Completed(report)
        } catch (failure: RecoveryReviewFailure) {
            HimZeroCandidateRecoveryReviewCorpusRuntimeResult.Failed(failure.reason, failure.safeContext)
        } catch (_: Throwable) {
            HimZeroCandidateRecoveryReviewCorpusRuntimeResult.Failed(
                HimZeroCandidateRecoveryReviewFailureReasonV1.REPORT_VALIDATION_FAILED,
                "report",
            )
        }
    }

    private fun validateInput(
        request: HimZeroCandidateRecoveryReviewCorpusRuntimeRequestV1,
    ) {
        try {
            request.inputBinding.validate()
        } catch (_: Throwable) {
            fail(HimZeroCandidateRecoveryReviewFailureReasonV1.INPUT_BINDING_MISMATCH, "inputBinding")
        }
        try {
            request.causeAnalysis.validate()
        } catch (_: Throwable) {
            fail(HimZeroCandidateRecoveryReviewFailureReasonV1.CAUSE_ANALYSIS_VALIDATION_FAILED, "causeAnalysis")
        }
        require(request.inputBinding.causeAnalysisLogicalDigest == request.causeAnalysis.logicalDigest) {
            fail(HimZeroCandidateRecoveryReviewFailureReasonV1.CAUSE_ANALYSIS_BINDING_MISMATCH, "causeAnalysisDigest")
        }
        try {
            validateCatalogAndAuthority(request.catalog, request.registry, request.authority, request.inputBinding)
        } catch (failure: RecoveryReviewFailure) {
            throw failure
        } catch (_: Throwable) {
            fail(HimZeroCandidateRecoveryReviewFailureReasonV1.CATALOG_VALIDATION_FAILED, "catalog")
        }
    }

    private fun validateCatalogAndAuthority(
        catalog: HimProductOnlyCanonicalMaster,
        registry: HimEntityIdRegistry,
        authority: HimCanonicalFamilyAuthority,
        binding: HimZeroCandidateRecoveryReviewCorpusInputBindingV1,
    ) {
        require(catalog.path == binding.canonicalCatalog.relativePath)
        require(catalog.contentSha256 == binding.canonicalCatalog.sha256)
        require(authority.schemaVersion == "1")
        require(authority.sourceCatalog.path == catalog.path)
        require(authority.sourceCatalog.contentSha256 == catalog.contentSha256)
        require(authority.sourceCatalog.recordCount == catalog.records.size)
        require(authority.families.size == catalog.records.size)
        require(authority.families.map { it.canonicalId }.distinct().size == authority.families.size)
        require(registry.entries.count { it.entityType == HimEntityType.CANONICAL } == catalog.records.size)
        require(registry.entries.map { it.entityId }.distinct().size == registry.entries.size)
        val registryByReference = registry.entries.associateBy { it.sourceReference }
        require(registry.entries.all {
            it.entityType == HimEntityType.CANONICAL &&
                it.sourceReferenceType == de.shopme.tools.knowledge.him.canonical.family.HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED
        })
        authority.families.zip(catalog.records).forEach { (family, record) ->
            require(family.canonicalName == record.itemname)
            require(family.normalizedName == record.normalized)
            require(family.taxonomyPaths == record.taxonomyPaths)
            require(registryByReference[record.normalized]?.entityId == family.canonicalId)
        }
    }

    private data class GroupMaterialization(
        val key: String,
        val priority: HimZeroCandidateRecoveryReviewPriorityV1,
        val references: List<String>,
        val targetIds: List<String>,
        val findingOccurrenceCount: Int,
    )

    private fun buildReport(
        request: HimZeroCandidateRecoveryReviewCorpusRuntimeRequestV1,
    ): HimZeroCandidateRecoveryReviewCorpusReportV1 {
        val cause = request.causeAnalysis
        val recordsByKey = cause.records.associateBy {
            HimZeroCandidateRecoveryReviewCorpusContractV1.referenceKey(it.source, it.evidenceReference)
        }
        require(recordsByKey.size == cause.records.size) {
            fail(HimZeroCandidateRecoveryReviewFailureReasonV1.DUPLICATE_EVIDENCE_REFERENCE, "causeRecords")
        }

        val groups = cause.recurringPrimaryValueGroups.map { group ->
            val references = group.references.map {
                HimZeroCandidateRecoveryReviewCorpusContractV1.referenceKey(it.source, it.evidenceReference)
            }
            val groupRecords = references.map { key ->
                recordsByKey[key] ?: fail(
                    HimZeroCandidateRecoveryReviewFailureReasonV1.UNKNOWN_GROUP_REFERENCE,
                    "groupReference",
                )
            }
            val targetIds = groupRecords.flatMap { it.canonicalEntityIds }.distinct().sorted()
            require(targetIds.isNotEmpty()) {
                fail(HimZeroCandidateRecoveryReviewFailureReasonV1.INVALID_PRIORITY_CLASSIFICATION, "groupTarget")
            }
            val priority = when {
                targetIds.size > 1 -> HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_AMBIGUOUS_AUDIT_TARGET
                groupRecords.all { it.primaryBucket == HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH } ->
                    HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_TEXT_SINGLE_AUDIT_TARGET
                groupRecords.all { it.primaryBucket == HimZeroCandidateCauseAnalysisPrimaryBucketV1.LOCALIZED_PRIMARY_ONLY } ->
                    HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_LOCALIZED_SINGLE_AUDIT_TARGET
                else -> HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_STRUCTURED_OR_MIXED_SINGLE_AUDIT_TARGET
            }
            GroupMaterialization(
                key = group.key,
                priority = priority,
                references = references,
                targetIds = targetIds,
                findingOccurrenceCount = groupRecords.sumOf { it.findingOccurrenceIds.size },
            )
        }
        require(groups.map { it.key }.distinct().size == groups.size) {
            fail(HimZeroCandidateRecoveryReviewFailureReasonV1.SELECTION_INVARIANT_FAILED, "groupKeys")
        }

        val membershipsByReference = mutableMapOf<String, MutableList<GroupMaterialization>>()
        groups.forEach { group ->
            group.references.forEach { key -> membershipsByReference.getOrPut(key) { mutableListOf() }.add(group) }
        }
        val supplementKeys = cause.records.filter {
            it.source == HimGroundTruthSource.CIQUAL &&
                it.primaryBucket == HimZeroCandidateCauseAnalysisPrimaryBucketV1.LOCALIZED_PRIMARY_ONLY
        }.map {
            HimZeroCandidateRecoveryReviewCorpusContractV1.referenceKey(it.source, it.evidenceReference)
        }.toSet()
        val selectedKeys = (membershipsByReference.keys + supplementKeys).toSet()
        val selected = cause.records.filter {
            HimZeroCandidateRecoveryReviewCorpusContractV1.referenceKey(it.source, it.evidenceReference) in selectedKeys
        }
        require(selected.map { HimZeroCandidateRecoveryReviewCorpusContractV1.referenceKey(it.source, it.evidenceReference) }.toSet() == selectedKeys) {
            fail(HimZeroCandidateRecoveryReviewFailureReasonV1.SELECTION_INVARIANT_FAILED, "selectionUnion")
        }

        val entries = selected.map { record ->
            val key = HimZeroCandidateRecoveryReviewCorpusContractV1.referenceKey(record.source, record.evidenceReference)
            val memberships = membershipsByReference[key].orEmpty()
                .sortedWith(compareBy({ it.priority.ordinal }, { it.key }))
            val reasons = buildList {
                if (memberships.isNotEmpty()) add(HimZeroCandidateRecoveryReviewSelectionReasonV1.RECURRING_PRIMARY_VALUE_GROUP)
                if (key in supplementKeys) add(HimZeroCandidateRecoveryReviewSelectionReasonV1.CIQUAL_LOCALIZED_PRIMARY_SUPPLEMENT)
            }
            require(reasons.isNotEmpty()) {
                fail(HimZeroCandidateRecoveryReviewFailureReasonV1.SELECTION_INVARIANT_FAILED, "selectionReason")
            }
            HimZeroCandidateRecoveryReviewCorpusEntryV1(
                stableEntryId = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(
                    "${HimZeroCandidateRecoveryReviewCorpusContractV1.VERSION}|$key",
                ),
                source = record.source,
                evidenceReference = record.evidenceReference,
                recordKind = record.recordKind,
                ownerShardId = record.ownerShardId,
                findingOccurrenceIds = record.findingOccurrenceIds,
                auditLinkedCanonicalTargets = record.canonicalEntityIds.map { resolveTarget(it, request.authority) }
                    .sortedBy { it.canonicalEntityId },
                originalFields = record.fields,
                primaryValues = record.primaryValues,
                primaryBucket = record.primaryBucket,
                diagnosticFlags = record.flags,
                selectionReasons = reasons,
                recurringGroupMemberships = memberships.map { group ->
                    HimZeroCandidateRecoveryReviewGroupMembershipV1(
                        primaryValue = group.key,
                        priorityClass = group.priority,
                        referenceCount = group.references.size,
                        findingOccurrenceCount = group.findingOccurrenceCount,
                        auditLinkedCanonicalEntityIds = group.targetIds,
                    )
                },
                associationState = HimZeroCandidateRecoveryReviewAssociationStateV1.UNVERIFIED_AUDIT_ASSOCIATION,
                reviewState = HimZeroCandidateRecoveryReviewStateV1.UNREVIEWED,
            )
        }.sortedWith(compareBy({ HimZeroCandidateRecoveryReviewCorpusContractV1.sourceIndex(it.source) }, { it.evidenceReference }))

        val counters = HimZeroCandidateRecoveryReviewCorpusCountersV1(
            causeAnalysisRecords = cause.records.size,
            recurringPrimaryValueGroups = groups.size,
            recurringReferenceMemberships = groups.sumOf { it.references.size },
            ciqualLocalizedSupplementRecords = supplementKeys.size,
            selectionOverlapRecords = entries.count { it.selectionReasons.size > 1 },
            selectedUniqueRecords = entries.size,
            auditLinkedCanonicalTargets = entries.flatMap { it.auditLinkedCanonicalTargets }.map { it.canonicalEntityId }.distinct().size,
            unreviewedRecords = entries.size,
            confirmedRecords = 0,
        )
        val priorityCounters = HimZeroCandidateRecoveryReviewCorpusContractV1.PRIORITY_ORDER.map { priority ->
            val selectedGroups = groups.filter { it.priority == priority }
            HimZeroCandidateRecoveryReviewPriorityCounterV1(
                priorityClass = priority,
                groups = selectedGroups.size,
                referenceMemberships = selectedGroups.sumOf { it.references.size },
                findingOccurrences = selectedGroups.sumOf { it.findingOccurrenceCount },
            )
        }
        val unsigned = HimZeroCandidateRecoveryReviewCorpusReportV1(
            contractId = HimZeroCandidateRecoveryReviewCorpusContractV1.VERSION,
            inputBinding = request.inputBinding,
            counters = counters,
            priorityCounters = priorityCounters,
            sourceBreakdown = HimZeroCandidateRecoveryReviewCorpusContractV1.SOURCE_ORDER.map { source ->
                HimZeroCandidateRecoveryReviewSourceBreakdownV1(source, entries.count { it.source == source })
            },
            recordKindBreakdown = HimZeroCandidateRecoveryReviewCorpusContractV1.RECORD_KIND_ORDER.map { kind ->
                HimZeroCandidateRecoveryReviewRecordKindBreakdownV1(kind, entries.count { it.recordKind == kind })
            },
            selectionReasonBreakdown = HimZeroCandidateRecoveryReviewCorpusContractV1.SELECTION_REASON_ORDER.map { reason ->
                HimZeroCandidateRecoveryReviewSelectionReasonBreakdownV1(reason, entries.count { reason in it.selectionReasons })
            },
            entries = entries,
            logicalDigest = "",
        )
        return unsigned.copy(logicalDigest = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.logicalDigest(unsigned))
    }

    private fun resolveTarget(
        entityId: String,
        authority: HimCanonicalFamilyAuthority,
    ): HimZeroCandidateRecoveryReviewCanonicalTargetV1 {
        val matches = authority.families.filter { it.canonicalId.value == entityId }
        require(matches.isNotEmpty()) {
            fail(HimZeroCandidateRecoveryReviewFailureReasonV1.CANONICAL_TARGET_NOT_FOUND, "canonicalTarget")
        }
        require(matches.size == 1) {
            fail(HimZeroCandidateRecoveryReviewFailureReasonV1.CANONICAL_TARGET_AMBIGUOUS, "canonicalTarget")
        }
        val family = matches.single()
        val identities = family.identities.map { it.identityName }
        val aliases = family.aliases.map { it.aliasName } + family.identities.flatMap { identity -> identity.aliases.map { it.aliasName } }
        return HimZeroCandidateRecoveryReviewCanonicalTargetV1(
            canonicalEntityId = family.canonicalId.value,
            canonicalName = family.canonicalName,
            identityTerms = identities.distinct().sorted(),
            aliasTerms = aliases.distinct().sorted(),
        )
    }

    private class RecoveryReviewFailure(
        val reason: HimZeroCandidateRecoveryReviewFailureReasonV1,
        val safeContext: String,
    ) : IllegalArgumentException()

    private fun fail(
        reason: HimZeroCandidateRecoveryReviewFailureReasonV1,
        safeContext: String,
    ): Nothing = throw RecoveryReviewFailure(reason, safeContext)
}

data class HimZeroCandidateRecoveryReviewCorpusRuntimeRequestV1(
    val enabled: Boolean,
    val causeAnalysis: HimZeroCandidateCauseAnalysisReportV1,
    val catalog: HimProductOnlyCanonicalMaster,
    val registry: HimEntityIdRegistry,
    val authority: HimCanonicalFamilyAuthority,
    val inputBinding: HimZeroCandidateRecoveryReviewCorpusInputBindingV1,
    val jsonOutputFile: File? = null,
    val textOutputFile: File? = null,
)

sealed interface HimZeroCandidateRecoveryReviewCorpusRuntimeResult<out T> {
    data class Completed<T>(val value: T) : HimZeroCandidateRecoveryReviewCorpusRuntimeResult<T>
    data class Skipped(val reason: String) : HimZeroCandidateRecoveryReviewCorpusRuntimeResult<Nothing>
    data class Failed(
        val reason: HimZeroCandidateRecoveryReviewFailureReasonV1,
        val safeContext: String,
    ) : HimZeroCandidateRecoveryReviewCorpusRuntimeResult<Nothing>
}
