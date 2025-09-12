package com.micrantha.eyespie.core.data.ai.source

class CluePromptSource {
    val colorsPrompt = """
        Examine the image and determine the dominant colors with a maximum of 5.
        Provide output as JSON with the following format: 
            [{data: string, confidence: number}]
        Confidence should take into account:
            - the total percentage
            - the variation of hue within the color
            - the brightness of the color compared to others
    """


    val detectPrompt = """
        Examine the image and detect the objects within it to a maximum of 5.
        Provide output as JSON with the following format: 
            [{data: string, confidence: number}]
    """

    val labelPrompt = """
        Examine the image and classify it with labels to a maximum of 5.
        Provide output as JSON with the following format: 
            [{data: string, confidence: number}]
     """
}
