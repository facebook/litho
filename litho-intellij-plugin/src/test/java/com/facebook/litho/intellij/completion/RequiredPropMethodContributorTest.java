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

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.psi.PsiClass;
import com.intellij.testFramework.fixtures.TestLookupElementPresentation;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class RequiredPropMethodContributorTest extends LithoPluginIntellijTest {

  public RequiredPropMethodContributorTest() {
    super("testdata/completion");
  }

  @Test
  public void renderElement() {
    LookupElementPresentation testPresentation = new TestLookupElementPresentation();
    LookupElement mockedDelegate = Mockito.mock(LookupElement.class);

    // Excluding input parameters influence on the render result
    RequiredPropLookupElement.create(mockedDelegate).renderElement(testPresentation);

    Assert.assertEquals(" required Prop", testPresentation.getTailText());
    Assert.assertTrue(testPresentation.isItemTextUnderlined());
  }

  @Test
  public void isRequiredPropSetter() {
    testHelper.getPsiClass(
        psiClasses -> {
          PsiClass cls = psiClasses.get(0);

          Assert.assertTrue(
              RequiredPropMethodContributor.RequiredPropMethodProvider.isRequiredPropSetter(
                  cls.getMethods()[0]));
          Assert.assertFalse(
              RequiredPropMethodContributor.RequiredPropMethodProvider.isRequiredPropSetter(
                  cls.getMethods()[1]));
          return true;
        },
        "RequiredPropMethodContributorTest.java");
  }
}
