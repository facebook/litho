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

import com.facebook.litho.annotations.Event;
import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.DelegateMethodDescriptions;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.intellij.psi.PsiClass;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class PsiDelegateMethodExtractorTest extends LithoPluginIntellijTest {
  public PsiDelegateMethodExtractorTest() {
    super("testdata/processor");
  }

  @Test
  public void delegateMethodExtraction() {
    testHelper.getPsiClass(
        psiClasses -> {
          Assert.assertNotNull(psiClasses);
          PsiClass cls = psiClasses.get(0);

          List<Class<? extends Annotation>> permittedParamAnnotations = new ArrayList<>();
          permittedParamAnnotations.add(Event.class);

          ImmutableList<SpecMethodModel<DelegateMethod, Void>> delegateMethods =
              PsiDelegateMethodExtractor.getDelegateMethods(
                  cls,
                  new ArrayList<>(
                      DelegateMethodDescriptions.LAYOUT_SPEC_DELEGATE_METHODS_MAP.keySet()),
                  permittedParamAnnotations,
                  ImmutableList.<Class<? extends Annotation>>of());

          PsiDelegateMethodExtractorTestHelper.assertDelegateMethodExtraction(delegateMethods);
          return true;
        },
        "DelegateMethodExtractionClass.java");
  }
}
