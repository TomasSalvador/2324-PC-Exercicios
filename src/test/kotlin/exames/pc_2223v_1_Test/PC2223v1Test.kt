package pt.isel.pc.basics.exames.pc_2223v_1_Test

import exames.pc_2223v_1.MessageBroadcaster
import exames.pc_2223v_1.ThreadPool
import exames.pc_2223v_1.UnsafeSuccessionA
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import java.util.concurrent.RejectedExecutionException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

const val N_OF_THREADS = 5

class PC2223v1Test {

    @Test
    fun `alinea a`() {
        val array = Array<Int>(10) { it }
        val u = UnsafeSuccessionA<Int>(array)
    }

    @Test
    fun ex2Test() {
        val mb = MessageBroadcaster<Int>()
        val message = 15
        val threads = List(N_OF_THREADS) {
            Thread.ofPlatform().start {
                assertEquals(message, mb.waitForMessage(5.seconds))
            }
        }

        Thread.sleep(1000)

        assertTrue { mb.sendToAll(message).all { it in threads } }

        threads.forEach { it.join() }

    }

    @Test
    fun ex3Test() {
        val runnable = Runnable {
            Thread.sleep(1000)
            println("Kachow")
        }
        val tp = ThreadPool(1)
        val threads = List(N_OF_THREADS) {
            Thread.ofPlatform().start {
                tp.execute(runnable)
            }
        }

        Thread.sleep(500)

        Thread.ofPlatform().start {
            Thread.sleep(3000)
            assertThrows<RejectedExecutionException> {
                tp.execute(runnable)
            }
        }

        logger.info("before shutdown")
        tp.shutdownAndWaitForTermination()
        logger.info("after shutdown")

        threads.forEach { it.join() }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PC2223v1Test::class.java)
    }
}

