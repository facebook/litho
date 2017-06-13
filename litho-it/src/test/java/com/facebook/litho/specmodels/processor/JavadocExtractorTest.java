/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.PropJavadocModel;

import org.junit.Before;
import org.junit.Test;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link JavadocExtractor}
 */
public class JavadocExtractorTest {
  private final Elements mElements = mock(Elements.class);
  private final TypeElement mTypeElement = mock(TypeElement.class);

  @Before
  public void setup() {
    when(mElements.getDocComment(mTypeElement)).thenReturn(
        "Test javadoc\n" +
        "@prop testProp prop for testing");
  }

  @Test
  public void testClassJavadoc() {
    String classJavadoc = JavadocExtractor.getClassJavadoc(mElements, mTypeElement);
    assertThat(classJavadoc).isEqualTo("Test javadoc\n");
  }

  @Test
  public void testPropsJavadoc() {
    ImmutableList<PropJavadocModel> propJavadocs =
        JavadocExtractor.getPropJavadocs(mElements, mTypeElement);
    assertThat(propJavadocs).hasSize(1);
    assertThat(propJavadocs.get(0).propName).isEqualTo("testProp");
    assertThat(propJavadocs.get(0).javadoc).isEqualTo("prop for testing");
  }
}
