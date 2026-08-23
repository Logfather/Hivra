package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.promotion

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndexPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistryEntry
import de.shopme.tools.knowledge.him.canonical.family.HimEntitySourceReferenceType
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthArtifactsV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCanonicalFamilyMutationLedger
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthEntityFingerprintIndexBuilderV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthEntityFingerprintIndexValidatorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseArtifactReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseBuildInputV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleasePersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseValidatorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimRetiredEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimTransactionalGroundTruthPublicationContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDataset
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetContractV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetPersistenceV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityGateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecision
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationLedgerPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationReason
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationRecord
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimProductionIndexFileIdentityReleaseContractV1
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class RunHimFirstRealHeringPromotionV1Test {

    @Test
    fun `real Hering promotion audits, dry-runs, and honors production opt-in`() {
        val root = projectRoot()
        val before = guardSnapshot(root)
        val validationBefore = validationFile(root).takeIf { it.isFile }?.readBytes()

        val activeAtStart = HimActiveGroundTruthResolutionV1().resolve(root)
        if (activeAtStart.releaseReference.value != EXPECTED_RELEASE) {
            val dataset = readDataset(root)
            val candidate = dataset.candidates.single { it.candidate.candidateReference.value == CANDIDATE_REFERENCE }.candidate
            val datasetDigest = HimCandidateIdentityV1.datasetDigest(dataset)
            val validationReference = HimCandidateValidationIdentityV1.validation(
                candidateReference = candidate.candidateReference,
                candidateDatasetDigest = datasetDigest,
                decision = HimCandidateValidationDecision.APPROVE,
                reason = HimCandidateValidationReason.SEMANTICALLY_CORRECT,
                supersededByCandidateReference = null,
            )
            val validation = HimCandidateValidationLedgerPersistenceV1.read(validationFile(root)).validations.single { it.candidateReference == candidate.candidateReference }
            require(validation.validationReference == validationReference)
            require(validation.decision == HimCandidateValidationDecision.APPROVE)
            val state = readActiveState(activeAtStart)
            validateActiveState(activeAtStart, state)
            assertAlreadyPromoted(null, activeAtStart, dataset)
            assertEquals(before, guardSnapshot(root))
            return
        }

        val prepared = prepare(root, persistValidation = false)

        assertEquals(EXPECTED_RELEASE, prepared.active.releaseReference.value)
        assertEquals(EXPECTED_RELEASE_SHA, sha256(prepared.active.releaseBytes).value)
        assertEquals(HimCandidatePromotionEligibilityStatus.PROMOTION_ELIGIBLE, prepared.eligibility.status)
        assertEquals(EXPECTED_PROMOTION_REFERENCE, prepared.promotion.promotionReference.value)
        assertEquals(EXPECTED_ENTITY_ID, prepared.entityId.value)

        val first = dryRun(prepared)
        val second = dryRun(prepared)
        assertDeterministic(first, second)

        val after = guardSnapshot(root)
        assertEquals(before, after)
        assertEquals(validationBefore, validationFile(root).takeIf { it.isFile }?.readBytes())
        assertFalse(currentReleaseIsDifferent(root, EXPECTED_RELEASE))

        if (System.getenv(PRODUCTION_OPT_IN) == "true") {
            val releaseNFilesBefore = releaseArtifactSnapshot(prepared.active.releaseDirectory)
            val validationPersistence = HimCandidateValidationLedgerPersistenceV1
            val validationFile = validationFile(root)
            val validationLedger = validationPersistence.addValidation(
                validationPersistence.read(validationFile),
                prepared.validation,
            )
            if (validationPersistence.read(validationFile) != validationLedger) {
                validationPersistence.write(validationFile, validationLedger)
            }
            val reloadedValidation = validationPersistence.read(validationFile)
            assertEquals(validationLedger, reloadedValidation)
            val eligibility = HimCandidatePromotionEligibilityGateV1.requireEligible(
                candidateReference = prepared.candidate.candidateReference,
                candidateDataset = prepared.dataset,
                currentCandidateDatasetDigest = HimCandidateIdentityV1.datasetDigest(prepared.dataset),
                validationLedger = reloadedValidation,
            )
            val execution = HimDeterministicPromotionExecutorV1().execute(
                projectRoot = root,
                promotion = prepared.promotion,
                newEntityId = prepared.entityId,
                eligibility = eligibility,
            )
            val activeAfter = HimActiveGroundTruthResolutionV1().resolve(root)
            assertEquals(execution.newReleaseReference, activeAfter.releaseReference)
            assertNotEquals(EXPECTED_RELEASE, activeAfter.releaseReference.value)
            assertSemanticDelta(prepared, activeAfter)
            validateActiveState(activeAfter, readActiveState(activeAfter))
            assertEquals(releaseNFilesBefore, releaseArtifactSnapshot(prepared.active.releaseDirectory))
            assertEquals(prepared.dataset, readDataset(root))
            assertNonValidationGuardsEqual(before, guardSnapshot(root))
            assertEquals(1, validationPersistence.read(validationFile).validations.count { it.candidateReference == prepared.candidate.candidateReference })
        }
    }

    @Test
    fun `idempotency is recognized in an isolated copy without a second release`() {
        val root = fixtureRoot()
        try {
            materializeActiveState(projectRoot(), root)
            val prepared = prepare(root, persistValidation = true)
            val first = executeDryRun(root, prepared)
            val releaseDirectoriesBefore = releaseDirectories(root)
            val currentBefore = currentPointer(root).readBytes()

            val activeAfter = HimActiveGroundTruthResolutionV1().resolve(root)
            assertAlreadyPromoted(
                prepared = prepared,
                active = activeAfter,
                candidateDataset = readDataset(root),
            )
            assertEquals(releaseDirectoriesBefore, releaseDirectories(root))
            assertArrayEquals(currentBefore, currentPointer(root).readBytes())
            assertTrue(first.execution.createdNewRelease)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun prepare(root: File, persistValidation: Boolean): Prepared {
        val datasetFile = candidateFile(root)
        val datasetBytesBefore = datasetFile.readBytes()
        val dataset = HimCandidateDatasetPersistenceV2.readDataset(datasetFile)
        val candidates = dataset.candidates.filter { it.candidate.candidateReference.value == CANDIDATE_REFERENCE }
        require(candidates.size == 1) { "Expected exactly one persisted Hering Candidate." }
        val candidate = candidates.single().candidate
        require(candidate.candidateTerm == CANDIDATE_TERM)
        require(candidate.confidence == de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence.HIGH)
        require(candidate.relation == HimCandidateRelation.Variant(HimFamilyEntityReference.Canonical(HERRING_ID)))

        val runMatches = dataset.runs.filter { it.runReference.value == GENERATION_RUN }
        require(runMatches.size == 1) { "Expected exactly one persisted Hering generation run." }
        val run = runMatches.single()
        require(run.occurrences.any { it.candidate.candidateReference.value == CANDIDATE_REFERENCE })
        val evidence = candidates.single().occurrences.flatMap { it.evidence }.map { it.reference.sourceRecordIdentity }.distinct()
        require(EXPECTED_EVIDENCE.all { it in evidence })

        val active = HimActiveGroundTruthResolutionV1().resolve(root)
        require(active.releaseReference.value == EXPECTED_RELEASE) { "Active Ground Truth is not Release N." }
        require(sha256(active.releaseBytes).value == EXPECTED_RELEASE_SHA)
        val state = readActiveState(active)
        validateActiveState(active, state)
        val herring = state.authority.families.filter { it.canonicalId == HERRING_ID }
        require(herring.size == 1)
        require(herring.single().canonicalName == HERRING_NAME)

        val datasetDigest = HimCandidateIdentityV1.datasetDigest(dataset)
        val validationReference = HimCandidateValidationIdentityV1.validation(
            candidateReference = candidate.candidateReference,
            candidateDatasetDigest = datasetDigest,
            decision = HimCandidateValidationDecision.APPROVE,
            reason = HimCandidateValidationReason.SEMANTICALLY_CORRECT,
            supersededByCandidateReference = null,
        )
        val validation = HimCandidateValidationRecord(
            validationReference = validationReference,
            candidateReference = candidate.candidateReference,
            candidateDatasetDigest = datasetDigest,
            decision = HimCandidateValidationDecision.APPROVE,
            reason = HimCandidateValidationReason.SEMANTICALLY_CORRECT,
            rationale = RATIONALE,
        )
        val validationPersistence = HimCandidateValidationLedgerPersistenceV1
        val validationFile = validationFile(root)
        val validationBefore = validationPersistence.read(validationFile)
        val validationAfter = validationPersistence.addValidation(validationBefore, validation)
        if (persistValidation && validationAfter != validationBefore) {
            validationPersistence.write(validationFile, validationAfter)
        }
        val reloadedValidation = validationPersistence.read(validationFile).let {
            if (persistValidation) it else validationAfter
        }
        val eligibility = HimCandidatePromotionEligibilityGateV1.requireEligible(
            candidateReference = candidate.candidateReference,
            candidateDataset = dataset,
            currentCandidateDatasetDigest = datasetDigest,
            validationLedger = reloadedValidation,
        )
        require(eligibility.status == HimCandidatePromotionEligibilityStatus.PROMOTION_ELIGIBLE)

        val promotion = HimCandidatePromotionProposalFactoryV1.create(
            candidateReference = candidate.candidateReference,
            candidateRelation = candidate.relation,
            candidateTerm = candidate.candidateTerm,
            candidateDatasetDigest = datasetDigest,
            authorityDigestBefore = sha256(active.authorityBytes),
            entityIdRegistryDigestBefore = sha256(active.activeRegistryBytes),
            eligibility = eligibility,
        )
        require(promotion.promotionReference.value == EXPECTED_PROMOTION_REFERENCE)

        val existing = existingPromotion(state, promotion.promotionReference.value)
        if (existing != null) {
            require(existing.entityId.value == EXPECTED_ENTITY_ID)
            require(existing.variantName == CANDIDATE_TERM)
            require(validationAfter.validations.any { it.validationReference == validationReference })
        }

        val allocator = HimDeterministicGroundTruthEntityIdAllocatorV1()
        val entityId = allocator.allocate(
            promotionReference = promotion.promotionReference,
            entityType = HimEntityType.VARIANT,
            registry = state.registry,
            authority = state.authority,
        )
        require(entityId.value.matches(Regex("[0-9A-Za-z]{6}")))
        require(entityId.value == EXPECTED_ENTITY_ID)
        require(entityId == allocator.allocate(promotion.promotionReference, HimEntityType.VARIANT, state.registry, state.authority))
        require(state.registry.entries.none { it.entityId == entityId })
        require(state.retired.entries.none { it.entityId == entityId })

        require(datasetFile.readBytes().contentEquals(datasetBytesBefore))
        return Prepared(
            root = root,
            dataset = dataset,
            candidate = candidate,
            active = active,
            state = state,
            validation = validation,
            validationReference = validationReference,
            eligibility = eligibility,
            promotion = promotion,
            entityId = entityId,
            validationBefore = validationBefore,
        )
    }

    private fun dryRun(prepared: Prepared): DryRunResult {
        val root = fixtureRoot()
        return try {
            materializeActiveState(prepared.root, root)
            executeDryRun(root, prepare(root, persistValidation = true))
        } finally {
            root.deleteRecursively()
        }
    }

    private fun executeDryRun(root: File, prepared: Prepared): DryRunResult {
        val releaseNFilesBefore = releaseArtifactSnapshot(prepared.active.releaseDirectory)
        val execution = HimDeterministicPromotionExecutorV1().execute(
            projectRoot = root,
            promotion = prepared.promotion,
            newEntityId = prepared.entityId,
            eligibility = prepared.eligibility,
        )
        val after = HimActiveGroundTruthResolutionV1().resolve(root)
        assertSemanticDelta(prepared, after)
        assertEquals(prepared.active.releaseReference, execution.previousReleaseReference)
        assertEquals(EXPECTED_ENTITY_ID, execution.newEntityId.value)
        assertTrue(execution.createdNewRelease)
        assertTrue(execution.activeReleaseVerified)
        assertEquals(1, readActiveState(after).ledger.entries.size)
        assertEquals(releaseNFilesBefore, releaseArtifactSnapshot(prepared.active.releaseDirectory))
        return DryRunResult(
            execution = execution,
            active = after,
            authorityBytes = after.authorityBytes,
            registryBytes = after.activeRegistryBytes,
            retiredBytes = after.retiredRegistryBytes,
            ledgerBytes = after.mutationLedgerBytes,
            fingerprintBytes = after.fingerprintIndexBytes,
            releaseBytes = after.releaseBytes,
        )
    }

    private fun assertDeterministic(first: DryRunResult, second: DryRunResult) {
        assertEquals(first.execution.newReleaseReference, second.execution.newReleaseReference)
        assertEquals(first.execution.mutationReference, second.execution.mutationReference)
        assertArrayEquals(first.authorityBytes, second.authorityBytes)
        assertArrayEquals(first.registryBytes, second.registryBytes)
        assertArrayEquals(first.retiredBytes, second.retiredBytes)
        assertArrayEquals(first.ledgerBytes, second.ledgerBytes)
        assertArrayEquals(first.fingerprintBytes, second.fingerprintBytes)
        assertArrayEquals(first.releaseBytes, second.releaseBytes)
    }

    private fun assertAlreadyPromoted(
        prepared: Prepared?,
        active: HimActiveGroundTruthArtifactsV1,
        candidateDataset: HimCandidateDataset?,
    ) {
        val state = readActiveState(active)
        val herring = state.authority.families.single { it.canonicalId == HERRING_ID }
        val variant = herring.variants.single { it.variantName == CANDIDATE_TERM }
        assertEquals(EXPECTED_ENTITY_ID, variant.variantId.value)
        val registryEntry = state.registry.entries.single { it.entityId.value == EXPECTED_ENTITY_ID }
        assertEquals(HimEntityType.VARIANT, registryEntry.entityType)
        assertEquals(HimEntitySourceReferenceType.GROUND_TRUTH_PROMOTION, registryEntry.sourceReferenceType)
        assertEquals(EXPECTED_PROMOTION_REFERENCE, registryEntry.sourceReference)
        assertEquals(1, state.ledger.entries.count { it.newEntityId.value == EXPECTED_ENTITY_ID })
        if (prepared != null) {
            assertEquals(prepared.validationReference.value, state.ledger.entries.single().validationReference.value)
        }
        if (candidateDataset != null) {
            assertTrue(candidateDataset.candidates.any { it.candidate.candidateReference.value == CANDIDATE_REFERENCE })
        }
    }

    private fun validateActiveState(active: HimActiveGroundTruthArtifactsV1, state: ActiveState) {
        val indexResult = HimGroundTruthEntityFingerprintIndexValidatorV1().validate(
            registry = state.registry,
            authority = state.authority,
            index = state.fingerprint,
            registrySha256 = sha256(active.activeRegistryBytes).value,
            authoritySha256 = sha256(active.authorityBytes).value,
        )
        assertEquals(state.fingerprint.entries.size, indexResult.entries)
        HimGroundTruthReleaseValidatorV1().validate(
            release = state.release,
            input = releaseInput(state, active),
        )
    }

    private fun assertSemanticDelta(prepared: Prepared, after: HimActiveGroundTruthArtifactsV1) {
        val beforeAuthority = prepared.state.authority
        val afterState = readActiveState(after)
        assertEquals(beforeAuthority.families.size, afterState.authority.families.size)
        assertEquals(0, afterState.authority.families.size - beforeAuthority.families.size)
        assertEquals(0, afterState.authority.families.sumOf { it.identities.size } - beforeAuthority.families.sumOf { it.identities.size })
        assertEquals(1, afterState.authority.families.sumOf { it.variants.size } - beforeAuthority.families.sumOf { it.variants.size })
        assertEquals(0, afterState.authority.authorityAliasCount() - beforeAuthority.authorityAliasCount())
        assertEquals(prepared.state.registry.entries.size + 1, afterState.registry.entries.size)
        assertEquals(prepared.state.ledger.entries.size + 1, afterState.ledger.entries.size)
        assertEquals(prepared.state.retired.entries, afterState.retired.entries)
        val herring = afterState.authority.families.single { it.canonicalId == HERRING_ID }
        assertEquals(1, herring.variants.count { it.variantId.value == EXPECTED_ENTITY_ID })
        assertEquals(CANDIDATE_TERM, herring.variants.single { it.variantId.value == EXPECTED_ENTITY_ID }.variantName)
        assertTrue(herring.identities.isEmpty())
        assertTrue(herring.aliases.isEmpty())
    }

    private fun existingPromotion(state: ActiveState, promotionReference: String): ExistingPromotion? {
        val family = state.authority.families.singleOrNull { it.canonicalId == HERRING_ID } ?: return null
        val variants = family.variants.filter { it.variantName == CANDIDATE_TERM }
        if (variants.isEmpty()) return null
        require(variants.size == 1) { "Duplicate Hering variant semantic name." }
        val variant = variants.single()
        val registry = state.registry.entries.singleOrNull { it.entityId == variant.variantId }
            ?: error("Existing Hering variant is absent from the active Registry.")
        require(registry.sourceReference == promotionReference) { "Existing Hering variant has a conflicting promotion reference." }
        require(state.ledger.entries.count { it.newEntityId == variant.variantId } == 1)
        return ExistingPromotion(variant.variantId, variant.variantName)
    }

    private fun readActiveState(active: HimActiveGroundTruthArtifactsV1): ActiveState {
        val persistence = HimCanonicalFamilyPersistence()
        val retired = GSON.fromJson(active.retiredRegistryBytes.toString(Charsets.UTF_8), HimRetiredEntityIdRegistry::class.java)
        return ActiveState(
            authority = persistence.readAuthority(active.authorityFile),
            registry = persistence.readRegistry(active.activeRegistryFile),
            retired = retired,
            ledger = de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCanonicalFamilyMutationLedgerPersistenceV1().read(active.mutationLedgerFile),
            fingerprint = HimEntityFingerprintIndexPersistence().read(active.fingerprintIndexFile),
            release = HimGroundTruthReleasePersistenceV1().read(active.releaseFile),
        )
    }

    private fun releaseInput(state: ActiveState, active: HimActiveGroundTruthArtifactsV1) = HimGroundTruthReleaseBuildInputV1(
        authority = state.authority,
        activeEntityIdRegistry = state.registry,
        retiredEntityIdRegistry = state.retired,
        mutationLedger = state.ledger,
        entityFingerprintIndex = state.fingerprint,
        authorityArtifact = releaseArtifact("canonical-family-authority.v1.json", active.authorityBytes, state.authority.families.size),
        activeEntityIdRegistryArtifact = releaseArtifact("him-entity-id-registry.v1.json", active.activeRegistryBytes, state.registry.entries.size),
        retiredEntityIdRegistryArtifact = releaseArtifact("retired-entity-id-registry.v1.json", active.retiredRegistryBytes, state.retired.entries.size),
        mutationLedgerArtifact = releaseArtifact("canonical-family-mutation-ledger.v1.json", active.mutationLedgerBytes, state.ledger.entries.size),
        entityFingerprintIndexArtifact = releaseArtifact("him-entity-fingerprint-index.v1.json", active.fingerprintIndexBytes, state.fingerprint.entryCount),
    )

    private fun releaseArtifact(path: String, bytes: ByteArray, count: Int) =
        HimGroundTruthReleaseArtifactReference(path, sha256(bytes), count)

    private fun materializeActiveState(sourceRoot: File, targetRoot: File) {
        val active = HimActiveGroundTruthResolutionV1().resolve(sourceRoot)
        val targetRelease = targetRoot.resolve("data/knowledge/him/canonical-family/groundtruth/${active.releaseDirectory.relativeTo(sourceRoot.resolve("data/knowledge/him/canonical-family/groundtruth"))}")
        active.releaseDirectory.walkTopDown().filter { it.isFile }.forEach { source ->
            val target = targetRelease.resolve(source.relativeTo(active.releaseDirectory).path)
            val targetParent = requireNotNull(target.parentFile)
            require(targetParent.mkdirs() || targetParent.isDirectory)
            source.copyTo(target, overwrite = true)
        }
        val pointer = targetRoot.resolve("data/knowledge/him/canonical-family/groundtruth/current.v1.json")
        val pointerParent = requireNotNull(pointer.parentFile)
        require(pointerParent.mkdirs() || pointerParent.isDirectory)
        currentPointer(sourceRoot).copyTo(pointer, overwrite = true)
        val candidate = candidateFile(sourceRoot)
        val candidateTarget = candidateFile(targetRoot)
        val candidateParent = requireNotNull(candidateTarget.parentFile)
        require(candidateParent.mkdirs() || candidateParent.isDirectory)
        candidate.copyTo(candidateTarget, overwrite = true)
    }

    private fun readDataset(root: File) = HimCandidateDatasetPersistenceV2.readDataset(candidateFile(root))

    private fun candidateFile(root: File) = root.resolve("${HimCandidateDatasetContractV2.MASTER_ROOT}/candidate-dataset.v2.json")
    private fun validationFile(root: File) = HimCandidateValidationLedgerPersistenceV1.defaultLedgerFile(root)
    private fun currentPointer(root: File) = root.resolve("data/knowledge/him/canonical-family/groundtruth/current.v1.json")
    private fun releaseDirectories(root: File) = root.resolve("data/knowledge/him/canonical-family/groundtruth/releases").listFiles().orEmpty().map { it.name }.sorted()
    private fun currentReleaseIsDifferent(root: File, expected: String) = HimActiveGroundTruthResolutionV1().resolve(root).releaseReference.value != expected
    private fun fixtureRoot() = Files.createTempDirectory("him-real-hering-promotion-v1-").toFile()
    private fun projectRoot(): File = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).canonicalFile) { it.parentFile }.first { File(it, "settings.gradle.kts").isFile }

    private fun guardSnapshot(root: File): Map<String, GuardState> {
        val paths = linkedSetOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json",
            "data/knowledge/him/candidates/master/candidate-dataset.v2.json",
            "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json",
            HimCandidateValidationLedgerPersistenceV1.LEDGER_PATH,
        )
        HimProductionIndexFileIdentityReleaseContractV1.sources.forEach { paths += it.optimizedSourcePath; paths += it.indexPath }
        return paths.associateWith { path -> fileState(root.resolve(path), hash = !path.startsWith("data/sources/")) }
    }

    private fun assertNonValidationGuardsEqual(
        before: Map<String, GuardState>,
        after: Map<String, GuardState>,
    ) {
        before.filterKeys { it != HimCandidateValidationLedgerPersistenceV1.LEDGER_PATH }.forEach { (path, state) ->
            assertEquals(state, after[path])
        }
    }

    private fun fileState(file: File, hash: Boolean) = GuardState(
        exists = file.isFile,
        sha256 = if (file.isFile && hash) sha256(file.readBytes()).value else "",
        bytes = if (file.isFile) file.length() else 0L,
        modified = if (file.isFile) file.lastModified() else 0L,
    )
    private fun releaseArtifactSnapshot(directory: File): Map<String, String> = directory.listFiles().orEmpty().filter { it.isFile }.associate { it.name to sha256(it.readBytes()).value }
    private fun sha256(bytes: ByteArray) = HimSha256(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) })

    private fun HimCanonicalFamilyAuthority.authorityAliasCount() = families.sumOf { it.aliases.size + it.identities.sumOf { identity -> identity.aliases.size } }

    private data class Prepared(
        val root: File,
        val dataset: HimCandidateDataset,
        val candidate: de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateHypothesis,
        val active: HimActiveGroundTruthArtifactsV1,
        val state: ActiveState,
        val validation: HimCandidateValidationRecord,
        val validationReference: de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecisionReference,
        val eligibility: HimCandidatePromotionEligibilityResultV1,
        val promotion: HimCandidatePromotionProposalV1,
        val entityId: HimEntityId,
        val validationBefore: de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationLedgerV1,
    )

    private data class ActiveState(
        val authority: HimCanonicalFamilyAuthority,
        val registry: de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry,
        val retired: HimRetiredEntityIdRegistry,
        val ledger: HimCanonicalFamilyMutationLedger,
        val fingerprint: de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndex,
        val release: de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthRelease,
    )

    private data class DryRunResult(
        val execution: HimDeterministicPromotionExecutionResultV1,
        val active: HimActiveGroundTruthArtifactsV1,
        val authorityBytes: ByteArray,
        val registryBytes: ByteArray,
        val retiredBytes: ByteArray,
        val ledgerBytes: ByteArray,
        val fingerprintBytes: ByteArray,
        val releaseBytes: ByteArray,
    )

    private data class ExistingPromotion(val entityId: HimEntityId, val variantName: String)
    private data class GuardState(val exists: Boolean, val sha256: String, val bytes: Long, val modified: Long)

    companion object {
        private const val CANDIDATE_TERM = "Hering eingelegt"
        private const val CANDIDATE_REFERENCE = "candidate:v1:8df2826510366d3cacc78d81f1ad9b3d81d50ef61d7ff3b29195eaf1ac1fd2ef"
        private const val GENERATION_RUN = "run:v2:c1ab93c23dde76444a3585afd80b592038ecf9c14e39442846ddbaf7671a0823"
        private const val EXPECTED_RELEASE = "release:v1:e877ccc673527e569520a9ee0932a79f6460ef3fabdb364a38200837e557c5f7"
        private const val EXPECTED_RELEASE_SHA = "804e646aad34659327bac0cd0d6224b3d8b4c5a9b198e052e91872a6b16224d6"
        private const val EXPECTED_PROMOTION_REFERENCE = "promotion:v1:3578a9afcbd7f5a3c2d774301ebf51c07a3835b9c0a81ed657ea084afa6b07c1"
        private const val EXPECTED_ENTITY_ID = "bvoJLp"
        private const val HERRING_NAME = "Hering"
        private val HERRING_ID = HimEntityId("OzlByp")
        private val EXPECTED_EVIDENCE = listOf(
            "off:product:row:4530988:code:5701157480343",
            "agribalyse:row:2171:agb:26010",
            "ciqual:food:26010",
        )
        private const val RATIONALE = "Eingelegt beziehungsweise mariniert is a preservation state of herring; the underlying food remains Hering. The broad term does not specifically denote Bismarckhering or Rollmops."
        private const val PRODUCTION_OPT_IN = "HIM_REAL_GROUND_TRUTH_PROMOTION"
        private val GSON = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    }
}
