package com.micrantha.eyespie.features.scan.usecase

import org.kodein.di.DirectDI

actual fun platformImageEmbeddingGenerator(di: DirectDI): ImageEmbeddingGenerator = 
    DeterministicImageEmbeddingGenerator()
