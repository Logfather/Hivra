package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyBatchVocabularyValidator
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatchManifestMerger
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatchManifestReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatchManifestWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalFirstGeneralizedSemanticPolicyWaveFactory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalFirstGeneralizedSemanticPolicyWaveCurationTest {

    @Test
    fun curateFirstGeneralizedSemanticPolicyWave() {
        val projectDirectory =
            resolveProjectDirectory()

        val manifestFile =
            File(
                projectDirectory,
                CURATED_MANIFEST_PATH
            )

        val sourceManifest =
            CanonicalCuratedSemanticPolicyBatchManifestReader()
                .read(manifestFile)

        val additions =
            CanonicalFirstGeneralizedSemanticPolicyWaveFactory()
                .createBatches()

        val vocabularyValidation =
            CanonicalSemanticPolicyBatchVocabularyValidator()
                .validate(
                    entries =
                        additions.flatMap {
                            it.policies
                        }
                )

        assertTrue(
            vocabularyValidation.valid,
            vocabularyValidation.issues.joinToString(
                separator =
                    System.lineSeparator()
            )
        )

        val result =
            CanonicalCuratedSemanticPolicyBatchManifestMerger()
                .merge(
                    source =
                        sourceManifest,

                    additions =
                        additions
                )

        assertTrue(result.valid)

        assertEquals(
            sourceManifest.batchCount +
                    additions.count { addition ->
                        sourceManifest.batches.none {
                            it.sourceImplementationBatchKey ==
                                    addition.sourceImplementationBatchKey
                        }
                    },
            result.batchCount
        )

        CanonicalCuratedSemanticPolicyBatchManifestWriter()
            .write(
                manifest =
                    result,

                outputFile =
                    manifestFile
            )

        val persisted =
            CanonicalCuratedSemanticPolicyBatchManifestReader()
                .read(manifestFile)

        assertEquals(
            result,
            persisted
        )

        println(
            buildString {
                appendLine(
                    "First generalized semantic policy wave curation"
                )
                appendLine(
                    "------------------------------------------------"
                )
                appendLine(
                    "Previous manifest: " +
                            sourceManifest.manifestId
                )
                appendLine(
                    "Updated manifest: " +
                            persisted.manifestId
                )
                appendLine(
                    "Previous batches: " +
                            sourceManifest.batchCount
                )
                appendLine(
                    "Updated batches: " +
                            persisted.batchCount
                )
                appendLine(
                    "Previous policies: " +
                            sourceManifest.policyCount
                )
                appendLine(
                    "Updated policies: " +
                            persisted.policyCount
                )
                appendLine(
                    "Wave batches: " +
                            additions.size
                )
                appendLine(
                    "Wave policies: " +
                            additions.sumOf {
                                it.policyCount
                            }
                )
                append(
                    "Manifest valid: " +
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
                CURATED_MANIFEST_PATH
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
        const val CURATED_MANIFEST_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-curation/" +
                    "canonical-curated-semantic-policy-batches.json"
    }
}