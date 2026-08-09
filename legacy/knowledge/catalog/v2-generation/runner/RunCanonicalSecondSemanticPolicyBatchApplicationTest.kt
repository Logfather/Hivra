package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSecondSemanticPolicyBatchApplier
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSecondSemanticPolicyBatchFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatchReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunCanonicalSecondSemanticPolicyBatchApplicationTest {

    @Test
    fun implementSecondSemanticPolicyBatch() {
        val projectDirectory =
            resolveProjectDirectory()

        val policyFile =
            File(
                projectDirectory,
                POLICY_SET_PATH
            )

        val batchFile =
            File(
                projectDirectory,
                BATCH_PATH
            )

        val sourcePolicySet =
            CanonicalFamilyAxisSemanticPolicySetReader()
                .read(policyFile)

        val batchResult =
            CanonicalSemanticPolicyImplementationBatchReader()
                .read(batchFile)

        val expectedEntries =
            CanonicalSecondSemanticPolicyBatchFactory()
                .createEntries()

        val sourceByIdentity =
            sourcePolicySet.entries
                .associateBy {
                    it.identityKey
                }

        val alreadyApplied =
            expectedEntries.all { expected ->
                sourceByIdentity[
                    expected.identityKey
                ] ==
                        expected
            }

        val result =
            CanonicalSecondSemanticPolicyBatchApplier()
                .apply(
                    sourcePolicySet =
                        sourcePolicySet,

                    batchResult =
                        batchResult
                )

        assertTrue(result.valid)

        if (alreadyApplied) {
            assertEquals(
                sourcePolicySet,
                result,
                "Already applied second batch must remain unchanged."
            )
        } else {
            assertEquals(
                sourcePolicySet.policyCount +
                        EXPECTED_POLICY_COUNT,
                result.policyCount
            )

            assertEquals(
                sourcePolicySet
                    .curatedAllowedValuesPolicyCount +
                        EXPECTED_POLICY_COUNT,
                result.curatedAllowedValuesPolicyCount
            )

            assertEquals(
                sourcePolicySet
                    .notApplicablePolicyCount,
                result.notApplicablePolicyCount
            )

            assertNotEquals(
                sourcePolicySet.policySetId,
                result.policySetId
            )
        }

        CanonicalFamilyAxisSemanticPolicySetWriter()
            .write(
                policySet =
                    result,

                outputFile =
                    policyFile
            )

        val persisted =
            CanonicalFamilyAxisSemanticPolicySetReader()
                .read(policyFile)

        assertEquals(
            result,
            persisted
        )

        val persistedByIdentity =
            persisted.entries
                .associateBy {
                    it.identityKey
                }

        expectedEntries.forEach { expected ->
            assertEquals(
                expected,
                persistedByIdentity[
                    expected.identityKey
                ]
            )
        }

        println(
            buildString {
                appendLine(
                    "Second semantic policy batch implementation"
                )
                appendLine(
                    "-------------------------------------------"
                )
                appendLine(
                    "Batch: " +
                            SECOND_BATCH_KEY
                )
                appendLine(
                    "Application mode: " +
                            if (alreadyApplied) {
                                "ALREADY_APPLIED"
                            } else {
                                "APPLIED"
                            }
                )
                appendLine(
                    "Previous policy set ID: " +
                            sourcePolicySet.policySetId
                )
                appendLine(
                    "Updated policy set ID: " +
                            persisted.policySetId
                )
                appendLine(
                    "Previous policies: " +
                            sourcePolicySet.policyCount
                )
                appendLine(
                    "Updated policies: " +
                            persisted.policyCount
                )
                appendLine(
                    "Added curated policies: " +
                            if (alreadyApplied) {
                                0
                            } else {
                                EXPECTED_POLICY_COUNT
                            }
                )
                appendLine(
                    "Covered batch policies: " +
                            expectedEntries.size
                )
                append(
                    "Updated policy set valid: " +
                            persisted.valid
                )
            }
        )
    }

    private fun resolveProjectDirectory(): File {
        val workingDirectory =
            File(
                requireNotNull(
                    System.getProperty("user.dir")
                )
            ).canonicalFile

        return when {
            File(
                workingDirectory,
                POLICY_SET_PATH
            ).isFile ->
                workingDirectory

            workingDirectory.name ==
                    "app" ->
                requireNotNull(
                    workingDirectory.parentFile
                )

            else ->
                error(
                    "Could not resolve ShopMe project directory from: " +
                            workingDirectory.absolutePath
                )
        }
    }

    private companion object {
        const val EXPECTED_POLICY_COUNT =
            15

        const val SECOND_BATCH_KEY =
            "semantic-policy-batch-beverages-flavor-profile-001"

        const val POLICY_SET_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-family-axis-semantic-policies.json"

        const val BATCH_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-batches/" +
                    "canonical-semantic-policy-implementation-batches.json"
    }
}