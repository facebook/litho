/*
 * Copyright 2014-present Facebook, Inc.
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

package com.facebook.litho.processor.integration.sections;

import com.facebook.litho.sections.specmodels.processor.SectionsComponentProcessor;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.junit.Test;

public class SectionsProcessorIntegrationTest {
  private static final String RES_PREFIX = "/processor/sections/";
  private static final String RES_PACKAGE =
      "com.facebook.litho.sections.processor.integration.resources";

  @Test
  public void compilesFullGroupSectionSpecWithoutError() {
    final JavaFileObject javaFileObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "FullGroupSectionSpec.java"));

    final JavaFileObject testEventFileObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "TestEvent.java"));

    final JavaFileObject testTagFileObject =
        JavaFileObjects.forResource(Resources.getResource(getClass(), RES_PREFIX + "TestTag.java"));

    final JavaFileObject expectedOutput =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "FullGroupSection.java"));

    Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
        .that(ImmutableList.of(javaFileObject, testEventFileObject, testTagFileObject))
        .processedWith(new SectionsComponentProcessor())
        .compilesWithoutError()
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "FullGroupSection.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "FullGroupSection$FullGroupSectionStateContainer.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "FullGroupSection$Builder.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "FullGroupSectionSpec.class")
        .and()
        .generatesSources(expectedOutput);
  }

  @Test
  public void compilesFullDiffSectionSpecWithoutError() {
    final JavaFileObject javaFileObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "FullDiffSectionSpec.java"));

    final JavaFileObject testEventFileObject =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "TestEvent.java"));

    final JavaFileObject testTagFileObject =
        JavaFileObjects.forResource(Resources.getResource(getClass(), RES_PREFIX + "TestTag.java"));

    final JavaFileObject expectedOutput =
        JavaFileObjects.forResource(
            Resources.getResource(getClass(), RES_PREFIX + "FullDiffSection.java"));

    Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
        .that(ImmutableList.of(javaFileObject, testEventFileObject, testTagFileObject))
        .processedWith(new SectionsComponentProcessor())
        .compilesWithoutError()
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "FullDiffSection.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT,
            RES_PACKAGE,
            "FullDiffSection$FullDiffSectionStateContainer.class")
        .and()
        .generatesFileNamed(
            StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "FullDiffSection$Builder.class")
        .and()
        .generatesFileNamed(StandardLocation.CLASS_OUTPUT, RES_PACKAGE, "FullDiffSectionSpec.class")
        .and()
        .generatesSources(expectedOutput);
  }
}
