/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn.algorithm

import com.linkedin.nn.distance.DistanceMetric
import org.testng.Assert
import org.testng.annotations.Test

class BruteForceNNSTest {
  @Test
  def testInit(): Unit = {
    val nns = new BruteForceNNS("foo").setDistanceMetric("l2")
    Assert.assertEquals(nns.uid, "foo")
    Assert.assertEquals(nns.getDistanceMetric, "l2")
  }

  @Test
  def testModelCreation(): Unit = {
    val model = new BruteForceNNS("foo").setDistanceMetric("l2").createModel()
    Assert.assertEquals(model.getDistanceMetric, "l2")
    Assert.assertEquals(model.distance.metric, DistanceMetric.l2)
  }
}
