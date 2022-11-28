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

import java.security.SecureRandom;

import com.cobber.fta.core.FTAPluginException;

/**
 * All Semantic Types implemented via Java code typically extend this class.
 */
public abstract class LogicalTypeCode extends LogicalType {
	protected SecureRandom random;

	public LogicalTypeCode(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		random = new SecureRandom(new byte[] { 3, 1, 4, 1, 5, 9, 2 });

		return true;
	}

	@Override
	public void seed(final byte[] seed) {

		random = new SecureRandom(seed);
	}
}
