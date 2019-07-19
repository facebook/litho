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

import static org.junit.Assert.*;

import com.facebook.litho.intellij.LithoPluginTestHelper;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import java.util.Optional;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ComponentFindUsagesHandlerFactoryTest {
  private final LithoPluginTestHelper testHelper = new LithoPluginTestHelper("testdata/file");

  @Before
  public void setUp() throws Exception {
    testHelper.setUp();
  }

  @After
  public void tearDown() throws Exception {
    testHelper.tearDown();
  }

  @Test
  public void canFindUsages() {
    testHelper.getPsiClass(
        psiClasses -> {
          Assert.assertNotNull(psiClasses);
          PsiClass layoutSpec = psiClasses.get(0);
          PsiClass otherSpec = psiClasses.get(1);
          PsiClass notSpec = psiClasses.get(2);

          ComponentFindUsagesHandlerFactory factory = new ComponentFindUsagesHandlerFactory();
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

    ComponentFindUsagesHandlerFactory.ComponentFindUsagesHandler handler =
        new ComponentFindUsagesHandlerFactory.ComponentFindUsagesHandler(
            mockedElement, psiClass -> Optional.of(mockedResult));

    // Includes both original element and "component" element to search for
    PsiElement[] primaryElements = handler.getPrimaryElements();
    Assert.assertEquals(2, primaryElements.length);
    Assert.assertEquals(mockedResult, primaryElements[0]);
    Assert.assertEquals(mockedElement, primaryElements[1]);
  }
}
