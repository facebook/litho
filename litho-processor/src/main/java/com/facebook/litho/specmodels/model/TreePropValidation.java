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

import com.facebook.litho.annotations.OnCreateTreeProp;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;

class TreePropValidation {

  static List<SpecModelValidationError> validate(SpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();

    final List<SpecMethodModel<DelegateMethod, Void>> onCreateTreePropMethods =
        SpecModelUtils.getMethodModelsWithAnnotation(specModel, OnCreateTreeProp.class);
    for (SpecMethodModel<DelegateMethod, Void> onCreateTreePropMethod : onCreateTreePropMethods) {
      if (onCreateTreePropMethod.returnType.equals(TypeName.VOID)) {
        validationErrors.add(
            new SpecModelValidationError(
                onCreateTreePropMethod.representedObject,
                "@OnCreateTreeProp methods cannot return void."));
      }

      if (onCreateTreePropMethod.returnType.isPrimitive()
          || onCreateTreePropMethod.returnType.toString().startsWith("java.lang.")
          || onCreateTreePropMethod.returnType.toString().startsWith("java.util.")) {
        validationErrors.add(
            new SpecModelValidationError(
                onCreateTreePropMethod.representedObject,
                "Returning a common JAVA class or a primitive is against the design"
                    + "of tree props, as they will be keyed on their specific types. Consider "
                    + "creating your own wrapper classes instead."));
      }

      if (onCreateTreePropMethod.methodParams.isEmpty()
          || !onCreateTreePropMethod
              .methodParams
              .get(0)
              .getTypeName()
              .equals(specModel.getContextClass())) {
        validationErrors.add(
            new SpecModelValidationError(
                onCreateTreePropMethod.representedObject,
                "The first argument of an @OnCreateTreeProp method should be "
                    + specModel.getComponentClass()
                    + "."));
      }
    }

    return validationErrors;
  }
}
