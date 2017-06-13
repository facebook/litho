/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.FromMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DelegateMethodModel;
import com.facebook.litho.specmodels.model.InterStageInputParamModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.MountSpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Modifier;

/**
 * Class that generates methods for Mount Specs.
 */
public class MountSpecGenerator {

  private MountSpecGenerator() {
  }

  public static TypeSpecDataHolder generateCanMountIncrementally(MountSpecModel specModel) {
    TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    if (specModel.canMountIncrementally()) {
      dataHolder.addMethod(
          MethodSpec.methodBuilder("canMountIncrementally")
              .addAnnotation(Override.class)
              .addModifiers(Modifier.PUBLIC)
              .returns(TypeName.BOOLEAN)
              .addStatement("return true")
              .build());
    }

    return dataHolder.build();
  }

  public static TypeSpecDataHolder generateShouldUseDisplayList(MountSpecModel specModel) {
    TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    if (specModel.shouldUseDisplayList()) {
      dataHolder.addMethod(
          MethodSpec.methodBuilder("shouldUseDisplayList")
              .addAnnotation(Override.class)
              .addModifiers(Modifier.PUBLIC)
              .returns(TypeName.BOOLEAN)
              .addStatement("return true")
              .build());
    }

    return dataHolder.build();
  }

  public static TypeSpecDataHolder generatePoolSize(MountSpecModel specModel) {
    return TypeSpecDataHolder.newBuilder()
        .addMethod(
            MethodSpec.methodBuilder("poolSize")
                .addAnnotation(Override.class)
                .addModifiers(javax.lang.model.element.Modifier.PROTECTED)
                .returns(TypeName.INT)
                .addStatement("return $L", specModel.getPoolSize())
                .build())
        .build();
  }

  public static TypeSpecDataHolder generateGetMountType(MountSpecModel specModel) {
    return TypeSpecDataHolder.newBuilder()
        .addMethod(
            MethodSpec.methodBuilder("getMountType")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE)
                .addStatement("return $T", specModel.getMountType())
                .build())
        .build();
  }

  public static TypeSpecDataHolder generateIsMountSizeDependent(MountSpecModel specModel) {
    TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    final DelegateMethodModel onMount =
        SpecModelUtils.getMethodModelWithAnnotation(specModel, OnMount.class);

    if (onMount == null) {
      return dataHolder.build();
    }

    for (MethodParamModel methodParamModel : onMount.methodParams) {
      if (methodParamModel instanceof InterStageInputParamModel &&
          (SpecModelUtils.hasAnnotation(methodParamModel, FromMeasure.class) ||
           SpecModelUtils.hasAnnotation(methodParamModel, FromBoundsDefined.class))) {
        return dataHolder
            .addMethod(MethodSpec.methodBuilder("isMountSizeDependent")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(TypeName.BOOLEAN)
                .addStatement("return true")
                .build())
            .build();
      }
    }

    return dataHolder.build();
  }
}
