/*
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn.build.plugins

/**
 * This class provides utils to resolve Scala version string into the suffix string
 * used for cross-build purpose.
 *  The resolution rule is:
 *   'x.y.z' -->  '_x.y'   (if x == 2 and y >= 10)
 *                '_x.y.z' (otherwise)
 */
class ScalaUtils {
  static def getScalaVersionSuffix(scalaVersion) {
    def tokens = scalaVersion.split('\\.')
    if (tokens == null || tokens.length < 3) {
      throw new RuntimeException ("Illegal scalaVersion string: [$scalaVersion]. " +
          "A valid version string should in the format of [x].[y].[z] .");
    }

    if (tokens[0] == '2' && Integer.parseInt(tokens[1]) >= 10) {
      return "_${tokens[0]}.${tokens[1]}"
    } else {
      return "_$scalaVersion"
    }
  }
}
