package exames.pc_2223v_1

import NodeLinkedList
import kotlinx.coroutines.selects.whileSelect
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class MessageBroadcaster<T> {

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val waitingThreads = NodeLinkedList<WaitingReq<T>>()

    data class WaitingReq<T>(
        val thread: Thread,
        var msg: T? = null,
    )

    @Throws(InterruptedException::class)
    fun waitForMessage(timeout: Duration): T? {
        lock.withLock {
            var remainingTime = timeout.inWholeNanoseconds
            val thisReq = WaitingReq<T>(Thread.currentThread())
            val thisNode = waitingThreads.enqueue(thisReq)

            while (true) {
                try {
                    remainingTime = condition.awaitNanos(remainingTime)
                } catch (ex: InterruptedException) {
                    if (thisReq.msg != null) {
                        Thread.currentThread().interrupt()
                        return thisReq.msg
                    }
                    waitingThreads.remove(thisNode)
                    throw ex
                }

                if (thisReq.msg != null) {
                    return thisReq.msg
                }

                if (remainingTime <= 0) {
                    waitingThreads.remove(thisNode)
                    return null
                }
            }
        }
    }

    fun sendToAll(message: T): List<Thread> {
        lock.withLock {
            val threads = waitingThreads.toList().map { it.thread }
            waitingThreads.forEach { it.msg = message }
            while(waitingThreads.notEmpty) {
                waitingThreads.pull()
            }
            condition.signalAll()
            return threads
        }
    }
}