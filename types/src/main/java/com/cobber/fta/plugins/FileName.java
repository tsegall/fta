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
package com.cobber.fta.plugins;

import java.util.Locale;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Content;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect a File Name.
 */
public class FileName extends LogicalTypeInfinite {
	private static final String REGEXP = ".+";

	private SingletonSet extensionsRef;
	private Set<String> extensions;
	private int maxExtensionLength;

	/**
	 * Construct a FileName plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public FileName(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		return Utils.getRandomDigits(getRandom(), 8) + "." + extensionsRef.getRandom(getRandom());
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		extensionsRef = new SingletonSet(new Content("resource", "/reference/file_extension.csv"));
		extensions = extensionsRef.getMembers();

		maxExtensionLength = extensions.stream().map(String::length).max(Integer::compareTo).get();

		return true;
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		return validate(input.trim(), true, count);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return validate(trimmed, true, 0);
	}

	/**
	 * This is NOT a test for filename validity - it is a credibility test.
	 * @param input The input to test
	 * @return True if we 'think' this is likely a filename.
	 */
	private boolean credibleFilename(final String input) {
		final int len = input.length();
		if (len < 5 || len > 300)
			return false;

		// Cheap test for a Windows filename if the Drive is present
		if (Utils.isSimpleAlpha(input.charAt(0)) && input.charAt(1) == ':' && (input.charAt(2) == '/' || input.charAt(2) == '\\'))
			return true;

		int sepCount = 0;
		int letters = 0;
		int sepIndex = -1;
		int doubled = 1;
		char sep = '\0';
		char lastCh = '\0';
		for (int i = 0; i < len; i++) {
			final char ch = input.charAt(i);
			if (sep == '\0' && (ch == '/' || ch == '\\'))
				sep = ch;
			if (Character.isISOControl(ch))
				return false;
			if (Character.isLetter(ch))
				letters++;
			switch (ch) {
			case '/':
				if (sep == '\\')
					return false;
				if (lastCh != '\0' && lastCh == '/')
					return false;
				if (letters == 0)
					return false;
				sepCount++;
				sepIndex = i;
				letters = 0;
				break;
			case '\\':
				if (sep == '/')
					return false;
				if (lastCh == '\\') {
					doubled = 2;
					if (i > 2 && input.charAt(i = 2) == '\\')
						return false;
				}
				if (letters == 0)
					return false;
				sepCount++;
				sepIndex = i;
				letters = 0;
				break;
			case '<':
			case '>':
			case '?':
			case '*':
				// These are valid in Unix filenames - just not very likely!
				return false;
			case ':':
			case '"':
			case '|':
				if (sep == '\\')
					return false;
				break;
			}
			lastCh = ch;
			if (i > sepIndex + 30)
				return false;
		}

		final int segments = sepCount/doubled;

		return segments > 0 && segments < 10;
	}

	public boolean validate(final String input, final boolean detectMode, final long count) {
		final int length = input.length();
		if (length < 3)
			return false;

		final int extensionIndex = input.lastIndexOf('.');
		if (extensionIndex == -1)
			return false;
		if (extensionIndex == length - 1 || extensionIndex < length - (maxExtensionLength + 1))
			return false;

		if (Character.toUpperCase(input.charAt(0)) == 'H' &&
				(input.startsWith("http:") || input.startsWith("HTTP:") || input.startsWith("https:") || input.startsWith("HTTPS:")))
			return false;

		final String extension = input.substring(extensionIndex + 1).toUpperCase(Locale.ENGLISH);

		return extensions.contains(extension) || credibleFilename(input);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (getHeaderConfidence(context.getStreamName()) >= 99)
			return PluginAnalysis.OK;

		final int minCardinality = 5;
		final int minSamples = 5;

		if (cardinality.size() < minCardinality || realSamples < minSamples)
			return PluginAnalysis.SIMPLE_NOT_OK;

		if (getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return PluginAnalysis.SIMPLE_NOT_OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final double confidence = (double)matchCount/realSamples;
		final int headerConfidence = getHeaderConfidence(context.getStreamName());

		if (headerConfidence >= 99)
			return Math.min(confidence + 0.20, 1.0);

		return headerConfidence == 0 ? Math.min(confidence, 0.95) : confidence;
	}
}
