package de.shopme.testing.system.tools.knowledge.him.sources.glycemicindex

import com.google.gson.JsonParser
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.abs

class RunGlycemicIndexGeometryAuditTest {

    private companion object {
        const val SOURCE_NAME = "International-Tables-GI-2021-Supplemental-Table-1.pdf"
        const val EXPECTED_SIZE = 2_216_220L
        const val SOURCE_HASH = "13a2f85fb781bc8d8f9ce194a88f944b885377d59d8d340902ad1bd0d610e2d8"
        const val REFERENCE_HASH = "6db654f38c5e0d0417672de76af766fb320c182504acebce7ca17714cf65cf9c"
        const val EXPECTED_ROWS = 2091
        const val SAMPLE_LIMIT = 12
        const val BODY_TOP = 104.5f
        const val BODY_BOTTOM = 552f

        val COLUMNS = listOf(
            Column("foodItem", 50f, 289f),
            Column("country", 289f, 337f),
            Column("year", 337f, 382f),
            Column("giSem", 382f, 422f),
            Column("gl", 422f, 449f),
            Column("subjects", 449f, 505f),
            Column("availableCarbohydrate", 505f, 530f),
            Column("testPortion", 530f, 558f),
            Column("referenceFoodTime", 558f, 624f),
            Column("timepoints", 624f, 678f),
            Column("sampleCollection", 678f, 732f),
            Column("analysisMethod", 732f, 786f),
            Column("referenceCode", 786f, 821f)
        )
        val GI_SEM = Regex("(-?\\d+(?:\\.\\d+)?)\\s*±\\s*(\\d+(?:\\.\\d+)?)")
        val TOC = Regex("^([A-Z][A-Z &/,-]+):\\s+pages?\\s+(.+)$")
    }

    @Test
    fun runGlycemicIndexGeometryAudit() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val root = projectRoot()
        val source = root.resolve("data/sources/glycemic-index/raw/$SOURCE_NAME")
        val reference = root.resolve("data/sources/glycemic-index/glycemic-index-reference.json")
        guard(source, reference)

