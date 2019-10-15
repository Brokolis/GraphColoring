package graphColoring.fx.controls

class NumberGraphView : ColoredGraphView<Int>() {

	override fun itemProducer(): Int {
		if (data == null) return 0
		return (data!!.graph.nodes.lastOrNull()?.value ?: -1) + 1
	}

}