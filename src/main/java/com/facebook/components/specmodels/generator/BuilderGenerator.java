// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.generator;

import javax.lang.model.element.Modifier;

import com.facebook.components.specmodels.model.ClassNames;
import com.facebook.components.specmodels.model.SpecModel;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;

/**
 * Class that generates the builder for a Component.
 */
public class BuilderGenerator {
  private static final String BUILDER = "Builder";
  private static final String BUILDER_POOL_FIELD = "mBuilderPool";
  private static final ClassName BUILDER_CLASS_NAME = ClassName.bestGuess(BUILDER);

  private BuilderGenerator() {
  }

  public static TypeSpecDataHolder generate(
      SpecModel specModel,
      boolean isStyleable,
      ClassName contextClass) {
    TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    dataHolder.addMethod(generateCreateBuilderMethodWithStyle(specModel));
    dataHolder.addMethod(generateCreateBuilderMethod(specModel));
    dataHolder.addTypeSpecDataHolder(generateFactoryMethod(specModel, isStyleable, contextClass));

    return dataHolder.build();
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

  static TypeSpecDataHolder generateFactoryMethod(
      SpecModel specModel,
      boolean isStyleable,
      ClassName contextClass) {
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
        .addParameter(contextClass, "context")
        .addStatement("$T builder = $L.acquire()", BUILDER_CLASS_NAME, BUILDER_POOL_FIELD)
        .beginControlFlow("if (builder == null)")
        .addStatement("builder = new $T()", BUILDER_CLASS_NAME)
        .endControlFlow();

    if (isStyleable) {
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

  static String getFactoryMethodName() {
    return "new" + BUILDER;
  }
}
