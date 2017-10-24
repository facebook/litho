/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import static com.facebook.litho.specmodels.model.SpecMethodModelValidation.validateMethodIsStatic;

import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.MirroredTypeException;

/**
 * Class for validating that the event declarations and event methods within a {@link SpecModel}
 * are well-formed.
 */
public class EventValidation {

  static List<SpecModelValidationError> validate(SpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    validationErrors.addAll(validateEventDeclarations(specModel));
    validationErrors.addAll(validateOnEventMethods(specModel));

    return validationErrors;
  }

  static List<SpecModelValidationError> validateEventDeclarations(SpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();

    for (EventDeclarationModel eventDeclaration : specModel.getEventDeclarations()) {
      if (eventDeclaration.returnType == null) {
        validationErrors.add(
            new SpecModelValidationError(
                eventDeclaration.representedObject,
                "Event declarations must be annotated with @Event."));
      }

      for (EventDeclarationModel.FieldModel fieldModel : eventDeclaration.fields) {
        if (!fieldModel.field.modifiers.contains(Modifier.PUBLIC) ||
                (fieldModel.field.modifiers.contains(Modifier.FINAL)
                        && !fieldModel.field.modifiers.contains(Modifier.STATIC))) {
          validationErrors.add(
              new SpecModelValidationError(
                  fieldModel.representedObject,
                  "Event fields must be declared as public non-final."));
        }
      }
    }

    return validationErrors;
  }

  static List<SpecModelValidationError> validateOnEventMethods(SpecModel specModel) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();

    final ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> eventMethods =
        specModel.getEventMethods();

    for (int i = 0, size = eventMethods.size(); i < size - 1; i++) {
      for (int j = i + 1; j < size; j++) {
        if (eventMethods.get(i).name.equals(eventMethods.get(j).name)) {
          validationErrors.add(
              new SpecModelValidationError(
                  eventMethods.get(i).representedObject,
                  "Two methods annotated with @OnEvent should not have the same name " +
                      "(" + eventMethods.get(i).name + ")."));
        }
      }
    }

    for (SpecMethodModel<EventMethod, EventDeclarationModel> eventMethod : eventMethods) {
      validationErrors.addAll(validateMethodIsStatic(specModel, eventMethod));

      if (!eventMethod.returnType.box().equals(eventMethod.typeModel.returnType.box())) {
        validationErrors.add(
            new SpecModelValidationError(
                eventMethod.representedObject,
                "Method must return "
                    + eventMethod.typeModel.returnType
                    + " since that is what "
                    + eventMethod.typeModel.name
                    + " expects."));
      }

      if (eventMethod.methodParams.isEmpty() ||
          !eventMethod.methodParams.get(0).getType().equals(specModel.getContextClass())) {
        validationErrors.add(
            new SpecModelValidationError(
                eventMethod.representedObject,
                "The first parameter for a method annotated with @OnEvent should be of type " +
                    specModel.getContextClass() + "."));
      }

      for (MethodParamModel methodParam : eventMethod.methodParams) {
        if (MethodParamModelUtils.isAnnotatedWith(methodParam, FromEvent.class)
            && !hasMatchingField(methodParam, eventMethod.typeModel.fields)) {
          validationErrors.add(
              new SpecModelValidationError(
                  methodParam.getRepresentedObject(),
                  "Param with name "
                      + methodParam.getName()
                      + " and type "
                      + methodParam.getType()
                      + " is not a member of "
                      + eventMethod.typeModel.name
                      + "."));
        }
      }
    }

    // Need some way of verifying that the return type and the @FromEvent parameters are
    // correct and valid.

    return validationErrors;
  }

  private static boolean hasMatchingField(
      MethodParamModel param,
      ImmutableList<EventDeclarationModel.FieldModel> fields) {
    for (EventDeclarationModel.FieldModel field : fields) {
      if (param.getName().equals(field.field.name)
          && (param.getType().box().equals(field.field.type.box())
              || isFromEventTypeSpecifiedInAnnotation(param, field.field.type))) {
        return true;
      }
    }

    return false;
  }

  private static boolean isFromEventTypeSpecifiedInAnnotation(
      MethodParamModel methodParamModel, TypeName eventFieldType) {
    FromEvent fromEvent =
        (FromEvent) MethodParamModelUtils.getAnnotation(methodParamModel, FromEvent.class);
    TypeName baseClassType;
    try {
      baseClassType = ClassName.get(fromEvent.baseClass());
    } catch (MirroredTypeException mte) {
      baseClassType = ClassName.get(mte.getTypeMirror());
    }
    return baseClassType.equals(eventFieldType);
  }
}
