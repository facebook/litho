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
import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.facebook.litho.intellij.services.ComponentsCacheService;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vcs.CodeSmellDetector;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    final Project project = e.getProject();
    final VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
    final PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    final Presentation presentation = e.getPresentation();
    if (project == null || virtualFile == null || psiFile == null) {
      presentation.setEnabledAndVisible(false);
      return;
    }
    final Module currentModule = FileIndexFacade.getInstance(project).getModuleForFile(virtualFile);
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
    final PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_RED_SYMBOLS + ".invoke");

    final List<String> specs = resolveRedSymbols(project, virtualFile, psiFile);
    LithoPluginUtils.showInfo(getMessage(virtualFile, specs), project);

    if (!specs.isEmpty()) {
      LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_RED_SYMBOLS + ".success");
    }
  }

  private static List<String> resolveRedSymbols(
      Project project, VirtualFile virtualFile, PsiFile psiFile) {
    final Map<String, List<Integer>> allRedSymbols = collectRedSymbols(virtualFile, project);
    return addToCache(allRedSymbols, virtualFile, psiFile, project);
  }

  @VisibleForTesting
  static Map<String, List<Integer>> collectRedSymbols(VirtualFile virtualFile, Project project) {
    Map<String, List<Integer>> redSymbols = new HashMap<>();
    CodeSmellDetector.getInstance(project)
        .findCodeSmells(Collections.singletonList(virtualFile))
        .forEach(
            error -> {
              final TextRange textRange = error.getTextRange();
              final String redSymbol = error.getDocument().getText(textRange);
              redSymbols.putIfAbsent(redSymbol, new ArrayList<>());
              redSymbols.get(redSymbol).add(textRange.getStartOffset());
            });
    return redSymbols;
  }

  private static List<String> addToCache(
      Map<String, List<Integer>> allRedSymbols,
      VirtualFile virtualFile,
      PsiFile psiFile,
      Project project) {
    final Module currentModule = FileIndexFacade.getInstance(project).getModuleForFile(virtualFile);
    final GlobalSearchScope symbolsScope =
        GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(currentModule);
    return addToCache(allRedSymbols, psiFile, project, symbolsScope);
  }

  @VisibleForTesting
  static List<String> addToCache(
      Map<String, List<Integer>> allRedSymbols,
      PsiFile psiFile,
      Project project,
      GlobalSearchScope symbolsScope) {
    return allRedSymbols.keySet().stream()
        .flatMap(
            symbol ->
                Arrays.stream(
                        PsiSearchUtils.findClassesByShortName(
                            project, symbolsScope, symbol + "Spec"))
                    .filter(LithoPluginUtils::isLayoutSpec)
                    .map(
                        specCls -> {
                          ServiceManager.getService(project, ComponentsCacheService.class)
                              .maybeUpdate(
                                  specCls,
                                  true,
                                  updatedCls ->
                                      bindExpressions(
                                          allRedSymbols.get(symbol), updatedCls, psiFile, project));
                          return specCls.getName();
                        })
                    .filter(Objects::nonNull))
        .collect(Collectors.toList());
  }

  private static void bindExpressions(
      List<Integer> symbolOffsets, PsiClass symbolCls, PsiFile containingFile, Project project) {
    symbolOffsets.stream()
        .map(
            offset ->
                PsiTreeUtil.findElementOfClassAtOffset(
                    containingFile, offset, PsiJavaCodeReferenceElement.class, false))
        .filter(Objects::nonNull)
        .forEach(
            expression -> {
              WriteCommandAction.runWriteCommandAction(
                  project,
                  () -> {
                    expression.bindToElement(symbolCls);
                  });
            });
  }

  private static String getMessage(VirtualFile virtualFile, List<String> specs) {
    return specs.size()
        + " red symbol(s) found in "
        + virtualFile.getNameWithoutExtension()
        + " "
        + (specs.isEmpty() ? "" : specs.toString());
  }
}
