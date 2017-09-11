/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.specmodels;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.BuilderMethodModel;
import com.facebook.litho.specmodels.model.DelegateMethodModel;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethodModel;
import com.facebook.litho.specmodels.model.InterStageInputParamModel;
import com.facebook.litho.specmodels.model.PropDefaultModel;
import com.facebook.litho.specmodels.model.PropJavadocModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.RenderDataDiffModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.facebook.litho.specmodels.model.StateParamModel;
import com.facebook.litho.specmodels.model.TreePropModel;
import com.facebook.litho.specmodels.model.UpdateStateMethodModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/** An implementation of SpecModel + Builder for testing purposes only. */
@Immutable
public class MockSpecModel implements SpecModel {
  private final String mSpecName;
  private final TypeName mSpecTypeName;
  private final String mComponentName;
  private final TypeName mComponentTypeName;
  private final ClassName mComponentClass;
  private final ImmutableList<DelegateMethodModel> mDelegateMethods;
  private final ImmutableList<EventMethodModel> mEventMethods;
  private final ImmutableList<EventMethodModel> mTriggerMethods;
  private final ImmutableList<UpdateStateMethodModel> mUpdateStateMethods;
  private final ImmutableList<PropModel> mProps;
  private final ImmutableList<PropDefaultModel> mPropDefaults;
  private final ImmutableList<TypeVariableName> mTypeVariables;
  private final ImmutableList<StateParamModel> mStateValues;
  private final ImmutableList<InterStageInputParamModel> mInterStageInputs;
  private final ImmutableList<TreePropModel> mTreeProps;
  private final ImmutableList<EventDeclarationModel> mEventDeclarations;
  private final ImmutableList<BuilderMethodModel> mImplicitBuilderMethods;
  private final ImmutableList<RenderDataDiffModel> mDiffs;
  private final String mClassJavadoc;
  private final ImmutableList<PropJavadocModel> mPropJavadocs;
  private final boolean mIsPublic;
  private final boolean mHasInjectedDependencies;
  @Nullable private final DependencyInjectionHelper mDependencyInjectionHelper;
  private final Object mRepresentedObject;
  private final TypeSpec mGeneratedTypeSpec;
  private final ClassName mContextClass;
  private final ClassName mStateContainerClass;
  private final boolean mHasDeepCopy;
  private final boolean mShouldCheckIdInIsEquivalentToMethod;
  private final TypeName mUpdateStateInterface;
  private final String mScopeMethodName;
  private final boolean mIsStylingSupported;
  private final List<SpecModelValidationError> mSpecModelValidationErrors;

