// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import javax.annotation.concurrent.Immutable;

import com.facebook.common.internal.ImmutableList;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;

/**
 * Model that is an abstract representation of a {@link com.facebook.components.annotations.Event}.
 */
@Immutable
public class EventDeclarationModel {
  public final ClassName name;
  public final TypeName returnType;
  public final ImmutableList<FieldModel> fields;
  public final Object representedObject;

  public EventDeclarationModel(
      ClassName name,
      TypeName returnType,
      ImmutableList<FieldModel> fields,
      Object representedObject) {
    this.name = name;
    this.returnType = returnType;
    this.fields = fields;
    this.representedObject = representedObject;
  }

  public static class FieldModel {
    public final FieldSpec field;
    public final Object representedObject;

    public FieldModel(FieldSpec field, Object representedObject) {
      this.field = field;
      this.representedObject = representedObject;
    }
  }
}
