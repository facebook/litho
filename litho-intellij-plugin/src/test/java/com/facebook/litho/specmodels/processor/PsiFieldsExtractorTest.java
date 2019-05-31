/*
 * Copyright 2019-present Facebook, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.facebook.litho.intellij.LithoPluginTestHelper;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.FieldModel;
import com.squareup.javapoet.FieldSpec;
import javax.lang.model.element.Modifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PsiFieldsExtractorTest {

  private final LithoPluginTestHelper testHelper = new LithoPluginTestHelper("testdata/processor");

  @Before
  public void setUp() throws Exception {
    testHelper.setUp();
  }

  @After
  public void tearDown() throws Exception {
    testHelper.tearDown();
  }

  @Test
  public void extractFields() {
    testHelper.getPsiClass(
        "TwoFieldsClass.java",
        psiClass -> {
          assertNotNull(psiClass);

          ImmutableList<FieldModel> fieldModels = PsiFieldsExtractor.extractFields(psiClass);

          assertEquals(fieldModels.size(), 2);

          FieldSpec extractedIntField = fieldModels.get(0).field;
          assertEquals(extractedIntField.name, "intField");
          assertEquals(extractedIntField.modifiers.size(), 3);
          assertTrue(extractedIntField.hasModifier(Modifier.PUBLIC));
          assertTrue(extractedIntField.hasModifier(Modifier.STATIC));
          assertTrue(extractedIntField.hasModifier(Modifier.FINAL));

          FieldSpec extractedFloatField = fieldModels.get(1).field;
          assertEquals(extractedFloatField.name, "floatField");
          assertEquals(extractedFloatField.modifiers.size(), 1);
          assertTrue(extractedFloatField.hasModifier(Modifier.STATIC));

          return true;
        });
  }

  @Test
  public void extractNoFields() {
    testHelper.getPsiClass(
        "NoFieldsClass.java",
        psiClass -> {
          assertNotNull(psiClass);

          ImmutableList<FieldModel> fieldModels = PsiFieldsExtractor.extractFields(psiClass);

          assertEquals(fieldModels.size(), 0);

          return true;
        });
  }
}
