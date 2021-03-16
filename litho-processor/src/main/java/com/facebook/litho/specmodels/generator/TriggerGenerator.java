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
import static com.facebook.litho.specmodels.model.ClassNames.EVENT_TRIGGER;
import static com.facebook.litho.specmodels.model.ClassNames.EVENT_TRIGGER_CONTAINER;
import static com.facebook.litho.specmodels.model.ClassNames.OBJECT;

import com.facebook.litho.annotations.FromTrigger;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.MethodParamModelUtils;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.util.Locale;
import javax.lang.model.element.Modifier;

/** Class that generates the trigger methods for a Component. */
@SuppressWarnings("NewApi")
public class TriggerGenerator {

  private static final String DEPRECATED_METHOD_JAVADOC =
      "@deprecated Use {@link #$L(ComponentContext, Handle)} instead.\n";

  private TriggerGenerator() {}

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    final TypeSpecDataHolder.Builder builder =
        TypeSpecDataHolder.newBuilder()
            .addTypeSpecDataHolder(generateStaticGetTriggerMethods(specModel))
            .addTypeSpecDataHolder(generateOnTriggerMethodDelegates(specModel))
            .addTypeSpecDataHolder(generateStaticTriggerMethods(specModel));

    if (!specModel.getTriggerMethods().isEmpty()) {
      builder.addMethod(generateAcceptTriggerEventImpl(specModel));
      builder.addMethod(generateRecordTriggers(specModel));
    }

