package de.shopme.tools.knowledge.him.training.corpus

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import java.security.MessageDigest

object HimTrainingPartitionContractV1 {
    const val VERSION = "HIM_TRAINING_PARTITION_V1"
    const val POLICY_VERSION = "HIM_TRAINING_PARTITION_POLICY_V1"
    const val BUCKET_DOMAIN_SIZE = 10_000
    const val TRAIN_FIRST_BUCKET = 0
    const val TRAIN_LAST_BUCKET = 7_999
    const val VALIDATION_FIRST_BUCKET = 8_000
    const val VALIDATION_LAST_BUCKET = 8_999
    const val HOLDOUT_FIRST_BUCKET = 9_000
    const val HOLDOUT_LAST_BUCKET = 9_999
}

enum class HimTrainingPartitionV1 {
    TRAIN,
    VALIDATION,
    HOLDOUT,
}

data class HimTrainingFamilyGroupReferenceV1(
    val value: String,
) {
    init {
        require(value.matches(Regex("family:v1:canonical:[0-9A-Za-z]{6}")))
    }

    companion object {
        fun canonical(canonicalId: HimEntityId) =
            HimTrainingFamilyGroupReferenceV1("family:v1:canonical:${canonicalId.value}")
    }
}

data class HimTrainingLineageReferenceV1(
    val value: String,
) {
    init {
        require(value.matches(Regex("lineage:v1:[0-9a-f]{64}")))
    }
}

sealed interface HimTrainingFamilyGroupResolutionV1 {
    data class Resolved(
        val groupReference: HimTrainingFamilyGroupReferenceV1,
    ) : HimTrainingFamilyGroupResolutionV1

    data class NotYetGroupable(
        val reason: String,
    ) : HimTrainingFamilyGroupResolutionV1 {
        init {
            require(reason.isNotBlank())
        }
    }
}

object HimTrainingFamilyGroupResolverV1 {
    fun resolve(example: HimTrainingExampleV1): HimTrainingFamilyGroupResolutionV1 =
        when (val target = example.target) {
            is HimTrainingTargetV1.ExistingCanonical -> resolved(target.canonicalId)
            is HimTrainingTargetV1.Identity -> resolved(target.parentCanonicalId)
            is HimTrainingTargetV1.Variant -> resolved(target.scope.canonicalId)
            is HimTrainingTargetV1.Alias -> resolved(target.equivalentEntity.canonicalId)
            is HimTrainingTargetV1.NewCanonical ->
                HimTrainingFamilyGroupResolutionV1.NotYetGroupable(
                    "NEW_CANONICAL has no validated existing Canonical Family identity in current F3.8 lineage.",
                )
        }

    fun resolve(negative: HimNegativeTrainingExampleV1): HimTrainingFamilyGroupResolutionV1 =
        resolve(negative.positiveExample)

    private fun resolved(canonicalId: HimEntityId) =
        HimTrainingFamilyGroupResolutionV1.Resolved(HimTrainingFamilyGroupReferenceV1.canonical(canonicalId))
}

sealed interface HimTrainingPartitionRecordV1 {
    val recordReference: String
    val positiveExample: HimTrainingExampleV1

    data class Positive(
        override val positiveExample: HimTrainingExampleV1,
    ) : HimTrainingPartitionRecordV1 {
        override val recordReference: String = positiveExample.exampleReference.value
    }

    data class Negative(
        val negativeExample: HimNegativeTrainingExampleV1,
    ) : HimTrainingPartitionRecordV1 {
        override val positiveExample: HimTrainingExampleV1 = negativeExample.positiveExample
        override val recordReference: String = negativeExample.reference.value
    }
}

data class HimTrainingPartitionAssignmentV1(
    val record: HimTrainingPartitionRecordV1,
    val groupReference: HimTrainingFamilyGroupReferenceV1,
    val partition: HimTrainingPartitionV1,
)

