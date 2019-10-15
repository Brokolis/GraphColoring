package graphColoring.graph

import graphColoring.Cloneable
import java.util.*

open class SimpleNode<T : Comparable<T>>(
        override var value: T,
        override val neighbors: SortedSet<SimpleNode<T>> = sortedSetOf()
) : Node<T, SimpleNode<T>> {

    override fun compareTo(other: SimpleNode<T>): Int {
        return value.compareTo(other.value)
    }

    /**
     * Clones the node by cloning the value if value is Cloneable, doesn't clone neighbors
     */
    override fun clone(): SimpleNode<T> {
        var value = this.value

        if (value is Cloneable<*>) {
            @Suppress("UNCHECKED_CAST")
            value = value.clone() as T
        }

        return SimpleNode(value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SimpleNode<*>

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "SimpleNode(value=$value, neighbors=${neighbors.size})"
    }

}