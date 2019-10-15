package graphColoring.graph

import java.util.*

class ColoredGraph<T : Comparable<T>>(nodes: SortedSet<ColoredNode<T>> = sortedSetOf()) : AbstractGraph<T, ColoredNode<T>>(nodes) {

    override fun produceNode(item: T) = ColoredNode(item)
    override fun produceNode(item: T, neighbors: SortedSet<ColoredNode<T>>) = ColoredNode(item, neighbors = neighbors)

    override fun clone(): ColoredGraph<T> {
        return ColoredGraph(cloneNodes())
    }

    override fun toString(): String {
        return "SimpleGraph(nodes=$nodes)"
    }

}