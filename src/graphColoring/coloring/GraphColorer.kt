package graphColoring.coloring

import graphColoring.graph.ColoredNode
import graphColoring.graph.Graph

interface GraphColorer {

    val name: String
    val description: String

    fun <T : Comparable<T>> color(graph: Graph<T, ColoredNode<T>>)

    fun <T : Comparable<T>> reset(graph: Graph<T, ColoredNode<T>>) {
        for (node in graph.nodes) {
            node.color = -1
        }
    }

}