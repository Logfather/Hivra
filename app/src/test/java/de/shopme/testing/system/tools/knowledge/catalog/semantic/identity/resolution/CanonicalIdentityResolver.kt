package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution

import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit.CanonicalIdentityAuditItem
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit.CanonicalIdentityDuplicateGroup
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit.CanonicalIdentityDuplicateReason
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit.CanonicalIdentitySourceLayer

class CanonicalIdentityResolver {

    fun resolve(
        groups: List<CanonicalIdentityDuplicateGroup>
    ): List<CanonicalIdentityResolution> =
        groups
            .map(::resolveGroup)
            .sortedWith(
                compareBy<CanonicalIdentityResolution>(
                    { it.decision.name },
                    { it.reason.name },
                    { it.fingerprint }
                )
            )

    private fun resolveGroup(
        group: CanonicalIdentityDuplicateGroup
    ): CanonicalIdentityResolution {

        /*
         * =========================================================
         * 1. BASELINE ↔ REGENERATED EXPANSION
         * =========================================================
         *
         * Eine regenerierte Expansion darf keine bereits vorhandene
         * Baseline-Identität noch einmal materialisieren.
         */

        val baseline =
            group.entries.filter {
                it.sourceLayer ==
                        CanonicalIdentitySourceLayer.BASELINE
            }

        val expansion =
            group.entries.filter {
                it.sourceLayer ==
                        CanonicalIdentitySourceLayer.REGENERATED_EXPANSION
            }

        if (
            baseline.size == 1 &&
            expansion.isNotEmpty()
        ) {
            return merge(
                group = group,
                winner = baseline.single(),
                merged = expansion,
                reason =
                    CanonicalIdentityResolutionReason
                        .BASELINE_PREFERRED_OVER_EXPANSION
            )
        }

        /*
         * =========================================================
         * 2. SINGULAR ↔ PLURAL
         * =========================================================
         */

        singularWinner(
            group.entries
        )?.let { winner ->

            return merge(
                group = group,
                winner = winner,
                merged =
                    group.entries.filterNot {
                        it == winner
                    },
                reason =
                    CanonicalIdentityResolutionReason
                        .SINGULAR_PREFERRED_OVER_PLURAL
            )
        }

        /*
         * =========================================================
         * 3. DEUTSCHE KOMPOSITA
         * =========================================================
         *
         * Verbindliche Catalog-Policy:
         *
         * Zusammenschreibung gewinnt immer gegenüber
         * Getrennt-/Bindestrich-/Kommaschreibung.
         */

        if (
            group.reason ==
            CanonicalIdentityDuplicateReason.PUNCTUATION_VARIANT
        ) {

            germanCompoundWinner(
                group.entries
            )?.let { winner ->

                return merge(
                    group = group,
                    winner = winner,
                    merged =
                        group.entries.filterNot {
                            it == winner
                        },
                    reason =
                        CanonicalIdentityResolutionReason
                            .GERMAN_COMPOUND_PREFERRED
                )
            }
        }

        /*
 * =========================================================
 * 4. FINAL EXPLICIT CANONICAL IDENTITY POLICY
 * =========================================================
 *
 * Nach Anwendung der generischen deterministischen
 * Resolution verbleibt nur noch ein kleiner, explizit
 * kuratierter Satz kanonischer Identitätskonflikte.
 */

        val finalPolicy =
            CanonicalIdentityFinalPolicy
                .preferredIdentity(
                    duplicateReason =
                        group.reason.name,
                    fingerprint =
                        group.fingerprint
                )

        if (finalPolicy != null) {

            val winner =
                group.entries
                    .singleOrNull {
                        it.normalized ==
                                finalPolicy.winnerNormalized
                    }
                    ?: error(
                        "Final canonical identity policy expects winner " +
                                "'${finalPolicy.winnerNormalized}' for " +
                                "${group.reason.name} / ${group.fingerprint}, " +
                                "but matching candidate was not found."
                    )

            return merge(
                group = group,
                winner = winner,
                merged =
                    group.entries.filterNot {
                        it == winner
                    },
                reason =
                    finalPolicy.reason
            )
        }

        /*
         * Alles andere bleibt bewusst offen.
         */
        return CanonicalIdentityResolution(
            duplicateReason =
                group.reason,

            fingerprint =
                group.fingerprint,

            decision =
                CanonicalIdentityResolutionDecision.REVIEW,

            reason =
                CanonicalIdentityResolutionReason
                    .NO_DETERMINISTIC_RESOLUTION,

            winner =
                null,

            merged =
                group.entries
                    .map(::candidate)
                    .sortedBy {
                        it.normalized
                    }
        )
    }

    private fun singularWinner(
        entries: List<CanonicalIdentityAuditItem>
    ): CanonicalIdentityAuditItem? {

        if (entries.size != 2) {
            return null
        }

        val names =
            entries
                .map { it.itemName }
                .toSet()

        return when {

            names ==
                    setOf(
                        "Apfel",
                        "Äpfel"
                    ) ->
                entries.first {
                    it.itemName == "Apfel"
                }

            names ==
                    setOf(
                        "Granatapfel",
                        "Granatäpfel"
                    ) ->
                entries.first {
                    it.itemName == "Granatapfel"
                }

            else ->
                null
        }
    }

    private fun germanCompoundWinner(
        entries: List<CanonicalIdentityAuditItem>
    ): CanonicalIdentityAuditItem? {

        if (entries.size < 2) {
            return null
        }

        val ranked =
            entries
                .map { entry ->
                    CompoundCandidate(
                        entry = entry,
                        separatorCount =
                            separatorCount(
                                entry.itemName
                            )
                    )
                }
                .sortedWith(
                    compareBy<CompoundCandidate>(
                        { it.separatorCount },
                        { it.entry.itemName.length },
                        { it.entry.normalized }
                    )
                )

        val winner =
            ranked.first()

        val runnerUp =
            ranked.getOrNull(1)
                ?: return null

        /*
         * Automatisch nur dann entscheiden, wenn der Gewinner
         * tatsächlich weniger Worttrenner besitzt.
         *
         * Dadurch wird z. B.
         *
         * Apfelschorle    (0)
         * Apfel Schorle   (1)
         *
         * eindeutig.
         */
        if (
            winner.separatorCount >=
            runnerUp.separatorCount
        ) {
            return null
        }

        return winner.entry
    }

    private fun separatorCount(
        value: String
    ): Int =
        value.count { character ->
            character == ' ' ||
                    character == '-' ||
                    character == '–' ||
                    character == ','
        }

    private fun merge(
        group: CanonicalIdentityDuplicateGroup,
        winner: CanonicalIdentityAuditItem,
        merged: List<CanonicalIdentityAuditItem>,
        reason: CanonicalIdentityResolutionReason
    ) =
        CanonicalIdentityResolution(
            duplicateReason =
                group.reason,

            fingerprint =
                group.fingerprint,

            decision =
                CanonicalIdentityResolutionDecision.MERGE,

            reason =
                reason,

            winner =
                candidate(winner),

            merged =
                merged
                    .map(::candidate)
                    .sortedBy {
                        it.normalized
                    }
        )

    private fun candidate(
        item: CanonicalIdentityAuditItem
    ) =
        CanonicalIdentityResolutionCandidate(
            itemName =
                item.itemName,
            normalized =
                item.normalized,
            category =
                item.category,
            sourceLayer =
                item.sourceLayer
        )

    private data class CompoundCandidate(
        val entry: CanonicalIdentityAuditItem,
        val separatorCount: Int
    )
}