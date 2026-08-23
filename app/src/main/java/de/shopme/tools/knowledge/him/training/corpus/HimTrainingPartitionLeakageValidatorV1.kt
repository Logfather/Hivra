package de.shopme.tools.knowledge.him.training.corpus

object HimTrainingPartitionLeakageValidatorV1 {
    fun validate(manifest: HimTrainingPartitionManifestV1): HimTrainingPartitionValidationResultV1 {
        val diagnostics = mutableListOf<HimTrainingPartitionLeakageDiagnosticV1>()
        val assignments = manifest.assignments

        if (manifest.policyVersion != HimTrainingPartitionContractV1.POLICY_VERSION) {
            diagnostics += diagnostic(
                HimTrainingPartitionLeakageLevelV1.INVALID_DETERMINISTIC_ASSIGNMENT,
                "policy",
                assignments.map { it.record.recordReference }.ifEmpty { listOf("manifest") },
                "Unsupported partition policy version.",
            )
        }
        val expectedDigest = HimTrainingPartitionManifestIdentityV1.digest(manifest.policyVersion, assignments)
        if (manifest.logicalDigest != expectedDigest) {
            diagnostics += diagnostic(
                HimTrainingPartitionLeakageLevelV1.INVALID_DETERMINISTIC_ASSIGNMENT,
                "manifest-digest",
                assignments.map { it.record.recordReference }.ifEmpty { listOf("manifest") },
                "Manifest logical digest is not deterministic for its assignments.",
            )
        }

        assignments.groupBy { it.record.recordReference }
            .filterValues { it.size > 1 }
            .forEach { (reference, duplicates) ->
                diagnostics += diagnostic(
                    HimTrainingPartitionLeakageLevelV1.EXACT_EXAMPLE_LEAKAGE,
                    reference,
                    duplicates.map { it.record.recordReference },
                    "The same training record reference occurs more than once.",
                )
            }

        val positiveAssignments = assignments
            .filterIsInstancePositive()
            .associateBy { it.record.positiveExample.exampleReference }
        val resolvedGroups = mutableMapOf<String, HimTrainingFamilyGroupReferenceV1>()
        assignments.forEach { assignment ->
            when (val resolution = resolve(assignment.record)) {
                is HimTrainingFamilyGroupResolutionV1.NotYetGroupable -> diagnostics += diagnostic(
                    HimTrainingPartitionLeakageLevelV1.INVALID_DETERMINISTIC_ASSIGNMENT,
                    assignment.record.recordReference,
                    listOf(assignment.record.recordReference),
                    resolution.reason,
                )
                is HimTrainingFamilyGroupResolutionV1.Resolved -> {
                    resolvedGroups[assignment.record.recordReference] = resolution.groupReference
                    if (resolution.groupReference != assignment.groupReference) {
                        diagnostics += diagnostic(
                            HimTrainingPartitionLeakageLevelV1.CANONICAL_FAMILY_LEAKAGE,
                            assignment.record.recordReference,
                            listOf(assignment.record.recordReference),
                            "Assignment group does not match the positive Ground-Truth family.",
                        )
                    }
                    val expectedPartition = HimTrainingPartitionPolicyV1.partitionForGroup(resolution.groupReference)
                    if (assignment.partition != expectedPartition) {
                        diagnostics += diagnostic(
                            HimTrainingPartitionLeakageLevelV1.INVALID_DETERMINISTIC_ASSIGNMENT,
                            resolution.groupReference.value,
                            listOf(assignment.record.recordReference),
                            "Assignment partition does not match the deterministic group bucket.",
                        )
                    }
                }
            }
        }

        assignments.filterIsInstanceNegative().forEach { assignment ->
            val negative = (assignment.record as HimTrainingPartitionRecordV1.Negative).negativeExample
            val positiveAssignment = positiveAssignments[negative.provenance.positiveExampleReference]
            if (positiveAssignment == null) {
                diagnostics += diagnostic(
                    HimTrainingPartitionLeakageLevelV1.DERIVED_NEGATIVE_LEAKAGE,
                    negative.provenance.positiveExampleReference.value,
                    listOf(negative.reference.value),
                    "Negative record references a positive example absent from the manifest.",
                )
            } else {
                if (positiveAssignment.partition != assignment.partition) {
                    diagnostics += diagnostic(
                        HimTrainingPartitionLeakageLevelV1.DERIVED_NEGATIVE_LEAKAGE,
                        negative.provenance.positiveExampleReference.value,
                        listOf(positiveAssignment.record.recordReference, assignment.record.recordReference),
                        "Negative record is assigned to a different partition than its positive example.",
                    )
                }
                if (positiveAssignment.groupReference != assignment.groupReference) {
                    diagnostics += diagnostic(
                        HimTrainingPartitionLeakageLevelV1.CANONICAL_FAMILY_LEAKAGE,
                        assignment.groupReference.value,
                        listOf(positiveAssignment.record.recordReference, assignment.record.recordReference),
                        "Negative record does not inherit its positive family group.",
                    )
                }
            }
        }

        assignments.groupBy { it.groupReference }
            .filterValues { it.map { assignment -> assignment.partition }.distinct().size > 1 }
            .forEach { (group, groupAssignments) ->
                diagnostics += diagnostic(
                    HimTrainingPartitionLeakageLevelV1.CANONICAL_FAMILY_LEAKAGE,
                    group.value,
                    groupAssignments.map { it.record.recordReference },
                    "Canonical Family appears in more than one partition.",
                )
            }

        assignments.groupBy { HimTrainingPartitionLineageIdentityV1.resolve(it.record.positiveExample) }
            .filterKeys { it != null }
            .filterValues { it.map { assignment -> assignment.partition }.distinct().size > 1 }
            .forEach { (lineage, lineageAssignments) ->
                diagnostics += diagnostic(
                    HimTrainingPartitionLeakageLevelV1.LINEAGE_LEAKAGE,
                    requireNotNull(lineage).value,
                    lineageAssignments.map { it.record.recordReference },
                    "The same Candidate/Validation/Promotion/Mutation lineage crosses partitions.",
                )
            }

        assignments.groupBy { HimTrainingPartitionManifestIdentityV1.semanticKey(it.record) }
            .filterValues { it.map { assignment -> assignment.partition }.distinct().size > 1 }
            .forEach { (semanticKey, semanticAssignments) ->
                diagnostics += diagnostic(
                    HimTrainingPartitionLeakageLevelV1.DUPLICATE_SEMANTIC_TARGET_LEAKAGE,
                    sha256(semanticKey),
                    semanticAssignments.map { it.record.recordReference },
                    "The same deterministic input/target semantic identity crosses partitions.",
                )
            }

        diagnostics += contextOnlyDiagnostics(assignments)
        return HimTrainingPartitionValidationResultV1(
            valid = diagnostics.none { it.fatal },
            diagnostics = diagnostics.sortedWith(compareBy({ it.level.name }, { it.key }, { it.recordReferences.joinToString("|") })),
        )
    }

