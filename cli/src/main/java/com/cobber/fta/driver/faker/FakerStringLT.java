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
package com.cobber.fta.driver.faker;

import java.security.SecureRandom;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.RegExpGenerator;

import nl.flotsam.xeger.Xeger;

public class FakerStringLT extends FakerLT {
	private static final SecureRandom random = new SecureRandom();
	private LogicalTypeInfinite freeText;
	private boolean initialized = false;
	private int minLength;
	private int maxLength;
	private String format;
	private String[] values;
	private Xeger generator;

	public FakerStringLT(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);
		freeText = (LogicalTypeInfinite) LogicalTypeFactory.newInstance(PluginDefinition.findByName("FREE_TEXT"), analysisConfig);

		return true;
	}

	@Override
	public String nextRandom() {
		if (!initialized) {
			if (parameters != null) {
				minLength = parameters.minLength;
				maxLength = parameters.maxLength;
				format = parameters.format;
				values = parameters.values;
				if (format != null)
					generator = new Xeger(RegExpGenerator.toAutomatonRE(format, true));
			}

			initialized = true;
		}

		if (values != null)
			return values[random.nextInt(values.length)];

		if (format.startsWith(".{")) {
			String text = freeText.nextRandom();
			while (text.length() < minLength) {
				text += " " + freeText.nextRandom();
			}
			if (text.length() > maxLength) {
				int offset = maxLength;
				while (text.charAt(offset) != ' ' && offset != 0)
					offset--;
				if (offset != 0)
					return text.substring(0, offset);
			}
		}

		return generator.generate();
	}
}
