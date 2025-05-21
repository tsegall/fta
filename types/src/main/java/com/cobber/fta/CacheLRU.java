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

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class CacheLRU<K, V> {
    private final Cache<K, V> cache;

    public CacheLRU(final int capacity) {
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(capacity)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();
    }

    public void put(final K key, final V value) {
        cache.put(key, value);
    }

    public V get(K key) {
        return cache.getIfPresent(key);
    }

    public void invalidate(final K key) {
        cache.invalidate(key);
    }

    public long size() {
        return cache.size();
    }
}
