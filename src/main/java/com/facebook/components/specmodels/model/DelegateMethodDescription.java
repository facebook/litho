// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import javax.annotation.concurrent.Immutable;
import javax.lang.model.element.Modifier;

import java.lang.annotation.Annotation;

import com.facebook.common.internal.ImmutableList;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;

/**
 * Describes the signature and other feature of a delegate method.
 *
 * We use method descriptions to refer to abstract methods defined in
 * com.facebook.components.ComponentLifecycle, so that we can define implementations that delegate
 * to client-declared methods with annotated props.
 */
@Immutable
public final class DelegateMethodDescription {

  /**
   * Defines possible param types that can be used in delegate methods.
   */
  public enum OptionalParameterType {
    PROP,
    TREE_PROP,
    STATE,
    PARAM,
    INTER_STAGE_OUTPUT,
    PROP_OUTPUT,
    STATE_OUTPUT,
    STATE_VALUE,
  }

  public final ImmutableList<AnnotationSpec> annotations;
  public final Modifier accessType;
  public final TypeName returnType;
  public final String name;
  public final ImmutableList<TypeName> definedParameterTypes;
  public final ImmutableList<OptionalParameterType> optionalParameterTypes;
  public final ImmutableList<TypeName> exceptions;

  private DelegateMethodDescription(Builder builder) {
    annotations = builder.annotations;
    accessType = builder.accessType;
    returnType = builder.returnType;
    name = builder.name;
    definedParameterTypes = builder.definedParameterTypes;
    optionalParameterTypes = builder.optionalParameterTypes;
    exceptions = builder.exceptions;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private ImmutableList<AnnotationSpec> annotations;
    private Modifier accessType;
    private TypeName returnType;
    private String name;
    private ImmutableList<TypeName> definedParameterTypes;
    private ImmutableList<OptionalParameterType> optionalParameterTypes;
    private ImmutableList<TypeName> exceptions;

    private Builder() {
    }

    public Builder annotations(ImmutableList<AnnotationSpec> annotations) {
      this.annotations = annotations;
      return this;
    }

    public Builder accessType(Modifier accessType) {
      this.accessType = accessType;
      return this;
    }

    public Builder returnType(TypeName returnType) {
      this.returnType = returnType;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder definedParameterTypes(ImmutableList<TypeName> parameterTypes) {
      this.definedParameterTypes = parameterTypes;
      return this;
    }

    public Builder optionalParameterTypes(
        ImmutableList<OptionalParameterType> optionalParameterTypes) {
      this.optionalParameterTypes = optionalParameterTypes;
      return this;
    }

    public Builder exceptions(ImmutableList<TypeName> exceptions) {
      this.exceptions = exceptions;
      return this;
    }

    public DelegateMethodDescription build() {
      validate();
      initFieldsIfRequired();

      return new DelegateMethodDescription(this);
    }

    private void validate() {
      if (accessType == null) {
        throw new IllegalStateException("Access type must be specified");
      }

      if (returnType == null) {
        throw new IllegalStateException("Return type must be specified");
      }

      if (name == null) {
        throw new IllegalStateException("Name must be specified");
      }

      if (optionalParameterTypes == null) {
        throw new IllegalStateException("Optional parameter types must be specified");
      }
    }

    private void initFieldsIfRequired() {
      if (annotations == null) {
        annotations = ImmutableList.of();
      }

      if (definedParameterTypes == null) {
        definedParameterTypes = ImmutableList.of();
      }

      if (exceptions == null) {
        exceptions = ImmutableList.of();
      }
    }
  }
}
