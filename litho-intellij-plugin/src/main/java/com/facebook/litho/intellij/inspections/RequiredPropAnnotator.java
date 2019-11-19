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

import com.facebook.litho.annotations.RequiredProp;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.DebounceEventLogger;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.facebook.litho.specmodels.processor.PsiAnnotationProxyUtils;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

public final class RequiredPropAnnotator implements Annotator {
  private static final EventLogger LOGGER = new DebounceEventLogger(7 * 60_000);
  private final Function<PsiMethodCallExpression, PsiClass> generatedClassResolver;

  public RequiredPropAnnotator() {
    this(
        methodCallExpression -> {
          PsiMethod psiMethod = methodCallExpression.resolveMethod();
          if (psiMethod == null) {
            return null;
          }
          PsiClass topLevelClass = PsiUtil.getTopLevelClass(psiMethod);
          if (LithoPluginUtils.isGeneratedClass(topLevelClass)) {
            return topLevelClass;
          }
          return null;
        });
  }

  /**
   * @param generatedClassResolver resolves input method to generated Litho class and returns it, or
   *     null if there is no such one.
   */
  @VisibleForTesting
  RequiredPropAnnotator(Function<PsiMethodCallExpression, PsiClass> generatedClassResolver) {
    this.generatedClassResolver = generatedClassResolver;
  }

  @Override
  public void annotate(PsiElement element, AnnotationHolder holder) {
    if (element instanceof PsiDeclarationStatement) {
      Arrays.stream(((PsiDeclarationStatement) element).getDeclaredElements())
          .filter(PsiVariable.class::isInstance)
          .map(declaredVariable -> ((PsiVariable) declaredVariable).getInitializer())
          .forEach(expression -> handleIfMethodCall(expression, holder));
    } else if (element instanceof PsiExpressionStatement) {
      handleIfMethodCall(((PsiExpressionStatement) element).getExpression(), holder);
    } else if (element instanceof PsiReturnStatement) {
      handleIfMethodCall(((PsiReturnStatement) element).getReturnValue(), holder);
    }
  }

  private void handleIfMethodCall(@Nullable PsiExpression expression, AnnotationHolder holder) {
    if (expression instanceof PsiMethodCallExpression) {
      PsiMethodCallExpression rootMethodCall = (PsiMethodCallExpression) expression;
      handleMethodCall(rootMethodCall, new HashSet<>(), holder);
    }
  }

  private void handleMethodCall(
      PsiMethodCallExpression currentMethodCall,
      Set<String> methodNamesCalled,
      AnnotationHolder holder) {
    PsiReferenceExpression methodExpression = currentMethodCall.getMethodExpression();
    methodNamesCalled.add(methodExpression.getReferenceName());

    // Assumption to find next method in a call chain
    PsiMethodCallExpression nextMethodCall =
        PsiTreeUtil.getChildOfType(methodExpression, PsiMethodCallExpression.class);
    if (nextMethodCall != null) {
      handleMethodCall(nextMethodCall, methodNamesCalled, holder);
    } else if ("create".equals(methodExpression.getReferenceName())) {
      // Finish call chain
      // TODO T47712852: allow setting required prop in another statement
      Optional.ofNullable(generatedClassResolver.apply(currentMethodCall))
          .map(generatedCls -> collectMissingRequiredProps(generatedCls, methodNamesCalled))
          .filter(result -> !result.isEmpty())
          .ifPresent(
              missingRequiredProps -> {
                LOGGER.log(EventLogger.EVENT_ANNOTATOR + ".required_prop");
                AnnotatorUtils.addError(
                    holder,
                    new SpecModelValidationError(
                        methodExpression,
                        "The following props are not marked as optional and were not supplied: "
                            + StringUtil.join(missingRequiredProps, ", ")));
              });
    }

    PsiExpressionList argumentList = currentMethodCall.getArgumentList();
    for (PsiExpression argument : argumentList.getExpressions()) {
      handleIfMethodCall(argument, holder);
    }
  }

  /**
   * @param generatedCls class containing inner Builder class with methods annotated as {@link
   *     RequiredProp} marking which Props are required for this class.
   * @param methodNames methods from the given class.
   * @return names of the generatedCls required props, that were not set after all methodNames
   *     calls.
   */
  private static Collection<String> collectMissingRequiredProps(
      PsiClass generatedCls, Collection<String> methodNames) {
    Map<String, Set<String>> propToMethods = getRequiredPropsToMethodNames(generatedCls);
    if (propToMethods.isEmpty()) {
      return Collections.emptySet();
    }
    Set<String> missingRequiredProps = new HashSet<>(propToMethods.keySet());
    Map<String, String> methodToProp = inverse(propToMethods);
    for (String methodName : methodNames) {
      if (methodToProp.containsKey(methodName)) {
        String prop = methodToProp.get(methodName);
        missingRequiredProps.remove(prop);
      }
    }
    return missingRequiredProps;
  }

  /**
   * @param generatedCls class containing inner Builder class with methods annotated as {@link
   *     RequiredProp} marking which Props are required for this class.
   * @return mapping from required prop name to the method names setting this prop. Return empty map
   *     if the provided class doesn't contain Builder inner class, or doesn't have methods with
   *     {@link RequiredProp} annotation.
   */
  private static Map<String, Set<String>> getRequiredPropsToMethodNames(PsiClass generatedCls) {
    PsiClass builder = generatedCls.findInnerClassByName("Builder", false);
    if (builder == null) {
      return Collections.emptyMap();
    }
    Map<String, Set<String>> propToMethods = new HashMap<>();
    PsiMethod[] methods = builder.getMethods();
    for (PsiMethod method : methods) {
      RequiredProp requiredProp =
          PsiAnnotationProxyUtils.findAnnotationInHierarchy(method, RequiredProp.class);
      if (requiredProp == null) {
        continue;
      }
      Set<String> methodNames = propToMethods.get(requiredProp.value());
      if (methodNames == null) {
        methodNames = new HashSet<>();
        propToMethods.put(requiredProp.value(), methodNames);
      }
      methodNames.add(method.getName());
    }
    return propToMethods;
  }

  private static <K, V> Map<V, K> inverse(Map<K, ? extends Collection<V>> keyToValue) {
    Map<V, K> inversed = new HashMap<>();
    for (Map.Entry<K, ? extends Collection<V>> entry : keyToValue.entrySet()) {
      for (V value : entry.getValue()) {
        inversed.put(value, entry.getKey());
      }
    }
    return inversed;
  }
}
