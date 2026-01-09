package com.micrantha.bluebell.platform

import androidx.compose.ui.graphics.ImageBitmap

actual fun ByteArray.toImageBitmap(): ImageBitmap = ImageBitmap(1, 1)
