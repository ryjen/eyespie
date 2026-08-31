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
 * Shared brand artwork drawn with Compose Canvas so it stays crisp at any size
 * and follows the themed palette. No raster assets are required.
 */

private val BrandViolet = Color(0xFF5B3E8F)
private val BrandVioletLight = Color(0xFFB79BE6)
private val BrandSky = Color(0xFF4F86C6)
private val BrandSun = Color(0xFFE6A23C)
private val BrandGreen = Color(0xFF3FA66A)
private val BrandInk = Color(0xFF2B2440)

/**
 * The Eyespie mark: two crossed spyglass barrels forming an "E", on a tinted
 * disc. Used in the app header and the onboarding flow.
 */
@Composable
fun EyespieLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = BrandViolet,
) {
    Canvas(modifier = modifier.size(size)) {
        val c = size.toPx() / 2f
        val r = size.toPx() / 2f

        // backing disc
        drawCircle(color = BrandVioletLight.copy(alpha = 0.35f), radius = r * 0.92f, center = Offset(c, c))

        // two crossed barrels
        val half = r * 0.74f
        val w = r * 0.30f
        rotate(35f, pivot = Offset(c, c)) {
            drawLine(tint, Offset(c - half, c + half), Offset(c + half, c - half), strokeWidth = w, cap = StrokeCap.Round)
        }
        rotate(-35f, pivot = Offset(c, c)) {
            drawLine(tint, Offset(c - half, c - half), Offset(c + half, c + half), strokeWidth = w, cap = StrokeCap.Round)
        }
        // centre hub
        drawCircle(color = tint, radius = r * 0.20f, center = Offset(c, c))
    }
}

/**
 * Onboarding illustration for [scene]. Each scene gets a small, friendly vector
 * picture matching the travel-spy tone of the mockups.
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
    // sky + ground
    drawRect(BrandSky.copy(alpha = 0.18f), Offset(0f, 0f), Size(w, horizon))
    drawRect(BrandGreen.copy(alpha = 0.20f), Offset(0f, horizon), Size(w, h - horizon))
    // mountains
    val m = Path().apply {
        moveTo(w * 0.10f, horizon)
        lineTo(w * 0.30f, horizon - h * 0.26f)
        lineTo(w * 0.50f, horizon)
        moveTo(w * 0.42f, horizon)
        lineTo(w * 0.66f, horizon - h * 0.36f)
        lineTo(w * 0.90f, horizon)
    }
    drawPath(m, BrandViolet.copy(alpha = 0.55f))
    // sun
    drawCircle(BrandSun, radius = h * 0.09f, center = Offset(w * 0.80f, horizon * 0.45f))
    // padlock body + shackle
    val lx = w * 0.5f
    val ly = horizon + (h - horizon) * 0.45f
    val lw = w * 0.16f
    drawRect(BrandViolet, Offset(lx - lw / 2f, ly), Size(lw, lw * 0.85f))
    drawCircle(BrandViolet, radius = lw * 0.34f, center = Offset(lx, ly))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCreateScene(w: Float, h: Float) {
    // crossed binoculars + location pin over a soft map
    drawRect(BrandVioletLight.copy(alpha = 0.25f), Offset(0f, 0f), Size(w, h))
    val c = Offset(w * 0.5f, h * 0.55f)
    val half = h * 0.30f
    val barrel = h * 0.12f
    rotate(35f, pivot = c) { drawLine(BrandViolet, Offset(c.x - half, c.y + half), Offset(c.x + half, c.y - half), strokeWidth = barrel, cap = StrokeCap.Round) }
    rotate(-35f, pivot = c) { drawLine(BrandViolet, Offset(c.x - half, c.y - half), Offset(c.x + half, c.y + half), strokeWidth = barrel, cap = StrokeCap.Round) }
    drawCircle(BrandViolet, radius = h * 0.07f, center = c)
    // pin
    val px = w * 0.74f
    val py = h * 0.30f
    val pr = h * 0.10f
    drawCircle(BrandSun, radius = pr, center = Offset(px, py - pr))
    drawPath(
        Path().apply {
            moveTo(px - pr, py - pr)
            lineTo(px + pr, py - pr)
            lineTo(px, py + pr * 1.4f)
            close()
        },
        BrandSun,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawShareScene(w: Float, h: Float) {
    // document with .eyespie moving to a device
    drawRect(BrandSky.copy(alpha = 0.16f), Offset(0f, 0f), Size(w, h))
    val dw = w * 0.26f
    val dx = w * 0.28f
    val dy = h * 0.32f
    drawRect(BrandViolet, Offset(dx, dy), Size(dw, h * 0.30f))
    drawLine(BrandInk.copy(alpha = 0.5f), Offset(dx + dw * 0.2f, dy + h * 0.07f), Offset(dx + dw * 0.8f, dy + h * 0.07f), strokeWidth = w * 0.012f)
    drawLine(BrandInk.copy(alpha = 0.5f), Offset(dx + dw * 0.2f, dy + h * 0.14f), Offset(dx + dw * 0.8f, dy + h * 0.14f), strokeWidth = w * 0.012f)
    // arrow to a phone
    val ax = w * 0.66f
    drawArrow(Offset(dx + dw, dy + h * 0.15f), Offset(ax, dy + h * 0.15f), BrandGreen, w * 0.02f)
    val pw = w * 0.20f
    drawRect(BrandViolet, Offset(ax, h * 0.24f), Size(pw, h * 0.46f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawJoinScene(w: Float, h: Float) {
    // an offered file meeting a device
    drawRect(BrandGreen.copy(alpha = 0.16f), Offset(0f, 0f), Size(w, h))
    val dw = w * 0.26f
    val dx = w * 0.30f
    val dy = h * 0.34f
    drawRect(BrandViolet, Offset(dx, dy), Size(dw, h * 0.28f))
    val ax = w * 0.64f
    drawArrow(Offset(dx + dw, dy + h * 0.14f), Offset(ax, dy + h * 0.14f), BrandSun, w * 0.02f)
    val pw = w * 0.20f
    drawRect(BrandViolet, Offset(ax, h * 0.26f), Size(pw, h * 0.42f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(from: Offset, to: Offset, color: Color, width: Float) {
    drawLine(color, from, to, strokeWidth = width, cap = StrokeCap.Round)
    val angle = kotlin.math.atan2(to.y - from.y, to.x - from.x)
    val len = width * 4f
    val left = angle + Math.PI.toFloat() * 0.85f
    val right = angle - Math.PI.toFloat() * 0.85f
    drawLine(color, to, Offset(to.x + len * cos(left), to.y + len * sin(left)), strokeWidth = width, cap = StrokeCap.Round)
    drawLine(color, to, Offset(to.x + len * cos(right), to.y + len * sin(right)), strokeWidth = width, cap = StrokeCap.Round)
}
