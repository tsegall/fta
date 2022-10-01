package com.cobber.fta;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class FiniteMap implements Map<String, Long> {
	private int maxCapacity;
	private Map<String, Long> impl;

	FiniteMap() {
		impl = new HashMap<>();
	}

	FiniteMap(final int maxCapacity) {
		impl = new HashMap<>();
		this.maxCapacity = maxCapacity;
	}

    public FiniteMap(Map<String, Long> m, final int maxCapacity) {
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

	public boolean mergeIfSpace(String key, Long value,
			BiFunction<? super Long, ? super Long, ? extends Long> remappingFunction) {
		final Long oldValue = get(key);

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
	public boolean containsKey(Object key) {
		return impl.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return impl.containsValue(value);
	}

	@Override
	public Long get(Object key) {
		return impl.get(key);
	}

	@Override
	public Long put(String key, Long value) {
		return impl.put(key, value);
	}

	@Override
	public Long remove(Object key) {
		return impl.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Long> m) {
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

	public boolean equals(FiniteMap other) {
		return impl.equals(other.impl) && maxCapacity == other.maxCapacity;
	}
}
