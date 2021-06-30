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
import static com.facebook.litho.specmodels.generator.GeneratorUtils.parameter;
import static com.facebook.litho.specmodels.model.ClassNames.EVENT_HANDLER;
import static com.facebook.litho.specmodels.model.ClassNames.OBJECT;
import static com.facebook.litho.specmodels.model.MethodParamModelUtils.isAnnotatedWith;

import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.FieldModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecMethodModelUtils;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.facebook.litho.specmodels.model.StateParamModel;
import com.facebook.litho.specmodels.model.TreePropModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
      builder.addMethod(generateDispatchOnEventImpl(specModel));
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
    final ClassName eventClassName = ClassName.bestGuess(eventDeclaration.getRawName().toString());
    final List<TypeVariableName> typeVariables;
    if (eventDeclaration.name instanceof ParameterizedTypeName) {
      typeVariables = new ArrayList<>();
      for (TypeName name : ((ParameterizedTypeName) eventDeclaration.name).typeArguments) {
        typeVariables.add((TypeVariableName) name);
      }
    } else {
      typeVariables = Collections.emptyList();
    }
    return TypeSpecDataHolder.newBuilder()
        .addMethod(
            MethodSpec.methodBuilder("get" + eventClassName.simpleName() + "Handler")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addAnnotation(ClassNames.NULLABLE)
                .addTypeVariables(typeVariables)
                .returns(ParameterizedTypeName.get(ClassNames.EVENT_HANDLER, eventDeclaration.name))
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
                    ComponentBodyGenerator.getEventHandlerInstanceName(eventDeclaration))
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
    final ClassName eventClassName = ClassName.bestGuess(eventDeclaration.getRawName().toString());
    MethodSpec.Builder eventDispatcherMethod =
        MethodSpec.methodBuilder("dispatch" + eventClassName.simpleName())
            .addModifiers(Modifier.STATIC)
            .addParameter(ClassNames.EVENT_HANDLER, "_eventHandler");

    eventDispatcherMethod.addStatement(
        "final $T _eventState = new $T()",
        eventDeclaration.getRawName(),
        eventDeclaration.getRawName());

    for (FieldModel fieldModel : eventDeclaration.fields) {
      if (fieldModel.field.modifiers.contains(Modifier.FINAL)) {
        continue;
      }

      // Ignore the generics Type Arguments in the method parameters.
      TypeName typeName = fieldModel.field.type;
      if (typeName instanceof ParameterizedTypeName) {
        typeName = ((ParameterizedTypeName) fieldModel.field.type).rawType;
      } else if (typeName instanceof TypeVariableName) {
        typeName =
            ((TypeVariableName) typeName).bounds.isEmpty()
                ? OBJECT
                : ((TypeVariableName) typeName).bounds.get(0);
      }

      eventDispatcherMethod
          .addParameter(
              ParameterSpec.builder(typeName, fieldModel.field.name)
                  .addAnnotations(fieldModel.field.annotations)
                  .build())
          .addStatement("_eventState.$L = $L", fieldModel.field.name, fieldModel.field.name);
    }

    eventDispatcherMethod.addStatement(
        "$T _dispatcher = _eventHandler.mHasEventDispatcher.getEventDispatcher()",
        ClassNames.EVENT_DISPATCHER);

    if (eventDeclaration.returnType.equals(TypeName.VOID)) {
      eventDispatcherMethod.addStatement("_dispatcher.dispatchOnEvent(_eventHandler, _eventState)");
    } else {
      eventDispatcherMethod
          .addStatement(
              "return ($T) _dispatcher.dispatchOnEvent(_eventHandler, _eventState)",
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
            .addTypeVariables(eventMethodModel.typeVariables)
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

    final String contextParamName = getContextParamName(specModel, eventMethodModel);

    for (int i = 0, size = eventMethodModel.methodParams.size(); i < size; i++) {
      final MethodParamModel methodParamModel = eventMethodModel.methodParams.get(i);

      final boolean hasParamAnnotation = isAnnotatedWith(methodParamModel, Param.class);
      if (hasParamAnnotation
          || isAnnotatedWith(methodParamModel, FromEvent.class)
          || methodParamModel.getTypeName().equals(specModel.getContextClass())) {

        TypeName type = methodParamModel.getTypeName();

        methodSpec.addParameter(
            parameter(
                type,
                methodParamModel.getName(),
                hasParamAnnotation
                    ? methodParamModel.getExternalAnnotations()
                    : Collections.emptyList()));
        delegation.add(methodParamModel.getName());
      } else if (hasLazyStateParams && methodParamModel instanceof StateParamModel) {
        delegation.add(
            "($T) stateContainer.$L", methodParamModel.getTypeName(), methodParamModel.getName());
      } else if (methodParamModel instanceof TreePropModel) {
        delegation.add(
            "useTreePropsFromContext() ? (($T) $L) : (($T) $L.$L)",
            methodParamModel.getTypeName(),
            contextParamName
                + ".getParentTreeProp("
                + TreePropGenerator.findTypeByTypeName(methodParamModel.getTypeName())
                + ".class)",
            methodParamModel.getTypeName(),
            REF_VARIABLE_NAME,
            methodParamModel.getName());
      } else {
        delegation.add(
            "($T) $L.$L",
            methodParamModel.getTypeName(),
            REF_VARIABLE_NAME,
            getImplAccessor(
                eventMethodModel.name.toString(), specModel, methodParamModel, contextParamName));
      }

      if (i < size - 1) {
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

  /** @return the name of the defined ComponentContext param on this event method. */
  static String getContextParamName(SpecModel specModel, SpecMethodModel eventMethodModel) {
    for (int i = 0, size = eventMethodModel.methodParams.size(); i < size; i++) {
      final ImmutableList<MethodParamModel> models = eventMethodModel.methodParams;
      final MethodParamModel methodParamModel = models.get(i);

      if ((methodParamModel.getAnnotations() == null || methodParamModel.getAnnotations().isEmpty())
          && methodParamModel.getTypeName().equals(specModel.getContextClass())) {
        return methodParamModel.getName();
      }
    }

    return null;
  }

  /** Generate a dispatchOnEvent() implementation for the component. */
  static MethodSpec generateDispatchOnEventImpl(SpecModel specModel) {
    final MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder("dispatchOnEventImpl")
            .addModifiers(Modifier.PROTECTED)
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
          generateEventHandlerFactory(
              eventMethodModel, specModel.getContextClass(), specModel.getComponentName()));
    }

    return typeSpecDataHolder.build();
  }

  static MethodSpec generateEventHandlerFactory(
      SpecMethodModel<EventMethod, EventDeclarationModel> eventMethodModel,
      TypeName paramClass,
      String componentName) {

    final Map.Entry<TypeName, List<TypeVariableName>> eventInfo = getEventInfo(eventMethodModel);

    final MethodSpec.Builder builder =
        MethodSpec.methodBuilder(eventMethodModel.name.toString())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariables(eventInfo.getValue())
            .addParameter(paramClass, "c")
            .returns(ParameterizedTypeName.get(ClassNames.EVENT_HANDLER, eventInfo.getKey()));

    final CodeBlock.Builder paramsBlock = CodeBlock.builder();

    paramsBlock.add("new Object[] {\n");
    paramsBlock.indent();
    paramsBlock.add("c,\n");

    for (MethodParamModel methodParamModel : eventMethodModel.methodParams) {
      if (isAnnotatedWith(methodParamModel, Param.class)) {
        builder.addParameter(parameter(methodParamModel));
        paramsBlock.add("$L,\n", methodParamModel.getName());
      }
    }

    paramsBlock.unindent();
    paramsBlock.add("}");

    builder.addStatement(
        "return newEventHandler($L.class, \"$L\", c, $L, $L)",
        componentName,
        componentName,
        eventMethodModel.name.toString().hashCode(),
        paramsBlock.build());

    return builder.build();
  }

  private static Map.Entry<TypeName, List<TypeVariableName>> getEventInfo(
      SpecMethodModel<EventMethod, EventDeclarationModel> model) {

    // If not a parameterised event type then immediately return the type
    if (!(model.typeModel.name instanceof ParameterizedTypeName)) {
      return new HashMap.SimpleEntry<>(model.typeModel.name, new ArrayList<>(model.typeVariables));
    }

    final ParameterizedTypeName eventType = (ParameterizedTypeName) model.typeModel.name;
    final List<TypeVariableName> methodVariables = new ArrayList<>(model.typeVariables);
    final List<TypeName> eventClassVariables = eventType.typeArguments;

    int numberOfOriginalTypeVariables = methodVariables.size();

    // Map the generic fields to their type variable name
    final Map<String, TypeName> fields = new HashMap<>();
    for (FieldModel field : model.typeModel.fields) {
      if (field.field.type instanceof TypeVariableName) {
        fields.put(field.field.name, field.field.type);
      }
    }

    // Fill the type variable list with the lower bounds
    final TypeName[] outputVariableTypes = new TypeName[eventClassVariables.size()];
    final Map<TypeName, Integer> positions = new HashMap<>();

    for (int i = 0; i < eventClassVariables.size(); i++) {
      TypeName v = eventClassVariables.get(i);
      if (v instanceof TypeVariableName) {
        List<TypeName> bounds = ((TypeVariableName) v).bounds;
        outputVariableTypes[i] =
            getNextTypeNameAndUpdateMethodVariables(
                bounds.isEmpty() ? OBJECT : bounds.get(0), methodVariables);
        positions.put(v, i);
      } else {
        throw new IllegalArgumentException("This should not be possible. _head in sand_");
      }
    }

    // Find usages of named generic fields
    model.methodParams.forEach(
        param -> {
          if (isAnnotatedWith(param, FromEvent.class) && fields.containsKey(param.getName())) {
            // Replace the type variable list with the new bounds
            final int position = positions.get(fields.get(param.getName()));
            if (param.getTypeName() instanceof TypeVariableName) {
              // If param is a type variable then use it
              outputVariableTypes[position] = param.getTypeName();
              // Remove the extra "T(n)" type variable
              methodVariables.set(numberOfOriginalTypeVariables + position, null);
            } else {
              // If param is concrete type then update the lower bounds of the type variable
              outputVariableTypes[position] =
                  updateTypeVariableBounds(
                      param.getTypeName(),
                      methodVariables,
                      numberOfOriginalTypeVariables + position);
            }
          }
        });

    return new HashMap.SimpleEntry<>(
        ParameterizedTypeName.get(eventType.rawType, outputVariableTypes),
        methodVariables.stream().filter(name -> name != null).collect(Collectors.toList()));
  }

  private static TypeName updateTypeVariableBounds(
      TypeName type, List<TypeVariableName> variables, int position) {
    TypeVariableName updated =
        TypeVariableName.get(variables.get(position).name, type.isPrimitive() ? type.box() : type);
    variables.set(position, updated);
    return updated;
  }

  private static TypeName getNextTypeNameAndUpdateMethodVariables(
      TypeName type, List<TypeVariableName> variables) {

    String name = "T";
    int i = 0;
    if (containsTypeVariable(variables, name)) {
      while (containsTypeVariable(variables, name + i)) {
        i++;
      }
      name = name + i;
    }

    final TypeVariableName variable = TypeVariableName.get(name, type);
    variables.add(variable);

    return variable;
  }

  private static boolean containsTypeVariable(List<TypeVariableName> variables, String name) {
    for (TypeVariableName variable : variables) {
      if (variable.name.equals(name)) {
        return true;
      }
    }
    return false;
  }
}
