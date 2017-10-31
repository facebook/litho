/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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

/** Tests {@link PropDefaultsExtractor}. */
public class PropDefaultsExtractorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  static class TestClass {
    private static final String title = "PROP_DEFAULT";

    @PropDefault
    public static void title$annotations() {}
  }

  @Test
  public void testKotlinPropDefaultsExtraction() {
    final Elements elements = mCompilationRule.getElements();
    final TypeElement element = elements.getTypeElement(TestClass.class.getCanonicalName());

    final ImmutableList<PropDefaultModel> propDefaults =
        PropDefaultsExtractor.getPropDefaults(element);

    assertThat(propDefaults).hasSize(1);

    assertThat(propDefaults.get(0).getName()).isEqualTo("title");
  }
}
