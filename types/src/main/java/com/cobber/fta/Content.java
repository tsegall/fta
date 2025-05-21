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

/**
 * Used to capture the members for a set of items - defined either inline or via a file or resource.
 */
public class Content {
	/** type describes the supplied content and must be one of 'inline', 'file' or 'resource'. */
	public String type;
	/** If type is inline - then this is the set of members allowed. */
	public String[] members;
	/** If type is 'file' or 'resource' then this provides the name of the file/resource. */
	public String reference;

	private static final String INLINE = "inline";

	public Content() {
	}

	public Content(final String type, final String reference) {
		this.type = type;
		this.reference = reference;
	}

	public Content(final String[] members) {
		this.type = INLINE;
		this.members = members;
	}

	public String getCacheKey() {
		if (INLINE.equals(type))
			return type + "---" + Arrays.hashCode(members);

		return type + "---" +  reference;
	}

	@Override
	public String toString() {
		return getCacheKey();
	}
}
