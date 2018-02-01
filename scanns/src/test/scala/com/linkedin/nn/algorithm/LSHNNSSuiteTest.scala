/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn.algorithm

import com.linkedin.nn.distance.DistanceMetric
import com.linkedin.nn.distance.DistanceMetric.DistanceMetric
import com.linkedin.nn.model.LSHNearestNeighborSearchModel
import org.testng.Assert
import org.testng.annotations.{DataProvider, Test}

class LSHNNSSuiteTest {
  @DataProvider
  def nnsProvider(): Array[Array[Any]] = {
    Array(
      Array(
        new JaccardMinHashNNS("minhash")
          .setNumHashes(100)
          .setSignatureLength(10)
          .setJoinParallelism(10000)
          .setBucketLimit(5000)
          .setShouldSampleBuckets(true)
          .setNumOutputPartitions(30)
          .setSeed(1L),
        "minhash", 100, 10, 10000, 5000, true, 30, 1L, DistanceMetric.jaccard, 10),
      Array(
        new CosineSignRandomProjectionNNS("signrp")
          .setNumHashes(500)
          .setSignatureLength(25)
          .setJoinParallelism(7000)
          .setBucketLimit(100)
          .setNumOutputPartitions(1)
          .setSeed(2L),
        "signrp", 500, 25, 7000, 100, false, 1, 2L, DistanceMetric.cosine, 20),
      Array(
        new L2ScalarRandomProjectionNNS("signrp")
          .setNumHashes(85)
          .setSignatureLength(17)
          .setJoinParallelism(10)
          .setBucketLimit(500)
          .setNumOutputPartitions(3)
          .setSeed(3L),
        "signrp", 85, 17, 10, 500, false, 3, 3L, DistanceMetric.l2, 5)
    )
  }

  @Test(dataProvider = "nnsProvider")
  def testInitAndModelCreation(nns: LSHNearestNeighborSearch[_ <: LSHNearestNeighborSearchModel[_]],
                               expUid: String,
                               expNumHashes: Int,
                               expSignatureLength: Int,
                               expJoinParallelism: Int,
                               expBucketLimit: Int,
                               expShouldSampleBuckets: Boolean,
                               expNumOutputPartitions: Int,
                               expSeed: Long,
                               expDistanceMetric: DistanceMetric,
                               expNumSignatures: Int): Unit = {
    // test init
    Assert.assertEquals(nns.uid, expUid)
    Assert.assertEquals(nns.getNumHashes, expNumHashes)
    Assert.assertEquals(nns.getSignatureLength, expSignatureLength)
    Assert.assertEquals(nns.getJoinParallelism, expJoinParallelism)
    Assert.assertEquals(nns.getBucketLimit, expBucketLimit)
    Assert.assertEquals(nns.getShouldSampleBuckets, expShouldSampleBuckets)
    Assert.assertEquals(nns.getNumOutputPartitions, expNumOutputPartitions)
    Assert.assertEquals(nns.getSeed, expSeed)

    // test model creation
    val model = nns.createModel(1000)
    Assert.assertEquals(model.uid, expUid)
    Assert.assertEquals(model.getNumHashes, expNumHashes)
    Assert.assertEquals(model.getSignatureLength, expSignatureLength)
    Assert.assertEquals(model.getJoinParallelism, expJoinParallelism)
    Assert.assertEquals(model.getBucketLimit, expBucketLimit)
    Assert.assertEquals(model.getShouldSampleBuckets, expShouldSampleBuckets)
    Assert.assertEquals(model.getNumOutputPartitions, expNumOutputPartitions)
    Assert.assertEquals(model.getSeed, expSeed)
    Assert.assertEquals(model.distance.metric, expDistanceMetric)
    Assert.assertEquals(model.getHashFunctions.length, expNumSignatures)
  }
}
