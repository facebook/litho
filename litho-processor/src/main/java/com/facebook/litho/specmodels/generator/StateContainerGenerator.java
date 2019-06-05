/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.specmodels.generator;

import static com.facebook.litho.specmodels.generator.ComponentBodyGenerator.getComparableType;
import static com.facebook.litho.specmodels.generator.StateGenerator.hasUpdateStateWithTransition;

import androidx.annotation.VisibleForTesting;
import com.facebook.litho.annotations.Comparable;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.StateParamModel;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

public class StateContainerGenerator {
  private static final String METHOD_NAME_CONSUME_TRANSITIONS = "consumeTransitions";

  static TypeSpec generate(SpecModel specModel) {
    final TypeSpec.Builder stateContainerClassBuilder =
        TypeSpec.classBuilder(getStateContainerClassName(specModel))
            .superclass(specModel.getStateContainerClass())
            .addAnnotation(
                AnnotationSpec.builder(VisibleForTesting.class)
                    .addMember("otherwise", "$L", VisibleForTesting.PRIVATE)
                    .build())
            .addModifiers(Modifier.STATIC)
            .addTypeVariables(specModel.getTypeVariables());

    final boolean hasUpdateStateWithTransition = hasUpdateStateWithTransition(specModel);
    if (hasUpdateStateWithTransition) {
      stateContainerClassBuilder.addSuperinterface(specModel.getTransitionContainerClass());
    }

    for (StateParamModel stateValue : specModel.getStateValues()) {
      stateContainerClassBuilder.addField(
          FieldSpec.builder(stateValue.getTypeName(), stateValue.getName())
              .addAnnotation(State.class)
              .addAnnotation(
                  AnnotationSpec.builder(Comparable.class)
                      .addMember("type", "$L", getComparableType(specModel, stateValue))
                      .build())
              .build());
    }

    if (hasUpdateStateWithTransition) {
      generateTransitionStaff(specModel).addToTypeSpec(stateContainerClassBuilder);
    }

    return stateContainerClassBuilder.build();
  }

  private static TypeSpecDataHolder generateTransitionStaff(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();

    final TypeName transitionClass = specModel.getTransitionClass().box();

    typeSpecDataHolder.addField(
        FieldSpec.builder(
                ParameterizedTypeName.get(ClassNames.LIST, transitionClass),
                GeneratorConstants.STATE_TRANSITIONS_FIELD_NAME)
            .initializer("new $T<>()", ClassNames.ARRAY_LIST)
            .build());

    final String transitionsCopyVarName = "transitionsCopy";
    typeSpecDataHolder.addMethod(
        MethodSpec.methodBuilder(METHOD_NAME_CONSUME_TRANSITIONS)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(ParameterizedTypeName.get(ClassNames.LIST, transitionClass))
            .addCode(
                CodeBlock.builder()
                    .beginControlFlow(
                        "if ($L.isEmpty())", GeneratorConstants.STATE_TRANSITIONS_FIELD_NAME)
                    .addStatement("return $T.EMPTY_LIST", ClassNames.COLLECTIONS)
                    .endControlFlow()
                    .build())
            .addStatement(
                "$T<$T> $N",
                ClassNames.LIST,
                specModel.getTransitionClass(),
                transitionsCopyVarName)
            .beginControlFlow("synchronized ($L)", GeneratorConstants.STATE_TRANSITIONS_FIELD_NAME)
            .addStatement(
                "$N = new $T<>($N)",
                transitionsCopyVarName,
                ClassNames.ARRAY_LIST,
                GeneratorConstants.STATE_TRANSITIONS_FIELD_NAME)
            .addStatement("$N.clear()", GeneratorConstants.STATE_TRANSITIONS_FIELD_NAME)
            .endControlFlow()
            .addStatement("return $N", transitionsCopyVarName)
            .build());

    return typeSpecDataHolder.build();
  }

  static String getStateContainerClassName(SpecModel specModel) {
    if (specModel.getStateValues().isEmpty()) {
      return specModel.getStateContainerClass().toString();
    } else {
      return specModel.getComponentName() + GeneratorConstants.STATE_CONTAINER_NAME_SUFFIX;
    }
  }
}
