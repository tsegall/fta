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
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;

public class TestExternalFacts {

	private Facts buildFacts() throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("numbers");
		for (int i = 1; i <= 30; i++)
			analysis.train(String.valueOf(i));
		return analysis.getResult().getFacts();
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void copyConstructorCopiesAllFields() throws FTAException {
		final Facts facts = buildFacts();
		facts.external.setTotalCount(1000L);
		facts.external.setTotalNullCount(5L);
		facts.external.setTotalBlankCount(3L);
		facts.external.setTotalInvalidCount(2L);
		facts.external.setTotalMatchCount(990L);
		facts.external.setTotalMean(15.5);
		facts.external.setTotalStandardDeviation(8.2);
		facts.external.setTotalMinValue("1");
		facts.external.setTotalMaxValue("1000");
		facts.external.setTotalMinLength(1);
		facts.external.setTotalMaxLength(4);
		facts.external.setKeyConfidence(0.99);
		facts.external.setUniqueness(0.95);

		final Facts.ExternalFacts copy = facts.new ExternalFacts(facts.external);

		assertEquals(copy.getTotalCount(), 1000L);
		assertEquals(copy.getTotalNullCount(), 5L);
		assertEquals(copy.getTotalBlankCount(), 3L);
		assertEquals(copy.getTotalInvalidCount(), 2L);
		assertEquals(copy.getTotalMatchCount(), 990L);
		assertEquals(copy.getTotalMean(), 15.5);
		assertEquals(copy.getTotalStandardDeviation(), 8.2);
		assertEquals(copy.getTotalMinValue(), "1");
		assertEquals(copy.getTotalMaxValue(), "1000");
		assertEquals(copy.getTotalMinLength(), 1);
		assertEquals(copy.getTotalMaxLength(), 4);
		assertEquals(copy.getKeyConfidence(), 0.99);
		assertEquals(copy.getUniqueness(), 0.95);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void equalsIdentity() throws FTAException {
		final Facts facts = buildFacts();
		assertTrue(facts.external.equals(facts.external));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void equalsNull() throws FTAException {
		assertFalse(buildFacts().external.equals(null));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void equalsWrongClass() throws FTAException {
		assertFalse(buildFacts().external.equals("not an ExternalFacts"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void equalsCopiedIsEqual() throws FTAException {
		final Facts facts = buildFacts();
		facts.external.setTotalCount(500L);
		final Facts.ExternalFacts copy = facts.new ExternalFacts(facts.external);
		assertTrue(facts.external.equals(copy));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void equalsAfterMutationNotEqual() throws FTAException {
		final Facts facts = buildFacts();
		facts.external.setTotalCount(500L);
		final Facts.ExternalFacts copy = facts.new ExternalFacts(facts.external);
		facts.external.setTotalCount(999L);
		assertFalse(facts.external.equals(copy));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void defaultConstructorHasSentinelValues() throws FTAException {
		final Facts facts = buildFacts();
		final Facts.ExternalFacts ef = facts.new ExternalFacts();
		assertNotNull(ef);
		assertEquals(ef.getTotalCount(), -1L);
		assertEquals(ef.getTotalNullCount(), -1L);
	}
}
