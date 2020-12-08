/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.specmodels.generator;

import com.facebook.litho.specmodels.model.SpecModel;
import com.squareup.javapoet.MethodSpec;
import javax.lang.model.element.Modifier;

/** Class that generates the preamble for a Component. */
public class PreambleGenerator {

  private PreambleGenerator() {}

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    return TypeSpecDataHolder.newBuilder()
        .addTypeSpecDataHolder(generateConstructor(specModel))
        .build();
  }

  /**
   * If the spec has injected dependencies, generate a DI constructor. Otherwise, generate a private
   * constructor to enforce singleton-ity.
   */
  static TypeSpecDataHolder generateConstructor(SpecModel specModel) {
    final MethodSpec.Builder constructorBuilder =
        MethodSpec.constructorBuilder().addStatement("super($S)", specModel.getComponentName());

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

    return TypeSpecDataHolder.newBuilder().addMethod(constructorBuilder.build()).build();
  }
}
