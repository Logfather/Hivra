package de.shopme.testing.system.tools.knowledge.him.sources.glycemicindex

import com.google.gson.JsonParser
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

class RunGlycemicIndexRawSourceAuditTest {

    private companion object {
        const val FILE_NAME = "International-Tables-GI-2021-Supplemental-Table-1.pdf"
        const val SIZE = 2_216_220L
        const val HASH = "13a2f85fb781bc8d8f9ce194a88f944b885377d59d8d340902ad1bd0d610e2d8"
        const val SAMPLE_LIMIT = 10
        val FOOD_LINE = Regex("^\\s*(\\d{1,4})\\s+.+$")
        val TOC_LINE = Regex("^([A-Z][A-Z &/,-]+):\\s+pages?\\s+(.+?)\\s*$")
        val VALUES = Regex("(?:19|20)\\d{2}(?:\\*|\\s*-\\s*(?:(?:19|20)?\\d{2})?)?\\s+(-?\\d+(?:\\.\\d+)?)\\s*±\\s*(\\d+(?:\\.\\d+)?)(?:\\s+(NS|-?\\d+(?:\\.\\d+)?)\\b)?")
        val YEAR = Regex("\\b(?:19|20)\\d{2}(?:\\*|\\s*-\\s*(?:\\d{2}|(?:19|20)\\d{2}))?")
    }

    @Test
    fun runGlycemicIndexRawSourceAudit() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val root = projectRoot()
        val source = root.resolve("data/sources/glycemic-index/raw/$FILE_NAME")
        val reference = root.resolve("data/sources/glycemic-index/glycemic-index-reference.json")
        guard(source)
        require(reference.isFile && reference.length() > 0L)

