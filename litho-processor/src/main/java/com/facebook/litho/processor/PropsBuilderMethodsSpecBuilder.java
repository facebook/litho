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
            "loadingEventHandler")
        .addStatement("super.setLoadingEventHandler(loadingEventHandler)")
        .addStatement("return this")
        .returns(mBuilderClass)
        .build();
  }

  private MethodSpec componentBuilder() {
    return builder(
        mPropParameter.parameter.name,
        Arrays.asList(parameter(mPropParameter.parameter.type, mPropParameter.parameter.name)),
        mPropParameter.parameter.name +
            " == null ? null : " +
            mPropParameter.parameter.name +
            ".makeShallowCopy()");
  }

  private MethodSpec regularBuilder(AnnotationSpec... extraAnnotations) {
    return builder(
        mPropParameter.parameter.name,
        Arrays.asList(parameter(
            mPropParameter.parameter.type,
            mPropParameter.parameter.name,
            extraAnnotations)),
        mPropParameter.parameter.name);
  }

  private MethodSpec resBuilder(ClassName annotationClassName, String resolver) {
    return builder(
        mPropParameter.parameter.name + "Res",
        Arrays.asList(parameter(TypeName.INT, "resId", annotation(annotationClassName))),
        "$L(resId)",
        resolver + "Res");
  }

  private MethodSpec resWithVarargsBuilder(
      ClassName annotationClassName,
      String resolver,
      TypeName varargsType,
      String varargsName) {
    return getMethodSpecBuilder(
        mPropParameter.parameter.name + "Res",
        Arrays.asList(
            parameter(TypeName.INT, "resId", annotation(annotationClassName)),
            ParameterSpec.builder(ArrayTypeName.of(varargsType), varargsName).build()),
        "$L(resId, " + varargsName + ")",
        resolver + "Res")
        .varargs(true)
        .build();
  }

  private List<MethodSpec> attrBuilders(ClassName annotationClassName, String resolver) {
    final List<MethodSpec> builders = new ArrayList<>();

    builders.add(builder(
        mPropParameter.parameter.name + "Attr",
        Arrays.asList(
            parameter(TypeName.INT, "attrResId", annotation(ClassNames.ATTR_RES)),
            parameter(TypeName.INT, "defResId", annotation(annotationClassName))),
        "$L(attrResId, defResId)",
        resolver + "Attr"));

    builders.add(builder(
        mPropParameter.parameter.name + "Attr",
        Arrays.asList(parameter(TypeName.INT, "attrResId", annotation(ClassNames.ATTR_RES))),
        "$L(attrResId, 0)",
        resolver + "Attr"));

    return builders;
  }

  private MethodSpec pxBuilder() {
    return builder(
        mPropParameter.parameter.name + "Px",
        Arrays.asList(parameter(
            mPropParameter.parameter.type,
            mPropParameter.parameter.name,
            annotation(ClassNames.PX))),
        mPropParameter.parameter.name);
  }

  private MethodSpec dipBuilder() {
    AnnotationSpec dipAnnotation = AnnotationSpec.builder(ClassNames.DIMENSION)
        .addMember("unit", "$T.DP", ClassNames.DIMENSION)
        .build();

    return builder(
        mPropParameter.parameter.name + "Dip",
        Arrays.asList(parameter(TypeName.FLOAT, "dips", dipAnnotation)),
        "dipsToPixels(dips)");
  }

  private MethodSpec sipBuilder() {
    AnnotationSpec spAnnotation = AnnotationSpec.builder(ClassNames.DIMENSION)
        .addMember("unit", "$T.SP", ClassNames.DIMENSION)
        .build();

    return builder(
        mPropParameter.parameter.name + "Sp",
        Arrays.asList(parameter(TypeName.FLOAT, "sips", spAnnotation)),
        "sipsToPixels(sips)");
  }

  private MethodSpec builderBuilder(ClassName builderClass) {

    return builder(
        mPropParameter.parameter.name,
        Arrays.asList(parameter(
            ParameterizedTypeName.get(builderClass, getBuilderGenericTypes()),
            mPropParameter.parameter.name + "Builder")),
        "$L.build()",
        mPropParameter.parameter.name + "Builder");
  }

  private TypeName[] getBuilderGenericTypes() {
    final List<TypeName> typeParameters = JPUtil.getTypeParameters(mPropParameter.parameter.type);
    final TypeName typeParameter = typeParameters == null
        ? WildcardTypeName.subtypeOf(ClassNames.COMPONENT_LIFECYCLE)
        : typeParameters.get(0);

    return new TypeName[]{typeParameter};
  }

  private ParameterSpec parameter(
      TypeName type,
      String name,
      AnnotationSpec... extraAnnotations) {
    final ParameterSpec.Builder builder = ParameterSpec.builder(type, name);

    for (ClassName annotation : mPropParameter.annotations) {
      builder.addAnnotation(annotation);
    }

    for (AnnotationSpec annotation : extraAnnotations) {
      builder.addAnnotation(annotation);
    }

    return builder.build();
  }

  private static AnnotationSpec annotation(ClassName className) {
    return AnnotationSpec.builder(className).build();
  }

  private MethodSpec.Builder getMethodSpecBuilder(
      String name,
      List<ParameterSpec> parameters,
      String statement,
      Object ...formatObjects) {
    final MethodSpec.Builder builder =
        MethodSpec.methodBuilder(name)
            .addModifiers(Modifier.PUBLIC)
            .returns(mBuilderClass)
            .addCode("this.$L.$L = ", mImplName, mPropParameter.parameter.name)
            .addStatement(statement, formatObjects);

    for (ParameterSpec param : parameters) {
      builder.addParameter(param);
    }

    if (!mPropParameter.optional) {
      builder.addStatement("$L.set($L)", mRequiredSetName, mIndex);
    }

    builder.addStatement("return this");

    return builder;
  }

  private MethodSpec builder(
      String name,
      List<ParameterSpec> parameters,
      String statement,
      Object ...formatObjects) {
    return getMethodSpecBuilder(name, parameters, statement, formatObjects).build();
