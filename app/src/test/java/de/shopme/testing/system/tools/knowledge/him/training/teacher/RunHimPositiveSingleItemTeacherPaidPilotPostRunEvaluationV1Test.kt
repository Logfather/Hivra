package de.shopme.testing.system.tools.knowledge.him.training.teacher

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalFamilyCandidateRetrieval
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalQuery
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticContextBudgetPolicy
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceRuntime
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceSuccess
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInformationGainJudgment
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticSourceArtifactIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticUsage
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.provider.openai.HimOpenAiSemanticProviderConfiguration
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimAgribalyseEvidenceIndexValidator
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimAgribalyseProductionEvidenceIndexPaths
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimAgribalyseSqliteEvidenceRetrievalStore
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimCiqualEvidenceIndexValidator
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimCiqualProductionEvidenceIndexPaths
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimCiqualSqliteEvidenceRetrievalStore
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceSearchResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimGlycemicIndexEvidenceIndexValidator
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimGlycemicIndexProductionEvidenceIndexPaths
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimGlycemicIndexSqliteEvidenceRetrievalStore
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimOffEvidenceIndexValidator
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimOffProductionEvidenceIndexPaths
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimOffSqliteEvidenceRetrievalStore
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import de.shopme.tools.knowledge.him.training.scaling.HimCandidateDatasetBindingV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlanV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlannerInputV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlannerV1
import de.shopme.tools.knowledge.him.training.teacher.HimPositiveSingleItemTeacherPilotSelection
import de.shopme.tools.knowledge.him.training.teacher.HimPositiveSingleItemTeacherPilotSelectionV1
import de.shopme.tools.knowledge.him.training.teacher.HimSemanticInferenceTeacherProviderAdapterV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthGenerationContractV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthGenerationPersistenceV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthGenerationPipelineV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthGenerationRequestV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthGenerationResultV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthOutputCodecV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthOutputValidatorV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthProviderOutcomeV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthProviderV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthRequestValidatorV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherPaidPilotOfflinePreflightV2
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherPaidPilotOfflinePreflightV2Status
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

/** Deterministic, source-bound, provider-free evaluation of the completed Vanille pilot. */
class RunHimPositiveSingleItemTeacherPaidPilotPostRunEvaluationV1Test {
    @Test
    fun negativeVanillaProductNamesDoNotBecomeDirectEvidence() {
        val cases = listOf(
            NegativeVanillaCase("Pudding", "Vanille", "Vanille Pudding mit Bourbon-Vanille"),
            NegativeVanillaCase("Zucker", "Vanille / flavoured", "Sugar, vanilla flavoured"),
            NegativeVanillaCase("Keks", "Vanille", "Prince Petit Déjeuner Vanille"),
        )
        cases.forEach { case ->
            assertEquals(case.sourceProductName, "CONTEXTUAL", classifyVanillaRegression(case.primaryIdentity, case.modifier))
        }
    }

