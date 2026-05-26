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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;

public class TestLogicalTypeRegExp {

	private LogicalTypeRegExp build(final FTAType baseType, final String regex,
			final String minimum, final String maximum, final String... invalid) throws FTAPluginException {
		final PluginLocaleEntry localeEntry = new PluginLocaleEntry("*", null, 0, regex);
		final PluginDefinition defn = new PluginDefinition(
				"TEST." + baseType, "Test", invalid.length == 0 ? null : invalid,
				null, null, new PluginLocaleEntry[] { localeEntry }, false, 95, baseType);
		defn.minimum = minimum;
		defn.maximum = maximum;
		final LogicalTypeRegExp lt = new LogicalTypeRegExp(defn);
		lt.initialize(new AnalysisConfig());
		return lt;
	}

	// ---- LONG bounds ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void longBelowMin() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.LONG, "\\d+", "10", "100");
		assertFalse(lt.isValid("5", true, 1));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void longInRange() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.LONG, "\\d+", "10", "100");
		assertTrue(lt.isValid("50", true, 1));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void longAboveMax() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.LONG, "\\d+", "10", "100");
		assertFalse(lt.isValid("150", true, 1));
	}

	// ---- DOUBLE bounds ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void doubleBelowMin() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.DOUBLE, "\\d+\\.?\\d*", "1.0", "10.0");
		assertFalse(lt.isValid("0.5", true, 1));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void doubleInRange() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.DOUBLE, "\\d+\\.?\\d*", "1.0", "10.0");
		assertTrue(lt.isValid("5.0", true, 1));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void doubleAboveMax() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.DOUBLE, "\\d+\\.?\\d*", "1.0", "10.0");
		assertFalse(lt.isValid("15.0", true, 1));
	}

	// ---- STRING bounds ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void stringBelowMin() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.STRING, "\\p{Alpha}+", "BETA", "ZETA");
		assertFalse(lt.isValid("ALPHA", true, 1));   // "ALPHA" < "BETA"
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void stringInRange() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.STRING, "\\p{Alpha}+", "BETA", "ZETA");
		assertTrue(lt.isValid("GAMMA", true, 1));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void stringAboveMax() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.STRING, "\\p{Alpha}+", "BETA", "ZETA");
		assertFalse(lt.isValid("ZZZZZ", true, 1));
	}

	// ---- invalidList ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void invalidListBlocks() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.STRING, "\\p{Alpha}+", null, null, "SKIP", "IGNORE");
		assertFalse(lt.isValid("SKIP", true, 1));
		assertFalse(lt.isValid("IGNORE", true, 1));
		assertTrue(lt.isValid("HELLO", true, 1));
	}

	// ---- isMatch ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void isMatchNull() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.STRING, "\\d{5}", null, null);
		assertFalse(lt.isMatch(null));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void isMatchFound() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.STRING, "\\d{5}", null, null);
		assertTrue(lt.isMatch("\\d{5}"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void isMatchNotFound() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.STRING, "\\d{5}", null, null);
		assertFalse(lt.isMatch("\\d{9}"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void isMatchAfterSet() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.STRING, "\\d{5}", null, null);
		assertTrue(lt.isMatch("\\d{5}"));   // sets matchEntry as side-effect
		assertTrue(lt.isMatch("\\d{5}"));   // second call uses the already-set matchEntry
		assertFalse(lt.isMatch("\\d{9}"));  // non-matching still returns false
	}

	// ---- nextRandom with xeger-incompatible regex ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void nextRandomXegerIncompatible() throws FTAPluginException {
		// Negative lookahead makes this incompatible with Xeger — nextRandom() must return null
		final LogicalTypeRegExp lt = build(FTAType.STRING, "\\p{Alpha}+(?!foo)", null, null);
		assertNull(lt.nextRandom());
		// Second call stays null (xegerCompatible remains false)
		assertNull(lt.nextRandom());
	}

	// ---- simple getters ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getMatchEntriesNonNull() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.STRING, "\\d{5}", null, null);
		final PluginMatchEntry[] entries = lt.getMatchEntries();
		assertNotNull(entries);
		assertEquals(entries.length, 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getMinSamplesDefault() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.STRING, "\\d{5}", null, null);
		assertEquals(lt.getMinSamples(), -1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void isMinMaxPresentFalse() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.STRING, "\\d{5}", null, null);
		assertFalse(lt.isMinMaxPresent());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void isClosedAlwaysFalse() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.STRING, "\\d{5}", null, null);
		assertFalse(lt.isClosed());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getBaseType() throws FTAPluginException {
		assertEquals(build(FTAType.LONG,   "\\d+",          null, null).getBaseType(), FTAType.LONG);
		assertEquals(build(FTAType.DOUBLE, "\\d+\\.?\\d*",  null, null).getBaseType(), FTAType.DOUBLE);
		assertEquals(build(FTAType.STRING, "\\p{Alpha}+",   null, null).getBaseType(), FTAType.STRING);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void setGetMatchEntry() throws FTAPluginException {
		final LogicalTypeRegExp lt = build(FTAType.STRING, "\\d{5}", null, null);
		assertNull(lt.getMatchEntry());
		final PluginMatchEntry entry = lt.getMatchEntries()[0];
		lt.setMatchEntry(entry);
		assertSame(lt.getMatchEntry(), entry);
		// getRegExp and isRegExpComplete route through matchEntry after it is set
		assertNotNull(lt.getRegExp());
		assertFalse(lt.isRegExpComplete());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void seedChangesRandomOutput() throws FTAPluginException {
		// seed() is accepted without error; nextRandom() must still produce a value
		final LogicalTypeRegExp lt = build(FTAType.STRING, "\\d{5}", null, null);
		lt.seed(new byte[]{7, 6, 5, 4, 3, 2, 1});
		final String r = lt.nextRandom();
		assertNotNull(r);
		assertTrue(r.matches("\\d{5}"));
	}
}
