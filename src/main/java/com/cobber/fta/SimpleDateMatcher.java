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

public class SimpleDateMatcher {
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
}
