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

public class CircularBuffer {
	final int depth;
	final String[][] buffer;
	int insert = 0;
	int retrieve = 0;
	int records = 0;

	public CircularBuffer(final int depth) {
		this.depth = depth;
		this.buffer = new String[depth][];
	}

	public boolean add(final String[] record) {
		if (records == depth)
			return false;
		this.buffer[insert] = record;
		insert = insert == depth - 1 ? 0 : insert + 1;
		records++;
		return true;
	}

	public String[] get() {
		if (records == 0)
			return null;
		final String[] ret = this.buffer[retrieve];
		records--;
		retrieve = retrieve == depth - 1 ? 0 : retrieve + 1;
		return ret;
	}

	public boolean isFull() {
		return records == depth;
	}
}