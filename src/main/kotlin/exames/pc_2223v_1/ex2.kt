package exames.pc_2223v_1

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class MessageBroadcaster<T> {

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val waitingThreads = mutableSetOf<WaitingReq<T>>()

    data class WaitingReq<T>(
        val thread: Thread,
        var msg: T? = null,
    )

    @Throws(InterruptedException::class)
    fun waitForMessage(timeout: Duration): T? {
        lock.withLock {
            var remainingTime = timeout.inWholeNanoseconds
            val thread = WaitingReq<T>(Thread.currentThread())

            while (true) {
                try {
                    waitingThreads.add(thread)
                    remainingTime = condition.awaitNanos(remainingTime)
                } catch (ex: InterruptedException) {
                    if (thread.msg != null) {
                        Thread.currentThread().interrupt()
                        return thread.msg
                    }
                    waitingThreads.remove(thread)
                    throw ex
                }

                if (thread.msg != null) {
                    return thread.msg
                }

                if (remainingTime <= 0) {
                    return null
                }
            }
        }
    }

    fun sendToAll(message: T): List<Thread> {
        lock.withLock {
            val threads = waitingThreads.toList().map { it.thread }
            waitingThreads.forEach { it.msg = message }
            waitingThreads.clear()
            condition.signalAll()
            return threads
        }
    }
}