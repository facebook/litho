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

package com.facebook.litho.sections.specmodels.model;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.BuilderMethodModel;
import com.facebook.litho.specmodels.model.CachedValueParamModel;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.FieldModel;
import com.facebook.litho.specmodels.model.InjectPropModel;
import com.facebook.litho.specmodels.model.InterStageInputParamModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.PropDefaultModel;
import com.facebook.litho.specmodels.model.PropJavadocModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.RenderDataDiffModel;
import com.facebook.litho.specmodels.model.SpecElementType;
import com.facebook.litho.specmodels.model.SpecGenerator;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelImpl;
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
import javax.annotation.Nullable;

/**
 * Model that is an abstract representation of a {@link
 * com.facebook.litho.sections.annotations.DiffSectionSpec}.
 */
public class DiffSectionSpecModel implements SpecModel, HasService {

  private final SpecModelImpl mSpecModel;
  private final @Nullable MethodParamModel mServiceParam;
  private final SpecGenerator<DiffSectionSpecModel> mSpecGenerator;

  public DiffSectionSpecModel(
      String qualifiedSpecClassName,
      String componentClassName,
      ImmutableList<SpecMethodModel<DelegateMethod, Void>> delegateMethods,
      ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> eventMethods,
      ImmutableList<AnnotationSpec> classAnnotations,
      ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> triggerMethods,
      ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> updateStateMethods,
      ImmutableList<TypeVariableName> typeVariables,
      ImmutableList<PropDefaultModel> propDefaults,
      ImmutableList<EventDeclarationModel> eventDeclarations,
      ImmutableList<BuilderMethodModel> builderMethodModels,
      ImmutableList<TagModel> tags,
      String classJavadoc,
      ImmutableList<PropJavadocModel> propJavadocs,
      boolean isPublic,
      SpecElementType specElementType,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper,
      Object representedObject,
      SpecGenerator<DiffSectionSpecModel> specGenerator,
      ImmutableList<FieldModel> fields) {
    mSpecModel =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName(qualifiedSpecClassName)
            .componentClassName(componentClassName)
            .componentClass(SectionClassNames.SECTION)
            .delegateMethods(delegateMethods)
            .updateStateMethods(updateStateMethods)
            .typeVariables(typeVariables)
            .eventMethods(eventMethods)
            .classAnnotations(classAnnotations)
            .triggerMethods(triggerMethods)
            .propDefaults(propDefaults)
            .eventDeclarations(eventDeclarations)
            .extraBuilderMethods(builderMethodModels)
            .tags(tags)
            .classJavadoc(classJavadoc)
            .propJavadocs(propJavadocs)
            .isPublic(isPublic)
            .dependencyInjectionHelper(dependencyInjectionHelper)
            .specElementType(specElementType)
            .representedObject(representedObject)
            .fields(fields)
            .build();
    mServiceParam = SectionSpecModelUtils.createServiceParam(mSpecModel);
    mSpecGenerator = specGenerator;
  }

  @Override
  public String getSpecName() {
    return mSpecModel.getSpecName();
  }

  @Override
  public ClassName getSpecTypeName() {
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
  public ImmutableList<FieldModel> getFields() {
    return mSpecModel.getFields();
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
  @Nullable
  public SpecMethodModel<EventMethod, Void> getWorkingRangeRegisterMethod() {
    return mSpecModel.getWorkingRangeRegisterMethod();
  }

  @Override
  public ImmutableList<WorkingRangeMethodModel> getWorkingRangeMethods() {
    return mSpecModel.getWorkingRangeMethods();
  }

  @Override
  public ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> getUpdateStateMethods() {
    return mSpecModel.getUpdateStateMethods();
  }

  @Override
  public ImmutableList<SpecMethodModel<UpdateStateMethod, Void>>
      getUpdateStateWithTransitionMethods() {
    return mSpecModel.getUpdateStateWithTransitionMethods();
  }

  @Override
  public ImmutableList<PropModel> getRawProps() {
    return mSpecModel.getRawProps();
  }

  @Override
  public ImmutableList<PropModel> getProps() {
    return mSpecModel.getProps();
  }

  @Override
  public ImmutableList<InjectPropModel> getRawInjectProps() {
    return mSpecModel.getRawInjectProps();
  }

  @Override
  public ImmutableList<InjectPropModel> getInjectProps() {
    return mSpecModel.getInjectProps();
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
  public ImmutableList<CachedValueParamModel> getCachedValues() {
    return mSpecModel.getCachedValues();
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
  public ImmutableList<TagModel> getTags() {
    return mSpecModel.getTags();
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
    return ClassNames.STATE_CONTAINER;
  }

  @Override
  public ClassName getTransitionClass() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ClassName getTransitionContainerClass() {
    throw new UnsupportedOperationException();
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
  public boolean shouldGenerateCopyMethod() {
    return true;
  }

  @Override
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
  public List<SpecModelValidationError> validate(EnumSet<RunMode> runMode) {
    return SpecModelValidation.validateDiffSectionSpecModel(this, runMode);
  }

  @Override
  public TypeSpec generate(EnumSet<RunMode> runMode) {
    return mSpecGenerator.generate(this, runMode);
  }

  @Override
  public MethodParamModel getServiceParam() {
    return mServiceParam;
  }

  @Override
  public boolean shouldGenerateIsEquivalentTo() {
    return true;
  }

  @Override
  public String toString() {
    return "DiffSectionSpecModel{"
        + "mSpecModel="
        + mSpecModel
        + ", mServiceParam="
        + mServiceParam
        + '}';
  }
}
