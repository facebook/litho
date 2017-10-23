/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;

import javax.lang.model.element.Modifier;

/**
 * Model that is an abstract representation of a method.
 */
@Immutable
public final class SpecMethodModel<Phantom, A> {
  public final ImmutableList<Annotation> annotations;
  public final ImmutableList<Modifier> modifiers;
  public final CharSequence name;
  public final TypeName returnType;
  public final ImmutableList<MethodParamModel> methodParams;
  public final Object representedObject;
  @Nullable
  public final A typeModel;

  public SpecMethodModel(
      ImmutableList<Annotation> annotations,
      ImmutableList<Modifier> modifiers,
      CharSequence name,
      TypeName returnType,
      ImmutableList<MethodParamModel> methodParams,
      Object representedObject,
      @Nullable
      A typeModel) {
    this.annotations = annotations;
    this.modifiers = modifiers;
    this.name = name;
    this.returnType = returnType;
    this.methodParams = methodParams;
    this.representedObject = representedObject;
    this.typeModel = typeModel;
  }
}
