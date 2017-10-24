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
import javax.lang.model.element.Modifier;

/**
 * Class for validating that the {@link SpecMethodModel}s for a {@link SpecModel} are well-formed.
 */
public final class SpecMethodModelValidation {
  private SpecMethodModelValidation() {}

  /**
   * Validate that the given {@link SpecMethodModel} is either static or statically accessible (i.e.
   * part of a well-known singleton structure).
   *
   * @param specModel SpecModel containing the SpecMethodModel
   * @param methodModel The method to analyze.
   * @return List of new errors, possibly empty.
   */
  public static List<SpecModelValidationError> validateMethodIsStatic(
      SpecModel specModel, SpecMethodModel<?, ?> methodModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    if (!specModel.hasInjectedDependencies()
        && specModel.getSpecElementType() == SpecElementType.JAVA_CLASS
        && !methodModel.modifiers.contains(Modifier.STATIC)) {
      validationErrors.add(
          new SpecModelValidationError(
              methodModel.representedObject,
              "Methods in a spec that doesn't have dependency injection must be static."));
    }
    return validationErrors;
  }
}