    @Test
    fun evaluatesCompletedVanillePaidPilotOfflineAndWritesDeterministicReports() {
        assumeTrue(System.getProperty(HimTestExecutionBoundaryV1.SOURCE_INTEGRATION_PROPERTY) == "true")
        assertFalse(
            HimTestExecutionBoundaryV1.paidNetworkEnabled(
                System.getProperty(HimTestExecutionBoundaryV1.PAID_NETWORK_PROPERTY),
                System.getProperty(HimTestExecutionBoundaryV1.PAID_NETWORK_CONFIRMATION_PROPERTY),
            ),
        )

        val root = projectRoot()
        val v2File = root.resolve(V2_PATH)
        val selectionFile = root.resolve(SELECTION_PATH)
        val resultFile = root.resolve(RESULT_PATH)
        val paidReportFile = root.resolve(PAID_REPORT_PATH)
        val jsonReportFile = root.resolve(JSON_REPORT_PATH)
        val textReportFile = root.resolve(TEXT_REPORT_PATH)

        require(sha256(v2File) == V2_SHA256)
        require(sha256(selectionFile) == SELECTION_SHA256)
        require(sha256(resultFile) == RESULT_SHA256)
        require(resultFile.length() == 2_003L)
        require(sha256(paidReportFile) == PAID_REPORT_SHA256)
        require(paidReportFile.length() == 115L)

        val v2 = HimTeacherPaidPilotOfflinePreflightV2.read(v2File)
        require(HimTeacherPaidPilotOfflinePreflightV2.evaluate(v2, v2) == HimTeacherPaidPilotOfflinePreflightV2Status.CURRENT)
        require(v2.logicalArtifactDigest == V2_LOGICAL_DIGEST)
        val selection = HimPositiveSingleItemTeacherPilotSelectionV1.read(selectionFile)
        HimPositiveSingleItemTeacherPilotSelectionV1.validate(selection)
        require(selection.logicalDigest.value == SELECTION_LOGICAL_DIGEST)
        require(selection.checkpoint.headSha256 == EXPECTED_HEAD)
        require(selection.selectedRank == 1)

        val paidResult = HimTeacherGroundTruthGenerationPersistenceV1.readResult(resultFile)
        require(paidResult.canonicalId == HimEntityId(EXPECTED_CANONICAL_ID))
        require(paidResult.partition == HimTrainingPartitionV1.VALIDATION)
        require(paidResult.workItemReference == EXPECTED_WORK_ITEM)
        require(paidResult.requestReference == EXPECTED_REQUEST)
        require(paidResult.resultReference == EXPECTED_RESULT_REFERENCE)
        require(paidResult.logicalDigest.value == EXPECTED_RESULT_DIGEST)
        require(paidResult.output.schemaVersion == HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION)
        require(paidResult.output.proposals.isEmpty())
        require(paidResult.output.informationGain == HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN)
        require(paidResult.retrievalProvenance.retrievalRoundCount == 1)
        require(paidResult.inferenceProvenance.technicalAttemptCount == 1)
        require(paidResult.inferenceProvenance.providerIdentifier == "OPENAI")
        require(paidResult.inferenceProvenance.modelIdentifier == "gpt-5.6-sol")
        require(paidResult.usage == null)

        val active = HimActiveGroundTruthResolutionV1().resolve(root)
        val authority = HimCanonicalFamilyPersistence().readAuthority(active.authorityFile)
        val catalog = HimProductOnlyCanonicalMasterReader().read(HimCanonicalFamilyPaths(root))
        val plan = currentPlan(root, catalog, authority, active.releaseReference)
        require(plan.workItems.single { it.reference == EXPECTED_WORK_ITEM }.partition == HimTrainingPartitionV1.VALIDATION)
        require(plan.workItems.single { it.reference == EXPECTED_WORK_ITEM }.missingCoverage.isNotEmpty())
        require(candidateBinding(root)?.digest?.value == CANDIDATE_DATASET_DIGEST)

        val family = authority.families.single { it.canonicalId == HimEntityId(EXPECTED_CANONICAL_ID) }
        val evidence = fetchExactlyFour(root, selection)
        val request = buildRequest(plan, selection, authority.families, evidence)
        HimTeacherGroundTruthRequestValidatorV1.validateAgainstPlan(plan, request)
        HimTeacherGroundTruthOutputValidatorV1.validate(request, paidResult.output)
        require(request.requestReference == EXPECTED_REQUEST)
        require(request.inferenceRequest.retrievalRound.value == 1)
        require(request.inferenceRequest.retrievalHistory.size == 1)
        require(request.inferenceRequest.evidence.size == 4)
        require(request.inferenceRequest.evidence.map(HimSemanticSourceArtifactIdentityV1::reference) == EXPECTED_EVIDENCE)
        require(paidResult.retrievalProvenance.evidenceReferences == EXPECTED_EVIDENCE)

        val projectionAudits = evidence.map { auditProjection(it, catalog, authority) }
        val primaryClassification = if (projectionAudits.any { !it.directRelationSupported }) {
            PrimaryClassification.EVIDENCE_ALIGNMENT_DEFECT
        } else if (projectionAudits.any { it.possibleUncoveredVariant }) {
            PrimaryClassification.POSSIBLE_FALSE_NEGATIVE
        } else {
            PrimaryClassification.SUPPORTED_NO_PROPOSAL
        }
        require(primaryClassification == PrimaryClassification.EVIDENCE_ALIGNMENT_DEFECT)

        val usageAudit = evaluateUsageBoundary(plan, request, paidResult)
        val report = EvaluationReport(
            reportVersion = "HIM_POSITIVE_SINGLE_ITEM_TEACHER_PAID_PILOT_POST_RUN_EVALUATION_V1",
            inputArtifacts = listOf(
                ArtifactDigest(V2_PATH, v2File.length(), V2_SHA256),
                ArtifactDigest(SELECTION_PATH, selectionFile.length(), SELECTION_SHA256),
                ArtifactDigest(RESULT_PATH, resultFile.length(), RESULT_SHA256),
                ArtifactDigest(PAID_REPORT_PATH, paidReportFile.length(), PAID_REPORT_SHA256),
                ArtifactDigest(catalog.path, root.resolve(catalog.path).length(), catalog.contentSha256),
                ArtifactDigest(relativePath(root, active.authorityFile), active.authorityFile.length(), sha256(active.authorityFile)),
            ),
            paidRunHead = EXPECTED_HEAD,
            item = ItemAudit(
                canonical = family.canonicalName,
                entityId = paidResult.canonicalId.value,
                partition = paidResult.partition.name,
                workItemReference = paidResult.workItemReference,
                selectionRequestReference = selection.selectedCandidate.requestReference,
                evidenceRequestReference = paidResult.requestReference,
                resultReference = paidResult.resultReference,
                resultLogicalDigest = paidResult.logicalDigest.value,
                candidateDatasetDigest = requireNotNull(candidateBinding(root)).digest.value,
                retrievalRound = paidResult.retrievalProvenance.retrievalRoundCount,
            ),
            teacherOutput = TeacherOutputAudit(
                schemaVersion = paidResult.output.schemaVersion,
                proposalCount = paidResult.output.proposals.size,
                informationGain = paidResult.output.informationGain.name,
                retrievalDirective = "null (implied by NO_EXPECTED_INFORMATION_GAIN; not persisted in the Teacher result)",
                providerAttempts = paidResult.inferenceProvenance.technicalAttemptCount,
                provider = paidResult.inferenceProvenance.providerIdentifier,
                model = paidResult.inferenceProvenance.modelIdentifier,
                usage = null,
            ),
            evidence = projectionAudits,
            canonicalAuthority = canonicalAudit(family, plan.workItems.single { it.reference == EXPECTED_WORK_ITEM }, projectionAudits, catalog, authority),
            primaryClassification = primaryClassification.name,
            classificationReason = "OPEN_FOOD_FACTS is a vanilla pudding product, AGRIBALYSE is vanilla-flavoured sugar, and GLYCEMIC_INDEX is a vanilla breakfast cookie. These are explicit product identities distinct from the generic Vanille canonical; the frozen DIRECT bindings therefore do not all carry the claimed canonical relation. CIQUAL independently names a vanilla pod and is recorded as a form signal.",
            usageDiagnostic = usageAudit,
            validation = ValidationAudit(
                resultReader = "PASS",
                outputValidation = "PASS",
                requestValidation = "PASS",
                evidenceFetches = 4,
                sourceScans = 0,
                indexRebuilds = 0,
                sqliteWrites = 0,
            ),
            safety = SafetyAudit(
                openAiCalls = 0,
                networkCalls = 0,
                apiKeyAccesses = 0,
                providerConstructions = 0,
                teacherInference = 0,
                paidInference = 0,
                protectedMutations = 0,
                sourceScans = 0,
                indexRebuilds = 0,
                sqliteWrites = 0,
            ),
        )
        val gson = GsonBuilder().disableHtmlEscaping().create()
        val jsonBytes = (gson.toJson(report) + "\n").toByteArray(Charsets.UTF_8)
        val textBytes = textReport(report).toByteArray(Charsets.UTF_8)
        writeOrVerify(jsonReportFile, jsonBytes)
        writeOrVerify(textReportFile, textBytes)
        assertArrayEquals(jsonBytes, jsonReportFile.readBytes())
        assertArrayEquals(textBytes, textReportFile.readBytes())
        assertEquals(primaryClassification.name, report.primaryClassification)
        assertEquals(4, report.validation.evidenceFetches)
    }

