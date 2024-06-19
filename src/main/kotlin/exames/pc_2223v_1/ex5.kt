package exames.pc_2223v_1

import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


suspend fun <T, U, R> combineFutures(f1: CompletableFuture<T>, f2: CompletableFuture<U>, combiner: (T, U)->R): R {

    return suspendCoroutine<R> { continuation ->

        f1.handle { value1, ex1 ->
            if (ex1 != null) {
                continuation.resumeWithException(ex1)
            }
            else {
                f2.handle { value2, ex2 ->
                    if (ex2 != null) {
                        continuation.resumeWithException(ex2)
                    }
                    else {
                        continuation.resume(combiner(value1, value2))
                    }
                }
            }
        }
    }

}


suspend fun <T, U, R> combineFuturesThenCombine(f1: CompletableFuture<T>, f2: CompletableFuture<U>, combiner: (T, U)->R): R {
    val f = f1.thenCombine(f2, combiner)
    return suspendCoroutine<R> {continuation ->
        f.handle { value, ex ->
            if (ex != null) {
                continuation.resumeWithException(ex)
            }
            else {
                continuation.resume(value)
            }
        }
    }
}