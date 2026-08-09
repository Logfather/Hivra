package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution

import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit.CanonicalIdentityAuditItem
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit.CanonicalIdentityDuplicateGroup
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit.CanonicalIdentityDuplicateReason
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit.CanonicalIdentityDuplicateSeverity
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit.CanonicalIdentitySourceLayer
import kotlin.test.Test
import kotlin.test.assertEquals

class CanonicalIdentityResolverTest {

    private val resolver =
        CanonicalIdentityResolver()

    @Test
    fun prefersBaselineOverRegeneratedExpansion() {

        val result =
            resolve(
                reason =
                    CanonicalIdentityDuplicateReason
                        .IDENTICAL_NORMALIZED_NAME,

                fingerprint =
                    "bohnen konserviert",

                entries =
                    listOf(
                        item(
                            name = "Bohnen Konserviert",
                            normalized =
                                "bohnen-konserviert",
                            layer =
                                CanonicalIdentitySourceLayer.BASELINE
                        ),
                        item(
                            name =
                                "Bohnen – Konserviert",
                            normalized =
                                "beans-preserved",
                            layer =
                                CanonicalIdentitySourceLayer
                                    .REGENERATED_EXPANSION
                        )
                    )
            )

        assertEquals(
            expected =
                CanonicalIdentityResolutionDecision.MERGE,
            actual =
                result.decision
        )

        assertEquals(
            expected =
                "bohnen-konserviert",
            actual =
                result.winner?.normalized
        )

        assertEquals(
            expected =
                CanonicalIdentityResolutionReason
                    .BASELINE_PREFERRED_OVER_EXPANSION,
            actual =
                result.reason
        )
    }

    @Test
    fun prefersSingularApfel() {

        val result =
            resolve(
                reason =
                    CanonicalIdentityDuplicateReason
                        .IDENTICAL_NORMALIZED_NAME,

                fingerprint =
                    "apfel",

                entries =
                    listOf(
                        item(
                            name = "Äpfel",
                            normalized = "aepfel"
                        ),
                        item(
                            name = "Apfel",
                            normalized = "apfel"
                        )
                    )
            )

        assertEquals(
            expected = "Apfel",
            actual = result.winner?.itemName
        )

        assertEquals(
            expected =
                CanonicalIdentityResolutionReason
                    .SINGULAR_PREFERRED_OVER_PLURAL,
            actual =
                result.reason
        )
    }

    @Test
    fun prefersSingularGranatapfel() {

        val result =
            resolve(
                reason =
                    CanonicalIdentityDuplicateReason
                        .IDENTICAL_NORMALIZED_NAME,

                fingerprint =
                    "granatapfel",

                entries =
                    listOf(
                        item(
                            name = "Granatäpfel",
                            normalized = "granataepfel"
                        ),
                        item(
                            name = "Granatapfel",
                            normalized = "granatapfel"
                        )
                    )
            )

        assertEquals(
            expected = "Granatapfel",
            actual = result.winner?.itemName
        )
    }

    @Test
    fun alwaysPrefersGermanCompoundSpelling() {

        assertCompoundWinner(
            separated = "Apfel Schorle",
            separatedNormalized =
                "apfel-schorle",
            compound = "Apfelschorle",
            compoundNormalized =
                "apfelschorle"
        )

        assertCompoundWinner(
            separated = "Balsamico Essig",
            separatedNormalized =
                "balsamico-essig",
            compound = "Balsamicoessig",
            compoundNormalized =
                "balsamicoessig"
        )

        assertCompoundWinner(
            separated = "Basmati Reis",
            separatedNormalized =
                "basmati-reis",
            compound = "Basmatireis",
            compoundNormalized =
                "basmatireis"
        )

        assertCompoundWinner(
            separated = "Chia Samen",
            separatedNormalized =
                "chia-samen",
            compound = "Chiasamen",
            compoundNormalized =
                "chiasamen"
        )

        assertCompoundWinner(
            separated = "Chili Pulver",
            separatedNormalized =
                "chili-pulver",
            compound = "Chilipulver",
            compoundNormalized =
                "chilipulver"
        )

        assertCompoundWinner(
            separated = "Cashew-Drink",
            separatedNormalized =
                "cashew-drink",
            compound = "Cashewdrink",
            compoundNormalized =
                "cashewdrink"
        )
    }

