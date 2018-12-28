package com.cobber.fta;

import java.io.InputStreamReader;

public class PluginCUSIP extends LogicalTypeFiniteSimple {
    public PluginCUSIP() {
    	super("CUSIP", "\\p{Alnum}{9}", "\\p{Alnum}{9}",
			new InputStreamReader(PluginCUSIP.class.getResourceAsStream("/CUSIP.txt")), 99);
	}
}
