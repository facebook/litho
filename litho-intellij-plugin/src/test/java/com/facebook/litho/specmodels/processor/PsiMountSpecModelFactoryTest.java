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
import com.facebook.litho.specmodels.model.MountSpecModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import org.junit.Before;
import org.junit.Test;

public class PsiMountSpecModelFactoryTest extends LithoPluginIntellijTest {
  private final PsiMountSpecModelFactory mFactory = new PsiMountSpecModelFactory();
  private final DependencyInjectionHelper mDependencyInjectionHelper =
      mock(DependencyInjectionHelper.class);

  private PsiFile mPsiFile;
  private MountSpecModel mMountSpecModel;

  public PsiMountSpecModelFactoryTest() {
    super("testdata/processor");
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    mPsiFile = testHelper.configure("PsiMountSpecModelFactoryTest.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              mMountSpecModel =
                  mFactory.createWithPsi(
                      mPsiFile.getProject(),
                      LithoPluginUtils.getFirstClass(
                              mPsiFile, cls -> "TestMountSpec".equals(cls.getName()))
                          .get(),
                      mDependencyInjectionHelper);
            });
  }

  @Test
  public void mountSpec_initModel_populateGenericSpecInfo() {
    MountSpecModelFactoryTestHelper.mountSpec_initModel_populateGenericSpecInfo(
        mMountSpecModel, mDependencyInjectionHelper);
  }

  @Test
  public void mountSpec_initModel_populateOnAttachInfo() {
    MountSpecModelFactoryTestHelper.mountSpec_initModel_populateOnAttachInfo(mMountSpecModel);
  }

  @Test
  public void mountSpec_initModel_populateOnDetachInfo() {
    MountSpecModelFactoryTestHelper.mountSpec_initModel_populateOnDetachInfo(mMountSpecModel);
  }

  @Test
  public void mountSpecWithImplicitMountType_initModel_populateMountType() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final MountSpecModel mountSpecModelWithImplicitMountType =
                  mFactory.createWithPsi(
                      mPsiFile.getProject(),
                      LithoPluginUtils.getFirstClass(
                              mPsiFile,
                              cls -> "TestMountSpecWithImplicitMountType".equals(cls.getName()))
                          .get(),
                      mDependencyInjectionHelper);
              MountSpecModelFactoryTestHelper
                  .mountSpecWithImplicitMountType_initModel_populateMountType(
                      mountSpecModelWithImplicitMountType);
            });
  }

  @Test
  public void mountSpecWithoutMountType_initModel_hasMountTypeNone() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final MountSpecModel mountSpecModelWithoutMountType =
                  mFactory.createWithPsi(
                      mPsiFile.getProject(),
                      LithoPluginUtils.getFirstClass(
                              mPsiFile,
                              cls -> "TestMountSpecWithoutMountType".equals(cls.getName()))
                          .get(),
                      mDependencyInjectionHelper);
              MountSpecModelFactoryTestHelper.mountSpecWithoutMountType_initModel_hasMountTypeNone(
                  mountSpecModelWithoutMountType);
            });
  }
}
