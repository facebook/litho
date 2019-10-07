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
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;

public class LithoPluginIntellijTest {

  protected final TestHelper testHelper;

  public LithoPluginIntellijTest(String testPath) {
    testHelper = new TestHelper(testPath);
  }

  @Before
  public void setUp() throws Exception {
    testHelper.setUp();
  }

  @After
  public void tearDown() throws Exception {
    testHelper.tearDown();
  }

  public static class TestHelper {
    private final String testPath;
    private CodeInsightTestFixture fixture;

    /** @param testPath in the form "testdata/dir" */
    public TestHelper(String testPath) {
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

    /**
     * Converts list of class names to the list of psi classes.
     *
     * @param handler calling class should pass handler for the psi classes. Passes null if
     *     conversion was unsuccessful at any step.
     * @param clsNames names of the classes to found in the test root directory (MyClass.java)
     */
    public void getPsiClass(Function<List<PsiClass>, Boolean> handler, String... clsNames) {
      ApplicationManager.getApplication()
          .invokeAndWait(
              () -> {
                List<PsiClass> psiClasses =
                    Stream.of(clsNames)
                        .filter(Objects::nonNull)
                        .map(
                            clsName -> {
                              String content = getContentOrNull(clsName);
                              if (content != null) {
                                return PsiFileFactory.getInstance(fixture.getProject())
                                    .createFileFromText(clsName, JavaFileType.INSTANCE, content);
                              }
                              return null;
                            })
                        .filter(PsiJavaFile.class::isInstance)
                        .map(PsiJavaFile.class::cast)
                        .map(PsiClassOwner::getClasses)
                        .filter(fileClasses -> fileClasses.length > 0)
                        .map(fileClasses -> fileClasses[0])
                        .collect(Collectors.toList());
                if (psiClasses.isEmpty()) {
                  handler.apply(null);
                } else {
                  handler.apply(psiClasses);
                }
              });
    }
  }
}
