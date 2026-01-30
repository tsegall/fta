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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

public class TestRegExpPlugins {
	private static final SecureRandom RANDOM = new SecureRandom();

	@Test(groups = { TestGroups.ALL })
	public void testRegExpLogicalType_MAC() throws IOException, FTAException {
		final String[] samples = {
				"00:0a:95:9d:68:16", "00:0a:94:77:68:16", "00:0a:95:9d:68:16", "00:0a:90:9d:68:16",
				"00:0a:95:9d:68:16", "00:0a:93:8a:68:16", "00:0a:95:9d:60:16", "00:0e:95:9d:68:16",
				"00:0a:95:9d:68:16", "00:0a:95:9d:68:16", "00:0a:95:9d:61:16", "00:0e:92:9d:68:16",
				"00:0b:95:9d:68:16", "00:0a:91:9b:68:16", "00:0a:95:9d:62:16", "00:0e:99:9d:68:16",
				"00:0c:95:9d:68:16", "00:0a:95:9e:68:16", "00:0a:95:9d:64:16", "00:0e:91:9d:68:16",
				"00:0d:95:9d:68:16", "00:0a:90:9d:68:16", "00:0a:95:9d:66:16", "00:0e:94:9d:68:16"
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(samples), "MAC", Locale.US, "MACADDRESS", FTAType.STRING, 1.0);
		assertEquals(result.getRegExp(), "\\p{XDigit}{2}:\\p{XDigit}{2}:\\p{XDigit}{2}:\\p{XDigit}{2}:\\p{XDigit}{2}:\\p{XDigit}{2}");
	}

	@Test(groups = { TestGroups.ALL })
	public void testRegExpLogicalType_MAC_Minus() throws IOException, FTAException {
		final String[] samples = {
				"40-1F-3C-B6-3C-BC", "1E-42-64-5E-54-69", "1F-9C-F0-33-2E-E5", "22-1F-8C-02-75-1D", "9B-D7-6F-02-9F-D5",
				"F7-53-CA-93-12-D4", "51-97-6E-08-B4-F1", "6E-41-38-BA-71-D5", "F7-39-A2-C7-EB-EF", "BD-C2-C1-6C-8D-E9",
				"F7-54-18-C2-F6-78", "47-4C-9C-79-A8-FC", "8C-78-96-5E-92-0F", "ED-3E-2D-BA-B3-14", "AC-4C-1E-41-4B-B6",
				"2E-11-84-3C-6C-30", "98-BF-19-66-EE-59", "48-AB-A4-2E-5C-26", "17-CA-C9-86-25-8E", "2A-60-2E-0A-FA-B9",
				"42-16-BA-40-1E-41", "D4-12-20-67-CF-CA", "0C-21-60-04-B5-99", "5B-06-F2-E6-13-DA", "DE-7C-42-3A-E7-2D",
				"E1-A0-95-4D-39-DF", "E0-0C-F6-F5-15-C3", "CB-7B-BA-ED-B4-2F", "BF-56-A4-90-CD-64", "FB-BE-10-51-9F-57",
				"E1-A5-73-CD-33-51", "0E-7D-E5-82-95-FF", "18-7E-54-4D-A3-A8", "19-C2-D8-68-ED-A2", "9C-76-B8-77-AB-36",
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(samples), "MAC", Locale.US, "MACADDRESS", FTAType.STRING, 1.0);
		assertEquals(result.getRegExp(), "\\p{XDigit}{2}-\\p{XDigit}{2}-\\p{XDigit}{2}-\\p{XDigit}{2}-\\p{XDigit}{2}-\\p{XDigit}{2}");
	}

