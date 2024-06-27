package exames.pc_2223v_1

import NodeLinkedList
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ThreadPool(
    nOfThreads: Int
) {
    init {
        repeat(nOfThreads) {
            Thread.ofPlatform().start {
                workerThreadLoop()
            }
        }
    }

    data class WaintingThread(
        val condition: Condition,
        var runnable: Runnable? = null,
    )

    private val lock = ReentrantLock()
    private var activeThreads = nOfThreads
    private val waitingThreads = NodeLinkedList<WaintingThread>()
    private val runnables = NodeLinkedList<Runnable>()
    private var shuttingDown = false
    private val termCond = lock.newCondition()


    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit {
        lock.withLock {
            if (shuttingDown) {
                throw RejectedExecutionException()
            }
            if (waitingThreads.notEmpty) {
                val workerTh = waitingThreads.pull().value
                workerTh.runnable = runnable
                workerTh.condition.signal()
                return
            }
            runnables.enqueue(runnable)
            return
        }
    }

    @Throws(InterruptedException::class)
    fun shutdownAndWaitForTermination(): Unit {
        lock.withLock {
            shuttingDown = true
            if (runnables.empty && waitingThreads.notEmpty) {
                val wtsize = waitingThreads.count
                waitingThreads.forEach { it.condition.signal() }
                while (waitingThreads.notEmpty) {
                    waitingThreads.pull()
                }
                if (wtsize==activeThreads) {
                    return
                }
            }
            while (true) {
                try {
                    termCond.await()
                } catch (ex: InterruptedException){
                    if (activeThreads==0){
                        Thread.currentThread().interrupt()
                        return
                    }
                    throw ex
                }
                
                if (activeThreads==0){
                    return
                }
            }
        }
    }

    private fun workerThreadLoop(){
        val cond = lock.newCondition()
        val thisThread = WaintingThread(cond)
        var runnable: Runnable? = null
        while (true) {
            lock.withLock {
                if (runnables.notEmpty) {
                    runnable = runnables.pull().value
                } else {
                    if (shuttingDown) {
                        activeThreads--
                        if (activeThreads == 0) {
                            termCond.signalAll()
                        }
                        return
                    }

                    try {
                        waitingThreads.enqueue(thisThread)
                        cond.await()
                    } catch (ex: InterruptedException) {
                        // ignored
                    }
                    if (thisThread.runnable != null) {
                        runnable = thisThread.runnable!!
                    }
                }
            }
            if (runnable != null) {
                runnable!!.run()
            }
        }
    }

}