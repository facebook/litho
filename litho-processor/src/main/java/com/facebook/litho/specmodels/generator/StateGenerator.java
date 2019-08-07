/*
 * Copyright 2014-present Facebook, Inc.
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

import static com.facebook.litho.specmodels.generator.GeneratorConstants.STATE_CONTAINER_FIELD_NAME;
import static com.facebook.litho.specmodels.generator.StateContainerGenerator.getStateContainerClassName;

import com.facebook.litho.annotations.Param;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.MethodParamModelUtils;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.facebook.litho.specmodels.model.StateParamModel;
import com.facebook.litho.specmodels.model.UpdateStateMethod;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.util.Locale;
import javax.lang.model.element.Modifier;

/** Class that generates the state methods for a Component. */
public class StateGenerator {
  static final int FLAG_LAZY = 1 << 31;

  private static final String STATE_UPDATE_IMPL_NAME_SUFFIX = "StateUpdate";
  private static final String STATE_CONTAINER_NAME = "_stateContainer";
  private static final String LAZY_STATE_UPDATE_VALUE_PARAM = "lazyUpdateValue";


  private enum StateUpdateType {
    DEFAULT,
    SYNC,
    ASYNC,
    WITH_TRANSITION,
  }

  private StateGenerator() {}

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    return TypeSpecDataHolder.newBuilder()
        .addTypeSpecDataHolder(generateHasState(specModel))
        .addTypeSpecDataHolder(generateTransferState(specModel))
        .addTypeSpecDataHolder(generateGetStateContainerWithLazyStateUpdatesApplied(specModel))
        .addTypeSpecDataHolder(generateOnStateUpdateMethods(specModel))
        .addTypeSpecDataHolder(generateLazyStateUpdateMethods(specModel))
        .build();
  }

  static TypeSpecDataHolder generateHasState(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();

    if (specModel.shouldGenerateHasState() && !specModel.getStateValues().isEmpty()) {
      typeSpecDataHolder.addMethod(
          MethodSpec.methodBuilder("hasState")
              .addAnnotation(Override.class)
              .addModifiers(Modifier.PROTECTED)
              .returns(TypeName.BOOLEAN)
              .addStatement("return true")
              .build());
    }

    return typeSpecDataHolder.build();
  }

  static TypeSpecDataHolder generateTransferState(SpecModel specModel) {
    if (specModel.getStateValues().isEmpty()) {
      return TypeSpecDataHolder.newBuilder().build();
    }

    final String stateContainerClassNameWithTypeVars =
        getStateContainerClassNameWithTypeVars(specModel);
    MethodSpec.Builder methodSpec =
        MethodSpec.methodBuilder("transferState")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PROTECTED)
            .addParameter(
                ParameterSpec.builder(specModel.getStateContainerClass(), "_prevStateContainer")
                    .build())
            .addParameter(
                ParameterSpec.builder(specModel.getStateContainerClass(), "_nextStateContainer")
                    .build())
            .addStatement(
                "$L prevStateContainer = ($L) _prevStateContainer",
                stateContainerClassNameWithTypeVars,
                stateContainerClassNameWithTypeVars)
            .addStatement(
                "$L nextStateContainer = ($L) _nextStateContainer",
                stateContainerClassNameWithTypeVars,
                stateContainerClassNameWithTypeVars);

    for (StateParamModel stateValue : specModel.getStateValues()) {
      methodSpec.addStatement(
          "nextStateContainer.$L = prevStateContainer.$L",
          stateValue.getName(),
          stateValue.getName());
    }

    if (hasUpdateStateWithTransition(specModel)) {
      methodSpec
          .beginControlFlow(
              "synchronized (prevStateContainer.$L)",
              GeneratorConstants.STATE_TRANSITIONS_FIELD_NAME)
          .addStatement(
              "nextStateContainer.$L = new ArrayList<>(prevStateContainer.$L)",
              GeneratorConstants.STATE_TRANSITIONS_FIELD_NAME,
              GeneratorConstants.STATE_TRANSITIONS_FIELD_NAME)
          .endControlFlow();
    }

    return TypeSpecDataHolder.newBuilder().addMethod(methodSpec.build()).build();
  }

  static TypeSpecDataHolder generateGetStateContainerWithLazyStateUpdatesApplied(
      SpecModel specModel) {
    // Currently we limit Lazy State provisioning only to @OnEvent callbacks.
    if (!SpecModelUtils.hasLazyState(specModel) || specModel.getEventMethods().size() == 0) {
      return TypeSpecDataHolder.newBuilder().build();
    }

    final String stateContainerClassName = getStateContainerClassName(specModel);
    MethodSpec methodSpec =
        MethodSpec.methodBuilder("getStateContainerWithLazyStateUpdatesApplied")
            .addModifiers(Modifier.PRIVATE)
            .returns(ClassName.bestGuess(stateContainerClassName))
            .addParameter(ParameterSpec.builder(specModel.getContextClass(), "c").build())
            .addParameter(
                ParameterSpec.builder(specModel.getComponentTypeName(), "component").build())
            .addStatement(
                "$L $L = new $L()",
                stateContainerClassName,
                STATE_CONTAINER_NAME,
                stateContainerClassName)
            .addStatement(
                "transferState(component.$L, $L)", STATE_CONTAINER_FIELD_NAME, STATE_CONTAINER_NAME)
            .addStatement("c.applyLazyStateUpdatesForContainer($L)", STATE_CONTAINER_NAME)
            .addStatement("return $L", STATE_CONTAINER_NAME)
            .build();

    return TypeSpecDataHolder.newBuilder().addMethod(methodSpec).build();
  }

  static boolean hasUpdateStateWithTransition(SpecModel specModel) {
    return specModel.getUpdateStateWithTransitionMethods() != null
        && !specModel.getUpdateStateWithTransitionMethods().isEmpty();
  }

  static TypeSpecDataHolder generateOnStateUpdateMethods(SpecModel specModel) {
    TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();
    int index = 0;
    for (SpecMethodModel<UpdateStateMethod, Void> updateStateMethod :
        specModel.getUpdateStateMethods()) {
      dataHolder.addTypeSpecDataHolder(
          generateOnStateUpdateMethod(
              specModel, updateStateMethod, StateUpdateType.DEFAULT, index));
      dataHolder.addTypeSpecDataHolder(
          generateOnStateUpdateMethod(specModel, updateStateMethod, StateUpdateType.ASYNC, index));
      dataHolder.addTypeSpecDataHolder(
          generateOnStateUpdateMethod(specModel, updateStateMethod, StateUpdateType.SYNC, index));

      index++;
    }

    if (hasUpdateStateWithTransition(specModel)) {
      for (SpecMethodModel<UpdateStateMethod, Void> updateStateWithTransitionMethod :
          specModel.getUpdateStateWithTransitionMethods()) {
        dataHolder.addTypeSpecDataHolder(
            generateOnStateUpdateMethod(
                specModel,
                updateStateWithTransitionMethod,
                StateUpdateType.WITH_TRANSITION,
                index));

        index++;
      }
    }

    return dataHolder.build();
  }

  static TypeSpecDataHolder generateOnStateUpdateMethod(
      SpecModel specModel,
      SpecMethodModel<UpdateStateMethod, Void> updateStateMethod,
      StateUpdateType stateUpdateType,
      int index) {

    final String name;
    switch (stateUpdateType) {
      case DEFAULT:
        name = updateStateMethod.name.toString();
        break;
      case ASYNC:
        name = updateStateMethod.name.toString() + "Async";
        break;
      case SYNC:
        name = updateStateMethod.name.toString() + "Sync";
        break;
      case WITH_TRANSITION:
        name = updateStateMethod.name.toString() + "WithTransition";
        break;
      default:
        throw new RuntimeException("Unhandled state update type: " + stateUpdateType);
    }

    final MethodSpec.Builder builder = MethodSpec.methodBuilder(name)
        .addModifiers(Modifier.PROTECTED, Modifier.STATIC)
        .addParameter(specModel.getContextClass(), "c");

    builder.addStatement(
        "$T _component = c.get$LScope()",
        specModel.getComponentClass(),
        specModel.getComponentClass().simpleName())
        .addCode(
            CodeBlock.builder()
                .beginControlFlow("if (_component == null)")
                .addStatement("return")
                .endControlFlow()
                .build());

    final CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
    codeBlockBuilder.add(
        "$T _stateUpdate = new $T($L",
        ClassNames.COMPONENT_STATE_UPDATE,
        ClassNames.COMPONENT_STATE_UPDATE,
        index);
    for (MethodParamModel methodParam : updateStateMethod.methodParams) {
      if (MethodParamModelUtils.isAnnotatedWith(methodParam, Param.class)) {
        final String paramName = methodParam.getName();

        builder
            .addParameter(methodParam.getTypeName(), paramName)
            .addTypeVariables(MethodParamModelUtils.getTypeVariables(methodParam));

        codeBlockBuilder.add(", ").add(paramName);
      }
    }
    codeBlockBuilder.add(");\n");
    builder.addCode(codeBlockBuilder.build());


    final String stateUpdateAttribution =
        '"' + specModel.getComponentName() + "." + updateStateMethod.name.toString() + '"';
    final String stateUpdateMethod;
    switch (stateUpdateType) {
      case SYNC:
        stateUpdateMethod = "updateStateSync";
        break;
      case DEFAULT:
      case ASYNC:
        stateUpdateMethod = "updateStateAsync";
        break;
      case WITH_TRANSITION:
        stateUpdateMethod = "updateStateWithTransition";
        break;
      default:
        throw new RuntimeException("Unhandled state update type: " + stateUpdateType);
    }
    builder.addStatement(
        "c." + stateUpdateMethod + "(_stateUpdate, " + stateUpdateAttribution + ")");

    return TypeSpecDataHolder.newBuilder().addMethod(builder.build()).build();
  }

  static TypeSpecDataHolder generateLazyStateUpdateMethods(SpecModel specModel) {
    TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();
    int index = 0;
    for (StateParamModel stateValue : specModel.getStateValues()) {
      if (stateValue.canUpdateLazily()) {
        dataHolder.addTypeSpecDataHolder(
            generateLazyStateUpdateMethod(specModel, stateValue, index++));
      }
    }

    return dataHolder.build();
  }

  static TypeSpecDataHolder generateLazyStateUpdateMethod(
      SpecModel specModel, StateParamModel stateValue, int index) {
    final MethodSpec.Builder builder =
        MethodSpec.methodBuilder(
                "lazyUpdate"
                    + stateValue.getName().substring(0, 1).toUpperCase(Locale.ROOT)
                    + stateValue.getName().substring(1))
            .addModifiers(Modifier.PROTECTED, Modifier.STATIC)
            .addParameter(specModel.getContextClass(), "c")
            .addTypeVariables(MethodParamModelUtils.getTypeVariables(stateValue))
            .addParameter(stateValue.getTypeName(), LAZY_STATE_UPDATE_VALUE_PARAM, Modifier.FINAL);

    builder.addStatement(
        "$T _component = c.get$LScope()",
        specModel.getComponentClass(),
        specModel.getComponentClass().simpleName())
        .addCode(
            CodeBlock.builder()
                .beginControlFlow("if (_component == null)")
                .addStatement("return")
                .endControlFlow()
                .build());

    final int type = FLAG_LAZY | index;
    builder.addStatement(
        "$T _stateUpdate = new $T($L, $L)",
        ClassNames.COMPONENT_STATE_UPDATE,
        ClassNames.COMPONENT_STATE_UPDATE,
        type,
        LAZY_STATE_UPDATE_VALUE_PARAM);

    builder.addStatement("c.updateStateLazy(_stateUpdate)");

    return TypeSpecDataHolder.newBuilder().addMethod(builder.build()).build();
  }

  private static String getStateContainerClassNameWithTypeVars(SpecModel specModel) {
    if (specModel.getStateValues().isEmpty()) {
      return specModel.getStateContainerClass().toString();
    }

    final StringBuilder stringBuilder = new StringBuilder();

    final ImmutableList<TypeVariableName> typeVariables = specModel.getTypeVariables();
    if (!typeVariables.isEmpty()) {
      stringBuilder.append("<");
      for (int i = 0, size = typeVariables.size(); i < size - 1; i++) {
        stringBuilder.append(typeVariables.get(i).name).append(", ");
      }

      stringBuilder.append(typeVariables.get(typeVariables.size() - 1)).append(">");
    }

    return getStateContainerClassName(specModel) + stringBuilder;
  }
}
