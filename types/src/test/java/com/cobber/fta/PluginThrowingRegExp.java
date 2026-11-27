/*
 * Copyright 2017-2026 Tim Segall
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

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;

/** Test fixture: LogicalTypeRegExp whose isValid() always throws. */
public class PluginThrowingRegExp extends LogicalTypeRegExp {

	public PluginThrowingRegExp(final PluginDefinition plugin) throws FTAPluginException {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);
		return true;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		throw new RuntimeException("deliberate isValid failure");
	}

	@Override
	public String getSemanticType() {
		return "THROWING_REGEXP";
	}

	@Override
	public String getRegExp() {
		return ".*";
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}
}
