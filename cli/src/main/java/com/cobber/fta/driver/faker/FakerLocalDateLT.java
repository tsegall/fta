/*
 * Copyright 2017-2024 Tim Segall
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
package com.cobber.fta.driver.faker;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.cobber.fta.PluginDefinition;

public class FakerLocalDateLT extends FakerLT {
	private boolean initialized = false;
	private LocalDate low;
	private LocalDate high;
	private DateTimeFormatter dtf;
	private long range;

	public FakerLocalDateLT(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		if (!initialized) {
			if (parameters != null) {
				if (locale == null)
					locale = Locale.getDefault();
                    dtf = DateTimeFormatter.ofPattern(parameters.format == null ? "yyyy-MM-dd" : parameters.format, locale);
	            if (parameters.low != null)
                    low = LocalDate.parse(parameters.low);
	            if (parameters.high != null)
                    high = LocalDate.parse(parameters.high);
			}

			if (high == null)
				high = LocalDate.of(2035, 1, 1);
			if (low == null)
				low = LocalDate.of(1960, 1, 1);

			range =  high.toEpochDay() - low.toEpochDay();

			initialized = true;
		}

		final long offset = getRandom().nextInt((int)range);
		final LocalDate newDateTime = low.plusDays(offset);

        return dtf.format(newDateTime);
	}
}
