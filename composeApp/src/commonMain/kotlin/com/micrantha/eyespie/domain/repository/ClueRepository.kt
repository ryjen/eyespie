package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.entities.Clue
import com.micrantha.eyespie.domain.entities.ColorClue
import com.micrantha.eyespie.domain.entities.ColorProof
import com.micrantha.eyespie.domain.entities.DetectClue
import com.micrantha.eyespie.domain.entities.DetectProof
import com.micrantha.eyespie.domain.entities.LabelClue
import com.micrantha.eyespie.domain.entities.LabelProof
import com.micrantha.eyespie.platform.scan.CameraImage

interface ClueRepository<T : Clue<*>, Proof : Collection<T>> {
    suspend fun analyze(image: CameraImage): Result<Proof>
}

typealias LabelRepository = ClueRepository<LabelClue, LabelProof>
typealias DetectRepository = ClueRepository<DetectClue, DetectProof>
typealias ColorRepository = ClueRepository<ColorClue, ColorProof>
