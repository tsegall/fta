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

/**
 * A set of core facts for the Analysis in question.
 */
public final class FactsCore {
	/** The minimum length (not trimmed) -- Only relevant for Numeric, Boolean and String. Note: For String and Boolean types this length includes any whitespace. */
	protected int minRawLength = Integer.MAX_VALUE;
	/** The maximum length (not trimmed) -- Only relevant for Numeric, Boolean and String. Note: For String and Boolean types this length includes any whitespace. */
	protected int maxRawLength = Integer.MIN_VALUE;
	/** Are any elements multi-line? */
	protected boolean multiline;
	/** Do any elements have leading White Space? */
	protected boolean leadingWhiteSpace;
	/** Do any elements have trailing White Space? */
	protected boolean trailingWhiteSpace;
	/** The percentage confidence (0-1.0) that the observed stream is a Key field. */
	protected Double keyConfidence;
	/** The number of leading zeros seen in sample set.  Only relevant for type Long. */
	protected long leadingZeroCount;
	/** Get the Decimal Separator used to interpret Doubles.  Only relevant for type double. */
	protected char decimalSeparator = '.';
	/** What is the uniqueness percentage of this column. */
	protected Double uniqueness;
}
