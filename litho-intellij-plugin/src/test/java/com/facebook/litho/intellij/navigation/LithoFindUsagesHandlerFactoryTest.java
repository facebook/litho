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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.junit.Test;

public class LithoFindUsagesHandlerFactoryTest extends LithoPluginIntellijTest {

  public LithoFindUsagesHandlerFactoryTest() {
    super("testdata/file");
  }

  @Test
  public void canFindUsages_classes() {
    testHelper.getPsiClass(
        psiClasses -> {
          assertNotNull(psiClasses);
          PsiClass layoutSpec = psiClasses.get(0);
          PsiClass otherSpec = psiClasses.get(1);
          PsiClass sectionSpec = psiClasses.get(2);
          PsiClass notSpec = psiClasses.get(3);

          LithoFindUsagesHandlerFactory factory = new LithoFindUsagesHandlerFactory();
          assertTrue(factory.canFindUsages(layoutSpec));
          assertTrue(factory.canFindUsages(otherSpec));
          assertTrue(factory.canFindUsages(sectionSpec));
          assertFalse(factory.canFindUsages(notSpec));

          return true;
        },
        "LayoutSpec.java",
        "MountSpec.java",
        "SectionSpec.java",
        "NotSpec.java");
  }

  @Test
  public void canFindUsages_methods() {
    testHelper.getPsiClass(
        psiClasses -> {
          final PsiMethod[] methods = psiClasses.get(0).getMethods();
          final PsiMethod staticMethod = methods[0];
          final PsiMethod nonStaticMethod = methods[1];

          LithoFindUsagesHandlerFactory factory = new LithoFindUsagesHandlerFactory();
          assertTrue(factory.canFindUsages(staticMethod));
          assertFalse(factory.canFindUsages(nonStaticMethod));

          return true;
        },
        "WithMethods.java");
  }
}
