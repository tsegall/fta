/*
 *  Copyright 2017-2025 Tim Segall
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

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.SortedSet;

/**
 * This set is a LinkedHashSet that implements the SortedSet interface.
 * We fundamentally want a set that is Sorted but the sort order is defined by the order of
 * insertion (i.e. the order is managed externally).
 * None of the core SortedSet functionality is implemented, however, you can run through the
 * elements in the correct order :-).
 */
public class PreSortedSet extends LinkedHashSet<String> implements SortedSet<String> {

	PreSortedSet(final Collection<?> c) {
		super();
		for (final Object o : c)
			this.add(o.toString());
	}

	@Override
	public String first() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String last() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Comparator<? super String> comparator() {
		return null;
	}

	@Override
	public SortedSet<String> subSet(final String fromElement, final String toElement) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SortedSet<String> headSet(final String toElement) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SortedSet<String> tailSet(final String fromElement) {
		throw new UnsupportedOperationException();
	}

	public SortedSet<String> reversed() {
		throw new UnsupportedOperationException();
	}
}
