package com.micrantha.eyespie.features.scan.ui.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import com.micrantha.eyespie.core.PreviewContext
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.features.scan.entities.ScanEditParams
import com.micrantha.eyespie.features.scan.entities.ScanEditUiState
import okio.Path.Companion.toPath

@Preview(showBackground = true, backgroundColor = 0xFF, widthDp = 200, heightDp = 400)
@Composable
fun ScanEditPreview() = PreviewContext(
    state = ScanEditUiState(
        image = object : Painter() {
            override val intrinsicSize = Size(350F, 350F)
            override fun DrawScope.onDraw() {
                drawRect(Color.Yellow)
            }
        },
        enabled = true,
        isBusy = false,
        clues = listOf(
            ScanEditUiState.Clue(
                answer = "An apple",
                clue = "I spy something that is round and red.",
                isSelected = false
            ),
            ScanEditUiState.Clue(
                answer = "A tree",
                clue = "I spy something that is tall and green.",
                isSelected = false
            )
        )
    )
) {
    ScanEditScreen(
        ScanEditParams(
            image = "some/image".toPath(),
            location = Location()
        )
    )
}
