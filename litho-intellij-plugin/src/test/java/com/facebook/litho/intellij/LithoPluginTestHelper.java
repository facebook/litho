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

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

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
    fixture.setTestDataPath(testPath);
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

  // Package-private to be used in lambda
  String getContentOrNull(String clsName) {
    try {
      return getContent(clsName);
    } catch (IOException e) {
      return null;
    }
  }

  public void getPsiClass(String clsName, Function<PsiClass, Boolean> handler) {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              Optional<Boolean> sucess =
                  Optional.of(clsName)
                      .map(LithoPluginTestHelper.this::getContentOrNull)
                      .map(
                          content ->
                              PsiFileFactory.getInstance(fixture.getProject())
                                  .createFileFromText(clsName, JavaFileType.INSTANCE, content))
                      .filter(PsiJavaFile.class::isInstance)
                      .map(PsiJavaFile.class::cast)
                      .map(PsiClassOwner::getClasses)
                      .filter(psiClasses -> psiClasses.length > 0)
                      .map(psiClasses -> psiClasses[0])
                      .map(handler);
              if (!sucess.isPresent()) {
                handler.apply(null);
              }
            });
  }
}

