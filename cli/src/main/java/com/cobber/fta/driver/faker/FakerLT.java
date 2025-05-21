/*
 * Copyright 2017-20252 Tim Segall
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

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeCode;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.token.TokenStreams;

public abstract class FakerLT extends LogicalTypeCode {
	FakerParameters parameters;

	public FakerLT(final PluginDefinition plugin) {
		super(plugin);
	}

	public void setControl(final FakerParameters parameters) {
		this.parameters = parameters;
	}

	@Override
	public String getRegExp() {
		return null;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		return false;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams,
			AnalysisConfig analysisConfig) {
		return null;
	}

	@Override
	public boolean isClosed() {
		return false;
	}
}
