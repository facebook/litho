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

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.FieldModel;
import com.squareup.javapoet.FieldSpec;
import javax.lang.model.element.Modifier;
import org.junit.Assert;

public class FieldsExtractorTestHelper {

  static void fieldExtraction(ImmutableList<FieldModel> fieldModels) {
    Assert.assertEquals(2, fieldModels.size());

    FieldSpec extractedIntField = fieldModels.get(0).field;
    Assert.assertEquals("intField", extractedIntField.name);
    Assert.assertEquals(3, extractedIntField.modifiers.size());
    Assert.assertTrue(extractedIntField.hasModifier(Modifier.PRIVATE));
    Assert.assertTrue(extractedIntField.hasModifier(Modifier.STATIC));
    Assert.assertTrue(extractedIntField.hasModifier(Modifier.FINAL));

    FieldSpec extractedFloatField = fieldModels.get(1).field;
    Assert.assertEquals("floatField", extractedFloatField.name);
    Assert.assertEquals(1, extractedFloatField.modifiers.size());
    Assert.assertTrue(extractedFloatField.hasModifier(Modifier.STATIC));
  }

  static void noFieldExtraction(ImmutableList<FieldModel> fieldModels) {
    Assert.assertTrue(fieldModels.isEmpty());
  }
}
