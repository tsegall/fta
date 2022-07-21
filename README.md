# Text Profiling and Semantic Type Detection #

Analyze Text data to determine Base Type and Semantic type information and other key metrics associated with a text stream.
Key objectives of the library include:
* Large set of built-in Semantic Types (extensible via JSON defined plugins).  See list below.
* Sufficiently fast to be used inline.   See Performance notes below.
* Usable in either Streaming or Bulk mode.
* Minimal false positives for Semantic type detection.
* Broad country/language support - including US, Canada, Mexico, Brazil, UK, Australia, much of Europe, Japan and China.
* Support for sharded analysis (i.e. Analysis results can be merged)

Notes:
* By default analysis is performed on the initial 4096 characters (adjustable via setMaxInputLength()).
* Semantic Type detection is typically predicated on plausible input data, for example, a field that contains data that looks
like phone numbers, but that are in fact invalid, will NOT be detected as the Semantic Type TELEPHONE.
* Date detection supports ~750 locales (no support for locales using non-Gregorian calendars or non-Arabic numerals).

Streaming example:
```java
public static void main(final String[] args) throws FTAException {
	final String[] inputs = {
				"Anaïs Nin", "Gertrude Stein", "Paul Cézanne", "Pablo Picasso", "Theodore Roosevelt",
				"Henri Matisse", "Georges Braque", "Henri de Toulouse-Lautrec", "Ernest Hemingway",
				"Alice B. Toklas", "Eleanor Roosevelt", "Edgar Degas", "Pierre-Auguste Renoir",
				"Claude Monet", "Édouard Manet", "Mary Cassatt", "Alfred Sisley",
				"Camille Pissarro", "Franklin Delano Roosevelt", "Winston Churchill" };

	// Use simple constructor - for improved detection provide an AnalyzerContext (see Contextual example).
	final TextAnalyzer analysis = new TextAnalyzer("Famous");

	for (String input : inputs)
		analysis.train(input);

	final TextAnalysisResult result = analysis.getResult();

	System.err.printf("Semantic Type: %s (%s)%n",
			result.getTypeQualifier(), result.getType());

	System.err.println("Detail: " + result.asJSON(true, 1));
}
```

Result: Semantic Type: **NAME.FIRST_LAST** (String)

Bulk example:
```java
public static void main(final String[] args) throws FTAException {

		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		final HashMap<String, Long> basic = new HashMap<>();

		basic.put("Male", 2_000_000L);
		basic.put("Female", 1_000_000L);
		basic.put("Unknown", 10_000L);

		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();

		System.err.printf("Semantic Type: %s (%s)%n", result.getTypeQualifier(), result.getType());

		System.err.println("Detail: " + result.asJSON(true, 1));
	}
```

Result: Semantic Type: **GENDER.TEXT_EN** (String)

## Date Format determination ##

If you are solely interested in determining the format of a date from a **single** sample, then the following example is a good starting point:

```java
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

public abstract class DetermineDateFormat {

	public static void main(final String[] args) {
		final DateTimeParser dtp = new DateTimeParser().withDateResolutionMode(DateResolutionMode.MonthFirst).withLocale(Locale.ENGLISH);

		// Determine the DataTimeFormatter for the following examples
		System.err.printf("Format is: '%s'%n", dtp.determineFormatString("26 July 2012"));
		System.err.printf("Format is: '%s'%n", dtp.determineFormatString("March 9 2012"));
		// Note: Detected as MM/dd/yyyy despite being ambiguous as we indicated MonthFirst above when insufficient data
		System.err.printf("Format is: '%s'%n", dtp.determineFormatString("07/04/2012"));
		System.err.printf("Format is: '%s'%n", dtp.determineFormatString("2012 March 20"));
		System.err.printf("Format is: '%s'%n", dtp.determineFormatString("2012/04/09 18:24:12"));

		// Determine format of the input below and then parse it
		String input = "Wed Mar 04 05:09:06 GMT-06:00 2009";

		String formatString = dtp.determineFormatString(input);

		// Grab the DateTimeFormatter from fta as this creates a case-insensitive parser and it supports a slightly wider set set of formats
		// For example, "yyyy" does not work out of the box if you use DateTimeFormatter.ofPattern
		DateTimeFormatter formatter = DateTimeParser.ofPattern(formatString);

		OffsetDateTime parsedDate = OffsetDateTime.parse(input, formatter);

		System.err.printf("Format is: '%s', Date is: '%s'%n", formatString, parsedDate.toString());
	}
}
```

