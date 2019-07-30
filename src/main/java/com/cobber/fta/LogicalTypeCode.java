package com.cobber.fta;

import java.util.Locale;
import java.util.Random;

public abstract class LogicalTypeCode extends LogicalType implements LTRandom {
	protected Random random = null;

	LogicalTypeCode(PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(Locale locale) {
		super.initialize(locale);

		random = new Random(3141592);

		return true;
	}

	@Override
	public void seed(long seed) {
		random = new Random(seed);
	}
}
