
## Changes ##

### 2.1.29 ###
 - Do not report shape detail if it is not meaningful (or complete)

### 2.2.0 ###
 - Change the Regular Expressions to be slightly more accurate and more Python friendly

### 2.2.1 ###
 - Add Town as a synonym for City

### 2.2.2 ###
 - Add support for UK Postal Codes

### 2.3.0 ###
 - Add support for AU States, and move more aggressively to plugins defintion

### 2.3.1 ###
 - Add support for Australian & Canadian Postal Codes

### 2.3.2 ###
 - Upgrade version of phone number library; improve NAME detection; improve Country detection

### 2.3.3 ###
 - Add support for NAME.LAST_FIRST (e.g. 'Segall, Tim'); fix NPE with --help

### 2.3.4 ###
 - Improve detection of Emails and URLs; add support for top & bottom k values

### 2.3.5 ###
 - More countries, improve ISO country code detection, improve city detection, improve long/lat detection

### 2.3.6 ###
 - Add support for MONTH_FULL (e.g. January), improve Zip detection (short zips), add support for NAME.FIRST_LAST (e.g. 'Tim Segall')

### 2.3.7 ###
 - Add support for MM/yyyy and MM-yyyy (will return a LocalDate)

### 2.3.8 ###
 - Add support for yyyy/MM and yyyy/MM and associated tests, plus some cleaning.

### 2.3.9 ###
 - Add support for SSN

### 2.3.10 ###
 - Improve (marginally) merged name detection, accept lon as a synonym for longitude

### 2.3.11 ###
 - Add support for detecting Language (as Text)
 - Relax cardinality constraints if the header looks really good
 - Try all RegExp types and take the best not the first
 - A GUID is a perfectly good key candidate
 - Fix issue related to no default Semantic Types preventing registration of any Semantic Types

### 2.3.12 ###
 - Add support for LANGUAGE.ISO-639-2, broaden out support for LANGUAGE.TEXT_EN

### 2.3.13 ###
 - Support Lang as a weak synonym for Language, bug - also validate outliers when looking at finite sets, do not back out Date types as aggressively

### 2.3.14 ###
 - Change default DateResolutionMode from None to Auto - if executing from the command line

### 2.3.15 ###
 - Add support for top K/bottom K on LocalDate, LocalDateTime, ZonedDateTime

### 2.3.16 ###
 - Fix multi-threading issue (and improve exception reporting)

### 2.3.17 ###
 - Support CA Postal Codes in the US locale
 - Bad regexp for FIRST_LAST and LAST_FIRST and LANGUAGE.TEXT_EN
 - Add LogicalTypeFactory to return a LogicalType from a PluginDefinition
 - Fixed bug in UK Postal Code random generation
 - isValid on RegExp's not honoring min & max values
 - CA Post Codes missing validation expression in plugins

### 2.3.18 ###
 - Just cleaning

### 2.3.19 ###
 - Cities should allow -'s and apostrophes (e.g. "Martha's Vineyard")

### 2.3.20 ###
 - RegExp matching should be based on the most frequent pattern and not rely on there being only one

### 2.3.21 ###
 - Use new class RandomSet to enable to access member randomly without a separate parallel array.

### 2.3.22 ###
 - Add support for checking dependencies, upgrade version of google phonenumber, support longs with trailing minus signs, fix copyright

### 2.3.23 ###
 - Support doubles with trailing minus signs

### 2.3.24 ###
 - Update libraries to resolve security vulnerabilities

### 2.3.25 ###
 - Add support for Structural Signature
 - Address issue related to incorrectly merging YY and YYYY

### 2.3.26 ###
 - Add support for detecting Hex numbers - use to support new Semantic Type - MAC Address
 - Add support for yyyy/dd/mm dates - really silly but they exist
 - Upgrade dependent libraries
 - Fix bug with with a mix of one and two digit percentages (e.g. 4%, 12%)

### 2.3.27 ###
 - Add support for dataSignature; add getters for dataSignature and structureSignature; add tests

