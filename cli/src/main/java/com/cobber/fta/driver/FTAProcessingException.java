/*
 * Copyright 2017-2023 Tim Segall
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
package com.cobber.fta.driver;

import com.cobber.fta.core.FTAException;

/**
 * FTAProcessingException is thrown when we have an fatal error processing a data stream.
 */
public class FTAProcessingException extends FTAException {
	private static final long serialVersionUID = 1L;
	private String filename;

	public FTAProcessingException(final String filename, final String message) {
		super(message);
		this.filename = filename;
	}

	public FTAProcessingException(final String filename, final String message, final Throwable cause) {
		super(message, cause);
		this.filename = filename;
	}

	public String getFilename() {
		return filename;
	}
}
