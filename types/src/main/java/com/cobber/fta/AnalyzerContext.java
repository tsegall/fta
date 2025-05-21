/*
 * Copyright 2017-2025 Tim Segall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cobber.fta;

import java.util.Arrays;
import java.util.Objects;

import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

/**
 * Definition of the Context for running an Analysis.
 */
public class AnalyzerContext {

	/** The name of the data stream (e.g. the column of the CSV file) */
	private String streamName;
	/** Determines what to do when the Date field is ambiguous (i.e. we cannot determine which of the fields is the day or the month. */
	private DateResolutionMode dateResolutionMode;
	/** The name of the composite of which this stream is part of, for example the table name, if we are processing a column. */
	private String compositeName;
	/** The name of all of the members of the composite (including this stream), for example the column names if we are processing a column of a table. */
	private String[] compositeStreamNames;
	/** The index of this stream name in the list of all field names - or -1 if it is not present (or no column names provided). */
	private int streamIndex;
	/** The 'known' answer for the Semantic Type - typically based on user input or a prior run. */
	private String[] semanticTypes;

	private boolean nested;

	public AnalyzerContext() {
	}

	public AnalyzerContext(final String streamName, final DateResolutionMode dateResolutionMode, final String compositeName, final String[] compositeStreamNames) {
		this.streamName = streamName == null ? "anonymous" : streamName;
		this.dateResolutionMode = dateResolutionMode == null ? DateResolutionMode.None : dateResolutionMode;
		this.compositeName = compositeName;
		if (compositeStreamNames == null) {
			this.compositeStreamNames = null;
			this.streamIndex = -1;
		}
		else {
			this.compositeStreamNames = new String[compositeStreamNames.length];
			System.arraycopy(compositeStreamNames, 0, this.compositeStreamNames, 0, compositeStreamNames.length);
			for (int i = 0; i < compositeStreamNames.length; i++) {
				if (compositeStreamNames[i] == null)
					compositeStreamNames[i] = "";
				else if (this.streamName.equals(compositeStreamNames[i].trim())) {
					this.streamIndex = i;
					break;
				}
			}
		}
	}

	/**
	 * Retrieve the stream name from the Context.
	 * @return The String name of this data stream.
	 */
	public String getStreamName() {
		return streamName;
	}

	/**
	 * Retrieve the index of the stream name from the list of all stream names.
	 * @return The index of this data stream (or -1 if not found).
	 */
	public int getStreamIndex() {
		return streamIndex;
	}

	/**
	 * Retrieve the DateResolutionMode from the Context.
	 * @return The DateResolution mode of this analysis.
	 */
	public DateResolutionMode getDateResolutionMode() {
		return dateResolutionMode;
	}

	/**
	 * Set the DateResolutionMode on the Context.
	 * @param dateResolutionMode The new DateResolutionMode.
	 */
	public void setDateResolutionMode(final DateResolutionMode dateResolutionMode) {
		this.dateResolutionMode = dateResolutionMode;
	}

	/**
	 * Retrieve the composite name (the enclosing object - for example, table or filename) from the Context.
	 * @return The String name of this composite stream.
	 */
	public String getCompositeName() {
		return compositeName;
	}

	/**
	 * Retrieve the names of all the fields on the enclosing object (for example, table columns or file headers) from the Context.
	 * @return The String array with the names of all the elements on this composite.
	 */
	public String[] getCompositeStreamNames() {
		return compositeStreamNames;
	}

	/**
	 * Set the 'known' Semantic Type for each field.
	 * @param semanticTypes The 'known' set of Semantic Types - null implies unknown.
	 * @return The AnalyzerContext
	 */
	public AnalyzerContext withSemanticTypes(final String[] semanticTypes) {
		this.semanticTypes = semanticTypes;
		return this;
	}

	/**
	 * Retrieve the Semantic Types of all the streams.
	 * @return The String array with the Semantic Type or null if not known.
	 */
	public String[] getSemanticTypes() {
		return semanticTypes;
	}

	/*
	 * Check whether any of the supplied Semantic Types is the type of the supplied field Index.
	 * @param searching The list of Semantic Types to check.
	 * @return True if the type of the supplied field exists and matches an item from the list.
	 */
	public boolean isSemanticType(final int fieldIndex, final String... searching) {
		for (final String semanticType : searching) {
			if (semanticType.equals(semanticTypes[fieldIndex]))
				return true;
		}

		return false;
	}

	/*
	 * Check whether any of the supplied Semantic Types is the type subsequent to this one.
	 * @param searching The list of Semantic Types to check.
	 * @return True if the subsequent type exists and matches an item from the list.
	 */
	public boolean isNextSemanticType(final String... searching) {
		if (semanticTypes == null)
			return false;

		final int current = getStreamIndex();
		if (current == -1 || current == semanticTypes.length - 1)
			return false;

		return isSemanticType(current + 1, searching);
	}

	/*
	 * Check whether any of the supplied Semantic Types is the type prior to this one.
	 * @param searching The list of Semantic Types to check.
	 * @return True if the prior type exists and matches an item from the list.
	 */
	public boolean isPreviousSemanticType(final String... searching) {
		if (semanticTypes == null)
			return false;

		final int current = getStreamIndex();
		if (current == -1 || current == 0)
			return false;

		return isSemanticType(current - 1, searching);
	}

	/*
	 * Check whether any of the supplied Semantic Types exist on this record.
	 * @param searching The Semantic Types to check.
	 * @return True if any of the Semantic Types exists on this record.
	 */
	public boolean existsSemanticType(final String... searching) {
		if (semanticTypes == null)
			return false;

		for (final String semanticType : searching) {
			for (final String currentType : semanticTypes) {
				if (semanticType.equals(currentType))
					return true;
			}
		}

		return false;
	}

	/*
	 * Find the relative index of the closest Semantic Type from the list.
	 * @param focus the focus point of the search
	 * @param searching The Semantic Types to check.
	 * @return The index of the closest supplied Semantic Type or Null if not found.
	 */
	public Integer indexOfSemanticType(final int focus, final String... searching) {
		if (semanticTypes == null)
			return null;

		if (focus == -1 || focus == 0)
			return null;

		Integer closest = null;

		for (final String semanticType : searching) {
			for (int i = 0; i < semanticTypes.length; i++) {
				if (i != focus && semanticType.equals(semanticTypes[i]))
					if (closest == null || Math.abs(closest) > Math.abs(i - focus))
						closest = i - focus;
			}
		}

		return closest;
	}

	public boolean isNested() {
		return nested;
	}

	/**
	 * setNested() is invoked when we are doing a reAnalyze() to indicated that we are doing a nested analysis.
	 */
	public void setNested() {
		this.nested = true;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final AnalyzerContext other = (AnalyzerContext) obj;
		return Objects.equals(compositeName, other.compositeName)
				&& Arrays.equals(compositeStreamNames, other.compositeStreamNames)
				&& dateResolutionMode == other.dateResolutionMode && Objects.equals(streamName, other.streamName);
	}
}
