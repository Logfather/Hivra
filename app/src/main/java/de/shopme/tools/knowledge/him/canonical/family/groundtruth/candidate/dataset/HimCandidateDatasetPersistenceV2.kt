package de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.io.File
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object HimCandidateDatasetPersistenceV2 {
    private val gson = GsonBuilder()
        .serializeNulls()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .registerTypeAdapter(HimCandidateRelation::class.java, CandidateRelationDeserializer)
        .registerTypeAdapter(HimFamilyEntityReference::class.java, FamilyEntityReferenceDeserializer)
        .create()

    fun addRun(dataset: HimCandidateDataset, run: HimCandidateGenerationRun): HimCandidateDataset {
        validateDeterministicOrdering(run)
        val existing = dataset.runs.find { it.runReference == run.runReference }
        require(existing == null || existing == run) { "Immutable Candidate run identity collision." }
        val runs = (dataset.runs + listOfNotNull(if (existing == null) run else null)).sortedBy { it.runReference.value }
        val occurrences = runs.flatMap { it.occurrences }.distinctBy { it.occurrenceReference }.sortedBy { it.occurrenceReference.value }
        val candidates = occurrences.groupBy { it.candidate.candidateReference }.map { (_, values) ->
            val hypotheses = values.map { it.candidate }

            val semanticIdentities = hypotheses
                .map { hypothesis ->
                    hypothesis.normalizedCandidateTerm to hypothesis.relation
                }
                .distinct()

            require(semanticIdentities.size == 1) {
                "CandidateReference collision between different semantic identities."
            }

            HimCandidateMasterRecord(
                candidate = hypotheses.first(),
                occurrences = values,
            )
        }.sortedBy { it.candidate.candidateReference.value }
        return HimCandidateDataset(runs = runs, candidates = candidates)
    }

    fun serialize(dataset: HimCandidateDataset): ByteArray = (gson.toJson(dataset) + "\n").toByteArray(Charsets.UTF_8)

    fun serializeRun(run: HimCandidateGenerationRun): ByteArray = (gson.toJson(run) + "\n").toByteArray(Charsets.UTF_8)

    fun readDataset(file: File): HimCandidateDataset =
        if (!file.isFile) HimCandidateDataset() else requireNotNull(gson.fromJson(file.readText(), HimCandidateDataset::class.java))

    fun writeNewRun(file: File, run: HimCandidateGenerationRun) {
        require(!file.exists()) { "Immutable Candidate generation run already exists: ${file.path}" }
        atomicWrite(file, serializeRun(run))
    }

    fun writeMaster(file: File, dataset: HimCandidateDataset) = atomicWrite(file, serialize(dataset))

    private fun atomicWrite(file: File, bytes: ByteArray) {
        val parent = requireNotNull(file.parentFile)
        require(parent.exists() || parent.mkdirs())
        val temporary = Files.createTempFile(parent.toPath(), ".${file.name}.", ".tmp")
        try {
            Files.write(temporary, bytes)
            Files.move(temporary, file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun validateDeterministicOrdering(run: HimCandidateGenerationRun) {
        require(run.inputRuns.map { it.inputRunReference.value } == run.inputRuns.map { it.inputRunReference.value }.sorted())
        run.inputRuns.forEach { input ->
            require(input.persistedCandidateReferences.map { it.value } == input.persistedCandidateReferences.map { it.value }.sorted())
            require(input.knownRelations.map { it.relationDescription } == input.knownRelations.map { it.relationDescription }.sorted())
            require(input.authorityConflicts.map { it.authorityEntityReference } == input.authorityConflicts.map { it.authorityEntityReference }.sorted())
            input.retrievalHistory.forEach { round ->
                val sources = round.directive.sourceQueries.map { it.source }
                require(sources == sources.sortedBy(HimGroundTruthSource::ordinal))
                val authoritativeSteps = round.directive.sourceQueries.flatMap { sourceQueries ->
                    sourceQueries.queries.map { sourceQueries.source to it }
                }
                require(round.steps.map { it.source to it.semanticQuery } == authoritativeSteps)
                require(round.steps.all { step -> step.evidenceReferences.distinct().size == step.evidenceReferences.size })
                require(round.includedEvidenceReferences.distinct().size == round.includedEvidenceReferences.size)
                require(round.omittedDueToBudgetEvidenceReferences.distinct().size == round.omittedDueToBudgetEvidenceReferences.size)
            }
        }
    }

    private object CandidateRelationDeserializer : JsonDeserializer<HimCandidateRelation> {
        override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): HimCandidateRelation {
            val value = json.asJsonObject
            return when {
                value.has("parentCanonicalId") -> context.deserialize(value, HimCandidateRelation.Identity::class.java)
                value.has("scope") -> context.deserialize(value, HimCandidateRelation.Variant::class.java)
                value.has("equivalentEntity") -> context.deserialize(value, HimCandidateRelation.Alias::class.java)
                value.entrySet().isEmpty() -> HimCandidateRelation.CreateNewCanonical
                else -> throw JsonParseException("Invalid Candidate relation structure")
            }
        }
    }

    private object FamilyEntityReferenceDeserializer : JsonDeserializer<HimFamilyEntityReference> {
        override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): HimFamilyEntityReference {
            val value = json.asJsonObject
            return if (value.has("identityId")) context.deserialize(value, HimFamilyEntityReference.Identity::class.java)
            else context.deserialize(value, HimFamilyEntityReference.Canonical::class.java)
        }
    }
}