If you are interested in determining the format based on a set of inputs, then the following example is good starting point:

```java
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

public abstract class DetermineDateFormatTrained {

	public static void main(final String[] args) {
		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.ENGLISH);

		final List<String> inputs = Arrays.asList( "10/1/2008", "10/2/2008", "10/3/2008", "10/4/2008", "10/5/2008", "10/10/2008" );

		inputs.forEach(dtp::train);

		// At this stage we are not sure of the date format, since with 'DateResolutionMode == None' we make no
		// assumption whether it is MM/DD or DD/MM and the format String is unbound (??/?/yyyy)
		System.err.println(dtp.getResult().getFormatString());

		// Once we train with another value which indicates that the Day must be the second field then the new
		// result is correctly determined to be MM/d/yyyy
		dtp.train("10/15/2008");
		System.err.println(dtp.getResult().getFormatString());

		// Once we train with another value which indicates that the Month is expressed using one or two digits the
		// result is correctly determined to be M/d/yyyy
		dtp.train("3/15/2008");
		System.err.println(dtp.getResult().getFormatString());
	}
}
```

Note: For Date Format determination you only need fta-core.jar.

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
 * distinctCount - Number of distinct (valid) samples, typically -1 if maxCardinality exceeded. See Note 2.
 * regExp - A Regular Expression (Java) that matches the detected Type
 * confidence - The percentage confidence (0-1.0) in the determination of the Type
 * type - The Base Type (one of Boolean, Double, Long, String, LocalDate, LocalTime, LocalDateTime, OffsetDateTime, ZonedDateTime)
 * typeQualifier - A modifier with respect to the Base Type. See Note 1.
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
 * uniqueness - The percentage (0.0-1.0) of elements in the stream with a cardinality of one, -1.0 if maxCardinality exceeded.  See Note 2.
 * keyConfidence - The percentage confidence (0-1.0) that the observed stream is a Key field (i.e. unique).  See Note 2.
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

Note 1: The value of the typeQualifier is dependent on the Base Type as follows:
 * Boolean - options are "TRUE_FALSE", "YES_NO", "ONE_ZERO"
 * String - options are "BLANK", "BLANKORNULL", "NULL"
 * Long - options are "GROUPING", "SIGNED", "SIGNED_TRAILING" ("GROUPING" and "SIGNED" are independent and can both be present).
 * Double - options are "GROUPING", "SIGNED", "SIGNED_TRAILING", "NON_LOCALIZED" ("GROUPING" and "SIGNED" are independent and can both be present).
 * LocalDate, LocalTime, LocalDateTime, ZonedDateTime, OffsetDateTime - The qualifier is the detailed date format string (See Java DateTimeFormatter for format details).
 * If any Logical plugins are installed - then additional Qualifiers may be returned. For example, if the LastName plugin is installed and a Last Name is detected then the Base Type will be STRING, and the qualifier will be "NAME.LAST".

