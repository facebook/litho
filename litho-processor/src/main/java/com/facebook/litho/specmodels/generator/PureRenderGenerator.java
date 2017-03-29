/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;

import java.lang.annotation.Annotation;

import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.specmodels.model.DelegateMethodModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.facebook.litho.specmodels.model.HasPureRender;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecModel;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import static com.facebook.litho.specmodels.generator.GeneratorConstants.DELEGATE_FIELD_NAME;

/**
 * Class that generates the pure render methods for a Component.
 */
public class PureRenderGenerator {

  private PureRenderGenerator() {
  }

  public static <S extends SpecModel & HasPureRender> TypeSpecDataHolder generate(S specModel) {
    TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    if (specModel.isPureRender()) {
      dataHolder.addTypeSpecDataHolder(generateIsPureRender());
      dataHolder.addTypeSpecDataHolder(generateShouldUpdateMethod(specModel));
    }

    return dataHolder.build();
  }

  static TypeSpecDataHolder generateIsPureRender() {
    return TypeSpecDataHolder.newBuilder().addMethod(
        MethodSpec.methodBuilder("isPureRender")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(TypeName.BOOLEAN)
            .addStatement("return true")
            .build())
        .build();
  }

  static TypeSpecDataHolder generateShouldUpdateMethod(SpecModel specModel) {
    final DelegateMethodModel methodModel =
        SpecModelUtils.getMethodModelWithAnnotation(specModel, ShouldUpdate.class);
    if (methodModel == null) {
      return TypeSpecDataHolder.newBuilder().build();
    }

    final MethodSpec.Builder shouldUpdateComponent =
        MethodSpec.methodBuilder("shouldUpdate")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(TypeName.BOOLEAN)
            .addParameter(specModel.getComponentClass(), "previous")
            .addParameter(specModel.getComponentClass(), "next");

    if (methodModel.methodParams.size() > 0) {
      shouldUpdateComponent
          .addStatement(
              "$L previousImpl = ($L) previous",
              ComponentImplGenerator.getImplClassName(specModel),
              ComponentImplGenerator.getImplClassName(specModel))
          .addStatement(
              "$L nextImpl = ($L) next",
              ComponentImplGenerator.getImplClassName(specModel),
              ComponentImplGenerator.getImplClassName(specModel));
    }

    final CodeBlock.Builder delegateParameters = CodeBlock.builder();
    delegateParameters.indent();
    final CodeBlock.Builder releaseDiffs = CodeBlock.builder();

    for (int i = 0, size = methodModel.methodParams.size(); i < size; i++) {
      MethodParamModel methodParam = methodModel.methodParams.get(i);
      shouldUpdateComponent
          .addStatement(
              "$T $L = acquireDiff(previousImpl.$L, nextImpl.$L)",
              methodParam.getType(),
              methodParam.getName(),
              methodParam.getName(),
              methodParam.getName());

      if (i != 0) {
        delegateParameters.add(",\n");
      }
      delegateParameters.add(methodParam.getName());

      releaseDiffs.addStatement("releaseDiff($L)", methodParam.getName());
    }
    delegateParameters.unindent();

    shouldUpdateComponent.addStatement(
        "boolean shouldUpdate = $L.$L($L)",
        DELEGATE_FIELD_NAME +
            (!specModel.hasInjectedDependencies() ?
                "" :
                specModel.getDependencyInjectionHelper()
                    .getSourceDelegateAccessorMethod(specModel)),
        methodModel.name,
        delegateParameters.build());
    shouldUpdateComponent.addCode(releaseDiffs.build());
    shouldUpdateComponent.addStatement("return shouldUpdate");

    return TypeSpecDataHolder.newBuilder().addMethod(shouldUpdateComponent.build()).build();
  }

  @Nullable
  static DelegateMethodModel getShouldUpdateMethodModel(SpecModel specModel) {
    for (DelegateMethodModel delegateMethodModel : specModel.getDelegateMethods()) {
      for (Annotation annotation : delegateMethodModel.annotations) {
        if (annotation.annotationType().equals(ShouldUpdate.class)) {
          return delegateMethodModel;
        }
      }
    }

    return null;
  }
}
