package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest

/**
 * Point-10/11 evaluation dataset authority. It reads the immutable V1 and
 * Expansion V2 authorities, combines them additively, and persists only the
 * new V2 corpus, partition, and readiness records.
 */
object HimP1EvaluationDatasetV2 {
    const val CONTRACT_ID = "HIM_P1_EVALUATION_DATASET_V2"
    const val VERSION = "2"
    const val CORPUS_STATE = "P1_EVALUATION_CORPUS_V2_PERSISTED"
    const val PARTITION_STATE = "P1_EVALUATION_PARTITION_V2_PERSISTED"
    const val READINESS_CONTRACT_ID = "HIM_P1_EVALUATION_DATASET_V2_READINESS"
    const val READY_STATE = "READY"
    const val BLOCKED_STATE = "BLOCKED"
    const val DURABLE_ROOT = "data/knowledge/him/training/evaluation/v2/p1-evaluation-dataset-v2"
    const val CORPUS_FILE = "corpus.v2.json"
    const val PARTITION_FILE = "partition.v2.json"
    const val READINESS_FILE = "evaluation-readiness.v2.json"
    const val CORPUS_DIGEST = "HIM_P1_EVALUATION_CORPUS_V2_DIGEST"
    const val PARTITION_DIGEST = "HIM_P1_EVALUATION_PARTITION_V2_DIGEST"
    const val POLICY_VERSION = "HIM_TRAINING_PARTITION_POLICY_V1"
    const val PARTITION_KEY = "familyGroupReference"
    const val HASH = "SHA-256"
    const val BUCKET_MODULUS = 10_000
    const val TRAIN_FIRST = 0
    const val TRAIN_LAST = 7_999
    const val VALIDATION_FIRST = 8_000
    const val VALIDATION_LAST = 8_999
    const val HOLDOUT_FIRST = 9_000
    const val HOLDOUT_LAST = 9_999
    const val EXPECTED_EXAMPLE_COUNT = 40
    const val EXPECTED_FAMILY_COUNT = 15
    const val EXPECTED_V1_CORPUS_DIGEST =
        "61fe8ce3d019b89e573020183f9baad9c51a8613ac392a162e103cd6da84b597"
    const val EXPECTED_V1_PARTITION_DIGEST =
        "62dc7471284d0758f2a3ab270efad6c4a9f255b69b67355a268cd669fdf59257"

    private const val V1_SNAPSHOT_PATH =
        "data/knowledge/him/training/corpora/v1/point10-companion-snapshots/61fe8ce3d019b89e573020183f9baad9c51a8613ac392a162e103cd6da84b597.point10-snapshot.v1.json"
    private const val V1_PARTITION_PATH =
        "data/knowledge/him/training/partitions/v1/point10/62dc7471284d0758f2a3ab270efad6c4a9f255b69b67355a268cd669fdf59257.partition.v1.json"
    private const val V1_POSITIVE_ROOT = "data/knowledge/him/training/positive-examples/v1"
    private const val V1_NEGATIVE_ROOT = "data/knowledge/him/training/negative-examples/v1"
    private const val EXPANSION_ROOT =
        "data/knowledge/him/training/expansions/v2/p1-human-validation-expansion-v2-batch-1-confirmation-v1"
    private const val EXPANSION_AUTHORITY = "$EXPANSION_ROOT/human-authorized-structured-semantics.v2.json"
    private const val EXPANSION_EXAMPLES = "$EXPANSION_ROOT/training-examples.v2.json"
    private const val EXPANSION_FAMILIES = "$EXPANSION_ROOT/family-groups.v2.json"
    private const val EXPANSION_INVENTORY = "$EXPANSION_ROOT/expansion-inventory.v2.json"
    private val gson = GsonBuilder().disableHtmlEscaping().serializeNulls().create()
    private val SHA256 = Regex("[0-9a-f]{64}")
    private val EXAMPLE_REFERENCE = Regex("(?:example|negative-example):v1:[0-9a-f]{64}")
    private val FAMILY_REFERENCE = Regex("family:v1:canonical:[0-9A-Za-z]{6}")
    private val PARTITION_NAMES = setOf("TRAIN", "VALIDATION", "HOLDOUT")
    private val TARGET_KINDS = setOf("EXISTING_CANONICAL", "IDENTITY", "VARIANT", "ALIAS", "NEW_CANONICAL")
    private val RELATIONS = setOf(
        "IDENTITY_OF",
        "VARIANT_OF",
        "PROCESSING_FORM_OF",
        "PREPARATION_STATE_OF",
        "PRODUCT_FORM_OF",
    )

