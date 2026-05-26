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

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;

public class TestLogicalTypeFiniteSimpleExternal {

	private PluginDefinition buildListPlugin(final String[] members) {
		final PluginDefinition defn = new PluginDefinition();
		defn.semanticType = "TEST.COLORS";
		defn.pluginType = "list";
		defn.content = new Content(members);
		defn.backout = ".*";
		defn.threshold = 95;
		defn.baseType = FTAType.STRING;
		defn.validLocales = PluginLocaleEntry.simple(new String[] { "*" });
		return defn;
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void uppercaseMembersEnglishLocaleOK() throws FTAPluginException {
		final LogicalType lt = LogicalTypeFactory.newInstance(
				buildListPlugin(new String[] { "RED", "GREEN", "BLUE" }),
				new AnalysisConfig(Locale.US));
		assertNotNull(lt);
		assertTrue(lt.isValid("RED"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void lowercaseMembersEnglishLocaleThrows() {
		try {
			LogicalTypeFactory.newInstance(
					buildListPlugin(new String[] { "red", "green", "blue" }),
					new AnalysisConfig(Locale.US));
			throw new AssertionError("Expected FTAPluginException for lowercase members with English locale");
		} catch (FTAPluginException e) {
			assertTrue(e.getMessage().contains("lower case"), "Exception message should mention 'lower case'");
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void lowercaseMembersNonEnglishLocaleOK() throws FTAPluginException {
		// German locale is not "en" — lowercase members are allowed
		final LogicalType lt = LogicalTypeFactory.newInstance(
				buildListPlugin(new String[] { "rot", "grün", "blau" }),
				new AnalysisConfig(Locale.GERMAN));
		assertNotNull(lt);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void emptyMembersEnglishLocaleOK() throws FTAPluginException {
		// Empty member set skips the lowercase check entirely
		final LogicalType lt = LogicalTypeFactory.newInstance(
				buildListPlugin(new String[] {}),
				new AnalysisConfig(Locale.US));
		assertNotNull(lt);
	}
}
