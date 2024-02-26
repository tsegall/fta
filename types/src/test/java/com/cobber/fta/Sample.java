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
package com.cobber.fta;

public class Sample {
	private String sample;
	private boolean valid;

	Sample(final String sample) {
		this.sample = sample;
		this.valid = true;
	}

	public String getSample() {
		return sample;
	}

	public boolean isValid() {
		return valid;
	}

	public static Sample[] allValid(final String[] samples) {
		final Sample[] ret = new Sample[samples.length];

		for (int i = 0; i < samples.length; i++)
			ret[i] = new Sample(samples[i]);

		return ret;
	}

	public static Sample[] setInvalid(final Sample[] samples, final int... indices) {
		for (final int index : indices)
			samples[index].valid = false;

		return samples;
	}
}