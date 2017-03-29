/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import javax.lang.model.element.Modifier;

import java.util.ArrayList;
import java.util.List;

import com.facebook.common.internal.ImmutableList;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;

import static com.facebook.litho.specmodels.model.ClassNames.STATE_VALUE;

/**
 * Class for validating that the state models within a  {@link SpecModel} are well-formed.
 */
public class StateValidation {

  static List<SpecModelValidationError> validate(SpecModel specModel) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(validateStateValues(specModel));
    validationErrors.addAll(validateOnUpdateStateMethods(specModel));

    return validationErrors;
  }

  static List<SpecModelValidationError> validateStateValues(SpecModel specModel) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();
    final ImmutableList<StateParamModel> stateValues = specModel.getStateValues();

    for (int i = 0, size = stateValues.size(); i < size - 1; i++) {
      final StateParamModel thisStateValue  = stateValues.get(i);
      for (int j = i + 1; j < size; j++) {
        final StateParamModel thatStateValue = stateValues.get(j);

        if (thisStateValue.getName().equals(thatStateValue.getName())) {
          if (!thisStateValue.getType().equals(thatStateValue.getType())) {
            validationErrors.add(new SpecModelValidationError(
                thatStateValue.getRepresentedObject(),
                "State values with the same name must have the same type."));
          }

          if (thisStateValue.canUpdateLazily() != thatStateValue.canUpdateLazily()) {
            validationErrors.add(new SpecModelValidationError(
                thatStateValue.getRepresentedObject(),
                "State values with the same name must have the same annotated value for " +
                    "canUpdateLazily()."));
          }
        }
      }
    }

    return validationErrors;
  }

  static List<SpecModelValidationError> validateOnUpdateStateMethods(SpecModel specModel) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();
    for (UpdateStateMethodModel updateStateMethodModel : specModel.getUpdateStateMethods()) {
      validationErrors.addAll(validateOnUpdateStateMethod(specModel, updateStateMethodModel));
    }

    return validationErrors;
  }

  /**
   * Validate that the declaration of a method annotated with {@link OnUpdateState} is correct:
   * <ul>
   *   <li>1. Method parameters annotated with {@link Param} don't have the same name as parameters
   *    annotated with {@link State} or {@link Prop}.</li>
   *   <li>2. Method parameters not annotated with {@link Param} must be of type
   *    com.facebook.litho.StateValue.</li>
