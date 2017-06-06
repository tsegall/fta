package com.cobber.fta;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DetermineDateTimeFormat {

	Map<String, Integer> results = new HashMap<String, Integer>();
	int sampleCount = 0;
	int nullCount = 0;
	int blankCount = 0;
	int invalidCount = 0;

	public String train(String input) {
		if (input == null) {
			nullCount++;
			return null;
		}

		String trimmed = input.trim();
		if (input.length() == 0) {
			blankCount++;
			return null;
		}

		String ret = intuitDateTimeFormat(trimmed);
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
		return map.entrySet().stream()
				.sorted(Map.Entry
						.comparingByValue())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	public String getResult() {
		if (results.size() == 1)
			return results.keySet().iterator().next();

		int timeElements = -1;
		int dateElements = -1;
		Boolean timeFirst = null;
		int dayOffset = -1;
		int monthOffset = -1;
		int yearOffset = -1;
		int yearLength = -1;
		Character dateSeparator = null;
		Character dateTimeSeparator = null;
		int located = 0;
		
		Map<String, Integer> byValue = sortByValue(results);

		for (Map.Entry<String, Integer> entry : byValue.entrySet()) {
			DateTimeAnalysisResult result = DateTimeAnalysisResult.toResult(entry.getKey());
			if (timeElements == -1)
				timeElements = result.timeElements;
			if (timeFirst == null)
				timeFirst = result.timeFirst;
			if (dateElements == -1)
				dateElements = result.dateElements;
			if (dayOffset == -1 && result.dayOffset != -1) {
				dayOffset = result.dayOffset;
				located++;
			}
			if (monthOffset == -1 && result.monthOffset != -1) {
				monthOffset = result.monthOffset;
				located++;
			}
			if (yearOffset == -1 && result.yearOffset != -1) {
				yearOffset = result.yearOffset;
				located++;
			}
			if (yearLength == -1)
				yearLength = result.yearLength;
			if (dateSeparator == null)
				dateSeparator = result.dateSeparator;
			if (dateTimeSeparator == null)
				dateTimeSeparator = result.dateTimeSeparator;
		}

		if (located == 2) {
			if (dayOffset == -1)
				dayOffset = 3 - monthOffset - yearOffset;
			else if (monthOffset == -1)
				monthOffset = 3 - dayOffset - yearOffset;
			else if (yearOffset == -1)
				yearOffset = 3 - dayOffset - monthOffset;
		}
		return new DateTimeAnalysisResult(timeElements, dateElements, timeFirst, dateTimeSeparator, yearLength,
				yearOffset, monthOffset, dayOffset, dateSeparator).toString();
	}

	public static String intuitDateTimeFormat(String input) {
		int len = input.length();

		// Fail fast if we can
		if (len < 4 || !Character.isDigit(input.charAt(0)))
			return null;

		int digits = 0;
		int value = 0;
		int[] dateField = new int[3];
		char dateSeparator = '_';
		int dateComponent = 0;
		int timeComponent = 0;
		boolean timeFirst = false;
		boolean yearInDateFirst = false;
		boolean fourDigitYear = false;
		boolean timeSeen = false;
		boolean timeClosed = false;
		boolean dateSeen = false;
		boolean dateClosed = false;
		boolean timeZoneExit = false;

		for (int i = 0; i < len && !timeZoneExit; i++) {
			char ch = input.charAt(i);
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
				break;
			case ':':
				timeFirst = dateComponent == 0;
				timeSeen = true;
				if (timeComponent == 0 && digits != 1 && digits != 2)
					return null;
				if (timeComponent == 1 && digits != 2)
					return null;
				if (timeComponent == 2)
					return null;
				timeComponent++;
				digits = 0;
				break;
			case '/':
			case '-':
				dateSeen = true;
				if (dateComponent == 0) {
					dateField[dateComponent] = value;
					dateSeparator = ch;
					yearInDateFirst = fourDigitYear = digits == 4;
					if (!yearInDateFirst && digits != 1 && digits != 2)
						return null;
				} else if (dateComponent == 1) {
					if (ch != dateSeparator)
						return null;
					if (digits != 1 && digits != 2)
						return null;
					dateField[dateComponent] = value;
				} else if (dateComponent == 2)
					return null;
				dateComponent++;
				digits = 0;
				value = 0;
				break;

			case ' ':
				if (timeSeen && !timeClosed) {
					if (digits != 2)
						return null;
					timeClosed = true;
				}
				if (dateSeen && !dateClosed) {
					if (digits != 2 && (yearInDateFirst == false && digits != 4))
						return null;
					fourDigitYear = digits == 4;
					dateField[dateComponent] = value;
					dateClosed = true;
				}
				digits = 0;
				value = 0;
				if (timeSeen && dateSeen)
					timeZoneExit = true;
				break;
			}
		}

		if (dateSeen && !dateClosed) {
			// Need to close out the date
			if (digits != 2 && (yearInDateFirst == false && digits != 4))
				return null;
			fourDigitYear = digits == 4;
			dateField[dateComponent] = value;
		}
		if (timeSeen && !timeClosed) {
			// Need to close out the time
			if (digits != 2)
				return null;
		}

		String timeAnswer = null;
		if (timeComponent != 0) {
			timeAnswer = timeComponent == 1 ? "HH:MM" : "HH:MM:SS";
			if (dateComponent == 0)
				return timeAnswer;
		}

		String timeZone = timeZoneExit ? " z" : "";
		String dateAnswer = null;
		// Support YYYY/MM/DD, DD/MM/YYYY, MM/DD/YYYY, DD/MM/YYYY, DD/MM/YY,
		// MM/DD/YY
		if (dateComponent != 0) {
			if (dateComponent == 1)
				return null;
			if (yearInDateFirst)
				dateAnswer = "YYYY" + dateSeparator + "MM" + dateSeparator + "DD";
			else {
				if (fourDigitYear) {
					// Year is the last field - attempt to determine which is
					// the month
					if (dateField[0] > 12)
						dateAnswer = "DD" + dateSeparator + "MM" + dateSeparator + "YYYY";
					else if (dateField[1] > 12)
						dateAnswer = "MM" + dateSeparator + "DD" + dateSeparator + "YYYY";
					else
						dateAnswer = "XX" + dateSeparator + "XX" + dateSeparator + "YYYY";
				} else {
					// If year is the first field - then assume YY/MM/DD
					if (dateField[0] > 31)
						dateAnswer = "YY" + dateSeparator + "MM" + dateSeparator + "DD";
					else if (dateField[2] > 31) {
						// Year is the last field - attempt to determine which
						// is the month
						if (dateField[0] > 12)
							dateAnswer = "DD" + dateSeparator + "MM" + dateSeparator + "YY";
						else if (dateField[1] > 12)
							dateAnswer = "MM" + dateSeparator + "DD" + dateSeparator + "YY";
						else
							dateAnswer = "XX" + dateSeparator + "XX" + dateSeparator + "YY";
					} else if (dateField[1] > 12)
						dateAnswer = "XX" + dateSeparator + "DD" + dateSeparator + "XX";
					else
						dateAnswer = "XX" + dateSeparator + "XX" + dateSeparator + "XX";
				}
			}
			if (timeComponent == 0)
				return dateAnswer + timeZone;
		}

		if (timeFirst)
			return timeAnswer + " " + dateAnswer + timeZone;
		else
			return dateAnswer + " " + timeAnswer + timeZone;
	}
}