    data class ExampleV2(
        val exampleReference: String,
        val familyGroupReference: String,
        val candidateId: String,
        val candidateName: String,
        val targetKind: String,
        val relation: String,
        val targetCanonicalId: String,
        val originAuthority: String,
        val authorityStatus: String,
        val humanAuthorityReference: String?,
        val source: String,
        val sourceArtifactSha256: String?,
        val evidenceRecordId: String?,
        val evidenceReferenceId: String?,
        val semanticLabel: String?,
        val variantLabel: String?,
        val processingForm: String?,
        val preparationState: String?,
        val productForm: String?,
    )

    data class FamilyV2(
        val familyGroupReference: String,
        val candidateId: String,
        val candidateName: String,
        val memberExampleReferences: List<String>,
        val memberCount: Int,
        val relationDistribution: Map<String, Int>,
        val authorityReferences: List<String>,
    )

    data class CorpusV2(
        val contractId: String,
        val version: String,
        val state: String,
        val corpusReference: String,
        val originalV1CorpusDigest: String,
        val originalV1PartitionDigest: String,
        val originalAuthorityReferences: List<String>,
        val expansionAuthorityReferences: List<String>,
        val humanAuthorityLineage: List<String>,
        val examples: List<ExampleV2>,
        val familyGroups: List<FamilyV2>,
        val logicalDigest: String,
    )

    data class FamilyAssignmentV2(
        val familyGroupReference: String,
        val candidateId: String,
        val memberCount: Int,
        val hash: String,
        val firstEightHex: String,
        val unsignedValue: Long,
        val bucket: Int,
        val partition: String,
    )

    data class ExampleAssignmentV2(
        val exampleReference: String,
        val familyGroupReference: String,
        val candidateId: String,
        val targetKind: String,
        val relation: String,
        val partition: String,
    )

    data class PartitionV2(
        val contractId: String,
        val version: String,
        val state: String,
        val partitionReference: String,
        val corpusReference: String,
        val corpusLogicalDigest: String,
        val policyVersion: String,
        val partitionKey: String,
        val hash: String,
        val bucketModulus: Int,
        val trainRange: String,
        val validationRange: String,
        val holdoutRange: String,
        val rngUsed: Boolean,
        val thresholdChanged: Boolean,
        val familyAssignments: List<FamilyAssignmentV2>,
        val exampleAssignments: List<ExampleAssignmentV2>,
        val trainFamilyCount: Int,
        val validationFamilyCount: Int,
        val holdoutFamilyCount: Int,
        val trainExampleCount: Int,
        val validationExampleCount: Int,
        val holdoutExampleCount: Int,
        val familyLeakageCount: Int,
        val exampleLeakageCount: Int,
        val crossPartitionDuplicateExampleCount: Int,
        val logicalDigest: String,
    )

    data class EvaluationReadinessV2(
        val contractId: String,
        val version: String,
        val state: String,
        val corpusReference: String,
        val corpusLogicalDigest: String,
        val partitionReference: String,
        val partitionLogicalDigest: String,
        val totalExampleCount: Int,
        val totalFamilyCount: Int,
        val trainFamilyCount: Int,
        val validationFamilyCount: Int,
        val holdoutFamilyCount: Int,
        val trainExampleCount: Int,
        val validationExampleCount: Int,
        val holdoutExampleCount: Int,
        val familyLeakageCount: Int,
        val exampleLeakageCount: Int,
        val partitionPolicyVersion: String,
        val humanAuthorityLineage: List<String>,
        val expansionLineage: List<String>,
        val logicalDigest: String,
    )

    data class PersistedArtifact(val path: String, val byteSize: Long, val sha256: String)

    data class Materialization(
        val corpus: CorpusV2,
        val partition: PartitionV2,
        val readiness: EvaluationReadinessV2,
        val artifacts: List<PersistedArtifact>,
    )

    fun materialize(repositoryRoot: File): Materialization {
        val corpus = buildCorpus(repositoryRoot)
        validateCorpus(corpus)
        val partition = buildPartition(corpus)
        validatePartition(corpus, partition)
        val readiness = buildReadiness(corpus, partition)
        validateReadiness(corpus, partition, readiness)
        val root = repositoryRoot.resolve(DURABLE_ROOT)
        val persisted = listOf(
            persistImmutable(root.resolve(CORPUS_FILE), serialize(corpus)),
            persistImmutable(root.resolve(PARTITION_FILE), serialize(partition)),
            persistImmutable(root.resolve(READINESS_FILE), serialize(readiness)),
        )
        return Materialization(corpus, partition, readiness, persisted)
    }

