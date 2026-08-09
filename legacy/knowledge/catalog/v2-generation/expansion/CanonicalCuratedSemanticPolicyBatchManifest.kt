package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation

data class CanonicalCuratedSemanticPolicyBatchManifest(
    val version: Int,

    val manifestId: String,

    val batchCount: Int,

    val policyCount: Int,

    val batches:
    List<CanonicalCuratedSemanticPolicyBatch>,

    val complete: Boolean,

    val deterministicOrderValid: Boolean,

    val valid: Boolean
) {

    init {
        require(version > 0)
        require(manifestId.isNotBlank())

        require(batchCount == batches.size)
        require(batchCount > 0)

        require(
            policyCount ==
                    batches.sumOf {
                        it.policyCount
                    }
        )

        require(
            batches
                .map {
                    it.sourceImplementationBatchKey
                }
                .distinct()
                .size ==
                    batches.size
        ) {
            "Curated manifest must not contain duplicate implementation batches."
        }

        require(
            batches
                .flatMap {
                    it.policies
                }
                .map {
                    it.identityKey
                }
                .distinct()
                .size ==
                    policyCount
        ) {
            "Curated manifest must not contain duplicate policy identities."
        }

        require(
            deterministicOrderValid ==
                    (
                            batches ==
                                    batches.sortedBy {
                                        it.sourceImplementationBatchKey
                                    }
                            )
        )

        require(
            complete ==
                    batches.all {
                        it.complete &&
                                it.valid
                    }
        )

        require(
            valid ==
                    (
                            complete &&
                                    deterministicOrderValid &&
                                    batchCount > 0 &&
                                    policyCount > 0
                            )
        )
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}