package pt.isel.pc.basics.exames.pc_2122_v1

import exames.pc_2122v_1.Semaphore
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds


class PC2122v1Tests {

    @Test
    fun `test ex 2`() {
        val nOfThreads = 5
        val sem = Semaphore(nOfThreads)
        val threads = List(nOfThreads) {
            Thread.ofPlatform().start {
                sem.acquire(5.seconds)
            }
        }


    }
}