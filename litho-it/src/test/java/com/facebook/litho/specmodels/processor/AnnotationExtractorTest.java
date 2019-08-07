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

import com.facebook.litho.annotations.OnBind;
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
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link AnnotationExtractor}. */
@RunWith(JUnit4.class)
public class AnnotationExtractorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.TYPE)
  @interface SourceAnnotation {}

  @OnCreateInitialState
  @OnBind
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
