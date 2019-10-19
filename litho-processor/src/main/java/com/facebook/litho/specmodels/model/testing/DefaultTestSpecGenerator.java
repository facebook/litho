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

package com.facebook.litho.specmodels.model.testing;

import com.facebook.litho.specmodels.generator.JavadocGenerator;
import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.facebook.litho.specmodels.generator.testing.MatcherGenerator;
import com.facebook.litho.specmodels.model.HasEnclosedSpecModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

public class DefaultTestSpecGenerator implements TestSpecGenerator {

  @Override
  public <T extends SpecModel & HasEnclosedSpecModel> TypeSpec generate(T specModel) {
    final TypeSpec.Builder typeSpec =
        TypeSpec.classBuilder(specModel.getComponentName())
            .addSuperinterface(specModel.getSpecTypeName())
            .addModifiers(Modifier.FINAL);

    if (!specModel.getTypeVariables().isEmpty()) {
      typeSpec.addTypeVariables(specModel.getTypeVariables());
    }

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
