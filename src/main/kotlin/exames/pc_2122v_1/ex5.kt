package exames.pc_2122v_1

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicReference

suspend fun race(f0: suspend () -> Int, f1: suspend () -> Int): Int {

    val res = AtomicReference<Int?>(null)

    supervisorScope {

        val outerScope = this

        launch {
            res.set(f0())
            outerScope.cancel()
        }

        launch {
            res.set(f1())
            outerScope.cancel()
        }

    }

    return res.get() ?: throw Exception()

}