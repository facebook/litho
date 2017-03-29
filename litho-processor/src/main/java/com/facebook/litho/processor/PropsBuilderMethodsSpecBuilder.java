/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor;

import javax.lang.model.element.Modifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.facebook.litho.javapoet.JPUtil;
import com.facebook.litho.specmodels.model.ClassNames;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;

class PropsBuilderMethodsSpecBuilder {
  private int mIndex;
  private PropParameter mPropParameter;
  private String mImplName;
  private String mRequiredSetName;
  private TypeName mBuilderClass;
  private ClassName mComponentClass;

  PropsBuilderMethodsSpecBuilder index(int index) {
    this.mIndex = index;
    return this;
  }

  PropsBuilderMethodsSpecBuilder propParameter(PropParameter propParameter) {
    this.mPropParameter = propParameter;
    return this;
  }

  PropsBuilderMethodsSpecBuilder componentClassName(ClassName componentClass) {
    this.mComponentClass = componentClass;
    return this;
  }

  PropsBuilderMethodsSpecBuilder implName(String implName) {
    this.mImplName = implName;
    return this;
  }

  PropsBuilderMethodsSpecBuilder requiredSetName(String requiredSetName) {
    this.mRequiredSetName = requiredSetName;
    return this;
  }

  PropsBuilderMethodsSpecBuilder builderClass(TypeName builderClass) {
    this.mBuilderClass = builderClass;
    return this;
  }

  List<MethodSpec> build() {
    final List<MethodSpec> builders = new ArrayList<>();

    switch (mPropParameter.resType) {
      case STRING:
        builders.add(regularBuilder());
        builders.add(resBuilder(ClassNames.STRING_RES, "resolveString"));
        builders.add(resWithVarargsBuilder(
            ClassNames.STRING_RES,
            "resolveString",
            TypeName.OBJECT,
            "formatArgs"));
        builders.addAll(attrBuilders(ClassNames.STRING_RES, "resolveString"));
        break;
      case STRING_ARRAY:
        builders.add(regularBuilder());
        builders.add(resBuilder(ClassNames.ARRAY_RES, "resolveStringArray"));
        builders.addAll(attrBuilders(ClassNames.ARRAY_RES, "resolveStringArray"));
        break;
      case INT:
        builders.add(regularBuilder());
        builders.add(resBuilder(ClassNames.INT_RES, "resolveInt"));
        builders.addAll(attrBuilders(ClassNames.INT_RES, "resolveInt"));
        break;
      case INT_ARRAY:
        builders.add(regularBuilder());
        builders.add(resBuilder(ClassNames.ARRAY_RES, "resolveIntArray"));
        builders.addAll(attrBuilders(ClassNames.ARRAY_RES, "resolveIntArray"));
        break;
      case BOOL:
        builders.add(regularBuilder());
        builders.add(resBuilder(ClassNames.BOOL_RES, "resolveBool"));
        builders.addAll(attrBuilders(ClassNames.BOOL_RES, "resolveBool"));
        break;
      case COLOR:
        builders.add(regularBuilder(annotation(ClassNames.COLOR_INT)));
        builders.add(resBuilder(ClassNames.COLOR_RES, "resolveColor"));
        builders.addAll(attrBuilders(ClassNames.COLOR_RES, "resolveColor"));
        break;
      case DIMEN_SIZE:
        builders.add(pxBuilder());
        builders.add(resBuilder(ClassNames.DIMEN_RES, "resolveDimenSize"));
        builders.addAll(attrBuilders(ClassNames.DIMEN_RES, "resolveDimenSize"));
        builders.add(dipBuilder());
        break;
      case DIMEN_TEXT:
        builders.add(pxBuilder());
        builders.add(resBuilder(ClassNames.DIMEN_RES, "resolveDimenSize"));
        builders.addAll(attrBuilders(ClassNames.DIMEN_RES, "resolveDimenSize"));
        builders.add(dipBuilder());
        builders.add(sipBuilder());
        break;
      case DIMEN_OFFSET:
        builders.add(pxBuilder());
        builders.add(resBuilder(ClassNames.DIMEN_RES, "resolveDimenOffset"));
        builders.addAll(attrBuilders(ClassNames.DIMEN_RES, "resolveDimenOffset"));
        builders.add(dipBuilder());
        break;
      case FLOAT:
        builders.add(regularBuilder());
        builders.add(resBuilder(ClassNames.DIMEN_RES, "resolveFloat"));
        builders.addAll(attrBuilders(ClassNames.DIMEN_RES, "resolveFloat"));
        break;
      case DRAWABLE:
        builders.add(regularBuilder());
        builders.add(resBuilder(ClassNames.DRAWABLE_RES, "resolveDrawable"));
        builders.addAll(attrBuilders(ClassNames.DRAWABLE_RES, "resolveDrawable"));
        break;
      case NONE:
        if (mPropParameter.parameter.type.equals(mComponentClass)) {
          builders.add(componentBuilder());
        } else {
          builders.add(regularBuilder());
        }
        break;
    }

    if (JPUtil.getRawType(mPropParameter.parameter.type).equals(ClassNames.COMPONENT)) {
      builders.add(builderBuilder(ClassNames.COMPONENT_BUILDER));
    }

    if (JPUtil.getRawType(mPropParameter.parameter.type).equals(ClassNames.REFERENCE)) {
      builders.add(builderBuilder(ClassNames.REFERENCE_BUILDER));
    }

    return builders;
  }

  MethodSpec buildKeySetter() {
    return MethodSpec.methodBuilder("key")
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ClassNames.STRING, "key")
        .addStatement("super.setKey(key)")
        .addStatement("return this")
        .returns(mBuilderClass)
        .build();
  }

  MethodSpec buildLoadingEventHandlerSetter() {
    return MethodSpec.methodBuilder("loadingEventHandler")
        .addModifiers(Modifier.PUBLIC)
        .addParameter(
            ParameterizedTypeName.get(
                ClassNames.EVENT_HANDLER,
                SectionClassNames.LOADING_EVENT_HANDLER),
