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

package com.facebook.litho.specmodels.model;

import com.facebook.litho.specmodels.generator.BuilderGenerator;
import com.facebook.litho.specmodels.generator.CachedValueGenerator;
import com.facebook.litho.specmodels.generator.ClassAnnotationsGenerator;
import com.facebook.litho.specmodels.generator.ComponentBodyGenerator;
import com.facebook.litho.specmodels.generator.DelegateMethodGenerator;
import com.facebook.litho.specmodels.generator.EventGenerator;
import com.facebook.litho.specmodels.generator.JavadocGenerator;
import com.facebook.litho.specmodels.generator.PreambleGenerator;
import com.facebook.litho.specmodels.generator.PureRenderGenerator;
import com.facebook.litho.specmodels.generator.RenderDataGenerator;
import com.facebook.litho.specmodels.generator.SimpleNameDelegateGenerator;
import com.facebook.litho.specmodels.generator.StateGenerator;
import com.facebook.litho.specmodels.generator.TagGenerator;
import com.facebook.litho.specmodels.generator.TreePropGenerator;
import com.facebook.litho.specmodels.generator.TriggerGenerator;
import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.facebook.litho.specmodels.generator.WorkingRangeGenerator;
import com.facebook.litho.specmodels.internal.RunMode;
import com.squareup.javapoet.TypeSpec;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class DefaultLayoutSpecGenerator implements SpecGenerator<LayoutSpecModel> {

  @Override
  public TypeSpec generate(LayoutSpecModel layoutSpecModel, EnumSet<RunMode> runMode) {
    final TypeSpec.Builder typeSpec =
        TypeSpec.classBuilder(layoutSpecModel.getComponentName())
            .superclass(ClassNames.COMPONENT)
            .addTypeVariables(layoutSpecModel.getTypeVariables());

    if (SpecModelUtils.isTypeElement(layoutSpecModel)) {
      typeSpec.addOriginatingElement((TypeElement) layoutSpecModel.getRepresentedObject());
    }

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
        .addTypeSpecDataHolder(ComponentBodyGenerator.generate(layoutSpecModel, null, runMode))
        .addTypeSpecDataHolder(TreePropGenerator.generate(layoutSpecModel))
        .addTypeSpecDataHolder(
            DelegateMethodGenerator.generateDelegates(
                layoutSpecModel,
                DelegateMethodDescriptions.LAYOUT_SPEC_DELEGATE_METHODS_MAP,
                runMode))
        .addTypeSpecDataHolder(PureRenderGenerator.generate(layoutSpecModel))
        .addTypeSpecDataHolder(EventGenerator.generate(layoutSpecModel))
        .addTypeSpecDataHolder(TriggerGenerator.generate(layoutSpecModel))
        .addTypeSpecDataHolder(WorkingRangeGenerator.generate(layoutSpecModel))
        .addTypeSpecDataHolder(StateGenerator.generate(layoutSpecModel))
        .addTypeSpecDataHolder(RenderDataGenerator.generate(layoutSpecModel))
        .addTypeSpecDataHolder(BuilderGenerator.generate(layoutSpecModel))
        .addTypeSpecDataHolder(TagGenerator.generate(layoutSpecModel, new LinkedHashSet<>()))
        .addTypeSpecDataHolder(CachedValueGenerator.generate(layoutSpecModel, runMode))
        .addTypeSpecDataHolder(SimpleNameDelegateGenerator.generate(layoutSpecModel))
        .build()
        .addToTypeSpec(typeSpec);

    return typeSpec.build();
  }
}
