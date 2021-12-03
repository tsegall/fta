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

/**
 * AnyInputToken captures the wild card token (.+) and will match anything.
 */
public class AnyInputToken extends Token {
	AnyInputToken() {
		super(Token.Type.ANY_INPUT);
	}

	@Override
	AnyInputToken newInstance() {
		return new AnyInputToken();
	}

	@Override
	Token merge(final Token o) {
		return this;
	}

	@Override
	int charactersUsed() {
		return -1;
	}

	@Override
	String getCharacters() {
		return null;
	}
}
