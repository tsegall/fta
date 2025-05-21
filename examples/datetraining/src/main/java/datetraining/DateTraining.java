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
package datetraining;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.dates.DateTimeParser;

public abstract class DateTraining {
	public static void main(final String[] args) throws FTAException {

		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.ENGLISH);

		final List<String> inputs = Arrays.asList( "20080112", "20060405", "19700412", "19990723", "20010424", "20060423" );

		inputs.forEach(dtp::train);

		System.err.println(dtp.getResult().getFormatString());
	}
}
