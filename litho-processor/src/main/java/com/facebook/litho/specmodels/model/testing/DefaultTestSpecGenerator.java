/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model.testing;

import com.facebook.litho.specmodels.generator.JavadocGenerator;
import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.facebook.litho.specmodels.generator.testing.MatcherGenerator;
import com.facebook.litho.specmodels.model.SpecModel;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

public class DefaultTestSpecGenerator implements TestSpecGenerator {

  @Override
  public TypeSpec generate(SpecModel specModel) {
    final TypeSpec.Builder typeSpec =
        TypeSpec.classBuilder(specModel.getComponentName())
            .addTypeVariables(specModel.getTypeVariables())
            .addSuperinterface(specModel.getSpecTypeName())
            .addModifiers(Modifier.FINAL);

    if (specModel.isPublic()) {
      typeSpec.addModifiers(Modifier.PUBLIC);
    }

    TypeSpecDataHolder.newBuilder()
        .addTypeSpecDataHolder(JavadocGenerator.generate(specModel))
        .addTypeSpecDataHolder(MatcherGenerator.generate(specModel))
        .build()
        .addToTypeSpec(typeSpec);

    return typeSpec.build();
  }
}
