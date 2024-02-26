/*
 *  * Copyright 2017-2024 Tim Segall
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

public class PluginAnalysis {
	private final boolean valid;
	private String newPattern;

	/** Analysis was successful. */
	public final static PluginAnalysis OK = new PluginAnalysis();

	/** Analysis was unsuccessful - and we want to back out to a wildcard match. */
	public final static PluginAnalysis SIMPLE_NOT_OK = new PluginAnalysis(KnownTypes.PATTERN_ANY_VARIABLE);

	/**
	 * Construct a 'happy' analysis.
	 */
	private PluginAnalysis() {
		valid = true;
	}

	/**
	 * Construct an 'unhappy' analysis - we think this set is not a valid instance of this Semantic Type.
	 * @param newPattern The recommended new pattern.
	 */
	public PluginAnalysis(final String newPattern) {
		valid = false;
		this.newPattern = newPattern;
	}

	public boolean isValid() {
		return valid;
	}

	public String getNewPattern() {
		return newPattern;
	}
}