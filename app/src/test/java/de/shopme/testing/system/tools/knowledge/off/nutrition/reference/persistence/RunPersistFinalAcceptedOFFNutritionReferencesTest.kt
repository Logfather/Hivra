package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.persistence

import de.shopme.tools.knowledge.off.extractor.OFFCandidateExtractor
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceCandidateGenerator
import de.shopme.tools.knowledge.off.nutrition.reference.persistence.OFFAcceptedNutritionReferenceManifestReader
import de.shopme.tools.knowledge.off.nutrition.reference.persistence.OFFAcceptedNutritionReferenceReader
import de.shopme.tools.knowledge.off.nutrition.reference.persistence.PersistAcceptedOFFNutritionReferences
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityFilter
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityPolicyReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunPersistFinalAcceptedOFFNutritionReferencesTest {

    @Test
    fun persistFinalAcceptedOFFNutritionReferences() {

        val repositoryRoot =
            resolveRepositoryRoot()

        val inputFile =
            repositoryRoot.resolve(
                "data/raw/openfoodfacts/" +
                        "openfoodfacts-products.jsonl.gz"
            )

        val policyFile =
            repositoryRoot.resolve(
                "data/generated/knowledge/policies/" +
                        "off-nutrition-reference-quality-policy.json"
            )

        val referenceOutputFile =
            repositoryRoot.resolve(
                "data/generated/knowledge/references/off/" +
                        "off-nutrition-references.accepted.jsonl.gz"
            )

        val manifestOutputFile =
            repositoryRoot.resolve(
                "data/generated/knowledge/references/off/" +
                        "off-nutrition-references.accepted.manifest.json"
            )

        require(inputFile.isFile) {
            "OFF input file does not exist: " +
                    inputFile.absolutePath
        }

        require(policyFile.isFile) {
            "OFF nutrition quality policy does not exist: " +
                    policyFile.absolutePath
        }

        val policy =
            OFFNutritionReferenceQualityPolicyReader()
                .read(
                    policyFile
                )

        val extractor =
            OFFCandidateExtractor()

        val candidateGenerator =
            OFFNutritionReferenceCandidateGenerator()

        val qualityFilter =
            OFFNutritionReferenceQualityFilter()

        val result =
            PersistAcceptedOFFNutritionReferences()
                .run(
                    inputFile =
                        inputFile,
                    policyFile =
                        policyFile,
                    policy =
                        policy,
                    referenceOutputFile =
                        referenceOutputFile,
                    manifestOutputFile =
                        manifestOutputFile,
                    forEachCandidate =
                        { consumer ->

                            extractor.forEachCandidate(
                                file =
                                    inputFile,
                                maxCandidates =
                                    null
                            ) { extractedCandidate ->

                                val generationResult =
                                    candidateGenerator.generate(
                                        candidates =
                                            listOf(
                                                extractedCandidate
                                            )
                                    )

                                generationResult.candidates
                                    .forEach { referenceCandidate ->
                                        consumer(
                                            referenceCandidate
                                        )
                                    }
                            }
                        },
                    isAccepted =
                        { candidate ->

                            val filterResult =
                                qualityFilter.filter(
                                    candidates =
                                        listOf(
                                            candidate
                                        )
                                )

                            filterResult.acceptedCandidates
                                .isNotEmpty()
                        }
                )

        assertTrue(
            result.referenceFile.isFile
        )

        assertTrue(
            result.manifestFile.isFile
        )

        assertEquals(
            865_511L,
            result.manifest.processedCandidateCount
        )

        assertEquals(
            818_631L,
            result.manifest.acceptedReferenceCount
        )

        assertEquals(
            46_880L,
            result.manifest.rejectedCandidateCount
        )

        assertEquals(
            result.manifest.processedCandidateCount,
            result.manifest.acceptedReferenceCount +
                    result.manifest.rejectedCandidateCount
        )

        val persistedManifest =
            OFFAcceptedNutritionReferenceManifestReader()
                .read(
                    manifestOutputFile
                )

        assertEquals(
            result.manifest,
            persistedManifest
        )

        val persistedReferenceCount =
            OFFAcceptedNutritionReferenceReader()
                .forEachReference(
                    inputFile =
                        referenceOutputFile,
                    consumer =
                        {}
                )

        assertEquals(
            result.manifest.acceptedReferenceCount,
            persistedReferenceCount
        )

        println(
            "referenceFile=" +
                    result.referenceFile.absolutePath
        )

        println(
            "manifestFile=" +
                    result.manifestFile.absolutePath
        )

        println(
            "processedCandidateCount=" +
                    result.manifest.processedCandidateCount
        )

        println(
            "acceptedReferenceCount=" +
                    result.manifest.acceptedReferenceCount
        )

        println(
            "rejectedCandidateCount=" +
                    result.manifest.rejectedCandidateCount
        )

        println(
            "contentSha256=" +
                    result.manifest.contentSha256
        )

        println(
            "uncompressedContentBytes=" +
                    result.manifest.uncompressedContentBytes
        )
    }

    private fun resolveRepositoryRoot(): File {

        val workingDirectory =
            File(".").canonicalFile

        return if (workingDirectory.name == "app") {
            requireNotNull(
                workingDirectory.parentFile
            ).canonicalFile
        } else {
            workingDirectory
        }
    }
}