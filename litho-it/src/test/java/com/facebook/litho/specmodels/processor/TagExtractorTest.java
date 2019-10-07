/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.specmodels.processor;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.TagModel;
import com.google.testing.compile.CompilationRule;
import com.squareup.javapoet.ClassName;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link TagExtractor} */
@RunWith(JUnit4.class)
public class TagExtractorTest {

  @Rule public CompilationRule mCompilationRule = new CompilationRule();
  private ImmutableList<TagModel> mTagModels;

  interface EmptyInterface {}

  interface NonEmptyInterface {
    void method();
  }

  interface ExtendedInterface extends NonEmptyInterface {}

  @LayoutSpec
  static class TestClass implements EmptyInterface, NonEmptyInterface, ExtendedInterface {

    @Override
    public void method() {}
  }

  @Before
  public void setUp() throws Exception {
    Elements elements = mCompilationRule.getElements();
    Types types = mCompilationRule.getTypes();

    mTagModels =
        TagExtractor.extractTagsFromSpecClass(
            types, elements.getTypeElement(TestClass.class.getCanonicalName()));
  }

  @Test
  public void testExtractsAllTags() {
    assertThat(mTagModels).hasSize(3);
  }

  @Test
  public void testValidExtraction() {
    TagModel emptyInterface = mTagModels.get(0);
    assertThat(emptyInterface.name)
        .isEqualTo(
            ClassName.bestGuess(
                "com.facebook.litho.specmodels.processor.TagExtractorTest.EmptyInterface"));
    assertThat(emptyInterface.hasMethods).isFalse();
    assertThat(emptyInterface.hasSupertype).isFalse();
  }

  @Test
  public void testNonEmptyTag() {
    TagModel nonEmptyInterface = mTagModels.get(1);
    assertThat(nonEmptyInterface.name)
        .isEqualTo(
            ClassName.bestGuess(
                "com.facebook.litho.specmodels.processor.TagExtractorTest.NonEmptyInterface"));
    assertThat(nonEmptyInterface.hasMethods).isTrue();
  }

  @Test
  public void testTagWithExtend() {
    TagModel extendedInterface = mTagModels.get(2);
    assertThat(extendedInterface.name)
        .isEqualTo(
            ClassName.bestGuess(
                "com.facebook.litho.specmodels.processor.TagExtractorTest.ExtendedInterface"));
    assertThat(extendedInterface.hasSupertype).isTrue();
  }
}
