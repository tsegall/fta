# Fast Text Analyzer #

Analyze Text data to determine type information and other key metrics associated with a text stream.
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

## Starting ##

Fastest way to get started is to review the samples provided.

## Building ##

`$ gradle wrapper --gradle-version 5.4.1`

`$ ./gradlew installDist`

## Running Tests - including coverage ##
`$ ./gradlew test jacocoTestReport`

## Generate JavaDoc ##
`$ ./gradlew javadoc`

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

To include the Java code in your application, or download the latest jars from the [Maven
repository](http://repo1.maven.org/maven2/com/cobber/fta/fta/).

## Javadoc ##

Javadoc is automatically updated to reflect the latest release at http://javadoc.io/doc/com.cobber.fta/fta/.

## Performance  ##

Indicative performance on an Intel 2.6Ghz i7.  The slower number is with Statistics enabled.

* ~1-2 million dates/sec
* ~2-4 million doubles/sec
* ~3-3 million strings/sec
* ~10-15 million longs/sec

## Semantic Type detection ##

In addition to detecting Base types (e.g. Dates, Floating point, Integers, String) it will also where possible infer Semantic type information along with the Base types.

The following Semantic Types are currently detected:

|Simple Name|Semantic Type|Description|
|-----|---------|-------------|
|Email|EMAIL|Email Address|
|Country (EN)|COUNTRY.TEXT_EN|Country as a string (English language)|
|Country (2-Letter)|COUNTRY.ISO-3166-2|Country as defined by ISO 3166 - Alpha 2|
|Country (3-Letter)|COUNTRY.ISO-3166-3|Country as defined by ISO 3166 - Alpha 3 |
|State (US)|STATE_PROVINCE.STATE_US|US State|
|Province (Canada)|STATE_PROVINCE.PROVINCE_CA|Canadian Province|
|State/Province (NA)|STATE_PROVINCE.STATE_PROVINCE_NA|NA State/Province|
|City|CITY|City|
|Street Address|STREET_ADDRESS_EN|Street Address (English Language)|
|URL (Generic)|URI.URL|URL - see RFC 3986|
|GUID|GUID|GUID, example 123e4567-e89b-12d3-a456-426655440000|
|Name (First)|NAME.FIRST|First Name|
|Name (Last)|NAME.LAST|Last Name|
|IP Address (v4)|IPADDRESS.IPV4|IP V4 Address|
|IP Address (v6)|IPADDRESS.IPV6|IP V6 Address|
|Gender (EN)|GENDER.TEXT_EN|Gender (English language)|
|Postal Code (US ZIP5)|POSTAL_CODE.ZIP5_US|Postal Code (US)|
|Currency Code (ISO)|CURRENCY_CODE.ISO-4217|Currency as defined by ISO 4217|
|Month (Abbreviation)|MONTH.ABBR|Month Abbreviation (<XX> = Language code, e.g. ES for Spanish)|
|Coordinates (Latitude)|COORDINATE.LATITUDE_DECIMAL|Latititude (Decimal degrees)|
|Coordinates (Longitude)|COORDINATE.LONGITUDE_DECIMAL|Longitude (Decimal degrees)|
|Coordinates (Lat-Long)|COORDINATE_PAIR.DECIMAL|Coordinate Pair (Decimal degrees)|
|Telephone (Generic)|TELEPHONE|Telephone Number (Generic)|
|Airport Code (IATA)|AIRPORT_CODE.IATA|IATA Airport Code|
