/*
 * Copyright 2017-2024 Tim Segall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cobber.fta;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;

/**
 * Tests StringConverter.
 */
public class TestStringConverter {
	public void baseTest(final String header, final String[] testCases, final Locale locale, final TypeInfo typeInfo) throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer(header);
		analysis.setLocale(locale);

		for (final String testCase : testCases)
			analysis.train(testCase);

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSampleCount(), testCases.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), typeInfo.getBaseType());

		final StringConverter sc = new StringConverter(typeInfo.getBaseType(), new TypeFormatter(typeInfo, analysis.getConfig()));

		for (final String testCase : testCases) {
			// Take the String representation of type typeInfo.getBaseType() and convert it to an equivalent double
			final double d = sc.toDouble(testCase);
			// Take the double and convert it back to the original type and then format it appropriately
			final String s = sc.formatted(sc.fromDouble(d));
			// Must round-trip!
			assertEquals(s, testCase);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DISTRIBUTION })
	void longSigned() throws IOException, FTAException {
		final Locale locale = Locale.forLanguageTag("en-US");
		final KnownTypes knownPatterns = new KnownTypes();
		knownPatterns.initialize(locale);
		final String[] testCases = { "0", "12", "12000", "-12", "-12000" };

		baseTest("longSigned", testCases, locale, knownPatterns.getByID(KnownTypes.ID.ID_SIGNED_LONG));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DISTRIBUTION })
	void longSignedGrouping() throws IOException, FTAException {
		final Locale locale = Locale.forLanguageTag("en-US");
		final KnownTypes knownPatterns = new KnownTypes();
		knownPatterns.initialize(locale);
		final String[] testCases = { "0", "12", "12,000", "-12", "-12,000" };

		baseTest("longSignedGrouping", testCases, locale, knownPatterns.getByID(KnownTypes.ID.ID_SIGNED_LONG_GROUPING));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DISTRIBUTION })
	void doubleSigned() throws IOException, FTAException {
		final Locale locale = Locale.forLanguageTag("en-US");
		final KnownTypes knownPatterns = new KnownTypes();
		knownPatterns.initialize(locale);
		final String[] testCases = { "0.01", "12.03", "12000.01", "-12.34", "-12000.345" };

		baseTest("doubleSigned", testCases, locale, knownPatterns.getByID(KnownTypes.ID.ID_SIGNED_DOUBLE));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DISTRIBUTION })
	void doubleSignedGrouping() throws IOException, FTAException {
		final Locale locale = Locale.forLanguageTag("en-US");
		final KnownTypes knownPatterns = new KnownTypes();
		knownPatterns.initialize(locale);
		final String[] testCases = { "0.01", "12.03", "12,000.01", "-12.34", "-12,000.345" };

		baseTest("doubleSignedGrouping", testCases, locale, knownPatterns.getByID(KnownTypes.ID.ID_SIGNED_DOUBLE_GROUPING));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DISTRIBUTION })
	void localDateTime() throws IOException, FTAException {
		final Locale locale = Locale.forLanguageTag("en-US");
		final KnownTypes knownTypes = new KnownTypes();
		knownTypes.initialize(locale);
		final TypeInfo typeInfo = new TypeInfo(null, null, FTAType.LOCALDATETIME, "MM/dd/yyyy h:mm:ss a", false, "MM/dd/yyyy h:mm:ss a");
		final String[] testCases = { "01/01/1990 8:30:00 AM", "01/31/1990 8:30:00 AM", "01/01/1970 12:00:00 AM", "01/01/1950 12:00:00 AM" };

		baseTest("localDateTime", testCases, locale, typeInfo);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DISTRIBUTION })
	void localDate() throws IOException, FTAException {
		final Locale locale = Locale.forLanguageTag("en-US");
		final KnownTypes knownTypes = new KnownTypes();
		knownTypes.initialize(locale);
		final TypeInfo typeInfo = new TypeInfo(null, null, FTAType.LOCALDATE, "yyyy", false, "yyyy");
		final String[] testCases = {
				"1990", "1984", "1993", "2022", "2011", "2012", "2013", "2014", "2015", "2016",
				"1980", "1974", "1973", "2011", "1914", "1945", "1913", "2046", "2015", "2016",
		};

		baseTest("year", testCases, locale, typeInfo);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DISTRIBUTION })
	void localTime() throws IOException, FTAException {
		final Locale locale = Locale.forLanguageTag("en-US");
		final KnownTypes knownTypes = new KnownTypes();
		knownTypes.initialize(locale);
		final TypeInfo typeInfo = new TypeInfo(null, null, FTAType.LOCALTIME, "H:mm:ss", false, "H:mm:ss");
		final String[] testCases = {
				"21:53:12", "12:53:12", "3:53:12", "18:53:12", "9:53:12", "0:53:12", "15:53:12", "6:53:12", "21:53:12", "12:53:12", "3:53:12", "18:53:12", "9:53:12", "0:53:12", "15:53:12",
				"6:53:12", "21:53:12", "12:53:12", "3:53:12", "18:53:12", "9:53:12", "0:53:12", "15:53:12", "6:53:12", "21:53:12", "12:53:12", "3:53:12", "18:53:12", "9:53:12"
		};

		baseTest("localTime", testCases, locale, typeInfo);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DISTRIBUTION })
	void offsetDateTime() throws IOException, FTAException {
		final Locale locale = Locale.forLanguageTag("en-US");
		final KnownTypes knownTypes = new KnownTypes();
		knownTypes.initialize(locale);
		final TypeInfo typeInfo = new TypeInfo(null, null, FTAType.OFFSETDATETIME, "dd/MM/yyyy HH:mm:ssxx", false, "dd/MM/yyyy HH:mm:ssxx");
		final String[] testCases = {
				"01/12/2018 12:34:44+0000", "12/01/2017 11:23:21-0100", "06/05/1998 18:19:21+0100",
				"31/12/2015 08:05:55-0500", "15/06/2019 23:15:31-0500", "21/02/2000 00:00:00+0000"
		};

		baseTest("offsetDateTime", testCases, locale, typeInfo);
	}

//	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	void zonedDateTime() throws IOException, FTAException {
		final Locale locale = Locale.forLanguageTag("en-US");
		final KnownTypes knownTypes = new KnownTypes();
		knownTypes.initialize(locale);
		final TypeInfo typeInfo = new TypeInfo(null, null, FTAType.ZONEDDATETIME, "MM/dd/yyyy HH:mm:ss z", false, "MM/dd/yyyy HH:mm:ss z");
		final String[] testCases = {
				"01/30/2012 10:59:48 UTC",
				"01/26/2012 10:42:23 GMT",
				"01/26/2012 10:42:23 GMT",
				"01/25/2012 16:46:43 EST",
				"01/25/2012 16:28:42 GMT",
				"01/24/2012 16:53:04 GMT"
		};

		baseTest("zonedDateTime", testCases, locale, typeInfo);
	}
}
