package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimTransactionalGroundTruthPublicationContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimTransactionalGroundTruthPublicationInputV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimTransactionalGroundTruthPublicationV1
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class RunHimActiveGroundTruthResolutionV1Test {

    @Test
    fun `active release resolves exactly through current pointer`() {
        val root =
            temporaryRoot()

        try {
            val input =
                input("A")

            val publication =
                HimTransactionalGroundTruthPublicationV1()
                    .publish(
                        projectRoot =
                            root,
                        input =
                            input,
                    ) {}

            val resolved =
                HimActiveGroundTruthResolutionV1()
                    .resolve(
                        projectRoot =
                            root
                    )

            assertEquals(
                publication.releaseReference,
                resolved.releaseReference,
            )

            assertEquals(
                publication.releaseDirectory
                    .canonicalFile,
                resolved.releaseDirectory
                    .canonicalFile,
            )

            assertArrayEquals(
                input.authorityBytes,
                resolved.authorityBytes,
            )

            assertArrayEquals(
                input.activeRegistryBytes,
                resolved.activeRegistryBytes,
            )

            assertArrayEquals(
                input.retiredRegistryBytes,
                resolved.retiredRegistryBytes,
            )

            assertArrayEquals(
                input.mutationLedgerBytes,
                resolved.mutationLedgerBytes,
            )

            assertArrayEquals(
                input.fingerprintIndexBytes,
                resolved.fingerprintIndexBytes,
            )

            assertArrayEquals(
                input.releaseBytes,
                resolved.releaseBytes,
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `new committed release becomes active immediately`() {
        val root =
            temporaryRoot()

        try {
            val publisher =
                HimTransactionalGroundTruthPublicationV1()

            val resolver =
                HimActiveGroundTruthResolutionV1()

            val first =
                publisher.publish(
                    projectRoot =
                        root,
                    input =
                        input("A"),
                ) {}

            val resolvedFirst =
                resolver.resolve(
                    projectRoot =
                        root
                )

            assertEquals(
                first.releaseReference,
                resolvedFirst.releaseReference,
            )

            val second =
                publisher.publish(
                    projectRoot =
                        root,
                    input =
                        input("B"),
                ) {}

            val resolvedSecond =
                resolver.resolve(
                    projectRoot =
                        root
                )

            assertEquals(
                second.releaseReference,
                resolvedSecond.releaseReference,
            )

            assertTrue(
                first.releaseReference !=
                        second.releaseReference
            )

            assertArrayEquals(
                input("B").authorityBytes,
                resolvedSecond.authorityBytes,
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `missing current pointer fails closed`() {
        val root =
            temporaryRoot()

        try {
            val failure =
                runCatching {
                    HimActiveGroundTruthResolutionV1()
                        .resolve(
                            projectRoot =
                                root
                        )
                }.exceptionOrNull()

            assertTrue(
                failure is
                        IllegalArgumentException
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `missing release artifact fails closed`() {
        val root =
            temporaryRoot()

        try {
            val publication =
                HimTransactionalGroundTruthPublicationV1()
                    .publish(
                        projectRoot =
                            root,
                        input =
                            input("A"),
                    ) {}

            val authorityFile =
                publication.releaseDirectory.resolve(
                    HimTransactionalGroundTruthPublicationContractV1
                        .AUTHORITY_FILE_NAME
                )

            assertTrue(
                authorityFile.delete()
            )

            val failure =
                runCatching {
                    HimActiveGroundTruthResolutionV1()
                        .resolve(
                            projectRoot =
                                root
                        )
                }.exceptionOrNull()

            assertTrue(
                failure is
                        IllegalArgumentException
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `tampered release artifact fails release identity verification`() {
        val root =
            temporaryRoot()

        try {
            val publication =
                HimTransactionalGroundTruthPublicationV1()
                    .publish(
                        projectRoot =
                            root,
                        input =
                            input("A"),
                    ) {}

            val authorityFile =
                publication.releaseDirectory.resolve(
                    HimTransactionalGroundTruthPublicationContractV1
                        .AUTHORITY_FILE_NAME
                )

            authorityFile.writeText(
                "tampered-authority\n"
            )

            val failure =
                runCatching {
                    HimActiveGroundTruthResolutionV1()
                        .resolve(
                            projectRoot =
                                root
                        )
                }.exceptionOrNull()

            assertTrue(
                failure is
                        IllegalArgumentException
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `tampered release file fails current pointer sha verification`() {
        val root =
            temporaryRoot()

        try {
            val publication =
                HimTransactionalGroundTruthPublicationV1()
                    .publish(
                        projectRoot =
                            root,
                        input =
                            input("A"),
                    ) {}

            val releaseFile =
                publication.releaseDirectory.resolve(
                    HimTransactionalGroundTruthPublicationContractV1
                        .RELEASE_FILE_NAME
                )

            releaseFile.writeText(
                "tampered-release\n"
            )

            val failure =
                runCatching {
                    HimActiveGroundTruthResolutionV1()
                        .resolve(
                            projectRoot =
                                root
                        )
                }.exceptionOrNull()

            assertTrue(
                failure is
                        IllegalArgumentException
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `current pointer may not resolve outside releases root`() {
        val root =
            temporaryRoot()

        try {
            val publisher =
                HimTransactionalGroundTruthPublicationV1()

            publisher.publish(
                projectRoot =
                    root,
                input =
                    input("A"),
            ) {}

            val groundTruthRoot =
                root.resolve(
                    HimTransactionalGroundTruthPublicationContractV1
                        .GROUND_TRUTH_DIRECTORY
                )

            val currentFile =
                groundTruthRoot.resolve(
                    HimTransactionalGroundTruthPublicationContractV1
                        .CURRENT_POINTER_FILE_NAME
                )

            val current =
                requireNotNull(
                    publisher.readCurrent(
                        root
                    )
                )

            val invalid =
                current.copy(
                    releaseDirectory =
                        "../outside"
                )

            currentFile.writeBytes(
                publisher.serializeCurrent(
                    invalid
                )
            )

            val failure =
                runCatching {
                    HimActiveGroundTruthResolutionV1()
                        .resolve(
                            projectRoot =
                                root
                        )
                }.exceptionOrNull()

            assertTrue(
                failure is
                        IllegalArgumentException
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `current pointer may not reference staging directory`() {
        val root =
            temporaryRoot()

        try {
            val publisher =
                HimTransactionalGroundTruthPublicationV1()

            val publication =
                publisher.publish(
                    projectRoot =
                        root,
                    input =
                        input("A"),
                ) {}

            val groundTruthRoot =
                root.resolve(
                    HimTransactionalGroundTruthPublicationContractV1
                        .GROUND_TRUTH_DIRECTORY
                )

            val staging =
                groundTruthRoot
                    .resolve(
                        HimTransactionalGroundTruthPublicationContractV1
                            .RELEASES_DIRECTORY_NAME
                    )
                    .resolve(
                        publication.releaseDirectory.name +
                                HimTransactionalGroundTruthPublicationContractV1
                                    .STAGING_SUFFIX
                    )

            assertTrue(
                staging.mkdirs()
            )

            val current =
                requireNotNull(
                    publisher.readCurrent(
                        root
                    )
                )

            val invalid =
                current.copy(
                    releaseDirectory =
                        HimTransactionalGroundTruthPublicationContractV1
                            .RELEASES_DIRECTORY_NAME +
                                "/" +
                                staging.name
                )

            groundTruthRoot
                .resolve(
                    HimTransactionalGroundTruthPublicationContractV1
                        .CURRENT_POINTER_FILE_NAME
                )
                .writeBytes(
                    publisher.serializeCurrent(
                        invalid
                    )
                )

            val failure =
                runCatching {
                    HimActiveGroundTruthResolutionV1()
                        .resolve(
                            projectRoot =
                                root
                        )
                }.exceptionOrNull()

            assertTrue(
                failure is
                        IllegalArgumentException
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `repeated resolution is deterministic and byte stable`() {
        val root =
            temporaryRoot()

        try {
            HimTransactionalGroundTruthPublicationV1()
                .publish(
                    projectRoot =
                        root,
                    input =
                        input("A"),
                ) {}

            val resolver =
                HimActiveGroundTruthResolutionV1()

            val first =
                resolver.resolve(
                    projectRoot =
                        root
                )

            val second =
                resolver.resolve(
                    projectRoot =
                        root
                )

            assertEquals(
                first,
                second,
            )

            assertArrayEquals(
                first.authorityBytes,
                second.authorityBytes,
            )

            assertArrayEquals(
                first.activeRegistryBytes,
                second.activeRegistryBytes,
            )

            assertArrayEquals(
                first.retiredRegistryBytes,
                second.retiredRegistryBytes,
            )

            assertArrayEquals(
                first.mutationLedgerBytes,
                second.mutationLedgerBytes,
            )

            assertArrayEquals(
                first.fingerprintIndexBytes,
                second.fingerprintIndexBytes,
            )

            assertArrayEquals(
                first.releaseBytes,
                second.releaseBytes,
            )
        } finally {
            root.deleteRecursively()
        }
    }

    private fun input(
        marker: String,
    ) =
        HimTransactionalGroundTruthPublicationInputV1(
            authorityBytes =
                bytes(
                    "authority-$marker"
                ),
            activeRegistryBytes =
                bytes(
                    "active-registry-$marker"
                ),
            retiredRegistryBytes =
                bytes(
                    "retired-registry-$marker"
                ),
            mutationLedgerBytes =
                bytes(
                    "mutation-ledger-$marker"
                ),
            fingerprintIndexBytes =
                bytes(
                    "fingerprint-index-$marker"
                ),
            releaseBytes =
                bytes(
                    "release-$marker"
                ),
        )

    private fun bytes(
        value: String,
    ): ByteArray =
        "$value\n".toByteArray(
            Charsets.UTF_8
        )

    private fun temporaryRoot() =
        Files.createTempDirectory(
            "him-active-ground-truth-resolution-v1-"
        ).toFile()
}