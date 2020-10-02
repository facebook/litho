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
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import java.io.IOException;
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
    PsiSearchUtils.addMock("Layout", null);
  }

  @Test
  public void updateLayoutComponentAsync_specNotChanged_sameInmemoryComponent() throws IOException {
    final PsiFile file = testHelper.configure("LayoutSpec2.java");
    final PsiFile fileNotChanged = testHelper.configure("LayoutSpec3.java");
    final Project project = testHelper.getProject();
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiClass spec = LithoPluginUtils.getFirstLayoutSpec(file).get();
              ComponentGenerateService.getInstance().updateLayoutComponentAsync(spec);
              final PsiClass cls =
                  ComponentsCacheService.getInstance(project).getComponent("Layout");
              assertThat(cls).isNotNull();

              final PsiClass specNotChanged =
                  LithoPluginUtils.getFirstLayoutSpec(fileNotChanged).get();
              ComponentGenerateService.getInstance().updateLayoutComponentAsync(specNotChanged);
              final PsiClass componentNotChanged =
                  ComponentsCacheService.getInstance(project).getComponent("Layout");
              assertThat(componentNotChanged).isSameAs(cls);
            });
  }

  @Test
  public void updateLayoutComponentAsync_specChanged_notSameInmemoryComponent() throws IOException {
    final PsiFile file = testHelper.configure("LayoutSpec.java");
    final PsiFile fileChanged = testHelper.configure("LayoutSpec2.java");
    final Project project = testHelper.getProject();
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiClass spec = LithoPluginUtils.getFirstLayoutSpec(file).get();
              ComponentGenerateService.getInstance().updateLayoutComponentAsync(spec);
              final PsiClass cls =
                  ComponentsCacheService.getInstance(project).getComponent("Layout");
              assertThat(cls).isNotNull();

              final PsiClass specChanged = LithoPluginUtils.getFirstLayoutSpec(fileChanged).get();
              ComponentGenerateService.getInstance().updateLayoutComponentAsync(specChanged);
              final PsiClass componentChanged =
                  ComponentsCacheService.getInstance(project).getComponent("Layout");
              assertThat(componentChanged).isNotSameAs(cls);
            });
  }

  @Test
  public void
      getSpecModel_whenSpecModelForDifferentPsiClassInstanceWithSameFqnExists_returnsExistingSpecModel()
          throws IOException {
    final PsiFile psiFile1 = testHelper.configure("LayoutSpec.java");
    final PsiFile psiFile2 = testHelper.configure("LayoutSpec.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiClass psiClass1 = LithoPluginUtils.getFirstLayoutSpec(psiFile1).get();
              final PsiClass psiClass2 = LithoPluginUtils.getFirstLayoutSpec(psiFile2).get();
              assertThat(psiClass1.getQualifiedName()).isEqualTo(psiClass2.getQualifiedName());

              ComponentGenerateService.getInstance().updateLayoutComponentSync(psiClass1);
              final SpecModel specModel1 =
                  ComponentGenerateService.getInstance().getSpecModel(psiClass1);
              final SpecModel specModel2 =
                  ComponentGenerateService.getInstance().getSpecModel(psiClass2);
              assertThat(specModel2).isNotNull();
              assertThat(specModel2).isEqualTo(specModel1);
            });
  }
}
