package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalFirstSemanticPolicyBatchApplier
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalFirstSemanticPolicyBatchFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatchReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunCanonicalFirstSemanticPolicyBatchApplicationTest {

    @Test
    fun implementFirstSemanticPolicyBatch() {
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
            CanonicalFirstSemanticPolicyBatchFactory()
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
            CanonicalFirstSemanticPolicyBatchApplier()
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
                "Already applied first batch must remain unchanged."
            )
        } else {
            assertEquals(
                sourcePolicySet.policyCount +
                        expectedEntries.size,
                result.policyCount
            )

            assertEquals(
                sourcePolicySet
                    .curatedAllowedValuesPolicyCount +
                        10,
                result.curatedAllowedValuesPolicyCount
            )

            assertEquals(
                sourcePolicySet
                    .notApplicablePolicyCount +
                        2,
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
                    "First semantic policy batch implementation"
                )
                appendLine(
                    "------------------------------------------"
                )
                appendLine(
                    "Batch: " +
                            FIRST_BATCH_KEY
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

            workingDirectory.name == "app" ->
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
        const val FIRST_BATCH_KEY =
            "semantic-policy-batch-vegetables-plant-species-001"

        const val POLICY_SET_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-family-axis-semantic-policies.json"

        const val BATCH_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-batches/" +
                    "canonical-semantic-policy-implementation-batches.json"
    }
}