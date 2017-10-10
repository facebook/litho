/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.specmodels.model.SpecElementType;
import com.google.testing.compile.CompilationRule;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import org.junit.Rule;
import org.junit.Test;

/** Tests {@link SpecElementTypeDeterminator} */
public class SpecElementTypeDeterminatorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  /**
   * We can't test with a real kotlin object here, so we'll replicate the structure that we test
   * for.
   */
  public static class FakeKotlinSingleton {
    public static final FakeKotlinSingleton INSTANCE = null;
  }

  @Test
  public void testNormalClass() {
    final Elements elements = mCompilationRule.getElements();
    final TypeElement typeElement = elements.getTypeElement(String.class.getName());

    assertThat(SpecElementTypeDeterminator.determine(typeElement))
        .isEqualTo(SpecElementType.JAVA_CLASS);
  }

  @Test
  public void testKotlinSingleton() {
    final Elements elements = mCompilationRule.getElements();
    final TypeElement typeElement =
        elements.getTypeElement(
            SpecElementTypeDeterminatorTest.FakeKotlinSingleton.class.getCanonicalName());

    assertThat(SpecElementTypeDeterminator.determine(typeElement))
        .isEqualTo(SpecElementType.KOTLIN_SINGLETON);
  }
}
