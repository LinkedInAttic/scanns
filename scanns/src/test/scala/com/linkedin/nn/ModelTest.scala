/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn

import com.linkedin.nn.algorithm.CosineSignRandomProjectionNNS
import com.linkedin.nn.lsh.SignRandomProjectionHashFunction
import com.linkedin.nn.test.SparkTestUtils
import com.linkedin.nn.utils.TopNQueue
import org.apache.spark.ml.linalg.Vectors
import org.testng.annotations.Test

/* A simple end-to-end run of a nearest neighbor search using the driver */
class ModelTest extends SparkTestUtils {
  // @Test
  // This is unable to be run currently due to some PriorityQueue serialization issues
  def testModel(): Unit = sparkTest("modelTest") {
    sc.getConf.registerKryoClasses(Array(classOf[TopNQueue], classOf[SignRandomProjectionHashFunction]))

    val file = "src/test/resources/nn/example.tsv"
    val data = sc.textFile(file)
      .map { line =>
        val split = line.split(" ")
        (split.head, Vectors.dense(split.tail.map(_.toDouble)))
      }
      .zipWithIndex
    val words = data.map { case (x, y) => (y, x._1) }
    val items = data.map { case (x, y) => (y, x._2) }
    words.cache()
    items.cache()
    val numFeatures = items.values.take(1)(0).size

    val model = new CosineSignRandomProjectionNNS()
      .setNumHashes(200)
      .setSignatureLength(10)
      .setBucketLimit(10)
      .setJoinParallelism(5)
      .createModel(numFeatures)
    val nbrs = model.getSelfAllNearestNeighbors(items, 10)
    nbrs.count()
  }
}
