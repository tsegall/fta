
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
