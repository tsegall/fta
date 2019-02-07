# Fast Text Analyzer #

Analyze Text data to determine type information and other key metrics associated with a text stream.
A key objective of the analysis is that it should be sufficiently fast to be in-line (e.g. as the
data is input from some source it should be possible to stream the data through this class without
undue performance degradation).  Support for non-English date detection is relatively robust,
with the following excpetions: No support for non-Gregorian calendars, no support for
non-Arabic numerals and no support for Japanese.

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

`$ gradle wrapper --gradle-version 5.1.1`

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
