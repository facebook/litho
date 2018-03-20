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
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.BuilderMethodModel;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.HasEnclosedSpecModel;
import com.facebook.litho.specmodels.model.HasPureRender;
import com.facebook.litho.specmodels.model.InjectPropModel;
import com.facebook.litho.specmodels.model.InterStageInputParamModel;
import com.facebook.litho.specmodels.model.PropDefaultModel;
import com.facebook.litho.specmodels.model.PropJavadocModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.RenderDataDiffModel;
import com.facebook.litho.specmodels.model.SpecElementType;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.facebook.litho.specmodels.model.StateParamModel;
import com.facebook.litho.specmodels.model.TagModel;
import com.facebook.litho.specmodels.model.TreePropModel;
import com.facebook.litho.specmodels.model.UpdateStateMethod;
import com.squareup.javapoet.AnnotationSpec;
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
public class MockSpecModel implements SpecModel, HasPureRender, HasEnclosedSpecModel {
  private final String mSpecName;
  private final TypeName mSpecTypeName;
  private final String mComponentName;
  private final TypeName mComponentTypeName;
  private final ClassName mComponentClass;
  private final ImmutableList<SpecMethodModel<DelegateMethod, Void>> mDelegateMethods;
  private final ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> mEventMethods;
  private final ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> mTriggerMethods;
  private final ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> mUpdateStateMethods;
  private final ImmutableList<PropModel> mProps;
  private final ImmutableList<PropModel> mRawProps;
  private final ImmutableList<InjectPropModel> mInjectProps;
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
  private final ImmutableList<AnnotationSpec> mClassAnnotations;
  private final ImmutableList<TagModel> mTags;
  private final SpecElementType mSpecElementType;
  private final boolean mIsPureRender;
  private final SpecModel mEnclosedSpecModel;

