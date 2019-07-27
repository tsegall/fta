package com.cobber.fta;

import java.io.FileNotFoundException;

public class LogicalTypeFiniteSimpleExternal extends LogicalTypeFiniteSimple {
	public LogicalTypeFiniteSimpleExternal(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin, plugin.regExpReturned, plugin.backout, plugin.threshold);
		setContent(plugin.contentType, plugin.content);
	}
}
