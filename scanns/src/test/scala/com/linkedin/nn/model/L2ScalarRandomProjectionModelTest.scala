/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn.model

import com.linkedin.nn.distance.DistanceMetric
import com.linkedin.nn.linalg.RandomProjection
import com.linkedin.nn.lsh.ScalarRandomProjectionHashFunction
import org.apache.spark.ml.linalg.{DenseMatrix, Vectors}
import org.testng.Assert
import org.testng.annotations.Test

class L2ScalarRandomProjectionModelTest {
  @Test
  def testGetBandedHashes(): Unit = {
    val rps = Array(
      new ScalarRandomProjectionHashFunction(new RandomProjection(DenseMatrix.eye(3)), 0.5),
      new ScalarRandomProjectionHashFunction(new RandomProjection(DenseMatrix.diag(Vectors.dense(1.0, 2.0, -1.0))), 0.5)
    ) // implicit here that signature length is 3 and numHashes is 6
    val model = new L2ScalarRandomProjectionModel(hashFunctions = rps)
    model.set(model.bucketWidth, 0.5)
    Assert.assertEquals(model.distance.metric, DistanceMetric.l2)
    Assert.assertEquals(
      model.getBandedHashes(Vectors.dense(1.2, -1.7, 0.5)),
      Array(Array(2, -4, 1), Array(2, -7, -1))
    )
  }
}
