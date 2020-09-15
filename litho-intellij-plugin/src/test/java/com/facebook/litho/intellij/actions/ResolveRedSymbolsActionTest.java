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
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.impl.MarkupModelImpl;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiTreeUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class ResolveRedSymbolsActionTest extends LithoPluginIntellijTest {

  public ResolveRedSymbolsActionTest() {
    super("testdata/actions");
  }

  @Ignore("T73932936")
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
              // Search and highlights in test env are not populated, do it manually
              PsiSearchUtils.addMock(
                  "LayoutSpec", PsiTreeUtil.findChildOfType(specPsiFile, PsiClass.class));
              parseDocument(editor.getDocument(), pf, project);

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
                  ComponentsCacheService.getInstance(project).getComponent("Layout");
              assertThat(cached).isNotNull();
              assertThat(cached.getName()).isEqualTo("Layout");
            });
  }

  /** In the test env MarkupModel isn't filled */
  public static void parseDocument(Document document, PsiFile psiFile, Project project) {
    final List<HighlightInfo> infos = new ArrayList<>();
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "") {
              @Override
              public void run(ProgressIndicator indicator) {
                final ProgressIndicator daemonIndicator = new DaemonProgressIndicator();
                ProgressManager.getInstance()
                    .runProcess(
                        () -> {
                          ReadAction.run(
                              () ->
                                  infos.addAll(
                                      ((DaemonCodeAnalyzerImpl)
                                              DaemonCodeAnalyzer.getInstance(project))
                                          .runMainPasses(psiFile, document, daemonIndicator)));
                        },
                        daemonIndicator);
              }
            });

    final MarkupModel markup = DocumentMarkupModel.forDocument(document, project, false);
    infos.forEach(
        info -> {
          info.setHighlighter(Mockito.mock(RangeHighlighterEx.class));
          ((MarkupModelImpl) markup)
              .addRangeHighlighterAndChangeAttributes(
                  info.startOffset,
                  info.endOffset,
                  HighlighterLayer.ERROR,
                  info.getTextAttributes(null, null),
                  HighlighterTargetArea.LINES_IN_RANGE,
                  false,
                  highlighter -> highlighter.setErrorStripeTooltip(info));
        });
    markup.getAllHighlighters();
  }
}
