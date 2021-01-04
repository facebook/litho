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

import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import java.util.HashMap;
import java.util.Map;

/**
 * Finds errors in the current file, tries to resolve them to Litho Specs, and updates generated
 * components.
 */
class ResolveRedSymbolsAction extends AnAction {

  @Override
  public void update(AnActionEvent e) {
    super.update(e);
    final Project project = e.getProject();
    final VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
    final PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    final Editor editor = e.getData(CommonDataKeys.EDITOR);
    final Presentation presentation = e.getPresentation();
    if (project == null
        || virtualFile == null
        || editor == null
        || !(psiFile instanceof PsiJavaFile)) {
      presentation.setEnabledAndVisible(false);
      return;
    }
    final Module currentModule = FileIndexFacade.getInstance(project).getModuleForFile(virtualFile);
    if (currentModule == null) {
      presentation.setEnabledAndVisible(false);
      return;
    }
    presentation.setEnabledAndVisible(true);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    // Verified nonNull in #update
    final Project project = e.getProject();
    final VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
    final PsiJavaFile psiFile = (PsiJavaFile) e.getData(CommonDataKeys.PSI_FILE);
    final Editor editor = e.getData(CommonDataKeys.EDITOR);

    Map<String, String> eventMetadata = new HashMap<>();
    RedSymbolsResolver.resolveRedSymbols(
        psiFile,
        virtualFile,
        editor,
        project,
        eventMetadata,
        finished -> {
          eventMetadata.put(EventLogger.KEY_TYPE, "action");
          eventMetadata.put(EventLogger.KEY_RESULT, finished ? "success" : "fail");
          LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_RED_SYMBOLS, eventMetadata);
        });
  }
}
