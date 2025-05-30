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

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class used to track the top and bottom K values.
 *
 * @param <T> The type of the object being tracked.
 * @param <C> The Class of a comparator used to compare objects of this type.
 */
public class TopBottomK<T extends Comparable<? super T>, C> {
	private int size;
	private NavigableSet<T> starter;
	private NavigableSet<T> top;
	private NavigableSet<T> bottom;
	private boolean split;

	TopBottomK() {
	}

	TopBottomK(final int size) {
		if (size <= 0)
			throw new IllegalArgumentException("size must be > 0");
		this.size = size;
		this.starter = new TreeSet<>();
	}

	/**
	 * Observe the items provided.
	 * @param items The set of items to be observed.
	 */
	public void observeAll(final Set<T> items) {
		items.forEach(item -> observe(item));
	}

	/**
	 * Observe the value provided.
	 * @param item The item to be observed.
	 */
	public void observe(final T item) {
		if (!split) {
			if (starter.size() < 2 * size)
				starter.add(item);
			if (starter.size() == 2 * size) {
				bottom = new TreeSet<>();
				for (int i = 0; i < size; i++)
					bottom.add(starter.pollFirst());
				top = new TreeSet<>();
				top.addAll(starter);
				split = true;
			}
		}
		else {
			if (item.compareTo(top.first()) > 0) {
				if (!top.contains(item)) {
					top.add(item);
					top.pollFirst();
				}
			}
			else if (item.compareTo(bottom.last()) < 0) {
				if (!bottom.contains(item)) {
					bottom.add(item);
					bottom.pollLast();
				}
			}
		}
	}

	/**
	 * Return the top K values (ordered LOWEST to HIGHEST).
	 * @return The top K values.
	 */
	public SortedSet<T> topK() {
		// If the set has been split into top and bottom - we have the answer
		if (split)
			return top;

		// If there are no more than size elements then we have the answer
		if (starter.size() <= size)
			return starter;

		final Iterator<T> iter = starter.descendingIterator();
		for (int i = 0; i < size - 1; i++)
			iter.next();
		final T splitter = iter.next();
		return starter.tailSet(splitter);
	}

	/**
	 * Return the bottom K values (ordered LOWEST to HIGHEST).
	 * @return The bottom K values.
	 */
	public SortedSet<T> bottomK() {
		// If the set has been split into top and bottom - we have the answer
		if (split)
			return bottom;

		// If there are no more than size elements then we have the answer
		if (size >= starter.size())
			return starter;

		final Iterator<T> iter = starter.iterator();
		for (int i = 0; i < size; i++)
			iter.next();
		final T splitter = iter.next();
		return starter.headSet(splitter);
	}

	/**
	 * Return the top K values as Strings (ordered HIGHEST to LOWEST).
	 * @return The top K values as Strings.
	 */
	public SortedSet<String> topKasString() {
		return new PreSortedSet(new TreeSet<>(topK()).descendingSet());
	}

	/**
	 * Return the bottom K values as Strings (ordered LOWEST to HIGHEST).
	 * @return The bottom K values as Strings.
	 */
	public SortedSet<String> bottomKasString() {
		return new PreSortedSet(bottomK());
	}
}
