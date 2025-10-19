package com.micrantha.bluebell.ext


inline fun <A, B, R> let(a: A?, b: B?, block: (A, B) -> R): R? =
    if (a != null && b != null) block(a, b) else null

inline fun <K, V, R> Map<K, V>?.getIf(key: K?, block: (V?) -> R): R? =
    key?.let { k -> block(this?.get(k)) }

fun <K, V> Map<K, V>?.getIf(key: K?): V? =
    key?.let { this?.get(it) }

fun <K, V> getIf(map: Map<K, V>?, key: K?): Pair<K, V>? =
    key?.let { k ->
        map?.get(k)?.let { value -> Pair(k, value) }
    }
