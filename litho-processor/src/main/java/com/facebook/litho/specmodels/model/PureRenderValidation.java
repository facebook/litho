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

package com.facebook.litho.specmodels.model;

import com.facebook.litho.annotations.ShouldUpdate;
import java.util.ArrayList;
import java.util.List;

public class PureRenderValidation {

  static <S extends SpecModel & HasPureRender> List<SpecModelValidationError> validate(
      S specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    final SpecMethodModel<DelegateMethod, Void> shouldUpdateMethod =
        SpecModelUtils.getMethodModelWithAnnotation(specModel, ShouldUpdate.class);

    if (shouldUpdateMethod != null) {
      if (!specModel.isPureRender()) {
        validationErrors.add(
            new SpecModelValidationError(
                shouldUpdateMethod.representedObject,
                "Specs defining a method annotated with @ShouldUpdate should also set "
                    + "isPureRender = true in the top-level spec annotation."));
      }
    }

    return validationErrors;
  }
}
