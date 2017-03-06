// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.generator;

import javax.lang.model.element.Modifier;

import com.facebook.components.specmodels.model.SpecModel;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

/**
 * Class that generates the canMeasure for a Component.
 */
public class CanMeasureGenerator {

  private CanMeasureGenerator() {
  }

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    if (!specModel.canMeasure()) {
      return TypeSpecDataHolder.newBuilder().build();
    }

    return TypeSpecDataHolder.newBuilder().addMethod(
        MethodSpec.methodBuilder("canMeasure")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PROTECTED)
            .returns(TypeName.BOOLEAN)
            .addStatement("return true")
            .build())
        .build();
  }
}
