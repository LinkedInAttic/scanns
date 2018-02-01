/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn.utils

import org.testng.Assert
import org.testng.annotations.{DataProvider, Test}

class TopNQueueTest {
  @DataProvider
  def queueDataProvider(): Array[Array[Any]] = {
    Array(
      Array(3, Array((0L, 1.0), (1L, 2.0))),
      Array(3, Array((0L, 1.0), (1L, 2.0), (-1L, 3.0))),
      Array(2, Array((0L, 1.0), (1L, 2.0), (-1L, 3.0))),
      Array(2, Array((0L, 1.0), (1L, 2.0), (-1L, 3.0), (1L, 2.0)))
    )
  }

  @Test(dataProvider = "queueDataProvider")
  def testTopNQueue(capacity: Int, elements: Array[(Long, Double)]): Unit = {
    val queue = new TopNQueue(capacity)
    elements.foreach(queue.enqueue(_))
    val expectedElements = elements.sorted(Ordering.by[(Long, Double), Double](x => x._2)).take(capacity)
    Assert.assertTrue(expectedElements.toSet == queue.priorityQ.toSet)
  }
}
