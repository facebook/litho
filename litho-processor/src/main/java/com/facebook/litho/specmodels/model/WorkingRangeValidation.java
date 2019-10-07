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
import java.util.Objects;
import javax.annotation.Nullable;

class WorkingRangeValidation {

  private WorkingRangeValidation() {}

  static List<SpecModelValidationError> validate(SpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();

    SpecModelValidationError noRegisterMethod = validateNoRegisterMethod(specModel);
    if (noRegisterMethod != null) {
      validationErrors.add(noRegisterMethod);
    }

    validationErrors.addAll(validateEmptyName(specModel));
    validationErrors.addAll(validateDuplicateName(specModel));

    return validationErrors;
  }

  @Nullable
  static SpecModelValidationError validateNoRegisterMethod(SpecModel specModel) {
    if (specModel.getWorkingRangeRegisterMethod() == null
        && !specModel.getWorkingRangeMethods().isEmpty()) {
      return new SpecModelValidationError(
          specModel.getRepresentedObject(),
          "You need to have a method annotated with @OnRegisterRanges in your spec "
              + "since there are @OnEnteredRange/@OnExitedRange annotated methods.");
    }
    return null;
  }

  static List<SpecModelValidationError> validateEmptyName(SpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();

    for (WorkingRangeMethodModel methodModel : specModel.getWorkingRangeMethods()) {
      if (methodModel.enteredRangeModel != null
          && methodModel.enteredRangeModel.typeModel != null) {
        WorkingRangeDeclarationModel declarationModel = methodModel.enteredRangeModel.typeModel;
        String nameInAnnotation = declarationModel.name;
        if (nameInAnnotation == null || nameInAnnotation.isEmpty()) {
          validationErrors.add(
              new SpecModelValidationError(
                  methodModel.enteredRangeModel.representedObject,
                  declarationModel.representedObject,
                  "The name in @OnEnteredRange cannot be empty."));
        }
      }
      if (methodModel.exitedRangeModel != null && methodModel.exitedRangeModel.typeModel != null) {
        WorkingRangeDeclarationModel declarationModel = methodModel.exitedRangeModel.typeModel;
        String nameInAnnotation = declarationModel.name;
        if (nameInAnnotation == null || nameInAnnotation.isEmpty()) {
          validationErrors.add(
              new SpecModelValidationError(
                  methodModel.exitedRangeModel.representedObject,
                  declarationModel.representedObject,
                  "The name in @OnExitedRange cannot be empty."));
        }
      }
    }
    return validationErrors;
  }

  static List<SpecModelValidationError> validateDuplicateName(SpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();

    for (WorkingRangeMethodModel methodModel : specModel.getWorkingRangeMethods()) {
      if (methodModel.enteredRangeModel != null
          && methodModel.enteredRangeModel.typeModel != null) {
        boolean isDuplicated =
            specModel.getWorkingRangeMethods().stream()
                    .filter(
                        it ->
                            it.enteredRangeModel != null && it.enteredRangeModel.typeModel != null)
                    .filter(
                        it ->
                            Objects.equals(
                                it.enteredRangeModel.typeModel.name,
                                methodModel.enteredRangeModel.typeModel.name))
                    .count()
                > 1;
        if (isDuplicated) {
          validationErrors.add(
              new SpecModelValidationError(
                  methodModel.enteredRangeModel.representedObject,
                  methodModel.enteredRangeModel.typeModel.representedObject,
                  "The name \""
                      + methodModel.name
                      + "\" is duplicated, it's must be unique across @OnEnteredRange methods."));
        }
      }

      if (methodModel.exitedRangeModel != null && methodModel.exitedRangeModel.typeModel != null) {
        boolean isDuplicated =
            specModel.getWorkingRangeMethods().stream()
                    .filter(
                        it -> it.exitedRangeModel != null && it.exitedRangeModel.typeModel != null)
                    .filter(
                        it ->
                            Objects.equals(
                                it.exitedRangeModel.typeModel.name,
                                methodModel.exitedRangeModel.typeModel.name))
                    .count()
                > 1;
        if (isDuplicated) {
          validationErrors.add(
              new SpecModelValidationError(
                  methodModel.exitedRangeModel.representedObject,
                  methodModel.exitedRangeModel.typeModel.representedObject,
                  "The name \""
                      + methodModel.name
                      + "\" is duplicated, it's must be unique across @OnExitedRange methods."));
        }
      }
    }
    return validationErrors;
  }
}
