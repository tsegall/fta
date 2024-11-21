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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.cobber.fta.PluginDefinition;

public class FakerLocalTimeLT extends FakerLT {
	private boolean initialized = false;
	private LocalTime low;
	private LocalTime high;
	private DateTimeFormatter dtf;
	private long range;

	public FakerLocalTimeLT(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		if (!initialized) {
			if (parameters != null) {
				if (locale == null)
					locale = Locale.getDefault();
				dtf = DateTimeFormatter.ofPattern(parameters.format == null ? "yyyy-MM-dd HH:mm:ss" : parameters.format, locale);
	            if (parameters.low != null)
                    low = LocalTime.parse(parameters.low, dtf);
	            if (parameters.high != null)
                    high = LocalTime.parse(parameters.high, dtf);
			}

			if (high == null)
				high = LocalTime.of(0, 0, 0, 0);
			if (low == null)
				low = LocalTime.of(23, 59, 59, 0);

			range =  high.toNanoOfDay() - low.toNanoOfDay();

			initialized = true;
		}

		final long offset = (long)(Math.abs(getRandom().nextDouble() * range));
		final LocalTime newDateTime = low.plusSeconds(offset);

        return dtf.format(newDateTime);
	}
}