### 2.3.28 ###
 - Improve test coverage

### 2.3.29 ###
 - Add support for BlackList (list of invalid values), improve test coverage

### 2.3.30 ###
 - Improve support for variable number digits in the fractional seconds

### 2.3.31 ###
 - Add support for detecting 'k' (Date format)

### 2.3.32 ###
 - Document 'blacklist', upgrade jackson, improve code documentation, add description for Logical Types
 - Switch Content for inline to a JSON document, add first/last as synonyms for FIRST and LAST NAME

### 2.3.33 ###
 - Cleanup DateTime tracking and add a double semantic test

### 2.3.34 ###
 - Add support for new Semantic Types - DAY.DIGITS and MONTH.DIGITS
 - Add support for minSamples and minMaxPresent on RegExp matchers
 - Add support for Boolean (y/n)
 - Improve regExp generated in the case of character classes e.g. we would rather see [A-G] than A|B|C|D|E|F|G
 - Fix issues with some missing topK and bottomK

### 2.3.35 ###
 - Fix StructureSignature generation and associated tests

### 2.3.36 ###
 - Improve outlier detection on enums

### 2.3.37 ###
 - Add support for min/max on String RegExp types
 - Add some missing TimeZones (short display names)
 - Support merging H and k if we detect a 24 time
 - Remove a few warnings
 - Improve test coverage
 - Fix Usage message
 - Update libraries

### 2.3.38 ###
 - Update Copyright to 2020
 - Improve test coverage

### 2.3.39 ###
 - Improve test coverage (and fix bug in trainBulk as a consequence :-) )
 - Fixup build.gradle warnings to prepare for gradle 7.0

### 2.3.40 ###
 - Address a set of findbugs issues
 - Improve test coverage

### 2.3.41 ###
 - Address remaining findbugs issues

### 2.3.42 ###
 - Sonar Lint cleanup
 - Upgrade gradle & libraries

### 2.3.43 ###
 - More Sonar Lint cleanup

### 2.3.44 ###
 - Fix bug with YYYYMMDD dates which have 00000000 as null value

### 2.3.46 ###
 - Address issue when no RegExpsToMatch were supplied
 - Improve test coverage

### 2.3.47 ###
 - Update dependencies
 - Improve test coverage
 - Fix bug where alpha string was not being promoted to alphanumeric string and hence not matching customer supplied logical type

### 2.3.48 ###
 - More Countries added to the list we recognize (also dropped the threshold to 85%)

### 2.3.49 ###
 - Externalize dependencies for easier management

### 2.3.50 ###
 - Bump versions

### 2.3.51 ###
 - Bump versions, blackList -> invalidList, outlier documentation.

### 3.0.0 ###
 - *** Signatures for 3.X are not the same as 2.X ***
 - Switch to using SecureRandom instead of Random
 - Zip refs file had bogus entry in it
 - If finite sets have same score make sure to select the one with the highest priority
 - Change DataSignature to have less dependency on Structure facts
 - Fix issue with FirstName/LastName not returning consistent results from one run to next with secureRandom
 - Bump versions

### 3.0.1
 - DataSignature is now independent of Structure
 - Support Unix date command which implies padding, e.g. 'Thu Jul  7 09:23:56 PDT 2020' and 'Thu Jul 23 09:56:23 PDT 2020'

