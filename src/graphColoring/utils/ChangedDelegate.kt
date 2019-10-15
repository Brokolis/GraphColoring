package graphColoring.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ChangedDelegate<T>(var lastValue: T) : ReadWriteProperty<Any?, T> {

    var value: T = lastValue

    val changed get() = value != lastValue

    fun save() {
        lastValue = value
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }

}