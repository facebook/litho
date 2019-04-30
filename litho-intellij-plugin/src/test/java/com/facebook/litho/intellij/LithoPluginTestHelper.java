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
package com.facebook.litho.intellij;

import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class LithoPluginTestHelper {
  private final String testPath;
  private CodeInsightTestFixture fixture;

  /** @param testPath in the form "testdata/dir" */
  public LithoPluginTestHelper(String testPath) {
    this.testPath = new File(testPath).getAbsolutePath();
  }

  public void setUp() throws Exception {
    // Disable assistive technologies to prevent test failure on CI:
    // java.awt.AWTError: Assistive Technology not found: org.GNOME.Accessibility.AtkWrapper
    final Properties props = System.getProperties();
    props.setProperty("javax.accessibility.assistive_technologies", "");

    final TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder =
        IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder("test");
    fixture =
        JavaTestFixtureFactory.getFixtureFactory()
            .createCodeInsightFixture(projectBuilder.getFixture());
    fixture.setUp();
  }

  public void tearDown() throws Exception {
    fixture.tearDown();
  }

  public void configure(String clsName) throws IOException {
    fixture.configureByText(clsName, getContent(clsName));
  }

  public String getTestDataPath(String clsName) {
    return testPath + "/" + clsName;
  }

  public CodeInsightTestFixture getFixture() {
    return fixture;
  }

  private String getContent(String clsName) throws IOException {
    return String.join(" ", Files.readAllLines(Paths.get(getTestDataPath(clsName))));
  }
}
