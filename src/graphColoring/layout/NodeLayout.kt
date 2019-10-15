package graphColoring.layout

import graphColoring.graph.Node
import graphColoring.Vector2D
import java.util.*

/**
 * Represents a drawable node on a 2D surface.
 *
 * Force-Directed SimpleGraph drawing
 *
 * @see GraphLayout
 */
data class NodeLayout<T : Comparable<T>, N : Node<T, N>>(
        val node: N,
        val neighbors: SortedSet<NodeLayout<T, N>> = sortedSetOf(),
        var position: Vector2D = Vector2D.Zero,
        var accel: Vector2D = Vector2D.Zero,

        var applyAccel: Boolean = true
) {
    constructor(node: N, comparator: Comparator<NodeLayout<T, N>>, x: Double, y: Double) : this(node, sortedSetOf(comparator), Vector2D(x, y))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NodeLayout<*, *>) return false

        if (node != other.node) return false

        return true
    }

    override fun hashCode(): Int {
        return node.hashCode()
    }

}