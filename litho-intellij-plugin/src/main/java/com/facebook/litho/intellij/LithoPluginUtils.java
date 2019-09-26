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

import com.facebook.litho.annotations.Event;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.State;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import java.util.Arrays;
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
        .filter(LithoClassNames.SECTION_CLASS_NAME::equals)
        .isPresent();
  }

  public static boolean isLithoSpec(@Nullable PsiFile psiFile) {
    if (psiFile == null) {
      return false;
    }
    return getFirstClass(psiFile, LithoPluginUtils::isLithoSpec).isPresent();
  }

  public static boolean isLithoSpec(@Nullable PsiClass psiClass) {
    return psiClass != null
        && (hasLithoComponentSpecAnnotation(psiClass) || hasLithoSectionSpecAnnotation(psiClass));
  }

  public static boolean isLayoutSpec(@Nullable PsiClass psiClass) {
    return psiClass != null && hasAnnotation(psiClass, equals(LayoutSpec.class.getName()));
  }

  static boolean isMountSpec(@Nullable PsiClass psiClass) {
    return psiClass != null && hasAnnotation(psiClass, equals(MountSpec.class.getName()));
  }

  public static boolean hasLithoComponentSpecAnnotation(@Nullable PsiClass psiClass) {
    if (psiClass == null) {
      return false;
    }
    return isSpecName(psiClass.getName()) && (isLayoutSpec(psiClass) || isMountSpec(psiClass));
  }

  public static boolean hasLithoSectionSpecAnnotation(PsiClass psiClass) {
    return isSpecName(psiClass.getName())
        && hasAnnotation(psiClass, startsWith("com.facebook.litho.sections.annotations"));
  }

  @VisibleForTesting
  /** @return if given name ends with "Spec". */
  static boolean isSpecName(@Nullable String clsName) {
    return clsName != null && clsName.endsWith(SPEC_SUFFIX);
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

  public static boolean isPropOrState(PsiParameter parameter) {
    return isProp(parameter) || isState(parameter);
  }

  public static boolean isProp(PsiParameter parameter) {
    return hasAnnotation(parameter, equals(Prop.class.getName()));
  }

  public static boolean isState(PsiParameter parameter) {
    return hasAnnotation(parameter, equals(State.class.getName()));
  }

  public static boolean isParam(PsiParameter parameter) {
    return hasAnnotation(parameter, equals(Param.class.getName()));
  }

  public static boolean isPropDefault(PsiField field) {
    return hasAnnotation(field, equals(PropDefault.class.getName()));
  }

  public static boolean isEvent(PsiClass psiClass) {
    return hasAnnotation(psiClass, equals(Event.class.getName()));
  }

  @Nullable
  public static String getLithoComponentNameFromSpec(@Nullable String specName) {
    if (isSpecName(specName)) {
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

  /**
   * Finds file containing Component from the given Spec name.
   *
   * @param qualifiedSpecName Name of the Spec to search component for. For example
   *     com.package.MySpec.java.
   * @param project Project to find Component in.
   * @return {@link PsiJavaFile} for the generated Component, for example com.package.My.java
   */
  public static Optional<PsiJavaFile> findGeneratedFile(String qualifiedSpecName, Project project) {
    return Optional.of(qualifiedSpecName)
        .map(LithoPluginUtils::getLithoComponentNameFromSpec)
        .map(qualifiedComponentName -> PsiSearchUtils.findClass(project, qualifiedComponentName))
        .map(PsiElement::getContainingFile)
        .filter(PsiJavaFile.class::isInstance)
        .map(PsiJavaFile.class::cast);
  }

  /**
   * Finds Generated Class from the given Spec name.
   *
   * @param qualifiedSpecName Name of the Spec to search generated class for. For example
   *     com.package.MySpec.java. If you provide simple name MySpec.java returned class could be
   *     found in wrong package.
   * @param project Project to find generated class in.
   */
  public static Optional<PsiClass> findGeneratedClass(String qualifiedSpecName, Project project) {
    return findGeneratedFile(qualifiedSpecName, project)
        .flatMap(
            generatedFile ->
                getFirstClass(
                    generatedFile,
                    psiClass ->
                        LithoPluginUtils.isComponentClass(psiClass)
                            || LithoPluginUtils.isSectionClass(psiClass)));
  }

  /** Finds LayoutSpec class in the given file. */
  public static Optional<PsiClass> getFirstLayoutSpec(PsiFile psiFile) {
    return getFirstClass(psiFile, LithoPluginUtils::isLayoutSpec);
  }

  public static Optional<PsiClass> getFirstClass(PsiFile psiFile, Predicate<PsiClass> classFilter) {
    return Optional.of(psiFile)
        .map(currentFile -> PsiTreeUtil.getChildrenOfType(currentFile, PsiClass.class))
        .flatMap(
            currentClasses ->
                Arrays.stream(currentClasses)
                    .filter(Objects::nonNull)
                    .filter(classFilter)
                    .findFirst());
  }
}
