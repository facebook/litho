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

import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.google.testing.compile.CompilationRule;
import com.squareup.javapoet.AnnotationSpec;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import org.junit.Rule;
import org.junit.Test;

/** Tests {@link AnnotationExtractor}. */
public class AnnotationExtractorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.TYPE)
  @interface SourceAnnotation {}

  @OnCreateInitialState
  @Deprecated
  @SourceAnnotation
  static class TestClass {}

  @Test
  public void testAnnotationExtraction() {
    final Elements elements = mCompilationRule.getElements();
    final TypeElement element = elements.getTypeElement(TestClass.class.getCanonicalName());

    final ImmutableList<AnnotationSpec> annotationSpecs =
        AnnotationExtractor.extractValidAnnotations(element);

    assertThat(annotationSpecs)
        .hasSize(1)
        .withFailMessage("Only the @Deprecated annotation should be extracted.");

    assertThat(annotationSpecs)
        .contains(AnnotationSpec.builder(Deprecated.class).build())
        .doesNotContain(AnnotationSpec.builder(OnCreateInitialState.class).build())
        .doesNotContain(AnnotationSpec.builder(SourceAnnotation.class).build());
  }
}
