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

package com.facebook.litho.intellij.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.specmodels.model.SpecModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ComponentGenerateServiceTest extends LithoPluginIntellijTest {

  public ComponentGenerateServiceTest() {
    super("testdata/services");
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    PsiSearchUtils.getInstance().addMock("Layout", null);
  }

  @After
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    PsiSearchUtils.getInstance().clearMocks();
  }

  @Test
  public void
      getSpecModel_whenLayoutSpecModelForDifferentPsiClassInstanceWithSameFqnExists_returnsExistingSpecModel()
          throws IOException {
    getSpecModel_whenSpecModelForDifferentPsiClassInstanceWithSameFqnExists_returnsExistingSpecModel(
        "InitialLayoutSpec.java");
  }

  @Test
  public void
      getSpecModel_whenMountSpecModelForDifferentPsiClassInstanceWithSameFqnExists_returnsExistingSpecModel()
          throws IOException {
    getSpecModel_whenSpecModelForDifferentPsiClassInstanceWithSameFqnExists_returnsExistingSpecModel(
        "InitialMountSpec.java");
  }

  private void
      getSpecModel_whenSpecModelForDifferentPsiClassInstanceWithSameFqnExists_returnsExistingSpecModel(
          String fileName) throws IOException {
    final PsiFile psiFile1 = testHelper.configure(fileName);
    final PsiFile psiFile2 = testHelper.configure(fileName);
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiClass psiClass1 =
                  LithoPluginUtils.getFirstClass(psiFile1, psiClass -> true).get();
              final PsiClass psiClass2 =
                  LithoPluginUtils.getFirstClass(psiFile2, psiClass -> true).get();
              assertThat(psiClass1.getQualifiedName()).isEqualTo(psiClass2.getQualifiedName());

              final SpecModel specModel1 =
                  ComponentGenerateService.getInstance().getOrCreateSpecModel(psiClass1);
              final SpecModel specModel2 =
                  ComponentGenerateService.getInstance().getOrCreateSpecModel(psiClass2);
              assertThat(specModel2).isNotNull();
              assertThat(specModel2).isEqualTo(specModel1);
            });
  }
}
