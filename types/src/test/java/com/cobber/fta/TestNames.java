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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;

public class TestNames {

	final PluginDefinition pluginLastName = PluginDefinition.findByName("NAME.LAST");
	final PluginDefinition pluginLastFirstName = PluginDefinition.findByName("NAME.LAST_FIRST");

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void nameLast() throws IOException, FTAException {
		final LogicalType validator = LogicalTypeFactory.newInstance(pluginLastName, new AnalysisConfig());

		assertTrue(validator.isValid("TATE Jr."));
		assertTrue(validator.isValid("TATE, Jr"));
		assertTrue(validator.isValid("TATE"));
		assertFalse(validator.isValid("TATE, BRIAN E"));
		assertTrue(validator.isValid("Davis Brown"));
		assertTrue(validator.isValid("Davis-Brown"));
		assertFalse(validator.isValid("Monday, Tuesday"));
		assertTrue(validator.isValid("REYES MOYANO"));
		assertTrue(validator.isValid("Thompson, Jr."));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void nameLast_DetectTrue() throws IOException, FTAException {
		final LogicalType validator = LogicalTypeFactory.newInstance(pluginLastName, new AnalysisConfig(Locale.forLanguageTag("es")));

		assertTrue(validator.isValid("TATE Jr.", true, 0));
		assertTrue(validator.isValid("TATE", true, 0));
		assertTrue(validator.isValid("TATE, Jr", true, 0));
		assertFalse(validator.isValid("TATE, BRIAN E", true, 0));
		assertTrue(validator.isValid("Davis Brown", true, 0));
		assertTrue(validator.isValid("Davis-Brown", true, 0));
		assertFalse(validator.isValid("Monday, Tuesday", true, 0));
		assertTrue(validator.isValid("REYES MOYANO", true, 0));
		assertTrue(validator.isValid("Thompson, Jr.", true, 0));
		assertFalse(validator.isValid("SAN ANDRES DE TUMACO", true, 0));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void nameLastFirst() throws IOException, FTAException {
		final LogicalType validator = LogicalTypeFactory.newInstance(pluginLastFirstName, new AnalysisConfig());

		assertTrue(validator.isValid("KING, LINDA        "));
		assertTrue(validator.isValid("DAY-LEWIS, DANIEL"));
	}

}
