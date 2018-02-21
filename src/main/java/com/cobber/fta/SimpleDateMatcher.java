/*
 * Copyright 2017-2018 Tim Segall
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SimpleDateMatcher {
	private static Map<String, SimpleDateMatcher> knownMatchers = new HashMap<String, SimpleDateMatcher>();

	static {
		Set<SimpleDateMatcher> matchers = new HashSet<SimpleDateMatcher>();

		matchers.add(new SimpleDateMatcher("d{4} d{2} d{2}", "yyyy MM dd", new int[] {8, 2, 5, 2, 0, 4}));

		matchers.add(new SimpleDateMatcher("d{4} d{2} d{2}", "yyyy MM dd", new int[] {8, 2, 5, 2, 0, 4}));
		matchers.add(new SimpleDateMatcher("d{4} d d{2}", "yyyy M dd", new int[] {7, 2, 5, 1, 0, 4}));
		matchers.add(new SimpleDateMatcher("d{4} d{2} d", "yyyy MM d", new int[] {8, 1, 5, 2, 0, 4}));
		matchers.add(new SimpleDateMatcher("d{4} d d", "yyyy M d", new int[] {7, 1, 5, 1, 0, 4}));

		matchers.add(new SimpleDateMatcher("d{2} a{3} d{4}", "dd MMM yyyy", new int[] {0, 2, 3, 3, 7, 4}));
		matchers.add(new SimpleDateMatcher("d a{3} d{4}", "d MMM yyyy", new int[] {0, 1, 2, 3, 6, 4}));
		matchers.add(new SimpleDateMatcher("d{2}-a{3}-d{4}", "dd-MMM-yyyy", new int[] {0, 2, 3, 3, 7, 4}));
		matchers.add(new SimpleDateMatcher("d-a{3}-d{4}", "d-MMM-yyyy", new int[] {0, 1, 2, 3, 6, 4}));
		matchers.add(new SimpleDateMatcher("d{2}/a{3}/d{4}", "dd/MMM/yyyy", new int[] {0, 2, 3, 3, 7, 4}));
		matchers.add(new SimpleDateMatcher("d/a{3}/d{4}", "d/MMM/yyyy", new int[] {0, 1, 2, 3, 6, 4}));

		matchers.add(new SimpleDateMatcher("d{2} a{4} d{4}", "dd MMMM yyyy", new int[] {0, 2, 3, -5, -4, 4}));
		matchers.add(new SimpleDateMatcher("d a{4} d{4}", "d MMMM yyyy", new int[] {0, 1, 2, -5, -4, 4}));
		matchers.add(new SimpleDateMatcher("d{2}-a{4}-d{4}", "dd-MMMM-yyyy", new int[] {0, 2, 3, -5, -4, 4}));
		matchers.add(new SimpleDateMatcher("d-a{4}-d{4}", "d-MMMM-yyyy", new int[] {0, 1, 2, -5, -4, 4}));
		matchers.add(new SimpleDateMatcher("d{2}/a{4}/d{4}", "dd/MMMM/yyyy", new int[] {0, 2, 3, -5, -4, 4}));
		matchers.add(new SimpleDateMatcher("d/a{4}/d{4}", "d/MMMM/yyyy", new int[] {0, 1, 2, -5, -4, 4}));

		matchers.add(new SimpleDateMatcher("d{2} a{3} d{2}", "dd MMM yy", new int[] {0, 2, 3, 3, 7, 2}));
		matchers.add(new SimpleDateMatcher("d a{3} d{2}", "d MMM yy", new int[] {0, 1, 2, 3, 6, 2}));
		matchers.add(new SimpleDateMatcher("d{2}-a{3}-d{2}", "dd-MMM-yy", new int[] {0, 2, 3, 3, 7, 2}));
		matchers.add(new SimpleDateMatcher("d-a{3}-d{2}", "d-MMM-yy", new int[] {0, 1, 2, 3, 6, 2}));
		matchers.add(new SimpleDateMatcher("d{2}/a{3}/d{2}", "dd/MMM/yy", new int[] {0, 2, 3, 3, 7, 2}));
		matchers.add(new SimpleDateMatcher("d/a{3}/d{2}", "d/MMM/yy", new int[] {0, 1, 2, 3, 6, 2}));

		matchers.add(new SimpleDateMatcher("a{3} d{2}, d{4}", "MMM dd',' yyyy", new int[] {4, 2, 0, 3, 8, 4}));
		matchers.add(new SimpleDateMatcher("a{3} d, d{4}", "MMM d',' yyyy", new int[] {4, 1, 0, 3, 7, 4}));
		matchers.add(new SimpleDateMatcher("a{3} d d{4}", "MMM dd yyyy", new int[] {4, 2, 0, 3, 7, 4}));
		matchers.add(new SimpleDateMatcher("a{3} d d{4}", "MMM d yyyy", new int[] {4, 1, 0, 3, 6, 4}));
		matchers.add(new SimpleDateMatcher("a{3}-þþ-d{4}", "MMM-dd-yyyy", new int[] {4, 2, 0, 3, 7, 4}));
		matchers.add(new SimpleDateMatcher("a{3}-d-d{4}", "MMM-d-yyyy", new int[] {4, 1, 0, 3, 6, 4}));

		matchers.add(new SimpleDateMatcher("a{4} d{2}, d{4}", "MMMM dd',' yyyy", new int[] {-8, 2, 0, 3, -4, 4}));
		matchers.add(new SimpleDateMatcher("a{4} d, d{4}", "MMMM d',' yyyy", new int[] {-7, 1, 0, 3, -4, 4}));
		matchers.add(new SimpleDateMatcher("a{4} d{2} d{4}", "MMMM dd yyyy", new int[] {-7, 2, 0, 3, -4, 4}));
		matchers.add(new SimpleDateMatcher("a{4} d d{4}", "MMMM d yyyy", new int[] {-6, 1, 0, 3, -4, 4}));
		matchers.add(new SimpleDateMatcher("a{4}-d{2}-d{4}", "MMMM-dd-yyyy", new int[] {-7, 2, 0, 3, -4, 4}));
		matchers.add(new SimpleDateMatcher("a{4}-d-d{4}", "MMMM-d-yyyy", new int[] {-6, 1, 0, 3, -4, 4}));

		matchers.add(new SimpleDateMatcher("d{8}Td{6}Z", "yyyyMMdd'T'HHmmss'Z'", new int[] {6, 2, 4, 2, 0, 4}));
		matchers.add(new SimpleDateMatcher("d{8}Td{6}", "yyyyMMdd'T'HHmmss", new int[] {6, 2, 4, 2, 0, 4}));
		matchers.add(new SimpleDateMatcher("d{8}Td{6}+d{4}", "yyyyMMdd'T'HHmmssx", new int[] {6, 2, 4, 2, 0, 4}));
		matchers.add(new SimpleDateMatcher("d{8}Td{6}-d{4}", "yyyyMMdd'T'HHmmssx", new int[] {6, 2, 4, 2, 0, 4}));
		matchers.add(new SimpleDateMatcher("d{8}Td{6}.d{3}+d{4}", "yyyyMMdd'T'HHmmss.SSSx", new int[] {6, 2, 4, 2, 0, 4}));
		matchers.add(new SimpleDateMatcher("d{8}Td{6}.d{3}-d{4}", "yyyyMMdd'T'HHmmss.SSSx", new int[] {6, 2, 4, 2, 0, 4}));

		matchers.add(new SimpleDateMatcher("d{2}/a{3}/d{2} d:d{2} P", "dd/MMM/yy h:mm a", new int[] {0, 2, 3, 3, 7, 2}));
		matchers.add(new SimpleDateMatcher("d{2}/a{3}/d{2} d{2}:d{2} P", "dd/MMM/yy hh:mm a", new int[] {0, 2, 3, 3, 7, 2}));

		for (SimpleDateMatcher sdm : matchers) {
			knownMatchers.put(sdm.getMatcher(), sdm);
		}
	}

	private final String matcher;
	private final String format;
	private int dayOffset;
	private final int dayLength;
	private int monthOffset;
	private final int monthLength;
	private int yearOffset;
	private final int yearLength;

	SimpleDateMatcher(final String matcher, final String format, final int[] dateFacts) {
		this.matcher = matcher;
		this.format = format;
		this.dayOffset = dateFacts[0];
		this.dayLength = dateFacts[1];
		this.monthOffset = dateFacts[2];
		this.monthLength = dateFacts[3];
		this.yearOffset = dateFacts[4];
		this.yearLength = dateFacts[5];
	}

	String getMatcher() {
		return matcher;
	}

	public int getMonthOffset() {
		return monthOffset;
	}

	public int getMonthLength() {
		return monthLength;
	}

	public int getDayOffset() {
		return dayOffset;
	}

	public int getYearOffset() {
		return yearOffset;
	}

	public int getYearLength() {
		return yearLength;
	}

	public String getFormat() {
		return format;
	}

	public int getDayLength() {
		return dayLength;
	}

	public static SimpleDateMatcher get(String pattern) {
		return knownMatchers.get(pattern);
	}
}
