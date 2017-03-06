// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.generator;

import com.facebook.components.specmodels.model.LayoutSpecModel;

import com.squareup.javapoet.TypeSpec;

/**
 * Class that generates a {@link com.facebook.components.annotations.LayoutSpec} from a
 * {@link LayoutSpecModel}.
 */
public class LayoutSpecGenerator {

  public static TypeSpec generate(LayoutSpecModel specModel) {
    final TypeSpec.Builder typeSpec =
        TypeSpec.classBuilder(specModel.getComponentName());

    PreambleGenerator.generate(specModel).addToTypeSpec(typeSpec);

    return typeSpec.build();
  }
}
