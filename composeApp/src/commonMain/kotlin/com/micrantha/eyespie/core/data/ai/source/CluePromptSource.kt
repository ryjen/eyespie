package com.micrantha.eyespie.core.data.ai.source

import com.micrantha.eyespie.core.data.ai.model.AiPrompt

class CluePromptSource {
    fun cluesPrompt() = AiPrompt(
        role = "user",
        prompt = """
            You are someone playing a game of "I Spy".
            
            Find the following clues:
            - colors
            - classifications
            - detected objects
            
            Try to relate or rank the clues.  Generate a final proof of the best clues.  
            Output as JSON with the following format:
            {"colors": {data: string, confidence: number}, "labels": {data: string, confidence: number}, "detections": {data: string, confidence: number}}
        """)

    fun rhymesPrompt(words: String) = AiPrompt(
        role = "user",
        prompt = """
            Generate a list of potential rhymes for the following words: $words
            
            Output as JSON with the following format:
            [{data: string, confidence: number}]
        """
    )

    fun proofPrompt(colors: String, classifications: String, detections: String) = AiPrompt(
        role = "user",
        prompt = """
            You are someone playing a game of "I Spy".
            Given the following potential clues formatted in JSON:
            
            Colors: $colors
            Classifications: $classifications
            Detections: $detections

            Generate a final proof of the best clues.  Try to relate or rank the clues.
            Output as JSON with the following format:
        """)

}
