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
import static com.facebook.litho.specmodels.generator.GeneratorConstants.STATE_TRANSITIONS_FIELD_NAME;

import com.facebook.litho.annotations.Param;
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
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.lang.model.element.Modifier;

/** Class that generates the state methods for a Component. */
public class StateGenerator {

  private static final String STATE_UPDATE_IMPL_NAME_SUFFIX = "StateUpdate";
  private static final String STATE_CONTAINER_PARAM_NAME = "_stateContainer";
  private static final String STATE_CONTAINER_NAME = "stateContainer";
  private static final String STATE_UPDATE_METHOD_NAME = "updateState";
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
        .addTypeSpecDataHolder(generateStateUpdateClasses(specModel))
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
                ComponentBodyGenerator.getStateContainerClassNameWithTypeVars(specModel),
                ComponentBodyGenerator.getStateContainerClassNameWithTypeVars(specModel))
            .addStatement(
                "$L nextStateContainer = ($L) _nextStateContainer",
                ComponentBodyGenerator.getStateContainerClassNameWithTypeVars(specModel),
                ComponentBodyGenerator.getStateContainerClassNameWithTypeVars(specModel));

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

    String stateContainerClassName = ComponentBodyGenerator.getStateContainerClassName(specModel);
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

  static TypeSpecDataHolder generateStateUpdateClasses(SpecModel specModel) {
    TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();
    for (SpecMethodModel<UpdateStateMethod, Void> updateStateMethod :
        specModel.getUpdateStateMethods()) {
      dataHolder.addTypeSpecDataHolder(
          generateStateUpdateClass(specModel, updateStateMethod, false));
    }

    if (hasUpdateStateWithTransition(specModel)) {
      for (SpecMethodModel<UpdateStateMethod, Void> updateStateWithTransitionMethod :
          specModel.getUpdateStateWithTransitionMethods()) {
        dataHolder.addTypeSpecDataHolder(
            generateStateUpdateClass(specModel, updateStateWithTransitionMethod, true));
      }
    }

    return dataHolder.build();
  }

  static boolean hasUpdateStateWithTransition(SpecModel specModel) {
    return specModel.getUpdateStateWithTransitionMethods() != null
        && !specModel.getUpdateStateWithTransitionMethods().isEmpty();
  }

  static TypeSpecDataHolder generateOnStateUpdateMethods(SpecModel specModel) {
    TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();
    for (SpecMethodModel<UpdateStateMethod, Void> updateStateMethod :
        specModel.getUpdateStateMethods()) {
      dataHolder.addTypeSpecDataHolder(
          generateOnStateUpdateMethod(specModel, updateStateMethod, StateUpdateType.DEFAULT));
      dataHolder.addTypeSpecDataHolder(
          generateOnStateUpdateMethod(specModel, updateStateMethod, StateUpdateType.ASYNC));
      dataHolder.addTypeSpecDataHolder(
          generateOnStateUpdateMethod(specModel, updateStateMethod, StateUpdateType.SYNC));
    }

    if (hasUpdateStateWithTransition(specModel)) {
      for (SpecMethodModel<UpdateStateMethod, Void> updateStateWithTransitionMethod :
          specModel.getUpdateStateWithTransitionMethods()) {
        dataHolder.addTypeSpecDataHolder(
            generateOnStateUpdateMethod(
                specModel, updateStateWithTransitionMethod, StateUpdateType.WITH_TRANSITION));
      }
    }

    return dataHolder.build();
  }

  static TypeSpecDataHolder generateOnStateUpdateMethod(
      SpecModel specModel,
      SpecMethodModel<UpdateStateMethod, Void> updateStateMethod,
      StateUpdateType stateUpdateType) {

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
    final String componentName = specModel.getComponentName();
    codeBlockBuilder.add(
        "$N.$N _stateUpdate = (($N) _component).$N(",
        componentName,
        getStateUpdateClassName(updateStateMethod),
        componentName,
        "create" + getStateUpdateClassName(updateStateMethod));

    boolean isFirstParam = true;
    for (MethodParamModel methodParam : updateStateMethod.methodParams) {
      if (MethodParamModelUtils.isAnnotatedWith(methodParam, Param.class)) {
        if (!isFirstParam) {
          codeBlockBuilder.add(", ");
        } else {
          isFirstParam = false;
        }
        builder.addParameter(methodParam.getTypeName(), methodParam.getName());
        builder.addTypeVariables(MethodParamModelUtils.getTypeVariables(methodParam));
        codeBlockBuilder.add(methodParam.getName());
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

  static TypeSpecDataHolder generateStateUpdateClass(
      SpecModel specModel,
      SpecMethodModel<UpdateStateMethod, Void> updateStateMethod,
      boolean withTransition) {

    final Map<String, TypeVariableName> types = new HashMap<>();

    final TypeSpec.Builder stateUpdateClassBuilder =
        TypeSpec.classBuilder(getStateUpdateClassName(updateStateMethod))
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addSuperinterface(specModel.getUpdateStateInterface());

    for (TypeVariableName t : specModel.getTypeVariables()) {
      types.put(t.name, t);
    }

    MethodSpec.Builder updateStateMethodBuilder =
        MethodSpec.methodBuilder(STATE_UPDATE_METHOD_NAME)
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(specModel.getStateContainerClass(), STATE_CONTAINER_PARAM_NAME)
            .addStatement(
                "$L $L = ($L) $L",
                ComponentBodyGenerator.getStateContainerClassNameWithTypeVars(specModel),
                STATE_CONTAINER_NAME,
                ComponentBodyGenerator.getStateContainerClassNameWithTypeVars(specModel),
                STATE_CONTAINER_PARAM_NAME);

    // Add constructor and member fields.
    MethodSpec.Builder constructor = MethodSpec.constructorBuilder();
    for (MethodParamModel methodParam : updateStateMethod.methodParams) {
      if (MethodParamModelUtils.isAnnotatedWith(methodParam, Param.class)) {
        stateUpdateClassBuilder.addField(
            methodParam.getTypeName(),
            getMemberName(methodParam),
            Modifier.PRIVATE);
        constructor
            .addParameter(methodParam.getTypeName(), methodParam.getName())
            .addStatement("$L = $L", getMemberName(methodParam), methodParam.getName());

        if (!specModel.hasInjectedDependencies()) {
          for (TypeVariableName t : MethodParamModelUtils.getTypeVariables(methodParam)) {
            types.put(t.name, t);
          }
        }
      } else {
        // Must be a StateValue<>.
        updateStateMethodBuilder
            .addStatement(
                "$T $L = new $T()",
                methodParam.getTypeName(),
                methodParam.getName(),
                methodParam.getTypeName())
            .addStatement(
                "$L.set($L.$L)",
                methodParam.getName(),
                STATE_CONTAINER_NAME,
                methodParam.getName());
      }
    }

    final String transitionLocalVarName = "transition";

    if (withTransition) {
      // Call the spec's update method and add transition to statecontainer's transition list.
      updateStateMethodBuilder.addStatement(
          "$T $N = $N.$N($L)",
          ClassNames.TRANSITION,
          transitionLocalVarName,
          SpecModelUtils.getSpecAccessor(specModel),
          updateStateMethod.name,
          getParamsForSpecUpdateMethodCall(updateStateMethod));

      updateStateMethodBuilder.addCode(
          CodeBlock.builder()
              .beginControlFlow("if ($L != null)", transitionLocalVarName)
              .addStatement(
                  "$N.$N.add($L)",
                  STATE_CONTAINER_NAME,
                  STATE_TRANSITIONS_FIELD_NAME,
                  transitionLocalVarName)
              .endControlFlow()
              .build());
    } else {
      // Call the spec's update method.
      updateStateMethodBuilder.addStatement(
          "$N.$N($L)",
          SpecModelUtils.getSpecAccessor(specModel),
          updateStateMethod.name,
          getParamsForSpecUpdateMethodCall(updateStateMethod));
    }

    // Set the new value of the state.
    for (MethodParamModel methodParamModel : updateStateMethod.methodParams) {
      if (!MethodParamModelUtils.isAnnotatedWith(methodParamModel, Param.class)) {
        updateStateMethodBuilder.addStatement(
            "$L.$L = $L.get()",
            STATE_CONTAINER_NAME,
            methodParamModel.getName(),
            methodParamModel.getName());
      }
    }

    stateUpdateClassBuilder.addTypeVariables(types.values());

    return TypeSpecDataHolder.newBuilder()
        .addType(stateUpdateClassBuilder
            .addMethod(constructor.build())
            .addMethod(updateStateMethodBuilder.build())
            .build())
        .build();
  }

  static TypeSpecDataHolder generateLazyStateUpdateMethods(SpecModel specModel) {
    TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();
    for (StateParamModel stateValue : specModel.getStateValues()) {
      if (stateValue.canUpdateLazily()) {
        dataHolder.addTypeSpecDataHolder(generateLazyStateUpdateMethod(specModel, stateValue));
      }
    }

    return dataHolder.build();
  }

  static TypeSpecDataHolder generateLazyStateUpdateMethod(
      SpecModel specModel,
      StateParamModel stateValue) {
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

    final MethodSpec.Builder stateUpdate =
        MethodSpec.methodBuilder(STATE_UPDATE_METHOD_NAME)
            .addAnnotation(Override.class)
            .addParameter(specModel.getStateContainerClass(), STATE_CONTAINER_PARAM_NAME)
            .addModifiers(Modifier.PUBLIC)
            .addStatement(
                "$L $L = ($L) $L",
                ComponentBodyGenerator.getStateContainerClassName(specModel),
                STATE_CONTAINER_NAME,
                ComponentBodyGenerator.getStateContainerClassName(specModel),
                STATE_CONTAINER_PARAM_NAME)
            .addStatement(
                "$L.$L = $L",
                STATE_CONTAINER_NAME,
                stateValue.getName(),
                LAZY_STATE_UPDATE_VALUE_PARAM);

    final TypeSpec.Builder stateBuilderImpl =
        TypeSpec.anonymousClassBuilder("")
            .addSuperinterface(specModel.getUpdateStateInterface())
            .addMethod(stateUpdate.build());

    builder.addStatement(
        "$T _stateUpdate = $L",
        specModel.getUpdateStateInterface(),
        stateBuilderImpl.build());

    builder.addStatement("c.updateStateLazy(_stateUpdate)");

    return TypeSpecDataHolder.newBuilder().addMethod(builder.build()).build();
  }

  private static String getStateUpdateClassName(
      SpecMethodModel<UpdateStateMethod, Void> updateMethod) {
    String methodName = updateMethod.name.toString();
    return methodName.substring(0, 1).toUpperCase(Locale.ROOT) +
        methodName.substring(1) +
        STATE_UPDATE_IMPL_NAME_SUFFIX;
  }

  private static String getMemberName(MethodParamModel methodParamModel) {
    return "m" + methodParamModel.getName().substring(0, 1).toUpperCase() +
        methodParamModel.getName().substring(1);
  }

  private static String getParamsForSpecUpdateMethodCall(
      SpecMethodModel<UpdateStateMethod, Void> updateStateMethod) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0, size = updateStateMethod.methodParams.size(); i < size; i++) {
      MethodParamModel methodParam = updateStateMethod.methodParams.get(i);
      if (MethodParamModelUtils.isAnnotatedWith(methodParam, Param.class)) {
        sb.append(getMemberName(methodParam));
      } else {
        sb.append(methodParam.getName());
      }

      if (i < size - 1) {
        sb.append(',');
      }
    }

    return sb.toString();
  }
}
