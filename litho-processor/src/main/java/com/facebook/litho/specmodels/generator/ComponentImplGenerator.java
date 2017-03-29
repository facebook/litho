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
