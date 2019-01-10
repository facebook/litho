/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.testing.specmodels;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.BuilderMethodModel;
import com.facebook.litho.specmodels.model.CachedValueParamModel;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.FieldModel;
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
import com.facebook.litho.specmodels.model.WorkingRangeMethodModel;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/** An implementation of SpecModel + Builder for testing purposes only. */
@Immutable
public class MockSpecModel implements SpecModel, HasPureRender, HasEnclosedSpecModel {
  private final String mSpecName;
  private final ClassName mSpecTypeName;
  private final String mComponentName;
  private final TypeName mComponentTypeName;
  private final ClassName mComponentClass;
  private final ImmutableList<SpecMethodModel<DelegateMethod, Void>> mDelegateMethods;
  private final ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> mEventMethods;
  private final ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> mTriggerMethods;
  @Nullable private final SpecMethodModel<EventMethod, Void> mWorkingRangeRegisterMethod;
  private final ImmutableList<WorkingRangeMethodModel> mWorkingRangeMethods;
  private final ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> mUpdateStateMethods;
  private final ImmutableList<SpecMethodModel<UpdateStateMethod, Void>>
      mUpdateStateWithTransitionMethods;
  private final ImmutableList<PropModel> mProps;
  private final ImmutableList<PropModel> mRawProps;
  private final ImmutableList<InjectPropModel> mRawInjectProps;
  private final ImmutableList<InjectPropModel> mInjectProps;
  private final ImmutableList<PropDefaultModel> mPropDefaults;
  private final ImmutableList<TypeVariableName> mTypeVariables;
  private final ImmutableList<StateParamModel> mStateValues;
  private final ImmutableList<CachedValueParamModel> mCachedValues;
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
  private final ClassName mTransitionClass;
  private final ClassName mTransitionContainerClass;
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
      ClassName specTypeName,
      String componentName,
      TypeName componentTypeName,
      ClassName componentClass,
      ImmutableList<SpecMethodModel<DelegateMethod, Void>> delegateMethods,
      ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> eventMethods,
      ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> triggerMethods,
      SpecMethodModel<EventMethod, Void> workingRangeRegisterMethod,
      ImmutableList<WorkingRangeMethodModel> workingRangeMethods,
      ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> updateStateMethods,
      ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> updateStateWithTransitionMethods,
      ImmutableList<PropModel> rawProps,
      ImmutableList<PropModel> props,
      ImmutableList<InjectPropModel> rawInjectProps,
      ImmutableList<InjectPropModel> injectProps,
      ImmutableList<PropDefaultModel> propDefaults,
      ImmutableList<TypeVariableName> typeVariables,
      ImmutableList<StateParamModel> stateValues,
      ImmutableList<CachedValueParamModel> cachedValues,
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
      ClassName transitionClass,
      ClassName transitionContainerClass,
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
    mWorkingRangeRegisterMethod = workingRangeRegisterMethod;
    mWorkingRangeMethods = workingRangeMethods;
    mUpdateStateMethods = updateStateMethods;
    mUpdateStateWithTransitionMethods = updateStateWithTransitionMethods;
    mRawProps = rawProps;
    mProps = props;
    mRawInjectProps = rawInjectProps;
    mInjectProps = injectProps;
    mPropDefaults = propDefaults;
    mTypeVariables = typeVariables;
    mStateValues = stateValues;
    mCachedValues = cachedValues;
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
    mTransitionClass = transitionClass;
    mTransitionContainerClass = transitionContainerClass;
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
  public ClassName getSpecTypeName() {
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
  public ImmutableList<FieldModel> getFields() {
    throw new RuntimeException("Mock was not provided with fields");
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
  @Nullable
  public SpecMethodModel<EventMethod, Void> getWorkingRangeRegisterMethod() {
    return mWorkingRangeRegisterMethod;
  }

  @Override
  public ImmutableList<WorkingRangeMethodModel> getWorkingRangeMethods() {
    return mWorkingRangeMethods;
  }

  @Override
  public ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> getUpdateStateMethods() {
    return mUpdateStateMethods;
  }

  @Override
  public ImmutableList<SpecMethodModel<UpdateStateMethod, Void>>
      getUpdateStateWithTransitionMethods() {
    return mUpdateStateWithTransitionMethods;
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
  public ImmutableList<InjectPropModel> getRawInjectProps() {
    return mRawInjectProps;
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
  public ImmutableList<CachedValueParamModel> getCachedValues() {
    return mCachedValues;
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
  public ClassName getTransitionClass() {
    return mTransitionClass;
  }

  @Override
  public ClassName getTransitionContainerClass() {
    return mTransitionContainerClass;
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

  @Override
  public boolean shouldGenerateCopyMethod() {
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
  public List<SpecModelValidationError> validate(EnumSet<RunMode> runMode) {
    return mSpecModelValidationErrors;
  }

  @Override
  public TypeSpec generate(EnumSet<RunMode> runMode) {
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

  @Override
  public boolean shouldGenerateIsEquivalentTo() {
    return false;
  }

  public static class Builder {
    private String mSpecName;
    private ClassName mSpecTypeName;
    private String mComponentName;
    private TypeName mComponentTypeName;
    private ClassName mComponentClass;
    private ImmutableList<SpecMethodModel<DelegateMethod, Void>> mDelegateMethods =
        ImmutableList.of();
    private ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> mEventMethods =
        ImmutableList.of();
    private ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> mTriggerMethods =
        ImmutableList.of();
    private SpecMethodModel<EventMethod, Void> mWorkingRangeRegisterMethod;
    private ImmutableList<WorkingRangeMethodModel> mWorkingRangeMethods = ImmutableList.of();
    private ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> mUpdateStateMethods =
        ImmutableList.of();
    private ImmutableList<PropModel> mRawProps = ImmutableList.of();
    private ImmutableList<SpecMethodModel<UpdateStateMethod, Void>>
        mUpdateStateWithTransitionMethods = ImmutableList.of();
    private ImmutableList<PropModel> mProps = ImmutableList.of();
    private ImmutableList<InjectPropModel> mRawInjectProps = ImmutableList.of();
    private ImmutableList<InjectPropModel> mInjectProps = ImmutableList.of();
    private ImmutableList<PropDefaultModel> mPropDefaults = ImmutableList.of();
    private ImmutableList<TypeVariableName> mTypeVariables = ImmutableList.of();
    private ImmutableList<StateParamModel> mStateValues = ImmutableList.of();
    private ImmutableList<CachedValueParamModel> mCachedValues = ImmutableList.of();
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
    private ClassName mTransitionClass;
    private ClassName mTransitionContainerClass;
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

    public Builder specTypeName(ClassName specTypeName) {
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

    public Builder workingRangeRegisterMethod(
        SpecMethodModel<EventMethod, Void> workingRangeRegisterMethod) {
      mWorkingRangeRegisterMethod = workingRangeRegisterMethod;
      return this;
    }

    public Builder workingRangeMethods(ImmutableList<WorkingRangeMethodModel> workingRangeMethods) {
      mWorkingRangeMethods = workingRangeMethods;
      return this;
    }

    public Builder updateStateMethods(
        ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> updateStateMethods) {
      mUpdateStateMethods = updateStateMethods;
      return this;
    }

    public Builder updateStateWithTransitionMethods(
        ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> updateStateWithTransitionMethods) {
      mUpdateStateWithTransitionMethods = updateStateWithTransitionMethods;
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

    public Builder rawInjectProps(ImmutableList<InjectPropModel> rawInjectProps) {
      mRawInjectProps = rawInjectProps;
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

    public Builder cachedValues(ImmutableList<CachedValueParamModel> cachedValues) {
      mCachedValues = cachedValues;
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

    public Builder transitionClass(ClassName transitionClass) {
      mTransitionClass = transitionClass;
      return this;
    }

    public Builder transitionContainerClass(ClassName transitionContainerClass) {
      mTransitionContainerClass = transitionContainerClass;
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
          mWorkingRangeRegisterMethod,
          mWorkingRangeMethods,
          mUpdateStateMethods,
          mUpdateStateWithTransitionMethods,
          mRawProps,
          mProps,
          mRawInjectProps,
          mInjectProps,
          mPropDefaults,
          mTypeVariables,
          mStateValues,
          mCachedValues,
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
          mTransitionClass,
          mTransitionContainerClass,
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
          && Objects.equals(mWorkingRangeRegisterMethod, builder.mWorkingRangeRegisterMethod)
          && Objects.equals(mWorkingRangeMethods, builder.mWorkingRangeMethods)
          && Objects.equals(mUpdateStateMethods, builder.mUpdateStateMethods)
          && Objects.equals(
              mUpdateStateWithTransitionMethods, builder.mUpdateStateWithTransitionMethods)
          && Objects.equals(mRawProps, builder.mRawProps)
          && Objects.equals(mProps, builder.mProps)
          && Objects.equals(mPropDefaults, builder.mPropDefaults)
          && Objects.equals(mTypeVariables, builder.mTypeVariables)
          && Objects.equals(mStateValues, builder.mStateValues)
          && Objects.equals(mCachedValues, builder.mCachedValues)
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
          && Objects.equals(mTransitionClass, builder.mTransitionClass)
          && Objects.equals(mTransitionContainerClass, builder.mTransitionContainerClass)
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
          mWorkingRangeRegisterMethod,
          mWorkingRangeMethods,
          mUpdateStateMethods,
          mUpdateStateWithTransitionMethods,
          mProps,
          mPropDefaults,
          mTypeVariables,
          mStateValues,
          mCachedValues,
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
          mTransitionClass,
          mTransitionContainerClass,
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
