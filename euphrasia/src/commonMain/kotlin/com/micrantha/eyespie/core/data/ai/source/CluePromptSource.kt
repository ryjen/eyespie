package com.micrantha.eyespie.core.data.ai.source

class CluePromptSource {

    // TODO: remote prompts
    fun clues() =  """
            Analyze this image for a game of 'I Spy'. Generate a maximum of 3 distinct clues.
                        
            For each clue:
            - Describe an object by its color, shape, rhyme, or unique feature (without naming it).
            - Provide the answer.
            - Include a confidence score from 0.0 to 1.0 indicating how certain you are about the match.
            
            Respond with three lines per clue:
            1. The clue
            2. The answer
            3. Confidence score
            
            Use a different clue style (e.g., color, shape, rhyme, or feature) for each object to ensure diversity.
            Avoid vague or generic clues.
            Prefer clues with higher confidence and larger shape.
            Only describe objects that are clearly visible and identifiable.
            If fewer than 3 objects are visible, generate only what's possible.
            
            Sample Output:
            
            I spy with my little eye, something round and red.
            An apple
            0.95
            I spy with my little eye something that is shiny, fast, and zooms down the street.
            A car
            0.85
            I spy with my little eye something that is green, leafy, and rustles in the wind.
            A tree
            0.75
       """

    fun guess(clue: String) = """
        You are playing a game of "I Spy." 
        
        Clue: $clue
                
        Your task is to guess the object being described. 
        Use the clue to infer the object's color, shape, name, and any distinctive features. 
        Respond with a single word or short phrase that best matches the clue.
        
        Respond with:
        The object being described.
    """
}
