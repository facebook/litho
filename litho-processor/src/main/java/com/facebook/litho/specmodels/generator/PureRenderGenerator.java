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

import com.facebook.litho.specmodels.model.HasPureRender;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import javax.lang.model.element.Modifier;

public class PureRenderGenerator {

  private PureRenderGenerator() {}

  public static TypeSpecDataHolder generate(HasPureRender specModel) {
    TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    if (specModel.isPureRender()) {
      dataHolder.addMethod(
          MethodSpec.methodBuilder("isPureRender")
              .addAnnotation(Override.class)
              .addModifiers(Modifier.PUBLIC)
              .returns(TypeName.BOOLEAN)
              .addStatement("return true")
              .build());
    }

    return dataHolder.build();
  }
}
