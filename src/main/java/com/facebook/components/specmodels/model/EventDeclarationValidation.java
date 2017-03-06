// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import javax.lang.model.element.Modifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for validating that the event declarations within a {@link SpecModel} are well-formed.
 */
public class EventDeclarationValidation {

  static List<SpecModelValidationError> validate(SpecModel specModel) {
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
            fieldModel.field.modifiers.contains(Modifier.FINAL)) {
          validationErrors.add(
              new SpecModelValidationError(
                  fieldModel.representedObject,
                  "Event fields must be declared as public non-final."));
        }
      }
    }

    return validationErrors;
  }
}
