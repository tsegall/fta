package com.cobber.fta;

import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Analyze String data to determine whether input represents a date or datetime.
 *
 * <p>
 * Typical usage is:
 * </p>
 * <pre>
 * {@code
 *		DateTimeParser dtp = new DateTimeParser(false);
 *
 *		dtp.train("2/7/2012 06:24:47");
 *		dtp.train("2/7/2012 09:44:04");
 *		dtp.train("2/7/2012 06:21:38");
 *		dtp.train("1/7/2012 23:16:14");
 *		dtp.train("19/7/2012 17:49:53");
 *
 *		DateTimeParserResult result = dtp.getResult();
 *      // Expect "d/M/yyyy HH:mm:ss");
 *		System.err.println(result.getFormatString());
 * }
 * </pre>
 */
public class DateTimeParser {
	// When we have ambiguity - should we prefer to conclude day first, month first or unspecified
	Boolean dayFirst = null;

	static class SimpleDateMatcher {
		String matcher;
		String format;
		int dayOffset;
		int dayLength;
		int monthOffset;
		int monthLength;
		int yearOffset;
		int yearLength;

		SimpleDateMatcher(String matcher, String format, int[] dateFacts) {
			this.matcher = matcher;
			this.format = format;
			this.dayOffset = dateFacts[0];
			this.dayLength = dateFacts[1];
			this.monthOffset = dateFacts[2];
			this.monthLength = dateFacts[3];
			this.yearOffset = dateFacts[4];
			this.yearLength = dateFacts[5];
		}
	}
	static Map<String, Integer> months = new HashMap<String, Integer>();
	static String monthPattern = null;
	static Map<String, Integer> monthAbbr = new HashMap<String, Integer>();
	static Set<String> timeZones = new HashSet<String>();
	static Map<String, SimpleDateMatcher> simpleDateMatcher = new HashMap<String, SimpleDateMatcher>();

