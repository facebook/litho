/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for validating that a {@link SpecModel} is well-formed.
 */
public class SpecModelValidation {

  public static List<SpecModelValidationError> validateSpecModel(
      SpecModel specModel, List<String> reservedPropNames) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();
    final DependencyInjectionHelper dependencyInjectionHelper =
        specModel.getDependencyInjectionHelper();
    if (specModel.hasInjectedDependencies() && dependencyInjectionHelper != null) {
      validationErrors.addAll(dependencyInjectionHelper.validate(specModel));
    }
    validationErrors.addAll(validateName(specModel));
    validationErrors.addAll(PropValidation.validate(specModel, reservedPropNames));
    validationErrors.addAll(StateValidation.validate(specModel));
    validationErrors.addAll(EventValidation.validate(specModel));
    validationErrors.addAll(TreePropValidation.validate(specModel));
    validationErrors.addAll(DiffValidation.validate(specModel));
    return validationErrors;
  }

  public static List<SpecModelValidationError> validateLayoutSpecModel(LayoutSpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(validateSpecModel(specModel, PropValidation.RESERVED_PROP_NAMES));
    validationErrors.addAll(PureRenderValidation.validate(specModel));
    validationErrors.addAll(DelegateMethodValidation.validateLayoutSpecModel(specModel));
    return validationErrors;
  }

  public static List<SpecModelValidationError> validateMountSpecModel(MountSpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(validateSpecModel(specModel, PropValidation.RESERVED_PROP_NAMES));
    validationErrors.addAll(PureRenderValidation.validate(specModel));
    validationErrors.addAll(DelegateMethodValidation.validateMountSpecModel(specModel));
    validationErrors.addAll(validateGetMountType(specModel));
    validationErrors.addAll(validateShouldUseDisplayLists(specModel));
    return validationErrors;
  }

  static List<SpecModelValidationError> validateName(SpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    if (!specModel.getSpecName().endsWith("Spec")) {
      validationErrors.add(
          new SpecModelValidationError(
              specModel.getRepresentedObject(),
              "You must suffix the class name of your spec with \"Spec\" e.g. a " +
                  "\"MyComponentSpec\" class name generates a component named " +
                  "\"MyComponent\"."));
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
              "onCreateMountContent's return type should be either a View or a Drawable " +
                  "subclass."));
    }

    return validationErrors;
  }

  static List<SpecModelValidationError> validateShouldUseDisplayLists(MountSpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();

    if (specModel.shouldUseDisplayList() &&
        specModel.getMountType() != ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_DRAWABLE) {
      validationErrors.add(
          new SpecModelValidationError(
              specModel.getRepresentedObject(),
              "shouldUseDisplayList = true can only be used on MountSpecs that mount a drawable."));
    }

    return validationErrors;
  }
}
