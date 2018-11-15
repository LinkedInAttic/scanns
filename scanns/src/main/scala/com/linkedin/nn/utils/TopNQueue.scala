/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn.utils

import com.linkedin.nn.Types.{ItemId, ItemIdDistancePair}

import scala.collection.{mutable, immutable}

import scala.collection.JavaConverters._
import java.util.PriorityQueue
import java.util.Comparator

/**
  *
  */
class pq[A](maxCapacity: Int, itemComparator: Comparator[A]) extends PriorityQueue[A](maxCapacity, itemComparator){
  def toSet: immutable.Set[A] = this.toArray.map(x => x.asInstanceOf[A]).toSet
  def reverseIterator: Iterator[A] = this.iterator.asScala.toList.reverseIterator
  def nonEmpty: Boolean = if(this.size() == 0) false else true
  def enqueue(elems: A*): Unit = {
    elems.foreach(e => this.add(e))
  }
  def dequeue(): A = this.poll()
  def head: A = this.peek()
}

class TopNQueue(maxCapacity: Int) extends Serializable{

  val ItemComparator: Comparator[ItemIdDistancePair] = new Comparator[ItemIdDistancePair](){
    override def compare(item1: ItemIdDistancePair, item2: ItemIdDistancePair): Int = {
      val diff: Double = item2._2 - item1._2
      if(diff > 0) return 1 else if(diff < 0) return -1 else return 0
    }
  }

  val priorityQ: pq[ItemIdDistancePair] = new pq[ItemIdDistancePair](maxCapacity, ItemComparator)
  val elements: mutable.HashSet[ItemId] = mutable.HashSet[ItemId]() // for deduplication

  /**
    * Enqueue elements in the queue
    * @param elems The elements to enqueue
    */
  def enqueue(elems: ItemIdDistancePair*): this.type = {
    elems.foreach { x =>
      if (!elements.contains(x._1)) {
        if (priorityQ.size < maxCapacity) {
          priorityQ.enqueue(x)
          elements.add(x._1)
        } else {
          if (priorityQ.head._2 > x._2) {
            elements.remove(priorityQ.dequeue()._1)
            priorityQ.enqueue(x)
            elements.add(x._1)
          }
        }
      }
    }
    this
  }

  def nonEmpty(): Boolean = priorityQ.nonEmpty

  def iterator(): Iterator[ItemIdDistancePair] = priorityQ.reverseIterator
}


/**
  * This is a simple wrapper around the scala [[mutable.PriorityQueue]] that allows it to only hold a fixed number of
  * elements. By default, [[mutable.PriorityQueue]] behaves as a max-priority queue i.e as a max heap. [[TopNQueue]]
  * can be used to get smallest-n elements in a streaming fashion.
  *
  * We also deduplicate the contents based on the first value of the tuple ([[ItemId]] id).
  *
  * @param maxCapacity max number of elements the queue will hold
  */
class TopNQueue_original(maxCapacity: Int) extends Serializable {

  val priorityQ: mutable.PriorityQueue[ItemIdDistancePair] =
    mutable.PriorityQueue[ItemIdDistancePair]()(Ordering.by[ItemIdDistancePair, Double](_._2))
  val elements: mutable.HashSet[ItemId] = mutable.HashSet[ItemId]() // for deduplication

  /**
    * Enqueue elements in the queue
    * @param elems The elements to enqueue
    */
  def enqueue(elems: ItemIdDistancePair*): Unit = {
    elems.foreach { x =>
      if (!elements.contains(x._1)) {
        if (priorityQ.size < maxCapacity) {
          priorityQ.enqueue(x)
          elements.add(x._1)
        } else {
          if (priorityQ.head._2 > x._2) {
            elements.remove(priorityQ.dequeue()._1)
            priorityQ.enqueue(x)
            elements.add(x._1)
          }
        }
      }
    }
  }

  def nonEmpty(): Boolean = priorityQ.nonEmpty

  def iterator(): Iterator[ItemIdDistancePair] = priorityQ.reverseIterator
}
