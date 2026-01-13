[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/com.cobber.fta/fta.svg?label=Maven%20Central)](https://search.maven.org/artifact/com.cobber.fta/fta)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/tsegall/fta?logo=GitHub)](https://github.com/tsegall/fta/releases)
[![CodeQL](https://github.com/tsegall/fta/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/tsegall/fta/actions/workflows/codeql-analysis.yml)
[![javadoc](https://javadoc.io/badge2/com.cobber.fta/fta/javadoc.svg)](https://javadoc.io/doc/com.cobber.fta/fta)
[![codecov](https://codecov.io/gh/tsegall/fta/graph/badge.svg?token=EV1PTKU4S7)](https://codecov.io/gh/tsegall/fta)

# Semantic Type Detection and Data Profiling #

Metadata/data identification Java library. Identifies Base Type (e.g. Boolean, Double, Long, String, LocalDate, LocalTime, ...) and
[Semantic Type](SemanticTypeDetection.md) (e.g. Gender, Age, Color, Country, ...).
Extensive country/language support. Extensible via user-defined plugins. Comprehensive Profiling support.

Design objectives:
* Large set of built-in Semantic Types (extensible via JSON defined plugins).  [Details](SemanticTypes.md).
* Extensive Profiling metrics (e.g. Min, Max, Distinct, signatures, …)
* Sufficiently fast to be used inline.   See Speed notes below.
* Minimal false positives for Semantic type detection. See Performance notes below.
* Usable in either Streaming, Bulk or Record  mode.
* Broad country/language support - including US, Canada, Mexico, Brazil, UK, Australia, India, much of Europe, Japan and China.
* Support for sharded analysis (i.e. Analysis results can be merged)
* Once stream is profiled then subsequent samples can be validated and/or new samples can be generated

Note: Date detection supports ~750 locales (no support for locales using non-Gregorian calendars or non-Arabic numerals).

## Usage

FTA is available in Maven Central. Include it in your project with:

```xml
<dependency>
    <groupId>com.cobber.fta</groupId>
    <artifactId>fta</artifactId>
    <version>17.1.0</version>
</dependency>
```

### Streaming Mode Example
Used when the source is inherently continuous, e.g. IOT device, flat file, etc.

```java
	String[] inputs = {
				"Anaïs Nin", "Gertrude Stein", "Paul Cézanne", "Pablo Picasso", "Theodore Roosevelt",
				"Henri Matisse", "Georges Braque", "Henri de Toulouse-Lautrec", "Ernest Hemingway",
				"Alice B. Toklas", "Eleanor Roosevelt", "Edgar Degas", "Pierre-Auguste Renoir",
				"Claude Monet", "Édouard Manet", "Mary Cassatt", "Alfred Sisley",
				"Camille Pissarro", "Franklin Delano Roosevelt", "Winston Churchill" };

	// Use simple constructor - for improved detection provide an AnalyzerContext (see Contextual example).
	TextAnalyzer analysis = new TextAnalyzer("Famous");

	for (String input : inputs)
		analysis.train(input);

	TextAnalysisResult result = analysis.getResult();

	System.err.printf("Semantic Type: %s (%s)%n",
			result.getSemanticType(), result.getType());

	System.err.println("Detail: " + result.asJSON(true, 1));
```

Result: Semantic Type: **NAME.FIRST_LAST** (String)

### Bulk Mode Example
Used when the source offers the ability to group at source, e.g. a Database.  The advantages of using Bulk mode are that as the data is pre-aggregated the analysis is significantly faster, and the Semantic Type detection is not biased by a set of outliers present early in the analysis.

```java
	TextAnalyzer analysis = new TextAnalyzer("Gender");
	HashMap<String, Long> basic = new HashMap<>();

	basic.put("Male", 2_000_000L);
	basic.put("Female", 1_000_000L);
	basic.put("Unknown", 10_000L);

	analysis.trainBulk(basic);

	TextAnalysisResult result = analysis.getResult();

	System.err.printf("Semantic Type: %s (%s)%n", result.getSemanticType(), result.getType());

	System.err.println("Detail: " + result.asJSON(true, 1));
```

Result: Semantic Type: **GENDER.TEXT_EN** (String)

### Record Mode Example
Used when the primary objective is Semantic Type information and not profiling, or when the focus is on a subset of the data (e.g. fewer than MAX_CARDINALITY records).  The advantage of using Record mode is that the Semantic Type detection is stronger and there is support for cross-stream analysis.


```java
	String[] headers = { "First", "Last", "MI" };
	String[][] names = {
			{ "Anaïs", "Nin", "" }, { "Gertrude", "Stein", "" }, { "Paul", "Campbell", "" },
			{ "Pablo", "Picasso", "" }, { "Theodore", "Camp", "" }, { "Henri", "Matisse", "" },
			{ "Georges", "Braque", "" }, { "Ernest", "Hemingway", "" }, { "Alice", "Toklas", "B." },
			{ "Eleanor", "Roosevelt", "" }, { "Edgar", "Degas", "" }, { "Pierre-Auguste", "Wren", "" },
			{ "Claude", "Monet", "" }, { "Édouard", "Sorenson", "" }, { "Mary", "Dunning", "" },
			{ "Alfred", "Jones", "" }, { "Joseph", "Smith", "" }, { "Camille", "Pissarro", "" },
			{ "Franklin", "Roosevelt", "Delano" }, { "Winston", "Churchill", "" }
	};

	AnalyzerContext context = new AnalyzerContext(null, DateResolutionMode.Auto, "customer", headers );
	TextAnalyzer template = new TextAnalyzer(context);

	RecordAnalyzer analysis = new RecordAnalyzer(template);
	for (String [] name : names)
		analysis.train(name);

	RecordAnalysisResult recordResult = analysis.getResult();

	for (TextAnalysisResult result : recordResult.getStreamResults()) {
		System.err.printf("Semantic Type: %s (%s)%n", result.getSemanticType(), result.getType());
	}
```

Result:

Semantic Type: ***NAME.FIRST*** (String)
</br>
Semantic Type: ***NAME.LAST*** (String)
</br>
Semantic Type: ***NAME.MIDDLE*** (String)

### Additional Examples

Are in the [examples](examples) directory.

## Date Format determination ##

If you are solely interested in determining the format of a date from a **single** sample, then the following example is a good starting point:

```java
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
```

If you are interested in determining the format based on a set of inputs, then the following example is good starting point:

```java
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
<summary><b>Supported Metrics</b></summary>

 * sampleCount - Number of samples observed (read [Merging Analyzes](#merging-analyses) for subtleties associated with merging)
 * matchCount - Number of samples that match the detected Base (or Semantic) type
 * nullCount - Number of null samples
 * blankCount - Number of blank samples
 * distinctCount - Number of distinct (valid) samples, typically -1 if maxCardinality exceeded. See Note 1.
 * regExp - A Regular Expression (Java) that matches the detected Type
 * confidence - The percentage confidence (0-1.0) in the determination of the Type. If no Semantic Type is detected then the confidence reflects the confidence in the Base Type, if a Semantic Type is detected then the confidence reflects the confidence in the Semantic Type.
 * type - The Base Type (one of Boolean, Double, Long, String, LocalDate, LocalTime, LocalDateTime, OffsetDateTime, ZonedDateTime)
 * typeModifier - A modifier with respect to the Base Type. See Base Type discussion.
 * min - The minimum value observed
 * max - The maximum value observed
 * bottomK (Date, Time, Numeric, and String types only) - lowest 10 values
 * topK (Date, Time, Numeric, and String types only) - highest 10 values
 * minLength - The minimum length (in characters) observed (includes whitespace)
 * maxLength - The maximum length (in characters) observed (includes whitespace)
 * cardinality - The cardinality of the valid set (or MaxCardinality if the set is larger than MaxCardinality)
 * outlierCardinality - The cardinality of the set of outliers (or MaxOutlierCardinality if the set is larger than MaxOutlierCardinality)
 * invalidCardinality - The cardinality of the set of invalid entries
 * leadingWhiteSpace - Does the observed set have leading white space
 * trailingWhiteSpace - Does the observed set have trailing white space
 * multiline - Does the observed set have leading multiline elements
 * isSemanticType - Does the observed stream, reflect a Semantic Type
 * semanticType - The Semantic Type detected (Only valid if isSemanticType is true)
 * uniqueness - The percentage (0.0-1.0) of non-null/non-blank elements in the stream with a cardinality of one, -1.0 if maxCardinality exceeded.  See Note 1.
 * keyConfidence - The percentage confidence (0-1.0) that the observed stream is a Key field (i.e. unique and non-null/non-blank).  See Note 1.
 * cardinalityDetail - Details on the valid set, list of elements and occurrence count
 * outlierDetail - Details on the invalid set, list of elements and occurrence count
 * shapesDetail - Details on the shapes set, list of elements and occurrence count. This will collapse all numerics to '9', and all alphabetics to 'X'
 * shapesCardinality - The cardinality of the shapes observed
 * lengthFrequency - String length frequencies for strings of length < 127 (Any strings >= 127 are at element 127).
 * structureSignature - A SHA-1 hash that reflects the data stream structure
 * dataSignature - A SHA-1 hash that reflects the data stream contents
 * mean (Numeric types only) - The mean (Uses Welford's algorithm)
 * standardDeviation (Numeric types only) - The population standard deviation (Uses Welford's algorithm)
 * leadingZeroCount (Long type only) - The leading number of zeroes
 * decimalSeparator (Double type only) - The character used to separate the integral component from the fractional component
 * quantiles - access to q-quantiles. See Note 2.
 * histograms - access to the associated histogram. See Note 3.

The following fields are **not** calculated by FTA (but may be set on the Analyzer).  With the exception of totalMean and totalStandardDeviation these 'total' fields will be set appropriately if two Analyzers are merged, and are available on the Analysis result.
 * totalCount - The total number of elements in the entire data stream (-1 unless set explicitly).
 * totalNullCount - The number of null elements in the entire data stream (-1 unless set explicitly).
 * totalBlankCount - The number of blank elements in the entire data stream (-1 unless set explicitly).
 * totalMean - The mean for Numeric types (Long, Double) across the entire data stream (null unless set explicitly).
 * totalStandardDeviation - The standard deviation for Numeric types (Long, Double) across the entire data stream (null unless set explicitly).
 * totalMinValue - The minimum value across the entire data stream (null unless set explicitly).
 * totalMaxValue - The maximum value across the entire data stream (null unless set explicitly).
 * totalMinLength - The minimum length for Numeric, Boolean, and String types across the entire data stream (-1 unless set explicitly).
 * totalMaxLength - The maximum length for Numeric, Boolean, and String types across the entire data stream (-1 unless set explicitly).

Note 1: This field may be set on the Analyzer - and if so FTA attempts no further analysis.

Note 2: quantiles are exact for any set where the cardinality is less than maxCardinality.  No support for quantiles for String types where maxCardinality is exceeded, for other types the quantiles are estimates that are within the relative-error guarantee.

Note 3: Histograms are precise for any set where the cardinality is less than maxCardinality.  No support for histograms for String types where maxCardinality is exceeded, for other types the histograms are estimates - see A Streaming Parallel Decision Tree Algorithm (https://www.jmlr.org/papers/volume11/ben-haim10a/ben-haim10a.pdf) for more details.

</details>

## Base Type detection ##
The Base Types detected are Boolean, Double, Long, String, LocalDate, LocalTime, LocalDateTime, OffsetDateTime, ZonedDateTime.

Associated with each Base Type is a typeModifier. The value of the typeModifier is dependent on the Base Type as follows:

 * Boolean - options are "TRUE_FALSE", "YES_NO", "Y_N", "ONE_ZERO"
 * String - options are "BLANK", "BLANKORNULL", "NULL"
 * Long - options are "GROUPING", "SIGNED", "SIGNED_TRAILING" ("GROUPING" and "SIGNED" are independent and can both be present).
 * Double - options are "GROUPING", "SIGNED", "SIGNED_TRAILING", "NON_LOCALIZED" ("GROUPING" and "SIGNED" are independent and can both be present).
 * LocalDate, LocalTime, LocalDateTime, ZonedDateTime, OffsetDateTime - The typeModifier is the detailed date format string (See Java DateTimeFormatter for format details).

## Semantic Type detection ##

In addition to detecting a set of Base types FTA will also, when enabled (default on - analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false) to disable) infer Semantic type information along with the Base types.

* Semantic Type detection is typically predicated on plausible input data, for example, a field that contains data that looks
like phone numbers, but that are in fact invalid, will NOT be detected as the Semantic Type TELEPHONE.
* The set of Semantic Types detected is dependent on the current locale
* The data stream name (e.g. the database field name or CSV field name) is commonly used to bias the detection.  For example, if the locale language is English and the data stream matches the regular expression '.\*(?i)(surname|last.?name|lname|maiden.?name|name.?last|last_nm).\*|last' then the detection is more likely to declare this stream a NAME.LAST Semantic Type. The data stream name can also be used to negatively bias the detection.  Consult the plugins.json file for more details.
* Assuming the entire set of stream names is available, Semantic Type detection of a particular column may be impacted by other stream names, for example the Semantic Type PERSON.AGE is detected if we detect another field of type GENDER or NAME.FIRST.
* When using Record mode for Semantic Type analysis - the detection of Semantic Types for a stream may be impacted by prior determination of the Semantic Type of another Stream (either via detection or provided with the Context)
* By default analysis is performed on the initial 4096 characters of the field (adjustable via setMaxInputLength()).
* If two Semantic Types have equal confidence then the Semantic Type with the highest priority will be selected.

[Details of Semantic Types detected](SemanticTypes.md)

### Performance ###

The English-language performance of Semantic Type determination is based on a large sample of inputs from open data portals.
The data set can be found at [semantic-types](https://github.com/tsegall/semantic-types).

Based on this set the average Precision across the identified Semantic Types is estimated at ~99.7%, the Recall at ~98.4% with an F1-Score of ~99.0%.

Precision == True Positives / (True Positives + False Positives)

Recall (Sensitivity) == True Positives / All Positives (i.e. True Positives + False Negatives)

F1-Score == 2 * ((Precision * Recall) / (Precision + Recall))

### Additional user-defined Semantic Types ###

Additional Semantic types can be detected by registering additional plugins (see registerPlugins).
The plugins can either be registered ahead of or after the built-in plugins.
There are three basic types of plugins:
* RegExp (regex) - captures any type that can be expressed via a Regular Expression (e.g. SSN).  Implemented via a set of Regular Expressions used to match against.
* Finite (list) - captures any finite type (e.g. ISO-3166-2 (Country codes), US States, ...).  Implemented via a supplied list with the valid elements enumerated.
* Code (java) - captures any complex type (e.g. Even numbers, Credit Cards numbers).  Implemented via a Java Class.

Note: The Context (the current Stream Name and other field names) can be used to bias detection of the incoming data and/or solely determine the detection.

```json
[
	{
		"semanticType": "HASH.SHA1_HEX",
		"description": "SHA1 Hash - hexadecimal",
		"pluginType": "regex",
		"validLocales": [
			{
				"localeTag": "*",
				"headerRegExps": [ { "regExp": ".*(?i)(SHA1|hash).*", "confidence": 99 } ],
				"matchEntries": [ {
					"regExpReturned": "\\p{XDigit}{40}",
					"isRegExpComplete": true
				} ]
			}
		],
		"documentation": [
			{ "source": "wikipedia", "reference": "https://en.wikipedia.org/wiki/SHA-1" }
		],
		"threshold": 99,
		"baseType": "STRING"
	},
	{
		"semanticType": "AIRLINE.IATA_CODE",
		"description": "IATA Airline Code",
		"pluginType": "list",
		"validLocales": [
			{
				"localeTag": "*",
				"headerRegExps": [ { "regExp": ".*(?i)(iata|air).*", "confidence": 99, "mandatory": true } ]
			}
		],
		"content": {
			"type": "resource",
			"reference": "/reference/IATA_airline.csv"
		},
		"documentation": [
			{ "source": "wikipedia", "reference": "https://en.wikipedia.org/wiki/Airline_codes#IATA_airline_designator" },
			{ "source": "schema", "reference": "https://schema.org/iataCode" }
		],
		"threshold": 97
	},
	{
		"semanticType": "EMAIL",
		"description": "Email Address",
		"pluginType": "java",
		"signature": "+A0AMjgeFlGRlPKsX/iXYmoWpfY=",
		"clazz": "com.cobber.fta.plugins.EmailLT",
		"validLocales": [
			{ "localeTag":  "*", "headerRegExps": [ { "regExp": ".*(?i)(email|e-mail|e_mail).*", "confidence": 90 } ] },
			{ "localeTag":  "es", "headerRegExps": [ { "regExp": ".*(?i)(correo|email|e-mail|e_mail).*", "confidence": 90 } ] }
		],
		"documentation": [
			{ "source": "wikidata", "reference": "https://www.wikidata.org/wiki/Property:P968" },
			{ "source": "wikipedia", "reference": "https://https://en.wikipedia.org/wiki/Email_address" },
			{ "source": "schema", "reference": "https://schema.org/email" }
		]
	}
]
```

The pluginType attribute of the JSON definition will determine the type of the plugin.
If the pluginType is 'java' then a *clazz* field is required and a new Instance of that class will be created.
If the pluginType is 'list' then the *content* field must be present and a new instance of LogicalTypeFiniteSimpleExternal will be instantiated.
If the pluginType is 'regex' then an instance of LogicalTypeRegExp will be instantiated.

In all cases the plugin definition and locale are passed as arguments.

### All Plugins ###

The mandatory 'semanticType' tag is the name of this Semantic Type.

The 'threshold' tag is the percentage confidence we require to establish the Stream Data as a valid instance of this Semantic Type.  In the simplest case this can be the percentage of samples detected as valid in the provided stream.  More commonly the confidence is determined by a combination of the header confidence and the observed data.
The threshold will default to 95% if not specified.

The 'baseType' tag constrains the plugin to streams that are of this Base Type (see discussion above on the valid Base Types).
The baseType is defined by the implementation for all Code Plugins, STRING for all Finite plugins, and must be defined for RegExp plugins.

The 'validLocales' array is used to constrain the plugin to a set of languages or locales.  This is the set of locales where the plugin should be enabled.
For example, a localeTag of "en-US,en-CA" indicates that the plugin should be enabled in both the US and Canada, a localeTag "en" indicates that the plugin should be enabled in
any locale that uses the English language. In addition the 'headerRegExps' tag is an ordered list of Regular Expression (and the associated confidence) used to match against the Stream Name (if present)'.  The headerRegExps is optional and is used to bias the detection.  It is possible to insist that a header is always present, this can be useful if misdetection based on the data is likely.  For example, the Semantic Type MONTH.DIGITS requires a header as the probability of an error without insisting on a reasonable header present is high. Note: a confidence of -100 means that the supplied value must NOT be present in the header.

The optional 'isRegExpComplete' tag indicates if the returned Regular Expression is a definitive representation of the Logical Type. For example, \\d{5} is not for US ZIP codes as 00000 is not a valid Zip but does match the Regular Expression.

The optional 'documentation' tag includes definitive reference material for the Semantic Type. This will typically include a well-known 'source' as well a 'reference' to the detailed information.

### RegExp plugins ###

The mandatory 'regExpReturned' tag is the validation string that will be returned by this plugin if a successful match is established.

The optional 'regExpsToMatch' tag is an ordered list of Regular Expressions used to match against the Stream Data.  If not set then the regExpReturned is used to match.

The optional 'minimum (maximum)' tags are valid for Stream of Base Type Long or Double and further restrict the data that will be considered valid.

The optional 'minMaxPresent' tag indicates that both the minimum and maximum value must be present in order for the Semantic Type to be recognized.

The optional 'minSamples' tag indicates that in order for this Semantic Type to be detected there must be at least this many samples.

The optional 'invalidList' tag is a list of invalid values for this Semantic Type, for example '[ "000-00-0000" ]' indicates that this is an invalid SSN, despite the fact that it matches the SSN regular expression.

#### Example
The following example is looking for an Indian Postal Code.  In this case the header is mandatory so we will insist on both detecting a regular expression of the form '\d{6}' and a case independent match for the header.  The plugin will return '[1-9]\\d{5}' as it is illegal to have a leading zero for an Indian Postal Code.

```json
	{
		"semanticType": "POSTAL_CODE.POSTAL_CODE_IN",
		"description": "Postal Code (IN)",
		"pluginType": "regex",
		"validLocales": [
			{
				"localeTag": "en-IN,hi-IN",
				"headerRegExps": [ { "regExp": ".*(?i)(?u)(pincode).*", "confidence": 95, "mandatory": true } ],
				"matchEntries": [ {
					"regExpsToMatch": [ "\\d{6}" ],
					"regExpReturned": "[1-9]\\d{5}"
				} ]
			}
		],
		"threshold": 98
	}
```

### Finite plugins ###

The mandatory 'content' element is required.

The 'type' tag determines how the content is provided (possible values are 'inline', 'resource', or 'file').
If the type is 'inline' then the tag 'members' is the array of possible values.  If the type is 'resource' or 'file' then the tag 'reference' is the file/resource that contains the list of values.  Note: the list of possible values is required to be upper case and encoded in UTF-8.

The optional 'ignoreList' tag is a set of strings that should be ignored when checking the known good list.

#### Example
The following example is looking for elements from the Periodic Table.  The list of valid entries is in a resource file named elements.csv.

```json
{
	"semanticType": "CUSTOM.ELEMENTS",
	"description": "Periodic Table Elements",
	"pluginType": "list",
	"validLocales": [ {
		"localeTag": "en"
	} ],
	"threshold": 95,
	"content": {
		"type": "resource",
		"reference": "/elements.csv"
	},
	"backout": "\\\\p{IsAlphabetic}{1,2}"
}
```

### Code plugins ###

Code plugins are implemented via a Java class.  This class will typically either extend LogicalTypeInfinite for types with a large number of members, or extend LogicalTypeFinite for a type with a finite number of members.  For a simple example refer to the code to detect IPv4 addresses (IPV4Address.java) or the sample PluginColor.java.

#### Key methods
isCandidate() - Fast check to see if the input might be an instance of this Semantic type.

isValid() - Is the supplied input an instance of this Semantic type?

getRegExp()	- The Regular Expression that most closely matches this Semantic Type.

getConfidence() - Will default to the number of valid samples / size of the sample set.  This is commonly overridden to bias the confidence based on the field name.

nextRandom() - Will generate a random (secure) valid example of this Semantic Type.

analyzeSet() - Given the data set analyzed determine if this set is likely an instance of this Semantic Type.

## Invalid Set ##

An invalid entry is one that is not valid for the detected type and/or Semantic type.

- For Finite plugins, the set of valid values is predefined and hence invalid detection is simply those elements not in the set.  For example the Semantic type COUNTRY.ISO-3166-2 is backed by a list of both current and historically valid two letter country codes, and hence the two letter string 'PP' would be detected as invalid, as would the string 'Unknown'.
- For RegExp plugins, the set of valid patterns is predefined and hence invalid detection is simply any element which does not match the pattern.
- For any fields detected as a known Semantic Type then the invalid entries are based on the particular Semantic Type, for example if the Semantic Type is
detected as a US Phone Number then numbers with invalid area codes or invalid area code exchange pairs will be flagged as invalid.
- For Code plugins, invalid entries may be detected based on a statistical analysis.  For example if there are 100 valid integers and one 'O' (letter O) then the 'O' would be identified as invalid.

Note: The Regular Expression used to detect a Semantic type may differ from the Regular Expression returned by the Semantic Type.  For example
"\\d{3}-\\d{2}-\\d{4}" is used to detect an SSN but the Regular Expression "(?!666|000|9\\d{2})\\d{3}-(?!00)\\d{2}-(?!0{4})\\d{4}" is used to validate and is returned.

## Outlier Set ##

An outlier is a data point that differs significantly from other members of the data set.  There are a set of algorithms used to detect outliers in the input stream.

- In certain cases, where a field is an enumerated type an outlier may be detected based on its Levenshtein distance from one of the other elements in the set. For example, 1000 instances of RED, 1000 instances of BLUE, 1000 instances of GREEN, and one instance of 'GREEEN'.

- In certain cases, where a field is a Long with no Semantic Type identified an outlier may be detected using density-based clustering. For example, a field (with a header of Month) containing many copies of 1-12 and then one instance of 44 would be detected as the Semantic Type MONTH.DIGITS after recognizing the '44' as an outlier.

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

**Data Signature** - Base 64 encoded version of a JSON structure which includes the following attributes: totalCount, sampleCount, matchCount, nullCount, blankCount, minLength, maxLength, cardinality, cardinalityDetail, outlierCardinality, outlierDetail, shapesCardinality, lengthFrequency, shapesDetail, leadingWhiteSpace trailingWhiteSpace, multiline

Additional attributes captured in JSON structure:
- Included if statistics are enabled: min, max, mean, standardDeviation, topK, bottomK
- Included if Base Type is Double: decimalSeparator
- Included if Base Type is Numeric: leadingZeroCount
- Included if Base Type is Date: dateResolutionMode


## Validation and Sample Generation ##

FTA can also be used to validate an input stream either based on known Semantic Types or on Semantic Types detected by FTA.  For example, it is possible to retrieve the LogicalType for a known Semantic Type and then invoke the isValid() method.  This is typically only useful for 'Closed' Semantic Types (isClosed() == true), i.e. those for which there is a known constrained set.  A good example of a closed Semantic Type is the Country code as defined by ISO-3166 Alpha 3.  An example where isValid() would be less useful is FIRST_NAME.  For those cases where the Semantic Type is not one of those known to FTA - the result returned will include a Java Regular Expression which can be used to validate new values.  Refer to the Validation example for further details.

In addition to validating a data Stream, FTA can also be used to generate a synthetic pseudo-random data stream.  For any detected Semantic Type which implements the LTRandom interface it is possible to generate a 'random' element of the Semantic Type by invoking nextRandom(). Refer to the Generation example for further details.

## Address Detection ##

There are multiple Semantic Types associated with addresses:
 - FULL_ADDRESS - A full address, typically this includes a Postal Code (Zip Code) and/or a State/Province as well as a Street number and Street name.
 - STREET_ADDRESS - First line of an address
 - STREET_ADDRESS2 - Second line of an address
 - STREET_ADDRESS3 - Third line of an address
 - STREET_ADDRESS4 - Fourth line of an address
 - STREET_MARKER - The Street qualifier, e.g. Road, Street, Avenue, Boulevard, etc.
 - STREET_NAME - Street name with no number, e.g. Penaton Avenue
 - STREET_NAME_BARE - Street name with no number and no Marker, e.g. Main, Lakeside
 - STREET_NUMBER - House number within street

## Faker ##

To create synthetic data, invoke the CLI with --faker and then a JSON file used to describe the desired output. This JSON description consists of multiple instances of a 'fieldName' and then a specification used to populate data.  For example:

```
cli --faker Simple.json --records 100

With the Simple.json below:

[
  {
    "fieldName": "FirstName",
    "index": 0,
    "type": "NAME.FIRST"
  },
  {
    "fieldName": "LastName",
    "index": 1,
    "type": "NAME.LAST"
  },
  {
    "fieldName": "AGE",
    "index": 2,
    "type": "LONG",
    "low": 18,
    "high": 100,
    "distribution": "gaussian",
    "nullPercent": 0.01
  },
  {
    "fieldName": "Gender",
    "index": 3,
    "type": "STRING",
    "values": [
      "M",
      "F",
      "U"
    ]
  },
  {
    "fieldName": "CreateDate",
    "index": 4,
    "type": "LOCALDATETIME",
    "format": "yyyy.MM.dd HH:mm:ss",
    "low": "2000.01.01 12:00:00",
    "high": "2022.08.08 12:00:00"
  }
]

```

Will produce a file with 100 records with five columns (FirstName,LastName,Age,Gender,CreateDate).

Within the specification the type is required and can either be a Semantic Type or a Base Type.  There are an additional set of optional parameters including:
 - low - the low bound
 - high - the high bound
 - format - the format for outputting this field (e.g. %03d for a LONG)
 - distribution - the distribution of the samples (gaussian, monotonic_increasing, monotonic_decreasing; the default is normal)
 - nullPercent - the percentage of nulls in this field
 - blankPercent - the percentage of blanks in this field
 - values - for an STRING type, the possible set of values can be specified

## Merging Analyses ##
FTA supports merging of analyses run on distinct data shards.  So for example, if part of the data to be profiled resides on one shard and the balance on a separate shard then FTA can be invoked on each shard separately and then merged.  To accomplish this, individual analyses should be executed (with similar configurations), the resulting serialized forms should then be deserialized on a common node and merged. Refer to the Merge example for further details.

The accuracy of the merge is determined by the cardinality of the two individual shards, and falls into one of the the following three cases:
- cardinality(one) + cardinality(two) < max cardinality
- cardinality(one) or cardinality(two) > max cardinality
- cardinality(one) + cardinality(two) >= max cardinality

Assuming all shards have a cardinality less than the maximum cardinality configured there should be no loss of accuracy due to the merge process.

If either shard has a cardinality greater than the maximum cardinality then certain information has already been lost (e.g. uniqueness, distinctCount, cardinality detail).

If the sum of the cardinality is greater than the maximum cardinality but neither individual shard has a cardinality greater than the maximum cardinality then the only attribute that will be indeterminate is the uniqueness of the merged set and clearly the cardinality of the resulting Analysis will be limited to the maximum cardinality.

Note: The input presented to the merged analysis is the union of the data captured by the cardinality detail, outliers detail and the topK and bottomK from each shard, and hence the sampleCount (and matchedCount) on the merged analysis will reflect these numbers.

## Frequently Asked Questions ##

### Why is FTA not detecting the Semantic Type XXX? ###
Beware of synthetic data.  FTA assumes and is tuned to expect real world data.  For example, the 'Phone Numbers' 617.193.9182 and 781.192.1295 look real but have an invalid area code, exchange pair and hence cannot be Phone Numbers.

### Why is FTA not detecting the Semantic Type CITY? ###

CITY is relatively unusual in that it is based  primarily on the header and not on the data. As per the discussion above there are fundamentally three types of plugins (regexp, list-backed, and Java). CITY is a regexp plugin where the Regular Expression is relatively forgiving.

For real word data (see for example https://github.com/tsegall/semantic-types) the detection performance for CITY is ~99.5%.

### Why does sampleCount not reflect the number of samples I provided ###

The sampleCount reflects the number of samples each individual TextAnalyzer has seen. In instances where the cardinality is high and you are merging Analyzes - this will not be the number of samples provided to the individual TextAnalyzers.  If you want the TextAnalysisResult to track the total number of samples observed then you must invoke setTotalCount() on the individual TextAnalyzers prior to merging, FTA will then return (via getTotalCount()) the correct result post merging.

## Releasing a new version ##

### Compile ###
`$ gradle wrapper`

`$ ./gradlew clean build installDist`

### Running Tests ###
All tests and coverage

`$ ./gradlew test jacocoTestReport`

All the examples

`$ ./gradlew examples.clean examples.build examples.run`

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

Then add Tag and Release.

`$ git tag v<VERSION> <COMMIT_ID>`

`$ git push origin v<VERSION>`

`$ gh release create v<VERSION> --notes "Description ..."`

### Executing ###
Using FTA from the command line, list options:

`$ cli/build/install/fta/bin/cli --help`

Analyze a Dutch CSV file:

`$ cli/build/install/fta/bin/cli --locale nl-NL ~/Downloads/sample.csv`

Generate a set of samples:

`$ cli/build/install/fta/bin/cli --faker '[ { "fieldName": "Country", "index": 0, "type": "COUNTRY.TEXT_EN" } ]' --records 20`

## Javadoc ##

Javadoc is automatically updated to reflect the latest release at http://javadoc.io/doc/com.cobber.fta/fta/ and http://javadoc.io/doc/com.cobber.fta/fta-core/ .

## Speed  ##

Indicative speed on an Intel 2.6Ghz i7.

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

During execution a file named &lt;Stream&gt;.fta will be created with the inputs to the analysis.

You can replay the trace file (assuming a local build) using:

`$ cli/build/install/fta/bin/cli --replay <Stream>.fta`

## Background Reading ##

* GitTables: A Large-Scale Corpus of Relational Tables (https://arxiv.org/pdf/2106.07258.pdf)
* Extracting Syntactic Patterns from Databases (https://arxiv.org/abs/1710.11528v2)
* Sherlock: A Deep Learning Approach to Semantic Data Type Detection (https://arxiv.org/pdf/1905.10688.pdf)
* Synthesizing Type-Detection Logic for Rich Semantic Data Types using Open-source Code (https://congyan.org/autotype.pdf)
* T2Dv2 Gold Standard for Matching Web Tables to DBpedia (http://webdatacommons.org/webtables/goldstandardV2.html)
* VizNet Towards a Visualization Learning and Benchmarking Repository (https://viznet.media.mit.edu/)
* Semantic Type Detection: Why It Matters, Current Approaches, and How to Improve It (https://megagon.ai/blog/semantic-type-detection-why-it-matters-current-approaches-and-how-to-improve-it)
* Auto-Type: Synthesizing Type-Detection Logic for Rich Semantic Data Types using Open-source Code (https://www.microsoft.com/en-us/research/publication/synthesizing-type-detection-logic-rich-semantic-data-types-using-open-source-code/)
* DDSketch: A Fast and Fully-Mergeable Quantile Sketch with Relative-Error Guarantees (https://arxiv.org/pdf/1908.10693.pdf)
* A Streaming Parallel Decision Tree Algorithm (https://www.jmlr.org/papers/volume11/ben-haim10a/ben-haim10a.pdf)

