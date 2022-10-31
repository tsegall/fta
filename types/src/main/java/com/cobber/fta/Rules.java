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
package com.cobber.fta;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Rules {
	private final static ObjectMapper MAPPER = new ObjectMapper();

	private class Rule {
		private String name;
		private String[] arguments;


		Rule(final String name, final String ...arguments) {
			this.name = name;
			this.arguments = arguments;
		}

		private void outputArray(final ArrayNode detail, final String[] array) {
			for (final String s : array)
				detail.add(s);
		}

		public ObjectNode asJSON() {
			final ObjectNode rule = MAPPER.createObjectNode();
			rule.put("name", name);

			if (arguments.length > 0)
				outputArray(rule.putArray("arguments"), arguments);

			return rule;
		}
	}

	private List<Rule> rules;

	public Rules() {
		rules = new ArrayList<>();
	}

	public void add(final String name, final String ...arguments) {
		rules.add(new Rule(name, arguments));
	}

	public void add(final String name) {
		rules.add(new Rule(name));
	}

	public boolean nonEmpty() {
		return rules.size() != 0;
	}

	public ArrayNode asJSON() {
		ObjectMapper objectMapper = new ObjectMapper();

		ArrayNode ruleArray =  objectMapper.createArrayNode();

		for (final Rule rule : rules)
			ruleArray.add(rule.asJSON());

		return ruleArray;
	}
}
