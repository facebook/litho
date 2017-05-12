/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.PropJavadocModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.SpecModel;

import com.squareup.javapoet.TypeName;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link JavadocGenerator}
 */
public class JavadocGeneratorTest {
  private final PropModel mRequiredPropModel = mock(PropModel.class);
  private final PropModel mOptionalPropModel = mock(PropModel.class);
  private final SpecModel mSpecModel = mock(SpecModel.class);

  @Test
  public void testGenerateJavadoc() {
    when(mSpecModel.getClassJavadoc()).thenReturn("Test Javadoc");
    when(mSpecModel.getPropJavadocs()).thenReturn(
        ImmutableList.of(
            new PropJavadocModel("propName1", "test prop1 javadoc"),
            new PropJavadocModel("propName2", "test prop2 javadoc")));
    when(mRequiredPropModel.getName()).thenReturn("propName1");
    when(mOptionalPropModel.getName()).thenReturn("propName2");
    when(mRequiredPropModel.getType()).thenReturn(TypeName.INT);
    when(mOptionalPropModel.getType()).thenReturn(TypeName.BOOLEAN);
    when(mOptionalPropModel.isOptional()).thenReturn(true);
    when(mSpecModel.getProps())
        .thenReturn(ImmutableList.of(mRequiredPropModel, mOptionalPropModel));

    TypeSpecDataHolder dataHolder = JavadocGenerator.generate(mSpecModel);
    assertThat(dataHolder.getJavadocSpecs()).hasSize(4);
    assertThat(dataHolder.getJavadocSpecs().get(0).toString()).isEqualTo("Test Javadoc");
    assertThat(dataHolder.getJavadocSpecs().get(1).toString()).isEqualTo("<p>\n");
    assertThat(dataHolder.getJavadocSpecs().get(2).toString())
        .isEqualTo("@prop-required propName1 int test prop1 javadoc\n");
    assertThat(dataHolder.getJavadocSpecs().get(3).toString())
        .isEqualTo("@prop-optional propName2 boolean test prop2 javadoc\n");
  }
}
