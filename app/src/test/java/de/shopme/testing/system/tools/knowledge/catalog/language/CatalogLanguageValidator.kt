package de.shopme.testing.system.tools.knowledge.catalog.language

import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogNormalizationResult
import de.shopme.testing.system.tools.knowledge.catalog.validation.CatalogIssueSeverity
import java.text.Normalizer
import java.util.Locale

class CatalogLanguageValidator {

    fun validate(
        entries: List<IndexedCatalogFoodItem>,
        normalizations: List<CatalogNormalizationResult>
    ): CatalogLanguageValidationResult {
        require(entries.map { it.sourceIndex }.distinct().size == entries.size) {
            "Catalog entries contain duplicate sourceIndex values."
        }

        require(
            normalizations.map { it.sourceIndex }.distinct().size ==
                    normalizations.size
        ) {
            "Catalog normalizations contain duplicate sourceIndex values."
        }

        val entrySourceIndices = entries
            .mapTo(sortedSetOf()) { it.sourceIndex }

        val normalizationSourceIndices = normalizations
            .mapTo(sortedSetOf()) { it.sourceIndex }

        val missingNormalizationSourceIndices =
            entrySourceIndices - normalizationSourceIndices

        require(missingNormalizationSourceIndices.isEmpty()) {
            "Missing catalog normalizations for source indices: " +
                    missingNormalizationSourceIndices.joinToString(", ")
        }

        val unexpectedNormalizationSourceIndices =
            normalizationSourceIndices - entrySourceIndices

        require(unexpectedNormalizationSourceIndices.isEmpty()) {
            "Catalog normalizations reference unknown source indices: " +
                    unexpectedNormalizationSourceIndices.joinToString(", ")
        }

        val issues = entries
            .sortedBy { it.sourceIndex }
            .flatMap(::validateEntry)
            .distinct()
            .sortedWith(LANGUAGE_ISSUE_COMPARATOR)

        val affectedSourceIndices = issues
            .mapTo(sortedSetOf()) { it.sourceIndex }
            .toList()

        val issueCountsByType = issues
            .groupingBy { it.type }
            .eachCount()
            .toList()
            .sortedBy { (type, _) -> type.name }
            .associate { it }

        val issueCountsByField = issues
            .groupingBy { it.field }
            .eachCount()
            .toSortedMap()

        val errorCount = issues.count {
            it.severity == CatalogIssueSeverity.ERROR
        }

        val warningCount = issues.count {
            it.severity == CatalogIssueSeverity.WARNING
        }

        val infoCount = issues.count {
            it.severity == CatalogIssueSeverity.INFO
        }

        return CatalogLanguageValidationResult(
            inputEntryCount = entries.size,
            affectedEntryCount = affectedSourceIndices.size,
            issueCount = issues.size,
            errorCount = errorCount,
            warningCount = warningCount,
            infoCount = infoCount,
            issueCountsByType = issueCountsByType,
            issueCountsByField = issueCountsByField,
            affectedSourceIndices = affectedSourceIndices,
            issues = issues,
            valid = errorCount == 0
        )
    }

    private fun validateEntry(
        entry: IndexedCatalogFoodItem
    ): List<CatalogLanguageIssue> {
        val issues = mutableListOf<CatalogLanguageIssue>()

        validateItemName(
            entry = entry,
            issues = issues
        )

        validatePlural(
            entry = entry,
            issues = issues
        )

        validateEnglishName(
            entry = entry,
            issues = issues
        )

        validateColloquialTerms(
            entry = entry,
            issues = issues
        )

        validateAutocompleteTokens(
            entry = entry,
            issues = issues
        )

        validatePhoneticTokens(
            entry = entry,
            issues = issues
        )

        return issues
    }

    private fun validateItemName(
        entry: IndexedCatalogFoodItem,
        issues: MutableList<CatalogLanguageIssue>
    ) {
        val itemName = entry.item.itemname

        addWhitespaceIssues(
            entry = entry,
            field = FIELD_ITEM_NAME,
            value = itemName,
            issues = issues
        )

        addCapitalizationIssue(
            entry = entry,
            field = FIELD_ITEM_NAME,
            value = itemName,
            issues = issues
        )

        addUmlautIssue(
            entry = entry,
            field = FIELD_ITEM_NAME,
            value = itemName,
            issues = issues
        )

        addApostropheIssue(
            entry = entry,
            field = FIELD_ITEM_NAME,
            value = itemName,
            issues = issues
        )

        addHyphenationIssue(
            entry = entry,
            field = FIELD_ITEM_NAME,
            value = itemName,
            issues = issues
        )

        addEnglishLanguageIssues(
            entry = entry,
            value = itemName,
            issues = issues
        )

        addAbbreviationIssue(
            entry = entry,
            field = FIELD_ITEM_NAME,
            value = itemName,
            issues = issues
        )

        addBrandIssue(
            entry = entry,
            value = itemName,
            issues = issues
        )

        addRetailerIssue(
            entry = entry,
            value = itemName,
            issues = issues
        )

        addPackageSizeIssue(
            entry = entry,
            value = itemName,
            issues = issues
        )

        addProductNumberIssue(
            entry = entry,
            value = itemName,
            issues = issues
        )

        addTypoIssues(
            entry = entry,
            field = FIELD_ITEM_NAME,
            value = itemName,
            issues = issues
        )
    }

