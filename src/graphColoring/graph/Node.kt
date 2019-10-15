package graphColoring.graph

import graphColoring.Cloneable
import java.util.*

/**
 * Represents a node of a graph.
 *
 * @see Graph
 */
interface Node<T : Comparable<T>, N : Node<T, N>> : Comparable<N>, Cloneable<N> {

    var value: T
    val neighbors: SortedSet<N>

}