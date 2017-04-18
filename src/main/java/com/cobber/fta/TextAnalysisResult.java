package com.cobber.textanalysis;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

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
	
	TextAnalysisResult(int matchCount, PatternInfo patternInfo, int sampleCount, int nullCount, int blankCount, double confidence, String min, String max, String sum, Map<String, Integer> cardinality) {
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
	}

	public double getConfidence() {
		return confidence;
	}
	
	public String getType() {
		return patternInfo.type;
	}
	
	public String getTypeQualifier() {
		return patternInfo.typeQualifier;
	}
	
	public String getMin() {
		return min;
	}
	
	public String getMax() {
		return max;
	}
	
	public String getPattern() {
		return patternInfo.pattern;
	}
	
	public int getMatchCount() {
		return matchCount;
	}

	public int getSampleCount() {
		return sampleCount;
	}

	public int getNullCount() {
		return nullCount;
	}

	public int getBlankCount() {
		return blankCount;
	}

	public int getCardinality() {
		return cardinality.size();
	}

	public Map<String, Integer> getCardinalityDetails() {
		return cardinality;
	}

	static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
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
		ret += ", cardinality=" + cardinality.size();
		if (cardinality.size() < .2 * sampleCount && cardinality.size() != TextAnalyzer.MAX_CARDINALITY_DEFAULT) {
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
		ret += "]";
		return ret;
	}
}
