/*
 * Copyright 2017-2025 Tim Segall
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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import org.testng.annotations.Test;

import com.cobber.fta.core.CircularBuffer;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.PatternFG;
import com.cobber.fta.core.RegExpGenerator;

public class TestUtils {
	private static final Random random = new Random(314159);
	public final static double EPSILON = 0.00000001;

	protected final static String validZips = "01770|01772|01773|02027|02030|02170|02379|02657|02861|03216|03561|03848|04066|04281|04481|04671|04921|05072|05463|05761|" +
			"06045|06233|06439|06704|06910|07101|07510|07764|08006|08205|08534|08829|10044|10260|10549|10965|11239|11501|11743|11976|" +
			"12138|12260|12503|12746|12878|13040|13166|13418|13641|13801|14068|14276|14548|14731|14865|15077|15261|15430|15613|15741|" +
			"15951|16210|16410|16662|17053|17247|17516|17765|17951|18109|18428|18702|18957|19095|19339|19486|19808|20043|20170|20370|" +
			"20540|20687|20827|21047|21236|21779|22030|22209|22526|22741|23015|23162|23310|23503|23868|24038|24210|24430|24594|24857|" +
			"25030|25186|25389|25638|25841|26059|26524|26525|26763|27199|27379|27587|27832|27954|28119|28280|28396|28543|28668|28774|" +
			"29111|29329|29475|29622|29744|30016|30119|30234|30343|30503|30643|31002|31141|31518|31727|31901|32134|32277|32455|32617|" +
			"32780|32934|33093|33265|33448|33603|33763|33907|34138|34470|34731|35053|35221|35491|35752|36022|36460|36616|36860|37087|";

	protected final static String validUSStates = "AL|AK|AZ|KY|KS|LA|ME|MD|MI|MA|MN|MS|MO|NE|MT|SD|TN|TX|UT|VT|WI|" +
			"VA|WA|WV|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|NE|NV|" +
			"NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|" +
			"WY|AL|AK|AZ|AR|CA|CO|CT|DC|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|" +
			"MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|RI|SC|SD|" +
			"TX|UT|VT|WV|WI|WY|NV|NH|NJ|OR|PA|RI|SC|AR|CA|CO|CT|ID|HI|IL|IN|";

	protected final static String validCAProvinces = "AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
			"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
			"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
			"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
			"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|";

	protected final static String validAUStates = "ACT|NSW|NT|QLD|SA|TAS|VIC|WA";

	// Set of valid months + 4 x "UNK"
	protected final static String months = "Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|Sep|Jan|Oct|Oct|Oct|" +
			"Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|Sep|Jan|Oct|Oct|Oct|" +
			"Jan|Mar|Jun|Jul|Feb|Dec|UNK|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|UNK|Sep|Jan|Oct|Oct|Oct|" +
			"Jan|UNK|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|May|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|Sep|Jan|Oct|Oct|Oct|" +
			"Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|UNK|Sep|Jan|Oct|Oct|Oct|";

	// Set of valid months + 4 x "UNK"
	protected final static String monthsFrench =
			"janv.|févr.|mars|avr.|mai|juin|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|avr.|mai|juin|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|avr.|mai|juin|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|avr.|mai|juin|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|UNK|mai|juin|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|avr.|mai|juin|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|avr.|mai|UNK|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|UNK|mai|juin|juil.|août|sept.|UNK|nov.|déc.|";

	protected final static String valid3166_2 =  "AL|AW|BZ|BW|BV|BR|IO|BN|BG|" +
			"BF|BI|CV|KH|CF|CK|DM|FK|GE|" +
			"GG|IS|JP|LA|LT|LU|MO|MK|MG|" +
			"MW|MY|MV|ML|MT|MU|MZ|NG|PG|" +
			"RO|WS|SK|SR|TG|TC|TV|UG|UA|" +
			"AE|GB|UM|US|UY|UZ|VG|";

	protected final static String valid3166_3 = "ALA|ARM|BEL|BIH|BWA|BVT|BRA|IOT|BRN|BGR|BFA|" +
			"BDI|CPV|CYM|COG|DJI|ETH|GMB|GTM|HUN|JAM|KGZ|" +
			"LIE|LTU|LUX|MAC|MKD|MDG|MWI|MYS|MDV|MLI|MRT|" +
			"MAR|NER|PAN|REU|VCT|SXM|SDN|TLS|TKM|TCA|TUV|" +
			"UGA|UKR|ARE|GBR|UMI|USA|URY|VNM|";

	protected final static String validUSStreets[] = {
			"9885 Princeton Court",
			"11 San Pablo Rd.",
			"365 3rd St.",
			"426 Brewery Street",
			"676 Thatcher St.",
			"848 Hawthorne St.",
			"788 West Coffee St.",
			"240 Arnold Avenue",
			"25 S. Hawthorne St.",
			"9314 Rose Street",
			"32 West Bellevue St.",
			"8168 Thomas Road",
			"353 Homewood Ave.",
			"14 North Cambridge Street",
			"30 Leeton Ridge Drive",
			"8412 North Mulberry Dr.",
			"7691 Beacon Street",
			"187 Lake View Drive",
			"318 Summerhouse Road",
			"609 Taylor Ave.",
			"47 Broad St.",
			"525 Valley View St.",
			"8 Greenview Ave.",
			"86 North Helen St.",
			"8763 Virginia Street",
			"10 Front Avenue",
			"141 Blue Spring Street",
			"99 W. Airport Ave.",
			"32 NW. Rocky River Ave.",
			"324 North Lancaster Dr."
	};

	protected final static String[] testCaseOptions = {
			"Integer", "Boolean", "Long", "Double", "Date",
			"ISO-3166-3", "ISO-3166-2", "ZIP", "US_STATE", "CA_PROVINCE",
			"US_STREET"
	};

	final static String[] candidatesISO3166_3 = TestUtils.valid3166_3.split("\\|");
	final static String[] candidatesISO3166_2 = TestUtils.valid3166_2.split("\\|");
	final static String[] candidatesZips = TestUtils.validZips.split("\\|");
	final static String[] candidatesUSStates = TestUtils.validUSStates.split("\\|");
	final static String[] candidatesCAProvinces = TestUtils.validCAProvinces.split("\\|");

	static String generateTestStreamValue(final int type) {

		switch (type) {
		case 0:
			// Integer
			return String.valueOf(random.nextInt());
		case 1:
			// Boolean
			return String.valueOf(random.nextBoolean());
		case 2:
			// Long
			return String.valueOf(random.nextLong());
		case 3:
			// Double
			return String.valueOf(random.nextDouble());
		case 4:
			// Date
			return new Date(random.nextLong()).toString();
		case 5:
			// ISO 3166-3
			return candidatesISO3166_3[random.nextInt(candidatesISO3166_3.length)];
		case 6:
			// ISO 3166-2
			return candidatesISO3166_2[random.nextInt(candidatesISO3166_2.length)];
		case 7:
			// Zip Cpde
			return candidatesZips[random.nextInt(candidatesZips.length)];
		case 8:
			// US State
			return candidatesUSStates[random.nextInt(candidatesUSStates.length)];
		case 9:
			// CA Provinces
			return candidatesCAProvinces[random.nextInt(candidatesCAProvinces.length)];
		case 10:
			// US Street
			return TestUtils.validUSStreets[random.nextInt(TestUtils.validUSStreets.length)];
		default:
			return "";
		}
	}

	static String[] generateTestStream(final int type, final int length) {
		final String[] result = new String[length];

		for (int i = 0; i < length; i++)
			result[i] = generateTestStreamValue(type);

		return result;
	}

	static String[] generateTestRecord(final int[] structure) {
		final String[] result = new String[structure.length];

		for (int i = 0; i < structure.length; i++)
			result[i] = generateTestStreamValue(structure[i]);

		return result;
	}

	static String
	getNegativePrefix(final Locale locale) {
		final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);
		final char minusSign = formatSymbols.getMinusSign();
		String negPrefix = "-";
		final NumberFormat nf = NumberFormat.getIntegerInstance(locale);
		if (nf instanceof DecimalFormat) {
			negPrefix = ((DecimalFormat) nf).getNegativePrefix();
			// Ignore the LEFT_TO_RIGHT_MARK if it exists
			if (!negPrefix.isEmpty() && negPrefix.charAt(0) == KnownTypes.LEFT_TO_RIGHT_MARK)
				negPrefix = negPrefix.substring(1);
			if (!negPrefix.isEmpty())
				if (negPrefix.charAt(0) == minusSign && minusSign == '-')
					negPrefix = KnownTypes.OPTIONAL_SIGN;
				else if (negPrefix.charAt(0) == minusSign && minusSign == '\u2212')  // Unicode minus
					negPrefix = KnownTypes.OPTIONAL_UNICODE_SIGN;
				else
					negPrefix = RegExpGenerator.slosh(negPrefix) + "?";
		}
		return negPrefix;
	}

	static String
	getNegativeSuffix(final Locale locale) {
		final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);
		final char minusSign = formatSymbols.getMinusSign();
		String negSuffix = "";
		final NumberFormat nf = NumberFormat.getIntegerInstance(locale);
		if (nf instanceof DecimalFormat) {
			negSuffix = ((DecimalFormat) nf).getNegativeSuffix();
			if (!negSuffix.isEmpty())
				if (negSuffix.charAt(0) == minusSign && minusSign == '-')
					negSuffix = KnownTypes.OPTIONAL_SIGN;
				else if (negSuffix.charAt(0) == minusSign && minusSign == '\u2212')  // Unicode minus
					negSuffix = KnownTypes.OPTIONAL_UNICODE_SIGN;
				else
					negSuffix = RegExpGenerator.slosh(negSuffix) + "?";
		}
		return negSuffix;
	}

	static int
	getGroupingSize(final Locale locale) {
		final NumberFormat nf = NumberFormat.getIntegerInstance(locale);
		if (!nf.isGroupingUsed())
			return -1;
		if (!(nf instanceof DecimalFormat))
			return -1;

		return ((DecimalFormat)nf).getGroupingSize();
	}

	static boolean isValidLocale(final String value) {
		for (final Locale locale : Locale.getAvailableLocales()) {
			if (value.equals(locale.toString())) {
				return true;
			}
		}
		return false;
	}

	protected static void checkSerialization(final TextAnalyzer toCheck) throws FTAException {
		final String originalSerialized = toCheck.serialize();
		final TextAnalyzer hydrated = TextAnalyzer.deserialize(originalSerialized);

		final boolean equals = hydrated.equals(toCheck, EPSILON);

		if (!equals) {
			System.err.println("Original:\n" + originalSerialized);
			System.err.println("Hydrated:\n" + hydrated.serialize());

			fail("hydated != original");
		}
	}

	@Test(groups = { TestGroups.ALL })
	public void testDistanceClose() throws IOException {
		final Set<String> universe = new HashSet<>(Arrays.asList("Primary", "Secondary", "Tertiary", "Secondory"));
		assertEquals(TextAnalyzer.distanceLevenshtein("Secondory", universe), 1);
	}

	@Test(groups = { TestGroups.ALL })
	public void testDistanceFar() throws IOException {
		final Set<String> universe = new HashSet<>(Arrays.asList("Primary", "Secondary", "Tertiary", "Secondory"));
		assertEquals(TextAnalyzer.distanceLevenshtein("Sec", universe), 6);
	}

	@Test(groups = { TestGroups.ALL })
	public void testDistanceHuge() throws IOException {
		final Set<String> universe = new HashSet<>(Arrays.asList("Primary", "Secondary", "Tertiary", "Secondory"));
		assertEquals(TextAnalyzer.distanceLevenshtein("S", universe), 7);
	}

	@Test(groups = { TestGroups.ALL })
	public void testDistanceTelco() throws IOException {
		final Set<String> universe = new HashSet<>(Arrays.asList(
				"DISCONNECT", "DISCONNECT FRACTIONAL", "DISCONNECT OTHE", "DISCONNECT STILL BILLING",
				"INSTALL FRACTIONAL", "INSTALL FRACTIONAL RERATE", "INSTALL OTHER", "RE-RATES", "RUN RATE"
		));
		assertEquals(TextAnalyzer.distanceLevenshtein("INSTALL FRACTIONAL", universe), 7);
	}

	@Test(groups = { TestGroups.ALL })
	public void testDistanceMedia() throws IOException {
		final Set<String> universe = new HashSet<>(Arrays.asList(
				"AUDIO DISC ; VOLUME", "OMPUTER DISC", "ONLINE RESOURCE", "\\QONLINE RESOURCE (EBOOK)\\E",
				"\\QONLINE RESOURCE (EPUB EBOOK)\\E", "\\QONLINE RESOURCE (PDF EBOOK ; EPUB EBOOK)\\E", "SHEET", "VOLUME"
		));
		assertEquals(TextAnalyzer.distanceLevenshtein("\\QONLINE RESOURCE (EBOOK)\\E", universe), 5);
	}

	@Test(groups = { TestGroups.ALL })
	public void testPatternFG() throws IOException {
		PatternFG patternFG = PatternFG.compile("|null|no data|sin dato|");

		assertFalse(patternFG.matcher("ß - Eszett as in straße	"));
		assertTrue(patternFG.matcher("null"));
		assertTrue(patternFG.matcher("NULL"));
		assertFalse(patternFG.matcher("zoom"));
		assertFalse(patternFG.matcher("a"));
		assertTrue(patternFG.matcher("no data"));
		assertTrue(patternFG.matcher("sin dato"));
		assertFalse(patternFG.matcher("long string"));
	}

	@Test(groups = { TestGroups.ALL })
	public void circularBuffer() throws IOException {
		final com.cobber.fta.core.CircularBuffer buffer = new CircularBuffer(2);

		assertFalse(buffer.isFull());
		assertTrue(buffer.add(new String[] {"1"}));
		assertFalse(buffer.isFull());
		assertTrue(buffer.add(new String[] {"2"}));
		assertTrue(buffer.isFull());
		assertFalse(buffer.add(new String[] {"3"}));

		String[] retrieve = buffer.get();
		assertFalse(buffer.isFull());
		assertEquals(retrieve.length, 1);
		assertEquals(retrieve[0], "1");

		retrieve = buffer.get();
		assertFalse(buffer.isFull());
		assertEquals(retrieve.length, 1);
		assertEquals(retrieve[0], "2");

		assert(buffer.get().length == 0);

		assertTrue(buffer.add(new String[] {"0"}));
		for (int i = 1; i < 100; i++) {
			assertTrue(buffer.add(new String[] {String.valueOf(i)}));
			retrieve = buffer.get();
			assertEquals(retrieve[0], String.valueOf(i - 1));
		}
	}

	static int getJavaVersion() {
		final String javaVersion = System.getProperty("java.specification.version");
		if ("1.8".equals(javaVersion))
			return 8;

		return Integer.parseInt(javaVersion);
	}

	protected static LogicalType getLogical(final TextAnalyzer analysis, final String semanticType) throws FTAPluginException {
		if (semanticType == null)
			return null;

		final PluginDefinition pluginDefinition = PluginDefinition.findByName(semanticType);
		final LogicalType logical = LogicalTypeFactory.newInstance(pluginDefinition, analysis.getConfig());

		if (logical instanceof LogicalTypeRegExp && !((LogicalTypeRegExp)logical).isRegExpComplete())
			return null;

		return logical;
	}

	protected static TextAnalysisResult simpleCore(final Sample[] samples, final String header, final Locale locale, final String semanticType, final FTAType type, final Double confidence) throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer(header);
		analysis.setLocale(locale);
		analysis.setDebug(2);
		final AnalysisConfig analysisConfig = analysis.getConfig();

		for (final Sample sample : samples)
			analysis.train(sample.getSample());

		final TextAnalysisResult result = analysis.getResult();
		checkSerialization(analysis);

		final String boundSemanticType = semanticType == null ? null : analysisConfig.bindSemanticType(semanticType);

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getSemanticType(), boundSemanticType);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), type);
		assertEquals(result.getConfidence(), confidence);

		assertNull(result.checkCounts());

		final LogicalType logical = getLogical(analysis, semanticType);

		final String re = result.getRegExp();
		for (final Sample sample : samples) {
			if (sample.isValid()) {
				final String value = sample.getSample();
				assertTrue(value.matches(re), value);
				if (logical != null && logical.isClosed())
					assertTrue(logical.isValid(value), value);
			}
		}

		// If we detected a Semantic Type we should be able to generate samples
		if (logical != null) {
			for (int i = 0; i < 10; i++) {
				final String value = logical.nextRandom();
				assertTrue(logical.isValid(value), value);
			}

			// Hard to validate if these are correct, OTOH good to check that they do not generate an exception
			logical.isClosed();
			logical.isRegExpComplete();
		}


		return result;
	}
}
