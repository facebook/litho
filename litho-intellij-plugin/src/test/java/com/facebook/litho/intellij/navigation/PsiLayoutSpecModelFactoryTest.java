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

import static org.mockito.Mockito.mock;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactoryTestHelper;
import com.facebook.litho.specmodels.processor.PsiLayoutSpecModelFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import org.junit.Before;
import org.junit.Test;

public class PsiLayoutSpecModelFactoryTest extends LithoPluginIntellijTest {
  private final PsiLayoutSpecModelFactory mFactory = new PsiLayoutSpecModelFactory();
  private final DependencyInjectionHelper mDependencyInjectionHelper =
      mock(DependencyInjectionHelper.class);

  private LayoutSpecModel mLayoutSpecModel;

  public PsiLayoutSpecModelFactoryTest() {
    super("testdata/navigation");
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    PsiFile psiFile = testHelper.configure("PsiLayoutSpecModelFactoryTest.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              mLayoutSpecModel =
                  mFactory.createWithPsi(
                      psiFile.getProject(),
                      LithoPluginUtils.getFirstLayoutSpec(psiFile).get(),
                      mDependencyInjectionHelper);
            });
  }

  @Test
  public void layoutSpec_initModel_populateGenericSpecInfo() {
    LayoutSpecModelFactoryTestHelper.layoutSpec_initModel_populateGenericSpecInfo(
        mLayoutSpecModel, mDependencyInjectionHelper);
  }

  @Test
  public void layoutSpec_initModel_populateOnAttachInfo() {
    LayoutSpecModelFactoryTestHelper.layoutSpec_initModel_populateOnAttachInfo(mLayoutSpecModel);
  }

  @Test
  public void layoutSpec_initModel_populateOnDetachInfo() {
    LayoutSpecModelFactoryTestHelper.layoutSpec_initModel_populateOnDetachInfo(mLayoutSpecModel);
  }
}
