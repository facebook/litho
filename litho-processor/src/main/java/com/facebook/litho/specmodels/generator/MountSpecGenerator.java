/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.specmodels.generator;

import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.FromMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.InterStageInputParamModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.MountSpecModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;

/** Class that generates methods for Mount Specs. */
public class MountSpecGenerator {

  private MountSpecGenerator() {}

  public static TypeSpecDataHolder generateHasChildLithoViews(MountSpecModel specModel) {
    TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    if (specModel.canMountIncrementally()) {
      dataHolder.addMethod(
          MethodSpec.methodBuilder("hasChildLithoViews")
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
                .addModifiers(Modifier.PROTECTED)
                .returns(TypeName.INT)
                .addStatement("return $L", specModel.getPoolSize())
                .build())
        .build();
  }

  public static TypeSpecDataHolder generateCanPreallocate(MountSpecModel specModel) {
    return TypeSpecDataHolder.newBuilder()
        .addMethod(
            MethodSpec.methodBuilder("canPreallocate")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(TypeName.BOOLEAN)
                .addStatement("return $L", specModel.canPreallocate())
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

    final SpecMethodModel<DelegateMethod, Void> onMount =
        SpecModelUtils.getMethodModelWithAnnotation(specModel, OnMount.class);

    if (onMount == null) {
      return dataHolder.build();
    }

    for (MethodParamModel methodParamModel : onMount.methodParams) {
      if (methodParamModel instanceof InterStageInputParamModel
          && (SpecModelUtils.hasAnnotation(methodParamModel, FromMeasure.class)
              || SpecModelUtils.hasAnnotation(methodParamModel, FromBoundsDefined.class))) {
        return dataHolder
            .addMethod(
                MethodSpec.methodBuilder("isMountSizeDependent")
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

  public static TypeSpecDataHolder generateCallsShouldUpdateOnMount(MountSpecModel specModel) {
    final TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    final ShouldUpdate shouldUpdateAnnotation = getShouldUpdateAnnotation(specModel);

    if (shouldUpdateAnnotation != null && shouldUpdateAnnotation.onMount()) {
      dataHolder.addMethod(
          MethodSpec.methodBuilder("callsShouldUpdateOnMount")
              .addAnnotation(Override.class)
              .addModifiers(Modifier.PUBLIC)
              .returns(TypeName.BOOLEAN)
              .addStatement("return true")
              .build());
    }

    return dataHolder.build();
  }

  @Nullable
  private static ShouldUpdate getShouldUpdateAnnotation(MountSpecModel specModel) {
    for (SpecMethodModel<DelegateMethod, Void> delegateMethodModel :
        specModel.getDelegateMethods()) {
      for (Annotation annotation : delegateMethodModel.annotations) {
        if (annotation.annotationType().equals(ShouldUpdate.class)) {
          return (ShouldUpdate) annotation;
        }
      }
    }

    return null;
  }
}
