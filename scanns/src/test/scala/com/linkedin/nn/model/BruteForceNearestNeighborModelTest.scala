/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn.model

import com.linkedin.nn.Types.Item
import com.linkedin.nn.algorithm.BruteForceNNS
import com.linkedin.nn.test.SparkTestUtils
import org.apache.spark.SparkContext
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.rdd.RDD
import org.testng.Assert
import org.testng.annotations.Test

class BruteForceNearestNeighborModelTest extends SparkTestUtils {
  def generateSquareDataset(sc: SparkContext, idOffset: Long, xCoordinateOffset: Double): RDD[Item] = {
    sc.parallelize(Array(
      (idOffset + 0, Vectors.dense(xCoordinateOffset + 0.0, 0.0)),
      (idOffset + 1, Vectors.dense(xCoordinateOffset + 0.0, 1.0)),
      (idOffset + 2, Vectors.dense(xCoordinateOffset + 1.0, 1.0)),
      (idOffset + 3, Vectors.dense(xCoordinateOffset + 1.0, 0.0))
    ))
  }

  @Test
  def testGetAllNearestNeighbors(): Unit = sparkTest("testGetAllNearestNeighbors") {
    val src = generateSquareDataset(sc, 0, 1.0)
    val candidates = generateSquareDataset(sc, 4, 2.0)
    val nbrs = new BruteForceNNS()
      .setDistanceMetric("l2")
      .createModel()
      .getAllNearestNeighbors(src, candidates, 2)
      .map(x => (x._1, x._2))
      .collect()
      .toSet
    Assert.assertTrue(nbrs.contains((0L, 4L)))
    Assert.assertTrue(nbrs.contains((0L, 5L)))
    Assert.assertTrue(nbrs.contains((1L, 4L)))
    Assert.assertTrue(nbrs.contains((1L, 5L)))
    Assert.assertTrue(nbrs.contains((2L, 4L)))
    Assert.assertTrue(nbrs.contains((2L, 5L)))
    Assert.assertTrue(nbrs.contains((3L, 4L)))
    Assert.assertTrue(nbrs.contains((3L, 5L)))
  }

  @Test
  def testGetNearestNeighbors(): Unit = sparkTest("testGetNearestNeighbors") {
    val data = generateSquareDataset(sc, 0, 1.0)
    val query = Vectors.dense(1.8, 0.0)
    val nbrs = new BruteForceNNS()
      .setDistanceMetric("l2")
      .createModel()
      .getNearestNeighbors(query, data, 3)
      .map(x => x._1)
      .toSet
    Assert.assertEquals(nbrs.size, 3)
    Assert.assertTrue(nbrs.contains(0))
    Assert.assertTrue(nbrs.contains(2))
    Assert.assertTrue(nbrs.contains(3))
  }
}
