// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import javax.lang.model.element.Modifier;

import java.util.List;

import com.facebook.common.internal.ImmutableList;
import com.facebook.components.annotations.OnCreateLayoutWithSizeSpec;
import com.facebook.components.specmodels.generator.BuilderGenerator;
import com.facebook.components.specmodels.generator.CanMeasureGenerator;
import com.facebook.components.specmodels.generator.ComponentImplGenerator;
import com.facebook.components.specmodels.generator.DelegateMethodGenerator;
import com.facebook.components.specmodels.generator.EventGenerator;
import com.facebook.components.specmodels.generator.JavadocGenerator;
import com.facebook.components.specmodels.generator.PreambleGenerator;
import com.facebook.components.specmodels.generator.PureRenderGenerator;
import com.facebook.components.specmodels.generator.StateGenerator;
import com.facebook.components.specmodels.generator.TreePropGenerator;
import com.facebook.components.specmodels.generator.TypeSpecDataHolder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
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
      boolean isPublic,
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
            .isPublic(isPublic)
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
  public List<SpecModelValidationError> validate() {
    return SpecModelValidation.validateLayoutSpecModel(this);
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
            LayoutSpecDelegateMethodDescriptions.DELEGATE_METHODS_MAP))
        .addTypeSpecDataHolder(CanMeasureGenerator.generate(this))
        .addTypeSpecDataHolder(PureRenderGenerator.generate(this))
        .addTypeSpecDataHolder(EventGenerator.generate(this))
        .addTypeSpecDataHolder(StateGenerator.generate(this))
        .addTypeSpecDataHolder(BuilderGenerator.generate(this))
        .build()
        .addToTypeSpec(typeSpec);

    return typeSpec.build();
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