data class HimTrainingPartitionDiagnosticsV1(
    val trainPositiveCount: Int,
    val trainNegativeCount: Int,
    val validationPositiveCount: Int,
    val validationNegativeCount: Int,
    val holdoutPositiveCount: Int,
    val holdoutNegativeCount: Int,
    val totalRecordCount: Int,
    val familyGroupCount: Int,
    val contextOnlyCrossPartitionReferenceCount: Int,
    val leakageDiagnostics: List<HimTrainingPartitionLeakageDiagnosticV1> = emptyList(),
) {
    init {
        require(
            listOf(
                trainPositiveCount,
                trainNegativeCount,
                validationPositiveCount,
                validationNegativeCount,
                holdoutPositiveCount,
                holdoutNegativeCount,
                totalRecordCount,
                familyGroupCount,
                contextOnlyCrossPartitionReferenceCount,
            ).all { it >= 0 },
        )
    }

    companion object {
        fun from(
            assignments: List<HimTrainingPartitionAssignmentV1>,
            leakageDiagnostics: List<HimTrainingPartitionLeakageDiagnosticV1> = emptyList(),
        ): HimTrainingPartitionDiagnosticsV1 {
            fun count(partition: HimTrainingPartitionV1, negative: Boolean) = assignments.count {
                it.partition == partition && (it.record is HimTrainingPartitionRecordV1.Negative) == negative
            }
            val contextCount = leakageDiagnostics.count {
                it.level == HimTrainingPartitionLeakageLevelV1.CONTEXT_ONLY_CROSS_PARTITION_REFERENCE
            }
            return HimTrainingPartitionDiagnosticsV1(
                trainPositiveCount = count(HimTrainingPartitionV1.TRAIN, false),
                trainNegativeCount = count(HimTrainingPartitionV1.TRAIN, true),
                validationPositiveCount = count(HimTrainingPartitionV1.VALIDATION, false),
                validationNegativeCount = count(HimTrainingPartitionV1.VALIDATION, true),
                holdoutPositiveCount = count(HimTrainingPartitionV1.HOLDOUT, false),
                holdoutNegativeCount = count(HimTrainingPartitionV1.HOLDOUT, true),
                totalRecordCount = assignments.size,
                familyGroupCount = assignments.map { it.groupReference }.distinct().size,
                contextOnlyCrossPartitionReferenceCount = contextCount,
                leakageDiagnostics = leakageDiagnostics,
            )
        }
    }
}

enum class HimTrainingPartitionLeakageLevelV1 {
    EXACT_EXAMPLE_LEAKAGE,
    DERIVED_NEGATIVE_LEAKAGE,
    CANONICAL_FAMILY_LEAKAGE,
    LINEAGE_LEAKAGE,
    DUPLICATE_SEMANTIC_TARGET_LEAKAGE,
    CONTEXT_ONLY_CROSS_PARTITION_REFERENCE,
    INVALID_DETERMINISTIC_ASSIGNMENT,
}

data class HimTrainingPartitionLeakageDiagnosticV1(
    val level: HimTrainingPartitionLeakageLevelV1,
    val fatal: Boolean,
    val key: String,
    val recordReferences: List<String>,
    val message: String,
) {
    init {
        require(key.isNotBlank() && recordReferences.isNotEmpty() && message.isNotBlank())
    }
}

data class HimTrainingPartitionValidationResultV1(
    val valid: Boolean,
    val diagnostics: List<HimTrainingPartitionLeakageDiagnosticV1>,
) {
    init {
        require(valid == diagnostics.none { it.fatal })
    }
}

data class HimTrainingPartitionManifestV1(
    val policyVersion: String,
    val assignments: List<HimTrainingPartitionAssignmentV1>,
    val diagnostics: HimTrainingPartitionDiagnosticsV1,
    val logicalDigest: de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256,
) {
    init {
        require(policyVersion == HimTrainingPartitionContractV1.POLICY_VERSION)
    }

    companion object {
        fun create(assignments: List<HimTrainingPartitionAssignmentV1>): HimTrainingPartitionManifestV1 {
            val ordered = assignments.sortedWith(HimTrainingPartitionOrderingV1.assignmentComparator)
            return HimTrainingPartitionManifestV1(
                policyVersion = HimTrainingPartitionContractV1.POLICY_VERSION,
                assignments = ordered,
                diagnostics = HimTrainingPartitionDiagnosticsV1.from(ordered),
                logicalDigest = HimTrainingPartitionManifestIdentityV1.digest(
                    HimTrainingPartitionContractV1.POLICY_VERSION,
                    ordered,
                ),
            )
        }
    }
}

