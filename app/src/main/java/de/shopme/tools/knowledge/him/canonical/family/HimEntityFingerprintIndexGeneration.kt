package de.shopme.tools.knowledge.him.canonical.family

import java.io.File

data class HimEntityFingerprintIndexGenerationResult(
    val index: HimEntityFingerprintIndex,
    val validation: HimEntityFingerprintIndexValidationResult,
    val indexFile: File,
    val indexSha256: String,
)

class HimEntityFingerprintIndexGeneration(
    private val authorityPersistence: HimCanonicalFamilyPersistence =
        HimCanonicalFamilyPersistence(),
    private val builder: HimEntityFingerprintIndexBuilder =
        HimEntityFingerprintIndexBuilder(),
    private val persistence: HimEntityFingerprintIndexPersistence =
        HimEntityFingerprintIndexPersistence(),
    private val validator: HimEntityFingerprintIndexValidator =
        HimEntityFingerprintIndexValidator(),
) {

    fun generate(projectRoot: File): HimEntityFingerprintIndexGenerationResult {
        val root = projectRoot.canonicalFile
        val familyPaths = HimCanonicalFamilyPaths(root)
        val indexFile = root.resolve(INDEX_PATH)
        val registrySha256 =
            HimProductOnlyCanonicalMasterReader.sha256(familyPaths.entityIdRegistry)
        val authoritySha256 =
            HimProductOnlyCanonicalMasterReader.sha256(familyPaths.familyAuthority)

        val registry = authorityPersistence.readRegistry(familyPaths.entityIdRegistry)
        val authority = authorityPersistence.readAuthority(familyPaths.familyAuthority)
        val index =
            builder.build(
                registry = registry,
                authority = authority,
                registrySha256 = registrySha256,
                authoritySha256 = authoritySha256,
            )

        validator.validate(
            registry = registry,
            authority = authority,
            index = index,
            registrySha256 = registrySha256,
            authoritySha256 = authoritySha256,
        )

        persistence.write(indexFile, persistence.serialize(index))

        val persistedIndex = persistence.read(indexFile)
        val validation =
            validator.validate(
                registry = registry,
                authority = authority,
                index = persistedIndex,
                registrySha256 = registrySha256,
                authoritySha256 = authoritySha256,
            )

        require(HimProductOnlyCanonicalMasterReader.sha256(familyPaths.entityIdRegistry) ==
                registrySha256)
        require(HimProductOnlyCanonicalMasterReader.sha256(familyPaths.familyAuthority) ==
                authoritySha256)

        return HimEntityFingerprintIndexGenerationResult(
            index = persistedIndex,
            validation = validation,
            indexFile = indexFile,
            indexSha256 = HimProductOnlyCanonicalMasterReader.sha256(indexFile),
        )
    }

    companion object {
        const val INDEX_PATH =
            "data/knowledge/him/canonical-family/index/" +
                    "him-entity-fingerprint-index.v1.json"
    }
}
