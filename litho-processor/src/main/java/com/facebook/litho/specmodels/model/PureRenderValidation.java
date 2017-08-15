/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.facebook.litho.annotations.ShouldUpdate;
import java.util.ArrayList;
import java.util.List;

public class PureRenderValidation {

  static <S extends SpecModel & HasPureRender> List<SpecModelValidationError> validate(
      S specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    final DelegateMethodModel shouldUpdateMethod =
        SpecModelUtils.getMethodModelWithAnnotation(specModel, ShouldUpdate.class);

    if (shouldUpdateMethod != null) {
      if (!specModel.isPureRender()) {
        validationErrors.add(
            new SpecModelValidationError(
                shouldUpdateMethod.representedObject,
                "Specs defining a method annotated with @ShouldUpdate should also set " +
                    "isPureRender = true in the top-level spec annotation."));
      }
    }

    return validationErrors;
  }
}