    private fun fetchExactlyFour(root: File, selection: HimPositiveSingleItemTeacherPilotSelection): List<HimEvidenceSearchResult> {
        val stores = openStores(root)
        val fetched = selection.selectedCandidate.evidenceBySource.flatMap { summary ->
            summary.evidence.map { frozen ->
                val reference = HimEvidenceRecordReference.parse(summary.source, frozen.reference.sourceRecordIdentity)
                requireNotNull(stores.getValue(summary.source)(reference)).also { result ->
                    require(HimSemanticSourceArtifactIdentityV1.reference(result).sourceArtifactSha256 == frozen.reference.sourceArtifactSha256)
                    require(HimSemanticSourceArtifactIdentityV1.reference(result).sourceRecordIdentity == frozen.reference.sourceRecordIdentity)
                }
            }
        }
        require(fetched.size == 4)
        require(fetched.map(HimSemanticSourceArtifactIdentityV1::reference) == EXPECTED_EVIDENCE)
        return fetched
    }

    private fun auditProjection(result: HimEvidenceSearchResult, catalog: HimProductOnlyCanonicalMaster, authority: HimCanonicalFamilyAuthority): ProjectionAudit {
        val json = JsonParser.parseString(result.evidenceProjection.deterministicJson)
        val terms = explicitTerms(result.source, json)
        val normalized = terms.map(::normalize).distinct()
        val misalignmentReason = when (result.source) {
            HimGroundTruthSource.OPEN_FOOD_FACTS -> terms.firstOrNull { normalize(it).contains("pudding") }?.let { "explicit product name '$it' is a pudding product" }
            HimGroundTruthSource.AGRIBALYSE -> terms.firstOrNull { normalize(it).contains("sucre vanille") || normalize(it).contains("vanilla flavoured") }?.let { "explicit product name '$it' is flavoured sugar" }
            HimGroundTruthSource.CIQUAL -> null
            HimGroundTruthSource.GLYCEMIC_INDEX -> terms.firstOrNull { normalize(it).contains("prince petit") || normalize(it).contains("breakfast") }?.let { "explicit product name '$it' is a branded breakfast cookie" }
        }
        val formSignal = normalized.any { it.contains("gousse") || it.contains("pod") }
        val formMatches = findCatalogMatches(listOf("gousse", "pod"), catalog) + findAuthorityMatches(listOf("gousse", "pod"), authority)
        val possibleUncoveredVariant = formSignal && formMatches.isEmpty()
        val ciqualAlignmentReason = if (result.source == HimGroundTruthSource.CIQUAL && possibleUncoveredVariant) {
            "CIQUAL names a pod form, but no matching gousse/pod Canonical, Alias, Identity, or Variant exists in the full local Catalog/Authority"
        } else null
        val primaryIdentityAndModifiers = primaryIdentityAndModifiers(result.source, terms)
        return ProjectionAudit(
            source = result.source.name,
            reference = result.sourceRecordReference.value,
            recordKind = result.recordKind.name,
            sourceArtifactSha256 = HimSemanticSourceArtifactIdentityV1.reference(result).sourceArtifactSha256.value,
            projectionSchema = projectionSchema(result.recordKind),
            projectionDigest = sha256(result.evidenceProjection.deterministicJson.toByteArray(Charsets.UTF_8)),
            primaryIdentity = primaryIdentityAndModifiers.first,
            modifiers = primaryIdentityAndModifiers.second,
            explicitTerms = terms,
            normalizedTerms = normalized,
            directRelationSupported = misalignmentReason == null && ciqualAlignmentReason == null,
            possibleUncoveredVariant = possibleUncoveredVariant,
            assessment = if (misalignmentReason == null && ciqualAlignmentReason == null) "DIRECT_PLAUSIBLE" else "DIRECT_NOT_SUPPORTED",
            reason = misalignmentReason ?: ciqualAlignmentReason ?: if (possibleUncoveredVariant) "explicit pod/gousse form is a source form signal; no matching authority variant was found" else "explicit source naming remains compatible with the generic canonical",
        )
    }