        Loader.loadPDF(source).use { document ->
            val pages = pages(document)
            val toc = pages.first().text.lineSequence().map(String::trim).mapNotNull { line ->
                TOC_LINE.matchEntire(line)?.let { Toc(it.groupValues[1], it.groupValues[2]) }
            }.toList()
            val records = records(pages)
            val parsed = records.mapNotNull { record ->
                VALUES.find(record.text)?.let { match ->
                    Parsed(record, match.groupValues[1], match.groupValues[2], match.groupValues[3], YEAR.find(record.text)?.value)
                }
            }
            require(pages.isNotEmpty() && pages.any { it.text.isNotBlank() })
            require(toc.isNotEmpty() && records.isNotEmpty() && parsed.isNotEmpty())

            val report = report(document, source, reference, pages, toc, records, parsed)
            val output = root.resolve("build/knowledge/reports/glycemic-index/glycemic-index-raw-source-audit.txt")
            val outputDirectory = requireNotNull(output.parentFile)
            require(outputDirectory.mkdirs() || outputDirectory.isDirectory)
            output.writeText(report, StandardCharsets.UTF_8)
            require(output.isFile && output.length() > 0L)
            println("GI raw source audit report written to: ${output.absolutePath}")
            println("pages=${pages.size}, measurementRows=${records.size}, parsedGI=${parsed.size}")
        }
    }

    private fun guard(file: File) {
        require(file.exists() && file.isFile && file.length() > 0L)
        require(file.length() == SIZE) { "Raw source size mismatch: ${file.length()}" }
        require(sha256(file) == HASH) { "Raw source SHA-256 mismatch" }
    }

    private fun pages(document: PDDocument): List<Page> {
        val stripper = PDFTextStripper().apply { sortByPosition = true }
        return (1..document.numberOfPages).map { number ->
            stripper.startPage = number
            stripper.endPage = number
            Page(number, stripper.getText(document).replace("\r\n", "\n").replace('\r', '\n'))
        }
    }

    private fun records(pages: List<Page>): List<Record> {
        val lines = pages.drop(1).dropLast(1).flatMap { page ->
            page.text.lineSequence().map { Line(page.number, it.trimEnd()) }.toList()
        }
        val starts = mutableListOf<Pair<Int, Int>>()
        var expected = 1
        lines.forEachIndexed { index, line ->
            val number = FOOD_LINE.matchEntire(line.text)?.groupValues?.get(1)?.toIntOrNull()
            if (number == expected) {
                starts += index to number
                expected++
            }
        }
        return starts.mapIndexed { position, start ->
            val slice = lines.subList(start.first, starts.getOrNull(position + 1)?.first ?: lines.size)
            Record(start.second, slice.first().page, slice.last().page, slice.joinToString(" ") { it.text.trim() }.replace(Regex("\\s+"), " ").trim())
        }
    }

    private fun report(document: PDDocument, source: File, reference: File, pages: List<Page>, toc: List<Toc>, records: List<Record>, parsed: List<Parsed>): String {
        val metadata = JsonParser.parseString(reference.readText()).asJsonObject["metadata"].asJsonObject
        val info = document.documentInformation
        val numbers = records.map { it.number }
        val missing = (numbers.min()..numbers.max()).filterNot(numbers.toSet()::contains)
        val duplicates = numbers.groupingBy { it }.eachCount().filterValues { it > 1 }.toSortedMap()
        val gi = parsed.map { it.gi.toDouble() }
        val gl = parsed.mapNotNull { it.gl.toDoubleOrNull() }
        val mean = pageLines(pages) { "mean of" in it.lowercase(Locale.ROOT) }
        val nominal = pageLines(pages) { "Average available carbohydrate portion" in it }
        val textPages = pages.count { it.text.isNotBlank() }
        val repeatedHeaders = pages.drop(1).count { "Food Number and Item" in it.text }
        val yearPatterns = parsed.mapNotNull { it.year }.groupingBy(::yearPattern).eachCount().toSortedMap()
        val dimensions = document.pages.map { "${f(it.mediaBox.width)} x ${f(it.mediaBox.height)} pt" }.distinct().sorted()
        val rotations = document.pages.map { it.rotation }.groupingBy { it }.eachCount().toSortedMap()
        val pageBreaks = records.count { it.firstPage != it.lastPage }
        val oldCount = metadata["sourceMeasurementCount"].asInt

        return buildString {
            section("GI SUPPLEMENTAL TABLE 1 RAW SOURCE AUDIT", '=')
            section("RAW SOURCE"); lines("Path: ${source.path}", "Size: ${source.length()} bytes", "SHA-256: ${sha256(source)}", "Immutable read-only input: true")
            section("DOCUMENT STRUCTURE"); lines("Pages: ${pages.size}", "Page dimensions: ${dimensions.joinToString()}", "Rotations: ${map(rotations)}", "Table-of-contents page: 1", "First measurement page: ${records.minOf { it.firstPage }}", "Last measurement page: ${records.maxOf { it.lastPage }}", "Footnote page: ${pages.size}")
            section("PDF METADATA"); lines("Title: ${display(info.title)}", "Author: ${display(info.author)}", "Subject: ${display(info.subject)}", "Creator: ${display(info.creator)}", "Producer: ${display(info.producer)}")
            section("TEXT EXTRACTION"); lines("Text layer: true", "Pages with text: $textPages", "Pages without text: ${pages.size - textPages}", "Table content text-based: true", "Repeated table headers: $repeatedHeaders", "Page-break row fragments: $pageBreaks", "Finding: columns, year ranges, reference codes and headings can wrap independently.")
            section("TABLE OF CONTENTS"); toc.forEach { appendLine("${it.category}: ${it.range}") }
            section("TABLE HEADER"); lines("Actual PDFBox lexical form (page 2): ${pages[1].text.lineSequence().take(10).joinToString(" | ") { it.trim() }.replace(Regex("\\s+"), " ")}", "Confirmed visual fields: Food Number and Item; Country of food production; Year of test; GI ± SEM (Glu = 100); GL; Subjects (type & number); Avail carb (Test portion); Test portion (g); Reference food & time period; Timepoints; Sample collection; Sample analysis method; Ref.")
            section("MAJOR CATEGORIES"); lines("Count: ${toc.count { it.category != "FOOTNOTES" }}"); toc.filter { it.category != "FOOTNOTES" }.forEach { appendLine(it.category) }
            section("ROW CLASSIFICATION"); lines("MEASUREMENT_ROW: ${records.size}", "CATEGORY_HEADING: ${toc.count { it.category != "FOOTNOTES" }} major headings", "SUBCATEGORY_HEADING: present; audit-only hierarchy", "MEAN_SUMMARY: ${mean.size}", "EXPLANATORY_TEXT: present", "HEADER: ${repeatedHeaders + 1} detected pages", "FOOTNOTE: page ${pages.size}", "UNKNOWN: retained in in-memory page text; no lines discarded")
            section("MEASUREMENT ROWS"); lines("Detected: ${records.size}", "Detection: monotone published Food Number sequence", "Records spanning extracted lines: ${records.count { it.text.length > 200 }}", "Records crossing pages: $pageBreaks"); samples(records.map { "Food ${it.number} [p${it.firstPage}]: ${it.text}" })
            section("FOOD NUMBER DOMAIN"); lines("Unique: ${numbers.distinct().size}", "Minimum: ${numbers.minOrNull()}", "Maximum: ${numbers.maxOrNull()}", "Duplicates: ${if (duplicates.isEmpty()) "none" else map(duplicates)}", "Missing sequence numbers: ${if (missing.isEmpty()) "none" else missing.joinToString()}")
            section("GI VALUE DOMAIN"); lines("Observed: ${parsed.size}", "Parseable: ${gi.size}", "Minimum: ${gi.minOrNull()}", "Maximum: ${gi.maxOrNull()}", "GI == 100: ${gi.count { it == 100.0 }}", "GI > 100: ${gi.count { it > 100.0 }}", "GI < 0: ${gi.count { it < 0.0 }}", "Decimal GI: ${parsed.count { '.' in it.gi }}", "SEM integer: ${parsed.count { '.' !in it.sem }}", "SEM decimal: ${parsed.count { '.' in it.sem }}", "Unparseable measurement records: ${records.size - parsed.size}"); samples(parsed.map { "Food ${it.record.number}: ${it.gi}±${it.sem}" }); lines("Unparseable record samples:"); samples(records.filter { record -> parsed.none { it.record.number == record.number } }.map { "Food ${it.number}: ${it.text}" })
            section("GL VALUE DOMAIN"); lines("Observed: ${parsed.count { it.gl.isNotBlank() }}", "Parseable: ${gl.size}", "Missing / NS: ${parsed.count { it.gl.isBlank() || it.gl == "NS" }}", "Minimum: ${gl.minOrNull()}", "Maximum: ${gl.maxOrNull()}", "Decimal forms: ${parsed.count { '.' in it.gl }}", "Unparseable non-NS: ${parsed.count { it.gl.isNotBlank() && it.gl != "NS" && it.gl.toDoubleOrNull() == null }}"); samples(parsed.map { "Food ${it.record.number}: ${it.gl.ifBlank { "<not extracted>" }}" })
            section("YEAR OF TEST DOMAIN"); yearPatterns.forEach { appendLine("${it.key}: ${it.value}") }; samples(parsed.mapNotNull { it.year }.distinct())
            section("SUBJECTS DOMAIN"); lines("Normal, participant-count forms: ${records.count { Regex("Normal,\\s*\\d+").containsMatchIn(it.text) }}", "Age, population, lean/overweight and range qualifiers occur in wrapped raw record text; no normalization applied."); samples(records.filter { "Normal," in it.text }.map { it.text })
            section("AVAILABLE CARBOHYDRATE DOMAIN"); lines("Typical visible lexemes: 25, 50", "Other/missing lexemes remain in raw record context; sequential extraction can concatenate adjacent numeric columns.")
            section("TEST PORTION DOMAIN"); lines("Integer, decimal and NS forms observed; standalone field counts deferred to geometry-aware Phase 2 extraction.")
            section("REFERENCE FOOD / TIME PERIOD DOMAIN"); domain(records, listOf("Glucose, 2h", "Bread, 2h", "Rice, 2h", "Glucose, 3h"))
            section("TIMEPOINTS DOMAIN"); lines("Standard: ${records.count { Regex("\\bStandard\\b").containsMatchIn(it.text) }}", "Explicit and wrapped sequences remain source lexical text.")
            section("SAMPLE COLLECTION DOMAIN"); domain(records, listOf("Capillary, plasma", "Capillary, whole blood", "Venous, plasma/serum", "Venous, whole blood", "NS"))
            section("SAMPLE ANALYSIS METHOD DOMAIN"); domain(records, listOf("Enzymatic", "YSI", "HemoCue", "Glucometer", "NS"))
            section("REFERENCE CODE DOMAIN"); lines("UO-bearing records: ${records.count { "UO" in it.text }}", "Numeric, UO and split/multiline forms observed; apparent UO1 / 0 splits are not repaired."); samples(records.filter { "UO" in it.text }.map { it.text })
            section("CATEGORY HIERARCHY"); lines("Major categories: ${toc.count { it.category != "FOOTNOTES" }}", "Subcategory and deeper headings are visibly present but deliberately not promoted to a final taxonomy.", "Sample: CEREAL GRAINS -> Rice -> Basmati, white rice, boiled -> measurements")
            section("MEAN SUMMARY ROWS"); lines("Count: ${mean.size}", "With leading Food Number: ${mean.count { FOOD_LINE.matches(it.second) }}", "Observed as unnumbered, generally GI-only published summaries; projection decision deferred."); samples(mean.map { "Page ${it.first}: ${it.second}" })
            section("CATEGORY NOMINAL GL NOTES"); lines("Detected: ${nominal.size}", "Notes are explanatory/category-level and were not copied into measurements."); samples(nominal.map { "Page ${it.first}: ${it.second}" })
            section("FOOTNOTES"); val foot = pages.last().text; lines("Page: ${pages.last().number}", "Numbered lexical starts: ${Regex("(?m)^\\s*\\d+\\s").findAll(foot).count()}", "Star marker present: ${'*' in foot}", "UO notation present: ${"UO" in foot}"); samples(foot.lineSequence().map(String::trim).filter(String::isNotBlank).toList())
            section("PDF EXTRACTION ANOMALIES"); lines("Replacement characters: ${pages.sumOf { it.text.count { char -> char == '\uFFFD' } }}", "Soft hyphens: ${pages.sumOf { it.text.count { char -> char == '\u00AD' } }}", "Private-use Unicode: ${pages.sumOf { it.text.count { char -> char.code in 0xE000..0xF8FF } }}", "Repeated headers: $repeatedHeaders", "Page-break fragmentation: $pageBreaks", "Observed: split header words, split year ranges, split reference codes, independent column wrapping, trademark symbols.")
            section("PRE-HIM REFERENCE CROSS-CHECK"); lines("sourceMeasurementCount: $oldCount", "stage5RetainedMeasurementCount: ${metadata["stage5RetainedMeasurementCount"].asInt}", "stage5ExcludedMeasurementCount: ${metadata["stage5ExcludedMeasurementCount"].asInt}", "stage5ReferenceIdentityCount: ${metadata["stage5ReferenceIdentityCount"].asInt}", "PDF detected measurement rows: ${records.size}", "Difference: ${records.size - oldCount}")
            section("AUDIT SUMMARY"); lines("PDFBox reliably loaded ${pages.size} pages and extracted text from $textPages pages.", "Food-number sequence complete: ${missing.isEmpty() && duplicates.isEmpty()}", "Normalized values persisted: false", "Aggregation/deduplication: false")
            section("OPEN QUESTIONS FOR PHASE 2"); lines("1. Projection treatment of unnumbered mean-summary rows.", "2. Representation of category hierarchy and nominal-GL notes.", "3. Geometry-based lexical boundaries for all visual columns.", "4. Source-faithful representation of footnotes and split reference codes.")
        }
    }

    private fun StringBuilder.section(title: String, char: Char = '-') { if (isNotEmpty()) appendLine(); appendLine(title); appendLine(char.toString().repeat(title.length)) }
    private fun StringBuilder.lines(vararg values: String) = values.forEach(::appendLine)
    private fun StringBuilder.samples(values: List<String>) = values.take(SAMPLE_LIMIT).forEach { appendLine("- ${it.take(500)}") }
    private fun StringBuilder.domain(records: List<Record>, values: List<String>) = values.forEach { value -> appendLine("$value: ${records.count { value in it.text }}") }
    private fun pageLines(pages: List<Page>, predicate: (String) -> Boolean) = pages.flatMap { p -> p.text.lineSequence().map(String::trim).filter(predicate).map { p.number to it }.toList() }
    private fun yearPattern(value: String) = when { value.endsWith("*") -> "YYYY*"; Regex("\\d{4}\\s*-\\s*\\d{4}").matches(value) -> "YYYY-YYYY"; Regex("\\d{4}\\s*-\\s*\\d{2}").matches(value) -> "YYYY-YY"; else -> "YYYY" }
    private fun display(value: String?) = value?.takeIf(String::isNotBlank) ?: "<missing>"
    private fun f(value: Float) = String.format(Locale.ROOT, "%.2f", value)
    private fun map(values: Map<*, *>) = values.entries.joinToString { "${it.key}=${it.value}" }
    private fun sha256(file: File) = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
    private fun projectRoot(): File { var current: File? = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile; repeat(8) { val c = current ?: error("Project root not found"); if (c.resolve("settings.gradle.kts").isFile && c.resolve("gradlew").isFile) return c; current = c.parentFile }; error("Project root not found") }

    private data class Page(val number: Int, val text: String)
    private data class Line(val page: Int, val text: String)
    private data class Toc(val category: String, val range: String)
    private data class Record(val number: Int, val firstPage: Int, val lastPage: Int, val text: String)
    private data class Parsed(val record: Record, val gi: String, val sem: String, val gl: String, val year: String?)
}
