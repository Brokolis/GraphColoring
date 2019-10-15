package graphColoring.graph

import graphColoring.Cloneable
import java.util.*

/**
 * This represents a non-oriented graph.
 *
 * The internal structure of the graph is entirely exposed for performance reasons, so be careful to not mess it up.
 * DON'T edit the relationships of nodes directly, use the provided methods for that.
 */
interface Graph<T : Comparable<T>, N : Node<T, N>> : Cloneable<Graph<T, N>> {

    val nodes: SortedSet<N>

    /**
     * Creates a node for the specified item if it doesn't exist already.
     */
    fun add(item: T): N

    /**
     * Creates a node for the specified item if it doesn't exist already,
     * creates a node for each neighbor if the neighbor doesn't exist and
     * creates the relationships between the item and the neighbors.
     */
    fun add(item: T, neighbors: Collection<T>): N

    /**
     * Removes the node of the specified item if it exists and removes any relationships related to the node.
     */
    fun remove(item: T)

    /**
     * Removes the relationship between the two nodes of the items if the nodes exist.
     */
    fun disconnect(item1: T, item2: T)

    /**
     * Removes all nodes.
     */
    fun clear()

}