    private fun assertCompoundWinner(
        separated: String,
        separatedNormalized: String,
        compound: String,
        compoundNormalized: String
    ) {

        val result =
            resolve(
                reason =
                    CanonicalIdentityDuplicateReason
                        .PUNCTUATION_VARIANT,

                fingerprint =
                    compound
                        .lowercase(),

                entries =
                    listOf(
                        item(
                            name = separated,
                            normalized =
                                separatedNormalized
                        ),
                        item(
                            name = compound,
                            normalized =
                                compoundNormalized
                        )
                    )
            )

        assertEquals(
            expected =
                CanonicalIdentityResolutionDecision.MERGE,
            actual =
                result.decision
        )

        assertEquals(
            expected =
                compound,
            actual =
                result.winner?.itemName
        )

        assertEquals(
            expected =
                CanonicalIdentityResolutionReason
                    .GERMAN_COMPOUND_PREFERRED,
            actual =
                result.reason
        )
    }

    @Test
    fun resolvesFinalKnownCanonicalIdentityConflicts() {

        assertFinalWinner(
            reason =
                CanonicalIdentityDuplicateReason
                    .IDENTICAL_NORMALIZED_NAME,
            fingerprint =
                "vollkorncracker",
            firstName =
                "Vollkorncracker",
            firstNormalized =
                "vollkorncracker",
            secondName =
                "Vollkorncräcker",
            secondNormalized =
                "vollkorncraecker",
            expectedWinner =
                "vollkorncracker"
        )

        assertFinalWinner(
            reason =
                CanonicalIdentityDuplicateReason
                    .PUNCTUATION_VARIANT,
            fingerprint =
                "weisskohl",
            firstName =
                "Weis(S)Kohl",
            firstNormalized =
                "weis-s-kohl",
            secondName =
                "Weißkohl",
            secondNormalized =
                "weisskohl",
            expectedWinner =
                "weisskohl"
        )

        assertFinalWinner(
            reason =
                CanonicalIdentityDuplicateReason
                    .WORD_ORDER_VARIANT,
            fingerprint =
                "beverages::gruner|tee",
            firstName =
                "Grüner Tee",
            firstNormalized =
                "gruener-tee",
            secondName =
                "Tee Grüner",
            secondNormalized =
                "tee-gruener",
            expectedWinner =
                "gruener-tee"
        )

        assertFinalWinner(
            reason =
                CanonicalIdentityDuplicateReason
                    .WORD_ORDER_VARIANT,
            fingerprint =
                "dairy::bio|eier",
            firstName =
                "Bio Eier",
            firstNormalized =
                "bio-eier",
            secondName =
                "Eier Bio",
            secondNormalized =
                "eier-bio",
            expectedWinner =
                "bio-eier"
        )

        assertFinalWinner(
            reason =
                CanonicalIdentityDuplicateReason
                    .WORD_ORDER_VARIANT,
            fingerprint =
                "fruit::beerenmix|tk",
            firstName =
                "Beerenmix TK",
            firstNormalized =
                "beerenmix-tk",
            secondName =
                "Tk-Beerenmix",
            secondNormalized =
                "tk-beerenmix",
            expectedWinner =
                "beerenmix-tk"
        )

        assertFinalWinner(
            reason =
                CanonicalIdentityDuplicateReason
                    .WORD_ORDER_VARIANT,
            fingerprint =
                "ready-meals::carne|chili|con|fertiggericht",
            firstName =
                "Chili Con Carne Fertiggericht",
            firstNormalized =
                "chili-con-carne-fertiggericht",
            secondName =
                "Fertiggericht Chili Con Carne",
            secondNormalized =
                "fertiggericht-chili-con-carne",
            expectedWinner =
                "chili-con-carne-fertiggericht"
        )

        assertFinalWinner(
            reason =
                CanonicalIdentityDuplicateReason
                    .WORD_ORDER_VARIANT,
            fingerprint =
                "ready-meals::fertiggericht|gemusecurry",
            firstName =
                "Fertiggericht Gemüsecurry",
            firstNormalized =
                "fertiggericht-gemuesecurry",
            secondName =
                "Gemüsecurry Fertiggericht",
            secondNormalized =
                "gemuesecurry-fertiggericht",
            expectedWinner =
                "gemuesecurry-fertiggericht"
        )

        assertFinalWinner(
            reason =
                CanonicalIdentityDuplicateReason
                    .WORD_ORDER_VARIANT,
            fingerprint =
                "ready-meals::fertiggericht|gulasch",
            firstName =
                "Fertiggericht Gulasch",
            firstNormalized =
                "fertiggericht-gulasch",
            secondName =
                "Gulasch Fertiggericht",
            secondNormalized =
                "gulasch-fertiggericht",
            expectedWinner =
                "gulasch-fertiggericht"
        )

        assertFinalWinner(
            reason =
                CanonicalIdentityDuplicateReason
                    .WORD_ORDER_VARIANT,
            fingerprint =
                "ready-meals::margherita|pizza|tk",
            firstName =
                "Pizza Margherita (TK)",
            firstNormalized =
                "pizza-margherita-tk",
            secondName =
                "Tk-Pizza Margherita",
            secondNormalized =
                "tk-pizza-margherita",
            expectedWinner =
                "pizza-margherita-tk"
        )

        assertFinalWinner(
            reason =
                CanonicalIdentityDuplicateReason
                    .WORD_ORDER_VARIANT,
            fingerprint =
                "snacks::frites|pommes|tk",
            firstName =
                "Pommes Frites TK",
            firstNormalized =
                "pommes-frites-tk",
            secondName =
                "Tk-Pommes Frites",
            secondNormalized =
                "tk-pommes-frites",
            expectedWinner =
                "pommes-frites-tk"
        )

        assertFinalWinner(
            reason =
                CanonicalIdentityDuplicateReason
                    .WORD_ORDER_VARIANT,
            fingerprint =
                "vegetables::gemusepfanne|tk",
            firstName =
                "Gemüsepfanne TK",
            firstNormalized =
                "gemuesepfanne-tk",
            secondName =
                "Tk-Gemüsepfanne",
            secondNormalized =
                "tk-gemuesepfanne",
            expectedWinner =
                "gemuesepfanne-tk"
        )

        assertFinalWinner(
            reason =
                CanonicalIdentityDuplicateReason
                    .WORD_ORDER_VARIANT,
            fingerprint =
                "rice::basmati|reis",
            firstName =
                "Basmati Reis",
            firstNormalized =
                "basmati-reis",
            secondName =
                "Reis Basmati",
            secondNormalized =
                "reis-basmati",
            expectedWinner =
                "basmati-reis"
        )

        assertFinalWinner(
            reason =
                CanonicalIdentityDuplicateReason
                    .WORD_ORDER_VARIANT,
            fingerprint =
                "rice::reis|sushi",
            firstName =
                "Reis Sushi",
            firstNormalized =
                "reis-sushi",
            secondName =
                "Sushi Reis",
            secondNormalized =
                "sushi-reis",
            expectedWinner =
                "sushi-reis"
        )

        assertFinalWinner(
            reason =
                CanonicalIdentityDuplicateReason
                    .WORD_ORDER_VARIANT,
            fingerprint =
                "vegetables::erbsen|tiefkuhl",
            firstName =
                "Erbsen (Tiefkühl)",
            firstNormalized =
                "erbsen-tiefkuehl",
            secondName =
                "Tiefkühl Erbsen",
            secondNormalized =
                "tiefkuehl-erbsen",
            expectedWinner =
                "erbsen-tiefkuehl"
        )
    }

