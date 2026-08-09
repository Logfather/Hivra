package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyBatchVocabularyValidator
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatchReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalThirdGeneralizedSemanticPolicyWaveFactoryTest {

    @Test
    fun createCompleteThirdGeneralizedWave() {

        val batches =
            createOrReadWaveBatches()

        assertEquals(
            EXPECTED_BATCH_COUNT,
            batches.size
        )

        assertEquals(
            EXPECTED_POLICY_COUNT,
            batches.sumOf {
                it.policyCount
            }
        )

        assertTrue(
            batches.all {
                it.valid &&
                        it.complete
            }
        )

        assertEquals(
            EXPECTED_CATEGORIES_AND_AXES,
            batches
                .map {
                    it.category to
                            it.axis
                }
                .sortedWith(
                    compareBy<Pair<String, String>> {
                        it.first
                    }.thenBy {
                        it.second
                    }
                )
        )

        val policies =
            batches.flatMap {
                it.policies
            }

        assertEquals(
            EXPECTED_POLICY_COUNT,
            policies
                .map {
                    it.identityKey
                }
                .distinct()
                .size
        )

        assertEquals(
            EXPECTED_CURATED_POLICY_COUNT,
            policies.count {
                it.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .CURATED_ALLOWED_VALUES
            }
        )

        assertEquals(
            EXPECTED_NOT_APPLICABLE_POLICY_COUNT,
            policies.count {
                it.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .NOT_APPLICABLE
            }
        )
    }

    @Test
    fun referenceOnlyCanonicalVariantValues() {
        val policies =
            createOrReadWaveBatches()
                .flatMap { batch ->
                    batch.policies
                }

        val result =
            CanonicalSemanticPolicyBatchVocabularyValidator()
                .validate(
                    entries =
                        policies
                )

        assertTrue(
            result.valid,
            result.issues.joinToString(
                separator =
                    System.lineSeparator()
            )
        )

        assertEquals(
            0,
            result.issueCount
        )
    }

    @Test
    fun preserveOrCoverEverySelectedBatchExactly() {
        val openBatches =
            readOpenBatches()

        val curatedManifest =
            readCuratedManifest()

        val batches =
            createOrReadWaveBatches()

        val openByKey =
            openBatches.batches
                .associateBy { batch ->
                    batch.batchKey
                }

        val persistedByKey =
            curatedManifest.batches
                .associateBy { batch ->
                    batch.sourceImplementationBatchKey
                }

        assertEquals(
            EXPECTED_BATCH_COUNT,
            batches.size
        )

        batches.forEach { batch ->
            val openBatch =
                openByKey[
                    batch.sourceImplementationBatchKey
                ]

            if (openBatch != null) {
                assertEquals(
                    openBatch.gaps
                        .map { gap ->
                            gap.gapKey
                        }
                        .sorted(),

                    batch.policies
                        .map { policy ->
                            policy.identityKey
                        }
                        .sorted(),

                    "Open Wave-3 batch must be covered exactly: " +
                            batch.sourceImplementationBatchKey
                )
            } else {
                assertEquals(
                    batch,
                    persistedByKey[
                        batch.sourceImplementationBatchKey
                    ],
                    "A closed Wave-3 batch must be preserved exactly " +
                            "in the curated manifest."
                )
            }
        }
    }

    private fun createOrReadWaveBatches():
            List<CanonicalCuratedSemanticPolicyBatch> {
        val projectDirectory =
            resolveProjectDirectory()

        val curatedManifestFile =
            File(
                projectDirectory,
                CURATED_MANIFEST_PATH
            )

        if (
            curatedManifestFile.isFile &&
            curatedManifestFile.length() > 0L
        ) {
            val persistedWaveBatches =
                CanonicalCuratedSemanticPolicyBatchManifestReader()
                    .read(
                        curatedManifestFile
                    )
                    .batches
                    .filter { batch ->
                        batch.curationId.startsWith(
                            WAVE_CURATION_ID_PREFIX
                        )
                    }
                    .sortedBy { batch ->
                        batch.sourceImplementationBatchKey
                    }

            if (persistedWaveBatches.isNotEmpty()) {
                require(
                    persistedWaveBatches.size ==
                            EXPECTED_BATCH_COUNT
                ) {
                    "Persisted generalized Wave 3 is incomplete: " +
                            "expected=$EXPECTED_BATCH_COUNT, " +
                            "actual=${persistedWaveBatches.size}."
                }

                require(
                    persistedWaveBatches.sumOf { batch ->
                        batch.policyCount
                    } ==
                            EXPECTED_POLICY_COUNT
                ) {
                    "Persisted generalized Wave 3 has an unexpected " +
                            "policy count."
                }

                require(
                    persistedWaveBatches.all { batch ->
                        batch.valid &&
                                batch.complete
                    }
                ) {
                    "Persisted generalized Wave 3 contains an invalid batch."
                }

                return persistedWaveBatches
            }
        }

        return CanonicalThirdGeneralizedSemanticPolicyWaveFactory()
            .createBatches(
                openBatches =
                    readOpenBatches(),

                curatedManifest =
                    readCuratedManifest()
            )
    }

    private fun readOpenBatches() =
        CanonicalSemanticPolicyImplementationBatchReader()
            .read(
                File(
                    resolveProjectDirectory(),
                    OPEN_BATCH_PATH
                )
            )

    private fun readCuratedManifest() =
        CanonicalCuratedSemanticPolicyBatchManifestReader()
            .read(
                File(
                    resolveProjectDirectory(),
                    CURATED_MANIFEST_PATH
                )
            )

    private fun resolveProjectDirectory(): File {
        val workingDirectory =
            File(
                requireNotNull(
                    System.getProperty("user.dir")
                )
            ).canonicalFile

        fun containsRequiredArtifacts(
            directory: File
        ): Boolean =
            File(
                directory,
                OPEN_BATCH_PATH
            ).isFile ||
                    File(
                        directory,
                        CURATED_MANIFEST_PATH
                    ).isFile

        return when {
            containsRequiredArtifacts(
                workingDirectory
            ) ->
                workingDirectory

            workingDirectory.name ==
                    "app" &&
                    containsRequiredArtifacts(
                        requireNotNull(
                            workingDirectory.parentFile
                        )
                    ) ->
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

        const val WAVE_CURATION_ID_PREFIX =
            "curated-wave-003-"

        const val EXPECTED_BATCH_COUNT =
            3

        const val EXPECTED_POLICY_COUNT =
            33

        const val EXPECTED_CURATED_POLICY_COUNT =
            19

        const val EXPECTED_NOT_APPLICABLE_POLICY_COUNT =
            14

        const val OPEN_BATCH_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-batches/" +
                    "canonical-semantic-policy-implementation-batches.json"

        val EXPECTED_CATEGORIES_AND_AXES =
            listOf(
                "baking-ingredients" to
                        "PHYSICAL_FORM",

                "beverages" to
                        "FOOD_TYPE",

                "meat" to
                        "CUT_FORM"
            )
    }
}