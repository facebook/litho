/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor.integration;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

import com.facebook.litho.specmodels.processor.ComponentsProcessor;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourceSubjectFactory;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import org.junit.Test;

public class ProcessorIntegrationTest {
  public static String RES_PREFIX = "/processor/";
  public static String RES_PACKAGE = "com.facebook.litho.processor.integration.resources";

  @Test
  public void failsToCompileWithWrongContext() throws IOException {
    final Enumeration<URL> resources = getClass().getClassLoader().getResources("");
    final ArrayList list = new ArrayList();
    while (resources.hasMoreElements()) {
      list.add(resources.nextElement());
    }
    final JavaFileObject javaFileObject = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "IncorrectOnCreateLayoutArgsComponentSpec.java"));
    Truth.assertAbout(JavaSourceSubjectFactory.javaSource())
        .that(javaFileObject)
        .processedWith(new ComponentsProcessor())
        .failsToCompile()
        .withErrorCount(1)
        .withErrorContaining(
            "Parameter in position 0 of a method annotated with interface " +
                "com.facebook.litho.annotations.OnCreateLayout should be of type " +
                "com.facebook.litho.ComponentContext")
        .in(javaFileObject)
        .onLine(22);
  }

  @Test
  public void compilesWithoutError() {
    final JavaFileObject javaFileObject = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "SimpleLayoutSpec.java"));

    final JavaFileObject expectedOutput = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "SimpleLayout.java"));

    Truth.assertAbout(JavaSourceSubjectFactory.javaSource())
        .that(javaFileObject)
        .processedWith(new ComponentsProcessor())
        .compilesWithoutError()
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "SimpleLayout.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "SimpleLayout$1.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "SimpleLayout$Builder.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "SimpleLayoutSpec.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "SimpleLayout$SimpleLayoutImpl.class")
        .and()
        .generatesSources(expectedOutput);
  }

  @Test
  public void compilesTestLayoutSpecWithoutError() {
    final JavaFileObject javaFileObject = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "TestLayoutSpec.java"));

    final JavaFileObject testTreePropFileObject = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "TestTreeProp.java"));

    final JavaFileObject testEventFileObject = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "TestEvent.java"));

    final JavaFileObject expectedOutput = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "TestLayout.java"));

    Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
        .that(ImmutableList.of(javaFileObject, testTreePropFileObject, testEventFileObject))
        .processedWith(new ComponentsProcessor())
        .compilesWithoutError()
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "TestLayout.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "TestLayout$1.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "TestLayout$TestLayoutStateContainerImpl.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "TestLayout$Builder.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "TestLayoutSpec.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "TestLayout$TestLayoutImpl.class")
        .and()
        .generatesSources(expectedOutput);
  }

  @Test
  public void compilesMountSpec() {
    final JavaFileObject javaFileObject = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "SimpleMountSpec.java"));

    final JavaFileObject expectedOutput = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "SimpleMount.java"));

    Truth.assertAbout(JavaSourceSubjectFactory.javaSource())
        .that(javaFileObject)
        .processedWith(new ComponentsProcessor())
        .compilesWithoutError()
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "SimpleMount.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "SimpleMount$1.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "SimpleMount$SimpleMountImpl.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "SimpleMount$Builder.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "SimpleMountSpec.class")
        .and()
        .generatesSources(expectedOutput);
  }

  @Test
  public void compilesTestMountSpec() {
    final JavaFileObject javaFileObject = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "TestMountSpec.java"));

    final JavaFileObject testTreePropFileObject = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "TestTreeProp.java"));

    final JavaFileObject testEventFileObject = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "TestEvent.java"));

    final JavaFileObject expectedOutput = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "TestMount.java"));

    Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
        .that(ImmutableList.of(javaFileObject, testTreePropFileObject, testEventFileObject))
        .processedWith(new ComponentsProcessor())
        .compilesWithoutError()
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "TestMount.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "TestMount$TestMountStateContainerImpl.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "TestMount$1.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "TestMount$TestMountImpl.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "TestMount$Builder.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "TestMountSpec.class")
        .and()
        .generatesSources(expectedOutput);
  }
}
