package graphColoring.fx

import graphColoring.fx.controls.NumberGraphView
import graphColoring.graph.ColoredGraph
import javafx.scene.control.SplitPane
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import javafx.scene.layout.RowConstraints
import kotlin.random.Random
import kotlin.random.nextInt

class Home {

	lateinit var menuViewGroup: ToggleGroup

	lateinit var graphViewContainer: GridPane

	lateinit var view00: NumberGraphView
	lateinit var view01: NumberGraphView
	lateinit var view10: NumberGraphView
	lateinit var view11: NumberGraphView

	val views: MutableList<NumberGraphView> = mutableListOf()

	fun initialize() {
		views.addAll(arrayOf(view00, view01, view10, view11))

		views.forEach {
			initGraphView(it)
		}

		menuViewGroup.selectedToggleProperty().addListener { _, _, newValue ->
			selectViewCount((newValue.userData as String).toInt())
		}

		selectViewCount((menuViewGroup.selectedToggle.userData as String).toInt())
	}

	private fun selectViewCount(count: Int) {
		when(count) {
			1 -> {
				graphViewContainer.columnConstraints.apply {
					clear()
					add(ColumnConstraints().apply { percentWidth = 100.0 })
				}
				graphViewContainer.rowConstraints.apply {
					clear()
					add(RowConstraints().apply { percentHeight = 100.0 })
				}
				graphViewContainer.children.apply {
					clear()
					add(view00)
				}
			}
			2 -> {
				graphViewContainer.columnConstraints.apply {
					clear()
					add(ColumnConstraints().apply { percentWidth = 50.0 })
					add(ColumnConstraints().apply { percentWidth = 50.0 })
				}
				graphViewContainer.rowConstraints.apply {
					clear()
					add(RowConstraints().apply { percentHeight = 100.0 })
				}
				graphViewContainer.children.apply {
					clear()
					addAll(view00, view01)
				}
			}
			4 -> {
				graphViewContainer.columnConstraints.apply {
					clear()
					add(ColumnConstraints().apply { percentWidth = 50.0 })
					add(ColumnConstraints().apply { percentWidth = 50.0 })
				}
				graphViewContainer.rowConstraints.apply {
					clear()
					add(RowConstraints().apply { percentHeight = 50.0 })
					add(RowConstraints().apply { percentHeight = 50.0 })
				}
				graphViewContainer.children.apply {
					clear()
					addAll(view00, view01, view10, view11)
				}
			}
		}
	}

	private fun initGraphView(graphView: NumberGraphView) {
		val graph: ColoredGraph<Int> = ColoredGraph()

		graphView.onClear {
			graph.clear()
		}

		graphView.onGenerate { nodesCount, fromNeighbors, toNeighbors ->
			graph.clear()

			val nodeRange = 1..nodesCount
			val nodes: List<Int> = nodeRange.toList()
			val neighborRange = fromNeighbors..toNeighbors

			for (nodeIndex in nodeRange) {
				val neighbors =
					if (neighborRange.isEmpty())
						listOf()
					else
						nodeRange.minus(nodeIndex).shuffled().take(Random.nextInt(neighborRange)).map { nodes[it - 1] }

				graph.add(nodes[nodeIndex - 1], neighbors)
				/*if (nodeIndex < nodes.size)
					graph.add(nodes[nodeIndex - 1], listOf(nodes[nodeIndex]))
				else
					graph.add(nodes[nodeIndex - 1])*/
			}
		}

		graphView.graph = graph
	}

}