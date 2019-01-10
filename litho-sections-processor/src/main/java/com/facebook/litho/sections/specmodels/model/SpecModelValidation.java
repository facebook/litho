/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.sections.specmodels.model;

import static com.facebook.litho.specmodels.model.SpecModelValidation.validateSpecModel;

import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.DelegateMethodValidation;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class SpecModelValidation {

  private static final List<String> SECTIONS_RESERVED_PROP_NAMES =
      Arrays.asList("key", "loadingEventHandler");

  public static List<SpecModelValidationError> validateGroupSectionSpecModel(
      GroupSectionSpecModel specModel, EnumSet<RunMode> runMode) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(
        validateSpecModel(specModel, SECTIONS_RESERVED_PROP_NAMES, new ArrayList<>(), runMode));
    validationErrors.addAll(
        DelegateMethodValidation.validateMethods(
            specModel,
            DelegateMethodDescriptions.getGroupSectionSpecDelegatesMap(specModel),
            Collections.unmodifiableMap(Collections.emptyMap())));
    return validationErrors;
  }

  public static List<SpecModelValidationError> validateDiffSectionSpecModel(
      DiffSectionSpecModel specModel, EnumSet<RunMode> runMode) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(
        validateSpecModel(specModel, SECTIONS_RESERVED_PROP_NAMES, new ArrayList<>(), runMode));
    validationErrors.addAll(
        DelegateMethodValidation.validateMethods(
            specModel,
            DelegateMethodDescriptions.getDiffSectionSpecDelegatesMap(specModel),
            Collections.unmodifiableMap(Collections.emptyMap())));
    return validationErrors;
  }
}
