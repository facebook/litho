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

import static com.facebook.litho.specmodels.internal.ImmutableList.copyOf;
import static com.facebook.litho.specmodels.model.SpecModelUtils.generateTypeSpec;
import static com.facebook.litho.specmodels.processor.MethodExtractorUtils.getMethodParams;

import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.OnUpdateStateWithTransition;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.UpdateStateMethod;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/** Extracts methods annotated with {@link OnUpdateState} from the given input. */
public class UpdateStateMethodExtractor {

  private static final List<Class<? extends Annotation>> METHOD_PARAM_ANNOTATIONS =
      new ArrayList<>();

  static {
    METHOD_PARAM_ANNOTATIONS.add(Param.class);
    METHOD_PARAM_ANNOTATIONS.add(Prop.class);
    METHOD_PARAM_ANNOTATIONS.add(State.class);
    METHOD_PARAM_ANNOTATIONS.add(TreeProp.class);
  }

  /** Get the delegate methods from the given {@link TypeElement}. */
  public static ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> getOnUpdateStateMethods(
      TypeElement typeElement,
      List<Class<? extends Annotation>> permittedInterStageInputAnnotations,
      Messager messager) {
    return extractOnUpdateStateMethods(
        typeElement, permittedInterStageInputAnnotations, messager, false);
  }

  /** Get the delegate methods from the given {@link TypeElement}. */
  public static ImmutableList<SpecMethodModel<UpdateStateMethod, Void>>
      getOnUpdateStateWithTransitionMethods(
          TypeElement typeElement,
          List<Class<? extends Annotation>> permittedInterStageInputAnnotations,
          Messager messager) {
    return extractOnUpdateStateMethods(
        typeElement, permittedInterStageInputAnnotations, messager, true);
  }

  private static ImmutableList<SpecMethodModel<UpdateStateMethod, Void>>
      extractOnUpdateStateMethods(
          TypeElement typeElement,
          List<Class<? extends Annotation>> permittedInterStageInputAnnotations,
          Messager messager,
          boolean isTransitionMethod) {
    final List<SpecMethodModel<UpdateStateMethod, Void>> delegateMethods = new ArrayList<>();

    for (Element enclosedElement : typeElement.getEnclosedElements()) {
      if (enclosedElement.getKind() != ElementKind.METHOD) {
        continue;
      }

      final Annotation onUpdateStateAnnotation =
          isTransitionMethod
              ? enclosedElement.getAnnotation(OnUpdateStateWithTransition.class)
              : enclosedElement.getAnnotation(OnUpdateState.class);

      if (onUpdateStateAnnotation != null) {
        final ExecutableElement executableElement = (ExecutableElement) enclosedElement;
        final List<MethodParamModel> methodParams =
            getMethodParams(
                executableElement,
                messager,
                getPermittedMethodParamAnnotations(permittedInterStageInputAnnotations),
                permittedInterStageInputAnnotations,
                ImmutableList.<Class<? extends Annotation>>of());

        final SpecMethodModel<UpdateStateMethod, Void> delegateMethod =
            SpecMethodModel.<UpdateStateMethod, Void>builder()
                .annotations(ImmutableList.<Annotation>of(onUpdateStateAnnotation))
                .modifiers(copyOf(new ArrayList<>(executableElement.getModifiers())))
                .name(executableElement.getSimpleName())
                .returnTypeSpec(generateTypeSpec(executableElement.getReturnType()))
                .typeVariables(ImmutableList.of())
                .methodParams(copyOf(methodParams))
                .representedObject(executableElement)
                .typeModel(null)
                .build();
        delegateMethods.add(delegateMethod);
      }
    }

    return ImmutableList.copyOf(delegateMethods);
  }

  private static List<Class<? extends Annotation>> getPermittedMethodParamAnnotations(
      List<Class<? extends Annotation>> permittedInterStageInputAnnotations) {
    final List<Class<? extends Annotation>> permittedMethodParamAnnotations =
        new ArrayList<>(METHOD_PARAM_ANNOTATIONS);
    permittedMethodParamAnnotations.addAll(permittedInterStageInputAnnotations);

    return permittedMethodParamAnnotations;
  }
}
