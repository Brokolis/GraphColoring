package graphColoring

import graphColoring.graph.ColoredNode
import graphColoring.graph.Graph
import graphColoring.graph.Node

fun Graph<*, *>.print() {
    fun nodeToString(node: Node<*, *>) = if (node is ColoredNode) "${node.value} (color=${node.color})" else node.value.toString()

    for (node in nodes) {
        println("${nodeToString(node)} (${node.neighbors.joinToString(", ", transform = { nodeToString(it) })})")
    }
}

fun <T : Comparable<T>> clamp(value: T, min: T, max: T): T {
    return if (value < min) min else if (value > max) max else value
}