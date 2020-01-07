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

import static com.facebook.litho.specmodels.model.SpecMethodModelValidation.validateMethodIsStatic;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Class for validating that the event declarations and event methods within a {@link SpecModel} are
 * well-formed.
 */
public class TriggerValidation {

  static List<SpecModelValidationError> validate(SpecModel specModel, EnumSet<RunMode> runMode) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(validateOnTriggerMethods(specModel, runMode));

    return validationErrors;
  }

  static List<SpecModelValidationError> validateOnTriggerMethods(
      SpecModel specModel, EnumSet<RunMode> runMode) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();

    final ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> triggerMethods =
        specModel.getTriggerMethods();

    for (int i = 0, size = triggerMethods.size(); i < size - 1; i++) {
      for (int j = i + 1; j < size; j++) {
        if (triggerMethods.get(i).name.equals(triggerMethods.get(j).name)) {
          validationErrors.add(
              new SpecModelValidationError(
                  triggerMethods.get(i).representedObject,
                  "Two methods annotated with @OnTrigger should not have the same name "
                      + "("
                      + triggerMethods.get(i).name
                      + ")."));
        }
      }
    }

    if (!runMode.contains(RunMode.ABI)) {
      for (SpecMethodModel<EventMethod, EventDeclarationModel> triggerMethod : triggerMethods) {
        validationErrors.addAll(validateMethodIsStatic(specModel, triggerMethod));

        TypeName returnType =
            triggerMethod.returnType instanceof ParameterizedTypeName
                ? ((ParameterizedTypeName) triggerMethod.returnType).rawType
                : triggerMethod.returnType;

        if (!returnType.equals(triggerMethod.typeModel.returnType)) {
          validationErrors.add(
              new SpecModelValidationError(
                  triggerMethod.representedObject,
                  "Method must return "
                      + triggerMethod.typeModel.returnType
                      + " since that is what "
                      + triggerMethod.typeModel.name
                      + " expects."));
        }

        if (triggerMethod.methodParams.isEmpty()
            || !triggerMethod
                .methodParams
                .get(0)
                .getTypeName()
                .equals(specModel.getContextClass())) {
          validationErrors.add(
              new SpecModelValidationError(
                  triggerMethod.representedObject,
                  "The first parameter for a method annotated with @OnTrigger should be of type "
                      + specModel.getContextClass()
                      + "."));
        }
      }
    }

    return validationErrors;
  }
}
