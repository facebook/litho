/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.sections.specmodels.model.DiffSectionSpecModel;
import com.facebook.litho.sections.specmodels.model.SpecModelValidation;
import com.facebook.litho.sections.specmodels.processor.DiffSectionSpecModelFactoryTestHelper;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class PsiDiffSectionSpecModelFactoryTest extends LithoPluginIntellijTest {
  private final PsiDiffSectionSpecModelFactory mFactory = new PsiDiffSectionSpecModelFactory();
  private final DependencyInjectionHelper mDependencyInjectionHelper =
      mock(DependencyInjectionHelper.class);

  private DiffSectionSpecModel mDiffSectionSpecModel;

  public PsiDiffSectionSpecModelFactoryTest() {
    super("testdata/processor");
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    final PsiFile psiFile = testHelper.configure("PsiDiffSectionSpecModelFactoryTest.java");

    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              PsiClass psiClass =
                  LithoPluginUtils.getFirstClass(
                          psiFile, cls -> cls.getName().equals("TestDiffSectionSpec"))
                      .get();
              mDiffSectionSpecModel =
                  mFactory.createWithPsi(
                      psiFile.getProject(), psiClass, mDependencyInjectionHelper);
            });
  }

  @Test
  public void psiDiffSectionSpecModelFactory_forDiffSectionSpec_populateGenericSpecInfo() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () ->
                DiffSectionSpecModelFactoryTestHelper
                    .create_forDiffSectionSpec_populateGenericSpecInfo(mDiffSectionSpecModel));
  }

  @Test
  public void psiDiffSectionSpecModelFactory_forDiffSectionadspec_populateGenericSpecInfo() {

    final List<SpecModelValidationError> validationErrors =
        SpecModelValidation.validateDiffSectionSpecModel(mDiffSectionSpecModel, RunMode.normal());
    assertThat(validationErrors).isEmpty();
  }
}