    private fun validatePlural(
        entry: IndexedCatalogFoodItem,
        issues: MutableList<CatalogLanguageIssue>
    ) {
        val plural = entry.item.plural ?: return

        addWhitespaceIssues(
            entry = entry,
            field = FIELD_PLURAL,
            value = plural,
            issues = issues
        )

        addCapitalizationIssue(
            entry = entry,
            field = FIELD_PLURAL,
            value = plural,
            issues = issues
        )

        addUmlautIssue(
            entry = entry,
            field = FIELD_PLURAL,
            value = plural,
            issues = issues
        )

        addApostropheIssue(
            entry = entry,
            field = FIELD_PLURAL,
            value = plural,
            issues = issues
        )

        addHyphenationIssue(
            entry = entry,
            field = FIELD_PLURAL,
            value = plural,
            issues = issues
        )

        addTypoIssues(
            entry = entry,
            field = FIELD_PLURAL,
            value = plural,
            issues = issues
        )

        val itemName = normalizeComparisonValue(entry.item.itemname)
        val normalizedPlural = normalizeComparisonValue(plural)

        if (normalizedPlural.isBlank()) {
            issues += issue(
                entry = entry,
                type = CatalogLanguageIssueType.INVALID_PLURAL,
                severity = CatalogIssueSeverity.WARNING,
                field = FIELD_PLURAL,
                originalValue = plural,
                suggestedValue = null,
                message = "Plural value is blank after normalization."
            )

            return
        }

        if (containsPluralMetadata(plural)) {
            issues += issue(
                entry = entry,
                type = CatalogLanguageIssueType.INVALID_PLURAL,
                severity = CatalogIssueSeverity.WARNING,
                field = FIELD_PLURAL,
                originalValue = plural,
                suggestedValue = stripPluralMetadata(plural),
                message =
                    "Plural value '$plural' contains explanatory metadata " +
                            "instead of a plain lexical form."
            )
        }

        if (looksLikePluralAlternatives(plural)) {
            issues += issue(
                entry = entry,
                type = CatalogLanguageIssueType.INVALID_PLURAL,
                severity = CatalogIssueSeverity.WARNING,
                field = FIELD_PLURAL,
                originalValue = plural,
                suggestedValue = null,
                message =
                    "Plural value '$plural' appears to contain multiple " +
                            "alternatives."
            )
        }

        if (
            itemName == normalizedPlural &&
            !mayHaveIdenticalSingularAndPlural(entry.item.itemname)
        ) {
            issues += issue(
                entry = entry,
                type = CatalogLanguageIssueType.INVALID_PLURAL,
                severity = CatalogIssueSeverity.INFO,
                field = FIELD_PLURAL,
                originalValue = plural,
                suggestedValue = null,
                message =
                    "Plural '$plural' is identical to the primary name, " +
                            "although this word is not a known invariant form."
            )
        }

        if (
            normalizedPlural.endsWith("s") &&
            normalizedPlural.length > 3 &&
            normalizedPlural.dropLast(1) == itemName &&
            itemName !in GERMAN_S_PLURAL_EXCEPTIONS
        ) {
            issues += issue(
                entry = entry,
                type = CatalogLanguageIssueType.INVALID_PLURAL,
                severity = CatalogIssueSeverity.INFO,
                field = FIELD_PLURAL,
                originalValue = plural,
                suggestedValue = null,
                message =
                    "Plural '$plural' may use an English-style s-plural " +
                            "for a German food name."
            )
        }
    }

    private fun validateEnglishName(
        entry: IndexedCatalogFoodItem,
        issues: MutableList<CatalogLanguageIssue>
    ) {
        val normalizedEnglish = entry.item.normalizedEnglish ?: return

        addWhitespaceIssues(
            entry = entry,
            field = FIELD_NORMALIZED_ENGLISH,
            value = normalizedEnglish,
            issues = issues
        )

        addApostropheIssue(
            entry = entry,
            field = FIELD_NORMALIZED_ENGLISH,
            value = normalizedEnglish,
            issues = issues
        )

        addHyphenationIssue(
            entry = entry,
            field = FIELD_NORMALIZED_ENGLISH,
            value = normalizedEnglish,
            issues = issues
        )

        addTypoIssues(
            entry = entry,
            field = FIELD_NORMALIZED_ENGLISH,
            value = normalizedEnglish,
            issues = issues
        )
    }

    private fun validateColloquialTerms(
        entry: IndexedCatalogFoodItem,
        issues: MutableList<CatalogLanguageIssue>
    ) {
        entry.item.colloquial
            .forEachIndexed { index, value ->
                val field = "$FIELD_COLLOQUIAL[$index]"

                addWhitespaceIssues(
                    entry = entry,
                    field = field,
                    value = value,
                    issues = issues
                )

                addUmlautIssue(
                    entry = entry,
                    field = field,
                    value = value,
                    issues = issues
                )

                addApostropheIssue(
                    entry = entry,
                    field = field,
                    value = value,
                    issues = issues
                )

                addHyphenationIssue(
                    entry = entry,
                    field = field,
                    value = value,
                    issues = issues
                )

                addTypoIssues(
                    entry = entry,
                    field = field,
                    value = value,
                    issues = issues
                )
            }
    }

    private fun validateAutocompleteTokens(
        entry: IndexedCatalogFoodItem,
        issues: MutableList<CatalogLanguageIssue>
    ) {
        entry.item.autocompleteTokens
            .forEachIndexed { index, value ->
                val field = "$FIELD_AUTOCOMPLETE_TOKENS[$index]"

                addWhitespaceIssues(
                    entry = entry,
                    field = field,
                    value = value,
                    issues = issues
                )

                addUmlautIssue(
                    entry = entry,
                    field = field,
                    value = value,
                    issues = issues
                )

                addApostropheIssue(
                    entry = entry,
                    field = field,
                    value = value,
                    issues = issues
                )

                addHyphenationIssue(
                    entry = entry,
                    field = field,
                    value = value,
                    issues = issues
                )
            }
    }

