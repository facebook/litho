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

package com.facebook.litho.specmodels.processor;

import static com.facebook.litho.specmodels.model.SpecModelUtils.generateTypeSpec;
import static com.facebook.litho.specmodels.processor.MethodExtractorUtils.getMethodParams;
import static com.facebook.litho.specmodels.processor.MethodExtractorUtils.getTypeVariables;

import com.facebook.litho.annotations.CachedValue;
import com.facebook.litho.annotations.InjectProp;
import com.facebook.litho.annotations.OnEnteredRange;
import com.facebook.litho.annotations.OnExitedRange;
import com.facebook.litho.annotations.OnRegisterRanges;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.WorkingRangeDeclarationModel;
import com.facebook.litho.specmodels.model.WorkingRangeMethodModel;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/** Extracts working ranges methods from the given input. */
public class WorkingRangesMethodExtractor {

  private static final List<Class<? extends Annotation>> METHOD_PARAM_ANNOTATIONS =
      new ArrayList<>();

  static {
    METHOD_PARAM_ANNOTATIONS.add(Prop.class);
    METHOD_PARAM_ANNOTATIONS.add(State.class);
    METHOD_PARAM_ANNOTATIONS.add(TreeProp.class);
    METHOD_PARAM_ANNOTATIONS.add(InjectProp.class);
    METHOD_PARAM_ANNOTATIONS.add(CachedValue.class);
  }

  private WorkingRangesMethodExtractor() {}

  @Nullable
  public static SpecMethodModel<EventMethod, Void> getRegisterMethod(
      TypeElement typeElement,
      List<Class<? extends Annotation>> permittedInterStageInputAnnotations,
      Messager messager) {
    for (Element enclosedElement : typeElement.getEnclosedElements()) {
      if (enclosedElement.getKind() != ElementKind.METHOD) {
        continue;
      }

      final ExecutableElement executableElement = (ExecutableElement) enclosedElement;
      final Annotation registerRangesAnnotation =
          enclosedElement.getAnnotation(OnRegisterRanges.class);

      if (registerRangesAnnotation != null) {
        final List<MethodParamModel> methodParams =
            getMethodParams(
                executableElement,
                messager,
                getPermittedMethodParamAnnotations(permittedInterStageInputAnnotations),
                permittedInterStageInputAnnotations,
                ImmutableList.of());

        return SpecMethodModel.<EventMethod, Void>builder()
            .annotations(ImmutableList.of())
            .modifiers(ImmutableList.copyOf(new ArrayList<>(executableElement.getModifiers())))
            .name(executableElement.getSimpleName())
            .returnTypeSpec(generateTypeSpec(executableElement.getReturnType()))
            .typeVariables(ImmutableList.copyOf(getTypeVariables(executableElement)))
            .methodParams(ImmutableList.copyOf(methodParams))
            .representedObject(executableElement)
            .build();
      }
    }
    return null;
  }

  /** Get the delegate methods from the given {@link TypeElement}. */
  public static ImmutableList<WorkingRangeMethodModel> getRangesMethods(
      Elements elements,
      TypeElement typeElement,
      List<Class<? extends Annotation>> permittedInterStageInputAnnotations,
      Messager messager) {
    final List<WorkingRangeMethodModel> workingRangeMethods = new ArrayList<>();

    for (Element enclosedElement : typeElement.getEnclosedElements()) {
      if (enclosedElement.getKind() != ElementKind.METHOD) {
        continue;
      }

      final ExecutableElement executableElement = (ExecutableElement) enclosedElement;
      final OnEnteredRange enteredRangeAnnotation =
          enclosedElement.getAnnotation(OnEnteredRange.class);
      final OnExitedRange exitedRangeAnnotation =
          enclosedElement.getAnnotation(OnExitedRange.class);

      if (enteredRangeAnnotation != null) {
        SpecMethodModel<EventMethod, WorkingRangeDeclarationModel> enteredRangeMethod =
            generateWorkingRangeMethod(
                elements,
                executableElement,
                permittedInterStageInputAnnotations,
                messager,
                OnEnteredRange.class);

        final String name = enteredRangeAnnotation.name();
        final WorkingRangeMethodModel workingRangeModel =
            workingRangeMethods.stream()
                .filter(it -> it.name.equals(name) && it.enteredRangeModel == null)
                .findFirst()
                .orElseGet(
                    () -> {
                      WorkingRangeMethodModel model = new WorkingRangeMethodModel(name);
                      workingRangeMethods.add(model);
                      return model;
                    });
        workingRangeModel.enteredRangeModel = enteredRangeMethod;
      }

      if (exitedRangeAnnotation != null) {
        SpecMethodModel<EventMethod, WorkingRangeDeclarationModel> exitedRangeMethod =
            generateWorkingRangeMethod(
                elements,
                executableElement,
                permittedInterStageInputAnnotations,
                messager,
                OnExitedRange.class);

        final String name = exitedRangeAnnotation.name();
        final WorkingRangeMethodModel workingRangeModel =
            workingRangeMethods.stream()
                .filter(it -> it.name.equals(name) && it.exitedRangeModel == null)
                .findFirst()
                .orElseGet(
                    () -> {
                      WorkingRangeMethodModel model = new WorkingRangeMethodModel(name);
                      workingRangeMethods.add(model);
                      return model;
                    });
        workingRangeModel.exitedRangeModel = exitedRangeMethod;
      }
    }
    return ImmutableList.copyOf(workingRangeMethods);
  }

  private static List<Class<? extends Annotation>> getPermittedMethodParamAnnotations(
      List<Class<? extends Annotation>> permittedInterStageInputAnnotations) {
    final List<Class<? extends Annotation>> permittedMethodParamAnnotations =
        new ArrayList<>(METHOD_PARAM_ANNOTATIONS);
    permittedMethodParamAnnotations.addAll(permittedInterStageInputAnnotations);
    return permittedMethodParamAnnotations;
  }

  @Nullable
  private static SpecMethodModel<EventMethod, WorkingRangeDeclarationModel>
      generateWorkingRangeMethod(
          Elements elements,
          ExecutableElement executableElement,
          List<Class<? extends Annotation>> permittedInterStageInputAnnotations,
          Messager messager,
          Class<? extends Annotation> annotationType) {
    final List<MethodParamModel> methodParams =
        getMethodParams(
            executableElement,
            messager,
            getPermittedMethodParamAnnotations(permittedInterStageInputAnnotations),
            permittedInterStageInputAnnotations,
            ImmutableList.of());

    final String nameInAnnotation =
        ProcessorUtils.getAnnotationParameter(
            elements, executableElement, annotationType, "name", String.class);

    List<? extends AnnotationMirror> annotationMirrors = executableElement.getAnnotationMirrors();
    AnnotationMirror mirror = null;
    for (AnnotationMirror m : annotationMirrors) {
      if (m.getAnnotationType().toString().equals(annotationType.getCanonicalName())) {
        mirror = m;
        break;
      }
    }

    return SpecMethodModel.<EventMethod, WorkingRangeDeclarationModel>builder()
        .annotations(ImmutableList.of())
        .modifiers(ImmutableList.copyOf(new ArrayList<>(executableElement.getModifiers())))
        .name(executableElement.getSimpleName())
        .returnTypeSpec(generateTypeSpec(executableElement.getReturnType()))
        .typeVariables(ImmutableList.copyOf(getTypeVariables(executableElement)))
        .methodParams(ImmutableList.copyOf(methodParams))
        .representedObject(executableElement)
        .typeModel(new WorkingRangeDeclarationModel(nameInAnnotation, mirror))
        .build();
  }
}
