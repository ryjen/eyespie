package com.micrantha.bluebell.domain

class StateMap<K, V>(
    private val map: MutableMap<K, V> = mutableMapOf()
) : Map<K, V> by map {

    fun copy(key: K, default: (() -> V)? = null, update: (V) -> V): StateMap<K, V> {
        this.map[key]?.let {
            this.map[key] = update(it)
        } ?: default?.let {
            this.map[key] = it()
        }
        return stateMapOf(this.map)
    }
}

fun <K, V> stateMapOf(vararg values: Pair<K, V>) = StateMap(mutableMapOf(*values))
fun <K, V> stateMapOf(values: Map<K, V>) = StateMap(values.toMutableMap())
