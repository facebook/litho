/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Model that is an abstract representation of a {@link com.facebook.litho.annotations.TestSpec}.
 */
public class TestSpecModel implements SpecModel, HasPureRender {
  private final SpecModelImpl mSpecModel;
  private final TestSpecGenerator mTestSpecGenerator;

  public TestSpecModel(
      String qualifiedSpecClassName,
      String componentClassName,
      ImmutableList<DelegateMethodModel> delegateMethods,
      ImmutableList<EventDeclarationModel> eventDeclarations,
      ImmutableList<BuilderMethodModel> builderMethodModels,
      String classJavadoc,
      ImmutableList<PropJavadocModel> propJavadocs,
      Object representedObject,
      TestSpecGenerator testSpecGenerator) {
    mSpecModel =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName(qualifiedSpecClassName)
            .componentClassName(componentClassName)
            .componentClass(ClassNames.COMPONENT)
            .delegateMethods(delegateMethods)
            .eventDeclarations(eventDeclarations)
            .extraBuilderMethods(builderMethodModels)
            .classJavadoc(classJavadoc)
            .propJavadocs(propJavadocs)
            .representedObject(representedObject)
            .build();
    mTestSpecGenerator = testSpecGenerator;
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
    return ImmutableList.of();
  }

  @Override
  public ImmutableList<TypeVariableName> getTypeVariables() {
    return ImmutableList.of();
  }

  @Override
  public ImmutableList<StateParamModel> getStateValues() {
    return ImmutableList.of();
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
    return true;
  }

  @Override
  public ClassName getContextClass() {
    return ClassNames.COMPONENT_CONTEXT;
  }

  @Override
  public ClassName getComponentClass() {
    return mSpecModel.getComponentClass();
  }

  @Override
  public ClassName getStateContainerClass() {
    return ClassNames.STATE_CONTAINER_COMPONENT;
  }

  @Override
  public TypeName getUpdateStateInterface() {
    return ClassNames.COMPONENT_STATE_UPDATE;
  }

  @Override
  public String getScopeMethodName() {
    return "getComponentScope";
  }

  @Override
  public boolean isStylingSupported() {
    return false;
  }

  @Override
  public boolean hasInjectedDependencies() {
    return false;
  }

  @Override
  public boolean shouldCheckIdInIsEquivalentToMethod() {
    return mSpecModel.shouldCheckIdInIsEquivalentToMethod();
  }

  @Override
  public boolean hasDeepCopy() {
    return mSpecModel.hasDeepCopy();
  }

  @Override
  @Nullable
  public DependencyInjectionHelper getDependencyInjectionHelper() {
    return null;
  }

  @Override
  public Object getRepresentedObject() {
    return mSpecModel.getRepresentedObject();
  }

  @Override
  public List<SpecModelValidationError> validate() {
    // TODO(T20862132): Add model validation for test specs
    return Collections.emptyList();
  }

  @Override
  public TypeSpec generate() {
    return mTestSpecGenerator.generate(this);
  }

  @Override
  public boolean isPureRender() {
    return true;
  }
}
