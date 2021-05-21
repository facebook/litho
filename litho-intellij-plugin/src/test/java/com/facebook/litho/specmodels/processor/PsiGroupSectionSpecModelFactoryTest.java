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
import com.facebook.litho.sections.specmodels.model.GroupSectionSpecModel;
import com.facebook.litho.sections.specmodels.processor.GroupSectionSpecModelFactoryTestHelper;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import org.junit.Before;
import org.junit.Test;

public class PsiGroupSectionSpecModelFactoryTest extends LithoPluginIntellijTest {
  private final PsiGroupSectionSpecModelFactory mFactory = new PsiGroupSectionSpecModelFactory();
  private final DependencyInjectionHelper mDependencyInjectionHelper =
      mock(DependencyInjectionHelper.class);

  private GroupSectionSpecModel mGroupSectionSpecModel;

  public PsiGroupSectionSpecModelFactoryTest() {
    super("testdata/processor");
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    PsiFile psiFile = testHelper.configure("PsiGroupSectionSpecModelFactoryTest.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              mGroupSectionSpecModel =
                  mFactory.createWithPsi(
                      psiFile.getProject(),
                      LithoPluginUtils.getFirstClass(
                              psiFile, cls -> cls.getName().equals("TestGroupSectionSpec"))
                          .get(),
                      mDependencyInjectionHelper);
            });
  }

  @Test
  public void psiGroupSectionSpecModelFactory_forGroupSectionSpec_populateGenericSpecInfo() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              GroupSectionSpecModelFactoryTestHelper
                  .create_forGroupSectionSpec_populateGenericSpecInfo(
                      mGroupSectionSpecModel, mDependencyInjectionHelper);
            });
  }

  @Test
  public void psiGroupSectionSpecModelFactory_forGroupSectionSpec_populateServiceInfo() {
    GroupSectionSpecModelFactoryTestHelper.create_forGroupSectionSpec_populateServiceInfo(
        mGroupSectionSpecModel);
  }

  @Test
  public void create_forGroupSectionSpec_populateTriggerInfo() {
    GroupSectionSpecModelFactoryTestHelper.create_forGroupSectionSpec_populateTriggerInfo(
        mGroupSectionSpecModel);
  }

  @Test
  public void create_forGroupSectionSpec_populateEventInfo() {
    GroupSectionSpecModelFactoryTestHelper.create_forGroupSectionSpec_populateEventInfo(
        mGroupSectionSpecModel);
  }

  @Test
  public void create_forGroupSectionSpec_populateUpdateStateInfo() {
    GroupSectionSpecModelFactoryTestHelper.create_forGroupSectionSpec_populateUpdateStateInfo(
        mGroupSectionSpecModel);
  }
}
