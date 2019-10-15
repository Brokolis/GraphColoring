package graphColoring.fx.graph

import graphColoring.Vector2D
import graphColoring.fx.ColorResolver
import graphColoring.fx.HueColorResolver
import graphColoring.graph.ColoredNode
import graphColoring.layout.GraphLayout
import graphColoring.layout.NodeLayout
import graphColoring.utils.EventHandler
import graphColoring.utils.addHandler
import graphColoring.utils.invoke
import graphColoring.utils.removeHandler
import javafx.geometry.Pos
import javafx.scene.input.MouseButton
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.shape.StrokeType
import javafx.scene.text.Text
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import graphColoring.graph.Node as GraphNode

/**
 * TODO: Combine some render and simulation tasks into one, rename simulationContext to calculationContext.
 * TODO: Use the new calculation context only for large calculations (e.g. layout.update())
 */
class GraphRenderer<T : Comparable<T>, N : GraphNode<T, N>>(
		private val container: Pane,
		private val fpsUpdate: (deltaTime: Double) -> Unit,
		private val graphLayout: GraphLayout<T, N>,

		private val itemProducer: () -> T,

		// this thread will be used for all graph layout method calls
		private val simulationContext: CoroutineContext = newSingleThreadContext("simulation thread"),
		// the main context everything will run in
		private val mainContext: CoroutineContext = Dispatchers.Main
) {

	var lineViewOrder: Double = 2.0
	var nodeViewOrder: Double = 1.0

	var nodeStrokeWidth: Double = 0.0
	var selectedNodeStrokeWidth: Double = 3.0

	var nodeRadius: Double = 15.0

	var fps: Int = 120

	var colorResolver: ColorResolver = HueColorResolver()

	var currentAction: RendererAction = RendererAction.SELECT
		set(value) {
			field = value

			GlobalScope.launch(mainContext) {
				tasks.send(SelectNodeTask(null))
			}
		}
	private var selectedNode: Node? = null

	// holds all tasks that need to be done by the main loop
	private var tasks: Channel<Task> = Channel(Channel.UNLIMITED)

	// the main loop of all launched coroutines
	private var mainLoop: Job

	// indicates whether the simulation of the graph should currently be running
	var simulationEnabled = false
		set(value) {
			if (field == value) return

			field = value

			if (field) {
				updateTime = true
			}
		}


	val edgesSize: Int
		get() = edges.size
	val nodesSize: Int
		get() = nodes.size

	private val edges: MutableList<Edge> = mutableListOf()
	private val nodes: MutableList<Node> = mutableListOf()

	private val changeHandlers: MutableList<EventHandler<() -> Unit>> = mutableListOf()

	private var lastTime: Long = 0
	private var updateTime = false

	init {
		container.widthProperty().addListener { _, _, newValue ->
			graphLayout.width = max(newValue.toDouble() - 2 * nodeRadius, 0.0)
		}
		container.heightProperty().addListener { _, _, newValue ->
			graphLayout.height = max(newValue.toDouble() - 2 * nodeRadius, 0.0)
		}

		container.setOnMousePressed { event ->
			if (currentAction == RendererAction.EDIT_NODES) {
				if (event.isPrimaryButtonDown) {
					GlobalScope.launch(mainContext) {
						tasks.send(CreateNodeSimulationTask(event.x - container.width * 0.5, event.y - container.height * 0.5))
					}
				}
			}

			GlobalScope.launch(mainContext) {
				tasks.send(SelectNodeTask(null))
			}

			container.requestFocus()
			event.consume()
		}

		graphLayout.width = max(container.width - 2 * nodeRadius, 0.0)
		graphLayout.height = max(container.height - 2 * nodeRadius, 0.0)

		graphLayout.minDistance = 4 * nodeRadius

		produceNodes()
		produceEdges()

		mainLoop = GlobalScope.launch(mainContext) {
			while (true) {
				val startTime = System.nanoTime()

				while (!tasks.isEmpty) {
					val task = tasks.receive()

					when (task) {
						is RenderTask -> task.doTask()
						is SimulationTask -> withContext(simulationContext) { task.doTask() }
					}
				}

				if (simulationEnabled) {
					if (updateTime) {
						lastTime = System.nanoTime()
						updateTime = false
					} else {
						withContext(simulationContext) {
							UpdateSimulationTask(System.nanoTime()).doTask()
						}
					}
				}

				// ceil the result
				val endTime = System.nanoTime()
				val delayAmount = 1000L / fps
				val duration = (endTime - startTime + 500_000) / 1_000_000

				if (delayAmount > duration) {
					delay(delayAmount - duration)
				} else {
					yield()
				}
			}
		}
	}

	fun destroy() {
		mainLoop.cancel()
	}

	fun resolveColors() {
		GlobalScope.launch(mainContext) {
			tasks.send(ColorNodesTask())
		}
	}

	fun reset() {
		GlobalScope.launch(mainContext) {
			tasks.send(ResetSimulationTask())
		}
	}

	private fun calculateEdgePos(line: Line, first: NodeLayout<T, N>, second: NodeLayout<T, N>) {
		line.apply {
			startX = first.position.x + container.width * 0.5
			startY = first.position.y + container.height * 0.5
			endX = second.position.x + container.width * 0.5
			endY = second.position.y + container.height * 0.5
		}
	}

	private fun produceEdge(first: NodeLayout<T, N>, second: NodeLayout<T, N>): Edge {
		val line = Line().apply {
			fill = Color.BLUE
			viewOrder = lineViewOrder

			calculateEdgePos(this, first, second)
		}

		return Edge(first, second, line)
	}

	private fun calculateNodePosition(circle: Circle, node: NodeLayout<T, N>) {
		circle.apply {
			centerX = node.position.x + container.width * 0.5
			centerY = node.position.y + container.height * 0.5
		}
	}

	private fun produceNode(node: NodeLayout<T, N>): Node {
		val circle = Circle().apply {
			radius = nodeRadius
			viewOrder = nodeViewOrder
			strokeType = StrokeType.OUTSIDE
			stroke = Color.ORANGE
			strokeWidth = nodeStrokeWidth

			fill = if (node.node is ColoredNode<*>) {
				colorResolver.resolve(node.node.color)
			} else {
				Color.GREEN
			}

			calculateNodePosition(this, node)
		}

		val nodeContainer = StackPane().apply {
			alignment = Pos.CENTER
		}

		val text = Text().apply {
			fill = Color.WHITE
			text = node.node.value.toString()
		}

		nodeContainer.children.add(text)
		nodeContainer.relocate(
				circle.centerX - nodeContainer.width / 2,
				circle.centerY - nodeContainer.height / 2)

		circle.centerXProperty().addListener { _, _, newValue ->
			nodeContainer.layoutX = newValue as Double - nodeContainer.width / 2 + nodeContainer.layoutBounds.minX
		}

		circle.centerYProperty().addListener { _, _, newValue ->
			nodeContainer.layoutY = newValue as Double - nodeContainer.height / 2 + nodeContainer.layoutBounds.minY
		}

		return setupNodeEvents(Node(node, circle, nodeContainer))
	}

	private fun setupNodeEvents(node: Node): Node {
		node.nodeContainer.isMouseTransparent = true

		var dragInProgress = false

		node.circle.setOnMousePressed { event ->
			var select = currentAction != RendererAction.EDIT_NODES
			val selectedNode = selectedNode

			if (currentAction == RendererAction.EDIT_NODES) {
				if (event.isSecondaryButtonDown) {
					GlobalScope.launch(mainContext) {
						tasks.send(RemoveNodeSimulationTask(node.node.node.value))
					}
					event.consume()
				}
			} else if (currentAction == RendererAction.EDIT_EDGES) {
				if (event.isPrimaryButtonDown || event.isSecondaryButtonDown) {
					if (selectedNode != null && selectedNode != node) {
						val isNeighbors = node.node.neighbors.contains(selectedNode.node)
						val node1 = node.node.node.value
						val node2 = selectedNode.node.node.value

						if (isNeighbors && event.isSecondaryButtonDown) {
							GlobalScope.launch(mainContext) {
								tasks.send(DisconnectNodesSimulationTask(node1, node2))
								tasks.send(SelectNodeTask(null))
							}
							select = false
							event.consume()
						} else if (!isNeighbors && event.isPrimaryButtonDown) {
							GlobalScope.launch(mainContext) {
								tasks.send(ConnectNodesSimulationTask(node1, node2))
								tasks.send(SelectNodeTask(null))
							}
							select = false
							event.consume()
						}
					}
				}
			}

			if (select && (event.isPrimaryButtonDown || event.isSecondaryButtonDown)) {
				GlobalScope.launch(mainContext) {
					tasks.send(SelectNodeTask(node))
				}

				event.consume()
			}
		}

		node.circle.setOnMouseDragged { event ->
			if (currentAction == RendererAction.SELECT) {
				if (event.isPrimaryButtonDown) {
					GlobalScope.launch(mainContext) {
						tasks.send(object : RenderTask {
							override suspend fun doTask() {
								node.node.applyAccel = false
								node.node.position = Vector2D(event.x - container.width * 0.5, event.y - container.height * 0.5)
								if (!simulationEnabled) {
									UpdatePositionsTask().doTask()
								}
							}
						})
					}
					event.consume()
				}
			}
		}

		node.circle.setOnDragDetected { event ->
			val selectedNode = selectedNode

			if (currentAction == RendererAction.EDIT_EDGES) {
				if (selectedNode == node && (event.isPrimaryButtonDown || event.isSecondaryButtonDown)) {
					node.circle.startFullDrag()
					dragInProgress = true
					event.consume()
				}
			}
		}

		node.circle.setOnMouseDragEntered { event ->
			val selectedNode = selectedNode

			if (currentAction == RendererAction.EDIT_EDGES) {
				if (selectedNode != null) {
					if (event.isPrimaryButtonDown) {
						GlobalScope.launch(mainContext) {
							tasks.send(object : RenderTask {
								override suspend fun doTask() {
									if (node.node.neighbors.all { it != selectedNode.node }) {
										tasks.send(ConnectNodesSimulationTask(node.node.node.value, selectedNode.node.node.value))
										tasks.send(SelectNodeTask(node))
									}
								}
							})
						}
						event.consume()
					} else if (event.isSecondaryButtonDown) {
						GlobalScope.launch(mainContext) {
							tasks.send(object : RenderTask {
								override suspend fun doTask() {
									if (node.node.neighbors.any { it == selectedNode.node }) {
										tasks.send(DisconnectNodesSimulationTask(node.node.node.value, selectedNode.node.node.value))
										tasks.send(SelectNodeTask(node))
									}
								}
							})
						}
						event.consume()
					}
				}
			}
		}

		node.circle.setOnMouseReleased { event ->
			if (currentAction == RendererAction.SELECT) {
				if (event.button == MouseButton.PRIMARY) {
					GlobalScope.launch(mainContext) {
						tasks.send(object : RenderTask {
							override suspend fun doTask() {
								node.node.applyAccel = true
								node.node.accel = Vector2D.Zero
							}
						})
					}
					event.consume()
				}
			} else if (currentAction == RendererAction.EDIT_EDGES) {
				if (dragInProgress) {
					dragInProgress = false
					GlobalScope.launch(mainContext) {
						tasks.send(SelectNodeTask(null))
					}
					event.consume()
				}
			}
		}

		return node
	}

	private fun produceEdges() {
		for (nodeLayout in graphLayout.nodes) {
			for (neighborLayout in nodeLayout.neighbors) {
				if (!edges.any { it.first == nodeLayout && it.second == neighborLayout || it.first == neighborLayout && it.second == nodeLayout }) {
					val edgeData = produceEdge(nodeLayout, neighborLayout)
					edges.add(edgeData)
					container.children.add(edgeData.line)
				}
			}
		}
	}

	private fun produceNodes() {
		for (nodeLayout in graphLayout.nodes) {
			if (!nodes.any { it.node == nodeLayout }) {
				val nodeData = produceNode(nodeLayout)
				nodes.add(nodeData)
				container.children.add(nodeData.circle)
				container.children.add(nodeData.nodeContainer)
			}
		}
	}

	private inner class Edge(var first: NodeLayout<T, N>, var second: NodeLayout<T, N>, var line: Line) {

		operator fun component1() = first
		operator fun component2() = second
		operator fun component3() = line

	}

	private inner class Node(var node: NodeLayout<T, N>, var circle: Circle, var nodeContainer: Region) {

		operator fun component1() = node
		operator fun component2() = circle
		operator fun component3() = nodeContainer

	}

	/**
	 * Represents a task to be run only inside the main loop
	 */

	private interface Task {
		suspend fun doTask()
	}

	/**
	 * Render tasks
	 */

	private interface RenderTask : Task

	private inner class UpdatePositionsTask : RenderTask {
		override suspend fun doTask() {
			for (edge in edges) {
				val (first, second, line) = edge
				calculateEdgePos(line, first, second)
			}

			for (nodeData in nodes) {
				val (nodeLayout, circle) = nodeData
				calculateNodePosition(circle, nodeLayout)
			}
		}
	}

	private inner class ResetRenderTask : RenderTask {
		override suspend fun doTask() {
			for (node in nodes) {
				container.children.removeAll(node.circle, node.nodeContainer)
			}

			for (edge in edges) {
				container.children.remove(edge.line)
			}

			nodes.clear()
			edges.clear()

			produceNodes()
			produceEdges()

			changeHandlers.invoke { it() }
		}
	}

	private inner class CreateNodeRenderTask(val nodeLayout: NodeLayout<T, N>) : RenderTask {
		override suspend fun doTask() {
			val node = produceNode(nodeLayout)
			nodes.add(node)
			container.children.addAll(node.circle, node.nodeContainer)
			changeHandlers.invoke { it() }
		}
	}

	private inner class RemoveNodeRenderTask(val nodeLayout: NodeLayout<T, N>) : RenderTask {
		override suspend fun doTask() {
			var neighborCount = nodeLayout.neighbors.size

			val nodeIterator = nodes.iterator()
			for (node in nodeIterator) {
				if (node.node == nodeLayout) {
					container.children.remove(node.nodeContainer)
					container.children.remove(node.circle)
					nodeIterator.remove()
					break
				}
			}

			if (neighborCount > 0) {
				val edgeIterator = edges.iterator()
				for (edge in edgeIterator) {
					if (edge.first == nodeLayout || edge.second == nodeLayout) {
						container.children.remove(edge.line)
						edgeIterator.remove()
						neighborCount--

						if (neighborCount == 0) break
					}
				}
			}

			changeHandlers.invoke { it() }
		}
	}

	private inner class ConnectNodesRenderTask(val node1: NodeLayout<T, N>, val node2: NodeLayout<T, N>) : RenderTask {
		override suspend fun doTask() {
			@Suppress("UNCHECKED_CAST")
			val edge = produceEdge(node1, node2)
			edges.add(edge)
			container.children.add(edge.line)
			changeHandlers.invoke { it() }
		}
	}

	private inner class DisconnectNodesRenderTask(val node1: NodeLayout<T, N>, val node2: NodeLayout<T, N>) : RenderTask {
		override suspend fun doTask() {
			val edgeIterator = edges.iterator()
			for (edge in edgeIterator) {
				if (edge.first == node1 && edge.second == node2 || edge.first == node2 && edge.second == node1) {
					container.children.remove(edge.line)
					edgeIterator.remove()
					break
				}
			}
			changeHandlers.invoke { it() }
		}
	}

	private inner class ColorNodesTask : RenderTask {
		override suspend fun doTask() {
			for (nodeData in nodes) {
				val (node, circle) = nodeData

				if (node.node is ColoredNode<*>) {
					circle.fillProperty().set(colorResolver.resolve(node.node.color))
				}
			}

			changeHandlers.invoke { it() }
		}
	}

	private inner class SelectNodeTask(val node: Node?) : RenderTask {
		override suspend fun doTask() {
			selectedNode?.circle?.strokeWidth = nodeStrokeWidth
			selectedNode = node
			selectedNode?.circle?.strokeWidth = selectedNodeStrokeWidth
			container.requestFocus()
		}
	}

	/**
	 * Simulation tasks
	 */

	private interface SimulationTask : Task

	private inner class UpdateSimulationTask(val time: Long) : SimulationTask {
		override suspend fun doTask() {
			val deltaTime = (time - lastTime) / 1_000_000_000.0
			lastTime = time

			GlobalScope.launch(mainContext) {
				fpsUpdate(deltaTime)
			}

			if (graphLayout.update(deltaTime)) {
				withContext(mainContext) {
					UpdatePositionsTask().doTask()
				}
			}
		}
	}

	private inner class ResetSimulationTask : SimulationTask {
		override suspend fun doTask() {
			graphLayout.reset()
			withContext(mainContext) {
				ResetRenderTask().doTask()
			}
		}
	}

	private inner class CreateNodeSimulationTask(val x: Double, val y: Double) : SimulationTask {
		override suspend fun doTask() {
			val node = graphLayout.createNode(itemProducer(), x, y)
			if (node != null) {
				withContext(mainContext) {
					CreateNodeRenderTask(node).doTask()
				}
			}
		}
	}

	private inner class RemoveNodeSimulationTask(val item: T) : SimulationTask {
		override suspend fun doTask() {
			val node = graphLayout.removeNode(item)
			if (node != null) {
				withContext(mainContext) {
					RemoveNodeRenderTask(node).doTask()
				}
			}
		}
	}

	private inner class ConnectNodesSimulationTask(val item1: T, val item2: T) : SimulationTask {
		override suspend fun doTask() {
			if (item1 == item2) return
			val nodePair = graphLayout.connectNodes(item1, item2)
			if (nodePair != null) {
				withContext(mainContext) {
					ConnectNodesRenderTask(nodePair.first, nodePair.second).doTask()
				}
			}
		}
	}

	private inner class DisconnectNodesSimulationTask(val item1: T, val item2: T) : SimulationTask {
		override suspend fun doTask() {
			if (item1 == item2) return
			val nodePair = graphLayout.disconnectNodes(item1, item2)
			if (nodePair != null) {
				withContext(mainContext) {
					DisconnectNodesRenderTask(nodePair.first, nodePair.second).doTask()
				}
			}
		}
	}

	fun onChange(name: String? = null, handler: () -> Unit) = addHandler(changeHandlers, name, handler)
	fun removeChangeHandler(name: String?) = removeHandler(changeHandlers, name)
	fun removeChangeHandler(handler: () -> Unit) = removeHandler(changeHandlers, handler)

	enum class RendererAction {
		SELECT,
		EDIT_NODES,
		EDIT_EDGES,
	}

}