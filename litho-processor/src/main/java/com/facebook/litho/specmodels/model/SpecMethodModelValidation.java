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

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;

/**
 * Class for validating that the {@link SpecMethodModel}s for a {@link SpecModel} are well-formed.
 */
public final class SpecMethodModelValidation {
  private SpecMethodModelValidation() {}

  /**
   * Validate that the given {@link SpecMethodModel} is static.
   *
   * @param specModel SpecModel containing the SpecMethodModel
   * @param methodModel The method to analyze.
   * @return List of new errors, possibly empty.
   */
  public static List<SpecModelValidationError> validateMethodIsStatic(
      SpecModel specModel, SpecMethodModel<?, ?> methodModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    if (specModel.getSpecElementType() == SpecElementType.JAVA_CLASS
        && !methodModel.modifiers.contains(Modifier.STATIC)) {
      validationErrors.add(
          new SpecModelValidationError(
              methodModel.representedObject, "Methods in a spec must be static."));
    }
    return validationErrors;
  }

  /**
   * Validate that a method adheres to the component method naming rules.
   *
   * @param methodModel The method to analyze.
   * @return List of new errors, possibly empty.
   */
  public static List<SpecModelValidationError> validateMethodName(
      SpecMethodModel<?, ?> methodModel) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>(1);

    if (methodModel.name.toString().startsWith("__")) {
      validationErrors.add(
          new SpecModelValidationError(
              methodModel.representedObject,
              String.format(
                  "Methods in a component must not start with '__' as they are "
                      + "reserved for internal use. Method '%s' violates this contract.",
                  methodModel.name)));
    }
    return validationErrors;
  }
}
