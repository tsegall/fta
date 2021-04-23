package com.cobber.fta.core;

public class FTAPluginException extends Exception {
	private static final long serialVersionUID = 1L;

	public FTAPluginException(String message) {
		super(message);
	}

	public FTAPluginException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
