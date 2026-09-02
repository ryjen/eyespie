package com.micrantha.eyespie.presentation.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared brand artwork drawn with Compose Canvas so it stays crisp at any size.
 *
 * Colors come from the canonical Micrantha Lens palette. This file must not grow
 * an independent app palette: the SVG/store icon and Compose mark are two renderings
 * of the same product identity.
 */

/**
 * Compact Compose rendering of the Micrantha Lens mark: five flower petals around
 * a camera-lens centre. Used in application chrome where loading the store raster
 * would be unnecessary.
 */
@Composable
fun EyespieLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = EyespieBrandColors.Ink,
) {
    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.minDimension / 2f

        drawCircle(
            color = EyespieBrandColors.Field.copy(alpha = 0.72f),
            radius = radius * 0.94f,
            center = center,
        )

        repeat(5) { index ->
            rotate(index * 72f, pivot = center) {
                val petalCenter = Offset(center.x, center.y - radius * 0.46f)
                drawOval(
                    color = EyespieBrandColors.Petal,
                    topLeft = Offset(
                        petalCenter.x - radius * 0.24f,
                        petalCenter.y - radius * 0.34f,
                    ),
                    size = Size(radius * 0.48f, radius * 0.68f),
                )
                drawOval(
                    color = EyespieBrandColors.PetalInner,
                    topLeft = Offset(
                        petalCenter.x - radius * 0.13f,
                        petalCenter.y - radius * 0.20f,
                    ),
                    size = Size(radius * 0.26f, radius * 0.40f),
                )
            }
        }

        drawCircle(
            color = EyespieBrandColors.Throat,
            radius = radius * 0.34f,
            center = center,
        )
        drawCircle(
            color = EyespieBrandColors.Iris,
            radius = radius * 0.25f,
            center = center,
        )
        drawCircle(
            color = tint,
            radius = radius * 0.15f,
            center = center,
        )
        drawCircle(
            color = EyespieBrandColors.White.copy(alpha = 0.72f),
            radius = radius * 0.045f,
            center = Offset(center.x - radius * 0.055f, center.y - radius * 0.055f),
        )
    }
}

/**
 * Onboarding illustration for [scene]. Each scene gets a small field-guide picture
 * using the canonical Micrantha palette and semantic success color.
 */
enum class BrandScene { Local, Create, Share, Join }

