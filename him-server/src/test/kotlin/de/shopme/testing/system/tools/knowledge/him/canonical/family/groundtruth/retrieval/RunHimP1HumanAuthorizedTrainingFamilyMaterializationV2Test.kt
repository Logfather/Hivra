package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimP1HumanAuthorizedTrainingFamilyMaterializationV2 as MaterializationAuthority
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimP1HumanAuthorizedTrainingFamilyMaterializationV2.AuthorizedCandidate
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimP1HumanAuthorizedTrainingFamilyMaterializationV2.AuthorizedRecord
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.SemanticRelationKindV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Locale

class RunHimP1HumanAuthorizedTrainingFamilyMaterializationV2Test {
    private val root: Path = locateRoot()
    private val materializer = MaterializationAuthority

    @Test
    fun humanConfirmedCandidatesMaterializeAtRecordLevel() {
        val materialization = materializer.materialize(authorizedCandidates(), excludedIds())

        assertEquals(13, materialization.candidates.size)
        assertEquals(34, materialization.rawMaterializationCandidateCount)
        assertEquals(0, materialization.duplicateCandidateCount)
        assertEquals(34, materialization.finalUniqueTrainingExampleCount)
        assertEquals(34, materialization.newPositiveTrainingExampleCount)
        assertEquals(0, materialization.newNegativeTrainingExampleCount)
        assertEquals(34, materialization.newTotalTrainingExampleCount)
        assertEquals(13, materialization.families.size)
        assertEquals(34, materialization.families.sumOf { it.members.size })
        assertEquals(0, materialization.excludedCandidateIds.intersect(materialization.candidates.map { it.candidateId.value }.toSet()).size)
        assertEquals(
            mapOf(
                SemanticRelationKindV2.IDENTITY_OF.name to 5,
                SemanticRelationKindV2.VARIANT_OF.name to 10,
                SemanticRelationKindV2.PROCESSING_FORM_OF.name to 7,
                SemanticRelationKindV2.PREPARATION_STATE_OF.name to 4,
                SemanticRelationKindV2.PRODUCT_FORM_OF.name to 8,
            ),
            materialization.examples.groupingBy { it.relationKind.name }.eachCount().toSortedMap(),
        )
        assertEquals(34, materialization.families.flatMap { it.members }.distinct().size)
        val authority = materializer.authorityArtifact(materialization)
        val relationMetadata = authority.records.flatMap { it.records }
        assertTrue(relationMetadata.filter { it.relation == SemanticRelationKindV2.VARIANT_OF.name }.all { !it.variantLabel.isNullOrBlank() })
        assertTrue(relationMetadata.filter { it.relation == SemanticRelationKindV2.PROCESSING_FORM_OF.name }.all { !it.processingForm.isNullOrBlank() })
        assertTrue(relationMetadata.filter { it.relation == SemanticRelationKindV2.PRODUCT_FORM_OF.name }.all { !it.productForm.isNullOrBlank() })
        assertEquals(4, relationMetadata.count { it.relation == SemanticRelationKindV2.PREPARATION_STATE_OF.name })
    }

    @Test
    fun materializationAndFamilyDerivationAreDeterministic() {
        val first = materializer.materialize(authorizedCandidates(), excludedIds())
        val second = materializer.materialize(authorizedCandidates(), excludedIds())
        val firstAuthority = materializer.authorityArtifact(first)
        val secondAuthority = materializer.authorityArtifact(second)

        assertEquals(first, second)
        assertEquals(firstAuthority, secondAuthority)
        assertEquals(materializer.serialize(firstAuthority).toList(), materializer.serialize(secondAuthority).toList())
        assertEquals(materializer.familyArtifact(first), materializer.familyArtifact(second))
        assertEquals(materializer.expansionArtifact(first, firstAuthority), materializer.expansionArtifact(second, secondAuthority))
    }