	static {

		GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();

		int actualMonths = cal.getActualMaximum(GregorianCalendar.MONTH);

		// Setup the Months
		int shortestMonth = Integer.MAX_VALUE;
		int longestMonth = Integer.MIN_VALUE;
		String[] longMonths = new DateFormatSymbols().getMonths();
		for (int i = 0; i <= actualMonths; i++) {
			String month = longMonths[i].toUpperCase(Locale.ROOT);
			months.put(month, i + 1);
			int len = month.length();
			if (len < shortestMonth)
				shortestMonth = len;
			if (len > longestMonth)
				longestMonth = len;
		}
		monthPattern = "\\p{Alpha}{" + String.valueOf(shortestMonth) + "," + String.valueOf(longestMonth) + "}";


		// Setup the Monthly abbreviations
		String[] shortMonths = new DateFormatSymbols().getShortMonths();
		for (int i = 0; i <= actualMonths; i++) {
			monthAbbr.put(shortMonths[i].toUpperCase(Locale.ROOT), i + 1);
		}

		// Cache the set of available Time Zones
		Collections.addAll(timeZones, TimeZone.getAvailableIDs());
		// Add the non-real Time Zones (that people use)
		timeZones.addAll(Arrays.asList(new String[] {"EDT", "CDT", "MDT", "PDT"}));

		simpleDateMatcher.put("d{4} d{2} d{2}", new SimpleDateMatcher("d{4} d{2} d{2}", "yyyy MM dd", new int[] {8, 2, 5, 2, 0, 4}));
		simpleDateMatcher.put("d{4} d d{2}", new SimpleDateMatcher("d{4} d d{2}", "yyyy M dd", new int[] {7, 2, 5, 1, 0, 4}));
		simpleDateMatcher.put("d{4} d{2} d", new SimpleDateMatcher("d{4} d{2} d", "yyyy MM d", new int[] {8, 1, 5, 2, 0, 4}));
		simpleDateMatcher.put("d{4} d d", new SimpleDateMatcher("d{4} d d", "yyyy M d", new int[] {7, 1, 5, 1, 0, 4}));

		simpleDateMatcher.put("d{2} a{3} d{4}", new SimpleDateMatcher("d{2} a{3} d{4}", "dd MMM yyyy", new int[] {0, 2, 3, 3, 7, 4}));
		simpleDateMatcher.put("d a{3} d{4}", new SimpleDateMatcher("d a{3} d{4}", "d MMM yyyy", new int[] {0, 1, 2, 3, 6, 4}));
		simpleDateMatcher.put("d{2}-a{3}-d{4}", new SimpleDateMatcher("d{2}-a{3}-d{4}", "dd-MMM-yyyy", new int[] {0, 2, 3, 3, 7, 4}));
		simpleDateMatcher.put("d-a{3}-d{4}", new SimpleDateMatcher("d-a{3}-d{4}", "d-MMM-yyyy", new int[] {0, 1, 2, 3, 6, 4}));
		simpleDateMatcher.put("d{2}/a{3}/d{4}", new SimpleDateMatcher("d{2}/a{3}/d{4}", "dd/MMM/yyyy", new int[] {0, 2, 3, 3, 7, 4}));
		simpleDateMatcher.put("d/a{3}/d{4}", new SimpleDateMatcher("d/a{3}/d{4}", "d/MMM/yyyy", new int[] {0, 1, 2, 3, 6, 4}));

		simpleDateMatcher.put("d{2} a{4} d{4}", new SimpleDateMatcher("d{2} a{4} d{4}", "dd MMMM yyyy", new int[] {0, 2, 3, -5, -4, 4}));
		simpleDateMatcher.put("d a{4} d{4}", new SimpleDateMatcher("d a{4} d{4}", "d MMMM yyyy", new int[] {0, 1, 2, -5, -4, 4}));
		simpleDateMatcher.put("d{2}-a{4}-d{4}", new SimpleDateMatcher("d{2}-a{4}-d{4}", "dd-MMMM-yyyy", new int[] {0, 2, 3, -5, -4, 4}));
		simpleDateMatcher.put("d-a{4}-d{4}", new SimpleDateMatcher("d-a{4}-d{4}", "d-MMMM-yyyy", new int[] {0, 1, 2, -5, -4, 4}));
		simpleDateMatcher.put("d{2}/a{4}/d{4}", new SimpleDateMatcher("d{2}/a{4}/d{4}", "dd/MMMM/yyyy", new int[] {0, 2, 3, -5, -4, 4}));
		simpleDateMatcher.put("d/a{4}/d{4}", new SimpleDateMatcher("d/a{4}/d{4}", "d/MMMM/yyyy", new int[] {0, 1, 2, -5, -4, 4}));

		simpleDateMatcher.put("d{2} a{3} d{2}", new SimpleDateMatcher("d{2} a{3} d{2}", "dd MMM yy", new int[] {0, 2, 3, 3, 7, 2}));
		simpleDateMatcher.put("d a{3} d{2}", new SimpleDateMatcher("d a{3} d{2}", "d MMM yy", new int[] {0, 1, 2, 3, 6, 2}));
		simpleDateMatcher.put("d{2}-a{3}-d{2}", new SimpleDateMatcher("d{2}-a{3}-d{2}", "dd-MMM-yy", new int[] {0, 2, 3, 3, 7, 2}));
		simpleDateMatcher.put("d-a{3}-d{2}", new SimpleDateMatcher("d-a{3}-d{2}", "d-MMM-yy", new int[] {0, 1, 2, 3, 6, 2}));
		simpleDateMatcher.put("d{2}/a{3}/d{2}", new SimpleDateMatcher("d{2}/a{3}/d{2}", "dd/MMM/yy", new int[] {0, 2, 3, 3, 7, 2}));
		simpleDateMatcher.put("d/a{3}/d{2}", new SimpleDateMatcher("d/a{3}/d{2}", "d/MMM/yy", new int[] {0, 1, 2, 3, 6, 2}));

		simpleDateMatcher.put("a{3} d{2}, d{4}", new SimpleDateMatcher("a{3} d{2}, d{4}", "MMM dd',' yyyy", new int[] {4, 2, 0, 3, 8, 4}));
		simpleDateMatcher.put("a{3} d, d{4}", new SimpleDateMatcher("a{3} d, d{4}", "MMM d',' yyyy", new int[] {4, 1, 0, 3, 7, 4}));
		simpleDateMatcher.put("a{3} d{2} d{4}", new SimpleDateMatcher("a{3} d d{4}", "MMM dd yyyy", new int[] {4, 2, 0, 3, 7, 4}));
		simpleDateMatcher.put("a{3} d d{4}", new SimpleDateMatcher("a{3} d d{4}", "MMM d yyyy", new int[] {4, 1, 0, 3, 6, 4}));
		simpleDateMatcher.put("a{3}-d{2}-d{4}", new SimpleDateMatcher("a{3}-þþ-d{4}", "MMM-dd-yyyy", new int[] {4, 2, 0, 3, 7, 4}));
		simpleDateMatcher.put("a{3}-d-d{4}", new SimpleDateMatcher("a{3}-d-d{4}", "MMM-d-yyyy", new int[] {4, 1, 0, 3, 6, 4}));

		simpleDateMatcher.put("a{4} d{2}, d{4}", new SimpleDateMatcher("a{4} d{2}, d{4}", "MMMM dd',' yyyy", new int[] {-8, 2, 0, 3, -4, 4}));
		simpleDateMatcher.put("a{4} d, d{4}", new SimpleDateMatcher("a{4} d, d{4}", "MMMM d',' yyyy", new int[] {-7, 1, 0, 3, -4, 4}));
		simpleDateMatcher.put("a{4} d{2} d{4}", new SimpleDateMatcher("a{4} d d{4}", "MMMM dd yyyy", new int[] {-7, 2, 0, 3, -4, 4}));
		simpleDateMatcher.put("a{4} d d{4}", new SimpleDateMatcher("a{4} d d{4}", "MMMM d yyyy", new int[] {-6, 1, 0, 3, -4, 4}));
		simpleDateMatcher.put("a{4}-d{2}-d{4}", new SimpleDateMatcher("a{4}-d{2}-d{4}", "MMMM-dd-yyyy", new int[] {-7, 2, 0, 3, -4, 4}));
		simpleDateMatcher.put("a{4}-d-d{4}", new SimpleDateMatcher("a{4}-d-d{4}", "MMMM-d-yyyy", new int[] {-6, 1, 0, 3, -4, 4}));

		simpleDateMatcher.put("d{8}Td{6}Z", new SimpleDateMatcher("d{8}Td{6}Z", "yyyyMMdd'T'HHmmss'Z'", new int[] {6, 2, 4, 2, 0, 4}));
		simpleDateMatcher.put("d{8}Td{6}", new SimpleDateMatcher("d{8}Td{6}", "yyyyMMdd'T'HHmmss", new int[] {6, 2, 4, 2, 0, 4}));

		simpleDateMatcher.put("d{2}/a{3}/d{2} d:d{2} P", new SimpleDateMatcher("d{2}/a{3}/d{2} d:d{2} P", "dd/MMM/yy h:mm a", new int[] {0, 2, 3, 3, 7, 2}));
		simpleDateMatcher.put("d{2}/a{3}/d{2} d{2}:d{2} P", new SimpleDateMatcher("d{2}/a{3}/d{2} d{2}:d{2} P", "dd/MMM/yy hh:mm a", new int[] {0, 2, 3, 3, 7, 2}));
	}

