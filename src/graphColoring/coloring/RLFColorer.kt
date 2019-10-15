package graphColoring.coloring

import graphColoring.graph.ColoredNode
import graphColoring.graph.Graph
import java.util.*

/**
 * SimpleGraph coloring implemented as RLF (Recursive Largest First) algorithm
 */
class RLFColorer : GraphColorer {

	override val name: String = "Recursive Largest First"
	override val description: String = "Selects a color and finds an uncolored node with the biggest degree, paints its neighbors of neighbors. Continues with a neighbor which has the biggest degree."

	override fun <T : Comparable<T>> color(graph: Graph<T, ColoredNode<T>>) {
		val nodes = graph.nodes

		val coloredNodes: SortedSet<ColoredNode<T>> = sortedSetOf() // C
		var workingNodes: SortedSet<ColoredNode<T>> = nodes.toSortedSet() // V'
		var neighborNodes: SortedSet<ColoredNode<T>> = sortedSetOf() // U
		var currentColor = 0 // q

		while (coloredNodes.size != nodes.size) {
			if (workingNodes.isEmpty()) { // for multiple components, lets add all the non-colored nodes
				for (node in nodes) {
					if (!coloredNodes.contains(node)) {
						workingNodes.add(node)
					}
				}

				currentColor = 0 // reset the color for the new component
			}

			var node = maxDegree(workingNodes, workingNodes) // v

			while (true) {
				node.color = currentColor

				workingNodes.remove(node)
				coloredNodes.add(node)

				removeNeighbors(node, workingNodes, neighborNodes)

				if (workingNodes.isEmpty()) break

				node = maxDegree(workingNodes, neighborNodes)
			}

			workingNodes = neighborNodes
			neighborNodes = sortedSetOf()
			currentColor++
		}
	}

	private fun <T : Comparable<T>> maxDegree(nodes: SortedSet<ColoredNode<T>>, neighbors: SortedSet<ColoredNode<T>>): ColoredNode<T> {
		var node: ColoredNode<T>? = null
		var degree = 0

		for (otherNode in nodes) {
			val otherDegree = getDegree(otherNode, neighbors)

			if (node === null || otherDegree > degree) {
				node = otherNode
				degree = otherDegree
			}
		}

		return node!!
	}

	private fun <T : Comparable<T>> getDegree(node: ColoredNode<T>, nodes: SortedSet<ColoredNode<T>>): Int {
		var degree = 0

		for (neighbor in node.neighbors) {
			if (nodes.contains(neighbor)) {
				degree++
				break
			}
		}

		return degree
	}

	private fun <T : Comparable<T>> removeNeighbors(node: ColoredNode<T>, nodes: SortedSet<ColoredNode<T>>, removedNodes: SortedSet<ColoredNode<T>>) {
		for (neighbor in node.neighbors) {
			val iterator = nodes.iterator()
			for (otherNode in iterator) {
				if (otherNode == neighbor) {
					iterator.remove()
					removedNodes.add(otherNode)
					break
				}
			}
		}
	}

}