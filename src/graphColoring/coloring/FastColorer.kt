package graphColoring.coloring

import graphColoring.graph.ColoredNode
import graphColoring.graph.Graph

class FastColorer : GraphColorer {

    override val name: String = "Fast"
    override val description: String = "Selects a color and, starting from the first node it finds, paints all the nodes it can with that color."

    override fun <T : Comparable<T>> color(graph: Graph<T, ColoredNode<T>>) {
        var color = 0
        var coloredNodes = 0

        while (coloredNodes < graph.nodes.size) {
            for (node in graph.nodes) {
                if (node.color == -1) {
                    var hasSameColoredNeighbor = false

                    for (neighbor in node.neighbors) {
                        if (neighbor.color == color) {
                            hasSameColoredNeighbor = true
                            break
                        }
                    }

                    if (!hasSameColoredNeighbor) {
                        node.color = color
                        coloredNodes++
                    }
                }
            }

            color++
        }
    }
}