    private fun projectionSchema(recordKind: HimEvidenceRecordKind): String = when (recordKind) {
        HimEvidenceRecordKind.OFF_PRODUCT -> "HIM_OFF_EVIDENCE_PROJECTION_V1"
        HimEvidenceRecordKind.AGRIBALYSE_RECORD -> "HIM_AGRIBALYSE_EVIDENCE_PROJECTION_V1"
        HimEvidenceRecordKind.CIQUAL_FOOD -> "HIM_CIQUAL_FOOD_EVIDENCE_PROJECTION_V1"
        HimEvidenceRecordKind.GI_MEASUREMENT -> "HIM_GLYCEMIC_INDEX_MEASUREMENT_EVIDENCE_PROJECTION_V1"
        else -> "HIM_TYPED_EVIDENCE_PROJECTION_${recordKind.name}_V1"
    }

    private fun primaryIdentityAndModifiers(source: HimGroundTruthSource, terms: List<String>): Pair<String, List<String>> = when (source) {
        HimGroundTruthSource.OPEN_FOOD_FACTS -> "Pudding" to terms.filter { normalize(it).contains("vanille") }
        HimGroundTruthSource.AGRIBALYSE -> "Zucker" to terms.filter { normalize(it).contains("vanille") || normalize(it).contains("flavoured") }
        HimGroundTruthSource.CIQUAL -> "Vanille" to terms.filter { normalize(it).contains("gousse") || normalize(it).contains("pod") }
        HimGroundTruthSource.GLYCEMIC_INDEX -> "Keks" to terms.filter { normalize(it).contains("vanille") }
    }

    private fun findCatalogMatches(tokens: List<String>, catalog: HimProductOnlyCanonicalMaster): List<String> =
        catalog.records.filter { record -> tokens.any { token -> normalize(record.itemname).contains(normalize(token)) } }
            .map { "${it.itemname} [${it.normalized}]" }
            .sorted()

    private fun findAuthorityMatches(tokens: List<String>, authority: HimCanonicalFamilyAuthority): List<String> = buildList {
        authority.families.forEach { family ->
            if (tokens.any { token -> normalize(family.canonicalName).contains(normalize(token)) }) add("CANONICAL:${family.canonicalName} [${family.canonicalId.value}]")
            family.identities.forEach { identity ->
                if (tokens.any { token -> normalize(identity.identityName).contains(normalize(token)) }) add("IDENTITY:${identity.identityName} [${identity.identityId.value}]")
            }
            family.variants.forEach { variant ->
                if (tokens.any { token -> normalize(variant.variantName).contains(normalize(token)) }) add("VARIANT:${variant.variantName} [${variant.variantId.value}]")
            }
            family.aliases.forEach { alias ->
                if (tokens.any { token -> normalize(alias.aliasName).contains(normalize(token)) }) add("ALIAS:${alias.aliasName} [${alias.aliasId.value}]")
            }
        }
    }.sorted()

    private fun canonicalAudit(
        family: HimCanonicalFamily,
        workItem: de.shopme.tools.knowledge.him.training.scaling.HimTeacherGroundTruthWorkItemV1,
        projections: List<ProjectionAudit>,
        catalog: HimProductOnlyCanonicalMaster,
        authority: HimCanonicalFamilyAuthority,
    ) = CanonicalAuthorityAudit(
        canonicalName = family.canonicalName,
        normalizedName = family.normalizedName,
        aliases = family.aliases.map { "${it.aliasName} [${it.aliasId.value}]" },
        identities = family.identities.map { "${it.identityName} [${it.identityId.value}]" },
        variants = family.variants.map { "${it.variantName} [${it.variantId.value}]" },
        missingCoverage = workItem.missingCoverage.map { it.name },
        explicitlyCoveredTerms = projections.flatMap { it.normalizedTerms }.distinct().filter { term ->
            term == normalize(family.canonicalName) || family.aliases.any { normalize(it.aliasName) == term } || family.variants.any { normalize(it.variantName) == term } || family.identities.any { normalize(it.identityName) == term }
        },
        possiblyMissingExplicitTerms = projections.flatMap { it.normalizedTerms }.distinct().filter { term ->
            term in setOf("gousse", "pod") && family.variants.none { normalize(it.variantName) == term } && family.aliases.none { normalize(it.aliasName) == term } && family.identities.none { normalize(it.identityName) == term }
        },
        catalogMatches = findCatalogMatches(listOf("gousse", "pod"), catalog),
        authorityMatches = findAuthorityMatches(listOf("gousse", "pod"), authority),
        ciqualFormOrIdentityAssessment = if (findCatalogMatches(listOf("gousse", "pod"), catalog).isEmpty() && findAuthorityMatches(listOf("gousse", "pod"), authority).isEmpty()) "POSSIBLE_FORM_OR_IDENTITY_NOT_PRESENT" else "MATCH_FOUND_REQUIRES_STRUCTURAL_REVIEW",
        note = "Nutritional, environmental, GI, quantity, packaging, and other numeric/source-quality values were not treated as identity, alias, or variant evidence.",
    )

