package exames.pc_2223v_1

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
    private val waitingThreads = mutableListOf<WaintingThread>()
    private val runnables = mutableListOf<Runnable>()
    private var shuttingDown = false
    private val termCond = lock.newCondition()


    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit {
        lock.withLock {
            if (shuttingDown) {
                throw RejectedExecutionException()
            }
            if (waitingThreads.isNotEmpty()) {
                val workerTh = waitingThreads.removeFirst()
                workerTh.runnable = runnable
                workerTh.condition.signal()
                return
            }
            runnables.add(runnable)
            return
        }
    }

    @Throws(InterruptedException::class)
    fun shutdownAndWaitForTermination(): Unit {
        lock.withLock {
            shuttingDown = true
            if (runnables.isEmpty() && waitingThreads.isNotEmpty()) {
                val wtsize = waitingThreads.size
                waitingThreads.forEach { it.condition.signal() }
                waitingThreads.clear()
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
                if (runnables.isNotEmpty()) {
                    runnable = runnables.removeFirst()
                } else {
                    if (shuttingDown) {
                        activeThreads--
                        if (activeThreads == 0) {
                            termCond.signalAll()
                        }
                        return
                    }

                    try {
                        waitingThreads.add(thisThread)
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