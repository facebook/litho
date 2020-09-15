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

import com.facebook.litho.intellij.IntervalLogger;
import com.facebook.litho.intellij.LithoClassNames;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.intellij.services.ComponentGenerateService;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaResolveResult;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.infos.MethodCandidateInfo;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

/** Annotator creates quick fix to add existing EventHandler to the method without arguments. */
public class EventHandlerAnnotator implements Annotator {
  private static final IntervalLogger DEBUG_LOGGER =
      new IntervalLogger(Logger.getInstance(EventHandlerAnnotator.class));
  static final String FIX_FAMILY_NAME = "LithoFix";

  @Override
  public void annotate(PsiElement element, AnnotationHolder holder) {
    DEBUG_LOGGER.logStep("start " + element);
    // Implementation similar to {@link HighlightMethodUtil#checkMethodCall}
    if (!(element instanceof PsiMethodCallExpression)) {
      return;
    }
    // MethodCall == method usage
    final PsiMethodCallExpression eventHandlerSetter = (PsiMethodCallExpression) element;
    final PsiExpressionList list = eventHandlerSetter.getArgumentList();
    if (!list.isEmpty()) {
      return;
    }
    final String eventQualifiedName = resolveEventName(eventHandlerSetter);
    if (eventQualifiedName == null) {
      return;
    }
    final PsiClass parentCls =
        (PsiClass) PsiTreeUtil.findFirstParent(eventHandlerSetter, PsiClass.class::isInstance);
    if (parentCls == null) {
      return;
    }
    final SpecModel parentModel = ComponentGenerateService.getSpecModel(parentCls);
    if (parentModel == null) {
      return;
    }
    final ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>>
        implementedEventHandlers = parentModel.getEventMethods();
    final String componentQualifiedName = parentModel.getComponentTypeName().toString();
    final Project project = eventHandlerSetter.getProject();
    final PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
    final String message = "Add " + AddArgumentFix.getCapitalizedMethoName(eventHandlerSetter);
    final SpecModelValidationError error = new SpecModelValidationError(list, message);

    final List<IntentionAction> fixes =
        implementedEventHandlers.stream()
            .filter(handler -> eventQualifiedName.equals(handler.typeModel.name.reflectionName()))
            .map(handler -> handler.name.toString())
            .distinct()
            .map(
                methodName ->
                    AddArgumentFix.createAddMethodCallFix(
                        eventHandlerSetter, componentQualifiedName, methodName, elementFactory))
            .collect(Collectors.toList());

    final PsiClass event = PsiSearchUtils.findClass(project, eventQualifiedName);
    if (event != null) {
      fixes.add(
          AddArgumentFix.createNewMethodCallFix(
              eventHandlerSetter, componentQualifiedName, event, parentCls));
    }
    AnnotatorUtils.addError(holder, error, fixes);
    DEBUG_LOGGER.logStep("end " + element);
  }

  /**
   * Tries to guess if the given methodCall requires event handler.
   *
   * @return Qualified name of the handled Event or null, if methodCall neither accepts event
   *     handler, nor require fix.
   */
  @Nullable
  private static String resolveEventName(PsiMethodCallExpression methodCall) {
    return Optional.of(methodCall.getMethodExpression().multiResolve(true))
        .map(results -> results.length == 1 ? results[0] : JavaResolveResult.EMPTY)
        .filter(MethodCandidateInfo.class::isInstance)
        .map(MethodCandidateInfo.class::cast)
        .filter(MethodCandidateInfo::isTypeArgumentsApplicable)
        .filter(info -> !info.isApplicable() && !info.isValidResult())
        .map(info -> info.getElement().getParameterList().getParameters())
        .filter(parameters -> parameters.length > 0) // method(EventHandler<T> e)
        .map(parameters -> parameters[0].getType())
        .filter(PsiClassType.class::isInstance)
        .filter(
            parameterType -> {
              String fullName = parameterType.getCanonicalText();
              int genericIndex = fullName.indexOf('<');
              if (genericIndex <= 0) {
                return false;
              }
              String className = fullName.substring(0, genericIndex);
              return LithoClassNames.EVENT_HANDLER_CLASS_NAME.equals(className);
            })
        .map(parameterType -> ((PsiClassType) parameterType).getParameters())
        .filter(generics -> generics.length == 1) // <T>
        .map(generics -> generics[0].getCanonicalText())
        .orElse(null);
  }
}
