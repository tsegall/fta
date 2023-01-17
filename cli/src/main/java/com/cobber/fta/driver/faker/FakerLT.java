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

	public FakerLT(PluginDefinition plugin) {
		super(plugin);
	}

	public void setControl(FakerParameters parameters) {
		this.parameters = parameters;
	}

	@Override
	public String getRegExp() {
		return null;
	}

	@Override
	public boolean isValid(String input, boolean detectMode, long count) {
		return false;
	}

	@Override
	public PluginAnalysis analyzeSet(AnalyzerContext context, long matchCount, long realSamples, String currentRegExp,
			Facts facts, FiniteMap cardinality, FiniteMap outliers, TokenStreams tokenStreams,
			AnalysisConfig analysisConfig) {
		return null;
	}

	@Override
	public boolean isClosed() {
		return false;
	}
}
