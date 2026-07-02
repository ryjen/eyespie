package com.micrantha.eyespie.features.onboarding.data

class FakeOnboardingRepository : OnboardingRepository {
    var runOnce: Boolean = false
    var model: String? = null
    var hasGenAIValue: Boolean = false

    override suspend fun setHasRunOnce(value: Boolean) {
        runOnce = value
    }

    override suspend fun hasRunOnce(): Boolean = runOnce

    override suspend fun setGenAiModel(model: String) {
        this.model = model
    }

    override suspend fun hasGenAI(): Boolean = hasGenAIValue

    override suspend fun genAiModel(): String? = model
}
