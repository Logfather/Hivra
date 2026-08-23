package de.shopme.testing.system.tools.knowledge.him.sources.ciqual

import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.TreeMap
import javax.xml.parsers.DocumentBuilderFactory

class RunCiqualRawSourceAuditTest {

    @Test
    fun runCiqualRawSourceAudit() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val projectRoot =
            resolveProjectRoot()

        val sourceDirectory =
            projectRoot.resolve(
                "data/sources/ciqual/raw"
            )

        require(sourceDirectory.isDirectory) {
            "CIQUAL raw source directory not found: ${sourceDirectory.absolutePath}"
        }

        val expectedFileNames =
            listOf(
                "alim_2025_11_03.xml",
                "alim_grp_2025_11_03.xml",
                "compo_2025_11_03.xml",
                "const_2025_11_03.xml",
                "sources_2025_11_03.xml"
            )

        val sourceFiles =
            expectedFileNames.map { fileName ->
                sourceDirectory.resolve(fileName).also { file ->
                    require(file.isFile) {
                        "CIQUAL raw source file not found: ${file.absolutePath}"
                    }

                    require(file.canRead()) {
                        "CIQUAL raw source file is not readable: ${file.absolutePath}"
                    }

                    require(file.length() > 0L) {
                        "CIQUAL raw source file is empty: ${file.absolutePath}"
                    }
                }
            }

        val actualFileNames =
            sourceDirectory
                .listFiles()
                .orEmpty()
                .filter {
                    it.isFile &&
                            it.extension.equals(
                                "xml",
                                ignoreCase = true
                            )
                }
                .map {
                    it.name
                }
                .sorted()

        require(
            actualFileNames ==
                    expectedFileNames.sorted()
        ) {
            buildString {
                appendLine(
                    "Unexpected CIQUAL raw XML source file set."
                )
                appendLine(
                    "Expected: ${expectedFileNames.sorted()}"
                )
                appendLine(
                    "Actual  : $actualFileNames"
                )
            }
        }

        val report =
            buildString {
                appendLine(
                    "CIQUAL RAW SOURCE AUDIT"
                )
                appendLine(
                    "======================="
                )
                appendLine()
                appendLine(
                    "Source directory : ${sourceDirectory.absolutePath}"
                )
                appendLine(
                    "Files            : ${sourceFiles.size}"
                )
                appendLine(
                    "Total size       : ${sourceFiles.sumOf { it.length() }} bytes"
                )

                sourceFiles.forEachIndexed { index, file ->
                    appendLine()

                    if (index > 0) {
                        appendLine(
                            "============================================================"
                        )
                        appendLine()
                    }

                    append(
                        auditFile(file)
                    )
                }
            }

        val reportFile =
            projectRoot.resolve(
                "build/knowledge/reports/ciqual/ciqual-raw-source-audit.txt"
            )

        require(
            reportFile.parentFile.mkdirs() ||
                    reportFile.parentFile.isDirectory
        ) {
            "Could not create CIQUAL audit report directory: " +
                    reportFile.parentFile.absolutePath
        }

        reportFile.writeText(
            report,
            StandardCharsets.UTF_8
        )

