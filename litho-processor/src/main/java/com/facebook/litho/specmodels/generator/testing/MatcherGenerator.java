/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator.testing;

import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.HasEnclosedSpecModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;

/** Class that generates the matcher builder for a test Component. */
public final class MatcherGenerator {

  private static final String BUILDER = "Matcher";
  private static final ClassName BUILDER_CLASS_NAME = ClassName.bestGuess(BUILDER);

  private MatcherGenerator() {}

  public static <T extends SpecModel & HasEnclosedSpecModel> TypeSpecDataHolder generate(
      T specModel) {
    return TypeSpecDataHolder.newBuilder()
        .addTypeSpecDataHolder(generateFactoryMethods(specModel))
        .addTypeSpecDataHolder(generateBuilder(specModel))
        .build();
  }

  private static TypeSpecDataHolder generateFactoryMethods(final SpecModel specModel) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    final MethodSpec.Builder factoryMethod =
        MethodSpec.methodBuilder("matcher")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(getMatcherType(specModel))
            .addParameter(specModel.getContextClass(), "c")
            .addStatement("return new $T(c)", BUILDER_CLASS_NAME);

    if (!specModel.getTypeVariables().isEmpty()) {
      factoryMethod.addTypeVariables(specModel.getTypeVariables());
    }

