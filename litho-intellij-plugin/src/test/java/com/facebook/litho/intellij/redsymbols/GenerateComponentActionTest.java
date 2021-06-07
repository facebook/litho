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

package com.facebook.litho.intellij.redsymbols;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import java.io.IOException;
import org.junit.Test;

public class GenerateComponentActionTest extends LithoPluginIntellijTest {

  public GenerateComponentActionTest() {
    super("testdata/file");
  }

  @Test
  public void getValidSpec_whenFileHasLayoutSpec_returnsSpec() throws IOException {
    final PsiFile psiFile = testHelper.configure("LayoutSpec.java");
    final AnActionEvent mock = mockEvent(psiFile);
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              assertThat(GenerateComponentAction.getValidSpec(mock).isPresent()).isTrue();
              assertThat(GenerateComponentAction.getValidSpec(mock).get().getQualifiedName())
                  .isEqualTo("LayoutSpec");
            });
  }

  @Test
  public void getValidSpec_whenFileHasMountSpec_returnsSpec() throws IOException {
    final PsiFile psiFile = testHelper.configure("MountSpec.java");
    final AnActionEvent mock = mockEvent(psiFile);
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              assertThat(GenerateComponentAction.getValidSpec(mock).isPresent()).isTrue();
              assertThat(GenerateComponentAction.getValidSpec(mock).get().getQualifiedName())
                  .isEqualTo("MountSpec");
            });
  }

  @Test
  public void getValidSpec_whenFileHasSectionSpec_returnsSpec() throws IOException {
    final PsiFile psiFile = testHelper.configure("SectionSpec.java");
    final AnActionEvent mock = mockEvent(psiFile);
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              assertThat(GenerateComponentAction.getValidSpec(mock).isPresent()).isTrue();
              assertThat(GenerateComponentAction.getValidSpec(mock).get().getQualifiedName())
                  .isEqualTo("SectionSpec");
            });
  }

  @Test
  public void getValidSpec_whenFileHasNoSpecs_returnsNothing() throws IOException {
    final PsiFile psiFile = testHelper.configure("NotSpec.java");
    final AnActionEvent mock = mockEvent(psiFile);
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              assertThat(GenerateComponentAction.getValidSpec(mock).isPresent()).isFalse();
            });
  }

  private static AnActionEvent mockEvent(PsiFile psiFile) {
    AnActionEvent mock = mock(AnActionEvent.class);
    when(mock.getData(CommonDataKeys.PSI_FILE)).thenReturn(psiFile);
    return mock;
  }
}
