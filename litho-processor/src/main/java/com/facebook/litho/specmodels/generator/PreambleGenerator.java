/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import javax.lang.model.element.Modifier;

import com.facebook.litho.specmodels.model.SpecModel;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import static com.facebook.litho.specmodels.generator.GeneratorConstants.DELEGATE_FIELD_NAME;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.SPEC_INSTANCE_NAME;

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
        .addTypeSpecDataHolder(generateGetter(specModel))
        .build();
  }

  /**
   * Generate a delegate to the Spec that defines this component.
   */
  static TypeSpecDataHolder generateSourceDelegate(SpecModel specModel) {
    final TypeName delegateTypeName =
        specModel.hasInjectedDependencies() ?
            specModel.getDependencyInjectionHelper().getSourceDelegateTypeName(specModel) :
            specModel.getSpecTypeName();

    final FieldSpec.Builder builder =
        FieldSpec.builder(delegateTypeName, DELEGATE_FIELD_NAME)
            .addModifiers(Modifier.PRIVATE);

    if (!specModel.hasInjectedDependencies()) {
      builder.initializer("new $T()", specModel.getSpecTypeName());
    }

    return TypeSpecDataHolder.newBuilder().addField(builder.build()).build();
  }

  /**
   * If the spec has injected dependencies, generate a DI constructor. Otherwise, generate a
   * private constructor to enforce singleton-ity.
   */
  static TypeSpecDataHolder generateConstructor(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    if (specModel.hasInjectedDependencies()) {
      typeSpecDataHolder.addMethod(
          specModel.getDependencyInjectionHelper().generateConstructor(specModel));
    } else {
      typeSpecDataHolder.addMethod(
          MethodSpec.constructorBuilder()
              .addModifiers(Modifier.PRIVATE)
              .build());
    }

    return typeSpecDataHolder.build();
  }

  /**
   * Generate a method for this component which either lazily instantiates a singleton reference or
   * returns this depending upon whether this spec injects dependencies or not.
   */
  static TypeSpecDataHolder generateGetter(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    if (!specModel.hasInjectedDependencies()) {
      typeSpecDataHolder.addField(
          FieldSpec
              .builder(
                  specModel.getComponentTypeName(),
                  SPEC_INSTANCE_NAME,
                  Modifier.PRIVATE,
                  Modifier.STATIC)
              .initializer("null")
              .build());

      typeSpecDataHolder.addMethod(
          MethodSpec.methodBuilder("get")
              .addModifiers(Modifier.PUBLIC)
              .addModifiers(Modifier.STATIC)
              .addModifiers(Modifier.SYNCHRONIZED)
              .returns(specModel.getComponentTypeName())
              .beginControlFlow("if ($L == null)", SPEC_INSTANCE_NAME)
              .addStatement("$L = new $T()", SPEC_INSTANCE_NAME, specModel.getComponentTypeName())
              .endControlFlow()
              .addStatement("return $L", SPEC_INSTANCE_NAME)
              .build());
    } else {
      typeSpecDataHolder.addMethod(
          MethodSpec.methodBuilder("get")
              .addModifiers(Modifier.PUBLIC)
              .returns(specModel.getComponentTypeName())
              .addStatement("return this")
              .build());
    }

    return typeSpecDataHolder.build();
  }
}
