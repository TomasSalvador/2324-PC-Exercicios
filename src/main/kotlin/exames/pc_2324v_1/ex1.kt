package exames.pc_2324v_1

import java.util.concurrent.atomic.AtomicInteger

class SafeCyclicSuccession<T>(
    private val items: Array<T>
){

    private var index = AtomicInteger(0)

    fun nextB(): T {
        while (true) {
            // Outra forma
            val obsIdx = index.get()
            var newIdx = obsIdx+1
            if (newIdx == items.size) {
                newIdx = 0
            }
            if (index.compareAndSet(obsIdx,newIdx)) {
                return items[obsIdx]
            }
            /* Como eu fiz.
            var changed = false
            if (obsIdx + 1 == items.size) {
                changed = index.compareAndSet(obsIdx, 0)
            } else {
                changed = index.compareAndSet(obsIdx, obsIdx + 1)
            }
            if (changed) {
                return items[obsIdx]
            }*/
        }
    }
}