package exames.pc_2122v_2

import NodeLinkedList
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class MessageQueue<T>() {

    private data class EnqueueRequest<T>(
        val condition: Condition,
        val msg: T,
        var thread: Thread? = null
    )

    private data class DequeueRequest<T>(
        val thread: Thread,
        val condition: Condition,
        val nOfMessages: Int,
        var messages: List<T>? = null
    )


    private val lock = ReentrantLock()
    private val waitingEnqueues = NodeLinkedList<EnqueueRequest<T>>()
    private val waitingDequeues = NodeLinkedList<DequeueRequest<T>>()


    @Throws(InterruptedException::class)
    fun tryEnqueue(message: T, timeout: Duration): Thread? {
        lock.withLock {
            // fast-path
            if (waitingDequeues.notEmpty) {
                if (waitingEnqueues.count + 1 == waitingDequeues.headNode!!.value.nOfMessages) {
                    val deQreq = waitingDequeues.pull().value

                    deQreq.messages = List<T>(deQreq.nOfMessages) {
                        if (it < deQreq.nOfMessages-1) {
                            waitingEnqueues.pull().value.msg
                        } else {
                            message
                        }
                    }

                    deQreq.condition.signal()
                    return deQreq.thread
                }
            }

            // wait-path
            var remainingTime = timeout.inWholeNanoseconds
            val condition = lock.newCondition()
            val thisReq = EnqueueRequest(condition, message)
            val thisNode = waitingEnqueues.enqueue(thisReq)

            while (true) {
                try {
                    remainingTime = condition.awaitNanos(remainingTime)
                } catch (ex: InterruptedException) {
                    // Sucesso
                    if (thisReq.thread != null) {
                        Thread.currentThread().interrupt()
                        return thisReq.thread
                    }
                    waitingEnqueues.remove(thisNode)
                    throw ex
                }

                // Sucesso
                if (thisReq.thread != null) {
                    return thisReq.thread
                }

                if (remainingTime <= 0) {
                    waitingEnqueues.remove(thisNode)
                    return null
                }

            }

        }
    }

    @Throws(InterruptedException::class)
    fun tryDequeue(nOfMessages: Int, timeout: Duration): List<T> {
        lock.withLock {
            // fast-path
            if (waitingEnqueues.count >= nOfMessages) {
                val listEnq = List(nOfMessages) { waitingEnqueues.pull().value }
                listEnq.forEach {
                    it.thread = Thread.currentThread()
                    it.condition.signal()
                }
                return listEnq.map { it.msg }
            }

            // wait-path
            var remainingTime = timeout.inWholeNanoseconds
            val condition = lock.newCondition()
            val thisReq = DequeueRequest<T>(Thread.currentThread(), condition, nOfMessages)
            val thisNode = waitingDequeues.enqueue(thisReq)

            while (true) {
                try {
                    remainingTime = condition.awaitNanos(remainingTime)
                } catch (ex: InterruptedException) {
                    // Sucesso
                    if (thisReq.messages != null) {
                        Thread.currentThread().interrupt()
                        return thisReq.messages!!
                    }
                    // timeout, nao sei é preciso
                    if (remainingTime <= 0) {
                        waitingDequeues.remove(thisNode)
                        val listEnq = List(waitingEnqueues.count) { waitingEnqueues.pull().value }
                        listEnq.forEach {
                            it.thread = Thread.currentThread()
                            it.condition.signal()
                        }
                        return listEnq.map { it.msg }
                    }
                    waitingDequeues.remove(thisNode)
                    throw ex
                }

                // Sucesso
                if (thisReq.messages != null) {
                    return thisReq.messages!!
                }

                // timeout
                if (remainingTime <= 0) {
                    waitingDequeues.remove(thisNode)
                    val listEnq = List(waitingEnqueues.count) { waitingEnqueues.pull().value }
                    listEnq.forEach {
                        it.thread = Thread.currentThread()
                        it.condition.signal()
                    }
                    return listEnq.map { it.msg }
                }

            }
        }
    }

}


















