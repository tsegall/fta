package com.cobber.fta;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class FiniteMap implements Map<String, Long> {
	private int maxCapacity;
	private Map<String, Long> impl;
	private boolean sorted = false;

	FiniteMap() {
		impl = new HashMap<>();
	}

	public FiniteMap(final int maxCapacity) {
		impl = new HashMap<>();
		this.maxCapacity = maxCapacity;
	}

    public FiniteMap(final Map<String, Long> m, final int maxCapacity) {
		impl = new HashMap<>(m);
		this.maxCapacity = maxCapacity;
    }

	public int getMaxCapacity() {
		return maxCapacity;
	}

	public void setMaxCapacity(final int maxCapacity) {
		this.maxCapacity = maxCapacity;
	}

	public void sortByValue() {
		impl = impl.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).
				collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	public void sortByKey(final NavigableMap<String, Long> newMap) {
		for (final Map.Entry<String, Long> entry : impl.entrySet())
			newMap.merge(entry.getKey(), entry.getValue(), Long::sum);

		impl = newMap;
		sorted = true;
	}

	public Map<String, Long> getImpl() {
		return impl;
	}

	public boolean isSorted() {
		return sorted;
	}

	public boolean mergeIfSpace(final String key, final Long value,
			final BiFunction<? super Long, ? super Long, ? extends Long> remappingFunction) {
		final Long oldValue = get(key);

		// If it is not already present and we are full then just return
		if (oldValue == null && impl.size() >= getMaxCapacity())
			return false;

		final Long newValue = (oldValue == null) ? value : remappingFunction.apply(oldValue, value);

		put(key, newValue);

        return true;
    }

	@Override
	public int size() {
		return impl.size();
	}

	@Override
	public boolean isEmpty() {
		return impl.isEmpty();
	}

	@Override
	public boolean containsKey(final Object key) {
		return impl.containsKey(key);
	}

	@Override
	public boolean containsValue(final Object value) {
		return impl.containsValue(value);
	}

	@Override
	public Long get(final Object key) {
		return impl.get(key);
	}

	@Override
	public Long put(final String key, final Long value) {
		return impl.put(key, value);
	}

	@Override
	public Long remove(final Object key) {
		return impl.remove(key);
	}

	@Override
	public void putAll(final Map<? extends String, ? extends Long> m) {
		impl.putAll(m);
	}

	@Override
	public void clear() {
		impl.clear();
	}

	@Override
	public Set<String> keySet() {
		return impl.keySet();
	}

	@Override
	public Collection<Long> values() {
		return impl.values();
	}

	@Override
	public Set<Entry<String, Long>> entrySet() {
		return impl.entrySet();
	}

	@Override
	public boolean equals(final Object o) {
		final FiniteMap other = (FiniteMap)o;
		return o != null && impl.equals(other.impl) && maxCapacity == other.maxCapacity;
	}
}
