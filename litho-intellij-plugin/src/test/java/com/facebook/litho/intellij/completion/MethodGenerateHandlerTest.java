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

package com.facebook.litho.intellij.completion;

import static org.fest.assertions.Assertions.assertThat;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.services.TemplateService;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateBuilderImpl;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import java.io.IOException;
import org.junit.Test;

public class MethodGenerateHandlerTest extends LithoPluginIntellijTest {
  public MethodGenerateHandlerTest() {
    super("testdata/completion");
  }

  @Test
  public void fillTemplate_forOnUpdateState_hasBoxedTypes() throws IOException {
    final Project project = testHelper.getProject();
    PsiFile psiFile = testHelper.configure("MethodGenerateHandlerOnUpdateStateTest.java");
    final PsiClass[] testClass = new PsiClass[2];
    final TemplateBuilderImpl[] templateBuilder = new TemplateBuilderImpl[1];
    final PsiMethod[] templateMethod = new PsiMethod[1];
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              testClass[0] =
                  LithoPluginUtils.getFirstClass(
                          psiFile,
                          cls -> "MethodGenerateHandlerMethodGenerateSpec".equals(cls.getName()))
                      .get();
              testClass[1] =
                  LithoPluginUtils.getFirstClass(psiFile, cls -> "Dummy".equals(cls.getName()))
                      .get();
              final TemplateService templateService =
                  ServiceManager.getService(project, TemplateService.class);
              templateMethod[0] = templateService.getMethodTemplate("OnUpdateState", project);
              CommandProcessor.getInstance()
                  .executeCommand(
                      project,
                      () -> {
                        WriteAction.compute(
                            () ->
                                templateMethod[0] =
                                    (PsiMethod) testClass[1].add(templateMethod[0]));
                        templateBuilder[0] = new TemplateBuilderImpl(templateMethod[0]);
                      },
                      null,
                      null);
              MethodGenerateHandler.fillTemplate(
                  templateMethod[0], templateBuilder[0], testClass[0]);
              final Template template = templateBuilder[0].buildTemplate();
              WriteCommandAction.runWriteCommandAction(
                  project,
                  () ->
                      runTemplates(
                          project,
                          testHelper.getFixture().getEditor(),
                          template,
                          templateMethod[0]));
            });

    assertThat(
            testHelper
                .getFixture()
                .getEditor()
                .getDocument()
                .getText()
                .contains(
                    "onUpdateState(com.facebook.litho.StateValue<java.lang.Boolean> someState)"))
        .isTrue();
  }

  private static void runTemplates(
      final Project myProject, final Editor editor, Template template, PsiElement element) {
    final TextRange range = element.getTextRange();
    WriteAction.run(
        () -> editor.getDocument().deleteString(range.getStartOffset(), range.getEndOffset()));
    TemplateManager.getInstance(myProject).startTemplate(editor, template);
  }
}
