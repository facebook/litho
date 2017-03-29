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

import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethodModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.MethodParamModelUtils;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.SpecModel;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import static com.facebook.litho.specmodels.generator.GeneratorConstants.ABSTRACT_IMPL_PARAM_NAME;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.DELEGATE_FIELD_NAME;
import static com.facebook.litho.specmodels.model.ClassNames.EVENT_HANDLER;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.IMPL_VARIABLE_NAME;
import static com.facebook.litho.specmodels.model.ClassNames.OBJECT;
import static com.facebook.litho.specmodels.generator.ComponentImplGenerator.getImplAccessor;

/**
 * Class that generates the event methods for a Component.
 */
public class EventGenerator {

  private EventGenerator() {
  }

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    return TypeSpecDataHolder.newBuilder()
        .addTypeSpecDataHolder(generateGetEventHandlerMethods(specModel))
        .addTypeSpecDataHolder(generateEventDispatchers(specModel))
        .addTypeSpecDataHolder(generateEventMethods(specModel))
        .addTypeSpecDataHolder(generateEventHandlerFactories(specModel))
        .addMethod(generateDispatchOnEvent(specModel))
        .build();
  }

  static TypeSpecDataHolder generateGetEventHandlerMethods(SpecModel specModel) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    for (EventDeclarationModel eventDeclaration : specModel.getEventDeclarations()) {
      dataHolder.addTypeSpecDataHolder(generateGetEventHandlerMethod(specModel, eventDeclaration));
    }

    return dataHolder.build();
  }

  static TypeSpecDataHolder generateGetEventHandlerMethod(
      SpecModel specModel,
      EventDeclarationModel eventDeclaration) {
    final String scopeMethodName = "getComponentScope";
    return TypeSpecDataHolder.newBuilder()
        .addMethod(
            MethodSpec.methodBuilder("get" + eventDeclaration.name.simpleName() + "Handler")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassNames.EVENT_HANDLER)
                .addParameter(specModel.getContextClass(), "context")
                .addCode(
                    CodeBlock.builder()
                        .beginControlFlow("if (context.$L() == null)", scopeMethodName)
                        .addStatement("return null")
                        .endControlFlow()
                        .build())
                .addStatement(
                    "return (($L.$L) context.$L()).$L",
                    specModel.getComponentName(),
                    ComponentImplGenerator.getImplClassName(specModel),
                    scopeMethodName,
                    ComponentImplGenerator.getEventHandlerInstanceName(eventDeclaration.name))
                .build())
        .build();
  }

  static TypeSpecDataHolder generateEventDispatchers(SpecModel specModel) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    for (EventDeclarationModel eventDeclaration : specModel.getEventDeclarations()) {
      dataHolder.addTypeSpecDataHolder(generateEventDispatcher(eventDeclaration));
    }

    return dataHolder.build();
  }

  static TypeSpecDataHolder generateEventDispatcher(EventDeclarationModel eventDeclaration) {
    MethodSpec.Builder eventDispatcherMethod =
        MethodSpec.methodBuilder("dispatch" + eventDeclaration.name.simpleName())
            .addModifiers(Modifier.STATIC)
            .addParameter(ClassNames.EVENT_HANDLER, "_eventHandler");

    eventDispatcherMethod.addStatement(
        "$T _eventState = new $T()",
        eventDeclaration.name,  // need to make these into types
        eventDeclaration.name);

    for (EventDeclarationModel.FieldModel fieldModel : eventDeclaration.fields) {
      eventDispatcherMethod
          .addParameter(fieldModel.field.type, fieldModel.field.name)
          .addStatement("_eventState.$L = $L", fieldModel.field.name, fieldModel.field.name);
    }

    eventDispatcherMethod.addStatement(
        "$T _lifecycle = _eventHandler.mHasEventDispatcher.getEventDispatcher()",
        ClassNames.EVENT_DISPATCHER);

    if (eventDeclaration.returnType.equals(TypeName.VOID)) {
      eventDispatcherMethod.addStatement("_lifecycle.dispatchOnEvent(_eventHandler, _eventState)");
    } else {
      eventDispatcherMethod
          .addStatement(
              "return ($L) _lifecycle.dispatchOnEvent(_eventHandler, _eventState)",
              eventDeclaration.returnType)
          .returns(eventDeclaration.returnType);
    }

    return TypeSpecDataHolder.newBuilder().addMethod(eventDispatcherMethod.build()).build();
  }

  static TypeSpecDataHolder generateEventMethods(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    for (EventMethodModel eventMethod : specModel.getEventMethods()) {
      typeSpecDataHolder.addMethod(generateEventMethod(specModel, eventMethod));
    }

    return typeSpecDataHolder.build();
  }

  /**
   * Generate a delegate to the Spec that defines this event method.
   */
  static MethodSpec generateEventMethod(
      SpecModel specModel,
      EventMethodModel eventMethodModel) {
    final String implName = specModel.getComponentName() + "Impl";
    final MethodSpec.Builder methodSpec = MethodSpec.methodBuilder(eventMethodModel.name.toString())
        .addModifiers(Modifier.PRIVATE)
        .returns(eventMethodModel.returnType)
        .addParameter(ClassNames.HAS_EVENT_DISPATCHER_CLASSNAME, ABSTRACT_IMPL_PARAM_NAME)
        .addStatement(
            "$L $L = ($L) $L", implName, IMPL_VARIABLE_NAME, implName, ABSTRACT_IMPL_PARAM_NAME);

    final CodeBlock.Builder delegation = CodeBlock.builder();

    final String sourceDelegateAccessor = DELEGATE_FIELD_NAME +
        (specModel.hasInjectedDependencies() ?
            specModel.getDependencyInjectionHelper().getSourceDelegateAccessorMethod(specModel) :
            "");
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

      if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, FromEvent.class) ||
          MethodParamModelUtils.isAnnotatedWith(methodParamModel, Param.class) ||
          methodParamModel.getType().equals(specModel.getContextClass())) {
        methodSpec.addParameter(methodParamModel.getType(), methodParamModel.getName());
        delegation.add(methodParamModel.getName());
      } else {
        delegation.add(
            "($T) $L.$L",
            methodParamModel.getType(),
            IMPL_VARIABLE_NAME,
            getImplAccessor(specModel, methodParamModel));
      }

      if (i < eventMethodModel.methodParams.size() - 1) {
        delegation.add(",\n");
      } else {
        delegation.add(");\n");
      }
    }

    delegation.unindent();

    methodSpec.addCode(delegation.build());

    if (!eventMethodModel.returnType.equals(TypeName.VOID)) {
      methodSpec.addStatement("return _result");
    }

    return methodSpec.build();
  }

  /**
   * Generate a dispatchOnEvent() implementation for the component.
   */
  static MethodSpec generateDispatchOnEvent(SpecModel specModel) {
    final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("dispatchOnEvent")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(TypeName.OBJECT)
        .addParameter(
            ParameterSpec.builder(EVENT_HANDLER, "eventHandler", Modifier.FINAL)
                .build())
        .addParameter(
            ParameterSpec.builder(OBJECT, "eventState", Modifier.FINAL).build());

    methodBuilder.addStatement("int id = eventHandler.id");
    methodBuilder.beginControlFlow("switch($L)", "id");

    for (EventMethodModel eventMethodModel : specModel.getEventMethods()) {
      methodBuilder.beginControlFlow("case $L:", eventMethodModel.name.toString().hashCode());

      final String eventVariableName = "_event";

      methodBuilder.addStatement(
          "$T $L = ($T) $L",
          eventMethodModel.eventType.name,
          eventVariableName,
          eventMethodModel.eventType.name,
          "eventState");

      final CodeBlock.Builder eventHandlerParams = CodeBlock.builder()
          .indent()
          .add("\n$L", "eventHandler.mHasEventDispatcher");

      int paramIndex = 0;
      for (MethodParamModel methodParamModel : eventMethodModel.methodParams) {
        if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, FromEvent.class)) {
          eventHandlerParams.add(",\n$L.$L", eventVariableName, methodParamModel.getName());
        } else if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, Param.class) ||
            methodParamModel.getType().equals(specModel.getContextClass())) {
          eventHandlerParams.add(
              ",\n($T) eventHandler.params[$L]", methodParamModel.getType(), paramIndex++);
        }
      }

      eventHandlerParams.unindent();

      if (!eventMethodModel.returnType.equals(TypeName.VOID)) {
        methodBuilder.addStatement(
            "return $L($L)",
            eventMethodModel.name,
            eventHandlerParams.build());
      } else {
        methodBuilder.addStatement(
            "$L($L)",
            eventMethodModel.name,
            eventHandlerParams.build());
        methodBuilder.addStatement("return null");
      }

      methodBuilder.endControlFlow();
    }

    return methodBuilder.addStatement("default:\nreturn null")
        .endControlFlow()
        .build();
  }

  static TypeSpecDataHolder generateEventHandlerFactories(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    for (EventMethodModel eventMethodModel : specModel.getEventMethods()) {
      typeSpecDataHolder.addMethod(
          generateEventHandlerFactory(eventMethodModel, specModel.getContextClass()));
      typeSpecDataHolder.addMethod(
          generateEventHandlerFactory(eventMethodModel, specModel.getComponentClass()));
    }

    return typeSpecDataHolder.build();
  }

  static MethodSpec generateEventHandlerFactory(
      EventMethodModel eventMethodModel,
      TypeName paramClass) {
    final MethodSpec.Builder builder = MethodSpec.methodBuilder(eventMethodModel.name.toString())
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameter(paramClass, "c")
        .returns(ParameterizedTypeName.get(
            ClassNames.EVENT_HANDLER,
            eventMethodModel.eventType.name));

    final CodeBlock.Builder paramsBlock = CodeBlock.builder();

    paramsBlock.add("new Object[] {\n");
    paramsBlock.indent();
    paramsBlock.add("c,\n");

    for (MethodParamModel methodParamModel : eventMethodModel.methodParams) {
      if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, Param.class)) {
        builder.addParameter(methodParamModel.getType(), methodParamModel.getName());
        paramsBlock.add("$L,\n", methodParamModel.getName());

        if (methodParamModel.getType() instanceof TypeVariableName) {
          builder.addTypeVariable((TypeVariableName) methodParamModel.getType());
        }
      }
    }

    paramsBlock.unindent();
    paramsBlock.add("}");

    builder.addStatement(
        "return newEventHandler(c, $L, $L)",
        eventMethodModel.name.toString().hashCode(),
        paramsBlock.build());

    return builder.build();
  }
}
