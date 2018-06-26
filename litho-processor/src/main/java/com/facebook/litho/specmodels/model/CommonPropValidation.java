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

import com.facebook.litho.annotations.CommonProp;
import com.facebook.litho.specmodels.internal.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.lang.model.element.Modifier;

/** Class for validating that the common props within a {@link SpecModel} are well-formed. */
public class CommonPropValidation {

  private static final List<Modifier> REQUIRED_COMMON_PROPS_MODIFIERS =
      Arrays.asList(Modifier.PROTECTED, Modifier.STATIC, Modifier.FINAL);

  static List<SpecModelValidationError> validate(
      SpecModel specModel, List<PropValidation.CommonPropModel> permittedCommonProps) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();

    validationErrors.addAll(validateCommonProps(specModel, permittedCommonProps));
    validationErrors.addAll(validateCommonPropDefaults(specModel));

    return validationErrors;
  }

  static List<SpecModelValidationError> validateCommonProps(
      SpecModel specModel, List<PropValidation.CommonPropModel> permittedCommonProps) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();

    final ImmutableList<SpecMethodModel<DelegateMethod, Void>> delegateMethods =
        specModel.getDelegateMethods();

    for (SpecMethodModel<DelegateMethod, Void> delegateMethod : delegateMethods) {
      for (MethodParamModel methodParamModel : delegateMethod.methodParams) {
        if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, CommonProp.class)) {
          boolean validName = false;
          for (PropValidation.CommonPropModel commonPropModel : permittedCommonProps) {
            if (commonPropModel.name.equals(methodParamModel.getName())) {
              validName = true;
              if (!commonPropModel.type.equals(methodParamModel.getTypeName())) {
                validationErrors.add(
                    new SpecModelValidationError(
                        methodParamModel.getRepresentedObject(),
                        "A common prop with name "
                            + commonPropModel.name
                            + " must have type of: "
                            + commonPropModel.type));
              }
            }
          }

          if (!validName) {
            validationErrors.add(
                new SpecModelValidationError(
                    methodParamModel.getRepresentedObject(),
                    "Common prop with name "
                        + methodParamModel.getName()
                        + " is incorrectly defined - see CommonPropValidation.java for a "
                        + "list of common props that may be used."));
          }
        }
      }
    }

    return validationErrors;
  }

  static List<SpecModelValidationError> validateCommonPropDefaults(SpecModel specModel) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();

    final ImmutableList<CommonPropDefaultModel> commonPropDefaults =
        specModel.getCommonPropDefaults();

    for (CommonPropDefaultModel commonPropDefault : commonPropDefaults) {
      boolean validName = false;
      for (PropValidation.CommonPropModel commonPropModel : PropValidation.VALID_COMMON_PROPS) {
        if (commonPropModel.name.equals(commonPropDefault.mName)) {
          validName = true;
          if (!commonPropModel.type.equals(commonPropDefault.mType)) {
            validationErrors.add(
                new SpecModelValidationError(
                    commonPropDefault.mRepresentedObject,
                    "A common prop default with name "
                        + commonPropModel.name
                        + " must have type of: "
                        + commonPropModel.type));
          }
        }
      }

      if (!validName) {
        validationErrors.add(
            new SpecModelValidationError(
                commonPropDefault.mRepresentedObject,
                "Common prop default with name "
                    + commonPropDefault.mName
                    + " is incorrectly defined - see CommonPropValidation.java for a "
                    + "list of common prop defaults that may be used."));
      }

      if (!(commonPropDefault.mModifiers.containsAll(REQUIRED_COMMON_PROPS_MODIFIERS))) {
        validationErrors.add(
            new SpecModelValidationError(
                commonPropDefault.mRepresentedObject,
                "Common prop defaults must be defined as protected, static and final"));
      }
    }

    return validationErrors;
  }

}
