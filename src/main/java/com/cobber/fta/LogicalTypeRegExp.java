package com.cobber.fta;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.cobber.fta.PatternInfo.Type;

public class LogicalTypeRegExp extends LogicalType {
	private Pattern pattern;
	private Long minLong;
	private Long maxLong;
	private Double minDouble;
	private Double maxDouble;
	public LogicalTypeRegExp(PluginDefinition plugin) {
		super(plugin);

		if (defn.minimum != null)
			switch (defn.baseType) {
			case LONG:
				minLong = defn.minimum != null ? Long.parseLong(defn.minimum) : null;
				maxLong = defn.maximum != null ? Long.parseLong(defn.maximum) : null;
				break;

			case DOUBLE:
				minDouble = defn.minimum != null ? Double.parseDouble(defn.minimum) : null;
				maxDouble = defn.minimum != null ? Double.parseDouble(defn.maximum) : null;
				break;
			}
	}

	@Override
	public boolean initialize(Locale locale) {
		super.initialize(locale);

		try {
			pattern = Pattern.compile(defn.regExpReturned);
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	@Override
	public String getQualifier() {
		return defn.qualifier;
	}

	@Override
	public String getRegExp() {
		return defn.regExpReturned;
	}

	@Override
	public boolean isRegExpComplete() {
		return true;
	}

	@Override
	public Type getBaseType() {
		return defn.baseType;
	}

	@Override
	public boolean isValid(String input) {
		return pattern.matcher(input).matches();
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts,
			Map<String, Long> cardinality, Map<String, Long> outliers) {

		if (defn.headerRegExps != null) {
			boolean requiredHeaderMissing = false;
			for (int i = 0; i < defn.headerRegExps.length && !requiredHeaderMissing; i++) {
				if (defn.headerRegExpConfidence[i] == 100 && !dataStreamName.matches(defn.headerRegExps[i]))
					requiredHeaderMissing = true;
			}
			if (requiredHeaderMissing)
				return defn.regExpReturned;
		}

		if (stringFacts != null) {
			switch (defn.baseType) {
			case LONG:
				if ((minLong != null && Long.parseLong(stringFacts.minValue) < minLong) ||
						(maxLong != null && Long.parseLong(stringFacts.maxValue) > maxLong))
					return defn.regExpReturned;
			case DOUBLE:
				if ((minDouble != null && Double.parseDouble(stringFacts.minValue) < minDouble) ||
						(maxDouble != null && Double.parseDouble(stringFacts.maxValue) > maxDouble))
					return defn.regExpReturned;
			}
		}

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : defn.regExpReturned;
	}

	public boolean isMatch(String regExp) {
		if (defn.regExpsToMatch == null)
			return true;

		for (String re : defn.regExpsToMatch) {
			if (regExp.equals(re))
				return true;
		}

		return false;
	}

	public String[] getHeaderRegExps() {
		return defn.headerRegExps;
	}

	@Override
	public boolean isClosed() {
		return false;
	}
}