	@Test(groups = { TestGroups.ALL })
	public void testRegExpLogicalType_SSN_plus_outlier() throws IOException, FTAException {
		final String samples[] = {
				"899-15-7132", "403-63-7601", "449-65-4529", "386-17-2441", "544-48-5289", "001-02-6231", "282-09-6397", "772-89-9633", "732-69-1882", "683-70-7033",
				"804-64-1609", "671-19-4599", "140-04-4156", "136-33-8247", "658-02-4787", "681-85-5591", "314-42-0145", "078-25-1656", "344-13-3607", "307-16-4602",
				"661-10-2799", "803-74-1105", "662-06-2409", "842-24-9681", "700-15-6143", "570-94-4039", "550-71-4562", "143-01-7564", "838-07-4308", "154-03-4450",
				"516-08-9578", "745-18-4866", "659-16-2315", "313-70-3148", "672-41-7763", "226-53-0370", "331-77-2805", "519-85-3603", "232-38-7471", "740-33-4691",
				"094-90-0055", "391-38-0191", "585-06-5483", "653-41-7685", "778-25-3934", "046-37-3176", "576-42-2246", "592-58-9721", "761-62-3901", "620-33-5211",
				"813-06-6968", "631-02-3980", "436-65-4849", "246-31-4425", "237-79-4696", "016-21-6241", "583-62-9331", "004-77-4476", "701-83-9040", "677-55-0322",
				"531-56-8962", "899-70-6700", "124-96-5266", "511-55-6231", "702-80-0175", "756-43-8140", "245-87-7568", "249-70-3875", "737-15-0606", "886-47-2270",
				"191-97-9672", "371-41-1368", "594-01-8226", "885-75-7764", "269-33-2125", "098-97-0916", "070-25-7057", "524-89-3062", "869-16-8530", "342-83-3472",
				"048-55-3586", "233-46-3791", "097-88-1544", "110-50-9774", "260-64-6773", "758-86-3703", "831-29-7907", "362-42-2799", "480-32-4511", "226-62-3754",
				"136-33-7291", "039-42-1364", "510-90-7575", "626-97-6912", "768-85-9118", "654-95-4223", "700-31-1292", "767-20-5141", "063-74-4131", "740-32-9176",
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(samples), "SSN", Locale.US, "SSN", FTAType.STRING, 1.0);
		assertEquals(result.getRegExp(), "(?!666|000|9\\d{2})\\d{3}-(?!00)\\d{2}-(?!0{4})\\d{4}", result.getRegExp());
		final TextAnalyzer analysis = new TextAnalyzer("SSN");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final LogicalType logical = TestUtils.getLogical(analysis, "SSN");
		assertFalse(logical.isValid("510-00-7575"));
	}

