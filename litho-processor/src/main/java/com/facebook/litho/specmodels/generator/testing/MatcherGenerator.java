/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator.testing;

import static com.facebook.litho.specmodels.generator.ComponentImplGenerator.getImplClassName;

import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.PropDefaultModel;
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
import java.util.ArrayList;
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

  public static TypeSpecDataHolder generate(final SpecModel specModel) {
    return TypeSpecDataHolder.newBuilder()
        .addTypeSpecDataHolder(generateFactoryMethods(specModel))
        .addTypeSpecDataHolder(generateBuilder(specModel))
        .build();
  }

  private static TypeSpecDataHolder generateFactoryMethods(final SpecModel specModel) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    final MethodSpec.Builder factoryMethod =
        MethodSpec.methodBuilder("matcher")
            .addModifiers(Modifier.PUBLIC)
            .addModifiers(Modifier.STATIC)
            .returns(BUILDER_CLASS_NAME)
            .addParameter(specModel.getContextClass(), "c")
            .addStatement("return new $T(c)", BUILDER_CLASS_NAME);

    return dataHolder.addMethod(factoryMethod.build()).build();
  }

  private static TypeSpecDataHolder generateBuilder(final SpecModel specModel) {
    final TypeSpec.Builder propsBuilderClassBuilder =
        TypeSpec.classBuilder(BUILDER)
            .addModifiers(Modifier.STATIC)
            .superclass(ClassNames.RESOURCE_RESOLVER);

    final MethodSpec constructor =
        MethodSpec.constructorBuilder()
            .addParameter(specModel.getContextClass(), "c")
            .addStatement("super.init(c, c.getResourceCache())")
            .build();

    propsBuilderClassBuilder.addMethod(constructor);

    int requiredPropIndex = 0;
    for (final PropModel prop : specModel.getProps()) {
      generatePropsBuilderMethods(specModel, prop, requiredPropIndex)
          .addToTypeSpec(propsBuilderClassBuilder);

      if (!prop.isOptional()) {
        requiredPropIndex++;
      }
    }

    propsBuilderClassBuilder.addMethod(generateBuildMethod(specModel));

    return TypeSpecDataHolder.newBuilder().addType(propsBuilderClassBuilder.build()).build();
  }

  private static TypeSpecDataHolder generatePropsBuilderMethods(
      final SpecModel specModel, final PropModel prop, final int requiredIndex) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();
    dataHolder.addField(matcherFieldBuilder(prop));
    dataHolder.addMethod(matcherFieldSetterBuilder(specModel, prop, requiredIndex));

    if (prop.hasVarArgs()) {
      dataHolder.addMethod(varArgBuilder(specModel, prop, requiredIndex));
      final ParameterizedTypeName type = (ParameterizedTypeName) prop.getType();
      if (getRawType(type.typeArguments.get(0)).equals(ClassNames.COMPONENT)) {
        dataHolder.addMethod(varArgBuilderBuilder(prop, requiredIndex));
      }
      // fall through to generate builder method for List<T>
    }

    switch (prop.getResType()) {
      case STRING:
        dataHolder.addMethod(regularBuilder(specModel, prop, requiredIndex));
        dataHolder.addMethod(
            resBuilder(specModel, prop, requiredIndex, ClassNames.STRING_RES, "resolveString"));
        dataHolder.addMethod(
            resWithVarargsBuilder(
                specModel,
                prop,
                requiredIndex,
                ClassNames.STRING_RES,
                "resolveString",
                TypeName.OBJECT,
                "formatArgs"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, requiredIndex, ClassNames.STRING_RES, "resolveString"));
        break;
      case STRING_ARRAY:
        dataHolder.addMethod(regularBuilder(specModel, prop, requiredIndex));
        dataHolder.addMethod(
            resBuilder(specModel, prop, requiredIndex, ClassNames.ARRAY_RES, "resolveStringArray"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(
                specModel, prop, requiredIndex, ClassNames.ARRAY_RES, "resolveStringArray"));
        break;
      case INT:
        dataHolder.addMethod(regularBuilder(specModel, prop, requiredIndex));
        dataHolder.addMethod(
            resBuilder(specModel, prop, requiredIndex, ClassNames.INT_RES, "resolveInt"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, requiredIndex, ClassNames.INT_RES, "resolveInt"));
        break;
      case INT_ARRAY:
        dataHolder.addMethod(regularBuilder(specModel, prop, requiredIndex));
        dataHolder.addMethod(
            resBuilder(specModel, prop, requiredIndex, ClassNames.ARRAY_RES, "resolveIntArray"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, requiredIndex, ClassNames.ARRAY_RES, "resolveIntArray"));
        break;
      case BOOL:
        dataHolder.addMethod(regularBuilder(specModel, prop, requiredIndex));
        dataHolder.addMethod(
            resBuilder(specModel, prop, requiredIndex, ClassNames.BOOL_RES, "resolveBool"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, requiredIndex, ClassNames.BOOL_RES, "resolveBool"));
        break;
      case COLOR:
        dataHolder.addMethod(
            regularBuilder(specModel, prop, requiredIndex, annotation(ClassNames.COLOR_INT)));
        dataHolder.addMethod(
            resBuilder(specModel, prop, requiredIndex, ClassNames.COLOR_RES, "resolveColor"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, requiredIndex, ClassNames.COLOR_RES, "resolveColor"));
        break;
      case DIMEN_SIZE:
        dataHolder.addMethod(pxBuilder(specModel, prop, requiredIndex));
        dataHolder.addMethod(
            resBuilder(specModel, prop, requiredIndex, ClassNames.DIMEN_RES, "resolveDimenSize"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, requiredIndex, ClassNames.DIMEN_RES, "resolveDimenSize"));
        dataHolder.addMethod(dipBuilder(specModel, prop, requiredIndex));
        break;
      case DIMEN_TEXT:
        dataHolder.addMethod(pxBuilder(specModel, prop, requiredIndex));
        dataHolder.addMethod(
            resBuilder(specModel, prop, requiredIndex, ClassNames.DIMEN_RES, "resolveDimenSize"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, requiredIndex, ClassNames.DIMEN_RES, "resolveDimenSize"));
        dataHolder.addMethod(dipBuilder(specModel, prop, requiredIndex));
        dataHolder.addMethod(sipBuilder(specModel, prop, requiredIndex));
        break;
      case DIMEN_OFFSET:
        dataHolder.addMethod(pxBuilder(specModel, prop, requiredIndex));
        dataHolder.addMethod(
            resBuilder(specModel, prop, requiredIndex, ClassNames.DIMEN_RES, "resolveDimenOffset"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(
                specModel, prop, requiredIndex, ClassNames.DIMEN_RES, "resolveDimenOffset"));
        dataHolder.addMethod(dipBuilder(specModel, prop, requiredIndex));
        break;
      case FLOAT:
        dataHolder.addMethod(regularBuilder(specModel, prop, requiredIndex));
        dataHolder.addMethod(
            resBuilder(specModel, prop, requiredIndex, ClassNames.DIMEN_RES, "resolveFloat"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(specModel, prop, requiredIndex, ClassNames.DIMEN_RES, "resolveFloat"));
        break;
      case DRAWABLE:
        dataHolder.addMethod(regularBuilder(specModel, prop, requiredIndex));
        dataHolder.addMethod(
            resBuilder(specModel, prop, requiredIndex, ClassNames.DRAWABLE_RES, "resolveDrawable"));
        dataHolder.addTypeSpecDataHolder(
            attrBuilders(
                specModel, prop, requiredIndex, ClassNames.DRAWABLE_RES, "resolveDrawable"));
        break;
      case NONE:
        if (prop.getType().equals(specModel.getComponentClass())) {
          dataHolder.addMethod(componentBuilder(specModel, prop, requiredIndex));
        } else {
          dataHolder.addMethod(regularBuilder(specModel, prop, requiredIndex));
        }
        break;
      case DRAWABLE_REFERENCE:
        // TODO(T15854501): Implement drawable reference comparison for this and other generators.
        break;
    }

    if (getRawType(prop.getType()).equals(ClassNames.COMPONENT)) {
      dataHolder.addMethod(
          builderBuilder(specModel, prop, requiredIndex, ClassNames.COMPONENT_BUILDER));
    }

    if (getRawType(prop.getType()).equals(ClassNames.REFERENCE)) {
      dataHolder.addMethod(
          builderBuilder(specModel, prop, requiredIndex, ClassNames.REFERENCE_BUILDER));
    }

    return dataHolder.build();
  }

  static String generatePropsDefaultInitializers(
      final SpecModel specModel, final PropDefaultModel propDefault) {

    switch (propDefault.getResType()) {
      case STRING:
        return generatePropDefaultResInitializer("resolveStringRes", propDefault, specModel);
      case STRING_ARRAY:
        return generatePropDefaultResInitializer("resolveStringArrayRes", propDefault, specModel);
      case INT:
        return generatePropDefaultResInitializer("resolveIntRes", propDefault, specModel);
      case INT_ARRAY:
        return generatePropDefaultResInitializer("resolveIntArrayRes", propDefault, specModel);
      case BOOL:
        return generatePropDefaultResInitializer("resolveBoolRes", propDefault, specModel);
      case COLOR:
        return generatePropDefaultResInitializer("resolveColorRes", propDefault, specModel);
      case DIMEN_SIZE:
        return generatePropDefaultResInitializer("resolveDimenSizeRes", propDefault, specModel);
      case DIMEN_TEXT:
        return generatePropDefaultResInitializer("resolveDimenSizeRes", propDefault, specModel);
      case DIMEN_OFFSET:
        return generatePropDefaultResInitializer("resolveDimenOffsetRes", propDefault, specModel);
      case FLOAT:
        return generatePropDefaultResInitializer("resolveFloatRes", propDefault, specModel);
      case DRAWABLE:
        return generatePropDefaultResInitializer("resolveDrawableRes", propDefault, specModel);
      case DRAWABLE_REFERENCE:
        break;
      case NONE:
        break;
    }

    return "";
  }

  private static TypeName getRawType(final TypeName type) {
    return type instanceof ParameterizedTypeName ? ((ParameterizedTypeName) type).rawType : type;
  }

  private static MethodSpec matcherFieldSetterBuilder(
      SpecModel specModel, PropModel prop, int requiredIndex) {
    final String propMatcherName = getPropMatcherName(prop);
    final String propName = prop.getName();

    return getMethodSpecBuilder(
            prop,
            requiredIndex,
            propName,
            ImmutableList.of(ParameterSpec.builder(getPropMatcherType(prop), "matcher").build()),
            CodeBlock.builder().addStatement("$L = matcher", propMatcherName).build())
        .build();
  }

  private static MethodSpec componentBuilder(
      final SpecModel specModel, final PropModel prop, final int requiredIndex) {
    return builder(
        specModel,
        prop,
        requiredIndex,
        prop.getName(),
        Arrays.asList(parameter(prop, prop.getType(), prop.getName())),
        "$L == null ? null : $L.makeShallowCopy()",
        prop.getName(),
        prop.getName());
  }

  private static MethodSpec regularBuilder(
      final SpecModel specModel,
      final PropModel prop,
      final int requiredIndex,
      final AnnotationSpec... extraAnnotations) {
    return builder(
        specModel,
        prop,
        requiredIndex,
        prop.getName(),
        Collections.singletonList(
            parameter(prop, prop.getType(), prop.getName(), extraAnnotations)),
        prop.getName());
  }

  private static FieldSpec matcherFieldBuilder(final PropModel prop) {
    return FieldSpec.builder(getPropMatcherType(prop), getPropMatcherName(prop))
        .addAnnotation(Nullable.class)
        .build();
  }

  private static ParameterizedTypeName getPropMatcherType(PropModel prop) {
    return ParameterizedTypeName.get(ClassNames.HAMCREST_MATCHER, prop.getType().box());
  }

  static String getPropMatcherName(final PropModel prop) {
    final String name = prop.getName();

    final int fst = Character.toUpperCase(name.codePointAt(0));

    return "m"
        + String.copyValueOf(Character.toChars(fst))
        + name.substring(name.offsetByCodePoints(0, 1))
        + "Matcher";
  }

  private static MethodSpec varArgBuilder(
      final SpecModel specModel,
      final PropModel prop,
      final int requiredIndex,
      final AnnotationSpec... extraAnnotations) {
    final ParameterizedTypeName parameterizedTypeName = (ParameterizedTypeName) prop.getType();
    final TypeName singleParameterType = parameterizedTypeName.typeArguments.get(0);
    final String varArgName = prop.getVarArgsSingleName();

    final String propName = prop.getName();
    final ParameterizedTypeName varArgType = (ParameterizedTypeName) prop.getType();
    final ParameterizedTypeName listType =
        ParameterizedTypeName.get(ClassName.get(ArrayList.class), varArgType.typeArguments.get(0));
    final CodeBlock codeBlock =
        CodeBlock.builder()
            .beginControlFlow("if ($L == null)", varArgName)
            .addStatement("return this")
            .endControlFlow()
            .build();

    return getMethodSpecBuilder(
            prop,
            requiredIndex,
            varArgName,
            Arrays.asList(parameter(prop, singleParameterType, varArgName, extraAnnotations)),
            codeBlock)
        .build();
  }

  private static MethodSpec varArgBuilderBuilder(final PropModel prop, final int requiredIndex) {
    final String varArgName = prop.getVarArgsSingleName();
    final ParameterizedTypeName varArgType = (ParameterizedTypeName) prop.getType();
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
            prop,
            requiredIndex,
            varArgName,
            Arrays.asList(parameter(prop, builderParameterType, varArgName + "Builder")),
            codeBlock)
        .build();
  }

  private static MethodSpec resBuilder(
      final SpecModel specModel,
      final PropModel prop,
      final int requiredIndex,
      final ClassName annotationClassName,
      final String resolver) {
    return builder(
        specModel,
        prop,
        requiredIndex,
        prop.getName() + "Res",
        Arrays.asList(parameter(prop, TypeName.INT, "resId", annotation(annotationClassName))),
        "$L(resId)",
        resolver + "Res");
  }

  private static MethodSpec resWithVarargsBuilder(
      final SpecModel specModel,
      final PropModel prop,
      final int requiredIndex,
      final ClassName annotationClassName,
      final String resolver,
      final TypeName varargsType,
      final String varargsName) {
    return getMethodSpecBuilder(
            specModel,
            prop,
            requiredIndex,
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
      final SpecModel specModel,
      final PropModel prop,
      final int requiredIndex,
      final ClassName annotationClassName,
      final String resolver) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    dataHolder.addMethod(
        builder(
            specModel,
            prop,
            requiredIndex,
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
            requiredIndex,
            prop.getName() + "Attr",
            Arrays.asList(
                parameter(prop, TypeName.INT, "attrResId", annotation(ClassNames.ATTR_RES))),
            "$L(attrResId, 0)",
            resolver + "Attr"));

    return dataHolder.build();
  }

  private static MethodSpec pxBuilder(
      final SpecModel specModel, final PropModel prop, final int requiredIndex) {
    return builder(
        specModel,
        prop,
        requiredIndex,
        prop.getName() + "Px",
        Arrays.asList(parameter(prop, prop.getType(), prop.getName(), annotation(ClassNames.PX))),
        prop.getName());
  }

  private static MethodSpec dipBuilder(
      final SpecModel specModel, final PropModel prop, final int requiredIndex) {
    final AnnotationSpec dipAnnotation =
        AnnotationSpec.builder(ClassNames.DIMENSION)
            .addMember("unit", "$T.DP", ClassNames.DIMENSION)
            .build();

    return builder(
        specModel,
        prop,
        requiredIndex,
        prop.getName() + "Dip",
        Arrays.asList(parameter(prop, TypeName.FLOAT, "dips", dipAnnotation)),
        "dipsToPixels(dips)");
  }

  private static MethodSpec sipBuilder(
      final SpecModel specModel, final PropModel prop, final int requiredIndex) {
    final AnnotationSpec spAnnotation =
        AnnotationSpec.builder(ClassNames.DIMENSION)
            .addMember("unit", "$T.SP", ClassNames.DIMENSION)
            .build();

    return builder(
        specModel,
        prop,
        requiredIndex,
        prop.getName() + "Sp",
        Arrays.asList(parameter(prop, TypeName.FLOAT, "sips", spAnnotation)),
        "sipsToPixels(sips)");
  }

  private static MethodSpec builderBuilder(
      final SpecModel specModel,
      final PropModel prop,
      final int requiredIndex,
      final ClassName builderClass) {
    return builder(
        specModel,
        prop,
        requiredIndex,
        prop.getName(),
        Arrays.asList(
            parameter(
                prop,
                ParameterizedTypeName.get(builderClass, getBuilderGenericTypes(prop, builderClass)),
                prop.getName() + "Builder")),
        "$L.build()",
        prop.getName() + "Builder");
  }

  private static TypeName[] getBuilderGenericTypes(
      final PropModel prop, final ClassName builderClass) {
    return getBuilderGenericTypes(prop.getType(), builderClass);
  }

  private static TypeName[] getBuilderGenericTypes(
      final TypeName type, final ClassName builderClass) {
    final TypeName typeParameter =
        type instanceof ParameterizedTypeName
                && !((ParameterizedTypeName) type).typeArguments.isEmpty()
            ? ((ParameterizedTypeName) type).typeArguments.get(0)
            : WildcardTypeName.subtypeOf(ClassNames.COMPONENT_LIFECYCLE);

    if (builderClass.equals(ClassNames.COMPONENT_BUILDER)) {
      return new TypeName[] {typeParameter, WildcardTypeName.subtypeOf(TypeName.OBJECT)};
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
      final SpecModel specModel,
      final PropModel prop,
      final int requiredIndex,
      final String name,
      final List<ParameterSpec> parameters,
      final String statement,
      final Object... formatObjects) {
    return getMethodSpecBuilder(
            specModel, prop, requiredIndex, name, parameters, statement, formatObjects)
        .build();
  }

  private static MethodSpec.Builder getMethodSpecBuilder(
      final SpecModel specModel,
      final PropModel prop,
      final int requiredIndex,
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

      return getMethodSpecBuilder(prop, requiredIndex, name, parameters, codeBlockBuilder.build());
    }

    final String propMatcherName = getPropMatcherName(prop);
    final CodeBlock formattedStatement = CodeBlock.of(statement, formatObjects);
    final CodeBlock codeBlock =
        CodeBlock.builder()
            .addStatement(
                "this.$N = $L.is($L)",
                propMatcherName,
                ClassNames.HAMCREST_CORE_IS,
                formattedStatement)
            .build();

    return getMethodSpecBuilder(prop, requiredIndex, name, parameters, codeBlock);
  }

  private static MethodSpec.Builder getMethodSpecBuilder(
      final PropModel prop,
      final int requiredIndex,
      final String name,
      final List<ParameterSpec> parameters,
      final CodeBlock codeBlock) {
    final MethodSpec.Builder builder =
        MethodSpec.methodBuilder(name)
            .addModifiers(Modifier.PUBLIC)
            .returns(BUILDER_CLASS_NAME)
            .addCode(codeBlock);

    for (final ParameterSpec param : parameters) {
      builder.addParameter(param);
    }

    builder.addStatement("return this");

    return builder;
  }

  private static CodeBlock generateMatchMethodBody(final SpecModel specModel) {
    final CodeBlock.Builder builder = CodeBlock.builder();

    // TODO(21831949): Create typed field for this.
    final SpecModel enclosedSpecModel = (SpecModel) specModel.getRepresentedObject();
    builder
        .beginControlFlow(
            "if (!value.getComponentClass().isAssignableFrom($L.class))",
            enclosedSpecModel.getComponentTypeName())
        .addStatement("return false")
        .endControlFlow();

    builder.addStatement(
        "final $1L impl = ($1L) value.getComponent()", getEnclosedImplClassName(enclosedSpecModel));

    for (PropModel prop : specModel.getProps()) {
      final String matcherName = getPropMatcherName(prop);

      builder
          .beginControlFlow(
              "if ($1N != null && !$1N.matches(impl.$2L))", matcherName, prop.getName())
          .addStatement("return false")
          .endControlFlow();
    }

    builder.addStatement("return true");

    return builder.build();
  }

  public static ClassName getEnclosedImplClassName(final SpecModel enclosedSpecModel) {
    final String componentTypeName = enclosedSpecModel.getComponentTypeName().toString();
    return ClassName.bestGuess(componentTypeName + '.' + getImplClassName(enclosedSpecModel));
  }

  private static MethodSpec generateBuildMethod(final SpecModel specModel) {
    final MethodSpec.Builder buildMethodBuilder =
        MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC)
            .returns(ClassNames.COMPONENT_MATCHER);

    final CodeBlock placeHolderCodeBlock = generateMatchMethodBody(specModel);

    final TypeSpec matcherInnerClass =
        TypeSpec.anonymousClassBuilder("")
            .superclass(ClassNames.COMPONENT_MATCHER)
            .addMethod(
                MethodSpec.methodBuilder("matches")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ClassNames.INSPECTABLE_COMPONENT, "value")
                    .returns(TypeName.BOOLEAN)
                    .addAnnotation(Override.class)
                    .addCode(placeHolderCodeBlock)
                    .build())
            .build();

    return buildMethodBuilder.addStatement("return $L", matcherInnerClass).build();
  }

  private static String generatePropDefaultResInitializer(
      final String resourceResolveMethodName,
      final PropDefaultModel propDefaultModel,
      final SpecModel specModel) {
    final StringBuilder builtInitializer = new StringBuilder();

    if (propDefaultModel.isResResolvable()) {
      return String.format(
          builtInitializer
              .append(resourceResolveMethodName)
              .append("(")
              .append(propDefaultModel.getResId())
              .append(")")
              .toString(),
          specModel.getSpecName(),
          propDefaultModel.getName());
    }

    return builtInitializer.toString();
  }
}
