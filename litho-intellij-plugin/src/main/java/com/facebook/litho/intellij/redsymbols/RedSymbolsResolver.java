/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.services.LithoGeneratedFileProvider;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx;
import com.intellij.codeInsight.daemon.impl.actions.AddImportAction;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.FileContentUtilCore;
import com.intellij.util.concurrency.AppExecutorUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

class RedSymbolsResolver {
  private static final Logger LOG = Logger.getInstance(RedSymbolsResolver.class);

  /**
   * Searches red symbols in the given file and tries generating missing classes.
   *
   * @param psiFile file to search red symbols. Should be same as virtualFile.
   * @param virtualFile file to search red symbols. Should be same as psiFile.
   * @param editor editor containing document.
   * @param project project to resolve red symbols.
   * @param eventMetadata mutable map to store event data.
   * @param onFinished accepts true, iff red symbols were found and bind, false otherwise. May be
   *     called before symbols binding.
   */
  static void resolveRedSymbols(
      PsiJavaFile psiFile,
      VirtualFile virtualFile,
      Editor editor,
      Project project,
      Map<String, String> eventMetadata,
      Consumer<Boolean> onFinished) {
    final Map<PsiClass, List<PsiElement>> resolved = new HashMap<>();
    Runnable action =
        () ->
            resolved.putAll(
                collectAndAddToCache(
                    psiFile, virtualFile, editor.getDocument(), project, eventMetadata));

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      action.run();
      return;
    }
    ReadAction.nonBlocking(action)
        .inSmartMode(project)
        .expireWith(project)
        .expireWhen(editor::isDisposed)
        .finishOnUiThread(
            ModalityState.NON_MODAL,
            aVoid -> {
              if (resolved.isEmpty()) {
                onFinished.accept(false);
                return;
              }
              long bindStart = System.currentTimeMillis();
              if (!bindExpressions(resolved, virtualFile, editor, project)) {
                onFinished.accept(false);
                return;
              }
              final long bindDelta = System.currentTimeMillis() - bindStart;
              eventMetadata.put(EventLogger.KEY_TIME_BIND_RED_SYMBOLS, String.valueOf(bindDelta));
              onFinished.accept(true);
            })
        .submit(AppExecutorUtil.getAppExecutorService());
  }

  private static Map<PsiClass, List<PsiElement>> collectAndAddToCache(
      PsiJavaFile psiFile,
      VirtualFile virtualFile,
      Document document,
      Project project,
      Map<String, String> eventMetadata) {
    long startTime = System.currentTimeMillis();
    eventMetadata.put(EventLogger.KEY_FILE, psiFile.getPackageName() + "." + psiFile.getName());

    final Map<String, List<PsiElement>> redSymbolToExpressions =
        collectRedSymbols(psiFile, document, project);

    final long collectedTime = System.currentTimeMillis();
    final long collectDelta = collectedTime - startTime;
    eventMetadata.put(EventLogger.KEY_TIME_COLLECT_RED_SYMBOLS, String.valueOf(collectDelta));
    eventMetadata.put(EventLogger.KEY_RED_SYMBOLS_ALL, redSymbolToExpressions.keySet().toString());
    LOG.debug("Collected in " + collectDelta + ", " + redSymbolToExpressions.keySet());
    final GlobalSearchScope symbolsScope =
        moduleWithDependenciesAndLibrariesScope(virtualFile, project);

    final Map<String, PsiClass> redSymbolToCls =
        addToCache(redSymbolToExpressions.keySet(), project, symbolsScope, eventMetadata);

    final long updatedTime = System.currentTimeMillis();
    final long updatedDelta = updatedTime - collectedTime;
    eventMetadata.put(EventLogger.KEY_TIME_RESOLVE_RED_SYMBOLS, String.valueOf(updatedDelta));
    eventMetadata.put(EventLogger.KEY_RED_SYMBOLS_RESOLVED, redSymbolToCls.keySet().toString());
    LOG.debug("Symbols are updated in " + updatedDelta + ", " + redSymbolToCls.keySet());

    if (!redSymbolToCls.isEmpty()) {
      LithoPluginUtils.showInfo(
          getMessage(virtualFile.getNameWithoutExtension(), redSymbolToCls.keySet()), project);
    }
    return combine(redSymbolToCls, redSymbolToExpressions);
  }

  private static GlobalSearchScope moduleWithDependenciesAndLibrariesScope(
      VirtualFile virtualFile, Project project) {
    final Module currentModule = FileIndexFacade.getInstance(project).getModuleForFile(virtualFile);
    if (currentModule == null) {
      return GlobalSearchScope.projectScope(project);
    }
    return GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(currentModule);
  }

  private static Map<String, List<PsiElement>> collectRedSymbols(
      PsiFile psiFile, Document document, Project project) {
    Map<String, List<PsiElement>> redSymbolToElements = new HashMap<>();
    DaemonCodeAnalyzerEx.processHighlights(
        document,
        project,
        HighlightSeverity.ERROR,
        0,
        document.getTextLength(),
        info -> {
          final String redSymbol =
              document.getText(new TextRange(info.startOffset, info.endOffset));
          if (!StringUtil.isJavaIdentifier(redSymbol) || !StringUtil.isCapitalized(redSymbol)) {
            return true;
          }

          final PsiJavaCodeReferenceElement ref =
              PsiTreeUtil.findElementOfClassAtOffset(
                  psiFile, info.startOffset, PsiJavaCodeReferenceElement.class, false);
          if (ref == null) return true;

          // Resolving links in comments is not critical for development
          if (PsiTreeUtil.findFirstParent(ref, element -> element instanceof PsiComment) != null) {
            return true;
          }

          redSymbolToElements.putIfAbsent(redSymbol, new ArrayList<>());
          redSymbolToElements.get(redSymbol).add(ref);
          return true;
        });
    return redSymbolToElements;
  }

  private static Map<String, PsiClass> addToCache(
      Collection<String> allRedSymbols,
      Project project,
      GlobalSearchScope symbolsScope,
      Map<String, String> eventMetadata) {
    final Map<String, PsiClass> redSymbolToClass = new HashMap<>();
    for (String redSymbol : allRedSymbols) {
      LithoGeneratedFileProvider.INSTANCE()
          .guessQualifiedNames(project, symbolsScope, redSymbol, eventMetadata)
          .stream()
          .map(
              fqn -> {
                // Red symbol might exist for present but not-bind class
                final PsiClass existingComponent =
                    PsiSearchUtils.getInstance().findOriginalClass(project, fqn);
                if (existingComponent != null) return existingComponent;

                final String newContent =
                    LithoGeneratedFileProvider.INSTANCE().createFileContent(fqn);
                if (newContent == null) return null;

                return FileGenerateUtils.updateClass(fqn, newContent, project);
              })
          .filter(Objects::nonNull)
          .forEach(component -> redSymbolToClass.put(redSymbol, component));
    }
    LithoGeneratedFileProvider.INSTANCE().clear();
    return redSymbolToClass;
  }

  /**
   * @return if the binding was finished before editor disposed.
   */
  private static boolean bindExpressions(
      Map<PsiClass, List<PsiElement>> resolved,
      VirtualFile virtualFile,
      Editor editor,
      Project project) {
    for (Map.Entry<PsiClass, List<PsiElement>> entry : resolved.entrySet()) {
      final PsiClass targetClass = entry.getKey();
      final List<PsiElement> expressions = entry.getValue();
      LOG.debug("Binding " + targetClass.getName());
      for (PsiElement expression : expressions) {
        if (editor.isDisposed()) return false;

        new AddImportAction(project, (PsiReference) expression, editor, targetClass).execute();
      }
    }
    FileContentUtilCore.reparseFiles(virtualFile);
    return true;
  }

  private static <T, V1, V2> Map<V1, V2> combine(Map<T, V1> map1, Map<T, V2> map2) {
    final Map<V1, V2> result = new HashMap<>();
    map1.entrySet()
        .forEach(
            entry -> {
              final V1 v1 = entry.getValue();
              final V2 v2 = map2.get(entry.getKey());
              result.put(v1, v2);
            });
    return result;
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