    fun validateOrThrow(manifest: HimTrainingPartitionManifestV1) {
        val result = validate(manifest)
        require(result.valid) {
            result.diagnostics.filter { it.fatal }.joinToString("; ") { "${it.level}: ${it.message}" }
        }
    }

    private fun contextOnlyDiagnostics(
        assignments: List<HimTrainingPartitionAssignmentV1>,
    ): List<HimTrainingPartitionLeakageDiagnosticV1> {
        val partitionsByFamily = assignments.groupBy { it.groupReference }
            .mapValues { (_, values) -> values.map { it.partition }.toSet() }
        val diagnostics = mutableListOf<HimTrainingPartitionLeakageDiagnosticV1>()
        assignments.forEach { assignment ->
            val currentFamily = assignment.groupReference
            assignment.record.positiveExample.input.canonicalContext
                .map { HimTrainingFamilyGroupReferenceV1.canonical(it.canonicalId) }
                .filter { it != currentFamily }
                .distinct()
                .forEach { contextFamily ->
                    val contextPartitions = partitionsByFamily[contextFamily].orEmpty()
                    if (contextPartitions.any { it != assignment.partition }) {
                        diagnostics += diagnostic(
                            HimTrainingPartitionLeakageLevelV1.CONTEXT_ONLY_CROSS_PARTITION_REFERENCE,
                            "${currentFamily.value}->${contextFamily.value}",
                            listOf(assignment.record.recordReference),
                            "Canonical is visible in model context but belongs to another partition; this is not target-family leakage.",
                            fatal = false,
                        )
                    }
                }
        }
        return diagnostics.distinctBy { it.level to it.key }
    }

    private fun resolve(record: HimTrainingPartitionRecordV1) =
        when (record) {
            is HimTrainingPartitionRecordV1.Positive -> HimTrainingFamilyGroupResolverV1.resolve(record.positiveExample)
            is HimTrainingPartitionRecordV1.Negative -> HimTrainingFamilyGroupResolverV1.resolve(record.negativeExample)
        }

    private fun diagnostic(
        level: HimTrainingPartitionLeakageLevelV1,
        key: String,
        recordReferences: List<String>,
        message: String,
        fatal: Boolean = true,
    ) = HimTrainingPartitionLeakageDiagnosticV1(level, fatal, key, recordReferences.sorted(), message)

    private fun List<HimTrainingPartitionAssignmentV1>.filterIsInstancePositive() =
        filter { it.record is HimTrainingPartitionRecordV1.Positive }

    private fun List<HimTrainingPartitionAssignmentV1>.filterIsInstanceNegative() =
        filter { it.record is HimTrainingPartitionRecordV1.Negative }

    private fun sha256(value: String): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
