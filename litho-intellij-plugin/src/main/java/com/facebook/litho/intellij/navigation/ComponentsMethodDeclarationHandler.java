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

package com.facebook.litho.intellij.navigation;

import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandlerBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

/** Navigates from Component method to Spec method(s) with the same name. */
public class ComponentsMethodDeclarationHandler extends GotoDeclarationHandlerBase {
  @Nullable
  @Override
  public PsiElement getGotoDeclarationTarget(@Nullable PsiElement sourceElement, Editor editor) {
    final PsiElement[] targets = getGotoDeclarationTargets(sourceElement, 0, editor);
    if (targets.length == 0) return null;

    return targets[0];
  }

  @Override
  public PsiElement[] getGotoDeclarationTargets(
      @Nullable PsiElement sourceElement, int offset, Editor editor) {
    // Exclusions
    if (sourceElement == null
        || PsiTreeUtil.getParentOfType(sourceElement, PsiImportStatement.class) != null) {
      return PsiElement.EMPTY_ARRAY;
    }
    final Project project = sourceElement.getProject();
    return BaseLithoComponentsDeclarationHandler.resolve(sourceElement)
        // Filter Methods
        .filter(PsiMethod.class::isInstance)
        .map(PsiMethod.class::cast)
        .map(method -> findSpecMethods(method, project))
        .findFirst()
        .map(
            result -> {
              LithoLoggerProvider.getEventLogger()
                  .log(EventLogger.EVENT_GOTO_NAVIGATION + ".method");
              return result;
            })
        .orElse(PsiMethod.EMPTY_ARRAY);
  }

  private static PsiMethod[] findSpecMethods(PsiMethod componentMethod, Project project) {
    if (!componentMethod.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
      return PsiMethod.EMPTY_ARRAY;
    }

    final PsiClass containingCls =
        (PsiClass) PsiTreeUtil.findFirstParent(componentMethod, PsiClass.class::isInstance);
    if (containingCls == null) return PsiMethod.EMPTY_ARRAY;

    if (!LithoPluginUtils.isGeneratedClass(containingCls)) return PsiMethod.EMPTY_ARRAY;

    // For Unit testing we don't care about package
    final String containingClsName =
        ApplicationManager.getApplication().isUnitTestMode()
            ? containingCls.getName()
            : containingCls.getQualifiedName();
    final PsiClass specCls =
        PsiSearchUtils.findClass(
            project, LithoPluginUtils.getLithoComponentSpecNameFromComponent(containingClsName));
    if (specCls == null) return PsiMethod.EMPTY_ARRAY;

    return specCls.findMethodsByName(componentMethod.getName(), true);
  }
}
