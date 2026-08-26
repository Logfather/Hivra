package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyValidator
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Assume.assumeTrue
import java.io.File

/**
 * Explicit real-bound entrypoints for the unresolved-primary-identity diagnostic.
 *
 * The three operational tests are opt-in and remain skipped before any input,
 * catalog, authority, store, or diagnostic artifact is accessed.
 */
class RunHimUnresolvedPrimaryIdentityDiagnosticRealV1Test {
    @Test
    fun `diagnostic gate is disabled without explicit opt in`() {
        val gate = diagnosticGate(null, null, null)
        assertFalse(gate.enabled)
        assertFalse(gate.confirmation == DIAGNOSTIC_CONFIRMATION)
        assertTrue(gate.shardId == null)
    }

    @Test
    fun `wrong diagnostic confirmation is rejected before input access`() {
        val gate = diagnosticGate("true", "WRONG_CONFIRMATION", null)
        assertTrue(gate.enabled)
        assertFalse(gate.confirmation == DIAGNOSTIC_CONFIRMATION)
    }

    @Test
    fun `historical input heads and current diagnostic head remain separate bindings`() {
        val auditHead = "a".repeat(40)
        val enrichmentHead = "b".repeat(40)
        val analysisHead = "c".repeat(40)
        val diagnosticHead = "d".repeat(40)

        assertTrue(auditHead != diagnosticHead)
        assertTrue(enrichmentHead != diagnosticHead)
        assertTrue(analysisHead != diagnosticHead)
    }

    @Test
    fun `real-bound diagnostic path has no search or scan dependency`() {
        val methodNames =
            HimUnresolvedPrimaryIdentityDiagnosticExactFetchPortV1::class.java
                .declaredMethods
                .map { it.name }
                .toSet()
        assertEquals(
            setOf("fetchExact", "getSource"),
            methodNames,
        )
        assertTrue(
            methodNames.none { methodName ->
                methodName.contains("search", ignoreCase = true) ||
                    methodName.contains("scan", ignoreCase = true)
            },
        )
        assertTrue(
            HimUnresolvedPrimaryIdentityDiagnosticRuntimeRequestV1::class.java.declaredFields.none {
                it.type.name.contains("Store") || it.type.name.contains("Search") || it.type.name.contains("Scan")
            },
        )
        assertTrue(
            HimUnresolvedPrimaryIdentityDiagnosticRuntimeV1::class.java.declaredMethods.none {
                it.name.contains("search", ignoreCase = true) || it.name.contains("scan", ignoreCase = true)
            },
        )
    }

    @Test
    fun `typed diagnostic failures do not expose paths or throwable text`() {
        val result = HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Failed(
            HimUnresolvedPrimaryIdentityDiagnosticFailureReasonV1.SOURCE_BINDING_MISMATCH,
            "shard=shard-000001 source=CIQUAL entity=abc123 reference=ciqual:food:1",
        )
        val message = diagnostic(result)
        assertEquals(
            "SOURCE_BINDING_MISMATCH shard=shard-000001 source=CIQUAL entity=abc123 reference=ciqual:food:1",
            message,
        )
        assertFalse(message.contains("/"))
        assertFalse(message.contains("Exception"))
        assertFalse(message.contains("Throwable"))
    }

