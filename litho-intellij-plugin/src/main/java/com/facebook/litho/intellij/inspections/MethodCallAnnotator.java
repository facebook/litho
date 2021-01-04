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

package com.facebook.litho.intellij.inspections;

import static com.facebook.litho.intellij.LithoPluginUtils.resolveEventName;

import com.facebook.litho.annotations.PropSetter;
import com.facebook.litho.intellij.IntervalLogger;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.intellij.services.ComponentGenerateService;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.facebook.litho.specmodels.processor.PsiAnnotationProxyUtils;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Annotator creates quick fix to add existing EventHandler to the method without arguments. */
public class MethodCallAnnotator implements Annotator {
  private static final IntervalLogger DEBUG_LOGGER =
      new IntervalLogger(Logger.getInstance(MethodCallAnnotator.class));
  static final String FIX_FAMILY_NAME = "LithoFix";

  @Override
  public void annotate(PsiElement element, AnnotationHolder holder) {
    DEBUG_LOGGER.logStep("start " + element);
    // Implementation similar to {@link HighlightMethodUtil#checkMethodCall}
    if (!(element instanceof PsiMethodCallExpression)) {
      return;
    }

    // MethodCall == method usage
    final PsiMethodCallExpression methodCall = (PsiMethodCallExpression) element;

    formatRequiredProp(methodCall, holder);

    addEventHandlerFix(holder, methodCall);

    DEBUG_LOGGER.logStep("end " + element);
  }

  private static void addEventHandlerFix(
      AnnotationHolder holder, PsiMethodCallExpression methodCall) {
    final PsiExpressionList list = methodCall.getArgumentList();
    if (!list.isEmpty()) {
      return;
    }
    final String eventQualifiedName = resolveEventName(methodCall);
    if (eventQualifiedName == null) {
      return;
    }
    final PsiClass parentCls =
        (PsiClass) PsiTreeUtil.findFirstParent(methodCall, PsiClass.class::isInstance);
    if (parentCls == null) {
      return;
    }
    final SpecModel parentModel = ComponentGenerateService.getInstance().getSpecModel(parentCls);
    if (parentModel == null) {
      return;
    }
    final ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>>
        implementedEventHandlers = parentModel.getEventMethods();
    final String componentQualifiedName = parentModel.getComponentTypeName().toString();
    final Project project = methodCall.getProject();
    final PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
    final String message = "Add " + AddArgumentFix.getCapitalizedMethoName(methodCall);
    final SpecModelValidationError error = new SpecModelValidationError(list, message);

    final List<IntentionAction> fixes =
        implementedEventHandlers.stream()
            .filter(handler -> eventQualifiedName.equals(handler.typeModel.name.reflectionName()))
            .map(handler -> handler.name.toString())
            .distinct()
            .map(
                methodName ->
                    AddArgumentFix.createAddMethodCallFix(
                        methodCall, componentQualifiedName, methodName, elementFactory))
            .collect(Collectors.toList());

    final PsiClass event = PsiSearchUtils.findClass(project, eventQualifiedName);
    if (event != null) {
      fixes.add(
          AddArgumentFix.createNewMethodCallFix(
              methodCall, componentQualifiedName, event, parentCls));
    }
    AnnotatorUtils.addError(holder, error, fixes);
  }

  private static void formatRequiredProp(
      PsiMethodCallExpression expression, AnnotationHolder holder) {
    Optional.ofNullable(expression.resolveMethod())
        .map(method -> PsiAnnotationProxyUtils.findAnnotationInHierarchy(method, PropSetter.class))
        .filter(PropSetter::required)
        .map(ignore -> expression.getMethodExpression().getReferenceNameElement())
        .ifPresent(
            element ->
                holder
                    .createInfoAnnotation(element, null)
                    .setTextAttributes(DefaultLanguageHighlighterColors.KEYWORD));
  }
}
