package de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndex
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndexPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthArtifactsV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCanonicalFamilyMutationLedger
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCanonicalFamilyMutationLedgerPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthEntityFingerprintIndexBuilderV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthEntityFingerprintIndexValidatorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthRelease
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseArtifactReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseBuildInputV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseBuilderV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleasePersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseSources
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseValidatorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimRetiredEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimTransactionalGroundTruthPublicationInputV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimTransactionalGroundTruthPublicationContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimTransactionalGroundTruthPublicationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimMutationReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityStatus
import java.io.File
import java.security.MessageDigest

data class HimDeterministicPromotionExecutionResultV1(
    val previousReleaseReference: HimGroundTruthReleaseIdentityV1,
    val newReleaseReference: HimGroundTruthReleaseIdentityV1,
    val newEntityId: HimEntityId,
    val mutationReference: HimMutationReference,
    val promotionType: HimCandidatePromotionType,
    val targetType: HimEntityType,
    val createdNewRelease: Boolean,
    val activeReleaseVerified: Boolean,
)

/**
 * Executes one already validated child promotion as one immutable
 * Ground-Truth release transition.
 *
 * This class deliberately has no Entity-ID generation, catalog, dimension,
 * network, or inference responsibility.
 */
class HimDeterministicPromotionExecutorV1(
    private val familyPersistence: HimCanonicalFamilyPersistence =
        HimCanonicalFamilyPersistence(),
    private val ledgerPersistence: HimCanonicalFamilyMutationLedgerPersistenceV1 =
        HimCanonicalFamilyMutationLedgerPersistenceV1(),
    private val fingerprintPersistence: HimEntityFingerprintIndexPersistence =
        HimEntityFingerprintIndexPersistence(),
    private val releasePersistence: HimGroundTruthReleasePersistenceV1 =
        HimGroundTruthReleasePersistenceV1(),
    private val activeResolution: HimActiveGroundTruthResolutionV1 =
        HimActiveGroundTruthResolutionV1(),
    private val childMutation: HimCanonicalFamilyChildMutationV1 =
        HimCanonicalFamilyChildMutationV1(familyPersistence),
    private val fingerprintBuilder: HimGroundTruthEntityFingerprintIndexBuilderV1 =
        HimGroundTruthEntityFingerprintIndexBuilderV1(),
    private val fingerprintValidator: HimGroundTruthEntityFingerprintIndexValidatorV1 =
        HimGroundTruthEntityFingerprintIndexValidatorV1(),
    private val releaseBuilder: HimGroundTruthReleaseBuilderV1 =
        HimGroundTruthReleaseBuilderV1(),
    private val releaseValidator: HimGroundTruthReleaseValidatorV1 =
        HimGroundTruthReleaseValidatorV1(),
    private val publisher: HimTransactionalGroundTruthPublicationV1 =
        HimTransactionalGroundTruthPublicationV1(),
) {

    fun execute(
        projectRoot: File,
        promotion: HimCandidatePromotionProposalV1,
        newEntityId: HimEntityId,
        eligibility: HimCandidatePromotionEligibilityResultV1,
    ): HimDeterministicPromotionExecutionResultV1 {
        require(promotion.target !is HimCandidatePromotionTargetV1.CreateCanonical) {
            "CREATE_CANONICAL is outside F3.7d.6 promotion scope."
        }

        validatePromotion(promotion, eligibility)

        val before = activeResolution.resolve(projectRoot)
        val stateBefore = readState(before)
        validateActiveState(before, stateBefore)

        val mutation = childMutation.apply(
            authorityBefore = stateBefore.authority,
            registryBefore = stateBefore.activeRegistry,
            promotion = promotion,
            validationReference =
                de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimValidationReference(
                    promotion.validationReference.value,
                ),
            newEntityId = newEntityId,
            authoritySha256Before = sha256(before.authorityBytes),
            registrySha256Before = sha256(before.activeRegistryBytes),
        )

        val ledgerAfter = ledgerPersistence.addEntry(
            ledger = stateBefore.mutationLedger,
            entry = mutation.mutationLedgerEntry,
        )

        val authorityAfterBytes = familyPersistence.serialize(mutation.authorityAfter)
        val registryAfterBytes = familyPersistence.serialize(mutation.registryAfter)
        val retiredRegistryAfterBytes = serializeRetiredRegistry(stateBefore.retiredRegistry)
        val ledgerAfterBytes = ledgerPersistence.serialize(ledgerAfter)

        val authorityAfterSha = sha256(authorityAfterBytes)
        val registryAfterSha = sha256(registryAfterBytes)
        val fingerprintAfter = fingerprintBuilder.build(
            registry = mutation.registryAfter,
            authority = mutation.authorityAfter,
            registrySha256 = registryAfterSha.value,
            authoritySha256 = authorityAfterSha.value,
        )
        fingerprintValidator.validate(
            registry = mutation.registryAfter,
            authority = mutation.authorityAfter,
            index = fingerprintAfter,
            registrySha256 = registryAfterSha.value,
            authoritySha256 = authorityAfterSha.value,
        )
        val fingerprintAfterBytes = fingerprintPersistence.serialize(fingerprintAfter)

        val releaseInput = releaseInput(
            authority = mutation.authorityAfter,
            activeRegistry = mutation.registryAfter,
            retiredRegistry = stateBefore.retiredRegistry,
            mutationLedger = ledgerAfter,
            fingerprintIndex = fingerprintAfter,
            authorityBytes = authorityAfterBytes,
            activeRegistryBytes = registryAfterBytes,
            retiredRegistryBytes = retiredRegistryAfterBytes,
            mutationLedgerBytes = ledgerAfterBytes,
            fingerprintIndexBytes = fingerprintAfterBytes,
            artifactPaths = ArtifactPaths.from(stateBefore.release.sources),
        )
        val releaseAfter = releaseBuilder.build(releaseInput)
        releaseValidator.validate(releaseAfter, releaseInput)
        val releaseAfterBytes = releasePersistence.serialize(releaseAfter)

        val publicationInput = HimTransactionalGroundTruthPublicationInputV1(
            authorityBytes = authorityAfterBytes,
            activeRegistryBytes = registryAfterBytes,
            retiredRegistryBytes = retiredRegistryAfterBytes,
            mutationLedgerBytes = ledgerAfterBytes,
            fingerprintIndexBytes = fingerprintAfterBytes,
            releaseBytes = releaseAfterBytes,
        )

        val publication = publisher.publish(
            projectRoot = projectRoot,
            input = publicationInput,
        ) { releaseDirectory ->
            validatePublishedRelease(
                releaseDirectory = releaseDirectory,
                input = publicationInput,
            )
        }

        val after = activeResolution.resolve(projectRoot)
        require(after.releaseReference == publication.releaseReference) {
            "Post-commit active release reference does not match publication."
        }
        require(after.authorityBytes.contentEquals(authorityAfterBytes)) {
            "Post-commit Authority bytes do not match AuthorityAfter."
        }
        require(after.activeRegistryBytes.contentEquals(registryAfterBytes)) {
            "Post-commit active Registry bytes do not match RegistryAfter."
        }
        require(after.retiredRegistryBytes.contentEquals(retiredRegistryAfterBytes)) {
            "Post-commit retired Registry bytes do not match expected bytes."
        }
        require(after.mutationLedgerBytes.contentEquals(ledgerAfterBytes)) {
            "Post-commit Mutation Ledger bytes do not match expected bytes."
        }
        require(after.fingerprintIndexBytes.contentEquals(fingerprintAfterBytes)) {
            "Post-commit Fingerprint Index bytes do not match expected bytes."
        }
        require(after.releaseBytes.contentEquals(releaseAfterBytes)) {
            "Post-commit Ground-Truth Release bytes do not match expected bytes."
        }

        return HimDeterministicPromotionExecutionResultV1(
            previousReleaseReference = before.releaseReference,
            newReleaseReference = publication.releaseReference,
            newEntityId = newEntityId,
            mutationReference = mutation.mutationLedgerEntry.mutationReference,
            promotionType = promotion.target.promotionType,
            targetType = mutation.mutationLedgerEntry.entityType,
            createdNewRelease = publication.created,
            activeReleaseVerified = true,
        )
    }

    private fun validatePromotion(
        promotion: HimCandidatePromotionProposalV1,
        eligibility: HimCandidatePromotionEligibilityResultV1,
    ) {
        require(
            eligibility.status == HimCandidatePromotionEligibilityStatus.PROMOTION_ELIGIBLE
        ) {
            "Candidate is not promotion eligible: ${eligibility.status}"
        }
        require(eligibility.candidateReference == promotion.candidateReference) {
            "Promotion eligibility belongs to a different candidate."
        }
        require(eligibility.validationReference?.value == promotion.validationReference.value) {
            "Promotion eligibility validation reference mismatch."
        }

        val expectedReference = HimCandidatePromotionIdentityV1.promotion(
            candidateReference = promotion.candidateReference,
            validationReference = promotion.validationReference,
            candidateDatasetDigest = promotion.candidateDatasetDigest,
            authorityDigestBefore = promotion.authorityDigestBefore,
            entityIdRegistryDigestBefore = promotion.entityIdRegistryDigestBefore,
            target = promotion.target,
        )
        require(expectedReference == promotion.promotionReference) {
            "Promotion reference does not match the promotion contents."
        }
    }

    private fun readState(
        active: HimActiveGroundTruthArtifactsV1,
    ): State {
        val retiredRegistry = gson.fromJson(
            active.retiredRegistryBytes.toString(Charsets.UTF_8),
            HimRetiredEntityIdRegistry::class.java,
        ) ?: error("Active retired Entity-ID Registry is invalid JSON.")

        return State(
            authority = familyPersistence.readAuthority(active.authorityFile),
            activeRegistry = familyPersistence.readRegistry(active.activeRegistryFile),
            retiredRegistry = retiredRegistry,
            mutationLedger = ledgerPersistence.read(active.mutationLedgerFile),
            fingerprintIndex = fingerprintPersistence.read(active.fingerprintIndexFile),
            release = releasePersistence.read(active.releaseFile),
        )
    }

    private fun validateActiveState(
        active: HimActiveGroundTruthArtifactsV1,
        state: State,
    ) {
        requireArtifactSha(active.authorityBytes, state.release.sources.canonicalFamilyAuthority.sha256)
        requireArtifactSha(active.activeRegistryBytes, state.release.sources.activeEntityIdRegistry.sha256)
        requireArtifactSha(active.retiredRegistryBytes, state.release.sources.retiredEntityIdRegistry.sha256)
        requireArtifactSha(active.mutationLedgerBytes, state.release.sources.mutationLedger.sha256)
        requireArtifactSha(active.fingerprintIndexBytes, state.release.sources.entityFingerprintIndex.sha256)

        fingerprintValidator.validate(
            registry = state.activeRegistry,
            authority = state.authority,
            index = state.fingerprintIndex,
            registrySha256 = sha256(active.activeRegistryBytes).value,
            authoritySha256 = sha256(active.authorityBytes).value,
        )

        releaseValidator.validate(
            release = state.release,
            input = releaseInput(
                authority = state.authority,
                activeRegistry = state.activeRegistry,
                retiredRegistry = state.retiredRegistry,
                mutationLedger = state.mutationLedger,
                fingerprintIndex = state.fingerprintIndex,
                authorityBytes = active.authorityBytes,
                activeRegistryBytes = active.activeRegistryBytes,
                retiredRegistryBytes = active.retiredRegistryBytes,
                mutationLedgerBytes = active.mutationLedgerBytes,
                fingerprintIndexBytes = active.fingerprintIndexBytes,
                artifactPaths = ArtifactPaths.from(state.release.sources),
            ),
        )
    }

    private fun releaseInput(
        authority: HimCanonicalFamilyAuthority,
        activeRegistry: HimEntityIdRegistry,
        retiredRegistry: HimRetiredEntityIdRegistry,
        mutationLedger: HimCanonicalFamilyMutationLedger,
        fingerprintIndex: HimEntityFingerprintIndex,
        authorityBytes: ByteArray,
        activeRegistryBytes: ByteArray,
        retiredRegistryBytes: ByteArray,
        mutationLedgerBytes: ByteArray,
        fingerprintIndexBytes: ByteArray,
        artifactPaths: ArtifactPaths = ArtifactPaths.DEFAULT,
    ): HimGroundTruthReleaseBuildInputV1 {
        return HimGroundTruthReleaseBuildInputV1(
            authority = authority,
            activeEntityIdRegistry = activeRegistry,
            retiredEntityIdRegistry = retiredRegistry,
            mutationLedger = mutationLedger,
            entityFingerprintIndex = fingerprintIndex,
            authorityArtifact = artifactPaths.authority(
                sha256(authorityBytes), authority.families.size,
            ),
            activeEntityIdRegistryArtifact = artifactPaths.registry(
                sha256(activeRegistryBytes), activeRegistry.entries.size,
            ),
            retiredEntityIdRegistryArtifact = artifactPaths.retiredRegistry(
                sha256(retiredRegistryBytes), retiredRegistry.entries.size,
            ),
            mutationLedgerArtifact = artifactPaths.ledger(
                sha256(mutationLedgerBytes), mutationLedger.entries.size,
            ),
            entityFingerprintIndexArtifact = artifactPaths.fingerprint(
                sha256(fingerprintIndexBytes), fingerprintIndex.entryCount,
            ),
        )
    }

    private fun validatePublishedRelease(
        releaseDirectory: File,
        input: HimTransactionalGroundTruthPublicationInputV1,
    ) {
        val authorityFile = releaseDirectory.resolve(HimTransactionalGroundTruthPublicationContractV1.AUTHORITY_FILE_NAME)
        val registryFile = releaseDirectory.resolve(HimTransactionalGroundTruthPublicationContractV1.ACTIVE_REGISTRY_FILE_NAME)
        val retiredFile = releaseDirectory.resolve(HimTransactionalGroundTruthPublicationContractV1.RETIRED_REGISTRY_FILE_NAME)
        val ledgerFile = releaseDirectory.resolve(HimTransactionalGroundTruthPublicationContractV1.MUTATION_LEDGER_FILE_NAME)
        val fingerprintFile = releaseDirectory.resolve(HimTransactionalGroundTruthPublicationContractV1.FINGERPRINT_INDEX_FILE_NAME)
        val releaseFile = releaseDirectory.resolve(HimTransactionalGroundTruthPublicationContractV1.RELEASE_FILE_NAME)

        require(authorityFile.readBytes().contentEquals(input.authorityBytes))
        require(registryFile.readBytes().contentEquals(input.activeRegistryBytes))
        require(retiredFile.readBytes().contentEquals(input.retiredRegistryBytes))
        require(ledgerFile.readBytes().contentEquals(input.mutationLedgerBytes))
        require(fingerprintFile.readBytes().contentEquals(input.fingerprintIndexBytes))
        require(releaseFile.readBytes().contentEquals(input.releaseBytes))

        val authority = familyPersistence.readAuthority(authorityFile)
        val registry = familyPersistence.readRegistry(registryFile)
        val retired = gson.fromJson(
            input.retiredRegistryBytes.toString(Charsets.UTF_8),
            HimRetiredEntityIdRegistry::class.java,
        ) ?: error("Published retired Entity-ID Registry is invalid JSON.")
        val ledger = ledgerPersistence.read(ledgerFile)
        val fingerprint = fingerprintPersistence.read(fingerprintFile)
        val release = releasePersistence.read(releaseFile)

        fingerprintValidator.validate(
            registry = registry,
            authority = authority,
            index = fingerprint,
            registrySha256 = sha256(input.activeRegistryBytes).value,
            authoritySha256 = sha256(input.authorityBytes).value,
        )
        releaseValidator.validate(
            release = release,
            input = releaseInput(
                authority = authority,
                activeRegistry = registry,
                retiredRegistry = retired,
                mutationLedger = ledger,
                fingerprintIndex = fingerprint,
                authorityBytes = input.authorityBytes,
                activeRegistryBytes = input.activeRegistryBytes,
                retiredRegistryBytes = input.retiredRegistryBytes,
                mutationLedgerBytes = input.mutationLedgerBytes,
                fingerprintIndexBytes = input.fingerprintIndexBytes,
                artifactPaths = ArtifactPaths.from(release.sources),
            ),
        )
    }

    private fun requireArtifactSha(bytes: ByteArray, expected: HimSha256) {
        require(sha256(bytes) == expected) {
            "Active Ground-Truth artifact SHA-256 does not match its release binding."
        }
    }

    private fun sha256(bytes: ByteArray): HimSha256 = HimSha256(
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) },
    )

    private fun serializeRetiredRegistry(value: HimRetiredEntityIdRegistry): ByteArray =
        (gson.toJson(value) + "\n").toByteArray(Charsets.UTF_8)

    private data class State(
        val authority: HimCanonicalFamilyAuthority,
        val activeRegistry: HimEntityIdRegistry,
        val retiredRegistry: HimRetiredEntityIdRegistry,
        val mutationLedger: HimCanonicalFamilyMutationLedger,
        val fingerprintIndex: HimEntityFingerprintIndex,
        val release: HimGroundTruthRelease,
    )

    private data class ArtifactPaths(
        val authorityPath: String,
        val registryPath: String,
        val retiredRegistryPath: String,
        val ledgerPath: String,
        val fingerprintPath: String,
    ) {
        fun authority(sha: HimSha256, count: Int) =
            HimGroundTruthReleaseArtifactReference(authorityPath, sha, count)

        fun registry(sha: HimSha256, count: Int) =
            HimGroundTruthReleaseArtifactReference(registryPath, sha, count)

        fun retiredRegistry(sha: HimSha256, count: Int) =
            HimGroundTruthReleaseArtifactReference(retiredRegistryPath, sha, count)

        fun ledger(sha: HimSha256, count: Int) =
            HimGroundTruthReleaseArtifactReference(ledgerPath, sha, count)

        fun fingerprint(sha: HimSha256, count: Int) =
            HimGroundTruthReleaseArtifactReference(fingerprintPath, sha, count)

        companion object {
            fun from(sources: HimGroundTruthReleaseSources) = ArtifactPaths(
                authorityPath = sources.canonicalFamilyAuthority.path,
                registryPath = sources.activeEntityIdRegistry.path,
                retiredRegistryPath = sources.retiredEntityIdRegistry.path,
                ledgerPath = sources.mutationLedger.path,
                fingerprintPath = sources.entityFingerprintIndex.path,
            )

            val DEFAULT = ArtifactPaths(
                authorityPath = HimTransactionalGroundTruthPublicationContractV1.AUTHORITY_FILE_NAME,
                registryPath = HimTransactionalGroundTruthPublicationContractV1.ACTIVE_REGISTRY_FILE_NAME,
                retiredRegistryPath = HimTransactionalGroundTruthPublicationContractV1.RETIRED_REGISTRY_FILE_NAME,
                ledgerPath = HimTransactionalGroundTruthPublicationContractV1.MUTATION_LEDGER_FILE_NAME,
                fingerprintPath = HimTransactionalGroundTruthPublicationContractV1.FINGERPRINT_INDEX_FILE_NAME,
            )
        }
    }

    private companion object {
        val gson = GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create()

    }
}
