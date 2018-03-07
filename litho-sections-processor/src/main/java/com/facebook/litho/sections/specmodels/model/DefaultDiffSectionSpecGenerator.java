/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.specmodels.model;

import com.facebook.litho.specmodels.generator.BuilderGenerator;
import com.facebook.litho.specmodels.generator.ComponentBodyGenerator;
import com.facebook.litho.specmodels.generator.DelegateMethodGenerator;
import com.facebook.litho.specmodels.generator.EventGenerator;
import com.facebook.litho.specmodels.generator.JavadocGenerator;
import com.facebook.litho.specmodels.generator.PreambleGenerator;
import com.facebook.litho.specmodels.generator.StateGenerator;
import com.facebook.litho.specmodels.generator.TagGenerator;
import com.facebook.litho.specmodels.generator.TriggerGenerator;
import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.facebook.litho.specmodels.model.SpecGenerator;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

public class DefaultDiffSectionSpecGenerator implements SpecGenerator<DiffSectionSpecModel> {

  @Override
  public TypeSpec generate(DiffSectionSpecModel specModel) {
    final TypeSpec.Builder typeSpec =
        TypeSpec.classBuilder(specModel.getComponentName())
            .superclass(SectionClassNames.SECTION)
            .addTypeVariables(specModel.getTypeVariables());

    if (specModel.isPublic()) {
      typeSpec.addModifiers(Modifier.PUBLIC);
    }

    if (!specModel.hasInjectedDependencies()) {
      typeSpec.addModifiers(Modifier.FINAL);
    }

    TypeSpecDataHolder.newBuilder()
        .addTypeSpecDataHolder(JavadocGenerator.generate(specModel))
        .addTypeSpecDataHolder(PreambleGenerator.generate(specModel))
        .addTypeSpecDataHolder(
            ComponentBodyGenerator.generate(specModel, specModel.getServiceParam()))
        .addTypeSpecDataHolder(BuilderGenerator.generate(specModel))
        .addTypeSpecDataHolder(StateGenerator.generate(specModel))
        .addTypeSpecDataHolder(EventGenerator.generate(specModel))
        .addTypeSpecDataHolder(
            DelegateMethodGenerator.generateDelegates(
                specModel, DelegateMethodDescriptions.getDiffSectionSpecDelegatesMap(specModel)))
        .addTypeSpecDataHolder(TriggerGenerator.generate(specModel))
        .addTypeSpecDataHolder(TagGenerator.generate(specModel))
        .build()
        .addToTypeSpec(typeSpec);

    return typeSpec.build();
  }
}
