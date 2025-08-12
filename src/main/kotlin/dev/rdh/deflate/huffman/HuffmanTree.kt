package dev.rdh.deflate.huffman

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue
import kotlin.math.max

/**
 * A Huffman tree for encoding and decoding symbols based on their frequencies.
 * @param frequencies The frequencies of each symbol, where the index is the symbol and the value is its frequency.
 * @param limit The maximum depth of the tree. Codes will not exceed this length.
 */
class HuffmanTree @JvmOverloads constructor(frequencies: IntArray, limit: Int = Int.MAX_VALUE) {
    private val numSymbols = frequencies.size

    // A map of which level of the tree corresponds to which leaf nodes
    private val depths = Object2ObjectAVLTreeMap<Int, MutableList<LeafNode>>()
    private var maxDepth = 0
    private val root = getTreeRoot(frequencies)

    init {
        buildDepths()
        while (maxDepth > limit) {
            // keep on trying to balance the tree until the tree is shorter than the limit
            balanceDepths()
        }
    }

    /**
     * Actually constructs the tree
     * @return the root node of the tree
     */
    private fun getTreeRoot(frequencies: IntArray): Node {
        val queue = ObjectHeapPriorityQueue<Node>()
        for (i in frequencies.indices) {
            if (frequencies[i] > 0) {
                queue.enqueue(LeafNode(i, frequencies[i]))
            }
        }

        // ensure at least two leaves
        when (queue.size()) {
            0 -> {
                queue.enqueue(LeafNode(0, 1))
                val s1 = if (numSymbols > 1) 1 else 0
                queue.enqueue(LeafNode(s1, 1))
            }
            1 -> {
                // add a second (dummy) symbol within bounds, different if possible
                val s0 = (queue.first() as LeafNode).symbol
                val s1 = if (numSymbols > 1) (if (s0 != 0) 0 else 1) else s0
                queue.enqueue(LeafNode(s1, 1))
            }
        }

        // build the tree
        while (queue.size() > 1) {
            val left = queue.dequeue()
            val right = queue.dequeue()
            queue.enqueue(InternalNode(left, right))
        }

        return queue.dequeue()
    }

    /**
     * Builds [depths] and [maxDepth] from the tree
     */
    private fun buildDepths() {
        // clear the old depths
        depths.clear()
        maxDepth = 0

        val queue = ObjectArrayFIFOQueue<Pair<Node, Int>>()
        queue.enqueue(Pair(root, 0))

        // BFS to build the depths
        while (!queue.isEmpty) {
            val (node, depth) = queue.dequeue()

            if (node is InternalNode) {
                queue.enqueue(Pair(node.left, depth + 1))
                queue.enqueue(Pair(node.right, depth + 1))
            } else {
                val list = depths.getOrPut(depth) { ObjectArrayList() }
                list.add(node as LeafNode)
                maxDepth = max(maxDepth, depth)
            }
        }
    }

    /**
     * Balances the tree by one step by moving the deepest leaf to a higher level
     */
    private fun balanceDepths() {
        val selected = depths[maxDepth]!!.first()
        val parent = selected.parent!!
        val sibling = if (selected.side == 0) parent.right else parent.left
        sibling as LeafNode

        val grandparent = parent.parent!!

        // move sibling to the parent's place
        if (parent.side == Node.LEFT) {
            grandparent.left = sibling
            grandparent.left.parent = grandparent
            grandparent.left.side = Node.LEFT
        } else {
            grandparent.right = sibling
            grandparent.right.parent = grandparent
            grandparent.right.side = Node.RIGHT
        }

        var moved = false

        // find a place for the selected leaf
        for (i in maxDepth - 2 downTo 1) {
            val leavesAtDepth = depths[i] ?: continue

            // find the first leaf that is not a sibling, and turn it into an internal node
            // with that leaf and the selected leaf as the children
            val leafToMerge: LeafNode = leavesAtDepth.first()
            val parentOfMerge = leafToMerge.parent

            if (leafToMerge.side == Node.Companion.LEFT) {
                parentOfMerge!!.left = InternalNode(selected, leafToMerge)
                parentOfMerge.left.parent = parentOfMerge
                parentOfMerge.left.side = Node.Companion.LEFT
            } else {
                parentOfMerge!!.right = InternalNode(selected, leafToMerge)
                parentOfMerge.right.parent = parentOfMerge
                parentOfMerge.right.side = Node.Companion.RIGHT
            }

            moved = true
            break
        }

        // make sure the leaf was reinserted
        check(moved) { "Can't balance the tree within the specified limit, failed at depth ${maxDepth - 1}" }

        // rebuild the depths
        buildDepths()
    }

    fun toAlphabet(): HuffmanAlphabet {
        val lengths = IntArray(numSymbols)
        for (d in 0..maxDepth) {
            val leaves = depths[d] ?: continue
            for (leaf in leaves) {
                lengths[leaf.symbol] = d
            }
        }

        return HuffmanAlphabet.fromLengths(lengths)
    }

    private abstract class Node(val weight: Int) : Comparable<Node> {
        var parent: InternalNode? = null
        var side: Int = 0

        override fun compareTo(other: Node): Int {
            return weight.compareTo(other.weight)
        }

        companion object {
            const val LEFT: Int = 0
            const val RIGHT: Int = 1
        }
    }

    private class InternalNode(left: Node, right: Node) : Node(left.weight + right.weight) {
        var left: Node
        var right: Node

        init {
            left.parent = this
            left.side = 0
            right.parent = this
            right.side = 1


            this.left = left
            this.right = right
        }
    }

    private class LeafNode(val symbol: Int, weight: Int) : Node(weight)
}