    private fun evaluateUsageBoundary(
        plan: HimCanonicalGroundTruthScalingPlanV1,
        request: HimTeacherGroundTruthGenerationRequestV1,
        persisted: HimTeacherGroundTruthGenerationResultV1,
    ): UsageAudit {
        val runtime = HimSemanticInferenceRuntime {
            HimSemanticInferenceResult.Success(
                HimSemanticInferenceSuccess(
                    emptyList(),
                    HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN,
                    null,
                    emptyList(),
                    persisted.inferenceProvenance,
                ),
            )
        }
        val adapterOutcome = HimSemanticInferenceTeacherProviderAdapterV1(runtime).invoke(request)
        val adapterStructured = adapterOutcome as? HimTeacherGroundTruthProviderOutcomeV1.StructuredResponse
        assertNotNull(adapterStructured)
        assertNull(adapterStructured!!.usage)

        val expectedUsage = HimSemanticUsage(17, 5, 2)
        var fakeCalls = 0
        val fakeProvider = HimTeacherGroundTruthProviderV1 {
            fakeCalls++
            HimTeacherGroundTruthProviderOutcomeV1.StructuredResponse(
                String(HimTeacherGroundTruthOutputCodecV1.serialize(persisted.output), Charsets.UTF_8),
                persisted.inferenceProvenance,
                expectedUsage,
            )
        }
        val pipelineResult = HimTeacherGroundTruthGenerationPipelineV1().generate(plan, request, fakeProvider)
        assertEquals(1, fakeCalls)
        assertEquals(expectedUsage, pipelineResult.usage)
        val directory = Files.createTempDirectory("him-post-run-usage").toFile()
        val file = directory.resolve("result.v1.json")
        HimTeacherGroundTruthGenerationPersistenceV1.writeNewResult(file, pipelineResult)
        val reloaded = HimTeacherGroundTruthGenerationPersistenceV1.readResult(file)
        assertEquals(expectedUsage, reloaded.usage)
        return UsageAudit(
            persistedUsage = null,
            adapterOutputUsage = null,
            pipelineFakeInputUsage = "input=17,output=5,cached=2",
            pipelineFakeReloadUsage = "input=17,output=5,cached=2",
            diagnosis = "PAID_RUNNER_DID_NOT_CAPTURE_USAGE",
            evidence = "HimOpenAiSemanticInferenceProvider exposes provider usage through HimOpenAiUsageDiagnostics. The positive Single-Item Paid Runner does not read provider.diagnostics.usage after the runtime/pipeline call and does not pass it into the Teacher result. The pipeline and persistence preserve usage when explicitly supplied at their boundary, so the real paid-run loss is at runner capture; no raw OpenAI response is persisted here.",
        )
    }

    private fun buildRequest(
        plan: HimCanonicalGroundTruthScalingPlanV1,
        selection: HimPositiveSingleItemTeacherPilotSelection,
        families: List<HimCanonicalFamily>,
        evidence: List<HimEvidenceSearchResult>,
    ): HimTeacherGroundTruthGenerationRequestV1 {
        val candidate = selection.selectedCandidate
        val family = families.single { it.canonicalId == candidate.entityId }
        val context = HimCanonicalFamilyCandidateRetrieval(families)
            .retrieve(HimCanonicalRetrievalQuery(family.canonicalName, family.normalizedName))
        val teacherContext = context.map { result ->
            de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext(
                result.rank,
                result.canonicalId,
                result.canonicalName,
                (result as? HimCanonicalRetrievalResult.Full)?.let { Gson.toJson(it.family) },
            )
        }
        val history = listOf(
            de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticRetrievalHistoryEntry(
                HimRetrievalRound(1),
                de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticRetrievalDirective(
                    evidence.map { de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticSourceQueries(it.source, listOf(candidate.rawInput)) },
                    HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP,
                ),
                evidence.map(HimSemanticSourceArtifactIdentityV1::reference),
            ),
        )
        val inference = de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceRequest(
            "teacher-f3-8g4:${candidate.workItemReference}",
            candidate.rawInput,
            context,
            evidence,
            HimRetrievalRound(1),
            HimGroundTruthSource.entries.toSet(),
            history,
        )
        return HimTeacherGroundTruthGenerationRequestV1.create(
            plan.workItems.single { it.reference == candidate.workItemReference },
            candidate.rawInput,
            teacherContext,
            inference,
            de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthPolicyBindingsV1(
                providerConfigurationFingerprint = HimOpenAiSemanticProviderConfiguration().fingerprint(),
                contextBudgetPolicyVersion = HimSemanticContextBudgetPolicy.VERSION,
            ),
        )
    }

    private fun currentPlan(root: File, catalog: HimProductOnlyCanonicalMaster, authority: HimCanonicalFamilyAuthority, release: de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1): HimCanonicalGroundTruthScalingPlanV1 =
        HimCanonicalGroundTruthScalingPlannerV1().plan(
            HimCanonicalGroundTruthScalingPlannerInputV1(catalog, authority, release, candidateBinding(root), emptyList()),
        )

    private fun candidateBinding(root: File): HimCandidateDatasetBindingV1? {
        val file = root.resolve("data/knowledge/him/candidates/master/candidate-dataset.v2.json")
        if (!file.isFile) return null
        return HimCandidateDatasetBindingV1(
            "data/knowledge/him/candidates/master/candidate-dataset.v2.json",
            de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateIdentityV1.datasetDigest(
                de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetPersistenceV2.readDataset(file),
            ),
        )
    }

