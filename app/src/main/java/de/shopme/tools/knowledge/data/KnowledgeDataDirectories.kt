package de.shopme.tools.knowledge.data

import java.io.File

object KnowledgeDataDirectories {

    /**
     * Persistente externe Quelldaten.
     *
     * Historisch lagen diese unter data/raw.
     * Die kanonische Datenarchitektur verwendet jetzt data/sources.
     */
    val raw =
        File(
            "../data/sources"
        )

    val preview =
        File(
            "../data/preview"
        )

    val generated =
        File(
            "../data/generated"
        )

    val openFoodFactsRaw =
        File(
            raw,
            "openfoodfacts"
        )

    val agribalyseRaw =
        File(
            raw,
            "agribalyse"
        )

    val openFoodFactsPreview =
        File(
            preview,
            "openfoodfacts"
        )

    val agribalysePreview =
        File(
            preview,
            "agribalyse"
        )

    val generatedKnowledge =
        File(
            generated,
            "knowledge"
        )
}