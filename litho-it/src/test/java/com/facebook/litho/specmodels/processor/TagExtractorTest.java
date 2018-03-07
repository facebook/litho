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

/** Tests {@link TagExtractor} */
public class TagExtractorTest {

  @Rule public CompilationRule mCompilationRule = new CompilationRule();
  private ImmutableList<TagModel> mTagsFromSpecClass;

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

    mTagsFromSpecClass =
        TagExtractor.extractTagsFromSpecClass(
            types, elements.getTypeElement(TestClass.class.getCanonicalName()));
  }

  @Test
  public void testExtractsAllTags() {
    assertThat(mTagsFromSpecClass).hasSize(3);
  }

  @Test
  public void testValidExtraction() {
    TagModel emptyInterface = mTagsFromSpecClass.get(0);
    assertThat(emptyInterface.name)
        .isEqualTo(
            ClassName.bestGuess(
                "com.facebook.litho.specmodels.processor.TagExtractorTest.EmptyInterface"));
    assertThat(emptyInterface.hasMethods).isFalse();
    assertThat(emptyInterface.hasSupertype).isFalse();
  }

  @Test
  public void testNonEmptyTag() {
    TagModel nonEmptyInterface = mTagsFromSpecClass.get(1);
    assertThat(nonEmptyInterface.name)
        .isEqualTo(
            ClassName.bestGuess(
                "com.facebook.litho.specmodels.processor.TagExtractorTest.NonEmptyInterface"));
    assertThat(nonEmptyInterface.hasMethods).isTrue();
  }

  @Test
  public void testTagWithExtend() {
    TagModel extendedInterface = mTagsFromSpecClass.get(2);
    assertThat(extendedInterface.name)
        .isEqualTo(
            ClassName.bestGuess(
                "com.facebook.litho.specmodels.processor.TagExtractorTest.ExtendedInterface"));
    assertThat(extendedInterface.hasSupertype).isTrue();
  }
}
