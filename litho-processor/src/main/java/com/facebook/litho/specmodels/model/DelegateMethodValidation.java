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

package com.facebook.litho.specmodels.model;

import static com.facebook.litho.specmodels.model.DelegateMethodDescription.isAllowedTypeAndConsume;
import static com.facebook.litho.specmodels.model.SpecMethodModelValidation.validateMethodIsStatic;
import static com.facebook.litho.specmodels.model.SpecMethodModelValidation.validateMethodName;

import com.facebook.litho.annotations.InjectProp;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.StringJoiner;
import javax.annotation.Nullable;

/**
 * Class for validating that the {@link SpecMethodModel}s for a {@link SpecModel} are well-formed.
 */
public class DelegateMethodValidation {

  static List<SpecModelValidationError> validateLayoutSpecModel(LayoutSpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(
        validateMethods(
            specModel,
            DelegateMethodDescriptions.LAYOUT_SPEC_DELEGATE_METHODS_MAP,
            DelegateMethodDescriptions.INTER_STAGE_INPUTS_MAP));

    final SpecMethodModel<DelegateMethod, Void> onCreateLayoutModel =
        SpecModelUtils.getMethodModelWithAnnotation(specModel, OnCreateLayout.class);
    final SpecMethodModel<DelegateMethod, Void> onCreateLayoutWithSizeSpecModel =
        SpecModelUtils.getMethodModelWithAnnotation(specModel, OnCreateLayoutWithSizeSpec.class);

    if (onCreateLayoutModel == null && onCreateLayoutWithSizeSpecModel == null) {
      validationErrors.add(
          new SpecModelValidationError(
              specModel.getRepresentedObject(),
              "You need to have a method annotated with either @OnCreateLayout "
                  + "or @OnCreateLayoutWithSizeSpec in your spec. In most cases, @OnCreateLayout "
                  + "is what you want."));
    } else if (onCreateLayoutModel != null && onCreateLayoutWithSizeSpecModel != null) {
      validationErrors.add(
          new SpecModelValidationError(
              specModel.getRepresentedObject(),
              "Your LayoutSpec should have a method annotated with either @OnCreateLayout "
                  + "or @OnCreateLayoutWithSizeSpec, but not both. In most cases, @OnCreateLayout "
                  + "is what you want."));
    }

    return validationErrors;
  }

  static List<SpecModelValidationError> validateMountSpecModel(MountSpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(
        validateMethods(
            specModel,
            DelegateMethodDescriptions.MOUNT_SPEC_DELEGATE_METHODS_MAP,
            DelegateMethodDescriptions.INTER_STAGE_INPUTS_MAP));

    final SpecMethodModel<DelegateMethod, Void> onCreateMountContentModel =
        SpecModelUtils.getMethodModelWithAnnotation(specModel, OnCreateMountContent.class);
    if (onCreateMountContentModel == null) {
      validationErrors.add(
          new SpecModelValidationError(
              specModel.getRepresentedObject(),
              "All MountSpecs need to have a method annotated with @OnCreateMountContent."));
    } else {
      final TypeName mountType = onCreateMountContentModel.returnType;
      ImmutableList<Class<? extends Annotation>> methodsAcceptingMountTypeAsSecondParam =
          ImmutableList.of(OnMount.class, OnBind.class, OnUnbind.class, OnUnmount.class);
      for (Class<? extends Annotation> annotation : methodsAcceptingMountTypeAsSecondParam) {
        final SpecMethodModel<DelegateMethod, Void> method =
            SpecModelUtils.getMethodModelWithAnnotation(specModel, annotation);
        if (method != null
            && (method.methodParams.size() < 2
                || !method.methodParams.get(1).getTypeName().equals(mountType))) {
          validationErrors.add(
              new SpecModelValidationError(
                  method.representedObject,
                  "The second parameter of a method annotated with "
                      + annotation
                      + " must "
                      + "have the same type as the return type of the method annotated with "
                      + "@OnCreateMountContent (i.e. "
                      + mountType
                      + ")."));
        }
      }
    }

    return validationErrors;
  }

