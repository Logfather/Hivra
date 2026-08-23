package de.shopme.testing.system.tools.knowledge.him.canonical.family

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyFoundationReleaseBuilder
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyFoundationReleasePersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyFoundationReleaseValidator
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyValidator
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndexPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndexValidator
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunHimCanonicalFamilyFoundationFreezeTest {

    @Test
    fun freezesValidatedFoundationInDeterministicReleaseRecord() {
        val projectRoot = KnowledgeBuildPaths.default().projectRoot
        val paths = HimCanonicalFamilyPaths(projectRoot)
        val indexFile = projectRoot.resolve(HimCanonicalFamilyFoundationReleaseBuilder.FINGERPRINT_INDEX_PATH)
        val f2dReport = projectRoot.resolve(F2D_REPORT_PATH)
        val releaseFile = projectRoot.resolve(HimCanonicalFamilyFoundationReleaseBuilder.RELEASE_RECORD_PATH)

        val productShaBefore = sha256(paths.productOnlyMaster)
        val registryShaBefore = sha256(paths.entityIdRegistry)
        val authorityShaBefore = sha256(paths.familyAuthority)
        val indexShaBefore = sha256(indexFile)

        assertEquals(HimCanonicalFamilyFoundationReleaseBuilder.PRODUCT_MASTER_SHA256, productShaBefore)
        assertEquals(HimCanonicalFamilyFoundationReleaseBuilder.ENTITY_ID_REGISTRY_SHA256, registryShaBefore)
        assertEquals(HimCanonicalFamilyFoundationReleaseBuilder.CANONICAL_FAMILY_AUTHORITY_SHA256, authorityShaBefore)
        assertEquals(HimCanonicalFamilyFoundationReleaseBuilder.FINGERPRINT_INDEX_SHA256, indexShaBefore)
        assertEquals(EXPECTED_F2D_REPORT_SHA256, sha256(f2dReport))
        assertTrue(f2dReport.readLines().contains("F2_FOUNDATION_RELEASE_READY=true"))

        val source = HimProductOnlyCanonicalMasterReader().read(paths)
        val familyPersistence = HimCanonicalFamilyPersistence()
        val registry = familyPersistence.readRegistry(paths.entityIdRegistry)
        val authority = familyPersistence.readAuthority(paths.familyAuthority)
        val familyValidation = HimCanonicalFamilyValidator().validate(source, registry, authority)
        val index = HimEntityFingerprintIndexPersistence().read(indexFile)
        val indexValidation = HimEntityFingerprintIndexValidator().validate(
            registry = registry,
            authority = authority,
            index = index,
            registrySha256 = registryShaBefore,
            authoritySha256 = authorityShaBefore,
        )

        assertEquals(1384, source.records.size)
        assertEquals(1384, familyValidation.registryEntries)
        assertEquals(1384, familyValidation.families)
        assertEquals(1384, indexValidation.entries)

        val builder = HimCanonicalFamilyFoundationReleaseBuilder()
        val validator = HimCanonicalFamilyFoundationReleaseValidator()
        val persistence = HimCanonicalFamilyFoundationReleasePersistence()
        val first = builder.build()
        val second = builder.build()
        validator.validate(first)
        validator.validate(second)

        val firstBytes = persistence.serialize(first)
        val secondBytes = persistence.serialize(second)
        assertContentEquals(firstBytes, secondBytes)
        assertEquals(sha256(firstBytes), sha256(secondBytes))

        persistence.writeNewOrRequireIdentical(releaseFile, firstBytes)
        val persisted = persistence.read(releaseFile)
        validator.validate(persisted)
        assertEquals(first, persisted)
        assertContentEquals(firstBytes, releaseFile.readBytes())

        assertEquals(productShaBefore, sha256(paths.productOnlyMaster))
        assertEquals(registryShaBefore, sha256(paths.entityIdRegistry))
        assertEquals(authorityShaBefore, sha256(paths.familyAuthority))
        assertEquals(indexShaBefore, sha256(indexFile))

        assertEquals(1, gitExit(projectRoot, "check-ignore", "-q", releaseFile.path))
        assertEquals(0, gitExit(projectRoot, "check-ignore", "-q", indexFile.path))
        assertEquals(1, gitExit(projectRoot, "check-ignore", "-q", paths.entityIdRegistry.path))
        assertEquals(1, gitExit(projectRoot, "check-ignore", "-q", paths.familyAuthority.path))
    }

    private fun gitExit(projectRoot: File, vararg arguments: String): Int =
        ProcessBuilder(listOf("git") + arguments)
            .directory(projectRoot)
            .redirectErrorStream(true)
            .start()
            .let { process ->
                process.inputStream.use { it.readBytes() }
                process.waitFor()
            }

    private fun sha256(file: File): String =
        HimProductOnlyCanonicalMasterReader.sha256(file)

    private fun sha256(bytes: ByteArray): String =
        HimProductOnlyCanonicalMasterReader.sha256(bytes)

    companion object {
        const val F2D_REPORT_PATH =
            "build/knowledge/reports/him/canonical-family/" +
                    "him-canonical-family-foundation-release.txt"
        const val EXPECTED_F2D_REPORT_SHA256 =
            "9db2845d9ddc2bb46b0cdec61e8d1533abc9e8598c63fdbcb3f3e9168e2459da"
    }
}
