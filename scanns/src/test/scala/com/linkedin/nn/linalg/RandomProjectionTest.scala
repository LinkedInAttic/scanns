/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn.linalg

import java.util.Random

import org.apache.spark.ml.linalg.{DenseMatrix, Vector, Vectors}
import org.testng.Assert
import org.testng.annotations.{DataProvider, Test}

class RandomProjectionTest {
  @DataProvider
  def dimensionGenerator(): Array[Array[Any]] = {
    val rand = new Random()
    Array(
      Array(25000, 16, rand),
      Array(100, 10, rand),
      Array(1000000, 20, rand)
    )
  }

  @Test(dataProvider = "dimensionGenerator")
  def testRandomProjectionGeneration(inputDim: Int, projectionDim: Int, rand: Random): Unit = {
    val rp = RandomProjection.generateGaussian(inputDim, projectionDim, rand)
    Assert.assertEquals(rp.matrix.numRows, projectionDim)
    Assert.assertEquals(rp.matrix.numCols, inputDim)
  }

  @DataProvider
  def matrixVectorGenerator(): Array[Array[Any]] = {
    Array(
      Array(
        DenseMatrix.eye(5),
        Vectors.dense(2.0, -1.7, 0.0, 1.2, 0.5),
        Vectors.dense(2.0, -1.7, 0.0, 1.2, 0.5),
        Array(1, 0, 0, 1, 1)
      ),
      Array(
        new DenseMatrix(3, 2, Array(-1.2, 0.2, 2.4, -0.5, 2.0, -1.0)),
        Vectors.dense(1.0, -1.0),
        Vectors.dense(-0.7, -1.8, 3.4),
        Array(0, 0, 1)
      )
    )
  }

  @Test(dataProvider = "matrixVectorGenerator")
  def testProjectSignProject(matrix: DenseMatrix,
                             inVec: Vector,
                             expectedProjection: Vector,
                             expectedSignProjection: Array[Int]): Unit = {
    val rp = new RandomProjection(matrix)
    Assert.assertEquals(rp.project(inVec), expectedProjection)
    Assert.assertEquals(rp.signProject(inVec), expectedSignProjection)
  }
}

