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

import com.cobber.fta.core.FTAPluginException;

public class LogicalTypeFiniteSimpleExternal extends LogicalTypeFiniteSimple {
	public LogicalTypeFiniteSimpleExternal(final PluginDefinition plugin) {
		super(plugin, plugin.backout, plugin.threshold);
		setContent(plugin.content);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		// Worth a quick check to see if a user is trying to register a file with lower case characters.  Unfortunately, this is restricted to English as some languages e.g. German
		// have lower case characters 'ß' which effectively masquerades as upper case.  Although an upper case version was added in 2008.
		if ("en".equals(analysisConfig.getLocale().getLanguage()) && !getMembers().isEmpty())
			for (final String member : getMembers())
				if (member.chars().anyMatch(Character::isLowerCase))
					throw new FTAPluginException("Logical Type: " + defn.semanticType + " (" + defn.content + ") contains lower case characters: '" + member + "'");

		return super.initialize(analysisConfig);
	}
}
