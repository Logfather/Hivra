package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family
.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy
.CanonicalFamilyAxisSemanticPolicyTestFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CanonicalFamilySemanticValuePolicyTest {

    private val policy =
        CanonicalFamilySemanticValuePolicy(
            policySet =
                CanonicalFamilyAxisSemanticPolicyTestFactory
                    .create(
                        CanonicalFamilyAxisSemanticPolicyTestFactory
                            .curated(
                                familyKey = "bread",
                                axis =
                                    CanonicalProductFamilyVariantAxis
                                        .GRAIN_TYPE,
                                allowedValues =
                                    listOf(
                                        "barley",
                                        "buckwheat",
                                        "corn",
                                        "millet",
                                        "mixed-grain",
                                        "oats",
                                        "rye",
                                        "spelt",
                                        "wheat",
                                        "wholegrain"
                                    )
                            ),

                        CanonicalFamilyAxisSemanticPolicyTestFactory
                            .closedIdentity(
                                familyKey = "pork",
                                axis =
                                    CanonicalProductFamilyVariantAxis
                                        .ANIMAL_SPECIES,
                                allowedValue = "pig"
                            ),

                        CanonicalFamilyAxisSemanticPolicyTestFactory
                            .closedIdentity(
                                familyKey = "beef",
                                axis =
                                    CanonicalProductFamilyVariantAxis
                                        .ANIMAL_SPECIES,
                                allowedValue = "cattle"
                            ),

                        CanonicalFamilyAxisSemanticPolicyTestFactory
                            .curated(
                                familyKey = "berries",
                                axis =
                                    CanonicalProductFamilyVariantAxis
                                        .PLANT_SPECIES,
                                allowedValues =
                                    listOf(
                                        "blackberry",
                                        "blueberry",
                                        "currant",
                                        "raspberry",
                                        "strawberry"
                                    )
                            )
                    )
        )

    @Test
    fun defineBreadGrainTypePolicy() {
        val allowedValues =
            policy.allowedValues(
                familyKey = "bread",
                axis =
                    CanonicalProductFamilyVariantAxis
                        .GRAIN_TYPE
            )

        assertNotNull(allowedValues)

        assertEquals(
            setOf(
                "barley",
                "buckwheat",
                "corn",
                "millet",
                "mixed-grain",
                "oats",
                "rye",
                "spelt",
                "wheat",
                "wholegrain"
            ),
            allowedValues
        )

        assertTrue(
            policy.hasExplicitPolicy(
                familyKey = "bread",
                axis =
                    CanonicalProductFamilyVariantAxis
                        .GRAIN_TYPE
            )
        )
    }

    @Test
    fun acceptRelevantBreadGrainTypes() {
        val allowedValues =
            requireNotNull(
                policy.allowedValues(
                    familyKey = "bread",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .GRAIN_TYPE
                )
            )

        listOf(
            "wheat",
            "rye",
            "spelt",
            "oats",
            "barley",
            "corn",
            "millet",
            "buckwheat",
            "mixed-grain",
            "wholegrain"
        ).forEach { value ->
            assertTrue(
                value in allowedValues,
                "Bread grain type '$value' must be accepted."
            )
        }
    }

    @Test
    fun rejectUnsupportedBreadGrainTypes() {
        val allowedValues =
            requireNotNull(
                policy.allowedValues(
                    familyKey = "bread",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .GRAIN_TYPE
                )
            )

        listOf(
            "rice",
            "quinoa",
            "amaranth",
            "einkorn",
            "emmer",
            "durum-wheat",
            "soft-wheat",
            "teff",
            "sorghum",
            "refined-grain"
        ).forEach { value ->
            assertFalse(
                value in allowedValues,
                "Bread grain type '$value' must not be accepted " +
                        "without explicit specialist-family curation."
            )
        }
    }

    @Test
    fun definePorkAnimalSpeciesPolicy() {
        val allowedValues =
            policy.allowedValues(
                familyKey = "pork",
                axis =
                    CanonicalProductFamilyVariantAxis
                        .ANIMAL_SPECIES
            )

        assertNotNull(allowedValues)

        assertEquals(
            setOf(
                "pig"
            ),
            allowedValues
        )

        assertTrue(
            policy.hasExplicitPolicy(
                familyKey = "pork",
                axis =
                    CanonicalProductFamilyVariantAxis
                        .ANIMAL_SPECIES
            )
        )
    }

    @Test
    fun rejectNonPigAnimalSpeciesForPork() {
        val allowedValues =
            requireNotNull(
                policy.allowedValues(
                    familyKey = "pork",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .ANIMAL_SPECIES
                )
            )

        listOf(
            "cattle",
            "calf",
            "chicken",
            "turkey",
            "duck",
            "goose",
            "sheep",
            "lamb",
            "goat",
            "deer",
            "wild-boar",
            "rabbit",
            "horse",
            "mixed-poultry",
            "mixed-meat"
        ).forEach { value ->
            assertFalse(
                value in allowedValues,
                "Animal species '$value' must not be accepted for pork."
            )
        }
    }

    @Test
    fun defineBeefAnimalSpeciesPolicy() {
        val allowedValues =
            policy.allowedValues(
                familyKey = "beef",
                axis =
                    CanonicalProductFamilyVariantAxis
                        .ANIMAL_SPECIES
            )

        assertNotNull(allowedValues)

        assertEquals(
            setOf(
                "cattle"
            ),
            allowedValues
        )

        assertTrue(
            policy.hasExplicitPolicy(
                familyKey = "beef",
                axis =
                    CanonicalProductFamilyVariantAxis
                        .ANIMAL_SPECIES
            )
        )
    }

    @Test
    fun rejectNonCattleAnimalSpeciesForBeef() {
        val allowedValues =
            requireNotNull(
                policy.allowedValues(
                    familyKey = "beef",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .ANIMAL_SPECIES
                )
            )

        listOf(
            "calf",
            "pig",
            "chicken",
            "turkey",
            "duck",
            "goose",
            "sheep",
            "lamb",
            "goat",
            "deer",
            "wild-boar",
            "rabbit",
            "horse",
            "mixed-poultry",
            "mixed-meat"
        ).forEach { value ->
            assertFalse(
                value in allowedValues,
                "Animal species '$value' must not be accepted for beef."
            )
        }
    }

    @Test
    fun defineBerriesPlantSpeciesPolicy() {
        val allowedValues =
            policy.allowedValues(
                familyKey = "berries",
                axis =
                    CanonicalProductFamilyVariantAxis
                        .PLANT_SPECIES
            )

        assertNotNull(allowedValues)

        assertEquals(
            setOf(
                "blackberry",
                "blueberry",
                "currant",
                "raspberry",
                "strawberry"
            ),
            allowedValues
        )

        assertTrue(
            policy.hasExplicitPolicy(
                familyKey = "berries",
                axis =
                    CanonicalProductFamilyVariantAxis
                        .PLANT_SPECIES
            )
        )
    }

    @Test
    fun rejectNonBerryPlantSpeciesForBerries() {
        val allowedValues =
            requireNotNull(
                policy.allowedValues(
                    familyKey = "berries",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PLANT_SPECIES
                )
            )

        listOf(
            "apple",
            "pear",
            "cherry",
            "plum",
            "peach",
            "apricot",
            "orange",
            "lemon",
            "lime",
            "grapefruit",
            "banana",
            "pineapple",
            "mango",
            "papaya",
            "grape",
            "watermelon",
            "tomato",
            "pepper",
            "cucumber",
            "carrot",
            "parsnip",
            "celery",
            "onion",
            "garlic",
            "leek",
            "broccoli",
            "cauliflower",
            "white-cabbage",
            "red-cabbage",
            "spinach",
            "lettuce",
            "zucchini",
            "eggplant",
            "pumpkin",
            "mushroom"
        ).forEach { value ->
            assertFalse(
                value in allowedValues,
                "Plant species '$value' must not be accepted for berries."
            )
        }
    }

    @Test
    fun returnNullForMissingFamilyAxisPolicy() {
        val allowedValues =
            policy.allowedValues(
                familyKey = "bread",
                axis =
                    CanonicalProductFamilyVariantAxis
                        .ANIMAL_SPECIES
            )

        assertEquals(
            null,
            allowedValues
        )

        assertFalse(
            policy.hasExplicitPolicy(
                familyKey = "bread",
                axis =
                    CanonicalProductFamilyVariantAxis
                        .ANIMAL_SPECIES
            )
        )
    }
}