        println(report)
        println(
            "CIQUAL raw source audit report written to: " +
                    reportFile.absolutePath
        )
    }

    private fun auditFile(
        file: File
    ): String {
        val document =
            parseXml(file)

        val root =
            document.documentElement

        val directChildren =
            childElements(root)

        val directChildNameCounts =
            directChildren
                .groupingBy {
                    elementName(it)
                }
                .eachCount()
                .toSortedMap()

        val recordElementName =
            determineLikelyRecordElementName(
                directChildNameCounts
            )

        val recordElements =
            recordElementName
                ?.let { expectedName ->
                    directChildren.filter {
                        elementName(it) ==
                                expectedName
                    }
                }
                .orEmpty()

        val structureCollector =
            XmlStructureCollector()

        collectStructure(
            element = root,
            path = "/${elementName(root)}",
            collector = structureCollector
        )

        val recordAnalysis =
            if (recordElements.isEmpty()) {
                null
            } else {
                analyzeRecords(
                    recordElements = recordElements,
                    rootName = elementName(root),
                    recordElementName =
                        requireNotNull(
                            recordElementName
                        )
                )
            }

        return buildString {
            appendLine(
                "FILE"
            )
            appendLine(
                "----"
            )
            appendLine(
                "Name               : ${file.name}"
            )
            appendLine(
                "Path               : ${file.absolutePath}"
            )
            appendLine(
                "Size               : ${file.length()} bytes"
            )
            appendLine(
                "XML version        : ${document.xmlVersion ?: "<unknown>"}"
            )
            appendLine(
                "XML encoding       : ${document.xmlEncoding ?: document.inputEncoding ?: "<unknown>"}"
            )
            appendLine(
                "Standalone         : ${document.xmlStandalone}"
            )
            appendLine(
                "Root element       : ${elementName(root)}"
            )
            appendLine(
                "Namespace URI      : ${displayNullable(root.namespaceURI)}"
            )
            appendLine(
                "Namespace prefix   : ${displayNullable(root.prefix)}"
            )
            appendLine(
                "Direct children    : ${directChildren.size}"
            )
            appendLine(
                "Child element types: ${directChildNameCounts.size}"
            )
            appendLine(
                "Likely record      : ${recordElementName ?: "<undetected>"}"
            )
            appendLine(
                "Record count       : ${recordElements.size}"
            )
            appendLine()

            appendLine(
                "DIRECT CHILD ELEMENTS"
            )
            appendLine(
                "---------------------"
            )

            if (directChildNameCounts.isEmpty()) {
                appendLine(
                    "<none>"
                )
            } else {
                directChildNameCounts.forEach { (name, count) ->
                    appendLine(
                        "$name = $count"
                    )
                }
            }

            appendLine()
            appendLine(
                "GLOBAL XML STRUCTURE"
            )
            appendLine(
                "--------------------"
            )
            appendLine(
                "Distinct element paths : ${structureCollector.elementOccurrences.size}"
            )
            appendLine(
                "Distinct attribute paths: ${structureCollector.attributeOccurrences.size}"
            )
            appendLine(
                "Text-bearing paths     : ${structureCollector.textValueCounts.size}"
            )
            appendLine(
                "Empty leaf values      : ${structureCollector.emptyValueCount}"
            )
            appendLine(
                "Whitespace-only leaves : ${structureCollector.whitespaceOnlyValueCount}"
            )
            appendLine(
                "CDATA nodes            : ${structureCollector.cdataNodeCount}"
            )
            appendLine(
                "Mixed-content elements : ${structureCollector.mixedContentElementCount}"
            )

            appendLine()
            appendLine(
                "ELEMENT PATHS"
            )
            appendLine(
                "-------------"
            )

            if (structureCollector.elementOccurrences.isEmpty()) {
                appendLine(
                    "<none>"
                )
            } else {
                structureCollector
                    .elementOccurrences
                    .forEach { (path, count) ->
                        appendLine(
                            "$path = $count"
                        )
                    }
            }

            appendLine()
            appendLine(
                "ATTRIBUTES"
            )
            appendLine(
                "----------"
            )

            if (structureCollector.attributeOccurrences.isEmpty()) {
                appendLine(
                    "<none>"
                )
            } else {
                structureCollector
                    .attributeOccurrences
                    .forEach { (path, count) ->
                        appendLine(
                            "$path = $count"
                        )
                    }
            }

            appendLine()
            appendLine(
                "SAMPLE VALUES BY LEAF PATH"
            )
            appendLine(
                "--------------------------"
            )

            if (structureCollector.sampleValues.isEmpty()) {
                appendLine(
                    "<none>"
                )
            } else {
                structureCollector
                    .sampleValues
                    .forEach { (path, values) ->
                        appendLine(
                            path
                        )

                        values.forEachIndexed { index, value ->
                            appendLine(
                                "  [${index + 1}] $value"
                            )
                        }
                    }
            }

            if (recordAnalysis != null) {
                appendLine()
                appendLine(
                    "RECORD FIELD COVERAGE"
                )
                appendLine(
                    "---------------------"
                )

                recordAnalysis
                    .pathStats
                    .forEach { (path, stats) ->
                        appendLine(
                            buildString {
                                append(path)
                                append(" | records=")
                                append(
                                    stats.recordsContainingPath
                                )
                                append("/")
                                append(
                                    recordAnalysis.recordCount
                                )
                                append(" | coverage=")
                                append(
                                    formatPercentage(
                                        numerator =
                                            stats.recordsContainingPath,
                                        denominator =
                                            recordAnalysis.recordCount
                                    )
                                )
                                append("%")
                                append(" | occurrences=")
                                append(
                                    stats.totalOccurrences
                                )
                                append(" | min/record=")
                                append(
                                    stats.minimumOccurrencesPerRecord
                                )
                                append(" | max/record=")
                                append(
                                    stats.maximumOccurrencesPerRecord
                                )
                                append(" | empty=")
                                append(
                                    stats.emptyValueCount
                                )
                                append(" | whitespace=")
                                append(
                                    stats.whitespaceOnlyValueCount
                                )
                            }
                        )
                    }

                appendLine()
                appendLine(
                    "RECORD ATTRIBUTE COVERAGE"
                )
                appendLine(
                    "-------------------------"
                )

                if (recordAnalysis.attributeStats.isEmpty()) {
                    appendLine(
                        "<none>"
                    )
                } else {
                    recordAnalysis
                        .attributeStats
                        .forEach { (path, stats) ->
                            appendLine(
                                buildString {
                                    append(path)
                                    append(" | records=")
                                    append(
                                        stats.recordsContainingPath
                                    )
                                    append("/")
                                    append(
                                        recordAnalysis.recordCount
                                    )
                                    append(" | coverage=")
                                    append(
                                        formatPercentage(
                                            numerator =
                                                stats.recordsContainingPath,
                                            denominator =
                                                recordAnalysis.recordCount
                                        )
                                    )
                                    append("%")
                                    append(" | occurrences=")
                                    append(
                                        stats.totalOccurrences
                                    )
                                    append(" | min/record=")
                                    append(
                                        stats.minimumOccurrencesPerRecord
                                    )
                                    append(" | max/record=")
                                    append(
                                        stats.maximumOccurrencesPerRecord
                                    )
                                }
                            )
                        }
                }

                appendLine()
                appendLine(
                    "RECORD STRUCTURAL SIGNATURES"
                )
                appendLine(
                    "----------------------------"
                )
                appendLine(
                    "Distinct signatures       : ${recordAnalysis.structuralSignatureCounts.size}"
                )
                appendLine(
                    "Repeated signature groups : ${recordAnalysis.repeatedStructuralSignatureGroups}"
                )
                appendLine(
                    "Records in repeated groups: ${recordAnalysis.recordsInRepeatedStructuralSignatureGroups}"
                )

                appendLine()
                appendLine(
                    "FIRST RECORD"
                )
                appendLine(
                    "------------"
                )
                appendLine(
                    recordAnalysis.firstRecord
                )

                appendLine()
                appendLine(
                    "LAST RECORD"
                )
                appendLine(
                    "-----------"
                )
                appendLine(
                    recordAnalysis.lastRecord
                )
            }

            appendLine()
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

                    setAttributeSafely(
                        ACCESS_EXTERNAL_DTD,
                        ""
                    )

                    setAttributeSafely(
                        ACCESS_EXTERNAL_SCHEMA,
                        ""
                    )

                    isXIncludeAware = false
                    isExpandEntityReferences = false
                }

        val builder =
            factory.newDocumentBuilder()

        return builder
            .parse(file)
            .also {
                it.documentElement.normalize()
            }
    }

    private fun analyzeRecords(
        recordElements: List<Element>,
        rootName: String,
        recordElementName: String
    ): RecordAnalysis {
        val perPathCountsByRecord =
            TreeMap<String, IntArray>()

        val perAttributeCountsByRecord =
            TreeMap<String, IntArray>()

        val pathEmptyCounts =
            TreeMap<String, Int>()

        val pathWhitespaceCounts =
            TreeMap<String, Int>()

        val structuralSignatureCounts =
            TreeMap<String, Int>()

        recordElements.forEachIndexed { recordIndex, record ->
            val pathCounts =
                TreeMap<String, Int>()

            val attributeCounts =
                TreeMap<String, Int>()

            collectRecordStructure(
                element = record,
                path =
                    "/$rootName/$recordElementName",
                pathCounts = pathCounts,
                attributeCounts = attributeCounts,
                emptyCounts = pathEmptyCounts,
                whitespaceCounts = pathWhitespaceCounts
            )

            pathCounts.forEach { (path, count) ->
                perPathCountsByRecord
                    .getOrPut(path) {
                        IntArray(
                            recordElements.size
                        )
                    }[recordIndex] =
                    count
            }

            attributeCounts.forEach { (path, count) ->
                perAttributeCountsByRecord
                    .getOrPut(path) {
                        IntArray(
                            recordElements.size
                        )
                    }[recordIndex] =
                    count
            }

            val structuralSignature =
                buildStructuralSignature(
                    pathCounts = pathCounts,
                    attributeCounts = attributeCounts
                )

            structuralSignatureCounts[
                structuralSignature
            ] =
                (
                        structuralSignatureCounts[
                            structuralSignature
                        ] ?: 0
                        ) + 1
        }

        val pathStats =
            perPathCountsByRecord
                .mapValues { (path, counts) ->
                    RecordPathStats(
                        recordsContainingPath =
                            counts.count {
                                it > 0
                            },
                        totalOccurrences =
                            counts.sum(),
                        minimumOccurrencesPerRecord =
                            counts.minOrNull() ?: 0,
                        maximumOccurrencesPerRecord =
                            counts.maxOrNull() ?: 0,
                        emptyValueCount =
                            pathEmptyCounts[path] ?: 0,
                        whitespaceOnlyValueCount =
                            pathWhitespaceCounts[path] ?: 0
                    )
                }
                .toSortedMap()

        val attributeStats =
            perAttributeCountsByRecord
                .mapValues { (_, counts) ->
                    RecordAttributeStats(
                        recordsContainingPath =
                            counts.count {
                                it > 0
                            },
                        totalOccurrences =
                            counts.sum(),
                        minimumOccurrencesPerRecord =
                            counts.minOrNull() ?: 0,
                        maximumOccurrencesPerRecord =
                            counts.maxOrNull() ?: 0
                    )
                }
                .toSortedMap()

        val repeatedSignatureCounts =
            structuralSignatureCounts
                .values
                .filter {
                    it > 1
                }

        return RecordAnalysis(
            recordCount =
                recordElements.size,
            pathStats =
                pathStats,
            attributeStats =
                attributeStats,
            structuralSignatureCounts =
                structuralSignatureCounts,
            repeatedStructuralSignatureGroups =
                repeatedSignatureCounts.size,
            recordsInRepeatedStructuralSignatureGroups =
                repeatedSignatureCounts.sum(),
            firstRecord =
                serializeElement(
                    recordElements.first()
                ),
            lastRecord =
                serializeElement(
                    recordElements.last()
                )
        )
    }

    private fun collectStructure(
        element: Element,
        path: String,
        collector: XmlStructureCollector
    ) {
        collector.elementOccurrences[path] =
            (
                    collector.elementOccurrences[path] ?: 0
                    ) + 1

        for (
        attributeIndex in
        0 until element.attributes.length
        ) {
            val attribute =
                element.attributes.item(
                    attributeIndex
                )

            val attributePath =
                "$path/@${attribute.nodeName}"

            collector.attributeOccurrences[
                attributePath
            ] =
                (
                        collector.attributeOccurrences[
                            attributePath
                        ] ?: 0
                        ) + 1
        }

        val children =
            childElements(
                element
            )

        val directTextNodes =
            directTextNodes(
                element
            )

        val hasNonWhitespaceText =
            directTextNodes.any {
                it.nodeType == Node.TEXT_NODE &&
                        it.nodeValue.orEmpty().isNotBlank()
            }

        val cdataNodes =
            directTextNodes.filter {
                it.nodeType ==
                        Node.CDATA_SECTION_NODE
            }

        if (
            children.isNotEmpty() &&
            (
                    hasNonWhitespaceText ||
                            cdataNodes.isNotEmpty()
                    )
        ) {
            collector.mixedContentElementCount++
        }

        collector.cdataNodeCount +=
            cdataNodes.size

        if (children.isEmpty()) {
            val rawValue =
                directText(
                    element
                )

            when {
                rawValue.isEmpty() -> {
                    collector.emptyValueCount++
                }

                rawValue.isBlank() -> {
                    collector.whitespaceOnlyValueCount++
                }

                else -> {
                    collector.textValueCounts[path] =
                        (
                                collector.textValueCounts[path]
                                    ?: 0
                                ) + 1

                    val sample =
                        normalizeSampleValue(
                            rawValue
                        )

                    if (sample.isNotEmpty()) {
                        val samples =
                            collector.sampleValues
                                .getOrPut(path) {
                                    mutableListOf()
                                }

                        if (
                            sample !in samples &&
                            samples.size <
                            SAMPLE_VALUES_PER_PATH
                        ) {
                            samples +=
                                sample
                        }
                    }
                }
            }
        }

        children.forEach { child ->
            collectStructure(
                element = child,
                path =
                    "$path/${elementName(child)}",
                collector = collector
            )
        }
    }

    private fun collectRecordStructure(
        element: Element,
        path: String,
        pathCounts: MutableMap<String, Int>,
        attributeCounts: MutableMap<String, Int>,
        emptyCounts: MutableMap<String, Int>,
        whitespaceCounts: MutableMap<String, Int>
    ) {
        pathCounts[path] =
            (
                    pathCounts[path] ?: 0
                    ) + 1

        for (
        attributeIndex in
        0 until element.attributes.length
        ) {
            val attribute =
                element.attributes.item(
                    attributeIndex
                )

            val attributePath =
                "$path/@${attribute.nodeName}"

            attributeCounts[
                attributePath
            ] =
                (
                        attributeCounts[
                            attributePath
                        ] ?: 0
                        ) + 1
        }

        val children =
            childElements(
                element
            )

        if (children.isEmpty()) {
            val rawValue =
                directText(
                    element
                )

            when {
                rawValue.isEmpty() -> {
                    emptyCounts[path] =
                        (
                                emptyCounts[path] ?: 0
                                ) + 1
                }

                rawValue.isBlank() -> {
                    whitespaceCounts[path] =
                        (
                                whitespaceCounts[path]
                                    ?: 0
                                ) + 1
                }
            }
        }

        children.forEach { child ->
            collectRecordStructure(
                element = child,
                path =
                    "$path/${elementName(child)}",
                pathCounts = pathCounts,
                attributeCounts =
                    attributeCounts,
                emptyCounts = emptyCounts,
                whitespaceCounts =
                    whitespaceCounts
            )
        }
    }

    private fun buildStructuralSignature(
        pathCounts: Map<String, Int>,
        attributeCounts: Map<String, Int>
    ): String =
        buildString {
            append(
                "ELEMENTS["
            )

            append(
                pathCounts
                    .entries
                    .joinToString(
                        separator = "|"
                    ) { (path, count) ->
                        "$path=$count"
                    }
            )

            append(
                "]ATTRIBUTES["
            )

            append(
                attributeCounts
                    .entries
                    .joinToString(
                        separator = "|"
                    ) { (path, count) ->
                        "$path=$count"
                    }
            )

            append(
                "]"
            )
        }

    private fun determineLikelyRecordElementName(
        directChildNameCounts: Map<String, Int>
    ): String? {
        if (directChildNameCounts.isEmpty()) {
            return null
        }

        return directChildNameCounts
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> {
                    it.value
                }.thenBy {
                    it.key
                }
            )
            .first()
            .key
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

    private fun directTextNodes(
        element: Element
    ): List<Node> {
        val result =
            ArrayList<Node>()

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
                result +=
                    node
            }
        }

        return result
    }

    private fun directText(
        element: Element
    ): String =
        directTextNodes(
            element
        ).joinToString(
            separator = ""
        ) {
            it.nodeValue.orEmpty()
        }

    private fun elementName(
        element: Element
    ): String =
        element.localName
            ?: element.tagName

    private fun serializeElement(
        element: Element
    ): String =
        buildString {
            appendSerializedElement(
                builder = this,
                element = element,
                depth = 0
            )
        }.trimEnd()

    private fun appendSerializedElement(
        builder: StringBuilder,
        element: Element,
        depth: Int
    ) {
        val indentation =
            "  ".repeat(
                depth
            )

        builder.append(
            indentation
        )

        builder.append(
            "<${element.tagName}"
        )

        val attributes =
            (0 until element.attributes.length)
                .map {
                    element.attributes.item(it)
                }
                .sortedBy {
                    it.nodeName
                }

        attributes.forEach { attribute ->
            builder.append(
                " ${attribute.nodeName}=\"${escapeXml(attribute.nodeValue.orEmpty())}\""
            )
        }

        val children =
            childElements(
                element
            )

        val text =
            directText(
                element
            )

        if (
            children.isEmpty() &&
            text.isEmpty()
        ) {
            builder.append(
                "/>"
            )
            return
        }

        builder.append(
            ">"
        )

        if (children.isEmpty()) {
            builder.append(
                escapeXml(
                    normalizeSerializedValue(
                        text
                    )
                )
            )

            builder.append(
                "</${element.tagName}>"
            )

            return
        }

        val normalizedText =
            normalizeSerializedValue(
                text
            )

        if (normalizedText.isNotEmpty()) {
            builder.append(
                escapeXml(
                    normalizedText
                )
            )
        }

        builder.appendLine()

        children.forEachIndexed { index, child ->
            appendSerializedElement(
                builder = builder,
                element = child,
                depth = depth + 1
            )

            if (
                index <
                children.lastIndex
            ) {
                builder.appendLine()
            }
        }

        builder.appendLine()
        builder.append(
            indentation
        )
        builder.append(
            "</${element.tagName}>"
        )
    }

    private fun normalizeSerializedValue(
        value: String
    ): String =
        value
            .replace(
                WHITESPACE_REGEX,
                " "
            )
            .trim()

    private fun normalizeSampleValue(
        value: String
    ): String {
        val normalized =
            value
                .replace(
                    WHITESPACE_REGEX,
                    " "
                )
                .trim()

        return if (
            normalized.length <=
            MAX_SAMPLE_VALUE_LENGTH
        ) {
            normalized
        } else {
            normalized.take(
                MAX_SAMPLE_VALUE_LENGTH
            ) + "…"
        }
    }

    private fun escapeXml(
        value: String
    ): String =
        value
            .replace(
                "&",
                "&amp;"
            )
            .replace(
                "<",
                "&lt;"
            )
            .replace(
                ">",
                "&gt;"
            )
            .replace(
                "\"",
                "&quot;"
            )
            .replace(
                "'",
                "&apos;"
            )

    private fun displayNullable(
        value: String?
    ): String =
        value
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "<none>"

    private fun formatPercentage(
        numerator: Int,
        denominator: Int
    ): String {
        if (denominator == 0) {
            return "0.00"
        }

        return String.format(
            Locale.ROOT,
            "%.2f",
            numerator.toDouble() /
                    denominator.toDouble() *
                    100.0
        )
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

    private fun DocumentBuilderFactory.setAttributeSafely(
        name: String,
        value: String
    ) {
        try {
            setAttribute(
                name,
                value
            )
        } catch (
            exception: IllegalArgumentException
        ) {
            throw IllegalStateException(
                "Could not configure XML parser attribute '$name'.",
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

            val parent =
                current.parentFile
                    ?: error(
                        "Could not resolve ShopMe project root from user.dir."
                    )

            current =
                parent
        }
    }

    private data class XmlStructureCollector(
        val elementOccurrences:
        MutableMap<String, Int> =
            TreeMap(),
        val attributeOccurrences:
        MutableMap<String, Int> =
            TreeMap(),
        val textValueCounts:
        MutableMap<String, Int> =
            TreeMap(),
        val sampleValues:
        MutableMap<String, MutableList<String>> =
            TreeMap(),
        var emptyValueCount: Int = 0,
        var whitespaceOnlyValueCount: Int = 0,
        var cdataNodeCount: Int = 0,
        var mixedContentElementCount: Int = 0
    )

    private data class RecordAnalysis(
        val recordCount: Int,
        val pathStats:
        Map<String, RecordPathStats>,
        val attributeStats:
        Map<String, RecordAttributeStats>,
        val structuralSignatureCounts:
        Map<String, Int>,
        val repeatedStructuralSignatureGroups:
        Int,
        val recordsInRepeatedStructuralSignatureGroups:
        Int,
        val firstRecord: String,
        val lastRecord: String
    )

    private data class RecordPathStats(
        val recordsContainingPath: Int,
        val totalOccurrences: Int,
        val minimumOccurrencesPerRecord: Int,
        val maximumOccurrencesPerRecord: Int,
        val emptyValueCount: Int,
        val whitespaceOnlyValueCount: Int
    )

    private data class RecordAttributeStats(
        val recordsContainingPath: Int,
        val totalOccurrences: Int,
        val minimumOccurrencesPerRecord: Int,
        val maximumOccurrencesPerRecord: Int
    )

    companion object {

        private const val ACCESS_EXTERNAL_DTD =
            "http://javax.xml.XMLConstants/property/accessExternalDTD"

        private const val ACCESS_EXTERNAL_SCHEMA =
            "http://javax.xml.XMLConstants/property/accessExternalSchema"

        private const val SAMPLE_VALUES_PER_PATH =
            5

        private const val MAX_SAMPLE_VALUE_LENGTH =
            240

        private val WHITESPACE_REGEX =
            Regex(
                "\\s+"
            )
    }
}
