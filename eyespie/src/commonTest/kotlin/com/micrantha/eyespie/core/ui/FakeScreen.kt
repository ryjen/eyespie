package com.micrantha.eyespie.core.ui

import androidx.compose.runtime.Composable

class FakeScreen(val name: String) : Screen {
    @Composable
    override fun Content() = Unit
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FakeScreen) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}
