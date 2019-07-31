package com.cobber.fta;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestRegExpPlugins {
	@Test
	public void testLatitudeSigned() throws IOException {
		String[] samples = new String[] {
				"51.5", "39.195", "46.18806", "-36.1333333", "33.52056", "39.79", "40.69361", "36.34333", "32.0666667", "48.8833333", "40.71417",
				"51.45", "29.42389", "43.69556", "40.03222", "53.6772222", "45.4166667", "17.3833333", "51.52721", "40.76083", "53.5", "51.8630556",
				"-26.1666667", "32.64", "62.9", "29.61944", "40.71417", "51.52721", "40.61278", "37.22667", "40.71417", "25.77389",
				"46.2333333", "40.65", "52.3333333", "38.96861", "-27.1666667", "33.44833", "29.76306", "43.77222", "43.77222", "34.33806",
				"56.0333333", "41.54278", "29.76306", "26.46111", "51.4", "55.6666667", "33.92417", "53.4247222", "26.12194", "-37.8166667"
		};

		final TextAnalyzer analysis = new TextAnalyzer("Latitude");
		for (String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getRegExp(), "[+-]?\\d+\\.\\d+");
		Assert.assertEquals(result.getTypeQualifier(), "COORDINATE.LATITUDE_DECIMAL");
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void testLatitudeUnsigned() throws IOException {
		String[] samples = new String[] {
				"51.5", "39.195", "46.18806", "36.1333333", "33.52056", "39.79", "40.69361", "36.34333", "32.0666667", "48.8833333", "40.71417",
				"51.45", "29.42389", "43.69556", "40.03222", "53.6772222", "45.4166667", "17.3833333", "51.52721", "40.76083", "53.5", "51.8630556",
				"26.1666667", "32.64", "62.9", "29.61944", "40.71417", "51.52721", "40.61278", "37.22667", "40.71417", "25.77389",
				"46.2333333", "40.65", "52.3333333", "38.96861", "27.1666667", "33.44833", "29.76306", "43.77222", "43.77222", "34.33806",
				"56.0333333", "41.54278", "29.76306", "26.46111", "51.4", "55.6666667", "33.92417", "53.4247222", "26.12194", "37.8166667"
		};

		final TextAnalyzer analysis = new TextAnalyzer("Latitude");
		for (String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getTypeQualifier(), "COORDINATE.LATITUDE_DECIMAL");
		Assert.assertEquals(result.getRegExp(), "[+-]?\\d+\\.\\d+");
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()));
//			Assert.assertTrue(samples[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void testCity() throws IOException {
		String[] samples = new String[] {
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
		for (String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getTypeQualifier(), "CITY");
		Assert.assertEquals(result.getRegExp(), "[-' \\.\\p{IsAlphabetic}]+");
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()), samples[i]);
		}
	}
}
