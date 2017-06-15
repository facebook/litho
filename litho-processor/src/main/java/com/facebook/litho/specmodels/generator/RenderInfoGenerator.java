/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Modifier;

import static com.facebook.litho.specmodels.generator.GeneratorConstants.IMPL_VARIABLE_NAME;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.PREVIOUS_RENDER_INFO_FIELD_NAME;

/**
 * Generates delegate methods for RenderInfo (which is used by lifecycle methods that support Diff
 * params).
 */
public final class RenderInfoGenerator {

  private RenderInfoGenerator() {
  }

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    if (!SpecModelUtils.hasDiffThatNeedsRenderInfoInfra(specModel)) {
      return TypeSpecDataHolder.newBuilder().build();
    }

    return TypeSpecDataHolder.newBuilder()
        .addMethod(generateNeedsPreviousRenderInfoMethod())
        .addMethod(generateRecordRenderInfoMethod(specModel))
        .addMethod(generateApplyPreviousRenderInfoMethod(specModel))
        .build();
  }

  private static MethodSpec generateNeedsPreviousRenderInfoMethod() {
    return MethodSpec.methodBuilder("needsPreviousRenderInfo")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .returns(TypeName.BOOLEAN)
        .addStatement("return true")
        .build();
  }

  private static MethodSpec generateRecordRenderInfoMethod(SpecModel specModel) {
    final TypeName renderInfoTypeName = ClassName.bestGuess(getRenderInfoImplClassName(specModel));
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

    return MethodSpec.methodBuilder("recordRenderInfo")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .addParameter(ClassNames.COMPONENT, "previousComponent")
        .addParameter(ClassNames.RENDER_INFO, "toRecycle")
        .returns(ClassNames.RENDER_INFO)
        .addCode(code)
        .build();
  }

  private static MethodSpec generateApplyPreviousRenderInfoMethod(SpecModel specModel) {
    final TypeName renderInfoTypeName = ClassName.bestGuess(getRenderInfoImplClassName(specModel));
    final TypeName implTypeName = ClassName.bestGuess(specModel.getComponentName() + "Impl");
    final CodeBlock code = CodeBlock.builder()
        .addStatement(
            "$T $L = ($T) $L",
            implTypeName,
            IMPL_VARIABLE_NAME,
            implTypeName,
            "component")
        .beginControlFlow("if ($L == null)", "previousRenderInfo")
        .addStatement("$L.$L = null", IMPL_VARIABLE_NAME, PREVIOUS_RENDER_INFO_FIELD_NAME)
        .addStatement("return")
        .endControlFlow()
        .beginControlFlow("if ($L.$L == null)", IMPL_VARIABLE_NAME, PREVIOUS_RENDER_INFO_FIELD_NAME)
        .addStatement(
            "$L.$L = new $T()",
            IMPL_VARIABLE_NAME,
            PREVIOUS_RENDER_INFO_FIELD_NAME,
            renderInfoTypeName)
        .endControlFlow()
        .addStatement(
            "$T $L = ($T) $L",
            renderInfoTypeName,
            "infoImpl",
            renderInfoTypeName,
            "previousRenderInfo")
        .addStatement(
            "$L.$L.copy($L)",
            IMPL_VARIABLE_NAME,
            PREVIOUS_RENDER_INFO_FIELD_NAME,
            "infoImpl")
        .build();

    return MethodSpec.methodBuilder("applyPreviousRenderInfo")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .addParameter(ClassNames.COMPONENT, "component")
        .addParameter(ClassNames.RENDER_INFO, "previousRenderInfo")
        .addCode(code)
        .build();
  }

  static String getRenderInfoImplClassName(SpecModel specModel) {
    return specModel.getComponentName() + "RenderInfo";
  }
}
