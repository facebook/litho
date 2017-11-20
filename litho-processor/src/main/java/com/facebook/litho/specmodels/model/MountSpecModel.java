/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.facebook.litho.specmodels.generator.BuilderGenerator;
import com.facebook.litho.specmodels.generator.ClassAnnotationsGenerator;
import com.facebook.litho.specmodels.generator.ComponentBodyGenerator;
import com.facebook.litho.specmodels.generator.DelegateMethodGenerator;
import com.facebook.litho.specmodels.generator.EventGenerator;
import com.facebook.litho.specmodels.generator.JavadocGenerator;
import com.facebook.litho.specmodels.generator.MountSpecGenerator;
import com.facebook.litho.specmodels.generator.PreambleGenerator;
import com.facebook.litho.specmodels.generator.PureRenderGenerator;
import com.facebook.litho.specmodels.generator.RenderDataGenerator;
import com.facebook.litho.specmodels.generator.StateGenerator;
import com.facebook.litho.specmodels.generator.TreePropGenerator;
import com.facebook.litho.specmodels.generator.TriggerGenerator;
import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.List;
import javax.lang.model.element.Modifier;

/**
 * Model that is an abstract representation of a {@link com.facebook.litho.annotations.MountSpec}.
 */
public class MountSpecModel implements SpecModel, HasPureRender {
  private final SpecModelImpl mSpecModel;
  private final boolean mIsPureRender;
  private final boolean mCanMountIncrementally;
  private final boolean mShouldUseDisplayList;
  private final int mPoolSize;
  private final boolean mCanPreallocate;
  private final TypeName mMountType;