    private fun resolve(
        reason: CanonicalIdentityDuplicateReason,
        fingerprint: String,
        entries: List<CanonicalIdentityAuditItem>
    ): CanonicalIdentityResolution =
        resolver
            .resolve(
                listOf(
                    CanonicalIdentityDuplicateGroup(
                        severity =
                            when (reason) {
                                CanonicalIdentityDuplicateReason
                                    .IDENTICAL_NORMALIZED_KEY,
                                CanonicalIdentityDuplicateReason
                                    .IDENTICAL_NORMALIZED_NAME ->
                                    CanonicalIdentityDuplicateSeverity.ERROR

                                else ->
                                    CanonicalIdentityDuplicateSeverity.REVIEW
                            },
                        reason =
                            reason,
                        fingerprint =
                            fingerprint,
                        entryCount =
                            entries.size,
                        categories =
                            entries
                                .map {
                                    it.category
                                }
                                .distinct(),
                        entries =
                            entries
                    )
                )
            )
            .single()

    private fun assertFinalWinner(
        reason: CanonicalIdentityDuplicateReason,
        fingerprint: String,
        firstName: String,
        firstNormalized: String,
        secondName: String,
        secondNormalized: String,
        expectedWinner: String
    ) {

        val result =
            resolve(
                reason =
                    reason,
                fingerprint =
                    fingerprint,
                entries =
                    listOf(
                        item(
                            name =
                                firstName,
                            normalized =
                                firstNormalized
                        ),
                        item(
                            name =
                                secondName,
                            normalized =
                                secondNormalized
                        )
                    )
            )

        assertEquals(
            expected =
                CanonicalIdentityResolutionDecision.MERGE,
            actual =
                result.decision
        )

        assertEquals(
            expected =
                expectedWinner,
            actual =
                result.winner?.normalized
        )
    }

    private fun item(
        name: String,
        normalized: String,
        layer: CanonicalIdentitySourceLayer =
            CanonicalIdentitySourceLayer.BASELINE
    ) =
        CanonicalIdentityAuditItem(
            itemName =
                name,
            normalized =
                normalized,
            category =
                "test",
            sourceLayer =
                layer
        )
}