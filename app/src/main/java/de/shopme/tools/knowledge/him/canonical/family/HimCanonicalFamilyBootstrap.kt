package de.shopme.tools.knowledge.him.canonical.family

import java.io.File

enum class HimCanonicalFamilyBootstrapMode {
    INITIAL_BOOTSTRAP,
    EXISTING_REGISTRY_REUSE,
}

data class HimCanonicalFamilyBootstrapResult(
    val mode: HimCanonicalFamilyBootstrapMode,
    val registry: HimEntityIdRegistry,
    val authority: HimCanonicalFamilyAuthority,
    val validation: HimCanonicalFamilyValidationResult,
    val registrySha256: String,
    val authoritySha256: String,
)

class HimCanonicalFamilyBootstrap(
    private val reader: HimProductOnlyCanonicalMasterReader =
        HimProductOnlyCanonicalMasterReader(),
    private val idGenerator: HimEntityIdGenerator = HimEntityIdGenerator(),
    private val persistence: HimCanonicalFamilyPersistence =
        HimCanonicalFamilyPersistence(),
    private val validator: HimCanonicalFamilyValidator =
        HimCanonicalFamilyValidator(),
) {

    fun run(projectRoot: File): HimCanonicalFamilyBootstrapResult {
        val paths = HimCanonicalFamilyPaths(projectRoot.canonicalFile)
        val source = reader.read(paths)
        val registryExists = paths.entityIdRegistry.exists()
        val authorityExists = paths.familyAuthority.exists()

        require(registryExists == authorityExists) {
            "Canonical Family Authority artifacts must either both exist or both be absent."
        }

        return if (registryExists) {
            reuse(paths, source)
        } else {
            initialize(paths, source)
        }
    }

    private fun initialize(
        paths: HimCanonicalFamilyPaths,
        source: HimProductOnlyCanonicalMaster,
    ): HimCanonicalFamilyBootstrapResult {
        val assignedIds = mutableSetOf<HimEntityId>()
        val registry =
            HimEntityIdRegistry(
                entries = source.records.map { record ->
                    HimEntityIdRegistryEntry(
                        entityId = idGenerator.generate(assignedIds),
                        entityType = HimEntityType.CANONICAL,
                        sourceReferenceType =
                            HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED,
                        sourceReference = record.normalized,
                    )
                },
            )
        val authority = createAuthority(source, registry)
        validator.validate(source, registry, authority)

        val registryContent = persistence.serialize(registry)
        val authorityContent = persistence.serialize(authority)

        persistence.writeNew(paths.entityIdRegistry, registryContent)
        persistence.writeNew(paths.familyAuthority, authorityContent)

        return loadValidatedResult(
            mode = HimCanonicalFamilyBootstrapMode.INITIAL_BOOTSTRAP,
            paths = paths,
            source = source,
        )
    }

    private fun reuse(
        paths: HimCanonicalFamilyPaths,
        source: HimProductOnlyCanonicalMaster,
    ): HimCanonicalFamilyBootstrapResult {
        val registry = persistence.readRegistry(paths.entityIdRegistry)
        val authority = persistence.readAuthority(paths.familyAuthority)
        val expectedAuthority = createAuthority(source, registry)

        require(authority == expectedAuthority) {
            "Persisted Canonical Family Authority differs from the current source and registry."
        }

        return loadValidatedResult(
            mode = HimCanonicalFamilyBootstrapMode.EXISTING_REGISTRY_REUSE,
            paths = paths,
            source = source,
        )
    }

    private fun createAuthority(
        source: HimProductOnlyCanonicalMaster,
        registry: HimEntityIdRegistry,
    ): HimCanonicalFamilyAuthority {
        val entriesByReference =
            registry.entries.associateBy { it.sourceReference }

        return HimCanonicalFamilyAuthority(
            schemaVersion = HimCanonicalFamilyValidator.AUTHORITY_SCHEMA_VERSION,
            sourceCatalog =
                HimCanonicalFamilySourceCatalog(
                    path = source.path,
                    contentSha256 = source.contentSha256,
                    recordCount = source.records.size,
                ),
            families = source.records.map { record ->
                val entry = requireNotNull(entriesByReference[record.normalized]) {
                    "No persisted HIM entity ID for canonical: ${record.normalized}"
                }
                require(entry.entityType == HimEntityType.CANONICAL)
                require(entry.sourceReferenceType ==
                        HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED)

                HimCanonicalFamily(
                    canonicalId = entry.entityId,
                    canonicalName = record.itemname,
                    normalizedName = record.normalized,
                    taxonomyPaths = record.taxonomyPaths,
                    lifecycleStatus = HimLifecycleStatus.ACTIVE,
                    identities = emptyList(),
                    variants = emptyList(),
                    aliases = emptyList(),
                )
            },
        )
    }

    private fun loadValidatedResult(
        mode: HimCanonicalFamilyBootstrapMode,
        paths: HimCanonicalFamilyPaths,
        source: HimProductOnlyCanonicalMaster,
    ): HimCanonicalFamilyBootstrapResult {
        val registry = persistence.readRegistry(paths.entityIdRegistry)
        val authority = persistence.readAuthority(paths.familyAuthority)
        val validation = validator.validate(source, registry, authority)

        return HimCanonicalFamilyBootstrapResult(
            mode = mode,
            registry = registry,
            authority = authority,
            validation = validation,
            registrySha256 = HimProductOnlyCanonicalMasterReader.sha256(paths.entityIdRegistry),
            authoritySha256 = HimProductOnlyCanonicalMasterReader.sha256(paths.familyAuthority),
        )
    }
}
