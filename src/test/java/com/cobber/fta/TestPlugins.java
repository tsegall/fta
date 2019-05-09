package com.cobber.fta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.cobber.fta.DateTimeParser.DateResolutionMode;
import com.cobber.fta.plugins.LogicalTypeAddressEN;
import com.cobber.fta.plugins.LogicalTypeCAProvince;
import com.cobber.fta.plugins.LogicalTypeCountryEN;
import com.cobber.fta.plugins.LogicalTypeEmail;
import com.cobber.fta.plugins.LogicalTypeFirstName;
import com.cobber.fta.plugins.LogicalTypeGUID;
import com.cobber.fta.plugins.LogicalTypeGenderEN;
import com.cobber.fta.plugins.LogicalTypeIPAddress;
import com.cobber.fta.plugins.LogicalTypeISO3166_2;
import com.cobber.fta.plugins.LogicalTypeISO3166_3;
import com.cobber.fta.plugins.LogicalTypeISO4217;
import com.cobber.fta.plugins.LogicalTypePhoneNumber;
import com.cobber.fta.plugins.LogicalTypeURL;
import com.cobber.fta.plugins.LogicalTypeUSState;
import com.cobber.fta.plugins.LogicalTypeUSZip5;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

public class TestPlugins {
	@Test
	public void basicGenderTwoValues() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		final String input = "Female|MALE|Male|Female|Female|MALE|Female|Female|Male|" +
				"Male|Female|Male|Male|Male|Female|Female|Male|Male|Male|" +
				"MALE|FEMALE|MALE|FEMALE|FEMALE|MALE|FEMALE|MALE|" +
				"Female|Male|Female|FEMALE|Male|Female|male|Male|Male|male|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(),  LogicalTypeGenderEN.SEMANTIC_TYPE);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE)");
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
		}
	}

	@Test
	public void basicGender() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		final String input = "Female|MALE|Male|Female|Female|MALE|Female|Female|Unknown|Male|" +
				"Male|Female|Male|Male|Male|Female|Female|Male|Male|Male|" +
				"Female|Male|Female|FEMALE|Male|Female|male|Male|Male|male|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeGenderEN.SEMANTIC_TYPE);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE|UNKNOWN)");
		final Map<String, Long> outliers = result.getOutlierDetails();
		long outlierCount = outliers.get("UNKNOWN");
		Assert.assertEquals(result.getMatchCount(), inputs.length - outlierCount);
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());

		LogicalType logicalGender = analysis.getPlugins().getRegistered(LogicalTypeGenderEN.SEMANTIC_TYPE);
		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
			boolean expected = "male".equalsIgnoreCase(inputs[i].trim()) || "female".equalsIgnoreCase(inputs[i].trim());
			Assert.assertEquals(logicalGender.isValid(inputs[i]), expected);
		}
	}

	@Test
	public void basicGenderDE() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		analysis.setLocale(Locale.forLanguageTag("de-AT"));

		final String input = "Female|MALE|Male|Female|Female|MALE|Female|Female|Unknown|Male|" +
				"Male|Female|Male|Male|Male|Female|Female|Male|Male|Male|" +
				"Female|Male|Female|FEMALE|Male|Female|male|Male|Male|male|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE|UNKNOWN)");
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
		}
	}

	@Test
	public void basicPhoneNumber() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Phone");
		final String[] inputs = new String[] {
				"+1 339 223 3709", "(650) 867-3450", "+44 191 4956203", "(650) 450-8810", "(512) 757-6000", "(336) 222-7000", "(014) 427-4427",
				"(785) 241-6200", "(312) 596-1000", "(503) 421-7800", "(520) 773-9050", "+1 617 875 9183", "(212) 842-5500", "(415) 901-7000",
				"+1 781 820 1290", "508.822.8383", "617-426-1400", "+1 781-219-3635"
		};

		for (int i = 0; i < inputs.length; i++)
			analysis.train(inputs[i]);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(),  LogicalTypePhoneNumber.SEMANTIC_TYPE);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), LogicalTypePhoneNumber.REGEXP);
		Assert.assertEquals(result.getOutlierCount(), 1);
		final Map<String, Long> outliers = result.getOutlierDetails();
		long outlierCount = outliers.get("(014) 427-4427");
		Assert.assertEquals(outlierCount, 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getConfidence(), 1.0 - (double)1/result.getSampleCount());

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].trim().matches(result.getRegExp()), inputs[i]);
		}
	}

	@Test
	public void basicGenderWithSpaces() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		final String input = " Female| MALE|Male| Female|Female|MALE |Female |Female |Unknown |Male |" +
				" Male|Female |Male|Male|Male|Female | Female|Male |Male |Male |" +
				" Female|Male |Female|FEMALE|Male| Female| male| Male| Male|  male |";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(),  LogicalTypeGenderEN.SEMANTIC_TYPE);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE|UNKNOWN)");
		final Map<String, Long> outliers = result.getOutlierDetails();
		long outlierCount = outliers.get("UNKNOWN");
		Assert.assertEquals(result.getMatchCount(), inputs.length - outlierCount);
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());

		LogicalType logicalGender = analysis.getPlugins().getRegistered(LogicalTypeGenderEN.SEMANTIC_TYPE);
		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].trim().matches(result.getRegExp()), inputs[i]);
			boolean expected = "male".equalsIgnoreCase(inputs[i].trim()) || "female".equalsIgnoreCase(inputs[i].trim());
			Assert.assertEquals(logicalGender.isValid(inputs[i]), expected);
		}
	}

	@Test
	public void basicGenderTriValue() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		final String input = "Female|MALE|Male|Female|Female|MALE|Female|Female|Unknown|Male|" +
				"Male|Female|Male|Male|Male|Female|Female|Male|Male|Male|" +
				"Unknown|Female|Unknown|Male|Unknown|Female|Unknown|Male|Unknown|Male|" +
				"Female|Male|Female|FEMALE|Male|Female|male|Male|Male|male|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(),  LogicalTypeGenderEN.SEMANTIC_TYPE);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE|UNKNOWN)");
		final Map<String, Long> outliers = result.getOutlierDetails();
		long outlierCount = outliers.get("UNKNOWN");
		Assert.assertEquals(result.getMatchCount(), inputs.length - outlierCount);
		Assert.assertEquals(result.getConfidence(), 1 - (double)outlierCount/result.getSampleCount());

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
		}
	}

	@Test
	public void basicGenderNoDefaults() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		analysis.setDefaultLogicalTypes(false);

		final String input = "Female|MALE|Male|Female|Female|MALE|Female|Female|Unknown|Male|" +
				"Male|Female|Male|Male|Male|Female|Female|Male|Male|Male|" +
				"Female|Male|Female|FEMALE|Male|Female|male|Male|Male|male|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE|UNKNOWN)");
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
		}
	}

	final String[] validCUSIPs = new String[] {
			"000307108", "000307908", "000307958", "000360206", "000360906", "000360956", "000361105", "000361905", "000361955", "000375204", "000375904",
			"000375954", "00081T108", "00081T908", "00081T958", "000868109", "000899104", "00090Q103", "00090Q903", "00090Q953", "000957100", "000957900",
			"000957950", "001084902", "020002101", "020002901", "020002951", "03842B901", "095229100", "171484908", "238661902", "260003108", "260003908",
			"260003958", "29275Y952", "34959E959", "38000Q102", "38000Q902", "38000Q952", "42226A907", "46138E677", "47023A309", "47023A909", "47023A959",
			"470299108", "47030M106", "47102XAH8", "47103U100", "47103U209", "47103U407", "47103U506", "564563104", "659310906", "67000B104", "67000B904",
			"67000B954", "670002AB0", "670002104", "670002904", "670002954", "670008AD3", "684000102", "684000902", "684000952", "72201R403", "74640Y114",
			"800013104", "800013904", "800013954", "80004CAF8", "80007A102", "80007A902", "80007A952", "80007P869", "80007P909", "80007P959", "80007T101",
			"000957950", "001084902", "020002101", "020002901", "020002951", "03842B901", "095229100", "171484908", "238661902", "260003108", "260003908",
			"260003958", "29275Y952", "34959E959", "38000Q102", "38000Q902", "38000Q952", "42226A907", "46138E677", "47023A309", "47023A909", "47023A959",
			"470299108", "47030M106", "47102XAH8", "47103U100", "47103U209", "47103U407", "47103U506", "564563104", "659310906", "67000B104", "67000B904",
			"67000B954", "670002AB0", "670002104", "670002904", "670002954", "670008AD3", "684000102", "684000902", "684000952", "72201R403", "74640Y114",
			"80007T901", "80007T951", "80007V106", "80007V906", "80007V956", "80283M901", "87236Y908", "91705J204", "97717W904"

	};

	@Test
	public void testRegisterFinite() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("CUSIP");
		analysis.setMaxCardinality(20000);
		List<PluginDefinition> plugins = new ArrayList<>();
		PluginDefinition plugin = new PluginDefinition("CUSIP", "com.cobber.fta.PluginCUSIP");
		plugin.hotWords = new String[] { "CUSIP" };
		plugins.add(plugin);

		try {
			analysis.getPlugins().registerPluginList(plugins, "C U S I P", null);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		for (int i = 0; i < validCUSIPs.length; i++)
			analysis.train(validCUSIPs[i]);

		analysis.train("666666666");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), PluginCUSIP.REGEXP);
		Assert.assertEquals(result.getTypeQualifier(), "CUSIP");
		Assert.assertEquals(result.getSampleCount(), validCUSIPs.length + 1);
		Assert.assertEquals(result.getMatchCount(), validCUSIPs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getMinLength(), 9);
		Assert.assertEquals(result.getMaxLength(), 9);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());

		int matchCount = 1;
		for (int i = 0; i < validCUSIPs.length; i++) {
			if (validCUSIPs[i].matches(result.getRegExp()))
				matchCount++;
		}
		Assert.assertEquals(matchCount, result.getMatchCount() + 1);
	}

	@Test
	public void testRegisterInfinite() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("CC");
		List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(new PluginDefinition("CUSIP", "com.cobber.fta.PluginCreditCard"));

		try {
			analysis.getPlugins().registerPluginList(plugins, "Ignore", null);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		final String[] input = {
//				"Credit Card Type,Credit Card Number",
				"American Express,378282246310005",
				"American Express,371449635398431",
				"American Express Corporate,378734493671000 ",
				"Australian BankCard,5610591081018250",
				"Diners Club,30569309025904",
				"Diners Club,38520000023237",
				"Discover,6011111111111117",
				"Discover,6011000990139424",
				"JCB,3530111333300000",
				"JCB,3566002020360505",
				"MasterCard,5555555555554444",
				"MasterCard,5105105105105100",
				"Visa,4111111111111111",
				"Visa,4012888888881881",
				"Visa,4222222222222",
//				"Dankort (PBS),76009244561",
				"Dankort (PBS),5019717010103742",
				"Switch/Solo (Paymentech),6331101999990016",
				"MasterCard,5555 5555 5555 4444",
				"MasterCard,5105 1051 0510 5100",
				"Visa,4111 1111 1111 1111",
				"Visa,4012 8888 8888 1881",
				"MasterCard,5555-5555-5555-4444",
				"MasterCard,5105-1051-0510-5100",
				"Visa,4111-1111-1111-1111",
				"Visa,4012-8888-8888-1881"
		};

		Set<String>  samples = new HashSet<String>();

		for (int i = 0; i < input.length; i++) {
			String s = input[i].split(",")[1];
			samples.add(s);
			analysis.train(s);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "CREDITCARD");
		Assert.assertEquals(result.getSampleCount(), input.length);
		Assert.assertEquals(result.getMatchCount(), input.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getMinLength(), 13);
		Assert.assertEquals(result.getMaxLength(), 19);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertTrue(result.isLogicalType());
		Assert.assertEquals(result.getRegExp(), PluginCreditCard.REGEXP);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (String s : samples) {
			Assert.assertTrue(s.matches(result.getRegExp()), s);
		}
	}

	@Test
	public void basicGUID() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("GUID");
		final String[] inputs = new String[] {
				"DAA3EDDE-5BCF-4D2A-8FB0-E120089343AF",
				"B0613BE8-88AF-4591-A9A0-059F80413212",
				"063BB913-7287-4A8A-B3DF-41EAA0EABF49",
				"B6011DC1-C4A3-4130-AD42-C3EA2BA35F8B",
				"327B2624-2467-4461-8CA3-2DCB30D06683",
				"BDC94786-4016-4C7A-85F7-A7558425FA26",
				"0525CA73-9A48-497A-AC2D-2596BFE66FF7",
				"88BD42BA-B4F2-4E9E-8BD3-6846F6692E44",
				"1456E784-D404-4864-BBD3-691988220732",
				"FF2B0C44-2277-4EB1-BB25-32CF23181672",
				"929945CC-E4AA-4FEA-BFD6-43B774C9FB05",
				"BC2D3965-24A5-4CC7-986A-99B869925ACD",
				"7C9C9A6C-0A38-41B6-A999-A9A4218D43FA",
				"3324F2BF-9CC6-446A-A02D-DDE2F2ECF31F",
				"F17AA339-5DCE-4318-9B1C-C95255D4C5CC",
				"D67F9D81-DBE7-4214-849F-41B937C628AB",
				"9892D51B-C490-4B6E-8DF0-B032BAAB0476",
				"6CBD3302-F067-4378-8955-CD57EA5E83EB",
				"BEDFFAF8-9E35-4155-A337-7981BA349E7B",
				"37285247-D431-4381-AC5F-7C3136E276C2",
				"6D490537-AA7B-45C5-BEDB-8572EBDEFD15",
				"51e55fd6-74ca-4b1d-b5fd-d210209e3fc4"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "GUID");
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getRegExp(), LogicalTypeGUID.REGEXP);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
		}
	}

	final String validEmails = "Bachmann@lavastorm.com|Biedermann@lavastorm.com|buchheim@lavastorm.com|" +
			"coleman@lavastorm.com|Drici@lavastorm.com|Garvey@lavastorm.com|jackson@lavastorm.com|" +
			"Jones@lavastorm.com|Marinelli@lavastorm.com|Nason@lavastorm.com|Parker@lavastorm.com|" +
			"Pigneri@lavastorm.com|Rasmussen@lavastorm.com|Regan@lavastorm.com|Segall@Lavastorm.com|" +
			"Pigneri2@lavastorm.com|ahern@lavastorm.com|reginald@lavastorm.com|blumfontaine@Lavastorm.com|" +
			"Smith@lavastorm.com|Song@lavastorm.com|Tolleson@lavastorm.com|wynn@lavastorm.com|" +
			"Ahmed@lavastorm.com|Benoit@lavastorm.com|Keane@lavastorm.com|Kilker@lavastorm.com|" +
			"Waters@lavastorm.com|Meagher@lavastorm.com|Mok@lavastorm.com|Mullin@lavastorm.com|" +
			"Nason@lavastorm.com|reilly@lavastorm.com|Scoble@lavastorm.com|Comerford@lavastorm.com|" +
			"Gallagher@lavastorm.com|Hughes@lavastorm.com|Kelly@lavastorm.com|" +
			"Tuddenham@lavastorm.com|Williams@lavastorm.com|Wilson@lavastorm.com|";

	@Test
	public void basicEmail() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = validEmails.split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train("tim@cobber com");
		analysis.train("tim@cobber com");
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 2 + result.getNullCount());
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "EMAIL");
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), LogicalTypeEmail.REGEXP);
		Assert.assertEquals(result.getConfidence(), 1 - (double)2/(result.getSampleCount() - result.getNullCount()));

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
		}
	}

	@Test
	public void degenerativeEmail() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = validEmails + validEmails + validEmails + validEmails + "ask|not|what|your|country|can|";
		final String inputs[] = input.split("\\|");
		final int ERRORS = 6;
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + result.getNullCount());
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "EMAIL");
		Assert.assertEquals(result.getMatchCount(), inputs.length - ERRORS);
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), LogicalTypeEmail.REGEXP);
		Assert.assertEquals(result.getConfidence(), 1 - (double)ERRORS/(result.getSampleCount() - result.getNullCount()));

		int matches = 0;
		for (int i = 0; i < inputs.length; i++)
			if (inputs[i].matches(result.getRegExp()))
				matches++;

		Assert.assertEquals(matches, result.getMatchCount());
	}

	private final static String INPUT_URLS = "http://www.lavastorm.com|ftp://ftp.sun.com|https://www.google.com|" +
			"https://www.homedepot.com|http://www.lowes.com|http://www.apple.com|http://www.sgi.com|" +
			"http://www.ibm.com|http://www.snowgum.com|http://www.zaius.com|http://www.cobber.com|" +
			"http://www.ey.com|http://www.zoomer.com|http://www.redshift.com|http://www.segall.net|" +
			"http://www.sgi.com|http://www.united.com|https://www.hp.com/printers/support|http://www.opinist.com|" +
			"http://www.java.com|http://www.slashdot.org|http://theregister.co.uk|";

	@Test
	public void basicURL() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = INPUT_URLS.split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);
		analysis.train("bogus");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 1 + result.getNullCount());
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length + 1 - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), LogicalTypeURL.REGEXP_PROTOCOL + LogicalTypeURL.REGEXP_RESOURCE);
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - result.getNullCount()));
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeURL.SEMANTIC_TYPE);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicURLResource() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = INPUT_URLS.split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i].substring(inputs[i].indexOf("://") + 3)) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + result.getNullCount());
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), LogicalTypeURL.REGEXP_RESOURCE);
		Assert.assertEquals(result.getConfidence(), 0.95);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeURL.SEMANTIC_TYPE);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicURLMixed() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = INPUT_URLS.split("\\|");

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (i % 2 == 1)
				analysis.train(inputs[i]);
			else analysis.train(inputs[i].substring(inputs[i].indexOf("://") + 3));
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length + result.getNullCount());
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), LogicalTypeURL.REGEXP_PROTOCOL + "?" + LogicalTypeURL.REGEXP_RESOURCE);
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeURL.SEMANTIC_TYPE);

		for (int i = 0; i < inputs.length; i++)
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
	}

	@Test
	public void backoutURL() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = INPUT_URLS.split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final int badURLs = 50;
		for (int i = 0; i < badURLs; i++)
			analysis.train(String.valueOf(i));

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length + badURLs + result.getNullCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getOutlierCount(), 0);
//		Assert.assertEquals(result.getMatchCount(), inputs.length + badURLs + result.getNullCount());
		Assert.assertEquals(result.getRegExp(), KnownPatterns.freezeANY(1, 35, 1, 35, result.getLeadingWhiteSpace(), result.getTrailingWhiteSpace(), result.getMultiline()));
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void notEmail() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "2@3|3@4|b4@5|" +
				"6@7|7@9|12@13|100@2|" +
				"Zoom@4|Marinelli@44|55@90341|Parker@46|" +
				"Pigneri@22|Rasmussen@77|478 @ 1912|88 @ LC|" +
				"Smith@99|Song@88|77@|@lavastorm.com|" +
				"Tuddenham@02421|Williams@uk|Wilson@99";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train("tim@cobber com");
		analysis.train("tim@cobber com");
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 2 + result.getNullCount());
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.freezeANY(3, 15, 3, 15, result.getLeadingWhiteSpace(), result.getTrailingWhiteSpace(), result.getMultiline()));
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getMatchCount(), inputs.length + 2);
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicZip() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = TestUtils.validZips.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeUSZip5.SEMANTIC_TYPE);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 32);
		Assert.assertEquals(result.getRegExp(), LogicalTypeUSZip5.REGEXP);
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void randomZip() throws IOException {
		PluginDefinition plugin = new PluginDefinition("ZIP", "com.cobber.fta.plugins.LogicalTypeUSZip5");
		LogicalTypeCode logical = LogicalTypeCode.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test
	public void randomGUID() throws IOException {
		PluginDefinition plugin = new PluginDefinition("GUID", "com.cobber.fta.plugins.LogicalTypeGUID");
		LogicalTypeCode logical = LogicalTypeCode.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test
	public void randomGender() throws IOException {
		PluginDefinition plugin = new PluginDefinition("GENDER", "com.cobber.fta.plugins.LogicalTypeGenderEN");
		LogicalTypeCode logical = LogicalTypeCode.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test
	public void randomCountry() throws IOException {
		PluginDefinition plugin = new PluginDefinition("COUNTRY.TEXT_EN", "com.cobber.fta.plugins.LogicalTypeCountryEN");
		LogicalTypeCode logical = LogicalTypeCode.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++) {
			String example = logical.nextRandom();
			Assert.assertTrue(example.matches(logical.getRegExp()));
			Assert.assertTrue(logical.isValid(example.toLowerCase()), example);
		}
	}

	@Test
	public void random3166_2() throws IOException {
		PluginDefinition plugin = new PluginDefinition("COUNTRY.ISO-3166-2", "com.cobber.fta.plugins.LogicalTypeISO3166_2");
		LogicalTypeCode logical = LogicalTypeCode.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test
	public void random3166_3() throws IOException {
		PluginDefinition plugin = new PluginDefinition("COUNTRY.ISO-3166-3", "com.cobber.fta.plugins.LogicalTypeISO3166_3");
		LogicalTypeCode logical = LogicalTypeCode.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test
	public void random4217() throws IOException {
		PluginDefinition plugin = new PluginDefinition("CURRENCY_CODE.ISO-4217", "com.cobber.fta.plugins.LogicalTypeISO4217");
		LogicalTypeCode logical = LogicalTypeCode.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}


	@Test
	public void randomIPAddress() throws IOException {
		PluginDefinition plugin = new PluginDefinition("IPADDRESS.IPV4", "com.cobber.fta.plugins.LogicalTypeIPAddress");
		LogicalTypeCode logical = LogicalTypeCode.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test
	public void randomPhoneNumber() throws IOException {
		PluginDefinition plugin = new PluginDefinition("PHONENUMBER", "com.cobber.fta.plugins.LogicalTypePhoneNumber");
		LogicalTypeCode logical = LogicalTypeCode.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test
	public void randomEmail() throws IOException {
		PluginDefinition plugin = new PluginDefinition("EMAIL", "com.cobber.fta.plugins.LogicalTypeEmail");
		LogicalTypeCode logical = LogicalTypeCode.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test
	public void randomFirst() throws IOException {
		PluginDefinition plugin = new PluginDefinition("FIRST_NAME", "com.cobber.fta.plugins.LogicalTypeFirstName");
		LogicalTypeCode logical = LogicalTypeCode.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test
	public void randomLast() throws IOException {
		PluginDefinition plugin = new PluginDefinition("LAST_NAME", "com.cobber.fta.plugins.LogicalTypeLastName");
		LogicalTypeCode logical = LogicalTypeCode.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++) {
			String example = logical.nextRandom();
			Assert.assertTrue(example.matches(logical.getRegExp()));
			Assert.assertTrue(logical.isValid(example));
		}
	}

	@Test
	public void randomURL() throws IOException {
		PluginDefinition plugin = new PluginDefinition("URL", "com.cobber.fta.plugins.LogicalTypeURL");
		LogicalTypeCode logical = LogicalTypeCode.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test
	public void randomIATA() throws IOException {
		PluginDefinition plugin = new PluginDefinition("URL", "com.cobber.fta.plugins.LogicalTypeIATA");
		LogicalTypeCode logical = LogicalTypeCode.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++) {
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
		}
	}

	@Test
	public void testRegister() throws IOException {
		TextAnalyzer analyzer = new TextAnalyzer();

		analyzer.registerDefaultPlugins("Magic Code", null);

		LogicalType logical = analyzer.getPlugins().getRegistered(LogicalTypeURL.SEMANTIC_TYPE);

		String valid = "http://www.infogix.com";
		String invalid = "www infogix.com";

		Assert.assertTrue(logical.isValid(valid));
		Assert.assertFalse(logical.isValid(invalid));

		logical = analyzer.getPlugins().getRegistered(LogicalTypeCountryEN.SEMANTIC_TYPE);

		String ChinaUpper = "CHINA";
		Assert.assertTrue(logical.isValid(ChinaUpper));

		String ChinaWithSpaces = "  CHINA  ";
		Assert.assertTrue(logical.isValid(ChinaWithSpaces));

		String ChinaCamel = "China";
		Assert.assertTrue(logical.isValid(ChinaCamel));

		String Lemuria = "Lemuria";
		Assert.assertFalse(logical.isValid(Lemuria));
	}

	@Test
	public void basicZipHeader() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("BillingPostalCode");
		final String inputs[] = new String[] {
			"", "", "", "", "", "", "", "", "", "27215", "75251", "66045", "", "",
			"", "", "", "", "94087", "", "", "", "", "", "", "", "", "", "", ""
		};

		for (int i = 0; i < inputs.length; i++)
			analysis.train(inputs[i]);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeUSZip5.SEMANTIC_TYPE);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 4);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 0);
		Assert.assertEquals(result.getRegExp(), LogicalTypeUSZip5.REGEXP);
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void basicIPAddress() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("BillingPostalCode");
		final String inputs[] = new String[] {
			"8.8.8.8", "4.4.4.4", "1.1.1.1", "172.217.4.196", "192.168.86.1", "64.68.200.46", "23.45.133.21",
			"15.73.4.77"
		};

		for (int i = 0; i < inputs.length; i++)
			analysis.train(inputs[i]);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeIPAddress.SEMANTIC_TYPE);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 0);
		Assert.assertEquals(result.getRegExp(), LogicalTypeIPAddress.REGEXP);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++)
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
	}

	@Test
	public void basicZipHeaderDE() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("BillingPostalCode");
		analysis.setLocale(Locale.forLanguageTag("de-AT"));
		final String inputs[] = new String[] {
			"", "", "", "", "", "", "", "", "", "27215", "75251", "66045", "", "",
			"", "", "", "", "94087", "", "", "", "", "", "", "", "", "", "", ""
		};

		for (int i = 0; i < inputs.length; i++)
			analysis.train(inputs[i]);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 4);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 0);
		Assert.assertEquals(result.getRegExp(), LogicalTypeUSZip5.REGEXP);
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void zipUnwind() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "02421|02420|02421|02420|02421|02420|02421|02420|02421|02420|" +
				"02421|02420|02421|02420|02421|02420|02421|02420|02421|02420|" +
				"10248|10249|10250|10251|10252|10253|10254|10255|10256|10257|10258|10259|10260|10261|10262|10263|10264|" +
				"bogus|" +
						"10265|10266|10267|10268|10269|10270|10271|10272|10273|10274|10275|10276|10277|10278|10279|10280|10281|" +
						"10282|10283|10284|10285|10286|10287|10288|10289|10290|10291|10292|10293|10294|10295|10296|10297|10298|" +
						"10299|10300|10301|10302|10303|10304|10305|10306|10307|10308|10309|10310|10311|10312|10313|10314|10315|" +
						"10316|10317|10318|10319|10320|10321|10322|10323|10324|10325|10326|10327|10328|10329|10330|10331|10332|" +
						"10333|10334|10335|10336|10337|10338|10339|10340|10341|10342|10343|10344|10345|10346|10347|10348|10349|" +
						"10350|10351|10352|10353|10354|10355|10356|10357|10358|10359|10360|10361|10362|10363|10364|10365|10366|" +
						"10367|10368|10369|10370|10371|10372|10373|10374|10375|10376|10377|10378|10379|10380|10381|10382|10383|" +
						"10384|10385|10386|10387|10388|10389|10390|10391|10392|10393|10394|10395|10396|10397|10398|10399|10400|" +
						"10401|10402|10403|10404|10405|10406|10407|10408|10409|10410|10411|10412|10413|10414|10415|10416|10417|" +
						"10418|10419|10420|10421|10422|10423|10424|10425|10426|10427|10428|10429|10430|10431|10432|10433|10434|" +
						"10435|10436|10437|10438|10439|10440|10441|10442|10443|10444|10445|10446|10447|10448|10449|10450|10451|" +
						"10452|10453|10454|10455|10456|10457|10458|10459|10460|10461|10462|10463|10464|10465|10466|10467|10468|" +
						"10469|10470|10471|10472|10473|10474|10475|10476|10477|10478|10479|10480|10481|10482|10483|10484|10485|" +
						"10486|10487|10488|10489|10490|10491|10492|10493|10494|10495|10496|10497|10498|10499|10500|10501|10502|" +
						"10503|10504|10505|10506|10507|10508|10509|10510|10511|10512|10513|10514|10515|10516|10517|10518|10519|" +
						"10520|10521|10522|10523|10524|10525|10526|10527|10528|10529|10530|10531|10532|10533|10534|10535|10536|" +
						"10537|10538|10539|10540|10541|10542|10543|10544|10545|10546|10547|10548|10549|10550|10551|10552|10553|" +
						"10554|10555|10556|10557|10558|10559|10560|10561|10562|10563|10564|10565|10566|10567|10568|10569|10570|" +
						"10571|10572|10573|10574|10575|10576|10577|10578|10579|10580|10581|10582|10583|10584|10585|10586|10587|" +
						"10588|10589|10590|10591|10592|10593|10594|10595|10596|10597|10598|10599|10600|10601|10602|10603|10604|" +
						"10605|10606|10607|10608|10609|10610|10611|10612|10613|10614|10615|10616|10617|10618|10619|10620|10621|" +
						"10622|10623|10624|10625|10626|10627|10628|10629|10630|10631|10632|10633|10634|10635|10636|10637|10638|" +
						"10639|10640|10641|10642|10643|10644|10645|10646|10647|10648|10649|10650|10651|10652|10653|10654|10655|" +
						"10656|10657|10658|10659|10660|10661|10662|10663|10664|10665|10666|10667|10668|10669|10670|10671|10672|" +
						"10673|10674|10675|10676|10677|10678|10679|10680|10681|10682|10683|10684|10685|10686|10687|10688|10689|" +
						"10690|10691|10692|10693|10694|10695|10696|10697|10698|10699|10700|10701|10702|10703|10704|10705|10706|" +
						"10707|10708|10709|10710|10711|10712|10713|10714|10715|10716|10717|10718|10719|10720|10721|10722|10723|" +
						"10724|10725|10726|10727|10728|10729|10730|10731|10732|10733|10734|10735|10736|10737|10738|10739|10740|" +
						"10741|10742|10743|10744|10745|10746|10747|10748|10749|10750|10751|10752|10753|10754|10755|10756|10757|" +
						"10758|10759|10760|10761|10762|10763|10764|10765|10766|10767|10768|10769|10770|10771|10772|10773|10774|" +
						"10775|10776|10777|10778|10779|10780|10781|10782|10783|10784|10785|10786|10787|10788|10789|10790|10791|" +
						"10792|10793|10794|10795|10796|10797|10798|10799|10800|10801|10802|10803|10804|10805|10806|10807|10808|" +
						"10809|10810|10811|10812|10813|10814|10815|10816|10817|10818|10819|10820|10821|10822|10823|10824|10825|" +
						"10826|10827|10828|10829|10830|10831|10832|10833|10834|10835|10836|10837|10838|10839|10840|10841|10842|" +
						"10843|10844|10845|10846|10847|10848|10849|10850|10851|10852|10853|10854|10855|10856|10857|10858|10859|" +
						"10860|10861|10862|10863|10864|10865|10866|10867|10868|10869|10870|10871|10872|10873|10874|10875|10876|" +
						"10877|10878|10879|10880|10881|10882|10883|10884|10885|10886|10887|10888|10889|10890|10891|10892|10893|" +
						"10894|10895|10896|10897|10898|10899|10900|10901|10902|10903|10904|10905|10906|10907|10908|10909|10910|" +
						"10911|10912|10913|10914|10915|10916|10917|10918|10919|10920|10921|10922|10923|10924|10925|10926|10927|" +
						"10928|10929|10930|10931|10932|10933|10934|10935|10936|10937|10938|10939|10940|10941|10942|10943|10944|" +
						"10945|10946|10947|10948|10949|10950|10951|10952|10953|10954|10955|10956|10957|10958|10959|10960|10961|" +
						"10962|10963|10964|10965|10966|10967|10968|10969|10970|10971|10972|10973|10974|10975|10976|10977|10978|" +
						"10979|10980|10981|10982|10983|10984|10985|10986|10987|10988|10989|10990|10991|10992|10993|10994|10995|" +
						"10996|10997|10998|10999|11000|11001|11002|11003|11004|11005|11006|11007|11008|11009|11010|11011|11012|" +
						"11013|11014|11015|11016|11017|11018|11019|11020|11021|11022|11023|11024|11025|11026|11027|11028|11029|" +
						"11030|11031|11032|11033|11034|11035|11036|11037|11038|11039|11040|11041|11042|11043|11044|11045|11046|" +
						"11047|11048|11049|11050|11051|11052|11053|11054|11055|11056|11057|11058|11059|11060|11061|11062|11063|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length - 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 20);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());

		int matches = 0;
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i].matches(result.getRegExp()))
					matches++;
		}
		Assert.assertEquals(result.getMatchCount(), matches);
	}

	@Test
	public void zipNotReal() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input =
				"10248|10249|10250|10251|10252|10253|10254|10255|10256|10257|10258|10259|10260|10261|10262|10263|10264|" +
						"10265|10266|10267|10268|10269|10270|10271|10272|10273|10274|10275|10276|10277|10278|10279|10280|10281|" +
						"10282|10283|10284|10285|10286|10287|10288|10289|10290|10291|10292|10293|10294|10295|10296|10297|10298|" +
						"10299|10300|10301|10302|10303|10304|10305|10306|10307|10308|10309|10310|10311|10312|10313|10314|10315|" +
						"10316|10317|10318|10319|10320|10321|10322|10323|10324|10325|10326|10327|10328|10329|10330|10331|10332|" +
						"10333|10334|10335|10336|10337|10338|10339|10340|10341|10342|10343|10344|10345|10346|10347|10348|10349|" +
						"10350|10351|10352|10353|10354|10355|10356|10357|10358|10359|10360|10361|10362|10363|10364|10365|10366|" +
						"10367|10368|10369|10370|10371|10372|10373|10374|10375|10376|10377|10378|10379|10380|10381|10382|10383|" +
						"10384|10385|10386|10387|10388|10389|10390|10391|10392|10393|10394|10395|10396|10397|10398|10399|10400|" +
						"10401|10402|10403|10404|10405|10406|10407|10408|10409|10410|10411|10412|10413|10414|10415|10416|10417|" +
						"10418|10419|10420|10421|10422|10423|10424|10425|10426|10427|10428|10429|10430|10431|10432|10433|10434|" +
						"10435|10436|10437|10438|10439|10440|10441|10442|10443|10444|10445|10446|10447|10448|10449|10450|10451|" +
						"10452|10453|10454|10455|10456|10457|10458|10459|10460|10461|10462|10463|10464|10465|10466|10467|10468|" +
						"10469|10470|10471|10472|10473|10474|10475|10476|10477|10478|10479|10480|10481|10482|10483|10484|10485|" +
						"10486|10487|10488|10489|10490|10491|10492|10493|10494|10495|10496|10497|10498|10499|10500|10501|10502|" +
						"10503|10504|10505|10506|10507|10508|10509|10510|10511|10512|10513|10514|10515|10516|10517|10518|10519|" +
						"10520|10521|10522|10523|10524|10525|10526|10527|10528|10529|10530|10531|10532|10533|10534|10535|10536|" +
						"10537|10538|10539|10540|10541|10542|10543|10544|10545|10546|10547|10548|10549|10550|10551|10552|10553|" +
						"10554|10555|10556|10557|10558|10559|10560|10561|10562|10563|10564|10565|10566|10567|10568|10569|10570|" +
						"10571|10572|10573|10574|10575|10576|10577|10578|10579|10580|10581|10582|10583|10584|10585|10586|10587|" +
						"10588|10589|10590|10591|10592|10593|10594|10595|10596|10597|10598|10599|10600|10601|10602|10603|10604|" +
						"10605|10606|10607|10608|10609|10610|10611|10612|10613|10614|10615|10616|10617|10618|10619|10620|10621|" +
						"10622|10623|10624|10625|10626|10627|10628|10629|10630|10631|10632|10633|10634|10635|10636|10637|10638|" +
						"10639|10640|10641|10642|10643|10644|10645|10646|10647|10648|10649|10650|10651|10652|10653|10654|10655|" +
						"10656|10657|10658|10659|10660|10661|10662|10663|10664|10665|10666|10667|10668|10669|10670|10671|10672|" +
						"10673|10674|10675|10676|10677|10678|10679|10680|10681|10682|10683|10684|10685|10686|10687|10688|10689|" +
						"10690|10691|10692|10693|10694|10695|10696|10697|10698|10699|10700|10701|10702|10703|10704|10705|10706|" +
						"10707|10708|10709|10710|10711|10712|10713|10714|10715|10716|10717|10718|10719|10720|10721|10722|10723|" +
						"10724|10725|10726|10727|10728|10729|10730|10731|10732|10733|10734|10735|10736|10737|10738|10739|10740|" +
						"10741|10742|10743|10744|10745|10746|10747|10748|10749|10750|10751|10752|10753|10754|10755|10756|10757|" +
						"10758|10759|10760|10761|10762|10763|10764|10765|10766|10767|10768|10769|10770|10771|10772|10773|10774|" +
						"10775|10776|10777|10778|10779|10780|10781|10782|10783|10784|10785|10786|10787|10788|10789|10790|10791|" +
						"10792|10793|10794|10795|10796|10797|10798|10799|10800|10801|10802|10803|10804|10805|10806|10807|10808|" +
						"10809|10810|10811|10812|10813|10814|10815|10816|10817|10818|10819|10820|10821|10822|10823|10824|10825|" +
						"10826|10827|10828|10829|10830|10831|10832|10833|10834|10835|10836|10837|10838|10839|10840|10841|10842|" +
						"10843|10844|10845|10846|10847|10848|10849|10850|10851|10852|10853|10854|10855|10856|10857|10858|10859|" +
						"10860|10861|10862|10863|10864|10865|10866|10867|10868|10869|10870|10871|10872|10873|10874|10875|10876|" +
						"10877|10878|10879|10880|10881|10882|10883|10884|10885|10886|10887|10888|10889|10890|10891|10892|10893|" +
						"10894|10895|10896|10897|10898|10899|10900|10901|10902|10903|10904|10905|10906|10907|10908|10909|10910|" +
						"10911|10912|10913|10914|10915|10916|10917|10918|10919|10920|10921|10922|10923|10924|10925|10926|10927|" +
						"10928|10929|10930|10931|10932|10933|10934|10935|10936|10937|10938|10939|10940|10941|10942|10943|10944|" +
						"10945|10946|10947|10948|10949|10950|10951|10952|10953|10954|10955|10956|10957|10958|10959|10960|10961|" +
						"10962|10963|10964|10965|10966|10967|10968|10969|10970|10971|10972|10973|10974|10975|10976|10977|10978|" +
						"10979|10980|10981|10982|10983|10984|10985|10986|10987|10988|10989|10990|10991|10992|10993|10994|10995|" +
						"10996|10997|10998|10999|11000|11001|11002|11003|11004|11005|11006|11007|11008|11009|11010|11011|11012|" +
						"11013|11014|11015|11016|11017|11018|11019|11020|11021|11022|11023|11024|11025|11026|11027|11028|11029|" +
						"11030|11031|11032|11033|11034|11035|11036|11037|11038|11039|11040|11041|11042|11043|11044|11045|11046|" +
						"11047|11048|11049|11050|11051|11052|11053|11054|11055|11056|11057|11058|11059|11060|11061|11062|11063|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void sameZip() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int copies = 100;
		final String sample = "02421";

		int locked = -1;

		for (int i = 0; i < copies; i++) {
			if (analysis.train(sample) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), copies);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), copies);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), copies);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertTrue(sample.matches(result.getRegExp()));
	}

	@Test
	public void sameZipWithHeader() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("ZipCode");
		final int copies = 100;
		final String sample = "02421";

		int locked = -1;

		for (int i = 0; i < copies; i++) {
			if (analysis.train(sample) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeUSZip5.SEMANTIC_TYPE);
		Assert.assertEquals(result.getSampleCount(), copies);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), copies);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), copies);
		Assert.assertEquals(result.getRegExp(), LogicalTypeUSZip5.REGEXP);
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertTrue(sample.matches(result.getRegExp()));
	}

	@Test
	public void basicStateHeader() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("BillingState");

		final String[] inputs = new String[] {
				"NY", "CA", "CA", "", "", "CA", "UK", "TX", "NC", "", "", "", "", "", "MA",
				"", "KS", "IL", "OR", "AZ", "NY", "CA", "CA", "MA", "MI", "ME", "", "", "", "",
				"", "KS", "IL", "OR", "AZ", "NY", "CA", "CA", "MA", "MI", "ME", "", "", "", ""
		};

		for (int i = 0; i < inputs.length; i++)
			analysis.train(inputs[i]);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeUSState.SEMANTIC_TYPE);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getBlankCount() - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), LogicalTypeUSState.REGEXP);
		final Map<String, Long> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.get("UK"), Long.valueOf(1));
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - result.getBlankCount()));
	}

	@Test
	public void basicStateSpaces() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("State", DateResolutionMode.DayFirst);
		final String input = " AL| AK| AZ| KY| KS| LA| ME| MD| MI| MA| MN| MS|MO |NE| MT| SD|TN | TX| UT| VT| WI|" +
						" VA| WA| WV| HI| ID| IL| IN| IA| KS| ky| LA| ME| MD| MA| MI| MN| MS| MO| MT| NE| NV|" +
						" NH| NJ| NM| NY| NC| ND| OH| OK| OR| PA| RI| SC| SD| TN| TX| UT| VT| VA| WA|  WV | WI|" +
						" WY| AL| AK| AZ| AR| CA| CO| CT| DC| de| FL| GA| HI| ID| IL| IN| IA| KS| KY| LA|SA|" +
						" MD| MA| MI| MN| MS| MO| MT| NE| NV| NH| NJ| NM| NY| NC| ND| OH| OK| OR| RI| SC| SD|" +
						" TX| UT| VT| WV| WI| WY| NV| NH| NJ| or| PA| RI| SC| AR| CA| CO| CT| ID| HI| IL| IN|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeUSState.SEMANTIC_TYPE);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), LogicalTypeUSState.REGEXP);
		final Map<String, Long> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.get("SA"), Long.valueOf(1));
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());

		LogicalType logical = analysis.getPlugins().getRegistered(LogicalTypeUSState.SEMANTIC_TYPE);
		for (int i = 0; i < inputs.length; i++) {
			String trimmed = inputs[i].trim();
			Assert.assertTrue(trimmed.matches(result.getRegExp()), inputs[i]);
			boolean expected = !outliers.containsKey(trimmed);
			Assert.assertEquals(logical.isValid(inputs[i]), expected);
		}
	}

	@Test
	public void basicEmailList() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "Bachmann@lavastorm.com,Biedermann@lavastorm.com|buchheim@lavastorm.com|" +
				"coleman@lavastorm.com,Drici@lavastorm.com|Garvey@lavastorm.com|jackson@lavastorm.com|" +
				"Jones@lavastorm.com|Marinelli@lavastorm.com,Nason@lavastorm.com,Parker@lavastorm.com|" +
				"Pigneri@lavastorm.com|Rasmussen@lavastorm.com|Regan@lavastorm.com|Segall@Lavastorm.com|" +
				"Smith@lavastorm.com|Song@lavastorm.com|Tolleson@lavastorm.com|wynn@lavastorm.com|" +
				"Ahmed@lavastorm.com|Benoit@lavastorm.com|Keane@lavastorm.com|Kilker@lavastorm.com|" +
				"Waters@lavastorm.com|Meagher@lavastorm.com|Mok@lavastorm.com|Mullin@lavastorm.com|" +
				"Nason@lavastorm.com|reilly@lavastorm.com|Scoble@lavastorm.com|Comerford@lavastorm.com|" +
				"Gallagher@lavastorm.com|Hughes@lavastorm.com|Kelly@lavastorm.com|" +
				"Tuddenham@lavastorm.com,Williams@lavastorm.com,Wilson@lavastorm.com";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeEmail.SEMANTIC_TYPE);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), LogicalTypeEmail.REGEXP);
		Assert.assertEquals(result.getConfidence(), 1.0);

		// Only simple emails match the regexp, so the count will not the 4 that include email lists :-(
		int matches = 0;
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i].matches(result.getRegExp()))
				matches++;
		}
		Assert.assertEquals(result.getMatchCount() - 4, matches);
	}

	@Test
	public void basicEmailListSemicolon() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "Bachmann@lavastorm.com;Biedermann@lavastorm.com|buchheim@lavastorm.com|" +
				"coleman@lavastorm.com;Drici@lavastorm.com|Garvey@lavastorm.com|jackson@lavastorm.com|" +
				"Jones@lavastorm.com|Marinelli@lavastorm.com;Nason@lavastorm.com;Parker@lavastorm.com|" +
				"Pigneri@lavastorm.com|Rasmussen@lavastorm.com|Regan@lavastorm.com|Segall@Lavastorm.com|" +
				"Smith@lavastorm.com|Song@lavastorm.com|Tolleson@lavastorm.com|wynn@lavastorm.com|" +
				"Ahmed@lavastorm.com|Benoit@lavastorm.com|Keane@lavastorm.com|Kilker@lavastorm.com|" +
				"Waters@lavastorm.com|Meagher@lavastorm.com|Mok@lavastorm.com|Mullin@lavastorm.com|" +
				"Nason@lavastorm.com|reilly@lavastorm.com|Scoble@lavastorm.com|Comerford@lavastorm.com|" +
				"Gallagher@lavastorm.com|Hughes@lavastorm.com|Kelly@lavastorm.com|" +
				"Tuddenham@lavastorm.com;Williams@lavastorm.com;Wilson@lavastorm.com|bo gus|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeEmail.SEMANTIC_TYPE);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length - 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), LogicalTypeEmail.REGEXP);
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
	}

	@Test
	public void basicStates() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		final String inputs[] = TestUtils.validUSStates.split("\\|");
		int locked = -1;

		analysis.train("XX");
		analysis.train("XX");
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i + 2;
		}
		analysis.train("XX");
		analysis.train("XX");
		analysis.train("XX");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeUSState.SEMANTIC_TYPE);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 5);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), LogicalTypeUSState.REGEXP);
		Assert.assertEquals(result.getConfidence(), 1 - (double)5/result.getSampleCount());

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicStatesWithDash() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		final String input = "AL|AK|AZ|KY|KS|LA|ME|MD|MI|MA|MN|MS|MO|NE|MT|SD|TN|TX|UT|VT|WI|" +
				"VA|WA|WV|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|NE|NV|-|" +
				"NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|-|" +
				"WY|AL|AK|AZ|AR|CA|CO|CT|DC|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|-|" +
				"MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|RI|SC|SD|-|" +
				"TX|UT|VT|WV|WI|WY|NV|NH|NJ|OR|PA|RI|SC|AR|CA|CO|CT|ID|HI|IL|IN|-|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeUSState.SEMANTIC_TYPE);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length - 5);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), LogicalTypeUSState.REGEXP);
		Assert.assertEquals(result.getConfidence(), 1 - (double)5/result.getSampleCount());

		for (int i = 0; i < inputs.length; i++) {
			if (!"-".equals(inputs[i]))
				Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicStates100Percent() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		analysis.setPluginThreshold(100);

		final String input = "AL|AK|AZ|KY|KS|LA|ME|MD|MI|MA|MN|MS|MO|NE|MT|SD|TN|TX|UT|VT|WI|" +
				"VA|WA|WV|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|NE|NV|XX|" +
				"NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|XX|" +
				"WY|AL|AK|AZ|AR|CA|CO|CT|DC|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|XX|" +
				"MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|RI|SC|SD|XX|" +
				"TX|UT|VT|WV|WI|WY|NV|NH|NJ|OR|PA|RI|SC|AR|CA|CO|CT|ID|HI|IL|IN|XX|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicStatesLower() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "al|ak|az|ky|ks|la|me|md|mi|ma|mn|ms|mo|ne|mt|sd|tn|tx|ut|vt|wi|" +
				"va|wa|wv|hi|id|il|in|ia|ks|ky|la|me|md|ma|mi|mn|ms|mo|mt|ne|nv|" +
				"nh|nj|nm|ny|nc|nd|oh|ok|or|pa|ri|sc|sd|tn|tx|ut|vt|va|wa|wv|wi|" +
				"wy|al|ak|az|ar|ca|co|ct|dc|de|fl|ga|hi|id|il|in|ia|ks|ky|la|me|" +
				"md|ma|mi|mn|ms|mo|mt|ne|nv|nh|nj|nm|ny|nc|nd|oh|ok|or|ri|sc|sd|" +
				"tx|ut|vt|wv|wi|wy|nv|nh|nj|or|pa|ri|sc|ar|ca|co|ct|id|hi|il|in|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeUSState.SEMANTIC_TYPE);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), LogicalTypeUSState.REGEXP);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicCA() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = TestUtils.validCAProvinces.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeCAProvince.SEMANTIC_TYPE);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), LogicalTypeCAProvince.REGEXP);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void notZipButNumeric() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int start = 10000;
		final int end = 99999;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i;
		}
		analysis.train("No Zip provided");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, start + TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), end + 1 - start);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), LogicalTypeUSZip5.REGEXP);
		Assert.assertEquals(result.getMatchCount(), end - start);
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
	}

	@Test
	public void notZips() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int start = 10000;
		final int end = 99999;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train(i < 80000 ? String.valueOf(i) : "A" + String.valueOf(i)) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, start + TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), end - start);
		Assert.assertEquals(result.getMatchCount(), end - start);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "(\\p{IsAlphabetic})?\\d{5}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMinValue(), "10000");
		Assert.assertEquals(result.getMaxValue(), "A99998");
	}

	// Set of valid months + 4 x "UNK"
	private final static String MONTH_TEST_GERMAN =
			"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|UNK|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|UNK|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|UNK|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|UNK|Nov|Dez|";

	@Test
	public void basicMonthAbbrGerman() throws IOException {

		if (!TestUtils.isValidLocale("de"))
			return;

		Locale german = Locale.forLanguageTag("de");

		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setLocale(german);

		final int badCount = 4;
		final String inputs[] = MONTH_TEST_GERMAN.split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{3}");
		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "MONTH.ABBR_de");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 1);
		final Map<String, Long> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.size(), 1);
		Assert.assertEquals(outliers.get("UNK"), Long.valueOf(4));
		Assert.assertEquals(result.getMatchCount(), inputs.length - badCount);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertTrue((double)analysis.getPluginThreshold()/100 < result.getConfidence());
		Assert.assertEquals(result.getConfidence(), 1 - (double)badCount/result.getSampleCount());

		// Even the UNK match the RE
		for (int i = 0; i < inputs.length; i++)
			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
	}

	@Test
	public void basicMonthAbbrBackout() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = TestUtils.months.split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		final int unknownCount = 10;
		for (int i = 0; i < unknownCount; i++)
			analysis.train("UNK");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "(?i)(APR|AUG|DEC|FEB|JAN|JUL|JUN|MAR|MAY|NOV|OCT|SEP|UNK)");
		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), result.getMatchCount());
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertTrue(inputs[0].matches(result.getRegExp()));

		int matches = 0;
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i].matches(result.getRegExp()))
					matches++;
		}
		Assert.assertEquals(result.getMatchCount() - unknownCount, matches);
	}

	@Test
	public void basicMonthAbbrExcessiveBad() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = TestUtils.months.split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		final int unknownCount = 10;
		for (int i = 0; i < unknownCount; i++)
			analysis.train("Bad");
		analysis.train("NA");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "(?i)(APR|AUG|BAD|DEC|FEB|JAN|JUL|JUN|MAR|MAY|NA|NOV|OCT|SEP|UNK)");
		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length + unknownCount + 1);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length + unknownCount + 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertTrue(inputs[0].matches(result.getRegExp()));

		for (int i = 0; i < inputs.length; i++)
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
	}

	@Test
	public void basicMonthAbbr() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int badCount = 4;
		final String inputs[] = TestUtils.months.split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{3}");
		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "MONTH.ABBR_en-US");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 1);
		final Map<String, Long> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.size(), 1);
		Assert.assertEquals(outliers.get("UNK"), Long.valueOf(4));
		Assert.assertEquals(result.getMatchCount(), inputs.length - badCount);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertTrue((double)analysis.getPluginThreshold()/100 < result.getConfidence());
		Assert.assertEquals(result.getConfidence(), 1 - (double)badCount/result.getSampleCount());

		// Even the UNK match the RE
		for (int i = 0; i < inputs.length; i++)
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));