  public static List<SpecModelValidationError> validateMethods(
      SpecModel specModel,
      Map<Class<? extends Annotation>, DelegateMethodDescription> delegateMethodDescriptions,
      Map<Class<? extends Annotation>, Class<? extends Annotation>> interStageInputsMap) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();

    for (SpecMethodModel<DelegateMethod, Void> delegateMethod : specModel.getDelegateMethods()) {
      validationErrors.addAll(validateMethodIsStatic(specModel, delegateMethod));
      validationErrors.addAll(validateMethodName(delegateMethod));
    }

    for (Map.Entry<Class<? extends Annotation>, DelegateMethodDescription> entry :
        delegateMethodDescriptions.entrySet()) {
      final Class<? extends Annotation> delegateMethodAnnotation = entry.getKey();
      final DelegateMethodDescription delegateMethodDescription = entry.getValue();

      final SpecMethodModel<DelegateMethod, Void> delegateMethod =
          SpecModelUtils.getMethodModelWithAnnotation(specModel, delegateMethodAnnotation);

      if (delegateMethod == null) {
        continue;
      }
      final ImmutableList<TypeName> allowedDelegateMethodArgs =
          delegateMethodDescription.allowedDelegateMethodArguments();

      validationErrors.addAll(
          validateDefinedParameterTypes(
              delegateMethod, delegateMethodAnnotation, allowedDelegateMethodArgs));

      validationErrors.addAll(
          validateReturnType(delegateMethod, delegateMethodAnnotation, delegateMethodDescription));

      int numberOfAllowedMethodArgsUsed =
          getNumberOfAllowedMethodArgsUsed(delegateMethod.methodParams, allowedDelegateMethodArgs);

      for (int i = numberOfAllowedMethodArgsUsed, size = delegateMethod.methodParams.size();
          i < size;
          i++) {
        final MethodParamModel delegateMethodParam = delegateMethod.methodParams.get(i);
        if (isOptionalParameter(delegateMethodParam, delegateMethodDescription.optionalParameters)
            && i
                < numberOfAllowedMethodArgsUsed
                    + delegateMethodDescription.optionalParameters.size()) {
          continue;
        }

        if (delegateMethodParam instanceof InterStageInputParamModel) {
          final Annotation annotation =
              getInterStageInputAnnotation(
                  delegateMethodParam, delegateMethodDescription.interStageInputAnnotations);

          if (annotation == null) {
            validationErrors.add(
                new SpecModelValidationError(
                    delegateMethodParam.getRepresentedObject(),
                    "Inter-stage input annotation is not valid for methods annotated with "
                        + delegateMethodAnnotation
                        + "; please use one "
                        + "of the following: "
                        + delegateMethodDescription.interStageInputAnnotations));
          } else {
            final Class<? extends Annotation> interStageOutputMethodAnnotation =
                interStageInputsMap.get(annotation.annotationType());

            final SpecMethodModel<DelegateMethod, Void> interStageOutputMethod =
                SpecModelUtils.getMethodModelWithAnnotation(
                    specModel, interStageOutputMethodAnnotation);

            if (interStageOutputMethod == null) {
              validationErrors.add(
                  new SpecModelValidationError(
                      delegateMethodParam.getRepresentedObject(),
                      "To use "
                          + annotation.annotationType()
                          + " on param "
                          + delegateMethodParam.getName()
                          + " you must have a method annotated "
                          + "with "
                          + interStageOutputMethodAnnotation
                          + " that has a param "
                          + "Output<"
                          + delegateMethodParam.getTypeName().box()
                          + "> "
                          + delegateMethodParam.getName()));
            } else if (!hasMatchingInterStageOutput(interStageOutputMethod, delegateMethodParam)) {
              validationErrors.add(
                  new SpecModelValidationError(
                      delegateMethodParam.getRepresentedObject(),
                      "To use "
                          + annotation.annotationType()
                          + " on param "
                          + delegateMethodParam.getName()
                          + " your method annotated "
                          + "with "
                          + interStageOutputMethodAnnotation
                          + " must have a param "
                          + "Output<"
                          + delegateMethodParam.getTypeName().box()
                          + "> "
                          + delegateMethodParam.getName()));
            }

            validateUniqueInterStatePropNames(
                interStageOutputMethodAnnotation,
                delegateMethodParam.getName(),
                specModel,
                validationErrors);
          }
        } else if (!isOptionalParamValid(
            specModel, delegateMethodDescription.optionalParameterTypes, delegateMethodParam)) {
          validationErrors.add(
              new SpecModelValidationError(
                  delegateMethodParam.getRepresentedObject(),
                  "Argument at index "
                      + i
                      + " is "
                      + getOptionalParamsError(delegateMethodDescription)));
        }
      }
    }

