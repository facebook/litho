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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.fixtures.TestLookupElementPresentation;
import java.util.List;
import org.junit.Test;
import org.mockito.Mockito;

public class RequiredPropMethodContributorTest extends LithoPluginIntellijTest {

  public RequiredPropMethodContributorTest() {
    super("testdata/completion");
  }

  @Test
  public void renderElement() {
    LookupElementPresentation testPresentation = new TestLookupElementPresentation();
    LookupElement mockedDelegate = mock(LookupElement.class);

    // Excluding input parameters influence on the render result
    RequiredPropLookupElement.create(mockedDelegate).renderElement(testPresentation);

    assertEquals(" required Prop", testPresentation.getTailText());
    assertTrue(testPresentation.isItemTextUnderlined());
  }

  @Test
  public void isRequiredPropSetter() {
    testHelper.getPsiClass(
        psiClasses -> {
          PsiClass cls = psiClasses.get(0);

          assertTrue(
              RequiredPropMethodContributor.RequiredPropMethodProvider.isRequiredPropSetter(
                  cls.getMethods()[0]));
          assertFalse(
              RequiredPropMethodContributor.RequiredPropMethodProvider.isRequiredPropSetter(
                  cls.getMethods()[1]));
          return true;
        },
        "RequiredPropMethodContributorTest.java");
  }

  @Test
  public void isComponentCreateMethod() {
    PsiMethod mockedMethod = mock(PsiMethod.class);
    PsiClass component = mockComponent();
    when(mockedMethod.getParent()).thenReturn(component);
    when(mockedMethod.getParameters()).thenReturn(new JvmParameter[1]);

    assertFalse(
        RequiredPropMethodContributor.RequiredPropMethodProvider.isComponentCreateMethod(
            mockedMethod));

    when(mockedMethod.getName()).thenReturn("create");
    assertTrue(
        RequiredPropMethodContributor.RequiredPropMethodProvider.isComponentCreateMethod(
            mockedMethod));
  }

  @Test
  public void findRequiredPropSetterNames() {
    testHelper.getPsiClass(
        psiClasses -> {
          PsiClass cls = psiClasses.get(0);
          String[] expected = {"one", "three"};

          List<String> names =
              RequiredPropMethodContributor.RequiredPropMethodProvider.findRequiredPropSetterNames(
                  cls);
          assertEquals(2, names.size());
          assertArrayEquals(expected, names.toArray());
          return true;
        },
        "RequiredPropMethodContributorTest.java");
  }

  static PsiClass mockComponent() {
    // We can't get PsiFile with super parent for testing, thant's why mocking whole hierarchy
    PsiClass mockedComponentLifecycle = mock(PsiClass.class);
    Mockito.when(mockedComponentLifecycle.getName()).thenReturn("ComponentLifecycle");

    PsiClass mockedSubComponent = mock(PsiClass.class);
    Mockito.when(mockedSubComponent.getSuperClass()).thenReturn(mockedComponentLifecycle);

    return mockedSubComponent;
  }
}