    fun buildCorpus(repositoryRoot: File): CorpusV2 {
        val v1 = loadV1(repositoryRoot)
        val expansion = loadExpansion(repositoryRoot)
        val examples = (v1.first + expansion.first).sortedBy { it.exampleReference }
        val families = (v1.second + expansion.second).sortedBy { it.familyGroupReference }
        require(examples.size == EXPECTED_EXAMPLE_COUNT) { "CORPUS_EXAMPLE_COUNT" }
        require(families.size == EXPECTED_FAMILY_COUNT) { "CORPUS_FAMILY_COUNT" }
        require(examples.map { it.exampleReference }.distinct().size == examples.size) { "DUPLICATE_EXAMPLE" }
        require(families.map { it.familyGroupReference }.distinct().size == families.size) { "DUPLICATE_FAMILY" }
        val unsigned = CorpusV2(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = CORPUS_STATE,
            corpusReference = "",
            originalV1CorpusDigest = EXPECTED_V1_CORPUS_DIGEST,
            originalV1PartitionDigest = EXPECTED_V1_PARTITION_DIGEST,
            originalAuthorityReferences = listOf(V1_SNAPSHOT_PATH, V1_PARTITION_PATH, V1_POSITIVE_ROOT, V1_NEGATIVE_ROOT),
            expansionAuthorityReferences = listOf(EXPANSION_AUTHORITY, EXPANSION_EXAMPLES, EXPANSION_FAMILIES, EXPANSION_INVENTORY),
            humanAuthorityLineage = listOf("P1_HUMAN_SPECIFICATION", "HUMAN_CONFIRMED_AI_ENRICHMENT"),
            examples = examples,
            familyGroups = families,
            logicalDigest = "",
        )
        val digest = corpusDigest(unsigned)
        return unsigned.copy(
            corpusReference = "training-corpus-evaluation:v2:$digest",
            logicalDigest = digest,
        )
    }

    fun buildPartition(corpus: CorpusV2): PartitionV2 {
        validateCorpus(corpus)
        val families = corpus.familyGroups.map { family ->
            val hash = sha256(family.familyGroupReference)
            val first = hash.substring(0, 8)
            val unsigned = first.toLong(16)
            FamilyAssignmentV2(
                familyGroupReference = family.familyGroupReference,
                candidateId = family.candidateId,
                memberCount = family.memberCount,
                hash = hash,
                firstEightHex = first,
                unsignedValue = unsigned,
                bucket = (unsigned % BUCKET_MODULUS).toInt(),
                partition = partitionFor((unsigned % BUCKET_MODULUS).toInt()),
            )
        }.sortedBy { it.familyGroupReference }
        val partitionByFamily = families.associate { it.familyGroupReference to it.partition }
        val examples = corpus.examples.map { example ->
            ExampleAssignmentV2(
                exampleReference = example.exampleReference,
                familyGroupReference = example.familyGroupReference,
                candidateId = example.candidateId,
                targetKind = example.targetKind,
                relation = example.relation,
                partition = requireNotNull(partitionByFamily[example.familyGroupReference]),
            )
        }.sortedBy { it.exampleReference }
        val unsigned = PartitionV2(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = PARTITION_STATE,
            partitionReference = "",
            corpusReference = corpus.corpusReference,
            corpusLogicalDigest = corpus.logicalDigest,
            policyVersion = POLICY_VERSION,
            partitionKey = PARTITION_KEY,
            hash = HASH,
            bucketModulus = BUCKET_MODULUS,
            trainRange = "$TRAIN_FIRST-$TRAIN_LAST",
            validationRange = "$VALIDATION_FIRST-$VALIDATION_LAST",
            holdoutRange = "$HOLDOUT_FIRST-$HOLDOUT_LAST",
            rngUsed = false,
            thresholdChanged = false,
            familyAssignments = families,
            exampleAssignments = examples,
            trainFamilyCount = families.count { it.partition == "TRAIN" },
            validationFamilyCount = families.count { it.partition == "VALIDATION" },
            holdoutFamilyCount = families.count { it.partition == "HOLDOUT" },
            trainExampleCount = examples.count { it.partition == "TRAIN" },
            validationExampleCount = examples.count { it.partition == "VALIDATION" },
            holdoutExampleCount = examples.count { it.partition == "HOLDOUT" },
            familyLeakageCount = 0,
            exampleLeakageCount = 0,
            crossPartitionDuplicateExampleCount = 0,
            logicalDigest = "",
        )
        val digest = partitionDigest(unsigned)
        return unsigned.copy(
            partitionReference = "training-partition-evaluation:v2:$digest",
            logicalDigest = digest,
        )
    }

