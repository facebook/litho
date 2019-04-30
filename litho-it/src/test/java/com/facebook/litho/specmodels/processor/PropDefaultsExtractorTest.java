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

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.PropDefaultModel;
import com.google.testing.compile.CompilationRule;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link PropDefaultsExtractor}. */
@RunWith(JUnit4.class)
public class PropDefaultsExtractorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  static class TestClassWithoutGetAnnotation {
    private static final String title = "PROP_DEFAULT";

    @PropDefault
    public static void title$annotations() {}
  }

  static class TestClassWithGet {
    private static final String title = "PROP_DEFAULT";

    @PropDefault
    public final String getTitle() {
      return title;
    }
  }

  @Test
  public void testKotlinPropDefaultsExtractionWithoutGetAnnotation() {
    final Elements elements = mCompilationRule.getElements();
    final TypeElement element =
        elements.getTypeElement(TestClassWithoutGetAnnotation.class.getCanonicalName());

    final ImmutableList<PropDefaultModel> propDefaults =
        PropDefaultsExtractor.getPropDefaults(element);

    assertThat(propDefaults).hasSize(1);

    assertThat(propDefaults.get(0).getName()).isEqualTo("title");
  }

  @Test
  public void testKotlinPropDefaultsExtractionWithGetAnnotation() {
    final Elements elements = mCompilationRule.getElements();
    final TypeElement element = elements.getTypeElement(TestClassWithGet.class.getCanonicalName());

    final ImmutableList<PropDefaultModel> propDefaults =
        PropDefaultsExtractor.getPropDefaults(element);

    assertThat(propDefaults).hasSize(1);

    assertThat(propDefaults.get(0).getName()).isEqualTo("title");
  }
}