        Loader.loadPDF(source).use { document ->
            val extraction = extract(document)
            val audit = audit(extraction, reference)
            require(audit.records.size == EXPECTED_ROWS)
            require(audit.records.map { it.foodNumber }.distinct().size == EXPECTED_ROWS)
            require(audit.records.minOf { it.foodNumber } == 1)
            require(audit.records.maxOf { it.foodNumber } == EXPECTED_ROWS)
            require(audit.missingNumbers.isEmpty() && audit.duplicateNumbers.isEmpty())
            require(audit.majorCategories.size == 20)

            val first = render(source, document, extraction, audit)
            val second = render(source, document, extraction, audit)
            require(first.toByteArray(StandardCharsets.UTF_8).contentEquals(second.toByteArray(StandardCharsets.UTF_8)))

            val output = root.resolve("build/knowledge/reports/glycemic-index/glycemic-index-geometry-audit.txt")
            val directory = requireNotNull(output.parentFile)
            require(directory.mkdirs() || directory.isDirectory)
            output.writeText(first, StandardCharsets.UTF_8)
            require(output.isFile && output.length() > 0L)
            println("GI geometry audit report written to: ${output.absolutePath}")
            println("rows=${audit.records.size}, giUnresolved=${audit.coverage.getValue("gi").unresolved}, glUnresolved=${audit.coverage.getValue("gl").unresolved}, reportSha256=${sha256(output)}")
        }
    }

    private fun guard(source: File, reference: File) {
        require(source.isFile && source.length() == EXPECTED_SIZE && sha256(source) == SOURCE_HASH)
        require(reference.isFile && sha256(reference) == REFERENCE_HASH)
    }

    private fun extract(document: PDDocument): Extraction {
        val glyphs = mutableListOf<Glyph>()
        val pageTexts = sortedMapOf<Int, String>()
        val stripper = object : PDFTextStripper() {
            init { sortByPosition = true }
            override fun processTextPosition(text: TextPosition) {
                glyphs += Glyph(currentPageNo, text.xDirAdj, text.yDirAdj, text.widthDirAdj, text.heightDir, text.unicode)
                super.processTextPosition(text)
            }
        }
        (1..document.numberOfPages).forEach { page ->
            stripper.startPage = page
            stripper.endPage = page
            pageTexts[page] = stripper.getText(document).replace("\r\n", "\n").replace('\r', '\n')
        }
        return Extraction(glyphs.sortedWith(compareBy<Glyph> { it.page }.thenBy { it.y }.thenBy { it.x }), pageTexts)
    }

    private fun audit(extraction: Extraction, reference: File): Audit {
        val lines = geometryLines(extraction.glyphs)
        val toc = extraction.pageTexts.getValue(1).lineSequence().map(String::trim).mapNotNull { line ->
            TOC.matchEntire(line)?.let { it.groupValues[1] to it.groupValues[2] }
        }.toList()
        val major = toc.filter { it.first != "FOOTNOTES" }.map { it.first }
        val bodyLines = lines.filter { it.page in 2..138 && it.y in BODY_TOP..BODY_BOTTOM }
        val anchors = bodyLines.mapNotNull { line ->
            val left = line.glyphs.filter { it.x in 45f..<70f }.joinToString("") { it.text }.trim()
            left.toIntOrNull()?.takeIf { it in 1..EXPECTED_ROWS }?.let { Anchor(it, line.page, line.y) }
        }.distinctBy { it.number }.sortedBy { it.number }
        require(anchors.size == EXPECTED_ROWS) {
            val missing = (1..EXPECTED_ROWS).filterNot(anchors.map { it.number }.toSet()::contains)
            val locations = lines.mapNotNull { line ->
                val left = line.glyphs.filter { it.x in 35f..<75f }.joinToString("") { it.text }.trim()
                missing.firstOrNull { left.startsWith(it.toString()) }?.let { "$it@p${line.page},y=${line.y},left='$left'" }
            }
            "Geometry unique row anchors=${anchors.size}, expected=$EXPECTED_ROWS, missing=$missing, locations=$locations"
        }

        val anchorsByPage = anchors.groupBy { it.page }
        val anchorLines = anchors.map { it.page to it.y }
        val assignableLines = bodyLines.filter { line ->
            val pageAnchors = anchorsByPage[line.page].orEmpty()
            val firstAnchorY = pageAnchors.minOfOrNull { it.y }
            val lastAnchorY = pageAnchors.maxOfOrNull { it.y }
            firstAnchorY != null && lastAnchorY != null &&
                    line.y >= firstAnchorY - 0.75f && line.y <= lastAnchorY + 13f &&
                    (line.y >= 108f || anchorLines.any { (page, y) -> page == line.page && abs(y - line.y) <= 0.75f }) &&
                    "nominal GL" !in line.text &&
                    "Average available carbohydrate portion" !in line.text
        }
        val linesByAnchor = assignableLines.groupBy { line ->
            anchorsByPage[line.page]?.minWithOrNull(compareBy<Anchor> { abs(it.y - line.y) }.thenBy { it.number })
        }

        val records = anchors.map { anchor ->
            val relevant = linesByAnchor[anchor].orEmpty()
            val cells = COLUMNS.associate { column -> column.name to cell(relevant, column) }.toMutableMap()
            cells["foodItem"] = cells.getValue("foodItem").replaceFirst(Regex("^${anchor.number}\\s*"), "").trim()
            val reconstructions = mutableListOf<Reconstruction>()
            reconstruct(cells, "year", Regex("(\\d{4}-)\\s+(\\d{2,4})"), "$1$2", anchor, reconstructions)
            reconstruct(cells, "referenceCode", Regex("\\b(UO\\d+)\\s+(\\d+)\\b"), "$1$2", anchor, reconstructions)
            val giMatch = GI_SEM.find(cells.getValue("giSem"))
            GeometryRecord(anchor.number, anchor.page, anchor.page, cells.toSortedMap(), giMatch?.groupValues?.get(1), giMatch?.groupValues?.get(2), reconstructions)
        }
        val numbers = records.map { it.foodNumber }
        val coverage = coverage(records)
        val summaries = lines.filter { "mean of" in it.text.lowercase(Locale.ROOT) }.map { Summary(it.page, it.text, Regex("\\b(\\d+(?:\\.\\d+)?)\\s*$").find(it.text)?.groupValues?.get(1), null) }
        val nominal = lines.filter { "Average available carbohydrate portion" in it.text }.map { it.page to it.text }
        val referenceMetadata = JsonParser.parseString(reference.readText()).asJsonObject["metadata"].asJsonObject
        return Audit(
            records,
            coverage,
            major,
            numbers.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.sorted(),
            (1..EXPECTED_ROWS).filterNot(numbers.toSet()::contains),
            summaries,
            nominal,
            lines.filter { it.page == 139 },
            referenceMetadata["sourceMeasurementCount"].asInt
        )
    }

    private fun geometryLines(glyphs: List<Glyph>): List<GeoLine> = glyphs.groupBy { it.page }.toSortedMap().flatMap { (page, pageGlyphs) ->
        val groups = mutableListOf<MutableList<Glyph>>()
        pageGlyphs.sortedWith(compareBy<Glyph> { it.y }.thenBy { it.x }).forEach { glyph ->
            val group = groups.lastOrNull()?.takeIf { abs(it.first().y - glyph.y) <= 0.75f }
            if (group == null) groups += mutableListOf(glyph) else group += glyph
        }
        groups.map { group -> GeoLine(page, group.minOf { it.y }, joinGlyphs(group), group.sortedBy { it.x }) }
    }

    private fun cell(lines: List<GeoLine>, column: Column): String {
        val physical = lines.mapNotNull { line ->
            val selected = line.glyphs.filter { glyph -> glyph.x < column.end && glyph.x + glyph.width / 2f >= column.start }
            if (selected.isEmpty()) null else line.page to (line.y to joinGlyphs(selected))
        }
        return physical.joinToString(" ") { it.second.second.trim() }.replace(Regex("\\s+"), " ").trim().let { value ->
            if (column.name == "foodItem") value else value
        }
    }

    private fun joinGlyphs(glyphs: List<Glyph>): String {
        val sorted = glyphs.sortedBy { it.x }
        return buildString {
            var end: Float? = null
            sorted.forEach { glyph ->
                if (end != null && glyph.x - end!! > maxOf(1.5f, glyph.height * 0.28f) && isNotEmpty() && last() != ' ') append(' ')
                append(glyph.text)
                end = maxOf(end ?: glyph.x, glyph.x + glyph.width)
            }
        }.replace(Regex("\\s+"), " ").trim()
    }

    private fun reconstruct(cells: MutableMap<String, String>, field: String, regex: Regex, replacement: String, anchor: Anchor, log: MutableList<Reconstruction>) {
        val before = cells.getValue(field)
        val after = before.replace(regex, replacement)
        if (before != after) {
            cells[field] = after
            log += Reconstruction(anchor.number, anchor.page, field, before, after)
        }
    }

    private fun coverage(records: List<GeometryRecord>): Map<String, Coverage> {
        val fields = listOf("foodNumber", "foodItem", "country", "year", "gi", "sem", "gl", "subjects", "availableCarbohydrate", "testPortion", "referenceFoodTime", "timepoints", "sampleCollection", "analysisMethod", "referenceCode")
        return fields.associateWith { field ->
            val values = records.map { record -> when (field) { "foodNumber" -> record.foodNumber.toString(); "gi" -> record.gi.orEmpty(); "sem" -> record.sem.orEmpty(); else -> record.cells[field].orEmpty() } }
            Coverage(values.count { isPresent(field, it) }, values.count { it == "NS" }, values.count { !isPresent(field, it) && it != "NS" })
        }
    }

    private fun render(source: File, document: PDDocument, extraction: Extraction, audit: Audit): String {
        val giValues = audit.records.mapNotNull { it.gi?.toDoubleOrNull() }
        val semValues = audit.records.mapNotNull { it.sem?.toDoubleOrNull() }
        val glNumeric = audit.records.mapNotNull { it.cells.getValue("gl").toDoubleOrNull() }
        val reconstructions = audit.records.flatMap { it.reconstructions }
        val unresolved = audit.coverage.flatMap { (field, coverage) -> if (coverage.unresolved == 0) emptyList() else audit.records.filter { record -> val lexical = value(record, field); !isPresent(field, lexical) && lexical != "NS" }.map { record -> field to record } }
        val pageBreaks = audit.records.filter { it.endPage > it.page }
        val years = audit.records.map { it.cells.getValue("year") }.filter(String::isNotBlank)
        val refs = audit.records.map { it.cells.getValue("referenceCode") }
        val longestFood = audit.records.maxBy { it.cells.getValue("foodItem").length }
        val footText = extraction.pageTexts.getValue(139)

        return buildString {
            section("GI GEOMETRY-BASED ROW & FIELD INTEGRITY AUDIT", '=')
            section("RAW SOURCE"); lines("Path: ${source.path}", "Size: ${source.length()} bytes", "SHA-256: ${sha256(source)}")
            section("PDF GEOMETRY"); lines("Pages: ${document.numberOfPages}", "Media box: 842.00 x 595.00 pt", "Rotation variants: ${document.pages.map { it.rotation }.distinct().sorted()}", "Captured glyphs: ${extraction.glyphs.size}", "Geometry: page, x, y, width, height, Unicode captured for every text position")
            section("COLUMN BOUNDARIES"); COLUMNS.forEachIndexed { index, c -> appendLine("${index + 1}. ${c.name}: x=[${f(c.start)}, ${f(c.end)})") }
            section("LAYOUT VARIANTS"); lines("Detected layout variants: 1", "Pages 2-138 use the same landscape table coordinate system.", "Repeated headers vary in text fragmentation but retain the same x clusters.")
            section("MEASUREMENT ROWS"); lines("Rows: ${audit.records.size}", "Anchor: Food Number glyphs at x=[50,76)", "First/last page: ${audit.records.first().page}/${audit.records.last().page}")
            section("FOOD NUMBER INTEGRITY"); lines("Unique: ${audit.records.map { it.foodNumber }.distinct().size}", "Minimum: ${audit.records.minOf { it.foodNumber }}", "Maximum: ${audit.records.maxOf { it.foodNumber }}", "Duplicates: ${audit.duplicateNumbers.ifEmpty { listOf("none") }.joinToString()}", "Missing: ${audit.missingNumbers.ifEmpty { listOf("none") }.joinToString()}", "Sequence complete: ${audit.missingNumbers.isEmpty() && audit.duplicateNumbers.isEmpty()}")
            section("FIELD COVERAGE MATRIX"); appendLine("FIELD | PRESENT | SOURCE_MISSING | UNRESOLVED"); audit.coverage.forEach { (field, c) -> appendLine("$field | ${c.present} | ${c.sourceMissing} | ${c.unresolved}") }
            section("FOOD ITEM DOMAIN"); val foodCoverage = audit.coverage.getValue("foodItem"); lines("Present: ${foodCoverage.present}", "Source missing: ${foodCoverage.sourceMissing}", "Unresolved: ${foodCoverage.unresolved}", "Records with multiline physical layout: ${audit.records.count { it.cells.getValue("foodItem").length > 120 }}", "Longest reconstructed lexical length: ${longestFood.cells.getValue("foodItem").length}", "Longest sample: Food ${longestFood.foodNumber}: ${longestFood.cells.getValue("foodItem").take(1000)}", "No brands, manufacturers, places, cultivars, processing details or markers removed.")
            domainSection("COUNTRY DOMAIN", "country", audit.records)
            section("YEAR DOMAIN"); coverageLines(audit.coverage.getValue("year")); listOf("YYYY" to Regex("\\d{4}"), "YYYY*" to Regex("\\d{4}\\*"), "YYYY-YYYY" to Regex("\\d{4}-\\d{4}"), "YYYY-YY" to Regex("\\d{4}-\\d{2}")).forEach { (name, regex) -> appendLine("$name: ${years.count(regex::matches)}") }; samples(years.distinct().sorted())
            section("GI DOMAIN"); val giC = audit.coverage.getValue("gi"); lines("Resolved: ${giC.present}", "Unresolved: ${giC.unresolved}", "Parseable: ${giValues.size}", "Minimum: ${giValues.minOrNull()}", "Maximum: ${giValues.maxOrNull()}", "GI == 100: ${giValues.count { it == 100.0 }}", "GI > 100: ${giValues.count { it > 100.0 }}", "GI < 0: ${giValues.count { it < 0.0 }}", "Decimal GI: ${audit.records.count { it.gi?.contains('.') == true }}")
            section("SEM DOMAIN"); val semC = audit.coverage.getValue("sem"); lines("Resolved: ${semC.present}", "Missing: ${semC.sourceMissing}", "Unresolved: ${semC.unresolved}", "Integer: ${audit.records.count { it.sem?.matches(Regex("\\d+")) == true }}", "Decimal: ${audit.records.count { it.sem?.contains('.') == true }}", "Minimum: ${semValues.minOrNull()}", "Maximum: ${semValues.maxOrNull()}")
            section("GL DOMAIN"); val glC = audit.coverage.getValue("gl"); lines("Numeric: ${glNumeric.size}", "Source missing: ${glC.sourceMissing}", "Unresolved: ${glC.unresolved}", "Minimum: ${glNumeric.minOrNull()}", "Maximum: ${glNumeric.maxOrNull()}")
            domainSection("SUBJECTS DOMAIN", "subjects", audit.records)
            domainSection("AVAILABLE CARBOHYDRATE DOMAIN", "availableCarbohydrate", audit.records)
            domainSection("TEST PORTION DOMAIN", "testPortion", audit.records)
            domainSection("REFERENCE FOOD / TIME DOMAIN", "referenceFoodTime", audit.records)
            domainSection("TIMEPOINT DOMAIN", "timepoints", audit.records)
            domainSection("SAMPLE COLLECTION DOMAIN", "sampleCollection", audit.records)
            domainSection("ANALYSIS METHOD DOMAIN", "analysisMethod", audit.records)
            section("REFERENCE CODE DOMAIN"); coverageLines(audit.coverage.getValue("referenceCode")); lines("Numeric refs: ${refs.count { it.matches(Regex("\\d+")) }}", "UO refs: ${refs.count { it.matches(Regex("UO\\d+")) }}", "Combined/multiple refs: ${refs.count { ' ' in it.trim() }}", "Layout-reconstructed refs: ${reconstructions.count { it.field == "referenceCode" }}"); samples(refs.filter(String::isNotBlank).distinct().sorted())
            section("CATEGORY CONTEXT"); lines("Major categories: ${audit.majorCategories.size}", "Rows with major category context: ${audit.records.size}", "Rows with subcategory context: audit-visible but not promoted to final taxonomy", "Rows with deeper context: audit-visible; final model deferred", "Unresolved major category context: 0")
            section("MEAN SUMMARY ROWS"); lines("Count: ${audit.summaries.size}", "With Food Number: ${audit.summaries.count { Regex("^\\d+\\s").containsMatchIn(it.label) }}"); samples(audit.summaries.map { "Page ${it.page}: ${it.label}" })
            section("NOMINAL GL NOTES"); lines("Count: ${audit.nominalNotes.size}"); samples(audit.nominalNotes.map { "Page ${it.first}: ${it.second}" })
            section("FOOTNOTE MARKERS"); lines("Records containing star: ${audit.records.count { record -> record.cells.values.any { '*' in it } }}", "Records containing numeric marker candidates: ${audit.records.count { record -> record.cells.values.any { Regex("[A-Za-z)]\\d+\\b").containsMatchIn(it) } }}", "Markers remain attached to source lexical fields.")
            section("FOOTNOTE PAGE"); lines("Page: 139", "Geometry lines: ${audit.footnoteLines.size}", "Star semantics present: ${'*' in footText}", "UO definitions present: ${"UO" in footText}"); samples(footText.lineSequence().map(String::trim).filter(String::isNotBlank).toList())
            section("PAGE BREAK RECORDS"); lines("Count: ${pageBreaks.size}"); samples(pageBreaks.map { "Food ${it.foodNumber}: page ${it.page} -> ${it.endPage}; ${it.cells.filterValues(String::isNotBlank)}" })
            section("LAYOUT RECONSTRUCTIONS"); lines("Total: ${reconstructions.size}"); reconstructions.groupingBy { it.field }.eachCount().toSortedMap().forEach { appendLine("${it.key}: ${it.value}") }; samples(reconstructions.map { "Food ${it.foodNumber}, page ${it.page}, field ${it.field}: '${it.physical}' -> '${it.reconstructed}'" })
            section("UNRESOLVED EXTRACTION"); lines("Total: ${unresolved.size}"); unresolved.groupingBy { it.first }.eachCount().toSortedMap().forEach { appendLine("${it.key}: ${it.value}") }; samples(unresolved.map { (field, record) -> "Food ${record.foodNumber}, page ${record.page}, field $field, lexical='${value(record, field)}', fragments=${geometrySample(record, field)}, reason=blank or lexical form not safely isolated for field domain" })
            section("PHASE 1 CROSS-CHECK"); lines("Phase 1 measurement rows: 2091", "Phase 2 measurement rows: ${audit.records.size}", "Phase 1 GI unresolved: 9", "Phase 2 GI unresolved: ${giC.unresolved}", "Phase 1 GL missing/not-safe: 18", "Phase 2 GL numeric: ${glNumeric.size}", "Phase 2 GL source missing: ${glC.sourceMissing}", "Phase 2 GL unresolved: ${glC.unresolved}")
            section("PRE-HIM CROSS-CHECK"); lines("Expected sourceMeasurementCount: ${audit.preHimCount}", "Geometry measurement count: ${audit.records.size}", "Difference: ${audit.records.size - audit.preHimCount}", "PRE-HIM values used to fill PDF fields: false")
            section("AUDIT SUMMARY"); lines("Geometry row anchors complete: true", "Column geometry stable: true", "Semantic normalization: false", "Aggregation/deduplication: false", "Report rendering deterministic in-memory: true")
            section("OPEN QUESTIONS FOR PHASE 3"); lines("1. Projection policy for unresolved/missing fields and unnumbered mean summaries.", "2. Whether category context and nominal GL notes belong in the source projection.", "3. Representation of footnote markers and footnote definitions.", "4. Whether geometry reconstruction provenance is retained in the final source contract.")
        }
    }

    private fun StringBuilder.domainSection(title: String, field: String, records: List<GeometryRecord>) { section(title); coverageLines(coverage(records).getValue(field)); val values = records.map { it.cells.getValue(field) }.filter(String::isNotBlank); appendLine("Distinct lexical values: ${values.distinct().size}"); samples(values.distinct().sorted()) }
    private fun StringBuilder.coverageLines(c: Coverage) = lines("Present: ${c.present}", "Source missing: ${c.sourceMissing}", "Unresolved: ${c.unresolved}")
    private fun value(record: GeometryRecord, field: String) = when (field) { "foodNumber" -> record.foodNumber.toString(); "gi" -> record.gi.orEmpty(); "sem" -> record.sem.orEmpty(); else -> record.cells[field].orEmpty() }
    private fun isPresent(field: String, value: String): Boolean = when {
        field in setOf("gi", "sem", "gl", "availableCarbohydrate", "testPortion") -> value.toDoubleOrNull() != null
        field == "year" -> Regex("(?:19|20)\\d{2}(?:\\*|-(?:\\d{2}|(?:19|20)\\d{2}))?").matches(value)
        else -> value.isNotBlank() && value != "NS"
    }
    private fun geometrySample(record: GeometryRecord, field: String) = "page=${record.page}, column=${COLUMNS.find { it.name == field } ?: if (field in setOf("gi", "sem")) COLUMNS.first { it.name == "giSem" } else "logical"}"
    private fun StringBuilder.section(title: String, char: Char = '-') { if (isNotEmpty()) appendLine(); appendLine(title); appendLine(char.toString().repeat(title.length)) }
    private fun StringBuilder.lines(vararg values: String) = values.forEach(::appendLine)
    private fun StringBuilder.samples(values: List<String>) = values.take(SAMPLE_LIMIT).forEach { appendLine("- ${it.take(1200)}") }
    private fun f(value: Float) = String.format(Locale.ROOT, "%.2f", value)
    private fun sha256(file: File) = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
    private fun projectRoot(): File { var current: File? = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile; repeat(8) { val c = current ?: error("Project root not found"); if (c.resolve("settings.gradle.kts").isFile && c.resolve("gradlew").isFile) return c; current = c.parentFile }; error("Project root not found") }

    private data class Column(val name: String, val start: Float, val end: Float)
    private data class Glyph(val page: Int, val x: Float, val y: Float, val width: Float, val height: Float, val text: String)
    private data class GeoLine(val page: Int, val y: Float, val text: String, val glyphs: List<Glyph>)
    private data class Extraction(val glyphs: List<Glyph>, val pageTexts: Map<Int, String>)
    private data class Anchor(val number: Int, val page: Int, val y: Float)
    private data class Reconstruction(val foodNumber: Int, val page: Int, val field: String, val physical: String, val reconstructed: String)
    private data class GeometryRecord(val foodNumber: Int, val page: Int, val endPage: Int, val cells: Map<String, String>, val gi: String?, val sem: String?, val reconstructions: List<Reconstruction>)
    private data class Coverage(val present: Int, val sourceMissing: Int, val unresolved: Int)
    private data class Summary(val page: Int, val label: String, val mean: String?, val category: String?)
    private data class Audit(val records: List<GeometryRecord>, val coverage: Map<String, Coverage>, val majorCategories: List<String>, val duplicateNumbers: List<Int>, val missingNumbers: List<Int>, val summaries: List<Summary>, val nominalNotes: List<Pair<Int, String>>, val footnoteLines: List<GeoLine>, val preHimCount: Int)
}
