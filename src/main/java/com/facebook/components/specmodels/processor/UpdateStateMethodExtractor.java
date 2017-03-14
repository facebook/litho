// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.processor;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import com.facebook.common.internal.ImmutableList;
import com.facebook.components.annotations.OnUpdateState;
import com.facebook.components.annotations.Param;
import com.facebook.components.annotations.Prop;
import com.facebook.components.annotations.State;
import com.facebook.components.annotations.TreeProp;
import com.facebook.components.specmodels.model.MethodParamModel;
import com.facebook.components.specmodels.model.UpdateStateMethodModel;

import com.squareup.javapoet.TypeName;

import static com.facebook.components.specmodels.processor.MethodExtractorUtils.getMethodParams;

/**
 * Extracts methods annotated with {@link OnUpdateState} from the given input.
 */
public class UpdateStateMethodExtractor {

  private static final List<Class<? extends Annotation>> METHOD_PARAM_ANNOTATIONS =
      new ArrayList<>();
  static {
    METHOD_PARAM_ANNOTATIONS.add(Param.class);
    METHOD_PARAM_ANNOTATIONS.add(Prop.class);
    METHOD_PARAM_ANNOTATIONS.add(State.class);
    METHOD_PARAM_ANNOTATIONS.add(TreeProp.class);
  }

  /**
   * Get the delegate methods from the given {@link TypeElement}.
   */
  public static ImmutableList<UpdateStateMethodModel> getOnUpdateStateMethods(
      TypeElement typeElement,
      List<Class<? extends Annotation>> permittedInterStageInputAnnotations) {
    final List<UpdateStateMethodModel> delegateMethods = new ArrayList<>();

    for (Element enclosedElement : typeElement.getEnclosedElements()) {
      if (enclosedElement.getKind() != ElementKind.METHOD) {
        continue;
      }

      Annotation onUpdateStateAnnotation = enclosedElement.getAnnotation(OnUpdateState.class);

      if (onUpdateStateAnnotation != null) {
        final ExecutableElement executableElement = (ExecutableElement) enclosedElement;
        final List<MethodParamModel> methodParams =
            getMethodParams(
                executableElement,
                getPermittedMethodParamAnnotations(permittedInterStageInputAnnotations),
                permittedInterStageInputAnnotations);

        final UpdateStateMethodModel delegateMethod =
            new UpdateStateMethodModel(
                onUpdateStateAnnotation,
                ImmutableList.copyOf(new ArrayList<>(executableElement.getModifiers())),
                executableElement.getSimpleName(),
                TypeName.get(executableElement.getReturnType()),
                ImmutableList.copyOf(methodParams),
                executableElement);
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
