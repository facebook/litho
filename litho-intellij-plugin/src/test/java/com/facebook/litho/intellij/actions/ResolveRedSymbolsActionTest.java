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

package com.facebook.litho.intellij.actions;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.intellij.services.ComponentsCacheService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiTreeUtil;
import java.io.IOException;
import java.util.HashMap;
import org.junit.Test;

public class ResolveRedSymbolsActionTest extends LithoPluginIntellijTest {

  public ResolveRedSymbolsActionTest() {
    super("testdata/actions");
  }

  @Test
  public void resolveRedSymbols() throws IOException {
    final Project project = testHelper.getFixture().getProject();
    final PsiJavaFile pf = (PsiJavaFile) testHelper.configure("ResolveRedSymbolsActionTest.java");
    final VirtualFile vf = pf.getViewProvider().getVirtualFile();
    final Editor editor = testHelper.getFixture().getEditor();

    final PsiFile specPsiFile = testHelper.configure("LayoutSpec.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              PsiSearchUtils.addMock(
                  "LayoutSpec", PsiTreeUtil.findChildOfType(specPsiFile, PsiClass.class));

              final HashMap<String, String> eventMetadata = new HashMap<>();
              ResolveRedSymbolsAction.resolveRedSymbols(
                  pf,
                  vf,
                  editor,
                  project,
                  eventMetadata,
                  ignore -> {
                    assertThat(eventMetadata).isNotEmpty();
                  });
              assertThat(eventMetadata.get("resolved_red_symbols")).isEqualTo("[Layout]");

              final PsiClass cached =
                  ServiceManager.getService(project, ComponentsCacheService.class)
                      .getComponent("Layout");
              assertThat(cached).isNotNull();
              assertThat(cached.getName()).isEqualTo("Layout");
            });
  }
}
