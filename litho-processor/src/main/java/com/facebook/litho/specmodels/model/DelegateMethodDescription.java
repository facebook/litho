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

package com.facebook.litho.specmodels.model;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import javax.annotation.concurrent.Immutable;
import javax.lang.model.element.Modifier;

/**
 * Describes the signature and other feature of a delegate method.
 *
 * We use method descriptions to refer to abstract methods defined in
 * com.facebook.litho.ComponentLifecycle, so that we can define implementations that delegate
 * to client-declared methods with annotated props.
 */
@Immutable
public final class DelegateMethodDescription {

  /**
   * Defines possible param types that can be used in delegate methods.
   */
  public enum OptionalParameterType {
    PROP,
    DIFF_PROP,
    TREE_PROP,
    STATE,
    DIFF_STATE,
    PARAM,
    INTER_STAGE_OUTPUT,
    PROP_OUTPUT,
    STATE_OUTPUT,
    STATE_VALUE,
    DIFF,
    INJECT_PROP,
    CACHED_VALUE,
  }

  public final ImmutableList<AnnotationSpec> annotations;
  public final Modifier accessType;
  public final TypeName returnType;
  public final String name;
  public final ImmutableList<TypeName> definedParameterTypes;
  public final ImmutableList<MethodParamModel> optionalParameters;
  public final ImmutableList<OptionalParameterType> optionalParameterTypes;
  public final ImmutableList<Class<? extends Annotation>> interStageInputAnnotations;
  public final ImmutableList<MethodSpec> extraMethods;
  public final ImmutableList<TypeName> exceptions;

  private DelegateMethodDescription(Builder builder) {
    annotations = builder.annotations;
    accessType = builder.accessType;
    returnType = builder.returnType;
    name = builder.name;
    definedParameterTypes = builder.definedParameterTypes;
    optionalParameters = builder.optionalParameters;
    optionalParameterTypes = builder.optionalParameterTypes;
    interStageInputAnnotations = builder.interStageInputAnnotations;
    extraMethods = builder.extraMethods;
    exceptions = builder.exceptions;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder fromDelegateMethodDescription(DelegateMethodDescription methodDescription) {
    return new Builder()
        .annotations(methodDescription.annotations)
        .accessType(methodDescription.accessType)
        .returnType(methodDescription.returnType)
        .name(methodDescription.name)
        .definedParameterTypes(methodDescription.definedParameterTypes)
        .optionalParameters(methodDescription.optionalParameters)
        .optionalParameterTypes(methodDescription.optionalParameterTypes)
        .interStageInputAnnotations(methodDescription.interStageInputAnnotations)
        .extraMethods(methodDescription.extraMethods)
        .exceptions(methodDescription.exceptions);
  }

  public static class Builder {
    private ImmutableList<AnnotationSpec> annotations;
    private Modifier accessType;
    private TypeName returnType;
    private String name;
    private ImmutableList<TypeName> definedParameterTypes;
    private ImmutableList<OptionalParameterType> optionalParameterTypes;
    private ImmutableList<MethodParamModel> optionalParameters;
    private ImmutableList<Class<? extends Annotation>> interStageInputAnnotations;
    private ImmutableList<MethodSpec> extraMethods;
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

    /**
     * Optional parameters are named parameters from the component impl that aren't props or state.
     */
    public Builder optionalParameters(ImmutableList<MethodParamModel> optionalParameters) {
      this.optionalParameters = optionalParameters;
      return this;
    }

    public Builder optionalParameterTypes(
        ImmutableList<OptionalParameterType> optionalParameterTypes) {
      this.optionalParameterTypes = optionalParameterTypes;
      return this;
    }

    public Builder interStageInputAnnotations(
        ImmutableList<Class<? extends Annotation>> interStageInputAnnotations) {
      this.interStageInputAnnotations = interStageInputAnnotations;
      return this;
    }

    /**
     * A list of extra methods that should be generate when this method description is used.
     */
    public Builder extraMethods(ImmutableList<MethodSpec> extraMethods) {
      this.extraMethods = extraMethods;
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

      if (optionalParameters == null) {
        optionalParameters = ImmutableList.of();
      }

      if (interStageInputAnnotations == null) {
        interStageInputAnnotations = ImmutableList.of();
      }

      if (extraMethods == null) {
        extraMethods = ImmutableList.of();
      }

      if (exceptions == null) {
        exceptions = ImmutableList.of();
      }
    }
  }
}