    private fun validatePhoneticTokens(
        entry: IndexedCatalogFoodItem,
        issues: MutableList<CatalogLanguageIssue>
    ) {
        entry.item.phoneticTokens
            .forEachIndexed { index, value ->
                val field = "$FIELD_PHONETIC_TOKENS[$index]"

                addWhitespaceIssues(
                    entry = entry,
                    field = field,
                    value = value,
                    issues = issues
                )

                addApostropheIssue(
                    entry = entry,
                    field = field,
                    value = value,
                    issues = issues
                )

                addHyphenationIssue(
                    entry = entry,
                    field = field,
                    value = value,
                    issues = issues
                )
            }
    }

    private fun addWhitespaceIssues(
        entry: IndexedCatalogFoodItem,
        field: String,
        value: String,
        issues: MutableList<CatalogLanguageIssue>
    ) {
        if (value.startsWithWhitespace()) {
            issues += issue(
                entry = entry,
                type = CatalogLanguageIssueType.LEADING_WHITESPACE,
                severity = CatalogIssueSeverity.WARNING,
                field = field,
                originalValue = value,
                suggestedValue = normalizeWhitespace(value),
                message = "Field '$field' contains leading whitespace."
            )
        }

        if (value.endsWithWhitespace()) {
            issues += issue(
                entry = entry,
                type = CatalogLanguageIssueType.TRAILING_WHITESPACE,
                severity = CatalogIssueSeverity.WARNING,
                field = field,
                originalValue = value,
                suggestedValue = normalizeWhitespace(value),
                message = "Field '$field' contains trailing whitespace."
            )
        }

        if (MULTIPLE_WHITESPACE_REGEX.containsMatchIn(value.trim())) {
            issues += issue(
                entry = entry,
                type = CatalogLanguageIssueType.MULTIPLE_WHITESPACE,
                severity = CatalogIssueSeverity.WARNING,
                field = field,
                originalValue = value,
                suggestedValue = normalizeWhitespace(value),
                message = "Field '$field' contains repeated whitespace."
            )
        }
    }

    private fun addCapitalizationIssue(
        entry: IndexedCatalogFoodItem,
        field: String,
        value: String,
        issues: MutableList<CatalogLanguageIssue>
    ) {
        val normalizedValue = normalizeWhitespace(value)

        if (normalizedValue.isBlank()) {
            return
        }

        if (normalizedValue == normalizedValue.uppercase(Locale.GERMAN)) {
            val letters = normalizedValue.count(Char::isLetter)

            if (letters >= MINIMUM_ALL_CAPS_LETTER_COUNT) {
                issues += issue(
                    entry = entry,
                    type = CatalogLanguageIssueType.INVALID_CAPITALIZATION,
                    severity = CatalogIssueSeverity.WARNING,
                    field = field,
                    originalValue = value,
                    suggestedValue = sentenceCase(normalizedValue),
                    message =
                        "Field '$field' is written entirely in uppercase."
                )
            }

            return
        }

        val firstLetterIndex = normalizedValue.indexOfFirst(Char::isLetter)

        if (
            firstLetterIndex >= 0 &&
            normalizedValue[firstLetterIndex].isLowerCase()
        ) {
            issues += issue(
                entry = entry,
                type = CatalogLanguageIssueType.INVALID_CAPITALIZATION,
                severity = CatalogIssueSeverity.INFO,
                field = field,
                originalValue = value,
                suggestedValue = uppercaseFirstLetter(normalizedValue),
                message =
                    "Field '$field' starts with a lowercase letter."
            )
        }

        val suspiciousUppercaseWords = WORD_REGEX
            .findAll(normalizedValue)
            .map { it.value }
            .drop(1)
            .filter(::isSuspiciouslyCapitalizedWord)
            .distinct()
            .sorted()
            .toList()

        if (suspiciousUppercaseWords.isNotEmpty()) {
            issues += issue(
                entry = entry,
                type = CatalogLanguageIssueType.INVALID_CAPITALIZATION,
                severity = CatalogIssueSeverity.INFO,
                field = field,
                originalValue = value,
                suggestedValue = null,
                matchedTerms = suspiciousUppercaseWords,
                message =
                    "Field '$field' contains potentially inconsistent " +
                            "capitalization: " +
                            suspiciousUppercaseWords.joinToString(", ") +
                            "."
            )
        }
    }

    private fun addEnglishLanguageIssues(
        entry: IndexedCatalogFoodItem,
        value: String,
        issues: MutableList<CatalogLanguageIssue>
    ) {
        val tokens = tokenize(value)

        if (tokens.isEmpty()) {
            return
        }

        val englishTokens = tokens
            .filter { it in ENGLISH_FOOD_TERMS }
            .sorted()

        val germanTokens = tokens
            .filter { it in GERMAN_FOOD_TERMS }
            .sorted()

        val clearlyEnglish =
            englishTokens.size >= MINIMUM_ENGLISH_PRIMARY_TERM_COUNT &&
                    germanTokens.isEmpty()

        if (clearlyEnglish) {
            issues += issue(
                entry = entry,
                type = CatalogLanguageIssueType.ENGLISH_PRIMARY_NAME,
                severity = CatalogIssueSeverity.WARNING,
                field = FIELD_ITEM_NAME,
                originalValue = value,
                suggestedValue = null,
                matchedTerms = englishTokens,
                message =
                    "Primary item name appears to be English rather than " +
                            "German: ${englishTokens.joinToString(", ")}."
            )

            return
        }

        if (englishTokens.isNotEmpty() && germanTokens.isNotEmpty()) {
            val matchedTerms = (englishTokens + germanTokens)
                .distinct()
                .sorted()

            issues += issue(
                entry = entry,
                type = CatalogLanguageIssueType.MIXED_LANGUAGE_NAME,
                severity = CatalogIssueSeverity.INFO,
                field = FIELD_ITEM_NAME,
                originalValue = value,
                suggestedValue = null,
                matchedTerms = matchedTerms,
                message =
                    "Primary item name appears to mix German and English " +
                            "food terminology."
            )
        }
    }

