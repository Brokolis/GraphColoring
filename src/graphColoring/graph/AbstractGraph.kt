package graphColoring.graph

import java.util.*

abstract class AbstractGraph<T : Comparable<T>, N : Node<T, N>>(nodes: SortedSet<N> = sortedSetOf()) : Graph<T, N> {

    override var nodes: SortedSet<N> = nodes
        protected set

    protected abstract fun produceNode(item: T): N
    protected abstract fun produceNode(item: T, neighbors: SortedSet<N>): N

    /**
     * Creates a node for the specified item if it doesn't exist already.
     */
    override fun add(item: T): N {
        var node = nodes.find { it.value == item }

        if (node != null) {
            return node
        }

        node = produceNode(item)
        nodes.add(node)
        return node
    }

    /**
     * Creates a node for the specified item if it doesn't exist already,
     * creates a node for each neighbor if the neighbor doesn't exist and
     * creates the relationships between the item and the neighbors.
     */
    override fun add(item: T, neighbors: Collection<T>): N {
        var node = nodes.find { it.value == item }

        if (node === null) { // node does not exist, create one
            node = produceNode(item)
            nodes.add(node)
        }

        for (neighbor in neighbors) {
            var neighborNode = nodes.find { it.value == neighbor }

            if (neighborNode === null) { // neighbor node doesn't exist, create one, add 'node' as its only neighbor
                neighborNode = produceNode(neighbor, sortedSetOf(node))
                nodes.add(neighborNode)
            } else { // neighbor node exists, add node as its neighbor
                neighborNode.neighbors.add(node)
            }

            node.neighbors.add(neighborNode) // add neighbor node as its node
        }

        return node
    }

    /**
     * Removes the node of the specified item if it exists and removes any relationships related to the node.
     */
    override fun remove(item: T) {
        val iterator = nodes.iterator()

        for (otherNode in iterator) {
            if (otherNode.value == item) { // found the node itself, remove it
                iterator.remove()
            } else {
                otherNode.neighbors.removeIf { it.value == item } // remove it from other nodes
            }
        }
    }

    /**
     * Removes the relationship between the two nodes of the items if the nodes exist.
     */
    override fun disconnect(item1: T, item2: T) {
        var node1Found = false
        var node2Found = false

        for (node in nodes) {
            if (node.value == item1) {
                if (!node.neighbors.removeIf { it.value == item2 }) return // item1 and item2 are not neighbors, exit
                node1Found = true
            } else if (node.value == item2) {
                if (!node.neighbors.removeIf { it.value == item1 }) return // item1 and item2 are not neighbors, exit
                node2Found = true
            }

            if (node1Found && node2Found) { // the connection is removed, exit
                return
            }
        }
    }

    /**
     * Removes all nodes.
     */
    override fun clear() {
        nodes.clear()
    }

    protected fun cloneNodes(): SortedSet<N> {
        val nodesCopy: MutableList<N> = mutableListOf()

        for (node in nodes) {
            nodesCopy.add(node.clone())
        }

        for ((i, node) in nodes.withIndex()) {
            val nodeCopy = nodesCopy[i]

            for (neighbor in node.neighbors) {
                for ((j, otherNode) in nodes.withIndex()) {
                    if (otherNode == neighbor) {
                        nodeCopy.neighbors.add(nodesCopy[j])
                        break
                    }
                }
            }
        }

        return nodesCopy.toSortedSet()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractGraph<*, *>) return false

        if (nodes != other.nodes) return false

        return true
    }

    override fun hashCode(): Int {
        return nodes.hashCode()
    }

}