### 3.0.2
 - Replace sum column for numerics (long, double) with mean and stadard deviation (using Welford's algorithm)
 - Add detection for a set of superceded ISO-4217 codes to aid with detection
 - Add option for retrieving plugin based on a training set
 - Added a couple of tests & fixed a couple of tests

### 3.0.3
 - getMean() and getStandardDeviation() should return boxed types

### 3.0.4
 - getStandardDeviation() needs to guard against null variance

### 3.0.5
 - DataSignature should not vary based on name of column! + bump versions

### 3.0.6
 - Add support for standalone date parsing

### 3.0.7
 - Bulk mode from command line was not honoring options

### 3.0.8
 - Restructure code to separate out date functionality (and improve interfaces)

### 3.0.9
 - After generating an enum we need to check again to see if this matches a logical type & update dependencies

### 3.0.10
 - Bump dependencies

### 3.0.11
 - Upgrade dependencies, improve support for padded fields - both days and hours

### 3.0.12
 - Subtle bug causes NPE, minLongNonZero is initialized to MAX_VALUE (as marker), upgrade libraries

### 3.0.13
 - Another attempt at fixing the issue in 3.0.12, also add a test

### 3.0.14
 - Bump versions

### 3.0.15
 - Bump versions

### 3.0.16
 - Fix bug with mixed Date processing, add ability to set field separator

### 3.0.17
 - Improve recognition of Phone Number fields that do not have a recognized header, bump libraries, improve recognition based on headers generally

### 3.0.18
 - Fix bug in Phase 3 of date detection, updated google phone number library

### 3.0.20
 - Lots of PMD cleanup
 - Fix issue related to updating regexp when not all samples matched
 - Add some Shape testing
 - Phone number samples need a decent cardinality to pass muster if no header
 - 'CS' country code was valid (so allow as legal)

### 3.0.21
 - Lots of PMD cleanup
 - Add support for CHECKDIGIT.LUHN (Digit String that has a valid Luhn Check digit)
 - Add support for Securities Identifiers - CUSIP, SEDOL, and ISIN

### 3.0.22
 - More PMD cleanups
 - Minor documentation fix

### 4.0.0
 - Interface change - now throw FTAPluginException if passed invalid Plugins
 - Add fta version to the output, just in case we ever change signatures
 - Add fta version to the jar - so we can tell what is deployed independent of the jar filename
 - Add --version option to Driver - so we can see what version we are running

### 4.1.0
 - Add support for localized offset in Dates 'O' and 'OOOO'
 - Don't throw unchecked exceptions for invalid locales and issues with plugins since these should be trapped and reported by client
 - Add support for Unicode minus sign \u2212
 - Add support to ignore Unicode LEFT_TO_RIGHT_MARK \u200E

### 4.1.1
 - Fixup documentation to align 4.X
 - Bump google phone number version
 - Change a couple more exceptions

### 4.2.0
 - Interface change - possibleKey (boolean) is now keyConfidence (double - 0.0 -> 1.0)
 - Added setKeyConfidence to TextAnalysisResult so we can override if external system knows better

### 4.3.0
 - Interface change - add support for totalCount, will be -1 unless set explicitly by something external that knows the answer.
 - Interface change - move setKeyConfidence to TextAnalyzer

### 4.3.1
 - Fix bad RegExp for CUSIPs
 - Upgrade google phone number library

### 4.3.2
 - Upgrade google phone number library
 - Fix unreliable test
 - Add support for EAN Checkdigit

### 4.3.3
 - Fix bug with fields with trailing pipe symbols

### 4.4.0
 - Add Uniqueness metric

### 4.4.1
 - Uniqueness should never be null -1.0 indicates no Perspective

### 4.5.0
 - Add new Semantic Type REGION - captures a World Region (e.g. Europe, North America, ...)
 - Add new Semantic Type STATE_PROVINCE.STATE_MX - Mexican State Code
 - Add new Semantic Types STATE_PROVINCE.STATE_NAME_<CC>, for CC = AU, CA, MX, US, and NA (for North America)
	- Captures State names, e.g. California, Ontario (also includes State Codes)

### 4.5.1
 - Switch Semantic Type REGION -> REGION.TEXT_EN (add test)
 - Prefer to generate the RegExpReturned from Finite types

### 4.5.2
 - Add new Semantic Types - CONTINENT.CODE_EN, CONTINENT.TEXT_EN

### 4.5.3
 - Fix broken Sample - SamplePlugin
 - Enhance ColorPlugin to support French as well as English
 - Add French sample

### 4.5.4
 - Fix formatting for TopK and BottomK (when Date or Time types) to honor formatting of input
