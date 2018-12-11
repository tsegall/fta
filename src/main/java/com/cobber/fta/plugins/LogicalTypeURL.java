package com.cobber.fta.plugins;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PatternInfo;

public class LogicalTypeURL extends LogicalTypeInfinite {

	@Override
	public boolean initialize() {
		return true;
	}

	@Override
	public String getQualifier() {
		return "URL";
	}

	@Override
	public String getRegexp() {
		return 	"^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
	}
		
	@Override
	public double getSampleThreshold() {
		return 0.95;
	}

	@Override
	public PatternInfo.Type getBaseType() {
		return PatternInfo.Type.STRING;
	}

	@Override
	public boolean isValid(String input) {
		try {
			final URL url = new URL(input);
			url.toURI();
			return true;
		} catch (MalformedURLException | URISyntaxException exception) {
			return false;
		}
	}

	@Override
	public boolean isCandidate(String input, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		boolean result = charCounts[':'] != 0 && compressed.indexOf("://") != -1;
		if (result)
			candidateCount++;
		return result;
	}
}