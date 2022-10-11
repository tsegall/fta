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
package com.cobber.fta.driver.faker;

import com.cobber.fta.PluginDefinition;

public class FakerLongLT extends FakerLT {
	private boolean initialized = false;
	private Long low;
	private Long high;
	private String format = "%d";
	private String distribution = "random";
	private long range;
	private long last = Long.MIN_VALUE;

	public FakerLongLT(PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		if (!initialized) {
			if (parameters != null) {
	            if (parameters.format != null)
                    format = parameters.format;
	            if (parameters.low != null)
                    low = Long.parseLong(parameters.low);
	            if (parameters.high != null)
                    high = Long.parseLong(parameters.high);
	            if (parameters.high != null)
                    high = Long.parseLong(parameters.high);
	            if (parameters.high != null)
                    high = Long.parseLong(parameters.high);
	            if (parameters.high != null)
                    high = Long.parseLong(parameters.high);
	            if (parameters.distribution != null)
	            	distribution = parameters.distribution;
			}


			if (high == null)
				high = Long.MAX_VALUE;
			if (low == null)
				low = 0L;

            range = high - low;
			initialized = true;
		}

		long l = 0;
		if (distribution.equals("random")) {
			double d = random.nextDouble();
			l = Math.round(d * range);

			if (low != null)
				l += low;
		} else if (distribution.equals("monotonic_increasing")) {
			if (last == Long.MIN_VALUE)
				last = low;
			else {
				if (++last > high)
					last = low;
			}
			l = last;

		} else if (distribution.equals("monotonic_decreasing")) {
			if (last == Long.MIN_VALUE) {
				last = high;
			}
			else {
				if (--last < low)
					last = high;
			}
			l = last;
		} else if (distribution.equals("gaussian")) {
			double d = random.nextGaussian();
			d += 5.0;				// Capture 5 SD's
			d = (d * range)/10;		// Scale up from ~0-10 to the range
			d += low;				// Shift origin to the low bound
			l = Math.round(d);
		}

		return String.format(format, l);
	}
}