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

import com.facebook.litho.annotations.OnCreateTreeProp;

import com.squareup.javapoet.TypeName;

class TreePropValidation {

  static List<SpecModelValidationError> validate(SpecModel specModel) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();

    final List<DelegateMethodModel> onCreateTreePropMethods =
        SpecModelUtils.getMethodModelsWithAnnotation(specModel, OnCreateTreeProp.class);
    for (DelegateMethodModel onCreateTreePropMethod : onCreateTreePropMethods) {
      if (onCreateTreePropMethod.returnType.equals(TypeName.VOID)) {
        validationErrors.add(
            new SpecModelValidationError(
                onCreateTreePropMethod.representedObject,
                "@OnCreateTreeProp methods cannot return void."));
      }

