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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.facebook.common.internal.ImmutableList;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.InterStageInputParamModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.facebook.litho.specmodels.model.StateParamModel;
import com.facebook.litho.specmodels.model.TreePropModel;
import com.facebook.litho.specmodels.model.UpdateStateMethodModel;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import static com.facebook.litho.specmodels.model.ClassNames.COMPONENT;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.IMPL_CLASS_NAME_SUFFIX;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.STATE_CONTAINER_FIELD_NAME;

/**
 * Class that generates the preamble for a Component.
 */
public class ComponentImplGenerator {

  private ComponentImplGenerator() {
  }

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    final String implClassName = getImplClassName(specModel);
    final TypeSpec.Builder implClassBuilder =
        TypeSpec.classBuilder(implClassName)
            .addModifiers(Modifier.PRIVATE)
            .superclass(
                ParameterizedTypeName.get(
                    ClassNames.COMPONENT,
                    specModel.getComponentTypeName()))
            .addSuperinterface(Cloneable.class);

    if (!specModel.hasInjectedDependencies()) {
      implClassBuilder.addModifiers(Modifier.STATIC);
      implClassBuilder.addTypeVariables(specModel.getTypeVariables());
    }

    final ClassName stateContainerImplClass =
        ClassName.bestGuess(getStateContainerImplClassName(specModel));
    implClassBuilder.addField(stateContainerImplClass, STATE_CONTAINER_FIELD_NAME);
    implClassBuilder.addMethod(
        generateStateContainerGetter(specModel.getStateContainerClass()));

    generateProps(specModel).addToTypeSpec(implClassBuilder);
    generateTreeProps(specModel).addToTypeSpec(implClassBuilder);
    generateInterStageInputs(specModel).addToTypeSpec(implClassBuilder);
    generateEventHandlers(specModel).addToTypeSpec(implClassBuilder);

    implClassBuilder.addMethod(generateImplConstructor(stateContainerImplClass));
    implClassBuilder.addMethod(generateGetSimpleName(specModel));
    implClassBuilder.addMethod(generateEqualsMethod(specModel, true));

    generateCopyInterStageImpl(specModel).addToTypeSpec(implClassBuilder);

    generateOnUpdateStateMethods(specModel).addToTypeSpec(implClassBuilder);
    generateMakeShallowCopy(specModel, /* hasDeepCopy */ false).addToTypeSpec(implClassBuilder);

