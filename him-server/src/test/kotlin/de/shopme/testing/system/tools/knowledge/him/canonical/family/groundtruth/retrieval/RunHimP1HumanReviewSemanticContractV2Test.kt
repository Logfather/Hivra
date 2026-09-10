package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.Gson
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimP1HumanReviewSemanticContractV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimP1SecondPassComparisonAuthorityV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HumanRationaleEvidenceV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.RecordRelationV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.SemanticDecisionV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.SemanticRelationKindV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.SemanticRelationV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.SemanticResolutionV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.SecondPassComparisonOutcomeV2
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceKindV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidencePositionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReviewUnitV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RunHimP1HumanReviewSemanticContractV2Test {
    @Test
    fun targetKindAlignmentUsesExistingTrainingTargetKinds() {
        val relation = relation(
            kind = SemanticRelationKindV2.VARIANT_OF,
            label = "Scamorza affumicata",
            variantLabel = "Scamorza affumicata",
        )

        assertEquals(HimCandidateType.VARIANT, relation.targetKind)
        assertTrue(HimP1HumanReviewSemanticContractV2.targetForTraining(relation) is HimTrainingTargetV1.Variant)
        assertFalse(HimP1HumanReviewSemanticContractV2.targetForTraining(relation) is HimTrainingTargetV1.Identity)
    }

    @Test
    fun resolvedComponentRelationUsesStructuredMetadataWithoutTargetKind() {
        val relation = relation(
            kind = SemanticRelationKindV2.COMPONENT_OR_DERIVED_PRODUCT_OF,
            label = "Ricotta",
        )

        assertEquals(null, relation.targetKind)
        assertEquals("AbCd12", relation.targetReference?.canonicalId?.value)
        assertEquals(SemanticResolutionV2.RESOLVED, relation.resolution)
        assertEquals(null, HimP1HumanReviewSemanticContractV2.targetForTraining(relation))
    }

    @Test
    fun componentRelationCannotBecomeAnExistingTrainingTargetKind() {
        val component = relation(
            kind = SemanticRelationKindV2.COMPONENT_OR_DERIVED_PRODUCT_OF,
            label = "Ricotta",
        )

        assertFalse(component.kind == SemanticRelationKindV2.VARIANT_OF)
        assertFalse(component.kind == SemanticRelationKindV2.IDENTITY_OF)
        assertFalse(component.kind == SemanticRelationKindV2.ALIAS_OF)
        assertFalse(component.kind == SemanticRelationKindV2.NEW_CANONICAL)
        assertEquals(null, HimP1HumanReviewSemanticContractV2.targetForTraining(component))
    }

    @Test
    fun resolvedComponentRelationCanParticipateInMixedRecordWiseDecision() {
        val original = record(
            candidate = "Ricotta",
            suffix = "ricotta-mixed",
            evidenceSuffixes = listOf("component", "variant"),
        )
        val target = HimFamilyEntityReference.Canonical(HimEntityId("7B7beg"))
        val component = SemanticRelationV2(
            kind = SemanticRelationKindV2.COMPONENT_OR_DERIVED_PRODUCT_OF,
            resolution = SemanticResolutionV2.RESOLVED,
            targetReference = target,
            semanticLabel = "Ricotta",
        )
        val variant = SemanticRelationV2(
            kind = SemanticRelationKindV2.VARIANT_OF,
            resolution = SemanticResolutionV2.RESOLVED,
            targetReference = target,
            semanticLabel = "Ricotta variant",
            variantLabel = "Ricotta variant",
        )

        val decision = project(
            original = original,
            aggregateRelation = SemanticRelationV2(
                kind = SemanticRelationKindV2.UNRESOLVED_MIXED_RELATION,
                resolution = SemanticResolutionV2.UNRESOLVED,
                targetReference = target,
                semanticLabel = "Mixed record-wise relation metadata.",
            ),
            recordRelations = listOf(
                RecordRelationV2(
                    evidenceRecordId = original.evidenceReferences[0].evidenceReferenceId,
                    evidenceReferenceIds = listOf(original.evidenceReferences[0].evidenceReferenceId),
                    relation = component,
                ),
                RecordRelationV2(
                    evidenceRecordId = original.evidenceReferences[1].evidenceReferenceId,
                    evidenceReferenceIds = listOf(original.evidenceReferences[1].evidenceReferenceId),
                    relation = variant,
                ),
            ),
            rationale = "The component relation remains structured and record-wise.",
        )

        decision.validate()
        assertEquals(2, decision.recordRelations.size)
        assertEquals(
            listOf(
                SemanticRelationKindV2.COMPONENT_OR_DERIVED_PRODUCT_OF,
                SemanticRelationKindV2.VARIANT_OF,
            ),
            decision.recordRelations.map { it.relation.kind },
        )
        assertEquals("7B7beg", decision.recordRelations[0].relation.targetReference?.canonicalId?.value)
        assertEquals(null, HimP1HumanReviewSemanticContractV2.targetForTraining(component))
    }

    @Test
    fun ricottaSixRecordRoundTripPreservesComponentRelationTargetAndEvidence() {
        val original = record(
            candidate = "Ricotta",
            suffix = "ricotta-component",
            evidenceSuffixes = listOf(
                "1566610:code:4011849210006",
                "1655289:code:278001665702713250000670",
                "1667245:code:8009679025514",
                "1705293:code:8000965014076",
                "198575:code:20036447",
                "892409:code:9005545000677",
            ),
        )
        val target = HimFamilyEntityReference.Canonical(HimEntityId("7B7beg"))
        val component = SemanticRelationV2(
            kind = SemanticRelationKindV2.COMPONENT_OR_DERIVED_PRODUCT_OF,
            resolution = SemanticResolutionV2.RESOLVED,
            targetReference = target,
            semanticLabel = "Ricotta",
        )
        val decision = project(
            original = original,
            aggregateRelation = SemanticRelationV2(
                kind = SemanticRelationKindV2.UNRESOLVED_MIXED_RELATION,
                resolution = SemanticResolutionV2.UNRESOLVED,
                targetReference = target,
                semanticLabel = "Ricotta",
            ),
            recordRelations = original.evidenceReferences.map { evidence ->
                RecordRelationV2(
                    evidenceRecordId = evidence.evidenceReferenceId,
                    evidenceReferenceIds = listOf(evidence.evidenceReferenceId),
                    relation = component,
                )
            },
            rationale = "All six evidence records support a component relation to Ricotta.",
        )

        val roundTripped = decision.copy(
            aggregateRelation = decision.aggregateRelation.copy(),
            recordRelations = decision.recordRelations.map { recordRelation ->
                recordRelation.copy(relation = recordRelation.relation.copy())
            },
            evidenceReferenceIds = decision.evidenceReferenceIds.toList(),
        )
        roundTripped.validate()

        assertEquals(6, roundTripped.recordRelations.size)
        assertEquals(6, roundTripped.evidenceReferenceIds.size)
        assertEquals(
            original.evidenceReferences.map { it.evidenceReferenceId }.sorted(),
            roundTripped.evidenceReferenceIds,
        )
        assertTrue(roundTripped.recordRelations.all {
            it.relation.kind == SemanticRelationKindV2.COMPONENT_OR_DERIVED_PRODUCT_OF &&
                it.relation.resolution == SemanticResolutionV2.RESOLVED &&
                it.relation.targetReference == target &&
                it.evidenceReferenceIds == listOf(it.evidenceRecordId)
        })
        assertEquals("7B7beg", roundTripped.aggregateRelation.targetReference?.canonicalId?.value)
        assertEquals(decision.logicalDigest, roundTripped.logicalDigest)
    }

    @Test
    fun semanticRoundTripPreservesSevenKnownBatchCases() {
        val cases = listOf(
            "Teff" to relation(SemanticRelationKindV2.VARIANT_OF, "Teff Ivory Grain", variantLabel = "Teff Ivory Grain"),
            "Fruchteis" to relation(SemanticRelationKindV2.VARIANT_OF, "Fruchteis Sticks", variantLabel = "Fruchteis Sticks"),
            "Rotbarsch" to relation(SemanticRelationKindV2.PRODUCT_FORM_OF, "Rotbarsch Filet", productForm = "Filet"),
            "Milchbrei" to relation(SemanticRelationKindV2.VARIANT_OF, "Milchbrei Kakao", variantLabel = "Milchbrei Kakao"),
            "Scamorza" to relation(SemanticRelationKindV2.VARIANT_OF, "Scamorza affumicata", variantLabel = "Scamorza affumicata"),
            "Rösti" to relation(SemanticRelationKindV2.PRODUCT_FORM_OF, "Vegetable rosti", productForm = "Vegetable rosti"),
            "Kardamom" to relation(SemanticRelationKindV2.PROCESSING_FORM_OF, "Kardamom gemahlen", processingForm = "gemahlen"),
        )

        cases.forEachIndexed { index, (candidate, relation) ->
            val decision = project(
                original = record(candidate, index.toString()),
                aggregateRelation = relation,
                recordRelations = emptyList(),
                rationale = "Explicit human semantic relation for $candidate.",
            )
            decision.validate()
            assertEquals(relation, decision.aggregateRelation)
            assertNotNull(decision.logicalDigest)
        }
        assertEquals(7, cases.size)
    }

    @Test
    fun processingAndProductFormsAreNotCollapsedIntoUndifferentiatedIdentity() {
        val processing = relation(
            SemanticRelationKindV2.PROCESSING_FORM_OF,
            "Kardamom gemahlen",
            processingForm = "gemahlen",
        )
        val product = relation(
            SemanticRelationKindV2.PRODUCT_FORM_OF,
            "Rotbarsch Filet",
            productForm = "Filet",
        )

        assertEquals(SemanticRelationKindV2.PROCESSING_FORM_OF, processing.kind)
        assertEquals(SemanticRelationKindV2.PRODUCT_FORM_OF, product.kind)
        assertEquals(HimCandidateType.VARIANT, processing.targetKind)
        assertEquals(HimCandidateType.VARIANT, product.targetKind)
        assertFalse(processing.kind == SemanticRelationKindV2.IDENTITY_OF)
        assertFalse(product.kind == SemanticRelationKindV2.IDENTITY_OF)
    }

    @Test
    fun recordWiseMixedRelationsCannotBeFlattenedToResolvedAggregate() {
        val grain = RecordRelationV2("grain-record", listOf(evidenceReferenceId("grain")), relation(
            SemanticRelationKindV2.VARIANT_OF,
            "Teff Ivory Grain",
            variantLabel = "Teff Ivory Grain",
        ))
        val flour = RecordRelationV2("flour-record", listOf(evidenceReferenceId("flour")), relation(
            SemanticRelationKindV2.COMPONENT_OR_DERIVED_PRODUCT_OF,
            resolution = SemanticResolutionV2.AMBIGUOUS,
            label = "Teff Ivory Flour is a derived product of Teff.",
        ))

        assertThrows(IllegalArgumentException::class.java) {
            project(
                original = record("Teff", "mixed", listOf("grain", "flour")),
                aggregateRelation = relation(
                    SemanticRelationKindV2.VARIANT_OF,
                    "Teff",
                    variantLabel = "Teff",
                ),
                recordRelations = listOf(grain, flour),
                rationale = "The packet contains explicitly different record-wise relationships.",
            )
        }

        val preserved = project(
            original = record("Teff", "mixed-valid", listOf("grain", "flour")),
            aggregateRelation = relation(
                SemanticRelationKindV2.UNRESOLVED_MIXED_RELATION,
                label = "Teff packet contains mixed record-wise relationships.",
                resolution = SemanticResolutionV2.AMBIGUOUS,
            ),
            recordRelations = listOf(grain, flour),
            rationale = "The packet contains explicitly different record-wise relationships.",
        )
        assertEquals(listOf("grain-record", "flour-record"), preserved.recordRelations.map { it.evidenceRecordId })
        assertEquals(null, HimP1HumanReviewSemanticContractV2.targetForTraining(preserved.aggregateRelation))
    }

    @Test
    fun unresolvedRelationCannotBeAutoResolvedAndV1LineageRemainsBound() {
        val original = record("Kardamom", "unresolved")
        val relation = relation(
            SemanticRelationKindV2.UNRESOLVED_MIXED_RELATION,
            label = "Processing-form distinction requires second-pass review.",
            resolution = SemanticResolutionV2.UNRESOLVED,
        )
        val projected = project(
            original = original,
            aggregateRelation = relation,
            recordRelations = emptyList(),
            rationale = "The human note is retained as rationale evidence.",
        )

        assertEquals(original.decision, projected.originalDecision)
        assertEquals(original.reviewUnit.reviewUnitId, projected.reviewUnitId)
        assertEquals("batch-v1", projected.v1BatchId)
        assertEquals(null, HimP1HumanReviewSemanticContractV2.targetForTraining(relation))
        assertTrue(projected.rationaleEvidence.text.contains("retained"))
    }

    @Test
    fun blindSecondPassPacketContainsEvidenceButNoPrimaryDecisionOrRationale() {
        val packet = HimP1HumanReviewSemanticContractV2.blindPacketFromV1(
            original = record("Rösti", "blind"),
            packetId = "human-review-expansion-v2:7HLNbE",
            candidateId = "7HLNbE",
            candidateLabel = "Rösti",
            decisionQuestion = "What structured semantic relationship is supported by this evidence?",
        )
        val json = JsonParser.parseString(Gson().toJson(packet)).asJsonObject

        assertTrue(packet.sourceEvidence.isNotEmpty())
        assertTrue(packet.allowedRelationKinds.contains(SemanticRelationKindV2.UNRESOLVED_MIXED_RELATION))
        assertFalse(json.has("originalDecision"))
        assertFalse(json.has("primaryDecision"))
        assertFalse(json.has("primaryRationale"))
        assertFalse(json.has("rationale"))
        assertFalse(json.has("partitionBucket"))
        assertFalse(json.has("modelPrediction"))
    }

    @Test
    fun sameReviewerSecondPassIsNeverMarkedPersonallyIndependent() {
        val primary = project(
            original = record("Scamorza", "compare-primary"),
            aggregateRelation = relation(SemanticRelationKindV2.VARIANT_OF, "Scamorza affumicata", variantLabel = "Scamorza affumicata"),
            recordRelations = emptyList(),
            rationale = "Primary semantic relation.",
        )
        val second = project(
            original = record("Scamorza", "compare-second"),
            aggregateRelation = relation(SemanticRelationKindV2.VARIANT_OF, "Scamorza affumicata", variantLabel = "Scamorza affumicata"),
            recordRelations = emptyList(),
            rationale = "Second-pass semantic relation.",
        ).copy(reviewUnitId = primary.reviewUnitId)

        val comparison = HimP1SecondPassComparisonAuthorityV2.compare(primary, second)
        assertEquals(SecondPassComparisonOutcomeV2.EXACT_AGREEMENT, comparison.outcome)
        assertTrue(comparison.sameReviewer)
        assertFalse(comparison.personallyIndependent)
        assertEquals(0, comparison.independentValidatorCount)
    }

    @Test
    fun completeRecordWiseRelationsWithMixedAggregateAreNotUnresolved() {
        val recordRelation = recordRelation(
            recordId = "record-1",
            relation = relation(SemanticRelationKindV2.VARIANT_OF, "Ravioli", variantLabel = "Ravioli with cheese"),
        )
        val decision = decision(
            aggregate = relation(
                SemanticRelationKindV2.UNRESOLVED_MIXED_RELATION,
                "Ravioli contains record-wise semantics.",
                resolution = SemanticResolutionV2.UNRESOLVED,
            ),
            recordRelations = listOf(recordRelation),
        )

        assertTrue(HimP1SecondPassComparisonAuthorityV2.recordWiseSemanticsFullyResolved(decision))
        assertEquals(
            SecondPassComparisonOutcomeV2.EXACT_AGREEMENT,
            HimP1SecondPassComparisonAuthorityV2.compare(decision, decision).outcome,
        )
    }

    @Test
    fun identicalRecordWiseRelationsHaveExactAgreement() {
        val relation = recordRelation("record-1", relation(SemanticRelationKindV2.IDENTITY_OF, "Ravioli"))
        val first = decision(recordRelations = listOf(relation))
        val second = decision(recordRelations = listOf(relation))

        assertEquals(SecondPassComparisonOutcomeV2.EXACT_AGREEMENT, compare(first, second))
    }

    @Test
    fun equivalentAggregateRepresentationHasSemanticCompatibility() {
        val recordRelation = recordRelation("record-1", relation(SemanticRelationKindV2.VARIANT_OF, "Ravioli", variantLabel = "Ravioli with cheese"))
        val first = decision(
            aggregate = relation(
                SemanticRelationKindV2.UNRESOLVED_MIXED_RELATION,
                "Technical mixed container.",
                resolution = SemanticResolutionV2.UNRESOLVED,
            ),
            recordRelations = listOf(recordRelation),
        )
        val second = decision(
            aggregate = relation(SemanticRelationKindV2.VARIANT_OF, "Ravioli", variantLabel = "Ravioli with cheese"),
            recordRelations = listOf(recordRelation),
        )

        assertEquals(SecondPassComparisonOutcomeV2.SEMANTICALLY_COMPATIBLE_AGREEMENT, compare(first, second))
    }

    @Test
    fun identityAndVariantAreMeaningfulDisagreement() {
        val first = decision(recordRelations = listOf(recordRelation("record-1", relation(SemanticRelationKindV2.IDENTITY_OF, "Ravioli"))))
        val second = decision(recordRelations = listOf(recordRelation("record-1", relation(SemanticRelationKindV2.VARIANT_OF, "Ravioli", variantLabel = "Ravioli with cheese"))))

        assertEquals(SecondPassComparisonOutcomeV2.MEANINGFUL_DISAGREEMENT, compare(first, second))
    }

    @Test
    fun processingFormAndProductFormAreMeaningfulDisagreement() {
        val first = decision(recordRelations = listOf(recordRelation("record-1", relation(SemanticRelationKindV2.PROCESSING_FORM_OF, "Kardamom", processingForm = "gemahlen"))))
        val second = decision(recordRelations = listOf(recordRelation("record-1", relation(SemanticRelationKindV2.PRODUCT_FORM_OF, "Kardamom", productForm = "Sticks"))))

        assertEquals(SecondPassComparisonOutcomeV2.MEANINGFUL_DISAGREEMENT, compare(first, second))
    }

    @Test
    fun differentTargetReferenceIsMeaningfulDisagreement() {
        val first = decision(recordRelations = listOf(recordRelation("record-1", relation(SemanticRelationKindV2.IDENTITY_OF, "Ravioli", targetId = "AbCd12"))))
        val second = decision(recordRelations = listOf(recordRelation("record-1", relation(SemanticRelationKindV2.IDENTITY_OF, "Ravioli", targetId = "7B7beg"))))

        assertEquals(SecondPassComparisonOutcomeV2.MEANINGFUL_DISAGREEMENT, compare(first, second))
    }

    @Test
    fun unresolvedRecordWiseRelationRemainsUnresolved() {
        val unresolved = relation(
            kind = SemanticRelationKindV2.COMPONENT_OR_DERIVED_PRODUCT_OF,
            label = "Ricotta is unresolved.",
            resolution = SemanticResolutionV2.AMBIGUOUS,
        )
        val decision = decision(
            aggregate = relation(
                SemanticRelationKindV2.UNRESOLVED_MIXED_RELATION,
                "Unresolved record-wise relation.",
                resolution = SemanticResolutionV2.UNRESOLVED,
            ),
            recordRelations = listOf(recordRelation("record-1", unresolved)),
        )

        assertEquals(SecondPassComparisonOutcomeV2.UNRESOLVED, compare(decision, decision))
    }

    @Test
    fun escalationRemainsRequiresEscalation() {
        val escalated = decision(
            originalDecision = HimZeroCandidateRecoveryHumanReviewDecisionV1.ESCALATE_OUT_OF_SCOPE,
        )

        assertEquals(SecondPassComparisonOutcomeV2.REQUIRES_ESCALATION, compare(escalated, escalated))
    }

    @Test
    fun resolvedComponentWithNullTargetKindRemainsComparable() {
        val component = relation(
            kind = SemanticRelationKindV2.COMPONENT_OR_DERIVED_PRODUCT_OF,
            label = "Ricotta",
        )
        val first = decision(recordRelations = listOf(recordRelation("record-1", component)))
        val second = decision(recordRelations = listOf(recordRelation("record-1", component)))

        assertTrue(HimP1SecondPassComparisonAuthorityV2.recordWiseSemanticsFullyResolved(first))
        assertEquals(SecondPassComparisonOutcomeV2.EXACT_AGREEMENT, compare(first, second))
    }

    @Test
    fun comparisonDoesNotMutateEitherDecision() {
        val first = decision(recordRelations = listOf(recordRelation("record-1", relation(SemanticRelationKindV2.IDENTITY_OF, "Ravioli"))))
        val second = decision(recordRelations = listOf(recordRelation("record-1", relation(SemanticRelationKindV2.IDENTITY_OF, "Ravioli"))))
        val firstDigest = first.logicalDigest
        val secondDigest = second.logicalDigest

        compare(first, second)

        assertEquals(firstDigest, first.logicalDigest)
        assertEquals(secondDigest, second.logicalDigest)
    }

    private fun project(
        original: HimZeroCandidateRecoveryHumanReviewDecisionRecordV1,
        aggregateRelation: SemanticRelationV2,
        recordRelations: List<RecordRelationV2>,
        rationale: String,
    ): SemanticDecisionV2 = HimP1HumanReviewSemanticContractV2.projectV1(
        original = original,
        v1BatchId = "batch-v1",
        v1BatchLogicalDigest = HimSha256("a".repeat(64)),
        packetId = "human-review-expansion-v2:7HLNbE",
        candidateId = "7HLNbE",
        aggregateRelation = aggregateRelation,
        recordRelations = recordRelations,
        rationale = HumanRationaleEvidenceV2(
            reviewerRef = original.reviewerRef,
            sourceDecisionIdentity = original.reviewUnit.reviewUnitId,
            text = rationale,
        ),
    )

    private fun decision(
        originalDecision: HimZeroCandidateRecoveryHumanReviewDecisionV1 = HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
        aggregate: SemanticRelationV2 = relation(
            SemanticRelationKindV2.UNRESOLVED_MIXED_RELATION,
            "Technical mixed container.",
            resolution = SemanticResolutionV2.UNRESOLVED,
        ),
        recordRelations: List<RecordRelationV2> = emptyList(),
    ): SemanticDecisionV2 = project(
        original = record(
            candidate = "Ravioli",
            suffix = "comparison",
            evidenceSuffixes = recordRelations.map { it.evidenceRecordId }.ifEmpty { listOf("comparison") },
        ),
        aggregateRelation = aggregate,
        recordRelations = recordRelations,
        rationale = "Comparison fixture.",
    ).copy(originalDecision = originalDecision)

    private fun compare(first: SemanticDecisionV2, second: SemanticDecisionV2): SecondPassComparisonOutcomeV2 =
        HimP1SecondPassComparisonAuthorityV2.compare(first, second).outcome

    private fun recordRelation(recordId: String, relation: SemanticRelationV2): RecordRelationV2 =
        RecordRelationV2(recordId, listOf(evidenceReferenceId(recordId)), relation)

    private fun relation(
        kind: SemanticRelationKindV2,
        label: String? = null,
        resolution: SemanticResolutionV2 = SemanticResolutionV2.RESOLVED,
        variantLabel: String? = null,
        processingForm: String? = null,
        productForm: String? = null,
        targetId: String = "AbCd12",
    ) = SemanticRelationV2(
        kind = kind,
        resolution = resolution,
        targetReference = if (resolution == SemanticResolutionV2.RESOLVED) {
            HimFamilyEntityReference.Canonical(HimEntityId(targetId))
        } else {
            null
        },
        semanticLabel = label ?: "Structured relation for $kind",
        variantLabel = variantLabel,
        processingForm = processingForm,
        productForm = productForm,
    )

    private fun record(
        candidate: String,
        suffix: String,
        evidenceSuffixes: List<String> = listOf(suffix),
    ): HimZeroCandidateRecoveryHumanReviewDecisionRecordV1 {
        val stable = "a".repeat(64)
        val evidence = evidenceSuffixes.map { evidenceSuffix ->
            HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1(
                evidenceReferenceId = evidenceReferenceId(evidenceSuffix),
                kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.ORIGIN_CORPUS_RECORD,
                directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
                position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION,
                artifactReference = "review-corpus.v1.json",
                recordReference = "off:product:row:$evidenceSuffix",
                artifactSha256 = "c".repeat(64),
                artifactLogicalDigest = "d".repeat(64),
                fieldReferences = listOf("productName"),
            )
        }
        val unit = HimZeroCandidateRecoveryHumanReviewReviewUnitV1(stable, "AbCd12")
        return HimZeroCandidateRecoveryHumanReviewDecisionRecordV1(
            contractId = HimZeroCandidateRecoveryHumanReviewContractV1.VERSION,
            reviewUnit = unit,
            reviewerRef = "human-reviewer:christian-glatschke:v1",
            reviewRound = 1,
            revision = 1,
            decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_AMBIGUOUS,
            reasonCodes = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.MULTIPLE_PLAUSIBLE_INTERPRETATIONS),
            evidenceReferences = evidence,
            reviewerNote = "Explicit human semantic note for $candidate.",
        )
    }

    private fun evidenceReferenceId(suffix: String): String = (suffix + "b").padEnd(64, 'b').take(64)
}
