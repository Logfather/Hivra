package de.shopme.testing.system.tools.knowledge.him.canonical.family

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndexGeneration
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunHimEntityFingerprintIndexGenerationTest {

    @Test
    fun generatesValidatedIndexDeterministicallyWithoutChangingAuthorities() {
        val projectRoot = KnowledgeBuildPaths.default().projectRoot
        val paths = HimCanonicalFamilyPaths(projectRoot)
        val registryShaBefore =
            HimProductOnlyCanonicalMasterReader.sha256(paths.entityIdRegistry)
        val authorityShaBefore =
            HimProductOnlyCanonicalMasterReader.sha256(paths.familyAuthority)
        val generation = HimEntityFingerprintIndexGeneration()

        val first = generation.generate(projectRoot)
        val firstBytes = first.indexFile.readBytes()
        val firstFingerprints = first.index.entries.map { it.fingerprint }
        val firstOrder = first.index.entries.map { it.canonicalId }

        val second = generation.generate(projectRoot)

        assertEquals(1384, first.validation.entries)
        assertEquals(1384, first.validation.uniqueFingerprints)
        assertEquals(0, first.validation.duplicateFingerprintGroups)
        assertEquals(0, first.validation.invalidFingerprints)
        assertEquals(0, first.validation.familiesWithoutFingerprint)
        assertEquals(0, first.validation.fingerprintsWithoutFamily)
        assertEquals(0, first.validation.unknownCanonicalIds)
        assertEquals(0, first.validation.duplicateCanonicalIdEntries)
        assertEquals(0, first.validation.identityReferences)
        assertEquals(0, first.validation.variantReferences)
        assertEquals(0, first.validation.fingerprintRecomputationMismatches)
        assertEquals(firstFingerprints, second.index.entries.map { it.fingerprint })
        assertEquals(firstOrder, second.index.entries.map { it.canonicalId })
        assertTrue(firstBytes.contentEquals(second.indexFile.readBytes()))
        assertEquals(first.indexSha256, second.indexSha256)
        assertEquals(
            registryShaBefore,
            HimProductOnlyCanonicalMasterReader.sha256(paths.entityIdRegistry),
        )
        assertEquals(
            authorityShaBefore,
            HimProductOnlyCanonicalMasterReader.sha256(paths.familyAuthority),
        )
    }
}
