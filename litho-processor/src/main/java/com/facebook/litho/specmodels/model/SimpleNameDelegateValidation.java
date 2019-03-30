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

import java.util.ArrayList;
import java.util.List;

/** Class for validating that the simple name delegate contract is upheld. */
public class SimpleNameDelegateValidation {

  static List<SpecModelValidationError> validate(LayoutSpecModel specModel) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();
    final String delegate = specModel.getSimpleNameDelegate();
    if (delegate == null || delegate.isEmpty()) {
      return validationErrors;
    }

    boolean isProp = false;
    for (PropModel prop : specModel.getProps()) {
      if (prop.getName().equals(delegate)) {
        isProp = true;

        if (!ClassNames.COMPONENT.equals(prop.getTypeName())) {
          validationErrors.add(
              new SpecModelValidationError(
                  specModel.getRepresentedObject(),
                  "simpleNameDelegate on a LayoutSpec must be a prop name for an @Prop that is a Component. @Prop '"
                      + delegate
                      + "' has type "
                      + prop.getTypeName()));
        }

        break;
      }
    }

    if (!isProp) {
      validationErrors.add(
          new SpecModelValidationError(
              specModel.getRepresentedObject(),
              "simpleNameDelegate on a LayoutSpec must be a prop name for an @Prop that is a Component. Did not find a @Prop named '"
                  + delegate
                  + "'."));
    }

    return validationErrors;
  }
}
