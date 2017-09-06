/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor.specmodels.model;

import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.sections.annotations.OnDiff;
import com.facebook.litho.sections.processor.SectionClassNames;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DelegateMethodModel;
import com.facebook.litho.specmodels.model.DelegateMethodValidation;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.MethodParamModelUtils;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;

/** Class for validating that the @OnDiff delegate method in a {@link SpecModel} is well-formed. */
class OnDiffValidation {

  private OnDiffValidation() {}

  static List<SpecModelValidationError> validate(DiffSectionSpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    DelegateMethodModel delegateMethod =
        SpecModelUtils.getMethodModelWithAnnotation(specModel, OnDiff.class);
    if (delegateMethod == null) {
      validationErrors.add(
          new SpecModelValidationError(
              specModel.getRepresentedObject(),
              "All DiffSectionSpecs need to have a method annotated with @OnDiff."));
    } else {
      final ImmutableList<TypeName> definedParameterTypes =
          ImmutableList.of(SectionClassNames.SECTION_CONTEXT, SectionClassNames.CHANGESET);
      validationErrors.addAll(DelegateMethodValidation.validateStatic(specModel, delegateMethod));

      validationErrors.addAll(
          DelegateMethodValidation.validateDefinedParameterTypes(
              delegateMethod, OnDiff.class, definedParameterTypes));

      for (MethodParamModel methodParam : delegateMethod.methodParams) {
        if ((MethodParamModelUtils.isAnnotatedWith(methodParam, Prop.class)
                || MethodParamModelUtils.isAnnotatedWith(methodParam, State.class))
            && !MethodParamModelUtils.isDiffType(methodParam)) {
          validationErrors.add(
              new SpecModelValidationError(
                  methodParam.getRepresentedObject(),
                  "@Prop and @State parameters used in @OnDiff methods must be wrapped"
                      + " in a Diff<> type (i.e. Diff<"
                      + methodParam.getType()
                      + ">)."));
        }
      }
    }
    return validationErrors;
  }
}
