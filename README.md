# Fast Text Analyzer #

Analyze Text data to determine Base Type and Semantic type information and other key metrics associated with a text stream.
A key objective of the analysis is that it should be sufficiently fast to be inline (e.g. as the
data is input from some source it should be possible to stream the data through this class without
undue performance degradation).  See Performance notes below.
Support for non-English date detection is relatively robust, with the following exceptions:
* No support for non-Gregorian calendars
* No support for non-Arabic numerals

Notes:
* By default analysis is performed on the initial 4096 characters of each record (adjustable via setMaxInputLength())
* Semantic Type detection is typically predicated on valid input data, for example, a field that contains data that looks
like phone numbers, but that are in fact invalid, will NOT be detected as the Semantic Type TELEPHONE.

Typical usage is:
```java
import java.util.Locale;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.TextAnalysisResult;

class Trivial {

        public static void main(String args[]) throws FTAException {

                // Use simple constructor - for improved detection you can provide an AnalyzerContext (see Contextual example)
                TextAnalyzer analysis = new TextAnalyzer("Age");

                analysis.train("12");
                analysis.train("62");
                analysis.train("21");
                analysis.train("37");

                TextAnalysisResult result = analysis.getResult();

                System.err.printf("Result: %s, Regular Expression: %s, Max: %s, Min: %s.\n", result.getType(), result.getRegExp(), result.getMaxValue(), result.getMinValue());
        }
}
```

## Date Format determination ##

If you are solely interested in determining the format of a date, then the following example is a good starting point:

```java
import java.util.Locale;
import com.cobber.fta.DateTimeParser;
import com.cobber.fta.DateTimeParser.DateResolutionMode;

public class DetermineDateFormat {

	public static void main(String[] args) {
		DateTimeParser dtp = new DateTimeParser(DateResolutionMode.MonthFirst, Locale.ENGLISH);

		System.err.println(dtp.determineFormatString("26 July 2012"));
		System.err.println(dtp.determineFormatString("March 9 2012"));
		// Note: Detected as MM/dd/yyyy despite being ambiguous as we indicated MonthFirst above when insufficient data
		System.err.println(dtp.determineFormatString("07/04/2012"));
		System.err.println(dtp.determineFormatString("2012 March 20"));
		System.err.println(dtp.determineFormatString("2012/04/09 18:24:12"));
	}
}
```

## Metrics ##

In addition to the input/configuration attributes:
 * streamName - Name of the input stream
 * dateResolutionMode - Mode used to determine how to resolve dates in the absence of adequate data. One of None, DayFirst, MonthFirst, or Auto. 
 * compositeName - Name of the Composite the Stream is a member of (e.g. Table Name)
 * compositeStreamNames - Ordered list of the Composite Stream names (including streamName)
 * detectionLocale - Locale used to run the analysis (e.g. "en-US")
 * ftaVersion - Version of FTA used to generate analysis

There are a large number of metrics detected, which vary based on the type of the input stream.

