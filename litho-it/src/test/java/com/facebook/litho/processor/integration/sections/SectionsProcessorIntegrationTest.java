/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor.integration.sections;

import com.facebook.litho.sections.specmodels.processor.SectionsComponentProcessor;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourceSubjectFactory;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.junit.Test;

public class SectionsProcessorIntegrationTest {
  private static final String RES_PREFIX = "/processor/sections/";
  private static final String RES_PACKAGE =
      "com.facebook.litho.sections.processor.integration.resources";

  @Test
  public void compilesGroupSpecWithoutError() {
    final JavaFileObject javaFileObject = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "SimpleGroupSectionSpec.java"));

    final JavaFileObject expectedOutput = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "SimpleGroupSection.java"));

    Truth.assertAbout(JavaSourceSubjectFactory.javaSource())
        .that(javaFileObject)
        .processedWith(new SectionsComponentProcessor())
        .compilesWithoutError()
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "SimpleGroupSection.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "SimpleGroupSection$1.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "SimpleGroupSection$Builder.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "SimpleGroupSectionSpec.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "SimpleGroupSection$SimpleGroupSectionImpl.class")
        .and()
        .generatesSources(expectedOutput);
  }

  @Test
  public void compilesDiffSpecWithoutError() throws Exception {
    final JavaFileObject javaFileObject = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "SimpleDiffSectionSpec.java"));

    final JavaFileObject expectedOutput = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "SimpleDiffSection.java"));

    Truth.assertAbout(JavaSourceSubjectFactory.javaSource())
        .that(javaFileObject)
        .processedWith(new SectionsComponentProcessor())
        .compilesWithoutError()
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "SimpleDiffSection.class")
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "SimpleDiffSection$1.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "SimpleDiffSection$Builder.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "SimpleDiffSectionSpec.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "SimpleDiffSection$SimpleDiffSectionImpl.class")
        .and()
        .generatesSources(expectedOutput);
  }

  @Test
  public void compilesFullGroupSectionSpecWithoutError() {
    final JavaFileObject javaFileObject = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "FullGroupSectionSpec.java"));

    final JavaFileObject testEventFileObject = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "TestEvent.java"));

    final JavaFileObject expectedOutput = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "FullGroupSection.java"));

    Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
        .that(ImmutableList.of(javaFileObject, testEventFileObject))
        .processedWith(new SectionsComponentProcessor())
        .compilesWithoutError()
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "FullGroupSection.class")
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "FullGroupSection$1.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "FullGroupSection$FullGroupSectionStateContainerImpl.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "FullGroupSection$Builder.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "FullGroupSectionSpec.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "FullGroupSection$FullGroupSectionImpl.class")
        .and()
        .generatesSources(expectedOutput);
  }

  @Test
  public void compilesFullDiffSectionSpecWithoutError() {
    final JavaFileObject javaFileObject = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "FullDiffSectionSpec.java"));

    final JavaFileObject testEventFileObject = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "TestEvent.java"));

    final JavaFileObject expectedOutput = JavaFileObjects.forResource(Resources.getResource(
        getClass(),
        RES_PREFIX + "FullDiffSection.java"));

    Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
        .that(ImmutableList.of(javaFileObject, testEventFileObject))
        .processedWith(new SectionsComponentProcessor())
        .compilesWithoutError()
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "FullDiffSection.class")
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "FullDiffSection$1.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "FullDiffSection$FullDiffSectionStateContainerImpl.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "FullDiffSection$Builder.class")
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "FullDiffSectionSpec.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "FullDiffSection$FullDiffSectionImpl.class")
        .and()
        .generatesSources(expectedOutput);
  }
}