    return TypeSpecDataHolder.newBuilder()
        .addType(generateStateContainerImpl(specModel))
        .addType(implClassBuilder.build())
        .build();
  }

  static TypeSpec generateStateContainerImpl(SpecModel specModel) {
    final TypeSpec.Builder stateContainerImplClassBuilder =
        TypeSpec.classBuilder(getStateContainerImplClassName(specModel))
            .addSuperinterface(specModel.getStateContainerClass());

    if (!specModel.hasInjectedDependencies()) {
      stateContainerImplClassBuilder.addModifiers(Modifier.STATIC, Modifier.PRIVATE);
      stateContainerImplClassBuilder.addTypeVariables(specModel.getTypeVariables());
    }

    for (StateParamModel stateValue : specModel.getStateValues()) {
      stateContainerImplClassBuilder.addField(FieldSpec.builder(
          stateValue.getType(),
          stateValue.getName()).addAnnotation(State.class).build());
    }

    return stateContainerImplClassBuilder.build();
  }

  static String getImplClassName(SpecModel specModel) {
    return specModel.getComponentName() + IMPL_CLASS_NAME_SUFFIX;
  }

  static String getImplInstanceName(SpecModel specModel) {
    final String implClassName = getImplClassName(specModel);
    return implClassName.substring(0, 1).toLowerCase(Locale.ROOT) + implClassName.substring(1);
  }

  static String getStateContainerImplClassName(SpecModel specModel) {
    return specModel.getComponentName() + GeneratorConstants.STATE_CONTAINER_IMPL_NAME_SUFFIX;
  }

  static MethodSpec generateStateContainerGetter(TypeName stateContainerClassName) {
    return MethodSpec.methodBuilder("getStateContainer")
        .addModifiers(Modifier.PROTECTED)
        .addAnnotation(Override.class)
        .returns(stateContainerClassName)
        .addStatement("return " + GeneratorConstants.STATE_CONTAINER_FIELD_NAME)
        .build();
  }

  static TypeSpecDataHolder generateProps(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    final ImmutableList<PropModel> props = specModel.getProps();

    for (PropModel prop : props) {
      final FieldSpec.Builder fieldBuilder = FieldSpec.builder(prop.getType(), prop.getName())
          .addAnnotation(Prop.class);
      if (prop.hasDefault(specModel.getPropDefaults())) {
        fieldBuilder.initializer("$L.$L", specModel.getSpecName(), prop.getName());
      }

      typeSpecDataHolder.addField(fieldBuilder.build());
    }

    return typeSpecDataHolder.build();
  }

  static TypeSpecDataHolder generateTreeProps(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    final ImmutableList<TreePropModel> treeProps = specModel.getTreeProps();

    for (TreePropModel treeProp : treeProps) {
      typeSpecDataHolder.addField(
          FieldSpec.builder(treeProp.getType(), treeProp.getName()).build());
    }

    return typeSpecDataHolder.build();
  }

  static TypeSpecDataHolder generateInterStageInputs(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    final ImmutableList<InterStageInputParamModel> interStageInputs =
        specModel.getInterStageInputs();

    for (InterStageInputParamModel interStageInput : interStageInputs) {
      typeSpecDataHolder.addField(
          FieldSpec.builder(interStageInput.getType(), interStageInput.getName()).build());
    }

    return typeSpecDataHolder.build();
  }

  static TypeSpecDataHolder generateEventHandlers(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();

    for (EventDeclarationModel eventDeclaration : specModel.getEventDeclarations()) {
      typeSpecDataHolder.addField(
          FieldSpec.builder(
              ClassNames.EVENT_HANDLER,
              getEventHandlerInstanceName(eventDeclaration.name))
              .build());
    }

    return typeSpecDataHolder.build();
  }

  static String getEventHandlerInstanceName(ClassName eventHandlerClassName) {
    final String eventHandlerName = eventHandlerClassName.simpleName();
    return eventHandlerName.substring(0, 1).toLowerCase(Locale.ROOT) +
        eventHandlerName.substring(1) +
        "Handler";
  }

  static MethodSpec generateImplConstructor(TypeName stateContainerImplClass) {
    return MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PRIVATE)
        .addStatement("super(get())")
        .addStatement(STATE_CONTAINER_FIELD_NAME + " = new $T()", stateContainerImplClass)
        .build();
  }

  static MethodSpec generateGetSimpleName(SpecModel specModel) {
    return MethodSpec.methodBuilder("getSimpleName")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(ClassNames.STRING)
        .addStatement("return \"" + specModel.getComponentName() + "\"")
        .build();
  }

  static MethodSpec generateEqualsMethod(SpecModel specModel, boolean shouldCheckId) {
    final String implClassName = getImplClassName(specModel);
    final String implInstanceName = getImplInstanceName(specModel);

    MethodSpec.Builder equalsBuilder = MethodSpec.methodBuilder("equals")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(TypeName.BOOLEAN)
        .addParameter(TypeName.OBJECT, "other")
        .beginControlFlow("if (this == other)")
        .addStatement("return true")
        .endControlFlow()
        .beginControlFlow("if (other == null || getClass() != other.getClass())")
        .addStatement("return false")
        .endControlFlow()
        .addStatement(implClassName + " " + implInstanceName + " = (" + implClassName + ") other");

    if (shouldCheckId) {
      equalsBuilder
          .beginControlFlow("if (this.getId() == " + implInstanceName + ".getId())")
          .addStatement("return true")
          .endControlFlow();
    }

    for (PropModel prop : specModel.getProps()) {
      equalsBuilder.addCode(getCompareStatement(specModel, implInstanceName, prop));
    }

    for (StateParamModel state : specModel.getStateValues()) {
      equalsBuilder.addCode(getCompareStatement(specModel, implInstanceName, state));
    }

    for (TreePropModel treeProp : specModel.getTreeProps()) {
      equalsBuilder.addCode(getCompareStatement(specModel, implInstanceName, treeProp));
    }

    equalsBuilder.addStatement("return true");

    return equalsBuilder.build();
  }

  static TypeSpecDataHolder generateCopyInterStageImpl(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();
    final ImmutableList<InterStageInputParamModel> interStageInputs =
        specModel.getInterStageInputs();

    if (!interStageInputs.isEmpty()) {
      final String implClassName = getImplClassName(specModel);
      final String implInstanceName = getImplInstanceName(specModel);
      final MethodSpec.Builder copyInterStageComponentBuilder = MethodSpec
          .methodBuilder("copyInterStageImpl")
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PROTECTED)
          .returns(TypeName.VOID)
          .addParameter(
              ParameterizedTypeName.get(
                  ClassNames.COMPONENT,
                  specModel.getComponentTypeName()),
              "impl")
          .addStatement(
              "$L " + implInstanceName + " = ($L) impl",
              implClassName,
              implClassName);

      for (InterStageInputParamModel interStageInput : interStageInputs) {
        copyInterStageComponentBuilder
            .addStatement(
                "$L = " + implInstanceName + ".$L",
                interStageInput.getName(),
                interStageInput.getName());
      }

      typeSpecDataHolder.addMethod(copyInterStageComponentBuilder.build());
    }

    return typeSpecDataHolder.build();
  }

  static TypeSpecDataHolder generateOnUpdateStateMethods(SpecModel specModel) {
    TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();

    for (UpdateStateMethodModel updateStateMethodModel : specModel.getUpdateStateMethods()) {
      final String stateUpdateClassName = getStateUpdateClassName(updateStateMethodModel);
      final List<MethodParamModel> params = getParams(updateStateMethodModel);

      final MethodSpec.Builder methodSpecBuilder = MethodSpec
          .methodBuilder("create" + stateUpdateClassName)
          .addModifiers(Modifier.PRIVATE)
          .returns(ClassName.bestGuess(stateUpdateClassName));

      for (MethodParamModel param : params) {
        methodSpecBuilder
            .addParameter(ParameterSpec.builder(param.getType(), param.getName()).build());
      }

      final CodeBlock.Builder constructor = CodeBlock.builder();
      constructor.add("return new " + stateUpdateClassName + "(");

      for (int i = 0, size = params.size(); i < size; i++) {
        constructor.add(params.get(i).getName());
        if (i < params.size() - 1) {
          constructor.add(", ");
        }
      }
      constructor.add(");\n");

      methodSpecBuilder.addCode(constructor.build());
      typeSpecDataHolder.addMethod(methodSpecBuilder.build());
    }

    return typeSpecDataHolder.build();
  }

  static TypeSpecDataHolder generateMakeShallowCopy(SpecModel specModel, boolean hasDeepCopy) {
    TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();

    final List<MethodParamModel> componentsInImpl = findComponentsInImpl(specModel);
    final ImmutableList<InterStageInputParamModel> interStageComponentVariables =
        specModel.getInterStageInputs();
    final ImmutableList<UpdateStateMethodModel> updateStateMethodModels =
        specModel.getUpdateStateMethods();

    if (componentsInImpl.isEmpty() &&
        interStageComponentVariables.isEmpty() &&
        updateStateMethodModels.isEmpty()) {
      return typeSpecDataHolder.build();
    }

    final String implClassName = getImplClassName(specModel);
    MethodSpec.Builder builder = MethodSpec.methodBuilder("makeShallowCopy")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(ClassName.bestGuess(implClassName));

    String deepCopy = hasDeepCopy ? "deepCopy" : "";

    if (hasDeepCopy) {
      builder.addParameter(ParameterSpec.builder(TypeName.BOOLEAN, "deepCopy").build());
    }

    builder.addStatement(
        "$L $L = ($L) super.makeShallowCopy($L)",
        implClassName,
        "component",
        implClassName,
        deepCopy);

    for (MethodParamModel componentParam : componentsInImpl) {
      builder.addStatement(
          "component.$L = component.$L != null ? component.$L.makeShallowCopy($L) : null",
          componentParam.getName(),
          componentParam.getName(),
          componentParam.getName(),
          deepCopy);
    }

    if (hasDeepCopy) {
      builder.beginControlFlow("if (!deepCopy)");
    }

    for (InterStageInputParamModel interStageInput : specModel.getInterStageInputs()) {
      builder.addStatement("component.$L = null", interStageInput.getName());
    }

    final String stateContainerImplClassName = getStateContainerImplClassName(specModel);
    if (stateContainerImplClassName != null) {
      builder.addStatement(
          "component." + GeneratorConstants.STATE_CONTAINER_FIELD_NAME + " = new $T()",
          ClassName.bestGuess(stateContainerImplClassName));
    }

    if (hasDeepCopy) {
      builder.endControlFlow();
    }

    builder.addStatement("return component");

    return typeSpecDataHolder.addMethod(builder.build()).build();
  }

  private static List<MethodParamModel> findComponentsInImpl(SpecModel specModel) {
    final List<MethodParamModel> componentsInImpl = new ArrayList<>();

    for (PropModel prop : specModel.getProps()) {
      final TypeName typeName = prop.getType();
      if (typeName.equals(ClassNames.COMPONENT) ||
          (typeName instanceof ParameterizedTypeName &&
              ((ParameterizedTypeName) typeName).rawType.equals(COMPONENT))) {
        componentsInImpl.add(prop);
      }
    }

    return componentsInImpl;
  }

  private static List<MethodParamModel> getParams(UpdateStateMethodModel updateStateMethodModel) {
    final List<MethodParamModel> params = new ArrayList<>();
    for (MethodParamModel methodParamModel : updateStateMethodModel.methodParams) {
      for (Annotation annotation : methodParamModel.getAnnotations()) {
        if (annotation.annotationType().equals(Param.class)) {
          params.add(methodParamModel);
          break;
        }
      }
    }

    return params;
  }

  private static String getStateUpdateClassName(UpdateStateMethodModel updateStateMethodModel) {
    String methodName = updateStateMethodModel.name.toString();
    return methodName.substring(0, 1).toUpperCase(Locale.ROOT) +
        methodName.substring(1) +
        GeneratorConstants.STATE_UPDATE_IMPL_NAME_SUFFIX;
  }

  private static CodeBlock getCompareStatement(
      SpecModel specModel,
      String implInstanceName,
      MethodParamModel field) {
    final CodeBlock.Builder codeBlock = CodeBlock.builder();

    final String implAccessor = getImplAccessor(specModel, field);
    if (field.getType() == TypeName.FLOAT) {
      codeBlock
          .beginControlFlow(
              "if (Float.compare($L, $L.$L) != 0)",
              implAccessor,
              implInstanceName,
              implAccessor)
          .addStatement("return false")
          .endControlFlow();
    } else if (field.getType() == TypeName.DOUBLE) {
      codeBlock
          .beginControlFlow(
              "if (Double.compare($L, $L.$L) != 0)",
              implAccessor,
              implInstanceName,
              implAccessor)
          .addStatement("return false")
          .endControlFlow();
    } else if (field.getType() instanceof ArrayTypeName) {
      codeBlock
          .beginControlFlow(
              "if (!Arrays.equals($L, $L.$L))",
              implAccessor,
              implInstanceName,
              implAccessor)
          .addStatement("return false")
          .endControlFlow();
    } else if (field.getType().isPrimitive()) {
      codeBlock
          .beginControlFlow(
              "if ($L != $L.$L)",
              implAccessor,
              implInstanceName,
              implAccessor)
          .addStatement("return false")
          .endControlFlow();
    } else if (field.getType().equals(ClassNames.REFERENCE)) {
      codeBlock
          .beginControlFlow(
              "if (Reference.shouldUpdate($L != $L.$L))",
              implAccessor,
              implInstanceName,
              implAccessor)
          .addStatement("return false")
          .endControlFlow();
    } else {
      codeBlock
          .beginControlFlow(
              "if ($L != null ? !$L.equals($L.$L) : $L.$L != null)",
              implAccessor,
              implAccessor,
              implInstanceName,
              implAccessor,
              implInstanceName,
              implAccessor)
          .addStatement("return false")
          .endControlFlow();
    }

    return codeBlock.build();
  }

  static String getImplAccessor(SpecModel specModel, MethodParamModel methodParamModel) {
    if (methodParamModel instanceof StateParamModel ||
        SpecModelUtils.getStateValueWithName(specModel, methodParamModel.getName()) != null) {
      return STATE_CONTAINER_FIELD_NAME + "." + methodParamModel.getName();
    }

    return methodParamModel.getName();
  }
}
