package graphColoring.layout

import graphColoring.Vector2D
import graphColoring.clamp
import graphColoring.graph.Graph
import graphColoring.graph.Node
import graphColoring.utils.ChangedDelegate
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Represents a drawable graph on a 2D surface.
 *
 * Force-Directed SimpleGraph drawing, spring-based system
 *
 * Each edge is a spring, between every other node that are not connected is a less powerful spring.
 * Also, each node is being bushed towards the middle of the drawing area.
 *
 * @see Graph
 */
class GraphLayout<T : Comparable<T>, N : Node<T, N>>(
		val graph: Graph<T, N>,

		width: Double = 0.0,
		height: Double = 0.0,

		var edgeForce: Double = 10.0,
		var nodeForce: Double = 20.0,
		var centerForce: Double = 1.0,

		var degeneration: Double = 5.0,

		var forceModifier: Double = 2.0,
		var minDistance: Double = 30.0,

		var accelStopThreshold: Double = 0.1
) {

	private val widthDelegate = ChangedDelegate(width)
	var width: Double by widthDelegate

	private val heightDelegate = ChangedDelegate(height)
	var height: Double by heightDelegate

	private var halfWidth: Double = 0.0
	private var halfHeight: Double = 0.0

	private var preferredDistance: Double = 0.0

	private val nodeComparator = kotlin.Comparator<NodeLayout<T, N>> { a, b ->
		a.node.compareTo(b.node)
	}

	val nodes: SortedSet<NodeLayout<T, N>> = sortedSetOf(nodeComparator)

	private var lastNodesSize: Int = 0

	init {
		calculateSizeParameters()
	}

	fun createNode(item: T, x: Double, y: Double): NodeLayout<T, N>? {
		if (nodes.any { it.node.value == item }) return null

		val node = graph.add(item)
		val nodeLayout = NodeLayout(node, nodeComparator, x, y)

		nodes.add(nodeLayout)

		return nodeLayout
	}

	fun removeNode(item: T): NodeLayout<T, N>? {
		graph.remove(item)

		val node = nodes.find { it.node.value == item } ?: return null
		val iterator = nodes.iterator()

		for (otherNode in iterator) {
			if (otherNode == node) {
				iterator.remove()
			} else {
				otherNode.neighbors.remove(node)
			}
		}

		return node
	}

	fun connectNodes(item1: T, item2: T): Pair<NodeLayout<T, N>, NodeLayout<T, N>>? {
		val node1 = graph.add(item1, listOf(item2))
		val node2 = node1.neighbors.find { it.value == item2 }

		var node1Layout: NodeLayout<T, N>? = null
		var node2Layout: NodeLayout<T, N>? = null

		for (node in nodes) {
			if (node.node == node1) {
				node1Layout = node
			} else if (node.node == node2) {
				node2Layout = node
			}

			if (node1Layout != null && node2Layout != null) break
		}

		if (node1Layout != null && node2Layout != null) {
			node1Layout.neighbors.add(node2Layout)
			node2Layout.neighbors.add(node1Layout)

			return Pair(node1Layout, node2Layout)
		}

		return null
	}

	fun disconnectNodes(item1: T, item2: T): Pair<NodeLayout<T, N>, NodeLayout<T, N>>? {
		graph.disconnect(item1, item2)

		var node1Layout: NodeLayout<T, N>? = null
		var node2Layout: NodeLayout<T, N>? = null

		for (node in nodes) {
			if (node.node.value == item1) {
				node1Layout = node
			} else if (node.node.value == item2) {
				node2Layout = node
			}

			if (node1Layout != null && node2Layout != null) break
		}

		if (node1Layout != null && node2Layout != null) {
			node1Layout.neighbors.remove(node2Layout)
			node2Layout.neighbors.remove(node1Layout)

			return Pair(node1Layout, node2Layout)
		}

		return null
	}

	private fun calculateSizeParameters() {
		halfWidth = width * 0.5
		halfHeight = height * 0.5
		preferredDistance = (max(min(width, height) / 3 - minDistance, 0.0)) * Math.pow(1.0 - 1.0 / 50.0, nodes.size.toDouble()) + minDistance
	}

	/**
	 * Calculates and applies acceleration.
	 */
	fun update(deltaTime: Double): Boolean {
		val center = Vector2D.Zero

		if (widthDelegate.changed || heightDelegate.changed || lastNodesSize != nodes.size) {
			calculateSizeParameters()
			widthDelegate.save()
			heightDelegate.save()
			lastNodesSize = nodes.size
		}

		var updated = false

		for (node in nodes) {
			val originalPosition = node.position

			if (node.applyAccel) {
				var accel = Vector2D.Zero

				node.position = Vector2D(clamp(node.position.x, -halfWidth, halfWidth), clamp(node.position.y, -halfHeight, halfHeight))

				accel += applyAttraction(node.position, center, 0.0) * centerForce // attract to center

				var neighborAccel = Vector2D.Zero
				for (neighbor in node.neighbors) { // apply direct edge forces
					neighborAccel += applyAttraction(node.position, neighbor.position, preferredDistance, true)
				}
				accel += neighborAccel * edgeForce

				var otherNodeAccel = Vector2D.Zero
				for (otherNode in nodes) { // apply non-connected node forces
					if (otherNode != node && !node.neighbors.contains(otherNode) && (node.position - otherNode.position).length < preferredDistance) {
						otherNodeAccel += applyAttraction(node.position, otherNode.position, preferredDistance, true)
					}
				}
				accel += otherNodeAccel * nodeForce

				node.accel = node.accel + accel * forceModifier * deltaTime

				val degenerationAccel = node.accel * degeneration * deltaTime
				if (node.accel.length > degenerationAccel.length) {
					node.accel -= degenerationAccel
				} else {
					node.accel = Vector2D(0.0, 0.0)
				}

				if (node.accel.length > accelStopThreshold) {
					node.position += node.accel * deltaTime
				} else {
					node.accel = Vector2D.Zero
				}
			}

			if (node.position.x < -halfWidth || node.position.x > halfWidth) {
				node.accel = Vector2D(0.0, node.accel.y)
			}

			if (node.position.y < -halfHeight || node.position.y > halfHeight) {
				node.accel = Vector2D(node.accel.x, 0.0)
			}

			node.position = Vector2D(clamp(node.position.x, -halfWidth, halfWidth), clamp(node.position.y, -halfHeight, halfHeight))

			if (node.position != originalPosition) {
				updated = true
			}
		}

		return updated
	}

	/**
	 * Creates an attraction or repulsion force based on preferred distance.
	 *
	 * The zero distance fix will apply small amount of acceleration in a random direction
	 * if the distance between two points is exactly zero.
	 */
	private fun applyAttraction(point1: Vector2D, point2: Vector2D, preferredDistance: Double, zeroDistanceFix: Boolean = false): Vector2D {
		val displacement = point2 - point1

		if (zeroDistanceFix && displacement.length == 0.0) {
			return Vector2D(Random.nextDouble(), Random.nextDouble()) * Random.nextDouble() * 10.0
		}

		return displacement.normalize() * (displacement.length - preferredDistance)
	}

	fun reset() {
		nodes.clear()

		for (node in graph.nodes) {
			//val x = 0
			val x = Random.nextDouble(-width / 2, width / 2)
			//val y = 0
			val y = Random.nextDouble(-height / 2, height / 2)
			nodes.add(NodeLayout(node, nodeComparator, x, y))
		}

		for (node in graph.nodes) {
			nodes.find { it.node == node }?.neighbors?.addAll(node.neighbors.map { neighbor ->
				nodes.find { it.node == neighbor }
			})
		}
	}

}