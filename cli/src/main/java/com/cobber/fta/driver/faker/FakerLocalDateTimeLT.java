/*
 * Copyright 2017-2023 Tim Segall
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.cobber.fta.PluginDefinition;

public class FakerLocalDateTimeLT extends FakerLT {
	private boolean initialized = false;
	private LocalDateTime low;
	private LocalDateTime high;
	private DateTimeFormatter dtf;
	private long range;

	public FakerLocalDateTimeLT(final PluginDefinition plugin) {
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
                    low = LocalDateTime.parse(parameters.low, dtf);
	            if (parameters.high != null)
                    high = LocalDateTime.parse(parameters.high, dtf);
			}

			if (high == null)
				high = LocalDateTime.of(2035, 1, 1, 1, 0, 0);
			if (low == null)
				low = LocalDateTime.of(1960, 1, 1, 1, 0, 0);

			range =  high.toEpochSecond(ZoneOffset.UTC) - low.toEpochSecond(ZoneOffset.UTC);

			initialized = true;
		}

		final long offset = (long)(Math.abs(getRandom().nextDouble() * range));
		final LocalDateTime newDateTime = low.plusSeconds(offset);

        return dtf.format(newDateTime);
	}
}
