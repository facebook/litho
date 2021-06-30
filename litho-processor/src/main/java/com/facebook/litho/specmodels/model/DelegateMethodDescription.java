/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import javax.annotation.concurrent.Immutable;
import javax.lang.model.element.Modifier;

/**
 * Describes the signature and other feature of a delegate method.
 *
 * <p>We use method descriptions to refer to abstract methods defined in
 * com.facebook.litho.Component, so that we can define implementations that delegate to
 * client-declared methods with annotated props.
 */
@Immutable
public final class DelegateMethodDescription {

  /** Defines possible param types that can be used in delegate methods. */
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
  public final ImmutableList<LifecycleMethodArgumentType> lifecycleMethodArgumentTypes;
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
    lifecycleMethodArgumentTypes = builder.lifecycleMethodArgumentTypes;
    optionalParameters = builder.optionalParameters;
    optionalParameterTypes = builder.optionalParameterTypes;
    interStageInputAnnotations = builder.interStageInputAnnotations;
    extraMethods = builder.extraMethods;
    exceptions = builder.exceptions;
  }

  public ImmutableList<TypeName> allowedDelegateMethodArguments() {
    List<TypeName> types = new ArrayList<>();
    lifecycleMethodArgumentTypes.forEach(arg -> types.add(arg.type));
    return ImmutableList.copyOf(types);
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
        .lifecycleMethodArguments(methodDescription.lifecycleMethodArgumentTypes)
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
    private ImmutableList<LifecycleMethodArgumentType> lifecycleMethodArgumentTypes;
    private ImmutableList<OptionalParameterType> optionalParameterTypes;
    private ImmutableList<MethodParamModel> optionalParameters;
    private ImmutableList<Class<? extends Annotation>> interStageInputAnnotations;
    private ImmutableList<MethodSpec> extraMethods;
    private ImmutableList<TypeName> exceptions;

    private Builder() {}

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

    public Builder lifecycleMethodArguments(ImmutableList<LifecycleMethodArgumentType> args) {
      this.lifecycleMethodArgumentTypes = args;
      return this;
    }

    /**
     * List of required arguments for both the lifecycle and delegate method.
     *
     * @param parameterTypes list of argument types.
     * @return the current {@link Builder}.
     * @deprecated Use {@link #lifecycleMethodArguments(ImmutableList)}
     */
    @Deprecated
    public Builder definedParameterTypes(ImmutableList<TypeName> parameterTypes) {
      final List<LifecycleMethodArgumentType> args = new ArrayList<>(parameterTypes.size());
      for (TypeName arg : parameterTypes) {
        args.add(new LifecycleMethodArgumentType(arg));
      }

      return lifecycleMethodArguments(ImmutableList.copyOf(args));
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

    /** A list of extra methods that should be generate when this method description is used. */
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

      if (lifecycleMethodArgumentTypes == null) {
        lifecycleMethodArgumentTypes = ImmutableList.of();
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

  /**
   * This utility searches for the first lifecycle argument used by the delegate method, and
   * consumes it. Every unmatched type encountered before that is removed from the queue since the
   * order of arguments needs to be maintained.
   */
  public static boolean isAllowedTypeAndConsume(MethodParamModel arg, Queue<TypeName> types) {
    while (!types.isEmpty()) {
      TypeName type = types.poll();
      if (isArgumentTypeAllowed(arg, type)) {
        return true;
      }
    }

    return false;
  }

  /** Checks if any allowed argument type matches the delegate method param. */
  public static boolean isArgumentTypeAllowed(MethodParamModel arg, ImmutableList<TypeName> types) {
    for (TypeName type : types) {
      if (DelegateMethodDescription.isArgumentTypeAllowed(arg, type)) {
        return true;
      }
    }

    return false;
  }

  /** Checks if the argument type matches the delegate method param. */
  public static boolean isArgumentTypeAllowed(MethodParamModel arg, TypeName expected) {
    TypeName actual = arg.getTypeName();
    return arg instanceof SimpleMethodParamModel
        && (expected.equals(ClassNames.OBJECT) || expected.equals(actual))
        && arg.getAnnotations().isEmpty();
  }
}
