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

package com.facebook.litho.specmodels.processor;

import static org.mockito.Mockito.mock;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
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
    super("testdata/processor");
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
  public void createWithPsi_forLayoutSpec_populateGenericSpecInfo() {
    LayoutSpecModelFactoryTestHelper.create_forLayoutSpec_populateGenericSpecInfo(
        mLayoutSpecModel, mDependencyInjectionHelper);
  }

  @Test
  public void createWithPsi_forLayoutSpec_populateOnAttachInfo() {
    LayoutSpecModelFactoryTestHelper.create_forLayoutSpec_populateOnAttachInfo(mLayoutSpecModel);
  }

  @Test
  public void createWithPsi_forLayoutSpec_populateOnDetachInfo() {
    LayoutSpecModelFactoryTestHelper.create_forLayoutSpec_populateOnDetachInfo(mLayoutSpecModel);
  }
}
