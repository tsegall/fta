/*
 * Copyright 2017-2019 Tim Segall
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
import java.util.Random;
import java.util.Set;

import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.StringFacts;

public class PluginColor extends LogicalTypeFinite {
	public final static String SEMANTIC_TYPE = "COLOR.TEXT_EN";
	private static Set<String> members = new HashSet<String>();
	private static String colors[] = new String[] {
			"RED",  "GREEN", "BLUE", "PINK", "BLACK", "WHITE", "ORANGE", "PURPLE",
			"GREY", "GREEN", "YELLOW", "MAUVE", "CREAM", "BROWN", "SILVER", "GOLD",
			"PEACH", "OLIVE", "LEMON", "LILAC", "BEIGE", "AMBER", "BURGUNDY"
	};
	private static Random random = new Random(401);

	static {
		members.addAll(Arrays.asList(colors));
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
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts,
			Map<String, Long> cardinality, Map<String, Long> outliers) {
		if (outliers.size() > 3)
			return ".+";

		if ((double)matchCount / realSamples >= getThreshold()/100.0)
			return null;

		return ".+";
	}
}
