/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional gr
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 */

package com.facebook.litho.specmodels.model.testing;

import static com.facebook.litho.specmodels.model.SpecModelValidation.validateSpecModel;

import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.PropValidation;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import java.util.ArrayList;
import java.util.List;

/** Class for validating that a {@link TestSpecModel} is well-formed. */
public class TestSpecModelValidation {
  public static List<SpecModelValidationError> validateTestSpecModel(TestSpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(
        validateSpecModel(specModel, PropValidation.COMMON_PROP_NAMES, RunMode.NORMAL));
    return validationErrors;
  }
}
