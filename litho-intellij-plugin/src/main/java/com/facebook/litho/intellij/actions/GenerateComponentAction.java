/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.intellij.actions;

import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.intellij.completion.ComponentGenerateUtils;
import com.facebook.litho.specmodels.processor.PsiAnnotationProxyUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Updates Component file for the given Spec file. Update implementation is defined by the {@link
 * ComponentGenerateUtils}. Currently works with the {@link LayoutSpec} only.
 */
public class GenerateComponentAction extends AnAction {

  @Override
  public void update(AnActionEvent e) {
    super.update(e);
    Optional<PsiClass> component =
        Optional.of(e)
            .map(event -> event.getData(CommonDataKeys.PSI_FILE))
            .map(currentFile -> PsiTreeUtil.getChildrenOfType(currentFile, PsiClass.class))
            .flatMap(this::getFirstLayoutSpec)
            .flatMap(
                specCls ->
                    ComponentGenerateUtils.findComponentFile(
                        specCls.getQualifiedName(), specCls.getProject()))
            .map(componentFile -> PsiTreeUtil.getChildOfType(componentFile, PsiClass.class));
    e.getPresentation().setEnabledAndVisible(component.isPresent());
    component.ifPresent(
        cls -> e.getPresentation().setText("Regenerate " + cls.getName() + " Component"));
  }

  Optional<PsiClass> getFirstLayoutSpec(PsiClass[] currentClasses) {
    return Arrays.stream(currentClasses)
        .filter(Objects::nonNull)
        .filter(
            psiClass ->
                PsiAnnotationProxyUtils.findAnnotationInHierarchy(psiClass, LayoutSpec.class)
                    != null)
        .findFirst();
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
    ComponentGenerateUtils.updateLayoutComponent(file);
  }
}