    return dataHolder.addMethod(factoryMethod.build()).build();
  }

  private static MethodSpec generateGetThisMethod(SpecModel specModel) {
    return MethodSpec.methodBuilder("getThis")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addStatement("return this")
        .returns(getMatcherType(specModel))
        .build();
  }

  private static <T extends SpecModel & HasEnclosedSpecModel> TypeSpecDataHolder generateBuilder(
      final T specModel) {
    final TypeSpec.Builder propsBuilderClassBuilder =
        TypeSpec.classBuilder(BUILDER)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .superclass(
                ParameterizedTypeName.get(ClassNames.BASE_MATCHER, getMatcherType(specModel)));

    if (!specModel.getTypeVariables().isEmpty()) {
      propsBuilderClassBuilder.addTypeVariables(specModel.getTypeVariables());
    }

    final MethodSpec constructor =
        MethodSpec.constructorBuilder()
            .addParameter(specModel.getContextClass(), "c")
            .addStatement("super.init(c, c.getResourceCache())")
            .build();

    propsBuilderClassBuilder.addMethod(constructor);

    for (final PropModel prop : specModel.getProps()) {
      generatePropsBuilderMethods(specModel, prop).addToTypeSpec(propsBuilderClassBuilder);
    }

    propsBuilderClassBuilder
        .addMethod(generateBuildMethod(specModel))
        .addMethod(generateGetThisMethod(specModel));

    return TypeSpecDataHolder.newBuilder().addType(propsBuilderClassBuilder.build()).build();
  }

  private static TypeSpecDataHolder generatePropsBuilderMethods(
      SpecModel specModel, final PropModel prop) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    if (getRawType(prop.getTypeName()).equals(ClassNames.COMPONENT)) {
      dataHolder.addField(matcherComponentFieldBuilder(prop));
      dataHolder.addMethod(matcherComponentFieldSetterBuilder(specModel, prop));
    }

    dataHolder.addField(matcherFieldBuilder(prop));
    dataHolder.addMethod(matcherFieldSetterBuilder(specModel, prop));

    if (prop.hasVarArgs()) {
      dataHolder.addMethod(varArgBuilder(specModel, prop));
      final ParameterizedTypeName type = (ParameterizedTypeName) prop.getTypeName();
      if (getRawType(type.typeArguments.get(0)).equals(ClassNames.COMPONENT)) {
        dataHolder.addMethod(varArgBuilderBuilder(specModel, prop));
      }
      // fall through to generate builder method for List<T>
    }

    switch (prop.getResType()) {
      case STRING:
        dataHolder.addMethod(regularBuilder(specModel, prop));
        dataHolder.addMethod(resBuilder(specModel, prop, ClassNames.STRING_RES, "resolveString"));
        dataHolder.addMethod(
            resWithVarargsBuilder(
                specModel,
                prop,
                ClassNames.STRING_RES,
                "resolveString",
                TypeName.OBJECT,
                "formatArgs"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, ClassNames.STRING_RES, "resolveString"));
        break;
      case STRING_ARRAY:
        dataHolder.addMethod(regularBuilder(specModel, prop));
        dataHolder.addMethod(
            resBuilder(specModel, prop, ClassNames.ARRAY_RES, "resolveStringArray"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, ClassNames.ARRAY_RES, "resolveStringArray"));
        break;
      case INT:
        dataHolder.addMethod(regularBuilder(specModel, prop));
        dataHolder.addMethod(resBuilder(specModel, prop, ClassNames.INT_RES, "resolveInt"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, ClassNames.INT_RES, "resolveInt"));
        break;
      case INT_ARRAY:
        dataHolder.addMethod(regularBuilder(specModel, prop));
        dataHolder.addMethod(resBuilder(specModel, prop, ClassNames.ARRAY_RES, "resolveIntArray"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, ClassNames.ARRAY_RES, "resolveIntArray"));
        break;
      case BOOL:
        dataHolder.addMethod(regularBuilder(specModel, prop));
        dataHolder.addMethod(resBuilder(specModel, prop, ClassNames.BOOL_RES, "resolveBool"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, ClassNames.BOOL_RES, "resolveBool"));
        break;
      case COLOR:
        dataHolder.addMethod(regularBuilder(specModel, prop, annotation(ClassNames.COLOR_INT)));
        dataHolder.addMethod(resBuilder(specModel, prop, ClassNames.COLOR_RES, "resolveColor"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, ClassNames.COLOR_RES, "resolveColor"));
        break;
      case DIMEN_SIZE:
        dataHolder.addMethod(pxBuilder(specModel, prop));
        dataHolder.addMethod(resBuilder(specModel, prop, ClassNames.DIMEN_RES, "resolveDimenSize"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, ClassNames.DIMEN_RES, "resolveDimenSize"));
        dataHolder.addMethod(dipBuilder(specModel, prop));
        break;
      case DIMEN_TEXT:
        dataHolder.addMethod(pxBuilder(specModel, prop));
        dataHolder.addMethod(resBuilder(specModel, prop, ClassNames.DIMEN_RES, "resolveDimenSize"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, ClassNames.DIMEN_RES, "resolveDimenSize"));
        dataHolder.addMethod(dipBuilder(specModel, prop));
        dataHolder.addMethod(sipBuilder(specModel, prop));
        break;
      case DIMEN_OFFSET:
        dataHolder.addMethod(pxBuilder(specModel, prop));
        dataHolder.addMethod(
            resBuilder(specModel, prop, ClassNames.DIMEN_RES, "resolveDimenOffset"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, ClassNames.DIMEN_RES, "resolveDimenOffset"));
        dataHolder.addMethod(dipBuilder(specModel, prop));
        break;
      case FLOAT:
        dataHolder.addMethod(regularBuilder(specModel, prop));
        dataHolder.addMethod(resBuilder(specModel, prop, ClassNames.DIMEN_RES, "resolveFloat"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, ClassNames.DIMEN_RES, "resolveFloat"));
        break;
      case DRAWABLE:
        dataHolder.addMethod(regularBuilder(specModel, prop));
        dataHolder.addMethod(
            resBuilder(specModel, prop, ClassNames.DRAWABLE_RES, "resolveDrawable"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, ClassNames.DRAWABLE_RES, "resolveDrawable"));
        break;
      case NONE:
        dataHolder.addMethod(regularBuilder(specModel, prop));
        break;
    }

    if (getRawType(prop.getTypeName()).equals(ClassNames.COMPONENT)) {
      dataHolder.addMethod(builderBuilder(specModel, prop, ClassNames.COMPONENT_BUILDER));
    }

    if (getRawType(prop.getTypeName()).equals(ClassNames.REFERENCE)) {
      dataHolder.addMethod(builderBuilder(specModel, prop, ClassNames.REFERENCE_BUILDER));
    }

    return dataHolder.build();
  }

  private static TypeName getRawType(final TypeName type) {
    return type instanceof ParameterizedTypeName ? ((ParameterizedTypeName) type).rawType : type;
  }

  private static MethodSpec matcherFieldSetterBuilder(SpecModel specModel, PropModel prop) {
    final String propMatcherName = getPropMatcherName(prop);
    final String propName = prop.getName();

    return getMethodSpecBuilder(
            specModel,
            ImmutableList.of(ParameterSpec.builder(getPropMatcherType(prop), "matcher").build()),
            CodeBlock.builder()
                .addStatement(
                    "$L = ($L) matcher", propMatcherName, getPropMatcherType(prop).toString())
                .build(),
            propName)
        .build();
  }

  private static MethodSpec matcherComponentFieldSetterBuilder(
      SpecModel specModel, PropModel prop) {
    final String propMatcherName = getPropComponentMatcherName(prop);
    final String propName = prop.getName();

    return getMethodSpecBuilder(
            specModel,
            ImmutableList.of(
                ParameterSpec.builder(getMatcherConditionTypeName(), "matcher").build()),
            CodeBlock.builder().addStatement("$L = matcher", propMatcherName).build(),
            propName)
        .build();
  }

  private static MethodSpec regularBuilder(
      SpecModel specModel, final PropModel prop, final AnnotationSpec... extraAnnotations) {
    return builder(
        specModel,
        prop,
        prop.getName(),
        Collections.singletonList(
            parameter(prop, prop.getTypeName(), prop.getName(), extraAnnotations)),
        prop.getName());
  }

  private static FieldSpec matcherComponentFieldBuilder(final PropModel prop) {
    return FieldSpec.builder(getMatcherConditionTypeName(), getPropComponentMatcherName(prop))
        .addAnnotation(Nullable.class)
        .build();
  }

  private static TypeName getMatcherConditionTypeName() {
    return ParameterizedTypeName.get(
        ClassNames.ASSERTJ_CONDITION, ClassNames.INSPECTABLE_COMPONENT);
  }

  private static FieldSpec matcherFieldBuilder(final PropModel prop) {
    return FieldSpec.builder(getPropMatcherType(prop), getPropMatcherName(prop))
        .addAnnotation(Nullable.class)
        .build();
  }

  private static ParameterizedTypeName getPropMatcherType(PropModel prop) {
    final TypeName rawType = getRawType(prop.getTypeName());

    // We can only match against unparameterized (i.e. raw) types. Thanks, Java.
    return ParameterizedTypeName.get(ClassNames.HAMCREST_MATCHER, rawType.box());
  }

  static String getPropComponentMatcherName(final PropModel prop) {
    return getBasePropMatcherName(prop, "ComponentMatcher");
  }

  static String getPropMatcherName(final PropModel prop) {
    return getBasePropMatcherName(prop, "Matcher");
  }

  private static String getBasePropMatcherName(final PropModel prop, final String suffix) {
    final String name = prop.getName();

    final int fst = Character.toUpperCase(name.codePointAt(0));

    return 'm'
        + String.copyValueOf(Character.toChars(fst))
        + name.substring(name.offsetByCodePoints(0, 1))
        + suffix;
  }

  private static TypeName getMatcherType(SpecModel specModel) {
    return (specModel.getTypeVariables().isEmpty())
        ? BUILDER_CLASS_NAME
        : ParameterizedTypeName.get(
            BUILDER_CLASS_NAME,
            specModel
                .getTypeVariables()
                .toArray(new TypeName[specModel.getTypeVariables().size()]));
  }

  private static MethodSpec varArgBuilder(
      SpecModel specModel, final PropModel prop, final AnnotationSpec... extraAnnotations) {
    final ParameterizedTypeName parameterizedTypeName = (ParameterizedTypeName) prop.getTypeName();
    final TypeName singleParameterType = parameterizedTypeName.typeArguments.get(0);
    final String varArgName = prop.getVarArgsSingleName();

    final CodeBlock codeBlock =
        CodeBlock.builder()
            .beginControlFlow("if ($L == null)", varArgName)
            .addStatement("return this")
            .endControlFlow()
            .build();

    return getMethodSpecBuilder(
            specModel,
            Collections.singletonList(
                parameter(prop, singleParameterType, varArgName, extraAnnotations)),
            codeBlock,
            varArgName)
        .build();
  }

  private static MethodSpec varArgBuilderBuilder(SpecModel specModel, final PropModel prop) {
    final String varArgName = prop.getVarArgsSingleName();
    final ParameterizedTypeName varArgType = (ParameterizedTypeName) prop.getTypeName();
    final TypeName internalType = varArgType.typeArguments.get(0);
    final CodeBlock codeBlock =
        CodeBlock.builder()
            .addStatement("$L($L.build())", varArgName, varArgName + "Builder")
            .build();
    final TypeName builderParameterType =
        ParameterizedTypeName.get(
            ClassNames.COMPONENT_BUILDER,
            getBuilderGenericTypes(internalType, ClassNames.COMPONENT_BUILDER));
    return getMethodSpecBuilder(
            specModel,
            Collections.singletonList(
                parameter(prop, builderParameterType, varArgName + "Builder")),
            codeBlock,
            varArgName)
        .build();
  }

  private static MethodSpec resBuilder(
      SpecModel specModel,
      final PropModel prop,
      final ClassName annotationClassName,
      final String resolver) {
    return builder(
        specModel,
        prop,
        prop.getName() + "Res",
        Collections.singletonList(
            parameter(prop, TypeName.INT, "resId", annotation(annotationClassName))),
        "$L(resId)",
        resolver + "Res");
  }

  private static MethodSpec resWithVarargsBuilder(
      SpecModel specModel,
      final PropModel prop,
      final ClassName annotationClassName,
      final String resolver,
      final TypeName varargsType,
      final String varargsName) {
    return getMethodSpecBuilder(
            specModel,
            prop,
            prop.getName() + "Res",
            Arrays.asList(
                parameter(prop, TypeName.INT, "resId", annotation(annotationClassName)),
                ParameterSpec.builder(ArrayTypeName.of(varargsType), varargsName).build()),
            "$L(resId, " + varargsName + ")",
            resolver + "Res")
        .varargs(true)
        .build();
  }

  private static TypeSpecDataHolder attrBuilders(
      SpecModel specModel,
      final PropModel prop,
      final ClassName annotationClassName,
      final String resolver) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    dataHolder.addMethod(
        builder(
            specModel,
            prop,
            prop.getName() + "Attr",
            Arrays.asList(
                parameter(prop, TypeName.INT, "attrResId", annotation(ClassNames.ATTR_RES)),
                parameter(prop, TypeName.INT, "defResId", annotation(annotationClassName))),
            "$L(attrResId, defResId)",
            resolver + "Attr"));

    dataHolder.addMethod(
        builder(
            specModel,
            prop,
            prop.getName() + "Attr",
            Collections.singletonList(
                parameter(prop, TypeName.INT, "attrResId", annotation(ClassNames.ATTR_RES))),
            "$L(attrResId, 0)",
            resolver + "Attr"));

    return dataHolder.build();
  }

  private static MethodSpec pxBuilder(SpecModel specModel, final PropModel prop) {
    return builder(
        specModel,
        prop,
        prop.getName() + "Px",
        Collections.singletonList(
            parameter(prop, prop.getTypeName(), prop.getName(), annotation(ClassNames.PX))),
        prop.getName());
  }

  private static MethodSpec dipBuilder(SpecModel specModel, final PropModel prop) {
    final AnnotationSpec dipAnnotation =
        AnnotationSpec.builder(ClassNames.DIMENSION)
            .addMember("unit", "$T.DP", ClassNames.DIMENSION)
            .build();

    return builder(
        specModel,
        prop,
        prop.getName() + "Dip",
        Collections.singletonList(parameter(prop, TypeName.FLOAT, "dips", dipAnnotation)),
        "dipsToPixels(dips)");
  }

  private static MethodSpec sipBuilder(SpecModel specModel, final PropModel prop) {
    final AnnotationSpec spAnnotation =
        AnnotationSpec.builder(ClassNames.DIMENSION)
            .addMember("unit", "$T.SP", ClassNames.DIMENSION)
            .build();

    return builder(
        specModel,
        prop,
        prop.getName() + "Sp",
        Collections.singletonList(parameter(prop, TypeName.FLOAT, "sips", spAnnotation)),
        "sipsToPixels(sips)");
  }

  private static MethodSpec builderBuilder(
      SpecModel specModel, final PropModel prop, final ClassName builderClass) {
    return builder(
        specModel,
        prop,
        prop.getName(),
        Collections.singletonList(
            parameter(
                prop,
                ParameterizedTypeName.get(builderClass, getBuilderGenericTypes(prop, builderClass)),
                prop.getName() + "Builder")),
        "$L.build()",
        prop.getName() + "Builder");
  }

  private static TypeName[] getBuilderGenericTypes(
      final PropModel prop, final ClassName builderClass) {
    return getBuilderGenericTypes(prop.getTypeName(), builderClass);
  }

  private static TypeName[] getBuilderGenericTypes(
      final TypeName type, final ClassName builderClass) {
    final TypeName typeParameter =
        type instanceof ParameterizedTypeName
                && !((ParameterizedTypeName) type).typeArguments.isEmpty()
            ? ((ParameterizedTypeName) type).typeArguments.get(0)
            : WildcardTypeName.subtypeOf(ClassNames.COMPONENT_LIFECYCLE);

    if (builderClass.equals(ClassNames.COMPONENT_BUILDER)) {
      return new TypeName[] {WildcardTypeName.subtypeOf(TypeName.OBJECT)};
    } else {
      return new TypeName[] {typeParameter};
    }
  }

  private static ParameterSpec parameter(
      final PropModel prop,
      final TypeName type,
      final String name,
      final AnnotationSpec... extraAnnotations) {
    final ParameterSpec.Builder builder =
        ParameterSpec.builder(type, name).addAnnotations(prop.getExternalAnnotations());

    for (final AnnotationSpec annotation : extraAnnotations) {
      builder.addAnnotation(annotation);
    }

    return builder.build();
  }

  private static AnnotationSpec annotation(final ClassName className) {
    return AnnotationSpec.builder(className).build();
  }

  private static MethodSpec builder(
      SpecModel specModel,
      final PropModel prop,
      final String name,
      final List<ParameterSpec> parameters,
      final String statement,
      final Object... formatObjects) {
    return getMethodSpecBuilder(specModel, prop, name, parameters, statement, formatObjects)
        .build();
  }

  private static MethodSpec.Builder getMethodSpecBuilder(
      SpecModel specModel,
      final PropModel prop,
      final String name,
      final List<ParameterSpec> parameters,
      final String statement,
      final Object... formatObjects) {

    if (prop.hasVarArgs()) {
      final String propName = prop.getName();

      final CodeBlock.Builder codeBlockBuilder =
          CodeBlock.builder()
              .beginControlFlow("if ($L == null)", propName)
              .addStatement("return this")
              .endControlFlow();

      return getMethodSpecBuilder(specModel, parameters, codeBlockBuilder.build(), name);
    }

    final String propMatcherName = getPropMatcherName(prop);
    final CodeBlock formattedStatement = CodeBlock.of(statement, formatObjects);
    final CodeBlock codeBlock =
        CodeBlock.builder()
            .addStatement(
                "this.$N = $L.is(($T) $L)",
                propMatcherName,
                ClassNames.HAMCREST_CORE_IS,
                getRawType(prop.getTypeName()),
                formattedStatement)
            .build();

    return getMethodSpecBuilder(specModel, parameters, codeBlock, name);
  }

  private static MethodSpec.Builder getMethodSpecBuilder(
      SpecModel specModel,
      final List<ParameterSpec> parameters,
      final CodeBlock codeBlock,
      final String name) {
    final MethodSpec.Builder builder =
        MethodSpec.methodBuilder(name)
            .addModifiers(Modifier.PUBLIC)
            .returns(getMatcherType(specModel))
            .addCode(codeBlock);

    for (final ParameterSpec param : parameters) {
      builder.addParameter(param);
    }

    builder.addStatement("return this");

    return builder;
  }

  private static <T extends SpecModel & HasEnclosedSpecModel> CodeBlock generateMatchMethodBody(
      final T specModel) {
    final CodeBlock.Builder builder = CodeBlock.builder();

    final SpecModel enclosedSpecModel = specModel.getEnclosedSpecModel();
    builder
        .beginControlFlow(
            "if (!value.getComponentClass().isAssignableFrom($L.class))",
            enclosedSpecModel.getComponentTypeName())
        .addStatement(
            "as(new $T(\"Sub-component of type \\\"$L\\\"\"))",
            ClassNames.ASSERTJ_TEXT_DESCRIPTION,
            enclosedSpecModel.getComponentTypeName())
        .addStatement("return false")
        .endControlFlow();

    builder.addStatement(
        "final $1L impl = ($1L) value.getComponent()", getEnclosedImplClassName(enclosedSpecModel));

    for (PropModel prop : specModel.getProps()) {
      if (getRawType(prop.getTypeName()).equals(ClassNames.COMPONENT)) {
        builder.add(generateComponentMatchBlock(prop));
      }

      // We generate matchers for both components as well as nested matchers, so the fall-through
      // here is intended.
      builder.add(generateValuePropMatchBlock(enclosedSpecModel, prop));
    }

    builder.addStatement("return true");

    return builder.build();
  }

  private static String getPropValueName(PropModel prop) {
    final String name = prop.getName();
    return "propValue" + name.substring(0, 1).toUpperCase() + name.substring(1);
  }

  private static CodeBlock generateFieldExtractorBlock(PropModel prop, String varName) {
    return CodeBlock.builder()
        .addStatement("final $T $L", prop.getTypeName(), varName)
        .beginControlFlow("try")
        .addStatement(
            "$L = ($T) impl.getClass().getDeclaredField($S).get(impl)",
            varName,
            prop.getTypeName(),
            prop.getName())
        .nextControlFlow("catch (Exception e)")
        .addStatement("throw new RuntimeException(e)")
        .endControlFlow()
        .build();
  }

  private static CodeBlock generateComponentMatchBlock(PropModel prop) {
    final String matcherName = getPropComponentMatcherName(prop);
    final String propValueName = getPropValueName(prop) + "Component";
    return CodeBlock.builder()
        .add(generateFieldExtractorBlock(prop, propValueName))
        .beginControlFlow(
            "if ($1N != null && !$1N.matches(value.getNestedInstance($2L)))",
            matcherName,
            propValueName)
        .addStatement("as($N.description())", matcherName)
        .addStatement("return false")
        .endControlFlow()
        .build();
  }

  private static CodeBlock generateValuePropMatchBlock(
      SpecModel enclosedSpecModel, PropModel prop) {
    final String matcherName = getPropMatcherName(prop);
    return CodeBlock.builder()
        .add(generateFieldExtractorBlock(prop, getPropValueName(prop)))
        .beginControlFlow(
            "if ($1N != null && !$1N.matches($2L))", matcherName, getPropValueName(prop))
        .add(generateMatchFailureStatement(enclosedSpecModel, matcherName, prop))
        .addStatement("return false")
        .endControlFlow()
        .build();
  }

  private static CodeBlock generateMatchFailureStatement(
      final SpecModel enclosedSpecModel, String matcherName, PropModel prop) {
    return CodeBlock.builder()
        .add("as(new $T(", ClassNames.ASSERTJ_TEXT_DESCRIPTION)
        .add(
            "\"Sub-component of type <$T> with prop <$L> %s (doesn't match %s)\", $N, $L",
            enclosedSpecModel.getComponentTypeName(),
            prop.getName(),
            matcherName,
            getPropValueName(prop))
        .addStatement("))")
        .build();
  }

  private static ClassName getEnclosedImplClassName(final SpecModel enclosedSpecModel) {
    final String componentTypeName = enclosedSpecModel.getComponentTypeName().toString();
    return ClassName.bestGuess(componentTypeName);
  }

  private static <T extends SpecModel & HasEnclosedSpecModel> MethodSpec generateBuildMethod(
      final T specModel) {
    final MethodSpec.Builder buildMethodBuilder =
        MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC)
            .returns(getMatcherConditionTypeName());

    final CodeBlock innerMatcherLogicBlock = generateMatchMethodBody(specModel);

    final TypeSpec matcherInnerClass =
        TypeSpec.anonymousClassBuilder("")
            .superclass(getMatcherConditionTypeName())
            .addMethod(
                MethodSpec.methodBuilder("matches")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ClassNames.INSPECTABLE_COMPONENT, "value")
                    .returns(TypeName.BOOLEAN)
                    .addAnnotation(Override.class)
                    .addCode(innerMatcherLogicBlock)
                    .build())
            .build();

    return buildMethodBuilder
        .addStatement("final $T mainBuilder = $L", getMatcherConditionTypeName(), matcherInnerClass)
        .addStatement(
            "return $T.allOf(mainBuilder, $T.buildCommonMatcher(this))",
            ClassNames.ASSERTJ_JAVA6ASSERTIONS,
            ClassNames.BASE_MATCHER_BUILDER)
        .build();
  }

}
