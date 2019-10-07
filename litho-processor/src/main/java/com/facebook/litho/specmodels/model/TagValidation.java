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

import androidx.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;

public class TagValidation {

  @VisibleForTesting
  static final String NON_EMPTY_ERROR_MESSAGE =
      "%s: Spec classes use interfaces as component tags. Tags cannot be non-empty interfaces like '%s'.";

  @VisibleForTesting
  static final String EXTEND_INTERFACE_ERROR_MESSAGE =
      "%s: Spec classes use interfaces as component tags. Tags cannot extend other interfaces like '%s'.";

  static List<SpecModelValidationError> validate(SpecModel specModel) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();

    for (TagModel tag : specModel.getTags()) {
      if (tag.hasMethods) {
        validationErrors.add(
            new SpecModelValidationError(
                tag.representedObject,
                String.format(NON_EMPTY_ERROR_MESSAGE, specModel.getSpecName(), tag.name)));
      }
      if (tag.hasSupertype) {
        validationErrors.add(
            new SpecModelValidationError(
                tag.representedObject,
                String.format(EXTEND_INTERFACE_ERROR_MESSAGE, specModel.getSpecName(), tag.name)));
      }
    }

    return validationErrors;
  }
}
