/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.intellij.foldings;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import org.junit.Assert;
import org.junit.Test;

public class DefaultPropertyFoldingBuilderTest extends LithoPluginIntellijTest {

  public DefaultPropertyFoldingBuilderTest() {
    super("testdata/foldings");
  }

  @Test
  public void folding_forLayoutSpec_matchingTemplate() {
    testFoldingForClassname("FoldingTestLayoutSpec.java");
  }

  @Test
  public void folding_forGroupSectionSpec_matchingTemplate() {
    testFoldingForClassname("FoldingTestGroupSectionSpec.java");
  }

  private void testFoldingForClassname(String clsName) {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              try {
                testHelper.getFixture().testFolding(testHelper.getTestDataPath(clsName));
              } catch (FileComparisonFailure e) {
                Assert.fail("Actual: " + e.getActual());
              }
            });
  }
}
