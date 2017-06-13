/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.facebook.litho.specmodels.generator.BuilderGenerator;
import com.facebook.litho.specmodels.generator.ComponentImplGenerator;
import com.facebook.litho.specmodels.generator.DelegateMethodGenerator;
import com.facebook.litho.specmodels.generator.EventGenerator;
import com.facebook.litho.specmodels.generator.JavadocGenerator;
import com.facebook.litho.specmodels.generator.MountSpecGenerator;
import com.facebook.litho.specmodels.generator.PreambleGenerator;
import com.facebook.litho.specmodels.generator.PureRenderGenerator;
import com.facebook.litho.specmodels.generator.RenderInfoGenerator;
import com.facebook.litho.specmodels.generator.StateGenerator;
import com.facebook.litho.specmodels.generator.TreePropGenerator;
import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.util.List;

import javax.lang.model.element.Modifier;

/**
 * Model that is an abstract representation of a
 * {@link com.facebook.litho.annotations.MountSpec}.
 */
public class MountSpecModel implements SpecModel, HasPureRender {
  private final SpecModelImpl mSpecModel;
  private final boolean mIsPureRender;
  private final boolean mCanMountIncrementally;
  private final boolean mShouldUseDisplayList;
  private final int mPoolSize;
  private final TypeName mMountType;

  public MountSpecModel(
      String qualifiedSpecClassName,
      String componentClassName,
      ImmutableList<DelegateMethodModel> delegateMethods,
      ImmutableList<EventMethodModel> eventMethods,
      ImmutableList<UpdateStateMethodModel> updateStateMethods,
      ImmutableList<TypeVariableName> typeVariables,
      ImmutableList<PropDefaultModel> propDefaults,
      ImmutableList<EventDeclarationModel> eventDeclarations,
      String classJavadoc,
      ImmutableList<PropJavadocModel> propJavadocs,
      boolean isPublic,
      DependencyInjectionHelper dependencyInjectionHelper,
      boolean isPureRender,
      boolean canMountIncrementally,
      boolean shouldUseDisplayList,
      int poolSize,
      TypeName mountType,
      Object representedObject) {
    mSpecModel =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName(qualifiedSpecClassName)
            .componentClassName(componentClassName)
            .delegateMethods(delegateMethods)
            .eventMethods(eventMethods)
            .updateStateMethods(updateStateMethods)
            .typeVariables(typeVariables)
            .propDefaults(propDefaults)
            .eventDeclarations(eventDeclarations)
            .classJavadoc(classJavadoc)
            .propJavadocs(propJavadocs)
            .isPublic(isPublic)
            .dependencyInjectionGenerator(dependencyInjectionHelper)
            .representedObject(representedObject)
            .build();
    mIsPureRender = isPureRender;
    mCanMountIncrementally = canMountIncrementally;
    mShouldUseDisplayList = shouldUseDisplayList;
    mPoolSize = poolSize;
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
  public ImmutableList<DiffModel> getDiffs() {
    return mSpecModel.getDiffs();
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
  public boolean hasInjectedDependencies() {
    return mSpecModel.hasInjectedDependencies();
  }

  public DependencyInjectionHelper getDependencyInjectionHelper() {
    return mSpecModel.getDependencyInjectionHelper();
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
            .superclass(ClassNames.COMPONENT_LIFECYCLE)
            .addTypeVariables(getTypeVariables());

    if (isPublic()) {
      typeSpec.addModifiers(Modifier.PUBLIC);
    }

    if (hasInjectedDependencies()) {
      getDependencyInjectionHelper().generate(this).addToTypeSpec(typeSpec);
    } else {
      typeSpec.addModifiers(Modifier.FINAL);
    }

    TypeSpecDataHolder.newBuilder()
        .addTypeSpecDataHolder(JavadocGenerator.generate(this))
        .addTypeSpecDataHolder(PreambleGenerator.generate(this))
        .addTypeSpecDataHolder(ComponentImplGenerator.generate(this))
        .addTypeSpecDataHolder(TreePropGenerator.generate(this))
        .addTypeSpecDataHolder(DelegateMethodGenerator.generateDelegates(
            this,
            DelegateMethodDescriptions.MOUNT_SPEC_DELEGATE_METHODS_MAP))
        .addTypeSpecDataHolder(MountSpecGenerator.generateGetMountType(this))
        .addTypeSpecDataHolder(MountSpecGenerator.generatePoolSize(this))
        .addTypeSpecDataHolder(MountSpecGenerator.generateCanMountIncrementally(this))
        .addTypeSpecDataHolder(MountSpecGenerator.generateShouldUseDisplayList(this))
        .addTypeSpecDataHolder(MountSpecGenerator.generateIsMountSizeDependent(this))
        .addTypeSpecDataHolder(PureRenderGenerator.generate(this))
        .addTypeSpecDataHolder(EventGenerator.generate(this))
        .addTypeSpecDataHolder(StateGenerator.generate(this))
        .addTypeSpecDataHolder(RenderInfoGenerator.generate(this))
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

  public TypeName getMountType() {
    return mMountType;
  }
}
