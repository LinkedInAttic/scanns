/*
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn.build.plugins

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

import static ScalaUtils.getScalaVersionSuffix

/**
 * This plugin provides Scala cross-build capability, it creates multiple projects with different scala version suffixes
 * that share the same module directory.
 */
class ScalaCrossBuildPlugin implements Plugin<Settings> {

  private static def includeSuffixedProject(settings, module, scalaVersion) {
    def path = module + getScalaVersionSuffix(scalaVersion)
    settings.include(path)
    def project = settings.findProject(path)
    project.projectDir = new File(project.projectDir.parent, module.split(':').last())
  }

  void apply(Settings settings) {

    def scalaCrossBuild = settings.extensions.create('scalaCrossBuild', ScalaCrossBuildExtension, settings.startParameter.projectProperties)

    scalaCrossBuild.projectsToNotCrossBuild.all { module ->
      settings.include(module)
    }

    scalaCrossBuild.projectsToCrossBuild.all { module ->
      if (scalaCrossBuild.buildDefaultOnly) {
        includeSuffixedProject(settings, module, scalaCrossBuild.defaultScalaVersion)
      }
      else {
        scalaCrossBuild.targetScalaVersions.each { v -> includeSuffixedProject(settings, module, v) }
      }
    }

    settings.gradle.projectsLoaded { g ->
      g.rootProject.subprojects {
        def projectScalaVersion = scalaCrossBuild.targetScalaVersions.find { name.contains(getScalaVersionSuffix(it)) }
        def scalaVersion = projectScalaVersion ? projectScalaVersion : scalaCrossBuild.defaultScalaVersion
        def scalaSuffix = getScalaVersionSuffix(scalaVersion)
        ext.scalaVersion = scalaVersion
        ext.scalaSuffix = scalaSuffix
        ext.defaultScalaVersion = scalaCrossBuild.defaultScalaVersion
        ext.defaultScalaSuffix = getScalaVersionSuffix(scalaCrossBuild.defaultScalaVersion)

        // map the output directories in a way such that outputs won't overlap
        buildDir = "${g.rootProject.buildDir}/$name"
      }
    }

  }
}
