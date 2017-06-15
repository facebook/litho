/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.squareup.javapoet.ParameterizedTypeName;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import static com.facebook.litho.specmodels.generator.GeneratorConstants.DELEGATE_FIELD_NAME;
import static com.facebook.litho.specmodels.model.ClassNames.OUTPUT;

/**
 * Utility methods for {@link SpecModel}s.
 */
public class SpecModelUtils {

  public static String getSpecAccessor(SpecModel specModel) {
    if (specModel.hasInjectedDependencies()) {
      return DELEGATE_FIELD_NAME +
          specModel.getDependencyInjectionHelper().getSourceDelegateAccessorMethod(specModel);
    }

    return specModel.getSpecName();
  }

  @Nullable
  public static PropModel getPropWithName(SpecModel specModel, String name) {
    for (PropModel prop : specModel.getProps()) {
      if (prop.getName().equals(name)) {
        return prop;
      }
    }

    return null;
  }

  @Nullable
  public static StateParamModel getStateValueWithName(SpecModel specModel, String name) {
    for (StateParamModel stateValue : specModel.getStateValues()) {
      if (stateValue.getName().equals(name)) {
        return stateValue;
      }
    }

    return null;
  }

  @Nullable
  public static DelegateMethodModel getMethodModelWithAnnotation(
      SpecModel specModel,
      Class<? extends Annotation> annotationClass) {
    for (DelegateMethodModel delegateMethodModel : specModel.getDelegateMethods()) {
      for (Annotation annotation : delegateMethodModel.annotations) {
        if (annotation.annotationType().equals(annotationClass)) {
          return delegateMethodModel;
        }
      }
    }

    return null;
  }

  public static List<DelegateMethodModel> getMethodModelsWithAnnotation(
      SpecModel specModel,
      Class<? extends Annotation> annotationClass) {
    final List<DelegateMethodModel> methodModels = new ArrayList<>();
    for (DelegateMethodModel delegateMethodModel : specModel.getDelegateMethods()) {
      for (Annotation annotation : delegateMethodModel.annotations) {
        if (annotation.annotationType().equals(annotationClass)) {
          methodModels.add(delegateMethodModel);
        }
      }
    }

    return methodModels;
  }

  public static boolean isPropOutput(SpecModel specModel, MethodParamModel methodParamModel) {
    final PropModel prop = getPropWithName(specModel, methodParamModel.getName());
    return prop != null &&
        methodParamModel.getType() instanceof ParameterizedTypeName &&
        ((ParameterizedTypeName) methodParamModel.getType()).rawType.equals(OUTPUT) &&
        ((ParameterizedTypeName) methodParamModel.getType()).typeArguments.size() == 1 &&
        ((ParameterizedTypeName) methodParamModel.getType()).typeArguments.get(0)
            .equals(prop.getType().box());
  }

  public static boolean isStateOutput(SpecModel specModel, MethodParamModel methodParamModel) {
    final StateParamModel stateValue =
        SpecModelUtils.getStateValueWithName(specModel, methodParamModel.getName());
    return stateValue != null &&
        methodParamModel.getType() instanceof ParameterizedTypeName &&
        ((ParameterizedTypeName) methodParamModel.getType()).rawType.equals(OUTPUT) &&
        ((ParameterizedTypeName) methodParamModel.getType()).typeArguments.size() == 1 &&
        ((ParameterizedTypeName) methodParamModel.getType()).typeArguments.get(0)
            .equals(stateValue.getType().box());
  }

  public static boolean isStateValue(SpecModel specModel, MethodParamModel methodParamModel) {
    final StateParamModel stateValue =
        SpecModelUtils.getStateValueWithName(specModel, methodParamModel.getName());
    return stateValue != null &&
        methodParamModel.getType() instanceof ParameterizedTypeName &&
        ((ParameterizedTypeName) methodParamModel.getType()).rawType
            .equals(ClassNames.STATE_VALUE) &&
        ((ParameterizedTypeName) methodParamModel.getType()).typeArguments.size() == 1 &&
        ((ParameterizedTypeName) methodParamModel.getType()).typeArguments.get(0)
            .equals(stateValue.getType().box());
  }

  /**
   * @return the model for state/prop that this Diff is refering to.
   */
  public static MethodParamModel getReferencedParamModelForDiff(
      SpecModel specModel,
      DiffModel diffModel) {
    if (MethodParamModelUtils.isAnnotatedWith(diffModel, Prop.class)) {
      return SpecModelUtils.getPropWithName(specModel, diffModel.getName());
    } else if (MethodParamModelUtils.isAnnotatedWith(diffModel, State.class)) {
      return SpecModelUtils.getStateValueWithName(specModel, diffModel.getName());
    }

    throw new RuntimeException(
        "Diff model wasn't annotated with @State or @Prop, some validation failed");
  }

  public static boolean hasAnnotation(
      MethodParamModel methodParam,
      Class<?> annotationClass) {
    for (Annotation annotation : methodParam.getAnnotations()) {
      if (annotation.annotationType().equals(annotationClass)) {
        return true;
      }
    }

    return false;
  }

  public static boolean hasDiffThatNeedsRenderInfoInfra(SpecModel specModel) {
    for (DiffModel diff : specModel.getDiffs()) {
      if (diff.needsRenderInfoInfra()) {
        return true;
      }
    }
    return false;
  }
}
