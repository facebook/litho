// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import com.facebook.common.internal.ImmutableList;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

/**
 * Simple implementation of {@link SpecModel}.
 */
@Immutable
public final class SpecModelImpl implements SpecModel {
  private static final String SPEC_SUFFIX = "Spec";

  private final String mSpecName;
  private final TypeName mSpecTypeName;
  private final String mComponentName;
  private final TypeName mComponentTypeName;
  private final ImmutableList<DelegateMethodModel> mDelegateMethods;
  private final ImmutableList<EventMethodModel> mEventMethods;
  private final ImmutableList<UpdateStateMethodModel> mUpdateStateMethods;
  private final ImmutableList<PropModel> mProps;
  private final ImmutableList<PropDefaultModel> mPropDefaults;
  private final ImmutableList<TypeVariableName> mTypeVariables;
  private final ImmutableList<StateParamModel> mStateValues;
  private final ImmutableList<InterStageInputParamModel> mInterStageInputs;
  private final ImmutableList<TreePropModel> mTreeProps;
  private final ImmutableList<EventDeclarationModel> mEventDeclarations;
  private final boolean mHasInjectedDependencies;
  @Nullable private final DependencyInjectionGenerator mDependencyInjectionGenerator;
  private final Object mRepresentedObject;

  private SpecModelImpl(
      String qualifiedSpecClassName,
      ImmutableList<DelegateMethodModel> delegateMethods,
      ImmutableList<EventMethodModel> eventMethods,
      ImmutableList<UpdateStateMethodModel> updateStateMethods,
      ImmutableList<TypeVariableName> typeVariables,
      ImmutableList<PropDefaultModel> propDefaults,
      ImmutableList<EventDeclarationModel> eventDeclarations,
      @Nullable DependencyInjectionGenerator dependencyInjectionGenerator,
      Object representedObject) {
    mSpecName = getSpecName(qualifiedSpecClassName);
    mSpecTypeName = ClassName.bestGuess(qualifiedSpecClassName);
    mComponentName = getComponentName(qualifiedSpecClassName);
    mComponentTypeName = getComponentTypeName(qualifiedSpecClassName);
    mDelegateMethods = delegateMethods;
    mEventMethods = eventMethods;
    mUpdateStateMethods = updateStateMethods;
    mProps = getProps(delegateMethods, eventMethods, updateStateMethods);
    mPropDefaults = propDefaults;
    mTypeVariables = typeVariables;
    mStateValues = getStateValues(delegateMethods, eventMethods, updateStateMethods);
    mInterStageInputs = getInterStageInputs(delegateMethods, eventMethods, updateStateMethods);
    mTreeProps = getTreeProps(delegateMethods, eventMethods, updateStateMethods);
    mEventDeclarations = eventDeclarations;
    mHasInjectedDependencies = dependencyInjectionGenerator != null;
    mDependencyInjectionGenerator = dependencyInjectionGenerator;
    mRepresentedObject = representedObject;
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
  public ClassName getContextClass() {
    throw new RuntimeException("Don't delegate to this method!");
  }

  @Override
  public ClassName getComponentClass() {
    throw new RuntimeException("Don't delegate to this method!");
  }

  @Override
  public ClassName getStateContainerClass() {
    throw new RuntimeException("Don't delegate to this method!");
  }

  @Override
  public TypeName getUpdateStateInterface() {
    throw new RuntimeException("Don't delegate to this method!");
  }

  @Override
  public boolean isStylingSupported() {
    throw new RuntimeException("Don't delegate to this method!");
  }

  @Override
  public boolean canMeasure() {
    throw new RuntimeException("Don't delegate to this method!");
  }

  @Override
  public boolean hasInjectedDependencies() {
    return mHasInjectedDependencies;
  }

  @Nullable
  @Override
  public DependencyInjectionGenerator getDependencyInjectionGenerator() {
    return mDependencyInjectionGenerator;
  }

  @Override
  public Object getRepresentedObject() {
    return mRepresentedObject;
  }

  private static String getSpecName(String qualifiedSpecClassName) {
    return qualifiedSpecClassName.substring(qualifiedSpecClassName.lastIndexOf('.') + 1);
  }

  private static TypeName getComponentTypeName(String qualifiedSpecClassName) {
    final String qualifiedComponentClassName =
        qualifiedSpecClassName.substring(0, qualifiedSpecClassName.length() - SPEC_SUFFIX.length());
    return ClassName.bestGuess(qualifiedComponentClassName);
  }

  private static String getComponentName(String qualifiedSpecClassName) {
    final String specName = getSpecName(qualifiedSpecClassName);
    return specName.substring(0, specName.length() - SPEC_SUFFIX.length());
  }

  private static ImmutableList<PropModel> getProps(
      ImmutableList<DelegateMethodModel> delegateMethods,
      ImmutableList<EventMethodModel> eventMethods,
      ImmutableList<UpdateStateMethodModel> updateStateMethods) {
    final Set<PropModel> props = new LinkedHashSet<>();
    for (DelegateMethodModel delegateMethod : delegateMethods) {
      for (MethodParamModel param : delegateMethod.methodParams) {
        if (param instanceof PropModel) {
          props.add((PropModel) param);
        }
      }
    }

    for (EventMethodModel eventMethod : eventMethods) {
      for (MethodParamModel param : eventMethod.methodParams) {
        if (param instanceof PropModel) {
          props.add((PropModel) param);
        }
      }
    }

    for (UpdateStateMethodModel updateStateMethod : updateStateMethods) {
      for (MethodParamModel param : updateStateMethod.methodParams) {
        if (param instanceof PropModel) {
          props.add((PropModel) param);
        }
      }
    }

    return ImmutableList.copyOf(new ArrayList<>(props));
  }

  private static ImmutableList<StateParamModel> getStateValues(
      ImmutableList<DelegateMethodModel> delegateMethods,
      ImmutableList<EventMethodModel> eventMethods,
      ImmutableList<UpdateStateMethodModel> updateStateMethods) {
    final Set<StateParamModel> stateValues = new LinkedHashSet<>();
    for (DelegateMethodModel delegateMethod : delegateMethods) {
      for (MethodParamModel param : delegateMethod.methodParams) {
        if (param instanceof StateParamModel) {
          stateValues.add((StateParamModel) param);
        }
      }
    }

    for (EventMethodModel eventMethod : eventMethods) {
      for (MethodParamModel param : eventMethod.methodParams) {
        if (param instanceof StateParamModel) {
          stateValues.add((StateParamModel) param);
        }
      }
    }

    for (UpdateStateMethodModel updateStateMethod : updateStateMethods) {
      for (MethodParamModel param : updateStateMethod.methodParams) {
        if (param instanceof StateParamModel) {
          stateValues.add((StateParamModel) param);
        }
      }
    }

    return ImmutableList.copyOf(new ArrayList<>(stateValues));
  }

  private static ImmutableList<InterStageInputParamModel> getInterStageInputs(
      ImmutableList<DelegateMethodModel> delegateMethods,
      ImmutableList<EventMethodModel> eventMethods,
      ImmutableList<UpdateStateMethodModel> updateStateMethods) {
    final Set<InterStageInputParamModel> interStageInputs = new LinkedHashSet<>();
    for (DelegateMethodModel delegateMethod : delegateMethods) {
      for (MethodParamModel param : delegateMethod.methodParams) {
        if (param instanceof InterStageInputParamModel) {
          interStageInputs.add((InterStageInputParamModel) param);
        }
      }
    }

    for (EventMethodModel eventMethod : eventMethods) {
      for (MethodParamModel param : eventMethod.methodParams) {
        if (param instanceof InterStageInputParamModel) {
          interStageInputs.add((InterStageInputParamModel) param);
        }
      }
    }

    for (UpdateStateMethodModel updateStateMethod : updateStateMethods) {
      for (MethodParamModel param : updateStateMethod.methodParams) {
        if (param instanceof InterStageInputParamModel) {
          interStageInputs.add((InterStageInputParamModel) param);
        }
      }
    }

    return ImmutableList.copyOf(new ArrayList<>(interStageInputs));
  }

  private static ImmutableList<TreePropModel> getTreeProps(
      ImmutableList<DelegateMethodModel> delegateMethods,
      ImmutableList<EventMethodModel> eventMethods,
      ImmutableList<UpdateStateMethodModel> updateStateMethods) {
    final Set<TreePropModel> treeProps = new LinkedHashSet<>();
    for (DelegateMethodModel delegateMethod : delegateMethods) {
      for (MethodParamModel param : delegateMethod.methodParams) {
        if (param instanceof TreePropModel) {
          treeProps.add((TreePropModel) param);
        }
      }
    }

    for (EventMethodModel eventMethod : eventMethods) {
      for (MethodParamModel param : eventMethod.methodParams) {
        if (param instanceof TreePropModel) {
          treeProps.add((TreePropModel) param);
        }
      }
    }

    for (UpdateStateMethodModel updateStateMethod : updateStateMethods) {
      for (MethodParamModel param : updateStateMethod.methodParams) {
        if (param instanceof TreePropModel) {
          treeProps.add((TreePropModel) param);
        }
      }
    }

    return ImmutableList.copyOf(new ArrayList<>(treeProps));
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String mQualifiedSpecClassName;
    private ImmutableList<DelegateMethodModel> mDelegateMethodModels;
    private ImmutableList<EventMethodModel> mEventMethodModels;
    private ImmutableList<UpdateStateMethodModel> mUpdateStateMethodModels;
    private ImmutableList<TypeVariableName> mTypeVariableNames;
    private ImmutableList<PropDefaultModel> mPropDefaultModels;
    private ImmutableList<EventDeclarationModel> mEventDeclarations;
    @Nullable private DependencyInjectionGenerator mDependencyInjectionGenerator;
    private Object mRepresentedObject;

    private Builder() {
    }

    public Builder qualifiedSpecClassName(String qualifiedSpecClassName) {
      mQualifiedSpecClassName = qualifiedSpecClassName;
      return this;
    }

    public Builder delegateMethods(ImmutableList<DelegateMethodModel> delegateMethodModels) {
      mDelegateMethodModels = delegateMethodModels;
      return this;
    }

    public Builder eventMethods(ImmutableList<EventMethodModel> eventMethodModels) {
      mEventMethodModels = eventMethodModels;
      return this;
    }

    public Builder updateStateMethods(
        ImmutableList<UpdateStateMethodModel> updateStateMethodModels) {
      mUpdateStateMethodModels = updateStateMethodModels;
      return this;
    }

    public Builder typeVariables(ImmutableList<TypeVariableName> typeVariableNames) {
      mTypeVariableNames = typeVariableNames;
      return this;
    }

    public Builder propDefaults(ImmutableList<PropDefaultModel> propDefaultModels) {
      mPropDefaultModels = propDefaultModels;
      return this;
    }

    public Builder eventDeclarations(ImmutableList<EventDeclarationModel> eventDeclarations) {
      mEventDeclarations = eventDeclarations;
      return this;
    }

    public Builder dependencyInjectionGenerator(
        @Nullable DependencyInjectionGenerator dependencyInjectionGenerator) {
      mDependencyInjectionGenerator = dependencyInjectionGenerator;
      return this;
    }

    public Builder representedObject(Object representedObject) {
      mRepresentedObject = representedObject;
      return this;
    }

    public SpecModelImpl build() {
      validate();
      initFieldsIfNecessary();

      return new SpecModelImpl(
          mQualifiedSpecClassName,
          mDelegateMethodModels,
          mEventMethodModels,
          mUpdateStateMethodModels,
          mTypeVariableNames,
          mPropDefaultModels,
          mEventDeclarations,
          mDependencyInjectionGenerator,
          mRepresentedObject);
    }

    private void validate() {
      if (mQualifiedSpecClassName == null) {
        throw new IllegalStateException("Must specify a qualified class name");
      }

      if (mDelegateMethodModels == null) {
        throw new IllegalStateException("Must specify delegate methods");
      }

      if (mRepresentedObject == null) {
        throw new IllegalStateException("Must specify represented object");
      }
    }

    private void initFieldsIfNecessary() {
      if (mTypeVariableNames == null) {
        mTypeVariableNames = ImmutableList.of();
      }

      if (mPropDefaultModels == null) {
        mPropDefaultModels = ImmutableList.of();
      }

      if (mEventMethodModels == null) {
        mEventMethodModels = ImmutableList.of();
      }

      if (mUpdateStateMethodModels == null) {
        mUpdateStateMethodModels = ImmutableList.of();
      }

      if (mEventDeclarations == null) {
        mEventDeclarations = ImmutableList.of();
      }
    }
  }
}
