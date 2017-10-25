/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.annotations.ResType;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.PropJavadocModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.testing.specmodels.MockMethodParamModel;
import com.facebook.litho.testing.specmodels.MockSpecModel;
import com.squareup.javapoet.TypeName;
import org.junit.Test;


/**
 * Tests {@link JavadocGenerator}
 */
public class JavadocGeneratorTest {
  @Test
  public void testGenerateJavadocProps() {
    final MethodParamModel requiredMethodParam =
        MockMethodParamModel.newBuilder().name("propName1").type(TypeName.INT).build();
    final MethodParamModel optionalMethodParam =
        MockMethodParamModel.newBuilder().name("propName2").type(TypeName.BOOLEAN).build();
    final SpecModel specModel =
        MockSpecModel.newBuilder()
            .classJavadoc("Test Javadoc")
            .propJavadocs(
                ImmutableList.of(
                    new PropJavadocModel("propName1", "test prop1 javadoc"),
                    new PropJavadocModel("propName2", "test prop2 javadoc")))
            .props(
                ImmutableList.of(
                    new PropModel(requiredMethodParam, false, ResType.INT, ""),
                    new PropModel(optionalMethodParam, true, ResType.BOOL, "")))
            .build();

    final TypeSpecDataHolder dataHolder = JavadocGenerator.generate(specModel);
    assertThat(dataHolder.getJavadocSpecs()).hasSize(4);

    assertThat(getJavadocsString(dataHolder))
        .isEqualTo(
            "Test Javadoc<p>\n"
                + "@prop-required propName1 int test prop1 javadoc\n"
                + "@prop-optional propName2 boolean test prop2 javadoc\n");
  }

  @Test
  public void testGenerateJavadocSeeReference() {
    final SpecModel specModel =
        MockSpecModel.newBuilder()
            .classJavadoc("Test Javadoc")
            // Any class with a well-known path is fine here.
            .specTypeName(TypeName.get(TypeSpecDataHolder.class))
            .build();

    final TypeSpecDataHolder dataHolder = JavadocGenerator.generate(specModel);
    assertThat(dataHolder.getJavadocSpecs()).hasSize(3);
    assertThat(getJavadocsString(dataHolder))
        .isEqualTo(
            "Test Javadoc<p>\n"
                + "\n"
                + "@see com.facebook.litho.specmodels.generator.TypeSpecDataHolder\n");
  }

  private final String getJavadocsString(TypeSpecDataHolder dataHolder) {
    return dataHolder
        .getJavadocSpecs()
        .stream()
        .reduce("", (s, javadocSpec) -> s + javadocSpec, (s, s2) -> s + s2);
  }
}
