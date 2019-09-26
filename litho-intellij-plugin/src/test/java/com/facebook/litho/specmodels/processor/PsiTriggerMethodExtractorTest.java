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

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.intellij.psi.PsiClass;
import com.squareup.javapoet.ClassName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link PsiTriggerMethodExtractor} */
@RunWith(JUnit4.class)
public class PsiTriggerMethodExtractorTest extends LithoPluginIntellijTest {
  public PsiTriggerMethodExtractorTest() {
    super("testdata/processor");
  }

  @Test
  public void psiMethodExtraction() {
    testHelper.getPsiClass(
        psiClasses -> {
          Assert.assertNotNull(psiClasses);
          PsiClass psiClass = psiClasses.get(0);
          List<Class<? extends Annotation>> permittedParamAnnotations = new ArrayList<>();

          ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> methods =
              PsiTriggerMethodExtractor.getOnTriggerMethods(psiClass, permittedParamAnnotations);

          TriggerMethodExtractorTestHelper.assertMethodExtraction(
              methods, ClassName.bestGuess("TestEvent"));
          return true;
        },
        "PsiTriggerMethodExtractionClass.java");
  }
}