  private MockSpecModel(
      String specName,
      TypeName specTypeName,
      String componentName,
      TypeName componentTypeName,
      ClassName componentClass,
      ImmutableList<DelegateMethodModel> delegateMethods,
      ImmutableList<EventMethodModel> eventMethods,
      ImmutableList<EventMethodModel> triggerMethods,
      ImmutableList<UpdateStateMethodModel> updateStateMethods,
      ImmutableList<PropModel> props,
      ImmutableList<PropDefaultModel> propDefaults,
      ImmutableList<TypeVariableName> typeVariables,
      ImmutableList<StateParamModel> stateValues,
      ImmutableList<InterStageInputParamModel> interStageInputs,
      ImmutableList<TreePropModel> treeProps,
      ImmutableList<EventDeclarationModel> eventDeclarations,
      ImmutableList<BuilderMethodModel> implicitBuilderMethods,
      ImmutableList<RenderDataDiffModel> diffs,
      String classJavadoc,
      ImmutableList<PropJavadocModel> propJavadocs,
      boolean isPublic,
      boolean hasInjectedDependencies,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper,
      Object representedObject,
      TypeSpec generatedTypeSpec,
      ClassName contextClass,
      ClassName stateContainerClass,
      boolean hasDeepCopy,
      boolean shouldCheckIdInIsEquivalentToMethod,
      TypeName updateStateInterface,
      String scopeMethodName,
      boolean isStylingSupported,
      List<SpecModelValidationError> specModelValidationErrors) {
    mSpecName = specName;
    mSpecTypeName = specTypeName;
    mComponentName = componentName;
    mComponentTypeName = componentTypeName;
    mComponentClass = componentClass;
    mDelegateMethods = delegateMethods;
    mEventMethods = eventMethods;
    mTriggerMethods = triggerMethods;
    mUpdateStateMethods = updateStateMethods;
    mProps = props;
    mPropDefaults = propDefaults;
    mTypeVariables = typeVariables;
    mStateValues = stateValues;
    mInterStageInputs = interStageInputs;
    mTreeProps = treeProps;
    mEventDeclarations = eventDeclarations;
    mImplicitBuilderMethods = implicitBuilderMethods;
    mDiffs = diffs;
    mClassJavadoc = classJavadoc;
    mPropJavadocs = propJavadocs;
    mIsPublic = isPublic;
    mHasInjectedDependencies = hasInjectedDependencies;
    mDependencyInjectionHelper = dependencyInjectionHelper;
    mRepresentedObject = representedObject;
    mGeneratedTypeSpec = generatedTypeSpec;
    mContextClass = contextClass;
    mStateContainerClass = stateContainerClass;
    mHasDeepCopy = hasDeepCopy;
    mShouldCheckIdInIsEquivalentToMethod = shouldCheckIdInIsEquivalentToMethod;
    mUpdateStateInterface = updateStateInterface;
    mScopeMethodName = scopeMethodName;
    mIsStylingSupported = isStylingSupported;
    mSpecModelValidationErrors = specModelValidationErrors;
  }

  @Override
  public String getSpecName() {
    return mSpecName;
  }

  @Override
  public TypeName getSpecTypeName() {
    return mSpecTypeName;
  }

  @Override
  public String getComponentName() {
    return mComponentName;
  }

  @Override
  public TypeName getComponentTypeName() {
    return mComponentTypeName;
  }

  @Override
  public ImmutableList<DelegateMethodModel> getDelegateMethods() {
    return mDelegateMethods;
  }

  @Override
  public ImmutableList<EventMethodModel> getEventMethods() {
    return mEventMethods;
  }

  @Override
  public ImmutableList<EventMethodModel> getTriggerMethods() {
    return mTriggerMethods;
  }

  @Override
  public ImmutableList<UpdateStateMethodModel> getUpdateStateMethods() {
    return mUpdateStateMethods;
  }

  @Override
  public ImmutableList<PropModel> getProps() {
    return mProps;
  }

  @Override
  public ImmutableList<PropDefaultModel> getPropDefaults() {
    return mPropDefaults;
  }

  @Override
  public ImmutableList<TypeVariableName> getTypeVariables() {
    return mTypeVariables;
  }

  @Override
  public ImmutableList<StateParamModel> getStateValues() {
    return mStateValues;
  }

  @Override
  public ImmutableList<InterStageInputParamModel> getInterStageInputs() {
    return mInterStageInputs;
  }

  @Override
  public ImmutableList<TreePropModel> getTreeProps() {
    return mTreeProps;
  }

  @Override
  public ImmutableList<EventDeclarationModel> getEventDeclarations() {
    return mEventDeclarations;
  }

  @Override
  public ImmutableList<BuilderMethodModel> getExtraBuilderMethods() {
    return mImplicitBuilderMethods;
  }

  @Override
  public ImmutableList<RenderDataDiffModel> getRenderDataDiffs() {
    return mDiffs;
  }

  @Override
  public String getClassJavadoc() {
    return mClassJavadoc;
  }

  @Override
  public ImmutableList<PropJavadocModel> getPropJavadocs() {
    return mPropJavadocs;
  }

  @Override
  public boolean isPublic() {
    return mIsPublic;
  }

  @Override
  public ClassName getContextClass() {
    return mContextClass;
  }

  @Override
  public ClassName getComponentClass() {
    return mComponentClass;
  }

  @Override
  public ClassName getStateContainerClass() {
    return mStateContainerClass;
  }

  @Override
  public TypeName getUpdateStateInterface() {
    return mUpdateStateInterface;
  }

