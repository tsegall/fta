/*
 * Copyright 2017-2024 Tim Segall
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
package com.cobber.fta.core;

/**
 * Tracing is thrown when we detect an issue with tracing.
 */
public class TraceException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public TraceException(final String message) {
		super("Tracing Error: " + message);
	}

	public TraceException(final String message, final Throwable cause) {
		super("Tracing Error: " + message, cause);
	}
}
