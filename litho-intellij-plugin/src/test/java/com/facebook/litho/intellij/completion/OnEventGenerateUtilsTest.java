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

package com.facebook.litho.intellij.completion;

import com.facebook.litho.intellij.LithoClassNames;
import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public class OnEventGenerateUtilsTest extends LithoPluginIntellijTest {

  public OnEventGenerateUtilsTest() {
    super("testdata/completion");
  }

  @Test
  public void createOnEventMethod() {
    testHelper.getPsiClass(
        psiClasses -> {
          Assert.assertNotNull(psiClasses);
          PsiClass psiEvent = psiClasses.get(0);

          PsiMethod onEventMethod =
              OnEventGenerateUtils.createOnEventMethod(psiEvent, psiEvent, Collections.emptyList());

          Assert.assertEquals(2, onEventMethod.getParameters().length);
          PsiType contextType = (PsiType) onEventMethod.getParameters()[0].getType();
          Assert.assertEquals(
              LithoClassNames.COMPONENT_CONTEXT_CLASS_NAME, contextType.getCanonicalText());
          Assert.assertEquals("boolean", onEventMethod.getReturnType().getCanonicalText());
          Assert.assertEquals("onReturnEvent", onEventMethod.getName());

          return true;
        },
        "ReturnEvent.java");
  }

  @Test
  public void createOnEventMethodInSection() {
    testHelper.getPsiClass(
        psiClasses -> {
          Assert.assertNotNull(psiClasses);
          PsiClass hasSectionAnnotation = psiClasses.get(0);

          PsiMethod onEventMethod =
              OnEventGenerateUtils.createOnEventMethod(
                  hasSectionAnnotation, hasSectionAnnotation, Collections.emptyList());

          Assert.assertEquals(1, onEventMethod.getParameters().length);
          PsiType contextType = (PsiType) onEventMethod.getParameters()[0].getType();
          Assert.assertEquals(
              LithoClassNames.SECTION_CONTEXT_CLASS_NAME, contextType.getCanonicalText());

          return true;
        },
        "OnEventSectionSpec.java");
  }
}
