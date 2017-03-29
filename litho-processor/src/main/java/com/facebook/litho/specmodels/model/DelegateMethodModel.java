/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import javax.annotation.concurrent.Immutable;
import javax.lang.model.element.Modifier;

import java.lang.annotation.Annotation;

import com.facebook.common.internal.ImmutableList;

import com.squareup.javapoet.TypeName;

/**
 * Model that is an abstract representation of a delegate method.
 */
@Immutable
public final class DelegateMethodModel {
  public final ImmutableList<Annotation> annotations;
  public final ImmutableList<Modifier> modifiers;
  public final CharSequence name;
  public final TypeName returnType;
  public final ImmutableList<MethodParamModel> methodParams;
  public final Object representedObject;

  public DelegateMethodModel(
      ImmutableList<Annotation> annotations,
      ImmutableList<Modifier> modifiers,
      CharSequence name,
      TypeName returnType,
      ImmutableList<MethodParamModel> methodParams,
      Object representedObject) {
    this.annotations = annotations;
    this.modifiers = modifiers;
    this.name = name;
    this.returnType = returnType;
    this.methodParams = methodParams;
    this.representedObject = representedObject;
  }
}