    fun buildReadiness(corpus: CorpusV2, partition: PartitionV2): EvaluationReadinessV2 {
        val ready = partition.trainFamilyCount > 0 &&
            partition.validationFamilyCount > 0 &&
            partition.holdoutFamilyCount > 0 &&
            partition.trainExampleCount > 0 &&
            partition.validationExampleCount > 0 &&
            partition.holdoutExampleCount > 0 &&
            partition.familyLeakageCount == 0 &&
            partition.exampleLeakageCount == 0
        val unsigned = EvaluationReadinessV2(
            contractId = READINESS_CONTRACT_ID,
            version = VERSION,
            state = if (ready) READY_STATE else BLOCKED_STATE,
            corpusReference = corpus.corpusReference,
            corpusLogicalDigest = corpus.logicalDigest,
            partitionReference = partition.partitionReference,
            partitionLogicalDigest = partition.logicalDigest,
            totalExampleCount = corpus.examples.size,
            totalFamilyCount = corpus.familyGroups.size,
            trainFamilyCount = partition.trainFamilyCount,
            validationFamilyCount = partition.validationFamilyCount,
            holdoutFamilyCount = partition.holdoutFamilyCount,
            trainExampleCount = partition.trainExampleCount,
            validationExampleCount = partition.validationExampleCount,
            holdoutExampleCount = partition.holdoutExampleCount,
            familyLeakageCount = partition.familyLeakageCount,
            exampleLeakageCount = partition.exampleLeakageCount,
            partitionPolicyVersion = partition.policyVersion,
            humanAuthorityLineage = corpus.humanAuthorityLineage,
            expansionLineage = corpus.expansionAuthorityReferences,
            logicalDigest = "",
        )
        return unsigned.copy(logicalDigest = readinessDigest(unsigned))
    }

    fun validateCorpus(corpus: CorpusV2) {
        require(corpus.contractId == CONTRACT_ID)
        require(corpus.version == VERSION)
        require(corpus.state == CORPUS_STATE)
        require(corpus.examples.size == EXPECTED_EXAMPLE_COUNT)
        require(corpus.familyGroups.size == EXPECTED_FAMILY_COUNT)
        require(corpus.originalV1CorpusDigest == EXPECTED_V1_CORPUS_DIGEST)
        require(corpus.originalV1PartitionDigest == EXPECTED_V1_PARTITION_DIGEST)
        require(corpus.originalAuthorityReferences == listOf(V1_SNAPSHOT_PATH, V1_PARTITION_PATH, V1_POSITIVE_ROOT, V1_NEGATIVE_ROOT))
        require(corpus.expansionAuthorityReferences == listOf(EXPANSION_AUTHORITY, EXPANSION_EXAMPLES, EXPANSION_FAMILIES, EXPANSION_INVENTORY))
        require(corpus.examples == corpus.examples.sortedBy { it.exampleReference })
        require(corpus.familyGroups == corpus.familyGroups.sortedBy { it.familyGroupReference })
        require(corpus.examples.map { it.exampleReference }.distinct().size == corpus.examples.size)
        require(corpus.familyGroups.map { it.familyGroupReference }.distinct().size == corpus.familyGroups.size)
        require(corpus.examples.all { example ->
            EXAMPLE_REFERENCE.matches(example.exampleReference) &&
                FAMILY_REFERENCE.matches(example.familyGroupReference) &&
                example.candidateId.isNotBlank() &&
                example.candidateName.isNotBlank() &&
                example.targetCanonicalId.isNotBlank() &&
                example.targetKind in TARGET_KINDS &&
                example.relation in RELATIONS &&
                example.authorityStatus == "HUMAN_AUTHORIZED" &&
                example.originAuthority in setOf("ORIGINAL_V1_HUMAN_AUTHORITY", "HUMAN_CONFIRMED_AI_ENRICHMENT")
        })
        require(corpus.familyGroups.all { family ->
            FAMILY_REFERENCE.matches(family.familyGroupReference) &&
                family.memberCount > 0 &&
                family.memberCount == family.memberExampleReferences.size &&
                family.memberExampleReferences == family.memberExampleReferences.sorted() &&
                family.memberExampleReferences.distinct().size == family.memberExampleReferences.size &&
                family.authorityReferences == family.authorityReferences.sorted()
        })
        val examplesByRef = corpus.examples.associateBy { it.exampleReference }
        val familyByRef = corpus.familyGroups.associateBy { it.familyGroupReference }
        require(corpus.familyGroups.all { family ->
            family.candidateId == family.familyGroupReference.substringAfterLast(":") &&
                family.relationDistribution == family.memberExampleReferences
                    .map { requireNotNull(examplesByRef[it]).relation }
                    .groupingBy { it }
                    .eachCount()
                    .toSortedMap()
        })
        require(corpus.examples.all { familyByRef[it.familyGroupReference]?.memberExampleReferences?.contains(it.exampleReference) == true })
        require(corpus.familyGroups.flatMap { it.memberExampleReferences }.distinct().size == corpus.examples.size)
        require(corpus.familyGroups.flatMap { it.memberExampleReferences }.all { examplesByRef.containsKey(it) })
        require(corpus.familyGroups.all { family -> family.memberExampleReferences.all { examplesByRef[it]?.familyGroupReference == family.familyGroupReference } })
        require(corpusReference(corpus.logicalDigest) == corpus.corpusReference)
        require(corpusDigest(corpus.copy(corpusReference = "", logicalDigest = "")) == corpus.logicalDigest)
        require(corpus.examples.none { it.originAuthority == "AI_ONLY" })
        require(corpus.examples.none { it.relation == "COMPONENT_OR_DERIVED_PRODUCT_OF" })
        require(corpus.examples.none { it.authorityStatus in setOf("UNRESOLVED", "ESCALATION", "HUMAN_REJECT") })
    }