/*
		analysis.train("Another bad element");
		result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\p{Alpha}{3}");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertTrue((double)analysis.getPluginThreshold()/100 < result.getConfidence());
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length + 1);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Map<String, Integer> updatedOutliers = result.getOutlierDetails();
		Assert.assertEquals(updatedOutliers.size(), 2);
		Assert.assertEquals(updatedOutliers.get("UNK"), Integer.valueOf(4));
		Assert.assertEquals(updatedOutliers.get("Another bad element"), Integer.valueOf(1));
		Assert.assertEquals(result.getMatchCount(), inputs.length - badCount);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1 - (double)(badCount + 1)/result.getSampleCount());
		*/
	}

	@Test
	public void basicMonthAbbrFrench() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setLocale(Locale.FRENCH);
		final int badCount = 4;
		final String inputs[] = TestUtils.monthsFrench.split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "[\\p{IsAlphabetic}\\.]{3,5}");
		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "MONTH.ABBR_fr");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 1);
		final Map<String, Long> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.size(), 1);
		Assert.assertEquals(outliers.get("UNK"), Long.valueOf(4));
		Assert.assertEquals(result.getMatchCount(), inputs.length - badCount);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertTrue((double)analysis.getPluginThreshold()/100 < result.getConfidence());
		Assert.assertEquals(result.getConfidence(), 1 - (double)badCount/result.getSampleCount());

		// Even the UNK match the RE
		for (int i = 0; i < inputs.length; i++)
			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
	}

	@Test
	public void basicStateLowCard() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("State", DateResolutionMode.DayFirst);
		final String input = "MA|MI|ME|MO|MS|";
		final String inputs[] = input.split("\\|");
		final int iters = 20;

		int locked = -1;

		for (int j = 0; j < iters; j++) {
			for (int i = 0; i < inputs.length; i++) {
				if (analysis.train(inputs[i]) && locked == -1)
					locked = i;
			}
		}
		final int UNKNOWN = 4;
		for (int k = 0; k < UNKNOWN; k++)
			analysis.train("NA");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\p{Alpha}{2}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeUSState.SEMANTIC_TYPE);
		Assert.assertEquals(result.getSampleCount(), inputs.length * iters + UNKNOWN);
		Assert.assertEquals(result.getCardinality(), 5);
		Assert.assertEquals(result.getOutlierCount(), 1);
		final Map<String, Long> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.size(), 1);
		Assert.assertEquals(outliers.get("NA"), Long.valueOf(UNKNOWN));
		Assert.assertEquals(result.getMatchCount(), inputs.length * iters);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1 - (double)UNKNOWN/result.getSampleCount());
	}

	@Test
	public void basicISO4127() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("CurrencyCode");
		final String input = "JMD|JOD|JPY|KES|KGS|KHR|KMF|KPW|KZT|LRD|MKD|MRU|" +
				"AFN|AOA|BBD|BIF|BSD|BZD|CHE|CHF|CHW|CLF|CLP|CNY|" +
				"MYR|NIO|PEN|PLN|RWF|SDG|SHP|SLL|SOS|SRD|SSP|STN|" +
				"SVC|SYP|SZL|THB|TOP|TZS|UYU|VND|XBA|XCD|XPD|XPF|" +
				"COP|COU|CRC|CUC|DJF|EGP|GBP|GMD|HRK|ILS|IRR|ISK|" +
				"XPT|XSU|XTS|XUA|XXX|YER|ZAR|ZMW|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\p{Alpha}{3}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeISO4217.SEMANTIC_TYPE);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getCardinality(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getMinLength(), 3);
		Assert.assertEquals(result.getMaxLength(), 3);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicISO3166_3() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("3166 Alpha-3");
		final String inputs[] = TestUtils.valid3166_3.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), LogicalTypeISO3166_3.REGEXP);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeISO3166_3.SEMANTIC_TYPE);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getCardinality(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getMinLength(), 3);
		Assert.assertEquals(result.getMaxLength(), 3);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void possibleSSN() throws IOException {
		final Random random = new Random(314159265);
		String[] samples = new String[1000];

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append('-');
			b.append(String.format("%02d", random.nextInt(100)));
			b.append('-');
			b.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}

		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setLengthQualifier(false);
		for (String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), "\\d{3}-\\d{2}-\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void testRegExpLogicalType_SSN() throws IOException {
		final Random random = new Random(314159265);
		String[] samples = new String[1000];

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append('-');
			b.append(String.format("%02d", random.nextInt(100)));
			b.append('-');
			b.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}

		final TextAnalyzer analysis = new TextAnalyzer("Primary SSN");
		List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(new PluginDefinition("SSN", "\\d{3}-\\d{2}-\\d{4}", null,
				new String[] { "en-US" }, new String[] { "SSN", "social" }, true, 98, PatternInfo.Type.STRING));

		try {
			analysis.getPlugins().registerPluginList(plugins, "SSN", null);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		for (String sample : samples) {
			analysis.train(sample);
		}
		analysis.train("032--45-0981");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), "\\d{3}-\\d{2}-\\d{4}");
		Assert.assertEquals(result.getTypeQualifier(), "SSN");
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getSampleCount(), samples.length + 1);
		final Map<String, Long> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.size(), 1);
		Assert.assertEquals(outliers.get("032--45-0981"), Long.valueOf(1));

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void testFinitePlugin() throws IOException {
		String[] planets = new String[] { "MERCURY", "VENUS", "EARTH", "MARS", "JUPITER", "SATURN", "URANUS", "NEPTUNE", "PLUTO", "" };
		Path path = Files.createTempFile("planets", ".txt");
		Files.write(path, String.join("\n", planets).getBytes(), StandardOpenOption.APPEND);
		final int SAMPLES = 100;
		final Random random = new Random(314159265);

		PluginDefinition pluginDefinition = new PluginDefinition("PLANET", "\\p{Alpha}*", path.toString(),
				new String[] { "en" }, new String[] {}, false, 98, PatternInfo.Type.STRING);

		final TextAnalyzer analysis = new TextAnalyzer("Planets");
		List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(pluginDefinition);

		try {
			analysis.getPlugins().registerPluginList(plugins, "Planets", null);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		for (int i = 0; i < SAMPLES; i++) {
			analysis.train(planets[random.nextInt(planets.length)]);
		}
		analysis.train("032--45-0981");

		final TextAnalysisResult result = analysis.getResult();

		Files.delete(path);

		Assert.assertEquals(result.getBlankCount(), 11);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), "\\p{Alpha}*");
		Assert.assertEquals(result.getTypeQualifier(), "PLANET");
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - 11));
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getSampleCount(), SAMPLES + 1);
		final Map<String, Long> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.size(), 1);
		Assert.assertEquals(outliers.get("032--45-0981"), Long.valueOf(1));
	}

	@Test
	public void testFinitePluginBackout() throws IOException {
		String[] planets = new String[] { "MERCURY", "VENUS", "EARTH", "MARS", "JUPITER", "SATURN", "URANUS", "NEPTUNE", "PLUTO", "" };
		Path path = Files.createTempFile("planets", ".txt");
		Files.write(path, String.join("\n", planets).getBytes(), StandardOpenOption.APPEND);
		final int SAMPLES = 100;
		final Random random = new Random(314159265);

		PluginDefinition pluginDefinition = new PluginDefinition("PLANET", "\\p{Alpha}*", path.toString(),
				new String[] { "en" }, new String[] {}, false, 98, PatternInfo.Type.STRING);

		final TextAnalyzer analysis = new TextAnalyzer("Planets");
		List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(pluginDefinition);

		try {
			analysis.getPlugins().registerPluginList(plugins, "Planets", null);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		analysis.train("io");
		for (int i = 0; i < SAMPLES; i++) {
			analysis.train(planets[random.nextInt(planets.length)]);
		}
		analysis.train("europa");

		final TextAnalysisResult result = analysis.getResult();

		Files.delete(path);

		Assert.assertEquals(result.getBlankCount(), 11);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), "(?i)(EARTH|EUROPA|IO|JUPITER|MARS|MERCURY|NEPTUNE|PLUTO|SATURN|URANUS|VENUS)");
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getSampleCount(), SAMPLES + 2);
	}

	@Test
	public void testNames() throws IOException {
		CsvParserSettings settings = new CsvParserSettings();
		settings.setHeaderExtractionEnabled(true);
		String[] header = null;
		String[] row;
		TextAnalyzer[] analysis = null;

		try (BufferedReader in = new BufferedReader(new InputStreamReader(TestPlugins.class.getResourceAsStream("/Names.txt")))) {

			CsvParser parser = new CsvParser(settings);
			parser.beginParsing(in);

			header = parser.getRecordMetadata().headers();
			analysis = new TextAnalyzer[header.length];
			for (int i = 0; i < header.length; i++) {
				analysis[i] = new TextAnalyzer(header[i]);
			}
			while ((row = parser.parseNext()) != null) {
				for (int i = 0; i < row.length; i++) {
					analysis[i].train(row[i]);
				}
			}
		}

		Assert.assertEquals(analysis[0].getResult().getTypeQualifier(), "NAME.FIRST");
		Assert.assertEquals(analysis[1].getResult().getTypeQualifier(), "NAME.LAST");

		LogicalType logicalFirst = analysis[0].getPlugins().getRegistered(LogicalTypeFirstName.SEMANTIC_TYPE);
		Assert.assertTrue(logicalFirst.isValid("Harry"));
	}

	@Test
	public void testLatitude() throws IOException {
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
		Assert.assertEquals(result.getRegExp(), "[+-]?\\d*\\.?\\d+");
		Assert.assertEquals(result.getTypeQualifier(), "COORDINATE.LATITUDE_DECIMAL_SIGNED");
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
	public void testRegExpLogicalType_CUSIP() throws IOException {
		final String CUSIP_REGEXP = "[\\p{IsAlphabetic}\\d]{9}";
		String[] samples = new String[] {
				"B38564108", "B38564908", "B38564958", "C10268AC1", "C35329AA6", "D18190898", "D18190908", "D18190958", "G0084W101", "G0084W901",
				"G0084W951", "G0129K104", "G0129K904", "G0129K954", "G0132V105", "G0176J109", "G0176J909", "G0176J959", "G01767105", "G01767905",
				"G01767955", "G0177J108", "G0177J908", "G0177J958", "G02602103", "G02602903", "G02602953", "G0335L102", "G0335L902", "G0335L952"

		};

		final TextAnalyzer analysis = new TextAnalyzer("CUSIP");
		List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(new PluginDefinition("CUSIP", "[\\p{IsAlphabetic}\\d]{9}", null,
				new String[] { }, new String[] { "CUSIP" }, true, 98, PatternInfo.Type.STRING));

		try {
			analysis.getPlugins().registerPluginList(plugins, "CUSIP", null);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		for (String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getRegExp(), CUSIP_REGEXP);
		Assert.assertEquals(result.getTypeQualifier(), "CUSIP");
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicISO3166_2() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("3166 Alpha-2");
		final String inputs[] = TestUtils.valid3166_2.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), LogicalTypeISO3166_2.REGEXP);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeISO3166_2.SEMANTIC_TYPE);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getCardinality(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getMinLength(), 2);
		Assert.assertEquals(result.getMaxLength(), 2);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicCountry() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "Venezuela|USA|Finland|USA|USA|Germany|France|Italy|Mexico|Germany|" +
				"Sweden|Germany|Sweden|Spain|Spain|Venezuela|Germany|Germany|Germany|Brazil|" +
				"Italy|UK|Brazil|Brazil|Brazil|Mexico|USA|France|Venezuela|France|" +
				"Ireland|Brazil|Italy|Germany|Belgium|Spain|Mexico|USA|Spain|USA|" +
				"Mexico|Ireland|USA|France|Germany|Germany|USA|UK|USA|USA|" +
				"UK|Mexico|Finland|UK|Mexico|Germany|USA|Germany|Spain|Sweden|" +
				"Portugal|USA|Venezuela|France|Canada|Finland|France|Ireland|Portugal|Germany|" +
				"USA|Canada|France|Denmark|Germany|Germany|USA|Germany|USA|Brazil|" +
				"Germany|USA|France|Austria|Portugal|Austria|Mexico|UK|Germany|Venezuela|" +
				"France|UK|France|Germany|France|Germany|UK|Mexico|Spain|Denmark|" +
				"Austria|USA|Switzerland|France|Brazil|Ireland|Poland|USA|Canada|UK|" +
				"Sweden|Brazil|Ireland|Venezuela|Austria|UK|Sweden|USA|Brazil|Norway|" +
				"UK|Canada|Austria|Germany|Austria|USA|USA|Venezuela|Germany|Portugal|" +
				"USA|Denmark|UK|USA|Austria|Austria|Italy|Venezuela|Brazil|Germany|" +
				"France|Argentina|Canada|Canada|Finland|France|Brazil|USA|Finland|Denmark|" +
				"Germany|Switzerland|Brazil|Brazil|Italy|Brazil|Canada|France|Spain|Austria|" +
				"Italy|Ireland|Austria|Canada|USA|Portugal|Sweden|UK|France|Finland|" +
				"Germany|Canada|USA|USA|Austria|Italy|Sweden|Sweden|Germany|Brazil|" +
				"Argentina|France|France|Germany|USA|UK|France|Finland|Germany|Germany|" +
				"Belgium|France|Sweden|Venezuela|UK|Belgium|Portugal|Denmark|Brazil|Italy|" +
				"Germany|USA|France|UK|UK|UK|Mexico|  Belgium  |Venezuela|Portugal|" +
				"France|USA|France|Brazil|USA|USA|UK|Venezuela|Venezuela|Brazil|" +
				"Germany|Austria|Venezuela|Portugal|Canada|France|Brazil|Canada|Brazil|Germany|" +
				"Venezuela|Venezuela|France|Germany|Mexico|Ireland|USA|Canada|Germany|Mexico|" +
				"Germany|Germany|USA|France|Brazil|Germany|Austria|Germany|Ireland|UK|Gondwanaland|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeCountryEN.SEMANTIC_TYPE);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 1);
		final Map<String, Long> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.size(), 1);
		long outlierCount = outliers.get("GONDWANALAND");
		Assert.assertEquals(result.getMatchCount(), inputs.length - outlierCount);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), ".+");
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
	}

	@Test
	public void thinAddress() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Example_Address");
		final String[] inputs = new String[] {
				"123 Test St",
				"124 Test St",
				"125 Test St",
				"126 Test St",
				"127 Test St",
				"128 Test St",
				"129 Test St",
				"130 Test St",
				"131 Test St"
		};

		for (int i = 0; i < inputs.length; i++)
			analysis.train(inputs[i]);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeAddressEN.SEMANTIC_TYPE);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), ".+");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void basicCountryHeader() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("BillingCountry");
		final String[] inputs = new String[] {
				"", "", "", "", "", "", "", "", "", "USA", "France", "USA",
				"", "", "", "", "US", "", "", "", "", "", "", "", "", "", "", "", "", ""
		};

		for (int i = 0; i < inputs.length; i++)
			analysis.train(inputs[i]);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeCountryEN.SEMANTIC_TYPE);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 4);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), ".+");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	String validUSStreets2[] = new String[] {
			"6649 N Blue Gum St",
			"4 B Blue Ridge Blvd",
			"8 W Cerritos Ave #54",
			"639 Main St",
			"34 Center St",
			"3 Mcauley Dr",
			"7 Eads St",
			"7 W Jackson Blvd",
			"5 Boston Ave #88",
			"228 Runamuck Pl #2808",
			"2371 Jerrold Ave",
			"37275 St  Rt 17m M",
			"25 E 75th St #69",
			"98 Connecticut Ave Nw",
			"56 E Morehead St",
			"73 State Road 434 E",
			"69734 E Carrillo St",
			"322 New Horizon Blvd",
			"1 State Route 27",
			"394 Manchester Blvd",
			"6 S 33rd St",
			"6 Greenleaf Ave",
			"618 W Yakima Ave",
			"74 S Westgate St",
			"3273 State St",
			"1 Central Ave",
			"86 Nw 66th St #8673",
			"2 Cedar Ave #84",
			"90991 Thorburn Ave",
			"386 9th Ave N",
			"74874 Atlantic Ave",
			"366 South Dr",
			"45 E Liberty St",
			"4 Ralph Ct",
			"2742 Distribution Way",
			"426 Wolf St",
			"128 Bransten Rd",
			"17 Morena Blvd",
			"775 W 17th St",
			"6980 Dorsett Rd",
			"2881 Lewis Rd",
			"7219 Woodfield Rd",
			"1048 Main St",
			"678 3rd Ave",
			"20 S Babcock St",
			"2 Lighthouse Ave",
			"38938 Park Blvd",
			"5 Tomahawk Dr",
			"762 S Main St",
	};

	String validUSAddresses[] = new String[] {
			"9885 Princeton Court Shakopee, MN 55379",
			"11 San Pablo Rd.  Nottingham, MD 21236",
			"",
			"365 3rd St.  Woodhaven, NY 11421",
			"426 Brewery Street Horn Lake, MS 38637",
			"676 Thatcher St.  Hagerstown, MD 21740",
			"848 Hawthorne St.  Rockaway, NJ 07866",
			"788 West Coffee St.  Abingdon, MD 21009",
			"240 Arnold Avenue Yorktown Heights, NY 10598",
			"25 S. Hawthorne St.  Elizabeth City, NC 27909",
			"9314 Rose Street Holyoke, MA 01040",
			"32 West Bellevue St.  Holly Springs, NC 27540",
			"8168 Thomas Road El Dorado, AR 71730",
			"353 Homewood Ave.  Poughkeepsie, NY 12601",
			"14 North Cambridge Street Anchorage, AK 99504",
			"30 Leeton Ridge Drive Bristol, CT 06010",
			"8412 North Mulberry Dr.  Tiffin, OH 44883",
			"7691 Beacon Street Marysville, OH 43040",
			"187 Lake View Drive Redford, MI 48239",
			"318 Summerhouse Road Lenoir, NC 28645",
			"",
			"609 Taylor Ave.  Fort Myers, FL 33905",
			"47 Broad St.  Baldwin, NY 11510",
			"525 Valley View St.  Natick, MA 01760",
			"8 Greenview Ave.  Lithonia, GA 30038",
			"86 North Helen St.  Clermont, FL 34711",
			"8763 Virginia Street Hyattsville, MD 20782",
			"10 Front Avenue Brookline, MA 02446",
			"141 Blue Spring Street Ocoee, FL 34761",
			"99 W. Airport Ave.  Eau Claire, WI 54701",
			"32 NW. Rocky River Ave.  Raeford, NC 28376",
			"324 North Lancaster Dr.  Wyoming, MI 49509"
	};

	@Test
	public void basicUSStreet() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		for (String s : TestUtils.validUSStreets) {
			analysis.train(s);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), TestUtils.validUSStreets.length);
		Assert.assertEquals(result.getCardinality(), TestUtils.validUSStreets.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeAddressEN.SEMANTIC_TYPE);
		Assert.assertEquals(result.getRegExp(), ".+");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void basicUSStreet2() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		for (String s : validUSStreets2) {
			analysis.train(s);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), validUSStreets2.length);
		Assert.assertEquals(result.getCardinality(), validUSStreets2.length);
		Assert.assertEquals(result.getMatchCount(), validUSStreets2.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeAddressEN.SEMANTIC_TYPE);
		Assert.assertEquals(result.getRegExp(), ".+");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}
}
