package de.shopme.testing.system.tools.knowledge.catalog.truecanonical

import de.shopme.tools.knowledge.catalog.canonical.rebuild.model.CanonicalFoodIdentity
import de.shopme.tools.knowledge.catalog.truecanonical.TrueCanonicalGermanFoodCatalogValidator
import de.shopme.tools.knowledge.catalog.truecanonical.TrueCanonicalGermanFoodIdentityRegistry
import kotlin.test.Test
import kotlin.test.assertTrue

class TrueCanonicalGermanFoodIdentityRegistryTest {

    @Test
    fun registryContainsOnlyTrueCanonicalIdentities() {

        val entries =
            TrueCanonicalGermanFoodIdentityRegistry
                .identities
                .map { identity ->

                    CanonicalFoodIdentity(
                        itemname =
                            identity.itemname,

                        normalized =
                            identity.normalized,

                        category =
                            identity.category,

                        variants =
                            emptyList(),

                        sourceVariants =
                            emptyList()
                    )
                }

        TrueCanonicalGermanFoodCatalogValidator()
            .validate(
                entries
            )

        assertTrue(
            entries.isNotEmpty()
        )
    }
}