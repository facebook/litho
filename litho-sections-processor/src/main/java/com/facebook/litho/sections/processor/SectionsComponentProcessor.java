/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor;

import com.facebook.litho.sections.annotations.DiffSectionSpec;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.processor.specmodels.model.DelegateMethodDescriptions;
import com.facebook.litho.sections.processor.specmodels.model.DiffSectionSpecModel;
import com.facebook.litho.sections.processor.specmodels.model.GroupSectionSpecModel;
import com.facebook.litho.sections.processor.specmodels.model.HasService;
import com.facebook.litho.specmodels.generator.BuilderGenerator;
import com.facebook.litho.specmodels.generator.ClassAnnotationsGenerator;
import com.facebook.litho.specmodels.generator.ComponentImplGenerator;
import com.facebook.litho.specmodels.generator.DelegateMethodGenerator;
import com.facebook.litho.specmodels.generator.EventGenerator;
import com.facebook.litho.specmodels.generator.PreambleGenerator;
import com.facebook.litho.specmodels.generator.StateGenerator;
import com.facebook.litho.specmodels.generator.TreePropGenerator;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.SpecModel;
import com.squareup.javapoet.TypeSpec;
import javax.annotation.Nullable;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * Processor used to generate <code>com.facebook.litho.sections.SectionLifecycle</code> and
 * <code>com.facebook.litho.sections.Section</code> classes for a {@link GroupSectionSpec}
 * or a {@link DiffSectionSpec}.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class SectionsComponentProcessor extends AbstractSectionsComponentProcessor {

  @Override
  protected void generate(GroupSectionSpecHelper groupSectionSpecHelper) {
    final GroupSectionSpecModel specModel = groupSectionSpecHelper.getSpecModel();
    final TypeSpec.Builder typeSpec = groupSectionSpecHelper.getTypeSpec();

    generateCommonListComponent(specModel, typeSpec);
    DelegateMethodGenerator.generateDelegates(
            specModel, DelegateMethodDescriptions.getGroupSectionSpecDelegatesMap(specModel))
        .addToTypeSpec(typeSpec);
    TreePropGenerator.generate(specModel).addToTypeSpec(typeSpec);
  }

  @Override
  protected void generate(DiffSectionSpecHelper diffSectionSpecHelper) {
    final DiffSectionSpecModel specModel = diffSectionSpecHelper.getSpecModel();
    final TypeSpec.Builder typeSpec = diffSectionSpecHelper.getTypeSpec();

    generateCommonListComponent(specModel, typeSpec);
    DelegateMethodGenerator.generateDelegates(
            specModel, DelegateMethodDescriptions.getDiffSectionSpecDelegatesMap(specModel))
        .addToTypeSpec(diffSectionSpecHelper.getTypeSpec());
  }

  private <S extends SpecModel & HasService> void generateCommonListComponent(
      S specModel, TypeSpec.Builder typeSpec) {

    if (specModel.hasInjectedDependencies()) {
      specModel.getDependencyInjectionHelper().generate(specModel).addToTypeSpec(typeSpec);
    } else {
      typeSpec.addModifiers(Modifier.FINAL);
    }

    PreambleGenerator.generate(specModel).addToTypeSpec(typeSpec);
    ClassAnnotationsGenerator.generate(specModel).addToTypeSpec(typeSpec);
    ComponentImplGenerator.generate(specModel, specModel.getServiceParam()).addToTypeSpec(typeSpec);
    BuilderGenerator.generate(specModel).addToTypeSpec(typeSpec);
    StateGenerator.generate(specModel).addToTypeSpec(typeSpec);
    EventGenerator.generate(specModel).addToTypeSpec(typeSpec);
  }

  @Override
  @Nullable
  protected DependencyInjectionHelper getDependencyInjectionGenerator(TypeElement typeElement) {
    return null;
  }
}
