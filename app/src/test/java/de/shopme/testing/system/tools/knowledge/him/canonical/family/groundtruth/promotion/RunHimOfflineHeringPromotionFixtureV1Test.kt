package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.promotion

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndex
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndexPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistryEntry
import de.shopme.tools.knowledge.him.canonical.family.HimEntitySourceReferenceType
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCanonicalFamilyMutationLedger
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCanonicalFamilyMutationLedgerPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthEntityFingerprintIndexBuilderV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthEntityFingerprintIndexValidatorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseArtifactReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseBuildInputV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseBuilderV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleasePersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseValidatorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimRetiredEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimTransactionalGroundTruthPublicationInputV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimTransactionalGroundTruthPublicationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimValidationReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDataset
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetContractV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetPersistenceV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateGenerationInputRun
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateGenerationRun
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateGenerationRunState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateFinalInferenceOutcome
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateHypothesis
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInferenceProvenance
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInputCompletionState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInputProvenance
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateConfidenceDiagnostics
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateEvidenceProvenance
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateOccurrence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateRetrievalRoundProvenance
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateRetrievalStepProvenance
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimRetrievalTerminalState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceOrigin
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInformationGainJudgment
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceSchema
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticRetrievalDirective
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticSourceQueries
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource as CandidateSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionProposalFactoryV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionTargetV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimDeterministicPromotionExecutionResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimDeterministicPromotionExecutorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityGateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecision
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationLedgerPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationReason
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationRecord
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class RunHimOfflineHeringPromotionFixtureV1Test {

    @Test
    fun `realistic offline Hering candidate reaches active Variant release`() {
        val root = fixtureRoot()
        try {
            val prepared = prepare(root)
            val execution = execute(prepared)
            assertPromotionResult(prepared, execution)
            assertPublishedState(prepared, execution)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `identical offline fixtures are byte deterministic and CREATE_CANONICAL is rejected`() {
        val firstRoot = fixtureRoot()
        val secondRoot = fixtureRoot()
        try {
            val first = execute(prepare(firstRoot))
            val second = execute(prepare(secondRoot))

            assertEquals(first.validationReference, second.validationReference)
            assertEquals(first.promotion.promotionReference, second.promotion.promotionReference)
            assertEquals(first.execution.mutationReference, second.execution.mutationReference)
            assertEquals(first.execution.newReleaseReference, second.execution.newReleaseReference)
            assertArrayEquals(first.active.authorityBytes, second.active.authorityBytes)
            assertArrayEquals(first.active.activeRegistryBytes, second.active.activeRegistryBytes)
            assertArrayEquals(first.active.retiredRegistryBytes, second.active.retiredRegistryBytes)
            assertArrayEquals(first.active.mutationLedgerBytes, second.active.mutationLedgerBytes)
            assertArrayEquals(first.active.fingerprintIndexBytes, second.active.fingerprintIndexBytes)
            assertArrayEquals(first.active.releaseBytes, second.active.releaseBytes)

            val negativeRoot = fixtureRoot()
            try {
                val prepared = prepare(negativeRoot)
                val currentBefore = currentPointer(negativeRoot)
                val invalidPromotion = HimCandidatePromotionProposalFactoryV1.create(
                    candidateReference = prepared.candidate.candidateReference,
                    candidateRelation = HimCandidateRelation.CreateNewCanonical,
                    candidateTerm = CANDIDATE_TERM,
                    candidateDatasetDigest = prepared.datasetDigest,
                    authorityDigestBefore = prepared.authorityDigest,
                    entityIdRegistryDigestBefore = prepared.registryDigest,
                    eligibility = prepared.eligibility,
                )

                assertFails {
                    HimDeterministicPromotionExecutorV1().execute(
                        projectRoot = negativeRoot,
                        promotion = invalidPromotion,
                        newEntityId = TEST_ENTITY_ID,
                        eligibility = prepared.eligibility,
                    )
                }
                assertArrayEquals(currentBefore, currentPointer(negativeRoot))
                assertEquals(
                    prepared.releaseN,
                    HimActiveGroundTruthResolutionV1().resolve(negativeRoot).releaseReference,
                )
            } finally {
                negativeRoot.deleteRecursively()
            }
        } finally {
            firstRoot.deleteRecursively()
            secondRoot.deleteRecursively()
        }
    }

    private fun prepare(root: File): Prepared {
        val releaseN = createInitialRelease(root)
        val active = HimActiveGroundTruthResolutionV1().resolve(root)
        assertEquals(releaseN, active.releaseReference)

        val candidateFixture = candidateFixture()
        val datasetDigest = HimCandidateIdentityV1.datasetDigest(candidateFixture.dataset)
        val validationReference = HimCandidateValidationIdentityV1.validation(
            candidateReference = candidateFixture.hypothesis.candidateReference,
            candidateDatasetDigest = datasetDigest,
            decision = HimCandidateValidationDecision.APPROVE,
            reason = HimCandidateValidationReason.SEMANTICALLY_CORRECT,
            supersededByCandidateReference = null,
        )
        val validation = HimCandidateValidationRecord(
            validationReference = validationReference,
            candidateReference = candidateFixture.hypothesis.candidateReference,
            candidateDatasetDigest = datasetDigest,
            decision = HimCandidateValidationDecision.APPROVE,
            reason = HimCandidateValidationReason.SEMANTICALLY_CORRECT,
            rationale = REAL_RATIONALE,
        )
        val validationLedger = HimCandidateValidationLedgerPersistenceV1.addValidation(
            ledger = de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationLedgerV1(),
            validation = validation,
        )
        val validationFile = HimCandidateValidationLedgerPersistenceV1.defaultLedgerFile(root)
        HimCandidateValidationLedgerPersistenceV1.write(validationFile, validationLedger)
        val reloadedValidationLedger = HimCandidateValidationLedgerPersistenceV1.read(validationFile)
        val eligibility = HimCandidatePromotionEligibilityGateV1.requireEligible(
            candidateReference = candidateFixture.hypothesis.candidateReference,
            candidateDataset = candidateFixture.dataset,
            currentCandidateDatasetDigest = datasetDigest,
            validationLedger = reloadedValidationLedger,
        )

        return Prepared(
            root = root,
            releaseN = releaseN,
            candidate = candidateFixture.hypothesis,
            candidateEvidenceReferences = candidateFixture.evidenceReferences,
            datasetDigest = datasetDigest,
            validationReference = validationReference,
            eligibility = eligibility,
            authorityDigest = digest(active.authorityBytes),
            registryDigest = digest(active.activeRegistryBytes),
        )
    }

    private fun execute(prepared: Prepared): Executed {
        val promotion = HimCandidatePromotionProposalFactoryV1.create(
            candidateReference = prepared.candidate.candidateReference,
            candidateRelation = prepared.candidate.relation,
            candidateTerm = prepared.candidate.candidateTerm,
            candidateDatasetDigest = prepared.datasetDigest,
            authorityDigestBefore = prepared.authorityDigest,
            entityIdRegistryDigestBefore = prepared.registryDigest,
            eligibility = prepared.eligibility,
        )
        val execution = HimDeterministicPromotionExecutorV1().execute(
            projectRoot = prepared.root,
            promotion = promotion,
            newEntityId = TEST_ENTITY_ID,
            eligibility = prepared.eligibility,
        )
        return Executed(
            validationReference = prepared.validationReference,
            promotion = promotion,
            execution = execution,
            active = HimActiveGroundTruthResolutionV1().resolve(prepared.root),
        )
    }

    private fun assertPromotionResult(prepared: Prepared, executed: Executed) {
        assertEquals(prepared.releaseN, executed.execution.previousReleaseReference)
        assertNotEquals(prepared.releaseN, executed.execution.newReleaseReference)
        assertEquals(CANDIDATE_TERM, prepared.candidate.candidateTerm)
        assertEquals(HimCandidateConfidence.HIGH, prepared.candidate.confidence)
        assertEquals(HimCandidatePromotionTargetV1.AddVariant::class, prepared.candidate.relation.relationTargetClass())
        assertEquals(
            HimFamilyEntityReference.Canonical(HERRING_ID),
            (prepared.candidate.relation as HimCandidateRelation.Variant).scope,
        )
        assertEquals(
            listOf(
                "off:product:row:4530988:code:5701157480343",
                "agribalyse:row:2171:agb:26010",
                "ciqual:food:26010",
            ),
            prepared.candidateEvidenceReferences,
        )
        assertEquals(HimEntityType.VARIANT, executed.execution.targetType)
        assertEquals(TEST_ENTITY_ID, executed.execution.newEntityId)
        assertTrue(executed.execution.createdNewRelease)
        assertTrue(executed.execution.activeReleaseVerified)
        assertEquals(executed.execution.newReleaseReference, executed.active.releaseReference)
        assertEquals(
            HimCandidatePromotionType.ADD_VARIANT,
            executed.execution.promotionType,
        )
    }

    private fun assertPublishedState(prepared: Prepared, executed: Executed) {
        val persistence = HimCanonicalFamilyPersistence()
        val authority = persistence.readAuthority(executed.active.authorityFile)
        assertEquals(1, authority.families.size)
        val herring = authority.families.single()
        assertEquals(HERRING_ID, herring.canonicalId)
        assertEquals("Hering", herring.canonicalName)
        assertTrue(herring.identities.isEmpty())
        assertTrue(herring.aliases.isEmpty())
        assertEquals(1, herring.variants.size)
        assertEquals(TEST_ENTITY_ID, herring.variants.single().variantId)
        assertEquals(CANDIDATE_TERM, herring.variants.single().variantName)

        val registry = persistence.readRegistry(executed.active.activeRegistryFile)
        assertEquals(2, registry.entries.size)
        assertEquals(1, registry.entries.count { it.entityId == TEST_ENTITY_ID })
        assertEquals(HimEntityType.VARIANT, registry.entries.single { it.entityId == TEST_ENTITY_ID }.entityType)
        assertEquals(HERRING_ID, registry.entries.first().entityId)

        val ledger = HimCanonicalFamilyMutationLedgerPersistenceV1().read(executed.active.mutationLedgerFile)
        assertEquals(1, ledger.entries.size)
        val mutation = ledger.entries.single()
        assertEquals(executed.execution.mutationReference, mutation.mutationReference)
        assertEquals(HimValidationReference(prepared.validationReference.value), mutation.validationReference)
        assertEquals(HimEntityType.VARIANT, mutation.entityType)
        assertEquals(HimFamilyEntityReference.Canonical(HERRING_ID), mutation.parent)

        val fingerprint = HimEntityFingerprintIndexPersistence().read(executed.active.fingerprintIndexFile)
        HimGroundTruthEntityFingerprintIndexValidatorV1().validate(
            registry = registry,
            authority = authority,
            index = fingerprint,
            registrySha256 = digest(executed.active.activeRegistryBytes).value,
            authoritySha256 = digest(executed.active.authorityBytes).value,
        )
        assertTrue(fingerprint.entries.any { TEST_ENTITY_ID in it.variantIds })

        val retired = gson.fromJson(
            executed.active.retiredRegistryBytes.toString(Charsets.UTF_8),
            HimRetiredEntityIdRegistry::class.java,
        )
        val release = HimGroundTruthReleasePersistenceV1().read(executed.active.releaseFile)
        HimGroundTruthReleaseValidatorV1().validate(
            release = release,
            input = releaseInput(
                authority = authority,
                registry = registry,
                retired = retired,
                ledger = ledger,
                fingerprint = fingerprint,
                active = executed.active,
            ),
        )
        val reconstructed = de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1Factory.compute(
            HimTransactionalGroundTruthPublicationInputV1(
                authorityBytes = executed.active.authorityBytes,
                activeRegistryBytes = executed.active.activeRegistryBytes,
                retiredRegistryBytes = executed.active.retiredRegistryBytes,
                mutationLedgerBytes = executed.active.mutationLedgerBytes,
                fingerprintIndexBytes = executed.active.fingerprintIndexBytes,
                releaseBytes = executed.active.releaseBytes,
            ),
        )
        assertEquals(executed.active.releaseReference, reconstructed)
        assertFalse(prepared.root.resolve("data/knowledge/catalog").exists())
        assertFalse(prepared.root.resolve("data/knowledge/runtime").exists())
    }

    private fun candidateFixture(): CandidateFixture {
        val relation = HimCandidateRelation.Variant(
            HimFamilyEntityReference.Canonical(HERRING_ID),
        )
        val candidateReference = HimCandidateIdentityV1.candidate(
            normalizedCandidateTerm = CANDIDATE_TERM.lowercase(),
            relation = relation,
        )
        val evidence = listOf(
            evidence(CandidateSource.OPEN_FOOD_FACTS, "off:product:row:4530988:code:5701157480343"),
            evidence(CandidateSource.AGRIBALYSE, "agribalyse:row:2171:agb:26010"),
            evidence(CandidateSource.CIQUAL, "ciqual:food:26010"),
        )
        val hypothesis = HimCandidateHypothesis(
            candidateReference = candidateReference,
            candidateTerm = CANDIDATE_TERM,
            normalizedCandidateTerm = CANDIDATE_TERM.lowercase(),
            relation = relation,
            confidence = HimCandidateConfidence.HIGH,
            evidenceOrigin = HimSemanticEvidenceOrigin.MIXED,
            shortRationale = REAL_RATIONALE,
        )
        val provenance = evidence.mapIndexed { index, reference ->
            HimCandidateEvidenceProvenance(
                reference = reference,
                recordKind = "real-pilot-evidence",
                retrievalRank = index + 1,
                includedInProviderContext = true,
                omittedDueToContextBudget = false,
                himAssignedRelation = HimSemanticEvidenceRelation.VARIANT,
            )
        }
        val sourceQueries = listOf(
            HimSemanticSourceQueries(CandidateSource.OPEN_FOOD_FACTS, listOf(CANDIDATE_TERM)),
            HimSemanticSourceQueries(CandidateSource.AGRIBALYSE, listOf(CANDIDATE_TERM)),
            HimSemanticSourceQueries(CandidateSource.CIQUAL, listOf(CANDIDATE_TERM)),
        )
        val directive = HimSemanticRetrievalDirective(
            sourceQueries = sourceQueries,
            informationGainJudgment = HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP,
        )
        val steps = sourceQueries.map { sourceQuery ->
            HimCandidateRetrievalStepProvenance(
                source = sourceQuery.source,
                semanticQuery = CANDIDATE_TERM,
                evidenceReferences = provenance.filter { it.reference.source == sourceQuery.source.name }.map { it.reference },
            )
        }
        val round = HimCandidateRetrievalRoundProvenance(
            round = HimRetrievalRound(1),
            directive = directive,
            steps = steps,
            includedEvidenceReferences = evidence,
            omittedDueToBudgetEvidenceReferences = emptyList(),
        )
        val inference = inference()
        val inputSet = HimSha256("2".repeat(64))
        val runReference = HimCandidateIdentityV1.run("offline-hering-fixture", inputSet, inference)
        val input = HimCandidateInputProvenance(CANDIDATE_TERM, CANDIDATE_TERM.lowercase())
        val inputRunReference = HimCandidateIdentityV1.inputRun(runReference, input)
        val inputRun = HimCandidateGenerationInputRun(
            inputRunReference = inputRunReference,
            input = input,
            canonicalContext = emptyList(),
            retrievalHistory = listOf(round),
            finalInformationGainJudgment = HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN,
            terminalState = HimRetrievalTerminalState.SUFFICIENT_EVIDENCE,
            completionState = HimCandidateInputCompletionState.COMPLETED,
            finalOutcome = HimCandidateFinalInferenceOutcome.SUCCESS_WITH_PERSISTED_CANDIDATE,
            confidenceDiagnostics = HimCandidateConfidenceDiagnostics(1, 0, 0, 0),
            knownRelations = emptyList(),
            authorityConflicts = emptyList(),
            persistedCandidateReferences = listOf(candidateReference),
            inference = inference,
            technicalFailure = null,
        )
        val occurrenceReference = HimCandidateIdentityV1.occurrence(
            candidate = candidateReference,
            run = runReference,
            inputRun = inputRunReference,
            evidence = provenance,
        )
        val occurrence = HimCandidateOccurrence(
            occurrenceReference = occurrenceReference,
            inputRunReference = inputRunReference,
            candidate = hypothesis,
            evidence = provenance,
        )
        val run = HimCandidateGenerationRun(
            runReference = runReference,
            generationMissionReference = "offline-hering-fixture",
            runInputSetIdentity = inputSet,
            state = HimCandidateGenerationRunState.COMPLETE,
            inputRuns = listOf(inputRun),
            occurrences = listOf(occurrence),
        )
        return CandidateFixture(
            hypothesis = hypothesis,
            dataset = HimCandidateDatasetPersistenceV2.addRun(HimCandidateDataset(), run),
            evidenceReferences = evidence.map { it.sourceRecordIdentity },
        )
    }

    private fun inference() = HimCandidateInferenceProvenance(
        provider = "OPENAI",
        model = "gpt-5.6-sol",
        providerConfigurationFingerprint = HimSha256("3".repeat(64)),
        inferenceSchema = HimSemanticInferenceSchema.OUTPUT_VERSION,
        instructionPolicy = HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
        contextBudgetPolicy = HimCandidateDatasetContractV2.CONTEXT_BUDGET_POLICY,
        retrievalFoundationRelease = "F3D_2_RETRIEVAL_FOUNDATION_V1",
        retrievalFoundationReleaseSha256 = HimSha256("4".repeat(64)),
        retrievalFoundationDigest = HimSha256("5".repeat(64)),
        technicalAttemptCount = 1,
        usage = null,
    )

    private fun createInitialRelease(root: File): HimGroundTruthReleaseIdentityV1 {
        val authority = HimCanonicalFamilyAuthority(
            schemaVersion = "1",
            sourceCatalog = HimCanonicalFamilySourceCatalog("catalog.json", "a".repeat(64), 1),
            families = listOf(
                HimCanonicalFamily(
                    canonicalId = HERRING_ID,
                    canonicalName = "Hering",
                    normalizedName = "hering",
                    taxonomyPaths = emptyList(),
                    lifecycleStatus = HimLifecycleStatus.ACTIVE,
                    identities = emptyList(),
                    variants = emptyList(),
                    aliases = emptyList(),
                ),
            ),
        )
        val registry = HimEntityIdRegistry(
            listOf(
                HimEntityIdRegistryEntry(
                    entityId = HERRING_ID,
                    entityType = HimEntityType.CANONICAL,
                    sourceReferenceType = HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED,
                    sourceReference = "hering",
                ),
            ),
        )
        val retired = HimRetiredEntityIdRegistry("HIM_RETIRED_ENTITY_ID_REGISTRY_V1", emptyList())
        val familyPersistence = HimCanonicalFamilyPersistence()
        val ledgerPersistence = HimCanonicalFamilyMutationLedgerPersistenceV1()
        val fingerprintPersistence = HimEntityFingerprintIndexPersistence()
        val authorityBytes = familyPersistence.serialize(authority)
        val registryBytes = familyPersistence.serialize(registry)
        val retiredBytes = serializeRetired(retired)
        val ledger = ledgerPersistence.emptyLedger()
        val ledgerBytes = ledgerPersistence.serialize(ledger)
        val fingerprint = HimGroundTruthEntityFingerprintIndexBuilderV1().build(
            registry = registry,
            authority = authority,
            registrySha256 = digest(registryBytes).value,
            authoritySha256 = digest(authorityBytes).value,
        )
        val fingerprintBytes = fingerprintPersistence.serialize(fingerprint)
        val releaseInput = HimGroundTruthReleaseBuildInputV1(
            authority = authority,
            activeEntityIdRegistry = registry,
            retiredEntityIdRegistry = retired,
            mutationLedger = ledger,
            entityFingerprintIndex = fingerprint,
            authorityArtifact = artifact("canonical-family-authority.v1.json", authorityBytes, authority.families.size),
            activeEntityIdRegistryArtifact = artifact("him-entity-id-registry.v1.json", registryBytes, registry.entries.size),
            retiredEntityIdRegistryArtifact = artifact("retired-entity-id-registry.v1.json", retiredBytes, retired.entries.size),
            mutationLedgerArtifact = artifact("canonical-family-mutation-ledger.v1.json", ledgerBytes, ledger.entries.size),
            entityFingerprintIndexArtifact = artifact("him-entity-fingerprint-index.v1.json", fingerprintBytes, fingerprint.entryCount),
        )
        val releaseBytes = HimGroundTruthReleasePersistenceV1().serialize(HimGroundTruthReleaseBuilderV1().build(releaseInput))
        val publication = HimTransactionalGroundTruthPublicationV1().publish(
            projectRoot = root,
            input = HimTransactionalGroundTruthPublicationInputV1(
                authorityBytes,
                registryBytes,
                retiredBytes,
                ledgerBytes,
                fingerprintBytes,
                releaseBytes,
            ),
        ) { }
        return publication.releaseReference
    }

    private fun releaseInput(
        authority: HimCanonicalFamilyAuthority,
        registry: HimEntityIdRegistry,
        retired: HimRetiredEntityIdRegistry,
        ledger: HimCanonicalFamilyMutationLedger,
        fingerprint: HimEntityFingerprintIndex,
        active: de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthArtifactsV1,
    ) = HimGroundTruthReleaseBuildInputV1(
        authority = authority,
        activeEntityIdRegistry = registry,
        retiredEntityIdRegistry = retired,
        mutationLedger = ledger,
        entityFingerprintIndex = fingerprint,
        authorityArtifact = artifact("canonical-family-authority.v1.json", active.authorityBytes, authority.families.size),
        activeEntityIdRegistryArtifact = artifact("him-entity-id-registry.v1.json", active.activeRegistryBytes, registry.entries.size),
        retiredEntityIdRegistryArtifact = artifact("retired-entity-id-registry.v1.json", active.retiredRegistryBytes, retired.entries.size),
        mutationLedgerArtifact = artifact("canonical-family-mutation-ledger.v1.json", active.mutationLedgerBytes, ledger.entries.size),
        entityFingerprintIndexArtifact = artifact("him-entity-fingerprint-index.v1.json", active.fingerprintIndexBytes, fingerprint.entryCount),
    )

    private fun artifact(path: String, bytes: ByteArray, count: Int) =
        HimGroundTruthReleaseArtifactReference(path, digest(bytes), count)

    private fun evidence(source: CandidateSource, identity: String) =
        HimEvidenceReference(source.name, HimSha256("1".repeat(64)), identity)

    private fun serializeRetired(value: HimRetiredEntityIdRegistry) =
        (gson.toJson(value) + "\n").toByteArray(Charsets.UTF_8)

    private fun digest(bytes: ByteArray) = HimSha256(
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) },
    )

    private fun currentPointer(root: File) = root.resolve(
        "data/knowledge/him/canonical-family/groundtruth/current.v1.json",
    ).readBytes()

    private fun fixtureRoot() = Files.createTempDirectory("him-offline-hering-promotion-v1-").toFile()

    private fun assertFails(block: () -> Unit) {
        assertTrue(runCatching(block).isFailure)
    }

    private fun HimCandidateRelation.relationTargetClass() =
        when (this) {
            is HimCandidateRelation.Variant -> HimCandidatePromotionTargetV1.AddVariant::class
            is HimCandidateRelation.Identity -> HimCandidatePromotionTargetV1.AddIdentity::class
            is HimCandidateRelation.Alias -> HimCandidatePromotionTargetV1.AddAlias::class
            HimCandidateRelation.CreateNewCanonical -> HimCandidatePromotionTargetV1.CreateCanonical::class
        }

    private data class CandidateFixture(
        val hypothesis: HimCandidateHypothesis,
        val dataset: HimCandidateDataset,
        val evidenceReferences: List<String>,
    )

    private data class Prepared(
        val root: File,
        val releaseN: HimGroundTruthReleaseIdentityV1,
        val candidate: HimCandidateHypothesis,
        val candidateEvidenceReferences: List<String>,
        val datasetDigest: HimSha256,
        val validationReference: de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecisionReference,
        val eligibility: HimCandidatePromotionEligibilityResultV1,
        val authorityDigest: HimSha256,
        val registryDigest: HimSha256,
    )

    private data class Executed(
        val validationReference: de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecisionReference,
        val promotion: de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionProposalV1,
        val execution: HimDeterministicPromotionExecutionResultV1,
        val active: de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthArtifactsV1,
    )

    companion object {
        private const val CANDIDATE_TERM = "Hering eingelegt"
        private const val REAL_RATIONALE =
            "Eingelegt beziehungsweise mariniert is a preservation state of herring; " +
                    "the underlying food remains Hering. The broad term does not specifically " +
                    "denote Bismarckhering or Rollmops."
        private val HERRING_ID = HimEntityId("OzlByp")
        private val TEST_ENTITY_ID = HimEntityId("Tst001")
        private val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    }
}
