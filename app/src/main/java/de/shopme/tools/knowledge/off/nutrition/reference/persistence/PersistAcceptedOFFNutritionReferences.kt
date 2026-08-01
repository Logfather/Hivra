package de.shopme.tools.knowledge.off.nutrition.reference.persistence

import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityPolicy
import java.io.File

class PersistAcceptedOFFNutritionReferences(
    private val referenceWriter:
    OFFAcceptedNutritionReferenceWriter =
        OFFAcceptedNutritionReferenceWriter(),
    private val manifestWriter:
    OFFAcceptedNutritionReferenceManifestWriter =
        OFFAcceptedNutritionReferenceManifestWriter()
) {

    fun run(
        inputFile: File,
        policyFile: File,
        policy: OFFNutritionReferenceQualityPolicy,
        referenceOutputFile: File,
        manifestOutputFile: File,
        forEachCandidate:
            (
            consumer:
                (CanonicalOFFNutritionReferenceCandidate) -> Unit
        ) -> Unit,
        isAccepted:
            (CanonicalOFFNutritionReferenceCandidate) -> Boolean
    ): PersistAcceptedOFFNutritionReferencesResult {

        require(inputFile.isFile) {
            "OFF input file does not exist: " +
                    inputFile.absolutePath
        }

        require(policyFile.isFile) {
            "OFF nutrition quality policy does not exist: " +
                    policyFile.absolutePath
        }

        require(
            policy.policyStatus.name == "APPROVED"
        ) {
            "OFF nutrition quality policy must be approved."
        }

        var processedCandidateCount =
            0L

        var rejectedCandidateCount =
            0L

        val writeResult =
            referenceWriter.write(
                outputFile =
                    referenceOutputFile
            ) { append ->

                forEachCandidate { candidate ->

                    processedCandidateCount++

                    if (isAccepted(candidate)) {
                        append(candidate)
                    } else {
                        rejectedCandidateCount++
                    }
                }
            }

        val acceptedReferenceCount =
            writeResult.writtenReferenceCount

        require(
            processedCandidateCount ==
                    acceptedReferenceCount +
                    rejectedCandidateCount
        ) {
            "Persistence counts are inconsistent: " +
                    "processed=$processedCandidateCount, " +
                    "accepted=$acceptedReferenceCount, " +
                    "rejected=$rejectedCandidateCount."
        }

        require(
            acceptedReferenceCount ==
                    policy.evidence.acceptedCandidateCount
        ) {
            "Persisted accepted reference count differs from " +
                    "the approved policy evidence: " +
                    "persisted=$acceptedReferenceCount, " +
                    "policy=${policy.evidence.acceptedCandidateCount}."
        }

        require(
            rejectedCandidateCount ==
                    policy.evidence.rejectedCandidateCount
        ) {
            "Rejected candidate count differs from the approved " +
                    "policy evidence: " +
                    "actual=$rejectedCandidateCount, " +
                    "policy=${policy.evidence.rejectedCandidateCount}."
        }

        require(
            processedCandidateCount ==
                    policy.evidence.generatedCandidateCount
        ) {
            "Processed candidate count differs from the approved " +
                    "policy evidence: " +
                    "actual=$processedCandidateCount, " +
                    "policy=${policy.evidence.generatedCandidateCount}."
        }

        val manifest =
            OFFAcceptedNutritionReferenceManifest(
                version =
                    OFFAcceptedNutritionReferenceManifest
                        .CURRENT_VERSION,
                format =
                    OFFAcceptedNutritionReferenceManifest
                        .JSONL_FORMAT,
                compression =
                    OFFAcceptedNutritionReferenceManifest
                        .GZIP_COMPRESSION,
                inputFile =
                    inputFile.canonicalPath,
                inputFileSizeBytes =
                    inputFile.length(),
                policyFile =
                    policyFile.canonicalPath,
                policyVersion =
                    policy.version,
                outputFile =
                    writeResult.outputFile.canonicalPath,
                processedCandidateCount =
                    processedCandidateCount,
                acceptedReferenceCount =
                    acceptedReferenceCount,
                rejectedCandidateCount =
                    rejectedCandidateCount,
                contentSha256 =
                    writeResult.contentSha256,
                uncompressedContentBytes =
                    writeResult.uncompressedContentBytes
            )

        val writtenManifestFile =
            manifestWriter.write(
                manifest =
                    manifest,
                outputFile =
                    manifestOutputFile
            )

        return PersistAcceptedOFFNutritionReferencesResult(
            referenceFile =
                writeResult.outputFile,
            manifestFile =
                writtenManifestFile,
            manifest =
                manifest
        )
    }
}