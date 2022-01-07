/*
 * Copyright 2017-2022 Tim Segall
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

import java.util.Set;

/**
 * A set of facts (based on type) for the Analysis in question.
 */
public class FactsTypeBased {
	/** The minimum value observed. */
	protected String minValue;
	/** The maximum value observed. */
	protected String maxValue;
	/** The mean of the observed values (Numeric types only). */
	protected Double mean;
	/** The variance of the observed values (Numeric types only). */
	protected Double variance;
	/** The top 10  values. */
	protected Set<String> topK;
	/** The bottom 10  values. */
	protected Set<String> bottomK;
}
