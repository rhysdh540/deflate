package dev.rdh.deflate.util

import dev.rdh.deflate.core.Match
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList

open class MatchList : MutableList<Match> {
    protected open val backing: IntList = IntArrayList()

    override val size: Int; get() = backing.size
    override fun isEmpty() = backing.isEmpty()
    override fun contains(element: Match) = backing.contains(element.packed)
    override fun containsAll(elements: Collection<Match>) = backing.containsAll(elements.map { it.packed })
    override fun get(index: Int) = Match(backing.getInt(index))
    override fun indexOf(element: Match) = backing.indexOf(element.packed)
    override fun lastIndexOf(element: Match) = backing.lastIndexOf(element.packed)
    override fun iterator(): MutableIterator<Match> = object : MutableIterator<Match> {
        private val it = backing.iterator()
        override fun hasNext() = it.hasNext()
        override fun next() = Match(it.nextInt())
        override fun remove() = it.remove()
    }
    override fun add(element: Match) = backing.add(element.packed)
    override fun remove(element: Match) = backing.rem(element.packed)
    override fun addAll(elements: Collection<Match>) = backing.addAll(elements.map { it.packed })
    override fun addAll(index: Int, elements: Collection<Match>) = backing.addAll(index, elements.map { it.packed })
    override fun removeAll(elements: Collection<Match>) = backing.removeAll(elements.map { it.packed })
    override fun retainAll(elements: Collection<Match>) = backing.retainAll(elements.map { it.packed })
    override fun clear() = backing.clear()
    override fun set(index: Int, element: Match) = Match(backing.set(index, element.packed))
    override fun add(index: Int, element: Match) = backing.add(index, element.packed)
    override fun removeAt(index: Int) = Match(backing.removeInt(index))
    override fun listIterator(): MutableListIterator<Match> = object : MutableListIterator<Match> {
        private val it = backing.listIterator()
        override fun hasNext() = it.hasNext()
        override fun next() = Match(it.nextInt())
        override fun hasPrevious() = it.hasPrevious()
        override fun previous() = Match(it.previousInt())
        override fun nextIndex() = it.nextIndex()
        override fun previousIndex() = it.previousIndex()
        override fun remove() = it.remove()
        override fun set(element: Match) = it.set(element.packed)
        override fun add(element: Match) = it.add(element.packed)
    }
    override fun listIterator(index: Int): MutableListIterator<Match> = object : MutableListIterator<Match> {
        private val it = backing.listIterator(index)
        override fun hasNext() = it.hasNext()
        override fun next() = Match(it.nextInt())
        override fun hasPrevious() = it.hasPrevious()
        override fun previous() = Match(it.previousInt())
        override fun nextIndex() = it.nextIndex()
        override fun previousIndex() = it.previousIndex()
        override fun remove() = it.remove()
        override fun set(element: Match) = it.set(element.packed)
        override fun add(element: Match) = it.add(element.packed)
    }

    override fun subList(fromIndex: Int, toIndex: Int): MatchList {
        val newBacking = backing.subList(fromIndex, toIndex)
        return object : MatchList() {
            override val backing = newBacking
        }
    }
}