package com.cobber.fta;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * TextAnalysisResult is the result of an analysis of a data stream. 
 */
public class TextAnalysisResult {
	int matchCount;
	int sampleCount;
	int nullCount;
	int blankCount;
	PatternInfo patternInfo;
	double confidence;
	String min;
	String max;
	String sum;
	Map<String, Integer> cardinality;
	Map<String, Integer> outliers;
	boolean key;
	
	TextAnalysisResult(int matchCount, PatternInfo patternInfo, int sampleCount, int nullCount, int blankCount, double confidence, String min, String max, String sum, Map<String, Integer> cardinality, Map<String, Integer> outliers, boolean key) {
		this.matchCount = matchCount;
		this.patternInfo = patternInfo;
		this.sampleCount = sampleCount;
		this.nullCount = nullCount;
		this.blankCount = blankCount;
		this.confidence = confidence;
		this.min = min;
		this.max = max;
		this.sum = sum;
		this.cardinality = cardinality;
		this.outliers = outliers;
		this.key = key;
	}

	
	/**
	 * Confidence in the type classification.
	 * @return Confidence as a percentage.
	 */
	public double getConfidence() {
		return confidence;
	}
	
	/**
	 * Type (as determined by training to date) as a String.  Possible types are: "String", "Long",
	 * "Double", "Date", "Time", or "DateTime".  In addition there are two pseudo-types "[BLANK]"
	 * (used to indicate a data stream with only empty fields i.e. "") and "[NULL]" (used to indicate
	 * a data stream with only null values). 
	 * @return The Type of the data stream.
	 */
	public String getType() {
		return patternInfo.type;
	}
	
	/**
	 * Get the optional Type Qualifier.  Possible qualifiers are "Email" for "String" and
	 * "Signed" for types of "Long" and "Double"
	 * @return The Type Qualifier for the Type.
	 */
	public String getTypeQualifier() {
		return patternInfo.typeQualifier;
	}
	
	/**
	 * Get the minimum value for numeric types ("Long" and "Double")
	 * @return The minimum value as a String.
	 */
	public String getMin() {
		return min;
	}
	
	/**
	 * Get the maximum value for numeric types ("Long" and "Double")
	 * @return The maximum value as a String.
	 */
	public String getMax() {
		return max;
	}
	
	/**
	 * Get Regular Expression that reflects the data stream.
	 * @return The Regular Expression.
	 */
	public String getPattern() {
		return patternInfo.pattern;
	}
	
	/**
	 * Get the count of all samples that matched the determined type.
	 * @return Count of all matches.
	 */
	public int getMatchCount() {
		return matchCount;
	}

	/**
	 * Get the count of all samples seen.
	 * @return Count of all samples.
	 */
	public int getSampleCount() {
		return sampleCount;
	}

	/**
	 * Get the count of all null samples.
	 * @return Count of all null samples.
	 */
	public int getNullCount() {
		return nullCount;
	}

	/**
	 * Get the count of all blank samples (Blank is "").  Note: "    " is not Blank.
	 * @return Count of all blank samples.
	 */
	public int getBlankCount() {
		return blankCount;
	}

	/**
	 * Get the cardinality for the current data stream.
	 * See {@link com.cobber.fta.TextAnalyzer#setMaxCardinality(int) setMaxCardinality()} method in TextAnalyzer.
	 * Note: This is not a complete cardinality analysis unless the cardinality of the
	 * data stream is less than the maximum cardinality (Default: {@value com.cobber.fta.TextAnalyzer#MAX_CARDINALITY_DEFAULT}).
	 * See also {@link com.cobber.fta.TextAnalyzer#setMaxCardinality(int) setMaxCardinality()} method in TextAnalyzer.
	 * @return Count of all blank samples.
	 */
	public int getCardinality() {
		return cardinality.size();
	}

	/**
	 * Get the cardinality details for the current data stream.  This is a Map of Strings and the count
	 * of occurrences.
	 * @return A Map of values and their occurrence frequency of the data stream to date.
	 */
	public Map<String, Integer> getCardinalityDetails() {
		return cardinality;
	}