    return builder.build();
  }

  /** Generate acceptTriggerEvent() implementation for the component. */
  static MethodSpec generateAcceptTriggerEventImpl(SpecModel specModel) {
    final MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder("acceptTriggerEventImpl")
            .addModifiers(Modifier.PROTECTED)
            .addAnnotation(Override.class)
            .returns(TypeName.OBJECT)
            .addParameter(
                ParameterSpec.builder(EVENT_TRIGGER, "eventTrigger", Modifier.FINAL).build())
            .addParameter(ParameterSpec.builder(OBJECT, "eventState", Modifier.FINAL).build())
            .addParameter(ArrayTypeName.of(TypeName.OBJECT), "params", Modifier.FINAL);

    methodBuilder.addStatement("int id = eventTrigger.mId");
    methodBuilder.beginControlFlow("switch($L)", "id");

    for (SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel :
        specModel.getTriggerMethods()) {
      methodBuilder.beginControlFlow(
          "case $L:", getMethodIdInt(specModel.getComponentName(), eventMethodModel));

      final String eventVariableName = "_event";

      methodBuilder.addStatement(
          "$T $L = ($T) $L",
          eventMethodModel.typeModel.getRawName(),
          eventVariableName,
          eventMethodModel.typeModel.getRawName(),
          "eventState");

      final CodeBlock.Builder eventTriggerParams =
          CodeBlock.builder()
              .indent()
              .add("\n($L) eventTrigger.mComponentContext", specModel.getContextClass());
      eventTriggerParams.add(",\n$L", "eventTrigger.mTriggerTarget");

      int paramIndex = 0;
      for (MethodParamModel methodParamModel : eventMethodModel.methodParams) {
        if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, FromTrigger.class)) {
          eventTriggerParams.add(",\n$L.$L", eventVariableName, methodParamModel.getName());
          continue;
        }

        if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, Param.class)) {
          eventTriggerParams.add(
              ",\n($T) params[$L]", methodParamModel.getTypeName(), paramIndex++);
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

  static MethodSpec generateRecordTriggers(SpecModel specModel) {
    final MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder("recordEventTrigger")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .addParameter(ClassNames.COMPONENT_CONTEXT, "c")
            .addParameter(ParameterSpec.builder(EVENT_TRIGGER_CONTAINER, "container").build());

    for (SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel :
        specModel.getTriggerMethods()) {
      String trigger = ComponentBodyGenerator.getEventTriggerInstanceName(eventMethodModel.name);
      methodBuilder
          .beginControlFlow("if ($L != null)", trigger)
          .addStatement("$L.mComponentContext = c", trigger)
          .addStatement("$L.mTriggerTarget = this", trigger)
          .addStatement("container.recordEventTrigger($L)", trigger)
          .endControlFlow();
    }

    return methodBuilder.build();
  }

  static TypeSpecDataHolder generateOnTriggerMethodDelegates(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    for (SpecMethodModel<EventMethod, EventDeclarationModel> eventMethod :
        specModel.getTriggerMethods()) {
      typeSpecDataHolder.addMethod(generateOnTriggerMethodDelegate(specModel, eventMethod));
    }

    return typeSpecDataHolder.build();
  }

  /** Generate a delegate to the Spec that defines this onTrigger method. */
  static MethodSpec generateOnTriggerMethodDelegate(
      SpecModel specModel, SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel) {
    final String componentName = specModel.getComponentName();
    final MethodSpec.Builder methodSpec =
        MethodSpec.methodBuilder(eventMethodModel.name.toString())
            .addModifiers(Modifier.PRIVATE)
            .returns(eventMethodModel.returnType)
            .addParameter(specModel.getContextClass(), "c")
            .addParameter(ClassNames.EVENT_TRIGGER_TARGET, ABSTRACT_PARAM_NAME)
            .addStatement(
                "$L $L = ($L) $L",
                componentName,
                REF_VARIABLE_NAME,
                componentName,
                ABSTRACT_PARAM_NAME);

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
        methodSpec.addParameter(
            ParameterSpec.builder(methodParamModel.getTypeName(), methodParamModel.getName())
                .addAnnotations(methodParamModel.getExternalAnnotations())
                .build());
        delegation.add(methodParamModel.getName());
      } else if (methodParamModel.getTypeName().equals(specModel.getContextClass())) {
        delegation.add("c", methodParamModel.getTypeName());
      } else {
        delegation.add(
            "($T) $L.$L",
            methodParamModel.getTypeName(),
            REF_VARIABLE_NAME,
            getImplAccessor(eventMethodModel.name.toString(), specModel, methodParamModel, "c"));
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

  static TypeSpecDataHolder generateStaticGetTriggerMethods(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    for (SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel :
        specModel.getTriggerMethods()) {
      // This is a bit of a hack -- this method is not used for Surfaces since they can't register
      // triggers from a builder. Instead of providing a dummy implementation of newEventTrigger
      // that this calls which would throw,
      // we just don't generate it
      if (specModel.getComponentClass().equals(ClassNames.COMPONENT)
          || specModel.getComponentClass().equals(ClassNames.SECTION)) {
        typeSpecDataHolder.addMethod(
            generateStaticGetTrigger(
                specModel.getComponentName(),
                specModel.getContextClass(),
                specModel.getComponentClass(),
                eventMethodModel));
      }
      typeSpecDataHolder.addMethod(
          generateDeprecatedStaticGetTrigger(
              specModel.getComponentName(), specModel.getContextClass(), eventMethodModel));
    }

    return typeSpecDataHolder.build();
  }

  private static MethodSpec generateDeprecatedStaticGetTrigger(
      String componentName,
      ClassName contextClassName,
      SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel) {

    MethodSpec.Builder triggerMethod =
        MethodSpec.methodBuilder(
                ComponentBodyGenerator.getEventTriggerInstanceName(eventMethodModel.name))
            .returns(ClassNames.EVENT_TRIGGER)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

    triggerMethod
        .addJavadoc(
            "@deprecated Do not use this method to get a EventTrigger to use later. Instead give the component a Handle and use {@link #$L(ComponentContext, Handle)}.\n",
            eventMethodModel.name.toString())
        .addAnnotation(java.lang.Deprecated.class)
        .addParameter(contextClassName, "c")
        .addParameter(ClassNames.STRING, "key")
        .addStatement(
            "$T methodId = $L", TypeName.INT, getMethodIdInt(componentName, eventMethodModel))
        .addStatement(
            "return newEventTrigger(c, key, methodId)",
            ComponentBodyGenerator.getEventTriggerInstanceName(eventMethodModel.name));

    return triggerMethod.build();
  }

  private static MethodSpec generateStaticGetTrigger(
      String componentName,
      ClassName contextClassName,
      ClassName componentClassName,
      SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel) {

    MethodSpec.Builder triggerMethod =
        MethodSpec.methodBuilder(getCreateEventTriggerMethodName(eventMethodModel.name))
            .returns(ClassNames.EVENT_TRIGGER)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC);

    triggerMethod
        .addParameter(contextClassName, "c")
        .addParameter(componentClassName, "component")
        .addStatement(
            "$T methodId = $L", TypeName.INT, getMethodIdInt(componentName, eventMethodModel))
        .addStatement("return newEventTrigger(c, component, methodId)");

    return triggerMethod.build();
  }

  private static int getMethodIdInt(
      String componentName, SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel) {
    String methodId =
        componentName + ComponentBodyGenerator.getEventTriggerInstanceName(eventMethodModel.name);
    return methodId.hashCode();
  }

  public static String getCreateEventTriggerMethodName(CharSequence triggerClassName) {
    String asString = triggerClassName.toString();
    return "create"
        + asString.substring(0, 1).toUpperCase(Locale.ROOT)
        + asString.substring(1)
        + "Trigger";
  }

  private enum TriggerLookup {
    HANDLE(ClassNames.HANDLE, "handle"),
    KEY(ClassNames.STRING, "key"),
    ;

    TriggerLookup(ClassName keyType, String paramName) {
      this.keyType = keyType;
      this.paramName = paramName;
    }

    public ClassName keyType;
    public String paramName;
  }

  static TypeSpecDataHolder generateStaticTriggerMethods(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    for (SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel :
        specModel.getTriggerMethods()) {
      typeSpecDataHolder.addMethod(
          generateStaticTriggerMethod(
              specModel.getComponentName(),
              specModel.getContextClass(),
              eventMethodModel,
              TriggerLookup.HANDLE));

      typeSpecDataHolder.addMethod(
          generateStaticTriggerMethod(
              specModel.getComponentName(),
              specModel.getContextClass(),
              eventMethodModel,
              TriggerLookup.KEY));

      typeSpecDataHolder.addMethod(
          generateStaticTriggerMethodWithTriggerHandler(
              specModel.getContextClass(), eventMethodModel));

      typeSpecDataHolder.addMethod(
          generateDeprecatedStateSelfTriggerMethod(
              specModel.getComponentName(),
              specModel.getContextClass(),
              specModel.getScopeMethodName(),
              eventMethodModel));
    }

    return typeSpecDataHolder.build();
  }

  private static MethodSpec generateStaticTriggerMethod(
      String componentName,
      ClassName contextClassName,
      SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel,
      TriggerLookup triggerLookup) {

    MethodSpec.Builder triggerMethod =
        MethodSpec.methodBuilder(eventMethodModel.name.toString())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

    if (triggerLookup == TriggerLookup.KEY) {
      triggerMethod
          .addJavadoc(DEPRECATED_METHOD_JAVADOC, eventMethodModel.name.toString())
          .addAnnotation(java.lang.Deprecated.class);
    } else if (triggerLookup == TriggerLookup.HANDLE) {
      triggerMethod.addJavadoc(
          "This will send the $L trigger to the component with the given handle.\nFor more information about using triggers, see https://fblitho.com/docs/trigger-events\n",
          eventMethodModel.name.toString());
    }

    triggerMethod
        .addParameter(contextClassName, "c")
        .addParameter(triggerLookup.keyType, triggerLookup.paramName)
        .addStatement(
            "$T methodId = $L", TypeName.INT, getMethodIdInt(componentName, eventMethodModel))
        .addStatement(
            "$T trigger = getEventTrigger(c, methodId, " + triggerLookup.paramName + ")",
            ClassNames.EVENT_TRIGGER);

    EventDeclarationModel eventDeclaration = eventMethodModel.typeModel;

    triggerMethod.beginControlFlow("if (trigger == null)");
    triggerMethod.addStatement(
        (eventDeclaration.returnType == null || eventDeclaration.returnType.equals(TypeName.VOID))
            ? "return"
            : "return null");
    triggerMethod.endControlFlow();

    return generateCommonStaticTriggerMethodCode(contextClassName, eventMethodModel, triggerMethod);
  }

  private static MethodSpec generateStaticTriggerMethodWithTriggerHandler(
      ClassName contextClassName,
      SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel) {

    MethodSpec.Builder triggerMethod =
        MethodSpec.methodBuilder(eventMethodModel.name.toString())
            .addJavadoc(DEPRECATED_METHOD_JAVADOC, eventMethodModel.name.toString())
            .addAnnotation(java.lang.Deprecated.class)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

    triggerMethod.addParameter(ClassNames.EVENT_TRIGGER, "trigger");

    return generateCommonStaticTriggerMethodCode(contextClassName, eventMethodModel, triggerMethod);
  }

  private static MethodSpec generateDeprecatedStateSelfTriggerMethod(
      String componentClass,
      ClassName contextClassName,
      String scopeMethodName,
      SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel) {
    MethodSpec.Builder triggerMethod =
        MethodSpec.methodBuilder(eventMethodModel.name.toString())
            .addModifiers(Modifier.STATIC)
            .addJavadoc(DEPRECATED_METHOD_JAVADOC, eventMethodModel.name.toString())
            .addAnnotation(java.lang.Deprecated.class);

    triggerMethod.addParameter(contextClassName, "c");

    addParametersToStaticTriggerMethods(contextClassName, eventMethodModel, triggerMethod);

    triggerMethod.addStatement(
        "$L component = ($L) c.$L()", componentClass, componentClass, scopeMethodName);

    final CodeBlock.Builder eventTriggerParams = CodeBlock.builder().add("\nc");
    eventTriggerParams.add(",\n($T) $L", ClassNames.EVENT_TRIGGER_TARGET, "component");

    for (MethodParamModel methodParamModel : eventMethodModel.methodParams) {
      if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, FromTrigger.class)) {
        eventTriggerParams.add(",\n$L", methodParamModel.getName());
        continue;
      }

      if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, Param.class)) {
        eventTriggerParams.add(",\n$L", methodParamModel.getName());
      }
    }

    EventDeclarationModel eventDeclaration = eventMethodModel.typeModel;
    if (eventDeclaration.returnType == null || eventDeclaration.returnType.equals(TypeName.VOID)) {
      triggerMethod.addStatement(
          "component.$L($L)", eventMethodModel.name, eventTriggerParams.build());
    } else {
      triggerMethod
          .addStatement(
              "return component.$L($L)", eventMethodModel.name, eventTriggerParams.build())
          .returns(eventDeclaration.returnType);
    }

    return triggerMethod.build();
  }

  private static MethodSpec generateCommonStaticTriggerMethodCode(
      ClassName contextClassName,
      SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel,
      MethodSpec.Builder eventTriggerMethod) {

    EventDeclarationModel eventDeclaration = eventMethodModel.typeModel;

    eventTriggerMethod.addStatement(
        "$T _eventState = new $T()",
        eventDeclaration.name, // need to make these into types
        eventDeclaration.name);

    final CodeBlock.Builder paramsBlock = CodeBlock.builder();
    paramsBlock.add("new Object[] {\n");
    paramsBlock.indent();

    addParametersToStaticTriggerMethods(contextClassName, eventMethodModel, eventTriggerMethod);
    addTriggerParams(contextClassName, eventMethodModel, eventTriggerMethod, paramsBlock);

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

  private static MethodSpec.Builder addParametersToStaticTriggerMethods(
      ClassName contextClassName,
      SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel,
      MethodSpec.Builder eventTriggerMethod) {
    for (int i = 0, size = eventMethodModel.methodParams.size(); i < size; i++) {
      final MethodParamModel methodParamModel = eventMethodModel.methodParams.get(i);

      if (methodParamModel.getTypeName().equals(contextClassName)) {
        continue;
      }

      if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, FromTrigger.class)) {
        eventTriggerMethod.addParameter(
            ParameterSpec.builder(methodParamModel.getTypeName(), methodParamModel.getName())
                .addAnnotations(methodParamModel.getExternalAnnotations())
                .build());
      }

      if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, Param.class)) {
        eventTriggerMethod.addParameter(
            ParameterSpec.builder(methodParamModel.getTypeName(), methodParamModel.getName())
                .addAnnotations(methodParamModel.getExternalAnnotations())
                .build());

        maybeAddGenericTypeToStaticFunction(methodParamModel, eventTriggerMethod);
      }
    }

    return eventTriggerMethod;
  }

  private static void maybeAddGenericTypeToStaticFunction(
      MethodParamModel methodParamModel, MethodSpec.Builder eventTriggerMethod) {
    if (methodParamModel.getTypeName() instanceof TypeVariableName) {
      eventTriggerMethod.addTypeVariable((TypeVariableName) methodParamModel.getTypeName());
    }
  }

  private static MethodSpec.Builder addTriggerParams(
      ClassName contextClassName,
      SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel,
      MethodSpec.Builder eventTriggerMethod,
      CodeBlock.Builder paramsBlock) {
    for (int i = 0, size = eventMethodModel.methodParams.size(); i < size; i++) {
      final MethodParamModel methodParamModel = eventMethodModel.methodParams.get(i);

      if (methodParamModel.getTypeName().equals(contextClassName)) {
        continue;
      }

      if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, FromTrigger.class)) {
        eventTriggerMethod.addStatement(
            "_eventState.$L = $L", methodParamModel.getName(), methodParamModel.getName());
      }

      if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, Param.class)) {
        paramsBlock.add("$L,\n", methodParamModel.getName());
      }
    }

    return eventTriggerMethod;
  }
}
