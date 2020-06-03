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

package com.facebook.litho.intellij.navigation;

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.junit.Test;

public class SpecMethodFindUsagesHandlerTest extends LithoPluginIntellijTest {

  public SpecMethodFindUsagesHandlerTest() {
    super("testdata/file");
  }

  @Test
  public void getPrimaryElements() {
    testHelper.getPsiClass(
        psiClasses -> {
          final PsiClass targetCls = psiClasses.get(0);
          final PsiMethod[] methods = targetCls.getMethods();
          final PsiMethod staticMethod = methods[0];
          final PsiClass withMethodsClass = targetCls.getInnerClasses()[0];

          final PsiElement[] primaryElements =
              new SpecMethodFindUsagesHandler(staticMethod, cls -> withMethodsClass)
                  .getPrimaryElements();
          assertThat(primaryElements.length).isEqualTo(2);
          assertThat(primaryElements[0].getParent()).isSameAs(withMethodsClass);
          assertThat(primaryElements[1].getParent()).isSameAs(targetCls);
          return true;
        },
        "WithMethods.java");
  }
}
