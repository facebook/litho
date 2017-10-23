/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import javax.annotation.concurrent.Immutable;
import javax.lang.model.element.Modifier;

/**
 * Model that is an abstract representation of an event method (a method annotated with
 * {@link OnEvent}).
 */
@Immutable
public final class EventMethodModel {
  public final ImmutableList<Modifier> modifiers;
  public final CharSequence name;
  public final TypeName returnType;
  public final ImmutableList<TypeVariableName> typeVariables;
  public final ImmutableList<MethodParamModel> methodParams;
  public final Object representedObject;
  public final EventDeclarationModel eventType;

  public EventMethodModel(
      EventDeclarationModel eventType,
      ImmutableList<Modifier> modifiers,
      CharSequence name,
      TypeName returnType,
      ImmutableList<TypeVariableName> typeVariables,
      ImmutableList<MethodParamModel> methodParams,
      Object representedObject) {
    this.eventType = eventType;
    this.modifiers = modifiers;
    this.name = name;
    this.returnType = returnType;
    this.typeVariables = typeVariables;
    this.methodParams = methodParams;
    this.representedObject = representedObject;
  }
}
