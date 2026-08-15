package com.micrantha.eyespie.features.things.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.micrantha.bluebell.ui.components.longDateTime
import com.micrantha.bluebell.ui.theme.Dimensions
import com.micrantha.eyespie.domain.entities.Thing
import kotlin.time.ExperimentalTime

/**
 * Game/nearby listings intentionally contain no target image or authority-only metadata.
 * The challenge image is not a guesser-facing capability.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun ThingListingCard(
    modifier: Modifier = Modifier,
    thing: Thing.Listing,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.content)
        ) {
            Text(text = longDateTime(thing.createdAt))
        }
    }
}
