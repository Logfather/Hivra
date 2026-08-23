package de.shopme.tools.knowledge.him.sources.ciqual

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class CiqualHimSourceProjector {

    fun project(
        sourceDirectory: File
    ): CiqualHimSourceArtifact {
        require(sourceDirectory.isDirectory) {
            "CIQUAL raw source directory not found: ${sourceDirectory.absolutePath}"
        }

        val foods =
            loadFoods(
                requireSourceFile(
                    sourceDirectory,
                    FOODS_FILE_NAME
                )
            )

        val taxonomy =
            loadTaxonomy(
                requireSourceFile(
                    sourceDirectory,
                    TAXONOMY_FILE_NAME
                )
            )

        val constituents =
            loadConstituents(
                requireSourceFile(
                    sourceDirectory,
                    CONSTITUENTS_FILE_NAME
                )
            )

        val sources =
            loadSources(
                requireSourceFile(
                    sourceDirectory,
                    SOURCES_FILE_NAME
                )
            )

        val foodsByCode =
            uniqueBySourceCode(
                records = foods,
                sourceName = "ALIM",
                codeName = "alim_code",
                code = CiqualFood::alimCode
            )

        val constituentsByCode =
            uniqueBySourceCode(
                records = constituents,
                sourceName = "CONST",
                codeName = "const_code",
                code = CiqualConstituent::constCode
            )

        val sourcesByCode =
            uniqueBySourceCode(
                records = sources,
                sourceName = "SOURCES",
                codeName = "source_code",
                code = CiqualSource::sourceCode
            )

        val compositionsByFood =
            loadCompositions(
                file =
                    requireSourceFile(
                        sourceDirectory,
                        COMPOSITIONS_FILE_NAME
                    ),
                foodsByCode = foodsByCode,
                constituentsByCode = constituentsByCode,
                sourcesByCode = sourcesByCode
            )

        return CiqualHimSourceArtifact(
            foods =
                foods
                    .map { food ->
                        food.copy(
                            compositions =
                                compositionsByFood[food.alimCode]
                                    .orEmpty()
                                    .sortedBy {
                                        it.constCode
                                    }
                        )
                    }
                    .sortedBy {
                        it.alimCode
                    },
            taxonomy =
                taxonomy.sortedWith(
                    compareBy(
                        CiqualFoodTaxonomy::groupCode,
                        CiqualFoodTaxonomy::subgroupCode,
                        CiqualFoodTaxonomy::subSubgroupCode
                    )
                ),
            constituents =
                constituents.sortedBy {
                    it.constCode
                },
            sources =
                sources.sortedBy {
                    it.sourceCode
                }
        )
    }

    private fun loadFoods(
        file: File
    ): List<CiqualFood> =
        recordElements(
            document = parseXml(file),
            expectedName = "ALIM"
        ).map { element ->
            CiqualFood(
                alimCode =
                    requiredText(
                        element,
                        "alim_code"
                    ),
                nameFr =
                    requiredText(
                        element,
                        "alim_nom_fr"
                    ),
                nameEn =
                    requiredText(
                        element,
                        "alim_nom_eng"
                    ),
                scientificName =
                    sourceLeaf(
                        element,
                        "alim_nom_sci"
                    ),
                groupCode =
                    requiredText(
                        element,
                        "alim_grp_code"
                    ),
                subgroupCode =
                    requiredText(
                        element,
                        "alim_ssgrp_code"
                    ),
                subSubgroupCode =
                    requiredText(
                        element,
                        "alim_ssssgrp_code"
                    ),
                jonesFactorLexical =
                    requiredText(
                        element,
                        "facteur_Jones"
                    ),
                compositions = emptyList()
            )
        }

    private fun loadTaxonomy(
        file: File
    ): List<CiqualFoodTaxonomy> =
        recordElements(
            document = parseXml(file),
            expectedName = "ALIM_GRP"
        ).map { element ->
            CiqualFoodTaxonomy(
                groupCode =
                    requiredText(
                        element,
                        "alim_grp_code"
                    ),
                groupNameFr =
                    requiredText(
                        element,
                        "alim_grp_nom_fr"
                    ),
                groupNameEn =
                    requiredText(
                        element,
                        "alim_grp_nom_eng"
                    ),
                subgroupCode =
                    requiredText(
                        element,
                        "alim_ssgrp_code"
                    ),
                subgroupNameFr =
                    requiredText(
                        element,
                        "alim_ssgrp_nom_fr"
                    ),
                subgroupNameEn =
                    requiredText(
                        element,
                        "alim_ssgrp_nom_eng"
                    ),
                subSubgroupCode =
                    requiredText(
                        element,
                        "alim_ssssgrp_code"
                    ),
                subSubgroupNameFr =
                    requiredText(
                        element,
                        "alim_ssssgrp_nom_fr"
                    ),
                subSubgroupNameEn =
                    requiredText(
                        element,
                        "alim_ssssgrp_nom_eng"
                    )
            )
        }

    private fun loadConstituents(
        file: File
    ): List<CiqualConstituent> =
        recordElements(
            document = parseXml(file),
            expectedName = "CONST"
        ).map { element ->
            CiqualConstituent(
                constCode =
                    requiredText(
                        element,
                        "const_code"
                    ),
                nameFr =
                    requiredText(
                        element,
                        "const_nom_fr"
                    ),
                nameEn =
                    requiredText(
                        element,
                        "const_nom_eng"
                    ),
                infoodsCode =
                    sourceLeaf(
                        element,
                        "code_INFOODS"
                    )
            )
        }

    private fun loadSources(
        file: File
    ): List<CiqualSource> =
        recordElements(
            document = parseXml(file),
            expectedName = "SOURCES"
        ).map { element ->
            CiqualSource(
                sourceCode =
                    requiredText(
                        element,
                        "source_code"
                    ),
                citation =
                    sourceLeaf(
                        element,
                        "ref_citation"
                    )
            )
        }

    private fun loadCompositions(
        file: File,
        foodsByCode: Map<String, CiqualFood>,
        constituentsByCode: Map<String, CiqualConstituent>,
        sourcesByCode: Map<String, CiqualSource>
    ): Map<String, List<CiqualComposition>> {
        val compositionsByFood =
            HashMap<String, MutableList<CiqualComposition>>()

        val compositionKeys =
            HashSet<CompositionKey>()

        recordElements(
            document = parseXml(file),
            expectedName = "COMPO"
        ).forEach { element ->
            val alimCode =
                requiredText(
                    element,
                    "alim_code"
                )

            val constCode =
                requiredText(
                    element,
                    "const_code"
                )

            require(alimCode in foodsByCode) {
                "COMPO references unknown ALIM alim_code '$alimCode'."
            }

            require(constCode in constituentsByCode) {
                "COMPO references unknown CONST const_code '$constCode'."
            }

            val key =
                CompositionKey(
                    alimCode = alimCode,
                    constCode = constCode
                )

            require(compositionKeys.add(key)) {
                "Duplicate COMPO (alim_code, const_code) pair: " +
                        "'$alimCode'/'$constCode'."
            }

            val sourceCode =
                sourceLeaf(
                    element,
                    "source_code"
                )

            if (sourceCode.missingAttributeValue == null) {
                require(sourceCode.lexicalValue in sourcesByCode) {
                    "COMPO references unknown SOURCES source_code " +
                            "'${sourceCode.lexicalValue}'."
                }
            }

            val composition =
                CiqualComposition(
                    constCode = constCode,
                    teneurLexical =
                        requiredText(
                            element,
                            "teneur"
                        ),
                    minimum =
                        sourceLeaf(
                            element,
                            "min"
                        ),
                    maximum =
                        sourceLeaf(
                            element,
                            "max"
                        ),
                    confidenceCode =
                        sourceLeaf(
                            element,
                            "code_confiance"
                        ),
                    sourceCode = sourceCode
                )

            compositionsByFood
                .getOrPut(alimCode) {
                    ArrayList()
                }
                .add(composition)
        }

        return compositionsByFood
    }

    private fun parseXml(
        file: File
    ): Document {
        val factory =
            DocumentBuilderFactory
                .newInstance()
                .apply {
                    isNamespaceAware = true
                    setFeature(
                        "http://apache.org/xml/features/disallow-doctype-decl",
                        true
                    )
                    setFeature(
                        "http://xml.org/sax/features/external-general-entities",
                        false
                    )
                    setFeature(
                        "http://xml.org/sax/features/external-parameter-entities",
                        false
                    )
                    setFeature(
                        "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                        false
                    )
                    isXIncludeAware = false
                    isExpandEntityReferences = false
                }

        return factory
            .newDocumentBuilder()
            .parse(file)
    }

    private fun recordElements(
        document: Document,
        expectedName: String
    ): List<Element> {
        val root =
            document.documentElement

        require(root.tagName == "TABLE") {
            "Unexpected CIQUAL XML root element: ${root.tagName}"
        }

        val records =
            childElements(root)

        require(records.all { it.tagName == expectedName }) {
            "Unexpected record element in CIQUAL XML. Expected '$expectedName'."
        }

        return records
    }

    private fun requiredText(
        parent: Element,
        childName: String
    ): String {
        val leaf =
            findDirectChild(
                parent,
                childName
            )

        require(!leaf.hasAttribute("missing")) {
            "Required CIQUAL field '$childName' is marked missing."
        }

        return sourceText(leaf)
    }

    private fun sourceLeaf(
        parent: Element,
        childName: String
    ): CiqualSourceLeaf {
        val leaf =
            findDirectChild(
                parent,
                childName
            )

        return CiqualSourceLeaf(
            lexicalValue = sourceText(leaf),
            missingAttributeValue =
                if (leaf.hasAttribute("missing")) {
                    leaf.getAttribute("missing")
                } else {
                    null
                }
        )
    }

    private fun findDirectChild(
        parent: Element,
        childName: String
    ): Element {
        val matches =
            childElements(parent)
                .filter {
                    it.tagName == childName
                }

        require(matches.size == 1) {
            "Expected exactly one '$childName' child in '${parent.tagName}', " +
                    "found ${matches.size}."
        }

        return matches.single()
    }

    private fun childElements(
        parent: Element
    ): List<Element> {
        val result =
            ArrayList<Element>()

        val children =
            parent.childNodes

        for (index in 0 until children.length) {
            val node =
                children.item(index)

            if (node.nodeType == Node.ELEMENT_NODE) {
                result += node as Element
            }
        }

        return result
    }

    private fun directText(
        element: Element
    ): String {
        val result =
            StringBuilder()

        val children =
            element.childNodes

        for (index in 0 until children.length) {
            val node =
                children.item(index)

            if (
                node.nodeType == Node.TEXT_NODE ||
                node.nodeType == Node.CDATA_SECTION_NODE
            ) {
                result.append(node.nodeValue)
            }
        }

        return result.toString()
    }

    private fun sourceText(
        element: Element
    ): String =
        directText(element).trim()

    private fun requireSourceFile(
        sourceDirectory: File,
        fileName: String
    ): File =
        sourceDirectory
            .resolve(fileName)
            .also { file ->
                require(file.isFile) {
                    "CIQUAL raw source file not found: ${file.absolutePath}"
                }
                require(file.canRead()) {
                    "CIQUAL raw source file is not readable: ${file.absolutePath}"
                }
            }

    private fun <T> uniqueBySourceCode(
        records: List<T>,
        sourceName: String,
        codeName: String,
        code: (T) -> String
    ): Map<String, T> {
        val result =
            LinkedHashMap<String, T>()

        records.forEach { record ->
            val sourceCode =
                code(record)

            require(result.put(sourceCode, record) == null) {
                "Duplicate $sourceName $codeName '$sourceCode'."
            }
        }

        return result
    }

    private data class CompositionKey(
        val alimCode: String,
        val constCode: String
    )

    private companion object {
        const val FOODS_FILE_NAME =
            "alim_2025_11_03.xml"

        const val TAXONOMY_FILE_NAME =
            "alim_grp_2025_11_03.xml"

        const val COMPOSITIONS_FILE_NAME =
            "compo_2025_11_03.xml"

        const val CONSTITUENTS_FILE_NAME =
            "const_2025_11_03.xml"

        const val SOURCES_FILE_NAME =
            "sources_2025_11_03.xml"
    }
}
