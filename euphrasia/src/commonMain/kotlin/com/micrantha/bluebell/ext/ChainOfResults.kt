package com.micrantha.bluebell.ext

fun interface ResultStep<in In, out Out> {
    suspend fun execute(input: In): Result<Out>
}

fun interface Step<in In, out Out> {
    suspend fun execute(input: In): Out
}

class ResultPipeline<In, Out>(
    private val run: suspend (In) -> Result<Out>
) {
    suspend operator fun invoke(input: In): Result<Out> = run(input)
    
    fun <Next> then(next: ResultStep<Out, Next>): ResultPipeline<In, Next> {
        return ResultPipeline { input ->
            run(input).fold(
                onSuccess = { next.execute(it) },
                onFailure = { Result.failure(it) }
            )
        }
    }

    fun <Next> then(next: Step<Out, Next>): ResultPipeline<In, Next> {
        return ResultPipeline { input ->
            run(input).map { next.execute(it) }
        }
    }


    companion object {
        fun <T> from(): ResultPipeline<T, T> {
            return ResultPipeline { input -> Result.success(input) }
        }

        fun <T> fromResult(block: suspend (T) -> Result<T>) = ResultPipeline<T, T> {
            block(it)
        }

        fun <T> from(block: suspend (T) -> T) = ResultPipeline<T, T> {
            Result.success(block(it))
        }
    }
}