  private MockSpecModel(
      String specName,
      TypeName specTypeName,
      String componentName,
      TypeName componentTypeName,
      ClassName componentClass,
      ImmutableList<SpecMethodModel<DelegateMethod, Void>> delegateMethods,
      ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> eventMethods,
      ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> triggerMethods,
      ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> updateStateMethods,
      ImmutableList<PropModel> rawProps,
      ImmutableList<PropModel> props,
      ImmutableList<InjectPropModel> injectProps,
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
      List<SpecModelValidationError> specModelValidationErrors,
      ImmutableList<AnnotationSpec> classAnnotations,
      ImmutableList<TagModel> tags,
      SpecElementType specElementType,
      boolean isPureRender,
      SpecModel enclosedSpecModel) {
    mSpecName = specName;
    mSpecTypeName = specTypeName;
    mComponentName = componentName;
    mComponentTypeName = componentTypeName;
    mComponentClass = componentClass;
    mDelegateMethods = delegateMethods;
    mEventMethods = eventMethods;
    mTriggerMethods = triggerMethods;
    mUpdateStateMethods = updateStateMethods;
    mRawProps = rawProps;
    mProps = props;
    mInjectProps = injectProps;
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
    mClassAnnotations = classAnnotations;
    mTags = tags;
    mSpecElementType = specElementType;
    mIsPureRender = isPureRender;
    mEnclosedSpecModel = enclosedSpecModel;
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
  public ImmutableList<SpecMethodModel<DelegateMethod, Void>> getDelegateMethods() {
    return mDelegateMethods;
  }

  @Override
  public ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> getEventMethods() {
    return mEventMethods;
  }

  @Override
  public ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> getTriggerMethods() {
    return mTriggerMethods;
  }

  @Override
  public ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> getUpdateStateMethods() {
    return mUpdateStateMethods;
  }

  @Override
  public ImmutableList<PropModel> getRawProps() {
    return mRawProps;
  }

  @Override
  public ImmutableList<PropModel> getProps() {
    return mProps;
  }

  @Override
  public ImmutableList<InjectPropModel> getInjectProps() {
    return mInjectProps;
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
  public ImmutableList<AnnotationSpec> getClassAnnotations() {
    return mClassAnnotations;
  }

  @Override
  public ImmutableList<TagModel> getTags() {
    return mTags;
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

  @Override
  public boolean shouldGenerateHasState() {
    return true;
  }

  @Nullable
  @Override
  public DependencyInjectionHelper getDependencyInjectionHelper() {
    return mDependencyInjectionHelper;
  }

  @Override
  public SpecElementType getSpecElementType() {
    return mSpecElementType;
  }

  @Override
  public Object getRepresentedObject() {
    return mRepresentedObject;
  }

  @Override
  public List<SpecModelValidationError> validate(RunMode runMode) {
    return mSpecModelValidationErrors;
  }

  @Override
  public TypeSpec generate() {
    return mGeneratedTypeSpec;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public SpecModel getEnclosedSpecModel() {
    return mEnclosedSpecModel;
  }

  @Override
  public boolean isPureRender() {
    return mIsPureRender;
  }

  public static class Builder {
    private String mSpecName;
    private TypeName mSpecTypeName;
    private String mComponentName;
    private TypeName mComponentTypeName;
    private ClassName mComponentClass;
    private ImmutableList<SpecMethodModel<DelegateMethod, Void>> mDelegateMethods =
        ImmutableList.of();
    private ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> mEventMethods =
        ImmutableList.of();
    private ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> mTriggerMethods =
        ImmutableList.of();
    private ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> mUpdateStateMethods =
        ImmutableList.of();
    private ImmutableList<PropModel> mRawProps = ImmutableList.of();
    private ImmutableList<PropModel> mProps = ImmutableList.of();
    private ImmutableList<InjectPropModel> mInjectProps = ImmutableList.of();
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
    private ImmutableList<AnnotationSpec> mClassAnnotations;
    private ImmutableList<TagModel> mTags = ImmutableList.of();
    private SpecElementType mSpecElementType;
    private boolean mIsPureRender;
    private SpecModel mEnclosedSpecModel;

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

    public Builder delegateMethods(
        ImmutableList<SpecMethodModel<DelegateMethod, Void>> delegateMethods) {
      mDelegateMethods = delegateMethods;
      return this;
    }

    public Builder eventMethods(
        ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> eventMethods) {
      mEventMethods = eventMethods;
      return this;
    }

    public Builder triggerMethods(
        ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> triggerMethods) {
      mTriggerMethods = triggerMethods;
      return this;
    }

    public Builder updateStateMethods(
        ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> updateStateMethods) {
      mUpdateStateMethods = updateStateMethods;
      return this;
    }

    public Builder rawProps(ImmutableList<PropModel> rawProps) {
      mRawProps = rawProps;
      return this;
    }

    public Builder props(ImmutableList<PropModel> props) {
      mProps = props;
      return this;
    }

    public Builder injectProps(ImmutableList<InjectPropModel> injectProps) {
      mInjectProps = injectProps;
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

    public Builder classAnnotations(ImmutableList<AnnotationSpec> classAnnotations) {
      mClassAnnotations = classAnnotations;
      return this;
    }

    public Builder tags(ImmutableList<TagModel> tags) {
      mTags = tags;
      return this;
    }

    public Builder specElementType(SpecElementType specElementType) {
      mSpecElementType = specElementType;
      return this;
    }

    public Builder isPureRender(boolean isPureRender) {
      mIsPureRender = isPureRender;
      return this;
    }

    public Builder enclosedSpecModel(SpecModel enclosedSpecModel) {
      mEnclosedSpecModel = enclosedSpecModel;
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
          mRawProps,
          mProps,
          mInjectProps,
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
          mSpecModelValidationErrors,
          mClassAnnotations,
          mTags,
          mSpecElementType,
          mIsPureRender,
          mEnclosedSpecModel);
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
          && Objects.equals(mRawProps, builder.mRawProps)
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
          && Objects.equals(mSpecModelValidationErrors, builder.mSpecModelValidationErrors)
          && Objects.equals(mClassAnnotations, builder.mClassAnnotations)
          && Objects.equals(mTags, builder.mTags)
          && Objects.equals(mSpecElementType, builder.mSpecElementType)
          && Objects.equals(mIsPureRender, builder.mIsPureRender)
          && Objects.equals(mEnclosedSpecModel, builder.mEnclosedSpecModel);
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
          mSpecModelValidationErrors,
          mClassAnnotations,
          mTags,
          mSpecElementType,
          mIsPureRender,
          mEnclosedSpecModel);
    }
  }
}