@Composable
fun OnboardingIllustration(
    scene: BrandScene,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
) {
    Canvas(modifier = modifier.size(width = 240.dp, height = height)) {
        val w = size.width
        val h = size.height
        when (scene) {
            BrandScene.Local -> drawLocalScene(w, h)
            BrandScene.Create -> drawCreateScene(w, h)
            BrandScene.Share -> drawShareScene(w, h)
            BrandScene.Join -> drawJoinScene(w, h)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLocalScene(w: Float, h: Float) {
    val horizon = h * 0.68f
    drawRect(EyespieBrandColors.PetalInner.copy(alpha = 0.16f), Offset(0f, 0f), Size(w, horizon))
    drawRect(Success.copy(alpha = 0.18f), Offset(0f, horizon), Size(w, h - horizon))

    val mountains = Path().apply {
        moveTo(w * 0.10f, horizon)
        lineTo(w * 0.30f, horizon - h * 0.26f)
        lineTo(w * 0.50f, horizon)
        moveTo(w * 0.42f, horizon)
        lineTo(w * 0.66f, horizon - h * 0.36f)
        lineTo(w * 0.90f, horizon)
    }
    drawPath(mountains, EyespieBrandColors.Ink.copy(alpha = 0.55f))
    drawCircle(
        EyespieBrandColors.Iris,
        radius = h * 0.09f,
        center = Offset(w * 0.80f, horizon * 0.45f),
    )

    val lockX = w * 0.5f
    val lockY = horizon + (h - horizon) * 0.45f
    val lockWidth = w * 0.16f
    drawRect(
        EyespieBrandColors.Ink,
        Offset(lockX - lockWidth / 2f, lockY),
        Size(lockWidth, lockWidth * 0.85f),
    )
    drawCircle(
        EyespieBrandColors.Ink,
        radius = lockWidth * 0.34f,
        center = Offset(lockX, lockY),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCreateScene(w: Float, h: Float) {
    drawRect(EyespieBrandColors.Petal.copy(alpha = 0.20f), Offset(0f, 0f), Size(w, h))

    val center = Offset(w * 0.5f, h * 0.55f)
    val lensRadius = h * 0.20f
    drawCircle(EyespieBrandColors.Throat, lensRadius * 1.25f, center)
    drawCircle(EyespieBrandColors.Iris, lensRadius, center)
    drawCircle(EyespieBrandColors.Pupil, lensRadius * 0.58f, center)
    drawCircle(
        EyespieBrandColors.White.copy(alpha = 0.75f),
        lensRadius * 0.13f,
        Offset(center.x - lensRadius * 0.20f, center.y - lensRadius * 0.20f),
    )

    val pinX = w * 0.74f
    val pinY = h * 0.30f
    val pinRadius = h * 0.10f
    drawCircle(EyespieBrandColors.Iris, radius = pinRadius, center = Offset(pinX, pinY - pinRadius))
    drawPath(
        Path().apply {
            moveTo(pinX - pinRadius, pinY - pinRadius)
            lineTo(pinX + pinRadius, pinY - pinRadius)
            lineTo(pinX, pinY + pinRadius * 1.4f)
            close()
        },
        EyespieBrandColors.Iris,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawShareScene(w: Float, h: Float) {
    drawRect(EyespieBrandColors.PetalInner.copy(alpha = 0.14f), Offset(0f, 0f), Size(w, h))
    val documentWidth = w * 0.26f
    val documentX = w * 0.28f
    val documentY = h * 0.32f
    drawRect(EyespieBrandColors.Ink, Offset(documentX, documentY), Size(documentWidth, h * 0.30f))
    drawLine(
        EyespieBrandColors.White.copy(alpha = 0.72f),
        Offset(documentX + documentWidth * 0.2f, documentY + h * 0.07f),
        Offset(documentX + documentWidth * 0.8f, documentY + h * 0.07f),
        strokeWidth = w * 0.012f,
    )
    drawLine(
        EyespieBrandColors.White.copy(alpha = 0.72f),
        Offset(documentX + documentWidth * 0.2f, documentY + h * 0.14f),
        Offset(documentX + documentWidth * 0.8f, documentY + h * 0.14f),
        strokeWidth = w * 0.012f,
    )

    val phoneX = w * 0.66f
    drawArrow(
        Offset(documentX + documentWidth, documentY + h * 0.15f),
        Offset(phoneX, documentY + h * 0.15f),
        Success,
        w * 0.02f,
    )
    drawRect(
        EyespieBrandColors.Pupil,
        Offset(phoneX, h * 0.24f),
        Size(w * 0.20f, h * 0.46f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawJoinScene(w: Float, h: Float) {
    drawRect(Success.copy(alpha = 0.14f), Offset(0f, 0f), Size(w, h))
    val documentWidth = w * 0.26f
    val documentX = w * 0.30f
    val documentY = h * 0.34f
    drawRect(EyespieBrandColors.Ink, Offset(documentX, documentY), Size(documentWidth, h * 0.28f))

    val phoneX = w * 0.64f
    drawArrow(
        Offset(documentX + documentWidth, documentY + h * 0.14f),
        Offset(phoneX, documentY + h * 0.14f),
        EyespieBrandColors.Iris,
        w * 0.02f,
    )
    drawRect(
        EyespieBrandColors.Pupil,
        Offset(phoneX, h * 0.26f),
        Size(w * 0.20f, h * 0.42f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(
    from: Offset,
    to: Offset,
    color: Color,
    width: Float,
) {
    drawLine(color, from, to, strokeWidth = width, cap = StrokeCap.Round)
    val angle = kotlin.math.atan2(to.y - from.y, to.x - from.x)
    val len = width * 4f
    val left = angle + Math.PI.toFloat() * 0.85f
    val right = angle - Math.PI.toFloat() * 0.85f
    drawLine(
        color,
        to,
        Offset(to.x + len * cos(left), to.y + len * sin(left)),
        strokeWidth = width,
        cap = StrokeCap.Round,
    )
    drawLine(
        color,
        to,
        Offset(to.x + len * cos(right), to.y + len * sin(right)),
        strokeWidth = width,
        cap = StrokeCap.Round,
    )
}
