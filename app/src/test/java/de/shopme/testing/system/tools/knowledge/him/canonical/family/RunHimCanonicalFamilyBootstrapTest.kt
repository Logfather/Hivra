package de.shopme.testing.system.tools.knowledge.him.canonical.family

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyBootstrap
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyBootstrapMode
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RunHimCanonicalFamilyBootstrapTest {

    @Test
    fun bootstrapsAndReusesPersistentCanonicalIdsDeterministically() {
        val projectRoot = KnowledgeBuildPaths.default().projectRoot
        val paths = HimCanonicalFamilyPaths(projectRoot)
        val artifactsExistedBeforeRun =
            paths.entityIdRegistry.exists() || paths.familyAuthority.exists()
        val bootstrap = HimCanonicalFamilyBootstrap()

        val first = bootstrap.run(projectRoot)
        val firstRegistryBytes = paths.entityIdRegistry.readBytes()
        val firstAuthorityBytes = paths.familyAuthority.readBytes()
        val firstIds = first.registry.entries.map { it.entityId }

        if (artifactsExistedBeforeRun) {
            assertEquals(HimCanonicalFamilyBootstrapMode.EXISTING_REGISTRY_REUSE, first.mode)
        } else {
            assertEquals(HimCanonicalFamilyBootstrapMode.INITIAL_BOOTSTRAP, first.mode)
        }

        val second = bootstrap.run(projectRoot)
        val secondIds = second.registry.entries.map { it.entityId }

        assertEquals(HimCanonicalFamilyBootstrapMode.EXISTING_REGISTRY_REUSE, second.mode)
        assertEquals(firstIds, secondIds)
        assertTrue(firstRegistryBytes.contentEquals(paths.entityIdRegistry.readBytes()))
        assertTrue(firstAuthorityBytes.contentEquals(paths.familyAuthority.readBytes()))
        assertEquals(first.registrySha256, second.registrySha256)
        assertEquals(first.authoritySha256, second.authoritySha256)
        assertEquals(1384, second.validation.registryEntries)
        assertEquals(1384, second.validation.families)
        assertEquals(1384, second.validation.uniqueIds)
        assertEquals(0, second.validation.invalidIds)
        assertEquals(0, second.validation.collisions)
        assertEquals(0, second.validation.identities)
        assertEquals(0, second.validation.variants)
        assertEquals(0, second.validation.aliases)
        assertFalse(paths.projectRoot.resolve(
            "data/knowledge/him/canonical-family/index/" +
                    "him-entity-fingerprint-index.v1.json"
        ).exists())
    }
}
