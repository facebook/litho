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

import com.facebook.common.internal.ImmutableList;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;

/**
 * Model that is an abstract representation of a {@link com.facebook.litho.annotations.Event}.
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
