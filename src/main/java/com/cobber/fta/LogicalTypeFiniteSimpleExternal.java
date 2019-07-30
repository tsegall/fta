package com.cobber.fta;

public class LogicalTypeFiniteSimpleExternal extends LogicalTypeFiniteSimple {
	public LogicalTypeFiniteSimpleExternal(PluginDefinition plugin) {
		super(plugin, plugin.regExpReturned, plugin.backout, plugin.threshold);
		setContent(plugin.contentType, plugin.content);
	}
}
