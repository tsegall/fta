
## Changes ##

### 18.15.1
 - fix: FakerDoubleLT monotonic_decreasing used Long.MIN_VALUE sentinel instead of Double.MIN_VALUE, preventing correct reset
 - fix: FakerEnumLT monotonic_increasing wrapped at options.length+1 causing ArrayIndexOutOfBoundsException on full cycle
 - test: Add TestFaker covering all faker types (Boolean, Enum, String, Long, Double, LocalDate, LocalDateTime, LocalTime, OffsetDateTime) and all distribution modes
 - test: Add TestPluginLocaleEntry covering all PluginLocaleEntry methods (header confidence, regExp returned, match entry index, copy constructor, simple factory, toString)
 - test: Add TestLogicalTypeRegExp covering LONG/DOUBLE/STRING bounds, invalidList, isMatch, and xeger-incompatible nextRandom
 - test: Add testMinMax and testRange to TestUtilsCore covering MinMax and Range constructors, merge, pattern generation, compareTo, equals, hashCode
 - test: Add escalationEquals to TestStandalonePlugins covering TypeDeterminer.Escalation equals/hashCode contract
 - test: Add TestPatternFG covering case-insensitive word-set matching and candidate first-char fast-reject
 - test: Add TestHeaderEntry covering lazy pattern init, compositeKey path, copy constructor, and toString
 - test: Add TestSemanticType covering getAllSemanticTypes (cached), getActiveSemanticTypes, all getters, and toJSONString
 - test: Add TestRecordAnalyzer covering wrong-length train, getAnalyzer, getAnalyzers, merge, and end-to-end
 - test: Add TestPluginDefinition covering getOrder (all Precedence values), findByName, copy constructor, getLocaleEntry, isMandatoryHeaderUnsatisfied, isLocaleSupported, getLocaleDescription, and getOptions
 - test: Extend TestLogicalTypeRegExp with getMatchEntries, getMinSamples, isMinMaxPresent, isClosed, getBaseType, setMatchEntry/getMatchEntry, and seed
 - test: Add TestCacheLRU covering put/get, invalidate, and size
 - test: Add TestLogicalType covering getPriority, isLocaleSensitive, isRegExpComplete, getHeaderConfidence (both overloads), setThreshold, acceptsBaseType, getPluginDefinition, isValid, compareTo, LogicalTypeFactory exception paths, and LogicalTypeBloomFilter backout
 - test: Add TestIdentityValidators covering NPI_US analyzeSet (OK and below-threshold), isValid, getRegExp, and BSN_NL analyzeSet backout, acceptsBaseType, isValid
 - test: Add TestExternalFacts covering copy constructor (all 13 fields), equals contract (identity/null/wrong class/copied/mutated), and default constructor sentinel values
 - test: Extend TestText with getDigits, getSentenceBreaks, and getWordBreaks
 - test: Add TestTypeInfo covering all 8 flag getters, setters (regExp/baseType/semanticType/force), toString, and hashCode contract
 - test: Extend TestUtilsCore with repeat (all 4 count branches), replaceAt, sortByValue, getRandomAlphas, isSimpleAlphas, getValue (all branches), isSafeRegExp, containsIgnoreCase, parseLong (+prefix and trailing-minus), parseDouble (trailing-minus), and determineStreamFormat OTHER path (Utils: 77.6% → 94.4%)
 - test: Add TestLogicalTypeFiniteSimpleExternal covering lowercase-check-throws for English locale, uppercase-OK, non-English lowercase OK, and empty-members OK (100% coverage)
 - chore: bump jackson-annotations to 2.21, slf4j-api to 2.0.18

### 18.15.0
 - feat: Add COLOR.TEXT_IT, COLOR.TEXT_PT, COLOR.TEXT_RU semantic types for Italian, Portuguese, and Russian color names
 - feat: Add POSTAL_CODE.POSTAL_CODE_IT and POSTAL_CODE.POSTAL_CODE_RU semantic types
 - feat: Add COLOR.TEXT_SV, COLOR.TEXT_FI, COLOR.TEXT_DA, COLOR.TEXT_PL, COLOR.TEXT_RO semantic types
 - feat: Add CONTINENT.TEXT_FR, CONTINENT.TEXT_DE, CONTINENT.TEXT_ES, CONTINENT.TEXT_IT, CONTINENT.TEXT_NL, CONTINENT.TEXT_PT, and CONTINENT.TEXT_RU semantic types
 - feat: CLI --signature --pluginName <name> now auto-detects plugin locale (no --locale required) and prints to stdout
 - feat: Add POSTAL_CODE.POSTAL_CODE_BR semantic type for Brazilian CEP (\\d{5}-\\d{3})
 - feat: Add COUNTRY.TEXT_IT, COUNTRY.TEXT_PT, COUNTRY.TEXT_RU semantic types for Italian, Portuguese, and Russian country names
 - feat: Add CONTINENT.TEXT_DA, CONTINENT.TEXT_SV, CONTINENT.TEXT_FI, CONTINENT.TEXT_PL, CONTINENT.TEXT_RO semantic types
 - feat: Add POSTAL_CODE_DK, POSTAL_CODE_FI, POSTAL_CODE_PL, POSTAL_CODE_RO, POSTAL_CODE_NO, POSTAL_CODE_AT, POSTAL_CODE_CH semantic types
 - feat: Add COUNTRY.TEXT_DA, COUNTRY.TEXT_SV, COUNTRY.TEXT_FI, COUNTRY.TEXT_PL, COUNTRY.TEXT_RO semantic types
 - feat: Add LANGUAGE.TEXT_FR, LANGUAGE.TEXT_DE, LANGUAGE.TEXT_ES, LANGUAGE.TEXT_IT, LANGUAGE.TEXT_PT, LANGUAGE.TEXT_RU, LANGUAGE.TEXT_NL semantic types
 - feat: Add LANGUAGE.TEXT_DA, LANGUAGE.TEXT_SV, LANGUAGE.TEXT_FI, LANGUAGE.TEXT_PL, LANGUAGE.TEXT_RO semantic types
 - fix: cleanse() now maps NO-BREAK SPACE (U+00A0) and NARROW NO-BREAK SPACE (U+202F) to regular space
 - feat: Add COLOR.TEXT_CS, CONTINENT.TEXT_CS, COUNTRY.TEXT_CS, LANGUAGE.TEXT_CS semantic types for Czech

### 18.14.4
 - chore: Bump libphonenumber to 9.0.31
 - refactor: Extract locale-specific tests into per-language files under com.cobber.fta.internationalization (French, German, Italian, Portuguese, Bulgarian, Russian, Japanese date tests)
 - docs: Add Javadoc to TextAnalyzer.getRegExp(KnownTypes.ID)

### 18.14.3
 - refactor: Move getResult() body into ResultFinalizer.buildResult(); remove nine dead wrapper methods from TextAnalyzer (backout, lengthQualifier, checkDateTimeTypes, switchToDate, finalizeLong, killInvalidDates, finalizeBoolean, finalizeDouble, finalizeString)
 - refactor: Facts.java — make cardinality private with getCardinality()/setCardinality() (setter preserves maxCapacity); add lifecycle phase documentation (ACCUMULATION/FINALIZATION/RESULT); make ExternalFacts fields private with public getters/setters

### 18.14.2
 - feat: webNG — Generate Records panel: POST /api/generate accepts Faker spec + record count + locale, returns synthetic CSV; output area is scrollable and copyable
 - feat: webNG — Faker Specification moved from always-visible panel to a slide-in triggered by a "Faker Spec" button in the Results table header, consistent with the per-field JSON popout
 - refactor: Faker implementation classes (FakerParameters, FakerLT, FakerBooleanLT, FakerDoubleLT, FakerEnumLT, FakerLocalDate/Time/DateTimeLT, FakerOffsetDateTimeLT, FakerLongLT, FakerStringLT) moved from cli to types (com.cobber.fta.faker), eliminating duplication between cli and webNG
 - fix: FakerLocalTimeLT — default format was "yyyy-MM-dd HH:mm:ss" (wrong for a time-only type); corrected to "HH:mm:ss"
 - fix: FakerLongLT — three duplicate assignments to high field removed
 - fix: webNG — base-type fakers failed for non-English locales (e.g. ja-JP) because PluginDefinition 2-arg constructor hardcodes "en"; validLocales now overridden to wildcard in GenerationService
 - fix: FakerStringLT — FREE_TEXT plugin lookup wrapped in try-catch; falls back to regex generator or numeric placeholder when locale is unsupported
 - chore: settings.gradle — add webNG to includeBuild list
 - chore: webNG build.gradle — add mavenLocal() to repositories to support development builds against locally-published fta
 - chore: Bump spotbugs plugin 6.5.4→6.5.5

### 18.14.1
 - fix: cli --help — correct --faker description (was "header is a comma separated list of Semantic Types"; now documents JSON array of column specs)
 - fix: cli --noQuantiles incorrectly disabled COLLECT_STATISTICS instead of DISTRIBUTIONS; now correctly disables quantile/histogram tracking
 - fix: cli --maxCardinality listed in --help but never parsed; added to DriverOptions.addFromStringArray
 - fix: cli --help — add missing options: --noDistributions, --noNullTextAsNull, --output
 - chore: Bump fastcsv 4.2.0→4.3.0
 - feat: webNG — Semantic Types page columns (ID, Description, Languages) are now sortable; uses ↕/↑/↓ indicators consistent with the Analysis page
 - docs: CLAUDE.md — add Releasing a New Version section (version location, example version-bump rule, publishing workflow); fix stale AGENTS.md reference; document JDK 22+ rejection; add Gradle wrapper version; name ChangeLog.md explicitly; note webNG in module structure
 - chore: Upgrade GitHub Actions release workflow — add staging-to-Central promotion step (eliminating PUBLISH.sh), upgrade actions/checkout@v4→v6, actions/setup-java@v4→v5, gradle/actions/wrapper-validation@v3→v6, bump Java runner from 17 to 21

### 18.14.0
 - fix: GENDER.TEXT_HR — GenderPair word and abbreviation order was swapped (MUŠKARCI/ŽENE → ŽENE/MUŠKARCI, M/Z → Z/M); affected nextRandom() labelling and opposites map
 - fix: GENDER.TEXT_PL — removed mojibake entry "MÊ¿CZYŸNI" (garbled encoding of MĘŻCZYŹNI) from Java plugin; removed garbled "P³eæ" alternate from plugins.json header regexp
 - fix: GENDER.TEXT_RO — inconsistent noun/adjective pair FEMEIE/MASCULIN replaced with consistent noun pair FEMEIE/BĂRBAT
 - fix: GENDER.TEXT_SV — single inconsistent pair KVINNA/MANLIG (noun+adjective) replaced with KVINNA/MAN and KVINNLIG/MANLIG
 - fix: GENDER.TEXT_<LANGUAGE> Finnish header regexp contained French word "Genre"; removed, leaving only "Gender" and "Sukupuoli"
 - fix: GENDER.TEXT_<LANGUAGE> Croatian header regexp missing native term; added "spol"
 - fix: GENDER.TEXT_<LANGUAGE> Spanish header regexp — "genero" (ASCII) did not match accented "género"; changed to (?iu)(Gender|gen[eé]ro|Sexo) in both confidence-99 and confidence-90 entries
 - test: Add TestInternationalization.java covering GENDER.TEXT_<LANGUAGE> for all 13 previously untested locales (bg, ca, es, fi, fr, hr, is, it, ms, pl, ro, ru, sv)
 - chore: Bump all example build.gradle FTA dependency ranges from 17.+ to 18.+
 - fix: minicli — missing import for AllocationTracker; bump fastcsv 3.7.0→4.2.0 and logback-classic 1.5.17→1.5.32 to match version catalog
 - docs: README — update Maven artifact version, add Java 17/21 requirements note, fix priority field description
 - docs: CLAUDE.md — fix priority field description to reflect ascending evaluation order and current range (100–2500)
 - fix: GENDER.TEXT_FR — "GARCON" (ASCII) did not match natural French spelling "garçon"; corrected to "GARÇON"
 - fix: COLOR.TEXT_ES — removed duplicate GRIS entry from es_color.csv
 - fix: COLOR.TEXT_NL — removed duplicate PAARS entry from nl_color.csv
 - test: Add COLOR.TEXT_ES and COLOR.TEXT_NL detection and no-header tests to TestStandalonePlugins
 - fix: COUNTRY.TEXT_DE — missing "threshold": 90 caused stricter default (95) than all peer language plugins; added to match ES/JA/NL
 - fix: COUNTRY.TEXT_ES — header regexp ".*(?i)(pais).*" did not match accented header "País"; corrected to ".*(?iu)(pa[ií]s).*"
 - feat: Add COUNTRY.TEXT_FR plugin (193 French country names, header hint "pays", priority 1044 evaluated before COUNTRY.TEXT_EN)
 - feat: Expand ja_countries.csv from 71 to 332 entries — add all 249 JIS X 0304 modern katakana/kanji names, kanji abbreviations (米国, 英国, 中国, 韓国, 北朝鮮, etc.), short katakana forms (アメリカ, ロシア, イラン, ベネズエラ), and split slash-notation dead entries into individual members
 - test: Add jaCountryDetectionModern covering modern JIS katakana names, kanji abbreviations, and short forms absent from the original list

### 18.13.0
- feat: Resequence priorities - so we can support internationization more simply.

### 18.12.0
- fix: COORDINATE.LONGITUDE_DECIMAL and COORDINATE.LATITUDE_DECIMAL not detected for ja-JP locale — 経度/緯度 appeared in the ca/da/de/... header regexp but ja was absent from that localeTag; added dedicated ja locale entries with period-only decimal separator
- fix: Remove 経度/緯度 dead code from ca,da,de,es,fi,fr,it,nl,ro,ru header regexps in COORDINATE.LATITUDE_DECIMAL and COORDINATE.LONGITUDE_DECIMAL — unreachable after ja locale entries were added

### 18.11.0
- feat: Add COLOR.TEXT_FR, COLOR.TEXT_DE, and COLOR.TEXT_JA semantic type plugins with French, German, and Japanese color name lists
- feat: Add LANGUAGE.TEXT_JA semantic type plugin with 85 Japanese language names (言語/母国語/母語 header hints)
- feat: Add ja locale support to COMPANY_NAME plugin (会社名/企業名/組織名/法人名/取引先名/商号 headers, mandatory)
- feat: Add HONORIFIC_JA semantic type plugin with 11 Japanese honorifics (様/さん/氏/先生/殿/君/くん/ちゃん/博士; 敬称/称号 header hints)
- feat: Add NATIONALITY_JA semantic type plugin with 107 Japanese demonyms (国籍/出身国 header hints); includes doublets 米国人/アメリカ人 and 英国人/イギリス人
- feat: Add CURRENCY.TEXT_JA semantic type plugin with 113 Japanese currency names (通貨/通貨名 header hints)
- feat: Add CONTINENT.TEXT_JA semantic type plugin with 11 Japanese continent names including alternates 欧州/北米/南米 (大陸/地域 header hints)
- feat: Add CRYPTOCURRENCY.TEXT_JA semantic type plugin with 45 Japanese cryptocurrency names (暗号通貨/仮想通貨/暗号資産 header hints)
- feat: Add JOB_TITLE_JA Java plugin detecting Japanese job titles via productive suffix heuristic (長/員/士/師/者/役/官) plus 37-entry known-titles list covering katakana loan words and irregular forms (役職/職種/職位/肩書 header hints)
- feat: Add PERSON.MARITAL_STATUS_JA Java plugin with 10 Japanese marital status terms (婚姻状況/婚姻/配偶者状況/結婚状況 header hints; mandatory header)
- feat: Add STATE_PROVINCE.REGION_NAME_JA list plugin with 20 entries covering Japan's 8 regional groupings in short/long/alternate forms (地方/地域区分/地域/地区 header hints)
- feat: Add INDUSTRY_JA Java plugin detecting Japanese industry names via 業/産業/業界 suffix heuristic plus 100-entry JSIC-based list; mandatory header (業界/業種/産業)

