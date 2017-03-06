// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

import com.squareup.javapoet.TypeName;

public class Parameter {
  public final TypeName type;
  public final String name;

  public Parameter(TypeName type, String name) {
    this.type = type;
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Parameter) {
      final Parameter p = (Parameter) o;
      return type.equals(p.type) && name.equals(p.name);
    }

    return false;
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + name.hashCode();
    return result;
  }
}
