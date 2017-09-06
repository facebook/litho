/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor.specmodels.model;

import static com.facebook.litho.specmodels.model.SpecModelValidation.validateSpecModel;

import com.facebook.litho.specmodels.model.SpecModelValidationError;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpecModelValidation {

  private static final List<String> SECTIONS_RESERVED_PROP_NAMES =
      Arrays.asList("key", "loadingEventHandler");

  public static List<SpecModelValidationError> validateGroupSectionSpecModel(
      GroupSectionSpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(validateSpecModel(specModel, SECTIONS_RESERVED_PROP_NAMES));
    return validationErrors;
  }

  public static List<SpecModelValidationError> validateDiffSectionSpecModel(
      DiffSectionSpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(OnDiffValidation.validate(specModel));
    if (validationErrors.isEmpty()) {
      validationErrors.addAll(validateSpecModel(specModel, SECTIONS_RESERVED_PROP_NAMES));
    }
    return validationErrors;
  }
}
