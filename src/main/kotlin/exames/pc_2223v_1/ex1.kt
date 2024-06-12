package exames.pc_2223v_1

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class UnsafeSuccession<T>(
    private val items: Array<T>
){
    private var index = 0
    fun next(): T? =
        if(index < items.size) {
            items[index++]
        } else {
            null
        }
}

// a)
class UnsafeSuccessionA<T>(
    private val items: Array<T>
){
    private var index = 0
    private val lock = ReentrantLock()

    fun next(): T? =
        lock.withLock {
            if (index < items.size) {
                items[index++]
            } else {
                null
            }
        }

}

// b)
class UnsafeSuccessionB<T>(
    private val items: Array<T>
){
    private var index = AtomicInteger()

    fun next(): T? {
        while (true) {
            val observedIdx = index.get()
            if (observedIdx < items.size) {
                if ( index.compareAndSet(observedIdx, observedIdx + 1) ) {
                    return items[observedIdx]
                }
            } else {
                return null
            }
        }
    }

}