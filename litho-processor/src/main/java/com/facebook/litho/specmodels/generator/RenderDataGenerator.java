/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import static com.facebook.litho.specmodels.generator.GeneratorConstants.IMPL_VARIABLE_NAME;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.PREVIOUS_RENDER_DATA_FIELD_NAME;

import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import javax.lang.model.element.Modifier;

/**
 * Generates delegate methods for RenderData (which is used by lifecycle methods that support Diff
 * params).
 */
public final class RenderDataGenerator {

  private RenderDataGenerator() {}

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    if (!SpecModelUtils.hasDiffThatNeedsRenderDataInfra(specModel)) {
      return TypeSpecDataHolder.newBuilder().build();
    }

    return TypeSpecDataHolder.newBuilder()
        .addMethod(generateNeedsPreviousRenderDataMethod())
        .addMethod(generateRecordRenderDataMethod(specModel))
        .addMethod(generateApplyPreviousRenderDataMethod(specModel))
        .build();
  }

  private static MethodSpec generateNeedsPreviousRenderDataMethod() {
    return MethodSpec.methodBuilder("needsPreviousRenderData")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .returns(TypeName.BOOLEAN)
        .addStatement("return true")
        .build();
  }

  private static MethodSpec generateRecordRenderDataMethod(SpecModel specModel) {
    final TypeName renderInfoTypeName = ClassName.bestGuess(getRenderDataImplClassName(specModel));
    final TypeName implTypeName = ClassName.bestGuess(specModel.getComponentName() + "Impl");
    final CodeBlock code = CodeBlock.builder()
        .addStatement(
            "$T $L = ($T) $L",
            implTypeName,
            IMPL_VARIABLE_NAME,
            implTypeName,
            "previousComponent")
        .add(
            "$T $L = $L != null ?\n",
            renderInfoTypeName,
            "renderInfo",
            "toRecycle")
        .indent()
        .add("($T) $L :\n", renderInfoTypeName, "toRecycle")
        .add("new $T();\n", renderInfoTypeName)
        .unindent()
        .addStatement("$L.record($L)", "renderInfo", IMPL_VARIABLE_NAME)
        .addStatement("return $L", "renderInfo")
        .build();

    return MethodSpec.methodBuilder("recordRenderData")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .addParameter(ClassNames.COMPONENT, "previousComponent")
        .addParameter(ClassNames.RENDER_DATA, "toRecycle")
        .returns(ClassNames.RENDER_DATA)
        .addCode(code)
        .build();
  }

  private static MethodSpec generateApplyPreviousRenderDataMethod(SpecModel specModel) {
    final TypeName renderInfoTypeName = ClassName.bestGuess(getRenderDataImplClassName(specModel));
    final TypeName implTypeName = ClassName.bestGuess(specModel.getComponentName() + "Impl");
    final CodeBlock code =
        CodeBlock.builder()
            .addStatement(
                "$T $L = ($T) $L", implTypeName, IMPL_VARIABLE_NAME, implTypeName, "component")
            .beginControlFlow("if ($L == null)", "previousRenderData")
            .addStatement("$L.$L = null", IMPL_VARIABLE_NAME, PREVIOUS_RENDER_DATA_FIELD_NAME)
            .addStatement("return")
            .endControlFlow()
            .beginControlFlow(
                "if ($L.$L == null)", IMPL_VARIABLE_NAME, PREVIOUS_RENDER_DATA_FIELD_NAME)
            .addStatement(
                "$L.$L = new $T()",
                IMPL_VARIABLE_NAME,
                PREVIOUS_RENDER_DATA_FIELD_NAME,
                renderInfoTypeName)
            .endControlFlow()
            .addStatement(
                "$T $L = ($T) $L",
                renderInfoTypeName,
                "infoImpl",
                renderInfoTypeName,
                "previousRenderData")
            .addStatement(
                "$L.$L.copy($L)", IMPL_VARIABLE_NAME, PREVIOUS_RENDER_DATA_FIELD_NAME, "infoImpl")
            .build();

    return MethodSpec.methodBuilder("applyPreviousRenderData")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .addParameter(ClassNames.COMPONENT, "component")
        .addParameter(ClassNames.RENDER_DATA, "previousRenderData")
        .addCode(code)
        .build();
  }

  static String getRenderDataImplClassName(SpecModel specModel) {
    return specModel.getComponentName() + "RenderData";
  }
}
