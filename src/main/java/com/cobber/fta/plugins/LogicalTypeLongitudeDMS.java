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
package com.cobber.fta.plugins;

import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAType;

public class LogicalTypeLongitudeDMS extends LogicalTypeCoordinateDMS {
	public static final String SEMANTIC_TYPE = "COORDINATE.LATITUDE_DMS";
	public static final String REGEXP = "(\\d{5,7}|\\d{1,2} \\d{1,2} \\d{1,2} ?)[EeWw]";
	private static final char[] DIRECTION = { 'E', 'W', 'e', 'w' };
	private static final int MAX_DEGREES = 180;

	public LogicalTypeLongitudeDMS(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	protected
	char[] getDirectionChars() {
		return DIRECTION;
	}

	@Override
	protected
	int getMaxDegrees() {
		return MAX_DEGREES;
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}
}
