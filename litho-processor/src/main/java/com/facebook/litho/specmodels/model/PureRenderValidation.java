/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import java.util.ArrayList;
import java.util.List;

import com.facebook.litho.annotations.ShouldUpdate;

import com.squareup.javapoet.ParameterizedTypeName;

/**
 * Class for validating that the pure render methods within a {@link SpecModel} are well-formed.
 */
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

      for (MethodParamModel methodParam : shouldUpdateMethod.methodParams) {
        final PropModel prop = SpecModelUtils.getPropWithName(specModel, methodParam.getName());

        if (prop == null) {
          validationErrors.add(
              new SpecModelValidationError(
                  methodParam.getRepresentedObject(),
                  "Names of parameters for a method annotated with @ShouldUpdate should match a " +
                      "declared Prop of the same name."));
          continue;
        }

        if (!(methodParam.getType() instanceof ParameterizedTypeName) ||
            !((ParameterizedTypeName) methodParam.getType()).rawType.equals(ClassNames.DIFF) ||
            ((ParameterizedTypeName) methodParam.getType()).typeArguments.size() != 1 ||
            !((ParameterizedTypeName) methodParam.getType()).typeArguments.get(0)
                .equals(prop.getType().box())) {
          validationErrors.add(
              new SpecModelValidationError(
                  methodParam.getRepresentedObject(),
                  "Types of parameters for a method annotated with @ShouldUpdate should be " +
                      "Diff<T>, where T is the type of the declared Prop of the same name."));
        }
      }
    }

    return validationErrors;
  }
}
