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

import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.intellij.services.ComponentGenerateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/** Utility class with methods to create, update, and cache files. */
class FileGenerateUtils {

  /** Alternative method to {@link RedSymbolsResolver} for invoking component generation. */
  @Nullable
  static PsiClass generateClass(PsiClass specCls) {
    final Pair<String, String> newComponent =
        ComponentGenerateService.getInstance().createLithoFileContent(specCls);
    if (newComponent == null) return null;

    return updateClass(newComponent.first, newComponent.second, specCls.getProject());
  }

  /** Updates generated Component file with new content. */
  @Nullable
  static PsiClass updateClass(String classFQN, String newContent, Project project) {
    final Optional<PsiClass> generatedClass =
        Optional.ofNullable(PsiSearchUtils.findOriginalClass(project, classFQN))
            .filter(cls -> !ComponentScope.contains(cls.getContainingFile()));

    if (generatedClass.isPresent()) {
      return updateExistingClass(newContent, generatedClass.get(), project);
    } else {
      return updateInMemoryClass(newContent, classFQN, project);
    }
  }

  @Nullable
  private static PsiClass updateExistingClass(
      String newContent, PsiClass generatedClass, Project project) {
    // Null is not expected scenario
    final Document document =
        PsiDocumentManager.getInstance(project).getDocument(generatedClass.getContainingFile());
    if (newContent.equals(document.getText())) {
      return generatedClass;
    }

    // Write access is allowed inside write-action only and on EDT
    if (ApplicationManager.getApplication().isDispatchThread()) {
      updateDocument(newContent, document);
    } else {
      ApplicationManager.getApplication().invokeLater(() -> updateDocument(newContent, document));
    }
    // Currently, we don't need reference to the pre-existing PsiClass.
    return null;
  }

  private static void updateDocument(String newContent, Document document) {
    WriteAction.run(() -> document.setText(newContent));
    FileDocumentManager.getInstance().saveDocument(document);
  }

  @Nullable
  private static PsiClass updateInMemoryClass(String newContent, String classFQN, Project project) {
    final String classShortName = StringUtil.getShortName(classFQN);
    if (classShortName.isEmpty()) return null;

    final ComponentsCacheService cacheService = ComponentsCacheService.getInstance(project);
    final PsiClass oldComponent = cacheService.getComponent(classFQN);
    if (oldComponent != null && newContent.equals(oldComponent.getContainingFile().getText())) {
      return oldComponent;
    }

    final PsiFile newFile =
        PsiFileFactory.getInstance(project)
            .createFileFromText(classShortName + ".java", StdFileTypes.JAVA, newContent);
    ComponentScope.include(newFile);
    final PsiClass inMemory =
        LithoPluginUtils.getFirstClass(newFile, cls -> classShortName.equals(cls.getName()))
            .orElse(null);
    if (inMemory == null) return null;

    cacheService.update(classFQN, inMemory);
    return inMemory;
  }
}
