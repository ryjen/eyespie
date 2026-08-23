package com.micrantha.eyespie.features.utility

import com.micrantha.eyespie.game.LocalGameSnapshot

object UtilityMapper {
    fun map(snapshot: LocalGameSnapshot): UtilityContent = UtilityContent(
        identityDisplayName = snapshot.identity.displayName,
        identityIdSuffix = snapshot.identity.id.value.takeLast(12),
    )
}
