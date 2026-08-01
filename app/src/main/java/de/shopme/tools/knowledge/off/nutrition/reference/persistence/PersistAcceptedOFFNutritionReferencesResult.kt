package de.shopme.tools.knowledge.off.nutrition.reference.persistence

import java.io.File

data class PersistAcceptedOFFNutritionReferencesResult(
    val referenceFile: File,
    val manifestFile: File,
    val manifest: OFFAcceptedNutritionReferenceManifest
)