package com.micrantha.eyespie.features.scan.usecase

import org.kodein.di.DI

actual fun platformImageEmbeddingGenerator(di: DI): ImageEmbeddingGenerator = 
    DeterministicImageEmbeddingGenerator()
