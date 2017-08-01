/*
 * Copyright 2017 Tim Segall
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

/**
 * The PatternInfo class maintains a set of information about a simple pattern.  This is used
 * to derive a Type from a pattern.  For example,
 * 		new PatternInfo("(?i)true|false", null, "", "Boolean", null)
 * indicates that a case insensitive match for true or false indicates a boolean type ("Boolean").
 */
public class PatternInfo {
	String pattern;
	String generalPattern;
	int minLength;
	int maxLength;
	String format;
	String type;
	String typeQualifier;

	/**
	 * Construct a new information block for the supplied pattern.
	 * @param pattern The pattern of interest.
	 * @param type The type of the pattern.
	 * @param typeQualifier The type qualifier of the pattern (optional).
	 * @param minLength The minimum length of this pattern (-1 implies undefined)
	 * @param maxLength The maximum length of this pattern (-1 implies undefined)
	 * @param generalPattern The general case of this pattern (optional).
	 * @param format The Java format specified for a date pattern (optional).
	 */
	public PatternInfo(String pattern, String type, String typeQualifier, int minLength, int maxLength, String generalPattern, String format) {
		this.pattern = pattern;
		this.type = type;
		this.typeQualifier = typeQualifier;
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.generalPattern = generalPattern;
		this.format = format;
	}


	/**
	 * Is this pattern Numeric?
	 * @return A boolean indicating if the Type for this pattern is numeric.
	 */
	public boolean isNumeric() {
		return "Long".equals(this.type) || "Double".equals(this.type);
	}
}
