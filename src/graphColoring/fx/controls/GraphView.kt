package graphColoring.fx.controls

import graphColoring.fx.graph.GraphRenderer
import graphColoring.graph.Graph
import graphColoring.graph.Node
import graphColoring.layout.GraphLayout
import graphColoring.utils.EventHandler
import graphColoring.utils.addHandler
import graphColoring.utils.invoke
import graphColoring.utils.removeHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.BorderPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.shape.Rectangle
import javafx.scene.text.Text
import javafx.util.StringConverter
import javafx.util.converter.IntegerStringConverter


abstract class GraphView<T : Comparable<T>, N : Node<T, N>> : BorderPane() {

	@FXML
	protected lateinit var canvas: Pane
	@FXML
	protected lateinit var toolbar: FlowPane
	@FXML
	protected lateinit var simulationBtn: ToggleButton
	@FXML
	protected lateinit var showFpsBtn: ToggleButton
	@FXML
	protected lateinit var modeGroup: ToggleGroup
	@FXML
	protected lateinit var numberOfNodesTxt: TextField
	@FXML
	protected lateinit var numberOfNeighborsTxt: TextField
	@FXML
	protected lateinit var fpsText: Text
	@FXML
	protected lateinit var nodesText: Text
	@FXML
	protected lateinit var edgesText: Text
	@FXML
	protected lateinit var footerTextContainer: HBox

	protected val numberOfNodesFormatter = TextFormatter(IntegerStringConverter(), 10) {
		if (Regex("^\\d*$").matches(it.text)) it else null
	}

	protected val numberOfNeighborsFormatter = TextFormatter(object : StringConverter<Array<Int>>() {
		override fun toString(other: Array<Int>?): String {
			return when (other?.size) {
				1 -> other[0].toString()
				2 -> if (other[0] == 0 && other[1] == 0) "" else if (other[0] == 0) other[1].toString() else "${other[0]}-${other[1]}"
				else -> ""
			}
		}

		override fun fromString(string: String?): Array<Int> {
			if (string == null || string.isEmpty()) return arrayOf(0, 0)

			val index = string.indexOf('-')

			if (index == -1)
				return arrayOf(0, string.toInt())

			return arrayOf(string.substring(0, index).toInt(), string.substring(index + 1).toInt())
		}

	}, arrayOf(0, 2)) {
		if (Regex("^\\d*-?\\d*$").matches(it.text)) it else null
	}

	var graph: Graph<T, N>? = null
		set(value) {
			if (data != null) return

			field = value

			createGraph(value!!)
		}

	val graphRenderer: GraphRenderer<T, N>?
		get() = data?.renderer

	protected var data: GraphData? = null

	protected val clearHandlers: MutableList<EventHandler<() -> Unit>> = mutableListOf()
	protected val generateHandlers: MutableList<EventHandler<(Int, Int, Int) -> Unit>> = mutableListOf()

	protected var interval = 200
	private var count = 0
	private var currentDelta = 0.0
	private var lastTime = System.currentTimeMillis()

	init {
		val loader = FXMLLoader(javaClass.getResource("/controls/GraphView.fxml"))
		@Suppress("LeakingThis")
		loader.setRoot(this)
		@Suppress("LeakingThis")
		loader.setController(this)
		loader.load<GraphView<T, N>>()

		val canvasClipArea = Rectangle()
		canvasClipArea.widthProperty().bind(canvas.widthProperty())
		canvasClipArea.heightProperty().bind(canvas.heightProperty())
		canvas.clip = canvasClipArea

		modeGroup.selectedToggleProperty().addListener { _, _, newValue ->
			setMode(newValue.userData as? String)
		}

		setMode(modeGroup.selectedToggle.userData as? String)

		numberOfNodesTxt.textFormatter = numberOfNodesFormatter
		numberOfNeighborsTxt.textFormatter = numberOfNeighborsFormatter
	}

	protected abstract fun itemProducer(): T

	@FXML
	protected fun clickClear() {
		data?.apply {
			clearHandlers.invoke { it() }
			renderer.reset()
		}
	}

	@FXML
	protected fun clickSimulation() {
		data?.apply {
			renderer.simulationEnabled = simulationBtn.isSelected
		}
	}

	@FXML
	protected fun clickShowFps() {
		data?.apply {
			if (showFpsBtn.isSelected) {
				count = 0
				currentDelta = 0.0
				lastTime = System.currentTimeMillis()
			}
		}
	}

	@FXML
	protected fun clickGenerate() {
		data?.apply {
			val numberOfNodes = numberOfNodesFormatter.value ?: return@apply
			val (minNumberOfNeighbors, maxNumberOfNeighbors) = numberOfNeighborsFormatter.value

			generateHandlers.invoke { it(numberOfNodes, minNumberOfNeighbors, maxNumberOfNeighbors) }
			renderer.reset()
		}
	}

	private fun setMode(mode: String?) {
		data?.apply {
			val newMode = when (mode) {
				"select" -> GraphRenderer.RendererAction.SELECT
				"editNodes" -> GraphRenderer.RendererAction.EDIT_NODES
				"editEdges" -> GraphRenderer.RendererAction.EDIT_EDGES
				else -> return
			}

			renderer.currentAction = newMode
		}
	}

	protected open fun createGraph(graph: Graph<T, N>) {
		val graphLayout = GraphLayout(graph)
		val graphRenderer = GraphRenderer(canvas, ::fpsUpdate, graphLayout, ::itemProducer)

		graphRenderer.simulationEnabled = simulationBtn.isSelected

		graphRenderer.onChange {
			nodesText.text = graphRenderer.nodesSize.toString()
			edgesText.text = graphRenderer.edgesSize.toString()
		}

		data = GraphData(graph, graphLayout, graphRenderer)
	}

	protected fun fpsUpdate(delta: Double) {
		if (!showFpsBtn.isSelected) return

		currentDelta += delta
		count++

		val currentTime = System.currentTimeMillis()

		if (currentTime - lastTime >= interval && currentDelta != 0.0) {
			fpsText.text = (count / currentDelta).toString()

			count = 0
			currentDelta = 0.0

			lastTime = currentTime
		}
	}

	fun onClear(name: String? = null, handler: () -> Unit) = addHandler(clearHandlers, name, handler)
	fun removeClearHandler(name: String?) = removeHandler(clearHandlers, name)
	fun removeClearHandler(handler: () -> Unit) = removeHandler(clearHandlers, handler)

	fun onGenerate(name: String? = null, handler: (nodesCount: Int, fromNeighbors: Int, toNeighbors: Int) -> Unit) = addHandler(generateHandlers, name, handler)
	fun removeGenerateHandler(name: String?) = removeHandler(generateHandlers, name)
	fun removeGenerateHandler(handler: (nodesCount: Int, fromNeighbors: Int, toNeighbors: Int) -> Unit) = removeHandler(generateHandlers, handler)

	protected inner class GraphData(
			val graph: Graph<T, N>,
			val layout: GraphLayout<T, N>,
			val renderer: GraphRenderer<T, N>
	)

}