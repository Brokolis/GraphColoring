package graphColoring

import graphColoring.coloring.*
import graphColoring.fx.HueColorResolver
import graphColoring.graph.ColoredGraph
import graphColoring.graph.ColoredNode
import graphColoring.graph.Graph
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.random.nextInt

val nodeCount = 10
val neighborCount = 0..2
val runAmount = 1000000

var graph: Graph<Int, ColoredNode<Int>> = ColoredGraph()

fun main() {
    clearGraph()
    fillGraph()

    println("Original graph:")
    graph.print()
    println()

    for (graphColorer in graphColorers) {
        println("Colored by ${graphColorer.name}:")
        println("Running for $runAmount iterations...")

        val startTime = System.nanoTime()
        for (i in 1..runAmount) {
            graphColorer.reset(graph)
            graphColorer.color(graph)
        }
        val endTime = System.nanoTime()

        println("Calculations took: ${(endTime - startTime) * 1e-9} s")
        println()

        /*graph.print()
        println()*/
    }
}

fun fillGraph() {
    val nodeRange = 1..nodeCount
    val nodes: List<Int> = nodeRange.toList()

    for (nodeIndex in nodeRange) {
        graph.add(nodes[nodeIndex - 1], nodeRange.minus(nodeIndex).shuffled().take(Random.nextInt(neighborCount)).map { nodes[it - 1] })
        /*if (nodeIndex < nodes.size)
            graph.add(nodes[nodeIndex - 1], listOf(nodes[nodeIndex]))
        else
            graph.add(nodes[nodeIndex - 1])*/
    }
}

fun clearGraph() {
    graph.clear()
}
