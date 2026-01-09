package com.micrantha.bluebell.domain

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class MutexMap<K, V> {
    protected val map = mutableMapOf<K, V>()
    protected val mutex = Mutex()

    suspend operator fun get(key: K): V? = mutex.withLock {
        map[key]
    }

    suspend fun values(): List<V> = mutex.withLock {
        map.values.toList()
    }
    
    suspend fun keys(): Set<K> = mutex.withLock {
        map.keys.toSet()
    }
    
    suspend fun size(): Int = mutex.withLock {
        map.size
    }
    
    suspend fun clear() = mutex.withLock {
        map.clear()
    }
    
    suspend fun isEmpty(): Boolean = mutex.withLock {
        map.isEmpty()
    }
    
    suspend fun containsKey(key: K): Boolean = mutex.withLock {
        map.containsKey(key)
    }

    suspend fun toMap(): Map<K, V> = mutex.withLock { map }
}

class ThreadSafeMap<K, V> : MutexMap<K, V>()

class MutableThreadSafeMap<K, V> : MutexMap<K, V>() {

    suspend fun putAll(other: Map<K, V>) = mutex.withLock {
        map.putAll(other)
    }

    suspend fun remove(key: K): V? = mutex.withLock {
        map.remove(key)
    }

    suspend fun put(key: K, value: V) = mutex.withLock {
        map[key] = value
    }


    suspend operator fun set(key: K, value: V) = mutex.withLock {
        map[key] = value
    }

    suspend fun getOrPut(key: K, defaultValue: () -> V): V = mutex.withLock {
        map.getOrPut(key, defaultValue)
    }

    suspend fun putIfAbsent(key: K, value: V): V? = mutex.withLock {
        if (map.containsKey(key).not()) {
            map.put(key, value)
        } else null
    }
}
