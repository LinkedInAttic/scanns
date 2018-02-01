/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn.model

import com.linkedin.nn.distance.DistanceMetric
import com.linkedin.nn.lsh.MinHashFunction
import org.apache.spark.ml.linalg.Vectors
import org.testng.Assert
import org.testng.annotations.Test

class JaccardMinHashModelTest {
  @Test
  def testGetBandedHashes(): Unit = {
    val minhashes = Array(
      new MinHashFunction(Array((5, -2), (3, 1))),
      new MinHashFunction(Array((-2, -1), (1, 0)))
    )
    val model = new JaccardMinHashModel(hashFunctions = minhashes)
    model.set(model.signatureLength, 2)
    Assert.assertEquals(model.distance.metric, DistanceMetric.jaccard)
    Assert.assertEquals(
      model.getBandedHashes(Vectors.sparse(4, Seq((0, 1.0), (3, -1.0)))),
      Array(Array(3, 4), Array(-9, 1))
    )
  }
}
