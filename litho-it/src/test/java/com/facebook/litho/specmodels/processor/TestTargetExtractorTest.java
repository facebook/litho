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
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
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
