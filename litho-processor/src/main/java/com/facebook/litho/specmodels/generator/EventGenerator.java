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

import static com.facebook.litho.specmodels.generator.ComponentBodyGenerator.getImplAccessor;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.ABSTRACT_PARAM_NAME;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.REF_VARIABLE_NAME;
import static com.facebook.litho.specmodels.model.ClassNames.EVENT_HANDLER;
import static com.facebook.litho.specmodels.model.ClassNames.OBJECT;

import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.FieldModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.MethodParamModelUtils;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecMethodModelUtils;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.facebook.litho.specmodels.model.StateParamModel;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import javax.lang.model.element.Modifier;

/** Class that generates the event methods for a Component. */
public class EventGenerator {

  private EventGenerator() {}

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    final TypeSpecDataHolder.Builder builder =
        TypeSpecDataHolder.newBuilder()
            .addTypeSpecDataHolder(generateGetEventHandlerMethods(specModel))
            .addTypeSpecDataHolder(generateEventDispatchers(specModel))
            .addTypeSpecDataHolder(generateEventMethods(specModel))
            .addTypeSpecDataHolder(generateEventHandlerFactories(specModel));

    if (!specModel.getEventMethods().isEmpty()) {
      builder.addMethod(generateDispatchOnEvent(specModel));
    }

