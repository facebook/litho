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

package com.facebook.litho.intellij.actions.templates;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import java.io.IOException;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class LithoTemplateActionTest extends LithoPluginIntellijTest {

  public LithoTemplateActionTest() {
    super("testdata/actions");
  }

  @Ignore("T73932936")
  @Test
  public void createJavaFile_noDoubleSuffix() throws IOException {
    final PsiFile specCls = testHelper.configure("LayoutSpec.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final TestTemplateAction templateAction = new TestTemplateAction();
              final String nameWithProvidedSuffix = "AnySectionSpec";
              templateAction.buildDialog(
                  testHelper.getFixture().getProject(),
                  null,
                  Mockito.mock(CreateFileFromTemplateDialog.Builder.class));
              final PsiFile created =
                  templateAction.createFile(
                      nameWithProvidedSuffix,
                      "GroupSectionSpec.java",
                      specCls.getContainingDirectory());
              assertThat(created.getName()).isEqualTo("AnySectionSpec.java");
            });
  }

  @Ignore("T73932936")
  @Test
  public void createJavaFile_addedSuffix() throws IOException {
    final PsiFile specCls = testHelper.configure("LayoutSpec.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final TestTemplateAction templateAction = new TestTemplateAction();
              final String nameWithoutSuffix = "Any";
              templateAction.buildDialog(
                  testHelper.getFixture().getProject(),
                  null,
                  Mockito.mock(CreateFileFromTemplateDialog.Builder.class));
              final PsiFile created =
                  templateAction.createFile(
                      nameWithoutSuffix, "GroupSectionSpec.java", specCls.getContainingDirectory());
              assertThat(created.getName()).isEqualTo("AnySectionSpec.java");
            });
  }

  @Ignore("T73932936")
  @Test
  public void createKotlinFile_noDoubleSuffix() throws IOException {
    final PsiFile specCls = testHelper.configure("LayoutSpec.kt");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final TestTemplateAction templateAction = new TestTemplateAction();
              final String nameWithProvidedSuffix = "AnySectionSpec";
              templateAction.buildDialog(
                  testHelper.getFixture().getProject(),
                  null,
                  Mockito.mock(CreateFileFromTemplateDialog.Builder.class));
              final PsiFile created =
                  templateAction.createFile(
                      nameWithProvidedSuffix,
                      "GroupSectionSpec.kt",
                      specCls.getContainingDirectory());
              assertThat(created.getName()).isEqualTo("AnySectionSpec.kt");
            });
  }

  @Ignore("T73932936")
  @Test
  public void createKotlinFile_addedSuffix() throws IOException {
    final PsiFile specCls = testHelper.configure("LayoutSpec.kt");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final TestTemplateAction templateAction = new TestTemplateAction();
              final String nameWithoutSuffix = "Any";
              templateAction.buildDialog(
                  testHelper.getFixture().getProject(),
                  null,
                  Mockito.mock(CreateFileFromTemplateDialog.Builder.class));
              final PsiFile created =
                  templateAction.createFile(
                      nameWithoutSuffix, "GroupSectionSpec.kt", specCls.getContainingDirectory());
              assertThat(created.getName()).isEqualTo("AnySectionSpec.kt");
            });
  }

  static class TestTemplateAction extends LithoTemplateAction {
    TestTemplateAction() {
      super("GroupSectionSpec", "SectionSpec");
    }
  }
}