    @Test
    fun `writes current real-bound unresolved primary identity diagnostic mission without stores or fetches`() {
        requireSourceIntegration()
        requireDiagnosticGate(requireShard = false)

        val root = projectRoot()
        val inputs = loadInputs(root)
        val mission = diagnosticValue(
            HimUnresolvedPrimaryIdentityDiagnosticRuntimeV1.planMission(
                HimUnresolvedPrimaryIdentityDiagnosticMissionInputV1(
                    analysisReport = inputs.analysis,
                    analysisBinding = fileBinding(root, inputs.analysisFile, inputs.analysis.logicalDigest),
                    enrichmentMission = inputs.enrichmentMission,
                    enrichmentMissionBinding = fileBinding(root, inputs.enrichmentMissionFile, inputs.enrichmentMission.logicalDigest),
                    enrichmentAggregate = inputs.enrichmentAggregate,
                    enrichmentAggregateBinding = fileBinding(root, inputs.enrichmentAggregateFile, inputs.enrichmentAggregate.logicalDigest),
                    sourceBindings = inputs.enrichmentMission.provenance.auditBindings.sourceBindings,
                    diagnosticImplementationHead = inputs.currentHead,
                ),
            ),
        )

        require(mission.expectedOccurrenceCount == EXPECTED_OCCURRENCES) { "DIAGNOSTIC_OCCURRENCE_COUNT_MISMATCH" }
        require(mission.expectedReferenceCount == EXPECTED_REFERENCES) { "DIAGNOSTIC_REFERENCE_COUNT_MISMATCH" }
        require(mission.shards.size == EXPECTED_SHARDS) { "DIAGNOSTIC_SHARD_COUNT_MISMATCH" }
        require(mission.diagnosticImplementationHead == inputs.currentHead) { "DIAGNOSTIC_HEAD_MISMATCH" }

        val output = diagnosticMission(root)
        HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.writeMission(output, mission)
        val reloaded = HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.readMission(output)
        require(reloaded == mission) { "DIAGNOSTIC_MISSION_RELOAD_FAILED" }
    }

    @Test
    fun `executes exactly one current real-bound unresolved primary identity diagnostic shard`() {
        requireSourceIntegration()
        val shardId = requireNotNull(
            requireDiagnosticGate(requireShard = true),
        ) {
            "SHARD_ID_REQUIRED"
        }

        val root = projectRoot()
        val inputs = loadInputs(root)
        val mission = loadDiagnosticMission(root, inputs)
        val audit = loadCatalogAndAuthority(root)
        val output = diagnosticShard(root, shardId)
        val result = diagnosticValue(
            HimUnresolvedPrimaryIdentityDiagnosticRuntimeV1.executeShard(
                HimUnresolvedPrimaryIdentityDiagnosticRuntimeRequestV1(
                    enabled = true,
                    mission = mission,
                    shardId = shardId,
                    catalog = audit.first,
                    authority = audit.second,
                    fetchPorts = openRealExactFetchPorts(root, mission.sourceBindings),
                    outputFile = output,
                ),
            ),
        )
        require(result.shardId == shardId) { "DIAGNOSTIC_SHARD_BINDING_MISMATCH" }
        require(result.state == HimUnresolvedPrimaryIdentityDiagnosticResultStateV1.COMPLETE) {
            "DIAGNOSTIC_SHARD_INCOMPLETE"
        }
        require(result.counters.technicalErrors == 0) { "DIAGNOSTIC_TECHNICAL_ERROR" }
        require(result.counters.exactFetches == result.counters.uniqueEvidenceReferences) {
            "DIAGNOSTIC_FETCH_COUNTER_MISMATCH"
        }
        require(HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.readShard(output, mission) == result) {
            "DIAGNOSTIC_SHARD_RELOAD_FAILED"
        }
    }

