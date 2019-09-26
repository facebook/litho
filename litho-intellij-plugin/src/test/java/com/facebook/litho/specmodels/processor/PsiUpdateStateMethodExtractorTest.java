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

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.UpdateStateMethod;
import com.intellij.psi.PsiClass;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class PsiUpdateStateMethodExtractorTest extends LithoPluginIntellijTest {
  public PsiUpdateStateMethodExtractorTest() {
    super("testdata/processor");
  }

  @Test
  public void methodExtraction() {
    testHelper.getPsiClass(
        psiClasses -> {
          Assert.assertNotNull(psiClasses);
          PsiClass cls = psiClasses.get(0);

          List<Class<? extends Annotation>> permittedParamAnnotations = new ArrayList<>();

          ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> methods =
              PsiUpdateStateMethodExtractor.getOnUpdateStateMethods(
                  cls, permittedParamAnnotations, false);

          UpdateStateMethodExtractorTestHelper.assertMethodExtraction(methods);
          return true;
        },
        "UpdateStateMethodExtraction.java");
  }
}
