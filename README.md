# Fast Text Analyzer #

Analyze Text data to determine Base (and optionally Semantic) type information and other key metrics associated with a text stream.
A key objective of the analysis is that it should be sufficiently fast to be in-line (e.g. as the
data is input from some source it should be possible to stream the data through this class without
undue performance degradation).  See Performance notes below.
Support for non-English date detection is relatively robust, with the following exceptions:
* No support for non-Gregorian calendars
* No support for non-Arabic numerals and no support for Japanese.

Typical usage is:
```java
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.TextAnalysisResult;
import java.util.Locale;

class Trivial {

        public static void main(String args[]) {

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

## Metrics ##

Metrics detected include:
 * sampleCount - number of samples observed
 * matchCount - number of samples that match the detected Base (or Semantic) type
 * nullCount - number of null samples
 * blankCount - number of blank samples
 * regExp - A Regular Expression (Java) that matches the detected Type
 * confidence - The confidence in the determination of the Type
 * type - The Base Type (one of Boolean, Double, Long, String, LocalDate, LocalTime, LocalDateTime, OffsetDateTime, ZonedDateTime)
 * typeQualifier - A modifier wrt. the Base Type (e.g. for Date types it will be a pattern, for Long types it might be SIGNED, for String types it might be COUNTRY.ISO-3166-2)
 * min - The minimum value observed
 * max - The maximum value observed
 * bottomK - lowest 10 values
 * topK - highest 10 values
 * minLength - The minimum length observed
 * maxLength - The maximum length observed
 * cardinality - The cardinality of the valid set (or MaxCardinality if the set is larger than MaxCardinality)
 * outlierCardinality - The cardinality of the invalid set (or MaxOutlierCardinality if the set is larger than MaxOutlierCardinality)
 * leadingWhiteSpace - Does the observed set have leading white space
 * trailingWhiteSpace - Does the observed set have trailing white space
 * multiline - Does the observed set have leading multiline elements
 * logicalType -Does the oserved set reflect a Semantic Type
 * possibleKey - Does the observed set appear to be a Key field (i.e. unique)
 * cardinalityDetail - Details on the valid set, list of elements and occurence count
 * outlierDetail - Details on the invalid set, list of elements and occurence count
 * shapeDetail - Details on the shapes set, list of elements and occurence count. This will collapse all numerics to '9', and all alphabetics to 'X'
 * structureSignature - A SHA-1 hash that reflects the data stream structure
 * dataSignature - A SHA-1 hash that reflects the data stream contents

## Semantic Type detection ##

In addition to detecting a set of Base types fta will also, when enabled (default on - setDefaultLogicalTypes(false) to disable) infer Semantic type information along with the Base types.

The following Semantic Types are currently detected:

Semantic Type|Description|
---------|-------------|
AIRPORT_CODE.IATA|IATA Airport Code
CITY|City/Town
COORDINATE.LATITUDE_DECIMAL|Latititude (Decimal degrees)
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
MONTH.ABBR_en-US|Month Abbreviation <LOCALE> = Locale, e.g. en-US for English langauge in US)
MONTH.DIGITS|Month represented as a number (1-12)
MONTH.FULL_en-US|Full Month name <LOCALE> = Locale, e.g. en-US for English langauge in US)
NAME.FIRST|First Name
NAME.FIRST_LAST|Merged Name (First Last)
NAME.LAST|Last Name
NAME.LAST_FIRST|Merged Name (Last, First)
POSTAL_CODE.POSTAL_CODE_CA|Postal Code (CA)
POSTAL_CODE.ZIP5_US|Postal Code (US)
SSN|Social Security Number (US)
STATE_PROVINCE.PROVINCE_CA|Canadian Province
STATE_PROVINCE.STATE_PROVINCE_NA|US State/Canadian Province
STATE_PROVINCE.STATE_US|US State
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

The optional 'headerRegExps' tag is an ordered list of Regular Expression used to match against the Stream Name (if present), along with the parallel list 'headerRegExpConfidence' it controls the use of the Stream Name to match the Semantic Type.  For RegExp plugins the headerRegExps are optional but if present must have a confidence of 100% and will be required to match in order for the Stream to be declared a match.

### RegExp plugins ###

The mandatory 'regExpReturned' tag is the validation string that will be returned by this plugin if a successful match is established.

The optional 'regExpsToMatch' tag is an ordered list of Regular Expressions used to match against the Stream Data.  If not set then the regExpReturned is used to match.

The optional 'minimum (maximum)' tags are valid for Stream of Base Type Long or Double and further restrict the data that will be considered valid.

The optional 'minMaxPresent' tag indicates that both the minimum and maxixum value must be present in order for the Semantic Type to be recognized.

The optional 'minSamples' tag indicates that in order for this Semantic Type to be detected there must be at least this many samples.

The optional 'blackList' tag is a list of invalid values for this Semantic Type, for example '[ "000-00-0000" ]' indicates that this is an invalid SSN despite the fact that it matches the SSN regular expression.

### Finite plugins ###

The mandatory 'filename' tag contains a file with the list of valid elements enumerated.

## Getting Starting ##

Fastest way to get started is to review the samples provided.

## Building ##

`$ gradle wrapper --gradle-version 6.1.1`

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

Download the latest jars from the [Maven
repository](https://mvnrepository.com/artifact/com.cobber.fta/fta).

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
* https://homes.cs.washington.edu/~congy/autotype.pdf
* http://webdatacommons.org/webtables/goldstandardV2.html

