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

import static com.facebook.litho.specmodels.processor.LayoutSpecModelFactory.INTER_STAGE_INPUT_ANNOTATIONS;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.intellij.psi.PsiClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link PsiEventMethodExtractor} */
@RunWith(JUnit4.class)
public class PsiEventMethodExtractorTest extends LithoPluginIntellijTest {
  public PsiEventMethodExtractorTest() {
    super("testdata/processor");
  }

  @Test
  public void psiMethodExtraction() {
    testHelper.getPsiClass(
        psiClasses -> {
          Assert.assertNotNull(psiClasses);
          PsiClass psiClass = psiClasses.get(0);

          ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> methods =
              PsiEventMethodExtractor.getOnEventMethods(psiClass, INTER_STAGE_INPUT_ANNOTATIONS);

          EventMethodExtractorTestHelper.assertMethodExtraction(methods);
          return true;
        },
        "PsiEventMethodExtractionClass.java");
  }
}
