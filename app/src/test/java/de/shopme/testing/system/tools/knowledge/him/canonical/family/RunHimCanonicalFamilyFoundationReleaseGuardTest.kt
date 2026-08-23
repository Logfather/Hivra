package de.shopme.testing.system.tools.knowledge.him.canonical.family

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalAlias
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyValidator
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalIdentity
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalVariant
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintGenerator
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndexBuilder
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndexPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndexValidator
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntitySourceReferenceType
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RunHimCanonicalFamilyFoundationReleaseGuardTest {

    @Test
    fun validatesFoundationAndWritesDeterministicReleaseReport() {
        val projectRoot = KnowledgeBuildPaths.default().projectRoot
        val paths = HimCanonicalFamilyPaths(projectRoot)
        val indexFile = projectRoot.resolve(INDEX_PATH)
        val stagingBefore = git(projectRoot, "diff", "--cached", "--name-status")

        val productShaBefore = sha256(paths.productOnlyMaster)
        val registryShaBefore = sha256(paths.entityIdRegistry)
        val authorityShaBefore = sha256(paths.familyAuthority)
        val indexShaBefore = sha256(indexFile)

        assertEquals(EXPECTED_PRODUCT_SHA256, productShaBefore)
        assertEquals(EXPECTED_REGISTRY_SHA256, registryShaBefore)
        assertEquals(EXPECTED_AUTHORITY_SHA256, authorityShaBefore)
        assertEquals(EXPECTED_INDEX_SHA256, indexShaBefore)

        val source = HimProductOnlyCanonicalMasterReader().read(paths)
        val authorityPersistence = HimCanonicalFamilyPersistence()
        val registry = authorityPersistence.readRegistry(paths.entityIdRegistry)
        val authority = authorityPersistence.readAuthority(paths.familyAuthority)
        val familyValidation =
            HimCanonicalFamilyValidator().validate(source, registry, authority)

        assertEquals(1384, source.records.size)
        assertEquals(1384, source.records.count { it.itemname.isNotBlank() })
        assertEquals(1384, source.records.map { it.itemname }.distinct().size)
        assertEquals(1384, source.records.count { it.normalized.isNotBlank() })
        assertEquals(1384, source.records.map { it.normalized }.distinct().size)
        assertTrue(source.records.all { it.taxonomyPaths.isNotEmpty() })

        validateF1Contracts()
        validateRegistryReconciliation(source.records.map { it.normalized }, registry.entries)
        validateApprovedOnlyAuthority(paths.familyAuthority)
        validatePreHimBoundary(projectRoot)

        val indexPersistence = HimEntityFingerprintIndexPersistence()
        val persistedIndex = indexPersistence.read(indexFile)
        val indexValidation =
            HimEntityFingerprintIndexValidator().validate(
                registry = registry,
                authority = authority,
                index = persistedIndex,
                registrySha256 = registryShaBefore,
                authoritySha256 = authorityShaBefore,
            )

        val regeneratedIndex =
            HimEntityFingerprintIndexBuilder().build(
                registry = registry,
                authority = authority,
                registrySha256 = registryShaBefore,
                authoritySha256 = authorityShaBefore,
            )
        val persistedBytes = indexFile.readBytes()
        val regeneratedBytes = indexPersistence.serialize(regeneratedIndex)

        assertEquals(persistedIndex, regeneratedIndex)
        assertContentEquals(persistedBytes, regeneratedBytes)
        assertEquals(EXPECTED_INDEX_SHA256, sha256(regeneratedBytes))

        validateGitPersistence(projectRoot, indexFile)

        val report =
            releaseReport(
                sourceRecords = source.records.size,
                familyValidation = familyValidation,
                indexEntries = indexValidation.entries,
                uniqueFingerprints = indexValidation.uniqueFingerprints,
                registrySha = registryShaBefore,
                authoritySha = authorityShaBefore,
                productSha = productShaBefore,
                indexSha = indexShaBefore,
                staging = stagingBefore,
            )
        val reportFile = projectRoot.resolve(REPORT_PATH)
        val reportDirectory = requireNotNull(reportFile.parentFile)
        require(reportDirectory.exists() || reportDirectory.mkdirs())

        reportFile.writeText(report)
        val reportRunOne = reportFile.readBytes()
        val reportShaRunOne = sha256(reportRunOne)
        reportFile.writeText(report)
        val reportRunTwo = reportFile.readBytes()
        val reportShaRunTwo = sha256(reportRunTwo)

        assertContentEquals(reportRunOne, reportRunTwo)
        assertEquals(reportShaRunOne, reportShaRunTwo)

        assertEquals(productShaBefore, sha256(paths.productOnlyMaster))
        assertEquals(registryShaBefore, sha256(paths.entityIdRegistry))
        assertEquals(authorityShaBefore, sha256(paths.familyAuthority))
        assertEquals(indexShaBefore, sha256(indexFile))
        assertEquals(stagingBefore, git(projectRoot, "diff", "--cached", "--name-status"))
    }

    private fun validateF1Contracts() {
        val canonicalFields = HimCanonicalFamily::class.java.declaredFields.map { it.name }.toSet()
        val identityFields = HimCanonicalIdentity::class.java.declaredFields.map { it.name }.toSet()
        val variantFields = HimCanonicalVariant::class.java.declaredFields.map { it.name }.toSet()
        val aliasFields = HimCanonicalAlias::class.java.declaredFields.map { it.name }.toSet()

        assertTrue(canonicalFields.containsAll(setOf(
            "canonicalId", "canonicalName", "normalizedName", "taxonomyPaths",
            "lifecycleStatus", "identities", "variants", "aliases"
        )))
        assertTrue(identityFields.containsAll(setOf(
            "identityId", "identityName", "normalizedName", "lifecycleStatus",
            "variants", "aliases"
        )))
        assertFalse("identities" in identityFields)
        assertTrue(variantFields.containsAll(setOf(
            "variantId", "variantName", "normalizedName", "lifecycleStatus"
        )))
        assertFalse("aliases" in variantFields)
        assertTrue(aliasFields.containsAll(setOf(
            "aliasId", "aliasName", "normalizedName", "lifecycleStatus"
        )))
        assertEquals(setOf("ACTIVE", "DEPRECATED"), HimLifecycleStatus.entries.map { it.name }.toSet())

        listOf("A7x2Qp", "0abZ91", "q8M2Ls").forEach { HimEntityId(it) }
        listOf("abc", "abcdefg", "abc-12", "abc_12", "äbc123", "ABC 12").forEach {
            assertTrue(runCatching { HimEntityId(it) }.isFailure)
        }

        val generatorMethods =
            HimEntityFingerprintGenerator::class.java.declaredMethods
                .filter { it.name == "generate" || it.name == "canonicalInput" }
        assertTrue(generatorMethods.isNotEmpty())
        assertTrue(generatorMethods.all { method ->
            method.parameterTypes.size == 3 &&
                    method.parameterTypes[0] == HimEntityId::class.java &&
                    method.parameterTypes[1] == HimEntityId::class.java &&
                    List::class.java.isAssignableFrom(method.parameterTypes[2])
        })
    }

    private fun validateRegistryReconciliation(
        normalizedValues: List<String>,
        entries: List<de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistryEntry>,
    ) {
        assertEquals(1384, entries.size)
        assertTrue(entries.all { it.entityType == HimEntityType.CANONICAL })
        assertTrue(entries.all {
            it.sourceReferenceType ==
                    HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED
        })
        assertTrue(entries.all { it.sourceReference.isNotBlank() })
        assertEquals(1384, entries.map { it.entityId }.distinct().size)
        assertEquals(1384, entries.map { it.sourceReference }.distinct().size)
        assertEquals(normalizedValues.toSet(), entries.map { it.sourceReference }.toSet())
    }

    private fun validateApprovedOnlyAuthority(authorityFile: File) {
        val content = authorityFile.readText()
        assertFalse(content.contains("\"PROPOSED\""))
        assertFalse(content.contains("\"REJECTED\""))
    }

    private fun validatePreHimBoundary(projectRoot: File) {
        val sourceDirectory = projectRoot.resolve(
            "app/src/main/java/de/shopme/tools/knowledge/him/canonical/family"
        )
        val forbidden = listOf(
            "CanonicalFoodIdentity",
            "TrueCanonicalFoodIdentity",
            "sourceVariants",
            "KnowledgeCandidate",
            "Matcher",
            "Merger",
        )
        val violations =
            sourceDirectory.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .flatMap { file ->
                    forbidden.asSequence()
                        .filter { token -> file.readText().contains(token) }
                        .map { token -> "${file.name}:$token" }
                }
                .toList()
        assertTrue(violations.isEmpty(), "PRE-HIM boundary violations: $violations")
    }

    private fun validateGitPersistence(
        projectRoot: File,
        indexFile: File,
    ) {
        val masterDirectory = HimCanonicalFamilyPaths.CANONICAL_FAMILY_MASTER_DIRECTORY
        assertTrue(git(
            projectRoot,
            "ls-files",
            "--stage",
            "--",
            "$masterDirectory/${HimCanonicalFamilyPaths.ENTITY_ID_REGISTRY_FILE_NAME}",
        ).isNotBlank())
        assertTrue(git(
            projectRoot,
            "ls-files",
            "--stage",
            "--",
            "$masterDirectory/${HimCanonicalFamilyPaths.FAMILY_AUTHORITY_FILE_NAME}",
        ).isNotBlank())
        assertEquals(0, gitExit(projectRoot, "check-ignore", "-q", indexFile.path))
        assertEquals(0, gitExit(
            projectRoot,
            "check-ignore",
            "-q",
            "data/sources/openfoodfacts/raw/openfoodfacts-products.jsonl.gz",
        ))

        val visibleData = git(
            projectRoot,
            "status",
            "--short",
            "--untracked-files=all",
            "--",
            "data",
        ).lineSequence().filter { it.isNotBlank() }.toList()
        assertTrue(visibleData.all { line ->
            line.endsWith("data/knowledge/him/canonical-family/master/" +
                    "him-entity-id-registry.v1.json") ||
                    line.endsWith("data/knowledge/him/canonical-family/master/" +
                            "canonical-family-authority.v1.json")
        })
    }

    private fun releaseReport(
        sourceRecords: Int,
        familyValidation: de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyValidationResult,
        indexEntries: Int,
        uniqueFingerprints: Int,
        registrySha: String,
        authoritySha: String,
        productSha: String,
        indexSha: String,
        staging: String,
    ): String =
        listOf(
            "HIM_CANONICAL_FAMILY_FOUNDATION_RELEASE_V1",
            "PRODUCT_MASTER_RECORDS=$sourceRecords",
            "PRODUCT_MASTER_SHA256=$productSha",
            "REGISTRY_ENTRIES=${familyValidation.registryEntries}",
            "REGISTRY_CANONICAL=${familyValidation.canonicalEntries}",
            "REGISTRY_IDENTITY=${familyValidation.identityEntries}",
            "REGISTRY_VARIANT=${familyValidation.variantEntries}",
            "REGISTRY_ALIAS=${familyValidation.aliasEntries}",
            "REGISTRY_UNIQUE_IDS=${familyValidation.uniqueIds}",
            "REGISTRY_INVALID_IDS=${familyValidation.invalidIds}",
            "REGISTRY_COLLISIONS=${familyValidation.collisions}",
            "REGISTRY_SHA256=$registrySha",
            "FAMILIES=${familyValidation.families}",
            "FAMILIES_ACTIVE=${familyValidation.activeFamilies}",
            "FAMILIES_DEPRECATED=${familyValidation.deprecatedFamilies}",
            "IDENTITY_RELATIONS=${familyValidation.identities}",
            "VARIANT_RELATIONS=${familyValidation.variants}",
            "ALIAS_RELATIONS=${familyValidation.aliases}",
            "FAMILY_AUTHORITY_SHA256=$authoritySha",
            "FINGERPRINT_ENTRIES=$indexEntries",
            "FINGERPRINT_UNIQUE=$uniqueFingerprints",
            "FINGERPRINT_INDEX_SHA256=$indexSha",
            "RECONCILIATION_INTEGRITY=PASS",
            "FAMILY_INTEGRITY=PASS",
            "F1_HIERARCHY=PASS",
            "PRE_HIM_BOUNDARY=PASS",
            "FINGERPRINT_INTEGRITY=PASS",
            "FINGERPRINT_DETERMINISM=PASS",
            "GIT_AUTHORITY_PERSISTENCE=PASS",
            "DERIVED_INDEX_IGNORED=PASS",
            "AUTHORITY_HASH_INTEGRITY=PASS",
            "STAGING_GUARD=PASS",
            "STAGING=${staging.replace('\n', ';')}",
            "BUILD_COMPILE_CONTRACT=PASS",
            "TARGETED_RELEASE_GUARD=PASS",
            "F2_FOUNDATION_RELEASE_READY=true",
        ).joinToString(separator = "\n", postfix = "\n")

    private fun git(projectRoot: File, vararg arguments: String): String {
        val process =
            ProcessBuilder(listOf("git") + arguments)
                .directory(projectRoot)
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        require(process.waitFor() == 0) { output }
        return output
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
        const val EXPECTED_PRODUCT_SHA256 =
            "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f"
        const val EXPECTED_REGISTRY_SHA256 =
            "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a"
        const val EXPECTED_AUTHORITY_SHA256 =
            "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184"
        const val EXPECTED_INDEX_SHA256 =
            "6dcb065d6dce0aaa22c8d1d3dd620372fc90d3fe9a3ec9d20ee54737a8f9a951"
        const val INDEX_PATH =
            "data/knowledge/him/canonical-family/index/" +
                    "him-entity-fingerprint-index.v1.json"
        const val REPORT_PATH =
            "build/knowledge/reports/him/canonical-family/" +
                    "him-canonical-family-foundation-release.txt"
    }
}