  @Override
  public String getScopeMethodName() {
    return mScopeMethodName;
  }

  @Override
  public boolean isStylingSupported() {
    return mIsStylingSupported;
  }

  @Override
  public boolean hasInjectedDependencies() {
    return mHasInjectedDependencies;
  }

  @Override
  public boolean shouldCheckIdInIsEquivalentToMethod() {
    return mShouldCheckIdInIsEquivalentToMethod;
  }

  @Override
  public boolean hasDeepCopy() {
    return mHasDeepCopy;
  }

  @Nullable
  @Override
  public DependencyInjectionHelper getDependencyInjectionHelper() {
    return mDependencyInjectionHelper;
  }

  @Override
  public Object getRepresentedObject() {
    return mRepresentedObject;
  }

  @Override
  public List<SpecModelValidationError> validate() {
    return mSpecModelValidationErrors;
  }

  @Override
  public TypeSpec generate() {
    return mGeneratedTypeSpec;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String mSpecName;
    private TypeName mSpecTypeName;
    private String mComponentName;
    private TypeName mComponentTypeName;
    private ClassName mComponentClass;
    private ImmutableList<DelegateMethodModel> mDelegateMethods = ImmutableList.of();
    private ImmutableList<EventMethodModel> mEventMethods = ImmutableList.of();
    private ImmutableList<EventMethodModel> mTriggerMethods = ImmutableList.of();
    private ImmutableList<UpdateStateMethodModel> mUpdateStateMethods = ImmutableList.of();
    private ImmutableList<PropModel> mProps = ImmutableList.of();
    private ImmutableList<PropDefaultModel> mPropDefaults = ImmutableList.of();
    private ImmutableList<TypeVariableName> mTypeVariables = ImmutableList.of();
    private ImmutableList<StateParamModel> mStateValues = ImmutableList.of();
    private ImmutableList<InterStageInputParamModel> mInterStageInputs = ImmutableList.of();
    private ImmutableList<TreePropModel> mTreeProps = ImmutableList.of();
    private ImmutableList<EventDeclarationModel> mEventDeclarations = ImmutableList.of();
    private ImmutableList<BuilderMethodModel> mImplicitBuilderMethods = ImmutableList.of();
    private ImmutableList<RenderDataDiffModel> mDiffs = ImmutableList.of();
    private String mClassJavadoc;
    private ImmutableList<PropJavadocModel> mPropJavadocs = ImmutableList.of();
    private boolean mIsPublic;
    private boolean mHasInjectedDependencies;
    private DependencyInjectionHelper mDependencyInjectionHelper;
    private Object mRepresentedObject;
    private TypeSpec mGeneratedTypeSpec;
    private ClassName mContextClass;
    private ClassName mStateContainerClass;
    private boolean mHasDeepCopy;
    private boolean mShouldCheckIdInIsEquivalentToMethod;
    private TypeName mUpdateStateInterface;
    private String mScopeMethodName;
    private boolean mIsStylingSupported;
    private List<SpecModelValidationError> mSpecModelValidationErrors = ImmutableList.of();

    public Builder specName(String specName) {
      mSpecName = specName;
      return this;
    }

    public Builder specTypeName(TypeName specTypeName) {
      mSpecTypeName = specTypeName;
      return this;
    }

    public Builder componentName(String componentName) {
      mComponentName = componentName;
      return this;
    }

    public Builder componentTypeName(TypeName componentTypeName) {
      mComponentTypeName = componentTypeName;
      return this;
    }

    public Builder componentClass(ClassName componentClass) {
      mComponentClass = componentClass;
      return this;
    }

    public Builder delegateMethods(ImmutableList<DelegateMethodModel> delegateMethods) {
      mDelegateMethods = delegateMethods;
      return this;
    }

    public Builder eventMethods(ImmutableList<EventMethodModel> eventMethods) {
      mEventMethods = eventMethods;
      return this;
    }

    public Builder triggerMethods(ImmutableList<EventMethodModel> triggerMethods) {
      mTriggerMethods = triggerMethods;
      return this;
    }

    public Builder updateStateMethods(ImmutableList<UpdateStateMethodModel> updateStateMethods) {
      mUpdateStateMethods = updateStateMethods;
      return this;
    }

    public Builder props(ImmutableList<PropModel> props) {
      mProps = props;
      return this;
    }

    public Builder propDefaults(ImmutableList<PropDefaultModel> propDefaults) {
      mPropDefaults = propDefaults;
      return this;
    }

    public Builder typeVariables(ImmutableList<TypeVariableName> typeVariables) {
      mTypeVariables = typeVariables;
      return this;
    }

    public Builder stateValues(ImmutableList<StateParamModel> stateValues) {
      mStateValues = stateValues;
      return this;
    }

    public Builder interStageInputs(ImmutableList<InterStageInputParamModel> interStageInputs) {
      mInterStageInputs = interStageInputs;
      return this;
    }

    public Builder treeProps(ImmutableList<TreePropModel> treeProps) {
      mTreeProps = treeProps;
      return this;
    }

    public Builder eventDeclarations(ImmutableList<EventDeclarationModel> eventDeclarations) {
      mEventDeclarations = eventDeclarations;
      return this;
    }

    public Builder implicitBuilderMethods(
        ImmutableList<BuilderMethodModel> implicitBuilderMethods) {
      mImplicitBuilderMethods = implicitBuilderMethods;
      return this;
    }

    public Builder diffs(ImmutableList<RenderDataDiffModel> diffs) {
      mDiffs = diffs;
      return this;
    }

    public Builder classJavadoc(String classJavadoc) {
      mClassJavadoc = classJavadoc;
      return this;
    }

    public Builder propJavadocs(ImmutableList<PropJavadocModel> propJavadocs) {
      mPropJavadocs = propJavadocs;
      return this;
    }

    public Builder isPublic(boolean isPublic) {
      mIsPublic = isPublic;
      return this;
    }

    public Builder hasInjectedDependencies(boolean hasInjectedDependencies) {
      mHasInjectedDependencies = hasInjectedDependencies;
      return this;
    }

    public Builder dependencyInjectionHelper(DependencyInjectionHelper dependencyInjectionHelper) {
      mDependencyInjectionHelper = dependencyInjectionHelper;
      return this;
    }

    public Builder representedObject(Object representedObject) {
      mRepresentedObject = representedObject;
      return this;
    }

    public Builder generatedTypeSpec(TypeSpec generatedTypeSpec) {
      mGeneratedTypeSpec = generatedTypeSpec;
      return this;
    }

    public Builder contextClass(ClassName contextClass) {
      mContextClass = contextClass;
      return this;
    }

    public Builder stateContainerClass(ClassName stateContainerClass) {
      mStateContainerClass = stateContainerClass;
      return this;
    }

    public Builder hasDeepCopy(boolean hasDeepCopy) {
      mHasDeepCopy = hasDeepCopy;
      return this;
    }

    public Builder shouldCheckIdInIsEquivalentToMethod(
        boolean shouldCheckIdInIsEquivalentToMethod) {
      mShouldCheckIdInIsEquivalentToMethod = shouldCheckIdInIsEquivalentToMethod;
      return this;
    }

    public Builder updateStateInterface(TypeName updateStateInterface) {
      mUpdateStateInterface = updateStateInterface;
      return this;
    }

    public Builder scopeMethodName(String scopeMethodName) {
      mScopeMethodName = scopeMethodName;
      return this;
    }

    public Builder isStylingSupported(boolean isStylingSupported) {
      mIsStylingSupported = isStylingSupported;
      return this;
    }

    public Builder specModelValidationErrors(
        List<SpecModelValidationError> specModelValidationErrors) {
      mSpecModelValidationErrors = specModelValidationErrors;
      return this;
    }

    public MockSpecModel build() {
      return new MockSpecModel(
          mSpecName,
          mSpecTypeName,
          mComponentName,
          mComponentTypeName,
          mComponentClass,
          mDelegateMethods,
          mEventMethods,
          mTriggerMethods,
          mUpdateStateMethods,
          mProps,
          mPropDefaults,
          mTypeVariables,
          mStateValues,
          mInterStageInputs,
          mTreeProps,
          mEventDeclarations,
          mImplicitBuilderMethods,
          mDiffs,
          mClassJavadoc,
          mPropJavadocs,
          mIsPublic,
          mHasInjectedDependencies,
          mDependencyInjectionHelper,
          mRepresentedObject,
          mGeneratedTypeSpec,
          mContextClass,
          mStateContainerClass,
          mHasDeepCopy,
          mShouldCheckIdInIsEquivalentToMethod,
          mUpdateStateInterface,
          mScopeMethodName,
          mIsStylingSupported,
          mSpecModelValidationErrors);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final Builder builder = (Builder) o;
      return mIsPublic == builder.mIsPublic
          && mHasInjectedDependencies == builder.mHasInjectedDependencies
          && mHasDeepCopy == builder.mHasDeepCopy
          && mShouldCheckIdInIsEquivalentToMethod == builder.mShouldCheckIdInIsEquivalentToMethod
          && mIsStylingSupported == builder.mIsStylingSupported
          && Objects.equals(mSpecName, builder.mSpecName)
          && Objects.equals(mSpecTypeName, builder.mSpecTypeName)
          && Objects.equals(mComponentName, builder.mComponentName)
          && Objects.equals(mComponentTypeName, builder.mComponentTypeName)
          && Objects.equals(mComponentClass, builder.mComponentClass)
          && Objects.equals(mDelegateMethods, builder.mDelegateMethods)
          && Objects.equals(mEventMethods, builder.mEventMethods)
          && Objects.equals(mTriggerMethods, builder.mTriggerMethods)
          && Objects.equals(mUpdateStateMethods, builder.mUpdateStateMethods)
          && Objects.equals(mProps, builder.mProps)
          && Objects.equals(mPropDefaults, builder.mPropDefaults)
          && Objects.equals(mTypeVariables, builder.mTypeVariables)
          && Objects.equals(mStateValues, builder.mStateValues)
          && Objects.equals(mInterStageInputs, builder.mInterStageInputs)
          && Objects.equals(mTreeProps, builder.mTreeProps)
          && Objects.equals(mEventDeclarations, builder.mEventDeclarations)
          && Objects.equals(mImplicitBuilderMethods, builder.mImplicitBuilderMethods)
          && Objects.equals(mDiffs, builder.mDiffs)
          && Objects.equals(mClassJavadoc, builder.mClassJavadoc)
          && Objects.equals(mPropJavadocs, builder.mPropJavadocs)
          && Objects.equals(mDependencyInjectionHelper, builder.mDependencyInjectionHelper)
          && Objects.equals(mRepresentedObject, builder.mRepresentedObject)
          && Objects.equals(mGeneratedTypeSpec, builder.mGeneratedTypeSpec)
          && Objects.equals(mContextClass, builder.mContextClass)
          && Objects.equals(mStateContainerClass, builder.mStateContainerClass)
          && Objects.equals(mUpdateStateInterface, builder.mUpdateStateInterface)
          && Objects.equals(mScopeMethodName, builder.mScopeMethodName)
          && Objects.equals(mSpecModelValidationErrors, builder.mSpecModelValidationErrors);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          mSpecName,
          mSpecTypeName,
          mComponentName,
          mComponentTypeName,
          mComponentClass,
          mDelegateMethods,
          mEventMethods,
          mTriggerMethods,
          mUpdateStateMethods,
          mProps,
          mPropDefaults,
          mTypeVariables,
          mStateValues,
          mInterStageInputs,
          mTreeProps,
          mEventDeclarations,
          mImplicitBuilderMethods,
          mDiffs,
          mClassJavadoc,
          mPropJavadocs,
          mIsPublic,
          mHasInjectedDependencies,
          mDependencyInjectionHelper,
          mRepresentedObject,
          mGeneratedTypeSpec,
          mContextClass,
          mStateContainerClass,
          mHasDeepCopy,
          mShouldCheckIdInIsEquivalentToMethod,
          mUpdateStateInterface,
          mScopeMethodName,
          mIsStylingSupported,
          mSpecModelValidationErrors);
    }
  }
}
