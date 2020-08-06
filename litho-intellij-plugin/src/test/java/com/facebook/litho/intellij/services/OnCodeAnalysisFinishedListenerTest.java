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

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.intellij.settings.AppSettingsState;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Test;

public class OnCodeAnalysisFinishedListenerTest extends LithoPluginIntellijTest {

  public OnCodeAnalysisFinishedListenerTest() {
    super("testdata/actions");
  }

  @Test
  public void daemonFinished_settingsTrue_resolved() throws IOException {
    final PsiFile specPsiFile = testHelper.configure("LayoutSpec.java");
    testHelper.configure("ResolveRedSymbolsActionTest.java");
    final Project project = testHelper.getFixture().getProject();
    AppSettingsState.getInstance(project).getState().resolveRedSymbols = true;
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              PsiSearchUtils.addMock(
                  "LayoutSpec", PsiTreeUtil.findChildOfType(specPsiFile, PsiClass.class));
              new OnCodeAnalysisFinishedListener(project).daemonFinished();
            });
    final PsiClass cached = ComponentsCacheService.getInstance(project).getComponent("Layout");
    assertThat(cached)
        .describedAs(
            "Not present in cache %s",
            Arrays.toString(ComponentsCacheService.getInstance(project).getAllComponents()))
        .isNotNull();
  }

  @Test
  public void daemonFinished_settingsFalse_notResolved() throws IOException {
    final PsiFile specPsiFile = testHelper.configure("LayoutSpec.java");
    testHelper.configure("ResolveRedSymbolsActionTest.java");
    final Project project = testHelper.getFixture().getProject();
    AppSettingsState.getInstance(project).getState().resolveRedSymbols = false;
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              PsiSearchUtils.addMock(
                  "LayoutSpec", PsiTreeUtil.findChildOfType(specPsiFile, PsiClass.class));
              new OnCodeAnalysisFinishedListener(project).daemonFinished();
            });
    final PsiClass cached = ComponentsCacheService.getInstance(project).getComponent("Layout");
    assertThat(cached).isNull();
  }
}
