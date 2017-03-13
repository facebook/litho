// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import com.facebook.common.internal.ImmutableList;
import com.facebook.components.annotations.OnCreateLayoutWithSizeSpec;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

/**
 * Model that is an abstract representation of a
 * {@link com.facebook.components.annotations.LayoutSpec}.
 */
public class LayoutSpecModel implements SpecModel, HasPureRender {
  private final SpecModelImpl mSpecModel;
  private final boolean mCanMeasure;
  private final boolean mIsPureRender;

  public LayoutSpecModel(
      String qualifiedSpecClassName,
      ImmutableList<DelegateMethodModel> delegateMethods,
      ImmutableList<EventMethodModel> eventMethods,
      ImmutableList<UpdateStateMethodModel> updateStateMethods,
      ImmutableList<TypeVariableName> typeVariables,
      ImmutableList<PropDefaultModel> propDefaults,
      ImmutableList<EventDeclarationModel> eventDeclarations,
      String classJavadoc,
      ImmutableList<PropJavadocModel> propJavadocs,
      DependencyInjectionHelper dependencyInjectionHelper,
      boolean isPureRender,
      Object representedObject) {
    mSpecModel =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName(qualifiedSpecClassName)
            .delegateMethods(delegateMethods)
            .eventMethods(eventMethods)
            .updateStateMethods(updateStateMethods)
            .typeVariables(typeVariables)
            .propDefaults(propDefaults)
            .eventDeclarations(eventDeclarations)
            .classJavadoc(classJavadoc)
            .propJavadocs(propJavadocs)
            .dependencyInjectionGenerator(dependencyInjectionHelper)
            .representedObject(representedObject)
            .build();
    mCanMeasure = canMeasure(mSpecModel);
    mIsPureRender = isPureRender;
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
  public String getClassJavadoc() {
    return mSpecModel.getClassJavadoc();
  }

  @Override
  public ImmutableList<PropJavadocModel> getPropJavadocs() {
    return mSpecModel.getPropJavadocs();
  }

  @Override
  public ClassName getContextClass() {
    return ClassNames.COMPONENT_CONTEXT;
  }

  @Override
  public ClassName getComponentClass() {
    return ClassNames.COMPONENT;
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
  public boolean isStylingSupported() {
    return true;
  }

  @Override
  public boolean canMeasure() {
    return mCanMeasure;
  }

  @Override
  public boolean hasInjectedDependencies() {
    return mSpecModel.hasInjectedDependencies();
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
  public boolean isPureRender() {
    return mIsPureRender;
  }

  private static boolean canMeasure(SpecModel specModel) {
    return SpecModelUtils.getMethodModelWithAnnotation(
        specModel,
        OnCreateLayoutWithSizeSpec.class) != null;
  }
}