    return builder.build();
  }

  static TypeSpecDataHolder generateGetEventHandlerMethods(SpecModel specModel) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    for (EventDeclarationModel eventDeclaration : specModel.getEventDeclarations()) {
      dataHolder.addTypeSpecDataHolder(generateGetEventHandlerMethod(specModel, eventDeclaration));
    }

    return dataHolder.build();
  }

  static TypeSpecDataHolder generateGetEventHandlerMethod(
      SpecModel specModel, EventDeclarationModel eventDeclaration) {
    final String scopeMethodName = specModel.getScopeMethodName();
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
                    "return (($L) context.$L()).$L",
                    specModel.getComponentName(),
                    scopeMethodName,
                    ComponentBodyGenerator.getEventHandlerInstanceName(eventDeclaration.name))
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
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();

    MethodSpec.Builder eventDispatcherMethod =
        MethodSpec.methodBuilder("dispatch" + eventDeclaration.name.simpleName())
            .addModifiers(Modifier.STATIC)
            .addParameter(ClassNames.EVENT_HANDLER, "_eventHandler");

    eventDispatcherMethod.addStatement(
        "final $T _eventState = new $T()", eventDeclaration.name, eventDeclaration.name);

    for (FieldModel fieldModel : eventDeclaration.fields) {
      if (fieldModel.field.modifiers.contains(Modifier.FINAL)) {
        continue;
      }
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
              "return ($T) _lifecycle.dispatchOnEvent(_eventHandler, _eventState)",
              eventDeclaration.returnType)
          .returns(eventDeclaration.returnType);
    }

    return typeSpecDataHolder.addMethod(eventDispatcherMethod.build()).build();
  }

  static TypeSpecDataHolder generateEventMethods(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    for (SpecMethodModel<EventMethod, EventDeclarationModel> eventMethod :
        specModel.getEventMethods()) {
      typeSpecDataHolder.addMethod(generateEventMethod(specModel, eventMethod));
    }

    return typeSpecDataHolder.build();
  }

  /** Generate a delegate to the Spec that defines this event method. */
  static MethodSpec generateEventMethod(
      SpecModel specModel, SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel) {
    final String componentName = specModel.getComponentName();
    final MethodSpec.Builder methodSpec =
        MethodSpec.methodBuilder(eventMethodModel.name.toString())
            .addModifiers(Modifier.PRIVATE)
            .returns(eventMethodModel.returnType)
            .addParameter(ClassNames.HAS_EVENT_DISPATCHER_CLASSNAME, ABSTRACT_PARAM_NAME)
            .addStatement(
                "$L $L = ($L) $L",
                componentName,
                REF_VARIABLE_NAME,
                componentName,
                ABSTRACT_PARAM_NAME);

    final boolean hasLazyStateParams = SpecMethodModelUtils.hasLazyStateParams(eventMethodModel);
    if (hasLazyStateParams) {
      methodSpec.addStatement(
          "$L stateContainer = getStateContainerWithLazyStateUpdatesApplied(c, $L)",
          StateContainerGenerator.getStateContainerClassName(specModel),
          REF_VARIABLE_NAME);
    }

    final CodeBlock.Builder delegation = CodeBlock.builder();

    final String sourceDelegateAccessor = SpecModelUtils.getSpecAccessor(specModel);
    final boolean isErrorDelegation =
        eventMethodModel.name.toString().equals(EventCaseGenerator.INTERNAL_ON_ERROR_HANDLER_NAME);

    if (isErrorDelegation) {
      delegation.add("$L(\n", "onError");
    } else if (eventMethodModel.returnType.equals(TypeName.VOID)) {
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

      if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, FromEvent.class)
          || MethodParamModelUtils.isAnnotatedWith(methodParamModel, Param.class)
          || methodParamModel.getTypeName().equals(specModel.getContextClass())) {
        methodSpec.addParameter(methodParamModel.getTypeName(), methodParamModel.getName());
        delegation.add(methodParamModel.getName());
      } else if (hasLazyStateParams && methodParamModel instanceof StateParamModel) {
        delegation.add(
            "($T) stateContainer.$L", methodParamModel.getTypeName(), methodParamModel.getName());
      } else {
        delegation.add(
            "($T) $L.$L",
            methodParamModel.getTypeName(),
            REF_VARIABLE_NAME,
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

  /** Generate a dispatchOnEvent() implementation for the component. */
  static MethodSpec generateDispatchOnEvent(SpecModel specModel) {
    final MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder("dispatchOnEvent")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(TypeName.OBJECT)
            .addParameter(
                ParameterSpec.builder(EVENT_HANDLER, "eventHandler", Modifier.FINAL).build())
            .addParameter(ParameterSpec.builder(OBJECT, "eventState", Modifier.FINAL).build());

    methodBuilder.addStatement("int id = eventHandler.id");
    methodBuilder.beginControlFlow("switch ($L)", "id");

    EventCaseGenerator.builder()
        .contextClass(specModel.getContextClass())
        .eventMethodModels(specModel.getEventMethods())
        // For now, Sections are not supported for error propagation
        .withErrorPropagation(specModel.getComponentClass().equals(ClassNames.COMPONENT))
        .writeTo(methodBuilder);

    return methodBuilder.addStatement("default:\nreturn null").endControlFlow().build();
  }

  static TypeSpecDataHolder generateEventHandlerFactories(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    for (SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel :
        specModel.getEventMethods()) {
      typeSpecDataHolder.addMethod(
          generateEventHandlerFactory(eventMethodModel, specModel.getContextClass()));
    }

    return typeSpecDataHolder.build();
  }

  static MethodSpec generateEventHandlerFactory(
      SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel, TypeName paramClass) {
    final MethodSpec.Builder builder =
        MethodSpec.methodBuilder(eventMethodModel.name.toString())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariables(eventMethodModel.typeVariables)
            .addParameter(paramClass, "c")
            .returns(
                ParameterizedTypeName.get(
                    ClassNames.EVENT_HANDLER, eventMethodModel.typeModel.name));

    final CodeBlock.Builder paramsBlock = CodeBlock.builder();

    paramsBlock.add("new Object[] {\n");
    paramsBlock.indent();
    paramsBlock.add("c,\n");

    for (MethodParamModel methodParamModel : eventMethodModel.methodParams) {
      if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, Param.class)) {
        builder.addParameter(methodParamModel.getTypeName(), methodParamModel.getName());
        paramsBlock.add("$L,\n", methodParamModel.getName());

        if (methodParamModel.getTypeName() instanceof TypeVariableName) {
          builder.addTypeVariable((TypeVariableName) methodParamModel.getTypeName());
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
