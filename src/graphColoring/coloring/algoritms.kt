package graphColoring.coloring

val graphColorers: List<GraphColorer> = listOf(
		RLFColorer(),
		RSFColorer(),
		FastColorer()
)