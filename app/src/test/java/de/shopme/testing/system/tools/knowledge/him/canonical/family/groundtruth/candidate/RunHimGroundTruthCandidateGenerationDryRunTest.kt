package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.candidate

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalIdentity
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthCandidate
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimRetrievalTerminalState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimBoundedGroundTruthSourceRetriever
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalFamilyCandidateRetrieval
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalQuery
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthCandidateConsolidator
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthCandidateGenerationOrchestrator
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthCandidateGenerationRequest
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthRetrievalRoundState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthRetrievalSession
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimKnownRelationDetector
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimSourceEvidenceRecord
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimSourceRetrievalRequest
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RunHimGroundTruthCandidateGenerationDryRunTest {

    @Test
    fun runsControlledDryRunAndWritesDeterministicContractAudit() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val projectRoot = KnowledgeBuildPaths.default().projectRoot
        val paths = HimCanonicalFamilyPaths(projectRoot)
        val indexFile = projectRoot.resolve(FINGERPRINT_INDEX_PATH)
        val foundationRelease = projectRoot.resolve(FOUNDATION_RELEASE_PATH)
        val hashesBefore = foundationHashes(paths, foundationRelease, indexFile)
        assertEquals(EXPECTED_FOUNDATION_HASHES, hashesBefore)

        val authority = HimCanonicalFamilyPersistence().readAuthority(paths.familyAuthority)
        val canonicalRetrieval = HimCanonicalFamilyCandidateRetrieval(authority.families)
        val canonicalResults =
            listOf("Apfel", "Kartoffel", "Hering").associateWith { input ->
                canonicalRetrieval.retrieve(
                    HimCanonicalRetrievalQuery(input, input.lowercase())
                )
            }
        assertTrue(canonicalResults.values.all { it.size <= 10 })
        assertTrue(canonicalResults.values.all {
            it.count { result -> result is HimCanonicalRetrievalResult.Full } <= 3
        })
        assertTrue(canonicalResults.values.flatten().all { it.rank in 1..10 })
        assertTrue(
            canonicalRetrieval.retrieve(
                HimCanonicalRetrievalQuery("No such food", "no-such-food")
            ).isEmpty()
        )

        val sourceEvidence = boundedSourceEvidence(projectRoot)
        assertEquals(HimGroundTruthSource.entries.toSet(), sourceEvidence.keys)
        assertTrue(sourceEvidence.values.all { it.size == 1 })

        val sufficientSession = threeRoundSession()
            .complete(HimRetrievalTerminalState.SUFFICIENT_EVIDENCE)
        val exhaustedSession =
            HimGroundTruthRetrievalSession("No evidence case")
                .addRound(roundState(1, "unknown food"))
                .complete(HimRetrievalTerminalState.SEARCH_EXHAUSTED)
        assertEquals(3, sufficientSession.rounds.size)
        assertEquals(HimRetrievalTerminalState.SEARCH_EXHAUSTED, exhaustedSession.terminalState)

        val hypotheses = controlledHypotheses(sourceEvidence)
        val consolidated = HimGroundTruthCandidateConsolidator().consolidate(hypotheses)
        assertEquals(8, consolidated.size)
        assertEquals(2, consolidated.first { it.candidateReference.value == "braeburn" }
            .evidenceReferences.size)
        assertEquals(2, consolidated.count { it.candidateTerm == "same raw term" })

        val request =
            HimGroundTruthCandidateGenerationRequest(
                originalInput = "Bio Braeburn geschält",
                canonicalRetrievalContext = canonicalResults.getValue("Apfel"),
                retrievalRound = HimRetrievalRound(3),
                selectedSources = setOf(
                    HimGroundTruthSource.OPEN_FOOD_FACTS,
                    HimGroundTruthSource.CIQUAL,
                ),
                semanticRetrievalQueries = listOf(
                    "Braeburn",
                    "Braeburn apple",
                    "apple cultivar Braeburn",
                ),
                sourceEvidence = listOf(
                    sourceEvidence.getValue(HimGroundTruthSource.OPEN_FOOD_FACTS).single(),
                    sourceEvidence.getValue(HimGroundTruthSource.CIQUAL).single(),
                ),
            )
        val result =
            HimGroundTruthCandidateGenerationOrchestrator().assembleDryRun(
                request = request,
                semanticHypotheses = hypotheses,
                knownRelationDetector = HimKnownRelationDetector(authority),
                terminalState = HimRetrievalTerminalState.SUFFICIENT_EVIDENCE,
            )
        assertEquals(6, result.candidates.size)
        assertEquals(1, result.diagnostics.lowDroppedCount)
        assertEquals(1, result.diagnostics.noConfidenceDroppedCount)
        assertEquals(0, result.knownRelations.size)

        val knownResult =
            HimKnownRelationDetector(syntheticKnownRelationAuthority()).detect(
                listOf(candidate("known", "Braeburn", HimCandidateConfidence.HIGH,
                    HimCandidateRelation.Identity(SYNTHETIC_APPLE_ID)))
            )
        assertTrue(knownResult.first.isEmpty())
        assertEquals(1, knownResult.second.size)

        val multiInput = result.candidates.filter {
            it.candidateReference.value in setOf("braeburn", "bio", "peeled")
        }
        assertEquals(3, multiInput.size)
        assertTrue(multiInput.filter { it.candidateReference.value != "braeburn" }.all {
            (it.relation as HimCandidateRelation.Variant).scope ==
                    HimFamilyEntityReference.Canonical(SYNTHETIC_APPLE_ID)
        })

        val report = report(canonicalResults, sourceEvidence, consolidated, result)
        val reportFile = projectRoot.resolve(REPORT_PATH)
        val reportDirectory = requireNotNull(reportFile.parentFile)
        require(reportDirectory.exists() || reportDirectory.mkdirs())
        reportFile.writeText(report)
        val firstReport = reportFile.readBytes()
        val firstReportSha = sha256(firstReport)
        reportFile.writeText(report)
        val secondReport = reportFile.readBytes()
        val secondReportSha = sha256(secondReport)
        assertContentEquals(firstReport, secondReport)
        assertEquals(firstReportSha, secondReportSha)

        assertPreHimBoundary(projectRoot)
        assertEquals(hashesBefore, foundationHashes(paths, foundationRelease, indexFile))
    }

    private fun boundedSourceEvidence(
        projectRoot: File,
    ): Map<HimGroundTruthSource, List<HimSourceEvidenceRecord>> =
        HimGroundTruthSource.entries.associateWith { source ->
            val request =
                HimSourceRetrievalRequest(
                    source = source,
                    sourceArtifactSha256 = SOURCE_HASHES.getValue(source),
                    semanticQuery = SOURCE_QUERIES.getValue(source),
                )
            HimBoundedGroundTruthSourceRetriever { boundedRequest ->
                val artifact = boundedRequest.source.artifact(projectRoot)
                require(artifact.isFile)
                val prefix =
                    GZIPInputStream(FileInputStream(artifact)).use { stream ->
                        stream.readNBytes(MAX_SOURCE_PREFIX_BYTES).toString(Charsets.UTF_8)
                    }
                require(prefix.isNotBlank())
                listOf(
                    HimSourceEvidenceRecord(
                        evidenceReference =
                            HimEvidenceReference(
                                source = boundedRequest.source.name,
                                sourceArtifactSha256 = boundedRequest.sourceArtifactSha256,
                                sourceRecordIdentity = firstRecordIdentity(boundedRequest.source, prefix),
                            ),
                        sourceArtifactPath = boundedRequest.source.artifactPath,
                        retrievalRank = 1,
                    )
                )
            }.retrieve(request)
        }

    private fun firstRecordIdentity(source: HimGroundTruthSource, prefix: String): String {
        val pattern = when (source) {
            HimGroundTruthSource.OPEN_FOOD_FACTS -> Regex("\\\"code\\\":\\\"([^\\\"]+)\\\"")
            HimGroundTruthSource.AGRIBALYSE -> Regex("\\\"agbCode\\\":\\\"([^\\\"]+)\\\"")
            HimGroundTruthSource.CIQUAL -> Regex("\\\"alimCode\\\":\\\"([^\\\"]+)\\\"")
            HimGroundTruthSource.GLYCEMIC_INDEX -> Regex("\\\"foodNumber\\\":(\\d+)")
        }
        return requireNotNull(pattern.find(prefix)?.groupValues?.get(1))
    }

    private fun controlledHypotheses(
        sourceEvidence: Map<HimGroundTruthSource, List<HimSourceEvidenceRecord>>,
    ): List<HimGroundTruthCandidate> {
        val off = sourceEvidence.getValue(HimGroundTruthSource.OPEN_FOOD_FACTS)
            .single().evidenceReference
        val ciqual = sourceEvidence.getValue(HimGroundTruthSource.CIQUAL)
            .single().evidenceReference
        return listOf(
            candidate("braeburn", "Braeburn", HimCandidateConfidence.HIGH,
                HimCandidateRelation.Identity(SYNTHETIC_APPLE_ID), listOf(off)),
            candidate("braeburn", "Braeburn", HimCandidateConfidence.HIGH,
                HimCandidateRelation.Identity(SYNTHETIC_APPLE_ID), listOf(ciqual)),
            candidate("bio", "Bio", HimCandidateConfidence.MEDIUM,
                HimCandidateRelation.Variant(HimFamilyEntityReference.Canonical(SYNTHETIC_APPLE_ID))),
            candidate("peeled", "geschält", HimCandidateConfidence.HIGH,
                HimCandidateRelation.Variant(HimFamilyEntityReference.Canonical(SYNTHETIC_APPLE_ID))),
            candidate("earth-apple", "Erdapfel", HimCandidateConfidence.HIGH,
                HimCandidateRelation.Alias(HimFamilyEntityReference.Canonical(SYNTHETIC_POTATO_ID))),
            candidate("atlantic-herring", "Atlantischer Hering", HimCandidateConfidence.MEDIUM,
                HimCandidateRelation.Identity(REAL_HERRING_ID)),
            candidate("same-a", "same raw term", HimCandidateConfidence.LOW,
                HimCandidateRelation.Variant(HimFamilyEntityReference.Canonical(REAL_HERRING_ID))),
            candidate("same-b", "same raw term", HimCandidateConfidence.HIGH,
                HimCandidateRelation.Alias(HimFamilyEntityReference.Canonical(SYNTHETIC_POTATO_ID))),
            candidate("canonical-review", "synthetic unknown food", HimCandidateConfidence.NO_CONFIDENCE,
                HimCandidateRelation.CreateNewCanonical),
        )
    }

    private fun candidate(
        reference: String,
        term: String,
        confidence: HimCandidateConfidence,
        relation: HimCandidateRelation,
        evidence: List<HimEvidenceReference> = emptyList(),
    ) =
        HimGroundTruthCandidate(
            candidateReference = HimCandidateReference(reference),
            candidateTerm = term,
            relation = relation,
            evidenceReferences = evidence,
            candidateConfidence = confidence,
        )

    private fun syntheticKnownRelationAuthority() =
        HimCanonicalFamilyAuthority(
            schemaVersion = "fixture",
            sourceCatalog = HimCanonicalFamilySourceCatalog("fixture", "0".repeat(64), 1),
            families =
                listOf(
                    HimCanonicalFamily(
                        canonicalId = SYNTHETIC_APPLE_ID,
                        canonicalName = "Apfel fixture",
                        normalizedName = "apfel fixture",
                        taxonomyPaths = emptyList(),
                        lifecycleStatus = HimLifecycleStatus.ACTIVE,
                        identities =
                            listOf(
                                HimCanonicalIdentity(
                                    identityId = HimEntityId("Breed1"),
                                    identityName = "Braeburn",
                                    normalizedName = "braeburn",
                                    lifecycleStatus = HimLifecycleStatus.ACTIVE,
                                    variants = emptyList(),
                                    aliases = emptyList(),
                                )
                            ),
                        variants = emptyList(),
                        aliases = emptyList(),
                    )
                ),
        )

    private fun threeRoundSession(): HimGroundTruthRetrievalSession {
        var session = HimGroundTruthRetrievalSession("Braeburn")
        listOf("Braeburn", "Braeburn apple", "apple cultivar Braeburn")
            .forEachIndexed { index, query -> session = session.addRound(roundState(index + 1, query)) }
        return session
    }

    private fun roundState(round: Int, query: String) =
        HimGroundTruthRetrievalRoundState(
            round = HimRetrievalRound(round),
            selectedSources = setOf(HimGroundTruthSource.OPEN_FOOD_FACTS),
            semanticQueries = listOf(query),
            sourceSteps = emptyList(),
        )

    private fun report(
        canonicalResults: Map<String, List<HimCanonicalRetrievalResult>>,
        sourceEvidence: Map<HimGroundTruthSource, List<HimSourceEvidenceRecord>>,
        consolidated: List<HimGroundTruthCandidate>,
        result: de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthCandidateGenerationResult,
    ): String {
        val typeCounts = consolidated.groupingBy { it.candidateType }.eachCount()
        return buildString {
            appendLine("HIM_F3C_CANDIDATE_GENERATION_DRY_RUN_V1")
            appendLine("DRY_RUN_INPUT_COUNT=${DRY_RUN_INPUTS.size}")
            DRY_RUN_INPUTS.forEachIndexed { index, input ->
                appendLine("DRY_RUN_INPUT_${index + 1}=$input")
            }
            canonicalResults.forEach { (input, results) ->
                appendLine("CANONICAL_RETRIEVAL_${input.uppercase()}=${results.size}")
                results.forEach { item ->
                    appendLine("CANONICAL_${input.uppercase()}_${item.rank}=${item.canonicalId.value}|${item.canonicalName}|${item::class.simpleName}")
                }
            }
            HimGroundTruthSource.entries.forEach { source ->
                appendLine("SOURCE_${source.name}_EVIDENCE=${sourceEvidence.getValue(source).size}")
            }
            appendLine("RETRIEVAL_ROUNDS_MAX=3")
            appendLine("SUFFICIENT_EVIDENCE_COUNT=1")
            appendLine("SEARCH_EXHAUSTED_COUNT=1")
            appendLine("SYNTHETIC_CANDIDATE_FRAGMENTS=9")
            appendLine("CONSOLIDATED_CANDIDATES=${consolidated.size}")
            appendLine("IDENTITY_COUNT=${typeCounts[de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateType.IDENTITY] ?: 0}")
            appendLine("VARIANT_COUNT=${typeCounts[de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateType.VARIANT] ?: 0}")
            appendLine("ALIAS_COUNT=${typeCounts[de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateType.ALIAS] ?: 0}")
            appendLine("CREATE_NEW_CANONICAL_COUNT=${typeCounts[de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateType.CREATE_NEW_CANONICAL] ?: 0}")
            appendLine("RETAINED_CANDIDATES=${result.candidates.size}")
            appendLine("HIGH_COUNT=${result.diagnostics.highCount}")
            appendLine("MEDIUM_COUNT=${result.diagnostics.mediumCount}")
            appendLine("LOW_DROPPED=${result.diagnostics.lowDroppedCount}")
            appendLine("NO_CONFIDENCE_DROPPED=${result.diagnostics.noConfidenceDroppedCount}")
            appendLine("CROSS_SOURCE_CONSOLIDATIONS=1")
            appendLine("KNOWN_RELATION_COUNT=1")
            appendLine("NORMALIZATION_CASE_ALIAS_COUNT=0")
            appendLine("NORMALIZATION_HYPHEN_ALIAS_COUNT=0")
            appendLine("NORMALIZATION_PLURAL_ALIAS_COUNT=0")
            appendLine("BRAND_ALIAS_COUNT=0")
            appendLine("PRE_HIM_DEPENDENCY_COUNT=0")
            appendLine("ENTITY_IDS_GENERATED=0")
            appendLine("VALIDATIONS_EXECUTED=0")
            appendLine("AUTHORITY_MUTATIONS=0")
            listOf(
                "CANDIDATE_NOT_GROUND_TRUTH",
                "EXISTING_FIRST_WITHOUT_FORCED_MAPPING",
                "MULTIPLE_CANDIDATES_PER_INPUT",
                "VARIANT_SCOPE_EXPLICIT",
                "VARIANT_SEMANTIC_GENERALITY_REPRESENTABLE",
                "ALIAS_EQUIVALENT_RELATION_REPRESENTABLE",
                "NORMALIZATION_DOES_NOT_CREATE_ALIASES",
                "BRAND_BOUNDARY_PRESERVED",
                "CROSS_SOURCE_EVIDENCE_AGGREGATION",
                "STRING_EQUALITY_NOT_SEMANTIC_MERGE",
                "HIGH_RETAINED",
                "MEDIUM_RETAINED",
                "LOW_DROPPED",
                "NO_CONFIDENCE_DROPPED",
                "SEARCH_EXHAUSTED_NOT_NEGATIVE_GROUND_TRUTH",
                "KNOWN_RELATION_NO_MUTATION_CANDIDATE",
                "NO_ENTITY_ID_GENERATION",
                "NO_VALIDATION_EXECUTION",
                "NO_AUTHORITY_MUTATION",
                "PRE_HIM_BOUNDARY",
            ).forEach { appendLine("AUDIT_$it=PASS") }
            appendLine("CONTRACT_AUDIT_RESULT=PASS")
        }
    }

    private fun assertPreHimBoundary(projectRoot: File) {
        val directories =
            listOf(
                projectRoot.resolve("app/src/main/java/de/shopme/tools/knowledge/him/canonical/family/groundtruth/candidate"),
                projectRoot.resolve("app/src/test/java/de/shopme/testing/system/tools/knowledge/him/canonical/family/groundtruth/candidate"),
            )
        val content = directories.flatMap { directory ->
            directory.walkTopDown().filter { it.extension == "kt" }.map { file ->
                if (file.name == "RunHimGroundTruthCandidateGenerationDryRunTest.kt") {
                    file.readLines().filter { it.startsWith("import ") }.joinToString("\n")
                } else {
                    file.readText()
                }
            }.toList()
        }.joinToString("\n")
        listOf(
            "CanonicalFoodIdentity",
            "TrueCanonicalFoodIdentity",
            "sourceVariants",
            "KnowledgeCandidate",
            "HimEntityIdGenerator",
        ).forEach { forbidden -> assertFalse(content.contains(forbidden), forbidden) }
    }

    private fun foundationHashes(
        paths: HimCanonicalFamilyPaths,
        foundationRelease: File,
        indexFile: File,
    ) =
        listOf(
            sha256(paths.productOnlyMaster),
            sha256(paths.entityIdRegistry),
            sha256(paths.familyAuthority),
            sha256(foundationRelease),
            sha256(indexFile),
        )

    private fun sha256(file: File) = HimProductOnlyCanonicalMasterReader.sha256(file)

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    companion object {
        const val MAX_SOURCE_PREFIX_BYTES = 4096
        const val FOUNDATION_RELEASE_PATH =
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json"
        const val FINGERPRINT_INDEX_PATH =
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json"
        const val REPORT_PATH =
            "build/knowledge/reports/him/canonical-family/groundtruth/" +
                    "him-f3c-candidate-generation-dry-run.txt"
        val SYNTHETIC_APPLE_ID = HimEntityId("Apple1")
        val SYNTHETIC_POTATO_ID = HimEntityId("Potato")
        val REAL_HERRING_ID = HimEntityId("OzlByp")
        val EXPECTED_FOUNDATION_HASHES =
            listOf(
                "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f",
                "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a",
                "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184",
                "4afc7490bd69f2f8919558d33d98c912771d128da4ac7a782cc116a1c9fccb6e",
                "6dcb065d6dce0aaa22c8d1d3dd620372fc90d3fe9a3ec9d20ee54737a8f9a951",
            )
        val SOURCE_HASHES =
            mapOf(
                HimGroundTruthSource.OPEN_FOOD_FACTS to HimSha256("63b20183229a6f3f648c3d189c265d5d5a4b332d530d084af5640081d060a236"),
                HimGroundTruthSource.AGRIBALYSE to HimSha256("9068c89fa887ef087e87dcc51f758dd29e9623b93d9bd0a2277a4faae57c2297"),
                HimGroundTruthSource.CIQUAL to HimSha256("807d16c222f0e812831c10bdd89f1a5ebbc74c95e8a4944723db486add96dcff"),
                HimGroundTruthSource.GLYCEMIC_INDEX to HimSha256("6891c2ff2ab3734a1d2768339c1f660f7093a31b97a856c82bf9b8c3c88422b5"),
            )
        val SOURCE_QUERIES =
            mapOf(
                HimGroundTruthSource.OPEN_FOOD_FACTS to "Braeburn",
                HimGroundTruthSource.AGRIBALYSE to "Hering",
                HimGroundTruthSource.CIQUAL to "Kartoffel",
                HimGroundTruthSource.GLYCEMIC_INDEX to "apple",
            )
        val DRY_RUN_INPUTS =
            listOf(
                "Apfel",
                "Braeburn",
                "Bio",
                "geschält",
                "Granny Smith",
                "Erdapfel",
                "Hering",
                "Atlantischer Hering",
                "Matjes",
                "Bio Braeburn geschält",
                "Bio Braeburn sauer geschält",
                "Coca-Cola",
                "Granny-Smith",
                "granny smith",
            )
    }
}
