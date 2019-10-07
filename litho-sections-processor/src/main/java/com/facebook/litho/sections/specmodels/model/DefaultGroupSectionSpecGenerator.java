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

package com.facebook.litho.sections.specmodels.model;

import com.facebook.litho.specmodels.generator.BuilderGenerator;
import com.facebook.litho.specmodels.generator.CachedValueGenerator;
import com.facebook.litho.specmodels.generator.ComponentBodyGenerator;
import com.facebook.litho.specmodels.generator.DelegateMethodGenerator;
import com.facebook.litho.specmodels.generator.EventGenerator;
import com.facebook.litho.specmodels.generator.JavadocGenerator;
import com.facebook.litho.specmodels.generator.PreambleGenerator;
import com.facebook.litho.specmodels.generator.StateGenerator;
import com.facebook.litho.specmodels.generator.TagGenerator;
import com.facebook.litho.specmodels.generator.TreePropGenerator;
import com.facebook.litho.specmodels.generator.TriggerGenerator;
import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.SpecGenerator;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class DefaultGroupSectionSpecGenerator implements SpecGenerator<GroupSectionSpecModel> {

  private final Set<ClassName> mBlacklistedTagInterfaces;

  public DefaultGroupSectionSpecGenerator() {
    this(new LinkedHashSet<>());
  }

  public DefaultGroupSectionSpecGenerator(Set<ClassName> blacklistedTagInterfaces) {
    mBlacklistedTagInterfaces = blacklistedTagInterfaces;
  }

  @Override
  public TypeSpec generate(GroupSectionSpecModel specModel, EnumSet<RunMode> runMode) {

    final TypeSpec.Builder typeSpec =
        TypeSpec.classBuilder(specModel.getComponentName())
            .superclass(SectionClassNames.SECTION)
            .addTypeVariables(specModel.getTypeVariables());

    if (SpecModelUtils.isTypeElement(specModel)) {
      typeSpec.addOriginatingElement((TypeElement) specModel.getRepresentedObject());
    }

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
                specModel,
                DelegateMethodDescriptions.getGroupSectionSpecDelegatesMap(specModel),
                runMode))
        .addTypeSpecDataHolder(TreePropGenerator.generate(specModel))
        .addTypeSpecDataHolder(TriggerGenerator.generate(specModel))
        .addTypeSpecDataHolder(TagGenerator.generate(specModel, mBlacklistedTagInterfaces))
        .addTypeSpecDataHolder(CachedValueGenerator.generate(specModel))
        .build()
        .addToTypeSpec(typeSpec);

    return typeSpec.build();
  }
}
