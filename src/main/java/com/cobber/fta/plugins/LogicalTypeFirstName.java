package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.PluginDefinition;

public class LogicalTypeFirstName extends LogicalTypeFiniteSimple {
	private class Dodge {
		Iterator<String> iter;
	}

	public final static String SEMANTIC_TYPE = "FIRST_NAME";
	public final static String REGEXP = "[- \\p{IsAlphabetic}]*";
	private static Set<String> members = new HashSet<String>();
	private final int ITERS = 3;
	private Dodge[] iterators = null;

	public LogicalTypeFirstName(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin.qualifier, plugin.hotWords, plugin.regExp != null ? plugin.regExp : REGEXP, ".*",
				new InputStreamReader(LogicalTypeCAProvince.class.getResourceAsStream("/reference/firstnames.txt")),
				95);
	}

	@Override
	public String nextRandom() {
		if (iterators == null) {
			iterators = new Dodge[ITERS];
			for (int i = 0; i < iterators.length; i++) {
				iterators[i] = new Dodge();
				iterators[i].iter = members.iterator();
				int offset = random.nextInt(members.size() / 2);
				for (int j = 0; j < offset; j++)
					iterators[i].iter.next();
			}
		}
		Dodge any = iterators[random.nextInt(ITERS)];
		if (!any.iter.hasNext())
			any.iter = members.iterator();
		return any.iter.next();
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}

	@Override
	public String[] getMemberArray() {
		return null;
	}
}
