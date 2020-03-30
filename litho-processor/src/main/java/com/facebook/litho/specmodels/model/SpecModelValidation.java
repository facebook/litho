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

import com.facebook.litho.specmodels.internal.RunMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Class for validating that a {@link SpecModel} is well-formed. */
public class SpecModelValidation {

  public static List<SpecModelValidationError> validateSpecModel(
      SpecModel specModel,
      List<String> reservedPropNames,
      List<PropValidation.CommonPropModel> permittedCommonProps,
      EnumSet<RunMode> runMode) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();
    final DependencyInjectionHelper dependencyInjectionHelper =
        specModel.getDependencyInjectionHelper();
    if (specModel.hasInjectedDependencies() && dependencyInjectionHelper != null) {
      validationErrors.addAll(dependencyInjectionHelper.validate(specModel));
    }
    validationErrors.addAll(validateName(specModel));
    validationErrors.addAll(
        PropValidation.validate(specModel, reservedPropNames, permittedCommonProps, runMode));
    validationErrors.addAll(StateValidation.validate(specModel));
    validationErrors.addAll(EventValidation.validate(specModel, runMode));
    validationErrors.addAll(TriggerValidation.validate(specModel, runMode));
    validationErrors.addAll(TreePropValidation.validate(specModel));
    validationErrors.addAll(DiffValidation.validate(specModel));
    validationErrors.addAll(TagValidation.validate(specModel));
    validationErrors.addAll(WorkingRangeValidation.validate(specModel));
    validationErrors.addAll(CachedValueValidation.validate(specModel));
    validationErrors.addAll(FieldsValidation.validate(specModel));
    validationErrors.addAll(DynamicPropsValidation.validate(specModel));
    return validationErrors;
  }

  public static List<SpecModelValidationError> validateLayoutSpecModel(
      LayoutSpecModel specModel, EnumSet<RunMode> runMode) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(PureRenderValidation.validate(specModel));
    validationErrors.addAll(DelegateMethodValidation.validateLayoutSpecModel(specModel));
    validationErrors.addAll(SimpleNameDelegateValidation.validate(specModel));
    validationErrors.addAll(
        validateSpecModel(
            specModel,
            PropValidation.COMMON_PROP_NAMES,
            PropValidation.VALID_COMMON_PROPS,
            runMode));
    return validationErrors;
  }

  public static List<SpecModelValidationError> validateMountSpecModel(
      MountSpecModel specModel, EnumSet<RunMode> runMode) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(PureRenderValidation.validate(specModel));
    validationErrors.addAll(DelegateMethodValidation.validateMountSpecModel(specModel));
    validationErrors.addAll(validateGetMountType(specModel));
    validationErrors.addAll(
        validateSpecModel(
            specModel,
            PropValidation.COMMON_PROP_NAMES,
            PropValidation.VALID_COMMON_PROPS,
            runMode));
    return validationErrors;
  }

  static List<SpecModelValidationError> validateName(SpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    if (!specModel.getSpecName().endsWith("Spec")) {
      validationErrors.add(
          new SpecModelValidationError(
              specModel.getRepresentedObject(),
              "You must suffix the class name of your spec with \"Spec\" e.g. a "
                  + "\"MyComponentSpec\" class name generates a component named "
                  + "\"MyComponent\"."));
    }

    return validationErrors;
  }

  static List<SpecModelValidationError> validateGetMountType(MountSpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();

    if (!specModel.getMountType().equals(ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_DRAWABLE)
        && !specModel.getMountType().equals(ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_VIEW)) {
      validationErrors.add(
          new SpecModelValidationError(
              specModel.getRepresentedObject(),
              "onCreateMountContent's return type should be either a View or a Drawable "
                  + "subclass."));
    }

    return validationErrors;
  }
}