	@Test(groups = { TestGroups.ALL })
	public void testRegExpLogicalType_SSN_noPlugin() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;
		final Set<String> samples = new HashSet<>();
		final TextAnalyzer analysis = new TextAnalyzer("SSN");
		analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);

		for (int i = 0; i < SAMPLE_COUNT; i++) {
			final String sample = String.format("%03d-%02d-%04d",
					RANDOM.nextInt(1000),  RANDOM.nextInt(100), RANDOM.nextInt(10000));
			samples.add(sample);
			analysis.train(sample);
		}
		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), SAMPLE_COUNT);
		assertEquals(result.getRegExp(), "\\d{3}-\\d{2}-\\d{4}", result.getRegExp());
		assertNull(result.getSemanticType());
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts(false));

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL })
	public void testRegExpLogicalType_Month() throws IOException, FTAException {
		final String[] samples = {
				"1", "3", "4", "7", "11", "4", "5", "6", "7", "12", "2",
				"3", "5", "6", "10", "11", "10", "3", "5", "2", "1", "12",
				"10", "9", "8", "4", "7" ,"6"
		};

		TestUtils.simpleCore(Sample.allValid(samples), "Month", Locale.US, "MONTH.DIGITS", FTAType.LONG, 1.0);
	}

	@Test(groups = { TestGroups.ALL })
	public void testLatitudeSigned() throws IOException, FTAException {
		final String[] samples = {
				"51.5", "39.195", "46.18806", "-36.1333333", "33.52056", "39.79", "40.69361", "36.34333", "32.0666667", "48.8833333", "40.71417",
				"51.45", "29.42389", "43.69556", "40.03222", "53.6772222", "45.4166667", "17.3833333", "51.52721", "40.76083", "53.5", "51.8630556",
				"-26.1666667", "32.64", "62.9", "29.61944", "40.71417", "51.52721", "40.61278", "37.22667", "40.71417", "25.77389",
				"46.2333333", "40.65", "52.3333333", "38.96861", "-27.1666667", "33.44833", "29.76306", "43.77222", "43.77222", "34.33806",
				"56.0333333", "41.54278", "29.76306", "26.46111", "51.4", "55.6666667", "33.92417", "53.4247222", "26.12194", "-37.8166667"
		};

		TestUtils.simpleCore(Sample.allValid(samples), "Latitude", Locale.US, "COORDINATE.LATITUDE_DECIMAL", FTAType.DOUBLE, 1.0);
	}

	@Test(groups = { TestGroups.ALL })
	public void testLatitudeUnsigned() throws IOException, FTAException {
		final String[] samples = {
				"51.5", "39.195", "46.18806", "36.1333333", "33.52056", "39.79", "40.69361", "36.34333", "32.0666667", "48.8833333", "40.71417",
				"51.45", "29.42389", "43.69556", "40.03222", "53.6772222", "45.4166667", "17.3833333", "51.52721", "40.76083", "53.5", "51.8630556",
				"26.1666667", "32.64", "62.9", "29.61944", "40.71417", "51.52721", "40.61278", "37.22667", "40.71417", "25.77389",
				"46.2333333", "40.65", "52.3333333", "38.96861", "27.1666667", "33.44833", "29.76306", "43.77222", "43.77222", "34.33806",
				"56.0333333", "41.54278", "29.76306", "26.46111", "51.4", "55.6666667", "33.92417", "53.4247222", "26.12194", "37.8166667"
		};

		TestUtils.simpleCore(Sample.allValid(samples), "Latitude", Locale.US, "COORDINATE.LATITUDE_DECIMAL", FTAType.DOUBLE, 1.0);
	}

	@Test(groups = { TestGroups.ALL })
	public void testLatitudeUnsigned_deDE() throws IOException, FTAException {
		final String[] samples = {
				"51,5", "39,195", "46,18806", "36,1333333", "33,52056", "39,79", "40,69361", "36,34333", "32,0666667", "48,8833333", "40,71417",
				"51,45", "29,42389", "43,69556", "40,03222", "53,6772222", "45,4166667", "17,3833333", "51,52721", "40,76083", "53,5", "51,8630556",
				"26,1666667", "32,64", "62,9", "29,61944", "40,71417", "51,52721", "40,61278", "37,22667", "40,71417", "25,77389",
				"46,2333333", "40,65", "52,3333333", "38,96861", "27,1666667", "33,44833", "29,76306", "43,77222", "43,77222", "34,33806",
				"56,0333333", "41,54278", "29,76306", "26,46111", "51,4", "55,6666667", "33,92417", "53,4247222", "26,12194", "37,8166667"
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(samples), "Latitude", Locale.GERMAN, "COORDINATE.LATITUDE_DECIMAL", FTAType.DOUBLE, 1.0);
		assertEquals(result.getMatchCount(), samples.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.ALL })
	public void shapeCacheOverflow() throws IOException, FTAException {
		TextAnalyzer[] analysis = null;
		int rows = 0;

		try (BufferedReader in = new BufferedReader(new InputStreamReader(TestPlugins.class.getResourceAsStream("/MAC.csv"), StandardCharsets.UTF_8))) {
			final CsvReader<NamedCsvRecord> csv = CsvReader.builder().ofNamedCsvRecord(in);

			for (final CloseableIterator<NamedCsvRecord> iter = csv.iterator(); iter.hasNext();) {
				final NamedCsvRecord rowRaw = iter.next();
				final String[] row = rowRaw.getFields().toArray(new String[0]);
				if (rows == 0) {
					final String[] header = rowRaw.getHeader().toArray(new String[0]);
					analysis = new TextAnalyzer[header.length];
					for (int i = 0; i < header.length; i++) {
						analysis[i] = new TextAnalyzer(new AnalyzerContext(header[i], DateResolutionMode.Auto, "Mac.csv", header));
					}
				}
				rows++;
				for (int i = 0; i < row.length; i++) {
					analysis[i].train(row[i]);
				}
			}
		}

		final TextAnalysisResult result = analysis[0].getResult();
		assertEquals(result.getSemanticType(), "MACADDRESS");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByName("MACADDRESS").signature);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getSampleCount(), 1000);
		assertEquals(result.getMatchCount(), 999);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 1);
		assertNull(result.checkCounts(false));
		final Entry<String, Long> only = result.getInvalidDetails().entrySet().iterator().next();
		assertEquals(only.getKey(), "rubbish");
		assertEquals(only.getValue(), 1);
		assertEquals(result.getConfidence(), 0.999);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.ALL })
	public void cardinalityCacheOverflow() throws IOException, FTAException {
		TextAnalyzer[] analysis = null;
		int rows = 0;

		try (BufferedReader in = new BufferedReader(new InputStreamReader(TestPlugins.class.getResourceAsStream("/color.csv"), StandardCharsets.UTF_8))) {
			final CsvReader<NamedCsvRecord> csv = CsvReader.builder().ofNamedCsvRecord(in);

			for (final CloseableIterator<NamedCsvRecord> iter = csv.iterator(); iter.hasNext();) {
				final NamedCsvRecord rowRaw = iter.next();
				final String[] row = rowRaw.getFields().toArray(new String[0]);
				if (rows == 0) {
					final String[] header = rowRaw.getHeader().toArray(new String[0]);
					analysis = new TextAnalyzer[header.length];
					for (int i = 0; i < header.length; i++) {
						analysis[i] = new TextAnalyzer(new AnalyzerContext(header[i], DateResolutionMode.Auto, "color.csv", header));
					}
				}
				rows++;
				for (int i = 0; i < row.length; i++) {
					analysis[i].train(row[i]);
				}
			}
		}

		final TextAnalysisResult result = analysis[0].getResult();
		assertEquals(result.getSemanticType(), "COLOR.HEX");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByName("COLOR.HEX").signature);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getSampleCount(), 20000);
		assertEquals(result.getMatchCount(), 19999);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 1);
		assertNull(result.checkCounts(false));
		final Entry<String, Long> only = result.getInvalidDetails().entrySet().iterator().next();
		assertEquals(only.getKey(), "rubbish");
		assertEquals(only.getValue(), 1);
		assertEquals(result.getConfidence(), 0.99995);
	}

	@Test(groups = { TestGroups.ALL })
	public void testCity() throws IOException, FTAException {
		final String[] samples = {
				"Abbott Park", "Akron", "Alberta", "Allentown", "Allison Park", "Alpharetta", "Alsip", "Alviso", "Andover", "Annapolis Junction",
				"Arlington", "Atlanta", "Austin", "Avon Lake", "Baltimore", "Battle Creek", "Beaverton", "Bella Vista", "Bellaire", "Bellevue",
				"Benton Harbor", "Berkeley Heights", "Bethel", "Bethesda", "Bethesday", "Beverly Hills", "Birmingham", "Bloomington", "Boston", "Brentwood",
				"Bridgewater", "Brussels", "Bruxelles", "Buffalo", "Burbank", "CB Utrecht", "Calhoun", "Cambridge", "Camden", "Canfield",
				"Canonsburg", "Carmel", "Carson", "Cedar Rapids", "Cedarburg", "Charleston", "Charlotte", "Chesapeake", "Chesterfield", "Chicago",
				"Cincinnati", "Clearwater", "Cleveland", "College Station", "Columbus", "Conyers", "Coral Gables", "Corning", "Cupertino", "Dallas",
				"Danville", "Davidson", "Dayton", "Dearborn", "Deerfield", "Denver", "Des Moines", "Des Plaines", "Detroit", "Downers Grove",
				"Doylestown", "Dublin", "Duluth", "Durham", "East Moline", "Eden Prairie", "Edmonton", "Edwardsville", "El Segundo", "Ewing",
				"Farmington", "Findlay", "Flint", "Florham Park", "Flushing", "Foothill Ranch", "Fort Washington", "Fort Wayne", "Fort Worth", "Franklin Lakes",
				"Franklin", "Gainesville", "Galveston", "Gatineau", "Georgetown", "Glen Mills", "Glenview", "Grand Junction", "Grand Rapids", "Greensboro",
				"Greenwich", "Hanover", "Harrisburg", "Hartford", "Hartsville", "Hazelwood", "Helsinki", "Hickory", "Hoboken", "Hoffman Estates",
				"Houston", "Indianapolis", "Irvine", "Irving", "Isleworth", "Issaquah", "Itasca", "Jackson", "Jacksonville", "Juno Beach",
				"Kansas City", "Keene", "Kenilworth", "Kingsport", "Knoxville", "Kohler", "Lake Forest", "Lake Zurich", "Lansing", "Lexington",
				"Libertyville", "Lisle", "Livonia", "London", "Long Beach", "Los Angeles", "Los Gatos", "Louisville", "MADRID", "Malmö",
				"MEMPHIS", "Madison", "Mansfield", "Marlborough", "McLean", "Medina", "Memphis", "Miami", "Middleton", "Midland",
				"Milpitas", "Milwaukee", "Minneapolis", "Minnetonka", "Mississauga", "Modesto", "Moline", "Monroe", "Monterrey", "Montgomery",
				"Montreal", "Morganton", "Morris Plains", "Morristown", "Mount Waverley", "Mountlake Terrace", "München", "NY", "NYC", "Naperville",
				"Naples", "Nashville", "Natick", "Naucalpan de Juarez", "New Albany", "New Brunswick", "New York", "Newport Beach", "Newport News", "Newton Square",
				"North Canton", "North Chicago", "Norwalk", "Oak Brook", "Oakland", "Oklahoma City", "Oklahoma city", "Olympia", "Omaha", "Orlando",
				"Oroville", "Oshkosh", "Owatonna", "Owings Mills", "PARIS CEDEX", "Palatine", "Palm Coast", "Parsippany", "Peoria", "Pewaukee",
				"Philadelphia", "Phoenix", "Piscataway", "Pittsburgh", "Pleasanton", "Portage", "Portland", "Providence", "Purchase", "Raleigh",
				"Rancho Cordova", "Redmond", "Redwood City", "Renville", "Reston", "Richardson", "Richfield", "Richmond", "Richomond", "Riverwoods",
				"Roanoke", "Rochester", "Rockford", "Rolling Meadows", "Rosemont", "Round Rock", "Royal Oak", "Saint Louis", "Saint Paul", "Saint Petersburg",
				"San Antonio", "San Diego", "San Francisco", "San Jose", "San Mateo", "Santa Ana", "Santa Clara", "Sarasota", "Scottsdale", "Sheboygan Falls",
				"Sheboygan", "Sheffield", "Shelton", "Siloam Springs", "Solna", "South San Francisco", "Southfield", "Sparks Glencoe", "Springdale", "Springfield",
				"St Louis", "St. Louis", "St. Paul", "Stamford", "Stevens Point", "Stockholm", "Sugar Land", "Sunnyvale", "Suwanee", "Sydney",
				"Tallahassee", "Tauranga", "Tempe", "The Hague", "Thousand Oaks", "Toledo", "Toronto", "Torrance", "Troy", "Tukwila",
				"Tulsa", "Union", "Uniondale", "Vancouver", "Victoria", "Voorhees", "Vorhees", "WEST PALM BEACH", "WESTBOROUGH", "Waltham",
				"Warren", "Washington", "Waterbury", "Wayzata", "West Chester", "West Des Moines", "West Palm Beach", "Westchester", "Westlake", "White Plains",
				"Wichita", "Wilmington", "Windsor", "Winston Salem", "Woonsocket", "York", "Zaventem", "Zurich", "bellevue"
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(samples), "Billing City", Locale.US, "CITY", FTAType.STRING, 1.0);

		final LogicalTypeCode logical = (LogicalTypeInfinite) LogicalTypeFactory.newInstance(PluginDefinition.findByName("CITY"), new AnalysisConfig(Locale.forLanguageTag("en-US")));

		for (final String sample : samples) {
			if (!sample.matches(result.getRegExp()))
				System.err.printf("Match failed: %s%n", sample);
			if (!logical.isValid(sample))
				System.err.printf("isValid failed: %s%n", sample);
			assertTrue(sample.matches(result.getRegExp()), sample);
		}
		assertEquals(result.getConfidence(), 1.0);
	}
}
