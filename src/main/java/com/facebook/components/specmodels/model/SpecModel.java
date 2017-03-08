// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import com.facebook.common.internal.ImmutableList;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

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

  /**
   * @return the list of methods defined in the spec for updating state.
   */
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
   * @return true if the generated class supports styling, false otherwise.
   */
  boolean isStylingSupported();

  /**
   * @return true if this component is able to measure itself using specific size constraints,
   * false otherwise.
   */
  boolean canMeasure();

  /**
   * @return whether this spec uses dependency injection.
   */
  boolean hasInjectedDependencies();

  /**
   * @return null if this spec does not use dependency injection, otherwise return the generator
   * that should be used to generate the correct methods for dependency injection to work for
   * this component.
   */
  DependencyInjectionGenerator getDependencyInjectionGenerator();

  /**
   * @return the element that this model represents.
   */
  Object getRepresentedObject();
}