	static int monthAbbreviationOffset(String month) {
		Integer offset = monthAbbr.get(month.toUpperCase(Locale.ROOT));
		return offset == null ? -1 : offset;
	}

	static int monthOffset(String month) {
		Integer offset = months.get(month.toUpperCase(Locale.ROOT));
		return offset == null ? -1 : offset;
	}

	Map<String, Integer> results = new HashMap<String, Integer>();
	int sampleCount = 0;
	int nullCount = 0;
	int blankCount = 0;
	int invalidCount = 0;

	DateTimeParser(Boolean dayFirst) {
		this.dayFirst = dayFirst;
	}

	DateTimeParser() {
		this(null);
	}

	/**
	 * Train is the core entry point used to supply input to the DateTimeParser.
	 * @param input The String representing a date with possible surrounding whitespace.
	 * @return A String representing the DateTime detected (Using DateTimeFormatter Patterns) or null if no match.
	 */
	public String train(String input) {
		sampleCount++;

		if (input == null) {
			nullCount++;
			return null;
		}

		String trimmed = input.trim();
		if (input.length() == 0) {
			blankCount++;
			return null;
		}

		String ret = determineFormatString(trimmed, null);
		if (ret == null) {
			invalidCount++;
			return null;
		}
		Integer seen = results.get(ret);
		if (seen == null)
			results.put(ret, 1);
		else
			results.put(ret, seen + 1);

		return ret;
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	/**
	 * Determine the result of the training complete to date. Typically invoked
	 * after all training is complete, but may be invoked at any stage.
	 * @return A DateTimeParserResult with the analysis of any training completed.
	 */
	public DateTimeParserResult getResult() {
		// If we have no good samples, call it a day
		if (sampleCount == nullCount + blankCount + invalidCount)
			return null;

		// If there is only one result then it must be correct :-)
		if (results.size() == 1)
			return DateTimeParserResult.asResult(results.keySet().iterator().next(), dayFirst);

		int timeElements = -1;
		int hourLength = -1;
		int dateElements = -1;
		int[] dateFieldLengths = new int[] {-1,-1,-1};
		int[] timeFieldLengths = new int[] {-1,-1,-1,-1};
		Boolean timeFirst = null;
		int dayOffset = -1;
		int monthOffset = -1;
		int yearOffset = -1;
		Character dateSeparator = null;
		Character dateTimeSeparator = null;
		String timeZone = null;
		Boolean amPmIndicator = null;
		StringBuilder last = null;
		String current = null;
		String answer = null;
		Pattern dayRE = Pattern.compile("([^d]*)([d]+)(.*)");
		Pattern monthRE = Pattern.compile("([^M]*)([M]+)(.*)");
		Pattern hourRE = Pattern.compile("([^H]*)([H]+)(.*)");
		Pattern unboundRE = Pattern.compile("([^?]*)([?]+)([^?]*)([?]+)(.*)");
		String answerMonths = null;
		String answerDays = null;
		String answerHours = null;
		String currentMonths = null;
		String currentDays = null;
		String currentHours = null;
		Matcher answerM = null;
		Matcher answerD = null;
		Matcher answerH = null;
		Matcher unbound = null;

		// Sort the results of our training by value so that we consider the most frequent first
		Map<String, Integer> byValue = sortByValue(results);

		// Iterate through all the results of our training, merging them to produce our best guess
		for (Map.Entry<String, Integer> entry : byValue.entrySet()) {
			String key = entry.getKey();
			DateTimeParserResult result = DateTimeParserResult.asResult(key, dayFirst);
			if (result == null) {
				System.err.println("NOT FOUND - input: '" + key + "'");
				continue;
			}
			if (timeElements == -1)
				timeElements = result.timeElements;
			if (result.timeFieldLengths != null) {
				for (int i = 0; i < result.timeFieldLengths.length; i++) {
					if (timeFieldLengths[i] == -1)
						timeFieldLengths[i] = result.timeFieldLengths[i];
					else
						if (result.timeFieldLengths[i] < timeFieldLengths[i])
							timeFieldLengths[i] = result.timeFieldLengths[i];
				}
			}
			if (hourLength == -1 || result.hourLength == 1 && hourLength == 2)
				hourLength = result.hourLength;
			if (timeFirst == null)
				timeFirst = result.timeFirst;
			if (dateElements == -1)
				dateElements = result.dateElements;

			// Merge two date lengths:
			//  - d and dd -> d
			//  - M and MM -> M
			//  - MMM and MMMM -> MMMM
			if (result.dateFieldLengths != null) {
				for (int i = 0; i < result.dateFieldLengths.length; i++) {
					if (dateFieldLengths[i] == -1)
						dateFieldLengths[i] = result.dateFieldLengths[i];
					else
						if (result.dateFieldLengths[i] == 1 && result.dateFieldLengths[i] < dateFieldLengths[i])
							dateFieldLengths[i] = result.dateFieldLengths[i];
				}
			}
			if (dayOffset == -1 && result.dayOffset != -1) {
				dayOffset = result.dayOffset;
			}
			if (monthOffset == -1 && result.monthOffset != -1) {
				monthOffset = result.monthOffset;
			}
			if (yearOffset == -1 && result.yearOffset != -1) {
				yearOffset = result.yearOffset;
			}
			if (dateSeparator == null)
				dateSeparator = result.dateSeparator;
			if (dateTimeSeparator == null)
				dateTimeSeparator = result.dateTimeSeparator;
			if (timeZone == null)
				timeZone = result.timeZone;
			if (amPmIndicator == null)
				amPmIndicator = result.amPmIndicator;

			current = key;
			Matcher m = monthRE.matcher(key);
			if (m.matches() && m.groupCount() == 3)
				currentMonths = m.group(2);
			Matcher d = dayRE.matcher(key);
			if (d.matches() && d.groupCount() == 3)
				currentDays = d.group(2);
			Matcher h = hourRE.matcher(key);
			if (h.matches() && h.groupCount() == 3)
				currentHours = h.group(2);
			if (answer == null) {
				answer = key;
				answerMonths = currentMonths;
				answerDays = currentDays;
				answerHours = currentHours;
			}
			else {
				if (answerDays == null && currentDays != null) {
					answer = key;
					answerD = dayRE.matcher(key);
					if (answerD.matches() && answerD.groupCount() == 3) {
						answerDays = answerD.group(2);
					}
				}
				if (answerMonths == null && currentMonths != null) {
					answerM = monthRE.matcher(key);
					if (answerM.matches() && answerM.groupCount() == 3) {
						answerMonths = answerM.group(2);
					}
				}

				// Manage the variable length fields:
				//   d and dd -> d
				//   M and MM -> M
				//   MMM and MMMM -> MMMM
				//   HH and H -> H
				if ("dd".equals(answerDays) && "d".equals(currentDays)) {
					answerD = dayRE.matcher(key);
					if (answerD.matches() && answerD.groupCount() == 3) {
						answer = answerD.group(1) + currentDays + answerD.group(3);
						answerDays = currentDays;
					}
				}
				if ("MM".equals(answerMonths) && "M".equals(currentMonths)) {
					answerM = monthRE.matcher(key);
					if (answerM.matches() && answerM.groupCount() == 3) {
						answer = answerM.group(1) + currentMonths + answerM.group(3);
						answerMonths = currentMonths;
					}
				}
				if ("MMM".equals(answerMonths) && "MMMM".equals(currentMonths)) {
					answerM = monthRE.matcher(key);
					if (answerM.matches() && answerM.groupCount() == 3) {
						answer = answerM.group(1) + currentMonths + answerM.group(3);
						answerMonths = currentMonths;
					}
				}
				if ("HH".equals(answerHours) && "H".equals(currentHours)) {
					answerH = hourRE.matcher(key);
					if (answerH.matches() && answerH.groupCount() == 3) {
						answer = answerH.group(1) + currentHours + answerH.group(3);
						answerHours = currentHours;
					}
				}
			}
		}

		if (dayFirst != null) {
			unbound = unboundRE.matcher(answer);
			if (unbound.matches() && unbound.groupCount() == 5) {
				if (dayFirst)
					answer = unbound.group(1) + unbound.group(2).replace('?','d') + unbound.group(3) + unbound.group(4).replace('?','M') + unbound.group(5);
				else
					answer = unbound.group(1) + unbound.group(2).replace('?','M') + unbound.group(3) + unbound.group(4).replace('?','d') + unbound.group(5);
			}
		}

		answerH = hourRE.matcher(answer);
		if (answerH.matches() && answerH.groupCount() == 3) {
			answerHours = answerH.group(2);
			if ("HH".equals(answerHours) && hourLength == 1) {
				answer = answerH.group(1) + "H" + answerH.group(3);
			}
		}

		answerD = dayRE.matcher(answer);
		if (dayOffset != -1 && answerD.matches() && answerD.groupCount() == 3) {
			answerDays = answerD.group(2);
			if ("dd".equals(answerDays) && dateFieldLengths[dayOffset] == 1) {
				answer = answerD.group(1) + "d" + answerD.group(3);
			}
		}

		answerM = monthRE.matcher(answer);
		if (monthOffset != -1 && answerM.matches() && answerM.groupCount() == 3) {
			answerMonths = answerM.group(2);
			if ("MM".equals(answerMonths) && dateFieldLengths[monthOffset] == 1) {
				answer = answerM.group(1) + "M" + answerM.group(3);
			}
		}

		if (timeZone == null)
			timeZone = "";

		return new DateTimeParserResult(answer.toString(), dayFirst, timeElements, timeFieldLengths, hourLength, dateElements, dateFieldLengths,
				timeFirst, dateTimeSeparator, yearOffset, monthOffset, dayOffset, dateSeparator, timeZone, amPmIndicator);
	}

	static String retDigits(int digitCount, char patternChar) {
		String ret = String.valueOf(patternChar);
		if (digitCount == 1)
			return ret;
		if (digitCount == 2)
			return ret + patternChar;
		return ret + patternChar + patternChar + patternChar;
	}

	/**
	 * Give a String as input with an offset and length return the integer at that position.
	 * @param input String to extract integer from
	 * @param offset Integer offset that marks the start
	 * @param length Integer length of integer to be extracted.
	 * @return An integer value from the supplied String.
	 */
	static int getValue(String input, int offset, int length) {
		return Integer.valueOf(input.substring(offset, offset + length));
	}

	static boolean plausibleDate(int[] dateValues, int[] dateDigits, int[] fieldOffsets) {
		int monthDays[] = {-1, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
		int year = dateValues[fieldOffsets[2]];
		if (year == 0 && dateDigits[fieldOffsets[2]] == 4)
			return false;
		int month = dateValues[fieldOffsets[1]];
		if (month == 0 || month > 12)
			return false;
		int day = dateValues[fieldOffsets[0]];
		if (day == 0 || day > monthDays[month])
			return false;
		return true;
	}

	private static String dateFormat(int[] dateDigits, char dateSeparator, Boolean dayFirst, boolean yearKnown) {
		if (dayFirst == null)
			return retDigits(dateDigits[0], '?') + dateSeparator + retDigits(dateDigits[1], '?') + dateSeparator + retDigits(dateDigits[2], yearKnown ? 'y' : '?');

		if (dayFirst)
			return retDigits(dateDigits[0], 'd') + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + retDigits(dateDigits[2], 'y');

		return retDigits(dateDigits[0], 'M') + dateSeparator + retDigits(dateDigits[1], 'd') + dateSeparator + retDigits(dateDigits[2], 'y');
	}

	static String compress(String input) {
		StringBuilder result = new StringBuilder();
		int len = input.length();
		char lastCh = '=';
		int count = 0;
		boolean amIndicator = false;
		if (input.toLowerCase(Locale.ROOT).endsWith("am") || input.toLowerCase(Locale.ROOT).endsWith("pm")) {
			len -= 2;
			amIndicator = true;
		}

		for (int i = 0; i < len; i++) {
			char ch = input.charAt(i);
			if (Character.isDigit(ch)) {
				if (Character.isDigit(lastCh))
					count++;
				else {
					if (count != 0) {
						result.append("{" + String.valueOf(count) + "}");
						count = 0;
					}
					result.append('d');
					lastCh = ch;
				}
			}
			else if (Character.isAlphabetic(ch)) {
				if (Character.isAlphabetic(lastCh)) {
					if (count < 3)
						count++;
				}
				else {
					if (count != 0) {
						result.append("{" + String.valueOf(count + 1) + "}");
						count = 0;
					}
					if ((i+1 == len || !Character.isAlphabetic(input.charAt(i+1))) && (ch == 'T' || ch == 'Z'))
							result.append(ch);
					else
						result.append('a');
					lastCh = ch;
				}
			}
			else {
				if (count != 0) {
					result.append("{" + String.valueOf(count + 1) + "}");
					count = 0;
				}
				result.append(ch);
				lastCh = ch;
			}
		}
		if (count != 0) {
			result.append("{" + String.valueOf(count + 1) + "}");
			count = 0;
		}

		if (amIndicator)
			result.append('P');

		return result.toString();
	}

	/**
	 * Determine a FormatString from an input string that may represent a Date, Time,
	 * DateTime, OffsetDateTime or a ZonedDateTime.
	 * @param input The String representing a date with optional leading/trailing whitespace
	 * @param dayFirst When we have ambiguity - should we prefer to conclude day first, month first or unspecified
	 * @return A String representing the DateTime detected (Using DateTimeFormatter Patterns) or null if no match.
	 */
	public static String determineFormatString(String input, Boolean dayFirst) {
		int len = input.length();

		// Remove leading spaces
		int start = 0;
		while (start < len && input.charAt(start) == ' ')
			start++;

		// Remove trailing spaces
		while (len >= 1 && input.charAt(len - 1) == ' ')
			len--;

		len -= start;

		String trimmed = input.substring(start, len + start);

		// Fail fast if we can
		if (len < 4 || (!Character.isDigit(trimmed.charAt(0)) && !Character.isAlphabetic(trimmed.charAt(0))))
			return null;

		if (trimmed.indexOf('þ') != -1)
			return null;

		String compressed = compress(trimmed);

		SimpleDateMatcher matcher = simpleDateMatcher.get(compressed);
		if (matcher != null) {
			int[] dateValue = new int[] {-1, -1, -1};
			if (matcher.monthLength < 0) {
				dateValue[1] = DateTimeParser.monthOffset(trimmed.substring(matcher.monthOffset, len + matcher.monthLength));
				if (dateValue[1] == -1)
					return null;
			}
			else if (matcher.monthLength == 3) {
				dateValue[1] = DateTimeParser.monthAbbreviationOffset(trimmed.substring(matcher.monthOffset, matcher.monthOffset + matcher.monthLength));
				if (dateValue[1] == -1)
					return null;
			}
			else {
				dateValue[1] = getValue(trimmed, matcher.monthOffset, matcher.monthLength);
			}
			dateValue[0] = getValue(trimmed, matcher.dayOffset >= 0 ? matcher.dayOffset : len + matcher.dayOffset, matcher.dayLength);
			dateValue[2] = getValue(trimmed, matcher.yearOffset >= 0 ? matcher.yearOffset : len + matcher.yearOffset, matcher.yearLength);

			if (!plausibleDate(dateValue, new int[] {matcher.dayLength, matcher.monthLength, matcher.yearLength}, new int[] {0,1,2}))
				return null;

			return matcher.format;
		}

		// Fail fast if we can
		if (!compressed.contains(":d{") && !compressed.contains("/d{") && !compressed.contains("-d{"))
			return null;

		// Fail fast if we can
		if (!Character.isDigit(trimmed.charAt(0)))
			return null;

		int digits = 0;
		int value = 0;
		int[] dateValue = new int[3];
		int[] dateDigits = new int[3];
		int[] timeValue = new int[4];
		int[] timeDigits = new int[4];
		char dateSeparator = '_';
		int dateComponent = 0;
		int timeComponent = 0;
		int hourLength = -1;
		boolean timeFirst = false;
		boolean yearInDateFirst = false;
		boolean fourDigitYear = false;
		boolean timeSeen = false;
		boolean timeClosed = false;
		boolean dateSeen = false;
		boolean dateClosed = false;
		String timeZone = "";
		boolean ISO8601 = false;
		boolean expectingAlphaTimeZone = false;

		int lastCh = 'þ';
		for (int i = 0; i < len && timeZone.length() == 0; i++) {
			char ch = trimmed.charAt(i);

			// Two spaces in a row is always bad news
			if (lastCh == ' ' && ch == ' ')
				return null;
			lastCh = ch;

			if (expectingAlphaTimeZone) {
				String currentTimeZone = trimmed.substring(i, len);
				if (!DateTimeParser.timeZones.contains(currentTimeZone))
					return null;
				timeZone = " z";
				continue;
			}

			switch (ch) {
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				value = value * 10 + ch - '0';
				digits++;
				if (digits > 4)
					return null;
				break;

			case ':':
				if ((dateSeen && !dateClosed) || (timeSeen && timeClosed) || timeComponent == 3)
					return null;

				timeFirst = dateComponent == 0;
				timeSeen = true;
				timeValue[timeComponent] = value;
				timeDigits[timeComponent] = digits;

				if (timeComponent == 0) {
					if (digits != 1 && digits != 2)
						return null;
					hourLength = digits;
				}
				if (timeComponent == 1 && digits != 2)
					return null;
				if (timeComponent == 2)
					return null;
				timeComponent++;
				digits = 0;
				value = 0;
				break;

			case '+':
				if (!ISO8601)
					return null;

				// FALL THROUGH

			case '-':
				if (ISO8601 || (dateSeen && dateClosed && timeSeen && timeComponent == 2)) {
					int hours = Integer.MIN_VALUE;
					int minutesOffset = Integer.MIN_VALUE;
					int secondsOffset = Integer.MIN_VALUE;

					i++;

					String offset = compress(trimmed.substring(i, len));

					// Expecting DD:DD:DD or DDDDDD or DD:DD or DDDD or DD
					if (i + 8 <= len && "d{2}:d{2}:d{2}".equals(offset)) {
						timeZone = "xxxxx";
						minutesOffset = 3;
						secondsOffset = 6;
					}
					else if (i + 6 <= len && "d{6}".equals(offset)) {
						timeZone = "xxxx";
						minutesOffset = 2;
						secondsOffset = 4;
					}
					else if (i + 5 <= len && "d{2}:d{2}".equals(offset)) {
						timeZone = "xxx";
						minutesOffset = 3;
					}
					else if (i + 4 <= len && "d{4}".equals(offset)) {
						timeZone = "xx";
					}
					else if (i + 2 <= len && "d{2}".equals(offset)) {
						timeZone = "x";
					}
					else
						return null;

					// Validate the hours
					hours = getValue(trimmed, i, 2);
					if (hours != Integer.MIN_VALUE && hours > 18)
						return null;

					// Validate the minutes
					if (minutesOffset != Integer.MIN_VALUE) {
						int minutes = getValue(trimmed, i + minutesOffset, 2);
						if (minutes != Integer.MIN_VALUE && minutes > 59)
							return null;
					}

					// Validate the seconds
					if (secondsOffset != Integer.MIN_VALUE) {
						int seconds = getValue(trimmed, i + secondsOffset, 2);
						if (seconds != Integer.MIN_VALUE && seconds > 59)
							return null;
					}
					break;
				}
				// FALL THROUGH

			case '/':
				if (timeSeen && !timeClosed)
					return null;
				if (dateComponent == 2)
					return null;

				dateSeen = true;
				dateValue[dateComponent] = value;
				dateDigits[dateComponent] = digits;
				if (dateComponent == 0) {
					dateSeparator = ch;
					fourDigitYear = digits == 4;
					yearInDateFirst = fourDigitYear || (digits == 2 && value > 31);
					if (!yearInDateFirst && digits != 1 && digits != 2)
						return null;
				} else if (dateComponent == 1) {
					if (ch != dateSeparator)
						return null;
					if (digits != 1 && digits != 2)
						return null;
				}

				dateComponent++;
				digits = 0;
				value = 0;
				break;

			case '.':
				if ((dateSeen && !dateClosed) || (timeSeen && timeClosed) || timeComponent != 2 || digits != 2)
					return null;
				timeValue[timeComponent] = value;
				timeDigits[timeComponent] = digits;
				timeComponent++;
				digits = 0;
				value = 0;
				break;

			case 'T':
				// ISO 8601
				if (timeSeen)
					return null;
				if (!dateSeen || dateClosed || digits != 2 || dateSeparator != '-' || !fourDigitYear || !yearInDateFirst)
					return null;
				ISO8601 = true;
				dateValue[dateComponent] = value;
				dateDigits[dateComponent] = digits;
				dateClosed = true;
				digits = 0;
				value = 0;
				break;

			case ' ':
				if (!dateSeen && !timeSeen)
					return null;
				if (timeSeen && !timeClosed) {
					if (digits != 2)
						return null;
					timeValue[timeComponent] = value;
					timeDigits[timeComponent] = digits;
					timeClosed = true;
				}
				else if (dateSeen && !dateClosed) {
					if (dateComponent != 2)
						return null;
					if (!((digits == 2) || (yearInDateFirst == false && digits == 4)))
						return null;
					if (!fourDigitYear)
						fourDigitYear = digits == 4;
					dateValue[dateComponent] = value;
					dateDigits[dateComponent] = digits;
					dateClosed = true;
				}
				else
					return null;
				digits = 0;
				value = 0;
				if (timeSeen && dateSeen)
					expectingAlphaTimeZone = true;
				break;

			default:
				return null;
			}
		}

		if (!dateSeen && !timeSeen)
			return null;

		if (dateSeen && !dateClosed) {
			// Need to close out the date
			if (yearInDateFirst) {
				if (digits != 2)
					return null;
			}
			else {
				if (digits != 2 && digits != 4)
					return null;
			}
			fourDigitYear = digits == 4;
			dateValue[dateComponent] = value;
			dateDigits[dateComponent] = digits;
			digits = 0;
		}
		if (timeSeen && !timeClosed) {
			// Need to close out the time
			if ((timeComponent != 3 && digits != 2) || (timeComponent == 3 && (digits == 0 || digits > 9)))
				return null;
			timeValue[timeComponent] = value;
			timeDigits[timeComponent] = digits;
			digits = 0;
		}

		if (digits != 0)
			return null;

		if (ISO8601 && timeComponent == 0)
			return null;

		String timeAnswer = null;
		if (timeComponent != 0) {
			if (timeValue[0] > 23 || timeValue[1] > 59 || (timeComponent >= 2 && timeValue[2] > 59))
				return null;
			String hours = hourLength == 1 ? "H" : "HH";
			timeAnswer = hours + ":mm";
			if (timeComponent >= 2)
				timeAnswer += ":ss";
			if (timeComponent == 3)
				timeAnswer += "." + "SSSSSSSSS".substring(0, timeDigits[3]);
			if (dateComponent == 0)
				return timeAnswer;
		}

		String dateAnswer = null;

		// Do we have any date components?
		if (dateComponent != 0) {
			// If we don't have two date components then it is invalid
			if (dateComponent == 1)
				return null;
			if (dateValue[1] == 0 || dateValue[1] > 31)
				return null;
			if (yearInDateFirst) {
				if (!plausibleDate(dateValue, dateDigits, new int[] {2,1,0}))
					return null;
				dateAnswer = retDigits(dateDigits[0], 'y') + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + retDigits(dateDigits[2], 'd');
			}
			else {
				if (fourDigitYear) {
					// Year is the last field - attempt to determine which is the month
					if (dateValue[0] > 12) {
						if (!plausibleDate(dateValue, dateDigits, new int[] {0,1,2}))
							return null;
						dateAnswer = "dd" + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + "yyyy";
					}
					else if (dateValue[1] > 12) {
						if (!plausibleDate(dateValue, dateDigits, new int[] {1,0,2}))
							return null;
						dateAnswer = retDigits(dateDigits[0], 'M') + dateSeparator + "dd" + dateSeparator + "yyyy";
					}
					else
						dateAnswer = dateFormat(dateDigits, dateSeparator, dayFirst, true);
				} else {
					// If the first group of digits is of length 1, then it is either d/MM/yy or M/dd/yy
					if (dateDigits[0] == 1) {
						if (dateValue[0] == 0)
							return null;
						if (dateValue[1] > 12) {
							if (!plausibleDate(dateValue, dateDigits, new int[] {1,0,2}))
								return null;
							dateAnswer = retDigits(dateDigits[0], 'M') + dateSeparator + "dd" + dateSeparator + "yy";
						}
						else
							dateAnswer = dateFormat(dateDigits, dateSeparator, dayFirst, true);
					}
					// If year is the first field - then assume yy/MM/dd
					else if (dateValue[0] > 31)
						dateAnswer = "yy" + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + retDigits(dateDigits[2], 'd');
					else if (dateValue[2] > 31) {
						// Year is the last field - attempt to determine which is the month
						if (dateValue[0] > 12) {
							if (!plausibleDate(dateValue, dateDigits, new int[] {0,1,2}))
								return null;
							dateAnswer = "dd" + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + "yy";
						}
						else if (dateValue[1] > 12) {
							if (!plausibleDate(dateValue, dateDigits, new int[] {1,0,2}))
								return null;
							dateAnswer = retDigits(dateDigits[0], 'M') + dateSeparator + "dd" + dateSeparator + "yy";
						}
						else
							dateAnswer = dateFormat(dateDigits, dateSeparator, dayFirst, true);
					} else if (dateValue[1] > 12) {
						if (!plausibleDate(dateValue, dateDigits, new int[] {1,0,2}))
							return null;
						dateAnswer = retDigits(dateDigits[0], 'M') + dateSeparator + "dd" + dateSeparator + "yy";
					}
					else
						dateAnswer = dateFormat(dateDigits, dateSeparator, dayFirst, false);
				}
			}
			if (timeComponent == 0)
				return dateAnswer + timeZone;
		}

		if (timeFirst)
			return timeAnswer + (ISO8601 ? "'T'" : " ") + dateAnswer + timeZone;
		else
			return dateAnswer + (ISO8601 ? "'T'" : " ") + timeAnswer + timeZone;
	}
}
