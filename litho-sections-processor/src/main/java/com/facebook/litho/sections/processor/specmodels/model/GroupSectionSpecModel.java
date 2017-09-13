/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor.specmodels.model;

import com.facebook.litho.sections.processor.SectionClassNames;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.BuilderMethodModel;
import com.facebook.litho.specmodels.model.DelegateMethodModel;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethodModel;
import com.facebook.litho.specmodels.model.InterStageInputParamModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.PropDefaultModel;
import com.facebook.litho.specmodels.model.PropJavadocModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.RenderDataDiffModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelImpl;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.facebook.litho.specmodels.model.StateParamModel;
import com.facebook.litho.specmodels.model.TreePropModel;
import com.facebook.litho.specmodels.model.UpdateStateMethodModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Model that is an abstract representation of a {@link
 * com.facebook.litho.sections.annotations.GroupSectionSpec}.
 */
public class GroupSectionSpecModel implements SpecModel, HasService {

  private final SpecModelImpl mSpecModel;
  private final @Nullable MethodParamModel mServiceParam;

  public GroupSectionSpecModel(
      String qualifiedSpecClassName,
      String componentClassName,
      ImmutableList<DelegateMethodModel> delegateMethods,
      ImmutableList<EventMethodModel> eventMethods,
      ImmutableList<EventMethodModel> triggerMethods,
      ImmutableList<UpdateStateMethodModel> updateStateMethods,
      ImmutableList<TypeVariableName> typeVariables,
      ImmutableList<PropDefaultModel> propDefaults,
      ImmutableList<EventDeclarationModel> eventDeclarations,
      ImmutableList<BuilderMethodModel> builderMethodModels,
      String classJavadoc,
      ImmutableList<PropJavadocModel> propJavadocs,
      boolean isPublic,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper,
      Object representedObject) {
    mSpecModel =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName(qualifiedSpecClassName)
            .componentClassName(componentClassName)
            .componentClass(SectionClassNames.SECTION)
            .delegateMethods(delegateMethods)
            .updateStateMethods(updateStateMethods)
            .typeVariables(typeVariables)
            .eventMethods(eventMethods)
            .triggerMethods(triggerMethods)
            .propDefaults(propDefaults)
            .eventDeclarations(eventDeclarations)
            .extraBuilderMethods(builderMethodModels)
            .classJavadoc(classJavadoc)
            .propJavadocs(propJavadocs)
            .isPublic(isPublic)
            .dependencyInjectionGenerator(dependencyInjectionHelper)
            .representedObject(representedObject)
            .build();
    mServiceParam = SectionSpecModelUtils.createServiceParam(mSpecModel);
  }

  @Override
  public String getSpecName() {
    return mSpecModel.getSpecName();
  }

  @Override
  public TypeName getSpecTypeName() {
    return mSpecModel.getSpecTypeName();
  }

  @Override
  public String getComponentName() {
    return mSpecModel.getComponentName();
  }

  @Override
  public TypeName getComponentTypeName() {
    return mSpecModel.getComponentTypeName();
  }

  @Override
  public ImmutableList<DelegateMethodModel> getDelegateMethods() {
    return mSpecModel.getDelegateMethods();
  }

  @Override
  public ImmutableList<EventMethodModel> getEventMethods() {
    return mSpecModel.getEventMethods();
  }

  @Override
  public ImmutableList<EventMethodModel> getTriggerMethods() {
    return mSpecModel.getTriggerMethods();
  }

  @Override
  public ImmutableList<UpdateStateMethodModel> getUpdateStateMethods() {
    return mSpecModel.getUpdateStateMethods();
  }

  @Override
  public ImmutableList<PropModel> getProps() {
    return mSpecModel.getProps();
  }

  @Override
  public ImmutableList<PropDefaultModel> getPropDefaults() {
    return mSpecModel.getPropDefaults();
  }

  @Override
  public ImmutableList<TypeVariableName> getTypeVariables() {
    return mSpecModel.getTypeVariables();
  }

  @Override
  public ImmutableList<StateParamModel> getStateValues() {
    return mSpecModel.getStateValues();
  }

  @Override
  public ImmutableList<InterStageInputParamModel> getInterStageInputs() {
    return mSpecModel.getInterStageInputs();
  }

  @Override
  public ImmutableList<TreePropModel> getTreeProps() {
    return mSpecModel.getTreeProps();
  }

  @Override
  public ImmutableList<EventDeclarationModel> getEventDeclarations() {
    return mSpecModel.getEventDeclarations();
  }

  @Override
  public ImmutableList<BuilderMethodModel> getExtraBuilderMethods() {
    return mSpecModel.getExtraBuilderMethods();
  }

  @Override
  public ImmutableList<RenderDataDiffModel> getRenderDataDiffs() {
    return mSpecModel.getRenderDataDiffs();
  }

  @Override
  public String getClassJavadoc() {
    return mSpecModel.getClassJavadoc();
  }

  @Override
  public ImmutableList<PropJavadocModel> getPropJavadocs() {
    return mSpecModel.getPropJavadocs();
  }

  @Override
  public boolean isPublic() {
    return mSpecModel.isPublic();
  }

  @Override
  public ClassName getContextClass() {
    return SectionClassNames.SECTION_CONTEXT;
  }

  @Override
  public ClassName getComponentClass() {
    return mSpecModel.getComponentClass();
  }

  @Override
  public ClassName getStateContainerClass() {
    return SectionClassNames.STATE_CONTAINER_SECTION;
  }

  @Override
  public TypeName getUpdateStateInterface() {
    return SectionClassNames.SECTION_STATE_UPDATE;
  }

  @Override
  public String getScopeMethodName() {
    return "getSectionScope";
  }

  @Override
  public boolean isStylingSupported() {
    return false;
  }

  @Override
  public boolean hasInjectedDependencies() {
    return mSpecModel.hasInjectedDependencies();
  }

  @Override
  public boolean shouldCheckIdInIsEquivalentToMethod() {
    return false;
  }

  @Override
  public boolean hasDeepCopy() {
    return true;
  }

  @Override
  public boolean shouldGenerateHasState() {
    return false;
  }

  @Override
  public DependencyInjectionHelper getDependencyInjectionHelper() {
    return mSpecModel.getDependencyInjectionHelper();
  }

  @Override
  public Object getRepresentedObject() {
    return mSpecModel.getRepresentedObject();
  }

  @Override
  public List<SpecModelValidationError> validate() {
    return SpecModelValidation.validateGroupSectionSpecModel(this);
  }

  @Override
  public TypeSpec generate() {
    return null;
  }

  @Override
  public MethodParamModel getServiceParam() {
    return mServiceParam;
  }
}
