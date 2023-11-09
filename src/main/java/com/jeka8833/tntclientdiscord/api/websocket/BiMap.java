package com.jeka8833.tntclientdiscord.api.websocket;

import java.util.HashMap;
import java.util.Map;

public class BiMap<K, V> {

    private final Map<K, V> keyToValueMap = new HashMap<>();
    private final Map<V, K> valueToKeyMap = new HashMap<>();

    synchronized public void put(K key, V value) {
        keyToValueMap.put(key, value);
        valueToKeyMap.put(value, key);
    }

    public K getKey(V value) {
        return valueToKeyMap.get(value);
    }

    public V get(K key) {
        return keyToValueMap.get(key);
    }

}
