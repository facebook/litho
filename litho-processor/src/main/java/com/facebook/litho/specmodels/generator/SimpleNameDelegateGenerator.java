/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.specmodels.generator;

import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import com.squareup.javapoet.MethodSpec;
import javax.lang.model.element.Modifier;

/**
 * Generates a Component.getSimpleNameDelegate method if LayoutSpec.simpleNameDelegate is defined.
 */
public class SimpleNameDelegateGenerator {

  private SimpleNameDelegateGenerator() {}

  public static TypeSpecDataHolder generate(LayoutSpecModel specModel) {
    final String delegatesToPropName = specModel.getSimpleNameDelegate();
    final TypeSpecDataHolder.Builder builder = TypeSpecDataHolder.newBuilder();
    if (delegatesToPropName == null || delegatesToPropName.isEmpty()) {
      return builder.build();
    }

    MethodSpec.Builder getDelegateComponentBuilder =
        MethodSpec.methodBuilder("getSimpleNameDelegate")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PROTECTED)
            .returns(ClassNames.COMPONENT)
            .addStatement("return $L", delegatesToPropName);
    builder.addMethod(getDelegateComponentBuilder.build());

    return builder.build();
  }
}