    private fun addUmlautIssue(
        entry: IndexedCatalogFoodItem,
        field: String,
        value: String,
        issues: MutableList<CatalogLanguageIssue>
    ) {
        val normalizedLowercase = value.lowercase(Locale.GERMAN)

        val matchedTerms = UMLAUT_REPLACEMENT_PATTERNS
            .keys
            .filter { pattern ->
                containsStandaloneReplacement(
                    value = normalizedLowercase,
                    replacement = pattern
                )
            }
            .sorted()

        if (matchedTerms.isEmpty()) {
            return
        }

        val suggestedValue = replaceCommonUmlautForms(value)

        if (suggestedValue == value) {
            return
        }

        issues += issue(
            entry = entry,
            type = CatalogLanguageIssueType.INVALID_UMLAUT_FORM,
            severity = CatalogIssueSeverity.INFO,
            field = field,
            originalValue = value,
            suggestedValue = suggestedValue,
            matchedTerms = matchedTerms,
            message =
                "Field '$field' contains ASCII umlaut spellings that may " +
                        "require canonical German spelling."
        )
    }

    private fun addApostropheIssue(
        entry: IndexedCatalogFoodItem,
        field: String,
        value: String,
        issues: MutableList<CatalogLanguageIssue>
    ) {
        val matchedCharacters = value
            .filter { it in NON_CANONICAL_APOSTROPHES }
            .map(Char::toString)
            .distinct()
            .sorted()

        if (matchedCharacters.isNotEmpty()) {
            issues += issue(
                entry = entry,
                type = CatalogLanguageIssueType.INVALID_APOSTROPHE,
                severity = CatalogIssueSeverity.WARNING,
                field = field,
                originalValue = value,
                suggestedValue = normalizeApostrophes(value),
                matchedTerms = matchedCharacters,
                message =
                    "Field '$field' contains non-canonical apostrophe " +
                            "characters."
            )
        }

        if (SPACE_AROUND_APOSTROPHE_REGEX.containsMatchIn(value)) {
            issues += issue(
                entry = entry,
                type = CatalogLanguageIssueType.INVALID_APOSTROPHE,
                severity = CatalogIssueSeverity.WARNING,
                field = field,
                originalValue = value,
                suggestedValue = normalizeApostropheSpacing(value),
                matchedTerms = listOf("'"),
                message =
                    "Field '$field' contains invalid spacing around an " +
                            "apostrophe."
            )
        }
    }

    private fun addHyphenationIssue(
        entry: IndexedCatalogFoodItem,
        field: String,
        value: String,
        issues: MutableList<CatalogLanguageIssue>
    ) {
        val nonCanonicalHyphens = value
            .filter { it in NON_CANONICAL_HYPHENS }
            .map(Char::toString)
            .distinct()
            .sorted()

        if (nonCanonicalHyphens.isNotEmpty()) {
            issues += issue(
                entry = entry,
                type = CatalogLanguageIssueType.INVALID_HYPHENATION,
                severity = CatalogIssueSeverity.WARNING,
                field = field,
                originalValue = value,
                suggestedValue = normalizeHyphens(value),
                matchedTerms = nonCanonicalHyphens,
                message =
                    "Field '$field' contains non-canonical hyphen " +
                            "characters."
            )
        }

        if (
            SPACE_AROUND_HYPHEN_REGEX.containsMatchIn(value) ||
            MULTIPLE_HYPHEN_REGEX.containsMatchIn(value)
        ) {
            issues += issue(
                entry = entry,
                type = CatalogLanguageIssueType.INVALID_HYPHENATION,
                severity = CatalogIssueSeverity.WARNING,
                field = field,
                originalValue = value,
                suggestedValue = normalizeHyphenSpacing(value),
                matchedTerms = listOf("-"),
                message =
                    "Field '$field' contains invalid hyphen spacing or " +
                            "repeated hyphens."
            )
        }

        val suspiciousNumericCompound = NUMERIC_COMPOUND_WITHOUT_HYPHEN_REGEX
            .find(value)
            ?.value

        if (suspiciousNumericCompound != null) {
            issues += issue(
                entry = entry,
                type = CatalogLanguageIssueType.INVALID_HYPHENATION,
                severity = CatalogIssueSeverity.INFO,
                field = field,
                originalValue = value,
                suggestedValue = null,
                matchedTerms = listOf(suspiciousNumericCompound),
                message =
                    "Field '$field' contains a numeric compound that may " +
                            "require a hyphen."
            )
        }
    }

    private fun addAbbreviationIssue(
        entry: IndexedCatalogFoodItem,
        field: String,
        value: String,
        issues: MutableList<CatalogLanguageIssue>
    ) {
        val abbreviations = ABBREVIATION_REGEX
            .findAll(value)
            .map { it.value.trim() }
            .filterNot { abbreviation ->
                abbreviation.lowercase(Locale.ROOT) in ALLOWED_ABBREVIATIONS
            }
            .distinct()
            .sorted()
            .toList()

        if (abbreviations.isEmpty()) {
            return
        }

        issues += issue(
            entry = entry,
            type = CatalogLanguageIssueType.ABBREVIATION_SUSPECTED,
            severity = CatalogIssueSeverity.INFO,
            field = field,
            originalValue = value,
            suggestedValue = null,
            matchedTerms = abbreviations,
            message =
                "Field '$field' contains possible abbreviations: " +
                        abbreviations.joinToString(", ") +
                        "."
        )
    }

