# Fast Text Analyzer #

Analyze Text data to determine Base Type and optionally Semantic type information and other key metrics associated with a text stream.
A key objective of the analysis is that it should be sufficiently fast to be in-line (e.g. as the
data is input from some source it should be possible to stream the data through this class without
undue performance degradation).  See Performance notes below.
Support for non-English date detection is relatively robust, with the following exceptions:
* No support for non-Gregorian calendars
* No support for non-Arabic numerals and no support for Japanese.

Typical usage is:
```java
import java.util.Locale;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.TextAnalysisResult;

class Trivial {

        public static void main(String args[]) throws FTAException {

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
 * fieldName - Name of the input stream
 * dateResolutionMode - Mode used to determine how to resolve dates in the absence of adequate data. One of None, DayFirst, MonthFirst, or Auto. 
 * detectionLocale - Locale used to run the analysis (e.g. "en-US")
 * ftaVersion - Version of FTA used to generate analysis

The following Metrics are detected:
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
 * minLength - The minimum length observed (Includes whitespace)
 * maxLength - The maximum length observed (Includes whitespace)
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

## Semantic Type detection ##

In addition to detecting a set of Base types fta will also, when enabled (default on - setDefaultLogicalTypes(false) to disable) infer Semantic type information along with the Base types.

Detection of some Semantic Types is dependent on the current locale as indicated below:

Semantic Type|Description|
---------|-------------|
AIRPORT_CODE.IATA|IATA Airport Code
CHECKDIGIT.LUHN|Digit String that has a valid Luhn Check digit (and length between 8 and 30 inclusive)
CHECKDIGIT.CUSIP|North American Security Identifier
CHECKDIGIT.SEDOL|UK/Ireland Security Identifier
CHECKDIGIT.ISIN|International Securities Identification Number
CHECKDIGIT.EAN13|EAN-13 Check digit (also UPC and ISBN-13)
CITY|City/Town
CONTINENT.CODE_EN|Continent Code
CONTINENT.TEXT_EN|Continent Name
COORDINATE.LATITUDE_DECIMAL|Latitude (Decimal degrees)
COORDINATE.LONGITUDE_DECIMAL|Longitude (Decimal degrees)
COORDINATE_PAIR.DECIMAL|Coordinate Pair (Decimal degrees)
COUNTRY.ISO-3166-2|Country as defined by ISO 3166 - Alpha 2
COUNTRY.ISO-3166-3|Country as defined by ISO 3166 - Alpha 3
COUNTRY.TEXT_EN|Country as a string (English language)
CREDIT_CARD_TYPE|Type of Credit CARD - e.g. AMEX, VISA, ...
CURRENCY_CODE.ISO-4217|Currency as defined by ISO 4217
DAY.DIGITS|Day represented as a number (1-31)
EMAIL|Email Address
GENDER.TEXT_EN|Gender (English Language)
GUID|Globally Unique Identifier, e.g. 30DD879E-FE2F-11DB-8314-9800310C9A67
IPADDRESS.IPV4|IP V4 Address
IPADDRESS.IPV6|IP V6 Address
LANGUAGE.ISO-639-2|Language code - ISO 639, two character
LANGUAGE.TEXT_EN|Language name, e.g. English, French, ...
MACADDRESS|MAC Address
MONTH.ABBR_<Locale>|Month Abbreviation <LOCALE> = Locale, for example, en-US for English language in US
MONTH.DIGITS|Month represented as a number (1-12)
MONTH.FULL_<Locale>|Full Month name <LOCALE> = Locale, for example, en-US for English language in US
NAME.FIRST|First Name
NAME.FIRST_LAST|Merged Name (First Last)
NAME.LAST|Last Name
NAME.LAST_FIRST|Merged Name (Last, First)
POSTAL_CODE.POSTAL_CODE_AU|Postal Code (en-AU)
POSTAL_CODE.POSTAL_CODE_CA|Postal Code (en-CA)
POSTAL_CODE.ZIP5_US|Postal Code (en-US)
POSTAL_CODE.POSTAL_CODE_UK|Postal Code (en-UK)
REGION.TEXT_EN|World Region
SSN|Social Security Number (en-US)
STATE_PROVINCE.STATE_AU|Australian State Code (en-AU)
STATE_PROVINCE.PROVINCE_CA|Canadian Province Code (en-CA/en-US)
STATE_PROVINCE.STATE_MX|Mexican State Code (es-MX)
STATE_PROVINCE.STATE_PROVINCE_NA|US State Code/Canadian Province Code/Mexican State Code (en-CA/en-US/es-MX)
STATE_PROVINCE.STATE_US|US State Code (en-CA/en-US)
STATE_PROVINCE.STATE_NAME_AU|Australian State Name (en-AU)
STATE_PROVINCE.PROVINCE_NAME_CA|Canadian Province Name (en-CA/en-US)
STATE_PROVINCE.STATE_NAME_MX|Mexican State Name (es-MX)
STATE_PROVINCE.STATE_PROVINCE_NAME_NA|US State Name/Canadian Province Name (en-CA/en-US/es-MX)
STATE_PROVINCE.STATE_NAME_US|US State Name (en-CA/en-US)
STREET_ADDRESS_EN|Street Address (English Language)
TELEPHONE|Telephone Number (Generic)
URI.URL|URL - see RFC 3986

Additional Semantic types can be detected by registering additional plugins (see registerPlugins). There are three basic types of plugins:
* Infinite - captures any infinite type (e.g. Even numbers).  Implemented via a Java Class.
* Finite - captures any finite type (e.g. First Name, Day of Week, ...).  Implemented via a supplied file with the valid elements enumerated.
* RegExp - captures any type that can be expressed via a Regular Expression (e.g. SSN).  Implemented via a supplied set of Regular Expressions.

Note: The Stream Name can be used to bias detection of the incoming data and/or solely determine the detection.

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
		"clazz": "com.cobber.fta.plugins.LogicalTypeIATA",
		"headerRegExps": [ ".*(?i)(iata|air).*" ],
		"headerRegExpConfidence": [ 100 ],
	},
	{
		"qualifier": "PLANET",
		"regExpReturned": "\\p{IsAlphabetic}*",
		"threshold": 98,
		"filename": "Planets.txt",
		"baseType": "STRING"
	}
]
```

