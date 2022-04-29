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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import java.util.List;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAMergeException;
import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import com.cobber.fta.dates.DateTimeParserResult;

public class TestMerge {
	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void changeMind() {
		final DateTimeParser dtp = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None);

		for (int i = 0; i < 100; i++)
			dtp.train("10/20/2008");
		DateTimeParserResult result = dtp.getResult();
		assertEquals(result.getFormatString(), "MM/dd/yyyy");

		for (int i = 0; i < 500; i++)
			dtp.train("21/3/2009");
		result = dtp.getResult();
		assertEquals(result.getFormatString(), "dd/M/yyyy");

		for (int i = 0; i < 1000; i++)
			dtp.train("3/30/2010");
		result = dtp.getResult();
		assertEquals(result.getFormatString(), "M/dd/yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void changeMindSharded() throws FTAMergeException {
		final DateTimeParser shardOne = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None);
		for (int i = 0; i < 100; i++)
			shardOne.train("10/20/2008");
		DateTimeParserResult shardOneResult = shardOne.getResult();
		assertEquals(shardOneResult.getFormatString(), "MM/dd/yyyy");

		final DateTimeParser shardTwo = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None);
		for (int i = 0; i < 500; i++)
			shardTwo.train("21/3/2009");
		DateTimeParserResult shardTwoResult = shardTwo.getResult();
		assertEquals(shardTwoResult.getFormatString(), "dd/M/yyyy");

		final DateTimeParser shardThree = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None);
		for (int i = 0; i < 1000; i++)
			shardThree.train("3/30/2010");
		DateTimeParserResult shardThreeResult = shardThree.getResult();
		assertEquals(shardThreeResult.getFormatString(), "M/dd/yyyy");

		shardOne.merge(shardTwo);
		shardOneResult = shardOne.getResult();
		assertEquals(shardOneResult.getFormatString(), "dd/M/yyyy");

		shardOne.merge(shardThree);
		shardOneResult = shardOne.getResult();
		assertEquals(shardOneResult.getFormatString(), "M/dd/yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void changeMindShardedHydrated() throws FTAMergeException {
		final DateTimeParser shardOne = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None);
		for (int i = 0; i < 100; i++)
			shardOne.train("10/20/2008");
		DateTimeParserResult shardOneResult = shardOne.getResult();
		assertEquals(shardOneResult.getFormatString(), "MM/dd/yyyy");
		final DateTimeParser shardOneHydrated = DateTimeParser.deserialize(shardOne.serialize());

		final DateTimeParser shardTwo = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None);
		for (int i = 0; i < 500; i++)
			shardTwo.train("21/3/2009");
		DateTimeParserResult shardTwoResult = shardTwo.getResult();
		assertEquals(shardTwoResult.getFormatString(), "dd/M/yyyy");
		final DateTimeParser shardTwoHydrated = DateTimeParser.deserialize(shardTwo.serialize());

		final DateTimeParser shardThree = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None);
		for (int i = 0; i < 1000; i++)
			shardThree.train("3/30/2010");
		DateTimeParserResult shardThreeResult = shardThree.getResult();
		assertEquals(shardThreeResult.getFormatString(), "M/dd/yyyy");
		final DateTimeParser shardThreeHydrated = DateTimeParser.deserialize(shardThree.serialize());

		shardOneHydrated.merge(shardTwoHydrated);
		shardOneResult = shardOneHydrated.getResult();
		assertEquals(shardOneResult.getFormatString(), "dd/M/yyyy");

		shardOneHydrated.merge(shardThreeHydrated);
		shardOneResult = shardOneHydrated.getResult();
		assertEquals(shardOneResult.getFormatString(), "M/dd/yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void checkShardedCount() throws FTAMergeException {
		final DateTimeParser shardOne = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None);
		shardOne.train("10/20/2008");
		shardOne.train(null);
		shardOne.train(" ");
		DateTimeParserResult shardOneResult = shardOne.getResult();
		assertEquals(shardOneResult.getFormatString(), "MM/dd/yyyy");

		final DateTimeParser shardTwo = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None);
		for (int i = 0; i < 2; i++) {
			shardTwo.train("21/3/2009");
			shardTwo.train(null);
			shardTwo.train("  ");
		}
		DateTimeParserResult shardTwoResult = shardTwo.getResult();
		assertEquals(shardTwoResult.getFormatString(), "dd/M/yyyy");

		final DateTimeParser shardThree = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None);
		for (int i = 0; i < 3; i++) {
			shardThree.train("3/30/2010");
			shardThree.train(null);
			shardThree.train("   ");
		}
		DateTimeParserResult shardThreeResult = shardThree.getResult();
		assertEquals(shardThreeResult.getFormatString(), "M/dd/yyyy");

		shardOne.merge(shardTwo);
		shardOneResult = shardOne.getResult();
		assertEquals(shardOneResult.getFormatString(), "dd/M/yyyy");

		shardOne.merge(shardThree);
		shardOneResult = shardOne.getResult();
		assertEquals(shardOneResult.getFormatString(), "M/dd/yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void mergeMyself() {
		final DateTimeParser shardOne = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None);
		for (int i = 0; i < 100; i++)
			shardOne.train("10/20/2008");
		DateTimeParserResult shardOneResult = shardOne.getResult();
		assertEquals(shardOneResult.getFormatString(), "MM/dd/yyyy");

		try {
			shardOne.merge(shardOne);
		}
		catch (FTAMergeException e) {
			// All good
			return;
		}
		fail("Should have thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void overlapping() throws FTAMergeException {
		final DateTimeParser shardOne = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None);
		for (int i = 0; i < 100; i++)
			shardOne.train("10/20/2008");
		DateTimeParserResult shardOneResult = shardOne.getResult();
		assertEquals(shardOneResult.getFormatString(), "MM/dd/yyyy");

		final DateTimeParser shardTwo = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None);
		for (int i = 0; i < 500; i++)
			shardTwo.train("21/3/2009");
		DateTimeParserResult shardTwoResult = shardTwo.getResult();
		assertEquals(shardTwoResult.getFormatString(), "dd/M/yyyy");

		final DateTimeParser shardThree = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None);
		for (int i = 0; i < 1000; i++)
			shardThree.train("11/30/2010");
		DateTimeParserResult shardThreeResult = shardThree.getResult();
		assertEquals(shardThreeResult.getFormatString(), "MM/dd/yyyy");

		shardOne.merge(shardTwo);
		shardOneResult = shardOne.getResult();
		assertEquals(shardOneResult.getFormatString(), "dd/M/yyyy");

		shardOne.merge(shardThree);
		shardOneResult = shardOne.getResult();
		assertEquals(shardOneResult.getFormatString(), "MM/dd/yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void differentLocales() throws FTAMergeException {
		final DateTimeParser shardOne = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None).withLocale(Locale.ENGLISH);
		for (int i = 0; i < 100; i++)
			shardOne.train("10/20/2008");
		DateTimeParserResult shardOneResult = shardOne.getResult();
		assertEquals(shardOneResult.getFormatString(), "MM/dd/yyyy");

		final DateTimeParser shardTwo = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None).withLocale(Locale.GERMAN);
		for (int i = 0; i < 500; i++)
			shardTwo.train("21/3/2009");
		DateTimeParserResult shardTwoResult = shardTwo.getResult();
		assertEquals(shardTwoResult.getFormatString(), "dd/M/yyyy");

		try {
			shardOne.merge(shardOne);
		}
		catch (FTAMergeException e) {
			// All good
			return;
		}
		fail("Should have thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void simpleStrict() throws FTAMergeException {
		List<String> list = List.of("21/01/2022", "test string 1", "test string 2");
		DateTimeParser dateTimeParser = new DateTimeParser().withDateResolutionMode(DateTimeParser.DateResolutionMode.Auto).withStrictMode(true);
		list.forEach(dateTimeParser::train);
		assertNull(dateTimeParser.getResult());
	}
}