sealed interface HimTrainingPartitionBuildResultV1 {
    data class Partitioned(
        val manifest: HimTrainingPartitionManifestV1,
    ) : HimTrainingPartitionBuildResultV1

    data class NotYetGroupable(
        val recordReference: String,
        val reason: String,
    ) : HimTrainingPartitionBuildResultV1 {
        init {
            require(recordReference.isNotBlank() && reason.isNotBlank())
        }
    }
}

class HimTrainingPartitionerV1 {
    fun assign(records: List<HimTrainingPartitionRecordV1>): HimTrainingPartitionBuildResultV1 {
        require(records.map { it.recordReference }.distinct().size == records.size) {
            "Duplicate positive or negative record reference."
        }
        val positiveReferences = records
            .filterIsInstance<HimTrainingPartitionRecordV1.Positive>()
            .map { it.positiveExample.exampleReference }
            .toSet()
        records.filterIsInstance<HimTrainingPartitionRecordV1.Negative>().forEach { negative ->
            require(negative.negativeExample.provenance.positiveExampleReference in positiveReferences) {
                "Negative record references a missing positive example."
            }
        }

        val assignments = records.map { record ->
            when (val resolution = resolve(record)) {
                is HimTrainingFamilyGroupResolutionV1.NotYetGroupable ->
                    return HimTrainingPartitionBuildResultV1.NotYetGroupable(record.recordReference, resolution.reason)
                is HimTrainingFamilyGroupResolutionV1.Resolved ->
                    HimTrainingPartitionAssignmentV1(
                        record = record,
                        groupReference = resolution.groupReference,
                        partition = HimTrainingPartitionPolicyV1.partitionForGroup(resolution.groupReference),
                    )
            }
        }
        val manifest = HimTrainingPartitionManifestV1.create(assignments)
        val validation = HimTrainingPartitionLeakageValidatorV1.validate(manifest)
        require(validation.valid) {
            validation.diagnostics.filter { it.fatal }
                .joinToString("; ") { "${it.level}: ${it.message}" }
        }
        return HimTrainingPartitionBuildResultV1.Partitioned(
            manifest.copy(
                diagnostics = HimTrainingPartitionDiagnosticsV1.from(
                    assignments = manifest.assignments,
                    leakageDiagnostics = validation.diagnostics,
                ),
            ),
        )
    }

    private fun resolve(record: HimTrainingPartitionRecordV1) =
        when (record) {
            is HimTrainingPartitionRecordV1.Positive -> HimTrainingFamilyGroupResolverV1.resolve(record.positiveExample)
            is HimTrainingPartitionRecordV1.Negative -> HimTrainingFamilyGroupResolverV1.resolve(record.negativeExample)
        }
}

object HimTrainingPartitionPolicyV1 {
    fun bucketForGroup(groupReference: HimTrainingFamilyGroupReferenceV1): Int {
        val digest = sha256(groupReference.value)
        return (digest.substring(0, 8).toLong(16) % HimTrainingPartitionContractV1.BUCKET_DOMAIN_SIZE).toInt()
    }

    fun partitionForBucket(bucket: Int): HimTrainingPartitionV1 {
        require(bucket in 0 until HimTrainingPartitionContractV1.BUCKET_DOMAIN_SIZE)
        return when (bucket) {
            in HimTrainingPartitionContractV1.TRAIN_FIRST_BUCKET..HimTrainingPartitionContractV1.TRAIN_LAST_BUCKET -> HimTrainingPartitionV1.TRAIN
            in HimTrainingPartitionContractV1.VALIDATION_FIRST_BUCKET..HimTrainingPartitionContractV1.VALIDATION_LAST_BUCKET -> HimTrainingPartitionV1.VALIDATION
            else -> HimTrainingPartitionV1.HOLDOUT
        }
    }