<details>
<summary><b>Metrics Supported</b></summary>

 * sampleCount - Number of samples observed
 * matchCount - Number of samples that match the detected Base (or Semantic) type
 * nullCount - Number of null samples
 * blankCount - Number of blank samples
 * regExp - A Regular Expression (Java) that matches the detected Type
 * confidence - The percentage confidence (0-1.0) in the determination of the Type
 * type - The Base Type (one of Boolean, Double, Long, String, LocalDate, LocalTime, LocalDateTime, OffsetDateTime, ZonedDateTime)
 * typeQualifier - A modifier with respect to the Base Type (e.g. for Date types it will be a pattern, for Long types it might be SIGNED, for String types it might be COUNTRY.ISO-3166-2)
 * min - The minimum value observed
 * max - The maximum value observed
 * bottomK (Date, Time, Numeric, and String types only) - lowest 10 values
 * topK (Date, Time, Numeric, and String types only) - highest 10 values
 * minLength - The minimum length (in characters) observed (includes whitespace)
 * maxLength - The maximum length (in characters) observed (includes whitespace)
 * cardinality - The cardinality of the valid set (or MaxCardinality if the set is larger than MaxCardinality)
 * outlierCardinality - The cardinality of the invalid set (or MaxOutlierCardinality if the set is larger than MaxOutlierCardinality)
 * leadingWhiteSpace - Does the observed set have leading white space
 * trailingWhiteSpace - Does the observed set have trailing white space
 * multiline - Does the observed set have leading multiline elements
 * logicalType - Does the observed stream, reflect a Semantic Type
 * uniqueness - The percentage (0.0-1.0) of elements in the stream with a cardinality of one, -1.0 if maxCardinality exceeded.  See Note 1.
 * keyConfidence - The percentage confidence (0-1.0) that the observed stream is a Key field (i.e. unique).  See Note 1.
 * cardinalityDetail - Details on the valid set, list of elements and occurrence count
 * outlierDetail - Details on the invalid set, list of elements and occurrence count
 * shapesDetail - Details on the shapes set, list of elements and occurrence count. This will collapse all numerics to '9', and all alphabetics to 'X'
 * shapesCardinality - The cardinality of the shapes observed
 * structureSignature - A SHA-1 hash that reflects the data stream structure
 * dataSignature - A SHA-1 hash that reflects the data stream contents
 * mean (Numeric types only) - The mean (Uses Welford's algorithm)
 * standardDeviation (Numeric types only) - The population standard deviation (Uses Welford's algorithm)
 * leadingZeroCount (Long type only) - The leading number of zeroes
 * decimalSeparator (Double type only) - The character used to separate the integral component from the fractional component

The following fields are *not* calculated by FTA (but may be set on the Analyzer).
 * totalCount - The total number of elements in the Data Stream (-1 unless set explicitly).

Note 1: this field may be set on the Analyzer - and if so FTA attempts no further analysis.

</details>

## Semantic Type detection ##

In addition to detecting a set of Base types fta will also, when enabled (default on - setDefaultLogicalTypes(false) to disable) infer Semantic type information along with the Base types.

Detection of some Semantic Types is dependent on the current locale as indicated below:

<details>
<summary><b>Semantic Types Supported</b></summary>

Semantic Type|Description|Locale|
---------|-------------|--------|
AIRPORT_CODE.IATA|IATA Airport Code|*
CHECKDIGIT.ABA|ABA Number (or Routing Transit Number (RTN))|*
CHECKDIGIT.CUSIP|North American Security Identifier|*
CHECKDIGIT.EAN13|EAN-13 Check digit (also UPC and ISBN-13)|*
CHECKDIGIT.IBAN|International Bank Account Number|*
CHECKDIGIT.ISIN|International Securities Identification Number|*
CHECKDIGIT.LUHN|Digit String that has a valid Luhn Check digit (and length between 8 and 30 inclusive)|*
CHECKDIGIT.SEDOL|UK/Ireland Security Identifier|*
CITY|City/Town|en
COMPANY_NAME|Company Name|en
CONTINENT.CODE_EN|Continent Code|en
CONTINENT.TEXT_EN|Continent Name|en
COORDINATE.LATITUDE_DECIMAL|Latitude (Decimal degrees)|*
COORDINATE.LONGITUDE_DECIMAL|Longitude (Decimal degrees)|*
COORDINATE.LATITUDE_DMS|Latitude (degrees/minutes/seconds)|*
COORDINATE.LONGITUDE_DMS|Longitude (degrees/minutes/seconds)|*
COORDINATE_PAIR.DECIMAL|Coordinate Pair (Decimal degrees)|*
COUNTRY.ISO-3166-2|Country as defined by ISO 3166 - Alpha 2|*
COUNTRY.ISO-3166-3|Country as defined by ISO 3166 - Alpha 3|*
COUNTRY.TEXT_EN|Country as a string (English language)|en
CREDIT_CARD_TYPE|Type of Credit CARD - e.g. AMEX, VISA, ...|*
CURRENCY_CODE.ISO-4217|Currency as defined by ISO 4217|*
CURRENCY.TEXT_EN|Currency Name|en
DAY.DIGITS|Day represented as a number (1-31)|*
DAY.ABBR_&lt;Locale&gt;|Day of Week Abbreviation &lt;LOCALE&gt; = Locale, e.g. en-US for English language in US|Current Locale
DAY.FULL_&lt;Locale&gt;|Full Day of Week name &lt;LOCALE&gt; = Locale, e.g. en-US for English language in US|Current Locale
EMAIL|Email Address|*
EPOCH.MILLISECONDS|Unix Epoch (Timestamp) - milliseconds|*
EPOCH.NANOSECONDS|Unix Epoch (Timestamp) - nanoseconds|*
GENDER.TEXT_&lt;Language&gt;|Gender|cn, de, en, es, fr, it, ms, nl, pt, tr, ja
GUID|Globally Unique Identifier, e.g. 30DD879E-FE2F-11DB-8314-9800310C9A67|*
HONORIFIC_EN|Title (English language)|en
IDENTITY.AADHAR_IN|Aadhar|en-IN, hi-IN
IDENTITY.DUNS|Data Universal Numbering System (Dun & Bradstreet)|*
IDENTITY.NHS_UK|NHS Number|en-UK
IDENTITY.SSN_FR|Social Security Number (France)|fr-FR
IDENTITY.SSN_CH|AVH Number / SSN (Switzerland)|de-CH, fr-CH, it-CH
IDENTITY.INDIVIDUAL_NUMBER_JA|Individual Number / My Number (Japan)|ja
INDUSTRY_EN|Industry Name|en
IPADDRESS.IPV4|IP V4 Address|*
IPADDRESS.IPV6|IP V6 Address|*
JOB_TITLE_EN|Job Title|en
LANGUAGE.ISO-639-2|Language code - ISO 639, two character|*
LANGUAGE.TEXT_EN|Language name, e.g. English, French, ...|en
MACADDRESS|MAC Address|*
MONTH.ABBR_&lt;Locale&gt;|Month Abbreviation &lt;LOCALE&gt; = Locale, for example, en-US for English language in US|Current Locale
MONTH.DIGITS|Month represented as a number (1-12)|*
MONTH.FULL_&lt;Locale&gt;|Full Month name &lt;LOCALE&gt; = Locale, for example, en-US for English language in US|Current Locale
NAME.FIRST|First Name|br, de, do, en, es, fr, gt, mx, nl, pr, pt
NAME.FIRST_LAST|Merged Name (First Last)|br, de, do, en, es, fr, gt, mx, nl, pr, pt
NAME.LAST|Last Name|br, de, do, en, es, fr, gt, mx, nl, pr, pt
NAME.LAST_FIRST|Merged Name (Last, First)|br, de, do, en, es, fr, gt, mx, nl, pr, pt
NAME.MIDDLE|Middle Name|br, de, do, en, es, fr, gt, mx, nl, pr, pt
NAME.MIDDLE_INITIAL|Middle Initial|br, de, do, en, es, fr, gt, mx, nl, pr, pt
NATIONALITY_EN|Nationality|en
POSTAL_CODE.POSTAL_CODE_&lt;Country&gt;|Postal Code|en-AU, en-CA, nl-NL, en-UK, fr-FR, ja, pt-PT
POSTAL_CODE.ZIP5_US|Postal Code|en-CA, en-US
POSTAL_CODE.ZIP5_PLUS4_US|Postal Code + 4|en-CA, en-US
SSN|Social Security Number (US)|en-US
STATE_PROVINCE.COUNTY_&lt;Country&gt;|County|en-UK, en-US
STATE_PROVINCE.DISTRICT_NAME_PT|Portuguese District Name|pt-PT
STATE_PROVINCE.STATE_&lt;Country&gt;|State Code|en-AU, pt-BR, es-MX, en-US
STATE_PROVINCE.STATE_NAME_&lt;Country&gt;|State Name|en-AU, pt-BR, de-DE, es-MX, en-US
STATE_PROVINCE.STATE_PROVINCE_NA|US State Code/Canadian Province Code/Mexican State Code|en-CA, en-US, es-MX
STATE_PROVINCE.PROVINCE_CA|Canadian Province Code|en-CA, en-US
STATE_PROVINCE.PROVINCE_IT|Italian Province Code|it-IT
STATE_PROVINCE.PROVINCE_ZA|South African Province Code|en-ZA
STATE_PROVINCE.PROVINCE_NAME_CA|Canadian Province Name|en-CA, en-US
STATE_PROVINCE.PROVINCE_NAME_IT|Italian Province Name|it-IT
STATE_PROVINCE.PROVINCE_NAME_ES|Spanish Province Name|es-ES
STATE_PROVINCE.PROVINCE_NAME_NL|Dutch Province Name|nl-NL
STATE_PROVINCE.PROVINCE_NAME_ZA|South African Province Name|en-ZA
STATE_PROVINCE.STATE_PROVINCE_NAME_NA|US State Name/Canadian Province Name|en-CA, en-US, es-MX
STATE_PROVINCE.DEPARTMENT_FR|French Department Name|fr-FR
STATE_PROVINCE.REGION_FR|French Region Name|fr-FR
STATE_PROVINCE.CANTON_CH|Swiss Canton Code|de-CH, fr-CH, it-CH
STATE_PROVINCE.CANTON_NAME_CH|Swiss Canton Name|de-CH, fr-CH, it-CH
STATE_PROVINCE.PREFECTURE_NAME_JP|Japanese Prefecture Name|ja
STREET_ADDRESS_EN|Street Address (English Language)|en
STREET_ADDRESS2_EN|Street Address - Line 2 (English Language)|en
STREET_MARKER_EN| Street Suffix (English Language)|en
TELEPHONE|Telephone Number (Generic)|*
TIMEZONE.IANA|IANA Time Zone (Olson)|*
URI.URL|URL - see RFC 3986|*
VIN|Vehicle Identification Number|*

Note:

Any of the above Semantic Types suffixed with one of the following are locale-sensitive:
 * &lt;Locale&gt; - replaced by the locale, for example, MONTH.FULL_fr-FR (Month Abbreviation in french French)
 * &lt;Language&gt; - replaced by the language from the locale, for example, GENDER.TEXT_PT (Gender in Portuguese)
 * &lt;Country&gt; - replaced by the country from the locale, for example, POSTAL_CODE.POSTAL_CODE_AU (Australian Postal Code)


</details>

Additional Semantic types can be detected by registering additional plugins (see registerPlugins). There are three basic types of plugins:
* Code - captures any complex type (e.g. Even numbers, Credit Cards numbers).  Implemented via a Java Class.
* Finite - captures any finite type (e.g. ISO-3166-2 (Country codes), US States, ...).  Implemented via a supplied list with the valid elements enumerated.
* RegExp - captures any type that can be expressed via a Regular Expression (e.g. SSN).  Implemented via a set of Regular Expressions used to match against.

Note: The Context (the current Stream Name and other field names) can be used to bias detection of the incoming data and/or solely determine the detection.

```json
[
	{
		"qualifier": "PII.SSN",
		"headerRegExps": [ ".*(?i)(SSN|Social).*" ],
		"headerRegExpConfidence": [ 70 ],
		"regExpReturned": "\\d{3}-\\d{2}-\\d{4}",
		"threshold": 98,
		"baseType": "STRING"
	},
	{
		"qualifier": "AIRPORT_CODE.IATA",
		"clazz": "com.cobber.fta.plugins.IATA",
		"headerRegExps": [ ".*(?i)(iata|air).*" ],
		"headerRegExpConfidence": [ 100 ],
	},
	{
		"qualifier" : "PLANET_JA",
		"description": "Planets in Japanese (Kanji) via an inline list",
		"headerRegExps" : [ ".*星" ],
		"headerRegExpConfidence" : [ 70 ],
		"contentType": "inline",
		"content": "{ \"members\": [ \"冥王星\", \"土星\", \"地球\", \"天王星\", \"木星\", \"水星\", \"海王星\", \"火星\", \"金星\" ] }",
		"regExpReturned" : "(?i)(冥王星|土星|地球|天王星|木星|水星|海王星|火星|金星)",
		"backout": ".*",
                "validLocales": [ "ja" ],
	},
]
```

The JSON definition will determine how the plugin is registered.  If a *clazz* field is present then a new Instance of that class will be created.
If the *content* field is present then a new instance of LogicalTypeFiniteSimpleExternal will be instantiated.
If neither *clazz* or *content* is present then an instance of LogicalTypeRegExp will be instantiated.
In all cases the plugin definition and locale are passed as arguments.

### All Plugins ###

The mandatory 'qualifier' tag is the name of this Semantic Type.

The 'threshold' tag is the percentage of valid samples required by this plugin to establish the Stream Data as a a valid instance of this Semantic Type.
The threshold will default to 95% if not specified.

The 'baseType' tag constrains the plugin to streams that are of this Base Type (see discussion above on the valid Base Types).
The baseType is defined by the implementation for all Code Plugins, STRING for all Finite plugins, and must be defined for RegExp plugins.

The optional 'validLocales' tag is used to constrain the plugin to a set of languages or locales.  This is the set of locales where the plugin should be enabled.
For example, [ "en-US" ,"en-CA" ] indicates that the plugin should be enabled in both the US and Canada, [ "en" ] indicates that the plugin should be enabled in
any locale that uses the English language.

The optional 'headerRegExps' tag is an ordered list of Regular Expression used to match against the Stream Name (if present), along with the parallel list 'headerRegExpConfidence' it controls the use of the Stream Name to match the Semantic Type.  For RegExp plugins the headerRegExps are optional but if present must have a confidence of 100% and will be required to match for the Stream to be declared a match.

The optional 'isRegExpComplete' tag indicates if the returned Regular Expression is a definitive representation of the Logical Type. For example, \\d{5} is not for US ZIP codes as 00000 is not a valid Zip but does match the Regular Expression.

### RegExp plugins ###

The mandatory 'regExpReturned' tag is the validation string that will be returned by this plugin if a successful match is established.

The optional 'regExpsToMatch' tag is an ordered list of Regular Expressions used to match against the Stream Data.  If not set then the regExpReturned is used to match.

The optional 'minimum (maximum)' tags are valid for Stream of Base Type Long or Double and further restrict the data that will be considered valid.

The optional 'minMaxPresent' tag indicates that both the minimum and maxixum value must be present in order for the Semantic Type to be recognized.

The optional 'minSamples' tag indicates that in order for this Semantic Type to be detected there must be at least this many samples.

The optional 'invalidList' tag is a list of invalid values for this Semantic Type, for example '[ "000-00-0000" ]' indicates that this is an invalid SSN, despite the fact that it matches the SSN regular expression.

### Finite plugins ###

The mandatory 'contentType' tag determines how the content is provided (possible values are 'inline', 'resource', or 'file').

The mandatory 'content' tag is either a file reference if the contentType is 'resource' or 'file' or the actual content if the contentType is 'inline'.  Note: the content is assumed to be UTF-8.

## Outliers ##

An outlier is a data point that differs significantly from other member of the data set.  There are a set of algorithms used to detect outliers in the input stream:
- For Finite plugins, the set of valid values is predefined and hence outlier detection is simply those elements not in the set.  For example the Semantic type COUNTRY.ISO-3166-2 is backed by a list of both current and historically valid two letter country codes, and hence the two letter string 'PP' would be detected as an outlier, as would the string 'Unknown'.
- For RegExp plugins, the set of valid patterns is predefined and hence outlier detection is simply any element which does not match the pattern.
Note: The Eegular Expression used to detect a Semantic type may differ from the Regular Expression returned by the Semantic Type.  For example
"\\d{3}-\\d{2}-\\d{4}" is used to detect an SSN but the Regular Expression "(?!666|000|9\\d{2})\\d{3}-(?!00)\\d{2}-(?!0{4})\\d{4}" is used to validate and is returned.
- For any fields detected as a known Semantic Type then the outliers are based on the particular Semantic Type, for example if the Semantic Type is
detected as a US Phone Number then numbers with invalid area codes or invalid area code exchange pairs will be flagged as outliers.
- For Code plugins, outliers may be detected based on a statistical analysis. For example if there are 100 valid integers and one 'O' (letter O) then the 'O' would be identified as an outlier.  In other cases, where an enumerated type is detected, for example 100 instances of RED, 100 instances of BLUE, 100 instances of PINK, and one instance of 'P1NK' then the instance of 'P1NK' would be identified as an outlier based on its Levenshtein distance from one of the other elements in the set.

## Regular Expressions ##

The regular expressions detected (regExp) are a valid Java Regular Expression for the data presented.  However, it is likely that the regular expression will generally be too lax and will commonly accept input that is valid according to the regular expression but not according to a 'true' definition of the type in question.  For example, the regular expression for a Social Security Number (SSN) detected will typically present as "\\d{3}-\\d{2}-\\d{4}" which will be valid for any true SSN, however, the inverse is not true - for example, in a true SSN the first component should have 3 digits and additionally should not be 000, 666, or between 900 and 999.

Where a field is detected as a Semantic Type, for example a UK Postal Code then the RegExp ("([A-Za-z][A-Ha-hK-Yk-y]?[0-9][A-Za-z0-9]? ?[0-9][A-Za-z]{2}|[Gg][Ii][Rr] ?0[Aa]{2})") will typically be more robust, although also potentially still not a perfect match. 

## Signatures ##

Given the following three data sets:
- ColumnName: BirthCountry
- Data: France, Germany, Italy, Spain, USA, Belgium, Australia, Italy, New Zealand, China, USA, Poland, Mozambique, Ghana, India, China, Pakistan
- ColumnName: AccountLocation
- Data: Sweden, Sweden, South Africa, Russia, Poland, Norway, Norway, Norway, Norway, Korea, Korea, Hungary, Hungary, Germany, Chile, Canada, Brazil, Austria, Austria
- ColumnName: PrimaryCountry
- Data: Austria, Austria, Brazil, Canada, Chile, Germany, Hungary, Hungary, Korea, Korea, Norway, Norway, Norway, Norway, Poland, Russia, South Africa, Sweden, Sweden

The StructureSignature for all three will be identical as the field will be detected as 'COUNTRY.TEXT_EN', i.e. a Country Name in the English language.  Note this is despite the fact that there is no overlapping data values in the BirthCountry dataset compared to the other data sets.
The DataSignature will be identical for AccountLocation and PrimaryCountry as the dataset is identical and will differ from the DataSignature for the BirthCountry.

## Validation and Sample Generation ##

FTA can also be used to validate an input stream either based on known Semantic Types or on Semantic Types detected by FTA.  For example, it is possible to retrieve the LogicalType for a known Semantic Type and then invoke the isValid() method.  This is typically only useful for 'Closed' Semantic Types (isClosed() == true), i.e. those for which there is a known constrained set.  A good example of a closed Semantic Type is the Country code as defined by ISO-3166 Alpha 3.  An example where isValid() would be less useful is FIRST_NAME.  For those cases where the Semantic Type is not one of those known to FTA - the result returned will include a Java Regular Expression which can be used to validate new values.  Please refer to the Validation example for further details.

In addition to validating a data Stream, FTA can also be used to generate a synthetic pseudo-random data stream.  For any detected Semantic Type which implements the LTRandom interface it is possible to generate a 'random' element of the Semantic Type by invoking nextRandom(). Please refer to the Generation example for further details.

## Getting Starting ##

Fastest way to get started is to review the samples provided.

## Building ##

`$ gradle wrapper --gradle-version 6.9.2`

`$ ./gradlew installDist`

## Running Tests ##
All tests and coverage

`$ ./gradlew test jacocoTestReport`

Just the dates tests

`$ ./gradlew test -Dgroups=dates`

Just one test

`$ ./gradlew test --tests TestDates.localeDateTest`

## Generate JavaDoc ##
`$ ./gradlew javadoc`

## Check Dependencies ##
`$ ./gradlew dependencyUpdates`

## Everything ...
`$ ./gradlew clean installDist test jacocoTestReport javadoc`

## Setup eclipse project ##
`$ ./gradlew eclipse`

## Releasing a new version ##
`$ ./gradlew uploadArchives`

Then go to http://central.sonatype.org/pages/releasing-the-deployment.html and follow the instructions!!
1. login to OSSRH available at https://s01.oss.sonatype.org/
2. Find and select the latest version in the Staging Repository
3. Close the staging repository (wait until complete)
4. Release the staging repository

## Executing ##
Using FTA from the command line, list options:

`$ build/install/fta/bin/fta --help`

Report on a CSV file:

`$ build/install/fta/bin/fta filename.csv`

## Java code ##

Download the latest jars from the [MVN
Repository](https://mvnrepository.com/artifact/com.cobber.fta/fta) or [Maven.org](https://search.maven.org/artifact/com.cobber.fta/fta).

## Javadoc ##

Javadoc is automatically updated to reflect the latest release at http://javadoc.io/doc/com.cobber.fta/fta/.

## Performance  ##

Indicative performance on an Intel 2.6Ghz i7.

* ~0.5 million dates/sec
* ~0.75 million doubles/sec
* ~2.4 million strings/sec
* ~1.4 million longs/sec

## Reporting Issues ##

First step is turn on tracing.  Either using setTrace (see JavaDoc), or via environment variable FTA_TRACE.

General form of options is &lt;attribute1&gt;=&lt;value1&gt;,&lt;attribute2&gt;=&lt;value2&gt; ...

Supported attributes are:
* enabled=true/false
* stream=&lt;name of stream&gt; (defaults to all)
* directory=&lt;directory for trace file&gt; (defaults to java.io.tmpdir)
* samples=&lt;# samples to trace&gt; (defaults to 1000)

For example:

`$ export FTA_TRACE="enabled=true,director=/tmp,samples=10000"`

This generates a file named &lt;Stream&gt;.fta with the inputs to the analysis for debugging.

## Background Reading ##

* Extracting Syntactic Patterns from Databases (https://arxiv.org/abs/1710.11528v2)
* Sherlock: A Deep Learning Approach to
Semantic Data Type Detection (https://arxiv.org/pdf/1905.10688.pdf)
* Synthesizing Type-Detection Logic for Rich Semantic Data
Types using Open-source Code (https://congyan.org/autotype.pdf)
* T2Dv2 Gold Standard for Matching Web Tables to DBpedia (http://webdatacommons.org/webtables/goldstandardV2.html)

