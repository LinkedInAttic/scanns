/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn.model

import com.linkedin.nn.distance.DistanceMetric
import com.linkedin.nn.linalg.RandomProjection
import com.linkedin.nn.lsh.SignRandomProjectionHashFunction
import org.apache.spark.ml.linalg.{DenseMatrix, Vectors}
import org.testng.Assert
import org.testng.annotations.Test

class CosineSignRandomProjectionModelTest {
  @Test
  def testGetBandedHashes(): Unit = {
    val rps = Array(
      new SignRandomProjectionHashFunction(new RandomProjection(DenseMatrix.eye(3))),
      new SignRandomProjectionHashFunction(new RandomProjection(DenseMatrix.diag(Vectors.dense(1.0, 2.0, -1.0))))
    ) // implicit here that signature length is 3 and numHashes is 6
    val model = new CosineSignRandomProjectionModel(hashFunctions = rps)
    Assert.assertEquals(model.distance.metric, DistanceMetric.cosine)
    Assert.assertEquals(model.getBandedHashes(Vectors.dense(1.0, 2.0, 3.0)), Array(Array(7), Array(6)))
  }
}
