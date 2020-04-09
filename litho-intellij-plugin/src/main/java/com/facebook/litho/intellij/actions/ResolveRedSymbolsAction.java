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

import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.facebook.litho.intellij.services.ComponentsCacheService;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.vcs.CodeSmellDetector;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Finds errors in the current file, tries to resolve them to Litho Specs, and updates generated
 * components.
 */
public class ResolveRedSymbolsAction extends AnAction {

  @Override
  public void update(AnActionEvent e) {
    super.update(e);
    final VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    final Presentation presentation = e.getPresentation();
    if (file == null) {
      presentation.setEnabledAndVisible(false);
      return;
    }
    final Project project = e.getProject();
    if (project == null) {
      presentation.setEnabledAndVisible(false);
      return;
    }
    final Module currentModule = FileIndexFacade.getInstance(project).getModuleForFile(file);
    if (currentModule == null) {
      presentation.setEnabledAndVisible(false);
      return;
    }
    presentation.setText("Resolve Litho red symbols in current file");
    presentation.setEnabledAndVisible(true);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    // Verified nonNull in #update
    final Project project = e.getProject();
    final VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_RED_SYMBOLS + ".invoke");

    final Collection<String> allRedSymbols = collectRedSymbols(virtualFile, project);
    final List<String> specs = addToCache(allRedSymbols, virtualFile, project);
    LithoPluginUtils.showInfo(getMessage(virtualFile, specs), project);

    if (!specs.isEmpty()) {
      LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_RED_SYMBOLS + ".success");
    }
  }

  @VisibleForTesting
  static Collection<String> collectRedSymbols(VirtualFile virtualFile, Project project) {
    return CodeSmellDetector.getInstance(project)
        .findCodeSmells(Collections.singletonList(virtualFile)).stream()
        .map(error -> error.getDocument().getText(error.getTextRange()))
        .collect(Collectors.toSet());
  }

  private static List<String> addToCache(
      Collection<String> allRedSymbols, VirtualFile virtualFile, Project project) {
    final Module currentModule = FileIndexFacade.getInstance(project).getModuleForFile(virtualFile);
    final GlobalSearchScope symbolsScope =
        GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(currentModule);
    return allRedSymbols.stream()
        .flatMap(
            symbol ->
                Arrays.stream(
                        PsiShortNamesCache.getInstance(project)
                            .getClassesByName(symbol + "Spec", symbolsScope))
                    .filter(LithoPluginUtils::isLayoutSpec)
                    .map(
                        specCls -> {
                          ServiceManager.getService(project, ComponentsCacheService.class)
                              .maybeUpdate(specCls, true);
                          return specCls.getName();
                        })
                    .filter(Objects::nonNull))
        .collect(Collectors.toList());
  }

  private static String getMessage(VirtualFile virtualFile, List<String> specs) {
    return specs.size()
        + " red symbol(s) found in "
        + virtualFile.getNameWithoutExtension()
        + " "
        + (specs.isEmpty() ? "" : specs.toString());
  }
}
