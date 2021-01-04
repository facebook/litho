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

package com.facebook.litho.intellij.completion;

import com.facebook.litho.intellij.services.ComponentGenerateService;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.StateParamModel;
import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.codeInsight.generation.GenerateMembersHandlerBase;
import com.intellij.codeInsight.generation.GenerationInfo;
import com.intellij.codeInsight.generation.TemplateGenerationInfo;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateBuilderImpl;
import com.intellij.codeInsight.template.impl.TextExpression;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Generates method. Doesn't prompt the user for additional data, uses pre-defined method instead.
 */
class MethodGenerateHandler extends GenerateMembersHandlerBase {
  private final List<PsiMethod> generatedMethods;
  private final PsiClass specClass;
  private final Document document;
  private final Project project;

  MethodGenerateHandler(
      List<PsiMethod> methods, PsiClass specClass, Document document, Project project) {
    super("");
    generatedMethods = methods;
    this.specClass = specClass;
    this.document = document;
    this.project = project;
  }

  @NotNull
  @Override
  protected List<? extends GenerationInfo> generateMemberPrototypes(
      PsiClass aClass, ClassMember[] members) throws IncorrectOperationException {
    return generatedMethods.stream()
        .map(
            generatedMethod ->
                new MethodTemplateGenerationInfo(generatedMethod, specClass, document, project))
        .collect(Collectors.toList());
  }

  @Override
  protected ClassMember[] chooseOriginalMembers(PsiClass aClass, Project project, Editor editor) {
    return ClassMember.EMPTY_ARRAY;
  }

  @Override
  protected ClassMember[] getAllOriginalMembers(PsiClass aClass) {
    return ClassMember.EMPTY_ARRAY;
  }

  @Override
  protected GenerationInfo[] generateMemberPrototypes(PsiClass aClass, ClassMember originalMember)
      throws IncorrectOperationException {
    return GenerationInfo.EMPTY_ARRAY;
  }

  static class MethodTemplateGenerationInfo extends TemplateGenerationInfo {
    private final PsiClass specClass;
    private final Document document;
    private final Project project;

    public MethodTemplateGenerationInfo(
        PsiMethod element, PsiClass specClass, Document document, Project project) {
      super(element, null);
      this.specClass = specClass;
      this.document = document;
      this.project = project;
    }

    @Override
    protected PsiElement getTemplateElement(PsiMethod method) {
      return null;
    }

    @Override
    public Template getTemplate() {
      PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document);

      final PsiMethod psiMethodWithContext = Objects.requireNonNull(getPsiMember());
      final TemplateBuilderImpl templateBuilder = new TemplateBuilderImpl(psiMethodWithContext);
      PsiTreeUtil.processElements(
          psiMethodWithContext,
          psiElement -> {
            if (psiElement instanceof PsiTypeElement
                && psiElement.getText().equals("placeholder_type")) {
              templateBuilder.replaceElement(
                  psiElement, new TextExpression(getFirstStateTypeAndName(specClass).first));
            } else if (psiElement instanceof PsiIdentifier
                && psiElement.getText().equals("placeholder_name")) {
              templateBuilder.replaceElement(
                  psiElement, new TextExpression(getFirstStateTypeAndName(specClass).second));
            } else if (psiElement instanceof PsiIdentifier
                && psiElement.getParent() instanceof PsiMethod) {
              // Method name
              templateBuilder.replaceElement(psiElement, new TextExpression(psiElement.getText()));
            } else if (psiElement instanceof PsiComment) {
              templateBuilder.replaceElement(psiElement, new TextExpression(psiElement.getText()));
              templateBuilder.setEndVariableAfter(psiElement);
            }
            return true;
          });
      final Template template = templateBuilder.buildTemplate();
      template.setToReformat(true);
      return template;
    }
  }

  private static Pair<String, String> getFirstStateTypeAndName(PsiClass specClass) {
    final SpecModel specModel =
        ComponentGenerateService.getInstance().getOrCreateSpecModel(specClass);
    if (specModel != null && !specModel.getStateValues().isEmpty()) {
      final StateParamModel stateParamModel = specModel.getStateValues().get(0);
      return new Pair<>(stateParamModel.getTypeName().toString(), stateParamModel.getName());
    }
    return new Pair<>("StateType", "stateName");
  }
}
