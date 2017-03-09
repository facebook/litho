// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import com.facebook.components.annotations.OnCreateTreeProp;

import com.squareup.javapoet.TypeName;

class TreePropValidation {

  static List<SpecModelValidationError> validate(SpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();

    final List<DelegateMethodModel> onCreateTreePropMethods =
        SpecModelUtils.getMethodModelsWithAnnotation(specModel, OnCreateTreeProp.class);
    for (DelegateMethodModel onCreateTreePropMethod : onCreateTreePropMethods) {
      for (Annotation annotation : onCreateTreePropMethod.annotations) {
        if (annotation.annotationType().equals(OnCreateTreeProp.class) &&
            ((OnCreateTreeProp) annotation).name().isEmpty()) {
          validationErrors.add(
              new SpecModelValidationError(
                  onCreateTreePropMethod.representedObject,
                  "@OnCreateTreeProp must define a valid name."));
        }
      }

      if (onCreateTreePropMethod.returnType.equals(TypeName.VOID)) {
        validationErrors.add(
            new SpecModelValidationError(
                onCreateTreePropMethod.representedObject,
                "@OnCreateTreeProp methods cannot return void."));
      }

      if (onCreateTreePropMethod.methodParams.isEmpty() ||
          !onCreateTreePropMethod.methodParams.get(0).getType()
              .equals(specModel.getContextClass())) {
        validationErrors.add(
            new SpecModelValidationError(
                onCreateTreePropMethod.representedObject,
                "The first argument of an @OnCreateTreeProp method should be " +
                    specModel.getComponentClass() + "."));
      }
    }

    return validationErrors;
  }
}
