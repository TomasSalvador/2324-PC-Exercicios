package exames.pc_2122v_1

import NodeLinkedList
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class Semaphore(private val initialUnits: Int) {

    private data class WaitingThread(
        val condition: Condition,
        var isDone: Boolean = false
    )

    private val lock = ReentrantLock()
    private var count = initialUnits
    private var shuttingDown = false
    private val waintingThreads = NodeLinkedList<WaitingThread>()
    private val termCond = lock.newCondition()

    fun release(): Unit {
        lock.withLock {
            count++
            if (shuttingDown) {
                if (count == initialUnits) {
                    termCond.signalAll()
                }
                return
            }
            if (waintingThreads.notEmpty) {
                count--
                val th = waintingThreads.pull().value
                th.isDone = true
                th.condition.signal()
            }
        }
    }

    @Throws(InterruptedException::class, RejectedExecutionException::class)
    fun acquire(timeout: Duration): Boolean {
        if (shuttingDown) {
            throw RejectedExecutionException()
        }

        if (count > 0) {
            count--
            return true
        }

        var remainingTime = timeout.inWholeNanoseconds
        val condition = lock.newCondition()
        val thisNode = waintingThreads.enqueue(WaitingThread(condition))

        while (true) {
            try {
                remainingTime = condition.awaitNanos(remainingTime)
            } catch (ex: InterruptedException) {
                if (thisNode.value.isDone) {
                    Thread.currentThread().interrupt()
                    return true
                }
                waintingThreads.remove(thisNode)
                throw ex
            }

            if (thisNode.value.isDone) {
                return true
            }

            if (shuttingDown) {
                waintingThreads.remove(thisNode)
                throw RejectedExecutionException()
            }

            if (remainingTime <= 0) {
                waintingThreads.remove(thisNode)
                return false
            }

        }
    }

    fun shutdown(): Unit {
        lock.withLock {
            if (!shuttingDown) {
                shuttingDown = true
                waintingThreads.forEach { it.condition.signal() }
            }
        }
    }

    @Throws(InterruptedException::class)
    fun awaitTermination(timeout: Duration): Boolean {
        lock.withLock {
            if (!shuttingDown) {
                shutdown()
            }

            if (count == initialUnits) {
                return true
            }

            var remainingTime = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingTime = termCond.awaitNanos(remainingTime)
                } catch (ex: InterruptedException) {
                    if (count == initialUnits) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    throw ex
                }

                if (count == initialUnits) {
                    return true
                }

                if (remainingTime <= 0) {
                    return false
                }
            }
        }
    }

}