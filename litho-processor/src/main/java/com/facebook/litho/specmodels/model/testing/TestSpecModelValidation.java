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
        validateSpecModel(
            specModel,
            PropValidation.COMMON_PROP_NAMES,
            PropValidation.VALID_COMMON_PROPS,
            RunMode.normal()));
    return validationErrors;
  }
}
