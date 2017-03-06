// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

import com.squareup.javapoet.TypeName;

public class ComponentField {
  final TypeName type;
  final String name;
  final boolean hasDefaultValue;

  public ComponentField(TypeName type, String name, boolean hasDefaultValue) {
    this.type = type;
    this.name = name;
    this.hasDefaultValue = hasDefaultValue;
  }
}
