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
import com.facebook.litho.specmodels.generator.ComponentImplGenerator;
import com.facebook.litho.specmodels.generator.DelegateMethodGenerator;
import com.facebook.litho.specmodels.generator.EventGenerator;
import com.facebook.litho.specmodels.generator.PreambleGenerator;
import com.facebook.litho.specmodels.generator.ShouldUpdateGenerator;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.SpecModel;
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
public class ListComponentProcessor extends AbstractListComponentsProcessor {

  @Override
  protected void generate(GroupSectionSpecHelper groupSectionSpecHelper) {
    final GroupSectionSpecModel specModel = groupSectionSpecHelper.getSpecModel();
    generateCommonListComponent(groupSectionSpecHelper);
    DelegateMethodGenerator.generateDelegates(
        specModel,
        DelegateMethodDescriptions.getGroupSectionSpecDelegatesMap(specModel))
        .addToTypeSpec(groupSectionSpecHelper.getTypeSpec());
    groupSectionSpecHelper.generateTreePropsMethods();
  }

  @Override
  protected void generate(DiffSectionSpecHelper diffSectionSpecHelper) {
    final DiffSectionSpecModel specModel = diffSectionSpecHelper.getSpecModel();
    generateCommonListComponent(diffSectionSpecHelper);
    DelegateMethodGenerator.generateDelegates(
            specModel, DelegateMethodDescriptions.getDiffSectionSpecDelegatesMap(specModel))
        .addToTypeSpec(diffSectionSpecHelper.getTypeSpec());
  }

  private <S extends SpecModel & HasService> void generateCommonListComponent(
      ListSpecHelper<S> listSpecHelper) {
    S specModel = listSpecHelper.getSpecModel();
    listSpecHelper.getTypeSpec().addModifiers(Modifier.FINAL);
    final Stages stages = listSpecHelper.getStages();

    stages.generateSourceDelegate(true);
    PreambleGenerator.generate(specModel).addToTypeSpec(listSpecHelper.getTypeSpec());

    ComponentImplGenerator.generate(specModel, specModel.getServiceParam())
        .addToTypeSpec(listSpecHelper.getTypeSpec());
    stages.generateTransferState(
        SectionClassNames.SECTION_CONTEXT,
        SectionClassNames.SECTION,
        SectionClassNames.STATE_CONTAINER_SECTION);

    ShouldUpdateGenerator.generate(specModel).addToTypeSpec(listSpecHelper.getTypeSpec());
    listSpecHelper.generateCreateInitialState();

    BuilderGenerator.generate(listSpecHelper.getSpecModel())
        .addToTypeSpec(listSpecHelper.getTypeSpec());

    stages.generateOnStateUpdateMethods(
        SectionClassNames.SECTION_CONTEXT,
        SectionClassNames.SECTION,
        SectionClassNames.STATE_CONTAINER_SECTION,
        SectionClassNames.SECTION_STATE_UPDATE,
        Stages.StaticFlag.STATIC);
    stages.generateLazyStateUpdateMethods(
        SectionClassNames.SECTION_CONTEXT,
        SectionClassNames.SECTION,
        SectionClassNames.STATE_CONTAINER_SECTION,
        SectionClassNames.SECTION_STATE_UPDATE);

    EventGenerator.generate(specModel).addToTypeSpec(listSpecHelper.getTypeSpec());
  }

  @Override
  @Nullable
  protected DependencyInjectionHelper getDependencyInjectionGenerator(TypeElement typeElement) {
    return null;
  }
}
