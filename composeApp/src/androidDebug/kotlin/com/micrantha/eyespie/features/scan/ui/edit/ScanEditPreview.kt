package com.micrantha.eyespie.features.scan.ui.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import com.micrantha.eyespie.core.PreviewContext
import com.micrantha.eyespie.core.ui.component.Choice
import com.micrantha.eyespie.domain.entities.Location
import okio.Path.Companion.toOkioPath
import java.nio.file.Paths

@Preview(showBackground = true, backgroundColor = 0xFF, widthDp = 200, heightDp = 400)
@Composable
fun ScanEditPreview() = PreviewContext(
    state = ScanEditUiState(
        image = object : Painter() {
            override val intrinsicSize = Size(200F, 400F)
            override fun DrawScope.onDraw() {
                drawRect(Color.Yellow)
            }
        },
        name = "Test",
        labels = listOf(
            Choice("label1", tag = "label1", key = "label1"),
            Choice("label2", tag = "label2", key = "label2"),
        ),
        colors = listOf(
            Choice("color1", tag = "color1", key = "color1"),
            Choice("color2", tag = "color2", key = "color2"),
        ),
        detections = listOf(
            Choice("detection1", tag = "detection1", key = "detection1"),
            Choice("detection2", tag = "detection2", key = "detection2"),
        ),
        customDetection = null,
        enabled = true,
        customLabel = "customLabel",
        customColor = null,
        showLabels = true,
        showColors = true,
        showDetections = true
    )
) {
    ScanEditScreen(
        it, ScanEditParams(
            Paths.get(".").toOkioPath(),
            Location()
        )
    )
}
