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

package com.facebook.litho.processor.integration;

import com.facebook.litho.specmodels.processor.ComponentsProcessor;
import com.facebook.litho.specmodels.processor.testing.ComponentsTestingProcessor;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourceSubjectFactory;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import java.io.IOException;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ProcessorIntegrationTest {
  public static String RES_PREFIX = "/processor/";
  public static String RES_PACKAGE = "com.facebook.litho.processor.integration.resources";

  @Ignore("T41117446") // Starts failing after updating gradle plugin to 3.3.1
  @Test
  public void failsToCompileWithWrongContext() throws IOException {
    final JavaFileObject javaFileObject =
        JavaFileObjects.forResource(
            Resources.getResource(
                getClass(), RES_PREFIX + "IncorrectOnCreateLayoutArgsComponentSpec.java"));
    Truth.assertAbout(JavaSourceSubjectFactory.javaSource())
        .that(javaFileObject)
        .processedWith(new ComponentsProcessor())
        .failsToCompile()
        .withErrorCount(1)
        .withErrorContaining(
            "Parameter in position 0 of a method annotated with interface "
                + "com.facebook.litho.annotations.OnCreateLayout should be of type "
                + "com.facebook.litho.ComponentContext")
        .in(javaFileObject)
        .onLine(28);
  }

  @Ignore("T41117446") //  Enable them after switching target to AndroidX
  @Test
  public void compilesTestLayoutSpecWithoutError() {
    final JavaFileObject javaFileObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "TestLayoutSpec.java"));

    final JavaFileObject testTreePropFileObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "TestTreeProp.java"));

    final JavaFileObject testEventFileObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "TestEvent.java"));

    final JavaFileObject testTagFileObject =
        JavaFileObjects.forResource(Resources.getResource(getClass(), RES_PREFIX + "TestTag.java"));

    final JavaFileObject expectedOutput =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "TestLayout.java"));

    Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
        .that(
            ImmutableList.of(
                javaFileObject, testTreePropFileObject, testEventFileObject, testTagFileObject))
        .processedWith(new ComponentsProcessor())
        .compilesWithoutError()
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "TestLayout.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "TestLayout$TestLayoutStateContainer.class")
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "TestLayout$Builder.class")
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "TestLayoutSpec.class")
        .and()
        .generatesSources(expectedOutput);
  }

  @Ignore("T41117446") //  Enable them after switching target to AndroidX
  @Test
  public void compilesTestMountSpec() {
    final JavaFileObject javaFileObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "TestMountSpec.java"));

    final JavaFileObject testTreePropFileObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "TestTreeProp.java"));

    final JavaFileObject testEventFileObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "TestEvent.java"));

    final JavaFileObject testTagFileObject =
        JavaFileObjects.forResource(Resources.getResource(getClass(), RES_PREFIX + "TestTag.java"));

    final JavaFileObject expectedOutput =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "TestMount.java"));

    Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
        .that(
            ImmutableList.of(
                javaFileObject, testTreePropFileObject, testEventFileObject, testTagFileObject))
        .processedWith(new ComponentsProcessor())
        .compilesWithoutError()
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "TestMount.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "TestMount$TestMountStateContainer.class")
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "TestMount$1.class")
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "TestMount$Builder.class")
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "TestMountSpec.class")
        .and()
        .generatesSources(expectedOutput);
  }

  @Ignore("T41117446") //  Enable them after switching target to AndroidX
  @Test
  public void compilesBasicTestSampleSpec() {
    final JavaFileObject testSpecObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "BasicTestSampleSpec.java"));
    final JavaFileObject layoutSpecObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "BasicLayoutSpec.java"));

    final JavaFileObject expectedOutput =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "BasicTestSample.java"));

    Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
        .that(ImmutableList.of(testSpecObject, layoutSpecObject))
        .processedWith(new ComponentsTestingProcessor(), new ComponentsProcessor())
        .compilesWithoutError()
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "BasicTestSample.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "BasicTestSample$Matcher.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "BasicTestSample$Matcher$1.class")
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "BasicTestSampleSpec.class")
        .and()
        .generatesSources(expectedOutput);
  }

  @Ignore("T41117446") // Starts failing after updating gradle plugin to 3.3.1
  @Test
  public void failsToCompileClassBasedTestSpec() throws IOException {
    final JavaFileObject javaFileObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "IncorrectClassBasedTestSpec.java"));
    final JavaFileObject layoutSpecObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "BasicLayoutSpec.java"));

    Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
        .that(ImmutableList.of(javaFileObject, layoutSpecObject))
        .processedWith(new ComponentsTestingProcessor(), new ComponentsProcessor())
        .failsToCompile()
        .withErrorCount(1)
        .withErrorContaining(
            "Specs annotated with @TestSpecs must be interfaces and cannot be of kind CLASS.")
        .in(javaFileObject)
        .onLine(23);
  }

  @Ignore("T41117446") // Starts failing after updating gradle plugin to 3.3.1
  @Test
  public void failsToCompileNonEmptyTestSpecInterface() throws IOException {
    final JavaFileObject javaFileObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "IncorrectNonEmptyTestSpec.java"));
    final JavaFileObject layoutSpecObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "BasicLayoutSpec.java"));

    Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
        .that(ImmutableList.of(javaFileObject, layoutSpecObject))
        .processedWith(new ComponentsTestingProcessor(), new ComponentsProcessor())
        .failsToCompile()
        .withErrorCount(1)
        .withErrorContaining(
            "TestSpec interfaces must not contain any members. Please remove these function declarations: ()void test, ()java.util.List<java.lang.Integer> list")
        .in(javaFileObject)
        .onLine(24);
  }
}