    fun validatePartition(corpus: CorpusV2, partition: PartitionV2) {
        validateCorpus(corpus)
        require(partition.contractId == CONTRACT_ID)
        require(partition.version == VERSION)
        require(partition.state == PARTITION_STATE)
        require(partition.corpusReference == corpus.corpusReference)
        require(partition.corpusLogicalDigest == corpus.logicalDigest)
        require(partition.policyVersion == POLICY_VERSION)
        require(partition.partitionKey == PARTITION_KEY)
        require(partition.hash == HASH)
        require(partition.bucketModulus == BUCKET_MODULUS)
        require(partition.trainRange == "0-7999")
        require(partition.validationRange == "8000-8999")
        require(partition.holdoutRange == "9000-9999")
        require(!partition.rngUsed)
        require(!partition.thresholdChanged)
        require(partition.familyAssignments.size == EXPECTED_FAMILY_COUNT)
        require(partition.exampleAssignments.size == EXPECTED_EXAMPLE_COUNT)
        require(partition.familyAssignments == partition.familyAssignments.sortedBy { it.familyGroupReference })
        require(partition.exampleAssignments == partition.exampleAssignments.sortedBy { it.exampleReference })
        require(partition.familyAssignments.map { it.familyGroupReference }.toSet() == corpus.familyGroups.map { it.familyGroupReference }.toSet())
        require(partition.exampleAssignments.map { it.exampleReference }.toSet() == corpus.examples.map { it.exampleReference }.toSet())
        require(partition.familyAssignments.all { family ->
            FAMILY_REFERENCE.matches(family.familyGroupReference) &&
                SHA256.matches(family.hash) &&
                family.hash == sha256(family.familyGroupReference) &&
                family.firstEightHex == family.hash.substring(0, 8) &&
                family.unsignedValue == family.firstEightHex.toLong(16) &&
                family.bucket == (family.unsignedValue % BUCKET_MODULUS).toInt() &&
                family.partition == partitionFor(family.bucket) &&
                family.memberCount == corpus.familyGroups.single { it.familyGroupReference == family.familyGroupReference }.memberCount &&
                family.candidateId == corpus.familyGroups.single { it.familyGroupReference == family.familyGroupReference }.candidateId
        })
        val familyPartitions = partition.familyAssignments.associate { it.familyGroupReference to it.partition }
        require(partition.exampleAssignments.all { assignment ->
            val source = corpus.examples.single { it.exampleReference == assignment.exampleReference }
            assignment.familyGroupReference == source.familyGroupReference &&
                assignment.candidateId == source.candidateId &&
                assignment.targetKind == source.targetKind &&
                assignment.relation == source.relation &&
                assignment.partition == familyPartitions[assignment.familyGroupReference]
        })
        require(partition.trainFamilyCount == partition.familyAssignments.count { it.partition == "TRAIN" })
        require(partition.validationFamilyCount == partition.familyAssignments.count { it.partition == "VALIDATION" })
        require(partition.holdoutFamilyCount == partition.familyAssignments.count { it.partition == "HOLDOUT" })
        require(partition.trainExampleCount == partition.exampleAssignments.count { it.partition == "TRAIN" })
        require(partition.validationExampleCount == partition.exampleAssignments.count { it.partition == "VALIDATION" })
        require(partition.holdoutExampleCount == partition.exampleAssignments.count { it.partition == "HOLDOUT" })
        require(partition.familyLeakageCount == 0)
        require(partition.exampleLeakageCount == 0)
        require(partition.crossPartitionDuplicateExampleCount == 0)
        require(partition.familyAssignments.single { it.familyGroupReference == "family:v1:canonical:uVfHe4" }.bucket == 7889)
        require(partition.familyAssignments.single { it.familyGroupReference == "family:v1:canonical:ZuhV5V" }.bucket == 9411)
        require(partitionReference(partition.logicalDigest) == partition.partitionReference)
        require(partitionDigest(partition.copy(partitionReference = "", logicalDigest = "")) == partition.logicalDigest)
    }

    fun validateReadiness(corpus: CorpusV2, partition: PartitionV2, readiness: EvaluationReadinessV2) {
        require(readiness.contractId == READINESS_CONTRACT_ID)
        require(readiness.version == VERSION)
        require(readiness.corpusReference == corpus.corpusReference)
        require(readiness.partitionReference == partition.partitionReference)
        require(readiness.totalExampleCount == corpus.examples.size)
        require(readiness.totalFamilyCount == corpus.familyGroups.size)
        val expectedState = if (
            partition.trainFamilyCount > 0 && partition.validationFamilyCount > 0 && partition.holdoutFamilyCount > 0 &&
            partition.trainExampleCount > 0 && partition.validationExampleCount > 0 && partition.holdoutExampleCount > 0 &&
            partition.familyLeakageCount == 0 && partition.exampleLeakageCount == 0
        ) READY_STATE else BLOCKED_STATE
        require(readiness.state == expectedState)
        require(readiness.logicalDigest == readinessDigest(readiness.copy(logicalDigest = "")))
    }

