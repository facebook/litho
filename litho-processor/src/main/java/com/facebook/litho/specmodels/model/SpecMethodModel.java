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
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.lang.annotation.Annotation;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.lang.model.element.Modifier;

/** Model that is an abstract representation of a method. */
@Immutable
public final class SpecMethodModel<Phantom, A> {
  public final ImmutableList<Annotation> annotations;
  public final ImmutableList<Modifier> modifiers;
  public final CharSequence name;
  public final TypeName returnType;
  public final TypeSpec returnTypeSpec;
  public final ImmutableList<TypeVariableName> typeVariables;
  public final ImmutableList<MethodParamModel> methodParams;
  public final Object representedObject;
  @Nullable public final A typeModel;

  public SpecMethodModel(
      ImmutableList<Annotation> annotations,
      ImmutableList<Modifier> modifiers,
      CharSequence name,
      TypeSpec returnTypeSpec,
      ImmutableList<TypeVariableName> typeVariables,
      ImmutableList<MethodParamModel> methodParams,
      Object representedObject,
      @Nullable A typeModel) {
    this.annotations = annotations;
    this.modifiers = modifiers;
    this.name = name;
    this.returnTypeSpec = returnTypeSpec;
    this.returnType = returnTypeSpec != null ? returnTypeSpec.getTypeName() : null;
    this.typeVariables = typeVariables;
    this.methodParams = methodParams;
    this.representedObject = representedObject;
    this.typeModel = typeModel;
  }

  public static <Phantom, A> Builder<Phantom, A> builder() {
    return new Builder<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SpecMethodModel<?, ?> that = (SpecMethodModel<?, ?>) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(modifiers, that.modifiers)
        && Objects.equals(name, that.name)
        && Objects.equals(returnType, that.returnType)
        && Objects.equals(returnTypeSpec, that.returnTypeSpec)
        && Objects.equals(typeVariables, that.typeVariables)
        && Objects.equals(methodParams, that.methodParams)
        && Objects.equals(representedObject, that.representedObject)
        && Objects.equals(typeModel, that.typeModel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        annotations,
        modifiers,
        name,
        returnType,
        returnTypeSpec,
        typeVariables,
        methodParams,
        representedObject,
        typeModel);
  }

  public static class Builder<Phantom, A> {
    private ImmutableList<Annotation> mAnnotations = ImmutableList.of();
    private ImmutableList<Modifier> mModifiers = ImmutableList.of();
    private CharSequence mName;
    private TypeSpec mReturnTypeSpec;
    private ImmutableList<TypeVariableName> mTypeVariables = ImmutableList.of();
    private ImmutableList<MethodParamModel> mMethodParams = ImmutableList.of();
    private Object mRepresentedObject;
    private A mTypeModel;

    private Builder() {}

    public Builder<Phantom, A> annotations(ImmutableList<Annotation> annotations) {
      mAnnotations = annotations;
      return this;
    }

    public Builder<Phantom, A> modifiers(ImmutableList<Modifier> modifiers) {
      mModifiers = modifiers;
      return this;
    }

    public Builder<Phantom, A> name(CharSequence name) {
      mName = name;
      return this;
    }

    public Builder<Phantom, A> returnTypeSpec(TypeSpec returnTypeSpec) {
      mReturnTypeSpec = returnTypeSpec;
      return this;
    }

    public Builder<Phantom, A> typeVariables(ImmutableList<TypeVariableName> typeVariables) {
      mTypeVariables = typeVariables;
      return this;
    }

    public Builder<Phantom, A> methodParams(ImmutableList<MethodParamModel> methodParams) {
      mMethodParams = methodParams;
      return this;
    }

    public Builder<Phantom, A> representedObject(Object representedObject) {
      mRepresentedObject = representedObject;
      return this;
    }

    public Builder<Phantom, A> typeModel(A typeModel) {
      mTypeModel = typeModel;
      return this;
    }

    public SpecMethodModel<Phantom, A> build() {
      return new SpecMethodModel<>(
          mAnnotations,
          mModifiers,
          mName,
          mReturnTypeSpec,
          mTypeVariables,
          mMethodParams,
          mRepresentedObject,
          mTypeModel);
    }
  }
}
