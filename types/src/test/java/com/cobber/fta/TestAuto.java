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

import java.io.IOException;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;

public class TestAuto {
	@Test(groups = { TestGroups.ALL })
	public void testCityDetection() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("city");
		analysis.setLocale(Locale.forLanguageTag("en-US"));

		final String[] inputs = {
			"Bronx", "Bronx", "Bronx", "Bronx", "Bronx", "Bronx",
			"Brooklyn", "Brooklyn", "Brooklyn", "Brooklyn", "Brooklyn", "Brooklyn", "Brooklyn",
			"New York", "New York", "New York", "New York", "New York", "New York", "New York", "New York", "New York",
			"Queens", "Queens", "Queens", "Queens", "Queens", "Queens", "Queens",
			"Staten Island"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), 30);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSemanticType(), "CITY");
	}

	@Test(groups = { TestGroups.ALL })
	public void testFirstName() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("first_name");
		analysis.setLocale(Locale.forLanguageTag("en-US"));

		final String[] inputs = {
			"AMY MARIE", "BRYAN LEE", "MELVIN LEE", "RILEY JAMES", "CHARLES H",
			"DANNY CHARLES", "JERROD ORVAL", "KAYLA ANN", "NANCY LYNN", "PATRICIA ANN",
			"SHAWNA JOE", "BARBARA JEAN", "DALE LOREN", "M CHRIS", "MARY CHRISTINE",
			"ASHLEE NICHOLE", "DARYL SCOTT", "JAMES RICHARD", "CHARLES DALE", "ANNE MARIE",
			"TAMARA LYNN", "JANE ANN", "KELLY ANNE", "PATSY ANNE", "ERICA JANE",
			"AMANDA JO", "BRIAN LEE", "DERRICK JOHN", "ELIZABETH", "EMMA JANE",
			"JOHN RAYMOND", "LINDA SUE", "DYLAN JAMES", "BRAD ALDEN", "EVELYN LEE",
			"ALTON FREDERICK", "JUDY KAY", "SILVIA", "CONNIE FAYE", "RYAN DEVLIN",
			"BEVERLY J", "MICHAEL SHAWN", "DOTTY DIANNE", "STEVEN LEE", "STORMY",
			"BRENDA ROSE", "AMBERLY JO", "BLAKE ALAN", "MATTHEW DAVID", "DONNA MARIE",
			"JAMES DEAN", "DENISE LENAE", "KATY VERONICA", "ANTHONY RAY", "CHELSEY LYNN",
			"GLENN LEE", "JANET LOUISE", "JEROMY WAYNE", "LISA KAY", "MELISSA SUE",
			"SHARON LEWIS", "ROSA MARIA", "BRANDON TYLER", "NATHAN PATRICK", "VICTORIA JOAN",
			"MARIE-ANDRE IJEOMA", "JENNIFER ANN", "ALEX JOHN", "DAISY DEANNA", "DIANE KAY",
			"GARY DEAN", "HANNAH LEE", "MORGAN ANNE", "NANCY MARIE", "RICHARD ALLEN",
			"RYAN DEAN", "LA DONNA", "TERESA MARIE", "VERLENE M", "WILLIAM LEWIS",
			"DUANE RUSSELL", "LINDA LOU", "MATTHEW EARL", "ALLEEN SUE", "CHRISTOPHER D",
			"CYNTHIA KAY", "CYNTHIA SUE", "DARION LEE", "JAMES MICHEAL", "LEO ARTHUR",
			"LOIS JOANN", "LORI LORINE", "LORI SUE", "PATRICK ALAN", "PAYTON LEA",
			"PHYLLIS R", "ROSEMARY MARGARET", "RUSSELL ALLAN", "SCOTT HALL", "SCOTT LYNN"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSampleCount(), 100);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSemanticType(), "NAME.FIRST");
	}
}