    private fun addBrandIssue(
        entry: IndexedCatalogFoodItem,
        value: String,
        issues: MutableList<CatalogLanguageIssue>
    ) {
        val tokens = tokenize(value)

        val matchedBrands = BRAND_TERMS
            .filter { brand ->
                phraseMatches(
                    normalizedValue = normalizeSearchValue(value),
                    normalizedPhrase = brand
                )
            }
            .sorted()

        if (matchedBrands.isEmpty()) {
            return
        }

        issues += issue(
            entry = entry,
            type = CatalogLanguageIssueType.BRAND_IN_PRIMARY_NAME,
            severity = CatalogIssueSeverity.WARNING,
            field = FIELD_ITEM_NAME,
            originalValue = value,
            suggestedValue = removeMatchedPhrases(
                value = value,
                phrases = matchedBrands
            ),
            matchedTerms = matchedBrands,
            message =
                "Primary item name contains brand terminology: " +
                        matchedBrands.joinToString(", ") +
                        "."
        )

        @Suppress("UNUSED_VARIABLE")
        val ignored = tokens
    }

    private fun addRetailerIssue(
        entry: IndexedCatalogFoodItem,
        value: String,
        issues: MutableList<CatalogLanguageIssue>
    ) {
        val normalizedValue = normalizeSearchValue(value)

        val matchedRetailers = RETAILER_TERMS
            .filter { retailer ->
                phraseMatches(
                    normalizedValue = normalizedValue,
                    normalizedPhrase = retailer
                )
            }
            .sorted()

        if (matchedRetailers.isEmpty()) {
            return
        }

        issues += issue(
            entry = entry,
            type = CatalogLanguageIssueType.RETAILER_IN_PRIMARY_NAME,
            severity = CatalogIssueSeverity.WARNING,
            field = FIELD_ITEM_NAME,
            originalValue = value,
            suggestedValue = removeMatchedPhrases(
                value = value,
                phrases = matchedRetailers
            ),
            matchedTerms = matchedRetailers,
            message =
                "Primary item name contains retailer terminology: " +
                        matchedRetailers.joinToString(", ") +
                        "."
        )
    }

    private fun addPackageSizeIssue(
        entry: IndexedCatalogFoodItem,
        value: String,
        issues: MutableList<CatalogLanguageIssue>
    ) {
        val matchedPackageSizes = PACKAGE_SIZE_REGEX
            .findAll(value)
            .map { it.value.trim() }
            .distinct()
            .sorted()
            .toList()

        if (matchedPackageSizes.isEmpty()) {
            return
        }

        issues += issue(
            entry = entry,
            type = CatalogLanguageIssueType.PACKAGE_SIZE_IN_PRIMARY_NAME,
            severity = CatalogIssueSeverity.WARNING,
            field = FIELD_ITEM_NAME,
            originalValue = value,
            suggestedValue = removeRegexMatches(
                value = value,
                regex = PACKAGE_SIZE_REGEX
            ),
            matchedTerms = matchedPackageSizes,
            message =
                "Primary item name contains package-size information: " +
                        matchedPackageSizes.joinToString(", ") +
                        "."
        )
    }

    private fun addProductNumberIssue(
        entry: IndexedCatalogFoodItem,
        value: String,
        issues: MutableList<CatalogLanguageIssue>
    ) {
        val productNumbers = PRODUCT_NUMBER_REGEX
            .findAll(value)
            .map { it.value.trim() }
            .distinct()
            .sorted()
            .toList()

        if (productNumbers.isEmpty()) {
            return
        }

        issues += issue(
            entry = entry,
            type = CatalogLanguageIssueType.PRODUCT_NUMBER_IN_PRIMARY_NAME,
            severity = CatalogIssueSeverity.WARNING,
            field = FIELD_ITEM_NAME,
            originalValue = value,
            suggestedValue = removeRegexMatches(
                value = value,
                regex = PRODUCT_NUMBER_REGEX
            ),
            matchedTerms = productNumbers,
            message =
                "Primary item name contains a product, variant or SKU " +
                        "number: ${productNumbers.joinToString(", ")}."
        )
    }

    private fun addTypoIssues(
        entry: IndexedCatalogFoodItem,
        field: String,
        value: String,
        issues: MutableList<CatalogLanguageIssue>
    ) {
        val normalizedValue = normalizeSearchValue(value)

        val typoMatches = TYPO_REPLACEMENTS
            .filterKeys { typo ->
                phraseMatches(
                    normalizedValue = normalizedValue,
                    normalizedPhrase = typo
                )
            }

        if (typoMatches.isEmpty()) {
            return
        }

        val matchedTerms = typoMatches.keys.sorted()
        val suggestedValue = replacePhrases(
            value = value,
            replacements = typoMatches
        )

        issues += issue(
            entry = entry,
            type = CatalogLanguageIssueType.TYPO_SUSPECTED,
            severity = CatalogIssueSeverity.WARNING,
            field = field,
            originalValue = value,
            suggestedValue = suggestedValue,
            matchedTerms = matchedTerms,
            message =
                "Field '$field' contains suspected spelling errors: " +
                        matchedTerms.joinToString(", ") +
                        "."
        )
    }

    private fun issue(
        entry: IndexedCatalogFoodItem,
        type: CatalogLanguageIssueType,
        severity: CatalogIssueSeverity,
        field: String,
        originalValue: String?,
        suggestedValue: String?,
        matchedTerms: List<String> = emptyList(),
        message: String
    ): CatalogLanguageIssue =
        CatalogLanguageIssue(
            type = type,
            severity = severity,
            sourceIndex = entry.sourceIndex,
            itemName = entry.item.itemname,
            field = field,
            originalValue = originalValue,
            suggestedValue = suggestedValue
                ?.let(::normalizeWhitespace)
                ?.takeIf(String::isNotBlank),
            matchedTerms = matchedTerms
                .filter(String::isNotBlank)
                .distinct()
                .sorted(),
            message = message
        )

    private fun containsPluralMetadata(value: String): Boolean =
        PLURAL_METADATA_REGEX.containsMatchIn(value)

    private fun stripPluralMetadata(value: String): String =
        value
            .replace(PLURAL_METADATA_REGEX, "")
            .let(::normalizeWhitespace)

