package com.cobber.fta;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.cobber.fta.PatternInfo.Type;

public class LogicalTypeRegExp extends LogicalType {
	private PluginDefinition defn;
	private Locale locale;
	private Pattern pattern;
	private Long minLong;
	private Long maxLong;
	private Double minDouble;
	private Double maxDouble;
	Set<String> hotWords = new HashSet<>();

	/**
	 * @param plugin The definition of this plugin
	 */
	public LogicalTypeRegExp(PluginDefinition plugin) {
		this.defn = plugin;

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
		this.locale = locale;

		if (defn.hotWords.length != 0) {
			for (String elt : defn.hotWords) {
				hotWords.add(elt);
				hotWords.add(elt.toUpperCase(locale));
			}
		}

		try {
			pattern = Pattern.compile(defn.regExp);
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
		return defn.regExp;
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
			Map<String, Integer> cardinality, Map<String, Integer> outliers) {

		if (hotWords.size() != 0) {
			boolean found = hotWords.contains(dataStreamName) || hotWords.contains(dataStreamName.toUpperCase(locale));
			if (defn.hotWordMandatory && !found)
				return defn.regExp;
		}

		if (stringFacts != null) {
			switch (defn.baseType) {
			case LONG:
				if ((minLong != null && Long.parseLong(stringFacts.minValue) < minLong) ||
						(maxLong != null && Long.parseLong(stringFacts.maxValue) > maxLong))
					return defn.regExp;
			case DOUBLE:
				if ((minDouble != null && Double.parseDouble(stringFacts.minValue) < minDouble) ||
						(maxDouble != null && Double.parseDouble(stringFacts.maxValue) > maxDouble))
					return defn.regExp;
			}
		}

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : defn.regExp;
	}

	public String[] getHotWords() {
		return defn.hotWords;
	}
}
