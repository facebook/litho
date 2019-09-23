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
package com.facebook.litho.intellij.completion;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;

public class CompletionUtilsTest extends LithoPluginIntellijTest {

  public CompletionUtilsTest() {
    super("testdata/completion");
  }

  @Test
  public void findFirstParent() {
    testHelper.getPsiClass(
        psiClasses -> {
          Assert.assertNotNull(psiClasses);
          Assert.assertEquals(2, psiClasses.size());

          PsiMethod layoutSpecMethod = psiClasses.get(0).getMethods()[0];
          Optional<PsiClass> layoutSpec =
              CompletionUtils.findFirstParent(layoutSpecMethod, LithoPluginUtils::isLayoutSpec);
          Assert.assertTrue(layoutSpec.isPresent());
          Optional<PsiClass> componentCls =
              CompletionUtils.findFirstParent(layoutSpecMethod, LithoPluginUtils::isComponentClass);
          Assert.assertFalse(componentCls.isPresent());

          PsiMethod sectionSpecMethod = psiClasses.get(1).getMethods()[0];
          Optional<PsiClass> layoutSpecInSection =
              CompletionUtils.findFirstParent(sectionSpecMethod, LithoPluginUtils::isLayoutSpec);
          Assert.assertFalse(layoutSpecInSection.isPresent());
          Optional<PsiClass> lithoSpec =
              CompletionUtils.findFirstParent(sectionSpecMethod, LithoPluginUtils::isLithoSpec);
          Assert.assertTrue(lithoSpec.isPresent());
          return true;
        },
        "LayoutSpec.java",
        "SectionSpec.java");
  }
}
