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

import com.cobber.fta.PluginDefinition;

public class FakerEnumLT extends FakerLT {
	private boolean initialized = false;
	private String distribution = "random";
	private String[] options;
	private int last = -1;

	public FakerEnumLT(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		if (!initialized) {
			if (parameters != null) {
	            if (parameters.values != null)
                    options = parameters.values.split("\\^");
	            if (parameters.distribution != null)
	            	distribution = parameters.distribution;
			}

			initialized = true;
		}

		String ret = null;
		if (distribution.equals("random")) {
			ret = options[random.nextInt(options.length)];
		} else if (distribution.equals("monotonic_increasing")) {
			if (last == -1)
				last = 0;
			else {
				if (++last > options.length)
					last = 0;
			}
			ret = options[last];
		} else if (distribution.equals("monotonic_decreasing")) {
			if (last == -1) {
				last = options.length - 1;
			}
			else {
				if (--last < 0)
					last = options.length - 1;
			}
			ret = options[last];
		}

		return ret;
	}
}
