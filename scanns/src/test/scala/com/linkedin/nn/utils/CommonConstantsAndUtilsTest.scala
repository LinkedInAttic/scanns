/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn.utils

import org.testng.Assert
import org.testng.annotations.Test

class CommonConstantsAndUtilsTest {
  @Test
  def testGetFeatureKey(): Unit = {
    Assert.assertEquals(CommonConstantsAndUtils.getFeatureKey("foo", "bar"), "foo\tbar")
    Assert.assertEquals(CommonConstantsAndUtils.getFeatureKey("foo", "bar", '%'), "foo%bar")
    Assert.assertEquals(CommonConstantsAndUtils.getFeatureKey("foo", ""), "foo\t")
    Assert.assertEquals(CommonConstantsAndUtils.getFeatureKey("", ""), "\t")
  }
}
