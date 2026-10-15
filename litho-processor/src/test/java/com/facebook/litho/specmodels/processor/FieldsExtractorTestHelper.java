/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.FieldModel;
import com.squareup.javapoet.FieldSpec;
import javax.lang.model.element.Modifier;

public class FieldsExtractorTestHelper {

  static void fieldExtraction(ImmutableList<FieldModel> fieldModels) {
    assertThat(fieldModels.size()).isEqualTo(2);

    FieldSpec extractedIntField = fieldModels.get(0).field;
    assertThat(extractedIntField.name).isEqualTo("intField");
    assertThat(extractedIntField.modifiers.size()).isEqualTo(3);
    assertThat(extractedIntField.hasModifier(Modifier.PRIVATE)).isTrue();
    assertThat(extractedIntField.hasModifier(Modifier.STATIC)).isTrue();
    assertThat(extractedIntField.hasModifier(Modifier.FINAL)).isTrue();

    FieldSpec extractedFloatField = fieldModels.get(1).field;
    assertThat(extractedFloatField.name).isEqualTo("floatField");
    assertThat(extractedFloatField.modifiers.size()).isEqualTo(1);
    assertThat(extractedFloatField.hasModifier(Modifier.STATIC)).isTrue();
  }

  static void noFieldExtraction(ImmutableList<FieldModel> fieldModels) {
    assertThat(fieldModels.isEmpty()).isTrue();
  }
}
