package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimCanonicalAgreementPreflightV1
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RunHimCanonicalAgreementPreflightV1Test {

    @Test
    fun hermeticPreflightCollectsAllMismatchesInEntityIdOrder() {
        val result = HimCanonicalAgreementPreflightV1.evaluate(
            authority(
                family("z00001", "Ras el Hanout", "ras-el-hanout-falsch"),
                family("a00001", "Sataysoße", "satay-sosse"),
                family("m00001", "Nicht passend", "anderer-schluessel"),
            ),
        )

        assertEquals(3, result.canonicalCount)
        assertEquals(listOf("a00001", "m00001", "z00001"), result.mismatches.map { it.entityId })
        assertEquals("Nicht passend", result.mismatches[1].canonicalName)
        assertEquals("anderer-schluessel", result.mismatches[1].normalizedName)
        assertFalse(result.passed)
    }

    @Test
    fun hermeticPreflightPassesAgreementForAmpersandAndSharpSS() {
        val result = HimCanonicalAgreementPreflightV1.evaluate(
            authority(
                family("a00001", "Erbsen & Möhren", "erbsen-und-moehren"),
                family("z00001", "Sataysoße", "sataysosse"),
                family("m00001", "Ras el Hanout", "ras-el-hanout"),
            ),
        )

        assertTrue(result.passed)
        assertTrue(result.mismatches.isEmpty())
    }

    @Test
    fun `validates all current canonical name bindings without opening evidence stores`() {
        HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()

        val root = projectRoot()
        val paths = HimCanonicalFamilyPaths(root)
        val catalog = HimProductOnlyCanonicalMasterReader().read(paths)
        val active = HimActiveGroundTruthResolutionV1().resolve(root)
        val authority = HimCanonicalFamilyPersistence().readAuthority(active.authorityFile)

        assertEquals(catalog.path, authority.sourceCatalog.path)
        assertEquals(catalog.contentSha256, authority.sourceCatalog.contentSha256)
        assertEquals(catalog.records.size, authority.sourceCatalog.recordCount)
        assertEquals(catalog.records.size, authority.families.size)

        val result = HimCanonicalAgreementPreflightV1.evaluate(authority)

        assertEquals(1384, result.canonicalCount)
        assertTrue(result.passed, result.mismatches.joinToString { "${it.entityId}:${it.canonicalName}:${it.normalizedName}" })
    }

    private fun authority(vararg families: HimCanonicalFamily) = HimCanonicalFamilyAuthority(
        schemaVersion = "fixture",
        sourceCatalog = HimCanonicalFamilySourceCatalog("fixture/catalog.json", "fixture", families.size),
        families = families.toList(),
    )

    private fun family(id: String, name: String, normalized: String) = HimCanonicalFamily(
        canonicalId = HimEntityId(id),
        canonicalName = name,
        normalizedName = normalized,
        taxonomyPaths = emptyList(),
        lifecycleStatus = HimLifecycleStatus.ACTIVE,
        identities = emptyList(),
        variants = emptyList(),
        aliases = emptyList(),
    )

    private fun projectRoot(): File {
        var current = File(System.getProperty("user.dir") ?: error("user.dir unavailable")).canonicalFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile) return current
            current = current.parentFile ?: error("Repository root not found")
        }
    }
}
