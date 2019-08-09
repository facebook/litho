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
import static com.facebook.litho.specmodels.generator.StateGenerator.FLAG_LAZY;
import static com.facebook.litho.specmodels.generator.StateGenerator.hasUpdateStateWithTransition;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.litho.annotations.Comparable;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.MethodParamModelUtils;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.facebook.litho.specmodels.model.StateParamModel;
import com.facebook.litho.specmodels.model.UpdateStateMethod;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.LinkedList;
import java.util.List;
import javax.lang.model.element.Modifier;

public class StateContainerGenerator {
  private static final String METHOD_NAME_CONSUME_TRANSITION = "consumeTransition";
  private static final String METHOD_NAME_APPLY_STATE_UPDATE = "applyStateUpdate";
  private static final String PARAM_NAME_STATE_UPDATE = "stateUpdate";
  private static final String VAR_NAME_PARAMS = "params";

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
      generateTransitionStuff(specModel).addToTypeSpec(stateContainerClassBuilder);
    }

    generateApplyStateUpdateMethod(specModel).addToTypeSpec(stateContainerClassBuilder);

    return stateContainerClassBuilder.build();
  }

  private static TypeSpecDataHolder generateTransitionStuff(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();

    final TypeName transitionClass = specModel.getTransitionClass().box();

    typeSpecDataHolder.addField(
        FieldSpec.builder(transitionClass, GeneratorConstants.STATE_TRANSITION_FIELD_NAME).build());

    final String transitionCopyVarName = "transitionCopy";
    typeSpecDataHolder.addMethod(
        MethodSpec.methodBuilder(METHOD_NAME_CONSUME_TRANSITION)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .addAnnotation(Nullable.class)
            .returns(transitionClass)
            .addStatement(
                "$T $N = $N",
                specModel.getTransitionClass(),
                transitionCopyVarName,
                GeneratorConstants.STATE_TRANSITION_FIELD_NAME)
            .addStatement("$N = null", GeneratorConstants.STATE_TRANSITION_FIELD_NAME)
            .addStatement("return $N", transitionCopyVarName)
            .build());

    return typeSpecDataHolder.build();
  }

  private static TypeSpecDataHolder generateApplyStateUpdateMethod(SpecModel specModel) {
    final boolean hasUpdateStateWithTransition = hasUpdateStateWithTransition(specModel);
    final MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(METHOD_NAME_APPLY_STATE_UPDATE)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .addParameter(ClassNames.COMPONENT_STATE_UPDATE, PARAM_NAME_STATE_UPDATE)
            .returns(TypeName.VOID);

    for (StateParamModel stateValue : specModel.getStateValues()) {
      final TypeName stateValueTypeName =
          ParameterizedTypeName.get(ClassNames.STATE_VALUE, stateValue.getTypeName().box());
      methodBuilder.addStatement("$T $L", stateValueTypeName, stateValue.getName());
    }
    methodBuilder.addCode("\n");

    methodBuilder.addStatement(
        "final $T $L = $L.$L",
        ArrayTypeName.of(TypeName.OBJECT),
        VAR_NAME_PARAMS,
        PARAM_NAME_STATE_UPDATE,
        "params");

    methodBuilder.beginControlFlow("switch ($L.$L)", PARAM_NAME_STATE_UPDATE, "type");
    int methodIndex = 0;
    for (SpecMethodModel<UpdateStateMethod, Void> updateStateMethod :
        specModel.getUpdateStateMethods()) {
      if (methodIndex > 0) {
        methodBuilder.addCode("\n");
      }

      methodBuilder.addCode(
          generateStateUpdateDelegatingCall(specModel, methodIndex++, updateStateMethod, false));
    }
    if (hasUpdateStateWithTransition) {
      if (methodIndex > 0) {
        methodBuilder.addCode("\n");
      }

      for (SpecMethodModel<UpdateStateMethod, Void> updateStateMethod :
          specModel.getUpdateStateWithTransitionMethods()) {
        methodBuilder.addCode(
            generateStateUpdateDelegatingCall(specModel, methodIndex++, updateStateMethod, true));
      }
    }
    if (hasStatesThatCanUpdateLazily(specModel)) {
      int stateIndex = 0;
      for (StateParamModel stateValue : specModel.getStateValues()) {
        if (stateValue.canUpdateLazily()) {
          if (methodIndex > 0 || stateIndex > 0) {
            methodBuilder.addCode("\n");
          }

          methodBuilder.addCode(generateApplyingLazyStateUpdateCode(stateIndex++, stateValue));
        }
      }
    }
    methodBuilder.endControlFlow();

    return TypeSpecDataHolder.newBuilder().addMethod(methodBuilder.build()).build();
  }

  static CodeBlock generateStateUpdateDelegatingCall(
      SpecModel specModel,
      int methodIndex,
      SpecMethodModel<UpdateStateMethod, Void> updateStateMethod,
      boolean withTransition) {
    final CodeBlock.Builder codeBlock = CodeBlock.builder();

    codeBlock.add("case $L:\n$>", methodIndex);

    final StringBuilder format = new StringBuilder();
    final List<Object> args = new LinkedList<>();
    if (withTransition) {
      format.append("$L = ");
      args.add(GeneratorConstants.STATE_TRANSITION_FIELD_NAME);
    }
    format.append("$N.$N(");
    args.add(SpecModelUtils.getSpecAccessor(specModel));
    args.add(updateStateMethod.name);

    int paramIndex = 0;
    for (int i = 0; i < updateStateMethod.methodParams.size(); i++) {
      if (i > 0) {
        format.append(", ");
      }

      final MethodParamModel methodParam = updateStateMethod.methodParams.get(i);
      if (MethodParamModelUtils.isAnnotatedWith(methodParam, Param.class)) {
        final TypeName paramTypeName = methodParam.getTypeName();
        if (!paramTypeName.equals(TypeName.OBJECT)) {
          format.append("($T) ");
          args.add(paramTypeName);
        }

        format.append("$L[$L]");
        args.add(VAR_NAME_PARAMS);
        args.add(paramIndex++);
      } else {
        // Must be a StateValue<.>
        final String name = methodParam.getName();
        codeBlock
            .addStatement("$L = new $T()", name, methodParam.getTypeName())
            .addStatement("$L.set(this.$L)", name, name);

        format.append("$L");
        args.add(name);
      }
    }

    format.append(')');

    codeBlock.addStatement(format.toString(), args.toArray());

    for (int i = 0; i < updateStateMethod.methodParams.size(); i++) {
      final MethodParamModel methodParam = updateStateMethod.methodParams.get(i);
      if (MethodParamModelUtils.isAnnotatedWith(methodParam, Param.class)) {
        continue;
      }
      final String name = methodParam.getName();
      codeBlock.addStatement("this.$L = $L.get()", name, name);
    }

    codeBlock.addStatement("break$<");

    return codeBlock.build();
  }

  static CodeBlock generateApplyingLazyStateUpdateCode(int index, StateParamModel stateValue) {
    final int stateUpdateType = FLAG_LAZY | index;

    final String stateName = stateValue.getName();
    final TypeName stateType = stateValue.getTypeName();

    return CodeBlock.builder()
        .add("case $L:\n$>", stateUpdateType)
        .addStatement("this.$L = ($T) $L[0]", stateName, stateType, VAR_NAME_PARAMS)
        .addStatement("break$<")
        .build();
  }

  static String getStateContainerClassName(SpecModel specModel) {
    if (specModel.getStateValues().isEmpty()) {
      return specModel.getStateContainerClass().toString();
    } else {
      return specModel.getComponentName() + GeneratorConstants.STATE_CONTAINER_NAME_SUFFIX;
    }
  }

  private static boolean hasStatesThatCanUpdateLazily(SpecModel specModel) {
    for (StateParamModel stateValue : specModel.getStateValues()) {
      if (stateValue.canUpdateLazily()) {
        return true;
      }
    }
    return false;
  }
}