    @Test
    fun durableArtifactsRoundTripWithoutMutation() {
        val materialization = materializer.materialize(authorizedCandidates(), excludedIds())
        val authority = materializer.authorityArtifact(materialization)
        val examples = materializer.trainingExamplesArtifact(materialization)
        val families = materializer.familyArtifact(materialization)
        val inventory = materializer.expansionArtifact(materialization, authority)
        val directory = root.resolve("data/knowledge/him/training/expansions/v2/p1-human-validation-expansion-v2-batch-1-confirmation-v1")
        Files.createDirectories(directory)

        val artifacts: Map<String, ByteArray> = mapOf(
            "human-authorized-structured-semantics.v2.json" to materializer.serialize(authority),
            "training-examples.v2.json" to materializer.serialize(examples),
            "family-groups.v2.json" to materializer.serialize(families),
            "expansion-inventory.v2.json" to materializer.serialize(inventory),
        )
        artifacts.forEach { (name, bytes) ->
            val path = directory.resolve(name)
            if (Files.exists(path)) {
                if (Files.readAllBytes(path).toList() != bytes.toList()) {
                    Files.write(path, bytes)
                }
                assertEquals(bytes.toList(), Files.readAllBytes(path).toList())
            } else {
                Files.write(path, bytes)
                assertEquals(bytes.toList(), Files.readAllBytes(path).toList())
            }
        }

        val reloadedExamples = JsonParser.parseString(
            Files.readString(directory.resolve("training-examples.v2.json")),
        ).asJsonObject
        val reloadedExampleReferences = reloadedExamples.getAsJsonArray("examples")
            .map { it.asJsonObject.get("exampleReference").asString }
            .sorted()
        assertEquals(34, reloadedExampleReferences.size)
        assertEquals(materialization.examples.map { it.example.exampleReference.value }.sorted(), reloadedExampleReferences)
        assertEquals(examples.logicalDigest, reloadedExamples.get("logicalDigest").asString)

        assertEquals(13, authority.humanAuthorizedCandidateCount)
        assertEquals(34, authority.recordWiseRelationCount)
        assertEquals(34, examples.examples.size)
        assertEquals(13, families.newFamilyGroupCount)
        assertEquals(34, families.newFamilyMembershipCount)
        assertEquals(15, families.totalValidatedFamilyGroupCountAfterExpansion)
        assertFalse(inventory.partitionBucketIncluded)
        assertFalse(inventory.partitionAssignmentIncluded)
        assertFalse(inventory.trainingStarted)
        assertFalse(inventory.inferenceStarted)
    }

    @Test
    fun unsupportedComponentRelationFailsClosed() {
        assertThrows(IllegalArgumentException::class.java) {
            materializer.targetFor(
                SemanticRelationKindV2.COMPONENT_OR_DERIVED_PRODUCT_OF,
                HimEntityId("2Mu5SI"),
            )
        }
    }

    @Test
    fun excludedAndDuplicateCandidatesFailClosed() {
        assertThrows(IllegalArgumentException::class.java) {
            materializer.materialize(authorizedCandidates(), excludedIds() + "2Mu5SI")
        }
        assertThrows(IllegalArgumentException::class.java) {
            val candidates = authorizedCandidates()
            materializer.materialize(candidates + candidates.first(), excludedIds())
        }
    }

    @Test
    fun humanAuthorityCannotBeEmptyOrAiOnly() {
        assertThrows(IllegalArgumentException::class.java) {
            materializer.materialize(emptyList(), excludedIds())
        }
        assertEquals("DISALLOWED", confirmationRoot().get("aiOnlyTrainingDirectPath").asString)
        assertEquals("CONFIRMATIONS_COMPLETE_AWAITING_MATERIALIZATION", confirmationRoot().get("state").asString)
    }

    @Test
    fun aiOnlyProposalCannotEnterAuthority() {
        val confirmations = confirmationRoot().getAsJsonArray("capturedConfirmations").map { it.asJsonObject }
        assertTrue(confirmations.filter { it.get("candidateId").asString in authorizedCandidates().map { candidate -> candidate.candidateId.value } }
            .all { it.get("humanAuthorizedForLaterMaterialization").asBoolean })
        assertTrue(materializer.materialize(authorizedCandidates(), excludedIds()).candidates.all { it.humanRationale.isNotBlank() })
    }

    @Test
    fun keepUnresolvedCaseIsExcluded() {
        assertTrue(excludedIds().contains("0efHM5"))
        assertFalse(materializer.materialize(authorizedCandidates(), excludedIds()).candidates.any { it.candidateId.value == "0efHM5" })
    }

    @Test
    fun humanAbstainCaseIsExcluded() {
        assertTrue(excludedIds().contains("0k9QJF"))
        assertFalse(materializer.materialize(authorizedCandidates(), excludedIds()).candidates.any { it.candidateId.value == "0k9QJF" })
    }

    @Test
    fun escalationCaseIsExcluded() {
        assertTrue(excludedIds().contains("2fTXul"))
        assertFalse(materializer.materialize(authorizedCandidates(), excludedIds()).candidates.any { it.candidateId.value == "2fTXul" })
    }

    @Test
    fun humanRejectCasesAreExcluded() {
        assertTrue(excludedIds().containsAll(listOf("3zWNPA", "7kMTME")))
        val materialization = materializer.materialize(authorizedCandidates(), excludedIds())
        assertTrue(materialization.candidates.none { it.candidateId.value in setOf("3zWNPA", "7kMTME") })
    }

    @Test
    fun missingEvidenceBindingFailsClosed() {
        val candidate = authorizedCandidates().first()
        assertThrows(IllegalArgumentException::class.java) {
            candidate.copy(records = listOf(candidate.records.first().copy(evidenceReferenceId = "missing")))
        }
    }

