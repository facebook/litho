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
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefaultPropertyFoldingBuilderTest {

  private final LithoPluginTestHelper testHelper = new LithoPluginTestHelper("testdata/foldings");
  private JavaCodeInsightTestFixture fixture;

  @Before
  public void setUp() throws Exception {
    final TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder =
        IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder("test");
    fixture =
        JavaTestFixtureFactory.getFixtureFactory()
            .createCodeInsightFixture(projectBuilder.getFixture());
    fixture.setUp();
  }

  @After
  public void tearDown() throws Exception {
    fixture.tearDown();
  }

  @Test
  public void testFolding() {
    String clsName = "DefaultPropertyFoldingTest.java";

    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              try {
                fixture.testFolding(testHelper.getTestDataPath(clsName));
              } catch (FileComparisonFailure e) {
                Assert.fail("Actual: " + e.getActual());
              }
            });
  }
}