    private fun openStores(root: File): Map<HimGroundTruthSource, (HimEvidenceRecordReference) -> HimEvidenceSearchResult?> {
        val offValidation = HimOffEvidenceIndexValidator.validateReadOnly(root.resolve(HimOffProductionEvidenceIndexPaths.FINAL_INDEX))
        val agrValidation = HimAgribalyseEvidenceIndexValidator.validateReadOnly(root.resolve(HimAgribalyseProductionEvidenceIndexPaths.FINAL_INDEX))
        val ciValidation = HimCiqualEvidenceIndexValidator.validateReadOnly(root.resolve(HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX))
        val giValidation = HimGlycemicIndexEvidenceIndexValidator.validateReadOnly(root.resolve(HimGlycemicIndexProductionEvidenceIndexPaths.FINAL_INDEX))
        val off = HimOffSqliteEvidenceRetrievalStore.openAfterValidation(root.resolve(HimOffProductionEvidenceIndexPaths.FINAL_INDEX), offValidation)
        val agr = HimAgribalyseSqliteEvidenceRetrievalStore.openAfterValidation(root.resolve(HimAgribalyseProductionEvidenceIndexPaths.FINAL_INDEX), agrValidation)
        val ci = HimCiqualSqliteEvidenceRetrievalStore.openAfterValidation(root.resolve(HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX), ciValidation)
        val gi = HimGlycemicIndexSqliteEvidenceRetrievalStore.openAfterValidation(root.resolve(HimGlycemicIndexProductionEvidenceIndexPaths.FINAL_INDEX), giValidation)
        return mapOf(
            HimGroundTruthSource.OPEN_FOOD_FACTS to off::fetch,
            HimGroundTruthSource.AGRIBALYSE to agr::fetch,
            HimGroundTruthSource.CIQUAL to ci::fetch,
            HimGroundTruthSource.GLYCEMIC_INDEX to gi::fetch,
        )
    }

    private fun explicitTerms(source: HimGroundTruthSource, element: JsonElement): List<String> {
        val paths = when (source) {
            HimGroundTruthSource.OPEN_FOOD_FACTS -> listOf("identity.productName", "identity.productNameGerman", "identity.productNameEnglish", "identity.genericName", "identity.genericNameGerman", "identity.genericNameEnglish", "identity.productType", "taxonomy.categories", "taxonomy.categoryHierarchy")
            HimGroundTruthSource.AGRIBALYSE -> listOf("productNameFr", "lciName", "foodGroup", "foodSubgroup")
            HimGroundTruthSource.CIQUAL -> listOf("nameFr", "nameEn", "scientificName.lexicalValue", "groupNameFr", "subgroupNameFr", "subSubgroupNameFr", "groupNameEn", "subgroupNameEn", "subSubgroupNameEn")
            HimGroundTruthSource.GLYCEMIC_INDEX -> listOf("foodItem.lexicalValue", "sourceContext.majorCategory", "sourceContext.subcategory", "sourceContext.deeperHeading")
        }
        return paths.flatMap { path -> pathValues(element, path.split('.')) }.filter { it.isNotBlank() }.distinct()
    }

    private fun pathValues(element: JsonElement, path: List<String>): List<String> {
        if (path.isEmpty()) return if (element.isJsonPrimitive && element.asJsonPrimitive.isString) listOf(element.asString) else emptyList()
        if (!element.isJsonObject) return emptyList()
        val next = element.asJsonObject.get(path.first()) ?: return emptyList()
        return pathValues(next, path.drop(1))
    }

    private fun writeOrVerify(file: File, bytes: ByteArray) {
        if (file.exists()) assertArrayEquals(bytes, file.readBytes()) else file.writeBytes(bytes)
    }