    fun partitionForGroup(groupReference: HimTrainingFamilyGroupReferenceV1): HimTrainingPartitionV1 =
        partitionForBucket(bucketForGroup(groupReference))

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

object HimTrainingPartitionLineageIdentityV1 {
    fun resolve(example: HimTrainingExampleV1): HimTrainingLineageReferenceV1? {
        val provenance = example.provenance
        val values = listOf(
            provenance.candidateReference?.value,
            provenance.generationRunReference?.value,
            provenance.inputRunReference?.value,
            provenance.validationReference?.value,
            provenance.promotionReference?.value,
            provenance.mutationReference?.value,
            provenance.groundTruthReleaseReference?.value,
        ).filterNotNull().filter { it.isNotBlank() }
        if (values.isEmpty()) return null
        val canonical = buildString {
            appendLine("contract=HIM_TRAINING_LINEAGE_REFERENCE_V1")
            values.forEach { appendLine("reference=${it.length}:$it") }
        }
        return HimTrainingLineageReferenceV1("lineage:v1:${sha256(canonical)}")
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

object HimTrainingPartitionManifestIdentityV1 {
    fun digest(
        policyVersion: String,
        assignments: List<HimTrainingPartitionAssignmentV1>,
    ): de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256 {
        val canonical = buildString {
            appendLine("contract=HIM_TRAINING_PARTITION_MANIFEST_IDENTITY_V1")
            appendLine("policy=$policyVersion")
            assignments.sortedWith(HimTrainingPartitionOrderingV1.assignmentComparator).forEach { assignment ->
                appendLine(
                    listOf(
                        assignment.groupReference.value,
                        assignment.partition.name,
                        assignment.record.recordType(),
                        assignment.record.recordReference,
                        assignment.record.positiveExample.exampleReference.value,
                        HimTrainingPartitionLineageIdentityV1.resolve(assignment.record.positiveExample)?.value.orEmpty(),
                        semanticKey(assignment.record),
                    ).joinToString("\u0000"),
                )
            }
        }
        return de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256(sha256(canonical))
    }

    internal fun semanticKey(record: HimTrainingPartitionRecordV1): String {
        val example = record.positiveExample
        val input = example.input
        val target = when (record) {
            is HimTrainingPartitionRecordV1.Positive -> HimNegativeTrainingExampleIdentityV1.targetKey(example.target)
            is HimTrainingPartitionRecordV1.Negative ->
                "positive=${HimNegativeTrainingExampleIdentityV1.targetKey(example.target)}|rejected=${HimNegativeTrainingExampleIdentityV1.targetKey(record.negativeExample.rejectedTarget)}"
        }
        return listOf(
            input.observedTerm,
            input.normalizedObservedTerm,
            input.canonicalContext.joinToString("|") { "${it.rank}:${it.canonicalId.value}:${it.canonicalName}:${it.fullRecordCanonicalJson.orEmpty()}" },
            input.evidence.sortedBy { "${it.reference.source}|${it.reference.sourceRecordIdentity}|${it.retrievalRank}" }
                .joinToString("|") { "${it.reference.source}:${it.reference.sourceArtifactSha256.value}:${it.reference.sourceRecordIdentity}:${it.recordKind}:${it.retrievalRank}" },
            target,
        ).joinToString("\u0000")
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

object HimTrainingPartitionOrderingV1 {
    val assignmentComparator: Comparator<HimTrainingPartitionAssignmentV1> =
        compareBy(
            { it.groupReference.value },
            { it.record.positiveExample.exampleReference.value },
            { it.record.recordType() },
            { it.record.recordReference },
        )

    private fun HimTrainingPartitionRecordV1.recordType() =
        if (this is HimTrainingPartitionRecordV1.Positive) "POSITIVE" else "NEGATIVE"
}

private fun HimTrainingPartitionRecordV1.recordType(): String =
    if (this is HimTrainingPartitionRecordV1.Positive) "POSITIVE" else "NEGATIVE"

private fun HimTrainingPartitionRecordV1.semanticTargetKey(): String =
    when (this) {
        is HimTrainingPartitionRecordV1.Positive -> HimNegativeTrainingExampleIdentityV1.targetKey(positiveExample.target)
        is HimTrainingPartitionRecordV1.Negative ->
            "positive=${HimNegativeTrainingExampleIdentityV1.targetKey(positiveExample.target)}|rejected=${HimNegativeTrainingExampleIdentityV1.targetKey(negativeExample.rejectedTarget)}"
    }