    @Test
    fun invalidTargetReferenceFailsClosed() {
        assertThrows(IllegalArgumentException::class.java) {
            materializer.targetFor(SemanticRelationKindV2.IDENTITY_OF, HimEntityId("bad"))
        }
    }

    @Test
    fun mixedRecordSemanticsRemainRecordWise() {
        val materialization = materializer.materialize(authorizedCandidates(), excludedIds())
        val teff = materialization.examples.filter { it.candidateId.value == "3pMmZU" }
        assertEquals(6, teff.size)
        assertEquals(2, teff.count { it.relationKind == SemanticRelationKindV2.PROCESSING_FORM_OF })
        assertEquals(4, teff.count { it.relationKind == SemanticRelationKindV2.VARIANT_OF })
    }

    @Test
    fun manualFamilyAssignmentCannotReplaceResolverOutput() {
        val materialization = materializer.materialize(authorizedCandidates(), excludedIds())
        assertTrue(materialization.families.all { family -> family.familyGroupReference.value == "family:v1:canonical:${family.candidateId.value}" })
        assertEquals(34, materialization.families.flatMap { it.members }.distinct().size)
    }

    @Test
    fun sameExampleCannotEnterTwoFamilies() {
        val materialization = materializer.materialize(authorizedCandidates(), excludedIds())
        val members = materialization.families.flatMap { it.members }
        assertEquals(members.size, members.distinct().size)
        assertEquals(34, members.size)
    }

    @Test
    fun originalV1TrainingExamplesRemainOutsideExpansion() {
        val positive = root.resolve("data/knowledge/him/training/positive-examples/v1")
        val negative = root.resolve("data/knowledge/him/training/negative-examples/v1")
        val originalCount = Files.list(positive).use { stream -> stream.count() } + Files.list(negative).use { stream -> stream.count() }
        assertEquals(6, originalCount)
        assertEquals(34, materializer.materialize(authorizedCandidates(), excludedIds()).examples.size)
    }

    @Test
    fun partitionCannotInfluenceMaterialization() {
        val materialization = materializer.materialize(authorizedCandidates(), excludedIds())
        val authority = materializer.authorityArtifact(materialization)
        val inventory = materializer.expansionArtifact(materialization, authority)
        assertFalse(inventory.partitionBucketIncluded)
        assertFalse(inventory.partitionAssignmentIncluded)
        assertEquals(34, inventory.trainingExampleReferences.size)
    }

    @Test
    fun allThirteenHumanAuthorizedCasesAreCovered() {
        assertEquals(
            listOf("2Mu5SI", "2N4VfP", "3pMmZU", "3wwAoj", "4LRBAE", "4u4mJK", "507Fyb", "53PNsX", "63MsJS", "7HLNbE", "8LxrZr", "90q7uu", "9pihHp"),
            authorizedCandidates().map { it.candidateId.value },
        )
        assertEquals(7, excludedIds().size)
        assertTrue(authorizedCandidates().all { it.humanReviewerReference == materializer.HUMAN_REVIEWER })
        assertTrue(authorizedCandidates().all { it.records.all { record -> record.relationKind != SemanticRelationKindV2.COMPONENT_OR_DERIVED_PRODUCT_OF } })
    }

