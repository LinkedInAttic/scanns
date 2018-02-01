/*
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn.build.plugins

/**
 * An extension to configure ScalaResolverPlugin
 */
class ScalaResolverExtension {

  Closure _initAction

  ScalaResolverExtension(Closure initAction) {
    _initAction = initAction
  }

  void targetScalaVersion(String targetVersion) {
    _initAction.call(targetVersion)
  }

}
