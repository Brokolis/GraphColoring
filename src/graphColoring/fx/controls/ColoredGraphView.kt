package graphColoring.fx.controls

import graphColoring.coloring.GraphColorer
import graphColoring.coloring.graphColorers
import graphColoring.graph.ColoredNode
import graphColoring.graph.Graph
import graphColoring.utils.EventHandler
import graphColoring.utils.addHandler
import graphColoring.utils.invoke
import graphColoring.utils.removeHandler
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.text.Text
import javafx.util.Duration

abstract class ColoredGraphView<T : Comparable<T>> : GraphView<T, ColoredNode<T>>() {

	protected val colorComboBox: ComboBox<GraphColorer>
	protected val colorText: Text

	val colorers: ObservableList<GraphColorer> = FXCollections.observableArrayList(graphColorers)

	protected val colorHandlers: MutableList<EventHandler<() -> Unit>> = mutableListOf()

	init {
		toolbar.children.apply {
			add(HBox().apply {
				children.add(Separator())

				colorComboBox = ComboBox<GraphColorer>(colorers).apply {
					setCellFactory {
						createColorCell()
					}

					buttonCell = createColorCell()

					selectionModel.selectFirst()
				}
				children.add(colorComboBox)

				children.add(Button().apply {
					text = "Color"
					setOnAction(::clickColor)
				})
			})
		}

		footerTextContainer.children.apply {
			add(HBox().apply {
				children.add(Text("Colors: "))
				colorText = Text("0")
				children.add(colorText)
			})
		}
	}

	override fun createGraph(graph: Graph<T, ColoredNode<T>>) {
		super.createGraph(graph)

		graphRenderer?.onChange {
			colorText.text = kotlin.run {
				val colors: MutableSet<Int> = mutableSetOf()
				for (node in graph.nodes) colors.add(node.color)
				colors.size.toString()
			}
		}
	}

	private fun createColorCell(): ListCell<GraphColorer> {
		return object : ListCell<GraphColorer>() {
			override fun updateItem(item: GraphColorer?, empty: Boolean) {
				super.updateItem(item, empty)
				text = item?.name
				tooltip = Tooltip(item?.description).apply {
					isWrapText = true
					showDelay = Duration.ZERO
					showDuration = Duration.INDEFINITE
					maxWidth = 300.0
				}
			}
		}
	}

	protected fun clickColor(event: ActionEvent) {
		data?.apply {
			colorComboBox.selectionModel.selectedItem?.let { colorer ->
				colorer.reset(graph)
				colorer.color(graph)
				renderer.resolveColors()
				colorHandlers.invoke { it() }
			}
		}
	}

	fun onColor(name: String? = null, handler: () -> Unit) = addHandler(colorHandlers, name, handler)
	fun removeColorHandler(name: String?) = removeHandler(colorHandlers, name)
	fun removeColorHandler(handler: () -> Unit) = removeHandler(colorHandlers, handler)

}