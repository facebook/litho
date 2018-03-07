/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
import org.junit.Test;

public class ProcessorIntegrationTest {
  public static String RES_PREFIX = "/processor/";
  public static String RES_PACKAGE = "com.facebook.litho.processor.integration.resources";

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
        .onLine(21);
  }

  @Test
  public void compilesWithoutError() {
    final JavaFileObject javaFileObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "SimpleLayoutSpec.java"));

    final JavaFileObject expectedOutput =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "SimpleLayout.java"));

    Truth.assertAbout(JavaSourceSubjectFactory.javaSource())
        .that(javaFileObject)
        .processedWith(new ComponentsProcessor())
        .compilesWithoutError()
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "SimpleLayout.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "SimpleLayout$Builder.class")
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "SimpleLayoutSpec.class")
        .and()
        .generatesSources(expectedOutput);
  }

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

  @Test
  public void compilesMountSpec() {
    final JavaFileObject javaFileObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "SimpleMountSpec.java"));

    final JavaFileObject expectedOutput =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "SimpleMount.java"));

    Truth.assertAbout(JavaSourceSubjectFactory.javaSource())
        .that(javaFileObject)
        .processedWith(new ComponentsProcessor())
        .compilesWithoutError()
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "SimpleMount.class")
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "SimpleMount$Builder.class")
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "SimpleMountSpec.class")
        .and()
        .generatesSources(expectedOutput);
  }

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

  @Test
  public void compilesSimpleTestSampleSpec() {
    final JavaFileObject testSpecObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "SimpleTestSampleSpec.java"));
    final JavaFileObject layoutSpecObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "SimpleLayoutSpec.java"));
    final JavaFileObject expectedOutput =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "SimpleTestSample.java"));

    Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
        .that(ImmutableList.of(testSpecObject, layoutSpecObject))
        .processedWith(new ComponentsTestingProcessor(), new ComponentsProcessor())
        .compilesWithoutError()
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "SimpleTestSample.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "SimpleTestSample$Matcher.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "SimpleTestSample$Matcher$1.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "SimpleTestSampleSpec.class")
        .and()
        .generatesSources(expectedOutput);
  }

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
        .onLine(16);
  }

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
        .onLine(17);
  }
}
