/*
 * Copyright 2018-present Facebook, Inc.
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

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.FieldModel;
import com.google.testing.compile.CompilationRule;
import com.squareup.javapoet.FieldSpec;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import org.junit.Rule;
import org.junit.Test;

/** Tests {@link FieldsExtractor}. */
public class FieldsExtractorTest {
  @Rule public CompilationRule compilationRule = new CompilationRule();

  private static class TwoFieldsClass {
    private static final int intField = 0;
    static float floatField = 0f;

    private boolean isTest() {
      return true;
    }
  }

  private static class NoFieldsClass {

    private int getOne() {
      return 1;
    }
  }

  @Test
  public void testFieldExtraction() {
    final TypeElement element =
        compilationRule.getElements().getTypeElement(TwoFieldsClass.class.getCanonicalName());
    ImmutableList<FieldModel> fieldModels = FieldsExtractor.extractFields(element);
    assertThat(fieldModels).hasSize(2);

    FieldSpec extractedIntField = fieldModels.get(0).field;
    assertThat(extractedIntField.name).isEqualTo("intField");
    assertThat(extractedIntField.modifiers).hasSize(3);
    assertThat(extractedIntField.hasModifier(Modifier.PRIVATE));
    assertThat(extractedIntField.hasModifier(Modifier.STATIC));
    assertThat(extractedIntField.hasModifier(Modifier.FINAL));

    FieldSpec extractedFloatField = fieldModels.get(1).field;
    assertThat(extractedFloatField.name).isEqualTo("floatField");
    assertThat(extractedFloatField.modifiers).hasSize(1);
    assertThat(extractedFloatField.hasModifier(Modifier.STATIC));
  }

  @Test
  public void testNoFieldExtraction() {
    final TypeElement element =
        compilationRule.getElements().getTypeElement(NoFieldsClass.class.getCanonicalName());
    ImmutableList<FieldModel> fieldModels = FieldsExtractor.extractFields(element);
    assertThat(fieldModels).hasSize(0);
  }
}
