package com.cobber.fta;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class TextAnalyzer {

	static final int SAMPLE_DEFAULT = 20;
	private int samples = SAMPLE_DEFAULT;
	static final int MAX_CARDINALITY_DEFAULT = 100;
	private int maxCardinality = MAX_CARDINALITY_DEFAULT;

	String name;
	DecimalFormatSymbols format;
	char decimalSeparator;
	char monetaryDecimalSeparator;
	char groupingSeparator;
	char minusSign;
	int sampleCount;
	int nullCount;
	int blankCount;
	Map<String, Integer> cardinality = new HashMap<String, Integer>();
	ArrayList<String> raw;				// 0245-11-98
	// 0: d{4}-d{2}-d{2}   1: d{+}-d{+}-d{+}    2: d{+}-d{+}-d{+}
	// 0: d{4}             1: d{+}              2: [-]d{+}
	// input "hello world" 0: a{5} a{5}         1: a{+} a{+}        2: a{+} 
	ArrayList<StringBuilder>[] levels = new ArrayList[3];
	
	String type;
	int matchLevel;
	int matchCount;
	String matchPattern;
	PatternInfo matchPatternInfo;

	boolean trainingStarted;
	
	double minDouble = Double.MAX_VALUE;
	double maxDouble = -Double.MAX_VALUE;
	BigDecimal sumBD = new BigDecimal(0);
	
	long minLong = Long.MAX_VALUE;
	long maxLong = Long.MIN_VALUE;
	BigInteger sumBI = new BigInteger("0");
	
	String min = null;
	String max = null;
	String sum = null;
	
	int minRawLength = Integer.MAX_VALUE;
	int maxRawLength = Integer.MIN_VALUE;
	
	int minLength = Integer.MAX_VALUE;
	int maxLength = Integer.MIN_VALUE;
	
	int possibleHMS = 0;
	int possibleHM = 0;
	int possibleEmails = 0;
	
	Map.Entry<String, Integer> lastCompute;
	
	static HashMap<String, PatternInfo> patternInfo = null;
	
	void addPattern(String pattern, String generalPattern, String format, String type, String typeQualifier) {
		patternInfo.put(pattern, new PatternInfo(pattern, generalPattern, format, type, typeQualifier));
	}

	void initPatternInfo() {
		patternInfo = new HashMap<String, PatternInfo>();
		addPattern("\\d{4}-\\d{2}-\\d{2}", "\\d{4}-\\d{1,2}-\\d{1,2}", "yyyy-MM-dd", "Date", null);
		addPattern("\\d{4}-\\d{1}-\\d{2}", "\\d{4}-\\d{1,2}-\\d{1,2}", "yyyy-MM-dd", "Date", null);
		addPattern("\\d{4}-\\d{1}-\\d{1}", "\\d{4}-\\d{1,2}-\\d{1,2}", "yyyy-MM-dd", "Date", null);
		addPattern("\\d{4}-\\d{2}-\\d{1}", "\\d{4}-\\d{1,2}-\\d{1,2}", "yyyy-MM-dd", "Date", null);
		addPattern("\\d{4}-\\d{1,2}-\\d{1,2}", null, "yyyy-MM-dd", "Date", null);

		addPattern("\\d{4}/\\d{2}/\\d{2}", "\\d{4}/\\d{1,2}/\\d{1,2}", "yyyy/MM/dd", "Date", null);
		addPattern("\\d{4}/\\d{1}/\\d{2}", "\\d{4}/\\d{1,2}/\\d{1,2}", "yyyy/MM/dd", "Date", null);
		addPattern("\\d{4}/\\d{2}/\\d{1}", "\\d{4}/\\d{1,2}/\\d{1,2}", "yyyy/MM/dd", "Date", null);
		addPattern("\\d{4}/\\d{1}/\\d{1}", "\\d{4}/\\d{1,2}/\\d{1,2}", "yyyy/MM/dd", "Date", null);
		addPattern("\\d{4}/\\d{1,2}/\\d{1,2}", null, "yyyy/MM/dd", "Date", null);
		
		addPattern("\\d{4} \\d{2} \\d{2}", "\\d{4} \\d{1,2} \\d{1,2}", "yyyy MM dd", "Date", null);
		addPattern("\\d{4} \\d{2} \\d{2}", "\\d{4} \\d{1,2} \\d{1,2}", "yyyy MM dd", "Date", null);
		addPattern("\\d{4} \\d{2} \\d{2}", "\\d{4} \\d{1,2} \\d{1,2}", "yyyy MM dd", "Date", null);
		addPattern("\\d{4} \\d{2} \\d{2}", "\\d{4} \\d{1,2} \\d{1,2}", "yyyy MM dd", "Date", null);
		addPattern("\\d{4} \\d{1,2} \\d{1,2}", null, "yyyy MM dd", "Date", null);

		addPattern("\\d{2}-\\d{2}-\\d{4}", "\\d{1,2}-\\d{1,2}-\\d{4}", "dd-MM-yyyy", "Date", null);
		addPattern("\\d{1}-\\d{2}-\\d{4}", "\\d{1,2}-\\d{1,2}-\\d{4}", "dd-MM-yyyy", "Date", null);
		addPattern("\\d{2}-\\d{1}-\\d{4}", "\\d{1,2}-\\d{1,2}-\\d{4}", "dd-MM-yyyy", "Date", null);
		addPattern("\\d{1}-\\d{1}-\\d{4}", "\\d{1,2}-\\d{1,2}-\\d{4}", "dd-MM-yyyy", "Date", null);
		addPattern("\\d{1,2}-\\d{1,2}-\\d{4}", null, "dd-MM-yyyy", "Date", null);

		addPattern("\\d{2}/\\d{2}/\\d{4}", "\\d{1,2}/\\d{1,2}/\\d{4}", "dd/MM/yyyy", "Date", null);
		addPattern("\\d{2}/\\d{2}/\\d{4}", "\\d{1,2}/\\d{1,2}/\\d{4}", "dd/MM/yyyy", "Date", null);
		addPattern("\\d{2}/\\d{2}/\\d{4}", "\\d{1,2}/\\d{1,2}/\\d{4}", "dd/MM/yyyy", "Date", null);
		addPattern("\\d{2}/\\d{2}/\\d{4}", "\\d{1,2}/\\d{1,2}/\\d{4}", "dd/MM/yyyy", "Date", null);
		addPattern("\\d{1,2}/\\d{1,2}/\\d{4}", null, "dd/MM/yyyy", "Date", null);

		addPattern("\\d{2} \\d{2} \\d{4}", null, "dd MM yyyy", "Date", null);
		addPattern("\\d{2} \\a{3} \\d{4}", null, "", "Date", null);

		addPattern("\\d{2}-\\a{3}-\\d{2}", "\\d{1,2}-\\a{3}-\\d{2}", "d-MMM-yy", "Date", null);
		addPattern("\\d{1}-\\a{3}-\\d{2}", "\\d{1,2}-\\a{3}-\\d{2}", "d-MMM-yy", "Date", null);
		addPattern("\\d{1,2}-\\a{3}-\\d{2}", null, "d-MMM-yy", "Date", null);

		addPattern("\\d{2}:\\d{2}:\\d{2}", "\\d{1,2}:\\d{2}:\\d{2}", "hh:mm:ss", "Time", null);
		addPattern("\\d{1}:\\d{2}:\\d{2}", "\\d{1,2}:\\d{2}:\\d{2}", "hh:mm:ss", "Time", null);
		addPattern("\\d{1,2}:\\d{2}:\\d{2}", null, "hh:mm:ss", "Time", null);

		addPattern("\\d{2}:\\d{2}", "\\d{1,2}:\\d{2}", "hh:mm", "Time", null);
		addPattern("\\d{1}:\\d{2}", "\\d{1,2}:\\d{2}", "hh:mm", "Time", null);
		addPattern("\\d{1,2}:\\d{2}", null, "hh:mm", "Time", null);
		
		// \d{2}/\d{1}/\d{4} \d{2}:\d{2}:\d{2}		

		addPattern("\\d{2}/\\d{2}/\\d{2} \\d{2}:\\d{2}", "\\d{1,2}/\\d{1,2}/\\d{2} \\d{1,2}:\\d{2}", "MM/dd/yy hh:mm", "DateTime", null);
		addPattern("\\d{1}/\\d{2}/\\d{2} \\d{2}:\\d{2}", "\\d{1,2}/\\d{1,2}/\\d{2} \\d{1,2}:\\d{2}", "MM/dd/yy hh:mm", "DateTime", null);
		addPattern("\\d{2}/\\d{1}/\\d{2} \\d{2}:\\d{2}", "\\d{1,2}/\\d{1,2}/\\d{2} \\d{1,2}:\\d{2}", "MM/dd/yy hh:mm", "DateTime", null);
		addPattern("\\d{1}/\\d{1}/\\d{2} \\d{2}:\\d{2}", "\\d{1,2}/\\d{1,2}/\\d{2} \\d{1,2}:\\d{2}", "MM/dd/yy hh:mm", "DateTime", null);
		addPattern("\\d{2}/\\d{2}/\\d{2} \\d{1}:\\d{2}", "\\d{1,2}/\\d{1,2}/\\d{2} \\d{1,2}:\\d{2}", "MM/dd/yy hh:mm", "DateTime", null);
		addPattern("\\d{1}/\\d{2}/\\d{2} \\d{1}:\\d{2}", "\\d{1,2}/\\d{1,2}/\\d{2} \\d{1,2}:\\d{2}", "MM/dd/yy hh:mm", "DateTime", null);
		addPattern("\\d{2}/\\d{1}/\\d{2} \\d{1}:\\d{2}", "\\d{1,2}/\\d{1,2}/\\d{2} \\d{1,2}:\\d{2}", "MM/dd/yy hh:mm", "DateTime", null);
		addPattern("\\d{1}/\\d{1}/\\d{2} \\d{1}:\\d{2}", "\\d{1,2}/\\d{1,2}/\\d{2} \\d{1,2}:\\d{2}", "MM/dd/yy hh:mm", "DateTime", null);
		addPattern("\\d{1,2}/\\d{1,2}/\\d{2} \\d{1,2}:\\d{2}", null, "MM/dd/yy hh:mm", "DateTime", null);

		addPattern("\\d{4}-\\d{2}-\\d{2}\\a{1}\\d{2}:\\d{2}", null, "", "DateTime", null);
		addPattern("\\d{4}/\\d{2}/\\d{2}\\a{1}\\d{2}:\\d{2}", null, "", "DateTime", null);
		addPattern("\\d{4} \\d{2} \\d{2}\\a{1}\\d{2}:\\d{2}", null, "", "DateTime", null);

		addPattern("\\d{2}-\\d{2}-\\d{4}\\a{1}\\d{2}:\\d{2}", null, "", "DateTime", null);
		addPattern("\\d{2}/\\d{2}/\\d{4}\\a{1}\\d{2}:\\d{2}", null, "", "DateTime", null);
		addPattern("\\d{2} \\d{2} \\d{4}\\a{1}\\d{2}:\\d{2}", null, "", "DateTime", null);

		addPattern("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", "\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2}", "yyyy-MM-dd hh:mm:ss", "DateTime", null);
		addPattern("\\d{4}-\\d{1}-\\d{2} \\d{2}:\\d{2}:\\d{2}", "\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2}", "yyyy-MM-dd hh:mm:ss", "DateTime", null);
		addPattern("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", "\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2}", "yyyy-MM-dd hh:mm:ss", "DateTime", null);
		addPattern("\\d{4}-\\d{1}-\\d{2} \\d{2}:\\d{2}:\\d{2}", "\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2}", "yyyy-MM-dd hh:mm:ss", "DateTime", null);
		addPattern("\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2}", null, "yyyy-MM-dd hh:mm:ss", "DateTime", null);

		addPattern("\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}:\\d{2}", "\\d{1,2}-\\d{1,2}-\\d{4} \\d{2}:\\d{2}:\\d{2}", "dd/MM/yyyy hh:mm:ss", "DateTime", null);
		addPattern("\\d{2}-\\d{1}-\\d{4} \\d{2}:\\d{2}:\\d{2}", "\\d{1,2}-\\d{1,2}-\\d{4} \\d{2}:\\d{2}:\\d{2}", "dd/MM/yyyy hh:mm:ss", "DateTime", null);
		addPattern("\\d{1}-\\d{2}-\\d{4} \\d{2}:\\d{2}:\\d{2}", "\\d{1,2}-\\d{1,2}-\\d{4} \\d{2}:\\d{2}:\\d{2}", "dd/MM/yyyy hh:mm:ss", "DateTime", null);
		addPattern("\\d{1}-\\d{1}-\\d{4} \\d{2}:\\d{2}:\\d{2}", "\\d{1,2}-\\d{1,2}-\\d{4} \\d{2}:\\d{2}:\\d{2}", "dd/MM/yyyy hh:mm:ss", "DateTime", null);
		addPattern("\\d{1,2}-\\d{1,2}-\\d{4} \\d{2}:\\d{2}:\\d{2}", null, "dd-MM-yyyy hh:mm:ss", "DateTime", null);

		addPattern("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}", "\\d{1,2}/\\d{1,2}/\\d{4} \\d{2}:\\d{2}:\\d{2}", "dd/MM/yyyy hh:mm:ss", "DateTime", null);
		addPattern("\\d{2}/\\d{1}/\\d{4} \\d{2}:\\d{2}:\\d{2}", "\\d{1,2}/\\d{1,2}/\\d{4} \\d{2}:\\d{2}:\\d{2}", "dd/MM/yyyy hh:mm:ss", "DateTime", null);
		addPattern("\\d{1}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}", "\\d{1,2}/\\d{1,2}/\\d{4} \\d{2}:\\d{2}:\\d{2}", "dd/MM/yyyy hh:mm:ss", "DateTime", null);
		addPattern("\\d{1}/\\d{1}/\\d{4} \\d{2}:\\d{2}:\\d{2}", "\\d{1,2}/\\d{1,2}/\\d{4} \\d{2}:\\d{2}:\\d{2}", "dd/MM/yyyy hh:mm:ss", "DateTime", null);
		addPattern("\\d{1,2}/\\d{1,2}/\\d{4} \\d{2}:\\d{2}:\\d{2}", null, "dd/MM/yyyy hh:mm:ss", "DateTime", null);

		addPattern("(?i)true|false", null, "", "Boolean", null);
		addPattern("\\a{+}", null, "", "String", null);
		addPattern("\\d{+}", null, "", "Long", null);
		addPattern("[-]\\d{+}", null, "", "Long", "Signed");
		addPattern("\\d{*}D\\d{+}", null, "", "Double", null);
		addPattern("[-]\\d{*}D\\d{+}", null, "", "Double", "Signed");
		addPattern("^$", null, null, "[BLANK]", null);
	}
	
	TextAnalyzer(String name) {
		if (patternInfo == null) {
			initPatternInfo();
		}

		this.name = name;
		format = new DecimalFormatSymbols();
		decimalSeparator = format.getDecimalSeparator();
		monetaryDecimalSeparator = format.getMonetaryDecimalSeparator();
		groupingSeparator = format.getGroupingSeparator();
		minusSign = format.getMinusSign();
	}
	
	TextAnalyzer() {
		this("anonymous");
	}

	/**
	 * Set the number of Samples to collect before attempting to determine the type.
	 * @param samples The number of samples to collect
	 * @return The previous value of this parameter
	 */
	public int setSampleSize(int samples) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change sample size once training has started");
		if (samples < SAMPLE_DEFAULT)
			throw new IllegalArgumentException("Cannot set sample size below " + SAMPLE_DEFAULT);
			
		int ret = samples;
		this.samples = samples;
		return ret;
	}

	/**
	 * Set the maximum cardinality that we will track.
	 * @param maxCardinality The maximum Cardinality that will be tracked (0 implies no tracking)
	 * @return The previous value of this parameter
	 */
	public int setMaxCardinality(int maxCardinality) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change maxCardinality once training has started");
		if (samples < 0)
			throw new IllegalArgumentException("Invalid value for maxCardinality " + maxCardinality);

		int ret = maxCardinality;
		this.maxCardinality = maxCardinality;
		return ret;
	}

	private boolean trackLong(String rawInput) {					
		long l;
		String input = rawInput.trim();
		
		try {
			l = Long.parseLong(input);
		}
		catch (NumberFormatException e) {
			return false;
		}
		if (l < minLong) {
			minLong = l;
		}
		if (l > maxLong) {
			maxLong = l;
		}
		int digits = l < 0 ? input.length() - 1 : input.length();
		if (digits < minLength)
			minLength = digits;
		if (digits > maxLength)
			maxLength = digits;
		
		sumBI = sumBI.add(BigInteger.valueOf(l));
			
		return true;
	}

	private boolean trackBooleanInfo(String input) {					
		return "true".equalsIgnoreCase(input.trim()) || "false".equalsIgnoreCase(input.trim()); 
	}

	private boolean trackString(String input) {
		if ("Email".equals(matchPatternInfo.typeQualifier)) {
			// Address lists commonly have ;'s as separators as opposed to the ','
			if (input.indexOf(';') != -1)
				input = input.replaceAll(";", ",");
			try {
				InternetAddress[] emails = InternetAddress.parse(input);
				return emails.length != 0;
			} catch (AddressException e) {
				return false;
			}		
		}

		return true;
	}
	
	private boolean trackDouble(String input) {					
		double d;

		try {
			d = Double.parseDouble(input.trim());
		}
		catch (NumberFormatException e) {
			return false;
		}
		if (d < minDouble) {
			minDouble = d;
		}
		if (d > maxDouble) {
			maxDouble = d;
		}

		sumBD = sumBD.add(BigDecimal.valueOf(d));
		
		return true;
	}
	
	private boolean trackDate(String dateFormat, String input) {
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);

		try {
			sdf.parse(input);
		}
		catch (ParseException e) {
			return false;
		}

		return true;
	}

	public boolean train(String rawInput) {
		// Initialize if we have not already done so
		if (!trainingStarted) {
			trainingStarted = true;
			raw = new ArrayList<String>(samples);
			levels[0] = new ArrayList<StringBuilder>(samples);
			levels[1] = new ArrayList<StringBuilder>(samples);
			levels[2] = new ArrayList<StringBuilder>(samples);
		}

		sampleCount++;

		if (rawInput == null) {
			nullCount++;
			return type != null;
		}

		if (rawInput.length() == 0) {
			blankCount++;
			return type != null;
		}

		raw.add(rawInput);
		
		String input = rawInput.trim();
		
		int length = input.length();

		Integer seen = cardinality.get(input);
		if (seen == null) {
			if (cardinality.size() < maxCardinality)
				cardinality.put(input, 1);
		}
		else
			cardinality.put(input, seen + 1);
		
		StringBuilder l0 = new StringBuilder(length);

		// Walk the string
		boolean numericSigned = false;
		int numericDecimalSeparators = 0;
		boolean notNumericOnly = false;
		int colons = 0;
		int colonIndex = -1;
		int commas = 0;
		int semicolons = 0;
		int atSigns = 0;
		for (int i = 0; i < length; i++) {
			char ch = input.charAt(i);
			if (i == 0 && ch == minusSign) {
				numericSigned = true;
			} else if (Character.isDigit(ch)) {
				l0.append('d');
			} else if (ch == decimalSeparator) { 
				l0.append('D');
				numericDecimalSeparators++;
			} else if (ch == groupingSeparator) {
				l0.append('G');
			} else if (Character.isAlphabetic(ch)) {
				l0.append('a');
				notNumericOnly = true;
			} else {
				if (ch == ':') {
					colons++;
					colonIndex = i;
				}
				else if (ch == '@')
					atSigns++;
				else if (ch == ',')
					commas++;
				else if (ch == ';')
					semicolons++;
				l0.append(ch);
				notNumericOnly = true;
			}
		}

		if (colons == 2) {
			if (colonIndex + 2 < length && colonIndex - 3 > 0 &&
					Character.isDigit(input.charAt(colonIndex - 1)) && Character.isDigit(input.charAt(colonIndex - 2)) &&
					input.charAt(colonIndex - 3) == ':' && Character.isDigit(input.charAt(colonIndex - 4)) &&
					Character.isDigit(input.charAt(colonIndex + 1)) && Character.isDigit(input.charAt(colonIndex + 1))) {
				possibleHMS++;
//				System.err.println("HMS detected " + name);
			}
		} else if (colons == 1) {
			if (colonIndex + 2 < length && colonIndex - 1 > 0 && Character.isDigit(input.charAt(colonIndex - 1)) &&
					Character.isDigit(input.charAt(colonIndex + 1)) && Character.isDigit(input.charAt(colonIndex + 1))) { 
				possibleHM++;
//				System.err.println("HM detected " + name);
			}
		}
		if (atSigns - 1 == commas || atSigns - 1 == semicolons)
			possibleEmails++;
		
		StringBuilder compressedl0 = new StringBuilder(length);
		if ("true".equalsIgnoreCase(input) || "false".equalsIgnoreCase(input)) {
			compressedl0.append("(?i)true|false");
		}
		else {
			// Walk the new level0 to create the new level1
			String l0withSentinel = l0.toString() + "|";
			char last = l0withSentinel.charAt(0);
			int repetitions = 1;
			for (int i = 1; i < l0withSentinel.length(); i++) {
				char ch = l0withSentinel.charAt(i);
				if (ch == last) {
					repetitions++;
				} else {
					if (last == 'd' || last == 'a') {
						compressedl0.append('\\').append(last);
						compressedl0.append('{').append(String.valueOf(repetitions)).append('}');
					}
					else {
						for (int j = 0; j < repetitions; j++) {
							compressedl0.append(last);
						}
					}
					last = ch;
					repetitions = 1;
				}
			}
		}
		levels[0].add(compressedl0);
		
		// Create the level 1 and 2
		if (notNumericOnly == false && numericDecimalSeparators <= 1) {
			StringBuilder l1 = new StringBuilder();
			StringBuilder l2 = new StringBuilder().append('[').append(minusSign).append(']');
			if (numericSigned)
				l1.append('[').append(minusSign).append(']');
			if (numericDecimalSeparators == 1) {
				l1.append("\\d{*}D");
				l2.append("\\d{*}D");
			}
			l1.append("\\d{+}");
			l2.append("\\d{+}");
			levels[1].add(l1);
			levels[2].add(l2);
		}
		else {
			// Fast version of replaceAll("\\{\\d*\\}", "{+}")
			StringBuilder collapsed = new StringBuilder(compressedl0);
			for (int i = 0; i < collapsed.length(); i++) {
				if (collapsed.charAt(i) == '{' && Character.isDigit(collapsed.charAt(i + 1))) {
					int start = ++i;
					while (collapsed.charAt(++i) != '}')
						/*EMPTY*/;
					if (start + 1 == i)
						collapsed.setCharAt(start, '+');
					else
						collapsed.replace(start, i, "+");
				}
			}
			
			// Level 1 is the collapsed version e.g. convert d{4}-d{2}-d{2] to d{+}-d{+}-d{+}
			PatternInfo found = patternInfo.get(compressedl0.toString());
			if (found != null && found.generalPattern != null) {
				levels[1].add(new StringBuilder(found.generalPattern));
				levels[2].add(new StringBuilder(collapsed));
			}
			else {	
				levels[1].add(new StringBuilder(collapsed));
				levels[2].add(new StringBuilder("\\a{+}"));
			}

		}
		
		trackResult(rawInput);

		if (sampleCount - (nullCount + blankCount) > samples) {
			raw.remove(0);
			levels[0].remove(0);
			levels[1].remove(0);
			levels[2].remove(0);
		}
		
		return type != null;
	}

	String lastResult() {
		int last = raw.size() - 1;
		return raw.get(last).toString() + " --- " +
				levels[0].get(last).toString() + " --- " +
				levels[1].get(last).toString();
	}
	
	Map.Entry<String, Integer> mergeable(int levelIndex, Map.Entry<String, Integer> larger, Map.Entry<String, Integer> smaller) {
		String largerKey = larger.getKey();
		String smallerKey = smaller.getKey();
		
		int offset = largerKey.lastIndexOf(smallerKey);
		if (offset != -1) {
			String missing = largerKey.substring(0, offset);
			// Coalesce the common numeric case of '.43' being the same as 0.43 
			if ("\\d{+}".equals(missing)) {
				String newKey = "\\d{*}" + smallerKey;
				return new AbstractMap.SimpleEntry<String, Integer>(String.valueOf(newKey.hashCode()) + "." + newKey, larger.getValue() + smaller.getValue());
			// Coalesce the common optional minus sign	
			} else if (missing.length() == 1 && missing.charAt(0) == minusSign)  {
				String newKey = "[" + minusSign + "]" + smallerKey;
				return new AbstractMap.SimpleEntry<String, Integer>(String.valueOf(newKey.hashCode()) + "." + newKey, larger.getValue() + smaller.getValue());
			}
		}

		return larger.getValue() > smaller.getValue() ? larger : smaller;
	}

	private Map.Entry<String, Integer> getBest(int levelIndex) {
		ArrayList<StringBuilder> level = levels[levelIndex];
		if (level.isEmpty())
			return null;

		Map<String, Integer> map = new HashMap<String, Integer>();
		
		// Calculate the frequency of every element
		for (StringBuilder s : level) {
			String key = s.toString();
			Integer seen = map.get(key);
			if (seen == null) {
				map.put(key, 1);
			}
			else {
				map.put(key, seen + 1);
			}
		}

		// Grab the best and the second best based on frequency
		int bestCount = 0;
		int secondBestCount = 0;
		Map.Entry<String, Integer> best = null;
		Map.Entry<String, Integer> secondBest = null;
		for (Map.Entry<String, Integer> entry : map.entrySet()) {
			if (entry.getValue() > bestCount) {
				secondBest = best;
				secondBestCount = bestCount;
				best = entry;
				bestCount = entry.getValue();
			} else if (entry.getValue() > secondBestCount) {
				secondBest = entry;
				secondBestCount = entry.getValue();
			}
		}

		String bestKey = best.getKey();
		String secondBestKey;
		if (secondBest != null) {
			secondBestKey = secondBest.getKey();
			
			PatternInfo bestPattern = patternInfo.get(bestKey);
			PatternInfo secondBestPattern = patternInfo.get(secondBestKey);
			if (bestPattern != null && secondBestPattern != null) {
				if (bestPattern.isNumeric() && secondBestPattern.isNumeric()) {
					if (!bestPattern.type.equals(secondBestPattern.type)) {
						// Promote Long to Double
						String newKey = "Double".equals(bestKey) ? best.getKey() : secondBest.getKey();
						best = new AbstractMap.SimpleEntry<String, Integer>(newKey, best.getValue() + secondBest.getValue());
					}
				}
				else if ("String".equals(bestPattern.type)) {
					// Promote anything to "String"
					best = new AbstractMap.SimpleEntry<String, Integer>(bestKey, best.getValue() + secondBest.getValue());
				}
			}
		}

		return best;
	}
	
	private void determineType() {
		// If we have fewer than 6 samples do not even pretend
		if (sampleCount - (nullCount + blankCount) < 6) {
			matchPattern = "\\a{+}";
			matchPatternInfo = patternInfo.get(matchPattern);
			type = matchPatternInfo.type;
			return;
		}

		Map.Entry<String, Integer> l0 = getBest(0);
		Map.Entry<String, Integer> l1 = getBest(1);
		Map.Entry<String, Integer> l2 = getBest(2);
		Map.Entry<String, Integer> best = l0;
		String pattern = null;
		
		if (best != null) {
			matchLevel = 1;
			pattern = best.getKey();
			matchPatternInfo = patternInfo.get(pattern);
			
			// Take any level 1 with something we recognize or a better count
			if (l1 != null && (matchPatternInfo == null || l1.getValue() > best.getValue())) {
				best = l1;
				matchLevel = 2;
				pattern = best.getKey();
				matchPatternInfo = patternInfo.get(pattern);
			}
			// Take a level 2 if
			//   - we have something we recognize (and we had nothing)
			//   - we have the same key but a better count
			//   - we have an improvement of at least 10%
			if (l2 != null && (matchPatternInfo == null || 
					(l1.getKey().equals(l2.getKey()) && l2.getValue() > best.getValue()) ||
					(!l1.getKey().equals(l2.getKey()) && l2.getValue() > best.getValue() + samples/10))) {
				best = l2;
				matchLevel = 3;
				pattern = best.getKey();
				matchPatternInfo = patternInfo.get(pattern);
			}
			if (type == null) {
				if (matchPatternInfo != null) {
					type = matchPatternInfo.type;
				}

				if (type != null) {
					switch (type) {
					case "Boolean":
						for (String sample : raw) {
							trackBooleanInfo(sample);
						}
						break;
						
					case "Long":
						for (String sample : raw) {
							trackLong(sample);
						}
						break;
						
					case "Double":
						for (String sample : raw) {
							trackDouble(sample);
						}
						break;
					
					case "String":
						if (possibleEmails == raw.size())
							matchPatternInfo = new PatternInfo(matchPattern, null, null, "String", "Email");
						for (String sample : raw) {
							trackString(sample);
						}
						break;
						
					case "Date":
					case "Time":
					case "DateTime":
						for (String sample : raw) {
							trackDate(matchPatternInfo.format, sample);
						}
						break;
					
					}

					matchCount = best.getValue();
					matchPattern = pattern;
				}
			}
		}
	}
	
	private void trackResult(String input) {
		// We always want to track basic facts for the field
		int length = input.length();

		if (length != 0 && length < minRawLength)
			minRawLength = length;
		if (length > maxRawLength)
			maxRawLength = length;
		
		// If the cache is full and we still have not determined a type so compute one
		if (type == null && sampleCount - (nullCount + blankCount) > samples) {
			determineType();
			return;
		}

		if (type == null) {
			return;
		}

		int lastIndex = raw.size() - 1;
		
		switch (type) {
		case "Boolean":
			if (trackBooleanInfo(input))
				matchCount++;
			break;
			
		case "Long":
			if (trackLong(input))
				matchCount++;
			break;
				
		case "Double":
			if (trackDouble(input))
				matchCount++;
			break;

		case "String":
			if (trackString(input))
				matchCount++;
			break;
			
		case "Date":
		case "Time":
		case "DateTime":
			if (trackDate(matchPatternInfo.format, input))
				matchCount++;
			break;
			
		default:
			System.err.println("HELLO");
			if ((matchLevel == 1 && matchPattern.equals(levels[0].get(lastIndex).toString())) ||
					(matchLevel == 2 && matchPattern.equals(levels[1].get(lastIndex).toString()))) {
				matchCount++;
			}
		}
	}

	public TextAnalysisResult getResult() {
		// If we have not already determined the type, now we need to
		if (type == null) {
			determineType();
		}
		
		// Compute our confidence
		double confidence = 0;
		int realSamples = sampleCount - (nullCount + blankCount);

		if (blankCount == sampleCount || nullCount == sampleCount) {
			type = blankCount == sampleCount ? "[BLANK]" : "[NULL]";
			matchPattern = "^$";
			matchPatternInfo = patternInfo.get(matchPattern);
			matchCount = sampleCount;
			confidence = sampleCount >= 10 ? 1.0 : 0.0;
		}
		else {
			confidence = (double)matchCount / realSamples;
		}
		
		if ("\\a{+}".equals(matchPattern)) {
			matchPattern = "\\a{" + minRawLength;
			if (minRawLength != maxRawLength)
				matchPattern += "," + maxRawLength;
			matchPattern += "}";
			matchPatternInfo = new PatternInfo(matchPattern, null, null, "String", matchPatternInfo.typeQualifier);
		} else if ("\\d{+}".equals(matchPattern)) {
			if (cardinality.size() == 2 && minLong == 0 && maxLong == 1) {
				// boolean by any other name
				matchPattern = "[0|1]";
				min = "0";
				max = "1";
				type = "Boolean";
				matchPatternInfo = new PatternInfo("[0|1]", null, null, "Boolean", null);
			}
			else {
				// We thought it was an integer field, but on reflection it does not feel like it
				if (realSamples > 100 && confidence < 0.9) {
					matchPattern = "\\a{" + minRawLength;
					type = "String";
					matchCount = realSamples;
					confidence = 1.0;
					if (minRawLength != maxRawLength)
						matchPattern += "," + maxRawLength;
					matchPattern += "}";
					matchPatternInfo = new PatternInfo(matchPattern, null, null, "String", null);
				}
				else {
					matchPattern = "\\d{" + minLength;
					if (minLength != maxLength)
						matchPattern += "," + maxLength;
					matchPattern += "}";
					matchPatternInfo = new PatternInfo(matchPattern, null, null, type, null);
				}
			}
		}

		if (type != null) {
			switch (type) {
			case "Long":
				min = String.valueOf(minLong);
				max = String.valueOf(maxLong);
				sum = sumBI.toString();
				break;
				
			case "Double":
				min = String.valueOf(minDouble);
				max = String.valueOf(maxDouble);
				sum = sumBD.toString();
				break;
			}
		}
		
		return new TextAnalysisResult(matchCount, matchPatternInfo, sampleCount, nullCount, blankCount, confidence, min, max, sum, cardinality);
	}
}