Note 2: This field may be set on the Analyzer - and if so FTA attempts no further analysis.

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
CHECKDIGIT.ISBN|ISBN-13 identifiers (with hyphens)|*
CHECKDIGIT.ISIN|International Securities Identification Number|*
CHECKDIGIT.LUHN|Digit String that has a valid Luhn Check digit (and length between 8 and 30 inclusive)|*
CHECKDIGIT.SEDOL|UK/Ireland Security Identifier|*
CHECKDIGIT.UPC|Universal Product Code|*
CITY|City/Town|en
COLOR.HEX|Hex Color code|*
COMPANY_NAME|Company Name|en
CONTINENT.CODE_EN|Continent Code|en
CONTINENT.TEXT_EN|Continent Name|en
COORDINATE.LATITUDE_DECIMAL|Latitude (Decimal degrees)|*
COORDINATE.LONGITUDE_DECIMAL|Longitude (Decimal degrees)|*
COORDINATE.LATITUDE_DMS|Latitude (degrees/minutes/seconds)|*
COORDINATE.LONGITUDE_DMS|Longitude (degrees/minutes/seconds)|*
COORDINATE.EASTING|Coordinate - Easting|*
COORDINATE.NORTHING|Coordinate - Northing|*
COORDINATE_PAIR.DECIMAL|Coordinate Pair (Decimal degrees)|*
COUNTRY.ISO-3166-2|Country as defined by ISO 3166 - Alpha 2|*
COUNTRY.ISO-3166-3|Country as defined by ISO 3166 - Alpha 3|*
COUNTRY.TEXT_&lt;Language&gt;|Country as a string|de, en
CREDIT_CARD_TYPE|Type of Credit CARD - e.g. AMEX, VISA, ...|*
CURRENCY_CODE.ISO-4217|Currency as defined by ISO 4217|*
CURRENCY.TEXT_EN|Currency Name|en
DAY.DIGITS|Day represented as a number (1-31)|*
DAY.ABBR_&lt;Locale&gt;|Day of Week Abbreviation &lt;LOCALE&gt; = Locale, e.g. en-US for English language in US|Current Locale
DAY.FULL_&lt;Locale&gt;|Full Day of Week name &lt;LOCALE&gt; = Locale, e.g. en-US for English language in US|Current Locale
EMAIL|Email Address|*
EPOCH.MILLISECONDS|Unix Epoch (Timestamp) - milliseconds|*
EPOCH.NANOSECONDS|Unix Epoch (Timestamp) - nanoseconds|*
FREE_TEXT|Free Text field - e.g. Description, Notes, Comments, ...|bg, ca, de, en, es, fr, it, nl, pt, ru
GENDER.TEXT_&lt;Language&gt;|Gender|bg, ca, de, en, es, fi, fr, hr, it, ja, ms, nl, pl, pt, ro, sv, tr, zh
GUID|Globally Unique Identifier, e.g. 30DD879E-FE2F-11DB-8314-9800310C9A67|*
HASH.SHA1_HEX|SHA1 Hash - hexadecimal|*
HASH.SHA256_HEX|SHA256 Hash - hexadecimal|*
HONORIFIC_EN|Title (English language)|en
IDENTITY.AADHAR_IN|Aadhar|en-IN, hi-IN
IDENTITY.DUNS|Data Universal Numbering System (Dun & Bradstreet)|*
IDENTITY.EIN_US|Employer Identification Number|en-US
IDENTITY.NHS_UK|NHS Number|en-UK
IDENTITY.SSN_FR|Social Security Number (France)|fr-FR
IDENTITY.SSN_CH|AVH Number / SSN (Switzerland)|de-CH, fr-CH, it-CH
IDENTITY.INDIVIDUAL_NUMBER_JA|Individual Number / My Number (Japan)|ja
IDENTITY.VAT_&lt;COUNTRY&gt;|Value-added Tax Identification Number|de-AT, ca-ES, es-ES, fr-FR, it-IT, en-GB, en-UK, pl-PL
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
PERIOD.HALF|Half (Year)|*
PERIOD.QUARTER|Quarter (Year)|*
PERSON.AGE|Age (Person)|en, es, fr, es, it, pt
PERSON.RACE|Race/Ethinicity (person)|*
POSTAL_CODE.POSTAL_CODE_&lt;Country&gt;|Postal Code|AU, BG, CA, FR, JA, NL, UK, ES, MX, PT, SE, UY
POSTAL_CODE.ZIP5_US|Postal Code|en-CA, en-US
POSTAL_CODE.ZIP5_PLUS4_US|Postal Code + 4|en-CA, en-US
SSN|Social Security Number (US)|en-US
STATE_PROVINCE.COMMUNE_IT|Italian Commune|it-IT
STATE_PROVINCE.COUNTY_&lt;Country&gt;|County|en-IE, en-UK, en-US, ga-IE, hu-HU
STATE_PROVINCE.DISTRICT_NAME_PT|Portuguese District Name|pt-PT
STATE_PROVINCE.MUNICIPALITY_BR|Brazilian Municipality|pt-BR
STATE_PROVINCE.STATE_&lt;Country&gt;|State Code|en-AU, pt-BR, es-MX, en-US
STATE_PROVINCE.STATE_NAME_&lt;Country&gt;|State Name|en-AU, pt-BR, de-DE, es-MX, en-US
STATE_PROVINCE.STATE_PROVINCE_NA|US State Code/Canadian Province Code/Mexican State Code|en-CA, en-US, es-MX
STATE_PROVINCE.PROVINCE_CA|Canadian Province Code|en-CA, en-US
STATE_PROVINCE.PROVINCE_IT|Italian Province Code|it-IT
STATE_PROVINCE.PROVINCE_ZA|South African Province Code|en-ZA
STATE_PROVINCE.PROVINCE_NAME_CA|Canadian Province Name|en-CA, en-US
STATE_PROVINCE.PROVINCE_NAME_EC|Ecuadorian Province Name|es-EC
STATE_PROVINCE.PROVINCE_NAME_ES|Spanish Province Name|es-ES
STATE_PROVINCE.PROVINCE_NAME_IE|Irish Province Name|en-IE
STATE_PROVINCE.PROVINCE_NAME_IT|Italian Province Name|it-IT
STATE_PROVINCE.PROVINCE_NAME_NL|Dutch Province Name|nl-NL
STATE_PROVINCE.PROVINCE_NAME_ZA|South African Province Name|en-ZA
STATE_PROVINCE.STATE_PROVINCE_NAME_NA|US State Name/Canadian Province Name|en-CA, en-US, es-MX
STATE_PROVINCE.DEPARTMENT_FR|French Department Name|fr-FR
STATE_PROVINCE.REGION_FR|French Region Name|fr-FR
STATE_PROVINCE.INSEE_CODE_FR|French Insee Code (5 digit)|fr-FR
STATE_PROVINCE.CANTON_CH|Swiss Canton Code|de-CH, fr-CH, it-CH
STATE_PROVINCE.CANTON_NAME_CH|Swiss Canton Name|de-CH, fr-CH, it-CH
STATE_PROVINCE.PREFECTURE_NAME_JP|Japanese Prefecture Name|ja
STREET_ADDRESS_&lt;Language&gt;|Street Address|bg, ca, da, de, en, es, fi, fr, hr, it, lv, nl, pl, pt, ro, ru, sk
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
                "description": "Naive SSN detection",
                "pluginType": "regex",
                "validLocales": [
                        {
                                "localeTag": "en-US",
                                "headerRegExps": [ { "regExp": ".*(?i)(SSN|Social).*", "confidence": 70 } ],
                                "matchEntries": [ { "regExpReturned" : "(?i)(冥王星|土星|地球|天王星|木星|水星|海王星|火星|金星)" } ]
                        }
                ],
                "threshold": 98,
                "baseType": "STRING"
        },
        {
                "qualifier": "AIRPORT_CODE.IATA",
                "description": "IATA Airport Code",
                "pluginType": "java",
                "clazz": "com.cobber.fta.plugins.IATA",
                "validLocales": [
                        {
                                "localeTag": "*",
                                "headerRegExps": [ { "regExp": ".*(?i)(iata|air).*", "confidence": 100 } ]
                        }
                ],
                "baseType": "STRING"
        },
        {
                "qualifier" : "PLANET_JA",
                "description": "Planets in Japanese (Kanji) via an inline list",
                "pluginType": "list",
                "validLocales": [
                        {
                                "localeTag": "ja",
                                "headerRegExps": [ { "regExp": ".*星", "confidence": 70 } ],
                                "matchEntries": [ { "regExpReturned" : "(?i)(冥王星|土星|地球|天王星|木星|水星|海王星|火星|金星)" } ]
                        }
                ],
                "contentType": "inline",
                "content": "{ \"members\": [ \"冥王星\", \"土星\", \"地球\", \"天王星\", \"木星\", \"水星\", \"海王星\", \"火星\", \"金星\" ] }",
                "backout": ".*",
                "baseType": "STRING"
        }
]
```

The pluginType attribute of the JSON definition will determine the type of the plugin.
If the pluginType is 'java' then a *clazz* field is required and a new Instance of that class will be created.
If the pluginType is 'list' then the *content* field must be present and a new instance of LogicalTypeFiniteSimpleExternal will be instantiated.
If the pluginType is 'regex' then an instance of LogicalTypeRegExp will be instantiated.

In all cases the plugin definition and locale are passed as arguments.

### All Plugins ###

The mandatory 'qualifier' tag is the name of this Semantic Type.

The 'threshold' tag is the percentage of valid samples required by this plugin to establish the Stream Data as a a valid instance of this Semantic Type.
The threshold will default to 95% if not specified.

The 'baseType' tag constrains the plugin to streams that are of this Base Type (see discussion above on the valid Base Types).
The baseType is defined by the implementation for all Code Plugins, STRING for all Finite plugins, and must be defined for RegExp plugins.

The 'validLocales' array is used to constrain the plugin to a set of languages or locales.  This is the set of locales where the plugin should be enabled.
For example, a localeTag of "en-US,en-CA" indicates that the plugin should be enabled in both the US and Canada, a localeTag "en" indicates that the plugin should be enabled in
any locale that uses the English language. In addition the 'headerRegExps' tag is an ordered list of Regular Expression (and the associated confidence) used to match against the Stream Name (if present)'.

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

### Signature Detail ###
**Structure Signature** - Base 64 encoded version of '&lt;Base Type&gt;:&lt;TypeInfo&gt;'. Where &lt;Base Type&gt; is as defined above and &lt;TypeInfo&gt; is either the TypeQualifer if a known Semantic Type, e.g. NAME.FIRST or the detected Regular Expression + the set of shapes.

**Data Signature** - Base 64 encoded version of a JSON structure which includes the following attributes: totalCount, sampleCount, matchCount, nullCount, blankCount, minLength, maxLength, cardinality, cardinalityDetail, outlierCardinality, outliertyDetail, shapesCardinality, shapesDetail, leadingWhiteSpace trailingWhiteSpace, multiline

Additional attributes captured in JSON structure:
- Included if statistics are enabled: min, max, mean, standardDeviation, topK, bottomK
- Included if Base Type == Double: decimalSeparator
- Included if Base Type is Numeric: leadingZeroCount
- Included if Base Type is Date: dateResolutionMode


## Validation and Sample Generation ##

FTA can also be used to validate an input stream either based on known Semantic Types or on Semantic Types detected by FTA.  For example, it is possible to retrieve the LogicalType for a known Semantic Type and then invoke the isValid() method.  This is typically only useful for 'Closed' Semantic Types (isClosed() == true), i.e. those for which there is a known constrained set.  A good example of a closed Semantic Type is the Country code as defined by ISO-3166 Alpha 3.  An example where isValid() would be less useful is FIRST_NAME.  For those cases where the Semantic Type is not one of those known to FTA - the result returned will include a Java Regular Expression which can be used to validate new values.  Refer to the Validation example for further details.

In addition to validating a data Stream, FTA can also be used to generate a synthetic pseudo-random data stream.  For any detected Semantic Type which implements the LTRandom interface it is possible to generate a 'random' element of the Semantic Type by invoking nextRandom(). Refer to the Generation example for further details.

## Merging Analyses ##
FTA supports merging of analyses run on distinct data shards.  So for example, if part of the data to be profiled resides on one shard and the balance on a separate shard then FTA can be invoked on each shard separately and then merged.  To accomplish this individual analyses should be executed (with similar configurations), the resulting serialized forms should then be deserialized on a common node and merged. Refer to the Merge example for further details.

The accuracy of the merge is determined by the cardinality of the two individual shards, and falls into one of the the following three cases:
- cardinality(one) + cardinality(two) < max cardinality
- cardinality(one) or cardinality(two) > max cardinality
- cardinality(one) + cardinality(two) >= max cardinality

Assuming all shards have a cardinality less than the maximum cardinality configured there should be no loss of accuracy due to the merge process.

If either shard has a cardinality greater than the maximum cardinality then certain information has already been lost (e.g. uniqueness, distinctCount, cardinality detail).

If the sum of the cardinality is greater than the maximum cardinality but neither individual shard has a cardinality greater than the maximum cardinality then the only attribute that will be indeterminate is the uniqueness of the merged set and clearly the cardinality of the resulting Analysis will be limited to the maximum cardinality.

Note: The input presented to the merged analysis is the union of the data captured by the cardinality detail, outliers detail and the topK and bottomK from each shard. 

## Getting Starting ##

Fastest way to get started is to review the examples included.

## Releasing a new version ##

### Compile ###
`$ gradle wrapper --gradle-version 7.5`

`$ ./gradlew clean build installDist`

### Running Tests ###
All tests and coverage

`$ ./gradlew test jacocoTestReport`

Just the dates tests

`$ ./gradlew test -Dgroups=dates`

Just one test

`$ ./gradlew types:test --tests TestDates.localeDateTest`

### Generate JavaDoc ###
`$ ./gradlew javadoc`

### Check Dependencies ###
`$ ./gradlew dependencyUpdates`

### Everything ... ###
`$ ./gradlew clean installDist test jacocoTestReport javadoc`

### Setup eclipse project ###
`$ ./gradlew eclipse`

### Releasing a new version ###
`$ ./gradlew publishMavenJavaPublicationToOssrhRepository`

Then go to http://central.sonatype.org/pages/releasing-the-deployment.html and follow the instructions!!
1. login to OSSRH available at https://s01.oss.sonatype.org/
2. Find and select the latest version in the Staging Repository
3. Close the staging repository (wait until complete)
4. Release the staging repository

### Executing ###
Using FTA from the command line, list options:

`$ types/build/install/fta/bin/types --help`

Report on a CSV file:

`$ types/build/install/fta/bin/types filename.csv`

Generate a set of samples:

`$ types/build/install/fta/bin/types --pluginName FREE_TEXT --records 1000`

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

`$ export FTA_TRACE="enabled=true,directory=/tmp,samples=10000"`

This generates a file named &lt;Stream&gt;.fta with the inputs to the analysis for debugging.

You can replay the trace file (assuming a local build) using:

`$ types/build/install/fta/bin/types --replay <Stream>.fta`

## Background Reading ##

* Extracting Syntactic Patterns from Databases (https://arxiv.org/abs/1710.11528v2)
* Sherlock: A Deep Learning Approach to
Semantic Data Type Detection (https://arxiv.org/pdf/1905.10688.pdf)
* Synthesizing Type-Detection Logic for Rich Semantic Data
Types using Open-source Code (https://congyan.org/autotype.pdf)
* T2Dv2 Gold Standard for Matching Web Tables to DBpedia (http://webdatacommons.org/webtables/goldstandardV2.html)
* VizNet Towards a Visualization Learning and Benchmarking Repository (https://viznet.media.mit.edu/)
* Semantic Type Detection: Why It Matters, Current Approaches, and How to Improve It (https://megagon.ai/blog/semantic-type-detection-why-it-matters-current-approaches-and-how-to-improve-it)
* Auto-Type: Synthesizing Type-Detection Logic for Rich Semantic Data Types using Open-source Code (https://www.microsoft.com/en-us/research/publication/synthesizing-type-detection-logic-rich-semantic-data-types-using-open-source-code/)