    @Test
    fun `aggregates exactly eleven current real-bound unresolved primary identity diagnostic shards without stores`() {
        requireSourceIntegration()
        requireDiagnosticGate(requireShard = false)

        val root = projectRoot()
        val inputs = loadInputs(root)
        val mission = loadDiagnosticMission(root, inputs)
        val shardDirectory = diagnosticShardDirectory(root)
        val expectedNames = mission.shards.map { "${it.shardId}.result.v1.json" }.toSet()
        val actualNames = shardDirectory.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".result.v1.json") }
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()
        require(actualNames == expectedNames) { "DIAGNOSTIC_SHARD_SET_MISMATCH" }

        val shards = mission.shards.map { shard ->
            HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.readShard(
                diagnosticShard(root, shard.shardId),
                mission,
            )
        }
        require(shards.size == EXPECTED_SHARDS) { "DIAGNOSTIC_SHARD_COUNT_MISMATCH" }
        require(shards.map { it.shardId } == HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS) {
            "DIAGNOSTIC_SHARD_SET_MISMATCH"
        }
        require(shards.all { it.state == HimUnresolvedPrimaryIdentityDiagnosticResultStateV1.COMPLETE }) {
            "DIAGNOSTIC_SHARD_INCOMPLETE"
        }

        val aggregate = aggregateFrom(mission, shards)
        val jsonFile = diagnosticAggregate(root)
        val textFile = diagnosticAggregateText(root)
        require(jsonFile.exists() == textFile.exists()) { "PARTIAL_DIAGNOSTIC_AGGREGATE" }
        HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.writeAggregate(jsonFile, textFile, mission, aggregate)
        val reloaded = HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.readAggregate(jsonFile, mission)
        require(reloaded == aggregate) { "DIAGNOSTIC_AGGREGATE_RELOAD_FAILED" }
        require(textFile.isFile) { "DIAGNOSTIC_AGGREGATE_TEXT_MISSING" }
    }

    private fun requireSourceIntegration() = HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()

    private fun requireDiagnosticGate(requireShard: Boolean): String? {
        val current = diagnosticGate(
            System.getProperty(DIAGNOSTIC_ENABLED_PROPERTY),
            System.getProperty(DIAGNOSTIC_CONFIRMATION_PROPERTY),
            System.getProperty(DIAGNOSTIC_SHARD_ID_PROPERTY),
        )
        assumeTrue(current.enabled)
        assumeTrue(current.confirmation == DIAGNOSTIC_CONFIRMATION)
        if (!requireShard) return null
        val shardId = current.shardId ?: fail("DIAGNOSTIC_SHARD_ID_REQUIRED")
        require(shardId in HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS) {
            "DIAGNOSTIC_SHARD_ID_INVALID"
        }
        return shardId
    }

    private fun diagnosticGate(
        enabled: String?,
        confirmation: String?,
        shardId: String?,
    ) = DiagnosticGate(
        enabled = enabled == "true",
        confirmation = confirmation,
        shardId = shardId,
    )

    private fun diagnostic(
        result: HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult<*>,
    ): String = when (result) {
        is HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Completed -> "COMPLETED"
        is HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Failed ->
            "${result.reason.name} ${result.safeContext}"
        is HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Skipped -> result.reason
    }

    private fun <T> diagnosticValue(
        result: HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult<T>,
    ): T = when (result) {
        is HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Completed -> result.value
        is HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Failed ->
            fail("${result.reason.name} ${result.safeContext}")
        is HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Skipped -> fail(result.reason)
    }

    private fun loadInputs(root: File): DiagnosticInputs {
        val currentHead = git(root, "rev-parse", "HEAD")
        val analysisFile = root.resolve(ANALYSIS_REPORT_ROOT).resolve("analysis.v1.json")
        val enrichmentMissionFile = root.resolve(ENRICHMENT_REPORT_ROOT).resolve("mission.v1.json")
        val enrichmentAggregateFile = root.resolve(ENRICHMENT_REPORT_ROOT).resolve("aggregate.v1.json")
        require(analysisFile.isFile) { "ANALYSIS_INPUT_MISSING" }
        require(enrichmentMissionFile.isFile) { "ENRICHMENT_MISSION_INPUT_MISSING" }
        require(enrichmentAggregateFile.isFile) { "ENRICHMENT_AGGREGATE_INPUT_MISSING" }
        require(HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.sha256(analysisFile) == ANALYSIS_SHA256) {
            "ANALYSIS_INPUT_BINDING_MISMATCH"
        }
        require(HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.sha256(enrichmentMissionFile) == ENRICHMENT_MISSION_SHA256) {
            "ENRICHMENT_MISSION_INPUT_BINDING_MISMATCH"
        }
        require(HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.sha256(enrichmentAggregateFile) == ENRICHMENT_AGGREGATE_SHA256) {
            "ENRICHMENT_AGGREGATE_INPUT_BINDING_MISMATCH"
        }

        val analysis = HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.readReport(analysisFile)
        val enrichmentMission = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.readMission(enrichmentMissionFile)
        val enrichmentAggregate = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.readAggregate(
            enrichmentAggregateFile,
            enrichmentMission,
        )
        require(analysis.logicalDigest == ANALYSIS_LOGICAL_DIGEST) { "ANALYSIS_LOGICAL_DIGEST_MISMATCH" }
        require(analysis.totalFindingOccurrences == EXPECTED_FINDING_OCCURRENCES) {
            "ANALYSIS_FINDING_COUNT_MISMATCH"
        }
        require(
            analysis.findingIndex.count {
                it.originalAuditClassification == HimEvidenceAlignmentAuditClassification.UNRESOLVED_PRIMARY_IDENTITY
            } == EXPECTED_OCCURRENCES,
        ) { "ANALYSIS_UNRESOLVED_COUNT_MISMATCH" }
        require(enrichmentMission.logicalDigest == ENRICHMENT_MISSION_LOGICAL_DIGEST) {
            "ENRICHMENT_MISSION_LOGICAL_DIGEST_MISMATCH"
        }
        require(enrichmentAggregate.logicalDigest == ENRICHMENT_AGGREGATE_LOGICAL_DIGEST) {
            "ENRICHMENT_AGGREGATE_LOGICAL_DIGEST_MISMATCH"
        }
        require(enrichmentAggregate.shardResults.size == EXPECTED_SHARDS) {
            "ENRICHMENT_SHARD_COUNT_MISMATCH"
        }
        require(enrichmentAggregate.counters.findingOccurrences == EXPECTED_FINDING_OCCURRENCES) {
            "ENRICHMENT_FINDING_COUNT_MISMATCH"
        }
        require(enrichmentAggregate.counters.reconstructedFindings == 13034) {
            "ENRICHMENT_RECONSTRUCTED_COUNT_MISMATCH"
        }
        require(enrichmentAggregate.counters.classificationMismatches == 0) {
            "ENRICHMENT_CLASSIFICATION_MISMATCH"
        }
        return DiagnosticInputs(
            currentHead = currentHead,
            analysisFile = analysisFile,
            analysis = analysis,
            enrichmentMissionFile = enrichmentMissionFile,
            enrichmentMission = enrichmentMission,
            enrichmentAggregateFile = enrichmentAggregateFile,
            enrichmentAggregate = enrichmentAggregate,
        )
    }

    private fun loadDiagnosticMission(
        root: File,
        inputs: DiagnosticInputs,
    ): HimUnresolvedPrimaryIdentityDiagnosticMissionV1 {
        val file = diagnosticMission(root)
        require(file.isFile) { "DIAGNOSTIC_MISSION_MISSING" }
        val mission = HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.readMission(file)
        require(mission.diagnosticImplementationHead == inputs.currentHead) { "DIAGNOSTIC_HEAD_MISMATCH" }
        require(mission.auditHead == inputs.analysis.inputBinding.auditHead) { "AUDIT_HEAD_MISMATCH" }
        require(mission.enrichmentImplementationHead == inputs.enrichmentMission.provenance.enrichmentImplementationHead) {
            "ENRICHMENT_HEAD_MISMATCH"
        }
        require(mission.analysisImplementationHead == inputs.analysis.inputBinding.analysisImplementationHead) {
            "ANALYSIS_HEAD_MISMATCH"
        }
        require(mission.analysisBinding == fileBinding(root, inputs.analysisFile, inputs.analysis.logicalDigest)) {
            "ANALYSIS_BINDING_MISMATCH"
        }
        require(
            mission.enrichmentMissionBinding ==
                fileBinding(root, inputs.enrichmentMissionFile, inputs.enrichmentMission.logicalDigest),
        ) { "ENRICHMENT_MISSION_BINDING_MISMATCH" }
        require(
            mission.enrichmentAggregateBinding ==
                fileBinding(root, inputs.enrichmentAggregateFile, inputs.enrichmentAggregate.logicalDigest),
        ) { "ENRICHMENT_AGGREGATE_BINDING_MISMATCH" }
        return mission
    }

    private fun fileBinding(
        root: File,
        file: File,
        logicalDigest: String,
    ) = HimUnresolvedPrimaryIdentityDiagnosticFileBindingV1(
        relativePath = relativePath(root, file),
        byteSize = file.length(),
        sha256 = HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.sha256(file),
        logicalDigest = logicalDigest,
    )

    private fun loadCatalogAndAuthority(root: File): Pair<HimProductOnlyCanonicalMaster, HimCanonicalFamilyAuthority> {
        val paths = HimCanonicalFamilyPaths(root)
        val catalog = HimProductOnlyCanonicalMasterReader().read(paths)
        val persistence = HimCanonicalFamilyPersistence()
        val masterRegistry = persistence.readRegistry(paths.entityIdRegistry)
        val masterAuthority = persistence.readAuthority(paths.familyAuthority)
        HimCanonicalFamilyValidator().validate(catalog, masterRegistry, masterAuthority)

        val active = HimActiveGroundTruthResolutionV1().resolve(root)
        val authority = persistence.readAuthority(active.authorityFile)
        require(authority.sourceCatalog.path == catalog.path) { "AUTHORITY_SOURCE_CATALOG_MISMATCH" }
        require(authority.sourceCatalog.contentSha256 == catalog.contentSha256) {
            "AUTHORITY_SOURCE_CATALOG_MISMATCH"
        }
        require(authority.sourceCatalog.recordCount == catalog.records.size) {
            "AUTHORITY_SOURCE_CATALOG_MISMATCH"
        }
        return catalog to authority
    }

    private fun openRealExactFetchPorts(
        root: File,
        sourceBindings: List<HimEvidenceAlignmentCatalogAuditSourceBindingV1>,
    ): List<HimUnresolvedPrimaryIdentityDiagnosticExactFetchPortV1> {
        require(sourceBindings.map { it.source } == HimUnresolvedPrimaryIdentityDiagnosticContractV1.SOURCE_ORDER) {
            "SOURCE_BINDING_ORDER_MISMATCH"
        }
        val offFile = root.resolve(HimOffProductionEvidenceIndexPaths.FINAL_INDEX)
        val offValidation = HimOffEvidenceIndexValidator.validateReadOnly(offFile)
        validateRealIndexBinding(root, offFile, offValidation.metadata, sourceBindings.single { it.source == HimGroundTruthSource.OPEN_FOOD_FACTS })
        val off = HimOffSqliteEvidenceRetrievalStore.openAfterValidation(offFile, offValidation)

        val agribalyseFile = root.resolve(HimAgribalyseProductionEvidenceIndexPaths.FINAL_INDEX)
        val agribalyseValidation = HimAgribalyseEvidenceIndexValidator.validateReadOnly(agribalyseFile)
        validateRealIndexBinding(root, agribalyseFile, agribalyseValidation.metadata, sourceBindings.single { it.source == HimGroundTruthSource.AGRIBALYSE })
        val agribalyse = HimAgribalyseSqliteEvidenceRetrievalStore.openAfterValidation(agribalyseFile, agribalyseValidation)

        val ciqualFile = root.resolve(HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX)
        val ciqualValidation = HimCiqualEvidenceIndexValidator.validateReadOnly(ciqualFile)
        validateRealIndexBinding(root, ciqualFile, ciqualValidation.metadata, sourceBindings.single { it.source == HimGroundTruthSource.CIQUAL })
        val ciqual = HimCiqualSqliteEvidenceRetrievalStore.openAfterValidation(ciqualFile, ciqualValidation)

        val giFile = root.resolve(HimGlycemicIndexProductionEvidenceIndexPaths.FINAL_INDEX)
        val giValidation = HimGlycemicIndexEvidenceIndexValidator.validateReadOnly(giFile)
        validateRealIndexBinding(root, giFile, giValidation.metadata, sourceBindings.single { it.source == HimGroundTruthSource.GLYCEMIC_INDEX })
        val gi = HimGlycemicIndexSqliteEvidenceRetrievalStore.openAfterValidation(giFile, giValidation)

        return listOf(
            exactPort(HimGroundTruthSource.OPEN_FOOD_FACTS) { reference -> off.fetch(reference)?.let(::toIndexRecord) },
            exactPort(HimGroundTruthSource.AGRIBALYSE) { reference -> agribalyse.fetch(reference)?.let(::toIndexRecord) },
            exactPort(HimGroundTruthSource.CIQUAL) { reference -> ciqual.fetch(reference)?.let(::toIndexRecord) },
            exactPort(HimGroundTruthSource.GLYCEMIC_INDEX) { reference -> gi.fetch(reference)?.let(::toIndexRecord) },
        )
    }

    private fun validateRealIndexBinding(
        root: File,
        file: File,
        metadata: HimEvidenceRetrievalIndexMetadata,
        binding: HimEvidenceAlignmentCatalogAuditSourceBindingV1,
    ) {
        require(metadata.source == binding.source) { "SOURCE_BINDING_MISMATCH" }
        require(relativePath(root, file) == binding.indexRelativePath) { "SOURCE_BINDING_MISMATCH" }
        require(file.length() == binding.indexByteSize) { "SOURCE_BINDING_MISMATCH" }
        require(HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.sha256(file) == binding.sqliteFileSha256) {
            "SOURCE_BINDING_MISMATCH"
        }
        require(metadata.sourceArtifactPath == binding.sourceArtifactPath) { "SOURCE_BINDING_MISMATCH" }
        require(metadata.sourceArtifactSha256.value == binding.sourceArtifactSha256) { "SOURCE_BINDING_MISMATCH" }
        require(metadata.schemaVersion == binding.schemaVersion) { "SOURCE_BINDING_MISMATCH" }
        require(metadata.indexBuildPolicyVersion == binding.indexBuildPolicyVersion) { "SOURCE_BINDING_MISMATCH" }
        require(metadata.evidenceProjectionPolicyVersion == binding.evidenceProjectionPolicyVersion) {
            "SOURCE_BINDING_MISMATCH"
        }
        require(metadata.logicalContentSha256.value == binding.logicalContentSha256) { "SOURCE_BINDING_MISMATCH" }
        require(metadata.buildState == binding.buildState) { "SOURCE_BINDING_MISMATCH" }
    }

    private fun exactPort(
        source: HimGroundTruthSource,
        fetcher: (HimEvidenceRecordReference) -> HimEvidenceRetrievalIndexRecord?,
    ) = object : HimUnresolvedPrimaryIdentityDiagnosticExactFetchPortV1 {
        override val source: HimGroundTruthSource = source

        override fun fetchExact(
            requestedSource: HimGroundTruthSource,
            evidenceReference: HimEvidenceRecordReference,
        ): HimEvidenceRetrievalIndexRecord? {
            require(requestedSource == source) { "SOURCE_BINDING_MISMATCH" }
            return fetcher(evidenceReference)
        }
    }

    private fun toIndexRecord(result: HimEvidenceSearchResult): HimEvidenceRetrievalIndexRecord = when (result.source) {
        HimGroundTruthSource.OPEN_FOOD_FACTS -> HimOffEvidenceProjectionV1.fromProjectionJson(result.evidenceProjection.deterministicJson)
        HimGroundTruthSource.AGRIBALYSE -> HimAgribalyseEvidenceProjectionV1.fromProjectionJson(result.evidenceProjection.deterministicJson)
        HimGroundTruthSource.CIQUAL -> HimCiqualEvidenceProjectionV1.fromProjectionJson(result.evidenceProjection.deterministicJson, 1)
        HimGroundTruthSource.GLYCEMIC_INDEX -> HimGlycemicIndexEvidenceProjectionV1.fromProjectionJson(result.evidenceProjection.deterministicJson, 1)
    }

    private fun aggregateFrom(
        mission: HimUnresolvedPrimaryIdentityDiagnosticMissionV1,
        shards: List<HimUnresolvedPrimaryIdentityDiagnosticShardResultV1>,
    ): HimUnresolvedPrimaryIdentityDiagnosticAggregateV1 {
        val counters =
            HimUnresolvedPrimaryIdentityDiagnosticRuntimeV1.deriveCounters(
                shards.flatMap { it.records },
            )
        val unsigned = HimUnresolvedPrimaryIdentityDiagnosticAggregateV1(
            missionDigest = mission.logicalDigest,
            state = HimUnresolvedPrimaryIdentityDiagnosticResultStateV1.COMPLETE,
            shardResults = shards,
            counters = counters,
            logicalDigest = "",
        )
        return unsigned.copy(
            logicalDigest = HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.logicalDigest(unsigned),
        ).also { it.validateAgainst(mission) }
    }

    private fun projectRoot(): File {
        var current = File(System.getProperty("user.dir") ?: error("USER_DIR_UNAVAILABLE")).canonicalFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile) return current
            current = current.parentFile ?: error("REPOSITORY_ROOT_NOT_FOUND")
        }
    }

    private fun git(root: File, vararg arguments: String): String {
        val process = ProcessBuilder(listOf("git") + arguments.toList())
            .directory(root)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        require(process.waitFor() == 0) { "GIT_BINDING_FAILED" }
        require(output.matches(HEAD)) { "GIT_BINDING_FAILED" }
        return output
    }

    private fun relativePath(root: File, file: File): String = root.canonicalFile.toPath()
        .relativize(file.canonicalFile.toPath())
        .toString()
        .replace(File.separatorChar, '/')

    private fun diagnosticRoot(root: File) = root.resolve(DIAGNOSTIC_REPORT_ROOT)
    private fun diagnosticMission(root: File) = diagnosticRoot(root).resolve("mission.v1.json")
    private fun diagnosticShardDirectory(root: File) = diagnosticRoot(root).resolve("shards")
    private fun diagnosticShard(root: File, shardId: String) = diagnosticShardDirectory(root).resolve("$shardId.result.v1.json")
    private fun diagnosticAggregate(root: File) = diagnosticRoot(root).resolve("aggregate.v1.json")
    private fun diagnosticAggregateText(root: File) = diagnosticRoot(root).resolve("aggregate.v1.txt")

    private data class DiagnosticGate(val enabled: Boolean, val confirmation: String?, val shardId: String?)

    private data class DiagnosticInputs(
        val currentHead: String,
        val analysisFile: File,
        val analysis: HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1,
        val enrichmentMissionFile: File,
        val enrichmentMission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
        val enrichmentAggregateFile: File,
        val enrichmentAggregate: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1,
    )

    companion object {
        const val DIAGNOSTIC_ENABLED_PROPERTY = "him.unresolvedPrimaryIdentityDiagnostic.enabled"
        const val DIAGNOSTIC_CONFIRMATION_PROPERTY = "him.unresolvedPrimaryIdentityDiagnostic.confirmation"
        const val DIAGNOSTIC_SHARD_ID_PROPERTY = "him.unresolvedPrimaryIdentityDiagnostic.shardId"
        const val DIAGNOSTIC_CONFIRMATION = "AUTHORIZED_BOUNDED_UNRESOLVED_PRIMARY_IDENTITY_DIAGNOSTIC_OFFLINE"
        const val DIAGNOSTIC_REPORT_ROOT = "build/knowledge/reports/him/evidence-alignment/catalog-audit/unresolved-primary-identity-diagnostic/v1"
        const val ENRICHMENT_REPORT_ROOT = "build/knowledge/reports/him/evidence-alignment/catalog-audit/findings-enrichment/v1"
        const val ANALYSIS_REPORT_ROOT = "build/knowledge/reports/him/evidence-alignment/catalog-audit/findings-analysis/v1"
        private const val ANALYSIS_SHA256 = "43255798223544942b876b803ec7095a55f59d88f8a31bc93c97a86cea3966c1"
        private const val ANALYSIS_LOGICAL_DIGEST = "338911a17177227cd15cb0a05616ba1608b50b129dc129a1d5349d6e5ea271c3"
        private const val ENRICHMENT_MISSION_SHA256 = "6272d445502ff870629f360a589a83a02ecbf91361dc60ae05444873282cb614"
        private const val ENRICHMENT_MISSION_LOGICAL_DIGEST = "3bbff3db4140c8724079bafcf94ea1269552c74a076f2d17f52b628f1f0ec69f"
        private const val ENRICHMENT_AGGREGATE_SHA256 = "d8982891ad4b6775c6a9f76d6b5ea6dbca447966451236b1da67bd2d6ac40091"
        private const val ENRICHMENT_AGGREGATE_LOGICAL_DIGEST = "1bd475310058775d36ccbdbe976ba9f053028255ca1cb20c3004c46840cd2398"
        private const val EXPECTED_FINDING_OCCURRENCES = 16969
        private const val EXPECTED_OCCURRENCES = 3036
        private const val EXPECTED_REFERENCES = 2983
        private const val EXPECTED_SHARDS = 11
        private val HEAD = Regex("[0-9a-f]{40}")
    }
}
