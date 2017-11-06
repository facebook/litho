/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import static com.facebook.litho.specmodels.generator.GeneratorConstants.STATE_CONTAINER_FIELD_NAME;

import static com.facebook.litho.specmodels.generator.ComponentBodyGenerator.getStateContainerClassName;
import com.facebook.litho.specmodels.model.SpecModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import javax.lang.model.element.Modifier;

/**
 * Class that generates the preamble for a Component.
 */
public class PreambleGenerator {

  private PreambleGenerator() {
  }

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    return TypeSpecDataHolder.newBuilder()
        .addTypeSpecDataHolder(generateSourceDelegate(specModel))
        .addTypeSpecDataHolder(generateConstructor(specModel))
        .build();
  }

  /**
   * Generate a delegate to the Spec that defines this component.
   */
  static TypeSpecDataHolder generateSourceDelegate(SpecModel specModel) {
    final TypeSpecDataHolder.Builder sourceDelegateBuilder = TypeSpecDataHolder.newBuilder();

    if (specModel.hasInjectedDependencies()) {
      sourceDelegateBuilder.addTypeSpecDataHolder(
          specModel.getDependencyInjectionHelper().generateSourceDelegate(specModel));
    }

    return sourceDelegateBuilder.build();
  }

  /**
   * If the spec has injected dependencies, generate a DI constructor. Otherwise, generate a
   * private constructor to enforce singleton-ity.
   */
  static TypeSpecDataHolder generateConstructor(SpecModel specModel) {
    final MethodSpec.Builder constructorBuilder =
        MethodSpec.constructorBuilder()
            .addStatement("super()");

    if (specModel.hasInjectedDependencies()) {
      final MethodSpec diConstructor =
          specModel.getDependencyInjectionHelper().generateConstructor(specModel);

      constructorBuilder
          .addAnnotations(diConstructor.annotations)
          .addCode(diConstructor.code)
          .addModifiers(diConstructor.modifiers)
          .addParameters(diConstructor.parameters);
    } else {
      constructorBuilder.addModifiers(Modifier.PRIVATE);
    }

    final boolean hasState = !specModel.getStateValues().isEmpty();
    if (hasState) {
      final ClassName stateContainerClass =
          ClassName.bestGuess(getStateContainerClassName(specModel));
      constructorBuilder.addStatement(
          STATE_CONTAINER_FIELD_NAME + " = new $T()", stateContainerClass);
    }

    return TypeSpecDataHolder.newBuilder().addMethod(constructorBuilder.build()).build();
  }
}
