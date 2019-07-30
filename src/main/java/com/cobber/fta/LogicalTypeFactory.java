package com.cobber.fta;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

/**
 * Construct a LogicalType from PluginDefinition or a Semantic Type name.
 */
public class LogicalTypeFactory {
	/**
	 * Return a Logical Type based simply on the name of the Logical Type.  The locale will be derived from the first valid locale in
	 * the internal plugins file.
	 *
	 * @param qualifier Name of the Semantic Type for which we wish to retrieve the Logical Type.
	 * @return The LogicalType The Logical Type associated with the definition (if it exists), null if non-existent.
	 *
	 *  Note: isValid(input) can be invoked on the resulting type, and nextRandom() if the type is an subclass of LogicalTypeCode.
	 */
	public static LogicalType newInstance(String qualifier) {
		PluginDefinition pluginDefinition = PluginDefinition.findByQualifier(qualifier);
		return LogicalTypeFactory.newInstance(pluginDefinition);
	}

	/**
	 * Return a Logical Type based on a Plugin Definition.
	 *
	 * @param plugin The Definition for this plugin
	 * @return The LogicalType The Logical Type associated with the definition (if it exists), null if non-existent.
	 */
	public static LogicalType newInstance(PluginDefinition plugin) {
		if (plugin == null)
			return null;

		Locale locale = null;

		// If the plugin is associated with a particular set of locales then use the first plausible one we find to override the default
		if (plugin.validLocales != null && plugin.validLocales.length != 0)
			locale = new Locale(plugin.validLocales[0]);
		else
			locale = Locale.getDefault();

		return newInstance(plugin, locale);
	}

	/**
	 * Return a Logical Type based on a Plugin Definition.
	 *
	 * @param plugin The Definition for this plugin
	 * @param locale The locale used for this LogicalType
	 * @return The LogicalType The Logical Type associated with the definition (if it exists), null if non-existent.
	 */
	public static LogicalType newInstance(PluginDefinition plugin, Locale locale) {
		LogicalType logical = null;

		if (plugin.clazz != null) {
			Class<?> newLogicalType;
			Constructor<?> ctor;

			try {
				newLogicalType = Class.forName(plugin.clazz);
				ctor = newLogicalType.getConstructor(PluginDefinition.class);
				logical = (LogicalTypeCode)ctor.newInstance(plugin);

			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				throw new IllegalArgumentException("Logical type: " + plugin.qualifier + " of class " + plugin.clazz + " does not appear to be a Logical Type.", e);
			}
		}
		else if (plugin.content != null)
			logical = new LogicalTypeFiniteSimpleExternal(plugin);
		else if (plugin.regExpReturned != null)
			logical = new LogicalTypeRegExp(plugin);

		if (logical == null || !(logical instanceof LogicalType))
			throw new IllegalArgumentException("Failed to instantiate a new Logical Type.");

		logical.initialize(locale);

		return logical;
	}
}