    fun serialize(value: Any): ByteArray = (gson.toJson(value) + "\n").toByteArray(StandardCharsets.UTF_8)

    fun deserializeCorpus(bytes: ByteArray): CorpusV2 = gson.fromJson(bytes.toString(StandardCharsets.UTF_8), CorpusV2::class.java)

    fun deserializePartition(bytes: ByteArray): PartitionV2 = gson.fromJson(bytes.toString(StandardCharsets.UTF_8), PartitionV2::class.java)

    fun deserializeReadiness(bytes: ByteArray): EvaluationReadinessV2 = gson.fromJson(bytes.toString(StandardCharsets.UTF_8), EvaluationReadinessV2::class.java)

    private fun loadV1(root: File): Pair<List<ExampleV2>, List<FamilyV2>> {
        val snapshot = readJson(root.resolve(V1_SNAPSHOT_PATH))
        val partition = readJson(root.resolve(V1_PARTITION_PATH))
        require(snapshot.get("snapshotLogicalDigest").asString == EXPECTED_V1_CORPUS_DIGEST)
        require(partition.get("partitionDigest").asString == EXPECTED_V1_PARTITION_DIGEST)
        val familyByRecord = partition.getAsJsonArray("members").associate {
            val member = it.asJsonObject
            member.get("recordReference").asString to member.get("familyGroupReference").asString
        }
        val files = files(root.resolve(V1_POSITIVE_ROOT)) + files(root.resolve(V1_NEGATIVE_ROOT))
        require(files.size == 6)
        val examples = files.map { file ->
            val wrapper = readJson(file)
            val isNegative = wrapper.has("genericNegativeExample")
            val example = if (isNegative) wrapper.getAsJsonObject("genericNegativeExample").getAsJsonObject("positiveExample") else wrapper.getAsJsonObject("example")
            val reference = if (isNegative) wrapper.getAsJsonObject("genericNegativeExample").get("reference").asString else wrapper.get("exampleReference").asString
            val family = requireNotNull(familyByRecord[reference])
            val target = targetInfo(example.getAsJsonObject("target"))
            val authority = example.getAsJsonObject("provenance").getAsJsonObject("supervisionAuthority")
            val evidence = authority.getAsJsonArray("evidenceReferences")?.firstOrNull()?.asJsonObject
            ExampleV2(
                exampleReference = reference,
                familyGroupReference = family,
                candidateId = target.second,
                candidateName = example.getAsJsonObject("input")
                    .getAsJsonArray("canonicalContext")
                    .firstOrNull()
                    ?.asJsonObject
                    ?.get("canonicalName")
                    ?.asString
                    ?: "V1-${target.second}",
                targetKind = target.first,
                relation = relationFor(target.first),
                targetCanonicalId = target.second,
                originAuthority = "ORIGINAL_V1_HUMAN_AUTHORITY",
                authorityStatus = "HUMAN_AUTHORIZED",
                humanAuthorityReference = authority.get("authority").asString,
                source = evidence?.get("source")?.asString ?: "P1_HUMAN_SPECIFICATION",
                sourceArtifactSha256 = evidence?.get("sourceArtifactSha256")?.asString,
                evidenceRecordId = evidence?.get("sourceRecordIdentity")?.asString,
                evidenceReferenceId = null,
                semanticLabel = null,
                variantLabel = null,
                processingForm = null,
                preparationState = null,
                productForm = null,
            )
        }.sortedBy { it.exampleReference }
        val families = examples.groupBy { it.familyGroupReference }.map { (reference, members) ->
            FamilyV2(
                familyGroupReference = reference,
                candidateId = reference.substringAfterLast(":"),
                candidateName = "V1-${reference.substringAfterLast(":")}",
                memberExampleReferences = members.map { it.exampleReference }.sorted(),
                memberCount = members.size,
                relationDistribution = members.groupingBy { it.relation }.eachCount().toSortedMap(),
                authorityReferences = members.mapNotNull { it.humanAuthorityReference }.distinct().sorted(),
            )
        }.sortedBy { it.familyGroupReference }
        require(families.size == 2)
        require(partition.get("trainCount").asInt == 4 && partition.get("validationCount").asInt == 0 && partition.get("holdoutCount").asInt == 2)
        return examples to families
    }

