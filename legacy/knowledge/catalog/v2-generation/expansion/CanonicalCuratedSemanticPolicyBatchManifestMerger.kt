package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation

import java.security.MessageDigest

class CanonicalCuratedSemanticPolicyBatchManifestMerger {

    fun merge(
        source:
        CanonicalCuratedSemanticPolicyBatchManifest,

        additions:
        List<CanonicalCuratedSemanticPolicyBatch>
    ): CanonicalCuratedSemanticPolicyBatchManifest {
        require(source.valid)
        require(additions.isNotEmpty())

        require(
            additions
                .map {
                    it.sourceImplementationBatchKey
                }
                .distinct()
                .size ==
                    additions.size
        ) {
            "Added curated batches must have unique batch keys."
        }

        val mergedByBatchKey =
            source.batches
                .associateBy {
                    it.sourceImplementationBatchKey
                }
                .toMutableMap()

        additions.forEach { addition ->
            require(addition.valid)

            val existing =
                mergedByBatchKey[
                    addition.sourceImplementationBatchKey
                ]

            require(
                existing == null ||
                        existing == addition
            ) {
                "Curated batch '${addition.sourceImplementationBatchKey}' " +
                        "already exists with divergent content."
            }

            mergedByBatchKey[
                addition.sourceImplementationBatchKey
            ] =
                addition
        }

        val batches =
            mergedByBatchKey
                .values
                .sortedBy {
                    it.sourceImplementationBatchKey
                }

        val policyCount =
            batches.sumOf {
                it.policyCount
            }

        return CanonicalCuratedSemanticPolicyBatchManifest(
            version =
                CanonicalCuratedSemanticPolicyBatchManifest
                    .CURRENT_VERSION,

            manifestId =
                createManifestId(
                    batches =
                        batches
                ),

            batchCount =
                batches.size,

            policyCount =
                policyCount,

            batches =
                batches,

            complete =
                batches.all {
                    it.complete &&
                            it.valid
                },

            deterministicOrderValid =
                batches ==
                        batches.sortedBy {
                            it.sourceImplementationBatchKey
                        },

            valid =
                batches.isNotEmpty() &&
                        batches.all {
                            it.valid &&
                                    it.complete
                        }
        )
    }

    private fun createManifestId(
        batches:
        List<CanonicalCuratedSemanticPolicyBatch>
    ): String {
        val payload =
            buildString {
                batches.forEach { batch ->
                    append(
                        batch.sourceImplementationBatchKey
                    )
                    append('|')
                    append(batch.category)
                    append('|')
                    append(batch.axis)
                    append('\n')

                    batch.policies.forEach { policy ->
                        append(policy.familyKey)
                        append('|')
                        append(policy.axis.name)
                        append('|')
                        append(policy.policyType.name)
                        append('|')
                        append(
                            policy.allowedValues
                                .joinToString(",")
                        )
                        append('|')
                        append(policy.rationale)
                        append('\n')
                    }
                }
            }

        val hash =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    payload.toByteArray(
                        Charsets.UTF_8
                    )
                )
                .joinToString("") {
                    "%02x".format(it)
                }
                .take(16)

        return "canonical-curated-semantic-policy-manifest-v1-$hash"
    }
}