    private fun looksLikePluralAlternatives(value: String): Boolean =
        PLURAL_ALTERNATIVE_REGEX.containsMatchIn(value)

    private fun mayHaveIdenticalSingularAndPlural(value: String): Boolean {
        val normalizedValue = normalizeComparisonValue(value)

        return normalizedValue in INVARIANT_SINGULAR_PLURAL_WORDS ||
                normalizedValue.endsWith("kaese") ||
                normalizedValue.endsWith("käse") ||
                normalizedValue.endsWith("obst") ||
                normalizedValue.endsWith("gemuese") ||
                normalizedValue.endsWith("gemüse") ||
                normalizedValue.endsWith("reis") ||
                normalizedValue.endsWith("fleisch") ||
                normalizedValue.endsWith("fisch")
    }

    private fun isSuspiciouslyCapitalizedWord(value: String): Boolean {
        if (value.length < 2) {
            return false
        }

        if (!value.first().isUpperCase()) {
            return false
        }

        if (value in ALLOWED_INTERNAL_CAPITALIZED_WORDS) {
            return false
        }

        if (value.all(Char::isUpperCase)) {
            return true
        }

        return value.drop(1).any(Char::isUpperCase)
    }

    private fun normalizeWhitespace(value: String): String =
        value
            .trim()
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")

    private fun normalizeComparisonValue(value: String): String =
        normalizeSearchValue(value)
            .replace(NON_ALPHANUMERIC_SPACE_REGEX, " ")
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .trim()

