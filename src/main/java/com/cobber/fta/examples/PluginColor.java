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
package com.cobber.fta.examples;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.Shapes;
import com.cobber.fta.TypeFacts;

public class PluginColor extends LogicalTypeFinite {
	public static final String SEMANTIC_TYPE = "COLOR.TEXT_EN";
	private static Set<String> members = new HashSet<>();
	private static String[] colors = new String[] {
			"RED",  "GREEN", "BLUE", "PINK", "BLACK", "WHITE", "ORANGE", "PURPLE",
			"GREY", "GREEN", "YELLOW", "MAUVE", "CREAM", "BROWN", "SILVER", "GOLD",
			"PEACH", "OLIVE", "LEMON", "LILAC", "BEIGE", "AMBER", "BURGUNDY"
	};

	static {
		members.addAll(Arrays.asList(colors));
	}

	public PluginColor(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}


	@Override
	public String nextRandom() {
		return colors[random.nextInt(colors.length)];
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String getRegExp() {
		return ".+";
	}

	@Override
	public String isValidSet(final String dataStreamName, final long matchCount, final long realSamples, final TypeFacts facts,
			final Map<String, Long> cardinality, final Map<String, Long> outliers, final Shapes shapes) {
		if (outliers.size() > 3)
			return ".+";

		if ((double)matchCount / realSamples >= getThreshold()/100.0)
			return null;

		return ".+";
	}
}
