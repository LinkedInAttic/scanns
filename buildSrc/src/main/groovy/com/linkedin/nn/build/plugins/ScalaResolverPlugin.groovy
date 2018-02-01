/*
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn.build.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

import static ScalaUtils.getScalaVersionSuffix

/**
 * This plugin helps resolve Scala dependencies with the correct Scala version.
 */
class ScalaResolverPlugin implements Plugin<Project> {

  Project project

  def initResolver(String scalaVersion) {
    def scalaSuffix = getScalaVersionSuffix(scalaVersion)
    project.configurations.all {
      resolutionStrategy.eachDependency { dep ->
        if (dep.target.group == 'org.scala-lang' && dep.target.version != scalaVersion) {
          dep.useVersion scalaVersion
        }
        def scalaPattern = dep.target.name =~ /(.+)(_2(\.[0-9]{1,2}){1,2})/
        if (scalaPattern.matches()) {
          def moduleName = scalaPattern.group(1)
          def scalaVariant = scalaPattern.group(2)
          if (scalaVariant != scalaSuffix) {
            println("replacing binary incompatible dependency ${dep.target.name} with ${moduleName + scalaSuffix}")
            dep.useTarget group: dep.target.group, name: moduleName + scalaSuffix, version: dep.target.version
          }
        }
      }
    }
  }

  void apply(Project project) {
    this.project = project
    project.extensions.create('scalaResolver', ScalaResolverExtension, this.&initResolver)
  }
}
