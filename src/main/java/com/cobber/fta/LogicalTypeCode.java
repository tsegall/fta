package com.cobber.fta;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

public abstract class LogicalTypeCode extends LogicalType {

	LogicalTypeCode(PluginDefinition plugin) {
		super(plugin);
	}

	public abstract String nextRandom();

	public static LogicalTypeCode newInstance(PluginDefinition plugin, Locale locale) {
		Class<?> newLogicalType;
		Constructor<?> ctor;
		LogicalTypeCode logical;

		try {
			newLogicalType = Class.forName(plugin.clazz);
			ctor = newLogicalType.getConstructor(PluginDefinition.class);
			logical = (LogicalTypeCode)ctor.newInstance(plugin);

			if (!(logical instanceof LogicalType))
				throw new IllegalArgumentException("Logical type: " + plugin.clazz + " does not appear to be a Logical Type.");

			logical.initialize(locale);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			return null;
		}
		return logical;
	}
}
