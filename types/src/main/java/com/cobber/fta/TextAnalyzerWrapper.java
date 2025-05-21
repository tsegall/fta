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
package com.cobber.fta;

/**
 * Simple class used to aid serialization/de-serialization of a TextAnalyzer.
 */
class TextAnalyzerWrapper {
	public AnalysisConfig analysisConfig;
	public AnalyzerContext analyzerContext;
	public Facts facts;

	TextAnalyzerWrapper() {
	}

	TextAnalyzerWrapper(final AnalysisConfig analysisConfig, final AnalyzerContext analyzerContext, final Facts facts) {
		this.analysisConfig = analysisConfig;
		this.analyzerContext = analyzerContext;
		this.facts = facts;
	}
}
