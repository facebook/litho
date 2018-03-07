/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import android.support.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;

public class TagValidation {

  @VisibleForTesting
  static final String NON_EMPTY_ERROR_MESSAGE =
      "Spec classes use interfaces as component tags. Tags cannot be non-empty interfaces.";

  @VisibleForTesting
  static final String EXTEND_INTERFACE_ERROR_MESSAGE =
      "Spec classes use interfaces as component tags. Tags cannot extend other interfaces.";

  static List<SpecModelValidationError> validate(SpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();

    for (TagModel tag : specModel.getTags()) {
      if (tag.hasMethods) {
        validationErrors.add(
            new SpecModelValidationError(tag.representedObject, NON_EMPTY_ERROR_MESSAGE));
      }
      if (tag.hasSupertype) {
        validationErrors.add(
            new SpecModelValidationError(tag.representedObject, EXTEND_INTERFACE_ERROR_MESSAGE));
      }
    }

    return validationErrors;
  }
}