    return validationErrors;
  }

  private static int getNumberOfAllowedMethodArgsUsed(
      ImmutableList<MethodParamModel> params, ImmutableList<TypeName> types) {
    int count = 0;

    for (MethodParamModel param : params) {
      if (DelegateMethodDescription.isArgumentTypeAllowed(param, types)) {
        count++;
      }
    }

    return count;
  }

  static void validateUniqueInterStatePropNames(
      Class<? extends Annotation> interStageOutputMethodAnnotation,
      String interStageInputParamName,
      SpecModel specModel,
      List<SpecModelValidationError> validationErrors) {
    validateDuplicateName(
        interStageInputParamName,
        specModel.getProps(),
        interStageOutputMethodAnnotation,
        Prop.class,
        validationErrors);
    validateDuplicateName(
        interStageInputParamName,
        specModel.getStateValues(),
        interStageOutputMethodAnnotation,
        State.class,
        validationErrors);
    validateDuplicateName(
        interStageInputParamName,
        specModel.getInjectProps(),
        interStageOutputMethodAnnotation,
        InjectProp.class,
        validationErrors);
    validateDuplicateName(
        interStageInputParamName,
        specModel.getTreeProps(),
        interStageOutputMethodAnnotation,
        TreeProp.class,
        validationErrors);
  }

  static void validateDuplicateName(
      String fieldName,
      List<? extends MethodParamModel> propModelList,
      Class existingAnnotation,
      Class expectedAnnotation,
      List<SpecModelValidationError> validationErrors) {
    if (propModelList == null) {
      return;
    }

    for (int i = 0, size = propModelList.size(); i < size; i++) {
      final MethodParamModel model = propModelList.get(i);
      if (fieldName.equals(model.getName())) {

        validationErrors.add(
            new SpecModelValidationError(
                model.getRepresentedObject(),
                "The parameter name of @"
                    + expectedAnnotation.getSimpleName()
                    + " \""
                    + model.getName()
                    + "\" and @"
                    + existingAnnotation.getSimpleName()
                    + " \""
                    + model.getName()
                    + "\" collide!"));
      }
    }
  }

  public static List<SpecModelValidationError> validateDefinedParameterTypes(
      SpecMethodModel<DelegateMethod, Void> delegateMethod,
      Class<? extends Annotation> delegateMethodAnnotation,
      ImmutableList<TypeName> allowedArgTypes) {

    List<SpecModelValidationError> validationErrors = new ArrayList<>();

    ImmutableList<MethodParamModel> delegateArgs = delegateMethod.methodParams;

    if (allowedArgTypes.size() == 0 || delegateArgs.size() == 0) {
      return validationErrors;
    }

    final Queue<TypeName> remainingAllowedTypes = new LinkedList<>(allowedArgTypes);

    int index = 0;
    boolean canUseAllowedTypes = true;

    for (MethodParamModel delegateArg : delegateArgs) {

      // Check if the param is an allowed type
      final boolean usesAllowedType = isAllowedTypeAndConsume(delegateArg, remainingAllowedTypes);

      // Check if optional arguments can be used anymore.
      if (usesAllowedType && !canUseAllowedTypes) {
        validationErrors.add(
            new SpecModelValidationError(
                delegateArg.getRepresentedObject(),
                "Parameter at position "
                    + index
                    + " of a method annotated with "
                    + delegateMethodAnnotation
                    + " is an optional arg and should be used in the beginning. "
                    + " Allowed optional args are ["
                    + argsToString(allowedArgTypes)
                    + "]"));
        break;
      }

      if (!usesAllowedType) {
        canUseAllowedTypes = false;
      }

      if (remainingAllowedTypes.isEmpty()) {
        break;
      }

      index++;
    }

    return validationErrors;
  }

  /** Serialises a list of arguments to a comma separated string. Useful for logging. */
  private static String argsToString(ImmutableList<TypeName> args) {
    StringJoiner joiner = new StringJoiner(", ");
    for (TypeName arg : args) {
      joiner.add(arg.toString());
    }
    return joiner.toString();
  }

  private static List<SpecModelValidationError> validateReturnType(
      SpecMethodModel<DelegateMethod, Void> delegateMethod,
      Class<? extends Annotation> delegateMethodAnnotation,
      DelegateMethodDescription delegateMethodDescription) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();

    // When using Kotlin, we get incorrect return type "error.NonExistentClass". Just ignore it.
    if (!delegateMethod.returnType.equals(ClassNames.NON_EXISTENT_CLASS)
        && !delegateMethodDescription.returnType.equals(TypeName.OBJECT)
        && !delegateMethodDescription.returnType.equals(delegateMethod.returnType)) {
      validationErrors.add(
          new SpecModelValidationError(
              delegateMethod.representedObject,
              "A method annotated with @"
                  + delegateMethodAnnotation.getSimpleName()
                  + " needs to return "
                  + delegateMethodDescription.returnType
                  + ". Note that even if your return value is a subclass of "
                  + delegateMethodDescription.returnType
                  + ", you should still use "
                  + delegateMethodDescription.returnType
                  + " as the return type."));
    }
    return validationErrors;
  }

  /**
   * We consider an optional parameter as something that comes immediately after defined parameters
   * and is not a special litho parameter (like a prop, state, etc...).
   */
  private static boolean isOptionalParameter(
      MethodParamModel methodParamModel, ImmutableList<MethodParamModel> extraOptionalParameters) {
    for (MethodParamModel extraOptionalParameter : extraOptionalParameters) {
      if (methodParamModel instanceof SimpleMethodParamModel
          && methodParamModel.getTypeName().equals(extraOptionalParameter.getTypeName())
          && methodParamModel.getAnnotations().isEmpty()) {
        return true;
      }
    }

    return false;
  }

  @Nullable
  private static Annotation getInterStageInputAnnotation(
      MethodParamModel methodParamModel,
      ImmutableList<Class<? extends Annotation>> validAnnotations) {
    for (Annotation annotation : methodParamModel.getAnnotations()) {
      if (validAnnotations.contains(annotation.annotationType())) {
        return annotation;
      }
    }

    return null;
  }

  private static boolean hasMatchingInterStageOutput(
      SpecMethodModel<DelegateMethod, Void> method, MethodParamModel interStageInput) {
    for (MethodParamModel methodParam : method.methodParams) {
      if (methodParam.getName().equals(interStageInput.getName())
          && methodParam.getTypeName() instanceof ParameterizedTypeName
          && ((ParameterizedTypeName) methodParam.getTypeName()).rawType.equals(ClassNames.OUTPUT)
          && ((ParameterizedTypeName) methodParam.getTypeName()).typeArguments.size() == 1
          && ((ParameterizedTypeName) methodParam.getTypeName())
              .typeArguments
              .get(0)
              .equals(interStageInput.getTypeName().box())) {
        return true;
      }
    }

    return false;
  }

  public static boolean isOptionalParamValid(
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
        return methodParamModel instanceof PropModel;
      case DIFF_PROP:
        return methodParamModel instanceof DiffPropModel;
      case TREE_PROP:
        return MethodParamModelUtils.isAnnotatedWith(methodParamModel, TreeProp.class);
      case STATE:
        return methodParamModel instanceof StateParamModel;
      case DIFF_STATE:
        return methodParamModel instanceof DiffStateParamModel;
      case PARAM:
        return MethodParamModelUtils.isAnnotatedWith(methodParamModel, Param.class);
      case INJECT_PROP:
        return MethodParamModelUtils.isAnnotatedWith(methodParamModel, InjectProp.class);
      case INTER_STAGE_OUTPUT:
        return methodParamModel.getTypeName() instanceof ParameterizedTypeName
            && ((ParameterizedTypeName) methodParamModel.getTypeName())
                .rawType.equals(ClassNames.OUTPUT);
      case PROP_OUTPUT:
        return SpecModelUtils.isPropOutput(specModel, methodParamModel);
      case STATE_OUTPUT:
        return SpecModelUtils.isStateOutput(specModel, methodParamModel);
      case STATE_VALUE:
        return SpecModelUtils.isStateValue(specModel, methodParamModel);
      case DIFF:
        return methodParamModel instanceof RenderDataDiffModel;
      case CACHED_VALUE:
        return methodParamModel instanceof CachedValueParamModel;
    }

    return false;
  }

  public static String getOptionalParamsError(DelegateMethodDescription delegateMethodDescription) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder
        .append("not a valid parameter, should be one of the following: ")
        .append(
            getStringRepresentationOfParamTypes(delegateMethodDescription.optionalParameterTypes));

    if (!delegateMethodDescription.optionalParameters.isEmpty()) {
      stringBuilder
          .append(
              "Or one of the following, where no annotations should be added to the parameter: ")
          .append(
              getStringRepresentationOfOptionalParams(
                  delegateMethodDescription.optionalParameters));
    }

    return stringBuilder.toString();
  }

  public static String getStringRepresentationOfParamTypes(
      ImmutableList<OptionalParameterType> optionalParameterTypes) {
    final StringBuilder stringBuilder = new StringBuilder();
    for (OptionalParameterType parameterType : optionalParameterTypes) {
      stringBuilder.append(getStringRepresentationOfParamType(parameterType)).append(". ");
    }

    return stringBuilder.toString();
  }

  private static String getStringRepresentationOfOptionalParams(
      ImmutableList<MethodParamModel> optionalParameters) {
    final StringBuilder stringBuilder = new StringBuilder();
    for (MethodParamModel optionalParameter : optionalParameters) {
      stringBuilder
          .append(optionalParameter.getTypeName())
          .append(" ")
          .append(optionalParameter.getName())
          .append(". ");
    }

    return stringBuilder.toString();
  }

  private static String getStringRepresentationOfParamType(
      OptionalParameterType optionalParameterType) {
    switch (optionalParameterType) {
      case PROP:
        return "@Prop T somePropName";
      case DIFF_PROP:
        return "@Prop Diff<T> somePropName";
      case TREE_PROP:
        return "@TreeProp T someTreePropName";
      case STATE:
        return "@State T someStateName";
      case DIFF_STATE:
        return "@State Diff<T> someStateName";
      case PARAM:
        return "@Param T someParamName";
      case INJECT_PROP:
        return "@InjectProp T someInjectPropName";
      case INTER_STAGE_OUTPUT:
        return "Output<T> someOutputName";
      case PROP_OUTPUT:
        return "Output<T> propName, where a prop with type T and name propName is "
            + "declared elsewhere in the spec";
      case STATE_OUTPUT:
        return "Output<T> stateName, where a state param with type T and name stateName is "
            + "declared elsewhere in the spec";
      case STATE_VALUE:
        return "StateValue<T> stateName, where a state param with type T and name stateName is "
            + "declared elsewhere in the spec";
      case DIFF:
        return "@State Diff<T> stateName or @Prop Diff<T> propName, where stateName/propName is "
            + "a declared state or prop param declared elsewhere in the spec.";
      case CACHED_VALUE:
        return "@CachedValue T value, where the cached value has a corresponding "
            + "@OnCalculateCachedValue method";
    }

    return "Unexpected parameter type - please report to the Components team";
  }
}
