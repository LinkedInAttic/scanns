/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn.algorithm

import com.linkedin.nn.model.BruteForceNearestNeighborModel
import com.linkedin.nn.params.BruteForceNNSParams
import org.apache.spark.ml.param.{ParamMap, Params}
import org.apache.spark.ml.util.Identifiable

class BruteForceNNS(override val uid: String = Identifiable.randomUID("BruteForceNNS"))
  extends BruteForceNNSParams with Serializable {

  def setDistanceMetric(metric: String): this.type = {
    set(distanceMetric -> metric)
  }

  def createModel(): BruteForceNearestNeighborModel = {
    val model = new BruteForceNearestNeighborModel(uid)
    copyValues(model)
    model
  }

  override def copy(extra: ParamMap): Params = defaultCopy(extra)
}
