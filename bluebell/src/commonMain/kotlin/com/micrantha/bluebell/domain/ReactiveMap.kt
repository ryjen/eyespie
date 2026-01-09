package com.micrantha.bluebell.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class ReactiveMap<K, V> {
    private val _map = MutableStateFlow<Map<K, V>>(emptyMap())
    val map: StateFlow<Map<K, V>> = _map.asStateFlow()
    
    fun put(key: K, value: V) {
        _map.update { currentMap ->
            currentMap + (key to value)
        }
    }
    
    fun get(key: K): V? = _map.value[key]
    
    fun remove(key: K) {
        _map.update { currentMap ->
            currentMap - key
        }
    }
    
    // Observe changes
    fun observeKey(key: K): Flow<V?> = map.map { it[key] }.distinctUntilChanged()
}
