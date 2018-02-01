/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.nn.test

import org.testng.{ITestResult, TestListenerAdapter}

/**
 * Implements a TestNG listener that converts "skip" status to "fail". This is a global approach to a problem where
 * exceptions thrown in the code for a DataProvider cause dependent tests to be silently skipped. The listener surfaces
 * any skip with an associated Throwable as a failure.
 */
class FailOnSkipListener extends TestListenerAdapter {

  /**
   * Invoked each time a test is skipped.
   *
   * @param tr ITestResult containing information about the run test
   */
  override def onTestSkipped(tr: ITestResult) {
    // If the skip was a result of an exception, change the skip to a failure
    if (Option(tr.getThrowable).isDefined) {
      tr.setStatus(ITestResult.FAILURE)
    }
  }
}
