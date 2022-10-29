package com.cobber.fta;

import java.util.ArrayList;
import java.util.List;

public class Rules {
	private class Rule {
		private String name;
		private String[] arguments;

		Rule(final String name, final String ...arguments) {
			this.name = name;
			this.arguments = arguments;
		}

		@Override
		public String toString() {
			final StringBuilder ret = new StringBuilder();
			ret.append(name);

			ret.append("(");

			for (int i = 0; i < arguments.length; i++) {
				if (i != 0)
					ret.append(", ");
				ret.append("'");
				ret.append(arguments[i]);
				ret.append("'");
			}

			ret.append(")");

			return ret.toString();
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

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();

		if (rules.size() > 1)
			b.append("[ ");

		for (int i = 0; i < rules.size(); i++) {
			if (i != 0)
				b.append(", ");
			b.append(rules.get(i).toString());
		}

		if (rules.size() > 1)
			b.append(" ]");

		return b.toString();
	}
}
