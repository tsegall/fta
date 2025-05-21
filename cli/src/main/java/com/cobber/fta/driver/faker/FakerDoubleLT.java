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
package com.cobber.fta.driver.faker;

import com.cobber.fta.PluginDefinition;

public class FakerDoubleLT extends FakerLT {
	private boolean initialized = false;
	private Double low;
	private Double high;
	private String format = "%f";
	private String distribution = "random";
	private double range;
	private double last = Double.MIN_VALUE;

	public FakerDoubleLT(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		if (!initialized) {
			if (parameters != null) {
	            if (parameters.format != null)
                    format = parameters.format;
	            if (parameters.low != null)
                    low = Double.parseDouble(parameters.low);
	            if (parameters.high != null)
                    high = Double.parseDouble(parameters.high);
	            if (parameters.distribution != null)
	            	distribution = parameters.distribution;
			}

			if (high == null)
				high = Double.MAX_VALUE;
			if (low == null)
				low = 0.0;

			range = high - low;
			initialized = true;
		}

		double d = 0.0;
		if ("random".equals(distribution)) {
			d = getRandom().nextDouble();
			d *= range;
			if (low != null)
				d += low;
		} else if ("monotonic_increasing".equals(distribution)) {
			if (last == Double.MIN_VALUE)
				last = low;
			else {
				if (++last > high)
					last = low;
			}
			d = last;

		} else if ("monotonic_decreasing".equals(distribution)) {
			if (last == Long.MIN_VALUE) {
				last = high;
			}
			else {
				if (--last < low)
					last = high;
			}
			d = last;
		} else if ("gaussian".equals(distribution)) {
			d = getRandom().nextGaussian();
			d += 5.0;				// Capture 5 SD's (basically convert [-5.0,5.0] to [0.0,10.0]
			d = (d * range)/10;		// Scale up from [0.0,10.0] to the range
			d += low;				// Shift origin to the low bound
		}

		return String.format(format, d);
	}
}
