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
package com.facebook.litho.intellij.file;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class LithoFindUsagesHandlerFactoryTest extends LithoPluginIntellijTest {

  public LithoFindUsagesHandlerFactoryTest() {
    super("testdata/file");
  }

  @Test
  public void canFindUsages() {
    testHelper.getPsiClass(
        psiClasses -> {
          Assert.assertNotNull(psiClasses);
          PsiClass layoutSpec = psiClasses.get(0);
          PsiClass otherSpec = psiClasses.get(1);
          PsiClass notSpec = psiClasses.get(2);

          LithoFindUsagesHandlerFactory factory = new LithoFindUsagesHandlerFactory();
          Assert.assertTrue(factory.canFindUsages(layoutSpec));
          Assert.assertTrue(factory.canFindUsages(otherSpec));
          Assert.assertFalse(factory.canFindUsages(notSpec));

          return true;
        },
        "LayoutSpec.java",
        "OtherSpec.java",
        "NotSpec.java");
  }

  @Test
  public void findUsages() {
    PsiClass mockedElement = Mockito.mock(PsiClass.class);
    PsiClass mockedResult = Mockito.mock(PsiClass.class);

    LithoFindUsagesHandlerFactory.GeneratedClassFindUsagesHandler handler =
        new LithoFindUsagesHandlerFactory.GeneratedClassFindUsagesHandler(
            mockedElement, psiClass -> Optional.of(mockedResult));

    // Includes both original element and "component" element to search for
    PsiElement[] primaryElements = handler.getPrimaryElements();
    Assert.assertEquals(2, primaryElements.length);
    Assert.assertEquals(mockedResult, primaryElements[0]);
    Assert.assertEquals(mockedElement, primaryElements[1]);
  }
}
