/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.intellij.foldings;

import com.facebook.litho.intellij.LithoPluginTestHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefaultPropertyFoldingBuilderTest {

  private final LithoPluginTestHelper testHelper = new LithoPluginTestHelper("testdata/foldings");

  @Before
  public void setUp() throws Exception {
    testHelper.setUp();
  }

  @After
  public void tearDown() throws Exception {
    testHelper.tearDown();
  }

  @Test
  public void testFolding() {
    String clsName = "DefaultPropertyFoldingTest.java";

    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              try {
                testHelper.getFixture().testFolding(testHelper.getTestDataPath(clsName));
              } catch (FileComparisonFailure e) {
                Assert.fail("Actual: " + e.getActual());
              }
            });
  }
}
