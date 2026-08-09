package de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility

import com.google.gson.JsonObject
import de.shopme.testing.system.tools.knowledge.catalog.semantic.combination.SemanticCombinationConstraintRegistry
import de.shopme.testing.system.tools.knowledge.catalog.semantic.combination.SemanticCombinationDecision
import de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility.ProductFamilySemanticProfile
import de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility.ProductFamilySemanticProfileRegistry
import de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility.ProductFamilyVariantCompatibilityDecision
import de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility.ProductFamilyVariantCompatibilityRegistry
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.SemanticVariantIdentityDecision
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.SemanticVariantIdentityPolicy
import de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility.value.FamilyVariantValuePlausibilityDecision
import de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility.value.FamilyVariantValuePlausibilityRegistry
import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantDefinition
import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantTaxonomyRegistry
import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantType

class MarketPlausibilityValidator {

    fun validate(
        json: JsonObject
    ): MarketPlausibilityResult? {

        val itemName =
            json.string("itemname")
                ?: return null

        val normalizedItem =
            json.string("normalized")
                ?: return null

        val category =
            json.string("category")
                ?: return null

        val parts =
            itemName
                .split(VARIANT_SEPARATOR)
                .map(String::trim)
                .filter(String::isNotBlank)

        /*
         * Baseline-/Family-Eintrag ohne Variant:
         * Der bestehende kanonische Basiskatalog wird hier nicht
         * erneut in Frage gestellt.
         */
        if (parts.size < 2) {
            return MarketPlausibilityResult(
                itemName = itemName,
                normalizedItem = normalizedItem,
                category = category,
                family = itemName,
                decision =
                    MarketPlausibilityDecision.ACCEPT,
                reason =
                    MarketPlausibilityReason.PROFILE_SUPPORTED_IDENTITY,
                identityVariantKeys =
                    emptyList(),
                knowledgeOnlyVariantKeys =
                    emptyList(),
                explanation =
                    "Base catalog identity without semantic expansion variant."
            )
        }

        val family =
            parts.first()

        val definitions =
            parts
                .drop(1)
                .map { rawVariant ->
                    requireNotNull(
                        SemanticVariantTaxonomyRegistry
                            .definitionFor(rawVariant)
                    ) {
                        "Missing semantic variant taxonomy for " +
                                "'$rawVariant' in '$itemName'."
                    }
                }

        val identityDefinitions =
            definitions.filter { definition ->
                SemanticVariantIdentityPolicy
                    .evaluate(definition.type)
                    .decision !=
                        SemanticVariantIdentityDecision.KNOWLEDGE_ONLY
            }

        val knowledgeOnlyDefinitions =
            definitions.filter { definition ->
                SemanticVariantIdentityPolicy
                    .evaluate(definition.type)
                    .decision ==
                        SemanticVariantIdentityDecision.KNOWLEDGE_ONLY
            }

        /*
         * ---------------------------------------------------------
         * 1. Generic placeholders are terminal reject.
         * ---------------------------------------------------------
         */

        if (
            definitions.any {
                it.type ==
                        SemanticVariantType.GENERIC_PLACEHOLDER
            }
        ) {
            return reject(
                itemName = itemName,
                normalizedItem = normalizedItem,
                category = category,
                family = family,
                identityDefinitions = identityDefinitions,
                knowledgeOnlyDefinitions = knowledgeOnlyDefinitions,
                reason =
                    MarketPlausibilityReason.GENERIC_PLACEHOLDER,
                explanation =
                    "Generic semantic placeholder cannot represent " +
                            "a market-facing canonical food identity."
            )
        }

        /*
         * ---------------------------------------------------------
         * 2. Pure Knowledge identities disappear completely.
         * ---------------------------------------------------------
         */

        if (
            identityDefinitions.isEmpty() &&
            knowledgeOnlyDefinitions.isNotEmpty()
        ) {
            return reject(
                itemName = itemName,
                normalizedItem = normalizedItem,
                category = category,
                family = family,
                identityDefinitions = identityDefinitions,
                knowledgeOnlyDefinitions = knowledgeOnlyDefinitions,
                reason =
                    MarketPlausibilityReason.KNOWLEDGE_ONLY_IDENTITY,
                explanation =
                    "Entry consists exclusively of knowledge attributes " +
                            "and does not form an independent product identity."
            )
        }

        /*
         * ---------------------------------------------------------
         * 3. Explicit market reject.
         * ---------------------------------------------------------
         */

        val explicitlyRejected =
            identityDefinitions.filter { definition ->
                MarketPlausibilityPolicy
                    .isExplicitlyRejected(
                        family = family,
                        variantKey =
                            definition.canonicalKey
                    )
            }

        if (explicitlyRejected.isNotEmpty()) {
            return reject(
                itemName = itemName,
                normalizedItem = normalizedItem,
                category = category,
                family = family,
                identityDefinitions = identityDefinitions,
                knowledgeOnlyDefinitions = knowledgeOnlyDefinitions,
                reason =
                    MarketPlausibilityReason
                        .EXPLICIT_FAMILY_VARIANT_REJECT,
                explanation =
                    "Explicitly market-incompatible variant(s): " +
                            explicitlyRejected
                                .joinToString {
                                    it.canonicalKey
                                }
            )
        }

        /*
         * ---------------------------------------------------------
         * 4. Inherent process is not product identity.
         * ---------------------------------------------------------
         */

        val inherent =
            identityDefinitions.filter { definition ->
                InherentProcessingPolicy
                    .isInherent(
                        family = family,
                        variantKey =
                            definition.canonicalKey
                    )
            }

        if (inherent.isNotEmpty()) {
            return reject(
                itemName = itemName,
                normalizedItem = normalizedItem,
                category = category,
                family = family,
                identityDefinitions = identityDefinitions,
                knowledgeOnlyDefinitions = knowledgeOnlyDefinitions,
                reason =
                    MarketPlausibilityReason
                        .INHERENT_PROCESSING_STATE,
                explanation =
                    "Variant describes an inherent product process " +
                            "rather than a distinct market identity: " +
                            inherent.joinToString {
                                it.canonicalKey
                            }
            )
        }

        /*
         * ---------------------------------------------------------
         * 5. Family compatibility.
         * ---------------------------------------------------------
         */

        val familyCompatibility =
            identityDefinitions.map { definition ->
                ProductFamilyVariantCompatibilityRegistry
                    .evaluate(
                        family = family,
                        category = category,
                        variant = definition
                    )
            }

        if (
            familyCompatibility.any {
                it.decision ==
                        ProductFamilyVariantCompatibilityDecision.REJECT
            }
        ) {
            return reject(
                itemName = itemName,
                normalizedItem = normalizedItem,
                category = category,
                family = family,
                identityDefinitions = identityDefinitions,
                knowledgeOnlyDefinitions = knowledgeOnlyDefinitions,
                reason =
                    MarketPlausibilityReason
                        .FAMILY_VARIANT_INCOMPATIBLE,
                explanation =
                    "At least one identity-forming variant is incompatible " +
                            "with the product family."
            )
        }

        /*
         * ---------------------------------------------------------
         * 6. Family-specific concrete value plausibility.
         * ---------------------------------------------------------
         *
         * TYPE compatibility alone must never promote every concrete
         * Value of that Type into a market identity.
         */

        val valuePlausibility =
            identityDefinitions.map { definition ->
                FamilyVariantValuePlausibilityRegistry
                    .evaluate(
                        family = family,
                        category = category,
                        variant = definition
                    )
            }

        if (
            valuePlausibility.any {
                it.decision ==
                        FamilyVariantValuePlausibilityDecision.REJECT
            }
        ) {
            return reject(
                itemName = itemName,
                normalizedItem = normalizedItem,
                category = category,
                family = family,
                identityDefinitions = identityDefinitions,
                knowledgeOnlyDefinitions = knowledgeOnlyDefinitions,
                reason =
                    MarketPlausibilityReason
                        .FAMILY_VARIANT_VALUE_INCOMPATIBLE,
                explanation =
                    "At least one concrete identity variant value " +
                            "is implausible for this product family."
            )
        }

        /*
         * ---------------------------------------------------------
         * 6. Combination compatibility.
         * ---------------------------------------------------------
         */

        if (identityDefinitions.size >= 2) {

            val combination =
                SemanticCombinationConstraintRegistry
                    .evaluate(
                        family = family,
                        category = category,
                        variants = identityDefinitions
                    )

            if (
                combination.decision ==
                SemanticCombinationDecision.REJECT
            ) {
                return reject(
                    itemName = itemName,
                    normalizedItem = normalizedItem,
                    category = category,
                    family = family,
                    identityDefinitions = identityDefinitions,
                    knowledgeOnlyDefinitions = knowledgeOnlyDefinitions,
                    reason =
                        MarketPlausibilityReason
                            .COMBINATION_INCOMPATIBLE,
                    explanation =
                        "Identity-forming variants are not semantically " +
                                "compatible as a combined catalog identity."
                )
            }
        }

        /*
         * ---------------------------------------------------------
         * 7. Explicit positive market evidence.
         * ---------------------------------------------------------
         */

        if (
            identityDefinitions.isNotEmpty() &&
            identityDefinitions.all { definition ->
                MarketPlausibilityPolicy
                    .isExplicitlyAccepted(
                        family = family,
                        variantKey =
                            definition.canonicalKey
                    )
            }
        ) {
            return accept(
                itemName = itemName,
                normalizedItem = normalizedItem,
                category = category,
                family = family,
                identityDefinitions = identityDefinitions,
                knowledgeOnlyDefinitions = knowledgeOnlyDefinitions,
                reason =
                    MarketPlausibilityReason
                        .EXPLICIT_FAMILY_VARIANT_ACCEPT,
                explanation =
                    "All identity variants have explicit positive " +
                            "market plausibility policy."
            )
        }

        /*
         * ---------------------------------------------------------
         * 8. Profile-supported deterministic acceptance.
         *
         * Only when:
         * - family has explicit non-GENERIC profile
         * - no knowledge-only variants remain
         * - every Family×Variant relation is ALLOW
         * - combination (if any) is ALLOW
         * ---------------------------------------------------------
         */

        val profile =
            ProductFamilySemanticProfileRegistry
                .profileFor(family)

        if (
            profile != ProductFamilySemanticProfile.GENERIC &&
            knowledgeOnlyDefinitions.isEmpty() &&
            familyCompatibility.isNotEmpty() &&
            familyCompatibility.all {
                it.decision ==
                        ProductFamilyVariantCompatibilityDecision.ALLOW
            } &&
            valuePlausibility.none {
                it.decision ==
                        FamilyVariantValuePlausibilityDecision.REJECT
            } &&
            combinationIsAllowed(
                family = family,
                category = category,
                definitions = identityDefinitions
            )
        ) {
            return accept(
                itemName = itemName,
                normalizedItem = normalizedItem,
                category = category,
                family = family,
                identityDefinitions = identityDefinitions,
                knowledgeOnlyDefinitions = knowledgeOnlyDefinitions,
                reason =
                    MarketPlausibilityReason
                        .PROFILE_SUPPORTED_IDENTITY,
                explanation =
                    "Identity is supported by explicit semantic family " +
                            "profile and compatible variant constraints."
            )
        }

        /*
         * Everything else remains REVIEW.
         */
        return MarketPlausibilityResult(
            itemName = itemName,
            normalizedItem = normalizedItem,
            category = category,
            family = family,
            decision =
                MarketPlausibilityDecision.REVIEW,
            reason =
                MarketPlausibilityReason
                    .UNRESOLVED_MARKET_PLAUSIBILITY,
            identityVariantKeys =
                identityDefinitions
                    .map { it.canonicalKey }
                    .sorted(),
            knowledgeOnlyVariantKeys =
                knowledgeOnlyDefinitions
                    .map { it.canonicalKey }
                    .sorted(),
            explanation =
                "Semantic structure is not sufficiently conclusive " +
                        "for deterministic market acceptance or rejection."
        )
    }

