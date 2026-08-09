package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.application

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatchReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalAppliedSemanticPolicyBatchManifestFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalSemanticPolicyBatchManifestApplierTest {

    @Test
    fun preserveAlreadyAppliedCuratedBatches() {
        val projectDirectory =
            resolveProjectDirectory()

        val policySet =
            CanonicalFamilyAxisSemanticPolicySetReader()
                .read(
                    File(
                        projectDirectory,
                        POLICY_SET_PATH
                    )
                )

        val openBatches =
            CanonicalSemanticPolicyImplementationBatchReader()
                .read(
                    File(
                        projectDirectory,
                        OPEN_BATCH_PATH
                    )
                )

        val manifest =
            CanonicalAppliedSemanticPolicyBatchManifestFactory()
                .create()

        val result =
            CanonicalSemanticPolicyBatchManifestApplier()
                .apply(
                    sourcePolicySet =
                        policySet,

                    openImplementationBatches =
                        openBatches,

                    curatedManifest =
                        manifest
                )

        assertTrue(
            result.valid,
            result.blockers.joinToString(
                separator =
                    System.lineSeparator()
            )
        )

        assertTrue(result.idempotent)

        assertEquals(
            0,
            result.newlyAppliedBatchCount
        )

        assertEquals(
            manifest.batchCount,
            result.alreadyAppliedBatchCount
        )

        assertEquals(
            0,
            result.newlyAppliedPolicyCount
        )

        assertEquals(
            manifest.policyCount,
            result.alreadyAppliedPolicyCount
        )

        assertEquals(
            policySet,
            result.resultingPolicySet
        )
    }

    @Test
    fun applyDeterministically() {
        val projectDirectory =
            resolveProjectDirectory()

        val policySet =
            CanonicalFamilyAxisSemanticPolicySetReader()
                .read(
                    File(
                        projectDirectory,
                        POLICY_SET_PATH
                    )
                )

        val openBatches =
            CanonicalSemanticPolicyImplementationBatchReader()
                .read(
                    File(
                        projectDirectory,
                        OPEN_BATCH_PATH
                    )
                )

        val manifest =
            CanonicalAppliedSemanticPolicyBatchManifestFactory()
                .create()

        val applier =
            CanonicalSemanticPolicyBatchManifestApplier()

        val first =
            applier.apply(
                policySet,
                openBatches,
                manifest
            )

        val second =
            applier.apply(
                first.resultingPolicySet,
                openBatches,
                manifest
            )

        assertEquals(
            first.resultingPolicySet,
            second.resultingPolicySet
        )

        assertTrue(second.idempotent)
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

        const val OPEN_BATCH_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-batches/" +
                    "canonical-semantic-policy-implementation-batches.json"
    }
}