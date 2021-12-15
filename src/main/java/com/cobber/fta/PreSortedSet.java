/*
 *  Copyright 2017-2021 Tim Segall
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
 * This set is basically a LinkedHashSet that implements the SortedSet interface.
 * We fundamentally want a set that is Sorted but the sort order is defined by the order of
 * insertion (i.e. the order is managed externally).
 * None of the core SortedSet functionality is implemented, however, you can run through the
 * elements in the correct order :-).
 */
public class PreSortedSet extends LinkedHashSet<String> implements SortedSet<String> {

	PreSortedSet(Collection<?> c) {
		for (Object o : c)
			this.add(o.toString());
	}

	@Override
	public String first() {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public String last() {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public Comparator<? super String> comparator() {
		return null;
	}

	@Override
	public SortedSet<String> subSet(String fromElement, String toElement) {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public SortedSet<String> headSet(String toElement) {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public SortedSet<String> tailSet(String fromElement) {
		throw new java.lang.UnsupportedOperationException();
	}
}
