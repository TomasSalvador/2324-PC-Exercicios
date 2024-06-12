package pt.isel.pc.basics

import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Exchanger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

//private val log = LoggerFactory.getLogger(ThreadingHazardsTests::class.java)

// Number of threads used on each test
private const val N_OF_THREADS = 10

// Number of repetitions performed by each thread
private const val N_OF_REPS = 1000000

private val threadBuilder: Thread.Builder = Thread.ofPlatform()

/**
 * Test class illustrating common errors due to the use of shared mutable date
 * by more than one thread.
 */
class ThreadingHazardsTests {

    /**************************************************************************
     * This test illustrates the hazards associated to
     * mutable data sharing between threads, in this case insertions into a linked list.
     */
    // Just a simple stack using a linked list
    class SimpleLinkedStack<T> {

        private class Node<T>(val item: T, val next: Node<T>?)

        // mutable
        private var head: Node<T>? = null

        fun push(value: T) {
            head = Node(item = value, next = head)
        }

        fun pop(): T? {
            val observedHead = head ?: return null
            head = observedHead.next
            return observedHead.item
        }

        val isEmpty: Boolean
            get() = head == null
    }

    // Note:  `nonThreadSafeList` reference is immutable, however the referenced data structure is mutable
    private val nonThreadSafeList = SimpleLinkedStack<Int>()

    @Test
    fun `loosing items on a linked list`() {
        val l: Lock = ReentrantLock()
        // when: creating N_OF_THREADS that each insert '1' N_OF_REPS times on the list
        val threads = List(N_OF_THREADS) {
            threadBuilder.start {
                // note that this code runs in a different thread
                repeat(N_OF_REPS) {
                    l.lock()
                    nonThreadSafeList.push(1)
                    l.unlock()
                }
            }
        }

        // and: waiting for these threads to end
        threads.forEach(Thread::join)

        // and: counting the number of elements in the list
        var acc = 0
        while (!nonThreadSafeList.isEmpty) {
            val elem = nonThreadSafeList.pop()
            checkNotNull(elem)
            acc += elem
        }

        // then: the number of elements is NOT N_OF_THREADS * N_OF_REPS
        assertNotEquals(N_OF_THREADS * N_OF_REPS, acc)
    }


    /**************************************************************************
     * This test illustrates the hazards associated to
     * mutable data sharing between threads, in this case insertions into a linked list with a Lock.
     */
    // Just a simple stack using a linked list with a lock
    class LockSimpleLinkedStack<T> {

        private class Node<T>(val item: T, val next: Node<T>?)

        // mutable
        private var head: Node<T>? = null

        private var size: Int = 0

        private val lock: Lock = ReentrantLock()

        fun push(value: T) {
            lock.lock()
            head = Node(item = value, next = head)
            size++
            lock.unlock()
        }

        fun pop(): T? {
            lock.lock()
            val observedHead = head
            if ( observedHead == null ) {
                lock.unlock()
                return null
            }else {
                head = observedHead.next
                size--
                lock.unlock()
                return observedHead.item
            }
        }

        fun getSize(): Int {
            return size
        }

        val isEmpty: Boolean
            get() = lock.withLock {  //Faz algo, adquirindo o Lock no inicio e libertando-o no fim.
                head == null
            }
    }

    // Note:  `nonThreadSafeList` reference is immutable, however the referenced data structure is mutable
    private val threadSafeListLock = LockSimpleLinkedStack<Int>()

    @Test
    fun `loosing items on a linked list with a lock`() {
        // when: creating N_OF_THREADS that each insert '1' N_OF_REPS times on the list
        val threads = List(N_OF_THREADS) {
            threadBuilder.start {
                // note that this code runs in a different thread
                repeat(N_OF_REPS) {
                    threadSafeListLock.push(1)
                }
            }
        }

        // and: waiting for these threads to end
        threads.forEach(Thread::join)

        // and: counting the number of elements in the list
        while (!threadSafeListLock.isEmpty) {
            val elem = threadSafeListLock.pop()
            checkNotNull(elem)
        }

        // then: the number of elements is NOT N_OF_THREADS * N_OF_REPS
        assertNotEquals(N_OF_THREADS * N_OF_REPS, threadSafeListLock.getSize())
    }


    /**
     * Ex da Serie 1, nao estava a dar para correr no outro projeto
     */
/*
    class CountDownLatch(private var count: Int) {
        init {
            require(count > 0) { "count must be above zero." }
        }

        private var done = false
        private val lock = ReentrantLock()
        private val condition = lock.newCondition()

        fun countDown() {
            lock.withLock {
                count--
                if (count == 0) {
                    done = true
                    condition.signalAll()
                }
            }
        }

        fun await(){
            lock.withLock {
                if (done)
                    return
                while (count != 0)
                    condition.await()
            }
        }
    }
*/
    @Test
    fun `the test`() {
        val cdl: CountDownLatch = CountDownLatch(1)
        // when: creating N_OF_THREADS that each insert '1' N_OF_REPS times on the list
        var idx = 0
        val threads = List(N_OF_THREADS) {
            threadBuilder.start {
                // note that this code runs in a different thread
                if (idx++ < N_OF_THREADS-1) {
                    cdl.await()
                    threadSafeListLock.push(1)
                }
                else {
                    try {
                        assertEquals(0,threadSafeListLock.getSize())
                    }
                    catch (e: Exception){
                        println(e.message)

                    }
                    finally {
                        cdl.countDown()
                    }
                }
            }
        }

        // and: waiting for these threads to end
        threads.forEach(Thread::join)

        assertEquals(N_OF_THREADS-1,threadSafeListLock.getSize())

        // and: counting the number of elements in the list
        while (!threadSafeListLock.isEmpty) {
            val elem = threadSafeListLock.pop()
            checkNotNull(elem)
        }

        // then: the number of elements is NOT N_OF_THREADS * N_OF_REPS
        assertEquals(0, threadSafeListLock.getSize())
    }
}