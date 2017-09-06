/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import javax.lang.model.element.Modifier;

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
