package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyBatchVocabularyValidator
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatchManifestMerger
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatchManifestReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatchManifestWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalSecondGeneralizedSemanticPolicyWaveFactory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalSecondGeneralizedSemanticPolicyWaveCurationTest {

    @Test
    fun curateSecondGeneralizedSemanticPolicyWave() {
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
            CanonicalSecondGeneralizedSemanticPolicyWaveFactory()
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

        val existingBatchKeys =
            sourceManifest.batches
                .map {
                    it.sourceImplementationBatchKey
                }
                .toSet()

        val newlyCuratedBatchCount =
            additions.count {
                it.sourceImplementationBatchKey !in
                        existingBatchKeys
            }

        val newlyCuratedPolicyCount =
            additions
                .filter {
                    it.sourceImplementationBatchKey !in
                            existingBatchKeys
                }
                .sumOf {
                    it.policyCount
                }

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
                    newlyCuratedBatchCount,
            result.batchCount
        )

        assertEquals(
            sourceManifest.policyCount +
                    newlyCuratedPolicyCount,
            result.policyCount
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

        additions.forEach { addition ->
            assertEquals(
                addition,
                persisted.batches.single {
                    it.sourceImplementationBatchKey ==
                            addition.sourceImplementationBatchKey
                }
            )
        }

        println(
            buildString {
                appendLine(
                    "Second generalized semantic policy wave curation"
                )
                appendLine(
                    "-------------------------------------------------"
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
                appendLine(
                    "Newly curated batches: " +
                            newlyCuratedBatchCount
                )
                appendLine(
                    "Newly curated policies: " +
                            newlyCuratedPolicyCount
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
                ) {
                    "System property 'user.dir' is not available."
                }
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
                ) {
                    "Could not resolve project directory from app directory."
                }

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