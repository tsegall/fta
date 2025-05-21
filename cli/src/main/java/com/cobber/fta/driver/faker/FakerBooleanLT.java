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
package com.cobber.fta.driver.faker;

import java.util.Locale;
import java.util.Random;

import com.cobber.fta.Keywords;
import com.cobber.fta.PluginDefinition;

public class FakerBooleanLT extends FakerLT {
	private boolean initialized = false;
	private final String[] values = new String[2];
	final Random random = new Random(31415926);

	public FakerBooleanLT(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		if (!initialized) {
			if (parameters != null) {
				if (locale == null)
					locale = Locale.getDefault();
	            if (parameters.format != null) {
	            	if ("YES_NO".equals(parameters.format)) {
	            		final Keywords keywords = Keywords.getInstance(locale);
	            		values[0] = keywords.get("YES");
	            		values[1] = keywords.get("NO");
	            	}
	            	else if ("Y_N".equals(parameters.format)) {
	            		values[0] = "Y";
	            		values[1] = "N";
	            	}
	            	else if ("ONE_ZERO".equals(parameters.format)) {
	            		values[0] = "1";
	            		values[1] = "0";
	            	}
	            	else if ("TRUE_FALSE".equals(parameters.format)) {
	            		values[0] = "TRUE";
	            		values[1] = "FALSE";
	            	}
	            }
			}
			initialized = true;
		}

		return values[random.nextInt(2)];
	}
}
