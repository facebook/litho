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

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.TestSpec;
import com.google.testing.compile.CompilationRule;
import javax.lang.model.element.TypeElement;
import org.junit.Rule;
import org.junit.Test;

public class TestTargetExtractorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  @LayoutSpec
  static class MyLayoutSpec {
    @OnCreateLayout
    public Component onCreateLayout(ComponentContext c, @Prop String s) {
      return null;
    }
  }

  @TestSpec(MyLayoutSpec.class)
  interface TestMyLayoutSpec {}

  @Test
  public void testExtraction() {
    final TypeElement typeElement =
        mCompilationRule.getElements().getTypeElement(TestMyLayoutSpec.class.getCanonicalName());

    final TypeElement testSpecValue = TestTargetExtractor.getTestSpecValue(typeElement);
    assertThat(testSpecValue)
        .isNotNull()
        .hasToString(
            "com.facebook.litho.specmodels.processor.TestTargetExtractorTest.MyLayoutSpec");
  }
}
