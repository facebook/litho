/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.specmodels.model.DelegateMethodModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import javax.lang.model.element.Modifier;

/** Class that generates the pure render methods for a Component. */
public class ShouldUpdateGenerator {

  private ShouldUpdateGenerator() {}

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    dataHolder.addTypeSpecDataHolder(generateShouldUpdateMethod(specModel));

    return dataHolder.build();
  }

  private static TypeSpecDataHolder generateShouldUpdateMethod(SpecModel specModel) {
    TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();
    final DelegateMethodModel methodModel =
        SpecModelUtils.getMethodModelWithAnnotation(specModel, ShouldUpdate.class);
    if (methodModel == null) {
      return dataHolder.build();
    }

    final ShouldUpdate annotation = getShouldUpdateAnnotation(methodModel);
    if (annotation.onMount()) {
      dataHolder.addMethod(
          MethodSpec.methodBuilder("callsShouldUpdateOnMount")
              .addAnnotation(Override.class)
              .addModifiers(Modifier.PUBLIC)
              .returns(TypeName.BOOLEAN)
              .addStatement("return true")
              .build());
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
      shouldUpdateComponent.addStatement(
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
        SpecModelUtils.getSpecAccessor(specModel),
        methodModel.name,
        delegateParameters.build());
    shouldUpdateComponent.addCode(releaseDiffs.build());
    shouldUpdateComponent.addStatement("return shouldUpdate");

    return dataHolder.addMethod(shouldUpdateComponent.build()).build();
  }

  private static ShouldUpdate getShouldUpdateAnnotation(DelegateMethodModel delegateMethodModel) {
    for (Annotation annotation : delegateMethodModel.annotations) {
      if (annotation.annotationType().equals(ShouldUpdate.class)) {
        return (ShouldUpdate) annotation;
      }
    }

    throw new RuntimeException("Expected to find a ShouldUpdate annotation");
  }
}
