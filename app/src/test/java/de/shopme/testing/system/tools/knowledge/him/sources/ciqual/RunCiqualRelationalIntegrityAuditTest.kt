package de.shopme.testing.system.tools.knowledge.him.sources.ciqual

import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.TreeMap
import java.util.TreeSet
import javax.xml.parsers.DocumentBuilderFactory

class RunCiqualRelationalIntegrityAuditTest {

    @Test
    fun runCiqualRelationalIntegrityAudit() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val projectRoot =
            resolveProjectRoot()

        val sourceRoot =
            projectRoot.resolve(
                "data/sources/ciqual/raw"
            )

        require(sourceRoot.isDirectory) {
            "CIQUAL raw source directory not found: ${sourceRoot.absolutePath}"
        }

        val files =
            SourceFiles(
                foods =
                    requireSourceFile(
                        sourceRoot,
                        "alim_2025_11_03.xml"
                    ),
                foodGroups =
                    requireSourceFile(
                        sourceRoot,
                        "alim_grp_2025_11_03.xml"
                    ),
                compositions =
                    requireSourceFile(
                        sourceRoot,
                        "compo_2025_11_03.xml"
                    ),
                constituents =
                    requireSourceFile(
                        sourceRoot,
                        "const_2025_11_03.xml"
                    ),
                sources =
                    requireSourceFile(
                        sourceRoot,
                        "sources_2025_11_03.xml"
                    )
            )

        val foods =
            loadFoods(
                files.foods
            )

        val foodGroups =
            loadFoodGroups(
                files.foodGroups
            )

        val constituents =
            loadConstituents(
                files.constituents
            )

        val sources =
            loadSources(
                files.sources
            )

        val foodAudit =
            auditFoods(
                foods
            )

        val groupAudit =
            auditFoodGroups(
                foodGroups = foodGroups,
                foods = foods
            )

        val constituentAudit =
            auditConstituents(
                constituents
            )

        val sourceAudit =
            auditSources(
                sources
            )

        val compositionAudit =
            auditCompositions(
                file = files.compositions,
                foods = foods,
                constituents = constituents,
                sources = sources
            )

        val report =
            buildReport(
                sourceRoot = sourceRoot,
                files = files,
                foods = foods,
                foodGroups = foodGroups,
                constituents = constituents,
                sources = sources,
                foodAudit = foodAudit,
                groupAudit = groupAudit,
                constituentAudit = constituentAudit,
                sourceAudit = sourceAudit,
                compositionAudit = compositionAudit
            )

        val reportFile =
            projectRoot.resolve(
                "build/knowledge/reports/ciqual/" +
                        "ciqual-relational-integrity-audit.txt"
            )

        require(
            reportFile.parentFile.mkdirs() ||
                    reportFile.parentFile.isDirectory
        ) {
            "Could not create CIQUAL report directory: " +
                    reportFile.parentFile.absolutePath
        }

        reportFile.writeText(
            report,
            StandardCharsets.UTF_8
        )

        println(report)

