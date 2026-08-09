package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalFirstSemanticPolicyBatchApplierTest {

    @Test
    fun applyOrPreserveFirstBatchCompletely() {
        val projectDirectory =
            resolveProjectDirectory()

        val sourcePolicySet =
            CanonicalFamilyAxisSemanticPolicySetReader()
                .read(
                    File(
                        projectDirectory,
                        POLICY_SET_PATH
                    )
                )

        val batchResult =
            CanonicalSemanticPolicyImplementationBatchReader()
                .read(
                    File(
                        projectDirectory,
                        BATCH_PATH
                    )
                )

        val expectedEntries =
            CanonicalFirstSemanticPolicyBatchFactory()
                .createEntries()

        val existingEntryCount =
            countExistingBatchEntries(
                sourceEntries =
                    sourcePolicySet.entries,

                expectedEntries =
                    expectedEntries
            )

        val result =
            CanonicalFirstSemanticPolicyBatchApplier()
                .apply(
                    sourcePolicySet =
                        sourcePolicySet,

                    batchResult =
                        batchResult
                )

        assertTrue(result.valid)

        when (existingEntryCount) {
            0 -> {
                assertEquals(
                    sourcePolicySet.policyCount +
                            expectedEntries.size,
                    result.policyCount
                )

                assertEquals(
                    sourcePolicySet
                        .notApplicablePolicyCount +
                            2,
                    result.notApplicablePolicyCount
                )

                assertEquals(
                    sourcePolicySet
                        .curatedAllowedValuesPolicyCount +
                            10,
                    result.curatedAllowedValuesPolicyCount
                )
            }

            expectedEntries.size -> {
                assertEquals(
                    sourcePolicySet,
                    result,
                    "An already applied first batch must be idempotent."
                )
            }

            else ->
                error(
                    "Test fixture contains a partially applied first batch: " +
                            "$existingEntryCount/${expectedEntries.size}."
                )
        }

        assertBatchEntriesPresent(
            resultEntries =
                result.entries,

            expectedEntries =
                expectedEntries
        )
    }

    @Test
    fun applyDeterministicallyAndIdempotently() {
        val projectDirectory =
            resolveProjectDirectory()

        val sourcePolicySet =
            CanonicalFamilyAxisSemanticPolicySetReader()
                .read(
                    File(
                        projectDirectory,
                        POLICY_SET_PATH
                    )
                )

        val batchResult =
            CanonicalSemanticPolicyImplementationBatchReader()
                .read(
                    File(
                        projectDirectory,
                        BATCH_PATH
                    )
                )

        val applier =
            CanonicalFirstSemanticPolicyBatchApplier()

        val first =
            applier.apply(
                sourcePolicySet =
                    sourcePolicySet,

                batchResult =
                    batchResult
            )

        val second =
            applier.apply(
                sourcePolicySet =
                    first,

                batchResult =
                    batchResult
            )

        assertEquals(
            first,
            second,
            "Applying the first semantic policy batch twice must be a no-op."
        )
    }

    private fun countExistingBatchEntries(
        sourceEntries:
        List<CanonicalFamilyAxisSemanticPolicyEntry>,

        expectedEntries:
        List<CanonicalFamilyAxisSemanticPolicyEntry>
    ): Int {
        val sourceIdentities =
            sourceEntries
                .map {
                    it.identityKey
                }
                .toSet()

        return expectedEntries.count {
            it.identityKey in
                    sourceIdentities
        }
    }

    private fun assertBatchEntriesPresent(
        resultEntries:
        List<CanonicalFamilyAxisSemanticPolicyEntry>,

        expectedEntries:
        List<CanonicalFamilyAxisSemanticPolicyEntry>
    ) {
        val resultByIdentity =
            resultEntries
                .associateBy {
                    it.identityKey
                }

        expectedEntries.forEach { expected ->
            assertEquals(
                expected,
                resultByIdentity[
                    expected.identityKey
                ],
                "Missing or divergent first-batch policy: " +
                        expected.identityKey
            )
        }
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
        const val POLICY_SET_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-family-axis-semantic-policies.json"

        const val BATCH_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-batches/" +
                    "canonical-semantic-policy-implementation-batches.json"
    }
}