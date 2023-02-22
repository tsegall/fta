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

import java.io.IOException;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.text.TextProcessor;

public class TestText {
	@Test(groups = { TestGroups.ALL, TestGroups.TEXT })
	public void reallySimple() throws IOException, FTAException {
		TextProcessor processor = new TextProcessor(Locale.US);

		TextProcessor.TextResult result = processor.analyze("The quick brown fox jumped over the lazy dog");

		assertEquals(result.getDetermination(), TextProcessor.Determination.OK);
		assertEquals(result.getWords(), 9);
		assertEquals(result.getAlphas(), 36);
		assertEquals(result.getSpaces(), 8);
		assertEquals(result.getPunctuation(), 0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TEXT })
	public void example() throws IOException, FTAException {
		TextProcessor processor = new TextProcessor(Locale.US);

		TextProcessor.TextResult result = processor.analyze("I.e. is an abbreviation for the phrase id est, which means \"that is.\" I.e. is used to restate something said previously in order to clarify its meaning. E.g. is short for exempli gratia, which means \"for example.\" E.g. is used before an item or list of items that serve as examples for the previous statement.");

		assertEquals(result.getDetermination(), TextProcessor.Determination.OK);
		assertEquals(result.getWords(), 23);
		assertEquals(result.getAlphas(), 91);
		assertEquals(result.getSpaces(), 21);
		assertEquals(result.getPunctuation(), 8);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TEXT })
	public void offWithTheirHeads() throws IOException, FTAException {
		TextProcessor processor = new TextProcessor(Locale.US);

		TextProcessor.TextResult result = processor.analyze("Off with their heads!");

		assertEquals(result.getDetermination(), TextProcessor.Determination.OK);
		assertEquals(result.getWords(), 4);
		assertEquals(result.getAlphas(), 17);
		assertEquals(result.getSpaces(), 3);
		assertEquals(result.getPunctuation(), 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TEXT })
	public void sixImpossibleThings() throws IOException, FTAException {
		TextProcessor processor = new TextProcessor(Locale.US);

		TextProcessor.TextResult result = processor.analyze("Why, sometimes I've believed as many as six impossible things before breakfast.");

		assertEquals(result.getDetermination(), TextProcessor.Determination.OK);
		assertEquals(result.getWords(), 12);
		assertEquals(result.getAlphas(), 65);
		assertEquals(result.getSpaces(), 11);
		assertEquals(result.getPunctuation(), 3);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TEXT })
	public void goingBackToYesterday() throws IOException, FTAException {
		TextProcessor processor = new TextProcessor(Locale.US);

		TextProcessor.TextResult result = processor.analyze("It's no use going back to yesterday,  because I was a different person then.");

		assertEquals(result.getDetermination(), TextProcessor.Determination.OK);
		assertEquals(result.getWords(), 14);
		assertEquals(result.getAlphas(), 59);
		assertEquals(result.getSpaces(), 14);
		assertEquals(result.getPunctuation(), 3);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TEXT })
	public void dreadfullyUglyChild() throws IOException, FTAException {
		TextProcessor processor = new TextProcessor(Locale.US);

		TextProcessor.TextResult result = processor.analyze("It would have made a dreadfully ugly child; but it makes rather a handsome pig.");

		assertEquals(result.getDetermination(), TextProcessor.Determination.OK);
		assertEquals(result.getWords(), 15);
		assertEquals(result.getAlphas(), 63);
		assertEquals(result.getSpaces(), 14);
		assertEquals(result.getPunctuation(), 2);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TEXT })
	public void quotes() throws IOException, FTAException {
		TextProcessor processor = new TextProcessor(Locale.US);

		TextProcessor.TextResult result = processor.analyze("'And what is the use of a book,' thought Alice, 'without pictures or conversation?'");

		assertEquals(result.getDetermination(), TextProcessor.Determination.OK);
		assertEquals(result.getWords(), 14);
		assertEquals(result.getAlphas(), 63);
		assertEquals(result.getSpaces(), 13);
		assertEquals(result.getPunctuation(), 7);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TEXT })
	public void whoInTheWorldAmI() throws IOException, FTAException {
		TextProcessor processor = new TextProcessor(Locale.US);

		TextProcessor.TextResult result = processor.analyze("'Who in the world am I?' Ah, that's the great puzzle!");

		assertEquals(result.getDetermination(), TextProcessor.Determination.OK);
		assertEquals(result.getWords(), 11);
		assertEquals(result.getAlphas(), 37);
		assertEquals(result.getSpaces(), 10);
		assertEquals(result.getPunctuation(), 6);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TEXT })
	public void singleWord() throws IOException, FTAException {
		TextProcessor processor = new TextProcessor(Locale.US);

		TextProcessor.TextResult result = processor.analyze("F944277490");

		assertEquals(result.getDetermination(), TextProcessor.Determination.SINGLE_WORD);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TEXT })
	public void noRealWords() throws IOException, FTAException {
		TextProcessor processor = new TextProcessor(Locale.US);

		TextProcessor.TextResult result = processor.analyze("F944277490 PAGE X1233456");

		assertEquals(result.getDetermination(), TextProcessor.Determination.PERCENT_TOO_LOW);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TEXT })
	public void tooLong() throws IOException, FTAException {
		TextProcessor processor = new TextProcessor(Locale.US);

		TextProcessor.TextResult result = processor.analyze("Sometimes antidisestablishmentarianism is just too long to be real.");

		assertEquals(result.getDetermination(), TextProcessor.Determination.TOO_LONG);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TEXT })
	public void sentenceTooShort() throws IOException, FTAException {
		TextProcessor processor = new TextProcessor(Locale.getDefault());

		TextProcessor.TextResult result = processor.analyze("Stop!");

		assertEquals(result.getDetermination(), TextProcessor.Determination.SENTENCE_TOO_SHORT);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TEXT })
	public void averageLow() throws IOException, FTAException {
		TextProcessor processor = new TextProcessor(Locale.US);

		TextProcessor.TextResult result = processor.analyze("I am a very tiny tract of land.");

		assertEquals(result.getDetermination(), TextProcessor.Determination.BAD_AVERAGE_LENGTH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TEXT })
	public void averageHigh() throws IOException, FTAException {
		TextProcessor processor = new TextProcessor(Locale.US);

		TextProcessor.TextResult result = processor.analyze("Tergiversation - definition: equivocation, circumlocution, prevarication.");

		assertEquals(result.getDetermination(), TextProcessor.Determination.BAD_AVERAGE_LENGTH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TEXT })
	public void randomNoise() throws IOException, FTAException {
		TextProcessor processor = new TextProcessor(Locale.US);

		TextProcessor.TextResult result = processor.analyze("=aaaaaaa= =bbbbbbbb= =ccccccc= =dddddddd= =eeeeeeee=.");

		assertEquals(result.getDetermination(), TextProcessor.Determination.NOT_ENOUGH_REAL_WORDS);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TEXT })
	public void hyphens() throws IOException, FTAException {
		TextProcessor processor = new TextProcessor(Locale.US);

		TextProcessor.TextResult result = processor.analyze("Netex - LocalTails-Unmatched");

		assertEquals(result.getDetermination(), TextProcessor.Determination.OK);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TEXT })
	public void addresses() throws IOException, FTAException {
		TextProcessor processor = new TextProcessor(Locale.US);

		TextProcessor.TextResult result = processor.analyze("Station Road");

		assertEquals(result.getDetermination(), TextProcessor.Determination.OK);
	}
}