  public MountSpecModel(
      String qualifiedSpecClassName,
      String componentClassName,
      ImmutableList<SpecMethodModel<DelegateMethod, Void>> delegateMethods,
      ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> eventMethods,
      ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> triggerMethods,
      ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> updateStateMethods,
      ImmutableList<String> cachedPropNames,
      ImmutableList<TypeVariableName> typeVariables,
      ImmutableList<PropDefaultModel> propDefaults,
      ImmutableList<EventDeclarationModel> eventDeclarations,
      ImmutableList<BuilderMethodModel> builderMethodModels,
      String classJavadoc,
      ImmutableList<AnnotationSpec> classAnnotations,
      ImmutableList<PropJavadocModel> propJavadocs,
      boolean isPublic,
      DependencyInjectionHelper dependencyInjectionHelper,
      boolean isPureRender,
      boolean canMountIncrementally,
      boolean shouldUseDisplayList,
      int poolSize,
      boolean canPreallocate,
      TypeName mountType,
      SpecElementType specElementType,
      Object representedObject) {
    mSpecModel =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName(qualifiedSpecClassName)
            .componentClassName(componentClassName)
            .componentClass(ClassNames.COMPONENT)
            .delegateMethods(delegateMethods)
            .eventMethods(eventMethods)
            .triggerMethods(triggerMethods)
            .updateStateMethods(updateStateMethods)
            .cachedPropNames(cachedPropNames)
            .typeVariables(typeVariables)
            .propDefaults(propDefaults)
            .eventDeclarations(eventDeclarations)
            .extraBuilderMethods(builderMethodModels)
            .classAnnotations(classAnnotations)
            .classJavadoc(classJavadoc)
            .propJavadocs(propJavadocs)
            .isPublic(isPublic)
            .dependencyInjectionGenerator(dependencyInjectionHelper)
            .specElementType(specElementType)
            .representedObject(representedObject)
            .build();
    mIsPureRender = isPureRender;
    mCanMountIncrementally = canMountIncrementally;
    mShouldUseDisplayList = shouldUseDisplayList;
    mPoolSize = poolSize;
    mCanPreallocate = canPreallocate;
    mMountType = mountType;
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
  public ImmutableList<SpecMethodModel<DelegateMethod, Void>> getDelegateMethods() {
    return mSpecModel.getDelegateMethods();
  }

  @Override
  public ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> getEventMethods() {
    return mSpecModel.getEventMethods();
  }

  @Override
  public ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> getTriggerMethods() {
    return mSpecModel.getTriggerMethods();
  }

  @Override
  public ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> getUpdateStateMethods() {
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
  public ImmutableList<AnnotationSpec> getClassAnnotations() {
    return mSpecModel.getClassAnnotations();
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
    return true;
  }

  @Override
  public boolean hasInjectedDependencies() {
    return mSpecModel.hasInjectedDependencies();
  }

  @Override
  public boolean hasDeepCopy() {
    return false;
  }

  @Override
  public boolean shouldGenerateHasState() {
    return true;
  }

  @Override
  public boolean shouldCheckIdInIsEquivalentToMethod() {
    return true;
  }

  public DependencyInjectionHelper getDependencyInjectionHelper() {
    return mSpecModel.getDependencyInjectionHelper();
  }

  @Override
  public SpecElementType getSpecElementType() {
    return mSpecModel.getSpecElementType();
  }

  @Override
  public Object getRepresentedObject() {
    return mSpecModel.getRepresentedObject();
  }

  @Override
  public List<SpecModelValidationError> validate() {
    return SpecModelValidation.validateMountSpecModel(this);
  }

  @Override
  public TypeSpec generate() {
    final TypeSpec.Builder typeSpec =
        TypeSpec.classBuilder(getComponentName())
            .superclass(
                ParameterizedTypeName.get(ClassNames.COMPONENT, mSpecModel.getComponentTypeName()))
            .addTypeVariables(getTypeVariables());

    if (isPublic()) {
      typeSpec.addModifiers(Modifier.PUBLIC);
    }

    if (!hasInjectedDependencies()) {
      typeSpec.addModifiers(Modifier.FINAL);
    }

    TypeSpecDataHolder.newBuilder()
        .addTypeSpecDataHolder(JavadocGenerator.generate(this))
        .addTypeSpecDataHolder(ClassAnnotationsGenerator.generate(this))
        .addTypeSpecDataHolder(PreambleGenerator.generate(this))
        .addTypeSpecDataHolder(ComponentBodyGenerator.generate(this, null))
        .addTypeSpecDataHolder(TreePropGenerator.generate(this))
        .addTypeSpecDataHolder(
            DelegateMethodGenerator.generateDelegates(
                this, DelegateMethodDescriptions.MOUNT_SPEC_DELEGATE_METHODS_MAP))
        .addTypeSpecDataHolder(MountSpecGenerator.generateGetMountType(this))
        .addTypeSpecDataHolder(MountSpecGenerator.generatePoolSize(this))
        .addTypeSpecDataHolder(MountSpecGenerator.generateCanPreallocate(this))
        .addTypeSpecDataHolder(MountSpecGenerator.generateCanMountIncrementally(this))
        .addTypeSpecDataHolder(MountSpecGenerator.generateShouldUseDisplayList(this))
        .addTypeSpecDataHolder(MountSpecGenerator.generateIsMountSizeDependent(this))
        .addTypeSpecDataHolder(MountSpecGenerator.generateCallsShouldUpdateOnMount(this))
        .addTypeSpecDataHolder(PureRenderGenerator.generate(this))
        .addTypeSpecDataHolder(EventGenerator.generate(this))
        .addTypeSpecDataHolder(TriggerGenerator.generate(this))
        .addTypeSpecDataHolder(StateGenerator.generate(this))
        .addTypeSpecDataHolder(RenderDataGenerator.generate(this))
        .addTypeSpecDataHolder(BuilderGenerator.generate(this))
        .build()
        .addToTypeSpec(typeSpec);

    return typeSpec.build();
  }

  @Override
  public boolean isPureRender() {
    return mIsPureRender;
  }

  public boolean canMountIncrementally() {
    return mCanMountIncrementally;
  }

  public boolean shouldUseDisplayList() {
    return mShouldUseDisplayList;
  }

  public int getPoolSize() {
    return mPoolSize;
  }

  public boolean canPreallocate() {
    return mCanPreallocate;
  }

  public TypeName getMountType() {
    return mMountType;
  }

  @Override
  public String toString() {
    return "MountSpecModel{"
        + "mSpecModel="
        + mSpecModel
        + ", mIsPureRender="
        + mIsPureRender
        + ", mCanMountIncrementally="
        + mCanMountIncrementally
        + ", mShouldUseDisplayList="
        + mShouldUseDisplayList
        + ", mPoolSize="
        + mPoolSize
        + ", mCanPreallocate="
        + mCanPreallocate
        + ", mMountType="
        + mMountType
        + '}';
  }
}
