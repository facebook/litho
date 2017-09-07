/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import static com.facebook.litho.specmodels.generator.ComponentImplGenerator.getImplAccessor;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.ABSTRACT_IMPL_PARAM_NAME;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.IMPL_VARIABLE_NAME;
import static com.facebook.litho.specmodels.model.ClassNames.EVENT_TRIGGER;
import static com.facebook.litho.specmodels.model.ClassNames.OBJECT;

import com.facebook.litho.annotations.FromTrigger;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethodModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.MethodParamModelUtils;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import javax.lang.model.element.Modifier;

/** Class that generates the trigger methods for a Component. */
public class TriggerGenerator {

  private TriggerGenerator() {}

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    final TypeSpecDataHolder.Builder builder =
        TypeSpecDataHolder.newBuilder()
            .addTypeSpecDataHolder(generateOnTriggerMethodDelegates(specModel))
            .addTypeSpecDataHolder(generateStaticTriggerMethods(specModel));

    if (!specModel.getTriggerMethods().isEmpty()) {
      builder.addMethod(overrideCanReceiveTriggerMethod());
      builder.addMethod(generateAcceptTriggerEvent(specModel));
    }

    return builder.build();
  }

  static MethodSpec overrideCanReceiveTriggerMethod() {
    return MethodSpec.methodBuilder("canAcceptTrigger")
        .addModifiers(Modifier.PROTECTED)
        .addAnnotation(Override.class)
        .returns(TypeName.BOOLEAN)
        .addStatement("return true")
        .build();
  }

  /** Generate acceptTriggerEvent() implementation for the component. */
  static MethodSpec generateAcceptTriggerEvent(SpecModel specModel) {
    final MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder("acceptTriggerEvent")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(TypeName.OBJECT)
            .addParameter(
                ParameterSpec.builder(EVENT_TRIGGER, "eventTrigger", Modifier.FINAL).build())
            .addParameter(ParameterSpec.builder(OBJECT, "eventState", Modifier.FINAL).build())
            .addParameter(ArrayTypeName.of(TypeName.OBJECT), "params", Modifier.FINAL);

    methodBuilder.addStatement("int id = eventTrigger.mId");
    methodBuilder.beginControlFlow("switch($L)", "id");

    for (EventMethodModel eventMethodModel : specModel.getTriggerMethods()) {
      String key =
          specModel.getComponentName()
              + ComponentImplGenerator.getEventTriggerInstanceName(eventMethodModel.name);
      methodBuilder.beginControlFlow("case $L:", key.hashCode());

      final String eventVariableName = "_event";

      methodBuilder.addStatement(
          "$T $L = ($T) $L",
          eventMethodModel.eventType.name,
          eventVariableName,
          eventMethodModel.eventType.name,
          "eventState");

      final CodeBlock.Builder eventTriggerParams =
          CodeBlock.builder().indent().add("\n$L", "eventTrigger.mTriggerTarget");

      int paramIndex = 0;
      for (MethodParamModel methodParamModel : eventMethodModel.methodParams) {
        if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, FromTrigger.class)) {
          eventTriggerParams.add(",\n$L.$L", eventVariableName, methodParamModel.getName());
          continue;
        }

        if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, Param.class)) {
          eventTriggerParams.add(",\n($T) params[$L]", methodParamModel.getType(), paramIndex++);
        }
      }

      eventTriggerParams.unindent();

      if (!eventMethodModel.returnType.equals(TypeName.VOID)) {
        methodBuilder.addStatement(
            "return $L($L)", eventMethodModel.name, eventTriggerParams.build());
      } else {
        methodBuilder.addStatement("$L($L)", eventMethodModel.name, eventTriggerParams.build());
        methodBuilder.addStatement("return null");
      }

      methodBuilder.endControlFlow();
    }

    return methodBuilder.addStatement("default:\nreturn null").endControlFlow().build();
  }

  static TypeSpecDataHolder generateOnTriggerMethodDelegates(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    for (EventMethodModel eventMethod : specModel.getTriggerMethods()) {
      typeSpecDataHolder.addMethod(generateOnTriggerMethodDelegate(specModel, eventMethod));
    }

    return typeSpecDataHolder.build();
  }

  /** Generate a delegate to the Spec that defines this onTrigger method. */
  static MethodSpec generateOnTriggerMethodDelegate(
      SpecModel specModel, EventMethodModel eventMethodModel) {
    final String implName = specModel.getComponentName() + "Impl";
    final MethodSpec.Builder methodSpec =
        MethodSpec.methodBuilder(eventMethodModel.name.toString())
            .addModifiers(Modifier.PRIVATE)
            .returns(eventMethodModel.returnType)
            .addParameter(ClassNames.HAS_EVENT_TRIGGER_CLASSNAME, ABSTRACT_IMPL_PARAM_NAME)
            .addStatement(
                "$L $L = ($L) $L",
                implName,
                IMPL_VARIABLE_NAME,
                implName,
                ABSTRACT_IMPL_PARAM_NAME);

    final CodeBlock.Builder delegation = CodeBlock.builder();

    final String sourceDelegateAccessor = SpecModelUtils.getSpecAccessor(specModel);
    if (eventMethodModel.returnType.equals(TypeName.VOID)) {
      delegation.add("$L.$L(\n", sourceDelegateAccessor, eventMethodModel.name);
    } else {
      delegation.add(
          "$T _result = ($T) $L.$L(\n",
          eventMethodModel.returnType,
          eventMethodModel.returnType,
          sourceDelegateAccessor,
          eventMethodModel.name);
    }

    delegation.indent();
    for (int i = 0, size = eventMethodModel.methodParams.size(); i < size; i++) {
      final MethodParamModel methodParamModel = eventMethodModel.methodParams.get(i);

      if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, FromTrigger.class)
          || MethodParamModelUtils.isAnnotatedWith(methodParamModel, Param.class)) {
        methodSpec.addParameter(methodParamModel.getType(), methodParamModel.getName());
        delegation.add(methodParamModel.getName());
      } else if (methodParamModel.getType().equals(specModel.getContextClass())) {
        delegation.add(
            "($T) $L.getScopedContext()", methodParamModel.getType(), IMPL_VARIABLE_NAME);
      } else {
        delegation.add(
            "($T) $L.$L",
            methodParamModel.getType(),
            IMPL_VARIABLE_NAME,
            getImplAccessor(specModel, methodParamModel));
      }

      delegation.add((i < eventMethodModel.methodParams.size() - 1) ? ",\n" : ");\n");
    }

    delegation.unindent();

    methodSpec.addCode(delegation.build());

    if (!eventMethodModel.returnType.equals(TypeName.VOID)) {
      methodSpec.addStatement("return _result");
    }

    return methodSpec.build();
  }

  static TypeSpecDataHolder generateStaticTriggerMethods(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    for (EventMethodModel eventMethodModel : specModel.getTriggerMethods()) {
      typeSpecDataHolder.addMethod(
          generateStaticTriggerMethodWithKey(
              specModel.getComponentName(), specModel.getContextClass(), eventMethodModel));
    }

    return typeSpecDataHolder.build();
  }

  static MethodSpec generateStaticTriggerMethodWithKey(
      String componentName, ClassName contextClassName, EventMethodModel eventMethodModel) {

    MethodSpec.Builder triggerMethod =
        MethodSpec.methodBuilder(eventMethodModel.name.toString())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

    String methodId =
        componentName + ComponentImplGenerator.getEventTriggerInstanceName(eventMethodModel.name);

    triggerMethod
        .addParameter(contextClassName, "c")
        .addParameter(ClassNames.STRING, "key")
        .addStatement("$T methodId = $L", TypeName.INT, methodId.hashCode())
        .addStatement("$T trigger = getEventTrigger(c, methodId, key)", ClassNames.EVENT_TRIGGER);

    return generateCommonStaticTriggerMethodCode(contextClassName, eventMethodModel, triggerMethod);
  }

  private static MethodSpec generateCommonStaticTriggerMethodCode(
      ClassName contextClassName,
      EventMethodModel eventMethodModel,
      MethodSpec.Builder eventTriggerMethod) {

    EventDeclarationModel eventDeclaration = eventMethodModel.eventType;

    eventTriggerMethod.beginControlFlow("if (trigger == null)");
    eventTriggerMethod.addStatement(
        (eventDeclaration.returnType == null || eventDeclaration.returnType.equals(TypeName.VOID))
            ? "return"
            : "return null");
    eventTriggerMethod.endControlFlow();

    eventTriggerMethod.addStatement(
        "$T _eventState = new $T()",
        eventDeclaration.name, // need to make these into types
        eventDeclaration.name);

    final CodeBlock.Builder paramsBlock = CodeBlock.builder();
    paramsBlock.add("new Object[] {\n");
    paramsBlock.indent();

    for (int i = 0, size = eventMethodModel.methodParams.size(); i < size; i++) {
      final MethodParamModel methodParamModel = eventMethodModel.methodParams.get(i);

      if (methodParamModel.getType().equals(contextClassName)) {
        continue;
      }

      if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, FromTrigger.class)) {
        eventTriggerMethod.addParameter(methodParamModel.getType(), methodParamModel.getName());
        eventTriggerMethod.addStatement(
            "_eventState.$L = $L", methodParamModel.getName(), methodParamModel.getName());
      }

      if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, Param.class)) {
        paramsBlock.add("$L,\n", methodParamModel.getName());
        eventTriggerMethod.addParameter(methodParamModel.getType(), methodParamModel.getName());
        if (methodParamModel.getType() instanceof TypeVariableName) {
          eventTriggerMethod.addTypeVariable((TypeVariableName) methodParamModel.getType());
        }
      }
    }

    paramsBlock.unindent();
    paramsBlock.add("}");

    if (eventDeclaration.returnType == null || eventDeclaration.returnType.equals(TypeName.VOID)) {
      eventTriggerMethod.addStatement(
          "trigger.dispatchOnTrigger(_eventState, $L)", paramsBlock.build());
    } else {
      eventTriggerMethod
          .addStatement(
              "return ($L) trigger.dispatchOnTrigger(_eventState, $L)",
              eventDeclaration.returnType,
              paramsBlock.build())
          .returns(eventDeclaration.returnType);
    }

    return eventTriggerMethod.build();
  }
}
