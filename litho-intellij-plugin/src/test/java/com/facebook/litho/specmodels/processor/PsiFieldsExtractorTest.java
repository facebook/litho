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

import com.facebook.litho.intellij.LithoPluginTestHelper;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.FieldModel;
import com.intellij.psi.PsiClass;
import org.junit.After;
import org.junit.Assert;
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
        psiClasses -> {
          Assert.assertNotNull(psiClasses);
          PsiClass psiClass = psiClasses.get(0);

          ImmutableList<FieldModel> fieldModels = PsiFieldsExtractor.extractFields(psiClass);
          FieldsExtractorTestHelper.fieldExtraction(fieldModels);
          return true;
        },
        "TwoFieldsClass.java");
  }

  @Test
  public void extractNoFields() {
    testHelper.getPsiClass(
        psiClasses -> {
          Assert.assertNotNull(psiClasses);
          PsiClass psiClass = psiClasses.get(0);

          ImmutableList<FieldModel> fieldModels = PsiFieldsExtractor.extractFields(psiClass);
          FieldsExtractorTestHelper.noFieldExtraction(fieldModels);
          return true;
        },
        "NoFieldsClass.java");
  }
}