The JSON definition will determine how the plugin is registered.  If a *clazz* field is present then a new Instance of that class will be created.
If the *content* field is present then a new instance of LogicalTypeFiniteSimpleExternal will be instantiated.
If neither *clazz* or *content* is present then an instance of LogicalTypeRegExp will be instantiated.
In all cases the plugin definition and locale are passed as arguments.

### All Plugins ###

The mandatory 'qualifier' tag is the name of this Semantic Type.

The mandatory 'threshold' tag is the percentage of valid samples required by this plugin to establish the Stream Data as a a valid instance of this Semantic Type.

The mandatory 'baseType' tag constrains the plugin to streams that are of this Base Type (see discussion above on the valid Base Types).

The optional 'validLocales' tag is used to constrain the plugin to a set of languages or locales.

The optional 'headerRegExps' tag is an ordered list of Regular Expression used to match against the Stream Name (if present), along with the parallel list 'headerRegExpConfidence' it controls the use of the Stream Name to match the Semantic Type.  For RegExp plugins the headerRegExps are optional but if present must have a confidence of 100% and will be required to match for the Stream to be declared a match.

### RegExp plugins ###

The mandatory 'regExpReturned' tag is the validation string that will be returned by this plugin if a successful match is established.

The optional 'regExpsToMatch' tag is an ordered list of Regular Expressions used to match against the Stream Data.  If not set then the regExpReturned is used to match.

The optional 'minimum (maximum)' tags are valid for Stream of Base Type Long or Double and further restrict the data that will be considered valid.

The optional 'minMaxPresent' tag indicates that both the minimum and maxixum value must be present in order for the Semantic Type to be recognized.

The optional 'minSamples' tag indicates that in order for this Semantic Type to be detected there must be at least this many samples.

The optional 'invalidList' tag is a list of invalid values for this Semantic Type, for example '[ "000-00-0000" ]' indicates that this is an invalid SSN, despite the fact that it matches the SSN regular expression.

### Finite plugins ###

The mandatory 'filename' tag contains a file with the list of valid elements enumerated.

## Outliers ##

An outlier is a data point that differs significantly from other member of the data set.  There are a set of algorithms used to detect outliers in the input stream:
- For Finite plugins, the set of valid values is predefined and hence outlier detection is simply those elements not in the set.  For example the Semantic type COUNTRY.ISO-3166-2 is backed by a list of both current and historically valid two letter country codes, and hence the two letter string 'PP' would be detected as an outlier, as would the string 'Unknown'.
- For RegExp plugins, the set of valid patterns is predefined and hence outlier detection is simply any element which does not match the pattern.
- For any fields detected as a known Semantic Type then the outliers are based on the particular Semantic Type, for example if the Semantic Type is
detected as a US Phone Number then numbers with invalid area codes or invalid area code exchange pairs will be flagged as outliers.
- For infinite plugins, outliers may be detected based on a statistical analysis. For example if there are 100 valid integers and one 'O' (letter O) then the 'O' would be identified as an outlier.  In other cases, where an enumerated type is detected, for example 100 instances of RED, 100 instances of BLUE, 100 instances of PINK, and one instance of 'P1NK' then the instance of 'P1NK' would be identified as an outlier based on its Levenshtein distance from one of the other elements in the set.

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

## Getting Starting ##

Fastest way to get started is to review the samples provided.

## Building ##

`$ gradle wrapper --gradle-version 6.8.3`

`$ ./gradlew installDist`

## Running Tests - including coverage ##
`$ ./gradlew test jacocoTestReport`

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
1. login to OSSRH available at https://oss.sonatype.org/
2. Find and select the latest version in the Staging Repository
3. Close the staging repository (wait until complete)
4. Release the staging repository

## Java code ##

Download the latest jars from the [MVN
Repository](https://mvnrepository.com/artifact/com.cobber.fta/fta) or [Maven.org](https://search.maven.org/artifact/com.cobber.fta/fta).

## Javadoc ##

Javadoc is automatically updated to reflect the latest release at http://javadoc.io/doc/com.cobber.fta/fta/.

## Performance  ##

Indicative performance on an Intel 2.6Ghz i7.  The slower number is with Statistics enabled.

* ~1-2 million dates/sec
* ~2-4 million doubles/sec
* ~3-3 million strings/sec
* ~10-15 million longs/sec

## Background Reading ##

* https://arxiv.org/pdf/1905.10688.pdf
* https://congyan.org/autotype.pdf
* http://webdatacommons.org/webtables/goldstandardV2.html

