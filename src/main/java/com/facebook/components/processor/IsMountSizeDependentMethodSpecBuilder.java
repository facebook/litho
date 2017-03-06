// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

class IsMountSizeDependentMethodSpecBuilder {

  MethodSpec build() {
    return MethodSpec.methodBuilder("isMountSizeDependent")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .returns(TypeName.BOOLEAN)
        .addStatement("return true")
        .build();
  }
}
