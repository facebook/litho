/**
 * Copyright (c) 2014-present, Facebook, Inc.
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

  public static List<SpecModelValidationError> validateSpecModel(SpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    if (specModel.hasInjectedDependencies()) {
      validationErrors.addAll(specModel.getDependencyInjectionHelper().validate(specModel));
    }
    validationErrors.addAll(validateName(specModel));
    validationErrors.addAll(PropValidation.validate(specModel));
    validationErrors.addAll(StateValidation.validate(specModel));
    validationErrors.addAll(EventValidation.validate(specModel));
    validationErrors.addAll(TreePropValidation.validate(specModel));
    return validationErrors;
  }

  public static List<SpecModelValidationError> validateLayoutSpecModel(LayoutSpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(validateSpecModel(specModel));
    validationErrors.addAll(PureRenderValidation.validate(specModel));
    validationErrors.addAll(DelegateMethodValidation.validateLayoutSpecModel(specModel));
    return validationErrors;
  }

  public static List<SpecModelValidationError> validateMountSpecModel(MountSpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(validateSpecModel(specModel));
    validationErrors.addAll(PureRenderValidation.validate(specModel));
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
}