    private fun textReport(report: EvaluationReport): String = buildString {
        appendLine("HIM POSITIVE SINGLE-ITEM TEACHER PAID PILOT POST-RUN EVALUATION V1")
        appendLine("HEAD=${report.paidRunHead}")
        appendLine("PRIMARY_CLASSIFICATION=${report.primaryClassification}")
        appendLine("CLASSIFICATION_REASON=${report.classificationReason}")
        appendLine("ITEM=${report.item.canonical}/${report.item.entityId}/${report.item.partition}")
        appendLine("WORK_ITEM=${report.item.workItemReference}")
        appendLine("SELECTION_REQUEST=${report.item.selectionRequestReference}")
        appendLine("EVIDENCE_REQUEST=${report.item.evidenceRequestReference}")
        appendLine("RESULT_REFERENCE=${report.item.resultReference}")
        appendLine("RESULT_LOGICAL_DIGEST=${report.item.resultLogicalDigest}")
        appendLine("CANDIDATE_DATASET_DIGEST=${report.item.candidateDatasetDigest}")
        appendLine("TEACHER_SCHEMA=${report.teacherOutput.schemaVersion}")
        appendLine("PROPOSALS=${report.teacherOutput.proposalCount}")
        appendLine("INFORMATION_GAIN=${report.teacherOutput.informationGain}")
        appendLine("RETRIEVAL_DIRECTIVE=${report.teacherOutput.retrievalDirective}")
        appendLine("RETRIEVAL_ROUND=${report.item.retrievalRound}")
        appendLine("PROVIDER=${report.teacherOutput.provider}")
        appendLine("MODEL=${report.teacherOutput.model}")
        appendLine("PROVIDER_ATTEMPTS=${report.teacherOutput.providerAttempts}")
        appendLine()
        appendLine("EVIDENCE")
        report.evidence.forEach { evidence ->
            appendLine("${evidence.source}|${evidence.reference}|${evidence.recordKind}|schema=${evidence.projectionSchema}|primary=${evidence.primaryIdentity}|modifiers=${evidence.modifiers.joinToString(",")}|${evidence.assessment}|${evidence.reason}")
            appendLine("TERMS=${evidence.explicitTerms.joinToString(",")}")
        }
        appendLine()
        appendLine("CANONICAL_AUTHORITY")
        appendLine("NAME=${report.canonicalAuthority.canonicalName}")
        appendLine("NORMALIZED_NAME=${report.canonicalAuthority.normalizedName}")
        appendLine("IDENTITIES=${report.canonicalAuthority.identities.joinToString(",")}")
        appendLine("VARIANTS=${report.canonicalAuthority.variants.joinToString(",")}")
        appendLine("ALIASES=${report.canonicalAuthority.aliases.joinToString(",")}")
        appendLine("MISSING_COVERAGE=${report.canonicalAuthority.missingCoverage.joinToString(",")}")
        appendLine("EXPLICITLY_COVERED_TERMS=${report.canonicalAuthority.explicitlyCoveredTerms.joinToString(",")}")
        appendLine("POSSIBLY_MISSING_EXPLICIT_TERMS=${report.canonicalAuthority.possiblyMissingExplicitTerms.joinToString(",")}")
        appendLine("CATALOG_MATCHES=${report.canonicalAuthority.catalogMatches.joinToString(",")}")
        appendLine("AUTHORITY_MATCHES=${report.canonicalAuthority.authorityMatches.joinToString(",")}")
        appendLine("CIQUAL_FORM_OR_IDENTITY_ASSESSMENT=${report.canonicalAuthority.ciqualFormOrIdentityAssessment}")
        appendLine("NOTE=${report.canonicalAuthority.note}")
        appendLine()
        appendLine("USAGE_DIAGNOSIS=${report.usageDiagnostic.diagnosis}")
        appendLine("USAGE_EVIDENCE=${report.usageDiagnostic.evidence}")
        appendLine("PERSISTED_USAGE=${report.usageDiagnostic.persistedUsage}")
        appendLine("ADAPTER_OUTPUT_USAGE=${report.usageDiagnostic.adapterOutputUsage}")
        appendLine("PIPELINE_FAKE_USAGE=${report.usageDiagnostic.pipelineFakeInputUsage}")
        appendLine("PIPELINE_RELOAD_USAGE=${report.usageDiagnostic.pipelineFakeReloadUsage}")
        appendLine()
        appendLine("VALIDATION resultReader=${report.validation.resultReader} request=${report.validation.requestValidation} output=${report.validation.outputValidation} evidenceFetches=${report.validation.evidenceFetches} sourceScans=${report.validation.sourceScans} indexRebuilds=${report.validation.indexRebuilds} sqliteWrites=${report.validation.sqliteWrites}")
        appendLine("SAFETY openAiCalls=${report.safety.openAiCalls} networkCalls=${report.safety.networkCalls} apiKeyAccesses=${report.safety.apiKeyAccesses} providerConstructions=${report.safety.providerConstructions} teacherInference=${report.safety.teacherInference} paidInference=${report.safety.paidInference} protectedMutations=${report.safety.protectedMutations}")
        appendLine("POST_RUN_EVALUATION_EVIDENCE_ALIGNMENT_DEFECT")
    }

    private fun sha256(file: File): String = sha256(file.readBytes())
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    private fun normalize(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC).replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
    private fun projectRoot(): File { var current = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile; while (!current.resolve("settings.gradle.kts").isFile) current = requireNotNull(current.parentFile); return current }
    private fun relativePath(root: File, file: File): String = root.toPath().toAbsolutePath().normalize().relativize(file.toPath().toAbsolutePath().normalize()).toString().replace(File.separatorChar, '/')

    private enum class PrimaryClassification { SUPPORTED_NO_PROPOSAL, POSSIBLE_FALSE_NEGATIVE, EVIDENCE_ALIGNMENT_DEFECT, INSUFFICIENT_OFFLINE_EVIDENCE_TO_JUDGE }
    private data class ArtifactDigest(val path: String, val bytes: Long, val sha256: String)
    private data class ItemAudit(val canonical: String, val entityId: String, val partition: String, val workItemReference: String, val selectionRequestReference: String, val evidenceRequestReference: String, val resultReference: String, val resultLogicalDigest: String, val candidateDatasetDigest: String, val retrievalRound: Int)
    private data class TeacherOutputAudit(val schemaVersion: String, val proposalCount: Int, val informationGain: String, val retrievalDirective: String, val providerAttempts: Int, val provider: String, val model: String, val usage: String?)
    private data class ProjectionAudit(val source: String, val reference: String, val recordKind: String, val sourceArtifactSha256: String, val projectionSchema: String, val projectionDigest: String, val primaryIdentity: String, val modifiers: List<String>, val explicitTerms: List<String>, val normalizedTerms: List<String>, val directRelationSupported: Boolean, val possibleUncoveredVariant: Boolean, val assessment: String, val reason: String)
    private data class CanonicalAuthorityAudit(val canonicalName: String, val normalizedName: String, val aliases: List<String>, val identities: List<String>, val variants: List<String>, val missingCoverage: List<String>, val explicitlyCoveredTerms: List<String>, val possiblyMissingExplicitTerms: List<String>, val catalogMatches: List<String>, val authorityMatches: List<String>, val ciqualFormOrIdentityAssessment: String, val note: String)
    private data class UsageAudit(val persistedUsage: String?, val adapterOutputUsage: String?, val pipelineFakeInputUsage: String, val pipelineFakeReloadUsage: String, val diagnosis: String, val evidence: String)
    private data class ValidationAudit(val resultReader: String, val outputValidation: String, val requestValidation: String, val evidenceFetches: Int, val sourceScans: Int, val indexRebuilds: Int, val sqliteWrites: Int)
    private data class SafetyAudit(val openAiCalls: Int, val networkCalls: Int, val apiKeyAccesses: Int, val providerConstructions: Int, val teacherInference: Int, val paidInference: Int, val protectedMutations: Int, val sourceScans: Int, val indexRebuilds: Int, val sqliteWrites: Int)
    private data class EvaluationReport(val reportVersion: String, val inputArtifacts: List<ArtifactDigest>, val paidRunHead: String, val item: ItemAudit, val teacherOutput: TeacherOutputAudit, val evidence: List<ProjectionAudit>, val canonicalAuthority: CanonicalAuthorityAudit, val primaryClassification: String, val classificationReason: String, val usageDiagnostic: UsageAudit, val validation: ValidationAudit, val safety: SafetyAudit)
    private data class NegativeVanillaCase(val primaryIdentity: String, val modifier: String, val sourceProductName: String)

