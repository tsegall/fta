package com.cobber.fta;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RandomSet<E> extends AbstractSet<E> {

    List<E> dta = new ArrayList<E>();
    Map<E, Integer> idx = new HashMap<E, Integer>();

    public RandomSet() {
    }

    public RandomSet(Collection<E> items) {
        for (E item : items) {
            idx.put(item, dta.size());
            dta.add(item);
        }
    }

    @Override
    public boolean add(E item) {
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
    public E removeAt(int id) {
        if (id >= dta.size()) {
            return null;
        }
        E res = dta.get(id);
        idx.remove(res);
        E last = dta.remove(dta.size() - 1);
        // skip filling the hole if last is removed
        if (id < dta.size()) {
            idx.put(last, id);
            dta.set(id, last);
        }
        return res;
    }

    @Override
	public boolean contains(Object item) {
        return idx.get(item) != null;
    }

    @Override
    public boolean remove(Object item) {
        Integer id = idx.get(item);
        if (id == null) {
            return false;
        }
        removeAt(id);
        return true;
    }

    public E get(int id) {
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