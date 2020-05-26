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
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
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
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    if (project == null || virtualFile == null || !(psiFile instanceof PsiJavaFile)) {
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

    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_RED_SYMBOLS + ".invoke");
    Map<String, String> eventMetadata = new HashMap<>();

    final Collection<String> specs =
        resolveRedSymbols(project, virtualFile, psiFile, eventMetadata);

    LithoPluginUtils.showInfo(getMessage(virtualFile.getNameWithoutExtension(), specs), project);
    String result = specs.isEmpty() ? ".fail" : ".success";
    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_RED_SYMBOLS + result, eventMetadata);
  }

  /**
   * Searches red symbols in the given file and tries generating missing classes.
   *
   * @param project project to resolve red symbols.
   * @param virtualFile file to search red symbols. Should be same as psiFile.
   * @param psiFile file to search red symbols. Should be same as virtualFile.
   * @param eventMetadata mutable map to store event data.
   * @return resolved red symbols.
   */
  public static Collection<String> resolveRedSymbols(
      Project project,
      VirtualFile virtualFile,
      PsiJavaFile psiFile,
      Map<String, String> eventMetadata) {
    long startTime = System.currentTimeMillis();
    eventMetadata.put(EventLogger.KEY_FILE, psiFile.getPackageName() + "." + psiFile.getName());
    final Map<String, List<Integer>> allRedSymbols = collectRedSymbols(virtualFile, project);
    final long collectedTime = System.currentTimeMillis();
    eventMetadata.put(
        EventLogger.KEY_TIME_COLLECT_RED_SYMBOLS, String.valueOf(collectedTime - startTime));
    eventMetadata.put(EventLogger.KEY_RED_SYMBOLS_ALL, allRedSymbols.keySet().toString());
    final GlobalSearchScope symbolsScope =
        moduleWithDependenciesAndLibrariesScope(virtualFile, project);
    final Collection<String> resolved =
        addToCache(allRedSymbols.keySet(), project, symbolsScope).keySet();
    final long endTime = System.currentTimeMillis();
    eventMetadata.put(
        EventLogger.KEY_TIME_RESOLVE_RED_SYMBOLS, String.valueOf(endTime - collectedTime));
    eventMetadata.put(EventLogger.KEY_RED_SYMBOLS_RESOLVED, resolved.toString());
    return resolved;
  }

  private static GlobalSearchScope moduleWithDependenciesAndLibrariesScope(
      VirtualFile virtualFile, Project project) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return GlobalSearchScope.projectScope(project);
    }
    final Module currentModule = FileIndexFacade.getInstance(project).getModuleForFile(virtualFile);
    return GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(currentModule);
  }

  private static Map<String, List<Integer>> collectRedSymbols(
      VirtualFile virtualFile, Project project) {
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

  private static Map<String, PsiClass> addToCache(
      Collection<String> allRedSymbols, Project project, GlobalSearchScope symbolsScope) {
    Map<String, PsiClass> redSymbolToClass = new HashMap<>();
    ComponentsCacheService componentsCache =
        ServiceManager.getService(project, ComponentsCacheService.class);
    for (String redSymbol : allRedSymbols) {
      Arrays.stream(
              PsiSearchUtils.findClassesByShortName(project, symbolsScope, redSymbol + "Spec"))
          .filter(LithoPluginUtils::isLayoutSpec)
          .forEach(
              specCls -> {
                final PsiClass resolved = componentsCache.maybeUpdate(specCls, false);
                redSymbolToClass.put(redSymbol, resolved);
              });
    }
    return redSymbolToClass;
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

  private static String getMessage(
      String containingFileName, Collection<String> foundRedSymbolNames) {
    return foundRedSymbolNames.size()
        + " red symbol(s) resolved in "
        + containingFileName
        + " "
        + (foundRedSymbolNames.isEmpty() ? "" : foundRedSymbolNames.toString());
  }
}
