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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.SpecModel;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

/**
 * Class that generates the builder for a Component.
 */
public class BuilderGenerator {

  private static final String BUILDER = "Builder";
  private static final String BUILDER_POOL_FIELD = "mBuilderPool";
  private static final ClassName BUILDER_CLASS_NAME = ClassName.bestGuess(BUILDER);
  private static final String CONTEXT_MEMBER_NAME = "mContext";
  private static final String CONTEXT_PARAM_NAME = "context";
  private static final String REQUIRED_PROPS_NAMES = "REQUIRED_PROPS_NAMES";
  private static final String REQUIRED_PROPS_COUNT = "REQUIRED_PROPS_COUNT";

  private BuilderGenerator() {
  }

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    return TypeSpecDataHolder.newBuilder()
        .addTypeSpecDataHolder(generateFactoryMethod(specModel))
        .addMethod(generateCreateBuilderMethodWithStyle(specModel))
        .addMethod(generateCreateBuilderMethod(specModel))
        .addTypeSpecDataHolder(generateBuilder(specModel))
        .build();
  }

  static MethodSpec generateCreateBuilderMethod(SpecModel specModel) {
    return MethodSpec.methodBuilder("create")
        .addModifiers(Modifier.PUBLIC)
        .returns(BUILDER_CLASS_NAME)
        .addParameter(specModel.getContextClass(), "context")
        .addStatement("return create(context, 0, 0)")
        .addModifiers(!specModel.hasInjectedDependencies() ? Modifier.STATIC : Modifier.FINAL)
        .build();
  }

  static MethodSpec generateCreateBuilderMethodWithStyle(SpecModel specModel) {
    return MethodSpec.methodBuilder("create")
        .addModifiers(Modifier.PUBLIC)
        .returns(BUILDER_CLASS_NAME)
        .addParameter(specModel.getContextClass(), "context")
        .addParameter(int.class, "defStyleAttr")
        .addParameter(int.class, "defStyleRes")
        .addStatement(
            "return new$L(context, defStyleAttr, defStyleRes, new $L())",
            BUILDER_CLASS_NAME,
            ComponentImplGenerator.getImplClassName(specModel))
        .addModifiers(!specModel.hasInjectedDependencies() ? Modifier.STATIC : Modifier.FINAL)
        .build();
  }

  static TypeSpecDataHolder generateFactoryMethod(SpecModel specModel) {
    final String implClassName = ComponentImplGenerator.getImplClassName(specModel);
    final String implParamName = ComponentImplGenerator.getImplInstanceName(specModel);
    final ClassName stateClass = ClassName.bestGuess(implClassName);

    final ParameterizedTypeName synchronizedPoolClass =
        ParameterizedTypeName.get(ClassNames.SYNCHRONIZED_POOL, BUILDER_CLASS_NAME);

    final FieldSpec.Builder poolField = FieldSpec.builder(synchronizedPoolClass, BUILDER_POOL_FIELD)
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .initializer("new $T(2)", synchronizedPoolClass);

    final MethodSpec.Builder factoryMethod = MethodSpec.methodBuilder(getFactoryMethodName())
        .addModifiers(Modifier.PRIVATE)
        .returns(BUILDER_CLASS_NAME)
        .addParameter(specModel.getContextClass(), "context")
        .addStatement("$T builder = $L.acquire()", BUILDER_CLASS_NAME, BUILDER_POOL_FIELD)
        .beginControlFlow("if (builder == null)")
        .addStatement("builder = new $T()", BUILDER_CLASS_NAME)
        .endControlFlow();

    if (specModel.isStylingSupported()) {
      factoryMethod
          .addParameter(int.class, "defStyleAttr")
          .addParameter(int.class, "defStyleRes")
          .addParameter(stateClass, implParamName)
          .addStatement(
              "builder.init(context, defStyleAttr, defStyleRes, $L)", implParamName);
    } else {
      factoryMethod
          .addParameter(stateClass, implParamName)
          .addStatement("builder.init(context, $L)", implParamName);
    }

    factoryMethod.addStatement("return builder");

    if (!specModel.hasInjectedDependencies() || specModel.getTypeVariables().isEmpty()) {
      factoryMethod.addModifiers(Modifier.STATIC);
      poolField.addModifiers(Modifier.STATIC);
    }

    return TypeSpecDataHolder.newBuilder()
        .addMethod(factoryMethod.build())
        .addField(poolField.build())
        .build();
  }

  static TypeSpecDataHolder generateBuilder(SpecModel specModel) {
    final String implClassName = ComponentImplGenerator.getImplClassName(specModel);
    final String implParamName = ComponentImplGenerator.getImplInstanceName(specModel);
    final String implMemberInstanceName = getImplMemberInstanceName(specModel);
    final ClassName implClass = ClassName.bestGuess(implClassName);
    final MethodSpec.Builder initMethodSpec = MethodSpec.methodBuilder("init")
        .addModifiers(Modifier.PRIVATE)
        .addParameter(specModel.getContextClass(), CONTEXT_PARAM_NAME);

    if (specModel.isStylingSupported()) {
      initMethodSpec
          .addParameter(int.class, "defStyleAttr")
          .addParameter(int.class, "defStyleRes")
          .addParameter(implClass, implParamName)
          .addStatement("super.init(context, defStyleAttr, defStyleRes, $L)", implParamName);
    } else {
      initMethodSpec
          .addParameter(implClass, implParamName)
          .addStatement("super.init(context, $L)", implParamName);
    }

    initMethodSpec
        .addStatement("$L = $L", implMemberInstanceName, implParamName)
        .addStatement("$L = $L", CONTEXT_MEMBER_NAME, CONTEXT_PARAM_NAME);

    final TypeSpec.Builder propsBuilderClassBuilder = TypeSpec.classBuilder(BUILDER)
        .addModifiers(Modifier.PUBLIC)
        .superclass(
            ParameterizedTypeName.get(
                ClassName.get(
                    specModel.getComponentClass().packageName(),
                    specModel.getComponentClass().simpleName(),
                    BUILDER),
                specModel.getComponentTypeName()))
        .addField(implClass, implMemberInstanceName)
        .addField(specModel.getContextClass(), CONTEXT_MEMBER_NAME);

    final List<String> requiredPropNames = new ArrayList<>();
    int numRequiredProps = 0;
    for (PropModel prop : specModel.getProps()) {
      if (!prop.isOptional()) {
        numRequiredProps++;
        requiredPropNames.add(prop.getName());
      }
    }

    if (numRequiredProps > 0) {
      final FieldSpec.Builder requiredPropsNamesBuilder =
          FieldSpec.builder(
              String[].class,
              REQUIRED_PROPS_NAMES,
              Modifier.PRIVATE,
              Modifier.FINAL)
              .initializer("new String[] {$L}", commaSeparateAndQuoteStrings(requiredPropNames));

      if (!specModel.hasInjectedDependencies()) {
        requiredPropsNamesBuilder.addModifiers(Modifier.STATIC);
      }

      propsBuilderClassBuilder.addField(requiredPropsNamesBuilder.build());

      final FieldSpec.Builder requiredPropsCountBuilder =
          FieldSpec.builder(
              int.class,
              REQUIRED_PROPS_COUNT,
              Modifier.PRIVATE,
              Modifier.FINAL)
              .initializer("$L", numRequiredProps);

      if (!specModel.hasInjectedDependencies()) {
        requiredPropsCountBuilder.addModifiers(Modifier.STATIC);
      }

      propsBuilderClassBuilder.addField(requiredPropsCountBuilder.build());

      propsBuilderClassBuilder
          .addField(FieldSpec.builder(
              BitSet.class,
              "mRequired",
              Modifier.PRIVATE)
              .initializer("new $T($L)", BitSet.class, REQUIRED_PROPS_COUNT)
              .build());

      initMethodSpec.addStatement("mRequired.clear()");
    }

    propsBuilderClassBuilder.addMethod(initMethodSpec.build());

    // If there are no type variables, then this class can always be static.
    // If the component implementation class is static, and there are type variables, then this
    // class can be static but must shadow the type variables from the class.
    // If the component implementation class is not static, and there are type variables, then this
    // class is not static and we get the type variables from the class.
    final boolean isBuilderStatic = specModel.getTypeVariables().isEmpty() ||
        !specModel.hasInjectedDependencies();
    if (isBuilderStatic) {
      propsBuilderClassBuilder.addModifiers(Modifier.STATIC);

      if (!specModel.getTypeVariables().isEmpty()) {
        propsBuilderClassBuilder.addTypeVariables(specModel.getTypeVariables());
      }
    }

    int requiredPropIndex = 0;
    for (PropModel prop : specModel.getProps()) {
      generatePropsBuilderMethods(specModel, prop, requiredPropIndex)
          .addToTypeSpec(propsBuilderClassBuilder);

      if (!prop.isOptional()) {
        requiredPropIndex++;
      }
    }

    for (EventDeclarationModel eventDeclaration : specModel.getEventDeclarations()) {
      propsBuilderClassBuilder.addMethod(
          generateEventDeclarationBuilderMethod(specModel, eventDeclaration));
    }

    propsBuilderClassBuilder
        .addMethod(generateKeySetterMethod())
        .addMethod(generateBuildMethod(specModel, numRequiredProps))
        .addMethod(generateReleaseMethod(specModel));

    return TypeSpecDataHolder.newBuilder().addType(propsBuilderClassBuilder.build()).build();
  }

  static String getFactoryMethodName() {
    return "new" + BUILDER;
  }

  private static String getImplMemberInstanceName(SpecModel specModel) {
    return "m" + ComponentImplGenerator.getImplClassName(specModel);
  }

  private static String commaSeparateAndQuoteStrings(List<String> strings) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < strings.size(); i++) {
      sb.append('"');
      sb.append(strings.get(i));
      sb.append('"');
      if (i < strings.size() - 1) {
        sb.append(", ");
      }
    }
    return sb.toString();
  }

  static TypeSpecDataHolder generatePropsBuilderMethods(
      SpecModel specModel,
      PropModel prop,
      int requiredIndex) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    switch (prop.getResType()) {
      case STRING:
        dataHolder.addMethod(regularBuilder(specModel, prop, requiredIndex));
        dataHolder.addMethod(
            resBuilder(specModel, prop, requiredIndex, ClassNames.STRING_RES, "resolveString"));
        dataHolder.addMethod(resWithVarargsBuilder(
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
                specModel,
                prop,
                requiredIndex,
                ClassNames.DIMEN_RES,
                "resolveDimenOffset"));
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
                specModel,
                prop,
                requiredIndex,
                ClassNames.DRAWABLE_RES,
                "resolveDrawable"));
        break;
      case NONE:
        if (prop.getType().equals(specModel.getComponentClass())) {
          dataHolder.addMethod(componentBuilder(specModel, prop, requiredIndex));
        } else {
          dataHolder.addMethod(regularBuilder(specModel, prop, requiredIndex));
        }
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

  static TypeName getRawType(TypeName type) {
    return type instanceof ParameterizedTypeName ? ((ParameterizedTypeName) type).rawType : type;
  }

  private static MethodSpec componentBuilder(
      SpecModel specModel,
      PropModel prop,
      int requiredIndex) {
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
      SpecModel specModel,
      PropModel prop,
      int requiredIndex,
      AnnotationSpec... extraAnnotations) {
    return builder(
        specModel,
        prop,
        requiredIndex,
        prop.getName(),
        Arrays.asList(parameter(prop, prop.getType(), prop.getName(), extraAnnotations)),
        prop.getName());
  }

  private static MethodSpec resBuilder(
      SpecModel specModel,
      PropModel prop,
      int requiredIndex,
      ClassName annotationClassName,
      String resolver) {
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
      SpecModel specModel,
      PropModel prop,
      int requiredIndex,
      ClassName annotationClassName,
      String resolver,
      TypeName varargsType,
      String varargsName) {
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
      SpecModel specModel,
      PropModel prop,
      int requiredIndex,
      ClassName annotationClassName,
      String resolver) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    dataHolder.addMethod(builder(
        specModel,
        prop,
        requiredIndex,
        prop.getName() + "Attr",
        Arrays.asList(
            parameter(prop, TypeName.INT, "attrResId", annotation(ClassNames.ATTR_RES)),
            parameter(prop, TypeName.INT, "defResId", annotation(annotationClassName))),
        "$L(attrResId, defResId)",
        resolver + "Attr"));

    dataHolder.addMethod(builder(
        specModel,
        prop,
        requiredIndex,
        prop.getName() + "Attr",
        Arrays.asList(parameter(prop, TypeName.INT, "attrResId", annotation(ClassNames.ATTR_RES))),
        "$L(attrResId, 0)",
        resolver + "Attr"));

    return dataHolder.build();
  }

  private static MethodSpec pxBuilder(
      SpecModel specModel,
      PropModel prop,
      int requiredIndex) {
    return builder(
        specModel,
        prop,
        requiredIndex,
        prop.getName() + "Px",
        Arrays.asList(parameter(prop, prop.getType(), prop.getName(), annotation(ClassNames.PX))),
        prop.getName());
  }

  private static MethodSpec dipBuilder(
      SpecModel specModel,
      PropModel prop,
      int requiredIndex) {
    AnnotationSpec dipAnnotation = AnnotationSpec.builder(ClassNames.DIMENSION)
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
      SpecModel specModel,
      PropModel prop,
      int requiredIndex) {
    AnnotationSpec spAnnotation = AnnotationSpec.builder(ClassNames.DIMENSION)
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
      SpecModel specModel,
      PropModel prop,
      int requiredIndex,
      ClassName builderClass) {
    return builder(
        specModel,
        prop,
        requiredIndex,
        prop.getName(),
        Arrays.asList(parameter(
            prop,
            ParameterizedTypeName.get(builderClass, getBuilderGenericTypes(prop)),
            prop.getName() + "Builder")),
        "$L.build()",
        prop.getName() + "Builder");
  }

  private static TypeName[] getBuilderGenericTypes(PropModel prop) {
    final TypeName typeParameter =
        prop.getType() instanceof ParameterizedTypeName &&
            !((ParameterizedTypeName) prop.getType()).typeArguments.isEmpty() ?
            ((ParameterizedTypeName) prop.getType()).typeArguments.get(0) :
            WildcardTypeName.subtypeOf(ClassNames.COMPONENT_LIFECYCLE);

    return new TypeName[]{typeParameter};
  }

  private static ParameterSpec parameter(
      PropModel prop,
      TypeName type,
      String name,
      AnnotationSpec... extraAnnotations) {
    final ParameterSpec.Builder builder =
        ParameterSpec.builder(type, name)
            .addAnnotations(prop.getExternalAnnotations());

    for (AnnotationSpec annotation : extraAnnotations) {
      builder.addAnnotation(annotation);
    }

    return builder.build();
  }

  private static AnnotationSpec annotation(ClassName className) {
    return AnnotationSpec.builder(className).build();
  }

  private static MethodSpec builder(
      SpecModel specModel,
      PropModel prop,
      int requiredIndex,
      String name,
      List<ParameterSpec> parameters,
      String statement,
      Object... formatObjects) {
    return getMethodSpecBuilder(
        specModel,
        prop,
        requiredIndex,
        name,
        parameters,
        statement,
        formatObjects).build();
  }

  private static MethodSpec.Builder getMethodSpecBuilder(
      SpecModel specModel,
      PropModel prop,
      int requiredIndex,
      String name,
      List<ParameterSpec> parameters,
      String statement,
      Object... formatObjects) {
    final MethodSpec.Builder builder =
        MethodSpec.methodBuilder(name)
            .addModifiers(Modifier.PUBLIC)
            .returns(BUILDER_CLASS_NAME)
            .addCode("this.$L.$L = ", getImplMemberInstanceName(specModel), prop.getName())
            .addStatement(statement, formatObjects);

    for (ParameterSpec param : parameters) {
      builder.addParameter(param);
    }

    if (!prop.isOptional()) {
      builder.addStatement("$L.set($L)", "mRequired", requiredIndex);
    }

    builder.addStatement("return this");

    return builder;
  }

  private static MethodSpec generateEventDeclarationBuilderMethod(
      SpecModel specModel,
      EventDeclarationModel eventDeclaration) {
    final String eventHandlerName =
        ComponentImplGenerator.getEventHandlerInstanceName(eventDeclaration.name);
    return MethodSpec.methodBuilder(eventHandlerName)
        .addModifiers(Modifier.PUBLIC)
        .returns(BUILDER_CLASS_NAME)
        .addParameter(ClassNames.EVENT_HANDLER, eventHandlerName)
        .addStatement("this.$L.$L = $L", getImplMemberInstanceName(specModel), eventHandlerName, eventHandlerName)
        .addStatement("return this")
        .build();
  }

  private static MethodSpec generateKeySetterMethod() {
    return MethodSpec.methodBuilder("key")
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ClassNames.STRING, "key")
        .addStatement("super.setKey(key)")
        .addStatement("return this")
        .returns(BUILDER_CLASS_NAME)
        .build();
  }

  private static MethodSpec generateBuildMethod(SpecModel specModel, int numRequiredProps) {
    final MethodSpec.Builder buildMethodBuilder = MethodSpec.methodBuilder("build")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(ParameterizedTypeName.get(
            specModel.getComponentClass(),
            specModel.getComponentTypeName()));

    if (numRequiredProps > 0) {
      buildMethodBuilder
          .beginControlFlow(
              "if (mRequired != null && mRequired.nextClearBit(0) < $L)", REQUIRED_PROPS_COUNT)
          .addStatement(
              "$T missingProps = new $T()",
              ParameterizedTypeName.get(List.class, String.class),
              ParameterizedTypeName.get(ArrayList.class, String.class))
          .beginControlFlow("for (int i = 0; i < $L; i++)", REQUIRED_PROPS_COUNT)
          .beginControlFlow("if (!mRequired.get(i))")
          .addStatement("missingProps.add($L[i])", REQUIRED_PROPS_NAMES)
          .endControlFlow()
          .endControlFlow()
          .addStatement(
              "throw new $T($S + $T.toString(missingProps.toArray()))",
              IllegalStateException.class,
              "The following props are not marked as optional and were not supplied: ",
              Arrays.class)
          .endControlFlow();
    }

    return buildMethodBuilder
        .addStatement(
            "$L $L = $L",
            ComponentImplGenerator.getImplClassName(specModel),
            ComponentImplGenerator.getImplInstanceName(specModel),
            getImplMemberInstanceName(specModel))
        .addStatement("release()")
        .addStatement("return $L", ComponentImplGenerator.getImplInstanceName(specModel))
        .build();
  }

  private static MethodSpec generateReleaseMethod(SpecModel specModel) {
    return MethodSpec.methodBuilder("release")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .addStatement("super.release()")
        .addStatement(getImplMemberInstanceName(specModel) + " = null")
        .addStatement(CONTEXT_MEMBER_NAME + " = null")
        .addStatement("$L.release(this)", BUILDER_POOL_FIELD)
        .build();
  }
}
