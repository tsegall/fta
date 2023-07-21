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
package com.cobber.fta.dates;

class TimeDateElement {
	enum TimeDateElementType {
		Time,
		Date,
		AMPM,
		English_AMPM,
		WhiteSpace,
		TimeZone,
		Indicator_8601,
		Constant
	}

	final static TimeDateElement Time = new TimeDateElement(TimeDateElementType.Time);
	final static TimeDateElement Date = new TimeDateElement(TimeDateElementType.Date);
	final static TimeDateElement TimeZone = new TimeDateElement(TimeDateElementType.TimeZone);

	final static TimeDateElement AMPM = new TimeDateElement(TimeDateElementType.AMPM, "a");
	final static TimeDateElement English_AMPM = new TimeDateElement(TimeDateElementType.English_AMPM, "P");
	final static TimeDateElement WhiteSpace = new TimeDateElement(TimeDateElementType.WhiteSpace, " ");
	final static TimeDateElement Indicator_8601 = new TimeDateElement(TimeDateElementType.Indicator_8601, "'T'");

	public final TimeDateElementType type;
	public final String rep;

	TimeDateElement(final TimeDateElementType type) {
		this.type = type;
		rep = null;
	}

	TimeDateElement(final TimeDateElementType type, final String rep) {
		this.type = type;
		this.rep = rep;
	}

	public TimeDateElementType getType() {
		return type;
	}

	public String getRepresentation() {
		return rep;
	}
}