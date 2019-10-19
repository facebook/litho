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

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.PropDefaultModel;
import com.intellij.psi.PsiClass;
import org.junit.Assert;
import org.junit.Test;

public class PsiPropDefaultsExtractorTest extends LithoPluginIntellijTest {
  public PsiPropDefaultsExtractorTest() {
    super("testdata/processor");
  }

  @Test
  public void propDefaultExtractionWithField() {
    testHelper.getPsiClass(
        psiClasses -> {
          Assert.assertNotNull(psiClasses);
          PsiClass cls = psiClasses.get(0);

          final ImmutableList<PropDefaultModel> propDefaults =
              PsiPropDefaultsExtractor.getPropDefaults(cls);

          PropDefaultsExtractorTestHelper.assertPropDefaultsExtraction(propDefaults);
          return true;
        },
        "PropDefaultExtractionWithField.java");
  }
}
