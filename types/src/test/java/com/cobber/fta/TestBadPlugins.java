/*
 * Copyright 2017-2026 Tim Segall
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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;

/**
 * Tests for TypeDeterminer plugin exception handling.
 *
 * When a plugin throws an unexpected exception during isCandidate() or isValid(),
 * the engine should: log the error, permanently disable that plugin for the stream
 * (by setting its count to Integer.MIN_VALUE), and continue analysing with all
 * remaining plugins unaffected.
 */
public class TestBadPlugins {

	private static final String[] EMAILS = {
		"alice@example.com", "bob@example.org", "carol@example.net",
		"dave@example.com", "eve@example.org", "frank@example.net",
		"grace@example.com", "hank@example.org", "ivy@example.net",
		"jack@example.com", "kate@example.org", "leo@example.net",
		"mary@example.com", "ned@example.org", "olive@example.net",
		"pete@example.com", "quinn@example.org", "rose@example.net",
		"sam@example.com", "tina@example.org",
	};

	/** Register a throwing LogicalTypeInfinite alongside the built-in EMAIL plugin.
	 *  EMAIL must still be detected; the bad plugin must not prevent it. */
	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testThrowingInfiniteDoesNotBlockDetection() throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("email_field");
		analysis.setLocale(Locale.forLanguageTag("en-US"));

		final PluginDefinition bad = new PluginDefinition("THROWING_INFINITE", "com.cobber.fta.PluginThrowingInfinite");
		bad.validLocales = new PluginLocaleEntry[] { new PluginLocaleEntry("*") };
		final List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(bad);
		analysis.getPlugins().registerPluginList(plugins, analysis.getConfig(), false);

		for (final String email : EMAILS)
			analysis.train(email);

		final TextAnalysisResult result = analysis.getResult();

		assertNotNull(result);
		assertEquals(result.getSemanticType(), "EMAIL");
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSampleCount(), EMAILS.length);
		assertEquals(result.getMatchCount(), EMAILS.length);
	}

	/** Register ONLY the throwing LogicalTypeInfinite (no built-in plugins).
	 *  The analysis must complete without exception and return a coherent base type. */
	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testThrowingInfiniteOnlyPluginCompletesGracefully() throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("data");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);

		final PluginDefinition bad = new PluginDefinition("THROWING_INFINITE", "com.cobber.fta.PluginThrowingInfinite");
		bad.validLocales = new PluginLocaleEntry[] { new PluginLocaleEntry("*") };
		final List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(bad);
		analysis.getPlugins().registerPluginList(plugins, analysis.getConfig(), false);

		for (final String email : EMAILS)
			analysis.train(email);

		final TextAnalysisResult result = analysis.getResult();

		assertNotNull(result);
		// No semantic type should win — the only plugin was disabled after its first throw
		assertNull(result.getSemanticType());
		// Base type must still be resolved correctly
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSampleCount(), EMAILS.length);
	}

	/** Register a throwing LogicalTypeRegExp alongside the built-in EMAIL plugin.
	 *  EMAIL (an Infinite type) must still be detected; the bad RegExp plugin must not prevent it. */
	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testThrowingRegExpDoesNotBlockDetection() throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("email_field");
		analysis.setLocale(Locale.forLanguageTag("en-US"));

		final PluginDefinition bad = new PluginDefinition("THROWING_REGEXP", "com.cobber.fta.PluginThrowingRegExp");
		bad.validLocales = new PluginLocaleEntry[] { new PluginLocaleEntry("*", null, 90, ".*") };
		final List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(bad);
		analysis.getPlugins().registerPluginList(plugins, analysis.getConfig(), false);

		for (final String email : EMAILS)
			analysis.train(email);

		final TextAnalysisResult result = analysis.getResult();

		assertNotNull(result);
		assertEquals(result.getSemanticType(), "EMAIL");
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSampleCount(), EMAILS.length);
		assertEquals(result.getMatchCount(), EMAILS.length);
	}

	/** Register ONLY the throwing LogicalTypeRegExp (no built-in plugins).
	 *  The analysis must complete without exception and return a coherent result.
	 *  Note: the plugin's analyzeSet() (called by ResultFinalizer) is independent of the isValid() path
	 *  that throws; the plugin may or may not win via analyzeSet().  What matters is that the exception
	 *  in isValid() during the detect window did not propagate or corrupt the analysis. */
	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testThrowingRegExpOnlyPluginCompletesGracefully() throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("data");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);

		final PluginDefinition bad = new PluginDefinition("THROWING_REGEXP", "com.cobber.fta.PluginThrowingRegExp");
		bad.validLocales = new PluginLocaleEntry[] { new PluginLocaleEntry("*", null, 90, ".*") };
		final List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(bad);
		analysis.getPlugins().registerPluginList(plugins, analysis.getConfig(), false);

		for (final String email : EMAILS)
			analysis.train(email);

		// The key assertion: analysis completes and returns a valid, coherent result despite the throws
		final TextAnalysisResult result = analysis.getResult();

		assertNotNull(result);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSampleCount(), EMAILS.length);
	}

	/** Both a throwing Infinite and a throwing RegExp plugin are registered alongside EMAIL.
	 *  Both bad plugins are disabled; EMAIL detection must be unaffected. */
	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testBothThrowingPluginsTogetherDoNotBlockDetection() throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("email_field");
		analysis.setLocale(Locale.forLanguageTag("en-US"));

		final List<PluginDefinition> plugins = new ArrayList<>();

		final PluginDefinition badInfinite = new PluginDefinition("THROWING_INFINITE", "com.cobber.fta.PluginThrowingInfinite");
		badInfinite.validLocales = new PluginLocaleEntry[] { new PluginLocaleEntry("*") };
		plugins.add(badInfinite);

		final PluginDefinition badRegExp = new PluginDefinition("THROWING_REGEXP", "com.cobber.fta.PluginThrowingRegExp");
		badRegExp.validLocales = new PluginLocaleEntry[] { new PluginLocaleEntry("*", null, 90, ".*") };
		plugins.add(badRegExp);

		analysis.getPlugins().registerPluginList(plugins, analysis.getConfig(), false);

		for (final String email : EMAILS)
			analysis.train(email);

		final TextAnalysisResult result = analysis.getResult();

		assertNotNull(result);
		assertEquals(result.getSemanticType(), "EMAIL");
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSampleCount(), EMAILS.length);
		assertEquals(result.getMatchCount(), EMAILS.length);
	}
}
