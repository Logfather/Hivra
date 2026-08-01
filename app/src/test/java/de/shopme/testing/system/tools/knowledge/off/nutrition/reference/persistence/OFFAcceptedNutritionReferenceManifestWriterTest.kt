package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.persistence

import de.shopme.tools.knowledge.off.nutrition.reference.persistence.OFFAcceptedNutritionReferenceManifest
import de.shopme.tools.knowledge.off.nutrition.reference.persistence.OFFAcceptedNutritionReferenceManifestReader
import de.shopme.tools.knowledge.off.nutrition.reference.persistence.OFFAcceptedNutritionReferenceManifestWriter
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class OFFAcceptedNutritionReferenceManifestWriterTest {

    @Test
    fun write_thenRead_preservesManifest() {

        val directory =
            Files.createTempDirectory(
                "off-reference-manifest"
            ).toFile()

        val outputFile =
            directory.resolve(
                "manifest.json"
            )

        val manifest =
            OFFAcceptedNutritionReferenceManifest(
                version = 1,
                format =
                    OFFAcceptedNutritionReferenceManifest
                        .JSONL_FORMAT,
                compression =
                    OFFAcceptedNutritionReferenceManifest
                        .GZIP_COMPRESSION,
                inputFile =
                    "/tmp/off.jsonl.gz",
                inputFileSizeBytes =
                    1_000L,
                policyFile =
                    "/tmp/policy.json",
                policyVersion =
                    1,
                outputFile =
                    "/tmp/references.jsonl.gz",
                processedCandidateCount =
                    100L,
                acceptedReferenceCount =
                    90L,
                rejectedCandidateCount =
                    10L,
                contentSha256 =
                    "a".repeat(64),
                uncompressedContentBytes =
                    10_000L
            )

        OFFAcceptedNutritionReferenceManifestWriter()
            .write(
                manifest =
                    manifest,
                outputFile =
                    outputFile
            )

        val restored =
            OFFAcceptedNutritionReferenceManifestReader()
                .read(outputFile)

        assertEquals(
            manifest,
            restored
        )
    }
}