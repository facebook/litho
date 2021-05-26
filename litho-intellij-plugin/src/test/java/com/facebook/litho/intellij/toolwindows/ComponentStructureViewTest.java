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

package com.facebook.litho.intellij.toolwindows;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.services.ComponentGenerateService;
import com.intellij.ide.structureView.StructureView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.impl.ToolWindowHeadlessManagerImpl;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import java.io.IOException;
import java.util.function.Consumer;
import org.junit.Test;
import org.mockito.Mockito;

public class ComponentStructureViewTest extends LithoPluginIntellijTest {
  private ComponentStructureView componentStructureView;

  public ComponentStructureViewTest() {
    super("testdata/file");
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

    final CodeInsightTestFixture fixture = testHelper.getFixture();
    final Project project = fixture.getProject();

    final ToolWindowManager toolWindowManager = new ToolWindowHeadlessManagerImpl(project);
    final ToolWindow toolWindow =
        toolWindowManager.registerToolWindow("Litho Spec", true, ToolWindowAnchor.TOP);

    final ToolWindow visibleToolWindow = Mockito.spy(toolWindow);
    when(visibleToolWindow.isVisible()).thenReturn(true);

    componentStructureView = ComponentStructureView.getInstance(project);
    componentStructureView.setup(visibleToolWindow);
  }

  @Test
  public void updateView_forLayoutSpec_setStructureView() throws IOException {
    verifyUpdateViewCallForFile(
        "LayoutSpec.java",
        structureView -> assertThat(structureView).isInstanceOf(StructureView.class));
  }

  @Test
  public void updateView_forMountSpec_setStructureView() throws IOException {
    verifyUpdateViewCallForFile(
        "MountSpec.java",
        structureView -> assertThat(structureView).isInstanceOf(StructureView.class));
  }

  @Test
  public void updateView_forSectionSpec_setStructureView() throws IOException {
    verifyUpdateViewCallForFile(
        "SectionSpec.java",
        structureView -> assertThat(structureView).isInstanceOf(StructureView.class));
  }

  @Test
  public void updateView_forNotSpec_emptyStructureView() throws IOException {
    verifyUpdateViewCallForFile(
        "NotSpec.java", structureView -> assertThat(structureView).isNull());
  }

  private void verifyUpdateViewCallForFile(String filename, Consumer<StructureView> assertion)
      throws IOException {
    final PsiFile file = testHelper.configure(filename);

    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiClass psiClass = LithoPluginUtils.getFirstClass(file, cls -> true).get();
              // Tool window subscription to ComponentGenerateService triggers updateView
              ComponentGenerateService.getInstance().getOrCreateSpecModel(psiClass);
              assertion.accept(componentStructureView.structureView);
            });
  }
}