        println(
            "CIQUAL relational integrity audit report written to: " +
                    reportFile.absolutePath
        )
    }

    private fun loadFoods(
        file: File
    ): List<FoodRecord> {
        val document =
            parseXml(file)

        return recordElements(
            document = document,
            expectedName = "ALIM"
        ).map { element ->
            FoodRecord(
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
                    optionalLeaf(
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
                jonesFactor =
                    requiredText(
                        element,
                        "facteur_Jones"
                    )
            )
        }
    }

    private fun loadFoodGroups(
        file: File
    ): List<FoodGroupRecord> {
        val document =
            parseXml(file)

        return recordElements(
            document = document,
            expectedName = "ALIM_GRP"
        ).map { element ->
            FoodGroupRecord(
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
    }

    private fun loadConstituents(
        file: File
    ): List<ConstituentRecord> {
        val document =
            parseXml(file)

        return recordElements(
            document = document,
            expectedName = "CONST"
        ).map { element ->
            ConstituentRecord(
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
                    optionalLeaf(
                        element,
                        "code_INFOODS"
                    )
            )
        }
    }

    private fun loadSources(
        file: File
    ): List<SourceRecord> {
        val document =
            parseXml(file)

        return recordElements(
            document = document,
            expectedName = "SOURCES"
        ).map { element ->
            SourceRecord(
                sourceCode =
                    requiredText(
                        element,
                        "source_code"
                    ),
                citation =
                    optionalLeaf(
                        element,
                        "ref_citation"
                    )
            )
        }
    }

    private fun auditFoods(
        foods: List<FoodRecord>
    ): FoodAudit {
        val codeCounts =
            foods
                .groupingBy {
                    it.alimCode
                }
                .eachCount()
                .toSortedMap()

        val duplicateCodes =
            codeCounts.filterValues {
                it > 1
            }

        val frenchNameCounts =
            foods
                .groupingBy {
                    normalizedIdentityText(
                        it.nameFr
                    )
                }
                .eachCount()
                .filterKeys {
                    it.isNotEmpty()
                }
                .toSortedMap()

        val englishNameCounts =
            foods
                .groupingBy {
                    normalizedIdentityText(
                        it.nameEn
                    )
                }
                .eachCount()
                .filterKeys {
                    it.isNotEmpty()
                }
                .toSortedMap()

        val duplicateFrenchNames =
            frenchNameCounts.filterValues {
                it > 1
            }

        val duplicateEnglishNames =
            englishNameCounts.filterValues {
                it > 1
            }

        val jonesFactorStats =
            NumericDomainStats()

        foods.forEach { food ->
            jonesFactorStats.observe(
                food.jonesFactor
            )
        }

        return FoodAudit(
            uniqueCodes =
                codeCounts.size,
            duplicateCodes =
                duplicateCodes,
            duplicateFrenchNames =
                duplicateFrenchNames,
            duplicateEnglishNames =
                duplicateEnglishNames,
            missingScientificNames =
                foods.count {
                    it.scientificName.isMissing
                },
            jonesFactorStats =
                jonesFactorStats.snapshot()
        )
    }

    private fun auditFoodGroups(
        foodGroups: List<FoodGroupRecord>,
        foods: List<FoodRecord>
    ): FoodGroupAudit {
        val tupleCounts =
            foodGroups
                .groupingBy {
                    it.key
                }
                .eachCount()

        val duplicateTuples =
            tupleCounts
                .filterValues {
                    it > 1
                }
                .toSortedMap()

        val availableTuples =
            foodGroups
                .mapTo(
                    TreeSet()
                ) {
                    it.key
                }

        val referencedTuples =
            foods
                .mapTo(
                    TreeSet()
                ) {
                    FoodGroupKey(
                        groupCode =
                            it.groupCode,
                        subgroupCode =
                            it.subgroupCode,
                        subSubgroupCode =
                            it.subSubgroupCode
                    )
                }

        val orphanFoodTuples =
            referencedTuples
                .filterNot {
                    it in availableTuples
                }
                .toSortedSet()

        val unreferencedGroupTuples =
            availableTuples
                .filterNot {
                    it in referencedTuples
                }
                .toSortedSet()

        val frenchLabelCounts =
            foodGroups
                .groupingBy {
                    normalizedIdentityText(
                        listOf(
                            it.groupNameFr,
                            it.subgroupNameFr,
                            it.subSubgroupNameFr
                        ).joinToString(
                            separator = " | "
                        )
                    )
                }
                .eachCount()
                .filterKeys {
                    it.isNotEmpty()
                }
                .toSortedMap()

        val englishLabelCounts =
            foodGroups
                .groupingBy {
                    normalizedIdentityText(
                        listOf(
                            it.groupNameEn,
                            it.subgroupNameEn,
                            it.subSubgroupNameEn
                        ).joinToString(
                            separator = " | "
                        )
                    )
                }
                .eachCount()
                .filterKeys {
                    it.isNotEmpty()
                }
                .toSortedMap()

        return FoodGroupAudit(
            uniqueTuples =
                availableTuples.size,
            duplicateTuples =
                duplicateTuples,
            referencedTuples =
                referencedTuples.size,
            orphanFoodTuples =
                orphanFoodTuples,
            unreferencedGroupTuples =
                unreferencedGroupTuples,
            duplicateFrenchLabels =
                frenchLabelCounts.filterValues {
                    it > 1
                },
            duplicateEnglishLabels =
                englishLabelCounts.filterValues {
                    it > 1
                }
        )
    }

    private fun auditConstituents(
        constituents: List<ConstituentRecord>
    ): ConstituentAudit {
        val codeCounts =
            constituents
                .groupingBy {
                    it.constCode
                }
                .eachCount()
                .toSortedMap()

        val infoodsCounts =
            constituents
                .asSequence()
                .filterNot {
                    it.infoodsCode.isMissing
                }
                .map {
                    it.infoodsCode.value
                }
                .filter {
                    it.isNotEmpty()
                }
                .groupingBy {
                    it
                }
                .eachCount()
                .toSortedMap()

        val frenchNameCounts =
            constituents
                .groupingBy {
                    normalizedIdentityText(
                        it.nameFr
                    )
                }
                .eachCount()
                .filterKeys {
                    it.isNotEmpty()
                }
                .toSortedMap()

        val englishNameCounts =
            constituents
                .groupingBy {
                    normalizedIdentityText(
                        it.nameEn
                    )
                }
                .eachCount()
                .filterKeys {
                    it.isNotEmpty()
                }
                .toSortedMap()

        return ConstituentAudit(
            uniqueCodes =
                codeCounts.size,
            duplicateCodes =
                codeCounts.filterValues {
                    it > 1
                },
            missingInfoodsCodes =
                constituents.count {
                    it.infoodsCode.isMissing
                },
            duplicateInfoodsCodes =
                infoodsCounts.filterValues {
                    it > 1
                },
            duplicateFrenchNames =
                frenchNameCounts.filterValues {
                    it > 1
                },
            duplicateEnglishNames =
                englishNameCounts.filterValues {
                    it > 1
                }
        )
    }

    private fun auditSources(
        sources: List<SourceRecord>
    ): SourceAudit {
        val codeCounts =
            sources
                .groupingBy {
                    it.sourceCode
                }
                .eachCount()
                .toSortedMap()

        val citationCounts =
            sources
                .asSequence()
                .filterNot {
                    it.citation.isMissing
                }
                .map {
                    normalizeWhitespace(
                        it.citation.value
                    )
                }
                .filter {
                    it.isNotEmpty()
                }
                .groupingBy {
                    it
                }
                .eachCount()
                .toSortedMap()

        return SourceAudit(
            uniqueCodes =
                codeCounts.size,
            duplicateCodes =
                codeCounts.filterValues {
                    it > 1
                },
            missingCitations =
                sources.count {
                    it.citation.isMissing
                },
            duplicateCitations =
                citationCounts.filterValues {
                    it > 1
                }
        )
    }

    private fun auditCompositions(
        file: File,
        foods: List<FoodRecord>,
        constituents: List<ConstituentRecord>,
        sources: List<SourceRecord>
    ): CompositionAudit {
        val knownFoodCodes =
            foods
                .mapTo(
                    HashSet()
                ) {
                    it.alimCode
                }

        val knownConstituentCodes =
            constituents
                .mapTo(
                    HashSet()
                ) {
                    it.constCode
                }

        val knownSourceCodes =
            sources
                .mapTo(
                    HashSet()
                ) {
                    it.sourceCode
                }

        val referencedFoodCodes =
            HashSet<String>()

        val referencedConstituentCodes =
            HashSet<String>()

        val referencedSourceCodes =
            HashSet<String>()

        val pairCounts =
            HashMap<CompositionKey, Int>()

        val orphanFoodCodes =
            TreeMap<String, Int>()

        val orphanConstituentCodes =
            TreeMap<String, Int>()

        val orphanSourceCodes =
            TreeMap<String, Int>()

        val confidenceCounts =
            TreeMap<String, Int>()

        val valueStats =
            NumericDomainStats()

        val minStats =
            NumericDomainStats()

        val maxStats =
            NumericDomainStats()

        var missingConfidenceCount =
            0

        var missingSourceCount =
            0

        var missingMinCount =
            0

        var missingMaxCount =
            0

        var minGreaterThanMax =
            0

        var valueBelowMin =
            0

        var valueAboveMax =
            0

        var valueMinComparable =
            0

        var valueMaxComparable =
            0

        var minMaxComparable =
            0

        val minGreaterThanMaxSamples =
            ArrayList<String>()

        val valueBelowMinSamples =
            ArrayList<String>()

        val valueAboveMaxSamples =
            ArrayList<String>()

        val document =
            parseXml(file)

        val records =
            recordElements(
                document = document,
                expectedName = "COMPO"
            )

        records.forEach { element ->
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

            val value =
                requiredText(
                    element,
                    "teneur"
                )

            val min =
                optionalLeaf(
                    element,
                    "min"
                )

            val max =
                optionalLeaf(
                    element,
                    "max"
                )

            val confidence =
                optionalLeaf(
                    element,
                    "code_confiance"
                )

            val sourceCode =
                optionalLeaf(
                    element,
                    "source_code"
                )

            referencedFoodCodes +=
                alimCode

            referencedConstituentCodes +=
                constCode

            if (alimCode !in knownFoodCodes) {
                increment(
                    orphanFoodCodes,
                    alimCode
                )
            }

            if (
                constCode !in
                knownConstituentCodes
            ) {
                increment(
                    orphanConstituentCodes,
                    constCode
                )
            }

            if (sourceCode.isMissing) {
                missingSourceCount++
            } else {
                val code =
                    sourceCode.value

                if (code.isNotEmpty()) {
                    referencedSourceCodes +=
                        code

                    if (
                        code !in
                        knownSourceCodes
                    ) {
                        increment(
                            orphanSourceCodes,
                            code
                        )
                    }
                }
            }

            if (confidence.isMissing) {
                missingConfidenceCount++

                increment(
                    confidenceCounts,
                    MISSING_TOKEN
                )
            } else {
                increment(
                    confidenceCounts,
                    confidence.value.ifEmpty {
                        EMPTY_TOKEN
                    }
                )
            }

            if (min.isMissing) {
                missingMinCount++
            }

            if (max.isMissing) {
                missingMaxCount++
            }

            val parsedValue =
                valueStats.observe(
                    value
                )

            val parsedMin =
                if (min.isMissing) {
                    null
                } else {
                    minStats.observe(
                        min.value
                    )
                }

            val parsedMax =
                if (max.isMissing) {
                    null
                } else {
                    maxStats.observe(
                        max.value
                    )
                }

            if (
                parsedMin != null &&
                parsedMax != null
            ) {
                minMaxComparable++

                if (
                    parsedMin >
                    parsedMax
                ) {
                    minGreaterThanMax++

                    addSample(
                        target =
                            minGreaterThanMaxSamples,
                        value =
                            compositionDiagnostic(
                                alimCode = alimCode,
                                constCode = constCode,
                                value = value,
                                min = min.value,
                                max = max.value
                            )
                    )
                }
            }

            if (
                parsedValue != null &&
                parsedMin != null
            ) {
                valueMinComparable++

                if (
                    parsedValue <
                    parsedMin
                ) {
                    valueBelowMin++

                    addSample(
                        target =
                            valueBelowMinSamples,
                        value =
                            compositionDiagnostic(
                                alimCode = alimCode,
                                constCode = constCode,
                                value = value,
                                min = min.value,
                                max =
                                    if (max.isMissing) {
                                        null
                                    } else {
                                        max.value
                                    }
                            )
                    )
                }
            }

            if (
                parsedValue != null &&
                parsedMax != null
            ) {
                valueMaxComparable++

                if (
                    parsedValue >
                    parsedMax
                ) {
                    valueAboveMax++

                    addSample(
                        target =
                            valueAboveMaxSamples,
                        value =
                            compositionDiagnostic(
                                alimCode = alimCode,
                                constCode = constCode,
                                value = value,
                                min =
                                    if (min.isMissing) {
                                        null
                                    } else {
                                        min.value
                                    },
                                max = max.value
                            )
                    )
                }
            }

            val key =
                CompositionKey(
                    alimCode =
                        alimCode,
                    constCode =
                        constCode
                )

            pairCounts[key] =
                (
                        pairCounts[key]
                            ?: 0
                        ) + 1
        }

        val duplicatePairs =
            pairCounts
                .filterValues {
                    it > 1
                }
                .toSortedMap()

        val unreferencedFoods =
            knownFoodCodes
                .filterNot {
                    it in referencedFoodCodes
                }
                .toSortedSet()

        val unreferencedConstituents =
            knownConstituentCodes
                .filterNot {
                    it in referencedConstituentCodes
                }
                .toSortedSet()

        val unreferencedSources =
            knownSourceCodes
                .filterNot {
                    it in referencedSourceCodes
                }
                .toSortedSet()

        return CompositionAudit(
            recordCount =
                records.size,
            uniqueFoodConstituentPairs =
                pairCounts.size,
            duplicatePairs =
                duplicatePairs,
            recordsInDuplicatePairs =
                duplicatePairs
                    .values
                    .sum(),
            maximumPairMultiplicity =
                pairCounts
                    .values
                    .maxOrNull()
                    ?: 0,
            orphanFoodCodes =
                orphanFoodCodes,
            orphanConstituentCodes =
                orphanConstituentCodes,
            orphanSourceCodes =
                orphanSourceCodes,
            referencedFoodCodes =
                referencedFoodCodes.size,
            referencedConstituentCodes =
                referencedConstituentCodes.size,
            referencedSourceCodes =
                referencedSourceCodes.size,
            unreferencedFoods =
                unreferencedFoods,
            unreferencedConstituents =
                unreferencedConstituents,
            unreferencedSources =
                unreferencedSources,
            confidenceCounts =
                confidenceCounts,
            missingConfidenceCount =
                missingConfidenceCount,
            missingSourceCount =
                missingSourceCount,
            missingMinCount =
                missingMinCount,
            missingMaxCount =
                missingMaxCount,
            valueStats =
                valueStats.snapshot(),
            minStats =
                minStats.snapshot(),
            maxStats =
                maxStats.snapshot(),
            minMaxComparable =
                minMaxComparable,
            minGreaterThanMax =
                minGreaterThanMax,
            minGreaterThanMaxSamples =
                minGreaterThanMaxSamples,
            valueMinComparable =
                valueMinComparable,
            valueBelowMin =
                valueBelowMin,
            valueBelowMinSamples =
                valueBelowMinSamples,
            valueMaxComparable =
                valueMaxComparable,
            valueAboveMax =
                valueAboveMax,
            valueAboveMaxSamples =
                valueAboveMaxSamples
        )
    }

    private fun buildReport(
        sourceRoot: File,
        files: SourceFiles,
        foods: List<FoodRecord>,
        foodGroups: List<FoodGroupRecord>,
        constituents: List<ConstituentRecord>,
        sources: List<SourceRecord>,
        foodAudit: FoodAudit,
        groupAudit: FoodGroupAudit,
        constituentAudit: ConstituentAudit,
        sourceAudit: SourceAudit,
        compositionAudit: CompositionAudit
    ): String =
        buildString {
            appendLine(
                "CIQUAL RELATIONAL INTEGRITY & VALUE DOMAIN AUDIT"
            )
            appendLine(
                "================================================"
            )
            appendLine()
            appendLine(
                "Source root : ${sourceRoot.absolutePath}"
            )
            appendLine()
            appendLine(
                "RAW FILES"
            )
            appendLine(
                "---------"
            )

            listOf(
                files.foods,
                files.foodGroups,
                files.compositions,
                files.constituents,
                files.sources
            ).forEach { file ->
                appendLine(
                    "${file.name} = ${file.length()} bytes"
                )
            }

            appendLine()
            appendLine(
                "SOURCE RECORD COUNTS"
            )
            appendLine(
                "--------------------"
            )
            appendLine(
                "ALIM     : ${foods.size}"
            )
            appendLine(
                "ALIM_GRP : ${foodGroups.size}"
            )
            appendLine(
                "COMPO    : ${compositionAudit.recordCount}"
            )
            appendLine(
                "CONST    : ${constituents.size}"
            )
            appendLine(
                "SOURCES  : ${sources.size}"
            )

            appendLine()
            appendLine(
                "ALIM INTEGRITY"
            )
            appendLine(
                "--------------"
            )
            appendLine(
                "Records                   : ${foods.size}"
            )
            appendLine(
                "Unique alim_code           : ${foodAudit.uniqueCodes}"
            )
            appendLine(
                "Duplicate alim_code groups : ${foodAudit.duplicateCodes.size}"
            )
            appendLine(
                "Duplicate alim_code records: ${foodAudit.duplicateCodes.values.sum()}"
            )
            appendLine(
                "Missing scientific names   : ${foodAudit.missingScientificNames}"
            )
            appendLine(
                "Duplicate FR name groups   : ${foodAudit.duplicateFrenchNames.size}"
            )
            appendLine(
                "Duplicate EN name groups   : ${foodAudit.duplicateEnglishNames.size}"
            )

            appendMapSamples(
                title =
                    "DUPLICATE ALIM CODES",
                values =
                    foodAudit.duplicateCodes
            )

            appendMapSamples(
                title =
                    "DUPLICATE NORMALIZED FRENCH FOOD NAMES",
                values =
                    foodAudit.duplicateFrenchNames
            )

            appendMapSamples(
                title =
                    "DUPLICATE NORMALIZED ENGLISH FOOD NAMES",
                values =
                    foodAudit.duplicateEnglishNames
            )

            appendNumericDomain(
                title =
                    "JONES FACTOR VALUE DOMAIN",
                stats =
                    foodAudit.jonesFactorStats
            )

            appendLine()
            appendLine(
                "ALIM_GRP INTEGRITY"
            )
            appendLine(
                "------------------"
            )
            appendLine(
                "Records                         : ${foodGroups.size}"
            )
            appendLine(
                "Unique taxonomy tuples          : ${groupAudit.uniqueTuples}"
            )
            appendLine(
                "Duplicate taxonomy tuple groups : ${groupAudit.duplicateTuples.size}"
            )
            appendLine(
                "Taxonomy tuples used by ALIM    : ${groupAudit.referencedTuples}"
            )
            appendLine(
                "ALIM taxonomy tuples not found  : ${groupAudit.orphanFoodTuples.size}"
            )
            appendLine(
                "Unreferenced ALIM_GRP tuples    : ${groupAudit.unreferencedGroupTuples.size}"
            )
            appendLine(
                "Duplicate FR label paths        : ${groupAudit.duplicateFrenchLabels.size}"
            )
            appendLine(
                "Duplicate EN label paths        : ${groupAudit.duplicateEnglishLabels.size}"
            )

            appendTaxonomyMapSamples(
                title =
                    "DUPLICATE TAXONOMY TUPLES",
                values =
                    groupAudit.duplicateTuples
            )

            appendTaxonomySetSamples(
                title =
                    "ALIM TAXONOMY TUPLES MISSING FROM ALIM_GRP",
                values =
                    groupAudit.orphanFoodTuples
            )

            appendTaxonomySetSamples(
                title =
                    "UNREFERENCED ALIM_GRP TAXONOMY TUPLES",
                values =
                    groupAudit.unreferencedGroupTuples
            )

            appendLine()
            appendLine(
                "CONST INTEGRITY"
            )
            appendLine(
                "---------------"
            )
            appendLine(
                "Records                     : ${constituents.size}"
            )
            appendLine(
                "Unique const_code           : ${constituentAudit.uniqueCodes}"
            )
            appendLine(
                "Duplicate const_code groups : ${constituentAudit.duplicateCodes.size}"
            )
            appendLine(
                "Missing INFOODS codes       : ${constituentAudit.missingInfoodsCodes}"
            )
            appendLine(
                "Duplicate INFOODS groups    : ${constituentAudit.duplicateInfoodsCodes.size}"
            )
            appendLine(
                "Duplicate FR names          : ${constituentAudit.duplicateFrenchNames.size}"
            )
            appendLine(
                "Duplicate EN names          : ${constituentAudit.duplicateEnglishNames.size}"
            )

            appendMapSamples(
                title =
                    "DUPLICATE CONST CODES",
                values =
                    constituentAudit.duplicateCodes
            )

            appendMapSamples(
                title =
                    "DUPLICATE INFOODS CODES",
                values =
                    constituentAudit.duplicateInfoodsCodes
            )

            appendMapSamples(
                title =
                    "DUPLICATE NORMALIZED FRENCH CONSTITUENT NAMES",
                values =
                    constituentAudit.duplicateFrenchNames
            )

            appendMapSamples(
                title =
                    "DUPLICATE NORMALIZED ENGLISH CONSTITUENT NAMES",
                values =
                    constituentAudit.duplicateEnglishNames
            )

            appendLine()
            appendLine(
                "SOURCES INTEGRITY"
            )
            appendLine(
                "-----------------"
            )
            appendLine(
                "Records                      : ${sources.size}"
            )
            appendLine(
                "Unique source_code           : ${sourceAudit.uniqueCodes}"
            )
            appendLine(
                "Duplicate source_code groups : ${sourceAudit.duplicateCodes.size}"
            )
            appendLine(
                "Missing citations            : ${sourceAudit.missingCitations}"
            )
            appendLine(
                "Duplicate citation groups    : ${sourceAudit.duplicateCitations.size}"
            )

            appendMapSamples(
                title =
                    "DUPLICATE SOURCE CODES",
                values =
                    sourceAudit.duplicateCodes
            )

            appendMapSamples(
                title =
                    "DUPLICATE SOURCE CITATIONS",
                values =
                    sourceAudit.duplicateCitations
            )

            appendLine()
            appendLine(
                "COMPO RELATIONAL INTEGRITY"
            )
            appendLine(
                "--------------------------"
            )
            appendLine(
                "Records                         : ${compositionAudit.recordCount}"
            )
            appendLine(
                "Unique (alim,const) pairs       : ${compositionAudit.uniqueFoodConstituentPairs}"
            )
            appendLine(
                "Duplicate pair groups           : ${compositionAudit.duplicatePairs.size}"
            )
            appendLine(
                "Records in duplicate pair groups: ${compositionAudit.recordsInDuplicatePairs}"
            )
            appendLine(
                "Maximum pair multiplicity       : ${compositionAudit.maximumPairMultiplicity}"
            )
            appendLine()
            appendLine(
                "Referenced ALIM codes            : ${compositionAudit.referencedFoodCodes}"
            )
            appendLine(
                "Orphan ALIM codes                : ${compositionAudit.orphanFoodCodes.size}"
            )
            appendLine(
                "Unreferenced ALIM records        : ${compositionAudit.unreferencedFoods.size}"
            )
            appendLine()
            appendLine(
                "Referenced CONST codes           : ${compositionAudit.referencedConstituentCodes}"
            )
            appendLine(
                "Orphan CONST codes               : ${compositionAudit.orphanConstituentCodes.size}"
            )
            appendLine(
                "Unreferenced CONST records       : ${compositionAudit.unreferencedConstituents.size}"
            )
            appendLine()
            appendLine(
                "Referenced SOURCES codes         : ${compositionAudit.referencedSourceCodes}"
            )
            appendLine(
                "Orphan SOURCES codes             : ${compositionAudit.orphanSourceCodes.size}"
            )
            appendLine(
                "Unreferenced SOURCES records     : ${compositionAudit.unreferencedSources.size}"
            )

            appendCompositionPairSamples(
                title =
                    "DUPLICATE (ALIM_CODE, CONST_CODE) PAIRS",
                values =
                    compositionAudit.duplicatePairs
            )

            appendMapSamples(
                title =
                    "ORPHAN ALIM CODES IN COMPO",
                values =
                    compositionAudit.orphanFoodCodes
            )

            appendMapSamples(
                title =
                    "ORPHAN CONST CODES IN COMPO",
                values =
                    compositionAudit.orphanConstituentCodes
            )

            appendMapSamples(
                title =
                    "ORPHAN SOURCE CODES IN COMPO",
                values =
                    compositionAudit.orphanSourceCodes
            )

            appendSetSamples(
                title =
                    "ALIM RECORDS NEVER REFERENCED BY COMPO",
                values =
                    compositionAudit.unreferencedFoods
            )

            appendSetSamples(
                title =
                    "CONST RECORDS NEVER REFERENCED BY COMPO",
                values =
                    compositionAudit.unreferencedConstituents
            )

            appendSetSamples(
                title =
                    "SOURCE RECORDS NEVER REFERENCED BY COMPO",
                values =
                    compositionAudit.unreferencedSources
            )

            appendLine()
            appendLine(
                "COMPO MISSING-VALUE DOMAIN"
            )
            appendLine(
                "--------------------------"
            )
            appendLine(
                "Missing code_confiance : ${compositionAudit.missingConfidenceCount}"
            )
            appendLine(
                "Missing source_code    : ${compositionAudit.missingSourceCount}"
            )
            appendLine(
                "Missing min            : ${compositionAudit.missingMinCount}"
            )
            appendLine(
                "Missing max            : ${compositionAudit.missingMaxCount}"
            )

            appendLine()
            appendLine(
                "CONFIDENCE CODE DOMAIN"
            )
            appendLine(
                "----------------------"
            )

            compositionAudit
                .confidenceCounts
                .forEach { (value, count) ->
                    appendLine(
                        "$value = $count"
                    )
                }

            appendNumericDomain(
                title =
                    "COMPO TENEUR VALUE DOMAIN",
                stats =
                    compositionAudit.valueStats
            )

            appendNumericDomain(
                title =
                    "COMPO MIN VALUE DOMAIN",
                stats =
                    compositionAudit.minStats
            )

            appendNumericDomain(
                title =
                    "COMPO MAX VALUE DOMAIN",
                stats =
                    compositionAudit.maxStats
            )

            appendLine()
            appendLine(
                "COMPO RANGE CONSISTENCY"
            )
            appendLine(
                "-----------------------"
            )
            appendLine(
                "Comparable min/max pairs : ${compositionAudit.minMaxComparable}"
            )
            appendLine(
                "min > max                : ${compositionAudit.minGreaterThanMax}"
            )
            appendLine()
            appendLine(
                "Comparable teneur/min     : ${compositionAudit.valueMinComparable}"
            )
            appendLine(
                "teneur < min              : ${compositionAudit.valueBelowMin}"
            )
            appendLine()
            appendLine(
                "Comparable teneur/max     : ${compositionAudit.valueMaxComparable}"
            )
            appendLine(
                "teneur > max              : ${compositionAudit.valueAboveMax}"
            )

            appendStringSamples(
                title =
                    "SAMPLES: MIN > MAX",
                values =
                    compositionAudit.minGreaterThanMaxSamples
            )

            appendStringSamples(
                title =
                    "SAMPLES: TENEUR < MIN",
                values =
                    compositionAudit.valueBelowMinSamples
            )

            appendStringSamples(
                title =
                    "SAMPLES: TENEUR > MAX",
                values =
                    compositionAudit.valueAboveMaxSamples
            )

            appendLine()
            appendLine(
                "AUDIT SUMMARY"
            )
            appendLine(
                "-------------"
            )
            appendLine(
                "ALIM duplicate codes       : ${foodAudit.duplicateCodes.size}"
            )
            appendLine(
                "ALIM taxonomy orphans      : ${groupAudit.orphanFoodTuples.size}"
            )
            appendLine(
                "CONST duplicate codes      : ${constituentAudit.duplicateCodes.size}"
            )
            appendLine(
                "SOURCE duplicate codes     : ${sourceAudit.duplicateCodes.size}"
            )
            appendLine(
                "COMPO orphan ALIM codes    : ${compositionAudit.orphanFoodCodes.size}"
            )
            appendLine(
                "COMPO orphan CONST codes   : ${compositionAudit.orphanConstituentCodes.size}"
            )
            appendLine(
                "COMPO orphan SOURCE codes  : ${compositionAudit.orphanSourceCodes.size}"
            )
            appendLine(
                "COMPO duplicate pairs      : ${compositionAudit.duplicatePairs.size}"
            )
            appendLine(
                "COMPO nonnumeric teneur    : ${compositionAudit.valueStats.nonNumericCount}"
            )
            appendLine(
                "COMPO min > max            : ${compositionAudit.minGreaterThanMax}"
            )
            appendLine(
                "COMPO teneur < min         : ${compositionAudit.valueBelowMin}"
            )
            appendLine(
                "COMPO teneur > max         : ${compositionAudit.valueAboveMax}"
            )
        }

    private fun StringBuilder.appendNumericDomain(
        title: String,
        stats: NumericDomainSnapshot
    ) {
        appendLine()
        appendLine(
            title
        )
        appendLine(
            "-".repeat(
                title.length
            )
        )
        appendLine(
            "Observed values       : ${stats.observedCount}"
        )
        appendLine(
            "Empty values          : ${stats.emptyCount}"
        )
        appendLine(
            "Integer lexical form  : ${stats.integerCount}"
        )
        appendLine(
            "Decimal comma         : ${stats.decimalCommaCount}"
        )
        appendLine(
            "Decimal point         : ${stats.decimalPointCount}"
        )
        appendLine(
            "Non-numeric           : ${stats.nonNumericCount}"
        )
        appendLine(
            "Negative numeric      : ${stats.negativeCount}"
        )
        appendLine(
            "Zero numeric          : ${stats.zeroCount}"
        )
        appendLine(
            "Positive numeric      : ${stats.positiveCount}"
        )
        appendLine(
            "Numeric minimum       : ${stats.minimum ?: "<none>"}"
        )
        appendLine(
            "Numeric maximum       : ${stats.maximum ?: "<none>"}"
        )

        appendStringSamples(
            title =
                "NON-NUMERIC SAMPLES",
            values =
                stats.nonNumericSamples
        )
    }

    private fun StringBuilder.appendMapSamples(
        title: String,
        values: Map<String, Int>
    ) {
        appendLine()
        appendLine(
            title
        )
        appendLine(
            "-".repeat(
                title.length
            )
        )

        if (values.isEmpty()) {
            appendLine(
                "<none>"
            )
            return
        }

        values
            .entries
            .take(
                SAMPLE_LIMIT
            )
            .forEach { (value, count) ->
                appendLine(
                    "$value = $count"
                )
            }

        if (
            values.size >
            SAMPLE_LIMIT
        ) {
            appendLine(
                "... ${values.size - SAMPLE_LIMIT} more"
            )
        }
    }

    private fun StringBuilder.appendSetSamples(
        title: String,
        values: Set<String>
    ) {
        appendLine()
        appendLine(
            title
        )
        appendLine(
            "-".repeat(
                title.length
            )
        )

        if (values.isEmpty()) {
            appendLine(
                "<none>"
            )
            return
        }

        values
            .take(
                SAMPLE_LIMIT
            )
            .forEach {
                appendLine(
                    it
                )
            }

        if (
            values.size >
            SAMPLE_LIMIT
        ) {
            appendLine(
                "... ${values.size - SAMPLE_LIMIT} more"
            )
        }
    }

    private fun StringBuilder.appendTaxonomyMapSamples(
        title: String,
        values: Map<FoodGroupKey, Int>
    ) {
        appendLine()
        appendLine(
            title
        )
        appendLine(
            "-".repeat(
                title.length
            )
        )

        if (values.isEmpty()) {
            appendLine(
                "<none>"
            )
            return
        }

        values
            .entries
            .take(
                SAMPLE_LIMIT
            )
            .forEach { (key, count) ->
                appendLine(
                    "${key.render()} = $count"
                )
            }

        if (
            values.size >
            SAMPLE_LIMIT
        ) {
            appendLine(
                "... ${values.size - SAMPLE_LIMIT} more"
            )
        }
    }

    private fun StringBuilder.appendTaxonomySetSamples(
        title: String,
        values: Set<FoodGroupKey>
    ) {
        appendLine()
        appendLine(
            title
        )
        appendLine(
            "-".repeat(
                title.length
            )
        )

        if (values.isEmpty()) {
            appendLine(
                "<none>"
            )
            return
        }

        values
            .take(
                SAMPLE_LIMIT
            )
            .forEach {
                appendLine(
                    it.render()
                )
            }

        if (
            values.size >
            SAMPLE_LIMIT
        ) {
            appendLine(
                "... ${values.size - SAMPLE_LIMIT} more"
            )
        }
    }

    private fun StringBuilder.appendCompositionPairSamples(
        title: String,
        values: Map<CompositionKey, Int>
    ) {
        appendLine()
        appendLine(
            title
        )
        appendLine(
            "-".repeat(
                title.length
            )
        )

        if (values.isEmpty()) {
            appendLine(
                "<none>"
            )
            return
        }

        values
            .entries
            .take(
                SAMPLE_LIMIT
            )
            .forEach { (key, count) ->
                appendLine(
                    "alim_code=${key.alimCode}, " +
                            "const_code=${key.constCode} = $count"
                )
            }

        if (
            values.size >
            SAMPLE_LIMIT
        ) {
            appendLine(
                "... ${values.size - SAMPLE_LIMIT} more"
            )
        }
    }

    private fun StringBuilder.appendStringSamples(
        title: String,
        values: List<String>
    ) {
        appendLine()
        appendLine(
            title
        )
        appendLine(
            "-".repeat(
                title.length
            )
        )

        if (values.isEmpty()) {
            appendLine(
                "<none>"
            )
            return
        }

        values.forEach {
            appendLine(
                it
            )
        }
    }

    private fun parseXml(
        file: File
    ): Document {
        val factory =
            DocumentBuilderFactory
                .newInstance()
                .apply {
                    isNamespaceAware = true

                    setFeatureSafely(
                        "http://apache.org/xml/features/disallow-doctype-decl",
                        true
                    )

                    setFeatureSafely(
                        "http://xml.org/sax/features/external-general-entities",
                        false
                    )

                    setFeatureSafely(
                        "http://xml.org/sax/features/external-parameter-entities",
                        false
                    )

                    setFeatureSafely(
                        "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                        false
                    )

                    isXIncludeAware =
                        false

                    isExpandEntityReferences =
                        false
                }

        return factory
            .newDocumentBuilder()
            .parse(file)
            .also {
                it.documentElement.normalize()
            }
    }

    private fun recordElements(
        document: Document,
        expectedName: String
    ): List<Element> {
        val root =
            document.documentElement

        require(
            root.tagName ==
                    "TABLE"
        ) {
            "Unexpected CIQUAL XML root element: ${root.tagName}"
        }

        val records =
            childElements(
                root
            )

        require(
            records.all {
                it.tagName ==
                        expectedName
            }
        ) {
            "Unexpected record element in CIQUAL XML. " +
                    "Expected '$expectedName'."
        }

        return records
    }

    private fun requiredText(
        parent: Element,
        childName: String
    ): String {
        val leaf =
            findDirectChild(
                parent = parent,
                childName = childName
            )

        require(
            !leaf.hasAttribute(
                "missing"
            )
        ) {
            "Required CIQUAL field '$childName' is marked missing."
        }

        return normalizeWhitespace(
            directText(
                leaf
            )
        )
    }

    private fun optionalLeaf(
        parent: Element,
        childName: String
    ): LeafValue {
        val leaf =
            findDirectChild(
                parent = parent,
                childName = childName
            )

        val missing =
            leaf.hasAttribute(
                "missing"
            )

        return LeafValue(
            value =
                normalizeWhitespace(
                    directText(
                        leaf
                    )
                ),
            isMissing =
                missing
        )
    }

    private fun findDirectChild(
        parent: Element,
        childName: String
    ): Element {
        val matches =
            childElements(
                parent
            ).filter {
                it.tagName ==
                        childName
            }

        require(
            matches.size == 1
        ) {
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

        for (
        index in
        0 until children.length
        ) {
            val node =
                children.item(index)

            if (
                node.nodeType ==
                Node.ELEMENT_NODE
            ) {
                result +=
                    node as Element
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

        for (
        index in
        0 until children.length
        ) {
            val node =
                children.item(index)

            if (
                node.nodeType ==
                Node.TEXT_NODE ||
                node.nodeType ==
                Node.CDATA_SECTION_NODE
            ) {
                result.append(
                    node.nodeValue.orEmpty()
                )
            }
        }

        return result.toString()
    }

    private fun normalizedIdentityText(
        value: String
    ): String =
        normalizeWhitespace(
            value
        ).lowercase(
            Locale.ROOT
        )

    private fun compositionDiagnostic(
        alimCode: String,
        constCode: String,
        value: String,
        min: String?,
        max: String?
    ): String =
        buildString {
            append(
                "alim_code="
            )
            append(
                alimCode
            )
            append(
                ", const_code="
            )
            append(
                constCode
            )
            append(
                ", teneur="
            )
            append(
                value
            )
            append(
                ", min="
            )
            append(
                min ?: "<missing>"
            )
            append(
                ", max="
            )
            append(
                max ?: "<missing>"
            )
        }

    private fun increment(
        map: MutableMap<String, Int>,
        key: String
    ) {
        map[key] =
            (
                    map[key]
                        ?: 0
                    ) + 1
    }

    private fun requireSourceFile(
        sourceRoot: File,
        fileName: String
    ): File =
        sourceRoot
            .resolve(
                fileName
            )
            .also { file ->
                require(file.isFile) {
                    "CIQUAL source file not found: ${file.absolutePath}"
                }

                require(file.canRead()) {
                    "CIQUAL source file is not readable: ${file.absolutePath}"
                }

                require(file.length() > 0L) {
                    "CIQUAL source file is empty: ${file.absolutePath}"
                }
            }

    private fun DocumentBuilderFactory.setFeatureSafely(
        feature: String,
        enabled: Boolean
    ) {
        try {
            setFeature(
                feature,
                enabled
            )
        } catch (
            exception: Exception
        ) {
            throw IllegalStateException(
                "Could not configure XML parser feature '$feature'.",
                exception
            )
        }
    }

    private fun resolveProjectRoot(): File {
        var current =
            File(
                requireNotNull(
                    System.getProperty(
                        "user.dir"
                    )
                ) {
                    "System property 'user.dir' is not available."
                }
            ).canonicalFile

        while (true) {
            if (
                current.resolve(
                    "settings.gradle.kts"
                ).isFile ||
                current.resolve(
                    "settings.gradle"
                ).isFile
            ) {
                return current
            }

            current =
                current.parentFile
                    ?: error(
                        "Could not resolve ShopMe project root from user.dir."
                    )
        }
    }

    private data class SourceFiles(
        val foods: File,
        val foodGroups: File,
        val compositions: File,
        val constituents: File,
        val sources: File
    )

    private data class LeafValue(
        val value: String,
        val isMissing: Boolean
    )

    private data class FoodRecord(
        val alimCode: String,
        val nameFr: String,
        val nameEn: String,
        val scientificName: LeafValue,
        val groupCode: String,
        val subgroupCode: String,
        val subSubgroupCode: String,
        val jonesFactor: String
    )

    private data class FoodGroupRecord(
        val groupCode: String,
        val groupNameFr: String,
        val groupNameEn: String,
        val subgroupCode: String,
        val subgroupNameFr: String,
        val subgroupNameEn: String,
        val subSubgroupCode: String,
        val subSubgroupNameFr: String,
        val subSubgroupNameEn: String
    ) {

        val key: FoodGroupKey
            get() =
                FoodGroupKey(
                    groupCode =
                        groupCode,
                    subgroupCode =
                        subgroupCode,
                    subSubgroupCode =
                        subSubgroupCode
                )
    }

    private data class ConstituentRecord(
        val constCode: String,
        val nameFr: String,
        val nameEn: String,
        val infoodsCode: LeafValue
    )

    private data class SourceRecord(
        val sourceCode: String,
        val citation: LeafValue
    )

    private data class FoodGroupKey(
        val groupCode: String,
        val subgroupCode: String,
        val subSubgroupCode: String
    ) : Comparable<FoodGroupKey> {

        override fun compareTo(
            other: FoodGroupKey
        ): Int =
            compareValuesBy(
                this,
                other,
                FoodGroupKey::groupCode,
                FoodGroupKey::subgroupCode,
                FoodGroupKey::subSubgroupCode
            )

        fun render(): String =
            "$groupCode/$subgroupCode/$subSubgroupCode"
    }

    private data class CompositionKey(
        val alimCode: String,
        val constCode: String
    ) : Comparable<CompositionKey> {

        override fun compareTo(
            other: CompositionKey
        ): Int =
            compareValuesBy(
                this,
                other,
                CompositionKey::alimCode,
                CompositionKey::constCode
            )
    }

    private data class FoodAudit(
        val uniqueCodes: Int,
        val duplicateCodes: Map<String, Int>,
        val duplicateFrenchNames: Map<String, Int>,
        val duplicateEnglishNames: Map<String, Int>,
        val missingScientificNames: Int,
        val jonesFactorStats: NumericDomainSnapshot
    )

    private data class FoodGroupAudit(
        val uniqueTuples: Int,
        val duplicateTuples: Map<FoodGroupKey, Int>,
        val referencedTuples: Int,
        val orphanFoodTuples: Set<FoodGroupKey>,
        val unreferencedGroupTuples: Set<FoodGroupKey>,
        val duplicateFrenchLabels: Map<String, Int>,
        val duplicateEnglishLabels: Map<String, Int>
    )

    private data class ConstituentAudit(
        val uniqueCodes: Int,
        val duplicateCodes: Map<String, Int>,
        val missingInfoodsCodes: Int,
        val duplicateInfoodsCodes: Map<String, Int>,
        val duplicateFrenchNames: Map<String, Int>,
        val duplicateEnglishNames: Map<String, Int>
    )

    private data class SourceAudit(
        val uniqueCodes: Int,
        val duplicateCodes: Map<String, Int>,
        val missingCitations: Int,
        val duplicateCitations: Map<String, Int>
    )

    private data class CompositionAudit(
        val recordCount: Int,
        val uniqueFoodConstituentPairs: Int,
        val duplicatePairs: Map<CompositionKey, Int>,
        val recordsInDuplicatePairs: Int,
        val maximumPairMultiplicity: Int,
        val orphanFoodCodes: Map<String, Int>,
        val orphanConstituentCodes: Map<String, Int>,
        val orphanSourceCodes: Map<String, Int>,
        val referencedFoodCodes: Int,
        val referencedConstituentCodes: Int,
        val referencedSourceCodes: Int,
        val unreferencedFoods: Set<String>,
        val unreferencedConstituents: Set<String>,
        val unreferencedSources: Set<String>,
        val confidenceCounts: Map<String, Int>,
        val missingConfidenceCount: Int,
        val missingSourceCount: Int,
        val missingMinCount: Int,
        val missingMaxCount: Int,
        val valueStats: NumericDomainSnapshot,
        val minStats: NumericDomainSnapshot,
        val maxStats: NumericDomainSnapshot,
        val minMaxComparable: Int,
        val minGreaterThanMax: Int,
        val minGreaterThanMaxSamples: List<String>,
        val valueMinComparable: Int,
        val valueBelowMin: Int,
        val valueBelowMinSamples: List<String>,
        val valueMaxComparable: Int,
        val valueAboveMax: Int,
        val valueAboveMaxSamples: List<String>
    )

    private class NumericDomainStats {

        private var observedCount =
            0

        private var emptyCount =
            0

        private var integerCount =
            0

        private var decimalCommaCount =
            0

        private var decimalPointCount =
            0

        private var nonNumericCount =
            0

        private var negativeCount =
            0

        private var zeroCount =
            0

        private var positiveCount =
            0

        private var minimum:
                BigDecimal? =
            null

        private var maximum:
                BigDecimal? =
            null

        private val nonNumericSamples =
            ArrayList<String>()

        fun observe(
            rawValue: String
        ): BigDecimal? {
            observedCount++

            val value =
                normalizeWhitespace(
                    rawValue
                )

            if (value.isEmpty()) {
                emptyCount++
                return null
            }

            val lexicalType =
                when {
                    INTEGER_REGEX.matches(
                        value
                    ) -> {
                        NumericLexicalType.INTEGER
                    }

                    DECIMAL_COMMA_REGEX.matches(
                        value
                    ) -> {
                        NumericLexicalType.DECIMAL_COMMA
                    }

                    DECIMAL_POINT_REGEX.matches(
                        value
                    ) -> {
                        NumericLexicalType.DECIMAL_POINT
                    }

                    else -> {
                        NumericLexicalType.NON_NUMERIC
                    }
                }

            when (lexicalType) {
                NumericLexicalType.INTEGER -> {
                    integerCount++
                }

                NumericLexicalType.DECIMAL_COMMA -> {
                    decimalCommaCount++
                }

                NumericLexicalType.DECIMAL_POINT -> {
                    decimalPointCount++
                }

                NumericLexicalType.NON_NUMERIC -> {
                    nonNumericCount++

                    addSample(
                        target =
                            nonNumericSamples,
                        value =
                            value
                    )

                    return null
                }
            }

            val parsed =
                try {
                    BigDecimal(
                        value.replace(
                            ',',
                            '.'
                        )
                    )
                } catch (
                    exception: NumberFormatException
                ) {
                    nonNumericCount++

                    addSample(
                        target =
                            nonNumericSamples,
                        value =
                            value
                    )

                    return null
                }

            when {
                parsed.signum() < 0 -> {
                    negativeCount++
                }

                parsed.signum() == 0 -> {
                    zeroCount++
                }

                else -> {
                    positiveCount++
                }
            }

            if (
                minimum == null ||
                parsed <
                requireNotNull(
                    minimum
                )
            ) {
                minimum =
                    parsed
            }

            if (
                maximum == null ||
                parsed >
                requireNotNull(
                    maximum
                )
            ) {
                maximum =
                    parsed
            }

            return parsed
        }

        fun snapshot(): NumericDomainSnapshot =
            NumericDomainSnapshot(
                observedCount =
                    observedCount,
                emptyCount =
                    emptyCount,
                integerCount =
                    integerCount,
                decimalCommaCount =
                    decimalCommaCount,
                decimalPointCount =
                    decimalPointCount,
                nonNumericCount =
                    nonNumericCount,
                negativeCount =
                    negativeCount,
                zeroCount =
                    zeroCount,
                positiveCount =
                    positiveCount,
                minimum =
                    minimum?.toPlainString(),
                maximum =
                    maximum?.toPlainString(),
                nonNumericSamples =
                    nonNumericSamples.toList()
            )
    }

    private data class NumericDomainSnapshot(
        val observedCount: Int,
        val emptyCount: Int,
        val integerCount: Int,
        val decimalCommaCount: Int,
        val decimalPointCount: Int,
        val nonNumericCount: Int,
        val negativeCount: Int,
        val zeroCount: Int,
        val positiveCount: Int,
        val minimum: String?,
        val maximum: String?,
        val nonNumericSamples: List<String>
    )

    private enum class NumericLexicalType {
        INTEGER,
        DECIMAL_COMMA,
        DECIMAL_POINT,
        NON_NUMERIC
    }

    companion object {

        private fun addSample(
            target: MutableList<String>,
            value: String
        ) {
            if (
                target.size <
                SAMPLE_LIMIT &&
                value !in target
            ) {
                target +=
                    value
            }
        }

        private fun normalizeWhitespace(
            value: String
        ): String =
            value
                .replace(
                    WHITESPACE_REGEX,
                    " "
                )
                .trim()

        private const val SAMPLE_LIMIT =
            25

        private const val MISSING_TOKEN =
            "<MISSING>"

        private const val EMPTY_TOKEN =
            "<EMPTY>"

        private val WHITESPACE_REGEX =
            Regex(
                "\\s+"
            )

        private val INTEGER_REGEX =
            Regex(
                "^[+-]?\\d+$"
            )

        private val DECIMAL_COMMA_REGEX =
            Regex(
                "^[+-]?\\d+,\\d+$"
            )

        private val DECIMAL_POINT_REGEX =
            Regex(
                "^[+-]?\\d+\\.\\d+$"
            )
    }
}