    private fun loadExpansion(root: File): Pair<List<ExampleV2>, List<FamilyV2>> {
        val authority = readJson(root.resolve(EXPANSION_AUTHORITY))
        val examplesRoot = readJson(root.resolve(EXPANSION_EXAMPLES))
        val familiesRoot = readJson(root.resolve(EXPANSION_FAMILIES))
        val inventory = readJson(root.resolve(EXPANSION_INVENTORY))
        require(sha256File(root.resolve(EXPANSION_AUTHORITY)) == "7a2fd138cb1fbc22b7b07461b2583122b34e6d221d352c8117c5d611f6cad5fe")
        require(sha256File(root.resolve(EXPANSION_EXAMPLES)) == "d47ee0f5f664fecaeca7390b02b913bb35d97756029d00fdad4d38850d0fc7f1")
        require(sha256File(root.resolve(EXPANSION_FAMILIES)) == "1260107995604173090bd323a6206e9138bb36ee007714e6a1df547d8473398f")
        require(sha256File(root.resolve(EXPANSION_INVENTORY)) == "15f181702ffa4558361fd966f09aaf7bca1057331ba7a6f2fe315ee1aa0df978")
        require(authority.get("humanAuthorizedCandidateCount").asInt == 13)
        require(examplesRoot.get("positiveCount").asInt == 34 && examplesRoot.get("negativeCount").asInt == 0)
        require(familiesRoot.get("newFamilyGroupCount").asInt == 13 && familiesRoot.get("newFamilyMembershipCount").asInt == 34)
        require(inventory.getAsJsonArray("excludedCandidateIds").size() == 7)
        val examples = examplesRoot.getAsJsonArray("examples").map { value ->
            val item = value.asJsonObject
            ExampleV2(
                exampleReference = item.get("exampleReference").asString,
                familyGroupReference = item.get("familyGroupReference").asString,
                candidateId = item.get("candidateId").asString,
                candidateName = item.get("candidateName").asString,
                targetKind = item.get("classification").asString,
                relation = item.get("relation").asString,
                targetCanonicalId = item.get("targetCanonicalId").asString,
                originAuthority = "HUMAN_CONFIRMED_AI_ENRICHMENT",
                authorityStatus = "HUMAN_AUTHORIZED",
                humanAuthorityReference = item.get("humanAuthorityReference").asString,
                source = item.get("source").asString,
                sourceArtifactSha256 = item.get("sourceArtifactSha256").asString,
                evidenceRecordId = item.get("evidenceRecordId").asString,
                evidenceReferenceId = item.get("evidenceReferenceId").asString,
                semanticLabel = nullable(item, "semanticLabel"),
                variantLabel = nullable(item, "variantLabel"),
                processingForm = nullable(item, "processingForm"),
                preparationState = nullable(item, "preparationState"),
                productForm = nullable(item, "productForm"),
            )
        }.sortedBy { it.exampleReference }
        val families = familiesRoot.getAsJsonArray("familyGroups").map { value ->
            val item = value.asJsonObject
            FamilyV2(
                familyGroupReference = item.get("familyGroupReference").asString,
                candidateId = item.get("candidateId").asString,
                candidateName = item.get("candidateName").asString,
                memberExampleReferences = item.getAsJsonArray("trainingExampleReferences").map { it.asString }.sorted(),
                memberCount = item.get("memberCount").asInt,
                relationDistribution = item.getAsJsonObject("relationDistribution").entrySet().associate { it.key to it.value.asInt }.toSortedMap(),
                authorityReferences = item.getAsJsonArray("humanAuthorityReferences").map { it.asString }.sorted(),
            )
        }.sortedBy { it.familyGroupReference }
        require(examples.size == 34 && families.size == 13)
        require(families.flatMap { it.memberExampleReferences }.distinct().size == 34)
        return examples to families
    }

    private fun targetInfo(target: JsonObject): Pair<String, String> {
        return when (target.get("targetType").asString) {
            "ExistingCanonical" -> "EXISTING_CANONICAL" to target.get("canonicalId").asString
            "Identity" -> "IDENTITY" to target.get("parentCanonicalId").asString
            "Variant" -> "VARIANT" to target.getAsJsonObject("scope").get("canonicalId").asString
            "Alias" -> "ALIAS" to target.getAsJsonObject("equivalentEntity").get("canonicalId").asString
            else -> "NEW_CANONICAL" to "NEW_CANONICAL"
        }
    }

    private fun relationFor(targetKind: String): String = when (targetKind) {
        "IDENTITY" -> "IDENTITY_OF"
        "VARIANT", "ALIAS", "EXISTING_CANONICAL" -> "VARIANT_OF"
        else -> "VARIANT_OF"
    }

