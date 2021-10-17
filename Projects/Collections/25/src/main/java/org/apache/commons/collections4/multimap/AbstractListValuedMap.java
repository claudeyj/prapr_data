/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.collections4.multimap;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.ListValuedMap;

/**
 * Abstract implementation of the {@link ListValuedMap} interface to simplify
 * the creation of subclass implementations.
 * <p>
 * Subclasses specify a Map implementation to use as the internal storage and
 * the List implementation to use as values.
 *
 * @since 4.1
 * @version $Id$
 */
public abstract class AbstractListValuedMap<K, V> extends AbstractMultiValuedMap<K, V>
        implements ListValuedMap<K, V> {

    /** The serialization version */
    private static final long serialVersionUID = 6024950625989666915L;

    /**
     * A constructor that wraps, not copies
     *
     * @param <C> the list type
     * @param map the map to wrap, must not be null
     * @param listClazz the collection class
     * @throws NullPointerException if the map is null
     */
    protected <C extends List<V>> AbstractListValuedMap(final Map<K, ? super C> map, Class<C> listClazz) {
        super(map, listClazz);
    }

    /**
     * A constructor that wraps, not copies
     *
     * @param <C> the list type
     * @param map the map to wrap, must not be null
     * @param listClazz the collection class
     * @param initialListCapacity the initial size of the values list
     * @throws NullPointerException if the map is null
     * @throws IllegalArgumentException if initialListCapacity is negative
     */
    protected <C extends List<V>> AbstractListValuedMap(final Map<K, ? super C> map, Class<C> listClazz,
                                                        final int initialListCapacity) {
        super(map, listClazz, initialListCapacity);
    }

    /**
     * Gets the list of values associated with the specified key. This would
     * return an empty list in case the mapping is not present
     *
     * @param key the key to retrieve
     * @return the <code>List</code> of values, will return an empty {@link List} for no mapping
     * @throws ClassCastException if the key is of an invalid type
     */
    @Override
    public List<V> get(Object key) {
        return new WrappedList(key);
    }

    /**
     * Removes all values associated with the specified key.
     * <p>
     * A subsequent <code>get(Object)</code> would return an empty list.
     *
     * @param key the key to remove values from
     * @return the <code>List</code> of values removed, will return an empty,
     *         unmodifiable list for no mapping found.
     * @throws ClassCastException if the key is of an invalid type
     */
    @Override
    public List<V> remove(Object key) {
        return ListUtils.emptyIfNull((List<V>) getMap().remove(key));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof ListValuedMap == false) {
            return false;
        }
        ListValuedMap<?, ?> other = (ListValuedMap<?, ?>) obj;
        if (other.size() != size()) {
            return false;
        }
        Iterator<?> it = keySet().iterator();
        while (it.hasNext()) {
            Object key = it.next();
            List<?> list = get(key);
            List<?> otherList = other.get(key);
            if (otherList == null) {
                return false;
            }
            if (ListUtils.isEqualList(list, otherList) == false) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 0;
        Iterator<Entry<K, Collection<V>>> it = getMap().entrySet().iterator();
        while (it.hasNext()) {
            Entry<K, Collection<V>> entry = it.next();
            K key = entry.getKey();
            List<V> valueList = (List<V>) entry.getValue();
            h += (key == null ? 0 : key.hashCode()) ^ ListUtils.hashCodeForList(valueList);
        }
        return h;
    }

    /**
     * Wrapped list to handle add and remove on the list returned by get(object)
     */
    private class WrappedList extends WrappedCollection implements List<V> {

        public WrappedList(Object key) {
            super(key);
        }

        @SuppressWarnings("unchecked")
        public void add(int index, V value) {
            List<V> list = (List<V>) getMapping();
            if (list == null) {
                list = (List<V>) AbstractListValuedMap.this.createCollection();
                list.add(index, value);
                getMap().put((K) key, list);
            }
            list.add(index, value);
        }

        @SuppressWarnings("unchecked")
        public boolean addAll(int index, Collection<? extends V> c) {
            List<V> list = (List<V>) getMapping();
            if (list == null) {
                list = (List<V>) createCollection();
                boolean result = list.addAll(index, c);
                if (result) {
                    getMap().put((K) key, list);
                }
                return result;
            }
            return list.addAll(index, c);
        }

        public V get(int index) {
            final List<V> list = ListUtils.emptyIfNull((List<V>) getMapping());
            return list.get(index);
        }

        public int indexOf(Object o) {
            final List<V> list = ListUtils.emptyIfNull((List<V>) getMapping());
            return list.indexOf(o);
        }

        public int lastIndexOf(Object o) {
            final List<V> list = ListUtils.emptyIfNull((List<V>) getMapping());
            return list.indexOf(o);
        }

        public ListIterator<V> listIterator() {
            return new ValuesListIterator(key);
        }

        public ListIterator<V> listIterator(int index) {
            return new ValuesListIterator(key, index);
        }

        public V remove(int index) {
            final List<V> list = ListUtils.emptyIfNull((List<V>) getMapping());
            V value = list.remove(index);
            if (list.isEmpty()) {
                AbstractListValuedMap.this.remove(key);
            }
            return value;
        }

        public V set(int index, V value) {
            final List<V> list = ListUtils.emptyIfNull((List<V>) getMapping());
            return list.set(index, value);
        }

        public List<V> subList(int fromIndex, int toIndex) {
            final List<V> list = ListUtils.emptyIfNull((List<V>) getMapping());
            return list.subList(fromIndex, toIndex);
        }

        @Override
        public boolean equals(Object other) {
            final List<V> list = (List<V>) getMapping();
            if (list == null) {
                return Collections.emptyList().equals(other);
            }
            if (other == null) {
                return false;
            }
            if (!(other instanceof List)) {
                return false;
            }
            List<?> otherList = (List<?>) other;
            if (ListUtils.isEqualList(list, otherList) == false) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            final List<V> list = (List<V>) getMapping();
            if (list == null) {
                return Collections.emptyList().hashCode();
            }
            return ListUtils.hashCodeForList(list);
        }

    }

    /** Values ListItrerator */
    private class ValuesListIterator implements ListIterator<V>{

        private final Object key;

        private List<V> values;
        private ListIterator<V> iterator;

        public ValuesListIterator(Object key){
            this.key = key;
            this.values = ListUtils.emptyIfNull((List<V>) getMap().get(key));
            this.iterator = values.listIterator();
        }

        public ValuesListIterator(Object key, int index){
            this.key = key;
            this.values = ListUtils.emptyIfNull((List<V>) getMap().get(key));
            this.iterator = values.listIterator(index);
        }

        @SuppressWarnings("unchecked")
        public void add(V value) {
            if (getMap().get(key) == null) {
                List<V> list = (List<V>) createCollection();
                getMap().put((K) key, list);
                this.values = list;
                this.iterator = list.listIterator();
            }
        this.iterator.add(value);
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public boolean hasPrevious() {
            return iterator.hasPrevious();
        }

        public V next() {
            return iterator.next();
        }

        public int nextIndex() {
            return iterator.nextIndex();
        }

        public V previous() {
            return iterator.previous();
        }

        public int previousIndex() {
            return iterator.previousIndex();
        }

        public void remove() {
            iterator.remove();
            if (values.isEmpty()) {
                getMap().remove(key);
            }
        }

        public void set(V value) {
            iterator.set(value);
        }

    }

}