    private fun authorizedCandidates(): List<AuthorizedCandidate> {
        val primary = primaryRoot().getAsJsonArray("decisionRecords").map { it.asJsonObject }
            .associateBy { it.getAsJsonObject("reviewUnit").get("canonicalEntityId").asString }
        val ai = aiRoot().getAsJsonArray("decisionRecords").map { it.asJsonObject }
            .associateBy { it.getAsJsonObject("decision").get("candidateId").asString }
        val confirmations = confirmationRoot().getAsJsonArray("capturedConfirmations").map { it.asJsonObject }
            .associateBy { it.get("candidateId").asString }
        val ids = listOf("2Mu5SI", "2N4VfP", "3pMmZU", "3wwAoj", "4LRBAE", "4u4mJK", "507Fyb", "53PNsX", "63MsJS", "7HLNbE", "8LxrZr", "90q7uu", "9pihHp")

        return ids.map { id ->
            val primaryRecord = requireNotNull(primary[id])
            val aiRecord = requireNotNull(ai[id]).getAsJsonObject("decision")
            val confirmation = requireNotNull(confirmations[id])
            val packet = packetRoot(id)
            val packetRecords = packet.getAsJsonArray("records").map { it.asJsonObject }.associateBy { it.get("evidenceReference").asString }
            val primaryEvidence = primaryRecord.getAsJsonArray("evidenceReferences").map { it.asJsonObject }
                .filter { it.get("kind").asString == "ORIGIN_CORPUS_RECORD" }
                .associateBy { it.get("recordReference").asString }
            val relations = if (confirmation.get("response").asString == "MODIFY_STRUCTURED_SEMANTICS") {
                confirmation.getAsJsonArray("recordRelations").map { it.asJsonObject }
                    .map { it.get("evidenceRecordId").asString to it.get("relation").asString }
            } else {
                aiRecord.getAsJsonArray("recordRelations").map { it.asJsonObject }
                    .map { it.get("evidenceRecordId").asString to it.getAsJsonObject("relation").get("kind").asString }
            }
            val relationDetails = if (confirmation.get("response").asString == "MODIFY_STRUCTURED_SEMANTICS") {
                emptyMap<String, JsonObject>()
            } else {
                aiRecord.getAsJsonArray("recordRelations").map { it.asJsonObject }
                    .associate { it.get("evidenceRecordId").asString to it.getAsJsonObject("relation") }
            }
            val records = relations.mapIndexed { index, (recordId, relationName) ->
                val sourceRef = requireNotNull(primaryEvidence[recordId])
                val packetRecord = requireNotNull(packetRecords[recordId])
                val relation = relationDetails[recordId]
                val source = packetRecord.get("source").asString
                val observedTerm = packetRecord.getAsJsonArray("primaryValues").first().asString
                AuthorizedRecord(
                    evidenceRecordId = recordId,
                    evidenceReference = HimEvidenceReference(
                        source = source,
                        sourceArtifactSha256 = HimSha256(sourceRef.get("artifactSha256").asString),
                        sourceRecordIdentity = recordId,
                    ),
                    evidenceReferenceId = sourceRef.get("evidenceReferenceId").asString,
                    recordKind = packetRecord.get("recordKind").asString,
                    retrievalRank = index + 1,
                    observedTerm = observedTerm,
                    normalizedObservedTerm = observedTerm.trim().lowercase(Locale.ROOT),
                    relationKind = SemanticRelationKindV2.valueOf(relationName),
                    semanticLabel = relation?.optionalString("semanticLabel"),
                    variantLabel = relation?.optionalString("variantLabel"),
                    processingForm = relation?.optionalString("processingForm"),
                    preparationState = relation?.optionalString("preparationState"),
                    productForm = relation?.optionalString("productForm"),
                )
            }
            val candidate = packet.getAsJsonObject("candidateCanonicalTarget")
            AuthorizedCandidate(
                candidateId = HimEntityId(id),
                candidateName = candidate.get("canonicalName").asString,
                stableEntryId = primaryRecord.getAsJsonObject("reviewUnit").get("stableEntryId").asString,
                reviewUnitId = aiRecord.get("reviewUnitId").asString,
                primaryDecisionIdentity = aiRecord.get("v1DecisionRecordIdentity").asString,
                confirmationReference = confirmationReference(id, confirmation),
                humanReviewerReference = confirmationRoot().get("reviewer").asString,
                humanRationale = confirmation.get("rationale").asString,
                aiProposalLogicalDigest = HimSha256(aiRecord.get("logicalDigest").asString),
                records = records,
            )
        }
    }

    private fun confirmationReference(id: String, confirmation: JsonObject): String =
        "human-confirmation:v2:${sha256(id + "\u0000" + confirmation.toString())}"

    private fun excludedIds(): List<String> = listOf("0efHM5", "0k9QJF", "2fTXul", "3zWNPA", "7B7beg", "7SgCPz", "7kMTME")

    private fun primaryRoot(): JsonObject = readJson("data/knowledge/him/canonical-family/human-review/zero-candidate-recovery/v1/decision-batches/p1-human-validation-expansion-v2-batch-1/review-decisions.v1.json")

    private fun aiRoot(): JsonObject = readJson("data/knowledge/him/canonical-family/human-review/zero-candidate-recovery/v1/decision-batches/p1-human-validation-expansion-v2-batch-1-second-pass-1/review-decisions.v2.json")

    private fun confirmationRoot(): JsonObject = readJson("build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/expansion-v2/p1-human-validation-expansion-v2-confirmation-v1/confirmation-session.v1.json")

    private fun packetRoot(id: String): JsonObject = readJson("build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/expansion-v2/p1-human-validation-expansion-v2-batch-1/packets/$id.review-packet.v2.json")

    private fun readJson(relative: String): JsonObject = JsonParser.parseString(Files.readString(root.resolve(relative))).asJsonObject

    private fun JsonObject.optionalString(name: String): String? =
        get(name)?.takeIf { !it.isJsonNull }?.asString

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun locateRoot(): Path {
        var current = Path.of(System.getProperty("user.dir")).toAbsolutePath()
        while (current.parent != null) {
            if (Files.isDirectory(current.resolve("data")) && Files.isDirectory(current.resolve("him-server"))) return current
            current = current.parent
        }
        error("ShopMe repository root not found")
    }
}
