/*
 * Copyright 2017-2021 Tim Segall
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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RandomSet<E> extends AbstractSet<E> {

	private final List<E> dta = new ArrayList<>();
	private final Map<E, Integer> idx = new HashMap<>();

	public RandomSet() {
		super();
	}

	public RandomSet(final Collection<E> items) {
		super();
		for (final E item : items) {
			idx.put(item, dta.size());
			dta.add(item);
		}
	}

	@Override
	public boolean add(final E item) {
		if (idx.containsKey(item)) {
			return false;
		}
		idx.put(item, dta.size());
		dta.add(item);
		return true;
	}

	/**
	 * Override element at position <code>id</code> with last element.
	 * @param id Index of element to remove
	 * @return Return the element removed
	 */
	public E removeAt(final int id) {
		if (id >= dta.size())
			return null;

		final E res = dta.get(id);
		idx.remove(res);
		final E last = dta.remove(dta.size() - 1);
		// skip filling the hole if last is removed
		if (id < dta.size()) {
			idx.put(last, id);
			dta.set(id, last);
		}
		return res;
	}

	@Override
	public boolean contains(final Object item) {
		return idx.get(item) != null;
	}

	@Override
	public boolean remove(final Object item) {
		final Integer id = idx.get(item);
		if (id == null)
			return false;

		removeAt(id);
		return true;
	}

	public E get(final int id) {
		return dta.get(id);
	}

	@Override
	public int size() {
		return dta.size();
	}

	@Override
	public Iterator<E> iterator() {
		return dta.iterator();
	}
}
