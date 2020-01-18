/*
 * Copyright 2017-2020 Tim Segall
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

import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Class used to track the top and bottom K values.
 *
 * @param <T> The type of the object being tracked.
 * @param <C> The Class of a comparator used to compare objects of this type.
 */
public class TopBottomK<T extends Comparable<C>, C> {
	private static final int DEFAULT_SIZE = 10;
	private int size;
	private TreeSet<T> starter;
	private TreeSet<T> top;
	private TreeSet<T> bottom;
	boolean split = false;

	TopBottomK() {
		this(DEFAULT_SIZE);
	}

	TopBottomK(int size) {
		if (size <= 0)
			throw new IllegalArgumentException("size must be > 0");
		this.size = size;
		this.starter = new TreeSet<>();
	}

	/**
	 * Observe the value provided.
	 * @param item The item to be observed.
	 */
	public void observe(T item) {
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
			if (item.compareTo((C) top.first()) > 0) {
				if (!top.contains(item)) {
					top.add(item);
					top.pollFirst();
				}
			}
			else if (item.compareTo((C) bottom.last()) < 0) {
				if (!bottom.contains(item)) {
					bottom.add(item);
					bottom.pollLast();
				}
			}
		}
	}

	/**
	 * Return the top K values (ordered lowest to highest).
	 * @return The top K values.
	 */
	public SortedSet<T> topK() {
		// If the set has been split into top and bottom - we have the answer
		if (split)
			return top;

		// If there are no more than size elements then we have the answer
		if (starter.size() <= size)
			return starter;

		Iterator<T> iter = starter.descendingIterator();
		for (int i = 0; i < size - 1; i++)
			iter.next();
		T splitter = iter.next();
		return starter.tailSet(splitter);
	}

	/**
	 * Return the bottom K values (ordered lowest to highest).
	 * @return The bottom K values.
	 */
	public SortedSet<T> bottomK() {
		// If the set has been split into top and bottom - we have the answer
		if (split)
			return bottom;

		// If there are no more than size elements then we have the answer
		if (size >= starter.size())
			return starter;

		Iterator<T> iter = starter.iterator();
		for (int i = 0; i < size; i++)
			iter.next();
		T splitter = iter.next();
		return starter.headSet(splitter);
	}

	/**
	 * Return the top K values as Strings (ordered highest to lowest).
	 * @return The top K values as Strings.
	 */
	public SortedSet<String> topKasString() {
		TreeSet<String> ret = new TreeSet<>(Collections.reverseOrder());
		ret.addAll(topK().stream().map(x->x.toString()).collect(Collectors.toSet()));

		return ret;
	}

	/**
	 * Return the bottom K values as Strings (ordered lowest to highest).
	 * @return The bottom K values as Strings.
	 */
	public SortedSet<String> bottomKasString() {
		return new TreeSet<>(bottomK().stream().map(x->x.toString()).collect(Collectors.toSet()));
	}
}
