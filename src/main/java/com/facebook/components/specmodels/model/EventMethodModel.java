// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import javax.annotation.concurrent.Immutable;
import javax.lang.model.element.Modifier;

import com.facebook.common.internal.ImmutableList;
import com.facebook.components.annotations.OnEvent;

import com.squareup.javapoet.TypeName;

/**
 * Model that is an abstract representation of an event method (a method annotated with
 * {@link OnEvent}).
 */
@Immutable
public final class EventMethodModel {
  public final EventDeclarationModel eventType;
  public final ImmutableList<Modifier> modifiers;
  public final CharSequence name;
  public final TypeName returnType;
  public final ImmutableList<MethodParamModel> methodParams;
  public final Object representedObject;

  public EventMethodModel(
      EventDeclarationModel eventType,
      ImmutableList<Modifier> modifiers,
      CharSequence name,
      TypeName returnType,
      ImmutableList<MethodParamModel> methodParams,
      Object representedObject) {
    this.eventType = eventType;
    this.modifiers = modifiers;
    this.name = name;
    this.returnType = returnType;
    this.methodParams = methodParams;
    this.representedObject = representedObject;
  }
}
