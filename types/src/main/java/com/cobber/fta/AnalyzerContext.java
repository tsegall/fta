/*
 * Copyright 2017-2022 Tim Segall
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

	AnalyzerContext() {
	}

	public AnalyzerContext(final String streamName, final DateResolutionMode dateResolutionMode, final String compositeName, final String[] compositeStreamNames) {
		this.streamName = streamName == null ? "anonymous" : streamName;
		this.dateResolutionMode = dateResolutionMode == null ? DateResolutionMode.None : dateResolutionMode;
		this.compositeName = compositeName;
		if (compositeStreamNames == null)
			this.compositeStreamNames = null;
		else {
			this.compositeStreamNames = new String[compositeStreamNames.length];
			System.arraycopy(compositeStreamNames, 0, this.compositeStreamNames, 0, compositeStreamNames.length);
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
}