    private fun corpusDigest(corpus: CorpusV2): String = sha256(
        buildString {
            appendLine(CORPUS_DIGEST)
            appendLine("v1-corpus=$EXPECTED_V1_CORPUS_DIGEST")
            appendLine("v1-partition=$EXPECTED_V1_PARTITION_DIGEST")
            corpus.originalAuthorityReferences.sorted().forEach { appendLine("original=$it") }
            corpus.expansionAuthorityReferences.sorted().forEach { appendLine("expansion=$it") }
            corpus.humanAuthorityLineage.sorted().forEach { appendLine("human=$it") }
            corpus.examples.forEach { example -> appendLine(exampleLine(example)) }
            corpus.familyGroups.forEach { family -> appendLine(familyLine(family)) }
        },
    )

    private fun partitionDigest(partition: PartitionV2): String = sha256(
        buildString {
            appendLine(PARTITION_DIGEST)
            appendLine(partition.corpusReference)
            appendLine(partition.corpusLogicalDigest)
            appendLine(partition.policyVersion)
            appendLine(partition.partitionKey)
            appendLine(partition.hash)
            appendLine(partition.bucketModulus.toString())
            appendLine(partition.trainRange)
            appendLine(partition.validationRange)
            appendLine(partition.holdoutRange)
            partition.familyAssignments.forEach { appendLine(gson.toJson(it)) }
            partition.exampleAssignments.forEach { appendLine(gson.toJson(it)) }
        },
    )

    private fun readinessDigest(readiness: EvaluationReadinessV2): String = sha256(
        buildString {
            appendLine("HIM_P1_EVALUATION_DATASET_V2_READINESS_DIGEST")
            appendLine(readiness.state)
            appendLine(readiness.corpusReference)
            appendLine(readiness.corpusLogicalDigest)
            appendLine(readiness.partitionReference)
            appendLine(readiness.partitionLogicalDigest)
            appendLine(listOf(readiness.totalExampleCount, readiness.totalFamilyCount, readiness.trainFamilyCount, readiness.validationFamilyCount, readiness.holdoutFamilyCount, readiness.trainExampleCount, readiness.validationExampleCount, readiness.holdoutExampleCount, readiness.familyLeakageCount, readiness.exampleLeakageCount).joinToString(","))
            appendLine(readiness.partitionPolicyVersion)
            readiness.humanAuthorityLineage.sorted().forEach { appendLine("human=$it") }
            readiness.expansionLineage.sorted().forEach { appendLine("expansion=$it") }
        },
    )

    private fun exampleLine(example: ExampleV2): String = listOf(
        example.exampleReference, example.familyGroupReference, example.candidateId, example.candidateName,
        example.targetKind, example.relation, example.targetCanonicalId, example.originAuthority,
        example.authorityStatus, example.humanAuthorityReference.orEmpty(), example.source,
        example.sourceArtifactSha256.orEmpty(), example.evidenceRecordId.orEmpty(), example.evidenceReferenceId.orEmpty(),
        example.semanticLabel.orEmpty(), example.variantLabel.orEmpty(), example.processingForm.orEmpty(),
        example.preparationState.orEmpty(), example.productForm.orEmpty(),
    ).joinToString("\u0000")

    private fun familyLine(family: FamilyV2): String = listOf(
        family.familyGroupReference, family.candidateId, family.candidateName, family.memberCount.toString(),
        family.memberExampleReferences.joinToString(","), family.relationDistribution.entries.joinToString(",") { "${it.key}=${it.value}" },
        family.authorityReferences.joinToString(","),
    ).joinToString("\u0000")

    private fun corpusReference(digest: String) = "training-corpus-evaluation:v2:$digest"
    private fun partitionReference(digest: String) = "training-partition-evaluation:v2:$digest"

    private fun partitionFor(bucket: Int): String = when (bucket) {
        in TRAIN_FIRST..TRAIN_LAST -> "TRAIN"
        in VALIDATION_FIRST..VALIDATION_LAST -> "VALIDATION"
        in HOLDOUT_FIRST..HOLDOUT_LAST -> "HOLDOUT"
        else -> error("BUCKET_OUT_OF_RANGE")
    }

    private fun persistImmutable(path: File, bytes: ByteArray): PersistedArtifact {
        path.parentFile.mkdirs()
        if (path.exists()) require(Files.readAllBytes(path.toPath()).contentEquals(bytes)) { "EXISTING_ARTIFACT_CONFLICT:${path.path}" }
        else Files.write(path.toPath(), bytes)
        return PersistedArtifact(path.path, bytes.size.toLong(), sha256(bytes))
    }

    private fun files(directory: File): List<File> = directory.listFiles().orEmpty().filter { it.isFile && it.extension == "json" }.sortedBy { it.name }

    private fun readJson(file: File): JsonObject = JsonParser.parseString(Files.readString(file.toPath())).asJsonObject

    private fun nullable(json: JsonObject, name: String): String? = json.get(name)?.takeIf { !it.isJsonNull }?.asString

    private fun sha256File(file: File): String = sha256(Files.readAllBytes(file.toPath()))

    private fun sha256(value: String): String = sha256(value.toByteArray(StandardCharsets.UTF_8))

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
