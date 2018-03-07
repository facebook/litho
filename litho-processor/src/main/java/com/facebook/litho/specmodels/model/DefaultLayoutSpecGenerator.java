/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.facebook.litho.specmodels.generator.BuilderGenerator;
import com.facebook.litho.specmodels.generator.ClassAnnotationsGenerator;
import com.facebook.litho.specmodels.generator.ComponentBodyGenerator;
import com.facebook.litho.specmodels.generator.DelegateMethodGenerator;
import com.facebook.litho.specmodels.generator.EventGenerator;
import com.facebook.litho.specmodels.generator.JavadocGenerator;
import com.facebook.litho.specmodels.generator.PreambleGenerator;
import com.facebook.litho.specmodels.generator.PureRenderGenerator;
import com.facebook.litho.specmodels.generator.RenderDataGenerator;
import com.facebook.litho.specmodels.generator.StateGenerator;
import com.facebook.litho.specmodels.generator.TagGenerator;
import com.facebook.litho.specmodels.generator.TreePropGenerator;
import com.facebook.litho.specmodels.generator.TriggerGenerator;
import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.squareup.javapoet.TypeSpec;
import java.util.LinkedHashSet;
import javax.lang.model.element.Modifier;

public class DefaultLayoutSpecGenerator implements SpecGenerator<LayoutSpecModel> {

  @Override
  public TypeSpec generate(LayoutSpecModel layoutSpecModel) {
    final TypeSpec.Builder typeSpec =
        TypeSpec.classBuilder(layoutSpecModel.getComponentName())
            .superclass(ClassNames.COMPONENT)
            .addTypeVariables(layoutSpecModel.getTypeVariables());

    if (layoutSpecModel.isPublic()) {
      typeSpec.addModifiers(Modifier.PUBLIC);
    }

    if (!layoutSpecModel.hasInjectedDependencies()) {
      typeSpec.addModifiers(Modifier.FINAL);
    }

    TypeSpecDataHolder.newBuilder()
        .addTypeSpecDataHolder(JavadocGenerator.generate(layoutSpecModel))
        .addTypeSpecDataHolder(ClassAnnotationsGenerator.generate(layoutSpecModel))
        .addTypeSpecDataHolder(PreambleGenerator.generate(layoutSpecModel))
        .addTypeSpecDataHolder(ComponentBodyGenerator.generate(layoutSpecModel, null))
        .addTypeSpecDataHolder(TreePropGenerator.generate(layoutSpecModel))
        .addTypeSpecDataHolder(
            DelegateMethodGenerator.generateDelegates(
                layoutSpecModel, DelegateMethodDescriptions.LAYOUT_SPEC_DELEGATE_METHODS_MAP))
        .addTypeSpecDataHolder(PureRenderGenerator.generate(layoutSpecModel))
        .addTypeSpecDataHolder(EventGenerator.generate(layoutSpecModel))
        .addTypeSpecDataHolder(TriggerGenerator.generate(layoutSpecModel))
        .addTypeSpecDataHolder(StateGenerator.generate(layoutSpecModel))
        .addTypeSpecDataHolder(RenderDataGenerator.generate(layoutSpecModel))
        .addTypeSpecDataHolder(BuilderGenerator.generate(layoutSpecModel))
        .addTypeSpecDataHolder(TagGenerator.generate(layoutSpecModel, new LinkedHashSet<>()))
        .build()
        .addToTypeSpec(typeSpec);

    return typeSpec.build();
  }
}
