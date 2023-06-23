
## Changes ##

### 14.6.1
 - BUG: RegExpSplitter.newInstance(String) Incorrectly Parses Ranges with Multiple Digits in the Max (Issue #44)

### 14.6.0
 - ENH: I18N - Add new Semantic Types - STATE_PROVINCE.REGION_NAME_PE (Peruvian Region/Department) + STATE_PROVINCE.PROVINCE_NAME_PE (Peruvian Province) + STATE_PROVINCE.REGION_NAME_TZ (Tanzanian Region)
 - ENH: Add new Semantic Types - STATE_PROVINCE.STATE_FIPS_US (US State FIPS code) + STATE_PROVINCE.COUNTY_FIPS_US (US County FIPS code)
 - ENH: I18N - Add "HEMBRA", "VARÓN" for Gender detection in Spanish
 - ENH: I18N - Add withEnglishAMPM() to DateTimeParser and TextAnalyzer.Feature.ALLOW_ENGLISH_AMPM to allow recognition of "AM" and "PM" independent of the locale
 - ENH: I18N - Improve recognition of STATE_PROVINCE.REGION_NAME_FR
 - ENH: I18N - Improve recognition of STATE_PROVINCE.STATE_BR
 - ENH: Bump jackson, google phonenumber and guava
 - BUG: I18N - Fix header regexp for Age in french (improves PERSON.AGE and PERSON.AGE_RANGE detection)
 - BUG: I18N - Change STATE_PROVINCE.REGION_IT to STATE_PROVINCE.REGION_NAME_IT (and hence signature)

### 14.5.0
 - ENH: Add new Semantic Type - FILENAME (Name of file)

### 14.4.0
 - ENH: Improve TextAnalysisResult.asPlugin() to support returning the plugin definition for known Semantic Types
 - INT: upgrade version of TestNG
 - CLI: attempt to detect if --skip should be utilized

### 14.3.3
 - ENH: Improve ModeBulk example to demonstrate plugin retrieval for a Semantic Type (#42)

### 14.3.2
 - BUG: Add missing file from checkin

### 14.3.1
 - BUG: Fix issue with late switches from DOUBLE_GROUPING to SIGNED_DOUBLE_GROUPING
 - BUG: Fix issue with Switching from SIGNED_DOUBLE_TRAILING to SIGNED_DOUBLE_TRAILING_GROUPING
 - BUG: Fix issue with 0000 as a year (format: yyyy)
 - ENH: Bump jackson
 - ENH: Bump google phonenumber
 - CLI: Defend against null option files
 - CLI: Continue processing even if one of the files provided has fatal errors

### 14.3.0
 - ENH: I18N - Improve support for COORDINATE.LATITUDE_DECIMAL and COORDINATE.LONGITUDE_DECIMAL (nl-NL)
 - ENH: Fix up test suite to support compiling with Java 17
 - CLI: Add support for --trailer to ignore the last <n> lines
 - BUG: Fix a couple of cases of incorrect qualifiers (SIGNED and GROUPING)

### 14.2.1
 - BUG: Fix a small number issues discovered running entire Viznet suite

### 14.2.0
 - ENH: Support 'yyyy/MM/dd HH' and friends in the DateTimeParser (and hence TextAnalyzer) (#36)

### 14.1.0
 - ENH: Flip EPOCH.SECONDS and EPOCH.MILLISECONDS from RegEx to Java - improves detection (#40)
 - BUG: Fix --pluginDetection to generate valid plugins from Training data (also associated test case)
 - BUG: Fix poor validation of Java plugin at registration time (#41)

### 14.0.0
 - ENH: Format of plugins has changed - content is now true JSON as opposed to a String
 - BUG: Fix --semanticType being ignored when using RecordAnalyzer

### 13.7.0
 - ENH: I18N - Add new Semantic Type - IDENTITY.NPI_US (National Provider Identifier (US))
 - ENH: DateTimeParser: Add support for date detection of the form yyyyMM and yyyyMMddHH (#39)
 - ENH: Improve detection of timestamps (minor)

### 13.6.2
 - ENH: Add support for period detection of the form yyyyMM (#38)
 - ENH: I18N - yyyyMMdd detection (now look for localized "date" header)
 - ENH: Improve robustness of command line processing

### 13.6.1
 - ENH: I18N - minor improvement to STREET_NAME_BARE_NL detection
 - ENH: Remove deprecated DateTimeParser constructors
 - BUG: Fix regExp returned with Doubles that are actually LocalDates (also add test case)
 - ENH: Improve ModeStreaming example to report if the Semantic Type is not detected due to non-supported locale (#35)
 - INT: Some code cleanups

### 13.6.0
 - BUG: EPOCH.MILLISECONDS was actually EPOCH.SECONDS, EPOCH.NANOSECONDS was actually EPOCH.MILLISECONDS (#37)

### 13.5.1
 - ENH: Improve PERSON.DATE_OF_BIRTH detection allow LocalDateTime (also es,nl support)
 - INT: Bump gradle to 8.1.1

### 13.5.0
 - ENH: Add general support for Semantic Types with Date/DateTime types
 - ENH: Add new Semantic Types - PERSON.YEAR_OF_BIRTH and PERSON.DATE_OF_BIRTH
 - ENH: Minor improvements to STREET_NUMBER detection
 - ENH: Bump google phonenumber
 - INT: Minor debugging improvements

### 13.4.1
 - ENH: Bump logback-classic
 - ENH: Bulk mode should prioritize 'interesting' values in preference to null or blanks (#33)
 - ENH: Bump jackson
 - INT: Update copyright
 - INT: Some code cleanups

### 13.4.0
 - ENH: I18N - Add new Semantic Type - IDENTITY.NI_UK (National Insurance Number (UK))
 - ENH: I18N - Add new Semantic Type - IDENTITY.PERSONNUMMER_SE (Personal identity number (Sweden))
 - ENH: Add new Semantic Type - IMEI (15 digit CHECKDIGIT.LUHN with header signal)
 - ENH: Bump google phonenumber
 - INT: Some code cleanups

### 13.3.0
 - BUG: ISO 639-1 incorrectly switched with ISO 639-2
 - ENH: Add Documentation tag to plugins file to document the Semantic Type (also display in Web)

### 13.2.0
 - ENH: Improve DIRECTION detection - Support InterCardinal Full names (NORTHEAST|NORTHWEST|SOUTHEAST|SOUTHWEST) (Recall - 93% -> 97%)
 - ENH: Add new Semantic Type - INDUSTRY_CODE.NAICS

### 13.1.1
 - BUG: Add missing file

### 13.1.0
 - ENH: Detect longs masquerading as doubles
 - ENH: Improve DIRECTION detection - NB/SB/EB/WB (and friends) (Recall - 66% -> 93%)
 - ENH: Improve AGE, MONTH.DIGITS, ZIP+4 detection

### 13.0.3
 - ENH: If we have a Long type and no Semantic Type detected, exclude outliers (using density-based clustering) and re-analyze
 - ENH: ISO 3166-2 insist on the presence of some signal from the header
 - ENH: Improve PERSON.AGE detection
 - ENH: Bump google phonenumber

### 13.0.2
 - ENH: Improve Web UI
 - ENH: Bump google phonenumber
 - ENH: Bump slf4jAPI
 - BUG: Fix command line invocations in the README

### 13.0.1
 - ENH: Improve performance of STREET_NUMBER (Recall - 82% -> 97%)

### 13.0.0
 - **ENH:** Library is now targetting Java 11+, use 12.X if you still require Java 8 support
 - ENH: Bump logback-classic
 - ENH: Support associated .options file directly (speeds up reference test run x3)

### 12.10.3
 - ENH: Improve PERSON.AGE, PERSON.AGE_RANGE, ADDRESS_FULL_EN, STREET_ADDRESS_EN detection
 - INT: Reimplement WordProcessor()
 - INT: Bump gradle to 8.0.2

### 12.10.2
 - ENH: Improve Gender(en), Race(en), and Age Range detection
 - ENH: Improve CompanyName (en) detection

### 12.10.1
 - BUG: Introduced issue with 12.10.0  - null not recognized when NULL_AS_TEXT set to false

### 12.10.0
 - ENH: Add new Semantic Type - PERSON.AGE_RANGE (en, es, fr, it, nl, pt)
 - ENH: Improve GUID detection
 - ENH: I18N - added wijk, wijknaam, buurt, buurtnaam as synonyms for CITY (nl)

### 12.9.4
 - ENH: Add simple web interface
 - INT: Update build to include examples (via composite build)

### 12.9.3
 - ENH: Add support for en-NL (along with existing support for nl-NL)
 - ENH: If the header looks good then do one more pass with the worst invalid removed (significant uptick in detection - ~1%)
 - ENH: Move getDefaultAnalysis from DriverUtils to TextAnalyzer (now a supported interface)
 - ENH: I18N - minor tweaks to Colombian Municipalities, airlines(en), titles(en), colors(en), countries(en), countries(nl)
 - ENH: Bump google phonenumber

### 12.9.2
 - BUG: Test Suite does not run cleanly when default locale is other than en-US (Issue #28)

### 12.9.1
 - ENH: Extend validation examples and allow case independent matching in PluginDefinition.findByQualifier()
 - ENH: I18N - Dutch - Add FRYSLÂN to the list of valid provinces
 - ENH: I18N - Colombian - Add a set of municipalities without diacritical marks
 - ENH: Improve STREET_MARKER_EN detection

### 12.9.0
 - INT: Improve count validation
 - ENH: Improve STREET_NAME_EN detection
 - ENH: Allow for hyphens in Aadhaar (also fix spelling)
 - BUG: Mismatched counts - Long Semantic Type detected incorrectly and backing out to Long
 - BUG: Mismatched counts - Semantic Type detected and Outliers present and overlapping in case

### 12.8.4
 - ENH: Improve NAME.FIRST_LAST detection
 - ENH: Add count validation to CLI (and use it to validate code against semantic-types suite)
 - BUG: Issue with Signed Double with grouping and exponent and invoking getResult() multiple times
 - BUG: Mismatched counts - Enums with outliers
 - BUG: Mismatched counts - RegExp Semantic Types with invalid entries
 - BUG: Mismatched counts - LocalDate(yyyy) with "0" entries
 - BUG: Mismatched counts - Long Semantic Type detected incorrecly and backing out to Double
 - INT: Bump gradle to 8.0

### 12.8.3
 - BUG: MinLength wrong when only have nulls and empty strings
 - BUG: Blank counts wrong when using RecordAnalyzer with prior Semantic Information
 - BUG: Mismatched counts (matchCount) for Semantic Types (Issue 25)

### 12.8.2
 - BUG: Blank counts wrong when re-analyzing (Issue #24)

### 12.8.1
 - BUG: Counts do not match for some Finite types - including GENDER.TEXT_EN (Issue #23)

### 12.8.0
 - ENH: I18N - Dutch - Add PERIOD.QUARTER support
 - ENH: I18N - Dutch - Improve PostCode detection, Province detection, Municipality Code detection
 - ENH: Date detection now catches 'MMMM, yyyy'
 - ENH: Impove LocalDate(yyyy) detection
 - CLI: Add ability to set quote char from CLI

### 12.7.1
 - ENH: Bump google phonenumber
 - ENH: Make examples depend on the latest major version
 - BUG: Add missing Java file for MUNICIPALITY_CODE_NL

### 12.7.0
 - ENH: I18N - Add new Semantic Type - STATE_PROVINCE.MUNICIPALITY_CODE_NL - Dutch Municipality Code
 - ENH: I18N - Improve Gender detection in Dutch - now also match 'VROUW' and 'MAN'
 - ENH: I18N - Improve Nationality, Company Name, and City detection in Dutch
 - ENH: I18N - Improve Department detection in Colombian
 - ENH: For a finite type with a good header, attempt to analyze removing the worst invalid entry (hoping to remove N/A, Not Present, All, etc)
 - ENH: Improve mini CLI - now supports --locale, --verbose
 - ENH: Allow longtitude as a mispelling of longitude (occurs 4 times in VizNet examples)

### 12.6.5
 - ENH: Restructure some examples to be stand-alone
 - ENH: Add simple CLI example
 - ENH: Bump versions

### 12.6.4
 - ENH: I18N - Add new Semantic Type - COLOR.TEXT_NL (Dutch)
 - ENH: I18N - Detect COMPANY_NAME in Dutch
 - ENH: Bump Jackson

### 12.6.3
 - ENH: I18N - Add new Semantic Type - IDENTITY.VAT_NL (Dutch)
 - ENG: I18N - Add new Semantic Type - IDENTITY.BSN_NL (Burger Service Nummer)
 - ENH: I18N - Improve detection of DAY.DIGITS and LocalDate(yyyy) in Spanish
 - ENH: I18N - Add 'ciudad' to detect CITY in Spanish

### 12.6.2
 - BUG: Fix typo in JSON file - introduced in 12.6.1

### 12.6.1
 - ENH: I18N - Detect PERSON.AGE in Dutch
 - ENH: I18N - Detect COUNTRY.ISO-3166-2, COUNTRY.ISO-3166-3 in Dutch
 - ENH: I18N - Detect MONTH.DIGITS/DAY.DIGITS in Dutch & Spanish
 - ENG: I18N - Add new Semantic Type - COUNTRY.TEXT_NL (akin to COUNTRY.TEXT_EN)

### 12.6.0
 - ENH: I18N - Add new Semantic Type - STATE_PROVINCE.MUNICIPALITY_NL - Dutch Municipality
 - ENH: I18N - Add new Semantic Type - STREET_NAME_BARE_NL (akin to STREET_NAME_BARE_EN)
 - ENG: I18N - Add new Semantic Type - NATIONALITY_NL (akin to NATIONALITY_EN)
 - ENH: I18N - Add Telefoon as a TELEPHONE header for Dutch
 - ENH: I18N - Add a set of the most common Dutch first names
 - ENH: I18N - Add 'plaats/woonplaats' to detect CITY in Dutch
 - ENH: I18N - Improve FREE_TEXT detection in Dutch
 - ENH: I18N - Detect STREET_NUMBER in Dutch

### 12.5.1
 - ENH: Improve detection of STREET_NUMBER and STREET_ADDRESS_BARE_EN
 - ENH: For RecordAnalyzer loop if any new Semantic Type detected (hopefully we pick up others on a subsequent pass)
 - ENH: Bump google phone number library

### 12.5.0
 - ENH: Change plugin interface to enable counting of detection entries (and use to improve Last Name detection, also add tests)
 - ENH: Improve set of recognized Street Markers

### 12.4.1
 - ENH: If using linear (not bulk) then if we have not detected a Semantic Type try replaying accumulated set in Bulk mode this has the potential to pick up entries where the first <n> (by default 20) are misleading.
 - ENH: Bump google phone number library

### 12.4.0
 - ENH: Add new Semantic Type - STREET_ADDRESS3_EN (Third line of an address)
 - ENH: Continue to improve address detection

### 12.3.3
 - ENH: Bump google phone number library

### 12.3.2
 - BUG: Stop double-barreled last names with spaces from preempting NAME.FIRST_LAST

### 12.3.1
 - INT: Add example for Record Mode to README and examples (ModeRecord, cf. ModeStreaming, ModeBulk)

### 12.3.0
 - ENH: Change return value of RecordAnalyzer

### 12.2.2
 - ENH: Reimplement CITY as a Java plugin (F1-Score from 97% to >99%)

### 12.2.1
 - ENH: Bump google phone number library
 - ENH: Bump slf4j
 - ENH: Change getHeaderConfidence to allow negative Header indication (i.e. < 0)

### 12.2.0
 - ENH: Improve Last Name detection

### 12.2.0
 - ENH: Improve RecordAnalyzer interface.
 - ENH: Add new Semantic Type - STREET_NUMBER (Street Number).
 - ENH: Improved Name detection (NAME.*)
 - ENH: Add the ability to specify known Semantic Types as part of the supplied Context (see withSemanticTypes)
 - BUG: Weekday abbreviations should also honor the NO_ABBREVIATION_PUNCTUATION feature (impacted locale CA)

### 12.1.1
 - ENH: Initial version of the new RecordAnalyzer interface (and use it from the CLI)

### 12.1.0
 - ENH: Add new Semantic Type - STREET_NAME_BARE_EN (Street Name without a Marker (e.g. no ST, RD, LN, ...))
 - ENH: More Address detection improvements
 - ENH: Bump google phone number library
 - ENH: Bump slf4j
 - INT: Bump gradle to 7.6

### 12.0.9
 - ENH: Revamp Address detection - now split into FULL_ADDRESS_EN, STREET_ADDRESS_EN, STREET_ADDRESS2_EN, STREET_NAME_EN
 - ENH: Content Format Detection - accept JSON that uses single quotes as opposed to the standard
 - ENH: Bump Jackson & slf4j
 - BUG: STREET_ADDRESS_EN - should trim() before checking length

### 12.0.8
 - ENH: Improve NAME.LAST_FIRST to cope with multiple spaces - e.g., "DAVIS,  RICHARD M"
 - ENH: Bump google phone number library
 - ENH: Bump Jackson

### 12.0.7
 - ENH: Default NULL_AS_TEXT to off. Note: by default the CLI enables this.
 - ENH: Improve detection for dates of the form MMM&lt;sep&gt;YYYY or MMMM&lt;sep&gt;YYYY

### 12.0.6
 - ENH: Improve NAME.MIDDLE_INITIAL recall

### 12.0.5
 - INT: Remove support for Rule generation (moved to separate utility)

### 12.0.4
 - ENH: Improve NON_LOCALIZED Double detection
 - ENH: Improve NAME.FIRST_LAST to cope with multiple spaces - e.g., "Rodney D.  Jones"
 - ENH: Support Date formats like "April,2015"

### 12.0.3
 - BUG: Missed a file on checkin

### 12.0.2
 - ENH: Reimplement INDUSTRY_EN as a Java plugin (Recall now at 90% against Suite, previously at 5%)
 - ENH: Add support for ignoreList so we can ignore things like 'OTHER' and 'N/A' on lists
 - ENH: Add Correlation data from Test Suite to inform likelihood of a semantic match
 - ENH: PERSON.AGE plugin should support DOUBLE as well as LONG
 - ENH: Improve COMPANY_NAME/COLOR detection
 - BUG: Fix up backing out from Semantic Types that have a base type of Double

### 12.0.1
 - BUG Fix example in README

### 12.0.0
 - **ENH:** Incompatible changes.
	isLogicalType() -> isSemanticType()
	getTypeQualifier() has split into getTypeModifier() and getSemanticType().  getSemanticType() is only valid if isSemanticType() is true.
	getTypeModifier() describes modification to the Base Type (for example SIGNED on base type LONG or DOUBLE, or YYYY-MM-dd for LOCALDATE)
	JSON output has also changed accordingly (Use analyzer.configure(TextAnalyzer.Feature.LEGACY_JSON, true) to revert to legacy JSON - pre 12.X)
 - ENH: Bump google phone number library
 - ENH: Add new Semantic Type - DIRECTION (Cardinal Direction)
 - INT: Improved test coverage on Histogram/Quantile support
 - BUG: Should preserve uniqueness (at least to the extent we can) on merge()
 - BUG: Numerous fixes for Histograms and Quartiles (especially around ugly data - e.g. trailing minus), also improve performance
 - BUG: Add UTF8 encoding option - just in case anyone builds on Windows

### 11.0.7
 - ENH: Add cutpoints on Histogram entries returned (as well as the BaseType cuts)
 - BUG: Various fixes to Histogram details
 - BUG: Move 0's in date from outliers to invalid (also fix matchCount)
 - INT: Much improved test coverage on Histogram support

### 11.0.6
 - ENH: Output histograms (10 wide) in JSON
 - ENH: Add 'faker' support - useful for testing
 - ENH: Add meaningful samples for COMPANY_NAME
 - CLI: Switch to return null if we see no data in CSV
 - BUG: Make sure to clamp values in LocalTime and OffsetDateTime since quantiles are only so accurate and we need to return a valid value

### 11.0.5
 - BUG: Fix Exception related to quantile determination when using LocalTime
 - BUG: Fix histogram bucketing (and support histograms once cardinality blown)

### 11.0.4
 - ENH: Change interface for getCardinalityDetails() from SortedMap to NavigableMap
 - ENH: Add support for histograms. See getHistogram().

### 11.0.3
 - ENH: Performance is slow when using trainBulk() and the counts are large and statistics are being generated

### 11.0.2
 - ENH: Change cardinalityDetails to return a SortedMap (Issue #19)

### 11.0.0
 - **ENH:** Behavior has changed with 11.0.0  - there is now a distinction between outliers and invalid entries.
      For example, with 1, 2, 3, 7, 8, 12, 9, 2, 23, BOGUS, 14 - 'BOGUS' is now an Invalid entry as opposed to an Outlier.
      See getInvalidCount() and getInvalidDetails().

	  **Note: Data Signatures have changed.**
 - ENH: Bump logback-classic

### 10.3.0
 - ENH: I18N - Add a few more Colombian Municipalities to improve detection
 - ENH: Add support for retrieving the path to the Trace file (Issue #17)
 - ENH: Bump commons-text, slf4j-api
 - BUG: Serialization now works with Quantiles
 - BUG: Some doubles were not capturing min/max/topK/bottomK (if logical type detected)
 - INT: Run tests in parallel (also speed up date tests - or more accurately only do 10% of locales each run)

### 10.2.1
 - ENH: Add new Semantic Type - PERSON.RACE_ABBR_EN

### 10.2.0
 - ENH: Add support for quantile determination. See getValueAtQuantile, getValuesAtQuantiles, and get/setQuantileRelativeAccuracy().
 - ENH: CLI - Add --json to output true JSON from command line
 - ENH: Bump google phone number library
 - INT: Remove a set of previously deprecated methods

### 10.1.0
 - ENH: Add new Semantic Type - PERSON.MARITAL_STATUS_EN

### 10.0.1
 - ENH: Improve PERSON.RACE_EN detection
 - ENH: Improve COLOR.TEXT_EN detection
 - ENH: Reimplement POSTAL_CODE.POSTAL_CODE_CA as a Java plugin (Recall now at 100% against Suite)

### 10.0.0
 - **ENH:** New Feature.NULL_AS_TEXT is enabled by default ("Null" (also No Data) - will be treated as a NULL record)
 - **BUG:** Changed PERSON.RACE to PERSON.RACE_EN (not backward compatible)
 - **INT:** Plugin definition has changed with 10.0 (getConfidence() now receives the full context, not just the StreamName)
 - ENH: Add new Semantic Type - LANGUAGE.ISO-639-1 - three letter country code
 - ENH: Add new Semantic Type - NAME.SUFFIX - Name Suffix (e.g. I, II, JR., ...)
 - ENH: Add new Semantic Type - COLOR.TEXT_EN - Color Name
 - ENH: Reimplemnent EIN as a Java plugin
 - ENH: I18N - Add new Semantic Type - STATE_PROVINCE.SUBURB_AU - Australian Suburb (generalize BloomFilter support)
 - ENH: I18N - Add new Semantic Type - STATE_PROVINCE.MUNICIPALITY_CO - Colombian Municipality
 - ENH: I18N - Add new Semantic Type - STATE_PROVINCE.DEPARTMENT_CO - Colombian Department
 - ENH: I18N - Add new Semantic Type - POSTAL_CODE.POSTAL_CODE_CO - Colombian Postal Code
 - ENH: I18N - Add new Semantic Type - COUNTRY.TEXT_ES - Country (Spanish)
 - ENH: Add a wrapper task to indicate the version of Gradle required (Issue #11)
 - ENH: Add ability to create Normalized file to capture both words with and without diacritical marks
 - ENH: Improve detection of DAY.DIGITS and MONTH.DIGITS (implemented in Java as opposed to regexp)
 - ENH: Improve MiddleName/MiddleInitial detection
 - ENH: Improve Street Address detection
 - ENH: Improve Company Name detection (business/organization are now synonyms)
 - ENH: Improve Person Age detection
 - ENH: Improve Person Race detection
 - ENH: Improve Person Gender detection
 - ENH: Improve english Country detection
 - ENH: Improve NAME.SUFFIX detection
 - ENH: Add support for pluginOptions
 - ENH: Bump slf4j (2.0.1) and logback-classic (1.4.1)
 - ENH: Bump google phone number library
 - ENH: Bump jackson
 - BUG: Should have cleansed and trimmed input before checking it in isValid()
 - BUG: Samples returned by VAT routines were typically not Valid
 - BUG: URL plugin was not trim()'ing input
 - BUG: Fix bug in Address2 if stream name not found in list of all field names

### 9.1.1
 - BUG: Don't output totalNullCount, totalBlankCount, totalMinLength, totalMaxLength if they are unset

### 9.1.0
 - INT: No change - other than bumping the version number.

### 9.0.21
 - ENH: if totalCount is set (i.e. != -1) then output the total* fields.

### 9.0.20
 - ENH: Add support for Totals (i.e. the ability to set BlankCount, NullCount, Min/Max Value, Min/Max Length, Mean/SD for the entire set)

### 9.0.19
 - BUG: fta-core should have been declared as an API (not implementation) dependency of fta

### 9.0.18
 - BUG: DateTimeParser with DateResolutionMode.Auto works as MonthFirst for all locales (Issue #10)
 - ENH: Minor improvements to Name detection
 - ENH: Bump google phonenumber

### 9.0.17
 - ENH: Add new Semantic Types - PERIOD.YEAR_RANGE, AIRLINE.IATA_CODE, AIRLINE.TEXT_EN
 - ENH: Re-implement PERIOD.QUARTER in Java (was RegExp) to improve Recall (Sensitivity)
 - ENH: Cleanse strings by replacing left/right quotes by ' to improve list matching
 - ENH: I18N - plz is a synonym for postleitzahl in Germany
 - ENH: I18N - Improve non-English detection of CITY
 - ENH: I18N - Improved Precision of some non-English Postcode detection
 - BUG: Pattern for YES_NO was [0|1] should have been (0|1)

### 9.0.16
 - ENH: I18N - Japanese - add Prefecture names without 'Prefecture' to list
 - ENH: I18N - Mexico - add State names without diacritic marks
 - ENH: I18N - Mexico - add locale es-MX for Mexican State names
 - ENH: I18N - Add new Semantic Type - STATE_PROVINCE.MUNICIPALITY_MX (Mexican Municipality)
 - BUG: Add a set of missing signatures

### 9.0.15
 - ENH: Support YR as a synonym for Year
 - ENH: Improve US County detection
 - ENH: Improve City detection (fix a set of false positives)
 - ENH: I18N - Add support for STREET_ADDRESS for bg, ca, da, de, es, fi, fr, hr, it, lv, nl, pl, pt, ro, ru, sk

### 9.0.14
 - ENH: I18N - Add new Semantic Types - STATE_PROVINCE.COUNTY_IE, STATE_PROVINCE.PROVINCE_NAME_IE
 - ENH: Bump google phonenumber
 - INT: Bump gradle to 7.5

### 9.0.13
 - ENH: Date processing - add detection for numeric only detection of dates (e.g. 2022, 20220712, 202207121830, 20220712183000) - default on, disable via withNumericMode(false)

### 9.0.12
 - ENH: Address Issue #7 - Allow setting secondary (actually an infinite number) locale for DateTimeParser determination
 - ENG: Support for 9 digit Zip + 4's

### 9.0.11
 - ENH: I18N - zip is a synonym for postleitzahl in Germany
 - ENH: Improve US County detection by adding some common misspellings
 - ENH: CLI - field names should be trimmed
 - BUG: CLI - if delimiter is specified, need to turn off autodetection

### 9.0.10
 - ENH: Add new Semantic Type - PERSON.RACE

### 9.0.9
 - ENH: I18N - Add FREE_TEXT support for Bulgarian, Catalan, Dutch, Portuguese, and Russian
 - ENH: I18N - Improve Japanese date detection
 - ENH: I18N - Improve Bulgarian date support - in particular support "14.02.2017г."
 - ENH: I18N - Add new Semantic Types - STATE_PROVINCE.INSEE_CODE_FR ("French Insee Code (5 digit)")
 - ENH: CLI - support --skip <n> to skip the first <n> lines
 - BUG: Handle dates of the form - "1995-02-28Z", will return "yyyy-MM-dd'Z'" (which can be used with LocalDate.parse) - used to return "yyyy-MM-ddZ"
 - BUG: Fix IDENTITY.VAT_GB - mixup with UK vs GB (locale is en-GB)

### 9.0.8
 - ENH: CLI - set totalCount in Bulk mode
 - ENH: I18N - Add FREE_TEXT support for Spanish and Italian
 - BUG: I18N - Chinese detection should be enabled for language 'zh' (not 'cn')
 - BUG: I18N - Date (Pass 2) was not being detected for 2015/9/9 (e.g. single digit day), picked up in Pass 3 for anything other than Japanese/Chinese

### 9.0.7
 - BUG: Date detection is sometimes overly aggressive, for example, Q4 2008-12
 - ENH: Add new Semantic Types - PERIOD.QUARTER, PERIOD.HALF
 - ENH: Add new Semantic Type - IDENTITY.VAT_<COUNTRY> (Countries supported AT, ES, FR, IT, PL, UK)
 - ENH: Add new Semantic Type - STATE_PROVINCE.PROVINCE_NAME_EC (Ecuador)

### 9.0.6
 - BUG: Address incorrectly collapsing time formats when both date and time needed collapsing (See Issue #6)
 - ENH: Bump google phonenumber

### 9.0.5
 - ENH: Add new Semantic Type - PERSON.AGE
 - ENH: I18N - Improve French Post Code detection

### 9.0.4
 - ENH: I18N - Improve Coordinate detection in Dutch
 - ENH: Improve consistency wrt to quoting ',' in returned Date formats

### 9.0.3
 - ENH: I18N - Initial support for Latvian
 - ENH: I18N - Enhance Croatian
 - ENH: I18N - Be more lenient for Dutch Post Codes
 - ENH: I18N - Improve French Department detection
 - ENH: Add new Semantic Types - IDENTITY.EIN_US ("Employer Identification Number (US)"), STATE_PROVINCE.COMMUNE_IT ("Italian Commune")
 - ENH: Add support for IBANs with embedded spaces
 - ENH: Reimplemnent SSN as a Java plugin
 - ENH: Improve Job Title detection
 - BUG: Switch GENDER.TEXT_CN to GENDER.TEXT_ZH (and change localeTag to 'zh')
 - INT: Switch to jakarta.mail from javax.mail
 - INT: CLI - Fixup dependencies

### 9.0.2
 - INT: Switch to Gradle 7 way of versioning stuff

### 9.0.1
 - ENH: I18N - Initial Greek support (Dates, Yes/No, PhoneNumbers)
 - ENH: I18N - Improve Chinese support
 - ENH: I18N - Add new Semantic Type - COUNTRY.TEXT_DE
 - BUG: Outliers were not being correctly tracked for late detected RegExp types

### 9.0.0
 - **Plugin definition has changed with 9.0 as has DateTimeParser.ofPattern.**
  - **ENH:** New Feature.NO_ABBREVIATION_PUNCTUATION is enabled by default**
 - ENH: I18N - Improve date detection in locales with abbreviations with periods for short-months (and AM/PM strings) (e.g. en-CA, en-AU) - see Feature.NO_ABBREVIATION_PUNCTUATION
 - ENH: I18N - Add support for FREE_TEXT in french
 - ENH: FREE_TEXT implies some level of uniqueness - change to insist on > .1 uniqueness
 - ENH: I18N - Add new Semantic Types - STATE_PROVINCE.COUNTY_HU, CHECKDIGIT.UPC, COLOR.HEX, HASH.SHA1_HEX, HASH.SHA256_HEX
 - ENH: Bump google phonenumber
 - ENH: Add support for MAC ADDRESS detection with minus as well as colon
 - ENH: Extend Shape support from 40 to 65 before declaring too long
 - BUG: I18N - Year in french has an acute!
 - BUG: I18N - Year in Catalan was wrong
 - BUG: I18N - Fix issue with Chakma (locale ccp), issue related to surrogate pairs
 - INT: I18N - Add --abbreviationPunctuation to CLI

### 8.0.30
 - ENH: Improve support for dates with full weekdays (EEEE)
 - BUG: Fix issue with ZoneDateTime or OffsetDateTime and no valid data

### 8.0.29
 - ENH: Cope with Date Format - "Fri 08 Jan 2010 15:11:16 +0000"

### 8.0.28
 - ENH: Bump google phonenumber
 - ENH: Decrease false positives on NAME.LAST_FIRST

### 8.0.27
 - INT: CLI - Move to separate jar
 - INT: Improve examples and README

### 8.0.26
 - ENH: I18N - Add new Semantic Type - POSTAL_CODE.BG
 - ENH: I18N - Improve year detection in Russian/Finnish/Danish
 - ENH: I18N - Add Bulgarian Gender support
 - ENH: I18N - Add Russian/Japanese LATITUDE/LONGITUDE detection

### 8.0.25
 - ENH: I18N - FREE_TEXT - improve German samples
 - ENH: I18N - Add new Semantic Types - POSTAL_CODE.POSTAL_CODE_UY, POSTAL_CODE.POSTAL_CODE_MX
 - BUG: Do not die if tracing is on and the fieldName is extremely long
 - INT: CLI - Support setting Trace options
 - INT: Support adjusting Max Columns (Univocity option) from the command line
 - INT: Improve code coverage

### 8.0.24
 - ENH: I18N - Add FREE_TEXT support for German
 - ENH: Split COORDINATE_PAIR.DECIMAL into COORDINATE_PAIR.DECIMAL (no parens) and COORDINATE_PAIR.DECIMAL (parens)
 - ENH: Do not label as COORDINATE.PAIR_DECIMAL without some signal from the header
 - ENH: Improve detection of COORDINATE_PAIR.DECIMAL

### 8.0.23
 - INT: Change logger name from fta to com.cobber.fta
 - INT: Suppress testNG logging when executing tests
 - INT: Improve test coverage
 - BUG: Fix issues with TimeZoneOffsets with seconds (e.g. GMT+08:09:20)

### 8.0.22
 - ENH: Add StrictMode to DateTimeParser

### 8.0.21
 - ENH: I18N - Add new Semantic Type - POSTAL_CODE.POSTAL_CODE_SE (uses Bloom Filter)
 - ENH: I18N - Improve COORDINATE_PAIR.DECIMAL, COORDINATE.LATITUDE_DECIMAL, and COORDINATE.LONGITUDE_DECIMAL detection rate (especially in Western Europe)
 - ENH: I18N - Update Japanese Postal Codes
 - ENH: I18N - Improve detection of non-localized doubles
 - ENH: Update plugin format to explicitly indicate the type of the plugin ('java', 'list', or 'regex')
 - ENH: Add support for a sample list for regex plugins (enables reasonable support for nextRandom())
 - ENH: Bump Jackson
 - BUG: I18N - Failed to handle UTF-8 minus sign on Exponents (e.g. locale "nn")

### 8.0.20
 - ENH: I18N - Improve STREET_ADDRESS_EN and STREET_ADDRESS2_EN detection rate (especially UK)
 - INT: I18N - More testing on non-localized doubles
 - INT: Add support for plugin validation from the command line

### 8.0.19
 - ENH: I18N - Gender support for Romanian
 - ENH: I18N - Improve support for Middle Name (cope with a blend of initials and names)
 - ENH: I18N - Improve JOB_TITLE detection
 - ENH: I18N - Improve Italian Province detection
 - ENH: I18N - Improve Yes/No detection for Bulgarian, Catalan, Finnish, and Slovakian
 - ENH: I18N - Improve 4 digit year detection for a set of European countries
 - BUG: I18N - Fix issue with non-localized Doubles (also added modifier to Double - NON_LOCALIZED) - also more tests

### 8.0.18
 - ENH: I18N - Improve Slovakian support
 - ENH: I18N - Improve French-Canadian support
 - ENH: I18N - Move "regExpsToMatch", "regExpReturned", "isRegExpComplete" to be per locale
 - ENH: I18N - Support a concept of a non-localized Double (e.g. for example latitude which commonly does not use locale specific decimal separator)
 - ENH: I18N - Gender support for Croatian, Catalan, Swedish and improve French
 - ENH: I18N - Initial support for Romanian
 - ENH: I18N - Add new Semantic Type - STATE_PROVINCE.REGION_IT
 - ENH: Bump google phonenumber

### 8.0.17
 - ENH: I18N - Add Gender support for Finnish, Polish
 - ENH: I18N - Other minor improvements for Danish, Finnish, Polish
 - ENH: Switch boolean setter of TextAnalyzer to configure(Feature) - deprecate old way
 - ENH: Initial support for contentFormat (disabled by default)

### 8.0.16
 - INT: I18N - Rewrite Gender plugin - improve I18N support

### 8.0.15
 - ENH: Improve header detection for Telephone
 - INT: Improve test coverage
 - BUG: Viznet - Do not generate bogus formats if date format switches, e.g. some dd/MM/yyyy then a set of yyyy-MM-dd - see TestDates.mixedDates()
 - BUG: Viznet - Do not introduce grouping if already a Logical Type - see TestLongs.testLongLogicalType()
 - BUG: Viznet - Fix date parsing for 02/08/2017 08:30:01 AM +0000 (ambiguous day & month).
 - BUG: Viznet - Fix dates masquerading as longs (with errors)

### 8.0.14
 - ENH: Add new Semantic Types - COORDINATE.EASTING, COORDINATE.NORTHING
 - BUG: Viznet - Fix NumberFormatException with space padded years - see TestDate.fiscalYear()
 - BUG: Viznet - Fix Exception with "88-0828S7" and many blanks - see RandomTests.strange()
 - BUG: Viznet - Another nasty -0828S7" - see RandomTests.viznet3()
 - BUG: Viznet - Another nasty '2018-06-26T15:27:50.' - see DetermineDateTimeFormatTests.unusualT()

### 8.0.13
 - ENH: I18N - Improve Spanish support (Postal Codes + Year + Yes/No)
 - BUG: Fix NPE on "05/09/2014 02:00:00 AM +0000" (rework PassTwo)

### 8.0.12
 - ENH: I18N - Add support for Brazilian municipalities - STATE_PROVINCE.MUNICIPALITY_BR
 - ENH: I18N - Improve gender support in Italian

### 8.0.11
 - ENH: Add serialize(), deserialize(), merge(), and apply() to DateTimeParser (+ tests)
 - ENH: Add fluid API support for config on DateTimeParser (deprecate old contructors)
 - BUG: Minor changes to improve DateTimeParser determination

### 8.0.10
 - ENH: Improve date documentation to cover training as well as simple format retrieval from a single sample

### 8.0.9
 - INT: Restructure plugins.json to ease localization

### 8.0.8
 - ENH: I18N - Improve first-name detection for German
 - ENH: I18N - Add POSTAL_CODE.POSTAL_CODE_DE
 - ENH: Add support for Finite Types on Longs

### 8.0.7
 - ENH: Bump google phonenumber
 - ENH: Improve detection of First Names in French/Spanish
 - ENH: Add a set of common African last names
 - ENH: Add a set of common Mexican first names
 - ENH: Minimal mod to header regexp to grab more lat/longs
 - ENH: Enhanced Gender to handle Woman/Man as well as Female/Male (and localized versions)
 - BUG: RegExp header for City was a little aggressive (picked up 'Subject Ethnicity')

### 8.0.6
 - ENH: I18N - Support localized versions of Yes/No for booleans e.g. Italian Si/No, French Oui/Non
 - ENH: Support localized versions of YEAR/DATE
 - ENH: I18N - Be more forgiving on Gender detection (also for non-English), also Sexo as a synonym for Gênero in Portuguese

### 8.0.5
 - ENH: Add a couple more countries to reference list (ESWATINI, NORTH MACEDONIA)
 - ENH: Improved documentation for asJSON

### 8.0.4
 - ENH: Add support for distinctCount

### 8.0.3
 - ENH: Improve performance for constant valued columns (x20)
 - ENH: Set Total Count when using command line tool
 - BUG: Fix variance/standardDeviation on merged analyses
 - INT: Improve test coverage on merge() especially when cardinality blown
 - INT: Update Roadmap

### 8.0.2
 - ENH: For Doubles & Longs print the min/max/top/bottom in approximately the format of the incoming data (including localization)
 - BUG: merge() could not cope with cases where the cardinality was large and the top/bottom values were not in the captured set
 - INT: Improve test coverage on merge()

### 8.0.1
 - BUG: Fix Serialization issue for tiny datasets
 - BUG: Fix issue for non-US locales where the returned TextAnalyzer was not in the locale of the merged entities
 - INT: Improve test coverage on TextAnalyzer serialize()/deserialize()/merge() in particular in non-default locales

### 8.0.0
 - **Data Signatures for 8.X are not the same as prior releases, see details below**
 - ENH: Add Serialize()/Deserialize()/merge() to TextAnalzer
 - ENH: Min/Max on the JSON output now outputs long/double values using the detection locale.
 - ENH: Bump google phonenumber
 - ENH: Add NumericWidening to AnalysisConfig object
 - BUG: Fix issue with data signatures not being consistent (due to order of Cardinality map not being constant)
 - BUG: Fix issue with mean/standard deviation being incorrectly calculated when using trainBulk (see TestBulk.bulkLong())
 - BUG: Fix issue with min/max not being printed with enough precision (see TestDoubles.verySmall())

### 7.0.5
 - ENH: Fix detection of '06/Jan/2008 15:04:05 -0700' and 'Mon, 02 Jan 2006 15:04:05 -0700'
 - INT: Cleanup a couple of tests

### 7.0.4
 - ENH: Bump Jackson due to CVE-2020-36518
 - ENH: Fix detection of '2014:3:31' and '2014:03:31'

### 7.0.3
 - ENH: Improve Date detection - coping with additional variable length components, e.g. the following now pass:
	- "May 8, 2009 5:57:51 PM", "oct. 7, 70", "2014/4/8 22:05", "2014/04/2 03:00:51", "2014:4:8 22:05", "2014:04:2 03:00:51", "2014:4:02 03:00:51"
 - ENH: DateTimeParser.determineFormatString() - Cope with single quotes in input string - generated an error previously, e.g., "oct. 7, '70"

### 7.0.2
 - ENH: Improve FREE_TEXT detection

### 7.0.1
 - BUG: Fixup plugin.json (qualifier was DOY.FULL_<LOCALE> but was actually returning DAY.FULL_<LOCALE>)
 - BUG: Fixup plugin.json (qualifier was DOY.ABBR_<LOCALE> but was actually returning DAY.ABBR_<LOCALE>)
 - ENH: Improve JavaDoc

### 7.0.0
 - **ENH:** Major version change as Plugin interface has changed (only impacts Plugin authors).
 - BUG: Fix issue related to outlier counting when using Bulk mode
 - BUG: COUNTRY_EN plugin was not honoring threshold from plugins file

### 6.0.9
 - INT: Cleaning up FREE_TEXT stuff (would like to run it without mandatory headers)

### 6.0.8
 - ENH: Add Semantic Type (FREE_TEXT) - captures Descriptions, Notes, Comments, ...

### 6.0.7
 - BUG: Fix replay
 - INT: Cleaning code, noqualifier option to JavaDoc
 - ENH: Tweak longitude, latitude header match string

### 6.0.6
 - ENH: Bump slf4j-api
 - BUG: Fix issue where setMaxInputLength could only be increased not decreased

### 6.0.5
 - ENH: Improve JavaDoc

### 6.0.4
 - ENH: Add support for replaying FTA trace files.

### 6.0.3
 - BUG: Handle grouping in doubles with Exponents (may never be seen but should not crash)

### 6.0.2
 - ENH: Bump google phone number.
 - INT: Fixup minor issues in README & document Signature details

### 6.0.1
 - INT: No longer need build7.gradle 

### 6.0.0
 - ENH: Split into fta-core (date-detection) and fta-types (Semantic Types)
 - ENH: Bump jackson
 - INT: Bump gradle to 7.X (finally)

### 5.1.24
 - ENH: Add a little more leniency for finite types (especially larger ones)
 - INT: Bump plugins-version, improve Date example

### 5.1.23
 - BUG: Fix NPE when using FTA from DBProfiler (also added test case)
 - INT: Cleaning imports
 - ENH: Broaden set of recognized languages

### 5.1.22
 - ENH: Add Semantic Type - CHECKDIGIT.ISBN
 - ENH: Bump guava.

### 5.1.21
 - ENH: Add Semantic Type - COMPANY_NAME (Header-only detection)
 - ENH: Bump google phone number.

### 5.1.20
 - ENH: Improve Portuguese support. Add Semantic Type - POSTAL_CODE.POSTAL_CODE_PT, STATE_PROVINCE.DISTRICT_NAME_PT.

### 5.1.19
 - ENH: Ensure that all Threshold statements are captured in the plugins file (not the code).  Clarify that the default is 95%.

### 5.1.18
 - ENH: Add Semantic Type - IDENTITY.DUNS (Data Universal Numbering System (Dun & Bradstreet))
 - INT: Add support for generating a set of sample files
 - INT: Fixup interpolation in debugging.

### 5.1.17
 - ENH: Add Semantic Type - POSTAL_CODE.POSTAL_CODE_JA, POSTAL_CODE.POSTAL_CODE_FR

### 5.1.16
 - INT: Add tests for IDENTITY.NHS_UK and IDENTITY.AADHAR_IN
 - BUG: COORDINATE.LONGITUDE_DMS and COORDINATE.LATITUDE_DMS were reversed

### 5.1.15
 - ENH: Add Semantic Type - IDENTITY.NHS_UK (UK)
 - ENH: Bump Google phone number
 - INT: Compress Gender in README

### 5.1.14
 - ENH: Add Semantic Type - IDENTITY.AADHAR_IN (India)
 - INT: Lots of renaming + newInstance already calls initialize - so don't do it again

### 5.1.13
 - ENH: Add Semantic Types - GENDER.TEXT_MS (Malaysia), GENDER.TEXT_CN (China)
 - ENH: Limited support for Chinese Dates
 - INT: printf to log.debug/info/...
 - INT: upgrade version of TestNG

### 5.1.12
 - BUG: totalSamples should have been a long.

### 5.1.11
 - INT: Update copyright year + add a few missing copyrights
 - ENH: Add an example of using the AnalyzerContext

### 5.1.10
 - ENH: Add new Semantic Type IDENTITY.INDIVIDUAL_NUMBER_JA (Individual Number / My Number) (locale ja)
 - ENH: Add new Semantic Type IDENTITY.SSN_CH (AVH / Sozialversicherungsnummer) (locale de-CH, fr-CH ,it-CH)
 - BUG: Fix SSN_FR for folks born overseas and those born in Corsica

### 5.1.9
 - INT: Back down slf4j to latest stable

### 5.1.8
 - ENH: Add new Semantic Type IDENTITY.SSN_FR (locale fr-FR)

### 5.1.7
 - ENH: Switch to sl4j for logging (delay initialization unless debug is on)
 - INT: Fixes courtesy of PMD

### 5.1.6
 - ENH: Add new Semantic Type NAME.MIDDLE, NAME.MIDDLE_INITIAL
 - ENH: If two Semantic Types are equal in score, then break the tie first with the header, then with the priority
 - INT: Use PluginDefinition.findByQualifier instead of new PluginDefinition(), initialize plugins

### 5.1.5
 - ENH: Bump Google phone number
 - ENH: Bump Gradle version
 - ENH: Add support for South African Province Names (STATE_PROVINCE.PROVINCE_ZA, STATE_PROVINCE.PROVINCE_NAME_ZA)

### 5.1.4
 - ENH: Bump Jackson Databind

### 5.1.3
 - ENH: Improve PhoneNumber plugin to support example generation for all locales

### 5.1.2
 - ENH: Improve Industry, TimeZone, and Honorific matching.

### 5.1.1
 - BUG: Name matching was a little greedy.

### 5.1.0
 - BUG: TopK/BottomK ordering is incorrect for non-strings.

### 5.0.9
 - ENH: Add new Semantic Types EPOCH.MILLISECONDS, EPOCH.NANOSECONDS

### 5.0.8
 - ENH: Cope with dates with 9 decimals for the fractional seconds.

### 5.0.7
 - BUG: Cope with null entries in trainBulk when tracing, improve error reporting for rubbish argument to trace.

### 5.0.6
 - ENH: Cope with dates with 7 decimals for the fractional seconds.

### 5.0.5
 - ENH: Cope with dates with 6 decimals for the fractional seconds - e.g. yyyy-MM-dd HH:mm:ss.SSSSSS.

### 5.0.4
 - ENH: missed reference file for IANA Time Zones.

### 5.0.3
 - INT: Cleaning up.
 - ENH: Bump google phone number.
 - ENH: Add new Semantic Type TIMEZONE.IANA - supports IANA (Olson) Time Zones.

### 5.0.2
 - INT: Cleaning up.

### 5.0.1
 - INT: Cleaning up, micro performance improvement

### 5.0.0
 - ENH: Next gen RegExp support (most signatures are unchanged - but there are some differences), also plugin signatures changed

### 4.9.2
 - INT: Reverse order of ChangeLog file :-)

### 4.9.1
 - BUG: Should have specified Charset when reading all reference files!!

### 4.9.0
 - ENH: Minor improvements for performance
 - ENH: Clamp input to 4096 characters (can be widened by invoking setMaxInputLength()) - will change existing signatures unless data is narrower than 4096.
	Note: this also backs out the change in 3.8.2 to optionally externally clamp.

### 4.8.2
 - ENH: Add support for tracing for trainBulk
 - ENH: Add support to externally set the maxLength of a field (if sending in truncated data to FTA)

### 4.8.1
 - ENH: Bump google phone number version

### 4.8.0
 - ENH: LogicalType now implements LTRandom - so you can also call nextRandom() from RegExp plugins
 - ENH: Add some Italian first and Last names
 - ENH: Improve RegExps for COORDINATES, MONTH.DIGITS, DAY.DIGITS
 - ENH: Support isRegExpComplete from pluginDefinition
 - INT: Add test to validate signatures in plugin file
 - BUG: Fix a couplle of signatures

### 4.7.11
 - ENH: Split out performance tests
 - ENH: Improve overall performance by passing around trimmed value

### 4.7.10
 - ENH: Add Semantic Types - STATE_PROVINCE.STATE_BR, STATE_PROVINCE.STATE_NAME_BR, STATE_PROVINCE.PREFECTURE_NAME_JA
 - INT: Improve testing infrastructure, so can run particular groups of tests
 - BUG: Complain if the priority of the user registered plugins overlaps the builtin space (0-2000]
 - ENH: Improve support for Japanese (dates & times, Gender, Prefectures)
 - BUG: Switch GENDER_JP to GENDER_JA and correct language (should have been ja not jp)

### 4.7.9
 - ENH: Add Semantic Types - COORDINATE.LATITUDE_DMS, COORDINATE.LONGITUDE_DMS
 - BUG: Fixed issue where short (i.e. missing leading 0's) Zip codes were not being detected
 - ENH: Refreshed US Zip list with latest from USPS
 - ENH: Add Semantic Type - POSTAL_CODE.ZIP5_PLUS4_US

### 4.7.8
 - ENH: Add tracing support - set via environment variable FTA_TRACE or via setTrace(String) e.g. setTrace("stream=COUNTY,samples=10000")
	Options are:
		enabled=true/false,
		stream=<name of stream> (defaults to all)
		directory=<directory for trace file> (defaults to java.io.tmpdir)
		samples=<samples to trace> (defults to 1000)
 - ENH: Cache ObjectMapper to improve performance

### 4.7.7
 - ENH: Improve the probability of locating a lat/long header
 - ENH: Change handling of plugin retrieval if no Locale specified (effectively defaulting to English), add TestCase
 - INT: Move more stuff into AnalysisConfig

### 4.7.6
 - ENH: Add a couple more street markers
 - ENH: Add Semantic Types - GENDER.TEXT_TR (Turkey)

### 4.7.5
 - ENH: Improve detection of Phone Numbers when we have numeric input
 - ENH: Cope with dates of the form M/YYYY as well as MM/YYYY
 - BUG: Fix bad name detection when all names are of the form 'FIRST M. LAST'

### 4.7.4
 - ENH: Add a set of missing signatures

### 4.7.3
 - ENH: Improve structure signature access (also cache on plugins file)

### 4.7.2
 - ENH: Improve reference list for industries
 - ENH: Improve random samples for Streets
 - ENH: Add new Semantic Type - STREET_ADDRESS2_EN (Second line of an address)
 - ENH: Add 'DISTRICT OF COLUMBIA' as a State name
 - ENH: Improve name support for Finland, and Norway

### 4.7.1
 - ENH: Add new Semantic type INDUSTRY_EN
 - ENH: Improve uniqueness detection, if a field is monotonic increasing or monotonic decreasing then its uniquness is 1!

### 4.7.0
 - ENH: Change isValidSet() on plugins to take an AnalyzeContext not just a stream name
 - NOTE: Bumped to 4.7.0 because preexisting plugins need to be minimally updated

### 4.6.6
 - ENH: Add support for generating 'random' examples of the CheckDigits (e.g. IBAN and friends)
 - ENH: nextRandom() on names now only returns names without spaces (somewhat less random :-))
 - ENH: Improve testing of nextRandom()
 - BUG: Fix issue in Japanese Gender support
 - BUG: Fixup typos in name of a couple of Canadian provinces
 - BUG: Fix US counties names to use simple hyphens
 - BUG: Fix regular expression returned for a number of Western European countries
 - BUG: Fix regular expression returned for CHECKDIGIT.EAN13

### 4.6.5
 - ENH: Extend driver to support AnalyzeContext usage
 - ENH: Add broader range of honorifics (IND. and MISC.)

### 4.6.4
 - BUG: Significant rework of fractional seconds handling to broaden support - see dateBug()

### 4.6.3
 - ENH: Allow other folks to call the Driver

### 4.6.2
 - ENH: Rework Gender support to improve I18N

### 4.6.1
 - ENH: Add support for the Netherlands (PostalCode, FIRST & LAST names, GENDER)

### 4.6.0
 - ENH: Extend context provided to Analysis (now includes Stream Name, Resolution Mode, Composite Name, Composite elements)
 - NOTE: Bumped to 4.6.0 because new TextAnalzer(null) will now complain, nobody ***should*** have done this, because that is really new TextAnalzer()

### 4.5.29
 - ENH: Improve FIRST and LAST name country support
 - ENH: Allow comments in reference files

### 4.5.28
 - ENH: Improve County list (by removing County word)

### 4.5.27
 - ENH: Add Semantic Types - CHECKDIGIT.ABA (ABA Number (or Routing Transit Number (RTN)))

### 4.5.26
 - ENH: Add example and documentation for generation use case.

### 4.5.25
 - ENH: Add Semantic Types - CURRENCY.TEXT_EN, STATE_PROVINCE.COUNTY_US
 - ENH: Bump jackson, googlephonenumber
 - ENH: Enable Name support for Brazil, German, French and Portugal
 - ENH: Add example and documentation for validation use case.

### 4.5.24
 - ENH: Bump google phone number version
 - ENH: Add Semantic Types - STATE_PROVINCE.CANTON_NAME_CH, STATE_PROVINCE.CANTON_CH

### 4.5.23
 - ENH: Add Semantic Types - NATIONALITY_EN, STATE_PROVINCE.COUNTY_UK
 - BUG: Be a bit more forgiving of rubbish like NaN when processing Double data (REGRESSION)

### 4.5.22
 - ENH: Add support for Italian/Spanish/Netherlands Provinces - STATE_PROVINCE.PROVINCE_IT, STATE_PROVINCE.PROVINCE_NAME_IT, STATE_PROVINCE.PROVINCE_NAME_ES, STATE_PROVINCE.PROVINCE_NAME_NL

### 4.5.21
 - BUG: Could not cope with fractional seconds that was not at the end (e.g. 2021-08-23T19:03:45.63-04:00)

### 4.5.20
 - ENH: Bump Jackson and google phone number
 - ENH: Minimize the number of bogus 'enums' - do not generate enum for long constant length strings of digits and alphas
 - ENH: Improve documentation
 - BUG: Fix Job Title plugin when presented with no words

### 4.5.19
 - ENH: Improve documentation wrt Locales
 - BUG: CONTINENT* and CITY should only be active in English language

### 4.5.18
 - ENH: Improve list of French regions (include common alternate spellings, old regions, etc).
 - ENH: Add Semantic Types - GENDER.TEXT_DE, GENDER.TEXT_FR

### 4.5.17
 - ENH: Add Semantic Types - GENDER.TEXT_JP, STATE_PROVINCE.DEPARTMENT_FR, STATE_PROVINCE.REGION_FR, STATE_PROVINCE.STATE_NAME_DE

### 4.5.16
 - ENH: CLI - Improve error message if plugin file not found
 - BUG: Fix rejection of Finite plugins with a small number of members

### 4.5.15
 - ENH: Improve Java Doc

### 4.5.14
 - ENH: Upgrade version of phone number library
 - ENH: Minimal mod to Gender detection in Portugese
 - ENH: Only use level 2 pattern if below detection threshold (and associated test)

### 4.5.13
 - ENH: Add Semantic Types - DAY.ABBR_<Locale>, DAY.FULL_<Locale>

### 4.5.12
 - INT: Clean up some warnings identified by Github

### 4.5.11
 - Switch back to random (no SecureRandom) for Bulk mode.
 - ENH: Add Semantic Types - GENDER.TEXT_PT, JOB_TITLE_EN

### 4.5.10
 - BUG: FirstLast Plugin - insist on a decent spread of distinct last names and distinct first names

### 4.5.9
 - BUG: Improve SSN RegExp and test cases, fix bug where detection compares against RegExpReturned not RegExpsToMatch
 - ENH: Improve RegExp for City and Names
 - ENH: Widen out list for Honorifics
 - ENH: Add new Semantic Type - VIN (Vehicle Identification Number)

### 4.5.8
 - Remove semantic Type REGION.TEXT_EN

### 4.5.7
 - ENH: Add new Semantic Types - CHECKDIGIT.IBAN

### 4.5.6
 - INT: Rationalize names of reference files
 - ENH: Add new Semantic Types - HONORIFIC_EN, STREET_MARKER_EN

### 4.5.5
 - ENH: RegExp for exponent should cope with Unicode Minus sign
 - ENH: Improve debugging support
 - Infinite types now only operate on the base type they are configure for - will stop TELEPHONE eating dates
 - BUG: FirstLast Plugin - insist on a decent spread of names, so don't get caught by 5 things that could be names repeated 2000 times
 - PhoneNumber Plugin - The Google library is very permissive and generally strips punctuation, be little more discerning so that we don't treat ordinary numbers as phone numbers
 - ENH: Switch Random to SecureRandom
 - ENH: Now builds on Java 11, always targets Java 8 (currently)

### 4.5.4
 - BUG: Fix formatting for TopK and BottomK (when Date or Time types) to honor formatting of input
 - ENH: Upgrade google phone number library

### 4.5.3
 - BUG: Fix broken Sample - SamplePlugin
 - ENH: Enhance ColorPlugin to support French as well as English
 - INT: Add French sample

### 4.5.2
 - ENH: Add new Semantic Types - CONTINENT.CODE_EN, CONTINENT.TEXT_EN

### 4.5.1
 - Switch Semantic Type REGION -> REGION.TEXT_EN (add test)
 - ENH: Prefer to generate the RegExpReturned from Finite types

### 4.5.0
 - ENH: Add new Semantic Type REGION - captures a World Region (e.g. Europe, North America, ...)
 - ENH: Add new Semantic Type STATE_PROVINCE.STATE_MX - Mexican State Code
 - ENH: Add new Semantic Types STATE_PROVINCE.STATE_NAME_<CC>, for CC = AU, CA, MX, US, and NA (for North America)
	- Captures State names, e.g. California, Ontario (also includes State Codes)

### 4.4.1
 - ENH: Uniqueness should never be null -1.0 indicates no Perspective

### 4.4.0
 - ENH: Add Uniqueness metric

### 4.3.3
 - BUG: Fix bug with fields with trailing pipe symbols

### 4.3.2
 - ENH: Upgrade google phone number library
 - BUG: Fix unreliable test
 - ENH: Add support for EAN Checkdigit

### 4.3.1
 - BUG: Fix bad RegExp for CUSIPs
 - ENH: Upgrade google phone number library

### 4.3.0
 - ENH: Interface change - add support for totalCount, will be -1 unless set explicitly by something external that knows the answer.
 - ENH: Interface change - move setKeyConfidence to TextAnalyzer

### 4.2.0
 - ENH: Interface change - possibleKey (boolean) is now keyConfidence (double - 0.0 -> 1.0)
 - ENH: Added setKeyConfidence to TextAnalysisResult so we can override if external system knows better

### 4.1.1
 - ENH: Fixup documentation to align 4.X
 - ENH: Bump google phone number version
 - Change a couple more exceptions

### 4.1.0
 - ENH: Add support for localized offset in Dates 'O' and 'OOOO'
 - BUG: Don't throw unchecked exceptions for invalid locales and issues with plugins since these should be trapped and reported by client
 - ENH: Add support for Unicode minus sign \u2212
 - ENH: Add support to ignore Unicode LEFT_TO_RIGHT_MARK \u200E

### 4.0.0
 - Interface change - now throw FTAPluginException if passed invalid Plugins
 - ENH: Add fta version to the output, just in case we ever change signatures
 - ENH: Add fta version to the jar - so we can tell what is deployed independent of the jar filename
 - ENH: Add --version option to Driver - so we can see what version we are running

### 3.0.22
 - INT: More PMD cleanups
 - ENH: Minor documentation fix

### 3.0.21
 - INT: Lots of PMD cleanup
 - ENH: Add support for CHECKDIGIT.LUHN (Digit String that has a valid Luhn Check digit)
 - ENH: Add support for Securities Identifiers - CUSIP, SEDOL, and ISIN

### 3.0.20
 - INT: Lots of PMD cleanup
 - BUG: Fix issue related to updating regexp when not all samples matched
 - Add some Shape testing
 - ENH: Phone number samples need a decent cardinality to pass muster if no header
 - 'CS' country code was valid (so allow as legal)

### 3.0.18
 - BUG: Fix bug in Phase 3 of date detection
 - ENH: Updated google phone number library

### 3.0.17
 - ENH: Improve recognition of Phone Number fields that do not have a recognized header, bump libraries, improve recognition based on headers generally

### 3.0.16
 - BUG: Fix bug with mixed Date processing
 - ENH: Add ability to set field separator

### 3.0.15
 - ENH: Bump versions

### 3.0.14
 - ENH: Bump versions

### 3.0.13
 - Another attempt at fixing the issue in 3.0.12, also add a test

### 3.0.12
 - BUG: Subtle bug causes NPE, minLongNonZero is initialized to MAX_VALUE (as marker)
 - ENH: Upgrade libraries

### 3.0.11
 - INT: Upgrade dependencies
 - ENH: Improve support for padded fields - both days and hours

### 3.0.10
 - ENH: Bump dependencies

### 3.0.9
 - ENH: After generating an enum we need to check again to see if this matches a logical type & update dependencies

### 3.0.8
 - ENH: Restructure code to separate out date functionality (and improve interfaces)

### 3.0.7
 - BUG: Bulk mode from command line was not honoring options

### 3.0.6
 - ENH: Add support for standalone date parsing

### 3.0.5
 - BUG: DataSignature should not vary based on name of column!
 - ENH: Bump versions

### 3.0.4
 - BUG: getStandardDeviation() needs to guard against null variance

### 3.0.3
 - getMean() and getStandardDeviation() should return boxed types

### 3.0.2
 - ENH: Replace sum column for numerics (long, double) with mean and stadard deviation (using Welford's algorithm)
 - ENH: Add detection for a set of superceded ISO-4217 codes to aid with detection
 - ENH: Add option for retrieving plugin based on a training set
 - INT: Added a couple of tests & fixed a couple of tests

### 3.0.1
 - ENH: DataSignature is now independent of Structure
 - ENH: Support Unix date command which implies padding, e.g. 'Thu Jul  7 09:23:56 PDT 2020' and 'Thu Jul 23 09:56:23 PDT 2020'

### 3.0.0
 - *** Signatures for 3.X are not the same as 2.X ***
 - ENH: Switch to using SecureRandom instead of Random
 - BUG: Zip refs file had bogus entry in it
 - BUG: If finite sets have same score make sure to select the one with the highest priority
 - ENH: Change DataSignature to have less dependency on Structure facts
 - BUG: Fix issue with FirstName/LastName not returning consistent results from one run to next with secureRandom
 - ENH: Bump versions

### 2.3.51
 - ENH: Bump versions, blackList -> invalidList, outlier documentation.

### 2.3.50
 - ENH: Bump versions

### 2.3.49
 - INT: Externalize dependencies for easier management

### 2.3.48
 - ENH: More Countries added to the list we recognize (also dropped the threshold to 85%)

### 2.3.47
 - ENH: Update dependencies
 - INT: Improve test coverage
 - BUG: Fix bug where alpha string was not being promoted to alphanumeric string and hence not matching customer supplied logical type

### 2.3.46
 - BUG: Address issue when no RegExpsToMatch were supplied
 - INT: Improve test coverage

### 2.3.44
 - BUG: Fix bug with YYYYMMDD dates which have 00000000 as null value

### 2.3.43
 - INT: More Sonar Lint cleanup

### 2.3.42
 - INT: Sonar Lint cleanup
 - ENH: Upgrade gradle & libraries

### 2.3.41
 - INT: Address remaining findbugs issues

### 2.3.40
 - INT: Address a set of findbugs issues
 - INT: Improve test coverage

### 2.3.39
 - BUG: Improve test coverage (and fix bug in trainBulk as a consequence :-) )
 - INT: Fixup build.gradle warnings to prepare for gradle 7.0

### 2.3.38
 - INT: Update Copyright to 2020
 - INT: Improve test coverage

### 2.3.37
 - ENH: Add support for min/max on String RegExp types
 - ENH: Add some missing TimeZones (short display names)
 - ENH: Support merging H and k if we detect a 24 time
 - INT: Remove a few warnings
 - INT: Improve test coverage
 - BUG: Fix Usage message
 - ENH: Update libraries

### 2.3.36
 - ENH: Improve outlier detection on enums

### 2.3.35
 - BUG: Fix StructureSignature generation and associated tests

### 2.3.34
 - ENH: Add support for new Semantic Types - DAY.DIGITS and MONTH.DIGITS
 - ENH: Add support for minSamples and minMaxPresent on RegExp matchers
 - ENH: Add support for Boolean (y/n)
 - ENH: Improve regExp generated in the case of character classes e.g. we would rather see [A-G] than A|B|C|D|E|F|G
 - BUG: Fix issues with some missing topK and bottomK

### 2.3.33
 - INT: Cleanup DateTime tracking and add a double semantic test

### 2.3.32
 - Document 'blacklist', upgrade jackson, improve code documentation, add description for Logical Types
 - Switch Content for inline to a JSON document, add first/last as synonyms for FIRST and LAST NAME

### 2.3.31
 - ENH: Add support for detecting 'k' (Date format)

### 2.3.30
 - ENH: Improve support for variable number digits in the fractional seconds

### 2.3.29
 - ENH: Add support for BlackList (list of invalid values), improve test coverage

### 2.3.28
 - INT: Improve test coverage

### 2.3.27
 - ENH: Add support for dataSignature; add getters for dataSignature and structureSignature; add tests

### 2.3.26
 - ENH: Add support for detecting Hex numbers - use to support new Semantic Type - MAC Address
 - ENH: Add support for yyyy/dd/mm dates - really silly but they exist
 - ENH: Upgrade dependent libraries
 - BUG: Fix bug with with a mix of one and two digit percentages (e.g. 4%, 12%)

### 2.3.25
 - ENH: Add support for Structural Signature
 - BUG: Address issue related to incorrectly merging YY and YYYY

### 2.3.24
 - ENH: Update libraries to resolve security vulnerabilities

### 2.3.23
 - ENH: Support doubles with trailing minus signs

### 2.3.22
 - ENH: Add support for checking dependencies, upgrade version of google phonenumber, support longs with trailing minus signs, fix copyright

### 2.3.21
 - Use new class RandomSet to enable to access member randomly without a separate parallel array.

### 2.3.20
 - RegExp matching should be based on the most frequent pattern and not rely on there being only one

### 2.3.19
 - Cities should allow -'s and apostrophes (e.g. "Martha's Vineyard")

### 2.3.18
 - INT: Just cleaning

### 2.3.17
 - ENH: Support CA Postal Codes in the US locale
 - BUG: Bad regexp for FIRST_LAST and LAST_FIRST and LANGUAGE.TEXT_EN
 - Add LogicalTypeFactory to return a LogicalType from a PluginDefinition
 - BUG: Fixed bug in UK Postal Code random generation
 - BUG: isValid on RegExp's not honoring min & max values
 - BUG: CA Post Codes missing validation expression in plugins

### 2.3.16
 - BUG: Fix multi-threading issue (and improve exception reporting)

### 2.3.15
 - ENH: Add support for top K/bottom K on LocalDate, LocalDateTime, ZonedDateTime

### 2.3.14
 - ENH: Change default DateResolutionMode from None to Auto - if executing from the command line

### 2.3.13
 - ENH: Support Lang as a weak synonym for Language
 - BUG: Also validate outliers when looking at finite sets, do not back out Date types as aggressively

### 2.3.12
 - ENH: Add support for LANGUAGE.ISO-639-2, broaden out support for LANGUAGE.TEXT_EN

### 2.3.11
 - ENH: Add support for detecting Language (as Text)
 - ENH: Relax cardinality constraints if the header looks really good
 - ENH: Try all RegExp types and take the best not the first
 - A GUID is a perfectly good key candidate
 - Fix issue related to no default Semantic Types preventing registration of any Semantic Types

### 2.3.10
 - ENH: Improve (marginally) merged name detection, accept lon as a synonym for longitude

### 2.3.9
 - ENH: Add support for SSN

### 2.3.8
 - ENH: Add support for yyyy/MM and yyyy/MM and associated tests, plus some cleaning.

### 2.3.7
 - ENH: Add support for MM/yyyy and MM-yyyy (will return a LocalDate)

### 2.3.6
 - ENH: Add support for MONTH_FULL (e.g. January), improve Zip detection (short zips), add support for NAME.FIRST_LAST (e.g. 'Tim Segall')

### 2.3.5
 - ENH: More countries, improve ISO country code detection, improve city detection, improve long/lat detection

### 2.3.4
 - ENH: Improve detection of Emails and URLs; add support for top & bottom k values

### 2.3.3
 - ENH: Add support for NAME.LAST_FIRST (e.g. 'Segall, Tim'); fix NPE with --help

### 2.3.2
 - ENH: Upgrade version of phone number library; improve NAME detection; improve Country detection

### 2.3.1
 - ENH: Add support for Australian & Canadian Postal Codes

### 2.3.0
 - ENH: Add support for AU States, and move more aggressively to plugins defintion

### 2.2.2
 - ENH: Add support for UK Postal Codes

### 2.2.1
 - ENH: Add Town as a synonym for City

### 2.2.0
 - ENH: Change the Regular Expressions to be slightly more accurate and more Python friendly

### 2.1.29
 - ENH: Do not report shape detail if it is not meaningful (or complete)