### 18.10.1
 - fix: GENDER.TEXT_ZH had wrong signature (duplicated GENDER.TEXT_JA's); corrected to XYuZCo4RjIIlf+CjvWH7/QGvKsM=
 - fix: STATE_PROVINCE.STATE_UNION_IN backout pattern double-escaped (\\\\p → \\p), making it an invalid regex at runtime
 - fix: NAME.LAST had malformed Wikipedia URL with duplicated prefix
 - fix: Age plugin now falls back to English locale when GENDER.TEXT_<LANGUAGE> does not support the active locale (same pattern already used for NAME.FIRST), enabling PERSON.AGE ja locale support without crashing
 - feat: Add ja locale header patterns to PERSON.AGE (年齢|年令), PERIOD.QUARTER (四半期), MONTH.DIGITS (月|月号|月数), DAY.DIGITS (日|日数), and FREE_TEXT (説明|備考|コメント|理由|記述|注記)
 - fix: TestStandalonePlugins used jp-JP (country code) instead of ja-JP (language code), masking all Japanese plugin test coverage; corrected locale tag
 - fix: PREFECTURE_ISO_JA and PREFECTURE_CODE_JA had wrong signatures (never validated due to jp-JP typo above); corrected
 - fix: jp_free_text_samples.csv renamed to ja_free_text_samples.csv to match the <LANGUAGE> template resolution used by FREE_TEXT plugin
 - fix: DISTRICT_NAME_IN locale tag had spurious space ("en-IN, hi-IN" → "en-IN,hi-IN")
 - fix: FREE_TEXT(ja-JP) validation failed because TextProcessor is space-based and Japanese has no word breaks; added Japanese-aware path in FreeText.isValid() using CJK character detection
 - fix: COORDINATE.LATITUDE_DECIMAL and COORDINATE.LONGITUDE_DECIMAL incorrectly included ja in the comma-decimal-separator locale group; Japan uses period as decimal separator, causing Xeger-generated values with commas to fail the range check
 - fix: POSTAL_CODE.POSTAL_CODE_JP had wrong signature (never validated due to jp-JP typo); corrected to QUMMXDoltEC7srC16M5LwjYV3Dc=
 - fix: AddressJA.isValid() rejected sample address 神奈川県横浜市中区山下町1 (city marker + single trailing number, no dashes); added acceptance path for this valid Japanese address form
 - fix: EmailLT.nextRandom() infinite loop for ja-JP locale — NAME.FIRST ja returns non-ASCII Japanese names, causing the isAscii retry loop to never terminate; email generation now always uses English locale since email addresses require ASCII
 - fix: CREDIT_CARD_TYPE had duplicate header regexp at confidence 90 and 70; the 70-confidence entry was unreachable dead code — removed
 - fix: COORDINATE.LATITUDE_DECIMAL and COORDINATE.LONGITUDE_DECIMAL incorrectly included zh in the comma-decimal-separator locale group; Chinese uses period as decimal separator (same bug as ja, fixed previously)
 - fix: PERSON.AGE_RANGE missing ja locale entry (年齢|年令 header patterns); added to match the ja locale support already present in PERSON.AGE
 - fix: STATE_PROVINCE.STATE_UNION_IN and STATE_PROVINCE.PROVINCE_ZA shared priority 128; STATE_UNION_IN bumped to 129
 - refactor: Extract Japanese.isJapaneseChar() utility; fixes AddressJA missing 々 (U+3005) and adds halfwidth katakana (U+FF65–U+FF9F) coverage; removes duplicate private implementations in FreeText, AddressJA, and NameLastFirst
 - fix: STATE_PROVINCE.PREFECTURE_CODE_JA header pattern was not mandatory, allowing it to win in TypeDeterminer over MONTH.DIGITS/DAY.DIGITS for numeric data then back out; marked mandatory to match its analyzeSet() behavior which always rejects without a header match
 - fix: PERSON.AGE ja header confidence bumped from 90 to 100; 年齢/年令 are unambiguous person-age terms enabling standalone detection without requiring composite sibling signals


### 18.10.0
 - chore: Bump Spring Boot 3.5.3 → 4.0.6 in examples/webNG; migrate Jackson imports from com.fasterxml.jackson to tools.jackson (Jackson 3.x package rename)
 - feat: Wire Japanese city names into CITY plugin — add ja_cities.csv reference data and load it for nextRandom() under ja locale; fix analyzeSet() to let a 99-confidence header bypass the maxLength guard (previously blocked all-short Japanese city names)
 - feat: Add STATE_PROVINCE.PREFECTURE_ISO_JA — Japanese prefecture ISO 3166-2 codes (JP-01 through JP-47), detected for ja locale
 - feat: Add STATE_PROVINCE.PREFECTURE_CODE_JA — Japanese prefecture JIS X 0401 numeric codes (01–47), header-gated to avoid false positives 
 - chore: Cleanup old web example

### 18.9.3
 - chore: Bump webNG frontend — vite 5→8, @vitejs/plugin-vue 5→6, vue-router 4→5, tailwindcss 3→4 (switched to @tailwindcss/vite plugin, CSS-based theme config, removed postcss/autoprefixer)
 - chore: Bump gradle to 9.5.1
 = docs: Add documentation to README for approxDistinctCount
 - docs: Move to Conventional Commits (https://www.conventionalcommits.org/) for ChangeLog

### 18.9.2
 - feat: Add examples/webNG — Spring Boot REST API (POST /api/analyze, GET /api/types, GET /api/version) with Vue 3 + Tailwind CSS frontend; features drag-and-drop upload, sortable/filterable results table, slide-over JSON detail panel, and locale-sensitive semantic types page with shared locale state across views.
 - fix: examples/web locale not applied to file analysis — Spring MVC bound the file field before the locale field (HTML form order); fixed by deferring CSV processing to getAnalysisResult() so all setters complete first.
 - fix: examples/web FastCSV 4.x throws IllegalArgumentException on duplicate CSV header fields; fixed via NamedCsvRecordHandler.allowDuplicateHeaderFields(true).
 - test: Add tests for POSTAL_CODE_CO, POSTAL_CODE_FR and POSTAL_CODE_UK covering isValid() valid/invalid inputs, end-to-end detection, and low-cardinality backout path.
 - test: Add unit tests for CircularBuffer, RandomSet, FloatToken, SimpleToken (all previously at 0% coverage), STREET_NAME_BARE_NL integration test, and LogicalTypeRegExp matchEntry branch coverage.

### 18.9.1
 - fix: Doubled https:// in plugins.json documentation references for EMAIL and COMPANY_NAME.
 - fix: CLI --createSemanticTypesMarkdown wrote to stderr instead of stdout.
 - docs: Multiple README fixes — stale Maven version (18.0.0→18.9.0), broken Email Wikipedia URL, matchedCount→matchCount, doubled-word typos, spurious apostrophe, Record mode description rewritten to lead with cross-stream context rationale, STREET_ADDRESS_JA added to Address Detection section.
 - docs: CLAUDE.md updated — Java 21 runtime requirement, SemanticTypes.md regeneration command, ChangeLog conventions, plugin signature/priority field guidance, plugin validation CLI pattern.

### 18.9.0
 - feat: Add STREET_ADDRESS_JA semantic type for Japanese street addresses (住所) — detects addresses with or without 〒XXX-XXXX postal prefix, requiring Japanese characters and block/lot notation in abbreviated numeric (1-2-3) or explicit kanji (丁目/番地/条) form; city-level markers (市/区/町/村) are recognised as an additional signal but not required (neighborhood+lot form is common in database address fields). Header hints: 住所, 所在地 (Issue #165).
 - fix: TELEPHONE not detected for Japanese locale ("ja") — locale.getCountry() returns empty for language-only tags; now maps "ja"→"JP", "ko"→"KR", "zh"→"CN", "hi"→"IN", "ar"→"SA" for phone number validation (Issue #165).

### 18.8.0
 - feat: Add Japanese locale support for NAME.LAST_FIRST — detects full names in Japanese Last-First order (姓名), supporting both space-separated (田中 太郎) and concatenated (田中太郎) forms using bloom filters. Header hints: 氏名, 姓名, 名前 (Issue #165).
 - feat: Add approxDistinctCount to TextAnalysisResult using Apache DataSketches HyperLogLog — opt-in via Feature.APPROX_DISTINCT_COUNT, exact for low-cardinality fields, ~1% error estimate for high-cardinality fields. Supports merge. Fixes Issue #92.
 - feat: Add Feature.COLLECT_SHAPES flag (enabled by default) to allow disabling shape tracking and serialization — reduces JSON payload size and CPU overhead in distributed/Spark workloads. Fixes Issue #166.
 - chore: Bump libphonenumber to 9.0.30

### 18.7.0
 - feat: Japanese locale support (Kanji) for NAME.FIRST and NAME.LAST using JMnedict-sourced bloom filters (Issue #165)

### 18.6.0
 - feat: Improve detection in Japanese locale: COUNTRY.TEXT_JA, PERSON.DATE_OF_BIRTH, CITY, EMAIL, TELEPHONE, POSTAL_CODE.POSTAL_CODE_JA (Issue #165)

### 18.5.0
 - feat: Japanese era date detection (令和, 平成, 昭和, etc.) with support for 元年 (traditional first-year-of-era notation) — including GGGGy*/GGGGyy* formats, JapaneseChronology, getRegExp(), parse(), ofPattern(), and statistics tracking (Issue #165)
 - chore: Bump spotbugs plugin to 6.5.4

### 18.4.0
 - fix: getShapeDetails() returns empty map after serialize/deserialize round-trip (Issue #166)
 - chore: Bump spotbugs plugin to 6.5.1, ben-manes versions plugin to 0.54.0, libphonenumber to 9.0.29, jackson to 2.21.3, gradle to 9.5.0
 - chore: Couple of security hardening fixes

### 18.3.0
 - chore: Bump spotbugs plugin to 6.5.0
 - chore: Bump guava to 33.6.0-jre
 - fix: Custom LOCALDATETIME (and other date/time) infinite-type plugins not evaluated during initial type determination (Issue #164)

### 18.2.0
 - chore: Code restructuring

### 18.1.0
 - chore: Improve security posture
 - chore: Fix synchronization issues
 - feat: Add totalInvalidCount and totalMatchCount to ExternalFacts to support tracking invalid/match counts across merged data streams (Issue #147)
 - fix: Fix duplicate totalBlankCount merge
 - feat: Remove deprecated registerPlugins methods
 - chore: Bump google phonenumber to 9.0.28

### 18.0.0
 - BREAKING CHANGE: Bump fastcsv to 4.2.0, Java target to 17, jacoco to 0.8.14
 - chore: Minor changes to support new version of fastcsv
 - fix: Fix synchronization issue with caching plugin definitions

### 17.5.7
 - chore: Bump gradle to 9.4.1, jackson to 2.21.2, google phonenumber to 9.0.27
 - fix: regExpReturned not being honored (Issue #159)

### 17.5.6
 - chore: Bump google phonenumber to 9.0.26
 - fix: Priority test for finite types was inverted (Issue #161)

### 17.5.5
 - chore: Bump gradle to 9.4.0

### 17.5.4
 - fix: Fix bug related to setting max value for RegularExpression Semantic Types (Issue #159)

### 17.5.3
 - chore: Bump google phonenumber to 9.0.24
 - fix: Reprocess data associated with DateTime types before declaring success (Issue #157)

### 17.5.2
 - feat: Move PluginCustomLocalDate to src from tests (and use it to re-implement BirthYear) (Issue #157)

### 17.5.1
 - chore: Bump jackson to 2.21.1

### 17.5.0
 - chore: Bump logback-classic to 1.5.31, google phonenumber to 9.0.24
 - feat: Improve support for Java plugins derived from DateTime types (Issue #157)
 - BREAKING CHANGE: pluginOptions in the JSON plugin definition are now a JSON object not a String

### 17.4.2
 - fix: User-defined plugins not being serialized (Issue #155)

### 17.4.1
 - chore: Bump logback-classic to 1.5.27, google phonenumber to 9.0.23, gradle to 9.3.1
 - chore: Cleaning.
 - fix: User-defined plugins not registered when merging two analyzers (Issue #155)

### 17.4.0
 - chore: Bump testng to 7.12.0, logback-classic to 1.5.26
 - feat: Improve ZIP detection if have data of the form 99999- (i.e. a trailing hyphen)
 - feat: CLI - Fix bug when validating data based on a plugin
 - feat: Add support for biasing detection based on composite name (Table/File) (Issue #154)

### 17.3.1
 - chore: Bump logback-classic to 1.5.25, Jackson to 2.21.0
 - chore: Cleaning.
 - chore: More gradle 10.0 changes

### 17.3.0
 - feat: Change approach to registering plugins - no longer require dataStream name.  Some methods added and some deprecated.

### 17.2.0
 - chore: Bump logback-classic to 1.5.24, gradle to 9.3.0, google phonenumber to 9.0.22
 - feat: CLI - Add --semanticTypesPre to control whether user-defined types (--semanticType) are registered before built-ins
 - fix: findByName on TextAnalyzer was not returning user-defined types
 - fix: Preserve precedence on plugins registered on TextAnalyzer template for RecordAnalyzer (Issue #152)
 - fix: Check for better RegExpTypes even if we have already detected a Semantic Type (Issue #152)
 - chore: Remove remaining references to deprecated registerPluginList()

### 17.1.10
 - chore: Bump logback-classic to 1.5.23, SpotBugs to 6.4.8, commons-text:1.15.0, google phonenumber to 9.0.21
 - feat: Small improvement to Semantic Type NAME.FIRST_LAST to reject more company names (Issue #151)

### 17.1.9
 - chore: Bump logback-classic to 1.5.21, SpotBugs to 6.4.7, commons-validator to 1.10.1, gradle to 9.2.1, google phonenumber to 9.0.19
 - chore: Switch examples to use non-deprecated method for registering plugins

### 17.1.8
 - fix: RegExp for list types incorrect if option 'words=[any|all|first' used
 - fix: RegExp returned for MunicipalityCodes in the Netherlands was wrong if empty strings supplied
 - chore: Bump google phonenumber to 9.0.18, gradle to 9.2.0, jackson to 2.20.1
 - chore: Some build.gradles changes to ready for gradle 10.0.0

### 17.1.7
 - feat: Add ability to edit built-in plugins (Issue #149) - see TestPlugins.pluginsEdit() for an example.

### 17.1.6
 - feat: Add support for registering Custom Plugins either higher than or lower than built-in plugins (Issue #148)
 - chore: Bump logback-classic to 1.5.20, SpotBugs to 6.4.4

### 17.1.5
 - chore: Bump google phonenumber to 9.0.16, logback-classic to 1.5.19, gradle to 9.1.0, SpotBugs to 6.4.3
 - fix: Fix a set of cases where the lengths frequencies did not correlate with the number of non-null samples seen.

### 17.1.4
 - fix: Semantic Date types were neither honoring priority nor considering header confidence (Issue #144)
 - chore: Bump SpotBugs to 6.4.2, google phonenumber to 9.0.14, guava to 33.5.0-jre

### 17.1.3
 - chore: Bump google phonenumber to 9.0.13, gradle to 9.0.0, commons-text to 1.14.0, jackson to 2.20.0, jakarta.mail to 2.0.2
 - fix: IDENTIFIER semantic type is detected even if DEFAULT_SEMANTIC_TYPES is disabled (Issue #144)

### 17.1.2
 - feat: Add support for \\p{Digit} as a synonym for \d in RegExp plugins (Issue #142)
 - feat: Validate the input file associated with a user-defined list plugin to check for lower case characters (Issue #143)
 - fix: RegExp for FREE_TEXT incorrect if newlines present
 - fix: RegExp for SPATIAL.WKT incorrect - missing '.'
 - fix: RegExp for STREET_ADDRESS_EN, FULL_ADDRESS_EN incorrect if newlines present
 - fix: RegExp incorrect if backing out from PhoneNumber misdetection and newlines present
 - fix: RegExp incorrect if Unicode alphabetic character present and outputting a set of values
 - fix: RegExp incorrect if returning a alphabetic character class with lower case e.g expected '(?i)[A-D]' for a, b, c, d not '[A-D]'
 - fix: RegExp incorrect if merging a simple numeric with a qualified numeric e.g. '\d' & '\d{2}\p{IsAlphabetic}{4}'
 - fix: RegExp for NAME.MIDDLE should allow periods
 - fix: RegExp for POSTAL_CODE.ZIP5_PLUS4_US incorrect if any Zips with fewer than 5 digits present
 - fix: VIN's outside the US do not have a check digit hence format is to easy to misinterpret so insist on a valid header
 - fix: Were not recognizing '?' as a special character for Regular Expression - hence they were not being sloshed
 - fix: RegExp for URLs not allowing parens
 - fix: If we change out mind and make it a Regular Expression semantic type then any outliers are actually invalids
 - chore: Fix a couple of warnings
 - chore: Add missing Indian Semantic Types to documentation
 - chore: Bump google phonenumber to 9.0.10, gradle to 8.14.3, commons-validator to 1.10 (fixes dependency on vulnerable commons-beanutils), spotbugs to 6.2.2

### 17.1.1
 - feat: Improve Indian District and State detection
 - chore: Migrate to the Central Publishing Portal (OSSRH Service is EOL)

### 17.1.0
 - feat: Add new Semantic Type - POSTAL_CODE.POSTAL_CODE_IN (Indian Postal Code) and STATE_PROVINCE.DISTRICT_NAME_IN (Indian District name)
 - fix: Fix name of STATE_PROVINCE.STATE_UNION_IN (was STATE_PROVINCE.STATE_IN)

### 17.0.1
 - chore: Point examples to 17.+
 - chore: Fix web example to use new CSV processor
 - chore: Bump spotbugs to 6.2.1

### 17.0.0
 - BREAKING CHANGE: registerPlugins signature changed now only throws IOException and FTAPluginException
 - feat: Add new Semantic Type - STATE_PROVINCE.STATE_UNION_NAME_IN (Indian State/Union name) and STATE_PROVINCE.STATE_IN (Indian State Code) (Issue #140)
 - feat: Add --withBOM to options to assert that a test file has a BOM
 - chore: Replace univocity parsers with fastcsv. (Issue #137)

### 16.2.13
 - feat: Improve sampleplugin example - to demonstrate loading from a file
 - chore: Bump jackson to 2.19.1, spotbugs to 6.2.0

### 16.2.12
 - feat: Improve sampleplugin example - to also demonstrate an RE plugin

### 16.2.11
 - chore: Fix SpotBugs issues

### 16.2.10
 - chore: Fix SpotBugs issues
 - chore: Bump google phonenumber to 9.0.7, gradle to 8.14.2

### 16.2.9
 - fix: Fix issues with sampleCount being incorrect - typically when doing a subsequent pass (Issue #134)

### 16.2.8
 - fix: Address issue where top and bottom are added to the cardinality set on merge, even if they match in a case insensitive fashion (Issue #130)

### 16.2.7
 - chore: Improve trace logging (save information during serialize even if no training has occurred)
 - chore: Bump google phonenumber to 9.0.6

### 16.2.6
 - fix: Not really an issue but don't add 0 entries to bulk map for recording
 - chore: Improve trace logging
 - chore: Bump springframework.boot to 3.5.0, gradle to 8.14.1

### 16.2.5
 - feat: Refresh US postal codes
 - fix: Trace files are not correct when Merging (Issue #130)
 - chore: Update Copyright

### 16.2.4
 - fix: Fix NPE when HistogramSPDT is serialized/deserialized (Issue #131)

### 16.2.3
 - feat: Improve performance of TextAnalyzer.merge()
 - fix: Fix NPE if we see something that looks like '0.9320E-'
 - chore: Add some setLocale() calls so that the examples run happily in any locale
 - chore: Bump guava to 33.4.8-jre, google phonenumber to 9.0.5, commons-text to 1.13.1, jackson to 2.19.0, gradle to 8.14

### 16.2.2
 - chore: Add test to check out bugs in TextAnalyzer.merge() (issues fixed in 16.2.1)

### 16.2.1
 - chore: Bump org.springframework.boot to 3.4.4, guava to 33.4.5-jre, google phonenumber to 9.0.2
 - fix: Add missing doco for IDENTITY.SIN_CA
 - fix: Fix two issues in TextAnalyzer.merge() - need to emptyCache() and fix issue if second argument had outliers/invalids not present in first
 - feat: CLI - Option description for --topBottomK was wrong (--maxTopBottomK)
 - feat: CLI - --testMerge now requires a numerical option to control how frequently to attempt merge


### 16.2.0
 - feat: Add ability to set the the number of top/bottom values tracked (Issue #127)

### 16.1.0
 - feat: Add ability to set the maximum number of shapes tracked (Issue #127)
 - chore: Bump guava to 33.4.5-jre

### 16.0.7
 - feat: Add new Semantic Type - IDENTITY.SIN_CA (Canadian - Social Insurance Number)
 - feat: Support --faker with inline specification (also fix incorrect example in README)
 - chore: Improve error message if calling Distributions entry points with DISTRIBUTIONS not enabled
 - chore: Add some more test cases for totalCount and merging
 - chore: Bump google phonenumber to 9.0.1, logback-classic to 1.5.18

### 16.0.6
 - feat: Change the default number of Histogram bins to 200 from 1000 (improves performance by ~3x for large inputs), to revert to prior behavior invoked setHistogramBins(1000) on the AnalysisConfig.  Note: If distributions (Quantiles/Histograms) are not required they can be disabled via configure() on the AnalysisConfig which will dramatically improve performance further.
 - fix: Honor minLength in FakerStringLT

### 16.0.5
 - feat: Add new Semantic Type - CRYPTOCURRENCY.TEXT_EN
 - feat: Faker - add String support to faker
 - chore: Bump jackson to 2.18.3

### 16.0.4
 - feat: Add new Semantic Type - FILENAME_EXT
 - feat: ~15% improvement in throughput if not calculating distributions (~3% if calculating distributions)
 - test: Fix flaky test case - extendedTotalsDates
 - chore: Bump google phonenumber to 9.0.0

### 16.0.3
 - feat: Faker - Switch to JSON definition, add String support to faker
 - feat: Improve the list of file extensions
 - feat: Minor performance improvements
 - fix: Fixed trivial bug in MergeSimple example
 - fix: Setting debug on the RecordAnalyzer template was not setting debug on the individual Analyzers
 - fix: Minor change to support building on Java 17
 - feat: Add memory diagnostics (debug level >= 3)
 - chore: Bump gradle to 8.13, logback-classic to 1.5.17

### 16.0.2
 - fix: Most total* (except totalCount) not surviving merge (and associated tests)
 - chore: Upgrade all examples to point to 16.+
 - chore: Bump org.springframework.boot to 3.4.3

### 16.0.1
 - feat: Improve documentation related to sampleCount and merging

### 16.0.0
 - BREAKING CHANGE: Changed name of Feature NULL_AS_TEXT to NULL_TEXT_AS_NULL
 - feat: Added mini-cache to train() entry - speeds up processing by ~4x if you have a large number of similar values (e.g. a boolean field with Y/N)
 - fix: NULL_TEXT_AS_NULL not being honored in trainBulk()
 - fix: Fix NPE in isValid() for some addresses
 - chore: Bump org.springframework.boot to 3.4.1, com.github.ben-manes.versions to 0.52.0, gradle to 8.12.1, google phonenumber to 8.13.55, testng to 7.11.0
 - feat: Enhance documentation as it relates to sampleCount and merging

### 15.12.0
 - feat: Add new Semantic Types - SPATIAL.WKT, SPATIAL.GEOJSON (#121)
 - chore: Bump logback-classic to 1.5.16, google phonenumber to 8.13.53
 - chore: Some minor cleaning
 - fix: If FORMAT_DETECTION is enabled - error messages were being printed to stdout

### 15.11.2
 - chore: Minor gradle cleanup
 - chore: Minor code cleanup

### 15.11.1
 - chore: Bump google phonenumber to 8.13.52, commons-text to 1.13.0, guava to 33.4.0-jre, logback-classic to 1.5.15, gradle to 8.12, org.springframework.boot to 3.4.1, io.spring.dependency-management to 1.1.7
 - chore: Some minor cleaning

### 15.11.0
 - feat: Add support for registering plugins on the RecordAnalyzer (via the template)
 - fix: Added missing source for DateDemo and Serialize examples
 - chore: Add ability to execute all examples (as well as build them) - task: examples.run

### 15.10.3
 - feat: Move around the examples, so they can be executed and are not shipped with the core libraries
 - chore: Bump jackson to 2.18.2, bump google phonenumber to 8.13.51

### 15.10.2
 - fix: Fix bug introduced in 15.9 which accidently removed isSemanticType field from Json (#120), also added test!

### 15.10.1
 - feat: Improve performance of TextAnalyzer.initialize() - especially significant if merge() is being called and causing lots of deserialization

### 15.10.0
 - feat: Improve performance of TextAnalyzer.merge (both memory usage and speed)
 - chore: Bump gradle to 8.11.1, org.springframework.boot to 3.4.0

### 15.9.0
 - feat: Remove support for TextAnalyzer.Feature.LEGACY_JSON
 - feat: cli option --json changed to --format json (default), added --format faker (to output a faker specification)
 - feat: Added Boolean, LocalTime, OffsetDateTime support to faker

### 15.8.0
 - chore: Bump gradle to 8.11, bump google phonenumber to 8.13.50
 - fix: Fix issue with very late changing of mind from LONG to DOUBLE, also new test case (Issue117), also improve error message (#117)

### 15.7.19
 - feat: Add an example to demonstrate merging two separate analyses

### 15.7.18
 - chore: Bump jackson to 2.18.1, bump google phonenumber to 8.13.48

### 15.7.17
 - chore: Bump logback-classic to 1.5.12, org.springframework.boot to 3.3.5

### 15.7.16
 - chore: Bump logback-classic to 1.5.11, bump google phonenumber to 8.13.48

### 15.7.15
 - chore: Bump google phonenumber to 8.13.46, jackson to 2.18.0

### 15.7.14
 - chore: Bump google phonenumber to 8.13.46, org.springframework.boot to 3.3.4, gradle to 8.10.2, guava to 33.3.1-jre

### 15.7.13
 - chore: Fix flaky test localeLongTest
 - chore: Bump org.springframework.boot to 3.3.3, google phonenumber to 8.13.45, logback-classic to 1.5.8, gradle to 8.10.1

### 15.7.12
 - chore: Bump gradle to 8.10, logback-classic to 1.5.7, google phonenumber to 8.13.43, guava to 33.3.0-jre

### 15.7.11
 - chore: Bump google phonenumber to 8.13.43, slf4j-api to 2.0.16

### 15.7.10
 - chore: Bump org.springframework.boot to 3.3.2, google phonenumber to 8.13.42

### 15.7.9
 - chore: Bump gradle to 8.9, google phonenumber to 8.13.41

### 15.7.8
 - chore: Bump jackson to 2.17.2
 - chore: Add new test (that fails) for a second first name (common in Spanish)

### 15.7.7
 - chore: Bump org.springframework.boot to 3.3.1
 - chore: Bump google phonenumber to 8.13.40

### 15.7.6
 - feat: Add --testmerge option to CLI to enable test suite to run each test using two analyzers and merging result
 - fix: Issue when merging where each shard has a different type (Issue #97)
 - fix: Issue when merging where each shard has the same date type but the formats differ (Issue #97)
 - fix: Issue when merging where each shard has the same numeric type but the modifiers differ (Issue #97)

### 15.7.5
 - fix: Fix --trailer option to the CLI
 - chore: Improve CLI help (--trailer, --bulk)

### 15.7.4
 - fix: Fix NPE if one element being merged has a null variance (Issue #95)
 - chore: Bump google phonenumber to 8.13.39

### 15.7.3
 - chore: Bump google phonenumber to 8.13.38, commons-validator to 1.9.0, org.springframework.boot to 3.3.0, guava to 33.2.1-jre
 - chore: Bump gradle to 8.8

### 15.7.2
 - chore: Remove some gradle 9.0 warnings
 - chore: Bump google phonenumber to 8.13.37, sketches-java to 0.8.3

### 15.7.1
 - feat: Improve documentation for getThreshold()/setThreshold()
 - chore: Bump io.spring.dependency-management to 1.1.5

### 15.7.0
 - fix: Double detection not honoring threshold (Issue #81)
 - fix: Double detection not handling exponentiation with a lower 'e' followed by a negative power
 - fix: Double detection not handling trailing minus when locale is de-DE
 - chore: Bump org.springframework.boot to 3.2.5, google phonenumber to 8.13.36, guava to 33.2.0-jre, jackson to 2.17.1

### 15.6.1
 - fix: Javadoc fix.

### 15.6.0
 - feat: Add support for String frequencies. See TextAnalysisResult.getLengthFrequencies(). (Issue #83)
 - chore: Bump google phonenumber to 8.13.35, logback-classic to 1.5.6, commons-text to 1.12.0, testng to 7.10.2

### 15.5.5
 - chore: Bump google phonenumber to 8.13.34, testng to 7.10.1, logback-classic to 1.5.5, slf4j-api to 2.0.13

### 15.5.4
 - test: Minor changes to test suite to support Java 22
 - chore: Bump google phonenumber to 8.13.33, org.springframework.boot to 3.2.4
 - chore: Bump gradle to 8.7

### 15.5.3
 - feat: Speed up serialization() (113μs -> 8μs = 14x) and deserialization() (73μs -> 8μs = 9x) of DateTimeParser().
 - chore: Bump CodeQL up to v3

### 15.5.2
 - feat: Speed up serialization() (662μs -> 46μs = 15x) and deserialization() (2562μs -> 1729μs) of TextAnalyzer().

### 15.5.1
 - chore: Upgrade spring boot in examples
 - chore: Bump logback-classic to 1.5.3, google phonenumber to 8.13.32, jackson to 2.17.0, guava to 33.1.0-jre
 - test: Improve test coverage

### 15.5.0
 - fix: OutOfMemoryError: Requested array size exceeds VM limit exception (Issue #70)
 - fix: getMinLength() was never returning 0
 - feat: Improve detection of non-localized numbers
 - chore: Bump google phonenumber to 8.13.31
 - chore: Bump copyright year and fix a few missing headers

### 15.4.3
 - chore: Bump google phonenumber to 8.13.30, logback-classic to 1.5.0

### 15.4.2
 - chore: Bump gradle to 8.6, slf4j-api to 2.0.12, google phonenumber to 8.13.29

### 15.4.1
 - feat: I18N - Add FREE_TEXT detection for a set of Langauages (hr, sv, tr)
 - feat: I18N - Add GENDER and Yes/No detection for Icelandic
 - docs: Fix SemanticTypes doc to reflect additional languages
 - chore: Bump testng to 7.9, slf4j-api to 2.0.10, google phonenumber

### 15.4.0
 - feat: I18N - Add FREE_TEXT detection for a set of Langauages (da, fi, ga, hu, lv, ro, sk) and improved sample generation for all languages
 - feat: Bump guava and Jackson

### 15.3.0
 - feat: Bump google phonenumber and commons-validator
 - feat: Added 1997 codes to NAICS detection to improve detection
 - feat: I18N - Add new Semantic Types - COLOR.TEXT_ES (Spanish Color)
 - feat: I18N - Add new Semantic Types - GENDER.TEXT_TR (Turkish Gender)
 - feat: I18N - Add French names for Canadian provinces

### 15.2.1
 - feat: Bump logback-classic and google phonenumber
 - feat: Improve JavaDoc
 - chore: Bump gradle to 8.5
 - chore: Update example web dependencies
 - chore: Minor cleanups

### 15.2.0
 - feat: Improve GENDER detection
 - fix: Fix issue when encountering 10 digits in the nanoseconds field (Issue #55)

### 15.1.10
 - feat: Improve nextRandom() for PERIOD.QUARTER
 - feat: Improve regExp for COORDINATE.LATITUDE_DMS and COORDINATE.LONGITUDE_DMS
 - feat: Bump jackson and google phonenumber
 - chore: Work on code coverage

### 15.1.9
 - fix: Fix for Polish VAT number generation
 - fix: Fix for RegExp for UK VAT numbers
 - chore: Work on code coverage

### 15.1.8
 - feat: Small improvement to DAY.DIGITS/MONTH.DIGITS detection
 - fix: RegExp for STATE_PROVINCE.MUNICIPALITY_CODE_NL was incorrect
 - chore: Work on code coverage

### 15.1.7
 - feat: Bump commons-text
 - feat: Checkin gradle wrapper files
 - chore: Remove useless minLength/maxLength/generalPattern on TypeInfo
 - chore: Automatically publish to maven when release is created
 - chore: Work on code coverage

### 15.1.6
 - feat: Bump google phonenumber
 - chore: Work on code coverage

### 15.1.5
 - fix: Examples should be dependent on 15.+ of FTA not 14.+
 - fix: equals() on Facts should have been <= epsilon not < epsilon (matters if epsilon is 0.0)
 - fix: Make TestDistribution.normalCurve() more robust by using equals as opposed to comparing serialized versions strings
 - chore: Work on code coverage
 - chore: Bump jacoco

### 15.1.4
 - feat: Minor changes to support Java 17
 - fix: Given monotonic increasing input (which looks like a ZIP) then backing out incorrectly set monotonicIncreasing to false
 - chore: Make the code and tests more resilient to a Locale with only a language (i.e. no Country)

### 15.1.3
 - feat: Minor improvements to TELEPHONE plugin (be more picky to reduce false positives) and accept Contact?Number as a positive header (to detect more valid items)
 - fix: Examples should be dependent on 15.+ of FTA not 14.+

### 15.1.2
 - feat: Bump google phonenumber, and guava, jackson-databind
 - feat: Improve mapping to schema.org Semantic Types
 - feat: Minor improvements to NAME.LAST_FIRST and NAME.FIRST_LAST
 - chore: Bump gradle to 8.4

### 15.1.1
 - feat: Bump google phonenumber
 - fix: GitTables - correctly reconstruct format string with single quote in it - e.g. "dd MMM'' yy"
 - fix: GitTables - do not error out if receive time of the form 12:35:43.
 - fix: GitTables - fix misdetection of "ATACCTAGCACACAGATCCCTCTCCAATGCATGAAAGTGA" as HASH.SHA1_HEX
 - fix: GitTables - fix StringIndexOutOfBoundsException in SimpleDateMatcher
 - fix: GitTables - fix issues with input of the form '8E5-' or '451E.'
 - fix: Fix getMaxInvalids()

### 15.1.0
 - feat: Add new Semantic Type - IDENTIFIER (Unique Identifier)
 - feat: Bump slf4jAPI, google phonenumber
 - fix: Add missing file - br_municipalities_code.csv
 - fix: Nested analysis was not using new copy of the AnalysisConfig
 - fix: Don't do nested analysis by removing outliers if Max Cardinality blown

### 15.0.1
 - feat: Bump google phonenumber
 - feat: Add new Semantic Type - STREET_ADDRESS4_EN (Fourth line of an address) (Issue #49)
 - feat: Improve US address detection (Issue #49)

### 15.0.0
 - BREAKING CHANGE: PluginDefinition.findByQualifier() has changed to PluginDefinition.findByName()
 - feat: Bump logback-classic, google phonenumber, and guava
 - feat: Minor improvements to NAME.FIRST, URI.URL, FILENAME detection
 - feat: Improve detection for list-based Semantic Types with small numbers of records
 - fix: Fix up documentation to align with qualifier to semantic type switch (Issue #47)
 - chore: Bump gradle to 8.3

### 14.7.2
 - fix: Numerous performance improvements, mostly impacting RecordAnalyzer with large number of columns (Issue #46)

### 14.7.1
 - fix: Missed a file on checkin
 - chore: Bump jacoco

### 14.7.0
 - feat: I18N - Add new Semantic Types - STATE_PROVINCE.MUNICIPALITY_CODE_BR (Brazilian Municipality code)
 - feat: I18N - Improve detection of non-localized doubles
 - feat: Improve detection of 'yyyy'
 - feat: Improve POSTAL_CODE.ZIP5_US & POSTAL_CODE.ZIP5_PLUS4_US detection
 - feat: Bump logback-classic, google phonenumber, and guava
 - feat: Significantly improve detection rate of Date Types - both precision and recall (especially non-US)
 - fix: Handle yyyy-MMM *and* noAbbreviationPunctuation - e.g. 1954-JUN with locale en-CA
 - fix: Handle "2023-02-03  09:56:22" - i.e. multiple spaces between the date and the time
 - fix: Fix incorrect result for "Sep  6 2018  8:43AM" - correct result is "MMM ppd yyyy pph:mma"
 - fix: Fix date format detection for a set of cases with bogus data (commonly resulting in k instead of H)
 - chore: Bump gradle to 8.2.1
 - chore: Automatically generate the documentation (SemanticTypes.md) for the list of Semantic Types detected

### 14.6.1
 - fix: RegExpSplitter.newInstance(String) Incorrectly Parses Ranges with Multiple Digits in the Max (Issue #44)

### 14.6.0
 - feat: I18N - Add new Semantic Types - STATE_PROVINCE.REGION_NAME_PE (Peruvian Region/Department) + STATE_PROVINCE.PROVINCE_NAME_PE (Peruvian Province) + STATE_PROVINCE.REGION_NAME_TZ (Tanzanian Region)
 - feat: Add new Semantic Types - STATE_PROVINCE.STATE_FIPS_US (US State FIPS code) + STATE_PROVINCE.COUNTY_FIPS_US (US County FIPS code)
 - feat: I18N - Add "HEMBRA", "VARÓN" for Gender detection in Spanish
 - feat: I18N - Add withEnglishAMPM() to DateTimeParser and TextAnalyzer.Feature.ALLOW_ENGLISH_AMPM to allow recognition of "AM" and "PM" independent of the locale
 - feat: I18N - Improve recognition of STATE_PROVINCE.REGION_NAME_FR
 - feat: I18N - Improve recognition of STATE_PROVINCE.STATE_BR
 - feat: Bump jackson, google phonenumber and guava
 - fix: I18N - Fix header regexp for Age in french (improves PERSON.AGE and PERSON.AGE_RANGE detection)
 - fix: I18N - Change STATE_PROVINCE.REGION_IT to STATE_PROVINCE.REGION_NAME_IT (and hence signature)

### 14.5.0
 - feat: Add new Semantic Type - FILENAME (Name of file)

### 14.4.0
 - feat: Improve TextAnalysisResult.asPlugin() to support returning the plugin definition for known Semantic Types
 - chore: upgrade version of TestNG
 - feat: CLI - attempt to detect if --skip should be utilized

### 14.3.3
 - feat: Improve ModeBulk example to demonstrate plugin retrieval for a Semantic Type (#42)

### 14.3.2
 - fix: Add missing file from checkin

### 14.3.1
 - fix: Fix issue with late switches from DOUBLE_GROUPING to SIGNED_DOUBLE_GROUPING
 - fix: Fix issue with Switching from SIGNED_DOUBLE_TRAILING to SIGNED_DOUBLE_TRAILING_GROUPING
 - fix: Fix issue with 0000 as a year (format: yyyy)
 - feat: Bump jackson
 - feat: Bump google phonenumber
 - feat: CLI - Defend against null option files
 - feat: CLI - Continue processing even if one of the files provided has fatal errors

### 14.3.0
 - feat: I18N - Improve support for COORDINATE.LATITUDE_DECIMAL and COORDINATE.LONGITUDE_DECIMAL (nl-NL)
 - feat: Fix up test suite to support compiling with Java 17
 - feat: CLI - Add support for --trailer to ignore the last <n> lines
 - fix: Fix a couple of cases of incorrect qualifiers (SIGNED and GROUPING)

### 14.2.1
 - fix: Fix a small number issues discovered running entire Viznet suite

### 14.2.0
 - feat: Support 'yyyy/MM/dd HH' and friends in the DateTimeParser (and hence TextAnalyzer) (#36)

### 14.1.0
 - feat: Flip EPOCH.SECONDS and EPOCH.MILLISECONDS from RegEx to Java - improves detection (#40)
 - fix: Fix --pluginDetection to generate valid plugins from Training data (also associated test case)
 - fix: Fix poor validation of Java plugin at registration time (#41)

### 14.0.0
 - BREAKING CHANGE: Format of plugins has changed - content is now true JSON as opposed to a String
 - fix: Fix --semanticType being ignored when using RecordAnalyzer

### 13.7.0
 - feat: I18N - Add new Semantic Type - IDENTITY.NPI_US (National Provider Identifier (US))
 - feat: DateTimeParser: Add support for date detection of the form yyyyMM and yyyyMMddHH (#39)
 - feat: Improve detection of timestamps (minor)

### 13.6.2
 - feat: Add support for period detection of the form yyyyMM (#38)
 - feat: I18N - yyyyMMdd detection (now look for localized "date" header)
 - feat: Improve robustness of command line processing

### 13.6.1
 - feat: I18N - minor improvement to STREET_NAME_BARE_NL detection
 - feat: Remove deprecated DateTimeParser constructors
 - fix: Fix regExp returned with Doubles that are actually LocalDates (also add test case)
 - feat: Improve ModeStreaming example to report if the Semantic Type is not detected due to non-supported locale (#35)
 - chore: Some code cleanups

### 13.6.0
 - fix: EPOCH.MILLISECONDS was actually EPOCH.SECONDS, EPOCH.NANOSECONDS was actually EPOCH.MILLISECONDS (#37)

### 13.5.1
 - feat: Improve PERSON.DATE_OF_BIRTH detection allow LocalDateTime (also es,nl support)
 - chore: Bump gradle to 8.1.1

### 13.5.0
 - feat: Add general support for Semantic Types with Date/DateTime types
 - feat: Add new Semantic Types - PERSON.YEAR_OF_BIRTH and PERSON.DATE_OF_BIRTH
 - feat: Minor improvements to STREET_NUMBER detection
 - feat: Bump google phonenumber
 - chore: Minor debugging improvements

### 13.4.1
 - feat: Bump logback-classic
 - feat: Bulk mode should prioritize 'interesting' values in preference to null or blanks (#33)
 - feat: Bump jackson
 - chore: Update copyright
 - chore: Some code cleanups

### 13.4.0
 - feat: I18N - Add new Semantic Type - IDENTITY.NI_UK (National Insurance Number (UK))
 - feat: I18N - Add new Semantic Type - IDENTITY.PERSONNUMMER_SE (Personal identity number (Sweden))
 - feat: Add new Semantic Type - IMEI (15 digit CHECKDIGIT.LUHN with header signal)
 - feat: Bump google phonenumber
 - chore: Some code cleanups

### 13.3.0
 - fix: ISO 639-1 incorrectly switched with ISO 639-2
 - feat: Add Documentation tag to plugins file to document the Semantic Type (also display in Web)

### 13.2.0
 - feat: Improve DIRECTION detection - Support InterCardinal Full names (NORTHEAST|NORTHWEST|SOUTHEAST|SOUTHWEST) (Recall - 93% -> 97%)
 - feat: Add new Semantic Type - INDUSTRY_CODE.NAICS

### 13.1.1
 - fix: Add missing file

### 13.1.0
 - feat: Detect longs masquerading as doubles
 - feat: Improve DIRECTION detection - NB/SB/EB/WB (and friends) (Recall - 66% -> 93%)
 - feat: Improve AGE, MONTH.DIGITS, ZIP+4 detection

### 13.0.3
 - feat: If we have a Long type and no Semantic Type detected, exclude outliers (using density-based clustering) and re-analyze
 - feat: ISO 3166-2 insist on the presence of some signal from the header
 - feat: Improve PERSON.AGE detection
 - feat: Bump google phonenumber

### 13.0.2
 - feat: Improve Web UI
 - feat: Bump google phonenumber
 - feat: Bump slf4jAPI
 - fix: Fix command line invocations in the README

### 13.0.1
 - feat: Improve performance of STREET_NUMBER (Recall - 82% -> 97%)

### 13.0.0
 - BREAKING CHANGE: Library is now targetting Java 11+, use 12.X if you still require Java 8 support
 - feat: Bump logback-classic
 - feat: Support associated .options file directly (speeds up reference test run x3)

### 12.10.3
 - feat: Improve PERSON.AGE, PERSON.AGE_RANGE, ADDRESS_FULL_EN, STREET_ADDRESS_EN detection
 - chore: Reimplement WordProcessor()
 - chore: Bump gradle to 8.0.2

### 12.10.2
 - feat: Improve Gender(en), Race(en), and Age Range detection
 - feat: Improve CompanyName (en) detection

### 12.10.1
 - fix: Introduced issue with 12.10.0  - null not recognized when NULL_AS_TEXT set to false

### 12.10.0
 - feat: Add new Semantic Type - PERSON.AGE_RANGE (en, es, fr, it, nl, pt)
 - feat: Improve GUID detection
 - feat: I18N - added wijk, wijknaam, buurt, buurtnaam as synonyms for CITY (nl)

### 12.9.4
 - feat: Add simple web interface
 - chore: Update build to include examples (via composite build)

### 12.9.3
 - feat: Add support for en-NL (along with existing support for nl-NL)
 - feat: If the header looks good then do one more pass with the worst invalid removed (significant uptick in detection - ~1%)
 - feat: Move getDefaultAnalysis from DriverUtils to TextAnalyzer (now a supported interface)
 - feat: I18N - minor tweaks to Colombian Municipalities, airlines(en), titles(en), colors(en), countries(en), countries(nl)
 - feat: Bump google phonenumber

### 12.9.2
 - fix: Test Suite does not run cleanly when default locale is other than en-US (Issue #28)

### 12.9.1
 - feat: Extend validation examples and allow case independent matching in PluginDefinition.findByQualifier()
 - feat: I18N - Dutch - Add FRYSLÂN to the list of valid provinces
 - feat: I18N - Colombian - Add a set of municipalities without diacritical marks
 - feat: Improve STREET_MARKER_EN detection

### 12.9.0
 - chore: Improve count validation
 - feat: Improve STREET_NAME_EN detection
 - feat: Allow for hyphens in Aadhaar (also fix spelling)
 - fix: Mismatched counts - Long Semantic Type detected incorrectly and backing out to Long
 - fix: Mismatched counts - Semantic Type detected and Outliers present and overlapping in case

### 12.8.4
 - feat: Improve NAME.FIRST_LAST detection
 - feat: Add count validation to CLI (and use it to validate code against semantic-types suite)
 - fix: Issue with Signed Double with grouping and exponent and invoking getResult() multiple times
 - fix: Mismatched counts - Enums with outliers
 - fix: Mismatched counts - RegExp Semantic Types with invalid entries
 - fix: Mismatched counts - LocalDate(yyyy) with "0" entries
 - fix: Mismatched counts - Long Semantic Type detected incorrecly and backing out to Double
 - chore: Bump gradle to 8.0

### 12.8.3
 - fix: MinLength wrong when only have nulls and empty strings
 - fix: Blank counts wrong when using RecordAnalyzer with prior Semantic Information
 - fix: Mismatched counts (matchCount) for Semantic Types (Issue 25)

### 12.8.2
 - fix: Blank counts wrong when re-analyzing (Issue #24)

### 12.8.1
 - fix: Counts do not match for some Finite types - including GENDER.TEXT_EN (Issue #23)

### 12.8.0
 - feat: I18N - Dutch - Add PERIOD.QUARTER support
 - feat: I18N - Dutch - Improve PostCode detection, Province detection, Municipality Code detection
 - feat: Date detection now catches 'MMMM, yyyy'
 - feat: Impove LocalDate(yyyy) detection
 - feat: CLI - Add ability to set quote char from CLI

### 12.7.1
 - feat: Bump google phonenumber
 - feat: Make examples depend on the latest major version
 - fix: Add missing Java file for MUNICIPALITY_CODE_NL

### 12.7.0
 - feat: I18N - Add new Semantic Type - STATE_PROVINCE.MUNICIPALITY_CODE_NL - Dutch Municipality Code
 - feat: I18N - Improve Gender detection in Dutch - now also match 'VROUW' and 'MAN'
 - feat: I18N - Improve Nationality, Company Name, and City detection in Dutch
 - feat: I18N - Improve Department detection in Colombian
 - feat: For a finite type with a good header, attempt to analyze removing the worst invalid entry (hoping to remove N/A, Not Present, All, etc)
 - feat: Improve mini CLI - now supports --locale, --verbose
 - feat: Allow longtitude as a mispelling of longitude (occurs 4 times in VizNet examples)

### 12.6.5
 - feat: Restructure some examples to be stand-alone
 - feat: Add simple CLI example
 - feat: Bump versions

### 12.6.4
 - feat: I18N - Add new Semantic Type - COLOR.TEXT_NL (Dutch)
 - feat: I18N - Detect COMPANY_NAME in Dutch
 - feat: Bump Jackson

### 12.6.3
 - feat: I18N - Add new Semantic Type - IDENTITY.VAT_NL (Dutch)
 - feat: I18N - Add new Semantic Type - IDENTITY.BSN_NL (Burger Service Nummer)
 - feat: I18N - Improve detection of DAY.DIGITS and LocalDate(yyyy) in Spanish
 - feat: I18N - Add 'ciudad' to detect CITY in Spanish

### 12.6.2
 - fix: Fix typo in JSON file - introduced in 12.6.1

### 12.6.1
 - feat: I18N - Detect PERSON.AGE in Dutch
 - feat: I18N - Detect COUNTRY.ISO-3166-2, COUNTRY.ISO-3166-3 in Dutch
 - feat: I18N - Detect MONTH.DIGITS/DAY.DIGITS in Dutch & Spanish
 - feat: I18N - Add new Semantic Type - COUNTRY.TEXT_NL (akin to COUNTRY.TEXT_EN)

### 12.6.0
 - feat: I18N - Add new Semantic Type - STATE_PROVINCE.MUNICIPALITY_NL - Dutch Municipality
 - feat: I18N - Add new Semantic Type - STREET_NAME_BARE_NL (akin to STREET_NAME_BARE_EN)
 - feat: I18N - Add new Semantic Type - NATIONALITY_NL (akin to NATIONALITY_EN)
 - feat: I18N - Add Telefoon as a TELEPHONE header for Dutch
 - feat: I18N - Add a set of the most common Dutch first names
 - feat: I18N - Add 'plaats/woonplaats' to detect CITY in Dutch
 - feat: I18N - Improve FREE_TEXT detection in Dutch
 - feat: I18N - Detect STREET_NUMBER in Dutch

### 12.5.1
 - feat: Improve detection of STREET_NUMBER and STREET_ADDRESS_BARE_EN
 - feat: For RecordAnalyzer loop if any new Semantic Type detected (hopefully we pick up others on a subsequent pass)
 - feat: Bump google phone number library

### 12.5.0
 - feat: Change plugin interface to enable counting of detection entries (and use to improve Last Name detection, also add tests)
 - feat: Improve set of recognized Street Markers

### 12.4.1
 - feat: If using linear (not bulk) then if we have not detected a Semantic Type try replaying accumulated set in Bulk mode this has the potential to pick up entries where the first <n> (by default 20) are misleading.
 - feat: Bump google phone number library

### 12.4.0
 - feat: Add new Semantic Type - STREET_ADDRESS3_EN (Third line of an address)
 - feat: Continue to improve address detection

### 12.3.3
 - feat: Bump google phone number library

### 12.3.2
 - fix: Stop double-barreled last names with spaces from preempting NAME.FIRST_LAST

### 12.3.1
 - chore: Add example for Record Mode to README and examples (ModeRecord, cf. ModeStreaming, ModeBulk)

### 12.3.0
 - feat: Change return value of RecordAnalyzer

### 12.2.2
 - feat: Reimplement CITY as a Java plugin (F1-Score from 97% to >99%)

### 12.2.1
 - feat: Bump google phone number library
 - feat: Bump slf4j
 - feat: Change getHeaderConfidence to allow negative Header indication (i.e. < 0)

### 12.2.0
 - feat: Improve Last Name detection

### 12.2.0
 - feat: Improve RecordAnalyzer interface.
 - feat: Add new Semantic Type - STREET_NUMBER (Street Number).
 - feat: Improved Name detection (NAME.*)
 - feat: Add the ability to specify known Semantic Types as part of the supplied Context (see withSemanticTypes)
 - fix: Weekday abbreviations should also honor the NO_ABBREVIATION_PUNCTUATION feature (impacted locale CA)

### 12.1.1
 - feat: Initial version of the new RecordAnalyzer interface (and use it from the CLI)

### 12.1.0
 - feat: Add new Semantic Type - STREET_NAME_BARE_EN (Street Name without a Marker (e.g. no ST, RD, LN, ...))
 - feat: More Address detection improvements
 - feat: Bump google phone number library
 - feat: Bump slf4j
 - chore: Bump gradle to 7.6

### 12.0.9
 - feat: Revamp Address detection - now split into FULL_ADDRESS_EN, STREET_ADDRESS_EN, STREET_ADDRESS2_EN, STREET_NAME_EN
 - feat: Content Format Detection - accept JSON that uses single quotes as opposed to the standard
 - feat: Bump Jackson & slf4j
 - fix: STREET_ADDRESS_EN - should trim() before checking length

### 12.0.8
 - feat: Improve NAME.LAST_FIRST to cope with multiple spaces - e.g., "DAVIS,  RICHARD M"
 - feat: Bump google phone number library
 - feat: Bump Jackson

### 12.0.7
 - feat: Default NULL_AS_TEXT to off. Note: by default the CLI enables this.
 - feat: Improve detection for dates of the form MMM&lt;sep&gt;YYYY or MMMM&lt;sep&gt;YYYY

### 12.0.6
 - feat: Improve NAME.MIDDLE_INITIAL recall

### 12.0.5
 - chore: Remove support for Rule generation (moved to separate utility)

### 12.0.4
 - feat: Improve NON_LOCALIZED Double detection
 - feat: Improve NAME.FIRST_LAST to cope with multiple spaces - e.g., "Rodney D.  Jones"
 - feat: Support Date formats like "April,2015"

### 12.0.3
 - fix: Missed a file on checkin

### 12.0.2
 - feat: Reimplement INDUSTRY_EN as a Java plugin (Recall now at 90% against Suite, previously at 5%)
 - feat: Add support for ignoreList so we can ignore things like 'OTHER' and 'N/A' on lists
 - feat: Add Correlation data from Test Suite to inform likelihood of a semantic match
 - feat: PERSON.AGE plugin should support DOUBLE as well as LONG
 - feat: Improve COMPANY_NAME/COLOR detection
 - fix: Fix up backing out from Semantic Types that have a base type of Double

### 12.0.1
 - fix: Update example in README

### 12.0.0
 - BREAKING CHANGE: Incompatible changes.
	isLogicalType() -> isSemanticType()
	getTypeQualifier() has split into getTypeModifier() and getSemanticType().  getSemanticType() is only valid if isSemanticType() is true.
	getTypeModifier() describes modification to the Base Type (for example SIGNED on base type LONG or DOUBLE, or YYYY-MM-dd for LOCALDATE)
	JSON output has also changed accordingly (Use analyzer.configure(TextAnalyzer.Feature.LEGACY_JSON, true) to revert to legacy JSON - pre 12.X)
 - feat: Bump google phone number library
 - feat: Add new Semantic Type - DIRECTION (Cardinal Direction)
 - test: Improved test coverage on Histogram/Quantile support
 - fix: Should preserve uniqueness (at least to the extent we can) on merge()
 - fix: Numerous fixes for Histograms and Quartiles (especially around ugly data - e.g. trailing minus), also improve performance
 - fix: Add UTF8 encoding option - just in case anyone builds on Windows

### 11.0.7
 - feat: Add cutpoints on Histogram entries returned (as well as the BaseType cuts)
 - fix: Various fixes to Histogram details
 - fix: Move 0's in date from outliers to invalid (also fix matchCount)
 - test: Much improved test coverage on Histogram support

### 11.0.6
 - feat: Output histograms (10 wide) in JSON
 - feat: Add 'faker' support - useful for testing
 - feat: Add meaningful samples for COMPANY_NAME
 - feat: CLI - Switch to return null if we see no data in CSV
 - fix: Make sure to clamp values in LocalTime and OffsetDateTime since quantiles are only so accurate and we need to return a valid value

### 11.0.5
 - fix: Fix Exception related to quantile determination when using LocalTime
 - fix: Fix histogram bucketing (and support histograms once cardinality blown)

### 11.0.4
 - feat: Change interface for getCardinalityDetails() from SortedMap to NavigableMap
 - feat: Add support for histograms. See getHistogram().

### 11.0.3
 - feat: Performance is slow when using trainBulk() and the counts are large and statistics are being generated

### 11.0.2
 - feat: Change cardinalityDetails to return a SortedMap (Issue #19)

### 11.0.0
 - BREAKING CHANGE: Behavior has changed with 11.0.0  - there is now a distinction between outliers and invalid entries.
      For example, with 1, 2, 3, 7, 8, 12, 9, 2, 23, BOGUS, 14 - 'BOGUS' is now an Invalid entry as opposed to an Outlier.
      See getInvalidCount() and getInvalidDetails().

	  **Note: Data Signatures have changed.**
 - feat: Bump logback-classic

### 10.3.0
 - feat: I18N - Add a few more Colombian Municipalities to improve detection
 - feat: Add support for retrieving the path to the Trace file (Issue #17)
 - feat: Bump commons-text, slf4j-api
 - fix: Serialization now works with Quantiles
 - fix: Some doubles were not capturing min/max/topK/bottomK (if logical type detected)
 - chore: Run tests in parallel (also speed up date tests - or more accurately only do 10% of locales each run)

### 10.2.1
 - feat: Add new Semantic Type - PERSON.RACE_ABBR_EN

### 10.2.0
 - feat: Add support for quantile determination. See getValueAtQuantile, getValuesAtQuantiles, and get/setQuantileRelativeAccuracy().
 - feat: CLI - Add --json to output true JSON from command line
 - feat: Bump google phone number library
 - chore: Remove a set of previously deprecated methods

### 10.1.0
 - feat: Add new Semantic Type - PERSON.MARITAL_STATUS_EN

### 10.0.1
 - feat: Improve PERSON.RACE_EN detection
 - feat: Improve COLOR.TEXT_EN detection
 - feat: Reimplement POSTAL_CODE.POSTAL_CODE_CA as a Java plugin (Recall now at 100% against Suite)

### 10.0.0
 - BREAKING CHANGE: New Feature.NULL_AS_TEXT is enabled by default ("Null" (also No Data) - will be treated as a NULL record)
 - fix: Changed PERSON.RACE to PERSON.RACE_EN (not backward compatible)
 - chore: Plugin definition has changed with 10.0 (getConfidence() now receives the full context, not just the StreamName)
 - feat: Add new Semantic Type - LANGUAGE.ISO-639-1 - three letter country code
 - feat: Add new Semantic Type - NAME.SUFFIX - Name Suffix (e.g. I, II, JR., ...)
 - feat: Add new Semantic Type - COLOR.TEXT_EN - Color Name
 - feat: Reimplemnent EIN as a Java plugin
 - feat: I18N - Add new Semantic Type - STATE_PROVINCE.SUBURB_AU - Australian Suburb (generalize BloomFilter support)
 - feat: I18N - Add new Semantic Type - STATE_PROVINCE.MUNICIPALITY_CO - Colombian Municipality
 - feat: I18N - Add new Semantic Type - STATE_PROVINCE.DEPARTMENT_CO - Colombian Department
 - feat: I18N - Add new Semantic Type - POSTAL_CODE.POSTAL_CODE_CO - Colombian Postal Code
 - feat: I18N - Add new Semantic Type - COUNTRY.TEXT_ES - Country (Spanish)
 - feat: Add a wrapper task to indicate the version of Gradle required (Issue #11)
 - feat: Add ability to create Normalized file to capture both words with and without diacritical marks
 - feat: Improve detection of DAY.DIGITS and MONTH.DIGITS (implemented in Java as opposed to regexp)
 - feat: Improve MiddleName/MiddleInitial detection
 - feat: Improve Street Address detection
 - feat: Improve Company Name detection (business/organization are now synonyms)
 - feat: Improve Person Age detection
 - feat: Improve Person Race detection
 - feat: Improve Person Gender detection
 - feat: Improve english Country detection
 - feat: Improve NAME.SUFFIX detection
 - feat: Add support for pluginOptions
 - feat: Bump slf4j (2.0.1) and logback-classic (1.4.1)
 - feat: Bump google phone number library
 - feat: Bump jackson
 - fix: Should have cleansed and trimmed input before checking it in isValid()
 - fix: Samples returned by VAT routines were typically not Valid
 - fix: URL plugin was not trim()'ing input
 - fix: Fix bug in Address2 if stream name not found in list of all field names

### 9.1.1
 - fix: Don't output totalNullCount, totalBlankCount, totalMinLength, totalMaxLength if they are unset

### 9.1.0
 - chore: No change - other than bumping the version number.

### 9.0.21
 - feat: if totalCount is set (i.e. != -1) then output the total* fields.

### 9.0.20
 - feat: Add support for Totals (i.e. the ability to set BlankCount, NullCount, Min/Max Value, Min/Max Length, Mean/SD for the entire set)

### 9.0.19
 - fix: fta-core should have been declared as an API (not implementation) dependency of fta

### 9.0.18
 - fix: DateTimeParser with DateResolutionMode.Auto works as MonthFirst for all locales (Issue #10)
 - feat: Minor improvements to Name detection
 - feat: Bump google phonenumber

### 9.0.17
 - feat: Add new Semantic Types - PERIOD.YEAR_RANGE, AIRLINE.IATA_CODE, AIRLINE.TEXT_EN
 - feat: Re-implement PERIOD.QUARTER in Java (was RegExp) to improve Recall (Sensitivity)
 - feat: Cleanse strings by replacing left/right quotes by ' to improve list matching
 - feat: I18N - plz is a synonym for postleitzahl in Germany
 - feat: I18N - Improve non-English detection of CITY
 - feat: I18N - Improved Precision of some non-English Postcode detection
 - fix: Pattern for YES_NO was [0|1] should have been (0|1)

### 9.0.16
 - feat: I18N - Japanese - add Prefecture names without 'Prefecture' to list
 - feat: I18N - Mexico - add State names without diacritic marks
 - feat: I18N - Mexico - add locale es-MX for Mexican State names
 - feat: I18N - Add new Semantic Type - STATE_PROVINCE.MUNICIPALITY_MX (Mexican Municipality)
 - fix: Add a set of missing signatures

### 9.0.15
 - feat: Support YR as a synonym for Year
 - feat: Improve US County detection
 - feat: Improve City detection (fix a set of false positives)
 - feat: I18N - Add support for STREET_ADDRESS for bg, ca, da, de, es, fi, fr, hr, it, lv, nl, pl, pt, ro, ru, sk

### 9.0.14
 - feat: I18N - Add new Semantic Types - STATE_PROVINCE.COUNTY_IE, STATE_PROVINCE.PROVINCE_NAME_IE
 - feat: Bump google phonenumber
 - chore: Bump gradle to 7.5

### 9.0.13
 - feat: Date processing - add detection for numeric only detection of dates (e.g. 2022, 20220712, 202207121830, 20220712183000) - default on, disable via withNumericMode(false)

### 9.0.12
 - feat: Address Issue #7 - Allow setting secondary (actually an infinite number) locale for DateTimeParser determination
 - feat: Support for 9 digit Zip + 4's

### 9.0.11
 - feat: I18N - zip is a synonym for postleitzahl in Germany
 - feat: Improve US County detection by adding some common misspellings
 - feat: CLI - field names should be trimmed
 - fix: CLI - if delimiter is specified, need to turn off autodetection

### 9.0.10
 - feat: Add new Semantic Type - PERSON.RACE

### 9.0.9
 - feat: I18N - Add FREE_TEXT support for Bulgarian, Catalan, Dutch, Portuguese, and Russian
 - feat: I18N - Improve Japanese date detection
 - feat: I18N - Improve Bulgarian date support - in particular support "14.02.2017г."
 - feat: I18N - Add new Semantic Types - STATE_PROVINCE.INSEE_CODE_FR ("French Insee Code (5 digit)")
 - feat: CLI - support --skip <n> to skip the first <n> lines
 - fix: Handle dates of the form - "1995-02-28Z", will return "yyyy-MM-dd'Z'" (which can be used with LocalDate.parse) - used to return "yyyy-MM-ddZ"
 - fix: Fix IDENTITY.VAT_GB - mixup with UK vs GB (locale is en-GB)

### 9.0.8
 - feat: CLI - set totalCount in Bulk mode
 - feat: I18N - Add FREE_TEXT support for Spanish and Italian
 - fix: I18N - Chinese detection should be enabled for language 'zh' (not 'cn')
 - fix: I18N - Date (Pass 2) was not being detected for 2015/9/9 (e.g. single digit day), picked up in Pass 3 for anything other than Japanese/Chinese

### 9.0.7
 - fix: Date detection is sometimes overly aggressive, for example, Q4 2008-12
 - feat: Add new Semantic Types - PERIOD.QUARTER, PERIOD.HALF
 - feat: Add new Semantic Type - IDENTITY.VAT_<COUNTRY> (Countries supported AT, ES, FR, IT, PL, UK)
 - feat: Add new Semantic Type - STATE_PROVINCE.PROVINCE_NAME_EC (Ecuador)

### 9.0.6
 - fix: Address incorrectly collapsing time formats when both date and time needed collapsing (See Issue #6)
 - feat: Bump google phonenumber

### 9.0.5
 - feat: Add new Semantic Type - PERSON.AGE
 - feat: I18N - Improve French Post Code detection

### 9.0.4
 - feat: I18N - Improve Coordinate detection in Dutch
 - feat: Improve consistency wrt to quoting ',' in returned Date formats

### 9.0.3
 - feat: I18N - Initial support for Latvian
 - feat: I18N - Enhance Croatian
 - feat: I18N - Be more lenient for Dutch Post Codes
 - feat: I18N - Improve French Department detection
 - feat: Add new Semantic Types - IDENTITY.EIN_US ("Employer Identification Number (US)"), STATE_PROVINCE.COMMUNE_IT ("Italian Commune")
 - feat: Add support for IBANs with embedded spaces
 - feat: Reimplemnent SSN as a Java plugin
 - feat: Improve Job Title detection
 - fix: Switch GENDER.TEXT_CN to GENDER.TEXT_ZH (and change localeTag to 'zh')
 - chore: Switch to jakarta.mail from javax.mail
 - chore: CLI - Fixup dependencies

### 9.0.2
 - chore: Switch to Gradle 7 way of versioning stuff

### 9.0.1
 - feat: I18N - Initial Greek support (Dates, Yes/No, PhoneNumbers)
 - feat: I18N - Improve Chinese support
 - feat: I18N - Add new Semantic Type - COUNTRY.TEXT_DE
 - fix: Outliers were not being correctly tracked for late detected RegExp types

### 9.0.0
 - BREAKING CHANGE: lugin definition has changed with 9.0 as has DateTimeParser.ofPattern.**
 - feat: New Feature.NO_ABBREVIATION_PUNCTUATION is enabled by default**
 - feat: I18N - Improve date detection in locales with abbreviations with periods for short-months (and AM/PM strings) (e.g. en-CA, en-AU) - see Feature.NO_ABBREVIATION_PUNCTUATION
 - feat: I18N - Add support for FREE_TEXT in french
 - feat: FREE_TEXT implies some level of uniqueness - change to insist on > .1 uniqueness
 - feat: I18N - Add new Semantic Types - STATE_PROVINCE.COUNTY_HU, CHECKDIGIT.UPC, COLOR.HEX, HASH.SHA1_HEX, HASH.SHA256_HEX
 - feat: Bump google phonenumber
 - feat: Add support for MAC ADDRESS detection with minus as well as colon
 - feat: Extend Shape support from 40 to 65 before declaring too long
 - fix: I18N - Year in french has an acute!
 - fix: I18N - Year in Catalan was wrong
 - fix: I18N - Fix issue with Chakma (locale ccp), issue related to surrogate pairs
 - chore: I18N - Add --abbreviationPunctuation to CLI

### 8.0.30
 - feat: Improve support for dates with full weekdays (EEEE)
 - fix: Fix issue with ZoneDateTime or OffsetDateTime and no valid data

### 8.0.29
 - feat: Cope with Date Format - "Fri 08 Jan 2010 15:11:16 +0000"

### 8.0.28
 - feat: Bump google phonenumber
 - feat: Decrease false positives on NAME.LAST_FIRST

### 8.0.27
 - chore: CLI - Move to separate jar
 - chore: Improve examples and README

### 8.0.26
 - feat: I18N - Add new Semantic Type - POSTAL_CODE.BG
 - feat: I18N - Improve year detection in Russian/Finnish/Danish
 - feat: I18N - Add Bulgarian Gender support
 - feat: I18N - Add Russian/Japanese LATITUDE/LONGITUDE detection

### 8.0.25
 - feat: I18N - FREE_TEXT - improve German samples
 - feat: I18N - Add new Semantic Types - POSTAL_CODE.POSTAL_CODE_UY, POSTAL_CODE.POSTAL_CODE_MX
 - fix: Do not die if tracing is on and the fieldName is extremely long
 - chore: CLI - Support setting Trace options
 - chore: Support adjusting Max Columns (Univocity option) from the command line
 - chore: Improve code coverage

### 8.0.24
 - feat: I18N - Add FREE_TEXT support for German
 - feat: Split COORDINATE_PAIR.DECIMAL into COORDINATE_PAIR.DECIMAL (no parens) and COORDINATE_PAIR.DECIMAL (parens)
 - feat: Do not label as COORDINATE.PAIR_DECIMAL without some signal from the header
 - feat: Improve detection of COORDINATE_PAIR.DECIMAL

### 8.0.23
 - chore: Change logger name from fta to com.cobber.fta
 - chore: Suppress testNG logging when executing tests
 - test: Improve test coverage
 - fix: Fix issues with TimeZoneOffsets with seconds (e.g. GMT+08:09:20)

### 8.0.22
 - feat: Add StrictMode to DateTimeParser

### 8.0.21
 - feat: I18N - Add new Semantic Type - POSTAL_CODE.POSTAL_CODE_SE (uses Bloom Filter)
 - feat: I18N - Improve COORDINATE_PAIR.DECIMAL, COORDINATE.LATITUDE_DECIMAL, and COORDINATE.LONGITUDE_DECIMAL detection rate (especially in Western Europe)
 - feat: I18N - Update Japanese Postal Codes
 - feat: I18N - Improve detection of non-localized doubles
 - feat: Update plugin format to explicitly indicate the type of the plugin ('java', 'list', or 'regex')
 - feat: Add support for a sample list for regex plugins (enables reasonable support for nextRandom())
 - feat: Bump Jackson
 - fix: I18N - Failed to handle UTF-8 minus sign on Exponents (e.g. locale "nn")

### 8.0.20
 - feat: I18N - Improve STREET_ADDRESS_EN and STREET_ADDRESS2_EN detection rate (especially UK)
 - test: I18N - More testing on non-localized doubles
 - chore: Add support for plugin validation from the command line

### 8.0.19
 - feat: I18N - Gender support for Romanian
 - feat: I18N - Improve support for Middle Name (cope with a blend of initials and names)
 - feat: I18N - Improve JOB_TITLE detection
 - feat: I18N - Improve Italian Province detection
 - feat: I18N - Improve Yes/No detection for Bulgarian, Catalan, Finnish, and Slovakian
 - feat: I18N - Improve 4 digit year detection for a set of European countries
 - fix: I18N - Fix issue with non-localized Doubles (also added modifier to Double - NON_LOCALIZED) - also more tests

### 8.0.18
 - feat: I18N - Improve Slovakian support
 - feat: I18N - Improve French-Canadian support
 - feat: I18N - Move "regExpsToMatch", "regExpReturned", "isRegExpComplete" to be per locale
 - feat: I18N - Support a concept of a non-localized Double (e.g. for example latitude which commonly does not use locale specific decimal separator)
 - feat: I18N - Gender support for Croatian, Catalan, Swedish and improve French
 - feat: I18N - Initial support for Romanian
 - feat: I18N - Add new Semantic Type - STATE_PROVINCE.REGION_IT
 - feat: Bump google phonenumber

### 8.0.17
 - feat: I18N - Add Gender support for Finnish, Polish
 - feat: I18N - Other minor improvements for Danish, Finnish, Polish
 - feat: Switch boolean setter of TextAnalyzer to configure(Feature) - deprecate old way
 - feat: Initial support for contentFormat (disabled by default)

### 8.0.16
 - chore: I18N - Rewrite Gender plugin - improve I18N support

### 8.0.15
 - feat: Improve header detection for Telephone
 - test: Improve test coverage
 - fix: Viznet - Do not generate bogus formats if date format switches, e.g. some dd/MM/yyyy then a set of yyyy-MM-dd - see TestDates.mixedDates()
 - fix: Viznet - Do not introduce grouping if already a Logical Type - see TestLongs.testLongLogicalType()
 - fix: Viznet - Fix date parsing for 02/08/2017 08:30:01 AM +0000 (ambiguous day & month).
 - fix: Viznet - Fix dates masquerading as longs (with errors)

### 8.0.14
 - feat: Add new Semantic Types - COORDINATE.EASTING, COORDINATE.NORTHING
 - fix: Viznet - Fix NumberFormatException with space padded years - see TestDate.fiscalYear()
 - fix: Viznet - Fix Exception with "88-0828S7" and many blanks - see RandomTests.strange()
 - fix: Viznet - Another nasty -0828S7" - see RandomTests.viznet3()
 - fix: Viznet - Another nasty '2018-06-26T15:27:50.' - see DetermineDateTimeFormatTests.unusualT()

### 8.0.13
 - feat: I18N - Improve Spanish support (Postal Codes + Year + Yes/No)
 - fix: Fix NPE on "05/09/2014 02:00:00 AM +0000" (rework PassTwo)

### 8.0.12
 - feat: I18N - Add support for Brazilian municipalities - STATE_PROVINCE.MUNICIPALITY_BR
 - feat: I18N - Improve gender support in Italian

### 8.0.11
 - feat: Add serialize(), deserialize(), merge(), and apply() to DateTimeParser (+ tests)
 - feat: Add fluid API support for config on DateTimeParser (deprecate old contructors)
 - fix: Minor changes to improve DateTimeParser determination

### 8.0.10
 - feat: Improve date documentation to cover training as well as simple format retrieval from a single sample

### 8.0.9
 - chore: Restructure plugins.json to ease localization

### 8.0.8
 - feat: I18N - Improve first-name detection for German
 - feat: I18N - Add POSTAL_CODE.POSTAL_CODE_DE
 - feat: Add support for Finite Types on Longs

### 8.0.7
 - feat: Bump google phonenumber
 - feat: Improve detection of First Names in French/Spanish
 - feat: Add a set of common African last names
 - feat: Add a set of common Mexican first names
 - feat: Minimal mod to header regexp to grab more lat/longs
 - feat: Enhanced Gender to handle Woman/Man as well as Female/Male (and localized versions)
 - fix: RegExp header for City was a little aggressive (picked up 'Subject Ethnicity')

### 8.0.6
 - feat: I18N - Support localized versions of Yes/No for booleans e.g. Italian Si/No, French Oui/Non
 - feat: Support localized versions of YEAR/DATE
 - feat: I18N - Be more forgiving on Gender detection (also for non-English), also Sexo as a synonym for Gênero in Portuguese

### 8.0.5
 - feat: Add a couple more countries to reference list (ESWATINI, NORTH MACEDONIA)
 - feat: Improved documentation for asJSON

### 8.0.4
 - feat: Add support for distinctCount

### 8.0.3
 - feat: Improve performance for constant valued columns (x20)
 - feat: Set Total Count when using command line tool
 - fix: Fix variance/standardDeviation on merged analyses
 - test: Improve test coverage on merge() especially when cardinality blown
 - chore: Update Roadmap

### 8.0.2
 - feat: For Doubles & Longs print the min/max/top/bottom in approximately the format of the incoming data (including localization)
 - fix: merge() could not cope with cases where the cardinality was large and the top/bottom values were not in the captured set
 - test: Improve test coverage on merge()

### 8.0.1
 - fix: Fix Serialization issue for tiny datasets
 - fix: Fix issue for non-US locales where the returned TextAnalyzer was not in the locale of the merged entities
 - test: Improve test coverage on TextAnalyzer serialize()/deserialize()/merge() in particular in non-default locales

### 8.0.0
 - BREAKING CHANGE: Data Signatures for 8.X are not the same as prior releases, see details below**
 - feat: Add Serialize()/Deserialize()/merge() to TextAnalzer
 - feat: Min/Max on the JSON output now outputs long/double values using the detection locale.
 - feat: Bump google phonenumber
 - feat: Add NumericWidening to AnalysisConfig object
 - fix: Fix issue with data signatures not being consistent (due to order of Cardinality map not being constant)
 - fix: Fix issue with mean/standard deviation being incorrectly calculated when using trainBulk (see TestBulk.bulkLong())
 - fix: Fix issue with min/max not being printed with enough precision (see TestDoubles.verySmall())

### 7.0.5
 - feat: Fix detection of '06/Jan/2008 15:04:05 -0700' and 'Mon, 02 Jan 2006 15:04:05 -0700'
 - test: Cleanup a couple of tests

### 7.0.4
 - feat: Bump Jackson due to CVE-2020-36518
 - feat: Fix detection of '2014:3:31' and '2014:03:31'

### 7.0.3
 - feat: Improve Date detection - coping with additional variable length components, e.g. the following now pass:
	- "May 8, 2009 5:57:51 PM", "oct. 7, 70", "2014/4/8 22:05", "2014/04/2 03:00:51", "2014:4:8 22:05", "2014:04:2 03:00:51", "2014:4:02 03:00:51"
 - feat: DateTimeParser.determineFormatString() - Cope with single quotes in input string - generated an error previously, e.g., "oct. 7, '70"

### 7.0.2
 - feat: Improve FREE_TEXT detection

### 7.0.1
 - fix: Fixup plugin.json (qualifier was DOY.FULL_<LOCALE> but was actually returning DAY.FULL_<LOCALE>)
 - fix: Fixup plugin.json (qualifier was DOY.ABBR_<LOCALE> but was actually returning DAY.ABBR_<LOCALE>)
 - feat: Improve JavaDoc

### 7.0.0
 - BREAKING CHANGE: Major version change as Plugin interface has changed (only impacts Plugin authors).
 - fix: Fix issue related to outlier counting when using Bulk mode
 - fix: COUNTRY_EN plugin was not honoring threshold from plugins file

### 6.0.9
 - chore: Cleaning up FREE_TEXT stuff (would like to run it without mandatory headers)

### 6.0.8
 - feat: Add Semantic Type (FREE_TEXT) - captures Descriptions, Notes, Comments, ...

### 6.0.7
 - fix: Fix replay
 - chore: Cleaning code, noqualifier option to JavaDoc
 - feat: Tweak longitude, latitude header match string

### 6.0.6
 - feat: Bump slf4j-api
 - fix: Fix issue where setMaxInputLength could only be increased not decreased

### 6.0.5
 - feat: Improve JavaDoc

### 6.0.4
 - feat: Add support for replaying FTA trace files.

### 6.0.3
 - fix: Handle grouping in doubles with Exponents (may never be seen but should not crash)

### 6.0.2
 - feat: Bump google phone number.
 - chore: Fixup minor issues in README & document Signature details

### 6.0.1
 - chore: No longer need build7.gradle 

### 6.0.0
 - feat: Split into fta-core (date-detection) and fta-types (Semantic Types)
 - feat: Bump jackson
 - chore: Bump gradle to 7.X (finally)

### 5.1.24
 - feat: Add a little more leniency for finite types (especially larger ones)
 - chore: Bump plugins-version, improve Date example

### 5.1.23
 - fix: Fix NPE when using FTA from DBProfiler (also added test case)
 - chore: Cleaning imports
 - feat: Broaden set of recognized languages

### 5.1.22
 - feat: Add Semantic Type - CHECKDIGIT.ISBN
 - feat: Bump guava.

### 5.1.21
 - feat: Add Semantic Type - COMPANY_NAME (Header-only detection)
 - feat: Bump google phone number.

### 5.1.20
 - feat: Improve Portuguese support. Add Semantic Type - POSTAL_CODE.POSTAL_CODE_PT, STATE_PROVINCE.DISTRICT_NAME_PT.

### 5.1.19
 - feat: Ensure that all Threshold statements are captured in the plugins file (not the code).  Clarify that the default is 95%.

### 5.1.18
 - feat: Add Semantic Type - IDENTITY.DUNS (Data Universal Numbering System (Dun & Bradstreet))
 - chore: Add support for generating a set of sample files
 - chore: Fixup interpolation in debugging.

### 5.1.17
 - feat: Add Semantic Type - POSTAL_CODE.POSTAL_CODE_JA, POSTAL_CODE.POSTAL_CODE_FR

### 5.1.16
 - chore: Add tests for IDENTITY.NHS_UK and IDENTITY.AADHAR_IN
 - fix: COORDINATE.LONGITUDE_DMS and COORDINATE.LATITUDE_DMS were reversed

### 5.1.15
 - feat: Add Semantic Type - IDENTITY.NHS_UK (UK)
 - feat: Bump Google phone number
 - chore: Compress Gender in README

### 5.1.14
 - feat: Add Semantic Type - IDENTITY.AADHAR_IN (India)
 - chore: Lots of renaming + newInstance already calls initialize - so don't do it again

### 5.1.13
 - feat: Add Semantic Types - GENDER.TEXT_MS (Malaysia), GENDER.TEXT_CN (China)
 - feat: Limited support for Chinese Dates
 - chore: printf to log.debug/info/...
 - chore: upgrade version of TestNG

### 5.1.12
 - fix: totalSamples should have been a long.

### 5.1.11
 - chore: Update copyright year + add a few missing copyrights
 - feat: Add an example of using the AnalyzerContext

### 5.1.10
 - feat: Add new Semantic Type IDENTITY.INDIVIDUAL_NUMBER_JA (Individual Number / My Number) (locale ja)
 - feat: Add new Semantic Type IDENTITY.SSN_CH (AVH / Sozialversicherungsnummer) (locale de-CH, fr-CH ,it-CH)
 - fix: Fix SSN_FR for folks born overseas and those born in Corsica

### 5.1.9
 - chore: Back down slf4j to latest stable

### 5.1.8
 - feat: Add new Semantic Type IDENTITY.SSN_FR (locale fr-FR)

### 5.1.7
 - feat: Switch to sl4j for logging (delay initialization unless debug is on)
 - chore: Fixes courtesy of PMD

### 5.1.6
 - feat: Add new Semantic Type NAME.MIDDLE, NAME.MIDDLE_INITIAL
 - feat: If two Semantic Types are equal in score, then break the tie first with the header, then with the priority
 - chore: Use PluginDefinition.findByQualifier instead of new PluginDefinition(), initialize plugins

### 5.1.5
 - feat: Bump Google phone number
 - feat: Bump Gradle version
 - feat: Add support for South African Province Names (STATE_PROVINCE.PROVINCE_ZA, STATE_PROVINCE.PROVINCE_NAME_ZA)

### 5.1.4
 - feat: Bump Jackson Databind

### 5.1.3
 - feat: Improve PhoneNumber plugin to support example generation for all locales

### 5.1.2
 - feat: Improve Industry, TimeZone, and Honorific matching.

### 5.1.1
 - fix: Name matching was a little greedy.

### 5.1.0
 - fix: TopK/BottomK ordering is incorrect for non-strings.

### 5.0.9
 - feat: Add new Semantic Types EPOCH.MILLISECONDS, EPOCH.NANOSECONDS

### 5.0.8
 - feat: Cope with dates with 9 decimals for the fractional seconds.

### 5.0.7
 - fix: Cope with null entries in trainBulk when tracing, improve error reporting for rubbish argument to trace.

### 5.0.6
 - feat: Cope with dates with 7 decimals for the fractional seconds.

### 5.0.5
 - feat: Cope with dates with 6 decimals for the fractional seconds - e.g. yyyy-MM-dd HH:mm:ss.SSSSSS.

### 5.0.4
 - feat: missed reference file for IANA Time Zones.

### 5.0.3
 - chore: Cleaning up.
 - feat: Bump google phone number.
 - feat: Add new Semantic Type TIMEZONE.IANA - supports IANA (Olson) Time Zones.

### 5.0.2
 - chore: Cleaning up.

### 5.0.1
 - chore: Cleaning up, micro performance improvement

### 5.0.0
 - feat: Next gen RegExp support (most signatures are unchanged - but there are some differences), also plugin signatures changed

### 4.9.2
 - chore: Reverse order of ChangeLog file :-)

### 4.9.1
 - fix: Should have specified Charset when reading all reference files!!

### 4.9.0
 - feat: Minor improvements for performance
 - feat: Clamp input to 4096 characters (can be widened by invoking setMaxInputLength()) - will change existing signatures unless data is narrower than 4096.
	Note: this also backs out the change in 3.8.2 to optionally externally clamp.

### 4.8.2
 - feat: Add support for tracing for trainBulk
 - feat: Add support to externally set the maxLength of a field (if sending in truncated data to FTA)

### 4.8.1
 - feat: Bump google phone number version

### 4.8.0
 - feat: LogicalType now implements LTRandom - so you can also call nextRandom() from RegExp plugins
 - feat: Add some Italian first and Last names
 - feat: Improve RegExps for COORDINATES, MONTH.DIGITS, DAY.DIGITS
 - feat: Support isRegExpComplete from pluginDefinition
 - test: Add test to validate signatures in plugin file
 - fix: Fix a couplle of signatures

### 4.7.11
 - test: Split out performance tests
 - feat: Improve overall performance by passing around trimmed value

### 4.7.10
 - feat: Add Semantic Types - STATE_PROVINCE.STATE_BR, STATE_PROVINCE.STATE_NAME_BR, STATE_PROVINCE.PREFECTURE_NAME_JA
 - test: Improve testing infrastructure, so can run particular groups of tests
 - fix: Complain if the priority of the user registered plugins overlaps the builtin space (0-2000]
 - feat: Improve support for Japanese (dates & times, Gender, Prefectures)
 - fix: Switch GENDER_JP to GENDER_JA and correct language (should have been ja not jp)

### 4.7.9
 - feat: Add Semantic Types - COORDINATE.LATITUDE_DMS, COORDINATE.LONGITUDE_DMS
 - fix: Fixed issue where short (i.e. missing leading 0's) Zip codes were not being detected
 - feat: Refreshed US Zip list with latest from USPS
 - feat: Add Semantic Type - POSTAL_CODE.ZIP5_PLUS4_US

### 4.7.8
 - feat: Add tracing support - set via environment variable FTA_TRACE or via setTrace(String) e.g. setTrace("stream=COUNTY,samples=10000")
	Options are:
		enabled=true/false,
		stream=<name of stream> (defaults to all)
		directory=<directory for trace file> (defaults to java.io.tmpdir)
		samples=<samples to trace> (defults to 1000)
 - feat: Cache ObjectMapper to improve performance

### 4.7.7
 - feat: Improve the probability of locating a lat/long header
 - feat: Change handling of plugin retrieval if no Locale specified (effectively defaulting to English), add TestCase
 - chore: Move more stuff into AnalysisConfig

### 4.7.6
 - feat: Add a couple more street markers
 - feat: Add Semantic Types - GENDER.TEXT_TR (Turkey)

### 4.7.5
 - feat: Improve detection of Phone Numbers when we have numeric input
 - feat: Cope with dates of the form M/YYYY as well as MM/YYYY
 - fix: Fix bad name detection when all names are of the form 'FIRST M. LAST'

### 4.7.4
 - feat: Add a set of missing signatures

### 4.7.3
 - feat: Improve structure signature access (also cache on plugins file)

### 4.7.2
 - feat: Improve reference list for industries
 - feat: Improve random samples for Streets
 - feat: Add new Semantic Type - STREET_ADDRESS2_EN (Second line of an address)
 - feat: Add 'DISTRICT OF COLUMBIA' as a State name
 - feat: Improve name support for Finland, and Norway

### 4.7.1
 - feat: Add new Semantic type INDUSTRY_EN
 - feat: Improve uniqueness detection, if a field is monotonic increasing or monotonic decreasing then its uniquness is 1!

### 4.7.0
 - feat: Change isValidSet() on plugins to take an AnalyzeContext not just a stream name
 - NOTE: Bumped to 4.7.0 because preexisting plugins need to be minimally updated

### 4.6.6
 - feat: Add support for generating 'random' examples of the CheckDigits (e.g. IBAN and friends)
 - feat: nextRandom() on names now only returns names without spaces (somewhat less random :-))
 - test: Improve testing of nextRandom()
 - fix: Fix issue in Japanese Gender support
 - fix: Fixup typos in name of a couple of Canadian provinces
 - fix: Fix US counties names to use simple hyphens
 - fix: Fix regular expression returned for a number of Western European countries
 - fix: Fix regular expression returned for CHECKDIGIT.EAN13

### 4.6.5
 - feat: Extend driver to support AnalyzeContext usage
 - feat: Add broader range of honorifics (IND. and MISC.)

### 4.6.4
 - fix: Significant rework of fractional seconds handling to broaden support - see dateBug()

### 4.6.3
 - feat: Allow other folks to call the Driver

### 4.6.2
 - feat: Rework Gender support to improve I18N

### 4.6.1
 - feat: Add support for the Netherlands (PostalCode, FIRST & LAST names, GENDER)

### 4.6.0
 - feat: Extend context provided to Analysis (now includes Stream Name, Resolution Mode, Composite Name, Composite elements)
 - NOTE: Bumped to 4.6.0 because new TextAnalzer(null) will now complain, nobody ***should*** have done this, because that is really new TextAnalzer()

### 4.5.29
 - feat: Improve FIRST and LAST name country support
 - feat: Allow comments in reference files

### 4.5.28
 - feat: Improve County list (by removing County word)

### 4.5.27
 - feat: Add Semantic Types - CHECKDIGIT.ABA (ABA Number (or Routing Transit Number (RTN)))

### 4.5.26
 - feat: Add example and documentation for generation use case.

### 4.5.25
 - feat: Add Semantic Types - CURRENCY.TEXT_EN, STATE_PROVINCE.COUNTY_US
 - feat: Bump jackson, googlephonenumber
 - feat: Enable Name support for Brazil, German, French and Portugal
 - feat: Add example and documentation for validation use case.

### 4.5.24
 - feat: Bump google phone number version
 - feat: Add Semantic Types - STATE_PROVINCE.CANTON_NAME_CH, STATE_PROVINCE.CANTON_CH

### 4.5.23
 - feat: Add Semantic Types - NATIONALITY_EN, STATE_PROVINCE.COUNTY_UK
 - fix: Be a bit more forgiving of rubbish like NaN when processing Double data (REGRESSION)

### 4.5.22
 - feat: Add support for Italian/Spanish/Netherlands Provinces - STATE_PROVINCE.PROVINCE_IT, STATE_PROVINCE.PROVINCE_NAME_IT, STATE_PROVINCE.PROVINCE_NAME_ES, STATE_PROVINCE.PROVINCE_NAME_NL

### 4.5.21
 - fix: Could not cope with fractional seconds that was not at the end (e.g. 2021-08-23T19:03:45.63-04:00)

### 4.5.20
 - feat: Bump Jackson and google phone number
 - feat: Minimize the number of bogus 'enums' - do not generate enum for long constant length strings of digits and alphas
 - feat: Improve documentation
 - fix: Fix Job Title plugin when presented with no words

### 4.5.19
 - feat: Improve documentation wrt Locales
 - fix: CONTINENT* and CITY should only be active in English language

### 4.5.18
 - feat: Improve list of French regions (include common alternate spellings, old regions, etc).
 - feat: Add Semantic Types - GENDER.TEXT_DE, GENDER.TEXT_FR

### 4.5.17
 - feat: Add Semantic Types - GENDER.TEXT_JP, STATE_PROVINCE.DEPARTMENT_FR, STATE_PROVINCE.REGION_FR, STATE_PROVINCE.STATE_NAME_DE

### 4.5.16
 - feat: CLI - Improve error message if plugin file not found
 - fix: Fix rejection of Finite plugins with a small number of members

### 4.5.15
 - feat: Improve Java Doc

### 4.5.14
 - feat: Upgrade version of phone number library
 - feat: Minimal mod to Gender detection in Portugese
 - feat: Only use level 2 pattern if below detection threshold (and associated test)

### 4.5.13
 - feat: Add Semantic Types - DAY.ABBR_<Locale>, DAY.FULL_<Locale>

### 4.5.12
 - chore: Clean up some warnings identified by Github

### 4.5.11
 - fix: Switch back to random (no SecureRandom) for Bulk mode.
 - feat: Add Semantic Types - GENDER.TEXT_PT, JOB_TITLE_EN

### 4.5.10
 - fix: FirstLast Plugin - insist on a decent spread of distinct last names and distinct first names

### 4.5.9
 - fix: Improve SSN RegExp and test cases, fix bug where detection compares against RegExpReturned not RegExpsToMatch
 - feat: Improve RegExp for City and Names
 - feat: Widen out list for Honorifics
 - feat: Add new Semantic Type - VIN (Vehicle Identification Number)

### 4.5.8
 - fix: Remove semantic Type REGION.TEXT_EN

### 4.5.7
 - feat: Add new Semantic Types - CHECKDIGIT.IBAN

### 4.5.6
 - chore: Rationalize names of reference files
 - feat: Add new Semantic Types - HONORIFIC_EN, STREET_MARKER_EN

### 4.5.5
 - feat: RegExp for exponent should cope with Unicode Minus sign
 - feat: Improve debugging support
 - fix: Infinite types now only operate on the base type they are configure for - will stop TELEPHONE eating dates
 - fix: FirstLast Plugin - insist on a decent spread of names, so don't get caught by 5 things that could be names repeated 2000 times
 - fix: PhoneNumber Plugin - The Google library is very permissive and generally strips punctuation, be little more discerning so that we don't treat ordinary numbers as phone numbers
 - feat: Switch Random to SecureRandom
 - feat: Now builds on Java 11, always targets Java 8 (currently)

### 4.5.4
 - fix: Fix formatting for TopK and BottomK (when Date or Time types) to honor formatting of input
 - feat: Upgrade google phone number library

### 4.5.3
 - fix: Fix broken Sample - SamplePlugin
 - feat: Enhance ColorPlugin to support French as well as English
 - chore: Add French sample

### 4.5.2
 - feat: Add new Semantic Types - CONTINENT.CODE_EN, CONTINENT.TEXT_EN

### 4.5.1
 - fix: Switch Semantic Type REGION -> REGION.TEXT_EN (add test)
 - feat: Prefer to generate the RegExpReturned from Finite types

### 4.5.0
 - feat: Add new Semantic Type REGION - captures a World Region (e.g. Europe, North America, ...)
 - feat: Add new Semantic Type STATE_PROVINCE.STATE_MX - Mexican State Code
 - feat: Add new Semantic Types STATE_PROVINCE.STATE_NAME_<CC>, for CC = AU, CA, MX, US, and NA (for North America)
	- Captures State names, e.g. California, Ontario (also includes State Codes)

### 4.4.1
 - feat: Uniqueness should never be null -1.0 indicates no Perspective

### 4.4.0
 - feat: Add Uniqueness metric

### 4.3.3
 - fix: Fix bug with fields with trailing pipe symbols

### 4.3.2
 - feat: Upgrade google phone number library
 - test: Fix unreliable test
 - feat: Add support for EAN Checkdigit

### 4.3.1
 - fix: Fix bad RegExp for CUSIPs
 - feat: Upgrade google phone number library

### 4.3.0
 - feat: Interface change - add support for totalCount, will be -1 unless set explicitly by something external that knows the answer.
 - feat: Interface change - move setKeyConfidence to TextAnalyzer

### 4.2.0
 - feat: Interface change - possibleKey (boolean) is now keyConfidence (double - 0.0 -> 1.0)
 - feat: Added setKeyConfidence to TextAnalysisResult so we can override if external system knows better

### 4.1.1
 - feat: Fixup documentation to align 4.X
 - feat: Bump google phone number version
 - fix: Change a couple more exceptions

### 4.1.0
 - feat: Add support for localized offset in Dates 'O' and 'OOOO'
 - fix: Don't throw unchecked exceptions for invalid locales and issues with plugins since these should be trapped and reported by client
 - feat: Add support for Unicode minus sign \u2212
 - feat: Add support to ignore Unicode LEFT_TO_RIGHT_MARK \u200E

### 4.0.0
 - BREAKING CHANGE: Interface change - now throw FTAPluginException if passed invalid Plugins
 - feat: Add fta version to the output, just in case we ever change signatures
 - feat: Add fta version to the jar - so we can tell what is deployed independent of the jar filename
 - feat: Add --version option to Driver - so we can see what version we are running

### 3.0.22
 - chore: More PMD cleanups
 - feat: Minor documentation fix

### 3.0.21
 - chore: Lots of PMD cleanup
 - feat: Add support for CHECKDIGIT.LUHN (Digit String that has a valid Luhn Check digit)
 - feat: Add support for Securities Identifiers - CUSIP, SEDOL, and ISIN

### 3.0.20
 - chore: Lots of PMD cleanup
 - fix: Fix issue related to updating regexp when not all samples matched
 - test: Add some Shape testing
 - feat: Phone number samples need a decent cardinality to pass muster if no header
 - fix: 'CS' country code was valid (so allow as legal)

### 3.0.18
 - fix: Fix bug in Phase 3 of date detection
 - feat: Updated google phone number library

### 3.0.17
 - feat: Improve recognition of Phone Number fields that do not have a recognized header, bump libraries, improve recognition based on headers generally

### 3.0.16
 - fix: Fix bug with mixed Date processing
 - feat: Add ability to set field separator

### 3.0.15
 - feat: Bump versions

### 3.0.14
 - feat: Bump versions

### 3.0.13
 - fix: Another attempt at fixing the issue in 3.0.12, also add a test

### 3.0.12
 - fix: Subtle bug causes NPE, minLongNonZero is initialized to MAX_VALUE (as marker)
 - feat: Upgrade libraries

### 3.0.11
 - chore: Upgrade dependencies
 - feat: Improve support for padded fields - both days and hours

### 3.0.10
 - feat: Bump dependencies

### 3.0.9
 - feat: After generating an enum we need to check again to see if this matches a logical type & update dependencies

### 3.0.8
 - feat: Restructure code to separate out date functionality (and improve interfaces)

### 3.0.7
 - fix: Bulk mode from command line was not honoring options

### 3.0.6
 - feat: Add support for standalone date parsing

### 3.0.5
 - fix: DataSignature should not vary based on name of column!
 - feat: Bump versions

### 3.0.4
 - fix: getStandardDeviation() needs to guard against null variance

### 3.0.3
 - fix: getMean() and getStandardDeviation() should return boxed types

### 3.0.2
 - feat: Replace sum column for numerics (long, double) with mean and stadard deviation (using Welford's algorithm)
 - feat: Add detection for a set of superceded ISO-4217 codes to aid with detection
 - feat: Add option for retrieving plugin based on a training set
 - test: Added a couple of tests & fixed a couple of tests

### 3.0.1
 - feat: DataSignature is now independent of Structure
 - feat: Support Unix date command which implies padding, e.g. 'Thu Jul  7 09:23:56 PDT 2020' and 'Thu Jul 23 09:56:23 PDT 2020'

### 3.0.0
 - BREAKING CHANGE: Signatures for 3.X are not the same as 2.X ***
 - feat: Switch to using SecureRandom instead of Random
 - fix: Zip refs file had bogus entry in it
 - fix: If finite sets have same score make sure to select the one with the highest priority
 - feat: Change DataSignature to have less dependency on Structure facts
 - fix: Fix issue with FirstName/LastName not returning consistent results from one run to next with secureRandom
 - feat: Bump versions

### 2.3.51
 - feat: Bump versions, blackList -> invalidList, outlier documentation.

### 2.3.50
 - feat: Bump versions

### 2.3.49
 - chore: Externalize dependencies for easier management

### 2.3.48
 - feat: More Countries added to the list we recognize (also dropped the threshold to 85%)

### 2.3.47
 - feat: Update dependencies
 - test: Improve test coverage
 - fix: Fix bug where alpha string was not being promoted to alphanumeric string and hence not matching customer supplied logical type

### 2.3.46
 - fix: Address issue when no RegExpsToMatch were supplied
 - test: Improve test coverage

### 2.3.44
 - fix: Fix bug with YYYYMMDD dates which have 00000000 as null value

### 2.3.43
 - chore: More Sonar Lint cleanup

### 2.3.42
 - chore: Sonar Lint cleanup
 - feat: Upgrade gradle & libraries

### 2.3.41
 - chore: Address remaining findbugs issues

### 2.3.40
 - chore: Address a set of findbugs issues
 - test: Improve test coverage

### 2.3.39
 - fix: Improve test coverage (and fix bug in trainBulk as a consequence :-) )
 - chore: Fixup build.gradle warnings to prepare for gradle 7.0

### 2.3.38
 - chore: Update Copyright to 2020
 - test: Improve test coverage

### 2.3.37
 - feat: Add support for min/max on String RegExp types
 - feat: Add some missing TimeZones (short display names)
 - feat: Support merging H and k if we detect a 24 time
 - chore: Remove a few warnings
 - test: Improve test coverage
 - fix: Fix Usage message
 - feat: Update libraries

### 2.3.36
 - feat: Improve outlier detection on enums

### 2.3.35
 - fix: Fix StructureSignature generation and associated tests

### 2.3.34
 - feat: Add support for new Semantic Types - DAY.DIGITS and MONTH.DIGITS
 - feat: Add support for minSamples and minMaxPresent on RegExp matchers
 - feat: Add support for Boolean (y/n)
 - feat: Improve regExp generated in the case of character classes e.g. we would rather see [A-G] than A|B|C|D|E|F|G
 - fix: Fix issues with some missing topK and bottomK

### 2.3.33
 - chore: Cleanup DateTime tracking and add a double semantic test

### 2.3.32
 - docs: Document 'blacklist', upgrade jackson, improve code documentation, add description for Logical Types
 - feat: Switch Content for inline to a JSON document, add first/last as synonyms for FIRST and LAST NAME

### 2.3.31
 - feat: Add support for detecting 'k' (Date format)

### 2.3.30
 - feat: Improve support for variable number digits in the fractional seconds

### 2.3.29
 - feat: Add support for BlackList (list of invalid values), improve test coverage

### 2.3.28
 - test: Improve test coverage

### 2.3.27
 - feat: Add support for dataSignature; add getters for dataSignature and structureSignature; add tests

### 2.3.26
 - feat: Add support for detecting Hex numbers - use to support new Semantic Type - MAC Address
 - feat: Add support for yyyy/dd/mm dates - really silly but they exist
 - feat: Upgrade dependent libraries
 - fix: Fix bug with with a mix of one and two digit percentages (e.g. 4%, 12%)

### 2.3.25
 - feat: Add support for Structural Signature
 - fix: Address issue related to incorrectly merging YY and YYYY

### 2.3.24
 - feat: Update libraries to resolve security vulnerabilities

### 2.3.23
 - feat: Support doubles with trailing minus signs

### 2.3.22
 - feat: Add support for checking dependencies, upgrade version of google phonenumber, support longs with trailing minus signs, fix copyright

### 2.3.21
 - perf: Use new class RandomSet to enable to access member randomly without a separate parallel array.

### 2.3.20
 - feat: RegExp matching should be based on the most frequent pattern and not rely on there being only one

### 2.3.19
 - feat: Cities should allow -'s and apostrophes (e.g. "Martha's Vineyard")

### 2.3.18
 - chore: Just cleaning

### 2.3.17
 - feat: Support CA Postal Codes in the US locale
 - fix: Bad regexp for FIRST_LAST and LAST_FIRST and LANGUAGE.TEXT_EN
 - feat: Add LogicalTypeFactory to return a LogicalType from a PluginDefinition
 - fix: Fixed bug in UK Postal Code random generation
 - fix: isValid on RegExp's not honoring min & max values
 - fix: CA Post Codes missing validation expression in plugins

### 2.3.16
 - fix: Fix multi-threading issue (and improve exception reporting)

### 2.3.15
 - feat: Add support for top K/bottom K on LocalDate, LocalDateTime, ZonedDateTime

### 2.3.14
 - feat: Change default DateResolutionMode from None to Auto - if executing from the command line

### 2.3.13
 - feat: Support Lang as a weak synonym for Language
 - fix: Also validate outliers when looking at finite sets, do not back out Date types as aggressively

### 2.3.12
 - feat: Add support for LANGUAGE.ISO-639-2, broaden out support for LANGUAGE.TEXT_EN

### 2.3.11
 - feat: Add support for detecting Language (as Text)
 - feat: Relax cardinality constraints if the header looks really good
 - feat: Try all RegExp types and take the best not the first
 - feat: A GUID is a perfectly good key candidate
 - fix: Address issue related to no default Semantic Types preventing registration of any Semantic Types

### 2.3.10
 - feat: Improve (marginally) merged name detection, accept lon as a synonym for longitude

### 2.3.9
 - feat: Add support for SSN

### 2.3.8
 - feat: Add support for yyyy/MM and yyyy/MM and associated tests, plus some cleaning.

### 2.3.7
 - feat: Add support for MM/yyyy and MM-yyyy (will return a LocalDate)

### 2.3.6
 - feat: Add support for MONTH_FULL (e.g. January), improve Zip detection (short zips), add support for NAME.FIRST_LAST (e.g. 'Tim Segall')

### 2.3.5
 - feat: More countries, improve ISO country code detection, improve city detection, improve long/lat detection

### 2.3.4
 - feat: Improve detection of Emails and URLs; add support for top & bottom k values

### 2.3.3
 - feat: Add support for NAME.LAST_FIRST (e.g. 'Segall, Tim'); fix NPE with --help

### 2.3.2
 - feat: Upgrade version of phone number library; improve NAME detection; improve Country detection

### 2.3.1
 - feat: Add support for Australian & Canadian Postal Codes

### 2.3.0
 - feat: Add support for AU States, and move more aggressively to plugins defintion

### 2.2.2
 - feat: Add support for UK Postal Codes

### 2.2.1
 - feat: Add Town as a synonym for City

### 2.2.0
 - feat: Change the Regular Expressions to be slightly more accurate and more Python friendly

### 2.1.29
 - feat: Do not report shape detail if it is not meaningful (or complete)
