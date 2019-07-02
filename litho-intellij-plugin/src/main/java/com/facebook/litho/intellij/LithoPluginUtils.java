/*
 * Copyright 2004-present Facebook, Inc.
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
package com.facebook.litho.intellij;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class LithoPluginUtils {
  private static final String SPEC_SUFFIX = "Spec";

  public static boolean isComponentClass(PsiClass psiClass) {
    return psiClass != null
        && psiClass.getSuperClass() != null
        && ("ComponentLifecycle".equals(psiClass.getSuperClass().getName())
            || "com.facebook.litho.Component".equals(psiClass.getSuperClass().getQualifiedName()));
  }

  public static boolean isSectionClass(@Nullable PsiClass psiClass) {
    return Optional.ofNullable(psiClass)
        .map(PsiClass::getSuperClass)
        .map(PsiClass::getQualifiedName)
        .filter("com.facebook.litho.sections.Section"::equals)
        .isPresent();
  }

  public static boolean isLithoSpec(@Nullable PsiFile psiFile) {
    if (psiFile == null) {
      return false;
    }
    PsiClass psiClass = PsiTreeUtil.findChildOfType(psiFile, PsiClass.class);
    return isLithoSpec(psiClass);
  }

  public static boolean isLithoSpec(@Nullable PsiClass psiClass) {
    return psiClass != null
        && (hasLithoSectionAnnotation(psiClass) || hasLithoAnnotation(psiClass));
  }

  public static boolean hasLithoAnnotation(@Nullable PsiClass psiClass) {
    if (psiClass == null) {
      return false;
    }
    return hasAnnotation(psiClass, startsWith("com.facebook.litho.annotations"));
  }

  @VisibleForTesting
  static boolean hasAnnotation(
      PsiModifierListOwner modifierListOwner, Predicate<String> nameFilter) {
    return Optional.of(modifierListOwner)
        .map(PsiModifierListOwner::getAnnotations)
        .map(
            annotations ->
                Stream.of(annotations)
                    .map(PsiAnnotation::getQualifiedName)
                    .filter(Objects::nonNull)
                    .anyMatch(nameFilter))
        .orElse(false);
  }

  @VisibleForTesting
  static Predicate<String> startsWith(String prefix) {
    return name -> name.startsWith(prefix);
  }

  private static Predicate<String> equals(String text) {
    return name -> name.equals(text);
  }

  public static boolean hasLithoSectionAnnotation(PsiClass psiClass) {
    return hasAnnotation(psiClass, startsWith("com.facebook.litho.sections.annotations"));
  }

  public static boolean isPropOrState(PsiParameter parameter) {
    return isProp(parameter) || isState(parameter);
  }

  public static boolean isProp(PsiParameter parameter) {
    return hasAnnotation(parameter, equals(LithoClassNames.PROP_CLASS_NAME));
  }

  public static boolean isState(PsiParameter parameter) {
    return hasAnnotation(parameter, equals(LithoClassNames.STATE_CLASS_NAME));
  }

  public static boolean isParam(PsiParameter parameter) {
    return hasAnnotation(parameter, equals(LithoClassNames.PARAM_ANNOTATION_NAME));
  }

  public static boolean isPropDefault(PsiField field) {
    return hasAnnotation(field, equals(LithoClassNames.PROP_DEFAULT_CLASS_NAME));
  }

  public static boolean isEvent(PsiClass psiClass) {
    return hasAnnotation(psiClass, equals(LithoClassNames.EVENT_ANNOTATION_NAME));
  }

  @Nullable
  public static String getLithoComponentNameFromSpec(@Nullable String specName) {
    if (specName != null && specName.endsWith(SPEC_SUFFIX)) {
      return specName.substring(0, specName.length() - SPEC_SUFFIX.length());
    }
    return null;
  }

  @Nullable
  @Contract("null -> null; !null -> !null")
  public static String getLithoComponentSpecNameFromComponent(@Nullable String componentName) {
    if (componentName != null) {
      return componentName + SPEC_SUFFIX;
    }
    return null;
  }

  /** @return the Stream of unique parameters from all methods excluding current method. */
  public static Stream<PsiParameter> getPsiParameterStream(
      @Nullable PsiMethod currentMethod, PsiMethod[] allMethods) {
    return Stream.of(allMethods)
        .filter(psiMethod -> !psiMethod.equals(currentMethod))
        .map(PsiMethod::getParameterList)
        .map(PsiParameterList::getParameters)
        .flatMap(Stream::of)
        .filter(distinctByKey(PsiParameter::getName));
  }

  private static <T> Predicate<T> distinctByKey(Function<? super T, ?> key) {
    Set<Object> seen = ContainerUtil.newConcurrentSet();
    return t -> seen.add(key.apply(t));
  }
}
