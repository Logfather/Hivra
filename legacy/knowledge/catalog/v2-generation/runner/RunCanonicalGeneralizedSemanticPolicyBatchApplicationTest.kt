package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatchReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.application.CanonicalSemanticPolicyBatchManifestApplier
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalAppliedSemanticPolicyBatchManifestFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatchManifestReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatchManifestWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalGeneralizedSemanticPolicyBatchApplicationTest {

    @Test
    fun applyCuratedSemanticPolicyManifest() {
        val projectDirectory =
            resolveProjectDirectory()

        val manifestFile =
            File(
                projectDirectory,
                CURATED_MANIFEST_PATH
            )

        if (
            !manifestFile.isFile ||
            manifestFile.length() == 0L
        ) {
            CanonicalCuratedSemanticPolicyBatchManifestWriter()
                .write(
                    manifest =
                        CanonicalAppliedSemanticPolicyBatchManifestFactory()
                            .create(),

                    outputFile =
                        manifestFile
                )
        }

        val manifest =
            CanonicalCuratedSemanticPolicyBatchManifestReader()
                .read(manifestFile)

        val policyFile =
            File(
                projectDirectory,
                POLICY_SET_PATH
            )

        val sourcePolicySet =
            CanonicalFamilyAxisSemanticPolicySetReader()
                .read(policyFile)

        val openBatches =
            CanonicalSemanticPolicyImplementationBatchReader()
                .read(
                    File(
                        projectDirectory,
                        OPEN_BATCH_PATH
                    )
                )

        val result =
            CanonicalSemanticPolicyBatchManifestApplier()
                .apply(
                    sourcePolicySet =
                        sourcePolicySet,

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

        CanonicalFamilyAxisSemanticPolicySetWriter()
            .write(
                policySet =
                    result.resultingPolicySet,

                outputFile =
                    policyFile
            )

        val persisted =
            CanonicalFamilyAxisSemanticPolicySetReader()
                .read(policyFile)

        assertEquals(
            result.resultingPolicySet,
            persisted
        )

        println(
            buildString {
                appendLine(
                    "Generalized semantic policy batch application"
                )
                appendLine(
                    "---------------------------------------------"
                )
                appendLine(
                    "Manifest: " +
                            manifest.manifestId
                )
                appendLine(
                    "Curated batches: " +
                            manifest.batchCount
                )
                appendLine(
                    "Curated policies: " +
                            manifest.policyCount
                )
                appendLine(
                    "Newly applied batches: " +
                            result.newlyAppliedBatchCount
                )
                appendLine(
                    "Already applied batches: " +
                            result.alreadyAppliedBatchCount
                )
                appendLine(
                    "Newly applied policies: " +
                            result.newlyAppliedPolicyCount
                )
                appendLine(
                    "Already applied policies: " +
                            result.alreadyAppliedPolicyCount
                )
                appendLine(
                    "Idempotent: " +
                            result.idempotent
                )
                append(
                    "Application valid: " +
                            result.valid
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
        const val POLICY_SET_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-family-axis-semantic-policies.json"

        const val OPEN_BATCH_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-batches/" +
                    "canonical-semantic-policy-implementation-batches.json"

        const val CURATED_MANIFEST_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-curation/" +
                    "canonical-curated-semantic-policy-batches.json"
    }
}