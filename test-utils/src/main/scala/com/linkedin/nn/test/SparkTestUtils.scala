/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn.test

import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession


trait SparkTestUtils {

  var sparkSession: SparkSession = _
  var sc: SparkContext = _

  /**
   * Provides a synchronized block for methods to safely create their own Spark contexts without stomping on others.
   * Users are expected to handle Spark context creation and cleanup correctly.
   *
   * @param name The test job name
   * @param body The execution closure
   */
  def sparkTestSelfServeContext(name: String)(body: => Unit): Unit = {
    SparkTestUtils.SPARK_LOCAL_CONFIG.synchronized {
      try {
        body
      } finally {
        System.clearProperty("spark.driver.port")
        System.clearProperty("spark.hostPort")
      }
    }
  }

  /**
   * Provides a synchronized block with an auto-created safe Spark context. This wrapper will handle both creation and
   * cleanup of the context.
   *
   * @param name The test job name
   * @param body The execution closure
   */
  def sparkTest(name: String)(body: => Unit): Unit = {
    SparkTestUtils.SPARK_LOCAL_CONFIG.synchronized {

      sparkSession = SparkSession.builder().master(SparkTestUtils.SPARK_LOCAL_CONFIG).appName(name).getOrCreate()
      sc = sparkSession.sparkContext

      try {
        body
      } finally {
        sc.stop()
        sparkSession.stop()
        System.clearProperty("spark.driver.port")
        System.clearProperty("spark.hostPort")
      }
    }
  }
}

object SparkTestUtils {
  val log: Logger = LogManager.getLogger(classOf[SparkTestUtils])

  val SPARK_LOCAL_CONFIG: String = "local[*]"
}
