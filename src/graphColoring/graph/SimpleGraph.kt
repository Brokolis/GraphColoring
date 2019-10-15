package graphColoring.graph

import java.util.*

class SimpleGraph<T : Comparable<T>>(nodes: SortedSet<SimpleNode<T>> = sortedSetOf()) : AbstractGraph<T, SimpleNode<T>>(nodes) {

    override fun produceNode(item: T) = SimpleNode(item)
    override fun produceNode(item: T, neighbors: SortedSet<SimpleNode<T>>) = SimpleNode(item, neighbors)

    override fun clone(): SimpleGraph<T> {
        return SimpleGraph(cloneNodes())
    }

    override fun toString(): String {
        return "SimpleGraph(nodes=$nodes)"
    }

}