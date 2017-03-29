/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import javax.lang.model.element.Modifier;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.facebook.common.internal.ImmutableList;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

/**
 * Class for validating that the {@link DelegateMethodModel}s for a {@link SpecModel} are
 * well-formed.
 */
public class DelegateMethodValidation {

  static List<SpecModelValidationError> validateLayoutSpecModel(
      LayoutSpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(
        validateMethods(specModel, DelegateMethodDescriptions.LAYOUT_SPEC_DELEGATE_METHODS_MAP));

    final DelegateMethodModel onCreateLayoutModel =
        SpecModelUtils.getMethodModelWithAnnotation(specModel, OnCreateLayout.class);
    final DelegateMethodModel onCreateLayoutWithSizeSpecModel =
        SpecModelUtils.getMethodModelWithAnnotation(specModel, OnCreateLayoutWithSizeSpec.class);

    if (onCreateLayoutModel == null && onCreateLayoutWithSizeSpecModel == null) {
      validationErrors.add(
          new SpecModelValidationError(
              specModel.getRepresentedObject(),
              "You need to have a method annotated with either @OnCreateLayout " +
                  "or @OnCreateLayoutWithSizeSpec in your spec. In most cases, @OnCreateLayout " +
                  "is what you want."));
    } else if (onCreateLayoutModel != null && onCreateLayoutWithSizeSpecModel != null) {
      validationErrors.add(
          new SpecModelValidationError(
              specModel.getRepresentedObject(),
              "Your LayoutSpec should have a method annotated with either @OnCreateLayout " +
                  "or @OnCreateLayoutWithSizeSpec, but not both. In most cases, @OnCreateLayout " +
                  "is what you want."));
    }

    return validationErrors;
  }

  static List<SpecModelValidationError> validateMethods(
      SpecModel specModel,
      Map<Class<? extends Annotation>, DelegateMethodDescription> delegateMethodDescriptions) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();

    for (DelegateMethodModel delegateMethod : specModel.getDelegateMethods()) {
      if (!specModel.hasInjectedDependencies() &&
          !delegateMethod.modifiers.contains(Modifier.STATIC)) {
        validationErrors.add(
            new SpecModelValidationError(
                delegateMethod.representedObject,
                "Methods in a spec that doesn't have dependency injection must be static."));
      }
    }

    for (Map.Entry<Class<? extends Annotation>, DelegateMethodDescription> entry :
        delegateMethodDescriptions.entrySet()) {
      final Class<? extends Annotation> delegateMethodAnnotation = entry.getKey();
      final DelegateMethodDescription delegateMethodDescription = entry.getValue();

      final DelegateMethodModel delegateMethod =
          SpecModelUtils.getMethodModelWithAnnotation(specModel, delegateMethodAnnotation);
      if (delegateMethod == null) {
        continue;
      }

      final ImmutableList<TypeName> definedParameterTypes =
          delegateMethodDescription.definedParameterTypes;
      if (delegateMethod.methodParams.size() < definedParameterTypes.size()) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0, size = definedParameterTypes.size(); i < size; i++) {
          stringBuilder.append(definedParameterTypes.get(i));
          if (i < size - 1) {
            stringBuilder.append(", ");
          }
        }
        validationErrors.add(
            new SpecModelValidationError(
                delegateMethod.representedObject,
                "Methods annotated with " + delegateMethodAnnotation + " must have at least " +
                    definedParameterTypes.size() + " parameters, and they should be of type " +
                    stringBuilder.toString() + "."));
      }

      for (int i = 0, size = delegateMethod.methodParams.size(); i < size; i++) {
        final MethodParamModel delegateMethodParam = delegateMethod.methodParams.get(i);

        if (i < definedParameterTypes.size()) {
          if (!delegateMethodParam.getType().equals(definedParameterTypes.get(i))) {
            validationErrors.add(
                new SpecModelValidationError(
                    delegateMethodParam.getRepresentedObject(),
                    "Parameter in position " + i + " of a method annotated with " +
                        delegateMethodAnnotation + " should be of type " +
                        definedParameterTypes.get(i) + "."));
          }
        } else {
          if (!isOptionalParamValid(
              specModel,
              delegateMethodDescription.optionalParameterTypes,
              delegateMethodParam)) {
            validationErrors.add(
                new SpecModelValidationError(
                    delegateMethodParam.getRepresentedObject(),
                    "Not a valid parameter, should be one of the following: " +
                        getStringRepresentationOfParamTypes(
                            delegateMethodDescription.optionalParameterTypes)));
          }
        }
      }
    }

    return validationErrors;
  }

  private static boolean isOptionalParamValid(
      SpecModel specModel,
      ImmutableList<OptionalParameterType> parameterTypes,
      MethodParamModel methodParamModel) {
    for (OptionalParameterType optionalParameterType : parameterTypes) {
      if (isParamOfType(specModel, optionalParameterType, methodParamModel)) {
        return true;
      }
    }

    return false;
  }

  private static boolean isParamOfType(
      SpecModel specModel,
      OptionalParameterType optionalParameterType,
      MethodParamModel methodParamModel) {
    switch (optionalParameterType) {
      case PROP:
        return MethodParamModelUtils.isAnnotatedWith(methodParamModel, Prop.class);
      case TREE_PROP:
        return MethodParamModelUtils.isAnnotatedWith(methodParamModel, TreeProp.class);
      case STATE:
        return MethodParamModelUtils.isAnnotatedWith(methodParamModel, State.class);
      case PARAM:
        return MethodParamModelUtils.isAnnotatedWith(methodParamModel, Param.class);
      case INTER_STAGE_OUTPUT:
        return methodParamModel.getType() instanceof ParameterizedTypeName &&
            ((ParameterizedTypeName) methodParamModel.getType()).rawType.equals(ClassNames.OUTPUT);
      case PROP_OUTPUT:
        return SpecModelUtils.isPropOutput(specModel, methodParamModel);
      case STATE_OUTPUT:
        return SpecModelUtils.isStateOutput(specModel, methodParamModel);
      case STATE_VALUE:
        return SpecModelUtils.isStateValue(specModel, methodParamModel);
    }

    return false;
  }

  private static String getStringRepresentationOfParamTypes(
      ImmutableList<OptionalParameterType> optionalParameterTypes) {
    final StringBuilder stringBuilder = new StringBuilder();
    for (OptionalParameterType parameterType : optionalParameterTypes) {
      stringBuilder
          .append(getStringRepresentationOfParamType(parameterType))
          .append(". ");
    }

    return stringBuilder.toString();
  }

  private static String getStringRepresentationOfParamType(
      OptionalParameterType optionalParameterType) {
    switch (optionalParameterType) {
      case PROP:
        return "@Prop T somePropName";
      case TREE_PROP:
        return "@TreeProp T someTreePropName";
      case STATE:
        return "@State T someStateName";
      case PARAM:
        return "@Param T someParamName";
      case INTER_STAGE_OUTPUT:
        return "Output<T> someOutputName";
      case PROP_OUTPUT:
        return "Output<T> propName, where a prop with type T and name propName is " +
            "declared elsewhere in the spec";
      case STATE_OUTPUT:
        return "Output<T> stateName, where a state param with type T and name stateName is " +
            "declared elsewhere in the spec";
      case STATE_VALUE:
        return "StateValue<T> stateName, where a state param with type T and name stateName is " +
            "declared elsewhere in the spec";
    }

    return "Unexpected parameter type - please report to the Components team";
  }
}
