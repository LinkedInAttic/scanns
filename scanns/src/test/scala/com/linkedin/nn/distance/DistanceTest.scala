/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn.distance

import org.apache.spark.ml.linalg.{DenseVector, SparseVector, Vector}
import org.testng.Assert
import org.testng.annotations.{DataProvider, Test}

class DistanceTest {
  import DistanceTest._

  @DataProvider
  def vectorDistances(): Array[Array[Any]] = {
    Array(
      Array("jaccard", v1, v2, 0.8),
      Array("jaccard", v1, v3, 0.25),
      Array("jaccard", v2, v3, 0.75),
      Array("cosine", v1, v2, 0.434),
      Array("cosine", v1, v3, 0.913),
      Array("cosine", v2, v3, 0.691),
      Array("l2", v1, v2, 2.958),
      Array("l2", v1, v3, 3.240),
      Array("l2", v2, v3, 2.179)
    )
  }

  @Test(dataProvider = "vectorDistances")
  def testDistance(metric: String, v1: Vector, v2: Vector, expectedDistance: Double): Unit = {
    Assert.assertEquals(DistanceMetric.getDistance(metric).compute(v1, v2), expectedDistance, DistanceTest.tolerance)
  }
}

object DistanceTest {
  val v1 = new DenseVector(Array(1.0, -2.0, 0.5, 0.0, -1.0))
  val v2 = new SparseVector(5, Array(1, 3), Array(0.5, -0.5))
  val v3 = new SparseVector(5, Array(0, 1, 4), Array(0.5, 1.0, -2.0))

  val tolerance = 0.001
}
