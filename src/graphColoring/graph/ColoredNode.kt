package graphColoring.graph

import graphColoring.Cloneable
import java.util.*

class ColoredNode<T : Comparable<T>>(
        override var value: T,
        var color: Int = -1,
        override val neighbors: SortedSet<ColoredNode<T>> = sortedSetOf()
) : Node<T, ColoredNode<T>> {

    override fun compareTo(other: ColoredNode<T>): Int {
        return value.compareTo(other.value)
    }

    /**
     * Clones the node by cloning the value if value is Cloneable, doesn't clone neighbors
     */
    override fun clone(): ColoredNode<T> {
        var value = this.value

        if (value is Cloneable<*>) {
            @Suppress("UNCHECKED_CAST")
            value = value.clone() as T
        }

        return ColoredNode(value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColoredNode<*>) return false

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "ColoredNode(value=$value, color=$color, neighbors=${neighbors.size})"
    }

}