package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalSecondSemanticPolicyBatchApplierTest {

    @Test
    fun applyOrPreserveSecondBatchCompletely() {
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
            CanonicalSecondSemanticPolicyBatchFactory()
                .createEntries()

        val existingEntryCount =
            countExistingEntries(
                sourceEntries =
                    sourcePolicySet.entries,

                expectedEntries =
                    expectedEntries
            )

        val result =
            CanonicalSecondSemanticPolicyBatchApplier()
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
            }

            EXPECTED_POLICY_COUNT -> {
                assertEquals(
                    sourcePolicySet,
                    result,
                    "An already applied second batch must be a no-op."
                )
            }

            else ->
                error(
                    "Test fixture contains a partially applied second batch: " +
                            "$existingEntryCount/$EXPECTED_POLICY_COUNT."
                )
        }

        assertExpectedEntriesPresent(
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
            CanonicalSecondSemanticPolicyBatchApplier()

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
            "Applying the second semantic policy batch twice must be a no-op."
        )
    }

    private fun countExistingEntries(
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

    private fun assertExpectedEntriesPresent(
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
                "Missing or divergent second-batch policy: " +
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
        const val EXPECTED_POLICY_COUNT =
            15

        const val POLICY_SET_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-family-axis-semantic-policies.json"

        const val BATCH_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-batches/" +
                    "canonical-semantic-policy-implementation-batches.json"
    }
}