    private fun normalizeSearchValue(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC)
            .lowercase(Locale.GERMAN)
            .replace('’', '\'')
            .replace('‘', '\'')
            .replace('`', '\'')
            .replace('´', '\'')
            .replace('‐', '-')
            .replace('-', '-')
            .replace('‒', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace('−', '-')
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .trim()

    private fun tokenize(value: String): Set<String> =
        normalizeSearchValue(value)
            .replace(NON_LETTER_OR_DIGIT_REGEX, " ")
            .split(' ')
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSortedSet()

    private fun sentenceCase(value: String): String {
        val lowercase = value.lowercase(Locale.GERMAN)

        return uppercaseFirstLetter(lowercase)
    }

    private fun uppercaseFirstLetter(value: String): String {
        val firstLetterIndex = value.indexOfFirst(Char::isLetter)

        if (firstLetterIndex < 0) {
            return value
        }

        return buildString(value.length) {
            append(value.substring(0, firstLetterIndex))
            append(
                value[firstLetterIndex]
                    .uppercaseChar()
            )
            append(value.substring(firstLetterIndex + 1))
        }
    }

    private fun replaceCommonUmlautForms(value: String): String {
        var result = value

        UMLAUT_REPLACEMENT_PATTERNS.forEach { (ascii, umlaut) ->
            result = result.replace(
                Regex(
                    pattern = "(?i)(?<![a-zäöüß])${Regex.escape(ascii)}" +
                            "(?![a-zäöüß])"
                ),
                umlaut
            )
        }

        return result
    }

    private fun containsStandaloneReplacement(
        value: String,
        replacement: String
    ): Boolean =
        Regex(
            pattern = "(?<![a-zäöüß])${Regex.escape(replacement)}" +
                    "(?![a-zäöüß])"
        ).containsMatchIn(value)

    private fun normalizeApostrophes(value: String): String {
        var result = value

        NON_CANONICAL_APOSTROPHES.forEach { apostrophe ->
            result = result.replace(apostrophe, '\'')
        }

        return normalizeApostropheSpacing(result)
    }

    private fun normalizeApostropheSpacing(value: String): String =
        value
            .replace(SPACE_AROUND_APOSTROPHE_REGEX, "'")
            .let(::normalizeWhitespace)

    private fun normalizeHyphens(value: String): String {
        var result = value

        NON_CANONICAL_HYPHENS.forEach { hyphen ->
            result = result.replace(hyphen, '-')
        }

        return normalizeHyphenSpacing(result)
    }

    private fun normalizeHyphenSpacing(value: String): String =
        value
            .replace(SPACE_AROUND_HYPHEN_REGEX, "-")
            .replace(MULTIPLE_HYPHEN_REGEX, "-")
            .let(::normalizeWhitespace)

    private fun removeRegexMatches(
        value: String,
        regex: Regex
    ): String =
        value
            .replace(regex, " ")
            .replace(ORPHANED_PUNCTUATION_REGEX, " ")
            .let(::normalizeWhitespace)

    private fun removeMatchedPhrases(
        value: String,
        phrases: List<String>
    ): String {
        var result = value

        phrases
            .sortedByDescending(String::length)
            .forEach { phrase ->
                result = result.replace(
                    Regex(
                        "(?i)(?<![\\p{L}\\p{N}])" +
                                Regex.escape(phrase) +
                                "(?![\\p{L}\\p{N}])"
                    ),
                    " "
                )
            }

        return result
            .replace(ORPHANED_PUNCTUATION_REGEX, " ")
            .let(::normalizeWhitespace)
    }

    private fun replacePhrases(
        value: String,
        replacements: Map<String, String>
    ): String {
        var result = value

        replacements
            .toList()
            .sortedByDescending { (source, _) -> source.length }
            .forEach { (source, replacement) ->
                result = result.replace(
                    Regex(
                        "(?i)(?<![\\p{L}\\p{N}])" +
                                Regex.escape(source) +
                                "(?![\\p{L}\\p{N}])"
                    ),
                    replacement
                )
            }

        return normalizeWhitespace(result)
    }

    private fun phraseMatches(
        normalizedValue: String,
        normalizedPhrase: String
    ): Boolean =
        Regex(
            "(?<![\\p{L}\\p{N}])" +
                    Regex.escape(normalizedPhrase) +
                    "(?![\\p{L}\\p{N}])"
        ).containsMatchIn(normalizedValue)

    private fun String.startsWithWhitespace(): Boolean =
        isNotEmpty() && first().isWhitespace()

    private fun String.endsWithWhitespace(): Boolean =
        isNotEmpty() && last().isWhitespace()

    private companion object {

        const val FIELD_ITEM_NAME = "itemname"
        const val FIELD_PLURAL = "plural"
        const val FIELD_NORMALIZED_ENGLISH = "normalizedEnglish"
        const val FIELD_COLLOQUIAL = "colloquial"
        const val FIELD_AUTOCOMPLETE_TOKENS = "autocomplete_tokens"
        const val FIELD_PHONETIC_TOKENS = "phonetic_tokens"

        const val MINIMUM_ALL_CAPS_LETTER_COUNT = 4
        const val MINIMUM_ENGLISH_PRIMARY_TERM_COUNT = 1

        val MULTIPLE_WHITESPACE_REGEX = Regex("\\s{2,}")
        val WORD_REGEX = Regex("[\\p{L}][\\p{L}\\p{M}'’-]*")
        val NON_LETTER_OR_DIGIT_REGEX = Regex("[^\\p{L}\\p{N}]+")
        val NON_ALPHANUMERIC_SPACE_REGEX = Regex("[^\\p{L}\\p{N} ]+")
        val ORPHANED_PUNCTUATION_REGEX = Regex(
            "(?:^|\\s)[,;:/|]+(?=\\s|$)"
        )

        val SPACE_AROUND_APOSTROPHE_REGEX = Regex("\\s*['’‘`´]\\s*")
        val SPACE_AROUND_HYPHEN_REGEX = Regex("\\s+[-‐-‒–—−]\\s*|\\s*[-‐-‒–—−]\\s+")
        val MULTIPLE_HYPHEN_REGEX = Regex("[-‐-‒–—−]{2,}")

        val NUMERIC_COMPOUND_WITHOUT_HYPHEN_REGEX = Regex(
            "(?i)\\b\\d+(?:[.,]\\d+)?\\s*prozent\\s+[\\p{L}]+"
        )

        val ABBREVIATION_REGEX = Regex(
            "(?<![\\p{L}\\p{N}])(?:[A-ZÄÖÜ]{2,6}|[A-Za-zÄÖÜäöüß]{1,4}\\.)" +
                    "(?![\\p{L}\\p{N}])"
        )

        val PACKAGE_SIZE_REGEX = Regex(
            pattern =
                "(?i)(?<![\\p{L}\\p{N}])" +
                        "(?:\\d+\\s*[x×]\\s*)?" +
                        "\\d+(?:[.,]\\d+)?" +
                        "\\s*" +
                        "(?:µg|ug|mg|g|kg|ml|cl|dl|l|" +
                        "stk\\.?|stueck|stück|portion(?:en)?|" +
                        "beutel|dose(?:n)?|glas|glaeser|gläser|" +
                        "flasche(?:n)?|packung(?:en)?)" +
                        "(?![\\p{L}\\p{N}])"
        )

        val PRODUCT_NUMBER_REGEX = Regex(
            pattern =
                "(?i)(?<![\\p{L}\\p{N}])" +
                        "(?:nr\\.?|nummer|no\\.?|typ|type|variante|" +
                        "artikel(?:nummer)?|sku)" +
                        "\\s*[:.#-]?\\s*" +
                        "[a-z0-9][a-z0-9._/-]*" +
                        "(?![\\p{L}\\p{N}])"
        )

        val PLURAL_METADATA_REGEX = Regex(
            "(?i)\\((?:plural|mehrzahl|pl\\.?|auch|selten)[^)]*\\)"
        )

        val PLURAL_ALTERNATIVE_REGEX = Regex(
            "(?i)\\s+(?:oder|bzw\\.)\\s+|[/|;]"
        )

        val NON_CANONICAL_APOSTROPHES = setOf(
            '’',
            '‘',
            '`',
            '´',
            'ʼ',
            '＇'
        )

        val NON_CANONICAL_HYPHENS = setOf(
            '‐',
            '-',
            '‒',
            '–',
            '—',
            '−',
            '﹘',
            '﹣',
            '－'
        )

        val ALLOWED_ABBREVIATIONS = setOf(
            "bio",
            "tk",
            "uvp"
        )

        val ALLOWED_INTERNAL_CAPITALIZED_WORDS = setOf(
            "Bio",
            "Curry",
            "Gouda",
            "Kefir",
            "Parmesan",
            "Pesto",
            "Pizza",
            "Tofu"
        )

        val UMLAUT_REPLACEMENT_PATTERNS = linkedMapOf(
            "aepfel" to "Äpfel",
            "apfelsaefte" to "Apfelsäfte",
            "baecker" to "Bäcker",
            "baerlauch" to "Bärlauch",
            "blaetter" to "Blätter",
            "broetchen" to "Brötchen",
            "bruehe" to "Brühe",
            "fruechte" to "Früchte",
            "fruehstueck" to "Frühstück",
            "gebaeck" to "Gebäck",
            "gefluegel" to "Geflügel",
            "gemuese" to "Gemüse",
            "gewuerz" to "Gewürz",
            "gewuerze" to "Gewürze",
            "gruen" to "grün",
            "gruene" to "grüne",
            "gruenen" to "grünen",
            "haehnchen" to "Hähnchen",
            "kaese" to "Käse",
            "kraeuter" to "Kräuter",
            "kuerbis" to "Kürbis",
            "kueken" to "Küken",
            "muesli" to "Müsli",
            "nuesse" to "Nüsse",
            "oel" to "Öl",
            "oele" to "Öle",
            "raeucher" to "Räucher",
            "roest" to "Röst",
            "suesse" to "süße",
            "suess" to "süß",
            "tiefgekuehlt" to "tiefgekühlt",
            "tuerkisch" to "türkisch",
            "wuerze" to "Würze",
            "wuerzig" to "würzig"
        )

        val TYPO_REPLACEMENTS = linkedMapOf(
            "almonde" to "Mandel",
            "ananasen" to "Ananas",
            "auberginie" to "Aubergine",
            "bannanen" to "Bananen",
            "bananne" to "Banane",
            "blumenkol" to "Blumenkohl",
            "broccoli" to "Brokkoli",
            "champinon" to "Champignon",
            "champion" to "Champignon",
            "cous-cous" to "Couscous",
            "cous cous" to "Couscous",
            "creme fraiche" to "Crème fraîche",
            "frischkaese" to "Frischkäse",
            "gelee royal" to "Gelée royale",
            "gorgonzolla" to "Gorgonzola",
            "hüner" to "Hühner",
            "kartoffell" to "Kartoffel",
            "kichererbsse" to "Kichererbse",
            "kohlrabiie" to "Kohlrabi",
            "korianderkraut" to "Koriander",
            "mozarella" to "Mozzarella",
            "parmesanerge" to "Parmesan",
            "papricka" to "Paprika",
            "peperonie" to "Peperoni",
            "petersielie" to "Petersilie",
            "pinienkerne" to "Pinienkerne",
            "quiona" to "Quinoa",
            "rucolla" to "Rucola",
            "sourcream" to "Sour Cream",
            "spagetti" to "Spaghetti",
            "spinatblätterer" to "Spinatblätter",
            "toffu" to "Tofu",
            "vanillie" to "Vanille",
            "zuchini" to "Zucchini"
        )

        val RETAILER_TERMS = setOf(
            "aldi",
            "aldi nord",
            "aldi süd",
            "alnatura markt",
            "biomarkt",
            "combi",
            "denn's",
            "denns",
            "edeka",
            "famila",
            "globus",
            "hit",
            "kaufland",
            "lidl",
            "marktkauf",
            "metro",
            "netto",
            "netto marken-discount",
            "norma",
            "penny",
            "real",
            "rewe",
            "tegut",
            "wasgau"
        )

        val BRAND_TERMS = setOf(
            "alpro",
            "andros",
            "ariel",
            "barilla",
            "bauer",
            "berchtesgadener land",
            "bärenmarke",
            "buitoni",
            "bürger",
            "campina",
            "coca-cola",
            "danone",
            "de cecco",
            "dr. oetker",
            "ehrmann",
            "ferrero",
            "frosta",
            "gazi",
            "gervais",
            "goldsteig",
            "haribo",
            "heinz",
            "hohes c",
            "iglo",
            "ja!",
            "kerrygold",
            "kinder",
            "knorr",
            "kühne",
            "landliebe",
            "leerdammer",
            "leibniz",
            "maggi",
            "meggle",
            "milka",
            "müller",
            "müllermilch",
            "mymuesli",
            "natreen",
            "nestlé",
            "nutella",
            "original wagner",
            "rama",
            "ritter sport",
            "schwartau",
            "seitenbacher",
            "söbbeke",
            "valess",
            "vegeta",
            "vivera",
            "wasa",
            "zott"
        )

        val ENGLISH_FOOD_TERMS = setOf(
            "apple",
            "bacon",
            "beans",
            "beef",
            "berry",
            "berries",
            "bread",
            "breast",
            "butter",
            "cake",
            "candy",
            "cheese",
            "chicken",
            "chips",
            "cream",
            "cucumber",
            "drink",
            "fish",
            "flour",
            "food",
            "fruit",
            "garlic",
            "grape",
            "ham",
            "juice",
            "lemon",
            "milk",
            "mushroom",
            "noodles",
            "onion",
            "orange",
            "pasta",
            "pepper",
            "pork",
            "potato",
            "rice",
            "salad",
            "sauce",
            "sausage",
            "seed",
            "seeds",
            "snack",
            "soup",
            "strawberry",
            "sugar",
            "sweet",
            "tomato",
            "turkey",
            "vegetable",
            "water",
            "wheat",
            "yogurt"
        )

        val GERMAN_FOOD_TERMS = setOf(
            "apfel",
            "banane",
            "beere",
            "birne",
            "bohne",
            "brot",
            "butter",
            "erdbeere",
            "fisch",
            "fleisch",
            "frucht",
            "gemüse",
            "gurke",
            "hähnchen",
            "joghurt",
            "käse",
            "kartoffel",
            "kirsche",
            "kohl",
            "milch",
            "möhre",
            "nudel",
            "obst",
            "paprika",
            "reis",
            "salat",
            "sauce",
            "schinken",
            "suppe",
            "tomate",
            "wasser",
            "weizen",
            "wurst",
            "zucker",
            "zwiebel"
        )

        val INVARIANT_SINGULAR_PLURAL_WORDS = setOf(
            "ananas",
            "brokkoli",
            "couscous",
            "fisch",
            "fleisch",
            "gemüse",
            "kaffee",
            "käse",
            "mais",
            "obst",
            "reis",
            "salami",
            "sellerie",
            "spinat",
            "tofu"
        )

        val GERMAN_S_PLURAL_EXCEPTIONS = setOf(
            "avocado",
            "café",
            "kiwi",
            "mango",
            "party",
            "pizza",
            "taco"
        )

        val LANGUAGE_ISSUE_COMPARATOR =
            compareBy<CatalogLanguageIssue>(
                { severityRank(it.severity) },
                { it.type.name },
                { it.sourceIndex },
                { it.field },
                { it.originalValue ?: "" },
                { it.suggestedValue ?: "" },
                { it.message }
            )

        fun severityRank(
            severity: CatalogIssueSeverity
        ): Int =
            when (severity) {
                CatalogIssueSeverity.ERROR -> 0
                CatalogIssueSeverity.WARNING -> 1
                CatalogIssueSeverity.INFO -> 2
            }
    }
}