    private fun combinationIsAllowed(
        family: String,
        category: String,
        definitions: List<SemanticVariantDefinition>
    ): Boolean {

        if (definitions.size <= 1) {
            return true
        }

        return SemanticCombinationConstraintRegistry
            .evaluate(
                family = family,
                category = category,
                variants = definitions
            )
            .decision ==
                SemanticCombinationDecision.ALLOW
    }

    private fun accept(
        itemName: String,
        normalizedItem: String,
        category: String,
        family: String,
        identityDefinitions: List<SemanticVariantDefinition>,
        knowledgeOnlyDefinitions: List<SemanticVariantDefinition>,
        reason: MarketPlausibilityReason,
        explanation: String
    ) =
        MarketPlausibilityResult(
            itemName = itemName,
            normalizedItem = normalizedItem,
            category = category,
            family = family,
            decision =
                MarketPlausibilityDecision.ACCEPT,
            reason = reason,
            identityVariantKeys =
                identityDefinitions
                    .map { it.canonicalKey }
                    .sorted(),
            knowledgeOnlyVariantKeys =
                knowledgeOnlyDefinitions
                    .map { it.canonicalKey }
                    .sorted(),
            explanation = explanation
        )

    private fun reject(
        itemName: String,
        normalizedItem: String,
        category: String,
        family: String,
        identityDefinitions: List<SemanticVariantDefinition>,
        knowledgeOnlyDefinitions: List<SemanticVariantDefinition>,
        reason: MarketPlausibilityReason,
        explanation: String
    ) =
        MarketPlausibilityResult(
            itemName = itemName,
            normalizedItem = normalizedItem,
            category = category,
            family = family,
            decision =
                MarketPlausibilityDecision.REJECT,
            reason = reason,
            identityVariantKeys =
                identityDefinitions
                    .map { it.canonicalKey }
                    .sorted(),
            knowledgeOnlyVariantKeys =
                knowledgeOnlyDefinitions
                    .map { it.canonicalKey }
                    .sorted(),
            explanation = explanation
        )

    private fun JsonObject.string(
        key: String
    ): String? {

        val value =
            get(key)
                ?: return null

        if (
            value.isJsonNull ||
            !value.isJsonPrimitive
        ) {
            return null
        }

        return value
            .asString
            .trim()
            .takeIf(String::isNotBlank)
    }

    private companion object {

        const val VARIANT_SEPARATOR =
            " – "
    }
}