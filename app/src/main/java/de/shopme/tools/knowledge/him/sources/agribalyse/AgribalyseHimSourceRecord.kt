package de.shopme.tools.knowledge.him.sources.agribalyse

/**
 * Source-faithful AGRIBALYSE record for HIM.
 *
 * Represents one product row from the AGRIBALYSE 3.2
 * "Synthese" worksheet.
 *
 * Contract:
 *
 * - source data only
 * - no canonical catalog mapping
 * - no HIM identity resolution
 * - no semantic normalization
 * - no candidate generation
 * - no knowledge merging
 * - no derived values
 *
 * Environmental impact values describe impacts for
 * 1 kg of product consumed by the consumer, including
 * intermediate losses as defined by AGRIBALYSE.
 */
data class AgribalyseHimSourceRecord(

    val agbCode: String,

    val ciqualCode: String,

    val foodGroup: String,

    val foodSubgroup: String,

    val productNameFr: String,

    val lciName: String,

    val seasonCode: String,

    val airTransportCode: String,

    val delivery: String,

    val packagingApproach: String,

    val preparation: String,

    val dataQualityRating: Double?,

    val efSingleScore: Double?,

    val climateChange: Double?,

    val ozoneDepletion: Double?,

    val ionisingRadiation: Double?,

    val photochemicalOzoneFormation: Double?,

    val particulateMatter: Double?,

    val humanToxicityNonCancer: Double?,

    val humanToxicityCancer: Double?,

    val terrestrialAndFreshwaterAcidification: Double?,

    val freshwaterEutrophication: Double?,

    val marineEutrophication: Double?,

    val terrestrialEutrophication: Double?,

    val freshwaterEcotoxicity: Double?,

    val landUse: Double?,

    val waterResourceDepletion: Double?,

    val energyResourceDepletion: Double?,

    val mineralResourceDepletion: Double?,

    val climateChangeBiogenic: Double?,

    val climateChangeFossil: Double?,

    val climateChangeLandUseChange: Double?
)