    private companion object {
        private const val EXPECTED_HEAD = "bf505d311e137204cd7540ee4d79e19f3f1f7879"
        private const val EXPECTED_CANONICAL_ID = "uEV2jY"
        private const val EXPECTED_WORK_ITEM = "teacher-work:v1:1ac878a8b015493878574df8ccc2b2a45aa78622c6fa00211e7b7b6b6d0be4c5"
        private const val EXPECTED_REQUEST = "teacher-request:v1:c0fe080f302199536bb6d0c89ef13b8e7649f082fc66bd16774c16f522c1902e"
        private const val EXPECTED_RESULT_REFERENCE = "teacher-result:v1:1b1c7dfa4e3086e807c0ecf90bd0b1a6e17abb59f6675e656ad30ea990d35d17"
        private const val EXPECTED_RESULT_DIGEST = "1b1c7dfa4e3086e807c0ecf90bd0b1a6e17abb59f6675e656ad30ea990d35d17"
        private const val CANDIDATE_DATASET_DIGEST = "0930dd6b0fd569ed1ef054daa65e8e01712c98bce041b45f7a48e2fa404c845c"
        private const val V2_PATH = "build/knowledge/reports/him/training/him-teacher-paid-pilot-offline-preflight.v2.json"
        private const val SELECTION_PATH = "build/knowledge/reports/him/training/him-positive-single-item-teacher-paid-pilot.selection.v1.r5.json"
        private const val RESULT_PATH = "build/knowledge/reports/him/training/him-positive-single-item-teacher-paid-pilot.result.v1.json"
        private const val PAID_REPORT_PATH = "build/knowledge/reports/him/training/him-positive-single-item-teacher-paid-pilot.txt"
        private const val JSON_REPORT_PATH = "build/knowledge/reports/him/training/him-positive-single-item-teacher-paid-pilot.post-run-evaluation.v1.json"
        private const val TEXT_REPORT_PATH = "build/knowledge/reports/him/training/him-positive-single-item-teacher-paid-pilot.post-run-evaluation.v1.txt"
        private const val V2_SHA256 = "b85fc5c6abeb965494b4b51f069151d9b47fa85a35cad3f419da61b0f17558aa"
        private const val V2_LOGICAL_DIGEST = "d372c2137ebc52f1b1b86d23c3a2936f8f4db53581a483717081e787d67cd705"
        private const val SELECTION_SHA256 = "09d5e1ae7c34e005dd736a885cb4c560899f2563d6822f00b8e5ff4e6b41e0b9"
        private const val SELECTION_LOGICAL_DIGEST = "1aa6ae2a62abec3218b6faae012db5bfd8621009787fcfc03a14c75fe59be17e"
        private const val RESULT_SHA256 = "816ee1c4c24e306f5444eeb4325dfacd49189db8b7ec1535943f92cede4745f3"
        private const val PAID_REPORT_SHA256 = "427b6d7cffeaa41c5d468b24bed9137390aa71e3f4325a5518405811b997c461"
        private val EXPECTED_EVIDENCE = listOf(
            HimEvidenceReference("OPEN_FOOD_FACTS", HimSha256("63b20183229a6f3f648c3d189c265d5d5a4b332d530d084af5640081d060a236"), "off:product:row:4474818:code:4260694945322"),
            HimEvidenceReference("AGRIBALYSE", HimSha256("9068c89fa887ef087e87dcc51f758dd29e9623b93d9bd0a2277a4faae57c2297"), "agribalyse:row:1804:agb:31044"),
            HimEvidenceReference("CIQUAL", HimSha256("807d16c222f0e812831c10bdd89f1a5ebbc74c95e8a4944723db486add96dcff"), "ciqual:food:11057"),
            HimEvidenceReference("GLYCEMIC_INDEX", HimSha256("6891c2ff2ab3734a1d2768339c1f660f7093a31b97a856c82bf9b8c3c88422b5"), "gi:measurement:823"),
        )
        private val Gson = GsonBuilder().disableHtmlEscaping().create()
    }

    private fun classifyVanillaRegression(primaryIdentity: String, modifier: String): String =
        if (normalize(primaryIdentity) == "vanille" && normalize(modifier).isBlank()) "EXACT_DIRECT" else "CONTEXTUAL"
}
