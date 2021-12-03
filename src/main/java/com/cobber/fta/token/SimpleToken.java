/*
 * Copyright 2017-2021 Tim Segall
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
package com.cobber.fta.token;

import com.cobber.fta.core.RegExpGenerator;

/**
 * SimpleToken captures a single character (not digits or alphas).
 */
public class SimpleToken extends Token {
	SimpleToken(char ch) {
		super(Token.Type.SIMPLE, ch);
	}

	@Override
	SimpleToken newInstance() {
		return new SimpleToken(ch);
	}

	@Override
	Token merge(final Token o) {
		return this;
	}

	@Override
	int charactersUsed() {
		return 1;
	}

	@Override
	String getCharacters() {
		return java.lang.String.valueOf(ch);
	}

	@Override
	String getRegExp(final boolean fitted) {
		return RegExpGenerator.slosh(ch);
	}
}
