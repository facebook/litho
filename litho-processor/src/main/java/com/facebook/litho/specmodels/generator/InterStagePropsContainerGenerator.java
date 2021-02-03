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

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.InterStageInputParamModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

public class InterStagePropsContainerGenerator {

  static TypeSpec generate(SpecModel specModel) {
    final TypeSpec.Builder interStagePropsContainerBuilder =
        TypeSpec.classBuilder(getInterStagePropsContainerClassName(specModel))
            .addSuperinterface(ClassNames.INTER_STAGE_PROPS_CONTAINER)
            .addModifiers(Modifier.STATIC);

    final ImmutableList<InterStageInputParamModel> interStageInputs =
        specModel.getInterStageInputs();
    for (InterStageInputParamModel interStageInput : interStageInputs) {
      interStagePropsContainerBuilder.addField(
          FieldSpec.builder(interStageInput.getTypeName().box(), interStageInput.getName())
              .build());
    }

    return interStagePropsContainerBuilder.build();
  }

  static String getInterStagePropsContainerClassName(SpecModel specModel) {
    if (specModel.getInterStageInputs() == null || specModel.getInterStageInputs().isEmpty()) {
      return "InterStagePropsContainer";
    } else {
      return specModel.getComponentName() + "InterStagePropsContainer";
    }
  }

  private static TypeSpecDataHolder generateInterStageInputs(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    final ImmutableList<InterStageInputParamModel> interStageInputs =
        specModel.getInterStageInputs();

    for (InterStageInputParamModel interStageInput : interStageInputs) {
      typeSpecDataHolder.addField(
          FieldSpec.builder(interStageInput.getTypeName().box(), interStageInput.getName())
              .build());
    }

    return typeSpecDataHolder.build();
  }
}
