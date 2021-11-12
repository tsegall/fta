/*
 * Copyright 2017-2021 Tim Segall
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

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.InternalErrorException;
import com.cobber.fta.plugins.LogicalTypeUSZip5;
import com.cobber.fta.plugins.LogicalTypeUSZipPlus4;

public class RandomTests {
	private static final SecureRandom random = new SecureRandom();

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void getReflectionSampleSize() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("getReflectionSampleSize");

		Assert.assertEquals(analysis.getReflectionSampleSize(), TextAnalyzer.REFLECTION_SAMPLES);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void getDefaultLogicalTypesDefault() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("getDefaultLogicalTypesDefault");

		Assert.assertTrue(analysis.getDefaultLogicalTypes());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setDefaultLogicalTypesTooLate() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setDefaultLogicalTypesTooLate");

		analysis.train("Hello, World");

		try {
			analysis.setDefaultLogicalTypes(false);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Cannot adjust Logical Type detection once training has started");
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setDefaultLogicalTypes() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setDefaultLogicalTypes");
		analysis.setDefaultLogicalTypes(false);
		Assert.assertFalse(analysis.getDefaultLogicalTypes());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void inadequateData() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("inadequateData");
		final String[] inputs = "47|89|90|91".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), FTAType.LONG);
		Assert.assertEquals(result.getMinValue(), "47");
		Assert.assertEquals(result.getMaxValue(), "91");

		for (final String input : inputs) {
			Assert.assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void noData() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("noData");
		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_NULL));
		Assert.assertEquals(result.getConfidence(), 0.0);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "NULL");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void rubbish() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("rubbish");
		final String[] inputs = "47|hello|hello,world|=====47=====|aaaa|0|12|b,b,b,b390|4083|ddd ddd|90|-------|+++++|42987|8901".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.freezeANY(1, 12, 1, 12, result.getLeadingWhiteSpace(), result.getTrailingWhiteSpace(), result.getMultiline()));
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getMinValue(), "+++++");
		Assert.assertEquals(result.getMaxValue(), "hello,world");
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 12);

		for (final String input : inputs) {
			Assert.assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void zip50() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("zip50");
		analysis.setPluginThreshold(90);
		int locked = -1;
		final int COUNT = 46;
		final int INVALID = 3;

		for (int i = 10000; i < 10000 + COUNT; i++) {
			if (analysis.train(String.valueOf(i)) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeUSZip5.SEMANTIC_TYPE);
		Assert.assertEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), COUNT);
		Assert.assertEquals(result.getMatchCount(), COUNT - INVALID);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getType(), FTAType.LONG);
		Assert.assertEquals(result.getMinValue(), "10000");
		Assert.assertEquals(result.getMaxValue(), "10045");
		Assert.assertEquals(result.getConfidence(), 0.9673913043478262);

		for (int i = 10000; i < 10000 + COUNT; i++) {
			Assert.assertTrue(String.valueOf(i).matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void mean100() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("mean100");
		int locked = -1;
		final int COUNT = 100;
		int sum = 0;

		for (int i = 0; i < COUNT; i++) {
			sum += i;
			if (analysis.train(String.valueOf(i)) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), COUNT);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}");
		Assert.assertEquals(result.getType(), FTAType.LONG);
		Assert.assertEquals(result.getMinValue(), "0");
		Assert.assertEquals(result.getMaxValue(), "99");
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMean(), Double.valueOf((double)sum/COUNT));
		Assert.assertEquals(result.getStandardDeviation(), Double.valueOf(28.86607004772212));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void limitedData() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("limitedData");
		final String[] inputs = "12|4|5|".split("\\|");
		final int pre = 3;
		final int post = 10;

		for (int i = 0; i < pre; i++)
			analysis.train("");
		for (final String input : inputs) {
			analysis.train(input);
		}
		for (int i = 0; i < post; i++)
			analysis.train("");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), pre + inputs.length + post);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), FTAType.LONG);

		for (final String input : inputs) {
			Assert.assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void debugging() throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("employeeNumber");
		analysis.setTrace("enabled=true");
		analysis.setDebug(2);
		final String pipedInput = "||||||||||||||||||||" +
				"F944255990|F944277490|F944277490|F944285690|F944285690|F944285690|F944285690|F944285690|F944296590|F944296590|" +
				"F944296590|F944296890|F944299990|F944299990|FN22844690|FN24121490|FN24122790|FN24623590|FN24628690|FN24628890|" +
				"FN27016490|FN27016890|FN27381590|FN27396790|FN29563390|FN29565590|FN29565790|FN29565990|FN29568490|FN29568890|" +
				"FN29584290|FN944102090|FN944104890|FN944106490|FN944108290|FN944113890|FN944118990|FN944124490|FN944124690|FN944124890|" +
				"¶ xyzzy ¶|" +     // MAGIC
				"FN944133090";
		final String inputs[] = pipedInput.split("\\|");

		try {
			for (final String input : inputs)
				analysis.train(input);
		}
		catch (InternalErrorException e) {
			// We expect this to happen ...
		}

		final TextAnalysisResult result = analysis.getResult();

		final int samplesProcessed = inputs.length - 1;

		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), samplesProcessed);
		Assert.assertEquals(result.getMatchCount(), samplesProcessed - result.getBlankCount() - 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getMinLength(), 9);
		Assert.assertEquals(result.getMaxLength(), 11);
		Assert.assertEquals(result.getBlankCount(), 20);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{10,11}");
		Assert.assertEquals(result.getConfidence(), 0.975609756097561);

		int matchCount = 0;
		for (int i = 0; i < samplesProcessed - 1; i++) {
			String input = inputs[i];
			if (!input.trim().isEmpty() && input.matches(result.getRegExp()))
				matchCount++;
		}
		Assert.assertEquals(matchCount, result.getMatchCount());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testTrim() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testTrim");
		final String pipedInput = " Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi          |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        ";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + result.getNullCount());
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 1);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_WHITESPACE + "((?i)(HELLO|HI|WORLD))" + KnownPatterns.PATTERN_WHITESPACE);
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertNull(result.getTypeQualifier());

		for (final String input : inputs) {
			Assert.assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void changeMind() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("changeMind");
		int locked = -1;

		for (int i = 0; i < 2 * AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i;
		}

		for (char ch = 'a'; ch <= 'z'; ch++) {
			analysis.train(String.valueOf(ch));
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), 2 * AnalysisConfig.DETECT_WINDOW_DEFAULT + 26);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 2 * AnalysisConfig.DETECT_WINDOW_DEFAULT + 26);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{1,2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void changeMindMinMax() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("changeMindMinMax");
		analysis.setThreshold(97);
		final String pipedInput =
				"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!!Volume!Volume!!!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!!Volume!Volume!!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!!!!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!!!!Volume!Audio disc ; Volume!Volume!!!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Audio disc ; Volume!!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!!Volume!Volume!!!!!!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!!!Volume!Volume!Volume!Volume!Volume!!!Volume!Volume!Volume!" +
						"Volume!Volume!!!!!!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"!Volume!Volume!!!Volume!Volume!Volume!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!!Volume!Volume!Volume!!Volume!Volume!!Volume!Volume!!Volume!Volume!Volume!" +
						"Volume!!Volume!Volume!!Volume!!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Online resource (ePub ebook)!Volume!Online resource (ePub ebook)!Volume!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"!Volume!Volume!Volume!Volume!Volume!Volume!Volume!!Volume!Volume!Volume!Volume!Volume!!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Audio disc ; Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Online resource (ePub ebook)!Volume!Volume!Volume!" +
						"Volume!Volume!!!!Volume!Volume!Volume!Volume!Volume!Volume!Computer disc!Volume!Volume!Volume!Volume!Volume!" +
						"!Volume!Online resource!Volume!!Volume!!Volume!!Volume!!Volume!Online resource (PDF ebook ; ePub ebook)!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Online resource (ePub ebook)!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Audio disc ; Volume!" +
						"Volume!Volume!Volume!Volume!Volume!!Volume!Volume!!Volume!Volume!Volume!!Volume!Volume!!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!";
		final String inputs[] = pipedInput.split("\\!");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount() + result.getBlankCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 0);
		Assert.assertEquals(result.getRegExp(), "(?i)(AUDIO DISC ; VOLUME|COMPUTER DISC|ONLINE RESOURCE|\\QONLINE RESOURCE (EPUB EBOOK)\\E|\\QONLINE RESOURCE (PDF EBOOK ; EPUB EBOOK)\\E|VOLUME)");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMinValue(), "Audio disc ; Volume");
		Assert.assertEquals(result.getMaxValue(), "Volume");

		final String regExp = result.getRegExp();
		for (final String input : inputs) {
			if (input.length() == 0)
				continue;
			Assert.assertTrue(input.matches(regExp), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testQualifierAlpha() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Alpha");
		final int STRING_LENGTH = 5;
		Assert.assertTrue(analysis.getLengthQualifier());
		analysis.setLengthQualifier(false);
		Assert.assertFalse(analysis.getLengthQualifier());
		final String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

		final int start = 10000;
		final int end = 99999;

		int locked = -1;

		for (int i = start; i < end; i++) {
			final StringBuilder sample = new StringBuilder(STRING_LENGTH);
			for (int j = 0; j < STRING_LENGTH; j++)
				sample.append(alphabet.charAt(random.nextInt(52)));
			if (analysis.train(sample.toString()) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{5}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testQualifierAlphaNumeric() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("AlphaNumeric");
		analysis.setLengthQualifier(false);

		final int start = 10000;
		final int end = 99999;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train('A' + String.valueOf(i)) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\p{IsAlphabetic}\\d{5}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void change2() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("change2");
		final String input = "AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
				"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|Sep|Jan|Oct|Oct|Oct|" +
				"Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
				"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|NA|Sep|Jan|Oct|Oct|Oct|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "(?i)(AB|APR|AUG|BC|DEC|FEB|JAN|JUL|JUN|MAR|MAY|MB|NA|NB|NL|NOV|NS|NT|NU|OCT|ON|PE|QC|SEP|SK|YT)");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void mixedZip() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("mixedZip");
		final String[] inputs = new String[] {
				"98115-2654", "98007", "98042-8501", "98311-3239", "98074-3322", "98039", "98466-2041", "98136-2633", "98166-3212", "98042-8213",
				"98121", "98038-8314", "98112-4739", "98059-7315", "20017-4261", "21204-2055", "21158-3604", "21784", "21776-9719", "20854",
				"22201-2618", "20017-1513", "20016-8001", "20008-5941", "20904-1209", "20901-1040", "20901-3105", "20817-6330", "20164", "20008-2522",
				"20109-3364", "20112-2759", "20708-1401", "20169-2703", "20155-1824", "20854-5497", "20169-1224", "20194-4323", "20190-4969", "20783-3052",
				"20716-1843", "20772-3222", "20882-1614", "20007-4104", "20112-3041", "20902", "20874-2915", "22305", "20165-2810", "20110-5357",
				"21078", "20770-3514", "20032-4801", "20220-0001", "22304-2552", "20772-4505", "20747-5101", "20769-9031", "20715", "20785-4618",
				"20746-3425", "21030-2210", "21078-1828", "20708-9758", "21228", "20754-9574", "21157-7720", "21048", "22192", "22205-3163",
				"21122-5702", "21220-1613", "21228", "21102-2059", "21221-3530", "21210-1556", "21040-1054", "21202-3504", "21043-6929", "21224-2141",
				"21042", "21093-7547", "21001", "21087", "20772-4137", "21111-1120", "21228-5317", "20678-3443", "20639", "20772-8378",
				"20772", "20735-4560", "21220", "21060-7241", "21220", "21009", "21108", "21201-5097", "22202", "22202", "20036", "20024", "20566",
				"21771", "21117", "20005", "21770", "20613", "20009","21229", "21791", "", "22134", "", "", "21225", "20850-3164", "21230", "21236",
				"20190", "20910", "21225", "21409-6107", "20782-3952", "22201-5798", "21205", "22202", "21250-1000", "20015-2770",
				"21209-2101", "21227-4817", "21009", "21204-4310", "22205-3163", "20015-1009", "21029", "21228", "20855-1555", "21227-1056",
				"21157-6530", "21042-3629", "21044-1211", "21794-9604", "20007-4373", "21009", "20903-2019", "20906-5271", "22206", "20769-9161",
				"20019-6732", "20737-1046", "20872-1867", "21074", "20854-6209", "20818-1328", "20906", "20876", "20740-3170", "20112-4735",
				"21201", "22202", "20782-2335", "20166-7547", "20019-1501", "20743", "22046-4235", "21218", "20770-1410", "20817-5700",
				"20905-5003", "20833-1711", "20008-4701", "22201-4502", "20842-9062", "20639-3035", "20166-2117", "20169-1932", "20782-3952", "22203-2054",
				"20854-2983", "21222", "20772-4237", "20878", "20879", "20874-1517", "20879", "20705", "20165-2496", "20772-5035",
				"21001", "20878", "21161", "20170-3241", "22201-5798", "20015-2770", "20882-1266", "20854-3916", "20715-3102", "20747"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getTypeQualifier(), LogicalTypeUSZipPlus4.SEMANTIC_TYPE);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getBlankCount());
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), LogicalTypeUSZipPlus4.REGEXP_VARIABLE);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			if (!input.isEmpty())
				Assert.assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void trailingAM() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("trailingAM");
		final String inputs[] = new String[] {
				"02s500000023SQ3AAM", "02s5000000233ThAAI", "02s5000000238JRAAY", "02s500000023QCEAA2",
				"02s500000023QCFAA2", "02s500000023SKAAA2", "02s5000000233TgAAI", "02s500000023Sw9AAE",
				"02s500000023T0pAAE", "02s500000023U6FAAU", "02s500000023qQVAAY", "02s500000023qQWAAY",
				"02s500000023r2FAAQ", "02s500000023rFiAAI", "02s500000023x3qAAA", "02s50000002GgdtAAC",
				"02s50000002GgduAAC", "02s50000002GkKXAA0", "02s50000002GrukAAC", "02s50000002GrulAAC",
				"02s50000002GsLCAA0", "02s50000002HCnGAAW", "02s50000002HUaFAAW", "02s50000002HUaGAAW",
				"02s50000002HV82AAG", "02s50000002HjVvAAK", "02s50000002Hl4NAAS", "02s50000002HnXRAA0",
				"02s50000002Hq1sAAC", "02s50000002HrQPAA0", "02s50000002HrraAAC", "02s50000002HxoKAAS",
				"02s50000002I6lQAAS", "02s50000002I90MAAS", "02s50000002I93BAAS", "02s50000002I9CSAA0"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{18}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			Assert.assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void frenchName() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("frenchName");
		final String pipedInput = "Adrien|Alain|Albert|Alexandre|Alexis|André|Antoine|Arnaud|Arthur|Aurélien|" +
				"Baptiste|Benjamin|Benoît|Bernard|Bertrand|Bruno|Cédric|Charles|Christian|Christophe|" +
				"Claude|Clément|Cyril|Damien|Daniel|David|Denis|Didier|Dominique|Dylan|" +
				"Emmanuel|Éric|Étienne|Enzo|Fabien|Fabrice|Florent|Florian|Francis|Franck|" +
				"François|Frédéric|Gabriel|Gaétan|Georges|Gérard|Gilbert|Gilles|Grégory|Guillaume|" +
				"Guy|Henri|Hervé|Hugo|Jacques|Jean|";
		//Jean-Claude|Jean-François|Jean-Louis|Jean-Luc|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{3,10}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			Assert.assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void basicLengthValidationBlanks() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Spaces");
		final int iters = 30;
		final Set<String> samples = new HashSet<>();

		int locked = -1;

		final StringBuilder sb = new StringBuilder("  ");

		for (int i = 0; i < iters; i++) {
			final String s = sb.toString();
			samples.add(s);
			if (analysis.train(s) && locked == -1)
				locked = i;
			sb.append(' ');
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_BLANK));
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "BLANK");
		Assert.assertEquals(result.getSampleCount(), iters);
		Assert.assertEquals(result.getBlankCount(), iters);
		Assert.assertEquals(result.getCardinality(), 0);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), iters - result.getBlankCount());
		Assert.assertEquals(result.getMinLength(), 2);
		Assert.assertEquals(result.getMaxLength(), 2 + iters - 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			if (sample.trim().length() > 0)
				Assert.assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void basicLengthValidationString() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Spaces");
		final int iters = 30;
		final Set<String> samples = new HashSet<>();

		int locked = -1;

		final StringBuilder sb = new StringBuilder("  ");

		for (int i = 0; i < iters; i++) {
			final String s = sb.toString();
			samples.add(s);
			if (analysis.train(s) && locked == -1)
				locked = i;
			sb.append(' ');
		}
		analysis.train("          abc          ");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_WHITESPACE + KnownPatterns.PATTERN_ALPHA + "{3}" + KnownPatterns.PATTERN_WHITESPACE);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), iters + 1);
		Assert.assertEquals(result.getBlankCount(), iters);
		Assert.assertEquals(result.getCardinality(), 1);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 1);
		Assert.assertEquals(result.getMinLength(), 2);
		Assert.assertEquals(result.getMaxLength(), 2 + iters - 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			if (sample.trim().length() > 0)
				Assert.assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void variableSpacesFixedLength() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("variableSpacesFixedLength");
		final String pipedInput = "JMD     |JOD     |JPYP    |KESQ    |KGS     |KHR     |" +
				" AXN    | AOAZ   | B1D    | BIFD   | BSD    | BZD    | CZE    | CHF    |" +
				"  MzR   |  NIO   |  P2N   |  PLN   |  RWF   |  SDG   |  SHP   |  SLL   |" +
				"   SVQ  |   SYP  |   S33Z |   THB  |   TOP  |   TZS  |   UYE  |   VND  |" +
				"    CQP |    COU |    CRC |    CUC |    DJF |    EGP |    GLP |    GMD |" +
				"     APT|     CSU|     44S|    LFUA|XXXXXXXX|     PER|     NAR|     ZMW|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "[ 	]*[\\p{IsAlphabetic}\\d]{3,8}[ 	]*");
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getCardinality(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getMinLength(), 8);
		Assert.assertEquals(result.getMaxLength(), 8);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			Assert.assertTrue(input.matches(result.getRegExp()));
		}
	}

	private final String alpha3 = "aaa|bbb|ccc|ddd|eee|fff|ggg|hhh|iii|jjj|" +
			"aaa|iii|sss|sss|sss|vvv|jjj|jjj|jjj|bbb|iii|uuu|bbb|bbb|vvv|mmm|uuu|fff|vvv|fff|" +
			"iii|ééé|iii|ggg|bbb|sss|mmm|uuu|sss|uuu|aaa|iii|sss|sss|sss|vvv|jjj|jjj|jjj|bbb|" +
			"iii|uuu|bbb|bbb|vvv|mmm|uuu|fff|vvv|fff|iii|bbb|iii|ggg|bbb|sss|mmm|uuu|sss|uuu|" +
			"aaa|iii|sss|sss|sss|vvv|jjj|jjj|jjj|ççç|iii|uuu|bbb|bbb|vvv|mmm|uuu|fff|vvv|fff|" +
			"iii|bbb|iii|ggg|bbb|sss|mmm|uuu|sss|uuu|aaa|iii|sss|sss|sss|vvv|jjj|jjj|jjj|bbb|" +
			"iii|uuu|bbb|bbb|vvv|mmm|uuu|fff|vvv|fff|iii|bbb|iii|ggg|bbb|sss|mmm|uuu|sss|uuu|" +
			"kkk|lll|nnn|ooo|qqq|ppp|rrr|ttt|www|zzz|mmm|iii|uuu|fff|ggg|ggg|uuu|uuu|uuu|uuu|";
	private final String number3 = "111|123|707|902|104|223|537|902|111|443|" +
			"121|234|738|902|002|431|679|093|124|557|886|631|235|569|002|149|963|271|905|501|" +
			"171|734|038|002|882|215|875|193|214|997|126|361|098|888|314|111|222|341|458|082|" +
			"371|334|438|442|782|715|775|893|314|337|326|781|984|349|534|888|654|841|158|182|" +
			"098|123|435|000|312|223|343|563|123|";

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void constantLength3_alpha() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("constantLength3_alpha");
		final String inputs[] = alpha3.split("\\|");

		for (final String input : inputs) {
			analysis.train("a" + input);
			analysis.train("b" + input);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{4}");
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length * 2);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length * 2);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			Assert.assertTrue(("a" + input).matches(result.getRegExp()));
			Assert.assertTrue(("b" + input).matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void constantLength3_alnum() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("constantLength3_alnum");
		final String inputs[] = (alpha3 + number3).split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_NUMERIC + "{3}" + '|' + KnownPatterns.PATTERN_ALPHA + "{3}");
		Assert.assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			Assert.assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void constantLength3_numal() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("constantLength3_numal");
		final String inputs[] = (number3 + alpha3).split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_NUMERIC + "{3}" + '|' + KnownPatterns.PATTERN_ALPHA + "{3}");
		Assert.assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			Assert.assertTrue(input.matches(result.getRegExp()));
		}
	}


	private void simpleStringTest(final String name, final String input) throws FTAException {
		simpleArrayTest(name, input.split("\\|"));
	}

	private void simpleArrayTest(final String name, final String[] inputs) throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("DataValueFootnoteSymbol");
		int locked = -1;
		int realSamples = 0;
		int empty = 0;
		int minTrimmedLength = Integer.MAX_VALUE;
		int maxTrimmedLength = Integer.MIN_VALUE;
		int minLength = Integer.MAX_VALUE;
		int maxLength = Integer.MIN_VALUE;

		for (final String input : inputs) {
			if (analysis.train(input) && locked == -1)
				locked = realSamples;
			if (input.trim().isEmpty())
				empty++;
			else
				realSamples++;

			int len = input.trim().length();
			if (len != 0) {
				if (len > maxTrimmedLength)
					maxTrimmedLength = len;
				if (len != 0 && len < minTrimmedLength)
					minTrimmedLength = len;
				len = input.length();
				if (len > maxLength)
					maxLength = len;
				if (len != 0 && len < minLength)
					minLength = len;
			}
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, realSamples >= AnalysisConfig.DETECT_WINDOW_DEFAULT ? AnalysisConfig.DETECT_WINDOW_DEFAULT : -1);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		if (inputs.length == empty)
			Assert.assertEquals(result.getTypeQualifier(), "BLANK");
		else
			Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getBlankCount(), empty);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getBlankCount());
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		int matches = 0;
		for (int i = 0; i < inputs.length; i++) {
			if (!inputs[i].trim().isEmpty() && inputs[i].matches(result.getRegExp()))
				matches++;
		}
		Assert.assertEquals(matches, result.getMatchCount());

		if (result.getMatchCount() != 0 && !result.getRegExp().startsWith("(?i)")) {
			String re = "";
			if (result.getLeadingWhiteSpace())
				re += KnownPatterns.PATTERN_WHITESPACE;
			re += KnownPatterns.freezeANY(minTrimmedLength, maxTrimmedLength, minLength, maxLength, result.getLeadingWhiteSpace(), result.getTrailingWhiteSpace(), result.getMultiline());
			if (result.getTrailingWhiteSpace())
				re += KnownPatterns.PATTERN_WHITESPACE;
			Assert.assertEquals(result.getRegExp(), re);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void basicEnum() throws IOException, FTAException {
		final String input = "APARTMENT|APARTMENT|DUPLEX|APARTMENT|DUPLEX|CONDO|DUPLEX|CONDO|" +
				"DUPLEX|DUPLEX|CONDO|CONDO|DUPLEX|DUPLEX|CONDO|APARTMENT|" +
				"DUPLEX|CONDO|CONDO|CONDO|DUPLEX|DUPLEX|DUPLEX|DUPLEX|CONDO|" +
				"DUPLEX|DUPLEX|APARTMENT|CONDO|DUPLEX|CONDO|APARTMENT|APARTMENT|DUPLEX|" +
				"DUPLEX|APARTMENT|APARTMENT|APARTMENT|CONDO|CONDO|APARTMENT|CONDO|DUPLEX|" +
				"DUPLEX|CONDO|APARTMENT|DUPLEX|CONDO|DUPLEX|APARTMENT|CONDO|APARTMENT|" +
				"CONDO|CONDO|CONDO|CONDO|MULTI-FAMILY|DUPLEX|APARTMENT|MULTI-FAMILY|DUPLEX|" +
				"CONDO|APARTMENT|APARTMENT|CONDO|CONDO|MULTI-FAMILY|DUPLEX|CONDO|APARTMENT|" +
				"CONDO|DUPLEX|APARTMENT|CONDO|DUPLEX|DUPLEX|APARTMENT|APARTMENT|APARTMENT|" +
				"APARTMENT|APARTMENT|APARTMENT|CONDO|CONDO|APARTMENT|APARTMENT|CONDO|APARTMENT|" +
				"CONDO|APARTMENT|CONDO|APARTMENT|DUPLEX|CONDO|APARTMENT|APARTMENT|DUPLEX|" +
				"CONDO|APARTMENT|APARTMENT|DUPLEX|DUPLEX|CONDO|APARTMENT|CONDO|APARTMENT|" +
				"APARTMENT|CONDO|APARTMENT|CONDO|DUPLEX|MULTI-FAMILY|DUPLEX|CONDO|DUPLEX|" +
				"CONDO|APARTMENT|CONDO|DUPLEX|MULTI-FAMILY|APARTMENT|CONDO|DUPLEX|DUPLEX|" +
				"MULTI-FAMILY|APARTMENT|APARTMENT|APARTMENT|DUPLEX|APARTMENT|CONDO|CONDO|DUPLEX|" +
				"DUPLEX|DUPLEX|APARTMENT|DUPLEX|APARTMENT|DUPLEX|DUPLEX|DUPLEX|CONDO|" +
				"CONDO|APARTMENT|APARTMENT|APARTMENT|DUPLEX|APARTMENT|CONDO|MULTI-FAMILY|CONDO|" +
				"APARTMENT|DUPLEX|DUPLEX|MULTI-FAMILY|MULTI-FAMILY|DUPLEX|DUPLEX|DUPLEX|APARTMENT|" +
				"APARTMENT|DUPLEX|APARTMENT|DUPLEX|APARTMENT|APARTMENT|CONDO|CONDO|CONDO|" +
				"CONDO|DUPLEX|CONDO|MULTI-FAMILY|CONDO|CONDO|APARTMENT|CONDO|APARTMENT|" +
				"CONDO|APARTMENT|APARTMENT|CONDO|DUPLEX|APARTMENT|APARTMENT|APARTMENT|CONDO|" +
				"CONDO|CONDO|DUPLEX|DUPLEX|APARTMENT|CONDO|DUPLEX|DUPLEX|APARTMENT|" +
				"APARTMENT|CONDO|DUPLEX|APARTMENT|CONDO|CONDO|DUPLEX|CONDO|CONDO|" +
				"DUPLEX|CONDO|APARTMENT|DUPLEX|CONDO|CONDO|APARTMENT|DUPLEX|DUPLEX|" +
				"CONDO|APARTMENT|APARTMENT|CONDO|APARTMENT|DUPLEX|CONDO|APARTMENT|MULTI-FAMILY|" +
				"DUPLEX|CONDO|APARTMENT|APARTMENT|CONDO|APARTMENT|MULTI-FAMILY|CONDO|DUPLEX|" +
				"DUPLEX|CONDO|DUPLEX|DUPLEX|DUPLEX|DUPLEX|CONDO|CONDO|CONDO|" +
				"APARTMENT|CONDO|APARTMENT|DUPLEX|APARTMENT|APARTMENT|APARTMENT|DUPLEX|APARTMENT|" +
				"DUPLEX|APARTMENT|APARTMENT|APARTMENT|DUPLEX|DUPLEX|DUPLEX|CONDO|CONDO|" +
				"DUPLEX|CONDO|CONDO|APARTMENT|CONDO|APARTMENT|APARTMENT|APARTMENT|CONDO|" +
				"CONDO|CONDO|DUPLEX|CONDO|APARTMENT|CONDO|DUPLEX|DUPLEX|APARTMENT|" +
				"CONDO|APARTMENT|DUPLEX|DUPLEX|MULTI-FAMILY|DUPLEX|DUPLEX|DUPLEX|DUPLEX|" +
				"DUPLEX|DUPLEX|APARTMENT|DUPLEX|CONDO|APARTMENT|APARTMENT|MULTI-FAMILY|DUPLEX|" +
				"APARTMENT|APARTMENT|CONDO|CONDO|DUPLEX|CONDO|DUPLEX|DUPLEX|DUPLEX|" +
				"APARTMENT|CONDO|CONDO|CONDO|APARTMENT|";
		simpleStringTest("basicEnum", input);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void blanksLeft() throws IOException, FTAException {
		final String[] inputs = new String[] {
				" D12345",
				"A123 56", "A1234567", "A12345678", "A123456789", "A123456", "A1234567", "A12345678", "A123456789",
				"B123 56", "B1234567", "B12345678", "B123456789", "B123456", "B1234567", "B12345678", "B123456789",
				"C123 56", "C1234567", "C12345678", "C123456789", "C123456", "C1234567", "C12345678", "C123456789"
		};

		simpleArrayTest("blanksLeft", inputs);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void blanksInInput() throws IOException, FTAException {
		final String[] inputs = new String[] {
				" D12345", "  C123456789",
				"A123 56", "A1234567", "A12345678", "        ", "A123456", "A1234567", "A12345678", "A123456789",
				"B123 56", "B1234567", "B12345678", "B123456789", "B123456", "B1234567", "B12345678", "B123456789",
				"C123 56", "C1234567", "C12345678", "C123456789", "    ", "C1234567", "C12345678", "C123456789"
		};

		simpleArrayTest("blanksLeft", inputs);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void allEmpty() throws IOException, FTAException {
		final String[] inputs = new String[] {
				"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
				"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
		};

		simpleArrayTest("allEmpty", inputs);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void blanksInField() throws IOException, FTAException {
		final String[] inputs = new String[] {
				"-", "-", "", "", "", "", "^^^", "", "", "", "-", "", "", "", "", "", "", "", "",
				"", "-", "", "", "", "", "", "-", "", "", "-", "", "-", "-", "", "", "", "-", "", "",
				"-", "", "", "", "-", "", "-", "-", "", "", "", "-", "", "-", "", "", "", "****", "****", "",
				"", "", "", "", "", "****", "****", "", "****", "****", "****", "****", "", "", "", "", "", "", "****", "****",
				"", "", "", "****", "****", "****", "****", "", "", "", "", "****", "****", "", "", "****", "", "", "", "", " "
		};

		simpleArrayTest("DataValueFootnoteSymbol", inputs);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void blanksAtEndOfField() throws IOException, FTAException {
		final String[] inputs = new String[] {
				"", "Foster Road", "", "Grove Road", "", "Library", "", "Bradgers Hill Road", "", "Tomlinson Avenue", "Wheatfield Road", "Tomlinson Avenue", "",
				"Bradgers Hill Road", "", "Nixon Street", "", "Moor Lane", "", "West Hanningfield Road", "Fambridge Road", "Victoria Drive", "Maypole Road",
				"Station Road", "Roundbush Road", "Harborough Hall Lane", "Colchester Road", "Church Road", "Roundbush Road", "Harborough Hall Lane", "Colchester Road", "The Folly",
				"Little Horkesley Road", "London Road", "Home Farm Lane", "Damants Farm Lane", "Hospital Lane", "Clarendon Way", "North Station Rbt", "New Writtle Street", "Oxford Road", "School Lane",
				"Tog Lane", "Station Road", "Colchester Road", "Cooks Hill", "Clarendon Way", "North Station Rbt", "Kelvedon Road", "Latchingdon Road", "Barnhall Road", "Trusses Road",
				"", "School Lane", "Castle Drive", "The Street", "Fairstead Hall Road", "Pepples Lane", "", "Eastern Avenue", "", "Red Lane",
				"", "Granville Street", "", "yes ", "Yes Tactile", "", "Wilmot Road", "Wilmot Lane", "", "Victoria Street",
				"", "Kirk Gate", "", "Gables Lea", "", "Village Hall", "", "Morley Road", "", "Beach Road",
				"Marine Parade", "", "Mount Pleasant", "", "Heol Camlan", "", "Golwg y Mynydd", "", "Sraid na h-Eaglaise", "",
				"Geilear", "", "Tom na Ba", "", "Rathad Ur", "", "Geilear", "Rathad Ur", "", "Struan Ruadh",
				"", "Struan Ruadh", "", "Rubhachlachainn", "", "Sraid a' Chaisteil", "Sraid a' Bhanca", "Ionad Casimir", "Ionad Mhicceallaig", "", "Slighe Ruairidh", ""
		};
		simpleArrayTest("blanksAtEndOfField", inputs);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void basicPromote() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicPromote");
		final String pipedInput =
				"01000053218|0100BRP90233|0100BRP90237|0180BAA01319|0180BAC30834|0190NSC30194|0190NSC30195|0190NSC30652|0190NSC30653|0190NSC30784|" +
						"0190NSC30785|0190NSY28569|0190NSZ01245|020035037|02900033|02900033|02900039|02901210|02903036|02903037|" +
						"030051210001|030051210002|030054160002|030055200003|03700325|03700325|0380F968G059|040000002968|049000000804|049002399361|" +
						"049002399861|0500CCITY084|0500CCITY248|0500CCITY476|0500FWISH002|0500HHUNT027|0500HSTNS060|0500HSTNS062|0500SHARS006|0500SHARS016|" +
						"0590PET621|0590PET622|0590PQG571|0600CR087|0600CR290|0610CH19130|0610CH548|0610EP19031|068000000461|068000000462|" +
						"068000000502|069000024300|0690WNA02867|0690WNA02867|075071047A|075071047B|07605752|077072401A|077072401A|077072572A|" +
						"077072583A|079073001K|0800COA10071|0800COA10194|0800COA10196|0800COA10196|0800COA10204|0800COA10207|0800COA10267|0800COA10268|" +
						"0800COA10268|0800COA10268|0800COA10386|0800COA10469|0800COA10470|0800COA10490|0800COB20133|0800COB20134|0800COB20138|0800COB20139|" +
						"0800COC30257|0800COC30258|0800COC30488|0800COC30504|0800COC30505|0800COC30649|0800COC30815|0800COC30873|0800COC31003|0800COC31004|" +
						"0800COC31093|0800COC31215|0800COC31216|0800COC31221|0800COC31222|0800COC31229|0800COC31231|0800COC31306|0800COC31307|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{8,12}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			Assert.assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void belowDetectWindow() throws IOException, FTAException {
		final String BAD = "hello";
		final String[] samples = new String[] {
				"1234567", "403901",  "6200243690", "6200243691", "6200243692", "6200243693", "6200243694", "5", "8", "9",
				BAD, "020035031", "6200243635", "6200243635", "6200206290", "6200206290",
		};

		final TextAnalyzer analysis = new TextAnalyzer("belowDetectWindow");
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), FTAType.LONG);
		Assert.assertEquals(result.getRegExp(), "\\d{1,10}");
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		Assert.assertEquals(result.getShapeCount(), 6);

		for (final String sample : samples) {
			if (!BAD.contentEquals(sample))
				Assert.assertTrue(sample.matches(result.getRegExp()), sample);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void basicText() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicText");
		final int iterations = 10000;
		int locked = -1;

		for (int i = 0; i < iterations; i++) {
			if (analysis.train("primary") && locked == -1)
				locked = i;
			if (analysis.train("secondary") && locked == -1)
				locked = i;
			if (analysis.train("tertiary") && locked == -1)
				locked = i;
			if (analysis.train("fictional") && locked == -1)
				locked = i;
			if (analysis.train(null) && locked == -1)
				locked = i;
		}
		analysis.train("secondory");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT/4);
		Assert.assertEquals(result.getSampleCount(), 5 * iterations + 1);
		Assert.assertEquals(result.getMatchCount(), 4 * iterations);
		Assert.assertEquals(result.getNullCount(), iterations);
		Assert.assertEquals(result.getCardinality(), 5);
		Assert.assertEquals(result.getRegExp(), "(?i)(FICTIONAL|PRIMARY|SECONDARY|TERTIARY)");
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/(result.getMatchCount() + 1));
		Assert.assertEquals(result.getOutlierCount(), 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void textBlocks() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("textBlocks");
		final int iterations = 100000;
		int locked = -1;
		final StringBuilder line = new StringBuilder();
		int minTrimmedLength = Integer.MAX_VALUE;
		int maxTrimmedLength = Integer.MIN_VALUE;
		int minLength = Integer.MAX_VALUE;
		int maxLength = Integer.MIN_VALUE;
		final String alphabet = "abcdefhijklmnopqrstuvwxyz";

		for (int i = 0; i < iterations; i++) {
			line.setLength(0);
			final int wordCount = random.nextInt(20);
			for (int words = 0; words < wordCount; words++) {
				final int charCount = random.nextInt(10);
				for (int chars = 0; chars < charCount; chars++) {
					line.append(alphabet.charAt(random.nextInt(25)));
				}
				line.append(' ');
			}
			final String sample = line.toString().trim();
			int len = sample.length();
			if (len > maxLength)
				maxLength = len;
			if (len < minLength && len != 0)
				minLength = len;
			len = sample.trim().length();
			if (len > maxTrimmedLength)
				maxTrimmedLength = len;
			if (len < minTrimmedLength && len != 0)
				minTrimmedLength = len;
			if (analysis.train(sample) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getSampleCount(), iterations);
		Assert.assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.freezeANY(minTrimmedLength, maxTrimmedLength, minLength, maxLength, result.getLeadingWhiteSpace(), result.getTrailingWhiteSpace(), result.getMultiline()));
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setDetectWindow() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setDetectWindow");
		int locked = -1;
		int sample = 0;

		analysis.setDetectWindow(2* AnalysisConfig.DETECT_WINDOW_DEFAULT);
		for (int i = 0; i <= AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			sample++;
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = sample;
		}
		for (int i = 0; i <= AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			sample++;
			if (analysis.train(String.valueOf(random.nextDouble())) && locked == -1)
				locked = sample;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, 2 * AnalysisConfig.DETECT_WINDOW_DEFAULT + 1);
		Assert.assertEquals(result.getSampleCount(), 2 * (AnalysisConfig.DETECT_WINDOW_DEFAULT + 1));
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), FTAType.DOUBLE);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void getDetectWindowSize()  throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("getDetectWindowSize");

		Assert.assertEquals(analysis.getDetectWindow(), AnalysisConfig.DETECT_WINDOW_DEFAULT);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setDetectWindowTooSmall() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setDetectWindowTooSmall");

		try {
			analysis.setDetectWindow(AnalysisConfig.DETECT_WINDOW_DEFAULT - 1);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Cannot set detect window size below " + AnalysisConfig.DETECT_WINDOW_DEFAULT);
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setDetectWindowTooLate() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setDetectWindowTooLate");
		int locked = -1;
		int i = 0;

		for (; i <= AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		try {
			analysis.setDetectWindow(2* AnalysisConfig.DETECT_WINDOW_DEFAULT);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Cannot change size of detect window once training has started");
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setLocaleTooLate() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setLocaleTooLate");
		analysis.setTrace("enabled=true");
		final Locale locale = Locale.forLanguageTag("en-US");
		int locked = -1;
		int i = 0;

		for (; i <= AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		try {
			analysis.setLocale(locale);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Cannot adjust Locale once training has started");
			return;
		}
		Assert.fail("Exception should have been thrown");
	}


	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void getMaxCardinality() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("getMaxCardinality");

		Assert.assertEquals(analysis.getMaxCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setMaxCardinalityTooSmall() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setMaxCardinalityTooSmall");

		try {
			analysis.setMaxCardinality(-1);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Invalid value for maxCardinality -1");
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setMaxCardinalityTooLate() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setMaxCardinalityTooLate");
		int locked = -1;
		int i = 0;

		for (; i <= AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		try {
			analysis.setMaxCardinality(2* AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Cannot change maxCardinality once training has started");
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void getOutlierCount() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("getOutlierCount");

		Assert.assertEquals(analysis.getMaxOutliers(), AnalysisConfig.MAX_OUTLIERS_DEFAULT);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setMaxOutliersTooSmall() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setMaxOutliersTooSmall");

		try {
			analysis.setMaxOutliers(-1);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Invalid value for outlier count -1");
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setMaxOutliersTooLate() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setMaxOutliersTooLate");
		int locked = -1;
		int i = 0;

		for (; i <= AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		try {
			analysis.setMaxOutliers(2* AnalysisConfig.MAX_OUTLIERS_DEFAULT);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Cannot change outlier count once training has started");
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void USPhone() throws IOException, FTAException {
		String[] samples = new String[1000];

		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append("+1 ");
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append(' ');
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append(' ');
			b.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}


		final TextAnalyzer analysis = new TextAnalyzer("USPhone");
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getRegExp(), "\\+\\d \\d{3} \\d{3} \\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			Assert.assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void USPhone2() throws IOException, FTAException {
		String[] samples = new String[1000];

		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append("1.");
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append('.');
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append('.');
			b.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}


		final TextAnalyzer analysis = new TextAnalyzer("USPhone2");
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getRegExp(), "\\d\\.\\d{3}\\.\\d{3}\\.\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			Assert.assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void USPhone3() throws IOException, FTAException {
		String[] samples = new String[1000];

		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append('(');
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append(") ");
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append(' ');
			b.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}


		final TextAnalyzer analysis = new TextAnalyzer("USPhone3");
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getRegExp(), "\\(\\d{3}\\) \\d{3} \\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			Assert.assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void bigExponents() throws IOException, FTAException {
		final String[] samples = new String[] {
				"5230CGX16431", "3590E094000", "3590E092401", "3590E012300", "66004890064", "020035020", "270000009882", "020035256", "5520WDB48305", "6200600740",
				"6200243690", "6200243690", "6200243690", "6200243690", "6200243690", "6200243690", "6200243690", "6200243690", "66004589900", "66004589900",
				"020035300", "020035300", "020035347", "6020710337", "6020710337", "020035257", "020035053", "020035030", "020035030", "020035031",
				"020035031", "6200243635", "6200243635", "6200206290", "6200206290", "3590E049400", "3590E094300", "3590E094300", "3590E094300"
		};

		final TextAnalyzer analysis = new TextAnalyzer("bigExponents");
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getRegExp(), "[\\p{IsAlphabetic}\\d]{9,12}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getShapeCount(), 6);
		final Map<String, Long> shapes = result.getShapeDetails();
		Assert.assertEquals(shapes.size(), result.getShapeCount());
		Assert.assertEquals(shapes.get("999999999999"), Long.valueOf(1));
		Assert.assertEquals(shapes.get("9999XXX99999"), Long.valueOf(2));

		for (final String sample : samples) {
			Assert.assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void difficultRegExp() throws IOException, FTAException {
		String[] samples = new String[1000];

		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append('[');
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append("){[0-9] ^");
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append('$');
			b.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}


		final TextAnalyzer analysis = new TextAnalyzer("difficultRegExp");
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getRegExp(), "\\[\\d{3}\\)\\{\\[\\d-\\d\\] \\^\\d{3}\\$\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			Assert.assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void bumpMaxCardinality() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("bumpMaxCardinality");

		analysis.setMaxCardinality(2 * AnalysisConfig.MAX_CARDINALITY_DEFAULT);

		final int nullIterations = 50;
		final int iterations = 2 * AnalysisConfig.MAX_CARDINALITY_DEFAULT + 100;
		int locked = -1;

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		int cnt = 0;
		while (cnt < iterations) {
			final long randomLong = random.nextInt(Integer.MAX_VALUE) + 1000000000L;
			if (randomLong >  9999999999L)
				continue;
			if (analysis.train(String.valueOf(randomLong)) && locked == -1)
				locked = cnt;
			cnt++;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations);
		Assert.assertEquals(result.getCardinality(), 2 * AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getType(), FTAType.LONG);
		Assert.assertEquals(result.getRegExp(), "\\d{10}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void keyFieldLong() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("keyFieldLong");
		final int start = 10000;
		final int end = start + AnalysisConfig.MAX_CARDINALITY_DEFAULT + 100;
		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), end - start);
		Assert.assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getType(), FTAType.LONG);
		Assert.assertEquals(result.getKeyConfidence(), 0.9);
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void defaultMaxOutliers() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Alphabet");
		final int start = 10000;
		final int end = start + AnalysisConfig.MAX_CARDINALITY_DEFAULT + 100;
		final int outliers = 15;
		int locked = -1;

		analysis.train("A");
		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}
		analysis.train("B");
		analysis.train("C");
		analysis.train("D");
		analysis.train("E");
		analysis.train("F");
		analysis.train("G");
		analysis.train("H");
		analysis.train("I");
		analysis.train("J");
		analysis.train("K");
		analysis.train("L");
		analysis.train("M");
		analysis.train("N");
		analysis.train("O");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getType(), FTAType.LONG);
		Assert.assertEquals(result.getOutlierCount(), outliers);
		Assert.assertEquals(result.getSampleCount(), outliers + end - start);
		Assert.assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getKeyConfidence(), 0.9);
		Assert.assertEquals(result.getConfidence(), 1 - (double)15/result.getSampleCount());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testDataSignature() throws IOException, FTAException {
		final TextAnalyzer analysis1 = new TextAnalyzer("Analysis1");
		final TextAnalyzer analysis2 = new TextAnalyzer("Analysis2");
		analysis2.setTotalCount(1000000);

		final int start = 10000;

		for (int i = start; i < start + 1000; i++) {
			analysis1.train(String.valueOf(i));
		}
		final TextAnalysisResult result1 = analysis1.getResult();
		Assert.assertEquals(result1.getTotalCount(), -1);

		for (int i = start; i < start + 1000; i++) {
			analysis2.train(String.valueOf(i));
		}
		final TextAnalysisResult result2 = analysis2.getResult();
		Assert.assertEquals(result2.getTotalCount(), 1000000);

		Assert.assertEquals(result1.getStructureSignature(), result2.getStructureSignature());
		Assert.assertNotEquals(result1.getDataSignature(), result2.getDataSignature());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testMaxLength() throws IOException, FTAException {
		final int MAX_LENGTH = 512;
		final TextAnalyzer analysis = new TextAnalyzer("maxLength");

		for (int i = 0; i < 1000; i++) {
			analysis.train("Hello - " + String.valueOf(i));
		}
		analysis.setMaxLength(MAX_LENGTH);

		final TextAnalysisResult result = analysis.getResult();
		Assert.assertEquals(result.getMaxLength(), MAX_LENGTH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setMaxOutliers() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Alphabet");
		final int start = 10000;
		final int end = 12000;
		final int outliers = 15;
		final int newMaxOutliers = 12;

		int locked = -1;

		analysis.setMaxOutliers(newMaxOutliers);

		analysis.train("A");
		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}
		analysis.train("B");
		analysis.train("C");
		analysis.train("D");
		analysis.train("E");
		analysis.train("F");
		analysis.train("G");
		analysis.train("H");
		analysis.train("I");
		analysis.train("J");
		analysis.train("K");
		analysis.train("L");
		analysis.train("M");
		analysis.train("N");
		analysis.train("O");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "[\\p{IsAlphabetic}\\d]{1,5}");
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(analysis.getMaxOutliers(), newMaxOutliers);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getSampleCount(), outliers + end - start);
		//BUG		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void keyFieldString() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("keyFieldString");
		final int start = 100000;
		final int end = 120000;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train("A" + i) && locked == -1)
				locked = i - start;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), end - start);
		Assert.assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getRegExp(), "\\p{IsAlphabetic}\\d{6}");
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getKeyConfidence(), 0.9);
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void notKeyField() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notKeyField");
		final int start = 10000;
		final int end = start + AnalysisConfig.MAX_CARDINALITY_DEFAULT + 100;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}

		analysis.train(String.valueOf(start));

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), 1 + end - start);
		Assert.assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getType(), FTAType.LONG);
		Assert.assertEquals(result.getKeyConfidence(), 0.0);
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	public String[] decoder = new String[] {
			"Integer", "Boolean", "Long", "Double", "Date",
			"ISO-3166-3", "ISO-3166-2", "ZIP", "US_STATE", "CA_PROVINCE",
			"US_STREET"
	};

	public String[] generateTestData(final int type, final int length) {
		String[] result = new String[length];
		final String[] candidatesISO3166_3 = TestUtils.valid3166_3.split("\\|");
		final String[] candidatesISO3166_2 = TestUtils.valid3166_2.split("\\|");
		final String[] candidatesZips = TestUtils.validZips.split("\\|");
		final String[] candidatesUSStates = TestUtils.validUSStates.split("\\|");
		final String[] candidatesCAProvinces = TestUtils.validCAProvinces.split("\\|");

		for (int i = 0; i < length; i++)
			switch (type) {
			case 0:
				// Integer
				result[i] = String.valueOf(random.nextInt());
				break;
			case 1:
				// Boolean
				result[i] = String.valueOf(random.nextBoolean());
				break;
			case 2:
				// Long
				result[i] = String.valueOf(random.nextLong());
				break;
			case 3:
				// Double
				result[i] = String.valueOf(random.nextDouble());
				break;
			case 4:
				// Date
				result[i] = new Date(random.nextLong()).toString();
				break;
			case 5:
				// ISO 3166-3
				result[i] = candidatesISO3166_3[random.nextInt(candidatesISO3166_3.length)];
				break;
			case 6:
				// ISO 3166-2
				result[i] = candidatesISO3166_2[random.nextInt(candidatesISO3166_2.length)];
				break;
			case 7:
				// Zip Cpde
				result[i] = candidatesZips[random.nextInt(candidatesZips.length)];
				break;
			case 8:
				// US State
				result[i] = candidatesUSStates[random.nextInt(candidatesUSStates.length)];
				break;
			case 9:
				// CA Provinces
				result[i] = candidatesCAProvinces[random.nextInt(candidatesCAProvinces.length)];
				break;
			case 10:
				// US Street
				result[i] = TestUtils.validUSStreets[random.nextInt(TestUtils.validUSStreets.length)];
				break;
			}

		return result;
	}

	class AnalysisThread implements Runnable {
		private final String id;
		private final int streamType;
		private final String[] stream;
		private final TextAnalysisResult answer;
		private final TextAnalyzer analysis;

		AnalysisThread(final String id, final int streamType, final String[] stream, final TextAnalysisResult answer) throws IOException, FTAException {
			this.id = id;
			this.streamType = streamType;
			this.stream = stream;
			this.answer = answer;
			analysis = new TextAnalyzer("AnalysisThread" + id);
			//			System.out.printf("Thread %s: created, Stream: type: %s, length: %d\n",
			//					this.id, decoder[this.streamType], this.stream.length);
		}

		@Override
		public void run() {
			//			long start = System.currentTimeMillis();
			try {
				for (final String input : stream)
					analysis.train(input);

				final TextAnalysisResult result = analysis.getResult();

				Assert.assertEquals(result.getSampleCount(), answer.getSampleCount());
				Assert.assertEquals(result.getNullCount(), answer.getNullCount());
				Assert.assertEquals(result.getBlankCount(), answer.getBlankCount());
				Assert.assertEquals(result.getRegExp(), answer.getRegExp());
				Assert.assertEquals(result.getConfidence(), answer.getConfidence());
				Assert.assertEquals(result.getType(), answer.getType());
				Assert.assertEquals(result.getMinValue(), answer.getMinValue());
				Assert.assertEquals(result.getMaxValue(), answer.getMaxValue());
			} catch (FTAException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			//			System.out.printf("Thread %s: exiting, duration %d\n", id, System.currentTimeMillis() - start);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testThreading() throws IOException, FTAException, FTAException, InterruptedException {
		final int THREADS = 1000;
		Thread[] threads = new Thread[THREADS];

		for (int t = 0; t < THREADS; t++) {
			final int type = random.nextInt(decoder.length);
			final int length = 30 + random.nextInt(10000);
			final String[] stream = generateTestData(type, length);

			final TextAnalyzer analysis = new TextAnalyzer("testThreading");
			for (final String input : stream)
				analysis.train(input);

			threads[t] = new Thread(new AnalysisThread(String.valueOf(t), type, stream, analysis.getResult()));
		}

		for (int t = 0; t < THREADS; t++)
			threads[t].start();

		for (int t = 0; t < THREADS; t++)
			if (threads[t].isAlive())
				threads[t].join();
	}

	class GetPlugin {
		// one instance of plugins per thread
		private final ThreadLocal<TextAnalyzer> textAnalyzer = new ThreadLocal<>();

		public Plugins getPlugins() {
			TextAnalyzer textAnalyzer = this.textAnalyzer.get();
			if (textAnalyzer == null) {
				// initialize textAnalyzer
				textAnalyzer = new TextAnalyzer("getPlugins");
				textAnalyzer.registerDefaultPlugins(Locale.getDefault());
				this.textAnalyzer.set(textAnalyzer);
			}
			return textAnalyzer.getPlugins();
		}
	}

	class PluginThread implements Runnable {
		private final String id;

		PluginThread(final String id) throws IOException, FTAException {
			this.id = id;
		}

		@Override
		public void run() {
			final GetPlugin pluginGetter = new GetPlugin();

			for (int i = 0; i < 1000; i++)
				pluginGetter.getPlugins();
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testThreadingIssue() throws IOException, FTAException, InterruptedException {
		final int THREADS = 1000;
		Thread[] threads = new Thread[THREADS];

		for (int t = 0; t < THREADS; t++)
			threads[t] = new Thread(new PluginThread(String.valueOf(t)));

		for (int t = 0; t < THREADS; t++)
			threads[t].start();

		for (int t = 0; t < THREADS; t++)
			if (threads[t].isAlive())
				threads[t].join();
	}

	private static String someSemanticTypes[] = new String[] {
			"EMAIL", "URI.URL", "IPADDRESS.IPV4", "IPADDRESS.IPV6", "TELEPHONE", "GUID",
			"POSTAL_CODE.ZIP5_US", "POSTAL_CODE.POSTAL_CODE_UK", "POSTAL_CODE.POSTAL_CODE_CA", "POSTAL_CODE.POSTAL_CODE_AU",
			"STREET_ADDRESS_EN", "GENDER.TEXT_<LOCALE>", "COUNTRY.TEXT_EN",
			"STATE_PROVINCE.PROVINCE_CA", "STATE_PROVINCE.STATE_US", "STATE_PROVINCE.STATE_PROVINCE_NA", "STATE_PROVINCE.STATE_AU",
			"CURRENCY_CODE.ISO-4217", "COUNTRY.ISO-3166-3", "COUNTRY.ISO-3166-2",
			"AIRPORT_CODE.IATA", "CITY", "SSN",
			"NAME.FIRST", "NAME.LAST", "NAME.LAST_FIRST", "NAME.FIRST_LAST",
			"CREDIT_CARD_TYPE", "LANGUAGE.ISO-639-2", "LANGUAGE.TEXT_EN",
			"MONTH.ABBR_<LOCALE>", "MONTH.FULL_<LOCALE>", "COORDINATE.LATITUDE_DECIMAL", "COORDINATE.LONGITUDE_DECIMAL", "COORDINATE_PAIR.DECIMAL"
	};

	class LogicalTypeThread implements Runnable {
		private String id;

		LogicalTypeThread(final String id) throws IOException, FTAException {
			this.id = id;
		}

		@Override
		public void run() {
			LogicalType logical = null;
			do {
				final String semanticType = someSemanticTypes[random.nextInt(someSemanticTypes.length)];
				try {
					logical = LogicalTypeFactory.newInstance(PluginDefinition.findByQualifier(semanticType), Locale.getDefault());
				} catch (FTAException e) {
					e.printStackTrace();
				}
			} while (!(logical instanceof LogicalTypeRegExp) || ((LogicalTypeRegExp)logical).isRegExpComplete());

			for (int i = 0; i < 1000; i++) {
				final String value = logical.nextRandom();
				if (logical.isRegExpComplete() && !logical.isValid(value)) {
					System.err.println("Issue with LogicalType'" + logical.getDescription() + "', value: " + value + "\n");
					Assert.assertTrue(logical.isValid(value), value);
				}
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testLogicalTypeThreading() throws IOException, FTAException, InterruptedException {
		final int THREADS = 1000;
		Thread[] threads = new Thread[THREADS];

		for (int t = 0; t < THREADS; t++)
			threads[t] = new Thread(new LogicalTypeThread(String.valueOf(t)));

		for (int t = 0; t < THREADS; t++)
			threads[t].start();

		for (int t = 0; t < THREADS; t++)
			if (threads[t].isAlive())
				threads[t].join();
	}

	//@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void fuzzInt() throws IOException, FTAException {
		final int SAMPLES = 1000;
		final int errorRate = 1;

		for (int iter = 0; iter < 100; iter++) {
			final TextAnalyzer analysis = new TextAnalyzer("fuzzInt");
			analysis.setThreshold(100 - errorRate);
			Assert.assertEquals(analysis.getThreshold(), 100 - errorRate);
			final int length = random.nextInt(9);
			final long low = (long)Math.pow(10, length);
			long lowest = low;
			long lowestFloat = low;
			final int lowLength = String.valueOf(low).length();
			final long high = low + SAMPLES - 1;
			final int highLength = String.valueOf(high).length();
			int misses = 0;
			final boolean sticky = random.nextBoolean();
			boolean isNegative = false;
			int floats = 0;
			int strings = 0;
			int nulls = 0;
			int blanks = 0;
			int errorCase = -1;
			long firstFloat = -1;
			final String[] errorCaseDecode = new String[] { "String", "NegativeInt", "Double", "negativeDouble", "null", "blank" };

			for (long i = low; i <= high; i++) {
				if (i != low && i != high && random.nextInt(99) < 2) {
					if (errorCase == -1 || !sticky)
						errorCase = random.nextInt(6);
					switch (errorCase) {
					case 0:
						// String
						analysis.train("hello");
						strings++;
						misses++;
						break;
					case 1:
						// NegativeInt
						analysis.train(String.valueOf(-i));
						isNegative = true;
						lowest = -i;
						break;
					case 2:
						// Double
						analysis.train(String.valueOf((double)i));
						floats++;
						if (firstFloat == -1)
							firstFloat = i - low - nulls - blanks;
						misses++;
						break;
					case 3:
						// Negative Double
						analysis.train(String.valueOf((double)-i));
						isNegative = true;
						floats++;
						lowestFloat = -i;
						if (firstFloat == -1)
							firstFloat = i - low - nulls - blanks;
						misses++;
						break;
					case 4:
						// Null
						analysis.train(null);
						nulls++;
						misses++;
						break;
					case 5:
						// Blank
						analysis.train("");
						blanks++;
						misses++;
						break;
					}
				}
				else
					analysis.train(String.valueOf(i));
			}

			FTAType answer;
			String re = "";
			String min;
			String max;
			if (firstFloat != -1 && firstFloat < analysis.getDetectWindow() || floats >= (errorRate * SAMPLES)/100) {
				misses -= floats;
				answer = FTAType.DOUBLE;
				min = String.valueOf((double)Math.min(lowest, lowestFloat));
				max = String.valueOf((double)high);
				re += min.charAt(0) == '-' ? "-?\\d+|-?(\\d+)?\\.\\d+" : "\\d+|(\\d+)?\\.\\d+";
			}
			else {
				if (isNegative)
					re += "-?";
				re += "\\d{" + lowLength;
				if (lowLength != highLength)
					re += "," + highLength;
				re += "}";
				answer = FTAType.LONG;
				min = String.valueOf(lowest);
				max = String.valueOf(high);
			}

			System.err.printf("Iter: %d, length: %d, start: %d, sticky: %b, re: %s, floats: %d, firstFloat: %d, strings: %d, errorCase: %s\n",
					iter, length, low, sticky, re, floats, firstFloat, strings, sticky ? errorCaseDecode[errorCase] : "Variable");

			final TextAnalysisResult result = analysis.getResult();

			Assert.assertEquals(result.getSampleCount(), SAMPLES);
			Assert.assertEquals(result.getNullCount(), nulls);
			Assert.assertEquals(result.getBlankCount(), blanks);
			Assert.assertEquals(result.getRegExp(), re);
			Assert.assertEquals(result.getConfidence(), (double)(SAMPLES-misses)/(SAMPLES - blanks - nulls));
			Assert.assertEquals(result.getType(), answer);
			Assert.assertEquals(result.getMinValue(), min);
			Assert.assertEquals(result.getMaxValue(), max);
		}
	}
}
