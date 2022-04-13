/*
 * Copyright 2017-2022 Tim Segall
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
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;

public class TestRegExpPlugins {
	private static final SecureRandom random = new SecureRandom();

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

		final TextAnalyzer analysis = new TextAnalyzer("MAC");
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getRegExp(), "\\p{XDigit}{2}:\\p{XDigit}{2}:\\p{XDigit}{2}:\\p{XDigit}{2}:\\p{XDigit}{2}:\\p{XDigit}{2}");
		assertEquals(result.getTypeQualifier(), "MACADDRESS");
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL })
	public void testRegExpLogicalType_SSN_plus_outlier() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;
		final Set<String> samples = new HashSet<>();
		final TextAnalyzer analysis = new TextAnalyzer("SSN");

		analysis.train("Unknown");

		for (int i = 0; i < SAMPLE_COUNT; i++) {
			final String sample = String.format("%03d-%02d-%04d",
					random.nextInt(1000),  random.nextInt(100), random.nextInt(10000));
			samples.add(sample);
			analysis.train(sample);
		}
		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), SAMPLE_COUNT + 1);
		assertEquals(result.getRegExp(), "\\d{3}-\\d{2}-\\d{4}", result.getRegExp());
		assertEquals(result.getTypeQualifier(), "SSN");
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL })
	public void testRegExpLogicalType_SSN_noPlugin() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;
		final Set<String> samples = new HashSet<>();
		final TextAnalyzer analysis = new TextAnalyzer("SSN");
		analysis.setDefaultLogicalTypes(false);

		for (int i = 0; i < SAMPLE_COUNT; i++) {
			final String sample = String.format("%03d-%02d-%04d",
					random.nextInt(1000),  random.nextInt(100), random.nextInt(10000));
			samples.add(sample);
			analysis.train(sample);
		}
		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), SAMPLE_COUNT);
		assertEquals(result.getRegExp(), "\\d{3}-\\d{2}-\\d{4}", result.getRegExp());
		assertNull(result.getTypeQualifier());
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL })
	public void testRegExpLogicalType_Month() throws IOException, FTAException {
		final String[] samples = {
				"1", "3", "4", "7", "11", "4", "5", "6", "7", "12", "2",
				"3", "5", "6", "10", "11", "10", "3", "5", "2", "1", "12",
				"10", "9", "8", "4", "7" ,"6"
		};

		final TextAnalyzer analysis = new TextAnalyzer("Month");
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getTypeQualifier(), "MONTH.DIGITS");
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
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

		final TextAnalyzer analysis = new TextAnalyzer("Latitude");
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getTypeQualifier(), "COORDINATE.LATITUDE_DECIMAL");
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
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

		final TextAnalyzer analysis = new TextAnalyzer("Latitude");
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getTypeQualifier(), "COORDINATE.LATITUDE_DECIMAL");
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
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

		final TextAnalyzer analysis = new TextAnalyzer("Billing City");
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getTypeQualifier(), "CITY");
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()), sample);
		}
	}
}
