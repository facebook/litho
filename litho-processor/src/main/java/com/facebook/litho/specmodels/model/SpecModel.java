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
import java.util.List;
import javax.annotation.Nullable;

/**
 * A model that represents a ComponentSpec.
 */
public interface SpecModel {

  /**
   * @return the name of the spec.
   */
  String getSpecName();

  /**
   * @return the {@link TypeName} representing the name of the Spec.
   */
  TypeName getSpecTypeName();

  /**
   * @return the name of the component that will be generated from this model.
   */
  String getComponentName();

  /**
   * @return the {@link TypeName} representing the name of the component that will be generated
   * from this model.
   */
  TypeName getComponentTypeName();

  /**
   * @return the list of methods defined in the spec which will be delegated to by the component
   * that is generated from this model.
   */
  ImmutableList<DelegateMethodModel> getDelegateMethods();

  /**
   * @return the list of event methods defined by the spec.
   */
  ImmutableList<EventMethodModel> getEventMethods();

  /** @return the list of trigger methods defined by the spec. */
  ImmutableList<EventMethodModel> getTriggerMethods();

  /** @return the list of methods defined in the spec for updating state. */
  ImmutableList<UpdateStateMethodModel> getUpdateStateMethods();

  /**
   * @return the set of props that are defined by the spec.
   */
  ImmutableList<PropModel> getProps();

  /**
   * @return the set of prop defaults defined by the spec.
   */
  ImmutableList<PropDefaultModel> getPropDefaults();

  /**
   * @return the type variables that are defined by the spec.
   */
  ImmutableList<TypeVariableName> getTypeVariables();

  /**
   * @return the set of state values that are defined by the spec.
   */
  ImmutableList<StateParamModel> getStateValues();

  /**
   * @return the set of inter-stage inputs that are defined by the spec.
   */
  ImmutableList<InterStageInputParamModel> getInterStageInputs();

  /**
   * @return the set of tree props that are defined by the spec.
   */
  ImmutableList<TreePropModel> getTreeProps();

  /**
   * @return the set of events that are defined by the spec.
   */
  ImmutableList<EventDeclarationModel> getEventDeclarations();

  /** @return the set of methods that are implicitly added to the builder. */
  ImmutableList<BuilderMethodModel> getExtraBuilderMethods();

  /** @return the set of diff params used within lifecycle methods in the spec. */
  ImmutableList<RenderDataDiffModel> getRenderDataDiffs();

  /**
   * @return the javadoc for this spec.
   */
  String getClassJavadoc();

  /**
   * @return the javadoc for the props defined by the spec.
   */
  ImmutableList<PropJavadocModel> getPropJavadocs();

  /**
   * @return whether the generated class should be public or not.
   */
  boolean isPublic();

  /**
   * @return the {@link ClassName} of the context that is used in the generated class.
   */
  ClassName getContextClass();

  /**
   * @return the {@link ClassName} of the component that is used in the generated class.
   */
  ClassName getComponentClass();

  /**
   * @return the {@link ClassName} of the state container class that is used in the generated class.
   */
  ClassName getStateContainerClass();

  /**
   * @return the {@link TypeName} of the update state interface that is used in the generated class.
   */
  TypeName getUpdateStateInterface();

  /**
   * @return the scope method name on the Context class.
   */
  String getScopeMethodName();

  /**
   * @return true if the generated class supports styling, false otherwise.
   */
  boolean isStylingSupported();

  /**
   * @return whether this spec uses dependency injection.
   */
  boolean hasInjectedDependencies();

  /**
   * @return whether or not to check component id in isEquivalentTo() method.
   */
  boolean shouldCheckIdInIsEquivalentToMethod();

  /**
   * @return whether or not to deep copy this component.
   */
  boolean hasDeepCopy();

  /** @return whether or not to generate a hasState method. */
  boolean shouldGenerateHasState();

  /**
   * @return null if this spec does not use dependency injection, otherwise return the generator
   *     that should be used to generate the correct methods for dependency injection to work for
   *     this component.
   */
  @Nullable
  DependencyInjectionHelper getDependencyInjectionHelper();

  /**
   * @return the element that this model represents.
   */
  Object getRepresentedObject();

  /**
   * @return a list of errors in the spec model. If the list is empty, then this model is valid.
   */
  List<SpecModelValidationError> validate();

  /**
   * @return a {@link TypeSpec} representing the class that is generated by
   * this model.
   */
  TypeSpec generate();
}