	/**
	 * Get the outlier count for the current data stream.
	 * See {@link com.cobber.fta.TextAnalyzer#setMaxOutlierCount(int) setMaxOutliers()} method in TextAnalyzer.
	 * Note: This is not a complete outlier analysis unless the outlier of the
	 * data stream is less than the maximum outlier count (Default: {@value com.cobber.fta.TextAnalyzer#MAX_OUTLIERS_DEFAULT}).
	 * See also {@link com.cobber.fta.TextAnalyzer#setMaxOutlierCount(int) setMaxOutliers()} method in TextAnalyzer.
	 * @return Count of all blank samples.
	 */
	public int getOutlierCount() {
		return outliers.size();
	}

	/**
	 * Get the outlier details for the current data stream.  This is a Map of Strings and the count
	 * of occurrences.
	 * @return A Map of values and their occurrence frequency of the data stream to date.
	 */
	public Map<String, Integer> getOutlierDetails() {
		return outliers;
	}

	/**
	 * Is this field a possible key?
	 * @return True if the field could be a key field.
	 */
	public boolean isKey() {
		return key;
	}

	private static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
	    SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
	        new Comparator<Map.Entry<K,V>>() {
	            @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
	                int res = e2.getValue().compareTo(e1.getValue());
	                if (e1.getKey().equals(e2.getKey())) {
	                    return res;
	                } else {
	                    return res != 0 ? res : 1;
	                }
	            }
	        }
	    );
	    sortedEntries.addAll(map.entrySet());
	    return sortedEntries;
	}
	
	/**
	 * A String representation of the Analysis.  This is not suitable for anything other than
	 * debug output and is likely to change with no notice!!
	 * @return A String representation of the analysis to date.
	 */
	@Override
	public String toString() {
		String ret = "TextAnalysisResult [matchCount=" + matchCount + ", sampleCount=" + sampleCount + ", nullCount="
				+ nullCount + ", blankCount=" + blankCount+ ", pattern=\"" + patternInfo.pattern + "\", confidence=" + confidence +
				", type=" + patternInfo.type +
				(patternInfo.typeQualifier != null ? "(" + patternInfo.typeQualifier + ")" : "") + ", min=";
		if (min != null)
			ret += "\"" + min + "\"";
		else
			ret += "null";
		ret += ", max=";
		if (max != null)
			ret += "\"" + max + "\"";
		else
			ret += "null";
		ret += ", sum=";
		if (sum != null)
			ret += "\"" + sum + "\"";
		else
			ret += "null";
		ret += ", cardinality=" + (cardinality.size() < TextAnalyzer.MAX_CARDINALITY_DEFAULT ? String.valueOf(cardinality.size()) : "MAX");
		if (cardinality.size() < .2 * sampleCount && cardinality.size() < TextAnalyzer.MAX_CARDINALITY_DEFAULT) {
			ret += " {";
			int i = 0;
			SortedSet<Map.Entry<String, Integer>> ordered = entriesSortedByValues(cardinality);
			for (Map.Entry<String,Integer> entry : ordered) {
				if (i++ == 10) {
					ret += "...";
					break;
				}
				ret += "\"" + entry.getKey() + "\":" + entry.getValue();
				ret += " ";
			}
			ret += "}";
		}

		if (outliers.size() != 0 && outliers.size() != TextAnalyzer.MAX_OUTLIERS_DEFAULT) {
			ret += ", outliers=" + outliers.size();
			if (outliers.size() < .2 * sampleCount) {
				ret += " {";
				int i = 0;
				SortedSet<Map.Entry<String, Integer>> ordered = entriesSortedByValues(outliers);
				for (Map.Entry<String,Integer> entry : ordered) {
					if (i++ == 10) {
						ret += "...";
						break;
					}
					ret += "\"" + entry.getKey() + "\":" + entry.getValue();
					ret += " ";
				}
				ret += "}";
			}
		}

		if (key)
			ret += ", PossibleKey";

		ret += "]";
		return ret;
	}
}
