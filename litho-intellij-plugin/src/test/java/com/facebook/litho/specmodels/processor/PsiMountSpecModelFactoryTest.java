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

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.MountSpecModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;

public class PsiMountSpecModelFactoryTest extends LithoPluginIntellijTest {
  private final PsiMountSpecModelFactory mFactory = new PsiMountSpecModelFactory();
  private final DependencyInjectionHelper mDependencyInjectionHelper =
      mock(DependencyInjectionHelper.class);

  private PsiFile mPsiFile;

  public PsiMountSpecModelFactoryTest() {
    super("testdata/processor");
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    mPsiFile = testHelper.configure("PsiMountSpecModelFactoryTest.java");
  }

  @Test
  public void createWithPsi_forMountSpecWithExplicitMountType_populateGenericSpecInfo() {
    verifyCreateWithPsiForSpec(
        "TestMountSpecWithExplicitMountType",
        mountSpecModel ->
            MountSpecModelFactoryTestHelper
                .create_forMountSpecWithExplicitMountType_populateGenericSpecInfo(
                    mountSpecModel, mDependencyInjectionHelper));
  }

  @Test
  public void createWithPsi_forMountSpecWithExplicitMountType_populateOnAttachInfo() {
    verifyCreateWithPsiForSpec(
        "TestMountSpecWithExplicitMountType",
        mountSpecModel ->
            MountSpecModelFactoryTestHelper
                .create_forMountSpecWithExplicitMountType_populateOnAttachInfo(mountSpecModel));
  }

  @Test
  public void createWithPsi_forMountSpecWithExplicitMountType_populateOnDetachInfo() {
    verifyCreateWithPsiForSpec(
        "TestMountSpecWithExplicitMountType",
        mountSpecModel ->
            MountSpecModelFactoryTestHelper
                .create_forMountSpecWithExplicitMountType_populateOnDetachInfo(mountSpecModel));
  }

  @Test
  public void createWithPsi_forMountSpecWithImplicitMountType_populateMountType() {
    verifyCreateWithPsiForSpec(
        "TestMountSpecWithImplicitMountType",
        mountSpecModel ->
            assertThat(mountSpecModel.getMountType())
                .isEqualTo(ClassNames.COMPONENT_MOUNT_TYPE_DRAWABLE));
  }

  @Test
  public void createWithPsi_forMountSpecWithoutMountType_hasMountTypeNone() {
    verifyCreateWithPsiForSpec(
        "TestMountSpecWithoutMountType",
        mountSpecModel ->
            assertThat(mountSpecModel.getMountType())
                .isEqualTo(ClassNames.COMPONENT_MOUNT_TYPE_NONE));
  }

  private void verifyCreateWithPsiForSpec(String specName, Consumer<MountSpecModel> assertion) {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final MountSpecModel mountSpecModel =
                  mFactory.createWithPsi(
                      mPsiFile.getProject(),
                      LithoPluginUtils.getFirstClass(
                              mPsiFile, cls -> specName.equals(cls.getName()))
                          .get(),
                      mDependencyInjectionHelper);
              assertion.accept(mountSpecModel);
            });
  }
}
