// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for validating that a {@link SpecModel} is well-formed.
 */
public class SpecModelValidation {

  public static List<SpecModelValidationError> validateSpecModel(SpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(StateValidation.validate(specModel));
    validationErrors.addAll(EventDeclarationValidation.validate(specModel));
    return validationErrors;
  }

  public static List<SpecModelValidationError> validateLayoutSpecModel(LayoutSpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(validateSpecModel(specModel));
    validationErrors.addAll(PureRenderValidation.validate(specModel));
    validationErrors.addAll(DelegateMethodValidation.validateLayoutSpecModel(specModel));
    return validationErrors;
  }
}
