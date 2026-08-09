package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalFirstSemanticPolicyBatchFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSecondSemanticPolicyBatchFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalThirdSemanticPolicyBatchFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import java.security.MessageDigest

class CanonicalAppliedSemanticPolicyBatchManifestFactory {

    fun create():
            CanonicalCuratedSemanticPolicyBatchManifest {
        val batches =
            listOf(
                batch(
                    sourceImplementationBatchKey =
                        "semantic-policy-batch-vegetables-plant-species-001",

                    category =
                        "vegetables",

                    axis =
                        "PLANT_SPECIES",

                    policies =
                        CanonicalFirstSemanticPolicyBatchFactory()
                            .createEntries()
                ),

                batch(
                    sourceImplementationBatchKey =
                        "semantic-policy-batch-beverages-flavor-profile-001",

                    category =
                        "beverages",

                    axis =
                        "FLAVOR_PROFILE",

                    policies =
                        CanonicalSecondSemanticPolicyBatchFactory()
                            .createEntries()
                ),

                batch(
                    sourceImplementationBatchKey =
                        "semantic-policy-batch-fruit-plant-species-001",

                    category =
                        "fruit",

                    axis =
                        "PLANT_SPECIES",

                    policies =
                        CanonicalThirdSemanticPolicyBatchFactory()
                            .createEntries()
                )
            )
                .sortedBy {
                    it.sourceImplementationBatchKey
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
                batches.sumOf {
                    it.policyCount
                },

            batches =
                batches,

            complete =
                batches.all {
                    it.complete
                },

            deterministicOrderValid =
                batches ==
                        batches.sortedBy {
                            it.sourceImplementationBatchKey
                        },

            valid =
                batches.isNotEmpty() &&
                        batches.all {
                            it.valid
                        }
        )
    }

    private fun batch(
        sourceImplementationBatchKey: String,
        category: String,
        axis: String,
        policies:
        List<CanonicalFamilyAxisSemanticPolicyEntry>
    ): CanonicalCuratedSemanticPolicyBatch {
        val sortedPolicies =
            policies.sortedWith(
                compareBy<
                        CanonicalFamilyAxisSemanticPolicyEntry
                        > {
                    it.familyKey
                }.thenBy {
                    it.axis.name
                }
            )

        return CanonicalCuratedSemanticPolicyBatch(
            version =
                CanonicalCuratedSemanticPolicyBatch
                    .CURRENT_VERSION,

            curationId =
                "curated-$sourceImplementationBatchKey",

            sourceImplementationBatchKey =
                sourceImplementationBatchKey,

            category =
                category,

            axis =
                axis,

            policyCount =
                sortedPolicies.size,

            policies =
                sortedPolicies,

            complete =
                sortedPolicies.all {
                    it.active &&
                            it.complete
                },

            valid =
                sortedPolicies.isNotEmpty() &&
                        sortedPolicies.all {
                            it.active &&
                                    it.complete
                        }
        )
    }

    private fun createManifestId(
        batches:
        List<CanonicalCuratedSemanticPolicyBatch>
    ): String {
        val canonicalPayload =
            buildString {
                batches.forEach { batch ->
                    append(
                        batch.sourceImplementationBatchKey
                    )
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
                        append('\n')
                    }
                }
            }

        val hash =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    canonicalPayload
                        .toByteArray(Charsets.UTF_8)
                )
                .joinToString("") {
                    "%02x".format(it)
                }
                .take(16)

        return "canonical-curated-semantic-policy-manifest-v1-$hash"
    }
}